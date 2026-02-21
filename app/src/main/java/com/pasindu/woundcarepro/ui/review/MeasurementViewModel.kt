package com.pasindu.woundcarepro.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.MeasurementRepository
import com.pasindu.woundcarepro.measurement.OutlineJsonConverter
import com.pasindu.woundcarepro.measurement.PolygonAreaCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MeasurementComputation(
    val areaPixels: Double,
    val areaCm2: Double?
)

class MeasurementViewModel(
    private val assessmentRepository: AssessmentRepository,
    private val measurementRepository: MeasurementRepository
) : ViewModel() {

    private val _assessment = MutableStateFlow<Assessment?>(null)
    val assessment: StateFlow<Assessment?> = _assessment

    private val _measurementComputation = MutableStateFlow<MeasurementComputation?>(null)
    val measurementComputation: StateFlow<MeasurementComputation?> = _measurementComputation

    fun loadMeasurement(assessmentId: String) {
        viewModelScope.launch {
            val assessment = assessmentRepository.getById(assessmentId) ?: return@launch
            _assessment.value = assessment

            val measurement = measurementRepository.getByAssessmentId(assessmentId)
            val areaPixels = measurement?.pixelArea
                ?: assessment.pixelArea
                ?: PolygonAreaCalculator.calculateAreaPixels(
                    OutlineJsonConverter.fromJson(assessment.polygonPointsJson ?: assessment.outlineJson).points
                )

            val areaCm2 = measurement?.areaCm2 ?: assessment.calibrationFactor?.let { areaPixels * it * it }
            _measurementComputation.value = MeasurementComputation(areaPixels, areaCm2)
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
