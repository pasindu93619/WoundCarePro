package com.pasindu.woundcarepro.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.entity.Measurement
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.MeasurementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HistoryItem(
    val assessment: Assessment,
    val latestMeasurement: Measurement?
)

class HistoryViewModel(
    private val assessmentRepository: AssessmentRepository,
    private val measurementRepository: MeasurementRepository
) : ViewModel() {
    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history

    fun loadRecentHistory() {
        viewModelScope.launch {
            val assessments = assessmentRepository.listRecent()
            _history.value = assessments.map { assessment ->
                HistoryItem(
                    assessment = assessment,
                    latestMeasurement = measurementRepository.getLatestByAssessmentId(assessment.assessmentId)
                )
            }
        }
    }
}

class HistoryViewModelFactory(
    private val assessmentRepository: AssessmentRepository,
    private val measurementRepository: MeasurementRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HistoryViewModel(assessmentRepository, measurementRepository) as T
    }
}
