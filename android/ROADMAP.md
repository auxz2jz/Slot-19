# Android-only Roadmap

## A0 — v0.1.0 capture candidate
- [x] Kotlin + Compose APK successfully built in Android-only GitHub Actions (run 37891374599, artifact id 11597604313; not user verified).
- [x] Live CameraX preview/analysis frame sampling implemented; up to 30 frames. **User camera test pending.**
- [x] Android document-picker video import and MediaMetadataRetriever sampling implemented; up to 40 frames. **User video test pending.**
- [x] Non-overwriting capture runs, saved JPEG validation (hash, dimensions, count), manifest, run result and structured event trace implemented.
- [x] User-facing Test This Version, manual failure and result export implemented (no on-device test yet).
- [x] Redacted test + diagnostics ZIP via Storage Access Framework implemented (on-device export verification pending).
- [x] Android unit tests and debug assemble SUCCESS in GitHub CI; source/build are **CANDIDATE, NOT VERIFIED**.

### Device verification still required before baseline
- [ ] User installs v0.1.0 and confirms camera permission and live preview.
- [x] User generated live run with 30 validated frames and in-app **MANUAL_PASS**. Preview framing/crop and actual images are not included in diagnostics; further visual details unconfirmed.
- [x] User selected a real recorded video, app decoded and validated 40/40 files with no missing frames. **Saved-image appearance not manually graded in this export.**
- [x] User completed guided PASS for live run and uploaded two structurally valid redacted diagnostic ZIPs. Recorded-video run has no guided manual result.
- [ ] Obtain recorded-video visual confirmation and whether square live frames have unwanted cropping; only then consider an overall user-verified baseline.

### Evidence-driven diagnostics follow-up
- [ ] Future candidate: include authoritative diagnostic export-completion evidence in a subsequent export/acknowledgment (current ZIPs capture export start but not post-close completion).
- [ ] Review live 1088×1088 output framing for possible unwanted cropping; avoid guessing from metadata alone.

## A1 — quality and usability
- [ ] Proven/tested live stream frame-orientation and exposure handling.
- [ ] Manual full-resolution CameraX ImageCapture; selected photo import.
- [ ] Frame sharpness, overlap, movement, duplicate rejection and visible guidance.
- [ ] Bounded storage/cleanup without deleting verified runs accidentally.

## A2 — photogrammetry geometry
- [ ] Initial ORB baseline, geometric verification, pose estimation, sparse reconstruction with diagnostics.
- [ ] Compare SIFT/AKAZE/learned models only when available and measured on device.
- [ ] Bundle refinement, dense geometry, mesh, texture, later Android-appropriate engines.

## A3 — optional advanced scanning
- [ ] Laser-line Still/Test with camera/laser-plane calibration separated, ON/OFF subtraction and structured stripe detection.
- [ ] Live laser scanning after still mode verified; hybrid fusion only after both geometry sources trustworthy.

Each Android baseline, build, test, and checkpoint remains platform-specific. No Android milestone can advance Windows statuses.

## New milestone v0.2.0 — Smart Auto Capture (experimental APK built)

- [x] **v0.1.0 last user-verified capture baseline:** live run (30 frames, manual visual PASS), imported video (40 frames, user said final image looked correct); preserve source in `backup/android-v0.1.0-user-verified-capture`.
- [x] Third distinct control **Start Smart Auto Capture / Stop Smart Auto Capture**, retaining original live/video capture methods.
- [x] Lightweight thumbnail-based sharpness/exposure/stability/view-change heuristics; status guidance and approximate progress bar. This is NOT tracked rotation, actual spatial overlap, or percent completion of the scan.
- [x] Optional CameraX ImageCapture full-res JPEG output, one photo in flight, max 30 photos, record validated JPEG dimensions/bytes/hash and each selected photo's scores.
- [x] Display saved frames progressively (every few frames) during recorded-video import.
- [x] Add pure-JVM tests for selector acceptance/rejection, motion, repeated views, cooldown and brightness compensation. GitHub Actions `37894898716` built APK and ran unit tests successfully.
- [x] Preserve exact v0.2.0 candidate source in `backup/android-v0.2.0-smart-ci-candidate`, SHA256 of APK recorded in `CHECKPOINT.md`.
- [ ] Physically verify smart CameraX ImageCapture can bind alongside existing preview and analysis on Samsung Galaxy S22 Ultra, without regressing either v0.1.0 mode.
- [ ] Test near-identical static view rejection, move + hold auto shutter, poor-light and motion warnings, saved full-res photo dimensions/orientation and readable diagnostics.
- [ ] Compare smart selected views versus standard fixed-interval capture on an actual object; tune thresholds using the first returned diagnostic evidence.
- [ ] User confirms v0.2.0, then and only then designate it the Android user-verified baseline.

## Longer term panorama-style guidance
- [ ] Investigate ORB/AKAZE/SIFT feature matching and geometric verification for meaningful camera/coverage estimates; real overlap cannot be inferred safely from simple brightness differences.
- [ ] Only after actual reliable pose estimation consider directional or angular guide / scene coverage map. Avoid inventing metric movement or exact angle.

## v0.3.0 — Configurable capture FPS, frame count and diagnostics (CI-built, user-unverified)

- [x] User confirmed Android v0.2.0 core capture/display behavior for ALL 3 capture modes. Preserved tested source commit on `backup/android-v0.2.0-user-verified-capture`; this does not verify 3D reconstruction or scientifically valid Smart frame quality.
- [x] Mode-specific persistent `Live`/`Video`/`Smart` requested FPS slider 0.5–5 FPS, 0.5 increments; max frames 10–300 in steps of 10. Smart is a **maximum full-res shutter rate**, only after heuristic viewpoint checks.
- [x] Runtime run manifests/results preserve selected FPS, requested frame count, saved source timestamps, actual accepted/sampled FPS, runtime, saved bytes, brightness/edge proxy and adjacent near-duplicate proxy.
- [x] `Export ALL Runs + FPS Comparison` saves every completed run's reports + combined summary in one ZIP, no original photo/video content; single-run export remains.
- [x] Requested vs measured FPS and diagnostic caveats explained onscreen; recommended start with 1 fps and compare 0.5/1/2/3 fps with equal temporal coverage.
- [x] New JVM unit tests plus existing test suite built in GitHub Actions run `37901552835`, SUCCESS; APK `Video3DCapture-Android-v0.3.0-CANDIDATE.apk`, SHA256 `51f5629305332b67a763f9007332fe96fc8fe285d1ab0b892c4cbafc87d01938`. Source on `backup/android-v0.3.0-ci-candidate`.
- [ ] User tests all mode presets/sliders including higher video FPS and larger frame count. Confirm achieved frames and file quality, test export all run histories, share diagnostic ZIP. Only then promote v0.3 to verified baseline.
- [ ] Future: analytical quality comparison using actual multi-view feature correspondence and camera registration; pixel-change heuristics alone cannot determine best reconstruction FPS.
- [ ] Later: manage/archive/delete selected old run folders, with explicit confirmation, and optional output image/contact-sheet export; no implicit deletions.



## v0.3.0 — real-device diagnostic evaluation

- [x] Received single-run and all-run ZIPs; both structurally valid, demonstrating aggregate export includes **9 independent runs** and correct latest-run metadata.
- [x] App reports **549 real saved JPEG file records**, all 9 capture runs with validated PASS and zero logged ERROR events. Three live, four video, two smart runs.
- [x] Device exercised FPS settings across live 1/2, video 1/3/5, smart 0.5/1, including video frame cap 170 and live frame cap 50.
- [x] Observed live measured rates ~0.98 and ~1.95; smart accepted rates ~0.44 and ~0.73 full-res photos/s.
- [x] Captured video adjacent near-duplicate *appearance proxy* about 0%, 5.9% and 34.9% for 1/3/5 FPS respectively over common first 33.8 seconds; different run caps affect total temporal coverage.
- [ ] **Request explicit v0.3 user visual confirmation** before upgrading last user-verified capture-only baseline. New ZIPs include no `test_results.json` manual verdict or actual JPEG pixel bytes.
- [ ] Correct diagnostic semantics: video `measuredFps` currently derives from *requested* sample timestamps and can falsely appear like achieved device decoder FPS; add truthful source/sample-vs-wall-clock throughput and decoded timestamp provenance if supported.
- [ ] Add more reliable per-frame image similarity/blur evaluation and cross-run source fingerprinting. Keep the simple pixel proxy separate from verified SfM overlap/pose.
- [ ] Consider storage management with explicit confirmation; 30 smart 12MP JPEGs occupied ~93–109MB in tests and 300 could be near 1GB.
- [ ] After v0.3 verification, start feature matching/registration evaluation on stationary-object handheld motion. Compare true registration coverage/geometry before making automatic “optimal FPS” assertions.

## Verified v0.3.0 capture milestone and experimental v0.4.0 geometry

- [x] **v0.3.0 LAST USER-VERIFIED ANDROID CAPTURE BASELINE:** user confirmed all three capture methods, displayed images, adjustable FPS controls worked. Tested source `8c61027d3eb83efcfa1bf8d4df4caf8631f2b262` preserved `backup/android-v0.3.0-user-verified-capture`. No 3D claim.
- [x] Preserve older v0.2.0 and v0.1.0 baselines, and unmodified Windows historical source.
- [x] Design optional per-run geometric diagnostics using `org.opencv:opencv:4.12.0`, rather than confusing pixel-change proxies with verified correspondences.
- [x] Add `Analyze Latest Run — ORB Geometry` after a completed run; do not block ongoing CameraX capture or modify saved JPGs.
- [x] Analyze at most ~80 pairs (including first/last), max edge 640 px, ~800 ORB keypoints, Hamming matcher ratio 0.75, fundamental-matrix RANSAC.
- [x] Save actual `featuresA/B`, `ratioTestMatches`, `fundamentalRansacInliers`, `inlierRatioOfMatches`, status and error details for every sampled neighboring pair.
- [x] Integrate `geometry_report.json`, `geometry_pairs.jsonl`, `geometry_last_failure.json` into both latest-run ZIP and all-runs ZIP.
- [x] Add JVM `GeometryPolicyTest.kt` validating sampling bounds and strict weak/consistent verdicts; GitHub run `37905706664` triggered for Android-only v0.4.0 candidate.
- [ ] Confirm GitHub OpenCV AAR resolution, Kotlin compile/tests, APK hash and archive; fix only actual source errors and preserve artifact snapshot.
- [ ] User tests ORB on multiple neighboring photographs with texture, featureless/similar views, and exports geometry ZIP; confirm 3 capture modes remain fully functional.
- [ ] Only user physical confirmation can upgrade v0.4.0 to VERIFIED.
- [ ] Future true camera pose: intrinsics from calibration/metadata, robust multi-view correspondences and SfM with triangulation/BA; diagnose low-parallax, planar/background degeneracy.
- [ ] Later compare actual registration success across 1/3/5 FPS before claiming an optimal capture rate; ORB epipolar pair support alone does not establish good 3D coverage.

