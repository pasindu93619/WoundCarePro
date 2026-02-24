package com.pasindu.woundcarepro.ui.review

import android.graphics.PointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.measurement.OutlineJsonConverter
import com.pasindu.woundcarepro.measurement.PolygonAreaCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReviewUiState(
    val points: List<PointF> = emptyList(),
    val isPolygonClosed: Boolean = false,
    val pixelArea: Double? = null,
    val saveState: FinalOutlineSaveState = FinalOutlineSaveState.Idle,
    val needsCalibration: Boolean = false
)

sealed interface FinalOutlineSaveState {
    data object Idle : FinalOutlineSaveState
    data object Saving : FinalOutlineSaveState
    data object Saved : FinalOutlineSaveState
    data class Error(val message: String) : FinalOutlineSaveState
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
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
            val outline = OutlineJsonConverter.fromJson(loaded?.polygonPointsJson ?: loaded?.outlineJson)
            val points = outline.points
            _uiState.value = ReviewUiState(
                points = points,
                isPolygonClosed = points.size >= 3,
                pixelArea = loaded?.pixelArea
            )
        }
    }

    fun addPoint(point: PointF) {
        val current = _uiState.value
        if (current.isPolygonClosed) return
        _uiState.value = current.copy(points = current.points + point)
    }

    fun closePolygon() {
        val current = _uiState.value
        if (current.points.size < 3) return
        _uiState.value = current.copy(
            isPolygonClosed = true,
            pixelArea = PolygonAreaCalculator.calculateAreaPixels(current.points)
        )
    }

    fun undoLastPoint() {
        val current = _uiState.value
        if (current.points.isEmpty()) return
        _uiState.value = current.copy(points = current.points.dropLast(1), isPolygonClosed = false, pixelArea = null)
    }

    fun clearPoints() {
        _uiState.value = ReviewUiState()
    }

    fun clearTransientState() {
        _uiState.value = _uiState.value.copy(saveState = FinalOutlineSaveState.Idle, needsCalibration = false)
    }

    fun saveFinalOutline(assessmentId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.isPolygonClosed || state.points.size < 3) return@launch
            _uiState.value = state.copy(saveState = FinalOutlineSaveState.Saving, needsCalibration = false)

            val pixelArea = state.pixelArea ?: PolygonAreaCalculator.calculateAreaPixels(state.points)
            val calibrationFactor = _assessment.value?.calibrationFactor
            if (calibrationFactor == null || calibrationFactor <= 0.0) {
                _uiState.value = _uiState.value.copy(
                    saveState = FinalOutlineSaveState.Error("Calibration is required before saving final outline."),
                    needsCalibration = true
                )
                return@launch
            }

            val outlineJson = OutlineJsonConverter.toJson(com.pasindu.woundcarepro.measurement.WoundOutline(state.points))
            val areaCm2 = pixelArea * calibrationFactor * calibrationFactor
            val perimeterPx = PolygonAreaCalculator.calculatePerimeterPixels(state.points)

            val saveResult = assessmentRepository.saveFinalOutlineToAssessment(
                assessmentId = assessmentId,
                finalOutlineJson = outlineJson,
                finalPolygonPointsJson = outlineJson,
                finalPixelArea = pixelArea,
                finalAreaCm2 = areaCm2,
                finalPerimeterPx = perimeterPx
            )

            saveResult.onSuccess {
                _assessment.value = assessmentRepository.getById(assessmentId)
                _uiState.value = _uiState.value.copy(
                    saveState = FinalOutlineSaveState.Saved,
                    pixelArea = pixelArea
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    saveState = FinalOutlineSaveState.Error(error.message ?: "Unable to save final outline")
                )
            }
        }
    }
}
