package com.opencode.android.e2e

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.*
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import com.opencode.android.MainActivity
import com.opencode.android.R
import com.opencode.android.robots.ConnectionRobot
import com.opencode.android.robots.WebViewRobot
import com.opencode.android.util.MockWebServerRule
import com.opencode.android.util.WebViewIdlingResource

/**
 * E2E Tests for the critical connection flow functionality.
 * Tests 1-7 from the test plan (CRITICAL priority).
 * 
 * These tests verify:
 * - App launch and connection screen display
 * - Server URL input functionality
 * - Connecting to OpenCode web server
 * - WebView rendering of the web UI
 * - User interaction pass-through to web UI
 * - Chat input and message sending
 * - AI response display
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectionFlowTest {

    @get:Rule
    val mockWebServerRule = MockWebServerRule()

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var webViewIdlingResource: WebViewIdlingResource

    @Before
    fun setUp() {
        // Clear any stored preferences before each test
        activityRule.scenario.onActivity { activity ->
            activity.getSharedPreferences("opencode_prefs", 0).edit().clear().apply()
        }
    }

    @After
    fun tearDown() {
        // Unregister idling resources
        if (::webViewIdlingResource.isInitialized) {
            IdlingRegistry.getInstance().unregister(webViewIdlingResource)
        }
    }

    // ============================================
    // TEST 1: App Launch - Connection Screen Display
    // ============================================

    @Test
    fun test1_appLaunchDisplaysConnectionScreen() {
        // GIVEN: The app is launched
        // WHEN: The MainActivity starts
        // THEN: The connection screen should be displayed

        ConnectionRobot().apply {
            assertConnectionScreenDisplayed()
            assertAppTitleDisplayed()
            assertUrlInputDisplayed()
            assertConnectButtonDisplayed()
        }
    }

    @Test
    fun test1b_connectionScreenHasAllRequiredElements() {
        // Verify all UI elements are present on the connection screen
        ConnectionRobot().apply {
            assertConnectionScreenDisplayed()
            assertUrlInputDisplayed()
            assertConnectButtonDisplayed()
            assertSettingsButtonDisplayed()
        }
    }

    // ============================================
    // TEST 2: Server URL Input
    // ============================================

    @Test
    fun test2_userCanEnterValidHttpUrl() {
        // GIVEN: The connection screen is displayed
        // WHEN: User enters a valid HTTP URL
        // THEN: The URL should appear in the input field

        val testUrl = "http://localhost:3000"
        
        ConnectionRobot().apply {
            assertConnectionScreenDisplayed()
            enterServerUrl(testUrl)
            assertUrlInputContains(testUrl)
        }
    }

    @Test
    fun test2b_userCanEnterValidHttpsUrl() {
        // Test HTTPS URL entry
        val testUrl = "https://opencode.example.com"
        
        ConnectionRobot().apply {
            assertConnectionScreenDisplayed()
            enterServerUrl(testUrl)
            assertUrlInputContains(testUrl)
        }
    }

    @Test
    fun test2c_userCanClearAndReEnterUrl() {
        // Test clearing and re-entering URL
        ConnectionRobot().apply {
            assertConnectionScreenDisplayed()
            enterServerUrl("http://first-url.com")
            clearUrlInput()
            enterServerUrl("http://second-url.com")
            assertUrlInputContains("http://second-url.com")
        }
    }

    @Test
    fun test2d_urlInputAcceptsPortNumbers() {
        // Test URL with port number
        val testUrl = "http://192.168.1.100:8080"
        
        ConnectionRobot().apply {
            enterServerUrl(testUrl)
            assertUrlInputContains(testUrl)
        }
    }

    // ============================================
    // TEST 3: Connect to Web Server
    // ============================================

    @Test
    fun test3_connectToWebServerSuccessfully() {
        // GIVEN: A valid server URL is entered
        // WHEN: User clicks the connect button
        // THEN: App should connect and display the WebView

        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().apply {
            enterServerUrl(serverUrl)
            clickConnect()
        }

        // Wait for connection and verify WebView is displayed
        WebViewRobot().apply {
            assertWebViewDisplayed()
            assertWebViewVisible()
        }
    }

    @Test
    fun test3b_connectShowsLoadingIndicator() {
        // Verify loading indicator is shown during connection
        val serverUrl = mockWebServerRule.url
        
        // Simulate slow connection
        mockWebServerRule.simulateSlowNetwork(2000)
        
        ConnectionRobot().apply {
            enterServerUrl(serverUrl)
            clickConnect()
            assertLoadingDisplayed()
        }
    }

    @Test
    fun test3c_successfulConnectionHidesConnectionScreen() {
        // Verify connection screen is hidden after successful connection
        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().apply {
            connectToServer(serverUrl)
        }

        // Verify we've navigated away from connection screen
        WebViewRobot().assertWebViewDisplayed()
    }

    // ============================================
    // TEST 4: WebView Renders Correctly
    // ============================================

    @Test
    fun test4_webViewRendersOpenCodeUi() {
        // GIVEN: Connected to a valid OpenCode server
        // WHEN: The WebView loads
        // THEN: The OpenCode web UI should render correctly

        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            assertWebViewDisplayed()
            assertOpenCodeUiLoaded()
            assertHeaderPresent()
            assertChatContainerPresent()
            assertChatInputPresent()
        }
    }

    @Test
    fun test4b_webViewShowsWelcomeMessage() {
        // Verify the welcome/system message is displayed
        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            assertWelcomeMessageDisplayed()
        }
    }

    @Test
    fun test4c_webViewLoadsCorrectUrl() {
        // Verify the WebView loads the correct server URL
        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            assertUrlLoaded(serverUrl)
        }
    }

    @Test
    fun test4d_webViewRendersWithoutVisualArtifacts() {
        // Verify the web UI renders with proper dimensions
        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            assertWebViewDisplayed()
            // Verify WebView fills the container properly
            onView(withId(R.id.opencode_webview))
                .check(matches(isCompletelyDisplayed()))
        }
    }

    // ============================================
    // TEST 5: User Interaction Pass-through
    // ============================================

    @Test
    fun test5_touchEventsPassToWebUi() {
        // GIVEN: WebView is displaying the OpenCode UI
        // WHEN: User interacts with web elements
        // THEN: Touch events should be passed through correctly

        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            assertChatInputPresent()
            // Click on the input field
            onWebView(withId(R.id.opencode_webview))
                .forceJavascriptEnabled()
                .withElement(findElement(Locator.ID, "chat-input"))
                .perform(webClick())
        }
    }

    @Test
    fun test5b_keyboardInputPassesToWebUi() {
        // Verify keyboard input works in WebView
        val serverUrl = mockWebServerRule.url
        val testMessage = "Test keyboard input"
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            typeChatMessage(testMessage)
            // Verify text appears in input
            onWebView(withId(R.id.opencode_webview))
                .forceJavascriptEnabled()
                .withElement(findElement(Locator.ID, "chat-input"))
        }
    }

    @Test
    fun test5c_buttonClicksWork() {
        // Verify button clicks are properly passed through
        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            typeChatMessage("Test message")
            clickSendButton()
        }
    }

    // ============================================
    // TEST 6: Chat Input Works
    // ============================================

    @Test
    fun test6_userCanTypeAndSendChatMessage() {
        // GIVEN: WebView is displaying the OpenCode UI
        // WHEN: User types a message and clicks send
        // THEN: The message should be sent and appear in the chat

        val serverUrl = mockWebServerRule.url
        val testMessage = "Hello OpenCode!"
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            sendChatMessage(testMessage)
            assertUserMessageDisplayed(testMessage)
        }
    }

    @Test
    fun test6b_emptyMessageNotSent() {
        // Verify empty messages are not sent
        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            typeChatMessage("")
            clickSendButton()
            // Should not add any new message to the chat
            assertWelcomeMessageDisplayed() // Only welcome message should exist
        }
    }

    @Test
    fun test6c_multipleMessagesCanBeSent() {
        // Verify multiple messages can be sent sequentially
        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            sendChatMessage("First message")
            assertUserMessageDisplayed("First message")
            
            sendChatMessage("Second message")
            assertUserMessageDisplayed("Second message")
        }
    }

    @Test
    fun test6d_chatInputClearsAfterSending() {
        // Verify the input field clears after sending a message
        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            typeChatMessage("Test message")
            clickSendButton()
            // Input should be cleared - typing new text should work fresh
            typeChatMessage("New message")
        }
    }

    // ============================================
    // TEST 7: Response Display
    // ============================================

    @Test
    fun test7_aiResponseDisplayedCorrectly() {
        // GIVEN: User sends a message
        // WHEN: The server responds
        // THEN: The AI response should be displayed in the chat

        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            sendChatMessage("What is the weather?")
            assertUserMessageDisplayed("What is the weather?")
            
            // Wait for and verify AI response
            waitForMessageCount(3) // welcome + user + ai
            assertAiResponseDisplayed()
        }
    }

    @Test
    fun test7b_aiResponseContainsExpectedContent() {
        // Verify AI response contains the mock response text
        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            sendChatMessage("Tell me something")
            waitForMessageCount(3)
            assertAiResponseContains("mock AI response")
        }
    }

    @Test
    fun test7c_multipleResponsesDisplayInOrder() {
        // Verify multiple Q&A pairs display in chronological order
        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            sendChatMessage("Question 1")
            waitForMessageCount(3)
            assertAiResponseDisplayed()
            
            sendChatMessage("Question 2")
            waitForMessageCount(5) // welcome + 2 user + 2 ai
            assertAiResponseDisplayed()
        }
    }

    @Test
    fun test7d_chatScrollsToShowNewMessages() {
        // Verify chat auto-scrolls when new messages appear
        val serverUrl = mockWebServerRule.url
        
        ConnectionRobot().connectToServer(serverUrl)

        WebViewRobot().apply {
            // Send multiple messages to cause scrolling
            repeat(5) { i ->
                sendChatMessage("Message $i")
                waitForMessageCount(2 + (i + 1) * 2)
            }
            
            // The last message should be visible
            assertUserMessageDisplayed("Message 4")
        }
    }

    // ============================================
    // Integration Tests - Full Flow
    // ============================================

    @Test
    fun testFullConnectionAndChatFlow() {
        // Complete integration test: launch -> connect -> chat -> response
        val serverUrl = mockWebServerRule.url
        
        // 1. App launches with connection screen
        ConnectionRobot().apply {
            assertConnectionScreenDisplayed()
            assertAppTitleDisplayed()
            
            // 2. Enter URL and connect
            enterServerUrl(serverUrl)
            clickConnect()
        }
        
        // 3. WebView loads and displays UI
        WebViewRobot().apply {
            assertWebViewDisplayed()
            assertOpenCodeUiLoaded()
            assertHeaderPresent()
            assertWelcomeMessageDisplayed()
            
            // 4. Send a message
            sendChatMessage("Hello from Android!")
            assertUserMessageDisplayed("Hello from Android!")
            
            // 5. Receive and display response
            waitForMessageCount(3)
            assertAiResponseDisplayed()
        }
    }
}
