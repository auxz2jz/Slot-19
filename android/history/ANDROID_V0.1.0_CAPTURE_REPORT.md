# Archived pre-v0.2.0 capture checkpoint

This document retains the historical v0.1.0 checkpoint text exactly as it stood before the new Smart Auto Capture release; its earlier verification statements were superseded by the user confirming the recorded video's final displayed image looked correct. See `../CHECKPOINT.md` for latest truth.

---

# Android recovery checkpoint — v0.1.0 source and APK candidate

**As of:** 2026-10-08 PDT (GitHub Actions build occurred 2026-10-09 UTC)

## Exact independent Android status
- Platform owner: Android worker/chat; owned files under `android/`, workflow `.github/workflows/android-build.yml`.
- Version: **Android v0.1.0** / `versionCode 1` / package `com.auxz2jz.videogrammetry`.
- **LAST USER-VERIFIED ANDROID BASELINE: NONE.**
- **LATEST UNVERIFIED ANDROID CANDIDATE: v0.1.0.** Build success is NOT user verification.
- **Exact code commit tested:** `fd7973c6a978136d1154872162f772127c5ecfd9`.
- **Candidate preservation branch:** `backup/android-v0.1.0-ci-candidate`, from tested source commit above.
- **Build:** GitHub Actions run [37891374599](https://github.com/auxz2jz/Slot-19/actions/runs/37891374599), **SUCCESS**. Tasks `testDebugUnitTest` and `assembleDebug` passed. APK file existence, archive upload, and SHA-256 check successful.
- **GitHub artifact:** `video3d-android-v0.1.0-candidate` (artifact ID `11597604313`).
- **APK inside artifact:** `Video3DCapture-Android-v0.1.0-CANDIDATE.apk`, 11,550,489 bytes.
- **APK SHA-256:** `132a5780d575bee9d243c9adccb35a953b42ad998760ef3711eaac620988b639`.
- **Android device install/manual test:** NOT YET PERFORMED BY USER. CameraX preview and sampling, MediaMetadataRetriever actual real-device video decoding and test ZIP export require physical testing.

## Features in candidate
- Compose Android screen with `Enable Camera`, `Start Live Sampling`, `Stop Live Sampling`, `Choose Video`, `Test This Version`, `Frames Look Correct`, `Expected Behavior Failed`, `Export Test + Diagnostics`.
- Live CameraX preview + ImageAnalysis samples up to 30 JPEGs ~1.2 seconds apart. Sampled frames come from raw analysis, not annotated UI.
- Video importer uses Android document picker and MediaMetadataRetriever; samples up to 40 JPEGs ~1 second apart, max width 1280; keeps original recording untouched.
- Isolated app-private capture runs, JPEG dimensions/hash verification, `manifest.json`, `result.json`, `events.jsonl`, bounded global action trace, user testing result, redacted diagnostic ZIP.
- No quality/overlap keyframe selection, 3D reconstruction, mesh, texture, laser, or engine integrations yet.

## Build/debug history
1. Workflow run `37891046767`: FAILED in upstream Android SDK setup action (obsolete `tools` package), before app compile. Resolved by using preinstalled SDK.
2. Workflow run `37891097406`: FAILED Kotlin compilation on missing `androidx.activity.compose.setContent` import. Added exact missing import.
3. Workflow run `37891374599`: SUCCESS, Android unit tests + APK assembled and uploaded.
No speculative rewrites were used; this is not a repeated two-failure approach.

## Windows preservation
- Historical Windows source, tests and root Windows project-memory/checkpoint file blob hashes unchanged during this Android work.
- Windows pre-Android protected snapshot: `backup/pre-android-windows-v0.1.0` at `aaec898cf2fadf95760aaedc436d0f7062bd7fbe`.
- Windows v0.1.0 source candidate remains unverified by user; Android APK does not replace it.
- See `windows/OWNERSHIP.md` and `shared/`.

## Files/results already received
- No user Android device diagnostics, video samples or visual confirmations received.
- Android GitHub workflow results and APK artifact identified above.

## Exact next action
**The user installs Android v0.1.0 candidate on a supported device** and exercises: camera permission, live preview, Start/Stop Live Sampling, Choose Video, inspection of saved frame orientation/colors, Test This Version (manual PASS or FAIL), and Export Test + Diagnostics. User sends failures/report. Confirm physical user-verified baseline only after explicit user testing and confirmation. Then address first real failures or plan intelligent keyframe selection. Do NOT begin laser or PC engine work here.

## Device test evidence received — 2026-10-09 UTC

Two user-uploaded Android v0.1.0 diagnostic ZIPs were inspected and found to be structurally valid (ZIP archive integrity checked); neither includes original source images/videos. They report no camera processing or decoder ERROR events. **The ZIPs document in-app output validation; they do not independently contain saved JPG bytes for external image/geometry verification.**

### Live CameraX run
- Submitted diagnostic ZIP: `Android-v0.1.0-test-diagnostics.zip`, SHA-256 `5c10e16b58e31fa712010d1d702b520c3636c6efcba210b6a0a7a51be066dcf2`.
- Run ID `20261009T062629Z_0565430e-f`; 30/30 frames; each JPEG recorded as 1088×1088. First-to-last sampled span 35.284 s; source timestamp interval mean ~1216.7 ms (range 1201–1242).
- Saved-run result `PASS`, outputValidated=true, manifest 30 entries. Guided `TEST_RESULT=PASS`, automatic validation=true, `visualResultSource=MANUAL_PASS`, user selected **Frames Look Correct**.
- Treat live frame capture and user-reported visual acceptability as **positively tested**, limited to this run. No evidence about full scene coverage, accurate camera intrinsics, geometry, or reconstruction. Square aspect ratio warrants explicit crop/coverage review before any quality claims.

### Recorded video run
- Submitted diagnostic ZIP: `Android-v0.1.0-test-diagnostics (1).zip`, SHA-256 `e57b69c526001c87baa8b93ef6d463d7c74eea3e20f9c63c02d6e9d5d626b8b1`.
- Run ID `20261009T062920Z_06fedd2f-2`; 40/40 decoded frames; each JPEG 720×1280; first-to-last requested positions span 39.000 s, with 1 s spacing; `unavailableFrames=0`.
- Saved-run result `PASS`, outputValidated=true; **no `test_results.json` or manual visual confirmation exists for this recorded-video run**. Automated recorded-video extraction is positively tested; visual quality remains unverified.

### Found diagnostics limitation
- Each ZIP contains `USER_ACTION EXPORT_TEST_AND_DIAGNOSTICS` and `OPERATION_START DIAGNOSTIC_EXPORT` but not `EXPORT_RESULT`. Both are valid ZIPs, supporting that actual user-driven export completed. In v0.1.0 the completion event is logged **after** closing the ZIP, so it is not inside its own snapshot. Fix in a future small, testable diagnostic update; do not change successful capture behavior merely to solve the reporting gap.
- Video input sampling is fixed at max 40 frames. A 40-frame pass confirms the first requested 39-second span, not the entire duration if the source video is longer.
- App-generated JPGs are not in either export; raw pixel/color/sharpness, FOV and scene overlap cannot be independently verified here.

### Baseline classification and exact next action
- **Last fully user-verified Android release baseline remains NONE**, pending the user's confirmation that both live and recorded modes look correct. Live in-app guided test has manual PASS; recorded-video has automatically verified capture only.
- **Candidate remains Android v0.1.0** at tested code commit `fd7973c6a978136d1154872162f772127c5ecfd9`; protected backup branch `backup/android-v0.1.0-ci-candidate`; APK SHA-256 unchanged. Do not rebuild or increment version for documentation-only test recording.
- Next: request/receive the user's visual assessment of recorded video output and whether 1088×1088 live captures crop content; optionally test a separate short video to evaluate sampling range. Once user confirms both core capture workflows, mark that scope as last user-verified baseline. For a future candidate, fix export self-diagnostics with a targeted regression test and add coverage-aware frame selection. Windows-owned files remain untouched.
