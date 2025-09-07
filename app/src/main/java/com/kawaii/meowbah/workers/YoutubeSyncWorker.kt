package com.kawaii.meowbah.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kawaii.meowbah.BuildConfig
import com.kawaii.meowbah.data.remote.YoutubeApiService
import com.kawaii.meowbah.data.YoutubeRepository
// Removed: import com.kawaii.meowbah.ui.screens.videos.PlaceholderYoutubeVideoItem
import com.kawaii.meowbah.data.remote.YoutubeVideoItem // Added: Import for the DTO from data.remote
import com.kawaii.meowbah.ui.activities.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class YoutubeSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "YoutubeSyncWorker"
        private const val TAG = "YoutubeSyncWorker"
        private const val PREFS_NAME = "YoutubeSyncPrefs"
        private const val KEY_NOTIFIED_VIDEO_IDS = "notifiedVideoIds"
        private const val KEY_LAST_PROCESSED_NEWEST_PUBLISHED_AT = "lastProcessedNewestPublishedAt"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting Youtube sync work")

        val youtubeRepository = YoutubeRepository(YoutubeApiService.create())
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val notifiedVideoIds = prefs.getStringSet(KEY_NOTIFIED_VIDEO_IDS, mutableSetOf()) ?: mutableSetOf()
        val lastProcessedNewestPublishedAtString = prefs.getString(KEY_LAST_PROCESSED_NEWEST_PUBLISHED_AT, null)

        var lastKnownNewestVideoDate: OffsetDateTime? = null
        if (lastProcessedNewestPublishedAtString != null) {
            try {
                lastKnownNewestVideoDate = OffsetDateTime.parse(lastProcessedNewestPublishedAtString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                Log.d(TAG, "Loaded last processed newest video date: $lastKnownNewestVideoDate")
            } catch (e: DateTimeParseException) {
                Log.e(TAG, "Error parsing stored last processed date: $lastProcessedNewestPublishedAtString", e)
            }
        } else {
            Log.d(TAG, "No last processed newest video date found in SharedPreferences.")
        }

        return withContext(Dispatchers.IO) {
            try {
                // The response.body() will be YoutubeVideoListResponse (from data.remote)
                // which contains List<YoutubeVideoItem>
                val response = youtubeRepository.getChannelVideos(
                    part = "snippet,id", // Ensure id is fetched for videoId
                    channelId = "UCzUnbX-2S2mMcfdd1jR2g-Q", 
                    apiKey = BuildConfig.YOUTUBE_API_KEY,
                    maxResults = 50, 
                    type = "video",
                    order = "date" 
                )

                if (response.isSuccessful) {
                    // videoItemList is now List<YoutubeVideoItem>?
                    val videoItemList: List<YoutubeVideoItem>? = response.body()?.items

                    if (videoItemList != null) {
                        Log.d(TAG, "Successfully fetched ${videoItemList.size} videos.")
                        val tempNotifiedIds = notifiedVideoIds.toMutableSet()
                        var newVideosFoundThisRun = false
                        var currentBatchOverallNewestVideoDate = lastKnownNewestVideoDate

                        // videoItem is now com.kawaii.meowbah.data.remote.YoutubeVideoItem
                        for (videoItem: YoutubeVideoItem in videoItemList) {
                            val videoId = videoItem.id?.videoId // Access from YoutubeId
                            val videoTitle = videoItem.snippet?.title // Access from YoutubeSnippet
                            val videoPublishedAtString = videoItem.snippet?.publishedAt // Access from YoutubeSnippet

                            if (videoId == null || videoTitle == null || videoPublishedAtString == null) {
                                Log.w(TAG, "Skipping video with missing ID, title, or publishedAt: $videoItem")
                                continue
                            }

                            val currentVideoDate: OffsetDateTime = try {
                                OffsetDateTime.parse(videoPublishedAtString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            } catch (e: DateTimeParseException) {
                                Log.e(TAG, "Error parsing video publishedAt date: $videoPublishedAtString for video ID $videoId", e)
                                continue 
                            }

                            if (lastKnownNewestVideoDate != null && !currentVideoDate.isAfter(lastKnownNewestVideoDate)) {
                                Log.d(TAG, "Video $videoTitle (Published: $currentVideoDate) is not newer than last processed ($lastKnownNewestVideoDate). Stopping check.")
                                break 
                            }

                            if (tempNotifiedIds.contains(videoId)) {
                                Log.d(TAG, "Video $videoTitle (ID: $videoId) already in notified set, skipping.")
                                continue
                            }

                            Log.i(TAG, "New video found: $videoTitle (ID: $videoId, Published: $currentVideoDate)")
                            NotificationUtils.showNewVideoNotification(
                                applicationContext,
                                videoTitle,
                                videoId,
                                null // Passing null for thumbnailPath
                            )
                            tempNotifiedIds.add(videoId)
                            newVideosFoundThisRun = true

                            if (currentBatchOverallNewestVideoDate == null || currentVideoDate.isAfter(currentBatchOverallNewestVideoDate)) {
                                currentBatchOverallNewestVideoDate = currentVideoDate
                            }
                        }

                        if (newVideosFoundThisRun) {
                            val editor = prefs.edit()
                            editor.putStringSet(KEY_NOTIFIED_VIDEO_IDS, tempNotifiedIds)
                            if (currentBatchOverallNewestVideoDate != null && 
                                (lastKnownNewestVideoDate == null || currentBatchOverallNewestVideoDate.isAfter(lastKnownNewestVideoDate))) {
                                editor.putString(KEY_LAST_PROCESSED_NEWEST_PUBLISHED_AT, currentBatchOverallNewestVideoDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                Log.d(TAG, "Updated last processed newest video date to: $currentBatchOverallNewestVideoDate")
                            }
                            editor.apply()
                            Log.d(TAG, "Updated SharedPreferences. Total notified IDs: ${tempNotifiedIds.size}")
                        } else {
                            Log.d(TAG, "No new videos found to notify in this run based on date and ID checks.")
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
