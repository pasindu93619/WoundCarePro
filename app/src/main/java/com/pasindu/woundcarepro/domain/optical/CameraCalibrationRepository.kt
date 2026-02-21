package com.pasindu.woundcarepro.domain.optical

import com.pasindu.woundcarepro.data.optical.calibration.StereoCalibrationData

interface CameraCalibrationRepository {
    suspend fun loadStereoCalibration(): Result<StereoCalibrationData>
}
