# Android Recovery Checkpoint — v0.6.0 PLY viewer and chessboard calibration

Date 2026-10-09 (America/Los_Angeles). Platform Android-only; Windows and Slot-8 source untouched.

## User-verified / protected baselines
- **Last fully USER-VERIFIED Android capture + ORB baseline: v0.4.0**, source `36323ccbda8d6dbf7efce929c14ae909233cee33` on `backup/android-v0.4.0-user-verified-orb`, ARM64 SHA256 `7f9aca331b78f56595f799971106c2e23a09b212f0ece565846fd40d3ebfb1cd`.
- **v0.5.0 device-tested output:** user reported all workflows operated, exported multiple ASCII PLYs, but had no way to view shapes. Thus capture/PLY serialization successful, *3D point cloud appearance/accuracy NOT visually verified*. Exact CI source `4538e52379eadcee400de9b40748144ebee9a42f`, recovery backup `backup/android-v0.5.0-device-tested-sparse-export`, plus original `backup/android-v0.5.0-two-view-ci-candidate`. APK ARM64 SHA256 `fbc26f5493e823703af40cd9a6f1be8b57d7f7537bb04554903b2f69aba083d9`.
- User's uploaded new `Android-v0.5.0-ALL-run-comparison.zip`: ZIP structural integrity PASS, **10 completed capture runs PASS, 10 `SPARSE_CANDIDATE` reports**, point counts 77/114/261/116/100/143/24/101/146/76. User previously supplied nine separate ASCII PLY clouds and could not view them in a generic phone viewer.
- Older v0.3/v0.2/v0.1 verified source backups remain intact.

## Current v0.6.0 candidate — NOT USER VERIFIED
- PR #5 merged to main at **`837ff21901b4d88cb6cab40d934270cf7b8ab330`** (Android-only, new point viewer, safe PLY reader and optional checkerboard calibration).
- **GitHub Actions run `37914986139` COMPLETED SUCCESS**, Android JVM tests + assembleDebug + checksum + artifact upload. Artifact `video3d-android-v0.6.0-candidate`, ID `11609631676`. Source tested `837ff21901b4d88cb6cab40d934270cf7b8ab330`, protected branch `backup/android-v0.6.0-viewer-calibration-ci-candidate`.
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
1. DONE: GitHub Android run `37914986139` SUCCESS. Source SHA, backup and APK hashes recorded below.
2. Test on Samsung Galaxy S22 Ultra: open previously generated sparse PLY directly from phone files or a saved app run, rotate/pinch/zoom, focus and reset, select another run; no 3D shape quality claim until visually reviewed.
3. Take 12–20 images of the printed 9×6 checkerboard with varied angles at same zoom and lighting. Select via **Calibrate Using Checkerboard Photos**; check accepted count and RMS, no automatic changes to sparse reconstruction.
4. Export ALL Runs + FPS Comparison to include redacted calibration summary. Return ZIP + visual viewer feedback, optionally photos/screenshots if troubleshooting.
5. Only after physical user confirmation mark v0.6 verified for viewer/calibration input; deeper calibrated sparse 3D remains a later milestone.

More details `android/POINT_VIEWER_CHECKERBOARD_PLAN.md`, `PROJECT_MEMORY.md`, `ROADMAP.md`, `DIAGNOSTICS_AND_TESTING.md`; global `auxz2jz/master-instruction-library`.

## Downloadable v0.6.0 APK fingerprints (reproducible)

- Source commit `837ff21901b4d88cb6cab40d934270cf7b8ab330`; immutable baseline branch `backup/android-v0.6.0-viewer-calibration-ci-candidate` points to it.
- CI https://github.com/auxz2jz/Slot-19/actions/runs/37914986139 — `testDebugUnitTest` and `assembleDebug` SUCCESS, produced APK artifact `video3d-android-v0.6.0-candidate`, ID `11609631676`.
- **ARM64 APK** `Video3DCapture-Android-v0.6.0-ARM64-CANDIDATE.apk`, **36,470,105 bytes**, SHA256 `5ffc06933091b4a6a648970ee756ed3b92677c965f6f5c3fa1eafe8143ed9689`.
- **Universal APK** `Video3DCapture-Android-v0.6.0-UNIVERSAL-CANDIDATE.apk`, **153,305,715 bytes**, SHA256 `718bb8f67d9552f979bb51234f6c6032cf22bb8e962c8d2a4ee99c734e582850`.
- Downloaded GitHub artifact ZIP integrity PASS; both APK SHA256 MATCH `SHA256SUMS.txt`, files verified at user-accessible output paths. **This only establishes a CI/tested candidate. Neither native viewer operation nor OpenCV checkerboard calibration is yet user verified.**
- Next: install ARM64 APK (export existing runs/PLYs before uninstall if signing mismatch). Test saved/imported PLY rotation/zoom/cluster, selected 12–20 checkerboard photos, calibration status and RMS, all-run diagnostics; preserve established v0.4 verified capture/ORB baseline and separately v0.5 user-tested sparse PLY generation.


## Android v0.6.0 user-device verification / version-label bug / v0.6.1 candidate (2026-10-09)

**User confirmation:** “everything seem to work just fine,” after testing Android v0.6.0 native PLY viewer, sparse analysis/exports and optional checkerboard calibration. v0.6.0 is now last USER-VERIFIED Android app for capture, ORB matching, experimental PLY production, in-app viewer and operational checkerboard-photo processing. The physical correctness of its sparse 3D models, scale and camera-mode-compatible intrinsics remains UNVERIFIED. Exact compiled v0.6 source protected on `backup/android-v0.6.0-user-verified-viewer-calibration` at `837ff21901b4d88cb6cab40d934270cf7b8ab330` (GitHub CI `37914986139`, ARM64 APK SHA256 `5ffc06933091b4a6a648970ee756ed3b92677c965f6f5c3fa1eafe8143ed9689`). Earlier backups v0.5, v0.4, v0.3 remain intact.

**Latest user files:** `Android-v0.6.0-last-run-diagnostics.zip` SHA256 `23ac942fadf4a38a307daa07cadb00bf3b92d6d671f7f3f5356fa596c008d888`; `Android-v0.6.0-ALL-run-comparison.zip` SHA256 `7c82ab2e561eec408e5ec060fa2b18b66e0456749fe34033fbc3a3da5c10`, both ZIP integrity PASS. All-run ZIP: 2 independent recorded-video capture runs PASS, latest 265 frames, sparse report `SPARSE_CANDIDATE` with 261 points. Checkerboard calibration: `CALIBRATION_CANDIDATE`, 15/15 images accepted, 9×6 inner corners at 25mm pitch, reported 0.162414 px RMS reprojection error; **not applied** to sparse analysis and not yet independently proven accurate against the scan lens/crop.

**Observed bug:** v0.6.0 PLY export picker suggested `Android-v0.5.0-sparse-two-view.ply` from hardcoded MainActivity string. More broadly, latest/all-run source reports and old event records embed hardcoded `android-0.5.0` or `android-0.4.0` despite running v0.6.0. Old stored files must NOT be rewritten: their original version may correctly identify older source runs or may contain historical labeling errors that should be explained without silently altering history.

**New patch v0.6.1 / PR #6**, merged source `78e896f7937a4c80a9c1c175963487346bedd816`. GitHub Actions build run `37917793167` queued at time of writing; DO NOT call this user-verified. Changes ONLY Android-owned `android/` and `.github/workflows/android-build.yml`. Uses central pure Kotlin `ExportNames.kt` and `ExportNamesTest.kt`: export filenames always derived from `BuildConfig.VERSION_NAME`, plus scan run ID when relevant, unique UTC millisecond timestamp. Three templates: `Android-v<version>-sparse-two-view-<runId>-<utc>.ply`, `Android-v<version>-last-run-diagnostics-<runId>-<utc>.zip`, and `Android-v<version>-ALL-run-comparison-<utc>.zip`. Gradle versionName now `0.6.1`, versionCode 7. App-internal stored PLY name `sparse_two_view.ply` intentionally remains unchanged for compatibility; old exported files are never auto-renamed/deleted. In Android SAF CreateDocument the filename remains only a suggestion users can edit.

Also added `export_metadata.json` in new ZIPs recording installed exporter version/timestamp and original run version (when present), and `originalCaptureAppVersion` per run in summary. New event/report/calibration/manifest fields use runtime app version going forward. **Previous stored run metadata is left untouched**. Android GitHub Actions now derives APK artifact name and APK version from Gradle version dynamically, so future bumps won't silently reuse older APK filenames. Pure JVM tests cover v0.6.1 and hypothetical v0.7.0, all output types, timestamps and run ID safety. Documented permanent standard at `android/EXPORT_NAMING_STANDARD.md`.

**Next steps:** check CI `37917793167`, record compiled source/branch and SHA256 validated APK; install on device and export PLY/last-run/all-run twice to confirm new name suggestions. Inspect `export_metadata.json` and compare captured original version; regress older capture/geometry/viewer/calibration. v0.6.1 candidate only until user confirms. Windows source and root checkpoint untouched.


## v0.6.1 compiled APK and source fingerprints — 2026-10-09

- **Exact compiled app source commit** `78e896f7937a4c80a9c1c175963487346bedd816`. Permanent branch `backup/android-v0.6.1-dynamic-export-ci-candidate` points to this same source.
- GitHub Actions **`37917793167` COMPLETED SUCCESS**, `testDebugUnitTest`, `assembleDebug`, automatically version-derived artifact filenames and SHA256 manifest. Link https://github.com/auxz2jz/Slot-19/actions/runs/37917793167.
- Uploaded GitHub artifact `video3d-android-v0.6.1-candidate` (artifact ID `11611096463`).
- **ARM64 Android APK**: `Video3DCapture-Android-v0.6.1-ARM64-CANDIDATE.apk`, **36,486,489 bytes**, SHA256 `71abe7e2d04194165911453657ba7b167e33c9e6e4a34feb0ca61c921d79b252`.
- **Universal Android APK**: `Video3DCapture-Android-v0.6.1-UNIVERSAL-CANDIDATE.apk`, **153,322,099 bytes**, SHA256 `671ac10d49e58293eca29a09c299c7c4c73a77c23c26cbf84d7afcb862ab6d7e`.
- Actual downloaded artifact ZIP integrity PASS, both APK SHA256 hashes MATCH `SHA256SUMS.txt`. APK files successfully extracted to download paths. User installed v0.6.1? **NO, not yet user tested.**
- Confirmed Windows root `videogrammetry/capture.py`, `videogrammetry/__main__.py`, `tests/test_capture.py`, root `CHECKPOINT.md` unchanged in main by exact Git blob hashes. Slot-8 untouched. Verified v0.6.0 fallback source remains `backup/android-v0.6.0-user-verified-viewer-calibration`.
- User next: install v0.6.1 ARM64; use sparse PLY / latest-run diagnostics / ALL run history export; check filenames all say v0.6.1 with unique run and UTC stamp. In new ZIP inspect `export_metadata.json` `exportingAppVersion = 0.6.1`; old capture manifest may still say v0.5.0 due to historic records, intentionally unmodified. Keep testing point cloud viewer/9x6 calibration and existing capture methods. Only user-device confirmation can promote v0.6.1 verified.
