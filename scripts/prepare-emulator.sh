#!/usr/bin/env bash
#
# Puts a running emulator (or device) into a state where the Compose instrumented tests can pass.
#
# Without this, every test using createComposeRule() fails with:
#   java.lang.IllegalStateException: No compose hierarchies found in the app.
# because the lock screen prevents the test ComponentActivity from reaching the foreground.
#
# None of these settings survive a cold boot, so run this after starting an emulator and before
# ./gradlew connectedDebugAndroidTest.
#
# Usage:
#   scripts/prepare-emulator.sh              # uses the only connected device
#   scripts/prepare-emulator.sh emulator-5554
#   ANDROID_SERIAL=emulator-5554 scripts/prepare-emulator.sh
#
set -euo pipefail

BOOT_TIMEOUT_SECONDS="${BOOT_TIMEOUT_SECONDS:-300}"

if [[ -n "${1:-}" ]]; then
    export ANDROID_SERIAL="$1"
fi

if ! command -v adb >/dev/null 2>&1; then
    echo "adb not found; add \$ANDROID_HOME/platform-tools to PATH" >&2
    exit 1
fi

echo "==> Waiting for a device"
adb wait-for-device

echo "==> Waiting for boot to complete (timeout ${BOOT_TIMEOUT_SECONDS}s)"
deadline=$(( $(date +%s) + BOOT_TIMEOUT_SECONDS ))
until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
    if (( $(date +%s) >= deadline )); then
        echo "Boot did not complete within ${BOOT_TIMEOUT_SECONDS}s" >&2
        exit 1
    fi
    sleep 2
done

echo "==> Dismissing the lock screen"
adb shell wm dismiss-keyguard
adb shell input keyevent 224          # KEYCODE_WAKEUP
adb shell svc power stayon true

echo "==> Disabling animations (flaky Compose assertions otherwise)"
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

echo "==> Ready"
adb devices
