package com.sxcution.app.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sxcution.app.R
import com.sxcution.app.adapters.SavedPlacesSimpleAdapter
import com.sxcution.app.data.SavedPlace
import com.sxcution.app.repository.SavedPlacesRepository
import com.google.android.gms.maps.model.LatLng

class SavedPlacesDialogFragment : DialogFragment() {
    
    companion object {
        const val TAG = "SavedPlacesDialogFragment"
        
        fun newInstance(): SavedPlacesDialogFragment {
            return SavedPlacesDialogFragment()
        }
    }
    
    private lateinit var repository: SavedPlacesRepository
    private lateinit var adapter: SavedPlacesSimpleAdapter
    private lateinit var rvSavedPlaces: RecyclerView
    private lateinit var paginationControls: LinearLayout
    private lateinit var btnPrevPage: Button
    private lateinit var btnNextPage: Button
    private lateinit var tvPageInfo: TextView
    private lateinit var btnClose: Button
    
    private var allPlaces: List<SavedPlace> = emptyList()
    private var currentPage = 0
    private val placesPerPage = 5
    
    // Callback interface
    interface OnPlaceSelectedListener {
        fun onPlaceSelected(place: SavedPlace)
    }
    
    private var placeSelectedListener: OnPlaceSelectedListener? = null
    
    fun setOnPlaceSelectedListener(listener: OnPlaceSelectedListener) {
        this.placeSelectedListener = listener
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = SavedPlacesRepository(requireContext())
        // Set dialog style for proper dimensions
        setStyle(STYLE_NORMAL, R.style.CustomDialog)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_saved_places, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        rvSavedPlaces = view.findViewById(R.id.rv_saved_places)
        paginationControls = view.findViewById(R.id.pagination_controls)
        btnPrevPage = view.findViewById(R.id.btn_prev_page)
        btnNextPage = view.findViewById(R.id.btn_next_page)
        tvPageInfo = view.findViewById(R.id.tv_page_info)
        btnClose = view.findViewById(R.id.btn_close_places)
        
        setupRecyclerView()
        setupPagination()
        setupCloseButton()
        
        // Load data
        loadPlaces()
    }
    
    override fun onStart() {
        super.onStart()
        // Set dialog window dimensions
        dialog?.window?.let { window ->
            val params = window.attributes
            params.width = (resources.displayMetrics.widthPixels * 0.8).toInt()
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = params
        }
    }
    
    private fun setupRecyclerView() {
        adapter = SavedPlacesSimpleAdapter(
            onPlaceClick = { place ->
                placeSelectedListener?.onPlaceSelected(place)
                dismiss()
            },
            onDeleteClick = { place ->
                showDeleteConfirmation(place)
            }
        )
        
        rvSavedPlaces.layoutManager = LinearLayoutManager(requireContext())
        rvSavedPlaces.adapter = adapter
    }
    
    private fun setupPagination() {
        btnPrevPage.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                updatePage()
            }
        }
        
        btnNextPage.setOnClickListener {
            val totalPages = if (allPlaces.isEmpty()) 1 else ((allPlaces.size - 1) / placesPerPage) + 1
            if (currentPage < totalPages - 1) {
                currentPage++
                updatePage()
            }
        }
    }
    
    private fun setupCloseButton() {
        btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    private fun loadPlaces() {
        allPlaces = repository.getAllPlaces()
        currentPage = 0
        updatePage()
    }
    
    private fun updatePage() {
        val totalPages = if (allPlaces.isEmpty()) 1 else ((allPlaces.size - 1) / placesPerPage) + 1
        val startIndex = currentPage * placesPerPage
        val endIndex = minOf(startIndex + placesPerPage, allPlaces.size)
        val pagePlaces = allPlaces.subList(startIndex, endIndex)
        
        adapter.updatePlaces(pagePlaces)
        tvPageInfo.text = "${currentPage + 1} / $totalPages"
        
        // Show/hide pagination controls
        paginationControls.visibility = if (totalPages > 1) View.VISIBLE else View.GONE
        
        // Enable/disable navigation buttons
        btnPrevPage.isEnabled = currentPage > 0
        btnNextPage.isEnabled = currentPage < totalPages - 1
    }
    
    private fun showDeleteConfirmation(place: SavedPlace) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Location")
            .setMessage("Delete this point?")
            .setPositiveButton("Delete") { _, _ ->
                repository.deletePlace(place.id)
                // Update the list in place instead of recreating dialog
                loadPlaces()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Clean up any resources if needed
    }
}
