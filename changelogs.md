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
