package com.kawaii.meowbah.ui.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MeowTalkScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "MeowTalkScheduler"
        private const val REQUEST_CODE_MEOWTALK = 1001
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(context, MeowTalkAlarmReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE_MEOWTALK, intent, flags)
    }

    fun schedule(
        intervalMinutes: Int,
        schedulingType: String,
        specificHour: Int,
        specificMinute: Int
    ) {
        Log.d(TAG, "Attempting to schedule MeowTalk: type='$schedulingType', interval=$intervalMinutes min, time=$specificHour:$specificMinute")
        val pendingIntent = getPendingIntent()

        if (schedulingType == "interval") {
            if (intervalMinutes > 0) {
                val intervalMillis = TimeUnit.MINUTES.toMillis(intervalMinutes.toLong())
                val firstTriggerMillis = System.currentTimeMillis() + intervalMillis // Start after one interval
                
                // For repeating alarms, setInexactRepeating is generally preferred for battery.
                // If more precision is needed, consider setRepeating with caution or WorkManager for deferrable tasks.
                try {
                    alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP, // Use RTC_WAKEUP to wake device if sleeping
                        firstTriggerMillis,
                        intervalMillis,
                        pendingIntent
                    )
                    Log.i(TAG, "MeowTalk scheduled with interval: $intervalMinutes minutes.")
                } catch (se: SecurityException) {
                     Log.e(TAG, "SecurityException on setInexactRepeating. Check SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM if targeting S+ and needing exactness (though inexact was used here).", se)
                }
            } else {
                Log.w(TAG, "Interval is 0, not scheduling interval-based MeowTalk.")
                cancel() // Cancel if interval is invalid
            }
        } else if (schedulingType == "specific_time") {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, specificHour)
                set(Calendar.MINUTE, specificMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If the time is already past for today, schedule it for tomorrow
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                    Log.d(TAG, "Specific time is past for today, scheduling for tomorrow at $specificHour:$specificMinute")
                }
            }
            // For daily specific time, setInexactRepeating with a daily interval is appropriate.
            // AlarmManager.INTERVAL_DAY is a constant for 24 hours.
            try {
                 alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY, 
                    pendingIntent
                )
                Log.i(TAG, "MeowTalk scheduled for specific time: ${calendar.time} (daily).")
            } catch (se: SecurityException) {
                 Log.e(TAG, "SecurityException on setInexactRepeating for specific time. Check permissions if targeting S+.", se)
            }

        } else {
            Log.w(TAG, "Unknown scheduling type: $schedulingType. Cancelling any existing MeowTalk.")
            cancel()
        }
    }

    fun cancel() {
        Log.i(TAG, "Cancelling MeowTalk alarm.")
        val pendingIntent = getPendingIntent()
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel() // Also cancel the pending intent itself
    }
}
