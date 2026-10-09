# Roadmap

All entries are PLANNED until implemented and tested. An implemented/CI-tested candidate is **not** USER VERIFIED.

## Milestone 0 — Initialization
- [x] Read Master Library mandatory files.
- [x] Verify Slot-19 was actually empty before writing.
- [x] Define purpose, ownership, approach, repository memory, roadmap, diagnostics and testing intentions.
- [ ] User review of initial platform/workflow assumptions (if changed, update plan without losing checkpoint).

## Milestone v0.1.0 — Acquisition foundation
- [ ] Accept a recorded video or a supported live camera/RTSP/HTTP source.
- [x] Extract regularly spaced frames (initially fixed interval) into a new non-overwriting project/run directory (recorded-video path CI-tested; live source device test pending).
- [ ] Preserve original input; capture provenance, source type, frame count, settings, duration if available and tool versions.
- [x] Validate actual output PNG images, hashes/count/dimensions and save `frames_manifest.json` (CI-tested).
- [x] Structured persistent diagnostics, error/timeout/cancellation reporting, bounded rolling action trace and `export-diagnostics` command (CI-tested for export and error cases; physical device tests pending).
- [x] CLI `test-this-version` guide with actual output validation, manual failure answer, test report and correlated diagnostic events (CI-tested; later GUI pending).
- [x] Python source compile and 6 targeted unit/integration tests passed in GitHub Actions run 37889805717; v0.1.0 **SOURCE CANDIDATE** committed, NOT a Windows EXE/APK.
- [ ] User physically tests and confirms before marking VERIFIED.

### v0.1.0 remaining / unverified
- [ ] Physically test recorded-video and live webcam/phone stream on the actual Windows setup.
- [ ] Ensure program captures full input provenance safely and check actual camera-specific FFmpeg compatibility.
- [ ] Add a friendly graphical interface after user confirms acquisition flow, if desired.

## Milestone v0.2.x — Quality-aware video frames
- [ ] Sharpness/blur, motion, exposure and overlap metrics; selection per camera mode and rejection reasons.
- [ ] Live preview with accepted/rejected frame feedback; avoid duplicate frames and excessive log frequency.
- [ ] Camera calibration/profile association, orientation and lens/intrinsics consistency.
- [ ] Input image import and ability to review/select/delete-from-candidate (without modifying originals).
- [ ] Low-resource operation and bounded capture/storage.

## Milestone v0.3.x — First actual photogrammetry pipeline
- [ ] Detect executable/version/capabilities for **COLMAP**; diagnose missing dependencies.
- [ ] Controlled isolated COLMAP run on selected frames; monitor command, phases, failures, and actual sparse output.
- [ ] Verify registered camera count, point counts, reprojection quality where exposed.
- [ ] Expose CPU/GPU capability and distinguish unavailable CUDA-dependent stages.

## Milestone v0.4.x — Comparative and modular pipeline support
- [ ] OpenMVG with OpenMVS conversion/dense/mesh/texture.
- [ ] AliceVision or Meshroom pipeline using verified installation/interface.
- [ ] MicMac command-driven pipeline with reproducible presets.
- [ ] OpenDroneMap optional workflow with explicit close-range limitations.
- [ ] Add engines one by one, supported versions and accurate UNAVAILABLE / READY / RUNNING / FAILED / VERIFIED_OUTPUT / NOT_TESTED statuses.
- [ ] Side-by-side runs on same frame manifest, separate workspaces and metrics; manual choice/fallback.

## Milestone v0.5.x+ — Meshing, visualization and polish
- [ ] Validated point cloud / mesh / texture interchange; viewer and export.
- [ ] Optional Manifold, Assimp, trimesh, meshoptimizer, MeshLab adapters as needed.
- [ ] Recovery/cancel/retry, benchmarking CPU/GPU/RAM, run comparison and user grading.
- [ ] Future Android capture/UI consideration ONLY when requested or justified; if added, enforce platform isolation.
