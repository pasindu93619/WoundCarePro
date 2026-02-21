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
                    seedDefaultRows(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    seedDefaultRows(db)
                }

                private fun seedDefaultRows(db: SupportSQLiteDatabase) {
                    val now = System.currentTimeMillis()
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO patients (patientId, name, createdAt)
                        VALUES (?, ?, ?)
                        """.trimIndent(),
                        arrayOf(DEFAULT_PATIENT_ID, "Anonymous Patient", 0L)
                    )
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO wounds (woundId, patientId, location, createdAtMillis)
                        VALUES (?, ?, ?, ?)
                        """.trimIndent(),
                        arrayOf(DEFAULT_WOUND_ID, DEFAULT_PATIENT_ID, "Unspecified", now)
                    )
                }
            }).addMigrations(
                DatabaseMigrations.MIGRATION_9_10,
                DatabaseMigrations.MIGRATION_10_11,
                DatabaseMigrations.MIGRATION_11_12,
                DatabaseMigrations.MIGRATION_12_13,
                DatabaseMigrations.MIGRATION_13_14,
                DatabaseMigrations.MIGRATION_14_15
            ).build().also { instance = it }
        }
    }
}
