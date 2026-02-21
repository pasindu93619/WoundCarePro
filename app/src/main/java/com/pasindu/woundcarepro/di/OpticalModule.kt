package com.pasindu.woundcarepro.di

import android.content.Context
import android.hardware.camera2.CameraManager
import com.pasindu.woundcarepro.data.optical.calibration.CameraCalibrationRepositoryImpl
import com.pasindu.woundcarepro.domain.optical.CameraCalibrationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OpticalModule {

    @Provides
    @Singleton
    fun provideCameraManager(@ApplicationContext context: Context): CameraManager {
        return context.getSystemService(CameraManager::class.java)
            ?: throw IllegalStateException("CameraManager service is unavailable")
    }

    @Provides
    @Singleton
    fun provideCameraCalibrationRepository(
        cameraManager: CameraManager
    ): CameraCalibrationRepository {
        return CameraCalibrationRepositoryImpl(cameraManager)
    }
}
