# KnitTools

## Purpose and scope

This file is the detailed implementation reference for the current KnitTools checkout. It is intended for:

- code-review questions and architecture audits;
- UI and UX design work;
- feature and screen inventory;
- persistence, security, and lifecycle reviews;
- build, dependency, CI, and release-surface checks;
- locating the source of truth for a behavior before changing it.

The base snapshot was re-verified against the source tree on 2026-08-24. Project Yarn Usage and Remaining Allocated Yarn V1, Room 24, and the source inventory were updated on 2026-08-29 after full local JVM and Android verification, following the project-folder and Measurements and Gauge work. This file describes the current working tree, including uncommitted changes, rather than assuming that Git `HEAD` contains the newest implementation. Counts, dependency versions, workflow pins, and generated schema versions are volatile and must be rechecked when precision matters.

This is a reference, not a replacement for the code. If this file conflicts with executable source, Gradle configuration, the Android manifest, Room schema exports, Firebase configuration, or tests, the executable source wins.

## Source-of-truth order

Use this order when answering questions or reviewing a change:

1. Production source and resources under `app/src/main` and `functions/src`.
2. Build and platform configuration: Gradle files, version catalog, manifests, `firebase.json`, `firestore.rules`, and package manifests.
3. Exported Room schemas and registered migration code.
4. Focused tests that exercise or structurally verify the exact behavior.
5. Repository instructions in `AGENTS.md`, `CODEX.md`, and, for product wording or visual direction, `CLAUDE.md`.
6. This file.
7. Plans, progress documents, old reviews, scanner context, and `README.md`.

Tests are evidence only for what they actually assert and only when their execution result is known. A source-contract test can prove the presence or absence of a source pattern; it does not prove device rendering, gesture behavior, migration behavior on a real database, or a release artifact.

## Product snapshot

KnitTools is a local-first Android knitting and crochet companion. Its main product areas are:

- project creation, project lifecycle, and local folder organization;
- a primary row or round counter with history, targets, sections, stitch tracking, reminders, and additional counter types;
- timed work sessions and historical insights;
- local pattern PDF import, project attachment, reading-line calibration, layered annotations, and annotated export;
- saved-pattern metadata and Ravelry discovery through a Firebase backend;
- yarn inventory cards, project-only yarn notes, and project-specific planned, allocated, and used yarn amounts;
- progress photos;
- home-screen counter widgets;
- knitting and crochet calculators and reference tables;
- one-time Pro entitlement with a user-started 14-day trial.

The app does not currently implement cloud synchronization, continuous Drive or Dropbox synchronization, voice commands, microphone input, model-backed instruction parsing, AI generation, analytics, release crash reporting, or a server-side journal.

### Current platform and versions

| Area | Current value |
|---|---|
| Android application ID and namespace | `com.finnvek.knittools` |
| Gradle modules | `:app` and `:baselineprofile` |
| Backend workspace | `functions/`, a separate Firebase Functions TypeScript package |
| Android compile SDK | 37 |
| Android target SDK | 37 |
| Android minimum SDK | 29 |
| App version | `versionCode 1`, `versionName "1.0.0"` |
| Room schema | 24 |
| Java toolchain | Eclipse Temurin JDK 17 |
| Gradle wrapper | 9.6.1 |
| Android Gradle Plugin | 9.3.1 |
| Kotlin and Compose compiler plugin | 2.4.10 |
| Firebase Functions runtime | Node.js 22 |
| Production UI | Jetpack Compose with Material 3 |

### Current source inventory

These are orientation counts, not coverage or pass results:

- 319 production Kotlin files under `app/src/main`;
- 244 Kotlin files in the JVM test source set under `app/src/test`;
- 34 Kotlin files in the Android instrumented-test source set under `app/src/androidTest`;
- 7 TypeScript test files matching `*.test.ts` under `functions/src`;
- 65 tracked or working-tree resource files under `app/src/main/res`.

### Current local validation

Project Yarn Usage V1 final verification on 2026-08-28 and 2026-08-29 passed 1,452 debug JVM tests across 242 suites with `--rerun-tasks`, with no failures, errors, or skipped tests. Separate direct offline commands passed KSP, Android-test compilation, debug app and test APK assembly, `lintDebug`, `ktlintCheck`, and `detekt`. Debug Lint reported no issues. The debug Compose stability dump was inspected: the editor, field, derived summary, and usage row are stable and skippable/restartable; the flow retains runtime-checked list inputs, and no stability exemption was added.

The final identical APK pair passed the entire 170-test installed package on API 36 (`emulator-5554`) and API 37 (`emulator-5556`), with no failed or skipped tests and no app crash or ANR. This includes real schema 23 to 24 migration with all 16 existing tables preserved, the full 1 to 24 chain, 14 usage repository tests, 11 usage Compose tests, and two real `MainActivity` usage flows. The 11-test API 36 rerun at a system-level 320 dp width also passed; its production and Compose-test source stayed unchanged by the later native-test project-reopening correction. Visual checks covered all requested usage states, long names, IME, light/dark themes, and system-level 200 percent font. A real save/recreation race was fixed by making successful completion durable UI state and clearing the committed draft even when sheet hiding is cancelled. Earlier full runs exposed a native-test lifecycle assumption: completing the project while its counter stayed open could switch the existing active-project selection. The test now closes that activity, proves persisted usage across completion/reopening, and explicitly opens the same project again before verifying unlink and source deletion; application navigation behavior was not changed. The unchanged Gauge clipboard-confirmation and project-folder popup tests also failed intermittently in earlier full runs. The final API 36 package was rerun alone with the same APK pair after closing the API 37 emulator; those unrelated tests were not modified.

These checks do not replace a human TalkBack listening pass. Some Compose-clock screenshots transiently omitted the Save label; native MainActivity checks at 320 dp and 200 percent German font showed the label in both themes. API 29 was not run or downloaded. The optional all-variant `stabilityDump` was blocked by missing release Firebase configuration; the direct `debugStabilityDump` passed without configuration changes. No release artifact or external-service behavior is claimed. Functions and the user's custom check wrappers were not run.

Earlier feature verification:

Measurements and Gauge V1 finalization on 2026-08-28 passed 1,435 debug JVM tests across 239 suites with `--rerun-tasks` and no failures, errors, or skipped tests. The focused JVM run passed 146 tests across 18 classes. After the Compose stability correction and the direct UI-test synchronization correction, the final combined KSP, Android-test compilation, debug app and test APK assembly, baseline-profile assembly, `lintDebug`, `ktlintCheck`, and `detekt` run completed successfully (175 tasks, 7 minutes 51 seconds); Lint and Detekt reported zero issues. `GaugeScreen` uses a stable composable ViewModel provider and the shared lifecycle-aware event collector; the debug stability baseline was updated without adding a stability exemption.

The final focused API 37 run passed all 13 `GaugeScreenTest` tests. The complete installed package, including all three `GaugeNavigationRuntimeTest` tests, passed 142 tests on API 36 (272.854 seconds) and 142 tests on API 37 (288.956 seconds), with no failures or skipped tests. Both final runs used identical app and test APKs, had no active default network, and reported no crash or ANR. An earlier post-stability API 37 run passed 141 of 142 tests because an immediate Finnish copy-confirmation visibility assertion failed; that test now waits up to five seconds for visibility while retaining the display and clipboard assertions. The earlier separate system-level 320 dp viewport and 200 percent font-scale three-test rerun predates these corrections and was not repeated. API 29 runtime testing remains unavailable because its AVD and system image are not installed. These results do not replace a human TalkBack listening pass or prove any release artifact or external-service behavior.

Project-folder verification on 2026-08-28 passed 1,361 debug JVM tests across 234 suites, the complete 126-test installed instrumentation package on API 36 and API 37, KSP, Android-test compilation, debug app and test APK assembly, debug Lint, ktlint, Detekt, and `git diff --check`. No JVM or instrumented test failed or was skipped in the final runs; Lint reported no issues. The instrumented suite includes 31 new tests and covers schema 22 to 23, older migration entrypoints and the full 1 to 23 chain, Room constraints, metadata-only transactions, project creation, folder UI, state restoration, trusted widget navigation, and active-session preservation. Schema 23 adds only the two organization tables; all 14 schema 22 entities remain structurally unchanged.

The same final app APK was checked with normal and disabled system animations, light and dark themes, normal and 200 percent font scale, and a 320 dp viewport. The narrow-width reruns passed 19 component/screen tests and six dark screen tests. Real MainActivity checks confirmed that a selected folder survives background process death and task restoration, while a new task starts at All Projects. Folder strings and plural resources cover all 11 locale sets; automated semantics, focus, touch-target, keyboard, and screenshot checks do not replace a human TalkBack listening pass. All Gradle work was offline, and both emulators ran with restricted networking and no active default network. No Functions tests, external-service setup, release artifact, or deployment was part of the folder work. API 29 runtime testing remains unavailable locally because its system image is not installed.

The earlier local stabilization run on 2026-08-26 executed 1,298 debug JVM tests, 95 installed instrumented tests on API 36, 95 installed instrumented tests on API 37, and 46 Functions tests across seven suites, all without failures or skipped tests. That Android run also passed KSP, Android-test compilation, debug app and test APK assembly, focused Room migrations 18 to 19, 19 to 20, 20 to 21, 21 to 22, and 1 to 22, debug Lint, ktlint, Detekt, and `git diff --check`; Lint reported zero errors, 25 existing unused-resource warnings, and three plural suggestions. Both emulator smoke launches opened Projects, Library, Tools, Insights, and Settings without a fatal crash or ANR.

The source-file counts above are inventory; they are not executed-test counts. API 29 runtime testing and a human TalkBack listening pass were not performed. Firebase and Ravelry were not configured, contacted, or deployed, and local Functions tests do not prove production OAuth or live upstream behavior. The Functions package targets Node.js 22; the earlier stabilization run used local Node.js 24.19.0 and npm 11.17.0, which is a nonblocking environment mismatch rather than deployment evidence.

## Repository layout

| Path | Responsibility |
|---|---|
| `app/` | Main Android application, resources, Room schemas, JVM tests, instrumented tests, and Compose stability baseline |
| `baselineprofile/` | Macrobenchmark and baseline-profile producer module |
| `functions/` | Firebase Functions v2 backend for Ravelry OAuth and metadata callables |
| `config/` | Security decisions, future sync specification, scanner exceptions, and Ravelry backend progress context |
| `gradle/` | Version catalog, verification metadata, OSV configuration, and wrapper configuration |
| `tools/` | Project-local PowerShell entry points, release-surface checks, test helpers, and specialized validation |
| `scripts/` | Compatibility delegates; security scanning logic must remain in `tools/sc.ps1` |
| `.deepsec/` | DeepSec configuration, custom matchers, report processing, and matcher tests |
| `.github/workflows/` | Android build and CodeQL workflows |
| `.github/dependabot.yml` | Dependency update configuration for Gradle, Actions, DeepSec npm, and Functions npm |

### `:app`

The application module owns Android startup, Compose UI, navigation, ViewModels, domain logic, repositories, Room, DataStore, file storage, Firebase clients, billing, widgets, resources, tests, and release gates. It also owns:

- debug-only sample-data seeding and debug-only Sentry diagnostics;
- exported Room schemas under `app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/`;
- the versioned Compose stability baseline at `app/stability/app-debug.stability`.

The module applies the Android application, Compose, Kotlin serialization, KSP, Room, Hilt, Baseline Profile, ktlint, Detekt, OWASP dependency-check, Compose Stability Analyzer, and JaCoCo-related build surfaces. Google Services is conditionally integrated through the app build logic. Do not reintroduce the old `org.jetbrains.kotlin.android` plugin or obsolete Kotlin source-set toggles.

### `:baselineprofile`

The baseline-profile module targets `:app`, uses the Android test and Baseline Profile plugins, and shares SDK values from the version catalog. Its existence is not proof that the profile has been regenerated for this checkout. Baseline Profile/Benchmark `1.5.0-beta01` is a documented temporary AGP 9 compatibility exception.

### `functions`

The backend package is not a Gradle module. It uses TypeScript and Firebase Functions v2, targets Node.js 22, compiles to `functions/lib`, exposes authenticated callables and an OAuth callback, and stores OAuth state, tokens, and rate-limit windows in Firestore. It never downloads or stores pattern PDFs.

Current core versions are `firebase-functions 7.3.2`, `firebase-admin 14.2.0`, `typescript 7.0.2`, and `@types/node 26.1.2`. Package overrides pin security-sensitive transitive packages including `brace-expansion`, `body-parser`, `form-data`, `js-yaml`, `protobufjs`, `rimraf`, and `uuid`.

## Android dependency map

`gradle/libs.versions.toml` is the source of truth. Current notable versions are:

| Dependency family | Version |
|---|---|
| Hilt | 2.60.1 |
| AndroidX Hilt | 1.4.0 |
| Room | 2.8.4 |
| Compose BOM | 2026.06.01 |
| Navigation Compose | 2.9.8 |
| Lifecycle | 2.11.0 |
| Kotlin coroutines | 1.11.0 |
| DataStore | 1.2.1 |
| AndroidX Core | 1.19.0 |
| Activity Compose | 1.13.0 |
| AppCompat | 1.7.1 |
| Kotlin serialization | 1.11.0 |
| Splash Screen | 1.2.0 |
| Play Review | 2.0.2 |
| Play In-App Updates | 2.1.0 |
| Google Play Billing | 9.1.0 |
| Glance | 1.1.1 |
| Coil Compose and Ktor 3 network loader | 3.5.0 |
| Ktor | 3.5.1 |
| AndroidX Browser | 1.10.0 |
| WorkManager | 2.11.2 |
| Firebase BOM | 34.17.0 |
| Google Services plugin | 4.5.0 |
| Sentry Android Core | 8.43.1, debug only |
| ktlint Gradle plugin | 14.2.0 |
| Detekt | 2.0.0-alpha.5 |
| OWASP dependency-check | 13.0.0 |
| Compose Stability Analyzer | 0.12.0 |
| Sonar Gradle plugin | 7.3.1.8318 |

Detekt `2.0.0-alpha.5` and Baseline Profile/Benchmark `1.5.0-beta01` are intentional temporary compatibility exceptions. WorkManager is directly pinned because it is part of the Glance transitive surface; there is no production `Worker` implementation. Ktor and OkHttp are present, but the current Ravelry product path uses authenticated Firebase callables rather than direct Android-to-Ravelry requests. Coil declares `coil-network-ktor3` explicitly and reuses the existing Ktor 3 and OkHttp engine surface for HTTPS thumbnail loading; it does not introduce a second image-network stack.

## Application startup and process lifetime

### Application object

`app/src/main/java/com/finnvek/knittools/App.kt` is the Hilt application entry point. `App.onCreate`:

1. initializes the source-set-specific `SentryInit` implementation;
2. launches `PreferencesManager.applyStoredAppLanguage()` in the injected application coroutine scope;
3. schedules yarn-photo orphan pruning;
4. schedules stale pattern-capture pruning on the injected I/O dispatcher;
5. invokes the build-variant `DemoDataSeeder` facade;
6. initializes billing and Pro state;
7. waits for the entitlement state and refreshes widgets when widget access becomes known.

Startup locale reads must not use `runBlocking`. Long-lived application work uses the Hilt-owned `@ApplicationScope`. Blocking work in that scope must still move to the injected `@IoDispatcher`.

`DemoDataSeeder` is a build-variant facade. The debug implementation delegates orchestration to `data/local/DebugDemoDataSeeder`, performs one `DatabaseTransactionRunner` transaction, and reuses repository writers so project counters and yarn links obey normal invariants. The release implementation is a no-op.

### Main activity

`MainActivity` is the single-activity Compose host. It installs the splash screen before `super.onCreate`, uses an animated exit only when system animators are enabled, resolves the stored theme before composing edge-to-edge content, and installs `PreferenceAwareHapticFeedback` above navigation. It also owns:

- billing, Pro, in-app review, and in-app update startup;
- counter-launch, OAuth-callback, and shared-Ravelry-import intent routing;
- the passive one-time trial-ended notice;
- flexible-update completion UI;
- per-app locale synchronization on resume.

`onNewIntent` handles OAuth callbacks, text shares, and widget launches. OAuth and share intents are separate trust domains and must not consume counter-launch tokens or trigger counter navigation.

### Widget launch trust boundary

A widget launch is not trusted merely because it targets `MainActivity`:

- `CounterLaunchTokenStore` issues an app-owned request ID;
- `MainActivity` atomically consumes it;
- unused IDs expire after 24 hours;
- legacy untimestamped IDs are rejected;
- the store is capped at 100 pending IDs;
- the consumed ID is saved across recreation;
- consumed extras are cleared from the activity intent.

This prevents external or replayed extras from selecting an arbitrary project or repeatedly opening the counter.

## Architecture and package ownership

Production Kotlin lives under `app/src/main/java/com/finnvek/knittools`.

| Package | Ownership |
|---|---|
| `auth/` | Firebase anonymous-auth gateway and Ravelry authentication seams |
| `billing/` | Play Billing connection, product details, purchase, acknowledgement, and restore |
| `data/datastore/` | Preferences, language mirror, trial persistence, and launch-token storage |
| `data/local/` | Room database, entities, DAOs, migrations, transactions, debug seeding |
| `data/remote/` | Firebase callable client, sanitized Ravelry transport models, backend error mapping |
| `data/storage/` | App-owned files, SAF copy paths, PDF rendering/export, progress and yarn photos |
| `di/` | Hilt bindings, database construction, dispatchers, and application scope |
| `domain/calculator/` | Pure calculations, formatting, parsing, row mapping, annotation geometry |
| `domain/model/` | Domain models, enums, persisted-value parsing, and link normalization |
| `pro/` | Entitlement, trial state, feature gates, and Pro state composition |
| `repository/` | Storage/framework seam for UI consumers |
| `ui/components/` | Shared reusable Compose components |
| `ui/navigation/` | Routes, top-level destinations, graph wiring, and bottom navigation |
| `ui/screens/` | Screen composables and screen-specific ViewModels |
| `ui/theme/` | Color, typography, shape, layout, and screen-specific dimension tokens |
| `util/` | Locale-sensitive formatting and small utilities |
| `widget/` | Glance UI, widget state, actions, and launch behavior |

### Layer rules

- `data/` owns Room, DataStore, file storage, Android framework access, and transport details.
- `domain/` owns calculations and domain models.
- `repository/` is the storage/framework seam for UI consumers.
- `ui/` owns screens, navigation, visual state, and ViewModels.
- Composables must not call DAOs directly or own business transactions.
- Repositories and ViewModels crossing architecture boundaries use injected `@IoDispatcher` rather than hardcoded `Dispatchers.IO`.
- Multi-DAO Room writes use `DatabaseTransactionRunner` from repository methods.
- UI-facing Room flows apply `retryOnRepositoryReadFailure` after entity-to-domain mapping. It preserves cancellation and retries at 250, 500, 1000, 2000, and 4000 ms, capped at 5000 ms.
- Final persistence that must outlive a ViewModel may use `@ApplicationScope`; ordinary screen work stays in `viewModelScope`.

## Navigation

### Top-level destinations

`ui/navigation/Screen.kt` is authoritative. The visible bottom-navigation order is:

1. Projects, route `projects_tab`, start `project_list`.
2. Library, route `library_tab`, start `library`.
3. Tools, route `tools_tab`, start `tools`.
4. Insights, route `insights_tab`, start `insights`.
5. Settings, route `settings_tab`, start `settings`.

Top-level navigation saves and restores state and avoids duplicate destinations.

### Route inventory

| Route | Primary UI |
|---|---|
| `project_list` | Project list |
| `counter` | Selected project counter workspace |
| `photo_gallery` | Current project's progress photos |
| `pattern_viewer/{projectId}` | Attached project PDF viewer |
| `session_history/{projectId}` | Project session history |
| `notes_editor/{projectId}` | Project notes editor |
| `library` | Library hub |
| `saved_patterns` | Saved-pattern list |
| `saved_pattern_detail/{savedPatternId}` | Saved-pattern metadata and actions |
| `library_pattern_viewer/{savedPatternId}` | Library PDF viewer |
| `my_yarn` | Yarn inventory |
| `yarn_card_detail/{cardId}` | Yarn-card detail |
| `all_photos` | Cross-project progress-photo library |
| `library_ravelry_detail/{patternId}` | Ravelry metadata from Library |
| `tools` | Tools landing screen |
| `gauge?projectId={projectId}` | Measurements and Gauge; optional project context, with bare `gauge` still valid |
| `increase_decrease` | Increase/decrease calculator |
| `cast_on` | Cast-on calculator |
| `yarn` | Yarn estimator |
| `needles` | Needle-size reference |
| `size_charts` | Size charts |
| `abbreviations?craftType={craftType}` | Abbreviation reference |
| `chart_symbols` | Chart-symbol reference |
| `ravelry` | Ravelry account and search |
| `ravelry_import/{importUrl}` | Shared Ravelry URL confirmation/import |
| `ravelry_detail/{patternId}` | Ravelry result detail |
| `insights` | Insights dashboard |
| `settings` | Settings |
| `pro_upgrade` | Global Pro upgrade |

`RavelryImport.createRoute` URI-encodes the URL. A raw URL must never be concatenated into a route segment.

### Graph ownership and back behavior

- `CounterViewModel` is shared at the Projects graph level.
- `LibraryViewModel` is shared at the Library graph level.
- Yarn-card detail resolves its ViewModel from the Library parent entry.
- `session_history/{projectId}` is registered from Projects and Insights so it returns to the correct context.
- `gauge?projectId={projectId}` is registered from Tools and Projects, with a `GaugeViewModel` owned by each route entry. The optional project ID supplies display context only; invalid, unavailable, or absent context does not disable the calculator.
- Invalid required arguments use `RouteArgumentFallback` and return to the owning top-level destination.
- Global `pro_upgrade` is outside the individual top-level graphs.
- `KnitToolsNavActions`, `CounterScreenActions`, and `RavelrySearchActions` group route actions.

The bottom bar is hidden only for `pro_upgrade`, both pattern viewer routes, and `notes_editor/{projectId}`. It remains visible on the counter and most detail screens. `NavHost` consumes outer scaffold padding; nested scaffolds must not add duplicate insets.

## Screen and feature inventory

### Projects

The Projects graph contains project creation and lifecycle, local folder organization, sorting, completed-project visibility, selection and bulk actions, the counter workspace, extra counters and repeat sections, reminders, notes, yarn, progress photos, attached pattern PDFs, project annotation layers, and session history.

The compact folder selector opens a scrollable sheet with the virtual All Projects and Unfiled views plus user-created folders. Folder names are trimmed, limited to 50 Kotlin `String.length` units, and reject controls and line separators; Java NFC followed by `Locale.ROOT` lowercasing supplies the unique canonical name while the display form is preserved. Folder actions create, rename, move earlier/later, and delete with an active-plus-completed project count. Empty folders persist; deleting a folder preserves its projects as Unfiled.

`ProjectListViewModel` owns the typed filter in `SavedStateHandle`, not DataStore. New tasks begin at All Projects; navigation and recreation restore the selection, and a missing folder falls back only after a real Room result. Active and visible completed projects are filtered after the existing SQL sort. The Continue hero comes from the first filtered active project with a positive count, and its row returns during selection. Completed visibility remains global. Distinct empty states explain an empty folder, empty Unfiled, or hidden completed projects.

Single-project moves use the existing Counter actions sheet; bulk moves use visible active and completed project IDs. Destinations are Unfiled and real folders, never All Projects. The operation preserves project contents, timestamps, files, and active-session state. Folder organization is free and does not filter Library, Insights, history, widgets, or project-ID navigation. Creation from a selected folder passes its ID to the canonical repository transaction; All Projects and Unfiled create an unfiled project, and the existing Pro project limit still applies.

### Library

Library contains Saved Patterns, My Yarn, All Photos, needle sizes, size charts, knitting or crochet abbreviation entry, chart symbols, saved-pattern detail, library PDF reading and master annotations, and yarn-card detail.

### Tools

Tools is a focused list, not a generic dashboard grid. It links to Measurements and Gauge, increase/decrease distribution, cast-on calculation, yarn estimation, and Ravelry discovery. Reference tables remain under Library navigation.

### Insights

Insights combines project and session data into total work time, exact rows and pace, active days, streak, trend, per-project time, a time-series chart, a 26-week project fabric, and project-scoped history navigation.

### Settings

Settings owns app language, light/dark/system theme, haptic feedback, keep-screen-awake and imperial-unit preferences, Pro status and upgrade/restore entry, the help-guide link, privacy summary, and app version.

### Screen source index

| Product surface | Primary source files |
|---|---|
| Project list | `ui/screens/project/ProjectListScreen.kt`, `ProjectListViewModel.kt`, `ui/components/ProjectListItem.kt` |
| Project folders | `ui/screens/project/ProjectFolderComponents.kt`, `ProjectFoldersState.kt`, `MoveProjectToFolderSheet.kt`, `ProjectFolderMoveViewModel.kt`, `repository/ProjectFolderRepository.kt` |
| Counter workspace | `ui/screens/counter/CounterScreen.kt`, `CounterViewModel.kt`, `CounterWorkspaceSections.kt`, `CounterProjectContentCards.kt` |
| Additional counters | `ui/screens/counter/MultiCounterComponents.kt`, `CounterUiStateReducers.kt` |
| Reminders | `ui/screens/counter/ReminderComponents.kt` |
| Project actions | `ui/screens/counter/ProjectActionsBottomSheet.kt` |
| Project yarn | `ui/screens/counter/ProjectYarnUsageFlow.kt`, `YarnManagementSheet.kt`, and `ProjectYarnUsageSheet.kt` |
| Progress photos | `ui/screens/counter/PhotoGalleryScreen.kt`, `PhotoComponents.kt` |
| Notes | `ui/screens/notes/NotesEditorScreen.kt`, `NotesEditorViewModel.kt` |
| Pattern viewer | `ui/screens/pattern/PatternViewerScreen.kt`, `PatternDocumentViewport.kt` |
| Pattern annotations | `PatternAnnotationViewModel.kt`, `PatternAnnotationToolbar.kt`, `PatternAnnotationOverlay.kt`, `PatternAnnotationLayerPanel.kt` |
| Library hub | `ui/screens/library/LibraryScreen.kt`, `LibraryViewModel.kt`, `LibraryTopBar.kt` |
| Saved patterns | `SavedPatternsScreen.kt`, `SavedPatternDetailScreen.kt`, `ui/screens/pattern/PatternPickerSheet.kt`, `PatternImageImportViewModel.kt`, `PatternImageImportSurface.kt` |
| Yarn inventory | `ui/screens/library/MyYarnScreen.kt`, `YarnStatusSheet.kt` |
| Yarn detail | `ui/screens/yarncard/YarnCardDetailScreen.kt` and its ViewModel |
| All photos | `ui/screens/library/AllPhotosScreen.kt` |
| Ravelry | `ui/screens/ravelry/RavelrySearchScreen.kt`, `RavelryDetailScreen.kt`, `RavelryViewModel.kt` |
| Shared URL import | `ui/screens/ravelry/RavelryImportConfirmationSheet.kt` |
| Insights | `ui/screens/insights/InsightsScreen.kt`, `InsightsViewModel.kt`, `InsightsSections.kt` |
| Insights chart/fabric | `InsightsChart.kt`, `InsightsChartModel.kt`, `InsightsProjectFabric.kt`, `InsightsProjectFabricModel.kt` |
| Session history | `ui/screens/session/SessionHistoryScreen.kt`, `SessionHistoryViewModel.kt` |
| Tools landing | `ui/screens/home/HomeScreen.kt` |
| Calculators | `GaugeScreen.kt`, `IncreaseDecreaseScreen.kt`, `CastOnScreen.kt`, `YarnEstimatorScreen.kt` |
| References | `NeedleSizeScreen.kt`, `SizeChartScreen.kt`, `AbbreviationsScreen.kt`, `ChartSymbolScreen.kt` |
| Settings | `ui/screens/settings/SettingsScreen.kt`, `SettingsViewModel.kt` |
| Pro | `ui/screens/pro/ProUpgradeScreen.kt`, `ProUpgradeViewModel.kt`, `ui/components/ProPromptSheet.kt` |

## Persistence

### Room database

`KnitToolsDatabase` uses schema version 24. Its 17 entities are `CounterProjectEntity`, `CounterHistoryEntity`, `YarnCardEntity`, `SessionEntity`, `ActiveSessionEntity`, `RowReminderEntity`, `ProgressPhotoEntity`, `ProjectCounterEntity`, `ProjectYarnNoteEntity`, `ProjectYarnUsageEntity`, `SavedPatternEntity`, `PatternAnnotationLayerEntity`, `PatternAnnotationEntity`, `PatternBookmarkEntity`, `ProjectDocumentEntity`, `ProjectFolderEntity`, and `ProjectFolderAssignmentEntity`.

Automatic migrations cover 1 to 2 and 2 to 3. Manual migrations cover every step from 3 to 4 through 23 to 24. `DatabaseModule` registers `ALL_MANUAL_MIGRATIONS`. Exported schemas 1 through 24 are retained.

#### Schema 18

Migration 17 to 18 adds non-null boolean/integer columns `secondaryCounterUsed` and `notesCreated` to `counter_projects` with default 0, then backfills both to 1 for legacy rows. New projects start false. These flags are monotonic so previously created compact-counter and note content remains usable after Pro access ends.

#### Schema 19

Migration 18 to 19 replaces the lossy saved-pattern `isFree` boolean with the stable `availability` values `free`, `paid`, and `unknown`. Legacy true maps to `free`; legacy false maps conservatively to `unknown` because the old value could not distinguish paid from unknown. The migration temporarily backs up saved-pattern-owned annotation layers and their annotations before rebuilding `saved_patterns`, then restores them in foreign-key order. Pattern IDs, metadata, project soft links, project-owned layers, indexes, triggers, and annotations remain intact.

#### Schema 20

Migration 19 to 20 adds `readingLineFollowCurrentRow` (default 1), `verticalReadingGuideEnabled` (default 0), and `verticalReadingGuideXFraction` (default 0.5) to `counter_projects`. It also creates project-owned `pattern_bookmarks` with active-document `documentKey`, name, zero-based page, normalized Y, creation time, a non-unique deterministic-order index, and a cascading project foreign key. Saved-pattern metadata, project links, gallery-import PDF state, row mappings, annotation layers, annotations, and annotation triggers remain intact.

#### Schema 21

Migration 20 to 21 creates `active_sessions`, the canonical source for the one globally active work session. A fixed primary key plus insert and update triggers enforce the singleton; the indexed project foreign key cascades on deletion. The migration adds no active row, so every schema 20 project, completed session, pattern, annotation, bookmark, and trigger remains unchanged. Exported schema 21 records the table, index, foreign key, and constraints.

#### Schema 22

Migration 21 to 22 creates `project_documents` without rebuilding `counter_projects`. Every project with a nonblank legacy `patternUri` receives exactly one primary relation in deterministic order. The migration preserves its readable URI, strongest available label, nullable valid Saved Pattern link, stable existing project or Saved Pattern `documentKey`, current page, row mapping, horizontal line and follow state, and vertical guide state. Projects without a readable legacy URI receive no row. The retained legacy project-pattern and reader columns remain compatibility data and are no longer the production source of truth.

The table uses a cascading project foreign key and a nullable `ON DELETE SET NULL` Saved Pattern foreign key. Database triggers reject a second primary row for the same project on insert or update. Repository transactions preserve exactly one primary document for every nonempty list during add, reorder, primary change, and removal.

#### Schema 23 and project folders

Migration 22 to 23 adds only `project_folders` and `project_folder_assignments`. All 14 previous tables, their rows, indexes, foreign keys, and triggers remain unchanged; `counter_projects` is not rebuilt. Both new tables start empty, so every migrated project is Unfiled.

`project_folders` stores `id`, display `name`, unique `normalizedName`, and manual `sortOrder`; ordering is always `sortOrder` then ID. `project_folder_assignments` has `projectId` as its primary key, one indexed `folderId`, and cascading foreign keys to the project and folder. A missing assignment means Unfiled. Virtual views have neither database rows nor sentinel IDs. Completing or reopening a project retains its assignment; project deletion removes only that assignment, not the folder.

`ProjectFolderRepository` owns metadata-only transactional writes and typed results, preserves cancellation, and observes one coherent joined organization snapshot through the repository retry boundary. Bulk moves deduplicate IDs and validate every project and destination before any write. Deleting a folder never calls file cleanup. `CounterRepository.createProject` validates an optional folder and inserts the new project plus assignment in the same limit-checked transaction; a stale folder cannot leave an unintended unfiled project. The folder DAO is supplied by `ProjectFolderDatabaseModule` beside the existing database module.

#### Schema 24 and project yarn usage

Migration 23 to 24 adds only `project_yarn_usage` and its four indexes. All 16 schema 23 entities remain unchanged; the migration does not rebuild existing tables or infer usage from notes, cards, quantities, or free text. The new table starts empty. Instrumented migration coverage compares every existing table's rows, schema, indexes, foreign keys, and triggers before/after migration and after usage create/update/delete, with both normal and recovery-required active sessions.

`project_yarn_usage` stores generated `id`, `projectId`, nullable `yarnCardId` and `projectYarnNoteId`, required `sourceNameSnapshot`, nullable `plannedMeters`, `allocatedMeters`, `usedMeters`, `metersPerSkein`, and `gramsPerSkein`, plus `createdAt` and `updatedAt`. The project foreign key cascades on deletion; both source foreign keys use `SET NULL`. Unique indexes on `(projectId, yarnCardId)` and `(projectId, projectYarnNoteId)` prevent duplicate non-null sources, while individual source indexes support foreign-key lookups. Creation requires an existing project-owned source; both IDs may become null after source deletion.

`ProjectYarnUsageRepository.observeForProject` maps one transactional Room relation snapshot through `retryOnRepositoryReadFailure` on the injected `@IoDispatcher`. Its `create`, `update`, and `delete` APIs validate ownership, finite nonnegative amounts, optional positive conversion pairs, and expected update revisions inside `DatabaseTransactionRunner`. Results distinguish success, existing usage, missing/foreign sources, invalid input, stale actions, and persistence failure; cancellation propagates and failed transactions roll back. UI never calls the usage DAO. Usage writes do not change project counts/timestamps, sessions, pattern documents, files, or global stash quantity.

#### Counter projects

`counter_projects` stores identity and name; primary count and step; craft type; main label type and optional custom label; horizontal reading-line visibility/Y/follow state; vertical-guide visibility/X; legacy `secondaryCount` and `secondaryCounterUsed`; notes and `notesCreated`; timestamps; optional section and stitch count; completion state, target, total rows and completion time; yarn-card ID CSV; saved-pattern link; attached PDF URI/name/page/row mapping; and stitch-tracking state.

Legacy projects default to knitting plus rows. Crochet projects default to rounds. Custom labels are trimmed and length-limited centrally before persistence and display.

#### Project documents and primary pattern

`project_documents` is the canonical list of readable PDFs attached to a project. Each relation has its own label, URI, deterministic sort order, primary flag, stable `documentKey`, optional Saved Pattern relation, current page, row mapping, horizontal reading line and follow state, and vertical guide state. `projectDocumentId` identifies selection and ordering; `documentKey` continues to isolate annotations and bookmarks. Labels are trimmed, nonblank, and limited centrally to 50 characters. Duplicate labels are allowed.

The first document becomes primary. Later documents can be opened, renamed, moved earlier or later, made primary, or removed from the Documents sheet. Removing the primary selects the earliest remaining relation as the new primary; removing the final relation closes the project viewer safely. Missing or unreadable files remain visible as unavailable metadata and can be removed without discarding their row first. Saved Pattern metadata without a local readable PDF stays in Library and is not a project document.

Local SAF PDF selection, gallery-image PDF creation, camera-image PDF creation, and Saved Patterns with attached local PDFs all converge on `ProjectDocumentRepository`. Multiple-document management has no new Pro gate; existing source-specific capture/import gates remain unchanged. Destructive repository operations capture app-owned cleanup inputs before their transaction, commit the authoritative database mutation first, and then attempt physical cleanup. Database failure or pre-commit cancellation leaves files intact; post-commit cleanup failure may leave an orphan but cannot roll back or misreport the committed database result. File deletion remains reference-aware across Saved Patterns, every project-document row, other projects, and any still-relevant legacy URI. Deleting a Saved Pattern clears only its nullable relation and retains the project document and shared file; every project-deletion route delegates distinct pattern URIs to the canonical reference-aware cleanup gate after its cascade commits.

The project list and counter surface primary-document status from one repository-owned bulk observation. The viewer remembers an explicitly selected relation through recreation, falls back to the primary or first available relation when needed, and keeps reader state per relation. All document-management copy is localized in the 11 supported resource directories, and row actions expose 48 dp targets with explicit accessibility semantics.

#### History and sessions

`counter_history` stores project, action, previous value, new value, and timestamp.

`sessions` stores project, start/end timestamps and rows, display minutes, exact `durationSeconds` and `rowsWorked`, and nullable `zoneId`. New sessions capture the device zone at session start. Cross-midnight day and pace splitting use that zone. Only a legacy null or invalid zone uses the current device zone as fallback.

`active_sessions` is the canonical source for the one globally active work session. Its fixed primary key and database triggers enforce the singleton, while its indexed project foreign key cascades on deletion. It stores stable session and recovery-interval tokens; original wall-clock time, zone, and start row; latest, trusted, reviewed, and pending row boundaries; trusted, pending, reviewed, and unreviewed net-row progress; checkpointed and reviewed duration baselines; wall-clock, `elapsedRealtime`, and boot-count anchors; and recovery prompt state. Explicit Start creates the row. Stop presents a summary before Save or Discard; Save inserts at most one coherent completed `sessions` row and deletes the active row in one transaction. Navigation, history, app backgrounding, and ViewModel clearing do not finalize it. Counter changes from the app or widget checkpoint the active row in the same transaction as the project count.

Same-boot recovery uses monotonic elapsed time and ignores wall-clock and time-zone changes. Reboot, missing boot identity, malformed anchors, or an unreviewed 24-hour interval creates a stable review interval instead of capping or guessing. Add accepts the suggested interval and pending rows, resets trusted anchors, and continues. Discard excludes pending time and rows, saves only already trusted work at most once, and stops. Edit accepts a validated total duration, combines it deterministically with the current row state, and stops once. Dismissal hides the dialog for that interval without resolving it; the passive review state remains visible.

Project switching and history do not split sessions. Starting another project is blocked until the active session is resolved. Project completion requires Save, Discard, or Cancel when its session is active; direct and bulk deletion discard the active row transactionally before deleting the project. Active rows are free functionality and are excluded from Session History and Insights until finalized. No foreground service, notification, WorkManager, AlarmManager, wake lock, or other background execution keeps the timer alive.

Work-session UI copy is localized in all 11 supported resource directories: default English, Finnish, Swedish, Norwegian Bokmål, Danish, German, Spanish, French, Italian, Dutch, and Portuguese. The compact row and dialogs use 48 dp actions, explicit text semantics, a focused recovery heading, a validated hours/minutes editor, and responsive narrow-width and 200 percent font layouts in both light and dark themes.

#### Yarn and project yarn notes

`yarn_cards` stores brand, name, fiber, weight, length, needle and gauge data, color, dye lot, category, care-symbol bitset, photo URI, creation time, stash quantity, status, and optional `linkedProjectId`.

`project_yarn_notes` stores project-owned name, description, quantity, notes, optional `savedYarnCardId`, and timestamps. A project yarn note survives conversion into an inventory card.

#### Saved patterns

`saved_patterns` stores source, nullable positive Ravelry ID, metadata, the canonical `free`/`paid`/`unknown` availability string, original and canonical URLs, optional local PDF URI, offline state, and save/update/sync timestamps. Persisted `ravelryId = 0` and `patternUrl` sentinels are not part of the current schema. Duplicate detection checks Ravelry ID, canonical URL, normalized original URL, then title plus designer only when explicitly requested.

#### Reminders, photos, and additional counters

- `row_reminders` stores `id`, `projectId`, `targetRow`, optional `repeatInterval`, `message`, `isCompleted`, and `createdAt`.
- `progress_photos` stores `id`, `projectId`, app-owned `photoUri`, `rowNumber`, optional `note`, and `createdAt`.
- `project_counters` stores `id`, `projectId`, `name`, `count`, `stepSize`, optional `repeatAt`, `sortOrder`, `createdAt`, `counterType`, shaping fields `startingStitches`/`stitchChange`/`shapeEveryN`, repeat-section fields `repeatStartRow`/`repeatEndRow`/`totalRepeats`/`currentRepeat`, and `linkedToMainCounter`.

`project_counters` is not the legacy `secondaryCount` store. Migrations must not duplicate that value. Old generated `Pattern repeat` backfill copies with the legacy signature are filtered at the counter UI boundary.

#### Annotation storage

`pattern_annotation_layers` stores exactly one nullable owner, `projectId` or `savedPatternId`, plus `documentKey`, active state, and timestamps. Database triggers enforce valid ownership and active project-layer uniqueness. `pattern_annotations` stores layer, page, kind, payload version, JSON payload, z-order, and timestamps.

#### Pattern bookmarks

`pattern_bookmarks` belongs to a project and the project layer's active `documentKey`; it stores a trimmed name of at most 50 characters, page, normalized Y, and creation time. Duplicate names and locations are allowed. Queries order rows by page, Y, creation time, and ID. Detach preserves rows, same-document reattach restores them, document replacement isolates them, and deleting the project cascades them. Saved-pattern deletion does not delete project bookmarks while the project document remains.

Hard child relationships cascade for history, sessions, reminders, photos, project counters, project yarn notes, project yarn usage, pattern bookmarks, annotation layers, and annotations. Intentional soft links are `yarn_cards.linkedProjectId`, `counter_projects.yarnCardIds`, `counter_projects.linkedPatternId`, and `project_yarn_notes.savedYarnCardId`; repositories maintain their invariants.

Schema 15 adds foreign-key lookup indexes, schema 16 adds session zones, schema 17 adds annotations, schema 18 adds monotonic feature-use flags, schema 19 preserves three-state saved-pattern availability, schema 20 adds project bookmarks plus followed horizontal and durable vertical reading-guide state, schema 21 adds the Room-owned active work-session singleton, schema 22 adds canonical multiple project documents and per-document reader state, schema 23 adds project-folder metadata and assignments, and schema 24 adds project yarn usage.

### DataStore and preferences

`PreferencesManager` uses a corruption handler that replaces unreadable preferences with an empty set. `AppPreferences` currently exposes:

| Preference | Default |
|---|---|
| `themeMode` | `LIGHT` |
| `appLanguage` | `SYSTEM` |
| `hapticFeedback` | true |
| `keepScreenAwake` | false |
| `useImperial` | false |
| `showCompletedProjects` | false |
| `projectSortOrder` | `ProjectSortOrder.DEFAULT` |

DataStore also persists dismissed tooltip IDs. `ProjectSortOrder.persistedValue` is the storage contract; UI and repositories use the enum rather than raw strings.

`ProjectSortOrder` values are `UPDATED` (`updated`), `NAME` (`name`), and `CREATED` (`created`). The default is `UPDATED`.

Runtime language is owned by AppCompat and Android per-app locale APIs. `app_language` is a persistence and migration mirror. Android 13 and newer migration tracks `app_language_migrated_to_system`, preserves an existing app-selected locale, and later synchronizes external system-setting changes back to DataStore.

Trial state and launch-token state have their own persistence helpers. Trial integrity must not be inferred from general app preferences alone.

## Domain behavior and transactional invariants

### Main counter

All primary counter changes go through `CounterRepository.applyMainCounterChange`. One repository transaction:

- reads the current project row;
- applies the requested delta using current state;
- writes the new primary count;
- appends history;
- resets or updates current-stitch state as required;
- applies linked additional-counter deltas according to domain rules;
- updates project timestamps.

The widget delegates through `applyWidgetCountChange` to the same semantics. Widget changes therefore affect `linkedToMainCounter` counters exactly as in-app changes do.

`CounterValueFormatter` shapes counter values for the UI. Compose maps its output slots to localized strings for the hero label, target status, button content descriptions, and project-list count text. UI code must not recreate count/target formatting independently.

### Craft type and main label

`CraftType` is `KNITTING` or `CROCHET`. `MainCounterLabelType` is `ROWS`, `ROUNDS`, `REPEATS`, or `CUSTOM`. Custom labels are trimmed and capped at 32 characters. An empty custom label falls back to the craft's default label.

### Additional counters

`ProjectCounterType` values are:

- `COUNT_UP`;
- `REPEATING`;
- `SHAPING`;
- `REPEAT_SECTION`.

`ProjectCounterLogic` owns type-specific validation and updates. Repositories apply those rules inside a transaction rather than embedding behavior in DAO SQL or composables.

`linkedToMainCounter` is chosen in the add/edit draft. A repeat-section counter must not be linked to the main counter because its progress already derives from main-counter rows. Named counters, shaping events, targets, and repeat-section progress remain separate from legacy `secondaryCount`.

### Notes

Project note replacement uses `CounterRepository.saveProjectNotes`. It merges the editor's base notes with current persisted notes so concurrent editor flows are preserved instead of blindly overwriting one another.

`notesCreated` is set monotonically when note content is created. After entitlement loss, a project that previously used notes retains access to that existing surface; a never-used project still follows the creation gate.

### Yarn links

`YarnCardLinks` owns CSV parsing and formatting for `counter_projects.yarnCardIds`. Callers must not split or join the field ad hoc.

`YarnCardRepository` is the canonical writer:

- `saveCard` normalizes any persisted `linkedProjectId`;
- `updateLinkedProjectId` updates both the card's `linkedProjectId` and the project's `yarnCardIds`;
- unlink, relink, and project deletion preserve both directions;
- `ProjectYarnNoteRepository.saveToMyYarn` creates or reuses a linked inventory card while retaining the project note, setting `savedYarnCardId`, and binding any existing usage row in one repository transaction. It never creates usage automatically or duplicates the note/card pair's existing usage, amounts, or snapshot.

### Pattern attachment

Pattern database state goes through `CounterRepository.attachPattern` and `detachPattern` so project fields, saved-pattern rows, annotation-layer activation, and related database state remain atomic. File deletion is a separate ownership decision:

- detaching does not delete reusable saved-pattern metadata;
- `SavedPatternRepository.deleteLocalPatternFileIfUnused` is the cleanup gate after saved-pattern deletion, detach, or project deletion;
- a local PDF is deleted only when no remaining database reference uses it.

### Project creation and deletion

The free-project limit is enforced transactionally in the repository. The repository counts all project rows, not only active projects, before creating another project without unlimited-project access. UI prompts are explanatory; they are not the persistence gate.

Project deletion coordinates child database cascades, soft-link cleanup, app-owned pattern files, progress photos, and yarn links. Every route gathers minimal cleanup inputs first, commits its database transaction without filesystem calls, and performs best-effort cleanup afterward in a non-cancellable post-commit boundary. Database failure or cancellation before commit leaves files intact. A cleanup failure after commit may leave an orphan, but the committed deletion remains authoritative and successful; shared PDFs remain protected by `SavedPatternRepository.deleteLocalPatternFileIfUnused`.

## Local files, URIs, and Storage Access Framework

### Ownership map

| Storage path | Owner and purpose | FileProvider exposed |
|---|---|---|
| `pattern_pdfs/<projectId>/` | App-owned imported project PDF documents | No |
| `pattern_captures/<projectId>/<sessionId>/` | Temporary app-owned camera or gallery images used to create a pattern PDF | Yes, narrowly for camera capture targets only |
| `progress_photos/<projectId>/` | App-owned progress-photo files | Yes, narrowly |
| `yarn_photos/<cardId>/` | App-owned yarn-card photos | No |
| cache `pattern_exports/` | Temporary annotated-PDF output | No |

`AppFileStorage` centralizes FileProvider authority and share-URI creation. `file_paths.xml` exposes only progress-photo and pattern-capture roots. Legacy `patterns/...` and `yarn_photos/...` URIs can be resolved internally for cleanup/read compatibility but are not broad share roots.

### Pattern PDF import

PDF import uses Android Storage Access Framework `OpenDocument(application/pdf)` and persistable URI permissions. The app copies the selected document into app-owned storage under the project. The Drive or Dropbox wording refers only to choosing a document through a provider exposed by the system picker.

There are no Drive or Dropbox SDKs, OAuth flows, provider-specific dependencies, or continuous sync. Future sync constraints are documented separately in `config/future-sync-spec.md` and are not implemented product behavior.

Gallery image import uses the system Photo Picker without broad media permissions. Every accepted image is copied during the picker-result handling into a collision-safe app-owned session before preview, so later reorder, removal, conversion, and restore logic do not depend on a temporary external URI grant. Exact duplicate URIs in one pending import keep their first position.

`PatternImageImportViewModel` owns pending session state, order, cancellation, progress, replacement confirmation, and repository attachment. `PatternDocumentStorage` validates device-supported still images, rejects animated input, decodes with a bounded 1800-pixel long edge, draws transparent pixels onto white, and writes one ordered PDF page at a time. The enforced limits are 20 pages, 25 MiB per image, 200 MiB total staged input, and a 32 MiB free-space reserve. Conversion publishes a unique app-owned PDF only after all pages succeed; failed or cancelled sessions remove uncommitted output and temporary images.

### PDF rendering

`PdfPageRenderer` is the single storage-layer renderer. Pattern UI must not create another `PdfRenderer` implementation. Render dimensions are bounded; current page rendering caps either bitmap dimension at 4096 pixels. Renderer ownership, file descriptors, pages, and bitmaps must be closed deterministically.

### Pattern camera capture

Pattern camera capture is a photo-to-PDF workflow:

- user-facing text uses photo and PDF wording, not scan/scanner claims;
- capture targets live in the same `pattern_captures/<projectId>/<sessionId>` staging layout as gallery imports;
- a successful capture enters the same preview, reorder, conversion, attachment, and cleanup pipeline as a gallery image;
- stale and abandoned capture files are pruned through repository/storage lifecycle code;
- camera availability is optional because the manifest declares the hardware feature as not required.

The internal Pro enum name `PATTERN_CAMERA_SCAN` is a legacy identifier, not user-facing product terminology.

### Progress photos

Capture-target creation and abandoned-capture cleanup go through `ProgressPhotoRepository` and `CounterViewModel` on `@IoDispatcher`. Composables do not instantiate `ProgressPhotoStorage` or directly delete files. Gallery and all-photos views consume repository data.

### Yarn photos

`YarnCardRepository.updatePhotoUri`:

1. copies the selected image into `yarn_photos/<cardId>`;
2. removes the new copy if persistence fails;
3. persists the new URI;
4. deletes the old app-owned photo only after the new URI is durable.

`App.onCreate` schedules `pruneUnreferencedPhotoFiles` so an interrupted replacement leaves at most a restart-cleanable orphan.

## Pattern reading, calibration, annotations, and export

### Reading line and row mapping

Attached-project reading-line state persists on the selected `ProjectDocument`:

- `readingLineEnabled`;
- `readingLineYFraction`;
- `currentPatternPage`;
- `patternRowMapping`.

`RowMappingParser` owns serialization of `RowMarker(row, page, yPosition)` values. A drag commit creates or updates the selected document's current row/page anchor through `CounterViewModel.upsertPatternRowMarker`. Calibration combines anchors through `mergePatternRowMarkers`. Live drag remains preview state in the viewer until commit. The project count remains global, but canonical counter and widget changes resolve followed reading-line movement only against the active document and never update the retained legacy project-level reader columns.

`resolveReadingLineYFraction` resolves movement in this order:

1. an exact row anchor;
2. interpolation between two anchors on the page;
3. one-sided row-step fallback;
4. ordinary row-step movement when no usable anchors exist.

Anchors from another PDF page must not influence the current page.

Library-only viewer reading-line state is saveable for session and configuration recreation, but it does not create a Room persistence path. Project-attached viewer state is durable.

### Annotation ownership

Annotation kinds currently include `FREEHAND`, `HIGHLIGHTER`, `LINE`, `ARROW`, `RECTANGLE`, `ELLIPSE`, `TEXT_BOX`, `CALLOUT`, `CHART_REGION`, and `CHART_TRACKER`.

- A saved pattern owns a reusable master layer.
- The master layer is editable in the library viewer.
- A project viewer renders that master layer read-only.
- Each project owns its own editable overlay for the same `documentKey`.
- Attach activates or creates the project layer.
- Detach deactivates it without deleting edits.
- Project or saved-pattern deletion cascades through layer ownership.

Geometry is stored in normalized page coordinates and transformed exactly once by `PatternPageCoordinateTransform`. Pointer motion remains in ViewModel memory. Persistence occurs at gesture and command boundaries rather than on every pointer event.

`PatternAnnotationCanvasRenderer` is the single renderer for both viewer overlays and rasterized export. Adding a second geometry or rendering path risks viewer/export divergence.

### Annotated PDF export

Export uses SAF `CreateDocument(application/pdf)`:

- the source PDF remains unchanged;
- pages are rendered one at a time;
- the export bitmap is bounded to at most 1800 pixels on its longest side;
- annotation layers are rendered through the shared renderer;
- a temporary PDF is written under cache `pattern_exports/`;
- the temporary output is copied to the selected destination;
- progress is reported;
- cancellation and failure clean temporary files and opened resources.

Static export tests do not replace an instrumented PDF write/read check.

## Localization and locale-sensitive formatting

All user-visible strings belong in Android resources. The base locale is English. Supported resource locales are:

- Finnish (`fi`);
- Swedish (`sv`);
- German (`de`);
- French (`fr`);
- Spanish (`es`);
- Portuguese (`pt`);
- Italian (`it`);
- Norwegian Bokmål (`nb`);
- Danish (`da`);
- Dutch (`nl`).

Together with the base resources, this produces 11 selectable app languages. `locales_config.xml` and `AppLanguage` must stay aligned with resource directories and the Settings picker.

Use plural resources for count-sensitive copy. Do not build sentences by concatenating separately translated fragments. The project intentionally avoids U+00B7 as a visual separator; use layout, whitespace, punctuation, or dedicated rows.

Locale-sensitive helpers include:

- `LocaleNumberFormatter`;
- `CanonicalNumberFormat` for stable persisted or transport numbers;
- `DurationDisplayFormatter`;
- `MinutesPerRowFormatter`;
- `LocaleDateFormat`;
- `LocalizedTextTransform`.

Code review must distinguish canonical machine formatting from localized display formatting. Decimal separators, plural forms, uppercase transforms, and relative time are locale-sensitive.

## Ravelry integration

### Trust boundary

Android never owns Ravelry client secrets, performs Basic Auth fallback, exchanges authorization codes, stores access or refresh tokens, or calls protected Ravelry APIs directly. The Android app uses Firebase anonymous authentication and Firebase Functions callables in `europe-west1`.

Ravelry credentials are Secret Manager secrets used only by Functions. They must not appear in `BuildConfig`, resources, `local.properties`, `debug.credentials.properties`, source, tests, APKs, or AABs.

### Android flow

`RavelryAuthManager` owns backend connection status, start, disconnect, callback completion, and current-user state. The authentication browser uses Auth Tab when available with a Custom Tabs fallback. Android handles only the token-free deep link `knittools://ravelry-auth-complete`.

The connected Browse Ravelry action opens Custom Tabs with sharing enabled. Android `ACTION_SEND text/plain` accepts a validated Ravelry pattern URL, but the app shows a local confirmation surface before requesting an import preview.

`RavelryBackendClient` maps callable responses to sanitized transport/domain models. Availability is preserved from transport through Room and presentation as `free`, `paid`, or `unknown` through the domain-owned `PatternAvailability`. Unknown must not be displayed as paid. A detail response without a positive `ravelryPatternId` is rejected rather than saved as ID 0.

Ravelry search, import confirmation, saved-pattern lists, Ravelry detail, and saved-pattern detail share one thumbnail field and the `RemotePatternImage` component. Only trimmed HTTPS URLs with a host are eligible. Loading uses a quiet themed footprint without a spinner; missing, malformed, non-HTTPS, and failed images remove their slot while adjacent text and actions remain usable. This path uses Coil's cache only and does not copy thumbnails into project, PDF, progress-photo, yarn-photo, capture, or Firebase Storage.

Ravelry results and saved-pattern metadata are not attached PDF documents. Project PDF viewing requires a local `patternUri`. Metadata detail opens `SavedPatternDetailScreen`; a local PDF opens the appropriate viewer.

### Backend functions

The implemented backend surface includes:

- OAuth start;
- OAuth callback;
- connection status;
- disconnect;
- current user;
- pattern search;
- import by pattern ID;
- import by pattern URL.

OAuth start stores a PKCE state document in `ravelryOAuthStates/{state}` with a short lifetime and the current `connectionGeneration`. The callback consumes one-time state, validates ownership and generation, and writes tokens only when the connection is still current.

Tokens live in `ravelryTokens/{uid}`. `functions/src/ravelry/tokenAccess.ts` is the access-token gate:

- rate limiting occurs before token refresh;
- expired access tokens refresh server-side with Secret Manager credentials;
- a rotated refresh token replaces the stored token only if the connection generation is unchanged;
- disconnect advances a tombstone generation so late callback, current-user, or refresh writes cannot recreate a disconnected connection.

Search and import callables sanitize upstream fields and return metadata only. They do not download pattern PDFs. URL import validates Ravelry hosts and pattern-library paths before resolving an ID.

The local Functions foundation was rechecked on 2026-08-26 with the existing lockfile and installed dependencies: 46 tests passed across seven suites and the TypeScript build succeeded. `package-lock.json` remained unchanged. Import sanitization requires a positive integer pattern ID and accepts only valid HTTPS thumbnail URLs. The package targets Node.js 22; local Node.js 24.19.0 and npm 11.17.0 are an environment mismatch, not deployment proof. No Firebase deployment, real OAuth callback, Secret Manager access, or live Ravelry/CDN request is established by these local checks.

### Rate limiting

The backend enforces per-UID and backend-global fixed windows. Current per-minute limits are:

| Surface | Per UID | Backend global |
|---|---:|---:|
| Authentication | 10 | 60 |
| Search | 30 | 120 |
| Import | 20 | 80 |

Global limits use ten Firestore shards, giving per-shard capacities of 6, 12, and 8 respectively. Every request also checks an active legacy `<bucket>_global` window before using shards. That compatibility check is temporary and must be removed only after deployment is complete and all legacy single-document writers are confirmed drained. A warm process caches a confirmed saturated shard window so repeated overload rejections do not rescan every shard.

### Firestore boundary

Root `firestore.rules` denies Android/client reads and writes to OAuth state, token, and rate-limit collections. Backend Admin SDK access is the intended path. A Firebase Auth identity is authentication for callable ownership, not authorization to read backend token documents directly.

## Pro, trial, and billing

### Entitlement model

`ProStatus` values are `TRIAL_NOT_STARTED`, `TRIAL_ACTIVE`, `TRIAL_EXPIRED`, and `PRO_PURCHASED`. `ProState.isPro` is true for active trial or purchased Pro.

The current product has one entitlement tier. `ProFeature` identifies the call site but does not create separate purchased entitlements. Feature IDs are:

- `UNLIMITED_PROJECTS`;
- `NOTES`;
- `SECONDARY_COUNTER`;
- `WIDGET`;
- `ROW_REMINDERS`;
- `PROGRESS_PHOTOS`;
- `MULTIPLE_COUNTERS`;
- `SHAPING_COUNTER`;
- `REPEAT_SECTION`;
- `PATTERN_CAMERA_SCAN`;
- `INSIGHTS_CHARTS`;
- `STREAK`;
- `UNLIMITED_YARN`.

Debug builds unlock `hasFeature` through `BuildConfig.DEBUG`. This does not change `isPro`, purchase state, trial state, or upgrade-screen purchase claims.

### Trial

The trial lasts 14 days and does not start automatically. The user starts it through an atomic `TrialManager.startTrial` path. Trial persistence records start state, last-known time, and clock-tamper state. A backward clock movement greater than one hour permanently marks the local trial state as tampered. Refresh is bounded and also reacts to a day boundary. Trial-ended copy is a one-time passive notice.

Status precedence is purchased Pro, active trial, not-started trial, then expired trial.

### Billing

`BillingManager` uses Google Play Billing 9.1.0 and one in-app product, `knittools_pro`. It:

- reconnects when the billing service disconnects;
- bounds initial setup retries;
- queries product details and existing purchases;
- selects a deterministic non-rental one-time offer;
- handles purchase, pending, already-owned, acknowledgement, and restore states;
- does not unlock a pending purchase;
- acknowledges eligible purchases with a bounded retry path;
- reports restore as restored, not found, or failed.

Purchase state readiness is distinct from the default `ProState`. Cold-start consumers must not fail closed before billing/trial state loads.

### Cold-start and contextual gates

`ProManager.hasFeatureAfterInitialLoad` waits for initial Pro and billing readiness with a bounded timeout and checks an already-known purchase. Widgets use this API rather than synchronously reading the default state.

`ProPromptSource` values are Projects, ProgressPhotos, Notes, YarnCards, SaveToMyYarn, Counters, Reminders, PatternCamera, and Widget. `ProPromptViewModel` resumes the blocked action exactly once after `TrialStartResult.Started`, `AlreadyStarted`, or observed Pro access.

The repository remains the authoritative gate for mutation. A prompt sheet is not a substitute for transactional enforcement.

Existing-content rules are deliberate:

- `secondaryCounterUsed` and `notesCreated` preserve used project surfaces;
- saved content is not silently deleted when access expires;
- new project, yarn, counter, reminder, photo, or camera creation follows its relevant gate;
- Insights can show basic metrics while Pro-only chart and streak surfaces remain gated;
- widgets use the cold-start-safe gate.

## UI architecture and design system

### General Compose rules

- Compose screens consume state from ViewModels and repositories; business transactions do not live in composables.
- Lifecycle-aware collection uses shared helpers such as `collectAsStateWithLifecycle` and `CollectWithLifecycleEffect`.
- Shared scaffolds and components are preferred before adding a feature-local equivalent.
- `ToolScreenScaffold` provides a plain themed surface, transparent top app bar, and a maximum content width of 600 dp.
- Scaffold backgrounds use `MaterialTheme.colorScheme.background` rather than `surface`.
- User-visible strings come from resources.
- Theme tokens and `MaterialTheme.knitToolsColors` replace hardcoded production colors.
- New cards, rows, and sections should not add decorative colored borders, side stripes, or frames. Existing functional selection, focus, input, annotation, and shape outlines should remain only where they communicate state or affordance.
- Touch targets should remain at least 48 dp even when the visible asset or icon is smaller.
- Screen-specific layout constants belong in theme dimension objects when that is the existing pattern.

### Theme architecture

The source files are:

- `ui/theme/Color.kt`;
- `ui/theme/Theme.kt`;
- `ui/theme/Type.kt`;
- `ui/theme/Shapes.kt`;
- `app/src/main/res/font/outfit.ttf`.

`KnitToolsTheme` provides fixed light and dark Material 3 schemes. Dynamic Material You colors are not used. The default stored theme is light; system dark mode matters only when the user selects `ThemeMode.SYSTEM`.

The visual direction is a warm 1970s craft palette: olive, burnt orange, avocado, mustard, cream, dusty rose, and restrained teal. `KnitToolsExtendedColors` adds `surfaceTint`, `secondaryOutline`, `onSurfaceMuted`, `brandWine`, `tealAccent`, `inactiveContent`, `navBarContainer`, `navBarIndicator`, `primaryTintContainer`, `activityCellEmpty`, `activityRamp`, and `yarnPalette`.

### Dark palette

| Token | Hex | Use |
|---|---|---|
| `Background` | `#1E1E12` | Main dark background |
| `BackgroundAlt` | `#252518` | Contrasting background area |
| `Surface` | `#2E2E20` | Base surface |
| `SurfaceHigh` | `#3A3A2A` | Elevated surface |
| `SurfaceHighest` | `#454535` | Input/highest surface |
| `Primary` | `#C45100` | Burnt-orange primary action |
| `PrimaryContainer` | `#D4722A` | Lighter primary container/gradient |
| `Secondary` | `#93AE4F` | Avocado labels and section emphasis |
| `SecondaryMuted` | `#6B8A35` | Muted avocado |
| `SecondaryContainer` | `#3A4020` | Secondary container |
| `Tertiary` | `#C9A435` | Mustard accent |
| `TertiaryContainer` | `#3A3520` | Tertiary container |
| `TextPrimary` | `#E8E4D0` | Warm cream primary text |
| `TextSecondary` | `#B8B4A0` | Secondary text |
| `TextMuted` | `#A8A491` | Muted text |
| `TextDisabled` | `#5A5840` | Disabled content |
| `DustyRose` | `#B8908F` | Trial and yarn accent |
| `Error` | `#C44D4D` | Error |
| `ErrorContainer` | `#3A2020` | Error container |
| `Success` | `#8BA44A` | Success |
| `SuccessContainer` | `#3A4020` | Success container |
| `NavBackground` | `#161610` | Bottom navigation |
| `NavText` | `#B0AC92` | Inactive navigation |
| `NavActive` | `#C45100` | Active navigation |
| `NavActiveBg` | `#3A2010` | Active navigation indicator |
| `RavelryTeal` | `#5F8A8B` | Ravelry accent |
| `PrimaryTintContainer` | `#3A2513` | Insights primary tint |
| `ActivityCellEmpty` | `#464633` | Empty fabric cell |

Dark Insights activity ramp: `#6B8A35`, `#93AE4F`, `#C9A435`, `#D4722A`.

### Light palette

| Token | Hex | Use |
|---|---|---|
| `LightBackground` | `#E8E4D0` | Main warm-cream background |
| `LightBackgroundAlt` | `#DDD8C3` | Contrasting background |
| `LightSurface` | `#D2CDB5` | Base surface |
| `LightSurfaceHigh` | `#BBB59A` | Elevated surface |
| `LightSurfaceMediumHigh` | `#C8C3A8` | Dialogs and popups |
| `LightSurfaceHighest` | `#A49D80` | Input/highest surface |
| `LightSecondary` | `#394B18` | Dark avocado |
| `LightSecondaryMuted` | `#5A7525` | Muted avocado |
| `LightSecondaryContainer` | `#D0DDB5` | Secondary container |
| `LightTertiary` | `#9A7B18` | Dark mustard |
| `LightTertiaryContainer` | `#E8DFB5` | Tertiary container |
| `LightTextPrimary` | `#2E2A1E` | Warm brown primary text |
| `LightTextSecondary` | `#4C4634` | Secondary text |
| `LightTextMuted` | `#4A473C` | Muted text |
| `LightTextDisabled` | `#C0BAA5` | Disabled content |
| `LightDustyRose` | `#9E706E` | Light-theme rose |
| `LightErrorContainer` | `#EAD0D0` | Error container |
| `LightSuccessContainer` | `#D0DDB5` | Success container |
| `LightNavBackground` | `#DDD8C3` | Bottom navigation |
| `LightNavText` | `#5A5440` | Inactive navigation |
| `LightNavActiveBg` | `#EAD0B5` | Active navigation indicator |
| `LightDivider` | `#C5C0A8` | Divider |
| `LightPrimaryTintContainer` | `#E6CFAC` | Insights tint |
| `LightActivityCellEmpty` | `#AFA98C` | Empty fabric cell |
| `LightCounterMinusIcon` | `#211E16` | Minus button icon |

Light Insights activity ramp: `#B8C47A`, `#93AE4F`, `#C9A435`, `#C45100`.

### Project/yarn palette

`yarnColorForId(id, palette)` selects `id.mod(palette.size)`. Compose passes `MaterialTheme.knitToolsColors.yarnPalette`; the default dark list is only a pure-function fallback.

Dark palette, in stable ID order:

1. `#C45100` burnt orange;
2. `#8BA44A` avocado;
3. `#C9A435` mustard;
4. `#B8908F` dusty rose;
5. `#9A6B4A` terracotta;
6. `#5A8A7A` teal;
7. `#9A82AA` lavender;
8. `#A85A3A` rust.

Light palette preserves the same ID order but uses `#C45100`, `#70843C`, `#957927`, `#A3706E`, `#9A6B4A`, `#578576`, `#8E73A0`, and `#A85A3A` for contrast.

`InsightChartColors` remains declared but has no production callers. The current chart, project fabric, project filter, mix bar, and project rows use the themed yarn palette.

### Typography

The app uses the Outfit variable font with platform sans-serif fallback. The font family exposes Normal, Medium, SemiBold, Bold, and ExtraBold through font-variation settings.

| Material role | Weight | Size | Letter spacing |
|---|---:|---:|---:|
| `displayLarge` | Bold | 57 sp | -0.25 sp |
| `displayMedium` | Bold | 45 sp | 0 |
| `displaySmall` | SemiBold | 36 sp | 0 |
| `headlineLarge` | Bold | 32 sp | 0 |
| `headlineMedium` | SemiBold | 28 sp | 0 |
| `headlineSmall` | SemiBold | 24 sp | 0 |
| `titleLarge` | SemiBold | 22 sp | 0 |
| `titleMedium` | SemiBold | 16 sp | 0.15 sp |
| `titleSmall` | Medium | 14 sp | 0.1 sp |
| `bodyLarge` | Normal | 16 sp | 0.5 sp |
| `bodyMedium` | Normal | 14 sp | 0.25 sp |
| `bodySmall` | Normal | 12 sp | 0.4 sp |
| `labelLarge` | SemiBold | 14 sp | 0.1 sp |
| `labelMedium` | SemiBold | 12 sp | 0.5 sp |
| `labelSmall` | SemiBold | 11 sp | 1.5 sp |

Documented implementation exceptions include the responsive counter hero number, adaptive bottom-navigation labels, content-card labels, the stitch badge, and chart labels. New UI should start with `AppTypography` rather than adding an inline size or weight.

`AppShapes` uses 8 dp small, 12 dp medium, and 16 dp large rounded corners.

## Project list UX

The current project list is implemented by `ui/components/ProjectListItem.kt` and `ui/screens/project/ProjectListScreen.kt`. The deleted `ProjectCard.kt` is not a current component.

### Overall structure

- The list uses cardless project rows with dividers.
- A large `Continue Knitting` hero remains a card.
- Active and completed projects have separate localized uppercase section headings and counts.
- The active hero candidate is the first active project whose count is greater than zero.
- A project shown in the hero is removed from the ordinary active list.
- The hero is hidden during multi-selection.
- Completed rows show final count and completion time; they do not show active target progress or attachment footer actions.
- The list uses 16 dp horizontal, 8 dp top, and 112 dp bottom padding.

### Continue hero

The hero shows project name, current formatted count, optional section, craft-aware label, and target progress/status. It intentionally does not show accumulated session minutes. The continue action uses `counter_continue_button.webp` with a 72 dp touch target and 64 dp visible asset.

### Project row

The full row is `combinedClickable`:

- tap opens the project;
- long press enters selection;
- selection shows a checkbox in a 48 dp column;
- selected rows use a primary color at 0.07 alpha;
- selection semantics expose the selected state.

The header shows the project name with `titleLarge` and a relative updated/completed timestamp. Context text chooses:

1. section name, if present;
2. pattern name, unless it is a raw `.pdf` filename or duplicates the project name;
3. craft label.

The layout becomes a compact stacked variant below 320 dp available width or above 1.0 font scale.

Active target progress shows localized target/status text, a fraction, and a thin progress track. The formatted current count uses primary emphasis.

The optional footer appears only for data that exists:

- the first linked yarn card, opening yarn detail;
- a local attached pattern PDF, opening the pattern;
- progress-photo count;
- note presence.

Metadata-only Ravelry/saved-pattern links do not masquerade as an attached PDF. During selection, attachment actions and their content descriptions are disabled so the row has one unambiguous action.

### Project-list actions

The top app bar owns sort order, completed visibility, and selection entry. The folder selector stays below it without replacing cardless rows or the Continue hero. Long press on active or completed rows enters selection; Select All selects only currently visible filtered rows. The vertical action area offers Move selected projects, Complete for active selections only, and confirmed Delete. A failed move keeps selection; success exits selection and restores selector focus. Folder deletion restores focus to a remaining folder or Create folder.

The create action is not a Material FAB. It uses the image-backed `counter_plus_button.webp` at bottom end with a 72 dp touch target and 64 dp visual. It is absent during selection. Free-limit handling uses `ProPromptSheet`, then retries the original create action if access is granted; the repository still enforces the limit transactionally.

### Project-list dimensions

`ProjectListDimens.kt` centralizes:

- screen horizontal 16 dp, top 8 dp, bottom 112 dp;
- hero padding 20 dp;
- hero action 72/64 dp;
- row vertical padding 14 dp;
- line gap 4 dp;
- progress group gap 8 dp, track inset 4 dp, height 4 dp;
- footer gap 6 dp, action target 48 dp, icon 18 dp;
- divider 1 dp at 0.15 alpha;
- section top 20 dp and bottom 8 dp;
- create action 72/64 dp.

## Counter workspace UX

### Route and top bar

The counter route is `counter` and obtains the selected project from the Projects-graph `CounterViewModel`. The top bar owns:

- back navigation;
- uppercase project name;
- overflow actions.

It does not show pattern subtitles, PDF names, Ravelry titles, or generic `Pattern attached` copy. The bottom navigation remains visible.

### First viewport and content order

`CounterScreen` uses one `LazyColumn`. The intended first viewport is top bar plus counter hero plus bottom navigation. The scroll order is:

1. main counter hero;
2. a deliberate reveal gap;
3. fixed project content cards;
4. additional-counter heading and list;
5. reminder content and other lower workspace surfaces.

The hero may include stitch tracking because it is active counting input. Pattern, yarn, notes, photos, reminders, extra counters, and informational content belong below the hero.

### Counter hero

The hero contains:

- optional repeat/section line;
- craft-aware primary label;
- large formatted count;
- optional target status;
- image-backed decrement and increment controls;
- image-backed undo;
- optional stitch tracker when active and permitted.

Primary controls use `CounterImageButton` and `counter_minus_button.webp`, `counter_plus_button.webp`, and `counter_undo_button.webp`. Smaller steppers remain Compose/Canvas controls; removed `CounterCraftButton`, `CounterHeroActionButton`, `plus_button`, and `minus_button` assumptions are stale.

The primary number targets 115 sp and can shrink to 48 sp to fit. Primary touch targets are 144 dp, visible plus is 125 dp, visible minus is 123 dp with a 1 dp optical offset, and undo is 92 dp. The control group is capped at 360 dp. Counter-specific values live in `CounterDimens.kt`.

### Project content cards

`CounterProjectContentCards` is a fixed five-card square layout:

- Pattern;
- Yarn;
- Notes;
- Photos;
- Reminders.

Pattern, Yarn, Notes, and Photos form a two-column square grid. Reminders is centered on its own row at half-grid width. Every card contains only icon and title: no previews, counts, chevrons, subtitles, reminder messages, or status badges.

The Pattern title is `Open Pattern` when a pattern link or PDF exists and `Add Pattern` otherwise. The click handler still distinguishes metadata from a local PDF.

Cards use theme surfaces, 8 dp corners, 12 dp grid spacing and padding, a 56 dp icon, and icon/title spacing of 12 dp. Their icon accents come from existing theme tokens; no decorative border or side stripe is used.

### Additional counters and reminders

Additional counters use cards with 88 dp minimum height, 16 dp corners, 22 dp horizontal and 18 dp vertical padding, 56 dp stepper targets, and a 48 dp overflow target. Repeat-section progress uses its own 8 dp bar.

Reminder alerts remain below the main workspace reveal rather than entering the first viewport. Repeating reminder copy uses plural resources and ordinary punctuation rather than a middle-dot separator.

### Screen orchestration

`CounterScreen` owns the sheet/dialog orchestration for project actions, counter editing, reminders, yarn management, photos, pattern choice/capture, and Pro prompts. `CounterUiStateReducers` and decision helpers keep state transitions testable. The screen must reuse shared UI surfaces; if a shared component cannot express a required state, the component contract should be reviewed before adding an isolated duplicate.

## Project actions

The project overflow/action sheet provides actions for project metadata, completion/reopen, pattern management, counter configuration, and deletion according to current state. Project actions call ViewModel/repository APIs rather than directly editing entities.

Completion records `completedAt` and preserves historical content. Reopening restores active-project presentation without inventing new session or history data. Destructive actions require explicit confirmation and coordinated file/link cleanup.

## Library and yarn UX

### Library hub

The Library landing screen provides direct entry to Saved Patterns, My Yarn, All Photos, needle sizes, size charts, abbreviations, and chart symbols. It uses a list-based shared screen language rather than turning into a generic icon-card dashboard.

### Saved Patterns

Saved Patterns contains local PDFs and metadata-only saved records. The list supports selection and deletion. `SavedPatternDetailScreen` owns metadata availability and actions. `PatternPickerSheet` lists all saved patterns for project attachment.

A saved pattern with `localPdfUri` can open the library viewer. A metadata-only record opens detail and can be attached as metadata, but it is not readable as a PDF until a local document is attached/imported.

### My Yarn

My Yarn is manual inventory:

- cards can be created without scanning or AI extraction;
- Photo Picker supplies an optional photo;
- cards show inventory status and quantity;
- list selection supports bulk actions;
- free versus Pro creation follows `UNLIMITED_YARN`.

User-facing scanner claims must not be reintroduced.

### Yarn-card detail

The detail route observes the target card through `YarnCardRepository.observeCard`. If the row disappears, navigation leaves the detail screen. Edits rely on repository results and the observed row rather than an optimistic local-only copy.

Detail supports status, quantity, project link, metadata, care symbols, and photo replacement. Project links use the bidirectional repository path described earlier.

### Project yarn notes

Project yarn notes are distinct from inventory cards. They support local name, description, quantity, and notes. `Save to My Yarn` creates or links an inventory card without deleting the project-specific note.

### Project yarn usage and remaining allocated yarn

The existing project Yarn sheet exposes Track usage for notes and linked My Yarn cards, followed by one compact used/remaining summary and Edit usage. A note with a saved card resolves to one logical usage item, retaining the existing source actions. Unlinking keeps usage; deleting either source clears only that foreign key. If both sources disappear, the snapshot-named row remains visible, editable, and deletable. Live source names take precedence without rewriting the creation-time snapshot. Completion and reopening preserve usage, and tracking stays free on existing yarn items without changing source-creation Pro limits or adding a project card or top-level destination.

Planned, allocated, and used are independent nullable `Double` meters. Blank is unknown, zero is a known amount, at least one amount is required, and negative/non-finite input is rejected rather than clamped. `YarnUsageCalculator` derives remaining as allocated minus used only when both are known; negative remaining is displayed as positive Over by. Planned never overwrites or constrains allocated. The meter/yard path reuses `MeasurementCalculator`; grams and fractional skeins require an explicit, user-confirmed pair of positive meters and grams per skein. No metadata parsing, automatic ratio inference, integer-skein rounding, or display-rounded calculation occurs. With 200 m / 100 g, allocated 300 g and used 175 g mean 600 m allocated, 350 m used, and 250 m / 125 g / 1.25 skeins remaining.

`ProjectYarnUsageViewModel`, `YarnUsageDraft`, and `YarnUsageSavedState` preserve raw numeric text separately from canonical amounts, pending unit changes, ratio inputs, source identity, and the expected revision across recreation. Locale comma/point input uses existing measurement parsing/formatting; unit switching converts valid values without persisting display rounding. Failed writes keep the draft, repeated actions are guarded, and successful persistence clears restorable input before closing the editor. `ProjectYarnUsageFlow` uses separate sheet states and waits for transitions, restores focus to the relevant action or heading, and closes a committed editor even when Activity recreation cancels its hiding animation.

The scrollable editor and delete confirmation use shared numeric fields and selectors, theme typography/colors, source-specific semantics, and at least 48 dp actions. All 11 locales contain the usage labels, errors, status text, and skein plural forms. IME, long names, narrow width, large font, and both themes are covered by rendered tests and screenshots. This feature stores only project usage: it never automatically changes global My Yarn inventory and has no file, network, Firebase, or Ravelry path.

## Tools and local calculators

The calculator/reference implementation is local and deterministic:

- `MeasurementCalculator` powers Measurements and Gauge V1: unit conversion, swatch measurement, count/size calculation, and pattern-gauge adjustment;
- increase/decrease logic distributes changes across a row;
- `CastOnCalculator` calculates cast-on stitches and resulting width from target width, stitch gauge, optional pattern repeat, and edge stitches;
- `YarnEstimator` estimates quantity using local formulas;
- `NeedleSizeData`, `SizeChartData`, `AbbreviationData`, and `ChartSymbolData` provide local reference content;
- `InstructionParser` is regex-only paste-to-parse logic.

Measurements and Gauge converts centimeters, inches, meters, and yards. Swatch width/stitch count and height/row count produce independent densities. The gauge bases are distinct: 10 cm is 100 mm, while 4 inches is 101.6 mm. Count calculations return the exact count, nearest whole count, and physical size represented by that rounded count; positive half values round up. Pattern adjustment also reports the original size, the size from leaving the pattern count unchanged, and the gauge difference. These are mathematical estimates, not automatic shaping or fit changes.

`GaugeViewModel` and `SavedStateHandle` retain raw input, local task/unit selections, canonical `Double` millimeters and counts per millimeter, and independent manual/swatch provenance. Local unit changes preserve physical values without changing the stored unit preference. `MeasurementNumberParser` admits only complete valid numeric input; finite/range checks precede integer conversion, and display rounding never feeds subsequent calculations. `GaugePresentation` builds both visible result sections and copied text from the same values.

The project action opens the same calculator with project-name context read through `CounterRepository.observeProject`; it does not change the project, notes, or Room data. The five project content cards and Pattern viewer entries remain unchanged. Copy writes localized plain text to the Android clipboard. Paste uses the existing local regex parser and retains its existing Pro gate; the four calculator tasks and Copy do not add a Pro gate or a network call.

There is no model client, prompt, cloud parser, or language-model fallback in calculator UI. Current abbreviation route input distinguishes craft type, although the current data list is the same for knitting and crochet.

## Insights UX and calculations

### State and data boundary

`InsightsViewModel` combines project and session repository flows into one `InsightsUiState`. `RepositoryLoad.Loading` and `Loaded` prevent an initially seeded empty list from replacing the skeleton before Room emits.

Heavy history calculations run upstream with `flowOn(ioDispatcher)`. Compose collects the single UI state.

`TimeRange` values are `ALL_TIME`, `THIS_WEEK`, and `THIS_MONTH`. The optional selected project filters sessions but does not change the stored data.

Metrics include:

- exact total duration;
- exact rows worked;
- minutes per row from `durationSeconds`;
- active days;
- streak;
- previous-period trend;
- time per project;
- chart interval and buckets;
- 26-week project fabric.

Session-zone day splitting is mandatory. A session crossing midnight is apportioned to local dates in its recorded zone rather than assigned wholesale by the current device zone.

### Pro shaping

Basic metrics remain available without Pro. For a non-Pro state:

- chart buckets are withheld even if `hasMeaningfulChartData` is true from the measured source;
- streak is withheld or zeroed at the UI boundary;
- Pro editorial copy explains the gated surfaces;
- project fabric is shown only for all-time context and only when the chart feature is available.

This is presentation shaping, not deletion of session history.

### Screen hierarchy

The top app-bar title is the active time-range control, not a static `Insights` label.

The content order is:

1. context row;
2. large time/row hero;
3. compact statistics;
4. trend;
5. chart or Pro/empty replacement;
6. project fabric when eligible;
7. all-project breakdown or selected-project last-worked detail;
8. selected-project session-history link.

For All Time, the context row shows a range kicker and project filter. Week/month contexts use the right-aligned project filter without redundant range copy.

A loading skeleton stays visible until both repositories emit. A full empty state is used when there are no sessions at all. A filtered/range empty state preserves the surrounding context and explains that the selection has no data.

All-project mode shows one mix bar plus project rows. Selected-project mode avoids repeating a one-project mix bar and instead shows last-worked information and an explicit history link.

### Chart model

A chart is meaningful only when at least two buckets contain data. Bucket policy is:

- This Week and This Month: day buckets;
- All Time within one calendar month: day buckets;
- All Time up to six months: week buckets, maximum 26;
- longer All Time: month buckets, maximum 12.

The model fills missing intervals with zero buckets. The initial selected bucket is the latest non-zero bucket. Selection state resets only when range or project changes.

All-project bars stack project-colored segments. Selected-project bars use the selected project's palette color. Plot height adapts to sparse, medium, or dense data. A knitted stitch lattice is rendered only when a bar is tall enough. The current selection is a baseline marker; unselected bars are not dimmed and there is no vertical antenna.

Chart interaction supports tap, drag/scrub, and accessibility custom actions. Axis labels are deliberately sparse to avoid overlap. Geometry tests cover label selection, bar slots, lattice thresholds, and selection mapping, but device rendering still requires runtime inspection.

Key chart dimensions are 168 dp normal plot height, 140 dp medium, 108 dp sparse, 22 dp axis band, 9 dp minimum visible bar, 2 dp minimum gap, 3 dp bar corner, and nominal week/month/all-time bar widths of 34/10/22 dp.

### Project fabric

`InsightsProjectFabric` represents 26 weeks aligned to the locale's first day of week. A day may contain strips for multiple projects rather than collapsing to one color. Selection exposes localized date and project-duration detail and supports accessibility actions.

The fabric uses the same themed project/yarn palette as chart segments. Empty cells use the theme activity-cell token. Geometry, month labels, selection, model aggregation, and palette contrast have focused tests.

### Insights dimensions

`InsightsDimens.kt` centralizes:

- 16 dp horizontal and 32 dp bottom content padding;
- 48 dp filter touch targets;
- 76 sp hero primary number with 52 sp minimum;
- 46 sp secondary number;
- 20 sp statistic values with 14 sp minimum;
- 22 dp section spacing;
- chart, axis, lattice, selection, and project-fabric geometry;
- 48 dp project-row minimum height;
- 14 dp project color dots;
- 22 dp project mix bar;
- empty, Pro, and skeleton dimensions.

## Widgets

`CounterWidget` is a Glance widget with responsive breakpoints:

- small: 120 by 48 dp;
- medium: 160 by 160 dp;
- large: 300 by 160 dp.

The provider XML allows both-axis resizing, declares a one-hour system update period, and targets the home screen.

Widget state has per-instance and shared forms. Resolution can use an existing instance state, shared state, the latest active project, or a default empty state. State updates synchronize widget instances after app or widget mutations.

Increment and decrement broadcasts are handled by non-exported `CounterWidgetActions`. The exported `CounterWidgetReceiver` is limited to the AppWidget provider role. Counter mutation uses repository semantics and Pro cold-start gating.

Opening a project from a widget issues the one-time token described in the startup section. Widget IDs, project IDs, and intent extras are never treated as self-authenticating input.

## Android manifest and exported surface

`app/src/main/AndroidManifest.xml` declares only:

- `android.permission.INTERNET`;
- `android.permission.VIBRATE`;
- `android.permission.CAMERA`.

Camera hardware is optional. There is no microphone permission.

Application security and platform settings:

- `allowBackup="false"`;
- explicit data-extraction and backup rules;
- `usesCleartextTraffic="false"`;
- right-to-left support enabled;
- per-app locale configuration;
- portrait orientation;
- `singleTop` main activity;
- edge-to-edge Compose theme flow.

### Exported components

| Component | Exported | Boundary |
|---|---:|---|
| `MainActivity` | Yes | Launcher, Ravelry completion deep link, text sharing |
| `CounterWidgetReceiver` | Yes | Requires `android.permission.BIND_APPWIDGET` |
| `CounterWidgetActions` | No | App-local increment/decrement broadcasts |
| AndroidX `FileProvider` | No | Grants temporary URI permissions only |

`MainActivity` accepts:

- launcher `MAIN`;
- browsable `knittools://ravelry-auth-complete`;
- `ACTION_SEND` with `text/plain`.

Every exported input is untrusted. Ravelry URLs, deep-link shape, extras, project IDs, and widget launch IDs are validated against app-owned rules/state before use.

## Security and privacy boundaries

### Network and secrets

- Cleartext traffic is disabled.
- Ravelry client credentials and tokens stay in the backend.
- Android Firebase configuration is not a Ravelry secret but is still local/generated configuration and must not be tracked at `app/google-services.json`.
- Release signing credentials are environment variables.
- Logging must not contain billing state, Ravelry credentials/tokens, pattern text, project data, notes, or other user content.
- There is no release analytics, tracking, replay, tracing, logcat breadcrumb collection, or crash reporting.

### Firebase scope

Firebase is allowed only for the Ravelry backend path:

- Firebase Anonymous Auth;
- Firebase Functions;
- Google Services configuration.

Firebase AI, ML Kit, Gemini, App Check, voice services, and model-backed parser dependencies are not part of the current product and must not be introduced as convenience transitive dependencies.

### Sentry

Sentry diagnostics exist only in `app/src/debug` and use `io.sentry:sentry-android-core` via `debugImplementation`. The release source set is a no-op. There is no Sentry Gradle plugin, source upload, replay, tracing, or release dependency.

Debug DSN resolution uses, in priority terms defined by the build helper:

- `KNITTOOLS_SENTRY_DSN`;
- `SENTRY_DSN`;
- ignored `debug.credentials.properties` key `sentry.dsn`.

The DSN must not be hardcoded or committed.

### Voice, AI, and local content

There is no `RECORD_AUDIO` permission, `SpeechRecognizer`, `TextToSpeech`, conversational voice command, or production microphone flow. Paste-to-parse is regex-only. Notes and pattern annotations are local. Do not infer AI or cloud processing from old documents, unused helper names, or generic networking dependencies.

## Build configuration and artifact gates

### Java, Kotlin, and Android

- Gradle daemon and CI use Eclipse Temurin 17.
- Android and Kotlin compile to Java 17.
- SDK versions come from `gradle/libs.versions.toml` and are consumed by both Android modules.
- Room schema export is enabled.
- Release shrinking/signing behavior is defined in `app/build.gradle.kts`.

### Release signing

Any task in the explicit release-artifact set requires:

- `KNITTOOLS_KEYSTORE_PATH`;
- `KNITTOOLS_KEYSTORE_PASSWORD`;
- `KNITTOOLS_KEY_ALIAS`;
- `KNITTOOLS_KEY_PASSWORD`.

The guarded set currently includes:

- `:app:assembleRelease`;
- `:app:bundleRelease`;
- `:app:packageRelease`;
- `:app:packageReleaseBundle`;
- `:app:packageReleaseUniversalApk`;
- `:app:signReleaseBundle`;
- `:app:publishRelease`.

Missing variables fail the task graph before artifact publication. Release signing and Firebase configuration are separate gates.

### Firebase Android configuration

The canonical local config is ignored `app/google-services.json`. CI or local automation can materialize it from `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64`.

Non-distribution variants `debug`, `benchmarkRelease`, and `nonMinifiedRelease` may generate ignored per-variant placeholder JSON when neither a real root file nor encoded config exists. Values such as `google_app_id`, `google_api_key`, and `project_id` come only from Google Services generated resources; they are not maintained in a parallel debug XML.

`assembleRelease` and `bundleRelease` depend on `verifyGoogleServicesJson` and require a real config. `lintRelease` may disable `processReleaseGoogleServices` only when no release artifact task is requested and no real config exists. This lint-only exception must never reach an artifact graph.

## CI and dependency automation

### Build workflow

`.github/workflows/build.yml` runs on pushes and pull requests to `main`. It uses:

- `actions/checkout` v7.0.1, pinned by SHA, with credential persistence disabled;
- `actions/setup-java` v5.7.0, pinned by SHA, Temurin 17;
- `gradle/actions/setup-gradle` v6.3.0, pinned by SHA.

It runs, sequentially:

1. `./gradlew assembleDebug`;
2. `./gradlew test`;
3. `./gradlew :app:ktlintCheck`;
4. `./gradlew :app:detekt`;
5. `./gradlew lint`.

### CodeQL workflow

`.github/workflows/codeql.yml` runs on pushes and pull requests to `main` and at 06:00 UTC each Monday. It:

- analyzes Java/Kotlin;
- uses manual build mode;
- builds `assembleDebug --no-daemon`;
- uses checkout v7.0.1, setup-java v5.7.0, and CodeQL action v4.37.6 pinned by SHA;
- has no separate Android setup action;
- has a six-hour job timeout.

### Backend CI gap

The current GitHub workflows do not run Functions TypeScript build or tests. Backend changes require separate evidence from:

- `npm --prefix functions test`;
- `npm --prefix functions run build`.

A green Android workflow does not validate Functions.

### Dependabot

`.github/dependabot.yml` checks weekly:

- Gradle dependencies at repository root;
- GitHub Actions at repository root;
- npm dependencies in `.deepsec`;
- npm dependencies in `functions`.

CodeQL action updates are grouped.

## Local validation and scanner surfaces

### Small direct checks

Choose the smallest command that proves the claim:

- JVM tests: `.\gradlew.bat --no-configuration-cache :app:testDebugUnitTest --rerun-tasks`;
- debug lint, ktlint, and Detekt: `.\gradlew.bat --no-configuration-cache :app:lintDebug :app:ktlintCheck :app:detekt`;
- KSP/Room code generation: `.\gradlew.bat --no-configuration-cache :app:kspDebugKotlin`;
- debug artifact: `.\gradlew.bat --no-configuration-cache :app:assembleDebug`;
- Functions tests: `npm --prefix functions test`;
- Functions type/build: `npm --prefix functions run build`;
- DeepSec matcher tests: `pnpm --dir .deepsec test:matchers`;
- custom DeepSec scan: `pnpm --dir .deepsec run deepsec:scan:custom`;
- whitespace/patch validation: `git diff --check`.

Do not run the user's aggregate wrapper scripts such as `lc` or `sc` during agent work. Their behavior is documented for orientation, and `-PlanOnly` or `-ResolveOnly` is preferred when a supported checker only needs planning.

### Project-local wrappers

Most short `tools/*.ps1` checker wrappers delegate to `C:\Dev\Android-check\tools\InvokeProjectCheck.ps1` and return the delegated exit code. The command names include `ac`, `cr`, `cs`, `db`, `dc`, `ds`, `ga`, `lc`, `ms`, `os`, `pc`, `ql`, `sc`, `sentry`, and `ss`.

Special cases:

- `tools/ad.ps1` builds/installs a debug APK and resolves `adb.exe` from `local.properties`;
- `tools/pc.ps1` defaults `PMD_CPD_MINIMUM_TOKENS` to 100;
- `tools/rs.ps1` and `tools/rst.ps1` are repository-local release-surface and self-test entry points;
- `tools/sonar.ps1 -PlanOnly` is read-only; an upload requires `-AllowExternalUpload` and a bounded Gradle process;
- `tools/sc.ps1` is the only security-check implementation source of truth;
- Bash security scripts are fail-closed compatibility delegates, not independent scanners;
- generated `reports/` must not be committed.

### Static analyzers

The repository has surfaces for:

- ktlint;
- Detekt, including a baseline and explicit toolchain diagnostics;
- Android Lint and release lint;
- Google Android Security Lints;
- OWASP dependency-check;
- OSV Scanner;
- DeepSec with custom Kotlin/Android matchers;
- CodeQL;
- Sonar and JaCoCo;
- PMD CPD;
- Compose Stability Analyzer;
- Baseline Profile generation;
- release-surface verification;
- debug/release Sentry dependency separation.

Each tool proves a different claim. A zero-match raw scan is not automatically the same as an actionable clean result if wrappers, suppressions, baselines, or exception registries classify findings.

### Scanner exceptions

`config/check-exceptions.json` is the scanner-exception registry. A MobSF exception requires one rule and one exact `findingPath`; a global `.mobsf` suppression must not hide a whole rule.

`gradle/osv-scanner.toml` uses package/advisory-specific exceptions for build-tool dependencies that appear in Gradle verification metadata. It must not return to a project-wide ignore model. Runtime dependency exposure still needs its own resolved-graph or artifact evidence.

DeepSec custom matchers cover:

- exported Android components;
- Kotlin entry points;
- intent data and extras;
- broad FileProvider paths;
- KnitTools file/URI operations;
- URI sharing without `ClipData`;
- Firebase callable and Ravelry credential surfaces;
- sensitive Android logging;
- widget counter mutation.

Accepted risk is limited to documented historical Ravelry credential findings in `config/security-decisions.md`. It is not a blanket suppression for API abuse, prompt injection, or unrelated findings.

## Test architecture

### Current inventory

The current working tree contains:

- 244 Kotlin files in `app/src/test`;
- 34 Kotlin files in `app/src/androidTest`;
- 7 TypeScript test files in `functions/src`.

These source-file counts include test helpers; they are not test-class counts or executed-test totals and should be refreshed after test additions/removals.

### What the test suite covers

The test surface includes:

- domain calculators and persisted-enum fallbacks;
- regex instruction parsing;
- locale-sensitive formatting;
- repository transactions and soft-link invariants;
- project yarn usage calculations, source identity, rollback, migration preservation, editor restoration, and native Yarn workflows;
- main-counter/history/linked-counter atomic behavior;
- Room migration source contracts and instrumented migrations;
- file storage and photo replacement ordering;
- pattern row mapping, annotations, geometry, rendering, and export;
- Pro/trial state and clock rollback;
- billing setup, restore, already-owned, pending, and acknowledgement paths;
- navigation arguments and widget launch tokens;
- widget data resolution and actions;
- ViewModel reducers and lifecycle state;
- project workspace and project-list source contracts;
- accessibility semantics and touch-target contracts;
- Ravelry auth, callable mapping, import confirmation, saved-pattern detail, and localization;
- Firebase/release-surface boundaries;
- Insights metrics, axis layout, selection, stitch lattice, project fabric, palette contrast, and ViewModel aggregation;
- workflow and scanner configuration anchors;
- Compose stability and source-level architecture contracts.

### Important instrumented boundaries

Instrumented tests are used where JVM source checks are insufficient, including real Room migration/lifecycle behavior, Android Canvas annotation rendering, and PDF export. Their presence is not proof that they ran on this checkout.

### Interpreting source-contract tests

Many tests intentionally read source files to prevent architectural regressions. Examples include:

- ensuring the counter first viewport and fixed content-card structure;
- ensuring image-backed main controls and Canvas-based small steppers;
- ensuring manual My Yarn flow and absence of scanner wording;
- ensuring Ravelry secrets and direct Android API paths stay absent;
- ensuring release configuration gates and workflow pins remain present;
- ensuring obsolete components/routes are not reintroduced.

These tests are valuable contracts, but a passing string assertion cannot prove actual layout, TalkBack traversal, touch handling, database atomicity, or runtime rendering.

## Code-review map

Use this section to route a question to the correct code and to identify the proof needed.

### 1. Room schema and migrations

Inspect:

- `data/local/KnitToolsDatabase.kt`;
- every relevant entity and DAO;
- `di/DatabaseModule.kt`;
- schema JSON 24 and the preceding schema;
- migration tests.

Questions:

- Does every schema change have a contiguous migration?
- Does the exported schema exactly match entity and index changes?
- Are non-null defaults valid for existing rows?
- Are hard foreign keys indexed and cascading intentionally?
- Are soft links repaired transactionally?
- Does migration 17 to 18 preserve existing notes/secondary-counter access?
- Does migration 18 to 19 map false to unknown and preserve saved-pattern annotation layers and annotations across the referenced-table rebuild?
- Does migration 21 to 22 backfill exactly one primary relation per readable legacy project while preserving stable document keys and per-document reader state?
- Does migration 22 to 23 add only empty folder/assignment tables and preserve every existing project-owned row and constraint?
- Does migration 23 to 24 add only empty usage storage, preserve all 16 earlier tables, and keep source deletion distinct from project deletion?

Proof: migration execution against old schemas plus schema identity, not compilation alone.

### 2. Main counter, history, and linked counters

Inspect:

- `repository/CounterRepository.kt`;
- `domain/calculator/ProjectCounterLogic.kt`;
- `domain/calculator/CounterValueFormatter.kt`;
- counter ViewModel reducers;
- widget action path.

Questions:

- Is current state read inside the transaction?
- Are count, history, stitch reset, and linked deltas atomic?
- Do app and widget paths call the same semantics?
- Does cancellation propagate?
- Are target and label strings built from shared formatting slots?

Proof: focused repository tests and, for widget/UI claims, runtime flow evidence.

### 3. Sessions and Insights

Inspect session creation, repository flows, `SessionMetrics.kt`, chart/fabric models, and `InsightsViewModel`.

Questions:

- Is `zoneId` captured at session start?
- Are cross-midnight seconds split in the stored zone?
- Does pace use `durationSeconds` and `rowsWorked`?
- Can an initial empty flow replace the loading skeleton?
- Does heavy history work run on `@IoDispatcher`?
- Are Pro data-shaping and measured-data flags distinct?
- Are chart bucket and selected-project rules preserved?

Proof: zone-aware unit cases, ViewModel aggregation tests, geometry tests, and device rendering for gestures.

### 4. Pattern PDF and reading line

Inspect `PatternDocumentStorage`, `PdfPageRenderer`, row-mapping calculators, viewer state, and repository attachment methods.

Questions:

- Is SAF access persisted or copied to app ownership correctly?
- Are Photo Picker results copied into app ownership before the external grant can expire?
- Are page order, size limits, animated-image rejection, bounded decode, and transparent backgrounds preserved in the generated PDF?
- Does cancellation remove staged images and any uncommitted PDF while leaving an already attached PDF intact?
- Is a local PDF required for viewer entry?
- Are page resources bounded and closed?
- Are project reading-line fields durable while library-only fields remain session state?
- Do manual page/line movement pause follow without creating markers, while explicit calibration still writes markers?
- Do app and widget counter changes update count/history/page/Y atomically when following is enabled?
- Do bookmark actions validate the active document and preserve rows across detach, reattach, completion, and saved-pattern deletion?
- Does a bookmark jump wait for the correct rendered page, focus its normalized Y once, and pause following?
- Can horizontal and vertical guides coexist without intercepting ordinary zoom and pan or entering export?
- Do anchors affect only their page?
- Does detach avoid deleting a still-referenced PDF?

Proof: storage tests, mapping tests, reference-count behavior, and device PDF opening.

### 5. Annotation layers and export

Inspect schema 17 entities/migrations/triggers, repository ownership, transform, ViewModel commands, renderer, and exporter.

Questions:

- Does each layer have exactly one owner?
- Is master content read-only in project context?
- Is project overlay retained across detach/reattach?
- Is normalized geometry transformed once?
- Are pointer events persisted only at boundaries?
- Do viewer and export share the renderer?
- Is temporary output cleaned on success, failure, and cancellation?

Proof: migration/lifecycle instrumented tests, renderer tests, export tests, and a produced PDF inspection.

### 6. Photos and file deletion

Inspect `AppFileStorage`, progress/yarn/pattern storage, repository update order, and startup pruning.

Questions:

- Is the new file removed if DB persistence fails?
- Is the old file retained until the replacement is durable?
- Are only app-owned URIs deleted?
- Can a crash leave only a prunable orphan?
- Are FileProvider roots narrow?
- Are composables free of direct file deletion?

Proof: failure-path tests and exact filesystem/DB state.

### 7. Yarn, saved-pattern, and project soft links

Inspect `YarnCardLinks`, `YarnCardRepository`, `ProjectYarnNoteRepository`, saved-pattern duplicate detection, and project deletion.

Questions:

- Are both sides of a yarn link changed together?
- Is malformed CSV normalized centrally?
- Does Save to My Yarn retain the project note and a single logical usage row without changing usage amounts or global inventory automatically?
- Are Ravelry ID 0 and raw URL sentinels rejected?
- Is metadata-only content kept distinct from local PDF availability?

Proof: repository transaction tests and resulting rows.

### 8. Exported components and intents

Inspect the manifest, `MainActivity` intent handling, `CounterLaunchTokenStore`, widget receiver/actions, Ravelry URL validation, and FileProvider paths.

Questions:

- Which component is exported and why?
- Is a permission attached where available?
- Can an external intent select a project?
- Can OAuth/share input consume a counter token?
- Is replay prevented across activity recreation?
- Are grants tied to a narrow content URI?

Proof: manifest and intent tests plus adversarial intent cases.

### 9. Pro, trial, and billing

Inspect `ProState`, `ProManager`, `TrialManager`, `BillingManager`, contextual prompts, and repository mutation gates.

Questions:

- Is debug unlock kept separate from purchase/trial claims?
- Does the trial start only after user action?
- Is start atomic?
- Does rollback tamper handling persist?
- Can a pending purchase unlock content?
- Is purchase readiness distinguished from default state?
- Does a prompt resume the requested action once?
- Is the actual mutation still gated transactionally?
- Does existing used content remain accessible as specified?

Proof: manager tests, billing fake/client tests, and repository result tests.

### 10. Ravelry Android-to-Functions contract

Inspect Android transport models, auth manager, Functions exports, OAuth state/token access, rate limiting, and Firestore rules.

Questions:

- Are callable names, parameters, error codes, and nullable fields aligned?
- Is availability preserved as unknown when unknown?
- Is a positive pattern ID required?
- Does URL import validate the host/path?
- Is PKCE state one-use and generation-bound?
- Can disconnect prevent late writes?
- Does refresh happen only after a limiter passes?
- Can Android read token documents or secrets?
- Is any PDF download implied where only metadata exists?

Proof: Android mapper tests, Functions tests/build, Firestore rules, and deployed-environment verification when deployment is claimed.

### 11. Navigation and shared state

Inspect `Screen.kt`, `NavGraph.kt`, parent-entry ViewModel ownership, action models, bottom-bar visibility, and route fallbacks.

Questions:

- Is the route argument encoded?
- Does detail state survive navigation as intended?
- Is a ViewModel shared only within the correct graph?
- Does invalid input return to the correct tab?
- Does top-level state save/restore without duplicates?
- Is bottom navigation visible/hidden according to the explicit route list?

Proof: navigation source tests and runtime back-stack behavior.

### 12. UI and UX

Inspect the current component rather than a similarly named deleted file. For Projects use `ProjectListItem.kt`, not `ProjectCard.kt`.

Questions:

- Does the information hierarchy match the current product rules?
- Does the first counter viewport remain uncluttered?
- Is an attached PDF distinguished from metadata?
- Are touch targets at least 48 dp?
- Is selection unambiguous for row and child actions?
- Does large font scale trigger a non-overlapping layout?
- Are strings localized and pluralized?
- Are theme tokens used in both light and dark themes?
- Are decorative borders/stripes avoided?
- Does TalkBack receive useful labels, roles, state, and custom chart actions?

Proof: source contracts, Compose semantics tests, screenshot/device inspection at multiple widths/font scales/themes, and TalkBack review.

### 13. Build and release configuration

Inspect the version catalog, app/baseline Gradle scripts, wrapper, Google Services tasks, signing task graph, manifest merge, dependency graph, and built artifact.

Questions:

- Is the changed dependency actually present in the resolved variant?
- Does a debug-only dependency leak into release?
- Does release require real Firebase config and signing?
- Can lint-only bypass leak into an artifact task?
- Is `app/google-services.json` untracked?
- Are generated resources the only Firebase value source?

Proof: resolved graph and artifact inspection; a successful configuration or debug build is not release proof.

### 14. Scanner and CI claims

Inspect raw reports, wrapper classification, exception registries, baselines, workflow files, and actual run IDs.

Questions:

- Did the analyzer run against the intended inputs?
- Did inputs change during the run?
- Was a finding suppressed, accepted, filtered, or truly absent?
- Does a wrapper return the delegated exit code?
- Does Android CI omit Functions coverage?
- Is a hosted-runner failure distinct from a code failure?

Proof: fresh analyzer-owned artifacts and exact run state.

## Implemented versus intentionally absent

### Implemented

- Local projects with knitting/crochet semantics, targets, completion, sorting, and bulk actions.
- Primary and additional counters, reminders, sections, shaping, repeats, stitch tracking, history, and undo.
- Sessions and detailed Insights.
- Saved patterns, local PDF import, gallery/camera images converted into one ordered local PDF, project bookmarks, horizontal/vertical reading guides, row following and calibration, annotations, and export.
- Ravelry metadata search/import through backend-owned OAuth.
- Yarn inventory, project yarn notes, photos, care symbols, links, and quantities.
- Progress photos and widgets.
- Local calculators and references.
- Per-app language, fixed themes, haptic preference.
- User-started trial and one-time Pro purchase.

### Not implemented

- Drive/Dropbox SDK integration or continuous sync.
- Cross-device sync, backup/restore workflow, conflict resolution, background sync, or OAuth token storage for storage providers.
- Ravelry PDF download.
- Voice or microphone commands.
- Speech recognition or text-to-speech.
- AI/model-backed instruction parsing.
- Cloud journal/notes processing.
- Release Sentry, analytics, tracking, replay, or tracing.
- Firebase AI, ML Kit, Gemini, or App Check.
- A generic Tools dashboard grid.

Future documents do not change this list until production code, configuration, and verification exist.

## Volatile facts and common stale assumptions

Recheck these directly:

- dependency and SDK versions in `gradle/libs.versions.toml`;
- Room version, migration registration, and latest schema JSON;
- CI action comments and their pinned SHAs;
- test-file counts;
- Google Services and release-signing task sets;
- scanner exception inventories;
- locale list and all resource directories.

Common stale assumptions:

- `allowBackup` is false, not true.
- Room is schema 24, not an earlier schema.
- `ProjectCard.kt` is deleted; the current row is `ProjectListItem.kt`.
- Projects are cardless list rows plus a separate Continue hero.
- The current main buttons use `CounterImageButton` and WebP assets.
- `CounterQuickActions` and `ProjectInfoSection` are not the workspace model.
- The route is `abbreviations?craftType={craftType}`, not a fixed abbreviation route.
- Saved-pattern detail does not automatically mean PDF viewer.
- A Ravelry link is metadata until a local PDF is attached.
- `app/google-services.json` is ignored and required for release artifacts; debug can use a placeholder.
- Signing and Firebase config are separate gates.
- Sentry is debug-only.
- A debug feature unlock does not mean `isPro` or purchased state.
- Widgets mutate through the same primary-counter repository semantics.
- Yarn-photo paths are not FileProvider share roots.
- Drive/Dropbox wording means SAF document selection only.
- Project reading-line calibration persists; library-only reading-line state does not.
- Voice commands are absent.
- `InstructionParser` is regex-only.
- Android CI does not test Functions.
- `README.md` is not the current implementation source of truth.

## Relationship to repository documents

- `AGENTS.md` defines working, architecture, safety, and verification rules.
- `CODEX.md` must remain aligned with `AGENTS.md`.
- `CLAUDE.md` provides product wording, UX, and visual direction; verify any implementation claim against current source.
- `config/future-sync-spec.md` describes future sync constraints, not shipped behavior.
- `config/ravelry-backend-progress.md` gives rollout context; production code and deployed state determine what is actually active.
- `config/security-decisions.md` records accepted security decisions.
- Room schema JSON is generated structural evidence.
- Plans and review documents may explain intent, but they do not override current code.

When a question needs a definitive technical answer, follow the source-of-truth order at the top of this file and cite the exact code/config/runtime path that proves it.
