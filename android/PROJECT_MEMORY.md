# Android Project Memory — Video 3D Capture Lab

Updated: 2026-10-09 UTC, using 2026-10-08 PDT user feedback.

## Ownership and governing rules
Android owns `android/` and `.github/workflows/android-build.yml` only, with platform-neutral shared decisions coordinated through `shared/`. Windows/Codex owns root Python `videogrammetry/`, `tests/`, historical Windows records. Read root `AGENTS.md` and mandatory files in `auxz2jz/master-instruction-library`. Never overwrite a verified baseline, never treat a CI success as a physical device verification. `auxz2jz/Slot-8` remains untouched reference.

## User-verified capture baseline
- **Android v0.2.0**, only as to the 3 modes capturing/displaying frames as reported by the user; **no 3D reconstruction verified**.
- Source: `21cc221cdd4421e3ee17c8167b970486cb38f365`. Backup `backup/android-v0.2.0-user-verified-capture`; APK SHA256 `320c78731c15567e377db30f1ccd6b6db223eb25611230c94e553842f37ec785`. CI run `37894898716`.
- User reported all three capture modes work and frame preview updates during processing. Uploaded v0.2.0 diagnostic ZIP has recorded-video 40/40 saved frames at 1 FPS, file validation PASS, manual PASS, zero error events. This ZIP contains **one** recorded-video run, not the complete history.
- Historical Android v0.1.0 capture-only baseline is retained on its dedicated backup branch. No Windows modification.

## Latest Android candidate: v0.3.0 (USER-UNVERIFIED)
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

## Exact next action
Build verification DONE; user-device verification PENDING. User tests slider effects using identical video at 0.5,1,2,3 FPS and checks useful viewpoints/overlap, then exports the ALL Runs ZIP. Use evidence to tune capture rate and later add actual feature matching; do not mark v0.3 verified until user confirms. Windows not affected.


## Latest v0.3.0 on-device evidence (ZIPs received; capture validation PASS, human verification pending)

The user uploaded both latest-run and **ALL Runs + FPS Comparison** v0.3.0 ZIPs. Both passed ZIP integrity. All-run summary includes **9 completed independent run IDs, 549 saved JPEGs, 267,721,254 saved JPEG bytes**, across live camera (3), recorded video (4), smart auto (2). **Every run reports validated PASS; no error events.** Last-run ZIP is the final 30-photo smart run and matches the all-run manifest/result. All-run ZIP is now confirmed to actually include previous runs.

Observed Live 1 FPS -> 30 at ~0.98; Live 2 FPS -> 50 at ~1.95; Smart 0.5 shutter cap -> 30 at ~0.44; Smart 1 shutter cap -> 30 at ~0.73; imported video 1 FPS -> 40; 3 FPS -> 40 and 152; 5 FPS -> 170. JPEG dimensions: Live 1088×1088, Smart 4000×3000, imported video 720×1280. Uploaded reports contain NO manually confirmed v0.3 guided-test PASS and no actual JPEG payloads.

**Important semantic limitation:** recorded-video `measuredFps` is calculated from requested source timestamps, so exact 1/3/5 values do not prove decoding capacity/actual presentation times. `processingElapsedMs` indicates throughput around 4–5 saved JPEGs/sec for these imports. For first 33.8 sec of the recording, the near-duplicate appearance proxy was 0/33 at 1 FPS, approximately 6/101 at 3 FPS, and 59/169 at 5 FPS. These are NOT actual duplicate files and NOT proven geometric overlap. Distinct full-run temporal coverage prevents naive global comparisons. **Recommendation: 1 FPS default and test 3 FPS if extra viewpoints useful; 5 FPS shows many more near-similar adjacent frames on these captures.**

See `android/CHECKPOINT.md` for all exact run IDs, ZIP hashes, observed metrics, caveats and next steps. **v0.3.0 remains USER-UNVERIFIED pending explicit user visual confirmation**. Preserve v0.2.0 last user-verified capture-only baseline and v0.3.0 candidate. Future focus: accurate diagnostics labeling, actual feature matching/overlap, selectable frame set review and quality-aware selection before claiming best reconstruction rate. Android docs only; Windows project untouched.
