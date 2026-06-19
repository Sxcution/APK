package com.sxcution.app.repository

import android.content.Context
import android.content.SharedPreferences
import com.sxcution.app.core.Logger
import com.sxcution.app.data.SimpleSavedPlace
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Simple repository using SharedPreferences for saved places
 */
class SimpleSavedPlacesRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("saved_places", Context.MODE_PRIVATE)
    private val _savedPlaces = MutableStateFlow<List<SimpleSavedPlace>>(emptyList())
    val savedPlaces: StateFlow<List<SimpleSavedPlace>> = _savedPlaces.asStateFlow()
    
    private val MAX_SAVED_PLACES = 20
    private val DEDUPLICATION_DISTANCE_M = 25.0
    
    init {
        loadSavedPlaces()
    }
    
    suspend fun smartSave(target: LatLng): SimpleSavedPlace {
        val meta = com.sxcution.app.utils.GeocodingUtils.reverseGeocodeSmart(target.latitude, target.longitude, context)
        return saveOrUpdateNearby(target, meta, radiusM = 20)
    }
    
    suspend fun saveOrUpdateNearby(target: LatLng, meta: com.sxcution.app.models.PlaceMeta, radiusM: Int): SimpleSavedPlace {
        val all = getAllPlaces()
        val near = all.minByOrNull {
            SphericalUtil.computeDistanceBetween(LatLng(it.lat, it.lng), target)
        }
        
        return if (near != null && SphericalUtil.computeDistanceBetween(LatLng(near.lat, near.lng), target) <= DEDUPLICATION_DISTANCE_M) {
            Logger.d("SimpleSavedPlacesRepo") { "Updating existing place: ${near.shortName}" }
            val updated = near.copy(
                shortName = chooseBetterShort(near.shortName, meta.shortName),
                fullName = meta.fullName ?: near.fullName,
                address = meta.address ?: near.address,
                lastUsedAt = System.currentTimeMillis()
            )
            savePlace(updated)
            updated
        } else {
            Logger.d("SimpleSavedPlacesRepo") { "Inserting new place: ${meta.shortName}" }
            val fresh = SimpleSavedPlace(
                id = UUID.randomUUID().toString(),
                lat = target.latitude, 
                lng = target.longitude, 
                shortName = meta.shortName,
                fullName = meta.fullName, 
                address = meta.address,
                geohash = meta.geohash, 
                plusCode = meta.plusCode, 
                radiusM = radiusM
            )
            savePlace(fresh)
            maybeEvictIfOverLimit()
            fresh
        }
    }
    
    suspend fun remove(item: SimpleSavedPlace) {
        val places = getAllPlaces().toMutableList()
        places.removeAll { it.id == item.id }
        saveAllPlaces(places)
        _savedPlaces.value = places
    }
    
    suspend fun upsert(item: SimpleSavedPlace) {
        savePlace(item)
    }
    
    private fun savePlace(place: SimpleSavedPlace) {
        val places = getAllPlaces().toMutableList()
        val existingIndex = places.indexOfFirst { it.id == place.id }
        if (existingIndex >= 0) {
            places[existingIndex] = place
        } else {
            places.add(place)
        }
        saveAllPlaces(places)
        _savedPlaces.value = places
    }
    
    fun getAllPlaces(): List<SimpleSavedPlace> {
        val jsonString = prefs.getString("places", "[]") ?: "[]"
        return try {
            val jsonArray = JSONArray(jsonString)
            val places = mutableListOf<SimpleSavedPlace>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                places.add(SimpleSavedPlace(
                    id = jsonObject.getString("id"),
                    lat = jsonObject.getDouble("lat"),
                    lng = jsonObject.getDouble("lng"),
                    shortName = jsonObject.getString("shortName"),
                    fullName = jsonObject.optString("fullName").takeIf { it.isNotEmpty() },
                    address = jsonObject.optString("address").takeIf { it.isNotEmpty() },
                    geohash = jsonObject.optString("geohash").takeIf { it.isNotEmpty() },
                    plusCode = jsonObject.optString("plusCode").takeIf { it.isNotEmpty() },
                    radiusM = jsonObject.optInt("radiusM", 20),
                    favorited = jsonObject.optBoolean("favorited", false),
                    lastUsedAt = jsonObject.optLong("lastUsedAt", System.currentTimeMillis()),
                    createdAt = jsonObject.optLong("createdAt", System.currentTimeMillis())
                ))
            }
            places.sortedWith(compareByDescending<SimpleSavedPlace> { it.favorited }.thenByDescending { it.lastUsedAt })
        } catch (e: Exception) {
            Logger.e("SimpleSavedPlacesRepo", { "Error loading places: ${e.message}" }, e)
            emptyList()
        }
    }
    
    private fun saveAllPlaces(places: List<SimpleSavedPlace>) {
        val jsonArray = JSONArray()
        places.forEach { place ->
            val jsonObject = JSONObject().apply {
                put("id", place.id)
                put("lat", place.lat)
                put("lng", place.lng)
                put("shortName", place.shortName)
                put("fullName", place.fullName ?: "")
                put("address", place.address ?: "")
                put("geohash", place.geohash ?: "")
                put("plusCode", place.plusCode ?: "")
                put("radiusM", place.radiusM)
                put("favorited", place.favorited)
                put("lastUsedAt", place.lastUsedAt)
                put("createdAt", place.createdAt)
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString("places", jsonArray.toString()).apply()
    }
    
    private fun loadSavedPlaces() {
        _savedPlaces.value = getAllPlaces()
    }
    
    private suspend fun maybeEvictIfOverLimit(max: Int = MAX_SAVED_PLACES) {
        val currentPlaces = getAllPlaces()
        if (currentPlaces.size > max) {
            val nonFavorited = currentPlaces.filter { !it.favorited }
                .sortedBy { it.lastUsedAt } // LRU
            if (nonFavorited.isNotEmpty()) {
                val toEvict = nonFavorited.first()
                Logger.d("SimpleSavedPlacesRepo") { "Evicting LRU non-favorited: ${toEvict.shortName}" }
                remove(toEvict)
            } else {
                Logger.w("SimpleSavedPlacesRepo") { "Cannot evict: all places are favorited and limit exceeded." }
            }
        }
    }
    
    private fun chooseBetterShort(existing: String, new: String): String {
        return if (new.length > existing.length && !new.contains("Lat") && !new.contains("Lng")) new else existing
    }
    
    suspend fun updateLastUsed(id: String) {
        val places = getAllPlaces().toMutableList()
        val index = places.indexOfFirst { it.id == id }
        if (index >= 0) {
            places[index] = places[index].copy(lastUsedAt = System.currentTimeMillis())
            saveAllPlaces(places)
            _savedPlaces.value = places
        }
    }
    
    suspend fun toggleFavorite(id: String): Boolean {
        val places = getAllPlaces().toMutableList()
        val index = places.indexOfFirst { it.id == id }
        if (index >= 0) {
            val newFavoriteStatus = !places[index].favorited
            places[index] = places[index].copy(favorited = newFavoriteStatus)
            saveAllPlaces(places)
            _savedPlaces.value = places
            return newFavoriteStatus
        }
        return false
    }
    
    suspend fun importPlaces(json: String): Int {
        return try {
            val gson = com.google.gson.Gson()
            val places = gson.fromJson(json, Array<SimpleSavedPlace>::class.java).toList()
            val currentPlaces = getAllPlaces().toMutableList()
            var importedCount = 0
            
            places.forEach { importedPlace ->
                val existingIndex = currentPlaces.indexOfFirst { it.id == importedPlace.id }
                if (existingIndex >= 0) {
                    currentPlaces[existingIndex] = importedPlace
                } else {
                    currentPlaces.add(importedPlace)
                }
                importedCount++
            }
            
            saveAllPlaces(currentPlaces)
            _savedPlaces.value = currentPlaces
            importedCount
        } catch (e: Exception) {
            Logger.e("SimpleSavedPlacesRepo", { "Import error: ${e.message}" }, e)
            0
        }
    }
    
    suspend fun deletePlace(place: SimpleSavedPlace) {
        remove(place)
    }
    
    suspend fun exportAllPlaces(): String {
        val places = getAllPlaces()
        val gson = com.google.gson.Gson()
        return gson.toJson(places)
    }
}
