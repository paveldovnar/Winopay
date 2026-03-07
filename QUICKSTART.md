# WinoPay - Quick Start

## Build and Publish APK to Telegram

### Setup (one-time)

Credentials already configured in `.env`:
- ✅ Telegram Bot Token
- ✅ Telegram Channel: @mFvkRv2f8ZMyYzcy

### Build & Publish

```bash
# Devnet debug (for testing)
./scripts/build_and_publish.sh devnetDebug

# Mainnet release (production - requires keystore)
./scripts/build_and_publish.sh mainnetRelease
```

### What Happens

1. Gradle builds APK
2. Extracts version info (versionName, versionCode, git sha)
3. Uploads to Telegram channel with caption:
   ```
   WinoPay devnetDebug v1.0.1 (2)
   Git: abc123 (main)
   Built: 2024-01-15 12:30:00 UTC
   ```

### GitHub Actions

Push to `main` → auto-build devnetDebug → publish to Telegram

```bash
git add .
git commit -m "Update app"
git push origin main
```

### Manual Publish (without build)

```bash
python3 scripts/telegram_upload.py \
  "$TG_BOT_TOKEN" \
  "$TG_CHAT_ID" \
  path/to/your.apk \
  "Custom caption"
```

### Troubleshooting

**No APK uploaded?**
- Check `.env` file exists with correct credentials
- Run: `cat .env` to verify

**Build fails?**
- Set JAVA_HOME: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
- Or add to `.env` file

**Need help?**
- See `DEPLOY.md` for detailed guide
- See `scripts/README.md` for script documentation

## Files

```
WinoPay/
├── .env                        ← Your credentials (DO NOT COMMIT)
├── scripts/
│   ├── telegram_upload.py     ← Python uploader
│   ├── publish_telegram.sh    ← Bash wrapper
│   └── build_and_publish.sh   ← All-in-one script
└── .github/workflows/
    ├── android-devnet.yml     ← Auto-build on push
    └── android-mainnet.yml    ← Release builds
```

## Current Status

✅ Telegram bot configured
✅ Channel: **Wino Pay update channel** (@mFvkRv2f8ZMyYzcy)
✅ Scripts working
✅ CI/CD workflows ready

**Next:** Push to GitHub to enable automatic builds.
