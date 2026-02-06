package com.example.datasetgenerator.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

/**
 * Helper class for handling local storage operations.
 * Safely saves files to public directories (Documents) accessible to the user.
 */
object LocalStorageHelper {
    
    private const val TAG = "LocalStorageHelper"
    private const val APP_FOLDER_NAME = "DatasetGenerator"

    /**
     * Copies a directory of frames to the public Documents directory.
     * Returns the absolute path where files were saved, or null on failure.
     */
    fun saveFramesToPublicStorage(
        context: Context,
        sourceDirectory: File,
        folderName: String
    ): String? {
        try {
            // Target path relative to Documents
            val relativePath = "$APP_FOLDER_NAME/$folderName"
            
            // For older Android versions (pre-Q/10), we use legacy file access
            // For Android Q+, we use MediaStore
            
            var savedCount = 0
            val sourceFiles = sourceDirectory.listFiles()?.filter { it.isFile } ?: return null
            
            if (sourceFiles.isEmpty()) return null
            
            Log.d(TAG, "Saving ${sourceFiles.size} files to $relativePath")
            
            val resolver = context.contentResolver
            
            for (file in sourceFiles) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/$relativePath")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    } else {
                        // Older versions might tricky with MediaStore for non-media files, but for JPEGs it should be fine.
                        // However, robust File API writing is better for older APIs if permissions allow.
                        // Ideally, we'd use File API for <Q, assuming WRITE_EXTERNAL_STORAGE is granted.
                    }
                }
                
                // Saving via MediaStore (standard way for modern Android public access)
                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                
                if (uri != null) {
                    try {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            FileInputStream(file).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        
                        // Mark as not pending
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            resolver.update(uri, contentValues, null, null)
                        }
                        savedCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save file ${file.name}", e)
                        // Try cleanup if failed
                        resolver.delete(uri, null, null)
                    }
                }
            }
            
            Log.d(TAG, "Access saved $savedCount/${sourceFiles.size} frames")
            
            // Return user-friendly path string
            return "${Environment.getExternalStorageDirectory().absolutePath}/${Environment.DIRECTORY_DOCUMENTS}/$relativePath"
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to public storage", e)
            return null
        }
    }
}
