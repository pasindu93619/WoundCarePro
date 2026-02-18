package com.pasindu.woundcarepro.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun MeasurementResultScreen(
    assessmentId: String,
    viewModel: MeasurementViewModel,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var woundAreaPixels by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf("") }
    val calibrationFactor by viewModel.calibrationFactor.collectAsState()

    LaunchedEffect(assessmentId) {
        viewModel.loadCalibration(assessmentId)
    }

    val areaPixelsValue = woundAreaPixels.toDoubleOrNull()
    val areaCm2 = areaPixelsValue?.let { viewModel.computeAreaCm2(it) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Measurement Result", style = MaterialTheme.typography.headlineMedium)

        if (calibrationFactor == null) {
            Text(
                text = "No calibration found. Please calibrate before measurement.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = "Calibration: 1 px = %.6f cm".format(calibrationFactor),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = woundAreaPixels,
                onValueChange = { woundAreaPixels = it },
                label = { Text("Wound area (pixels²)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = if (areaCm2 != null) {
                    "Converted wound area: %.4f cm²".format(areaCm2)
                } else {
                    "Enter wound area in pixels² to convert"
                },
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (saveMessage.isNotBlank()) {
            Text(text = saveMessage, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = {
                val finalPixels = areaPixelsValue
                if (finalPixels == null) {
                    saveMessage = "Please enter a valid wound area to continue."
                    return@Button
                }
                viewModel.saveMeasurement(assessmentId = assessmentId, areaPixels = finalPixels) {
                    saveMessage = "Measurement saved."
                    onNext()
                }
            },
            enabled = calibrationFactor != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save & Continue")
        }
    }
}
