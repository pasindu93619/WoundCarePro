package com.pasindu.woundcarepro.ui.camera

import androidx.camera.core.ImageProxy
import java.util.Locale
import kotlin.math.abs

object QualityGateEvaluator {
    private const val LUMA_STRIDE = 4
    private const val BLUR_TARGET_WIDTH = 64
    private const val BLUR_MIN_DIMENSION = 32
    const val MAX_TILT_DEGREES = 10f
    const val MIN_LIGHT_LUMA = 60.0
    const val MAX_LIGHT_LUMA = 200.0
    const val MAX_GLARE_PERCENT = 0.03
    const val MIN_BLUR_SCORE = 18.0

    data class QcResult(
        val meanLuma: Double,
        val glarePct: Double,
        val blurScore: Double,
        val pitchDeg: Float,
        val rollDeg: Float,
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
    }

    fun evaluate(pitchDeg: Float, rollDeg: Float, imageProxy: ImageProxy): QcResult {
        val plane = imageProxy.planes.firstOrNull()
            ?: return emptyResult(pitchDeg, rollDeg)

        val width = imageProxy.width
        val height = imageProxy.height
        if (width <= 0 || height <= 0) {
            return emptyResult(pitchDeg, rollDeg)
        }

        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        var lumaSum = 0.0
        var lumaCount = 0
        var glareCount = 0

        val scale = maxOf(1, width / BLUR_TARGET_WIDTH)
        val downWidth = (width / scale).coerceAtLeast(BLUR_MIN_DIMENSION)
        val downHeight = (height / scale).coerceAtLeast(BLUR_MIN_DIMENSION)
        val downsample = IntArray(downWidth * downHeight)

        val xStep = (width.toFloat() / downWidth).coerceAtLeast(1f)
        val yStep = (height.toFloat() / downHeight).coerceAtLeast(1f)

        var downIndex = 0
        var yCoord = 0f
        while (downIndex < downsample.size && yCoord < height) {
            val sourceY = yCoord.toInt().coerceIn(0, height - 1)
            var xCoord = 0f
            repeat(downWidth) {
                val sourceX = xCoord.toInt().coerceIn(0, width - 1)
                val luma = getLuma(buffer, rowStride, pixelStride, sourceX, sourceY)
                downsample[downIndex] = luma
                downIndex++
                xCoord += xStep
            }
            yCoord += yStep
        }

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val luma = getLuma(buffer, rowStride, pixelStride, x, y)
                lumaSum += luma
                lumaCount++
                if (luma >= 250) {
                    glareCount++
                }
                x += LUMA_STRIDE
            }
            y += LUMA_STRIDE
        }

        val meanLuma = if (lumaCount > 0) lumaSum / lumaCount else 0.0
        val glarePct = if (lumaCount > 0) glareCount.toDouble() / lumaCount else 0.0
        val blurScore = laplacianVariance(downsample, downWidth, downHeight)

        val tiltPass = abs(pitchDeg) <= MAX_TILT_DEGREES && abs(rollDeg) <= MAX_TILT_DEGREES
        val lightPass = meanLuma in MIN_LIGHT_LUMA..MAX_LIGHT_LUMA
        val glarePass = glarePct <= MAX_GLARE_PERCENT
        val blurPass = blurScore >= MIN_BLUR_SCORE
        val overallPass = tiltPass && lightPass && glarePass && blurPass

        return QcResult(
            meanLuma = meanLuma,
            glarePct = glarePct,
            blurScore = blurScore,
            pitchDeg = pitchDeg,
            rollDeg = rollDeg,
            tiltPass = tiltPass,
            lightPass = lightPass,
            glarePass = glarePass,
            blurPass = blurPass,
            overallPass = overallPass
        )
    }

    private fun laplacianVariance(gray: IntArray, width: Int, height: Int): Double {
        if (width < 3 || height < 3) return 0.0

        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val c = gray[y * width + x]
                val laplacian =
                    (gray[(y - 1) * width + x] +
                        gray[(y + 1) * width + x] +
                        gray[y * width + x - 1] +
                        gray[y * width + x + 1]) -
                        4 * c

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
        buffer: java.nio.ByteBuffer,
        rowStride: Int,
        pixelStride: Int,
        x: Int,
        y: Int
    ): Int {
        val index = y * rowStride + x * pixelStride
        return buffer.get(index).toInt() and 0xFF
    }

    private fun emptyResult(pitchDeg: Float, rollDeg: Float): QcResult {
        return QcResult(
            meanLuma = 0.0,
            glarePct = 1.0,
            blurScore = 0.0,
            pitchDeg = pitchDeg,
            rollDeg = rollDeg,
            tiltPass = false,
            lightPass = false,
            glarePass = false,
            blurPass = false,
            overallPass = false
        )
    }
}
