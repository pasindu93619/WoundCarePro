package com.pasindu.woundcarepro.data.optical.calibration

import android.util.SizeF

data class StereoCalibrationData(
    val logicalCameraId: String,
    val mainCameraId: String,
    val ultraWideCameraId: String,
    val mainFocalLengthsMm: FloatArray,
    val ultraFocalLengthsMm: FloatArray,
    val mainSensorPhysicalSizeMm: SizeF,
    val ultraSensorPhysicalSizeMm: SizeF,
    val mainIntrinsic: FloatArray?,
    val ultraIntrinsic: FloatArray?,
    val mainDistortion: FloatArray?,
    val ultraDistortion: FloatArray?,
    val baselineTranslation: FloatArray?,
    val baselineMm: Float?,
    val fxPixelsMain: Float?,
    val fyPixelsMain: Float?,
    val fxPixelsUltra: Float?,
    val fyPixelsUltra: Float?
)
