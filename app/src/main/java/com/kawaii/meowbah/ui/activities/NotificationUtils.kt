package com.kawaii.meowbah.ui.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kawaii.meowbah.MainActivity // To open MainActivity on tap
import com.kawaii.meowbah.R // For drawable resources

object NotificationUtils {

    private const val CHANNEL_ID = "meowbah_new_video_channel"
    private const val CHANNEL_NAME = "New Meowbah Videos"
    private const val CHANNEL_DESCRIPTION = "Notifications for new Meowbah video uploads"
    private const val NOTIFICATION_ID_PREFIX = "new_video_"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNewVideoNotification(context: Context, videoTitle: String, videoId: String) {
        // Create an explicit intent for an Activity in your app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Optionally, you can add data to the intent to navigate to the specific video
            // e.g., putExtra("videoId", videoId)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 
            videoId.hashCode(), // Use videoId hashcode for unique request code 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Use the new monochrome icon
        val smallIconResId = R.drawable.ic_notification_meowbah_monochrome 

        // Use your app logo as the large icon
        val largeIconBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.meowlogo)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconResId) 
            .setLargeIcon(largeIconBitmap)
            .setContentTitle("New Meowbah Video!")
            .setContentText(videoTitle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismiss notification when tapped

        with(NotificationManagerCompat.from(context)) {
            val notificationId = (NOTIFICATION_ID_PREFIX + videoId).hashCode()
            try {
                 notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                // This can happen if POST_NOTIFICATIONS permission is not granted on Android 13+
                // Log or handle this appropriately. For now, just logging.
                android.util.Log.e("NotificationUtils", "SecurityException: Missing POST_NOTIFICATIONS permission?", e)
            }
           
        }
    }
}
