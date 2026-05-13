# AGENTS.md — KnitTools

Keep this file aligned with `CODEX.md`. If one changes, update the other in the same change.

Use [`CLAUDE.md`](/home/emma/dev/KnitTools/CLAUDE.md) when product wording, visual direction, or UX structure matters.

## Snapshot

- Android app in `app` plus `baselineprofile`
- Kotlin + Jetpack Compose + Material 3
- Hilt, Room, DataStore, Glance
- Room schema version `9`
- AGP `9.1.0` + Kotlin Compose plugin `2.3.10`

## Architecture

- `data/` owns Room, DataStore, storage, Android framework access
- `domain/` owns calculation logic and domain models
- `repository/` is the seam between storage/framework details and UI consumers
- `ui/` owns screens, navigation, theme, and ViewModels
- Coroutine dispatchers that cross architectural boundaries are provided through `di/DispatchersModule` (`@IoDispatcher`); avoid hardcoded `Dispatchers.IO` in repositories and ViewModels
- Multi-step Room writes that span DAOs go through `data/local/DatabaseTransactionRunner` from repository methods; UI code must not split bidirectional yarn/project links or pattern attachment writes into separate persistence calls
- `YarnCardRepository.updateLinkedProjectId` is the canonical writer for `yarn_cards.linkedProjectId` plus `counter_projects.yarnCardIds`
- Pattern attach/detach database state goes through `CounterRepository.attachPattern` / `detachPattern` so saved-pattern rows, annotations, and project pattern fields stay atomic
- Pattern PDF files are app-owned documents under `pattern_pdfs/<projectId>`; `SavedPatternRepository.deleteLocalPatternFileIfUnused` is the cleanup gate after saved-pattern deletion, project detach, and project deletion
- Pattern camera capture temp images live under `pattern_captures/<projectId>` and are the only pattern files exposed through FileProvider; legacy `patterns/...` FileProvider URIs are resolved internally by `AppFileStorage`
- Keep business logic out of composables when a ViewModel or use case should own it
- Runtime app language is owned by AppCompat/Android per-app locale APIs; DataStore `app_language` is only a persistence and migration mirror managed by `PreferencesManager`
- `ai/AiVoiceAction` is the single AI voice action contract; keyword-only voice commands stay in the counter UI voice handler
- Journal UI and `JournalEntryViewModel` live under `ui/screens/notes`; `ai/journal` owns only journal AI processing and result models; completed journal entries are exposed through `JournalEntryUiState.pendingEntry` and consumed by `NotesEditorScreen`
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
- Debug-only Ravelry credentials belong in ignored `debug.credentials.properties`, not `local.properties`; release Ravelry credentials must come only from `KNITTOOLS_RAVELRY_*` environment variables

## Security

- Keep `usesCleartextTraffic` disabled unless explicitly justified
- Firebase AI calls must keep Firebase App Check enabled through the Play Integrity provider, and AI SDK instances should request limited-use App Check tokens
- Exported components must stay intentional and minimal
- Treat extras on exported activities as untrusted unless they are explicitly validated against app-owned state
- Keep `FileProvider` usage least-privilege
- Do not log billing state, OCR text, AI prompt content, or user project data

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


<claude-mem-context>
# Memory Context

# [KnitTools] recent context, 2026-05-12 4:51pm GMT+3

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 44 obs (16,169t read) | 2,115,861t work | 99% savings

### Apr 30, 2026
4617 11:16p ✅ Lint and Security Check Scripts Ported to Windows
4619 " 🔵 Android Lint Found 246 Errors and 115 Warnings
4618 " 🔵 Lint-Check Script Reporting False Success on Windows
4620 11:18p 🔵 Lint Issues Breakdown: 236 Missing Translations Dominate Error Count
S448 Investigating lint-check script failures on Windows and discovering hidden Android Lint issues (Apr 30, 11:19 PM)
4621 11:45p 🔵 Translation Gap Identified: 236 Strings Missing from Non-Finnish Locales
4622 11:49p 🔴 Fixed ktlint Path Pattern for Windows Compatibility
4623 11:50p 🔴 Fixed Android Lint Report Parsing to Read Full Text Report
4624 11:51p 🔴 Fixed WrongConstant Error in URI Permission Flags
### May 4, 2026
4959 4:08p 🔵 Knitting App Offline Capability Analysis Requested
4960 " 🔵 Speech Recognition Architecture Confirmed Network-Dependent
4961 " 🔵 Dual Voice Command Architecture Revealed
4962 4:10p 🔵 On-Device AI Capabilities Found Using ML Kit and Gemini Nano
4963 " 🔵 GeminiAiService Centralizes All Cloud AI Features
4964 4:11p 🔵 Dual Yarn Label Scanning Architecture with On-Device and Cloud Options
4965 " 🟣 Offline-Friendly Voice Commands Implemented with Local Parsing and TTS
4966 4:12p 🔵 Triple Voice Command Architecture with Local Parsing and AI Fallback
S679 Document KnitTools app online/offline capabilities after discovering user's assumption about voice commands was incorrect (May 4, 4:12 PM)
### May 8, 2026
5297 9:14p 🔵 Gradle test task does not support --tests filter flag
5300 9:56p 🔄 AiVoiceAction Relocated from UI Layer to AI Package
5301 " 🔄 Journal UI Components Relocated from AI Package to Notes Screen Package
5298 9:58p 🔵 Gradle daemon file lock prevented test execution
5299 " 🔵 Voice command AI classes moved from counter UI package to dedicated ai package
5306 10:50p 🔵 KnitTools Room Entity Refactoring Baseline Established
### May 9, 2026
5307 2:50a 🔵 Baseline profile module minSdk lower than app module minSdk
5310 " 🔵 Baseline Profile Module Has Lower minSdk Than App Module
5311 " 🔵 All AndroidX and Firebase Dependencies Compatible with SDK 36
5313 " 🔵 SDK Configuration and Library Compatibility Verification Complete
5318 " ⚖️ Baselineprofile minSdk=28 and Library Version Strategy Decisions
5319 2:56a ✅ Updated baselineprofile minSdk to match app minimum SDK level
5320 " ✅ Updated Firebase BoM and ML Kit GenAI Prompt to newer releases
### May 12, 2026
5321 10:14a 🔵 SonarQube Analysis Failed Due to Duplicate File Indexing
5322 " 🔵 SonarQube Configuration Missing Test Source Separation
5323 10:15a 🔴 Delegated SonarQube Source Configuration to Gradle Plugin
5324 " 🔵 SonarQube Analysis Fails Due to Duplicate File Indexing
5326 " 🔵 SonarCloud Reports 67 Open Issues Across Code Quality Categories
5325 10:22a 🔴 Fixed SonarQube Duplicate File Indexing by Filtering Gradle-Managed Properties
5327 10:30a 🔵 SonarQube Issues Statistical Breakdown Reveals Key Problem Areas
5329 " 🔵 SonarQube Analysis Identified 67 Code Quality Issues
5328 10:31a 🔵 SonarQube Identifies One Vulnerability and One Bug Among Code Smells
**5330** 10:34a 🔄 **Injected IO Dispatcher to Fix Hardcoded Dispatchers Issues**
Fixed SonarQube kotlin:S6310 rule violations by introducing dependency injection for coroutine dispatchers. Created app/src/main/java/com/finnvek/knittools/di/DispatchersModule.kt with an AppDispatchers object and @IoDispatcher qualifier. Injected the dispatcher into ProgressPhotoRepository, YarnLabelScanRepository, and CounterViewModel constructors. Updated PatternViewerScreen and PatternPickerSheet to reference AppDispatchers.IO instead of Dispatchers.IO directly. This makes dispatcher selection testable and centralizes the suppression of the hardcoded dispatcher warning to a single location.
~324t 🛠️ 185,615

**5331** " 🔄 **Replaced Uri.parse with toUri KTX Extension**
Replaced all Uri.parse() calls with the AndroidX Core KTX extension .toUri() to address Android lint UseKtx warnings. The extension provides a more Kotlin-idiomatic approach and is recommended by Android best practices. Changes span authentication flows (RavelryAuthManager OAuth callback), photo handling (PhotoGalleryScreen camera captures), external browser launches (RavelryDetailScreen, SettingsScreen), and PDF pattern viewing (PatternViewerScreen, PatternPickerSheet, CounterViewModel).
~264t 🛠️ 185,615

**5332** " 🔄 **Replaced Bitmap and SharedPreferences APIs with KTX Extensions**
Replaced deprecated or non-idiomatic Android APIs with AndroidX KTX extensions to address Android lint UseKtx warnings. Bitmap operations now use the androidx.core.graphics.scale extension and createBitmap function, which are more concise and handle edge cases better. SharedPreferences editing now uses the androidx.core.content.edit { } extension with automatic apply()/commit() handling. Also fixed SonarQube kotlin:S899 issue by checking the Boolean return value of File.delete() and calling deleteOnExit() as a fallback in ProgressPhotoStorage.
~303t 🛠️ 185,615

**5333** " 🔄 **Converted ViewModel Suspend Functions to Callback-Based APIs**
Fixed SonarQube kotlin:S6313 rule violations stating "Classes extending ViewModel should not expose suspending functions." Refactored CounterViewModel.selectProjectByIdForLaunch, LibraryViewModel.getSavedPattern, and YarnCardViewModel.loadCardForDetail from suspend functions returning values to regular functions accepting callbacks. Each method now launches its own coroutine in viewModelScope and invokes the callback with the result. This pattern aligns with Android ViewModel best practices where ViewModels should manage their own coroutine lifecycle and communicate results via callbacks or StateFlow. Updated navigation LaunchedEffect blocks in NavGraph to use the callback-based APIs for route argument validation and updated YarnCardViewModelTest to match the new API.
~392t 🛠️ 185,615

5334 " 🔄 Replaced mutableStateOf with Primitive-Specific State Functions
**5344** 3:09p 🔵 **Snackbar Event Handling Patterns Verified Across Configuration Changes**
Investigation of snackbar event handling across MainActivity, CounterScreen, YarnEstimatorScreen, NotesEditorScreen, and PatternViewerScreen revealed three intentional patterns for managing one-time vs. repeating snackbar events. In-app update prompts use an incrementing Long counter as the LaunchedEffect key, ensuring the snackbar re-appears after configuration changes until the user acts. One-time error messages (scanError, pendingEntry) use nullable StateFlow fields that are immediately cleared after display via updateField { copy(scanError = null) } or consumePendingEntry(), preventing replay. Voice command events use MutableSharedFlow with extraBufferCapacity=1, which does not buffer or replay events. Copy-to-clipboard snackbars launch from user gesture callbacks without LaunchedEffect wrapping, so they cannot replay. Test coverage confirms both the intentional replay behavior (downloadedUpdatePromptId increments on resume) and the consumption pattern (pendingEntry is consumed and state is reset). No unintended replay risks were found; all snackbar patterns match their intended lifecycle semantics.
~535t 🔍 140,030


Access 2116k tokens of past work via get_observations([IDs]) or mem-search skill.
</claude-mem-context>
