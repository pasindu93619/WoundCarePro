package com.pasindu.woundcarepro.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val assessmentRepository: AssessmentRepository
) : ViewModel() {
    private val _assessment = MutableStateFlow<Assessment?>(null)
    val assessment: StateFlow<Assessment?> = _assessment

    fun loadAssessment(assessmentId: String) {
        viewModelScope.launch {
            _assessment.value = assessmentRepository.getById(assessmentId)
        }
    }

    fun updateOutline(assessmentId: String, outlineJson: String, onUpdated: () -> Unit = {}) {
        viewModelScope.launch {
            val current = assessmentRepository.getById(assessmentId) ?: return@launch
            assessmentRepository.upsert(current.copy(outlineJson = outlineJson))
            _assessment.value = current.copy(outlineJson = outlineJson)
            onUpdated()
        }
    }
}

class ReviewViewModelFactory(
    private val assessmentRepository: AssessmentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ReviewViewModel(assessmentRepository) as T
    }
}
