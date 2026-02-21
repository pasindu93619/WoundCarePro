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
            ).fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedDefaultPatient(db)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        seedDefaultPatient(db)
                    }

                    private fun seedDefaultPatient(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO patients (patientId, name, createdAt)
                            VALUES (?, ?, ?)
                            """.trimIndent(),
                            arrayOf(DEFAULT_PATIENT_ID, "Anonymous Patient", 0L)
                        )
                    }
                })
                .build().also { instance = it }
        }
    }
}
