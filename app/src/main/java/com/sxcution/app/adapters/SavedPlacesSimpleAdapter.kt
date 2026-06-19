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
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class AdapterItem {
        data class Header(val groupName: String) : AdapterItem()
        data class Place(val place: SavedPlace) : AdapterItem()
    }

    private var adapterItems: List<AdapterItem> = emptyList()

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_PLACE = 1
    }

    fun updatePlaces(places: List<SavedPlace>) {
        // Group places by groupName (fallback to "Default" if null)
        val grouped = places.groupBy { it.groupName ?: "Default" }
        
        // Sort groups alphabetically/numerically
        val sortedGroupNames = grouped.keys.sortedWith(compareBy { it.lowercase() })
        
        val items = mutableListOf<AdapterItem>()
        for (groupName in sortedGroupNames) {
            items.add(AdapterItem.Header(groupName))
            // Sort places inside the group alphabetically
            val groupPlaces = grouped[groupName]?.sortedBy { it.name.lowercase() } ?: emptyList()
            for (place in groupPlaces) {
                items.add(AdapterItem.Place(place))
            }
        }
        this.adapterItems = items
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (adapterItems[position]) {
            is AdapterItem.Header -> VIEW_TYPE_HEADER
            is AdapterItem.Place -> VIEW_TYPE_PLACE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_group_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_saved_place_simple, parent, false)
            PlaceViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = adapterItems[position]) {
            is AdapterItem.Header -> (holder as HeaderViewHolder).bind(item.groupName)
            is AdapterItem.Place -> (holder as PlaceViewHolder).bind(item.place)
        }
    }

    override fun getItemCount(): Int = adapterItems.size

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvGroupTitle: TextView = itemView.findViewById(R.id.tv_group_title)
        fun bind(groupName: String) {
            tvGroupTitle.text = groupName
        }
    }

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPlaceName: TextView = itemView.findViewById(R.id.tv_place_name)
        private val tvPlaceAddress: TextView = itemView.findViewById(R.id.tv_place_address)
        private val btnDelete: Button = itemView.findViewById(R.id.btn_delete_place)

        fun bind(place: SavedPlace) {
            tvPlaceName.text = place.name
            
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
