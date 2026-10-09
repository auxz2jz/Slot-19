# Windows ownership and handoff

The **existing historical root** of Slot-19 is the Windows implementation; do not move its files merely to create a tidy platform layout.

Windows-owned paths: `videogrammetry/`, `tests/`, `pyproject.toml`, and `.github/workflows/tests.yml`. Existing root `PROJECT_MEMORY.md`, `CHECKPOINT.md`, `ROADMAP.md`, `ARCHITECTURE.md`, `DIAGNOSTICS_AND_TESTING.md` remain the Windows-owned/historical project records until the Windows agent deliberately adds its own equivalents. Shared `AGENTS.md` and `README.md` need conflict-safe updates.

Protected backup branch: `backup/pre-android-windows-v0.1.0` at `aaec898cf2fadf95760aaedc436d0f7062bd7fbe`. Windows source candidate v0.1.0, 6/6 CI tests PASS, **no user verified baseline**. Continue Windows work from current main, inspect git history, do not reset either platform's current work. Android worker does not modify Windows source, tests, configuration, or Windows project memory.

Read `shared/` for feature ideas; evaluate/implement independently. Never treat Android test or build as verifying Windows.
