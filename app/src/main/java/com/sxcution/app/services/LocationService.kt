package com.sxcution.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sxcution.app.MainActivity
import com.sxcution.app.R
import com.sxcution.app.core.LocationBus
import com.sxcution.app.core.Logger
import java.util.Locale
import java.util.Random
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationService : Service() {
    
    private var locationManager: LocationManager? = null
    private var targetLocation: Location? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationUpdateHandler: Handler? = null
    private var locationUpdateRunnable: Runnable? = null
    private var lastUpdateTime: Long = 0
    private var lastUpdateNanos: Long = 0
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    
    // Bearing rotation variables
    private var isBearingRotationEnabled = true // Default ON
    private var lastBearingUpdateTime: Long = 0
    private var currentBearing: Float = 0f
    private var lastKnownLatitude: Double = 0.0
    private var lastKnownLongitude: Double = 0.0
    private var isStationary = true
    
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_service_channel"
        var isRunning = false
    }
    
    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationUpdateHandler = Handler(Looper.getMainLooper())
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_LOCATION_SERVICE" -> startLocationService(intent)
            "STOP_LOCATION_SERVICE" -> stopLocationService()
            "AUTO_RESTART_GPS" -> autoRestartGps()
            "UPDATE_LOCATION" -> updateLocation(intent)
            "UPDATE_BEARING_ROTATION" -> updateBearingRotationSetting(intent)
        }
        
        return START_STICKY
    }
    
    private fun startLocationService(intent: Intent) {
        val latitude = intent.getDoubleExtra("target_latitude", 0.0)
        val longitude = intent.getDoubleExtra("target_longitude", 0.0)
        val accuracy = intent.getFloatExtra("target_accuracy", 2f)
        
        if (latitude == 0.0 && longitude == 0.0) {
            stopSelf()
            return
        }
        
        // Get SharedPreferences - EXACTLY like DNA app
        val prefs = getSharedPreferences("location_prefs", MODE_PRIVATE)
        
        // Initialize monotonic timestamps
        lastUpdateTime = System.currentTimeMillis()
        lastUpdateNanos = SystemClock.elapsedRealtimeNanos()
        
        // Initialize bearing rotation
        lastBearingUpdateTime = lastUpdateTime
        currentBearing = prefs.getInt("bearing", 4).toFloat()
        lastKnownLatitude = latitude
        lastKnownLongitude = longitude
        
        // Load bearing rotation setting (default ON)
        isBearingRotationEnabled = prefs.getBoolean("bearing_rotation_enabled", true)
        
        targetLocation = Location("gps").apply {
            this.latitude = latitude
            this.longitude = longitude
            
            // Use accuracy from intent first, then SharedPreferences like DNA app
            this.accuracy = if (accuracy > 0) accuracy else prefs.getInt("accuracy", 2).toFloat()
            this.altitude = prefs.getInt("altitude", 5).toDouble()
            this.bearing = prefs.getInt("bearing", 4).toFloat()
            this.speed = prefs.getInt("speed", 1).toFloat()
            
            // Set monotonic timestamps
            this.time = lastUpdateTime
            this.elapsedRealtimeNanos = lastUpdateNanos
        }
        
        startForeground(NOTIFICATION_ID, createNotification())
        isRunning = true
        
        // Start location service using Test Provider method
        startLocationUpdates()
        
        Logger.d("LocationService") { "Location service started with target: $latitude, $longitude" }
        Logger.d("LocationService") { "LocationManager initialized" }
    }
    
    private fun stopLocationService() {
        stopLocationUpdates()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        isRunning = false
        
        Logger.d("LocationService") { "Location service stopped" }
    }
    
    private fun startLocationUpdates() {
        try {
            Logger.d("LocationService") { "Starting location updates..." }
            
            // Method from original DNA APK: Use Test Provider for both GPS and Network
            setupTestProviders()
            startLocationUpdateLoop()
            
            // Google Play Services integration like DNA app
            setupGooglePlayServices()
            
            // Save state for auto-restart
            saveLocationState()
            
            Logger.d("LocationService") { "Location updates started successfully" }
            
        } catch (e: Exception) {
            Logger.e("LocationService", { "Error starting location updates" }, e)
        }
    }
    
    private fun setupGooglePlayServices() {
        try {
            // Like DNA app: Get Google Play Services LocationClient
            val locationClient = LocationServices.getFusedLocationProviderClient(this)
            
            // Like DNA app: Set mock mode (if available)
            // Store locationClient for potential future use in system-level spoofing
            fusedLocationClient = locationClient
            
            // Log successful setup
            // Google Play Services setup completed silently
            
        } catch (e: Exception) {
            Log.e("LocationService", "Error setting up Google Play Services", e)
        }
    }
    
    private fun setupTestProviders() {
        try {
            // Setup GPS Test Provider (like original DNA APK)
            setupGPSProvider()
            
            // Setup Network Test Provider (like original DNA APK)
            setupNetworkProvider()
            
            // Test providers setup completed silently
            
        } catch (e: Exception) {
            Log.e("LocationService", "Error setting up test providers", e)
        }
    }
    
    private fun setupGPSProvider() {
        try {
            // Remove existing provider if exists
            try {
                locationManager?.removeTestProvider("gps")
            } catch (_: Exception) {
                // Provider doesn't exist, that's fine
            }
            
            // Add GPS test provider - EXACTLY like original DNA APK
            @Suppress("DEPRECATION", "MagicNumber")
            locationManager?.addTestProvider(
                "gps", // DNA uses "gps" string, not LocationManager.GPS_PROVIDER
                false, // requiresNetwork (DNA uses false)
                false, // requiresSatellite (DNA uses false)
                false, // requiresCell (DNA uses false)
                false, // hasMonetaryCost
                true,  // supportsAltitude
                true,  // supportsSpeed
                true,  // supportsBearing
                1,     // powerRequirement (1 = low power)
                2      // accuracy (2 = fine accuracy)
            )
            
            // Enable the test provider
            locationManager?.setTestProviderEnabled("gps", true)
            
            // Set test provider status for consistency
            @Suppress("DEPRECATION")
            locationManager?.setTestProviderStatus("gps", 2, null, System.currentTimeMillis())
            
            // GPS test provider setup completed - removed debug log to match DNA behavior
            
        } catch (e: Exception) {
            Logger.e("LocationService", { "Error setting up GPS provider" }, e)
            Logger.e("LocationService", { "Exception details: ${e.message}" }, e)
            Logger.e("LocationService", { "Exception type: ${e.javaClass.simpleName}" }, e)
        }
    }
    
    private fun setupNetworkProvider() {
        try {
            // Remove existing provider if exists
            try {
                locationManager?.removeTestProvider("network")
            } catch (_: Exception) {
                // Provider doesn't exist, that's fine
            }
            
            // Add Network test provider - EXACTLY like original DNA APK
            @Suppress("DEPRECATION", "MagicNumber")
            locationManager?.addTestProvider(
                "network", // DNA uses "network" string, not LocationManager.NETWORK_PROVIDER
                false, // requiresNetwork (DNA uses false)
                false, // requiresSatellite (DNA uses false)
                false, // requiresCell (DNA uses false)
                false, // hasMonetaryCost
                true,  // supportsAltitude
                true,  // supportsSpeed
                true,  // supportsBearing
                1,     // powerRequirement (1 = low power)
                2      // accuracy (2 = fine accuracy)
            )
            
            // Enable the test provider
            locationManager?.setTestProviderEnabled("network", true)
            
            // Set test provider status for consistency
            @Suppress("DEPRECATION")
            locationManager?.setTestProviderStatus("network", 2, null, System.currentTimeMillis())
            
            // Network test provider setup completed - removed debug log to match DNA behavior
            
        } catch (e: Exception) {
            Logger.e("LocationService", { "Error setting up Network provider" }, e)
            Logger.e("LocationService", { "Exception details: ${e.message}" }, e)
            Logger.e("LocationService", { "Exception type: ${e.javaClass.simpleName}" }, e)
        }
    }
    
    private fun startLocationUpdateLoop() {
        Logger.d("LocationService") { "Starting location update loop..." }
        locationUpdateRunnable = object : Runnable {
            override fun run() {
                targetLocation?.let { location ->
                    try {
                        // CRITICAL: Ensure monotonic timestamps
                        val currentTime = System.currentTimeMillis()
                        val currentNanos = SystemClock.elapsedRealtimeNanos()
                        
                        // Only update if time has progressed (avoid non-monotonic)
                        if (currentTime > lastUpdateTime && currentNanos > lastUpdateNanos) {
                            lastUpdateTime = currentTime
                            lastUpdateNanos = currentNanos
                            
                            // Check if location has changed (movement detection)
                            val locationChanged = (abs(location.latitude - lastKnownLatitude) > 0.000001 ||
                                                 abs(location.longitude - lastKnownLongitude) > 0.000001)
                            
                            if (locationChanged) {
                                // Calculate real bearing from movement vector
                                currentBearing = calculateBearingFromMovement(lastKnownLatitude, lastKnownLongitude, 
                                                                              location.latitude, location.longitude)
                                isStationary = false
                                lastKnownLatitude = location.latitude
                                lastKnownLongitude = location.longitude
                            } else {
                                // Location is stationary - update bearing rotation if enabled
                                isStationary = true
                                updateBearingRotation(currentTime)
                            }
                            
                            // Create GPS location with monotonic timestamps
                            val gpsLocation = Location(LocationManager.GPS_PROVIDER).apply {
                                latitude = location.latitude
                                longitude = location.longitude
                                accuracy = location.accuracy
                                altitude = location.altitude
                                speed = location.speed
                                bearing = currentBearing
                                time = lastUpdateTime
                                elapsedRealtimeNanos = lastUpdateNanos
                            }
                            
                            // Create Network location with SAME timestamps (critical for consistency)
                            val networkLocation = Location(LocationManager.NETWORK_PROVIDER).apply {
                                latitude = location.latitude + (Random().nextDouble() - 0.5) * 0.00001
                                longitude = location.longitude + (Random().nextDouble() - 0.5) * 0.00001
                                accuracy = location.accuracy + Random().nextFloat() * 2
                                altitude = location.altitude
                                speed = location.speed
                                bearing = currentBearing
                                time = lastUpdateTime  // SAME timestamp
                                elapsedRealtimeNanos = lastUpdateNanos  // SAME timestamp
                            }
                            
                            // Update both providers with synchronized timestamps
                            locationManager?.setTestProviderLocation(LocationManager.GPS_PROVIDER, gpsLocation)
                            locationManager?.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, networkLocation)
                            
                            // Emit location update to LocationBus for notification display
                            serviceScope.launch {
                                LocationBus.emit(gpsLocation.latitude, gpsLocation.longitude, gpsLocation.accuracy)
                            }
                            
                            // Location updated - removed debug log to match DNA behavior
                        } else {
                            Logger.w("LocationService") { "Skipping update - timestamp not monotonic" }
                        }
                        
                    } catch (e: Exception) {
                        Logger.e("LocationService", { "Error updating location" }, e)
                    }
                }
                
                // Schedule next update (every 2 seconds to avoid "blocked - too fast")
                locationUpdateHandler?.postDelayed(this, 2000)
            }
        }
        
        locationUpdateHandler?.post(locationUpdateRunnable!!)
    }
    
    private fun stopLocationUpdates() {
        try {
            // Stop location updates
            locationUpdateRunnable?.let { runnable ->
                locationUpdateHandler?.removeCallbacks(runnable)
            }
            
            // Remove test providers
            try {
                locationManager?.removeTestProvider("gps")
            } catch (_: Exception) {
                // GPS provider removed silently
            }
            
            try {
                locationManager?.removeTestProvider("network")
            } catch (_: Exception) {
                // Network provider removed silently
            }
            
            // Location updates stopped silently
            
        } catch (e: Exception) {
            Log.e("LocationService", "Error stopping location updates", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("SpellCheckingInspection")
            val channel = NotificationChannel(
                CHANNEL_ID,
                "${getString(R.string.app_name)} Service", // NOSONAR - Brand name
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Location management service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val locationText = targetLocation?.let { 
            "Lat: ${String.format(Locale.getDefault(), "%.6f", it.latitude)}, Lng: ${String.format(Locale.getDefault(), "%.6f", it.longitude)}"
        } ?: "Unknown location"
        
        @Suppress("SpellCheckingInspection")
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("${getString(R.string.app_name)} Running") // NOSONAR - Brand name
            .setContentText(locationText)
            .setSmallIcon(R.drawable.ic_location_on)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    /**
     * Auto-restart GPS service with last saved location
     */
    private fun autoRestartGps() {
        try {
            val prefs = getSharedPreferences("location_service_prefs", MODE_PRIVATE)
            val lastLatitude = prefs.getFloat("last_latitude", 0f)
            val lastLongitude = prefs.getFloat("last_longitude", 0f)
            val lastAccuracy = prefs.getFloat("last_accuracy", 2f)
            val wasRunning = prefs.getBoolean("was_running", false)
            
            if (wasRunning && lastLatitude != 0f && lastLongitude != 0f) {
                Logger.d("LocationService") { "Auto-restarting GPS to: $lastLatitude, $lastLongitude" }
                
                // Create target location
                targetLocation = Location("target").apply {
                    latitude = lastLatitude.toDouble()
                    longitude = lastLongitude.toDouble()
                    accuracy = lastAccuracy
                    altitude = 0.0
                    speed = 0f
                    bearing = 0f
                }
                
                // Start location service
                startLocationUpdates()
            } else {
                Logger.d("LocationService") { "No auto-restart needed - GPS was not running" }
            }
        } catch (e: Exception) {
            Logger.e("LocationService", { "Failed to auto-restart GPS: ${e.message}" }, e)
        }
    }
    
    /**
     * Update location for movement simulation
     */
    private fun updateLocation(intent: Intent) {
        val latitude = intent.getDoubleExtra("target_latitude", 0.0)
        val longitude = intent.getDoubleExtra("target_longitude", 0.0)
        val accuracy = intent.getFloatExtra("target_accuracy", 2f)
        
        if (latitude != 0.0 && longitude != 0.0) {
            targetLocation = Location("target").apply {
                this.latitude = latitude
                this.longitude = longitude
                this.accuracy = accuracy
                altitude = 0.0
                speed = 0f
                bearing = 0f
            }
            
            Logger.d("LocationService") { "Location updated for movement simulation: $latitude, $longitude" }
        }
    }
    
    /**
     * Update bearing rotation setting
     */
    private fun updateBearingRotationSetting(intent: Intent) {
        val enabled = intent.getBooleanExtra("bearing_rotation_enabled", true)
        setBearingRotationEnabled(enabled)
    }
    
    /**
     * Save current location state for auto-restart
     */
    private fun saveLocationState() {
        try {
            val prefs = getSharedPreferences("location_service_prefs", MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("was_running", isRunning)
                targetLocation?.let { location ->
                    putFloat("last_latitude", location.latitude.toFloat())
                    putFloat("last_longitude", location.longitude.toFloat())
                    putFloat("last_accuracy", location.accuracy)
                }
                apply()
            }
            Logger.d("LocationService") { "Location state saved for auto-restart" }
        } catch (e: Exception) {
            Logger.e("LocationService", { "Failed to save location state: ${e.message}" }, e)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    /**
     * Calculate bearing from movement vector (for real movement)
     */
    private fun calculateBearingFromMovement(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): Float {
        val lat1 = Math.toRadians(fromLat)
        val lat2 = Math.toRadians(toLat)
        val deltaLng = Math.toRadians(toLng - fromLng)
        
        val y = sin(deltaLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLng)
        
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }
    
    /**
     * Update bearing rotation for stationary locations
     */
    private fun updateBearingRotation(currentTime: Long) {
        if (!isBearingRotationEnabled) return
        
        // Update bearing every 2-5 seconds when stationary
        val timeSinceLastBearingUpdate = currentTime - lastBearingUpdateTime
        if (timeSinceLastBearingUpdate >= 2000) { // 2 seconds minimum
            // Random interval between 2-5 seconds
            val randomInterval = (2000 + Random().nextDouble() * 3000).toLong() // 2-5 seconds
            
            if (timeSinceLastBearingUpdate >= randomInterval) {
                // Generate random bearing (0-360 degrees)
                currentBearing = (Random().nextDouble() * 360).toFloat()
                lastBearingUpdateTime = currentTime
                
                Logger.d("LocationService") { "Bearing rotated to: $currentBearing deg (stationary)" }
            }
        }
    }
    
    /**
     * Set bearing rotation enabled/disabled
     */
    fun setBearingRotationEnabled(enabled: Boolean) {
        isBearingRotationEnabled = enabled
        val prefs = getSharedPreferences("location_prefs", MODE_PRIVATE)
        prefs.edit { putBoolean("bearing_rotation_enabled", enabled) }
        
        Logger.d("LocationService") { "Bearing rotation ${if (enabled) "enabled" else "disabled"}" }
    }
    
    
    override fun onDestroy() {
        super.onDestroy()
        saveLocationState() // Save state before destroying
        stopLocationUpdates()
        isRunning = false
    }
}
