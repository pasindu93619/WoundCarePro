package com.pasindu.woundcarepro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pasindu.woundcarepro.data.local.entity.AiSegmentationResult

@Dao
interface AiSegmentationResultDao {
    @Upsert
    suspend fun upsert(result: AiSegmentationResult)

    @Query("SELECT * FROM ai_segmentation_results WHERE assessmentId = :assessmentId LIMIT 1")
    suspend fun getByAssessmentId(assessmentId: String): AiSegmentationResult?

    @Query("DELETE FROM ai_segmentation_results WHERE assessmentId = :assessmentId")
    suspend fun deleteByAssessmentId(assessmentId: String)
}
