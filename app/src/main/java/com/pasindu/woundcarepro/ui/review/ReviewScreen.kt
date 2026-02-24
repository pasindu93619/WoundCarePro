package com.pasindu.woundcarepro.ui.review

import android.graphics.BitmapFactory
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import kotlin.math.min

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

    LaunchedEffect(uiState.saveState) {
        when (val saveState = uiState.saveState) {
            is FinalOutlineSaveState.Error -> {
                coroutineScope.launch { snackbarHostState.showSnackbar(saveState.message) }
                viewModel.clearTransientState()
            }
            FinalOutlineSaveState.Saved -> {
                onNextAfterSave(uiState.needsCalibration)
                viewModel.clearTransientState()
            }
            FinalOutlineSaveState.Idle, FinalOutlineSaveState.Saving -> Unit
        }
    }


    LaunchedEffect(uiState.needsCalibration) {
        if (uiState.needsCalibration) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Calibration needed for cm²")
            }
        }
    }

    val activeImagePath = assessment?.rectifiedImagePath ?: assessment?.imagePath
    val bitmap = activeImagePath?.let {
        BitmapFactory.decodeFile(it)?.asImageBitmap()
    }

    fun mapCanvasTapToImagePoint(
        tap: Offset,
        canvasSize: IntSize,
        imageWidth: Float,
        imageHeight: Float
    ): Offset? {

        if (canvasSize.width <= 0 || canvasSize.height <= 0) return null

        val cw = canvasSize.width.toFloat()
        val ch = canvasSize.height.toFloat()

        val scale = min(cw / imageWidth, ch / imageHeight)
        val drawnW = imageWidth * scale
        val drawnH = imageHeight * scale

        val left = (cw - drawnW) / 2f
        val top = (ch - drawnH) / 2f

        val xIn = tap.x - left
        val yIn = tap.y - top

        if (xIn < 0f || yIn < 0f || xIn > drawnW || yIn > drawnH) return null

        val ix = xIn / scale
        val iy = yIn / scale

        return Offset(ix, iy)
    }

    val statusMessage = when {
        activeImagePath == null -> "No captured image found. Please retake."
        uiState.isPolygonClosed -> "Polygon closed. Save to persist."
        uiState.saveState == FinalOutlineSaveState.Saving -> "Saving final outline..."
        uiState.saveState == FinalOutlineSaveState.Saved -> "Final outline saved"
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
        Text(statusMessage, style = MaterialTheme.typography.bodyMedium)

        if (bitmap == null) {
            Text(
                "No image loaded. Please retake.",
                color = MaterialTheme.colorScheme.error
            )
            Button(
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retake")
            }
            return@Column
        }

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

                        val imagePoint = PointF(mapped.x, mapped.y)
                        viewModel.addPoint(imagePoint)
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

                // Draw points
                mappedPoints.forEach { c ->
                    drawCircle(Color.Yellow, 8f, c)
                    drawCircle(Color.Black, 8f, c, style = Stroke(width = 2f))
                }

                // Draw lines
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

                // Close + fill polygon
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

                    drawPath(poly, Color.Red.copy(alpha = 0.25f))
                }
            }
        }

        Text(
            text = "Pixel area: ${
                uiState.pixelArea?.let { String.format("%.2f", it) } ?: "-"
            }",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.undoLastPoint() },
                modifier = Modifier.weight(1f),
                enabled = uiState.saveState != FinalOutlineSaveState.Saving && uiState.points.isNotEmpty()
            ) { Text("Undo") }

            Button(
                onClick = { viewModel.closePolygon() },
                modifier = Modifier.weight(1f),
                enabled = uiState.points.size >= 3 &&
                        !uiState.isPolygonClosed &&
                        uiState.saveState != FinalOutlineSaveState.Saving
            ) { Text("Close") }

            Button(
                onClick = { viewModel.clearPoints() },
                modifier = Modifier.weight(1f),
                enabled = uiState.saveState != FinalOutlineSaveState.Saving && uiState.points.isNotEmpty()
            ) { Text("Clear") }

            Button(
                onClick = { viewModel.saveFinalOutline(assessmentId) },
                modifier = Modifier.weight(1f),
                enabled = uiState.isPolygonClosed && uiState.saveState != FinalOutlineSaveState.Saving
            ) {
                if (uiState.saveState == FinalOutlineSaveState.Saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        }

        Button(
            onClick = onMarkerCalibration,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.saveState != FinalOutlineSaveState.Saving
        ) { Text("Marker Calibration") }

        Button(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.saveState != FinalOutlineSaveState.Saving
        ) { Text("Retake") }
    }
}

private fun mapImagePointToCanvasOffset(
    point: Offset,
    canvasSize: Size,
    imageWidth: Float,
    imageHeight: Float
): Offset? {

    val cw = canvasSize.width
    val ch = canvasSize.height

    if (cw <= 0f || ch <= 0f) return null

    val scale = min(cw / imageWidth, ch / imageHeight)
    val drawnW = imageWidth * scale
    val drawnH = imageHeight * scale

    val left = (cw - drawnW) / 2f
    val top = (ch - drawnH) / 2f

    val cx = left + point.x * scale
    val cy = top + point.y * scale

    return Offset(cx, cy)
}
