package com.pasindu.woundcarepro.ui.review

import android.graphics.PointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.SaveOutlineResult
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
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false,
    val needsCalibration: Boolean = false
)

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
        _uiState.value = _uiState.value.copy(saveError = null, saveSuccess = false, needsCalibration = false)
    }

    fun saveOutline(assessmentId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.isPolygonClosed || state.points.size < 3) return@launch
            _uiState.value = state.copy(isSaving = true, saveError = null, saveSuccess = false)

            val outlineJson = OutlineJsonConverter.toJson(com.pasindu.woundcarepro.measurement.WoundOutline(state.points))
            when (val result = assessmentRepository.saveOutlineAndMeasurement(
                assessmentId = assessmentId,
                outlineJson = outlineJson,
                pixelArea = PolygonAreaCalculator.calculateAreaPixels(state.points)
            )) {
                is SaveOutlineResult.Success -> {
                    _assessment.value = result.assessment
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = true,
                        pixelArea = result.measurement.pixelArea,
                        needsCalibration = result.measurement.areaCm2 == null
                    )
                }
                is SaveOutlineResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, saveError = result.message)
                }
            }
        }
    }
}
