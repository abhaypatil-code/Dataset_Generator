package com.example.datasetgenerator.data

import android.content.Context
import android.util.Log
import com.example.datasetgenerator.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

class DriveRepository(private val context: Context) {

    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        .requestIdToken(context.getString(R.string.default_web_client_id)) // Using string resource
        .build()

    val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, signInOptions)

    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun getDriveService(account: GoogleSignInAccount): Drive = withContext(Dispatchers.IO) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Dataset Generator").build()
    }

    suspend fun createFolder(driveService: Drive, folderName: String): String? = withContext(Dispatchers.IO) {
        try {
            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = folderName
            fileMetadata.mimeType = "application/vnd.google-apps.folder"

            val file = driveService.files().create(fileMetadata)
                .setFields("id")
                .execute()
            file.id
        } catch (e: Exception) {
            Log.e("DriveRepository", "Error creating folder", e)
            null
        }
    }

    suspend fun uploadFile(driveService: Drive, localFile: File, parentFolderId: String?): String? = withContext(Dispatchers.IO) {
        try {
            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = localFile.name
            parentFolderId?.let { fileMetadata.parents = listOf(it) }

            val mediaContent = FileContent("image/jpeg", localFile)

            val file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            file.id
        } catch (e: Exception) {
            Log.e("DriveRepository", "Error uploading file", e)
            null
        }
    }
}
