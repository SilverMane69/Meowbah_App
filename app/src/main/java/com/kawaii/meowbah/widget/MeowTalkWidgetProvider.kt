package com.kawaii.meowbah.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.kawaii.meowbah.MainActivity // Import MainActivity to access constants
import com.kawaii.meowbah.R
import com.kawaii.meowbah.ui.notifications.MeowTalkAlarmReceiver // Required for intent extras

/**
 * Implementation of App Widget functionality.
 */
class MeowTalkWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_MEOWTALK_PHRASE_UPDATED = "com.kawaii.meowbah.ACTION_MEOWTALK_PHRASE_UPDATED"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent) // Important to call super for other actions
        if (ACTION_MEOWTALK_PHRASE_UPDATED == intent.action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, javaClass.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
            onUpdate(context, appWidgetManager, appWidgetIds) // Trigger update for all instances
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val sharedPrefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
    val currentPhrase = sharedPrefs.getString(MainActivity.KEY_MEOWTALK_CURRENT_PHRASE, "No MeowTalk phrase set.")

    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.meow_talk_widget_layout)
    views.setTextViewText(R.id.appwidget_text, currentPhrase)

    // Create an Intent to launch MainActivity and navigate to MeowTalk tab
    val activityIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra(MeowTalkAlarmReceiver.EXTRA_NAVIGATE_TO_TAB, MeowTalkAlarmReceiver.MEOWTALK_TAB_ROUTE)
    }

    val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pendingIntent = PendingIntent.getActivity(context, appWidgetId, activityIntent, pendingIntentFlags)

    views.setOnClickPendingIntent(R.id.meow_talk_widget_root, pendingIntent)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
