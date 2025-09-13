UHW-16 — Offline Playback (With Online Fallback) Proof

What to capture
- Short demo video in airplane mode showing:
  - Cached content plays (e.g., Surah 001 or Page 1 after pre-downloading some items).
  - Uncached items skip with a toast: “Offline: skipping uncached item”.
  - Upfront warning when trying to play a selection with zero cached items offline.
- Log export (filter by `PlaybackService` and `CacheManager`).

Steps
1) Use Downloads screen to cache a small set (e.g., Surah 001 or Page 1).
2) Enable Airplane Mode.
3) Play:
   - Single cached ayah → should play.
   - Single uncached ayah → service shows toast and skips.
   - Page/Surah with partial cache → cached ayahs play; uncached ayahs skip with a toast; playback doesn’t stall.
   - Selection with zero cached items → upfront toast: “Offline: no cached audio for this …”.
4) Save the video as `offline-demo.mp4` and logs as `offline-log.txt` in this folder.

Notes
- This UHW does not add retry-once UI; that’s UHW-18.
