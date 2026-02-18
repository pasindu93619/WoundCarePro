package com.pasindu.woundcarepro.data.local.repository

import com.pasindu.woundcarepro.data.local.dao.AssessmentDao
import com.pasindu.woundcarepro.data.local.entity.Assessment

interface AssessmentRepository {
    suspend fun upsert(assessment: Assessment)
    suspend fun getById(assessmentId: String): Assessment?
    suspend fun listByPatient(patientId: String): List<Assessment>
    suspend fun listRecent(): List<Assessment>
    suspend fun delete(assessmentId: String)
}

class AssessmentRepositoryImpl(
    private val assessmentDao: AssessmentDao
) : AssessmentRepository {
    override suspend fun upsert(assessment: Assessment) = assessmentDao.upsert(assessment)

    override suspend fun getById(assessmentId: String): Assessment? = assessmentDao.getById(assessmentId)

    override suspend fun listByPatient(patientId: String): List<Assessment> = assessmentDao.listByPatient(patientId)

    override suspend fun listRecent(): List<Assessment> = assessmentDao.listRecent()

    override suspend fun delete(assessmentId: String) = assessmentDao.delete(assessmentId)
}
