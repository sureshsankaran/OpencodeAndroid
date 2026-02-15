package com.opencode.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

/**
 * RecyclerView adapter for displaying session tabs in a horizontal list.
 * Shows active sessions with visual indication of the selected session.
 */
class SessionTabAdapter(
    private val onTabClick: (ServerSession) -> Unit,
    private val onCloseClick: (ServerSession) -> Unit
) : ListAdapter<ServerSession, SessionTabAdapter.ViewHolder>(SessionDiffCallback()) {

    private var selectedSessionId: String? = null

    fun setSelectedSession(sessionId: String?) {
        val previousId = selectedSessionId
        selectedSessionId = sessionId
        
        // Notify changes for the previous and new selected items
        currentList.forEachIndexed { index, session ->
            if (session.id == previousId || session.id == sessionId) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session_tab, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = getItem(position)
        val isSelected = session.id == selectedSessionId
        holder.bind(session, isSelected, onTabClick, onCloseClick)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tabCard: MaterialCardView = itemView.findViewById(R.id.session_card)
        private val statusIndicator: View = itemView.findViewById(R.id.session_status_indicator)
        private val sessionName: TextView = itemView.findViewById(R.id.session_name)
        private val sessionStatus: TextView = itemView.findViewById(R.id.session_status)
        private val closeButton: ImageButton = itemView.findViewById(R.id.session_close_button)

        fun bind(
            session: ServerSession,
            isSelected: Boolean,
            onTabClick: (ServerSession) -> Unit,
            onCloseClick: (ServerSession) -> Unit
        ) {
            sessionName.text = session.displayName
            
            // Update status indicator and text
            val context = itemView.context
            val (statusDrawable, statusText) = when {
                session.isConnected() -> R.drawable.status_indicator_connected to context.getString(R.string.status_connected)
                session.isConnecting() -> R.drawable.status_indicator_connecting to context.getString(R.string.status_connecting)
                session.hasError() -> R.drawable.status_indicator_disconnected to (session.getErrorMessage() ?: context.getString(R.string.error_connection_failed))
                else -> R.drawable.status_indicator_disconnected to context.getString(R.string.status_disconnected)
            }
            statusIndicator.setBackgroundResource(statusDrawable)
            sessionStatus.text = statusText
            
            // Update selection state
            if (isSelected) {
                tabCard.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.session_tab_active_stroke)
                tabCard.strokeColor = context.getColor(R.color.primary)
                tabCard.cardElevation = context.resources.getDimension(R.dimen.session_tab_active_elevation)
                sessionName.setTextColor(context.getColor(R.color.text_primary))
            } else {
                tabCard.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.session_tab_inactive_stroke)
                tabCard.strokeColor = context.getColor(R.color.input_border)
                tabCard.cardElevation = context.resources.getDimension(R.dimen.session_tab_inactive_elevation)
                sessionName.setTextColor(context.getColor(R.color.text_secondary))
            }
            
            // Click listeners
            tabCard.setOnClickListener {
                onTabClick(session)
            }
            
            closeButton.setOnClickListener {
                onCloseClick(session)
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
                    oldItem.connectionState == newItem.connectionState
        }
    }
}
