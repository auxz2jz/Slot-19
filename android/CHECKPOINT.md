# Android Recovery Checkpoint — v0.6.0 PLY viewer and chessboard calibration

Date 2026-10-09 (America/Los_Angeles). Platform Android-only; Windows and Slot-8 source untouched.

## User-verified / protected baselines
- **Last fully USER-VERIFIED Android capture + ORB baseline: v0.4.0**, source `36323ccbda8d6dbf7efce929c14ae909233cee33` on `backup/android-v0.4.0-user-verified-orb`, ARM64 SHA256 `7f9aca331b78f56595f799971106c2e23a09b212f0ece565846fd40d3ebfb1cd`.
- **v0.5.0 device-tested output:** user reported all workflows operated, exported multiple ASCII PLYs, but had no way to view shapes. Thus capture/PLY serialization successful, *3D point cloud appearance/accuracy NOT visually verified*. Exact CI source `4538e52379eadcee400de9b40748144ebee9a42f`, recovery backup `backup/android-v0.5.0-device-tested-sparse-export`, plus original `backup/android-v0.5.0-two-view-ci-candidate`. APK ARM64 SHA256 `fbc26f5493e823703af40cd9a6f1be8b57d7f7537bb04554903b2f69aba083d9`.
- User's uploaded new `Android-v0.5.0-ALL-run-comparison.zip`: ZIP structural integrity PASS, **10 completed capture runs PASS, 10 `SPARSE_CANDIDATE` reports**, point counts 77/114/261/116/100/143/24/101/146/76. User previously supplied nine separate ASCII PLY clouds and could not view them in a generic phone viewer.
- Older v0.3/v0.2/v0.1 verified source backups remain intact.

## Current v0.6.0 candidate — NOT USER VERIFIED
- PR #5 merged to main at **`837ff21901b4d88cb6cab40d934270cf7b8ab330`** (Android-only, new point viewer, safe PLY reader and optional checkerboard calibration).
- GitHub Actions initial run **`37914986139`** queued at checkpoint creation. Do not claim build PASS or APK until actual result checked.
- Native `SparseCloudView.kt`: colored 3D points drawn on an Android Canvas; drag to rotate, pinch to zoom, reset, change point size/colors, optional 90% cluster focus to hide large sparse triangulation outliers; arbitrary unknown units.
- `PlyParser.kt`: bounded offline ASCII 1.0 PLY reader with XYZ and flexible RGB property order, 50k vertex cap and finite coordinate checks; pure JVM `PlyParserTest.kt` verifies generated PLY, alternate order, invalid formats and checkerboard dimensions.
- Main UI: **View Sparse Points — 3D** opens a built-in interactive dialog with all retained app-private saved sparse runs, and **Open a PLY File to View** via Android Storage Access Framework for exported/imported ASCII PLY; previous capture/ORB/pose workflow untouched. The viewer does not imply correct 3D geometry.
- Physical checkerboard user specification: **9 columns × 6 rows of INNER corners (54)**, 10×7 physical squares, **25.0 mm pitch**, full black-and-white pattern **250×175 mm**, printed 100% Actual Size (no Fit to Page) on US Letter 8.5×11 inches. The 250 mm side aligns along the 11-inch paper dimension. White background is acceptable if internal corner contrast is present and the printed square pitch is measured with a ruler.
- `CheckerboardCalibrator.kt`: optional **Calibrate Using Checkerboard Photos**, Android multi-document picker for 8–40 JPEGs (12–20 recommended) from same camera/lens/zoom, OpenCV `findChessboardCornersSB` 9×6 inner corners, object points spaced 25mm, `calibrateCamera`, finite K/distortion and reprojection RMS checks, per-image accept/reject + diagnostics. Bounded bitmap resolution. Saved app-private `camera_calibration/last_attempt.json`, `last_checkerboard.json` for numerically plausible, low-RMS **candidate**, and `events.jsonl`. Only ALL Runs ZIP embeds these redacted reports (no photos/URIs).
- **Important safety:** calibration estimated from selected photos is NOT automatically applied to current sparse reconstruction; lens/camera/sensor crop/video framing could differ. Next milestone must confirm camera compatibility and calibration accuracy before use. The printed pattern being known is not itself a camera calibration.
- No modifications to Windows project, Slot-8, PLY source frame bytes, or original v0.5 user point clouds.

## Known risks / physical test
- No physical user test of v0.6 PLY drag/pinch, Android Storage Access import, or OpenCV checkerboard detection. JVM tests and GitHub APK compile alone do not verify visual functionality.
- PLY viewer shows experimental two-view points, not a complete mesh or full 360-degree object. Focus cluster may exclude genuine points; show all to inspect outliers.
- EXIF orientation and source camera calibration may differ; selected files need one lens/zoom and consistent image orientation/resolution. Repeated near-identical board positions can lead to unreliable calibration even with low RMS. Calibration candidate is not guaranteed correct and is not auto-applied.
- CI may fail first; inspect first real error and fix smallest Android-only code, no blind retries.
- Debug APK signing may require uninstall/erase app data. Before uninstalling existing installed APK, export important diagnostics and copy exported PLYs; installed app-private run folders may be lost.

## Next exact action
1. Check v0.6 GitHub CI `37914986139`; resolve compilation/test errors using log evidence. Record final source hash/backup branch and artifact hashes.
2. Test on Samsung Galaxy S22 Ultra: open previously generated sparse PLY directly from phone files or a saved app run, rotate/pinch/zoom, focus and reset, select another run; no 3D shape quality claim until visually reviewed.
3. Take 12–20 images of the printed 9×6 checkerboard with varied angles at same zoom and lighting. Select via **Calibrate Using Checkerboard Photos**; check accepted count and RMS, no automatic changes to sparse reconstruction.
4. Export ALL Runs + FPS Comparison to include redacted calibration summary. Return ZIP + visual viewer feedback, optionally photos/screenshots if troubleshooting.
5. Only after physical user confirmation mark v0.6 verified for viewer/calibration input; deeper calibrated sparse 3D remains a later milestone.

More details `android/POINT_VIEWER_CHECKERBOARD_PLAN.md`, `PROJECT_MEMORY.md`, `ROADMAP.md`, `DIAGNOSTICS_AND_TESTING.md`; global `auxz2jz/master-instruction-library`.
