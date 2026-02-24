package com.pasindu.woundcarepro.domain.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WoundSegmentationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        // Placeholder model asset path (model file not added yet): models/wound_segmentation.tflite
        const val MODEL_PATH: String = "models/wound_segmentation.tflite"

        // TODO: Replace with actual model input width once model contract is finalized.
        const val INPUT_WIDTH: Int = 0

        // TODO: Replace with actual model input height once model contract is finalized.
        const val INPUT_HEIGHT: Int = 0
    }

    suspend fun segment(imagePath: String): Result<Unit> {
        // Keep parameter referenced to avoid lint warnings until inference is implemented.
        val requestedImagePath = imagePath
        // TODO: Create and manage TensorFlow Lite Interpreter lifecycle.
        return Result.failure(
            UnsupportedOperationException(
                "WoundSegmentationEngine.segment is not implemented yet for path: $requestedImagePath"
            )
        )
    }
}
