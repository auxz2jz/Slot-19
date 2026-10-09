# v0.3.0 frame-rate comparisons and diagnostics — Android only

## User request and preserved baseline
All 3 v0.2 modes were exercised by the user and they report that they look fine. The supplied v0.2.0 `recorded_video` test ZIP for run `20261009T074031Z_c87a7f9c-2` contains 40 JPEG manifests (720x1280), 0–39s at requested 1 FPS, saved-run PASS, guided visual MANUAL_PASS, and no application errors. The ZIP is for ONE run, not all three: old per-run events are preserved in separate app-private folders. The user verbally confirms the other capture modes; no comparable ZIPs for those two are included in this latest upload.

Protect exact v0.2.0 source commit `21cc221cdd4421e3ee17c8167b970486cb38f365` on branch `backup/android-v0.2.0-user-verified-capture` and existing CI candidate backup. User verification applies to capture/display actions **only**, not meaningful view coverage, model fidelity, or 3D reconstruction.

## v0.3.0 feature work
- Three independent mode-specific settings presets (Live 1fps/30; Video 1fps/40; Smart 0.5 max photos/sec/30), editable by 0.5–5.0 fps slider and 10–300 total-frame slider. Smart remains novelty/quality driven; fps is *maximum shutter rate*, not a forced schedule. Manual mode samples from live camera; video samples requested source times.
- Settings written to individual run diagnostics, including requested FPS and max frame cap.
- After completion, record per-run saved count, elapsed processing, actual accepted/sampled FPS computed from source timestamps, total saved bytes, mean thumbnail brightness and edge-detail proxy where available, adjacent near-duplicate proxy count. No true geometric image overlap or 3D-quality claim. ImageCapture smart frame decisions already contain sharpness, brightness and view-change metrics.
- `Export ALL Runs + FPS Comparison` uses Android SAF to bundle every completed run's diagnostic files and `all_runs_summary.json`, without including private videos or frame images. Older single-run ZIP remains available. No per-run evidence is overwritten by subsequent captures. Rolling global action log is bounded and rotating.
- During video extraction existing UI refreshes saved-frame preview periodically. More FPS will increase disk, CPU/battery use and may not be achieved exactly under load.
- No changes to Windows, Slot-8, or actual 3D reconstruction.

## Comparability and experiment
Recommend start with existing recording and process it in Video mode at 0.5, 1, 2, and 3 fps under the same source. Choose enough maxFrames to cover the same duration for every trial; otherwise truncated runs are **not** directly comparable. For example a 30s video needs at least 91 frames at 3 fps. Inspect actual saved images for sharpness and useful viewpoints; use all-run comparison ZIP for measured capture spacing, brightness/edge proxy, duplicates, bytes and processing times. Smart capture is a qualitatively separate experiment because it saves full-resolution still photographs from live camera.

No algorithm can yet announce an empirically proven `best FPS` solely from these metrics; later geometry with registered cameras and reconstruction completeness should drive that recommendation. User retains final quality judgment.

## Verification gate
Android GitHub Actions compile + JVM tests must succeed on candidate source; source backup then device test. On-device tests: slider settings reflected in run manifest, 1/2/3fps output intervals for one recording, increased max frame count, three-mode regression, smart minimum-shutter-gap, ZIP all-runs contains 3+ independent run IDs and comparisons. The user confirms or reports failures. Then update Android-only checkpoint, preserve verified v0.2 fallback.
