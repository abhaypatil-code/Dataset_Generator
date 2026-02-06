# Google Drive API Setup Guide

This guide walks you through setting up Google Cloud credentials for the Dataset Generator app.

## Step 1: Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click the project dropdown at the top
3. Click **New Project**
4. Enter project name: `Dataset Generator`
5. Click **Create**

## Step 2: Enable Google Drive API

1. In your new project, go to **APIs & Services** → **Library**
2. Search for "Google Drive API"
3. Click on it and press **Enable**

## Step 3: Configure OAuth Consent Screen

1. Go to **APIs & Services** → **OAuth consent screen**
2. Select **External** user type (or Internal if using Google Workspace)
3. Click **Create**
4. Fill in the required fields:
   - **App name**: Dataset Generator
   - **User support email**: Your email
   - **Developer contact information**: Your email
5. Click **Save and Continue**
6. On the **Scopes** page:
   - Click **Add or Remove Scopes**
   - Search for and select: `https://www.googleapis.com/auth/drive.file`
   - Click **Update**
7. Click **Save and Continue**
8. Add test users (your email for testing)
9. Click **Save and Continue**

## Step 4: Create OAuth 2.0 Credentials

### Android Client (for app signing)

1. Go to **APIs & Services** → **Credentials**
2. Click **Create Credentials** → **OAuth client ID**
   > [!NOTE]
   > If you are taken to a "Credential Type" wizard asking "What data will you be accessing?", select **User data**. This is because the app accesses the *user's* personal Google Drive.
3. Select **Android** as application type
4. Enter:
   - **Name**: Dataset Generator Android
   - **Package name**: `com.example.datasetgenerator`
   - **SHA-1 certificate fingerprint**: See below
5. Click **Create**

### Get SHA-1 Fingerprint

For debug builds:
```bash
# Windows
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

# Mac/Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the SHA1 value and paste it in the Cloud Console.

### Web Client (required for ID token)

1. Click **Create Credentials** → **OAuth client ID**
2. Select **Web application** as application type
3. Enter **Name**: Dataset Generator Web Client
4. Leave redirect URIs empty for now
5. Click **Create**
6. **Copy the Client ID** - you'll need this for the app

## Step 5: Configure the App

1. Open `HomeScreen.kt`
2. Find this line:
   ```kotlin
   .requestIdToken("YOUR_WEB_CLIENT_ID")
   ```
3. Replace `YOUR_WEB_CLIENT_ID` with your Web Client ID

## Step 6: (Optional) Publish OAuth Consent

For production use:
1. Go to **OAuth consent screen**
2. Click **Publish App**
3. Complete Google's verification process

For testing with limited users, you can skip this step.

## Troubleshooting

### "Sign in failed" error
- Verify SHA-1 fingerprint matches your debug/release key
- Check package name matches exactly: `com.example.datasetgenerator`
- Ensure you're using the Web Client ID, not the Android Client ID

### "Access denied" error
- Verify the Drive API is enabled
- Check that `drive.file` scope is added to consent screen
- For external users, ensure your email is added as a test user

### "Access blocked: App has not completed the Google verification process"
This error occurs because the app is in "Testing" mode in the Google Cloud Console, and the email you are trying to use is not added as a "Test User".

**To fix this:**
1. Go to [Google Cloud Console > OAuth consent screen](https://console.cloud.google.com/apis/credentials/consent)
2. Look for the **Test users** section
3. Click **+ ADD USERS**
4. Enter the email address of the account you are trying to sign in with
5. Click **Save**
6. Try signing in again on the app (no need to reinstall)

### Token refresh issues
- The app currently uses ID tokens which expire
- For production, implement proper OAuth token refresh flow

## Security Notes

- Never commit Client IDs to public repositories
- For production, use environment variables or build configs
- Consider using Firebase Auth for more robust authentication

## Additional Resources

- [Google Drive API Documentation](https://developers.google.com/drive/api/v3/about-sdk)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android)
- [OAuth 2.0 for Mobile Apps](https://developers.google.com/identity/protocols/oauth2/native-app)
