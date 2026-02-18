package com.pasindu.woundcarepro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assessments",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["patientId"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["timestamp"])
    ]
)
data class Assessment(
    @PrimaryKey
    val assessmentId: String,
    val patientId: String = "anonymous",
    val timestamp: Long,
    val imagePath: String?,
    val outlineJson: String?,
    val calibrationFactor: Double?
)
