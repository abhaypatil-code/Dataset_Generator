package com.example.datasetgenerator.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.datasetgenerator.data.AppSettings
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

/**
 * Home screen with Google Sign-In and recording start functionality.
 * Field-friendly UI with large touch targets.
 */
@Composable
fun HomeScreen(
    onStartRecording: () -> Unit,
    onPlayVideo: (String) -> Unit,
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel<HomeViewModel>()
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings(context) }
    
    var isSignedIn by remember { mutableStateOf(appSettings.isSignedIn()) }
    var signedInEmail by remember { mutableStateOf(appSettings.signedInEmail) }
    
    // Video list state
    val videoList by viewModel.videoList.collectAsState()
    
    // Load videos when screen appears
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadVideos()
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            onStartRecording()
        } else {
            Toast.makeText(context, "Camera and audio permissions required", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Always try to get the task result to handle potential errors
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            // getResult(ApiException::class.java) enables better error handling
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            
            // Success
            appSettings.signedInEmail = account.email
            isSignedIn = true
            signedInEmail = account.email
            Toast.makeText(context, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
        } catch (e: com.google.android.gms.common.api.ApiException) {
            // Common status codes:
            // 12500: Sign-in failed (often wrong SHA-1)
            // 12501: User cancelled
            // 10: Developer error (misconfiguration)
            if (e.statusCode == 12501) {
                // User cancelled, no need to show error
            } else {
                Toast.makeText(context, "Sign-in failed. Error Code: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Sign-in error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    val onSignInClick = {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        signInLauncher.launch(googleSignInClient.signInIntent)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),
                        Color(0xFF0D1421)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LazyColumn to allow scrolling
            androidx.compose.foundation.lazy.LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    // Top Section - Branding and Auth
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📸", fontSize = 40.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Dataset Generator",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "ML Training Dataset Generator",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))


                        // Auth Status / Sign In Button
                        if (isSignedIn) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "✅ Signed in as ${signedInEmail?.substringBefore("@")}",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Button(
                                    onClick = onSignInClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "G ", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Sign in with Google",
                                            color = Color.Gray,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                TextButton(
                                    onClick = { 
                                        appSettings.isGuestMode = true
                                        // Trigger recomposition or simply handle downstream
                                        isSignedIn = false // Effectively still false for "Drive", but we can use appSettings.isGuestMode in logic
                                        Toast.makeText(context, "Guest Mode Enabled", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(
                                        text = "Skip Sign In (Save Locally)",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    // Middle Section - Instructions
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "📋 HOW TO USE",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            InstructionItem("1", "Point camera at object's LEFT side")
                            InstructionItem("2", "Slowly walk around to FRONT")
                            InstructionItem("3", "End at RIGHT side")
                        }
                    }
                }
                
                // Video Gallery Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View recorded videos",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                if (videoList.isEmpty()) {
                    item {
                        Text(
                            text = "No videos recorded yet",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        )
                    }
                } else {
                    items(videoList.size) { index ->
                        val videoFile = videoList[index]
                        VideoListItem(
                            videoFile = videoFile,
                            onPlay = { onPlayVideo(Uri.fromFile(videoFile).toString()) },
                            onShare = { viewModel.shareVideo(context, videoFile) }
                        )
                    }
                }
            }
            
            // Fixed Bottom Section - Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Google Drive Status (Collapsed/ simplified if scrolling needed, but kept same for now)
                
                // Main Start Button
                Button(
                    onClick = {
                        val isGuest = appSettings.isGuestMode
                        if (!isSignedIn && !isGuest) {
                            Toast.makeText(context, "Please sign in or skip to continue", Toast.LENGTH_SHORT).show()
                            // Highlight auth options?
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.CAMERA,
                                    android.Manifest.permission.RECORD_AUDIO
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = "📹  START RECORDING",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun VideoListItem(
    videoFile: java.io.File,
    onPlay: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "▶️", fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = videoFile.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${videoFile.length() / 1024 / 1024} MB",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            androidx.compose.material3.IconButton(onClick = onShare) {
                Text(text = "📤", fontSize = 20.sp)
            }
            
            androidx.compose.material3.IconButton(onClick = onPlay) {
                Text(text = "👁️", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun InstructionItem(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 15.sp
        )
    }
}
