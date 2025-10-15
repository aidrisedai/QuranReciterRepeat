# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

### Basic Build Operations
```bash
./gradlew build                    # Full build
./gradlew assembleDebug           # Debug APK
./gradlew assembleRelease         # Release APK (requires signing config)
./gradlew clean                   # Clean build files
```

### Testing
```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests (requires device/emulator)
./gradlew testDebugUnitTest       # Debug unit tests only
```

### Single Test Execution
```bash
./gradlew test --tests "com.repeatquran.playback.ThreadingStressTest"
./gradlew test --tests "*.PlaybackServiceTest"
```

### Linting and Code Quality
```bash
./gradlew lint                    # Android lint
./gradlew lintDebug              # Debug variant lint only
```

## Project Architecture

### Core Structure
This is an Android Quran recitation app with multi-reciter playback, repetition controls, and session history. The architecture follows a layered approach:

**Playback Layer (`com.repeatquran.playback`)**
- `PlaybackService`: Foreground service managing ExoPlayer and media sessions
- `PlaybackManager`: Core playback logic and queue management  
- `PlaybackStateManager`: Thread-safe state management singleton
- `ThreadSafePlaybackState`: Synchronized state container
- `generator/`: Media item generation for different playback modes (single verse, range, page, surah)

**Data Layer (`com.repeatquran.data`)**
- `RepeatQuranDatabase`: Room database with SessionEntity, PageSegmentEntity, PresetEntity
- `SessionRepository`: Session CRUD operations and history management
- `PresetRepository`: User-saved playback configurations
- `CacheManager`: Audio file caching system
- `PageSeeder`: Populates page segment data on first app run

**UI Layer (`com.repeatquran.ui`)**
- `MainActivity`: Tabbed interface with global controls (speed, repeat, reciters)
- Tab Fragments: `VerseTabFragment`, `RangeTabFragment`, `PageTabFragment`, `SurahTabFragment`
- `ModesPagerAdapter`: ViewPager2 adapter for tab navigation
- `SpeedControlHelper`: Shared speed control logic

### Key Components

**Multi-Reciter System**: Nested sequential playback where each verse cycles through selected reciters before moving to next verse. Reciter selection stored in SharedPreferences as CSV string.

**Repeat Logic**: Configurable repeat counts (1, 3, 5, 10, ∞) applied at verse level within the reciter cycle.

**Page Playback**: Uses `page_segments` database table to map Madani Quran pages to verse ranges. Each page contains multiple segments played sequentially.

**Session Tracking**: All playback sessions logged to Room database with source type, verse ranges, reciters, and timestamps for history display.

**Foreground Service**: `PlaybackService` maintains foreground status during active playback with media notification and MediaSession for system integration.

### Dependencies
- **Hilt**: Dependency injection framework
- **Room**: Local database (sessions, presets, page segments)
- **ExoPlayer**: Media playback engine
- **Material Design Components**: UI framework
- **Robolectric**: Unit testing framework

### Testing Strategy
- Unit tests in `app/src/test/` with Robolectric for Android framework mocking
- Integration tests in `app/src/androidTest/` for database and service interactions
- Performance tests for playback queue management and threading
- Stress tests for rapid play/pause operations

### State Management
The app uses a hybrid approach:
- `PlaybackStateManager` singleton for global playback state
- SharedPreferences for user settings (speed, repeat count, reciter selection)
- Room database for persistent data (sessions, presets, page segments)
- Local fragment state for UI-specific data

### Audio Streaming
Audio files streamed from external servers with local caching via `CacheManager`. ExoPlayer handles buffering and network resilience. 2-verse buffer maintained for smooth playback.

## Development Notes

### Signing Configuration
Release builds require signing properties in `gradle.properties`:
- `RQ_STORE_FILE`: Path to keystore
- `RQ_STORE_PASSWORD`: Keystore password  
- `RQ_KEY_ALIAS`: Key alias
- `RQ_KEY_PASSWORD`: Key password

### Target SDK
- Compile SDK: 35
- Target SDK: 35
- Min SDK: 21

### ProGuard
Release builds use ProGuard with custom rules in `proguard-rules.pro`. Test obfuscation before release to ensure reflection-dependent code still works.