package com.pasindu.woundcarepro.data.local.repository

import com.pasindu.woundcarepro.data.local.dao.AuditLogDao
import com.pasindu.woundcarepro.data.local.entity.AuditLog
import java.util.UUID

interface AuditRepository {
    suspend fun logAudit(
        action: String,
        patientId: String? = null,
        assessmentId: String? = null,
        metadataJson: String? = null
    )
}

class AuditRepositoryImpl(
    private val auditLogDao: AuditLogDao
) : AuditRepository {
    override suspend fun logAudit(
        action: String,
        patientId: String?,
        assessmentId: String?,
        metadataJson: String?
    ) {
        auditLogDao.insert(
            AuditLog(
                auditId = UUID.randomUUID().toString(),
                timestampMillis = System.currentTimeMillis(),
                action = action,
                patientId = patientId,
                assessmentId = assessmentId,
                metadataJson = metadataJson
            )
        )
    }
}
