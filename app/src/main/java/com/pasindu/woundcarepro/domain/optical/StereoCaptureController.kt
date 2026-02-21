package com.pasindu.woundcarepro.domain.optical

import android.view.Surface
import kotlinx.coroutines.flow.Flow

interface StereoCaptureController {
    suspend fun start(previewSurface: Surface? = null): Result<Unit>
    suspend fun stop(): Result<Unit>
    fun framePairsFlow(): Flow<StereoFramePair>
}
