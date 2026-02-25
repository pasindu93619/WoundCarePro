package com.pasindu.woundcarepro.di

import com.pasindu.woundcarepro.data.local.WoundCareDatabase
import com.pasindu.woundcarepro.data.local.dao.AiSegmentationResultDao
import com.pasindu.woundcarepro.data.local.dao.AuditLogDao
import com.pasindu.woundcarepro.data.repository.AiSegmentationRepository
import com.pasindu.woundcarepro.data.repository.AiSegmentationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiSegmentationDaoModule {

    @Provides
    @Singleton
    fun provideAiSegmentationResultDao(
        database: WoundCareDatabase
    ): AiSegmentationResultDao {
        return database.aiSegmentationResultDao()
    }

    @Provides
    @Singleton
    fun provideAuditLogDao(
        database: WoundCareDatabase
    ): AuditLogDao {
        return database.auditLogDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AiSegmentationRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAiSegmentationRepository(
        aiSegmentationRepositoryImpl: AiSegmentationRepositoryImpl
    ): AiSegmentationRepository
}
