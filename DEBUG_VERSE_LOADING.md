# Debugging Verse Loading Issue

## Changes Made

Fixed the QuranVerseProvider to:
1. ✅ Call callbacks on main UI thread (using Handler)
2. ✅ Simplified API endpoint (removed `/ar.alafasy` edition specifier)
3. ✅ Added detailed logging at every step
4. ✅ Proper connection cleanup in finally block
5. ✅ Better error messages

## New Build

📦 **APK Location**: `app/build/outputs/apk/debug/app-debug.apk`

Install with:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## How to Debug

### Step 1: Clear Old Logs
```bash
adb logcat -c
```

### Step 2: Start the App and Try Again
1. Open the app
2. Navigate to a memorization goal
3. Click "Start Session"

### Step 3: Check Logs Immediately
```bash
adb logcat -d | grep -E "QuranVerseProvider|MemorizationSession"
```

## What to Look For

### Success Indicators:
```
QuranVerseProvider: Fetching verse from API: https://api.alquran.cloud/v1/ayah/1:1
QuranVerseProvider: Connecting to API...
QuranVerseProvider: Response code: 200
QuranVerseProvider: API response received: XXX chars
QuranVerseProvider: Successfully fetched verse 1:1: بِسْمِ...
MemorizationSession: Loaded verse 1:1 - بِسْمِ...
```

### Error Indicators:

**Network Permission Issue:**
```
ERROR QuranVerseProvider: Error fetching verse 1:1: SecurityException
```
→ Solution: Check INTERNET permission in AndroidManifest.xml (should already be there)

**Connection Timeout:**
```
ERROR QuranVerseProvider: Error fetching verse 1:1: SocketTimeoutException
```
→ Solution: Check internet connection, try on WiFi

**DNS/Network Error:**
```
ERROR QuranVerseProvider: Error fetching verse 1:1: UnknownHostException
```
→ Solution: Device can't resolve api.alquran.cloud, check DNS

**SSL/Certificate Error:**
```
ERROR QuranVerseProvider: Error fetching verse 1:1: SSLException
```
→ Solution: Device doesn't trust the SSL certificate (rare on modern devices)

**API Error:**
```
ERROR QuranVerseProvider: API returned error code: 404
```
→ Solution: Invalid surah:ayah combination

## Quick Tests

### Test 1: Check Internet Connection
```bash
# From your computer (should work)
curl -s "https://api.alquran.cloud/v1/ayah/1:1" | head -5

# From device
adb shell ping -c 3 api.alquran.cloud
```

### Test 2: Check App Permissions
```bash
adb shell dumpsys package com.repeatquran | grep permission
```

Should show:
```
android.permission.INTERNET: granted=true
android.permission.RECORD_AUDIO: granted=true
```

### Test 3: Test Network from Device
```bash
# If device has curl/wget
adb shell "curl -s https://api.alquran.cloud/v1/ayah/1:1"
```

## Common Issues and Fixes

### Issue 1: "Failed to load verse text. Check internet connection"

**Possible Causes:**
1. Device has no internet
2. Firewall/VPN blocking api.alquran.cloud
3. Android network security config blocking cleartext
4. Certificate validation failing

**Solutions:**
1. Connect device to WiFi with internet
2. Disable VPN if active
3. Check Android version (should work on API 21+)
4. Try on different device/emulator

### Issue 2: App Crashes on Start

**Check crash logs:**
```bash
adb logcat -d | grep -E "AndroidRuntime|FATAL"
```

### Issue 3: No Logs Appearing

**Verify ADB is working:**
```bash
adb devices
# Should show your device

adb logcat -d | wc -l
# Should show line count > 0
```

## Alternative: Use Hardcoded Test Data

If API continues to fail, we can temporarily hardcode test verses. Add this to QuranVerseProvider:

```java
// In getVerseText(), before the API call:
if (surah == 1 && ayah == 1) {
    mainHandler.post(() -> callback.onVerseLoaded("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"));
    return;
}
```

This will bypass the API for testing verse matching.

## What Changed in the Code

### Before (Problem):
```java
// Callback called from background thread
callback.onVerseLoaded(verseText); // ❌ Wrong thread!
```

### After (Fixed):
```java
// Callback called on main UI thread
mainHandler.post(() -> callback.onVerseLoaded(finalText)); // ✅ Correct!
```

## Next Steps

1. **Install new APK** with fixes
2. **Clear logcat** before testing
3. **Try starting a session** again
4. **Capture logs** immediately
5. **Share logs** if still failing

The new build has much more detailed logging, so we'll be able to see exactly where it's failing.

## Expected Behavior After Fix

1. User clicks "Start Session"
2. Loading message shows: "Loading verse..."
3. API call happens in background (1-2 seconds)
4. Verse text loaded successfully
5. Status changes to "🎤 Listening..."
6. Speech recognition starts

If you still see "Failed to load verse text", please run:
```bash
adb logcat -c
# Start session in app
adb logcat -d | grep -E "QuranVerseProvider" > verse_logs.txt
```

And share the `verse_logs.txt` content.
