package com.sxcution.app.repository

import android.content.Context
import android.content.SharedPreferences
import com.sxcution.app.data.SavedPlace
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class SavedPlacesRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("saved_places", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val SAVED_PLACES_KEY = "saved_places_list"
    
    fun savePlace(place: SavedPlace) {
        val places = getAllPlaces().toMutableList()
        places.add(place)
        
        val json = gson.toJson(places)
        sharedPreferences.edit().putString(SAVED_PLACES_KEY, json).apply()
    }
    
    fun getAllPlaces(): List<SavedPlace> {
        val json = sharedPreferences.getString(SAVED_PLACES_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<SavedPlace>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
        fun deletePlace(placeId: String) {
        val places = getAllPlaces().toMutableList()
        places.removeAll { it.id == placeId }
        
        val json = gson.toJson(places)
        sharedPreferences.edit().putString(SAVED_PLACES_KEY, json).apply()
    }
    
    fun savePlaces(newPlaces: List<SavedPlace>): Int {
        val places = getAllPlaces().toMutableList()
        val existingIds = places.map { it.id }.toSet()
        var addedCount = 0
        for (place in newPlaces) {
            if (place.id !in existingIds) {
                places.add(place)
                addedCount++
            }
        }
        if (addedCount > 0) {
            val json = gson.toJson(places)
            sharedPreferences.edit().putString(SAVED_PLACES_KEY, json).apply()
        }
        return addedCount
    }
    
    fun createPlace(name: String, latitude: Double, longitude: Double, groupName: String? = null): SavedPlace {
        return SavedPlace(
            id = UUID.randomUUID().toString(),
            name = name,
            latitude = latitude,
            longitude = longitude,
            groupName = groupName
        )
    }

    fun getGroups(): List<String> {
        val json = sharedPreferences.getString("saved_groups_list", null) ?: return listOf("Default")
        val type = object : TypeToken<List<String>>() {}.type
        val groups: List<String> = gson.fromJson(json, type) ?: listOf("Default")
        if ("Default" !in groups) {
            return listOf("Default") + groups
        }
        return groups
    }

    fun addGroup(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val groups = getGroups().toMutableList()
        if (trimmed.lowercase() in groups.map { it.lowercase() }) return false
        groups.add(trimmed)
        val json = gson.toJson(groups)
        sharedPreferences.edit().putString("saved_groups_list", json).apply()
        return true
    }
}
