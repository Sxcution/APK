package com.sxcution.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LocationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "android.intent.action.BOOT_COMPLETED" -> {
                val serviceIntent = Intent(context, com.sxcution.app.services.LocationService::class.java).apply {
                    action = "AUTO_RESTART_GPS"
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
