UHW-24 — APK Packaging (Sideload) Proof

Keystore (local; do not commit)
1) Create a release keystore:
   keytool -genkeypair -v -keystore ~/release.keystore -alias rq -keyalg RSA -keysize 2048 -validity 3650
2) Add to your ~/.gradle/gradle.properties (or project gradle.properties, not committed):
   RQ_STORE_FILE=/Users/you/release.keystore
   RQ_STORE_PASSWORD=your_store_password
   RQ_KEY_ALIAS=rq
   RQ_KEY_PASSWORD=your_key_password

Build
- ./gradlew assembleRelease
- Artifact: app/build/outputs/apk/release/app-release.apk

Install
- adb install -r app/build/outputs/apk/release/app-release.apk
- Or copy to device and tap to install (allow unknown sources).

Verify
- Launch app; onboarding shows on first run; QA button hidden in release.
- Quick smoke test: play a single ayah; pause/resume; observe media notification.

Artifacts to attach
- Screenshot of app/build/outputs/apk/release/ folder with APK.
- Install screenshot/short video.
