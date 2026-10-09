# Android Video 3D Capture Lab — v0.1.0 CANDIDATE

Independent Android implementation in `auxz2jz/Slot-19/android/`. **Windows code remains in the historical root and must never be overwritten by Android work.** All platform rules: `../AGENTS.md`, `../shared/` and the canonical Master Instruction Library.

## APK and build record
- **Debug APK:** `Video3DCapture-Android-v0.1.0-CANDIDATE.apk` inside [GitHub Actions run 37891374599](https://github.com/auxz2jz/Slot-19/actions/runs/37891374599), artifact `video3d-android-v0.1.0-candidate`. GitHub artifacts have a limited retention period.
- Exact candidate build source: commit `fd7973c6a978136d1154872162f772127c5ecfd9`, preserved as branch `backup/android-v0.1.0-ci-candidate`.
- SHA-256: `132a5780d575bee9d243c9adccb35a953b42ad998760ef3711eaac620988b639`.
- `app-debug.apk` generated with command `gradle -p android :app:testDebugUnitTest :app:assembleDebug` using Java 17, Gradle 8.10.2 and Android 35 SDK.
- Requires **Android 8 (API 26) or later**. Unverified on any physical device. Debug signing from GitHub Actions is not guaranteed to be stable across rebuilds (an uninstall may be necessary before installing a differently signed later candidate; uninstalling may remove app-private capture runs).

## User: Test This Version
1. Download the APK artifact. On phone, install it with your normal Android file installer (approve installation from that file source when prompted).
2. Open **Video 3D Capture Lab**. Grant camera permission through **Enable Camera** and confirm the preview works.
3. Keep an object stationary, move the phone around it, press **Start Live Sampling**, then **Stop Live Sampling**. Verify actual saved frame count and preview. Live mode captures no more than 30 JPEG frames at about 1.2-second intervals.
4. Tap **Choose Video**, pick an existing video recording, wait for frame validation; verify saved frame preview. Imported-video source is not altered.
5. Tap **Test This Version**, select **Frames Look Correct** only when physically confirmed; otherwise **Expected Behavior Failed**.
6. Tap **Export Test + Diagnostics** to create a report ZIP. Diagnostic ZIP deliberately **does not include original video or saved JPEG frames**. Send it back along with any visual description of problems.

The current app does **NOT** perform photogrammetric reconstruction, generate point clouds/meshes/textures, capture full-resolution still photos, select intelligent keyframes, or laser scan. Build/CI PASS is not an Android user-verified baseline.

## Source layout
- `app/src/main/java/com/auxz2jz/videogrammetry/MainActivity.kt`: Compose UI, CameraX preview/raw frame sampling, video importer and guided test controls.
- `ScanRepository.kt`: isolated run storage, frame validation/hash, persistent diagnostics, test report and redacted ZIP export.
- `FramePolicy.kt`: bounded fixed-interval sampling policy with JUnit tests.
- `CHECKPOINT.md`: exact source build hash, test history, known limits, and next action.
- `PROJECT_MEMORY.md`, `ROADMAP.md`, `DIAGNOSTICS_AND_TESTING.md`: Android-only durable state.

Windows/Codex should read `../windows/OWNERSHIP.md` and only modify Windows-owned files.
