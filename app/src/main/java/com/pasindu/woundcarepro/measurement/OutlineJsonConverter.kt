package com.pasindu.woundcarepro.measurement

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
