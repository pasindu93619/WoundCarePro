package com.pasindu.woundcarepro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pasindu.woundcarepro.data.local.DEFAULT_PATIENT_ID

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
    val patientId: String = DEFAULT_PATIENT_ID,
    val timestamp: Long,
    val imagePath: String?,
    val outlineJson: String?,
    val pixelArea: Double?,
    val calibrationFactor: Double?,
    val woundLocation: String? = null,
    val guidanceMetricsJson: String? = null
)
