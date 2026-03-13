# Release Artifacts

## Local build artifacts (this environment)
- `SmsSyncAndroid-debug.apk` ✅ Android debug APK
- `SmsSyncWindows-linux-x64` ⚠️ Linux binary built from the Windows client source (not a native Windows `.exe`)

## True Windows EXE
Use either:
1. GitHub Actions workflow `.github/workflows/release.yml` (recommended), or
2. Run `scripts/build_release_windows.bat` on a Windows machine.

Both produce `SmsSyncWindows.exe`.
