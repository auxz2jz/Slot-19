# Android-only Roadmap

## A0 — v0.1.0 capture candidate
- [ ] Build Kotlin + Compose debug APK in Android-only GitHub Actions workflow.
- [ ] Live CameraX preview and saved frame samples (limited count; no background recording).
- [ ] Android system document-picker video import and bounded sampling via MediaMetadataRetriever.
- [ ] Separate non-overwriting capture runs, validated JPEG files, frame manifest and structured diagnostic traces.
- [ ] User-facing `Test This Version`, manual failure and PASS only with validated outputs plus explicit visual confirmation.
- [ ] Export redacted test + diagnostics ZIP through Android Storage Access Framework.
- [ ] Unit/build checks in GitHub CI; candidate status until user physically confirms.

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
