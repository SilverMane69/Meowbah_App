package com.kawaii.meowbah.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kawaii.meowbah.data.model.TokenResponse

class TokenStorageService(context: Context) {

    companion object {
        private const val TAG = "TokenStorageService"
        private const val PREF_FILE_NAME = "meowbah_oauth_tokens"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ACCESS_TOKEN_EXPIRY_TIME_MS = "access_token_expiry_time_ms"
    }

    private val masterKey: MasterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext, // Use application context
        PREF_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(tokenResponse: TokenResponse) {
        // expiresIn is in seconds, convert to milliseconds for expiry time calculation
        val expiresInMs = tokenResponse.expiresIn * 1000L 
        val expiryTimeMs = System.currentTimeMillis() + expiresInMs
        with(sharedPreferences.edit()) {
            putString(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
            tokenResponse.refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
                ?: remove(KEY_REFRESH_TOKEN) // Clear refresh token if null in response
            putLong(KEY_ACCESS_TOKEN_EXPIRY_TIME_MS, expiryTimeMs)
            apply()
        }
        Log.d(TAG, "Tokens saved. Access token expires around: ${java.util.Date(expiryTimeMs)}. Refresh token present: ${tokenResponse.refreshToken != null}")
    }

    fun getAccessToken(): String? {
        val token = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
        val expiryTime = sharedPreferences.getLong(KEY_ACCESS_TOKEN_EXPIRY_TIME_MS, 0L)

        return if (token != null && System.currentTimeMillis() < expiryTime) {
            Log.d(TAG, "Retrieved valid access token.")
            token
        } else {
            if (token != null && System.currentTimeMillis() >= expiryTime) {
                Log.w(TAG, "Access token found but has expired.")
            } else if (token == null){
                Log.d(TAG, "No access token found in storage.")
            }
            clearTokens() // Clear expired or invalid tokens
            null 
        }
    }

    fun getRefreshToken(): String? {
        val refreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
        if (refreshToken != null) {
            Log.d(TAG, "Retrieved refresh token.")
        } else {
            Log.d(TAG, "No refresh token found in storage.")
        }
        return refreshToken
    }

    fun clearTokens() {
        with(sharedPreferences.edit()) {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_ACCESS_TOKEN_EXPIRY_TIME_MS)
            apply()
        }
        Log.d(TAG, "All OAuth tokens cleared from secure storage.")
    }

    fun isAccessTokenValid(): Boolean {
        val token = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
        val expiryTime = sharedPreferences.getLong(KEY_ACCESS_TOKEN_EXPIRY_TIME_MS, 0L)
        val isValid = token != null && System.currentTimeMillis() < expiryTime
        Log.d(TAG, "Checked access token validity: $isValid")
        return isValid
    }
}
