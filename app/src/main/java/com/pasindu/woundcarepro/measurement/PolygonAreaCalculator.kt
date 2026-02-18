package com.pasindu.woundcarepro.measurement

import android.graphics.PointF
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.abs

data class WoundOutline(
    val points: List<PointF>
)

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

    fun toJson(outline: WoundOutline): String = gson.toJson(outline)

    fun fromJson(json: String?): WoundOutline {
        if (json.isNullOrBlank()) return WoundOutline(emptyList())

        return runCatching {
            val type = object : TypeToken<WoundOutline>() {}.type
            gson.fromJson<WoundOutline>(json, type) ?: WoundOutline(emptyList())
        }.getOrDefault(WoundOutline(emptyList()))
    }
}
