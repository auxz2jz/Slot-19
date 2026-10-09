# Android Project Memory — Video 3D Capture Lab

Updated: 2026-10-09 UTC, using 2026-10-08 PDT user feedback.

## Ownership and governing rules
Android owns `android/` and `.github/workflows/android-build.yml` only, with platform-neutral shared decisions coordinated through `shared/`. Windows/Codex owns root Python `videogrammetry/`, `tests/`, historical Windows records. Read root `AGENTS.md` and mandatory files in `auxz2jz/master-instruction-library`. Never overwrite a verified baseline, never treat a CI success as a physical device verification. `auxz2jz/Slot-8` remains untouched reference.

## User-verified capture baseline — v0.3.0
The user confirmed that all three modes, image previews, and adjustable frame-rate controls worked. **Android v0.3.0 is last USER-VERIFIED for capture/settings/export only.** No 3D reconstruction, true pose or geometric quality is verified.
- Exact source: `8c61027d3eb83efcfa1bf8d4df4caf8631f2b262`.
- Backup: `backup/android-v0.3.0-user-verified-capture`.
- GitHub Actions run `37901552835` PASS; APK SHA256 `51f5629305332b67a763f9007332fe96fc8fe285d1ab0b892c4cbafc87d01938`.
- User shared nine successful device runs with validated 549 saved frames across all modes. Per-run logs and complete observations archived at `android/history/ANDROID_V0.3.0_FPS_DEVICE_REPORT.md`.
- Earlier user-verified v0.2.0 and v0.1.0 source snapshots remain preserved.

## Historical v0.3.0 feature recap
- PR #1 merged at source commit `8c61027d3eb83efcfa1bf8d4df4caf8631f2b262`, Android v0.3.0 versionCode 3.
- `CaptureOptions.kt`: mode-specific requested FPS and max frames; ranges 0.5..5.0 FPS step 0.5 and 10..300 frames step 10, separate persisted mode preferences. Default Live 1 FPS/30, Video 1 FPS/40, Smart capped at 0.5 full-res photos/sec/30. Smart is a quality/novelty selector, never forced scheduled video sampling.
- `ScanRepository.kt`: per-run settings, frame times/count, estimated measured FPS from saved source timestamps, elapsed compute, brightness/edge and adjacent-duplicate pixel proxies, and `exportAllRunDiagnostics` that includes **every** completed run's manifest/result/events/test without private frames/videos.
- MainActivity UI: `Live/Video/Smart` setting chips, rate and maximum sliders with honest recommendation text, `Export ALL Runs + FPS Comparison`. Original capture buttons unchanged.
- GitHub Actions `37901552835` **SUCCESS** (JVM unit tests + debug APK). Artifact ID `11603001558`, name `video3d-android-v0.3.0-candidate`, APK 11,566,873 bytes, SHA256 `51f5629305332b67a763f9007332fe96fc8fe285d1ab0b892c4cbafc87d01938`. Tested source preserved on `backup/android-v0.3.0-ci-candidate`. New pure JVM CaptureOptionsTest and existing selector/frame tests included.
- Read `android/CAPTURE_RATE_TEST_PLAN.md` and `android/CHECKPOINT.md` for experiment and exact next step.

## Distinctions, known limitations
- Old behavior: each run saved independently in app-private folders; the old ZIP exporter exported only latest run. Rolling global actions log rotates at about 2 MB and does not replace per-run files. Clearing app data/uninstall may remove runs.
- `measuredFps` measures actual selected spacing in **source time**, not software decoding speed. Increasing requested rate may not yield more captured frames on device. `sharpnessProxy`/nearDuplicate count are rough appearance metrics, not verified overlap or 3D model quality. Same source clip and matched temporal window recommended for fair tests.
- Diagnostic exporter records `EXPORT_RESULT` after ZIP creation; same ZIP may lack its own completion event. No image bytes in ZIP; device visual inspection still necessary.
- This remains capture-only: no point cloud, camera pose reconstruction, meshing, texture or laser.

## Current development: Android v0.4.0 (NOT USER-VERIFIED)
- Opt-in **Analyze Latest Run — ORB Geometry** added in Android-only PR #2, merged source `4503a4cf9c40a8709fd13b009a766901545aff40`. Original OpenCV CI run `37905706664` SUCCESS; optimized ARM64/universal CI run `37906142981` SUCCESS. Exact latest tested source `36323ccbda8d6dbf7efce929c14ae909233cee33`, branch `backup/android-v0.4.0-arm64-ci-candidate`. GitHub artifact `video3d-android-v0.4.0-candidate` ID `11603669766`; ARM64 APK SHA256 `7f9aca331b78f56595f799971106c2e23a09b212f0ece565846fd40d3ebfb1cd`, universal SHA256 `156d697bdfb1a8f8eac5ee1e061f4dead356f8a5bb861d5386482077d3fce327`. No device OpenCV run verified.
- OpenCV official Android 4.12 dependency, ORB keypoints/BRIEF descriptors, Hamming nearest-neighbor 0.75 ratio and fundamental-matrix RANSAC per pair of saved frames. Max ~80 pairs/run, JPEGs downscaled only for analysis, capture images unchanged, run scope includes prior v0.3 frames if app data persists.
- `geometry_report.json` and `geometry_pairs.jsonl` exported in both single-run and ALL Runs ZIP; no raw images. Results count epipolar-consistent vs weak pairs; **do not confuse this with object-only coverage, camera pose registration, or 3D construction**.
- New separate background executor so existing live/video/smart modes remain unaffected. On library load failure or weak/featureless images, report FAILED/INCONCLUSIVE diagnostics; do not override capture PASS.
- CI unit tests for pure verdict thresholds and sampling policy added. **No device OpenCV verification yet**.
- Continue: inspect CI, patch first compiler issue if present, preserve exact v0.4 candidate, ask user to exercise captured-frame ORB analysis on textured object and export diagnostics. If v0.4 fails, keep v0.3 baseline. Do not touch Windows/Slot-8.

See `android/CHECKPOINT.md`, `android/ORB_GEOMETRY_PLAN.md`, `android/DIAGNOSTICS_AND_TESTING.md`, `android/ROADMAP.md` for exact handoff.
