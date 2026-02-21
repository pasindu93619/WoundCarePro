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
    indices = [
        Index(value = ["assessmentId"], unique = true),
        Index(value = ["createdAtMillis"])
    ]
)
data class Measurement(
    @PrimaryKey
    val measurementId: String,
    val assessmentId: String,
    val createdAtMillis: Long,
    val pixelArea: Double,
    val areaCm2: Double?,
    val outlineJson: String?
)
