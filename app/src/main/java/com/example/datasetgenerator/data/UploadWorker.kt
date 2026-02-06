package com.example.datasetgenerator.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.ForegroundInfo
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.pm.ServiceInfo
import com.example.datasetgenerator.domain.FrameExtractor
import java.io.File

/**
 * Background worker for uploading frame data to Google Drive.
 * Implements retry logic and foreground notification for long-running uploads.
 */
class UploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "UploadWorker"
        private const val NOTIFICATION_CHANNEL_ID = "cattle_upload_channel"
        private const val NOTIFICATION_ID = 1001
        
        // Input data keys
        const val KEY_FOLDER_PATH = "folderPath"
        const val KEY_FOLDER_NAME = "folderName"
        const val KEY_OBJECT_NAME = "objectName"
        
        // Output data keys
        const val KEY_SUCCESS = "success"
        const val KEY_UPLOADED_COUNT = "uploadedCount"
        const val KEY_ERROR_MESSAGE = "errorMessage"
    }
    
    private val driveHelper = GoogleDriveHelper(applicationContext)
    
    override suspend fun doWork(): Result {
        val folderPath = inputData.getString(KEY_FOLDER_PATH) 
            ?: return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Missing folder path"))
        val folderName = inputData.getString(KEY_FOLDER_NAME) 
            ?: return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Missing folder name"))
        val objectName = inputData.getString(KEY_OBJECT_NAME)
            ?: "Unknown"
        
        val framesDirectory = File(folderPath)
        if (!framesDirectory.exists() || !framesDirectory.isDirectory) {
            Log.e(TAG, "Invalid frames directory: $folderPath")
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Frames directory not found"))
        }
        
        // Check authentication
        if (!driveHelper.isAuthenticated()) {
            Log.w(TAG, "Not authenticated to Google Drive")
            return Result.retry() // Will retry when user signs in
        }
        
        // Create notification channel
        createNotificationChannel()
        
        // Set foreground with notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setForeground(createForegroundInfo("Starting upload...", ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
        } else {
            setForeground(createForegroundInfo("Starting upload..."))
        }
        
        Log.d(TAG, "Starting upload for folder: $folderName at $folderPath")
        
        // Perform upload
        val result = driveHelper.uploadFramesToDrive(
            framesDirectory = framesDirectory,
            folderName = folderName,
            objectName = objectName,
            onProgress = { progress ->
                setProgressAsync(workDataOf(
                    "currentFile" to progress.currentFile,
                    "totalFiles" to progress.totalFiles,
                    "progress" to progress.overallProgress
                ))
                updateNotification("Uploading ${progress.currentFile}/${progress.totalFiles}: ${progress.currentFileName}")
            }
        )
        
        return if (result.success) {
            Log.d(TAG, "Upload successful: ${result.uploadedCount} files uploaded")
            
            // Cleanup temp frames after successful upload
            FrameExtractor.cleanupFrames(framesDirectory)
            
            Result.success(workDataOf(
                KEY_SUCCESS to true,
                KEY_UPLOADED_COUNT to result.uploadedCount
            ))
        } else {
            Log.e(TAG, "Upload failed: ${result.errorMessage}")
            
            // Retry on failure (WorkManager will handle backoff)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(workDataOf(
                    KEY_SUCCESS to false,
                    KEY_ERROR_MESSAGE to result.errorMessage
                ))
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                    "Upload Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of dataset uploads"
            }
            
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createForegroundInfo(message: String, type: Int = 0): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Uploading Files")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .setProgress(100, 0, true)
            .build()
        
        return if (type != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, type)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
    
    private fun updateNotification(message: String) {
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Uploading Files")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
}
