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
- `pc` runs PMD CPD duplicate detection with KnitTools' default `PMD_CPD_MINIMUM_TOKENS=100`, `cr` runs compose-rules through ktlint/detekt, `ga` runs Android Lint with Google Android Security Lints, and `cs` is available for Compose Stability Analyzer projects.
- `sc` runs dependency, secret, and light Semgrep checks; `sc -Full` also runs the Android-specific `ac` path and DeepSec custom report
- Typical commands: `./gradlew assembleDebug`, `./gradlew test`, `./gradlew :app:detekt`, `./gradlew lint`
- Do not run the user's wrapper scripts such as `lc` or `sc`
- Never commit generated `reports/`


<claude-mem-context>
# Memory Context

# [KnitTools] recent context, 2026-05-18 1:52am GMT+3

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 50 obs (18,165t read) | 3,493,434t work | 99% savings

### Apr 30, 2026
S448 Investigating lint-check script failures on Windows and discovering hidden Android Lint issues (Apr 30, 11:19 PM)
### May 4, 2026
S679 Document KnitTools app online/offline capabilities after discovering user's assumption about voice commands was incorrect (May 4, 4:12 PM)
### May 8, 2026
5298 9:58p 🔵 Gradle daemon file lock prevented test execution
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
5371 8:09a 🔵 Windows Sandbox Blocks Skill File Access with Permission Error
5373 " 🔵 Cast-on calculator edge case identified via TDD
5374 " 🔴 Fixed cast-on calculator to handle pattern repeat larger than body stitches
5375 " 🔵 Cast-on calculator validation analysis shows consistent pattern
5376 8:16a 🔵 Cast-on calculator changes appear to have been reverted
5381 8:39a 🔵 Cast-on calculator fix applied three times due to persistent file reversion
5378 12:39p 🔴 Cast-on calculator edge case fixed via TDD red-green cycle
5379 " 🔵 Cast-on calculator inspection completed with no additional issues found
5382 1:14p 🟣 CodeQL workflow restored for KnitTools project
5383 " 🔴 Fixed Gradle dependency verification failures in CI
5384 " 🔴 Git worktree used for safe main branch operations
5385 1:32p 🟣 CodeQL Security Scanning Restored for KnitTools Android Project
5386 " 🔴 Platform-Specific Gradle Dependency Verification Fixed for CI
5387 " 🔴 ktlint Code Style Violations Fixed in Three UI Screen Files
5388 " 🔴 Git Worktree Used for Safe Main Branch Operations
5389 2:23p 🟣 CodeQL workflow restored for KnitTools CI security scanning
5390 " 🔴 Dependency verification extended with 18 missing artifacts for CI compatibility
5391 " 🔴 Ktlint formatting violations fixed for CI compliance
5392 " 🔵 dBcheck CodeQL configuration pattern identified for Android projects
5394 4:40p 🔴 Fixed session tracking accuracy and lifecycle handling
5396 7:40p 🔵 Pace calculation logic verified with comprehensive edge-case guards
### May 18, 2026
**5398** 12:25a ✅ **Disabled ossIndex analyzer in OWASP dependency-check configuration**
The KnitTools project uses multiple security scanning tools: osv-scanner for comprehensive vulnerability detection and OWASP dependency-check for additional analysis. The ossIndex analyzer within dependency-check was creating redundant checks since osv-scanner already performs comprehensive CVE scanning against the same vulnerability databases. To streamline the security pipeline and eliminate duplicate reporting, the ossIndex analyzer was disabled in the dependency-check Gradle configuration. This change does not reduce security coverage—osv-scanner continues to identify all CVEs in dependencies like the logback-core arbitrary code execution vulnerability and Bouncy Castle timing channel issues. The dependency-check tool now focuses on its other analyzers (retirejs already disabled, others remain active) while osv-scanner handles the primary CVE detection workload.
~389t 🛠️ 43,908

**5400** 12:28a 🔵 **OSV-scanner scans verification-metadata.xml containing build-time dependencies not in runtime classpath**
Investigation of osv-scanner FAILED (1) exit code revealed that osv-scanner scans gradle/verification-metadata.xml as a lockfile, which captures all Gradle dependency resolution metadata including buildscript classpath, test dependencies, and plugin dependencies—not just the app's runtime dependencies. The scan found CVEs in logback-core 1.3.14 (arbitrary code execution), netty packages (multiple CVEs), bouncycastle 1.79 (LDAP injection, timing channel), jdom2 2.0.6 (XXE), commons-lang3 3.16.0, and httpclient 4.5.6. However, running `./gradlew :app:dependencyInsight` on these packages confirmed they are NOT present in the app's debugRuntimeClasspath—they exist only in the verification metadata from build-time resolution. The only runtime vulnerability found is guava 31.0.1-jre (CVE exposing potential security issues), which enters the runtime classpath transitively: ML Kit GenAI Prompt → kotlinx-coroutines-guava:1.10.2 → guava:31.0.1-jre. This explains why osv-scanner reports many more vulnerabilities than are actually exploitable in the shipped app—it's scanning the complete Gradle dependency graph, not just the runtime attack surface.
~492t 🔍 31,199

**5403** " 🔴 **OSV scanner failure fixed by upgrading Guava and filtering verification metadata**
The security-check pipeline was failing because osv-scanner found vulnerabilities in gradle/verification-metadata.xml. The root cause was kotlinx-coroutines-guava transitively pulling in Guava 31.0.1-jre, which had known CVEs. Additionally, OSV was incorrectly treating the verification metadata file (which contains checksums for all build-time dependencies) as a runtime dependency lockfile.

The fix applied a two-part solution: (1) Added a dependency constraint in app/build.gradle.kts forcing Guava to upgrade to 33.5.0-android, which resolved the runtime vulnerability; (2) Created gradle/osv-scanner.toml with a PackageOverrides rule to ignore verification-metadata.xml, since it's checksum metadata not a runtime dependency source.

This pattern follows the same approach documented in the msgtap rollout summary from 2026-05-16, where the same OSV filtering issue was resolved. The fix ensures that OSV only scans actual runtime dependencies while OWASP dependency-check handles the full build dependency CVE scanning.

Verification with dependencyInsight confirmed both debug and release configurations now resolve Guava to 33.5.0-android via conflict resolution. OSV scanner now exits cleanly with 0 vulnerabilities found after filtering 1164 build-time packages from the verification metadata.
~549t 🛠️ 157,782

**5407** " 🚨 **OSV Scanner Detected Vulnerability in KnitTools Dependencies**
A comprehensive security dependency check was executed on the KnitTools project at C:\Dev\KnitTools. Three security tools were run: gradle dependency verification (passed), osv-scanner (failed with 1 vulnerability), and OWASP dependency-check (passed). The osv-scanner failure indicates at least one known security vulnerability exists in the project's dependencies that requires investigation and remediation. Results are available in the reports directory for detailed analysis.
~228t 🚨 7,513

**5408** " 🔴 **OSV scanner now passes by filtering Gradle verification metadata**
The security-check pipeline for KnitTools was failing because OSV scanner treated gradle/verification-metadata.xml as a dependency lockfile and reported CVE findings for all Maven artifacts listed there, including Guava 31.0.1-jre with multiple known vulnerabilities. The fix involved two parallel changes: (1) instructing OSV scanner to ignore verification metadata via gradle/osv-scanner.toml since runtime dependencies are checked by OWASP dependency-check instead, and (2) forcing an upgrade to Guava 33.5.0-android by adding a dependency constraint that overrides the transitive 31.0.1-jre version brought in by kotlinx-coroutines-guava. The verification metadata was updated with checksums for the new Guava artifacts (.jar, .module, .pom). OSV scanner now completes cleanly with 0 results, though OWASP dependency-check verification continues to time out when run with the updated dependency tree.
~478t 🛠️ 171,510


Access 3493k tokens of past work via get_observations([IDs]) or mem-search skill.
</claude-mem-context>
