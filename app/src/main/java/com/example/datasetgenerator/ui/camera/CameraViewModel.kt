package com.example.datasetgenerator.ui.camera

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.camera.core.Preview
import androidx.camera.video.VideoRecordEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.datasetgenerator.data.VideoRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CapturePhase(
    val instruction: String,
    val shortLabel: String,
    val icon: String,
    val durationSeconds: Int = 5
) {
    LEFT_SIDE("Record the LEFT SIDE\n(Back-end side view)", "LEFT", "←", 6),
    MOVE_TO_FRONT("MOVE smoothly to FRONT", "FRONT", "↑", 4),
    RIGHT_SIDE("Record the RIGHT SIDE", "RIGHT", "→", 6);

    companion object {
        val phases = values().toList()
    }
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val videoRecorder = VideoRecorder(application)
    
    // Recording State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentPhaseIndex = MutableStateFlow(0)
    val currentPhaseIndex: StateFlow<Int> = _currentPhaseIndex.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()
    
    private val _totalRecordingSeconds = MutableStateFlow(0)
    val totalRecordingSeconds: StateFlow<Int> = _totalRecordingSeconds.asStateFlow()

    private val _recordedVideoUri = MutableStateFlow<Uri?>(null)
    val recordedVideoUri: StateFlow<Uri?> = _recordedVideoUri.asStateFlow()
    
    private val _shouldVibrate = MutableStateFlow(false)
    val shouldVibrate: StateFlow<Boolean> = _shouldVibrate.asStateFlow()

    private var timerJob: Job? = null

    fun bindCamera(lifecycleOwner: LifecycleOwner, preview: Preview) {
        viewModelScope.launch {
            videoRecorder.bindToLifecycle(lifecycleOwner, preview)
        }
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        // Create file
        val context = getApplication<Application>()
        val baseDir = context.getExternalFilesDir("DatasetGenerator")
        val videoDir = File(baseDir, "Videos").apply { mkdirs() }
        val filename = "VIDEO_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        val file = File(videoDir, filename)

        videoRecorder.startRecording(file) { event ->
            if (event is VideoRecordEvent.Start) {
                _isRecording.value = true
                resetGuidance()
                startTimer()
            } else if (event is VideoRecordEvent.Finalize) {
                _isRecording.value = false
                stopTimer()
                if (!event.hasError()) {
                    _recordedVideoUri.value = event.outputResults.outputUri
                } else {
                    Log.e("CameraViewModel", "Recording error: ${event.error}")
                }
            }
        }
    }

    private fun stopRecording() {
        videoRecorder.stopRecording()
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                _elapsedSeconds.value += 1
                _totalRecordingSeconds.value += 1
                
                checkPhaseProgression()
            }
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
    }
    
    private fun checkPhaseProgression() {
        val phases = CapturePhase.phases
        val currentIndex = _currentPhaseIndex.value
        val currentPhase = phases.getOrNull(currentIndex)
        
        if (currentPhase != null && _elapsedSeconds.value >= currentPhase.durationSeconds) {
            if (currentIndex < phases.size - 1) {
                _currentPhaseIndex.value += 1
                _elapsedSeconds.value = 0
                triggerVibration()
            }
        }
    }
    
    private fun triggerVibration() {
        viewModelScope.launch {
            _shouldVibrate.value = true
            delay(100)
            _shouldVibrate.value = false
        }
    }

    private fun resetGuidance() {
        _currentPhaseIndex.value = 0
        _elapsedSeconds.value = 0
        _totalRecordingSeconds.value = 0
    }

    fun onVibrationHandled() {
        _shouldVibrate.value = false
    }
    
    fun resetRecordingState() {
        _recordedVideoUri.value = null
        resetGuidance()
    }
}
