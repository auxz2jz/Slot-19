# Diagnostics and Guided Testing — Initial Contract

Follow mandatory `DIAGNOSTICS_STANDARD.md` and `GUIDED_TESTING_STANDARD.md` from `auxz2jz/master-instruction-library`.

## Actual feature inventory
At **INITIALIZATION**, there is NO implemented UI, button, worker, or engine. The following are **planned v0.1.0 operations**, not existing controls:
- choose a recording or live source; choose frame interval/max count/duration
- preflight FFmpeg/camera/source/storage
- extract frames into a unique output folder
- finish/cancel/error handling
- validate resulting frame files; emit manifest/summary
- export diagnostics, run guided `Test This Version` and record tester failure/success.

Any new actual UI or command must update the coverage map below using its **real** final label, not copy from another app.

## v0.1.0 Diagnostic Coverage Map (PLANNED)

| Operation | Trigger | What must be verified | Typical failure | Required trace/test |
|---|---|---|---|---|
| Source selection | User chooses recording/live input | Path/source type valid without exposing passwords | missing/unreadable file, bad URL, unsupported camera | USER_ACTION / PRECHECK / FAIL |
| Dependency probe | extract requested | actual FFmpeg executable and supported mode found | binary missing, unsupported device | OPERATION_START / PRECHECK / RESULT |
| Start extraction | user confirms | subprocess starts with correct safe arguments | failed spawn, rejected input | USER_ACTION / OPERATION_START / ERROR |
| Frames written | decode/selected frames | images exist and decode/validate; actual count >0; per-frame provenance | no frames, corrupt/partial output | PROGRESS / OUTPUT_VALIDATION / RESULT |
| Long-running capture | live worker | bounded duration/count, last progress and stall detection | blocked stream, unexpected disconnect | WATCHDOG / ERROR / CANCEL / RESULT |
| Diagnostics export | user requests | bundle actually written, nonempty, redacted | disk permissions/full | EXPORT_START / EXPORT_RESULT / EXPORT_ERROR |
| Guided test | tester starts v0.1.0 guide | each objective behavior checked; visual result manually confirmable | absent frames, false positive | TEST_STEP / TEST_VERIFICATION / TEST_RESULT |

## Event specification (PLANNED)
Persist rolling JSONL with session ID, UTC and monotonic timing, sequence, event category and severity, version, correlationId/runId/testId, redacted source metadata, attempted command with sanitized arguments, state transition, actual result/metrics, error/trace, durations, and append/rotation limits. Separate human summary, machine-readable manifest, and structured guided-test results. Detect first real failure. Crash handlers where meaningful for platform. Do not log credentials, full private paths, frame contents, camera URL secrets.

## Guided test plan for first candidate (NOT YET EXECUTABLE)
1. **Dependency preflight:** follow exact invocation shown by built version. Expected installed FFmpeg recognized; missing dependency gives actionable failure, not false PASS.
2. **Recorded video:** use user's short sample recording or test fixture. Extract frames; verify each exists/decodes and manifest count equals actual output. Expected nonempty frame set.
3. **Live source:** select available supported webcam or reachable stream, capture for bounded duration. Verify nonempty output and no hang. Mark BLOCKED if source unavailable, not PASS.
4. **Invalid input:** missing recording URL/file fails gracefully, persists diagnostic failure, produces no 'success'.
5. **Export diagnostics:** ensure package exists and contains actual results and redaction; test success determined from physical output.
6. **Human review:** show/inspect representative frames for blur/coverage and allow explicit **Expected Behavior Failed** note.
7. **Report:** persist test session, PASS/FAIL/BLOCKED/UNTESTED, source of determination (AUTO_VERIFIED, AUTO_FAIL, MANUAL_PASS/FAIL), first failure, correlated diagnostics, machine JSON + readable TXT.

Test implementation should include a permanent user-invocable guide; do not claim this document alone is a running guided-test system. No test is PASS from click/request/progress alone.

## Error correction
Inspect summary → locate failed test/operation → ordered event sequence → first abnormal state → original error → smallest evidence-based fix → retest and update checkpoint. After 2 equivalent failures stop the approach; after 3 distinct failures escalate per global rules.
