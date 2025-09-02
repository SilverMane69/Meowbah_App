package com.kawaii.meowbah.ui.screens.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kawaii.meowbah.data.remote.YoutubeApiService // Assuming this path is correct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log

// --- Placeholder Data Classes for API Interaction ---

data class PlaceholderId(
    val kind: String?,
    val videoId: String?
)

data class PlaceholderSnippet(
    val title: String?,
    val channelTitle: String?,
    val description: String?,
    val thumbnails: PlaceholderThumbnails?
)

data class PlaceholderThumbnails(
    val default: PlaceholderThumbnail?,
    val medium: PlaceholderThumbnail?,
    val high: PlaceholderThumbnail?
)

data class PlaceholderThumbnail(
    val url: String?
)

data class PlaceholderStatistics(
    val viewCount: String?
)

data class PlaceholderContentDetails( // Added for video duration
    val duration: String?
    // Other fields like dimension, definition, caption etc. can be added if needed
)

data class PlaceholderYoutubeVideoItem( // For items from youtube.search.list
    val id: PlaceholderId?,
    val snippet: PlaceholderSnippet?,
    val statistics: PlaceholderStatistics?
)

data class PlaceholderYoutubeVideoListResponse( // For response from youtube.search.list
    val items: List<PlaceholderYoutubeVideoItem>
)

// Placeholders for Video Details (youtube.videos.list)
data class PlaceholderVideoDetailItem(
    val id: String?,
    val snippet: PlaceholderSnippet?,
    val statistics: PlaceholderStatistics?,
    val contentDetails: PlaceholderContentDetails? // Added this field
)

data class PlaceholderVideoDetailResponse(
    val items: List<PlaceholderVideoDetailItem>
)

// --- End of Placeholder Data Classes ---


class VideosViewModel : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val youtubeApiService: YoutubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YoutubeApiService::class.java)
    }

    private val apiKey = "AIzaSyC2OviBSUQ4TIzR76g0doH6HX2b32LI10s" // IMPORTANT: Store securely and check validity!
    private val channelId = "UCNytjdD5-KZInxjVeWV_qQw"

    fun fetchVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: PlaceholderYoutubeVideoListResponse = youtubeApiService.getChannelVideos(
                    part = "snippet,id",
                    channelId = channelId,
                    apiKey = apiKey,
                    maxResults = 20,
                    type = "video",
                    order = "date"
                )

                _videos.value = response.items.map { apiItem ->
                    VideoItem(
                        id = apiItem.id?.videoId ?: "",
                        snippet = VideoSnippet(
                            title = apiItem.snippet?.title ?: "No Title",
                            channelTitle = apiItem.snippet?.channelTitle ?: "No Channel",
                            localized = LocalizedSnippet(
                                title = apiItem.snippet?.title ?: "No Title",
                                description = apiItem.snippet?.description ?: ""
                            ),
                            thumbnails = Thumbnails(
                                default = Thumbnail(url = apiItem.snippet?.thumbnails?.default?.url ?: "", width = 120, height = 90),
                                medium = Thumbnail(
                                    url = apiItem.snippet?.thumbnails?.medium?.url ?: "",
                                    width = 320,
                                    height = 180
                                ),
                                high = Thumbnail(url = apiItem.snippet?.thumbnails?.high?.url ?: "", width = 480, height = 360)
                            )
                        ),
                        statistics = VideoStatistics(
                            viewCount = apiItem.statistics?.viewCount ?: "0"
                        )
                    )
                }

            } catch (e: Exception) {
                Log.e("VideosViewModel", "Error fetching videos", e)
                _error.value = "Failed to load videos: ${e.localizedMessage ?: e.message}"
                _videos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
