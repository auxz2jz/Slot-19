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
