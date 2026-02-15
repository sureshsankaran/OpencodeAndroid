package com.opencode.android

import android.os.Bundle
import java.util.UUID

/**
 * Represents a server session with its connection state and WebView state.
 * Each session corresponds to a connection to a different OpenCode server.
 */
data class ServerSession(
    /** Unique identifier for this session */
    val id: String = UUID.randomUUID().toString(),
    
    /** Server URL for this session */
    val serverUrl: String,
    
    /** Display name for the session (derived from URL or custom) */
    val displayName: String = extractDisplayName(serverUrl),
    
    /** Current connection state */
    var connectionState: ConnectionState = ConnectionState.Disconnected,
    
    /** Timestamp when session was created */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** Timestamp of last activity */
    var lastActiveAt: Long = System.currentTimeMillis(),
    
    /** WebView state bundle for session restoration */
    var webViewState: Bundle? = null
) {
    /**
     * Connection states for a session
     */
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
        
        fun isActive(): Boolean = this is Connecting || this is Connected
    }
    
    /**
     * Update the last active timestamp
     */
    fun touch() {
        lastActiveAt = System.currentTimeMillis()
    }
    
    /**
     * Check if session is currently connected
     */
    fun isConnected(): Boolean = connectionState is ConnectionState.Connected
    
    /**
     * Check if session is currently connecting
     */
    fun isConnecting(): Boolean = connectionState is ConnectionState.Connecting
    
    /**
     * Check if session has an error
     */
    fun hasError(): Boolean = connectionState is ConnectionState.Error
    
    /**
     * Get error message if in error state
     */
    fun getErrorMessage(): String? = (connectionState as? ConnectionState.Error)?.message
    
    companion object {
        /**
         * Extract a display name from a URL
         */
        fun extractDisplayName(url: String): String {
            return try {
                val cleanUrl = url.trim()
                    .removePrefix("http://")
                    .removePrefix("https://")
                    .removeSuffix("/")
                
                // Get host:port part
                val hostPart = cleanUrl.split("/").first()
                
                // Shorten common patterns
                when {
                    hostPart.startsWith("localhost") -> "localhost" + 
                        (if (hostPart.contains(":")) ":${hostPart.substringAfter(":")}" else "")
                    hostPart.startsWith("127.0.0.1") -> "localhost" +
                        (if (hostPart.contains(":")) ":${hostPart.substringAfter(":")}" else "")
                    hostPart.startsWith("192.168.") || hostPart.startsWith("10.") -> hostPart
                    else -> {
                        // For domain names, use the main domain
                        val parts = hostPart.split(".")
                        if (parts.size >= 2) {
                            "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
                        } else {
                            hostPart
                        }
                    }
                }
            } catch (e: Exception) {
                url.take(20)
            }
        }
        
        /**
         * Maximum number of concurrent sessions allowed
         */
        const val MAX_SESSIONS = 5
    }
}

/**
 * Serializable representation of a session for persistence
 */
data class SessionData(
    val id: String,
    val serverUrl: String,
    val displayName: String,
    val createdAt: Long,
    val lastActiveAt: Long
) {
    fun toSession(): ServerSession {
        return ServerSession(
            id = id,
            serverUrl = serverUrl,
            displayName = displayName,
            createdAt = createdAt,
            lastActiveAt = lastActiveAt
        )
    }
    
    companion object {
        fun fromSession(session: ServerSession): SessionData {
            return SessionData(
                id = session.id,
                serverUrl = session.serverUrl,
                displayName = session.displayName,
                createdAt = session.createdAt,
                lastActiveAt = session.lastActiveAt
            )
        }
    }
}
