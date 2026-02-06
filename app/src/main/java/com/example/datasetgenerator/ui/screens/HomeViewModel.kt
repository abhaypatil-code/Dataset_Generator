package com.example.datasetgenerator.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.datasetgenerator.data.AppDatabase
import com.example.datasetgenerator.data.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    // We need to instantiate the repository manually here since we don't have Hilt/Koin
    // In a real app we'd inject this
    private val repository: VideoRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = VideoRepository(application, database.recordingDao())
    }
    
    private val _videoList = MutableStateFlow<List<File>>(emptyList())
    val videoList: StateFlow<List<File>> = _videoList.asStateFlow()
    
    fun loadVideos() {
        viewModelScope.launch {
            _videoList.value = repository.getAllVideos()
        }
    }
    
    fun shareVideo(context: Context, videoFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            videoFile
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Video"))
    }
}
