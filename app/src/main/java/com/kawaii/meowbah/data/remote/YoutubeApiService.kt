package com.kawaii.meowbah.data.remote

import com.kawaii.meowbah.data.YoutubeVideoDetailResponse // Import the new response type
// Removed: import com.kawaii.meowbah.data.YoutubeVideoListResponse
import com.kawaii.meowbah.ui.screens.videos.PlaceholderYoutubeVideoListResponse // Added import
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
    ): PlaceholderYoutubeVideoListResponse // Changed return type

    @GET("videos") // Endpoint for getting video details by ID
    suspend fun getVideoDetails(
        @Query("part") part: String, // e.g., "snippet,contentDetails"
        @Query("id") id: String,     // Video ID
        @Query("key") apiKey: String
    ): YoutubeVideoDetailResponse // Return the new response type
}
