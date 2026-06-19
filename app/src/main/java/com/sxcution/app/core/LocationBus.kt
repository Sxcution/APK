package com.sxcution.app.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * In-process communication bus for location updates.
 * Replaces external broadcasts with internal Flow-based messaging.
 */
object LocationBus {
    
    /**
     * Internal mutable flow for emitting location updates
     */
    private val _locationTicks = MutableSharedFlow<LocationUpdate>(
        replay = 0, 
        extraBufferCapacity = 1
    )
    
    /**
     * Public read-only flow for collecting location updates
     */
    val locationTicks: SharedFlow<LocationUpdate> = _locationTicks
    
    /**
     * Emit a location update to the bus
     * 
     * @param lat Latitude
     * @param lng Longitude  
     * @param acc Accuracy (optional)
     */
    suspend fun emit(lat: Double, lng: Double, acc: Float? = null) {
        _locationTicks.emit(LocationUpdate(lat, lng, acc))
    }
    
    /**
     * Data class representing a location update
     */
    data class LocationUpdate(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float? = null
    )
}
