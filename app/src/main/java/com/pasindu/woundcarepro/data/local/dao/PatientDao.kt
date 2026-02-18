package com.pasindu.woundcarepro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pasindu.woundcarepro.data.local.entity.Patient

@Dao
interface PatientDao {
    @Upsert
    suspend fun upsert(patient: Patient)

    @Query("SELECT * FROM patients WHERE patientId = :patientId LIMIT 1")
    suspend fun getById(patientId: String): Patient?

    @Query("SELECT * FROM patients ORDER BY createdAt DESC")
    suspend fun listRecent(): List<Patient>

    @Query("DELETE FROM patients WHERE patientId = :patientId")
    suspend fun delete(patientId: String)
}
