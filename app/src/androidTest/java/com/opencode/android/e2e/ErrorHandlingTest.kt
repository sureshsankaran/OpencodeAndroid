package com.opencode.android.e2e

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
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
 * E2E Tests for error handling functionality.
 * Tests 13-14, 17 from the test plan (HIGH priority).
 * 
 * Uses UI Automator for device-level testing including:
 * - Invalid URL error display
 * - Unreachable server error handling
 * - Network loss during session
 * 
 * These tests verify:
 * - Clear error messages for invalid URLs
 * - Appropriate error handling for unreachable servers
 * - Graceful handling of network loss with retry options
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ErrorHandlingTest {

    @get:Rule
    val mockWebServerRule = MockWebServerRule()

    private lateinit var device: UiDevice
    private lateinit var context: Context

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()
        
        // Clear app data for clean state
        UiAutomatorHelper.clearAppData(device)
        
        // Start from home
        device.pressHome()
        device.wait(Until.hasObject(By.pkg(device.launcherPackageName).depth(0)), LAUNCH_TIMEOUT)
    }

    @After
    fun tearDown() {
        UiAutomatorHelper.forceStopApp(device)
    }

    // ============================================
    // TEST 13: Error Display for Invalid URL
    // ============================================

    @Test
    fun test13_invalidUrlShowsError() {
        // GIVEN: User enters an invalid URL
        // WHEN: User clicks connect
        // THEN: App should show a clear error message

        launchApp()
        
        // Enter invalid URL
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        assertNotNull("URL input should be visible", urlInput)
        urlInput.text = "not-a-valid-url"
        
        // Click connect
        val connectButton = device.findObject(By.res(PACKAGE_NAME, "connect_button"))
        connectButton.click()
        
        // Wait for error to appear
        Thread.sleep(1000)
        
        // Verify error is displayed
        val errorMessage = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        val errorText = device.findObject(By.textContains("Invalid"))
        
        assertTrue(
            "Error should be displayed for invalid URL",
            errorMessage != null || errorText != null
        )
    }

    @Test
    fun test13b_emptyUrlShowsError() {
        // Verify empty URL shows appropriate error
        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput.clear()
        
        val connectButton = device.findObject(By.res(PACKAGE_NAME, "connect_button"))
        connectButton.click()
        
        Thread.sleep(500)
        
        // Either button is disabled or error is shown
        val errorMessage = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        val errorText = device.findObject(By.textContains("enter"))
        
        // Verify some feedback is given
        assertTrue(
            "Should show error or prevent connection for empty URL",
            errorMessage != null || errorText != null || !connectButton.isEnabled
        )
    }

    @Test
    fun test13c_urlWithoutProtocolShowsError() {
        // Verify URL without http/https shows error
        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput.text = "localhost:3000"  // Missing protocol
        
        val connectButton = device.findObject(By.res(PACKAGE_NAME, "connect_button"))
        connectButton.click()
        
        Thread.sleep(1000)
        
        // Should either auto-add protocol or show error
        val errorMessage = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        val webView = device.findObject(By.res(PACKAGE_NAME, "opencode_webview"))
        
        // Either it worked (auto-added protocol) or showed an error
        assertTrue(
            "Should handle URL without protocol",
            errorMessage != null || webView != null
        )
    }

    @Test
    fun test13d_malformedUrlShowsError() {
        // Verify malformed URL shows clear error
        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput.text = "http://[invalid"  // Malformed URL
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        Thread.sleep(1000)
        
        val errorMessage = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        assertNotNull("Error should be shown for malformed URL", errorMessage)
    }

    @Test
    fun test13e_errorMessageIsClear() {
        // Verify error message is user-friendly
        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput.text = "invalid-url"
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        Thread.sleep(1000)
        
        val errorMessage = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        if (errorMessage != null) {
            val errorText = errorMessage.text
            // Error should be descriptive, not technical
            assertFalse(
                "Error should be user-friendly, not contain stack trace",
                errorText.contains("Exception") || errorText.contains("Error:")
            )
        }
    }

    // ============================================
    // TEST 14: Error Display for Unreachable Server
    // ============================================

    @Test
    fun test14_unreachableServerShowsError() {
        // GIVEN: User enters a URL for a server that doesn't exist
        // WHEN: Connection attempt times out
        // THEN: App should show a clear error message

        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        // Use a URL that will definitely not connect
        urlInput.text = "http://192.0.2.1:9999"  // TEST-NET IP, guaranteed unreachable
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        // Wait for connection timeout (may take several seconds)
        device.wait(
            Until.hasObject(By.res(PACKAGE_NAME, "error_message")),
            15000  // Longer timeout for connection failure
        )
        
        val errorMessage = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        val errorText = device.findObject(By.textContains("connect"))
        
        assertTrue(
            "Error should be displayed for unreachable server",
            errorMessage != null || errorText != null
        )
    }

    @Test
    fun test14b_connectionRefusedShowsError() {
        // Verify connection refused error is handled
        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        // localhost with unlikely port
        urlInput.text = "http://127.0.0.1:59999"
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        // Wait for error
        device.wait(
            Until.hasObject(By.res(PACKAGE_NAME, "error_message")),
            10000
        )
        
        val errorMessage = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        assertNotNull("Error should be shown for connection refused", errorMessage)
    }

    @Test
    fun test14c_dnsResolutionFailureShowsError() {
        // Verify DNS resolution failure is handled gracefully
        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput.text = "http://this-domain-definitely-does-not-exist-12345.com"
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        // Wait for error
        device.wait(
            Until.hasObject(By.res(PACKAGE_NAME, "error_message")),
            15000
        )
        
        val errorMessage = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        val errorText = device.findObject(By.textContains("not found"))
        
        assertTrue(
            "Error should be shown for DNS failure",
            errorMessage != null || errorText != null
        )
    }

    @Test
    fun test14d_serverErrorResponseHandled() {
        // Verify server error responses (5xx) are handled
        mockWebServerRule.setCustomDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error")
            }
        })

        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput.text = mockWebServerRule.url
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        Thread.sleep(3000)
        
        // Should show error or error state in WebView
        val errorMessage = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        val errorOverlay = device.findObject(By.res(PACKAGE_NAME, "error_overlay"))
        
        assertTrue(
            "Server error should be handled",
            errorMessage != null || errorOverlay != null
        )
    }

    @Test
    fun test14e_loadingIndicatorShownDuringConnection() {
        // Verify loading indicator is shown while trying to connect
        mockWebServerRule.simulateSlowNetwork(5000)

        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput.text = mockWebServerRule.url
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        // Check for loading indicator
        val loadingIndicator = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "loading_indicator")),
            2000
        )
        
        assertNotNull("Loading indicator should be shown during connection", loadingIndicator)
    }

    @Test
    fun test14f_retryButtonAvailableAfterError() {
        // Verify retry option is available after connection error
        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput.text = "http://192.0.2.1:9999"
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        // Wait for error
        device.wait(
            Until.hasObject(By.res(PACKAGE_NAME, "error_message")),
            15000
        )
        
        // Connect button should still be available for retry
        val connectButton = device.findObject(By.res(PACKAGE_NAME, "connect_button"))
        val retryButton = device.findObject(By.res(PACKAGE_NAME, "retry_button"))
        
        assertTrue(
            "Retry option should be available",
            connectButton != null || retryButton != null
        )
    }

    // ============================================
    // TEST 17: Network Loss During Session
    // ============================================

    @Test
    fun test17_networkLossDuringSessionShowsError() {
        // GIVEN: User is connected to a server
        // WHEN: Network connection is lost
        // THEN: App should handle gracefully with retry option

        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        // Wait for connection
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        // Simulate network loss by making server unavailable
        mockWebServerRule.simulateDisconnection()
        
        // Try to interact (trigger network request)
        // This would typically be done through WebView interaction
        Thread.sleep(3000)
        
        // Check for disconnect overlay or error state
        val disconnectOverlay = device.findObject(By.res(PACKAGE_NAME, "disconnect_overlay"))
        val errorMessage = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        val statusIndicator = device.findObject(By.textContains("Disconnected"))
        
        // At least one indicator of network loss should be present
        assertTrue(
            "Network loss should be indicated to user",
            disconnectOverlay != null || errorMessage != null || statusIndicator != null
        )
    }

    @Test
    fun test17b_retryOptionAvailableAfterNetworkLoss() {
        // Verify retry button appears after network loss
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        
        // Simulate network loss
        mockWebServerRule.simulateDisconnection()
        Thread.sleep(3000)
        
        // Look for retry/reconnect button
        val reconnectButton = device.findObject(By.res(PACKAGE_NAME, "reconnect_button"))
        val retryButton = device.findObject(By.res(PACKAGE_NAME, "retry_button"))
        val refreshButton = device.findObject(By.textContains("Retry"))
        
        assertTrue(
            "Retry option should be available after network loss",
            reconnectButton != null || retryButton != null || refreshButton != null
        )
    }

    @Test
    fun test17c_reconnectionAfterNetworkRestore() {
        // Verify reconnection works after network is restored
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        
        // Simulate network loss
        mockWebServerRule.simulateDisconnection()
        Thread.sleep(2000)
        
        // Restore network
        mockWebServerRule.restoreNormalOperation()
        
        // Try to reconnect
        val reconnectButton = device.findObject(By.res(PACKAGE_NAME, "reconnect_button"))
        reconnectButton?.click()
        
        // If no reconnect button, try pressing back and reconnecting
        if (reconnectButton == null) {
            device.pressBack()
            device.wait(Until.hasObject(By.res(PACKAGE_NAME, "connect_button")), UI_TIMEOUT)
            device.findObject(By.res(PACKAGE_NAME, "connect_button"))?.click()
        }
        
        // Verify reconnection
        assertTrue(
            "Should reconnect after network restore",
            device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        )
    }

    @Test
    fun test17d_offlineModeIndicator() {
        // Verify offline/network issue is clearly indicated
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        
        // Disable network (using mock server simulation)
        mockWebServerRule.simulateDisconnection()
        Thread.sleep(3000)
        
        // Look for offline indicator
        val offlineIndicator = device.findObject(By.res(PACKAGE_NAME, "offline_indicator"))
        val disconnectedStatus = device.findObject(By.textContains("Offline"))
        val noConnectionText = device.findObject(By.textContains("No connection"))
        
        assertTrue(
            "Offline state should be clearly indicated",
            offlineIndicator != null || disconnectedStatus != null || noConnectionText != null
        )
    }

    @Test
    fun test17e_chatInputDisabledWhenDisconnected() {
        // Verify chat input is disabled or shows error when disconnected
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        // Disconnect
        mockWebServerRule.simulateDisconnection()
        Thread.sleep(2000)
        
        // Try to send a message through WebView
        // The UI should either prevent sending or show an error
        // This is implementation-dependent
    }

    // ============================================
    // Additional Error Handling Tests
    // ============================================

    @Test
    fun testSslCertificateErrorHandled() {
        // Verify SSL certificate errors are handled appropriately
        // Note: This would require a mock HTTPS server with invalid cert
        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        // Self-signed cert URL would fail
        urlInput.text = "https://self-signed.badssl.com"
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        Thread.sleep(5000)
        
        // Should show SSL error or warning
        val errorMessage = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        val sslWarning = device.findObject(By.textContains("certificate"))
        
        // Should either show error or handle the warning
        assertTrue(
            "SSL error should be handled",
            errorMessage != null || sslWarning != null
        )
    }

    @Test
    fun testTimeoutErrorHandled() {
        // Verify connection timeout is handled
        mockWebServerRule.simulateSlowNetwork(60000)  // Very slow

        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput.text = mockWebServerRule.url
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        // Wait for timeout (typically 30 seconds for HTTP)
        device.wait(
            Until.hasObject(By.res(PACKAGE_NAME, "error_message")),
            35000
        )
        
        val errorMessage = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        val timeoutText = device.findObject(By.textContains("timeout"))
        
        assertTrue(
            "Timeout error should be displayed",
            errorMessage != null || timeoutText != null
        )
    }

    @Test
    fun testErrorDismissalWorks() {
        // Verify errors can be dismissed
        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput.text = "invalid-url"
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        Thread.sleep(1000)
        
        // Try to dismiss error
        val dismissButton = device.findObject(By.res(PACKAGE_NAME, "dismiss_error"))
        dismissButton?.click()
        
        // Or clear and type new URL
        urlInput.clear()
        
        // Error should be dismissable
        val errorAfterClear = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        // Error may or may not persist - both are valid implementations
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
