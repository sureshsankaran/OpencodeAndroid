package com.opencode.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for displaying server sessions.
 * Shows server name, URL, connection status, and last connected time.
 */
class ServerListAdapter(
    private val onServerClick: (ServerSession) -> Unit,
    private val onServerLongClick: (ServerSession) -> Unit,
    private val onDeleteClick: (ServerSession) -> Unit
) : ListAdapter<ServerSession, ServerListAdapter.ViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_server_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = getItem(position)
        holder.bind(session, onServerClick, onServerLongClick, onDeleteClick)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serverNameText: TextView = itemView.findViewById(R.id.server_name)
        private val serverUrlText: TextView = itemView.findViewById(R.id.server_url)
        private val statusIndicator: ImageView = itemView.findViewById(R.id.status_indicator)
        private val statusText: TextView = itemView.findViewById(R.id.status_text)
        private val lastConnectedText: TextView = itemView.findViewById(R.id.last_connected)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_server)

        fun bind(
            session: ServerSession,
            onServerClick: (ServerSession) -> Unit,
            onServerLongClick: (ServerSession) -> Unit,
            onDeleteClick: (ServerSession) -> Unit
        ) {
            serverNameText.text = session.displayName
            serverUrlText.text = session.serverUrl
            
            // Update status indicator and text
            when {
                session.isConnected() -> {
                    statusIndicator.setImageResource(R.drawable.status_indicator_connected)
                    statusText.text = itemView.context.getString(R.string.status_connected)
                    statusText.setTextColor(itemView.context.getColor(R.color.status_connected))
                }
                session.isConnecting() -> {
                    statusIndicator.setImageResource(R.drawable.status_indicator_connecting)
                    statusText.text = itemView.context.getString(R.string.status_connecting)
                    statusText.setTextColor(itemView.context.getColor(R.color.status_connecting))
                }
                session.hasError() -> {
                    statusIndicator.setImageResource(R.drawable.status_indicator_disconnected)
                    statusText.text = session.getErrorMessage() ?: itemView.context.getString(R.string.error_connection_failed)
                    statusText.setTextColor(itemView.context.getColor(R.color.error))
                }
                else -> {
                    statusIndicator.setImageResource(R.drawable.status_indicator_disconnected)
                    statusText.text = itemView.context.getString(R.string.status_disconnected)
                    statusText.setTextColor(itemView.context.getColor(R.color.text_hint))
                }
            }
            
            // Format last connected time
            lastConnectedText.text = formatLastConnected(session.lastActiveAt)
            
            itemView.setOnClickListener {
                onServerClick(session)
            }
            
            itemView.setOnLongClickListener {
                onServerLongClick(session)
                true
            }
            
            deleteButton.setOnClickListener {
                onDeleteClick(session)
            }
        }

        private fun formatLastConnected(timestamp: Long): String {
            if (timestamp == 0L) return ""
            
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "${diff / 3600_000}h ago"
                diff < 604800_000 -> "${diff / 86400_000}d ago"
                else -> {
                    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
            }
        }
    }

    private class SessionDiffCallback : DiffUtil.ItemCallback<ServerSession>() {
        override fun areItemsTheSame(oldItem: ServerSession, newItem: ServerSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ServerSession, newItem: ServerSession): Boolean {
            return oldItem.serverUrl == newItem.serverUrl &&
                    oldItem.displayName == newItem.displayName &&
                    oldItem.connectionState == newItem.connectionState &&
                    oldItem.lastActiveAt == newItem.lastActiveAt
        }
    }
}
