package com.pasindu.woundcarepro.data.local.dao

data class ExportAssessmentMeasurementRow(
    val assessmentId: String,
    val assessmentCreatedAtEpochMillis: Long,
    val assessmentStatus: String,
    val woundAreaPixels: Double?,
    val woundAreaCm2: Double?,
    val measuredAtEpochMillis: Long?
)
