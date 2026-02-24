package com.pasindu.woundcarepro.data.local.repository

import androidx.room.withTransaction
import com.pasindu.woundcarepro.data.local.dao.AssessmentDao
import com.pasindu.woundcarepro.data.local.WoundCareDatabase
import com.pasindu.woundcarepro.data.local.dao.MeasurementDao
import com.pasindu.woundcarepro.data.local.dao.PatientDao
import com.pasindu.woundcarepro.data.local.dao.WoundDao
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.entity.Measurement
import com.pasindu.woundcarepro.data.local.entity.Patient
import com.pasindu.woundcarepro.data.local.entity.Wound
import android.database.sqlite.SQLiteConstraintException
import java.util.UUID

sealed interface SaveOutlineResult {
    data class Success(val assessment: Assessment, val measurement: Measurement) : SaveOutlineResult
    data class Error(val message: String) : SaveOutlineResult
}

interface AssessmentRepository {
    suspend fun upsert(assessment: Assessment): Result<Unit>
    suspend fun createAssessment(selectedPatientId: String? = null): Assessment
    suspend fun getById(assessmentId: String): Assessment?
    suspend fun listByPatient(patientId: String): List<Assessment>
    suspend fun listRecent(): List<Assessment>
    suspend fun updateGuidanceMetrics(assessmentId: String, guidanceMetricsJson: String)
    suspend fun updateCaptureMetadata(assessmentId: String, imagePath: String, guidanceMetricsJson: String)
    suspend fun updateMarkerCalibration(
        assessmentId: String,
        rectifiedImagePath: String,
        markerCornersJson: String,
        homographyJson: String,
        calibrationFactor: Double
    ): Assessment?
    suspend fun delete(assessmentId: String)
    suspend fun saveOutlineAndMeasurement(
        assessmentId: String,
        outlineJson: String,
        pixelArea: Double
    ): SaveOutlineResult
    suspend fun saveFinalOutlineToAssessment(
        assessmentId: String,
        finalOutlineJson: String,
        finalPolygonPointsJson: String,
        finalPixelArea: Double,
        finalAreaCm2: Double,
        finalPerimeterPx: Double
    )
}

class AssessmentRepositoryImpl(
    private val database: WoundCareDatabase,
    private val assessmentDao: AssessmentDao,
    private val patientDao: PatientDao,
    private val woundDao: WoundDao = database.woundDao(),
    private val measurementDao: MeasurementDao,
    private val auditRepository: AuditRepository
) : AssessmentRepository {
    override suspend fun upsert(assessment: Assessment): Result<Unit> {
        return runCatching {
            database.withTransaction {
                val safeAssessment = ensureParentRows(assessment)
                assessmentDao.upsert(safeAssessment)
            }
        }
    }

    override suspend fun createAssessment(selectedPatientId: String?): Assessment {
        val now = System.currentTimeMillis()
        val patientId = selectedPatientId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

        return database.withTransaction {
            val woundId = "wound-$patientId"
            val safeAssessment = ensureParentRows(
                Assessment(
                    assessmentId = UUID.randomUUID().toString(),
                    patientId = patientId,
                    woundId = woundId,
                    timestamp = now,
                    imagePath = null,
                    outlineJson = null,
                    polygonPointsJson = null,
                    pixelArea = null,
                    calibrationFactor = null,
                    guidanceMetricsJson = null
                )
            )
            try {
                assessmentDao.upsert(safeAssessment)
            } catch (error: SQLiteConstraintException) {
                throw IllegalStateException("Unable to create assessment because related patient or wound is missing.", error)
            }
            safeAssessment
        }
    }


    private suspend fun ensureParentRows(assessment: Assessment): Assessment {
        val now = System.currentTimeMillis()
        val patientId = assessment.patientId.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val woundId = assessment.woundId.takeIf { it.isNotBlank() } ?: "wound-$patientId"

        if (!patientDao.exists(patientId)) {
            patientDao.upsert(
                Patient(
                    patientId = patientId,
                    name = "Unknown",
                    createdAt = now
                )
            )
        }

        val wound = woundDao.getById(woundId)
        when {
            wound == null -> woundDao.upsert(
                Wound(
                    woundId = woundId,
                    patientId = patientId,
                    location = "Unspecified",
                    createdAtMillis = now
                )
            )
            wound.patientId != patientId -> woundDao.upsert(
                wound.copy(patientId = patientId)
            )
        }

        return assessment.copy(patientId = patientId, woundId = woundId)
    }

    override suspend fun getById(assessmentId: String): Assessment? = assessmentDao.getById(assessmentId)

    override suspend fun listByPatient(patientId: String): List<Assessment> = assessmentDao.listByPatient(patientId)

    override suspend fun listRecent(): List<Assessment> = assessmentDao.listRecent()

    override suspend fun updateGuidanceMetrics(assessmentId: String, guidanceMetricsJson: String) {
        assessmentDao.updateGuidanceMetrics(assessmentId, guidanceMetricsJson)
    }

    override suspend fun updateCaptureMetadata(
        assessmentId: String,
        imagePath: String,
        guidanceMetricsJson: String
    ) {
        assessmentDao.updateCaptureMetadata(assessmentId, imagePath, guidanceMetricsJson)
        val assessment = assessmentDao.getById(assessmentId)
        auditRepository.logAudit(
            action = "CAPTURE_PHOTO",
            patientId = assessment?.patientId,
            assessmentId = assessmentId,
            metadataJson = guidanceMetricsJson
        )
    }

    override suspend fun updateMarkerCalibration(
        assessmentId: String,
        rectifiedImagePath: String,
        markerCornersJson: String,
        homographyJson: String,
        calibrationFactor: Double
    ): Assessment? {
        return database.withTransaction {
            val current = assessmentDao.getById(assessmentId) ?: return@withTransaction null
            val updated = ensureParentRows(
                current.copy(
                    rectifiedImagePath = rectifiedImagePath,
                    calibrationFactor = calibrationFactor
                )
            )
            assessmentDao.upsert(updated)
            val measurement = measurementDao.getByAssessmentId(assessmentId)
            if (measurement != null) {
                val areaCm2 = measurement.pixelArea * calibrationFactor * calibrationFactor
                measurementDao.upsert(measurement.copy(areaCm2 = areaCm2, createdAtMillis = System.currentTimeMillis()))
            }
            updated
        }
    }

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

            val updatedAssessment = ensureParentRows(assessment.copy(outlineJson = outlineJson, pixelArea = pixelArea))
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
            auditRepository.logAudit(
                action = "SAVE_OUTLINE",
                patientId = assessment.patientId,
                assessmentId = assessmentId,
                metadataJson = "{\"pixelArea\":$pixelArea,\"areaCm2\":${areaCm2 ?: "null"}}"
            )
            SaveOutlineResult.Success(updatedAssessment, measurement)
        }
    }

    override suspend fun saveFinalOutlineToAssessment(
        assessmentId: String,
        finalOutlineJson: String,
        finalPolygonPointsJson: String,
        finalPixelArea: Double,
        finalAreaCm2: Double,
        finalPerimeterPx: Double
    ) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val assessment = assessmentDao.getById(assessmentId) ?: return@withTransaction
            val finalSavedAtMillis = assessment.finalSavedAtMillis ?: now

            assessmentDao.updateFinalOutlineAndMetrics(
                assessmentId = assessmentId,
                finalOutlineJson = finalOutlineJson,
                finalPolygonPointsJson = finalPolygonPointsJson,
                finalPixelArea = finalPixelArea,
                finalAreaCm2 = finalAreaCm2,
                finalPerimeterPx = finalPerimeterPx,
                finalSavedAtMillis = finalSavedAtMillis
            )

            val existingMeasurement = measurementDao.getByAssessmentId(assessmentId)
            val measurement = if (existingMeasurement != null) {
                existingMeasurement.copy(
                    createdAtMillis = finalSavedAtMillis,
                    pixelArea = finalPixelArea,
                    areaCm2 = finalAreaCm2,
                    outlineJson = finalOutlineJson
                )
            } else {
                Measurement(
                    measurementId = UUID.randomUUID().toString(),
                    assessmentId = assessmentId,
                    createdAtMillis = finalSavedAtMillis,
                    pixelArea = finalPixelArea,
                    areaCm2 = finalAreaCm2,
                    outlineJson = finalOutlineJson
                )
            }
            measurementDao.upsert(measurement)

            auditRepository.logAudit(
                action = "SAVE_FINAL_OUTLINE",
                patientId = assessment.patientId,
                assessmentId = assessmentId,
                metadataJson = "{\"finalPixelArea\":$finalPixelArea,\"finalAreaCm2\":$finalAreaCm2,\"finalPerimeterPx\":$finalPerimeterPx}"
            )
        }
    }
}
