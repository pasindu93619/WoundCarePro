package com.pasindu.woundcarepro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pasindu.woundcarepro.data.local.DEFAULT_PATIENT_ID
import com.pasindu.woundcarepro.data.local.DEFAULT_WOUND_ID

@Entity(
    tableName = "assessments",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["patientId"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.SET_DEFAULT
        ),
        ForeignKey(
            entity = Wound::class,
            parentColumns = ["woundId"],
            childColumns = ["woundId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["woundId"]),
        Index(value = ["timestamp"])
    ]
)
data class Assessment(
    @PrimaryKey
    val assessmentId: String,
    val patientId: String = DEFAULT_PATIENT_ID,
    val woundId: String = DEFAULT_WOUND_ID,
    val timestamp: Long,
    val imagePath: String?,
    val rectifiedImagePath: String? = null,
    val outlineJson: String?,
    val polygonPointsJson: String?,
    val finalOutlineJson: String? = null,
    val finalPolygonPointsJson: String? = null,
    val pixelArea: Double?,
    val finalPixelArea: Double? = null,
    val finalAreaCm2: Double? = null,
    val finalPerimeterPx: Double? = null,
    val finalSavedAtMillis: Long? = null,
    val calibrationFactor: Double?,
    val woundLocation: String? = null,
    val guidanceMetricsJson: String? = null
)
