📋 UX Review Artifact — Repeat Quran (v1)
✅ Decisions + Alternatives

Streak banner: persistent + minimal text only. (Alt: contextual / history only)

Surah/Reciter: hybrid (dropdowns + quick-select tiles). (Alt: dropdowns only / progressive)

Quick-select tiles: most recently used. (Alt: curated list / pinned favorites)

Repeat dropdown: basic visible, advanced under “More.” (Alt: all visible / adaptive)

Multi-reciter: fixed slots with dropdowns, no duplicates. (Alt: multi-select / draggable chips)

Playback controls: hybrid (repeat inline, reciter in options sheet). (Alt: all inline / all hidden)

History: merged into one “Recent Sessions” card. (Alt: separate / resume only)

Downloads: hybrid (auto + manual pre-download). (Alt: auto only / manual only)

Bottom nav: icon-only. (Alt: icons + labels / adaptive labels)

Error handling: plain messages + actionable fixes. (Alt: plain only / silent fallback)

Streaks: light encouragement messages. (Alt: none / badges/goals)

Range selection: dropdowns with type-ahead. (Alt: scroll wheels / tap-to-select)

Spaced repetition: toggle under repeat dropdown. (Alt: hidden / presets only)

History: replay + edit + save as preset. (Alt: replay only / replay + edit)

Presets: stored in History tab. (Alt: separate tab / inline on Home)

Storage: invisible to users. (Alt: basic info / detailed usage)

Dark mode: none in v1 (light only). (Alt: toggle / playback-only dimming)

Onboarding: one-screen walkthrough. (Alt: skip / inline tooltips)

Analytics: minimal (sessions + errors). (Alt: streak+repeat / detailed playback)

⚠️ Usability Gaps

Dropdown fatigue: heavy reliance still risks feeling like a form.

Storage invisibility: may frustrate users on low-end devices if downloads fail.

Icon-only nav: discoverability issue for first-time users.

History + Presets: merged UI could clutter over time.

No dark mode: may alienate night-time revisers.

⚠️ Test Coverage Gaps

Need real-world tests on time-to-start playback (dropdowns vs tiles).

Error recovery under offline/storage scenarios must be simulated.

Onboarding effectiveness: measure if users start playback within 30s.

Retention vs streak banner: test if encouragement improves repeat use.

History vs Presets confusion: usability test whether users see the difference.

⚠️ Blind Spots

Multi-reciter limit: fixed 2 slots may block advanced users.

Encouragement tone: must not clash with spiritual context.

Spaced repetition: toggle is visible, but concept may remain unclear.

Analytics minimalism: could leave you blind on which features truly add value.

🌱 Growth Reflection

Repeat Quran v1 achieves ultra-minimalism with memorization focus. It trades feature depth for clarity, which is smart. But by hiding storage, limiting analytics, and sticking to light mode only, you’re leaving blind spots that might surface once testers come in.

Growth hinges on:

Adding voice input to reduce dropdown fatigue.

Expanding multi-reciter beyond 2 for advanced learners.

Testing spaced repetition UX early so it doesn’t stall adoption.

Building feedback loops (via analytics + user interviews) to prioritize what really drives retention.