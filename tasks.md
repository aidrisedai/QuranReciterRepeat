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

## Phase 4: Pages (Priority Next)
- [x] **UHW-26: Page Dataset (Schema + Seeder)**  
  - Add Room table `page_segments` with rows: (page, order, surah, startAyah, endAyah).  
  - Bundle assets JSON for Madani pages; seed DB on first run.  
  - **Proof**: Instrumented test asserting known pages load with correct segment counts/order.  

- [x] **UHW-27: Page Selection UI**  
  - Add page input (1–604) with validation + “Load Page” button.  
  - **Proof**: Screenshot of UI; invalid input shows inline error.  

- [ ] **UHW-28: Page Playback Integration**  
  - ACTION_LOAD_PAGE: Build ayah list from `page_segments`, apply nested reciter cycle + repeat N/∞.  
  - **Proof**: Demo video of page playback; logs show cycle order + item counts.  
  
- [x] Implemented: wired UI Load Page to service; service builds cycle from DB and enqueues N/∞ cycles; logs order and sizes.  

---

## Phase 5: Multi-Reciter
- [ ] **UHW-9: Multi-Select Dropdown**  
  - Prevent duplicates. Number reciters in selection.  
  - **Proof**: Screenshot of dropdown with multiple selected.  

- [x] **UHW-10: Nested Sequential Playback**  
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
- [x] **UHW-14: Progressive Caching**  
  - Save verse files in internal storage.  
  - Scope: single verse, ranges, pages, and full surahs (per-reciter).  
  - **Proof**: Device file explorer screenshot under `files/audio/<reciterId>/<SSS><AAA>.mp3` + Logcat `CacheManager` lines.  

- [x] **UHW-15: Downloads Tab**  
  - ✅ if all verses cached, else ⬇️.  
  - Implemented as Downloads screen: per-reciter, check/download/clear for Surah and Page.  
  - **Proof**: Screenshot of Downloads screen with status; Clear then Check shows updated status.  
  - **QA Note**: Clear cached file → status updates correctly.  

- [x] **UHW-16: Offline Playback (With Online Fallback)**  
  - If not cached → fetch online; if offline → error message.  
  - **Proof**: Demo video in airplane mode showing cached playback and uncached skips + log export.  
  
  Implemented: per-item skip with toast + logs when offline; upfront warning when zero cached in a selection; no playback stalls.  

---

## Phase 7: Resume & Error Handling
- [x] **UHW-17: Session Resume (Exact Position)**  
  - Save ExoPlayer millisecond state.  
  - Implemented: periodic save of source context + media index + ms position; Resume action rebuilds queue and seeks to exact position for single/range/page/surah with saved reciters and repeat.  
  - **Proof**: Demo video showing resume mid-verse across sources.  

- [x] **UHW-18: Error Handling (Retry Once + Actionable)**  
  - Retry failed verse once. Show “Retry / Skip” if still failing.  
  - Implemented: per-item retry map; on online error → auto-retry once; on second failure → notification actions “Retry / Skip” handled by service; offline path unchanged (skip with toast).  
  - **Proof**: Manual test log with failure, retry, and actionable step.  

---

## Phase 8: QA Harness
- [x] **UHW-19: Unit Tests (Core + Edge Cases)**  
  - Added JVM + Robolectric tests for: repeat enqueue (single, repeat=3), history DB insert/retrieve, cache path+hit, resume state capture.  
  - Deferred: full player error retry simulation (partly covered via service actions), deep page DB cycle under resume (covered by service IO fix).  
  - **Proof**: Local test run log/screenshot under `docs/proof/UHW-19/`.  

- [ ] **UHW-20: Stress Test Playback**  
  - Simulate pause/resume, switch reciters mid-play, call interruption.  
  - **Proof**: Manual test log.  

---

## Phase 9: Onboarding & Analytics
- [x] **UHW-21: Onboarding Screen (Static)**  
  - One-time fullscreen intro shown on first launch with “Get Started”.  
  - Skips on subsequent launches (persisted in `onboarding.seen`).  
  - **Proof**: Screenshot in docs/proof/UHW-21/.  

- [x] **UHW-22: Minimal Analytics Logs**  
  - Log play, pause, repeat, errors (local only).  
  - Implemented: `AnalyticsLogger` writes structured events to Logcat and rotating files under `files/logs/analytics-*.log`. Hooked into app_open, repeat_set, load_* actions, and error paths.  
  - **Proof**: Logcat screenshot and a snippet from `analytics-0.log` in docs/proof/UHW-22/.  

- [ ] **UHW-22: Minimal Analytics Logs**  
  - Log play, pause, repeat, errors (local only).  
  - **Proof**: Logcat screenshot.  

---

## Phase 10: Presets & Release
- [x] **UHW-23: Preset Management (Save/Edit)**  
  - Room-backed presets: save from current inputs, list, play, rename/edit repeat, delete.  
  - UI: Presets section on Home with actions; uses saved reciters and repeat.  
  - **Proof**: Screenshot + playback log (play_request from preset).  

- [ ] **UHW-24: APK Packaging (Sideload)**  
  - Generate signed APK, share with testers.  
  - Implemented signingConfig scaffold using gradle.properties; local keystore; assembleRelease generates app-release.apk.  
  - **Proof**: APK file + installation demo (folder screenshot + install clip).  

---

## Phase 11: Theming
- [ ] **UHW-25: Dark Mode Toggle**  
  - Add toggle in settings; persist preference.  
  - **Proof**: Screenshots of all tabs in both modes.  

---

## Phase 12: UI Overhaul (Delight + Focus)
- [ ] **UHW-UI-1: Theme Tokens (Islamic Look)**  
  - Add Material color/typography/shape tokens (deep green + gold), light/dark palettes.  
  - Proof: Screenshots (Home) in light/dark.  

- [ ] **UHW-UI-2: Iconography + Header Polish**  
  - Update header/iconography, spacing, dividers; keep layout.  
  - Proof: Before/after screenshot.  

- [ ] **UHW-UI-3: Tabs Navigation (Verse | Range | Page | Surah)**  
  - Add TabLayout + ViewPager2 skeleton with 4 tabs.  
  - Proof: Short video of tab switching.  

- [ ] **UHW-UI-4: Persist Last Mode**  
  - Save last selected tab; auto-open there; small toggle.  
  - Proof: Relaunch video restoring tab.  

- [ ] **UHW-UI-5: Verse Tab (Focused Form)**  
  - Move single-verse inputs + controls to Verse tab.  
  - Proof: Screenshot + play works.  

- [ ] **UHW-UI-6: Range Tab (Focused Form)**  
  - Move range inputs + controls to Range tab.  
  - Proof: Screenshot + logs.  

- [ ] **UHW-UI-7: Page Tab (Focused Form)**  
  - Move page input + controls to Page tab.  
  - Proof: Screenshot + logs.  

- [ ] **UHW-UI-8: Surah Tab (Focused Form)**  
  - Move surah picker + controls to Surah tab.  
  - Proof: Screenshot + logs.  

- [ ] **UHW-UI-9: Global Reciters + Repeat UX**  
  - Top pill row; persist globally; applies across tabs.  
  - Proof: Screenshot + carried values in logs.  

- [ ] **UHW-UI-10: Half Recitation Toggle (Simple Split)**  
  - Toggle on Range/Page/Surah: first half by A, second half by B (odd=ceil/floor; >2 cascade).  
  - Proof: Demo + logs with order and counts.  

- [ ] **UHW-UI-11: Visual QA + RTL/Arabic**  
  - RTL sanity, Arabic headings font, padding polish.  
  - Proof: RTL screenshots.  

- [ ] **UHW-UI-12: Analytics + Downloads Restyle**  
  - Log tab usage + half toggle; Downloads aligned to new palette.  
  - Proof: Logcat + screenshot.  

---

## Phase 8: QA Harness
- [x] **UHW-20: Stress Test Playback**  
  - Add QA screen to simulate rapid controls, focus loss/gain, reciter switch, and ∞ soak.  
  - **Proof**: Logcat export under `docs/proof/UHW-20/stress-log.txt` and optional short clips.  
