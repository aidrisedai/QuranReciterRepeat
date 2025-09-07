Proof Artifacts — UHW-8: Repeat Logic

Please add the following here:

- repeat-3x-demo.mp4 — Video showing a verse repeating exactly 3 times before advancing
- repeat-infinite-demo.mp4 — Short video showing a verse repeating indefinitely until Next is pressed
- repeat-logs.txt — Logcat export showing loop detections and transitions

Test checklist:
1) Set repeat to 3 in the dropdown; start playback. Observe the same verse plays 3x, then advances.
2) Set repeat to ∞; start playback. Observe it loops the same verse; press Next and it advances.
3) Try Next/Prev during repeats; confirm it navigates correctly and resets repeat count.
4) Save the two videos and a log export filtered by tags: PlaybackService|PlaybackManager.

