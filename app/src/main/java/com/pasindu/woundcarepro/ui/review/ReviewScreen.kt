package com.pasindu.woundcarepro.ui.review

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pasindu.woundcarepro.measurement.OutlineJsonConverter
import com.pasindu.woundcarepro.measurement.OutlinePoint

@Composable
fun ReviewScreen(
    assessmentId: String,
    viewModel: ReviewViewModel,
    onRetake: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assessment by viewModel.assessment.collectAsState()

    LaunchedEffect(assessmentId) {
        viewModel.loadAssessment(assessmentId)
    }

    val statusMessage = if (assessment?.imagePath == null) {
        "No captured image found. Please retake."
    } else {
        "Review captured image"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Review", style = MaterialTheme.typography.headlineMedium)
        Text(text = statusMessage, style = MaterialTheme.typography.bodyMedium)

        val bitmap = assessment?.imagePath?.let { path ->
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Captured wound photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }

        Button(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Retake")
        }

        Button(
            onClick = {
                val outlinePoints = bitmap?.let {
                    val width = it.width.toDouble()
                    val height = it.height.toDouble()
                    listOf(
                        OutlinePoint(width * 0.25, height * 0.25),
                        OutlinePoint(width * 0.75, height * 0.25),
                        OutlinePoint(width * 0.75, height * 0.75),
                        OutlinePoint(width * 0.25, height * 0.75)
                    )
                }.orEmpty()

                viewModel.updateOutline(
                    assessmentId,
                    outlineJson = OutlineJsonConverter.toJson(outlinePoints)
                ) {
                    onAccept()
                }
            },
            enabled = assessment?.imagePath != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Accept & Continue")
        }
    }
}
