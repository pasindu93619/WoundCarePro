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
    val pixelArea: Double? = null
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
            val points = OutlineJsonConverter.fromJson(loaded?.polygonPointsJson ?: loaded?.outlineJson)
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
        val updated = current.points + point
        _uiState.value = current.copy(points = updated)
    }

    fun closePolygon() {
        val current = _uiState.value
        if (current.points.size < 3) return
        val area = PolygonAreaCalculator.calculateAreaPixels(current.points)
        _uiState.value = current.copy(isPolygonClosed = true, pixelArea = area)
    }

    fun undoLastPoint() {
        if (_uiState.value.points.isEmpty()) return
        val updatedPoints = _uiState.value.points.dropLast(1)
        _uiState.value = _uiState.value.copy(
            points = updatedPoints,
            isPolygonClosed = false,
            pixelArea = null
        )
    }

    fun clearPoints() {
        _uiState.value = ReviewUiState()
    }

    fun clearTransientState() {
        _uiState.value = _uiState.value.copy(saveError = null, saveSuccess = false, needsCalibration = false)
    }

    fun saveOutlineAndMeasurement(assessmentId: String) {
        viewModelScope.launch {
            val current = assessmentRepository.getById(assessmentId) ?: return@launch
            val state = _uiState.value
            if (!state.isPolygonClosed || state.points.size < 3) return@launch
            val points = state.points
            val area = PolygonAreaCalculator.calculateAreaPixels(points)
            val polygonPointsJson = OutlineJsonConverter.toJson(points)
            val updated = current.copy(
                outlineJson = polygonPointsJson,
                polygonPointsJson = polygonPointsJson,
                pixelArea = area
            )
            assessmentRepository.upsert(updated)
            _assessment.value = updated
            _uiState.value = _uiState.value.copy(pixelArea = area, isPolygonClosed = true)
            onSaved()
        }
    }
}
