package com.opencode.android

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Custom WebViewClient for handling OpenCode web UI loading.
 * Manages page loading, error handling, and URL navigation.
 */
class OpenCodeWebViewClient(
    private val listener: WebViewClientListener
) : WebViewClient() {

    private var hasError = false

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        hasError = false
        listener.onPageStarted(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (!hasError) {
            listener.onPageFinished(url)
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        
        // Allow navigation within the same domain
        // External links could be handled differently if needed
        return false
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        
        // Only handle main frame errors
        if (request?.isForMainFrame == true) {
            hasError = true
            val errorMessage = when (error?.errorCode) {
                ERROR_HOST_LOOKUP -> "Server not found"
                ERROR_CONNECT -> "Could not connect to server"
                ERROR_TIMEOUT -> "Connection timed out"
                ERROR_IO -> "Network error"
                ERROR_FAILED_SSL_HANDSHAKE -> "SSL connection failed"
                else -> error?.description?.toString() ?: "Connection failed"
            }
            listener.onError(errorMessage)
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        // For development, we might want to proceed with self-signed certs
        // In production, this should be more restrictive
        val errorMessage = when (error?.primaryError) {
            SslError.SSL_EXPIRED -> "SSL certificate expired"
            SslError.SSL_IDMISMATCH -> "SSL certificate hostname mismatch"
            SslError.SSL_NOTYETVALID -> "SSL certificate not yet valid"
            SslError.SSL_UNTRUSTED -> "SSL certificate not trusted"
            else -> "SSL error"
        }
        
        // Cancel by default for security
        handler?.cancel()
        listener.onSslError(errorMessage)
    }

    /**
     * Listener interface for WebView events
     */
    interface WebViewClientListener {
        fun onPageStarted(url: String?)
        fun onPageFinished(url: String?)
        fun onError(message: String)
        fun onSslError(message: String)
    }
}
