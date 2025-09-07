# 📑 Repeat Quran — Product Requirements Document (PRD, v1 Draft)

## 🎯 Core Purpose
Repeat Quran is a **memorization-first Qur’an audio app**, designed to help users listen and revise in structured and flexible repetition formats. Unlike generic Qur’an players, its focus is on **repetition, multi-reciter playback, and memorization tracking**.

---

## ✅ Key Decisions

### 1. Primary Goal
- **Hybrid**: Support memorization and revision/listening.  
- Not a general player.

### 2. Repetition Formats
- Block-based (verse/page/surah).  
- Custom ranges.  
- Smart repeat (fixed counts, “until stop”).  
- Manual spaced repetition (intervals chosen by user).

### 3. Multi-Reciter Playback
- Sequential (default).  
- Parallel playback (supported).  
- Alternating postponed to v2.

### 4. Onboarding
- Simple sequential playback as default.  
- Advanced options (parallel, spaced) available but not forced.

### 5. Playback Controls
- Dynamic: play, pause, repeat, change reciter/block mid-session.  
- Not full media-player UI.

### 6. Content Scope
- Pure recitation (no translation/tafsir).  
- UI purely functional for selection.

### 7. Offline Strategy
- Progressive caching → first play downloads automatically, no re-downloads.

### 8. User Interaction
- UI-only (dropdowns, text selection).  
- No voice commands in v1.

### 9. History
- Remembers last session.  
- History list of recently played blocks.

### 10. Audience
- Both students (structured hifz) and adults (revision-on-the-go).

### 11. Platform
- Android only.

### 12. Accounts
- No accounts → all data local.

### 13. Design
- Ultra-minimalist. Flat UI, dropdowns, focus on function.

### 14. Analytics
- Basic anonymous usage (feature usage, session counts).  
- No personal tracking.

### 15. Monetization
- Free at launch.  
- Planned freemium (advanced features in future).

### 16. Launch
- Soft launch via APK to small tester group.

---

## 📏 Success Metrics
- **Adoption**: ≥100 beta testers download APK.  
- **Engagement**: ≥50% of testers return ≥3 times in first week.  
- **Memorization Usage**: ≥70% of sessions use repeat features (not just play once).  
- **Reliability**: <5% playback/caching errors reported.  

---

## ⚠️ Blind Spots
- UI complexity risk: multi-reciter + repetition options may still overwhelm some users.  
- Manual spaced repetition may feel unintuitive without presets.  
- Offline caching may cause storage issues on low-memory devices.  
- No translations may limit appeal beyond memorization-focused users.  
- Soft launch feedback loop depends heavily on tester selection.

---

## 🔍 Growth Reflection
- Start small: simple, focused tool for memorization.  
- Grow by layering **voice input**, **alternating playback**, and **adaptive spaced repetition**.  
- Move from **free → freemium** once advanced features add clear extra value.  
- Staged rollout beyond soft launch once stability and retention are proven.  
