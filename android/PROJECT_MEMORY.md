# Android Project Memory — Video 3D Capture Lab

## Ownership / source of truth

Updated 2026-10-08 PDT. Android development owns `android/` and its dedicated workflow `.github/workflows/android-build.yml` in `auxz2jz/Slot-19`. Windows/Codex owns the original root Python implementation, source/tests/checkpoint; **Android must not edit Windows sources**. See root `AGENTS.md`, `windows/OWNERSHIP.md`, `shared/` and `auxz2jz/master-instruction-library`.

## Goal

Android-native camera/video/still capture for 3D photogrammetry, stationary object and moving handheld phone. Later add natural-feature reconstruction, dense geometry, mesh and texture. Camera capture modes remain distinct. No turntable or special markers required. No runtime GitHub/cloud dependency.

## Last user-verified baseline (capture only)

**Android v0.1.0** was physically exercised: CameraX live frame sampling produced 30 verified frames and user marked visual guide PASS. Recorded-video import yielded 40 valid frames and the user subsequently confirmed its displayed extracted frame looked fine. Both diagnostic ZIPs valid. This verifies those core capture behaviors on the user's device, NOT any geometric reconstruction or object-coverage claim.

- Source commit `fd7973c6a978136d1154872162f772127c5ecfd9`.
- Permanent backup branch `backup/android-v0.1.0-user-verified-capture` (source also preserved in `backup/android-v0.1.0-ci-candidate`).
- APK SHA-256 `132a5780d575bee9d243c9adccb35a953b42ad998760ef3711eaac620988b639`.
- Detailed device evidence archived in `android/history/ANDROID_V0.1.0_CAPTURE_REPORT.md`.

## Latest UNVERIFIED candidate: Android v0.2.0

- New **Smart Auto Capture** as a third mode; original `Start Live Sampling` and `Choose Video` retained.
- `SmartFrameSelector.kt` analyzes downsampled live frames for lighting, sharpness proxy, stability, and image-change proxy. Guidance and progress indicator are approximate; no camera pose or physical-angle tracking.
- When qualified, uses CameraX ImageCapture for an actual full-resolution still JPEG, independently verified by dimensions/bytes/hash, saved without preview overlay or downsampling. Max 30 smart photos per run.
- Importing an existing video now shows latest saved frames periodically during extraction rather than only at completion.
- Android `v0.2.0`, `versionCode 2`, code commit `21cc221cdd4421e3ee17c8167b970486cb38f365`; backup `backup/android-v0.2.0-smart-ci-candidate`.
- GitHub CI run `37894898716` completed SUCCESS: Kotlin compile, SmartFrameSelector/FramePolicy JVM tests, debug APK assembly and artifact upload.
- Artifact `video3d-android-v0.2.0-candidate` ID `11599926676`, APK SHA256 `320c78731c15567e377db30f1ccd6b6db223eb25611230c94e553842f37ec785`. Android v0.2.0 device user verification: **NONE**.

## Current diagnostics and shortcomings

Built-in `ScanRepository.kt` persists per-run `manifest.json`, `result.json`, `events.jsonl`, validated saved JPEGs, guided manual test results, redacted ZIP; smart mode includes `FRAME_DECISION`, shutter attempt and validated-result events. The ZIP self-export completion event may not be included in the same export; this is a known issue, not a capture failure.

Experimental smart selection is heuristic and not SfM-verified. False positives/negatives possible with backgrounds, lighting and motion. Need actual user camera compatibility, full-res photo orientation, progress guidance effectiveness, and preservation of both prior modes. No actual 3D reconstruction/laser scanning implemented here.

## Recent failure/build history

v0.1 initial GitHub SDK setup issue then missing Compose import fixed. v0.2 added unit-checked selection and both capture modes; final code built in CI. No known physical v0.2 failure yet. Respect two-equivalent-failures stop rule and global diagnostics/testing standards.

## Exact next step

Install v0.2.0 debug APK, compare original two capture modes, then enable `Start Smart Auto Capture` and test stationary-phone no-duplicates, slow-angle move+hold auto shutter, blur/lighting rejection, and clear full-resolution saved photo. Complete `Test This Version` with explicit pass/fail, export ZIP and provide feedback. **Do not promote v0.2.0 as verified until user confirms.** If installing over earlier GitHub debug build fails because the signing key changed, export required app-private data before uninstalling. No changes to Windows project.
