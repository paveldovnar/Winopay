#!/bin/bash
set -euo pipefail

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# WinoPay - Setup GitHub Secrets
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
#
# Automatically adds secrets to GitHub repository for CI/CD.
#
# Prerequisites:
#   - GitHub CLI installed (brew install gh)
#   - Authenticated with gh (gh auth login)
#
# Usage:
#   ./scripts/setup_github_secrets.sh
#
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "WinoPay - GitHub Secrets Setup"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo "❌ ERROR: GitHub CLI (gh) is not installed"
    echo ""
    echo "Install it with:"
    echo "  macOS:  brew install gh"
    echo "  Linux:  See https://github.com/cli/cli#installation"
    echo ""
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo "❌ ERROR: Not authenticated with GitHub"
    echo ""
    echo "Run: gh auth login"
    echo ""
    exit 1
fi

# Load .env file if exists
if [[ -f .env ]]; then
    echo "✅ Loading credentials from .env"
    source .env
else
    echo "⚠️  No .env file found"
    echo ""
fi

# Validate required variables
REQUIRED_VARS=(
    "TG_BOT_TOKEN"
    "TG_CHAT_ID"
)

MISSING_VARS=()
for var in "${REQUIRED_VARS[@]}"; do
    if [[ -z "${!var:-}" ]]; then
        MISSING_VARS+=("$var")
    fi
done

if [[ ${#MISSING_VARS[@]} -gt 0 ]]; then
    echo "❌ ERROR: Missing required environment variables:"
    for var in "${MISSING_VARS[@]}"; do
        echo "  - $var"
    done
    echo ""
    echo "Set them in .env file or export them:"
    echo "  export TG_BOT_TOKEN='your_token'"
    echo "  export TG_CHAT_ID='@your_channel'"
    echo ""
    exit 1
fi

# Get repository info
REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || echo "")

if [[ -z "$REPO" ]]; then
    echo "❌ ERROR: Not in a GitHub repository"
    echo ""
    echo "Initialize git and create GitHub repo first:"
    echo "  git init"
    echo "  gh repo create"
    echo ""
    exit 1
fi

echo "Repository: $REPO"
echo ""
echo "Secrets to add:"
echo "  - TG_BOT_TOKEN (${TG_BOT_TOKEN:0:10}...hidden)"
echo "  - TG_CHAT_ID ($TG_CHAT_ID)"

# Optional secrets
if [[ -n "${REOWN_PROJECT_ID:-}" ]]; then
    echo "  - REOWN_PROJECT_ID (${REOWN_PROJECT_ID:0:10}...hidden)"
fi

echo ""
read -p "Continue? (y/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 0
fi

echo ""
echo "Adding secrets to GitHub..."

# Add TG_BOT_TOKEN
echo -n "TG_BOT_TOKEN... "
echo -n "$TG_BOT_TOKEN" | gh secret set TG_BOT_TOKEN --repo "$REPO"
echo "✅"

# Add TG_CHAT_ID
echo -n "TG_CHAT_ID... "
echo -n "$TG_CHAT_ID" | gh secret set TG_CHAT_ID --repo "$REPO"
echo "✅"

# Add REOWN_PROJECT_ID if set
if [[ -n "${REOWN_PROJECT_ID:-}" ]]; then
    echo -n "REOWN_PROJECT_ID... "
    echo -n "$REOWN_PROJECT_ID" | gh secret set REOWN_PROJECT_ID --repo "$REPO"
    echo "✅"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ GitHub Secrets configured successfully!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Next steps:"
echo "  1. Commit your code: git add . && git commit -m 'Initial commit'"
echo "  2. Push to GitHub: git push origin main"
echo "  3. GitHub Actions will automatically build and publish to Telegram"
echo ""
echo "View secrets: https://github.com/$REPO/settings/secrets/actions"
echo ""
