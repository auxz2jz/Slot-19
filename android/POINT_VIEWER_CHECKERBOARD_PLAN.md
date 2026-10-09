# Android v0.6.0 viewer and checkerboard calibration — candidate scope

## Evidence and last good recovery point
- User confirmed Android v0.5 captured frames/geometry analysis and exported multiple PLYs successfully but **cannot inspect the point clouds with their available viewer**. Do not claim the 3D model accurately resembles their object. Preserve v0.4 user-confirmed ORB capture baseline and v0.5 device-tested capture/PLY output recovery branch `backup/android-v0.5.0-device-tested-sparse-export` (exact compiled source `4538e52379eadcee400de9b40748144ebee9a42f`).
- New all-run ZIP `Android-v0.5.0-ALL-run-comparison.zip` passes integrity; includes 10 capture runs PASS and 10 `SPARSE_CANDIDATE` reports (point counts 77,114,261,116,100,143,24,101,146,76). No statement about recognizable shape.
- Android-owned paths only. Never modify Windows or Slot-8.

## Viewer
- Add **View Sparse Points — 3D** next to prior Analyze Sparse and Export Sparse PLY buttons. Native Android Canvas offline XYZ color rendering: touch drag rotates, pinch zooms, reset view, point-size slider, original RGB/monochrome, focus-main-cluster toggle for very distant outliers.
- Open the latest saved point cloud or select **any retained saved run** from dropdown; import externally exported ASCII PLY through Android Storage Access Framework. Input parser explicitly supports ASCII 1.0 with reordered XYZ/RGB columns, bounds to 50,000 vertices, validates finite coordinates and count, rejects binary/corrupt files. Does not upload or modify images/PLY.
- Records viewer load success/failure to run diagnostic events. User-visible test requires viewer showing point clouds that move when dragged, pinch zoom and cluster focus responding as expected; does not claim full 3D reconstruction.
- Runs without PLY remain valid; imported PLY can be opened even with no app-saved clouds.

## Physical checkerboard profile
- Target: checkerboard **9×6 inner corners (54)**, **10×7 square cells**, each square **25.0 mm**, outer checkerboard **250×175 mm**. On 8.5×11 in US Letter, 250 mm (~9.84 in) side runs along paper's 11 in length and 175 mm (~6.89 in) runs along 8.5 in width; print at 100% Actual Size and physically verify a 25 mm square with ruler. White backing is fine if black-white internal grid corners remain distinct.
- Optional new **Calibrate Using Checkerboard Photos** picker accepts 8–40 *still photos from one physical camera/lens/zoom* (recommended 12–20), varied tilt, distance and positions, all board corners in frame, even light/no glare. OpenCV 4.12 `findChessboardCornersSB` uses 9×6 grid; `calibrateCamera` estimates K and distortion, RMS and accepted/rejected photos. Input image pixel count bounded for memory. Writes `camera_calibration/last_attempt.json`, `last_checkerboard.json` only for finite low-error candidate, and event log. No images copied.
- **Important safeguard:** computed intrinsics are *NOT automatically injected into the existing v0.5 sparse two-view engine*. Imported calibration photos may come from a different camera, lens, focus, aspect, crop or video mode; blindly applying them creates false precision. Save profile separately for next verified experiment only. Note sample downscaling and EXIF orientation limitations in diagnostics; calibration is not final validated.
- ALL Runs ZIP contains redacted calibration profile/attempt/event logs, no calibration image bytes/URIs. Optional printed checkerboard drawing is not generated; user already has physical print.

## Test gate and next work
- Pure JVM PLY tests: valid generated PLY + RGB, reordered columns, corrupt/binary rejection, exact 9x6/25mm dimension facts.
- GitHub Android-only CI `testDebugUnitTest` and `assembleDebug`, ARM64 + universal APK hashes. Diagnose first actual compiler failure, avoid repetitive loop.
- On device: open existing 24–261 point saved sparse outputs, rotate/pinch/recenter, import exported PLY, check color/outlier focus, ensure all three capture modes/ORB/two-view/exports regressions absent. Test checkerboard photos: count accepted, reprojection RMS, no silent application to sparse; also test a blurred image rejection. Export ALL Runs ZIP.
- **v0.6 remains candidate until user explicitly verifies on-device.** Last user-verified baseline v0.4 for capture/ORB, v0.5 device-tested limited to capture/sparse output but model shape unverified.
