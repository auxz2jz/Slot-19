# Android project memory — Video Photogrammetry

**Date:** 2026-10-08. **Platform ownership:** exclusively `android/` plus `.github/workflows/android-build.yml`. Windows stays in historical root; never modify its source or checkpoints.

## Goal
Native, local-first Android photogrammetry capture application. Stationary object + handheld phone moving around it. Eventually manual still photos, automatic high-res photos, live/video selected frames, and Android-appropriate 3D reconstruction. Laser and hybrid are separate deferred modules. No markers or turntable required. No runtime GitHub/cloud requirement.

## Baselines and candidates (pre-implementation checkpoint)
- **LAST USER-VERIFIED ANDROID BASELINE: NONE.**
- Latest candidate: NONE before first source/build.
- First target **Android v0.1.0**: live camera preview with limited sampled frames; import local video and extract fixed-interval frames; save run frames and manifest; persistent diagnostics; guided test report/export.
- Device target: Android smartphone (Samsung Galaxy S22 Ultra Android 16 is an intended practical tester, but device not tested).
- Current task: build v0.1.0 Android debug APK via GitHub Actions and record commit, build evidence, artifact hash, user-test instructions.

## Architecture decisions
- Kotlin + Jetpack Compose interface; CameraX Preview + ImageAnalysis for live frames; Android MediaMetadataRetriever for recorded video. Local app-private project/run folders; user-selected SAF export for diagnostics. All outputs are candidates until physically validated. No PC-only SfM libraries forced onto phone. Separate shared neutral interface docs under `shared/`.
- First v0.1.0 is capture/diagnostics foundation, **not a complete scanner or 3D reconstructor**.
- Prefer real frame validity, not successful button tap, as PASS evidence. Privacy: no raw source videos/frames in diagnostic ZIP; no full private paths logged.

## Known limitations and failed approaches
- No prior Android code in Slot-19; no user tests; initial live capture and video extraction must be validated on real phone. First version may lack feature-based intelligent selection, calibration, SfM, point clouds, mesh and texture.
- No failed approaches at initialization; record subsequent failures with evidence.

## Files/results received
No Android device reports supplied for this new Slot-19 Android implementation. Slot-8 is separate read-only reference.

## Plan and exact next action
1. Checkpoint and isolate Windows source; record shared ownership and Android roadmap — DONE.
2. Implement first Android capture/diagnostics/guided test candidate under `android/`; create Android-only Actions workflow.
3. Compile/inspect actual CI results; smallest evidence-driven corrections; checkpoint.
4. User installs APK and tests video/live extraction, exported ZIP and visual frame quality. Only then may mark user VERIFIED.
