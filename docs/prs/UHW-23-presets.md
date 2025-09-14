Title: UHW-23 — Preset Management (Save/Edit)

Summary
- Adds Room-backed preset management: save from current inputs (single/range/page/surah), list on Home, play, rename/edit repeat, and delete.

Scope
- DB: `PresetEntity`, `PresetDao`, repo; DB version bump (fallbackToDestructiveMigration in place).
- UI: Home — “Save Preset” button with type picker; “Presets” section listing items with Play/Edit/Delete.
- Playback: Replays via existing `ACTION_LOAD_*`; uses saved reciters and repeat.

Proof
- Screenshot: `docs/proof/UHW-23/presets-screenshot.png`.
- Log: play_request emitted when replaying a preset.

Regression
- No impact on playback order, repeat accuracy, or offline/error behavior.

Branch
- `feature/uhw-23-presets`

