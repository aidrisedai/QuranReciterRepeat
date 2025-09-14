Title: UHW-24 — APK Packaging (Sideload)

Summary
- Adds signingConfig scaffold for release APK signing; keystore is local and referenced via gradle.properties.

Scope
- app/build.gradle: release signingConfig reads RQ_* properties for keystore file, store/key passwords, and alias.
- Proof instructions and steps under `docs/proof/UHW-24/`.

How to build
- Create keystore (see README), add properties, run `./gradlew assembleRelease`.
- Artifact: `app/build/outputs/apk/release/app-release.apk`.

Regression
- No runtime changes; only build configuration.

Branch
- `feature/uhw-24-apk`

