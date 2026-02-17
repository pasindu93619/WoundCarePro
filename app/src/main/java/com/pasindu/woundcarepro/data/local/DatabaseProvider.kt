package com.pasindu.woundcarepro.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var instance: WoundCareDatabase? = null

    fun getDatabase(context: Context): WoundCareDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WoundCareDatabase::class.java,
                "wound-care.db"
            ).build().also { instance = it }
        }
    }
}
