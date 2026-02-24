package com.pasindu.woundcarepro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pasindu.woundcarepro.data.local.dao.AiSegmentationResultDao
import com.pasindu.woundcarepro.data.local.dao.AssessmentDao
import com.pasindu.woundcarepro.data.local.dao.AuditLogDao
import com.pasindu.woundcarepro.data.local.dao.ConsentDao
import com.pasindu.woundcarepro.data.local.dao.MeasurementDao
import com.pasindu.woundcarepro.data.local.dao.PatientDao
import com.pasindu.woundcarepro.data.local.dao.WoundDao
import com.pasindu.woundcarepro.data.local.entity.AiSegmentationResult
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.entity.AuditLog
import com.pasindu.woundcarepro.data.local.entity.Consent
import com.pasindu.woundcarepro.data.local.entity.Measurement
import com.pasindu.woundcarepro.data.local.entity.Patient
import com.pasindu.woundcarepro.data.local.entity.Wound

@Database(
    entities = [
        Patient::class,
        Wound::class,
        Assessment::class,
        Measurement::class,
        AiSegmentationResult::class,
        Consent::class,
        AuditLog::class
    ],
    version = 17,
    exportSchema = true
)
abstract class WoundCareDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun woundDao(): WoundDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun aiSegmentationResultDao(): AiSegmentationResultDao
    abstract fun consentDao(): ConsentDao
    abstract fun auditLogDao(): AuditLogDao
}
