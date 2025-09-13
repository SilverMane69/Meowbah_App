package com.kawaii.meowbah.ui.activities

import android.app.Notification // Required for Notification.VISIBILITY_PUBLIC
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kawaii.meowbah.MainActivity // To open MainActivity on tap
import com.kawaii.meowbah.R // For drawable resources
import java.io.File

object NotificationUtils {

    // Temp define constants to avoid MainActivity dependency during build issue
    private const val ACTION_VIEW_VIDEO_TEMP = "com.kawaii.meowbah.ACTION_VIEW_VIDEO"
    private const val EXTRA_VIDEO_ID_TEMP = "com.kawaii.meowbah.EXTRA_VIDEO_ID"

    private const val CHANNEL_ID = "meowbah_new_video_channel"
    private const val CHANNEL_NAME = "New Meowbah Videos"
    private const val CHANNEL_DESCRIPTION = "Notifications for new Meowbah video uploads"
    private const val NOTIFICATION_ID_PREFIX = "new_video_"
    private const val TAG = "NotificationUtils"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Video Notification Channel created with public lockscreen visibility.")
        }
    }

    fun showNewVideoNotification(context: Context, videoTitle: String, videoId: String, thumbnailPath: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = ACTION_VIEW_VIDEO_TEMP 
            putExtra(EXTRA_VIDEO_ID_TEMP, videoId) 
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 
            videoId.hashCode(), 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val smallIconResId = R.drawable.ic_notification_meowbah_monochrome // MODIFIED LINE
        var largeIconBitmap: Bitmap? = null
        var videoThumbnailBitmap: Bitmap? = null

        if (!thumbnailPath.isNullOrBlank()) {
            try {
                val thumbnailFile = File(thumbnailPath)
                if (thumbnailFile.exists()) {
                    videoThumbnailBitmap = BitmapFactory.decodeFile(thumbnailFile.absolutePath)
                    largeIconBitmap = videoThumbnailBitmap // Use video thumbnail as large icon if available
                } else {
                    Log.w(TAG, "Thumbnail file not found at: $thumbnailPath")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding thumbnail from path: $thumbnailPath", e)
            }
        }

        // Fallback to meowlogo if video thumbnail couldn't be loaded
        if (largeIconBitmap == null) {
            largeIconBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.meowlogo)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconResId) 
            .setLargeIcon(largeIconBitmap) // This will be video thumbnail if loaded, else meowlogo
            .setContentTitle("Nyaa~ New video just dropped!")
            .setContentText("Tap to watch meow! \"${videoTitle}\" is ready to watch. Tap to view!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Apply BigPictureStyle if video thumbnail was successfully loaded
        videoThumbnailBitmap?.let {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(it)
                    .bigLargeIcon(null as Bitmap?) // Resolved ambiguity by casting null
            )
        }

        with(NotificationManagerCompat.from(context)) {
            val notificationId = (NOTIFICATION_ID_PREFIX + videoId).hashCode()
            try {
                 notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: Missing POST_NOTIFICATIONS permission?", e)
            }
        }
    }
}
