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
- Keep business logic out of composables when a ViewModel or use case should own it
- `ai/AiVoiceAction` is the single AI voice action contract; keyword-only voice commands stay in the counter UI voice handler
- Journal UI and `JournalEntryViewModel` live under `ui/screens/notes`; `ai/journal` owns only journal AI processing and result models
- PDF rendering lives in `data/storage/PdfPageRenderer`; pattern UI should not define renderer copies

## Navigation Rules

- Top-level tabs are `Projects`, `Library`, `Tools`, `Insights`, `Settings`
- `TopLevelDestination` in [Screen.kt](/home/emma/dev/KnitTools/app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt) is the source of truth
- `CounterViewModel` is shared at the Projects graph level
- `LibraryViewModel` is shared at the Library graph level
- Widget counter launches carry a `CounterLaunchRequest.requestId`; `MainActivity` clears consumed launch extras and saves the consumed id across recreation
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

# [KnitTools] recent context, 2026-05-09 3:28am GMT+3

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 29 obs (10,976t read) | 879,963t work | 99% savings

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
**5307** 2:50a 🔵 **Baseline profile module minSdk lower than app module minSdk**
SDK verification revealed a minSdk mismatch between the app and baselineprofile modules in the KnitTools Android project. The app module targets Android 10+ (minSdk 29) while the baseline profile test module targets Android 9+ (minSdk 28). Although Android Gradle Plugin typically allows test modules to have a lower minSdk for broader device compatibility during profiling, this can cause instrumentation test failures or unexpected behavior when the baseline profile generation tries to install on devices between API 28-29. Both modules consistently use compileSdk 36 (Android 14) and targetSdk 36. The dependency analysis confirmed all major libraries (Firebase AI, ML Kit, Glance widgets, Play Billing) are properly declared in gradle/libs.versions.toml and consumed through the version catalog pattern.
~355t 🔍 30,833

**5310** " 🔵 **Baseline Profile Module Has Lower minSdk Than App Module**
The KnitTools Android project has a minSdk mismatch between the app module (minSdk=29) and the baselineprofile test module (minSdk=28). The baselineprofile module generates performance profiles by running instrumented tests against the app, so it should match the app's minimum SDK requirements. The discrepancy was discovered by examining both build.gradle.kts files and verifying the merged AndroidManifest.xml files in build/intermediates. Both modules correctly use compileSdk=36 and targetSdk=36. The fix is to change baselineprofile/build.gradle.kts minSdk from 28 to 29 to match the app module.
~303t 🔍 115,358

**5311** " 🔵 **All AndroidX and Firebase Dependencies Compatible with SDK 36**
Dependency insight verification confirmed all critical AndroidX and Firebase libraries are compatible with the project's SDK configuration (compileSdk 36, targetSdk 36, minSdk 29). Firebase AI uses the google-ai backend and provides generativeModel and liveModel APIs. ML Kit GenAI Prompt provides on-device Gemini Nano capabilities via Generation.getClient(). Glance 1.1.1 powers home screen widgets. Play Billing 8.3.0 handles in-app purchases. The project uses Gradle dependency constraints to align transitive dependencies (e.g., Ktor versions from Firebase AI, kotlinx.serialization for Room 2.8.x). No compatibility issues were found with these SDK levels.
~388t 🔍 115,358

5313 " 🔵 SDK Configuration and Library Compatibility Verification Complete
5318 " ⚖️ Baselineprofile minSdk=28 and Library Version Strategy Decisions
**5319** 2:56a ✅ **Updated baselineprofile minSdk to match app minimum SDK level**
The baselineprofile module minSdk was increased from 28 to 29 to match the main app module's minimum SDK level. While the original minSdk=28 did not cause build failures or runtime issues (baseline profile is a development tool, not a shipped component), this change creates consistency across modules. The baselineprofile module is used to generate Baseline Profiles that optimize app startup and runtime performance by pre-compiling critical code paths (Compose, navigation, counters). After the change, Gradle verification tasks confirmed AAR metadata compatibility remains intact for both benchmarkRelease and nonMinifiedRelease variants.
~281t 🛠️ 100,783

**5320** " ✅ **Updated Firebase BoM and ML Kit GenAI Prompt to newer releases**
Updated dependency versions for Firebase and ML Kit AI libraries to their latest releases. The Firebase BoM (Bill of Materials) upgrade from 34.12.0 to 34.13.0 brings firebase-ai from 17.11.0 to 17.12.0, which the app uses for cloud-based Gemini AI features (GeminiAiService calls Firebase.ai().generativeModel() and liveModel()). The ML Kit GenAI Prompt API update from beta1 to beta2 affects on-device AI functionality (NanoAvailability checks Generation.getClient().checkStatus()). Both are patch/beta updates likely containing bug fixes and improvements. Dependency resolution verified successfully through Gradle insight tasks, and AAR metadata checks passed, confirming compatibility with compileSdk=36 and AGP 9.1.0.
~340t 🛠️ 100,783


Access 880k tokens of past work via get_observations([IDs]) or mem-search skill.
</claude-mem-context>
