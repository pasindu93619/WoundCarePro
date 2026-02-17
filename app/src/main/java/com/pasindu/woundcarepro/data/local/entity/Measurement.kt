package com.pasindu.woundcarepro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(
            entity = Assessment::class,
            parentColumns = ["assessmentId"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["assessmentId"])]
)
data class Measurement(
    @PrimaryKey
    val measurementId: String,
    val assessmentId: String,
    val areaPixels: Double,
    val areaCm2: Double?,
    val createdAt: Long
)
