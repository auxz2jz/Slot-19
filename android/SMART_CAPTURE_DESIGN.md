# Smart Auto Capture — v0.2.0 implementation plan and guarded scope

## Starting evidence
- Android v0.1.0 CameraX live capture: 30/30 saved, app-validated PASS, user visually marked PASS.
- Android v0.1.0 recorded video: 40/40 saved, app-validated PASS, user says final preview looks fine (2026-10-08 PDT). No image data in ZIP, so this does not validate photogrammetric coverage.
- **Last user-verified capture baseline: v0.1.0**, source commit `fd7973c6a978136d1154872162f772127c5ecfd9`, Android APK SHA256 `132a5780d575bee9d243c9adccb35a953b42ad998760ef3711eaac620988b639`. Branch `backup/android-v0.1.0-user-verified-capture` created from this exact source. Do not overwrite or relabel it.
- Windows branch/source remain out of scope.

## Requested third mode
**Smart Auto Capture** complements existing **Start Live Sampling** and **Choose Video**, without replacing or changing those controls.

1. Live camera preview continuously available.
2. A low-resolution independent raw analysis stream measures luminance, edge/sharpness proxy, temporal stability, and visual change relative to previous accepted camera view.
3. Operator feedback: `Too dark/bright`, `Hold steady`, `Move to a new angle`, `Move back for overlap`, `Ready — capturing`, and count/progress toward next viewpoint.
4. Once acceptance conditions are met, request a normal CameraX high-resolution still via ImageCapture, not merely a screenshot of the preview.
5. A real saved, valid JPEG with dimensions/bytes/hash and manifest entry is proof of acceptance; no tap-only PASS.
6. Up to 30 frames per session, explicit Stop, one photo in flight at a time, no live video file required.
7. Save rejection reasons and scores in structured diagnostic events with rate limiting; show latest saved photo, preserve raw/preview separation, guided test and ZIP export.

## Algorithm boundaries and honesty
- First cut: lightweight pure Kotlin thumbnail score/scene novelty heuristics to test usefulness without forcing heavy SfM/NDK dependencies. It estimates view change, **not metric translation, degrees around the object, true tracked coverage, geometric RANSAC inliers or guaranteed 3D overlap**. Any arrow, distance or precise percent around object would be false precision.
- Keep cooldown and stillness gate; avoid dozens of redundant photos while stationary. Strong scene change may mean LOST overlap, not necessarily a great new view.
- Scoring thresholds are provisional and must be checked against actual device footage. Preserve manual v0.1.0 modes exactly as fallback.
- Full-resolution JPEG camera use-case combination requires device tests; if unsupported report the smart feature unavailable without breaking the original preview/legacy capture.
- At first milestone no panorama stitching, 3D reconstruction, or laser scanning.

## Diagnostics/tests
- Unit tests for sharpness/exposure, repeated frame rejection, movement, stabilization and new-view acceptance from synthetic grayscale signatures.
- CI debug APK compile + tests; no user-verified status until user installs and confirms smart mode.
- New run's accepted frames must be genuine JPEG outputs from ImageCapture. Track view metric, suggested guidance, thresholds, acceptance/rejection reasons, capture completion, valid frame count, capture failures, and test/report.
- Test three cases: stationary phone (should not rapidly accept redundant frames), move slowly around object then pause (should accept more views), move/blur too fast or poor lighting (should give useful warning).
- Future: ORB/SIFT spatial feature tracking and essential-matrix geometry, improved overlap, coverage map / panorama-inspired directional guide when pose estimation is demonstrably reliable.

## First code checkpoint
Add `SmartFrameSelector.kt` plus JVM tests, integrate optional CameraX ImageCapture, add `ScanRun.saveCapturedJpeg`, then UI third mode and status/progress, increment Android version to v0.2.0/versionCode 2, GitHub CI and real-device guided-test plan. No changes to Windows Python files.
