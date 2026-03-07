# WinoPay Build Scripts

Scripts for building and publishing APKs to Telegram.

## Scripts

### `telegram_upload.py`

Python script for uploading files to Telegram via Bot API.

**Features:**
- Multipart/form-data upload
- Works reliably across all platforms
- 5-minute timeout for large files
- Automatic MIME type detection

**Usage:**
```bash
python3 telegram_upload.py <bot_token> <chat_id> <file_path> [caption]
```

### `publish_telegram.sh`

Publishes an APK to Telegram channel via Bot API. Uses `telegram_upload.py` internally.

**Usage:**
```bash
TG_BOT_TOKEN=xxx TG_CHAT_ID=@channel ./scripts/publish_telegram.sh <apk_path> <caption>
```

**Environment Variables:**
- `TG_BOT_TOKEN` - Telegram Bot API token (required)
- `TG_CHAT_ID` - Telegram chat ID or `@channel_name` (required)

**Example:**
```bash
TG_BOT_TOKEN="123456:ABC-DEF..." \
TG_CHAT_ID="@winopay_builds" \
./scripts/publish_telegram.sh \
  app/build/outputs/apk/devnet/debug/app-devnet-debug.apk \
  "WinoPay devnetDebug v1.0.1 (2)
   Git: abc123
   Built: 2024-01-15 12:30:00 UTC"
```

### `build_and_publish.sh`

Builds APK and publishes to Telegram (combines build + publish).

**Usage:**
```bash
./scripts/build_and_publish.sh <variant>
```

**Variants:**
- `devnetDebug` - Devnet debug build
- `devnetRelease` - Devnet release build
- `mainnetDebug` - Mainnet debug build
- `mainnetRelease` - Mainnet release build

**Example:**
```bash
# Build only (no publish)
./scripts/build_and_publish.sh devnetDebug

# Build and publish
TG_BOT_TOKEN="xxx" TG_CHAT_ID="@channel" ./scripts/build_and_publish.sh devnetDebug
```

## Telegram Bot Setup

### 1. Create Telegram Bot

1. Message [@BotFather](https://t.me/BotFather) on Telegram
2. Send `/newbot` and follow instructions
3. Copy the bot token (format: `123456789:ABCDEF...`)

### 2. Create Channel

1. Create a Telegram channel (public or private)
2. Add your bot as administrator with "Post messages" permission
3. Get channel ID:
   - **Public channel:** `@channel_name` (e.g., `@winopay_builds`)
   - **Private channel:** numeric ID (e.g., `-1001234567890`)

To get private channel ID:
```bash
# Send a message to the channel, then:
curl "https://api.telegram.org/bot<BOT_TOKEN>/getUpdates"
# Look for "chat":{"id":-1001234567890}
```

### 3. Set Environment Variables

**Local:**
```bash
export TG_BOT_TOKEN="123456789:ABCDEF..."
export TG_CHAT_ID="@winopay_builds"
```

**GitHub Actions:**
1. Go to repository Settings → Secrets and variables → Actions
2. Add repository secrets:
   - `TG_BOT_TOKEN` - Your bot token
   - `TG_CHAT_ID` - Your channel ID

## GitHub Actions

Two workflows are provided:

### `android-devnet.yml`

Builds devnet variants and publishes to Telegram.

**Triggers:**
- Push to `main` branch (if app files changed)
- Manual workflow dispatch

**Required Secrets:**
- `TG_BOT_TOKEN` - Telegram bot token
- `TG_CHAT_ID` - Telegram channel ID
- `REOWN_PROJECT_ID` - Reown/WalletConnect project ID (optional)

### `android-mainnet.yml`

Builds mainnet variants and publishes to Telegram.

**Triggers:**
- Push tag `v*` (e.g., `v1.0.1`)
- Manual workflow dispatch

**Required Secrets:**
- `TG_BOT_TOKEN` - Telegram bot token
- `TG_CHAT_ID` - Telegram channel ID
- `REOWN_PROJECT_ID` - Reown/WalletConnect project ID (optional)
- `KEYSTORE_BASE64` - Base64-encoded keystore file (for release builds)
- `KEYSTORE_PASSWORD` - Keystore password
- `KEY_ALIAS` - Key alias
- `KEY_PASSWORD` - Key password

**Encode keystore to base64:**
```bash
base64 -i your-keystore.jks | pbcopy  # macOS
base64 -w 0 your-keystore.jks          # Linux
```

## Local Testing

Test the publish script without building:

```bash
# 1. Build APK manually
./gradlew assembleDevnetDebug

# 2. Test publish script
TG_BOT_TOKEN="your_token" \
TG_CHAT_ID="@your_channel" \
./scripts/publish_telegram.sh \
  app/build/outputs/apk/devnet/debug/app-devnet-debug.apk \
  "Test build"
```

## Security Notes

- **NEVER** commit `TG_BOT_TOKEN` to git
- **NEVER** print tokens in logs
- Use environment variables or GitHub Secrets only
- Tokens are hidden in script output (shows only first 10 chars)

## Troubleshooting

### "ERROR: TG_BOT_TOKEN environment variable is not set"
Set the token:
```bash
export TG_BOT_TOKEN="your_token_here"
```

### "ERROR: APK file not found"
Check the APK path is correct:
```bash
ls -lh app/build/outputs/apk/devnet/debug/
```

### "HTTP 400: Bad Request"
- Check `TG_CHAT_ID` is correct
- Make sure bot is added to channel as admin
- For private channels, ID must be negative (e.g., `-1001234567890`)

### "HTTP 403: Forbidden"
- Bot is not added to channel
- Bot doesn't have "Post messages" permission

### GitHub Actions fails
Check secrets are set correctly:
1. Go to repository Settings → Secrets and variables → Actions
2. Verify all required secrets are present
3. Check workflow logs for specific error
