# Recovery Checkpoint

**Date:** 2026-10-08
**State:** CANDIDATE v0.1.0 (source/CI-tested, never user verified)
**Source repository:** `auxz2jz/Slot-19`; branch `main`.
**Rules repository:** `auxz2jz/master-instruction-library`.
**Last user-verified baseline:** NONE.
**Current build:** Python CLI source v0.1.0 (no native installer/APK).
**Latest candidate:** v0.1.0, candidate source commit `9025e96ada71a974204f0189ce0bfbe1328ede25`.
**First candidate:** v0.1.0 source implemented; CI compiled/tests passed; no device/user verification.
**Artifacts and hashes:** Git commit ID above identifies tested source; later commits update documentation, not this code. No EXE/APK/installer or release artifact. GitHub Actions run `37889805717` is the automated-test evidence.
**Known failures/tests:** 6/6 CI unit/integration tests PASS, 0 skipped; includes real generated video extraction, PNG/hash checks, bad input and guided diagnostics export. No Windows webcam/phone RTSP physical test. No reconstruction engine implementation yet.
**Files/results received for Slot-19:** None.
**Protection:** Slot-8 must remain unchanged.

**Completed this checkpoint:** Repository was verified empty; Master Library read; startup docs established; v0.1.0 frame-extraction CLI, source validation, persistent event trace, export, guided CLI test and CI created and passing.

**Exact next action:** USER DEVICE TEST v0.1.0 on Windows with a short handheld moving-camera recording using README commands; review saved frame files and `test-this-version` report; optionally test reachable RTSP or DirectShow camera and export diagnostics. Record outcomes before proceeding to quality-aware frame selection and initial COLMAP adapter.

Recovery commands in the Master Library are conditional examples, NOT active instructions here.
