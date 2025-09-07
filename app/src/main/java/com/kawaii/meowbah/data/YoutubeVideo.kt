package com.kawaii.meowbah.data

// Removed redundant data class definitions that are now in com.kawaii.meowbah.data.remote.YoutubeApiData.kt

import com.kawaii.meowbah.data.remote.YoutubeApiService
import com.kawaii.meowbah.data.remote.YoutubeVideoListResponse // Updated import
import retrofit2.Response

class YoutubeRepository(private val youtubeApiService: YoutubeApiService) {
    suspend fun getChannelVideos(
        part: String,
        channelId: String,
        apiKey: String,
        maxResults: Int,
        type: String,
        order: String
    ): Response<YoutubeVideoListResponse> { // Updated return type
        return youtubeApiService.getChannelVideos(
            part = part,
            channelId = channelId,
            apiKey = apiKey,
            maxResults = maxResults,
            type = type,
            order = order
        )
    }
}
