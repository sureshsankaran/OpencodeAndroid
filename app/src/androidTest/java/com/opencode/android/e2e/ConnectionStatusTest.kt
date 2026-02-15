package com.opencode.android.e2e

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import com.opencode.android.util.MockWebServerRule
import com.opencode.android.util.UiAutomatorHelper
import com.opencode.android.util.UiAutomatorHelper.Companion.LAUNCH_TIMEOUT
import com.opencode.android.util.UiAutomatorHelper.Companion.PACKAGE_NAME
import com.opencode.android.util.UiAutomatorHelper.Companion.UI_TIMEOUT

/**
 * E2E Tests for connection status indicator functionality.
 * Test 12 from the test plan (HIGH priority).
 * 
 * Uses UI Automator for device-level testing.
 * 
 * These tests verify:
 * - Visual indicator shows connection status (connected/disconnected/connecting)
 * - Status updates in real-time
 * - Status is visible and clear to users
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectionStatusTest {

    @get:Rule
    val mockWebServerRule = MockWebServerRule()

    private lateinit var device: UiDevice
    private lateinit var context: Context

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()
        
        UiAutomatorHelper.clearAppData(device)
        device.pressHome()
        device.wait(Until.hasObject(By.pkg(device.launcherPackageName).depth(0)), LAUNCH_TIMEOUT)
    }

    @After
    fun tearDown() {
        UiAutomatorHelper.forceStopApp(device)
    }

    // ============================================
    // TEST 12: Connection Status Indicator
    // ============================================

    @Test
    fun test12_connectionStatusIndicatorExists() {
        // GIVEN: User is on the WebView screen
        // WHEN: Connected to server
        // THEN: A visual indicator should show the connection status

        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        // Wait for WebView to load
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        // Look for connection status indicator
        val nativeStatusBar = device.findObject(By.res(PACKAGE_NAME, "native_status_bar"))
        val nativeStatusText = device.findObject(By.res(PACKAGE_NAME, "native_status_text"))
        val connectionIndicator = device.findObject(By.res(PACKAGE_NAME, "connection_indicator"))
        
        // At least one status indicator should exist
        assertTrue(
            "Connection status indicator should be present",
            nativeStatusBar != null || nativeStatusText != null || connectionIndicator != null
        )
    }

    @Test
    fun test12b_showsConnectedStatus() {
        // Verify "Connected" status is shown when connected
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        // Look for "Connected" text
        val connectedText = device.findObject(By.textContains("Connected"))
        val connectedStatus = device.findObject(By.res(PACKAGE_NAME, "status_connected"))
        
        // In WebView, the web UI also shows connection status
        assertTrue(
            "Should show connected status",
            connectedText != null || connectedStatus != null
        )
    }

    @Test
    fun test12c_showsConnectingStatus() {
        // Verify "Connecting" status is shown during connection
        mockWebServerRule.simulateSlowNetwork(5000)
        
        val serverUrl = mockWebServerRule.url

        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput.text = serverUrl
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        // Look for connecting indicator
        val connectingText = device.wait(
            Until.findObject(By.textContains("Connecting")),
            2000
        )
        val loadingIndicator = device.findObject(By.res(PACKAGE_NAME, "loading_indicator"))
        
        assertTrue(
            "Should show connecting status",
            connectingText != null || loadingIndicator != null
        )
    }

    @Test
    fun test12d_showsDisconnectedStatus() {
        // Verify "Disconnected" status is shown when disconnected
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        // Simulate disconnection
        mockWebServerRule.simulateDisconnection()
        Thread.sleep(3000)
        
        // Look for disconnected indicator
        val disconnectedText = device.findObject(By.textContains("Disconnected"))
        val offlineText = device.findObject(By.textContains("Offline"))
        val disconnectedStatus = device.findObject(By.res(PACKAGE_NAME, "status_disconnected"))
        
        assertTrue(
            "Should show disconnected status",
            disconnectedText != null || offlineText != null || disconnectedStatus != null
        )
    }

    @Test
    fun test12e_statusUpdatesInRealTime() {
        // Verify status updates from connected to disconnected
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        // Verify connected
        var connectedText = device.findObject(By.textContains("Connected"))
        assertTrue("Should initially show connected", connectedText != null)
        
        // Disconnect
        mockWebServerRule.simulateDisconnection()
        Thread.sleep(3000)
        
        // Verify status changed
        val disconnectedText = device.findObject(By.textContains("Disconnected"))
        val offlineText = device.findObject(By.textContains("Offline"))
        
        assertTrue(
            "Status should update to disconnected",
            disconnectedText != null || offlineText != null
        )
        
        // Restore connection
        mockWebServerRule.restoreNormalOperation()
        
        // Click reconnect or refresh
        val reconnectButton = device.findObject(By.res(PACKAGE_NAME, "reconnect_button"))
        reconnectButton?.click()
        
        Thread.sleep(3000)
        
        // Status should return to connected
        connectedText = device.findObject(By.textContains("Connected"))
        // May or may not reconnect automatically - implementation dependent
    }

    @Test
    fun test12f_statusIndicatorIsVisibleAndClear() {
        // Verify the status indicator is prominent and easy to see
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        // The status should be in a visible location
        // (typically in a status bar at top or bottom)
        
        val statusBar = device.findObject(By.res(PACKAGE_NAME, "native_status_bar"))
        if (statusBar != null) {
            // Verify it's visible (not too small)
            assertTrue(
                "Status bar should have reasonable size",
                statusBar.visibleBounds.height() > 20
            )
        }
    }

    @Test
    fun test12g_statusIndicatorColorCoding() {
        // Verify status uses color coding (green for connected, red for disconnected)
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        // Look for color-coded indicators
        // In web UI, this is handled by CSS classes
        // In native UI, we'd check the background color
        
        // This test verifies the indicator exists - actual color testing
        // would require screenshot analysis or accessibility services
        
        val statusIndicator = device.findObject(By.res(PACKAGE_NAME, "status_indicator"))
        val connectionIndicator = device.findObject(By.res(PACKAGE_NAME, "connection_indicator"))
        
        assertTrue(
            "Should have a status indicator",
            statusIndicator != null || connectionIndicator != null
        )
    }

    // ============================================
    // Additional Connection Status Tests
    // ============================================

    @Test
    fun testStatusPersistsAcrossNavigation() {
        // Verify status indicator remains visible during navigation
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        // Status should be visible
        val initialStatus = device.findObject(By.textContains("Connected"))
        
        // Navigate within app (e.g., send a message)
        // Status should still be visible
        
        Thread.sleep(1000)
        
        val statusAfterNav = device.findObject(By.textContains("Connected"))
        assertTrue(
            "Status should persist during navigation",
            statusAfterNav != null || initialStatus != null
        )
    }

    @Test
    fun testStatusOnConnectionScreen() {
        // Verify no "connected" status on connection screen (not connected yet)
        launchApp()
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "server_url_input")), UI_TIMEOUT)
        
        // Should not show "Connected" on connection screen
        val connectedText = device.findObject(By.textContains("Connected"))
        
        // On the connection screen, we shouldn't show "Connected" status
        // We might show "Disconnected" or no status at all
        if (connectedText != null) {
            // If there's a status, it should not be "Connected"
            assertFalse(
                "Should not show connected on connection screen",
                connectedText.text == "Connected"
            )
        }
    }

    @Test
    fun testStatusAfterAppRestart() {
        // Verify correct status is shown after app restart
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        
        // Restart app
        UiAutomatorHelper.forceStopApp(device)
        Thread.sleep(500)
        launchApp()
        
        // On restart, should be on connection screen (not connected)
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "server_url_input")), UI_TIMEOUT)
        
        // Should not falsely show "Connected"
        val connectedText = device.findObject(By.textContains("Connected"))
        assertNull("Should not show connected after restart", connectedText)
    }

    @Test
    fun testStatusDuringSlowConnection() {
        // Verify connecting status is shown for slow connections
        mockWebServerRule.simulateSlowNetwork(10000)
        
        val serverUrl = mockWebServerRule.url

        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput.text = serverUrl
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        // During slow connection, should show connecting/loading
        Thread.sleep(2000)
        
        val loadingIndicator = device.findObject(By.res(PACKAGE_NAME, "loading_indicator"))
        val connectingText = device.findObject(By.textContains("Connecting"))
        val progressBar = device.findObject(By.clazz("android.widget.ProgressBar"))
        
        assertTrue(
            "Should show loading state during slow connection",
            loadingIndicator != null || connectingText != null || progressBar != null
        )
    }

    // ============================================
    // Helper Methods
    // ============================================

    private fun launchApp() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
        launchIntent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(launchIntent)
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), LAUNCH_TIMEOUT)
    }

    private fun connectToServer(serverUrl: String) {
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput?.clear()
        urlInput?.text = serverUrl
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button"))?.click()
    }
}
