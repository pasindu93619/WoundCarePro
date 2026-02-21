package com.pasindu.woundcarepro.measurement

import android.graphics.Bitmap

object ImageWarp {
    fun warpWithInverseMapping(
        source: Bitmap,
        homographySrcToDst: DoubleArray,
        outputWidth: Int,
        outputHeight: Int
    ): Bitmap {
        val inverse = HomographySolver.invert3x3(homographySrcToDst)
        val out = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)

        for (y in 0 until outputHeight) {
            for (x in 0 until outputWidth) {
                val src = mapPoint(inverse, x.toDouble(), y.toDouble())
                val sx = src.first.toInt()
                val sy = src.second.toInt()
                if (sx in 0 until source.width && sy in 0 until source.height) {
                    out.setPixel(x, y, source.getPixel(sx, sy))
                }
            }
        }
        return out
    }

    private fun mapPoint(h: DoubleArray, x: Double, y: Double): Pair<Double, Double> {
        val w = h[6] * x + h[7] * y + h[8]
        if (kotlin.math.abs(w) < 1e-12) return 0.0 to 0.0
        val nx = (h[0] * x + h[1] * y + h[2]) / w
        val ny = (h[3] * x + h[4] * y + h[5]) / w
        return nx to ny
    }
}
