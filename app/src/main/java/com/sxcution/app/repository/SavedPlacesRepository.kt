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
    
    fun createPlace(name: String, latitude: Double, longitude: Double): SavedPlace {
        return SavedPlace(
            id = UUID.randomUUID().toString(),
            name = name,
            latitude = latitude,
            longitude = longitude
        )
    }
}
