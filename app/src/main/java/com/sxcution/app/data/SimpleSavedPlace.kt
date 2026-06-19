package com.sxcution.app.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Simple data class for saved places using SharedPreferences
 */
@Parcelize
data class SimpleSavedPlace(
    val id: String,
    val lat: Double,
    val lng: Double,
    val shortName: String,
    val fullName: String? = null,
    val address: String? = null,
    val geohash: String? = null,
    val plusCode: String? = null,
    val radiusM: Int = 20,
    val favorited: Boolean = false,
    val lastUsedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable
