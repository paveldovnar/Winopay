#!/bin/bash
# Quick script to add GitHub secrets
# Usage: bash ADD_SECRETS.sh

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "GitHub Secrets Setup"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check gh CLI
if ! command -v gh &> /dev/null; then
    echo "⚠️  GitHub CLI not found. Installing..."
    brew install gh
fi

# Load .env
if [[ -f .env ]]; then
    source .env
    echo "✅ Loaded credentials from .env"
else
    echo "❌ .env file not found"
    exit 1
fi

echo ""
echo "Adding secrets to GitHub repository..."
echo ""

# Add secrets
echo "$TG_BOT_TOKEN" | gh secret set TG_BOT_TOKEN
echo "$TG_CHAT_ID" | gh secret set TG_CHAT_ID

if [[ -n "${REOWN_PROJECT_ID:-}" ]]; then
    echo "$REOWN_PROJECT_ID" | gh secret set REOWN_PROJECT_ID
fi

echo ""
echo "✅ Done! Secrets added to GitHub."
echo ""
echo "View at: gh repo view --web → Settings → Secrets"
