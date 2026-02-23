package com.pasindu.woundcarepro.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.AuditRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalibrationViewModel(
    private val assessmentRepository: AssessmentRepository,
    private val auditRepository: AuditRepository
) : ViewModel() {

    private val _assessment = MutableStateFlow<Assessment?>(null)
    val assessment: StateFlow<Assessment?> = _assessment.asStateFlow()

    fun loadAssessment(assessmentId: String) {
        viewModelScope.launch {
            _assessment.value = assessmentRepository.getById(assessmentId)
        }
    }

    fun saveCalibration(assessmentId: String, calibrationFactor: Double, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val current = assessmentRepository.getById(assessmentId) ?: return@launch
            val updated = current.copy(calibrationFactor = calibrationFactor)
            val result = assessmentRepository.upsert(updated)
            if (result.isFailure) return@launch
            auditRepository.logAudit(
                action = "SAVE_CALIBRATION",
                patientId = current.patientId,
                assessmentId = assessmentId,
                metadataJson = "{\"calibrationFactor\":$calibrationFactor}"
            )
            _assessment.value = updated
            onSaved()
        }
    }
}

class CalibrationViewModelFactory(
    private val assessmentRepository: AssessmentRepository,
    private val auditRepository: AuditRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CalibrationViewModel(assessmentRepository, auditRepository) as T
    }
}
