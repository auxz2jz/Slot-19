# Android v0.8.1 — Object Focus photo selector usability repair

## Verified user repro
User on Samsung Galaxy S22 Ultra installed Android v0.8.0, imported a video and produced sparse points, opened Object Focus, but source photos were far too small; ONE finger dragging did not draw any rectangle; trying two fingers created a misplaced box; pressing Create did not advance/produce any result. User expected a NEXT PHOTO step, asking whether the same object should be selected in both source photos. This is a **failed feature test**, not a failure of frame import, ORB or triangulation.

Uploaded ZIPs:
- `Android-v0.8.0-last-run-diagnostics-20261009T214912Z_0e501cd5-0-20261009T215253482Z.zip`
- `Android-v0.8.0-ALL-run-comparison-20261009T215248265Z.zip`
Both ZIP integrity PASS. Same recorded video run `20261009T214912Z_0e501cd5-0`, 300 720×1280 frames, 5 requested/measured FPS, ORB geometry completed (75/75 pairs supported), sparse result `SPARSE_CANDIDATE` with 158 XYZ points selected from frames 0 and 6. `sparse_point_projections.json` includes 158 corresponding source-point rows and original PLY fingerprint. Diagnostic `events.jsonl` records `OPEN_OBJECT_FOCUS_SELECTOR` but no `APPLY_OBJECT_FOCUS`, no object-focus selection report, no focus PLY. Strongly consistent with UI gesture/navigation blocker, not evidence of a reconstruction engine failure.

## v0.8.1 patch
- Rework `ObjectFocusDialog.kt` as a true **single photo per screen step** full-height modal: **Photo 1 of 2 → Next Photo → Photo 2 of 2 → Create Object PLY**. Clear Back/Cancel, Reset View. Selected boxes retained while navigating.
- Make the one photo as large as screen height permits using `Modifier.weight(1f)`, removing the old stacked 220dp photos and `verticalScroll` ancestor that could intercept one-finger drags. `ObjectRegionView` requests that its parent not intercept touch during one-finger interaction, ignores multi-touch as a box source and draws an immediately visible green draft rectangle.
- Explicit DRAW (one finger) and MOVE (one finger pan) modes, with +/− zoom buttons from 1x to 5x, image-aligned normalized ROI coordinates unaffected by zoom/pan. Reset view and drag again to replace rectangle. No multi-finger action required.
- Small invalid boxes fail only after finger release with useful UI text. Avoid staleness of previous selection after starting a redraw. Release decoded image bitmap when entire wizard is dismissed, not when flipping steps.
- Redacted low-volume `USER_ACTION/OBJECT_FOCUS_UI_*` events track valid/invalid drawing, navigation, mode and zoom, and Create request without embedding photo pixels. Existing `ObjectFocusProcessor` filtering, SHA alignment, diagnostic exports, original full scene PLY, third-view check, capture, camera calibration unchanged.
- Patch versionCode 10 / versionName **0.8.1**; APK/export filenames auto-derive build version. Never relabel earlier user exports.

## Regression gates
- GitHub Android JVM unit tests (existing pure ROI geometry tests), debug APK build, checksum and artifact upload must PASS. **No Android instrumentation/device gesture test in CI**; user phone verification required.
- Real-device test: install preserving v0.8.0 saved run, open **Select Object in Two Photos** without new recording; screen must show large Photo 1. In DRAW mode drag ONE finger (no scrolling/misplaced box), green rectangle visible while dragging. Zoom +; switch MOVE and pan; switch DRAW then adjust region. Tap NEXT PHOTO to see Photo 2; select SAME physical object and press CREATE OBJECT PLY. Status should show retained/excluded counts, focused PLY should appear separately in viewer and export. Back must preserve Photo 1 selection and not clear source PLY.
- Export latest run ZIP, verify `OBJECT_FOCUS_UI_NEXT_PHOTO`, `OBJECT_FOCUS_UI_DRAW_VALID`, `OBJECT_FOCUS_UI_CREATE_REQUEST`, `APPLY_OBJECT_FOCUS` and `object_focus_report.json`. If no points qualify, expect NO_POINTS not fabricated mesh. Confirm old full scene PLY unchanged.
- v0.8.1 remains user-UNVERIFIED candidate until this test. The v0.7.0 point viewer display and rotation remain user-confirmed; last broadly user-confirmed app baseline v0.6.1 kept intact. v0.8.0 failure baseline backed up at `backup/android-v0.8.0-user-tested-selection-ui-failed`. Android-only changes; Windows and Slot-8 untouched.
