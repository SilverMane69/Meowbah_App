package com.kawaii.meowbah.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kawaii.meowbah.ui.notifications.MeowTalkScheduler

class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
        // Re-define SharedPreferences keys here to avoid direct dependency on MainActivity companion
        // Ensure these exactly match the keys in MainActivity.kt
        private const val PREFS_NAME = "MeowbahAppPreferences"
        private const val KEY_MEOWTALK_ENABLED = "meowTalkEnabled"
        private const val KEY_MEOWTALK_INTERVAL_MINUTES = "meowTalkIntervalMinutes"
        private const val KEY_MEOWTALK_SCHEDULING_TYPE = "meowTalkSchedulingType"
        private const val KEY_MEOWTALK_SPECIFIC_HOUR = "meowTalkSpecificHour"
        private const val KEY_MEOWTALK_SPECIFIC_MINUTE = "meowTalkSpecificMinute"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Device boot completed. Checking MeowTalk schedule.")

            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isMeowTalkEnabled = sharedPrefs.getBoolean(KEY_MEOWTALK_ENABLED, false)

            if (isMeowTalkEnabled) {
                Log.d(TAG, "MeowTalk was enabled. Rescheduling...")
                val intervalMinutes = sharedPrefs.getInt(KEY_MEOWTALK_INTERVAL_MINUTES, 30)
                val schedulingType = sharedPrefs.getString(KEY_MEOWTALK_SCHEDULING_TYPE, "interval") ?: "interval"
                val specificHour = sharedPrefs.getInt(KEY_MEOWTALK_SPECIFIC_HOUR, 12)
                val specificMinute = sharedPrefs.getInt(KEY_MEOWTALK_SPECIFIC_MINUTE, 0)

                val scheduler = MeowTalkScheduler(context)
                scheduler.schedule(
                    intervalMinutes = intervalMinutes,
                    schedulingType = schedulingType,
                    specificHour = specificHour,
                    specificMinute = specificMinute
                )
                Log.i(TAG, "MeowTalk rescheduled successfully after boot.")
            } else {
                Log.i(TAG, "MeowTalk was not enabled. No action needed.")
            }
        }
    }
}
