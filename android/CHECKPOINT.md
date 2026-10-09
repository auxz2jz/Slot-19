# Android Recovery Checkpoint — v0.2.0 Smart Auto Capture

Updated 2026-10-08 PDT / GitHub build records are 2026-10-09 UTC. This file is the **current Android-only handoff**. Prior detailed v0.1.0 test evidence is archived verbatim at `android/history/ANDROID_V0.1.0_CAPTURE_REPORT.md`.

## Current status (do not confuse with Windows)

- Repository: `auxz2jz/Slot-19`. Governing instructions: `auxz2jz/master-instruction-library` and repo `AGENTS.md`.
- Platform: **Android**; owned source `android/`, Android-only GitHub Actions workflow. No edits to Windows source or windows-specific documentation from Android feature work.
- **LAST USER-VERIFIED ANDROID BASELINE: v0.1.0 for the *capture workflows*** (live camera sample frames and recorded-video extraction/preview; NO 3D reconstruction). The user's guided live run marked PASS and the user explicitly said the final extracted frame from recorded video looked fine. Preservation branch: `backup/android-v0.1.0-user-verified-capture`, exact source commit `fd7973c6a978136d1154872162f772127c5ecfd9`. v0.1 APK SHA-256: `132a5780d575bee9d243c9adccb35a953b42ad998760ef3711eaac620988b639`. Live diagnostic run 30/30 saved JPEGs with manual PASS; recorded-video run 40/40 saved JPEGs; user separately confirmed visible recorded image. Both diagnostic ZIPs were structurally valid, with no capture error reported.
- **LATEST UNVERIFIED ANDROID CANDIDATE: v0.2.0**, `versionCode 2`, `versionName 0.2.0`, package `com.auxz2jz.videogrammetry`. Candidate exact tested source commit `21cc221cdd4421e3ee17c8167b970486cb38f365`; preserved branch `backup/android-v0.2.0-smart-ci-candidate`.
- GitHub Actions `37894898716` SUCCESS — `testDebugUnitTest` and `assembleDebug` both PASS, artifact ZIP checksum generated and APK uploaded. Build URL: https://github.com/auxz2jz/Slot-19/actions/runs/37894898716
- Artifact name `video3d-android-v0.2.0-candidate`, artifact ID `11599926676`. APK filename `Video3DCapture-Android-v0.2.0-CANDIDATE.apk`; APK bytes `11550489`; SHA-256 `320c78731c15567e377db30f1ccd6b6db223eb25611230c94e553842f37ec785`. Artifact ZIP and APK hash checked against embedded `SHA256SUMS.txt` (PASS).
- **Android v0.2.0 has not been physically tested or user-verified.** No automatic-photo camera test, useful-view selection test, JPEG color/orientation/FOV test, or photo-coverage test performed on user's device. The v0.1.0 verified capture baseline is never overwritten merely by v0.2.0 build success.

## Implementation / v0.2.0 feature

**Three capture modes, not replacements:** (1) Original `Start Live Sampling` (up to 30 fixed-interval camera frames), (2) `Choose Video` (up to 40 fixed-interval imported video frames, with progressive saved-frame preview now visible during import), (3) new `Start Smart Auto Capture` / `Stop Smart Auto Capture`.

The new smart mode uses CameraX ImageAnalysis at approximately 350ms intervals to evaluate a lightweight 64x48 grayscale thumbnail. `SmartFrameSelector.kt` checks mean brightness, spatial-edge sharpness proxy, stability between frames, image-change estimate since last accepted photo, 1.8s minimum spacing, plus estimated possible lost overlap. It displays simple panorama-inspired operator feedback and a **view-change progress bar**; it does NOT measure camera pose, degrees, distance, true geometric image overlap, or object-complete coverage.

The optional CameraX **ImageCapture** use case requests a full-resolution still JPEG after the algorithm accepts a new view; saved JPG is not an overlay screenshot and is not re-compressed. `ScanRun.saveCapturedJpeg` records file size, dimensions, SHA256, time and score metrics; an automatic action is PASS only after a real decodable file is present. Up to 30 smart photos per run. Smart use-case binding failure should not prevent the original two capture modes, but that fallback is not yet physically device-tested.

The Compose preview shows the latest saved frame at reduced display resolution to prevent huge Bitmap memory use, while saved full-resolution JPEG remains intact. Still photos may contain EXIF rotation metadata that needs device verification. Existing guided `Test This Version` result semantics and diagnostics ZIP remain; new smart decisions emit rate-limited `FRAME_DECISION` events and saved-frame validation.

## Known limitations, risks, and diagnostic gaps

- Quality/novelty heuristics are **experimental**, must not be interpreted as ORB-inlier/RANSAC verified overlap or reliable panorama direction. Smooth or repetitive objects and camera exposure changes can confuse the selector; tests on different backgrounds are essential.
- Additional CameraX ImageCapture use case may not be compatible with every device/camera combination. Disable that third mode if its binding fails, rather than regress legacy capture. Needs physical test.
- v0.2.0 is still a **capture-only** app; no sparse point cloud, SfM, mesh, texture, laser or 3D reconstruction engine.
- Live v0.1 output had square 1088x1088 sample dimensions; no assumption of complete field of view. Smart full-res actual aspect ratio and EXIF orientation are unverified.
- Diagnostic ZIP includes export-start events but may not include its own export-completion event, because that event is written after closing ZIP. Historical known gap; do not misreport ZIP failure.
- The smart guide uses the existing latest-run manual pass/fail; **not a complete spatial-coverage analysis**. Expected Behavior Failed must remain selectable.
- Android GitHub Actions build uses debug signing; the certificate may differ from a previously installed candidate. If upgrading fails, export diagnostics and preserve desired app data BEFORE uninstalling the old APK, as uninstall deletes app-private capture runs.
- No user device diagnostics exist yet for v0.2.0.

## Build/debug and tests

- Initial v0.1 build SDK setup and missing `setContent` import problems are archived; v0.1 user-tested capture baseline is preserved.
- v0.2 tests added in `SmartFrameSelectorTest.kt` for first-frame, duplicate rejection, motion/stabilization, too-dark/too-bright/soft image rejection, brightness compensation, capture cooldown and bounded progress; original `FramePolicyTest.kt` retained. GitHub Actions ran JVM tests and `assembleDebug` successfully.
- A smart-capture error cannot silently yield PASS after an earlier successful frame: `smartHadError` forces failure. Preview display decode downsampled separately from full-size photo.
- No real camera/Android instrumentation test in CI; correctness of actual full-resolution photos and selection behavior remains USER TEST PENDING.

## Exact next action

Install v0.2.0 on Android phone (safely preserving v0.1 data). Check `Start Live Sampling` and `Choose Video` still behave correctly. Test `Start Smart Auto Capture` on a stationary object: first hold still, then move gradually to a new angle and hold again. Observe guidance/progress and number of *actual validated full-resolution photos*. Try leaving camera still (should not accumulate many identical photos), then poor lighting/fast movement (should warn or decline). `Stop Smart Auto Capture`, inspect saved latest-photo appearance, choose manual PASS or `Expected Behavior Failed` via `Test This Version`, export diagnostics and share results.

Only mark v0.2.0 verified after user says it works. If regression, return to preserved verified v0.1.0 source/APK and make smallest evidence-based correction. Windows and Slot-8 source remain untouched.
