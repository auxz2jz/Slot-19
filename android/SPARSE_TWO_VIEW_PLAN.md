# v0.5.0 — experimental TWO-view camera pose and sparse point cloud (Android only)

## Starting verified state
User confirms Android v0.4.0 capture/UI/ORB analysis works. Diagnostic uploads: v0.4 latest ZIP SHA-256 `a3b21e8f604a7323f69eab99d79315fca439c5d1c3069f16657f9ed36f861e2a`; all-runs ZIP SHA-256 `49fc9840b0b4e098ef1de937988c7340ded87f8cf3f0fb02667edeb1c1c392f2`. 9 capture PASS, 8 completed ORB analyses, 290/290 tested image pairs EPIPOLAR_CONSISTENT with 60,303 ratio matches, 38,452 RANSAC inliers. All passing could still be due to object/background texture or low parallax. This confirms feature matching *operation*, not metric camera poses, object completeness, or point cloud geometry.

**LAST USER-VERIFIED ANDROID BASELINE: v0.4.0 for capture and ORB pair analysis**, exact CI-tested source `36323ccbda8d6dbf7efce929c14ae909233cee33`, branch `backup/android-v0.4.0-user-verified-orb`. v0.4.0 ARM64 APK SHA256 `7f9aca331b78f56595f799971106c2e23a09b212f0ece565846fd40d3ebfb1cd`. Keep v0.3, v0.2, v0.1 backups intact. Windows and Slot-8 untouched.

## Scope (small independent milestone)
Add `Analyze Sparse 3D — Two Views` button and independent analyzer after an existing completed run. Preserve current 3 modes, quality sliders and v0.4 ORB analysis unchanged. Use actual OpenCV 4.12: ORB descriptors, Hamming KNN mutual or Lowe ratio matches; Essential-matrix RANSAC with estimated camera intrinsics, relative rotation/translation via recoverPose cheirality, triangulate inlier pairs and reject negative depth / poor reprojection / low parallax. Choose among a few bounded view pairs from the same run (not an entire fused scene), stop at a deliberately small capped workload.

**Critical camera model caveat:** current app does not have trustworthy camera intrinsic calibration for recorded video or all phones. v0.5.0 uses estimated focal and center, and assumes same camera intrinsics / no lens distortion between selected pair. Record `intrinsicsSource=ESTIMATED_NOT_CALIBRATED`, focal guess and image dimensions in report. Recovered translation has **arbitrary scale**, not meters. No real full-object multi-view reconstruction, dense surface or mesh.

Save `sparse_report.json` with every candidate pair's match/inlier/parallax/reprojection counts, verdicts, and selected pair; when quality gates pass save `sparse_two_view.ply` as ASCII vertex-only colored points in arbitrary units. Keep originals and capture PASS immutable. Export PLY via dedicated Android SAF `Export Sparse PLY`; geometry report enters existing latest/all diagnostic ZIPs but raw PLY not included automatically.

Diagnostics must record attempt/start/progress/result/failure with run ID; failed pose estimate is not capture failure. Edge cases: planar/pure rotation/repeated textures and dominant background may fool E-RANSAC. Valid PLY generated with explicit uncalibrated and unscaled warnings; if insufficient inlier/parallax, save failure/inconclusive report and no PLY.

## Testing
- Pure Kotlin unit tests for pose/parallax/reprojection threshold policy and selected indices.
- GitHub Kotlin compile, JVM tests and versioned ARM64/universal artifacts.
- Android user-device test on textured object after changing camera viewpoint gradually; verify generated points in 3D viewer or plain PLY; test near-stationary/featureless input should report INCONCLUSIVE rather than create false 3D geometry. Export diagnostics and PLY.
- No baseline promotion until user manually verifies. Do not equate build PASS with physical verification.
