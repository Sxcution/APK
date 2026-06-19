package com.sxcution.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sxcution.app.adapters.FavoriteLocationAdapter
import com.sxcution.app.databinding.ActivityFavoritesBinding
import com.sxcution.app.models.FavoriteLocation

class FavoritesActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var adapter: FavoriteLocationAdapter
    private val favoriteLocations = mutableListOf<FavoriteLocation>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        loadFavorites()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Favorite Locations"
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = FavoriteLocationAdapter(favoriteLocations) { _ ->
            // Handle item click - set as fake location
            // TODO: Implement item click action
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FavoritesActivity)
            adapter = this@FavoritesActivity.adapter
        }
    }
    
    private fun loadFavorites() {
        // TODO: Load from SharedPreferences or database
        // For now, add some sample data
        favoriteLocations.addAll(
            listOf(
                FavoriteLocation(
                    id = 1,
                    name = "Home",
                    address = "123 Main Street, District 1, HCMC",
                    latitude = 10.8231,
                    longitude = 106.6297,
                    isFavorite = true
                ),
                FavoriteLocation(
                    id = 2,
                    name = "Office",
                    address = "456 Business District, District 3, HCMC",
                    latitude = 10.7831,
                    longitude = 106.6897,
                    isFavorite = true
                ),
                FavoriteLocation(
                    id = 3,
                    name = "Gym",
                    address = "789 Fitness Center, District 7, HCMC",
                    latitude = 10.7431,
                    longitude = 106.7297,
                    isFavorite = true
                )
            )
        )
        
        adapter.notifyDataSetChanged()
        
        // Show/hide empty state
        binding.emptyState.visibility = if (favoriteLocations.isEmpty()) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }
}
