🧪 QA Strategy & Artifact — Repeat Quran MVP
✅ Test Coverage Checklist

Playback & Repeat

 90%+ playback accuracy across all test cases.

 Actionable error messages for all failures (network, storage, corrupted file).

 Full multi-reciter playback sync (all repeat modes).

 Complete repeat coverage (fixed, ∞, custom).

 Seamless verse-to-verse playback (no gaps).

History & Streaks

 Resume last session (basic check only).

 Streak count accurate, increments daily, resets on missed days.

 History list shows last sessions; replay works.

 Preset save/edit: tested, but non-blocking.

Offline & Downloads

 Cached Surahs must always replay offline.

 Downloads tab accurately reflects cached files (✅ vs ⬇️).

Controls & UI

 Playback controls must pass stress tests (pause, next, reciter/repeat switch mid-play).

 Dropdowns: search, multi-select (no duplicates), selections must persist into playback.

 Light mode visuals render consistently across all screens.

Background & Resume

 Background playback works across app switch, calls, Bluetooth handoff.

 Session continuity: resume mid-verse on reopen.

Analytics & Onboarding

 Local logs exist and are accurate (play/pause/repeat).

 Onboarding screen appears (presence only).

🚨 Critical Fail Conditions

Playback accuracy <90%.

Any mismatch between UI selections (Surah, verse, reciter, repeat) and actual playback.

Missing actionable error messages on failure.

Multi-reciter playback out of sync.

Repeat modes not honoring counts (fixed, ∞, custom).

Streak banner count incorrect or failing to reset.

Cached Surah not playing offline.

Playback controls or dropdown selections not persisting correctly.

Background playback fails during app switch or Bluetooth handoff.

Session does not resume mid-verse after exit/reopen.

Audio gaps or clipping between consecutive verses.

Light mode rendering breaks core screens (Home, Playback, History, Downloads).

Analytics logs missing for core playback actions.

📊 Test Coverage Ratio

Covered by QA strategy: ~80–85% of v1 features.

Uncovered (logged, not blocking):

Storage growth / cache eviction.

Preset editing robustness.

Dark mode support.

Audio fidelity across devices.

Advanced spaced repetition UX clarity.

Estimated Coverage: 82% (above 70% threshold).

⚠️ Blind Spots

Storage bloat on low-memory devices (no eviction policy in v1).

Analytics minimalism: may lack insights into what drives retention.

Dropdown fatigue: correctness tested, but UX flow still may feel heavy.

Streak encouragement tone: not tested for cultural sensitivity.

Preset editing: only partially tested.

🌱 Growth Reflection

This QA strategy prioritizes memorization trust:

Playback accuracy, repeat integrity, streak reliability, and session continuity are non-negotiable.

You chose strict thresholds (90%+ accuracy, session continuity, multi-reciter full sync, actionable errors). That makes v1 slower to release but preserves credibility with memorization-focused users.

By logging (not blocking) storage, presets, and dark mode, you keep QA lean while documenting clear next-version improvements.

In short: QA ensures v1 won’t betray user trust in memorization flow — even under stress tests. Growth depends on layering analytics, storage management, and UX refinements in future versions.