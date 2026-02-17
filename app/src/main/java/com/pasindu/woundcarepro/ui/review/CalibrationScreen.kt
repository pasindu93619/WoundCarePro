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
fun CalibrationScreen(
    assessmentId: Long,
    assessmentDao: AssessmentDao,
    onCalibrationSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var referenceLengthPixels by remember { mutableStateOf("") }
    var referenceLengthCm by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Enter reference object size for calibration") }

    val pixelsValue = referenceLengthPixels.toDoubleOrNull()
    val cmValue = referenceLengthCm.toDoubleOrNull()
    val isValid = pixelsValue != null && cmValue != null && pixelsValue > 0.0 && cmValue > 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Calibration", style = MaterialTheme.typography.headlineMedium)
        Text(text = status, style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = referenceLengthPixels,
            onValueChange = { referenceLengthPixels = it },
            label = { Text("Reference length (pixels)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = referenceLengthCm,
            onValueChange = { referenceLengthCm = it },
            label = { Text("Reference length (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val cmPerPixel = cmValue!! / pixelsValue!!
                scope.launch {
                    assessmentDao.insertCalibrationParams(
                        CalibrationParams(
                            assessmentId = assessmentId,
                            referenceLengthPixels = pixelsValue,
                            referenceLengthCm = cmValue,
                            cmPerPixel = cmPerPixel
                        )
                    )
                    status = "Calibration saved (1 px = %.6f cm)".format(cmPerPixel)
                    onCalibrationSaved()
                }
            },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Calibration")
        }
    }
}
