package com.kawaii.meowbah.ui.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kawaii.meowbah.MainActivity // For accessing companion object constants
import com.kawaii.meowbah.MEOWTALK_NOTIFICATION_CHANNEL_ID
import com.kawaii.meowbah.R
import com.kawaii.meowbah.data.MeowTalkPhrases // Needed to pick a new phrase
import com.kawaii.meowbah.util.MeowTalkEventBus
import com.kawaii.meowbah.widget.MeowTalkWidgetProvider // Added import
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.random.Random

class MeowTalkAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MeowTalkAlarmReceiver"
        private const val NOTIFICATION_ID = 12345
        const val EXTRA_NAVIGATE_TO_TAB = "navigateToTab"
        const val MEOWTALK_TAB_ROUTE = "meowtalk_tab"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Alarm received. Will pick new phrase, save, notify, and emit.")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Pick a new random phrase
        val newActivePhrase = if (MeowTalkPhrases.list.isNotEmpty()) {
            MeowTalkPhrases.list[Random.nextInt(MeowTalkPhrases.list.size)]
        } else {
            "Meow! Default phrase for notification!"
        }
        Log.d(TAG, "New phrase selected for this cycle: $newActivePhrase")

        // 2. Save this new phrase to SharedPreferences, making it the current active phrase
        val sharedPrefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString(MainActivity.KEY_MEOWTALK_CURRENT_PHRASE, newActivePhrase)
            apply()
        }
        Log.d(TAG, "Saved new active phrase to Prefs: $newActivePhrase")

        // Broadcast that the phrase has been updated for the widget
        val widgetUpdateIntent = Intent(context, MeowTalkWidgetProvider::class.java).apply {
            action = MeowTalkWidgetProvider.ACTION_MEOWTALK_PHRASE_UPDATED
        }
        context.sendBroadcast(widgetUpdateIntent)
        Log.d(TAG, "Sent broadcast for widget update: ${MeowTalkWidgetProvider.ACTION_MEOWTALK_PHRASE_UPDATED}")

        // 3. Post notification with this new active phrase
        val launchAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_NAVIGATE_TO_TAB, MEOWTALK_TAB_ROUTE)
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingLaunchIntent = PendingIntent.getActivity(context, 0, launchAppIntent, pendingIntentFlags)

        val notification = NotificationCompat.Builder(context, MEOWTALK_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_meowbah_monochrome)
            .setContentTitle("Meowbah's MeowTalk")
            .setContentText(newActivePhrase) // Use the newly selected phrase
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingLaunchIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // ADDED THIS LINE
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Notification posted with phrase: $newActivePhrase")

            // 4. Emit this new active phrase via EventBus so ViewModel can update the screen
            val pendingResult = goAsync()
            GlobalScope.launch {
                try {
                    MeowTalkEventBus.emitNewPhraseActivated(newActivePhrase)
                    Log.d(TAG, "Event emitted for activated phrase: $newActivePhrase")
                } finally {
                    pendingResult.finish()
                }
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Could not post notification.", e)
            // Consider prompting user to grant POST_NOTIFICATIONS permission if this occurs on Android 13+
            // Or guide them to app settings.
        } catch (e: Exception) {
            Log.e(TAG, "Error posting MeowTalk notification or emitting event", e)
        }
    }
}
