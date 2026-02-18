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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var saveMessage by remember { mutableStateOf("") }
    val computation by viewModel.measurementComputation.collectAsState()

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
