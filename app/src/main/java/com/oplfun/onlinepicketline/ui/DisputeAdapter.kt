package com.onlinepicketline.onlinepicketline.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.onlinepicketline.onlinepicketline.R
import com.onlinepicketline.onlinepicketline.data.model.LaborDispute

/**
 * Adapter for displaying labor disputes in a RecyclerView
 */
class DisputeAdapter : ListAdapter<LaborDispute, DisputeAdapter.DisputeViewHolder>(DiffCallback) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisputeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dispute, parent, false)
        return DisputeViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: DisputeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class DisputeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val companyName: TextView = itemView.findViewById(R.id.company_name)
        private val disputeType: TextView = itemView.findViewById(R.id.dispute_type)
        private val description: TextView = itemView.findViewById(R.id.description)
        private val domains: TextView = itemView.findViewById(R.id.domains)
        
        fun bind(dispute: LaborDispute) {
            companyName.text = dispute.companyName
            disputeType.text = dispute.disputeType
            description.text = dispute.description
            domains.text = "Blocked domains: ${dispute.getAllDomains().joinToString(", ")}"
        }
    }
    
    companion object DiffCallback : DiffUtil.ItemCallback<LaborDispute>() {
        override fun areItemsTheSame(oldItem: LaborDispute, newItem: LaborDispute): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: LaborDispute, newItem: LaborDispute): Boolean {
            return oldItem == newItem
        }
    }
}
