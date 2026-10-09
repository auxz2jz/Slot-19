# Android Video 3D Capture Lab — v0.2.0

This is the **Android-only** implementation of Slot-19. The Windows Python/Codex project is independent and owns the historical repository root. See `../AGENTS.md`, `../windows/OWNERSHIP.md`, `../shared/`, `PROJECT_MEMORY.md`, `CHECKPOINT.md`.

## Download / APK build evidence

**Latest Android APK is v0.2.0 CANDIDATE (not user-verified).** [GitHub Actions success](https://github.com/auxz2jz/Slot-19/actions/runs/37894898716), artifact `video3d-android-v0.2.0-candidate` (ID `11599926676`); inside: `Video3DCapture-Android-v0.2.0-CANDIDATE.apk`.

- Source tested: `21cc221cdd4421e3ee17c8167b970486cb38f365`, protected as `backup/android-v0.2.0-smart-ci-candidate`.
- SHA-256 `320c78731c15567e377db30f1ccd6b6db223eb25611230c94e553842f37ec785`.
- APK size: `11550489` bytes. Android 8+ (minSdk 26); Android SDK 35 build, debug signed.
- Build and unit-test command: `gradle -p android :app:testDebugUnitTest :app:assembleDebug`, Gradle 8.10.2, Java 17.
- **LAST USER-VERIFIED BASELINE: Android v0.1.0 core capture only**, preserved in `backup/android-v0.1.0-user-verified-capture`. Do not downgrade/delete it based on an untested newer candidate. If the APK cannot upgrade due to debug signing, export any desired runs BEFORE uninstalling v0.1.0; uninstall removes app-private data.

## Three modes (v0.2.0)

1. **Start Live Sampling / Stop Live Sampling** — original fixed-interval live CameraX saved frames, up to 30; v0.1.0 user-verified behavior preserved in code.
2. **Choose Video** — import a recording with Android system file picker and extract up to 40 frames, about 1 second apart. Now shows saved-frame preview incrementally during extraction, though video analysis itself remains fixed-interval.
3. **Start Smart Auto Capture / Stop Smart Auto Capture** — preview camera continuously; lightweight visual scoring asks you to move to a new angle, hold steady, adjust light, or move back when image changes too much. A small progress bar estimates image-change toward another viewpoint; it is NOT a measured movement angle, reliable overlap %, or panorama coverage map. An accepted frame triggers an actual CameraX high-resolution still JPEG, not a saved preview screenshot; max 30 photos. Saved image is hash/dimensions validated in its run's manifest.

No 3D reconstruction, camera-pose recovery, mesh/texture, or laser scanning yet. No automatic engine has produced a 3D result.

## Test v0.2.0 on your phone

1. First export any current tests/captures from v0.1.0; install v0.2.0 over existing app if Android accepts its debug signature. If upgrade fails, uninstall only after backing up desired app-private data.
2. Verify old **Start Live Sampling** and **Choose Video** still work, including the new progressive preview while video frames extract.
3. Set a textured, well-lit **stationary object** on a table and start **Smart Auto Capture**. Hold steady and see whether the first photo is saved.
4. Keep phone in the same location for a few seconds — the app should not take a flood of identical images.
5. Slowly move around the object to a somewhat new angle, with overlapping views. Hold steady again until the guidance reports a new photo. The progress bar is a heuristic, not an exact motion guide.
6. Move too rapidly or use poor light to see if warnings change. Do not depend on this experiment to measure spatial object coverage.
7. Tap **Stop Smart Auto Capture**. Check validated photo count, view last saved photo for color/orientation and ask whether image was truly full-resolution.
8. Open **Test This Version** and select **Frames Look Correct** only for actual good results; otherwise **Expected Behavior Failed**. Tap **Export Test + Diagnostics** to save an evidence ZIP, then share it with this chat.

Diagnostic ZIP includes manifest/run/test and operation events, not private JPEGs/videos; a separately uploaded screenshot/photo is needed to assess actual sharpness/geometry visually. Documented ZIP export-completion trace limitation from v0.1.0 remains.

## Developer map
- `app/src/main/java/com/auxz2jz/videogrammetry/MainActivity.kt` — Compose screen, CameraX binding and three modes.
- `SmartFrameSelector.kt` — experimental grayscale image quality/view-change logic (no measured pose).
- `ScanRepository.kt` — frame/JPEG validation, hashing, reports, diagnosis, testing, export.
- `FramePolicy.kt` — v0.1 fixed-interval behavior preserved.
- `app/src/test/.../SmartFrameSelectorTest.kt` and `FramePolicyTest.kt` — JVM policy regression tests.
- `SMART_CAPTURE_DESIGN.md`, `CHECKPOINT.md`, `PROJECT_MEMORY.md`, `ROADMAP.md` — enduring design, builds, last verified baseline and exact next action.
- `history/` — archived earlier checkpoint evidence.

**Windows source remains untouched by Android development.**
