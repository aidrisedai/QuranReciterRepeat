UHW-19 — Unit Tests (Core + Edge Cases) Proof

What to capture
- Local test run summary (`./gradlew testDebugUnitTest`) or screenshot of passed tests.

Included tests
- PlaybackServiceRepeatTest: verifies enqueue count for single ayah with repeat=3.
- HistoryDbTest: verifies last sessions ordering via Room + Repository.
- CacheManagerTest: verifies target path and cache hit.
- ResumeCaptureTest: verifies resume.* SharedPreferences are captured on range load.

Notes
- Additional tests (error actions, deep offline cycles) can be added later; current set covers MVP-critical logic.
