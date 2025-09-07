package com.kawaii.meowbah.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kawaii.meowbah.ui.activities.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException // Added this import
import java.net.URL
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
        private const val RSS_FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=UCNytjdD5-KZInxjVeWV_qQw" // CORRECTED Channel ID
        private const val NS_YT = "http://www.youtube.com/xml/schemas/2015"
        private const val NS_ATOM = "http://www.w3.org/2005/Atom" // Default namespace for title, published, entry
    }

    private data class RssVideoEntry(
        val videoId: String,
        val title: String,
        val publishedAtString: String
    )

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting RSS Youtube sync work")

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
                val rssVideoEntries = fetchAndParseRssFeed()

                if (rssVideoEntries.isEmpty() && runAttemptCount == 0) { 
                    Log.d(TAG, "No video entries found in RSS feed or feed was empty on first attempt.")
                }

                Log.d(TAG, "Processing ${rssVideoEntries.size} video entries from RSS.")
                val tempNotifiedIds = notifiedVideoIds.toMutableSet()
                var newVideosFoundThisRun = false
                var currentBatchOverallNewestVideoDate = lastKnownNewestVideoDate

                for (entry in rssVideoEntries) {
                    val videoId = entry.videoId
                    val videoTitle = entry.title
                    val videoPublishedAtString = entry.publishedAtString

                    val currentVideoDate: OffsetDateTime = try {
                        OffsetDateTime.parse(videoPublishedAtString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    } catch (e: DateTimeParseException) {
                        Log.e(TAG, "Error parsing RSS video publishedAt date: $videoPublishedAtString for video ID $videoId", e)
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

                    Log.i(TAG, "New video found via RSS: $videoTitle (ID: $videoId, Published: $currentVideoDate)")
                    NotificationUtils.showNewVideoNotification(
                        applicationContext,
                        videoTitle,
                        videoId,
                        null 
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
                    Log.d(TAG, "No new videos found to notify in this run based on RSS date and ID checks.")
                }
                Result.success()

            } catch (e: IOException) {
                Log.e(TAG, "IO exception during RSS sync (network issue)", e)
                Result.retry()
            } catch (e: XmlPullParserException) {
                Log.e(TAG, "XML parsing exception during RSS sync", e)
                 Result.failure() 
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during RSS sync", e)
                Result.failure()
            }
        }
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result.trim()
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    private fun fetchAndParseRssFeed(): List<RssVideoEntry> {
        val entries = mutableListOf<RssVideoEntry>()
        var connection: HttpURLConnection? = null
        var reader: InputStreamReader? = null

        try {
            val url = URL(RSS_FEED_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "RSS Fetch: Server returned HTTP $responseCode ${connection.responseMessage}")
                return entries 
            }

            reader = InputStreamReader(connection.inputStream, Charsets.UTF_8)
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(reader)

            parser.nextTag() 
            parser.require(XmlPullParser.START_TAG, NS_ATOM, "feed")

            var currentVideoId: String? = null
            var currentTitle: String? = null
            var currentPublishedAt: String? = null

            while (parser.next() != XmlPullParser.END_TAG || parser.name != "feed") { 
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                if (parser.namespace == NS_ATOM && parser.name == "entry") {
                    currentVideoId = null
                    currentTitle = null
                    currentPublishedAt = null

                    while (parser.next() != XmlPullParser.END_TAG || parser.name != "entry") {
                        if (parser.eventType != XmlPullParser.START_TAG) {
                            continue
                        }
                        when (parser.namespace) {
                            NS_ATOM -> {
                                when (parser.name) {
                                    "title" -> currentTitle = readText(parser)
                                    "published" -> currentPublishedAt = readText(parser)
                                    else -> skip(parser)
                                }
                            }
                            NS_YT -> {
                                when (parser.name) {
                                    "videoId" -> currentVideoId = readText(parser)
                                    else -> skip(parser)
                                }
                            }
                            else -> skip(parser)
                        }
                    } 
                    if (currentVideoId != null && currentTitle != null && currentPublishedAt != null) {
                        entries.add(RssVideoEntry(currentVideoId, currentTitle, currentPublishedAt))
                    } else {
                        Log.w(TAG, "Skipping RSS entry with missing fields: videoId=$currentVideoId, title=$currentTitle, publishedAt=$currentPublishedAt")
                    }
                     parser.require(XmlPullParser.END_TAG, NS_ATOM, "entry") 
                } else {
                    skip(parser) 
                }
            }
            parser.require(XmlPullParser.END_TAG, NS_ATOM, "feed")

        } catch (e: MalformedURLException) {
            Log.e(TAG, "Malformed RSS URL: $RSS_FEED_URL", e)
        } catch (e: IOException) {
            Log.e(TAG, "IOException during RSS fetch or parse", e)
            throw e 
        } catch (e: XmlPullParserException) {
            Log.e(TAG, "XmlPullParserException during RSS parse", e)
            throw e 
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in fetchAndParseRssFeed", e)
        } finally {
            try {
                reader?.close()
            } catch (ioe: IOException) {
                Log.e(TAG, "Error closing InputStreamReader", ioe)
            }
            connection?.disconnect()
        }
        Log.d(TAG, "fetchAndParseRssFeed finished. Found ${entries.size} entries.")
        return entries
    }
}
