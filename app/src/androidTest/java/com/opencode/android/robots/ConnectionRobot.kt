package com.opencode.android.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.Matchers.allOf
import com.opencode.android.R

/**
 * Robot pattern class for interacting with the Connection screen.
 * Provides a clean API for E2E tests to interact with the connection UI.
 */
class ConnectionRobot {

    /**
     * Verify that the connection screen is displayed
     */
    fun assertConnectionScreenDisplayed(): ConnectionRobot {
        onView(withId(R.id.connection_screen_container))
            .check(matches(isDisplayed()))
        return this
    }

    /**
     * Verify the app title is displayed
     */
    fun assertAppTitleDisplayed(): ConnectionRobot {
        onView(withId(R.id.app_title))
            .check(matches(isDisplayed()))
        onView(withId(R.id.app_title))
            .check(matches(withText("OpenCode")))
        return this
    }

    /**
     * Verify the URL input field is displayed
     */
    fun assertUrlInputDisplayed(): ConnectionRobot {
        onView(withId(R.id.server_url_input))
            .check(matches(isDisplayed()))
        return this
    }

    /**
     * Verify the connect button is displayed
     */
    fun assertConnectButtonDisplayed(): ConnectionRobot {
        onView(withId(R.id.connect_button))
            .check(matches(isDisplayed()))
        return this
    }

    /**
     * Enter a server URL in the input field
     */
    fun enterServerUrl(url: String): ConnectionRobot {
        onView(withId(R.id.server_url_input))
            .perform(clearText(), typeText(url), closeSoftKeyboard())
        return this
    }

    /**
     * Click the connect button
     */
    fun clickConnect(): ConnectionRobot {
        onView(withId(R.id.connect_button))
            .perform(click())
        return this
    }

    /**
     * Verify an error message is displayed
     */
    fun assertErrorDisplayed(errorMessage: String): ConnectionRobot {
        onView(withId(R.id.error_message))
            .check(matches(isDisplayed()))
        onView(withId(R.id.error_message))
            .check(matches(withText(errorMessage)))
        return this
    }

    /**
     * Verify the error message view is displayed (any error)
     */
    fun assertErrorViewDisplayed(): ConnectionRobot {
        onView(withId(R.id.error_message))
            .check(matches(isDisplayed()))
        return this
    }

    /**
     * Verify loading indicator is displayed
     */
    fun assertLoadingDisplayed(): ConnectionRobot {
        onView(withId(R.id.loading_indicator))
            .check(matches(isDisplayed()))
        return this
    }

    /**
     * Verify loading indicator is not displayed
     */
    fun assertLoadingNotDisplayed(): ConnectionRobot {
        onView(withId(R.id.loading_indicator))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        return this
    }

    /**
     * Verify the recent servers list is displayed
     */
    fun assertRecentServersDisplayed(): ConnectionRobot {
        onView(withId(R.id.recent_servers_list))
            .check(matches(isDisplayed()))
        return this
    }

    /**
     * Click on a recent server item
     */
    fun clickRecentServer(serverUrl: String): ConnectionRobot {
        onView(allOf(withId(R.id.recent_server_item), withText(serverUrl)))
            .perform(click())
        return this
    }

    /**
     * Clear the URL input field
     */
    fun clearUrlInput(): ConnectionRobot {
        onView(withId(R.id.server_url_input))
            .perform(clearText())
        return this
    }

    /**
     * Verify the URL input contains specific text
     */
    fun assertUrlInputContains(text: String): ConnectionRobot {
        onView(withId(R.id.server_url_input))
            .check(matches(withText(text)))
        return this
    }

    /**
     * Click the clear history button
     */
    fun clickClearHistory(): ConnectionRobot {
        onView(withId(R.id.clear_history_button))
            .perform(click())
        return this
    }

    /**
     * Verify settings button is displayed
     */
    fun assertSettingsButtonDisplayed(): ConnectionRobot {
        onView(withId(R.id.settings_button))
            .check(matches(isDisplayed()))
        return this
    }

    /**
     * Click the settings button
     */
    fun clickSettings(): ConnectionRobot {
        onView(withId(R.id.settings_button))
            .perform(click())
        return this
    }

    /**
     * Verify no recent servers message is displayed
     */
    fun assertNoRecentServersMessage(): ConnectionRobot {
        onView(withId(R.id.no_recent_servers_message))
            .check(matches(isDisplayed()))
        return this
    }

    /**
     * Verify the connection screen is NOT displayed (navigated away)
     */
    fun assertConnectionScreenNotDisplayed(): ConnectionRobot {
        onView(withId(R.id.connection_screen_container))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        return this
    }

    /**
     * Connect to server - combined action
     */
    fun connectToServer(url: String): ConnectionRobot {
        enterServerUrl(url)
        clickConnect()
        return this
    }

    companion object {
        fun connectionScreen(block: ConnectionRobot.() -> Unit): ConnectionRobot {
            return ConnectionRobot().apply(block)
        }
    }
}
