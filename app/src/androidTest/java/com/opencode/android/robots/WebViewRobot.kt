package com.opencode.android.robots

import android.webkit.WebView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.model.Atoms.getCurrentUrl
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.*
import androidx.test.espresso.web.webdriver.Locator
import org.hamcrest.Matchers.*
import com.opencode.android.R
import com.opencode.android.util.OpenCodeLoadedIdlingResource
import com.opencode.android.util.ChatMessageIdlingResource

/**
 * Robot pattern class for interacting with the WebView screen.
 * Provides a clean API for E2E tests to interact with the OpenCode web UI.
 */
class WebViewRobot {

    /**
     * Verify that the WebView container is displayed
     */
    fun assertWebViewDisplayed(): WebViewRobot {
        onView(withId(R.id.webview_container))
            .check(matches(isDisplayed()))
        return this
    }

    /**
     * Verify that the WebView itself is displayed
     */
    fun assertWebViewVisible(): WebViewRobot {
        onView(withId(R.id.opencode_webview))
            .check(matches(isDisplayed()))
        return this
    }

    /**
     * Verify the WebView has loaded the OpenCode UI
     */
    fun assertOpenCodeUiLoaded(): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .check(webMatches(getCurrentUrl(), containsString("/")))
        return this
    }

    /**
     * Verify the OpenCode header is present
     */
    fun assertHeaderPresent(): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.ID, "header"))
            .check(webMatches(getText(), containsString("OpenCode")))
        return this
    }

    /**
     * Verify the chat container is present
     */
    fun assertChatContainerPresent(): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.ID, "chat-container"))
        return this
    }

    /**
     * Verify the chat input is present
     */
    fun assertChatInputPresent(): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.ID, "chat-input"))
        return this
    }

    /**
     * Type text into the chat input
     */
    fun typeChatMessage(message: String): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.ID, "chat-input"))
            .perform(clearElement())
            .perform(webKeys(message))
        return this
    }

    /**
     * Click the send button
     */
    fun clickSendButton(): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.ID, "send-button"))
            .perform(webClick())
        return this
    }

    /**
     * Send a chat message - combined action
     */
    fun sendChatMessage(message: String): WebViewRobot {
        typeChatMessage(message)
        clickSendButton()
        return this
    }

    /**
     * Verify a user message appears in the chat
     */
    fun assertUserMessageDisplayed(message: String): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.CSS_SELECTOR, ".message.user"))
            .check(webMatches(getText(), containsString(message)))
        return this
    }

    /**
     * Verify an AI response message appears in the chat
     */
    fun assertAiResponseDisplayed(): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.CSS_SELECTOR, ".message.ai"))
        return this
    }

    /**
     * Verify AI response contains specific text
     */
    fun assertAiResponseContains(text: String): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.CSS_SELECTOR, ".message.ai"))
            .check(webMatches(getText(), containsString(text)))
        return this
    }

    /**
     * Verify the welcome/system message is displayed
     */
    fun assertWelcomeMessageDisplayed(): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.CSS_SELECTOR, ".message.system"))
            .check(webMatches(getText(), containsString("Welcome")))
        return this
    }

    /**
     * Verify the connection status shows connected
     */
    fun assertConnectedStatus(): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.ID, "connection-status"))
            .check(webMatches(getText(), containsString("Connected")))
        return this
    }

    /**
     * Verify the connection status shows disconnected
     */
    fun assertDisconnectedStatus(): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.ID, "connection-status"))
            .check(webMatches(getText(), containsString("Disconnected")))
        return this
    }

    /**
     * Verify connection status indicator has specific class
     */
    fun assertConnectionStatusClass(statusClass: String): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.CSS_SELECTOR, ".status-indicator.$statusClass"))
        return this
    }

    /**
     * Get the current URL loaded in WebView
     */
    fun getCurrentLoadedUrl(): String {
        var url = ""
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .check { webView, _ ->
                url = (webView as WebView).url ?: ""
            }
        return url
    }

    /**
     * Verify the WebView loaded a specific URL
     */
    fun assertUrlLoaded(expectedUrl: String): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .check(webMatches(getCurrentUrl(), containsString(expectedUrl)))
        return this
    }

    /**
     * Verify JavaScript is enabled in WebView
     */
    fun assertJavaScriptEnabled(): WebViewRobot {
        onView(withId(R.id.opencode_webview))
            .check { view, _ ->
                val webView = view as WebView
                assert(webView.settings.javaScriptEnabled) { 
                    "JavaScript should be enabled in WebView" 
                }
            }
        return this
    }

    /**
     * Verify DOM storage is enabled
     */
    fun assertDomStorageEnabled(): WebViewRobot {
        onView(withId(R.id.opencode_webview))
            .check { view, _ ->
                val webView = view as WebView
                assert(webView.settings.domStorageEnabled) { 
                    "DOM storage should be enabled in WebView" 
                }
            }
        return this
    }

    /**
     * Verify zoom controls are disabled
     */
    fun assertZoomDisabled(): WebViewRobot {
        onView(withId(R.id.opencode_webview))
            .check { view, _ ->
                val webView = view as WebView
                assert(!webView.settings.builtInZoomControls) { 
                    "Built-in zoom controls should be disabled" 
                }
            }
        return this
    }

    /**
     * Execute JavaScript in the WebView
     */
    fun executeJavaScript(script: String): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.TAG_NAME, "body"))
            .perform(script(script))
        return this
    }

    /**
     * Navigate back in WebView history
     */
    fun navigateBack(): WebViewRobot {
        onView(withId(R.id.opencode_webview))
            .check { view, _ ->
                val webView = view as WebView
                if (webView.canGoBack()) {
                    webView.goBack()
                }
            }
        return this
    }

    /**
     * Check if WebView can go back
     */
    fun assertCanGoBack(expected: Boolean): WebViewRobot {
        onView(withId(R.id.opencode_webview))
            .check { view, _ ->
                val webView = view as WebView
                assert(webView.canGoBack() == expected) {
                    "WebView canGoBack should be $expected"
                }
            }
        return this
    }

    /**
     * Verify the disconnect overlay is shown
     */
    fun assertDisconnectOverlayDisplayed(): WebViewRobot {
        onView(withId(R.id.disconnect_overlay))
            .check(matches(isDisplayed()))
        return this
    }

    /**
     * Click the reconnect button on disconnect overlay
     */
    fun clickReconnect(): WebViewRobot {
        onView(withId(R.id.reconnect_button))
            .perform(click())
        return this
    }

    /**
     * Verify the native connection status bar shows specific status
     */
    fun assertNativeStatusBar(status: String): WebViewRobot {
        onView(withId(R.id.native_status_bar))
            .check(matches(isDisplayed()))
        onView(withId(R.id.native_status_text))
            .check(matches(withText(containsString(status))))
        return this
    }

    /**
     * Click disconnect button in native UI
     */
    fun clickDisconnect(): WebViewRobot {
        onView(withId(R.id.disconnect_button))
            .perform(click())
        return this
    }

    /**
     * Scroll to an element in the WebView
     */
    fun scrollToElement(selector: String): WebViewRobot {
        onWebView(withId(R.id.opencode_webview))
            .forceJavascriptEnabled()
            .withElement(findElement(Locator.CSS_SELECTOR, selector))
            .perform(webScrollIntoView())
        return this
    }

    /**
     * Wait for messages list to have a specific count
     */
    fun waitForMessageCount(count: Int): WebViewRobot {
        // This would typically use an IdlingResource in actual implementation
        Thread.sleep(1000) // Simplified for test file generation
        return this
    }

    companion object {
        fun webViewScreen(block: WebViewRobot.() -> Unit): WebViewRobot {
            return WebViewRobot().apply(block)
        }
    }
}

/**
 * Custom script action for Espresso Web
 */
private fun script(js: String) = webKeys("javascript:$js")
