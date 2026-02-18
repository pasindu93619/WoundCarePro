package com.pasindu.woundcarepro.measurement

import kotlin.math.abs
import org.json.JSONArray
import org.json.JSONObject

data class OutlinePoint(
    val x: Double,
    val y: Double
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
    fun toJson(points: List<OutlinePoint>): String {
        val array = JSONArray()
        points.forEach { point ->
            array.put(
                JSONObject()
                    .put("x", point.x)
                    .put("y", point.y)
            )
        }
        return array.toString()
    }

    fun fromJson(json: String?): List<OutlinePoint> {
        if (json.isNullOrBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    if (!item.has("x") || !item.has("y")) continue
                    add(OutlinePoint(x = item.getDouble("x"), y = item.getDouble("y")))
                }
            }
        }.getOrDefault(emptyList())
    }
}
