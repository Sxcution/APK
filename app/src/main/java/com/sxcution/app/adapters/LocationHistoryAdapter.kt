package com.sxcution.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sxcution.app.R
import com.sxcution.app.models.LocationHistoryItem

class LocationHistoryAdapter(
    private val items: List<LocationHistoryItem>,
    private val onItemClick: (LocationHistoryItem) -> Unit
) : RecyclerView.Adapter<LocationHistoryAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.text_name)
        val addressTextView: TextView = view.findViewById(R.id.text_address)
        val coordinatesTextView: TextView = view.findViewById(R.id.text_coordinates)
        val timeTextView: TextView = view.findViewById(R.id.text_time)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location_history, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.nameTextView.text = item.name
        holder.addressTextView.text = item.address
        holder.coordinatesTextView.text = item.getCoordinates()
        holder.timeTextView.text = item.getFormattedTime()
        
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }
    
    override fun getItemCount() = items.size
}
