# Technical Architecture — Initial Design

## Boundaries

```text
[Video recording | live camera | RTSP/HTTP stream | photo folder]
                           |
              Acquisition / frame extraction
                           |
               Provenance + image validation
                           |
     Immutable source frames & selected-frame manifest
                           |
         Engine adapter / capability registry / scheduler
          |         |          |        |       |
        COLMAP   OpenMVG    AliceVision  MicMac   ODM
                    |        /Meshroom
                  OpenMVS
                           |
             Result validation and comparison
                           |
                3D cloud / mesh / texture
                    optional mesh tools
```

## Platform
Windows-first local processing with Python for orchestration and UI. Command-line/protocol adapters call only **locally installed** external binaries, without shell injection. FFmpeg/ffprobe are provisional acquisition dependencies to be validated. A dedicated phone UI is not part of initial development; a phone may feed a reachable local stream. No GitHub/cloud service required at runtime.

## Acquisition modes
1. **Recorded video:** project imports video by reference, decodes a sample of frames at specified interval; keeps original unchanged.
2. **Live local camera:** platform-supported webcam or USB UVC input, bounded by capture duration/frame count, implemented only for verifiably supported device names.
3. **Live network video:** FFmpeg-compatible RTSP/HTTP source reachable on the LAN. Credentials are never saved unredacted to logs/manifests.
4. **Still photographs:** future, or as engine input after explicit validation.

Each run gets a unique ID, separate directory, UTC and monotonic timing, source type, sanitized metadata, output manifest, and original frame hash/size when feasible. Re-runs never overwrite earlier runs or original media.

## Selection
Start with regular time interval (v0.1.0). Later quality-aware keyframe selection should measure sharpness, exposure, temporal separation, motion and feature overlap, and should record why each frame is accepted/rejected. Higher frame count is not proof of greater geometric coverage; never promise that video frames outperform well-taken original still photos.

## Camera/motion geometry
Default capture technique: **move the camera around a stationary object** with diffuse stable illumination, slow motion, overlapping views, focus/exposure/zoom locked where possible, and coverage at varied elevations. Rotating objects on a turntable with a fixed scene/background requires verified subject masking / alternate handling; no unsupported assumption that ordinary SfM will disregard the stationary background.

## Engine catalog and actual capabilities
Status of EVERY external engine at initialization: **PLANNED / NOT CONNECTED / NOT TESTED**. The following names are goals, not claims of integrated support.

| Engine/tool | Intended stage | Notes |
|---|---|---|
| COLMAP | SfM, image registration, dense processing where supported | First pipeline adapter; CPU/GPU capabilities differ |
| OpenMVG | camera geometry / SfM | Produces inputs convertible for OpenMVS |
| OpenMVS | dense point cloud, mesh, texture | Downstream of compatible SfM geometry |
| AliceVision | SfM, MVS, meshing/texturing stages | Meshroom leverages AliceVision; avoid counting twice as independent algorithm |
| Meshroom | AliceVision workflow orchestration | Adapter may use Meshroom CLI and/or AliceVision binaries |
| MicMac | photogrammetry CLI pipeline | Research/version validation before adapter |
| OpenDroneMap (ODM) | integrated SfM/MVS/texturing | Optimized for aerial/geospatial workflow; optional close-range experiments |
| Manifold | mesh geometry/repair | Not a photogrammetry SfM solver |
| Assimp | import/export model interchange | Not an SfM solver |
| trimesh | mesh processing/validation | Not an SfM solver |
| meshoptimizer | mesh optimization | Not an SfM solver |
| MeshLab | mesh inspection/processing | Not an SfM solver |

Repository reference: `auxz2jz/Slot-8/ENGINE_INTEGRATION.md` (read-only). Other engine names/alternatives must first be assessed for actual role, license and platform support.

## Engine adapter contract (planned)
- `probe()`: binary path, version, platform, GPU/CUDA and required dependencies -> available/unavailable + reason.
- `prepare(run, frames_manifest)`: immutable inputs and isolated engine work folder.
- `execute()`: argument array (no shell); operation/run ID, captured process output, progress and timeout/stall monitoring, cancel handling.
- `inspect_results()`: verify required artifacts are present, nonempty, parseable, geometrically plausible within explicit thresholds; report counts and caveats.
- `export()`: normalized project-relative artifact references; never invent formats/quality metrics.
- `failure()`: first failed precondition/command/stage, exit status, stderr context, and error trace.
Compatibility converters are explicit, versioned stages with unit tests, not guessed file renames.

## Comparisons and fallback
Run engines independently on **the same selected-frame manifest**, with isolated workspaces, quality metrics and provenance. Automatic fallback is optional and must record failure reasons; never silently claim a different engine completed the requested engine's output. Expose result uncertainty and subjective appearance for user confirmation.

## Security and resource safety
No secrets/raw credentials in diagnostics; private frames remain local; explicit user-driven export; bounded disk/log growth; preflight storage, RAM, and available engine capabilities; cancellation and timeout handling; no automatic massive engine installation. Packaging/license review is a prerequisite for redistribution.
