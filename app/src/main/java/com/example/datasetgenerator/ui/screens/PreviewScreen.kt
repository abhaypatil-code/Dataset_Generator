package com.example.datasetgenerator.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Preview screen for reviewing recorded video before approval.
 * Provides options to approve (and enter object name) or retake the recording.
 */
@Composable
fun PreviewScreen(
    videoUri: String?,
    onRetake: () -> Unit,
    onApprove: (String) -> Unit,
    viewOnly: Boolean = false
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var objectName by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = if (viewOnly) "📹 VIEW RECORDING" else "📹 REVIEW RECORDING",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        // Video Preview
        if (videoUri != null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(videoUri))
                            val mediaController = MediaController(ctx)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Error state
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Video not available",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Instructions
        if (!viewOnly) {
            Text(
                text = "Review the video above.\nIs the capture sequence complete?",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Action Buttons
        if (!viewOnly) {
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Retake Button
            Button(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "🔄 RETAKE",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Approve Button
            Button(
                onClick = { showNameDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                enabled = videoUri != null
            ) {
                Text(
                    text = "✅ APPROVE",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
        }
        }
        } else {
            // View Only - Close Button
            Button(
                onClick = onRetake, // Acts as Close/Back
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("CLOSE", fontSize = 16.sp, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Object Name Input Dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { 
                showNameDialog = false
                nameError = null
            },
            containerColor = Color(0xFF2D2D2D),
            title = {
                Column {
                    Text(
                        text = "🏷️ ENTER OBJECT NAME",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This will be used as the folder name for your dataset",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = objectName,
                        onValueChange = { 
                            objectName = it
                            nameError = null
                        },
                        label = { Text("Object Name", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("e.g., Chair, Laptop, Bottle", color = Color.Gray) },
                        isError = nameError != null,
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color(0xFF4CAF50),
                            unfocusedIndicatorColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (nameError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = nameError!!,
                            color = Color(0xFFE53935),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = objectName.trim()
                        when {
                            trimmedName.isEmpty() -> {
                                nameError = "Object name cannot be empty"
                            }
                            trimmedName.length < 2 -> {
                                nameError = "Name too short"
                            }
                            !trimmedName.matches(Regex("^[a-zA-Z0-9_\\- ]+$")) -> {
                                nameError = "Use only letters, numbers, spaces, - or _"
                            }
                            else -> {
                                onApprove(trimmedName)
                                showNameDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = "SAVE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = { 
                        showNameDialog = false
                        nameError = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = "CANCEL",
                        fontSize = 16.sp
                    )
                }
            }
        )
    }
}
