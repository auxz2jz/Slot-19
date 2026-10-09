# Android Video 3D Capture Lab — v0.5.0 two-view sparse CANDIDATE (v0.4.0 capture/ORB verified)

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

### Verified v0.4.0 CI artifact links / identity

- ARM64 phone APK: `Video3DCapture-Android-v0.4.0-ARM64-CANDIDATE.apk`, 36,404,569 bytes, SHA256 `7f9aca331b78f56595f799971106c2e23a09b212f0ece565846fd40d3ebfb1cd`.
- Universal fallback: `Video3DCapture-Android-v0.4.0-UNIVERSAL-CANDIDATE.apk`, 153,240,179 bytes, SHA256 `156d697bdfb1a8f8eac5ee1e061f4dead356f8a5bb861d5386482077d3fce327`.
- CI SUCCESS: https://github.com/auxz2jz/Slot-19/actions/runs/37906142981 — artifact `video3d-android-v0.4.0-candidate`, ID `11603669766`. Exact built source `36323ccbda8d6dbf7efce929c14ae909233cee33`, saved `backup/android-v0.4.0-arm64-ci-candidate`.
- Both artifact ZIP and generated `SHA256SUMS.txt` passed verification. Device OpenCV-native functionality remains **USER TEST PENDING**; verified v0.3.0 capture version preserved.

## v0.5.0 — First experimental two-view sparse 3D point cloud

The user's last physical verification covered Android v0.4.0 capture and ORB feature analysis. Exact protected source `36323ccbda8d6dbf7efce929c14ae909233cee33` at `backup/android-v0.4.0-user-verified-orb`. This does not verify 3D reconstruction.

V0.5.0 keeps **all three original capture modes**, sample-rate controls, and **Analyze Latest Run — ORB Geometry** unchanged. The fourth opt-in step, **Analyze Sparse 3D — Two Views**, tries a small number of pairs from the latest completed run. It uses real feature matching, essential-matrix RANSAC, relative pose recovery, two-view triangulation, cheirality and reprojection/parallax rejection to create a tiny colored sparse PLY when enough evidence is available. It can correctly respond **INCONCLUSIVE** with no PLY when views have inadequate parallax, features or consistency.

**Accuracy limitations: camera calibration is estimated rather than measured, lens distortion is ignored, translation scale is unknown, only ONE pair reconstructed, background may dominate, and the result is not a full 3D scan or textured mesh.** Do not use coordinates as actual dimensions.

### Device test guide
1. Verify the pre-existing Live, Video and Smart capture modes still work; capture a well-lit detailed stationary object while moving your phone around it.
2. Optionally tap the old ORB analysis first (real 2D feature correspondence evidence). Tap **Analyze Sparse 3D — Two Views** after a valid run with 10–30+ photos. Wait for the result.
3. On success, tap **Export Sparse PLY — Experimental** and pick a destination through Android's document picker. Open the PLY in any point-cloud viewer; assess whether the rough shape seems plausible, knowing orientation/size will be arbitrary. No PLY is enabled for an INCONCLUSIVE analysis.
4. Tap **Export Test + Diagnostics** or **Export ALL Runs + FPS Comparison**. New `sparse_report.json` (and optional `sparse_last_failure.json`) is included; private images and PLY bytes are excluded from the redacted ZIP by default.
5. Try an almost stationary view or featureless subject; a high ORB matching score alone does not imply real 3D baseline. Share diagnostics and any description/screenshots of an exported PLY, including whether it is clearly unrelated to the object.
6. Do not declare v0.5 VERIFIED until you personally test it. If installation requires uninstalling an earlier GitHub debug-signed APK, **export all run diagnostics and desired app data before uninstall**, which may erase app-private captures.

**Source/testing:** read `SPARSE_TWO_VIEW_PLAN.md`, `CHECKPOINT.md` and `DIAGNOSTICS_AND_TESTING.md`. GitHub Actions run `37910662990` tests v0.5 Android JVM source and builds ARM64/universal APKs; check results before assuming they exist.

## Experimental v0.5.0 sparse PLY — source, APK and device test instructions

The user verified all three Android capture modes, settings and OpenCV ORB geometric-pair analysis in **v0.4.0**, including nine successful capture sessions and eight completed ORB reports. This is the **last user-verified Android baseline**, exact protected source `36323ccbda8d6dbf7efce929c14ae909233cee33` at `backup/android-v0.4.0-user-verified-orb`, ARM64 APK SHA256 `7f9aca331b78f56595f799971106c2e23a09b212f0ece565846fd40d3ebfb1cd`. Windows never changed.

**NEW v0.5.0 experimental Android source:** `4538e52379eadcee400de9b40748144ebee9a42f`, protected branch `backup/android-v0.5.0-two-view-ci-candidate`. GitHub Actions build `37910662990` SUCCESS: https://github.com/auxz2jz/Slot-19/actions/runs/37910662990 ; artifact `video3d-android-v0.5.0-candidate`, ID `11606766326`.
- ARM64 APK: `Video3DCapture-Android-v0.5.0-ARM64-CANDIDATE.apk`, 36,437,337 bytes, SHA256 `fbc26f5493e823703af40cd9a6f1be8b57d7f7537bb04554903b2f69aba083d9`.
- Universal APK: `Video3DCapture-Android-v0.5.0-UNIVERSAL-CANDIDATE.apk`, 153,272,947 bytes, SHA256 `bba3d23425edcc7f6cbd91b6298bdc69ba9c3e5a79891d5521c4493d1f0127cd`. Both checksums matched embedded SHA256SUMS.txt, downloaded archive integrity PASS.

**What to test:** Preserve any data needed from older version first; different GitHub debug certificate may require uninstall, which can erase app-private runs. On the phone, capture a well-lit detailed stationary object with camera moving around it using one of the three existing methods. Confirm former ORB function still works. Then tap **Analyze Sparse 3D — Two Views**. A successful pair with adequate triangulation saves **an experimental sparse point cloud**, available via **Export Sparse PLY — Experimental**. Open the PLY in a PLY-capable 3D point viewer to inspect point shape, and upload ZIP from Export Test + Diagnostics or Export ALL Runs for the matched pair/inlier/parallax/reprojection evidence. If it reports INCONCLUSIVE, export ZIP anyway. Repeat on low-texture/nearly stationary images to evaluate false-positive behavior.

**CRITICAL:** one selected pair only, rough estimated camera intrinsics (not calibrated), scale arbitrary, background features possible, no real-world measurements, no dense/full-model reconstruction, no mesh, no texture map or globally optimized multi-view poses. An apparent PLY is not proof of an accurate model. **v0.5.0 is a CANDIDATE until physically user-verified.** See `SPARSE_TWO_VIEW_PLAN.md` and `CHECKPOINT.md`.


## v0.6.0 phone build — view PLY directly + optional chessboard photos

**Available now, user physical test pending.** Source `837ff21901b4d88cb6cab40d934270cf7b8ab330`, protected `backup/android-v0.6.0-viewer-calibration-ci-candidate`. GitHub Android Actions run [37914986139](https://github.com/auxz2jz/Slot-19/actions/runs/37914986139) passed unit tests/Android compile and uploaded artifact `video3d-android-v0.6.0-candidate`, ID `11609631676`.

- **Recommended ARM64 APK** `Video3DCapture-Android-v0.6.0-ARM64-CANDIDATE.apk` (36,470,105 bytes), SHA256 `5ffc06933091b4a6a648970ee756ed3b92677c965f6f5c3fa1eafe8143ed9689`.
- **Universal APK** `Video3DCapture-Android-v0.6.0-UNIVERSAL-CANDIDATE.apk` (153,305,715 bytes), SHA256 `718bb8f67d9552f979bb51234f6c6032cf22bb8e962c8d2a4ee99c734e582850`. Artifact ZIP integrity and both hashes verified against `SHA256SUMS.txt`.

### View your existing sparse PLY point clouds
1. If old app data is retained, tap **View Sparse Points — 3D** after capturing or processing. Tap **Saved scans** to choose from previous app-private runs. If the prior APK had to be uninstalled and its private files disappeared, tap **Open a PLY File to View** to import a previously exported ASCII PLY from phone storage.
2. In the native viewer, drag to rotate, pinch to zoom, change point size, toggle Point Colors / Monochrome, hide large spatial outliers using Focus Cluster, and Reset View. This is offline; no 3D renderer or external viewer is needed.
3. The displayed points are from one experimental two-view reconstruction with unknown real-world scale. A point cluster is not a watertight mesh or proven accurate object model.

### Calibrate with your own black-and-white grid
1. Make sure your US Letter print is at 100% **Actual Size**. Your physical target has **9 × 6 INNER corners**, not 9 × 6 squares, **10 × 7 total squares**, each **25.0 mm**, outer pattern **250 × 175 mm**, with the 250 mm dimension along the page's 11-inch side. Measure a printed square with a ruler before trusting the 25.0 mm number.
2. Using the same rear camera/lens/zoom you intend to scan with, take **12–20 sharp photos** of the board. Keep its 54 inner corners visible, vary viewing angles/positions/distances, use even light and avoid glare, motion blur or digital zoom. The backing paper being white is fine; the important black-white internal corners should have clear contrast.
3. Tap **Calibrate Using Checkerboard Photos** and choose the pictures with Android's multiple-image picker. This runs OpenCV checkerboard recognition and calibration locally, then displays how many photos were accepted and the reprojection RMS error.
4. The redacted results are saved in `camera_calibration/last_attempt.json` (and `last_checkerboard.json` for plausible candidate) and included in **Export ALL Runs + FPS Comparison** diagnostics. No camera images are exported in that ZIP.
5. **Calibration is not automatically applied to v0.5 sparse reconstruction.** Camera sensor crop, aspect ratio, lens, zoom and source video may not match the calibration photos. Measure and validate compatibility before using K/distortion in future geometric reconstruction.

The last *fully* user-verified Android capture/ORB baseline is v0.4.0; v0.5.0 had device-confirmed sparse PLY generation but 3D shape could not be viewed. v0.6.0 is a build-tested **CANDIDATE** until the user verifies the viewer, legacy modes and optional calibration on their phone. Keep your previously exported PLYs and diagnostic ZIPs before uninstalling a differently signed debug APK; uninstall may erase private captured runs. See `CHECKPOINT.md` and `POINT_VIEWER_CHECKERBOARD_PLAN.md` for handoff.


## v0.6.1 permanent automatic export filenames (fixed)

**Bug fixed:** Android v0.6.0 asked for a `v0.5.0` PLY filename and newly generated diagnostics sometimes carried a hard-coded earlier version. The fix is now in v0.6.1 (source `78e896f7937a4c80a9c1c175963487346bedd816`, recovery branch `backup/android-v0.6.1-dynamic-export-ci-candidate`; [GitHub Actions build 37917793167](https://github.com/auxz2jz/Slot-19/actions/runs/37917793167) SUCCESS).

**Export names derive from the currently installed app version**, not manually typed numbers. Every PLY and latest-run ZIP includes its original run ID plus UTC millisecond timestamp; all-run ZIP includes version + timestamp. E.g. `Android-v0.6.1-sparse-two-view-<runId>-<UTCtime>.ply`, `Android-v0.6.1-last-run-diagnostics-<runId>-<UTCtime>.zip`, `Android-v0.6.1-ALL-run-comparison-<UTCtime>.zip`. When Gradle `versionName` changes for a future release, these filename suggestions and CI APK/artifact names change automatically. Android's save dialog still allows the user to change a suggested name.

Two new pieces of provenance keep history honest: each newly exported ZIP includes `export_metadata.json` with its exporter's version/time, and the all-runs summary includes the **original capture version** per run. Existing old photos, `manifest.json`, `sparse_report.json` and PLYs are **never silently renamed or rewritten**. Stable internal `sparse_two_view.ply` name remains for backward compatibility.

- ARM64 APK `Video3DCapture-Android-v0.6.1-ARM64-CANDIDATE.apk`, 36,486,489 bytes, SHA256 `71abe7e2d04194165911453657ba7b167e33c9e6e4a34feb0ca61c921d79b252`.
- Universal APK `Video3DCapture-Android-v0.6.1-UNIVERSAL-CANDIDATE.apk`, 153,322,099 bytes, SHA256 `671ac10d49e58293eca29a09c299c7c4c73a77c23c26cbf84d7afcb862ab6d7e`. Both were hash-checked against GitHub Actions artifact manifest, and zip integrity passed.

User should install and verify new naming for all export types, confirm no app regression, and export diagnostics. v0.6.1 is an **unverified patch candidate** until user confirmation; **v0.6.0 remains last user-verified Android app baseline**. See `EXPORT_NAMING_STANDARD.md` and `CHECKPOINT.md`. Windows implementation unchanged.
