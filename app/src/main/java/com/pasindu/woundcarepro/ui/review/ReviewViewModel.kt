package com.pasindu.woundcarepro.ui.review

import android.graphics.PointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.SaveOutlineResult
import com.pasindu.woundcarepro.measurement.OutlineJsonConverter
import com.pasindu.woundcarepro.measurement.PolygonAreaCalculator
import com.pasindu.woundcarepro.measurement.WoundOutline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReviewUiState(
    val points: List<PointF> = emptyList(),
    val pixelArea: Double? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false,
    val needsCalibration: Boolean = false
)

class ReviewViewModel(
    private val assessmentRepository: AssessmentRepository
) : ViewModel() {
    private val _assessment = MutableStateFlow<Assessment?>(null)
    val assessment: StateFlow<Assessment?> = _assessment

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState

    fun loadAssessment(assessmentId: String) {
        viewModelScope.launch {
            val loaded = assessmentRepository.getById(assessmentId)
            _assessment.value = loaded
            val outline = OutlineJsonConverter.fromJson(loaded?.outlineJson)
            _uiState.value = ReviewUiState(
                points = outline.points,
                pixelArea = loaded?.pixelArea
            )
        }
    }

    fun addPoint(point: PointF) {
        val updated = _uiState.value.points + point
        _uiState.value = _uiState.value.copy(points = updated, pixelArea = null, saveSuccess = false)
    }

    fun undoLastPoint() {
        if (_uiState.value.points.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            points = _uiState.value.points.dropLast(1),
            pixelArea = null,
            saveSuccess = false
        )
    }

    fun clearPoints() {
        _uiState.value = _uiState.value.copy(points = emptyList(), pixelArea = null, saveSuccess = false)
    }

    fun clearTransientState() {
        _uiState.value = _uiState.value.copy(saveError = null, saveSuccess = false, needsCalibration = false)
    }

    fun saveOutline(assessmentId: String) {
        viewModelScope.launch {
            val points = _uiState.value.points
            if (points.size < 3) {
                _uiState.value = _uiState.value.copy(saveError = "Add at least 3 points before saving")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null, saveSuccess = false)

            val area = PolygonAreaCalculator.calculateAreaPixels(points)
            val outlineJson = OutlineJsonConverter.toJson(WoundOutline(points = points))

            when (val result = assessmentRepository.saveFinalOutlineAndMeasurement(assessmentId, outlineJson, area)) {
                is SaveOutlineResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, saveError = result.message)
                }

                is SaveOutlineResult.Success -> {
                    _assessment.value = result.assessment
                    _uiState.value = _uiState.value.copy(
                        pixelArea = area,
                        isSaving = false,
                        saveSuccess = true,
                        needsCalibration = result.measurement.areaCm2 == null
                    )
                }
            }
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
