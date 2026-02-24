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

    var firstPoint by remember { mutableStateOf<PointF?>(null) }
    var secondPoint by remember { mutableStateOf<PointF?>(null) }
    var realLengthInput by remember { mutableStateOf("") }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(assessmentId) {
        viewModel.loadAssessment(assessmentId)
    }

    val activeImagePath = assessment?.rectifiedImagePath ?: assessment?.imagePath
    val imageBitmap = remember(activeImagePath) {
        activeImagePath?.let { path ->
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }

    val pixelDistance = if (firstPoint != null && secondPoint != null) {
        hypot(
            (secondPoint!!.x - firstPoint!!.x).toDouble(),
            (secondPoint!!.y - firstPoint!!.y).toDouble()
        )
    } else {
        0.0
    }

    val realLengthCm = realLengthInput.toDoubleOrNull()
    val factor = if (pixelDistance > 0.0 && (realLengthCm ?: 0.0) > 0.0) {
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

        Text(
            text = "Calibration",
            style = MaterialTheme.typography.headlineMedium
        )

        if (imageBitmap != null) {
            val imageWidth = imageBitmap.width.toFloat()
            val imageHeight = imageBitmap.height.toFloat()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
                    .onSizeChanged { containerSize = it }
                    .pointerInput(imageBitmap, containerSize, firstPoint, secondPoint) {
                        detectTapGestures { tapOffset ->
                            val mapped = mapCanvasTapToImagePoint(
                                tap = tapOffset,
                                canvasSize = containerSize,
                                imageWidth = imageWidth,
                                imageHeight = imageHeight
                            ) ?: return@detectTapGestures

                            val imagePoint = mapped.toPointF()

                            when {
                                firstPoint == null -> firstPoint = imagePoint
                                secondPoint == null -> secondPoint = imagePoint
                                else -> {
                                    firstPoint = imagePoint
                                    secondPoint = null
                                }
                            }
                        }
                    }
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Captured wound image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val startOffset = firstPoint?.let {
                        mapImagePointToCanvasOffset(it, size, imageWidth, imageHeight)
                    }
                    val endOffset = secondPoint?.let {
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
                        drawCircle(
                            color = Color.Black,
                            radius = 12f,
                            center = it,
                            style = Stroke(width = 3f)
                        )
                    }
                    endOffset?.let {
                        drawCircle(color = Color.Yellow, radius = 12f, center = it)
                        drawCircle(
                            color = Color.Black,
                            radius = 12f,
                            center = it,
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
        } else {
            Text(text = "No image available for calibration.")
        }

        OutlinedTextField(
            value = realLengthInput,
            onValueChange = { realLengthInput = it },
            label = { Text("Real length (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Pixel distance: ${String.format("%.2f", pixelDistance)} px",
            style = MaterialTheme.typography.bodyMedium
        )

        if (factor != null) {
            Text(
                text = "Calibration factor: ${String.format("%.6f", factor)} cm/px",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = {
                firstPoint = null
                secondPoint = null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Points")
        }

        Button(
            onClick = {
                if (firstPoint == null || secondPoint == null) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Tap two points on the image before saving")
                    }
                    return@Button
                }

                if (pixelDistance <= 0.0 || (realLengthCm ?: 0.0) <= 0.0) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Enter a valid real length (> 0)")
                    }
                    return@Button
                }

                val calibrationFactor = realLengthCm!! / pixelDistance
                viewModel.saveCalibration(assessmentId, calibrationFactor) {
                    onCalibrationSaved()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Calibration")
        }
    }
}
