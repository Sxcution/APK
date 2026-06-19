package com.sxcution.app

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.sxcution.app.core.Logger
import com.sxcution.app.databinding.ActivityMainBinding
import com.sxcution.app.services.LocationService
import com.sxcution.app.service.ForegroundLocationService
import com.sxcution.app.data.SavedPlace
import com.sxcution.app.repository.SavedPlacesRepository
import com.sxcution.app.adapters.SavedPlacesSimpleAdapter
import com.sxcution.app.dialogs.SavedPlacesDialogFragment
import com.sxcution.app.dialogs.SettingsDialogFragment
import com.sxcution.app.utils.GeocodingUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.launch
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlin.math.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var savedPlacesRepository: SavedPlacesRepository
    private var isLocationServiceRunning = false
    private var currentLocation: Location? = null
    private var targetLocation: Location? = null
    private var targetMarker: Marker? = null
    private var currentZoomLevel = 15f
    private var currentPinLocation: LatLng? = null
    private var selectedPlaceName: String? = null
    
    // Movement simulation variables
    private var isMovementSimulationEnabled = false
    private var movementSimulationHandler: Handler? = null
    private var movementSimulationRunnable: Runnable? = null
    private var movementAngle = 0.0
    private val movementRadius = 0.00005 // ~5 meters in degrees
    private var movementSpeed = 0.1 // radians per update (adjustable)
    private var movementCenterLat = 0.0
    private var movementCenterLng = 0.0
    
    // Zoom with marker setting
    private var isZoomWithMarkerEnabled = false // Default OFF
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val PREFS_NAME = "sxcution_prefs"
        private const val KEY_LAST_LAT = "last_latitude"
        private const val KEY_LAST_LNG = "last_longitude"
        private const val KEY_LAST_ZOOM = "last_zoom"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        savedPlacesRepository = SavedPlacesRepository(this)
        
        // Load zoom with marker setting
        isZoomWithMarkerEnabled = getSharedPreferences("sxcution_settings", Context.MODE_PRIVATE)
            .getBoolean("zoom_with_marker_enabled", false) // Default false
        
        setupViews()
        requestPermissions()
        requestBatteryOptimization()
        
        // Only start location service - no additional services needed
        
        // Setup map ngay láº­p tá»©c (khÃ´ng chá» quyá»n)
        setupMapImmediately()
    }
    
    private fun setupViews() {
        binding.apply {
            btnStartStop.setOnClickListener {
                // Button clicked silently
                if (isLocationServiceRunning) {
                    stopLocationService()
                } else {
                    startLocationService()
                }
            }
            
            btnSmartSetSave.setOnClickListener {
                onSaveButtonClick()
            }
            
            btnSavedPlaces.setOnClickListener {
                onPlacesButtonClick()
            }
            
            btnZoomIn.setOnClickListener {
                zoomIn()
            }
            
            btnZoomOut.setOnClickListener {
                zoomOut()
            }
            
            btnMyLocation.setOnClickListener {
                goToMyLocation()
            }
            
            btnSettings.setOnClickListener {
                showSettingsDialog()
            }
        }
    }
    
    private fun requestPermissions() {
        // Kiá»ƒm tra náº¿u app Ä‘Æ°á»£c cÃ i láº§n Ä‘áº§u hoáº·c permissions bá»‹ reset
        if (isFirstInstallOrPermissionsReset()) {
            // KhÃ´ng hiá»ƒn thá»‹ thÃ´ng bÃ¡o - Ä‘Ã£ cÃ³ dialog Battery Optimization
        }
        
        // BÆ°á»›c 1: Xin quyá»n vá»‹ trÃ­ cÆ¡ báº£n (Location)
        requestLocationPermissions()
    }
    
    private fun requestLocationPermissions() {
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        Dexter.withContext(this)
            .withPermissions(*locationPermissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        // Quyá»n vá»‹ trÃ­ Ä‘Ã£ Ä‘Æ°á»£c cáº¥p, tiáº¿p tá»¥c vá»›i quyá»n thÃ´ng bÃ¡o
                        requestNotificationPermissions()
                        getCurrentLocation()
                        setupMap()
                    } else {
                        Toast.makeText(this@MainActivity, "Location permission is required for app to work", Toast.LENGTH_LONG).show()
                    }
                }
                
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }
    
    private fun requestNotificationPermissions() {
        // BÆ°á»›c 2: Xin quyá»n thÃ´ng bÃ¡o (Notifications)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermissions = arrayOf(
                Manifest.permission.POST_NOTIFICATIONS
            )
            
            Dexter.withContext(this)
                .withPermissions(*notificationPermissions)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.areAllPermissionsGranted()) {
                            // Quyá»n thÃ´ng bÃ¡o Ä‘Ã£ Ä‘Æ°á»£c cáº¥p, tiáº¿p tá»¥c vá»›i quyá»n cháº¡y ná»n
                            requestBackgroundPermissions()
                        } else {
                            // Tiáº¿p tá»¥c vá»›i quyá»n cháº¡y ná»n dÃ¹ thÃ´ng bÃ¡o bá»‹ tá»« chá»‘i
                            requestBackgroundPermissions()
                        }
                    }
                    
                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }
                }).check()
        } else {
            // Android < 13 khÃ´ng cáº§n quyá»n thÃ´ng bÃ¡o, chuyá»ƒn tháº³ng sang quyá»n cháº¡y ná»n
            requestBackgroundPermissions()
        }
    }
    
    private fun requestBackgroundPermissions() {
        // BÆ°á»›c 3: Xin quyá»n cháº¡y ná»n (Background)
        val backgroundPermissions = arrayOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE
        )
        
        Dexter.withContext(this)
            .withPermissions(*backgroundPermissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        // Táº¥t cáº£ quyá»n Ä‘Ã£ Ä‘Æ°á»£c cáº¥p
                        Toast.makeText(this@MainActivity, "All permissions granted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Some permissions denied - app may not work properly", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }
    
    
    /**
     * Kiá»ƒm tra náº¿u app Ä‘Æ°á»£c cÃ i láº§n Ä‘áº§u hoáº·c permissions bá»‹ reset
     */
    private fun isFirstInstallOrPermissionsReset(): Boolean {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasBackgroundPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android < 13 khÃ´ng cáº§n quyá»n nÃ y
        }
        
        return !hasLocationPermission || !hasBackgroundPermission || !hasNotificationPermission
    }
    
    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // Check if already optimized
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    // Directly request ignore battery optimizations - shows system dialog "Let app always run in background?"
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
                
            } catch (e: Exception) {
                Logger.e("MainActivity", { "Failed to request battery optimization: ${e.message}" }, e)
            }
        }
    }
    
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun setupMapImmediately() {
        try {
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            if (mapFragment != null) {
                mapFragment.getMapAsync(this)
                // Map initializing silently
            } else {
                Toast.makeText(this, "Map fragment not found in layout", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing map: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun getCurrentLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    Toast.makeText(this@MainActivity, "Current location found", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(this@MainActivity, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupMap() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
                if (mapFragment != null) {
                    mapFragment.getMapAsync(this)
                } else {
                    Toast.makeText(this, "Map fragment not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error setting up map: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Location permission required to display map", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        try {
            // Set map type
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            
            syncMapLocationDisplay()
            
            // Try to restore last position, if not, try to show real location of the machine
            val restored = restoreLastPosition()
            if (!restored) {
                if (checkLocationPermission()) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val realLatLng = LatLng(it.latitude, it.longitude)
                            currentZoomLevel = 15f
                            targetLocation = Location("target").apply {
                                latitude = it.latitude
                                longitude = it.longitude
                                accuracy = 2f
                            }
                            selectedPlaceName = findSavedPlaceName(realLatLng)
                            setTargetMarker(realLatLng, selectedPlaceName ?: "Current Location")
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(realLatLng, 15f))
                            updateAddressPreview(realLatLng)
                            updateUI()
                        } ?: run {
                            // Fallback via LocationManager to get real device location
                            val locManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                            val backupLocation = try {
                                locManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                    ?: locManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                            } catch (e: SecurityException) { null }
                            
                            backupLocation?.let { backupLoc ->
                                val realLatLng = LatLng(backupLoc.latitude, backupLoc.longitude)
                                currentZoomLevel = 15f
                                targetLocation = Location("target").apply {
                                    latitude = backupLoc.latitude
                                    longitude = backupLoc.longitude
                                    accuracy = 2f
                                }
                                selectedPlaceName = findSavedPlaceName(realLatLng)
                                setTargetMarker(realLatLng, selectedPlaceName ?: "Current Location")
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(realLatLng, 15f))
                                updateAddressPreview(realLatLng)
                                updateUI()
                            } ?: run {
                                // Default fallback to HCMC location
                                val defaultLocation = LatLng(10.8231, 106.6297)
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                                setTargetMarker(defaultLocation, "Ho Chi Minh City")
                                updateUI()
                            }
                        }
                    }
                } else {
                    // Permission not granted yet: default to HCMC location
                    val defaultLocation = LatLng(10.8231, 106.6297)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                    setTargetMarker(defaultLocation, "Ho Chi Minh City")
                    updateUI()
                }
            }
            
            googleMap.setOnMapClickListener { latLng ->
                // Update pin location
                currentPinLocation = latLng
                
                selectedPlaceName = findSavedPlaceName(latLng)
                setTargetMarker(latLng, selectedPlaceName ?: "Selected Location")
                
                // Create Location object from LatLng
                targetLocation = Location("target").apply {
                    latitude = latLng.latitude
                    longitude = latLng.longitude
                    accuracy = 2f
                    altitude = 0.0
                    speed = 0f
                    bearing = 0f
                }
                
                // Keep movement center ready, but do not apply map clicks instantly while service is running.
                if (isMovementSimulationEnabled) {
                    movementCenterLat = latLng.latitude
                    movementCenterLng = latLng.longitude
                    movementAngle = 0.0
                }
                
                // Update address preview
                updateAddressPreview(latLng)
                
                // Update UI
                updateUI()
                
                // Conditional zoom - only zoom if setting is enabled
                if (isZoomWithMarkerEnabled) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, currentZoomLevel))
                }
                
                // Save position
                saveLastPosition(latLng.latitude, latLng.longitude, currentZoomLevel)
            }
            
            // Map loaded silently
            
            // Last position restored above
        } catch (e: Exception) {
            Toast.makeText(this, "Error in onMapReady: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun startLocationService() {
        // Location service started silently
        
        if (targetLocation == null) {
            Toast.makeText(this, "Please select a location on the map first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val targetLatLng = LatLng(targetLocation!!.latitude, targetLocation!!.longitude)
        setTargetMarker(targetLatLng, selectedPlaceName ?: "Selected Location")
        
        // Get address from coordinates using geocoding (only once when starting)
        lifecycleScope.launch {
            try {
                val address = GeocodingUtils.getFullAddressForBanner(
                    targetLocation!!.latitude,
                    targetLocation!!.longitude,
                    this@MainActivity
                )
                
                // Start the main LocationService (fake GPS logic)
                val locationIntent = Intent(this@MainActivity, LocationService::class.java).apply {
                    action = "START_LOCATION_SERVICE"
                    putExtra("target_latitude", targetLocation?.latitude)
                    putExtra("target_longitude", targetLocation?.longitude)
                    putExtra("target_accuracy", targetLocation?.accuracy ?: 2f)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(locationIntent)
                } else {
                    startService(locationIntent)
                }
                
                // Start the ForegroundLocationService (notification display) with address
                val foregroundLocationIntent = Intent(this@MainActivity, ForegroundLocationService::class.java).apply {
                    putExtra("smart_name", address)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(foregroundLocationIntent)
                } else {
                    startService(foregroundLocationIntent)
                }
                
                isLocationServiceRunning = true
                updateUI()
                
                // Service started silently
            } catch (e: Exception) {
                Logger.e("MainActivity", { "Error getting address: ${e.message}" }, e)
                
                // Fallback: start services without address
                val locationIntent = Intent(this@MainActivity, LocationService::class.java).apply {
                    action = "START_LOCATION_SERVICE"
                    putExtra("target_latitude", targetLocation?.latitude)
                    putExtra("target_longitude", targetLocation?.longitude)
                    putExtra("target_accuracy", targetLocation?.accuracy ?: 2f)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(locationIntent)
                } else {
                    startService(locationIntent)
                }
                
                val foregroundLocationIntent = Intent(this@MainActivity, ForegroundLocationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(foregroundLocationIntent)
                } else {
                    startService(foregroundLocationIntent)
                }
                
                isLocationServiceRunning = true
                updateUI()
            }
        }
    }
    
    private fun stopLocationService() {
        // Stop the main LocationService
        val locationIntent = Intent(this, LocationService::class.java).apply {
            action = "STOP_LOCATION_SERVICE"
        }
        startService(locationIntent)
        
        // Stop the ForegroundLocationService
        val foregroundLocationIntent = Intent(this, ForegroundLocationService::class.java).apply {
            action = ForegroundLocationService.ACTION_STOP
        }
        startService(foregroundLocationIntent)
        
        isLocationServiceRunning = false
        updateUI()
        
        // Service stopped silently
    }
    
    private fun updateUI() {
        binding.apply {
            val displayName = if (isLocationServiceRunning) { selectedPlaceName ?: "Selected Location" } else { selectedPlaceName ?: if (targetLocation != null) "Selected Location" else "No Location Selected" }
            tvBannerTitle.text = displayName

            if (isLocationServiceRunning) {
                btnStartStop.text = "Stop"
                btnStartStop.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, android.R.color.holo_red_dark)
                statusText.text = "Location Service Running"
                statusText.setTextColor(android.graphics.Color.parseColor("#00ff00"))
            } else {
                btnStartStop.text = "Start"
                btnStartStop.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.primary_color)
                statusText.text = "Location Service Stopped"
                statusText.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.white))
                statusText.typeface = android.graphics.Typeface.DEFAULT_BOLD
                statusText.textSize = 13f * 1.1f // TÄƒng 10% tá»« 13sp lÃªn 14.3sp
            }
            
            btnStartStop.isEnabled = targetLocation != null
            // Button state updated silently
        }
        syncMapLocationDisplay()
    }

    private fun syncMapLocationDisplay() {
        if (!::googleMap.isInitialized) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = false
        } catch (_: SecurityException) {
            // Permission can be revoked while the activity is alive.
        }

        if (targetLocation != null && targetMarker == null) {
            setTargetMarker(
                LatLng(targetLocation!!.latitude, targetLocation!!.longitude),
                selectedPlaceName ?: "Selected Location"
            )
        }
    }
    
    private fun zoomIn() {
        if (::googleMap.isInitialized) {
            currentZoomLevel = minOf(currentZoomLevel + 1f, 21f)
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoomLevel))
        }
    }
    
    private fun zoomOut() {
        if (::googleMap.isInitialized) {
            currentZoomLevel = maxOf(currentZoomLevel - 1f, 1f)
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoomLevel))
        }
    }
    
    private fun goToMyLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val myLocation = LatLng(it.latitude, it.longitude)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f))
                    currentZoomLevel = 15f
                }
            }
        }
    }
    
    private fun saveLastPosition(lat: Double, lng: Double, zoom: Float) {
        sharedPreferences.edit().apply {
            putFloat(KEY_LAST_LAT, lat.toFloat())
            putFloat(KEY_LAST_LNG, lng.toFloat())
            putFloat(KEY_LAST_ZOOM, zoom)
            apply()
        }
    }
    
    private fun findSavedPlaceName(latLng: LatLng): String? {
        try {
            val places = savedPlacesRepository.getAllPlaces()
            for (place in places) {
                val latDiff = abs(place.latitude - latLng.latitude)
                val lngDiff = abs(place.longitude - latLng.longitude)
                if (latDiff < 0.0001 && lngDiff < 0.0001) {
                    return place.name
                }
            }
        } catch (e: Exception) {
            Logger.e("MainActivity", { "Error finding saved place name: ${e.message}" }, e)
        }
        return null
    }

    private fun restoreLastPosition(): Boolean {
        val lastLat = sharedPreferences.getFloat(KEY_LAST_LAT, 0f)
        val lastLng = sharedPreferences.getFloat(KEY_LAST_LNG, 0f)
        val lastZoom = sharedPreferences.getFloat(KEY_LAST_ZOOM, 15f)
        
        if (lastLat != 0f && lastLng != 0f) {
            val lastPosition = LatLng(lastLat.toDouble(), lastLng.toDouble())
            currentZoomLevel = lastZoom
            
            // Restore target location
            targetLocation = Location("target").apply {
                latitude = lastLat.toDouble()
                longitude = lastLng.toDouble()
                accuracy = 2f
            }
            
            selectedPlaceName = findSavedPlaceName(lastPosition)
            
            setTargetMarker(
                lastPosition,
                selectedPlaceName ?: "Selected Location",
                "Lat: ${String.format("%.6f", lastLat)}, Lng: ${String.format("%.6f", lastLng)}"
            )
            // Move camera to last position
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, lastZoom))
            
            updateUI()
            return true
        }
        return false
    }
    
    override fun onResume() {
        super.onResume()
        // Check if service is running
        isLocationServiceRunning = LocationService.isRunning
        updateUI()
    }
    
    override fun onPause() {
        super.onPause()
        // Don't save position when app goes to background - only save when app is completely closed
    }
        
    /**
     * Smart Set & Save functionality
     */
    private fun onSaveButtonClick() {
        val target = currentPinLocation ?: targetLocation?.let { 
            LatLng(it.latitude, it.longitude) 
        }
        
        if (target == null) {
            Toast.makeText(this, "Please select a location on the map first", Toast.LENGTH_SHORT).show()
            return
        }
        
        showSaveLocationDialog(target)
    }
    
    private fun showSaveLocationDialog(location: LatLng) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_place, null)
        val etPlaceName = dialogView.findViewById<EditText>(R.id.et_place_name)
        val tvAddressPreview = dialogView.findViewById<TextView>(R.id.tv_address_preview)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val spGroupSelect = dialogView.findViewById<android.widget.Spinner>(R.id.sp_group_select)
        val btnAddGroup = dialogView.findViewById<Button>(R.id.btn_add_group)
        
        // Helper to update Group Spinner
        val updateGroupSpinner = { selectedGroupName: String? ->
            val groups = savedPlacesRepository.getGroups()
            val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, groups)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spGroupSelect.adapter = adapter
            
            selectedGroupName?.let {
                val index = groups.indexOf(it)
                if (index != -1) {
                    spGroupSelect.setSelection(index)
                }
            }
        }
        
        // Load initial groups
        updateGroupSpinner(null)
        
        // Add Group click listener
        btnAddGroup.setOnClickListener {
            val inputEdit = EditText(this)
            inputEdit.hint = "Group name"
            inputEdit.setPadding(32, 16, 32, 16)
            
            AlertDialog.Builder(this)
                .setTitle("Add Group")
                .setView(inputEdit)
                .setPositiveButton("Create") { _, _ ->
                    val newGroupName = inputEdit.text.toString().trim()
                    if (newGroupName.isNotEmpty()) {
                        val added = savedPlacesRepository.addGroup(newGroupName)
                        if (added) {
                            updateGroupSpinner(newGroupName)
                            Toast.makeText(this, "Group created: $newGroupName", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Group already exists or is invalid", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        // Hiển thị địa chỉ ngay lập tức
        lifecycleScope.launch {
            try {
                val address = GeocodingUtils.getFullAddressForBanner(location.latitude, location.longitude, this@MainActivity)
                tvAddressPreview.text = address
            } catch (e: Exception) {
                tvAddressPreview.text = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}"
            }
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        btnSave.setOnClickListener {
            val placeName = etPlaceName.text.toString().trim()
            if (placeName.isNotEmpty()) {
                val selectedGroup = spGroupSelect.selectedItem?.toString() ?: "Default"
                val savedPlace = savedPlacesRepository.createPlace(
                    placeName, 
                    location.latitude, 
                    location.longitude,
                    selectedGroup
                )
                savedPlacesRepository.savePlace(savedPlace)
                selectedPlaceName = placeName
                updateUI()
                Toast.makeText(this, "Location saved to $selectedGroup: $placeName", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a location name", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * Update address preview text
     */
    private fun updateAddressPreview(latLng: LatLng) {
        lifecycleScope.launch {
            try {
                val fullAddress = GeocodingUtils.getFullAddressForBanner(
                    latLng.latitude, 
                    latLng.longitude, 
                    this@MainActivity
                )
                binding.addressPreview.text = fullAddress
            } catch (e: Exception) {
                binding.addressPreview.text = "Lat: ${String.format("%.6f", latLng.latitude)}, Lng: ${String.format("%.6f", latLng.longitude)}"
            }
        }
    }

    private fun setTargetMarker(latLng: LatLng, title: String, snippet: String? = null) {
        if (!::googleMap.isInitialized) return
        targetMarker?.remove()
        val markerOptions = MarkerOptions()
            .position(latLng)
            .title(title)
        if (snippet != null) {
            markerOptions.snippet(snippet)
        }
        targetMarker = googleMap.addMarker(markerOptions)
    }

    /**
     * Open saved places list
     */
    private fun onPlacesButtonClick() {
        showSavedPlacesDialog()
    }
    
    private fun showSavedPlacesDialog() {
        // Check if dialog is already showing
        val existingDialog = supportFragmentManager.findFragmentByTag(SavedPlacesDialogFragment.TAG)
        if (existingDialog != null && existingDialog.isVisible) {
            return // Dialog already showing, don't create another one
        }
        
        val dialog = SavedPlacesDialogFragment.newInstance()
        dialog.setOnPlaceSelectedListener(object : SavedPlacesDialogFragment.OnPlaceSelectedListener {
            override fun onPlaceSelected(place: SavedPlace) {
                // Move marker and camera to the selected place
                val latLng = LatLng(place.latitude, place.longitude)
                currentPinLocation = latLng
                
                // Update targetLocation for Start button functionality
                targetLocation = Location("target").apply {
                    latitude = place.latitude
                    longitude = place.longitude
                    accuracy = 2f
                    altitude = 0.0
                    speed = 0f
                    bearing = 0f
                }
                
                selectedPlaceName = place.name
                setTargetMarker(latLng, place.name)
                
                // Move camera to the location
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, currentZoomLevel))
                
                // Update address preview
                updateAddressPreview(latLng)
                
                updateUI()
                
                Toast.makeText(this@MainActivity, "Moved to: ${place.name}", Toast.LENGTH_SHORT).show()
            }
        })
        
        dialog.show(supportFragmentManager, SavedPlacesDialogFragment.TAG)
    }
    /**
     * Start persistent service to prevent app from being paused by Android system
     */
    
    /**
     * Start app pause protection service
     */
    
    /**
     * Show Settings Dialog
     */
    private fun showSettingsDialog() {
        val dialog = SettingsDialogFragment.newInstance()
        dialog.setOnSettingsChangedListener(object : SettingsDialogFragment.OnSettingsChangedListener {
            override fun onMovementSimulationChanged(enabled: Boolean) {
                isMovementSimulationEnabled = enabled
                if (enabled) {
                    startMovementSimulation()
                } else {
                    stopMovementSimulation()
                }
            }

            override fun onMovementSpeedChanged(speed: Float) {
                movementSpeed = speed * 0.1 // Convert to radians per update
            }

            override fun onZoomWithMarkerChanged(enabled: Boolean) {
                isZoomWithMarkerEnabled = enabled
            }
            
            override fun onBearingRotationChanged(enabled: Boolean) {
                // Update bearing rotation setting in LocationService
                val intent = Intent(this@MainActivity, LocationService::class.java).apply {
                    action = "UPDATE_BEARING_ROTATION"
                    putExtra("bearing_rotation_enabled", enabled)
                }
                startService(intent)
            }

            override fun onLanguageChanged(language: String) {
                // Language switching disabled
            }
        })
        
        dialog.show(supportFragmentManager, SettingsDialogFragment.TAG)
    }

    /**
     * Start circular movement simulation
     */
    private fun startMovementSimulation() {
        if (targetLocation == null) {
            Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show()
            return
        }

        movementCenterLat = targetLocation!!.latitude
        movementCenterLng = targetLocation!!.longitude
        movementAngle = 0.0

        if (isLocationServiceRunning) {
            val locationIntent = Intent(this, LocationService::class.java).apply {
                action = "UPDATE_LOCATION"
                putExtra("target_latitude", movementCenterLat)
                putExtra("target_longitude", movementCenterLng)
                putExtra("target_accuracy", 2f)
            }
            startService(locationIntent)
        }

        movementSimulationHandler = Handler(Looper.getMainLooper())
        movementSimulationRunnable = object : Runnable {
            override fun run() {
                if (isMovementSimulationEnabled) {
                    val newLat = movementCenterLat + movementRadius * cos(movementAngle)
                    val newLng = movementCenterLng + movementRadius * sin(movementAngle)

                    targetLocation = Location("target").apply {
                        latitude = newLat
                        longitude = newLng
                        accuracy = 2f
                        altitude = 0.0
                        speed = 0f
                        bearing = 0f
                    }

                    if (isLocationServiceRunning) {
                        val locationIntent = Intent(this@MainActivity, LocationService::class.java).apply {
                            action = "UPDATE_LOCATION"
                            putExtra("target_latitude", newLat)
                            putExtra("target_longitude", newLng)
                            putExtra("target_accuracy", 2f)
                        }
                        startService(locationIntent)
                    }

                    movementAngle += movementSpeed
                    if (movementAngle >= 2 * PI) {
                        movementAngle = 0.0
                    }

                    movementSimulationHandler?.postDelayed(this, 1000)
                }
            }
        }

        movementSimulationHandler?.post(movementSimulationRunnable!!)
        Toast.makeText(this, "Movement simulation started", Toast.LENGTH_SHORT).show()
    }

    /**
     * Stop circular movement simulation
     */
    private fun stopMovementSimulation(showToast: Boolean = true) {
        movementSimulationRunnable?.let { runnable ->
            movementSimulationHandler?.removeCallbacks(runnable)
        }
        movementSimulationHandler = null
        movementSimulationRunnable = null
        if (showToast) {
            Toast.makeText(this, "Movement simulation stopped", Toast.LENGTH_SHORT).show()
        }
    }
    
    
    override fun onDestroy() {
        super.onDestroy()
        stopMovementSimulation(showToast = false)
        
        // Save current position only when app is completely destroyed
        if (::googleMap.isInitialized && targetLocation != null) {
            saveLastPosition(
                targetLocation!!.latitude,
                targetLocation!!.longitude,
                currentZoomLevel
            )
        }
    }
    
}
