# How to Troubleshoot Installation and Build Errors

This guide assumes you have already attempted [Getting Started](../tutorials/01-getting-started.md).

## `adb: command not found`

**Cause**: Android SDK `platform-tools` is missing or not on `PATH`.

**Fix**:

```bash
# Add to ~/.bashrc or ~/.zshrc
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Then reload your shell and verify with `adb version`.

## `compileSdkVersion not found`

**Cause**: The required Android platform is not installed.

**Fix**:

```bash
sdkmanager "platforms;android-35"
```

## `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

**Cause**: Signature or package name conflict with an existing installation.

**Fix**:

```bash
adb uninstall com.hihusky.mnemora.debug
```

Then retry installation.

## Slow dependency downloads

**Cause**: Network latency to Maven Central / Google repositories.

**Fix**: Configure a proxy or mirror in `gradle.properties` or `~/.gradle/gradle.properties`:

```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
```

## No connected devices found

**Cause**: Device not connected, USB debugging disabled, or unauthorized.

**Fix**:

1. Connect the device via USB.
2. Enable **Developer options** → **USB debugging**.
3. Accept the RSA fingerprint dialog on the device.
4. Run `adb devices` to confirm.

## Build succeeds but app crashes immediately

**Cause**: Possible ABI mismatch or missing native libraries.

**Fix**:

```bash
./scripts/dev.sh clean
./scripts/dev.sh run
```

If the crash persists, capture the full crash log:

```bash
adb logcat -d > crash.log
```

and open an issue with the log attached.
