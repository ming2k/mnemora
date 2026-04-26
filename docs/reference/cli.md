# CLI Reference

## `scripts/dev.sh`

Development helper script. All commands run from the project root.

| Command | Description |
|---|---|
| `build` | Compile debug APK (`./gradlew assembleDebug`) |
| `install` | Install debug APK to device (builds first if needed) |
| `start` | Launch the main activity |
| `log` | Tail app logs (auto-launches if not running) |
| `run` | Full flow: build → install → start → log |
| `watch` | Auto-rebuild and install when `app/src/` changes |
| `inspect` | Capture screenshot and dump UI hierarchy |
| `clean` | Clean build artifacts (`./gradlew clean`) |
| `uninstall` | Uninstall debug package (`adb uninstall`) |

## Gradle Tasks

| Task | Description |
|---|---|
| `./gradlew assembleDebug` | Build debug APK |
| `./gradlew installDebug` | Build and install debug APK |
| `./gradlew assembleRelease` | Build release APK |
| `./gradlew test` | Run unit tests |
| `./gradlew connectedAndroidTest` | Run instrumented tests on device |
| `./gradlew clean` | Delete build artifacts |
| `./gradlew app:dependencies` | Print dependency tree |

## ADB Snippets

| Operation | Command |
|---|---|
| View all logs | `adb logcat` |
| Tail app logs | `adb logcat --pid=$(adb shell pidof com.hihusky.mnema.debug)` |
| Clear logs | `adb logcat -c` |
| Force reinstall (keep data) | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| Launch app | `adb shell am start -n com.hihusky.mnema.debug/com.hihusky.mnema.MainActivity` |

## Package Names

| Build Type | Application ID |
|---|---|
| Debug | `com.hihusky.mnema.debug` |
| Release | `com.hihusky.mnema` |

The debug package uses the `.debug` suffix and can coexist with the release build on the same device.

## Related Files

- [`scripts/dev.sh`](../../scripts/dev.sh) — Development helper script source
- [`build.gradle.kts`](../../build.gradle.kts) — Project-level build config
- [`app/build.gradle.kts`](../../app/build.gradle.kts) — App-level build config
