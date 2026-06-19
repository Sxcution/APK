package com.sxcution.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sxcution.app.R

/**
 * Utility class for managing notifications
 */
object NotificationUtils {
    
    const val CHANNEL_ID = "location_service"
    const val SERVICE_ID = 1001
    
    /**
     * Creates the notification channel for the foreground location service.
     * Channel is set to IMPORTANCE_LOW (silent) with no vibration/sound.
     */
    fun Context.createMonNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Creates a notification builder for the foreground location service
     */
    fun Context.createLocationNotificationBuilder(
        title: String,
        text: String,
        contentIntent: android.app.PendingIntent?,
        stopAction: android.app.PendingIntent?
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_location,
                getString(R.string.stop),
                stopAction
            )
    }
}
