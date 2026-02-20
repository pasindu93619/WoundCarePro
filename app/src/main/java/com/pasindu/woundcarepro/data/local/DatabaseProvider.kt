package com.pasindu.woundcarepro.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    @Volatile
    private var instance: WoundCareDatabase? = null

    fun getDatabase(context: Context): WoundCareDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WoundCareDatabase::class.java,
                "wound-care.db"
            ).addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val now = System.currentTimeMillis()
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO patients (patientId, name, createdAt)
                        VALUES ('anonymous', 'Anonymous', $now)
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO wounds (woundId, patientId, location, createdAtMillis)
                        VALUES ('unspecified', 'anonymous', 'Unspecified', $now)
                        """.trimIndent()
                    )
                }
            }).addMigrations(
                DatabaseMigrations.MIGRATION_9_10,
                DatabaseMigrations.MIGRATION_10_11,
                DatabaseMigrations.MIGRATION_11_12,
                DatabaseMigrations.MIGRATION_12_13,
                DatabaseMigrations.MIGRATION_13_14
            ).build().also { instance = it }
        }
    }
}
