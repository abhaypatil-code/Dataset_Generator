package com.example.datasetgenerator.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class UploadStatus {
    PENDING, UPLOADING, COMPLETED, FAILED
}

class Converters {
    @TypeConverter
    fun fromStatus(status: UploadStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(status: String): UploadStatus {
        return UploadStatus.valueOf(status)
    }
}

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val id: String, // Timestamp-based, ensuring uniqueness
    val objectName: String,
    val videoPath: String,
    val frameFolderPath: String,
    val uploadStatus: UploadStatus,
    val timestamp: Long,
    val frameCount: Int = 0
)
