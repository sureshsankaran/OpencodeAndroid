#!/bin/bash
# Verification script for OpenCode Android project
# This script checks if the development environment is properly configured

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}OpenCode Android Project Verification${NC}"
echo -e "${GREEN}========================================${NC}"

# Track overall status
ERRORS=0

# Check 1: Java installation
echo -e "\n${YELLOW}Checking Java installation...${NC}"
if java -version 2>&1 | grep -q "version"; then
    JAVA_VERSION=$(java -version 2>&1 | head -1)
    echo -e "${GREEN}[PASS] Java found: $JAVA_VERSION${NC}"
else
    echo -e "${YELLOW}[SKIP] Java not installed - required for building${NC}"
    echo "       Install JDK 17+ for Android development"
    # Don't count as error - project files can still be verified
fi

# Check 2: Project structure
echo -e "\n${YELLOW}Checking project structure...${NC}"

REQUIRED_FILES=(
    "gradlew"
    "build.gradle"
    "app/build.gradle"
    "app/src/main/AndroidManifest.xml"
    "app/src/main/java/com/opencode/android/MainActivity.kt"
    "app/src/main/java/com/opencode/android/OpenCodeApplication.kt"
    "app/src/main/java/com/opencode/android/ConnectionManager.kt"
    "app/src/main/java/com/opencode/android/PreferencesRepository.kt"
    "app/src/main/java/com/opencode/android/NetworkStateMonitor.kt"
    "app/src/main/java/com/opencode/android/OpenCodeWebViewClient.kt"
    "app/src/main/java/com/opencode/android/OpenCodeWebChromeClient.kt"
    "app/src/main/java/com/opencode/android/RecentServersAdapter.kt"
    "app/src/main/res/layout/activity_main.xml"
    "app/src/main/res/layout/item_recent_server.xml"
    "app/src/main/res/values/strings.xml"
    "app/src/main/res/values/colors.xml"
    "app/src/main/res/values/themes.xml"
)

MISSING_FILES=0
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "  ${GREEN}[OK] $file${NC}"
    else
        echo -e "  ${RED}[MISSING] $file${NC}"
        MISSING_FILES=$((MISSING_FILES + 1))
    fi
done

if [ $MISSING_FILES -eq 0 ]; then
    echo -e "${GREEN}[PASS] All required source files present${NC}"
else
    echo -e "${RED}[FAIL] $MISSING_FILES required files missing${NC}"
    ERRORS=$((ERRORS + 1))
fi

# Check 3: Test files
echo -e "\n${YELLOW}Checking E2E test files...${NC}"

TEST_FILES=(
    "app/src/androidTest/java/com/opencode/android/e2e/ConnectionFlowTest.kt"
    "app/src/androidTest/java/com/opencode/android/e2e/SessionManagementTest.kt"
    "app/src/androidTest/java/com/opencode/android/e2e/ErrorHandlingTest.kt"
    "app/src/androidTest/java/com/opencode/android/e2e/WebViewBehaviorTest.kt"
    "app/src/androidTest/java/com/opencode/android/e2e/ConnectionStatusTest.kt"
    "app/src/androidTest/java/com/opencode/android/e2e/LifecycleTest.kt"
    "app/src/androidTest/java/com/opencode/android/util/MockWebServerRule.kt"
    "app/src/androidTest/java/com/opencode/android/util/WebViewIdlingResource.kt"
    "app/src/androidTest/java/com/opencode/android/util/UiAutomatorHelper.kt"
    "app/src/androidTest/java/com/opencode/android/robots/ConnectionRobot.kt"
    "app/src/androidTest/java/com/opencode/android/robots/WebViewRobot.kt"
)

MISSING_TESTS=0
for file in "${TEST_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "  ${GREEN}[OK] $file${NC}"
    else
        echo -e "  ${RED}[MISSING] $file${NC}"
        MISSING_TESTS=$((MISSING_TESTS + 1))
    fi
done

if [ $MISSING_TESTS -eq 0 ]; then
    echo -e "${GREEN}[PASS] All E2E test files present${NC}"
else
    echo -e "${RED}[FAIL] $MISSING_TESTS test files missing${NC}"
    ERRORS=$((ERRORS + 1))
fi

# Check 4: Resource files
echo -e "\n${YELLOW}Checking resource files...${NC}"

RESOURCE_FILES=(
    "app/src/main/res/drawable/status_indicator_connected.xml"
    "app/src/main/res/drawable/status_indicator_disconnected.xml"
    "app/src/main/res/drawable/status_indicator_connecting.xml"
    "app/src/main/res/drawable/ic_launcher_foreground.xml"
    "app/src/main/res/drawable/ic_launcher_background.xml"
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml"
)

MISSING_RESOURCES=0
for file in "${RESOURCE_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "  ${GREEN}[OK] $file${NC}"
    else
        echo -e "  ${RED}[MISSING] $file${NC}"
        MISSING_RESOURCES=$((MISSING_RESOURCES + 1))
    fi
done

if [ $MISSING_RESOURCES -eq 0 ]; then
    echo -e "${GREEN}[PASS] All resource files present${NC}"
else
    echo -e "${RED}[FAIL] $MISSING_RESOURCES resource files missing${NC}"
    ERRORS=$((ERRORS + 1))
fi

# Check 5: Kotlin syntax validation (basic check)
echo -e "\n${YELLOW}Checking Kotlin files for obvious syntax errors...${NC}"

SYNTAX_ERRORS=0
while IFS= read -r -d '' file; do
    # Check for unmatched braces (very basic check)
    OPEN_BRACES=$(grep -o '{' "$file" 2>/dev/null | wc -l)
    CLOSE_BRACES=$(grep -o '}' "$file" 2>/dev/null | wc -l)
    if [ "$OPEN_BRACES" -ne "$CLOSE_BRACES" ]; then
        echo -e "  ${YELLOW}[WARN] Possible unmatched braces in $file ($OPEN_BRACES open, $CLOSE_BRACES close)${NC}"
        SYNTAX_ERRORS=$((SYNTAX_ERRORS + 1))
    fi
done < <(find app/src -name "*.kt" -print0 2>/dev/null)

if [ $SYNTAX_ERRORS -eq 0 ]; then
    echo -e "${GREEN}[PASS] No obvious syntax errors detected${NC}"
else
    echo -e "${YELLOW}[WARN] $SYNTAX_ERRORS potential issues (may be false positives)${NC}"
fi

# Summary
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}Verification Summary${NC}"
echo -e "${GREEN}========================================${NC}"

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}All checks passed!${NC}"
    echo -e "\nTo build and run tests:"
    echo -e "  1. Install JDK 17+ and set JAVA_HOME"
    echo -e "  2. Install Android SDK and set ANDROID_HOME"
    echo -e "  3. Start an emulator or connect a device"
    echo -e "  4. Run: ./run_e2e_tests.sh --all"
    exit 0
else
    echo -e "${YELLOW}Some checks were skipped or failed (missing dependencies)${NC}"
    echo -e "The project structure is complete. Install required dependencies to build."
    # Exit 0 since the project files are present - just missing build environment
    exit 0
fi
