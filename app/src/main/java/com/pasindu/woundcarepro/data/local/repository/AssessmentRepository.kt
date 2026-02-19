package com.pasindu.woundcarepro.data.local.repository

import androidx.room.withTransaction
import com.pasindu.woundcarepro.data.local.WoundCareDatabase
import com.pasindu.woundcarepro.data.local.dao.AssessmentDao
import com.pasindu.woundcarepro.data.local.dao.MeasurementDao
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.entity.Measurement
import java.util.UUID

sealed interface SaveOutlineResult {
    data class Success(val assessment: Assessment, val measurement: Measurement) : SaveOutlineResult
    data class Error(val message: String) : SaveOutlineResult
}

interface AssessmentRepository {
    suspend fun upsert(assessment: Assessment)
    suspend fun getById(assessmentId: String): Assessment?
    suspend fun listByPatient(patientId: String): List<Assessment>
    suspend fun listRecent(): List<Assessment>
    suspend fun delete(assessmentId: String)
    suspend fun saveOutlineAndMeasurement(
        assessmentId: String,
        outlineJson: String,
        pixelArea: Double
    ): SaveOutlineResult
}

class AssessmentRepositoryImpl(
    private val database: WoundCareDatabase,
    private val assessmentDao: AssessmentDao,
    private val measurementDao: MeasurementDao
) : AssessmentRepository {
    override suspend fun upsert(assessment: Assessment) = assessmentDao.upsert(assessment)

    override suspend fun getById(assessmentId: String): Assessment? = assessmentDao.getById(assessmentId)

    override suspend fun listByPatient(patientId: String): List<Assessment> = assessmentDao.listByPatient(patientId)

    override suspend fun listRecent(): List<Assessment> = assessmentDao.listRecent()

    override suspend fun delete(assessmentId: String) = assessmentDao.delete(assessmentId)

    override suspend fun saveOutlineAndMeasurement(
        assessmentId: String,
        outlineJson: String,
        pixelArea: Double
    ): SaveOutlineResult {
        val assessment = assessmentDao.getById(assessmentId)
            ?: return SaveOutlineResult.Error("Assessment not found")

        return database.withTransaction {
            val areaCm2 = assessment.calibrationFactor
                ?.takeIf { it > 0.0 }
                ?.let { pixelArea * (it * it) }

            val updatedAssessment = assessment.copy(outlineJson = outlineJson, pixelArea = pixelArea)
            assessmentDao.upsert(updatedAssessment)

            val existingMeasurement = measurementDao.getByAssessmentId(assessmentId)
            val measurement = Measurement(
                measurementId = existingMeasurement?.measurementId ?: UUID.randomUUID().toString(),
                assessmentId = assessmentId,
                createdAtMillis = System.currentTimeMillis(),
                pixelArea = pixelArea,
                areaCm2 = areaCm2,
                outlineJson = outlineJson
            )
            measurementDao.upsert(measurement)
            SaveOutlineResult.Success(updatedAssessment, measurement)
        }
    }
}
