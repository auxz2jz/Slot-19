# Android v0.8.0: Object Focus — implementation and safety plan

## Why
User visually reviewed a rotated v0.7.0 cloud: a small partial cluster in the center, a possible featureless tabletop around it, and many more peripheral/background points. User authorized adding a couple of features. This is **not proof of semantic segmentation** or a physically accurate object model.

## Scope: TWO related features
1. **Select Object in Two Photos:** after Analyze Sparse 3D produces a source-pair PLY, open its two actual saved JPEG frames, draw rectangles on both. No assumed centered object. All source JPEGs and original PLY remain unchanged. Old v0.7 PLYs do not have saved 3D-to-photo feature mapping; re-run the two-view analysis once on that run to create it.
2. **Preview and export Object-Focused PLY:** each accepted original sparse vertex has two 2D feature positions; retain only points within the user-drawn ROI on BOTH saved source images. Save `sparse_object_focus.ply` separately from `sparse_two_view.ply`, with independent saved viewer selection and automatically versioned export `Android-v0.8.0-object-focus-<runId>-<UTCtimestamp>.ply`. Zero matching vertices yields a specific `NO_POINTS_IN_BOTH_REGIONS` verdict, not a fabricated model.

## Source correspondence and diagnostics
- `SparseTwoViewAnalyzer.kt` records a `sparse_point_projections.json` ordered 1:1 with vertices from the original PLY; each record is normalized [0..1] to the **decoded JPEG** dimensions and has `pointIndex`, `firstX/Y`, `secondX/Y`. It retains the original pair indices, run ID, algorithm version, dimensions, and SHA256 of original PLY to block stale/mismatched maps.
- `ObjectFocusProcessor.kt` checks saved capture validity and original PLY fingerprint before filtering; saves user box coordinates, original/retained/excluded counts, candidate/inconclusive status and error diagnostics in `object_focus_report.json`, `object_focus_selection.json` and `object_focus_last_failure.json`. Both diagnostic ZIPs include those JSON records but NOT original images or raw PLY bytes. Persistent action/result/error events retained.
- Original scene ORB feature matches remain available to camera pose; target ROI is an experimental filtering step AFTER pose/triangulation. User can compare both scene and filtered clouds in native viewer. Filtered set is not guaranteed to belong exclusively to the physical object; rectangle can contain tabletop/background. 3D scale remains arbitrary.
- Versioned filename rule from v0.6.1 preserved for BOTH PLY choices and diagnostics; GitHub APK filenames auto-versioned from Gradle v0.8.0. Do not change Windows or Slot-8.

## Test plan
- JVM `ObjectFocusPolicyTest`: dual-view requirement, same-object boundaries, rejecting invalid/tiny/missing rectangle, no automatic guessed object region. `ExportNamesTest` tests v0.8 object-only name independent of original PLY naming.
- CI `:app:testDebugUnitTest :app:assembleDebug` and SHA manifest; ARM64 & universal artifacts.
- **On Samsung phone:** generate sparse PLY from a new frame/video capture. Open **Select Object in Two Photos** and draw one green rectangle around object in FIRST saved source photo, another in SECOND source photo; submit. Check kept/excluded point counts and report; open native viewer `Object focus` vs `Full scene`; export both distinct PLYs and latest/all diagnostic ZIP; check original source PLY SHA does NOT change after focus. Try rectangles around empty background to expect NO_POINTS or fewer points. Re-run sparse analysis and ensure old filtered output invalidates. Try old v0.7 scan (no map) to receive rerun guidance rather than silently false results. Confirm Live/Video/Smart, ORB, third-view, checkerboard and filename version unchanged.
- v0.8.0 is **candidate only** until user physical confirmation. v0.7.0 viewer display/rotation user-verified, v0.6.1 last comprehensive user-confirmed app functionality. Preserve source backups.
