UHW-22 — Minimal Analytics Logs Proof

What to capture
- Logcat screenshot showing events: app_open, repeat_set, load_* and error_*.
- Snippet of `files/logs/analytics-0.log` content.

Steps
1) Launch app (triggers app_open).
2) Change repeat value (repeat_set).
3) Start a single verse, range, page, and surah (load_single/load_range/load_page/load_surah).
4) Induce an error (toggle network mid-verse) to capture error_playback and error_retry/actionable.
5) Copy `analytics-0.log` lines and save to `docs/proof/UHW-22/analytics-log.txt`.
