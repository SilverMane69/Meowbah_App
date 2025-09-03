package com.kawaii.meowbah.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.kawaii.meowbah.R
import com.kawaii.meowbah.data.SampleVideos
import com.kawaii.meowbah.data.WidgetCompatibleVideo
import com.kawaii.meowbah.ui.theme.AvailableTheme

class VideoWidgetViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var videoList = listOf<WidgetCompatibleVideo>()
    private var currentThemeName: String? = null
    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    companion object {
        // Define constants for the Intent action and extras
        const val ACTION_VIEW_VIDEO = "com.kawaii.meowbah.ACTION_VIEW_VIDEO"
        const val EXTRA_VIDEO_ID = "com.kawaii.meowbah.EXTRA_VIDEO_ID"
    }

    override fun onCreate() {
        videoList = SampleVideos.MOCK_VIDEO_LIST
        loadTheme()
    }

    override fun onDataSetChanged() {
        loadTheme()
        videoList = SampleVideos.MOCK_VIDEO_LIST 
    }

    private fun loadTheme() {
        currentThemeName = VideoWidgetConfigureActivity.loadThemePref(context, appWidgetId)
    }

    override fun onDestroy() {
        videoList = emptyList()
    }

    override fun getCount(): Int {
        return videoList.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val videoItem = videoList[position]
        val views = RemoteViews(context.packageName, R.layout.video_widget_item)

        views.setTextViewText(R.id.video_widget_item_title, videoItem.title)
        views.setTextViewText(R.id.video_widget_item_description, videoItem.description)

        var itemTitleColor = ContextCompat.getColor(context, R.color.widget_default_text_primary)
        var itemDescriptionColor = ContextCompat.getColor(context, R.color.widget_default_text_secondary)

        when (currentThemeName) {
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
        views.setTextColor(R.id.video_widget_item_title, itemTitleColor)
        views.setTextColor(R.id.video_widget_item_description, itemDescriptionColor)

        // Create the fill-in Intent for the item click
        val fillInIntent = Intent().apply {
            // Set the action defined in the companion object
            action = ACTION_VIEW_VIDEO
            // Add the video ID as an extra
            val extras = Bundle()
            extras.putString(EXTRA_VIDEO_ID, videoItem.id)
            putExtras(extras)
        }
        
        // Set the click event for the root of the list item
        views.setOnClickFillInIntent(R.id.video_widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        // Access videoList using the position
        if (position >= 0 && position < videoList.size) {
            return videoList[position].id.hashCode().toLong()
        }
        // Fallback, though ideally this shouldn't be reached if adapter and list view are in sync
        return position.toLong() 
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}
