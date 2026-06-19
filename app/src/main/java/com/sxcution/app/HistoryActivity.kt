package com.sxcution.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sxcution.app.adapters.LocationHistoryAdapter
import com.sxcution.app.databinding.ActivityHistoryBinding
import com.sxcution.app.models.LocationHistoryItem

class HistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: LocationHistoryAdapter
    private val locationHistory = mutableListOf<LocationHistoryItem>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        loadHistory()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Location History"
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = LocationHistoryAdapter(locationHistory) { _ ->
            // Handle item click - could open map or set as fake location
            // TODO: Implement item click action
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = this@HistoryActivity.adapter
        }
    }
    
    private fun loadHistory() {
        // TODO: Load from SharedPreferences or database
        // For now, add some sample data
        locationHistory.addAll(
            listOf(
                LocationHistoryItem(
                    id = 1,
                    name = "Ho Chi Minh City",
                    address = "District 1, Ho Chi Minh City, Vietnam",
                    latitude = 10.8231,
                    longitude = 106.6297,
                    timestamp = System.currentTimeMillis() - 86400000
                ),
                LocationHistoryItem(
                    id = 2,
                    name = "Hanoi",
                    address = "Ba Dinh District, Hanoi, Vietnam",
                    latitude = 21.0285,
                    longitude = 105.8542,
                    timestamp = System.currentTimeMillis() - 172800000
                ),
                LocationHistoryItem(
                    id = 3,
                    name = "Da Nang",
                    address = "Hai Chau District, Da Nang, Vietnam",
                    latitude = 16.0544,
                    longitude = 108.2022,
                    timestamp = System.currentTimeMillis() - 259200000
                )
            )
        )
        
        adapter.notifyDataSetChanged()
        
        // Show/hide empty state
        binding.emptyState.visibility = if (locationHistory.isEmpty()) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }
}
