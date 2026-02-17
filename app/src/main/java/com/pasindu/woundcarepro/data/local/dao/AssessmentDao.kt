package com.pasindu.woundcarepro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pasindu.woundcarepro.data.local.entity.Assessment

@Dao
interface AssessmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assessment: Assessment)

    @Query("SELECT * FROM patient_assessments WHERE assessmentId = :assessmentId LIMIT 1")
    suspend fun getById(assessmentId: String): Assessment?

    @Query("SELECT * FROM patient_assessments ORDER BY timestamp DESC")
    suspend fun getAll(): List<Assessment>

    @Delete
    suspend fun delete(assessment: Assessment)
}
