package com.pasindu.woundcarepro.di

import android.content.Context
import androidx.room.Room
import com.pasindu.woundcarepro.data.local.WoundCareDatabase
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWoundCareDatabase(
        @ApplicationContext context: Context
    ): WoundCareDatabase {
        return Room.databaseBuilder(
            context,
            WoundCareDatabase::class.java,
            "wound-care.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideAssessmentRepository(database: WoundCareDatabase): AssessmentRepository {
        return AssessmentRepositoryImpl(database.assessmentDao())
    }
}
