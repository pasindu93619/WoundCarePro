package com.pasindu.woundcarepro.data.optical.capture

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import com.pasindu.woundcarepro.domain.optical.CameraCalibrationRepository
import com.pasindu.woundcarepro.domain.optical.StereoCaptureController
import com.pasindu.woundcarepro.domain.optical.StereoFramePair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.TreeMap
import kotlin.coroutines.resume
import kotlin.math.abs

class Camera2StereoCaptureController(
    private val cameraManager: CameraManager,
    private val calibrationRepository: CameraCalibrationRepository
) : StereoCaptureController {

    private val pairingToleranceNs = 10_000_000L
    private val maxBufferedFrames = 5

    private val framePairEvents = MutableSharedFlow<StereoFramePair>(extraBufferCapacity = 8)
    private val processingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pairingMutex = Mutex()

    private val pendingMain = TreeMap<Long, PendingFrame>()
    private val pendingUltra = TreeMap<Long, PendingFrame>()

    @Volatile
    private var cameraThread: HandlerThread? = null

    @Volatile
    private var cameraHandler: Handler? = null

    @Volatile
    private var cameraDevice: CameraDevice? = null

    @Volatile
    private var captureSession: CameraCaptureSession? = null

    @Volatile
    private var mainReader: ImageReader? = null

    @Volatile
    private var ultraReader: ImageReader? = null

    @Volatile
    private var isStarted = false

    override fun framePairsFlow(): Flow<StereoFramePair> = framePairEvents.asSharedFlow()

    @SuppressLint("MissingPermission")
    override suspend fun start(previewSurface: Surface?): Result<Unit> = withContext(Dispatchers.IO) {
        if (isStarted) return@withContext Result.success(Unit)

        runCatching {
            val calibration = calibrationRepository.loadStereoCalibration().getOrElse { error ->
                throw IllegalStateException("Unable to load stereo calibration", error)
            }

            val yuvSize = selectCaptureSize(calibration.logicalCameraId)
                ?: throw IllegalStateException("No suitable YUV_420_888 size for logical camera ${calibration.logicalCameraId}")

            setupCameraThread()
            val handler = cameraHandler ?: throw IllegalStateException("Camera handler initialization failed")

            mainReader = ImageReader.newInstance(yuvSize.width, yuvSize.height, ImageFormat.YUV_420_888, 4)
            ultraReader = ImageReader.newInstance(yuvSize.width, yuvSize.height, ImageFormat.YUV_420_888, 4)

            mainReader?.setOnImageAvailableListener(
                { reader ->
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    processingScope.launch { processIncomingImage(CameraRole.MAIN, image) }
                },
                handler
            )

            ultraReader?.setOnImageAvailableListener(
                { reader ->
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    processingScope.launch { processIncomingImage(CameraRole.ULTRA, image) }
                },
                handler
            )

            val openedCamera = openCamera(calibration.logicalCameraId, handler)
            cameraDevice = openedCamera

            val mainSurface = mainReader?.surface ?: throw IllegalStateException("Main ImageReader surface unavailable")
            val ultraSurface = ultraReader?.surface ?: throw IllegalStateException("Ultra ImageReader surface unavailable")

            val session = createCaptureSession(
                device = openedCamera,
                mainSurface = mainSurface,
                ultraSurface = ultraSurface,
                mainPhysicalId = calibration.mainCameraId,
                ultraPhysicalId = calibration.ultraWideCameraId,
                previewSurface = previewSurface,
                handler = handler
            )
            captureSession = session

            val requestBuilder = openedCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(mainSurface)
                addTarget(ultraSurface)
                previewSurface?.let(::addTarget)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }

            session.setRepeatingRequest(requestBuilder.build(), null, handler)
            isStarted = true
            Log.i(TAG, "Stereo capture started at ${yuvSize.width}x${yuvSize.height}")
            Unit
        }.onFailure {
            safeCloseAll()
        }
    }

    override suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            safeCloseAll()
            Log.i(TAG, "Stereo capture stopped")
            Unit
        }
    }

    private suspend fun selectCaptureSize(logicalCameraId: String): Size? {
        val characteristics = cameraManager.getCameraCharacteristics(logicalCameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return null
        val yuvSizes = map.getOutputSizes(ImageFormat.YUV_420_888)?.toList().orEmpty()
        if (yuvSizes.isEmpty()) return null

        return yuvSizes.firstOrNull { it.width == 1280 && it.height == 720 }
            ?: yuvSizes
                .filter { it.width <= 1920 && it.height <= 1080 }
                .maxWithOrNull(compareBy<Size> { it.width * it.height }.thenBy { it.width })
            ?: yuvSizes.minByOrNull { it.width * it.height }
    }

    private fun setupCameraThread() {
        if (cameraThread != null && cameraHandler != null) return
        cameraThread = HandlerThread("StereoCamera2Thread").also { it.start() }
        cameraHandler = Handler(cameraThread!!.looper)
    }

    private suspend fun openCamera(cameraId: String, handler: Handler): CameraDevice {
        return suspendCancellableCoroutine { continuation ->
            try {
                cameraManager.openCamera(
                    cameraId,
                    object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            continuation.resume(camera)
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            camera.close()
                            if (continuation.isActive) {
                                continuation.resumeWith(
                                    Result.failure(IllegalStateException("Logical camera $cameraId disconnected"))
                                )
                            }
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            camera.close()
                            if (continuation.isActive) {
                                continuation.resumeWith(
                                    Result.failure(IllegalStateException("Failed to open logical camera $cameraId. error=$error"))
                                )
                            }
                        }
                    },
                    handler
                )
            } catch (security: SecurityException) {
                continuation.resumeWith(Result.failure(IllegalStateException("Camera permission denied", security)))
            } catch (throwable: Throwable) {
                continuation.resumeWith(
                    Result.failure(IllegalStateException("Unable to open logical camera $cameraId", throwable))
                )
            }
        }
    }

    private suspend fun createCaptureSession(
        device: CameraDevice,
        mainSurface: Surface,
        ultraSurface: Surface,
        mainPhysicalId: String,
        ultraPhysicalId: String,
        previewSurface: Surface?,
        handler: Handler
    ): CameraCaptureSession {
        return suspendCancellableCoroutine { continuation ->
            try {
                val callback = object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        continuation.resume(session)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        session.close()
                        if (continuation.isActive) {
                            continuation.resumeWith(
                                Result.failure(IllegalStateException("Stereo capture session configuration failed"))
                            )
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val mainOutput = OutputConfiguration(mainSurface).apply { setPhysicalCameraId(mainPhysicalId) }
                    val ultraOutput = OutputConfiguration(ultraSurface).apply { setPhysicalCameraId(ultraPhysicalId) }
                    val outputConfigurations = mutableListOf(mainOutput, ultraOutput)
                    if (previewSurface != null) {
                        outputConfigurations += OutputConfiguration(previewSurface)
                    }
                    val executor = java.util.concurrent.Executor { runnable -> handler.post(runnable) }
                    val sessionConfiguration = SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        outputConfigurations,
                        executor,
                        callback
                    )
                    device.createCaptureSession(sessionConfiguration)
                } else {
                    Log.w(TAG, "Physical camera routing requires API 28+. Falling back to logical outputs only.")
                    val surfaces = buildList {
                        add(mainSurface)
                        add(ultraSurface)
                        if (previewSurface != null) add(previewSurface)
                    }
                    device.createCaptureSession(surfaces, callback, handler)
                }
            } catch (throwable: Throwable) {
                continuation.resumeWith(
                    Result.failure(IllegalStateException("Unable to configure stereo session", throwable))
                )
            }
        }
    }

    private suspend fun processIncomingImage(role: CameraRole, image: Image) {
        val pending = try {
            val packed = packYuv420(image)
            PendingFrame(image.timestamp, packed, image.width, image.height)
        } finally {
            image.close()
        }

        pairingMutex.withLock {
            val ownBuffer = if (role == CameraRole.MAIN) pendingMain else pendingUltra
            val oppositeBuffer = if (role == CameraRole.MAIN) pendingUltra else pendingMain
            ownBuffer[pending.timestampNs] = pending
            pruneBuffer(ownBuffer)

            val pairMatch = oppositeBuffer.entries
                .minByOrNull { (_, candidate) -> abs(candidate.timestampNs - pending.timestampNs) }

            if (pairMatch != null) {
                val delta = abs(pairMatch.value.timestampNs - pending.timestampNs)
                if (delta <= pairingToleranceNs) {
                    oppositeBuffer.remove(pairMatch.key)
                    ownBuffer.remove(pending.timestampNs)

                    val main = if (role == CameraRole.MAIN) pending else pairMatch.value
                    val ultra = if (role == CameraRole.ULTRA) pending else pairMatch.value
                    framePairEvents.tryEmit(
                        StereoFramePair(
                            timestampNs = maxOf(main.timestampNs, ultra.timestampNs),
                            mainYuv = main.bytes,
                            ultraYuv = ultra.bytes,
                            width = main.width,
                            height = main.height
                        )
                    )
                }
            }

            pruneBuffer(oppositeBuffer)
        }
    }

    private fun packYuv420(image: Image): ByteArray {
        val ySize = image.width * image.height
        val chromaSize = ySize / 2
        val out = ByteArray(ySize + chromaSize)

        copyPlane(image.planes[0], image.width, image.height, out, 0)
        val uOffset = ySize
        val uSize = copyPlane(image.planes[1], image.width / 2, image.height / 2, out, uOffset)
        copyPlane(image.planes[2], image.width / 2, image.height / 2, out, uOffset + uSize)
        return out
    }

    private fun copyPlane(
        plane: Image.Plane,
        width: Int,
        height: Int,
        out: ByteArray,
        outOffset: Int
    ): Int {
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        var outputIndex = outOffset
        val rowData = ByteArray(rowStride)

        for (row in 0 until height) {
            if (pixelStride == 1) {
                val length = width
                buffer.get(out, outputIndex, length)
                outputIndex += length
                if (row < height - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            } else {
                val length = (width - 1) * pixelStride + 1
                buffer.get(rowData, 0, length)
                for (col in 0 until width) {
                    out[outputIndex++] = rowData[col * pixelStride]
                }
                if (row < height - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }
        return outputIndex - outOffset
    }

    private fun pruneBuffer(buffer: TreeMap<Long, PendingFrame>) {
        while (buffer.size > maxBufferedFrames) {
            buffer.pollFirstEntry()
        }
    }

    private fun safeCloseAll() {
        isStarted = false

        captureSession?.close()
        captureSession = null

        cameraDevice?.close()
        cameraDevice = null

        mainReader?.close()
        mainReader = null

        ultraReader?.close()
        ultraReader = null

        runBlocking {
            pairingMutex.withLock {
                pendingMain.clear()
                pendingUltra.clear()
            }
        }

        cameraThread?.quitSafely()
        cameraThread = null
        cameraHandler = null
    }

    private enum class CameraRole {
        MAIN,
        ULTRA
    }

    private data class PendingFrame(
        val timestampNs: Long,
        val bytes: ByteArray,
        val width: Int,
        val height: Int
    )

    private companion object {
        const val TAG = "Camera2StereoCapture"
    }
}
