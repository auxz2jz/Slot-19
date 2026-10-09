# Android diagnostic coverage and testing — v0.4.0 ORB geometry candidate (v0.3 capture verified)

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


## v0.4.0 ORB/Hamming + RANSAC geometric consistency (EXPERIMENTAL)

**User-verified version is v0.3.0 capture/settings only.** Source `8c61027d3eb83efcfa1bf8d4df4caf8631f2b262` is preserved. New v0.4 analysis is not device-verified. See `android/ORB_GEOMETRY_PLAN.md`.

### Actual button and outputs

- Complete one of the three existing capture modes; leave a valid `result.json` with at least two frames. Press **Analyze Latest Run — ORB Geometry**.
- The app uses a *separate thread* and keeps an on-screen per-pair progress bar. The underlying captured JPGs, manifest, file hashes, capture validation PASS and the old frame-rate settings are not changed.
- Only saved JPEG image bytes, not browser/UI screenshot overlays, are analyzed. Per pair: resize to at most 640px max dimension, detect ≤~800 ORB keypoints, derive binary descriptors, knn BFMatcher (Hamming) with 0.75 ratio test, fundamental-matrix RANSAC with 1.5px threshold and 0.99 confidence.
- Bounded at most 80 image *pairs*, selecting indices from first to last. For each evaluated pair the `geometry_pairs.jsonl` stores frame indices/names, keypoints A/B, ratio-test match count, F RANSAC inlier count, ratio of inliers to ratio-test matches, conservative status and explanatory limitation.
- Aggregated `geometry_report.json`: source frame count, pair count, skipped comparisons, epipolar-consistent count, weak/unavailable count, accumulated matches/inliers, `COMPLETED` status, timestamp/analysis ID, and caveat that this does not prove object-only overlap or 3D reconstruction. Failures use `geometry_last_failure.json` and persistent `ERROR` event; a bad geometric estimate does not mark an otherwise valid captured run FAIL.
- Both Export Test + Diagnostics and Export ALL Runs + FPS Comparison include geometry report/pair JSONL alongside original capture reports; original photos/videos are excluded. Old v0.3 runs may be analyzed *if* existing app-private storage survives APK update.

### Guided physical tests

1. With a textured stationary object, capture ~10–30 neighboring photos or video frames while moving the camera slowly. Tap Analyze Latest Run. Record number of analyzed pairs, supported pairs and warnings; inspect JSONL.
2. Capture a nearly unchanged viewpoint. Features might match consistently even with no parallax! **Do not interpret good RANSAC inliers as successful triangulation or camera translation.** Compare with differing views.
3. Capture a featureless/blurred surface or rapid movement; should result in an honest `TOO_FEW_FEATURES`, `INSUFFICIENT_MATCHES`, or `RANSAC_REJECTED`, not an invented success.
4. Export diagnostics; inspect `geometry_report.json` and `geometry_pairs.jsonl`; if native OpenCV cannot load, capture should still work and geometry failure should be visible/logged.
5. Re-test all three capture modes, FPS sliders, latest-run ZIP and all-run ZIP to rule out regressions.
6. Explicitly report visual result and whether Android OpenCV works on device; only then mark v0.4 user-verified.

### Diagnostic honesty and remaining work
- Fundamental RANSAC inliers are **2D epipolar-consistency checks**, not actual 3D points, real angular displacement, intrinsic-corrected camera poses, pure object mask overlap, coverage percentage, or successful point-cloud/mesh generation.
- Flat scenes, repeated texture, dominant background features, pure camera rotation and uncalibrated intrinsics can yield high epipolar inliers with poor SfM triangulation; F may reject legitimate low-baseline views. `EPIPOLAR_CONSISTENT` is a *pair verdict*, not a user scan PASS.
- JPEG Smart capture EXIF orientation is not yet normalized before ORB matching; check physical orientation in device tests.
- The existing Test This Version manual `Frames Look Correct` validates capture results only; it does not automatically confirm geometry report quality. Geometry analysis must have its own COMPLETED status and the user must inspect it separately.
- CI compiles tests on JVM including pure `GeometryPolicyTest`. There is **no OpenCV native end-to-end instrumented test** in GitHub CI; runtime load and output meaningfulness are user-device test targets.

## v0.5.0 first sparse two-view reconstruction — **USER UNVERIFIED**

**Actual UI:** `Analyze Sparse 3D — Two Views` available after valid completed run with at least 2 frames; separate executor and progress for ≤7 anchor pairs. `Export Sparse PLY — Experimental` only after candidate PLY exists. Existing capture buttons/ORB geometry analysis unaffected.

**Diagnostics:**
- `ANALYZE_SPARSE_TWO_VIEW` user action and correlation/analysis ID; bounded `ANALYSIS_PROGRESS`; `ANALYSIS_RESULT` `SPARSE_CANDIDATE`/ `INCONCLUSIVE`; `ERROR` and `sparse_last_failure.json` on exception.
- `sparse_report.json`: per-pair indices, ORB keypoints/matches, pose cheirality inlier count, estimated focal/principal point, accepted 3D points, median parallax and reprojection errors, conservative pass/fail reasons, selected pair when accepted.
- Camera intrinsics are **guessed**, lens distortion is untreated, 3D scale unknown, only ONE pair triangulated. Positive RANSAC/cheirality does NOT establish accuracy or object-only geometry. A very featureless, planar, static or rotated-only sequence may produce `INCONCLUSIVE`. Reports are included in both ZIPs but PLY and raw frames remain excluded for privacy, unless the user specifically selects PLY output through Android SAF.
- Guard against false positives: adequate keypoints/Hamming ratio matches; Essential RANSAC + recoverPose; positive depth in two camera frames; minimum parallax and valid projected pixel error; select best qualified pair or decline. Tests for purely geometric policy are JVM; OpenCV native correctness and actual PLY appearance require real device verification.
- **Manual test:** first verify old Live, Video, Smart and ORB features. Capture ~20–30 overlapping moving-camera views of a textured object. Tap sparse analysis, inspect status and report. If candidate, export PLY, open in 3D point viewer and inspect shape (relative arbitrary coordinates). Repeat with static and featureless object; do not claim PASS simply from button taps. Send full diagnostic ZIP plus visual result. Only user physically confirming qualifies as a verified baseline.


## v0.6.0 native sparse viewer + opt-in checkerboard calibration (candidate)

**Platform-specific capabilities:** `View Sparse Points — 3D` opens a native Android Canvas PLY point view; `Saved scans` menu selects retained app-private sparse PLYs; `Open a PLY File to View` uses Android OpenDocument; drag rotates, pinch zooms, Reset View restores position, Point size slider changes rendering, Monochrome/Point Colors toggles RGB, Focus Cluster/Show All changes outlier display. Parser supports ASCII PLY v1.0 XYZ and optional RGB (flexible column order), refuses malformed/binary/nonfinite coordinates or more than 50,000 vertices. No upload/network requirement. Viewer selection/error events logged against latest run, with selected original run ID metadata (not absolute path). Viewing points does not validate 3D accuracy.

**Calibration:** `Calibrate Using Checkerboard Photos` chooses 8–40 images through Android OpenMultipleDocuments. OpenCV `findChessboardCornersSB` searches exactly **9×6 inner intersections = 54** at known **25.0mm pitch** on a **10×7 = 250×175mm square-cell board**. Same lens/camera/zoom, fixed focus mode and mixed viewpoints are essential. `calibrateCamera` estimates intrinsics and distortion, writes RMS and per-photo accept/reject without storing picture content. Attempts/profile/events saved under app-private `camera_calibration`; **Export ALL Runs** bundles redacted calibration reports so users can upload diagnostics. Profile remains **UNAPPLIED** to sparse reconstruction until camera crop/lens compatibility is checked; a profile with low RMS alone is not a fully verified calibration.

**Guided physical test:** (1) Verify 3 prior capture modes and old ORB/sparse buttons. (2) Open saved PLY, rotate with touch, zoom with pinch, change color/point size, Focus and Show All, switch scan, Reset View. (3) Import an exported ASCII PLY and verify correct colored point count; malformed file should produce an explicit error without breaking app. (4) Measure printed board square with ruler; take 12–20 bright, sharp 9×6 shots at varying tilts with board fully visible and same camera/lens/zoom. (5) Use import picker; inspect accepted count and reprojection RMS. (6) Try no-board/blurred views; expect rejections without fictitious calibration PASS. (7) Export ALL-run ZIP and review `calibration/last_attempt.json` plus `calibration/events.jsonl`. (8) Record visual human PASS/FAIL; candidate is not verified until user explicitly confirms.

**CI tests:** `PlyParserTest.kt` tests exact PLY generated by SparsePolicy, flexible column order, corrupt/binary rejection, and checkerboard inner-corner/size constants. Android Actions `37914986139` compiled, ran all JVM tests and produced ARM64/universal APKs PASS. **No Android native viewer UI instrumentation or actual calibration-photo correctness tested by CI**. Keep v0.4 fully verified fallback, v0.5 device-validated sparse export and v0.6 CI candidate separate.


## v0.6.1 dynamic export version naming & provenance tests

**Observed bug:** on installed v0.6.0, Export Sparse PLY picker suggested an old `v0.5.0` filename and new reports/ZIP summary incorrectly carried hard-coded `android-0.5.0` labels. User verified the actual capture, PLY viewer and calibration app operations work. This fix does NOT alter source photographs/geometry or retag historical saved data.

**New behavior:** all three Android CreateDocument actions calculate their suggested name from the actual installed `BuildConfig.VERSION_NAME`, with safe run ID and UTC-millisecond timestamp (`ExportNames`). PLY: `Android-v0.6.1-sparse-two-view-<runId>-<timestamp>.ply`; latest ZIP analogous; ALL ZIP `Android-v0.6.1-ALL-run-comparison-<timestamp>.zip`. User may edit picker name; `EXPORT_NAME_SUGGESTED` event distinguishes suggestion from actual successful export.

Both new diagnostic ZIPs include `export_metadata.json` specifying `exportingAppVersion`, `exportType`, `exportCreatedUtcMs`; run ZIP includes `originalCaptureAppVersion`. ALL summary has `originalCaptureAppVersion` per run and `exportedByAppVersion`. Prior saved manifest version remains unchanged; newly created manifest/ORB/sparse/calibration/event metadata records runtime version.

**JVM tests** `ExportNamesTest` cover all three output modes, v0.6.1 future v0.7.0 propagation, malformed run identifier rejection, and repeated export uniqueness. Github Actions uses Gradle versionName to name APK files and uploaded artifacts. Physical test after CI: export two PLY files and two ZIPs (latest and all) and confirm version + timestamp changes; unzip exports to inspect provenance; regress PLY viewer, Smart/Live/Video, checkerboard detection. App v0.6.1 remains candidate until user confirmation. Keep verified v0.6.0 fallback.


## v0.7.0 third-view geometric evidence — unverified physical candidate
After existing **Analyze Sparse 3D — Two Views** creates a PLY, tap **Verify Sparse Points in Third View**. Program independently recomputes original first+selected source pair to recover anchor ORB feature-index/3D tracks, finds those same feature descriptors in up to four later saved frames, uses OpenCV `solvePnPRansac` with guessed same-camera intrinsics, counts 3D→2D inliers, computes median inlier reprojected pixel error and records per-third-view verdict. This is not triangulation of a new cloud, no combined point file, no bundle adjustment, no object-only masking, no physical scale. Good PnP means some 3D points reproject consistently in another frame, NOT accurate metric model.
- `THIRD_VIEW_SUPPORTED`, `INCONCLUSIVE`, `FAILED` report status independent of capture PASS.
- `third_view_report.json` includes baseline run pair, attempted third frames, shared tracks, inliers, median reprojection, and calibration-compatibility result. `third_view_last_failure.json` on thrown error. Both included in latest/all ZIP. Persistent USER_ACTION, ANALYSIS_PROGRESS, ANALYSIS_RESULT/ERROR events correlate run and operation ID.
- Calibration comparison: 1000×467 board working image and 720×1280 original video have unequal orientation-independent aspect ratios. `INCOMPATIBLE_ASPECT_RATIO`; no K application. Even when aspect matches, `ASPECT_ONLY_MATCH_LENS_CROP_UNVERIFIED`, not auto-applied.
- Test once on normal translating handheld sequence and again on stationary/low-texture sequence. Original PLY must still be exported/viewed; button should not alter saved points, and output should preserve captured photos. Export ZIP, inspect third-view report & ratio, verify version naming still auto correct and all three prior captures work. Record manual visual PASS/FAIL. No version verification without user physical confirmation.
- JVM ThirdViewPolicyTest checks threshold/verdict logic and aspect mismatch, Android CI `37919747862` pending at handoff.


## v0.8.0 source-image object region selection

**New controls:** After real 2-view pose cloud exists, **Select Object in Two Photos** opens actual selected source pair; user draws an independent green rectangle in each frame and taps **Create Object-Focused PLY**. Then **View Object-Focused Points — 3D** loads the candidate with existing rotate/zoom controls, and **Export Object-Focused PLY — Experimental** saves a versioned file distinct from full scene; the viewer's **Saved scans** menu distinguishes Full Scene and Object Focus.

**Source/verification:** `sparse_point_projections.json` records ordered normalized first/second source pixel positions for actual saved vertices, selected pair dimensions and original PLY fingerprint. ObjectFocusProcessor rejects wrong run/point count/hash and invalid/missing ROI; outputs object-focus selection and report and ERROR events without mutating full-scene PLY. Retention requires image positions in BOTH chosen boxes; `objectCandidatePoints` + `excludedScenePoints` must sum to original point count. Zero matches yields NO_POINTS rather than a false model. Reports in latest/all ZIP; original frames and PLY never included in redacted ZIP.

**Device test:** (1) capture or import video and run two-view analysis; for an old PLY re-run Analyze Two Views first to build correspondence map; (2) click Select Object in Two Photos and inspect both real source photos; (3) draw green rectangle around same target in both; (4) create object candidate, inspect status/kept count; (5) compare object and full clouds by rotating both; (6) export both PLYs using automatic v0.8 names and diagnostics; (7) try selecting irrelevant areas in both images to check NO_POINTS/less points; (8) check old ORB, third-view, checkerboard, Live/Smart/video, native viewer and export naming unaffected. No claim of accurate object segmentation or completed 3D. Build candidate not VERIFIED until user physical confirmation.

**Automated tests:** `ObjectFocusPolicyTest` ROI containment, both-view gating, tiny/missing boxes, boundaries. `ExportNamesTest` object-only name independent of full scene and derived from installed version. CI is GitHub Actions Android `:app:testDebugUnitTest :app:assembleDebug`. Preserve per-feature diagnostics and source artifacts.


## v0.8.1 guided test: finger-drawing + step-by-step source photo UI

User observed broken v0.8.0 object selector even though 158-vertex sparse stage worked. Latest ZIP includes OPEN_OBJECT_FOCUS_SELECTOR but not APPLY_OBJECT_FOCUS or object_focus_report. New v0.8.1 workflow: enlarge first photo (full available screen), default DRAW with ONE finger, see green draft and final rectangle, zoom with +/− and optionally switch to MOVE to pan. No need for two fingers. Tap NEXT PHOTO only after photo 1 has a valid box; photo 2 should now replace photo 1 with same object at another angle; draw again, tap CREATE OBJECT PLY. BACK returns to first box; Reset View resets zoom/pan, not the ROI. Each `DRAW_VALID`, `DRAW_INVALID`, `NEXT_PHOTO`, `BACK_PHOTO`, `CREATE_REQUEST`, zoom/mode is logged as a redacted USER_ACTION with photoNumber, no image pixels. After success, original whole-scene PLY remains unchanged and object-only PLY/count/report is created or explicitly NO_POINTS if no overlap. New code has no instrumented real Android touch integration tests; CI Kotlin/JVM tests insufficient to claim touch fixed until user tests device. If still failing, upload ZIP to correlate last action. Source v0.8.1 candidate, last user-confirmed v0.7 display+rotation; no Windows edits.

## v0.10.0 model isolation and multi-view photo regression

Real v0.9 device evidence: early object detection created **261**, later **249** ROI-first point candidates, but subsequent legacy AFTER-filter overwritten these with **56**, then **38** scene-subset points. User said cloud appearance still the same. New 0.10 must preserve all variants independently; no inferred object quality from higher point counts.

**Test plan (new preview package .preview100):**
1. Fresh install separate from user-verified 0.8.1 and 0.9 preview; check v0.10.0 launcher `Video 3D Capture Lab 0.10 Preview`. Import stationary-object video, verify source run/300 frames if using same video. Do not uninstall old versions; preview app-private data is separate.
2. Before reconstruction, draw ROI for SAME object on actual two source photographs. Tap Analyze Sparse 3D. New viewer choice `Full scene · 2-view` and distinct `Object reconstruction · ROI-first` should appear. Export separately; filenames include `sparse-two-view` vs `object-reconstruction`. Check early reconstruction report `early_object_reconstruction_report.json`, full scene `sparse_report.json`.
3. Tap **Build Multi-View Object Cloud — Experimental**. It should try up to 8 more source photos (not simply filter old PLY). Observe reported `pnpRegisteredExtraViews`, `viewsContributingNewPoints`, `newPointsFromAdditionalFrames`, `totalPoints`, per-frame `pnpMedianErrorPx`, and `newAccepted3dPoints`. If >=2 extra frames provide >=4 additional points beyond ROI source pair, `MULTIVIEW_SPARSE_CANDIDATE` with distinct `sparse_object_multiview.ply`. Otherwise `INCONCLUSIVE` is honest; never write fake PLY. Export dedicated `object-multi-view` file if available.
4. NOW use legacy **Select Object in Two Photos AFTER analysis** to filter scene points. `Filtered scene · subset` and `filtered-scene` export should show a strict subset of original scene, with report `object_focus_report.json`. **Must NOT change ROI-first or multi-view PLY SHA256/point count**. Compare 3 or 4 cloud appearances in viewer and tell whether target object is actually recognizable. Original scene and ROI-first local XYZ coordinate frames are *not* aligned; multi-view coordinates match ROI-first base frame, not scene.
5. Latest and ALL ZIPs must include `early_object_focus_selection.json`, `early_object_reconstruction_report.json`, `object_focus_report.json`, `object_multiview_report.json` (if attempted) and relevant error JSONs. They exclude source camera photos, videos and PLY XYZ rows to preserve privacy; export PLYs explicitly. Verify run IDs, timestamps and no overwrites.
6. Third-view PnP remains a separate scene consistency test (NOT multi-view proof). Also test calibration BEFORE importing any video on a clean preview, optionally rotate phone DURING processing, and export calibration-only diagnostic ZIP. Earlier pre-import crash and OpenCV multi-view operation still need real device verification.
7. Android CI `:app:testDebugUnitTest :app:assembleDebug` plus `CloudArtifactsTest` for distinct files/export names and `MultiViewPolicyTest` for robust PnP/geometry threshold policy. CI result not user verification; no dense mesh, bundle adjustment, true scale or semantic object mask included.
