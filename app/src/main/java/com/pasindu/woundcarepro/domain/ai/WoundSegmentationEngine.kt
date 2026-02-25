package com.pasindu.woundcarepro.domain.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot

@Singleton
class WoundSegmentationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val MODEL_PATH: String = "models/wound_segmentation.tflite"
        private const val INTERPRETER_THREADS = 4
        private const val BOUNDARY_SIMPLIFICATION_EPSILON = 2.0f
    }

    data class SegmentationOutput(
        val boundaryPoints: List<PointF>,
        val tissuePercentages: Map<Int, Float>,
        val confidence: Float,
        val runtimeMs: Long
    )

    private val interpreter: Interpreter by lazy {
        val options = Interpreter.Options().apply {
            setNumThreads(INTERPRETER_THREADS)
            // Optional NNAPI acceleration; off by default for compatibility.
            setUseNNAPI(false)
        }
        Interpreter(loadModelFile(), options)
    }

    suspend fun segment(imagePath: String): SegmentationOutput {
        val startedAt = System.currentTimeMillis()

        val sourceBitmap = BitmapFactory.decodeFile(imagePath)
            ?: throw IllegalArgumentException("Unable to decode image from path: $imagePath")

        val inputTensor = interpreter.getInputTensor(0)
        val shape = inputTensor.shape()
        if (shape.size != 4 || shape[0] != 1) {
            throw IllegalStateException(
                "Unexpected input tensor shape ${shape.contentToString()} for $MODEL_PATH. Expected [1, height, width, channels]."
            )
        }

        val inputHeight = shape[1]
        val inputWidth = shape[2]
        val inputChannels = shape[3]

        if (inputChannels !in setOf(1, 3, 4)) {
            throw IllegalStateException("Unexpected input channels: $inputChannels")
        }

        val resizedBitmap = Bitmap.createScaledBitmap(sourceBitmap, inputWidth, inputHeight, true)
        val inputBuffer = bitmapToInputBuffer(resizedBitmap, inputChannels, inputTensor.dataType())

        val outputTensor = interpreter.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        val outputDataType = outputTensor.dataType()

        val outputArray = allocateOutputArray(outputShape, outputDataType)
        interpreter.run(inputBuffer, outputArray)

        val parsed = parseOutput(outputArray, outputShape, outputDataType)
        val largestBoundary = extractLargestBoundary(parsed.labels)
        val simplifiedBoundary = simplifyPolygon(largestBoundary, BOUNDARY_SIMPLIFICATION_EPSILON)
        val remappedBoundary = remapPointsToOriginal(
            simplifiedBoundary,
            fromWidth = inputWidth,
            fromHeight = inputHeight,
            toWidth = sourceBitmap.width,
            toHeight = sourceBitmap.height
        )

        val runtimeMs = System.currentTimeMillis() - startedAt

        return SegmentationOutput(
            boundaryPoints = remappedBoundary,
            tissuePercentages = parsed.tissuePercentages,
            confidence = parsed.confidence,
            runtimeMs = runtimeMs
        )
    }

    private fun loadModelFile(): MappedByteBuffer {
        return try {
            context.assets.openFd(MODEL_PATH).use { fileDescriptor ->
                FileInputStream(fileDescriptor.fileDescriptor).channel.use { fileChannel ->
                    fileChannel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fileDescriptor.startOffset,
                        fileDescriptor.declaredLength
                    )
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Missing or unreadable model asset at '$MODEL_PATH'. Ensure app/src/main/assets/$MODEL_PATH exists.",
                e
            )
        }
    }

    private fun bitmapToInputBuffer(
        bitmap: Bitmap,
        channels: Int,
        dataType: DataType
    ): ByteBuffer {
        val bytesPerChannel = when (dataType) {
            DataType.FLOAT32 -> 4
            DataType.UINT8 -> 1
            else -> throw IllegalStateException("Unsupported input tensor data type: $dataType")
        }

        val capacity = bitmap.width * bitmap.height * channels * bytesPerChannel
        val buffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)
            val channelValues = when (channels) {
                1 -> intArrayOf(((r + g + b) / 3))
                3 -> intArrayOf(r, g, b)
                4 -> intArrayOf(r, g, b, (pixel ushr 24 and 0xFF))
                else -> throw IllegalStateException("Unsupported channel count: $channels")
            }

            for (value in channelValues) {
                when (dataType) {
                    DataType.FLOAT32 -> buffer.putFloat(value / 255f)
                    DataType.UINT8 -> buffer.put(value.toByte())
                    else -> Unit
                }
            }
        }

        buffer.rewind()
        return buffer
    }

    private fun allocateOutputArray(shape: IntArray, dataType: DataType): Any {
        if (shape.isEmpty() || shape[0] != 1) {
            throw IllegalStateException(
                "Unexpected output tensor shape ${shape.contentToString()} for $MODEL_PATH."
            )
        }

        val h: Int
        val w: Int
        val c: Int

        when (shape.size) {
            4 -> {
                h = shape[1]
                w = shape[2]
                c = shape[3]
            }

            3 -> {
                h = shape[1]
                w = shape[2]
                c = 1
            }

            else -> {
                throw IllegalStateException(
                    "Unexpected output tensor shape ${shape.contentToString()} for $MODEL_PATH. Expected [1,h,w] or [1,h,w,c]."
                )
            }
        }

        return when (dataType) {
            DataType.FLOAT32 -> if (c == 1) Array(1) { Array(h) { FloatArray(w) } }
            else Array(1) { Array(h) { Array(w) { FloatArray(c) } } }

            DataType.UINT8 -> if (c == 1) Array(1) { Array(h) { ByteArray(w) } }
            else Array(1) { Array(h) { Array(w) { ByteArray(c) } } }

            else -> throw IllegalStateException("Unsupported output tensor data type: $dataType")
        }
    }

    private data class ParsedOutput(
        val labels: Array<IntArray>,
        val tissuePercentages: Map<Int, Float>,
        val confidence: Float
    )

    @Suppress("UNCHECKED_CAST")
    private fun parseOutput(output: Any, shape: IntArray, dataType: DataType): ParsedOutput {
        val height = shape[1]
        val width = shape[2]
        val channels = if (shape.size == 4) shape[3] else 1

        val labels = Array(height) { IntArray(width) }
        val tissueCounts = mutableMapOf<Int, Int>()
        var woundPixelCount = 0
        var confidenceSum = 0f

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (channels == 1) {
                    val score = when (dataType) {
                        DataType.FLOAT32 -> (output as Array<Array<FloatArray>>)[0][y][x]
                        DataType.UINT8 -> ((output as Array<Array<ByteArray>>)[0][y][x].toInt() and 0xFF) / 255f
                        else -> throw IllegalStateException("Unsupported output data type: $dataType")
                    }
                    val label = if (score >= 0.5f) 1 else 0
                    labels[y][x] = label
                    if (label == 1) {
                        woundPixelCount++
                        confidenceSum += score
                    }
                } else {
                    val probs = FloatArray(channels)
                    when (dataType) {
                        DataType.FLOAT32 -> {
                            val logits = (output as Array<Array<Array<FloatArray>>>)[0][y][x]
                            System.arraycopy(logits, 0, probs, 0, channels)
                        }

                        DataType.UINT8 -> {
                            val raw = (output as Array<Array<Array<ByteArray>>>)[0][y][x]
                            for (i in 0 until channels) {
                                probs[i] = (raw[i].toInt() and 0xFF) / 255f
                            }
                        }

                        else -> throw IllegalStateException("Unsupported output data type: $dataType")
                    }

                    var bestIdx = 0
                    var bestScore = probs[0]
                    for (i in 1 until channels) {
                        if (probs[i] > bestScore) {
                            bestScore = probs[i]
                            bestIdx = i
                        }
                    }

                    labels[y][x] = bestIdx
                    if (bestIdx > 0) {
                        tissueCounts[bestIdx] = (tissueCounts[bestIdx] ?: 0) + 1
                        woundPixelCount++
                        confidenceSum += bestScore
                    }
                }
            }
        }

        val tissuePercentages = if (woundPixelCount == 0 || channels == 1) {
            emptyMap()
        } else {
            tissueCounts.mapValues { (_, count) -> (count.toFloat() / woundPixelCount.toFloat()) * 100f }
        }

        val confidence = if (woundPixelCount > 0) confidenceSum / woundPixelCount else 0f

        return ParsedOutput(labels, tissuePercentages, confidence)
    }

    private fun extractLargestBoundary(labels: Array<IntArray>): List<PointF> {
        val height = labels.size
        val width = labels.firstOrNull()?.size ?: 0
        if (height == 0 || width == 0) return emptyList()

        val visited = Array(height) { BooleanArray(width) }
        var bestComponent: List<Pair<Int, Int>> = emptyList()

        val directions = arrayOf(
            intArrayOf(1, 0),
            intArrayOf(-1, 0),
            intArrayOf(0, 1),
            intArrayOf(0, -1)
        )

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (labels[y][x] <= 0 || visited[y][x]) continue

                val queue = ArrayDeque<Pair<Int, Int>>()
                val component = mutableListOf<Pair<Int, Int>>()
                queue.add(y to x)
                visited[y][x] = true

                while (queue.isNotEmpty()) {
                    val (cy, cx) = queue.removeFirst()
                    component.add(cy to cx)

                    for (d in directions) {
                        val ny = cy + d[0]
                        val nx = cx + d[1]
                        if (ny !in 0 until height || nx !in 0 until width) continue
                        if (labels[ny][nx] <= 0 || visited[ny][nx]) continue
                        visited[ny][nx] = true
                        queue.add(ny to nx)
                    }
                }

                if (component.size > bestComponent.size) bestComponent = component
            }
        }

        if (bestComponent.isEmpty()) return emptyList()

        val componentSet = bestComponent.toHashSet()
        val boundary = mutableListOf<PointF>()

        for ((y, x) in bestComponent) {
            var isBoundary = false
            for (d in directions) {
                val ny = y + d[0]
                val nx = x + d[1]
                if (ny !in 0 until height || nx !in 0 until width || !componentSet.contains(ny to nx)) {
                    isBoundary = true
                    break
                }
            }
            if (isBoundary) boundary.add(PointF(x.toFloat(), y.toFloat()))
        }

        val centerX = boundary.map { it.x }.average().toFloat()
        val centerY = boundary.map { it.y }.average().toFloat()

        return boundary.sortedBy { kotlin.math.atan2((it.y - centerY).toDouble(), (it.x - centerX).toDouble()) }
    }

    private fun simplifyPolygon(points: List<PointF>, epsilon: Float): List<PointF> {
        if (points.size < 3) return points
        return douglasPeucker(points, epsilon)
    }

    private fun douglasPeucker(points: List<PointF>, epsilon: Float): List<PointF> {
        if (points.size < 3) return points

        val first = points.first()
        val last = points.last()

        var maxDistance = 0f
        var index = 0

        for (i in 1 until points.lastIndex) {
            val distance = perpendicularDistance(points[i], first, last)
            if (distance > maxDistance) {
                index = i
                maxDistance = distance
            }
        }

        return if (maxDistance > epsilon) {
            val left = douglasPeucker(points.subList(0, index + 1), epsilon)
            val right = douglasPeucker(points.subList(index, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(point: PointF, lineStart: PointF, lineEnd: PointF): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        if (dx == 0f && dy == 0f) {
            return hypot(point.x - lineStart.x, point.y - lineStart.y)
        }

        val numerator = kotlin.math.abs(
            dy * point.x - dx * point.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x
        )
        val denominator = hypot(dx, dy)
        return numerator / denominator
    }

    private fun remapPointsToOriginal(
        points: List<PointF>,
        fromWidth: Int,
        fromHeight: Int,
        toWidth: Int,
        toHeight: Int
    ): List<PointF> {
        if (fromWidth == 0 || fromHeight == 0) return emptyList()

        val sx = toWidth.toFloat() / fromWidth.toFloat()
        val sy = toHeight.toFloat() / fromHeight.toFloat()

        return points.map { PointF(it.x * sx, it.y * sy) }
    }
}
