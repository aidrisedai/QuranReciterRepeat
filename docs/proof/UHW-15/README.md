UHW-15 — Downloads Screen Proof

What to capture
- Screenshot of the Downloads screen showing:
  - Reciter dropdown
  - Surah status (✅ when fully cached, ⬇️ otherwise) with X/Y count
  - Page status similarly
  - Demonstrate Clear → Check updates status

Steps
1) From Home, tap “Downloads”.
2) Pick a reciter.
3) For Surah: choose a small surah (e.g., 001) → tap Check. If ⬇️, tap Download Missing → wait a few seconds → Check again (should approach ✅). Tap Clear → Check (should drop).
4) For Page: enter Page 1 → Check → Download Missing → Check → Clear → Check.
5) Save screenshot as `downloads-screenshot.png` in this folder.

Notes
- Status is per reciter. Switching reciters changes counts.
- Downloads run via `CacheManager.cacheAsync`; allow a few seconds for the first batch to materialize.
