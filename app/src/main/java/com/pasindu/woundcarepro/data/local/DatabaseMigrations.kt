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

    val MIGRATION_10_11: Migration = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val measurementColumns = db.getColumnNames("measurements")
            if (!measurementColumns.contains("createdAtMillis")) {
                db.execSQL("ALTER TABLE measurements ADD COLUMN createdAtMillis INTEGER NOT NULL DEFAULT 0")
            }
            if (!measurementColumns.contains("pixelArea")) {
                db.execSQL("ALTER TABLE measurements ADD COLUMN pixelArea REAL NOT NULL DEFAULT 0")
            }
            if (!measurementColumns.contains("areaCm2")) {
                db.execSQL("ALTER TABLE measurements ADD COLUMN areaCm2 REAL")
            }

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_measurements_assessmentId ON measurements(assessmentId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_patientId ON assessments(patientId)")
        }
    }

    val MIGRATION_11_12: Migration = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val assessmentColumns = db.getColumnNames("assessments")
            if (!assessmentColumns.contains("guidanceMetricsJson")) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN guidanceMetricsJson TEXT")
            }
        }
    }

    val MIGRATION_12_13: Migration = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val assessmentColumns = db.getColumnNames("assessments")
            if (!assessmentColumns.contains("rectifiedImagePath")) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN rectifiedImagePath TEXT")
            }
            if (!assessmentColumns.contains("markerCornersJson")) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN markerCornersJson TEXT")
            }
            if (!assessmentColumns.contains("homographyJson")) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN homographyJson TEXT")
            }
        }
    }
}

private fun SupportSQLiteDatabase.getColumnNames(tableName: String): Set<String> {
    query("PRAGMA table_info(`$tableName`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        val columns = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            if (nameIndex >= 0) {
                columns.add(cursor.getString(nameIndex))
            }
        }
        return columns
    }
}
