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
