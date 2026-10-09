# Project Memory — Multi-Engine Video Photogrammetry Lab

Updated: 2026-10-08.

## Purpose
Independent second photogrammetry project using live camera/video streams, imported video recordings and existing photos to extract useful frame sets and compare multiple reconstruction engines. Preserve original captures; no runtime GitHub/cloud requirement.

## Platform and scope
**Windows-first desktop processing core (Python planned)**, with optional smartphone live feed over LAN and possible future Android UI. Architecture can run on other desktop OSes where its dependencies exist. Second platform implementation **NOT STARTED**.

## Status and version identity
- Overall: **CANDIDATE v0.1.0** — a CLI frame extraction preview is implemented; no user verification.
- Current version/build: Python source v0.1.0; **no Windows binary, APK, or packaged installer**.
- **LAST USER-VERIFIED BASELINE: NONE**.
- **LATEST UNVERIFIED CANDIDATE: v0.1.0** — acquisition code commit `9025e96ada71a974204f0189ce0bfbe1328ede25`; GitHub CI run `37889805717` passed 6 tests.
- First candidate target: `v0.1.0` (SOURCE EXISTS / CI-TESTED / USER-UNVERIFIED).
- Candidate source identity: commit `9025e96ada71a974204f0189ce0bfbe1328ede25` plus subsequent documentation commits; no compiled release artifact hash.
- Last automated test: GitHub Actions `37889805717`, Python compile PASS, six unit/integration tests PASS including synthetic recorded-video decoding, invalid media failure, guided test results and ZIP export; no physical Windows or phone-camera test.
- Files/results received in **this** project: none. The separate Slot-8 project and its diagnostics are reference material, not this project's tests.

## Current task
Establish permanent initialization, plan input acquisition from video/live feeds, then implement a minimal independently testable ingestion/extraction candidate with diagnostics and guided testing. Real reconstruction engines are incremental later milestones.

## Implementation plan
1. Document source input contract, run/result status, and engine roles. **DONE.**
2. Create Python CLI frame-extraction runner with recorded video and designed live FFmpeg camera/stream handling, fresh run directories, output/sha/PNG validation and frame manifest. **CANDIDATE; recorded-video CI-tested, live device unverified.**
3. Add evidence-rich run diagnostics, audited ZIP export, and guided CLI `Test This Version` steps validating actual files with manual PASS/FAIL. **CANDIDATE; CI-tested.**
4. GitHub Actions synthetic recording, faulty recording, diagnostics, guided test and source-parameter tests: **6 PASS** on CI; v0.1.0 **candidate created**. No physical user verification.
5. Add frame quality/overlap selector, and implement genuine engine adapters one at a time with capability checks, comparably isolated runs, and validated outputs.

## Architecture and decisions
- Source frames are the shared boundary between acquisition and reconstruction; all engine comparisons reference the same immutable selected frame set.
- Group **COLMAP**; **OpenMVG→OpenMVS**; **AliceVision / Meshroom**; **MicMac**; and **ODM** as independent candidate pipelines where available. Meshroom is an AliceVision frontend/orchestrator, not a mathematically independent SfM engine.
- Include optional model/mesh tools **Manifold, Assimp, trimesh, meshoptimizer, MeshLab** in their correct postprocessing roles, not mislabelled photogrammetry solvers.
- Phone or handheld camera should move around a stationary, well-lit object by default. Do not confuse a rotating object against a static backdrop with a moving-camera reconstruction unless background masking/special handling is validated.
- Persist calibration/intrinsics provenance, resolution, timestamps, frame rejection reasons, and actual engine/tool versions.
- Explicit status for missing engines; no fake fallbacks/results.

## Known bugs, limitations, risks
- A command-line acquisition preview exists. No confirmed source bugs yet. No Windows EXE or Android APK; live RTSP/DirectShow/UVC requires device testing. Reconstructed 3D outputs are NOT implemented.
- Video is often blurrier and more compressed than still photos; frame extraction alone cannot restore lost texture/detail.
- Heavy engines require desktop CPU/GPU/RAM/storage; some stages depend on CUDA or platform-specific binaries.
- Capture clocks/timestamps, motion blur, duplicate views, lens calibration and changing focus/zoom can undermine reconstruction.
- Live phone streaming requires a reachable supported local video source; this project will not assume a particular third-party phone streaming app.
- Security/privacy: redact camera URLs including credentials; no raw video in diagnostics or automated upload.
- External engine license/version compatibility is to be checked before distribution/packaging.

## Failed approaches
None for this new project.

## Results and diagnostics
Synthetic video and error-path tests passed under GitHub Actions `37889805717`; test run contains 6 passes, 0 skips. No Windows/user hardware or reconstruction tests. Do not reuse Slot-8 evidence.

## Exact next action
**User/device test of v0.1.0 is next:** install Python + FFmpeg on a Windows machine, extract frames from a short handheld video, run `test-this-version`, and review/export diagnostics. Then inspect actual evidence, resolve any camera/recorded-input failures, and plan quality-aware keyframe selection before the first COLMAP engine adapter. No unverified candidate replaces the user-verified baseline (currently NONE).
