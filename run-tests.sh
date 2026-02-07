#!/usr/bin/env bash
# OPL for Android - Test Runner
# Runs unit tests via Gradle

set -euo pipefail

cd "$(dirname "$0")"

echo "========================================="
echo "  OPL for Android — Test Suite"
echo "========================================="
echo ""

# Locate the Gradle wrapper
if [[ -x "./gradlew" ]]; then
    GRADLE="./gradlew"
elif command -v gradle >/dev/null 2>&1; then
    GRADLE="$(command -v gradle)"
else
    echo "❌ Neither ./gradlew nor gradle found." >&2
    echo "   Run:  gradle wrapper  to generate the wrapper." >&2
    exit 127
fi

echo "Using: $GRADLE"
echo ""

# Run unit tests (local JVM tests, no emulator needed)
"$GRADLE" testDebugUnitTest --console=plain 2>&1

echo ""
echo "========================================="
echo "  Tests complete!"
echo "========================================="
