package com.pasindu.woundcarepro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pasindu.woundcarepro.data.local.entity.Assessment

@Dao
interface AssessmentDao {
    @Upsert
    suspend fun upsert(assessment: Assessment)

    @Query("SELECT * FROM assessments WHERE assessmentId = :assessmentId LIMIT 1")
    suspend fun getById(assessmentId: String): Assessment?

    @Query("SELECT * FROM assessments WHERE patientId = :patientId ORDER BY timestamp DESC")
    suspend fun listByPatient(patientId: String): List<Assessment>

    @Query("SELECT * FROM assessments ORDER BY timestamp DESC")
    suspend fun listRecent(): List<Assessment>

    @Query("UPDATE assessments SET guidanceMetricsJson = :guidanceMetricsJson WHERE assessmentId = :assessmentId")
    suspend fun updateGuidanceMetrics(assessmentId: String, guidanceMetricsJson: String)

    @Query(
        "UPDATE assessments SET imagePath = :imagePath, guidanceMetricsJson = :guidanceMetricsJson WHERE assessmentId = :assessmentId"
    )
    suspend fun updateCaptureMetadata(assessmentId: String, imagePath: String, guidanceMetricsJson: String)

    @Query(
        """
        UPDATE assessments
        SET finalOutlineJson = :finalOutlineJson,
            finalPolygonPointsJson = :finalPolygonPointsJson,
            finalPixelArea = :finalPixelArea,
            finalAreaCm2 = :finalAreaCm2,
            finalPerimeterPx = :finalPerimeterPx,
            finalSavedAtMillis = :finalSavedAtMillis
        WHERE assessmentId = :assessmentId
        """
    )
    suspend fun updateFinalOutlineAndMetrics(
        assessmentId: String,
        finalOutlineJson: String?,
        finalPolygonPointsJson: String?,
        finalPixelArea: Double?,
        finalAreaCm2: Double?,
        finalPerimeterPx: Double?,
        finalSavedAtMillis: Long?
    )

    @Query("DELETE FROM assessments WHERE assessmentId = :assessmentId")
    suspend fun delete(assessmentId: String)


    @Query(
        """
        SELECT 
            a.assessmentId AS assessmentId,
            a.timestamp AS assessmentCreatedAtEpochMillis,
            CASE WHEN a.calibrationFactor IS NULL OR a.calibrationFactor <= 0 THEN 'NEEDS_CALIBRATION' ELSE 'CALIBRATED' END AS assessmentStatus,
            m.pixelArea AS woundAreaPixels,
            m.areaCm2 AS woundAreaCm2,
            m.createdAtMillis AS measuredAtEpochMillis
        FROM assessments a
        LEFT JOIN measurements m ON m.assessmentId = a.assessmentId
        WHERE a.timestamp BETWEEN :startEpochMillis AND :endEpochMillis
        ORDER BY a.timestamp DESC
        """
    )
    suspend fun getAssessmentsWithMeasurementsForRange(
        startEpochMillis: Long,
        endEpochMillis: Long
    ): List<ExportAssessmentMeasurementRow>
}
