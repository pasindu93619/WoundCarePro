package com.pasindu.woundcarepro.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "assessments")
data class Assessment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val status: String = "CREATED"
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

@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(
            entity = Assessment::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["assessmentId"], unique = true)]
)
data class Measurement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assessmentId: Long,
    val woundAreaPixels: Double,
    val woundAreaCm2: Double,
    val measuredAtEpochMillis: Long = System.currentTimeMillis()
)

data class ExportAssessmentMeasurementRow(
    val assessmentId: Long,
    val assessmentCreatedAtEpochMillis: Long,
    val assessmentStatus: String,
    val woundAreaPixels: Double?,
    val woundAreaCm2: Double?,
    val measuredAtEpochMillis: Long?
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

    @Insert
    suspend fun insertCalibrationParams(calibrationParams: CalibrationParams): Long

    @Query("SELECT * FROM calibration_params WHERE assessmentId = :assessmentId ORDER BY createdAtEpochMillis DESC LIMIT 1")
    suspend fun getLatestCalibrationForAssessment(assessmentId: Long): CalibrationParams?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeasurement(measurement: Measurement): Long

    @Query(
        """
        SELECT
            a.id AS assessmentId,
            a.createdAtEpochMillis AS assessmentCreatedAtEpochMillis,
            a.status AS assessmentStatus,
            m.woundAreaPixels AS woundAreaPixels,
            m.woundAreaCm2 AS woundAreaCm2,
            m.measuredAtEpochMillis AS measuredAtEpochMillis
        FROM assessments a
        LEFT JOIN measurements m ON m.assessmentId = a.id
        WHERE a.createdAtEpochMillis BETWEEN :startEpochMillis AND :endEpochMillis
        ORDER BY a.createdAtEpochMillis ASC
        """
    )
    suspend fun getAssessmentsWithMeasurementsForRange(
        startEpochMillis: Long,
        endEpochMillis: Long
    ): List<ExportAssessmentMeasurementRow>
}

@Database(
    entities = [Assessment::class, ImageAsset::class, CalibrationParams::class, Measurement::class],
    version = 4,
    exportSchema = false
)
abstract class WoundCareDatabase : RoomDatabase() {
    abstract fun assessmentDao(): AssessmentDao
}
