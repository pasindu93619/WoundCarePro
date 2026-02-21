package com.pasindu.woundcarepro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pasindu.woundcarepro.data.local.entity.AuditLog

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insert(auditLog: AuditLog)

    @Query("SELECT * FROM audit_logs ORDER BY timestampMillis DESC")
    suspend fun listRecent(): List<AuditLog>
}
