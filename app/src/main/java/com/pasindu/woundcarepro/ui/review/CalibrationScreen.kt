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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.hypot

@Composable
fun CalibrationScreen(
    assessmentId: String,
    viewModel: CalibrationViewModel,
    onCalibrationSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assessment by viewModel.assessment.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var startPoint by remember { mutableStateOf<PointF?>(null) }
    var endPoint by remember { mutableStateOf<PointF?>(null) }
    var realLengthCmInput by remember { mutableStateOf("") }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(assessmentId) {
        viewModel.loadAssessment(assessmentId)
    }

    val bitmap = assessment?.imagePath?.let { path ->
        BitmapFactory.decodeFile(path)?.asImageBitmap()
    }

    val pixelDistance = if (startPoint != null && endPoint != null) {
        hypot(
            (endPoint!!.x - startPoint!!.x).toDouble(),
            (endPoint!!.y - startPoint!!.y).toDouble()
        )
    } else {
        0.0
    }

    val realLengthCm = realLengthCmInput.toDoubleOrNull()
    val calibrationFactor = if (pixelDistance > 0.0 && (realLengthCm ?: 0.0) > 0.0) {
        realLengthCm!! / pixelDistance
    } else {
        null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SnackbarHost(hostState = snackbarHostState)
        Text(text = "Calibration", style = MaterialTheme.typography.headlineMedium)

        if (bitmap != null) {
            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(bitmap, canvasSize, startPoint, endPoint) {
                        detectTapGestures { tapOffset ->
                            val mapped = mapCanvasTapToImagePoint(
                                tap = tapOffset,
                                canvasSize = canvasSize,
                                imageWidth = imageWidth,
                                imageHeight = imageHeight
                            ) ?: return@detectTapGestures

                            when {
                                startPoint == null -> startPoint = mapped
                                endPoint == null -> endPoint = mapped
                                else -> {
                                    startPoint = mapped
                                    endPoint = null
                                }
                            }
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
                    val startOffset = startPoint?.let {
                        mapImagePointToCanvasOffset(it, size, imageWidth, imageHeight)
                    }
                    val endOffset = endPoint?.let {
                        mapImagePointToCanvasOffset(it, size, imageWidth, imageHeight)
                    }

                    if (startOffset != null && endOffset != null) {
                        drawLine(
                            color = Color.Cyan,
                            start = startOffset,
                            end = endOffset,
                            strokeWidth = 5f
                        )
                    }

                    startOffset?.let {
                        drawCircle(color = Color.Yellow, radius = 12f, center = it)
                        drawCircle(color = Color.Black, radius = 12f, center = it, style = Stroke(width = 3f))
                    }
                    endOffset?.let {
                        drawCircle(color = Color.Yellow, radius = 12f, center = it)
                        drawCircle(color = Color.Black, radius = 12f, center = it, style = Stroke(width = 3f))
                    }
                }
            }
        } else {
            Text(text = "No captured image found.")
        }

        OutlinedTextField(
            value = realLengthCmInput,
            onValueChange = { realLengthCmInput = it },
            label = { Text("Real length (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Pixel distance: ${String.format("%.2f", pixelDistance)} px",
            style = MaterialTheme.typography.bodyMedium
        )

        if (calibrationFactor != null) {
            Text(
                text = "Calibration factor: ${String.format("%.6f", calibrationFactor)} cm/px",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = {
                startPoint = null
                endPoint = null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Points")
        }

        Button(
            onClick = {
                if (pixelDistance <= 0.0 || (realLengthCm ?: 0.0) <= 0.0) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Select 2 points and enter a valid real length")
                    }
                    return@Button
                }
                val factor = realLengthCm!! / pixelDistance
                viewModel.saveCalibration(assessmentId, factor) {
                    onCalibrationSaved()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Calibration")
        }
    }
}
