Title: UHW-15 — Downloads Screen (Check/Download/Clear per Reciter)

Summary
- Adds a simple Downloads screen to check cache status and manage downloads for Surah and Page per selected reciter.
- Status shows ✅ when all ayahs cached, else ⬇️ with X/Y count.

Scope
- UI: `DownloadsActivity` + `activity_downloads.xml` with Reciter dropdown, Surah and Page sections.
- Logic: Uses `CacheManager` to enqueue missing files and delete cached files; Page enumeration via `page_segments`.
- Navigation: Button on Home to open Downloads.

Proof
- Add screenshot to `docs/proof/UHW-15/downloads-screenshot.png`.
- Steps in `docs/proof/UHW-15/README.md`.

Regression
- Playback, repeat logic, reciter order unaffected.
- Only additive UI; no change to service or queue building.

How to test
1) Build and install debug APK.
2) Home → Downloads.
3) Select a reciter; for Surah 001 tap Check/Download Missing/Clear and observe counts.
4) For Page 1 do the same.
5) Confirm files appear under internal storage and that re-check reaches ✅.

Notes / Follow-ups
- Consider DownloadManager for progress and resumable downloads in v2.
