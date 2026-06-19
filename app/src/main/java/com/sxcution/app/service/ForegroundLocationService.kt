package com.sxcution.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sxcution.app.MainActivity
import com.sxcution.app.R
import com.sxcution.app.core.LocationBus
import com.sxcution.app.core.Logger
import com.sxcution.app.utils.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground location service that displays current location in notification.
 * Service runs silently with IMPORTANCE_LOW notification.
 */
class ForegroundLocationService : Service() {
    
    companion object {
        const val ACTION_STOP = "com.sxcution.app.ACTION_STOP"
    }
    
    private var locationUpdateJob: Job? = null
    private var lastNotificationUpdate = 0L
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var currentSmartName: String? = null
    
    override fun onCreate() {
        super.onCreate()
        
        // Create notification channel
        createNotificationChannel()
        
        // Start collecting location updates
        startLocationUpdateCollection()
        
        Logger.d("ForegroundLocationService") { "Service created" }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
        }
        
        // Check for smart name in intent extras
        intent?.getStringExtra("smart_name")?.let { smartName ->
            currentSmartName = smartName
            Logger.d("ForegroundLocationService") { "Received smart name: $smartName" }
        }
        
        // Start foreground service with initial notification
        startForegroundService()
        
        Logger.d("ForegroundLocationService") { "Service started" }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        locationUpdateJob?.cancel()
        serviceScope.cancel()
        Logger.d("ForegroundLocationService") { "Service destroyed" }
    }
    
    /**
     * Starts collecting location updates from LocationBus
     */
    private fun startLocationUpdateCollection() {
        locationUpdateJob = serviceScope.launch {
            LocationBus.locationTicks.collect { locationUpdate ->
                updateNotificationWithLocation(locationUpdate)
            }
        }
    }
    
    /**
     * Updates notification with new location data
     * Rate-limited to â‰¥1 second between updates
     */
    private fun updateNotificationWithLocation(locationUpdate: LocationBus.LocationUpdate) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationUpdate >= 1000) { // Rate limit â‰¥1s
            val locationText = buildNotificationText(locationUpdate)
            updateNotification(locationText)
            lastNotificationUpdate = currentTime
        }
    }
    
    /**
     * Build notification text with smart name support
     */
    private fun buildNotificationText(locationUpdate: LocationBus.LocationUpdate): String {
        val coordinates = "Lat: %.6f, Lng: %.6f".format(
            locationUpdate.latitude,
            locationUpdate.longitude
        )
        
        return when {
            // If we have a smart name (address), show it instead of "Location active"
            currentSmartName != null -> currentSmartName!!
            // In debug builds, always show coordinates
            isDebugBuild() -> coordinates
            // In production, show neutral text
            else -> "Location active"
        }
    }
    
    /**
     * Check if this is a debug build
     */
    private fun isDebugBuild(): Boolean {
        return try {
            val buildConfigClass = Class.forName("com.sxcution.app.BuildConfig")
            val debugField = buildConfigClass.getField("DEBUG")
            debugField.getBoolean(null)
        } catch (e: Exception) {
            false // Default to production mode if can't determine
        }
    }
    
    /**
     * Starts the foreground service with notification
     */
    private fun startForegroundService() {
        val notification = createLocationNotification("Location service running", "Initializing...")
        startForeground(NotificationUtils.SERVICE_ID, notification)
    }
    
    /**
     * Stops the foreground service
     */
    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }
    
    /**
     * Updates the notification with new text
     */
    private fun updateNotification(text: String) {
        val notification = createLocationNotification("Location service running", text)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationUtils.SERVICE_ID, notification)
    }
    
    /**
     * Creates the location notification
     */
    private fun createLocationNotification(title: String, text: String): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, ForegroundLocationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID)
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
                stopPendingIntent
            )
            .build()
    }
    
    /**
     * Creates the notification channel for the foreground location service
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationUtils.CHANNEL_ID,
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
}
