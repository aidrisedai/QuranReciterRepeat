UHW-14 — Progressive Caching Proof

What to capture
- Device File Explorer screenshot showing cached files under `data/data/com.repeatquran/files/audio/<reciterId>/`.
- Logcat snippet showing first-time `CacheManager` lines and no new lines on replay.

Steps
1) Launch app and select a reciter.
2) Play a single verse (e.g., 002:255), a small range (002:255–257), Page 1, and Surah 1.
3) Open Android Studio → Device File Explorer → `data > data > com.repeatquran > files > audio > <reciterId>`.
   - Verify presence of files like `002255.mp3`, `002256.mp3`, `001001.mp3` … `001007.mp3`.
4) Capture a screenshot and save it as `cache-screenshot.png` in this folder.
5) In Logcat, filter by `CacheManager` and copy logs to `cache-log.txt` in this folder.

Notes
- File naming: `SSS` = 3-digit surah; `AAA` = 3-digit ayah.
- Per-reciter: each reciter caches to its own subfolder.
