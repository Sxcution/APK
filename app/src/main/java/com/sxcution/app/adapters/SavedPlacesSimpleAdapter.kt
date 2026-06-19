package com.sxcution.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sxcution.app.R
import com.sxcution.app.data.SavedPlace
import com.sxcution.app.utils.GeocodingUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedPlacesSimpleAdapter(
    private val onPlaceClick: (SavedPlace) -> Unit,
    private val onDeleteClick: (SavedPlace) -> Unit
) : RecyclerView.Adapter<SavedPlacesSimpleAdapter.ViewHolder>() {

    private var places: List<SavedPlace> = emptyList()

    fun updatePlaces(places: List<SavedPlace>) {
        this.places = places
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_place_simple, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val place = places[position]
        holder.bind(place)
    }

    override fun getItemCount(): Int = places.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPlaceName: TextView = itemView.findViewById(R.id.tv_place_name)
        private val tvPlaceAddress: TextView = itemView.findViewById(R.id.tv_place_address)
        private val btnDelete: Button = itemView.findViewById(R.id.btn_delete_place)

        fun bind(place: SavedPlace) {
            tvPlaceName.text = place.name
            
            // Hiá»ƒn thá»‹ Ä‘á»‹a chá»‰
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val address = withContext(Dispatchers.IO) {
                        GeocodingUtils.getFullAddressForBanner(place.latitude, place.longitude, itemView.context)
                    }
                    tvPlaceAddress.text = address
                } catch (e: Exception) {
                    tvPlaceAddress.text = "Lat: ${String.format("%.6f", place.latitude)}, Lng: ${String.format("%.6f", place.longitude)}"
                }
            }

            itemView.setOnClickListener {
                onPlaceClick(place)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(place)
            }
        }
    }
}
