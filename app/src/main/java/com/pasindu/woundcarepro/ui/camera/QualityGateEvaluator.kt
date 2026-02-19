package com.pasindu.woundcarepro.ui.camera

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.util.Locale
import kotlin.math.abs

private const val DEFAULT_ANALYSIS_SAMPLE_STRIDE = 4
private const val BLUR_TARGET_WIDTH = 96
private const val BLUR_MIN_DIMENSION = 32

data class QualityGateResult(
    val pitchDeg: Float,
    val rollDeg: Float,
    val meanLuma: Double,
    val glarePct: Double,
    val blurScore: Double,
    val tiltPass: Boolean,
    val lightPass: Boolean,
    val glarePass: Boolean,
    val blurPass: Boolean,
    val overallPass: Boolean
) {
    fun toGuidanceMetricsJson(timestampMillis: Long, qcStatus: String): String {
        return """
            {
              "timestampMillis": $timestampMillis,
              "pitchDeg": ${"%.2f".format(Locale.US, pitchDeg)},
              "rollDeg": ${"%.2f".format(Locale.US, rollDeg)},
              "meanLuma": ${"%.2f".format(Locale.US, meanLuma)},
              "glarePct": ${"%.6f".format(Locale.US, glarePct)},
              "blurScore": ${"%.2f".format(Locale.US, blurScore)},
              "tiltPass": $tiltPass,
              "lightPass": $lightPass,
              "glarePass": $glarePass,
              "blurPass": $blurPass,
              "overallPass": $overallPass,
              "qcStatus": "$qcStatus"
            }
        """.trimIndent()
    }

    companion object {
        fun empty(pitchDeg: Float = 0f, rollDeg: Float = 0f): QualityGateResult {
            return QualityGateResult(
                pitchDeg = pitchDeg,
                rollDeg = rollDeg,
                meanLuma = 0.0,
                glarePct = 1.0,
                blurScore = 0.0,
                tiltPass = false,
                lightPass = false,
                glarePass = false,
                blurPass = false,
                overallPass = false
            )
        }
    }
}

class QualityGateEvaluator(
    private val lumaStride: Int = DEFAULT_ANALYSIS_SAMPLE_STRIDE
) {
    companion object {
        const val MAX_TILT_DEGREES = 10f
        const val MIN_LIGHT_LUMA = 60.0
        const val MAX_LIGHT_LUMA = 200.0
        const val MAX_GLARE_PERCENT = 0.03
        const val MIN_BLUR_SCORE = 120.0
    }

    private var downsampleBuffer = IntArray(0)

    fun evaluate(pitchDeg: Float, rollDeg: Float, imageProxy: ImageProxy): QualityGateResult {
        val yPlane = imageProxy.planes.firstOrNull() ?: return QualityGateResult.empty(pitchDeg, rollDeg)
        val width = imageProxy.width
        val height = imageProxy.height
        if (width <= 0 || height <= 0) {
            return QualityGateResult.empty(pitchDeg, rollDeg)
        }

        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride
        val buffer = yPlane.buffer

        val downScale = maxOf(1, width / BLUR_TARGET_WIDTH)
        val downWidth = (width / downScale).coerceAtLeast(BLUR_MIN_DIMENSION)
        val downHeight = (height / downScale).coerceAtLeast(BLUR_MIN_DIMENSION)
        val downsampleSize = downWidth * downHeight
        if (downsampleBuffer.size < downsampleSize) {
            downsampleBuffer = IntArray(downsampleSize)
        }

        var lumaSum = 0.0
        var lumaCount = 0
        var glareCount = 0

        val xStep = maxOf(1, width / downWidth)
        val yStep = maxOf(1, height / downHeight)

        var downIndex = 0
        var y = 0
        while (y < height && downIndex < downsampleSize) {
            var x = 0
            while (x < width && downIndex < downsampleSize) {
                val luma = getLuma(buffer, rowStride, pixelStride, x, y)
                downsampleBuffer[downIndex++] = luma
                x += xStep
            }
            y += yStep
        }

        y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val luma = getLuma(buffer, rowStride, pixelStride, x, y)
                lumaSum += luma
                lumaCount++
                if (luma >= 250) glareCount++
                x += lumaStride
            }
            y += lumaStride
        }

        val sampledPixels = downIndex.coerceAtLeast(1)
        val blurScore = laplacianVariance(
            gray = downsampleBuffer,
            width = downWidth,
            height = downHeight,
            size = sampledPixels
        )

        val meanLuma = if (lumaCount == 0) 0.0 else lumaSum / lumaCount
        val glarePct = if (lumaCount == 0) 1.0 else glareCount.toDouble() / lumaCount

        val tiltPass = abs(pitchDeg) <= MAX_TILT_DEGREES && abs(rollDeg) <= MAX_TILT_DEGREES
        val lightPass = meanLuma in MIN_LIGHT_LUMA..MAX_LIGHT_LUMA
        val glarePass = glarePct <= MAX_GLARE_PERCENT
        val blurPass = blurScore >= MIN_BLUR_SCORE
        val overallPass = tiltPass && lightPass && glarePass && blurPass

        return QualityGateResult(
            pitchDeg = pitchDeg,
            rollDeg = rollDeg,
            meanLuma = meanLuma,
            glarePct = glarePct,
            blurScore = blurScore,
            tiltPass = tiltPass,
            lightPass = lightPass,
            glarePass = glarePass,
            blurPass = blurPass,
            overallPass = overallPass
        )
    }

    private fun laplacianVariance(gray: IntArray, width: Int, height: Int, size: Int): Double {
        if (width < 3 || height < 3 || size < width * 3) return 0.0

        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val index = y * width + x
                if (index >= size) continue
                val up = gray[(y - 1) * width + x]
                val down = gray[(y + 1) * width + x]
                val left = gray[y * width + x - 1]
                val right = gray[y * width + x + 1]
                val center = gray[index]
                val laplacian = -4 * center + up + down + left + right

                val value = laplacian.toDouble()
                sum += value
                sumSq += value * value
                count++
            }
        }

        if (count == 0) return 0.0
        val mean = sum / count
        return (sumSq / count) - (mean * mean)
    }

    private fun getLuma(
        buffer: ByteBuffer,
        rowStride: Int,
        pixelStride: Int,
        x: Int,
        y: Int
    ): Int {
        val index = y * rowStride + x * pixelStride
        return buffer.get(index).toInt() and 0xFF
    }
}
