package com.opencode.android

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebStorage

/**
 * Application class for OpenCode Android.
 * Initializes app-wide configurations and manages WebView settings.
 */
class OpenCodeApplication : Application() {

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var connectionManager: ConnectionManager
        private set

    lateinit var sessionManager: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize repositories
        preferencesRepository = PreferencesRepository(this)
        connectionManager = ConnectionManager(preferencesRepository)
        sessionManager = SessionManager(preferencesRepository)
        
        // Migrate legacy servers if needed
        preferencesRepository.migrateFromLegacy()
        
        // Configure WebView cookies to persist across sessions
        configureCookieManager()
    }

    private fun configureCookieManager() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        // Note: setAcceptThirdPartyCookies requires a WebView instance, 
        // so we'll configure this per-WebView in MainActivity
    }

    /**
     * Clear all WebView data (for testing or user-requested reset)
     */
    fun clearWebViewData() {
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()
    }

    companion object {
        lateinit var instance: OpenCodeApplication
            private set
    }
}
