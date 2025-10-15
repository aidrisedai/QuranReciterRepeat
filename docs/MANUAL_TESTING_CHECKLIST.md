# Phase 3 Refactoring - Manual Testing Checklist

**Date:** January 15, 2025  
**Build:** assembleDebug SUCCESS ✅  
**APK Location:** `app/build/outputs/apk/debug/app-debug.apk`

---

## 🎯 Testing Strategy

Test each fragment systematically to verify:
1. ✅ **Functionality** - All features work as before
2. ✅ **State Persistence** - Dropdowns survive rotation
3. ✅ **Playback Controls** - Play/Pause/Stop buttons work correctly
4. ✅ **Tab Switching** - Switching tabs during playback works
5. ✅ **Edge Cases** - Rapid clicking, no reciter, invalid input

---

## 📋 Pre-Testing Setup

### Step 1: Install the App
```bash
# Install fresh build on connected device/emulator
cd /Users/azeezidris/AndroidStudioProjects/RepeatQuranWithCodex
./gradlew installDebug

# Or install manually:
# adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Configure Reciters
- [ ] Open app
- [ ] Go to Settings (if available) or Reciter selection
- [ ] Select at least one reciter
- [ ] Confirm selection is saved

### Step 3: Enable Developer Options (Optional)
- [ ] Enable "Don't keep activities" to test state persistence
- [ ] Enable "Show layout bounds" to verify UI rendering

---

## 🧪 Test Suite 1: VerseTabFragment

### Basic Functionality
- [ ] **Open VerseTab**
  - Navigate to Verse tab
  - Verify UI loads correctly
  - Check all dropdowns are visible

- [ ] **Test Surah Dropdown**
  - Click Surah dropdown
  - Type "001" - should filter to Al-Fatihah
  - Select "001 Al-Fatihah"
  - Verify Ayah dropdown updates (max 7 ayahs)

- [ ] **Test Ayah Dropdown**
  - Click Ayah dropdown
  - Verify it shows "1" through "7"
  - Select "1"

- [ ] **Test Real-time Validation**
  - Type invalid ayah number (e.g., "99")
  - Verify red border appears
  - Type valid ayah number
  - Verify red border disappears

### Playback Controls
- [ ] **Test Play Button**
  - Click Play button
  - Verify loading toast appears
  - Verify button changes to "Pause"
  - Verify icon changes to pause icon
  - Listen for audio playback to start

- [ ] **Test Pause Button**
  - While playing, click Pause
  - Verify audio pauses
  - Verify button changes to "Play"
  - Verify icon changes to play icon

- [ ] **Test Resume**
  - Click Play again
  - Verify audio resumes from same position

- [ ] **Test Stop Button**
  - Click Stop button
  - Verify audio stops
  - Verify "Stopped" toast appears
  - Verify Play button is enabled

### State Persistence (Rotation)
- [ ] **Setup State**
  - Select Surah "002 Al-Baqarah"
  - Select Ayah "255"
  - Do NOT start playback yet

- [ ] **Rotate Device**
  - Rotate device/emulator (Ctrl+F11 or Ctrl+F12)
  - Verify Surah dropdown still shows "002 Al-Baqarah"
  - Verify Ayah dropdown still shows "255"

- [ ] **Rotate During Playback**
  - Start playback
  - Rotate device while playing
  - Verify playback continues
  - Verify button state is correct (Pause)

### Edge Cases
- [ ] **Test Rapid Clicking**
  - Rapidly click Play button 5 times
  - Verify only one playback starts
  - Verify no crashes or errors

- [ ] **Test No Reciter**
  - Clear reciter selection (if possible)
  - Try to play
  - Verify error toast: "Select at least one reciter first"

- [ ] **Test Invalid Input**
  - Select Surah "001"
  - Type "999" in Ayah field
  - Try to play
  - Verify validation error appears

---

## 🧪 Test Suite 2: RangeTabFragment

### Basic Functionality
- [ ] **Open RangeTab**
  - Navigate to Range tab
  - Verify all 4 dropdowns are visible
  - Verify End Ayah and Play buttons are visible

- [ ] **Test Start Surah Dropdown**
  - Click Start Surah dropdown
  - Type "001" - should filter to Al-Fatihah
  - Select "001 Al-Fatihah"
  - Verify Start Ayah dropdown updates (max 7)

- [ ] **Test Smart Auto-Sync**
  - Select Start Surah "002 Al-Baqarah"
  - Verify End Surah automatically changes to "002 Al-Baqarah"
  - Select Start Surah "003 Ali 'Imran"
  - Verify End Surah automatically changes to "003 Ali 'Imran"

- [ ] **Test End Surah Dropdown**
  - Manually select End Surah "005 Al-Ma'idah"
  - Verify End Ayah dropdown updates correctly

- [ ] **Test Range Validation**
  - Set Start: Surah 5, Ayah 1
  - Set End: Surah 3, Ayah 1
  - Try to play
  - Verify error: "End before start"

### UI Visibility Workaround
- [ ] **Test UI Elements Visible**
  - After selecting dropdowns
  - Scroll down to verify End Ayah layout is visible
  - Verify Play and Stop buttons are visible
  - Verify no elements are cut off

### Playback Controls
- [ ] **Test Play Button**
  - Set valid range (e.g., Surah 1:1 to 1:3)
  - Click Play
  - Verify loading toast appears
  - Verify button changes to "Pause"

- [ ] **Test Range Playback**
  - Listen for audio to play verse 1
  - Wait for verse 2
  - Wait for verse 3
  - Verify playback stops or continues as configured

- [ ] **Test Stop Button**
  - During playback, click Stop
  - Verify audio stops immediately

### State Persistence (Rotation)
- [ ] **Setup State**
  - Set Start: Surah "002", Ayah "1"
  - Set End: Surah "002", Ayah "5"

- [ ] **Rotate Device**
  - Rotate device
  - Verify all 4 dropdowns retain values
  - Verify lastStartSurah tracking still works

### Edge Cases
- [ ] **Test Same Verse Range**
  - Set Start: Surah 1, Ayah 1
  - Set End: Surah 1, Ayah 1
  - Verify it plays single verse

- [ ] **Test Full Surah Range**
  - Set Start: Surah 1, Ayah 1
  - Set End: Surah 1, Ayah 7
  - Verify it plays entire surah

---

## 🧪 Test Suite 3: SurahTabFragment

### Basic Functionality
- [ ] **Open SurahTab**
  - Navigate to Surah tab
  - Verify dropdown is visible
  - Verify Play and Stop buttons visible

- [ ] **Test Surah Dropdown (Non-filterable)**
  - Click Surah dropdown
  - Type "001" - verify NO filtering (intentional UX)
  - Scroll to find "001 Al-Fatihah"
  - Select it

- [ ] **Test Direct Selection**
  - Click dropdown
  - Select "002 Al-Baqarah" directly
  - Verify selection is saved

### Playback Controls
- [ ] **Test Play Entire Surah**
  - Select "001 Al-Fatihah"
  - Click Play
  - Verify loading toast: "Loading surah 001…"
  - Verify button changes to "Pause"
  - Listen for audio to start

- [ ] **Test Surah Playback Progress**
  - Let it play for a few verses
  - Verify audio continues playing
  - Click Pause to stop

- [ ] **Test Stop Button**
  - Click Stop
  - Verify "Stopped" toast appears
  - Verify audio stops

### State Persistence (Rotation)
- [ ] **Setup State**
  - Select "112 Al-Ikhlas"

- [ ] **Rotate Device**
  - Rotate device
  - Verify dropdown still shows "112 Al-Ikhlas"

- [ ] **Rotate During Playback**
  - Start playback
  - Rotate device
  - Verify playback continues

### Edge Cases
- [ ] **Test Invalid Selection**
  - Manually type "999" if possible
  - Try to play
  - Verify validation error

---

## 🧪 Test Suite 4: PageTabFragment

### Basic Functionality
- [ ] **Open PageTab**
  - Navigate to Page tab
  - Verify page dropdown is visible
  - Verify helper text: "Enter page 1–604"

- [ ] **Test Common Pages Dropdown**
  - Click page dropdown
  - Verify common pages shown: 1, 11, 21, 31...591, 595-604
  - Select page "1"

- [ ] **Test Custom Filter**
  - Click dropdown
  - Type "5" - should show: 5, 50, 51...59, 500, 501...
  - Type "50" - should show: 50, 500, 501...509
  - Select a page

- [ ] **Test Last 10 Pages**
  - Click dropdown
  - Scroll to bottom
  - Verify pages 595-604 are individually listed

### Playback Controls
- [ ] **Test Play Page**
  - Select page "1"
  - Click Play
  - Verify loading toast: "Loading page 1…"
  - Verify button changes to "Pause"
  - Listen for audio to start

- [ ] **Test Page Playback**
  - Let it play for a few verses
  - Verify audio continues
  - Click Pause to stop

- [ ] **Test Stop Button**
  - Click Stop
  - Verify "Stopped" toast appears

### State Persistence (Rotation)
- [ ] **Setup State**
  - Select page "604" (last page)

- [ ] **Rotate Device**
  - Rotate device
  - Verify dropdown still shows "604"

### Edge Cases
- [ ] **Test Invalid Page Number**
  - Type "999" in page field
  - Try to play
  - Verify validation error: "Enter 1–604"

- [ ] **Test Boundary Pages**
  - Test page 1 (first page)
  - Test page 604 (last page)
  - Verify both work correctly

---

## 🧪 Test Suite 5: Cross-Fragment Tests

### Tab Switching During Playback
- [ ] **Verse → Range**
  - Start playback in VerseTab
  - Switch to RangeTab
  - Verify VerseTab audio continues
  - Switch back to VerseTab
  - Verify Pause button is correct

- [ ] **Range → Surah**
  - Start playback in RangeTab
  - Switch to SurahTab
  - Click Play in SurahTab
  - Verify RangeTab playback stops
  - Verify SurahTab playback starts

- [ ] **Surah → Page**
  - Start playback in SurahTab
  - Switch to PageTab
  - Click Play in PageTab
  - Verify SurahTab playback stops
  - Verify PageTab playback starts

- [ ] **Page → Verse**
  - Start playback in PageTab
  - Switch to VerseTab
  - Click Play in VerseTab
  - Verify PageTab playback stops
  - Verify VerseTab playback starts

### Resume After Tab Switch
- [ ] **Pause, Switch, Resume**
  - Start playback in VerseTab
  - Pause
  - Switch to RangeTab
  - Switch back to VerseTab
  - Click Play
  - Verify it resumes from where it paused

- [ ] **Stop, Switch, Play New**
  - Start playback in RangeTab
  - Stop
  - Switch to SurahTab
  - Select a surah
  - Click Play
  - Verify new playback starts

---

## 🧪 Test Suite 6: Memory & Performance

### Memory Leak Test
- [ ] **Rotate 10 Times**
  - Start playback in any tab
  - Rotate device 10 times quickly
  - Verify app doesn't crash
  - Verify memory usage stays stable

- [ ] **Switch Tabs 20 Times**
  - Start playback
  - Switch between all 4 tabs rapidly 20 times
  - Verify no crashes
  - Verify UI remains responsive

### Cooldown/Debounce Test
- [ ] **Rapid Play Clicks**
  - Click Play button 10 times rapidly
  - Verify only one playback starts
  - Verify button becomes disabled briefly
  - Verify button re-enables after cooldown

- [ ] **Rapid Tab Switches**
  - Switch tabs rapidly 10 times
  - Verify UI remains stable
  - Verify no duplicate listeners

---

## 🧪 Test Suite 7: Error Handling

### No Reciter Selected
- [ ] **Test All Tabs Without Reciter**
  - Clear reciter selection
  - Try to play in VerseTab → verify error
  - Try to play in RangeTab → verify error
  - Try to play in SurahTab → (no validation?)
  - Try to play in PageTab → verify error

### Network Issues (if applicable)
- [ ] **Test Offline Playback**
  - Disable Wi-Fi and mobile data
  - Try to play
  - Verify appropriate error message

### Background/Foreground
- [ ] **Test Background Playback**
  - Start playback
  - Press Home button
  - Verify playback continues in background
  - Verify notification appears (if implemented)

- [ ] **Test Foreground Return**
  - Return to app
  - Verify button state is correct (Pause)
  - Verify playback controls work

---

## 📊 Testing Results Template

### Summary
```
Date: _______________
Tester: _______________
Device: _______________
Android Version: _______________

Overall Result: ☐ PASS  ☐ FAIL  ☐ ISSUES FOUND

┌────────────────────────────────────────────┐
│  Test Suite         Pass  Fail  Issues     │
├────────────────────────────────────────────┤
│  VerseTab           ___   ___   ___        │
│  RangeTab           ___   ___   ___        │
│  SurahTab           ___   ___   ___        │
│  PageTab            ___   ___   ___        │
│  Cross-Fragment     ___   ___   ___        │
│  Memory/Perf        ___   ___   ___        │
│  Error Handling     ___   ___   ___        │
└────────────────────────────────────────────┘
```

### Issues Found
```
Issue #1: _______________________________________________
Severity: ☐ Critical  ☐ High  ☐ Medium  ☐ Low
Steps to Reproduce:
1. 
2. 
3. 
Expected: 
Actual: 
Fragment: 
Screenshot: 

Issue #2: _______________________________________________
...
```

---

## ✅ Testing Completion Checklist

- [ ] All test suites completed
- [ ] All issues documented
- [ ] Results shared with team
- [ ] Critical issues fixed
- [ ] Regression tests passed
- [ ] App ready for deployment

---

## 📞 Support

**If you find issues:**
1. Document in Issues Found section
2. Check backup files: `*.java.backup`
3. Check git log: `git log --oneline -10`
4. Rollback if needed: `git revert HEAD`

**For questions:**
- Review: `docs/PHASE_3_FINAL_REPORT.md`
- Review: `docs/PHASE_3_REFACTORING_PLAN.md`

---

**Happy Testing!** 🧪✨
