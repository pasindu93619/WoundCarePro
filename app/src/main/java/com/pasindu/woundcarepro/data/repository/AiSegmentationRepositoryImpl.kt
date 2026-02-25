package com.pasindu.woundcarepro.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import com.pasindu.woundcarepro.data.local.dao.AuditLogDao
import com.pasindu.woundcarepro.data.local.dao.AiSegmentationResultDao
import com.pasindu.woundcarepro.data.local.entity.AiSegmentationResult
import com.pasindu.woundcarepro.data.local.entity.AuditLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONObject

class AiSegmentationRepositoryImpl @Inject constructor(
    private val aiSegmentationResultDao: AiSegmentationResultDao,
    private val auditLogDao: AuditLogDao,
    @ApplicationContext private val context: Context
) : AiSegmentationRepository {
    override suspend fun upsert(result: AiSegmentationResult) {
        aiSegmentationResultDao.upsert(result)
    }

    override suspend fun upsertSegmentationResult(
        assessmentId: String,
        maskBitmap: Bitmap,
        boundaryPoints: List<PointF>,
        tissueMap: Map<String, Float>,
        confidence: Float?,
        runtimeMs: Long
    ) {
        val timestamp = System.currentTimeMillis()
        val masksDir = File(context.filesDir, "ai_masks").apply { mkdirs() }
        val maskFile = File(masksDir, "${assessmentId}_${timestamp}.png")
        FileOutputStream(maskFile).use { stream ->
            maskBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }

        val boundaryJson = JSONArray().apply {
            boundaryPoints.forEach { point ->
                put(
                    JSONObject()
                        .put("x", point.x)
                        .put("y", point.y)
                )
            }
        }
        val tissueJson = JSONObject().apply {
            tissueMap.forEach { (tissue, percentage) ->
                put(tissue, percentage)
            }
        }
        val autoOutlineJson = JSONObject()
            .put("boundaryJson", boundaryJson)
            .put("tissueJson", tissueJson)
            .toString()

        val existing = aiSegmentationResultDao.getByAssessmentId(assessmentId)
        aiSegmentationResultDao.upsert(
            AiSegmentationResult(
                resultId = existing?.resultId ?: UUID.randomUUID().toString(),
                assessmentId = assessmentId,
                modelVersion = existing?.modelVersion ?: "unknown",
                autoOutlineJson = autoOutlineJson,
                maskPngPath = maskFile.absolutePath,
                confidence = confidence,
                processingTimeMs = runtimeMs,
                createdAt = timestamp
            )
        )

        val metadataJson = JSONObject()
            .put("confidence", confidence)
            .put("runtimeMs", runtimeMs)
            .toString()
        auditLogDao.insert(
            AuditLog(
                auditId = UUID.randomUUID().toString(),
                timestampMillis = timestamp,
                action = "AI_SEGMENTATION_RUN",
                patientId = null,
                assessmentId = assessmentId,
                metadataJson = metadataJson
            )
        )
    }

    override suspend fun getByAssessmentId(assessmentId: String): AiSegmentationResult? {
        return aiSegmentationResultDao.getByAssessmentId(assessmentId)
    }

    override suspend fun deleteByAssessmentId(assessmentId: String) {
        aiSegmentationResultDao.deleteByAssessmentId(assessmentId)
    }

    override suspend fun logSegmentationFailure(assessmentId: String, reason: String) {
        val metadataJson = JSONObject()
            .put("status", "failed")
            .put("reason", reason)
            .toString()
        auditLogDao.insert(
            AuditLog(
                auditId = UUID.randomUUID().toString(),
                timestampMillis = System.currentTimeMillis(),
                action = "AI_SEGMENTATION_FAILED",
                patientId = null,
                assessmentId = assessmentId,
                metadataJson = metadataJson
            )
        )
    }
}
