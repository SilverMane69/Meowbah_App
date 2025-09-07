package com.kawaii.meowbah.ui.screens.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kawaii.meowbah.data.CachedVideoInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class VideosViewModel : ViewModel() {

    private val _videos = MutableStateFlow<List<CachedVideoInfo>>(emptyList())
    val videos: StateFlow<List<CachedVideoInfo>> = _videos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    companion object {
        private const val TAG = "VideosViewModel"
        private const val RSS_FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=UCNytjdD5-KZInxjVeWV_qQw" // Meowbah's Channel ID - CORRECTED
        private const val NS_ATOM = "http://www.w3.org/2005/Atom"
        private const val NS_YT = "http://www.youtube.com/xml/schemas/2015"
        private const val NS_MEDIA = "http://search.yahoo.com/mrss/"
    }

    private data class RssFeedVideoItem(
        val id: String,
        val title: String,
        val publishedAt: String?,
        val description: String?,
        val thumbnailUrl: String?
    )

    fun fetchVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val feedItems = fetchAndParseRssFeed()
                _videos.value = feedItems.map {
                    CachedVideoInfo(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        thumbnailUrl = it.thumbnailUrl,
                        publishedAt = it.publishedAt
                    )
                }
                if (feedItems.isEmpty()) {
                    Log.d(TAG, "No videos found from RSS feed or feed was empty.")
                    // Optionally set an error or a specific state for empty list
                }
            } catch (e: IOException) {
                Log.e(TAG, "IOException fetching or parsing RSS feed", e)
                _error.value = "Network error. Please check your connection."
                _videos.value = emptyList()
            } catch (e: XmlPullParserException) {
                Log.e(TAG, "XmlPullParserException parsing RSS feed", e)
                _error.value = "Error parsing video feed."
                _videos.value = emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error fetching videos from RSS", e)
                _error.value = "Failed to load videos due to an unexpected error."
                _videos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchAndParseRssFeed(): List<RssFeedVideoItem> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<RssFeedVideoItem>()
        var connection: HttpURLConnection? = null
        var reader: InputStreamReader? = null

        try {
            val url = URL(RSS_FEED_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "RSS Fetch: Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                // Set error state if HTTP request fails
                // This part was missing proper error propagation to the UI for HTTP errors
                throw IOException("Failed to fetch RSS feed. Server responded with ${connection.responseCode}")
            }

            reader = InputStreamReader(connection.inputStream, Charsets.UTF_8)
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(reader)

            parser.nextTag() // Advance to the first tag, should be <feed>
            parser.require(XmlPullParser.START_TAG, NS_ATOM, "feed")

            var currentVideoId: String? = null
            var currentTitle: String? = null
            var currentPublishedAt: String? = null
            var currentDescription: String? = null
            var currentThumbnailUrl: String? = null

            while (parser.next() != XmlPullParser.END_TAG || parser.name != "feed") {
                if (parser.eventType != XmlPullParser.START_TAG) continue

                if (parser.namespace == NS_ATOM && parser.name == "entry") {
                    currentVideoId = null
                    currentTitle = null
                    currentPublishedAt = null
                    currentDescription = null
                    currentThumbnailUrl = null

                    while (parser.next() != XmlPullParser.END_TAG || parser.name != "entry") {
                        if (parser.eventType != XmlPullParser.START_TAG) continue
                        when (parser.namespace) {
                            NS_ATOM -> when (parser.name) {
                                "title" -> currentTitle = readText(parser)
                                "published" -> currentPublishedAt = readText(parser)
                                else -> skip(parser)
                            }
                            NS_YT -> when (parser.name) {
                                "videoId" -> currentVideoId = readText(parser)
                                else -> skip(parser)
                            }
                            NS_MEDIA -> when (parser.name) {
                                "group" -> {
                                    // Handle media:group to find description and thumbnail
                                    while (parser.next() != XmlPullParser.END_TAG || parser.name != "group") {
                                        if (parser.eventType != XmlPullParser.START_TAG) continue
                                        if (parser.namespace == NS_MEDIA) {
                                            when (parser.name) {
                                                "description" -> currentDescription = readText(parser)
                                                "thumbnail" -> {
                                                    // Prefer a specific thumbnail or just take the first one found
                                                    // For simplicity, taking the first one if not already set
                                                    if (currentThumbnailUrl == null) {
                                                         currentThumbnailUrl = parser.getAttributeValue(null, "url")
                                                    }
                                                    skip(parser) // Skip to end tag of thumbnail
                                                }
                                                else -> skip(parser)
                                            }
                                        } else {
                                            skip(parser)
                                        }
                                    }
                                }
                                else -> skip(parser)
                            }
                            else -> skip(parser)
                        }
                    } // End of entry processing
                    if (currentVideoId != null && currentTitle != null) {
                        entries.add(RssFeedVideoItem(currentVideoId, currentTitle, currentPublishedAt, currentDescription, currentThumbnailUrl))
                    } else {
                        Log.w(TAG, "Skipping RSS entry with missing ID or Title: videoId=$currentVideoId, title=$currentTitle")
                    }
                    parser.require(XmlPullParser.END_TAG, NS_ATOM, "entry")
                } else {
                    skip(parser)
                }
            }
            parser.require(XmlPullParser.END_TAG, NS_ATOM, "feed")

        } catch (e: MalformedURLException) {
            Log.e(TAG, "Malformed RSS URL: $RSS_FEED_URL", e)
            throw IOException("Malformed RSS URL", e) // Propagate as IOException for consistent handling
        } finally {
            reader?.close()
            connection?.disconnect()
        }
        Log.d(TAG, "fetchAndParseRssFeed finished. Found ${entries.size} entries.")
        entries
    }

    // Helper function to read text content of a tag
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag() // Consume the END_TAG of the current element
        }
        return result.trim()
    }

    // Helper function to skip unknown tags
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException("Parser not at START_TAG before skip()")
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
