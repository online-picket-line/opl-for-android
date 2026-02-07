package com.onlinepicketline.opl.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.onlinepicketline.opl.R
import com.onlinepicketline.opl.data.model.Geofence

/**
 * Adapter for displaying nearby strike/geofence items in a RecyclerView.
 */
class GeofenceAdapter : ListAdapter<Geofence, GeofenceAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_geofence, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val employerName: TextView = itemView.findViewById(R.id.employerName)
        private val actionType: TextView = itemView.findViewById(R.id.actionType)
        private val location: TextView = itemView.findViewById(R.id.location)
        private val distance: TextView = itemView.findViewById(R.id.distance)
        private val description: TextView = itemView.findViewById(R.id.description)

        fun bind(geofence: Geofence) {
            employerName.text = geofence.employerName
            actionType.text = geofence.actionType.replaceFirstChar { it.uppercase() }
            location.text = geofence.location ?: "Unknown location"
            distance.text = formatDistance(geofence.distance)
            description.text = geofence.description ?: ""
            description.visibility = if (geofence.description.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        private fun formatDistance(meters: Int): String {
            return if (meters < 1000) {
                "$meters m away"
            } else {
                val miles = meters / 1609.34
                String.format("%.1f mi away", miles)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Geofence>() {
            override fun areItemsTheSame(oldItem: Geofence, newItem: Geofence) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Geofence, newItem: Geofence) =
                oldItem == newItem
        }
    }
}
