package com.pasindu.woundcarepro.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.DEFAULT_PATIENT_ID
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
        // Temporary default patient until Patient selection screen is implemented.
        patientId: String = DEFAULT_PATIENT_ID,
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
                    pixelArea = null,
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
