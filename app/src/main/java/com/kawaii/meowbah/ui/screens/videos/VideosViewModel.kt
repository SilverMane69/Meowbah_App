package com.kawaii.meowbah.ui.screens.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kawaii.meowbah.BuildConfig // Added import for BuildConfig
import com.kawaii.meowbah.data.remote.YoutubeApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

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

data class PlaceholderContentDetails(
    val duration: String?
)

data class PlaceholderYoutubeVideoItem(
    val id: PlaceholderId?,
    val snippet: PlaceholderSnippet?,
    val statistics: PlaceholderStatistics?
)

data class PlaceholderYoutubeVideoListResponse(
    val items: List<PlaceholderYoutubeVideoItem>
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

    // API key is now fetched from BuildConfig
    private val apiKey = BuildConfig.YOUTUBE_API_KEY 
    private val channelId = "UCNytjdD5-KZInxjVeWV_qQw"

    fun fetchVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // Request description for logging - API key value is not directly logged here.
            val requestDescription = "youtubeApiService.getChannelVideos(channelId=$channelId)"
            try {
                val response: PlaceholderYoutubeVideoListResponse = youtubeApiService.getChannelVideos(
                    part = "snippet,id",
                    channelId = channelId,
                    apiKey = apiKey, // Uses the apiKey from BuildConfig
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
                Log.d("VideosViewModel", "Successfully fetched videos for $requestDescription")

            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: "No error body"
                var detailedMessage = "HTTP ${e.code()}: ${e.message()}"
                try {
                    val errorJson = JSONObject(errorBody)
                    val specificReason = errorJson.optJSONObject("error")
                                               ?.optJSONArray("errors")
                                               ?.optJSONObject(0)
                                               ?.optString("reason", "Unknown reason")
                    val apiMessage = errorJson.optJSONObject("error")
                                              ?.optString("message", e.message()) 
                    detailedMessage = "API Error (HTTP ${e.code()}) - Reason: $specificReason, Message: $apiMessage"
                } catch (jsonE: Exception) {
                    Log.e("VideosViewModel", "Failed to parse error body as JSON: $errorBody", jsonE)
                }
                Log.e("VideosViewModel", "API Error for $requestDescription. Details: $detailedMessage\nRaw Error Body: $errorBody", e)
                _error.value = "API Error: $detailedMessage. Please check logs for full error body."
                _videos.value = emptyList()
            } catch (e: IOException) {
                Log.e("VideosViewModel", "Network Error for $requestDescription: ${e.message}", e)
                _error.value = "Network error. Please check your connection."
                _videos.value = emptyList()
            } catch (e: Exception) {
                Log.e("VideosViewModel", "Unexpected error for $requestDescription: ${e.message}", e)
                _error.value = "Failed to load videos due to an unexpected error."
                _videos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
