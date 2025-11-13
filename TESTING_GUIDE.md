# Testing Guide - Goal Management System

## Prerequisites

1. **Install the app**:
```bash
cd ~/AndroidStudioProjects/RepeatQuranWithCodex
./gradlew installDebug
```

2. **Check the app is installed**:
```bash
~/Library/Android/sdk/platform-tools/adb shell pm list packages | grep repeatquran
```

Should show: `package:com.repeatquran`

---

## Test 1: Launch Goal Input (Create a Goal)

### Option A: From Code (Quick Test)
Add a test button to your HomeActivity or any existing activity:

```java
// Add to HomeActivity.java temporarily:
Button testButton = new Button(this);
testButton.setText("Test Goal Creation");
testButton.setOnClickListener(v -> {
    Intent intent = new Intent(this, com.repeatquran.memorization.GoalInputActivity.class);
    startActivity(intent);
});
// Add to your layout
```

### Option B: Launch via ADB (Fastest)
```bash
~/Library/Android/sdk/platform-tools/adb shell am start -n com.repeatquran/com.repeatquran.memorization.GoalInputActivity
```

### What to Test:

1. **Text Input**:
   - Type: "Surah Al-Mulk"
   - ✅ Should show preview: "Surah 67 (Al-Mulk) • Complete surah • 30 verses"
   
2. **Try Different Inputs**:
   - "5 verses per day" → Should parse as daily goal
   - "Juz Amma" → Should show Surahs 78-114, 564 verses
   - "2:1-10" → Should show Al-Baqarah verses 1-10

3. **Voice Input**:
   - Tap "🎤 Use Voice Input"
   - Say: "Memorize Surah Al-Mulk"
   - ✅ Should fill text field and parse

4. **Strictness Selection**:
   - Try selecting each level (Lenient/Moderate/Strict)
   - ✅ Should stay selected

5. **Create Goal**:
   - Tap "Create Goal"
   - ✅ Should show success toast
   - ✅ Should close and return

### Expected Logs:
```bash
~/Library/Android/sdk/platform-tools/adb logcat | grep "GoalInput\|GoalParser"
```

Should see:
```
GoalParser: Detected surah by name: 67
GoalParser: Goal parsed successfully: Surah 67 (Al-Mulk) • 30 verses
GoalInputActivity: Goal created with ID: 1
```

---

## Test 2: View Goals List

### Launch Goal List:
```bash
~/Library/Android/sdk/platform-tools/adb shell am start -n com.repeatquran/com.repeatquran.memorization.GoalListActivity
```

### What to Test:

1. **Empty State** (if no goals):
   - ✅ Should show "📋 No active goals yet"
   - ✅ Should show FAB button

2. **With Goals** (after creating one):
   - ✅ Should show goal card with:
     - Title: "Surah 67 - Al-Mulk" (or your goal)
     - Progress: "0/30"
     - Progress bar at 0%
     - Strictness badge: "Moderate"
     - Goal type badge: "One-time" or "5 verses/day"
     - "Continue" button
     - "Pause" button

3. **Tabs**:
   - Tap "Paused" tab → Should show "No paused goals"
   - Tap "Completed" tab → Should show "No completed goals yet"
   - Tap "Active" tab → Should show your goal again

4. **Goal Actions**:
   - **Pause**: Tap "Pause" button
     - ✅ Goal should disappear from Active tab
     - ✅ Switch to "Paused" tab → Goal should appear there
     - ✅ Button should say "Resume" instead of "Continue"
   
   - **Resume**: Tap "Resume" button
     - ✅ Goal should move back to Active tab
   
   - **Delete**: Tap menu (⋮) → Delete
     - ✅ Should show confirmation dialog
     - ✅ Tap "Delete" → Goal removed
     - ✅ Should show "Goal deleted" toast

5. **Create New Goal**:
   - Tap FAB button
   - ✅ Should open Goal Input Activity
   - Create another goal
   - ✅ Should return and show new goal in list

6. **Continue Button** (placeholder):
   - Tap "Continue" on a goal
   - ✅ Should show toast: "Continue: Memorize Surah Al-Mulk"
   - (This will launch MemorizationSessionActivity once Phase 3 is complete)

### Expected Logs:
```bash
~/Library/Android/sdk/platform-tools/adb logcat | grep "GoalList\|GoalsAdapter"
```

Should see:
```
GoalListActivity: Loading stats from X sessions
GoalListActivity: Updating UI - Active goals: 1
GoalsAdapter: Binding goal: Surah 67
```

---

## Test 3: Database Persistence

### Test Data Survives App Restart:

1. **Create a goal**
2. **Close the app completely**:
```bash
~/Library/Android/sdk/platform-tools/adb shell am force-stop com.repeatquran
```

3. **Reopen Goal List**:
```bash
~/Library/Android/sdk/platform-tools/adb shell am start -n com.repeatquran/com.repeatquran.memorization.GoalListActivity
```

4. ✅ **Your goal should still be there!**

### Verify Database:
```bash
~/Library/Android/sdk/platform-tools/adb shell run-as com.repeatquran ls databases/
```

Should show: `repeat_quran.db`

### Check Goal Data:
```bash
~/Library/Android/sdk/platform-tools/adb shell run-as com.repeatquran sqlite3 databases/repeat_quran.db "SELECT * FROM memorization_goals;"
```

Should show your goal data with all fields.

---

## Test 4: Multiple Goals

### Create Multiple Goals:
1. "Surah Al-Mulk"
2. "5 verses per day"
3. "Last 10 surahs"

### Verify:
- ✅ All show in Active tab
- ✅ Each has correct verse count
- ✅ Progress bars all at 0%
- ✅ Can pause/resume each independently
- ✅ Only one shows as active in database (newest)

**Note**: System allows only ONE active goal at a time (by design - deactivates previous when creating new)

---

## Test 5: Edge Cases

### Invalid Inputs:
1. Type: "blah blah blah"
   - ✅ Should show error: "Could not understand goal"
   - ✅ Create button should stay disabled

2. Empty input:
   - ✅ Create button should be disabled

3. Type: "Surah 999"
   - ✅ Should handle gracefully (Ayah count will be 0 or error)

### Stress Test:
1. Create 10+ goals rapidly
   - ✅ Should all appear in list
   - ✅ No crashes
   - ✅ Smooth scrolling

2. Pause/Resume rapidly
   - ✅ Status updates correctly
   - ✅ No UI glitches

---

## Test 6: Integration with Existing Features

### From Home Screen:
If you have the Memorization card on your home screen, you can wire it up:

```java
// In HomeActivity.java, find memorizationSessionCard click:
memorizationSessionCard.setOnClickListener(v -> {
    // Check if there's an active goal
    new Thread(() -> {
        MemorizationGoalRepository goalRepo = new MemorizationGoalRepository(this);
        MemorizationGoalEntity activeGoal = goalRepo.getActiveGoal();
        
        runOnUiThread(() -> {
            if (activeGoal != null) {
                // Has active goal - show goals list
                Intent intent = new Intent(this, com.repeatquran.memorization.GoalListActivity.class);
                startActivity(intent);
            } else {
                // No goal - create one
                Intent intent = new Intent(this, com.repeatquran.memorization.GoalInputActivity.class);
                startActivity(intent);
            }
        });
    }).start();
});
```

---

## Common Issues & Solutions

### Issue: "ActivityNotFoundException"
**Solution**: Make sure activities are in AndroidManifest.xml
```bash
grep -n "GoalInputActivity\|GoalListActivity" app/src/main/AndroidManifest.xml
```

Should show both activities registered.

### Issue: App crashes on database access
**Solution**: Database migration is using fallbackToDestructiveMigration. Clear app data:
```bash
~/Library/Android/sdk/platform-tools/adb shell pm clear com.repeatquran
```

Then reinstall:
```bash
./gradlew installDebug
```

### Issue: Voice input doesn't work
**Solution**: Make sure microphone permission is granted in Settings → Apps → Repeat Quran → Permissions

### Issue: Progress not saving
**Solution**: Check logs for database errors:
```bash
~/Library/Android/sdk/platform-tools/adb logcat | grep "MemorizationGoalRepository\|Database"
```

---

## Quick Test Script

Save this as `test_goals.sh`:

```bash
#!/bin/bash
ADB=~/Library/Android/sdk/platform-tools/adb

echo "🧪 Testing Goal Management System..."
echo ""

echo "1. Installing app..."
./gradlew installDebug

echo ""
echo "2. Launching Goal Input..."
$ADB shell am start -n com.repeatquran/com.repeatquran.memorization.GoalInputActivity

echo ""
echo "👉 Create a goal in the app, then press Enter to continue..."
read

echo ""
echo "3. Launching Goal List..."
$ADB shell am start -n com.repeatquran/com.repeatquran.memorization.GoalListActivity

echo ""
echo "👉 Test pause/resume/delete, then press Enter to continue..."
read

echo ""
echo "4. Checking database..."
$ADB shell run-as com.repeatquran sqlite3 databases/repeat_quran.db "SELECT id, goalText, totalVerses, currentProgress FROM memorization_goals;"

echo ""
echo "✅ Testing complete!"
```

Make executable and run:
```bash
chmod +x test_goals.sh
./test_goals.sh
```

---

## Expected Results Summary

### ✅ Working Features:
- Goal creation with natural language
- Voice input parsing
- Goal list with tabs
- Pause/Resume functionality
- Delete with confirmation
- Progress tracking (0% for now)
- Database persistence
- Multiple goals management

### 🚧 Not Yet Working:
- "Continue" button → Will work when MemorizationSessionActivity is built
- Progress updates → Will work during memorization sessions
- Goal completion → Will trigger after completing all verses

---

## Screenshots to Verify

Take screenshots to verify UI:
```bash
~/Library/Android/sdk/platform-tools/adb exec-out screencap -p > screenshot.png
```

**Expected Screens**:
1. Goal Input with parsed preview
2. Goal List with active goal
3. Paused goals tab
4. Goal card showing progress
5. Delete confirmation dialog

---

## Next: Test Memorization Session (Phase 3)

Once you complete Phase 3 using the guide, test:
1. "Continue" button launches session
2. AI gives feedback on recitation
3. Error blocking works
4. Progress updates after correct verse
5. Celebration on completion

---

**Ready to test!** Start with the ADB commands above to quickly launch each screen. 🚀
