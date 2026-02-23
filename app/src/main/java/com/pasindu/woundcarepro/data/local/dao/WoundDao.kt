package com.pasindu.woundcarepro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pasindu.woundcarepro.data.local.entity.Wound

@Dao
interface WoundDao {
    @Upsert
    suspend fun upsert(wound: Wound)

    @Query("SELECT * FROM wounds WHERE woundId = :woundId LIMIT 1")
    suspend fun getById(woundId: String): Wound?

    @Query("SELECT EXISTS(SELECT 1 FROM wounds WHERE woundId = :woundId)")
    suspend fun exists(woundId: String): Boolean

    @Query("SELECT * FROM wounds WHERE patientId = :patientId ORDER BY createdAtMillis DESC")
    suspend fun listByPatient(patientId: String): List<Wound>
}
