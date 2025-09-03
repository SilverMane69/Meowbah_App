package com.kawaii.meowbah.data

import com.google.gson.annotations.SerializedName
import com.kawaii.meowbah.data.remote.YoutubeApiService // Added for YoutubeRepository
import com.kawaii.meowbah.ui.screens.videos.PlaceholderYoutubeVideoListResponse // Added for YoutubeRepository
import retrofit2.Response // Added for YoutubeRepository

// Response for a list of videos (e.g., from search or playlist)
data class YoutubeVideoListResponse(
    @SerializedName("items") val items: List<YoutubeVideoItem>
)

// Item typically used in a list, like search results
data class YoutubeVideoItem(
    @SerializedName("id") val id: VideoId, // For search results, id is an object
    @SerializedName("snippet") val snippet: VideoSnippet
)

data class VideoId( // Used by YoutubeVideoItem for search results
    @SerializedName("videoId") val videoId: String
)

// This Snippet class is used by both search results and video details
data class VideoSnippet(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("thumbnails") val thumbnails: Thumbnails
    // Consider adding publishedAt, channelTitle, etc. if needed from snippet
)

data class Thumbnails(
    @SerializedName("high") val high: Thumbnail,
    @SerializedName("medium") val medium: Thumbnail,
    @SerializedName("default") val default: Thumbnail
)

data class Thumbnail(
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)

// --- Data classes for Video Detail (REMOVED - Now in YoutubeVideoDetailResponse.kt) ---
// The following were removed to prevent redeclaration:
// data class YoutubeVideoDetailResponse(...)
// data class YoutubeVideoDetailItem(...)
// data class YoutubeContentDetails(...)

// --- YoutubeRepository class added here ---
class YoutubeRepository(private val youtubeApiService: YoutubeApiService) {
    suspend fun getChannelVideos(
        part: String,
        channelId: String,
        apiKey: String,
        maxResults: Int,
        type: String,
        order: String
    ): Response<PlaceholderYoutubeVideoListResponse> {
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
