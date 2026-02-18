package com.pasindu.woundcarepro.ui.review

import android.graphics.PointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.measurement.OutlineJsonConverter
<<<<<<< HEAD
import com.pasindu.woundcarepro.measurement.OutlinePoint
import com.pasindu.woundcarepro.measurement.PolygonAreaCalculator
=======
import com.pasindu.woundcarepro.measurement.PolygonAreaCalculator
import com.pasindu.woundcarepro.measurement.WoundOutline
>>>>>>> main
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReviewUiState(
<<<<<<< HEAD
    val assessment: Assessment? = null,
    val points: List<OutlinePoint> = emptyList(),
=======
    val points: List<PointF> = emptyList(),
>>>>>>> main
    val pixelArea: Double? = null
)

class ReviewViewModel(
    private val assessmentRepository: AssessmentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState

    fun loadAssessment(assessmentId: String) {
        viewModelScope.launch {
<<<<<<< HEAD
            val assessment = assessmentRepository.getById(assessmentId)
            _uiState.value = _uiState.value.copy(
                assessment = assessment,
                points = OutlineJsonConverter.fromJson(assessment?.outlineJson).let { saved ->
                    if (saved.size > 1 && saved.first() == saved.last()) saved.dropLast(1) else saved
                },
                pixelArea = assessment?.pixelArea
=======
            val loaded = assessmentRepository.getById(assessmentId)
            _assessment.value = loaded
            val outline = OutlineJsonConverter.fromJson(loaded?.outlineJson)
            _uiState.value = ReviewUiState(
                points = outline.points,
                pixelArea = loaded?.pixelArea
>>>>>>> main
            )
        }
    }

<<<<<<< HEAD
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
=======
    fun addPoint(point: PointF) {
        val updated = _uiState.value.points + point
        _uiState.value = _uiState.value.copy(points = updated, pixelArea = null)
    }

    fun undoLastPoint() {
        if (_uiState.value.points.isEmpty()) return
        _uiState.value = _uiState.value.copy(points = _uiState.value.points.dropLast(1), pixelArea = null)
>>>>>>> main
    }

    fun clearPoints() {
        _uiState.value = _uiState.value.copy(points = emptyList(), pixelArea = null)
    }

    fun saveOutline(assessmentId: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val current = assessmentRepository.getById(assessmentId) ?: return@launch
            val points = _uiState.value.points
            if (points.size < 3) return@launch
<<<<<<< HEAD

            val closedPoints = if (points.first() == points.last()) points else points + points.first()
            val pixelArea = PolygonAreaCalculator.calculateAreaPixels(closedPoints)
            val outlineJson = OutlineJsonConverter.toJson(closedPoints)
            val updated = current.copy(outlineJson = outlineJson, pixelArea = pixelArea)


            assessmentRepository.upsert(updated)
            _uiState.value = _uiState.value.copy(assessment = updated, pixelArea = pixelArea)
=======
            val area = PolygonAreaCalculator.calculateAreaPixels(points)
            val outlineJson = OutlineJsonConverter.toJson(WoundOutline(points = points))
            val updated = current.copy(outlineJson = outlineJson, pixelArea = area)
            assessmentRepository.upsert(updated)
            _assessment.value = updated
            _uiState.value = _uiState.value.copy(pixelArea = area)
>>>>>>> main
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
