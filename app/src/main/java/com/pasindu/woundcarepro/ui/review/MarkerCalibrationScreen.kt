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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun MarkerCalibrationScreen(
    assessmentId: String,
    viewModel: MarkerCalibrationViewModel,
    onRectificationSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assessment by viewModel.assessment.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var markerSizeInput by remember { mutableStateOf("5.0") }
    val context = LocalContext.current

    LaunchedEffect(assessmentId) {
        viewModel.loadAssessment(assessmentId)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearTransient()
        }
    }

    LaunchedEffect(uiState.done) {
        if (uiState.done) {
            onRectificationSaved()
            viewModel.clearTransient()
        }
    }

    val bitmap = assessment?.imagePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SnackbarHost(snackbar)
        Text("Marker Calibration", style = MaterialTheme.typography.headlineMedium)
        Text("Tap order: top-left, top-right, bottom-right, bottom-left")

        if (bitmap != null) {
            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()
            var canvasSize by remember { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(bitmap, uiState.points) {
                        detectTapGestures { tapOffset ->
                            val mapped = mapCanvasTapToImagePoint(tapOffset, canvasSize, imageWidth, imageHeight)
                                ?: return@detectTapGestures
                            viewModel.addPoint(mapped)
                        }
                    }
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Calibration source",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                Canvas(Modifier.fillMaxSize()) {
                    val mapped = uiState.points.mapNotNull {
                        mapImagePointToCanvasOffset(it, size, imageWidth, imageHeight)
                    }
                    if (mapped.size >= 2) {
                        for (i in 0 until mapped.lastIndex) {
                            drawLine(Color.Green, mapped[i], mapped[i + 1], strokeWidth = 4f)
                        }
                    }
                    if (mapped.size == 4) {
                        drawLine(Color.Green, mapped[3], mapped[0], strokeWidth = 4f)
                    }
                    mapped.forEachIndexed { index, pt ->
                        drawCircle(color = Color.Yellow, radius = 10f, center = pt)
                        drawContext.canvas.nativeCanvas.drawText(
                            "${index + 1}",
                            pt.x + 12f,
                            pt.y - 12f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 34f
                            }
                        )
                    }
                }
            }
        } else {
            Text("No captured image found")
        }

        OutlinedTextField(
            value = markerSizeInput,
            onValueChange = {
                markerSizeInput = it
                it.toDoubleOrNull()?.takeIf { value -> value > 0.0 }?.let(viewModel::setMarkerSizeCm)
            },
            label = { Text("Marker size (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Derived calibration: ${uiState.calibrationFactor?.let { String.format("%.6f", it) } ?: "-"} cm/px")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = viewModel::undoPoint, modifier = Modifier.weight(1f), enabled = !uiState.isSaving) { Text("Undo") }
            Button(onClick = viewModel::clearPoints, modifier = Modifier.weight(1f), enabled = !uiState.isSaving) { Text("Clear") }
            Button(
                onClick = { viewModel.rectifyAndSave(context, assessmentId) },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSaving && uiState.points.size == 4 && uiState.calibrationFactor != null
            ) {
                if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text("Rectify & Continue")
            }
        }
    }
}
