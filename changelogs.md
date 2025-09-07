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
