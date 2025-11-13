# Testing Checklist - Home Screen Updates

## ✅ What Was Fixed

### 1. **Greeting Text Height & Spacing**
- ✅ Reduced from 32sp to 24sp
- ✅ Reduced subtitle from 18sp to 15sp  
- ✅ Reduced top margin from 24dp to 16dp
- ✅ More compact, less overwhelming

### 2. **Islamic & Fun Greetings**
Added varied greetings based on time of day:

**Morning (5 AM - 12 PM):**
- "Sabah al-khair! ☀️"
- "Rise and shine! 🌅"
- "Good Morning! 🌤️"
- "Blessed morning to you! ✨"

**Afternoon (12 PM - 5 PM):**
- "Masa' al-khair! ☀️"
- "Good Afternoon! 🌞"
- "Hope you're having a blessed day! 💫"
- "Great to see you! 🌟"

**Evening (5 PM - 9 PM):**
- "Masa' al-khair! 🌙"
- "Good Evening! ⭐"
- "Blessed evening to you! 🌃"
- "Peace be upon you! 🌙"

**Night (9 PM - 5 AM):**
- "Tisbah 'ala khair! 🌙"
- "Good Night! ✨"
- "May your night be blessed! 💤"
- "Rest well! 🌠"

*Greetings rotate daily for variety!*

### 3. **Listening Metrics - Now With Logging**
Added debug logging to track:
- Number of sessions loaded
- Verses calculated per session
- Listening time per session
- Total calculations

### 4. **Daily Streak Tracker - Enhanced**
Improved with:
- Better week boundary detection
- More sessions checked (100 vs 30)
- Detailed logging for debugging
- Accurate day-of-week matching

## 🧪 How to Test

### Test 1: Greetings ✨
1. **Launch the app**
2. **Check the greeting** - Should be smaller and appropriate for current time
3. **Wait until next time period** (or change device time)
4. **Force close and reopen app** - Greeting should change
5. **Verify emoji appears** with the greeting

**Expected Results:**
- Greeting text is comfortable size (not too big)
- Subtitle is visible but subtle
- Appropriate greeting for time of day
- Fun emoji included

---

### Test 2: Verses Listened 🎧

#### If You Have Existing Sessions:
1. **Check the verse count** - Should show actual number
2. **Open Android Studio Logcat** (or use `adb logcat`)
3. **Filter for "HomeActivity"**
4. **Look for logs like:**
   ```
   Loading stats from X sessions
   Session: range, verses: 25, started: ...
   Total verses: 150, Total time: 45 min
   Updating UI with verses: 150
   ```

#### If Starting Fresh (0 verses):
1. **Verse count should show "0 ayat"**
2. **Play a listening session** (Go to Learn → Select verses → Play)
3. **Let it complete or stop it**
4. **Return to Home screen**
5. **Verses should increase!**

**Expected Results:**
- Shows actual number, not just 0
- Updates after each listening session
- Logs show calculation details

---

### Test 3: Time Listened ⏱️

#### Check Current Time:
1. **Look at "Time Listened" card**
2. **Should show format like:**
   - "45 min" (if less than 1 hour)
   - "2h 30m" (if 1+ hours)
   - "0 min" (if no sessions yet)

#### Verify Calculation:
1. **Check logs for:**
   ```
   Duration: X minutes
   Total time: X min
   Time text: Xh Xm
   ```
2. **Time should match your actual listening history**

**Expected Results:**
- Shows cumulative listening time
- Format is readable (hours + minutes)
- Updates after sessions

---

### Test 4: Daily Streak 🔥

#### Understanding the Display:
- **M T W T F S S** = Monday through Sunday
- **✓ in filled circle** = Day completed
- **Empty circle** = Day not done yet
- **Resets every Sunday**

#### Testing Steps:
1. **Check today's day** - Should show checkmark if you listened today
2. **Check logs for:**
   ```
   Checking streak from X recent sessions
   Week starts at: [date]
   Session on Mon: range
   Week completed: Sun=false, Mon=true, Tue=true, ...
   ```
3. **Play a session today** (if not done)
4. **Return to home**
5. **Today should now have checkmark**

**Expected Results:**
- Current week displayed correctly
- Days with listening show ✓
- Days without listening show empty circle
- Accurate based on session history

---

## 🔍 Debugging via Logs

To see detailed logs:

```bash
# Connect device and run:
adb logcat | grep "HomeActivity"

# You should see:
# - Loading stats from X sessions
# - Session calculations
# - Total verses and time
# - Streak checking
# - Week completion status
```

### Key Log Messages to Look For:

**Metrics:**
```
HomeActivity: Loading stats from 15 sessions
HomeActivity: Session: range, verses: 25, started: 1234567890
HomeActivity: Duration: 5 minutes
HomeActivity: Total verses: 375, Total time: 75 min
HomeActivity: Updating UI with verses: 375
HomeActivity: Time text: 1h 15m
```

**Streak:**
```
HomeActivity: Checking streak from 100 recent sessions
HomeActivity: Week starts at: Sun Nov 03 00:00:00
HomeActivity: Session on Mon: single
HomeActivity: Session on Thu: range
HomeActivity: Week completed: Sun=false, Mon=true, Tue=false, Wed=false, Thu=true, Fri=false, Sat=false
```

---

## ✅ Confirmation Checklist

Before moving to next tasks, verify:

- [ ] **Greeting is smaller and less overwhelming**
- [ ] **Greeting changes based on time of day**
- [ ] **Islamic greetings appear (Sabah al-khair, etc.)**
- [ ] **Emojis show in greetings**
- [ ] **Verses Listened shows actual number** (not just 0)
- [ ] **Time Listened shows actual time** (not just "0 min")
- [ ] **Verses increase after listening sessions**
- [ ] **Time increases after listening sessions**
- [ ] **Daily streak shows checkmarks for completed days**
- [ ] **Streak updates after today's listening**
- [ ] **Logs show calculations working** (via adb logcat)

---

## 🐛 If Something Doesn't Work

### Issue: Verses/Time Still Shows 0

**Check:**
1. Do you have any listening sessions in history?
2. Check logs - are sessions being loaded?
3. Try playing a session and stopping it

**Fix:**
- Sessions need `endedAt` timestamp to count
- Must be in database (SessionEntity table)

### Issue: Streak Not Working

**Check:**
1. Are there sessions this week?
2. Check logs for week boundary
3. Verify session timestamps

**Fix:**
- Streak only shows current week (Sun-Sat)
- Sessions must have `startedAt` > week start time

### Issue: Greeting Not Changing

**Check:**
1. What time is it on device?
2. Is time in correct range?

**Fix:**
- Close app completely (swipe away)
- Reopen - greeting should update

---

## 📊 Expected Numbers

### For New Users:
- Verses Listened: **0 ayat**
- Time Listened: **0 min**
- Streak: **All empty circles**

### After First Session (e.g., 10 verses, 3 minutes):
- Verses Listened: **10 ayat**
- Time Listened: **3 min**
- Streak: **Today's circle has ✓**

### After Regular Use (e.g., 1 month):
- Verses Listened: **2,000+ ayat**
- Time Listened: **10h+ **
- Streak: **Multiple checkmarks**

---

## ✨ All Features Working!

Once you've confirmed all items in the checklist, we're ready to move forward in shaa Allah! 🎉

The home screen now:
- Has comfortable greeting size with Islamic touches
- Shows accurate listening metrics
- Tracks daily streaks correctly
- Updates in real-time
- Has detailed logging for debugging

**Next steps await your confirmation! 🚀**
