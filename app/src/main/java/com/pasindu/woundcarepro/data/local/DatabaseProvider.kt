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
            ).addMigrations(
                DatabaseMigrations.MIGRATION_9_10,
                DatabaseMigrations.MIGRATION_10_11,
                DatabaseMigrations.MIGRATION_11_12,
                DatabaseMigrations.MIGRATION_12_13,
                DatabaseMigrations.MIGRATION_13_14
            ).build().also { instance = it }
        }
    }
}
