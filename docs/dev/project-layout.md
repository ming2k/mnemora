# Project Layout

A tour of the source tree for new contributors.

```
app/src/main/java/com/hihusky/mnema/
├── MainActivity.kt              # Single Activity entry point
├── MnemaApplication.kt          # Application class + Hilt entry
├── di/
│   ├── AppModule.kt             # App-level bindings
│   └── DatabaseModule.kt        # Room database provider
├── data/
│   ├── local/
│   │   └── db/
│   │       ├── AppDatabase.kt
│   │       ├── dao/             # Room DAOs
│   │       └── entity/          # Room entities
│   ├── model/                   # Data classes / domain models
│   └── repository/              # DataRepository, SettingsRepository
├── domain/
│   └── service/                 # Business logic services
├── ui/
│   ├── components/              # Reusable composables
│   ├── navigation/              # NavHost and route definitions
│   ├── screens/                 # One package per screen
│   │   ├── home/
│   │   ├── bookdetail/
│   │   ├── practice/
│   │   ├── review/
│   │   ├── test/
│   │   ├── settings/
│   │   └── collectiondetail/
│   └── theme/                   # Colors, typography, theme
```

## Key Files at Root

| File | Purpose |
|---|---|
| `build.gradle.kts` | Project-level plugins and versions |
| `app/build.gradle.kts` | App dependencies, SDK versions, build types |
| `scripts/dev.sh` | CLI development helper |
| `local.properties` | SDK path (not in git) |
| `gradle.properties` | Gradle settings, proxy config |

## Where to add new code

- New screen: create a package under `ui/screens/<screen>/` with `Screen.kt` and `ViewModel.kt`.
- New database entity: add to `data/local/db/entity/` and update `AppDatabase.kt`.
- New business logic: add a method to an existing domain service or create a new one in `domain/service/`.
