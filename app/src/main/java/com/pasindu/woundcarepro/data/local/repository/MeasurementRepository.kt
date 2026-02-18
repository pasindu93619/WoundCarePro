package com.pasindu.woundcarepro.data.local.repository

import com.pasindu.woundcarepro.data.local.dao.MeasurementDao
import com.pasindu.woundcarepro.data.local.entity.Measurement

interface MeasurementRepository {
    suspend fun upsert(measurement: Measurement)
    suspend fun getById(measurementId: String): Measurement?
    suspend fun getLatestByAssessmentId(assessmentId: String): Measurement?
    suspend fun listByPatient(patientId: String): List<Measurement>
    suspend fun listRecent(): List<Measurement>
    suspend fun delete(measurementId: String)
}

class MeasurementRepositoryImpl(
    private val measurementDao: MeasurementDao
) : MeasurementRepository {
    override suspend fun upsert(measurement: Measurement) = measurementDao.upsert(measurement)

    override suspend fun getById(measurementId: String): Measurement? = measurementDao.getById(measurementId)

    override suspend fun getLatestByAssessmentId(assessmentId: String): Measurement? =
        measurementDao.getLatestByAssessmentId(assessmentId)

    override suspend fun listByPatient(patientId: String): List<Measurement> = measurementDao.listByPatient(patientId)

    override suspend fun listRecent(): List<Measurement> = measurementDao.listRecent()

    override suspend fun delete(measurementId: String) = measurementDao.delete(measurementId)
}
