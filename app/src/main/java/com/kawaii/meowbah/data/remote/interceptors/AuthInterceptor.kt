package com.kawaii.meowbah.data.remote.interceptors

import android.util.Log
import com.kawaii.meowbah.data.TokenStorageService
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenStorageService: TokenStorageService) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = tokenStorageService.getAccessToken() // getAccessToken already checks for expiry

        if (accessToken != null) {
            Log.d(TAG, "Valid access token found. Adding Authorization header.")
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
            return chain.proceed(authenticatedRequest)
        } else {
            Log.d(TAG, "No valid access token found. Proceeding with original request (likely API key only).")
            // If the request requires OAuth and no token is present, it will likely fail.
            // If the request uses an API key (as a query parameter), it might proceed for public data.
        }
        return chain.proceed(originalRequest)
    }
}
