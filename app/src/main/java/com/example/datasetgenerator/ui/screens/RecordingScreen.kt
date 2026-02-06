package com.example.datasetgenerator.ui.screens

import android.content.Context
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.datasetgenerator.ui.camera.CameraViewModel
import com.example.datasetgenerator.ui.camera.CapturePhase

@Composable
fun RecordingScreen(
    onRecordingFinished: (String) -> Unit,
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // State observation
    val isRecording by viewModel.isRecording.collectAsState()
    val currentPhaseIndex by viewModel.currentPhaseIndex.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val totalRecordingSeconds by viewModel.totalRecordingSeconds.collectAsState()
    val recordedVideoUri by viewModel.recordedVideoUri.collectAsState()
    val shouldVibrate by viewModel.shouldVibrate.collectAsState()
    
    val currentPhase = if (isRecording && currentPhaseIndex < CapturePhase.phases.size) {
        CapturePhase.phases[currentPhaseIndex]
    } else null
    
    val previewView = remember { PreviewView(context) }

    // Camera Lifecycle
    LaunchedEffect(Unit) {
        val preview = androidx.camera.core.Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        viewModel.bindCamera(lifecycleOwner, preview)
    }

    // Effect for handling finished recording
    LaunchedEffect(recordedVideoUri) {
        recordedVideoUri?.let { uri ->
            onRecordingFinished(uri.toString())
            viewModel.resetRecordingState()
        }
    }
    
    // Effect for vibration
    LaunchedEffect(shouldVibrate) {
        if (shouldVibrate) {
            vibrateDevice(context)
            viewModel.onVibrationHandled()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Top Overlay - Instructions
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRecording && currentPhase != null) {
                // Phase Progress Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CapturePhase.phases.forEachIndexed { index, phase ->
                        PhaseIndicator(
                            label = phase.shortLabel,
                            isActive = index == currentPhaseIndex,
                            isCompleted = index < currentPhaseIndex
                        )
                        if (index < CapturePhase.phases.size - 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }
                
                // Main Instruction Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Direction arrow
                        Text(
                            text = currentPhase.icon,
                            fontSize = 48.sp,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Instruction text
                        Text(
                            text = currentPhase.instruction,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Timer for current phase
                        val remainingSeconds = currentPhase.durationSeconds - elapsedSeconds
                        Text(
                            text = "Phase: ${remainingSeconds}s remaining",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp
                        )
                    }
                }
            } else if (!isRecording) {
                // Pre-recording instructions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📹 CAPTURE SEQUENCE",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "1. Start from LEFT side\n2. Move to FRONT\n3. End at RIGHT side",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
        
        // Bottom Overlay - Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Recording duration display
            if (isRecording) {
                Text(
                    text = formatDuration(totalRecordingSeconds),
                    color = Color.Red,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Large Record/Stop Button
            Button(
                onClick = {
                    viewModel.toggleRecording()
                },
                modifier = Modifier
                    .size(if (isRecording) 100.dp else 120.dp)
                    .then(
                        if (isRecording) {
                            Modifier.border(4.dp, Color.Red, CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color.Red.copy(alpha = 0.9f) else Color(0xFF4CAF50)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                if (isRecording) {
                    // Stop icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White, RoundedCornerShape(4.dp))
                    )
                } else {
                    Text(
                        text = "START\nRECORD",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PhaseIndicator(
    label: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    val backgroundColor = when {
        isCompleted -> Color(0xFF4CAF50)
        isActive -> Color(0xFFFFC107)
        else -> Color.Gray.copy(alpha = 0.5f)
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

@Suppress("DEPRECATION")
private fun vibrateDevice(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    vibrator?.let {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            it.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            it.vibrate(200)
        }
    }
}
