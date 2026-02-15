#!/bin/bash
# Script to run all OpenCode Android E2E tests with UI Automator
# Requires an emulator or device to be running

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}OpenCode Android E2E Test Runner${NC}"
echo -e "${GREEN}========================================${NC}"

# Check if ANDROID_HOME is set
if [ -z "$ANDROID_HOME" ]; then
    echo -e "${RED}Error: ANDROID_HOME is not set${NC}"
    echo "Please set ANDROID_HOME to your Android SDK path"
    exit 1
fi

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo -e "${RED}Error: adb not found${NC}"
    echo "Please ensure Android SDK platform-tools is in your PATH"
    exit 1
fi

# Check for connected devices/emulators
echo -e "\n${YELLOW}Checking for connected devices...${NC}"
DEVICES=$(adb devices | grep -v "List" | grep -v "^$" | wc -l)
if [ "$DEVICES" -eq 0 ]; then
    echo -e "${RED}Error: No devices or emulators connected${NC}"
    echo ""
    echo "Please start an emulator or connect a device:"
    echo "  - To start an emulator: emulator -avd <avd_name>"
    echo "  - To list available AVDs: emulator -list-avds"
    exit 1
fi

echo -e "${GREEN}Found $DEVICES device(s)${NC}"
adb devices

# Disable animations for reliable testing
echo -e "\n${YELLOW}Disabling animations for testing...${NC}"
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

# Clean and build the project
echo -e "\n${YELLOW}Building the project...${NC}"
./gradlew clean assembleDebug assembleAndroidTest

# Uninstall existing app and test APK
echo -e "\n${YELLOW}Uninstalling existing app...${NC}"
adb uninstall com.opencode.android 2>/dev/null || true
adb uninstall com.opencode.android.test 2>/dev/null || true

# Install the app and test APK
echo -e "\n${YELLOW}Installing APKs...${NC}"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

# Function to run specific test class
run_test_class() {
    local TEST_CLASS=$1
    echo -e "\n${YELLOW}Running: $TEST_CLASS${NC}"
    adb shell am instrument -w \
        -e class "com.opencode.android.e2e.$TEST_CLASS" \
        -e clearPackageData true \
        com.opencode.android.test/com.opencode.android.util.OpenCodeTestRunner
}

# Parse command line arguments
TEST_FILTER=""
while [[ $# -gt 0 ]]; do
    case $1 in
        --test)
            TEST_FILTER="$2"
            shift 2
            ;;
        --connection)
            TEST_FILTER="ConnectionFlowTest"
            shift
            ;;
        --session)
            TEST_FILTER="SessionManagementTest"
            shift
            ;;
        --error)
            TEST_FILTER="ErrorHandlingTest"
            shift
            ;;
        --webview)
            TEST_FILTER="WebViewBehaviorTest"
            shift
            ;;
        --status)
            TEST_FILTER="ConnectionStatusTest"
            shift
            ;;
        --lifecycle)
            TEST_FILTER="LifecycleTest"
            shift
            ;;
        --all)
            TEST_FILTER=""
            shift
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --test <TestClass>   Run a specific test class"
            echo "  --connection         Run ConnectionFlowTest"
            echo "  --session            Run SessionManagementTest"
            echo "  --error              Run ErrorHandlingTest"
            echo "  --webview            Run WebViewBehaviorTest"
            echo "  --status             Run ConnectionStatusTest"
            echo "  --lifecycle          Run LifecycleTest"
            echo "  --all                Run all tests (default)"
            echo "  --help               Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Run tests
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}Running E2E Tests${NC}"
echo -e "${GREEN}========================================${NC}"

if [ -n "$TEST_FILTER" ]; then
    run_test_class "$TEST_FILTER"
else
    echo -e "\n${YELLOW}Running all E2E tests...${NC}"
    
    # Run all test classes
    adb shell am instrument -w \
        -e package com.opencode.android.e2e \
        -e clearPackageData true \
        com.opencode.android.test/com.opencode.android.util.OpenCodeTestRunner
fi

# Re-enable animations
echo -e "\n${YELLOW}Re-enabling animations...${NC}"
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}Tests completed!${NC}"
echo -e "${GREEN}========================================${NC}"
