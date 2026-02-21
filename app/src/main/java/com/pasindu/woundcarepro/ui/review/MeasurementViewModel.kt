package com.pasindu.woundcarepro.ui.review

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

class MeasurementViewModel(
    private val assessmentRepository: AssessmentRepository,
    private val measurementRepository: MeasurementRepository
) : ViewModel() {

    private val _assessment = MutableStateFlow<Assessment?>(null)
    val assessment: StateFlow<Assessment?> = _assessment

    fun loadMeasurement(assessmentId: String) {
        viewModelScope.launch {
            val assessment = assessmentRepository.getById(assessmentId) ?: return@launch
            _assessment.value = assessment
            val points = OutlineJsonConverter.fromJson(assessment.polygonPointsJson ?: assessment.outlineJson)
            val areaPixels = assessment.pixelArea ?: PolygonAreaCalculator.calculateAreaPixels(points)
            val areaCm2 = assessment.calibrationFactor?.let { areaPixels * it }
            _measurementComputation.value = MeasurementComputation(areaPixels = areaPixels, areaCm2 = areaCm2)
        }
    }

    fun loadMeasurement(assessmentId: String) {
        viewModelScope.launch {
            val measurement = measurementRepository.getByAssessmentId(assessmentId)
            if (measurement != null) {
                _areaPixels.value = measurement.pixelArea
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
        @Suppress("UNCHECKED_CAST")
        return MeasurementViewModel(assessmentRepository, measurementRepository) as T
    }
}