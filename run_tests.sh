#!/bin/bash
# Wrapper script for running tests
# Falls back to verify_project.sh if bun is not available

set -e

if command -v bun &> /dev/null; then
    echo "Running tests with bun..."
    bun test tests/**/*.e2e.ts
else
    echo "Bun not available, running project verification..."
    ./verify_project.sh
fi
