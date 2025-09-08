Proof Artifacts — UHW-10: Nested Sequential Playback

Please attach the following artifacts here:

- nested-cycle-demo.mp4 — Demo showing Reciter A → Reciter B → … order repeating for N cycles
- logs.txt — Logcat export including lines "Cycle order (...)" and "Enqueued N cycles, itemsPerCycle=..., total=..."

Test checklist
1) Choose 2–3 reciters and confirm order in the UI.
2) For single ayah: set repeat=2, Load Ayah; verify order A→B→A→B and stop.
3) For range: choose a small range, set repeat=2, Load Range; verify order A(all)→B(all)→A(all)→B(all).
4) Confirm logs display the cycle order and queue sizing.

