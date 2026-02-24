package com.pasindu.woundcarepro.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pasindu.woundcarepro.data.local.DEFAULT_PATIENT_ID
import com.pasindu.woundcarepro.data.local.DEFAULT_WOUND_ID
import com.pasindu.woundcarepro.data.local.DatabaseMigrations
import com.pasindu.woundcarepro.data.local.WoundCareDatabase
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepository
import com.pasindu.woundcarepro.data.local.repository.AssessmentRepositoryImpl
import com.pasindu.woundcarepro.data.local.repository.AuditRepository
import com.pasindu.woundcarepro.data.local.repository.AuditRepositoryImpl
import com.pasindu.woundcarepro.data.local.repository.ConsentRepository
import com.pasindu.woundcarepro.data.local.repository.ConsentRepositoryImpl
import com.pasindu.woundcarepro.data.local.repository.PatientRepository
import com.pasindu.woundcarepro.data.local.repository.PatientRepositoryImpl
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
        )
                        .addMigrations(
                DatabaseMigrations.MIGRATION_9_10,
                DatabaseMigrations.MIGRATION_10_11,
                DatabaseMigrations.MIGRATION_11_12,
                DatabaseMigrations.MIGRATION_12_13,
                DatabaseMigrations.MIGRATION_13_14,
                DatabaseMigrations.MIGRATION_14_15,
                DatabaseMigrations.MIGRATION_15_16,
                DatabaseMigrations.MIGRATION_16_17
            )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    seedDefaultRows(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    seedDefaultRows(db)
                }

                private fun seedDefaultRows(db: SupportSQLiteDatabase) {
                    val now = System.currentTimeMillis()
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO patients (patientId, name, createdAt)
                        VALUES (?, ?, ?)
                        """.trimIndent(),
                        arrayOf(DEFAULT_PATIENT_ID, "Anonymous Patient", 0L)
                    )
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO wounds (woundId, patientId, location, createdAtMillis)
                        VALUES (?, ?, ?, ?)
                        """.trimIndent(),
                        arrayOf(DEFAULT_WOUND_ID, DEFAULT_PATIENT_ID, "Unspecified", now)
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
    fun providePatientRepository(database: WoundCareDatabase): PatientRepository {
        return PatientRepositoryImpl(database.patientDao())
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
            patientDao = database.patientDao(),
            woundDao = database.woundDao(),
            measurementDao = database.measurementDao(),
            auditRepository = auditRepository
        )
    }
}
