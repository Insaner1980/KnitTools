# KnitTools

KnitTools is an Android knitting toolkit built with Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, Firebase AI, Ravelry integration, Play Billing, and Glance widgets.

For the most detailed current architecture map, use [`PROJECT.md`](PROJECT.md). Older `knittools-*.md` files are planning/delta notes unless `PROJECT.md` points to them as current source of truth.

## Project Structure

- Gradle modules: `:app`, `:baselineprofile`
- App namespace/applicationId: `com.finnvek.knittools`
- Main source root: `app/src/main/java/com/finnvek/knittools`
- Baseline profile namespace: `com.finnvek.knittools.baselineprofile`

Main source packages:

- `ai` - Firebase AI, Gemini Nano/on-device parsers, voice AI, journal AI
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

Release Ravelry credentials are also read from environment variables:

```bash
export KNITTOOLS_RAVELRY_BASIC_AUTH_USER=user
export KNITTOOLS_RAVELRY_BASIC_AUTH_PASSWORD=password
export KNITTOOLS_RAVELRY_OAUTH2_CLIENT_ID=client-id
export KNITTOOLS_RAVELRY_OAUTH2_CLIENT_SECRET=client-secret
export KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS=true
```

## Current Documentation

- [`PROJECT.md`](PROJECT.md) - current code-backed project map
- [`AGENTS.md`](AGENTS.md) / [`CODEX.md`](CODEX.md) - agent working rules
- [`CLAUDE.md`](CLAUDE.md) - product, UX, and visual direction notes
- [`ONLINE_OFFLINE.md`](ONLINE_OFFLINE.md) - feature network requirements

## License

MIT
