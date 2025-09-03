package com.kawaii.meowbah.data

import android.util.Log
import com.kawaii.meowbah.data.model.TokenResponse
import com.kawaii.meowbah.data.remote.AuthApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val authApiService: AuthApiService) {

    companion object {
        private const val TAG = "AuthRepository"
        // Your Android app's client ID from Google Cloud Console
        private const val CLIENT_ID = "1017808544125-taqamrcgk5n6rbm0llubi8t1dge6hi4c.apps.googleusercontent.com"
        private const val GRANT_TYPE_AUTH_CODE = "authorization_code"
        // private const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token" // For later
    }

    suspend fun exchangeCodeForTokens(authCode: String): Result<TokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to exchange auth code for tokens. Auth Code Length: ${authCode.length}") // Log length for privacy
                val response = authApiService.exchangeAuthCodeForToken(
                    grantType = GRANT_TYPE_AUTH_CODE,
                    clientId = CLIENT_ID,
                    authCode = authCode
                )

                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d(TAG, "Token exchange successful. Access Token: ${it.accessToken.take(10)}..., Refresh Token: ${it.refreshToken?.take(10)}...")
                        Result.success(it)
                    } ?: run {
                        Log.e(TAG, "Token exchange successful but response body is null.")
                        Result.failure(Exception("Token exchange response body is null"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Token exchange failed. Code: ${response.code()}, Message: ${response.message()}, Error Body: $errorBody")
                    Result.failure(Exception("Token exchange failed: ${response.code()} ${response.message()} - $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during token exchange", e)
                Result.failure(e)
            }
        }
    }

    // Placeholder for refresh token logic
    // suspend fun refreshAccessToken(refreshToken: String): Result<TokenResponse> { ... }
}
