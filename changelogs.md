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
