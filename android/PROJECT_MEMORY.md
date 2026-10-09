# Android project memory — Video Photogrammetry

**Date:** 2026-10-08. **Platform ownership:** exclusively `android/` plus `.github/workflows/android-build.yml`. Windows stays in historical root; never modify its source or checkpoints.

## Goal
Native, local-first Android photogrammetry capture application. Stationary object + handheld phone moving around it. Eventually manual still photos, automatic high-res photos, live/video selected frames, and Android-appropriate 3D reconstruction. Laser and hybrid are separate deferred modules. No markers or turntable required. No runtime GitHub/cloud requirement.

## Baselines and candidates (pre-implementation checkpoint)
- **LAST USER-VERIFIED ANDROID BASELINE: NONE** (user has not installed/tested this candidate).
- **Latest unverified candidate: Android v0.1.0** (source commit `fd7973c6a978136d1154872162f772127c5ecfd9`; APK SHA-256 `132a5780d575bee9d243c9adccb35a953b42ad998760ef3711eaac620988b639`).
- Android v0.1.0 implemented as **CANDIDATE**: Compose + CameraX preview/sampling; local video frame import; saved frames, validated manifest, event logs, guided test and ZIP diagnostic export. GitHub CI compile/unit test SUCCESS, no device verification.
- Device target: Android smartphone (Samsung Galaxy S22 Ultra Android 16 is an intended practical tester, but device not tested).
- Current task: user-device validation of Android v0.1.0 APK; diagnose issues using exported evidence and preserve successful candidate source.

## Architecture decisions
- Kotlin + Jetpack Compose interface; CameraX Preview + ImageAnalysis for live frames; Android MediaMetadataRetriever for recorded video. Local app-private project/run folders; user-selected SAF export for diagnostics. All outputs are candidates until physically validated. No PC-only SfM libraries forced onto phone. Separate shared neutral interface docs under `shared/`.
- First v0.1.0 is capture/diagnostics foundation, **not a complete scanner or 3D reconstructor**.
- Prefer real frame validity, not successful button tap, as PASS evidence. Privacy: no raw source videos/frames in diagnostic ZIP; no full private paths logged.

## Known limitations and failed approaches
- Android v0.1.0 source exists and CI assembled successfully. Still requires real-device CameraX, frame colors/orientation, recorded-video decode and exported-ZIP testing. It lacks quality-based selection, camera calibration, SfM, point clouds, mesh and texture.
- CI setup action failed once because obsolete Android SDK package `tools` was requested; replaced with runner SDK. Kotlin compile then failed once for missing Compose `setContent` import; fixed exactly. Next build and unit tests PASSED. No repeated speculative approach.

## Files/results received
No user Android device reports supplied. GitHub Actions run `37891374599` PASSED build + unit tests; artifact `video3d-android-v0.1.0-candidate`, SHA-256 `132a5780d575bee9d243c9adccb35a953b42ad998760ef3711eaac620988b639`. Source backup branch `backup/android-v0.1.0-ci-candidate`. Slot-8 is read-only.

## Plan and exact next action
1. Checkpoint and isolate Windows source; record shared ownership and Android roadmap — DONE.
2. Implement first Android capture/diagnostics/guided test candidate under `android/`; create Android-only Actions workflow — DONE.
3. Compile/inspect CI; targeted fixes for SDK setup and missing import; APK and tests SUCCESS — DONE.
4. NEXT: User installs APK and tests video/live extraction, exported ZIP and visual frame quality. Only after explicit user confirmation may Android version be marked VERIFIED.

## Current source/artifact checkpoint
Exact tested source: `fd7973c6a978136d1154872162f772127c5ecfd9`. GitHub workflow `37891374599` successful. APK hash `132a5780d575bee9d243c9adccb35a953b42ad998760ef3711eaac620988b639`. For complete build history, known limitations, ownership and next device actions read `android/CHECKPOINT.md`. No source from Windows was modified.
