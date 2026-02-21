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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    LaunchedEffect(uiState.needsCalibration) {
        if (uiState.needsCalibration) {
            snackbarHostState.showSnackbar("Calibration needed for cm²")
        }
    }

    val activeImagePath = assessment?.rectifiedImagePath ?: assessment?.imagePath
    val statusMessage = if (activeImagePath == null) {
        "No captured image found. Please retake."
    } else if (uiState.isPolygonClosed) {
        "Polygon closed. Save to persist."
    } else {
        "Tap on wound border to add polygon points"
    }

    val bitmap = activeImagePath?.let { path ->
        BitmapFactory.decodeFile(path)?.asImageBitmap()
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
        Text(text = "Review", style = MaterialTheme.typography.headlineMedium)
        Text(text = statusMessage, style = MaterialTheme.typography.bodyMedium)

        Button(
            onClick = onMarkerCalibration,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving && assessment?.imagePath != null
        ) {
            Text("Marker Calibration (Recommended)")
        }

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
                    .pointerInput(bitmap, uiState.points, uiState.isPolygonClosed) {
                        detectTapGestures { tapOffset ->
                            if (uiState.isPolygonClosed) return@detectTapGestures
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

                    if (mappedPoints.size >= 3 && uiState.isPolygonClosed) {
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(mappedPoints.first().x, mappedPoints.first().y)
                                for (i in 1 until mappedPoints.size) {
                                    lineTo(mappedPoints[i].x, mappedPoints[i].y)
                                }
                                close()
                            },
                            color = Color.Red.copy(alpha = 0.25f)
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
                    }

                    mappedPoints.forEach { point ->
                        drawCircle(color = Color.Yellow, radius = 8f, center = point)
                        drawCircle(color = Color.Black, radius = 8f, center = point, style = Stroke(width = 2f))
                    }
                }
            }
        }

        Text(
            text = "Pixel area: ${uiState.pixelArea?.let { String.format("%.2f", it) } ?: "-"}",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { viewModel.undoLastPoint() }, modifier = Modifier.weight(1f), enabled = !uiState.isSaving) {
                Text("Undo")
            }
            Button(
                onClick = { viewModel.closePolygon() },
                enabled = uiState.points.size >= 3 && !uiState.isPolygonClosed,
                modifier = Modifier.weight(1f)
            ) {
                Text("Close")
            }
            Button(onClick = { viewModel.clearPoints() }, modifier = Modifier.weight(1f)) {
                Text("Clear")
            }
            Button(
                onClick = { viewModel.saveOutline(assessmentId) },
                enabled = uiState.isPolygonClosed,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
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

private fun isSelfIntersecting(points: List<PointF>): Boolean {
    if (points.size < 4) return false

    for (i in points.indices) {
        val a1 = points[i]
        val a2 = points[(i + 1) % points.size]

        for (j in i + 1 until points.size) {
            if (kotlin.math.abs(i - j) <= 1) continue
            if (i == 0 && j == points.lastIndex) continue

            val b1 = points[j]
            val b2 = points[(j + 1) % points.size]

internal fun mapImagePointToCanvasOffset(
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

    return false
}

private fun segmentsIntersect(p1: PointF, p2: PointF, p3: PointF, p4: PointF): Boolean {
    val d1 = direction(p3, p4, p1)
    val d2 = direction(p3, p4, p2)
    val d3 = direction(p1, p2, p3)
    val d4 = direction(p1, p2, p4)

    return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
        ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
}

private fun direction(a: PointF, b: PointF, c: PointF): Float {
    return ((c.x - a.x) * (b.y - a.y)) - ((c.y - a.y) * (b.x - a.x))
}
