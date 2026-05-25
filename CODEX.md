KnitTools Codex Instructions

Keep this file aligned with `AGENTS.md`. If one changes, update the other in the same change.

Use [`CLAUDE.md`](/home/emma/dev/KnitTools/CLAUDE.md) when product wording, visual direction, or UX structure matters.

## Snapshot

- Android app in `app` plus `baselineprofile`
- Kotlin + Jetpack Compose + Material 3
- Hilt, Room, DataStore, Glance
- Room schema version `12`
- AGP `9.1.0` + Kotlin Compose plugin `2.3.10`

## Architecture

- `data/` owns Room, DataStore, storage, Android framework access
- `domain/` owns calculation logic and domain models
- `repository/` is the seam between storage/framework details and UI consumers
- `ui/` owns screens, navigation, theme, and ViewModels
- Coroutine dispatchers that cross architectural boundaries are provided through `di/DispatchersModule` (`@IoDispatcher`); avoid hardcoded `Dispatchers.IO` in repositories and ViewModels
- Multi-step Room writes that span DAOs go through `data/local/DatabaseTransactionRunner` from repository methods; UI code must not split bidirectional yarn/project links or pattern attachment writes into separate persistence calls
- Widget row count changes go through `CounterRepository.applyWidgetCountChange`, which reads the current project row and writes count, history, and current-stitch reset inside one repository transaction
- Project note replacement writes go through `CounterRepository.saveProjectNotes`, which merges against the editor's base notes so concurrent editor flows are preserved instead of overwritten
- Session rows store both display minutes and exact `durationSeconds`/`rowsWorked`; insights pace calculations must use the exact fields and split cross-midnight sessions by the device local date
- Insights screen state is aggregated in `InsightsUiState`; heavy session-history calculations should run upstream with `@IoDispatcher` before Compose collects the single UI state
- Legacy secondary counter state lives in `counter_projects.secondaryCount`; `project_counters` is only for named extra, repeating, shaping, and repeat-section counters, migrations must not duplicate `secondaryCount` into `project_counters`, and old generated `Pattern repeat` backfill copies are ignored at the counter UI boundary
- Extra counter type rules are owned by `domain/model/ProjectCounterType` plus `domain/calculator/ProjectCounterLogic`; repositories should apply those domain rules inside a transaction instead of duplicating counter behavior in DAO SQL
- Yarn/project link writes go through `YarnCardRepository`: `saveCard` normalizes any persisted `linkedProjectId`, and `updateLinkedProjectId` is the canonical explicit relink writer for `yarn_cards.linkedProjectId` plus `counter_projects.yarnCardIds`
- Yarn-card ID CSV parsing/formatting is owned by `domain/model/YarnCardLinks`; callers should not split or join `counter_projects.yarnCardIds` ad hoc
- Project-only yarn notes live in `project_yarn_notes` and go through `ProjectYarnNoteRepository`; `saveToMyYarn` creates a linked `YarnCard` while preserving the project note row through `savedYarnCardId` in one repository transaction
- Yarn card detail routes observe the target card through `YarnCardRepository.observeCard`; the route must leave the detail screen if the row disappears, and detail edits should rely on repository write results plus the observed row instead of optimistic local-only state
- Pattern attach/detach database state goes through `CounterRepository.attachPattern` / `detachPattern` so saved-pattern rows, annotations, and project pattern fields stay atomic
- Pattern PDF files are app-owned documents under `pattern_pdfs/<projectId>`; `SavedPatternRepository.deleteLocalPatternFileIfUnused` is the cleanup gate after saved-pattern deletion, project detach, and project deletion
- Pattern camera capture is a photo-to-PDF flow: user-facing copy must use photo/PDF wording instead of scan/scanner wording, temp images live under `pattern_captures/<projectId>`, and only pattern/progress photo paths are exposed through FileProvider; FileProvider authority and share URI creation go through `AppFileStorage`, while legacy `patterns/...` and `yarn_photos/...` URIs are resolved internally by `AppFileStorage` for cleanup/read compatibility
- Keep business logic out of composables when a ViewModel or use case should own it
- Runtime app language is owned by AppCompat/Android per-app locale APIs; DataStore `app_language` is only a persistence and migration mirror managed by `PreferencesManager`
- Ravelry is intentionally backendless: OAuth authorization requests include PKCE, but release builds may embed Ravelry Basic Auth credentials and OAuth client secret after explicit `KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS=true` opt-in; keep `config/security-decisions.md` aligned with this accepted risk
- Voice/microphone commands are intentionally absent from the counter; do not reintroduce SpeechRecognizer, TextToSpeech, or conversational voice without a new explicit product/security decision
- Paste-to-parse uses the regex-only `domain/calculator/InstructionParser`; keep model-backed parser code out of calculator UI
- Notes editing is local-only; do not reintroduce cloud journal processing or cloud cleanup without a new explicit product/security decision
- PDF rendering lives in `data/storage/PdfPageRenderer`; pattern UI should not define renderer copies

## Navigation Rules

- Top-level tabs are `Projects`, `Library`, `Tools`, `Insights`, `Settings`
- `TopLevelDestination` in [Screen.kt](/home/emma/dev/KnitTools/app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt) is the source of truth
- `CounterViewModel` is shared at the Projects graph level
- `LibraryViewModel` is shared at the Library graph level
- Widget counter launches carry a `CounterLaunchRequest.requestId`; `MainActivity` clears consumed launch extras and saves the consumed id across recreation
- Widget counter launch ids must be issued by `data/storage/CounterLaunchTokenStore`; `MainActivity` must ignore untrusted counter extras and OAuth callback intents must not trigger counter navigation
- Pattern viewer entry points require an attached PDF URI; Ravelry pattern links are metadata until a local PDF is attached
- Do not turn `Tools` back into a generic dashboard grid
- Project list sort order is `domain/model/ProjectSortOrder`; DataStore persists its `persistedValue`, but UI/repository code should use the enum

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
- Debug-only Ravelry credentials belong in ignored `debug.credentials.properties`, not `local.properties`; release Ravelry credentials come from `KNITTOOLS_RAVELRY_*` environment variables and require explicit embedded-secret opt-in
- Firebase, Google Services, App Check, and model-backed parser dependencies are intentionally absent after model-backed feature removal; do not add them back as transitive convenience dependencies

## Security

- Keep `usesCleartextTraffic` disabled unless explicitly justified
- Ravelry Basic Auth credentials and OAuth client secret are an accepted no-backend risk only when documented in `config/security-decisions.md` and gated by the release opt-in; DeepSec marks only the documented Ravelry `secrets-exposure` findings as accepted-risk after revalidation
- Exported components must stay intentional and minimal
- Treat extras on exported activities as untrusted unless they are explicitly validated against app-owned state
- Keep `FileProvider` usage least-privilege
- Do not log billing state, voice transcripts, Ravelry credentials, pattern text, or user project data

## Working Conventions

- Comments and commit messages should be in Finnish
- Prefer explicit imports
- Avoid wildcard imports
- Avoid `!!`
- Prefer minimal targeted edits over broad rewrites

## Verification

- Prefer the smallest useful check
- Project-local PowerShell wrappers are two-letter `tools/*.ps1` scripts; check wrappers delegate to `C:\Dev\Android-check\tools\AndroidProjectChecks.psm1`, and `ad` delegates to `C:\Dev\Android-check\tools\InstallDebugToDevice.ps1`
- `lc` runs ktlint, detekt, and Android lint into `reports/ktlint.txt`, `reports/detekt.txt`, and `reports/lint.txt`
- `ad`, `ac`, `dc`, `ss`, `ds`, `ms`, `os`, `ql`, `db`, `pc`, `cs`, `cr`, `ga`, and `sc` are project-local wrappers; use `-PlanOnly` or `-ResolveOnly` for dry checks where supported
- `ad` builds `assembleDebug`, resolves `adb.exe` from `local.properties` `sdk.dir`, and installs `app/build/outputs/apk/debug/app-debug.apk` with `adb install -r`; use `ad -NoBuild` to install an already-built APK
- `pc` runs PMD CPD duplicate detection with KnitTools' default `PMD_CPD_MINIMUM_TOKENS=100`, `cr` runs compose-rules through ktlint/detekt, `ga` runs Android Lint with Google Android Security Lints, and `cs` is available for Compose Stability Analyzer projects.
- `sc` runs dependency, secret, and light Semgrep checks; `sc -Full` also runs the Android-specific `ac` path and DeepSec custom report
- Typical commands: `./gradlew assembleDebug`, `./gradlew test`, `./gradlew :app:detekt`, `./gradlew lint`
- Do not run the user's wrapper scripts such as `lc` or `sc`
- Never commit generated `reports/`
