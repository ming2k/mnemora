# Configuration Reference

Toolchain, build variants, and environment knobs.

## Build Variants

Configured in `app/build.gradle.kts`.

| Variant | `applicationIdSuffix` | `isMinifyEnabled` | App Name |
|---|---|---|---|
| `debug` | `.debug` | `false` | Mnemora (test) |
| `release` | — | `true` | Mnemora |

## SDK and JVM Versions

| Property | Value |
|---|---|
| `compileSdk` | 37 |
| `minSdk` | 24 |
| `targetSdk` | 36 |
| `sourceCompatibility` | Java 17 |
| `targetCompatibility` | Java 17 |
| `jvmTarget` | `17` |

## Gradle and Plugins

Versions are managed centrally in `gradle/libs.versions.toml`; the table
mirrors it. Prefer editing the version catalog over `build.gradle.kts`.

| Plugin | Version |
|---|---|
| Android Gradle Plugin | `9.3.2` |
| Kotlin | `2.3.21` |
| Hilt | `2.60.1` |
| KSP | `2.3.11` |
| Room | `2.8.4` |
| ktlint (plugin / engine) | `14.2.0` / `1.8.0` |
| detekt | `1.23.8` |

## Key Dependencies

| Library | Version |
|---|---|
| Compose BOM | `2026.08.00` |
| Navigation Compose | `2.10.0` |
| Hilt Navigation Compose | `1.4.0` |
| OkHttp | `5.5.0` |
| Coil Compose | `3.6.1` |
| kotlinx-serialization-json | `1.11.0` |
| kotlinx-coroutines | `1.11.0` |
| DataStore Preferences | `1.2.1` |

## Versioning

`versionCode` and `versionName` live in `app/build.gradle.kts`
(`defaultConfig`). Each release bumps both and matches the pattern
`versionName = "0.0.<versionCode>"`.

## Environment

| Variable | Purpose | Example |
|---|---|---|
| `ANDROID_HOME` | Android SDK root | `$HOME/Android/Sdk` |

`local.properties` (never committed) carries the debug/release signing
properties consumed by the `release` signing config:

| Property | Purpose |
|---|---|
| `signing.storeFile` | Keystore path relative to the repo root |
| `signing.storePassword` | Keystore password |
| `signing.keyAlias` | Key alias |
| `signing.keyPassword` | Key password |

## Static Analysis Gates

| Tool | Config | Baseline |
|---|---|---|
| ktlint | `ktlint {}` block in `app/build.gradle.kts` | none |
| detekt | `detekt.yml` (repo root) | `detekt-baseline.xml` |
| Android lint | `lint {}` block in `app/build.gradle.kts` | `lint-baseline.xml` |

CI runs `ktlintCheck detekt lintDebug` and `testDebugUnitTest assembleDebug`
on every push and pull request; see `.github/workflows/ci.yml`.

## See also

- [How to build from CLI](../how-to/build-from-cli.md)
- [Developer setup](../dev/setup.md)
