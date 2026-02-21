package com.pasindu.woundcarepro.data.optical.calibration

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.SizeF
import com.pasindu.woundcarepro.domain.optical.CameraCalibrationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class CameraCalibrationRepositoryImpl(
    private val cameraManager: CameraManager
) : CameraCalibrationRepository {

    override suspend fun loadStereoCalibration(): Result<StereoCalibrationData> = withContext(Dispatchers.IO) {
        runCatching {
            val logicalCameraId = findLogicalMultiCameraId()
                ?: throw IllegalStateException(
                    "No logical multi-camera found (missing REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)"
                )

            val logicalCharacteristics = cameraManager.getCameraCharacteristics(logicalCameraId)
            val physicalCameraIds = getPhysicalCameraIds(logicalCharacteristics)
            if (physicalCameraIds.size < 2) {
                throw IllegalStateException(
                    "Not enough physical cameras for logical camera $logicalCameraId (found ${physicalCameraIds.size})"
                )
            }

            val physicalInfos = physicalCameraIds.map { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?: throw IllegalStateException("Missing focal lengths for physical camera $id")
                val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    ?: throw IllegalStateException("Missing sensor physical size for physical camera $id")
                PhysicalCameraInfo(
                    id = id,
                    focalLengthsMm = focalLengths,
                    sensorPhysicalSizeMm = sensorSize,
                    intrinsic = characteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION),
                    distortion = characteristics.get(CameraCharacteristics.LENS_DISTORTION),
                    poseTranslation = characteristics.get(CameraCharacteristics.LENS_POSE_TRANSLATION)
                )
            }

            val main = physicalInfos.maxByOrNull { it.maxFocalLengthMm }
            val ultra = physicalInfos.minByOrNull { it.minFocalLengthMm }

            if (main == null || ultra == null || main.id == ultra.id) {
                throw IllegalStateException("Cannot determine distinct main and ultra-wide physical cameras")
            }

            val baselineTranslation = computeBaselineTranslation(main.poseTranslation, ultra.poseTranslation)
            val baselineMm = baselineTranslation?.let(::toMillimeters)

            StereoCalibrationData(
                logicalCameraId = logicalCameraId,
                mainCameraId = main.id,
                ultraWideCameraId = ultra.id,
                mainFocalLengthsMm = main.focalLengthsMm,
                ultraFocalLengthsMm = ultra.focalLengthsMm,
                mainSensorPhysicalSizeMm = main.sensorPhysicalSizeMm,
                ultraSensorPhysicalSizeMm = ultra.sensorPhysicalSizeMm,
                mainIntrinsic = main.intrinsic,
                ultraIntrinsic = ultra.intrinsic,
                mainDistortion = main.distortion,
                ultraDistortion = ultra.distortion,
                baselineTranslation = baselineTranslation,
                baselineMm = baselineMm,
                fxPixelsMain = main.intrinsic?.getOrNull(0),
                fyPixelsMain = main.intrinsic?.getOrNull(1),
                fxPixelsUltra = ultra.intrinsic?.getOrNull(0),
                fyPixelsUltra = ultra.intrinsic?.getOrNull(1)
                // TODO: If intrinsic is null, compute approx fx/fy once output image resolution is known.
            )
        }
    }

    private fun findLogicalMultiCameraId(): String? {
        return cameraManager.cameraIdList.firstOrNull { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                ?: return@firstOrNull false
            capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
        }
    }

    private fun getPhysicalCameraIds(characteristics: CameraCharacteristics): Set<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            characteristics.physicalCameraIds
        } else {
            emptySet()
        }
    }

    private fun computeBaselineTranslation(
        mainTranslation: FloatArray?,
        ultraTranslation: FloatArray?
    ): FloatArray? {
        return when {
            mainTranslation != null && ultraTranslation != null &&
                mainTranslation.size >= 3 && ultraTranslation.size >= 3 -> {
                floatArrayOf(
                    mainTranslation[0] - ultraTranslation[0],
                    mainTranslation[1] - ultraTranslation[1],
                    mainTranslation[2] - ultraTranslation[2]
                )
            }

            mainTranslation != null && mainTranslation.size >= 3 -> mainTranslation.copyOf(3)
            ultraTranslation != null && ultraTranslation.size >= 3 -> ultraTranslation.copyOf(3)
            else -> null
        }
    }

    private fun toMillimeters(translation: FloatArray): Float {
        val magnitude = sqrt(
            (translation[0] * translation[0]) +
                (translation[1] * translation[1]) +
                (translation[2] * translation[2])
        )

        // Camera2 pose translation is usually in meters, but some vendor implementations may behave differently.
        // If the magnitude already looks like millimeters (> 1.0 is unlikely for meters in phone camera baselines),
        // keep it as-is; otherwise convert meters to millimeters.
        return if (magnitude in 0f..0.5f) magnitude * 1000f else magnitude
    }

    private data class PhysicalCameraInfo(
        val id: String,
        val focalLengthsMm: FloatArray,
        val sensorPhysicalSizeMm: SizeF,
        val intrinsic: FloatArray?,
        val distortion: FloatArray?,
        val poseTranslation: FloatArray?
    ) {
        val maxFocalLengthMm: Float
            get() = focalLengthsMm.maxOrNull() ?: Float.MIN_VALUE

        val minFocalLengthMm: Float
            get() = focalLengthsMm.minOrNull() ?: Float.MAX_VALUE
    }
}
