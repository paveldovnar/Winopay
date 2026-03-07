# In-App Update Checker

Self-hosted update checker for debug/internal builds (non-Play-Store distribution).

## Security & Constraints

- ✅ **Debug builds only** - Auto-disabled in release builds
- ✅ **User confirmation required** - Uses Android system installer (no silent install)
- ✅ **SHA-256 verification** - Downloads verified before install
- ✅ **Signature matching** - Android enforces APK signature match
- ✅ **Multichain-neutral** - No chain-specific logic

## Architecture

### Files Modified/Created

1. **UpdateChecker.kt** - Core update logic
   - Fetches latest.json from endpoint
   - Compares versionCode
   - Downloads APK to cache
   - Verifies SHA-256 hash
   - Launches system installer

2. **UpdateDialog.kt** - UI component
   - Checking → Available → Downloading → Install flow
   - Progress bar for download
   - Error handling

3. **FileProvider config**
   - `res/xml/provider_paths.xml` - Exposes cache/updates directory
   - AndroidManifest.xml - FileProvider declaration

4. **build.gradle.kts**
   - Added `UPDATE_ENDPOINT` buildConfig field
   - Configurable via env var or gradle property

5. **SettingsScreen.kt**
   - Added "Check for updates" row (debug builds only)

6. **Navigation.kt**
   - Settings: Manual check on button click
   - Dashboard: Auto-check on start (silent, shows dialog if update available)

## Endpoint Format

### latest.json

Host a JSON file at `{UPDATE_ENDPOINT}/releases/latest.json`:

```json
{
  "versionCode": 3,
  "versionName": "1.0.2",
  "apkUrl": "https://example.com/releases/app-devnet-debug-1.0.2.apk",
  "sha256": "abc123def456...full-sha256-hash"
}
```

**Fields:**
- `versionCode` (int) - Must be > BuildConfig.VERSION_CODE to trigger update
- `versionName` (string) - Displayed to user (e.g., "1.0.2")
- `apkUrl` (string) - Direct download URL for APK
- `sha256` (string) - Lowercase hex SHA-256 hash of APK

### APK Hosting

Host the APK file at the URL specified in `apkUrl`:
- Must be publicly accessible via HTTP/HTTPS
- Same APK signature as installed app (Android enforces this)

## Server Setup (Minimal)

### Option 1: Simple Static Hosting

```bash
# 1. Create directory structure
mkdir -p releases

# 2. Copy APK
cp app/build/outputs/apk/devnet/debug/app-devnet-debug.apk releases/app-devnet-debug-1.0.2.apk

# 3. Calculate SHA-256
sha256sum releases/app-devnet-debug-1.0.2.apk
# Output: abc123def456... releases/app-devnet-debug-1.0.2.apk

# 4. Create latest.json
cat > releases/latest.json <<EOF
{
  "versionCode": 3,
  "versionName": "1.0.2",
  "apkUrl": "https://your-server.com/releases/app-devnet-debug-1.0.2.apk",
  "sha256": "abc123def456...paste-full-hash-here"
}
EOF

# 5. Host with any static server
# Examples:
# - Nginx: Point to releases directory
# - Python: python3 -m http.server 8080
# - GitHub Pages: Push to gh-pages branch
# - Cloudflare Pages: Deploy releases folder
```

### Option 2: GitHub Releases (Recommended for Testing)

1. Go to GitHub → Releases → Create new release
2. Upload APK as release asset
3. Get direct download URL (right-click asset → Copy link)
4. Create `latest.json` in your repo or separate hosting
5. Point `apkUrl` to GitHub download URL

### Option 3: Self-Hosted with Auto-Update Script

```bash
#!/bin/bash
# update_manifest.sh

APK_PATH="$1"
VERSION_CODE="$2"
VERSION_NAME="$3"
BASE_URL="https://your-server.com/releases"

# Calculate SHA-256
SHA256=$(sha256sum "$APK_PATH" | cut -d' ' -f1)

# Generate latest.json
cat > releases/latest.json <<EOF
{
  "versionCode": $VERSION_CODE,
  "versionName": "$VERSION_NAME",
  "apkUrl": "$BASE_URL/$(basename $APK_PATH)",
  "sha256": "$SHA256"
}
EOF

echo "Updated latest.json:"
cat releases/latest.json
```

Usage:
```bash
./update_manifest.sh app-devnet-debug.apk 3 "1.0.2"
```

## Configuration

### Set UPDATE_ENDPOINT

**Option 1: Environment variable (recommended)**
```bash
export UPDATE_ENDPOINT="https://your-server.com"
./gradlew assembleDevnetDebug
```

**Option 2: gradle.properties**
```properties
UPDATE_ENDPOINT=https://your-server.com
```

**Option 3: Command line**
```bash
./gradlew assembleDevnetDebug -PUPDATE_ENDPOINT=https://your-server.com
```

### Verify Configuration

Check logs after app start (if endpoint configured):
```
UPDATE|CHECK|endpoint=https://your-server.com|currentVersion=2
```

## Manual Test Steps

### Prerequisites

1. **Build initial version (v1.0.1, versionCode=2)**
   ```bash
   export UPDATE_ENDPOINT="https://your-test-server.com"
   ./gradlew assembleDevnetDebug
   adb install app/build/outputs/apk/devnet/debug/app-devnet-debug.apk
   ```

2. **Prepare update (v1.0.2, versionCode=3)**
   - Edit `build.gradle.kts`: `versionCode = 3`, `versionName = "1.0.2"`
   - Build: `./gradlew assembleDevnetDebug`
   - Calculate SHA-256:
     ```bash
     sha256sum app/build/outputs/apk/devnet/debug/app-devnet-debug.apk
     ```
   - Host APK and create `latest.json` (see Server Setup above)

### Test Case 1: Auto-Check on Dashboard

1. **Setup**: Ensure latest.json has versionCode=3
2. **Action**: Open app (Dashboard screen)
3. **Expected**:
   - Log: `UPDATE|CHECK|endpoint=...`
   - Log: `UPDATE|AVAILABLE|new=3|current=2|name=1.0.2`
   - Dialog appears: "Update available - Version 1.0.2 is available"
4. **Action**: Click "Download"
5. **Expected**:
   - Log: `UPDATE|DOWNLOAD|url=...`
   - Progress bar shows 0-100%
   - Log: `UPDATE|VERIFY|start`
   - Log: `UPDATE|VERIFY_OK|sha256=...`
   - Log: `UPDATE|INSTALL_INTENT|file=...`
   - Android installer opens
6. **Action**: Click "Install" in system installer
7. **Expected**: App updates to v1.0.2

### Test Case 2: Manual Check from Settings

1. **Action**: Go to Settings → "Check for updates"
2. **Expected**: Same flow as Test Case 1

### Test Case 3: No Update Available

1. **Setup**: latest.json has versionCode=2 (same as installed)
2. **Action**: Settings → "Check for updates"
3. **Expected**:
   - Log: `UPDATE|NO_UPDATE|latest=2|current=2`
   - Dialog: "No update available - You're running the latest version"

### Test Case 4: SHA-256 Mismatch

1. **Setup**: latest.json has wrong SHA-256 hash
2. **Action**: Settings → "Check for updates" → "Download"
3. **Expected**:
   - Log: `UPDATE|VERIFY|error=hash_mismatch|expected=...|actual=...`
   - Dialog: "Update check failed - SHA-256 verification failed"
   - Downloaded APK is deleted

### Test Case 5: Network Error

1. **Setup**: Set invalid UPDATE_ENDPOINT or disable network
2. **Action**: Settings → "Check for updates"
3. **Expected**:
   - Log: `UPDATE|CHECK|error=...`
   - Dialog: "Update check failed - [error message]"

### Test Case 6: Release Build (Disabled)

1. **Setup**: Build release variant
2. **Expected**: "Check for updates" button NOT shown in Settings
3. **Expected**: No auto-check on Dashboard

## Logging

All update events are logged with `UPDATE|` prefix:

```
UPDATE|CHECK|endpoint=...|currentVersion=2
UPDATE|AVAILABLE|new=3|current=2|name=1.0.2
UPDATE|NO_UPDATE|latest=2|current=2
UPDATE|DOWNLOAD|url=...|version=1.0.2
UPDATE|DOWNLOAD|complete|size=15728640
UPDATE|VERIFY|start
UPDATE|VERIFY_OK|sha256=abc123...
UPDATE|VERIFY|error=hash_mismatch|expected=...|actual=...
UPDATE|INSTALL_INTENT|file=update-1.0.2.apk
UPDATE|CHECK|error=...
UPDATE|CLEANUP|deleted=update-1.0.1.apk
```

Filter logs:
```bash
adb logcat | grep "UPDATE|"
```

## Troubleshooting

### "Update check failed - Update endpoint not configured"

- Ensure UPDATE_ENDPOINT is set in build.gradle or env var
- Rebuild app after setting endpoint
- Check BuildConfig.UPDATE_ENDPOINT exists

### "Update check failed - HTTP 404"

- Verify latest.json is accessible at `{UPDATE_ENDPOINT}/releases/latest.json`
- Test URL in browser
- Check CORS if hosting on different domain

### SHA-256 verification failed

- Recalculate SHA-256: `sha256sum app.apk`
- Ensure hash in latest.json matches exactly (lowercase hex)
- Verify APK wasn't corrupted during upload

### Android installer says "App not installed"

- APK signature doesn't match installed app
- Sign both APKs with same keystore
- For debug builds, ensure both use debug keystore

### "Check for updates" button not showing

- Only visible in debug builds (BuildConfig.DEBUG == true)
- Rebuild with debug variant
- Check `UpdateChecker.isUpdateCheckEnabled()` returns true

## Production Deployment

**WARNING**: This updater is designed for debug/internal builds only.

For Play Store releases:
1. Remove UPDATE_ENDPOINT from production builds
2. Use Google Play In-App Updates API instead
3. Or keep updater disabled (BuildConfig.DEBUG == false)

For enterprise/internal distribution:
1. Create "internal" flavor in build.gradle
2. Enable updater only for internal flavor
3. Use proper HTTPS endpoint with SSL
4. Consider adding update signature verification
5. Implement delta updates for bandwidth efficiency

## Security Considerations

- ✅ SHA-256 verification prevents tampered APKs
- ✅ Android signature matching prevents unauthorized updates
- ✅ User confirmation required (no silent install)
- ⚠️ Endpoint should use HTTPS in production
- ⚠️ Consider adding JSON signature verification
- ⚠️ Rate limit update endpoint to prevent abuse
