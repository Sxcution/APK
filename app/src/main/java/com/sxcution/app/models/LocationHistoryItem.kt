package com.sxcution.app.models

import java.text.SimpleDateFormat
import java.util.*

data class LocationHistoryItem(
    val id: Long,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
) {
    fun getFormattedTime(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
    
    fun getCoordinates(): String {
        return String.format("%.6f, %.6f", latitude, longitude)
    }
}
