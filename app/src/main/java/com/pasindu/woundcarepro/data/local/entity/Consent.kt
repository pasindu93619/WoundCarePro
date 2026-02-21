package com.pasindu.woundcarepro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "consents",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["patientId"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["timestampMillis"])
    ]
)
data class Consent(
    @PrimaryKey
    val consentId: String,
    val patientId: String,
    val timestampMillis: Long,
    val consentGiven: Boolean,
    val consentType: String,
    val note: String?
)
