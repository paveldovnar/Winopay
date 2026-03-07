# WinoPay Deployment Guide

Guide for setting up automated builds and Telegram publishing.

## Quick Start

### 1. Local Build & Publish

```bash
# Clone and setup
cd WinoPay
cp .env.example .env
# Edit .env with your credentials

# Build and publish
./scripts/build_and_publish.sh devnetDebug
```

### 2. GitHub Actions Setup

```bash
# Add secrets to GitHub repository
# Settings → Secrets and variables → Actions → New repository secret

# Required secrets:
TG_BOT_TOKEN=your_bot_token
TG_CHAT_ID=@your_channel
REOWN_PROJECT_ID=your_reown_id

# For mainnet release builds, also add:
KEYSTORE_BASE64=base64_encoded_keystore
KEYSTORE_PASSWORD=password
KEY_ALIAS=alias
KEY_PASSWORD=password
```

## Telegram Bot Setup

### Create Bot

1. Message [@BotFather](https://t.me/BotFather)
2. Send `/newbot`
3. Follow instructions and copy token

### Create Channel

1. Create Telegram channel (public or private)
2. Add bot as administrator with "Post messages" permission
3. Get channel ID:
   - **Public:** `@channel_name`
   - **Private:** Use this command:
     ```bash
     # Post a message to channel, then:
     curl "https://api.telegram.org/bot<TOKEN>/getUpdates"
     # Find: "chat":{"id":-1001234567890}
     ```

### Test Locally

```bash
# Set env vars (or add to .env file)
export TG_BOT_TOKEN="your_token"
export TG_CHAT_ID="@your_channel"

# Option 1: Build and publish in one command
./scripts/build_and_publish.sh devnetDebug

# Option 2: Build, then publish separately
./gradlew assembleDevnetDebug
./scripts/publish_telegram.sh \
  app/build/outputs/apk/devnet/debug/app-devnet-debug.apk \
  "Test build"

# Option 3: Use Python uploader directly
python3 scripts/telegram_upload.py \
  "$TG_BOT_TOKEN" \
  "$TG_CHAT_ID" \
  app/build/outputs/apk/devnet/debug/app-devnet-debug.apk \
  "Test build"
```

## GitHub Actions Workflows

### Devnet Builds (`android-devnet.yml`)

**Triggers:**
- Push to `main` (if app files changed)
- Manual dispatch

**What it does:**
1. Builds `devnetDebug` APK
2. Publishes to Telegram
3. Uploads artifact to GitHub

**Manual trigger:**
```bash
# In GitHub UI:
Actions → Android Devnet Build → Run workflow
```

### Mainnet Builds (`android-mainnet.yml`)

**Triggers:**
- Push tag `v*` (e.g., `git tag v1.0.1 && git push --tags`)
- Manual dispatch

**What it does:**
1. Builds `mainnetRelease` APK (signed)
2. Verifies APK signature
3. Publishes to Telegram
4. Uploads artifact to GitHub

**Manual trigger:**
```bash
# In GitHub UI:
Actions → Android Mainnet Build → Run workflow
```

## Release Process

### Development Builds

```bash
# Automatic: just push to main
git push origin main

# Or manual via GitHub Actions UI
```

### Production Release

```bash
# 1. Update version in app/build.gradle.kts
versionCode = 3
versionName = "1.0.2"

# 2. Commit
git add app/build.gradle.kts
git commit -m "Bump version to 1.0.2"

# 3. Tag
git tag v1.0.2

# 4. Push
git push origin main --tags

# GitHub Actions will automatically build and publish
```

## Required Secrets

### All Builds
- `TG_BOT_TOKEN` - Telegram bot API token
- `TG_CHAT_ID` - Telegram channel ID
- `REOWN_PROJECT_ID` - WalletConnect project ID (optional)

### Release Builds (mainnet only)
- `KEYSTORE_BASE64` - Base64-encoded keystore
- `KEYSTORE_PASSWORD` - Keystore password
- `KEY_ALIAS` - Key alias
- `KEY_PASSWORD` - Key password

### Encode Keystore

```bash
# macOS
base64 -i your-keystore.jks | pbcopy

# Linux
base64 -w 0 your-keystore.jks

# Paste to GitHub secret KEYSTORE_BASE64
```

## Build Variants

| Variant | Network | Signing | Use Case |
|---------|---------|---------|----------|
| `devnetDebug` | Devnet | Debug | Development testing |
| `devnetRelease` | Devnet | Release | Devnet production |
| `mainnetDebug` | Mainnet | Debug | Mainnet testing |
| `mainnetRelease` | Mainnet | Release | **Production** |

## Telegram Message Format

Devnet build:
```
WinoPay devnetDebug v1.0.1 (2)
Git: abc123 (main)
Built: 2024-01-15 12:30:00 UTC

GitHub Actions #42
```

Mainnet release:
```
🚀 WinoPay mainnetRelease v1.0.1 (2)
Git: abc123 (v1.0.1)
Built: 2024-01-15 12:30:00 UTC

GitHub Actions #42
```

## Troubleshooting

### Local Builds

**Error: "TG_BOT_TOKEN not set"**
```bash
# Check .env file exists
cat .env

# Or set manually
export TG_BOT_TOKEN="your_token"
export TG_CHAT_ID="@channel"
```

**Error: "APK not found"**
```bash
# Check build succeeded
./gradlew assembleDevnetDebug

# Verify APK path
ls -lh app/build/outputs/apk/devnet/debug/
```

### GitHub Actions

**Build fails: "REOWN_PROJECT_ID not set"**
- Add secret in repository settings
- Or remove from required env vars if not needed

**Telegram publish fails: "HTTP 403"**
- Bot not added to channel
- Bot missing "Post messages" permission

**Mainnet signing fails**
- Check all keystore secrets are set correctly
- Verify KEYSTORE_BASE64 is valid base64

### Telegram API

**Error: "Bad Request: chat not found"**
- Wrong `TG_CHAT_ID`
- For private channels, use numeric ID (negative number)

**Error: "Forbidden: bot was blocked by the user"**
- Bot not added as admin to channel

## Security Checklist

- [ ] Never commit `TG_BOT_TOKEN` to git
- [ ] Never commit keystore files (`.jks`, `.keystore`)
- [ ] Use GitHub Secrets for sensitive data
- [ ] Keep `.env` in `.gitignore`
- [ ] Rotate tokens if accidentally exposed
- [ ] Use different tokens for dev/prod

## Monitoring

### Check Recent Builds

```bash
# GitHub Actions
# repository → Actions → Recent workflow runs

# Telegram channel
# Check your channel for published APKs
```

### Download Artifacts

APKs are stored in GitHub for:
- Devnet: 30 days
- Mainnet: 90 days

To download:
1. Go to Actions → Workflow run
2. Scroll to "Artifacts"
3. Download ZIP

## Next Steps

After setup:
1. ✅ Test local build: `./scripts/build_and_publish.sh devnetDebug`
2. ✅ Test GitHub Actions: Push to `main`
3. ✅ Check Telegram channel for APK
4. ✅ Create release: `git tag v1.0.0 && git push --tags`

## Support

- Scripts: See `scripts/README.md`
- Telegram Bot API: https://core.telegram.org/bots/api
- GitHub Actions: https://docs.github.com/en/actions
