package com.pasindu.woundcarepro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pasindu.woundcarepro.data.local.dao.AssessmentDao
import com.pasindu.woundcarepro.data.local.dao.MeasurementDao
import com.pasindu.woundcarepro.data.local.dao.PatientDao
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.entity.Measurement
import com.pasindu.woundcarepro.data.local.entity.Patient
import com.pasindu.woundcarepro.data.local.entity.Wound

@Database(
    entities = [
        Patient::class,
        Wound::class,
        Assessment::class,
        Measurement::class
    ],
    version = 13,
    exportSchema = false
)
abstract class WoundCareDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun measurementDao(): MeasurementDao
}
