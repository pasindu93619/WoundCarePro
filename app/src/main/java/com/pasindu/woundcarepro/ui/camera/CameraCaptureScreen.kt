package com.pasindu.woundcarepro.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pasindu.woundcarepro.data.local.AssessmentDao
import com.pasindu.woundcarepro.data.local.ImageAsset
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

@Composable
fun CameraCaptureScreen(
    assessmentId: Long,
    assessmentDao: AssessmentDao,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var statusMessage by remember { mutableStateOf("Ready") }

    val cameraExecutor = rememberCameraExecutor()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Camera permission is required to capture wound images.")
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Grant camera permission")
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val capture = ImageCapture.Builder().build()
                        imageCapture = capture

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                        } catch (exception: Exception) {
                            Log.e("CameraCaptureScreen", "Binding failed", exception)
                            statusMessage = "Camera initialization failed"
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
        }

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Button(
            onClick = {
                val capture = imageCapture ?: return@Button
                val imageFile = createImageFile(context, assessmentId)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

                statusMessage = "Capturing..."
                capture.takePicture(
                    outputOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            scope.launch {
                                assessmentDao.insertImageAsset(
                                    ImageAsset(
                                        assessmentId = assessmentId,
                                        filePath = imageFile.absolutePath,
                                        lightingQc = "PENDING",
                                        focusQc = "PENDING",
                                        orientationQc = "PENDING"
                                    )
                                )
                                statusMessage = "Saved: ${imageFile.name}"
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("CameraCaptureScreen", "Image capture failed", exception)
                            statusMessage = "Capture failed"
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Capture Image")
        }
    }
}

@Composable
private fun rememberCameraExecutor(): ExecutorService {
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }
    return executor
}

private fun Context.hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
}

private fun createImageFile(context: Context, assessmentId: Long): File {
    val imagesDir = File(context.filesDir, "assessment_images/$assessmentId")
    if (!imagesDir.exists()) {
        imagesDir.mkdirs()
    }
    return File(imagesDir, "IMG_${System.currentTimeMillis()}.jpg")
}
