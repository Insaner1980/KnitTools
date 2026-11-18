# KnitTools

KnitTools is an Android workspace for knitting and crochet, built with Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, Google Play Billing, and Glance widgets. Local projects and tools work without an app account.

## Features

- Projects with folders, sorting, completion/reopening, and a separate overview and working counter.
- Row/round/custom-label counting, targets, Undo/history, secondary and additional counters, linked/repeating/shaping counters, repeat sections, stitches, and row reminders.
- Explicit work sessions with background-safe timing anchors, recovery review, session history, and Insights.
- Saved Pattern metadata and website links; multiple project PDFs with independent reader state, guides, calibration, bookmarks, annotation layers/chart tools, and marked-PDF export.
- PDF Open/Share import and ordered camera/gallery photo-to-PDF creation.
- Progress photos, My Yarn, project yarn notes, and independent planned/allocated/used yarn amounts.
- Local measurements/gauge, cast-on, increase/decrease and yarn calculators, plus needle/size/abbreviation/chart-symbol references.
- Home-screen widgets, 12 selectable locale options across 11 languages (including both Portuguese variants), light/dark/system theme selection, and manual backup/replacement restore.

The five main tabs are Projects, Library, Tools, Insights, and Settings. A user-started 14-day trial and the one-time Play product `knittools_pro` unlock Pro features. Free use allows one active project; completed projects and existing content are retained. New Library yarn-card creation requires Pro, with no numeric free-card allowance. Detailed gates and continued-access rules are in [PROJECT.md](PROJECT.md#pro-trial-and-billing).

## Ravelry And Network Behavior

Ravelry opens in the app's WebView. Users sign in on Ravelry's own website; website cookies retain the session. Selected HTTPS PDF downloads go directly into Save Pattern for project attachment or Library storage. Checkout/payment links open externally. General user-added web patterns open in an external browser.

Android no longer implements Ravelry Firebase authentication, OAuth callbacks, API search or callable metadata import. The backend code in `functions/` remains in the repository but has no current Android consumer. Previously saved Ravelry metadata is still readable.

Current Android Firebase usage is release Crashlytics. PostHog usage analytics is release-only, optional, and off by default; debug Sentry requires a developer-configured DSN. Local backup uses Android's document picker, including available cloud file providers, without Drive/Dropbox SDKs or continuous synchronization. See [ONLINE_OFFLINE.md](ONLINE_OFFLINE.md) and the draft [PRIVACY-POLICY.md](PRIVACY-POLICY.md).

## Project Structure

| Area | Responsibility |
| --- | --- |
| `app` | Android application, resources, tests, and exported Room schemas |
| `baselineprofile` | Android profile/benchmark test module targeting the app |
| `functions` | Retained Firebase Functions v2 Ravelry backend; independently built/tested |
| `domain` | Domain models and local calculation/validation rules |
| `data` | Room, DataStore, Android storage/providers, timing, backup, and website PDF downloading |
| `repository` | Transactional storage/framework boundary for UI consumers |
| `ui` | Compose screens, components, navigation, theme, and ViewModels |
| `di`, `billing`, `pro`, `analytics`, `widget` | Injection, Play/trial gates, consent-aware analytics, and Glance |

Package entries are under `app/src/main/java/com/finnvek/knittools`. The application ID/namespace is `com.finnvek.knittools`; Gradle includes `:app` and `:baselineprofile`. The profile module namespace is `com.finnvek.knittools.baselineprofile`.

## Build And Test

Use the checked-in wrapper with **JDK 17 (Eclipse Temurin)** and the configured Android SDK. Current configuration uses Gradle 9.8.1, compile/target SDK 37, minimum SDK 29, and Room schema 28. The version catalog and Gradle files are authoritative.

From Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:ktlintCheck
.\gradlew.bat :app:detekt
.\gradlew.bat :app:lintDebug
```

On Linux/macOS, use `./gradlew` for the same tasks. These are available commands, not execution results. Instrumentation and Baseline Profile generation need a separately selected emulator/device and the owning test configuration. Use the isolated debug profiles for the destructive active-policy/notes campaigns described in PROJECT.md. Do not commit generated `reports/` output.

For retained backend maintenance, Node 22 and the independent lockfile are configured:

```bash
npm --prefix functions ci --ignore-scripts
npm --prefix functions test
```

Backend tests do not establish a deployed service or an Android integration.

## Release Configuration

Release artifact tasks require the following private configuration; do not commit values or signing files:

| Configuration | Mechanism |
| --- | --- |
| Signing | `KNITTOOLS_KEYSTORE_PATH`, `KNITTOOLS_KEYSTORE_PASSWORD`, `KNITTOOLS_KEY_ALIAS`, `KNITTOOLS_KEY_PASSWORD`; keystore must exist |
| Firebase Crashlytics | Ignored `app/google-services.json`, or `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64` to generate it locally |
| PostHog ingestion configuration | `KNITTOOLS_POSTHOG_PROJECT_TOKEN`, or ignored `posthog.properties` with `projectToken`; user analytics consent still defaults off |

Debug and profile-producer variants can generate ignored placeholder Firebase configuration when real configuration is absent. Release artifacts cannot use that fallback. A lint-only release check can run without producing a configured/signed release artifact.

Release minifies/shrinks resources and configures Crashlytics mapping upload. Android Firebase Auth/Functions dependencies are forbidden by the current release boundary. Retained backend Ravelry credentials belong in Secret Manager, not Android. The written Ravelry permission requirement before Play release remains recorded in [config/security-decisions.md](config/security-decisions.md); configuration/source inspection is not release certification.

## Documentation

- [PROJECT.md](PROJECT.md): detailed source-backed product, architecture, data, storage, integration, QA, and development reference. Its snapshot and historical evidence dates matter.
- [AGENTS.md](AGENTS.md) / [CODEX.md](CODEX.md): aligned working rules.
- [CLAUDE.md](CLAUDE.md): product wording, UX, and visual direction.
- [ONLINE_OFFLINE.md](ONLINE_OFFLINE.md): current feature network requirements.
- [PRIVACY-POLICY.md](PRIVACY-POLICY.md): current technical privacy draft; publisher contact remains missing.

Older plans and reports describe intent or dated evidence; current implementation/configuration wins when they disagree. This overview was checked against source on 2026-10-11; no new build, device, provider, billing, telemetry, or deployment run was performed for this documentation update.

## License

[MIT](LICENSE)
