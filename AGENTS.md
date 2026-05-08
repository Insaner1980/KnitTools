# AGENTS.md - KnitTools

Keep this file aligned with `CODEX.md`. If one changes, update the other in the same change.

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


<claude-mem-context>
# Memory Context

# [KnitTools] recent context, 2026-05-09 12:25am GMT+3

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 22 obs (8,185t read) | 292,067t work | 97% savings

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
**5300** 9:56p 🔄 **AiVoiceAction Relocated from UI Layer to AI Package**
AiVoiceAction sealed class was refactored from the UI counter screen package (com.finnvek.knittools.ui.screens.counter) to a dedicated AI package (com.finnvek.knittools.ai). This architectural change better reflects the class's role as an AI/voice command domain model rather than UI-specific logic. Verification confirmed all old import references were successfully updated throughout the codebase with no orphaned references remaining. The VoiceCommandInterpreterTest suite validates the refactored code works correctly with all tests passing.
~308t 🛠️ 5,036

**5301** " 🔄 **Journal UI Components Relocated from AI Package to Notes Screen Package**
Journal feature components were refactored to better align with architectural boundaries. The Compose UI components (JournalEntryBottomSheet) and presentation logic (JournalEntryViewModel) were moved from the AI package to the UI notes screen package where they belong as user interface concerns. The AI-specific logic (JournalEntryProcessor and JournalProcessResult) remained in the ai/journal package, maintaining a clear separation between UI presentation and AI processing concerns. This change improves package cohesion by grouping UI components with their parent screen while keeping AI integration logic isolated in the appropriate domain package. All imports were updated across affected files, and project documentation (AGENTS.md, CODEX.md, memory/MEMORY.md) was synchronized to reflect the new architecture.
~431t 🛠️ 45,688

**5298** 9:58p 🔵 **Gradle daemon file lock prevented test execution**
Test execution failed during the ASM transformation phase because a Gradle daemon had locked the NotesEditorViewModelTest$vm$1.class file in the build intermediates directory. This is a common Windows issue where daemon processes hold file handles. The solution was to stop all Gradle daemons using ./gradlew --stop, which terminated 2 active daemons and released the file locks, allowing the test build to complete successfully on the next run.
~241t 🔍 9,032

**5299** " 🔵 **Voice command AI classes moved from counter UI package to dedicated ai package**
Voice command processing logic has been refactored from the counter UI package into a dedicated ai package. AiVoiceAction and VoiceCommandInterpreter are now properly separated from UI concerns. CounterViewModel has been updated with new imports to reference the relocated classes. Tests for VoiceCommandInterpreter have been created in the matching ai package test directory. However, CounterViewModel itself does not yet have unit tests, as evidenced by the test runner finding no matches for "*CounterViewModel*".
~299t 🔍 9,032

**5306** 10:50p 🔵 **KnitTools Room Entity Refactoring Baseline Established**
A phased Room entity refactoring was planned to align KnitTools Android app with clean architecture guidelines. The baseline assessment confirmed 8 Room entities spread across counter projects, sessions, photos, patterns, yarn cards, reminders, and annotations. These entities currently leak into UI ViewModels (CounterViewModel, ProjectListViewModel, LibraryViewModel) and domain logic (ProjectCounterLogic, ReminderLogic). The gradle test baseline established 5 pre-existing TrialManagerTest failures unrelated to entity work. No mapper infrastructure exists - the codebase has toEntity functions only for UI form-to-entity conversion in YarnCardViewModel. The refactoring will introduce domain models in domain/model/ with entity-to-domain bidirectional mappers in data/local/, keeping Room entity field names and database schema unchanged. Test directory structure for mapper tests was created at app/src/test/java/com/finnvek/knittools/data/local/.
~471t 🔍 69,415


Access 292k tokens of past work via get_observations([IDs]) or mem-search skill.
</claude-mem-context>
