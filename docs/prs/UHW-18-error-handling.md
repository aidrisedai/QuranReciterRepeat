Title: UHW-18 — Error Handling (Retry Once + Actionable)

Summary
- Auto-retry once on online playback errors; if it fails again, show a notification with Retry/Skip actions handled by the service.

Scope
- Service: retry map; ACTION_RETRY_ITEM and ACTION_SKIP_ITEM; error notification channel and actions; cancel on success.

Proof
- `docs/proof/UHW-18/error-retry-log.txt` (logcat with failure → retry → action); optional short video.

Regression
- Offline path unchanged (skip with toast); playback order and repeat preserved.

Branch
- `feature/uhw-18-error-handling`

