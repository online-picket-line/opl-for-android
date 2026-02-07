#!/usr/bin/env bash
# OPL for Android - Test Runner
# Runs unit tests via Gradle

set -euo pipefail

cd "$(dirname "$0")"

echo "========================================="
echo "  OPL for Android — Test Suite"
echo "========================================="
echo ""

# Check for Java
if [[ -z "${JAVA_HOME:-}" ]]; then
    if command -v java >/dev/null 2>&1; then
        export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(which java)")")")"
    else
        echo "❌ JAVA_HOME is not set and no 'java' command found in PATH." >&2
        echo "   Install a JDK and set JAVA_HOME to run Android tests." >&2
        exit 127
    fi
fi

# Auto-detect ANDROID_HOME
if [[ -z "${ANDROID_HOME:-}" ]]; then
    if [[ -d "$HOME/android-sdk" ]]; then
        export ANDROID_HOME="$HOME/android-sdk"
    elif [[ -d "/usr/lib/android-sdk" ]]; then
        export ANDROID_HOME="/usr/lib/android-sdk"
    else
        echo "❌ ANDROID_HOME is not set and no Android SDK found." >&2
        echo "   Install the Android SDK or set ANDROID_HOME." >&2
        exit 127
    fi
fi

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
