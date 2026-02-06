package com.example.datasetgenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.datasetgenerator.ui.screens.HomeScreen
import com.example.datasetgenerator.ui.screens.PreviewScreen
import com.example.datasetgenerator.ui.screens.RecordingScreen
import com.example.datasetgenerator.ui.screens.UploadScreen
import com.example.datasetgenerator.ui.theme.DatasetGeneratorTheme
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DatasetGeneratorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {
                        
                        // 1. Home Screen
                        composable("home") {
                            HomeScreen(
                                onStartRecording = { 
                                    navController.navigate("recording") 
                                },
                                onPlayVideo = { uri ->
                                    val encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
                                    navController.navigate("preview/$encodedUri?viewOnly=true")
                                }
                            )
                        }
                        
                        // 2. Recording Screen
                        composable("recording") {
                            RecordingScreen(
                                onRecordingFinished = { videoUri ->
                                    val encodedUri = URLEncoder.encode(videoUri, StandardCharsets.UTF_8.toString())
                                    navController.navigate("preview/$encodedUri")
                                }
                            )
                        }
                        
                        // 3. Preview Screen
                        composable(
                            route = "preview/{videoUri}?viewOnly={viewOnly}",
                            arguments = listOf(
                                navArgument("videoUri") { type = NavType.StringType },
                                navArgument("viewOnly") { type = NavType.BoolType; defaultValue = false }
                            )
                        ) { backStackEntry ->
                            val videoUri = backStackEntry.arguments?.getString("videoUri")
                            val viewOnly = backStackEntry.arguments?.getBoolean("viewOnly") ?: false
                            PreviewScreen(
                                videoUri = videoUri,
                                viewOnly = viewOnly,
                                onRetake = {
                                    // Pop back
                                    navController.popBackStack() 
                                },
                                onApprove = { objectName ->
                                    val safeObjectName = URLEncoder.encode(objectName, StandardCharsets.UTF_8.toString())
                                    val safeUri = URLEncoder.encode(videoUri, StandardCharsets.UTF_8.toString())
                                    navController.navigate("upload/$safeUri/$safeObjectName")
                                }
                            )
                        }
                        
                        // 4. Upload Screen
                        composable(
                            route = "upload/{videoUri}/{objectName}",
                            arguments = listOf(
                                navArgument("videoUri") { type = NavType.StringType },
                                navArgument("objectName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val videoUri = backStackEntry.arguments?.getString("videoUri") ?: ""
                            val objectName = backStackEntry.arguments?.getString("objectName") ?: ""
                            
                            UploadScreen(
                                videoUri = videoUri,
                                objectName = objectName,
                                onUploadComplete = {
                                    // Reset to home, clearing back stack
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
