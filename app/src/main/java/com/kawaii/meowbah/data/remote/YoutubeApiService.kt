package com.kawaii.meowbah.data.remote

import com.kawaii.meowbah.data.YoutubeVideoDetailResponse
import com.kawaii.meowbah.ui.screens.videos.PlaceholderYoutubeVideoListResponse
import retrofit2.Response // Added import for Response wrapper
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeApiService {
    @GET("search")
    suspend fun getChannelVideos(
        @Query("part") part: String,
        @Query("channelId") channelId: String,
        @Query("key") apiKey: String,
        @Query("maxResults") maxResults: Int,
        @Query("type") type: String,
        @Query("order") order: String
    ): Response<PlaceholderYoutubeVideoListResponse> // Changed return type to Response<T>

    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String,
        @Query("id") id: String,
        @Query("key") apiKey: String
    ): Response<YoutubeVideoDetailResponse> // Also ensuring this returns Response<T> for consistency

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"

        fun create(): YoutubeApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(YoutubeApiService::class.java)
        }
    }
}
