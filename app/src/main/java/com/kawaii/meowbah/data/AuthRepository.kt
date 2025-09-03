package com.kawaii.meowbah.data

import android.util.Log
import com.kawaii.meowbah.data.model.TokenResponse
import com.kawaii.meowbah.data.remote.AuthApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val authApiService: AuthApiService) {

    companion object {
        private const val TAG = "AuthRepository"
        // Web Client ID from Google API Console, used for server-side validation/exchange
        private const val CLIENT_ID = "691684333330-k4a1q7tq2cbfm023mal00h1h1ffd6g2q.apps.googleusercontent.com" // CORRECTED CLIENT_ID
        private const val GRANT_TYPE_AUTH_CODE = "authorization_code"
        // private const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token" // For later
    }

    suspend fun exchangeCodeForTokens(authCode: String): Result<TokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to exchange auth code for tokens. Auth Code Length: ${authCode.length}")
                val response = authApiService.exchangeAuthCodeForToken(
                    grantType = GRANT_TYPE_AUTH_CODE,
                    clientId = CLIENT_ID, // Uses the corrected CLIENT_ID above
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
