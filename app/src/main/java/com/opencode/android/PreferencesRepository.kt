package com.opencode.android

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.opencode.android.data.Server
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository for managing app preferences and persistent storage.
 * Handles server configurations, connection history, and user settings.
 */
class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    // ========================================
    // Server Management (New Multi-Server Support)
    // ========================================

    /**
     * Get all saved servers
     */
    var servers: List<Server>
        get() {
            val jsonString = prefs.getString(KEY_SERVERS, null) ?: return emptyList()
            return Server.listFromJson(jsonString)
        }
        set(value) {
            prefs.edit { putString(KEY_SERVERS, Server.listToJson(value)) }
        }

    /**
     * Get a server by ID
     */
    fun getServer(serverId: String): Server? {
        return servers.find { it.id == serverId }
    }

    /**
     * Get a server by URL
     */
    fun getServerByUrl(url: String): Server? {
        return servers.find { it.url == url }
    }

    /**
     * Add or update a server
     */
    fun saveServer(server: Server) {
        val currentServers = servers.toMutableList()
        val existingIndex = currentServers.indexOfFirst { it.id == server.id }
        
        if (existingIndex >= 0) {
            currentServers[existingIndex] = server
        } else {
            currentServers.add(0, server)
        }
        
        servers = currentServers.take(MAX_SERVERS)
    }

    /**
     * Update server's last connected timestamp
     */
    fun updateServerLastConnected(serverId: String) {
        val server = getServer(serverId) ?: return
        val updatedServer = server.copy(lastConnected = System.currentTimeMillis())
        saveServer(updatedServer)
    }

    /**
     * Update server name
     */
    fun updateServerName(serverId: String, newName: String) {
        val server = getServer(serverId) ?: return
        val updatedServer = server.copy(name = newName)
        saveServer(updatedServer)
    }

    /**
     * Delete a server by ID
     */
    fun deleteServer(serverId: String) {
        val currentServers = servers.toMutableList()
        currentServers.removeAll { it.id == serverId }
        servers = currentServers
        
        // Clear active server if it was deleted
        if (activeServerId == serverId) {
            activeServerId = null
        }
    }

    /**
     * Get active server ID (currently selected/viewing server)
     */
    var activeServerId: String?
        get() = prefs.getString(KEY_ACTIVE_SERVER_ID, null)
        set(value) = prefs.edit { putString(KEY_ACTIVE_SERVER_ID, value) }

    /**
     * Get the active server
     */
    fun getActiveServer(): Server? {
        val id = activeServerId ?: return null
        return getServer(id)
    }

    // ========================================
    // Legacy Support (Backward Compatibility)
    // ========================================

    /**
     * Get the last connected server URL (legacy support)
     */
    var lastServerUrl: String?
        get() = prefs.getString(KEY_LAST_SERVER_URL, null)
        set(value) = prefs.edit { putString(KEY_LAST_SERVER_URL, value) }

    /**
     * Get the list of recent server URLs (legacy support)
     */
    var recentServerUrls: List<String>
        get() {
            val urlsString = prefs.getString(KEY_RECENT_URLS, null) ?: return emptyList()
            return urlsString.split(URL_SEPARATOR).filter { it.isNotBlank() }
        }
        set(value) {
            val urlsString = value.take(MAX_RECENT_URLS).joinToString(URL_SEPARATOR)
            prefs.edit { putString(KEY_RECENT_URLS, urlsString) }
        }

    /**
     * Add a URL to the recent servers list (legacy - will migrate to Server objects)
     */
    fun addRecentUrl(url: String) {
        // Check if server already exists
        val existingServer = getServerByUrl(url)
        if (existingServer != null) {
            // Update last connected
            updateServerLastConnected(existingServer.id)
        } else {
            // Create new server from URL
            val newServer = Server.fromUrl(url)
            saveServer(newServer)
        }
        
        // Also maintain legacy list for backward compatibility
        val currentUrls = recentServerUrls.toMutableList()
        currentUrls.remove(url)
        currentUrls.add(0, url)
        recentServerUrls = currentUrls.take(MAX_RECENT_URLS)
        lastServerUrl = url
    }

    /**
     * Remove a URL from recent servers (legacy support)
     */
    fun removeRecentUrl(url: String) {
        // Remove from servers list
        getServerByUrl(url)?.let { server ->
            deleteServer(server.id)
        }
        
        // Also maintain legacy list
        val currentUrls = recentServerUrls.toMutableList()
        currentUrls.remove(url)
        recentServerUrls = currentUrls
        
        if (lastServerUrl == url) {
            lastServerUrl = currentUrls.firstOrNull()
        }
    }

    /**
     * Clear all connection history
     */
    fun clearHistory() {
        prefs.edit {
            remove(KEY_RECENT_URLS)
            remove(KEY_LAST_SERVER_URL)
            remove(KEY_SERVERS)
            remove(KEY_ACTIVE_SERVER_ID)
        }
    }

    /**
     * Migrate legacy URL-only servers to Server objects
     */
    fun migrateFromLegacy() {
        if (servers.isNotEmpty()) return // Already migrated
        
        val legacyUrls = recentServerUrls
        if (legacyUrls.isEmpty()) return
        
        val migratedServers = legacyUrls.map { url ->
            Server.fromUrl(url)
        }
        servers = migratedServers
    }

    // ========================================
    // Active Sessions Management
    // ========================================

    /**
     * Save active sessions to preferences
     */
    fun saveActiveSessions(sessions: List<SessionData>, activeSessionId: String?) {
        val jsonArray = JSONArray()
        sessions.forEach { session ->
            val jsonObj = JSONObject().apply {
                put("id", session.id)
                put("serverUrl", session.serverUrl)
                put("displayName", session.displayName)
                put("createdAt", session.createdAt)
                put("lastActiveAt", session.lastActiveAt)
            }
            jsonArray.put(jsonObj)
        }
        prefs.edit {
            putString(KEY_ACTIVE_SESSIONS, jsonArray.toString())
            putString(KEY_CURRENT_ACTIVE_SESSION, activeSessionId)
        }
    }

    /**
     * Load active sessions from preferences
     * @return Pair of session list and active session ID
     */
    fun loadActiveSessions(): Pair<List<SessionData>, String?> {
        val jsonString = prefs.getString(KEY_ACTIVE_SESSIONS, null)
        val activeSessionId = prefs.getString(KEY_CURRENT_ACTIVE_SESSION, null)
        
        if (jsonString.isNullOrBlank()) {
            return Pair(emptyList(), null)
        }
        
        return try {
            val jsonArray = JSONArray(jsonString)
            val sessions = (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                SessionData(
                    id = obj.getString("id"),
                    serverUrl = obj.getString("serverUrl"),
                    displayName = obj.getString("displayName"),
                    createdAt = obj.getLong("createdAt"),
                    lastActiveAt = obj.getLong("lastActiveAt")
                )
            }
            Pair(sessions, activeSessionId)
        } catch (e: Exception) {
            Pair(emptyList(), null)
        }
    }

    /**
     * Clear all active sessions
     */
    fun clearActiveSessions() {
        prefs.edit {
            remove(KEY_ACTIVE_SESSIONS)
            remove(KEY_CURRENT_ACTIVE_SESSION)
        }
    }

    // ========================================
    // App Settings
    // ========================================

    /**
     * Check if this is the first launch
     */
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit { putBoolean(KEY_FIRST_LAUNCH, value) }

    /**
     * Get auto-reconnect preference
     */
    var autoReconnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RECONNECT, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_RECONNECT, value) }

    companion object {
        private const val PREFS_NAME = "opencode_prefs"
        private const val KEY_LAST_SERVER_URL = "last_server_url"
        private const val KEY_RECENT_URLS = "recent_urls"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect"
        private const val KEY_SERVERS = "servers_json"
        private const val KEY_ACTIVE_SERVER_ID = "active_server_id"
        private const val KEY_ACTIVE_SESSIONS = "active_sessions_json"
        private const val KEY_CURRENT_ACTIVE_SESSION = "current_active_session_id"
        private const val URL_SEPARATOR = "|"
        private const val MAX_RECENT_URLS = 10
        private const val MAX_SERVERS = 20
    }
}
