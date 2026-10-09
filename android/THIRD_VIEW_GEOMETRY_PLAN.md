# Android v0.7.0 — Third-view validation of two-view sparse XYZ, not a mesh

## Verified base and incoming evidence
- User confirmed v0.6.1 actual export naming, basic capture, sparse PLY and viewer operations “everything seems like it worked”. User uploaded v0.6.1 PLY and latest-run/all-run ZIPs (integrity PASS). **Last USER-VERIFIED Android APP FUNCTIONALITY baseline v0.6.1**: exact compiled source `78e896f7937a4c80a9c1c175963487346bedd816`, backup `backup/android-v0.6.1-user-verified-export-naming`. Geometry shape / calibration compatibility NOT confirmed.
- Latest v0.6.1 imported-video run `20261009T103931Z_58193e78-2`: 300 source frames (720×1280 original), FPS 5; sparse report `SPARSE_CANDIDATE`, chosen source frames 0 and 6, 158 points, 232 ratio matches and 158 cheirality inliers, median parallax 9.848 degrees, median two-view reprojection error 0.354px. Exported PLY has 158 finite XYZ+RGB vertices.
- Repeated checkerboard calibration from 9×6 inner corners and 25mm pitch used 15 photos, accepted 13, `CALIBRATION_CANDIDATE` RMS 0.16682px with working image 1000×467px. 1000×467 image aspect (2.14:1) and original imported-video frame 720×1280 (1.78:1) differ. This is NOT evidence of same lens/crop/calibrated scan intrinsics. Do NOT inject guessed or board K automatically.

## v0.7.0 independent geometric test
- Keep all prior Live / imported-video / Smart capture modes, preview, two-view PLY output, native point viewer, 9×6 checkerboard capture and auto-versioned ZIP/PLY/APK names unchanged.
- New **Verify Sparse Points in Third View** button available AFTER a valid two-view PLY is created. Bound to a few (≤4) later saved frames, never same source pair.
- Recompute first+selected baseline ORB+essential/pose+triangulated 3D, preserving anchor feature index→3D mapping; match anchor ORB descriptors in independent third photo, build true 3D→2D tracks, estimate third relative camera pose using OpenCV PnP RANSAC, compute inlier count and median reprojection error. Small thresholds: ≥12 correspondences, ≥10 inliers, ≥45% inlier ratio, ≤3px median error. Report per-frame verdict. **Does not alter existing source, original PLY or capture PASS**, and it does NOT solve multi-view bundle adjustment, merge clouds or measure metric scale. A positive third view is extra cross-check evidence, not a complete scan.
- New diagnostics `third_view_report.json` or `third_view_last_failure.json`; both included in latest and all-runs ZIP. Status `THIRD_VIEW_SUPPORTED`, `INCONCLUSIVE` or `FAILED`; source pair/frame indices, track/inlier counts, reprojection error, estimated intrinsics caveats.
- Calibration compatibility rule uses physical image aspect ratio ignoring orientation: if calibrated image and original capture frame aspect differ by >1%, report `INCOMPATIBLE_ASPECT_RATIO`; if they match, report `ASPECT_ONLY_MATCH_LENS_CROP_UNVERIFIED` and still do NOT apply intrinsics. User needs same camera/lens/crop/zoom for actual calibration migration.
- Pure JVM `ThirdViewPolicyTest.kt` covers bounded candidate frames, rejected weak poses, and mismatch handling; CI builds ARM64 and universal debug APKs using dynamic names from Gradle versionName.

## Device test
1. Update v0.6.1 → v0.7.0 APK without uninstall when possible. If uninstall required, export PLYs/ZIPs first.
2. Use old camera/Smart or video import as before, keep object stationary and move phone; generate sparse two-view. Check old viewer and PLY export.
3. Tap **Verify Sparse Points in Third View**; inspect status and PnP inliers/reprojection in latest-run or all-run ZIP. Try a more stationary/blurred/camera-rotated sequence as a negative.
4. Confirm checkerboard calibration is reported as incompatible on frame geometries that don't match; do not mistake low board RMS for compatibility. No dimension claims.
5. User personally confirms all controls and exports; only then v0.7 USER-VERIFIED. Retain v0.6.1 backup. Windows/Slot-8 untouched.

**Known limitation:** May fail on low-texture/planar scenes, background matches, pure rotation, poor intrinsics, video motion, and PnP pose degeneracies. Such failure should not mark an otherwise valid capture FAIL. True SfM global multi-view structure and calibrated bundle adjustment are later steps.
