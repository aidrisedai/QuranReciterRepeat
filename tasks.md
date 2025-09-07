# 📋 Repeat Quran MVP — Units of Human Work (Final Engineer-Ready)

> Rule: Each UHW ≤ 90 minutes, independently verifiable.  
> Proof required: screenshot, demo video, APK, or log evidence.  
> Decisions: Nested sequential multi-reciter, lazy-fetch-next playback, internal caching, dynamic streaks, retry-once error handling, APK sideload release, light + dark themes, motivational streak banner.  

---

## Phase 1: Setup
- [x] **UHW-1: Project Scaffolding**  
  - Create new Android Studio project (`RepeatQuranApp`).  
  - Configure package name + min SDK.  
  - **Proof**: Screenshot of Hello World build on emulator.  

- [x] **UHW-2: Dependency Setup**  
  - Add Gradle deps (Hilt, Room, ExoPlayer, Material3 for dark mode).  
  - Validate sync.  
  - **Proof**: Screenshot of successful Gradle sync.  

 - [x] **UHW-3: Repo & ViewModel Stubs**  
  - Create empty `SessionViewModel`, `HistoryViewModel`, `PlaybackViewModel`.  
  - Add placeholder methods.  
  - **Proof**: Unit test that instantiates each VM.  

---

## Phase 2: Core Playback
- [x] **UHW-4: ExoPlayer Integration (Lazy-Fetch-Next)**  
  - Implement queue at verse-level with 2-verse buffer.  
  - **Proof**: Demo video of Surah playback.  
  - **QA Note**: Test weak WiFi → no gaps allowed.  

- [x] **UHW-5: Playback Service**  
  - Foreground Service + MediaSession (always on).  
  - Support background playback & Bluetooth handoff.  
  - **Proof**: Demo video of playback persisting across app switch + Bluetooth.  
  - **QA Note**: Must pause/resume after phone call.  

- [x] **UHW-6: Playback UI Controls**  
  - Compose UI: Play, Pause, Next, Prev.  
  - **Proof**: Screenshot of controls on emulator + physical device.  
  - **QA Note**: Verify light + dark mode consistency.  

---

## Phase 3: Repetition
- [x] **UHW-7: Repeat Dropdown**  
  - Values = {1,3,5,10,∞}.  
  - **Proof**: Screenshot of dropdown.  
  - **QA Note**: Selection persists into playback + logs.  

- [x] **UHW-8: Repeat Logic**  
  - Implement repeat counts in ExoPlayer queue.  
  - **Proof**: Demo video of 3× repeat execution.  
  - **QA Note**: Verify dropdown selection matches playback.  

---

## Phase 4: Multi-Reciter
- [ ] **UHW-9: Multi-Select Dropdown**  
  - Prevent duplicates. Number reciters in selection.  
  - **Proof**: Screenshot of dropdown with multiple selected.  

- [ ] **UHW-10: Nested Sequential Playback**  
  - Order = Reciter A → Reciter B → repeat whole cycle.  
  - **Proof**: Demo video showing nested loop.  
  - **QA Note**: Logs must show order matches playback.  

---

## Phase 5: History & Streaks
- [ ] **UHW-11: Local History Storage**  
  - Save all sessions in Room DB.  
  - **Proof**: Unit test inserting/retrieving records.  

- [ ] **UHW-12: Quick History UI**  
  - Display last 4 sessions on Home.  
  - **Proof**: Screenshot of Home with history list.  
  - **QA Note**: Verify DB has all sessions.  

- [ ] **UHW-13: Streak Counter (Encouragement Banner)**  
  - Compute streaks dynamically from logs.  
  - Banner = “🔥 X-day streak — keep going!”  
  - **Proof**: Screenshots across 3 days with skip.  

---

## Phase 6: Offline & Downloads
- [ ] **UHW-14: Progressive Caching**  
  - Save verse files in internal storage.  
  - **Proof**: Device file explorer screenshot.  

- [ ] **UHW-15: Downloads Tab**  
  - ✅ if all verses cached, else ⬇️.  
  - **Proof**: Screenshot of Downloads tab.  
  - **QA Note**: Clear cached file → status updates correctly.  

- [ ] **UHW-16: Offline Playback (With Online Fallback)**  
  - If not cached → fetch online; if offline → error message.  
  - **Proof**: Demo video in airplane mode.  

---

## Phase 7: Resume & Error Handling
- [ ] **UHW-17: Session Resume (Exact Position)**  
  - Save ExoPlayer millisecond state.  
  - **Proof**: Demo video showing resume mid-verse.  

- [ ] **UHW-18: Error Handling (Retry Once + Actionable)**  
  - Retry failed verse once. Show “Retry / Skip” if still failing.  
  - **Proof**: Manual test log.  

---

## Phase 8: QA Harness
- [ ] **UHW-19: Unit Tests (Core + Edge Cases)**  
  - Test repeat logic, reciter queue, history DB, caching reuse, resume state, error retry.  
  - **Proof**: CI test log screenshot.  

- [ ] **UHW-20: Stress Test Playback**  
  - Simulate pause/resume, switch reciters mid-play, call interruption.  
  - **Proof**: Manual test log.  

---

## Phase 9: Onboarding & Analytics
- [ ] **UHW-21: Onboarding Screen (Static)**  
  - One intro screen.  
  - **Proof**: Screenshot.  

- [ ] **UHW-22: Minimal Analytics Logs**  
  - Log play, pause, repeat, errors (local only).  
  - **Proof**: Logcat screenshot.  

---

## Phase 10: Presets & Release
- [ ] **UHW-23: Preset Management (Save/Edit)**  
  - Save, edit, replay presets in History tab.  
  - **Proof**: Screenshot + playback log.  

- [ ] **UHW-24: APK Packaging (Sideload)**  
  - Generate signed APK, share with testers.  
  - **Proof**: APK file + installation demo.  

---

## Phase 11: Theming
- [ ] **UHW-25: Dark Mode Toggle**  
  - Add toggle in settings; persist preference.  
  - **Proof**: Screenshots of all tabs in both modes.  
