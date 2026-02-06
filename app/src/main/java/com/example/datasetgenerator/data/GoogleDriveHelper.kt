package com.example.datasetgenerator.data

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.example.datasetgenerator.data.AppSettings
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for Google Drive API operations.
 * Handles folder creation, file uploads, and authentication.
 */
class GoogleDriveHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleDriveHelper"
        private const val APP_NAME = "DatasetGenerator"
        private const val MIME_TYPE_FOLDER = "application/vnd.google-apps.folder"
        private const val MIME_TYPE_JPEG = "image/jpeg"
        private const val MIME_TYPE_JSON = "application/json"
        
        // Target folder ID for public upload
        private const val TARGET_FOLDER_ID = "GOOGLE_DRIVE_FOLDER_ID_PLACEHOLDER"
    }
    
    private val appSettings = AppSettings(context)
    
    /**
     * Creates and returns a Drive service instance using GoogleAccountCredential
     */
    private fun getDriveService(): Drive? {
        val email = appSettings.signedInEmail ?: return null
        
        return try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = Account(email, "com.google")
            
            Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(APP_NAME)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Drive service", e)
            null
        }
    }
    
    /**
     * Checks if user is authenticated and can access Drive
     */
    fun isAuthenticated(): Boolean {
        return appSettings.signedInEmail != null
    }
    
    /**
     * Data class for upload progress tracking
     */
    data class UploadProgress(
        val currentFile: Int,
        val totalFiles: Int,
        val currentFileName: String,
        val overallProgress: Float
    )
    
    /**
     * Upload result containing folder info
     */
    data class UploadResult(
        val success: Boolean,
        val folderId: String? = null,
        val folderName: String? = null,
        val uploadedCount: Int = 0,
        val errorMessage: String? = null
    )
    
    /**
     * Uploads all files from a directory to Google Drive.
     * Creates a folder with the specified name and uploads all files into it.
     *
     * @param framesDirectory Directory containing frame images
     * @param folderName Name for the Drive folder (object_timestamp)
     * @param onProgress Callback for progress updates
     * @return UploadResult indicating success/failure
     */
    suspend fun uploadFramesToDrive(
        framesDirectory: File,
        folderName: String,
        objectName: String,
        onProgress: (UploadProgress) -> Unit
    ): UploadResult = withContext(Dispatchers.IO) {
        val driveService = getDriveService()
        if (driveService == null) {
            return@withContext UploadResult(
                success = false,
                errorMessage = "Not authenticated. Please sign in to Google Drive."
            )
        }
        
        try {
            // Target folder ID provided by user
            val targetParentFolderId = TARGET_FOLDER_ID
            
            // 1. Create/Get folder for the Object
            val objectFolderId = getOrCreateFolder(driveService, objectName, targetParentFolderId)
            if (objectFolderId == null) {
                return@withContext UploadResult(
                    success = false,
                    errorMessage = "Failed to create object folder: $objectName"
                )
            }
            
            // 2. Create folder for this specific recording session inside the object folder
            val sessionFolderName = "${objectName}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"
            val sessionFolderId = getOrCreateFolder(driveService, sessionFolderName, objectFolderId)
            
            if (sessionFolderId == null) {
                return@withContext UploadResult(
                    success = false,
                    errorMessage = "Failed to create session folder: $sessionFolderName"
                )
            }

            
            // Get list of files to upload
            val filesToUpload = framesDirectory.listFiles()
                ?.filter { it.isFile }
                ?.sortedBy { it.name }
                ?: emptyList()
            
            if (filesToUpload.isEmpty()) {
                return@withContext UploadResult(
                    success = false,
                    errorMessage = "No files to upload"
                )
            }
            
            var uploadedCount = 0
            val totalFiles = filesToUpload.size
            
            // Upload each file
            for ((index, file) in filesToUpload.withIndex()) {
                onProgress(UploadProgress(
                    currentFile = index + 1,
                    totalFiles = totalFiles,
                    currentFileName = file.name,
                    overallProgress = index.toFloat() / totalFiles
                ))
                
                val mimeType = when (file.extension.lowercase()) {
                    "jpg", "jpeg" -> MIME_TYPE_JPEG
                    "json" -> MIME_TYPE_JSON
                    else -> "application/octet-stream"
                }
                
                val uploaded = uploadFile(driveService, file, sessionFolderId, mimeType)
                if (uploaded) {
                    uploadedCount++
                    Log.d(TAG, "Uploaded: ${file.name}")
                } else {
                    Log.w(TAG, "Failed to upload: ${file.name}")
                }
            }
            
            onProgress(UploadProgress(
                currentFile = totalFiles,
                totalFiles = totalFiles,
                currentFileName = "Complete",
                overallProgress = 1.0f
            ))
            
            UploadResult(
                success = uploadedCount == totalFiles,
                folderId = sessionFolderId,
                folderName = sessionFolderName,
                uploadedCount = uploadedCount,
                errorMessage = if (uploadedCount < totalFiles) "Some files failed to upload" else null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            UploadResult(
                success = false,
                errorMessage = "Upload error: ${e.message}"
            )
        }
    }
    
    /**
     * Gets existing folder by name or creates a new one
     */
    private fun getOrCreateFolder(
        driveService: Drive,
        folderName: String,
        parentId: String
    ): String? {
        return try {
            // Search for existing folder
            // Note: 'root' alias is handled by Drive API, but if passing a specific ID, just use it.
            val query = "name='$folderName' and mimeType='$MIME_TYPE_FOLDER' and '$parentId' in parents and trashed=false"
            
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
            
            if (result.files.isNotEmpty()) {
                Log.d(TAG, "Found existing folder: $folderName")
                return result.files[0].id
            }
            
            // Create new folder
            val fileMetadata = DriveFile().apply {
                name = folderName
                mimeType = MIME_TYPE_FOLDER
                parents = Collections.singletonList(parentId)
            }
            
            val folder = driveService.files().create(fileMetadata)
                .setFields("id")
                .execute()
            
            Log.d(TAG, "Created folder: $folderName with ID: ${folder.id}")
            folder.id
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get/create folder: $folderName", e)
            null
        }
    }
    
    /**
     * Uploads a single file to specified folder
     */
    private fun uploadFile(
        driveService: Drive,
        file: File,
        folderId: String,
        mimeType: String
    ): Boolean {
        return try {
            val fileMetadata = DriveFile().apply {
                name = file.name
                parents = Collections.singletonList(folderId)
            }
            
            val mediaContent = FileContent(mimeType, file)
            
            driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file: ${file.name}", e)
            false
        }
    }
}
