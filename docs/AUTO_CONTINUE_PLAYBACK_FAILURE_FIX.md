# Auto-Continue Playback Failure Analysis & Fix

## Issue Report
User gets "Playback failed" notification when a page finishes and auto-continues to the next page.

## Root Cause

### The Problem Flow
1. **Page 5 finishes playing** → `STATE_ENDED`
2. **Auto-continue broadcast sent** → PageTabFragment receives it
3. **Content validation passes** (resumePage=5, currentPage=5)
4. **navigateNext() called** → Updates UI to page 6
5. **loadAndPlay() called** → Sends `ACTION_LOAD_PAGE` with page=6
6. **PlaybackService tries to load page 6 audio**
7. **onPlayerError() fires** (missing audio file or network issue)
8. **After 1 retry, shows "Playback failed" notification**

### Why It Happens
The `isContentForThisFragment()` validation has a race condition:
- It checks if `resumePage == currentPage` 
- During auto-continue, both are still the old page number (5)
- Validation passes ✅
- Fragment navigates to next page (6)
- But **resumePage in SharedPreferences is still 5** 
- Service tries to load page 6 but encounters error
- Error notification shown

### The Real Issue
The `onPlayerError()` handler (PlaybackService.java:203-248) shows an error notification after **any** playback failure, including those that happen during auto-continue. This is too aggressive.

## Solutions

### Option 1: Graceful Auto-Continue Error Handling (RECOMMENDED)
Suppress error notifications during the auto-continue window and silently skip to the next item or stop gracefully.

**Pros:**
- Non-disruptive user experience
- Auto-continue feels seamless
- Users won't see scary error messages

**Cons:**
- Might hide legitimate issues
- User won't know why playback stopped

### Option 2: Stricter Content Validation
Update `isContentForThisFragment()` to check if we're in an auto-continue scenario and validate against the **next** page number.

**Pros:**
- Prevents incorrect fragment from handling auto-continue
- More precise validation

**Cons:**
- Complex logic
- Doesn't solve the underlying error handling issue

### Option 3: Pre-flight Check Before Auto-Continue
Check if the next page's audio is cached/available before triggering auto-continue.

**Pros:**
- Prevents errors proactively
- Clean user experience

**Cons:**
- Requires async check before navigation
- Slows down auto-continue
- Complex implementation

## Recommended Fix: Option 1 (Graceful Error Handling)

Add a flag to track when auto-continue is active and suppress error notifications during that window.

### Implementation Steps:

1. **Add auto-continue state flag**
   ```java
   private volatile boolean isAutoContinuing = false;
   ```

2. **Set flag when auto-continue broadcast is sent**
   ```java
   if (autoContinue) {
       isAutoContinuing = true;
       // Clear flag after 5 seconds (generous timeout)
       mainHandler.postDelayed(() -> isAutoContinuing = false, 5000);
       sendBroadcast(autoContinueIntent);
   }
   ```

3. **Suppress error notification during auto-continue**
   ```java
   public void onPlayerError(@NonNull PlaybackException error) {
       if (isAutoContinuing) {
           Log.w("PlaybackService", "Error during auto-continue, skipping silently");
           isAutoContinuing = false;
           mainHandler.post(() -> {
               if (player.hasNextMediaItem()) {
                   player.seekToNextMediaItem();
               } else {
                   player.stop();
               }
           });
           return;
       }
       // ... existing error handling
   }
   ```

### Alternative: Simpler Fix
Just catch errors within the first 3 seconds of any new page load (not just auto-continue) and skip silently:

```java
private volatile long lastLoadTime = 0;

// In handleLoadPage():
lastLoadTime = System.currentTimeMillis();

// In onPlayerError():
long timeSinceLoad = System.currentTimeMillis() - lastLoadTime;
if (timeSinceLoad < 3000) {
    // Error within 3 seconds of loading - likely missing audio
    Log.w("PlaybackService", "Quick error after load, skipping item");
    mainHandler.post(() -> {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem();
        else player.stop();
    });
    return;
}
```

This is simpler and handles all quick-fail scenarios, not just auto-continue.

## Testing After Fix
1. Enable auto-continue
2. Play a page where the next page has missing audio
3. Let it finish and auto-continue
4. **Expected:** Silently skips or stops, NO error notification
5. **Verify:** Check logcat for warning message

## Files to Modify
- `PlaybackService.java` (onPlayerError method, lines 203-248)

## Priority
🔴 HIGH - Affects user experience during normal usage of the auto-continue feature
