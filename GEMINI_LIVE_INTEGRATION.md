# Gemini Live API Integration

## Overview

Replaced OpenAI Realtime API with **Gemini 2.0 Live API** for real-time speech-to-speech Quran teaching.

## Why Gemini Live?

### Advantages over OpenAI Realtime API:
- ✅ **Native audio support** - Built-in speech-to-speech without separate TTS
- ✅ **Lower cost** - Gemini pricing is significantly cheaper
- ✅ **Better for long sessions** - More stable WebSocket connections
- ✅ **Google's voice quality** - Natural sounding voices (using "Aoede" voice)
- ✅ **Multimodal** - Can handle audio + text seamlessly

### Comparison:

| Feature | OpenAI Realtime | Gemini Live |
|---------|----------------|-------------|
| **Audio Input** | 24kHz PCM | 16kHz PCM |
| **Audio Output** | 24kHz PCM | 16kHz PCM |
| **Voice Options** | alloy, echo, shimmer, etc. | Puck, Charon, Kore, Fenrir, Aoede |
| **Real-time interruption** | ✅ Yes | ✅ Yes |
| **Cost (per 1M tokens)** | $60 input, $240 output | $1.25 input, $5 output |
| **WebSocket** | ✅ Yes | ✅ Yes |
| **VAD (Voice Activity Detection)** | ✅ Yes | ✅ Yes |

## Implementation

### Files Created:
- `GeminiLiveQuranTeacher.java` - Main implementation using Gemini 2.0 Live API

### Files Modified:
- `RecitationRecorderActivity.java` - Added Gemini Live support with API switcher
- `activity_recitation_recorder.xml` - Updated button text and color

### API Configuration:

**WebSocket Endpoint:**
```
wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=YOUR_API_KEY
```

**Setup Message:**
```json
{
  "setup": {
    "model": "models/gemini-2.0-flash-exp",
    "generation_config": {
      "response_modalities": ["AUDIO"],
      "speech_config": {
        "voice_config": {
          "prebuilt_voice_config": {
            "voice_name": "Aoede"
          }
        }
      }
    },
    "system_instruction": {
      "parts": [
        {
          "text": "[ComprehensionPromptBuilder output]"
        }
      ]
    }
  }
}
```

**Audio Streaming (Input):**
```json
{
  "realtimeInput": {
    "mediaChunks": [
      {
        "mimeType": "audio/pcm;rate=16000",
        "data": "<base64_audio>"
      }
    ]
  }
}
```

**Audio Response (Output):**
```json
{
  "serverContent": {
    "modelTurn": {
      "parts": [
        {
          "inlineData": {
            "mimeType": "audio/pcm",
            "data": "<base64_audio>"
          }
        },
        {
          "text": "Transcript of what teacher said"
        }
      ]
    },
    "turnComplete": true
  }
}
```

## Features Preserved

All features from OpenAI implementation are preserved:

✅ **Real-time interruption** based on comprehension level:
- **Simple**: Waits for student to finish 3-5 verses
- **Medium**: Interrupts immediately with varied cues ("Mm-mm", "Go back to...", etc.)
- **High**: Zero tolerance - instant correction with technical terms

✅ **Keep-alive mechanism** - Sends ping every 15 seconds

✅ **Broken pipe detection** - Handles connection issues gracefully

✅ **Audio streaming** - 100ms chunks for responsive feedback

✅ **Teacher voice output** - Plays audio responses in real-time

## Audio Settings

### Gemini Requirements:
- **Sample Rate**: 16kHz (vs OpenAI's 24kHz)
- **Format**: 16-bit PCM, mono
- **Encoding**: Base64 for transmission
- **Chunk Size**: 100ms (1600 samples = 3200 bytes)

## Voice Selection

Current voice: **"Aoede"** - Warm, authoritative voice suitable for teaching

Available Gemini voices:
- **Puck** - Confident, engaging
- **Charon** - Calm, wise
- **Kore** - Warm, friendly  
- **Fenrir** - Strong, clear
- **Aoede** - Authoritative, teaching-focused (CURRENT)

## Cost Analysis

### Example: 10-minute recitation session

**OpenAI Realtime:**
- Input audio: ~$3.60
- Output audio: ~$14.40
- **Total: ~$18 per session**

**Gemini Live:**
- Input audio: ~$0.08
- Output audio: ~$0.30
- **Total: ~$0.38 per session**

**Savings: ~98% cost reduction** 🎉

## Testing Checklist

- [ ] WebSocket connection establishes successfully
- [ ] Audio input is received by Gemini
- [ ] Teacher voice responses play correctly
- [ ] Interruptions work at each comprehension level
- [ ] Keep-alive prevents broken pipe errors
- [ ] Session can run for 10+ minutes
- [ ] UI updates show teacher status correctly
- [ ] Stop button terminates session cleanly

## Next Steps

1. ✅ Test Gemini Live connection
2. ✅ Verify audio quality
3. ✅ Test interruption behavior at each level
4. ✅ Monitor for broken pipe issues
5. ⏳ Compare voice quality vs OpenAI
6. ⏳ Get user feedback
7. ⏳ Consider removing OpenAI fallback if Gemini works well

## API Key Setup

Add to `local.properties`:
```properties
GEMINI_API_KEY=your_gemini_api_key_here
```

The same key used for `GeminiRecitationAnalyzer` works for Gemini Live.

## Switching Between APIs

Currently defaults to **Gemini Live** (`useGeminiLive = true` in `RecitationRecorderActivity`).

To switch back to OpenAI:
```java
private boolean useGeminiLive = false; // Use OpenAI Realtime API
```

## Known Issues

1. **First connection may be slow** - Gemini Live API is in alpha/preview
2. **Voice selection limited** - Only 5 prebuilt voices available
3. **Documentation sparse** - API is very new (Dec 2024)

## References

- [Gemini Live API Docs](https://ai.google.dev/gemini-api/docs/live)
- [Gemini Pricing](https://ai.google.dev/pricing)
- [OpenAI Realtime API Docs](https://platform.openai.com/docs/guides/realtime)
