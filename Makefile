# OpenCode Android Makefile
# Provides test targets for the TDD system

.PHONY: test verify build clean

# Default test target - runs project verification
test:
	@./verify_project.sh

# Verify project structure
verify:
	@./verify_project.sh

# Build the Android project (requires Java/Android SDK)
build:
	@./gradlew assembleDebug

# Run E2E tests on device/emulator (requires Android environment)
test-e2e:
	@./run_e2e_tests.sh --all

# Clean build artifacts
clean:
	@./gradlew clean 2>/dev/null || true
