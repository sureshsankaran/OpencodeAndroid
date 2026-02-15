package com.opencode.android.util

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Custom test runner for OpenCode E2E tests.
 * Configures the test environment for UI Automator and Espresso tests.
 */
class OpenCodeTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        // Use the test application for isolated testing
        return super.newApplication(classLoader, TestApplication::class.java.name, context)
    }

    override fun onStart() {
        // Disable animations for more reliable UI testing
        disableAnimations()
        super.onStart()
    }

    override fun onDestroy() {
        // Re-enable animations after tests complete
        enableAnimations()
        super.onDestroy()
    }

    private fun disableAnimations() {
        try {
            val instrumentation = this
            instrumentation.uiAutomation.executeShellCommand(
                "settings put global window_animation_scale 0"
            ).close()
            instrumentation.uiAutomation.executeShellCommand(
                "settings put global transition_animation_scale 0"
            ).close()
            instrumentation.uiAutomation.executeShellCommand(
                "settings put global animator_duration_scale 0"
            ).close()
        } catch (e: Exception) {
            // Ignore - animations may already be disabled
        }
    }

    private fun enableAnimations() {
        try {
            val instrumentation = this
            instrumentation.uiAutomation.executeShellCommand(
                "settings put global window_animation_scale 1"
            ).close()
            instrumentation.uiAutomation.executeShellCommand(
                "settings put global transition_animation_scale 1"
            ).close()
            instrumentation.uiAutomation.executeShellCommand(
                "settings put global animator_duration_scale 1"
            ).close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}

/**
 * Test Application class for isolated testing environment.
 */
class TestApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize test-specific configurations
        initTestEnvironment()
    }

    private fun initTestEnvironment() {
        // Clear any cached data for clean test state
        getSharedPreferences("opencode_prefs", MODE_PRIVATE).edit().clear().apply()
        
        // Set test mode flag
        getSharedPreferences("opencode_test", MODE_PRIVATE)
            .edit()
            .putBoolean("is_test_mode", true)
            .apply()
    }
}
