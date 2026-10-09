# Instructions for all agents working in this repository

## Current platform ownership — 2026-10-08
- Android is now active. **Android worker owns only `android/` and `.github/workflows/android-build.yml`.** Android checkpoint, baseline, roadmap, diagnostics, source and APKs are owned there.
- Windows/Codex owns the historical root implementation: `videogrammetry/`, `tests/`, `pyproject.toml`, `.github/workflows/tests.yml`, and the original root Windows checkpoint/memory/roadmap/testing records. Preserve their names/locations.
- Read `windows/OWNERSHIP.md` and `shared/SHARED_DECISIONS.md` before operating in a platform area. Shared product specifications live in `shared/`; check latest versions before editing shared files.
- Backup of pre-Android Windows state: branch `backup/pre-android-windows-v0.1.0` at `aaec898cf2fadf95760aaedc436d0f7062bd7fbe`. Its v0.1.0 Python candidate is user-UNVERIFIED.
- An Android CI compile, APK artifact or user test never verifies the Windows implementation, and vice versa.


**Mandatory governing instruction library:** `auxz2jz/master-instruction-library`, GitHub default branch `main`.

Read **`INSTRUCTION_INDEX.md` first**, then all applicable mandatory files: `CORE_DEVELOPMENT_RECOVERY_RULES.md`, `DIAGNOSTICS_STANDARD.md`, `GUIDED_TESTING_STANDARD.md`, `CODEX_USAGE_EFFICIENCY_STANDARD.md` (for coding agents), and `CROSS_PLATFORM_COLLABORATION_STANDARD.md` before introducing a second platform implementation.

Read this project's `PROJECT_MEMORY.md`, `CHECKPOINT.md`, `ROADMAP.md`, `DIAGNOSTICS_AND_TESTING.md`, and relevant `ARCHITECTURE.md` sections before changing code.

**Golden workflow:** READ → PLAN → RECORD PLAN → CHECKPOINT → MODIFY → BUILD → TEST → ANALYZE → RECORD RESULT → CHECKPOINT → CONTINUE.

- The user's latest explicit instructions override older project assumptions.
- **LAST VERIFIED BASELINE** is only a version physically tested and explicitly confirmed by the user. A compile, CI success, or automated test produces a **CANDIDATE**, never a user-verified baseline.
- Never overwrite or destroy last verified source/artifacts; preserve source hashes and checkpoints.
- Evidence-first debugging; after 2 essentially identical failures STOP that approach. After 3 meaningfully different failed attempts escalate according to the Master Library.
- Every meaningful operation requires diagnostics: request vs state/progress vs verified result vs error, correlation IDs, timing, redacted persistent logs, and export. Build actual-feature-based guided tests with manual fail and human confirmation when necessary.
- Minimize model usage with targeted file reads and small patches, not by skipping correctness or tests.
- Do not execute recovery/emergency example commands merely by reading them.
- **Do not modify `auxz2jz/Slot-8`.** It is a read-only technical reference. New source and artifacts belong here.
- If Android or another implementation is added later, separate its source, tests, checkpoints, and baseline from the desktop implementation. Share only genuinely platform-neutral product contracts.
- Never represent engine detection as successful reconstruction or a preview as a completed 3D model.
- Keep GitHub durable source of truth; update handoff and roadmap at checkpoints.
