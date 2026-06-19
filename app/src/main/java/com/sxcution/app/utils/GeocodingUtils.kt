package com.sxcution.app.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.sxcution.app.core.Logger
import com.sxcution.app.models.PlaceMeta
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

/**
 * Utility class for reverse geocoding and smart naming
 */
object GeocodingUtils {
    
    /**
     * Perform smart reverse geocoding to get place metadata
     */
    suspend fun reverseGeocodeSmart(
        lat: Double, 
        lng: Double, 
        context: Context
    ): PlaceMeta = withContext(Dispatchers.IO) {
        
        Logger.d("GeocodingUtils") { "Reverse geocoding for lat=$lat, lng=$lng" }
        
        // Try Android Geocoder first
        val geocoderResult = tryGeocoder(lat, lng, context)
        if (geocoderResult != null) {
            Logger.d("GeocodingUtils") { "Geocoder success: ${geocoderResult.shortName}" }
            return@withContext geocoderResult
        }
        
        // Fallback to coordinate-based naming
        val fallbackResult = createFallbackMeta(lat, lng)
        Logger.d("GeocodingUtils") { "Using fallback: ${fallbackResult.shortName}" }
        return@withContext fallbackResult
    }
    
    /**
     * Try Android Geocoder for reverse geocoding
     */
    private suspend fun tryGeocoder(lat: Double, lng: Double, context: Context): PlaceMeta? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            Logger.w("GeocodingUtils") { "Geocoder not present" }
            return@withContext null
        }
        
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val shortName = Naming.shortFromGeocoder(address)
                val fullName = buildFullName(address)
                val addressString = buildAddressString(address)
                val plusCode = Naming.generatePlusCode(lat, lng)
                val geohash = generateGeohash(lat, lng, 8)
                
                return@withContext PlaceMeta(
                    shortName = shortName,
                    fullName = fullName,
                    address = addressString,
                    plusCode = plusCode,
                    geohash = geohash
                )
            }
        } catch (e: IOException) {
            Logger.e("GeocodingUtils", { "Geocoder IOException: ${e.message}" }, e)
        } catch (e: Exception) {
            Logger.e("GeocodingUtils", { "Geocoder error: ${e.message}" }, e)
        }
        
        return@withContext null
    }
    
    /**
     * Create fallback metadata when geocoding fails
     */
    private fun createFallbackMeta(lat: Double, lng: Double): PlaceMeta {
        val shortName = Naming.fallbackShort(lat, lng)
        val plusCode = Naming.generatePlusCode(lat, lng)
        val geohash = generateGeohash(lat, lng, 8)
        
        return PlaceMeta(
            shortName = shortName,
            fullName = null,
            address = null,
            plusCode = plusCode,
            geohash = geohash
        )
    }
    
    /**
     * Build full name from address components
     */
    private fun buildFullName(address: Address): String? {
        val parts = mutableListOf<String>()
        
        address.featureName?.let { if (it.isNotBlank()) parts.add(it) }
        address.thoroughfare?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }
        address.subLocality?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }
        address.locality?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }
        address.adminArea?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }
        address.countryName?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }
        
        return if (parts.isNotEmpty()) parts.joinToString(", ") else null
    }
    
    /**
     * Get full address for banner display
     * Uses getAddressLine(0) which provides the complete formatted address
     */
    suspend fun getFullAddressForBanner(lat: Double, lng: Double, context: Context): String = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            return@withContext "${"%.6f".format(lat)}, ${"%.6f".format(lng)}"
        }
        
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // Use getAddressLine(0) which provides the complete formatted address
                val fullAddress = address.getAddressLine(0)
                if (!fullAddress.isNullOrBlank()) {
                    return@withContext fullAddress
                }
                
                // Fallback to buildAddressString if getAddressLine(0) is empty
                val addressString = buildAddressString(address)
                if (!addressString.isNullOrBlank()) {
                    return@withContext addressString
                }
            }
        } catch (e: IOException) {
            Logger.e("GeocodingUtils", { "Geocoder IOException: ${e.message}" }, e)
        } catch (e: Exception) {
            Logger.e("GeocodingUtils", { "Geocoder Exception: ${e.message}" }, e)
        }
        
        // Final fallback to coordinates
        return@withContext "${"%.6f".format(lat)}, ${"%.6f".format(lng)}"
    }
    
    /**
     * Build address string from address components
     * Avoids using subThoroughfare (street number) as primary component
     */
    private fun buildAddressString(address: Address): String? {
        val parts = mutableListOf<String>()
        
        // Start with thoroughfare (street name) instead of subThoroughfare (street number)
        address.thoroughfare?.let { if (it.isNotBlank()) parts.add(it) }
        // Add street number after street name if available
        address.subThoroughfare?.let { if (it.isNotBlank()) parts.add(it) }
        address.subLocality?.let { if (it.isNotBlank()) parts.add(it) }
        address.locality?.let { if (it.isNotBlank()) parts.add(it) }
        address.adminArea?.let { if (it.isNotBlank()) parts.add(it) }
        address.countryName?.let { if (it.isNotBlank()) parts.add(it) }
        
        return if (parts.isNotEmpty()) parts.joinToString(", ") else null
    }
    
    /**
     * Generate geohash (simplified implementation)
     */
    private fun generateGeohash(lat: Double, lng: Double, precision: Int): String {
        val chars = "0123456789bcdefghjkmnpqrstuvwxyz"
        var hash = ""
        var bit = 0
        var ch = 0
        var even = true
        var latMin = -90.0
        var latMax = 90.0
        var lngMin = -180.0
        var lngMax = 180.0
        
        while (hash.length < precision) {
            if (even) {
                val lngMid = (lngMin + lngMax) / 2
                if (lng >= lngMid) {
                    ch = ch or (1 shl (4 - bit))
                    lngMin = lngMid
                } else {
                    lngMax = lngMid
                }
            } else {
                val latMid = (latMin + latMax) / 2
                if (lat >= latMid) {
                    ch = ch or (1 shl (4 - bit))
                    latMin = latMid
                } else {
                    latMax = latMid
                }
            }
            
            even = !even
            
            if (bit < 4) {
                bit++
            } else {
                hash += chars[ch]
                bit = 0
                ch = 0
            }
        }
        
        return hash
    }
}
