package com.pasindu.woundcarepro.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.AuditRepository
import com.pasindu.woundcarepro.data.local.repository.ConsentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ConsentUiState(
    val patientId: String? = null,
    val note: String = "",
    val blockedMessage: String? = null
)

class ConsentViewModel(
    private val assessmentRepository: AssessmentRepository,
    private val consentRepository: ConsentRepository,
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConsentUiState())
    val uiState: StateFlow<ConsentUiState> = _uiState

    fun load(assessmentId: String) {
        viewModelScope.launch {
            val patientId = assessmentRepository.getById(assessmentId)?.patientId
            _uiState.value = _uiState.value.copy(patientId = patientId)
        }
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun submit(assessmentId: String, granted: Boolean, onAllowed: () -> Unit) {
        viewModelScope.launch {
            val patientId = _uiState.value.patientId ?: return@launch
            consentRepository.recordConsent(
                patientId = patientId,
                consentType = "PHOTO",
                consentGiven = granted,
                note = _uiState.value.note.takeIf { it.isNotBlank() }
            )
            auditRepository.logAudit(
                action = "CONSENT_DECISION",
                patientId = patientId,
                assessmentId = assessmentId,
                metadataJson = "{\"consentType\":\"PHOTO\",\"consentGiven\":$granted}"
            )
            if (granted) {
                onAllowed()
            } else {
                _uiState.value = _uiState.value.copy(
                    blockedMessage = "Photo capture blocked: consent denied for wound photography and measurement."
                )
            }
        }
    }
}

class ConsentViewModelFactory(
    private val assessmentRepository: AssessmentRepository,
    private val consentRepository: ConsentRepository,
    private val auditRepository: AuditRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ConsentViewModel(assessmentRepository, consentRepository, auditRepository) as T
    }
}
