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

    val MIGRATION_13_14: Migration = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val assessmentColumns = db.getColumnNames("assessments")
            if (!assessmentColumns.contains("finalOutlineJson")) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN finalOutlineJson TEXT")
            }
            if (!assessmentColumns.contains("finalPolygonPointsJson")) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN finalPolygonPointsJson TEXT")
            }
            if (!assessmentColumns.contains("finalPixelArea")) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN finalPixelArea REAL")
            }
            if (!assessmentColumns.contains("finalAreaCm2")) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN finalAreaCm2 REAL")
            }
            if (!assessmentColumns.contains("finalPerimeterPx")) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN finalPerimeterPx REAL")
            }
            if (!assessmentColumns.contains("finalSavedAtMillis")) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN finalSavedAtMillis INTEGER")
            }

            db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_woundId ON assessments(woundId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_assessments_timestamp ON assessments(timestamp)")
        }
    }

    val MIGRATION_14_15: Migration = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            if (!db.tableExists("audit_logs")) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS audit_logs (
                      auditId TEXT NOT NULL PRIMARY KEY,
                      timestampMillis INTEGER NOT NULL,
                      action TEXT NOT NULL,
                      patientId TEXT,
                      assessmentId TEXT,
                      metadataJson TEXT
                    )
                    """.trimIndent()
                )
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_timestampMillis ON audit_logs(timestampMillis)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_patientId ON audit_logs(patientId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_assessmentId ON audit_logs(assessmentId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_action ON audit_logs(action)")
        }
    }

    val MIGRATION_15_16: Migration = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            if (!db.tableExists("consents")) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS consents (
                      consentId TEXT NOT NULL PRIMARY KEY,
                      patientId TEXT NOT NULL,
                      timestampMillis INTEGER NOT NULL,
                      consentGiven INTEGER NOT NULL,
                      consentType TEXT NOT NULL,
                      note TEXT,
                      FOREIGN KEY(patientId) REFERENCES patients(patientId) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS index_consents_patientId ON consents(patientId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_consents_timestampMillis ON consents(timestampMillis)")
        }
    }

    val MIGRATION_16_17: Migration = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            if (!db.tableExists("ai_segmentation_results")) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ai_segmentation_results (
                      resultId TEXT NOT NULL PRIMARY KEY,
                      assessmentId TEXT NOT NULL,
                      modelVersion TEXT NOT NULL,
                      autoOutlineJson TEXT NOT NULL,
                      maskPngPath TEXT,
                      confidence REAL,
                      processingTimeMs INTEGER NOT NULL,
                      createdAt INTEGER NOT NULL,
                      FOREIGN KEY(assessmentId) REFERENCES assessments(assessmentId) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_ai_segmentation_results_assessmentId ON ai_segmentation_results(assessmentId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_segmentation_results_createdAt ON ai_segmentation_results(createdAt)")
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


private fun SupportSQLiteDatabase.tableExists(tableName: String): Boolean {
    query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use { cursor ->
        return cursor.moveToFirst()
    }
}
