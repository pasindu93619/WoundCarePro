package com.pasindu.woundcarepro.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["timestampMillis"]),
        Index(value = ["patientId"]),
        Index(value = ["assessmentId"]),
        Index(value = ["action"])
    ]
)
data class AuditLog(
    @PrimaryKey
    val auditId: String,
    val timestampMillis: Long,
    val action: String,
    val patientId: String?,
    val assessmentId: String?,
    val metadataJson: String?
)
