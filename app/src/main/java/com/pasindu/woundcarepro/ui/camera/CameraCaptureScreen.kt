package com.pasindu.woundcarepro.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraCaptureScreen(
    assessmentId: String,
    onPhotoCaptured: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    var hasCameraPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var statusMessage by remember { mutableStateOf("Ready") }
    var isCapturing by remember { mutableStateOf(false) }
    var latestQc by remember { mutableStateOf(QualityGateResult.empty()) }

    val cameraExecutor = rememberCameraExecutor()
    val sensorState = rememberTiltState()
    val qualityGateEvaluator = remember { QualityGateEvaluator() }

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
                .background(MaterialTheme.colorScheme.background)
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
            .background(MaterialTheme.colorScheme.background)
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
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .build()
                            .also { analyzer ->
                                analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                                    try {
                                        val qcResult = qualityGateEvaluator.evaluate(
                                            pitchDeg = sensorState.pitchDeg,
                                            rollDeg = sensorState.rollDeg,
                                            imageProxy = imageProxy
                                        )
                                        mainExecutor.execute { latestQc = qcResult }
                                    } catch (e: Exception) {
                                        Log.e("CameraCaptureScreen", "QC analyzer failed", e)
                                    } finally {
                                        imageProxy.close()
                                    }
                                }
                            }

                        try {
                            cameraProvider.unbindAll()
                            imageCapture = capture
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture,
                                analysis
                            )
                        } catch (exception: Exception) {
                            Log.e("CameraCaptureScreen", "Binding failed", exception)
                            statusMessage = "Camera initialization failed"
                        }
                    }, mainExecutor)
                    previewView
                }
            )

            QcOverlay(
                qcResult = latestQc,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
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
                val qcResultSnapshot = latestQc
                val qcStatus = if (qcResultSnapshot.overallPass) "PASS" else "WARN"

                isCapturing = true
                statusMessage = "Capturing..."
                capture.takePicture(
                    outputOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val metricsJson = qcResultSnapshot.toGuidanceMetricsJson(
                                timestampMillis = System.currentTimeMillis(),
                                qcStatus = qcStatus
                            )
                            viewModel.saveCaptureMetadata(
                                assessmentId = assessmentId,
                                imagePath = imageFile.absolutePath,
                                guidanceMetricsJson = metricsJson
                            ) {
                                mainExecutor.execute {
                                    isCapturing = false
                                    statusMessage = "Saved: ${imageFile.name}"
                                    onPhotoCaptured(assessmentId)
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("CameraCaptureScreen", "Image capture failed", exception)
                            mainExecutor.execute {
                                isCapturing = false
                                statusMessage = "Capture failed"
                            }
                        }
                    }
                )
            },
            enabled = latestQc.overallPass && !isCapturing,
            modifier = Modifier.fillMaxWidth()
        ) {
            val label = when {
                isCapturing -> "Capturing..."
                latestQc.overallPass -> "Capture Image"
                else -> "Adjust framing for quality"
            }
            Text(label)
        }
    }
}

@Composable
private fun QcOverlay(
    qcResult: QualityGateResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        QcLine(
            label = "Tilt",
            pass = qcResult.tiltPass,
            detail = "pitch ${qcResult.pitchDeg.format2()}°, roll ${qcResult.rollDeg.format2()}°"
        )
        QcLine(
            label = "Light",
            pass = qcResult.lightPass,
            detail = "mean ${qcResult.meanLuma.format2()}"
        )
        QcLine(
            label = "Glare",
            pass = qcResult.glarePass,
            detail = "${(qcResult.glarePct * 100).format2()}%"
        )
        QcLine(
            label = "Blur",
            pass = qcResult.blurPass,
            detail = "score ${qcResult.blurScore.format2()}"
        )
        Text(
            text = if (qcResult.overallPass) "Overall: READY" else "Overall: NOT READY",
            color = if (qcResult.overallPass) Color(0xFF7CFC00) else Color(0xFFFFA726),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun QcLine(label: String, pass: Boolean, detail: String) {
    Text(
        text = "$label: ${if (pass) "PASS" else "FAIL"} ($detail)",
        color = if (pass) Color(0xFF7CFC00) else Color(0xFFFFA726),
        style = MaterialTheme.typography.bodySmall
    )
}

private data class TiltState(
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f
)

@Composable
private fun rememberTiltState(): TiltState {
    val context = LocalContext.current
    var tiltState by remember { mutableStateOf(TiltState()) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (rotationSensor == null) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                private val rotationMatrix = FloatArray(9)
                private val orientation = FloatArray(3)

                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                    tiltState = TiltState(pitchDeg = pitch, rollDeg = roll)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    return tiltState
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

private fun createImageFile(context: Context, assessmentId: String): File {
    val imagesDir = File(context.filesDir, "assessment_images/$assessmentId")
    if (!imagesDir.exists()) {
        imagesDir.mkdirs()
    }
    return File(imagesDir, "IMG_${System.currentTimeMillis()}.jpg")
}

private fun Float.format2(): String = String.format(Locale.US, "%.2f", this)
private fun Double.format2(): String = String.format(Locale.US, "%.2f", this)
