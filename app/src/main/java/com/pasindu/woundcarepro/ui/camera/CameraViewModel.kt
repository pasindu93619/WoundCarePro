package com.pasindu.woundcarepro.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.UUID
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
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
