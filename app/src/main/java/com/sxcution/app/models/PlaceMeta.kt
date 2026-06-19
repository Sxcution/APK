package com.sxcution.app.models

/**
 * Metadata for a place including smart naming information
 */
data class PlaceMeta(
    val shortName: String,
    val fullName: String?,
    val address: String?,
    val plusCode: String?,
    val geohash: String?
)
