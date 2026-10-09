# Android diagnostic coverage and testing — v0.3.0 candidate

**State:** Android v0.2.0 basic capture/display physically user verified. New v0.3.0 adjustable rate, larger frame counts and combined-export features built and unit-tested in [GitHub Actions 37901552835](https://github.com/auxz2jz/Slot-19/actions/runs/37901552835); **new settings and comparison feature not yet verified on a device**. Governing rules: Master Instruction Library `DIAGNOSTICS_STANDARD.md` and `GUIDED_TESTING_STANDARD.md`.

## Actual implemented UI and operations
- `Enable Camera` when camera permission missing: system runtime permission prompt.
- CameraX preview on the main screen: CameraX Preview and independent RGBA ImageAnalysis pipeline, no overlay baked into source.
- `Start Live Sampling` / `Stop Live Sampling`: capture max 30 JPEG frames approximately every 1.2 seconds, isolated run.
- `Choose Video`: Android Storage Access Framework document picker; MediaMetadataRetriever decodes up to 40 frames at requested 1-second intervals into validated JPEGs. Originals remain unchanged.
- `Test This Version`: presents concrete user instructions; `Frames Look Correct` or `Expected Behavior Failed` stores result with objective frame/hashes validity.
- `Export Test + Diagnostics`: user selects ZIP destination via system document creation; report files exported without raw source recording or camera image. Missing/cancelled picker has no successful export claim.

## Implemented Diagnostic Coverage Map

| Operation | Trigger and attempt | Actual success signal | Failure and recorded evidence | Device test |
|---|---|---|---|---|
| Camera permission/preview | Enable Camera + bind Preview/ImageAnalysis | CameraX bind succeeds; independent visual inspection needed | caught bind exception / permission denial | inspect live screen |
| Live sampling | Start Live Sampling -> analyzer RGBA frames | actual saved JPGs, decoded dimensions, SHA-256 hashes, matching manifest and result | save/analysis exceptions or no frames; FAIL result | walk around stationary object, Stop Live Sampling |
| Recorded video | Choose Video -> MediaMetadataRetriever | saved JPG frame count >0, dimensions/hash validated | invalid duration, decoder or IO exception; FAIL report | select existing short local video |
| User stop / auto limit | Stop Live Sampling or hit 30 samples | run actually finalized and validated | invalid/missing frames; report FAIL | inspect count and saved preview |
| Test This Version | open guide, manual visual PASS/FAIL | saved run must be objective PASS and user manually confirms visual accuracy | user reported failure, no completed run, invalid saved files | use both controls meaningfully |
| Export | choose ZIP destination | ZIP stream closes and bytes written; EXPORT_RESULT event | open/write error: ERROR event | export/inspect/share ZIP |

Event file: `capture_runs/<run-id>/events.jsonl`; persistent bounded trace `diagnostics/actions.jsonl` with rotated previous file. Each event carries event/category, user operation, elapsed monotonic and timestamp, appVersion, run correlation and JSON details. Runs retain `frames/`, `manifest.json`, `result.json`, and optionally `test_results.json`/`test_report.txt`. Private paths and source URIs are not deliberately logged.

## Guided Test This Version — phone procedure
1. Install candidate and launch. Tap **Enable Camera**; authorize camera. Verify preview visibly updates. If not, record a Problem.
2. Stand by a stationary object and move phone slowly around it. Tap **Start Live Sampling**, then **Stop Live Sampling**; check nonzero saved frame count and latest frame preview. No full-resolution stills, 3D model or laser scan is expected.
3. Tap **Choose Video** and pick a short recorded clip of the stationary object while the camera moved. Wait for **Saved and validated N frames** and examine the displayed sample image. This is only fixed-interval sampling, not intelligent keyframes.
4. Tap **Test This Version**. Choose **Frames Look Correct** only if output evidence and visual image appear correct; otherwise choose **Expected Behavior Failed**. This stores a device test report.
5. Tap **Export Test + Diagnostics**, save the ZIP, verify it opens, and provide it to developer if anything is wrong. No original camera frames/videos are included.

## Known v0.1.0 gaps (not misrepresented as complete)
- CI currently compiles/tests pure frame interval policy and assembles APK. **No emulator/device camera, decoder or SAF export instrumented test yet.**
- Guide primarily verifies the latest completed run and shows latest saved frame; it does not evaluate all keyframes' geometric overlap. A good last frame does not prove that the scan covers a complete object.
- Live frame RGBA colors/orientation and recorded decoder support remain phone-specific verification targets.
- No dedicated stall watchdog for video decoder, live capture lifecycle or frame count; single-thread queue and bounded frames, with captured caught exceptions.
- ZIP contains existing event log at export start; final `EXPORT_RESULT` event is logged after closing the ZIP and may not be in that particular export. This is a documented diagnostics self-export coverage gap to address.
- Test dialog session is not yet resumable; the saved run and manual result persist. No 3D reconstruction tests exist yet.

## Failure correction
Read run result -> first abnormal `events.jsonl` event -> error type / failing frame -> minimally scoped correction -> targeted CI build -> physical user retest. If essentially identical approach fails twice, STOP and reassess; follow three-failure escalation. No candidate becomes a verified baseline merely by successful APK build.

## v0.2.0 candidate addendum — Third mode Smart Auto Capture

This section supersedes v0.1.0-only labels above. Candidate source `21cc221cdd4421e3ee17c8167b970486cb38f365`; GitHub Actions `37894898716` SUCCESS; user device test PENDING. Last physically user-verified capture baseline is **v0.1.0**, which the user reported working for live and imported-video preview.

### Additional actual UI
- `Start Smart Auto Capture`, `Stop Smart Auto Capture`, approximate progress bar and free-text image quality / next-view guidance.
- These are additive to `Start Live Sampling`, `Stop Live Sampling`, `Choose Video`, `Test This Version`, `Frames Look Correct`, `Expected Behavior Failed`, and `Export Test + Diagnostics`.
- During recorded-video extraction, latest saved JPEG preview updates after first/every five extracted frames.

### Diagnostic coverage (new real code)

| Operation | User action or automatic trigger | Actual success condition | Failure / warning | Guided device check |
|---|---|---|---|---|
| Smart availability | bind optional CameraX ImageCapture use case | ImageCapture is bound without breaking v0.1 Preview/ImageAnalysis | third use-case unavailable; button disabled, original modes must remain | try all three modes |
| Smart run start | tap Start Smart Auto Capture | new non-overwriting run, `USER_ACTION` + `OPERATION_START` logged | another active mode/permission issue | start; inspect state |
| Analyze preview | periodic 64x48 raw RGBA sampling | score luminance, edge sharpness proxy, motion and image novelty | `FRAME_DECISION` with `TOO_DARK`, `TOO_BRIGHT`, `SOFT_IMAGE`, `CAMERA_MOVING`, `COOLDOWN`, `SAME_VIEW`, `POSSIBLE_LOST_OVERLAP`, `READY` when reason changes | stationary, slow move/hold, bad light |
| Automatic shutter | selector READY + no photo in flight | CameraX ImageCapture JPEG is physically saved, decodes, has positive dimensions and length; hash/metadata in manifest | `SMART_SHUTTER` / `SMART_IMAGE_CAPTURE_VALIDATE` error; run cannot pass if shutter error | confirm saved count and full-res dimensions |
| Stop/limit | Stop Smart Auto Capture or 30 photos | complete run `OUTPUT_VALIDATION` / `OPERATION_RESULT` with actual saved/verified JPEGs | if 0 frames or any capture error, FAIL | stop/review |
| Guided test / diagnostic ZIP | existing Test This Version buttons | objective output validity AND user's manual visual confirmation; ZIP readable, no raw JPGs/video included | explicit manual FAIL; ZIP self-export-completion event may not be included | export and upload ZIP |

### Limitations
No true geometric overlap verification or orientation/translation estimate. A progress bar does not establish 3D scene coverage. The CameraX full-resolution still and EXIF orientation must be physically reviewed. GitHub's JVM selector tests do not verify real camera sensor behavior. The known v0.1.0 diagnostics ZIP completion-event limitation remains. Tune thresholds only from measured runs; preserve last verified v0.1.0 source. See `CHECKPOINT.md` for exact build/recovery record.

## v0.3.0 actual new diagnostic operations and coverage

| Operation | Trigger | Accepted proof/metrics | Failure evidence |
|---|---|---|---|
| Select mode and change rate/limit | Live/Video/Smart chips and FPS/maximum sliders | Requested FPS and frame cap serialized in each new run's CAPTURE_SETTINGS event, manifest and result | Invalid 0.5–5fps or 10–300 bound rejected by CaptureOptions, tests |
| Live/Video rate execution | Press Start Live Sampling / Choose Video | Actual JPEG manifests, sourceTimeMs differences, measuredFps, saved count and processingElapsedMs; per-frame mean brightness, sharpness edge proxy, estimated near-duplicate adjacent pairs | Lower measured rate, partial result, bad JPEG/hash, missing frames or exception |
| Smart maximum shutter rate | Start Smart Auto Capture | No automatic shutter more frequently than requested cap; accepted JPG validated with novelty, brightness, stability and detail proxies | Photo unavailable, too-similar view, poor light, motion/rejection, shutter exception, no saved photos |
| Export ALL Runs + FPS Comparison | System ZIP picker | all_runs_summary.json with every completed run ID; separate files under runs/<run-id>/; user verifies ZIP opens | Empty history, storage permission/ZIP error |
| Latest run report | Export Test + Diagnostics | Only latest run, existing immutable run ID and captured settings | Post-closed ZIP may omit its own EXPORT_RESULT event; older known limitation |

**Rate honesty:** requested vs `measuredFps` (derived from gaps in saved `sourceTimeMs`) are separate. Recorded-video timestamp spacing does not measure how quickly MediaMetadataRetriever decoded frames or whether multiple samples decode to duplicate content. `nearDuplicateAdjacentProxyCount`, `sharpnessProxy`, and `brightnessMean` are small-thumbnail appearance proxies only. There is no true overlap score, camera-pose registration, sparse/dense point cloud, or engine-level validation. No program can yet assert the globally “best FPS” from these proxies alone.

**Controlled rate experiment:** import the SAME short video with matched temporal coverage at requested 0.5, 1, 2, 3 FPS and sufficiently high maxFrames (e.g., 100 for a 30-second recording). Confirm images visually, make a manual PASS/FAIL with Test This Version, export ALL Runs, compare saved counts, source timestamp spacing, duplicate proxies, brightness/detail proxies, bytes and processing time. Test video imports and live sampling separately; Smart Auto Capture is driven by novelty and quality and is not a fixed FPS test.

**Persistence:** each unique run under app-private capture_runs persists unless app data is cleared. Global events are capped/rotated independently. Every single-run diagnostic export represents one run. The new all-run export includes all completed runs without camera image/video bytes. APK debug signing may differ between builds; backup/export runs before uninstalling.

**Outstanding:** add true image correspondence/inlier/RANSAC quality measurements, user-selectable per-frame preview/contact sheet, export completion proof in ZIP, Android instrumentation tests across modes and frame-rate limits. Preserve verified v0.2 baseline when implementing these future features.
