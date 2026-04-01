# Android Kotlin Starter

Finnvek Android project template. Kotlin + Jetpack Compose + Material Design 3.

## Quick Start

1. Click **Use this template** on GitHub (or clone the repo)
2. Replace `com.finnvek.knittools` with your package name
3. Replace `MyApp` in `settings.gradle.kts` and `strings.xml`
4. Update `releaseSigningEnvPrefix` in `app/build.gradle.kts`

## What's Included

- **Gradle**: Version catalog, R8, signing config with env var placeholders
- **Compose**: BOM, Material 3, Navigation, Animation, Foundation
- **Architecture**: Hilt DI, Room, DataStore, Coroutines, Lifecycle
- **Quality**: detekt (with Compose rules), ktlint, Android Lint
- **CI/CD**: GitHub Actions (build, test, CodeQL)
- **Theme**: Minimal M3 setup (dynamic colors, default typography/shapes)

## What's NOT Included

- Navigation, screens, UI components, business logic
- Dark/light theme selection or custom color palettes
- `lint-check` / `security-check` scripts (global tools in `~/bin/`)

## Project Structure

```
app/src/main/java/com/finnvek/template/
├── ui/
│   ├── screens/        # App screens
│   ├── components/     # Shared UI components
│   ├── theme/          # M3 theme (Theme.kt, Type.kt, Shapes.kt)
│   ├── navigation/     # Navigation graph
│   ├── state/          # UI state classes
│   └── viewmodel/      # ViewModels
├── data/
│   ├── local/          # Room database, DAOs, entities
│   └── datastore/      # DataStore preferences
├── domain/
│   ├── usecase/        # Use cases
│   └── model/          # Domain models
├── repository/         # Repository pattern
├── di/                 # Hilt modules
└── util/
    ├── extensions/     # Kotlin extensions
    └── constants/      # App constants
```

## Release Signing

Set environment variables before release build:

```bash
export APP_KEYSTORE_PATH=/path/to/keystore.jks
export APP_KEYSTORE_PASSWORD=password
export APP_KEY_ALIAS=alias
export APP_KEY_PASSWORD=password
```

Change `APP` prefix in `app/build.gradle.kts` to match your app.

## License

MIT
