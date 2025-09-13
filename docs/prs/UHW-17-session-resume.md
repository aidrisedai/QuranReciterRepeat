Title: UHW-17 — Session Resume (Exact Position)

Summary
- Adds exact resume across single/range/page/surah by persisting selection context, reciters, repeat, media index, and positionMs; rebuilds queue and seeks to exact ms on ACTION_RESUME.

Scope
- Service: ACTION_RESUME; periodic save to SharedPreferences; rebuilds with saved reciters; IO thread fix for page resume DB query.
- UI: Resume button on Home; disabled during active playback via service broadcast.

Proof
- `docs/proof/UHW-17/resume-demo.mp4`.

Regression
- Playback order, repeat accuracy, and offline handling preserved.

Branch
- `feature/uhw-17-session-resume`

