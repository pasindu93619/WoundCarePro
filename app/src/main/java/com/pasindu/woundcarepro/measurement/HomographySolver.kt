package com.pasindu.woundcarepro.measurement

import android.graphics.PointF

object HomographySolver {
    fun solve(src: List<PointF>, dst: List<PointF>): DoubleArray {
        require(src.size == 4 && dst.size == 4) { "Homography requires 4 source and 4 destination points" }

        val a = Array(8) { DoubleArray(8) }
        val b = DoubleArray(8)

        for (i in 0 until 4) {
            val x = src[i].x.toDouble()
            val y = src[i].y.toDouble()
            val u = dst[i].x.toDouble()
            val v = dst[i].y.toDouble()

            val row1 = i * 2
            val row2 = row1 + 1

            a[row1][0] = x
            a[row1][1] = y
            a[row1][2] = 1.0
            a[row1][3] = 0.0
            a[row1][4] = 0.0
            a[row1][5] = 0.0
            a[row1][6] = -u * x
            a[row1][7] = -u * y
            b[row1] = u

            a[row2][0] = 0.0
            a[row2][1] = 0.0
            a[row2][2] = 0.0
            a[row2][3] = x
            a[row2][4] = y
            a[row2][5] = 1.0
            a[row2][6] = -v * x
            a[row2][7] = -v * y
            b[row2] = v
        }

        val h = gaussianElimination(a, b)
        return doubleArrayOf(
            h[0], h[1], h[2],
            h[3], h[4], h[5],
            h[6], h[7], 1.0
        )
    }

    fun invert3x3(m: DoubleArray): DoubleArray {
        require(m.size == 9)
        val a = m[0]; val b = m[1]; val c = m[2]
        val d = m[3]; val e = m[4]; val f = m[5]
        val g = m[6]; val h = m[7]; val i = m[8]

        val det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
        require(kotlin.math.abs(det) > 1e-12) { "Homography matrix is singular" }

        val invDet = 1.0 / det
        return doubleArrayOf(
            (e * i - f * h) * invDet,
            (c * h - b * i) * invDet,
            (b * f - c * e) * invDet,
            (f * g - d * i) * invDet,
            (a * i - c * g) * invDet,
            (c * d - a * f) * invDet,
            (d * h - e * g) * invDet,
            (b * g - a * h) * invDet,
            (a * e - b * d) * invDet
        )
    }

    private fun gaussianElimination(a: Array<DoubleArray>, b: DoubleArray): DoubleArray {
        val n = b.size
        for (i in 0 until n) {
            var maxRow = i
            for (r in i + 1 until n) {
                if (kotlin.math.abs(a[r][i]) > kotlin.math.abs(a[maxRow][i])) maxRow = r
            }
            if (maxRow != i) {
                val tmpRow = a[i]
                a[i] = a[maxRow]
                a[maxRow] = tmpRow
                val tmpB = b[i]
                b[i] = b[maxRow]
                b[maxRow] = tmpB
            }

            val pivot = a[i][i]
            require(kotlin.math.abs(pivot) > 1e-12) { "Cannot solve homography; degenerate point configuration" }

            for (c in i until n) a[i][c] /= pivot
            b[i] /= pivot

            for (r in 0 until n) {
                if (r == i) continue
                val factor = a[r][i]
                for (c in i until n) {
                    a[r][c] -= factor * a[i][c]
                }
                b[r] -= factor * b[i]
            }
        }
        return b
    }
}
