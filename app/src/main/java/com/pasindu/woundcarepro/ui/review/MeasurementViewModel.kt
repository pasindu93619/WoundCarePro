package com.pasindu.woundcarepro.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.MeasurementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeasurementViewModel(
    private val assessmentRepository: AssessmentRepository,
    private val measurementRepository: MeasurementRepository
) : ViewModel() {
    private val _areaPixels = MutableStateFlow<Double?>(null)
    val areaPixels: StateFlow<Double?> = _areaPixels

    private val _areaCm2 = MutableStateFlow<Double?>(null)
    val areaCm2: StateFlow<Double?> = _areaCm2

    fun loadMeasurement(assessmentId: String) {
        viewModelScope.launch {
            val measurement = measurementRepository.getByAssessmentId(assessmentId)
            if (measurement != null) {
                _areaPixels.value = measurement.areaPixels
                _areaCm2.value = measurement.areaCm2
            } else {
                val assessment = assessmentRepository.getById(assessmentId)
                _areaPixels.value = assessment?.pixelArea
                _areaCm2.value = null
            }
        }
    }
}

class MeasurementViewModelFactory(
    private val assessmentRepository: AssessmentRepository,
    private val measurementRepository: MeasurementRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MeasurementViewModel(assessmentRepository, measurementRepository) as T
    }
}
