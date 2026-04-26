#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
APK_DIR="${PROJECT_ROOT}/app/build/outputs/apk/debug"
APK_PATH="${APK_DIR}/app-debug.apk"
PACKAGE_DEBUG="com.hihusky.mnema.debug"
PACKAGE_RELEASE="com.hihusky.mnema"
ACTIVITY="${PACKAGE_DEBUG}/com.hihusky.mnema.MainActivity"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

die() {
    echo "[ERROR] $*" >&2
    exit 1
}

info() {
    echo "[INFO] $*"
}

check_adb() {
    command -v adb >/dev/null 2>&1 || die "adb not found. Ensure Android SDK platform-tools is installed and on PATH."
    local devices
    devices=$(adb devices | grep -v "List of devices" | grep -v "^$" || true)
    [[ -n "$devices" ]] || die "No Android devices connected. Enable USB debugging and connect a device."
}

# ---------------------------------------------------------------------------
# Commands
# ---------------------------------------------------------------------------

cmd_build() {
    info "Building debug APK..."
    cd "${PROJECT_ROOT}"
    ./gradlew assembleDebug
    info "Build complete: ${APK_PATH}"
}

cmd_install() {
    check_adb
    if [[ ! -f "${APK_PATH}" ]]; then
        info "APK not found, building first..."
        cmd_build
    fi
    info "Installing debug APK..."
    adb install -r "${APK_PATH}"
    info "Install complete."
}

cmd_start() {
    check_adb
    info "Launching ${ACTIVITY}..."
    adb shell am start -n "${ACTIVITY}" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
    info "App launched."
}

cmd_log() {
    check_adb
    local pid
    pid=$(adb shell pidof "${PACKAGE_DEBUG}" 2>/dev/null || true)
    if [[ -z "$pid" ]]; then
        info "App not running, starting first..."
        cmd_start
        sleep 2
        pid=$(adb shell pidof "${PACKAGE_DEBUG}" 2>/dev/null || true)
    fi
    info "Tailing logs for ${PACKAGE_DEBUG} (pid=${pid})..."
    adb logcat --pid="${pid}"
}

cmd_run() {
    cmd_build
    cmd_install
    cmd_start
    sleep 1
    cmd_log
}

cmd_clean() {
    info "Cleaning build artifacts..."
    cd "${PROJECT_ROOT}"
    ./gradlew clean
    info "Clean complete."
}

cmd_uninstall() {
    check_adb
    info "Uninstalling debug package..."
    adb uninstall "${PACKAGE_DEBUG}" || true
    info "Uninstall complete."
}

cmd_watch() {
    check_adb
    local watcher=""
    if command -v inotifywait >/dev/null 2>&1; then
        watcher="inotifywait"
    elif command -v fswatch >/dev/null 2>&1; then
        watcher="fswatch"
    else
        die "No file watcher found. Install inotify-tools (Linux) or fswatch (macOS)."
    fi

    info "Watch mode started. Monitoring app/src/ for changes..."
    info "Press Ctrl+C to stop."

    if [[ "$watcher" == "inotifywait" ]]; then
        while true; do
            inotifywait -r -e modify,move,create,delete "${PROJECT_ROOT}/app/src/" 2>/dev/null || true
            info "Change detected. Rebuilding..."
            cmd_build && cmd_install
            info "Done. Waiting for next change..."
        done
    else
        fswatch -r -o "${PROJECT_ROOT}/app/src/" | while read -r; do
            info "Change detected. Rebuilding..."
            cmd_build && cmd_install
            info "Done. Waiting for next change..."
        done
    fi
}

cmd_inspect() {
    check_adb
    local ts
    ts=$(date +%Y%m%d-%H%M%S)
    local outdir="/tmp/mnema-inspect-${ts}"
    mkdir -p "${outdir}"

    info "Capturing screenshot..."
    adb shell screencap -p /data/local/tmp/mnema_screenshot.png
    adb pull /data/local/tmp/mnema_screenshot.png "${outdir}/screenshot.png" >/dev/null
    adb shell rm /data/local/tmp/mnema_screenshot.png

    info "Dumping UI hierarchy..."
    adb shell uiautomator dump /data/local/tmp/mnema_window_dump.xml >/dev/null 2>&1 || true
    adb pull /data/local/tmp/mnema_window_dump.xml "${outdir}/window_dump.xml" >/dev/null 2>&1 || true
    adb shell rm /data/local/tmp/mnema_window_dump.xml 2>/dev/null || true

    info "Inspect artifacts saved to: ${outdir}"
    ls -la "${outdir}"
}

# ---------------------------------------------------------------------------
# Usage
# ---------------------------------------------------------------------------

usage() {
    cat <<EOF
Usage: $(basename "$0") <command>

Development helper script for Mnemora.

Commands:
  run        Full flow: build → install → start → log
  build      Compile debug APK
  install    Install debug APK to device (builds first if needed)
  start      Launch the main activity
  log        Tail app logs
  watch      Auto-rebuild and install when app/src/ changes
  inspect    Capture screenshot and dump UI hierarchy
  clean      Clean build artifacts
  uninstall  Uninstall debug package

Examples:
  $(basename "$0") run
  $(basename "$0") watch
  $(basename "$0") inspect
EOF
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

main() {
    if [[ $# -eq 0 ]]; then
        usage
        exit 1
    fi

    local cmd="$1"
    shift || true

    case "$cmd" in
        build)       cmd_build "$@" ;;
        install)     cmd_install "$@" ;;
        start)       cmd_start "$@" ;;
        log)         cmd_log "$@" ;;
        run)         cmd_run "$@" ;;
        clean)       cmd_clean "$@" ;;
        uninstall)   cmd_uninstall "$@" ;;
        watch)       cmd_watch "$@" ;;
        inspect)     cmd_inspect "$@" ;;
        -h|--help|help) usage ;;
        *)
            die "Unknown command: $cmd"
            ;;
    esac
}

main "$@"
