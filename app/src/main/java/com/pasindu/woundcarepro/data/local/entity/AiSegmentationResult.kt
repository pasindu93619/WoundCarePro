package com.pasindu.woundcarepro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "ai_segmentation_results",
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
        Index(value = ["createdAt"])
    ]
)
data class AiSegmentationResult(
    @PrimaryKey
    val resultId: String = UUID.randomUUID().toString(),
    val assessmentId: String,
    val modelVersion: String,
    val autoOutlineJson: String,
    val maskPngPath: String?,
    val confidence: Float?,
    val processingTimeMs: Long,
    val createdAt: Long
)
