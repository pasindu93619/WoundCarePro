package com.pasindu.woundcarepro.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "patients",
    indices = [Index(value = ["name"])]
)
data class Patient(
    @PrimaryKey
    val patientId: String,
    val name: String,
    val createdAt: Long
)
