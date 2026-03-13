#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android-client"

: "${ANDROID_SDK_ROOT:?Please set ANDROID_SDK_ROOT}"
export ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"

cd "$ANDROID_DIR"
./gradlew --no-daemon clean assembleDebug

APK_PATH="$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found: $APK_PATH" >&2
  exit 1
fi

echo "APK built: $APK_PATH"
