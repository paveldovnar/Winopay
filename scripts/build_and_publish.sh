#!/bin/bash
set -euo pipefail

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# WinoPay - Build and Publish APK
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
#
# Builds APK and publishes to Telegram channel.
#
# Usage:
#   ./scripts/build_and_publish.sh <variant>
#
# Variants:
#   devnetDebug      - Devnet debug build
#   devnetRelease    - Devnet release build
#   mainnetDebug     - Mainnet debug build
#   mainnetRelease   - Mainnet release build
#
# Example:
#   TG_BOT_TOKEN=xxx TG_CHAT_ID=@channel ./scripts/build_and_publish.sh devnetDebug
#
# Environment Variables (REQUIRED for publish):
#   TG_BOT_TOKEN - Telegram Bot API token (optional - skips publish if not set)
#   TG_CHAT_ID   - Telegram chat ID or @channel_name (optional)
#
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

VARIANT="${1:-devnetDebug}"

# Determine build type
case "$VARIANT" in
    devnetDebug)
        GRADLE_TASK="assembleDevnetDebug"
        APK_DIR="app/build/outputs/apk/devnet/debug"
        APK_PATTERN="app-devnet-debug.apk"
        ;;
    devnetRelease)
        GRADLE_TASK="assembleDevnetRelease"
        APK_DIR="app/build/outputs/apk/devnet/release"
        APK_PATTERN="app-devnet-release.apk"
        ;;
    mainnetDebug)
        GRADLE_TASK="assembleMainnetDebug"
        APK_DIR="app/build/outputs/apk/mainnet/debug"
        APK_PATTERN="app-mainnet-debug.apk"
        ;;
    mainnetRelease)
        GRADLE_TASK="assembleMainnetRelease"
        APK_DIR="app/build/outputs/apk/mainnet/release"
        APK_PATTERN="app-mainnet-release.apk"
        ;;
    *)
        echo "ERROR: Unknown variant: $VARIANT"
        echo "Valid variants: devnetDebug, devnetRelease, mainnetDebug, mainnetRelease"
        exit 1
        ;;
esac

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Building WinoPay - $VARIANT"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Set JAVA_HOME if not set
if [[ -z "${JAVA_HOME:-}" ]]; then
    if [[ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]]; then
        export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
        echo "JAVA_HOME set to: $JAVA_HOME"
    fi
fi

# Build APK
echo ""
echo "Running: ./gradlew $GRADLE_TASK"
./gradlew "$GRADLE_TASK"

# Find APK
APK_PATH="$APK_DIR/$APK_PATTERN"
if [[ ! -f "$APK_PATH" ]]; then
    echo "ERROR: APK not found at: $APK_PATH"
    echo "Build may have failed or APK path is incorrect"
    exit 1
fi

echo ""
echo "✅ Build successful: $APK_PATH"
echo "Size: $(du -h "$APK_PATH" | cut -f1)"

# Extract version info from build.gradle.kts
VERSION_NAME=$(grep 'versionName = ' app/build.gradle.kts | head -1 | sed 's/.*versionName = "\(.*\)".*/\1/')
VERSION_CODE=$(grep 'versionCode = ' app/build.gradle.kts | head -1 | sed 's/.*versionCode = \(.*\)/\1/')

# Get git info
GIT_SHA=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")

# Get timestamp
TIMESTAMP=$(date -u '+%Y-%m-%d %H:%M:%S UTC')

# Build caption
CAPTION="WinoPay $VARIANT v$VERSION_NAME ($VERSION_CODE)
Git: $GIT_SHA ($GIT_BRANCH)
Built: $TIMESTAMP"

echo ""
echo "Version:   v$VERSION_NAME ($VERSION_CODE)"
echo "Git:       $GIT_SHA ($GIT_BRANCH)"
echo "Timestamp: $TIMESTAMP"

# Check if we should publish to Telegram
if [[ -z "${TG_BOT_TOKEN:-}" ]] || [[ -z "${TG_CHAT_ID:-}" ]]; then
    echo ""
    echo "⚠️  Skipping Telegram publish (TG_BOT_TOKEN or TG_CHAT_ID not set)"
    echo ""
    echo "To publish to Telegram, set environment variables:"
    echo "  export TG_BOT_TOKEN=your_bot_token"
    echo "  export TG_CHAT_ID=@your_channel"
    echo ""
    exit 0
fi

# Publish to Telegram
echo ""
./scripts/publish_telegram.sh "$APK_PATH" "$CAPTION"
