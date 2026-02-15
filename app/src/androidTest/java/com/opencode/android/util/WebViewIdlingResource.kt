package com.opencode.android.util

import android.webkit.WebView
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Espresso IdlingResource for WebView operations.
 * Waits for WebView to finish loading before allowing test assertions.
 */
class WebViewIdlingResource(
    private val webView: WebView,
    private val resourceName: String = "WebViewIdlingResource"
) : IdlingResource {

    @Volatile
    private var callback: ResourceCallback? = null
    private val isIdle = AtomicBoolean(true)

    override fun getName(): String = resourceName

    override fun isIdleNow(): Boolean {
        val idle = checkIfIdle()
        if (idle && !isIdle.get()) {
            isIdle.set(true)
            callback?.onTransitionToIdle()
        }
        return idle
    }

    override fun registerIdleTransitionCallback(callback: ResourceCallback?) {
        this.callback = callback
    }

    private fun checkIfIdle(): Boolean {
        return webView.progress == 100
    }

    fun setIdle(idle: Boolean) {
        isIdle.set(idle)
        if (idle) {
            callback?.onTransitionToIdle()
        }
    }
}

/**
 * IdlingResource that waits for a JavaScript condition to be true in WebView.
 * Useful for waiting on async operations within the web UI.
 */
class JavaScriptIdlingResource(
    private val webView: WebView,
    private val condition: String,
    private val resourceName: String = "JavaScriptIdlingResource"
) : IdlingResource {

    @Volatile
    private var callback: ResourceCallback? = null
    
    @Volatile
    private var isIdle = false

    override fun getName(): String = resourceName

    override fun isIdleNow(): Boolean {
        if (isIdle) return true
        
        webView.evaluateJavascript(condition) { result ->
            if (result == "true") {
                isIdle = true
                callback?.onTransitionToIdle()
            }
        }
        return isIdle
    }

    override fun registerIdleTransitionCallback(callback: ResourceCallback?) {
        this.callback = callback
    }
}

/**
 * IdlingResource that waits for OpenCode web UI to be fully loaded.
 * Checks for the window.openCodeLoaded flag set by the JavaScript.
 */
class OpenCodeLoadedIdlingResource(
    private val webView: WebView
) : IdlingResource {

    @Volatile
    private var callback: ResourceCallback? = null
    
    @Volatile
    private var isIdle = false

    override fun getName(): String = "OpenCodeLoadedIdlingResource"

    override fun isIdleNow(): Boolean {
        if (isIdle) return true

        // Check if page is loaded and OpenCode app is initialized
        webView.evaluateJavascript(
            "(function() { return window.openCodeLoaded === true && document.body.getAttribute('data-loaded') === 'true'; })()"
        ) { result ->
            if (result == "true") {
                isIdle = true
                callback?.onTransitionToIdle()
            }
        }
        return isIdle
    }

    override fun registerIdleTransitionCallback(callback: ResourceCallback?) {
        this.callback = callback
    }

    fun reset() {
        isIdle = false
    }
}

/**
 * IdlingResource that waits for a specific element to appear in WebView.
 */
class ElementPresentIdlingResource(
    private val webView: WebView,
    private val selector: String,
    private val resourceName: String = "ElementPresentIdlingResource"
) : IdlingResource {

    @Volatile
    private var callback: ResourceCallback? = null
    
    @Volatile
    private var isIdle = false

    override fun getName(): String = resourceName

    override fun isIdleNow(): Boolean {
        if (isIdle) return true

        webView.evaluateJavascript(
            "(function() { return document.querySelector('$selector') !== null; })()"
        ) { result ->
            if (result == "true") {
                isIdle = true
                callback?.onTransitionToIdle()
            }
        }
        return isIdle
    }

    override fun registerIdleTransitionCallback(callback: ResourceCallback?) {
        this.callback = callback
    }
}

/**
 * IdlingResource that waits for messages to appear in the chat container.
 */
class ChatMessageIdlingResource(
    private val webView: WebView,
    private val expectedMessageCount: Int,
    private val messageType: String? = null // "user", "ai", "system", or null for any
) : IdlingResource {

    @Volatile
    private var callback: ResourceCallback? = null
    
    @Volatile
    private var isIdle = false

    override fun getName(): String = "ChatMessageIdlingResource"

    override fun isIdleNow(): Boolean {
        if (isIdle) return true

        val selector = when (messageType) {
            null -> ".message"
            else -> ".message.$messageType"
        }

        webView.evaluateJavascript(
            "(function() { return document.querySelectorAll('$selector').length >= $expectedMessageCount; })()"
        ) { result ->
            if (result == "true") {
                isIdle = true
                callback?.onTransitionToIdle()
            }
        }
        return isIdle
    }

    override fun registerIdleTransitionCallback(callback: ResourceCallback?) {
        this.callback = callback
    }
}
