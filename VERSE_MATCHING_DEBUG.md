# Verse Matching Debug - New Build

## What Changed

### 1. Reduced Matching Thresholds (for testing)
- **Word matching**: 70% → 50% (more lenient)
- **Similarity threshold**: 75% → 60% (more lenient)
- This will make it easier to advance verses while we debug

### 2. Added Debug Logging
- Shows transcript vs expected verse in logs
- Shows word match percentage
- Shows similarity score
- Logs when verse is NOT complete

### 3. Visual Feedback
- Now shows **expected verse text** on screen
- Shows your transcription in real-time
- Format:
  ```
  📖 Verse 1
  
  Expected:
  بِسْمِ اللَّهِ الرَّحْمَٰنِ...
  
  🎤 Hearing:
  بسم الله الرحمن الرحيم...
  ```

## Install New Build

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Testing Steps

### 1. Start a Session
- Open app
- Go to your Surah 39 goal
- Click "Start Session"
- Wait for "🎤 Listening..."

### 2. Observe the Expected Verse
You should now see the **expected Arabic text** on screen. This tells you:
- ✅ If the verse loaded from API successfully
- ✅ What the app is comparing your recitation against

### 3. Recite the Verse
- Speak clearly in Arabic
- Watch the "🎤 Hearing:" section update in real-time
- **Compare** what you're saying vs what's expected

### 4. Check if Verse Advances
After completing the verse:
- Should show similarity score (e.g., "✓ Verse 1: 85% match")
- Should automatically advance after 2 seconds
- New expected verse should load

## What to Look For

### ✅ Good Signs:
1. Expected verse text appears (Arabic text)
2. Your speech appears in "Hearing:" section
3. Verse advances after completion
4. Progress bar updates

### ⚠️ Problem Signs:

**"Expected: Loading..."**
→ Verse text not loaded from API
→ Check internet connection

**"Expected: [empty or weird characters]"**
→ API returned bad data
→ Check which verse you're on (might be invalid)

**Speech recognized but verse doesn't advance**
→ Matching algorithm not detecting completion
→ Check logs (see below)

**No speech appearing**
→ Speech recognition not working
→ Check microphone permission

## Viewing Logs

```bash
# Clear logs first
adb logcat -c

# Recite a verse

# View matching logs
adb logcat -d | grep -E "VerseMatchingEngine|MemorizationSession"
```

### What Logs Show:

**Successful verse load:**
```
QuranVerseProvider: Successfully fetched verse 39:1: تَنزِيلُ الْكِتَابِ...
MemorizationSession: Loaded verse 39:1 - تَنزِيلُ...
```

**Speech recognition:**
```
MemorizationSession: Final result: تنزيل الكتاب من الله
```

**Verse matching:**
```
MemorizationSession: === Verse Matching Debug ===
MemorizationSession: Transcript so far: تنزيل الكتاب من الله
MemorizationSession: Expected verse: تَنزِيلُ الْكِتَابِ مِنَ اللَّهِ...
VerseMatchingEngine: Word match: 4/8 = 50.0%
VerseMatchingEngine: Similarity: 85%
MemorizationSession: Is verse complete: true
MemorizationSession: Verse match similarity: 85.0%
```

**Verse NOT complete:**
```
MemorizationSession: Is verse complete: false
MemorizationSession: Verse not complete yet, continuing to listen...
```

## Troubleshooting

### Issue: Expected verse shows but never advances

**Check logs for:**
```
MemorizationSession: Is verse complete: false
```

**Possible causes:**
1. **Not enough words matched** - Speech recognition might be missing words
2. **Different pronunciation** - STT might transcribe differently than expected
3. **Verse is very long** - Need to recite more of it

**Solution:** Look at the logs to see word match percentage. If it's below 50%, you need to recite more clearly or more of the verse.

### Issue: Expected verse is NULL

**Check logs for:**
```
MemorizationSession: ERROR: Expected verse text is NULL!
```

**Solution:** Stop session and restart. The API call failed.

### Issue: Speech not being recognized

**Check:**
- Microphone permission granted?
- Speaking in Arabic?
- Speaking clearly and loud enough?
- Background noise interfering?

## Example Success Flow

```
1. Session starts
2. UI shows: "📖 Verse 1\n\nExpected:\nتَنزِيلُ الْكِتَابِ مِنَ اللَّهِ..."
3. You recite: "تنزيل الكتاب من الله العزيز الحكيم"
4. UI updates: "🎤 Hearing:\nتنزيل الكتاب من الله العزيز الحكيم"
5. Logs show: "Word match: 6/8 = 75%"
6. Logs show: "Similarity: 82%"
7. Logs show: "Is verse complete: true"
8. UI shows: "✓ Verse 1: 82% match - Excellent!"
9. After 2 seconds, advances to Verse 2
10. Progress bar updates: 1/X
```

## Adjusting Thresholds

If verses are **advancing too easily** (false positives):
- Increase thresholds in `VerseMatchingEngine.java`
- Word match: 0.5 → 0.7
- Similarity: 0.60 → 0.75

If verses are **never advancing** (too strict):
- Decrease thresholds
- Word match: 0.5 → 0.3
- Similarity: 0.60 → 0.50

Current settings are intentionally lenient for testing!

## Next Steps

Once you test this build:

1. **If it works**: Great! We can fine-tune thresholds
2. **If expected verse shows but doesn't advance**: Share logs so I can see matching details
3. **If expected verse doesn't show**: API loading issue, check internet
4. **If speech not recognized**: Device STT issue

Let me know what you see! 🔍
