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
}

@Database(
    entities = [Assessment::class, ImageAsset::class],
    version = 2,
    exportSchema = false
)
abstract class WoundCareDatabase : RoomDatabase() {
    abstract fun assessmentDao(): AssessmentDao
}
