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
        onCreated: (String, String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                assessmentRepository.createAssessment(selectedPatientId)
            }.onSuccess { assessment ->
                onCreated(assessment.assessmentId, assessment.patientId)
            }.onFailure { error ->
                onError(error.message ?: "Unable to create assessment")
            }
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
