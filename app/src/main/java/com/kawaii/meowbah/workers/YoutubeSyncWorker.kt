package com.kawaii.meowbah.workers

// import android.appwidget.AppWidgetManager // Widget-related import removed
// import android.content.ComponentName // Widget-related import removed
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kawaii.meowbah.BuildConfig
// import com.kawaii.meowbah.R // Widget-related import removed
import com.kawaii.meowbah.data.CachedVideoInfo
import com.kawaii.meowbah.data.VideoContentProviderContract
import com.kawaii.meowbah.data.remote.YoutubeApiService
import com.kawaii.meowbah.data.remote.YoutubeVideoListResponse
import com.kawaii.meowbah.data.remote.YoutubeVideoItem
import com.kawaii.meowbah.data.YoutubeRepository
import com.kawaii.meowbah.ui.activities.NotificationUtils
// import com.kawaii.meowbah.widgets.VideoWidgetProvider // Widget-related import removed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class YoutubeSyncWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "YoutubeSyncWorker"
        private const val TAG = "YoutubeSyncWorker"
        private const val PREFS_NAME = "YoutubeSyncPrefs"
        private const val KEY_NOTIFIED_VIDEO_IDS = "notifiedVideoIds"
    }

    private suspend fun cacheThumbnail(videoId: String, thumbnailUrl: String?): String? = withContext(Dispatchers.IO) {
        if (thumbnailUrl.isNullOrEmpty()) {
            return@withContext null
        }
        val thumbnailsDir = File(appContext.cacheDir, "thumbnails")
        if (!thumbnailsDir.exists()) thumbnailsDir.mkdirs()
        val cachedFile = File(thumbnailsDir, "$videoId.jpg")
        var fos: FileOutputStream? = null
        var connection: HttpURLConnection? = null
        var inputStream: java.io.InputStream? = null
        try {
            val url = URL(thumbnailUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    fos = FileOutputStream(cachedFile)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                    return@withContext cachedFile.absolutePath
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "cacheThumbnail (Worker) Exception for $videoId: ${e.message}", e)
        } finally {
            fos?.close()
            inputStream?.close()
            connection?.disconnect()
        }
        return@withContext null
    }

    private suspend fun updateVideoDataInContentProvider(videoList: List<CachedVideoInfo>) = withContext(Dispatchers.IO) {
        val contentResolver = appContext.contentResolver
        Log.d(TAG, "Worker: Updating ContentProvider. Deleting old data...")
        try {
            val deletedRows = contentResolver.delete(VideoContentProviderContract.CONTENT_URI_VIDEOS, null, null)
            Log.d(TAG, "Worker: Deleted $deletedRows rows from ContentProvider.")

            if (videoList.isNotEmpty()) {
                val contentValuesArray = videoList.map {
                    ContentValues().apply {
                        put(VideoContentProviderContract.VideoColumns.COLUMN_VIDEO_ID, it.id)
                        put(VideoContentProviderContract.VideoColumns.COLUMN_TITLE, it.title)
                        put(VideoContentProviderContract.VideoColumns.COLUMN_DESCRIPTION, it.description)
                        put(VideoContentProviderContract.VideoColumns.COLUMN_CACHED_THUMBNAIL_PATH, it.cachedThumbnailPath)
                        put(VideoContentProviderContract.VideoColumns.COLUMN_PUBLISHED_AT, it.publishedAt)
                    }
                }.toTypedArray()
                // The provider default sort is by published_at desc, so order of bulk insert is not paramount for final display
                val insertedRows = contentResolver.bulkInsert(VideoContentProviderContract.CONTENT_URI_VIDEOS, contentValuesArray)
                Log.d(TAG, "Worker: Bulk inserted $insertedRows rows into ContentProvider.")
            }

            // Widget notification code removed
            // val appWidgetManager = AppWidgetManager.getInstance(appContext)
            // val componentName = ComponentName(appContext, VideoWidgetProvider::class.java)
            // val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            // if (appWidgetIds.isNotEmpty()) {
            // appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.video_widget_list_view)
            // Log.d(TAG, "Worker: Notified ${appWidgetIds.size} widgets to update views.")
            // }
        } catch (e: Exception) {
            Log.e(TAG, "Worker: Error updating ContentProvider: ${e.message}", e)
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting Youtube sync work")
        val youtubeRepository = YoutubeRepository(YoutubeApiService.create())
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notifiedVideoIds = prefs.getStringSet(KEY_NOTIFIED_VIDEO_IDS, emptySet()) ?: emptySet()

        return withContext(Dispatchers.IO) {
            try {
                val response = youtubeRepository.getChannelVideos(
                    part = "snippet,id",
                    channelId = BuildConfig.YOUTUBE_CHANNEL_ID,
                    apiKey = BuildConfig.YOUTUBE_API_KEY,
                    maxResults = 20, // Fetch more for CP, notifications will still check against prefs
                    type = "video",
                    order = "date"
                )

                if (response.isSuccessful) {
                    val youtubeData: YoutubeVideoListResponse? = response.body()
                    val videoApiItems: List<YoutubeVideoItem> = youtubeData?.items ?: emptyList()

                    if (videoApiItems.isNotEmpty()) {
                        Log.d(TAG, "Worker: Successfully fetched ${videoApiItems.size} videos from API.")
                        val newNotifiedIds = notifiedVideoIds.toMutableSet()
                        var newVideosFoundCount = 0
                        val processedCachedVideoInfos = mutableListOf<CachedVideoInfo>()

                        // Process for notifications (oldest of the new first)
                        // And build up list for ContentProvider
                        for (apiItem in videoApiItems.reversed()) { 
                            val videoId = apiItem.id?.videoId 
                            val videoTitle = apiItem.snippet?.title
                            if (videoId != null && videoTitle != null) {
                                val cachedThumbnailPath = cacheThumbnail(videoId, apiItem.snippet?.thumbnails?.high?.url)
                                processedCachedVideoInfos.add(
                                    CachedVideoInfo(
                                        id = videoId,
                                        title = videoTitle,
                                        description = apiItem.snippet?.description ?: "",
                                        cachedThumbnailPath = cachedThumbnailPath,
                                        publishedAt = apiItem.snippet?.publishedAt
                                    )
                                )
                                if (!notifiedVideoIds.contains(videoId)) {
                                    NotificationUtils.showNewVideoNotification(appContext, videoTitle, videoId, cachedThumbnailPath)
                                    newNotifiedIds.add(videoId) 
                                    newVideosFoundCount++
                                }
                            }
                        }
                        
                        // Update ContentProvider with all fetched (and processed) videos, typically newest first or rely on CP sort order.
                        // processedCachedVideoInfos is currently oldest first, reversing for CP update.
                        updateVideoDataInContentProvider(processedCachedVideoInfos.reversed()) 

                        if (newVideosFoundCount > 0) {
                            prefs.edit().putStringSet(KEY_NOTIFIED_VIDEO_IDS, newNotifiedIds).apply()
                        }
                        Result.success()
                    } else {
                        Log.w(TAG, "Worker: API call successful but no video items returned.")
                        updateVideoDataInContentProvider(emptyList()) // Clear CP if API returns empty
                        Result.success()
                    }
                } else {
                    Log.e(TAG, "Worker: API call failed: ${response.code()}")
                    Result.failure()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Worker: Exception during sync: ${e.message}", e)
                if (e is IOException || e is HttpException) Result.retry() else Result.failure()
            }
        }
    }
}
