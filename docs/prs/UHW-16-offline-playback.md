Title: UHW-16 — Offline Playback (With Online Fallback)

Summary
- Ensures smooth behavior offline: cached items play; uncached items skip with clear messaging and no stalls.

Scope
- Manifest: add ACCESS_NETWORK_STATE for connectivity checks.
- Util: NetworkUtil.isOnline(Context).
- PlaybackService: per-item offline handling via onPlayerError; upfront toast when an offline selection has zero cached items; builders now compute cached counts.

Proof
- Demo in airplane mode showing cached playback and uncached skips: `docs/proof/UHW-16/offline-demo.mp4`.
- Logs: `docs/proof/UHW-16/offline-log.txt`.

Regression
- Playback order, repeat accuracy, and UI consistency preserved.
- Changes only affect error/offline paths.

How to test
1) Use the Downloads screen to cache some ayahs (e.g., Surah 001 or Page 1).
2) Enable Airplane Mode.
3) Play single, range, page, and surah cases; confirm behavior and capture proof assets.
