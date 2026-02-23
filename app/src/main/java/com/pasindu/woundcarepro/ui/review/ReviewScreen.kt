package com.pasindu.woundcarepro.ui.review

import android.graphics.BitmapFactory
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
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ReviewScreen(
    assessmentId: String,
    viewModel: ReviewViewModel,
    onRetake: () -> Unit,
    onNextAfterSave: (needsCalibration: Boolean) -> Unit,
    onMarkerCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assessment by viewModel.assessment.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(assessmentId) { viewModel.loadAssessment(assessmentId) }

    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let { msg ->
            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
            viewModel.clearTransientState()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNextAfterSave(uiState.needsCalibration)
            viewModel.clearTransientState()
        }
    }

    LaunchedEffect(uiState.needsCalibration) {
        if (uiState.needsCalibration) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Calibration needed for cm²") }
        }
    }

    val activeImagePath = assessment?.rectifiedImagePath ?: assessment?.imagePath
    val bitmap = activeImagePath?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }

    val statusMessage = when {
        activeImagePath == null -> "No captured image found. Please retake."
        uiState.isPolygonClosed -> "Polygon closed. Save to persist."
        else -> "Tap on wound border to add polygon points"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SnackbarHost(hostState = snackbarHostState)
        Text("Review", style = MaterialTheme.typography.headlineMedium)
        Text(statusMessage)

        if (bitmap != null) {
            var canvasPx by remember { mutableStateOf(IntSize.Zero) }
            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .onSizeChanged { canvasPx = it }
                    .pointerInput(bitmap, uiState.points, uiState.isPolygonClosed, canvasPx) {
                        detectTapGestures { tap ->
                            if (uiState.isPolygonClosed) return@detectTapGestures

                            val mapped = mapCanvasTapToImagePoint(
                                tap = tap,
                                canvasSize = canvasPx,
                                imageWidth = imageWidth,
                                imageHeight = imageHeight
                            ) ?: return@detectTapGestures

                            viewModel.addPoint(mapped.toPointF())
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

                    if (mappedPoints.size >= 3 && uiState.isPolygonClosed) {
                        drawLine(
                            color = Color.Red,
                            start = mappedPoints.last(),
                            end = mappedPoints.first(),
                            strokeWidth = 4f
                        )

                        val poly = Path().apply {
                            moveTo(mappedPoints.first().x, mappedPoints.first().y)
                            for (i in 1 until mappedPoints.size) {
                                lineTo(mappedPoints[i].x, mappedPoints[i].y)
                            }
                            close()
                        }
                        drawPath(poly, color = Color.Red.copy(alpha = 0.25f))
                    }

                    mappedPoints.forEach { c ->
                        drawCircle(color = Color.Yellow, radius = 8f, center = c)
                        drawCircle(color = Color.Black, radius = 8f, center = c, style = Stroke(width = 2f))
                    }
                }
            }
        }

        Text("Pixel area: ${uiState.pixelArea?.let { String.format("%.2f", it) } ?: "-"}")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.undoLastPoint() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSaving
            ) { Text("Undo") }
            Button(
                onClick = { viewModel.closePolygon() },
                enabled = uiState.points.size >= 3 && !uiState.isPolygonClosed,
                modifier = Modifier.weight(1f)
            ) { Text("Close") }
            Button(
                onClick = { viewModel.clearPoints() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSaving
            ) { Text("Clear") }
            Button(
                onClick = { viewModel.saveOutline(assessmentId) },
                enabled = uiState.isPolygonClosed && !uiState.isSaving,
                modifier = Modifier.weight(1f)
            ) { Text("Save") }
        }

        Button(
            onClick = onMarkerCalibration,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving
        ) { Text("Marker Calibration") }

        Button(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving
        ) { Text("Retake") }
    }
}
