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
    onCalibrate: () -> Unit,
    onMarkerCalibration: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assessment by viewModel.assessment.collectAsState()
    val computation by viewModel.measurementComputation.collectAsState()

    LaunchedEffect(assessmentId) {
        viewModel.loadMeasurement(assessmentId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Measurement Result", style = MaterialTheme.typography.headlineMedium)

        val areaPixels = computation?.areaPixels
        if (areaPixels == null) {
            Text(text = "No outline saved yet", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(text = "Area (pixels²): ${"%.2f".format(areaPixels)}", style = MaterialTheme.typography.bodyLarge)
        }

        val points = OutlineJsonConverter.fromJson(assessment?.polygonPointsJson ?: assessment?.outlineJson).points
        val bitmap = assessment?.rectifiedImagePath
            ?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
            ?: assessment?.imagePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }

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
                        mapImagePointToCanvasOffset(it, size, bitmap.width.toFloat(), bitmap.height.toFloat())
                    }
                    if (mapped.size >= 3) {
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(mapped.first().x, mapped.first().y)
                                for (i in 1 until mapped.size) lineTo(mapped[i].x, mapped[i].y)
                                close()
                            },
                            color = Color.Red.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }

        val areaCm2 = computation?.areaCm2
        if (areaCm2 != null) {
            Text(text = "Area (cm²): ${"%.2f".format(areaCm2)} cm²", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
        } else {
            Text(text = "Calibration needed to compute cm²", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onCalibrate, modifier = Modifier.fillMaxWidth()) { Text("Calibrate") }
            Button(onClick = onMarkerCalibration, modifier = Modifier.fillMaxWidth()) { Text("Marker Calibration") }
        }
    }
}
