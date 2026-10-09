# Android recovery checkpoint — v0.4.0 ORB geometric pair analysis

Date 2026-10-09. Current source of truth for Android only; historical v0.3.0 user device findings archived at `android/history/ANDROID_V0.3.0_FPS_DEVICE_REPORT.md`. Windows original Python code in Slot-19 and all Slot-8 files are OUT OF SCOPE and remain unchanged.

## LAST USER-VERIFIED ANDROID CAPTURE BASELINE — v0.3.0
User explicitly confirmed 2026-10-09: “Yes everything looked fine let's go ahead with the next step”, answering whether images, Smart Auto photos and adjustable FPS sliders behaved as expected. This establishes **Android v0.3.0 as user verified for its three capture/display workflows, adjustable rate/limit settings and all-run export**, not actual SfM, reconstructed 3D geometry, or any scientifically established optimum FPS.
- **Exact tested source:** `8c61027d3eb83efcfa1bf8d4df4caf8631f2b262`.
- **Preservation branch:** `backup/android-v0.3.0-user-verified-capture`. Existing `backup/android-v0.3.0-ci-candidate` preserved too.
- **APK SHA256:** `51f5629305332b67a763f9007332fe96fc8fe285d1ab0b892c4cbafc87d01938`.
- **CI:** Android Actions run `37901552835` SUCCESS, artifact `video3d-android-v0.3.0-candidate`, ID `11603001558`.
- **Real device data:** two ZIPs; all-run ZIP has 9 PASS-valid completed sessions, 549 frames, 267,721,254 bytes, live/video/smart modes, no logged errors. User says all three capture methods and frames look good. Exact details and limitations archived in v0.3 history.
- **Older good versions:** v0.2.0 `backup/android-v0.2.0-user-verified-capture`; v0.1.0 own branch.

## LATEST CANDIDATE — v0.4.0 (UNVERIFIED)
- Android-only PR #2 merged to main at **`4503a4cf9c40a8709fd13b009a766901545aff40`**. GitHub Actions run **`37905706664`** queued/in progress; DO NOT claim SUCCESS until checked.
- New dependency `org.opencv:opencv:4.12.0` (official Android AAR, Apache 2.0). New opt-in **Analyze Latest Run — ORB Geometry** after any completed capture.
- `OrbGeometryAnalyzer.kt`: downsampled (max 640 pixel edge) actual saved frame JPEGs -> ORB keypoints/descriptors (~800) -> BFMatcher/Hamming 0.75 ratio -> fundamental-matrix RANSAC (1.5px at resized scale, confidence 0.99), count inliers and per-pair verdict.
- `GeometryPolicy.kt` and JVM `GeometryPolicyTest.kt`: strict support thresholds (>=12 ratio matches/inliers, >=30% matched RANSAC inliers) and max 80 sampled pair comparisons with full first/last coverage. Every pair report includes keypoint/match/inlier counts. The ORB feature matcher is REAL; it does NOT measure true object-only overlap, recover metric camera position, solve bundle adjustment, or generate a point cloud/mesh.
- Run-local **`geometry_report.json`** and **`geometry_pairs.jsonl`** persisted separately from existing capture manifest/result and JPEGs. OpenCV init failure stored as `geometry_last_failure.json`, and diagnostic events/visible error status. Existing single-run and all-run ZIPs include these geometry reports, not image data. Runs without analysis report no geometry.
- Capturing video, live, or Smart works exactly as before, unaffected by analysis confidence; geometry processing starts ONLY by user tapping button on completed capture. Dedicated analysis executor prevents blocking camera preview. Geometry analysis can be computationally heavy, and OpenCV runtime load requires real device testing.
- Candidate source has not been user/device verified and should never displace v0.3.0 baseline if it fails.

## Known risks and expected limits
- Official OpenCV AAR bundles native libraries and may significantly increase APK size.
- Low textured objects, repetitive patterns, reflective surfaces, low parallax, strong backgrounds, and pure camera rotation can give inconclusive or misleading epipolar metrics. A high inlier count on background is not the same as verified object coverage.
- Camera intrinsics, true geometric registration, epipolar degeneracy screening, image sharpness/fps optimization, stereo triangulation and dense 3D reconstruction are future work. No automated “best FPS” claim yet.
- JPEG smart-mode EXIF orientation is not explicitly normalized before feature analysis. If saved Smart stills rotate unexpectedly, return diagnostics and compare real frames, do not guess.
- Historical ZIP exporter may omit its own post-write EXPORT_RESULT event. No image bytes in diagnostic ZIP, only redacted per-run metrics.
- APK debug signing keys may differ. Before uninstalling a previously installed version, export diagnostics and any app-private data needed; uninstall may remove capture runs.

## Required next action
1. Check GitHub Actions run `37905706664` for compile/JVM tests and APK. On failure, diagnose FIRST ACTUAL compiler/dependency error and patch ONLY Android-owned source. Stop repeated failed approaches per Master Library.
2. If successful, record exact source SHA, artifact ID, APK SHA256 and backup source branch. Verify unchanged Windows file blob SHAs.
3. User installs v0.4.0; test all three capture modes still work, then ORB analysis on saved frames; run a textured object and a near-identical/featureless set, inspect status and report, export ALL Runs ZIP.
4. Only after explicit user visual and function confirmation mark v0.4 as verified. Preserve 0.3 backup; then choose geometry-aware capture tuning, camera pose/intrinsic calibration and eventual triangulation.

See `android/ORB_GEOMETRY_PLAN.md`, `android/PROJECT_MEMORY.md`, `android/ROADMAP.md` and `android/DIAGNOSTICS_AND_TESTING.md`; observe global `AGENTS.md` and canonical `auxz2jz/master-instruction-library`.
