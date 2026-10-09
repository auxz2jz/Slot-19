# Android v0.4.0 – ORB + Geometric Pair Analysis Plan

## Verified capture-only baseline
User has physically tested Android v0.3.0 (all three capture modes, sliders and frame previews) and said “everything looked fine”, following reports of 9 completed device sessions and valid all-runs ZIP. **Promote Android v0.3.0 capture/UI scope as last user-verified baseline**, not 3D reconstruction or precise frame alignment. Immutable tested source `8c61027d3eb83efcfa1bf8d4df4caf8631f2b262`, original artifact SHA256 `51f5629305332b67a763f9007332fe96fc8fe285d1ab0b892c4cbafc87d01938`. Dedicated backup `backup/android-v0.3.0-user-verified-capture`. Existing v0.2.0 and v0.1.0 backups retained.

## New milestone deliberately isolated
Add **Analyze Latest Run — ORB Geometry** button AFTER any capture. The three working capture paths must remain unchanged. Analysis loads existing app-private JPEGs, computes real ORB keypoints/descriptors, Hamming nearest-neighbor ratio matches, and OpenCV fundamental-matrix RANSAC inliers on **pairs of saved photographs**, yielding diagnostics of geometric consistency across adjacent viewpoints. It does not reconstruct a 3D cloud/mesh nor recover true camera pose. Do not use the older gray-pixel novelty as a geometry surrogate.

OpenCV Android is an officially maintained Maven Central AAR (`org.opencv:opencv:4.12.0`; Apache 2.0). This increases APK size and adds native binaries; confirm Android CI and real-device OpenCV load before claiming operation. Use bounded 640px preprocessing, ~800 features/frame, max 80 sampled image pairs per analysis run to control memory/time. Full-resolution saved JPEG files remain unmodified and analysis is user-triggered only, so live preview/shot processing remain unchanged.

## Genuine results & limitations
Pair outcome: feature count in A/B, descriptor count, bidirectional or ratio-tested match count, fundamental-matrix RANSAC inlier count/ratio, optional median inlier motion, status such as `GEOMETRY_SUPPORTED`, `TOO_FEW_FEATURES`, `INSUFFICIENT_MATCHES`, `RANSAC_REJECTED`, or explicit error. RANSAC inliers are epipolar consistency **on the whole image**; background may dominate, and planar/pure-rotation/low-parallax cases are degenerate. Never call 2D keypoint matches a 3D reconstruction, percent around object, measured camera angle or guaranteed object coverage.

Persist per-run `geometry_report.json` + `geometry_pairs.jsonl`, progress/events and warnings. Include those in both latest-run ZIP and all-runs ZIP. The geometry task gets dedicated background executor, recoverable state and explicit manual PASS/FAIL guided test instruction. It must not turn a valid capture-run PASS into a failure just because geometry is weak. The user's current 0.3 data may be retained if installed over compatible signing; warn about uninstall data deletion.

## Verification
Add unit tests for pure geometry verdict/summary thresholds and skip policy, retaining prior tests. GitHub build must compile new official OpenCV AAR, run unit tests and produce an APK. Device test includes same/static shots (should find matches but little informative baseline), neighboring photos circling a stationary object (geometric inliers possible), featureless/blurred scenes (honest low confidence). If OpenCV crashes, record error, keep three capture methods usable. Only user physically confirming functionality promotes 0.4.

Windows root Python files, its checkpoint, and Slot-8 are untouched. No automatic best-FPS claim or live smart-shot gating until actual pairwise evidence proves valuable.
