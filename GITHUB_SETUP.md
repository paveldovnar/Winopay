# GitHub Setup - Add Secrets for CI/CD

## Quick Setup (Web UI)

### 1. Create GitHub Repository (if not exists)

Go to https://github.com/new and create a new repository named `WinoPay`.

### 2. Add Secrets

Go to: **Settings → Secrets and variables → Actions → New repository secret**

Add these secrets:

#### Required Secrets

| Name | Value |
|------|-------|
| `TG_BOT_TOKEN` | `8672910198:AAH9OpXP8eB1KuIEzW0GNGGOK1qUlQ_uxOE` |
| `TG_CHAT_ID` | `@mFvkRv2f8ZMyYzcy` |

#### Optional Secrets

| Name | Value | Notes |
|------|-------|-------|
| `REOWN_PROJECT_ID` | your_project_id | From https://cloud.reown.com |

### 3. Push Code

```bash
# Add remote
git remote add origin https://github.com/YOUR_USERNAME/WinoPay.git

# Add all files
git add .

# Commit
git commit -m "Initial commit with Telegram autopublish"

# Push
git push -u origin main
```

### 4. Verify

- Go to **Actions** tab on GitHub
- You should see a workflow run triggered
- After build completes, check your Telegram channel for the APK

---

## Alternative: CLI Setup

### Option 1: Using GitHub CLI

```bash
# Install gh CLI (if not installed)
brew install gh

# Login
gh auth login

# Add secrets from .env
source .env

gh secret set TG_BOT_TOKEN --body "$TG_BOT_TOKEN"
gh secret set TG_CHAT_ID --body "$TG_CHAT_ID"

# Optional
gh secret set REOWN_PROJECT_ID --body "$REOWN_PROJECT_ID"
```

### Option 2: Using Python Script

```bash
# Install PyNaCl
pip3 install PyNaCl

# Get GitHub token from: https://github.com/settings/tokens/new
# Required scope: repo

export GITHUB_TOKEN='your_token_here'

# Add secrets
python3 scripts/add_github_secrets.py YOUR_USERNAME/WinoPay
```

### Option 3: Using Shell Script

```bash
# Run setup script (requires gh CLI)
./scripts/setup_github_secrets.sh
```

---

## For Release Builds (Mainnet)

If you want to enable signed release builds, also add these secrets:

### Keystore Setup

1. **Create keystore** (if you don't have one):
   ```bash
   keytool -genkey -v -keystore release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias release
   ```

2. **Encode keystore to base64**:
   ```bash
   # macOS
   base64 -i release.jks | pbcopy

   # Linux
   base64 -w 0 release.jks
   ```

3. **Add to GitHub Secrets**:

| Name | Value |
|------|-------|
| `KEYSTORE_BASE64` | (paste base64 string) |
| `KEYSTORE_PASSWORD` | your_keystore_password |
| `KEY_ALIAS` | release (or your alias) |
| `KEY_PASSWORD` | your_key_password |

---

## Verify Setup

After adding secrets, check:
- Go to https://github.com/YOUR_USERNAME/WinoPay/settings/secrets/actions
- You should see:
  - ✅ TG_BOT_TOKEN
  - ✅ TG_CHAT_ID
  - (Optional) REOWN_PROJECT_ID
  - (For releases) KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD

---

## Troubleshooting

### "Secret not found" in Actions
- Check secret names match exactly (case-sensitive)
- Re-add the secret via Settings → Secrets

### Workflow doesn't trigger
- Check `.github/workflows/*.yml` files are committed
- Push to `main` branch (not `master`)
- Check workflow is enabled: Actions → Select workflow → Enable

### Build fails: "REOWN_PROJECT_ID not set"
- Either add the secret, or remove it from workflow if not needed
- Edit `.github/workflows/android-devnet.yml` and remove from `env:` section

---

## Current Values

Your credentials (from `.env`):
- Bot Token: `8672910198:AAH9OpXP8eB1KuIEzW0GNGGOK1qUlQ_uxOE`
- Channel: `@mFvkRv2f8ZMyYzcy` (Wino Pay update channel)

**Do NOT commit `.env` to git!** (Already in `.gitignore`)
