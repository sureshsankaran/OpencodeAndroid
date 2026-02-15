package com.opencode.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

/**
 * RecyclerView adapter for displaying active server sessions as tabs.
 */
class ActiveSessionsAdapter(
    private val onSessionClick: (ServerSession) -> Unit,
    private val onSessionClose: (ServerSession) -> Unit
) : ListAdapter<ServerSession, ActiveSessionsAdapter.ViewHolder>(SessionDiffCallback()) {

    private var activeSessionId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session_tab, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = getItem(position)
        holder.bind(session, session.id == activeSessionId, onSessionClick, onSessionClose)
    }

    /**
     * Update the currently active session
     */
    fun setActiveSession(sessionId: String?) {
        val previousActiveId = activeSessionId
        activeSessionId = sessionId
        
        // Find and update the previously active item
        currentList.forEachIndexed { index, session ->
            if (session.id == previousActiveId || session.id == sessionId) {
                notifyItemChanged(index)
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sessionCard: MaterialCardView = itemView.findViewById(R.id.session_card)
        private val sessionName: TextView = itemView.findViewById(R.id.session_name)
        private val sessionStatus: TextView = itemView.findViewById(R.id.session_status)
        private val statusIndicator: View = itemView.findViewById(R.id.session_status_indicator)
        private val closeButton: ImageButton = itemView.findViewById(R.id.session_close_button)

        fun bind(
            session: ServerSession,
            isActive: Boolean,
            onSessionClick: (ServerSession) -> Unit,
            onSessionClose: (ServerSession) -> Unit
        ) {
            sessionName.text = session.displayName
            
            // Update status text and indicator
            val context = itemView.context
            when (session.connectionState) {
                is ServerSession.ConnectionState.Connected -> {
                    sessionStatus.text = context.getString(R.string.status_connected)
                    statusIndicator.setBackgroundResource(R.drawable.status_indicator_connected)
                }
                is ServerSession.ConnectionState.Connecting -> {
                    sessionStatus.text = context.getString(R.string.status_connecting)
                    statusIndicator.setBackgroundResource(R.drawable.status_indicator_connecting)
                }
                is ServerSession.ConnectionState.Error -> {
                    sessionStatus.text = context.getString(R.string.status_error)
                    statusIndicator.setBackgroundResource(R.drawable.status_indicator_disconnected)
                }
                is ServerSession.ConnectionState.Disconnected -> {
                    sessionStatus.text = context.getString(R.string.status_disconnected)
                    statusIndicator.setBackgroundResource(R.drawable.status_indicator_disconnected)
                }
            }
            
            // Update card appearance based on active state
            if (isActive) {
                sessionCard.strokeColor = ContextCompat.getColor(context, R.color.primary)
                sessionCard.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.session_tab_active_stroke)
                sessionCard.cardElevation = context.resources.getDimension(R.dimen.session_tab_active_elevation)
            } else {
                sessionCard.strokeColor = ContextCompat.getColor(context, R.color.input_border)
                sessionCard.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.session_tab_inactive_stroke)
                sessionCard.cardElevation = context.resources.getDimension(R.dimen.session_tab_inactive_elevation)
            }
            
            // Set click listeners
            sessionCard.setOnClickListener {
                onSessionClick(session)
            }
            
            closeButton.setOnClickListener {
                onSessionClose(session)
            }
        }
    }

    private class SessionDiffCallback : DiffUtil.ItemCallback<ServerSession>() {
        override fun areItemsTheSame(oldItem: ServerSession, newItem: ServerSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ServerSession, newItem: ServerSession): Boolean {
            return oldItem.id == newItem.id &&
                   oldItem.serverUrl == newItem.serverUrl &&
                   oldItem.displayName == newItem.displayName &&
                   oldItem.connectionState::class == newItem.connectionState::class
        }
    }
}
