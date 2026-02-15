package com.opencode.android

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages multiple server sessions, allowing users to connect to multiple
 * OpenCode servers simultaneously and switch between them.
 */
class SessionManager(private val preferencesRepository: PreferencesRepository) {

    /**
     * All active sessions (connected or recently used)
     */
    private val _sessions = MutableStateFlow<List<ServerSession>>(emptyList())
    val sessions: StateFlow<List<ServerSession>> = _sessions.asStateFlow()

    /**
     * Currently active/visible session
     */
    private val _activeSession = MutableStateFlow<ServerSession?>(null)
    val activeSession: StateFlow<ServerSession?> = _activeSession.asStateFlow()

    /**
     * Listeners for session events
     */
    private val listeners = mutableListOf<SessionListener>()

    init {
        // Load persisted sessions on initialization
        loadPersistedSessions()
    }

    /**
     * Create a new session for a server URL
     * @return The created session, or existing session if URL already has one
     */
    fun createSession(serverUrl: String): ServerSession {
        // Check if session for this URL already exists
        val existingSession = _sessions.value.find { it.serverUrl == serverUrl }
        if (existingSession != null) {
            setActiveSession(existingSession.id)
            return existingSession
        }

        // Check max sessions limit
        if (_sessions.value.size >= ServerSession.MAX_SESSIONS) {
            // Remove oldest inactive session
            val oldestInactive = _sessions.value
                .filter { !it.connectionState.isActive() }
                .minByOrNull { it.lastActiveAt }
            
            if (oldestInactive != null) {
                removeSession(oldestInactive.id)
            } else {
                // All sessions are active, remove the oldest one
                val oldest = _sessions.value.minByOrNull { it.lastActiveAt }
                if (oldest != null) {
                    removeSession(oldest.id)
                }
            }
        }

        // Create new session
        val newSession = ServerSession(serverUrl = serverUrl)
        val updatedSessions = _sessions.value + newSession
        _sessions.value = updatedSessions
        
        // Set as active
        _activeSession.value = newSession
        
        // Persist
        persistSessions()
        
        // Notify listeners
        listeners.forEach { it.onSessionCreated(newSession) }
        listeners.forEach { it.onActiveSessionChanged(newSession) }
        
        return newSession
    }

    /**
     * Get a session by its ID
     */
    fun getSession(sessionId: String): ServerSession? {
        return _sessions.value.find { it.id == sessionId }
    }

    /**
     * Get a session by server URL
     */
    fun getSessionByUrl(serverUrl: String): ServerSession? {
        return _sessions.value.find { it.serverUrl == serverUrl }
    }

    /**
     * Set the currently active session
     */
    fun setActiveSession(sessionId: String) {
        val session = _sessions.value.find { it.id == sessionId }
        if (session != null && session != _activeSession.value) {
            // Update last active timestamp for previous session
            _activeSession.value?.touch()
            
            // Set new active session
            session.touch()
            _activeSession.value = session
            
            // Persist the change
            persistSessions()
            
            // Notify listeners
            listeners.forEach { it.onActiveSessionChanged(session) }
        }
    }

    /**
     * Update session connection state
     */
    fun updateSessionState(sessionId: String, state: ServerSession.ConnectionState) {
        val sessions = _sessions.value.toMutableList()
        val index = sessions.indexOfFirst { it.id == sessionId }
        
        if (index != -1) {
            val session = sessions[index]
            val oldState = session.connectionState
            session.connectionState = state
            session.touch()
            
            _sessions.value = sessions
            
            // Update active session reference if needed
            if (_activeSession.value?.id == sessionId) {
                _activeSession.value = session
            }
            
            // Notify listeners
            listeners.forEach { it.onSessionStateChanged(session, oldState, state) }
        }
    }

    /**
     * Remove a session
     */
    fun removeSession(sessionId: String) {
        val session = _sessions.value.find { it.id == sessionId } ?: return
        
        val updatedSessions = _sessions.value.filter { it.id != sessionId }
        _sessions.value = updatedSessions
        
        // If this was the active session, switch to another
        if (_activeSession.value?.id == sessionId) {
            val nextSession = updatedSessions.maxByOrNull { it.lastActiveAt }
            _activeSession.value = nextSession
            nextSession?.let {
                listeners.forEach { listener -> listener.onActiveSessionChanged(it) }
            }
        }
        
        // Persist
        persistSessions()
        
        // Notify listeners
        listeners.forEach { it.onSessionRemoved(session) }
    }

    /**
     * Remove all sessions
     */
    fun removeAllSessions() {
        val currentSessions = _sessions.value.toList()
        _sessions.value = emptyList()
        _activeSession.value = null
        
        persistSessions()
        
        currentSessions.forEach { session ->
            listeners.forEach { it.onSessionRemoved(session) }
        }
    }

    /**
     * Get the number of active (connected) sessions
     */
    fun getActiveSessionCount(): Int {
        return _sessions.value.count { it.connectionState.isActive() }
    }

    /**
     * Check if there's room for more sessions
     */
    fun canCreateNewSession(): Boolean {
        return _sessions.value.size < ServerSession.MAX_SESSIONS ||
               _sessions.value.any { !it.connectionState.isActive() }
    }

    /**
     * Add a listener for session events
     */
    fun addListener(listener: SessionListener) {
        listeners.add(listener)
    }

    /**
     * Remove a listener
     */
    fun removeListener(listener: SessionListener) {
        listeners.remove(listener)
    }

    /**
     * Persist sessions to preferences
     */
    private fun persistSessions() {
        val sessionDataList = _sessions.value.map { SessionData.fromSession(it) }
        preferencesRepository.saveActiveSessions(sessionDataList, _activeSession.value?.id)
    }

    /**
     * Load persisted sessions from preferences
     */
    private fun loadPersistedSessions() {
        val (sessionDataList, activeSessionId) = preferencesRepository.loadActiveSessions()
        
        val loadedSessions = sessionDataList.map { it.toSession() }
        _sessions.value = loadedSessions
        
        // Restore active session
        _activeSession.value = if (activeSessionId != null) {
            loadedSessions.find { it.id == activeSessionId }
        } else {
            loadedSessions.maxByOrNull { it.lastActiveAt }
        }
    }

    /**
     * Listener interface for session events
     */
    interface SessionListener {
        fun onSessionCreated(session: ServerSession)
        fun onSessionRemoved(session: ServerSession)
        fun onSessionStateChanged(
            session: ServerSession,
            oldState: ServerSession.ConnectionState,
            newState: ServerSession.ConnectionState
        )
        fun onActiveSessionChanged(session: ServerSession)
    }

    /**
     * Empty implementation of SessionListener for convenience
     */
    open class SimpleSessionListener : SessionListener {
        override fun onSessionCreated(session: ServerSession) {}
        override fun onSessionRemoved(session: ServerSession) {}
        override fun onSessionStateChanged(
            session: ServerSession,
            oldState: ServerSession.ConnectionState,
            newState: ServerSession.ConnectionState
        ) {}
        override fun onActiveSessionChanged(session: ServerSession) {}
    }
}
