package com.kawaii.meowbah.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kawaii.meowbah.BuildConfig
import com.kawaii.meowbah.data.remote.YoutubeApiService
import com.kawaii.meowbah.ui.screens.videos.PlaceholderYoutubeVideoListResponse // Using existing placeholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.json.JSONObject
import java.io.IOException

class YoutubeSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val youtubeApiService: YoutubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YoutubeApiService::class.java)
    }

    companion object {
        const val WORK_NAME = "YoutubeSyncWorker"
        private const val TAG = "YoutubeSyncWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.YOUTUBE_API_KEY
        val channelId = "UCNytjdD5-KZInxjVeWV_qQw" // Meowbah's Channel ID
        val requestDescription = "YoutubeSyncWorker - getChannelVideos(channelId=$channelId)"

        if (apiKey.isEmpty()) {
            Log.e(TAG, "API Key is empty. Cannot perform sync.")
            return@withContext Result.failure()
        }

        Log.d(TAG, "Starting YouTube video sync for $requestDescription")

        try {
            val response: PlaceholderYoutubeVideoListResponse = youtubeApiService.getChannelVideos(
                part = "snippet,id",
                channelId = channelId,
                apiKey = apiKey,
                maxResults = 25, // Fetches a reasonable number of recent videos
                type = "video",
                order = "date"
            )

            // In a full implementation, you would save response.items to a local database (e.g., Room)
            // For now, we'll just log the number of items fetched.
            Log.d(TAG, "Successfully fetched ${response.items.size} videos for $requestDescription.")
            // Example: response.items.forEach { video -> Log.d(TAG, "Fetched video: ${video.snippet?.title}") }
            
            Result.success()

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
                Log.e(TAG, "Failed to parse error body as JSON for $requestDescription: $errorBody", jsonE)
            }
            Log.e(TAG, "API Error for $requestDescription. Details: $detailedMessage\nRaw Error Body: $errorBody", e)
            // Retry if it's a server error (5xx) or potentially a rate limit issue (though 403 usually isn't transient for this API)
            if (e.code() >= 500) {
                Result.retry()
            } else {
                Result.failure() // Don't retry for client errors like 403 immediately without changes
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network Error for $requestDescription: ${e.message}", e)
            Result.retry() // Network errors are good candidates for retry
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during $requestDescription: ${e.message}", e)
            Result.failure()
        }
    }
}
