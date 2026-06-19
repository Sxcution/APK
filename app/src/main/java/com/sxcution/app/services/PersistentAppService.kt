package com.sxcution.app.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sxcution.app.R
import kotlinx.coroutines.*

class PersistentAppService : Service() {
    
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var keepAliveJob: Job? = null
    
    companion object {
        private const val NOTIFICATION_ID = 9999
        private const val CHANNEL_ID = "persistent_service_channel"
        private const val TAG = "PersistentAppService"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PersistentAppService created")
        
        // Create notification channel
        createNotificationChannel()
        
        // Acquire wake lock to prevent CPU sleep
        acquireWakeLock()
        
        // Start foreground service with notification
        startForegroundService()
        
        // Start keep-alive mechanism
        startKeepAlive()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "PersistentAppService started")
        
        // Restart keep-alive if needed
        if (keepAliveJob?.isActive != true) {
            startKeepAlive()
        }
        
        // Return START_STICKY to restart if killed
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PersistentAppService destroyed")
        
        // Cancel keep-alive job
        keepAliveJob?.cancel()
        
        // Release wake lock
        releaseWakeLock()
        
        // Restart service immediately
        restartService()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps app running in background"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Protection")
            .setContentText("Keeping app active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Sxcution::PersistentService"
            )
            wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
            Log.d(TAG, "Wake lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }
    
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Wake lock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock", e)
        }
    }
    
    private fun startKeepAlive() {
        keepAliveJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Update notification to show service is alive
                    updateNotification()
                    
                    // Sleep for 30 seconds
                    delay(30_000)
                } catch (e: Exception) {
                    Log.e(TAG, "Keep-alive error", e)
                    delay(5_000) // Wait 5 seconds before retry
                }
            }
        }
    }
    
    private fun updateNotification() {
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App Protection")
                .setContentText("Active - ${System.currentTimeMillis()}")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false)
                .setSilent(true)
                .build()
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification", e)
        }
    }
    
    private fun restartService() {
        try {
            val restartIntent = Intent(this, PersistentAppService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
            Log.d(TAG, "Service restart initiated")
            
            // Also restart GPS service if it was running
            restartGpsService()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart service", e)
        }
    }
    
    private fun restartGpsService() {
        try {
            val gpsIntent = Intent(this, LocationService::class.java).apply {
                action = "AUTO_RESTART_GPS"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(gpsIntent)
            } else {
                startService(gpsIntent)
            }
            Log.d(TAG, "GPS service auto-restart initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart GPS service", e)
        }
    }
}
