# Video Dataset Collection App

An Android application for collecting high-quality labeled video and image data for training computer vision models (e.g., CNNs) for object identification or classification.

## Features

- **Guided Video Recording** - Visual prompts guide users through the optimal capture sequence (e.g., Left → Front → Right)
- **Automatic Frame Extraction** - Videos are automatically converted to labeled image sequences using Android's MediaMetadataRetriever
- **Google Drive Upload** - Extracted frames are uploaded to the user's Google Drive for easy access
- **Metadata Generation** - JSON metadata files accompany each recording with label, timestamp, resolution, and frame count
- **Offline Support** - Recordings are queued and uploaded when network is available
- **Field-Friendly UI** - Large buttons and clear instructions designed for non-technical users

## Requirements

- Android 8.0 (API 26) or higher
- Camera with video recording capability
- Internet connection for Google Drive upload

## Setup Instructions

### 1. Clone and Open Project

```bash
git clone <repository-url>
cd "Data Collection Tool"
```

Open the project in Android Studio Hedgehog or later.

### 2. Configure Google Cloud (Required for Drive Upload)

See [GOOGLE_DRIVE_SETUP.md](GOOGLE_DRIVE_SETUP.md) for detailed instructions.

**Quick Summary:**
1. Create a project in [Google Cloud Console](https://console.cloud.google.com/)
2. Enable the Google Drive API
3. Configure OAuth consent screen
4. Create OAuth 2.0 credentials (Android app)
5. Get your Web Client ID
6. Update the configuration (e.g., `HomeScreen.kt`) with your Web Client ID:
   ```kotlin
   .requestIdToken("YOUR_WEB_CLIENT_ID") // Replace this
   ```

### 3. Build the APK

```bash
# Debug build
./gradlew assembleDebug

# APK location: app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. **Launch the app** and grant camera/audio permissions
2. **Sign in to Google Drive** (optional but recommended)
3. **Tap START RECORDING**
4. **Follow the on-screen prompts** to capture the object from all angles
5. **Review** the recording and tap APPROVE or RETAKE
6. **Enter the object name** (e.g., "Chair", "Monitor", "ID_123")
7. **Frames are extracted and uploaded automatically**

## Output Data Structure

### Google Drive
Uploads to the shared data collection folder.

```
[Shared Folder]/
├── Object_Name_1706986200000/
│   ├── frame_0001.jpg
│   ├── frame_0002.jpg
│   ├── ...
│   └── metadata.json
├── Another_Object_1706986500000/
│   └── ...
```

### Metadata JSON Format

```json
{
  "object_name": "Chair", 
  "timestamp": 1706986200000,
  "frame_count": 25,
  "video_resolution": "1080p",
  "video_width": 1920,
  "video_height": 1080,
  "video_duration_ms": 25000,
  "extraction_fps": 1,
  "capture_date": "2024-02-03T22:30:00.000+0530",
  "folder_name": "Chair_1706986200000"
}
```
*(Note: "object_name" field maps to the entered object name)*

## Configuration

### Frame Rate Adjustment

Default frame extraction rate is **1 FPS**. To change:

1. Open `AppSettings.kt`
2. Modify `DEFAULT_FRAME_RATE` (1-5 FPS recommended)
3. Higher FPS = more frames but larger dataset

### Video Quality

The app records in HD (1280x720) by default. To change:

1. Open `RecordingScreen.kt`
2. Modify `QualitySelector` to:
   - `Quality.SD` - Lower quality, smaller files
   - `Quality.FHD` - Full HD (1080p)
   - `Quality.UHD` - 4K (if supported by device)

## Dataset Scalability Considerations

- **Storage**: Each 20-second recording produces ~20 frames at 1 FPS (~2-4 MB total)
- **Throughput**: Scalable for high-volume data collection
- **Labeling**: Folder names serve as labels for ML training
- **Consistency**: All frames have consistent aspect ratio and resolution

## Architecture

```
com.example.datasetgenerator/
├── data/
│   ├── AppSettings.kt       # SharedPreferences wrapper
│   ├── GoogleDriveHelper.kt # Drive API operations
│   └── UploadWorker.kt      # Background upload worker
├── domain/
│   ├── FrameExtractor.kt    # Android Native extraction
│   └── MetadataGenerator.kt # JSON metadata creation
└── ui/
    ├── screens/
    │   ├── HomeScreen.kt    # Entry + sign-in
    │   ├── RecordingScreen.kt # Camera capture
    │   ├── PreviewScreen.kt   # Video review
    │   └── UploadScreen.kt    # Processing + upload
```

## Tech Stack

- **Kotlin** - Primary language
- **Jetpack Compose** - UI framework
- **CameraX** - Video recording
- **MediaMetadataRetriever** - Frame extraction
- **WorkManager** - Background uploads
- **Google Drive API v3** - Cloud storage

## License

[Add your license here]
