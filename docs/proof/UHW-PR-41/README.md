Proof plan for UHW-PR-41: Block Playback Without Reciter

1) Ensure no reciter is selected (clear reciters.order).
2) Tap Play on Verse/Range/Page/Surah.
- Expected: toast "Select at least one reciter"; no playback; Logcat warns "Play request ignored: no reciters selected".
3) Select a reciter, then Play.
- Expected: playback starts; logs show Load action.

