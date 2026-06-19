package com.sxcution.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sxcution.app.R
import com.sxcution.app.models.FavoriteLocation

class FavoriteLocationAdapter(
    private val items: List<FavoriteLocation>,
    private val onItemClick: (FavoriteLocation) -> Unit
) : RecyclerView.Adapter<FavoriteLocationAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.text_name)
        val addressTextView: TextView = view.findViewById(R.id.text_address)
        val coordinatesTextView: TextView = view.findViewById(R.id.text_coordinates)
        val favoriteIcon: ImageView = view.findViewById(R.id.icon_favorite)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_location, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.nameTextView.text = item.name
        holder.addressTextView.text = item.address
        holder.coordinatesTextView.text = item.getCoordinates()
        
        // Set favorite icon
        holder.favoriteIcon.setImageResource(
            if (item.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )
        
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }
    
    override fun getItemCount() = items.size
}
