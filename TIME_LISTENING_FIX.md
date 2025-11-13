# Time Listening Real-Time Update Fix ⏱️

## Issue Identified

The **verses listened** stat was updating in real-time during playback, but **time listened** was only updating after a session ended. This created an inconsistent user experience where verses counted up immediately but time remained static until playback finished.

## Root Cause

### Verses Tracking (Was Working ✅)
```
PlaybackService.onMediaItemTransition()
  → trackCurrentVerse() 
  → Insert into VerseProgressEntity (real-time)
  → Broadcast VERSE_TRACKED
  → HomeActivity refreshes immediately
```

### Time Tracking (Was Broken ❌)
```
HomeActivity.loadListeningStats()
  → Loop through sessions
  → Check: if (session.endedAt != null && session.startedAt > 0)
  → Only count COMPLETED sessions
  → Active sessions ignored!
```

**Problem**: Active sessions (where `endedAt` is `null`) were completely skipped, so time only updated when playback ended.

## Solution

Modified `HomeActivity.loadListeningStats()` to include **active sessions** by using the current time as the end time when `endedAt` is `null`.

### Changes Made

**File**: `HomeActivity.java` (lines 171-231)

#### Change 1: Total Time Calculation
```java
// BEFORE
if (session.endedAt != null && session.startedAt > 0) {
    long duration = session.endedAt - session.startedAt;
    totalListeningTimeMs += duration;
}

// AFTER
if (session.startedAt > 0) {
    // If session ended, use endedAt; otherwise use current time (for active sessions)
    long endTime = session.endedAt != null ? session.endedAt : currentTime;
    long duration = endTime - session.startedAt;
    
    // Only count reasonable durations (max 24 hours per session)
    if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
        totalListeningTimeMs += duration;
    }
}
```

#### Change 2: Today/Week Time Calculation
```java
// BEFORE
if (session.endedAt != null && session.startedAt > 0) {
    sessionDuration = session.endedAt - session.startedAt;
}

// AFTER
if (session.startedAt > 0) {
    // If session ended, use endedAt; otherwise use current time (for active sessions)
    long endTime = session.endedAt != null ? session.endedAt : currentTime;
    sessionDuration = endTime - session.startedAt;
    
    // Only count reasonable durations (max 24 hours per session)
    if (sessionDuration < 0 || sessionDuration >= 24 * 60 * 60 * 1000) {
        sessionDuration = 0;
    }
}
```

## How It Works Now

### During Active Playback
1. Session starts → `startedAt` set, `endedAt` is `null`
2. User returns to Home → `loadListeningStats()` called
3. For active session: `endTime = currentTime` (not null!)
4. Duration calculated as: `currentTime - startedAt`
5. Time displayed includes elapsed time of active session ✅

### After Playback Ends
1. Playback completes → `endedAt` set in database
2. Session marked as ended
3. For completed session: `endTime = endedAt`
4. Duration calculated as: `endedAt - startedAt`
5. Time shows actual session duration ✅

### Safety Features

**24-Hour Cap**: Sessions with unrealistic durations (>24 hours) are skipped to prevent data corruption from clock changes or app crashes.

**Negative Duration Check**: Filters out invalid sessions where endTime < startTime.

## Update Flow

```
User starts playback (10:00 AM)
  ↓
Session created: startedAt=10:00, endedAt=null
  ↓
User returns to Home at 10:05 AM
  ↓
loadListeningStats() called
  ↓
Active session found: endTime = currentTime (10:05)
  ↓
Duration = 10:05 - 10:00 = 5 minutes
  ↓
Time displayed: "5m" ✅
  ↓
User returns to Home at 10:10 AM
  ↓
Duration = 10:10 - 10:00 = 10 minutes
  ↓
Time displayed: "10m" ✅
  ↓
Playback ends at 10:15 AM
  ↓
endedAt = 10:15 set in database
  ↓
Final duration = 10:15 - 10:00 = 15 minutes
  ↓
Time displayed: "15m" ✅
```

## Real-Time Updates

This fix works in conjunction with the existing broadcast system:

### 1. Verse Updates (Every Verse)
```
trackCurrentVerse() 
  → Broadcast: VERSE_TRACKED
  → HomeActivity receives
  → loadListeningStats() called
  → Time updated (now includes active session)
```

### 2. Session End Updates
```
onPlaybackStateChanged(STATE_ENDED)
  → markEnded() sets endedAt
  → Broadcast: SESSION_ENDED  
  → HomeActivity receives
  → loadListeningStats() called
  → Time updated with final duration
```

### 3. Manual Refresh (onResume)
```
User returns to Home
  → onResume() called
  → loadListeningStats() called
  → Time updated (active or completed)
```

## Testing Checklist

- [x] Start playback from Learn tab
- [x] Navigate to Home screen **during** playback
- [x] Verify time updates to show elapsed time
- [x] Wait a few verses and return to Home again
- [x] Verify time increased
- [x] Let playback complete
- [x] Return to Home
- [x] Verify final time is accurate

## Benefits

✅ **Consistent UX**: Verses and time now both update in real-time  
✅ **Immediate Feedback**: Users see listening progress instantly  
✅ **Accurate Tracking**: Active sessions properly counted  
✅ **No Data Loss**: Works with existing session-based system  
✅ **Safe Calculations**: 24-hour cap prevents data corruption  
✅ **Backward Compatible**: Works with both active and completed sessions  

## Technical Notes

- No database schema changes required
- No changes to PlaybackService needed
- Purely a calculation fix in HomeActivity
- Works with existing broadcast system
- Performance impact: negligible (same query, different calculation)

## Production Readiness

This fix is **production-ready** and addresses a critical UX issue. The time listening metric now properly reflects real-time activity, matching user expectations and providing instant motivation feedback.

---

**Status**: ✅ Fixed, Built, and Installed  
**Version**: Included in current debug build  
**Impact**: High (resolves user-facing inconsistency)  
**Risk**: Low (calculation logic change only, no schema/API changes)
