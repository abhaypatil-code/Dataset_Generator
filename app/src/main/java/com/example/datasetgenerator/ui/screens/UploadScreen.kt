package com.example.datasetgenerator.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.datasetgenerator.data.AppSettings
import com.example.datasetgenerator.data.UploadWorker
import com.example.datasetgenerator.domain.FrameExtractor
import com.example.datasetgenerator.domain.MetadataGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withContext

/**
 * Upload screen showing frame extraction and upload progress.
 * Handles the complete post-recording workflow.
 */
sealed class UploadState {
    object Initializing : UploadState()
    data class Extracting(val progress: Float) : UploadState()
    data class ExtractComplete(val frameCount: Int) : UploadState()
    object Queuing : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

@Composable
fun UploadScreen(
    videoUri: String,
    objectName: String,
    onUploadComplete: () -> Unit
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings(context) }
    
    // Add state for storage choice
    var uploadState by remember { mutableStateOf<UploadState>(UploadState.Initializing) }
    var frameCount by remember { mutableIntStateOf(0) }
    var extractionProgress by remember { mutableFloatStateOf(0f) }
    var savedLocalPath by remember { mutableStateOf<String?>(null) }
    
    // Final result data needed for saving/uploading
    var extractionResult by remember { mutableStateOf<FrameExtractor.ExtractionResult?>(null) }
    
    val animatedProgress by animateFloatAsState(
        targetValue = extractionProgress,
        animationSpec = tween(300),
        label = "progress"
    )

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    // Helper to perform the local save
    fun saveLocally() {
        val result = extractionResult ?: return
        val timestamp = System.currentTimeMillis() // Or reuse the one from extraction if we had it
        val folderName = "${objectName}_${System.currentTimeMillis()}"
        
        uploadState = UploadState.Queuing // Re-use Queuing state for "Saving..."
        
        // Run in scope
        scope.launch(Dispatchers.IO) {
            val savedPath = com.example.datasetgenerator.data.LocalStorageHelper.saveFramesToPublicStorage(
                context, 
                result.outputDirectory, 
                folderName
            )
            
            withContext(Dispatchers.Main) {
                if (savedPath != null) {
                    savedLocalPath = savedPath
                    uploadState = UploadState.Success // Success state
                } else {
                    uploadState = UploadState.Error("Failed to save to Documents folder")
                }
            }
        }
    }

    // Helper to perform the upload (Drive)
    fun uploadToDrive() {
         val result = extractionResult ?: return
         val timestamp = System.currentTimeMillis()
         
         uploadState = UploadState.Queuing
         
         val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(
                UploadWorker.KEY_FOLDER_PATH to result.outputDirectory.absolutePath,
                UploadWorker.KEY_FOLDER_NAME to "${objectName}_$timestamp",
                UploadWorker.KEY_OBJECT_NAME to objectName
            ))
            .build()
        
        WorkManager.getInstance(context).enqueue(uploadWork)
        uploadState = UploadState.Success
    }
    
    LaunchedEffect(Unit) {
        uploadState = UploadState.Extracting(0f)
        
        // Remove file:// prefix for File operations
        val path = videoUri.removePrefix("file://")
        val timestamp = System.currentTimeMillis()
        val fps = appSettings.frameRate
        
        // Extract frames with progress callback
        val result = FrameExtractor.extractFrames(
            context = context,
            videoPath = path,
            objectName = objectName,
            timestamp = timestamp,
            fps = fps,
            onProgress = { progress ->
                extractionProgress = progress
                uploadState = UploadState.Extracting(progress)
            }
        )
        
        if (result != null && result.frames.isNotEmpty()) {
            frameCount = result.frames.size
            extractionResult = result // Save result for later use
            
            // Generate metadata file
            val metadataFile = withContext(Dispatchers.IO) {
                MetadataGenerator.generateMetadataFile(
                    outputDirectory = result.outputDirectory,
                    objectName = objectName,
                    timestamp = timestamp,
                    frameCount = result.frames.size,
                    videoWidth = result.videoWidth,
                    videoHeight = result.videoHeight,
                    videoDurationMs = result.videoDurationMs,
                    extractionFps = fps
                )
            }

            if (metadataFile == null) {
                uploadState = UploadState.Error("Failed to generate metadata")
                return@LaunchedEffect
            }
            
            // Extraction Done. 
            // If Guest Mode -> Auto-save locally? Or show prompt? Request says "Prompt user"
            // We'll set state to ExtractComplete to show choice UI.
            uploadState = UploadState.ExtractComplete(result.frames.size)
            
        } else {
            uploadState = UploadState.Error("Failed to extract frames from video")
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            when (val state = uploadState) {
                is UploadState.Initializing -> {
                    StatusIndicator(
                        emoji = "⏳",
                        title = "Initializing...",
                        subtitle = "Preparing video for processing"
                    )
                }
                
                is UploadState.Extracting -> {
                    StatusIndicator(
                        emoji = "🎬",
                        title = "Extracting Frames",
                        subtitle = "Converting video to images"
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    LinearProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }
                
                is UploadState.ExtractComplete -> {
                    StatusIndicator(
                        emoji = "✅",
                        title = "Extraction Complete",
                        subtitle = "${state.frameCount} frames ready"
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Where would you like to save?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Local Storage Option
                    Button(
                        onClick = { saveLocally() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3) // Blue for local
                        )
                    ) {
                        Text("💾  Save to Device Storage", fontSize = 16.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Drive Option (Only if signed in)
                    if (appSettings.isSignedIn()) {
                        Button(
                            onClick = { uploadToDrive() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50) // Green for Drive
                            )
                        ) {
                            Text("☁️  Upload to Google Drive", fontSize = 16.sp)
                        }
                    } else {
                        // Guest info
                        Text(
                            text = "Sign in to enable Cloud Upload",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                
                is UploadState.Queuing -> {
                    StatusIndicator(
                        emoji = "💾",
                        title = "Saving...",
                        subtitle = "Writing files to storage" // Generic message for both
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                is UploadState.Success -> {
                    StatusIndicator(
                        emoji = "🎉",
                        title = "Success!",
                         // Differentiate message if Saved Locally vs Uploaded
                        subtitle = if (savedLocalPath != null) "Saved to Device" else "Upload Queued"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Summary card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(16.dp)
                    ) {
                        Column {
                            SummaryRow("Object", objectName)
                            SummaryRow("Frames", frameCount.toString())
                            if (savedLocalPath != null) {
                                Text(
                                    text = "Location:", 
                                    color = Color.White.copy(alpha = 0.7f), 
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Text(
                                    text = savedLocalPath ?: "",
                                    color = Color(0xFF64B5F6),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                SummaryRow("Status", "Upload queued (background)")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = onUploadComplete,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("RECORD ANOTHER", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                is UploadState.Error -> {
                    StatusIndicator(
                        emoji = "❌",
                        title = "Error",
                        subtitle = state.message
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onUploadComplete,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("GO BACK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    emoji: String,
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 40.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Text(
        text = "$label: $value",
        color = Color.White.copy(alpha = 0.9f),
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
