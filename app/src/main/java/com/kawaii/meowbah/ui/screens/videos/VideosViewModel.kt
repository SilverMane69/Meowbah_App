package com.kawaii.meowbah.ui.screens.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kawaii.meowbah.BuildConfig
import com.kawaii.meowbah.data.remote.YoutubeApiService
import com.kawaii.meowbah.data.CachedVideoInfo
import com.kawaii.meowbah.data.remote.YoutubeVideoListResponse // Added import for the DTO from data.remote
// Removed Placeholder DTO imports as they will be deleted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import retrofit2.Response

// --- Local Placeholder Data Classes are removed as we now use DTOs from data.remote ---

class VideosViewModel : ViewModel() {

    private val _videos = MutableStateFlow<List<CachedVideoInfo>>(emptyList())
    val videos: StateFlow<List<CachedVideoInfo>> = _videos

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

    private val apiKey = BuildConfig.YOUTUBE_API_KEY
    private val channelId = "UCzUnbX-2S2mMcfdd1jR2g-Q" // Meowbah's Channel ID

    fun fetchVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val requestDescription = "youtubeApiService.getChannelVideos(channelId=$channelId)"
            try {
                // Using YoutubeVideoListResponse from data.remote package
                val apiResponse: Response<YoutubeVideoListResponse> = youtubeApiService.getChannelVideos(
                    part = "snippet,id", 
                    channelId = channelId,
                    apiKey = apiKey,
                    maxResults = 20, 
                    type = "video",
                    order = "date"
                )

                if (apiResponse.isSuccessful) {
                    val remoteData = apiResponse.body()
                    if (remoteData != null) {
                        // apiItem is now com.kawaii.meowbah.data.remote.YoutubeVideoItem
                        _videos.value = remoteData.items.mapNotNull { apiItem ->
                            val videoId = apiItem.id?.videoId // Accessing videoId from YoutubeId
                            if (videoId == null) {
                                Log.w("VideosViewModel", "Skipping video item with null ID: $apiItem")
                                return@mapNotNull null 
                            }
                            CachedVideoInfo(
                                id = videoId,
                                title = apiItem.snippet?.title ?: "No Title", // Accessing from YoutubeSnippet
                                description = apiItem.snippet?.description, // Accessing from YoutubeSnippet
                                publishedAt = apiItem.snippet?.publishedAt, // Accessing from YoutubeSnippet
                                cachedThumbnailPath = null 
                            )
                        }
                        Log.d("VideosViewModel", "Successfully fetched and mapped videos for $requestDescription. Count: ${_videos.value.size}")
                    } else {
                        Log.e("VideosViewModel", "API call successful but response body is null for $requestDescription")
                        _error.value = "Failed to load videos: Empty response from server."
                        _videos.value = emptyList()
                    }
                } else {
                    val errorBody = apiResponse.errorBody()?.string() ?: "Unknown error"
                    var detailedMessage = "API Error (HTTP ${apiResponse.code()})"
                    try {
                        val errorJson = JSONObject(errorBody)
                        val specificReason = errorJson.optJSONObject("error")
                                                   ?.optJSONArray("errors")
                                                   ?.optJSONObject(0)
                                                   ?.optString("reason", "Unknown reason")
                        val apiMessageFromJson = errorJson.optJSONObject("error")
                                                  ?.optString("message", apiResponse.message()) 
                        detailedMessage = "API Error (HTTP ${apiResponse.code()}) - Reason: $specificReason, Message: $apiMessageFromJson"
                    } catch (jsonE: Exception) {
                        Log.e("VideosViewModel", "Failed to parse error body as JSON: $errorBody", jsonE)
                        detailedMessage = "API Error (HTTP ${apiResponse.code()}). Could not parse error details. Raw: $errorBody"
                    }
                    Log.e("VideosViewModel", "API call failed for $requestDescription. Code: ${apiResponse.code()}, Error: $errorBody")
                    _error.value = detailedMessage
                    _videos.value = emptyList()
                }

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
                Log.e("VideosViewModel", "HttpException for $requestDescription. Details: $detailedMessage\nRaw Error Body: $errorBody", e)
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
