# Configuration Reference

## Build Variants

Configured in `app/build.gradle.kts`.

| Variant | `applicationIdSuffix` | `isMinifyEnabled` | App Name |
|---|---|---|---|
| `debug` | `.debug` | `false` | Mnemora (test) |
| `release` | — | `true` | Mnemora |

## SDK Versions

| Property | Value |
|---|---|
| `compileSdk` | 35 |
| `minSdk` | 21 |
| `targetSdk` | 35 |

## Java / Kotlin Targets

| Property | Value |
|---|---|
| `sourceCompatibility` | Java 11 |
| `targetCompatibility` | Java 11 |
| `jvmTarget` | `11` |
| `kotlinCompilerExtensionVersion` | `1.5.15` |

## Gradle and Plugins

| Plugin | Version |
|---|---|
| Android Gradle Plugin | `8.6.1` |
| Kotlin | `2.0.21` |
| Compose Compiler | `2.0.21` |
| Hilt | `2.52` |
| KSP | `2.0.21-1.0.28` |
| Room | `2.6.1` |

## Key Dependencies

| Library | Version |
|---|---|
| Compose BOM | `2024.10.00` |
| Navigation Compose | `2.8.3` |
| Hilt Navigation Compose | `1.2.0` |
| OkHttp | `4.12.0` |
| Coil Compose | `2.7.0` |
| kotlinx-serialization-json | `1.7.3` |

## Environment Variables

| Variable | Purpose | Example |
|---|---|---|
| `ANDROID_HOME` | Android SDK root | `$HOME/Android/Sdk` |

## See also

- [How to build from CLI](../how-to/build-from-cli.md)
- [Developer setup](../dev/setup.md)
