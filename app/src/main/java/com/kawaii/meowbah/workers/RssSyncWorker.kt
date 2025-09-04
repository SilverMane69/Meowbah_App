package com.kawaii.meowbah.workers

import android.content.Context
import android.util.Log
import android.util.Xml
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kawaii.meowbah.BuildConfig
import com.kawaii.meowbah.ui.screens.videos.LocalizedSnippet
import com.kawaii.meowbah.ui.screens.videos.Thumbnail
import com.kawaii.meowbah.ui.screens.videos.Thumbnails
import com.kawaii.meowbah.ui.screens.videos.VideoItem
import com.kawaii.meowbah.ui.screens.videos.VideoSnippet
import com.kawaii.meowbah.ui.screens.videos.VideoStatistics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class RssSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val TAG = "RssSyncWorker"
    private val channelId = BuildConfig.YOUTUBE_CHANNEL_ID

    override suspend fun doWork(): Result {
        if (channelId.isBlank()) {
            Log.e(TAG, "Channel ID is blank. Cannot fetch RSS. Worker failing.")
            return Result.failure()
        }

        Log.i(TAG, "Starting RSS Sync for channel ID: $channelId")

        return withContext(Dispatchers.IO) {
            val rssUrlString = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
            var connection: HttpURLConnection? = null
            try {
                Log.d(TAG, "Connecting to $rssUrlString on ${Thread.currentThread().name}")
                val url = URL(rssUrlString)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val parser = Xml.newPullParser()
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    parser.setInput(InputStreamReader(inputStream, "UTF-8"))

                    val videoList = mutableListOf<VideoItem>()
                    var eventType = parser.eventType
                    var currentVideoId: String? = null
                    var currentTitle: String? = null
                    var currentDescription: String? = null
                    var currentThumbnailUrl: String? = null
                    var currentPublishedAt: String? = null
                    var inEntry = false
                    var inMediaGroup = false

                    while (eventType != XmlPullParser.END_DOCUMENT && videoList.size < 20) {
                        val tagName = parser.name
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (tagName.equals("entry", ignoreCase = true)) {
                                    inEntry = true
                                    currentVideoId = null
                                    currentTitle = null
                                    currentDescription = null
                                    currentThumbnailUrl = null
                                    currentPublishedAt = null
                                } else if (tagName.equals("media:group", ignoreCase = true)) {
                                    inMediaGroup = true
                                } else if (inEntry) {
                                    if (inMediaGroup) {
                                        when (tagName?.toLowerCase()) {
                                            "media:description" -> currentDescription = parser.nextText()
                                            "media:thumbnail" -> currentThumbnailUrl = parser.getAttributeValue(null, "url")
                                            // Add other media:group tags if needed, using parser.nextText() for text content
                                        }
                                    } else { // Not in media:group, directly under entry
                                        when (tagName?.toLowerCase()) {
                                            "yt:videoid" -> currentVideoId = parser.nextText()
                                            "title" -> currentTitle = parser.nextText()
                                            "published" -> currentPublishedAt = parser.nextText()
                                            // media:thumbnail should be handled within media:group based on your XML
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
                                                channelTitle = "Meowbah (from RSS Worker)",
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
                                        videoList.add(videoItem)
                                    }
                                    inEntry = false
                                } else if (tagName.equals("media:group", ignoreCase = true)) {
                                    inMediaGroup = false
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    Log.i(TAG, "Successfully parsed ${videoList.size} videos from RSS.")
                    Result.success()
                } else {
                    Log.e(TAG, "Failed to load videos from RSS. HTTP Response Code: ${connection.responseCode} for URL $rssUrlString")
                    Result.failure()
                }
            } catch (e: XmlPullParserException) {
                Log.e(TAG, "XML Parsing Error: ${e.message}", e)
                Result.failure()
            } catch (e: MalformedURLException) {
                Log.e(TAG, "Malformed URL: $rssUrlString", e)
                Result.failure()
            } catch (e: IOException) {
                Log.e(TAG, "Network IOException: ${e.message}", e)
                Result.failure()
            } catch (e: Exception) {
                Log.e(TAG, "General Exception during RSS sync: ${e.message}", e)
                Result.failure()
            } finally {
                connection?.disconnect()
            }
        }
    }

    companion object {
        const val WORK_NAME = "RssSyncWorker"
    }
}
