# Shared decisions and ownership

2026-10-08: **Windows and Android independently developed in one Slot-19 repository.** Historical root `videogrammetry/`, `tests/`, `pyproject.toml` are **Windows-owned**. Never move or rewrite them during Android work. Android owns `android/`, Android-specific GitHub workflow and artifacts. Windows worker may continue historical root; `windows/OWNERSHIP.md` documents the exception. All shared product decisions go under `shared/`.

2026-10-08: Protected snapshot branch `backup/pre-android-windows-v0.1.0` points to full pre-Android repository commit `aaec898cf2fadf95760aaedc436d0f7062bd7fbe`. Windows's actual source candidate commit is `9025e96ada71a974204f0189ce0bfbe1328ede25`, CI passes 6 tests, user verification NONE. The backup is not a new user-verified version.

2026-10-08: First Android milestone will capture live CameraX preview frame samples and import/extract frames from recorded video; **no 3D reconstruction** yet. Build APK in GitHub Actions. Android/Windows app versions and verification records are independent.

2026-10-08: Android v0.1.0 **source and APK candidate created** independently under `android/`. Exact tested source commit `fd7973c6a978136d1154872162f772127c5ecfd9` preserved on `backup/android-v0.1.0-ci-candidate`. GitHub Actions `37891374599` passed Android unit test + debug APK build; artifact id `11597604313` and APK SHA-256 `132a5780d575bee9d243c9adccb35a953b42ad998760ef3711eaac620988b639`. **No Android user-verified baseline exists yet.** Windows historical source and checkpoint SHA values were confirmed unchanged; the Windows pre-Android backup branch remains intact.
