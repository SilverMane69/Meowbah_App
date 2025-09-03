package com.kawaii.meowbah.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.kawaii.meowbah.MainActivity // Assuming MainActivity is your main app entry
import com.kawaii.meowbah.R
import com.kawaii.meowbah.ui.theme.AvailableTheme

class VideoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the widget is deleted, clean up the saved theme preference
        for (appWidgetId in appWidgetIds) {
            VideoWidgetConfigureActivity.deleteThemePref(context, appWidgetId)
        }
        super.onDeleted(context, appWidgetIds)
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.video_widget_layout)

            // Load the saved theme
            val themeName = VideoWidgetConfigureActivity.loadThemePref(context, appWidgetId)
            var widgetBackgroundColor = ContextCompat.getColor(context, R.color.widget_default_background)
            var widgetTitleColor = ContextCompat.getColor(context, R.color.widget_default_text_primary)

            when (themeName) {
                AvailableTheme.Pink.displayName -> {
                    widgetBackgroundColor = ContextCompat.getColor(context, R.color.widget_pink_background)
                    widgetTitleColor = ContextCompat.getColor(context, R.color.widget_pink_text_primary)
                }
                AvailableTheme.Lavender.displayName -> {
                    widgetBackgroundColor = ContextCompat.getColor(context, R.color.widget_lavender_background)
                    widgetTitleColor = ContextCompat.getColor(context, R.color.widget_lavender_text_primary)
                }
                AvailableTheme.Mint.displayName -> {
                    widgetBackgroundColor = ContextCompat.getColor(context, R.color.widget_mint_background)
                    widgetTitleColor = ContextCompat.getColor(context, R.color.widget_mint_text_primary)
                }
                // MaterialYou or default will use the default widget colors defined above
            }

            // Apply theme colors to the main widget layout
            // Note: Setting background color on the root of RemoteViews can be tricky.
            // It's often better to have an inner root view in your XML (like a FrameLayout or LinearLayout)
            // and set its background. Assuming video_widget_layout.xml has a root LinearLayout.
            // If R.id.widget_root_layout is the ID of the root LinearLayout in video_widget_layout.xml:
            // views.setInt(R.id.widget_root_layout, "setBackgroundColor", widgetBackgroundColor)
            // For now, we will assume the default background set in XML is sufficient or use a simpler approach.
            // We'll primarily theme text and specific elements for broad compatibility.
            views.setTextColor(R.id.widget_title, widgetTitleColor)
            // Consider adding a specific background to the title's container if desired.

            val intent = Intent(context, VideoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.video_widget_list_view, intent)
            views.setEmptyView(R.id.video_widget_list_view, R.id.video_widget_empty_view)

            val itemClickIntent = Intent(context, MainActivity::class.java)
            val itemClickPendingIntent = PendingIntent.getActivity(
                context,
                0,
                itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setPendingIntentTemplate(R.id.video_widget_list_view, itemClickPendingIntent)

            val configIntent = Intent(context, VideoWidgetConfigureActivity::class.java)
            configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // Ensure a unique request code for the pending intent if multiple widgets can trigger this
            val configPendingIntent = PendingIntent.getActivity(context, appWidgetId, configIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_title, configPendingIntent) // Allow re-configuring by clicking title


            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.video_widget_list_view)
        }
    }
}
