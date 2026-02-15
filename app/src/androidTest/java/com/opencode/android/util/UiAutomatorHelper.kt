package com.opencode.android.util

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull

/**
 * UI Automator helper for E2E testing on emulated devices.
 * Provides utilities for device-level interactions and cross-app testing.
 */
class UiAutomatorHelper {

    companion object {
        const val LAUNCH_TIMEOUT = 5000L
        const val UI_TIMEOUT = 3000L
        const val PACKAGE_NAME = "com.opencode.android"

        /**
         * Get the UiDevice instance for the current test
         */
        fun getDevice(): UiDevice {
            return UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        }

        /**
         * Launch the OpenCode app from home screen
         */
        fun launchApp(): UiDevice {
            val device = getDevice()
            
            // Start from home screen
            device.pressHome()
            
            // Wait for launcher
            val launcherPackage = device.launcherPackageName
            assertNotNull("Launcher package should not be null", launcherPackage)
            device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT)
            
            // Launch the app
            val context = ApplicationProvider.getApplicationContext<Context>()
            val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            assertNotNull("Launch intent should not be null", intent)
            context.startActivity(intent)
            
            // Wait for app to appear
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), LAUNCH_TIMEOUT)
            
            return device
        }

        /**
         * Wait for an element to appear by resource ID
         */
        fun waitForElement(device: UiDevice, resourceId: String, timeout: Long = UI_TIMEOUT): UiObject2? {
            return device.wait(
                Until.findObject(By.res(PACKAGE_NAME, resourceId)),
                timeout
            )
        }

        /**
         * Wait for element to appear by text
         */
        fun waitForText(device: UiDevice, text: String, timeout: Long = UI_TIMEOUT): UiObject2? {
            return device.wait(
                Until.findObject(By.text(text)),
                timeout
            )
        }

        /**
         * Wait for element to appear by text containing
         */
        fun waitForTextContaining(device: UiDevice, text: String, timeout: Long = UI_TIMEOUT): UiObject2? {
            return device.wait(
                Until.findObject(By.textContains(text)),
                timeout
            )
        }

        /**
         * Find element by resource ID
         */
        fun findById(device: UiDevice, resourceId: String): UiObject2? {
            return device.findObject(By.res(PACKAGE_NAME, resourceId))
        }

        /**
         * Find element by text
         */
        fun findByText(device: UiDevice, text: String): UiObject2? {
            return device.findObject(By.text(text))
        }

        /**
         * Click an element by resource ID
         */
        fun clickById(device: UiDevice, resourceId: String): Boolean {
            val element = waitForElement(device, resourceId)
            return element?.let {
                it.click()
                true
            } ?: false
        }

        /**
         * Enter text into an element by resource ID
         */
        fun enterText(device: UiDevice, resourceId: String, text: String): Boolean {
            val element = waitForElement(device, resourceId)
            return element?.let {
                it.click()
                it.text = text
                true
            } ?: false
        }

        /**
         * Clear text from an element by resource ID
         */
        fun clearText(device: UiDevice, resourceId: String): Boolean {
            val element = waitForElement(device, resourceId)
            return element?.let {
                it.clear()
                true
            } ?: false
        }

        /**
         * Check if an element is displayed
         */
        fun isDisplayed(device: UiDevice, resourceId: String): Boolean {
            return findById(device, resourceId) != null
        }

        /**
         * Check if text is displayed on screen
         */
        fun isTextDisplayed(device: UiDevice, text: String): Boolean {
            return findByText(device, text) != null
        }

        /**
         * Press back button
         */
        fun pressBack(device: UiDevice) {
            device.pressBack()
        }

        /**
         * Press home button
         */
        fun pressHome(device: UiDevice) {
            device.pressHome()
        }

        /**
         * Rotate device to landscape
         */
        fun rotateLandscape(device: UiDevice) {
            device.setOrientationLandscape()
        }

        /**
         * Rotate device to portrait
         */
        fun rotatePortrait(device: UiDevice) {
            device.setOrientationPortrait()
        }

        /**
         * Unfreeze rotation
         */
        fun unfreezeRotation(device: UiDevice) {
            device.unfreezeRotation()
        }

        /**
         * Take a screenshot
         */
        fun takeScreenshot(device: UiDevice, filename: String): Boolean {
            val file = java.io.File("/sdcard/Pictures/$filename.png")
            return device.takeScreenshot(file)
        }

        /**
         * Wait for app to be idle
         */
        fun waitForIdle(device: UiDevice, timeout: Long = UI_TIMEOUT) {
            device.waitForIdle(timeout)
        }

        /**
         * Scroll down in a scrollable container
         */
        fun scrollDown(device: UiDevice, resourceId: String): Boolean {
            val element = findById(device, resourceId)
            return element?.scroll(androidx.test.uiautomator.Direction.DOWN, 1.0f) ?: false
        }

        /**
         * Scroll up in a scrollable container
         */
        fun scrollUp(device: UiDevice, resourceId: String): Boolean {
            val element = findById(device, resourceId)
            return element?.scroll(androidx.test.uiautomator.Direction.UP, 1.0f) ?: false
        }

        /**
         * Enable WiFi (requires system permissions)
         */
        fun enableWifi(device: UiDevice) {
            device.executeShellCommand("svc wifi enable")
        }

        /**
         * Disable WiFi (requires system permissions)
         */
        fun disableWifi(device: UiDevice) {
            device.executeShellCommand("svc wifi disable")
        }

        /**
         * Enable mobile data (requires system permissions)
         */
        fun enableMobileData(device: UiDevice) {
            device.executeShellCommand("svc data enable")
        }

        /**
         * Disable mobile data (requires system permissions)
         */
        fun disableMobileData(device: UiDevice) {
            device.executeShellCommand("svc data disable")
        }

        /**
         * Clear app data
         */
        fun clearAppData(device: UiDevice) {
            device.executeShellCommand("pm clear $PACKAGE_NAME")
        }

        /**
         * Force stop the app
         */
        fun forceStopApp(device: UiDevice) {
            device.executeShellCommand("am force-stop $PACKAGE_NAME")
        }

        /**
         * Get current activity name
         */
        fun getCurrentActivity(device: UiDevice): String {
            return device.currentPackageName
        }

        /**
         * Verify we're in the OpenCode app
         */
        fun assertInApp(device: UiDevice) {
            val currentPackage = device.currentPackageName
            assert(currentPackage == PACKAGE_NAME) {
                "Expected to be in $PACKAGE_NAME but was in $currentPackage"
            }
        }
    }
}
