package com.kawaii.meowbah.ui.screens.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kawaii.meowbah.BuildConfig
import com.kawaii.meowbah.data.remote.YoutubeApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import android.util.Xml // For RSS XML Parsing
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser // For RSS XML Parsing
import org.xmlpull.v1.XmlPullParserException // For RSS XML Parsing
import retrofit2.HttpException
import java.io.IOException
import java.io.InputStreamReader // For RSS XML Parsing
import java.net.HttpURLConnection // For RSS XML Parsing
import java.net.MalformedURLException // For RSS XML Parsing
import java.net.URL // For RSS XML Parsing
import retrofit2.Response
import kotlinx.coroutines.Dispatchers // ADDED for withContext
import kotlinx.coroutines.withContext   // ADDED for withContext

// VideoItem and related classes (VideoSnippet, LocalizedSnippet, Thumbnails, Thumbnail, VideoStatistics)
// are assumed to be defined in their own file (e.g., VideoItem.kt) and imported as needed.

// --- Placeholder Data Classes for API Interaction (as originally provided) ---
data class PlaceholderId(
    val kind: String?,
    val videoId: String?
)

data class PlaceholderSnippet(
    val title: String?,
    val channelTitle: String?,
    val description: String?,
    val thumbnails: PlaceholderThumbnails?,
    val publishedAt: String?
)

data class PlaceholderThumbnails(
    val default: PlaceholderThumbnail?,
    val medium: PlaceholderThumbnail?,
    val high: PlaceholderThumbnail?
)

data class PlaceholderThumbnail(
    val url: String?
)

data class PlaceholderContentDetails(
    val duration: String?
)

data class PlaceholderYoutubeVideoItem(
    val id: PlaceholderId?,
    val snippet: PlaceholderSnippet?
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

    private val apiKey = BuildConfig.YOUTUBE_API_KEY
    private val channelId = BuildConfig.YOUTUBE_CHANNEL_ID

    // RSS Fallback limiting
    private var rssFallbackAttemptCount = 0
    private val MAX_RSS_FALLBACK_ATTEMPTS = 3
    private val TAG = "VideosViewModel"

    private fun tryRssFallback() {
        viewModelScope.launch { // This launch uses Dispatchers.Main.immediate by default
            if (rssFallbackAttemptCount < MAX_RSS_FALLBACK_ATTEMPTS) {
                rssFallbackAttemptCount++
                Log.d(TAG, "tryRssFallback: Attempting RSS. Attempt #$rssFallbackAttemptCount/$MAX_RSS_FALLBACK_ATTEMPTS for channel $channelId")
                _isLoading.value = true 
                _error.value = null
                fetchVideosFromRss() // fetchVideosFromRss is suspend and will switch context internally
            } else {
                Log.w(TAG, "tryRssFallback: Max RSS fallback attempts ($MAX_RSS_FALLBACK_ATTEMPTS) reached. Not attempting further RSS.")
                _error.value = "Failed to load videos after multiple attempts. Please try again later."
                _videos.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchVideosFromRss() {
        if (channelId.isBlank()) {
            Log.e(TAG, "fetchVideosFromRss: Channel ID is blank. Cannot fetch RSS.")
            _error.value = "Channel ID not configured for RSS fallback."
            _videos.value = emptyList()
            _isLoading.value = false
            return
        }

        // Perform network operations on Dispatchers.IO
        val result: Result<List<VideoItem>> = withContext(Dispatchers.IO) {
            val rssUrlString = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
            var connection: HttpURLConnection? = null
            try {
                Log.d(TAG, "fetchVideosFromRss (on ${Thread.currentThread().name}): Connecting to $rssUrlString")
                val url = URL(rssUrlString)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val parser = Xml.newPullParser()
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false) // Important for media:group etc.
                    parser.setInput(InputStreamReader(inputStream, "UTF-8"))

                    val tempVideoList = mutableListOf<VideoItem>()
                    var eventType = parser.eventType
                    var currentVideoId: String? = null
                    var currentTitle: String? = null
                    var currentDescription: String? = null // ADDED for description
                    var currentThumbnailUrl: String? = null
                    var currentPublishedAt: String? = null 
                    var inEntry = false
                    var inMediaGroup = false // To handle elements within <media:group>

                    while (eventType != XmlPullParser.END_DOCUMENT && tempVideoList.size < 20) { 
                        val tagName = parser.name
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (tagName.equals("entry", ignoreCase = true)) {
                                    inEntry = true
                                    currentVideoId = null
                                    currentTitle = null
                                    currentDescription = null // Reset for new entry
                                    currentThumbnailUrl = null
                                    currentPublishedAt = null
                                } else if (tagName.equals("media:group", ignoreCase = true)) {
                                    inMediaGroup = true
                                } else if (inEntry) {
                                    if (inMediaGroup) {
                                        when {
                                            tagName.equals("media:description", ignoreCase = true) && parser.next() == XmlPullParser.TEXT -> currentDescription = parser.text
                                            tagName.equals("media:thumbnail", ignoreCase = true) -> currentThumbnailUrl = parser.getAttributeValue(null, "url")
                                            // Other media:group elements if needed
                                        }
                                    } else {
                                        when {
                                            tagName.equals("yt:videoId", ignoreCase = true) && parser.next() == XmlPullParser.TEXT -> currentVideoId = parser.text
                                            tagName.equals("title", ignoreCase = true) && parser.next() == XmlPullParser.TEXT -> currentTitle = parser.text
                                            tagName.equals("published", ignoreCase = true) && parser.next() == XmlPullParser.TEXT -> currentPublishedAt = parser.text
                                            // Note: media:thumbnail might be outside media:group in some feeds, adjust if necessary
                                            // If thumbnail is reliably inside media:group, this can be removed.
                                            tagName.equals("media:thumbnail", ignoreCase = true) -> currentThumbnailUrl = parser.getAttributeValue(null, "url") 
                                        }
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (tagName.equals("entry", ignoreCase = true)) {
                                    if (currentVideoId != null && currentTitle != null) {
                                        val videoItem = VideoItem(
                                            id = currentVideoId!!,
                                            snippet = VideoSnippet(
                                                title = currentTitle!!,
                                                channelTitle = "Meowbah (from RSS)", 
                                                localized = LocalizedSnippet(
                                                    title = currentTitle!!,
                                                    description = currentDescription ?: "No description available from RSS."
                                                ),
                                                thumbnails = Thumbnails(
                                                    default = Thumbnail(url = currentThumbnailUrl ?: "", width = 120, height = 90),
                                                    medium = Thumbnail(url = currentThumbnailUrl ?: "", width = 320, height = 180),
                                                    high = Thumbnail(url = currentThumbnailUrl ?: "", width = 480, height = 360)
                                                ),
                                                publishedAt = currentPublishedAt
                                            ),
                                            statistics = VideoStatistics(
                                                viewCount = null, 
                                                likeCount = null  
                                            )
                                        )
                                        tempVideoList.add(videoItem)
                                    }
                                    inEntry = false
                                } else if (tagName.equals("media:group", ignoreCase = true)) {
                                    inMediaGroup = false
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    Log.d(TAG, "fetchVideosFromRss (on ${Thread.currentThread().name}): Successfully parsed ${tempVideoList.size} videos.")
                    Result.success(tempVideoList)
                } else {
                    val errorMsg = "Failed to load videos from RSS (HTTP ${connection.responseCode})."
                    Log.e(TAG, "fetchVideosFromRss (on ${Thread.currentThread().name}): HTTP Error ${connection.responseCode} for URL $rssUrlString")
                    Result.failure(IOException(errorMsg))
                }
            } catch (e: XmlPullParserException) {
                Log.e(TAG, "fetchVideosFromRss (on ${Thread.currentThread().name}): XML Parsing Error: ${e.message}", e)
                Result.failure(IOException("Error parsing RSS feed.", e))
            } catch (e: MalformedURLException) {
                Log.e(TAG, "fetchVideosFromRss (on ${Thread.currentThread().name}): Malformed URL: $rssUrlString", e)
                Result.failure(IOException("Invalid RSS feed URL.", e))
            } catch (e: IOException) { 
                Log.e(TAG, "fetchVideosFromRss (on ${Thread.currentThread().name}): Network IOException: ${e.message}", e)
                Result.failure(e)
            } catch (e: Exception) { 
                Log.e(TAG, "fetchVideosFromRss (on ${Thread.currentThread().name}): General Exception: ${e.message}", e)
                Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }

        result.fold(
            onSuccess = { videoList ->
                Log.d(TAG, "fetchVideosFromRss (onSuccess): Parsed ${videoList.size} items. First item details (if any): ID: ${videoList.firstOrNull()?.id}, Desc: ${videoList.firstOrNull()?.snippet?.localized?.description}, PubAt: ${videoList.firstOrNull()?.snippet?.publishedAt}")
                _videos.value = videoList
                _error.value = null
                 Log.d(TAG, "fetchVideosFromRss (main thread update): Successfully updated UI with ${videoList.size} videos from RSS.")
            },
            onFailure = { exception ->
                _videos.value = emptyList()
                _error.value = exception.message ?: "An unexpected error occurred fetching RSS."
                Log.e(TAG, "fetchVideosFromRss (main thread update): Failed to update UI from RSS.", exception)
            }
        )
        _isLoading.value = false
    }


    fun fetchVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val requestDescription = "youtubeApiService.getChannelVideos(channelId=$channelId)"
            Log.d(TAG, "fetchVideos: Attempting API call for $requestDescription. RSS attempts so far: $rssFallbackAttemptCount")

            if (channelId.isBlank()) {
                Log.e(TAG, "fetchVideos: YouTube Channel ID is blank. Cannot fetch from API.")
                tryRssFallback()
                return@launch
            }

            try {
                val apiResponse: Response<PlaceholderYoutubeVideoListResponse> = youtubeApiService.getChannelVideos(
                    part = "snippet,id",
                    channelId = channelId,
                    apiKey = apiKey,
                    maxResults = 20,
                    type = "video",
                    order = "date"
                )

                if (apiResponse.isSuccessful) {
                    val placeholderData = apiResponse.body()
                    if (placeholderData != null && placeholderData.items.isNotEmpty()) {
                        _videos.value = placeholderData.items.mapNotNull { apiItem ->
                            val videoId = apiItem.id?.videoId
                            if (videoId.isNullOrBlank()) {
                                Log.w(TAG, "Skipping item with null or blank videoId: $apiItem")
                                return@mapNotNull null
                            }
                            VideoItem(
                                id = videoId,
                                snippet = VideoSnippet(
                                    title = apiItem.snippet?.title ?: "No Title",
                                    channelTitle = apiItem.snippet?.channelTitle ?: "No Channel",
                                    localized = LocalizedSnippet(
                                        title = apiItem.snippet?.title ?: "No Title",
                                        description = apiItem.snippet?.description ?: "No Description"
                                    ),
                                    thumbnails = Thumbnails(
                                        default = Thumbnail(url = apiItem.snippet?.thumbnails?.default?.url ?: "", width = 120, height = 90),
                                        medium = Thumbnail(
                                            url = apiItem.snippet?.thumbnails?.medium?.url ?: "",
                                            width = 320,
                                            height = 180
                                        ),
                                        high = Thumbnail(url = apiItem.snippet?.thumbnails?.high?.url ?: "", width = 480, height = 360)
                                    ),
                                    publishedAt = apiItem.snippet?.publishedAt
                                ),
                                statistics = VideoStatistics(
                                    viewCount = null, 
                                    likeCount = null  
                                )
                            )
                        }
                        rssFallbackAttemptCount = 0 
                        Log.d(TAG, "Successfully fetched and mapped videos from API. Count: ${_videos.value.size}")
                        _isLoading.value = false
                    } else {
                        Log.w(TAG, "API call successful but response body is null or empty. Attempting RSS fallback.")
                        tryRssFallback()
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
                        Log.e(TAG, "Failed to parse error body as JSON: $errorBody", jsonE)
                        detailedMessage = "API Error (HTTP ${apiResponse.code()}). Raw: $errorBody"
                    }
                    Log.e(TAG, "API call failed. Code: ${apiResponse.code()}, Error: $detailedMessage")
                    tryRssFallback()
                }

            } catch (e: HttpException) {
                Log.e(TAG, "HttpException: ${e.message}", e)
                tryRssFallback()
            } catch (e: IOException) {
                Log.e(TAG, "Network Error: ${e.message}", e)
                tryRssFallback()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}", e)
                tryRssFallback()
            } 
        }
    }
}
