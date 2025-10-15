# Range Playback Logging Guide (Deprecated)

Note: The Range backend has been removed temporarily while we rebuild it. The details below refer to the previous implementation (RangeGenerator/RollingFeeder) and are no longer applicable at runtime. For the new effort, see scaffolding under `app/src/main/java/com/repeatquran/playback/range/` (RangePlanner, RangeEngine), which we will flesh out collaboratively.

## Overview

The range playback system has multiple code paths for generating verse sequences. To help debug any sequencing issues, detailed logging has been added to all these paths.

## Logging Locations and When They Appear

### 1. `buildVersesForRange()` Function
**File**: `PlaybackService.java` lines 688-736
**When it runs**: Only when resuming half-split range playback
**Log prefix**: `buildVersesForRange:`

**Sample logs**:
```
D/PlaybackService: buildVersesForRange: Building range 2:255 to 3:5
D/PlaybackService: buildVersesForRange: Processing surah 2, ayahs 255 to 286 (total: 286)
D/PlaybackService: buildVersesForRange: Added verse 1: 002:255
D/PlaybackService: buildVersesForRange: Added verse 2: 002:256
...
D/PlaybackService: buildVersesForRange: Completed surah 2 with 32 ayahs
D/PlaybackService: buildVersesForRange: Processing surah 3, ayahs 1 to 5 (total: 200)
D/PlaybackService: buildVersesForRange: Added verse 33: 003:001
...
D/PlaybackService: buildVersesForRange: Complete! Generated 37 verses total
D/PlaybackService: buildVersesForRange: First 10 verses in sequence:
D/PlaybackService: buildVersesForRange: [0] 002:255
D/PlaybackService: buildVersesForRange: [1] 002:256
...
```

### 2. `handleLoadRange()` - Test Mode Half-Split
**File**: `PlaybackService.java` lines 1915-1932
**When it runs**: During range playback in test/debug mode with half-split enabled
**Log prefix**: `handleLoadRange (TEST):`

### 3. `handleLoadRange()` - Production Mode Half-Split  
**File**: `PlaybackService.java` lines 1968-1985
**When it runs**: During range playback in production mode with half-split enabled
**Log prefix**: `handleLoadRange (PROD):`

### 4. `RangeGenerator.buildVerseList()` 
**File**: `RangeGenerator.java` lines 107-122
**When it runs**: When RangeGenerator is instantiated for regular (non-half-split) range playback
**Log prefix**: `RangeGenerator:`

**Sample logs**:
```
D/RangeGenerator: buildVerseList: range 2:255 to 3:5
D/RangeGenerator: Processing surah 2, ayahs 255 to 286
D/RangeGenerator: Added verse 1: 2:255
D/RangeGenerator: Added verse 2: 2:256
...
D/RangeGenerator: buildVerseList complete: 37 verses total
```

### 5. `RangeGenerator.next()` 
**File**: `RangeGenerator.java` lines 76-78
**When it runs**: Each time the RollingFeeder requests more media items
**Log prefix**: `RangeGenerator:`

**Sample logs**:
```
D/RangeGenerator: Sequence debug: cursor=0, unitIndex=0, verseIndex=0, reciterIndex=0, verse=002:255, reciter=test_reciter1
D/RangeGenerator: Sequence debug: cursor=1, unitIndex=1, verseIndex=1, reciterIndex=0, verse=002:256, reciter=test_reciter1
...
D/RangeGenerator: Sequence debug: cursor=37, unitIndex=0, verseIndex=0, reciterIndex=1, verse=002:255, reciter=test_reciter2
```

### 6. `buildRangeCycle()` 
**File**: `PlaybackService.java` lines 595-596
**When it runs**: When building pre-computed range cycles (test mode non-half-split, or resume operations)
**Log prefix**: `buildRangeCycle debug:`

## Code Path Summary

The range playback system uses different approaches depending on the mode:

1. **Production + Regular Mode**: Uses `RangeGenerator` with `RollingFeeder` (lazy loading)
2. **Production + Half-Split Mode**: Uses inline verse building + `buildHalfSplit`
3. **Test + Regular Mode**: Uses `buildRangeCycle` (pre-computed)
4. **Test + Half-Split Mode**: Uses inline verse building + `buildHalfSplit`
5. **Resume + Half-Split Mode**: Uses `buildVersesForRange` + `buildHalfSplit`

## How to Use This Logging

When debugging range sequencing issues:

1. **Enable verbose logging** by filtering for `PlaybackService` and `RangeGenerator` tags
2. **Look for the appropriate log prefix** based on your scenario
3. **Verify verse sequence** by checking the logged verse numbers are in correct order
4. **Check cross-surah transitions** - ensure verses go from surah X:last_ayah to surah (X+1):001
5. **Verify reciter rotation** - ensure reciters cycle in the expected order

## Example Complete Log Flow

For a range 2:285 to 3:2 with 2 reciters in production mode:

```
D/PlaybackService: NEW: Load Range: 002 — Al-Baqarah:285 → 003 — Aal Imran:2, Repeat=1, HalfSplit=false
D/RangeGenerator: buildVerseList: range 2:285 to 3:2
D/RangeGenerator: Processing surah 2, ayahs 285 to 286
D/RangeGenerator: Added verse 1: 2:285
D/RangeGenerator: Added verse 2: 2:286
D/RangeGenerator: Processing surah 3, ayahs 1 to 2
D/RangeGenerator: Added verse 3: 3:1
D/RangeGenerator: Added verse 4: 3:2
D/RangeGenerator: buildVerseList complete: 4 verses total
D/RangeGenerator: Sequence debug: cursor=0, unitIndex=0, verseIndex=0, reciterIndex=0, verse=002:285, reciter=reciter1
D/RangeGenerator: Sequence debug: cursor=1, unitIndex=1, verseIndex=1, reciterIndex=0, verse=002:286, reciter=reciter1
D/RangeGenerator: Sequence debug: cursor=2, unitIndex=2, verseIndex=2, reciterIndex=0, verse=003:001, reciter=reciter1
D/RangeGenerator: Sequence debug: cursor=3, unitIndex=3, verseIndex=3, reciterIndex=0, verse=003:002, reciter=reciter1
D/RangeGenerator: Sequence debug: cursor=4, unitIndex=0, verseIndex=0, reciterIndex=1, verse=002:285, reciter=reciter2
...
```

This shows the correct sequence: all verses for reciter1 first, then all verses for reciter2, with proper cross-surah transition from 002:286 to 003:001.
