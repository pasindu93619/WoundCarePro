package com.pasindu.woundcarepro.data.repository

import com.pasindu.woundcarepro.data.local.dao.AiSegmentationResultDao
import com.pasindu.woundcarepro.data.local.entity.AiSegmentationResult
import javax.inject.Inject

class AiSegmentationRepositoryImpl @Inject constructor(
    private val aiSegmentationResultDao: AiSegmentationResultDao
) : AiSegmentationRepository {
    override suspend fun upsert(result: AiSegmentationResult) {
        aiSegmentationResultDao.upsert(result)
    }

    override suspend fun getByAssessmentId(assessmentId: String): AiSegmentationResult? {
        return aiSegmentationResultDao.getByAssessmentId(assessmentId)
    }

    override suspend fun deleteByAssessmentId(assessmentId: String) {
        aiSegmentationResultDao.deleteByAssessmentId(assessmentId)
    }
}
