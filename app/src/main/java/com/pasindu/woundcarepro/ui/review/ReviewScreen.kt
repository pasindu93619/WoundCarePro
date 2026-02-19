package com.pasindu.woundcarepro.ui.review

import android.graphics.BitmapFactory
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun ReviewScreen(
    assessmentId: String,
    viewModel: ReviewViewModel,
    onRetake: () -> Unit,
    onNextAfterSave: (needsCalibration: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val assessment by viewModel.assessment.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(assessmentId) {
        viewModel.loadAssessment(assessmentId)
    }

    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearTransientState()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNextAfterSave(uiState.needsCalibration)
            viewModel.clearTransientState()
        }
    }

    val statusMessage = if (assessment?.imagePath == null) {
        "No captured image found. Please retake."
    } else {
        "Tap on wound border to add polygon points"
    }

    val bitmap = assessment?.imagePath?.let { path ->
        BitmapFactory.decodeFile(path)?.asImageBitmap()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SnackbarHost(hostState = snackbarHostState)
        Text(text = "Review", style = MaterialTheme.typography.headlineMedium)
        Text(text = statusMessage, style = MaterialTheme.typography.bodyMedium)

        if (bitmap != null) {
            var canvasSize by remember { mutableStateOf(IntSize.Zero) }
            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(bitmap, uiState.points) {
                        detectTapGestures { tapOffset ->
                            val mapped = mapCanvasTapToImagePoint(
                                tap = tapOffset,
                                canvasSize = canvasSize,
                                imageWidth = imageWidth,
                                imageHeight = imageHeight
                            ) ?: return@detectTapGestures
                            viewModel.addPoint(mapped)
                        }
                    }
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Captured wound photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val mappedPoints = uiState.points.mapNotNull { point ->
                        mapImagePointToCanvasOffset(
                            point = point,
                            canvasSize = size,
                            imageWidth = imageWidth,
                            imageHeight = imageHeight
                        )
                    }

                    if (mappedPoints.size >= 2) {
                        for (i in 0 until mappedPoints.lastIndex) {
                            drawLine(
                                color = Color.Red,
                                start = mappedPoints[i],
                                end = mappedPoints[i + 1],
                                strokeWidth = 4f
                            )
                        }
                    }

                    if (mappedPoints.size >= 3) {
                        drawLine(
                            color = Color.Red.copy(alpha = 0.7f),
                            start = mappedPoints.last(),
                            end = mappedPoints.first(),
                            strokeWidth = 3f
                        )
                    }

                    mappedPoints.forEach { point ->
                        drawCircle(color = Color.Yellow, radius = 8f, center = point)
                    }
                }
            }
        }

        Text(
            text = "Saved pixel area: ${uiState.pixelArea?.let { String.format("%.2f", it) } ?: "-"}",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { viewModel.undoLastPoint() }, modifier = Modifier.weight(1f), enabled = !uiState.isSaving) {
                Text("Undo")
            }
            Button(onClick = { viewModel.clearPoints() }, modifier = Modifier.weight(1f), enabled = !uiState.isSaving) {
                Text("Clear")
            }
            Button(
                onClick = { viewModel.saveOutlineAndMeasurement(assessmentId) },
                enabled = uiState.points.size >= 3 && !uiState.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                } else {
                    Text("Save Outline")
                }
            }
        }

        Button(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving
        ) {
            Text("Retake")
        }
    }
}

private fun mapCanvasTapToImagePoint(
    tap: Offset,
    canvasSize: IntSize,
    imageWidth: Float,
    imageHeight: Float
): PointF? {
    if (canvasSize.width == 0 || canvasSize.height == 0) return null

    val canvasW = canvasSize.width.toFloat()
    val canvasH = canvasSize.height.toFloat()
    val imageAspect = imageWidth / imageHeight
    val canvasAspect = canvasW / canvasH

    val drawnW: Float
    val drawnH: Float
    val left: Float
    val top: Float

    if (imageAspect > canvasAspect) {
        drawnW = canvasW
        drawnH = canvasW / imageAspect
        left = 0f
        top = (canvasH - drawnH) / 2f
    } else {
        drawnH = canvasH
        drawnW = canvasH * imageAspect
        top = 0f
        left = (canvasW - drawnW) / 2f
    }

    if (tap.x < left || tap.x > left + drawnW || tap.y < top || tap.y > top + drawnH) return null

    val normalizedX = (tap.x - left) / drawnW
    val normalizedY = (tap.y - top) / drawnH

    return PointF(normalizedX * imageWidth, normalizedY * imageHeight)
}

private fun mapImagePointToCanvasOffset(
    point: PointF,
    canvasSize: androidx.compose.ui.geometry.Size,
    imageWidth: Float,
    imageHeight: Float
): Offset? {
    if (canvasSize.width == 0f || canvasSize.height == 0f) return null

    val imageAspect = imageWidth / imageHeight
    val canvasAspect = canvasSize.width / canvasSize.height

    val drawnW: Float
    val drawnH: Float
    val left: Float
    val top: Float

    if (imageAspect > canvasAspect) {
        drawnW = canvasSize.width
        drawnH = canvasSize.width / imageAspect
        left = 0f
        top = (canvasSize.height - drawnH) / 2f
    } else {
        drawnH = canvasSize.height
        drawnW = canvasSize.height * imageAspect
        top = 0f
        left = (canvasSize.width - drawnW) / 2f
    }

    val x = left + (point.x / imageWidth) * drawnW
    val y = top + (point.y / imageHeight) * drawnH
    return Offset(x, y)
}
