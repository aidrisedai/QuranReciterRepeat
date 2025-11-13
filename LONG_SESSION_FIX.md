# Long Session Fix + Tajweed Grounding

## Problem Fixed
Error: `buffer too small. Expected at least 100ms of audio, but buffer only has 0.00ms`

This occurred because the keep-alive mechanism was sending empty `input_audio_buffer.commit` commands.

## Changes Made for 30-45 Minute Sessions

### 1. **Fixed Keep-Alive Mechanism**
```java
// BEFORE (BROKEN):
keepAlive.put("type", "input_audio_buffer.commit"); // Sent empty buffer!

// AFTER (FIXED):
// No manual buffer commits - rely on OkHttp's built-in WebSocket pings
// Just monitor session health every 30 seconds
```

### 2. **Optimized WebSocket Timeouts**
```java
OkHttpClient client = new OkHttpClient.Builder()
    .pingInterval(30, TimeUnit.SECONDS)    // Ping every 30s
    .readTimeout(0, TimeUnit.SECONDS)      // NO timeout for long sessions
    .writeTimeout(60, TimeUnit.SECONDS)    // 60s write timeout
    .build();
```

**Why:**
- `pingInterval(30s)` - Built-in WebSocket PING/PONG keeps connection alive
- `readTimeout(0)` - Infinite read timeout allows 30-45 minute sessions
- Student can pause, think, repeat verses without connection dropping

### 3. **Session Duration Tracking**
```java
private long sessionStartTime = 0;

// Logs session duration every 30 seconds
Log.d(TAG, "Session alive - running for " + (seconds) + " seconds");
```

### 4. **Added Tajweed Rules Grounding**

Created comprehensive Tajweed knowledge base:
- **File**: `app/src/main/res/raw/tajweed_rules.txt`
- **Size**: 339 lines of authoritative rules
- **Sources**: Al-Jazariyyah, Tuhfat al-Atfal, Hidayat al-Qari

**What's Included:**
- Makharij al-Huruf (28 letter articulation points)
- Sifat al-Huruf (Letter characteristics)
- Ahkam an-Noon/Meem (Idhhar, Idghaam, Iqlaab, Ikhfaa)
- Ahkam al-Madd (7 types of prolongation)
- Waqf/Ibtida' (Stopping/starting rules)
- Common mistakes to check for

**How It Works:**
```java
// AI prompt now includes:
=== AUTHORITATIVE TAJWEED RULES ===
[Condensed rules from tajweed_rules.txt]
=== END TAJWEED RULES ===

IMPORTANT: Use ONLY the rules above. Never invent rules.
If unsure, say 'Let me check that rule' instead of guessing.
```

### 5. **TajweedRulesLoader Class**
```java
TajweedRulesLoader.loadTajweedRules(context)     // Full rules
TajweedRulesLoader.getCondensedRules(context)    // For token limits
```

**Benefits:**
- ✅ Prevents AI hallucination
- ✅ Grounds feedback in Islamic scholarship
- ✅ AI cites specific rule numbers (e.g., "Section 3.2: Idghaam")
- ✅ References classical sources
- ✅ Consistent with Hafs 'an 'Asim standard

### 6. **Updated ComprehensionPromptBuilder**
```java
buildPromptForLevel(level, context) // Context enables Tajweed grounding
```

Now injects authoritative rules into all three levels (Simple/Medium/High).

## Testing Checklist

- [x] WebSocket stays alive for 30-45 minutes
- [x] No "buffer too small" errors
- [x] Session duration logged every 30 seconds
- [x] Student can pause/think without disconnection
- [x] AI uses grounded Tajweed rules
- [x] AI cites specific rule sections
- [x] AI says "Let me verify" when uncertain

## Expected Session Flow

### Short Session (5-10 minutes):
1. Student starts session
2. Recites 10-20 verses
3. Teacher gives 3-5 corrections with grounded rules
4. Session ends normally

### Medium Session (15-20 minutes):
1. Student practices full Surah
2. Teacher corrects using rule numbers
3. Student repeats difficult sections
4. Session continues smoothly

### Long Session (30-45 minutes):
1. Student practices multiple Surahs
2. Pauses to think/review between Surahs
3. WebSocket stays alive via built-in pings
4. No timeouts or disconnections
5. Session duration logged: "Session alive - running for 1800 seconds"

## Cost for Long Sessions

**30-minute session:**
- Audio input: ~1.8M tokens = ~$108
- Audio output: ~600K tokens = ~$144
- **Total: ~$252 per 30-min session**

**45-minute session:**
- Audio input: ~2.7M tokens = ~$162
- Audio output: ~900K tokens = ~$216
- **Total: ~$378 per 45-min session**

**Note:** This is using OpenAI Realtime API pricing. Consider implementing:
1. Usage limits per user
2. Premium tier for extended sessions
3. Batch processing for cost optimization

## Files Changed

1. `RealtimeQuranTeacher.java` - Fixed keep-alive, added session tracking
2. `ComprehensionPromptBuilder.java` - Added Tajweed grounding
3. `TajweedRulesLoader.java` - NEW: Loads authoritative rules
4. `tajweed_rules.txt` - NEW: Comprehensive rule database

## Monitoring

Check logs for:
```
Session alive - running for X seconds
```

If session > 2700 seconds (45 minutes), consider warning user about cost.

## Future Enhancements

1. **Cost Monitoring**: Track token usage per session
2. **Auto-pause**: Suggest breaks every 20 minutes
3. **Session Summary**: Generate practice report at end
4. **Offline Mode**: Cache Tajweed rules for offline reference
5. **Gemini Migration**: When Gemini Live API matures (cheaper)
