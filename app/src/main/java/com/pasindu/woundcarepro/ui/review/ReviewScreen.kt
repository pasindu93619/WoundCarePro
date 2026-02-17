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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pasindu.woundcarepro.data.local.AssessmentDao
import com.pasindu.woundcarepro.data.local.ImageAsset
import kotlinx.coroutines.launch

private const val AcceptedStatus = "PHOTO_ACCEPTED"

@Composable
fun ReviewScreen(
    assessmentId: Long,
    assessmentDao: AssessmentDao,
    onRetake: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var latestAsset by remember { mutableStateOf<ImageAsset?>(null) }
    var statusMessage by remember { mutableStateOf("Loading captured image...") }

    LaunchedEffect(assessmentId) {
        latestAsset = assessmentDao.getLatestAssetForAssessment(assessmentId)
        statusMessage = if (latestAsset == null) {
            "No captured image found. Please retake."
        } else {
            "Review captured image"
        }
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

        val bitmap = remember(latestAsset?.filePath) {
            latestAsset?.filePath?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }
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
                scope.launch {
                    assessmentDao.updateAssessmentStatus(assessmentId, AcceptedStatus)
                    onAccept()
                }
            },
            enabled = latestAsset != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Accept & Continue")
        }
    }
}
