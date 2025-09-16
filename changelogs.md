# 📜 Repeat Quran MVP — Changelogs

---

## Phase 1: Setup

### UHW-1: Project Scaffolding (2025-09-07)
- Created new Android Studio project (`RepeatQuranApp`) in this repo.
- Configured package name (`com.repeatquran`) and min SDK (21).
- Language: Java; UI: XML + Material Components DayNight (not Compose).
- Structure: Single `app` module with package scaffolding for `core`, `playback`, `history`, `settings` (Option C hybrid).
- **Proof**: Hello World screen + Gradle sync screenshot (to be attached under `docs/proof/UHW-1/`).

### UHW-2: Dependency Setup (2025-09-07)
- Added Hilt plugin and dependencies (hilt-android, compiler), Room (runtime, compiler), and ExoPlayer.
- Kept Material Components DayNight theme (Compose deferred per notes).
- Gradle sync verified by CEO (screenshot stored under `docs/proof/UHW-2/gradle-sync.png`).
- **Proof**: Gradle sync success screenshot.

### UHW-3: Repo & ViewModel Stubs (2025-09-07)
- Added Java ViewModels: `SessionViewModel`, `HistoryViewModel`, `PlaybackViewModel` with placeholder methods.
- Added JVM unit tests to instantiate each VM and assert defaults.
- Updated dependencies: lifecycle-viewmodel (runtime) and JUnit (test).
- **Proof**: `docs/proof/UHW-3/unit-test-log.png` (tests passing screenshot).

### UHW-4: ExoPlayer Integration (2025-09-07)
- Implemented `PlaybackManager` with lazy-fetch-next and buffer-ahead=2.
- Added `VerseProvider` and a simple demo provider with placeholder MP3s.
- Wired playback in `MainActivity` for proof capture; added INTERNET permission.
- **Proof**: Demo video and logs stored under `docs/proof/UHW-4/`.

### UHW-5: Playback Service (2025-09-07)
- Added `PlaybackService` as a Foreground Service with `MediaSessionCompat` and `PlayerNotificationManager`.
- Updated `MainActivity` to start the service; added INTERNET and FOREGROUND_SERVICE_MEDIA_PLAYBACK permissions.
- Notification permission requested on Android 13+.
- **Proof**: `docs/proof/UHW-5/background-playback-demo.mp4` and `docs/proof/UHW-5/call-interruption-log.txt`.

### UHW-6: Playback UI Controls (2025-09-07)
- Added in-app controls (Play, Pause, Next, Prev) via Material buttons in XML.
- Wired buttons to `PlaybackService` actions; service maps to ExoPlayer controls.
- Verified DayNight visibility via screenshots on emulator and device.
- **Proof**: `docs/proof/UHW-6/controls-emulator.png`, `docs/proof/UHW-6/controls-device.png` (optional dark mode screenshot).

### UHW-7: Repeat Dropdown (2025-09-07)
- Implemented Material Exposed Dropdown with suggestions {1,3,5,10,∞}.
- Enabled typing custom counts (1–9999) with validation and persistence to `repeat.count`.
- On play/start, `PlaybackService` logs the selected value for proof.
- **Proof**: `docs/proof/UHW-7/repeat-dropdown.png`, `docs/proof/UHW-7/repeat-custom-value.png`, `docs/proof/UHW-7/repeat-log.txt`.

### UHW-8: Repeat Logic (2025-09-07)
- Single-ayah: Exact repeats by building N-duplicate playlist (repeat=OFF) or ∞ via single item (repeat=ONE). Uses current typed repeat value at click.
- Multi-verse (whole passage): Added range loader (Start Surah:Ayah → End Surah:Ayah). Finite N duplicates whole list; ∞ loops list with REPEAT_MODE_ALL.
- Provider feeding disabled for single-ayah/range sessions to avoid extra items; Play resumes existing queue without reseed.
- **Proof**: `docs/proof/UHW-8/repeat-3x-demo.mp4`, `docs/proof/UHW-8/repeat-infinite-demo.mp4`, `docs/proof/UHW-8/range-2x-demo.mp4`, logs under `docs/proof/UHW-8/`.

## Phase 4: Pages (Priority Next)

### UHW-26: Page Dataset (Schema + Seeder) (2025-09-07)
- Added Room table `page_segments` and assets `pages.json` with segments per page.
- Seeder runs on first app launch; instrumented test validates sample pages.
- **Proof**: Android test screenshot; seed log in Logcat.

### UHW-27: Page Selection UI (2025-09-07)
- Added page input (1–604) and “Load Page” button with validation and last-page persistence.
- Playback wiring deferred to UHW-28.
- **Proof**: Screenshot of the UI with valid/invalid input behavior.

### UHW-28: Page Playback Integration (2025-09-13)
- Wired Page UI to `PlaybackService` ACTION_LOAD_PAGE.
- Service builds ayah list from `page_segments`, applies nested reciter cycle, and enqueues finite N cycles or ∞ via REPEAT_MODE_ALL.
- Logs record cycle order and queue sizing for proof.
- Proof: `docs/proof/UHW-28/README.md`.

### UHW-9: Multi-Select Reciters (2025-09-07)
- Added multi-choice dialog to select reciters; prevents duplicates and preserves saved order with numbered rendering.
- Reciter list curated to style variants only (Murattal/Mujawwad/Teacher), no bitrate duplication; entries map to real everyayah folder IDs.
- Alphabetically sorted dialog for better scanning; saved order used for playback.
- **Proof**: `docs/proof/UHW-9/reciters-dialog.png`, `docs/proof/UHW-9/reciters-selected.png`.

### UHW-10: Nested Sequential Playback (2025-09-07)
- Implemented per-cycle playback across selected reciters in saved order for both single ayah and verse ranges.
- Finite N: enqueue N cycles with REPEAT_MODE_OFF; ∞: enqueue one cycle with REPEAT_MODE_ALL.
- Disabled provider feeding for explicit queues; logs record cycle order and queue sizing for proof.
- **Proof**: `docs/proof/UHW-10/nested-cycle-demo.mp4`, `docs/proof/UHW-10/logs.txt`.

## Phase 5: History & Streaks

### UHW-11: Local History Storage (2025-09-13)
- Persist playback sessions in Room: create on load (source, range/page/surah info, reciters order, repeat), mark end with timestamp and cycles.
- Expose retrieval APIs for recent sessions.
- Proof: Covered by unit tests; see `docs/proof/UHW-19/` where history DB insert/retrieve is validated.

## Phase 6: Offline & Downloads

### UHW-14: Progressive Caching (2025-09-08)
- Implemented progressive caching across all playback sources: single verse, verse ranges, pages (via `page_segments`), and full surahs.
- Uses `CacheManager` to save files to internal storage at `files/audio/<reciterId>/<SSS><AAA>.mp3` with atomic `.part` rename on completion.
- Playback prefers cached files (file URI) and otherwise streams while caching in the background (`cacheAsync`). Per‑reciter isolation maintained by folder.
- Verified by playing a page and a surah; files appear in Device File Explorer; replays show no new cache downloads in Logcat (`CacheManager`).
- **Proof**: `docs/proof/UHW-14/cache-screenshot.png`, `docs/proof/UHW-14/cache-log.txt`.

### UHW-15: Downloads Tab (2025-09-08)
- Added a simple Downloads screen (Java + XML) navigable from Home.
- Features: pick reciter; check Surah/Page cache status (✅ or ⬇️ with X/Y count); Download Missing (enqueue `cacheAsync` for missing items); Clear (delete local files and refresh).
- Page status uses `page_segments` via Room DAO to enumerate ayahs; Surah status uses static ayah counts.
- Proof: `docs/proof/UHW-15/downloads-screenshot.png` showing statuses before and after Clear/Download; steps documented in README.

### UHW-16: Offline Playback (With Online Fallback) (2025-09-08)
- Added offline handling: when offline, cached items play; uncached items are skipped gracefully with a clear toast and logs, avoiding playback stalls.
- Player-level per-item handling via `onPlayerError` detects offline and advances to next item; upfront warning if an entire selection has zero cached items while offline.
- Manifest: added `ACCESS_NETWORK_STATE`; utility: `NetworkUtil.isOnline()`.
- Proof: Demo video in airplane mode showing cached clips playing and uncached items skipping with message; log export `docs/proof/UHW-16/offline-log.txt`.

## Phase 7: Resume & Error Handling

### UHW-17: Session Resume (Exact Position) (2025-09-08)
- Added exact resume: periodically persists session context (source, range/page/surah, repeat, reciters, current media index and positionMs).
- Home “Resume” button sends ACTION_RESUME to rebuild the queue and seek to the exact millisecond within the ayah.
- Works for single, range, page, and surah; respects saved reciters and repeat; integrates with offline behavior (skips uncached when offline).
- Proof: `docs/proof/UHW-17/resume-demo.mp4`.

### UHW-18: Error Handling (Retry Once + Actionable) (2025-09-13)
- Implemented single auto-retry for online playback errors. If a verse fails again, a notification presents “Retry” and “Skip” actions handled by the service.
- Offline behavior unchanged (skips uncached with toast per UHW-16). Error notifications cancel automatically when skipping/continuing succeeds.
- Proof: `docs/proof/UHW-18/error-retry-log.txt` with steps and logs of retry/skip.

## Phase 8: QA Harness

### UHW-19: Unit Tests (Core + Edge Cases) (2025-09-13)
- Added Robolectric tests covering: enqueue count for repeat=3 (single ayah), history DB last sessions ordering, CacheManager path and cache hit, and resume state capture via service intents.
- Test deps added: robolectric, room-testing, mockito (future expansion). Run locally via `./gradlew testDebugUnitTest`.
- Proof: `docs/proof/UHW-19/test-report.png` or test log snippet.

### UHW-20: Stress Test Playback (2025-09-13)
- Added a debug‑only QA screen to simulate rapid control spamming, focus loss/gain (simulated), mid‑play reciter switching, and ∞ repeat soak.
- Logs integrated in PlaybackService for state and error paths; QA actions drive service intents to validate stability under stress.
- Proof: `docs/proof/UHW-20/stress-log.txt` and optional short videos.

## Phase 9: Onboarding & Analytics

### UHW-21: Onboarding Screen (Static) (2025-09-13)
- Added a one-time fullscreen onboarding activity with title, brief bullets, and “Get Started”.
- Shows only on first launch; persists `onboarding.seen` and returns to Home.
- Proof: `docs/proof/UHW-21/onboarding-screenshot.png`.

### UHW-22: Minimal Analytics Logs (2025-09-13)
- Added `AnalyticsLogger` to emit structured events to Logcat and rotating files under app internal storage.
- Events: app_open, repeat_set, load_single/range/page/surah, error_playback, error_retry, error_actionable.
- Proof: Logcat screenshot and `analytics-0.log` snippet added under `docs/proof/UHW-22/`.

## Phase 10: Presets & Release

### UHW-23: Preset Management (Save/Edit) (2025-09-13)
- Added Room-backed presets with save from current inputs, list on Home, play, rename/edit repeat, and delete.
- Uses saved reciters and repeat; maps to existing playback actions (single/range/page/surah).
- Proof: `docs/proof/UHW-23/presets-screenshot.png` and play_request logs when replaying.

### UHW-24: APK Packaging (Sideload) (2025-09-13)
- Added release signingConfig scaffold reading from gradle.properties (keystore path and credentials not committed).
- Build via `./gradlew assembleRelease`; artifact at `app/build/outputs/apk/release/app-release.apk`.
- Proof: Folder screenshot and install demo under `docs/proof/UHW-24/`.

## Phase 12: UI Overhaul (Delight + Focus)

### UHW-UI-1: Theme Tokens (Islamic Look) (2025-09-13)
- Added Islamic-inspired palettes (light/dark) and rounded component shapes via Material Components XML theming.
- Primary deep green with gold accents; updated background/surface/outline tokens.
- Proof: `docs/proof/UHW-UI-1/light.png`, `docs/proof/UHW-UI-1/dark.png`.

### UHW-UI-2: Iconography + Header Polish (2025-09-13)
- Added Material top app bar with title, section heading style, and subtle dividers/margins for structure.
- No functional changes.

### UHW-UI-3: Tabs Navigation (Verse | Range | Page | Surah) (2025-09-13)
- Added TabLayout + ViewPager2 skeleton with 4 placeholder fragments.
- Proof: `docs/proof/UHW-UI-3/tabs-demo.mp4`.

### UHW-UI-4: Persist Last Mode (2025-09-13)
- Saves last selected tab (`ui.last.mode`) and auto-opens it when `ui.remember.mode` is enabled (toolbar menu toggle, default on).
- Proof: Relaunch video showing restored tab.

### UHW-UI-5: Verse Tab (Focused Form) (2025-09-13)
- Implemented Verse tab with focused Surah dropdown + Ayah input and Play/Pause/Resume controls.
- Validates inputs; stores last surah; uses saved repeat preference; wires to PlaybackService ACTION_LOAD_SINGLE.

### UHW-UI-6: Range Tab (Focused Form) (2025-09-13)
- Implemented Range tab with Start/End Surah dropdowns and Ayah inputs; Play/Pause/Resume controls.
- Validates inputs; stores last start/end surah; uses saved repeat; wires to ACTION_LOAD_RANGE.

### UHW-UI-7: Page Tab (Focused Form) (2025-09-13)
- Implemented Page tab with page input and Play/Pause/Resume controls.
- Validates page; stores last page; uses saved repeat; wires to ACTION_LOAD_PAGE.

### UHW-UI-8: Surah Tab (Focused Form) (2025-09-13)
- Implemented Surah tab with surah dropdown and Play/Pause/Resume controls.
- Validates surah; stores last surah; uses saved repeat; wires to ACTION_LOAD_SURAH.

### UHW-UI-9: Global Reciters + Repeat UX (2025-09-13)
- Added top pill row with Repeat and Reciters chips; tapping opens repeat dropdown or reciter picker.
- Values persist globally and apply across tabs; chips reflect current selections.

### UHW-UI-10: Half Recitation Toggle (Simple Split) (2025-09-13)
- Added a toggle on Range/Page/Surah tabs to split playback across selected reciters in contiguous segments (2 reciters → halves; >2 cascades by order).
- Extras and pref `ui.half.split` drive service behavior; logs indicate half-split mode and items per cycle.
- UX improvement: Added a Settings screen with a global toggle and a simple visual example (2-reciter and 3-reciter bars) for clearer discoverability. Toolbar menu opens Settings.

### UHW-UI-11: Visual QA + RTL/Arabic (2025-09-13)
- Added Arabic strings for key labels and verified RTL layout with start/end-aware constraints.
- Updated UI to consume string resources and ensured pill row and tabs render correctly in RTL.

### UHW-UI-12: Analytics + Downloads Restyle (2025-09-14)
- Added analytics logs for tab selections (`tab_selected`) and half-split toggles (`half_split_set` per tab).
- DownloadsActivity logs open and page/surah download/clear actions; UI restyled with a Material toolbar and section heading styles.

## Phase 13: Production Readiness

### UHW-PR-01: Pause/Play Resume Fix (2025-09-16)
- Resume-if-needed: if paused with a queue, Resume/Play continues from the same position (no queue rebuilds).
- Broadcast now includes `playing`; `active` reflects actual `isPlaying`.
- Proof: Screen capture demonstrating pause→play continues at the same millisecond.

### UHW-PR-02: Resume Button Logic Fix (2025-09-16)
- ACTION_RESUME now resumes current queue when paused; rebuilds from snapshot only when no active queue.
- UI can reliably gate Resume based on broadcast `playing` state.
- Proof: Logs and demo showing correct gating and behavior from cold start.

### UHW-PR-03: Downloads Back Navigation (2025-09-16)
- Added toolbar up/back in Downloads screen to return to Home.
- Proof: Short clip showing navigation.

### UHW-PR-04: Media Notification Polish
- TODO: Ensure notification actions reflect state; compact actions; cancel error notice on recovery.

### UHW-PR-05: Audio Focus + Interruptions
- TODO: Handle focus, ducking, calls; verify on device.

### UHW-PR-06: Permissions & Policy Readiness
- TODO: Post-notifications prompt 33+; Settings privacy link; Data Safety mapping.

### UHW-PR-07: Crash/Log Collection (Local)
- TODO: Verify log rotation; add Export Logs in Settings.

### UHW-PR-08: Release Hardening
- TODO: Enable shrinker/minify, keep rules, version bump, signed release sanity.

### UHW-PR-09: UI Polish (States)
- TODO: Disabled/empty/loading states; Day/Night visual QA.

### UHW-PR-10: Store Assets
- TODO: Adaptive/round icons; screenshots; feature graphic.

### UHW-PR-11: Smoke Tests
- TODO: Robolectric tests for resume flow, recent sessions filter, downloads back nav.

### UHW-PR-12: StrictMode Sweep
- TODO: Identify and move main-thread I/O; verify clean logs.

### UHW-PR-13: Controls Consolidation (2025-09-16)
- Consolidated controls: kept Play on each tab to validate inputs; added a single toolbar Pause/Resume toggle reflecting playback state; removed per-tab Pause/Resume.
- Proof: Video showing tab Play + global Pause/Resume working.

### UHW-PR-14: Single Source of Truth (State Broadcast) (2025-09-16)
- Broadcasts now fire on play/pause changes and right after enqueue, keeping UI toggles accurate and responsive.

### UHW-PR-15: Service Toggle Action (Simple) (2025-09-16)
- Added ACTION_TOGGLE: if playing → pause(); else → resume (or rebuild from snapshot when needed).

### UHW-PR-16: Controls Row Simplification (One Toggle) (2025-09-16)
- Controls row now uses a single Pause/Resume toggle wired to ACTION_TOGGLE; toolbar toggle mirrors the same.

### UHW-PR-17: Tab Play Consistency (2025-09-16)
- Ensured tab Play buttons only load/start selection (no implicit pause/resume behavior).

### UHW-PR-18: UI Binding and Enablement
- TODO: Bind strictly to broadcast extras; verify initial enable post-enqueue on all devices.

### UHW-PR-20: Service Warmup + Immediate Feedback (2025-09-16)
- Warm-started PlaybackService on app launch to avoid cold-start lag.
- Page/Surah Play now shows immediate feedback and briefly disables the button to prevent double-tap while the queue builds.

### UHW-PR-21: Broadcast After Enqueue (Page/Surah) (2025-09-16)
- Verified and ensured broadcast after enqueue/play so UI toggles flip promptly on Page/Surah loads.
