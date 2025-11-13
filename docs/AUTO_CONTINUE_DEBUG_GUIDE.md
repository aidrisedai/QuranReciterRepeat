# Auto-Continue Debugging Guide

## Issue Report
User reported that auto-continue to next page works for Surah and Verse tabs, but not for Page tab on their device.

## Changes Made (v1.X.X)

### 1. Stricter Content Validation ✅
Previously, `isContentForThisFragment()` only checked the content **type** (page/surah/verse), not the actual content itself. This caused false positives where a fragment would claim ownership of content it didn't display.

**Fixed in all three fragments:**

#### PageTabFragment
- **Before:** Only checked `sourceType == "page"`
- **After:** Checks `sourceType == "page"` AND `resumePage == currentPage`
- **Result:** Only triggers auto-continue when the page that just ended matches the currently displayed page

#### SurahTabFragment  
- **Before:** Only checked `sourceType == "surah"`
- **After:** Checks `sourceType == "surah"` AND `resumeSurah == currentSurah`
- **Result:** Only triggers auto-continue when the surah that just ended matches the currently displayed surah

#### VerseTabFragment
- **Before:** Only checked `sourceType == "single"`
- **After:** Checks `sourceType == "single"` AND `resumeSurah:resumeAyah == currentSurah:currentAyah`
- **Result:** Only triggers auto-continue when the verse that just ended matches the currently displayed verse

### 2. Comprehensive Logging 🔍
Added detailed logging at every step of the auto-continue flow to diagnose issues:

#### PlaybackService Logs (when playback ends)
```
Playback ended - auto-continue setting: true
Broadcasting auto-continue: sourceType=page, page=5, surah=1:1
Auto-continue broadcast sent
```

#### BaseTabFragment Logs (when broadcast received)
```
Auto-continue broadcast received
Fragment state: added=true, detached=false
Content ownership check: true
Can navigate next: true
All checks passed, navigating to next and auto-playing
Navigation successful
Auto-continue completed successfully
```

#### Fragment-Specific Logs (content validation)
```
PageTabFragment: Content validation: sourceType=page, resumePage=5, currentPage=5, isOwner=true
SurahTabFragment: Content validation: sourceType=surah, resumeSurah=1, currentSurah=1, isOwner=true
VerseTabFragment: Content validation: sourceType=single, resumeVerse=1:1, currentVerse=1:1, isOwner=true
```

## How to Capture Logs from User's Device

### Option 1: Using Android Studio (Recommended)
1. Connect user's device via USB with USB debugging enabled
2. Open Android Studio → Logcat
3. Filter by tag: `PageTabFragment` or `PlaybackService` or `BaseTabFragment`
4. Reproduce the issue
5. Copy relevant logs

### Option 2: Using adb Command Line
```bash
# Clear existing logs first
adb logcat -c

# Start capturing logs (filtered for our app)
adb logcat | grep -E "PageTabFragment|SurahTabFragment|VerseTabFragment|PlaybackService|BaseTabFragment"

# Or save to file
adb logcat | grep -E "PageTabFragment|SurahTabFragment|VerseTabFragment|PlaybackService|BaseTabFragment" > auto_continue_logs.txt
```

### Option 3: User Captures Logs Directly (No Computer Needed)
User can install a logcat app from Play Store:
- **LogCat Extreme** (recommended)
- **Logcat Reader**
- **aLogcat**

Instructions:
1. Install logcat app
2. Open app and grant permissions
3. Set filter to: `PageTabFragment|PlaybackService|BaseTabFragment`
4. Reproduce the issue
5. Share/export logs from the app

## Expected Log Flow for Successful Auto-Continue

### When Page 5 Playback Ends and Auto-Continues to Page 6:

```
1. PlaybackService: Playback ended - auto-continue setting: true
2. PlaybackService: Broadcasting auto-continue: sourceType=page, page=5, surah=1:1
3. PlaybackService: Auto-continue broadcast sent

4. PageTabFragment: Auto-continue broadcast received
5. PageTabFragment: Fragment state: added=true, detached=false
6. PageTabFragment: Content validation: sourceType=page, resumePage=5, currentPage=5, isOwner=true
7. PageTabFragment: Content ownership check: true
8. PageTabFragment: Can navigate next: true
9. PageTabFragment: All checks passed, navigating to next and auto-playing
10. PageTabFragment: Page set to: 6
11. PageTabFragment: Navigation successful
12. PageTabFragment: Loading page 6…
13. PageTabFragment: Auto-continue completed successfully
```

## Troubleshooting Guide

### If Auto-Continue Not Working

Check the logs for these common failure points:

#### 1. Auto-Continue Setting Disabled
```
PlaybackService: Playback ended - auto-continue setting: false
```
**Fix:** Enable "Auto continue" in Settings

#### 2. Broadcast Not Received
```
PlaybackService: Auto-continue broadcast sent
[NO LOG FROM PageTabFragment]
```
**Possible causes:**
- Fragment is paused/stopped
- User switched to different tab
- Android killed the broadcast receiver

#### 3. Fragment Not in Valid State
```
PageTabFragment: Fragment state: added=false, detached=true
PageTabFragment: Fragment not in valid state for auto-continue
```
**Cause:** User navigated away from the Page tab before playback ended

#### 4. Content Ownership Mismatch
```
PageTabFragment: Content validation: sourceType=page, resumePage=5, currentPage=3, isOwner=false
PageTabFragment: Content does not belong to this fragment, ignoring auto-continue
```
**Cause:** User manually changed the page number while previous page was still playing

#### 5. Cannot Navigate Next (End of Content)
```
PageTabFragment: Can navigate next: false
PageTabFragment: Cannot navigate to next, end of content
```
**Cause:** Already at page 604 (or surah 114, or last verse)

#### 6. Navigation Failed
```
PageTabFragment: All checks passed, navigating to next and auto-playing
PageTabFragment: Navigation returned false
```
**Cause:** Internal error in navigation logic

## Testing Checklist

### Manual Testing Steps:
1. ✅ Enable "Auto continue" in Settings
2. ✅ Go to Page tab, select page 5
3. ✅ Press Play
4. ✅ Wait for page 5 playback to complete
5. ✅ Verify it auto-advances to page 6 and starts playing
6. ✅ Repeat for Surah tab (surah 1 → surah 2)
7. ✅ Repeat for Verse tab (verse 1:1 → verse 1:2)

### Edge Cases:
- ✅ Test at last page (604) - should NOT auto-continue
- ✅ Test at last surah (114) - should NOT auto-continue
- ✅ Test at last verse of Quran - should NOT auto-continue
- ✅ Switch tabs while playing - should only continue in active tab
- ✅ Change selection while playing - should NOT auto-continue to wrong content

## Known Limitations
- Auto-continue only works when the fragment is **active and visible**
- If user switches tabs during playback, auto-continue will not trigger
- Range tab does NOT support auto-continue (by design - too complex)

## Version History
- **v1.X.X (2025-01-24):** Added strict content validation and comprehensive logging
- **Previous:** Basic auto-continue with type-only validation
