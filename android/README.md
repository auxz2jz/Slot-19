# Android Video 3D Capture Lab — v0.3.0 CANDIDATE

Native Kotlin/Compose camera capture application in `auxz2jz/Slot-19/android/`. Windows program separately owned; read `../windows/OWNERSHIP.md` and `../shared/`. This Android app is a **frame/photo capture research tool**: it does **not** yet compute camera poses, point clouds, meshes, textures, or laser geometry.

## Tested versions and release identities
**Last USER-VERIFIED Android CAPTURE baseline: v0.2.0**, based on user reporting all three modes work and visible extracted frames look correct. Exact source `21cc221cdd4421e3ee17c8167b970486cb38f365`, backup `backup/android-v0.2.0-user-verified-capture`, original APK SHA256 `320c78731c15567e377db30f1ccd6b6db223eb25611230c94e553842f37ec785`. No 3D model/true overlap verification.

**Latest CANDIDATE: v0.3.0**, CI test and APK build PASSED, **physical device testing pending**.
- GitHub Actions [run 37901552835](https://github.com/auxz2jz/Slot-19/actions/runs/37901552835), artifact `video3d-android-v0.3.0-candidate` (artifact ID 11603001558).
- APK: `Video3DCapture-Android-v0.3.0-CANDIDATE.apk`, 11,566,873 bytes.
- APK SHA256: `51f5629305332b67a763f9007332fe96fc8fe285d1ab0b892c4cbafc87d01938`.
- Exact built source commit `8c61027d3eb83efcfa1bf8d4df4caf8631f2b262`, preserved backup `backup/android-v0.3.0-ci-candidate`.

GitHub debug APK uses an ephemeral signing key. If updating the existing app fails because it was built with a different key, **export important diagnostics before uninstalling** (app-private runs may be deleted on uninstall). If desired, keep the verified 0.2 APK as fallback. Android 8/API 26 or newer required.

## Capture and recommended presets

Your three original capture modes remain:
- **Start Live Sampling**: save regularly sampled camera frames; recommended **1 FPS** while moving phone slowly around a stationary object. Adjustable from 0.5–5 requested FPS. Default maximum 30 frames.
- **Choose Video**: select an existing recording, extract source frames at selected requested rate; recommended **1 FPS** to start, compare **0.5, 1, 2, and 3 FPS** using the *same video*. Default maximum 40 frames; this may capture only the start of longer videos.
- **Start Smart Auto Capture**: analyze live camera for image sharpness, lighting, stability and changed viewpoints; request full-resolution still photos when accepted. Recommended **at most 0.5 photos/sec** to allow moving between views. Its FPS slider sets the **maximum allowed shutter rate**, NOT the exact number of photos taken; no true pose/angle measurement is available yet.

At top of screen, select the **Live**, **Video**, or **Smart** settings chip. Adjust:
- FPS slider: **0.5–5.0** steps of 0.5;
- Maximum saved frames slider: **10–300** steps of 10.

Each mode keeps its own settings across app sessions. Slides/settings apply to the named mode, even if a different capture button is pressed. Requested FPS is a target; actual capture/decoder speed may be lower. High FPS or many photos can consume considerable battery, RAM, time and storage.

## How run diagnostics work

- Every capture/import creates a new **run ID** with its own `manifest.json`, `result.json`, `events.jsonl`, actual saved JPEG files, and optional `test_results.json`.
- New runs **do not overwrite** older runs. These are stored within app-private space; uninstall or clearing app data may delete them.
- `Export Test + Diagnostics` exports **only the latest run** (no raw frames/video).
- **Export ALL Runs + FPS Comparison** exports the metadata/logs for **all completed runs**, plus `all_runs_summary.json`; the ZIP excludes camera photos and private source videos.
- Each new v0.3 run records requested FPS, the measured spacing of accepted frames in their source timeline, actual frame count, processing time, saved bytes, and simple brightness/sharpness/near-duplicate appearance proxies where available. This is useful for debugging and controlled comparison but does not prove which settings will produce the best 3D model.
- The rolling global action log rotates when large; per-run logs remain separate until app-data removal.
- Known diagnostic limitation: a ZIP may not contain its own final `EXPORT_RESULT` because that event is appended after ZIP completion.

## Suggested guided test

1. Verify all three capture buttons still work after installing v0.3.0. Check saved frames and preview.
2. Select **Video** settings, adjust to **1 FPS** with **100 max frames**, import a short ~30-second recorded clip. Run the same source again at **2 FPS**, then **3 FPS**, keeping 100-frame cap and identical source; compare saved counts, preview and any blur, and output diagnostics. Also try **0.5 FPS** if desired.
3. Try **Live** at 1 and 2 FPS; the observed accepted/saved rate may differ due to camera, JPEG encoding and device performance.
4. Try **Smart** with default 0.5 max shutter FPS; hold the phone steady then change to a new overlapping angle. Examine full-resolution photo quality. Increasing its cap doesn't override the novel-view gate.
5. Select **Test This Version** and record honest manual PASS or **Expected Behavior Failed**.
6. Export **ALL Runs + FPS Comparison**, verify ZIP opens, and upload it back with your observations. This is the best evidence for comparing sampled rates.

See `CHECKPOINT.md`, `PROJECT_MEMORY.md`, `ROADMAP.md`, `CAPTURE_RATE_TEST_PLAN.md` and `DIAGNOSTICS_AND_TESTING.md` for development handoff. Windows/Codex must only touch its owned historical root Python source.


## New experimental v0.4.0 ORB feature matching

**User-verified capture fallback is Android v0.3.0.** Exact source SHA `8c61027d3eb83efcfa1bf8d4df4caf8631f2b262` and original APK SHA256 `51f5629305332b67a763f9007332fe96fc8fe285d1ab0b892c4cbafc87d01938` are preserved in `backup/android-v0.3.0-user-verified-capture`.

The Android v0.4.0 **candidate** adds an independent **Analyze Latest Run — ORB Geometry** action. After you successfully capture or import photos, tap that button. It detects actual ORB visual features in adjacent saved photos, tests Hamming descriptor correspondences with a Lowe-style ratio filter, then computes fundamental-matrix RANSAC inlier counts and an epipolar-consistency verdict. This is NOT a point cloud, mesh, proven camera pose, or measurement of percent object scanned. Flat backgrounds, nearly stationary camera and featureless surfaces may mislead it; weak geometry does not mean the camera failed to save photos.

**Device test:**
1. Complete a small 10–30-image Live, Video or Smart capture of a stationary object, moving the camera gradually and keeping the object prominent.
2. Tap **Analyze Latest Run — ORB Geometry**. Verify the progress and final supported-pair count; do not expect a finished 3D model.
3. Repeat with a static or featureless object; see whether it reports weak/inconclusive matches. A static image may still match well without useful triangulation.
4. Tap **Export Test + Diagnostics** or **Export ALL Runs + FPS Comparison**. New reports are `geometry_report.json` and `geometry_pairs.jsonl`; the ZIP includes metrics and logs only, not original photos.
5. Send the ZIP and your observed camera/photo behavior to the Android developer. Report any OpenCV loading error, UI freeze or unusual image framing. Re-test all three previously working capture methods.
6. V0.4.0 is not user-verified until you test and explicitly confirm it; return to preserved v0.3.0 source/APK if this candidate fails.

**Build note:** Official OpenCV 4.12 Android AAR adds native code; a universal debug APK is significantly larger than older releases. ARM64-specific build outputs are intended for modern ARM64 phones, with universal fallback. If updating a GitHub-signed debug APK requires uninstalling the previous version, export important data first; uninstall may delete app-private captures.

See `ORB_GEOMETRY_PLAN.md`, `CHECKPOINT.md` and `DIAGNOSTICS_AND_TESTING.md` for algorithms, source fingerprint and tests. **Windows implementation is unchanged.**
