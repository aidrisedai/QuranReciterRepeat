Proof Artifacts — UHW-5: Playback Service

Please add the following here:

- background-playback-demo.mp4 — Video showing playback continues after switching apps and via Bluetooth handoff
- call-interruption-log.txt — Logcat export showing pause/resume around a mock/real phone call

Test checklist:
1) Launch app; playback starts.
2) Press Home; confirm audio continues and a media notification appears.
3) Connect/disconnect a Bluetooth headset; confirm audio routes correctly and controls work.
4) Trigger a phone call (or simulate focus loss); confirm playback pauses and resumes after.
5) Save screen recording and Logcat filtered by tags: PlaybackService|ExoPlayer|MediaSession.

