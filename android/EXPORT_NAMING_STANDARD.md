# Mandatory Android export filename/version standard — v0.6.1 onward

## Why
User verified Android v0.6.0 PLY viewer, capture/export and checkerboard calibration behave as expected, but PLY export still offered `Android-v0.5.0-sparse-two-view.ply`, and all-run ZIP internally identified `android-0.5.0`. This was caused by hardcoded filenames and current report metadata. Treat this as a correctness and provenance bug, **not a reconstruction engine regression**.

## New permanent rule

**Never write a version number directly into user-facing export naming templates.** Every new build has its version specified once via Gradle `android/app/build.gradle.kts` `versionName`. At runtime use `BuildConfig.VERSION_NAME` and `ExportNames` for every exported-file dialog. The Android CI workflow derives the APK filenames and artifact name from the same `versionName`, avoiding hand edits when the version changes.

The version in a filename means **the version of the app performing the export**, even if the scan originated in an older version. This must be made clear in the diagnostic package via `export_metadata.json`. Never rewrite an old run's `manifest.json` or `sparse_report.json` just because it was exported again by newer software.

### Current format
- PLY: `Android-v<installedVersion>-sparse-two-view-<runId>-<UTCtimestamp>.ply`
- Latest-run ZIP: `Android-v<installedVersion>-last-run-diagnostics-<runId>-<UTCtimestamp>.zip`
- All-runs ZIP: `Android-v<installedVersion>-ALL-run-comparison-<UTCtimestamp>.zip`

Timestamp format `yyyyMMdd'T'HHmmssSSS'Z'` in UTC, with milliseconds. Per-run exports carry existing, unique run ID; all names distinguish versions and times. Users may still override suggested names in Android's document picker. The app never silently renames/deletes older files, nor changes its internal stable `sparse_two_view.ply` storage name (backward compatibility).

### Provenance
- `export_metadata.json` inside both ZIP types: `exportType`, `exportingAppVersion`, `exportCreatedUtcMs` plus original captured run version when available.
- `all_runs_summary.json`: new `exportedByAppVersion`, `exportCreatedUtcMs`, and `originalCaptureAppVersion` per run.
- New event/report/manifest and calibration appVersion fields are generated from `BuildConfig.VERSION_NAME` going forward; historical run files **not rewritten**.
- UI logs suggested export filename as a `USER_ACTION/EXPORT_NAME_SUGGESTED` event. Android SAF may allow users to rename before export; logs refer to suggestion only.

### Evidence
User uploaded `Android-v0.6.0-last-run-diagnostics.zip` and `Android-v0.6.0-ALL-run-comparison.zip`, both valid ZIP files. Two runs in all-run ZIP PASS. One `sparse_report.json` includes 261 point candidate. `calibration/last_attempt.json` indicates all 15 of 15 checkerboard images accepted with reported 0.1624 px reprojection RMS (`CALIBRATION_CANDIDATE`), not yet confirmed compatible with scanning camera. Latest report/manifest currently shows `android-0.5.0` because embedded metadata was hardcoded: do not relabel these already-stored historical files after the fact.

## Regression/test plan
- Pure JVM `ExportNamesTest`: tests each export category, correct app v0.6.1 prefix, future v0.7.0 automatically propagates, UTC millisecond timestamps, unique repeated exports, and unsafe run identifier rejection.
- Android CI must pass `:app:testDebugUnitTest :app:assembleDebug` on source with v0.6.1 and upload ARM64 and universal APK with filenames derived from Gradle version.
- Physical user test: Export PLY and both ZIP types from v0.6.1; inspect proposed filenames, ensure none says v0.5.0 or v0.6.0; export twice; ZIP should contain new exporter metadata and original run version; verify viewer, 9×6 calibration, all three capture modes remain unchanged.
- Last user-confirmed v0.6.0 source is protected under `backup/android-v0.6.0-user-verified-viewer-calibration`, exact CI source `837ff21901b4d88cb6cab40d934270cf7b8ab330`. Full-geometry correctness and calibration compatibility remain unverified.
