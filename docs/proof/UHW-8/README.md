Proof Artifacts — UHW-8: Repeat Logic (Single Ayah + Whole Passage)

Please attach the following artifacts here:

- repeat-3x-demo.mp4 — Single ayah plays exactly 3 times, then pauses at start
- repeat-infinite-demo.mp4 — Single ayah loops indefinitely until Pause/Next
- range-2x-demo.mp4 — A Start→End verse range plays twice as a whole passage
- logs.txt — Logcat export filtered by tags: PlaybackService|PlaybackManager

Test checklist
1) Single ayah finite (e.g., 1:1, repeat=3): Use Load Ayah; verify 3 exact plays, then seek(0) and pause.
2) Single ayah ∞: Use Load Ayah; verify it loops until Pause/Next.
3) Range finite (e.g., 1:4 → 1:6, repeat=2): Use Load Range; verify complete list repeats twice, then stops.
4) Confirm no unrelated items appended (no "Buffer appended" during single-ayah/range).

