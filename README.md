# KnitTools

KnitTools is an Android knitting toolkit built with Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, Ravelry integration, Play Billing, and Glance widgets.

For the most detailed current architecture map, use [`PROJECT.md`](PROJECT.md). Older `knittools-*.md` files are planning/delta notes unless `PROJECT.md` points to them as current source of truth.

## Project Structure

- Gradle modules: `:app`, `:baselineprofile`
- App namespace/applicationId: `com.finnvek.knittools`
- Main source root: `app/src/main/java/com/finnvek/knittools`
- Baseline profile namespace: `com.finnvek.knittools.baselineprofile`

Main source packages:

- `auth` - Ravelry authentication
- `billing` / `pro` - Play Billing, trial, Pro feature access, in-app review/update helpers
- `data` - Room, DataStore, remote API models/services, local file storage
- `di` - Hilt modules
- `domain` - calculator logic and domain models
- `repository` - storage/framework boundary for UI consumers
- `ui` - Compose screens, components, navigation, theme, ViewModels
- `widget` - Glance home screen widget

## Build And Test

```bash
./gradlew assembleDebug
./gradlew test
./gradlew :app:detekt
./gradlew lint
./gradlew :app:generateBaselineProfile
```

Do not commit generated `reports/` output.

## Release Signing

Release signing is environment-variable driven. Set these before release builds:

```bash
export KNITTOOLS_KEYSTORE_PATH=/path/to/keystore.jks
export KNITTOOLS_KEYSTORE_PASSWORD=password
export KNITTOOLS_KEY_ALIAS=alias
export KNITTOOLS_KEY_PASSWORD=password
```

Ravelry's old backendless accepted-risk path is superseded by `Ravelry Firebase Backend And Saved Patterns Plan.md`. Android no longer embeds Ravelry credentials or stores Ravelry tokens; Ravelry secrets belong in the Firebase backend.

Android Firebase builds require the project-specific config file locally at ignored path `app/google-services.json`, or CI must generate it from `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64`.

```bash
export KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64=base64-encoded-google-services-json
```

## Current Documentation

- [`PROJECT.md`](PROJECT.md) - current code-backed project map
- [`AGENTS.md`](AGENTS.md) / [`CODEX.md`](CODEX.md) - agent working rules
- [`CLAUDE.md`](CLAUDE.md) - product, UX, and visual direction notes
- [`ONLINE_OFFLINE.md`](ONLINE_OFFLINE.md) - feature network requirements

## License

MIT
