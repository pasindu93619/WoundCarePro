package com.pasindu.woundcarepro.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val assessmentRepository: AssessmentRepository,
    private val auditRepository: AuditRepository
) : ViewModel() {

    fun createAssessment(
        selectedPatientId: String? = null,
        onCreated: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            assessmentRepository.upsert(
                Assessment(
                    assessmentId = id,
                    patientId = patientId,
                    timestamp = System.currentTimeMillis(),
                    imagePath = null,
                    outlineJson = null,
                    polygonPointsJson = null,
                    pixelArea = null,
                    calibrationFactor = null
                )
            )
            onCreated(assessment.assessmentId, assessment.patientId)
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
