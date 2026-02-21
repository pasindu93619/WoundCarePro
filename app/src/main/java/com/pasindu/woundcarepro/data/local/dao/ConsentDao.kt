package com.pasindu.woundcarepro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pasindu.woundcarepro.data.local.entity.Consent

@Dao
interface ConsentDao {
    @Upsert
    suspend fun upsert(consent: Consent)

    @Query(
        """
        SELECT * FROM consents
        WHERE patientId = :patientId AND consentType = :consentType
        ORDER BY timestampMillis DESC
        LIMIT 1
        """
    )
    suspend fun latestByType(patientId: String, consentType: String): Consent?
}
