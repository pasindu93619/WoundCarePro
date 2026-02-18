package com.pasindu.woundcarepro.ui.review

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pasindu.woundcarepro.measurement.OutlineJsonConverter

@Composable
fun MeasurementResultScreen(
    assessmentId: String,
    viewModel: MeasurementViewModel,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var saveMessage by remember { mutableStateOf("") }
    val computation by viewModel.measurementComputation.collectAsState()
    val assessment by viewModel.assessment.collectAsState()

    LaunchedEffect(assessmentId) {
        viewModel.loadMeasurement(assessmentId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Measurement Result", style = MaterialTheme.typography.headlineMedium)

        Text(
            text = "Area (pixels²): ${computation?.areaPixels?.let { "%.2f".format(it) } ?: "N/A"}",
            style = MaterialTheme.typography.bodyLarge
        )

        val points = OutlineJsonConverter.fromJson(assessment?.polygonPointsJson ?: assessment?.outlineJson)
        val bitmap = assessment?.imagePath?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }

        if (bitmap != null && points.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color.Black)
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Measurement result image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val mapped = points.mapNotNull {
                        mapImagePointToCanvasOffset(
                            point = it,
                            canvasSize = size,
                            imageWidth = bitmap.width.toFloat(),
                            imageHeight = bitmap.height.toFloat()
                        )
                    }
                    if (mapped.size >= 3) {
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(mapped.first().x, mapped.first().y)
                                for (i in 1 until mapped.size) {
                                    lineTo(mapped[i].x, mapped[i].y)
                                }
                                close()
                            },
                            color = Color.Red.copy(alpha = 0.25f)
                        )
                    }
                    if (mapped.size >= 2) {
                        for (i in 0 until mapped.lastIndex) {
                            drawLine(Color.Red, mapped[i], mapped[i + 1], 3f)
                        }
                    }
                    if (mapped.size >= 3) {
                        drawLine(Color.Red, mapped.last(), mapped.first(), 3f)
                    }
                }
            }
        }

        val areaCm2 = computation?.areaCm2
        if (areaCm2 != null) {
            Text(
                text = "Area (cm²): %.4f".format(areaCm2),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Text(
                text = "Calibration not set. Area in cm² is unavailable.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (saveMessage.isNotBlank()) {
            Text(text = saveMessage, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = {
                viewModel.saveMeasurement(assessmentId = assessmentId) {
                    saveMessage = "Measurement saved."
                    onNext()
                }
            },
            enabled = computation != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save & Continue")
        }
    }
}
