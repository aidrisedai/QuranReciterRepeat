# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

**Repeat Quran** is a memorization-focused Quran audio app built for Android. It helps users listen to Quranic recitations with structured repetition, multi-reciter playback, and memorization tracking. The app emphasizes repetition-based learning with features like verse-level repeat counts, page/surah playback, and progress tracking.

## Key Architecture

This is a modern Android app following **MVVM pattern with Hilt DI**:

- **UI Layer**: Java + XML layouts (not Compose) with Material Design components
- **Data Layer**: Room database for local storage, repositories for data management
- **Playback Layer**: ExoPlayer for audio streaming with foreground service for background playback
- **Storage**: Progressive caching to internal storage, offline-first approach
- **No Backend**: Entirely local app that fetches audio from public Quran APIs

### Core Components

- `PlaybackService`: Foreground service handling audio playback, media session, and queue management
- `MainActivity`: Tab-based UI (Verse/Range/Page/Surah) with unified controls
- `SessionRepository`: Manages playback history and streak calculations
- `CacheManager`: Handles progressive downloading and offline storage
- ViewModels: `SessionViewModel`, `PlaybackViewModel`, `HistoryViewModel`

## Common Development Commands

### Building and Testing
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

### Development Workflow
```bash
# Install debug build
adb install app/build/outputs/apk/debug/app-debug.apk

# View logs during development
adb logcat | grep "RepeatQuran\|PlaybackService\|CacheManager"

# Check analytics logs
adb shell run-as com.repeatquran cat files/logs/analytics-0.log
```

### Testing Specific Features
```bash
# Test offline functionality
adb shell svc wifi disable
adb shell svc data disable

# Re-enable connectivity
adb shell svc wifi enable
adb shell svc data enable

# Clear app data for fresh testing
adb shell pm clear com.repeatquran
```

## Architecture Patterns

### Playbook for Common Tasks

**Adding New Audio Sources:**
1. Extend `VerseProvider` interface
2. Update `PlaybackService` action handling
3. Add UI controls in appropriate tab fragment
4. Update `CacheManager` for offline support

**Modifying Playback Logic:**
- Core queue management happens in `PlaybackService.buildQueue()`
- Repeat logic is handled via ExoPlayer concatenating playlists
- Multi-reciter sequencing uses nested loops (reciter cycles within repeat cycles)

**Adding New UI Tabs:**
1. Create fragment extending base pattern from existing tabs
2. Update `ModesPagerAdapter` 
3. Add tab to `TabLayoutMediator` setup in `MainActivity`
4. Wire service actions for the new input type

**Database Changes:**
- Room entities are in `data/db/` package
- Migrations handled in database class
- Always update entity version and provide migration path

### Key Technical Decisions

- **Verse-level Granularity**: Audio files cached per verse for precise control
- **Progressive Caching**: Downloads happen on first play, no manual download required
- **Nested Sequential Multi-Reciter**: Reciter A → Reciter B → repeat entire cycle
- **Local Analytics Only**: All tracking stays on device in rotating log files
- **No User Accounts**: Everything stored locally using SharedPreferences and Room

## Development Notes

### Important Classes to Understand
- `PlaybackService`: Heart of the app - manages ExoPlayer, queuing, and media session
- `MainActivity`: Contains all tab UI and global controls
- `CacheManager`: Handles progressive downloading and file management
- `SessionRepository`: Manages history and streak calculations
- `AyahCounts`: Shared utility for Quran structure data

### Testing Strategy
The app uses a hybrid testing approach:
- Unit tests for core logic (repositories, utilities)
- Robolectric tests for Android components
- Manual QA documented in `/qa` activities
- Demo videos and screenshots required for feature verification

### Error Handling Patterns
- **Auto-retry Once**: Failed downloads retry automatically once
- **Graceful Degradation**: Offline mode skips unavailable content with user feedback
- **User-Actionable**: Persistent failures show "Retry/Skip" notification actions

### Performance Considerations
- Large range selections (like full Surahs) build queues on background thread
- Downloads and file I/O operations moved off main thread
- ExoPlayer buffering configured for verse-level precision
- Progressive caching prevents re-downloads

### UI/UX Patterns
- **Tab-based Navigation**: Verse | Range | Page | Surah
- **Global Controls**: Repeat count and reciter selection persist across tabs
- **Inline Validation**: Real-time feedback on input fields
- **State Broadcasting**: Service broadcasts playback state to keep UI in sync

### Cultural/Domain Considerations
- **RTL Support**: Layout supports Arabic text and right-to-left reading
- **Islamic Design**: Uses green/gold color scheme appropriate for Quranic content
- **Memorization Focus**: Features optimized for spaced repetition and review
- **Respectful Error Handling**: Graceful handling of recitation interruptions

## Project Structure Highlights

```
app/src/main/java/com/repeatquran/
├── MainActivity.java              # Main tabbed interface
├── playback/
│   ├── PlaybackService.java      # Core audio service
│   ├── PlaybackManager.java      # ExoPlayer wrapper
│   └── VerseProvider.java        # Audio source abstraction
├── data/
│   ├── SessionRepository.java    # History/streaks management
│   ├── CacheManager.java         # File caching system
│   └── db/                       # Room entities
├── ui/
│   ├── *TabFragment.java         # Individual tab implementations
│   └── ModesPagerAdapter.java    # Tab navigation
└── util/
    ├── AyahCounts.java          # Quran structure data
    └── SurahNames.java          # Localized Surah names
```

Key documentation files:
- `prd.md`: Product requirements and feature decisions
- `eng.md`: Technical architecture documentation  
- `tasks.md`: Detailed development task breakdown
- `notes.md`: Project notes and issue tracking

<citations>
<document>
<document_type>WARP_DOCUMENTATION</document_type>
<document_id>getting-started/quickstart-guide/coding-in-warp</document_id>
</document>
</citations>