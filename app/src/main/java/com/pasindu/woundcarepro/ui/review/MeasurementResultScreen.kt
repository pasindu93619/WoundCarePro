package com.pasindu.woundcarepro.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp

@Composable
fun MeasurementResultScreen(
    assessmentId: String,
    viewModel: MeasurementViewModel,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val areaPixels by viewModel.areaPixels.collectAsState()

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

        if (areaPixels == null) {
            Text(
                text = "No outline saved yet",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Text(
                text = "Area (pixels²): ${"%.2f".format(areaPixels)}",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Text(
            text = "Calibration not applied yet (cm² will be added next milestone)",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}
