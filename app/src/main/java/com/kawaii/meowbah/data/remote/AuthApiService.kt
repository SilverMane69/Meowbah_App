package com.kawaii.meowbah.data.remote

import com.kawaii.meowbah.data.model.TokenResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AuthApiService {

    @FormUrlEncoded
    @POST("token") // Changed to relative path
    suspend fun exchangeAuthCodeForToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("code") authCode: String
    ): Response<TokenResponse>

    // Placeholder for token refresh function
    // @FormUrlEncoded
    // @POST("token")
    // suspend fun refreshAccessToken(
    //     @Field("grant_type") grantType: String = "refresh_token",
    //     @Field("client_id") clientId: String,
    //     @Field("refresh_token") refreshToken: String
    // ): Response<TokenResponse> 

    companion object {
        private const val BASE_URL = "https://oauth2.googleapis.com/"

        fun create(): AuthApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(AuthApiService::class.java)
        }
    }
}
