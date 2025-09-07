🎨 Repeat Quran — Design Artifact (v1)
Wireframe Sketches (structured ASCII-style)

Home (Hybrid Layout)

 -------------------------------------------------
| 🔥 Streak Banner: "3-day streak"                |
 -------------------------------------------------
| ▶️ Resume Last Session                          |
 -------------------------------------------------
| [Surah ▼ search] [Verse Range ▼] [Reciter ▼✓]   |
| [Repeat ▼ (1,3,5,10,Until,Custom)]              |
 -------------------------------------------------
| Quick History (last 4)                          |
| - Surah Kahf 1–10 | Minshawi | 3x | ▶️          |
| - Surah Yasin 20–25 | Husary | ∞ | ▶️           |
| - ... up to 4                                     |
 -------------------------------------------------
| Bottom Nav: [🎧 Home] [🕘 History] [⬇️ Downloads]|
 -------------------------------------------------


Playback Screen (Dynamic Controls)

 -------------------------------------------------
| Surah Kahf | Verses 1–10 | Minshawi | Repeat 3x |
 -------------------------------------------------
| [◀️ Prev]  [⏯️ Play/Pause]  [▶️ Next]           |
| [Change Reciter ▼] [Change Repeat ▼]            |
 -------------------------------------------------


Downloads Tab

 -------------------------------------------------
| Surah Al-Fatiha ▼                               |
|   - Minshawi  ✅ downloaded                     |
|   - Husary    ⬇️ not downloaded                 |
 -------------------------------------------------
| Surah Al-Baqarah ▼                              |
|   - Minshawi  ⬇️                                |
|   - Sudais    ⬇️                                |
 -------------------------------------------------


History Tab

 -------------------------------------------------
| Full History                                    |
| - [▶️] Surah Kahf 1–10 | Minshawi | Repeat 3x   |
| - [▶️] Surah Yasin 20–25 | Husary | Repeat ∞    |
| - ...                                           |
 -------------------------------------------------

Key Design Decisions + Alternatives

Hybrid Home chosen over simple or wizard.

Dropdowns with search for Surah, Verse, Reciter (fast + minimal).

Multi-reciter = multi-select dropdown with automatic numbering.

All repetition formats in one dropdown (clear, nothing hidden).

Dynamic playback controls (full flexibility).

History = last 4 sessions with quick actions.

Offline caching = automatic + optional Downloads tab.

Bottom nav (icon-only) with Home, History, Downloads.

Light mode only for v1.

Session streak as banner at top of Home.

Block info at top of playback screen (always visible).

Usability Score (self-assessment)

Clarity: 4.5 / 5 (dropdowns, streak banner, simple nav).

Speed: 4 / 5 (hybrid home + quick resume).

Flexibility: 4 / 5 (multi-reciter, dynamic repeat).

Overall Usability: 4.2 / 5 for v1 (strong minimal baseline).

Blind Spots

Dropdown-heavy flow may feel “form-like” instead of fluid.

No translation might frustrate casual listeners.

Session streak could feel gamified in a spiritual context.

Icon-only bottom nav may confuse some users (need clear icons).

Storage concerns on low-end devices if caching grows.

Growth Reflection

v1 keeps focus on memorization + repetition.

Future growth:

Voice commands to reduce dropdown reliance.

Dark mode for night-time revisers.

Alternating playback for advanced learners.

Adaptive spaced repetition as premium feature.