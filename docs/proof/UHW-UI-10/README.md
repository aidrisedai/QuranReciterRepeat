UHW-UI-10 — Half Recitation Toggle (Settings Visual)

Proof capture suggestions:
- Screenshot 1: Settings screen showing the “Split halves across reciters” toggle and the two visual bars.
- Screenshot 2: Range/Page/Surah tab with the toggle visible and reflecting the saved state.
- Log snippet: `half_split_set` from Settings and a subsequent `Page half-split enabled; itemsPerCycle=...` from PlaybackService.

How to reproduce:
1) Open app → Menu → Settings → Toggle “Split halves across reciters” ON. Take a screenshot.
2) Return to Page tab (or Range/Surah); verify switch is ON. Take a screenshot.
3) Select reciters (2 or 3) and start playback for a Page. Export logs showing half-split enabled.

