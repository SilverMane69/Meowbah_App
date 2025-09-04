package com.kawaii.meowbah.ui.screens.videos

import android.app.Application
// import android.appwidget.AppWidgetManager // Removed for widget removal
// import android.content.ComponentName // Removed for widget removal
import android.content.ContentValues // Added for ContentProvider
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kawaii.meowbah.BuildConfig
// import com.kawaii.meowbah.R // Removed as R.id.video_widget_list_view is no longer used here
import com.kawaii.meowbah.data.remote.YoutubeApiService
import com.kawaii.meowbah.data.remote.YoutubeVideoListResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import android.util.Xml 
import com.kawaii.meowbah.data.CachedVideoInfo
import com.kawaii.meowbah.data.VideoContentProviderContract // Added for ContentProvider
// import com.kawaii.meowbah.widgets.VideoWidgetProvider // Removed for widget removal
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser 
import org.xmlpull.v1.XmlPullParserException 
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader 
import java.net.HttpURLConnection 
import java.net.MalformedURLException 
import java.net.URL 
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideosViewModel(application: Application) : AndroidViewModel(application) {

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
    private val channelId = BuildConfig.YOUTUBE_CHANNEL_ID

    private var rssFallbackAttemptCount = 0
    private val MAX_RSS_FALLBACK_ATTEMPTS = 3
    private val TAG = "VideosViewModel"

    private suspend fun cacheThumbnail(videoId: String, thumbnailUrl: String?): String? = withContext(Dispatchers.IO) {
        if (thumbnailUrl.isNullOrEmpty()) {
            Log.d(TAG, "cacheThumbnail: Thumbnail URL is null or empty for video ID $videoId.")
            return@withContext null
        }
        val context = getApplication<Application>()
        val thumbnailsDir = File(context.cacheDir, "thumbnails")
        if (!thumbnailsDir.exists()) {
            thumbnailsDir.mkdirs()
        }
        val fileName = "$videoId.jpg"
        val cachedFile = File(thumbnailsDir, fileName)
        var fos: FileOutputStream? = null
        var connection: HttpURLConnection? = null
        var inputStream: java.io.InputStream? = null
        try {
            val url = URL(thumbnailUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 7000
            connection.readTimeout = 15000
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
            Log.e(TAG, "cacheThumbnail: Exception for $videoId, URL $thumbnailUrl: ${e.message}", e)
        } finally {
            fos?.close()
            inputStream?.close()
            connection?.disconnect()
        }
        return@withContext null 
    }

    private suspend fun updateVideoDataInContentProvider(videoList: List<CachedVideoInfo>) = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val contentResolver = context.contentResolver

        Log.d(TAG, "Updating ContentProvider. Deleting old data...")
        try {
            val deletedRows = contentResolver.delete(VideoContentProviderContract.CONTENT_URI_VIDEOS, null, null)
            Log.d(TAG, "Deleted $deletedRows rows from ContentProvider.")

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

                val insertedRows = contentResolver.bulkInsert(VideoContentProviderContract.CONTENT_URI_VIDEOS, contentValuesArray)
                Log.d(TAG, "Bulk inserted $insertedRows rows into ContentProvider.")
            } else {
                Log.d(TAG, "Video list is empty, nothing to insert into ContentProvider after delete.")
            }

            // Widget notification code removed

        } catch (e: Exception) {
            Log.e(TAG, "Error updating ContentProvider: ${e.message}", e)
        }
    }

    private fun tryRssFallback() {
        viewModelScope.launch { 
            if (rssFallbackAttemptCount < MAX_RSS_FALLBACK_ATTEMPTS) {
                rssFallbackAttemptCount++
                _isLoading.value = true 
                _error.value = null
                fetchVideosFromRss()
            } else {
                _error.value = "Failed to load videos after multiple attempts."
                _videos.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchVideosFromRss() {
        if (channelId.isBlank()) {
            _error.value = "Channel ID not configured for RSS fallback."
            _videos.value = emptyList(); _isLoading.value = false; return
        }
        val result: Result<List<CachedVideoInfo>> = withContext(Dispatchers.IO) {
            val rssUrlString = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
            var connection: HttpURLConnection? = null
            val tempCachedVideoList = mutableListOf<CachedVideoInfo>()
            try {
                val url = URL(rssUrlString)
                connection = url.openConnection() as HttpURLConnection
                // ... (Rest of RSS parsing as before) ...
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val parser = Xml.newPullParser()
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    parser.setInput(InputStreamReader(inputStream, "UTF-8"))
                    var eventType = parser.eventType
                    var currentVideoId: String? = null; var currentTitle: String? = null; var currentDescription: String? = null
                    var currentThumbnailUrl: String? = null; var currentPublishedAt: String? = null 
                    var inEntry = false; var inMediaGroup = false
                    while (eventType != XmlPullParser.END_DOCUMENT && tempCachedVideoList.size < 20) { 
                        val tagName = parser.name
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (tagName.equals("entry", ignoreCase = true)) { inEntry = true; currentVideoId = null; currentTitle = null; currentDescription = null; currentThumbnailUrl = null; currentPublishedAt = null }
                                else if (tagName.equals("media:group", ignoreCase = true)) { inMediaGroup = true }
                                else if (inEntry) {
                                    if (inMediaGroup) {
                                        if (tagName.equals("media:description", ignoreCase = true) && parser.next() == XmlPullParser.TEXT) currentDescription = parser.text
                                        if (tagName.equals("media:thumbnail", ignoreCase = true)) currentThumbnailUrl = parser.getAttributeValue(null, "url")
                                    } else {
                                        if (tagName.equals("yt:videoId", ignoreCase = true) && parser.next() == XmlPullParser.TEXT) currentVideoId = parser.text
                                        if (tagName.equals("title", ignoreCase = true) && parser.next() == XmlPullParser.TEXT) currentTitle = parser.text
                                        if (tagName.equals("published", ignoreCase = true) && parser.next() == XmlPullParser.TEXT) currentPublishedAt = parser.text
                                        if (tagName.equals("media:thumbnail", ignoreCase = true) && !inMediaGroup) currentThumbnailUrl = parser.getAttributeValue(null, "url")
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (tagName.equals("entry", ignoreCase = true)) {
                                    if (currentVideoId != null && currentTitle != null) {
                                        val cachedPath = cacheThumbnail(currentVideoId!!, currentThumbnailUrl)
                                        tempCachedVideoList.add(CachedVideoInfo(currentVideoId!!, currentTitle!!, currentDescription ?: "", cachedPath, currentPublishedAt))
                                    }
                                    inEntry = false
                                } else if (tagName.equals("media:group", ignoreCase = true)) { inMediaGroup = false }
                            }
                        }
                        eventType = parser.next()
                    }
                    Result.success(tempCachedVideoList)
                } else {
                    Result.failure(IOException("Failed RSS (HTTP ${connection.responseCode})"))
                }
            } catch (e: Exception) { Result.failure(e) } 
            finally { connection?.disconnect() }
        }

        result.fold(
            onSuccess = { cachedVideoList ->
                _videos.value = cachedVideoList
                _error.value = null
                viewModelScope.launch { updateVideoDataInContentProvider(cachedVideoList) } // Update CP
                Log.d(TAG, "RSS success: ${cachedVideoList.size} videos.")
            },
            onFailure = { exception ->
                _videos.value = emptyList()
                _error.value = exception.message ?: "Error fetching RSS."
                Log.e(TAG, "RSS failed.", exception)
            }
        )
        _isLoading.value = false
    }

    fun fetchVideos() {
        viewModelScope.launch {
            _isLoading.value = true; _error.value = null
            if (channelId.isBlank()) { tryRssFallback(); return@launch }
            try {
                val apiResponse: Response<YoutubeVideoListResponse> = youtubeApiService.getChannelVideos(
                    part = "snippet,id", channelId = channelId, apiKey = apiKey, maxResults = 20, type = "video", order = "date"
                )
                if (apiResponse.isSuccessful) {
                    val youtubeData = apiResponse.body()
                    if (youtubeData != null && youtubeData.items.isNotEmpty()) {
                        val cachedVideoInfoList = mutableListOf<CachedVideoInfo>()
                        for (apiItem in youtubeData.items) {
                            val videoId = apiItem.id?.videoId
                            if (videoId.isNullOrBlank()) continue
                            val thumbnailUrl = apiItem.snippet?.thumbnails?.high?.url 
                                ?: apiItem.snippet?.thumbnails?.medium?.url 
                                ?: apiItem.snippet?.thumbnails?.default?.url
                            val cachedPath = cacheThumbnail(videoId, thumbnailUrl)
                            cachedVideoInfoList.add(CachedVideoInfo(videoId, apiItem.snippet?.title ?: "", apiItem.snippet?.description ?: "", cachedPath, apiItem.snippet?.publishedAt))
                        }
                        _videos.value = cachedVideoInfoList
                        updateVideoDataInContentProvider(cachedVideoInfoList) // Update CP
                        _error.value = null; rssFallbackAttemptCount = 0 
                    } else { tryRssFallback() }
                } else { tryRssFallback() }
            } catch (e: Exception) { tryRssFallback() } 
            finally { _isLoading.value = false }
        }
    }
}
