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
**v0.1.0 SOURCE CANDIDATE** — Python command-line acquisition tool committed; source compile and automated tests run in GitHub Actions. No Windows hardware/device/user test yet. The actual photogrammetry reconstruction engines remain **NOT IMPLEMENTED**. Keep automated tests separate from a user-verified baseline.

## License and reproducibility
Record external engine versions, installation methods, capabilities, license implications, exact commands (without secrets), and input/output file hashes. External engine programs are not vendored or downloaded without an explicit implementation need. Distinguish unavailable / planned / implemented / test-passed / user-verified.


## Run the first preview (Windows)

Install **Python 3.10+** and **FFmpeg/ffprobe** locally, ensuring \`ffmpeg\` is on PATH. Use a command prompt in the checked-out Slot-19 repository. No extra Python packages are required to run v0.1.0.

\`\`\`powershell
py -m videogrammetry check
py -m videogrammetry engines
py -m videogrammetry extract --project ".\MyScans" --recording "C:\Videos\object.mp4" --interval 1 --max-frames 120
py -m videogrammetry test-this-version --project ".\MyScans" --recording "C:\Videos\object.mp4"
\`\`\`

For a reachable live RTSP network stream (for example a phone/IP camera that **already** exposes RTSP on your LAN), use:

\`\`\`powershell
py -m videogrammetry extract --project ".\MyScans" --stream "rtsp://CAMERA_HOST/stream" --duration 30 --interval 1 --max-frames 60
\`\`\`

For a Windows camera, specify its **actual DirectShow device name**:

\`\`\`powershell
py -m videogrammetry extract --project ".\MyScans" --camera "Integrated Camera" --duration 20
\`\`\`

Replace sample filenames, camera names, and addresses with real local sources. Do **not** put passwords in commands you plan to share. RTSP/DirectShow live input is designed but **not yet verified with your camera or computer**.

Results live under \`MyScans/runs/<run-id>/\` including \`frames/\`, \`frames_manifest.json\`, \`events.jsonl\`, and \`run_result.json\`. Each new run gets a unique folder; the original video is preserved. The guide asks you to inspect representative frames and allows an explicit failure response. Diagnostic ZIP exports contain reports **without the source video or frame images**.

## Important first-version limitations

- v0.1.0 is a **command-line acquisition preview**, not yet a graphical scanning app.
- It extracts frames at a **fixed time interval**. Automatic sharpness/overlap filtering is a later milestone.
- A PASS means real frame files were validated. It does **not** mean that any 3D reconstruction has run.
- The \`engines\` command lists planned integrations, not working installations.
- Full photogrammetry and multiple engine comparison are subsequent milestones.
