# Multi-Engine Video Photogrammetry Lab

A **new, independent** local-first photogrammetry project. The existing Android 3D Scan Studio in `auxz2jz/Slot-8` is a read-only reference and must not be changed by this project.

## Mission
Use **recorded video, live camera/video streams, and optionally existing photographs** as sources of geometrically useful frames. Compare multiple real photogrammetry reconstruction engines on the *same immutable source frame set*, optionally recover/fallback after a failed run, and produce inspectable point clouds, meshes, textures, and detailed evidence about why a result passed or failed.

## Implementation choice
Start with a **Windows-first, cross-platform-capable Python processing core** to host desktop engines; support built-in Windows cameras, phone/IP-camera streams on the local network, and recorded videos when the sources are accessible to FFmpeg. A dedicated Android UI is **not** being implemented at initialization. Do not claim COLMAP, AliceVision, etc. run on Android.

The project MUST NOT require GitHub or cloud services during normal use. External reconstruction executables are optional local dependencies and must be detected and verified, not silently assumed installed.

## Start here
- `AGENTS.md`: mandatory instructions / ownership.
- `PROJECT_MEMORY.md`: live state, verified baseline, candidate, exact next action.
- `CHECKPOINT.md`: current recovery checkpoint.
- `ROADMAP.md`: priorities and phase gates.
- `ARCHITECTURE.md`: source, selection, adapters, engine catalog and design decisions.
- `DIAGNOSTICS_AND_TESTING.md`: diagnostic coverage map and guided test requirements.

## Current status
**PROJECT INITIALIZED — design/documentation only.** No first build, verified baseline, executable engine integration, or user device tests yet. Planned first candidate: **v0.1.0**. Keep candidate/verified status separate.

## License and reproducibility
Record external engine versions, installation methods, capabilities, license implications, exact commands (without secrets), and input/output file hashes. External engine programs are not vendored or downloaded without an explicit implementation need. Distinguish unavailable / planned / implemented / test-passed / user-verified.
