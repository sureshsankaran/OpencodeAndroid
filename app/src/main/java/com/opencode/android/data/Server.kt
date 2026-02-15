package com.opencode.android.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Data class representing a server configuration.
 * 
 * @property id Unique identifier for the server
 * @property name User-friendly display name for the server
 * @property url The server URL
 * @property lastConnected Timestamp of last successful connection (milliseconds)
 * @property createdAt Timestamp when the server was added (milliseconds)
 */
data class Server(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val lastConnected: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Returns the display name, falling back to URL hostname if name is empty
     */
    fun getDisplayName(): String {
        return name.ifBlank { 
            try {
                java.net.URL(url).host
            } catch (e: Exception) {
                url
            }
        }
    }

    /**
     * Serialize to JSON
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put(KEY_ID, id)
            put(KEY_NAME, name)
            put(KEY_URL, url)
            put(KEY_LAST_CONNECTED, lastConnected)
            put(KEY_CREATED_AT, createdAt)
        }
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_URL = "url"
        private const val KEY_LAST_CONNECTED = "lastConnected"
        private const val KEY_CREATED_AT = "createdAt"

        /**
         * Deserialize from JSON
         */
        fun fromJson(json: JSONObject): Server {
            return Server(
                id = json.optString(KEY_ID, UUID.randomUUID().toString()),
                name = json.optString(KEY_NAME, ""),
                url = json.getString(KEY_URL),
                lastConnected = json.optLong(KEY_LAST_CONNECTED, 0L),
                createdAt = json.optLong(KEY_CREATED_AT, System.currentTimeMillis())
            )
        }

        /**
         * Serialize a list of servers to JSON string
         */
        fun listToJson(servers: List<Server>): String {
            val jsonArray = JSONArray()
            servers.forEach { jsonArray.put(it.toJson()) }
            return jsonArray.toString()
        }

        /**
         * Deserialize a list of servers from JSON string
         */
        fun listFromJson(jsonString: String): List<Server> {
            if (jsonString.isBlank()) return emptyList()
            return try {
                val jsonArray = JSONArray(jsonString)
                (0 until jsonArray.length()).map { i ->
                    fromJson(jsonArray.getJSONObject(i))
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        /**
         * Create a Server from just a URL (for backward compatibility)
         */
        fun fromUrl(url: String): Server {
            return Server(
                name = "",
                url = url
            )
        }
    }
}
