package com.example.datasetgenerator.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoRepository(private val context: Context, val dao: RecordingDao) {

    private val videosDir = File(context.getExternalFilesDir(null), "DatasetGenerator/Videos")
    private val framesDir = File(context.getExternalFilesDir(null), "DatasetGenerator/TempFrames")

    init {
        if (!videosDir.exists()) videosDir.mkdirs()
        if (!framesDir.exists()) framesDir.mkdirs()
    }

    fun createVideoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(videosDir, "VIDEO_${timeStamp}.mp4")
    }
    
    fun getFramesFolder(recordingId: String): File {
        val folder = File(framesDir, recordingId)
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    suspend fun saveRecordingMetadata(
        id: String,
        objectName: String,
        videoPath: String,
        frameCount: Int
    ) {
        val entity = RecordingEntity(
            id = id,
            objectName = objectName,
            videoPath = videoPath,
            frameFolderPath = getFramesFolder(id).absolutePath,
            uploadStatus = UploadStatus.PENDING,
            timestamp = System.currentTimeMillis(),
            frameCount = frameCount
        )
        dao.insertRecording(entity)
    }

    suspend fun discardVideo(videoPath: String) {
        withContext(Dispatchers.IO) {
            val file = File(videoPath)
            if (file.exists()) {
                file.delete()
            }
        }
    }
    
    // Clear old temporary frames if needed
    suspend fun clearFrames(recordingId: String) {
        withContext(Dispatchers.IO) {
            val folder = getFramesFolder(recordingId)
            folder.deleteRecursively()
        }
    }
    
    suspend fun getAllVideos(): List<File> {
        return withContext(Dispatchers.IO) {
            if (videosDir.exists()) {
                videosDir.listFiles()
                    ?.filter { it.isFile && it.extension == "mp4" }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()
            } else {
                emptyList()
            }
        }
    }
}
