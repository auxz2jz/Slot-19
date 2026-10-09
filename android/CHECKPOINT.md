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
- New `CaptureOptionsTest.kt` verifies valid bounds and measured source FPS; `SmartFrameSelectorTest.kt` and legacy frame tests retained. CI run `37901552835` COMPLETED SUCCESS with Android JVM unit tests and debug APK compilation.
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

## v0.3.0 reproducible APK result

- Exact tested source commit: `8c61027d3eb83efcfa1bf8d4df4caf8631f2b262` (Android-only PR #1 merged). Candidate backup branch `backup/android-v0.3.0-ci-candidate` points to this source.
- GitHub Actions `37901552835`: **COMPLETED SUCCESS** (unit tests and Android debug build). Artifact `video3d-android-v0.3.0-candidate`, artifact ID `11603001558`.
- APK `Video3DCapture-Android-v0.3.0-CANDIDATE.apk`, 11,566,873 bytes. SHA256 `51f5629305332b67a763f9007332fe96fc8fe285d1ab0b892c4cbafc87d01938`. Retrieved ZIP checked for integrity and APK hash compared with the embedded `SHA256SUMS.txt`: PASS.
- v0.3.0 is **NOT USER-VERIFIED**. GitHub compile/JVM tests do not guarantee user-device CameraX FPS, decoding throughput or settings behavior. Historical v0.2.0 user-verified capture fallback preserved.
- CLI/source note: Android code in `android/`; pre-existing Windows Python files intentionally unaffected.
- User next: install v0.3.0; check all three modes and slider values; for one prerecorded video try 0.5, 1, 2, 3 FPS and enough frames to cover equal time; then use Export ALL Runs + FPS Comparison. Share ZIP and impressions. Missing exact device-independent `best` rate remains a test/reconstruction question, not an automatic conclusion.


## v0.3.0 user-submitted device diagnostics — 9-run comparison (received October 2026)

**Exact inputs:** `Android-v0.3.0-last-run-diagnostics.zip` (ZIP SHA256 `568e547bd3a3c5173278715fbd73f2dc5e45470461c324e59bc567fb3770edf3`); `Android-v0.3.0-ALL-run-comparison.zip` (ZIP SHA256 `a2b16e454229c100c200682c26a33e32bee8e212af1dcc64289a1bd6cbe18c75`). Both ZIPs passed integrity checks. All-run ZIP contains `all_runs_summary.json` and per-run manifest/result/events; 9 distinct completed runs, 549 saved frames, 267,721,254 bytes (approximately 267.72 decimal MB) of app-reported output JPEGs. These files are diagnostics only: **no original JPEG image bytes or video samples to independently verify real visual quality**.

**Mode breakdown:** three `live_camera` runs, four `recorded_video` runs, two `smart_auto` runs. All 9 `result.json` status PASS with `outputValidated=true`; zero `ERROR` diagnostic events. No `test_results.json` manual guided PASS is present for the new v0.3 runs: **v0.3 remains device-tested automatically PASS but not user-confirmed VERIFIED**. Last-run ZIP refers to final smart run `20261009T081550Z_a52f8cfd-4`, also present in all-run ZIP. Latest-run events contains 2 additional export events relative to earlier all-run snapshot, not a failed capture.

### Tested configurations and actual observations

| UTC-based run ID suffix | Mode | FPS requested | Frame cap | Saved | Rate measured by app | Pixel-proxy near duplicate pairs | Saved MB | Elapsed processing s |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| `43d069bd-b` | Live | 1 | 30 | 30 | 0.981 | 0 of 29 | 5.61 | 29.71 |
| `e65f249b-9` | Smart | 0.5 shutter cap | 30 | 30 | 0.437 accepted/s | not measured | 109.30 | 70.01 |
| `e58b860d-1` | Video | 1 | 40 | 40 | 1.000 nominal source spacing | 0 of 39 | 4.88 | 10.67 |
| `19e1b7df-1` | Video | 3 | 40 | 40 | 3.003 nominal | 1 of 39 | 4.81 | 8.36 |
| `d613cadf-3` | Video | 3 | 170 | 152 | 3.003 nominal | 9 of 151 | 18.05 | 30.63 |
| `57d2ab79-e` | Video | 5 | 170 | 170 | 5.000 nominal | 59 of 169 | 20.37 | 34.04 |
| `6a42f5c8-a` | Live | 1 | 30 | 7 (manual stop) | 0.986 | 0 of 6 | 1.48 | 6.33 |
| `b210e655-a` | Live | 2 | 50 | 50 | 1.946 | 0 of 49 | 10.21 | 25.35 |
| `a52f8cfd-4` | Smart | 1 shutter cap | 30 | 30 | 0.731 accepted/s | not measured | 93.02 | 40.38 |

Live JPEGs 1088x1088; recorded-video JPEGs 720x1280; smart CameraX still JPEGs 4000x3000. All saved image SHA values are unique within their runs; near-duplicate proxy means visually similar pixels, not byte-identical images. Smart auto decisions included READY, COOLDOWN, CAMERA_MOVING, and one TOO_DARK / two SAME_VIEW events in the 0.5 max shutter run, no shutter errors.

### Comparison caveats and evidence-based recommendations
- Important diagnostics semantics bug/gap: for imported video `measuredFps` is derived from **requested `sourceTimeMs` values**, not independently measured decoded presentation times; 3.003/5.000 values are nominal requested spacing by construction, **not evidence that the decoder can decode frames that quickly**. `processingElapsedMs` and frameCount allow observing actual throughput separately (roughly 4–5 JPEGs/second on these video imports). In a future version, label this clearly as requested sampling cadence and report actual throughput and optical near-duplicate rate separately.
- Comparing video rates with a common first 33.8 seconds: 1 FPS had 0/33 adjacent near-duplicate-proxy pairs, 3 FPS (longer run) had approximately 6/101 (~5.9%), 5 FPS had 59/169 (~34.9%). Even these are rough small-thumbnail differences, NOT RANSAC verified geometric correspondence. **5 FPS produces many more visually-similar adjacent images than 3 FPS in this footage**, but 3 FPS is not yet proven to give best reconstruction quality.
- Unequal frame caps/time windows invalidate naive full-run comparisons: 3 FPS at cap 40 covers 12.987s; 3 FPS at 170 cap covers 50.283s; 5 FPS at cap 170 covers 33.800s; 1 FPS at cap 40 covers 39s. Match source-duration windows in future tests.
- Recommend **1 FPS as economical default** on this footage; **3 FPS as higher-coverage experiment**; 5 FPS is an experimental upper bound not automatically beneficial; Smart 0.5–1 max shutter FPS depends on actual view changes and is not directly comparable.
- Large Smart full-resolution JPEGs: 30 images occupied ~93–109 MB; a maximum of 300 could consume ~1 GB if similarly sized. Retain cap warning.
- No saved source-video filename/hash or independent decoded timestamp; source identity cannot be fully proven from report alone (overlapping frame hashes suggest some tests reuse the same recording). Compare the same original video with equal temporal coverage whenever possible.
- Neither JPEG validity nor visual-change proxies validates useful 3D features, overlap, pose, or resulting 3D mesh. Plan feature matching/geometric verification before implementing an automatic **best FPS** recommendation.

**Verified baseline decision:** no promotion without user's explicit v0.3 visual confirmation. Preserve verified v0.2.0 source and v0.3 candidate build separately. Next: ask user if higher-rate preview/photos look good; if yes, record v0.3 as verified for capture/settings/export scope only. Then prioritize fixing FPS diagnostic labeling, smarter per-frame preview/selection and true feature-based overlap comparisons. No Windows source modification authorized or needed.
