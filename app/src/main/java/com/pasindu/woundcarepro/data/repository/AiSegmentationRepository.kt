package com.pasindu.woundcarepro.data.repository

import android.graphics.Bitmap
import android.graphics.PointF
import com.pasindu.woundcarepro.data.local.entity.AiSegmentationResult

interface AiSegmentationRepository {
    suspend fun upsert(result: AiSegmentationResult)
    suspend fun upsertSegmentationResult(
        assessmentId: String,
        maskBitmap: Bitmap,
        boundaryPoints: List<PointF>,
        tissueMap: Map<String, Float>,
        confidence: Float?,
        runtimeMs: Long
    )
    suspend fun getByAssessmentId(assessmentId: String): AiSegmentationResult?
    suspend fun deleteByAssessmentId(assessmentId: String)
}
