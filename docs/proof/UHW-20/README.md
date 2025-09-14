UHW-20 — Stress Test Playback Proof

Run these scenarios and capture logs (Logcat filter: PlaybackService).

Scenarios
- Spam controls: QA → enter Count=20, Delay=150ms → Spam Play/Pause, then Spam Next/Prev.
- Simulate call: QA → Simulate Focus Loss (Call) → Simulate Focus Gain.
- Reciter switch mid‑play: QA → Switch Reciters Mid‑Play (verify order preserved next cycle).
- ∞ soak: QA → Start ∞ Soak (2 min). Observe for stability (no stalls or ANRs).
- Manual: Bluetooth connect/disconnect; toggle airplane mode mid‑queue (cached continues, uncached skip); real/virtual call pause/resume.

Artifacts
- Save log export to `docs/proof/UHW-20/stress-log.txt`.
- Optional short videos for spam and focus.
