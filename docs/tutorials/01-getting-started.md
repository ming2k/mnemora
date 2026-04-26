# Getting Started: Your First Mnemora Build

By the end of this tutorial you will have:

- A running local build of Mnemora on your Android device
- Created and installed the debug APK
- Seen the app launch and verified log output
- Run a smoke test confirming your environment works

**Estimated time:** 15 minutes  
**Difficulty:** Beginner

## Prerequisites

- **JDK 17 or 21** (Gradle 8.x compatible)
- **Android SDK** with the following components:
  - `platform-tools` (`adb`)
  - `build-tools;35.0.0`
  - `platforms;android-35`
  - `cmdline-tools;latest`
- **A physical Android device** with USB debugging enabled, or an emulator running
- **4 GB free RAM** for Gradle builds
- Basic familiarity with the command line

## Step 1: Verify your environment

Check Java:

```bash
java -version
javac -version
```

You should see `openjdk version "17"` (or `21`).

Check Android SDK tools:

```bash
adb devices
```

Expected output:

```
List of devices attached
c09ffe9e    device
```

> **If you see no devices:** Enable USB debugging on your phone and authorize the computer. If using an emulator, start it first.

## Step 2: Configure the project

```bash
cd mnemora
```

Ensure `local.properties` exists with your SDK path:

```properties
sdk.dir=/home/yourname/Android/Sdk
```

> Create this file manually if it is missing.

## Step 3: Build the project

```bash
./gradlew build
```

The first build downloads the Gradle wrapper and all Maven dependencies. This may take 5–15 minutes depending on network speed.

You should see:

```
BUILD SUCCESSFUL in Xm Ys
```

> **If you see `compileSdkVersion not found`:** Run `sdkmanager "platforms;android-35"` to install the missing platform.

## Step 4: Install and launch

```bash
./scripts/dev.sh run
```

This performs:

1. `./gradlew assembleDebug`
2. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. `adb shell am start -n com.hihusky.mnemora.debug/com.hihusky.mnemora.MainActivity`
4. `adb logcat --pid=$(adb shell pidof com.hihusky.mnemora.debug)`

You should see the app open on your device and logs streaming in the terminal.

## Step 5: Run the smoke tests

```bash
./gradlew test
```

All unit tests should pass. This verifies your environment is correctly configured.

## What's next?

- Want to understand the daily development workflow? See [Build from CLI](../how-to/build-from-cli.md)
- Want to understand the architecture? See [Architecture overview](../explanation/architecture-overview.md)
- Want to contribute code? See [Developer setup](../dev/setup.md)

## Troubleshooting

- **`adb: command not found`**: Ensure `platform-tools` is installed and `$ANDROID_HOME/platform-tools` is in `PATH`.
- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`**: Uninstall the existing version: `adb uninstall com.hihusky.mnemora.debug`
- **Slow dependency downloads**: Configure a proxy in `gradle.properties` or `~/.gradle/gradle.properties`.
- See the full [troubleshooting guide](../how-to/troubleshoot-installation.md) for more.
