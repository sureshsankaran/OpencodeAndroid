package com.opencode.android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Main activity for the OpenCode Android app.
 * Manages the connection screen, multiple server sessions, and WebView for displaying the OpenCode web UI.
 */
class MainActivity : AppCompatActivity(), 
    OpenCodeWebViewClient.WebViewClientListener,
    OpenCodeWebChromeClient.WebChromeClientListener,
    ConnectionManager.ConnectionStateListener,
    SessionManager.SessionListener {

    // Application-level dependencies
    private val app: OpenCodeApplication by lazy { application as OpenCodeApplication }
    private val connectionManager: ConnectionManager by lazy { app.connectionManager }
    private val preferencesRepository: PreferencesRepository by lazy { app.preferencesRepository }
    private val sessionManager: SessionManager by lazy { app.sessionManager }

    // Network monitoring
    private lateinit var networkStateMonitor: NetworkStateMonitor

    // Connection Screen Views
    private lateinit var connectionScreenContainer: ConstraintLayout
    private lateinit var serverUrlInput: TextInputEditText
    private lateinit var connectButton: MaterialButton
    private lateinit var errorMessage: TextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var recentServersList: RecyclerView
    private lateinit var noRecentServersMessage: TextView
    private lateinit var clearHistoryButton: MaterialButton
    private lateinit var settingsButton: ImageButton

    // WebView Container Views
    private lateinit var webViewContainer: LinearLayout
    private lateinit var sessionBar: LinearLayout
    private lateinit var activeSessionsList: RecyclerView
    private lateinit var addSessionButton: ImageButton
    private lateinit var menuButton: ImageButton
    private lateinit var webView: WebView
    private lateinit var nativeStatusBar: View
    private lateinit var nativeStatusText: TextView
    private lateinit var statusIndicator: View
    private lateinit var disconnectButton: ImageButton
    private lateinit var disconnectOverlay: FrameLayout
    private lateinit var disconnectMessage: TextView
    private lateinit var reconnectButton: MaterialButton
    private lateinit var goBackButton: MaterialButton
    private lateinit var webViewLoadingOverlay: FrameLayout

    // Drawer Views
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerSessionCount: TextView
    private lateinit var drawerSessionsList: RecyclerView
    private lateinit var drawerNewSessionButton: MaterialButton
    private lateinit var drawerSettingsButton: MaterialButton
    private lateinit var drawerSessionsAdapter: ActiveSessionsAdapter

    // Adapters
    private lateinit var recentServersAdapter: RecentServersAdapter
    private lateinit var activeSessionsAdapter: ActiveSessionsAdapter

    // WebView state storage for sessions
    private val sessionWebViewStates = mutableMapOf<String, Bundle>()

    // File chooser callback
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupWebView()
        setupClickListeners()
        setupNetworkMonitoring()
        setupBackNavigation()
        setupSessionObservers()
        
        // Register as connection state listener
        connectionManager.addListener(this)
        sessionManager.addListener(this)

        // Restore state or load initial UI
        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        } else {
            // Check if there are existing sessions to restore
            if (sessionManager.sessions.value.isNotEmpty()) {
                showWebView()
                updateSessionsUI()
                sessionManager.activeSession.value?.let { session ->
                    loadSessionInWebView(session)
                }
            } else {
                showConnectionScreen()
                loadRecentServers()
                autoFillLastUrl()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.onResume()
            // Check if we need to reconnect when coming back from background
            checkAndReconnectIfNeeded()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) {
            webView.onPause()
            // Save WebView state when going to background
            saveCurrentWebViewState()
        }
    }

    override fun onStart() {
        super.onStart()
        if (::webView.isInitialized) {
            webView.resumeTimers()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::webView.isInitialized) {
            webView.pauseTimers()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionManager.removeListener(this)
        sessionManager.removeListener(this)
        networkStateMonitor.unregister()
        
        // Save current WebView state for active session
        saveCurrentWebViewState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_CONNECTED, webViewContainer.isVisible)
        outState.putString(KEY_CURRENT_SESSION_ID, sessionManager.activeSession.value?.id)
        
        // Save WebView state for current session
        saveCurrentWebViewState()
        
        // Save WebView state bundle
        if (::webView.isInitialized) {
            val webViewState = Bundle()
            webView.saveState(webViewState)
            outState.putBundle(KEY_WEBVIEW_STATE, webViewState)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle configuration changes without recreating activity
    }

    private fun initViews() {
        // Drawer Layout
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        // Connection Screen Views
        connectionScreenContainer = findViewById(R.id.connection_screen_container)
        serverUrlInput = findViewById(R.id.server_url_input)
        connectButton = findViewById(R.id.connect_button)
        errorMessage = findViewById(R.id.error_message)
        loadingIndicator = findViewById(R.id.loading_indicator)
        recentServersList = findViewById(R.id.recent_servers_list)
        noRecentServersMessage = findViewById(R.id.no_recent_servers_message)
        clearHistoryButton = findViewById(R.id.clear_history_button)
        settingsButton = findViewById(R.id.settings_button)

        // WebView Container Views
        webViewContainer = findViewById(R.id.webview_container)
        sessionBar = findViewById(R.id.session_bar)
        activeSessionsList = findViewById(R.id.active_sessions_list)
        addSessionButton = findViewById(R.id.add_session_button)
        menuButton = findViewById(R.id.menu_button)
        webView = findViewById(R.id.opencode_webview)
        nativeStatusBar = findViewById(R.id.native_status_bar)
        nativeStatusText = findViewById(R.id.native_status_text)
        statusIndicator = findViewById(R.id.status_indicator)
        disconnectButton = findViewById(R.id.disconnect_button)
        disconnectOverlay = findViewById(R.id.disconnect_overlay)
        disconnectMessage = findViewById(R.id.disconnect_message)
        reconnectButton = findViewById(R.id.reconnect_button)
        goBackButton = findViewById(R.id.go_back_button)
        webViewLoadingOverlay = findViewById(R.id.webview_loading_overlay)

        // Drawer Views
        drawerSessionCount = navigationView.findViewById(R.id.drawer_session_count)
        drawerSessionsList = navigationView.findViewById(R.id.drawer_sessions_list)
        drawerNewSessionButton = navigationView.findViewById(R.id.drawer_new_session_button)
        drawerSettingsButton = navigationView.findViewById(R.id.drawer_settings_button)

        // Setup Recent Servers RecyclerView
        recentServersAdapter = RecentServersAdapter(
            onServerClick = { url -> onRecentServerClicked(url) },
            onDeleteClick = { url -> onDeleteRecentServer(url) }
        )
        recentServersList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = recentServersAdapter
        }

        // Setup Active Sessions RecyclerView
        activeSessionsAdapter = ActiveSessionsAdapter(
            onSessionClick = { session -> onSessionClicked(session) },
            onSessionClose = { session -> onSessionCloseClicked(session) }
        )
        activeSessionsList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = activeSessionsAdapter
        }

        // Setup Drawer Sessions RecyclerView
        drawerSessionsAdapter = ActiveSessionsAdapter(
            onSessionClick = { session -> 
                onSessionClicked(session)
                closeDrawer()
            },
            onSessionClose = { session -> onSessionCloseClicked(session) }
        )
        drawerSessionsList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = drawerSessionsAdapter
        }

        // Setup drawer
        setupDrawer()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.apply {
            webViewClient = OpenCodeWebViewClient(this@MainActivity)
            webChromeClient = OpenCodeWebChromeClient(this@MainActivity)

            settings.apply {
                // Enable JavaScript for the web UI
                javaScriptEnabled = true
                
                // Enable DOM storage for web app
                domStorageEnabled = true
                
                // Enable database storage
                databaseEnabled = true
                
                // Set cache mode
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Enable zooming but hide controls
                setSupportZoom(true)
                builtInZoomControls = false
                displayZoomControls = false
                
                // Allow mixed content for development (HTTP resources on HTTPS)
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                
                // Enable wide viewport
                useWideViewPort = true
                loadWithOverviewMode = true
                
                // Allow file access for potential file uploads
                allowFileAccess = true
                
                // Set user agent to identify as OpenCode Android client
                userAgentString = "$userAgentString OpenCodeAndroid/${BuildConfig.VERSION_NAME}"
            }
        }
    }

    private fun setupClickListeners() {
        connectButton.setOnClickListener {
            attemptConnection()
        }

        serverUrlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                attemptConnection()
                true
            } else {
                false
            }
        }

        clearHistoryButton.setOnClickListener {
            showClearHistoryDialog()
        }

        settingsButton.setOnClickListener {
            // TODO: Open settings screen
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
        }

        disconnectButton.setOnClickListener {
            showCloseSessionConfirmation()
        }

        reconnectButton.setOnClickListener {
            attemptReconnect()
        }

        goBackButton.setOnClickListener {
            closeCurrentSession()
        }

        addSessionButton.setOnClickListener {
            onAddNewSessionClicked()
        }

        menuButton.setOnClickListener {
            openDrawer()
        }

        // Drawer button listeners
        drawerNewSessionButton.setOnClickListener {
            closeDrawer()
            onAddNewSessionClicked()
        }

        drawerSettingsButton.setOnClickListener {
            closeDrawer()
            // TODO: Open settings screen
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDrawer() {
        // Configure drawer to only be swipeable when WebView is visible
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {
                updateDrawerContent()
            }
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

        // Initially lock drawer when on connection screen
        updateDrawerLockState()
    }

    private fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun updateDrawerLockState() {
        if (webViewContainer.isVisible) {
            // Enable swipe to open drawer when in WebView mode
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            // Lock drawer when on connection screen
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }

    private fun updateDrawerContent() {
        val sessions = sessionManager.sessions.value
        val sessionCount = sessions.size
        drawerSessionCount.text = getString(R.string.sessions_count, sessionCount)
        
        drawerSessionsAdapter.submitList(sessions.toList())
        sessionManager.activeSession.value?.let { activeSession ->
            drawerSessionsAdapter.setActiveSession(activeSession.id)
        }
    }

    private fun setupNetworkMonitoring() {
        networkStateMonitor = NetworkStateMonitor(this)
        
        networkStateMonitor.isConnected
            .onEach { isConnected ->
                handleNetworkStateChange(isConnected)
            }
            .launchIn(lifecycleScope)
    }

    private fun setupSessionObservers() {
        // Observe session changes
        sessionManager.sessions
            .onEach { sessions ->
                updateSessionsUI()
            }
            .launchIn(lifecycleScope)

        sessionManager.activeSession
            .onEach { session ->
                session?.let {
                    activeSessionsAdapter.setActiveSession(it.id)
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    // If drawer is open, close it
                    drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                        closeDrawer()
                    }
                    // If disconnect overlay is showing, dismiss it
                    disconnectOverlay.isVisible -> {
                        disconnectOverlay.visibility = View.GONE
                    }
                    // If WebView can go back, navigate back in web history
                    webViewContainer.isVisible && webView.canGoBack() -> {
                        webView.goBack()
                    }
                    // If WebView is showing and there are multiple sessions, show connection screen
                    webViewContainer.isVisible && sessionManager.sessions.value.size > 1 -> {
                        showCloseSessionConfirmation()
                    }
                    // If WebView is showing with single session, ask to close
                    webViewContainer.isVisible -> {
                        showCloseSessionConfirmation()
                    }
                    // Otherwise, let system handle back press (exit app)
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    // ========================================
    // Session Management
    // ========================================

    private fun onSessionClicked(session: ServerSession) {
        if (session.id == sessionManager.activeSession.value?.id) {
            return // Already active
        }
        
        // Save current WebView state before switching
        saveCurrentWebViewState()
        
        // Switch to the selected session
        sessionManager.setActiveSession(session.id)
        loadSessionInWebView(session)
    }

    private fun onSessionCloseClicked(session: ServerSession) {
        AlertDialog.Builder(this)
            .setTitle(R.string.close_session_title)
            .setMessage(getString(R.string.close_session_message, session.displayName))
            .setPositiveButton(R.string.disconnect) { _, _ ->
                closeSession(session)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun onAddNewSessionClicked() {
        if (!sessionManager.canCreateNewSession()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.session_limit_title)
                .setMessage(getString(R.string.session_limit_message, ServerSession.MAX_SESSIONS))
                .setPositiveButton(R.string.ok, null)
                .show()
            return
        }
        
        // Save current WebView state before showing connection screen
        saveCurrentWebViewState()
        
        // Show connection screen for new session
        showConnectionScreen()
        loadRecentServers()
        serverUrlInput.text?.clear()
    }

    private fun closeSession(session: ServerSession) {
        // Remove WebView state for this session
        sessionWebViewStates.remove(session.id)
        
        // Remove session from manager
        sessionManager.removeSession(session.id)
        
        // If no more sessions, show connection screen
        if (sessionManager.sessions.value.isEmpty()) {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            showConnectionScreen()
            loadRecentServers()
        } else {
            // Load the new active session
            sessionManager.activeSession.value?.let { newActiveSession ->
                loadSessionInWebView(newActiveSession)
            }
        }
    }

    private fun closeCurrentSession() {
        sessionManager.activeSession.value?.let { session ->
            closeSession(session)
        }
    }

    private fun loadSessionInWebView(session: ServerSession) {
        // Try to restore WebView state for this session
        val savedState = sessionWebViewStates[session.id] ?: session.webViewState
        
        if (savedState != null) {
            webView.restoreState(savedState)
        } else {
            // Load fresh
            webView.loadUrl(session.serverUrl)
        }
        
        // Update connection manager
        connectionManager.switchToSession(session.id, session.serverUrl)
        
        updateSessionsUI()
    }

    private fun saveCurrentWebViewState() {
        sessionManager.activeSession.value?.let { session ->
            val state = Bundle()
            webView.saveState(state)
            sessionWebViewStates[session.id] = state
            session.webViewState = state
        }
    }

    private fun updateSessionsUI() {
        val sessions = sessionManager.sessions.value
        activeSessionsAdapter.submitList(sessions.toList())
        sessionManager.activeSession.value?.let { activeSession ->
            activeSessionsAdapter.setActiveSession(activeSession.id)
        }
        
        // Show/hide session bar based on number of sessions
        sessionBar.visibility = if (sessions.isNotEmpty()) View.VISIBLE else View.GONE
        
        // Update drawer content as well
        updateDrawerContent()
    }

    // ========================================
    // Connection Handling
    // ========================================

    private fun attemptConnection() {
        val url = serverUrlInput.text?.toString() ?: ""
        
        // Validate URL
        val validationResult = connectionManager.validateUrl(url)
        
        if (!validationResult.isValid) {
            showError(validationResult.errorMessage ?: getString(R.string.error_invalid_url))
            return
        }

        // Clear any previous error
        hideError()
        
        // Start connection
        val normalizedUrl = validationResult.normalizedUrl ?: url
        connectToServer(normalizedUrl)
    }

    private fun connectToServer(url: String) {
        // Create or get session for this URL
        val session = sessionManager.createSession(url)
        
        // Update UI to show loading state
        showLoading()
        
        // Update connection state
        connectionManager.onSessionConnectionStarted(session.id, url)
        sessionManager.updateSessionState(session.id, ServerSession.ConnectionState.Connecting)
        
        // Show WebView container
        showWebView()
        
        // Load the URL in WebView
        webView.loadUrl(url)
    }

    private fun attemptReconnect() {
        disconnectOverlay.visibility = View.GONE
        
        sessionManager.activeSession.value?.let { session ->
            connectToServer(session.serverUrl)
        } ?: run {
            connectionManager.currentServerUrl?.let { url ->
                connectToServer(url)
            }
        }
    }

    /**
     * Check if reconnection is needed after returning from background.
     * This handles the case where the app becomes idle after phone lock/unlock.
     */
    private fun checkAndReconnectIfNeeded() {
        // Only check if we're showing the WebView and auto-reconnect is enabled
        if (!webViewContainer.isVisible || !preferencesRepository.autoReconnect) {
            return
        }

        val currentSession = sessionManager.activeSession.value ?: return

        // If disconnect overlay is already showing, don't interfere
        if (disconnectOverlay.isVisible) {
            return
        }

        // Check network availability first
        if (!networkStateMonitor.isConnected.value) {
            showDisconnectOverlay(getString(R.string.error_network_unavailable))
            sessionManager.updateSessionState(
                currentSession.id,
                ServerSession.ConnectionState.Error(getString(R.string.error_network_unavailable))
            )
            return
        }

        // Inject JavaScript to check if the web UI connection is still alive
        // If the web app has a connection status, we can check it
        webView.evaluateJavascript(
            """
            (function() {
                // Check if there's a visible disconnect/error state in the web UI
                // or if the page needs to be refreshed
                try {
                    // Check if document is fully loaded and interactive
                    if (document.readyState !== 'complete') {
                        return 'loading';
                    }
                    // Check for common error indicators in the web UI
                    var errorElements = document.querySelectorAll('[class*="error"], [class*="disconnect"], [class*="offline"]');
                    for (var i = 0; i < errorElements.length; i++) {
                        var el = errorElements[i];
                        if (el.offsetParent !== null && el.innerText && el.innerText.toLowerCase().includes('disconnect')) {
                            return 'disconnected';
                        }
                    }
                    return 'connected';
                } catch (e) {
                    return 'error';
                }
            })();
            """.trimIndent()
        ) { result ->
            val status = result?.replace("\"", "") ?: "unknown"
            when (status) {
                "disconnected", "error" -> {
                    // Web UI indicates disconnection, attempt reconnect
                    runOnUiThread {
                        attemptReconnect()
                    }
                }
                "loading" -> {
                    // Page is still loading, no action needed
                }
                // "connected" or "unknown" - assume OK
            }
        }
    }

    private fun showCloseSessionConfirmation() {
        sessionManager.activeSession.value?.let { session ->
            AlertDialog.Builder(this)
                .setTitle(R.string.close_session_title)
                .setMessage(getString(R.string.close_session_message, session.displayName))
                .setPositiveButton(R.string.disconnect) { _, _ ->
                    closeSession(session)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    // ========================================
    // UI State Management
    // ========================================

    private fun showConnectionScreen() {
        connectionScreenContainer.visibility = View.VISIBLE
        webViewContainer.visibility = View.GONE
        disconnectOverlay.visibility = View.GONE
        hideLoading()
        updateDrawerLockState()
    }

    private fun showWebView() {
        connectionScreenContainer.visibility = View.GONE
        webViewContainer.visibility = View.VISIBLE
        webViewLoadingOverlay.visibility = View.GONE
        disconnectOverlay.visibility = View.GONE
        updateSessionsUI()
        updateDrawerLockState()
    }

    private fun showLoading() {
        loadingIndicator.visibility = View.VISIBLE
        connectButton.isEnabled = false
        webViewLoadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingIndicator.visibility = View.GONE
        connectButton.isEnabled = true
        webViewLoadingOverlay.visibility = View.GONE
    }

    private fun showError(message: String) {
        errorMessage.text = message
        errorMessage.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorMessage.visibility = View.GONE
    }

    private fun loadRecentServers() {
        val recentUrls = connectionManager.getRecentUrls()
        
        if (recentUrls.isEmpty()) {
            recentServersList.visibility = View.GONE
            noRecentServersMessage.visibility = View.VISIBLE
            clearHistoryButton.visibility = View.GONE
        } else {
            recentServersList.visibility = View.VISIBLE
            noRecentServersMessage.visibility = View.GONE
            clearHistoryButton.visibility = View.VISIBLE
            recentServersAdapter.submitList(recentUrls)
        }
    }

    private fun autoFillLastUrl() {
        connectionManager.getLastServerUrl()?.let { lastUrl ->
            serverUrlInput.setText(lastUrl)
        }
    }

    private fun onRecentServerClicked(url: String) {
        serverUrlInput.setText(url)
        attemptConnection()
    }

    private fun onDeleteRecentServer(url: String) {
        preferencesRepository.removeRecentUrl(url)
        loadRecentServers()
    }

    private fun showClearHistoryDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_history_title)
            .setMessage(R.string.clear_history_message)
            .setPositiveButton(R.string.clear) { _, _ ->
                connectionManager.clearHistory()
                loadRecentServers()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun handleNetworkStateChange(isConnected: Boolean) {
        if (!isConnected && webViewContainer.isVisible) {
            // Show disconnect overlay when network is lost
            showDisconnectOverlay(getString(R.string.error_network_unavailable))
            
            // Update session state
            sessionManager.activeSession.value?.let { session ->
                sessionManager.updateSessionState(
                    session.id, 
                    ServerSession.ConnectionState.Error(getString(R.string.error_network_unavailable))
                )
            }
        }
    }

    private fun showDisconnectOverlay(message: String) {
        disconnectMessage.text = message
        disconnectOverlay.visibility = View.VISIBLE
    }

    private fun updateConnectionStatus(state: ConnectionManager.ConnectionState) {
        when (state) {
            is ConnectionManager.ConnectionState.Connecting -> {
                nativeStatusBar.visibility = View.VISIBLE
                nativeStatusText.text = getString(R.string.status_connecting)
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_connecting)
            }
            is ConnectionManager.ConnectionState.Connected -> {
                nativeStatusBar.visibility = View.GONE // Hide status bar when connected
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_connected)
            }
            is ConnectionManager.ConnectionState.Error -> {
                nativeStatusBar.visibility = View.VISIBLE
                nativeStatusText.text = state.message
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_disconnected)
            }
            is ConnectionManager.ConnectionState.Disconnected -> {
                nativeStatusBar.visibility = View.GONE
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_disconnected)
            }
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        val isConnected = savedInstanceState.getBoolean(KEY_IS_CONNECTED, false)
        val currentSessionId = savedInstanceState.getString(KEY_CURRENT_SESSION_ID)
        val webViewState = savedInstanceState.getBundle(KEY_WEBVIEW_STATE)
        
        if (isConnected && sessionManager.sessions.value.isNotEmpty()) {
            showWebView()
            updateSessionsUI()
            
            // Restore to the saved session or the active one
            val sessionToRestore = if (currentSessionId != null) {
                sessionManager.getSession(currentSessionId)
            } else {
                sessionManager.activeSession.value
            }
            
            sessionToRestore?.let { session ->
                sessionManager.setActiveSession(session.id)
                if (webViewState != null) {
                    webView.restoreState(webViewState)
                } else {
                    loadSessionInWebView(session)
                }
            }
        } else {
            showConnectionScreen()
            loadRecentServers()
            autoFillLastUrl()
        }
    }

    // ========================================
    // WebViewClientListener Implementation
    // ========================================

    override fun onPageStarted(url: String?) {
        showLoading()
    }

    override fun onPageFinished(url: String?) {
        hideLoading()
        showWebView()
        
        val currentSession = sessionManager.activeSession.value
        if (currentSession != null) {
            // Use the original session URL for saving to recent URLs, not the current page URL
            // This prevents internal/redirect URLs from being saved to history
            connectionManager.onSessionConnectionSuccess(currentSession.id, currentSession.serverUrl)
            sessionManager.updateSessionState(currentSession.id, ServerSession.ConnectionState.Connected)
        } else {
            connectionManager.onConnectionSuccess(url ?: "")
        }
    }

    override fun onError(message: String) {
        hideLoading()
        
        val currentSession = sessionManager.activeSession.value
        
        if (webViewContainer.isVisible) {
            // Show error overlay if already in WebView
            showDisconnectOverlay(message)
        } else {
            // Show error on connection screen
            showError(message)
            showConnectionScreen()
        }
        
        if (currentSession != null) {
            connectionManager.onSessionConnectionFailed(currentSession.id, message)
            sessionManager.updateSessionState(currentSession.id, ServerSession.ConnectionState.Error(message))
        } else {
            connectionManager.onConnectionFailed(message)
        }
    }

    override fun onSslError(message: String) {
        // For development, we might show a dialog to proceed
        // In production, we should not allow this
        onError(message)
    }

    // ========================================
    // WebChromeClientListener Implementation
    // ========================================

    override fun onProgressChanged(progress: Int) {
        // Could update a progress bar if desired
    }

    override fun onReceivedTitle(title: String?) {
        // Could update app bar title if we had one
    }

    override fun onJsAlert(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    override fun onJsConfirm(message: String, callback: (Boolean) -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ -> callback(true) }
            .setNegativeButton(R.string.cancel) { _, _ -> callback(false) }
            .show()
    }

    override fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ) {
        // TODO: Implement file chooser
        this.filePathCallback = filePathCallback
    }

    override fun onConsoleMessage(
        level: ConsoleMessage.MessageLevel,
        message: String,
        lineNumber: Int,
        sourceId: String
    ) {
        // Log console messages for debugging
        when (level) {
            ConsoleMessage.MessageLevel.ERROR -> 
                android.util.Log.e("WebView", "[$sourceId:$lineNumber] $message")
            ConsoleMessage.MessageLevel.WARNING -> 
                android.util.Log.w("WebView", "[$sourceId:$lineNumber] $message")
            else -> 
                android.util.Log.d("WebView", "[$sourceId:$lineNumber] $message")
        }
    }

    // ========================================
    // ConnectionStateListener Implementation
    // ========================================

    override fun onConnectionStateChanged(
        oldState: ConnectionManager.ConnectionState,
        newState: ConnectionManager.ConnectionState,
        sessionId: String?
    ) {
        runOnUiThread {
            updateConnectionStatus(newState)
            updateSessionsUI()
        }
    }

    override fun onSessionSwitched(sessionId: String, url: String) {
        runOnUiThread {
            updateSessionsUI()
        }
    }

    // ========================================
    // SessionListener Implementation
    // ========================================

    override fun onSessionCreated(session: ServerSession) {
        runOnUiThread {
            updateSessionsUI()
        }
    }

    override fun onSessionRemoved(session: ServerSession) {
        runOnUiThread {
            updateSessionsUI()
        }
    }

    override fun onSessionStateChanged(
        session: ServerSession,
        oldState: ServerSession.ConnectionState,
        newState: ServerSession.ConnectionState
    ) {
        runOnUiThread {
            updateSessionsUI()
        }
    }

    override fun onActiveSessionChanged(session: ServerSession) {
        runOnUiThread {
            activeSessionsAdapter.setActiveSession(session.id)
        }
    }

    companion object {
        private const val KEY_IS_CONNECTED = "is_connected"
        private const val KEY_CURRENT_SESSION_ID = "current_session_id"
        private const val KEY_WEBVIEW_STATE = "webview_state"
    }
}
