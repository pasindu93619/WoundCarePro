package com.pasindu.woundcarepro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey
    val patientId: String,
    val name: String,
    val hospitalId: String?,
    val createdAt: Long
)
