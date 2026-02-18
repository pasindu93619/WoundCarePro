package com.pasindu.woundcarepro.measurement

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlin.math.abs

data class OutlinePoint(
    val x: Float,
    val y: Float
)

data class WoundOutline(
    val points: List<OutlinePoint>
)

object PolygonAreaCalculator {
    fun calculateAreaPixels(points: List<OutlinePoint>): Double {
        if (points.size < 3) return 0.0

        var sum = 0.0
        for (i in points.indices) {
            val current = points[i]
            val next = points[(i + 1) % points.size]
            sum += (current.x * next.y) - (next.x * current.y)
        }
        return abs(sum) / 2.0
    }
}

object OutlineJsonConverter {
    private val gson = Gson()

    fun toJson(points: List<OutlinePoint>): String {
        return gson.toJson(WoundOutline(points = points))
    }

    fun fromJson(json: String?): List<OutlinePoint> {
        if (json.isNullOrBlank()) return emptyList()

        return try {
            gson.fromJson(json, WoundOutline::class.java)?.points.orEmpty()
        } catch (_: JsonSyntaxException) {
            emptyList()
        }
    }
}
