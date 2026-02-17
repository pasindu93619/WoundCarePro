package com.pasindu.woundcarepro.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.pasindu.woundcarepro.data.local.dao.MeasurementDao
import com.pasindu.woundcarepro.data.local.dao.PatientDao
import com.pasindu.woundcarepro.data.local.entity.Measurement
import com.pasindu.woundcarepro.data.local.entity.Patient

@Entity(tableName = "assessments")
data class Assessment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: String = "PATIENT-001",
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val status: String = "CREATED",
    val woundAreaCm2: Double? = null
)

data class PatientAreaTrendPoint(
    val assessmentId: Long,
    val patientId: String,
    val createdAtEpochMillis: Long,
    val woundAreaCm2: Double
)

@Entity(
    tableName = "image_assets",
    foreignKeys = [
        ForeignKey(
            entity = Assessment::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["assessmentId"])]
)
data class ImageAsset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assessmentId: Long,
    val filePath: String,
    val capturedAtEpochMillis: Long = System.currentTimeMillis(),
    val lightingQc: String = "PENDING",
    val focusQc: String = "PENDING",
    val orientationQc: String = "PENDING"
)

@Entity(
    tableName = "calibration_params",
    foreignKeys = [
        ForeignKey(
            entity = Assessment::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["assessmentId"])]
)
data class CalibrationParams(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assessmentId: Long,
    val referenceLengthPixels: Double,
    val referenceLengthCm: Double,
    val cmPerPixel: Double,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@Dao
interface AssessmentDao {
    @Insert
    suspend fun insertAssessment(assessment: Assessment): Long

    @Insert
    suspend fun insertImageAsset(imageAsset: ImageAsset): Long

    @Query("SELECT * FROM image_assets WHERE assessmentId = :assessmentId ORDER BY capturedAtEpochMillis DESC")
    suspend fun getAssetsForAssessment(assessmentId: Long): List<ImageAsset>

    @Query("SELECT * FROM image_assets WHERE assessmentId = :assessmentId ORDER BY capturedAtEpochMillis DESC LIMIT 1")
    suspend fun getLatestAssetForAssessment(assessmentId: Long): ImageAsset?

    @Query("UPDATE assessments SET status = :status WHERE id = :assessmentId")
    suspend fun updateAssessmentStatus(assessmentId: Long, status: String)

    @Query("UPDATE assessments SET woundAreaCm2 = :woundAreaCm2, status = :status WHERE id = :assessmentId")
    suspend fun saveMeasurementResult(assessmentId: Long, woundAreaCm2: Double, status: String = "MEASURED")

    @Insert
    suspend fun insertCalibrationParams(calibrationParams: CalibrationParams): Long

    @Query("SELECT * FROM calibration_params WHERE assessmentId = :assessmentId ORDER BY createdAtEpochMillis DESC LIMIT 1")
    suspend fun getLatestCalibrationForAssessment(assessmentId: Long): CalibrationParams?

    @Query("SELECT * FROM assessments WHERE woundAreaCm2 IS NOT NULL ORDER BY createdAtEpochMillis DESC")
    suspend fun getMeasuredAssessments(): List<Assessment>

    @Query(
        """
        SELECT id AS assessmentId, patientId, createdAtEpochMillis, woundAreaCm2
        FROM assessments
        WHERE patientId = :patientId AND woundAreaCm2 IS NOT NULL
        ORDER BY createdAtEpochMillis ASC
        """
    )
    suspend fun getTrendPointsForPatient(patientId: String): List<PatientAreaTrendPoint>
}

@Database(
    entities = [
        Assessment::class,
        ImageAsset::class,
        CalibrationParams::class,
        Patient::class,
        com.pasindu.woundcarepro.data.local.entity.Assessment::class,
        Measurement::class
    ],
    version = 5,
    exportSchema = false
)
abstract class WoundCareDatabase : RoomDatabase() {
    abstract fun assessmentDao(): AssessmentDao
    abstract fun patientDao(): PatientDao
    abstract fun patientAssessmentDao(): com.pasindu.woundcarepro.data.local.dao.AssessmentDao
    abstract fun measurementDao(): MeasurementDao
}
