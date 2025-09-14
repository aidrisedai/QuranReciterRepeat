Title: UHW-20 — Stress Test Playback (QA Harness)

Summary
- Adds a debug-only QA screen to simulate stress scenarios: rapid Play/Pause and Next/Prev, simulated audio focus loss/gain (call), mid-play reciter switching, and ∞ repeat soak.

Scope
- UI: `QAActivity` + `activity_qa.xml` (visible only in debug builds from Home).
- Service: `ACTION_SIMULATE_FOCUS_LOSS` and `ACTION_SIMULATE_FOCUS_GAIN` to pause/resume with logs.
- Proof steps documented under `docs/proof/UHW-20/README.md`.

Proof
- Logcat export: `docs/proof/UHW-20/stress-log.txt` (PlaybackService filter).
- Optional short videos for spam and focus.

Regression
- No changes to release build UX; repeat accuracy, reciter order, offline behavior, and error handling preserved.

Branch
- `feature/uhw-20-stress-test`

