package com.pasindu.woundcarepro.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.DEFAULT_PATIENT_ID
import com.pasindu.woundcarepro.data.local.DEFAULT_WOUND_ID
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val assessmentRepository: AssessmentRepository,
    private val auditRepository: AuditRepository
) : ViewModel() {

    fun createAssessment(
        patientId: String = DEFAULT_PATIENT_ID,
        onCreated: (String, String) -> Unit
    ) {
        val id = UUID.randomUUID().toString()
        viewModelScope.launch {
            assessmentRepository.upsert(
                Assessment(
                    assessmentId = id,
                    patientId = patientId,
                    woundId = DEFAULT_WOUND_ID,
                    timestamp = System.currentTimeMillis(),
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
                assessmentId = id,
                metadataJson = null
            )
            onCreated(id, patientId)
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
