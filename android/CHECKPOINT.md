# Android recovery checkpoint — v0.3.0 adjustable FPS and cross-run diagnostics

Updated 2026-10-09 UTC (2026-10-08 PDT user session). **Platform: Android only.**

## Verified capture baseline (preserve)
- **LAST USER-VERIFIED CAPTURE BASELINE: Android v0.2.0**, limited to the three capture modes and visible saved-frame behavior, as explicitly reported by user. This is NOT verified photogrammetry reconstruction, accurate image overlap, or validated automatic keyframe quality.
- User statement: *“I tested all three methods ... frames change when they're being analyzed ... everything looks fine.”*
- User-uploaded `Android-v0.2.0-test-diagnostics.zip` recorded-video run `20261009T074031Z_c87a7f9c-2`: 40/40 frames, 720×1280, requested 0–39s (1 FPS), app validation PASS, manual `FRAMES_LOOK_CORRECT` PASS, zero errors; valid diagnostic ZIP. It contains only the most recent run.
- Source commit `21cc221cdd4421e3ee17c8167b970486cb38f365`; backup branch `backup/android-v0.2.0-user-verified-capture` and `backup/android-v0.2.0-smart-ci-candidate`.
- CI for verified version: run `37894898716` SUCCESS. APK SHA256 `320c78731c15567e377db30f1ccd6b6db223eb25611230c94e553842f37ec785`. Older v0.1.0 baseline source retained separately.
- Exact Windows historical code, root checkpoint, and `auxz2jz/Slot-8` have not been modified by this feature.

## New Android candidate v0.3.0 (NOT USER-VERIFIED)
- Source merge commit `8c61027d3eb83efcfa1bf8d4df4caf8631f2b262` (PR #1), `versionCode=3`, `versionName=0.3.0`.
- Three independent, persistent mode-specific settings accessible via `Live`, `Video`, `Smart` chips. FPS slider 0.5–5.0 in 0.5 increments; total frames slider 10–300 in 10 increments. Default Live 1 FPS/30; Video 1 FPS/40; Smart cap 0.5 photos/sec/30. Smart uses the rate only as a minimum time between eligible full-resolution photos, still checking quality and novelty.
- Existing `Start Live Sampling`, `Choose Video` and `Start Smart Auto Capture` retained. Video extraction updates displayed frame periodically. A new `Export ALL Runs + FPS Comparison` exports all completed runs' metadata/results/events/tests into one ZIP, no source videos/photos.
- Each new run records requested FPS/max frames, actual sampled interval from saved source times, output file count/bytes, elapsed processing, mean thumbnail sharpness/brightness proxy where available, estimated adjacent duplicates in fixed-interval modes, and smart image-change/proxy metrics. **These are not proof of geometric reconstruction quality; no ORB pose/point clouds/mesh yet.**
- Global event trace is bounded/rotating; individual per-run reports persist separately in app-private storage. A normal `Export Test + Diagnostics` creates a ZIP for the latest run ONLY. `Export ALL Runs` produces cross-run report collection. Uninstalling app may delete runs.
- Comparison recommended: reprocess one identical 30-second recording at 0.5 / 1 / 2 / 3 FPS with maxFrames sufficient for equal temporal coverage (e.g., 100 for all). Measure spacing, images, blur/edge proxy, duplicates, bytes and processing time. Higher FPS is not automatically better.

## Diagnostic/test and known limitations
- New `CaptureOptionsTest.kt` verifies valid bounds and measured source FPS; `SmartFrameSelectorTest.kt` and legacy frame tests retained. Android GitHub Actions build run `37901552835` initiated by merge; status must be checked, do not infer PASS before it completes.
- Actual camera frame rate and Android MediaMetadataRetriever seek accuracy depend on hardware/decoder. Requested FPS is a target; measured FPS reflects **selected source timestamps**, not importer compute speed.
- Smart full-res photos may use significant storage; 300 is a safety cap, not a claim that 300 are optimal. The smart heuristic does not reconstruct camera poses; exact panorama angle/overlap guidance is deferred.
- Diagnostic ZIP export may lack its own post-close `EXPORT_RESULT` event; a known observability issue, not necessarily export failure.
- `app/src/main/java/.../ScanRepository.kt` logs per-run data but main screen only displays the latest frame and latest single-run report. All-run ZIP enables comparison without deleting old runs.
- The submitted ZIP contains no photos, so true camera framing/sharpness of those source images cannot be independently inspected here.

## Exact next action
1. Confirm new v0.3.0 GitHub Actions completed with Android compile, JVM tests and APK artifact; inspect failures and fix only targeted causes if any.
2. Record exact compiled source commit, artifact hash, backup candidate branch and ensure Windows code unaffected.
3. User installs APK, tests all 3 modes plus FPS sliders on video/live and smart maximum rate, verifies saved count and saved-frame display.
4. User exports **ALL Runs + FPS Comparison** and reports visual results at different rates, including photo quality.
5. Only after explicit user confirmation promote v0.3.0 to user-verified capture baseline. Preserve verified v0.2.0 fallback.
