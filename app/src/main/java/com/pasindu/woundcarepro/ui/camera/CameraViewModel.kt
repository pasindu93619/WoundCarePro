package com.pasindu.woundcarepro.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import java.util.UUID
import kotlinx.coroutines.launch

class CameraViewModel(
    private val assessmentRepository: AssessmentRepository
) : ViewModel() {

    fun createAssessment(
        patientId: String = "anonymous",
        onCreated: (String) -> Unit
    ) {
        val id = UUID.randomUUID().toString()
        viewModelScope.launch {
            assessmentRepository.upsert(
                Assessment(
                    assessmentId = id,
                    patientId = patientId,
                    timestamp = System.currentTimeMillis(),
                    imagePath = null,
                    outlineJson = null,
                    calibrationFactor = null
                )
            )
            onCreated(id)
        }
    }

    fun saveImagePath(assessmentId: String, imagePath: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val current = assessmentRepository.getById(assessmentId) ?: return@launch
            assessmentRepository.upsert(current.copy(imagePath = imagePath))
            onSaved()
        }
    }
}

class CameraViewModelFactory(
    private val assessmentRepository: AssessmentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CameraViewModel(assessmentRepository) as T
    }
}
