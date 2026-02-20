package com.pasindu.woundcarepro.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.DEFAULT_WOUND_ID
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.entity.Patient
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.AuditRepository
import com.pasindu.woundcarepro.data.local.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val assessmentRepository: AssessmentRepository,
    private val patientRepository: PatientRepository,
    private val auditRepository: AuditRepository
) : ViewModel() {

    fun createAssessment(
        onCreated: (String, String) -> Unit
    ) {
        val patientId = UUID.randomUUID().toString()
        val assessmentId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            patientRepository.upsert(
                Patient(
                    patientId = patientId,
                    name = "Unknown",
                    createdAt = now
                )
            )
            assessmentRepository.upsert(
                Assessment(
                    assessmentId = assessmentId,
                    patientId = patientId,
                    woundId = DEFAULT_WOUND_ID,
                    timestamp = now,
                    imagePath = null,
                    outlineJson = null,
                    pixelArea = null,
                    calibrationFactor = null,
                    guidanceMetricsJson = null
                )
            )
            auditRepository.logAudit(
                action = "CREATE_ASSESSMENT",
                patientId = patientId,
                assessmentId = assessmentId,
                metadataJson = null
            )
            onCreated(assessmentId, patientId)
        }
    }

    fun saveCaptureMetadata(
        assessmentId: String,
        imagePath: String,
        guidanceMetricsJson: String,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            assessmentRepository.updateCaptureMetadata(
                assessmentId = assessmentId,
                imagePath = imagePath,
                guidanceMetricsJson = guidanceMetricsJson
            )
            onSaved()
        }
    }
}
