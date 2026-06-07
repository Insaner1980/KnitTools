# AGENTS.md — KnitTools

Keep this file aligned with `CODEX.md`. If one changes, update the other in the same change.

Use [`CLAUDE.md`](/home/emma/dev/KnitTools/CLAUDE.md) when product wording, visual direction, or UX structure matters.

## Snapshot

- Android app in `app` plus `baselineprofile`
- Kotlin + Jetpack Compose + Material 3
- Hilt, Room, DataStore, Glance
- Room schema version `13`
- AGP `9.1.0` + Kotlin Compose plugin `2.3.10`

## Architecture

- `data/` owns Room, DataStore, storage, Android framework access
- `domain/` owns calculation logic and domain models
- `repository/` is the seam between storage/framework details and UI consumers
- `ui/` owns screens, navigation, theme, and ViewModels
- Coroutine dispatchers that cross architectural boundaries are provided through `di/DispatchersModule` (`@IoDispatcher`); avoid hardcoded `Dispatchers.IO` in repositories and ViewModels
- Multi-step Room writes that span DAOs go through `data/local/DatabaseTransactionRunner` from repository methods; UI code must not split bidirectional yarn/project links or pattern attachment writes into separate persistence calls
- Main counter changes go through `CounterRepository.applyMainCounterChange`, which reads the current project row and writes count, history, current-stitch reset, and linked-counter deltas inside one repository transaction; widget row count changes delegate to the same semantics through `applyWidgetCountChange`
- Project craft type and main counter label are owned by `domain/model/CraftType` and `domain/model/MainCounterLabelType`; legacy projects default to `KNITTING` + `ROWS`, crochet projects default to `ROUNDS`, and custom labels are trimmed and limited centrally before persistence/display
- Main counter display text is shaped by `domain/calculator/CounterValueFormatter`; Compose maps its slots to localized strings for hero labels, target rows, button content descriptions, and project-card count text
- Extra-counter linked state is stored in `ProjectCounter.linkedToMainCounter` from the add/edit counter draft; repeat-section counters must not be marked linked because they already derive progress from main-counter rows
- Project note replacement writes go through `CounterRepository.saveProjectNotes`, which merges against the editor's base notes so concurrent editor flows are preserved instead of overwritten
- Session rows store both display minutes and exact `durationSeconds`/`rowsWorked`; insights pace calculations must use the exact fields and split cross-midnight sessions by the device local date
- Insights screen state is aggregated in `InsightsUiState`; heavy session-history calculations should run upstream with `@IoDispatcher` before Compose collects the single UI state
- Legacy secondary counter state lives in `counter_projects.secondaryCount`; `project_counters` is only for named extra, repeating, shaping, and repeat-section counters, migrations must not duplicate `secondaryCount` into `project_counters`, and old generated `Pattern repeat` backfill copies are ignored at the counter UI boundary
- Extra counter type rules are owned by `domain/model/ProjectCounterType` plus `domain/calculator/ProjectCounterLogic`; repositories should apply those domain rules inside a transaction instead of duplicating counter behavior in DAO SQL
- Yarn/project link writes go through `YarnCardRepository`: `saveCard` normalizes any persisted `linkedProjectId`, and `updateLinkedProjectId` is the canonical explicit relink writer for `yarn_cards.linkedProjectId` plus `counter_projects.yarnCardIds`
- Yarn-card ID CSV parsing/formatting is owned by `domain/model/YarnCardLinks`; callers should not split or join `counter_projects.yarnCardIds` ad hoc
- Project-only yarn notes live in `project_yarn_notes` and go through `ProjectYarnNoteRepository`; `saveToMyYarn` creates a linked `YarnCard` while preserving the project note row through `savedYarnCardId` in one repository transaction
- Yarn card detail routes observe the target card through `YarnCardRepository.observeCard`; the route must leave the detail screen if the row disappears, and detail edits should rely on repository write results plus the observed row instead of optimistic local-only state
- Yarn card photo updates go through `YarnCardRepository.updatePhotoUri`, which copies the selected image with `data/storage/YarnPhotoStorage` into app-owned `yarn_photos/<cardId>` storage and cleans up the old app-owned photo; yarn photos are not FileProvider-exposed share paths
- Pattern attach/detach database state goes through `CounterRepository.attachPattern` / `detachPattern` so saved-pattern rows, annotations, and project pattern fields stay atomic
- Pattern PDF files are app-owned documents under `pattern_pdfs/<projectId>`; `SavedPatternRepository.deleteLocalPatternFileIfUnused` is the cleanup gate after saved-pattern deletion, project detach, and project deletion
- Pattern PDF import, including the v1 Drive/Dropbox copy, stays on Android Storage Access Framework `OpenDocument(application/pdf)` plus persistable URI grants; do not add Drive/Dropbox SDKs, OAuth, provider-specific dependencies, or a separate import storage flow before a new sync spec
- Drive/Dropbox sync is future-spec work tracked in `config/future-sync-spec.md`. Manual export/import or backup/restore comes before continuous sync. Do not market cross-device sync until conflict handling, background sync, offline behavior, OAuth/token storage, and the Pro gate are specified and implemented
- Pattern viewer reading-line state belongs to `counter_projects.readingLineEnabled` and `readingLineYFraction` for attached project PDFs; library-only pattern viewer state is session/rotation-saveable and must not create a saved-pattern schema path in v1
- Pattern camera capture is a photo-to-PDF flow: user-facing copy must use photo/PDF wording instead of scan/scanner wording, temp images live under `pattern_captures/<projectId>`, and only pattern/progress photo paths are exposed through FileProvider; FileProvider authority and share URI creation go through `AppFileStorage`, while legacy `patterns/...` and `yarn_photos/...` URIs are resolved internally by `AppFileStorage` for cleanup/read compatibility
- Keep business logic out of composables when a ViewModel or use case should own it
- Runtime app language is owned by AppCompat/Android per-app locale APIs; DataStore `app_language` is only a persistence and migration mirror managed by `PreferencesManager`
- Ravelry is intentionally backendless: OAuth authorization requests include PKCE, but release builds may embed Ravelry Basic Auth credentials and OAuth client secret after explicit `KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS=true` opt-in; keep `config/security-decisions.md` aligned with this accepted risk
- Debug-only Pro override is centralized in `ProState.hasFeature` through `BuildConfig.DEBUG`; it opens feature gates in debug builds without changing `isPro`, billing purchase state, trial state, or Pro upgrade UI purchase claims
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
- Counter route first viewport is top bar plus row-counter hero plus bottom navigation; reminder cards, project content cards, extra counters, and stitch tracking belong below the hero scroll
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


<claude-mem-context>
# Memory Context

# [KnitTools] recent context, 2026-05-31 11:30am GMT+3

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 50 obs (21,682t read) | 2,907,443t work | 99% savings

### May 17, 2026
5385 1:32p 🟣 CodeQL Security Scanning Restored for KnitTools Android Project
5389 2:23p 🟣 CodeQL workflow restored for KnitTools CI security scanning
5390 " 🔴 Dependency verification extended with 18 missing artifacts for CI compatibility
5391 " 🔴 Ktlint formatting violations fixed for CI compliance
5392 " 🔵 dBcheck CodeQL configuration pattern identified for Android projects
5394 4:40p 🔴 Fixed session tracking accuracy and lifecycle handling
5396 7:40p 🔵 Pace calculation logic verified with comprehensive edge-case guards
5418 10:27p 🚨 Removed Firebase config file from entire Git history
### May 18, 2026
5398 12:25a ✅ Disabled ossIndex analyzer in OWASP dependency-check configuration
5400 12:28a 🔵 OSV-scanner scans verification-metadata.xml containing build-time dependencies not in runtime classpath
5403 " 🔴 OSV scanner failure fixed by upgrading Guava and filtering verification metadata
5407 " 🚨 OSV Scanner Detected Vulnerability in KnitTools Dependencies
5408 " 🔴 OSV scanner now passes by filtering Gradle verification metadata
### May 19, 2026
5448 2:54p 🔴 Fixed Ravelry OAuth refresh failure handling and pattern save feedback
5449 3:20p 🔵 Ravelry token refresh failure signs user out and shows reconnect prompt
### May 20, 2026
5479 5:19p 🔵 osv-scan Filters Gradle Dependency Verification Metadata
5480 " 🔵 OSV-Scanner Gradle Verification Metadata Filtering is Documented Recurring Issue
### May 21, 2026
5505 2:57p 🔵 Two Dependabot PRs blocked by missing Gradle verification-metadata.xml entries
### May 29, 2026
5513 6:12p 🔵 Extra Counter Value Display Logic Located
5514 6:13p 🔵 Counter Value Format String Resources Inventory
5516 6:14p 🔵 Counter Target Helper Text Has Test Coverage
5518 " 🔵 Counter Value Formatting Scattered Across Four UI Surfaces
5525 8:07p 🟣 Implemented Shared Counter Value Formatter
5526 8:09p 🟣 Added Comprehensive Unit Tests for Counter Value Formatter
5523 8:10p 🔵 Counter Formatting Function Usage Patterns Mapped
### May 30, 2026
5527 1:17a 🟣 Migrated MultiCounterComponents to Use CounterValueFormatter
5532 1:23a ✅ All Unit Tests Pass After CounterValueFormatter Migration
S828 Continue CounterValueFormatter implementation - user requested "jatka" to proceed with Step 1 of centralizing counter display logic (May 30, 1:23 AM)
S833 Continue counter UI polish work - fixed reminder repeat display formatting (May 30, 1:24 AM)
5533 1:58a 🔵 Repeat Count Format Has Single Usage and No Existing Plural Resources
5534 " 🟣 Implemented Android plural resources for reminder repeat display
5535 " 🟣 Created comprehensive regression tests for reminder repeat display format
5536 " 🔵 Unit tests passed confirming reminder repeat plural implementation correctness
5537 6:52a 🟣 Counter Display and Workspace Polish Implementation
S838 Fix KnitTools Android build failure caused by missing counter string resources after accidental git revert (May 30, 6:53 AM)
5538 8:01p ✅ Added shaping counter subtitle string resources across all locales
5539 8:06p 🟣 Shaping counter subtitle displays next shaping event below counter name
5540 " 🔵 Tests explicitly prohibit next_shaping_format string resource
5541 " ⚖️ Abandoned shaping counter subtitle due to test prohibition
5542 " 🔄 Removed shaping counter subtitle display from CounterListItem
5544 8:08p 🔵 Comprehensive audit revealed 16 missing string resources in counter module
5543 8:17p 🔵 Missing string resources identified in MultiCounterComponents.kt
5545 8:19p 🔵 Missing string resources were never compiled into build artifacts
5546 " 🔵 Missing string usage patterns mapped with format arguments and context
5547 8:20p 🔵 Systemic missing string resources across entire KnitTools app
5548 " 🔵 Predecessor string translations exist in git HEAD across all 11 locales
5549 8:23p 🔵 Pattern and stitches translation vocabulary established across all locales
5550 8:24p 🔵 project_content_pattern missing from established naming pattern with sibling translations available
**5551** " 🔵 **repeat_section_progress_format exists with middle-dot separator across all locales**
Verification scan confirmed that repeat_section_progress_format string resource exists in current working tree across all 11 supported locales with consistent middle-dot separator formatting. The format pattern is "Repeat X of Y · Row Z of W" with localized prepositions and nouns. This string is NOT one of the missing resources identified earlier - it exists and is used in MultiCounterComponents.kt for displaying repeat section progress. The middle-dot (·) provides visual separation between the two progress components. Finnish uniquely uses slash (/) for ratios while all other languages use prepositions (of/af/von/de/sur/di/av/van). This discovery confirms that not all strings referenced in the counter module are missing; only specific ones identified in the comprehensive audit lack definitions.
~416t 🔍 1,529

**5552** " 🔴 **Restored 16 missing counter string resources across all 11 locales via Python script**
Created and executed automated Python script to restore all missing counter-related string resources across the entire KnitTools project. The script contained hardcoded professional translations for 15 string resources and 1 plural resource across all 11 supported languages (English, Danish, German, Spanish, Finnish, French, Italian, Norwegian, Dutch, Portuguese, Swedish). The script intelligently inserted the resources after the existing counter_increase line to maintain logical grouping, or before the closing resources tag as fallback. All 11 locale files were successfully modified, resolving the Kotlin compilation errors caused by unresolved string resource references in CounterWorkspaceSections.kt and MultiCounterComponents.kt. This batch restoration approach ensured consistency across all locales and eliminated the need for manual translation and insertion into each file.
~525t 🛠️ 26,957

**5553** 11:07p 🔴 **Verified all counter string resources restored and XML files valid**
Post-restoration verification confirmed complete success of the string resource recovery operation. Re-scanning the counter module source files (CounterWorkspaceSections.kt, MultiCounterComponents.kt, CounterValueFormatter.kt) found zero missing string or plural resource references, confirming all 16 previously missing resources were successfully restored. XML validation using Python's ElementTree parser verified that all 11 locale strings.xml files remain well-formed after the batch insertion, ensuring no syntax errors were introduced. The temporary Python restoration script was cleaned up after successful execution. The counter module compilation errors are now fully resolved with complete internationalization support across all 11 languages. The only remaining missing resources in the broader app are dynamic template strings (abbreviations, symbols, size rows, units, insights footer) that are outside the counter module scope and unrelated to the current build failure.
~420t 🛠️ 13,717

S839 Fix KnitTools Android build failure caused by missing counter string resources after accidental git revert, restore all missing resources across 11 locales, and verify successful compilation (May 30, 11:07 PM)
**5554** 11:08p 🔴 **Build verification successful: KnitTools debug APK compiled with all restored counter strings**
Clean build verification confirmed complete resolution of the counter module compilation failure. After restoring all 16 missing string resources (15 strings + 1 plural) across all 11 locale files, the Gradle build system successfully compiled the entire KnitTools Android application without errors. The build executed 33 tasks from scratch (after clean), leveraged 10 tasks from Gradle cache, and kept 1 task up-to-date, demonstrating efficient incremental build behavior. The resulting debug APK weighs 88.7 MB, consistent with a full-featured Android application with multiple locale resources. This successful build proves that the Python-scripted batch restoration of missing counter string resources completely resolved the unresolved reference errors in CounterWorkspaceSections.kt and MultiCounterComponents.kt, and the application is now in a buildable state on the codex/project-workspace-cards branch.
~388t 🛠️ 2,987

S843 Verify clean restoration of 16 counter resources, then remove middle-dot separator from repeat_section_progress_format across all locales and update test expectations (May 30, 11:12 PM)
**5555** 11:14p 🔵 **Git diff confirms clean restoration: 19 additions per locale, zero modifications to existing strings**
Git diff analysis confirms the string restoration operation was surgically clean with zero impact to existing translations. Every locale file (values, values-da, values-de, values-es, values-fi, values-fr, values-it, values-nb, values-nl, values-pt, values-sv) shows exactly 19 additions and 0 deletions versus HEAD, proving no existing string resources were modified, replaced, or removed during the restoration process. The absence of deletion lines confirms the Python script's insertion logic worked correctly: it found the anchor line (counter_increase) and appended the new resource block without disturbing surrounding content. The CRLF/LF warnings are cosmetic - the Python script used Windows-native line endings which git will normalize to LF on commit, maintaining consistency with the repository's existing line-ending configuration. This verification provides confidence that the restoration preserved all original translations while adding only the missing counter resources.
~450t 🔍 2,467

S846 Remove middle-dot separator from repeat_section_progress_format and fix failing counter string resource tests (May 30, 11:14 PM)
S840 Fix KnitTools Android build failure by restoring 16 missing counter string resources and verify clean restoration without modifying existing translations (May 30, 11:14 PM)
S845 Remove middle-dot separator from repeat_section_progress_format and verify counter resource restoration integrity (May 30, 11:32 PM)
S844 Remove middle-dot separator from repeat_section_progress_format string resource across all locales and update test expectations (May 30, 11:32 PM)
S848 Remove middle-dot separator from repeat_section_progress_format across all locales and ensure counter string resource tests pass (May 30, 11:35 PM)
**Investigated**: Test failure analysis revealed CounterStringResourcesSourceTest failing on deprecated "next_shaping_format" resource existence assertion (line 33), separate from middle-dot removal work. Verified deprecated resource existed in all 11 locale files due to earlier git checkout restoring it from HEAD. Confirmed middle-dot successfully removed from repeat_section_progress_format and XML well-formedness across all locale files. Re-ran tests after deprecated resource cleanup.

**Learned**: The CounterStringResourcesSourceTest contains multiple assertions checking both feature requirements (no middle-dot separators) and codebase hygiene (no deprecated keys). The git checkout operation that wiped counter strings also restored the deprecated next_shaping_format resource that the branch had intentionally deleted. Test now checks for bare middot character "·" instead of space-middot-space " · " pattern to catch any separator usage. All three issues (missing counter resources, unwanted middot separator, deprecated key restoration) were side effects of the same git checkout accident.

**Completed**: Successfully removed middle-dot separator from repeat_section_progress_format in all 11 locales (e.g., "Repeat %1$d of %2$d · Row %3$d of %4$d" → "Repeat %1$d of %2$d Row %3$d of %4$d"). Updated CounterStringResourcesSourceTest assertion to detect any middot character usage. Removed deprecated next_shaping_format resource from all 11 locale files (1 removal per file, confirmed no source code references remain). XML validation passed. Unit tests BUILD SUCCESSFUL in 1m 30s with CounterStringResourcesSourceTest and CounterValueFormatterTest both passing (EXIT=0).

**Next Steps**: All requested work completed successfully. Counter resource restoration verified clean (19 additions, 0 deletions per locale), middle-dot separator fully removed, deprecated resources cleaned up, and all tests passing. Awaiting user feedback or next request.


Access 2907k tokens of past work via get_observations([IDs]) or mem-search skill.
</claude-mem-context>
