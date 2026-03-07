#!/bin/bash
set -euo pipefail

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# WinoPay - Publish APK to Telegram
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
#
# Usage:
#   TG_BOT_TOKEN=xxx TG_CHAT_ID=@channel ./scripts/publish_telegram.sh <apk_path> <caption>
#
# Example:
#   TG_BOT_TOKEN=123:ABC TG_CHAT_ID=@winopay_builds \
#     ./scripts/publish_telegram.sh app/build/outputs/apk/devnet/debug/app-devnet-debug.apk \
#     "WinoPay devnetDebug v1.0.1 (2)
#      abc123def
#      2024-01-15 12:30:00 UTC"
#
# Environment Variables (REQUIRED):
#   TG_BOT_TOKEN - Telegram Bot API token
#   TG_CHAT_ID   - Telegram chat ID or @channel_name
#
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

APK_PATH="${1:-}"
CAPTION="${2:-}"

# Validate inputs
if [[ -z "$APK_PATH" ]]; then
    echo "ERROR: APK path is required"
    echo "Usage: $0 <apk_path> <caption>"
    exit 1
fi

if [[ ! -f "$APK_PATH" ]]; then
    echo "ERROR: APK file not found: $APK_PATH"
    exit 1
fi

if [[ -z "$CAPTION" ]]; then
    echo "ERROR: Caption is required"
    echo "Usage: $0 <apk_path> <caption>"
    exit 1
fi

# Validate environment variables
if [[ -z "${TG_BOT_TOKEN:-}" ]]; then
    echo "ERROR: TG_BOT_TOKEN environment variable is not set"
    echo "Set it to your Telegram Bot API token"
    exit 1
fi

if [[ -z "${TG_CHAT_ID:-}" ]]; then
    echo "ERROR: TG_CHAT_ID environment variable is not set"
    echo "Set it to your Telegram chat ID or @channel_name"
    exit 1
fi

# Log (without exposing secrets)
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Publishing APK to Telegram"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "APK:     $APK_PATH"
echo "Size:    $(du -h "$APK_PATH" | cut -f1)"
echo "Chat ID: $TG_CHAT_ID"
echo "Bot:     ${TG_BOT_TOKEN:0:10}...(hidden)"
echo ""
echo "Caption:"
echo "$CAPTION"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Use Python script for upload (curl has file access issues in some environments)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_UPLOADER="${SCRIPT_DIR}/telegram_upload.py"

if [[ ! -f "$PYTHON_UPLOADER" ]]; then
    echo "ERROR: Python uploader not found: $PYTHON_UPLOADER"
    exit 1
fi

# Upload via Python
python3 "$PYTHON_UPLOADER" "$TG_BOT_TOKEN" "$TG_CHAT_ID" "$APK_PATH" "$CAPTION"
exit $?
