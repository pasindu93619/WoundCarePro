package com.pasindu.woundcarepro.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import kotlinx.coroutines.launch

class CalibrationViewModel(
    private val assessmentRepository: AssessmentRepository
) : ViewModel() {

    fun saveCalibration(assessmentId: String, calibrationFactor: Double, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val current = assessmentRepository.getById(assessmentId) ?: return@launch
            assessmentRepository.upsert(current.copy(calibrationFactor = calibrationFactor))
            onSaved()
        }
    }
}

class CalibrationViewModelFactory(
    private val assessmentRepository: AssessmentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CalibrationViewModel(assessmentRepository) as T
    }
}
