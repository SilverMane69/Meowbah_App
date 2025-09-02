package com.kawaii.meowbah.ui.screens.videodetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kawaii.meowbah.data.YoutubeVideoDetailItem
import com.kawaii.meowbah.data.remote.YoutubeApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow // Added this import
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class VideoDetailViewModel(
    private val videoId: String,
    private val apiKey: String // Keep for simplicity, ideally inject ApiService
) : ViewModel() {

    private val _videoDetailItem = MutableStateFlow<YoutubeVideoDetailItem?>(null)
    val videoDetailItem: StateFlow<YoutubeVideoDetailItem?> = _videoDetailItem.asStateFlow() // Changed here

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow() // Changed here

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow() // Changed here

    private val youtubeApiService: YoutubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YoutubeApiService::class.java)
    }

    fun fetchVideoDetails() {
        if (videoId.isEmpty()) {
            _error.value = "Video ID is missing."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // The API for a single video by ID returns a list, so we take the first item.
                val response = youtubeApiService.getVideoDetails(
                    part = "snippet,contentDetails",
                    id = videoId,
                    apiKey = apiKey
                )
                if (response.items.isNotEmpty()) {
                    _videoDetailItem.value = response.items[0]
                } else {
                    _error.value = "Video not found."
                    _videoDetailItem.value = null
                }
            } catch (e: Exception) {
                Log.e("VideoDetailViewModel", "Error fetching video details for ID $videoId", e)
                _error.value = "Failed to load video details: ${e.localizedMessage}"
                _videoDetailItem.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Factory to pass videoId and apiKey to the ViewModel
class VideoDetailViewModelFactory(
    private val videoId: String,
    private val apiKey: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoDetailViewModel(videoId, apiKey) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
