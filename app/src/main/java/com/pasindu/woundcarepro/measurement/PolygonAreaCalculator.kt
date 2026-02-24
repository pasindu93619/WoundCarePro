package com.pasindu.woundcarepro.measurement

import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.hypot

object PolygonAreaCalculator {
    fun calculateAreaPixels(points: List<PointF>): Double {
        if (points.size < 3) return 0.0

        var sum = 0.0
        for (i in points.indices) {
            val current = points[i]
            val next = points[(i + 1) % points.size]
            sum += (current.x * next.y) - (next.x * current.y)
        }
        return abs(sum) / 2.0
    }

    fun calculatePerimeterPixels(points: List<PointF>): Double {
        if (points.size < 3) return 0.0

        var perimeter = 0.0
        for (i in points.indices) {
            val current = points[i]
            val next = points[(i + 1) % points.size]
            perimeter += hypot((next.x - current.x).toDouble(), (next.y - current.y).toDouble())
        }
        return perimeter
    }

}
