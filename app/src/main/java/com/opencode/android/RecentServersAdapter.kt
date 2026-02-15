package com.opencode.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for displaying recent server URLs.
 */
class RecentServersAdapter(
    private val onServerClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<String, RecentServersAdapter.ViewHolder>(ServerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_server, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val serverUrl = getItem(position)
        holder.bind(serverUrl, onServerClick, onDeleteClick)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serverUrlText: TextView = itemView.findViewById(R.id.recent_server_url)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_recent_server)

        fun bind(
            serverUrl: String,
            onServerClick: (String) -> Unit,
            onDeleteClick: (String) -> Unit
        ) {
            serverUrlText.text = serverUrl
            
            itemView.setOnClickListener {
                onServerClick(serverUrl)
            }
            
            deleteButton.setOnClickListener {
                onDeleteClick(serverUrl)
            }
        }
    }

    private class ServerDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}
