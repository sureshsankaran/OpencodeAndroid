package com.opencode.android.util

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.TimeUnit

/**
 * JUnit Rule that manages a MockWebServer for testing OpenCode web server connections.
 * Simulates the OpenCode web UI responses for E2E testing.
 */
class MockWebServerRule : TestRule {

    lateinit var server: MockWebServer
        private set

    val url: String
        get() = server.url("/").toString().removeSuffix("/")

    private var customDispatcher: Dispatcher? = null

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                server = MockWebServer()
                setupDefaultDispatcher()
                server.start()
                try {
                    base.evaluate()
                } finally {
                    server.shutdown()
                }
            }
        }
    }

    /**
     * Sets up the default dispatcher that serves the mock OpenCode web UI
     */
    private fun setupDefaultDispatcher() {
        server.dispatcher = customDispatcher ?: object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path == "/" || request.path == "/index.html" -> {
                        MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "text/html")
                            .setBody(getMockOpenCodeHtml())
                    }
                    request.path?.startsWith("/api/") == true -> {
                        handleApiRequest(request)
                    }
                    request.path?.endsWith(".js") == true -> {
                        MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/javascript")
                            .setBody(getMockJavaScript())
                    }
                    request.path?.endsWith(".css") == true -> {
                        MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "text/css")
                            .setBody(getMockCss())
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
    }

    /**
     * Handle mock API requests
     */
    private fun handleApiRequest(request: RecordedRequest): MockResponse {
        return when {
            request.path == "/api/health" -> {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"status": "ok", "version": "1.0.0"}""")
            }
            request.path == "/api/chat" && request.method == "POST" -> {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"response": "This is a mock AI response from the OpenCode server."}""")
            }
            request.path == "/api/session" -> {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"sessionId": "mock-session-123", "connected": true}""")
            }
            else -> MockResponse().setResponseCode(404)
        }
    }

    /**
     * Returns mock HTML that simulates the OpenCode web UI
     */
    private fun getMockOpenCodeHtml(): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>OpenCode</title>
                <link rel="stylesheet" href="/styles.css">
            </head>
            <body>
                <div id="app" class="opencode-container">
                    <header id="header" class="opencode-header">
                        <h1>OpenCode</h1>
                        <div id="connection-status" class="status-indicator connected">Connected</div>
                    </header>
                    <main id="chat-container" class="chat-container">
                        <div id="messages" class="messages-list">
                            <div class="message system">Welcome to OpenCode! How can I help you today?</div>
                        </div>
                    </main>
                    <footer id="input-area" class="input-area">
                        <form id="chat-form">
                            <input 
                                type="text" 
                                id="chat-input" 
                                class="chat-input" 
                                placeholder="Type your message..."
                                autocomplete="off"
                            />
                            <button type="submit" id="send-button" class="send-button">Send</button>
                        </form>
                    </footer>
                </div>
                <script src="/app.js"></script>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Returns mock JavaScript for the OpenCode web UI
     */
    private fun getMockJavaScript(): String {
        return """
            (function() {
                const form = document.getElementById('chat-form');
                const input = document.getElementById('chat-input');
                const messages = document.getElementById('messages');
                const statusIndicator = document.getElementById('connection-status');
                
                // Mark page as loaded
                window.openCodeLoaded = true;
                document.body.setAttribute('data-loaded', 'true');
                
                form.addEventListener('submit', async function(e) {
                    e.preventDefault();
                    const text = input.value.trim();
                    if (!text) return;
                    
                    // Add user message
                    const userMsg = document.createElement('div');
                    userMsg.className = 'message user';
                    userMsg.textContent = text;
                    userMsg.setAttribute('data-testid', 'user-message');
                    messages.appendChild(userMsg);
                    
                    input.value = '';
                    
                    // Simulate API call and response
                    try {
                        const response = await fetch('/api/chat', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ message: text })
                        });
                        const data = await response.json();
                        
                        const aiMsg = document.createElement('div');
                        aiMsg.className = 'message ai';
                        aiMsg.textContent = data.response;
                        aiMsg.setAttribute('data-testid', 'ai-message');
                        messages.appendChild(aiMsg);
                    } catch (error) {
                        const errorMsg = document.createElement('div');
                        errorMsg.className = 'message error';
                        errorMsg.textContent = 'Error: Could not send message';
                        messages.appendChild(errorMsg);
                    }
                });
                
                // Connection status handling
                window.setConnectionStatus = function(status) {
                    statusIndicator.className = 'status-indicator ' + status;
                    statusIndicator.textContent = status.charAt(0).toUpperCase() + status.slice(1);
                };
                
                // Expose for testing
                window.openCodeApp = {
                    sendMessage: function(text) {
                        input.value = text;
                        form.dispatchEvent(new Event('submit'));
                    },
                    getMessages: function() {
                        return Array.from(messages.querySelectorAll('.message')).map(m => ({
                            type: m.classList.contains('user') ? 'user' : 
                                  m.classList.contains('ai') ? 'ai' : 'system',
                            text: m.textContent
                        }));
                    }
                };
            })();
        """.trimIndent()
    }

    /**
     * Returns mock CSS for the OpenCode web UI
     */
    private fun getMockCss(): String {
        return """
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
            .opencode-container { display: flex; flex-direction: column; height: 100vh; }
            .opencode-header { padding: 16px; background: #1a1a2e; color: white; display: flex; justify-content: space-between; align-items: center; }
            .status-indicator { padding: 4px 12px; border-radius: 12px; font-size: 12px; }
            .status-indicator.connected { background: #4ade80; color: #166534; }
            .status-indicator.disconnected { background: #f87171; color: #991b1b; }
            .status-indicator.connecting { background: #fbbf24; color: #92400e; }
            .chat-container { flex: 1; overflow-y: auto; padding: 16px; background: #0f0f23; }
            .messages-list { display: flex; flex-direction: column; gap: 12px; }
            .message { padding: 12px 16px; border-radius: 8px; max-width: 80%; }
            .message.user { background: #3b82f6; color: white; align-self: flex-end; }
            .message.ai { background: #1e293b; color: #e2e8f0; align-self: flex-start; }
            .message.system { background: #334155; color: #94a3b8; align-self: center; font-style: italic; }
            .message.error { background: #dc2626; color: white; align-self: center; }
            .input-area { padding: 16px; background: #1a1a2e; border-top: 1px solid #334155; }
            #chat-form { display: flex; gap: 8px; }
            .chat-input { flex: 1; padding: 12px 16px; border: 1px solid #334155; border-radius: 8px; background: #0f0f23; color: white; font-size: 16px; }
            .chat-input:focus { outline: none; border-color: #3b82f6; }
            .send-button { padding: 12px 24px; background: #3b82f6; color: white; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; }
            .send-button:hover { background: #2563eb; }
        """.trimIndent()
    }

    /**
     * Configure custom dispatcher for specific test scenarios
     */
    fun setCustomDispatcher(dispatcher: Dispatcher) {
        customDispatcher = dispatcher
        if (::server.isInitialized) {
            server.dispatcher = dispatcher
        }
    }

    /**
     * Enqueue a specific response for the next request
     */
    fun enqueueResponse(response: MockResponse) {
        server.enqueue(response)
    }

    /**
     * Simulate server disconnection by returning errors
     */
    fun simulateDisconnection() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse()
                    .setResponseCode(503)
                    .setBody("Service Unavailable")
            }
        }
    }

    /**
     * Simulate slow network by adding delay to responses
     */
    fun simulateSlowNetwork(delayMs: Long = 3000) {
        val originalDispatcher = server.dispatcher
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return originalDispatcher.dispatch(request)
                    .setBodyDelay(delayMs, TimeUnit.MILLISECONDS)
            }
        }
    }

    /**
     * Restore normal server operation
     */
    fun restoreNormalOperation() {
        setupDefaultDispatcher()
    }

    /**
     * Get the number of requests received
     */
    fun getRequestCount(): Int = server.requestCount

    /**
     * Get a recorded request by index
     */
    fun getRecordedRequest(index: Int = 0): RecordedRequest? {
        return try {
            server.takeRequest(100, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            null
        }
    }
}
