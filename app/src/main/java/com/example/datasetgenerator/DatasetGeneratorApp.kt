package com.example.datasetgenerator

import android.app.Application
import com.example.datasetgenerator.data.AppDatabase
import com.example.datasetgenerator.data.VideoRepository

class DatasetGeneratorApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { VideoRepository(this, database.recordingDao()) }
}
