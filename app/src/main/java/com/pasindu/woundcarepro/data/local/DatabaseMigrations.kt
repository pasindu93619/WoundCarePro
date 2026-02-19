package com.pasindu.woundcarepro.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_9_10: Migration = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE assessments ADD COLUMN woundLocation TEXT")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS measurements_new (
                    measurementId TEXT NOT NULL PRIMARY KEY,
                    assessmentId TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    pixelArea REAL NOT NULL,
                    areaCm2 REAL,
                    outlineJson TEXT,
                    FOREIGN KEY(assessmentId) REFERENCES assessments(assessmentId) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO measurements_new (measurementId, assessmentId, createdAtMillis, pixelArea, areaCm2, outlineJson)
                SELECT m.measurementId, m.assessmentId, m.createdAt, m.areaPixels, m.areaCm2, a.outlineJson
                FROM measurements m
                INNER JOIN (
                    SELECT assessmentId, MAX(createdAt) AS maxCreatedAt
                    FROM measurements
                    GROUP BY assessmentId
                ) latest
                ON m.assessmentId = latest.assessmentId AND m.createdAt = latest.maxCreatedAt
                LEFT JOIN assessments a ON a.assessmentId = m.assessmentId
                """.trimIndent()
            )

            db.execSQL("DROP TABLE measurements")
            db.execSQL("ALTER TABLE measurements_new RENAME TO measurements")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_measurements_assessmentId ON measurements(assessmentId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_measurements_createdAtMillis ON measurements(createdAtMillis)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_patientId ON assessments(patientId)")
        }
    }
}
