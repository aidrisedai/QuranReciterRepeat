Proof Artifacts — UHW-28: Page Playback Integration

Please add the following here:

- page-playback-demo.mp4 — Video showing playback of a chosen page with nested reciters (A → B → …) and correct repeats
- logs.txt — Logcat export including lines:
  - "Cycle order (page X): …"
  - "Page X itemsPerCycle=..."
  - "Enqueued N cycles, itemsPerCycle=..., total=..." or "Enqueued 1 cycle (∞ loop), items=..."

Test checklist
1) Choose 2 reciters and order them, set repeat=2.
2) Enter a page (e.g., 1 or any) and tap Load Page.
3) Verify order A(all ayahs on page) → B(all ayahs on page) → A(all) → B(all), then stop.
4) Try repeat=∞; verify looping.
5) Confirm logs include cycle order and sizing details.

