# Setup GitHub Secrets - Quick Guide

Repository: **https://github.com/paveldovnar/Winopay**

## Option 1: Web UI (Easiest - 2 minutes)

### Step 1: Go to Secrets Settings
Open: https://github.com/paveldovnar/Winopay/settings/secrets/actions/new

### Step 2: Add TG_BOT_TOKEN
- **Name:** `TG_BOT_TOKEN`
- **Secret:** `8672910198:AAH9OpXP8eB1KuIEzW0GNGGOK1qUlQ_uxOE`
- Click **Add secret**

### Step 3: Add TG_CHAT_ID
- Click **New repository secret** again
- **Name:** `TG_CHAT_ID`
- **Secret:** `@mFvkRv2f8ZMyYzcy`
- Click **Add secret**

### ✅ Done!

Verify at: https://github.com/paveldovnar/Winopay/settings/secrets/actions

---

## Option 2: Command Line (After gh auth login)

```bash
# 1. Login to GitHub
gh auth login

# 2. Run this command
source .env && \
echo "$TG_BOT_TOKEN" | gh secret set TG_BOT_TOKEN --repo paveldovnar/Winopay && \
echo "$TG_CHAT_ID" | gh secret set TG_CHAT_ID --repo paveldovnar/Winopay && \
echo "✅ Secrets added!"
```

---

## After Setup

```bash
# Commit and push
git add .
git commit -m "Add Telegram autopublish"
git push -u origin main
```

GitHub Actions will automatically:
1. Build devnetDebug APK
2. Publish to Telegram channel: **Wino Pay update channel** (@mFvkRv2f8ZMyYzcy)

View builds: https://github.com/paveldovnar/Winopay/actions
