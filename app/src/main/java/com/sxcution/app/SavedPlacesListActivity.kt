package com.sxcution.app

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sxcution.app.adapters.SavedPlacesAdapter
import com.sxcution.app.core.Logger
import com.sxcution.app.data.SimpleSavedPlace
import com.sxcution.app.databinding.ActivitySavedPlacesListBinding
import com.sxcution.app.repository.SimpleSavedPlacesRepository
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

/**
 * Activity for displaying and managing saved places
 */
class SavedPlacesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedPlacesListBinding
    private lateinit var adapter: SavedPlacesAdapter
    private lateinit var repository: SimpleSavedPlacesRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: LatLng? = null
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedPlacesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize components
        repository = SimpleSavedPlacesRepository(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupRecyclerView()
        setupSearch()
        setupButtons()
        getCurrentLocation()
        observeSavedPlaces()
    }

    private fun setupRecyclerView() {
        adapter = SavedPlacesAdapter(
            onSetNowClick = { savedPlace -> setNow(savedPlace) },
            onMoreClick = { savedPlace -> showMoreOptions(savedPlace) },
            onItemClick = { savedPlace -> setNow(savedPlace) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SavedPlacesListActivity)
            adapter = this@SavedPlacesListActivity.adapter
        }

        // Setup swipe to delete
        setupSwipeToDelete()
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // Handle drag to reorder if needed
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val savedPlace = adapter.currentList[position]
                
                AlertDialog.Builder(this@SavedPlacesListActivity)
                    .setTitle("Delete Place")
                    .setMessage("Are you sure you want to delete '${savedPlace.shortName}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            repository.deletePlace(savedPlace)
                            Toast.makeText(this@SavedPlacesListActivity, "Deleted: ${savedPlace.shortName}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Filter adapter based on search text
                // val query = s.toString().lowercase()
                // This would require implementing filtering in the adapter
                // For now, we'll keep it simple
            }
        })
    }

    private fun setupButtons() {
        binding.btnExport.setOnClickListener {
            exportPlaces()
        }

        binding.btnImport.setOnClickListener {
            importPlaces()
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                    adapter.updateCurrentLocation(currentLocation)
                }
            }
        }
    }

    private fun observeSavedPlaces() {
        lifecycleScope.launch {
            repository.savedPlaces.collect { places ->
                adapter.submitList(places)
                binding.emptyState.visibility = if (places.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setNow(savedPlace: SimpleSavedPlace) {
        lifecycleScope.launch {
            try {
                // Update last used timestamp
                repository.updateLastUsed(savedPlace.id)
                
                // Start location service with this place
                val intent = Intent(this@SavedPlacesListActivity, MainActivity::class.java).apply {
                    putExtra("set_location", true)
                    putExtra("target_lat", savedPlace.lat)
                    putExtra("target_lng", savedPlace.lng)
                    putExtra("target_name", savedPlace.shortName)
                }
                startActivity(intent)
                finish()
                
                Toast.makeText(this@SavedPlacesListActivity, "Set to: ${savedPlace.shortName}", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Logger.e("SavedPlacesListActivity", { "Failed to set location: ${e.message}" }, e)
                Toast.makeText(this@SavedPlacesListActivity, "Failed to set location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMoreOptions(savedPlace: SimpleSavedPlace) {
        val options = arrayOf(
            "Rename",
            if (savedPlace.favorited) "Unpin" else "Pin as Favorite",
            "Export Single",
            "Delete"
        )

        AlertDialog.Builder(this)
            .setTitle(savedPlace.shortName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> renamePlace(savedPlace)
                    1 -> toggleFavorite(savedPlace)
                    2 -> exportSinglePlace(savedPlace)
                    3 -> deletePlace(savedPlace)
                }
            }
            .show()
    }

    private fun renamePlace(savedPlace: SimpleSavedPlace) {
        val input = android.widget.EditText(this).apply {
            setText(savedPlace.shortName)
        }

        AlertDialog.Builder(this)
            .setTitle("Rename Place")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        val updated = savedPlace.copy(shortName = newName)
                        repository.saveOrUpdateNearby(
                            LatLng(updated.lat, updated.lng),
                            com.sxcution.app.models.PlaceMeta(
                                shortName = newName,
                                fullName = updated.fullName,
                                address = updated.address,
                                plusCode = updated.plusCode,
                                geohash = updated.geohash
                            ),
                            updated.radiusM
                        )
                        Toast.makeText(this@SavedPlacesListActivity, "Renamed to: $newName", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleFavorite(savedPlace: SimpleSavedPlace) {
        lifecycleScope.launch {
            val newFavoriteStatus = repository.toggleFavorite(savedPlace.id)
            val message = if (newFavoriteStatus) "Pinned as favorite" else "Unpinned"
            Toast.makeText(this@SavedPlacesListActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportSinglePlace(savedPlace: SimpleSavedPlace) {
        lifecycleScope.launch {
            try {
                val json = gson.toJson(savedPlace)
                val file = File(getExternalFilesDir(null), "saved_place_${savedPlace.id}.json")
                FileWriter(file).use { writer -> writer.write(json as String) }
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                    putExtra(Intent.EXTRA_SUBJECT, "Saved Place: ${savedPlace.shortName}")
                }
                startActivity(Intent.createChooser(intent, "Export Place"))
                
            } catch (e: Exception) {
                Logger.e("SavedPlacesListActivity", { "Export failed: ${e.message}" }, e)
                Toast.makeText(this@SavedPlacesListActivity, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deletePlace(savedPlace: SimpleSavedPlace) {
        AlertDialog.Builder(this)
            .setTitle("Delete Place")
            .setMessage("Are you sure you want to delete '${savedPlace.shortName}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    repository.deletePlace(savedPlace)
                    Toast.makeText(this@SavedPlacesListActivity, "Deleted: ${savedPlace.shortName}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportPlaces() {
        lifecycleScope.launch {
            try {
                val json = repository.exportAllPlaces()
                val file = File(getExternalFilesDir(null), "saved_places_export.json")
                FileWriter(file).use { writer -> writer.write(json) }
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                    putExtra(Intent.EXTRA_SUBJECT, "Saved Places Export")
                }
                startActivity(Intent.createChooser(intent, "Export All Places"))
                
            } catch (e: Exception) {
                Logger.e("SavedPlacesListActivity", { "Export failed: ${e.message}" }, e)
                Toast.makeText(this@SavedPlacesListActivity, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importPlaces() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
        }
        @Suppress("DEPRECATION")
        startActivityForResult(Intent.createChooser(intent, "Select JSON file"), IMPORT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == IMPORT_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val json = inputStream?.bufferedReader()?.use { it.readText() }
                    
                    if (json != null) {
                        lifecycleScope.launch {
                            val importedCount = repository.importPlaces(json)
                            Toast.makeText(this@SavedPlacesListActivity, "Imported $importedCount places", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Logger.e("SavedPlacesListActivity", { "Import failed: ${e.message}" }, e)
                    Toast.makeText(this@SavedPlacesListActivity, "Import failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val IMPORT_REQUEST_CODE = 1001
    }
}
