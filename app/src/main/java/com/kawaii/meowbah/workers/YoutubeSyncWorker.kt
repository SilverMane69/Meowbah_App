package com.kawaii.meowbah.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kawaii.meowbah.BuildConfig
import com.kawaii.meowbah.data.remote.YoutubeApiService
import com.kawaii.meowbah.data.YoutubeRepository // Assuming this import is correct and the file/class is accessible
// Explicitly import PlaceholderYoutubeVideoItem to avoid alias issues
import com.kawaii.meowbah.ui.screens.videos.PlaceholderYoutubeVideoItem 
import com.kawaii.meowbah.ui.activities.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class YoutubeSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "YoutubeSyncWorker"
        private const val TAG = "YoutubeSyncWorker"
        private const val PREFS_NAME = "YoutubeSyncPrefs"
        private const val KEY_NOTIFIED_VIDEO_IDS = "notifiedVideoIds"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting Youtube sync work")
        // This line assumes YoutubeRepository from com.kawaii.meowbah.data is resolved
        val youtubeRepository = YoutubeRepository(YoutubeApiService.create())
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notifiedVideoIds = prefs.getStringSet(KEY_NOTIFIED_VIDEO_IDS, emptySet()) ?: emptySet()

        return withContext(Dispatchers.IO) {
            try {
                val response = youtubeRepository.getChannelVideos(
                    part = "snippet", 
                    channelId = "UCzUnbX-2S2mMcfdd1jR2g-Q", 
                    apiKey = BuildConfig.YOUTUBE_API_KEY,
                    maxResults = 10,
                    type = "video",
                    order = "date" 
                )

                if (response.isSuccessful) {
                    // response.body() is PlaceholderYoutubeVideoListResponse?
                    // .items is List<PlaceholderYoutubeVideoItem>?
                    val videoItemList: List<PlaceholderYoutubeVideoItem>? = response.body()?.items

                    if (videoItemList != null) {
                        Log.d(TAG, "Successfully fetched ${videoItemList.size} videos")
                        val newNotifiedIds = notifiedVideoIds.toMutableSet()
                        var newVideosFound = 0

                        // videoItem is now explicitly PlaceholderYoutubeVideoItem
                        for (videoItem: PlaceholderYoutubeVideoItem in videoItemList.reversed()) { 
                            // Accessing fields from PlaceholderYoutubeVideoItem, PlaceholderId, PlaceholderSnippet
                            val videoId = videoItem.id?.videoId 
                            val videoTitle = videoItem.snippet?.title

                            if (videoId != null && videoTitle != null && !notifiedVideoIds.contains(videoId)) {
                                Log.d(TAG, "New video found: $videoTitle (ID: $videoId)")
                                NotificationUtils.showNewVideoNotification(
                                    applicationContext,
                                    videoTitle, 
                                    videoId    
                                )
                                newNotifiedIds.add(videoId) 
                                newVideosFound++
                            }
                        }

                        if (newVideosFound > 0) {
                            prefs.edit().putStringSet(KEY_NOTIFIED_VIDEO_IDS, newNotifiedIds).apply()
                            Log.d(TAG, "Updated notified video IDs. Total notified: ${newNotifiedIds.size}")
                        } else {
                            Log.d(TAG, "No new videos found to notify.")
                        }
                        Result.success()
                    } else {
                        Log.e(TAG, "Video items list is null or response body is null")
                        Result.failure()
                    }
                } else {
                    Log.e(TAG, "API call failed with code: ${response.code()}, message: ${response.message()}")
                    Result.failure()
                }
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP exception during Youtube sync", e)
                Result.retry()
            } catch (e: IOException) {
                Log.e(TAG, "IO exception during Youtube sync (likely network issue)", e)
                Result.retry()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during Youtube sync", e)
                Result.failure()
            }
        }
    }
}
