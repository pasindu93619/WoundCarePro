package com.pasindu.woundcarepro.data.local.repository

import com.pasindu.woundcarepro.data.local.dao.PatientDao
import com.pasindu.woundcarepro.data.local.entity.Patient

interface PatientRepository {
    suspend fun upsert(patient: Patient)
    suspend fun getById(patientId: String): Patient?
    suspend fun listRecent(): List<Patient>
    suspend fun delete(patientId: String)
}

class PatientRepositoryImpl(
    private val patientDao: PatientDao
) : PatientRepository {
    override suspend fun upsert(patient: Patient) = patientDao.upsert(patient)

    override suspend fun getById(patientId: String): Patient? = patientDao.getById(patientId)

    override suspend fun listRecent(): List<Patient> = patientDao.listRecent()

    override suspend fun delete(patientId: String) = patientDao.delete(patientId)
}
