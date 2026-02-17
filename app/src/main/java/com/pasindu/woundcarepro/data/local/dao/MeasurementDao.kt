package com.pasindu.woundcarepro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pasindu.woundcarepro.data.local.entity.Measurement

@Dao
interface MeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: Measurement)

    @Query("SELECT * FROM measurements WHERE measurementId = :measurementId LIMIT 1")
    suspend fun getById(measurementId: String): Measurement?

    @Query("SELECT * FROM measurements ORDER BY createdAt DESC")
    suspend fun getAll(): List<Measurement>

    @Delete
    suspend fun delete(measurement: Measurement)
}
