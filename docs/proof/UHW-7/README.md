Proof Artifacts — UHW-7: Repeat Dropdown

Please add the following here:

- repeat-dropdown.png — Screenshot showing the exposed dropdown with values {1,3,5,10,∞}
- repeat-log.txt — Logcat extract showing PlaybackService logging the selected repeat count on play/start

Test checklist:
1) Launch app → open dropdown and select a value (e.g., 3 or ∞).
2) Force-stop the app and relaunch → dropdown should show the previously selected value (persistence check).
3) Tap Play → capture Logcat containing: "Starting playback with repeat count=...".
4) Save a screenshot and the log export to this folder.

