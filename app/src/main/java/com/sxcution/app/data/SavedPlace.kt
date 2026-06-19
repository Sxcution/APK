package com.sxcution.app.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SavedPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val groupName: String? = null
) : Parcelable
