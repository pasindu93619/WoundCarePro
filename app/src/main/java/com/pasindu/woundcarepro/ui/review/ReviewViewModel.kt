package com.pasindu.woundcarepro.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.measurement.OutlineJsonConverter
import com.pasindu.woundcarepro.measurement.OutlinePoint
import com.pasindu.woundcarepro.measurement.PolygonAreaCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReviewUiState(
    val assessment: Assessment? = null,
    val points: List<OutlinePoint> = emptyList(),
    val pixelArea: Double? = null
)

class ReviewViewModel(
    private val assessmentRepository: AssessmentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState

    fun loadAssessment(assessmentId: String) {
        viewModelScope.launch {
            val assessment = assessmentRepository.getById(assessmentId)
            _uiState.value = _uiState.value.copy(
                assessment = assessment,
                points = OutlineJsonConverter.fromJson(assessment?.outlineJson).let { saved ->
                    if (saved.size > 1 && saved.first() == saved.last()) saved.dropLast(1) else saved
                },
                pixelArea = assessment?.pixelArea
            )
        }
    }

    fun addPoint(point: OutlinePoint) {
        _uiState.value = _uiState.value.copy(
            points = _uiState.value.points + point,
            pixelArea = null
        )
    }

    fun undoLastPoint() {
        val currentPoints = _uiState.value.points
        if (currentPoints.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            points = currentPoints.dropLast(1),
            pixelArea = null
        )
    }

    fun clearPoints() {
        _uiState.value = _uiState.value.copy(points = emptyList(), pixelArea = null)
    }

    fun saveOutline(assessmentId: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val current = assessmentRepository.getById(assessmentId) ?: return@launch
            val points = _uiState.value.points
            if (points.size < 3) return@launch

            val closedPoints = if (points.first() == points.last()) points else points + points.first()
            val pixelArea = PolygonAreaCalculator.calculateAreaPixels(closedPoints)
            val outlineJson = OutlineJsonConverter.toJson(closedPoints)
            val updated = current.copy(outlineJson = outlineJson, pixelArea = pixelArea)


            assessmentRepository.upsert(updated)
            _uiState.value = _uiState.value.copy(assessment = updated, pixelArea = pixelArea)
            onSaved()
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
