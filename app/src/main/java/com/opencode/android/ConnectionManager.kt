package com.opencode.android

import android.util.Patterns
import java.net.MalformedURLException
import java.net.URL

/**
 * Manages connection state and server URL handling.
 * Now supports session-aware connection management for multi-server support.
 */
class ConnectionManager(private val preferencesRepository: PreferencesRepository) {

    /**
     * Current connection state (for backward compatibility)
     */
    var connectionState: ConnectionState = ConnectionState.Disconnected
        private set

    /**
     * Currently connected server URL (for backward compatibility)
     */
    var currentServerUrl: String? = null
        private set

    /**
     * Currently active session ID
     */
    var currentSessionId: String? = null
        private set

    /**
     * Listeners for connection state changes
     */
    private val listeners = mutableListOf<ConnectionStateListener>()

    /**
     * Validate a server URL
     * @return ValidationResult with success/failure and optional error message
     */
    fun validateUrl(url: String): ValidationResult {
        if (url.isBlank()) {
            return ValidationResult(false, "Please enter a server URL")
        }

        // Add protocol if missing
        val normalizedUrl = normalizeUrl(url)

        // Check URL format
        if (!Patterns.WEB_URL.matcher(normalizedUrl).matches()) {
            return ValidationResult(false, "Please enter a valid URL")
        }

        // Try to parse URL
        return try {
            val parsedUrl = URL(normalizedUrl)
            if (parsedUrl.protocol != "http" && parsedUrl.protocol != "https") {
                ValidationResult(false, "URL must use http or https protocol")
            } else {
                ValidationResult(true, null, normalizedUrl)
            }
        } catch (e: MalformedURLException) {
            ValidationResult(false, "Invalid URL format")
        }
    }

    /**
     * Normalize URL by adding protocol if missing
     */
    fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("localhost") || trimmed.startsWith("127.0.0.1") -> "http://$trimmed"
            trimmed.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+.*")) -> "http://$trimmed"
            else -> "https://$trimmed"
        }
    }

    /**
     * Mark connection as started
     */
    fun onConnectionStarted(url: String) {
        currentServerUrl = url
        updateState(ConnectionState.Connecting)
    }

    /**
     * Mark connection as started for a specific session
     */
    fun onSessionConnectionStarted(sessionId: String, url: String) {
        currentSessionId = sessionId
        currentServerUrl = url
        updateState(ConnectionState.Connecting, sessionId)
    }

    /**
     * Mark connection as successful
     */
    fun onConnectionSuccess(url: String) {
        currentServerUrl = url
        preferencesRepository.addRecentUrl(url)
        updateState(ConnectionState.Connected)
    }

    /**
     * Mark session connection as successful
     */
    fun onSessionConnectionSuccess(sessionId: String, url: String) {
        currentSessionId = sessionId
        currentServerUrl = url
        preferencesRepository.addRecentUrl(url)
        updateState(ConnectionState.Connected, sessionId)
    }

    /**
     * Mark connection as failed
     */
    fun onConnectionFailed(error: String? = null) {
        updateState(ConnectionState.Error(error ?: "Connection failed"))
    }

    /**
     * Mark session connection as failed
     */
    fun onSessionConnectionFailed(sessionId: String, error: String? = null) {
        updateState(ConnectionState.Error(error ?: "Connection failed"), sessionId)
    }

    /**
     * Mark as disconnected
     */
    fun onDisconnected() {
        updateState(ConnectionState.Disconnected)
    }

    /**
     * Mark session as disconnected
     */
    fun onSessionDisconnected(sessionId: String) {
        if (currentSessionId == sessionId) {
            currentSessionId = null
        }
        updateState(ConnectionState.Disconnected, sessionId)
    }

    /**
     * Switch to a different session
     */
    fun switchToSession(sessionId: String, url: String) {
        currentSessionId = sessionId
        currentServerUrl = url
        listeners.forEach { it.onSessionSwitched(sessionId, url) }
    }

    /**
     * Get last connected URL for auto-fill
     */
    fun getLastServerUrl(): String? = preferencesRepository.lastServerUrl

    /**
     * Get recent server URLs
     */
    fun getRecentUrls(): List<String> = preferencesRepository.recentServerUrls

    /**
     * Clear connection history
     */
    fun clearHistory() {
        preferencesRepository.clearHistory()
    }

    /**
     * Add a listener for connection state changes
     */
    fun addListener(listener: ConnectionStateListener) {
        listeners.add(listener)
    }

    /**
     * Remove a listener
     */
    fun removeListener(listener: ConnectionStateListener) {
        listeners.remove(listener)
    }

    private fun updateState(newState: ConnectionState, sessionId: String? = null) {
        val oldState = connectionState
        connectionState = newState
        listeners.forEach { it.onConnectionStateChanged(oldState, newState, sessionId) }
    }

    /**
     * Connection state enum
     */
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()

        fun toSessionState(): ServerSession.ConnectionState {
            return when (this) {
                is Disconnected -> ServerSession.ConnectionState.Disconnected
                is Connecting -> ServerSession.ConnectionState.Connecting
                is Connected -> ServerSession.ConnectionState.Connected
                is Error -> ServerSession.ConnectionState.Error(this.message)
            }
        }
    }

    /**
     * URL validation result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val normalizedUrl: String? = null
    )

    /**
     * Listener interface for connection state changes
     */
    interface ConnectionStateListener {
        fun onConnectionStateChanged(
            oldState: ConnectionState,
            newState: ConnectionState,
            sessionId: String? = null
        )

        /**
         * Called when switching to a different session
         */
        fun onSessionSwitched(sessionId: String, url: String) {}
    }
}
