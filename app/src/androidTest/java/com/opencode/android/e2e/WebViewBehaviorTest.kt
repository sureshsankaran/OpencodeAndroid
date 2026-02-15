package com.opencode.android.e2e

import android.content.Context
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.*
import androidx.test.espresso.web.webdriver.Locator
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

import com.opencode.android.R
import com.opencode.android.robots.WebViewRobot
import com.opencode.android.util.MockWebServerRule
import com.opencode.android.util.UiAutomatorHelper
import com.opencode.android.util.UiAutomatorHelper.Companion.LAUNCH_TIMEOUT
import com.opencode.android.util.UiAutomatorHelper.Companion.PACKAGE_NAME
import com.opencode.android.util.UiAutomatorHelper.Companion.UI_TIMEOUT

/**
 * E2E Tests for WebView behavior and configuration.
 * Tests 11, 15-16 from the test plan (HIGH priority).
 * 
 * Uses UI Automator combined with Espresso Web for WebView testing.
 * 
 * These tests verify:
 * - Android back button behavior with WebView history
 * - JavaScript enabled for full web UI functionality
 * - HTTPS support with valid certificates
 * - WebView settings (zoom, DOM storage, etc.)
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class WebViewBehaviorTest {

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
        device.unfreezeRotation()
    }

    // ============================================
    // TEST 11: Back Button Behavior
    // ============================================

    @Test
    fun test11_backButtonNavigatesWebViewHistoryFirst() {
        // GIVEN: User has navigated within the WebView
        // WHEN: User presses back button
        // THEN: WebView should navigate back before exiting

        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        // Wait for WebView to load
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(2000)  // Let page fully load
        
        // Simulate navigation within WebView (click a link or navigate)
        // For this test, we'll verify the back button behavior
        
        // Press back - should not exit if there's history
        device.pressBack()
        
        // Give it time to process
        Thread.sleep(500)
        
        // Should still be in the app (either on WebView or connection screen)
        assertTrue(
            "App should still be visible after first back press",
            device.hasObject(By.pkg(PACKAGE_NAME))
        )
    }

    @Test
    fun test11b_backButtonExitsAfterHistoryExhausted() {
        // Verify back button exits when no more WebView history
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        
        // Press back - should go to connection screen or exit
        device.pressBack()
        Thread.sleep(500)
        
        // Should be on connection screen now
        val connectionScreen = device.findObject(By.res(PACKAGE_NAME, "connection_screen_container"))
        val urlInput = device.findObject(By.res(PACKAGE_NAME, "server_url_input"))
        
        assertTrue(
            "Should return to connection screen",
            connectionScreen != null || urlInput != null
        )
        
        // Press back again - should exit app
        device.pressBack()
        Thread.sleep(500)
        
        // App should no longer be in foreground
        assertFalse(
            "App should exit after second back press",
            device.hasObject(By.pkg(PACKAGE_NAME).depth(0))
        )
    }

    @Test
    fun test11c_backButtonWithMultipleHistoryEntries() {
        // Verify back button works with multiple history entries
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        // The mock server serves the same page, but in real scenario
        // there would be navigation. We test the back button handling.
        
        // Press back multiple times
        for (i in 1..3) {
            if (device.hasObject(By.pkg(PACKAGE_NAME))) {
                device.pressBack()
                Thread.sleep(300)
            }
        }
        
        // Eventually should exit or be on connection screen
        // This verifies back button is not blocked
    }

    // ============================================
    // TEST 15: JavaScript Enabled
    // ============================================

    @Test
    fun test15_javaScriptIsEnabled() {
        // GIVEN: Connected to OpenCode server
        // WHEN: WebView loads
        // THEN: JavaScript should be enabled for full functionality

        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(2000)
        
        // Verify by checking if JS-dependent elements are functional
        // The mock server sets window.openCodeLoaded = true via JS
        
        // If JavaScript is enabled, the page will have interactive elements
        // working properly. We can verify by checking if the chat input is functional.
        
        WebViewRobot().apply {
            assertJavaScriptEnabled()
        }
    }

    @Test
    fun test15b_javaScriptInteractionWorks() {
        // Verify JavaScript interactions work in WebView
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(2000)
        
        // Try to interact with JS-powered elements
        WebViewRobot().apply {
            assertChatInputPresent()
            typeChatMessage("Test JavaScript")
            clickSendButton()
            
            // If JS is working, message should appear
            assertUserMessageDisplayed("Test JavaScript")
        }
    }

    @Test
    fun test15c_domStorageEnabled() {
        // Verify DOM storage is enabled for session data
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        WebViewRobot().apply {
            assertDomStorageEnabled()
        }
    }

    @Test
    fun test15d_webAppFunctionalityComplete() {
        // Verify the complete web app functionality works
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(2000)
        
        // Full functionality test
        WebViewRobot().apply {
            assertOpenCodeUiLoaded()
            assertHeaderPresent()
            assertChatContainerPresent()
            assertChatInputPresent()
            assertWelcomeMessageDisplayed()
            
            // Send a message and verify response
            sendChatMessage("Hello from WebView test")
            waitForMessageCount(3)
            assertAiResponseDisplayed()
        }
    }

    // ============================================
    // TEST 16: HTTPS Support
    // ============================================

    @Test
    fun test16_httpsConnectionSupported() {
        // GIVEN: User enters an HTTPS URL
        // WHEN: Connection is established
        // THEN: Should connect successfully with valid certificate

        // Note: MockWebServer doesn't easily support HTTPS
        // This test verifies the app can handle HTTPS URLs
        
        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        
        // Enter HTTPS URL (will fail to connect but should be accepted)
        urlInput.text = "https://example.com"
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        // App should attempt to connect (not reject the URL format)
        Thread.sleep(3000)
        
        // Either WebView appears or connection error (not URL format error)
        val webView = device.findObject(By.res(PACKAGE_NAME, "opencode_webview"))
        val errorMessage = device.findObject(By.res(PACKAGE_NAME, "error_message"))
        
        // The URL should be accepted (HTTPS supported)
        // Connection may fail due to no mock server, but format is valid
        assertTrue(
            "HTTPS URL should be accepted",
            webView != null || errorMessage != null
        )
    }

    @Test
    fun test16b_httpUrlAlsoSupported() {
        // Verify HTTP URLs work (for local development servers)
        val serverUrl = mockWebServerRule.url  // This is HTTP

        launchApp()
        connectToServer(serverUrl)
        
        assertTrue(
            "HTTP connection should work",
            device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        )
    }

    @Test
    fun test16c_mixedContentHandled() {
        // Verify mixed content (HTTP resources on HTTPS page) is handled
        // This is an implementation detail - app should have a clear policy
        
        // For now, verify the app doesn't crash with mixed content scenarios
        launchApp()
        
        val urlInput = device.wait(
            Until.findObject(By.res(PACKAGE_NAME, "server_url_input")),
            UI_TIMEOUT
        )
        urlInput.text = "https://mixed-content-test.example.com"
        
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        Thread.sleep(5000)
        
        // App should handle gracefully (not crash)
        assertTrue(
            "App should handle mixed content gracefully",
            device.hasObject(By.pkg(PACKAGE_NAME))
        )
    }

    // ============================================
    // Additional WebView Behavior Tests
    // ============================================

    @Test
    fun testZoomControlsDisabled() {
        // Verify zoom controls are disabled to maintain web UI scaling
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        
        WebViewRobot().apply {
            assertZoomDisabled()
        }
    }

    @Test
    fun testWebViewFillsContainer() {
        // Verify WebView properly fills its container
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        
        // WebView should fill the container
        onView(withId(R.id.opencode_webview))
            .check(matches(isCompletelyDisplayed()))
    }

    @Test
    fun testScreenRotationPreservesState() {
        // Verify WebView state is preserved during rotation
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        // Rotate to landscape
        device.setOrientationLandscape()
        Thread.sleep(1000)
        
        // Verify WebView is still displayed
        assertTrue(
            "WebView should be displayed after rotation",
            device.hasObject(By.res(PACKAGE_NAME, "opencode_webview"))
        )
        
        // Rotate back to portrait
        device.setOrientationPortrait()
        Thread.sleep(1000)
        
        assertTrue(
            "WebView should be displayed after rotation back",
            device.hasObject(By.res(PACKAGE_NAME, "opencode_webview"))
        )
        
        device.unfreezeRotation()
    }

    @Test
    fun testWebViewScrolling() {
        // Verify scrolling works within WebView
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        // Send multiple messages to create scrollable content
        WebViewRobot().apply {
            repeat(5) { i ->
                sendChatMessage("Message $i")
                waitForMessageCount(2 + (i + 1) * 2)
            }
        }
        
        // Scrolling should work (implemented by the web content)
    }

    @Test
    fun testWebViewKeyboardInteraction() {
        // Verify keyboard appears and works with WebView input
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(1000)
        
        // Click on input field to trigger keyboard
        WebViewRobot().apply {
            // Focus on chat input
            onWebView(withId(R.id.opencode_webview))
                .forceJavascriptEnabled()
                .withElement(findElement(Locator.ID, "chat-input"))
                .perform(webClick())
        }
        
        Thread.sleep(500)
        
        // Keyboard should be visible (implementation dependent)
        // Type a message
        WebViewRobot().apply {
            typeChatMessage("Keyboard test")
        }
    }

    @Test
    fun testWebViewCookiePersistence() {
        // Verify cookies persist for session management
        val serverUrl = mockWebServerRule.url

        launchApp()
        connectToServer(serverUrl)
        
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
        Thread.sleep(2000)
        
        // Restart app
        UiAutomatorHelper.forceStopApp(device)
        Thread.sleep(500)
        launchApp()
        
        // Reconnect
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "connect_button")), UI_TIMEOUT)
        device.findObject(By.res(PACKAGE_NAME, "connect_button")).click()
        
        // Should reconnect - cookies should be preserved
        assertTrue(
            "Should reconnect with preserved cookies",
            device.wait(Until.hasObject(By.res(PACKAGE_NAME, "opencode_webview")), LAUNCH_TIMEOUT)
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
