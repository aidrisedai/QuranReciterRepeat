# 📝 Repeat Quran — Project Notes

## 📌 Mid-Project Feature Requests
- [ ] **Feature:** Voice Input for Commands  
  - **Requested by:** CEO (future growth reflection)  
  - **Decision:** Log for later (defer to v2)  
  - **Changelog Reference:** Pending  

- [ ] **Feature:** Alternating Playback (Reciter A → Reciter B per verse)  
  - **Requested by:** CEO (reflection during design)  
  - **Decision:** Log for later (advanced learners, v2)  
  - **Changelog Reference:** Pending  

- [ ] **Feature:** Adaptive Spaced Repetition  
  - **Requested by:** CEO (growth reflection)  
  - **Decision:** Log for later (premium feature)  
  - **Changelog Reference:** Pending  

---

## ✅ Addressed Requests (MVP)
- Surah labels in dropdowns (Verse, Range, Settings) now show `NNN — Surah Name` consistent with the Surah tab. Parsing remains stable using the first three digits.

---

## ⚠️ Blind Spots Identified
- **Area:** Cache Eviction / Storage Management  
  - **Details:** Internal storage grows unbounded with verse files.  
  - **Mitigation Plan:** Add UHW for cache management (auto-evict, per-surah delete) in v2.  
  - **Status:** Open  

- **Area:** Analytics Accuracy  
  - **Details:** MVP only logs locally; no cohort insights.  
  - **Mitigation Plan:** Add Firebase/Amplitude integration in v2.  
  - **Status:** Open  

- **Area:** Accessibility  
  - **Details:** RTL layout, font scaling, and small-screen testing not yet planned.  
  - **Mitigation Plan:** Add accessibility UHWs in v2.  
  - **Status:** Open  

- **Area:** Service Warmup Auto-Play  
  - **Details:** ACTION_START may auto-play if queue exists.  
  - **Mitigation Plan:** PR-24 removes auto-play; warm + broadcast only.  
  - **Status:** Planned (PR-24).  

- **Area:** Range Build on Main Thread  
  - **Details:** Large ranges may cause jank during queue build.  
  - **Mitigation Plan:** PR-25 offloads to ioExecutor then enqueues on main.  
  - **Status:** Planned (PR-25).  

- **Area:** Downloads I/O on UI Thread  
  - **Details:** Clear/download loops may stutter.  
  - **Mitigation Plan:** PR-26 move to background; post updates to UI.  
  - **Status:** Planned (PR-26).  

- **Area:** Duplication of AYAH_COUNTS  
  - **Details:** Defined in multiple classes; drift risk.  
  - **Mitigation Plan:** PR-27 extract helper.  
  - **Status:** Planned (PR-27).  

- **Area:** Settings/Downloads discoverability  
  - **Details:** Two screens caused confusion; Page checks overlapped toolbar.  
  - **Mitigation Plan:** Merged Downloads into Settings and removed extra visuals; toolbar sits outside scroll.  
  - **Status:** Addressed (PR-33..PR-36).  

- **Area:** Half-Split Discoverability  
  - **Details:** Users didn’t notice the per-tab toggle.  
  - **Mitigation Plan:** Add Settings entry with a global toggle and visual examples; keep per‑tab switches in sync.  
  - **Status:** Addressed (Settings added).  

- **Area:** Cultural Sensitivity of Streak Banner  
  - **Details:** Encouragement tone (“🔥 X-day streak”) could clash with spiritual context.  
  - **Mitigation Plan:** Run user testing with memorization teachers/learners in v2.  
  - **Status:** Open  

---

## ⏳ Deferred Tasks
- **Task:** Material3 (Compose) vs Material Components decision  
  - **Reason for Defer:** MVP uses Java + XML with Material Components DayNight due to developer preference; revisit Compose Material3 in v2 if needed.  
  - **Target Phase:** v2  
- **Task:** Dark Mode Refinements (night-optimized theme)  
  - **Reason for Defer:** Basic toggle only in v1; refine in v2.  
  - **Target Phase:** v2  

- **Task:** Preset Editing Robustness  
  - **Reason for Defer:** Only basic save/edit tested in v1.  
  - **Target Phase:** v2  

---

## 🧪 Regression Promises Log
- **Promise:** App builds and launches (“Hello World”)  
  - **Last Verified On:** 2025-09-07  
  - **Verification Method:** Build + emulator screenshot (stored under docs/proof/UHW-1/)  
  - **Status:** ✅  

- **Promise:** UI consistency (light/dark readability for controls)  
  - **Last Verified On:** 2025-09-07  
  - **Verification Method:** Screenshots of playback controls in both modes (docs/proof/UHW-6/)  
  - **Status:** ✅  

- **Promise:** Repeat dropdown selection always persists into playback  
  - **Last Verified On:** 2025-09-07  
  - **Verification Method:** Dropdown screenshots + Logcat showing selected repeat in PlaybackService (docs/proof/UHW-7/)  
  - **Status:** ✅  

- **Promise:** Repeat accuracy (single-verse and whole passage)  
  - **Last Verified On:** 2025-09-07  
  - **Verification Method:** Demo videos for 3× single-ayah, ∞ single-ayah, and 2× range; logs show exact playlist sizing  
  - **Status:** ✅  

- **Promise:** Cached surahs always replay offline  
  - **Last Verified On:** 2025-09-08  
  - **Verification Method:** Demo video (airplane mode); uncached items skip with message; logs captured  
  - **Status:** ✅  

- **Promise:** Resume mid‑ayah preserves exact position  
  - **Last Verified On:** 2025-09-13  
  - **Verification Method:** Resume demo across single/range/page/surah  
  - **Status:** ✅  

- **Promise:** Error handling never stalls playback  
  - **Last Verified On:** 2025-09-13  
  - **Verification Method:** Manual log with auto‑retry then actionable Retry/Skip  
  - **Status:** ✅  

- **Promise:** Progressive caching writes files for all sources (single, range, page, surah)  
  - **Last Verified On:** 2025-09-08  
  - **Verification Method:** Device File Explorer shows `files/audio/<reciterId>/<SSS><AAA>.mp3` after playback; Logcat `CacheManager` logs on first play, none on replay  
  - **Status:** ✅  

---

## 🔍 Reflection Entries
- **Checkpoint:** After Verification (Pre-Execution)  
  - **Observation:** Roadmap is complete, QA hooks embedded, blind spots logged.  
  - **Blind Spots Surfaced:** Cache management, analytics, accessibility, cultural tone.  
  - **Growth Action:** Defer blind spots into v2 UHWs; keep focus on v1 trust-first launch.  
