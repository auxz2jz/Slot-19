# Android diagnostics and guided tests — initial design

Actual planned v0.1.0 UI controls: **Start Live Sampling**, **Stop Live Sampling**, **Choose Video**, **Test This Version**, **Frames Look Correct**, **Expected Behavior Failed**, **Export Test + Diagnostics**. If implementation changes the labels, update this document with real labels.

Mandatory events for important operations: `USER_ACTION`, `OPERATION_START`, `STATE_TRANSITION`, `OUTPUT_VALIDATION`, `OPERATION_RESULT`, `ERROR`, `TEST_VERIFICATION`, `TEST_RESULT`, `EXPORT_RESULT`. Per-run UUID/correlation; monotonic elapsed and UTC times; version, counts, safe source metadata, exceptions and errors. Persistent append-friendly bounded logs. No raw images/audio/credentials/paths in diagnostics.

Feature coverage map:

| Feature | Input/trigger | Actual result evidence | Failure condition | Guided check |
|---|---|---|---|---|
| Preview | permission + lifecycle | CameraX bind succeeds and preview surface receives frames, separate from analysis | permission denied/no device/bind exception | open preview, visually confirm |
| Live extraction | Start → frames from ImageAnalysis | count of files saved, each JPEG decodes, manifest matches | 0 frames, invalid files, IO error | start, move phone, stop, inspect |
| Video import | file picker → MediaMetadataRetriever | recognized duration, decoded distinct requested positions, JPEG files verified | no duration, decode fails, 0 frames | choose short video, inspect |
| Stop | user stop | run finalizes, valid result/manifest exists | unfinished run, missing report | confirm count/result |
| Guided test | test guide + manual verdict | actual completed run count >0 and file validation plus explicit user visual confirmation | user reported failure/invalid run | confirm vs failure both available |
| Diagnostic export | create ZIP via SAF | bytes actually written; ZIP content names verified where practical | URI cancelled, permission/storage error | export and share report |

Performance budgets: bounded sampling and storage, no unlimited queued frames; throttle live frames. Maintain raw frames independently from any preview overlays; no 3D reconstruction is claimed. Reassess image quality, orientation and accurate live preview on device using diagnostics. Test results are authoritative over superficial button actions.
