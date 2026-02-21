package com.pasindu.woundcarepro.data.local.repository

import com.pasindu.woundcarepro.data.local.dao.ConsentDao
import com.pasindu.woundcarepro.data.local.entity.Consent
import java.util.UUID

interface ConsentRepository {
    suspend fun recordConsent(
        patientId: String,
        consentType: String,
        consentGiven: Boolean,
        note: String?
    )

    suspend fun hasGrantedConsent(patientId: String, consentType: String): Boolean
}

class ConsentRepositoryImpl(
    private val consentDao: ConsentDao
) : ConsentRepository {
    override suspend fun recordConsent(
        patientId: String,
        consentType: String,
        consentGiven: Boolean,
        note: String?
    ) {
        consentDao.upsert(
            Consent(
                consentId = UUID.randomUUID().toString(),
                patientId = patientId,
                timestampMillis = System.currentTimeMillis(),
                consentGiven = consentGiven,
                consentType = consentType,
                note = note
            )
        )
    }

    override suspend fun hasGrantedConsent(patientId: String, consentType: String): Boolean {
        return consentDao.latestByType(patientId, consentType)?.consentGiven == true
    }
}
