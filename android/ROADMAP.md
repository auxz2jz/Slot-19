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
