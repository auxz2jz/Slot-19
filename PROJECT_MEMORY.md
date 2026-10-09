# Project Memory — Multi-Engine Video Photogrammetry Lab

Updated: 2026-10-08.

## Purpose
Independent second photogrammetry project using live camera/video streams, imported video recordings and existing photos to extract useful frame sets and compare multiple reconstruction engines. Preserve original captures; no runtime GitHub/cloud requirement.

## Platform and scope
**Windows-first desktop processing core (Python planned)**, with optional smartphone live feed over LAN and possible future Android UI. Architecture can run on other desktop OSes where its dependencies exist. Second platform implementation **NOT STARTED**.

## Status and version identity
- Overall: **INITIALIZED / PLANNED**, not an implemented program.
- Current version/build: **none**.
- **LAST USER-VERIFIED BASELINE: NONE**.
- **LATEST UNVERIFIED CANDIDATE: NONE**.
- First candidate target: `v0.1.0` (PLANNED, NOT BUILT).
- Commit/source hash of testable candidate: N/A.
- Last actual test result: none.
- Files/results received in **this** project: none. The separate Slot-8 project and its diagnostics are reference material, not this project's tests.

## Current task
Establish permanent initialization, plan input acquisition from video/live feeds, then implement a minimal independently testable ingestion/extraction candidate with diagnostics and guided testing. Real reconstruction engines are incremental later milestones.

## Implementation plan
1. Document source input contract, run/result status, and engine roles. **This documentation is initial checkpoint.**
2. Create a self-contained desktop frame-extraction runner (recorded video, live streams/camera where FFmpeg source is supported), with immutable project/run inputs and source frame manifest.
3. Add evidence-rich run diagnostics, export, and guided `Test This Version` steps that validate actual files.
4. Test on synthetic/known recordings and faulty inputs; produce `v0.1.0` candidate, never silently claim user verification.
5. Add frame quality/overlap selector, and implement genuine engine adapters one at a time with capability checks, comparably isolated runs, and validated outputs.

## Architecture and decisions
- Source frames are the shared boundary between acquisition and reconstruction; all engine comparisons reference the same immutable selected frame set.
- Group **COLMAP**; **OpenMVG→OpenMVS**; **AliceVision / Meshroom**; **MicMac**; and **ODM** as independent candidate pipelines where available. Meshroom is an AliceVision frontend/orchestrator, not a mathematically independent SfM engine.
- Include optional model/mesh tools **Manifold, Assimp, trimesh, meshoptimizer, MeshLab** in their correct postprocessing roles, not mislabelled photogrammetry solvers.
- Phone or handheld camera should move around a stationary, well-lit object by default. Do not confuse a rotating object against a static backdrop with a moving-camera reconstruction unless background masking/special handling is validated.
- Persist calibration/intrinsics provenance, resolution, timestamps, frame rejection reasons, and actual engine/tool versions.
- Explicit status for missing engines; no fake fallbacks/results.

## Known bugs, limitations, risks
- No software implemented yet; no bugs can be confirmed.
- Video is often blurrier and more compressed than still photos; frame extraction alone cannot restore lost texture/detail.
- Heavy engines require desktop CPU/GPU/RAM/storage; some stages depend on CUDA or platform-specific binaries.
- Capture clocks/timestamps, motion blur, duplicate views, lens calibration and changing focus/zoom can undermine reconstruction.
- Live phone streaming requires a reachable supported local video source; this project will not assume a particular third-party phone streaming app.
- Security/privacy: redact camera URLs including credentials; no raw video in diagnostics or automated upload.
- External engine license/version compatibility is to be checked before distribution/packaging.

## Failed approaches
None for this new project.

## Results and diagnostics
No build/test, diagnostics, or benchmark results yet. Do not reuse Slot-8 success/failure results as evidence for Slot-19.

## Exact next action
After this documentation checkpoint, implement **only v0.1.0 frame acquisition/extraction with validated output, persistent diagnostic trace, and basic guided testing**. Do not attempt all SfM/dense engines in one untestable change.
