# AGENTS.md — KnitTools

Keep this file aligned with `CODEX.md`. If one changes, update the other in the same change.

Use [`CLAUDE.md`](/home/emma/dev/KnitTools/CLAUDE.md) when product wording, visual direction, or UX structure matters.

## Snapshot

- Android app in `app` plus `baselineprofile`
- Kotlin + Jetpack Compose + Material 3
- Hilt, Room, DataStore, Glance
- Room schema version `10`
- AGP `9.1.0` + Kotlin Compose plugin `2.3.10`

## Architecture

- `data/` owns Room, DataStore, storage, Android framework access
- `domain/` owns calculation logic and domain models
- `repository/` is the seam between storage/framework details and UI consumers
- `ui/` owns screens, navigation, theme, and ViewModels
- Coroutine dispatchers that cross architectural boundaries are provided through `di/DispatchersModule` (`@IoDispatcher`); avoid hardcoded `Dispatchers.IO` in repositories and ViewModels
- Multi-step Room writes that span DAOs go through `data/local/DatabaseTransactionRunner` from repository methods; UI code must not split bidirectional yarn/project links or pattern attachment writes into separate persistence calls
- Widget row count changes go through `CounterRepository.applyWidgetCountChange`, which reads the current project row and writes count, history, and current-stitch reset inside one repository transaction
- Project note replacement writes go through `CounterRepository.saveProjectNotes`, which merges against the editor's base notes so concurrent appends from voice/journal/editor flows are preserved instead of overwritten
- Session rows store both display minutes and exact `durationSeconds`/`rowsWorked`; insights pace calculations must use the exact fields and split cross-midnight sessions by the device local date
- Legacy secondary counter state lives in `counter_projects.secondaryCount`; `project_counters` is only for named extra, repeating, shaping, and repeat-section counters, migrations must not duplicate `secondaryCount` into `project_counters`, and old generated `Pattern repeat` backfill copies are ignored at the counter UI boundary
- Yarn/project link writes go through `YarnCardRepository`: `saveCard` normalizes any persisted `linkedProjectId`, and `updateLinkedProjectId` is the canonical explicit relink writer for `yarn_cards.linkedProjectId` plus `counter_projects.yarnCardIds`
- Yarn card detail routes observe the target card through `YarnCardRepository.observeCard`; the route must leave the detail screen if the row disappears, and detail edits should rely on repository write results plus the observed row instead of optimistic local-only state
- Pattern attach/detach database state goes through `CounterRepository.attachPattern` / `detachPattern` so saved-pattern rows, annotations, and project pattern fields stay atomic
- Pattern PDF files are app-owned documents under `pattern_pdfs/<projectId>`; `SavedPatternRepository.deleteLocalPatternFileIfUnused` is the cleanup gate after saved-pattern deletion, project detach, and project deletion
- Pattern camera capture temp images live under `pattern_captures/<projectId>` and are the only pattern files exposed through FileProvider; legacy `patterns/...` FileProvider URIs are resolved internally by `AppFileStorage`
- Keep business logic out of composables when a ViewModel or use case should own it
- Runtime app language is owned by AppCompat/Android per-app locale APIs; DataStore `app_language` is only a persistence and migration mirror managed by `PreferencesManager`
- Ravelry is intentionally backendless: OAuth authorization requests include PKCE, but release builds may embed Ravelry Basic Auth credentials and OAuth client secret after explicit `KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS=true` opt-in; keep `config/security-decisions.md` aligned with this accepted risk
- `ai/AiVoiceAction` is the single AI voice action contract; keyword-only voice commands stay in the counter UI voice handler
- `ai/live/LiveVoiceFunctionCallMapper` is the validation boundary for Gemini Live tool calls; Gemini Live only maps non-mutating `query_project` and `help` tools because project context is untrusted, while keyword-only voice commands remain the mutating path
- `ai/live/VoiceLiveSession` owns Gemini Live voice session lifecycle, quota, and timeout state; Firebase-specific connection setup lives in `ai/live/FirebaseVoiceLiveConnector` as the framework boundary
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
- Debug-only Ravelry credentials belong in ignored `debug.credentials.properties`, not `local.properties`; release Ravelry credentials come from `KNITTOOLS_RAVELRY_*` environment variables and require explicit embedded-secret opt-in

## Security

- Keep `usesCleartextTraffic` disabled unless explicitly justified
- Ravelry Basic Auth credentials and OAuth client secret are an accepted no-backend risk only when documented in `config/security-decisions.md` and gated by the release opt-in; DeepSec marks only the documented Ravelry `secrets-exposure` findings as accepted-risk after revalidation
- Firebase AI calls must keep Firebase App Check enabled through the Play Integrity provider, and AI SDK instances should request limited-use App Check tokens
- Gemini Live system instructions must treat project context as untrusted quoted data; user/project/imported text must not be interpolated as executable model instructions
- Gemini Live tools must stay non-mutating unless a deterministic recent-spoken-intent or confirmation boundary is added in code
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
- Project-local PowerShell wrappers are two-letter `tools/*.ps1` scripts; check wrappers delegate to `C:\Dev\Android-check\tools\AndroidProjectChecks.psm1`, and `ad` delegates to `C:\Dev\Android-check\tools\InstallDebugToDevice.ps1`
- `lc` runs ktlint, detekt, and Android lint into `reports/ktlint.txt`, `reports/detekt.txt`, and `reports/lint.txt`
- `ad`, `ac`, `dc`, `ss`, `ds`, `ms`, `os`, `ql`, `db`, `pc`, `cs`, `cr`, `ga`, and `sc` are project-local wrappers; use `-PlanOnly` or `-ResolveOnly` for dry checks where supported
- `ad` builds `assembleDebug`, resolves `adb.exe` from `local.properties` `sdk.dir`, and installs `app/build/outputs/apk/debug/app-debug.apk` with `adb install -r`; use `ad -NoBuild` to install an already-built APK
- `pc` runs PMD CPD duplicate detection, `cr` runs compose-rules through ktlint/detekt, `ga` runs Android Lint with Google Android Security Lints, and `cs` is available for Compose Stability Analyzer projects.
- `sc` runs dependency, secret, and light Semgrep checks; `sc -Full` also runs the Android-specific `ac` path and DeepSec custom report
- Typical commands: `./gradlew assembleDebug`, `./gradlew test`, `./gradlew :app:detekt`, `./gradlew lint`
- Do not run the user's wrapper scripts such as `lc` or `sc`
- Never commit generated `reports/`


<claude-mem-context>
# Memory Context

# [KnitTools] recent context, 2026-05-17 1:14pm GMT+3

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 50 obs (18,324t read) | 2,391,109t work | 99% savings

### Apr 30, 2026
4618 11:16p 🔵 Lint-Check Script Reporting False Success on Windows
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
5330 10:34a 🔄 Injected IO Dispatcher to Fix Hardcoded Dispatchers Issues
5331 " 🔄 Replaced Uri.parse with toUri KTX Extension
5332 " 🔄 Replaced Bitmap and SharedPreferences APIs with KTX Extensions
5333 " 🔄 Converted ViewModel Suspend Functions to Callback-Based APIs
5334 " 🔄 Replaced mutableStateOf with Primitive-Specific State Functions
5344 3:09p 🔵 Snackbar Event Handling Patterns Verified Across Configuration Changes
### May 17, 2026
**5371** 8:09a 🔵 **Windows Sandbox Blocks Skill File Access with Permission Error**
During cast-on calculator inspection session startup, the system attempted to load the using-superpowers skill file from the plugin cache. Both attempts to read C:\Users\emmah\.codex\plugins\cache\openai-curated\superpowers\dc902811\skills\using-superpowers\SKILL.md failed with Windows sandbox error "CreateProcessAsUserW failed: 5". Error code 5 indicates ACCESS_DENIED in Windows, suggesting the sandbox lacks permissions to execute PowerShell commands accessing user profile directories. This blocks skill system initialization and may affect other plugin-cached resources.
~268t 🔍 343

5373 " 🔵 Cast-on calculator edge case identified via TDD
5374 " 🔴 Fixed cast-on calculator to handle pattern repeat larger than body stitches
5375 " 🔵 Cast-on calculator validation analysis shows consistent pattern
**5376** 8:16a 🔵 **Cast-on calculator changes appear to have been reverted**
After successfully applying and testing the cast-on calculator edge case fix, a subsequent file read approximately 2 minutes later shows the original unfixed code. The earlier patch changed line 34 to handle nearestDown==0 case and made totalDown nullable at line 36, and tests passed confirming the fix worked. However, the file now shows the pre-fix state with non-nullable totalDown and simple nearestUp calculation. No git operations were observed that would explain this reversion. This suggests either a workspace issue, file system problem, unsaved changes, or manual revert by the user between tool executions.
~320t 🔍 3,462

**5381** 8:39a 🔵 **Cast-on calculator fix applied three times due to persistent file reversion**
Cast-on calculator edge case fix underwent three complete TDD cycles due to mysterious file reversions. First round at 09:36:33 successfully added test for pattern repeat > body stitches, saw it fail, implemented nullable totalDown fix, verified all tests pass. Git status confirmed both files modified. Second round at 09:38:24 began with files showing original unfixed code despite no observed revert operations; git status showed only AGENTS.md modified. Process repeated: test added, failed correctly (RED), fix applied, tests passed (GREEN). Third round at 09:41:25 started again with original code, repeated full cycle, achieved BUILD SUCCESSFUL. No tool executions show git operations, file overwrites, or other mechanisms that would explain the reversions. Each iteration proved the fix works identically.
~409t 🔍 36,028

**5378** 12:39p 🔴 **Cast-on calculator edge case fixed via TDD red-green cycle**
Cast-on calculator edge case successfully fixed following strict TDD methodology. The session wrote the failing test first for the scenario where pattern repeat (10 stitches) exceeds body stitches (4 from 2cm at 22st/10cm gauge). Test failed with AssertionError as expected, proving it catches the bug. Implementation then applied the fix: when nearestDown computes to zero, nearestUp uses patternRepeat directly instead of nearestDown + patternRepeat; totalDown becomes nullable via takeIf guard; adjustedDownWidth handles null totalDown. All 14 tests (13 original + 1 new) now pass. Git status confirms both files modified and ready to commit. This demonstrates proper red-green TDD cycle with verification before completion.
~351t 🛠️ 50,915

**5379** " 🔵 **Cast-on calculator inspection completed with no additional issues found**
Comprehensive inspection of cast-on calculator found no issues beyond the pattern-repeat edge case. Mathematical formulas are correct for both metric (10cm gauge reference) and imperial (4in) systems. Pattern repeat logic properly treats edge stitches as outside the repeat—snapping body stitches to nearest multiple, then adding edges on top. Validation follows the established pattern: NumberInputField filters input at component level, CastOnScreen validates at derivedStateOf layer (null check + range check), calculator assumes valid inputs. Terminology is accurate and consistent across all 11 supported languages. UI correctly handles nullable adjustedDown/adjustedUp/adjustedDownWidth fields using safe-call operators. The only gap is lack of UI-layer tests for CastOnScreen, though domain logic has comprehensive coverage with 14 unit tests.
~439t 🔍 50,915


Access 2391k tokens of past work via get_observations([IDs]) or mem-search skill.
</claude-mem-context>
