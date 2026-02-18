package com.pasindu.woundcarepro.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Measurement
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.MeasurementRepository
import com.pasindu.woundcarepro.measurement.OutlineJsonConverter
import com.pasindu.woundcarepro.measurement.PolygonAreaCalculator
import java.util.UUID
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
    private val _measurementComputation = MutableStateFlow<MeasurementComputation?>(null)
    val measurementComputation: StateFlow<MeasurementComputation?> = _measurementComputation

    fun loadMeasurement(assessmentId: String) {
        viewModelScope.launch {
            val assessment = assessmentRepository.getById(assessmentId) ?: return@launch
            val points = OutlineJsonConverter.fromJson(assessment.outlineJson).points
            val areaPixels = PolygonAreaCalculator.calculateAreaPixels(points)
            val areaCm2 = assessment.calibrationFactor?.let { areaPixels * it }
            _measurementComputation.value = MeasurementComputation(areaPixels = areaPixels, areaCm2 = areaCm2)
        }
    }

    fun saveMeasurement(assessmentId: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val computed = _measurementComputation.value ?: return@launch
            measurementRepository.upsert(
                Measurement(
                    measurementId = UUID.randomUUID().toString(),
                    assessmentId = assessmentId,
                    areaPixels = computed.areaPixels,
                    areaCm2 = computed.areaCm2,
                    createdAt = System.currentTimeMillis()
                )
            )
            onSaved()
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
