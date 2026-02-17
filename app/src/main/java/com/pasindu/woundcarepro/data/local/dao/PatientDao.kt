package com.pasindu.woundcarepro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pasindu.woundcarepro.data.local.entity.Patient

@Dao
interface PatientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: Patient)

    @Query("SELECT * FROM patients WHERE patientId = :patientId LIMIT 1")
    suspend fun getById(patientId: String): Patient?

    @Query("SELECT * FROM patients ORDER BY createdAt DESC")
    suspend fun getAll(): List<Patient>

    @Delete
    suspend fun delete(patient: Patient)
}
