UHW-18 — Error Handling (Retry Once + Actionable) Proof

What to capture
- Log snippet showing: initial failure (online), one auto-retry, second failure, then user-triggered Retry/Skip and outcome.

Steps
1) Start playback of an online item (ensure it’s not cached).
2) Briefly toggle network off/on mid-ayah to simulate an error (or use a bad URL if available).
3) Observe: first failure → auto-retry once; second failure → notification appears with “Retry” and “Skip”.
4) Tap “Retry” → item restarts from the beginning; if it fails again, tap “Skip” → queue advances.
5) Export Logcat filtered by `PlaybackService` to `error-retry-log.txt` in this folder.

Notes
- Offline errors continue to skip with a toast (from UHW-16) and won’t show the retry notification.
