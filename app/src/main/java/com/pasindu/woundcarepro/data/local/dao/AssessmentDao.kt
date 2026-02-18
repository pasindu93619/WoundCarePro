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

    @Query("DELETE FROM assessments WHERE assessmentId = :assessmentId")
    suspend fun delete(assessmentId: String)
}
