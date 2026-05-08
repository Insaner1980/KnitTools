KnitTools Codex Instructions

Keep this file aligned with `AGENTS.md`. If one changes, update the other in the same change.

Use [`CLAUDE.md`](CLAUDE.md) when product wording, visual direction, or UX structure matters.

## Snapshot

- Gradle modules are `:app` and `:baselineprofile`
- Android app lives in `app`
- Baseline profile tests live in `baselineprofile`
- Main app namespace/applicationId is `com.finnvek.knittools`
- Baseline profile namespace is `com.finnvek.knittools.baselineprofile`
- Kotlin + Jetpack Compose + Material 3
- Hilt, Room, DataStore, Glance
- Room schema version `7`
- AGP 9 built-in Kotlin is in use

## Architecture

- Main source root is `app/src/main/java/com/finnvek/knittools`
- Top-level source packages are `ai`, `auth`, `billing`, `data`, `di`, `domain`, `pro`, `repository`, `ui`, `util`, and `widget`
- `data/` owns Room, DataStore, storage, Android framework access
- `data/local/EntityMappers.kt` owns Room entity `<->` domain model conversion; mapper tests must preserve every entity field and default
- `data/remote` owns Ravelry API models/service; `data/storage` owns app file creation helpers
- `domain/` owns calculation logic and Room-free domain models
- `repository/` is the seam between storage/framework details and UI consumers
- `CounterRepository`, `SavedPatternRepository`, `ProgressPhotoRepository`, `YarnCardRepository`, `PatternAnnotationRepository`, `ProjectCounterRepository`, and `ReminderRepository` expose domain models publicly and map to Room entities internally
- Room entity types are `data/local` storage details and should not leak into `ui/` or `domain/calculator`
- `ui/` owns screens, navigation, theme, and ViewModels; journal UI and `JournalEntryViewModel` live in `ui/screens/notes`
- `ai/` owns Firebase AI integrations, AI voice command action contracts, and remaining on-device parsers; journal AI processing lives in `ai/journal`; yarn label parsing is Gemini-only, while yarn label photo file creation belongs in `data/storage`
- `auth/` owns Ravelry authentication
- `billing/` and `pro/` own billing, trials, Pro feature access, reviews, and updates
- `di/` owns Hilt modules
- `widget/` owns Glance home screen widget code
- Keep business logic out of composables when a ViewModel or use case should own it

## Navigation Rules

- Top-level tabs are `Projects`, `Library`, `Tools`, `Insights`, `Settings`
- `TopLevelDestination` in [Screen.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt) is the source of truth
- `CounterViewModel` is shared at the Projects graph level
- `LibraryViewModel` is shared at the Library graph level
- Do not turn `Tools` back into a generic dashboard grid

## UI Rules

- All user-visible strings go in `res/values/strings.xml`
- Use theme tokens and `MaterialTheme.knitToolsColors`, not hardcoded colors
- Scaffold background should use the app `background` color
- Reuse `ToolScreenScaffold` and shared UI components before adding feature-local scaffolds
- Avoid inline typography overrides except documented project exceptions

## Data And Build Rules

- Room changes must keep the migration chain and schema export coherent
- Do not bypass repositories from UI code just because a DAO is nearby
- Do not add back `org.jetbrains.kotlin.android`
- Do not reintroduce `android.disallowKotlinSourceSets`, `android.newDsl`, or `android.builtInKotlin` toggles unless absolutely necessary
- Release signing must stay environment-variable-driven

## Security

- Keep `usesCleartextTraffic` disabled unless explicitly justified
- Exported components must stay intentional and minimal
- Keep `FileProvider` usage least-privilege
- Do not log billing state, label scan content, AI prompt content, or user project data

## Working Conventions

- Comments and commit messages should be in Finnish
- Prefer explicit imports
- Avoid wildcard imports
- Avoid `!!`
- Prefer minimal targeted edits over broad rewrites

## Verification

- Prefer the smallest useful check
- Typical commands: `./gradlew assembleDebug`, `./gradlew test`, `./gradlew :app:detekt`, `./gradlew lint`
- Do not run the user's wrapper scripts such as `lint-check` or `security-check`
- Never commit generated `reports/`
