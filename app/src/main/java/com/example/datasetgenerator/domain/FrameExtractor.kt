package com.example.datasetgenerator.domain

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts video frames using Android's native MediaMetadataRetriever.
 * Designed for generating CNN training datasets from object videos.
 * 
 * This implementation uses the built-in Android API instead of FFmpeg,
 * making it more reliable and avoiding external dependencies.
 */
object FrameExtractor {
    
    private const val TAG = "FrameExtractor"
    
    /**
     * Default frame extraction rate (frames per second)
     * Lower values = fewer frames but smaller dataset
     * Higher values = more frames but larger dataset
     */
    const val DEFAULT_FPS = 10
    
    /**
     * JPEG quality for saved frames (0-100)
     */
    private const val JPEG_QUALITY = 90
    
    /**
     * Data class containing extraction results and metadata
     */
    data class ExtractionResult(
        val frames: List<File>,
        val outputDirectory: File,
        val videoWidth: Int,
        val videoHeight: Int,
        val videoDurationMs: Long,
        val fps: Int
    )
    
    /**
     * Extracts frames from a video file at the specified frame rate.
     *
     * @param context Android context
     * @param videoPath Absolute path to the video file
     * @param objectName Object name (used for folder naming)
     * @param timestamp Recording timestamp
     * @param fps Frames per second to extract (default: 1)
     * @param onProgress Optional callback for progress updates (0.0 to 1.0)
     * @return ExtractionResult containing frames and metadata, or null on failure
     */
    suspend fun extractFrames(
        context: Context,
        videoPath: String,
        objectName: String,
        timestamp: Long,
        fps: Int = DEFAULT_FPS,
        onProgress: ((Float) -> Unit)? = null
    ): ExtractionResult? {
        return withContext(Dispatchers.IO) {
            var retriever: MediaMetadataRetriever? = null
            try {
                onProgress?.invoke(0.05f)
                
                // Create output directory with object_timestamp naming
                val baseDir = File(context.getExternalFilesDir("DatasetGenerator"), "TempFrames")
                val folderName = "${objectName}_$timestamp"
                val outputDir = File(baseDir, folderName)
                
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                
                // Initialize MediaMetadataRetriever
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(videoPath)
                
                // Get video metadata
                val durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
                
                val width = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )?.toIntOrNull() ?: 0
                
                val height = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )?.toIntOrNull() ?: 0
                
                Log.d(TAG, "Video info: ${width}x${height}, duration: ${durationMs}ms")
                onProgress?.invoke(0.1f)
                
                if (durationMs <= 0) {
                    Log.e(TAG, "Invalid video duration: $durationMs")
                    return@withContext null
                }
                
                // Calculate frame extraction intervals
                val frameIntervalMs = 1000L / fps.coerceIn(1, 30)
                val totalFrames = (durationMs / frameIntervalMs).toInt().coerceAtLeast(1)
                
                Log.d(TAG, "Extracting $totalFrames frames at ${fps} FPS (interval: ${frameIntervalMs}ms)")
                
                val extractedFrames = mutableListOf<File>()
                var currentTimeMs = 0L
                var frameIndex = 1
                
                while (currentTimeMs < durationMs) {
                    // Extract frame at current position (convert to microseconds)
                    val bitmap = retriever.getFrameAtTime(
                        currentTimeMs * 1000, // Convert ms to µs
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    
                    if (bitmap != null) {
                        // Save frame as JPEG
                        val frameFile = File(outputDir, "frame_%04d.jpg".format(frameIndex))
                        val saved = saveBitmapToFile(bitmap, frameFile)
                        bitmap.recycle()
                        
                        if (saved) {
                            extractedFrames.add(frameFile)
                            frameIndex++
                        }
                    }
                    
                    // Update progress
                    val progress = 0.1f + (0.85f * (currentTimeMs.toFloat() / durationMs))
                    onProgress?.invoke(progress)
                    
                    currentTimeMs += frameIntervalMs
                }
                
                Log.d(TAG, "Extracted ${extractedFrames.size} frames to ${outputDir.absolutePath}")
                onProgress?.invoke(1.0f)
                
                if (extractedFrames.isEmpty()) {
                    Log.e(TAG, "No frames were extracted")
                    return@withContext null
                }
                
                ExtractionResult(
                    frames = extractedFrames,
                    outputDirectory = outputDir,
                    videoWidth = width,
                    videoHeight = height,
                    videoDurationMs = durationMs,
                    fps = fps
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception during frame extraction", e)
                null
            } finally {
                try {
                    retriever?.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing MediaMetadataRetriever", e)
                }
            }
        }
    }
    
    /**
     * Saves a Bitmap to a JPEG file
     */
    private fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save frame: ${file.name}", e)
            false
        }
    }
    
    /**
     * Cleans up temporary frames directory after successful upload
     */
    suspend fun cleanupFrames(frameDirectory: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (frameDirectory.exists() && frameDirectory.isDirectory) {
                    val deleted = frameDirectory.deleteRecursively()
                    Log.d(TAG, "Cleanup ${if (deleted) "successful" else "failed"}: ${frameDirectory.absolutePath}")
                    deleted
                } else {
                    Log.w(TAG, "Directory does not exist: ${frameDirectory.absolutePath}")
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during cleanup", e)
                false
            }
        }
    }
}
