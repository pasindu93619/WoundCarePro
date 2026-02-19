package com.pasindu.woundcarepro.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pasindu.woundcarepro.data.local.DEFAULT_PATIENT_ID
import com.pasindu.woundcarepro.data.local.DatabaseMigrations
import com.pasindu.woundcarepro.data.local.WoundCareDatabase
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepositoryImpl
import com.pasindu.woundcarepro.data.local.repository.AuditRepository
import com.pasindu.woundcarepro.data.local.repository.AuditRepositoryImpl
import com.pasindu.woundcarepro.data.local.repository.ConsentRepository
import com.pasindu.woundcarepro.data.local.repository.ConsentRepositoryImpl
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
        ).addMigrations(
            DatabaseMigrations.MIGRATION_9_10,
            DatabaseMigrations.MIGRATION_10_11,
            DatabaseMigrations.MIGRATION_11_12,
            DatabaseMigrations.MIGRATION_12_13,
            DatabaseMigrations.MIGRATION_13_14
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    seedDefaultPatient(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    seedDefaultPatient(db)
                }

                private fun seedDefaultPatient(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO patients (patientId, name, createdAt)
                        VALUES (?, ?, ?)
                        """.trimIndent(),
                        arrayOf(DEFAULT_PATIENT_ID, "Anonymous Patient", 0L)
                    )
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideAuditRepository(database: WoundCareDatabase): AuditRepository {
        return AuditRepositoryImpl(database.auditLogDao())
    }

    @Provides
    @Singleton
    fun provideConsentRepository(database: WoundCareDatabase): ConsentRepository {
        return ConsentRepositoryImpl(database.consentDao())
    }

    @Provides
    @Singleton
    fun provideAssessmentRepository(
        database: WoundCareDatabase,
        auditRepository: AuditRepository
    ): AssessmentRepository {
        return AssessmentRepositoryImpl(
            database = database,
            assessmentDao = database.assessmentDao(),
            measurementDao = database.measurementDao(),
            auditRepository = auditRepository
        )
    }
}
