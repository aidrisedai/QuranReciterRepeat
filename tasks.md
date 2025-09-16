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

- [x] **UHW-28: Page Playback Integration**  
  - ACTION_LOAD_PAGE: Build ayah list from `page_segments`, apply nested reciter cycle + repeat N/∞.  
  - **Proof**: Demo video of page playback; logs show cycle order + item counts.  
  

---

## Phase 5: Multi-Reciter
- [x] **UHW-9: Multi-Select Dropdown**  
  - Prevent duplicates. Number reciters in selection.  
  - **Proof**: Screenshot of dropdown with multiple selected.  

- [x] **UHW-10: Nested Sequential Playback**  
  - Order = Reciter A → Reciter B → repeat whole cycle.  
  - **Proof**: Demo video showing nested loop.  
  - **QA Note**: Logs must show order matches playback.  

---

## Phase 5: History & Streaks
- [x] **UHW-11: Local History Storage**  
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
 

---

## Phase 10: Presets & Release
- [x] **UHW-23: Preset Management (Save/Edit)**  
  - Room-backed presets: save from current inputs, list, play, rename/edit repeat, delete.  
  - UI: Presets section on Home with actions; uses saved reciters and repeat.  
  - **Proof**: Screenshot + playback log (play_request from preset).  

- [x] **UHW-24: APK Packaging (Sideload)**  
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
- [x] **UHW-UI-1: Theme Tokens (Islamic Look)**  
  - Add Material color/typography/shape tokens (deep green + gold), light/dark palettes.  
  - Proof: Screenshots (Home) in light/dark.  

- [x] **UHW-UI-2: Iconography + Header Polish**  
  - Update header/iconography, spacing, dividers; keep layout.  
  - Proof: Before/after screenshot.  
  
  Implemented: Added Material top app bar, section heading style, and subtle dividers/margins.  

- [x] **UHW-UI-3: Tabs Navigation (Verse | Range | Page | Surah)**  
  - Added TabLayout + ViewPager2 skeleton with 4 tabs and placeholder fragments.  
  - Proof: Short video of tab switching.  

- [x] **UHW-UI-4: Persist Last Mode**  
  - Save last selected tab; auto-open there; toolbar menu toggle “Remember my mode” (default on).  
  - Proof: Relaunch video restoring tab.  

- [x] **UHW-UI-5: Verse Tab (Focused Form)**  
  - Moved single-verse inputs + Play/Pause/Resume into Verse tab with validation and prefs for repeat/last surah.  
  - Proof: Screenshot + logs showing ACTION_LOAD_SINGLE with inputs.  

- [x] **UHW-UI-6: Range Tab (Focused Form)**  
  - Moved range inputs + controls to Range tab with validation and prefs for last start/end surah; wired ACTION_LOAD_RANGE.  
  - Proof: Screenshot + logs.  

- [x] **UHW-UI-7: Page Tab (Focused Form)**  
  - Moved page input + controls to Page tab; persists last page; wires ACTION_LOAD_PAGE.  
  - Proof: Screenshot + logs.  

- [x] **UHW-UI-8: Surah Tab (Focused Form)**  
  - Moved surah picker + controls to Surah tab; persists last surah; wires ACTION_LOAD_SURAH.  
  - Proof: Screenshot + logs.  

- [x] **UHW-UI-9: Global Reciters + Repeat UX**  
  - Added top pill row with Repeat and Reciters chips; taps open repeat dropdown or reciter picker; values persist and apply across tabs.  
  - Proof: Screenshot + logs showing carried values.  

- [x] **UHW-UI-10: Half Recitation Toggle (Simple Split)**  
  - Toggle on Range/Page/Surah tabs; per cycle, items split across reciters in contiguous segments (2 reciters → halves; >2 segments across order).  
  - Implemented extras + pref `ui.half.split`; service builds cycle accordingly; logs include half-split notes.  
  - Proof: Demo + logs with itemsPerCycle and correct reciter segments.  

- [x] **UHW-UI-11: Visual QA + RTL/Arabic**  
  - Added Arabic strings for key labels; verified RTL layout; adjusted labels to use resources; ensured start/end constraints and pill row stability.  
  - Proof: RTL screenshots.  

- [x] **UHW-UI-12: Analytics + Downloads Restyle**  
  - Added analytics for tab selections and half-split toggles; downloads screen logs open/download/clear actions.  
  - Restyled Downloads with a Material toolbar and section heading styles to match the palette.  
  - Proof: Logcat events + Downloads screenshot.  

---

## Phase 8: QA Harness
- [x] **UHW-20: Stress Test Playback**  
  - Add QA screen to simulate rapid controls, focus loss/gain, reciter switch, and ∞ soak.  
  - **Proof**: Logcat export under `docs/proof/UHW-20/stress-log.txt` and optional short clips.  

---

## Phase 13: Production Readiness
- [x] **UHW-PR-01: Pause/Play Resume Fix**  
  - Resume-if-needed: if paused with queue, Resume/Play continues from the same position; no rebuilds.  
  - Proof: Screen recording showing pause→play resuming exact ms.

- [x] **UHW-PR-02: Resume Button Logic Fix**  
  - Broadcast real playing state; disable Resume while playing; ACTION_RESUME rebuilds only when needed.  
  - Proof: Logs + demo where Resume is gated and works from cold start.

- [x] **UHW-PR-03: Downloads Back Navigation**  
  - Add toolbar up/back in Downloads to return to Home.  
  - Proof: Short clip showing back navigation.

- [ ] **UHW-PR-04: Media Notification Polish**  
  - Ensure notification Play/Pause actions reflect state; compact view actions correct; error notification cancels on recovery.  
  - Proof: Video of notification actions.

- [ ] **UHW-PR-05: Audio Focus + Interruptions**  
  - Request focus, duck on transient, pause/resume on calls; verify on device.  
  - Proof: Manual log on phone call and focus changes.

- [ ] **UHW-PR-06: Permissions & Policy Readiness**  
  - Post-notifications prompt (33+), Settings Privacy Policy link; Data Safety mapping draft.  
  - Proof: Screenshot of prompt + policy link.

- [ ] **UHW-PR-07: Crash/Log Collection (Local)**  
  - Verify analytics log rotation; add “Export Logs” in Settings for QA.  
  - Proof: Exported log file.

- [ ] **UHW-PR-08: Release Hardening**  
  - Enable shrinker/minify; keep rules; version bump; signed release sanity.  
  - Proof: assembleRelease output and smoke run.

- [ ] **UHW-PR-09: UI Polish (States)**  
  - Disabled states for Resume when playing; empty/loading states polished; Day/Night checks.  
  - Proof: Screenshots in both modes.

- [ ] **UHW-PR-10: Store Assets**  
  - Verify adaptive/round icons; capture screenshots; feature graphic draft.  
  - Proof: /store_assets folder.

- [ ] **UHW-PR-11: Smoke Tests**  
  - Robolectric tests: play→pause→play; recent sessions filter; Downloads back nav.  
  - Proof: Test logs.

- [ ] **UHW-PR-12: StrictMode Sweep**  
  - Identify and move main-thread I/O; confirm clean logs for typical flows.  
  - Proof: StrictMode log capture.

- [x] **UHW-PR-13: Controls Consolidation**  
  - Keep Play on tabs; move Pause/Resume to a single toolbar toggle that reflects state; remove per-tab Pause/Resume.  
  - Proof: Short video showing Play from a tab and global Pause/Resume working across tabs.

- [x] **UHW-PR-14: Single Source of Truth (State Broadcast)**  
  - Broadcast on play/pause changes and after enqueue so UI toggles enable/labels reliably.  
  - Proof: Video showing toggle flips immediately on Play/Pause.

- [x] **UHW-PR-15: Service Toggle Action (Simple)**  
  - Add ACTION_TOGGLE in service: if playing → pause(); else → resume/rebuild.  
  - Proof: One-button control works regardless of state.

- [x] **UHW-PR-16: Controls Row Simplification (One Toggle)**  
  - Controls row uses a single Pause/Resume toggle (sends ACTION_TOGGLE); toolbar toggle mirrors it.  
  - Proof: Both toggles stay in sync.

- [x] **UHW-PR-17: Tab Play Consistency**  
  - Ensure Play on all tabs only loads/starts selection (no implicit toggle).  
  - Proof: Play never pauses; only builds and starts.

- [ ] **UHW-PR-18: UI Binding and Enablement**  
  - Bind toggles solely to broadcast extras (hasQueue, playing); ensure initial enable on first enqueue.  
  - Proof: Toggle enabled after first Play; updates on Pause/Resume.
