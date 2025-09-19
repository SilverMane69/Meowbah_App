package com.kawaii.meowbah.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kawaii.meowbah.data.db.AppDatabase
import com.kawaii.meowbah.data.model.RssPost
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
// Removed Pattern import as it's no longer used

// This data class is used by the RSS parsing logic
data class RssPostEntry(
    val guid: String,
    val title: String,
    val link: String,
    val pubDate: String, // Expected format: RFC_1123_DATE_TIME (e.g., "Sat, 20 Jul 2024 10:00:00 GMT")
    val description: String,
    val imageUrl: String? // Will always be null now
)

class PostsSyncWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "PostsSyncWorker"
        private const val TAG = "PostsSyncWorker"
        private const val RSS_FEED_URL = "https://nitter.privacyredirect.com/meowbahv/rss"
        private const val POSTS_WORKER_PREFS_NAME = "PostsSyncPrefs"
        private const val KEY_PROCESSED_POST_GUIDS = "processedPostGuids"
        private const val KEY_LAST_PROCESSED_NEWEST_PUB_DATE = "lastProcessedNewestPostPubDate"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting RSS Posts sync work")

        val sharedPrefs = appContext.getSharedPreferences(POSTS_WORKER_PREFS_NAME, Context.MODE_PRIVATE)
        val processedGuids = sharedPrefs.getStringSet(KEY_PROCESSED_POST_GUIDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        val lastProcessedNewestPubDateStr = sharedPrefs.getString(KEY_LAST_PROCESSED_NEWEST_PUB_DATE, null)

        val rssPostDao = AppDatabase.getInstance(appContext).rssPostDao()

        var lastStoredNewestPubDate: OffsetDateTime? = null
        if (lastProcessedNewestPubDateStr != null) {
            try {
                lastStoredNewestPubDate = OffsetDateTime.parse(lastProcessedNewestPubDateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            } catch (e: DateTimeParseException) {
                Log.e(TAG, "Error parsing stored lastProcessedNewestPostPubDate: $lastProcessedNewestPubDateStr", e)
            }
        }

        val allFetchedPosts = fetchAndParseRssFeed()
        if (allFetchedPosts.isEmpty() && xmlFetchErrorOccurred) {
            Log.d(TAG, "Failed to fetch or parse RSS feed. Retrying later.")
            return Result.retry()
        }
        if (allFetchedPosts.isEmpty()){
            Log.d(TAG, "No posts fetched from RSS feed.")
            return Result.success()
        }

        val newPostsToProcess = mutableListOf<RssPostEntry>()
        var currentBatchNewestPubDate: OffsetDateTime? = lastStoredNewestPubDate

        for (postEntry in allFetchedPosts) {
            if (processedGuids.contains(postEntry.guid)) {
                Log.d(TAG, "Skipping already processed post (GUID from SharedPreferences): ${postEntry.guid}")
                continue
            }

            val postPubDate: OffsetDateTime?
            try {
                postPubDate = OffsetDateTime.parse(postEntry.pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
            } catch (e: DateTimeParseException) {
                Log.e(TAG, "Error parsing post pubDate: ${postEntry.pubDate} for post GUID: ${postEntry.guid}", e)
                continue
            }

            if (lastStoredNewestPubDate != null && !postPubDate.isAfter(lastStoredNewestPubDate)) {
                Log.d(TAG, "Skipping post ${postEntry.guid} as it is not newer than last batch newest date ${lastStoredNewestPubDate}")
                continue
            }
            
            if (rssPostDao.getPostByGuid(postEntry.guid) != null) {
                Log.d(TAG, "Skipping already processed post (GUID from DB): ${postEntry.guid}")
                processedGuids.add(postEntry.guid) 
                continue
            }

            newPostsToProcess.add(postEntry)
            if (currentBatchNewestPubDate == null || postPubDate.isAfter(currentBatchNewestPubDate)) {
                currentBatchNewestPubDate = postPubDate
            }
        }

        return if (newPostsToProcess.isNotEmpty()) {
            Log.i(TAG, "Found ${newPostsToProcess.size} new posts to process.")
            val editor = sharedPrefs.edit()
            newPostsToProcess.forEach { postEntry ->
                try {
                    val postPubDateTime = OffsetDateTime.parse(postEntry.pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
                    val dbPost = RssPost(
                        guid = postEntry.guid,
                        title = postEntry.title,
                        link = postEntry.link,
                        pubDateEpochSeconds = postPubDateTime.toEpochSecond(),
                        description = postEntry.description,
                        imageUrl = postEntry.imageUrl // This will be null from RssPostEntry
                    )
                    rssPostDao.insertPost(dbPost)
                    Log.d(TAG, "Inserted new post to DB - GUID: ${dbPost.guid}, Title: ${dbPost.title}, ImageURL: ${dbPost.imageUrl}")
                    processedGuids.add(postEntry.guid) 
                } catch (e: DateTimeParseException) {
                    Log.e(TAG, "Error parsing pubDate again before DB insert for post GUID: ${postEntry.guid}", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting post GUID: ${postEntry.guid} into DB", e)
                }
            }
            editor.putStringSet(KEY_PROCESSED_POST_GUIDS, processedGuids)
            if (currentBatchNewestPubDate != null) {
                editor.putString(KEY_LAST_PROCESSED_NEWEST_PUB_DATE, currentBatchNewestPubDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            }
            editor.apply()
            Log.i(TAG, "Successfully processed and attempted to save ${newPostsToProcess.size} new posts. Updated SharedPreferences.")
            Result.success()
        } else {
            Log.i(TAG, "No new posts found to process.")
            Result.success()
        }
    }

    private var xmlFetchErrorOccurred = false

    private fun fetchAndParseRssFeed(): List<RssPostEntry> {
        xmlFetchErrorOccurred = false
        val entries = mutableListOf<RssPostEntry>()
        var connection: HttpURLConnection? = null
        var xmlContent: String? = null

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
                xmlFetchErrorOccurred = true
                return entries
            }
            xmlContent = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Malformed RSS URL: $RSS_FEED_URL", e)
            xmlFetchErrorOccurred = true
            return entries
        } catch (e: IOException) {
            Log.e(TAG, "IOException during RSS fetch", e)
            xmlFetchErrorOccurred = true
            return entries
        } finally {
            connection?.disconnect()
        }

        if (xmlContent == null) {
            Log.d(TAG, "xmlContent is null, cannot parse.")
            xmlFetchErrorOccurred = true
            return entries
        }

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var currentGuid: String? = null
            var currentTitle: String? = null
            var currentLink: String? = null
            var currentPubDate: String? = null
            var currentDescription: String? = null
            // currentImageUrl variable is removed
            var inItem = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            // Log.d(TAG, "Entered <item> tag") // Logging can be reduced now
                            inItem = true
                            currentGuid = null
                            currentTitle = null
                            currentLink = null
                            currentPubDate = null
                            currentDescription = null
                        } else if (inItem) {
                            // Log.d(TAG, "Processing tag inside <item>: $tagName")
                            when {
                                tagName.equals("title", ignoreCase = true) -> currentTitle = readText(parser)
                                tagName.equals("link", ignoreCase = true) -> currentLink = readText(parser)
                                tagName.equals("pubDate", ignoreCase = true) -> currentPubDate = readText(parser)
                                tagName.equals("guid", ignoreCase = true) -> currentGuid = readText(parser)
                                tagName.equals("description", ignoreCase = true) -> {
                                    // val guidForDescLog = currentGuid ?: "unknown GUID"
                                    // Log.d(TAG, "Entered <description> tag for GUID: $guidForDescLog")
                                    val descHtml = readText(parser)
                                    currentDescription = descHtml
                                    // Log.d(TAG, "Description HTML for $guidForDescLog: $descHtml")
                                    // Image extraction logic (regex, matcher.find()) is entirely removed.
                                }
                                else -> skip(parser)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", ignoreCase = true) && inItem) {
                            if (currentGuid != null && currentTitle != null && currentLink != null && currentPubDate != null && currentDescription != null) {
                                // imageUrl is explicitly passed as null here
                                entries.add(RssPostEntry(currentGuid, currentTitle, currentLink, currentPubDate, currentDescription, null))
                            } else {
                                Log.w(TAG, "Skipping RSS item with missing fields: guid=$currentGuid, title=$currentTitle, link=$currentLink, pubDate=$currentPubDate")
                            }
                            inItem = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            Log.e(TAG, "XmlPullParserException during RSS parse", e)
            xmlFetchErrorOccurred = true
        } catch (e: IOException) {
            Log.e(TAG, "IOException during RSS parse", e)
            xmlFetchErrorOccurred = true
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in parsing RSS feed", e)
            xmlFetchErrorOccurred = true
        }
        Log.d(TAG, "fetchAndParseRssFeed finished. Found ${entries.size} entries. Fetch error: $xmlFetchErrorOccurred")
        return entries
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text ?: ""
            parser.nextTag()
        }
        return result.trim()
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException("Parser not at START_TAG in skip()")
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
} // End of PostsSyncWorker class
