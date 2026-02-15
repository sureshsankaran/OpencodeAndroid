package com.opencode.android

import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Custom WebChromeClient for handling JavaScript dialogs, 
 * file uploads, and progress updates.
 */
class OpenCodeWebChromeClient(
    private val listener: WebChromeClientListener
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        listener.onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        listener.onReceivedTitle(title)
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        listener.onJsAlert(message ?: "")
        result?.confirm()
        return true
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        listener.onJsConfirm(message ?: "") { confirmed ->
            if (confirmed) result?.confirm() else result?.cancel()
        }
        return true
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        listener.onShowFileChooser(filePathCallback, fileChooserParams)
        return true
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        // Log console messages for debugging
        consoleMessage?.let {
            listener.onConsoleMessage(
                it.messageLevel(),
                it.message(),
                it.lineNumber(),
                it.sourceId()
            )
        }
        return true
    }

    /**
     * Listener interface for WebChromeClient events
     */
    interface WebChromeClientListener {
        fun onProgressChanged(progress: Int)
        fun onReceivedTitle(title: String?)
        fun onJsAlert(message: String)
        fun onJsConfirm(message: String, callback: (Boolean) -> Unit)
        fun onShowFileChooser(
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        )
        fun onConsoleMessage(
            level: ConsoleMessage.MessageLevel,
            message: String,
            lineNumber: Int,
            sourceId: String
        )
    }
}
