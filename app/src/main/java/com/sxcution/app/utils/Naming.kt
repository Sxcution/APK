package com.sxcution.app.utils

import android.location.Address
import java.text.Normalizer
import java.util.Locale

/**
 * Utility class for generating smart names from geocoding data
 */
object Naming {
    
    private const val MAX_SHORT_NAME_LENGTH = 28
    
    /**
     * Generate a short name from Android Geocoder Address
     */
    fun shortFromGeocoder(address: Address): String {
        val parts = mutableListOf<String>()
        
        // Prefer featureName or thoroughfare
        address.featureName?.let { if (it.isNotBlank()) parts.add(it) }
        if (parts.isEmpty()) {
            address.thoroughfare?.let { if (it.isNotBlank()) parts.add(it) }
        }
        
        // Add subLocality if available
        address.subLocality?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }
        
        // Add locality if available
        address.locality?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }
        
        // If still empty, try admin area
        if (parts.isEmpty()) {
            address.adminArea?.let { if (it.isNotBlank()) parts.add(it) }
        }
        
        val result = parts.joinToString(", ")
        return if (result.isBlank()) {
            "Unknown Location"
        } else {
            truncateToLength(result, MAX_SHORT_NAME_LENGTH)
        }
    }
    
    /**
     * Generate fallback short name from coordinates
     */
    fun fallbackShort(lat: Double, lng: Double): String {
        val geo8 = generateGeohash(lat, lng, 8)
        return "Lat %.6f, Lng %.6f ($geo8)".format(locale = Locale.US, lat, lng)
    }
    
    /**
     * Generate a simple geohash (basic implementation)
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
    
    /**
     * Generate Plus Code (simplified implementation)
     */
    fun generatePlusCode(lat: Double, lng: Double): String {
        // Simplified Plus Code generation
        // In a real implementation, you'd use the Open Location Code library
        val geo8 = generateGeohash(lat, lng, 8)
        return geo8.uppercase()
    }
    
    /**
     * Normalize text for sorting (remove diacritics)
     */
    fun normalizeForSorting(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
            .lowercase()
    }
    
    /**
     * Truncate string to specified length
     */
    private fun truncateToLength(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) {
            text
        } else {
            text.substring(0, maxLength - 3) + "..."
        }
    }
    
    /**
     * Choose the better short name between two options
     */
    fun chooseBetterShort(existing: String, new: String): String {
        return when {
            new == "Unknown Location" -> existing
            existing == "Unknown Location" -> new
            new.length < existing.length -> new
            else -> existing
        }
    }
}
