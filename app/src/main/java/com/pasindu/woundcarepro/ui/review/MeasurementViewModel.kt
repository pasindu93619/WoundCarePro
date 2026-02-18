package com.pasindu.woundcarepro.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Measurement
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.MeasurementRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeasurementViewModel(
    private val assessmentRepository: AssessmentRepository,
    private val measurementRepository: MeasurementRepository
) : ViewModel() {
    private val _calibrationFactor = MutableStateFlow<Double?>(null)
    val calibrationFactor: StateFlow<Double?> = _calibrationFactor

    fun loadCalibration(assessmentId: String) {
        viewModelScope.launch {
            _calibrationFactor.value = assessmentRepository.getById(assessmentId)?.calibrationFactor
        }
    }

    fun computeAreaCm2(areaPixels: Double): Double? {
        val factor = _calibrationFactor.value ?: return null
        return areaPixels * factor * factor
    }

    fun saveMeasurement(assessmentId: String, areaPixels: Double, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val areaCm2 = computeAreaCm2(areaPixels) ?: return@launch
            measurementRepository.upsert(
                Measurement(
                    measurementId = UUID.randomUUID().toString(),
                    assessmentId = assessmentId,
                    areaPixels = areaPixels,
                    areaCm2 = areaCm2,
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
