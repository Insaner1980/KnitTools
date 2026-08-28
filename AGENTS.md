# KnitTools Repository Instructions

Keep `AGENTS.md` and `CODEX.md` aligned. If one changes, update the other in the same change.

Use [`CLAUDE.md`](CLAUDE.md) when product wording, visual direction, or UX structure matters.

## Snapshot

- Android app in `app` plus `baselineprofile`; Ravelry Firebase backend in `functions`
- Kotlin + Jetpack Compose + Material 3
- Hilt, Room, DataStore, Glance
- Room schema version `23`
- AGP `9.3.1` + Kotlin Compose plugin `2.4.10`
- Gradle daemon and CI use Eclipse Temurin JDK `17`
- Detekt `2.0.0-alpha.5` and Baseline Profile/Benchmark `1.5.0-beta01` are temporary AGP 9 compatibility exceptions; keep them until stable compatible releases are verified

## Architecture

- Scanneripoikkeukset ovat `config/check-exceptions.json`-rekisterissä. MobSF-poikkeus vaatii yhden säännön ja yhden täsmällisen `findingPath`-polun; `.mobsf` ei saa piilottaa sääntöä globaalisti.
- `data/` owns Room, DataStore, storage, Android framework access
- `domain/` owns calculation logic and domain models
- `repository/` is the seam between storage/framework details and UI consumers
- `ui/` owns screens, navigation, theme, and ViewModels
- Coroutine dispatchers that cross architectural boundaries are provided through `di/DispatchersModule` (`@IoDispatcher`); avoid hardcoded `Dispatchers.IO` in repositories and ViewModels
- Application-lifetime coroutine work uses the Hilt-owned `@ApplicationScope` from `di/DispatchersModule`; ViewModels may use it only for final persistence that must outlive `viewModelScope`, and blocking work launched there must still select the injected `@IoDispatcher`
- Multi-step Room writes that span DAOs go through `data/local/DatabaseTransactionRunner` from repository methods; UI code must not split bidirectional yarn/project links or pattern attachment writes into separate persistence calls
- Destructive repository operations capture app-owned cleanup inputs before the database transaction, commit the authoritative database mutation first, and then perform best-effort physical cleanup. A database failure or pre-commit cancellation leaves files intact; a post-commit cleanup failure may leave an orphan but must not report the committed deletion as rolled back. Shared pattern PDFs still go through `SavedPatternRepository.deleteLocalPatternFileIfUnused`.
- Debug sample data keeps its root `DemoDataSeeder` as a build-variant facade only; `data/local/DebugDemoDataSeeder` orchestrates one `DatabaseTransactionRunner` transaction and reuses `ProjectCounterRepository`/`YarnCardRepository` writers for their domain and bidirectional-link invariants, while release remains a no-op
- UI-facing Room observation flows apply `repository/retryOnRepositoryReadFailure` after DAO-to-domain mapping; the shared retry boundary preserves cancellation and uses capped backoff so a single read or mapping exception cannot permanently terminate ViewModel state sharing
- Main counter changes go through `CounterRepository.applyMainCounterChange`, which reads the current project row and writes count, history, current-stitch reset, and linked-counter deltas inside one repository transaction; widget row count changes delegate to the same semantics through `applyWidgetCountChange`
- Project craft type and main counter label are owned by `domain/model/CraftType` and `domain/model/MainCounterLabelType`; legacy projects default to `KNITTING` + `ROWS`, crochet projects default to `ROUNDS`, and custom labels are trimmed and limited centrally before persistence/display
- Main counter display text is shaped by `domain/calculator/CounterValueFormatter`; Compose maps its slots to localized strings for hero labels, target rows, button content descriptions, and project-card count text
- Extra-counter linked state is stored in `ProjectCounter.linkedToMainCounter` from the add/edit counter draft; repeat-section counters must not be marked linked because they already derive progress from main-counter rows
- Project note replacement writes go through `CounterRepository.saveProjectNotes`, which merges against the editor's base notes so concurrent editor flows are preserved instead of overwritten
- Session rows store display minutes, exact `durationSeconds`/`rowsWorked`, and nullable `zoneId`; new sessions capture the device zone at session start, legacy null zones fall back to the current device zone, and insights must use the session zone for cross-midnight day and pace-bucket splitting
- Background-safe work sessions are explicit Start/Stop actions backed by the single Room-owned `active_sessions` row. Wall-clock, elapsed-realtime, boot-count, zone, row, recovery, and stable token anchors survive process death; background correctness comes from those anchors, not background ticking. Main app and widget counter changes checkpoint duration and net rows in the same transaction as the count. Reboot, unavailable boot identity, malformed anchors, and an unreviewed 24-hour interval require user review. Recovery Add continues from new trusted anchors, while Discard and Edit finalize at most once through the stable recovery token. Completed rows remain separate in `sessions`; finalization is a repository-owned, idempotent transaction, and UI prompts do not replace its invariants. Navigation, app backgrounding, history opening, and ViewModel clearing must not start or finalize a session.
- Insights screen state is aggregated in `InsightsUiState`; repository session/project flows carry an explicit loading state so seeded empty lists cannot replace the skeleton before Room emits, exact minutes-per-row uses `durationSeconds`, and heavy session-history calculations should run upstream with `@IoDispatcher` before Compose collects the single UI state
- Legacy secondary counter state lives in `counter_projects.secondaryCount`; `project_counters` is only for named extra, repeating, shaping, and repeat-section counters, migrations must not duplicate `secondaryCount` into `project_counters`, and old generated `Pattern repeat` backfill copies are ignored at the counter UI boundary
- Extra counter type rules are owned by `domain/model/ProjectCounterType` plus `domain/calculator/ProjectCounterLogic`; repositories should apply those domain rules inside a transaction instead of duplicating counter behavior in DAO SQL
- Yarn/project link writes go through `YarnCardRepository`: `saveCard` normalizes any persisted `linkedProjectId`, and `updateLinkedProjectId` is the canonical explicit relink writer for `yarn_cards.linkedProjectId` plus `counter_projects.yarnCardIds`
- Yarn-card ID CSV parsing/formatting is owned by `domain/model/YarnCardLinks`; callers should not split or join `counter_projects.yarnCardIds` ad hoc
- Project-only yarn notes live in `project_yarn_notes` and go through `ProjectYarnNoteRepository`; `saveToMyYarn` creates a linked `YarnCard` while preserving the project note row through `savedYarnCardId` in one repository transaction
- Yarn card detail routes observe the target card through `YarnCardRepository.observeCard`; the route must leave the detail screen if the row disappears, and detail edits should rely on repository write results plus the observed row instead of optimistic local-only state
- Yarn card photo updates go through `YarnCardRepository.updatePhotoUri`, which copies the selected image with `data/storage/YarnPhotoStorage` into app-owned `yarn_photos/<cardId>` storage, cleans up failed replacement copies, and deletes the old app-owned photo only after the new URI is persisted; `App.onCreate` schedules `YarnCardRepository.pruneUnreferencedPhotoFiles` so interrupted swaps leave only restart-cleanable orphans; yarn photos are not FileProvider-exposed share paths
- Pattern attach/detach database state goes through `CounterRepository.attachPattern` / `detachPattern` so saved-pattern rows, annotations, and project pattern fields stay atomic
- Pattern PDF files are app-owned documents under `pattern_pdfs/<projectId>`; `SavedPatternRepository.deleteLocalPatternFileIfUnused` is the cleanup gate after saved-pattern deletion, project detach, and project deletion
- Saved patterns use source metadata through `domain/model/SavedPatternSource` and Room schema 14 fields: `source`, nullable `ravelryPatternId`, `originalUrl`, `canonicalUrl`, nullable `localPdfUri`, `isAvailableOffline`, `updatedAt`, and nullable `lastSyncedAt`; do not reintroduce `ravelryId = 0` or `patternUrl` as persisted saved-pattern sentinels. Repository duplicate detection checks Ravelry ID, canonical URL, normalized original URL, then title+designer only when explicitly requested.
- Pattern PDF import, including the v1 Drive/Dropbox copy, stays on Android Storage Access Framework `OpenDocument(application/pdf)` plus persistable URI grants; do not add Drive/Dropbox SDKs, OAuth, provider-specific dependencies, or a separate import storage flow before a new sync spec
- Drive/Dropbox sync is future-spec work tracked in `config/future-sync-spec.md`. Manual export/import or backup/restore comes before continuous sync. Do not market cross-device sync until conflict handling, background sync, offline behavior, OAuth/token storage, and the Pro gate are specified and implemented
- Attached project PDF reading assistance persists page, horizontal visibility/Y/follow state, and vertical visibility/X per `project_documents` relation; the retained fields on `counter_projects` are migration compatibility data, not a second source of truth. Manual page or horizontal-line movement pauses follow without creating a row marker, while the explicit row-marker and two-point calibration actions remain the only marker writers. Library horizontal and vertical guides stay `rememberSaveable(patternUri)` state and must not create project or saved-pattern schema paths.
- Canonical app and widget counter changes resolve followed reading-line page/Y inside the same `CounterRepository.applyMainCounterChange` transaction as count, history, stitch reset, and linked counters. `resolveReadingLineLocation` prefers an exact current-page marker, then an unambiguous exact other-page marker or same-page bracket, and otherwise applies conservative row-delta fallback without guessing across ambiguous pages; mapping changes and return-to-row re-evaluate with zero delta.
- Project pattern bookmarks are project-owned Room rows keyed by the active project annotation layer's `documentKey`; deterministic order is page/Y/creation/id, detach preserves rows, same-document reattach restores them, replacement hides the old document's rows, and project deletion cascades. Bookmark jumps validate the active document, update page/Y with follow paused, and issue a one-shot rendered-page viewport focus request; bookmarks and reading guides are not annotations, are excluded from annotated export, and are not Pro-gated.
- Pattern PDF annotations use Room schema 17 tables `pattern_annotation_layers` and `pattern_annotations`: a saved-pattern master layer is reusable and read-only in project viewers, while each project owns an editable overlay layer keyed by the same `documentKey`; attach activates or creates the project layer, detach deactivates it without deleting edits, and project/saved-pattern deletion cascades through layer ownership.
- Room schema 18 stores monotonic `secondaryCounterUsed` and `notesCreated` project flags so previously created compact-counter and note content stays usable after Pro access ends; migrations mark legacy projects as used while new projects start with both flags false.
- Room schema 19 persists saved-pattern availability through `domain/model/PatternAvailability` as stable `free`, `paid`, or `unknown` values. Migration 18 to 19 maps legacy true to `free` and false to `unknown`, and must preserve saved-pattern and project annotation layers while rebuilding the referenced table.
- Room schema 20 adds project-owned `pattern_bookmarks` plus `readingLineFollowCurrentRow`, `verticalReadingGuideEnabled`, and `verticalReadingGuideXFraction`; migration 19 to 20 preserves saved patterns, project links, gallery-import PDF state, row mappings, annotation layers, annotations, and annotation triggers.
- Room schema 21 adds the fixed-primary-key, project-owned `active_sessions` table and its singleton triggers. Migration 20 to 21 leaves completed sessions and all project, pattern, annotation, bookmark, yarn, reminder, and photo data unchanged.
- Room schema 22 adds project-owned `project_documents` as the canonical source for readable project PDFs and their document-specific reader state. Migration 21 to 22 backfills one primary document from every nonblank legacy project PDF without reconstructing `counter_projects`; the retained legacy pattern and reader columns are compatibility data, not a second source of truth.
- Room schema 23 adds only `project_folders` and `project_folder_assignments`, leaving every earlier table unchanged. All Projects and Unfiled are typed virtual views, never rows or sentinel IDs; a missing assignment means Unfiled, and each project has at most one assignment. Folder deletion preserves projects; project deletion cascades its assignment without deleting the folder.
- `ProjectFolderRepository` owns metadata-only folder and membership transactions, typed errors, cancellation, and one coherent retrying organization observation. Folder names preserve trimmed display text, reject controls/line separators, and use NFC plus `Locale.ROOT` lowercase uniqueness with a 50-unit Kotlin string limit. Bulk moves validate all distinct project IDs and the destination before writing; folder operations must not change project timestamps, sessions, files, documents, photos, or yarn. `CounterRepository.createProject` checks an optional target folder and writes the project and assignment in the same Pro-limit-checked transaction.
- `ProjectListViewModel` owns the folder filter in `SavedStateHandle`, not DataStore. New tasks start in All Projects; restored folder IDs are validated only after a real Room snapshot. Filter existing SQL-sorted active/completed flows and the Continue hero only in Projects; completed visibility stays global, selection is limited to visible rows, and Library, Insights, history, widgets, and project-ID navigation remain unfiltered.
- `ProjectDocumentRepository` owns document creation, rename, order, primary selection, removal, availability, and post-commit reference-aware file cleanup. A nonempty project has exactly one primary document, enforced by repository transactions and database triggers; UI and ViewModels must not call the DAO directly.
- `projectDocumentId` identifies and orders the project relation, while stable `documentKey` values continue to own annotations and bookmarks. Reader state is per document, the active document drives counter reading-line updates, and Saved Pattern or project deletion must preserve shared files until neither Library, `project_documents`, nor a still-relevant legacy URI references them.
- Annotation geometry is stored in normalized page coordinates and transformed exactly once by `PatternPageCoordinateTransform`; pointer moves stay in ViewModel memory and persistence occurs at gesture/command boundaries. `PatternAnnotationCanvasRenderer` is the single renderer for viewer overlays and rasterized export.
- Annotated PDF export uses SAF `CreateDocument(application/pdf)`, renders bounded bitmaps page by page through `PdfPageRenderer`, writes a cache temp PDF before copying to the destination, reports progress, preserves the source PDF, and always cleans its temp file on success, failure, or cancellation.
- Pattern camera capture is a photo-to-PDF flow: user-facing copy must use photo/PDF wording instead of scan/scanner wording, temp images live under `pattern_captures/<projectId>`, and only pattern/progress photo paths are exposed through FileProvider; FileProvider authority and share URI creation go through `AppFileStorage`, while legacy `patterns/...` and `yarn_photos/...` URIs are resolved internally by `AppFileStorage` for cleanup/read compatibility
- Gallery image pattern import uses the system Photo Picker and copies each accepted image immediately into an app-owned `pattern_captures/<projectId>/<sessionId>` session. `PatternImageImportViewModel` owns ordering, cancellation, and restoreable pending state; `PatternDocumentStorage` owns bounded decode and one-page-at-a-time PDF creation with limits of 20 pages, 25 MiB per image, 200 MiB per session, an 1800-pixel long edge, and a 32 MiB free-space reserve. External image URIs are never stored as permanent project state, and the generated app-owned PDF is attached only through `CounterRepository`.
- Progress photo capture-target creation and abandoned-capture cleanup go through `ProgressPhotoRepository` and `CounterViewModel` on `@IoDispatcher`; composables must not instantiate `ProgressPhotoStorage` or delete progress-photo files directly
- Keep business logic out of composables when a ViewModel or use case should own it
- Compose-haptiikka kulkee juuritasolla `ui/PreferenceAwareHapticFeedback`-portin kautta, jotta DataStoren `hapticFeedback`-asetus koskee myös komponenttien oletushaptiikkaa kuten `combinedClickable`-pitkäpainalluksia; counterin eksplisiittiset värinäkutsut pysyvät lisäksi saman preferenssin takana
- Runtime app language is owned by AppCompat/Android per-app locale APIs; DataStore `app_language` is only a persistence and migration mirror managed by `PreferencesManager`
- `App.onCreate` launches `PreferencesManager.applyStoredAppLanguage()` from the application coroutine scope; do not reintroduce `runBlocking` for startup locale reads
- Ravelry's old backendless accepted-risk decision is superseded by `Ravelry Firebase Backend And Saved Patterns Plan.md` and tracked in `config/ravelry-backend-progress.md`; Android no longer owns Ravelry secrets, token exchange, token storage, or Basic Auth fallback
- Ravelry backend lives in `functions/` as Firebase Functions v2 TypeScript, deployed through root `firebase.json` with Firestore rules in `firestore.rules`; Phase 3 implements backend-owned OAuth2 start/callback/status/disconnect/current-user flow, `ravelryOAuthStates/{state}` PKCE state storage with the OAuth-start token `connectionGeneration`, and `ravelryTokens/{uid}` token storage; Phase 4 adds backend `ravelrySearchPatterns`, `ravelryImportPatternById`, and `ravelryImportPatternByUrl` metadata-only callables that sanitize Ravelry fields, never download pattern PDFs, and enforce per-UID plus ten-shard fixed-window backend-global Firestore rate limits in `ravelryRateLimits`; rollout compatibility checks an active legacy `<bucket>_global` window on every request before using shards, and this temporary check must be removed in a separate cleanup only after deployment has completed and all legacy single-document writers are confirmed drained; a warm Functions process caches a confirmed saturated shard window so repeated overload rejections do not rescan all shards; Phase 5 adds Android Firebase Auth/Functions dependencies, anonymous auth, and `RavelryBackendClient`; Phase 6 makes `RavelryAuthManager` own backend auth status/start/disconnect/callback state, opens auth through Auth Tab with Custom Tabs fallback, and handles only token-free `knittools://ravelry-auth-complete` callbacks in Android UI; Phase 7 moves saved patterns to schema 14 source metadata while preserving existing saved-pattern IDs; Phase 8 completes Ravelry UI and saved-pattern UX: Android `ACTION_SEND text/plain` imports validated Ravelry pattern URLs but must show local confirmation before backend import preview, connected Browse Ravelry opens Custom Tabs with share enabled, `SavedPatternDetailScreen` owns metadata availability/actions, PatternPickerSheet lists all saved patterns, and project pattern cards open SavedPatternDetail for metadata-only links while attached PDFs still open the PDF viewer; Phase 9 hardens `tools/release-surface.ps1` so only Firebase Auth/Functions/Google Services are allowed for this backend, Firebase AI/ML Kit/Gemini/voice remain forbidden, tracked `app/google-services.json` fails, and locally/env-known Ravelry secret values are scanned without printing them
- Android Ravelry backend response mapping preserves sanitized availability through `domain/model/PatternAvailability` (`free`/`paid`/`unknown`) and preserves backend `canonicalUrl`/`originalUrl` on `PatternDetail`; persistence and Ravelry cards keep that exact value instead of deriving unknown as paid, and detail responses without a positive `ravelryPatternId` are rejected instead of saved as ID 0
- Remote Ravelry thumbnails use the explicit Coil 3 Ktor network artifact through the shared HTTPS-only `ui/components/RemotePatternImage`; absent, malformed, non-HTTPS, and failed images leave no placeholder or duplicate project-owned image state
- Ravelry backend access-token use goes through `functions/src/ravelry/tokenAccess.ts`; expired `ravelryTokens/{uid}` access tokens refresh server-side with Secret Manager Ravelry credentials only after the owning auth/search/import limiter has passed, rotated refresh tokens replace old stored refresh tokens only when the token connection generation is still current, and disconnect advances the token document's `connectionGeneration` tombstone so late callback/current-user/refresh writes cannot recreate a disconnected token
- Debug-only Pro override is centralized in `ProState.hasFeature` through `BuildConfig.DEBUG`; it opens feature gates in debug builds without changing `isPro`, billing purchase state, trial state, or Pro upgrade UI purchase claims
- Cold-start widget Pro gates use `ProManager.hasFeatureAfterInitialLoad` and `BillingManager.purchaseStateReady`; widget code must not synchronously fail closed from the default `ProState` before initial trial/billing state has had a chance to load
- Debug-only Sentry diagnostics live under `app/src/debug` and use `io.sentry:sentry-android-core` only through `debugImplementation`; the release source set is a no-op and release builds must stay free of `io.sentry` dependencies
- Voice/microphone commands are intentionally absent from the counter; do not reintroduce SpeechRecognizer, TextToSpeech, or conversational voice without a new explicit product/security decision
- Paste-to-parse uses the regex-only `domain/calculator/InstructionParser`; keep model-backed parser code out of calculator UI
- Measurements and Gauge V1 keeps raw numeric text and canonical `Double` millimeters/counts per millimeter in `GaugeViewModel` and `SavedStateHandle`; `MeasurementCalculator` owns arithmetic and `GaugePresentation` owns localized output. Unit changes preserve canonical values, stitch and row densities retain independent manual/swatch provenance, and display rounding must never feed calculation. Calculator actions do not write Room, preferences, or notes or call network services; Copy writes plain text to the clipboard and regex paste retains its existing Pro gate.
- Notes editing is local-only; do not reintroduce cloud journal processing or cloud cleanup without a new explicit product/security decision
- PDF rendering lives in `data/storage/PdfPageRenderer`; pattern UI should not define renderer copies

## Navigation Rules

- Top-level tabs are `Projects`, `Library`, `Tools`, `Insights`, `Settings`
- `TopLevelDestination` in [Screen.kt](/home/emma/dev/KnitTools/app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt) is the source of truth
- `CounterViewModel` is shared at the Projects graph level
- `LibraryViewModel` is shared at the Library graph level
- Widget counter launches carry a `CounterLaunchRequest.requestId`; `MainActivity` clears consumed launch extras and saves the consumed id across recreation
- Widget counter launch ids must be issued by `data/storage/CounterLaunchTokenStore` and atomically consumed by `MainActivity`; unused ids expire after 24 hours, legacy untimestamped ids are rejected, untrusted counter extras are ignored, and OAuth callback intents must not trigger counter navigation or consume counter tokens
- Pattern viewer entry points require an attached PDF URI; Ravelry pattern links are metadata until a local PDF is attached
- Do not turn `Tools` back into a generic dashboard grid
- `Screen.Gauge` is the shared Measurements and Gauge screen for Tools and Projects; its optional `projectId` supplies display context only. The project entry belongs in `ProjectActionsBottomSheet`; preserve the five project content cards and existing Pattern viewer entries.
- Project list sort order is `domain/model/ProjectSortOrder`; DataStore persists its `persistedValue`, but UI/repository code should use the enum

## UI Rules

- All user-visible strings go in `res/values/strings.xml`
- Use theme tokens and `MaterialTheme.knitToolsColors`, not hardcoded colors
- Counter route first viewport is top bar plus row-counter hero plus bottom navigation; the row-counter hero may include stitch tracking because it is active counting input, while reminder cards, project content cards, and extra counters belong below the hero scroll
- Counter top bar owns back navigation, the uppercase project name, and overflow; do not restore pattern subtitles, PDF names, Ravelry names, or `Pattern attached` copy there
- `CounterProjectContentCards` is a fixed five-card square grid: Pattern, Yarn, Notes, Photos, Reminders. The first four cards form a two-column square grid and the Reminders tile is centered on its own row; cards contain only icon plus title, never previews, counts, chevrons, or reminder messages
- Counter-specific spacing, hero, progress, repeat pill, grid, icon, extra-counter card, and touch-target dimensions belong in `ui/theme/CounterDimens.kt`
- Scaffold background should use the app `background` color
- Reuse `ToolScreenScaffold` and shared UI components before adding feature-local scaffolds
- Avoid inline typography overrides except documented project exceptions

## Data And Build Rules

- Room changes must keep the migration chain and schema export coherent
- Do not bypass repositories from UI code just because a DAO is nearby
- Do not add back `org.jetbrains.kotlin.android`
- Do not reintroduce `android.disallowKotlinSourceSets`, `android.newDsl`, or `android.builtInKotlin` toggles unless absolutely necessary
- Release signing must stay environment-variable-driven
- Android Firebase config belongs in ignored `app/google-services.json` locally or is generated from `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64`; release artifact tasks materialize the ignored file from that env value when needed and still require the config, while debug artifact tasks may generate ignored `app/src/debug/google-services.json` placeholder config for local buildability; lint-only `lintRelease` may skip `processReleaseGoogleServices` when neither real config source exists, but this exception must never apply when a release artifact task is present; Google Services -pluginin generoimat resurssit ovat debug-Firebase-arvojen ainoa lähde, joten `google_app_id`-, `google_api_key`- tai `project_id`-arvoja ei saa ylläpitää erillisessä debug XML:ssä; Ravelry credentials must not be added to Android `BuildConfig`, `debug.credentials.properties`, `local.properties`, resources, or source code
- Debug-only Sentry DSN belongs in `KNITTOOLS_SENTRY_DSN`, `SENTRY_DSN`, or ignored `debug.credentials.properties` as `sentry.dsn`; do not hardcode or commit it
- Firebase and Google Services are allowed only for the Ravelry backend migration phases documented in `Ravelry Firebase Backend And Saved Patterns Plan.md`; do not add Firebase AI, ML Kit, Gemini, App Check, or model-backed parser dependencies as transitive convenience dependencies
- Functions runtime is `nodejs22`; `firebase-functions` and `firebase-admin` versions must satisfy npm peer dependencies instead of being forced with `--legacy-peer-deps`

## Security

- Keep `usesCleartextTraffic` disabled unless explicitly justified
- Ravelry Basic Auth credentials and OAuth client secret are removed from Android; new Ravelry work must keep secrets and token exchange server-side, and DeepSec accepted-risk handling must not expand beyond documented historical findings
- Exported components must stay intentional and minimal
- Treat extras on exported activities as untrusted unless they are explicitly validated against app-owned state
- Keep `FileProvider` usage least-privilege
- Do not log billing state, voice transcripts, Ravelry credentials, pattern text, or user project data
- Do not add release-path crash reporting, analytics, tracking, Sentry Gradle plugin uploads, source-context uploads, replay, tracing, or logcat breadcrumbs without a new explicit product/security decision

## Working Conventions

- Comments and commit messages should be in Finnish
- Prefer explicit imports
- Avoid wildcard imports
- Avoid `!!`
- Prefer minimal targeted edits over broad rewrites

## Verification

- Prefer the smallest useful check
- Project-local PowerShell wrappers are mostly short `tools/*.ps1` scripts; check wrappers delegate to `C:\Dev\Android-check\tools\InvokeProjectCheck.ps1`, `ad` delegates to `C:\Dev\Android-check\tools\InstallDebugToDevice.ps1`, and release-surface wrappers are repo-local.
- `lc` runs ktlint, detekt, and debug Android lint into `reports/ktlint.txt`, `reports/detekt.txt`, and `reports/lint.txt`; the shared checker parses fresh analyzer-owned reports separately, surfaces Detekt baseline entries and toolchain diagnostics, records the before/after Git and lint-input fingerprints in the run manifest plus `reports/input-state.txt`, and fails if lint inputs change during execution. `lc -Full` also runs release lint, while `lc -Fresh` adds `--rerun-tasks --no-build-cache` for an explicit uncached audit.
- `ad`, `ac`, `dc`, `ss`, `ds`, `ms`, `os`, `ql`, `db`, `pc`, `cs`, `cr`, `ga`, `sentry`, `rs`, `rst`, and `sc` are project-local wrappers; use `-PlanOnly` or `-ResolveOnly` for dry checks where supported
- `ad` builds `assembleDebug`, resolves `adb.exe` from `local.properties` `sdk.dir`, and installs `app/build/outputs/apk/debug/app-debug.apk` with `adb install -r`; use `ad -NoBuild` to install an already-built APK
- `pc` runs PMD CPD duplicate detection with KnitTools' default `PMD_CPD_MINIMUM_TOKENS=100`, `cr` runs compose-rules through ktlint/detekt, `ga` runs Android Lint with Google Android Security Lints, and `cs` is available for Compose Stability Analyzer projects.
- `sentry` verifies that debug includes `io.sentry`, release does not include `io.sentry`, and writes `reports/sentry.txt`
- `rs` delegates to `tools/release-surface.ps1`; `rst` delegates to `tools/release-surface-test.ps1`. These guard KnitTools-specific release and architecture boundaries and must stay repo-local.
- `sc` runs dependency, secret, and light Semgrep checks; `sc -Full` also runs the Android-specific `ac` path and DeepSec custom report
- `tools/sc.ps1` is the only security-check source of truth. Bash scripts under `scripts/` are compatibility delegates and must fail closed instead of implementing separate scanners.
- `app/stability/app-debug.stability` is the versioned Compose stability baseline; other variant dumps remain local and ignored.
- `tools/sonar.ps1 -PlanOnly` is read-only; an actual Sonar upload requires `-AllowExternalUpload` and runs through a bounded managed Gradle process.
- `tools/sonar.ps1` resolves shared checker modules from `ANDROID_CHECK_ROOT` when set and otherwise uses `C:\Dev\Android-check`; a missing module fails closed before analysis.
- Typical commands: `./gradlew assembleDebug`, `./gradlew test`, `./gradlew :app:detekt`, `./gradlew lint`
- Do not run the user's wrapper scripts such as `lc` or `sc`
- Never commit generated `reports/`
