package com.sxcution.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sxcution.app.R
import com.sxcution.app.data.SimpleSavedPlace
import com.google.maps.android.SphericalUtil
import com.google.android.gms.maps.model.LatLng

/**
 * Adapter for displaying saved places in RecyclerView
 */
class SavedPlacesAdapter(
    private val onSetNowClick: (SimpleSavedPlace) -> Unit,
    private val onMoreClick: (SimpleSavedPlace) -> Unit,
    private val onItemClick: (SimpleSavedPlace) -> Unit
) : ListAdapter<SimpleSavedPlace, SavedPlacesAdapter.SavedPlaceViewHolder>(SavedPlaceDiffCallback()) {

    private var currentLocation: LatLng? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedPlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_place, parent, false)
        return SavedPlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedPlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateCurrentLocation(location: LatLng?) {
        currentLocation = location
        notifyDataSetChanged()
    }

    inner class SavedPlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.title_text)
        private val subtitleText: TextView = itemView.findViewById(R.id.subtitle_text)
        private val distanceText: TextView = itemView.findViewById(R.id.distance_text)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.favorite_icon)
        private val setNowButton: ImageButton = itemView.findViewById(R.id.btn_set_now)
        private val moreButton: ImageButton = itemView.findViewById(R.id.btn_more)

        fun bind(savedPlace: SimpleSavedPlace) {
            titleText.text = savedPlace.shortName
            subtitleText.text = savedPlace.address ?: "Lat: ${String.format("%.6f", savedPlace.lat)}, Lng: ${String.format("%.6f", savedPlace.lng)}"
            
            // Show/hide favorite icon
            favoriteIcon.visibility = if (savedPlace.favorited) View.VISIBLE else View.GONE
            
            // Calculate and show distance if current location is available
            currentLocation?.let { current ->
                val distance = SphericalUtil.computeDistanceBetween(
                    current,
                    LatLng(savedPlace.lat, savedPlace.lng)
                )
                val distanceKm = distance / 1000
                distanceText.text = if (distanceKm < 1) {
                    "${distance.toInt()}m"
                } else {
                    "${String.format("%.1f", distanceKm)}km"
                }
                distanceText.visibility = View.VISIBLE
            } ?: run {
                distanceText.visibility = View.GONE
            }

            // Set click listeners
            setNowButton.setOnClickListener {
                onSetNowClick(savedPlace)
            }

            moreButton.setOnClickListener {
                onMoreClick(savedPlace)
            }

            itemView.setOnClickListener {
                onItemClick(savedPlace)
            }
        }
    }

    class SavedPlaceDiffCallback : DiffUtil.ItemCallback<SimpleSavedPlace>() {
        override fun areItemsTheSame(oldItem: SimpleSavedPlace, newItem: SimpleSavedPlace): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SimpleSavedPlace, newItem: SimpleSavedPlace): Boolean {
            return oldItem == newItem
        }
    }
}
