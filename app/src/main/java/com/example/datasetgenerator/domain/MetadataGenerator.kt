package com.example.datasetgenerator.domain

import android.util.Log
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

/**
 * Generates metadata JSON files for extracted video frames.
 * This metadata is essential for CNN training dataset organization.
 */
object MetadataGenerator {
    
    private const val TAG = "MetadataGenerator"
    private const val METADATA_FILENAME = "metadata.json"
    
    /**
     * Metadata structure for a recording session
     */
    data class RecordingMetadata(
        val objectName: String,
        val timestamp: Long,
        val frameCount: Int,
        val videoResolution: String,
        val videoDurationMs: Long,
        val extractionFps: Int,
        val captureDate: String
    )
    
    /**
     * Generates a metadata.json file in the specified directory
     *
     * @param outputDirectory Directory where frames are stored
     * @param objectName Object name
     * @param timestamp Recording timestamp
     * @param frameCount Number of extracted frames
     * @param videoWidth Video width in pixels
     * @param videoHeight Video height in pixels
     * @param videoDurationMs Video duration in milliseconds
     * @param extractionFps FPS used for frame extraction
     * @return The created metadata file, or null on failure
     */
    fun generateMetadataFile(
        outputDirectory: File,
        objectName: String,
        timestamp: Long,
        frameCount: Int,
        videoWidth: Int,
        videoHeight: Int,
        videoDurationMs: Long,
        extractionFps: Int
    ): File? {
        return try {
            val resolution = getResolutionLabel(videoWidth, videoHeight)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
            val captureDate = dateFormat.format(Date(timestamp))
            
            val metadata = JSONObject().apply {
                put("object_name", objectName)
                put("timestamp", timestamp)
                put("frame_count", frameCount)
                put("video_resolution", resolution)
                put("video_width", videoWidth)
                put("video_height", videoHeight)
                put("video_duration_ms", videoDurationMs)
                put("extraction_fps", extractionFps)
                put("capture_date", captureDate)
                put("folder_name", outputDirectory.name)
            }
            
            val metadataFile = File(outputDirectory, METADATA_FILENAME)
            metadataFile.writeText(metadata.toString(2))
            
            Log.d(TAG, "Generated metadata file: ${metadataFile.absolutePath}")
            metadataFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate metadata file", e)
            null
        }
    }
    
    /**
     * Converts width/height to a human-readable resolution label
     */
    private fun getResolutionLabel(width: Int, height: Int): String {
        val maxDimension = maxOf(width, height)
        return when {
            maxDimension >= 3840 -> "4K"
            maxDimension >= 1920 -> "1080p"
            maxDimension >= 1280 -> "720p"
            maxDimension >= 854 -> "480p"
            else -> "${width}x${height}"
        }
    }
    
    /**
     * Reads and parses an existing metadata file
     */
    fun readMetadata(metadataFile: File): RecordingMetadata? {
        return try {
            if (!metadataFile.exists()) {
                Log.w(TAG, "Metadata file does not exist: ${metadataFile.absolutePath}")
                return null
            }
            
            val json = JSONObject(metadataFile.readText())
            RecordingMetadata(
                objectName = json.getString("object_name"),
                timestamp = json.getLong("timestamp"),
                frameCount = json.getInt("frame_count"),
                videoResolution = json.getString("video_resolution"),
                videoDurationMs = json.getLong("video_duration_ms"),
                extractionFps = json.getInt("extraction_fps"),
                captureDate = json.getString("capture_date")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read metadata file", e)
            null
        }
    }
}
