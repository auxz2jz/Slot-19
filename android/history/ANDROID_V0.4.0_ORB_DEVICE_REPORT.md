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
- Android-only PR #2 merged to main at **`4503a4cf9c40a8709fd13b009a766901545aff40`**. GitHub Actions run **`37905706664` SUCCESS** (first universal OpenCV build). ARM64 optimized variant built separately in **`37906142981` SUCCESS**.
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
1. DONE: GitHub Actions runs `37905706664` and `37906142981` completed SUCCESS (Android Kotlin compile, pure-JVM tests, APK assembly, artifact upload and checksums).
2. DONE: exact source, artifacts and hashes recorded below. Verified historical Windows source/tests/checkpoint blob hashes unchanged.
3. NEXT: User installs v0.4.0 ARM64, confirms prior three capture modes, then ORB analyzes a textured object and compares a featureless or static set; inspect output and export latest / ALL Runs ZIP.
4. Only after explicit user visual and function confirmation mark v0.4 as verified. Preserve 0.3 backup; then choose geometry-aware capture tuning, camera pose/intrinsic calibration and eventual triangulation.

See `android/ORB_GEOMETRY_PLAN.md`, `android/PROJECT_MEMORY.md`, `android/ROADMAP.md` and `android/DIAGNOSTICS_AND_TESTING.md`; observe global `AGENTS.md` and canonical `auxz2jz/master-instruction-library`.

## Verified CI / downloadable v0.4.0 APK identities — 2026-10-09

- **Latest source tested**: `36323ccbda8d6dbf7efce929c14ae909233cee33` (Android PR #3 ABI split, PR #2 ORB implementation already merged). **Backup branch `backup/android-v0.4.0-arm64-ci-candidate`** points to this exact source.
- **Final GitHub Actions**: `37906142981` COMPLETED SUCCESS, including `testDebugUnitTest`, `assembleDebug`, SHA256 checks, and APK artifact upload.
- **Artifact:** `video3d-android-v0.4.0-candidate`, ID `11603669766`, GitHub run https://github.com/auxz2jz/Slot-19/actions/runs/37906142981
- **ARM64 user-phone APK:** `Video3DCapture-Android-v0.4.0-ARM64-CANDIDATE.apk`, 36,404,569 bytes, SHA256 `7f9aca331b78f56595f799971106c2e23a09b212f0ece565846fd40d3ebfb1cd`. Intended for ARM64 Android phones such as Galaxy S22 Ultra.
- **Universal fallback APK:** `Video3DCapture-Android-v0.4.0-UNIVERSAL-CANDIDATE.apk`, 153,240,179 bytes, SHA256 `156d697bdfb1a8f8eac5ee1e061f4dead356f8a5bb861d5386482077d3fce327`. Both downloaded from GitHub Actions, outer ZIP passed integrity check, both hashes passed `SHA256SUMS.txt`.
- Earlier *full universal only* v0.4 compile at `4503a4cf9c40a8709fd13b009a766901545aff40` also passed GitHub Actions `37905706664`. Original source preserved `backup/android-v0.4.0-orb-ci-candidate`, APK SHA256 `e880fbc1f0fbd5e6ecb90aa653adc71d49034f58e2fcbeb448e694af1df009ab`. Superseded by ABI-specific build, not a verified baseline.
- **No Android v0.4.0 real-device OpenCV output verified yet**. The official library being compiled/packaged does not prove ORB matching or native initialization succeeds on a specific phone.
- **Still last user-verified Android capture baseline:** v0.3.0 at `8c61027d3eb83efcfa1bf8d4df4caf8631f2b262`; backup `backup/android-v0.3.0-user-verified-capture`. No Windows source/checkpoint changes.
- Next: user tests photo capture and "Analyze Latest Run — ORB Geometry", exports latest or full ZIP, reports accepted/rejected geometric pairs and any native loading/error statuses. Do not promote v0.4 until explicit confirmation.


## Final user verification 2026-10-09
User supplied v0.4.0 last-run and all-run ZIPs and explicitly confirmed everything tested fine. Nine capture runs PASS; 8 geometry analyses COMPLETE; 290/290 analyzed pairs EPIPOLAR_CONSISTENT, 60,303 ratio matches and 38,452 RANSAC inliers. Device verification scope covers capture, ORB geometric pair analysis and diagnostic export, NOT actual 3D reconstruction. Source `36323ccbda8d6dbf7efce929c14ae909233cee33`; user-verified backup `backup/android-v0.4.0-user-verified-orb`. ZIP hashes: `a3b21e8f604a7323f69eab99d79315fca439c5d1c3069f16657f9ed36f861e2a` (single); `49fc9840b0b4e098ef1de937988c7340ded87f8cf3f0fb02667edeb1c1c392f2` (all-run). 1 capture remained unanalyzed; no user-reported failure.
