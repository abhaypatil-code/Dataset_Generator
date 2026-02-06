package com.example.datasetgenerator.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * App settings manager using SharedPreferences.
 * Handles frame extraction settings and OAuth token storage.
 */
class AppSettings(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "dataset_generator_prefs"
        private const val SECURE_PREFS_NAME = "dataset_generator_secure_prefs"
        
        // Settings keys
        const val KEY_FRAME_RATE = "frame_rate"
        const val KEY_VIDEO_QUALITY = "video_quality"
        const val KEY_SIGNED_IN_EMAIL = "signed_in_email"
        
        // Secure keys
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_TOKEN_EXPIRY = "token_expiry"
        
        // Default values
        const val DEFAULT_FRAME_RATE = 10
        const val DEFAULT_VIDEO_QUALITY = "HD"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Encrypted preferences for sensitive data (tokens)
    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular prefs if encryption fails
            prefs
        }
    }
    
    // Frame rate for extraction (1-5 FPS)
    var frameRate: Int
        get() = prefs.getInt(KEY_FRAME_RATE, DEFAULT_FRAME_RATE)
        set(value) = prefs.edit { putInt(KEY_FRAME_RATE, value.coerceIn(1, 5)) }
    
    // Video quality setting
    var videoQuality: String
        get() = prefs.getString(KEY_VIDEO_QUALITY, DEFAULT_VIDEO_QUALITY) ?: DEFAULT_VIDEO_QUALITY
        set(value) = prefs.edit { putString(KEY_VIDEO_QUALITY, value) }
    
    // Signed-in user email (display only)
    var signedInEmail: String?
        get() = prefs.getString(KEY_SIGNED_IN_EMAIL, null)
        set(value) = prefs.edit { putString(KEY_SIGNED_IN_EMAIL, value) }
    
    // OAuth access token (encrypted)
    var accessToken: String?
        get() = securePrefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = securePrefs.edit { putString(KEY_ACCESS_TOKEN, value) }
    
    // OAuth refresh token (encrypted)
    var refreshToken: String?
        get() = securePrefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = securePrefs.edit { putString(KEY_REFRESH_TOKEN, value) }
    
    // Guest mode flag
    var isGuestMode: Boolean
        get() = prefs.getBoolean("is_guest_mode", false)
        set(value) = prefs.edit { putBoolean("is_guest_mode", value) }
    
    // Token expiry timestamp
    var tokenExpiry: Long
        get() = securePrefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        set(value) = securePrefs.edit { putLong(KEY_TOKEN_EXPIRY, value) }
    
    /**
     * Check if user is signed in to Google
     */
    /**
     * Check if user is signed in to Google
     */
    fun isSignedIn(): Boolean {
        return !signedInEmail.isNullOrBlank()
    }
    
    /**
     * Check if token is expired
     * Not used with GoogleAccountCredential
     */
    fun isTokenExpired(): Boolean {
        return false
    }
    
    /**
     * Save OAuth tokens after successful sign-in
     * Not used with GoogleAccountCredential
     */
    fun saveTokens(access: String, refresh: String?, expiresInSeconds: Long) {
        // No-op: Tokens managed by GoogleAccountCredential
    }
    
    /**
     * Clear all auth data on sign-out
     */
    fun clearAuth() {
        accessToken = null
        refreshToken = null
        tokenExpiry = 0L
        signedInEmail = null
    }
    
    /**
     * Clear all settings
     */
    fun clearAll() {
        prefs.edit { clear() }
        securePrefs.edit { clear() }
    }
}
