package com.kawaii.meowbah.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.util.Xml
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.kawaii.meowbah.BuildConfig
import com.kawaii.meowbah.R
import com.kawaii.meowbah.data.WidgetCompatibleVideo
import com.kawaii.meowbah.data.remote.YoutubeApiService
import com.kawaii.meowbah.ui.theme.AvailableTheme
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class VideoWidgetViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    @Volatile
    private var videoList = listOf<WidgetCompatibleVideo>()
    private var currentThemeName: String? = null
    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private var rssFallbackAttemptCount = 0
    private val MAX_RSS_FALLBACK_ATTEMPTS = 3

    private val youtubeApiService: YoutubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YoutubeApiService::class.java)
    }
    private val apiKey = BuildConfig.YOUTUBE_API_KEY
    private val channelId = BuildConfig.YOUTUBE_CHANNEL_ID

    companion object {
        const val ACTION_VIEW_VIDEO = "com.kawaii.meowbah.ACTION_VIEW_VIDEO"
        const val EXTRA_VIDEO_ID = "com.kawaii.meowbah.EXTRA_VIDEO_ID"
        private const val TAG = "VideoWidgetFactory"
        private const val TARGET_THUMBNAIL_WIDTH_PX = 160 // Approx 80dp @ xhdpi
        private const val TARGET_THUMBNAIL_HEIGHT_PX = 120 // Approx 60dp @ xhdpi
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate for widget $appWidgetId. Initial videoList size: ${videoList.size}")
        loadTheme()
        fetchDataAsync()
    }

    override fun onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged for widget $appWidgetId. Current videoList size before fetch: ${videoList.size}")
        loadTheme()
        fetchDataAsync()
    }

    private fun loadTheme() {
        currentThemeName = VideoWidgetConfigureActivity.loadThemePref(context, appWidgetId)
    }

    private fun scaleBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        if (source.width <= maxWidth && source.height <= maxHeight) {
            return source // No scaling needed
        }
        val ratioX = maxWidth.toDouble() / source.width.toDouble()
        val ratioY = maxHeight.toDouble() / source.height.toDouble()
        val ratio = Math.min(ratioX, ratioY)
        val newWidth = (source.width * ratio).toInt()
        val newHeight = (source.height * ratio).toInt()
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    private fun downloadBitmap(urlString: String?): Bitmap? {
        if (urlString.isNullOrEmpty()) {
            Log.d(TAG, "downloadBitmap: URL is null or empty for widget $appWidgetId.")
            return null
        }
        Log.d(TAG, "downloadBitmap (widget $appWidgetId): Attempting to download from $urlString")
        var originalBitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 7000
            connection.readTimeout = 15000
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val input = connection.inputStream
                originalBitmap = BitmapFactory.decodeStream(input)
                input.close()
                if (originalBitmap != null) {
                    Log.d(TAG, "downloadBitmap (widget $appWidgetId): Original size ${originalBitmap!!.width}x${originalBitmap!!.height}")
                    scaledBitmap = scaleBitmap(originalBitmap!!, TARGET_THUMBNAIL_WIDTH_PX, TARGET_THUMBNAIL_HEIGHT_PX)
                    Log.d(TAG, "downloadBitmap (widget $appWidgetId): Scaled to ${scaledBitmap!!.width}x${scaledBitmap!!.height}")
                    if (originalBitmap != scaledBitmap) { // Only recycle if scaledBitmap is a new instance
                        originalBitmap?.recycle()
                    }
                    scaledBitmap
                } else {
                    Log.e(TAG, "downloadBitmap (widget $appWidgetId): Failed to decode bitmap from $urlString")
                    null
                }
            } else {
                Log.e(TAG, "downloadBitmap (widget $appWidgetId): HTTP error $responseCode for $urlString")
                null
            }
        } catch (e: MalformedURLException) {
            Log.e(TAG, "downloadBitmap (widget $appWidgetId): Malformed URL: $urlString", e)
            null
        } catch (e: IOException) {
            Log.e(TAG, "downloadBitmap (widget $appWidgetId): IOException for $urlString: ${e.message}", e)
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "downloadBitmap (widget $appWidgetId): SecurityException for $urlString: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "downloadBitmap (widget $appWidgetId): General Exception for $urlString: ${e.message}", e)
            originalBitmap?.recycle() // Clean up original if scaling failed or other exception
            null
        }
    }
    
    private fun tryRssFallback() {
        if (rssFallbackAttemptCount < MAX_RSS_FALLBACK_ATTEMPTS) {
            rssFallbackAttemptCount++
            Log.d(TAG, "tryRssFallback (widget $appWidgetId): Attempting RSS. Attempt #$rssFallbackAttemptCount/$MAX_RSS_FALLBACK_ATTEMPTS")
            fetchVideosFromRss()
        } else {
            Log.w(TAG, "tryRssFallback (widget $appWidgetId): Max RSS fallback attempts ($MAX_RSS_FALLBACK_ATTEMPTS) reached.")
        }
    }

    private fun fetchVideosFromRss() {
        Log.d(TAG, "fetchVideosFromRss (widget $appWidgetId): Current channelId '$channelId'")
        if (channelId.isBlank()) {
            Log.e(TAG, "fetchVideosFromRss (widget $appWidgetId): Channel ID is blank.")
            return
        }

        val rssUrlString = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
        var connection: HttpURLConnection? = null
        val tempVideoList = mutableListOf<WidgetCompatibleVideo>()

        try {
            Log.d(TAG, "fetchVideosFromRss (widget $appWidgetId): Connecting to $rssUrlString")
            val url = URL(rssUrlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(InputStreamReader(inputStream, "UTF-8"))
                var eventType = parser.eventType
                var currentVideoId: String? = null
                var currentTitle: String? = null
                var currentDescription: String? = null 
                var currentThumbnailUrlString: String? = null
                var currentPublishedAt: String? = null 
                var inEntry = false
                var inMediaGroup = false 

                while (eventType != XmlPullParser.END_DOCUMENT && tempVideoList.size < 10) { 
                    val tagName = parser.name
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (tagName.equals("entry", ignoreCase = true)) {
                                inEntry = true
                                currentVideoId = null; currentTitle = null; currentDescription = null; currentThumbnailUrlString = null; currentPublishedAt = null
                            } else if (tagName.equals("media:group", ignoreCase = true)) {
                                inMediaGroup = true
                            } else if (inEntry) {
                                if (inMediaGroup) {
                                    when {
                                        tagName.equals("media:description", ignoreCase = true) && parser.next() == XmlPullParser.TEXT -> currentDescription = parser.text
                                        tagName.equals("media:thumbnail", ignoreCase = true) -> currentThumbnailUrlString = parser.getAttributeValue(null, "url")
                                    }
                                } else {
                                    when {
                                        tagName.equals("yt:videoId", ignoreCase = true) && parser.next() == XmlPullParser.TEXT -> currentVideoId = parser.text
                                        tagName.equals("title", ignoreCase = true) && parser.next() == XmlPullParser.TEXT -> currentTitle = parser.text
                                        tagName.equals("published", ignoreCase = true) && parser.next() == XmlPullParser.TEXT -> currentPublishedAt = parser.text
                                        tagName.equals("media:thumbnail", ignoreCase = true) && !inMediaGroup -> currentThumbnailUrlString = parser.getAttributeValue(null, "url")
                                    }
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (tagName.equals("entry", ignoreCase = true)) {
                                if (currentVideoId != null && currentTitle != null) {
                                    val bitmap = downloadBitmap(currentThumbnailUrlString)
                                    tempVideoList.add(WidgetCompatibleVideo(
                                        id = currentVideoId!!,
                                        title = currentTitle!!,
                                        description = currentDescription ?: "No description available.",
                                        thumbnailUrl = currentThumbnailUrlString,
                                        thumbnailBitmap = bitmap, // This will now be the scaled bitmap
                                        publishedAt = currentPublishedAt 
                                    ))
                                }
                                inEntry = false
                            } else if (tagName.equals("media:group", ignoreCase = true)) {
                                inMediaGroup = false
                            }
                        }
                    }
                    eventType = parser.next()
                }
                videoList = tempVideoList
                Log.d(TAG, "fetchVideosFromRss (widget $appWidgetId): Successfully parsed ${videoList.size} videos.")
            } else {
                Log.e(TAG, "fetchVideosFromRss (widget $appWidgetId): HTTP Error ${connection.responseCode} for $rssUrlString")
            }
        } catch (e: XmlPullParserException) {
            Log.e(TAG, "fetchVideosFromRss (widget $appWidgetId): XML Parsing Error: ${e.message}", e)
        } catch (e: IOException) {
            Log.e(TAG, "fetchVideosFromRss (widget $appWidgetId): Network IOException: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "fetchVideosFromRss (widget $appWidgetId): General Exception: ${e.message}", e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchDataAsync() {
        Log.d(TAG, "fetchDataAsync (widget $appWidgetId): Initiating video fetch.")
        Thread { 
            var fetchedFromApi = false
            try {
                Log.d(TAG, "fetchDataAsync (widget $appWidgetId): API attempt. ChannelId: '$channelId'. RSS attempts: $rssFallbackAttemptCount")
                if (channelId.isBlank()) {
                    Log.e(TAG, "fetchDataAsync (widget $appWidgetId): YouTube Channel ID is blank.")
                    tryRssFallback()
                    return@Thread
                }
                val response = youtubeApiService.getChannelVideosSync(
                    part = "snippet", 
                    channelId = channelId, 
                    apiKey = apiKey,
                    maxResults = 10, 
                    type = "video", 
                    order = "date"
                ).execute()

                if (response.isSuccessful && response.body() != null) {
                    val youtubeVideoItems = response.body()!!.items ?: emptyList()
                    if (youtubeVideoItems.isNotEmpty()) {
                        videoList = youtubeVideoItems.mapNotNull { apiItem ->
                            apiItem.id?.videoId?.let { videoId ->
                                val thumbnailUrl = apiItem.snippet?.thumbnails?.high?.url
                                    ?: apiItem.snippet?.thumbnails?.medium?.url
                                    ?: apiItem.snippet?.thumbnails?.default?.url
                                val bitmap = downloadBitmap(thumbnailUrl) // This will now return a scaled bitmap
                                WidgetCompatibleVideo(
                                    id = videoId,
                                    title = apiItem.snippet?.title ?: "No Title",
                                    description = apiItem.snippet?.description ?: "No description",
                                    thumbnailUrl = thumbnailUrl,
                                    thumbnailBitmap = bitmap, // This will now be the scaled bitmap
                                    publishedAt = apiItem.snippet?.publishedAt
                                )
                            }
                        }
                        fetchedFromApi = true
                        rssFallbackAttemptCount = 0 
                        Log.d(TAG, "fetchDataAsync (widget $appWidgetId): YouTube API success, ${videoList.size} videos.")
                    } else {
                        Log.w(TAG, "fetchDataAsync (widget $appWidgetId): YouTube API success but 0 items. Attempting RSS.")
                        tryRssFallback()
                    }
                } else {
                    Log.e(TAG, "fetchDataAsync (widget $appWidgetId): YouTube API Error ${response.code()}.")
                    tryRssFallback()
                }
            } catch (e: IOException) {
                Log.e(TAG, "fetchDataAsync (widget $appWidgetId): YouTube API IOException: ${e.message}. Attempting RSS.", e)
                tryRssFallback()
            } catch (e: Exception) {
                Log.e(TAG, "fetchDataAsync (widget $appWidgetId): YouTube API General Exception: ${e.message}. Attempting RSS.", e)
                tryRssFallback()
            } finally {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.video_widget_list_view)
                Log.d(TAG, "fetchDataAsync (widget $appWidgetId): Notified data changed. Source: ${if(fetchedFromApi) "API" else "RSS/Fallback"}. Items: ${videoList.size}. RSS Attempts: $rssFallbackAttemptCount")
            }
        }.start()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy for widget $appWidgetId")
        videoList = emptyList() 
    }

    override fun getCount(): Int {
        Log.d(TAG, "getCount for widget $appWidgetId: returning ${videoList.size}")
        return videoList.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        Log.d(TAG, "getViewAt for widget $appWidgetId, pos $position. List size: ${videoList.size}")
        if (position < 0 || position >= videoList.size) {
            Log.e(TAG, "getViewAt: Invalid position $position for widget $appWidgetId")
            return RemoteViews(context.packageName, R.layout.video_widget_item).apply {
                setTextViewText(R.id.video_widget_item_title, "Error: Item unavailable")
                setViewVisibility(R.id.video_widget_item_thumbnail, View.GONE)
            }
        }
        val videoItem = videoList[position]
        val views = RemoteViews(context.packageName, R.layout.video_widget_item)

        views.setTextViewText(R.id.video_widget_item_title, videoItem.title)
        views.setTextViewText(R.id.video_widget_item_description, videoItem.description)
        
        videoItem.publishedAt?.let { date ->
            views.setTextViewText(R.id.video_widget_item_published_at, date) 
            views.setViewVisibility(R.id.video_widget_item_published_at, View.VISIBLE)
        } ?: run {
            views.setViewVisibility(R.id.video_widget_item_published_at, View.GONE)
        }

        if (videoItem.thumbnailBitmap != null) {
            views.setImageViewBitmap(R.id.video_widget_item_thumbnail, videoItem.thumbnailBitmap)
            views.setViewVisibility(R.id.video_widget_item_thumbnail, View.VISIBLE)
        } else {
            views.setImageViewResource(R.id.video_widget_item_thumbnail, R.drawable.ic_placeholder)
            views.setViewVisibility(R.id.video_widget_item_thumbnail, View.VISIBLE)
            Log.d(TAG, "getViewAt (widget $appWidgetId): Thumbnail bitmap null for ${videoItem.title}. Placeholder. URL: ${videoItem.thumbnailUrl}")
        }

        var itemTitleColor = ContextCompat.getColor(context, R.color.widget_default_text_primary)
        var itemDescriptionColor = ContextCompat.getColor(context, R.color.widget_default_text_secondary)

        currentThemeName?.let { theme ->
            when (theme) {
                AvailableTheme.Pink.displayName -> {
                    itemTitleColor = ContextCompat.getColor(context, R.color.widget_pink_text_primary)
                    itemDescriptionColor = ContextCompat.getColor(context, R.color.widget_pink_text_secondary)
                }
                AvailableTheme.Lavender.displayName -> {
                    itemTitleColor = ContextCompat.getColor(context, R.color.widget_lavender_text_primary)
                    itemDescriptionColor = ContextCompat.getColor(context, R.color.widget_lavender_text_secondary)
                }
                AvailableTheme.Mint.displayName -> {
                    itemTitleColor = ContextCompat.getColor(context, R.color.widget_mint_text_primary)
                    itemDescriptionColor = ContextCompat.getColor(context, R.color.widget_mint_text_secondary)
                }
            }
        }
        views.setTextColor(R.id.video_widget_item_title, itemTitleColor)
        views.setTextColor(R.id.video_widget_item_description, itemDescriptionColor)

        val fillInIntent = Intent().apply {
            action = ACTION_VIEW_VIDEO
            val extras = Bundle()
            extras.putString(EXTRA_VIDEO_ID, videoItem.id)
            putExtras(extras)
        }
        views.setOnClickFillInIntent(R.id.video_widget_item_root, fillInIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews {
        Log.d(TAG, "getLoadingView for widget $appWidgetId")
        return RemoteViews(context.packageName, R.layout.widget_loading_view).apply {
            setTextViewText(R.id.loading_text, "Loading videos...")
        }
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        if (position >= 0 && position < videoList.size) {
            return (videoList[position].id + (videoList[position].publishedAt ?: "")).hashCode().toLong()
        }
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}
