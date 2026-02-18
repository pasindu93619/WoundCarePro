package com.pasindu.woundcarepro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pasindu.woundcarepro.data.local.entity.Measurement

@Dao
interface MeasurementDao {
    @Upsert
    suspend fun upsert(measurement: Measurement)

    @Query("SELECT * FROM measurements WHERE measurementId = :measurementId LIMIT 1")
    suspend fun getById(measurementId: String): Measurement?

    @Query(
        """
        SELECT m.* FROM measurements m
        INNER JOIN assessments a ON a.assessmentId = m.assessmentId
        WHERE a.patientId = :patientId
        ORDER BY m.createdAt DESC
        """
    )
    suspend fun listByPatient(patientId: String): List<Measurement>

    @Query("SELECT * FROM measurements ORDER BY createdAt DESC")
    suspend fun listRecent(): List<Measurement>

    @Query("SELECT * FROM measurements WHERE assessmentId = :assessmentId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByAssessmentId(assessmentId: String): Measurement?

    @Query("DELETE FROM measurements WHERE measurementId = :measurementId")
    suspend fun delete(measurementId: String)
}
