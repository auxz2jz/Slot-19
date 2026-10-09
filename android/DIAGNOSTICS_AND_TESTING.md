# Android diagnostic coverage and testing — v0.1.0 candidate

**State:** implemented in Android source; compiled and unit-tested in GitHub run [37891374599](https://github.com/auxz2jz/Slot-19/actions/runs/37891374599). **Not yet verified on an actual phone.** Governing rules: Master Instruction Library `DIAGNOSTICS_STANDARD.md` and `GUIDED_TESTING_STANDARD.md`.

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
