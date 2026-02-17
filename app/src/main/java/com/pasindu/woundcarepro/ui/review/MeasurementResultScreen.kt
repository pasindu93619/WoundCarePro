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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pasindu.woundcarepro.data.local.AssessmentDao
import com.pasindu.woundcarepro.data.local.CalibrationParams
import kotlinx.coroutines.launch

@Composable
fun MeasurementResultScreen(
    assessmentId: Long,
    assessmentDao: AssessmentDao,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var calibration by remember { mutableStateOf<CalibrationParams?>(null) }
    var woundAreaPixels by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf("") }

    LaunchedEffect(assessmentId) {
        calibration = assessmentDao.getLatestCalibrationForAssessment(assessmentId)
    }

    val areaPixelsValue = woundAreaPixels.toDoubleOrNull()
    val areaCm2 = if (calibration != null && areaPixelsValue != null && areaPixelsValue > 0.0) {
        areaPixelsValue * calibration!!.cmPerPixel * calibration!!.cmPerPixel
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
        Text(text = "Measurement Result", style = MaterialTheme.typography.headlineMedium)

        if (calibration == null) {
            Text(
                text = "No calibration found. Please calibrate before measurement.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = "Calibration: 1 px = %.6f cm".format(calibration!!.cmPerPixel),
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
                val finalArea = areaCm2
                if (finalArea == null) {
                    saveMessage = "Please enter a valid wound area to continue."
                    return@Button
                }
                scope.launch {
                    assessmentDao.saveMeasurementResult(assessmentId = assessmentId, woundAreaCm2 = finalArea)
                    saveMessage = "Measurement saved."
                    onNext()
                }
            },
            enabled = calibration != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save & Continue")
        }
    }
}
