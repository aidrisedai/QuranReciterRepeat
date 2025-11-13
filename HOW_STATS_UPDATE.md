# How Stats Update - Complete Technical Guide 📊

## ✨ The Amazing Update System

Your listening stats now update **automatically and in real-time**! Here's exactly how it works:

---

## 🔄 3 Ways Stats Update

### 1. **When You Open the App** 🚀
- App launches → `onCreate()` called
- Immediately calculates stats from database
- Displays current numbers

### 2. **When You Return to Home Screen** 🏠
- Switch to another app → Come back
- `onResume()` called automatically
- Refreshes all stats instantly
- Streak tracker also updates

### 3. **When A Listening Session Ends** ⚡ **[NEW!]**
- **Automatic Real-Time Updates!**
- Session completes → PlaybackService broadcasts
- HomeActivity receives broadcast
- Stats refresh immediately **without** leaving the screen
- You see updated numbers right away!

---

## 🎯 The Complete Flow

### Step-by-Step: What Happens When You Listen

```
1. You tap "Play" in Learn section
   ↓
2. PlaybackService starts playing verses
   ↓
3. Session saved to database with:
   - startedAt: current timestamp
   - verse details (surah, ayah, range, etc.)
   - repeat count
   ↓
4. You listen... 🎧
   ↓
5. Playback finishes (or you stop it)
   ↓
6. PlaybackService marks session as ended:
   - Updates endedAt: timestamp
   - Saves to database
   ↓
7. PlaybackService broadcasts: "SESSION_ENDED" 📢
   ↓
8. HomeActivity receives broadcast (if open)
   ↓
9. HomeActivity automatically refreshes stats:
   - Queries all sessions from database
   - Calculates Today/This Week/All Time
   - Updates UI with new numbers
   ↓
10. You see updated stats! ✨
```

---

## 💾 How Data is Stored

### Session Database (SessionEntity)
```java
Session {
    id: 123
    startedAt: 1699056789000  // milliseconds
    endedAt: 1699056969000    // milliseconds (saved when playback ends)
    sourceType: "range"        // single, range, page, surah
    startSurah: 2
    startAyah: 1
    endSurah: 2
    endAyah: 10
    repeatCount: 3
    recitersCsv: "husary,minshawi"
    cyclesRequested: null
    cyclesCompleted: null
}
```

### Key Fields:
- **startedAt**: When playback began (always set)
- **endedAt**: When playback finished (set on completion)
- **sourceType**: How you selected verses
- **verse details**: What was played
- **repeatCount**: How many times

---

## 🧮 How Stats Are Calculated

### Verses Listened Calculation

```java
For each session:
    if (sourceType == "single"):
        verses = 1 × repeatCount
    
    else if (sourceType == "range"):
        if (same surah):
            verses = (endAyah - startAyah + 1) × repeatCount
        else:
            verses = (calculate across surahs) × repeatCount
    
    else if (sourceType == "surah"):
        verses = (total ayat in surah) × repeatCount
    
    else if (sourceType == "page"):
        verses = ~15 × repeatCount  // estimate
    
    Add to total
```

**Example:**
- Play verses 2:1-10 (10 verses)
- Repeat 3 times
- Result: **30 verses** counted

### Time Listened Calculation

```java
For each session:
    if (endedAt exists and startedAt > 0):
        duration = endedAt - startedAt  // in milliseconds
        Add to total time
```

**Example:**
- Started: 10:00:00 AM
- Ended: 10:15:30 AM
- Duration: **15m 30s** counted

### Time Period Filtering

**Today:**
```java
Calendar today = midnight today
For each session:
    if (session.startedAt >= today):
        count it
```

**This Week:**
```java
Calendar weekStart = last Sunday at midnight
For each session:
    if (session.startedAt >= weekStart):
        count it
```

**All Time:**
```java
Count all sessions ever
```

---

## 📡 The Broadcast System

### In PlaybackService (When Session Ends):
```java
@Override
public void onPlaybackStateChanged(int state) {
    if (state == Player.STATE_ENDED && currentSessionId != null) {
        // Save session end time
        sessionRepo.markEnded(id, System.currentTimeMillis(), cycles);
        
        // Broadcast to anyone listening
        Intent intent = new Intent(ACTION_SESSION_ENDED);
        sendBroadcast(intent);
        
        Log: "Session ended broadcast sent"
    }
}
```

### In HomeActivity (Listening for Updates):
```java
@Override
protected void onResume() {
    // Register receiver
    sessionEndedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log: "Session ended - refreshing stats"
            
            // Refresh everything!
            loadListeningStats();
            setupStreakTracker();
        }
    };
    
    registerReceiver(sessionEndedReceiver, filter);
}

@Override
protected void onPause() {
    // Clean up
    unregisterReceiver(sessionEndedReceiver);
}
```

---

## 🔍 How to Verify It's Working

### Test Scenario:

1. **Open the app** → Note current stats
   ```
   Today: 25 verses, 15m
   ```

2. **Stay on Home screen** (don't navigate away)

3. **Open Learn section in another tab/window**

4. **Play some verses** (e.g., 5 verses, 2 minutes)

5. **Let playback complete**

6. **Go back to Home screen**

7. **Stats should automatically update!**
   ```
   Today: 30 verses, 17m  ✨ (updated!)
   ```

### Check Logs:
```bash
adb logcat | grep "HomeActivity\|PlaybackService"
```

**You'll see:**
```
PlaybackService: Playback ended
PlaybackService: Session ended broadcast sent
HomeActivity: Session ended broadcast received - refreshing stats
HomeActivity: Loading stats from X sessions
HomeActivity: Updating UI - Today: 30, Week: 150, Total: 1234
```

---

## 🎯 Why This is Amazing

### Before (Old System):
- ❌ Had to close and reopen app
- ❌ Manual refresh only
- ❌ Stats felt stale
- ❌ No real-time feedback

### After (New System):
- ✅ **Automatic updates!**
- ✅ **Real-time refresh**
- ✅ **Instant feedback**
- ✅ **Live stats** while using app
- ✅ **Background updates** via broadcast
- ✅ **No user action needed**

---

## 🛠️ Technical Implementation Details

### Thread Safety:
- Database queries run on **background thread**
- UI updates run on **main thread** (via `runOnUiThread()`)
- Prevents UI freezing

### Performance:
- Efficient database queries (indexed by timestamp)
- Calculations done once per update
- Cached in memory during display
- Broadcast receiver lightweight

### Battery Friendly:
- Receiver only active when HomeActivity visible
- Unregistered when not needed
- No background services
- No polling

### Reliability:
- Sessions saved immediately when playback starts
- End time updated when playback completes
- Database transactions atomic
- Broadcast guaranteed delivery to registered receivers

---

## 📊 What Gets Tracked

### Automatically Captured:
✅ Verses listened (calculated from session type)
✅ Time listened (actual duration)
✅ Today's activity
✅ This week's activity
✅ All-time totals
✅ Which days you listened (for streak)

### Not Tracked:
❌ Individual verse details in stats (stored in sessions)
❌ Specific reciters used (stored but not in stats UI)
❌ Pause/resume within session
❌ Skips or interruptions

---

## 🎨 UI Update Flow

```
Broadcast Received
    ↓
loadListeningStats() called
    ↓
Background thread:
    - Query all sessions
    - Calculate Today stats
    - Calculate Week stats
    - Calculate All Time stats
    ↓
Main thread:
    - versesTodayCount.setText("30")
    - versesWeekCount.setText("150")
    - versesListenedCount.setText("1234")
    - timeTodayCount.setText("17m")
    - timeWeekCount.setText("2h 30m")
    - listeningTimeCount.setText("15h 45m")
    ↓
User sees updated numbers! ✨
```

---

## 💡 Pro Tips

### To See Updates Instantly:
1. Keep Home screen open
2. Start playback from Learn tab
3. Let it complete
4. Stats update automatically!

### To Force Refresh:
1. Navigate away from Home
2. Return to Home
3. `onResume()` triggers refresh

### To Debug:
```bash
# Watch real-time logs
adb logcat | grep "HomeActivity"

# Look for:
# - "Loading stats from X sessions"
# - "Updating UI - Today: X, Week: Y, Total: Z"
# - "Session ended broadcast received"
```

---

## 🚀 Summary

**Your stats now update in 3 amazing ways:**

1. **App Launch** - Immediate calculation
2. **Return to Home** - Auto-refresh on resume
3. **Session Complete** - Real-time broadcast update ⚡

**Everything is automatic, real-time, and battery-friendly!**

The system tracks actual listening data from your database, calculates accurately, and displays instantly. No manual refresh needed—it just works! ✨

---

**Enjoy your real-time listening stats! 🎧📊**
