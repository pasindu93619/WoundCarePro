package com.pasindu.woundcarepro.measurement

import android.graphics.PointF
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.abs

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
}

object OutlineJsonConverter {
    private val gson = Gson()

    fun toJson(points: List<PointF>): String = gson.toJson(points)

    fun fromJson(json: String?): List<PointF> {
        if (json.isNullOrBlank()) return emptyList()

        return runCatching {
            val type = object : TypeToken<List<PointF>>() {}.type
            gson.fromJson<List<PointF>>(json, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }
}
