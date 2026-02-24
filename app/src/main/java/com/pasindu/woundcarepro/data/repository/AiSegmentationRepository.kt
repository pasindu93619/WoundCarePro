package com.pasindu.woundcarepro.data.repository

import com.pasindu.woundcarepro.data.local.entity.AiSegmentationResult

interface AiSegmentationRepository {
    suspend fun upsert(result: AiSegmentationResult)
    suspend fun getByAssessmentId(assessmentId: String): AiSegmentationResult?
    suspend fun deleteByAssessmentId(assessmentId: String)
}
