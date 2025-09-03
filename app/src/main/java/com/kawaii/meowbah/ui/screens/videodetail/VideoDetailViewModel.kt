package com.kawaii.meowbah.ui.screens.videodetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kawaii.meowbah.BuildConfig
import com.kawaii.meowbah.data.remote.YoutubeApiService
import com.kawaii.meowbah.data.YoutubeVideoDetailResponse // For API response
import com.kawaii.meowbah.ui.screens.videos.PlaceholderVideoDetailItem
// VideoItem is no longer used as initialDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class VideoDetailViewModel : ViewModel() {

    private val _videoDetail = MutableStateFlow<PlaceholderVideoDetailItem?>(null)
    val videoDetail: StateFlow<PlaceholderVideoDetailItem?> = _videoDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val youtubeApiService: YoutubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YoutubeApiService::class.java)
    }
    private val apiKey = BuildConfig.YOUTUBE_API_KEY

    fun fetchVideoDetails(videoId: String) { // MODIFIED: Removed initialDetails parameter
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _videoDetail.value = null // Reset previous detail

            val requestDescription = "youtubeApiService.getVideoDetails(id=$videoId)"
            try {
                Log.d("VideoDetailViewModel", "Fetching details for $requestDescription")
                val apiResponse = youtubeApiService.getVideoDetails(
                    part = "snippet,contentDetails,statistics",
                    id = videoId,
                    apiKey = apiKey
                )

                if (apiResponse.isSuccessful) {
                    val apiItem = apiResponse.body()?.items?.firstOrNull()
                    if (apiItem != null) {
                        // Construct PlaceholderVideoDetailItem directly from API response
                        _videoDetail.value = PlaceholderVideoDetailItem(
                            id = apiItem.id ?: videoId, // Use apiItem.id, fallback to passed videoId
                            snippet = PlaceholderVideoDetailItem.Snippet(
                                title = apiItem.snippet?.title ?: "No Title",
                                description = apiItem.snippet?.description ?: "No Description",
                                thumbnails = PlaceholderVideoDetailItem.Thumbnails(
                                    default = PlaceholderVideoDetailItem.Thumbnail(apiItem.snippet?.thumbnails?.default?.url ?: ""),
                                    medium = PlaceholderVideoDetailItem.Thumbnail(apiItem.snippet?.thumbnails?.medium?.url ?: ""),
                                    high = PlaceholderVideoDetailItem.Thumbnail(apiItem.snippet?.thumbnails?.high?.url ?: "")
                                ),
                                channelTitle = apiItem.snippet?.channelTitle ?: "Unknown Channel",
                                publishedAt = apiItem.snippet?.publishedAt ?: ""
                            ),
                            contentDetails = PlaceholderVideoDetailItem.ContentDetails(
                                duration = apiItem.contentDetails?.duration ?: "P0D" // Default to "P0D" if null
                            ),
                            statistics = PlaceholderVideoDetailItem.Statistics(
                                viewCount = apiItem.statistics?.viewCount, // Can be null
                                likeCount = apiItem.statistics?.likeCount  // Can be null
                            )
                        )
                        Log.d("VideoDetailViewModel", "Successfully mapped details for $requestDescription")
                    } else {
                        Log.w("VideoDetailViewModel", "No video details found in API response for $requestDescription. Items list was null or empty.")
                        _error.value = "Detailed video information not found."
                    }
                } else {
                    val errorBody = apiResponse.errorBody()?.string() ?: "Unknown API error"
                    var detailedMessage = "API Error (HTTP ${apiResponse.code()}) for $requestDescription"
                    try {
                        val errorJson = JSONObject(errorBody)
                        detailedMessage = errorJson.optJSONObject("error")?.optString("message", apiResponse.message()) ?: detailedMessage
                    } catch (jsonE: Exception) { /* Ignore if not JSON */ }
                    Log.e("VideoDetailViewModel", "API call failed: $detailedMessage. Raw: $errorBody")
                    _error.value = "Failed to load video details: $detailedMessage"
                }
            } catch (e: HttpException) {
                Log.e("VideoDetailViewModel", "HttpException for $requestDescription: ${e.message}", e)
                _error.value = "Network request error: ${e.message}"
            } catch (e: IOException) {
                Log.e("VideoDetailViewModel", "IOException for $requestDescription: ${e.message}", e)
                _error.value = "Network connection error. Please check your connection."
            } catch (e: Exception) {
                Log.e("VideoDetailViewModel", "Unexpected error for $requestDescription: ${e.message}", e)
                _error.value = "An unexpected error occurred while fetching details."
            } finally {
                _isLoading.value = false
                Log.d("VideoDetailViewModel", "Finished fetching details for $requestDescription. Loading: ${_isLoading.value}, Error: ${_error.value}, Detail: ${_videoDetail.value != null}")
            }
        }
    }
}
