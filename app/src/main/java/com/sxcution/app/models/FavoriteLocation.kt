package com.sxcution.app.models

data class FavoriteLocation(
    val id: Long,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean
) {
    fun getCoordinates(): String {
        return String.format("%.6f, %.6f", latitude, longitude)
    }
}
