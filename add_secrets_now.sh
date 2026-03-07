#!/bin/bash
set -euo pipefail

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Adding GitHub Secrets to paveldovnar/Winopay"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check if gh is installed
if ! command -v gh &> /dev/null; then
    echo "Installing GitHub CLI..."
    brew install gh
fi

# Check if logged in
if ! gh auth status &> /dev/null; then
    echo ""
    echo "Please login to GitHub first:"
    echo "  gh auth login"
    echo ""
    exit 1
fi

# Load .env
if [[ ! -f .env ]]; then
    echo "❌ .env file not found"
    exit 1
fi

source .env

echo ""
echo "Adding secrets..."
echo ""

# Add TG_BOT_TOKEN
echo -n "TG_BOT_TOKEN... "
echo "$TG_BOT_TOKEN" | gh secret set TG_BOT_TOKEN --repo paveldovnar/Winopay
echo "✅"

# Add TG_CHAT_ID
echo -n "TG_CHAT_ID... "
echo "$TG_CHAT_ID" | gh secret set TG_CHAT_ID --repo paveldovnar/Winopay
echo "✅"

# Add REOWN_PROJECT_ID if exists
if [[ -n "${REOWN_PROJECT_ID:-}" ]]; then
    echo -n "REOWN_PROJECT_ID... "
    echo "$REOWN_PROJECT_ID" | gh secret set REOWN_PROJECT_ID --repo paveldovnar/Winopay
    echo "✅"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Secrets configured successfully!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "View at: https://github.com/paveldovnar/Winopay/settings/secrets/actions"
echo ""
echo "Next steps:"
echo "  git add ."
echo "  git commit -m 'Add Telegram autopublish'"
echo "  git push -u origin main"
echo ""
