package com.pasindu.woundcarepro.ui.review

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.woundcarepro.data.local.entity.Assessment
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.measurement.HomographySolver
import com.pasindu.woundcarepro.measurement.ImageWarp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot

data class MarkerCalibrationUiState(
    val points: List<PointF> = emptyList(),
    val markerSizeCm: Double = 5.0,
    val calibrationFactor: Double? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
)

class MarkerCalibrationViewModel(
    private val assessmentRepository: AssessmentRepository
) : ViewModel() {

    private val _assessment = MutableStateFlow<Assessment?>(null)
    val assessment: StateFlow<Assessment?> = _assessment.asStateFlow()

    private val _uiState = MutableStateFlow(MarkerCalibrationUiState())
    val uiState: StateFlow<MarkerCalibrationUiState> = _uiState.asStateFlow()

    fun loadAssessment(assessmentId: String) {
        viewModelScope.launch {
            _assessment.value = assessmentRepository.getById(assessmentId)
        }
    }

    fun addPoint(point: PointF) {
        if (_uiState.value.points.size >= 4) return
        val updatedPoints = _uiState.value.points + point
        _uiState.value = _uiState.value.copy(
            points = updatedPoints,
            calibrationFactor = deriveCalibrationFactor(updatedPoints, _uiState.value.markerSizeCm),
            error = null,
            done = false
        )
    }

    fun undoPoint() {
        if (_uiState.value.points.isEmpty()) return
        val updatedPoints = _uiState.value.points.dropLast(1)
        _uiState.value = _uiState.value.copy(
            points = updatedPoints,
            calibrationFactor = deriveCalibrationFactor(updatedPoints, _uiState.value.markerSizeCm)
        )
    }

    fun clearPoints() {
        _uiState.value = _uiState.value.copy(points = emptyList(), calibrationFactor = null)
    }

    fun setMarkerSizeCm(size: Double) {
        _uiState.value = _uiState.value.copy(
            markerSizeCm = size,
            calibrationFactor = deriveCalibrationFactor(_uiState.value.points, size)
        )
    }

    fun rectifyAndSave(context: Context, assessmentId: String) {
        viewModelScope.launch {
            val assessment = _assessment.value ?: return@launch
            val sourcePath = assessment.imagePath ?: run {
                _uiState.value = _uiState.value.copy(error = "Captured image not found")
                return@launch
            }
            val points = _uiState.value.points
            val factor = _uiState.value.calibrationFactor
            if (points.size != 4 || factor == null || factor <= 0.0) {
                _uiState.value = _uiState.value.copy(error = "Select all 4 points in order first")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isSaving = true, error = null, done = false)

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = BitmapFactory.decodeFile(sourcePath)
                        ?: error("Unable to decode captured image")
                    val destinationCorners = listOf(
                        PointF(0f, 0f),
                        PointF(999f, 0f),
                        PointF(999f, 999f),
                        PointF(0f, 999f)
                    )
                    val homography = HomographySolver.solve(points, destinationCorners)
                    val rectified = ImageWarp.warpWithInverseMapping(bitmap, homography, 1000, 1000)
                    val outputPath = saveRectified(context, assessmentId, rectified)
                    val updated = assessmentRepository.updateMarkerCalibration(
                        assessmentId = assessmentId,
                        rectifiedImagePath = outputPath,
                        markerCornersJson = pointsToJson(points),
                        homographyJson = matrixToJson(homography),
                        calibrationFactor = factor
                    ) ?: error("Assessment not found")
                    updated
                }
            }

            result.onSuccess {
                _assessment.value = it
                _uiState.value = _uiState.value.copy(isSaving = false, done = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, error = it.message ?: "Rectification failed")
            }
        }
    }

    fun clearTransient() {
        _uiState.value = _uiState.value.copy(error = null, done = false)
    }

    private fun deriveCalibrationFactor(points: List<PointF>, markerSizeCm: Double): Double? {
        if (points.size != 4 || markerSizeCm <= 0.0) return null
        val top = distance(points[0], points[1])
        val bottom = distance(points[3], points[2])
        val avg = (top + bottom) / 2.0
        if (avg <= 0.0) return null
        return markerSizeCm / avg
    }

    private fun distance(a: PointF, b: PointF): Double {
        return hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble())
    }

    private fun saveRectified(context: Context, assessmentId: String, bitmap: Bitmap): String {
        val dir = File(context.filesDir, "assessment_images/$assessmentId")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "RECTIFIED_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        }
        return file.absolutePath
    }

    private fun pointsToJson(points: List<PointF>): String {
        val arr = JSONArray()
        points.forEach {
            val pair = JSONArray()
            pair.put(it.x.toDouble())
            pair.put(it.y.toDouble())
            arr.put(pair)
        }
        return arr.toString()
    }

    private fun matrixToJson(matrix: DoubleArray): String {
        val arr = JSONArray()
        matrix.forEach { arr.put(it) }
        return arr.toString()
    }
}

class MarkerCalibrationViewModelFactory(
    private val assessmentRepository: AssessmentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MarkerCalibrationViewModel(assessmentRepository) as T
    }
}
