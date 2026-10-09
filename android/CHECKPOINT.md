# Android checkpoint — v0.5.0 two-view sparse candidate

As of 2026-10-09 PDT. Platform: **Android only** in `auxz2jz/Slot-19/android/` and `.github/workflows/android-build.yml`. Windows-owned historical Python code/tests/checkpoint and `auxz2jz/Slot-8` must remain unchanged.

## Last USER-VERIFIED ANDROID BASELINE — v0.4.0
- User explicitly confirmed v0.4.0 tested fine after uploading latest-run + ALL-run diagnostics. Verified scope: **three frame/photo capture modes, adjustable sampling controls and diagnostics, actual OpenCV ORB/Hamming feature matching, fundamental-matrix RANSAC consistency reports**. **No actual 3D reconstruction or accurate camera pose has yet been user verified.**
- Source commit `36323ccbda8d6dbf7efce929c14ae909233cee33`. Protected preservation branch **`backup/android-v0.4.0-user-verified-orb`**; original candidate branch `backup/android-v0.4.0-arm64-ci-candidate`.
- Android Actions run `37906142981` passed compiled/unit-tested ARM64/universal builds. Verified ARM64 candidate APK SHA256 `7f9aca331b78f56595f799971106c2e23a09b212f0ece565846fd40d3ebfb1cd`.
- User ZIP `Android-v0.4.0-last-run-diagnostics.zip` SHA256 `a3b21e8f604a7323f69eab99d79315fca439c5d1c3069f16657f9ed36f861e2a`: 30 frames, 29/29 ORB geometry pairs consistent, 8,374 ratio matches and 5,892 fundamental RANSAC inliers.
- User ZIP `Android-v0.4.0-ALL-run-comparison.zip` SHA256 `49fc9840b0b4e098ef1de937988c7340ded87f8cf3f0fb02667edeb1c1c392f2`: 9 capture-run results PASS; 8 completed ORB analyses (1 run NOT_ANALYZED); total 290/290 pairs EPIPOLAR_CONSISTENT, 60,303 ratio matches, 38,452 RANSAC inliers. All reported inliers on full images; background and degeneracy could inflate apparent usefulness, and all passes do NOT establish meaningful 3D.
- Historical detailed v0.4.0 checkpoint archived at `android/history/ANDROID_V0.4.0_ORB_DEVICE_REPORT.md`. Preserve independently verified Android v0.3.0, v0.2.0, v0.1.0 backups.

## New v0.5.0 experimental CANDIDATE — NOT user verified
- Android PR #4 merged to main at `09368d80fb8c91de13f7f263ca3a698f7a941000`, versionCode 5, versionName 0.5.0.
- Initial GitHub Actions `37910493563` FAILED Kotlin compile due to missing `JSONArray.length()` invocation in sparse report. Fixed exactly in commit `4538e52379eadcee400de9b40748144ebee9a42f`. **Follow-up CI `37910662990` COMPLETED SUCCESS**, including `testDebugUnitTest`, `assembleDebug`, SHA256 manifest, APK upload.
- New optional **Analyze Sparse 3D — Two Views** button after completed capture. Existing capture modes, ORB feature analysis, settings and stored JPGs deliberately unchanged.
- `SparseTwoViewAnalyzer.kt` analyzes a bounded 7 candidate pairs anchored to first saved image; performs real OpenCV 4.12 ORB matching, essential-matrix RANSAC, `recoverPose`, `triangulatePoints`, positive-depth, parallax and reprojection filtering. Chooses one acceptable two-view pair, saves at most a small **unscaled, uncalibrated** colored ASCII PLY. The feature exists only as an EXPERIMENTAL pose/cloud proof of concept, not full-scene SfM/mesh.
- ***Camera intrinsics are guessed*** (principal point near image center and focal ~0.95×larger frame dimension, no lens correction); monocular relative translation has **unknown scale** (cannot report mm, cm, meters). Feature matches may come from the background, and calibrated real-world object coverage is NOT measured.
- New `sparse_report.json` contains pair attempts, match/cheirality/parallax/reprojection findings, chosen pair or inconclusive verdict; `sparse_last_failure.json` records errors. Latest-run/all-run diagnostic ZIPs include reports but NOT raw image or PLY bytes.
- A separate **Export Sparse PLY — Experimental** user action exports generated point cloud via Android SAF only when a PLY exists. Each result retains the original capture PASS and originals unchanged.
- `SparsePolicy.kt` and JVM `SparsePolicyTest.kt` verify candidate pair bound, acceptance gate values and honest ASCII PLY encoding. Native OpenCV trajectory/point geometry have NO instrumented end-to-end device test yet.

## Known risks / limits
- Inaccurate intrinsics, distortion, camera EXIF rotation, flat surfaces, pure rotations, poor parallax or dominant moving background can yield invalid, sparse, inverted, or INCONCLUSIVE geometry even if previous ORB pair check returned EPIPOLAR_CONSISTENT. User must inspect exported PLY and camera motion.
- Source data from existing app-private runs remains after an update only if package signing is compatible. GitHub debug signing can change; uninstall may erase stored runs. Export reports before uninstall.
- Camera setup/preview and three old capture paths must never be rewritten just to enable new reconstruction.
- Generated PLY has unknown scale and only one pair; there is no full multi-view bundle adjustment, dense geometry, textured mesh or calibration-based model yet. No automatic claims of best FPS.
- Existing ZIP self-export may miss the post-close EXPORT_RESULT event; known older gap.

## Next actions
1. DONE: First compiler failure isolated to `frames.length` property typo; fixed to `frames.length()`. Final Android CI run `37910662990` SUCCESS.
2. DONE: Tested source and backup branch, two APK SHA256 hashes and artifacts recorded below; check Windows ownership at end of work.
3. User physically tests three capture methods and ORB again; run **Analyze Sparse 3D — Two Views** on a textured object with viewpoint translation and a stationary/featureless negative test. Export PLY if available, plus diagnostic ZIP and description of its appearance.
4. Promote v0.5 only after explicit user confirmation. Preserve last verified v0.4 source and APK.
5. Next stage, conditional on actual PLY output and calibration quality: more accurate intrinsics, degeneracy screening, multi-view tracking, proper SfM bundle adjustment and iterative sparse densification.

See `android/SPARSE_TWO_VIEW_PLAN.md`, `PROJECT_MEMORY.md`, `ROADMAP.md`, `DIAGNOSTICS_AND_TESTING.md`, master `auxz2jz/master-instruction-library` and repository `AGENTS.md`.

## v0.5.0 successful CI / reproducible APK artifacts — 2026-10-09
- **Exact compiled candidate source SHA:** `4538e52379eadcee400de9b40748144ebee9a42f`.
- **Immutable candidate recovery branch:** `backup/android-v0.5.0-two-view-ci-candidate` at exact compiled source.
- **CI run:** `37910662990` **SUCCESS**, link https://github.com/auxz2jz/Slot-19/actions/runs/37910662990 ; Android debug JVM unit tests, compile, ARM64 and universal APK assembly, hash manifest and artifact upload succeeded.
- **GitHub artifact:** `video3d-android-v0.5.0-candidate`, artifact ID `11606766326`.
- **ARM64 APK:** `Video3DCapture-Android-v0.5.0-ARM64-CANDIDATE.apk`, 36,437,337 bytes, SHA256 `fbc26f5493e823703af40cd9a6f1be8b57d7f7537bb04554903b2f69aba083d9`.
- **Universal APK:** `Video3DCapture-Android-v0.5.0-UNIVERSAL-CANDIDATE.apk`, 153,272,947 bytes, SHA256 `bba3d23425edcc7f6cbd91b6298bdc69ba9c3e5a79891d5521c4493d1f0127cd`.
- ZIP archive integrity PASS and both APK SHA256 checks MATCH embedded `SHA256SUMS.txt`; extracted into local output paths. No instrumentation/device two-view pose test has been performed; success means compile/unit tests, NOT accurate 3D.
- Last VERIFIED Android baseline remains v0.4.0 capture and ORB, source `36323ccbda8d6dbf7efce929c14ae909233cee33`, branch `backup/android-v0.4.0-user-verified-orb`. Do not replace it without physical user confirmation.
