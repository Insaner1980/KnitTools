# KnitTools Product and Engineering Reference

## Scope and verified source snapshot

This is the product and technical reference for the **source working tree inspected on November 10, 2026 (Europe/Helsinki)**. It describes what the application contains, how its UI and data operations work, and where to investigate behavior when writing code-review questions, tests or UI changes. Current source, configuration and exported schemas are authoritative; plans and historical test receipts are secondary evidence. This snapshot includes existing uncommitted implementation, test and documentation changes; checking out the HEAD below alone does not reproduce it.

This update checks the current implementation against the previous reference. It is a documentation/source verification, not a new application build, runtime test run, security audit or release certification. A test named below is existing test source unless execution is explicitly identified.

All repository links are relative to the root. Package shorthand such as `repository/CounterRepository.kt` means a path below `app/src/main/java/com/finnvek/knittools`. “Implemented” means present and reachable subject to state, validation and entitlement; it does not mean released, live-service verified or freshly runtime-tested.

| Snapshot item | Inspected value |
| --- | --- |
| Repository | `C:\Dev\KnitTools` |
| Branch | `codex/paikalliset-muutokset-20261109` |
| Source HEAD before this documentation edit | `7aef2e0e0bd3540c45b5e952e3d6133f49e02de4` |
| Verification date | November 10, 2026, Europe/Helsinki |
| Starting working tree | Dirty: existing tracked changes, including `PROJECT.md`, plus five untracked Kotlin test helpers; preserved |
| Starting index | No staged changes |
| Documentation output | Root `PROJECT.md` only |
| Application ID / namespace | `com.finnvek.knittools` |
| App version | `1.0.0`, version code `1` |
| Android SDK | Minimum 29; target and compile 37 |
| Room schema | 28 |
| Current database shape | 19 entities/tables, 214 columns; 28 exported schema versions |
| Supported app languages | 12 locale options; System is a selection mode |
| Resource inventory | 1,597 translatable strings and 34 plurals per locale; 1,631 names per locale across all text-resource files |
| Modules | `:app` and `:baselineprofile` |
| Main Android inventory | 457 Kotlin files under `app/src/main`; variant-specific implementation is additional |
| Test source inventory | 387 Kotlin files under `app/src/test` (385 tracked + 2 untracked), 1 under `testDebug`, 1 under `sharedTest`; 206 Kotlin (203 tracked + 3 untracked) and 2 Java files under `androidTest` |
| Baseline/driver source inventory | 6 Kotlin files under `baselineprofile/src`, including the external optimized-runtime smoke driver |
| Functions inventory | 33 TypeScript files including 14 test files; retained backend with no Android consumer |

Source counts include tracked files and explicitly identified nonignored untracked files in this working tree, including helpers/fixtures; they are not counts of test methods or successful executions. The main source set also contains code whose behavior depends on the build variant, so its file count is not a release-reachability metric. Ignored build/report artifacts are dated evidence, not authoritative production sources. Configuration names are documented without copying local credentials or tokens.

### Reading this reference for reviews, tests and UI work

For a feature's current behavior, start with its product chapter and follow the linked implementation. For a review, also read the canonical call chains, persistence constraints and ownership checklist. For tests, use the feature-to-test map and distinguish pure logic, fake-repository assertions, Android SQLite, Compose interaction and live-service evidence. For UI work, read navigation, the visual system, screen composition and the onboarding chapter together: the application has both global tab navigation and context-specific project/reader/modal state.

Terms such as *must*, *not allowed* and *future work* describe repository contracts or explicit boundaries. They are not evidence that an external service is configured, a test has run, or a proposed feature is implemented. Source-observed limitations are called out next to the relevant workflow.

| Intended use | Read together | Result to produce |
| --- | --- | --- |
| Code-review questions | Relevant feature chapter, [canonical call chains](#canonical-call-chains), [ownership checklist](#ownership-checklist-for-code-changes), [regression scenarios](#review-questions-and-regression-scenarios) | A concrete trigger, expected invariant, owning source and realistic failure path; a question is not a confirmed finding |
| Test design or a regression fix | Relevant feature, [state lifetimes](#state-lifetime-and-recovery-ownership), [test ownership](#feature-to-test-ownership-map), [verification selection](#verification-selection) | The smallest applicable test level and observable result, including failure/cancellation where material |
| UI or UX changes | [Navigation](#navigation-and-external-entry-points), [visual system](#visual-system-and-accessibility), [screen composition](#screen-composition-and-interaction-reference), [guidance](#first-launch-guidance-and-the-bundled-guide), [CLAUDE.md](CLAUDE.md) | Entry/exit flow, state-specific presentation, reused component/token and localized actions |
| Database or backup work | [Persistence](#persistence-reference), [backup](#manual-backup-and-replacement-restore), [file ownership](#files-pdf-processing-and-cleanup) | Schema/migration, transaction, identity/reference and restore-compatibility consequences |
| Security or privacy review | [Manifest/network boundaries](#security-privacy-and-telemetry), [Ravelry reachability](#reachability-and-security-matrix), [release admission](#variants-isolated-tests-and-release-admission) | Actual reachable variant/surface, trusted and untrusted inputs, data destination and evidence limits |
| Localization or accessibility | [Localization](#localization), [responsive behavior](#responsive-layout-semantics-focus-and-feedback), owning component/resource tests | Resource/plural/format/semantics changes and appropriate validation; respect the explicit restriction on repeating prior device configurations |
| Build, release or toolchain questions | [Toolchain](#toolchain-dependencies-and-build-variants), [CI](#ci-analyzers-and-release-configuration), [current limitations](#supported-limits-and-unresolved-verification) | Configured behavior separated from executed checks, artifact identity and live-service status |

When using this document as context for another review or implementation request, include the affected feature and current diff. State whether the desired output is questions, a read-only audit, a test plan, or an implementation. Require each proposed finding to name the reachable trigger, expected versus actual behavior, owning source and evidence. For tests, distinguish assertions that exist from tests that actually executed. For UI, specify the route, state and intended user action before proposing layout changes. Do not turn historical plans, source-only observations or stronger hypothetical designs into requirements.

### Evidence and investigation boundaries

The November 10 refresh reconciles current local changes with this existing reference. Direct inspection focuses on repository instructions, PDF intent consumption/restoration and stale actions, guidance logging, navigation/layout/type and Pro-state owners, build variants, CI, retained backend refresh handling and test fixtures. Existing detailed feature sections remain the broader reference; this refresh is not a new line-by-line audit of every implementation. Schema metadata, file/directory links, heading anchors, declared versions and resource/test inventories receive mechanical consistency checks.

Only `PROJECT.md` is changed. Deterministic `sonar analyze secrets` checks precede workspace content reads. No application build, JVM/instrumented/backend test suite, device/emulator operation, hosted analysis upload, commit, push, pull request, deployment or release is performed for this update. Historical execution evidence has its own provenance and limitations.

The user-facing feature chapters come first. Navigation and design explain how those workflows are presented; architecture and the persistence dictionary explain ownership; security, tooling, test evidence and development rules explain how to change the project safely.

## Contents

- [Scope and verified source snapshot](#scope-and-verified-source-snapshot)
  - [Reading this reference for reviews, tests and UI work](#reading-this-reference-for-reviews-tests-and-ui-work)
  - [Evidence and investigation boundaries](#evidence-and-investigation-boundaries)
- [Product and feature behavior](#product-and-feature-behavior)
  - [Product model and everyday workflows](#product-model-and-everyday-workflows)
  - [Feature availability at a glance](#feature-availability-at-a-glance)
- [First launch, guidance and the bundled guide](#first-launch-guidance-and-the-bundled-guide)
  - [Eight-step onboarding journey](#eight-step-onboarding-journey)
  - [Starter-project setup and the compact Projects start state](#starter-project-setup-and-the-compact-projects-start-state)
  - [Contextual tips and committed celebrations](#contextual-tips-and-committed-celebrations)
  - [Offline guide and contextual actions](#offline-guide-and-contextual-actions)
- [Projects and organization](#projects-and-organization)
  - [Projects, folders, sorting, and selection](#projects-folders-sorting-and-selection)
    - [Completion and deletion boundaries](#completion-and-deletion-boundaries)
- [Counting, reminders and notes](#counting-reminders-and-notes)
  - [Main counter, target, history, and Undo](#main-counter-target-history-and-undo)
    - [Main-counter step size: presets and custom amounts](#main-counter-step-size-presets-and-custom-amounts)
    - [Step-size state and asynchronous ownership](#step-size-state-and-asynchronous-ownership)
  - [Secondary, extra, repeating, shaping, and repeat-section counters](#secondary-extra-repeating-shaping-and-repeat-section-counters)
  - [Stitch tracking and row reminders](#stitch-tracking-and-row-reminders)
  - [Local notes and concurrent editors](#local-notes-and-concurrent-editors)
- [Sessions, timing and recovery](#sessions-timing-and-recovery)
  - [Work sessions and history](#work-sessions-and-history)
    - [Time anchors and recovery decisions](#time-anchors-and-recovery-decisions)
- [Patterns, project documents and PDF workflows](#patterns-project-documents-and-pdf-workflows)
  - [Library, patterns, and project documents](#library-patterns-and-project-documents)
  - [PDF reading, annotations, and creation](#pdf-reading-annotations-and-creation)
  - [Rendering, mapping, and annotation budgets](#rendering-mapping-and-annotation-budgets)
  - [Markup and chart interaction details](#markup-and-chart-interaction-details)
    - [Editing existing text annotations](#editing-existing-text-annotations)
  - [Annotated export budgets and failure behavior](#annotated-export-budgets-and-failure-behavior)
  - [Import and cleanup decision points](#import-and-cleanup-decision-points)
- [Yarn, usage and progress photos](#yarn-usage-and-progress-photos)
  - [Photos and yarn](#photos-and-yarn)
    - [Usage revisions and failure results](#usage-revisions-and-failure-results)
    - [Photo commit and cancellation boundaries](#photo-commit-and-cancellation-boundaries)
  - [Image creation and photo replacement](#image-creation-and-photo-replacement)
- [Calculators and reference library](#calculators-and-reference-library)
  - [Calculators and reference material](#calculators-and-reference-material)
    - [Interpretation of calculation results](#interpretation-of-calculation-results)
- [Insights and activity](#insights-and-activity)
  - [Insights](#insights)
  - [Insights resource and consistency evidence](#insights-resource-and-consistency-evidence)
- [Widgets, preferences and Android services](#widgets-preferences-and-android-services)
  - [Widgets, preferences, help, reviews, and updates](#widgets-preferences-help-reviews-and-updates)
- [Current Ravelry integration](#current-ravelry-integration)
  - [Reachable Android behavior](#reachable-android-behavior)
  - [Navigation, settings, and accepted boundary](#navigation-settings-and-accepted-boundary)
  - [PDF downloads into Save Pattern](#pdf-downloads-into-save-pattern)
  - [Retained backend, disconnected Android integration, and future status](#retained-backend-disconnected-android-integration-and-future-status)
  - [Reachability and security matrix](#reachability-and-security-matrix)
- [Pro, trial, and billing](#pro-trial-and-billing)
  - [Entitlement model and feature gates](#entitlement-model-and-feature-gates)
  - [Pro presentation and trial journey](#pro-presentation-and-trial-journey)
  - [Trial persistence and clock handling](#trial-persistence-and-clock-handling)
  - [Google Play purchase and restore](#google-play-purchase-and-restore)
  - [Entitlement enforcement and continued access](#entitlement-enforcement-and-continued-access)
- [Manual backup and replacement restore](#manual-backup-and-replacement-restore)
  - [Workflow and archive contract](#workflow-and-archive-contract)
  - [Budgets and validation](#budgets-and-validation)
  - [Export snapshot and provider IO](#export-snapshot-and-provider-io)
  - [Identity rebasing and replacement transaction](#identity-rebasing-and-replacement-transaction)
  - [Drive, Dropbox, and future synchronization](#drive-dropbox-and-future-synchronization)
  - [Backup implementation ownership](#backup-implementation-ownership)
- [Navigation and external entry points](#navigation-and-external-entry-points)
  - [Graphs and route inventory](#graphs-and-route-inventory)
  - [Back stack, visibility, and ownership](#back-stack-visibility-and-ownership)
  - [Activity intents and widget launch](#activity-intents-and-widget-launch)
  - [Incoming PDF identity, restoration and stale actions](#incoming-pdf-identity-restoration-and-stale-actions)
  - [Route contracts and transient presentation](#route-contracts-and-transient-presentation)
- [Visual system and accessibility](#visual-system-and-accessibility)
  - [Identity and theme ownership](#identity-and-theme-ownership)
  - [Palette and semantic colors](#palette-and-semantic-colors)
  - [Typography, shapes, dimensions, and assets](#typography-shapes-dimensions-and-assets)
  - [Shared component contracts and screen hierarchy](#shared-component-contracts-and-screen-hierarchy)
  - [Responsive layout, semantics, focus, and feedback](#responsive-layout-semantics-focus-and-feedback)
  - [Screen composition and interaction reference](#screen-composition-and-interaction-reference)
- [Architecture and lifetime](#architecture-and-lifetime)
  - [Layers and canonical ownership](#layers-and-canonical-ownership)
  - [Startup and activity lifetime](#startup-and-activity-lifetime)
  - [Coroutines, Flow, state restoration, and concurrency](#coroutines-flow-state-restoration-and-concurrency)
  - [Canonical call chains](#canonical-call-chains)
  - [State lifetime and recovery ownership](#state-lifetime-and-recovery-ownership)
- [Persistence reference](#persistence-reference)
  - [Room ownership and constraints](#room-ownership-and-constraints)
  - [Complete table dictionary](#complete-table-dictionary)
    - [counter_projects](#counter_projects)
    - [counter_history](#counter_history)
    - [yarn_cards](#yarn_cards)
    - [sessions](#sessions)
    - [active_sessions](#active_sessions)
    - [row_reminders](#row_reminders)
    - [progress_photos](#progress_photos)
    - [project_counters](#project_counters)
    - [project_yarn_notes](#project_yarn_notes)
    - [saved_patterns](#saved_patterns)
    - [pattern_annotation_layers](#pattern_annotation_layers)
    - [pattern_annotations](#pattern_annotations)
    - [pattern_bookmarks](#pattern_bookmarks)
    - [project_documents](#project_documents)
    - [project_folders](#project_folders)
    - [project_folder_assignments](#project_folder_assignments)
    - [project_yarn_usage](#project_yarn_usage)
    - [project_completions](#project_completions)
    - [onboarding_projects](#onboarding_projects)
  - [Migration chain](#migration-chain)
  - [Preferences and other persisted state](#preferences-and-other-persisted-state)
  - [Column semantics and weak relationships](#column-semantics-and-weak-relationships)
- [Files, PDF processing, and cleanup](#files-pdf-processing-and-cleanup)
  - [Ownership and provider surface](#ownership-and-provider-surface)
  - [Deletion and reference safety](#deletion-and-reference-safety)
- [Security, privacy, and telemetry](#security-privacy-and-telemetry)
  - [Android manifest and external surface](#android-manifest-and-external-surface)
  - [Local content, network, and secret boundaries](#local-content-network-and-secret-boundaries)
  - [Release Crashlytics](#release-crashlytics)
  - [Optional release usage analytics](#optional-release-usage-analytics)
  - [Debug-only Sentry](#debug-only-sentry)
  - [Accepted limits and security evidence](#accepted-limits-and-security-evidence)
  - [App manifest versus dependency-merged manifest](#app-manifest-versus-dependency-merged-manifest)
  - [Network and data boundary map](#network-and-data-boundary-map)
  - [Secret configuration and release obligations](#secret-configuration-and-release-obligations)
- [Localization](#localization)
  - [Languages and selection](#languages-and-selection)
  - [Formatting, units, and terminology](#formatting-units-and-terminology)
  - [Resource and input conventions](#resource-and-input-conventions)
- [Toolchain, dependencies, and build variants](#toolchain-dependencies-and-build-variants)
  - [Repository/module structure and Android build](#repositorymodule-structure-and-android-build)
  - [Dependency catalog](#dependency-catalog)
  - [Variants, isolated tests, and release admission](#variants-isolated-tests-and-release-admission)
  - [Baseline Profile and backend build](#baseline-profile-and-backend-build)
  - [Compatibility pins, generated outputs, and release artifacts](#compatibility-pins-generated-outputs-and-release-artifacts)
- [CI, analyzers, and release configuration](#ci-analyzers-and-release-configuration)
  - [GitHub configuration](#github-configuration)
  - [Direct checks and wrapper ownership](#direct-checks-and-wrapper-ownership)
  - [Analyzer semantics and exception inventory](#analyzer-semantics-and-exception-inventory)
  - [DeepSec and release-surface checks](#deepsec-and-release-surface-checks)
  - [Configured execution versus current readiness](#configured-execution-versus-current-readiness)
- [Testing and evidence](#testing-and-evidence)
  - [Current test architecture](#current-test-architecture)
  - [Feature-to-test ownership map](#feature-to-test-ownership-map)
  - [Review questions and regression scenarios](#review-questions-and-regression-scenarios)
  - [Turning a review question into a test or UI task](#turning-a-review-question-into-a-test-or-ui-task)
  - [Text annotation editing validation](#text-annotation-editing-validation)
  - [Existing execution receipts and their source limits](#existing-execution-receipts-and-their-source-limits)
  - [Documentation-specific verification](#documentation-specific-verification)
- [Development and review invariants](#development-and-review-invariants)
  - [Workflow and source authority](#workflow-and-source-authority)
  - [Ownership checklist for code changes](#ownership-checklist-for-code-changes)
  - [Verification selection](#verification-selection)
  - [How to choose the source of truth](#how-to-choose-the-source-of-truth)
  - [Repository source map](#repository-source-map)
- [Current status, limitations, and document authority](#current-status-limitations-and-document-authority)
  - [Implemented and reachable](#implemented-and-reachable)
  - [Retained, partial, historical, and planned](#retained-partial-historical-and-planned)
  - [Supported limits and unresolved verification](#supported-limits-and-unresolved-verification)
  - [Related documents and authority](#related-documents-and-authority)
  - [Reference maintenance and verification boundary](#reference-maintenance-and-verification-boundary)

## Product and feature behavior

### Product model and everyday workflows

KnitTools is a local-first Android knitting/crochet companion. A project connects counting, pattern documents/web metadata, notes, yarn and usage, progress photos, reminders, saved work sessions and completion history. There is no KnitTools account or content-sync backend. The current Ravelry integration is its website in an in-app browser; retained Firebase API code is a separate, disconnected package.

The five top-level tabs are **Projects, Library, Tools, Insights and Settings**. Projects is the ordinary start graph. First launch can show the eight-step onboarding journey before that graph; the compact empty Projects state and its local demo remain separate. Initial loading is explicit: default empty values are not proof that no projects or entitlement exist.

| Everyday workflow | User path | Important distinction |
| --- | --- | --- |
| Start or resume work | New Project → Counter; project card → overview; Continue button → Counter | Opening/navigating does not start a timed session |
| Keep counting context | Main +/−/Reset/Undo, target, compact secondary, stitches and extra counters | Each kind has its own stored state/rules; actual writes go through repositories |
| Organize a project | Overview sections and overflow actions | Overview shares Projects-scoped Counter state and the existing content editors |
| Read a pattern | Import/share/download PDF or attach a Library PDF → project reader | Metadata-only web/Ravelry links do not become readable PDFs |
| Record time | Explicit Start → persisted active session → Stop/review → saved session | Background correctness uses Room time anchors, not a background ticker |
| Keep yarn information | Project notes/links and independent planned/allocated/used amounts; Library My Yarn | Project usage never automatically deducts global stash quantity |
| Use tools/references | Tools calculators; Library reference material | Calculations/reference lookup are local; Copy/paste and Ravelry have explicit separate boundaries |
| Review activity | Insights range/project filters and session/completion history | Historical row-unit/pace output is unavailable without a historical unit snapshot |
| Keep a backup | Settings → Backup & Restore → SAF export or confirmed replacement | Restore replaces local content; it is neither merge nor automatic sync |

Project creation is immediately usable at count zero. Optional setup suggestions, contextual tips and the offline guide help users discover features without creating required setup tasks. Free use, optional user-started trial and one-time Pro purchase share one content model; creation/admission gates and access to existing content are documented separately below.

### Feature availability at a glance

| User capability | Where it lives | Current implementation boundary |
| --- | --- | --- |
| Projects, craft/unit labels, folders, sorting, completion/reopening | Projects / project overview | Local Room data; one free active-project admission rule |
| Main count, target, history and Undo | Counter / Counter History | Atomic repository changes; target does not cap counting |
| Secondary, extra, repeating, shaping and repeat-section counters | Counter and its content/actions sheets | Creation gates vary; persisted counters remain distinct from entitlement |
| Stitch tracking and row reminders | Counter | Stitches reset with a successful row change; reminders are in-app, not scheduled notifications |
| Start/Stop timing and recovery | Counter / Session History | One persistent active session; reviewed recovery for untrusted time |
| Saved PDFs, website metadata, retained Ravelry rows | Library / Saved Pattern detail | Local metadata and private PDFs; no current API import client |
| Multiple project PDFs and independent reader state | Project overview / PDF reader documents sheet | `project_documents` owns relations, ordering and reader state |
| Markup, chart regions/trackers, bookmarks and guides | PDF reader | Normalized geometry; bounded operations; project/master layer distinction |
| Photo-to-PDF creation | Pattern picker / image import flow | Camera/gallery staging and bounded conversion; no OCR |
| Progress images | Project Photos / Library All Photos | Private images associated with project/count/note |
| Yarn cards, project yarn notes and amounts | Library My Yarn / project Yarn sheet | Separate metadata, reciprocal links and canonical meter usage |
| Practical calculations and reference tables | Tools / Library Reference | Local arithmetic/data; selected English regex paste |
| Statistics and activity visualization | Insights | Session-zone-aware calculations, completion event count, gated advanced presentation |
| Home-screen counter | Glance widget | Shared active project and canonical counter writes; Pro gate |
| Preferences and portability | Settings | Per-app language, visual/interaction settings, manual replacement backup |
| Trial and one-time Pro purchase | Pro Upgrade / contextual prompts / Settings | User-started local 14-day trial and Play INAPP product |
| Ravelry website browsing and selected PDF download | Global in-app browser | Website login/cookies, HTTPS download into Save Pattern |

The backend API, OAuth callbacks, old native Ravelry search/detail/import screens, cloud synchronization, voice processing, and AI-powered parsing are not currently reachable Android features. Retained code and future specifications are separated later in this reference.

## First launch, guidance and the bundled guide

### Eight-step onboarding journey

The first-launch journey is a full-screen `OnboardingScreen` before the ordinary Projects UI. `OnboardingViewModel.loadDraft` creates a draft only when no stored draft, dismissed welcome or existing project makes that inappropriate. Existing users are determined from **all** projects, including completed projects and projects hidden by folder filters. External PDF/share/counter requests have their own startup priority; the journey must not consume them as a demonstration action.

| Step in `OnboardingStep` | User-visible behavior | State and correctness boundary |
| --- | --- | --- |
| `Welcome` | KnitTools stamp, craft illustration, introduction and three benefits | Progress indicator is omitted on this step; illustration obeys available height |
| `Craft` | Illustrated Knitting, Crochet and Both choices | A tap selects and advances; Both creates knitting projects unless the project form changes the craft |
| `Needs` | Select what the app should remember: counting, pattern, returning, yarn or projects | Multiple selections preserve selection order; wording follows craft; these answers personalize presentation, not feature access |
| `Counter` | Working local +/−/Undo demo over a three-row pattern with a following reading line | `demoPatternRow(count)` is zero-based `floorMod(count, 3)`; two successful positive demo changes reveal a return card; no real count/session/history write |
| `Toolbox` | Two-column feature grid with feature names, cross-stitch icons and benefit text | `pickedJourneyTools` comes first in Needs selection order; Counting/Returning map to Counter, Projects to Insights; highlighted tiles are recommendations, not checkboxes; three icon details animate once |
| `Project` | Empty name field with a localized example, craft, starting count, optional target/PDF | Default starting count is zero; demo count is never copied; shared project validation and repository admission apply |
| `Ready` | Created project's name, next count and optional PDF name between hairlines | Available only after committed creation; no return to the form; Back and Continue advance to Price |
| `Price` | Shared Pro benefits/free content, confirmed offer, optional trial, purchase or free continuation | `onboardingPriceMode` decides Loading/Trial/Purchase/Continue/Skip from loaded facts; unavailable sales must not trap the user |

`OnboardingDraft` persists ID, step, ordered Needs choices, craft and explicit-choice state, demo count/history/increment tally, raw form text, options visibility, copied PDF URI/name, created project ID, completion and pending-exit state in **`onboarding_state` DataStore**. Demo history keeps the last 100 states and uses the project's 99,999 start-count ceiling. Undo does not decrease the tally that revealed the return example. Back is permitted only from Craft through Project. Skip goes to Price; leaving Price continues into the app.

`CounterRepository.createOnboardingProject` performs project creation, optional document attachment and the unique draft receipt in **one Room transaction**. Schema 26's `onboarding_projects(draftId, projectId)` receipt prevents a second project when process death happens after commit but before DataStore catches up. Reload resumes an interrupted Ready as Ready; other unfinished drafts with a committed receipt resume Price. If a different project was created meanwhile, the old unfinished journey is completed rather than creating a duplicate. The receipt is not backed up as user content.

The PDF draft uses `OnboardingPdfStorage` and app-owned storage. Replacing/removing it persists the new draft before deleting the previous unused copy; a failed draft write cleans its new copy, and cleanup after persistence cannot undo a successful draft operation. Creation success remains authoritative if remembering the last craft subsequently fails.

`startTrial` queues the introductory trial journey only for `TrialStartResult.Started`, with onboarding origin recorded, before completing the draft. AlreadyActive completes without a second start introduction; expired/tampered/failed starts retain explicit feedback. Confirmed purchased status can finish the journey with Pro-active feedback. `exitPending` reopens the already-created project after interruption and is acknowledged separately; it is not a request to create another project.

Visual assets are bundled clay-style WebP illustrations shown by shared `CraftIllustration`. Toolbox and choices use shared glyphs and staggered entrances; no infinite hero animation is required. Ready is a noninteractive summary, not a second Continue control. The preview build's simulated price/purchase UI belongs only to the isolated onboarding package; release uses the real billing/no-op variant facade.

Sources: [OnboardingDraft.kt](app/src/main/java/com/finnvek/knittools/domain/model/OnboardingDraft.kt), [OnboardingViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingViewModel.kt), [OnboardingScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingScreen.kt), [OnboardingSteps.kt](app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingSteps.kt), [OnboardingToolbox.kt](app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingToolbox.kt), [OnboardingPriceMode.kt](app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingPriceMode.kt), [OnboardingPdfStorage.kt](app/src/main/java/com/finnvek/knittools/data/storage/OnboardingPdfStorage.kt).

### Starter-project setup and the compact Projects start state

The ordinary empty Projects screen remains `ProjectsStartState` after the full journey. It is a separate presentation from onboarding and from the real project workspace. Its local example does not create project/history/session data.

An active starter project's overview can show a flat **Project setup** section directly below Continue. Its four optional suggestions are Target, Pattern, Yarn and Work Time. They derive from an actual target, an attached project document or compatible web pattern, linked yarn/project yarn notes, and a saved session. A positive count does not satisfy Target, and starting at zero is immediately usable. Incomplete suggestions sort first; complete rows have no action. There is no aggregate progress meter or all-done message.

Hide persists in `onboarding_state`. When all four facts become complete, the section remains for the current visit and is hidden on a later visit, including return to a reused navigation entry. `ProjectSetupVisit(projectId, routeEntryGeneration)` distinguishes navigation return from recreation; `CounterViewModel` owns persistence and the current celebration visit. Work Time opens the existing Counter route with an explicit WORK_SESSION tip request. It preserves loading/lifecycle/modal/placement checks while bypassing automatic-tip timing, seen state and process budget.

Sources: [ProjectSetupProgress.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/ProjectSetupProgress.kt), [ProjectSetupList.kt](app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectSetupList.kt), [ProjectsStartState.kt](app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectsStartState.kt).

### Contextual tips and committed celebrations

`TipRules` is pure eligibility logic. The process-wide Hilt `TipSession` owns one presentation at a time, route ownership and a budget of **two automatic tips per process**, with a chained FIRST_ROW → UNDO exception. `TipHost` waits for a resumed/loaded route, **2,000 ms** and a valid position; it blocks on relevant menus, sheets, forms, imports, active-session recovery, errors and focused input. Anchors use `Modifier.tipAnchor`; protected counter controls cannot be covered. An outside tap dismisses the tip without consuming the pointer event, so the same tap may activate the underlying control.

| Tip trigger | Current rule, in addition to readiness, priority, seen state and budget |
| --- | --- |
| First row / Undo | Starter project; Undo follows a committed first-row signal |
| Step size | Step still 1 and at least 30 committed increments in the project, or three recent increments within 2,000 ms |
| Work session | Counter opened on at least two days, with no saved or active session |
| Pattern tile | Onboarding PDF ready, or at least three counter opens with no document/web pattern |
| Keep screen on | At least four counter opens and the preference is off |
| Widget | Pinning supported and a saved/pending first session or at least three counter-open days |
| Backup | At least one project, seven days since first use, no successful backup; inline card on Projects |
| Folders | At least four projects, using unfiltered membership |
| Reading line | Project reader with documents; toolbar anchor opens settings without moving the line |
| Overview / Tools | Route-specific introduction subject to shared eligibility |
| Target / first saved session | Committed event facts; special persistent identity rules below |

The persisted per-project increment tally is capped at 30 and does not count an initial value or an optimistic tap whose write failed. Target crossing uses `applyMainCounterChangeWithResult`'s before/after/target snapshot from one transaction; Undo never triggers it. Per-project TARGET_REACHED memory is recorded only after visible presentation and survives tip reset. The first saved-session fact is classified inside the session transaction; monotonic `first_save_observed` prevents replay after history deletion or resetting tips.

`ProjectCompletedSheet` consumes a new **single-project completion-event snapshot** once by event ID. Bulk/idempotent completion does not celebrate; reopen then complete can create a new event. The snapshot is retained before clearing selection, and Next reuses ordinary project creation with current quota/folder rules. Settings resets persistent and process tip state only after the persistence write succeeds; it does not reset trial activity markers or already-celebrated target identities.

Sources: [TipRules.kt](app/src/main/java/com/finnvek/knittools/domain/tips/TipRules.kt), [TipSession.kt](app/src/main/java/com/finnvek/knittools/ui/tips/TipSession.kt), [TipHost.kt](app/src/main/java/com/finnvek/knittools/ui/tips/TipHost.kt), [OnboardingStateRepository.kt](app/src/main/java/com/finnvek/knittools/repository/OnboardingStateRepository.kt), [ProjectCompletedSheet.kt](app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCompletedSheet.kt).

### Offline guide and contextual actions

The bundled **How to Use KnitTools** guide has five topics and 26 items. Its topic keys, item strings, optional actions and `ProFeature` metadata belong to `GuideContent`; no website is needed to read it. Settings Info and the project overview footer open it. Root and topic routes are registered in both Settings and Projects, preserving the originating selected tab.

Guide actions open the existing counter/latest project, Tools, Library, My Yarn, Saved Patterns, Insights, Backup or Ravelry flow. A missing project falls back to Projects; an unknown topic falls back to the guide root. Asynchronous results are bound to the originating entry. Pro markers wait for initial entitlement readiness. Showing tips again uses the same reset operation as Settings and exposes persistence failure instead of claiming success early. Topic content uses shared hairline rows and typography within `ToolScreenScaffold`.

Sources: [GuideContent.kt](app/src/main/java/com/finnvek/knittools/ui/screens/guide/GuideContent.kt), [GuideScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/guide/GuideScreen.kt), [GuideViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/guide/GuideViewModel.kt), [GuideNavigation.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/GuideNavigation.kt).

## Projects and organization

### Projects, folders, sorting, and selection

Onboarding Phase 2 extends ordinary project creation with an optional starting count (empty means 0; valid range 0–99,999) and target (empty means no target; otherwise 1–99,999). `ProjectStartValues` is the shared parser, including the existing target dialog. `CounterRepository.createProject` keeps quota admission first in the transaction and inserts these as initial project values, with no history row, session checkpoint, linked-counter delta, or reading-line movement. Undo immediately after creation does nothing. Imported-PDF creation keeps its existing behavior.

The Create variant of `ProjectDetailsSheet` uses shared labeled fields, a craft-aware next-count helper, and a noninteractive `ContinueProjectCard` preview only before the first project. Name, craft and starting count are immediately visible; unit/custom label and target are under saveable **More options**. Restored advanced values open the disclosure. Collapsing preserves values, clears focus and cannot hide blocking validation errors; IME traversal visits only visible fields. Edit mode keeps name/craft/label scope. Unfiltered `observeHasAnyProject()` drives first-project behavior even when completed projects or other folders are hidden. Successful ordinary creation remembers the craft and marks the starter project only when the database was empty before insertion. Pending creation preserves count/target through quota retries; Java serial UID remains 1 while payloadVersion 2 distinguishes new drafts, allowing incompatible legacy payloads to be discarded safely.

**Creation.** New Project uses the shared `ProjectDetailsSheet`/`FormSheet`. The draft includes a name, craft type, main-counter label, optional custom label, starting count and optional target. Names are trimmed and must be nonblank. `ProjectNameRules` compares trimmed `Locale.ROOT` lowercase names across existing projects; a collision becomes `Name (2)`, then the next free suffix. This is a repository naming rule, not a database unique index. Custom count labels are trimmed and truncated safely to 32 Kotlin string units; an empty custom label cannot become the persisted chosen custom label.

`CounterRepository.createProject` checks the active-project quota and optional folder in the transaction that inserts both project and assignment. The form defaults to the last-created craft (initially knitting), its default counter label, and a zero starting count. New-project completion opens Counter. A project created from an incoming PDF is knitting/rows and unfiled; project plus PDF attachment is one transaction, so attachment failure does not consume the free slot with an empty project. `ProjectNameRules` has no independently enforced maximum name length; the 50-unit folder/extra-counter limit must not be invented for project names. Archive text-field limits remain a separate acceptance boundary.

**List and overview.** Normal cards open `project_overview/{projectId}`. A card uses the newest progress photo or a craft-specific `FabricSwatch`, the original-cased name, section/progress text and, for completed items, completion date. It has no separate counter button. The Continue hero body opens overview; its separate continue control opens Counter. The overview and Counter use one Projects-scoped `CounterViewModel` and the shared `ProjectContentHost`.

The overview displays photo/placeholder, name and completion state, Continue card, yarn, pattern/documents, notes, photos, reminders, and total recorded work. Edit details, folder move, completion/reactivation and delete belong to the actions menu. Opening the overview does not start a session.

**Sorting and visibility.** `ProjectSortOrder` offers Updated, Name and Created; default is Updated. Active Updated sorts by `updatedAt DESC`, completed Updated by `completedAt DESC`, Name by SQLite `COLLATE NOCASE ASC`, and Created by `createdAt DESC`, with `id DESC` tie-breaks. Active/completed flows are SQL sorted before folder filtering. The completed-project visibility preference is global and defaults false. Continue picks the first visible active project in that order with `count > 0`; with no positive-count candidate there is no hero. The active count still includes the hero project. The hero is not another independently persisted project selection.

**Folders.** The Projects title is a `ScreenTitleSelector` that opens a scrollable folder sheet. All Projects and Unfiled are typed virtual filters; neither is a database row or sentinel folder ID. A missing assignment means Unfiled. Each project has at most one assignment. Folder creation, rename, ordering and deletion are free metadata operations.

Folder names preserve trimmed display text, reject controls and Unicode line/paragraph separators, and have a 50-unit Kotlin string limit. Uniqueness is NFC-normalized plus `Locale.ROOT` lowercase. Bulk moves deduplicate IDs and validate every project and destination before writing. Deleting a folder deletes its assignments and leaves projects intact.

`ProjectListViewModel` stores its folder filter in `SavedStateHandle`. A new task starts at All Projects. A restored folder is checked only after an actual organization snapshot, so an initial empty placeholder cannot invalidate a valid filter. Folder mutations must not alter project timestamps, counters, sessions, files, documents, photographs or yarn.

**Selection.** Long press enters multi-select; active and completed rows can be selected together when visible. The shared `SelectionActionBar` exposes move, complete, and destructive delete. Selection is reconciled with visible rows when filters/visibility change; Select All does not act on hidden projects. Bulk complete/delete processes projects sequentially and stops on the first unavailable/failing result; earlier successful projects are not rolled back as one batch. Folder bulk moves instead validate the whole set and write their metadata transactionally. Library, Insights, widgets, ID navigation and histories are not filtered by the Projects folder selection.

**Completion, reopening and deletion.** Completion records each active-to-completed transition in `project_completions` with a zone, in the same repository transaction as the lifecycle change. Reopening clears the current completion state but preserves events; a later completion adds another event. Existing multiple active projects remain usable without Pro. New creation or reopening is admitted free when no active project exists; an additional active project needs current access. Completed projects do not use the free active quota.

An active work session introduces an explicit completion/save-or-discard decision and stale-session checks. Deletion resolves the active session before deleting, cascades project-owned records, clears reciprocal yarn links, and cleans unused private files after commit. These operations are not implemented by removing a list item optimistically.

Sources: [ProjectListViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListViewModel.kt), [ProjectCreationActions.kt](app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectCreationActions.kt), [ProjectFolderRepository.kt](app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt), [ProjectNameRules.kt](app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt), [CounterRepository.kt](app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt).

#### Completion and deletion boundaries

The project lifecycle is not a universal read-only flag over every related table. Canonical main-count changes reject completed projects, and the repository also rejects adding a new extra counter to a completed project. Existing content operations have their own guards; do not infer that every note, file, reminder or metadata mutation is prohibited merely from completion. The completion writer now verifies that the row still exists before recording the transition, so a delayed action cannot manufacture an event for a deleted project.

Deletion captures the app-owned file candidates, commits database deletion and cascading child removal, and then attempts reference-aware file cleanup. A database failure leaves files intact. Completion and reopening preserve documents, yarn usage and prior completion events. The optional destination folder is validated in the same transaction as project creation and the active-project entitlement check.

## Counting, reminders and notes

### Main counter, target, history, and Undo

The main workspace prioritizes the unit label, a large count, increase/decrease controls and Undo. The count label is Rows, Rounds, Repeats or custom text; craft-aware/localized display slots come from `CounterValueFormatter` and `MainCounterDisplayText`, not scattered hard-coded row strings.

Increment/decrement use a positive configured step. Arithmetic uses wider intermediates, floors at zero and saturates at `Int.MAX_VALUE`. A change that reaches a bound without changing the count is a no-op and does not replace the reversible history. Reset is a persisted change to zero. A positive optional target adds the common progress bar and localized “of”/remaining text; users may continue beyond it. Completed projects reject working-count mutations.

`CounterRepository.applyMainCounterChange` rereads the current project, then updates count/history, resets current stitch if tracking is enabled, applies linked-counter deltas, follows the active document's reading line, and checkpoints an active session inside one `DatabaseTransactionRunner` boundary. Widget count changes use `applyWidgetCountChange`, which delegates to this same operation.

History rows store the action, before/after values and timestamp, plus nullable linked-counter and session-undo snapshots. Undo chooses the newest **inserted history ID**, independently of wall-clock movement, and requires the matching project, nonnegative unequal values, a current count equal to the recorded new value, and a supported reversible action with consistent direction. It writes the previous value and deletes that history row atomically with dependent counter/reader/session work. Repeated Undo can walk earlier valid rows; it is not a transient UI-only snapshot or arbitrary historical restore.

Schema 27's `linkedCounterDeltasJson` stores each affected counter's **actual clamped delta**. Undo reverses those deltas rather than the requested main delta, and does not modify a counter linked after that history entry. Legacy null snapshots keep their inverse-main-delta fallback. Schema 28's `sessionUndoJson` stores session row-accounting anchors. Restoration requires the same session token and unchanged reviewed duration/row baseline. Matching recovery tokens restore row fields directly; a transition from no recovery into an unreviewed recovery state is handled explicitly, including transfer of pending/unreviewed progress for LONG_RUNNING. A new session or explicit review baseline cannot be rewound. Old/malformed/inapplicable snapshots keep the compatible fallback, and **elapsed time is never restored backwards**. Backup remaps embedded counter IDs and clears session undo snapshots because restoration creates new session identities.

The ViewModel can update the count optimistically, but committed facts own history and celebration signals. Its post-write reread checks selection generation, counter observation version and action version before publishing, so a late read cannot overwrite a newer observation/action or another project. Display delta feedback is not evidence that the write committed.

Counter History is a separate route with a project label, date grouping and locale/device time-format display. Missing/deleted project state leaves the route. The completed-session ceiling described below is not a limit on counter-history rows.

Sources: [CounterLogic.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/CounterLogic.kt), [CounterValueFormatter.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/CounterValueFormatter.kt), [CounterHistoryViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counterhistory/CounterHistoryViewModel.kt), [CounterRepository.kt](app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt).

#### Main-counter step size: presets and custom amounts

**Meaning and entry point.** The circular `×N` button between the main minus and plus buttons displays the selected project's saved `counter_projects.stepSize`, initially **1**. It opens `MainCounterStepSheet`; it does not cycle through values or multiply the existing count. A tap on plus/minus adds/subtracts the configured amount once. Tapping the main hero/count surface uses the same increment action and step; activating the step ring or another child control must not also trigger that surface increment. The image buttons use ordinary `clickable` actions, without hold-to-repeat. This is a basic feature with no Pro gate. It does not change the step of the secondary counter, stitch counter or independently configured extra counters.

**Editor contract.** The shared `FormSheet` title follows the main counter label: **Rows per Tap**, **Rounds per Tap**, **Repeats per Tap**, or `<custom label> per Tap`. Its description is “Choose how much one tap adds or subtracts.” A `SegmentedToggle` offers **1, 2, 5** and a `NumberInputField` accepts a **Custom amount** with placeholder `10`. Opening with a preset selects that segment and leaves the text empty; another saved value pre-fills the custom field and selects no preset. Selecting a preset clears custom text. Editing custom text clears the preset, even if the typed value is `1`, `2` or `5`; the two input modes are mutually exclusive.

`parseMainCounterStepSize` in `CounterLogic.kt` trims surrounding whitespace, requires nonempty digit-only text, parses an `Int` without overflow, and accepts **1–99 inclusive** (`MAX_MAIN_COUNTER_STEP_SIZE = 99`). Empty text, zero, negatives, signs such as `+5`, fractions, decimal separators, scientific notation, values above 99 and overflow are invalid. Invalid custom input shows the localized range error and disables Save; a selected preset remains valid with an empty custom field. Typing or choosing a preset changes only the draft. Cancel/dismiss before saving leaves the saved step unchanged.

**Save, failure and lifecycle.** `ProjectContentHost` keeps sheet visibility in `rememberSaveable(state.projectId)` and keys the sheet composition by project ID. Preset/custom drafts are `rememberSaveable`; busy/error state and the save `Job` are in-memory. The route passes its expected project ID to the public, non-suspending `CounterViewModel.setStepSize(size, expectedProjectId, onResult): Job`. This captures the selection generation before launching in `viewModelScope`; private `saveStepSize` serializes saves through `stepSizeSaveMutex`, validates the range and checks both project ID and selection generation before and after the repository call. A stale sheet, a switch to another project or a reopened selection cannot apply a late result to the current UI. After success, the ViewModel updates the step on the latest counter state, preserving any count observed while the write was suspended.

While saving, the confirm action shows busy state, repeat Save is blocked, Cancel/dismiss callbacks are guarded, and preset/text changes are ignored. The result callback closes the sheet on success; `false` keeps the draft open, displays “Could not save the step size. Try again.” and allows retry. Ordinary write exceptions become failure; coroutine cancellation is rethrown. `DisposableEffect` cancels the returned job when the sheet leaves composition, preserving cancellation when its project/route disappears. Cancellation or a stale result after a database commit is not a compensating database rollback.

**Persistence boundary.** `CounterRepository.updateProjectStepSize` validates **1–99** on injected `@IoDispatcher`, checks that the project still exists inside `DatabaseTransactionRunner`, then calls `CounterProjectDao.updateStepSize`. That SQL updates only `stepSize` and `updatedAt`. Saving a step does not change the count, add/delete counter history, reset stitches, move linked counters or reading guides, or start/finalize/checkpoint a session. Even saving the same step executes this timestamp update. The setter has no completed-project check; the separate working-count mutation still rejects completed projects. This reuses the existing Room column and model default of 1, with no schema migration or global DataStore preference. The 99 limit belongs to the new-input/save contract: `CounterLogic` itself only enforces a lower bound of 1 and does not retroactively clamp existing positive stored steps to 99.

**Counting, history and widget behavior.** Each main-button tap remains one `applyMainCounterChange` transaction and one history entry for a real count change. The repository rereads the persisted count and step; it does not trust an optimistic UI value or simulate N individual taps. At count 12 with ×5, plus produces 17 and minus produces 7. At count 2, minus with ×5 produces 0, an actual delta of −2; near `Int.MAX_VALUE`, increment similarly clamps. A no-op at a bound adds no history. One Undo restores the whole previous tap, including a clamped tap, and retains the currently configured step. Changing the step itself creates no Undo entry and does not erase earlier history. Reset still means zero, not subtraction by one configured step.

Actual signed count deltas drive linked extra counters, current-stitch reset, active-document reading-line follow and active-session row checkpointing through their existing canonical transaction rules. `applyWidgetCountChange` delegates to this same repository operation, so widget plus/minus also use the project's saved step even though the widget has no step editor. Step settings remain independent per project.

**Presentation and accessibility.** `MainCounterStepButton` uses the shared circular `CounterStepButtonFace`, a transparent interior, neutral ring/text at ×1 and primary ring/readable-primary text above 1. `CounterButtons` measures the label using dimensions owned by `CounterDimens`: 8 dp padding per side, a **48–96 dp** ring size and 8 dp gaps from the main buttons. The control exposes Button semantics and a localized description containing the label-specific title, current step and Change action. Plus/minus descriptions include the configured amount above 1; at 1 they retain the existing singular unit wording. The visible multiplier uses the multiplication sign `×`, via resources, rather than a literal ASCII `x`.

For steps above 1, successful local count changes in `increment`, `decrement` and `reset` attempt to emit a project-tagged `MainCounterStepFeedback` containing the **actual clamped UI delta**. A signed value such as `+5` or `−2` appears beside the main number for **900 ms** in a reserved 72 dp area; no delta is shown for step 1, a no-op or Undo. Collection is active only while the route lifecycle is STARTED, uses `collectLatest` and clears on replacement, lifecycle exit or project change. The flow has no replay and one extra buffer slot, so feedback is transient/best-effort. The delta is emitted before persistence, not as a save receipt; authoritative repository rereads can correct the optimistic count. Decorative delta text has cleared semantics to avoid a second announcement of the counter change.

Implementation entry points: [MainCounterStepSheet.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/MainCounterStepSheet.kt), [CounterScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt), [CounterWorkspaceSections.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt), [CounterViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt), [MainCounterDisplayText.kt](app/src/main/java/com/finnvek/knittools/ui/components/MainCounterDisplayText.kt), [CounterDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/CounterDimens.kt), [CounterProjectDao.kt](app/src/main/java/com/finnvek/knittools/data/local/CounterProjectDao.kt). See the [feature-to-test ownership map](#feature-to-test-ownership-map) for review and regression coverage.

#### Step-size state and asynchronous ownership

The stored main-counter step is exposed through presets and custom input. The repository's setter changes only the selected project's step and update timestamp; it is not a count mutation and therefore creates no count-history row, session row delta or reminder hit. A subsequent increment/decrement uses the persisted step through the canonical counter change.

The ViewModel serializes saves with a mutex and returns its save Job to the sheet. **Sheet disposal cancels that Job**; user dismissal is blocked while saving. Project/selection identity is checked before and after writing, and success updates the latest counter state without replacing an intervening count. A cancellation or stale result after commit cannot roll back that committed write. This boundary is separate from extra-counter step fields and adds no Pro feature.

### Secondary, extra, repeating, shaping, and repeat-section counters

The compact legacy secondary counter belongs to `counter_projects.secondaryCount`. It is not a named extra counter and must not be copied into `project_counters`. Its monotonic `secondaryCounterUsed` flag preserves use after trial/Pro ends.

Named extra counters are project-owned rows with a positive step, order and typed configuration. Add Counter validates a trimmed nonblank name of at most 50 Kotlin string units, a nonnegative count and valid type-specific fields. Existing item menus expose rename, reset and delete. Do not infer a general edit-type/edit-step workflow from persisted fields or a historical “Add/Edit” description.

| Type | Persisted configuration and current behavior |
| --- | --- |
| `COUNT_UP` | Ordinary bounded step counter; irrelevant repeat/shaping fields are cleared |
| `REPEATING` | Positive `repeatAt`; direct increment wraps `(count + step) % repeatAt` when the threshold is reached |
| `SHAPING` | Nonnegative starting stitches, signed stitch change, positive `shapeEveryN`; the counter increments normally and the display shows its cadence cycle |
| `REPEAT_SECTION` | Positive start row, inclusive end at/after start, positive repeat total; position is derived from main rows and cannot be linked as a delta counter |

**Presentation differs from raw storage.** `CounterValueFormatter.cyclePosition` returns zero for a zero/nonpositive count and otherwise a one-based cycle position, showing the cycle length at exact positive multiples. It uses `repeatAt` for repeating counters and `shapeEveryN` for shaping. A directly wrapped repeating count can therefore be zero. Shaping's starting-stitch/change metadata is stored and validated, but current production code does not implement the old document's claimed calculated current-stitch forecast or next-shaping-row calculation. The nonexistent shaping-calculator source reference has been removed; the current functionality is the persisted setup and cadence display.

**Linked counters.** The add draft can mark eligible extra counters linked to main. The canonical main transaction adds the actual signed main-count delta to their stored count, clamped to `0..Int.MAX_VALUE`, and records each actual linked delta in history. This path does not call the direct repeating increment/modulo routine; linked and manually incremented repeating counters must not be documented as identical storage transitions. Snapshot-backed Undo reverses the recorded per-counter deltas; legacy history uses the inverse main delta. Repeat-section counters are excluded because they already derive position. Newly created extra counters use positive sort order so they cannot match the legacy zero-order secondary-counter backfill filter.

**Repeat sections.** With length `end - start + 1`, section state starts at count zero/repeat one before the start. Within the range, repeat and row are derived with quotient/remainder. At `start + length * repeats - 1` and beyond, the section is complete and clamps to its final tracked repeat. Progress uses wider arithmetic. This state is interpreted through `RepeatSectionLogic` and the formatter; the generic extra-counter increment/decrement do not advance a repeat section.

Creation requires `MULTIPLE_COUNTERS` and, for shaping/repeat sections, its specialized feature gate. Existing counters remain content when access ends. Legacy generated “Pattern repeat” duplicates are excluded at the UI boundary, not promoted into another canonical counter.

Sources: [ProjectCounterRepository.kt](app/src/main/java/com/finnvek/knittools/repository/ProjectCounterRepository.kt), [ProjectCounterLogic.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/ProjectCounterLogic.kt), [RepeatSectionLogic.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/RepeatSectionLogic.kt), [MultiCounterComponents.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt).

### Stitch tracking and row reminders

Stitch tracking is independent of secondary and extra counters. Enabling tracking without a positive total opens the total dialog; confirming stores the total and enables tracking together, and cancellation leaves it disabled. Editing the total alone preserves the prior tracking choice. Current stitch is constrained to `0..stitchCount`; lowering the total clamps it. Disabling preserves the total and resets the current position, while clearing the total disables tracking. A successful main-row change resets the tracked current stitch. Completed-project guards apply to working stitch mutations.

Reminders are project-owned in-app messages for an exact target row or positive repeat-interval multiples. Message text is trimmed, nonblank and at most 200 characters. Creation is Pro-gated; existing reminders have edit/delete actions through `ReminderRepository` with project ownership and optional expected-row checks. Dismissal/presentation is separate from data ownership.

These reminders are not Android alarms, WorkManager timers, push notifications or a notification-permission feature. They do not promise an alert for every intermediate row skipped by a larger step. The counter's active alert and haptic behavior depend on current matching state.

Sources: [ReminderLogic.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/ReminderLogic.kt), [ReminderRepository.kt](app/src/main/java/com/finnvek/knittools/repository/ReminderRepository.kt), [CounterViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt), [ReminderComponents.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/ReminderComponents.kt).

Reminder matching is exact: the configured target itself matches, and a repeating reminder matches when the current count is at or above the target and its difference from the target is divisible by the positive repeat interval. A multi-step count jump is evaluated at its resulting count; it is not an instruction to replay every skipped row. Completed reminders do not trigger. Repository validation requires a positive target, a null or positive interval, and a nonblank message; message normalization trims and limits it to 200 Kotlin string units without cutting a surrogate pair. Updates/deletes validate project ownership and, where supplied, the expected reminder snapshot. See `domain/calculator/ReminderLogic.kt` and `repository/ReminderRepository.kt`.

### Local notes and concurrent editors

Notes are available as a project content sheet and a full-screen `notes_editor/{projectId}` destination. The full editor's top bar says Notes; `ProjectNameLabel` supplies context while preserving the project name's case. There is no remote journal processing, cleanup model, voice transcript or cloud rewrite.

`NotesEditorViewModel` holds a draft and its base in `SavedStateHandle`, debounces saves by 1,000 ms, exposes immediate save/discard and write-failure state, and retains newer local typing when an earlier write completes. On clearing, eligible unsaved content is handed to the application scope as a final best-effort persistence attempt. This is not a guarantee that arbitrary process termination flushes every keystroke.

`CounterRepository.saveProjectNotes` compares the requested result against its editor base and current stored content. Concurrent independent content is merged instead of blindly overwriting the latest project row; merged blocks use the existing separator convention. `notesCreated` is monotonic and preserves access to content previously created under entitlement. A new nonblank first use requires authorization/current access.

Sources: [NotesEditorViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/notes/NotesEditorViewModel.kt), [NotesEditorScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/notes/NotesEditorScreen.kt), [CounterRepository.kt](app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt).

The notes editor also observes entitlement changes alongside the project. Losing access while editing does not replace the unsaved draft with an empty string, and an entitlement change can unlock the preserved draft without reopening the screen. Debounced writes are cancelled when superseded; coroutine cancellation is propagated rather than displayed as an ordinary persistence failure. Final application-scope persistence is best effort, not a guarantee that Android will keep a killed process alive.

## Sessions, timing and recovery

### Work sessions and history

**Explicit work lifecycle.** Start/Stop actions control a single Room-owned active session across the application. Navigation, backgrounding, opening history, and ViewModel clearing do not start or finalize it. A UI ticker updates visible timing; durable wall-clock/elapsed-realtime/boot/zone anchors establish correctness while the app is not ticking.

Starting distinguishes Started, Already Active, Project Conflict, Missing Project, Completed Project, History Limit Reached, and Persistence Failure. Only one active project session exists. Main app/widget counter changes checkpoint trusted duration and net counter progress in the same transaction as counting. Completed session rows store start/end counter values (legacy row-named fields), display minutes, exact seconds, net counter progress, and a zone captured at session start. The completed-session retention ceiling is 100,000 rows.

**Timing and recovery.** On the same trusted boot, elapsed time is checkpointed seconds plus the nonnegative elapsed-realtime difference, with saturating addition. Recovery review is required for invalid anchors, unavailable boot identity, changed boot count, regressing elapsed realtime, or an unreviewed interval of at least 86,400 seconds. Reasons are `REBOOTED`, `BOOT_IDENTITY_UNAVAILABLE`, `INVALID_ANCHORS`, and `LONG_RUNNING`. A credible wall-clock duration after reboot is a suggestion, not silently trusted time.

**Review actions.** Active records carry a stable `sessionToken` and a `recoveryIntervalToken`. Stop/discard/prompt/recovery operations reject stale context. Stop refuses automatic finalization while review is needed. Recovery Add continues from new trusted anchors; Discard and Edit finalize through idempotent token-guarded repository transactions. Invalid duration, stale action, history limit, and persistence failures remain explicit outcomes. Completion/deletion UI decisions do not replace repository safeguards.

**History and metrics.** Session History supports project context and a delete menu. Completed history rows have no edit action; duration editing belongs to active-session recovery. Display minutes remain compatible with old rows, but calculations use exact `durationSeconds` and `rowsWorked`. New zone IDs preserve the original local day when a session is later viewed elsewhere; legacy null zones fall back to the current device zone. Cross-midnight splitting belongs to the metrics/Insights calculation, not a naive UTC-date grouping.

`rowsWorked`, `startRow` and `endRow` retain their existing stored names and numeric semantics but have no historical unit field. Session History therefore presents a neutral counter value/range; the active-session row, pending recovery progress and Stop summary use counter-progress wording. Editing a project's current Rows/Rounds/Repeats/custom label cannot relabel earlier sessions as measured rows. This is a presentation/interpretation change, with no schema or saved-history rewrite.

Sources: [ActiveSessionTiming.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/ActiveSessionTiming.kt), [ActiveSessionResults.kt](app/src/main/java/com/finnvek/knittools/repository/ActiveSessionResults.kt), [SessionTimeSource.kt](app/src/main/java/com/finnvek/knittools/data/time/SessionTimeSource.kt), [SessionHistoryViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/session/SessionHistoryViewModel.kt), [SessionMetrics.kt](app/src/main/java/com/finnvek/knittools/ui/screens/insights/SessionMetrics.kt).

#### Time anchors and recovery decisions

`ActiveSessionTiming` separates trusted elapsed time from wall-clock suggestions. An active row contains a stable recovery token, project ID, start/checkpoint wall time, elapsed-realtime values, boot identity, zone, row anchors and pending/recovery facts. Same-boot monotonic deltas can be trusted; missing boot identity, reboot, malformed anchors or an unreviewed interval of at least 24 hours require review. A display ticker only refreshes the UI and cannot establish background correctness.

A main-app or widget count change checkpoints trusted session time and the actual signed count delta inside the count transaction. This allows net counter progress to fall when a user decrements or undoes. Recovery Add accepts the reviewed duration and continues from new trusted anchors; Edit and Discard finalize through the stable token, with Discard retaining the trusted portion rather than inventing elapsed work. Stop/recovery writes and removal of the active row are atomic and repeated token-based finalization cannot create a duplicate completed session.

Native writers cap completed sessions at 100,000; the check and insertion occur inside the same transaction, including completion/replacement/recovery paths. Existing larger histories are not truncated. Long-session analysis can inspect at most 366 local-day segments per session. These are source-level resource bounds, not a measured maximum-history performance guarantee.

## Patterns, project documents and PDF workflows

### Library, patterns, and project documents

**Library.** The hub leads to Saved Patterns, My Yarn, and All Photos. Library state is independent of the Projects folder filter. Saved Patterns includes local PDFs, retained Ravelry metadata, and user-added website links. A saved item can be attached through a project picker that includes active and completed projects.

**Saved Pattern identity.** The existing `saved_patterns` row uses `SavedPatternSource` (`Ravelry`, `LocalFile`, `WebLink`, `Other`, `Unknown`) and nullable source fields. Source enum definitions live in `domain/model/SavedPattern.kt`. Persisted fields are `ravelryPatternId`, original/canonical URL, local PDF URI, offline availability, update/sync timestamps, and `PatternAvailability` (`free`, `paid`, `unknown`). Compatibility getters such as `ravelryId` do not justify restoring integer-zero or old URL sentinels in storage.

Duplicate matching checks Ravelry ID, canonical URL, normalized original URL, then title/designer only when explicitly requested. Details own available actions according to actual source/file availability. Missing observed items leave their detail route. Remote thumbnails use the shared HTTPS-only component and disappear on invalid/missing/failed URLs rather than inserting a placeholder or project-owned duplicate image.

**Website links.** Titles and optional designers use the shared safe-text boundary (120 string units); title is required.  Add/Edit Web Pattern saves a local title/designer/URL. Valid HTTP links are permitted with an insecure-link warning; browsing revalidates before an external browsable `ACTION_VIEW`. Ravelry pattern links instead enter the in-app Ravelry browser. Compatible old `Other` rows count as web patterns only without a local PDF and with a valid non-Ravelry web URL.

A web pattern is free metadata. It creates no offline PDF or `project_documents` relation and has no backend import/Pro path. Attach/unlink/replacement/delete must preserve project PDF documents. `SavedPatternRepository` owns metadata edits and linked-title propagation; `CounterRepository.attachSavedPatternMetadata` / `unlinkSavedPatternMetadata` own transactional replacement and stale-action checks. Replacing linked metadata asks for confirmation against the observed expected saved-pattern ID.

**Readable documents.** A project may own multiple `project_documents` relations with label, order, availability, saved-pattern reference, stable document key, primary flag, and independent reader state. `ProjectDocumentRepository` owns creation, rename, reorder, primary selection, removal, and reference-aware cleanup. A nonempty document set has exactly one primary document. Document removal and metadata unlink are different operations.

`projectDocumentId` identifies a relation/order; `documentKey` owns annotation/bookmark identity. The selected/active document drives reading-line updates. The old PDF/reader fields on `counter_projects` are compatibility data, not an alternative authority. A shared file must survive removal of one Library/project reference while another reference remains.

**Open/Share PDF.** The app accepts PDF `ACTION_VIEW` and `ACTION_SEND` streams and copies transient grants immediately into `pattern_pdfs/0`. Save Pattern offers an editable cleaned label, one-tap attachment to an active project, new-project creation subject to the active quota, or Library-only saving. New project plus PDF is one repository transaction, so a failed attach does not leave an empty new project. Successful project saves open the overview; Library saves open detail.

Pending URI/name are restorable via `SavedStateHandle` only if the private file still passes canonical-location checks. Mutex/busy guards prevent duplicate writes/dismissal; failures retain pending data for retry. Discard/replacement cleans unused copies through the repository gate. A failed cleanup can leave an orphan; no claim is made that every `pattern_pdfs/0` orphan is automatically pruned at startup.

**Shared text.** `PatternShareCoordinator` preserves one pending and one queued payload; duplicates are ignored and a third distinct payload returns Busy rather than overwriting pending work. Parsing permits at most one distinct canonical HTTP(S) URL. Bounds are 16 KiB text, 4 KiB subject, and 2,048-character URL. Empty, ambiguous, malformed, and overlong input have typed failures. Recognized Ravelry pattern URLs open the website flow; there is no automatic backend preview request.

Sources: [SavedPatternRepository.kt](app/src/main/java/com/finnvek/knittools/repository/SavedPatternRepository.kt), [SavedPattern.kt](app/src/main/java/com/finnvek/knittools/domain/model/SavedPattern.kt), [ProjectDocumentRepository.kt](app/src/main/java/com/finnvek/knittools/repository/ProjectDocumentRepository.kt), [SavedPatternDetailScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternDetailScreen.kt), [WebPatternUrl.kt](app/src/main/java/com/finnvek/knittools/domain/model/WebPatternUrl.kt).

### PDF reading, annotations, and creation

**Reading mode.** A project or Library local PDF opens in the reader. Reading mode prioritizes the page, uses Browse, offers Mark up in the top bar, and exposes bookmarks from the bottom bar. Markup mode shows the tool controls and Done. Layers and marked-PDF export live in the overflow menu. The sole Android PDF renderer is `PdfPageRenderer`; a second renderer inside UI would violate ownership.

**Guides and calibration.** Project page, horizontal-line enabled/Y/follow state, vertical-guide enabled/X, and row mapping are persisted per document. Manual page/horizontal movement pauses follow without creating a marker. Only explicit row-marker and two-point calibration actions write mappings. Return-to-row and mapping changes recompute with zero row delta. Library horizontal/vertical guides are `rememberSaveable(patternUri)` state, with no project/saved-pattern schema path.

Following prefers an exact marker on the current page, then an unambiguous exact marker on another page or a same-page bracket. Otherwise it conservatively applies a row-delta fallback without guessing across ambiguous pages. App and widget row changes resolve this inside the main-counter transaction. Late gestures/actions must stay bound to the initiating document/mapping rather than overwrite a newly selected document.

Reader labels and announcements describe **counter values**, not an assumed knitting row: the English actions include Follow main counter, Return to current count and Calibrate counter positions. `ReadingLineRowLabel` and bottom navigation use `row_label_format` (Count N), while persisted mapping/API identifiers retain their historical row names. Geometric chart rows/columns are still spatial coordinates, not renamed counter units.

**Bookmarks.** Project bookmarks have a trimmed nonempty name of at most 50 string units, validated page/Y, and the active project's document key. Names may repeat. Order is page, Y, creation, ID. Detach preserves bookmarks, reattachment of the same document restores them, replacement hides the old document's bookmarks, and project deletion cascades. A jump pauses follow and issues a one-shot viewport focus request after page rendering. Bookmarks are free and excluded from annotation export.

**Layers and tools.** Library owns an editable saved-pattern master layer. A project viewer renders that reusable master read-only plus its own editable overlay for the same document key. Detach deactivates project ownership without deleting edits; same-document reattach restores it. Tools include pen, highlighter, eraser, select, line, arrow, rectangle, ellipse, text, callout, and chart. Text/callout open an editor; chart tracking resolves domain drafts/counter identities rather than creating a separate opaque counter system.

Coordinates are normalized page coordinates transformed once by `PatternPageCoordinateTransform`. Pointer movement remains in ViewModel memory; persistence occurs at gesture/command boundaries. A 50-command in-memory undo/redo stack is document/layer-context guarded. `PatternAnnotationCanvasRenderer` renders viewer overlays and rasterized export consistently. Budget/codec checks reject unsafe overlarge legacy pages as a whole while retaining rows; they do not silently display a truncated prefix.

**Editing existing text.** Selecting an editable text-box annotation exposes **Edit text**, which reuses the text editor with its existing content. Save updates that same annotation; Cancel leaves persisted content unchanged. The project overlay is editable, while a reusable Library master remains read-only inside a project. The master can be edited in its owning Library viewer. Blank text cannot be saved; repository validation also enforces the versioned payload and page budgets. Successful changes participate in the existing Undo/Redo history. The editor stays bound to its original target rather than following a later selection; see [the editing contract](#editing-existing-text-annotations) for stale-context and concurrent-write handling. This changes app-owned text-box annotations, not the PDF's original embedded text.

**Export.** Marked PDF export uses SAF `CreateDocument(application/pdf)`, renders one bounded page bitmap at a time into a cache temporary PDF, reports progress, then copies to the selected destination. It preserves the original PDF and cleans the temporary file on success/failure/cancellation. The output is rasterized: original searchable text/vector/link structure is not preserved. Reading guides/bookmarks are excluded. The chosen provider may retain a partial destination on failure. Exact budgets are in [Annotated export budgets and failure behavior](#annotated-export-budgets-and-failure-behavior).

**Camera/gallery.** Photo-to-PDF creation uses photo/PDF wording, not a document scanner claim. Camera captures and Photo Picker images are immediately copied into a private session, ordered in a restorable ViewModel draft, bounded/decoded, and converted page by page. Limits: 20 pages, 25 MiB/image, 200 MiB/session, 1,800-pixel long edge, 32 MiB reserve. Animated images are rejected; transparency is flattened onto white. Camera and gallery photo-to-PDF entry points are gated by `PATTERN_CAMERA_SCAN`; document import/reading is separate. Gallery's narrow/large-font provider fallback is described under storage/accessibility.

Sources: [PatternViewerScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt), [PatternAnnotationViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationViewModel.kt), [PatternAnnotationRepository.kt](app/src/main/java/com/finnvek/knittools/repository/PatternAnnotationRepository.kt), [PatternDocumentStorage.kt](app/src/main/java/com/finnvek/knittools/data/storage/PatternDocumentStorage.kt), [PdfPageRenderer.kt](app/src/main/java/com/finnvek/knittools/data/storage/PdfPageRenderer.kt).

### Rendering, mapping, and annotation budgets

| Boundary | Current limit / behavior |
| --- | --- |
| Display PDF rendering | Bounded render dimensions, maximum side 4,096 pixels; failed-render bitmaps recycled |
| Row-mapping JSON | 256 KiB, at most 4,096 markers; malformed entries handled independently, duplicate row/page keeps first, deterministic page/row order |
| Annotation payload | Version 1, maximum 256 KiB UTF-8; freehand maximum 2,048 points |
| Chart shape | Rows/columns 1–999, total at most 10,000 cells |
| One layer/page | 256 annotations, 16,384 work units, 2 MiB payload total |
| Unsafe legacy page | Bounded read/257th-row guard; reject whole unsafe editable/read surface without deleting stored data |
| Combined selection | At most two visible layers, 512 annotations / 32,768 work; eraser mutates editable layer only |
| Hit testing | Deterministic highest z-index/ID within bounded geometry |
| Undo/redo | 50 in-memory commands, context/stale guards |

Annotation codec/page budget and repository checks are distinct from export-wide caps. `PatternPageCoordinateTransform` applies normalized-to-rendered geometry once; export and view share `PatternAnnotationCanvasRenderer`. Chart tracker references must be rebased during restore alongside counter IDs.

### Markup and chart interaction details

Markup tools are user commands over a normalized page: freehand pen/highlighter, eraser, selection, line/arrow/rectangle/ellipse, text boxes, symbolic callouts, chart regions and chart trackers. Highlighter axis lock and optional pressure affect drawing; pointer movement remains a draft until commit. Selection supports moving/resizing, duplication, deletion and z-order changes. Undo/redo stores at most 50 commands in memory and is invalidated by a changed owner/layer context; it is not a persistent cross-launch history.

Callouts use check, cross, star, exclamation or note symbols plus title/description. Codec normalization bounds stroke width to 0.5–64, text size to 8–96 sp, finite unit coordinates to page space, and coordinate precision to five decimal places. The payload version is 1, encoded UTF-8 is at most 256 KiB, and freehand point count is at most 2,048.

A chart region records page bounds, name, rows/columns and row/column directions. A tracker adds a main/extra-counter reference, counter-start value, grid-start index, optional end wrapping, color/opacity and C2C origin. Row direction is top-to-bottom or bottom-to-top; column direction is left-to-right, right-to-left or alternating. The same stored counter identity must be preserved/rebased through attachment/restore rather than guessed from a counter name.

| Tracker mode | Highlight behavior |
| --- | --- |
| Active row | Cells in the resolved current row |
| Active column | Cells in the resolved current column |
| Crosshair | Union of current row and column |
| Completed cells | Cells from the beginning through the current index |
| C2C diagonal | Current cell in the configured corner-to-corner diagonal traversal |

A missing referenced counter produces unavailable tracking rather than silently choosing a different counter. Grid limits are 1–999 in each direction and 10,000 cells per chart. The ViewModel's highlight aggregation also applies the 20,000-cell page/document budgets; export applies its own tracker-work ceiling. These mechanisms do not prove physical-stylus pressure accuracy, all gesture combinations, PDF fidelity or maximum device performance.

Sources: [PatternAnnotationTypes.kt](app/src/main/java/com/finnvek/knittools/domain/model/PatternAnnotationTypes.kt), [ChartTrackerResolver.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/ChartTrackerResolver.kt), [PatternAnnotationViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationViewModel.kt), [PatternAnnotationPayloadCodec.kt](app/src/main/java/com/finnvek/knittools/domain/model/PatternAnnotationPayloadCodec.kt).

#### Editing existing text annotations

**Entry point and draft ownership.** `PatternAnnotationToolbar` exposes `onEditText` only for a selected editable text annotation. `PatternAnnotationViewModel.patternToolbarActions()` in `PatternViewerScreen.kt` wires it to `PatternAnnotationViewModel.editSelectedText()`. The ViewModel captures `PatternAnnotationTextEdit(owner, annotation)`; the shared `PatternTextEditorDialog` is keyed to that edit and initializes its local `rememberSaveable` text from `TextBoxPayload.text`. Existing `onAddTextBox` / `addTextBox` creation remains separate. The dialog uses `ScrollableFormDialog`, localized Edit text/error copy and the shared neutral Cancel button. While saving, editing, Save, Cancel and dismissal are disabled. Cancel before saving performs no write; saving text identical to the opening snapshot simply closes the editor without creating a command.

**Target and permission binding.** `saveTextEdit(edit, text)` accepts only the exact active edit object, then verifies the owner, editable layer, original page and continued presence of the original annotation ID. Changing selection while the editor is open does not retarget it. A page change, including away and back, clears the edit; an owner/layer-context change also clears it and the command history. Observing that the annotation has disappeared clears the edit. Cancelled or invalidated callbacks cannot reopen the old operation. UI checks are not the persistence authority: the transaction rechecks ownership and the document key. Project writes require the matching project-owned active layer; a saved-pattern owner may write only its own master. A project cannot use this path to edit the Library master.

**Transactional update and concurrent changes.** The write path is `PatternAnnotationCommand.EditText` → `PatternAnnotationRepository.updateText` → `PatternAnnotationDao.updateText`. The DAO's Room transaction reloads the layer and the bounded original page, requires the original ID and `TEXT_BOX` kind, and compares the decoded current annotation with the editor's expected snapshot. `updatedAt` is deliberately excluded from that comparison: undo/restore can produce the same content with a newer timestamp. Changes to decoded text, styling, bounds, ordering or other annotation content cause failure instead of a last-writer-wins overwrite. This is a content comparison, not a monotonic revision-token contract. Missing rows fail; this path uses update, never insert/restore/upsert, so a late save cannot recreate a deleted annotation. The transaction also runs the existing whole-page budget validation after the update, so a failed budget check rolls back the write.

**Payload preservation and validation.** `PatternAnnotationPayloadCodec.replaceText` validates the stored version-1 text payload, replaces only the `text` member in its current raw JSON object, then validates the resulting payload again. It preserves unrelated JSON members, including unknown future fields, rather than rebuilding the payload from the decoded model. The row retains its ID, layer, page, geometry, styling, z-order, creation time and payload version; only text and `updatedAt` change. The timestamp advances to at least the previous timestamp plus one. Blank/whitespace-only text, unsupported versions and payloads over 256 KiB UTF-8 are rejected; the early string-length bound is not a substitute for the final encoded-byte check. Valid text is not trimmed, and multiline/Unicode content is preserved. No schema, dependency, source-PDF rewrite or separate annotation Pro gate is introduced.

**Undo, errors and cancellation.** `EditText.apply()` constructs its inverse from the successfully returned annotation and the previous text. Undo and Redo therefore use the same checked text-update path and preserve unknown fields; a competing content change can reject a stale history operation. The existing 50-command, in-memory history remains scoped to the owner/layer context. A normal save failure retains the dialog's draft and displays a localized error; a conflict requires closing and reopening to capture a fresh snapshot. Coroutine cancellation is rethrown by the command pipeline. Cancellation before the Room commit rolls back the transaction; cancellation observed after commit does not imply rollback of durable text. Only a command whose success is delivered in the matching context adds its inverse to the UI history; there is no persistent undo history across process death.

Sources: [PatternAnnotationToolbar.kt](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationToolbar.kt), [PatternAnnotationViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationViewModel.kt), [PatternAnnotationCommand.kt](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationCommand.kt), [PatternAnnotationRepository.kt](app/src/main/java/com/finnvek/knittools/repository/PatternAnnotationRepository.kt), [PatternAnnotationDao.kt](app/src/main/java/com/finnvek/knittools/data/local/PatternAnnotationDao.kt), [PatternAnnotationPayloadCodec.kt](app/src/main/java/com/finnvek/knittools/domain/model/PatternAnnotationPayloadCodec.kt).

### Annotated export budgets and failure behavior

The exporter uses a source-side ceiling of 14,400 pixels, maximum 100 pages, rendered bitmap side 1,800, at most 3.24 million pixels/page and 48.6 million total. Annotation export caps include 2,000 annotations, 16 MiB payload, 250,000 work units, and 20,000 tracker work units. The temporary PDF is capped at 200 MiB, with 32 MiB reserve (232 MiB combined admission requirement).

Rendering one bitmap at a time bounds bitmap lifetime; Android `PdfDocument` still retains its page representation until writing completes. It is not a fully streaming PDF rewrite. Export uses SAF CreateDocument, a complete cache temp, then provider copy. Temp cleanup occurs on success/failure/cancellation; provider-side partial output can remain. Source PDF, read-only master ownership, guides/bookmarks, and original text/vector fidelity have the distinct behaviors described above.

Before export, the destination is rejected if it is the same URI or a recognizable canonical local/app-owned file alias of the source, including supported FileProvider aliases. This prevents the output copy from truncating its own input. Unknown aliasing inside a third-party provider remains outside that proof. Export is rasterized page by page, so it is not a lossless editable-vector PDF round trip.

### Import and cleanup decision points

| Input/action | Ownership and success boundary | Failure/cancellation behavior |
| --- | --- | --- |
| SAF PDF chosen for project | Persist read permission where supported, copy into app-owned storage, validate through `PdfPageRenderer`, then attach transactionally | Copy/attachment result determines success; failed unpublished copy is removed when possible |
| PDF Open/Share from another app | Immediately copy transient grant to `pattern_pdfs/0`; Save Pattern chooses project/new project/Library | Pending draft restored only for an existing canonical private file; busy/mutex guards reject duplicate save |
| Ravelry PDF download | Download cache file over HTTPS, then use the same validated private-copy/Save Pattern flow | MIME/filename alone is insufficient; partial cache/private copy cleaned; provider/download errors remain separate |
| Gallery selection | Copy accepted unique source URIs into the current private import session immediately | Duplicate selection is ignored; invalid/animated/oversize input has explicit failure; external URI is not permanent project state |
| Camera result | Target tied to initiating project and permission/result state | Abandoned/cancelled/late result cleans its owned target instead of attaching to another project |
| Image conversion | Decode and write pages sequentially, flatten transparency onto white; generated PDF attaches only after creation | Cancellation/failed attach removes temporary/unpublished output; original durable source is preserved |
| Remove document or Library PDF | Database relation removed first; shared-file gate checks all current references | Other relations retain shared bytes; physical failure can leave an orphan |
| Replace photo | New unique file exists before publishing URI; old file removed after success | Failed URI publication removes the replacement, preserving the previous photo |

PDF import does **not** share the website downloader's fixed 100 MiB limit. `copyPdfToInternal` computes its copy budget from Android allocatable storage minus the 32 MiB reserve, validates at least one readable page, and publishes a unique filename. It refuses to treat an already app-owned URI as an arbitrary external source. Image-session, export and backup limits are independent and can be stricter.

Private PDF filenames are cleaned, limited to 180 characters including extension/suffix, and written without replacing an existing file. Document display labels use their own 50-unit limit and `PatternDisplayNames`; filesystem naming and UI labeling are different contracts.

Capture pruning uses a seven-day stale-age default. Yarn-photo startup pruning and restore-journal recovery have separate ownership; an incoming-PDF discard is cleaned through the application lifetime because no universal startup sweep of `pattern_pdfs/0` is established. No cleanup routine should recursively delete an entire shared PDF directory solely because one project disappeared.

The progress-photo viewer can share an image through `ACTION_SEND`, an app FileProvider URI, `ClipData` and temporary read permission. The selected receiving app/provider then owns its copy/access. FileProvider does not expose the source PDF or the whole private files root.

## Yarn, usage and progress photos

### Photos and yarn

**Progress photos.** Project Photos captures/images are stored privately with row number, note, and creation time. Project Gallery and Library All Photos offer viewing and row menus; All Photos uses the shared project filter. The photo viewer is fully opaque. Capture-target creation and abandoned-target deletion belong to `ProgressPhotoRepository` / `CounterViewModel` on injected IO, not composables. New capture is gated; existing photos remain data. Notes are capped at 100 characters; image processing is bounded to a 1,920-pixel edge and JPEG quality 80.

**My Yarn.** A yarn card stores brand/name/fiber, grams/length per skein, needle/gauge information, color/dye lot, weight category/care bitset, optional private photo, integer stash quantity, status, and optional project link. Status is In Stash, In Use, or Finished; unsupported stored values normalize to In Stash. Quantity defaults to one stash unit and is a nonnegative integer, not a meter/gram balance.

New yarn-card creation requires `UNLIMITED_YARN`; there is no implemented numeric free-card allowance. Edits/existing cards remain supported after entitlement ends. Do not invent a free limit of several cards. Status uses `SegmentedToggle` below the name and a shared trailing status pill on list cards. Linked cards show the localized project line; redundant In Use decoration is hidden where that line already communicates it. Yarn weight uses eight standardized Ravelry labels plus editable imported/Other text. `StandardYarnWeight` no longer stores a `ply` hint, and `YarnWeightField` does not display one: a thickness category does not establish the yarn's strand construction. The persisted category labels remain unchanged.

Yarn detail's care section displays each selected symbol with its localized text in a 144 dp-wide item. `CareSymbolIcon` retains its 28 dp semantic icon, draws an explicit hand entering a wash tub, and places tumble-dry temperature dots inside the circle. Washing labels specify Celsius. The existing care bitset/selection model is unchanged; glyph corrections do not alter stored care values.

Detail routes observe the actual card. Missing rows exit; edits use repository results and observation instead of optimistic local-only state. `YarnCardRepository` owns reciprocal `linkedProjectId` and project yarn-ID CSV writes; `YarnCardLinks` parses/formats that CSV, capped at 1,000 IDs per project. Photo replacement copies first, persists the new URI, then deletes the old private photo. Startup pruning removes unreferenced yarn-photo files.

**Project yarn notes.** A lightweight project-owned yarn description can exist without a Library card. Save to My Yarn creates/reuses a linked card and preserves the note through `savedYarnCardId` in one transaction. If usage already exists, it binds that record rather than creating duplicate usage; saving a note does not create usage automatically.

Every existing note exposes Edit and Delete through the shared `RowOverflowMenu` in `YarnManagementSheet`. Edit reuses `ProjectYarnForm`, prefilled with name, description, quantity and notes. It works before and after Save to My Yarn and adds no Pro gate. Cancel closes the draft without writing. While saving, the form disables input and dismissal and clears keyboard focus. A failed save leaves the draft visible with an inline, accessibility-announced error and permits retry; a missing note or conflicting edit requires closing and reopening the editor.

`CounterViewModel.editProjectYarnNote` checks the selected project and calls `ProjectYarnNoteRepository.edit` on injected IO. The repository reads, compares and updates the existing row inside `DatabaseTransactionRunner`; it never inserts a replacement. Blank names are rejected, and the creation normalization is reused: trim name/description/notes and clamp quantity to at least one. The four editable fields are compared against the opening snapshot. A different concurrent edit returns `Conflict`; a missing row or mismatched note/project identity returns `Missing`. If the current fields already equal the normalized draft, retry succeeds without another write, including after cancellation of a response following commit. Pre-commit failure or cancellation rolls back; post-commit cancellation does not undo the saved row.

Only the four editable fields and the note's own `updatedAt` change. The current row supplies its ID, project ownership, creation time and latest `savedYarnCardId`, so a concurrent Save to My Yarn link is preserved and a deleted note cannot be recreated. Editing stays local to the project note: it does not modify the linked Library card, global stash quantity, usage identity or amounts, reciprocal project links, project timestamps/counters, sessions, documents or files, and it never creates or duplicates usage. This editor is separate from ordinary project Notes and Library yarn-card editing.

**Project usage.** A yarn card and the project note referring to it resolve to one logical `project_yarn_usage` record. Planned, allocated, and used amounts are independent nullable canonical `Double` meters: null means unknown, zero means known zero. Remaining is allocated minus used; negative values display overage. Converting grams or fractional skeins requires a user-confirmed positive meter/gram relationship. Usage has no Pro gate and does not deduct global stash quantity, update project timestamps/counters, touch sessions, or change files/documents.

`ProjectYarnUsageRepository` owns coherent observation, source identity, validation, revision checks, and transactional create/update/delete. Source deletion preserves editable usage through nullable source FKs and a creation-time name snapshot. Live source names take priority when available. Unlinking, completion, and reopening retain usage.

Sources: [YarnCardRepository.kt](app/src/main/java/com/finnvek/knittools/repository/YarnCardRepository.kt), [ProjectYarnNoteRepository.kt](app/src/main/java/com/finnvek/knittools/repository/ProjectYarnNoteRepository.kt), [YarnManagementSheet.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/YarnManagementSheet.kt), [ProjectYarnUsageRepository.kt](app/src/main/java/com/finnvek/knittools/repository/ProjectYarnUsageRepository.kt), [YarnCardDetailScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/yarncard/YarnCardDetailScreen.kt), [ProgressPhotoRepository.kt](app/src/main/java/com/finnvek/knittools/repository/ProgressPhotoRepository.kt).

#### Usage revisions and failure results

Creating usage requires a current project-owned source and at least one known, finite, nonnegative amount. A note and its saved card are resolved before duplicate detection; a repeated create returns the existing logical record. Update/delete require the current `updatedAt` revision and project ownership. Updates advance the revision monotonically with checked arithmetic; stale actions return a typed result instead of overwriting another editor.

Amounts are never rounded back from their display text. Meter/yard conversion, gram conversion and fractional skein conversion are calculation choices over the same stored meters. A positive input that underflows to zero is invalid. If either skein conversion parameter is supplied, the positive length/weight relationship must be valid. The inventory's integer quantity, the estimator's suggested purchase count and a project's allocated/used meters are three independent values.

#### Photo commit and cancellation boundaries

Progress photos now use Android `ImageDecoder` with software allocation and sampled output, including decoder-applied orientation. A capture note is limited without cutting a UTF-16 surrogate pair. After an insert failure or cancelled response, `ProgressPhotoRepository` checks persisted references in a non-cancellable IO cleanup block before deleting a newly copied file. An indeterminate check conservatively retains the file.

Yarn photo replacement follows the same integrity principle: if persistence may already have committed, reread the card before discarding the new image. Delete only a copy known to be unreferenced; retain uncertain copies for restart pruning. A cancelled caller is not proof that a database transaction failed. These commit/cancellation safeguards protect files without claiming every cancellation response means “nothing changed.”

### Image creation and photo replacement

`PatternImageImportViewModel` owns ordered/restorable pending image state; storage deduplicates selected source URIs and immediately copies accepted images. Default is Android Photo Picker; when width <360 dp and font scale ≥1.5, `OpenMultipleDocuments` supplies the explicit accessibility fallback. URI permissions are not saved as durable project image references.

Pattern-image limits are 20 pages, 25 MiB per image, 200 MiB per session, 1,800-pixel decoded long edge, and 32 MiB free-space reserve. Decode is sampled/bounded and pages are rendered sequentially; animated content is rejected and alpha becomes white. Generated PDFs attach only through `CounterRepository`. Stale capture pruning considers only credible positive/nonfuture timestamps older than seven days.

`ProgressPhotoStorage` samples bounded images to a 1,920-pixel edge/JPEG 80. `YarnPhotoStorage` copies raw source bytes into a private `.jpg` path, bounded to 25 MiB and reserve 32 MiB; the filename is not a guarantee of transcoding. Yarn replacement deletes a failed new copy, commits the new URI before old deletion, and serializes swaps/pruning/backup coordination. `App.onCreate` schedules unreferenced yarn-photo pruning for interrupted swaps.

## Calculators and reference library

### Calculators and reference material

Tools presents local calculators plus Ravelry; Library's Reference section opens the reference pages. Calculator actions do not write Room, preferences, project notes, or network services. Copy writes plain text to the clipboard. Optional project context for Gauge is navigation context, not automatic project mutation.

| Tool | Implemented calculation / behavior | Authority |
| --- | --- | --- |
| Increase/Decrease | Positive current/change counts; paired decreases require two source stitches each. M1 requires at least two starting stitches and at most N−1 flat / N circular increases. Computes easy/balanced instructions and rejects overflow | `IncreaseDecreaseCalculator` |
| Measurements and Gauge | Unit conversion, swatch/manual stitch and row density, count for desired size, size from count, and adjustment from pattern gauge to actual gauge | `MeasurementCalculator`, `GaugeViewModel`, `GaugePresentation` |
| Cast On | Canonical density from per-10-cm/per-4-inch gauge; rounds without a repeat, or chooses the nearest usable repeat from the unrounded exact body count; adds edge stitches afterward and reports alternatives/actual widths | `CastOnCalculator` |
| Yarn Estimator | Positive finite total length, length/skein, grams/skein; raw exact skeins from division, display formatting separate; purchase count `ceil(total/lengthPerSkein)`, total grams from whole purchase count | `YarnEstimator` |
| Needle Sizes | 38 metric rows from 1.5–25 mm; numbered Japanese 0–15 use their exact Clover diameters instead of nearby US/metric equivalents; metric-prefix or normalized exact regional search | `NeedleSizeData` |
| Size Charts | 46 body-measurement rows and 123 measurements, including 58 ranges; canonical cm and derived inches across Baby, Child/Youth, Women, Men, Head and Hand | `SizeChartData`, `SizeMeasurement` |
| Abbreviations | Resource-backed localized meanings/descriptions and case-insensitive search; current knitting/crochet selector returns the same dataset | Abbreviation data/screen |
| Chart Symbols | 24 resource-backed, code-drawn right-side reference symbols; four-/six-stitch cables have wider canvases; pattern-specific keys still take precedence | `ChartSymbolData`, `ChartSymbolScreen` |

Measurements keep raw edit text and canonical `Double` millimeters/counts-per-millimeter in `GaugeViewModel` and `SavedStateHandle`. Unit switches preserve canonical values; stitch and row density keep separate manual/swatch provenance. Density is count/length, size is count/density, and adjusted count is actual density times original pattern size. Display rounding never feeds arithmetic. Whole-count conversion rejects nonfinite/negative/overflow values; some results legitimately round to zero.

Regex paste is entitlement-gated using current `isPro`, separate from the feature enum. `InstructionParser` accepts at most 10,000 characters, supports selected English increase/decrease/gauge/swatch phrases, and returns `input_too_long` beyond its limit. Per-inch gauge is converted to the per-four-inch representation. Localized output does not establish arbitrary-language parsing support. There is no model/AI parser.

Sources: [MeasurementCalculator.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/MeasurementCalculator.kt), [GaugeViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/gauge/GaugeViewModel.kt), [IncreaseDecreaseCalculator.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/IncreaseDecreaseCalculator.kt), [CastOnCalculator.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/CastOnCalculator.kt), [YarnEstimator.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/YarnEstimator.kt), [InstructionParser.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/InstructionParser.kt).

#### Interpretation of calculation results

A cast-on desired width describes the body. With a repeat, distance is measured from the exact `Double` body count, avoiding whole-stitch prerounding that could select the wrong multiple. For example, 12.4 body stitches with repeat 8 selects 16 rather than prerounding to 12 and treating it as a tie at 8. Edge stitches are added after that selection, and reported actual width includes those edges. A nearest-repeat tie selects the valid lower alternative; a zero lower body repeat is not offered. There is no negative or overflowing total. Gauge input in inches uses an actual four-inch length, not an assumed ten-centimeter equivalent.

Yarn Estimator answers how many whole skeins to purchase for a known total length. It does not infer garment yardage from size or subtract already allocated yarn. Its total weight is the weight of the rounded-up purchase, while exact fractional skeins remain a separate result. The result uses `BadgePill(uppercase = false)` to retain the lowercase mass symbol `g`; other badge callers still default to localized uppercase. Extra-yarn advice is conditional rather than an instruction always to buy one additional skein. “Choose from My Yarn” currently navigates to My Yarn; the route does not implement automatic selection return or field prefilling.

Increase/Decrease validates arithmetic in wider integer types before converting the result. Paired decreases consume two source stitches per decrease. An M1 consumes an existing bar between stitches once, so insufficient bars return `NotEnoughBarsForIncrease` and no valid pattern, replacing the former excessive-increase warning. Flat Easy instructions reserve a final `K1` so their final M1 remains inside the edge; balanced flat instructions also retain a trailing knit group. The generated `K`, `M1` and `K2tog` notation remains knitting notation even though the application supports crochet projects. Reference data and selected parsing rules must not be described as a full pattern-language interpreter.

**Reference-data detail.** `NeedleSizeData` contains Japanese sizes 0–15 at 2.1, 2.4, 2.7, 3.0, 3.3, 3.6, 3.9, 4.2, 4.5, 4.8, 5.1, 5.4, 5.7, 6.0, 6.3 and 6.6 mm. An unavailable regional association is `—`; 2.0 or 4.0 mm is not silently assigned the nearest Japanese size. The existing US and UK/Canadian conventions and search behavior remain.

`SizeChartData` now has Baby 5, Child/Youth 8, Women 9, Men 8, Head 7 and Hand 9 rows. Baby uses 3/6/12/18/24-month labels and chest/waist/hips, with no preemie body row or head/length columns. Child/Youth retains chest/waist/hips/arm-to-underarm length, omitting height. Women spans XS–5XL and Men S–5XL with source ranges; labels no longer assert local clothing-number equivalents. Head includes Preemie, Baby, Toddler, Child, Tween, Women and Men ranges. Hand distinguishes three child age bands and Women/Men S/M/L via `SizeLabel.Resource.groupResId`. These are body measurements, not finished garment dimensions.

`SizeMeasurement` keeps lower and upper cm/inch endpoints; formatting preserves both endpoints with an en dash, uses localized decimals, and derives inches from cm/2.54 rather than copying independently rounded source inches. Integral values display without decimals; other values use one. Source attribution in the data files and saved audit is not a new external standards verification by this documentation update.

`ChartSymbolData` omits the unsupported knitwise-slip entry; abbreviation teaching about slipping remains a separate reference. The renderer distinguishes twisted loops, centered/directional decreases, branched M1/lifted increases and KFB, and a detached purl dot for P2tog. Cable canvases are 64 or 96 dp wide by 32 dp high, with four/six cell ticks and an uninterrupted foreground band indicating the held-front group. Single-cell symbols remain 32×32 dp. The adopted right-side convention is described in the localized disclaimer; the app does not assert one universal pattern key.

## Insights and activity

### Insights

Insights combines completed sessions, projects, and completion events into a single `InsightsUiState`. Repository flows expose loading explicitly; seeded empty lists cannot prematurely replace the skeleton. Heavy aggregation runs upstream on injected IO. Time, neutral counter progress and completion counts/history remain basic data views. Charts and the completion timeline use `INSIGHTS_CHARTS`; the project activity fabric additionally requires the All Time range. Streak has its own gate rather than hiding all Insights.

The range selector offers All Time (default), This Week, and This Month; the project pill filters independently of Projects folders. Session metrics retain exact seconds/net counter deltas and each session's zone, including cross-midnight splitting. Internal `totalRows` uses wider arithmetic, but no session stores a historical counter unit. `InsightsViewModel` therefore always publishes `rowMetricsAvailable = false` and `MinutesPerRowDisplay.Unavailable`; `InsightsStatsRow` requires that availability flag before showing row/pace columns. This applies even to a currently Rows-labelled project and survives label changes/project filtering. Numeric aggregates and chart-bucket deltas remain available internally; project rows display `counter_progress_format`, not a rows plural. This is a data-provenance limit, not a Pro restriction. Empty ranges omit hero/stats/trend; absent completions omit that section.

Active dates are restricted to the selected range through **device today**, even if an already completed instant falls on tomorrow in its stored session zone. The All Time presentation first date is clamped to today, preventing a reversed start/end range. Stored zones, session timestamps, exact seconds, duration totals and project filters are preserved. A future stored-zone date may therefore contribute duration without adding an active day or a visible future chart bucket.

| Range | Chart buckets |
| --- | --- |
| This Week | Locale-first-weekday through today, daily |
| This Month | First day of current month through today, daily |
| All Time, no first session | Current-month daily axis |
| All Time, under two calendar months | Daily from first session through today |
| All Time, two through six calendar months | Locale-aligned weekly buckets, maximum 26 |
| All Time, beyond six months | Monthly buckets, maximum 12 |

Zero intervals remain in the axis; sparse work is not compressed into adjacent active-only columns. A meaningful chart needs positive total minutes and more than a single active bucket; `INSIGHTS_CHARTS` determines chart versus Pro prompt. Week/Month trend compares the matching previous period only if the baseline reaches 30 minutes, with integer-rounded percent change. Streak is independent: consecutive local active dates ending today or yesterday, plus best historical streak, gated by `STREAK`.

The chart supports selection, tap/horizontal drag, previous/next accessibility actions, and a selected baseline marker. Labels are sparse (day maximum 5, week 4, month 6); today clearance applies beyond seven buckets. The 26-week project fabric uses locale week starts, code-drawn texture, and multiple project strips within an active date rather than one collapsed project color. Completion events include repeated complete/reopen cycles and use the selected project/period; their timeline follows `INSIGHTS_CHARTS`. Completion filtering accepts dates through the full current week/month end (All Time through LocalDate.MAX), and its axis can extend to an accepted future event; this differs from the session chart's normal through-today axis.

Sources: [InsightsViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsViewModel.kt), [InsightsSessionAccumulator.kt](app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsSessionAccumulator.kt), [InsightsChartModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsChartModel.kt), [InsightsProjectFabricModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsProjectFabricModel.kt).

### Insights resource and consistency evidence

Current `SessionDao` reads at most **256 rows per keyset query**. `CounterRepository` holds one Room transaction across session existence/project-activity facts, earliest local date and all history batches, folds/release batches, and publishes only a completed snapshot. Cancellation is checked per batch/session. Invalidation triggers replacement calculation; it does not create the consistent snapshot itself.

Week/month SQL restricts overlapping rows to the preceding comparison period or later and applies project filtering; the lower bound uses UTC+18 midnight to preserve accepted stored-zone dates. Exact zone-aware splitting stays in Kotlin. First All-Time date examines bounded candidates within 36 hours of the earliest UTC start. `InsightsActivityDates` uses sparse 4,096-day bit blocks; chart/fabric windows and active dates have their own calendar bounds, while duration/net-progress aggregation retains its existing lower-bound semantics. Future-dated contributions are not deleted from history or silently subtracted from totals; dates after today are excluded from the active-date set.

[config/insights-session-budget.md](config/insights-session-budget.md) explains ceilings and performance limits. Analysis may split up to 366 day segments/session; full 100,000-row history can require substantial aggregate work/storage. The session transaction can delay writers. Independently observed projects/completion events/gates do not make the entire combined UI one database-wide snapshot. Neither batching nor pure tests establish maximum device latency/memory.

## Widgets, preferences and Android services

### Widgets, preferences, help, reviews, and updates

The Glance counter uses four responsive size declarations: 120×48, 120×80, 160×160 and 300×160 dp. Small layout becomes stacked at sufficient height; its card now uses zero vertical padding in every small-layout branch, avoiding the previous extra inset in the 120×48 normal-font layout. Medium/large show more project/target content. English widget name/description say Project Counter rather than Row Counter. All instances follow shared application-owned active-project state rather than independently configured project databases. The resolver accepts a still-active stored project, then falls back to the latest active project, or empty/default data.

Widget actions go through the nonexported action receiver, wait for initial Pro/Billing loading before checking `WIDGET`, invoke the canonical repository count change, reread current state and refresh instances. Missing/completed projects are not countable. UI refresh failure is best effort after persistence, and a Pro-required widget opens a contextual prompt. Cold-start readiness checks have bounded waits; they are not a permanent offline purchase cache.

Widget rendering distinguishes `ProActionAccess.Unknown` from `Locked` and collects `featureAccessFlow` inside Glance content, so later access results can replace the initial loading/error state. Unknown offers a bounded purchase-restore/refresh retry; Locked directs the user to the existing Pro path. Ready Allowed/Trial state resolves the **shared widget store before stale per-instance data**, then the latest active project. `syncAll` persists shared state and updates each placed instance; one removed/failing instance must not prevent the others from refreshing. Launcher-ID discovery runs on IO because the Glance call performs synchronous framework work.

General Settings contains Language, Theme, Haptic Feedback, Keep Screen Awake and Imperial Units. Pro contains entitlement/upgrade and Restore Purchases when not purchased. Data contains Backup & Restore. Info starts with the bundled How to Use KnitTools guide, followed by external Help and Guide and Show tips again, then the privacy summary, the optional usage-statistics switch and app version. Help is an external website action with browser failure handling. There is no general Ravelry account-management or cloud-sync settings page.

`InAppReviewManager` persists action count and request state in `review_state`. It becomes eligible after 20 counted actions and no prior request. Main Activity observes eligibility and claims the request before contacting Play; failure to obtain review info resets the flag. The current eligibility predicate does not contain an independent “Pro purchased” shortcut, despite older instruction wording. Play retains control of whether review UI appears.

`InAppUpdateManager` uses flexible updates. Activity creation checks availability; resume checks an already downloaded update. Download completion produces an indefinite Restart snackbar, and Restart calls completion. Duplicate flows/listener notifications are guarded and Activity destruction cleans the listener. No Play interaction was executed here.

Sources: [CounterWidget.kt](app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt), [CounterWidgetDataResolver.kt](app/src/main/java/com/finnvek/knittools/widget/CounterWidgetDataResolver.kt), [CounterWidgetActions.kt](app/src/main/java/com/finnvek/knittools/widget/CounterWidgetActions.kt), [CounterWidgetState.kt](app/src/main/java/com/finnvek/knittools/widget/CounterWidgetState.kt), [SettingsScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/settings/SettingsScreen.kt), [InAppReviewManager.kt](app/src/main/java/com/finnvek/knittools/pro/InAppReviewManager.kt), [InAppUpdateManager.kt](app/src/main/java/com/finnvek/knittools/pro/InAppUpdateManager.kt).

## Current Ravelry integration

### Reachable Android behavior

The current Android Ravelry integration is `ui/screens/ravelry/RavelryBrowserScreen.kt`: an in-app WebView, Ravelry's own sign-in pages and a user-selected PDF download flow. Tools → Ravelry, Import from Ravelry, incoming shared Ravelry links, Ravelry URLs entered in the web-pattern editor, and saved Ravelry patterns' Open action all lead to `ravelry_browser?url={url}`.

The user signs in to the website. Android has no Ravelry password form, native OAuth state machine, access/refresh-token store, callable search client or connected-account screen. Website cookies and DOM storage can keep the site session across browser openings. The app does not implement a native Disconnect action or a separate password manager. A website search or account page is Ravelry UI rendered by WebView.

A valid optional start URL must pass `ravelryPageUrlOrNull`: HTTPS and the exact host `ravelry.com` or `www.ravelry.com`. Invalid/missing input opens the default Ravelry pattern search page. This validator is distinct from the broader rules used for page navigation and checkout detection.

Existing Saved Pattern rows whose source is Ravelry remain readable, editable through their applicable local actions, attachable as metadata and openable on the website. Their availability, designer, URL and optional HTTPS thumbnail remain metadata. Their existence does not mean the removed API client can import or refresh them. A newly downloaded PDF enters the ordinary local Save Pattern workflow.

Sources: [RavelryBrowserScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryBrowserScreen.kt), [RavelryLinks.kt](app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryLinks.kt), [NavGraph.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt).

### Navigation, settings, and accepted boundary

The WebView enables JavaScript and DOM storage. File and content access are disabled. Cookie acceptance is enabled; debugging follows `BuildConfig.DEBUG`. The inspected implementation adds no JavaScript-to-native bridge or certificate-error bypass. Disposal flushes cookies and destroys the WebView; it does not clear the website account session.

`RavelryBrowserState` is screen-owned and uses a request-keyed `rememberSaveable` Saver. It captures bounded WebView history before release/state saving rather than retaining a live WebView through recreation. The **whole serialized Bundle is capped at 32 KiB**, with an 8 KiB history/metadata budget. AndroidX bounded save is used when available; the compatibility path measures the resulting Parcel too. Both visited and original history URLs must be eligible Ravelry HTTPS pages without credentials/checkout; oversized, unsafe or unusable snapshots fall back to the last eligible page (URL length at most 2,048), or pattern search. A new request ID cannot reuse another request's state. This is bounded browser-state restoration, not a guarantee that website form state survives every renderer/provider version.

Back skips ineligible history entries. Every navigation/download/progress callback checks that its WebView is still the current instance. `onRenderProcessGone` disposes the failed renderer using previously captured metadata and shows a local error/Retry surface; Retry creates a fresh view and safe fallback instead of querying the dead renderer. Source: [RavelryBrowserState.kt](app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryBrowserState.kt).

Navigation is classified by `ravelryNavigation`:

| Request | Behavior |
| --- | --- |
| Non-HTTPS scheme, including HTTP, intent, file and content | Block |
| Ravelry checkout path/checkout host or recognized PayPal, Stripe or Braintree payment host | Open externally |
| Ordinary user main-frame navigation to a non-Ravelry HTTPS host | Open externally |
| Ravelry HTTPS navigation | Allow in WebView |
| HTTPS server redirect or subframe on another host | May remain in WebView |

Consequently, this is not a universal “Ravelry hosts only” network firewall. Redirects, frames and downloaded documents can involve external HTTPS hosts. The initial URL validator is narrower than navigation's Ravelry subdomain/payment handling; do not use one as proof of the other's restrictions. Checkout classification is also applied before loading a start address.

System Back first navigates WebView history when possible; the app top-bar back action exits the browser route. Bottom navigation is hidden on this route. External browser failures are handled by the app rather than making a native purchase flow.

[config/security-decisions.md](config/security-decisions.md) records the accepted browser approach and the requirement to obtain written Ravelry permission before Google Play release. This source inspection does not establish that permission has been obtained. It also did not verify live website login, payment behavior, cookies, CAPTCHA or downloadable access. Site behavior and provider state are external to the checked-in source.

### PDF downloads into Save Pattern

The WebView download callback accepts a PDF candidate by MIME type or suggested filename. `WebPdfDownloader` downloads to `cache/web_pdf_downloads`; `IncomingPdfViewModel` then copies through `PatternDocumentStorage.copyPdfToInternal`, which validates a readable PDF and the private-storage budget. The download cache copy is cleaned after handoff. A filename or MIME match alone does not establish that the body is a valid PDF.

The downloader uses HTTPS at every hop, resolves relative redirects and permits at most five redirects. It asks the WebView cookie store for cookies for each exact requested URL rather than forwarding a previous host's Cookie header. It supplies the browser user agent, uses 30-second connect/read timeouts, streams in 64 KiB chunks and caps bytes at 100 MiB. Declared length does not replace the streamed-byte check. Cancellation is checked while copying; connection and output cleanup run through their owning finally/cancellation paths.

The Save Pattern sheet can attach to an existing project, create a project with normal free/Pro admission, or save to Library only. Discard deletes the private pending copy when it remains unused. The file does not need to appear in the public Downloads directory. The user must already be allowed to download the document through the website session; no paywall/access-control bypass is implemented.

Source: [WebPdfDownloader.kt](app/src/main/java/com/finnvek/knittools/data/remote/WebPdfDownloader.kt). Tests use controlled request/stream behavior; they do not prove all CDN cookies, redirect chains or Ravelry account states.

### Retained backend, disconnected Android integration, and future status

The repository retains a Firebase Functions v2 TypeScript package under `functions/`, initialized in `functions/src/index.ts`, region `europe-west1`, runtime Node 22. Root `firebase.json`, `firestore.rules` and `firestore.indexes.json` describe deployable backend configuration. This package is **not called by the current Android app**. Source presence does not establish whether an older service is deployed, publicly reachable or decommissioned.

| Export | Retained implementation |
| --- | --- |
| `ravelryStartAuth` | Authenticated callable; creates expiring PKCE/state and captures connection generation |
| `ravelryCallback` | HTTP OAuth callback; validates/claims state and coordinates token completion |
| `ravelryCompleteAuth` | Callable completion of the backend-owned connection flow |
| `ravelryAuthStatus` | Callable connection status |
| `ravelryDisconnect` | Callable disconnection that advances the generation tombstone |
| `ravelryCurrentUser` | Callable current-user metadata using backend token access |
| `ravelrySearchPatterns` | Callable sanitized, paginated pattern metadata search |
| `ravelryImportPatternById` | Callable metadata lookup by validated pattern ID |
| `ravelryImportPatternByUrl` | Callable metadata lookup by validated Ravelry URL |

The backend owns Secret Manager parameters `RAVELRY_CLIENT_ID` and `RAVELRY_CLIENT_SECRET`, callback configuration, ten-minute OAuth state, token exchange and refresh. Firestore collections are `ravelryOAuthStates`, `ravelryTokens` and `ravelryRateLimits`. Android clients are denied direct access by the rules; Admin SDK access is separate. Backend callable authentication and validation are required; no assertion of deployed App Check enforcement is made.

`tokenAccess.ts` refreshes expired access tokens only after the owning limiter. Generation comparisons prevent a delayed callback/current-user/refresh write from reconnecting a disconnected account. The token store preserves generation tombstones and coordinates pending completion/rotated refresh tokens. Tokens and raw API bodies are not Android response data.

If the refresh request fails, `getUsableRavelryToken` rereads the token store to handle another request having already refreshed the same connection. This failure-path fallback accepts only a token with the same connection generation, changed access/refresh/expiry data and no currently expired deadline. A missing, unchanged, expired or different-generation token, or a failed reread, preserves the original refresh error. The successful-refresh conditional-save path is separate. [tokenAccess.test.ts](functions/src/ravelry/tokenAccess.test.ts) contains the corresponding race/error cases; none was executed in this documentation update. This remains backend-only behavior, with no Android consumer.

`rateLimit.ts` implements per-UID and backend-global fixed windows; global capacity is divided over ten shards. Compatibility checks an active legacy `<bucket>_global` window before sharded admission, and a warm process can cache a confirmed saturated window. Removing this compatibility check is a separate deployment-dependent cleanup requiring evidence that legacy writers have drained; it is not justified by Android's client removal.

Search/import sanitize metadata and do not download private or paid pattern PDFs. Backend tests include pure/unit fixtures and optional Firestore-emulator cases; the default test run can skip emulator-specific tests when the host is absent.

Historical Android Firebase anonymous authentication, Auth Tab/Custom Tab OAuth, `RavelryAuthManager`, `RavelryBackendClient`, native Ravelry search/detail/import screens and the `knittools://ravelry-auth-complete` manifest callback were removed/disconnected on the recorded October 9 transition. The backend still contains that callback string; it is not a current Android deep link. Android no longer declares Firebase Auth or Functions dependencies. Crashlytics remains a separate active Firebase integration.

The browser replaced Custom Tabs because its download callback can hand PDFs directly to Save Pattern. No plan to restore API search, native OAuth or automatic Ravelry synchronization is assumed. The historical backend/UI plans are retained design records, not the current Android feature specification.

### Reachability and security matrix

| Surface | Current Android | Retained backend | Status |
| --- | --- | --- | --- |
| Search Ravelry patterns | Website search page inside WebView | Callable search implementation | Website reachable; native API search disconnected |
| Sign in | Ravelry page plus private WebView cookies/storage | PKCE/browser-binding/token protocol | Website login reachable; native OAuth removed |
| Current user / auth status / Disconnect | No native screen or client call | Exported callable functions | Backend maintenance only; no current app account state |
| Import metadata by ID/URL | No native backend preview/import client | Sanitized metadata-only callables | Retained backend, not shipped Android API workflow |
| Download PDF | Selected website download → validated private PDF → Save Pattern | Backend never downloads PDFs | Current website workflow |
| Saved Ravelry data | Existing Room source/availability/URL/thumbnail fields | Historical source of metadata | Read/display/open remains; no automatic resync |
| `knittools://ravelry-auth-complete` | No manifest/deep-link handler | Historical completion URI constant remains | Retained protocol string, not an Android destination |
| Firebase Auth/Functions libraries | Absent from app dependencies | Admin/Functions dependencies in Node package | Do not infer Android Firebase login from backend package |

A WebView session is not encrypted app-managed OAuth token storage. No native password form, JavaScript credential bridge, certificate-bypass callback, universal cookie-clear action or backend Disconnect action is implemented. The downloader reads cookies only for the URL it is about to request. Website logout and external-browser cookies are separate sessions.

Backend source organization is `auth.ts`/`authCore.ts`/`browserAuth.ts` for callable/HTTP entry points, `oauth2.ts` for exchange/refresh, state/token stores and Firestore adapters, `rateLimit.ts` for quotas, and `patternImport.ts`/`client.ts`/`urlParsing.ts` for sanitized search/import. Files named `*.emulator.test.ts` are part of the Node test harness; their names do not prove a Firebase Emulator Suite process was run. The independently configured backend test job continues to run in CI even though Android no longer calls these endpoints. The existence of those tests says nothing about whether a cloud deployment remains active, configured or externally accessible.

## Pro, trial, and billing

### Entitlement model and feature gates

[ProState.kt](app/src/main/java/com/finnvek/knittools/pro/ProState.kt) has one entitlement tier: active trial or purchased Pro. Status is `TRIAL_NOT_STARTED`, `TRIAL_ACTIVE`, `TRIAL_EXPIRED`, or `PRO_PURCHASED`. `ProState.hasFeature` normally opens all gates in debug through `BuildConfig.DEBUG`, without changing `isPro`, Play purchase state, trial state, or purchase claims. Isolated active-project-policy tests deliberately disable that override.

| `ProFeature` | Admission / access behavior |
| --- | --- |
| `UNLIMITED_PROJECTS` | New/reactivated active projects beyond the free single-active-project policy; completed projects do not consume the quota |
| `NOTES` | New notes use; monotonic `notesCreated` preserves existing note access |
| `SECONDARY_COUNTER` | New legacy secondary-counter use; `secondaryCounterUsed` preserves existing use |
| `WIDGET` | Widget actions/presentation check cold-start-safe entitlement |
| `ROW_REMINDERS` | Creation of row reminders |
| `PROGRESS_PHOTOS` | New progress-photo capture flow |
| `MULTIPLE_COUNTERS` | Creation of additional counters |
| `SHAPING_COUNTER` | Shaping-counter creation in addition to extra-counter admission |
| `REPEAT_SECTION` | Repeat-section creation in addition to extra-counter admission |
| `PATTERN_CAMERA_SCAN` | Camera and gallery photo-to-PDF creation; name is internal, user copy says photo/PDF |
| `INSIGHTS_CHARTS` | Meaningful activity charts and completion timeline |
| `STREAK` | Current/best streak access |
| `UNLIMITED_YARN` | New Library yarn-card creation; no numeric free-card allowance |
| `INSTRUCTION_PASTE` | Regex paste in Increase/Decrease and Gauge; active trial or purchased Pro, with the shared debug override |

Ordinary main counting, basic session/completion data, folders, web-pattern metadata, project usage, PDF import/reading, bookmarks, and local backup are not globally hidden behind Pro. Annotation code inspected here has no separate annotation Pro gate. Regex paste uses `hasFeature(INSTRUCTION_PASTE)` in the UI and Gauge ViewModel, preserving trial/purchased admission while using the centralized debug override.

Onboarding Phase 7 centralizes action presentation in `proActionAccess` and `ProMarker`. Marker readiness uses `ProManager.initialStateReady`, independently of existing `initialControlsReady`. Project creation/reactivation and Save PDF use the unfiltered active count; notes and the compact counter retain their monotonic-use exceptions. Resolver values are computed in each owning composable and reused for action semantics. `ProMarkerDimens` and light/dark `proMarkerContainer`, `proMarkerContent`, and `proMarkerStitch` tokens define the shared woven label.

| Action access | Marker |
| --- | --- |
| Unknown | None until initial state is ready |
| Allowed | None for free, previously unlocked, purchased or debug-open access |
| Locked | Filled PRO label |
| Trial | Transparent TRIAL label with stitched outline |

Markers sit on Add actions, including Projects, overview, notes, photos, yarn, counters and reminders. Existing Save PDF loading/quota behavior remains intact. The compact repeat counter, regex paste and widget action remain discoverable behind their existing pending Pro flow. Phase 9 supplies benefit-first `InstructionPaste` and `WidgetPin` prompt copy and explicit trial/purchase terms. `CounterWidgetPinHost` owns launcher support and the framework pin request; unsupported launchers receive manual instructions. Opening the compact repeat counter after trial does not increment it. Phase 11 entitlement rebalancing is excluded.

Existing data is retained when trial/purchase access ends. Notes/secondary access specifically uses persisted monotonic flags; existing counters, yarn cards and stored content remain distinct from creating new gated content. Reactivation/new-project limits are still repository-enforced. A UI Pro badge or an already-open sheet is not enforcement.

[ProManager.kt](app/src/main/java/com/finnvek/knittools/pro/ProManager.kt) combines trial/billing readiness and state. `hasFeatureAfterInitialLoad` waits for Pro and Billing initial readiness (each bounded at two seconds) before widget decisions, avoiding failure from the default pre-load state. A timeout remains a real uncertainty/failure path, not live Play proof.

### Pro presentation and trial journey

The Pro page is an editorial, cardless screen in this order: fixed-height craft illustration, heading and real personal facts when available, status pill, stitched `LaunchPriceTag` with the purchase action inside, outlined trial action, Pro Adds, trial journey, compact expandable Free Forever, and Restore purchases last. The illustration shrinks or disappears in short windows. A valid launch quote can show its original price, computed saving and deadline; those values come from Play rather than a hard-coded currency/discount. The expired-trial body appears only for expired status. Contextual prompts lead with the feature benefit and explain that the user-started 14-day trial requires no payment method and never becomes a charge. Purchased users do not receive an onboarding upsell; presentation uses loaded `ProStatus`, not debug feature unlock.

`ProPriceDetails` supplies the same quote to Pro, contextual prompts and the trial-ended notice. The isolated onboarding preview may supply a simulated offer and local purchase dialog; it is not evidence of a real product, Play price or successful purchase. Missing product/price data does not disable an otherwise eligible local trial or free use. The planned launch campaign is one calendar month after actual public release in Europe/Helsinki; the public release date remains unset in repository instructions. The personal 14-day trial is independent and does not reserve a launch price.

`TrialJourneyStateRepository.state` is nullable until all four Room observations, onboarding state and `ProManager.initialStateReady` have supplied their real values. It supplies the unfiltered active count and latest active project, plus projects, named counters, yarn cards and progress photos whose `createdAt` belongs to `[trialStartTimestamp, trialStartTimestamp + 14 days)`. The end is exclusive, so later free additions do not appear as trial activity after expiry. The calculation uses a nonnegative timestamp difference to avoid overflow. `ProjectCounterRepository.getAllCounters()` maps the new all-project DAO observation and uses the existing retry boundary. Summary wording uses localized plurals and the platform locale-aware list formatter; unknown or empty summaries remain absent.

`TrialJourneyContent` is shared by the Pro page and `TrialJourneySheet`. Its `OverviewLinkRow` actions and text progress (`trial_journey_progress`) use actual facts; the sheet uses `trial_journey_intro`. The ordinary journey offers widget pinning, adding a `REPEATING` counter, adding a project, opening this week's Insights chart and camera-to-PDF. Without an active project it offers project creation and the chart. An **onboarding-origin** start sheet omits chart/photo-PDF: it offers widget/repeat-counter/project with an active project, or only first-project creation without one. `TrialJourneyFacts.steps(fromOnboarding)` owns this difference.

Choosing a journey action hides the sheet temporarily. It returns when the user comes back, including after cancellation, until explicit Close consumes the pending journey or all its visible steps are done. `TrialJourneyNavigation` routes into existing actions; creation retains its admission/draft flow, and Counter actions wait for the intended project/readiness/modal gates. Opening a destination is not completion evidence.

`CounterDestination` owns selection of the requested project before rendering the shared Counter content or handing off a journey action. Its request identity and current-entry guards prevent a stale load from consuming a newer request. Journey requests remain pending until the matching action is handled; their saved-state payload contains only primitive fields. Missing project targets return through the existing Projects fallback.

`CounterWidgetPinHost.hasInstalledWidgets` queries the launcher's actual widget IDs and is refreshed when the UI resumes. A successful pin request is not proof of installation. Widgets have no creation timestamp, so they can complete a checklist row but never enter the timestamped trial summary. `trial_chart_seen_for_start` and `trial_photo_pdf_created_for_start` in the existing onboarding store bind the other two observations to the trial start. The chart marker requires the actual visible weekly chart and resumed lifecycle; photo-PDF completion requires a successful camera/gallery PDF attachment. Ordinary PDF import cannot satisfy it, and best-effort guidance persistence cannot change an already successful import result.

Both Pro entry ViewModels request the singleton `TrialJourneyCoordinator` only for `TrialStartResult.Started`, before returning the contextual result/resuming the pending original action; onboarding also records its origin. Already-active or failed starts do not queue another introduction. MainActivity saves/restores pending and onboarding-origin flags in its instance-state bundle. No additional DataStore is created for the process queue. Presentation waits for real facts, a resumed route and clear modal surfaces.

`TrialManager.claimTrialThreeDayNotice()` evaluates current persisted timing and claims `trial_three_day_notice_shown` in one serialized edit, only for an active untampered trial with exactly three days remaining. It returns true only after the write succeeds. The host waits for initial facts and clear startup/modal gates before claiming, and saves a pending notice across recreation. The trial-ended notice uses the same personal summary and gives Continue Free and See Pro equal visual weight. Existing explicit-launch suppression and one-time end-notice ownership remain in place.

### Trial persistence and clock handling

[TrialManager.kt](app/src/main/java/com/finnvek/knittools/pro/TrialManager.kt) implements a **14-day, user-started** local trial. It is not automatically activated by installation or backed by a server account. Preferences DataStore `trial_state` stores:

`trial_start_timestamp`, `last_known_timestamp`, `clock_tampered`, `trial_end_notice_shown`, `trial_three_day_notice_shown`, `trial_elapsed_duration_millis`, `trial_anchor_wall_clock_millis`, `trial_anchor_elapsed_realtime_millis`, and `trial_anchor_boot_count`.

A valid time snapshot has positive wall time, nonnegative elapsed realtime, and nonnegative available boot count. Clean unstarted legacy state can start only from a valid snapshot. Corruption writes tampered=true; legacy active timing or malformed partial anchors fails closed rather than trusting a reset trial.

Same-boot timing advances with nonnegative elapsed-realtime delta. A later boot uses nonnegative wall delta plus one hour per boot-count increment, accepted only when consistent with elapsed realtime. Decreasing/missing boot identity, reversed/inconsistent anchors, overflow, and wall rollback greater than one hour against last-known/start tamper-lock entitlement. `daysRemaining` uses ceiling days; active requires remaining duration and no tamper flag. Refresh delay is the smaller of 15 minutes and next elapsed-day boundary, then 15 minutes after expiry.

There is no general trial-key cleanup routine; evaluations overwrite persisted state, with absent boot anchor removed when appropriate. Manual backup excludes these keys. App reset/reinstallation/Android storage policy and live Play ownership are separate from this local implementation; do not claim server-enforced perpetual trial identity.

Clock snapshots are taken inside the serialized DataStore edit used for start/refresh. This ordering prevents overlapping operations from persisting an older snapshot after a newer one and incorrectly marking an ordinary concurrent request as clock tampering. The local trial still is not a server-backed identity or a cryptographic entitlement.

### Google Play purchase and restore

[BillingManager.kt](app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt) queries the **one-time INAPP product `knittools_pro`**. There is no subscription tier or hard-coded price. UI uses Play `formattedPrice`. `SelectedOneTimeOffer` binds formatted price, micros, currency, purchase-option ID, offer ID/token and optional launch start/end/full-price fields in one snapshot. Pending one-time purchases are enabled.

Offer selection first excludes rentals/preorders. It prefers an eligible `offerId == "launch"` with a nonblank token and finite interval `start < end`, active in `[start, end)`. Otherwise it considers non-launch offers without a time window. Ordering is deterministic: no offer ID first, then no limited-quantity metadata, purchase-option ID, offer ID, micros and token. An original full price is retained only if greater than the selected price. The legacy single-offer property is used only when the list is empty. Application language does not choose billing currency.

Resumed price surfaces request refresh through a coalesced product-query Job. Product queries carry generation and BillingClient identity; superseded responses are ignored. An expiry Job invalidates a cached launch quote and refreshes it, and the purchase tap also checks the current clock against its interval. A changed/expired quote produces review feedback and requires **another explicit purchase tap**; it must not silently launch a higher price. Loading, pending purchase and an in-flight purchase suppress duplicate launches. A failed query must not be described as a verified regular-price offer.

The Billing client connects asynchronously, enables automatic reconnection, and caps explicit connection attempts at three with two-second spacing. Initial purchase readiness has a two-second wait boundary. Restore queries owned INAPP purchases and returns Restored, Not Found, or Failed. Matching PURCHASED records unlock local entitlement immediately, before acknowledgement; PENDING emits pending state without unlocking.

Acknowledgement requires a matching purchased, unacknowledged record with a nonblank token. Retryable network/disconnected/unavailable/error/item-not-owned responses requery purchases with up to three retries five seconds apart; nonretryable failures stop. Ack failure does not immediately remove in-memory entitlement. ITEM_ALREADY_OWNED triggers requery; empty/failing restore emits an explicit already-owned restore failure.

A later successful owned-purchase query recomputes entitlement from current PURCHASED records, so absence can remove purchased access. There is no backend purchase-validation/refund-notification service. Product availability, price, Play Console configuration, purchase/acknowledgement, pending completion and revocation behavior were not tested live during this task. Play Review/Update integrations are separate from Pro purchase enforcement.

Main Activity waits for initial launch/Pro readiness before claiming a one-time passive trial-ended notice. Explicit Pro/widget/share/counter launches suppress that passive notice so it does not compete with the requested flow. Continue Free dismisses it; See Pro opens the upgrade flow. Notice claiming is persisted through `TrialManager`, not inferred from every recomposition.

`BillingManager` handles `ITEM_ALREADY_OWNED` from both the purchase-update callback and the immediate `launchBillingFlow` result by querying owned purchases. It does not grant Pro from that response code alone. A successful query containing no qualifying purchased item, or a failed query, produces the already-owned restoration failure message. Pending items do not unlock access.

Purchase queries carry a generation and client identity so an older response cannot replace a newer purchase notification. A failed query preserves previously established purchased state; a successful empty query can remove it. Acknowledgement has its own in-flight/retry tracking. These are local source guarantees; actual Play SKU availability, price, account restoration, pending payment, refunds and revocation were not exercised in this documentation task.

### Entitlement enforcement and continued access

The feature enum names use cases, not separate paid tiers. `hasFeature` currently returns the same trial/purchased decision for each feature, plus the debug-only override. There is no subscription renewal state or per-feature Play SKU.

| Operation after trial/Pro ends | Current policy |
| --- | --- |
| Use existing active projects | Existing multiple active rows are retained and usable |
| Create or reopen a project | Allowed free when zero active projects remain; another active project requires entitlement at the repository boundary |
| Read PDFs, web metadata, bookmarks and basic history | Local content remains; no global pattern-reading paywall |
| Edit existing notes / use previously used secondary | Allowed through stored monotonic usage flags |
| Operate/rename/reset/delete existing extra counters | Existing content; creation remains separately gated |
| Edit existing yarn metadata, quantities/photos/links | Existing card remains; creating a new Library card requires `UNLIMITED_YARN` |
| Maintain project yarn notes / usage amounts / folders / backup | No separate Pro gate for these local data operations |
| Create reminder/photo/image-PDF or use widget | Owning operation checks the corresponding feature/admitted action |
| View advanced Insights charts/timeline or streak | Presentation follows `INSIGHTS_CHARTS` / `STREAK`; underlying basic records are preserved |

Contextual prompts retain pending user actions so that a successful trial activation can resume the requested operation. A stored admission for an in-flight camera/photo/editor action has a narrow initiating context; it is not permission to bypass unrelated future creation checks. UI badges and debug feature unlock must never be described as a real Play purchase.

## Manual backup and replacement restore

### Workflow and archive contract

[BackupScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/backup/BackupScreen.kt) and [BackupViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/backup/BackupViewModel.kt) expose user-selected SAF export and import preview. Confirmed restore **replaces all current project data**; it is not a merge. Preview/confirmation state is separate from a live mutation.

[BackupRepository.kt](app/src/main/java/com/finnvek/knittools/repository/BackupRepository.kt) coordinates [data/backup](app/src/main/java/com/finnvek/knittools/data/backup). `.knittools-backup` is an `application/octet-stream` ZIP with format version 1, data version 1, current schema 28, `manifest.json`, 18 `tables/<table>.jsonl` streams, and content-addressed `files/<sha256>.bin` entries. Table streams use ordered column headers and primitive row arrays. It contains logical records, not a copied SQLite database/WAL.

The manifest identifies KnitTools format, creation time, app version, schema, and bounded file paths/sizes/hashes. Structural allowlists, duplicate/path checks, ZIP/ZIP64 local/central inventory agreement, trailing-data/decompression/depth checks, declared budgets, hashes, row validation, and a shadow Room database reject invalid archives before confirmation/replacement. App version is informational. The reader accepts format/data version 1 with **schema 25 through 28**; legacy counter-history headers can omit session snapshots and, for older history, linked-delta snapshots. Other versions are unsupported. SHA-256 detects inconsistent bytes but supplies no cryptographic authenticity. The archive is **unencrypted and unauthenticated**; project notes/photos/PDFs are readable by anyone who can access the file.

Included: 18 of the 19 tables, durable referenced PDFs, progress photos and yarn photos, relationships, annotation payloads, mappings/bookmarks, folders/usage/completions, and active-session recovery state. Excluded: onboarding project draft receipts, preferences/language/theme/consent, trial/Play state, credentials/WebView cookies, widget configurations/launch tokens, caches/pending capture sessions, diagnostics, and raw SQLite files. Destination settings remain destination-owned.

### Budgets and validation

| Boundary | Limit |
| --- | --- |
| Archive input and extracted bytes | 512 MiB each |
| One table / all table streams | 32 MiB / 64 MiB |
| Rows per table | 100,000 |
| Completed sessions | 100,000, shared with session persistence ceiling |
| Total rows | 250,000 |
| One JSON row / field | 2 Mi / 256 Ki UTF-16 characters |
| Durable file | 200 MiB; yarn photos retain the smaller 25 MiB boundary |
| Durable files count / aggregate | 2,000 / 448 MiB |
| Identity mapping entries | 100,000 per table / 150,000 total |
| Manifest | 512 KiB |
| ZIP entries | 2,019 |
| Free-space reserve | 32 MiB, in addition to required staging/publication bytes |

Limits are admission/streaming checks, not promises that every device can complete a maximum-sized backup. Native session writer and backup reader share the session ceiling. Validation checks trusted column headers, scalar types, nullability, bounded integers/booleans, foreign keys, folder names/normalization, yarn-ID lists, annotation payload/page budgets, file inventory and production Room constraints. Soft/embedded identities are resolved or reserved during rebasing. Do not infer that every possible semantic enum/string value is independently validated merely because SQLite import succeeds.

ZIP validation also compares local-file-header and central-directory size, compressed size, CRC and method information, including ZIP64 values and filename/offset bounds. Hashes and allowlisted names do not excuse a contradictory ZIP structure. These checks are enforced before live replacement. The hashes detect corruption/inconsistency; an attacker able to rewrite the archive and its manifest can compute new hashes, so the archive is not authenticated.

### Export snapshot and provider IO

Export freezes/stages external content before the locked database/private-file snapshot. This is **not one simultaneous snapshot across arbitrary external providers and Room**. A newly referenced external file not prestaged causes failure rather than being silently omitted. Database transaction, `PatternFileReferenceCoordinator`, yarn-photo coordination, and the backup mutex preserve the app-owned boundary.

A fully validated private archive precedes destination copy. The SAF provider can fail/retain a partial output. `BackupProviderIo` reserves a singleton atomic operation across reads/writes, rejects concurrent work before opening a descriptor, and owns its worker plus abort jobs. Daemon provider workers/closers and a watchdog are bounded by operation ownership rather than a broad application thread kill.

The no-progress timeout is **30 seconds per blocking open/read/write/flush/close**. Positive-byte progress resets it; zero-byte reads do not. Stream chunks are 64 KiB. Cancellation signals the provider and closes the descriptor; NonCancellable cleanup waits for owned worker completion before rethrowing. A provider that ignores cancellation/close can retain the reservation until process restart. This is not a guaranteed whole-operation deadline or guaranteed termination of hostile provider code.

The current provider writer rejects a known canonical `file:` URI that aliases its private source archive before opening the destination in truncating mode. This does not prove detection of opaque aliases in arbitrary external content providers. The destination can contain partial output after another failure; provider writes do not have the database transaction's rollback semantics.

### Identity rebasing and replacement transaction

Preview has a fresh selection UUID and stale-action guards. Its approval state is ViewModel memory, not a durable process-restored authorization. Export/validation can be cancelled; replacement does not allow cancellation/Back to abandon a live transaction. Source archive freezes and validates in private staging/shadow Room before confirmation, and must remain valid for the selected operation. Restoration rebases identities above live existing IDs/SQLite sequences instead of reusing old IDs into existing back stacks. It rewrites FKs, soft IDs, yarn CSV, stable document keys, annotation chart-tracker counter IDs and dependent relations coherently.

Restored files receive fresh private paths, including `pattern_pdfs/0/restore-<UUID>-<sha256>.pdf`, rather than overwriting existing files in place. Active sessions receive fresh session/recovery tokens and require review; foreign device elapsed/boot anchors are not accepted as trusted running time. Successful restore clears stale navigation state and refreshes widgets best effort.

Replacement stages new files, writes/fsyncs an atomic `restore-files.json` journal before publication, publishes new files, then clears/imports all live tables and reseeds identity sequences in **one Room transaction**. All required restored app-owned files must exist before committing. Validation/publication/transaction failure before commit preserves old database and files.

After commit, old reference-free files are deleted best effort. Cleanup/journal failure does not roll back committed data; a retained journal is recovered at startup against authoritative live references. Journal recovery must run before independent cleanup can remove files needed to resolve an interrupted restore. UI success and widget refresh are not proof of a remote provider's durability.

### Drive, Dropbox, and future synchronization

Any installed SAF provider may let users manually select a local/cloud file for PDF import or backup. There is no Drive/Dropbox SDK, OAuth, automatic synchronization, conflict resolution service, or cross-device live counter/session sync. Pattern PDF v1 import remains `OpenDocument(application/pdf)` plus grants/private copy.

[config/future-sync-spec.md](config/future-sync-spec.md) records future specification work: manual export/import or backup/restore precedes continuous sync. Background/offline/conflict handling, credentials, provider lifecycle and Pro policy need an explicit new spec. Do not market cross-device sync from a functioning file picker.

### Backup implementation ownership

| Component | Responsibility |
| --- | --- |
| `BackupRepository` | Operation/selection mutex, source staging, content locks, preview/revalidation, shadow/live transaction and widget refresh |
| `BackupArchive` / `BackupZipStructure` | Allowlisted ZIP layout, structural inventory, bounded streaming extraction/writing and hashes |
| `BackupFormat` / `BackupBudget` | Format/schema versions, table order, shared limits and overflow-safe accounting |
| `BackupTables` | Trusted-schema JSONL headers/rows, bound SQL values, content checks, clear/import order and database verification |
| `BackupFiles` | Five durable URI columns and referenced-byte export, deduplicated by content |
| `BackupIdentityMap` | Fresh identities, FK/soft-link/document-key/chart-counter rewriting and sequence reservation |
| `BackupRestoreFiles` | Fresh file plan, durable journal, publication and reference-aware post-commit cleanup |
| `BackupProviderIo` | Android descriptor ownership, single-operation admission, progress deadlines and cancellation |
| `BackupViewModel` | UI phases, selection UUID, preview lifetime, confirmation, user-visible result and back/cancel policy |

The five URI columns are `counter_projects.patternUri` (compatibility), `saved_patterns.localPdfUri`, `project_documents.localPdfUri`, `progress_photos.photoUri` and `yarn_cards.photoUri`. Export enumerates references rather than copying entire directories. A missing required referenced file fails the operation; unused/orphan files are excluded.

A successful export is a validated local archive followed by a completed provider write. A successful restore is replacement of local authoritative records/files. Neither outcome establishes cloud-provider synchronization, remote replication, encryption, authenticity, live OAuth, or a merge/conflict policy. Native content can exceed archive-specific field/table budgets, so an otherwise valid large note/history/file collection may require reducing its size before this backup format accepts it; no silent truncation is promised.

## Navigation and external entry points

### Graphs and route inventory

[Screen.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt) defines routes and `TopLevelDestination`; [NavGraph.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt) registers destinations and arguments. `TopLevelNavigation` centralizes tab switching; the backup completion action in `NavGraph` clears restored stacks. Required positive entity IDs are checked before resolving content; unavailable/deleted entities pop or return to the owning tab instead of rendering stale data.

| Graph | Start destination | Main shared state |
| --- | --- | --- |
| `projects_tab` | `project_list` | Projects-graph `CounterViewModel` |
| `library_tab` | `library` | Library-graph `LibraryViewModel`; yarn detail uses the Library parent |
| `tools_tab` | `tools` | Tool-specific ViewModels/state |
| `insights_tab` | `insights` | `InsightsViewModel`; project Session History has its own route state |
| `settings_tab` | `settings` | Settings/Backup route ViewModels |

| Destination / exact route template | Owning graph and arguments | Entry / state behavior |
| --- | --- | --- |
| `project_list` | Projects | List, folders, selection, sorting, Continue |
| `project_overview/{projectId}` | Projects; required Long ID | Project card/attachment result; selects validated project and uses overview mode |
| `counter` | Projects; no project-ID argument | Uses selected shared project after selection succeeds |
| `photo_gallery` | Projects | Selected project's gallery |
| `pattern_viewer/{projectId}?selectedProjectDocumentId={selectedProjectDocumentId}` | Projects; required project Long, optional document Long default `-1` | Project document reader; active/primary resolution when optional selection is absent |
| `session_history/{projectId}` | Registered in Projects and Insights; required Long | Project session history with route-owned ViewModel |
| `counter_history/{projectId}` | Projects; required Long | Main-counter audit history |
| `notes_editor/{projectId}` | Projects; required Long | Full-screen local editor |
| `gauge?projectId={projectId}` | Registered in Projects and Tools; optional nullable string | Measurements/Gauge with optional project context; bare `gauge` is valid |
| `library` | Library | Library hub |
| `saved_patterns` | Library | Saved-pattern list/selection |
| `saved_pattern_detail/{savedPatternId}` | Library; required Long | Observed detail; unavailable rows leave |
| `library_pattern_viewer/{savedPatternId}` | Library; required Long | Requires a local PDF; Library master layer |
| `my_yarn` | Library | Yarn list/selection |
| `yarn_card_detail/{cardId}` | Library; required Long | Observed card through Library parent state |
| `all_photos` | Library | Project-filtered photo list/selection |
| `tools` | Tools | Calculator and Ravelry launcher |
| `increase_decrease` | Tools | Increase/decrease tool |
| `cast_on` | Tools | Cast-on tool |
| `yarn` | Tools | Yarn Estimator |
| `needles` | Library | Needle conversion/reference |
| `size_charts` | Library | Measurement categories |
| `abbreviations?craftType={craftType}` | Library; default `KNITTING`, domain fallback | Craft-context abbreviation reference |
| `chart_symbols` | Library | Chart-symbol reference |
| `insights` | Insights | Aggregated metrics/filter/chart state |
| `settings` | Settings | Preferences, Pro, data, information |
| `guide` | Registered in Settings and Projects | Bundled guide root; original tab remains selected |
| `guide/{topic}` | Registered in Settings and Projects; stable topic key | Local topic; unknown key falls back to root |
| `backup` | Settings | Manual export/preview/confirmed replacement |
| `pro_upgrade` | Global | Trial, Play price/purchase, restore, status |
| `ravelry_browser?url={url}&requestId={requestId}` | Global; optional encoded URL and request identity | Validated start page or default Ravelry search page; different incoming requests have independent restoration identity |
| `web_pattern_editor?origin={origin}&projectId={projectId}&patternId={patternId}` | Global dialog destination | `FormSheetDialogHost`; manual/share, project, or edit context validated by editor state |

The web editor accepts manual/share context without an entity ID, project context with project ID, and edit context with pattern ID. Invalid combinations do not grant arbitrary entity mutation. Query values are encoded by route builders. Project/session/gallery/reader routes must use their validated selection rather than a last-known unrelated project.

### Back stack, visibility, and ownership

- Tab switching uses single-top navigation, saved/restored tab state, and pop-up-to the root start destination. Selecting the current tab is a no-op. Successful replacement restore clears saved tab stacks and resets to Projects so stale pre-restore IDs cannot select restored content.
- Project overview and Counter share the Projects-scoped `CounterViewModel`; matching reciprocal navigation reuses a previous destination for the same project where possible. This is not a second independent project editor.
- Counter selection/launch is asynchronous. The route waits for validated selection instead of briefly rendering the previous project. Invalid required IDs/missing rows use a fallback pop or owning-tab route. PDF and annotation ViewModels are route-scoped and bound to document/layer context.
- Bottom navigation is hidden for Pro Upgrade, Ravelry, project/Library PDF readers, Notes, Web Pattern Editor, and Backup. It stays visible on the other registered screens, including overview, Counter, calculators, and histories.
- Ravelry system Back first uses WebView history when available; its toolbar Back exits the route. PDF Back respects active reading/markup behavior. Sheet/dialog dismissal follows pending/busy state rather than always cancelling in-flight persistence.
- List selection in Saved Patterns, My Yarn, and All Photos is cleared when leaving its route. Folder state and global completed visibility have the different lifetimes described above.
- Incoming-PDF and Library attachment success open the overview. The web editor's Open Project completion also calls `openProjectOverview`; the Yarn detail's linked-project action and Insights project actions use the internal counter-launch path. Do not assume every attachment-related action has the same destination.

### Activity intents and widget launch

`MainActivity` is an exported single-top activity with launcher, PDF Open/Share, and `ACTION_SEND text/plain` filters. `onNewIntent` routes through the same incoming handlers. PDF handling resets the consumed intent action to `ACTION_MAIN`, clears data/type/`EXTRA_STREAM`, and tracks consumption separately from the mutable Intent, because Activity restoration can supply the original system Intent again. There is no registered native Ravelry OAuth completion URI.

PDF acceptance checks explicit/resolved MIME for VIEW, or PDF SEND with an actual stream. Text sharing is bounded and parsed before a browser/editor decision. App-local counter launches use `CounterLaunchRequest` / `CounterLaunchTokenStore`: a one-time stored token admits an app-created launch request, and the requested project must resolve through application state. The token store does not encode a project-ID binding. Raw externally supplied project-ID/widget extras are not treated as sufficient authorization. Tokens are navigation aids, not public API credentials.

Sources: [MainActivity.kt](app/src/main/java/com/finnvek/knittools/MainActivity.kt), [TopLevelNavigation.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/TopLevelNavigation.kt), [CounterLaunchRequest.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/CounterLaunchRequest.kt), [CounterLaunchTokenStore.kt](app/src/main/java/com/finnvek/knittools/data/storage/CounterLaunchTokenStore.kt), [AndroidManifest.xml](app/src/main/AndroidManifest.xml).

### Incoming PDF identity, restoration and stale actions

[IncomingPdfIntent.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/IncomingPdfIntent.kt), [IncomingPdfViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/IncomingPdfViewModel.kt) and [IncomingPdfHost.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/IncomingPdfHost.kt) separate four identities: the received Activity intent, the asynchronous copy request version, the private staged PDF URI, and the eventual project or Library row. They are not interchangeable deduplication keys.

| Boundary | Current behavior | Review or regression consequence |
| --- | --- | --- |
| Same-process Activity recreation | The retained ViewModel owns `pdfIntentConsumed`; a matching original PDF intent is cleared without starting another copy | Mutating the first Intent object alone is insufficient to prevent replay |
| Saved Activity state | `MainActivity` saves the consumed flag only when `pdfIntentConsumed && !receiving`; restoration combines the saved flag with the retained ViewModel flag | An interrupted receive is deliberately left retryable after process restoration; successful provider access still depends on available URI permission and source bytes |
| Explicit new intent | `onNewIntent` resets PDF consumption before dispatch | A new deliberate share remains eligible, including after the previous PDF was discarded; the flag is not permanent URI deduplication |
| Pending PDF restoration | `SavedStateHandle` stores URI/name; restoration accepts only an existing app-owned file directly inside `pattern_pdfs/0` | A malformed saved value or another private file cannot become a pending import merely by supplying a URI |
| Overlapping copies | Increasing `incomingRequestVersion` rejects an older copy after a newer request; unused copies go through reference-aware cleanup | An old completion must not replace the newer pending document |
| Attach, new project, Library save and dismiss | The host captures the displayed `pdf.localUri` and passes `expectedPdfUri`; the ViewModel checks equality under `pendingMutex` before acting | A callback from PDF A must not save, attach or discard PDF B after replacement |
| In-flight save and another import | Save/publication and pending replacement use the same mutex; `busy` blocks duplicate save taps | The new document remains pending after the earlier operation finishes; success belongs to the initiating operation |
| Save failure or duplicate attachment | Failure keeps the pending PDF available for retry; already-attached/duplicate results open the requested project and clean only an unused copy | Failure feedback, database commitment and physical cleanup are separate outcomes |
| Abandoned staging cleanup | Application-scope cleanup calls `deleteLocalPatternFileIfUnused`; exceptions can leave an orphan | Do not promise a universal startup sweep of `pattern_pdfs/0` or delete a file that has acquired a real reference |

Successful attachment/new-project creation emits `OpenProject`; successful Library save emits `OpenSavedPattern`. The host collects these outcomes with lifecycle awareness and delegates navigation. Presentation readiness, Pro marker readiness and mutation admission are separate: a visible sheet is not proof that all controls or entitlements have loaded.

`IncomingPdfViewModelTest` covers stale callbacks, overlapping copies/publication, duplicate taps, invalid restoration, failed-save retry and owner cancellation. `PatternShareIntentRestoreTest` has two PDF scenarios: a staged PDF is not copied again when the original Intent is restored, and a discarded PDF does not reopen while a later share is accepted. Those PDF cases require `.activepolicytest` or `.notestest`. They reconstruct Activity saved state/original Intent; their existence is not evidence of a fresh OS process-kill/provider-grant test. Text-share consumption has its own coordinator and test.

### Route contracts and transient presentation

There are 30 distinct leaf route templates plus five top-level graph routes; Gauge and Session History are each registered in two graphs. A route string is not necessarily a public Android deep link. The manifest declares launcher/text/PDF intents and no browsable native OAuth/app-link route.

| Parameter | Validation / default |
| --- | --- |
| Required `projectId`, `savedPatternId`, `cardId` | Navigation Long plus positive-ID check; route loading then verifies the actual row |
| `selectedProjectDocumentId` | Optional Long sentinel `-1` at navigation boundary; selected relation must belong to the requested project |
| Gauge `projectId` | Optional nullable string parsed defensively by `optionalGaugeProjectId`; malformed/missing context does not grant a project mutation |
| Abbreviations `craftType` | String default `KNITTING`; domain parser supplies a supported craft fallback |
| Ravelry `url` | Optional nullable encoded string; screen applies its own HTTPS start-page validation |
| Editor `origin` | `manual`, `project`, `share`, `edit`; unsupported value rejected by typed route model |
| Editor IDs | Manual/Share require no positive IDs; Project requires project-only ID; Edit requires pattern-only ID |

Project content sheets, photo/PDF preview dialogs, image import surface, reminder/counter forms, layer/bookmark sheets and contextual Pro prompts are presentation state within routes, not additional `Screen` routes. Photo Gallery intentionally has no ID parameter and reads the Projects-scoped selected project. A new route must choose its state owner explicitly rather than accidentally acquiring a second CounterViewModel.

During async selection the route checks its originating back-stack entry before navigating. Required-argument fallback pops if possible and otherwise opens the owning tab. Counter/overview reciprocal navigation pops the previous matching destination when appropriate. Restoring a full backup clears all saved top-level stacks and replaces the current stack with Projects; file/ID rebasing also protects late old actions.

## Visual system and accessibility

### Identity and theme ownership

The implemented visual identity uses warm parchment/light olive surfaces or dark olive surfaces, an orange main action, condensed readable type, stitch graphics, and fabric textures. Current layouts take precedence over older Fable/Projects design plans; the product guidance calls this “70s Craft Revival.” [CLAUDE.md](CLAUDE.md) supplies product/visual guidance; code owns actual rendering.

[KnitToolsTheme in Theme.kt](app/src/main/java/com/finnvek/knittools/ui/theme/Theme.kt) provides Material 3 plus project-specific extended colors. Light is the preference default; Light, Dark, and System are selectable. Colors are fixed project palettes, not wallpaper-derived dynamic color. Use semantic tokens instead of scattering raw colors.

### Palette and semantic colors

[Color.kt](app/src/main/java/com/finnvek/knittools/ui/theme/Color.kt) and `Theme.kt` are the palette/mapping authorities. The following values are hexadecimal sRGB, not contrast certification.

| Token / role | Dark | Light |
| --- | --- | --- |
| Background | `#1E1E12` | `#E8E4D0` |
| Surface | `#2E2E20` | `#D2CDB5` |
| Surface high | `#3A3A2A` | `#BBB59A` |
| Surface medium-high | — | `#C8C3A8` |
| Surface highest | `#454535` | `#A49D80` |
| Primary action | `#C45100` | `#C45100` |
| Primary container | `#D4722A` | Same primary-container token |
| On primary | `#FFFFFF` | `#FFFFFF` |
| On accent | `#161610` | `#161610` |
| Secondary | `#93AE4F` | `#394B18` |
| Secondary muted | `#6B8A35` | No separate light-muted token |
| Secondary container | `#3A4020` | `#D0DDB5` |
| Tertiary | `#C9A435` | `#9A7B18` |
| Tertiary container | `#3A3520` | `#E8DFB5` |
| Primary text | `#E8E4D0` | `#2E2A1E` |
| Secondary text | `#B8B4A0` | `#4C4634` |
| Muted text | `#A8A491` | `#4A473C` |
| Dusty rose | `#B8908F` | `#9E706E` |
| Section label | Dusty rose | `#7F5452` |
| Error | `#C44D4D` | `#C44D4D` |
| Error container | `#3A2020` | `#EAD0D0` |
| Success | `#8BA44A` | `#8BA44A` |
| Divider | `#3A3A2A` | `#C5C0A8` |
| Navigation background | `#161610` | `#DDD8C3` |
| Navigation inactive text | `#B0AC92` | `#5A5440` |
| Navigation active background token | `#3A2010` | `#EAD0B5` |
| Ravelry teal | `#5F8A8B` | `#4A7172` |
| Card container | Surface high | `#D6D0B8` |
| Input field | Surface highest | `#F3F0E3` |
| Screen field | Surface highest | Light card container |
| Empty-state text | Muted text | `#6A6350` |
| Placeholder text | Secondary text | Light empty-state text |
| Action surface | `#1A2E2B` | `#C4D9D0` |
| Readable primary text | `#E8823A` | `#9A3F00` |
| Neutral yarn | `#4A4A38` | `#BDB69A` |
| Empty activity cell | `#464633` | `#AFA98C` |
| Low activity intensity | Secondary muted | `#B8C47A` |

`selectedCardContainer` uses primary at 7% alpha. Table header uses dark surface-highest/light card container. Dark activity intensity uses SecondaryMuted/Secondary/Tertiary/PrimaryContainer; light uses LightActivityLow/Secondary/Tertiary/Primary. Material foreground/background mappings and extended tokens are separate; e.g. the light secondary is a deliberately dark foreground.

The deterministic project/yarn palette in `YarnColorPalette` has eight slots. Dark: `#CC6821`, `#8BA44A`, `#C9A435`, `#B8908F`, `#A57B5D`, `#5E8D7D`, `#9A82AA`, `#B57357`. Light: `#C45100`, `#687A38`, `#8A7024`, `#966765`, `#976949`, `#517B6D`, `#836A93`, `#A85A3A`. Entity-derived stable selection makes color consistent across project/yarn/fabric views. Static color tests do not prove a complete rendered screen passes contrast requirements.

### Typography, shapes, dimensions, and assets

[Type.kt](app/src/main/java/com/finnvek/knittools/ui/theme/Type.kt) uses **Barlow Semi Condensed**, not Outfit. Five bundled static TTF weights are Regular 400, Medium 500, SemiBold 600, Bold 700, ExtraBold 800, paired with system sans fallback. The SIL OFL license is [barlow_semi_condensed_ofl.txt](app/src/main/assets/licenses/barlow_semi_condensed_ofl.txt).

| Bundled font file under `app/src/main/res/font` | Weight |
| --- | --- |
| [barlow_semi_condensed_regular.ttf](app/src/main/res/font/barlow_semi_condensed_regular.ttf) | 400 |
| [barlow_semi_condensed_medium.ttf](app/src/main/res/font/barlow_semi_condensed_medium.ttf) | 500 |
| [barlow_semi_condensed_semibold.ttf](app/src/main/res/font/barlow_semi_condensed_semibold.ttf) | 600 |
| [barlow_semi_condensed_bold.ttf](app/src/main/res/font/barlow_semi_condensed_bold.ttf) | 700 |
| [barlow_semi_condensed_extrabold.ttf](app/src/main/res/font/barlow_semi_condensed_extrabold.ttf) | 800 |

| Material typography | Size / weight | Letter spacing where explicit |
| --- | --- | --- |
| Display large / medium / small | 57 Bold / 45 Bold / 36 SemiBold sp | Large `-0.25 sp` |
| Headline large / medium / small | 32 Bold / 28 SemiBold / 24 SemiBold sp | Default |
| Title large / medium / small | 22 SemiBold / 16 SemiBold / 14 Medium sp | Medium `0.15`, small `0.1 sp` |
| Body large / medium / small | 16 Regular / 14 Regular / 12 Regular sp | `0.5` / `0.25` / `0.4 sp` |
| Label large / medium / small | 14 SemiBold / 12 SemiBold / 11 SemiBold sp | `0.1` / `0.5` / `1.5 sp` |

Project-card name is 18 sp; Continue count 22 sp Bold; extra-counter name 17 sp and value headline-medium Bold. Project names are never uppercased. `SectionLabel` uppercases ordinary section labels; `ProjectNameLabel` preserves user content. Main counter fits from 115 sp down to 48 sp with bounded fitting iterations. Insights hero is 76 sp ExtraBold (minimum 52), secondary 46 Bold, units 26/20; stat values 20 Bold (minimum 14), axis 13. Width fitting is implemented behavior, not proof that all locale/font combinations remain comfortably readable.

[Shapes.kt](app/src/main/java/com/finnvek/knittools/ui/theme/Shapes.kt) uses 8/12/16 dp small/medium/large radii. Pills use 50% rounding; overview hero is 24 dp. Shared spacing is 4/8/12 dp, common content padding 16/24 dp, icons 20 dp. [ComponentDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/ComponentDimens.kt), [CounterDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/CounterDimens.kt), [ProjectListDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/ProjectListDimens.kt), [ProjectOverviewDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/ProjectOverviewDimens.kt), and [InsightsDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/InsightsDimens.kt) own screen constants.

| Component / layout | Important dimensions |
| --- | --- |
| Form sheet | Horizontal 20 dp, bottom 32 dp, content gap 12 dp, actions minimum 48 dp |
| Sheet choices | Minimum 56 dp height, 16×12 dp padding |
| Project list | 16 dp sides; cards gap 10, padding start/vertical/end 10/10/14 |
| Project thumbnail | 72 dp normal / 56 compact, radius 14, text gap 14 |
| Continue hero | Padding 20, gap 12, action 72 dp touch / 64 visual |
| List bottom | 112 dp clearance for New Project button (64 visual, 16 margin) |
| Progress | 4 dp track, on-surface track alpha 0.15 |
| Counter | 24 dp horizontal; hero arrangement/height is measured; regular control width maximum 360 dp, large variant scales by 1.4 |
| Main +/- | 144 dp targets with 125/123 dp image visuals |
| Undo | 92 dp target/visual; action gap 16 |
| Extra-counter card | Minimum 88 dp, radius 16, padding 22×18 |
| Extra +/- and overflow | 56 dp touch / 42 visual; overflow 48 |
| Overview | Sides 16, photo hero 210 / no-photo 96, rows 56, radius 24 |
| Overview yarn/steppers | Swatch 48, stepper visual 44 / touch 48 |
| Content glyph / empty state | Row glyph 28, empty graphic 96, padding 32, gap 24 |

Counter images (`counter_plus_button.webp`, `counter_minus_button.webp`, `counter_undo_button.webp`, `counter_continue_button.webp`) and splash artwork are density-independent raster assets. Launcher assets have density variants. `CrossStitchIcon`, `CrossStitchGlyph`, `CrossStitchDimens`, and accent mapping share project-content glyphs; `FabricSwatch` draws knitting/crochet/yarn texture in code. These graphics are not a separate remote asset service.

### Shared component contracts and screen hierarchy

- Every standard sheet uses the theme surface and `SheetTitle`. Project actions is the explicit exception whose title is a project card. Choices use `SheetOptionRow`; form actions use `FormSheetActions` and a filled confirmation button.
- `LabeledTextField` and `NumberInputField` use labels above the same frame. `ProjectYarnForm` is a shared `FormSheet`; usage uses `SheetTitle`, segmented units, and shared form actions. Unit text is not duplicated in labels and values.
- Plain confirmations use `ConfirmationDialog`; destructive delete/replace selects destructive styling. Custom AlertDialog remains for forms/multiple choices. `CancelButton` is always neutral; Material-dialog confirm actions are text buttons.
- `RowOverflowMenu` holds applicable row actions; completed session rows offer Delete only. `SelectionActionBar` is one low row of text actions with red delete. `ProjectFilterPill` / `FilterMenuItem` are shared by Insights and Library Photos. FilterChip is restricted to the PDF annotation palette.
- Project cards use `ProjectCard` in a shaped Surface, with thumbnail, two-line name, optional section/pattern line, progress/completion/session state, and selection. They open overview and contain no counter button. `ContinueProjectCard` is the shared working-entry card. The old cardless `ProjectListItem` description is obsolete.
- Counter's measured hero groups count/target, image-backed controls, secondary/stitch state and its own spacing. Active-session status and project content follow in the LazyColumn; a 48 dp reveal gap separates content. The layout chooses stacked, compact stacked or side by side according to actual measured fit. Undo remains with the primary controls. `ProjectContentHost(overview = true)` uses `ProjectOverviewScreen`/sections for management and the shared Continue progress card.
- Library is a destination hub with focused lists/details, not a Projects-folder view. My Yarn places status under the name and usage actions inside the card. Tools uses calculator input/result components and text actions such as Choose from My Yarn. Gauge choices and Size Chart categories use `SegmentedToggle`.
- Insights uses range/project filters, hero, compact stats/trend, chart, completion data, and activity fabric according to available data. Settings is divided into General, Pro, Data, and Info; backup belongs to Data. Notes has only the Notes top-bar title. PDF labels are cleaned by `PatternDisplayNames`.

`UiConsistencySourceTest` enforces exact component exception lists. Source-contract enforcement prevents certain structural regressions; it does not verify screenshot geometry or interaction on a device.

### Responsive layout, semantics, focus, and feedback

`AdaptiveLayoutSize` uses **measured host content constraints**, after navigation/insets, with width classes at 600 and 840 dp. A navigation rail is used at width ≥600 dp and height ≥480 dp; a short wide window uses the 64 dp `ShortNavigationBar`. `ContentWidthPolicy` caps Forms at 600 dp, ListDetail at 840 dp and Charts at 1,200 dp. Separating/occluding folds select one unobstructed pane, preferring top/start when areas tie. Centered titles share their content column.

`rememberCounterHeroLayout` measures the caption, number, targets, controls, optional secondary and stitches. It uses normal stacked spacing if it fits, compact spacing next, then side by side only if neither fits and content width is at least **552 dp**. Otherwise compact content can scroll. A candidate at least 600×840 dp uses 1.4× controls only if that larger stacked layout fits. Extra stacked whitespace is capped at 64 dp. Side-by-side numbers also fit against row height, and Undo sits below the step ring between primary buttons. This replaces the old fixed 360 dp hero description.

Counter content tiles use two-plus-two-plus-one at narrow width and three-plus-two from 480 dp. Projects cards can use two columns from 760 dp list width; short windows move New Project to the top bar. Project-card compact mode uses width <320 dp or font scale above `FontScaleThresholds.COMPACT` (1.3); `LARGE` is 1.5. `SegmentedToggle` uses radio-button semantics, selectable grouping and minimum 48 dp targets; its shared implementation owns wrapping/grid behavior. Gallery import has a compact-window SAF fallback. These are current source contracts, not a new visual test run.

Onboarding/Pro illustrations shrink to 84 dp below 480 dp content height and disappear below 360 dp, with large-font handling through shared thresholds. Short onboarding price actions can sit side by side. Short PDF viewers use 48 dp top bars and reduced bottom padding; annotation palettes are capped at Form width. [AdaptiveLayout.kt](app/src/main/java/com/finnvek/knittools/ui/layout/AdaptiveLayout.kt), [CounterHeroLayout.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterHeroLayout.kt), [FontScaleThresholds.kt](app/src/main/java/com/finnvek/knittools/ui/theme/FontScaleThresholds.kt) and screen dimension owners define the exact rules.

Bottom navigation has five icon/uppercase-localized labels, theme container, primary selected color, and inactive semantic colors. Its Material indicator is the container color, despite an active-background token existing. Label measurement considers translated labels and reduces size in 0.5 sp steps down to its configured minimum, then uses one-line ellipsis. This is a source mechanism, not a large-font legibility guarantee.

Image-backed controls expose Button roles and localized descriptions; decorative image/glyph descriptions are null. `LabeledCounterImageButton` keeps the clickable action before `clearAndSetSemantics` so the combined label/Pro badge does not remove activation. Form fields use keyboard/IME and focus handling through their shared components; route changes, sheet closure, and picker outcomes are state-owned. Custom chart selection exposes next/previous actions. Gesture-based PDF editing, viewport focus after bookmark jumps, and system Back require runtime testing beyond semantics inspection.

Counter image press feedback animates in 90 ms with scale/translation (up to 1.8% shrink and 1.5 dp shift), and disabled alpha 0.62. Shared result transitions use 6 dp displacement and 200/150 ms enter/exit constants. Startup considers system animation availability. A universal custom reduced-motion/focus-ring implementation has not been established by this inspection.

Root `PreferenceAwareHapticFeedback` gates Compose/default long-press haptics using the DataStore preference; explicit counter vibration is also preference-gated. Keep-screen-awake is opt-in. The source manifest does **not** lock activity orientation. Window resize, rotation and restoration belong to the adaptive layout/navigation contracts. Splash styling is fixed by Android theme resources while app preference/theme initialization resolves; it is not evidence of an independently themed night splash.

Source-level adaptation and test availability are recorded here without treating them as fresh visual proof. Existing runtime evidence is consolidated under Testing and evidence.

Current layouts use a wrapping `FlowRow` for Continue-card count/target content, a weighted label group that keeps the Settings unit switch visible, and measured chart-axis labels that omit colliding ticks while reserving space for Today. Weekly chart semantics now have their own weekly resource rather than falling through to the monthly description. These are source-observed adaptations; no fresh rendering claim is made.

Shared `FormSheet` uses a current dismissal flag in `confirmValueChange` and blocks Back while cancellation is disabled. A pending save can therefore retain its editor and draft across attempted hide gestures. PDF page bitmap state is keyed by renderer and page, so a page transition does not publish the preceding page's retained bitmap as the new page. Bookmark viewport focus is consumed only when its target page is rendered.

**Project-specific validation policy:** the previously completed 320 dp, 200% font, combined and TalkBack checks must not be repeated without the user's explicit request for those checks. This documentation pass inspected source and existing evidence only.

### Screen composition and interaction reference

| Screen family | Implemented hierarchy and controls | State / adaptation details |
| --- | --- | --- |
| Onboarding | Welcome → Craft → Needs → Counter → Toolbox → Project → Ready → Price | Persisted draft and transactional creation receipt; shared illustrations/forms/price sections; explicit free exit |
| Projects | Folder title selector, Continue hero, active/completed sections, tonal `ProjectCard` list/grid, New Project image pill | SQL order retained; measured list width and height decide grid/add placement; selection uses the same cards |
| Project overview | Transparent top bar, latest photo or 96 dp fabric placeholder, name/status, Continue card, hairline sections | Photo hero 210 dp; project name is not uppercased; menu owns lifecycle/manage actions |
| Counter | Unit and fitted number, target/progress, image +/-/Undo, central ×N step editor and transient signed delta, secondary/stitches, then session/content/extra-counter/reminders | Project steps 1–99; measured stacked/compact/side-by-side hero with Undo kept alongside primary controls |
| Saved Pattern detail | Name/designer, one primary open action, source and project sections, edit/delete overflow | PDF, web and retained Ravelry data choose actions according to actual file/source state |
| Yarn detail | Photo/neutral yarn placeholder, name, status segments, large quantity with steppers, hairline information sections | Title appears after name scrolls away; imported weight text remains editable |
| Library / Tools | Accent-title `HubListItem` cards; Library separates My Collection and Reference | Tools has five current entries; its historical `HomeScreen` name is not the app's start route |
| Calculator | `ToolScreenScaffold`, grouped `ToolInputCard`, labeled fields and segmented options, result or `ResultPlaceholder` | Scrollable content, localized inputs/results; Copy is explicit clipboard output |
| Insights | Range in app-bar title, right-aligned project pill, hero/stats/trend, day/week/month chart, completions and fabric as applicable | Empty sections omitted; single-project view omits redundant mix and links to Session History |
| Settings | General → Pro → Data → Info with indented dividers and actionable chevrons | Restore busy/status and consent are observable state, not optimistic visual-only toggles |
| PDF | Reading mode gives the page priority; Mark up / Done switches tool mode; layers/export in overflow, bookmarks in bottom controls | Current tool resets to Browse in reading mode; document selection owns renderer/annotation context |

Project glyphs cover the five counter content tiles and overview links; shared cross-stitch empty states accompany Photos. Fabric distinguishes knitting V stitches, crochet strands and neutral twisted yarn. Image placeholders do not infer a photographed yarn's real color. `RemotePatternImage` is reserved for validated HTTPS thumbnails and removes failed images from layout.

Form focus is explicitly managed where the workflow needs it. The shared web editor requests the missing title first for a prefilled shared URL, moves through title/URL/designer fields using FocusRequesters/IME actions, and validates before save. Recovery dialog headings request focus. Bookmark jumps publish a one-shot request that waits for the rendered page before moving viewport focus. These code mechanisms do not establish successful spoken TalkBack traversal.

Animation source includes navigation fades (150 ms exit; 100 ms entry after 150 ms delay), result transitions, counter press feedback, expanding reference rows and the splash reveal. Splash exit is 760 ms with reveal/fade constants and bypasses animation when platform animators are disabled. No comprehensive per-feature reduced-motion audit ran.

Keyboard behavior is primarily Compose field/IME and focus behavior. There is no documented desktop-style command palette or global hardware-key shortcut map. Adaptive source and test scenarios exist for short/wide/tablet/fold constraints; only actual executions establish behavior on a particular device/window/IME configuration.

## Architecture and lifetime

### Layers and canonical ownership

| Layer | Responsibility and boundaries |
| --- | --- |
| `domain/model`, `domain/calculator` | Domain rules, typed values, pure arithmetic/geometry/timing/validation; no composable persistence |
| `data/local` | Room entities/DAOs/migrations/constraints and transaction adapter |
| `data/datastore`, `data/storage`, `data/time`, `data/remote`, `data/backup` | Preferences, framework/file/provider access, clock anchors, website PDF download, bounded archive infrastructure |
| `repository` | Storage-to-domain seam; transactional writes, coherent observation, entitlement/data validation, cleanup ownership |
| `ui/screens`, `ui/components`, `ui/navigation`, `ui/theme` | ViewModels, state collection, rendering, navigation, shared visual contracts |
| `di` | Hilt database/analytics/dispatcher bindings |
| `billing`, `pro`, `widget` | Play connection/purchases, trial/feature state, Glance state/actions through repository semantics |
| `functions` | Retained backend implementation; no current Android consumer |

The app is Hilt-based, single-activity, Compose/Material 3, with Room, Preferences DataStore, Flow, and ViewModels. Compose observes state; repositories write DAOs. Avoid business rules in composables, direct UI DAO writes, a second PDF renderer, ad-hoc yarn CSV, or duplicate counter/date/annotation formatting logic.

### Startup and activity lifetime

[App.kt](app/src/main/java/com/finnvek/knittools/App.kt) initializes variant-specific diagnostics, release consent-aware analytics, billing/Pro, and application-owned cleanup. Stored app language applies asynchronously from the application scope, never through startup `runBlocking`. Backup-journal recovery precedes dependent yarn-photo pruning; stale pattern-capture cleanup uses IO. Debug `DemoDataSeeder` is a facade to `DebugDemoDataSeeder`, which performs one transaction and reuses repository writers; release is a no-op.

Billing/Pro initial state and widget refresh are asynchronous; widget refresh after readiness is best effort. Startup cleanup catches ordinary failures while preserving cancellation. Android `onTerminate` is not a reliable process-death cleanup mechanism. Main Activity resolves preferences/theme, splash, single-top intents, per-app locales, keep-awake and navigation; lifecycle callbacks do not persist completed work sessions.

MainActivity distinguishes same-process Activity recreation through the retained non-configuration instance. Such recreation receives no new splash window, so its splash gate starts resolved. A cold start or process restoration has no retained instance and still waits for the real splash exit callback. Startup reporting and passive trial notices share this gate; a saved-state Boolean must not bypass the actual splash after process death.

### Coroutines, Flow, state restoration, and concurrency

[DispatchersModule.kt](app/src/main/java/com/finnvek/knittools/di/DispatchersModule.kt) provides `@IoDispatcher` for boundary-crossing blocking work and `@ApplicationScope` with `SupervisorJob` and Main dispatcher. Billing/Pro own their process-lifetime state scopes. ViewModels use `viewModelScope` for screen work; application scope is reserved for final persistence/cleanup that must outlive the ViewModel, with injected IO still selected for blocking operations.

UI-facing Room flows apply `retryOnRepositoryReadFailure` **after mapping**. The retry boundary preserves cancellation and caps backoff; a read or mapper failure cannot permanently terminate state sharing. Loading is represented explicitly where empty defaults would be misleading. Typed operation results distinguish validation/stale/missing/entitlement/storage failures; expected rejection is not an invented success.

`SavedStateHandle` owns transient/restorable selection/drafts such as folder filters, Gauge numeric canonical state, pending imports and share requests. `rememberSaveable` handles appropriate presentation state, such as Library guides. Room owns project/session/document truth. Persistable provider permission is not permanent app-owned storage: transient selected images/PDFs must be copied where required.

Selected project and document changes use context/generation guards so delayed work cannot write the new selection using old draft data. Session actions use tokens; metadata attachment uses expected IDs; usage updates use revision checks. Mutexes coordinate pending PDF import, file-reference/restore operations and yarn-photo changes. These locks serve different boundaries; they are not a substitute for Room transactions.

[DatabaseTransactionRunner.kt](app/src/main/java/com/finnvek/knittools/data/local/DatabaseTransactionRunner.kt) coordinates multi-DAO repository mutations. Main counter/history/link/session/reading writes, bidirectional yarn links, document ownership, project admission, completion events, and replacement restore each keep their stated atomic boundary. File operations cannot join a SQLite transaction: cleanup inputs are captured first, database mutation commits first, then unused files are deleted best effort.

**Database failure or pre-commit cancellation leaves files intact**. A post-commit cleanup failure may leave an orphan but must not present the committed deletion as rolled back. `SavedPatternRepository.deleteLocalPatternFileIfUnused` is the shared-PDF deletion gate, coordinated with file-reference operations. Backup provider worker ownership and cancellation have additional limits described in the backup section.

### Canonical call chains

| User trigger | UI / asynchronous owner | Canonical mutation boundary | Durable outcome |
| --- | --- | --- | --- |
| Main +/−/Reset/Undo | `ProjectContentHost` → `CounterViewModel` | `CounterRepository.applyMainCounterChange` | Count/history, linked deltas, stitch reset, active-session checkpoint, active-document follow |
| Save main step (×1/×2/×5/custom) | `MainCounterStepSheet` → `CounterViewModel.setStepSize`, `viewModelScope` job/result callback | `updateProjectStepSize` → transaction → `CounterProjectDao.updateStepSize` | Existing project `stepSize` and `updatedAt` only; no count/history/session mutation |
| Widget +/− | `CounterWidgetActions` and Hilt `WidgetEntryPoint` | `applyWidgetCountChange` → main mutation | Same semantics followed by best-effort widget state refresh |
| New project | `ProjectCreationActions` / list VM | `createProject` transaction | Quota and folder checked with project/assignment insert |
| Stop / resolve recovery | Counter's session state/actions | Token-guarded `CounterRepository` session methods | Final session plus singleton deletion, or reviewed continuation |
| Complete / reopen | Project actions/list VM | Lifecycle transaction and current completion generation | Completion event appended; reopening preserves events |
| Attach PDF | Incoming/picker/reader VM | Storage preflight then Counter/Document repository transaction | Saved pattern/document/layer/metadata coherence |
| Attach web metadata | Editor / attachment VM | Expected-ID `attachSavedPatternMetadata` | Metadata link only; PDFs preserved |
| Change yarn link | Counter or yarn VM | `YarnCardRepository.updateLinkedProjectId` | Card soft link plus project CSV stay reciprocal |
| Edit project yarn note | Editor coroutine → `CounterViewModel.editProjectYarnNote`, injected IO | `ProjectYarnNoteRepository.edit` → transaction → `ProjectYarnNoteDao.update` | Existing row only; editable-field conflict detection, current Library link and usage relationships preserved |
| Save project yarn note to Library | Yarn sheet VM | `ProjectYarnNoteRepository.saveToMyYarn` | Note retained, card created/reused, existing usage identity bound once |
| Edit project amounts | `ProjectYarnUsageViewModel` | Revision-checked usage repository transaction | Canonical nullable meter amounts; no stash/counter/session side effect |
| Replace yarn photo | `YarnCardViewModel` | Photo mutex, bounded copy, repository URI swap | Old bytes removed after successful persistence |
| Draw / edit annotation | `PatternAnnotationViewModel` | Context-bound repository command at gesture end | Versioned payload in owning layer; pointer stream remains memory |
| Restore backup | `BackupViewModel` | Revalidated shadow import, fresh files/IDs, live Room transaction | All content replaced; journal recovers cleanup after interruption |

### State lifetime and recovery ownership

| State | Owner | Survives / does not imply |
| --- | --- | --- |
| Counts, notes, relationships, completed sessions/events | Room | Durable after committed writes; UI state is not an alternate persistence source |
| Active session and recovery decisions | Singleton Room row and stable tokens | Background/process recreation via anchors; no background ticking requirement |
| Selected project | Projects-scoped Counter VM plus its restoration logic | Shared by Counter/overview/gallery; required route IDs still revalidated |
| Main step / step-editor draft | Room `counter_projects.stepSize` / project-keyed sheet `rememberSaveable` | Saved value is durable per project; draft is separate, busy/error/job are transient, sheet disposal cancels its save job |
| Selected document / bookmarks | Reader VM and per-document Room state | Stale relation/owner is rejected; a label/order change does not change annotation identity |
| Folder filter | List VM `SavedStateHandle` | Restoration only after a real snapshot validates the ID; not a global preference |
| Gauge draft | Route VM `SavedStateHandle` | Raw text plus canonical values; no automatic project or preference writes |
| Notes draft/base | Editor `SavedStateHandle` | Local restoration and repository merge; final flush is best effort |
| Image/PDF draft | Import VM saved state and validated private staging files | A lost/invalid file cannot be restored merely because a URI string survived |
| Incoming PDF intent consumption | Activity-owned incoming-PDF ViewModel plus Activity saved-state flag | Retained through rotation; completed handling survives saved-state restoration, while an interrupted receive remains retryable and a new intent resets consumption |
| Library reading guides | `rememberSaveable(patternUri)` | Presentation restoration; no project/document schema row created |
| Play product/entitlement | Billing/Pro process state and Play query | Readiness is explicit; not copied in a backup |
| Website login | WebView cookies/storage | Website session retained locally; not native OAuth or app identity |
| Restore preview approval | Selection-owned VM memory | Process recreation does not recreate approval to replace content |
| Onboarding draft / committed receipt | `onboarding_state` DataStore / Room `onboarding_projects` | Restores the journey; transactional receipt prevents duplicate project creation after interrupted draft persistence |
| Tip presentation / per-project celebration | Process `TipSession` / `OnboardingStateRepository` | One active presentation and budget; durable identities survive appropriate resets |
| Trial-start sheet | `TrialJourneyCoordinator` plus Activity saved state | Pending/origin survive recreation; choosing an action does not consume the journey |
| Browser history / renderer | Request-keyed `RavelryBrowserState` Saver | Bounded safe history or fallback crosses recreation; live WebView is destroyed, not retained |

Application `onTerminate` is not guaranteed on Android process death. Durable correctness must come from committed data and explicit recovery, not a teardown callback. Temporary UI scopes can cancel; token/revision/generation guards prevent their late results from mutating a different current entity.

Expected validation/missing/stale/feature failures use typed result models. Read failures may retry after mapping; writes are not universally blindly retried. Some final cleanup/flush paths deliberately suppress ordinary errors and can leave an orphan or unsaved draft. Describe the owning path's result rather than promising every failure is visible or every operation automatically recovers.

## Persistence reference

### Room ownership and constraints

[KnitToolsDatabase.kt](app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt) declares schema 28 and 19 entities. [DatabaseModule.kt](app/src/main/java/com/finnvek/knittools/di/DatabaseModule.kt) opens `knittools.db`, registers every manual migration plus Room auto-migrations, and installs constraints. Destructive fallback is not configured. Exported JSON schemas 1–28 are available in [app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase](app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase). Version 28's identity hash is `da23c177722e708f242c5d2998ed91e0`; schema 28 adds the nullable `counter_history.sessionUndoJson` session snapshot (`MIGRATION_27_28`).

| Table | Entity / DAO | Canonical writer / meaning |
| --- | --- | --- |
| `counter_projects` | `CounterProjectEntity` / `CounterProjectDao` | `CounterRepository`; project lifecycle, main/secondary/stitch counts, notes, compatibility reader fields |
| `counter_history` | `CounterHistoryEntity` / `CounterProjectDao` | Main-counter transaction and validated Undo |
| `yarn_cards` | `YarnCardEntity` / `YarnCardDao` | `YarnCardRepository`; stash metadata/quantity, photo and reciprocal links |
| `sessions` | `SessionEntity` / `SessionDao` | `CounterRepository`; completed exact session records |
| `active_sessions` | `ActiveSessionEntity` / `SessionDao` | `CounterRepository`; one active session, timing/recovery/token anchors |
| `row_reminders` | `RowReminderEntity` / `RowReminderDao` | `ReminderRepository`; project in-app reminders |
| `progress_photos` | `ProgressPhotoEntity` / `ProgressPhotoDao` | `ProgressPhotoRepository`; project image records |
| `project_counters` | `ProjectCounterEntity` / `ProjectCounterDao` | `ProjectCounterRepository`, canonical main transaction for derived/linked updates |
| `project_yarn_notes` | `ProjectYarnNoteEntity` / `ProjectYarnNoteDao` | `ProjectYarnNoteRepository`; lightweight project yarn metadata |
| `saved_patterns` | `SavedPatternEntity` / `SavedPatternDao` | `SavedPatternRepository`; Library source metadata and local PDF references |
| `pattern_annotation_layers` | `PatternAnnotationLayerEntity` / `PatternAnnotationLayerDao` | `PatternAnnotationLayerRepository`, transactional attachment |
| `pattern_annotations` | `PatternAnnotationEntity` / `PatternAnnotationDao` | `PatternAnnotationRepository`; bounded versioned normalized payloads |
| `pattern_bookmarks` | `PatternBookmarkEntity` / `PatternBookmarkDao` | `PatternBookmarkRepository`; project/document-key reading positions |
| `project_documents` | `ProjectDocumentEntity` / `ProjectDocumentDao` | `ProjectDocumentRepository`; project PDF relation and reader-state authority |
| `project_folders` | `ProjectFolderEntity` / `ProjectFolderDao` | `ProjectFolderRepository`; metadata organization |
| `project_folder_assignments` | `ProjectFolderAssignmentEntity` / `ProjectFolderDao` | Folder repository / transactional project creation |
| `project_yarn_usage` | `ProjectYarnUsageEntity` / `ProjectYarnUsageDao` | `ProjectYarnUsageRepository`; canonical independent meter amounts |
| `project_completions` | `ProjectCompletionEntity` / `CounterProjectDao` | `CounterRepository`; every completion transition |
| `onboarding_projects` | `OnboardingProjectEntity` / `CounterProjectDao` | `CounterRepository.createOnboardingProject`; idempotent draft receipt |

The table dictionary below reproduces column affinity, nullability, default, key, index, and FK metadata from the current exported schema. Kotlin booleans/enums/timestamps can use INTEGER/TEXT storage; affinity alone does not replace domain validation. Auto-generated primary IDs are distinct from the active-session singleton or project-keyed folder assignment.

**Additional installed constraints not fully expressed by the schema JSON:**

- `ActiveSessionSchemaConstraints` insert/update triggers require singleton ID 1; project deletion cascades its active row.
- Annotation layers require exactly one owner (project XOR saved pattern); owner+document-key uniqueness and triggers restrict a project to at most one active layer. Layer deletion cascades annotations.
- `ProjectDocumentSchemaConstraints` prevents multiple primary documents. Repositories maintain the stronger invariant: exactly one primary if the project document set is nonempty.
- Folder assignments are keyed by project ID; folder deletion cascades assignments, leaving projects unfiled. Name uniqueness is stored in `normalizedName` and enforced by a unique index.
- Usage source indexes are unique per project/card and project/note; both nullable source FKs use `SET NULL`, so deleting source metadata preserves usage. Repository identity additionally coalesces a note and its saved card.

Project `linkedPatternId`, yarn `linkedProjectId`, project yarn-ID CSV, and note `savedYarnCardId` include soft relationships that must be maintained by repository transactions. Do not assume every non-null ID has an FK. Saved-pattern indexes for Ravelry/URLs/PDF are lookup indexes, not a comprehensive SQL uniqueness policy.

`PreservingSQLiteOpenHelperFactory` replaces the destructive corruption callback: it closes and reports corruption while preserving database files for explicit recovery. This is data preservation, not automatic repair or a guarantee that a corrupt database can open.

Default document reader state is page 0, null mapping, horizontal disabled/Y 0.5/follow true, vertical disabled/X 0.5. Canonical document state supersedes retained project columns. Stable document keys, not a mutable file label or relation order, bind annotation/bookmark ownership.

### Complete table dictionary

#### counter_projects

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `name` | TEXT | No | — |
| `count` | INTEGER | No | — |
| `craftType` | TEXT | No | `'KNITTING'` |
| `mainCounterLabelType` | TEXT | No | `'ROWS'` |
| `mainCounterCustomLabel` | TEXT | Yes | `NULL` |
| `readingLineEnabled` | INTEGER | No | `0` |
| `readingLineYFraction` | REAL | No | `0.5` |
| `readingLineFollowCurrentRow` | INTEGER | No | `1` |
| `verticalReadingGuideEnabled` | INTEGER | No | `0` |
| `verticalReadingGuideXFraction` | REAL | No | `0.5` |
| `secondaryCount` | INTEGER | No | — |
| `secondaryCounterUsed` | INTEGER | No | `0` |
| `stepSize` | INTEGER | No | — |
| `notes` | TEXT | No | — |
| `notesCreated` | INTEGER | No | `0` |
| `createdAt` | INTEGER | No | — |
| `updatedAt` | INTEGER | No | — |
| `sectionName` | TEXT | Yes | `NULL` |
| `stitchCount` | INTEGER | Yes | `NULL` |
| `isCompleted` | INTEGER | No | `0` |
| `totalRows` | INTEGER | Yes | `NULL` |
| `completedAt` | INTEGER | Yes | `NULL` |
| `yarnCardIds` | TEXT | No | `''` |
| `linkedPatternId` | INTEGER | Yes | `NULL` |
| `patternUri` | TEXT | Yes | `NULL` |
| `patternName` | TEXT | Yes | `NULL` |
| `currentPatternPage` | INTEGER | No | `0` |
| `patternRowMapping` | TEXT | Yes | `NULL` |
| `stitchTrackingEnabled` | INTEGER | No | `0` |
| `currentStitch` | INTEGER | No | `0` |
| `targetRows` | INTEGER | Yes | `NULL` |

Indexes: `index_counter_projects_linkedPatternId` (`linkedPatternId`).

Foreign keys: none declared.

#### counter_history

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `projectId` | INTEGER | No | — |
| `action` | TEXT | No | — |
| `previousValue` | INTEGER | No | — |
| `newValue` | INTEGER | No | — |
| `timestamp` | INTEGER | No | — |
| `linkedCounterDeltasJson` | TEXT | Yes | — |
| `sessionUndoJson` | TEXT | Yes | — |

Indexes: `index_counter_history_projectId` (`projectId`).

Foreign keys: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION.

#### yarn_cards

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `brand` | TEXT | No | — |
| `yarnName` | TEXT | No | — |
| `fiberContent` | TEXT | No | — |
| `weightGrams` | TEXT | No | — |
| `lengthMeters` | TEXT | No | — |
| `needleSize` | TEXT | No | — |
| `gaugeInfo` | TEXT | No | — |
| `colorName` | TEXT | No | — |
| `colorNumber` | TEXT | No | — |
| `dyeLot` | TEXT | No | — |
| `weightCategory` | TEXT | No | — |
| `careSymbols` | INTEGER | No | — |
| `photoUri` | TEXT | No | — |
| `createdAt` | INTEGER | No | — |
| `quantityInStash` | INTEGER | No | `1` |
| `status` | TEXT | No | `'IN_STASH'` |
| `linkedProjectId` | INTEGER | Yes | `NULL` |

Indexes: `index_yarn_cards_linkedProjectId` (`linkedProjectId`).

Foreign keys: none declared.

#### sessions

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `projectId` | INTEGER | No | — |
| `startedAt` | INTEGER | No | — |
| `endedAt` | INTEGER | No | — |
| `startRow` | INTEGER | No | — |
| `endRow` | INTEGER | No | — |
| `durationMinutes` | INTEGER | No | — |
| `durationSeconds` | INTEGER | No | `0` |
| `rowsWorked` | INTEGER | No | `0` |
| `zoneId` | TEXT | Yes | — |

Indexes: `index_sessions_projectId` (`projectId`); `index_sessions_startedAt` (`startedAt`); `index_sessions_endedAt_startedAt` (`endedAt`, `startedAt`); `index_sessions_projectId_endedAt_startedAt` (`projectId`, `endedAt`, `startedAt`).

Foreign keys: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION.

#### active_sessions

Primary key: `singletonId`.

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `singletonId` | INTEGER | No | — |
| `sessionToken` | TEXT | No | — |
| `projectId` | INTEGER | No | — |
| `startedAtWallMillis` | INTEGER | No | — |
| `startZoneId` | TEXT | No | — |
| `startRow` | INTEGER | No | — |
| `lastObservedRow` | INTEGER | No | — |
| `trustedLastObservedRow` | INTEGER | No | — |
| `trustedRowsWorked` | INTEGER | No | — |
| `pendingRowsWorked` | INTEGER | No | — |
| `reviewedRowsWorked` | INTEGER | No | — |
| `reviewedLastObservedRow` | INTEGER | No | — |
| `unreviewedRowsWorked` | INTEGER | No | — |
| `checkpointedDurationSeconds` | INTEGER | No | — |
| `reviewedDurationBaselineSeconds` | INTEGER | No | — |
| `segmentStartedAtWallMillis` | INTEGER | No | — |
| `segmentStartedElapsedRealtimeMillis` | INTEGER | No | — |
| `bootCount` | INTEGER | Yes | — |
| `recoveryReason` | TEXT | Yes | — |
| `recoveryIntervalToken` | TEXT | Yes | — |
| `recoverySuggestedDurationSeconds` | INTEGER | Yes | — |
| `recoveryPromptShown` | INTEGER | No | — |
| `updatedAtWallMillis` | INTEGER | No | — |

Indexes: `index_active_sessions_projectId` (`projectId`).

Foreign keys: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION.

#### row_reminders

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `projectId` | INTEGER | No | — |
| `targetRow` | INTEGER | No | — |
| `repeatInterval` | INTEGER | Yes | — |
| `message` | TEXT | No | — |
| `isCompleted` | INTEGER | No | — |
| `createdAt` | INTEGER | No | — |

Indexes: `index_row_reminders_projectId` (`projectId`).

Foreign keys: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION.

#### progress_photos

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `projectId` | INTEGER | No | — |
| `photoUri` | TEXT | No | — |
| `rowNumber` | INTEGER | No | — |
| `note` | TEXT | Yes | — |
| `createdAt` | INTEGER | No | — |

Indexes: `index_progress_photos_projectId` (`projectId`).

Foreign keys: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION.

#### project_counters

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `projectId` | INTEGER | No | — |
| `name` | TEXT | No | — |
| `count` | INTEGER | No | — |
| `stepSize` | INTEGER | No | — |
| `repeatAt` | INTEGER | Yes | — |
| `sortOrder` | INTEGER | No | — |
| `createdAt` | INTEGER | No | — |
| `counterType` | TEXT | No | `'COUNT_UP'` |
| `startingStitches` | INTEGER | Yes | `NULL` |
| `stitchChange` | INTEGER | Yes | `NULL` |
| `shapeEveryN` | INTEGER | Yes | `NULL` |
| `repeatStartRow` | INTEGER | Yes | `NULL` |
| `repeatEndRow` | INTEGER | Yes | `NULL` |
| `totalRepeats` | INTEGER | Yes | `NULL` |
| `currentRepeat` | INTEGER | Yes | `NULL` |
| `linkedToMainCounter` | INTEGER | No | `0` |

Indexes: `index_project_counters_projectId` (`projectId`).

Foreign keys: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION.

#### project_yarn_notes

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `projectId` | INTEGER | No | — |
| `name` | TEXT | No | — |
| `description` | TEXT | No | — |
| `quantity` | INTEGER | No | `1` |
| `notes` | TEXT | No | — |
| `savedYarnCardId` | INTEGER | Yes | `NULL` |
| `createdAt` | INTEGER | No | — |
| `updatedAt` | INTEGER | No | — |

Indexes: `index_project_yarn_notes_projectId` (`projectId`).

Foreign keys: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION.

#### saved_patterns

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `source` | TEXT | No | — |
| `ravelryPatternId` | INTEGER | Yes | — |
| `name` | TEXT | No | — |
| `designerName` | TEXT | No | — |
| `thumbnailUrl` | TEXT | Yes | — |
| `difficulty` | REAL | Yes | — |
| `gaugeStitches` | REAL | Yes | — |
| `gaugeRows` | REAL | Yes | — |
| `needleSize` | TEXT | Yes | — |
| `yarnWeight` | TEXT | Yes | — |
| `yardage` | INTEGER | Yes | — |
| `availability` | TEXT | No | — |
| `originalUrl` | TEXT | No | — |
| `canonicalUrl` | TEXT | No | — |
| `localPdfUri` | TEXT | Yes | — |
| `isAvailableOffline` | INTEGER | No | — |
| `savedAt` | INTEGER | No | — |
| `updatedAt` | INTEGER | No | — |
| `lastSyncedAt` | INTEGER | Yes | — |

Indexes: `index_saved_patterns_ravelryPatternId` (`ravelryPatternId`); `index_saved_patterns_canonicalUrl` (`canonicalUrl`); `index_saved_patterns_originalUrl` (`originalUrl`); `index_saved_patterns_localPdfUri` (`localPdfUri`).

Foreign keys: none declared.

#### pattern_annotation_layers

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `projectId` | INTEGER | Yes | — |
| `savedPatternId` | INTEGER | Yes | — |
| `documentKey` | TEXT | No | — |
| `isActive` | INTEGER | No | — |
| `createdAt` | INTEGER | No | — |
| `updatedAt` | INTEGER | No | — |

Indexes: `index_pattern_annotation_layers_projectId_documentKey` (`projectId`, `documentKey`; unique); `index_pattern_annotation_layers_savedPatternId_documentKey` (`savedPatternId`, `documentKey`; unique).

Foreign keys: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION; `savedPatternId` → `saved_patterns` (`id`), delete CASCADE, update NO ACTION.

#### pattern_annotations

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `layerId` | INTEGER | No | — |
| `page` | INTEGER | No | — |
| `kind` | TEXT | No | — |
| `payloadVersion` | INTEGER | No | — |
| `payloadJson` | TEXT | No | — |
| `zIndex` | INTEGER | No | — |
| `createdAt` | INTEGER | No | — |
| `updatedAt` | INTEGER | No | — |

Indexes: `index_pattern_annotations_layerId_page_zIndex` (`layerId`, `page`, `zIndex`).

Foreign keys: `layerId` → `pattern_annotation_layers` (`id`), delete CASCADE, update NO ACTION.

#### pattern_bookmarks

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `projectId` | INTEGER | No | — |
| `documentKey` | TEXT | No | — |
| `name` | TEXT | No | — |
| `pageIndex` | INTEGER | No | — |
| `yFraction` | REAL | No | — |
| `createdAt` | INTEGER | No | — |

Indexes: `index_pattern_bookmarks_project_document_position` (`projectId`, `documentKey`, `pageIndex`, `yFraction`, `createdAt`, `id`).

Foreign keys: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION.

#### project_documents

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `projectId` | INTEGER | No | — |
| `savedPatternId` | INTEGER | Yes | — |
| `documentKey` | TEXT | No | — |
| `label` | TEXT | No | — |
| `localPdfUri` | TEXT | No | — |
| `sortOrder` | INTEGER | No | — |
| `isPrimary` | INTEGER | No | — |
| `currentPage` | INTEGER | No | — |
| `rowMapping` | TEXT | Yes | — |
| `readingLineEnabled` | INTEGER | No | — |
| `readingLineYFraction` | REAL | No | — |
| `readingLineFollowCurrentRow` | INTEGER | No | — |
| `verticalReadingGuideEnabled` | INTEGER | No | — |
| `verticalReadingGuideXFraction` | REAL | No | — |
| `createdAt` | INTEGER | No | — |
| `updatedAt` | INTEGER | No | — |

Indexes: `index_project_documents_project_order` (`projectId`, `sortOrder`, `id`); `index_project_documents_project_primary` (`projectId`, `isPrimary`); `index_project_documents_project_key` (`projectId`, `documentKey`; unique); `index_project_documents_project_uri` (`projectId`, `localPdfUri`; unique); `index_project_documents_project_saved_pattern` (`projectId`, `savedPatternId`; unique); `index_project_documents_savedPatternId` (`savedPatternId`); `index_project_documents_localPdfUri` (`localPdfUri`).

Foreign keys: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION; `savedPatternId` → `saved_patterns` (`id`), delete SET NULL, update NO ACTION.

#### project_folders

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `name` | TEXT | No | — |
| `normalizedName` | TEXT | No | — |
| `sortOrder` | INTEGER | No | — |

Indexes: `index_project_folders_normalizedName` (`normalizedName`; unique).

Foreign keys: none declared.

#### project_folder_assignments

Primary key: `projectId`.

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `projectId` | INTEGER | No | — |
| `folderId` | INTEGER | No | — |

Indexes: `index_project_folder_assignments_folderId` (`folderId`).

Foreign keys: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION; `folderId` → `project_folders` (`id`), delete CASCADE, update NO ACTION.

#### project_yarn_usage

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `projectId` | INTEGER | No | — |
| `yarnCardId` | INTEGER | Yes | — |
| `projectYarnNoteId` | INTEGER | Yes | — |
| `sourceNameSnapshot` | TEXT | No | — |
| `plannedMeters` | REAL | Yes | — |
| `allocatedMeters` | REAL | Yes | — |
| `usedMeters` | REAL | Yes | — |
| `metersPerSkein` | REAL | Yes | — |
| `gramsPerSkein` | REAL | Yes | — |
| `createdAt` | INTEGER | No | — |
| `updatedAt` | INTEGER | No | — |

Indexes: `index_project_yarn_usage_projectId_yarnCardId` (`projectId`, `yarnCardId`; unique); `index_project_yarn_usage_projectId_projectYarnNoteId` (`projectId`, `projectYarnNoteId`; unique); `index_project_yarn_usage_yarnCardId` (`yarnCardId`); `index_project_yarn_usage_projectYarnNoteId` (`projectYarnNoteId`).

Foreign keys: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION; `yarnCardId` → `yarn_cards` (`id`), delete SET NULL, update NO ACTION; `projectYarnNoteId` → `project_yarn_notes` (`id`), delete SET NULL, update NO ACTION.

#### project_completions

Primary key: `id` (auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `id` | INTEGER | No | — |
| `projectId` | INTEGER | No | — |
| `completedAt` | INTEGER | No | — |
| `zoneId` | TEXT | Yes | — |

Indexes: `index_project_completions_projectId` (`projectId`).

Foreign keys: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION.

#### onboarding_projects

Primary key: `draftId` (not auto-generated).

| Column | SQLite affinity | Nullable | SQL default |
| --- | --- | --- | --- |
| `draftId` | TEXT | No | — |
| `projectId` | INTEGER | No | — |

Unique index: `index_onboarding_projects_projectId` (`projectId`).

Foreign key: `projectId` → `counter_projects` (`id`), delete CASCADE, update NO ACTION. The draft receipt prevents duplicate project creation after retry and is excluded from logical backups.

### Migration chain

Room exports every version 1–28. Versions 1→2 and 2→3 use declared auto-migrations; subsequent migrations are explicitly registered. Schema snapshots, migration implementation, constraint callbacks, and migration tests must change together for new persistent state.

| Transition | Implemented change / preservation boundary |
| --- | --- |
| 1→2 | Adds yarn cards |
| 2→3 | Adds project section/stitch/completion/yarn fields and completed sessions |
| 3→4 | Adds reminders, progress photos, and additional counters |
| 4→5 | Adds Saved Patterns and project linked-pattern field |
| 5→6 | Adds stash quantity/status/project linkage and shaping/repeating counter fields/backfill |
| 6→7 | Adds legacy PDF/reader/stitch/repeat-section and annotation state |
| 7→8 | Adds project target rows |
| 8→9 | Adds session-start index |
| 9→10 | Adds exact session seconds/rows with guarded legacy backfill |
| 10→11 | Adds session time/project query indexes |
| 11→12 | Adds project yarn notes |
| 12→13 | Adds craft/main-label/reading-line and linked extra-counter fields |
| 13→14 | Rebuilds saved-pattern source, URL, and local-PDF metadata while preserving IDs |
| 14→15 | Adds soft-link lookup indexes |
| 15→16 | Adds nullable session zone ID |
| 16→17 | Adds annotation layers/rows and migrates legacy payloads; stable keys such as `saved:<id>:v1` or `legacy-project:<id>` |
| 17→18 | Adds monotonic secondary-counter/notes-used flags; old projects marked used, new defaults false |
| 18→19 | Replaces boolean pattern availability with `free`/`paid`/`unknown`; legacy true→free, false→unknown; rebuild preserves layer/annotation ownership |
| 19→20 | Adds bookmarks, reading-line follow flag, and vertical-guide state while preserving PDF/mapping/layers/triggers |
| 20→21 | Adds singleton `active_sessions`; prior data unchanged |
| 21→22 | Adds `project_documents`; backfills primary relations from nonblank legacy PDFs with reader state, retains project compatibility columns |
| 22→23 | Adds only empty folders/assignments; no earlier table changes |
| 23→24 | Adds only empty project yarn usage plus four indexes; no amount backfill |
| 24→25 | Adds project completion events; backfills at most one valid current completion per completed project, nullable legacy zone |
| 25→26 | Adds only onboarding project draft receipts and their unique project index; preserves earlier tables |
| 26→27 | `MIGRATION_26_27` adds nullable `counter_history.linkedCounterDeltasJson` TEXT for actual linked-counter history deltas; preserves existing history with null snapshots |
| 27→28 | `MIGRATION_27_28` adds nullable `counter_history.sessionUndoJson` TEXT for session row-accounting undo snapshots; preserves existing history with null snapshots |

Migration 24→25 requires a credible completion timestamp at/after creation; it does not invent all historical completion cycles. Session null zones and completion null zones keep the established current-device-zone fallback. Migration 18→19 must preserve tables that reference the rebuilt saved-pattern table; a successful table rebuild without those rows is not a correct migration.

Useful deterministic ordering includes document `sortOrder,id`, extra-counter `sortOrder,id`, bookmark `pageIndex,yFraction,createdAt,id` and descending session `startedAt,id`. Counter Undo chooses newest inserted history ID; display ordering must not be mistaken for the authoritative Undo choice when wall time moves. Do not substitute composition order or mutable names for persistent tie-breakers.

### Preferences and other persisted state

Onboarding Phase 1 adds `last_project_craft_type` (default KNITTING, unknown values fall back to KNITTING) to ordinary preferences. The separate `OnboardingStateRepository` owns `onboarding_state`: initialization version, welcome dismissal, starter project, setup visibility/celebration, seen tip IDs, first-use day, counter entry/day counts, first-session pending state, a bounded chronological set of 200 celebrated project IDs, and successful-backup status. It uses safe read defaults and corruption replacement and is excluded from backup. Application-scope IO initialization checks unfiltered active-or-completed project existence and seeds basic tips for existing users. Successful replacement restore seeds basic tips; export success records backup creation. These guidance writes do not change the result of an already successful backup operation.

Phase 4 adds process-owned `TipSession` and pure `TipRules`, with `TipHost` rendering a shared `StitchMarkerTip` against registered bounds. The host waits for a resumed, loaded route and two seconds, respects modal/input/import/recovery/error blockers, and protects counter controls from overlap. An outside tap dismisses the tip and may activate the underlying control with the same pointer event. One tip can be shown at a time; two automatic tips may be shown per process, plus chained UNDO. Seen state persists when a shown tip ends. `projectIncrementCounts` stores actual committed increments up to 30 per project, independent of the starting count and undoable history. Settings resets both persistent tip state and the process budget only on successful persistence. The backup suggestion is an inline Projects list card and uses the unfiltered project membership snapshot.

Phase 8 adds committed celebration triggers. `applyMainCounterChangeWithResult` returns the transaction's before/after counts and target; its Boolean wrapper preserves existing callers. A crossing excludes Undo and emits `TipSignal.TargetReached`. TARGET_REACHED uses the project-specific celebrated set instead of global tip seen state, and TipHost records it only after visible placement has rendered; reset preserves that per-project memory. `StopSessionResult.Saved.wasFirstCompletedSession` is determined from the prior completed-session count within the save transaction. A null saved ID never signals a session. `TipSignal.SessionSaved` atomically records `first_save_observed` and queues the first-session tip only for a genuinely first row; the monotonic marker survives reset and deletion of sessions. No Room schema change is required.

The project PDF reader exposes a reading-line toolbar button in reading mode. It anchors READING_LINE and opens the existing settings menu, retaining its callbacks and blocking behavior. Opening the menu does not mutate reading-line state; Library and markup mode do not gain this button.

Phase 9 adds only `trial_chart_seen_for_start` and `trial_photo_pdf_created_for_start` to `onboarding_state`, with zero as the unset value. Their writers ignore missing starts and retain the newest recorded trial start, so a late older write cannot replace newer progress. Resetting tips preserves both markers. Trial notice claims remain in `trial_state`, while the pending start sheet belongs to UI saved state. No Room schema or backup-content change is introduced.

[PreferencesManager.kt](app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt) owns `knittools_preferences` Preferences DataStore. Read IO failures return safe defaults; corruption replaces malformed preferences with empty preferences. Safe writes expose/handle failure rather than guaranteeing disk success.

| Preference | Default / ownership |
| --- | --- |
| `theme_mode` | Light (1); System=0, Dark=2 |
| `app_language` | System; persistence/migration mirror of platform locale APIs |
| `haptic_feedback` | true |
| `keep_screen_awake` | false |
| `use_imperial` | false |
| `show_completed_projects` | false |
| Project sort | Updated; typed `ProjectSortOrder` persisted value |
| Usage analytics consent | false |
| Language migration flag | Tracks migration to Android per-app locale ownership |

API 33+ uses `LocaleManager`; older versions use `AppCompatDelegate`. Startup asynchronously applies stored language. A language change writes its mirror before applying platform state; changing raw DataStore directly is not the whole locale implementation.

Other persisted stores have deliberately separate boundaries:

- `trial_state` DataStore: trial timing/tamper/end-notice anchors; excluded from manual backup. Details below.
- `review_state` DataStore: `review_requested` and `action_count`; safe-default corruption handling, excluded from manual backup.
- Widget Preferences DataStore/Glance state: chosen app-owned project/count presentation; refresh after restore rather than importing old widget configurations.
- Private `counter_launch_tokens` SharedPreferences: one-use UUID launch records, capped at 100 with 24-hour validity; synchronous commit must succeed for consumption.
- `SavedStateHandle` / saved-instance state: restorable drafts, pending PDFs/shares, filters/selection; not the manual backup format.
- WebView cookies/DOM storage: website session state; not an app Ravelry account or exported backup content.
- Crash/analytics/Play SDK storage: integration-managed state; not project content backup.
- Restore staging and `restore-files.json`: private recovery bookkeeping, not user-visible Library entries.

### Column semantics and weak relationships

The full dictionary above is the schema contract, while these interpretations prevent common misuse:

| Data group | Meaning and constraints outside raw affinity |
| --- | --- |
| Project counters and timestamps | Main/secondary/stitch integers are working values; `updatedAt` is not a folder/usage revision or a session clock |
| `counter_projects.stepSize` | Project-specific main-button/widget amount, default 1; new editor/setter accepts 1–99; saving also updates `updatedAt`, with no count/history side effects; distinct from each `project_counters.stepSize` |
| Project `totalRows` versus `targetRows` | Retained completion/progress information and explicit count target are different fields; do not combine them because both mention rows |
| Legacy project reader/PDF fields | Compatibility/migration/read-cleanup inputs; new reader state belongs to a document relation |
| Yarn per-skein fields | Several descriptive quantities are TEXT to preserve imported/manual content; project usage uses validated canonical REAL meters |
| Yarn-card quantity | Integer stash units, independent from meter amounts and allocation |
| `sessions.durationMinutes` | Compatibility/display minutes; exact timing uses `durationSeconds` |
| `sessions.rowsWorked` | Recorded net counter progress, not recomputed from current project count; no historical counter-unit snapshot |
| Session/completion `zoneId` | New records preserve event zone; legacy null uses current device-zone fallback |
| Active anchors | Wall time, monotonic elapsed time and boot identity have different roles; restored foreign anchors require review |
| `linkedPatternId`, `linkedProjectId`, yarn CSV, `savedYarnCardId` | Soft links maintained by repositories; no corresponding SQL foreign key in several cases |
| `documentKey` | Stable annotation/bookmark identity, not file label, current page or relation ID |
| Annotation payload | Kind + version + bounded JSON, normalized coordinates, stable chart-counter references |
| Usage `sourceNameSnapshot` | Creation-time fallback after source loss; live source name takes precedence |
| Completion rows | Events, including repeated lifecycle cycles; not just the current number of completed project rows |

The installed trigger names are `active_sessions_require_singleton_id_insert/update`, `pattern_annotation_layers_owner_insert/update`, `pattern_annotation_layers_active_insert/update`, and `project_documents_primary_insert/update`. The document triggers prevent more than one primary; repository ordering/selection/removal logic maintains at least one for a nonempty set. The schema JSON alone does not fully describe these trigger invariants.

Deleting a project cascades its history, sessions/active session, reminders, photos, extra counters, notes, annotation layers/annotations, bookmarks, documents, folder assignment, usage and completion events. Deleting a yarn source sets nullable usage foreign keys to null; it does not erase recorded amounts. Deleting a saved pattern removes its master layers and clears document saved-pattern foreign keys, while reference-aware cleanup preserves still-used PDF bytes.

## Files, PDF processing, and cleanup

### Ownership and provider surface

[AppFileStorage.kt](app/src/main/java/com/finnvek/knittools/data/storage/AppFileStorage.kt) resolves app-owned file/URI compatibility, validates canonical paths, and centralizes FileProvider authority and share URI construction. A missing owned URI must not be reinterpreted as an arbitrary external provider read.

| Location | Content / lifetime |
| --- | --- |
| `files/pattern_pdfs/<projectId>` | Imported/generated private PDFs; `0` is used for incoming/Library/restore staging destinations |
| `files/pattern_captures/<projectId>/<sessionId>` | Private photo-to-PDF source session; copied images and camera captures |
| `files/progress_photos/<projectId>` | Durable project progress images |
| `files/yarn_photos/<cardId>` | Durable private yarn-card images |
| `cache/pattern_exports` | Temporary annotated export PDFs |
| `cache/web_pdf_downloads` | Temporary HTTPS website download files before private import |
| `noBackupFilesDir/manual-backup/<UUID>` | Validated export/preview/restore staging and recovery files |

Only pattern-capture and progress-photo paths are FileProvider-exposed. Yarn photos and pattern PDFs are not broad share roots. Historical `patterns/...` and yarn FileProvider URIs remain internally resolvable for read/cleanup compatibility without restoring those roots to exported provider paths. [file_paths.xml](app/src/main/res/xml/file_paths.xml) and manifest authority must remain aligned with the helper.

Pattern import uses SAF `OpenDocument(application/pdf)` and persistable URI grants where available, then creates a unique private copy, validates at least one PDF page, reserves disk space, and cleans failed/cancelled copies. The 100 MiB website download ceiling does not mean every other import path has that same ceiling; use the owning storage budget. Copied labels/filenames are cleaned by `PatternDisplayNames` and bounded filename logic (180-character PDF filename limit).

### Deletion and reference safety

Project deletion, document removal, saved-pattern deletion, and pattern detach collect app-owned cleanup references before the authoritative database transaction. A shared PDF is deleted only when Library, project documents, and still-relevant legacy project URIs no longer reference it. Deleting an annotation layer is a database ownership action, not permission to remove a shared source file.

Physical cleanup is best effort after commit; failures leave orphans for the owning cleanup/recovery path rather than misreport rollback. Failed attachment/import cleans the unattached copy when possible. Progress-photo pruning, yarn-photo pruning, capture-session pruning, shared-PDF deletion, and backup-journal recovery have separate rules; there is no universal filesystem sweep that is safe for every directory.

## Security, privacy, and telemetry

### Android manifest and external surface

[AndroidManifest.xml](app/src/main/AndroidManifest.xml) requests INTERNET and VIBRATE; camera hardware is optional. Both photo capture paths delegate to `ActivityResultContracts.TakePicture()` without requesting CAMERA, after their existing product/Pro admission checks. No broad storage permission is required for SAF/Photo Picker. There is no `RECORD_AUDIO` permission, `SpeechRecognizer`, `TextToSpeech`, conversational voice command, or production microphone flow.

`allowBackup=false`, cloud/device-transfer rules exclude the root, `usesCleartextTraffic=false`, RTL support and locale config are declared. The exported Main Activity is singleTop with adjustResize and no orientation lock. The widget receiver is exported with `BIND_APPWIDGET`; counter action receivers are nonexported. FileProvider is nonexported with temporary URI grants and only the two narrow image roots. Manifest declarations/configuration do not establish a tested release manifest or device/provider behavior.

General web patterns use `WebPatternUrl`: trim/length ≤2,048, HTTP(S), valid authority/port, no userinfo/control/unsafe whitespace/bad percent encoding, canonicalized IDN/lowercase host/default port, and rejection of localhost/.local/noncanonical or private/reserved literal IP forms. It does not resolve DNS. Revalidation precedes external browsable `ACTION_VIEW`; missing browser/security/runtime failures become typed results. HTTP warning does not authorize cleartext in the Ravelry WebView/downloader.

Incoming MIME/content and text parsing have the independent bounds described above. URLs/labels from external pages/intents remain untrusted input. File ownership is checked before cleanup; export/camera FileProvider grants are not a broad private-data publication surface.

### Local content, network, and secret boundaries

Project records, notes, PDF annotations, photos, yarn/usage, calculators and sessions are locally owned. The app does not provide an app account/content cloud database. Ravelry website pages and HTTPS document/thumbnail hosts are network integrations; current Android does not call retained Functions. Play Billing/Review/Update, Firebase Crashlytics and consented PostHog are distinct external services.

Signing material, Google-services configuration, PostHog project configuration and debug Sentry DSN are private configuration mechanisms. Describe variable/file names only; never print real values. Backend Ravelry credentials are Secret Manager values. Tracked `app/google-services.json` is forbidden by the release-surface policy. A PostHog public project token is ingestion configuration, not permission to send user content.

Manual backups contain sensitive unencrypted user content. Explicit export grants access to the selected provider; Android automatic backup is disabled. Clipboard Copy places user-selected calculator output in the system clipboard. These are real data boundaries, not a blanket claim that no data can leave the device.

### Release Crashlytics

Firebase Android dependencies currently use Crashlytics; Auth/Functions were removed from Android. Release Crashlytics collection/mapping upload is configured; debug/benchmark variants disable collection/upload. Crash reporting is separately approved from usage analytics and is not controlled by the PostHog consent toggle. R8 mapping configuration is build tooling, not proof that a current release mapping was uploaded or symbolication verified.

Crash telemetry may include SDK/platform diagnostics. The predefined analytics content restriction does not automatically prove every stack trace/vendor SDK payload is empty of context; live payload and privacy/release disclosures require separate verification. No cloud content analysis, Firebase AI/ML Kit/Gemini, microphone or voice feature is implemented.

### Optional release usage analytics

[config/posthog.md](config/posthog.md) describes the consent boundary; the current implementation and catalog define SDK behavior/version. `PostHogAnalytics` initializes lazily in release when `usageAnalyticsEnabled` is true (default false), uses the EU endpoint, and handles lifecycle/navigation. `UsageAnalytics` accepts predefined event/allowlisted screen names only, without project content/IDs, URLs, notes, counts/search text or billing state.

The domain events are: `project creation started`, `project creation submitted`, `project creation cancelled`, `project created`, `project creation failed`, `pdf import started`, `pdf import succeeded`, `pdf import failed`, `counter incremented`, `counter decremented`, `work session start requested`, `work session saved`, `welcome demo used`, `welcome tools chosen`, `welcome dismissed`, `guide opened`, `tip acted`, and `tip dismissed`. The onboarding events are wired to their UI consumers in the phases that introduce them. Lifecycle/navigation also uses `app opened`, `app backgrounded`, `$screen`, and `screen exited`.

Screen normalization strips query and slash suffix and maps guide topic routes to `guide_topic`, then allowlists 32 names: tools, counter, increase_decrease, gauge, cast_on, yarn, needles, size_charts, abbreviations, chart_symbols, settings, backup, pro_upgrade, yarn_card_detail, ravelry_browser, project_list, project_overview, photo_gallery, pattern_viewer, notes_editor, session_history, counter_history, library, saved_patterns, saved_pattern_detail, web_pattern_editor, library_pattern_viewer, my_yarn, all_photos, insights, guide, guide_topic. Unknown routes are dropped; topic keys and project IDs are never sent.

Automatic capture, user profiles, replay, feature flags, push capture, tracing/logging/native crash capture, and GeoIP enrichment are disabled. Pseudonymous installation/session/device context still belongs to the SDK boundary. Counter increment/decrement analytics describe requested actions, not database-success receipts.

**Revocation is stronger than capture-only opt-out.** `PostHogAnalytics` disables capture and synchronously closes the current client's HTTP admission gate before SDK retirement/queue cleanup. `PostHogConsentClient` retires that gate irreversibly before `optOut`/`close`; later flush/retry/close cannot reopen it. Denied calls get a local discard response without transport, which must not be counted as delivered events. Requests admitted before retirement are already in flight and are cancelled best effort; they may have reached the server. Revocation is not remote deletion of previously delivered data.

Each SDK instance gets a never-reused `cache/knittools-usage-analytics/consent-<UUID>` directory for its event/replay/log/legacy prefixes. Old recognized directories are cleaned narrowly and are never imported or reused, including after process death, deletion failure or rapid off/on. Consent-off rotates a local DataStore generation marker so an observation that skips the intermediate off state still retires the prior client. SDK consent persistence is disabled; DataStore remains authoritative. Queue/SDK initialization and retirement are serialized separately from short event/state locks, so revoking an unfinished initialization does not need to wait for it to finish. A superseded initializer cannot become the active client.

This deliberately sacrifices durable cross-process delivery, even while consent remains on. Cleanup can leave ineligible bytes after interruption; storage deletion success is not what controls delivery eligibility. Automatic HTTP retries/redirects are disabled. The implementation uses version-specific PostHog internal configuration seams for HTTP/storage prefixes; changes to the declared SDK require real-SDK consent/queue regression validation. No live analytics delivery or server retention was checked in this documentation task.

Sources: [PostHogAnalytics.kt](app/src/main/java/com/finnvek/knittools/analytics/PostHogAnalytics.kt), [PostHogConsentClient.kt](app/src/main/java/com/finnvek/knittools/analytics/PostHogConsentClient.kt), [PreferencesManager.kt](app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt). `config/posthog.md` retains historical SDK version wording; the current catalog is the version authority.

### Debug-only Sentry

Sentry Android core is `debugImplementation` only with initialization under [app/src/debug](app/src/debug); release source is no-op. DSN comes from ignored debug configuration/environment; no DSN means no initialization. Automatic startup/provider collection is disabled; traces/profiles, activity/frame collection, NDK, broad breadcrumbs, screenshots/view hierarchy and related automatic features are disabled. There is no Sentry Gradle upload plugin. Debug diagnostics are not a release analytics opt-in substitute.

### Accepted limits and security evidence

Browser redirect/subframe and cookie rules, local trial timing, client-side Play entitlement, provider cancellation limits, post-commit orphans, rasterized export and unencrypted archives are documented implementation boundaries. They are not proven vulnerabilities merely because stronger alternatives exist. The written Ravelry permission requirement is unresolved in the recorded decision. Release manifests/artifacts, live purchases, provider semantics, cloud deployments and vendor payloads remain unverified by this documentation inspection.

Scanner exceptions are controlled by [config/check-exceptions.json](config/check-exceptions.json), not blanket rule hiding. MobSF requires a single rule and exact `findingPath`; `.mobsf` must not globally suppress a registered finding rule. Current inventories and historical scan receipts appear in the QA sections.

### App manifest versus dependency-merged manifest

The source manifest's explicit permission contract is INTERNET and VIBRATE. SDK manifests can add normal permissions/components; the complete permission set must be checked in the merged manifest.

Existing local merged artifacts were inspected read-only: debug artifact timestamp 2026-10-15 21:04 EEST, release artifact timestamp 2026-10-14 02:06 EEST. They are **historical build outputs**, not freshly merged manifests for the inspected HEAD. Both additionally show ACCESS_NETWORK_STATE, Play BILLING, WAKE_LOCK, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE and the application signature-protected dynamic-receiver permission.

Their dependency surface includes permission-protected Glance RemoteViews, WorkManager job/diagnostic components, Profile Installer's DUMP-protected receiver, Firebase initialization/discovery/session/transport components and private Billing/Play proxy activities. Debug additionally includes Compose preview/test support and Sentry providers, with Sentry automatic initialization disabled by the debug manifest. Such dependency services do not implement project synchronization or background session ticking.

For a release change, inspect the actual newly merged release manifest and the release-surface exported-component rules; do not transfer debug exported preview/test activities into the production surface inventory. No current merged/signed APK certification is claimed here.

### Network and data boundary map

| Integration | Initiator / data category | Consent/configuration boundary |
| --- | --- | --- |
| Ravelry website | User opens browser; HTTPS pages, normal website cookies and user agent | Website sign-in; no native OAuth client |
| Website PDF hosts | User selects download; HTTPS requests/cookies appropriate to each URL | 100 MiB download ceiling, redirects validated by scheme, private PDF verification |
| Remote thumbnails | Valid retained HTTPS thumbnail URL through Coil/Ktor | Source metadata; no project-photo upload |
| Generic pattern/help website | Explicit external browsable action | External browser/provider owns its page/session behavior |
| Google Play | Product query, purchase/restore/ack, review/update | Play services/store configuration; no app payment-card form |
| Firebase Crashlytics | Release SDK crash/ANR reporting and build mapping upload | Configured automatically for release; separate from usage consent |
| PostHog EU | Allowed usage/lifecycle/screen events and durations | Release only, lazy client after opt-in, defaults off |
| Sentry | Developer-configured debug diagnostics | Debug DSN only; no release dependency |
| SAF/Photo Picker/camera | User-selected content or destination | Provider permissions/grants; provider may independently access network |
| Retained Firebase backend | No Android request path | Independent backend deployment status not established |

The configured endpoint is not an exhaustive packet-capture inventory of each vendor SDK. Crashlytics, Play, websites, document providers and pseudonymous analytics have their own transport/storage behavior. Local-content privacy should be described per boundary, not as an absolute “no network/no data leaves the device” claim.

### Secret configuration and release obligations

| Configuration | Location/mechanism | Never document or commit |
| --- | --- | --- |
| Release signing | Four `KNITTOOLS_KEYSTORE_*/KEY_*` variables listed in build section | Keystore bytes, passwords or signing material |
| Firebase Android config | Ignored `app/google-services.json` or Base64 CI variable | Private local config contents; never promote placeholder to release |
| PostHog ingestion token | Ignored `posthog.properties` / configured environment | Actual value or personal/project-secret API keys |
| Debug Sentry | Ignored `debug.credentials.properties` / DSN environment | Actual DSN/config values |
| Historical backend OAuth | Firebase Secret Manager names and server-only exchange | Client secrets, access/refresh tokens, browser proofs or state values |

Source and scanner exception files may describe a public client-key format. This does not authorize copying local values into documentation. This rewrite names configuration mechanisms only.

[PRIVACY-POLICY.md](PRIVACY-POLICY.md) and [ONLINE_OFFLINE.md](ONLINE_OFFLINE.md) now describe the website browser, release Crashlytics, optional analytics and manual backup. They must not be described as still asserting the removed API/no-telemetry architecture. The privacy policy remains a draft because publisher contact information is absent. Written Ravelry permission and final store/service disclosures are release obligations recorded in the repository, with completion not verified here.

## Localization

### Languages and selection

[locales_config.xml](app/src/main/res/xml/locales_config.xml), [AppLanguage.kt](app/src/main/java/com/finnvek/knittools/data/datastore/AppLanguage.kt), Android resource directories, and `PreferencesManager` are the authorities.

| Language | Tag / resource directory |
| --- | --- |
| English | `en`, base `values` |
| Danish | `da`, `values-da` |
| German | `de`, `values-de` |
| Spanish | `es`, `values-es` |
| Finnish | `fi`, `values-fi` |
| French | `fr`, `values-fr` |
| Italian | `it`, `values-it` |
| Norwegian Bokmål | `nb`, `values-nb` |
| Dutch | `nl`, `values-nl` |
| Portuguese | `pt`, `values-pt` |
| Brazilian Portuguese | `pt-BR`, `values-pt-rBR` |
| Swedish | `sv`, `values-sv` |

System is a selection mode in addition to the 12 translated locale options. Settings displays Brazilian Portuguese as `Português (Brasil)`. `AppLanguage.BRAZILIAN_PORTUGUESE` stores and applies `pt-BR`; existing Portuguese retains `pt`, its resources and saved preferences. `fromLanguageTag` matches the first tag case-insensitively before base-language fallback: `pt-BR`/`PT-br` select Brazilian Portuguese, while `pt-PT` and `pt-AO` fall back to `pt`. No separate Latin-American Spanish locale is configured. Android fallback handles unsupported device languages. Runtime language uses per-app platform/AppCompat APIs; DataStore is only the mirror/migration state. `supportsRtl` does not mean an Arabic/Hebrew translation or RTL device proof exists.

The November 10 source inventory contains **1,631 translatable resource names per locale: 1,597 strings and 34 plurals**, across all relevant XML files. The seven two-form locales (en/fi/sv/nb/da/de/nl) each contain 1,665 text variants; the five three-form locales (fr/it/es/pt/pt-BR) each contain 1,699. Overall this is **20,150 text variants and 19,572 locale/key pairs**. Nontranslatable resources and non-text theme/color XML are excluded. Files are not arranged identically in every locale: enumerate actual resource children rather than assuming one file per feature or comparing `strings.xml` alone.

`strings_onboarding.xml` currently has **325 named string/plural resources per locale**, included in the totals above. Name/type parity and placeholder/plural source contracts are distinct from editorial quality, Android ICU selection and rendered fit. An old audit count or hash is not current evidence after resources change.

[LANGUAGE_AUDIT.md](LANGUAGE_AUDIT.md), [shared corrections](config/language-audit/shared-fixes.md) and the locale reports under [config/language-audit](config/language-audit) preserve dated content decisions and technical receipts. Their historical key inventories are not the current inventory above. This documentation task does not rerun those suites, conduct a native-speaker review, or certify every localized screen/OS combination.

### Formatting, units, and terminology

UI strings/plurals/content descriptions use Android resources; numbers/dates use locale-sensitive helpers. Session/history display respects local dates and 12/24-hour preferences; Insights uses stored session zones and locale first weekday. Null legacy zones fall back to the device. Folder uniqueness uses Unicode NFC/Root lowercase, not language-sensitive display sorting. Project names preserve original case.

Canonical calculation data uses mm, density/counts-per-mm and meters; user display can use metric/imperial units. Inches derive from canonical measurements; Yarn Estimator length can switch meters/yards while weight remains grams and its result preserves lowercase `g`. Gauge raw input is preserved independently from formatted output. Size Charts use localized numeric formatting (integral zero decimals, otherwise one), including both endpoints of ranges.

Finnish product terminology calls a knitting pattern an “ohje” and a web pattern a “verkko-ohje”; localized My Yarn uses `my_yarn_title`. Filename-like imported labels are cleaned without inventing designer metadata. English knitting abbreviations and the regex instruction grammar remain deliberately limited; translations do not imply parser support for all 12 locale options or distinct crochet abbreviation data.

Accessibility/source tests and historical instrumented locale evidence are listed below. No current 320 dp/200%/TalkBack/RTL or full multilingual visual run occurred in this documentation task.

### Resource and input conventions

Onboarding uses `values/strings_onboarding.xml` and matching da, de, es, fi, fr, it, nb, nl, pt, pt-rBR and sv files. Each key has a Finnish translator-context comment; MissingTranslation suppression is prohibited. English screen titles retain Title Case; other languages use natural local casing and craft idioms. Finnish uses ohje and verkko-ohje, and My Yarn uses each language's existing product name. `OnboardingResourcesSourceTest` checks configured locales, key sets/types, placeholders, plural quantities, translator comments, live consumers and wording rules. `ProLocalizationSourceTest` preserves Pro/Billing localization coverage and the current active-project policy copy. The release-readiness plan and historical onboarding report remain separate from a current test verdict.

English defaults are in `values/`; each supported language mirrors the translatable resource names. Preview/constants marked `translatable=false` are excluded from parity counts. Brazilian Portuguese has its own complete `values-pt-rBR` resources alongside `values-pt`. No separate regional translation directory is configured for es-419 or en-GB; locale formatting may still use the selected runtime region.

`MeasurementNumberParser` is the shared boundary for supported decimal/integer input. Preserve raw text while editing and separate parse validity from formatted display. `LocaleNumberFormatter`, `MeasurementNumberFormatter`, localized date helpers, duration and minutes-per-row formatters prevent per-screen decimal/time conventions from diverging. Needle reference search additionally normalizes comma decimal input and matches metric prefixes or exact regional labels.

Knitting instructions generated by Increase/Decrease retain standard `K`, `M1` and `K2tog` notation. Numeric field parsing accepts ASCII digits, the locale decimal separator or a dot, with negative values only where requested. Grouping separators and exponent notation are not accepted. Empty text, a sign alone when permitted, or a trailing decimal separator can remain incomplete edit state. Nonfinite/underflow/out-of-range values are rejected; integer fields stay within Int bounds.

The regex parser uses selected English words and dot-decimal number forms; localized resource coverage does not turn it into a multilingual natural-language parser. Knitting/crochet abbreviation routes currently select the same source dataset, so a distinct comprehensive crochet dictionary must not be promised.

Plural resources and whole localized phrases are used where counts/grammar matter. Insights keeps range wording out of the middle of a hero sentence and uses standalone number/noun phrases. Dates with stored event zones and app-language formatting are separate from the physical device's current zone and time-format preferences. Resource name equality does not validate every placeholder, grammatical case, translation meaning or runtime ICU result.

Shared counter-related copy distinguishes a count from a known row/round unit. Main-counter labels still follow the selected project label; session progress/history, photo captions, reminders/repeat intervals, reader following and calibration use neutral counter wording. This does not rename Kotlin/database fields or geometric chart rows. Instructional references clarify slip orientation, yarn-in-front/behind relative to the worker, source body-size categories and the right-side chart convention rather than inferring those meanings from an English abbreviation alone.

`relativeDayText`, shared by `InsightsProjectRow` and `InsightsLastWorkedNote`, uses `pluralStringResource(R.plurals.relative_time_months_ago, months, months)` for `months = days / 30`. The 30/59/60/90-day cases therefore select and display 1/1/2/3 months with the same value. Today/yesterday/day/week thresholds and approximate month arithmetic are unchanged. Spanish has distinct singular/plural wording; locale-specific one/other or one/many/other forms are retained, including the regional Brazilian set.

## Toolchain, dependencies, and build variants

### Repository/module structure and Android build

| Area | Purpose |
| --- | --- |
| `app` | Android application, variant code/resources, tests and Room schema exports |
| `baselineprofile` | Android test module targeting the app for profile generation/benchmark infrastructure |
| `functions` | Retained Ravelry backend TypeScript package and independent npm lockfile |
| `gradle`, root/app Gradle files | Wrapper/toolchain/catalog/dependency verification and build configuration |
| `config` | Checker module/task declaration, analyzer exceptions/rules, operational design boundaries |
| `tools`, `scripts` | PowerShell delegates/local validation tests, security/Sonar/release helpers |
| `.deepsec` | Separate pinned DeepSec review workspace with Android/Kotlin custom matchers |
| `.github` | Build/test CI and Dependabot; no checked-in CodeQL workflow |
| `docs`, root plans | Product/design/implementation history; must be checked against current source |
| `reports`, build directories | Mostly ignored local evidence/generated outputs, not current production source |

[settings.gradle.kts](settings.gradle.kts) includes only `:app` and `:baselineprofile`; Foojay resolver convention is 1.0.0. Repositories are Google, Maven Central and Gradle plugin portal as applicable, with project repository additions forbidden. AGP built-in Kotlin is used; do not restore a separate `org.jetbrains.kotlin.android` plugin by habit.

[gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties) pins **Gradle 9.8.1** and its distribution SHA-256. [gradle-daemon-jvm.properties](gradle/gradle-daemon-jvm.properties) selects ADOPTIUM/Eclipse Temurin **JDK 17**; Java source/target and CI are 17. [gradle.properties](gradle.properties) enables build/configuration cache, disables parallel project execution, limits workers to two, gives Gradle 2 GiB heap, and uses in-process Kotlin compilation. AndroidX, nontransitive R and R8 full mode are configured.

### Dependency catalog

[gradle/libs.versions.toml](gradle/libs.versions.toml) is the version authority; [app/build.gradle.kts](app/build.gradle.kts) and [build.gradle.kts](build.gradle.kts) establish actual use and overrides. BOM-managed libraries do not have separately declared catalog versions.

| Tool/library | Current declared version | Role |
| --- | --- | --- |
| Android Gradle Plugin | 9.5.1 | App/test plugin, built-in Kotlin toolchain |
| Kotlin Compose/serialization plugin | 2.4.21 | Compose compiler and serialization |
| KSP | 2.3.12 | Room/Hilt code generation |
| Compose BOM | 2026.09.00 | UI, Foundation, Animation, Material 3, tooling/tests |
| Navigation Compose | 2.10.2 | Current route graphs |
| Lifecycle | 2.11.0 | ViewModel/lifecycle-aware collection/process lifecycle |
| Hilt / AndroidX Hilt | 2.60.1 / 1.4.0 | Injection / Compose ViewModel integration |
| Room | 2.8.5 | Persistence/compiler/testing |
| Coroutines | 1.11.0 | Core/Android/Play integration and tests |
| DataStore | 1.2.1 | Preferences/trial/widget state |
| Core KTX / Activity Compose / AppCompat | 1.19.1 / 1.13.0 / 1.8.0 | Android integration/per-app language |
| WebKit / WindowManager | 1.18.0 / 1.5.1 | WebView state support and adaptive/fold-aware window information |
| Kotlinx serialization | 1.12.0 | Domain payload/archive JSON and Room support |
| Guava | 33.7.2-android | Required compatibility dependency |
| Splashscreen | 1.2.0 | Android launch surface |
| Play Review / Update | 2.0.2 / 2.1.0 | Play integrations |
| Billing | 9.1.0 | One-time Pro |
| Glance | 1.1.1 | Widget rendering |
| Coil | 3.6.3 | Compose image loading; explicit `coil-network-ktor3` for remote thumbnails |
| Ktor | 3.6.1 | Core/OkHttp image-network support; mock client in tests |
| WorkManager | 2.12.0 | Constrained Glance transitive runtime; does not implement continuous sync |
| Firebase BOM | 34.19.0 | Current Android Crashlytics dependency |
| Google Services / Crashlytics plugin | 4.5.0 / 3.0.8 | Variant configuration/mapping integration |
| PostHog | 3.75.0 | Release opt-in usage analytics with consent delivery/queue boundary |
| Sentry Android core | 8.62.0 | Debug-only diagnostics |
| Baseline Profile / Benchmark | 1.5.0 | Stable producer/benchmark tooling |
| Profile Installer | 1.4.1 | Profile installation support |
| Wire runtime | 7.1.0 | Explicit security-compatible resolution override |
| ktlint Gradle | 14.2.0 | Format/lint plugin |
| Detekt | 2.0.0-alpha.5 | Temporary AGP 9 compatibility exception |
| Compose rules | 0.6.3 | Detekt/ktlint Compose checks; pinned compatible rules build for Detekt 2.0.0-alpha.5 |
| Android security lint | 1.0.4 | Additional lint checks |
| Compose stability analyzer | 0.15.0 | Debug stability baseline/report |
| OWASP Dependency-Check | 13.0.0 | Dependency advisory scan |
| Sonar Gradle plugin | 7.5.0.8588 | Analysis/coverage integration |
| Dependency Analysis | 3.20.0 | Catalogued but root plugin disabled pending AGP 9 compatibility |
| JUnit / MockK / Turbine | 4.13.2 / 1.14.11 / 1.2.1 | JVM tests |
| org.json | 20260814 | JVM Android-JSON-compatible tests |
| AndroidX Test JUnit / Runner / Espresso | 1.3.0 / 1.7.0 / 3.7.0 | Device/Compose test support |

Root buildscript forces Jackson core/databind **2.22.4** and jsoup **1.23.2**, plus selected Guava/jose4j/JDOM/Bouncy Castle versions for tooling. Subprojects constrain Guava flavor/version and Wire 7.1.0. A resolution override is distinct from a direct app dependency or a vulnerability verdict. Detekt alpha is an explicit temporary exception; retain it until a stable AGP-compatible release is verified. Baseline tooling uses stable 1.5.0. The inactive dependency-analysis catalog entry is not a configured passing check.

[gradle/verification-metadata.xml](gradle/verification-metadata.xml) records artifact verification checksums with metadata verification enabled and signature verification disabled. Update it only with verified resolved dependencies/artifacts; XML presence is not proof of a newly resolved graph. Backend uses `functions/package-lock.json`; DeepSec has its own pnpm lock/workspace plus root forwarding scripts/lock. No dependency was changed or resolved during this documentation task.

### Variants, isolated tests, and release admission

Normal debug includes tooling, Sentry and the debug demo facade/shared test support. Release source implements no-op debug diagnostics/demo behavior, is nondebuggable, minifies and shrinks resources with [proguard-rules.pro](app/proguard-rules.pro). Baseline plugin supplies `benchmarkRelease` and `nonMinifiedRelease` producer support; neither placeholder configuration is a release certificate.

`-Pknittools.isolatedTest=activepolicy|notes|onboarding` configures debug packages `.activepolicytest`, `.notestest` or `.onboardingpreview`. [app/src/isolatedTest/AndroidManifest.xml](app/src/isolatedTest/AndroidManifest.xml) removes INTERNET/BILLING. Active-policy tests disable ordinary debug feature unlock. The onboarding preview uses `app/build/onboarding`, a KnitTools Preview label, persisted preview setup and simulated offer/purchase UI; its release facade is a no-op. These profiles prevent destructive/controlled tests from using the normal app package; runner arguments and package checks still have to match the chosen scenario.

`-Pknittools.optimizedSmoke=true` is a **different**, opt-in artifact path for `benchmarkRelease` and `nonMinifiedRelease`, using `com.finnvek.knittools.optimizedtest`. It requires non-debuggable, nonproduction-signed profile artifacts and verifies minification/resource shrinking for benchmarkRelease. [app/src/optimizedSmoke/AndroidManifest.xml](app/src/optimizedSmoke/AndroidManifest.xml) marks the target test-only and removes INTERNET/BILLING; it gets generated placeholder Firebase configuration and an empty analytics token. Ordinary release still requires the real release prerequisites below. This test artifact is not a distributable release.

Release artifact tasks require:

1. `KNITTOOLS_KEYSTORE_PATH`, `KNITTOOLS_KEYSTORE_PASSWORD`, `KNITTOOLS_KEY_ALIAS`, `KNITTOOLS_KEY_PASSWORD`, and an existing keystore.
2. Real ignored `app/google-services.json` or `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64`, validated for the application package.
3. A syntactically valid PostHog project token from `KNITTOOLS_POSTHOG_PROJECT_TOKEN` or ignored `posthog.properties` (`projectToken`). SDK consent remains off by default even when the build requires ingestion configuration.

Debug/benchmark/nonminified producer variants can use generated ignored placeholders if no real Firebase configuration exists. Release artifacts cannot. `lintRelease` without artifact tasks may skip Google Services generation when real configuration is absent; passing lint in that path is not proof of a signed release artifact. Debug DSN uses `KNITTOOLS_SENTRY_DSN`/`SENTRY_DSN` or ignored local debug configuration.

The build disables generated Hilt unit-test Java compile tasks through a targeted workaround and reads AGP built-in Kotlin output directories for JaCoCo. Do not assume older AGP class paths or automatically remove these compatibility details without checking the owning configuration/tests.

### Baseline Profile and backend build

[baselineprofile/build.gradle.kts](baselineprofile/build.gradle.kts) uses the Android test plugin, namespace `com.finnvek.knittools.baselineprofile`, target `:app`, and connected-device producer configuration. A generator/profile installer is configured; no new profile generation or measured startup/render performance was executed here.

[functions/package.json](functions/package.json) is private, targets Node 22 and `lib/index.js`, builds with TypeScript 7.0.2, and tests by build then `node --test "lib/**/*.test.js"`. It declares firebase-admin 14.5.0, firebase-functions 7.4.0, @types/node 26.6.6 and pinned transitive overrides. Declared type-package version is separate from the Node runtime. Deployment uses root Firebase config with predeploy compilation; no deployment, secrets/rules change or live function call occurred here.

### Compatibility pins, generated outputs, and release artifacts

| Configuration | Why it matters |
| --- | --- |
| AGP built-in Kotlin plus Compose/serialization plugins | There is no separate Kotlin Android plugin declaration; compiler/plugin updates must follow the actual AGP toolchain |
| Detekt alpha.5 + Compose Rules 0.6.3 | Repository-approved temporary compatibility exception; incompatible rules introduce another Detekt core |
| Dependabot Compose Rules ignore | `io.nlopez.compose.rules:*` versions >=0.6.4 are excluded until the Detekt compatibility decision changes |
| Runtime constraints | Serialization core, Guava Android, Kotlin stdlib-jdk7, WorkManager runtime/ktx and Wire are deliberately constrained; catalog presence alone is not a direct dependency |
| OWASP 13 task data directory | Direct task property override compensates for plugin default/convention behavior; removal needs owning-tool verification |
| Hilt unit-test task workaround | `hiltJavaCompile*UnitTest` tasks disabled deliberately; JVM tests still compile/run through their configured path |
| Compose stability | Shared config explicitly marks `java.time.LocalDate` immutable; debug baseline is tracked, other dumps local |
| JaCoCo | AGP built-in Kotlin/Javac output directories used, with a nonempty-class guard; old Sonar property paths are overridden by managed Gradle integration |
| Gradle wrapper | Distribution SHA-256 verification and URL validation are configured; no wrapper update performed here |
| Dependency verification | Checksums include previous artifacts too; presence of an old component/version is not evidence it is currently resolved |

Debug APK output is normally `app/build/outputs/apk/debug/app-debug.apk`; Android-test APK and release/bundle outputs are generated below the usual `app/build/outputs` trees. Outputs are ignored and must not be treated as inspected source. Release R8 uses the optimized default configuration plus the currently minimal project ProGuard file. Resource shrinking and R8 full mode are enabled. Mapping upload is enabled only for the release build type; no mapping was uploaded here.

The Baseline Profile generator currently collects **startup only** with `startActivityAndWait()`. Its source comment's broader references to critical navigation/counter paths do not constitute scripted interaction coverage; no such additional scenario is present in the generator. The module uses connected-device generation, not a configured managed-emulator matrix.

Backend runtime/tooling versions are declared separately: firebase-admin 14.5.0, firebase-functions 7.4.0, TypeScript 7.0.2, Node types 26.6.6, Node runtime 22. Type declarations do not raise the deployed runtime. Package overrides are brace-expansion 5.0.12, form-data 4.0.6, glob 14.0.3, js-yaml 5.2.0, protobufjs 7.6.5, rimraf 6.1.3 and uuid 11.1.1. The independent npm lockfile governs installation.

## CI, analyzers, and release configuration

### GitHub configuration

[build.yml](.github/workflows/build.yml) runs on main pushes and PRs targeting main, with read-only contents permission and commit-pinned actions. It currently defines **three jobs**:

| Job | Configured execution | What it does not establish |
| --- | --- | --- |
| `build` | Checkout v7.0.1; Temurin 17 through Setup Java v6.1.0; Gradle Setup v6.4.1; debug assemble; `:app:testDebugUnitTest`; app and baselineprofile ktlint; app Detekt; lint including `:app:lintRelease`; Sonar with quality-gate waiting; test/lint report upload | No signed production artifact, complete instrumentation matrix or live Play verification |
| `optimized-smoke` | Nonproduction optimized target/external driver build; disposable API 36 `aosp_atd` x86_64 Pixel 2 emulator; two explicitly selected smoke methods; APK/driver/mapping identity and diagnostics retained | The remaining driver methods, all-device coverage, performance measurements and production-service delivery are not implied |
| `functions` | Setup Node v7.1.0, Node 22, `npm --prefix functions ci --ignore-scripts`, build/test | No deployed Functions/Firestore/Secret Manager verification; emulator-dependent Node cases can be skipped without an emulator host |

Hosted Sonar runs for main pushes and same-repository PRs; the condition excludes fork-PR uploads. It uses `SONAR_TOKEN` and `-Dsonar.qualitygate.wait=true` with a 20-minute step timeout. Existing source configuration does not prove the token is present or the current hosted quality gate passes. Upload artifacts use v7.0.2 and seven-day retention.

Workflow concurrency groups runs by workflow name and PR number, falling back to the individual run ID outside a PR. `cancel-in-progress` is enabled only for `pull_request` events: a newer run for the same PR cancels its older in-progress workflow. Main-push runs have separate groups and are not cancelled by this rule. This is current local YAML configuration, not evidence that the change has been pushed or exercised by GitHub.

The optimized job explicitly selects `projectCounterSessionAndNavigation` and `freeGateTrialAndAdaptiveTransitions`, expecting **two** completed tests. Those cover project/counter/Undo/navigation, explicit session stop/save across process restart, free-project admission, local trial and selected window transitions. The driver class has **seven** test methods in source; default local helper expectations and a CI subset are different scopes. No pipeline result was fetched for this documentation task.

There is no checked-in `.github/workflows/codeql.yml` at this snapshot. `tools/ql.ps1` is a local CodeQL checker delegate; it does not establish a scheduled GitHub CodeQL workflow. The older PROJECT description of a pinned/scheduled CodeQL action was stale.

[dependabot.yml](.github/dependabot.yml) schedules weekly Gradle, GitHub Actions, `.deepsec` npm, and `functions` npm updates, with a seven-day version cooldown and a Compose Rules compatibility ignore. No CI deployment or automatic app publication workflow is established by these files.

### Direct checks and wrapper ownership

Useful Windows commands below describe available validation, **not commands run in this documentation task**. Choose the smallest owning check for an authorized code change.

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest --tests "fully.qualified.TestClass"
.\gradlew.bat :app:ktlintCheck
.\gradlew.bat :app:detekt
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:lintRelease
.\gradlew.bat :app:debugStabilityCheck
.\gradlew.bat :app:jacocoDebugUnitTestReport
```

[config/android-check.json](config/android-check.json) defines modules/source sets/variants, build tasks (`:app:assembleDebug`, `:baselineprofile:assemble`), JVM task, regular/full lint, stability, dependency configurations and Semgrep config. Most short PowerShell wrappers delegate to `ANDROID_CHECK_ROOT/tools/InvokeProjectCheck.ps1`, defaulting to `C:\Dev\Android-check`; invalid/missing checker roots fail closed. Their real behavior also depends on that external checker installation.

| Wrapper | Check command / boundary |
| --- | --- |
| `tools/ac.ps1` | Aggregate `android-check` |
| `bc.ps1`, `tc.ps1`, `lc.ps1` | Build, test, lint delegates |
| `sc.ps1`, `ss.ps1` | Aggregate security / secret scan |
| `cs.ps1`, `cr.ps1`, `pc.ps1` | Compose stability, Compose rules, PMD CPD |
| `dc.ps1`, `os.ps1` | OWASP/dependency and OSV |
| `ms.ps1`, `ga.ps1`, `ql.ps1` | MobSF, Android security lint, CodeQL |
| `ds.ps1`, `db.ps1` | DeepSec, Dependabot |
| `sentry.ps1` | Diagnostics tooling delegate |
| `sonar.ps1`, `rs.ps1`, `rst.ps1` | Dedicated Sonar/release-surface helpers and their tests |

`tools/pc.ps1` defaults `PMD_CPD_MINIMUM_TOKENS` to 100 only when absent. `tools/cs.ps1` compares the debug stability report against [app/stability/app-debug.stability](app/stability/app-debug.stability) and [stability_config.conf](stability_config.conf). Baselines are reviewed state, not permission to silence new violations. Repository guidance reserves aggregate lint/security workflows for explicit authority; none was invoked here.

### Analyzer semantics and exception inventory

- **Android Lint:** aborts on errors; warnings are not globally fatal. Explicit rules include API, permissions, translation/hardcoded text, resources, recycling/leaks, thread use, RTL and descriptions. Regular checks target debug; full config also includes release.
- **ktlint / Compose rules:** root/app format checks exclude generated/build sources; failures are not ignored. Compose rules are supplied to both relevant analyzers.
- **Detekt:** [config/detekt/detekt.yml](config/detekt/detekt.yml), parallel analysis as configured, and [app/detekt-baseline.xml](app/detekt-baseline.xml). Current static baseline: zero manually suppressed, 22 current issue IDs; this is not a fresh scan verdict.
- **OWASP:** plugin 13.0.0, debug/release runtime classpaths, HTML/JSON, default fail CVSS ≥7, local suppression XML, configurable data/update/NVD key/delay/retry. Direct task data-directory handling accounts for the plugin compatibility workaround. Hosted suppressions, KEV, OSS Index and RetireJS are disabled; node audit can use configured pnpm. Advisory coverage is limited to enabled analyzers/data.
- **OSV:** wrapper/checker dependency scanning with exception ownership and bounded operations. A no-findings receipt is different from zero dependencies or an unexecuted query.
- **Semgrep / MobSF / secret scanners:** [config/semgrep/knittools-security.yml](config/semgrep/knittools-security.yml), project custom rules and checker delegates; gitleaks/TruffleHog results and exact exceptions must be interpreted from raw receipts, not summary adjectives.
- **Sonar / JaCoCo:** root analysis depends on `:app:jacocoDebugUnitTestReport` and `:app:compileDebugAndroidTestJavaWithJavac`; the Android-test Java output supplies Sonar test binaries; [sonar-project.properties](sonar-project.properties) supplies project configuration. JaCoCo reads Kotlin/Javac debug classes, rejects empty class inputs, and excludes generated code plus UI/widget/billing/pro/framework/storage/local/remote areas. Its percentage describes included classes, not whole-app runtime coverage. Sonar execution uploads data and needs separate authorization.

At the inspected snapshot, [config/check-exceptions.json](config/check-exceptions.json) has **611 registered entries**: 485 gitleaks, 61 MobSF (`mobsfscan`), 60 TruffleHog, 2 OSV, 1 Semgrep, 1 Detekt and 1 OWASP. **60 expire November 30, 2026; 551 expire January 18, 2027.** These are registry entries, not numbers of active vulnerabilities or findings observed by this documentation task. Exact scope, selector, rationale and expiry matter; a registered exception does not authorize hiding a rule globally or treating an unexecuted scanner as clean.

[dependency-check-suppressions.xml](config/dependency-check-suppressions.xml) still has **12 suppression nodes**. A receipt's applied-suppression count (for example 14 in the Oct-16 security receipt) is a run count, not the current XML-node count. Keep rationale/path/expiry, registered entries, applied findings and scanner scope distinct. The stability baseline additionally records `BadgePill`'s new stable Boolean `uppercase` parameter; this inventory update is not a new stability run.

### DeepSec and release-surface checks

[.deepsec/package.json](.deepsec/package.json) pins DeepSec 2.3.9; [.deepsec/deepsec.config.ts](.deepsec/deepsec.config.ts) registers Android/Kotlin entry point, exported component, intent input, FileProvider/path, URI grant, widget mutation, sensitive logging and historical Ravelry credential/callable matchers. Root npm scripts forward into that workspace. A source scan selects candidates; process/revalidate uses an external model; export emits stored finding state. Reusing prior analyses or finding zero new files is not fresh semantic analysis of every source file.

`.deepsec/scripts/mark-accepted-risks.mjs` synchronizes narrowly identified historical credential findings with `config/security-decisions.md` and refuses reintroduced removed-risk surfaces. Old auth priority paths/prompt wording can remain historical configuration; they do not restore Android auth. Built-in TS/JS/Python matcher coverage alone is insufficient for the Kotlin application. Agent-assisted review is not a substitute for dependency SCA or device testing.

[tools/release-surface.ps1](tools/release-surface.ps1) checks release dependency/manifest/exported/credential/schema boundaries; companion release-surface, exported-surface and archive tests validate its own behavior. It allows Firebase BOM/Crashlytics and Google Services for Crashlytics while prohibiting AI/ML Kit/Gemini/voice and tracked private Google configuration, and scans locally/env-known Ravelry secret values without printing them. Firebase Auth and Functions are not approved Android dependencies; the forbidden-dependency checks enforce their absence.

Static release-surface success cannot establish a signed artifact, actual R8 behavior, provider/runtime grants, live telemetry, Play compliance, website permission or deployed Functions. Release readiness additionally needs valid signing/config, privacy/contact/release disclosures, supported platform tests and applicable Play/site decisions. Nothing was published or uploaded in this task.

### Configured execution versus current readiness

The single checked-in workflow file is `build.yml`, with Android build/analysis, optimized emulator smoke and Functions jobs. It configures Sonar quality-gate waiting and a limited API 36 smoke subset. It does not establish a complete screenshot/accessibility matrix, measured benchmark, signed production release, live Billing purchase, Firebase deployment or automatic Play publishing. Repository code does not establish GitHub branch protection, actual secret availability, current remote CI status or Play Console setup; those external states were not queried.

`tools/ad.ps1` is the install delegate and is intentionally separate from inspection. Other script tests cover wrapper portability, security delegation, pnpm dependency-check configuration, Sonar planning/timeouts, and release surface/archives. Their existence verifies neither current outcomes nor authorization to invoke scanner/upload/device operations during a documentation task.

The Sonar helper permits a read-only `-PlanOnly`; an actual upload requires `-AllowExternalUpload` and `SONAR_TOKEN`, and managed Gradle execution has a timeout. CLI authentication for issue reading is a separate mechanism. A successful upload task or an empty local issue response is not the authoritative hosted coverage/quality-gate result for a later revision.

Configured suppression inventories are not a scanner pass. Parse/tool errors, invalid/missing fresh reports and excluded source scope must remain visible. `REPORT_INVALID` or `TOOL_ERROR` with zero parsed findings is not a clean result. Detekt baseline count, registered exceptions, XML suppression nodes, applied findings, advisories and unsuppressed findings measure different things.

## Testing and evidence

### Current test architecture

The inspected working tree has **387 Kotlin files in `app/src/test`, 1 in `testDebug`, 1 shared test helper in `sharedTest`, and 206 Kotlin plus 2 Java files in `androidTest`**. Of these, two JVM and three Android helper files are currently untracked; the tracked counts remain 385 and 203 respectively. Shared source is included by both JVM and Android tests, so adding these counts does not produce a number of distinct tests. `baselineprofile/src` has six Kotlin files, including an external driver with seven optimized-smoke methods; CI selects two. Functions has 14 `.test.ts` files among 33 TypeScript files. These inventories include helpers/fixtures and are not execution counts or pass results.

The five local helper files belong to the inspected test implementation and must accompany dependent test changes when reproducing this working tree elsewhere:

| Source root | Local helper | Responsibility |
| --- | --- | --- |
| `app/src/test/java/com/finnvek/knittools` | [repository/CounterHistoryTestSupport.kt](app/src/test/java/com/finnvek/knittools/repository/CounterHistoryTestSupport.kt) | Shared MockK history/count recorder used by repository tests |
| Same JVM root | [ui/theme/ContrastTestSupport.kt](app/src/test/java/com/finnvek/knittools/ui/theme/ContrastTestSupport.kt) | Shared relative luminance and alpha-composited contrast calculation |
| `app/src/androidTest/java/com/finnvek/knittools` | [ui/ViewTestSupport.kt](app/src/androidTest/java/com/finnvek/knittools/ui/ViewTestSupport.kt) | Native View descendant traversal for runtime assertions |
| Same Android root | [ui/screens/backup/BackupTestContext.kt](app/src/androidTest/java/com/finnvek/knittools/ui/screens/backup/BackupTestContext.kt) | Fixture-owned files/no-backup directories through a Context wrapper |
| Same Android root | [ui/screens/counter/CounterActivityTestFixture.kt](app/src/androidTest/java/com/finnvek/knittools/ui/screens/counter/CounterActivityTestFixture.kt) | Shared splash rule, Activity Compose rule and app/database/repository fixture fields |

These are helper counts, not five additional executable test cases. No files are staged or committed by this documentation update.

| Mechanism | Representative current source | What it verifies / limit |
| --- | --- | --- |
| Pure JVM domain tests | [domain/calculator](app/src/test/java/com/finnvek/knittools/domain/calculator), [domain/model](app/src/test/java/com/finnvek/knittools/domain/model) | Arithmetic/overflow, labels, timing, URLs, payload/page geometry, row mapping and units; no platform PDF/provider/Room runtime |
| Repository JVM tests | [repository](app/src/test/java/com/finnvek/knittools/repository) | Transaction contracts, fake DAO failures, main/history/link/session/notes/usage results and stale actions; fakes are not SQLite FK/trigger proof |
| Coroutines/ViewModels | [ui/screens](app/src/test/java/com/finnvek/knittools/ui/screens) | State reducers, restoration, selected-context races, loading, cancellation and aggregates with test schedulers/MockK/Turbine |
| Billing/trial JVM | [billing](app/src/test/java/com/finnvek/knittools/billing), [pro](app/src/test/java/com/finnvek/knittools/pro) | Clock/tamper/boot boundaries, purchase recovery/readiness/ack states; not real Play Console/backend validation |
| Backup JVM | [data/backup](app/src/test/java/com/finnvek/knittools/data/backup) | ZIP structure, hashes/budgets/identity rewriting, provider deadlines/admission/rejection/late work with controlled fakes |
| Real Room instrumentation | [data/local](app/src/androidTest/java/com/finnvek/knittools/data/local), [repository](app/src/androidTest/java/com/finnvek/knittools/repository) | Migrations, FKs/triggers, coherent snapshots/concurrency, all-table replacement and rollback on Android SQLite |
| Compose instrumentation | [ui/screens](app/src/androidTest/java/com/finnvek/knittools/ui/screens), [ui/components](app/src/androidTest/java/com/finnvek/knittools/ui/components) | Route/back, dialogs/sheets, counter/session/usage/reader interaction, focus and semantics; only the actual executed configuration is proven |
| Rendering/storage instrumentation | [data/storage](app/src/androidTest/java/com/finnvek/knittools/data/storage) | Android renderer/canvas/storage behavior, bounded PDF/image processing |
| Localization/accessibility | Resource/source contracts, `YarnUsageLocalizationTest`, image-button semantics and usage-focus tests | Names/format rules/ICU plurals/selected semantics; not native-language editorial quality or universal 200% layout fit |
| Source-contract tests | [ArchitectureSingleSourceSourceTest.kt](app/src/test/java/com/finnvek/knittools/ArchitectureSingleSourceSourceTest.kt), UI/release/ownership source tests | Specific structural/textual decisions; intentionally different from executing those user flows |
| Functions Node tests | [functions/src](functions/src), npm build/test script | Validation, limits, token generation/refresh, callback/completion races using harnesses; no deployed secrets/rules/functions/site behavior |
| Baseline Profile | [baselineprofile/src](baselineprofile/src) | Startup-only producer scenario; measured results need an actual authorized device/emulator run |
| Optimized external runtime driver | [OptimizedSmokeTest.kt](baselineprofile/src/benchmarkRelease/java/com/finnvek/knittools/baselineprofile/OptimizedSmokeTest.kt), [optimized-smoke.ps1](tools/optimized-smoke.ps1) | UiAutomator against a non-debuggable R8/resource-shrunk test artifact; helper checks package/manifest/APK/driver/mapping identity and expected executed-test count; disposable emulator only |

Migration-specific instrumentation covers layered annotations (17), availability rebuild (19), bookmarks/guides (20), active sessions (21), project documents (22), folders (23), and broader migration paths. Schema-24 usage/schema-25 completion source/export/backup tests add their own boundaries; do not label every schema change as freshly instrumented from file existence.

Counter/session tests cover actual main-value rereads, concurrent writes, Undo eligibility, linked deltas, active checkpoint and recovery token idempotence. Real Room snapshot tests exercise cancellation releasing writers, project deletion/history replacement between batches and keyset filtering. Backup tests include 10,000/10,001/100,000 accepted sessions, 100,001 rejection, identity/annotation/file failures, and provider ownership. Native history is retained; the admission ceiling does not truncate older oversize histories.

### Feature-to-test ownership map

The following classes show where to select a focused check. They are present source, **not a claim of a fresh passing run**.

| Behavior / realistic failure path | Current owning tests |
| --- | --- |
| Active-project quota and reopening races | `ActiveProjectPolicyTest`, `ActiveProjectPolicyRoomTest`, isolated `ActiveProjectPolicyRuntimeTest`, `CounterReactivationPolicyTest` |
| Onboarding persistence, initialization and backup markers | `OnboardingStateRepositoryTest`, `LastProjectCraftPreferenceTest`, backup ViewModel tests; onboarding state is separate from backup contents |
| Eight-step onboarding, draft restoration, receipt idempotence and PDF cleanup | `OnboardingDraftTest`, `OnboardingViewModelTest`, `OnboardingProjectRoomTest`, `OnboardingPdfStorageTest`, `OnboardingNavigationTest`, `OnboardingScreenTest`; project commit and DataStore completion are separate failure boundaries |
| Toolbox selection order/animation and price modes | `OnboardingToolboxTest`, `OnboardingToolboxAnimationTest`, `OnboardingPricePolicyTest`, `OnboardingPreviewTest`, `AdaptiveOnboardingLayoutTest`; preview and real purchase are separate paths |
| Starting count, target, pending draft compatibility and creation form | `ProjectStartValuesTest`, `CounterRepositoryCreateProjectTest`, `ProjectCreationActionsTest`, `ProjectCreationStateTest`, `ProjectDetailsSheetTest`; real Room and isolated policy cases in `ActiveProjectPolicyRoomTest` / `ActiveProjectPolicyRuntimeTest` |
| Projects start-state eligibility, dismissal, Tools and local demo | `ProjectsStartStateViewModelTest`, `ProjectFoldersScreenTest`, `WelcomeExampleCardTest`; no repository writes from the example and once-per-process demo analytics |
| Tip triggers, process budget, placement, input and reset | `TipRulesTest`, `TipSessionTest`, `TipHostTest`, `SettingsViewModelTest`, committed-counter and import feedback tests; no quota consumed before visible placement |
| Project setup, visit-scoped completion and explicit work-session guidance | `ProjectSetupProgressTest`, `CounterProjectSetupTest`, `ProjectSetupListTest`, `ProjectSetupNavigationTest`; TipSession/TipHost tests cover manual requests, unchanged automatic budgets, route ownership and delayed placement |
| Bundled guide content, actions and graph ownership | `GuideContentTest`, `GuideResourcesSourceTest`, `GuideViewModelTest`, `GuideScreenTest`, `GuideNavigationTest`; topic round trips, unknown-topic fallback, all actions, loaded project selection, stale-entry rejection, Pro readiness and reset feedback |
| Contextual Pro marker states, contrast and semantics | `ProActionAccessTest`, `ProMarkerContrastTest`, `ProMarkerTest`, `UiConsistencySourceTest`; context/status/readiness/flag matrix, shared resolver calls and unchanged exception lists |
| Pro consumer readiness, discovery and widget pinning | `ProPresentationConsumersTest`, `ProjectFoldersScreenTest`, `CounterProActionsTest`, `CounterProReadinessTest`, `CounterWidgetPinHostTest`; unfiltered project quota, independent marker/control readiness, merged action semantics, visible gated actions and supported/unsupported/declined pin requests |
| Committed target, first-session and completion celebrations | `CounterRepositoryCelebrationTest`, `CounterCelebrationSignalsTest`, `CounterCompletionCelebrationTest`, `ProjectListCompletionCelebrationTest`, `OnboardingStateRepositoryTest`, `TipSessionTest`, `TipHostTest`, `ProjectCompletedSheetTest`, `ProjectFoldersScreenTest`; transactional facts/rollback, visible per-project target memory, durable first-save identity, craft-aware facts, exact event consumption and Next with folder/quota preservation |
| Trial journey facts, progress, start queue and three-day notice | `TrialJourneyStateRepositoryTest`, `TrialThreeDayNoticeTest`, `OnboardingStateRepositoryTest`, `ProjectCounterRepositoryTest`, `TrialJourneyStartViewModelTest`, `InsightsTrialJourneyTest`, `PatternImageImportViewModelTest`, `TrialJourneyUiTest`, `TrialJourneyPresentationTest`, `TrialJourneyNavigationTest`, `TrialNoticeRestorationTest`; real loading, half-open trial window, active-project targeting, markers, successful-start queueing, actual visible/import completion, modal settling/focus, restored direct actions, atomic claims, persistence failure and notice recreation without repeat claims. Execution receipts are tracked separately in `reports/onboarding-implementation/PROGRESS.md` |
| Onboarding resource lifecycle | `OnboardingResourcesSourceTest`, `BrazilianPortugueseLocaleContractTest`, `ReferenceResourceLocalizationTest`, `OnboardingLocalePresentationTest`; configured locale parity, types/placeholders/plurals/comments and live consumers; no MissingTranslation suppression |
| Current-row main writes, Undo and widget semantics | `CounterRepositoryMainCounterChangeTest`, `CounterHistoryRoomTest`, `CounterRepositoryWidgetActionTest`, `CounterMainWriteResultTest` |
| Snapshot-backed Undo, clock reversal and late observation | `CounterUndoIntegrityRoomTest`, `CounterSessionUndoTest`, `CounterHistoryContractTest`, `CounterUndoSourceTest`, counter ViewModel tests; actual linked deltas, same-session/interval row accounting and forward-only elapsed time |
| Main step parsing, whole-tap Undo and isolated persistence | `CounterLogicTest` (1–99 parsing/rejections, arithmetic bounds and whole-step Undo); `CounterRepositoryMainCounterChangeTest` (step setter validation, missing project, exact DAO write/transaction; canonical count mutations) |
| Main step async save, failure/retry, stale selection and feedback | `MainCounterStepViewModelTest` (actual clamped feedback, step-1/no-op suppression, failed/retried write, intervening count observation, switched/reopened selection, selection change before coroutine start, cancellation/unlocked retry, invalid/stale requests) |
| Main step sheet and control hit regions | `MainCounterStepSheetTest` (preset/custom exclusivity, invalid input, failed-save retry and disposal cancellation; en/fi/de × both themes at combined 320 dp/200%); `CounterTouchBoundsTest` (×1/×5/×99, control separation/semantics, ring opening and narrow/large-font configurations) |
| Session anchors, recovery, completion/deletion choices | `ActiveSessionTimingTest`, `CounterRepositoryActiveSessionTest`, `WorkSessionComposeTest`, `ActiveSessionProcessTest` |
| Completion events and Insights filtering | `ProjectCompletionContractTest`, `InsightsCompletionModelTest`, `InsightsCompletionViewModelTest`, `InsightsCompletionSectionTest` |
| Folder uniqueness, moves, restoration and SQL behavior | `ProjectFolderRepositoryTest`, `ProjectFolderDatabaseTest`, `ProjectFolderRepositoryIntegrationTest`, `ProjectFoldersViewModelTest` |
| Counter/form domain rules and shaping cadence | `ProjectCounterLogicTest`, `ProjectCounterRepositoryTest`, `AddCounterFormValidationTest`, `ShapingCounterSourceTest`, `RepeatSectionLogicTest` |
| Notes drafts, concurrent base merge and failure | `CounterRepositoryNotesMergeTest`, `NotesEditorViewModelTest`, `NotesEditorFailureTest` |
| Project yarn note identity, normalization and concurrent writes | `ProjectYarnNoteEditRoomTest`: same-row update and database reopen, linked/unlinked usage preservation, invalid name, failure before commit, cancellation before/after commit, competing editable fields/deletion/Save to My Yarn, ownership checks and simultaneous editors |
| Project yarn note editor state and accessibility | `ProjectYarnNoteEditViewModelTest`: project guard, no Pro check, failure results and cancellation propagation; `ProjectYarnNoteEditUiTest`: prefill, Cancel, save/reopen, validation/retry, conflict/missing errors, pending-save dismissal guard, keyboard/scrolling, 48 dp touch targets, text fit and native accessibility focus; en/fi/de × both themes, with 320 dp, 200% font and their combination in the device matrix |
| Yarn reciprocal links, replacement photo and usage revisions | `YarnCardRepositoryDomainApiTest`, `YarnCardPhotoReplacementTest`, `ProjectYarnUsageRepositoryTest` (JVM and Android), usage focus/runtime/localization tests |
| PDF relation ownership, primary constraints and selection | `ProjectDocumentRepositoryTest`, `ProjectDocumentDatabaseTest`, `ProjectDocumentRuntimeIntegrationTest`, `PatternViewerViewModelTest` |
| Guides, mapping, bookmarks and document lifecycle | `ReadingLineLocationResolverTest`, `RowMappingParserTest`, `PatternBookmarkLifecycleTest`, `PatternReadingAssistanceComposeTest` |
| Annotation normalization, context races, pressure and budgets | `PatternAnnotationPayloadCodecTest`, `PatternAnnotationGeometryTest`, `PatternAnnotationEditorContextTest`, `PatternAnnotationInputPressureTest`, page-budget tests |
| Existing text annotation editing, raw payload preservation and stale writes | `PatternAnnotationPayloadCodecTest`, `PatternAnnotationTextRoomTest` with `AnnotationTextTestFixture`: validation, identity/style/unknown-field preservation, database reopen, cancel, permissions, history, stale context/deletion and commit/cancellation boundaries |
| Text editor interaction, translations and accessibility | `PatternAnnotationTextUiTest`: real toolbar/ViewModel/Room wiring, prefill, Save/Cancel, Undo/Redo, read-only master, concurrent-write error/draft retention, keyboard/scrolling, touch bounds, text fit and native accessibility focus |
| PDF/image import and bounded raster export | `PatternDocumentFilesTest`, `PatternImageImportViewModelTest`, `PatternImagePdfPipelineTest`, `PatternPdfExporterTest`, `PatternPdfExportBudgetTest` |
| Backup structure, limits, rollback, provider admission/deadline | `BackupArchiveTest`, `BackupZipStructureTest`, `BackupProviderAdmissionTest`, `BackupProviderDeadlineTest`, `BackupRepositoryTest`, `BackupRestoreProcessTest` |
| Shared text/PDF intent queue and restoration | `PatternShareCoordinatorViewModelTest`, `PatternShareIntentRestoreTest`, `IncomingPdfViewModelTest`, `ExternalActivityResultOwnershipTest`; distinguish original-Intent/saved-state reconstruction, stale-URI callback rejection and controlled owner cancellation from actual OS process-kill evidence |
| Ravelry browser navigation, restoration, renderer loss and downloads | `RavelryLinksTest`, `RavelryBrowserNavigationTest`, `RavelryBrowserStateTest`, `RavelryRendererLossTest`, `RavelryBrowserSourceTest`, `WebPdfDownloaderTest`; bounded eligible history and stale-instance rejection are distinct from live site/login behavior |
| General website validation/external intents | `WebPatternUrlTest`, `WebPatternRepositoryRoomTest`, `ExternalWebLinkIntentTest`, editor route/screen tests |
| Play/trial/readiness failure paths | `BillingManagerTest`, `BillingRecoveryTest`, `TrialManagerTest`, `ProManagerTest`, `WidgetReadinessRuntimeTest` |
| Launch quote validity, refresh, explicit re-tap and shared price UI | `OneTimePurchaseOfferTest`, `BillingPriceRefreshTest`, `LaunchPriceTagTest`, `LaunchPriceTagUiTest`; finite eligible launch versus regular offers, expiry/generation and presentation |
| Visual semantics/design contracts | `UiConsistencySourceTest`, `CounterTouchBoundsTest`, `SegmentedToggleTest`, `SheetTitleTest`, `ProjectListContrastTest`, palette/font-fallback tests |
| Gauge canonical precision and restoration | `MeasurementCalculatorTest`, `MeasurementNumberParserTest`, `GaugeViewModelTest`, `GaugeScreenTest`, `GaugeNavigationRuntimeTest` |
| M1 capacity/placement and exact cast-on repeat choice | `IncreaseDecreaseCalculatorTest` expands easy/balanced output for 2–50 stitches in both styles, checks unique valid bars and total stitches; `CastOnCalculatorTest` covers 11.6/12.0/12.4 midpoint behavior, edge additions and metric/imperial input |
| Reference ranges, Japanese diameters and shared symbols | `NeedleSizeDataTest`, `SizeChartDataTest`, `ChartSymbolDataTest`; `SharedAuditSymbolsRuntimeTest` exercises production chart/care renderers in both themes; geometry/source coverage is distinct from an independent standards review |
| Historical counter units after label edits | `InsightsCounterUnitsTest` and real-Room `InsightsCounterUnitsRuntimeTest` preserve exact time/net progress while Rows/Rounds/Repeats/custom labels and all/project filters change; assert row/pace output stays unavailable |
| Stored-zone dates beyond device today | `InsightsSessionZoneRangeTest` covers All Time/week/month/project filters, first-date clamping, active-day denominators, unchanged durations and English compositions; JVM composition is not Android ICU/rendering |
| Relative month plural selection | `InsightsRelativeMonthTextTest` covers both production last-worked components at 30/59/60/90 days plus day/week controls and Spanish plural selection |
| Regional language integration | `AppLanguageTest`, `BrazilianPortugueseLocaleContractTest`, `ReferenceResourceLocalizationTest`, `AppLanguageObservationTest`, `BrazilianPortugueseResourcesTest`; tag matching, resources/placeholders, plural categories and platform synchronization |
| Insights snapshot/cancellation/resource behavior | `SessionInsightsDatabaseTest`, `InsightsQueryCancellationTest`, `InsightsDeviceBudgetTest`, accumulator/chart/fabric/metrics tests |
| Widget host and refresh | `WidgetHostRuntimeTest`, `CounterWidgetDataResolverTest`, `CounterWidgetRefreshTest` |
| Consent delivery/queue revocation, initialization races and data minimization | `PostHogAnalyticsTest`, `PostHogAnalyticsConcurrencyTest`, `PostHogQueueConsentTest`, `PostHogAndroidConsentRuntimeTest`, `AutomaticBackupRulesTest`; local/real-SDK controlled transport assertions are distinct from vendor delivery/retention |
| Default Compose haptics respect preferences | `PreferenceAwareHapticFeedbackTest`, `CounterHapticSourceTest`; shared long-press gate and explicit counter-vibration policy |

Locale source tests now discover configured tags through `ProjectSourceFiles.configuredResourceDirectories()`, translating regional `pt-BR` into Android's `values-pt-rBR` qualifier, and parse all applicable text-resource files. Existing source/UI tests were aligned to neutral counter text, hidden historical pace, and current resource placement. Runtime fixtures also wait for relevant PDF readiness, scope duplicate selectors, and measure actual rendered text lines; such test repairs do not establish new product behavior.

The Android suite includes destructive/process-style scenarios and opt-in fixture controls. Use the isolated application profiles and explicit runner arguments where required; ordinary connected instrumentation must not be assumed safe against a user's normal app data. The existence of a process probe does not prove that every kill point or all OEM process policies were exercised.

### Review questions and regression scenarios

Use the contracts below to formulate focused questions. For each one, trace the current route → ViewModel → repository/domain → DAO/storage and select tests from the ownership map above. A scenario is a verification target, not an assertion that a defect exists or a fresh test passed.

| Area | Concrete question / boundary to verify |
| --- | --- |
| First launch | If project creation commits but draft persistence fails, does reload find the same draft receipt and avoid another project? Does an interrupted Ready stay Ready? |
| Startup routing | Can a pending external PDF/share/widget request coexist with onboarding, splash readiness and trial notices without being consumed by another route? |
| PDF replay / replacement | After restoring the original system Intent, is staged or discarded PDF A left consumed? Does a new share still work, and can a callback captured for A ever save or discard replacement PDF B? Is interrupted-copy retry distinguished from completed handling? |
| Creation/quota | Are zero/empty starting count, optional target, hidden advanced fields, current folder and unfiltered active quota validated inside the correct creation path? |
| Main count / linked Undo | With main count 2, step 5 and linked count 1, decrement clamps both to zero. Does Undo restore linked 1 rather than inventing 2, and leave subsequently linked counters alone? |
| Session Undo | After Reset/Undo or a clamped change, are row-accounting snapshots used only in the same session/review interval while elapsed time continues? What happens to old/restored history without snapshots? |
| Async count/step state | Can a suspended post-write read or step save overwrite a newer action/observation or another selected project? Does disposing the step sheet cancel its Job without claiming database rollback? |
| Session lifecycle | Do navigation, backgrounding and ViewModel clearing leave the active row running? Are reboot/unknown boot/24-hour intervals reviewed and repeated finalization tokens idempotent? |
| Completion | Does a single new event celebrate once after selection clears, while bulk/idempotent completion stays quiet and reopening preserves prior events? |
| Tips / setup | Do initial counts avoid increment signals, blocked/unplaced tips avoid premature consumption, resets preserve target/first-save identity, and reused route visits hide completed setup at the correct visit? |
| Project documents | Can metadata replace/unlink/delete leave every PDF intact? Is one primary maintained transactionally, with document-specific guides and stable annotation/bookmark identity? |
| Files / cancellation | Does failure before commit preserve old files, failed replacement remove only its new copy, and post-commit cleanup failure retain the committed result? Are shared PDF references respected? |
| Yarn usage | Do a note and its saved card resolve to one usage row? Are unknown versus zero amounts and revision conflicts preserved without deducting stash or changing project/session state? |
| Reader / annotations | Are coordinates transformed once, manual guide moves distinct from marker writers, stale document callbacks rejected, and an unsafe page rejected without deleting its stored content? |
| Browser | Can an oversized/unsafe history fall back safely, a new request avoid old state, dead renderer callbacks remain inert, and redirects receive cookies for their own URL? |
| Billing | Can expiry, a refreshed higher quote or a late product response launch an unreviewed price? Does the user need a fresh tap, and can an eligible offline trial/free use continue without a price? |
| Pro readiness / widgets | Is Unknown distinct from Locked, and does the composed widget react to later entitlement results without starting a session or bypassing canonical count writes? |
| Backup | Do schema 25–28 compatibility, embedded counter-ID remapping, cleared session snapshots, fresh live identities, file publication and replacement commit remain coherent after failure/cancellation? |
| Analytics | Can rapid off/on, SDK flush/close/retry, late initialization or failed queue deletion deliver old-consent events? Are already in-flight requests distinguished from newly denied delivery? |
| Retained backend refresh | If an obsolete refresh fails after another request rotates the token, does the fallback require changed data, the same connection generation and current validity while preserving the original error otherwise? |
| UI / localization | Do both theme tokens, actual content constraints, current locale/plurals, short-window Undo visibility, shared forms and merged action semantics survive the changed flow? Respect the explicit restriction on rerunning prior accessibility configurations. |
| Evidence | Which assertions are source checks, fake tests, real SQLite/Compose, optimized-driver methods or live-service checks? Does the result record the actual variant, selection and executed count? |

### Turning a review question into a test or UI task

Use the following fields for a bounded task. Apply only the dimensions relevant to the change; this is not a request to run every suite or every combination.

| Field | Specify before judging the result |
| --- | --- |
| User-visible trigger | The route and actual action, including whether it comes from a widget, external share, restored Activity or another tab |
| Starting state | Selected project/document, existing data, loaded versus unknown state, applicable Pro/trial status and meaningful empty/error cases |
| Required result | Observable UI feedback/navigation and authoritative persisted result; name what must remain unchanged when it is a real invariant |
| Race or failure boundary | The relevant point before/after commit, replacement/selection change, owner cancellation, duplicate tap or provider failure |
| Implementation owner | Current route/component, ViewModel, repository/domain operation and DAO/storage boundary from this reference |
| Smallest useful proof | Existing test/class and level; use SQLite for FK/trigger/transaction claims and rendered interaction for geometry/focus claims |
| UI constraints | Existing shared control/token, resource keys/plurals, content width/height, theme and modality; preserve current project wording and navigation |
| Execution receipt | Exact command, branch/HEAD plus relevant dirty changes, variant/package, selected and executed cases, platform and skipped/blocked cases |

Example: start with staged PDF A, capture its Save/Attach/Start Project/Dismiss callbacks, receive PDF B, then invoke A's callbacks. The expected result is that B stays pending and no A callback publishes or deletes B; B's own save still works. The owning implementation is `IncomingPdfHost` plus `IncomingPdfViewModel`, and the existing JVM case is `stale PDF actions do not affect a replacement pending document`. Activity-intent replay needs the separate Android restoration cases. Neither level alone proves provider grants after a real process kill.

For UI work, check loading, empty, populated, invalid, saving, failed-save and restored states only where the feature actually has them. Unknown entitlement is not a purchase failure; a cancelled operation is not automatically a rolled-back commit; a skeleton is not an empty dataset. Reuse the screen/component contracts above, and do not repeat 320 dp, 200% font, their combination or TalkBack without an explicit request for those checks.

### Text annotation editing validation

The owning instrumentation uses [AnnotationTextTestFixture.kt](app/src/androidTest/java/com/finnvek/knittools/ui/screens/pattern/AnnotationTextTestFixture.kt): a uniquely named, file-backed Room database with synthetic project/document/master/overlay data and an assertion that the target package is `com.finnvek.knittools.activepolicytest`. Closing and reopening that database checks persisted state, not just ViewModel memory.

- [PatternAnnotationTextRoomTest.kt](app/src/androidTest/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationTextRoomTest.kt) has 16 cases covering edits and unchanged metadata/unknown JSON fields, reopen persistence, Cancel and late callbacks, repeated Undo/Redo and delete/restore history, stale Undo, invalid text, project read-only versus Library edit permission, page/document changes, deletion, selection changes and competing text/style/geometry writes. The failure test throws after the update inside an outer real Room transaction, before commit. Separate cancellation tests suspend before commit or after the outer transaction has returned, cancel the operation, and assert both the cancellation result and database contents; the post-commit case also reopens the database. These controlled boundaries do not simulate every process-kill or provider failure.
- [PatternAnnotationTextUiTest.kt](app/src/androidTest/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationTextUiTest.kt) has two scenarios for each of en/fi/de/fr × light/dark, giving 16 cases per display configuration. It exercises the production toolbar action wiring with the real ViewModel/repository, long multiline Unicode text, IME visibility, scrolling, text/input and native accessibility focus, minimum 48 dp touch bounds, label fit, read-only behavior and conflict feedback. Instrumentation arguments `textWidth` and `textFontScale` verify the requested device configuration. This is focused editor coverage, not a full PDF-reader journey or spoken TalkBack audit.
- [PatternAnnotationPayloadCodecTest.kt](app/src/test/java/com/finnvek/knittools/domain/model/PatternAnnotationPayloadCodecTest.kt) covers text replacement, unknown-field preservation, escaped Unicode and invalid/oversized/unsupported-version input. Existing `PatternAnnotationPageBudgetTest` and `PatternAnnotationEditorContextTest` remain companion regressions for bounded writes and editor context behavior.


Execution evidence is consolidated in [Existing execution receipts and their source limits](#existing-execution-receipts-and-their-source-limits). The fixture/test descriptions above establish intended assertions and implementation wiring, not a fresh execution in this task. The project forbids repeating its previously completed 320 dp / 200% / TalkBack checks without an explicit request for those checks.

### Existing execution receipts and their source limits

Historical execution reports are evidence only for their recorded revision, dirty-source fingerprint, command, variant, test selection, platform and provider state. This update does not inherit their pass status. Ignored reports can be absent from a fresh clone; obsolete local report links have been removed from this reference.

Tracked context includes [LANGUAGE_AUDIT.md](LANGUAGE_AUDIT.md), [config/local-backup.md](config/local-backup.md), [config/annotation-page-limits.md](config/annotation-page-limits.md), [config/insights-session-budget.md](config/insights-session-budget.md) and the repository's release-readiness/remaining-work documents. They mix design, dated receipts and outstanding verification; inspect the dated entry and owning current source before citing a result. Their existence does not prove the November 10 working tree passed an application suite.

The project instructions record repeated prior 320 dp, 200% font, combined and TalkBack checks and explicitly prohibit repeating them without a request for those specific checks. No such checks are rerun here. Existing source coverage remains useful for selecting future authorized tests; test-file names, cached Gradle tasks, skipped Node cases and an empty parser result are not positive test execution.

Remote CI, hosted Sonar, Play Console, real purchases, Ravelry login/download availability, Firestore deployments and telemetry delivery were not queried. Current configuration is described in its owning chapters without assigning a current external pass/fail state.

### Documentation-specific verification

This documentation update uses deterministic pre-read secrets scans and read-only source/configuration inspection. Mechanical checks validate local Markdown targets and internal anchors, generated contents, table shape and fenced blocks; the schema-28 column dictionary against all 19 exported entities/214 fields (name, affinity, nullability and SQL default); declared catalog version values; text-resource name/type parity and inventories; referenced test source names; and Git whitespace/change scope. Scanning a directory before inspection is not a claim that every file in it received a semantic code review.

These checks establish reference consistency, not runtime correctness. No Gradle build, JVM suite, Android instrumentation, emulator, physical device, backend suite, aggregate lint/security wrapper or external upload is executed. No historical report is promoted into a new pass claim. External links and external standards are not revalidated by this source-only task.

The retained deliverable is root PROJECT.md. No application code, tests, dependencies, schema exports, configuration or other documentation is changed. No helper script, generated report, build copy, server or background test process is needed for the update.

## Development and review invariants

### Workflow and source authority

[AGENTS.md](AGENTS.md) and [CODEX.md](CODEX.md) are aligned working instructions and must change together if either is edited. [CLAUDE.md](CLAUDE.md) owns product wording/design guidance. Inspect actual branch, staged/dirty work and owning implementation before a change. Source/config/schema/test implementation outrank a plan or AI finding; verify reported defects before editing.

Apply the minimal-sufficient-work deletion rule after correctness, explicit requirements and required checks. Preserve unrelated user changes. Reviews/plans do not authorize edits. Implementation requests require actual completion; For Android skill-driven work, inspect read-only and explain the evidence/approach before scoped edits; an explicit fix/implementation request authorizes those edits without a second approval of the same work. The specific repository prohibition on repeating previously completed 320 dp, 200% font, combined and TalkBack checks takes precedence over generic UI checklists. Report static, local, device, provider, service and release evidence separately.

Create a task feature branch when the checkout is a protected integration branch, use `codex/` for new branches, and commit/push only on explicit request. Never create/open/merge a PR: the user does that manually in GitHub. Inspect staged content and complete push commit/destination scope; no direct protected-branch updates. Commit messages are concise Finnish; source identifiers/comments stay English. Stop only task-owned temporary processes, never broad executable-name kills.

For this documentation update, only PROJECT.md is an authorized output. In ordinary project work, commit/push require an explicit request; never commit/push protected integration branches or create/merge PRs. User-created PRs are the repository workflow. Use the current AGENTS.md/CODEX.md instructions, including their specific prohibition on repeating previously completed accessibility configurations, rather than treating an older checklist as authorization.

### Ownership checklist for code changes

| Change area | Read first / invariant to preserve |
| --- | --- |
| Main count/Undo/widget | `CounterRepository.applyMainCounterChange`, `applyWidgetCountChange`, history/domain logic; current-row reread and all dependent mutations atomic |
| Main step editor | `MainCounterStepSheet`, `CounterViewModel.setStepSize`, `CounterRepository.updateProjectStepSize`; 1–99, expected project/generation, cancellable ViewModel-owned save, no count/history mutation, optimistic delta feedback is not persistence confirmation |
| Session start/stop/recovery | `ActiveSessionTiming`, repository typed results, `SessionDao`; one singleton, valid anchors, token guards/idempotence, no lifecycle finalization |
| Completion/reactivation | Repository admission/generation and `ProjectCompletionMigration25`; every transition event, preserved history, no quota bypass |
| Project folders | `ProjectFolderRepository`, typed folder state; metadata only, single assignment, NFC/Root uniqueness, global completed preference |
| Extra/secondary/stitches | `ProjectCounterType`/logic, Counter repository; no legacy duplication, repeat sections derived, persisted continued-access flags |
| Notes | `saveProjectNotes` base merge; no network processing or stale overwrite |
| Yarn links/usage | `YarnCardRepository`, `YarnCardLinks`, note/usage repositories; reciprocal transaction, source coalescing, revisions, independent nullable meters, no stash deduction |
| Documents/saved metadata | `ProjectDocumentRepository`, Saved/Counter repositories; stable document keys, exact primary rule, metadata unlink preserves readable docs |
| Incoming PDF share/restoration | `MainActivity`, `IncomingPdfIntent`, `IncomingPdfHost`, `IncomingPdfViewModel`; consumed-intent restoration, request-version ordering, staged-URI checks and mutex-bound expected-URI actions |
| File deletion/photo replacement | `AppFileStorage`, reference coordinator, unused-PDF gate; copy before URI swap, SQL before best-effort deletion, shared files preserved |
| Reader/bookmark/calibration | Per-document state, mapping parser/location resolver; explicit marker writers, stale gesture guards, conservative page choice |
| Annotations/export | Shared coordinate transform/renderer/codec/page budget; master read-only in project, one transform, bounded whole-page validation, source preserved |
| Backup/restore | Archive/budget/provider/identity/journal code; preflight and confirmation checks, one admitted provider operation, fresh identities/files, atomic live replacement |
| Pro/billing | `ProState`, `ProManager`, `TrialManager`, `BillingManager`; repository admission, existing-content semantics, initial readiness, no debug purchase claim |
| UI/navigation | `Screen`, `TopLevelNavigation`, graph-owned state, shared components/tokens and source exceptions; valid IDs, cleared restore stacks, no uppercased project names |
| Ravelry | Current browser/link/downloader; no accidental reconnection of retained API/OAuth, HTTPS hops, per-URL cookies, external purchase boundary |
| Analytics/diagnostics | Usage event/route allowlists, consent, release/debug source sets; no user content, IDs/URLs/billing state, no release Sentry |
| Schema/dependencies/release | Room exports+migrations+callbacks, version catalog/verification, Gradle artifact gates; no destructive fallback, no unverified preview upgrade |

### Verification selection

Use existing focused tests for realistic changed behavior/failure paths; do not mirror implementation or add a new harness when an existing owning check suffices. Real Room invariants need Android database coverage when appropriate; source tests alone cannot prove constraints. Provider fakes cover ownership but not every OEM/document service. UI source and semantics checks do not establish visual correctness. However, the project-specific instructions state that 320 dp width, 200% font scale, their combination and TalkBack have already been tested repeatedly: do not rerun them unless the user explicitly requests those specific checks. A generic audit or “test everything” request does not override that restriction. Choose other necessary focused checks within the authorized task.

Preserve cancellation at coroutine boundaries; ordinary exception recovery must not swallow it. Application-lifetime persistence must still select injected IO. Flow retry comes after domain mapping. Multi-DAO writes use `DatabaseTransactionRunner`; do not add a ViewModel sequence that can leave half a relationship. File cleanup failure after commit is not a rolled-back delete.

When reporting checks, record command, revision/dirty fingerprint, time, executed case count versus cached tasks, exact scope and blocks. Raw reports precede fixes; exception/baseline changes require evidence and rationale. A reviewer should be able to follow UI→ViewModel→domain/repository→DAO/storage→test without relying on this document alone.

### How to choose the source of truth

1. Start with [AGENTS.md](AGENTS.md)/[CODEX.md](CODEX.md), then the current owning route/ViewModel and [CLAUDE.md](CLAUDE.md) for product/visual work.
2. Follow the concrete action into the owning repository and domain rule. Check whether a method returns a typed success/rejection, not merely whether a coroutine launched.
3. Inspect DAO SQL, production trigger callbacks and exported schema for persistence changes. An annotation `@Transaction`, a fake runner or a UI callback alone does not prove the full invariant.
4. Inspect private-file publication/reference locks and cleanup order whenever a URI or deletion changes. Database rollback and post-commit physical cleanup have different success semantics.
5. Select domain/JVM, real Room, UI, provider, device or live-service validation according to the boundary changed. Source assertions are useful for ownership/style rules and insufficient for geometry or real provider behavior.
6. Update this reference and any genuinely related documentation after behavior/configuration changes. Keep current facts together and move dated receipts into the evidence section.

Do not use a plan's class name, an old `functions/` endpoint or an AI review claim as proof of current reachability. A new Room field requires an actual migration/backup impact review; an optional source ID does not authorize writing an orphan relationship. Never split the project's reciprocal yarn links, document/layer attachment, session finalization or lifecycle completion event into independent UI writes.

### Repository source map

| Directory / owner | Main implementation responsibilities |
| --- | --- |
| Root `App`, `MainActivity`, `MainActivityTheme` | Hilt startup/cleanup, theme/locale readiness, activity intents, review/update and navigation host |
| `domain/model` | Typed project/craft/count/source/availability/folder/document/session/yarn/annotation models and validation |
| `domain/calculator` | Counter/repeat/timing, numeric measurement/estimation, reference data, geometry/chart/reading-line and local instruction rules |
| `data/local` | All 19 entities, DAOs, exported migration chain, schema constraints, safe open helper and SQL chunking |
| `data/datastore` | App preferences and locale migration/safe-read-write mechanisms |
| `data/storage` | App-owned URI boundaries, photo/PDF pipelines, renderer/exporter and counter-launch trust tokens |
| `data/backup` | Logical archive, bounded validation/provider IO, identity/file publication and recovery |
| `data/remote/WebPdfDownloader` | Selected browser PDF HTTPS download only |
| `repository/CounterRepository` | Main project/count/history/session/completion and attachment coordination |
| `repository/ProjectCounterRepository`, `ReminderRepository` | Project-owned counter/reminder validation and mutation results |
| `repository/YarnCardRepository`, `ProjectYarnNoteRepository`, `ProjectYarnUsageRepository` | Reciprocal links, private photos, note-to-card conversion and independent usage amounts |
| `repository/ProjectFolderRepository` | Coherent organization snapshot, folder/membership operations |
| `repository/ProjectDocumentRepository`, `SavedPatternRepository` | Canonical readable relations and Library metadata; shared-PDF reference safety |
| Annotation/layer/bookmark repositories | Ownership/context, bounded normalized payload and reading position commands |
| `ui/navigation` | All routes, graph scoping, incoming PDF/share coordination and safe arguments |
| `ui/screens/project`, `counter`, `session`, `counterhistory`, `notes` | Project workspaces, content sheets, timer decisions and persisted histories/editors |
| `ui/screens/library`, `yarncard`, `pattern`, `ravelry` | Library/detail editors, private reader/markup/import, current website browser |
| `ui/screens/gauge`, `increase`, `caston`, `yarn`, references | Local calculators, canonical edit state and resource-backed references |
| `ui/screens/insights` | Bounded aggregation, chart/fabric/completion models and data-dependent rendering |
| `ui/screens/pro`, `settings`, `backup` | Entitlement prompts/purchase display, preferences and replacement backup UI |
| `ui/components`, `ui/theme` | Shared controls/text/formatting/semantics, fabric/glyphs and visual tokens |
| `widget` | Glance rendering, application-shared display state and repository action receiver |
| `billing`, `pro`, `analytics`, `di` | Play connection/trial/features, controlled telemetry and singleton/dispatcher bindings |
| Debug/debugShared/release source sets | Demo/test-policy/diagnostic facades; debug-only controls never become release setters |
| `functions/src/ravelry` | Disconnected backend OAuth/state/token/rate-limit/search/import implementation and Node tests |

## Current status, limitations, and document authority

### Implemented and reachable

Current Android entry flows expose full onboarding, starter-project setup, contextual tips, the bundled guide, projects/folders/lifecycle, overview and main counter/history, secondary/stitch/extra/repeat/cadence counters, notes/reminders, explicit sessions and reviewed recovery, Library PDF/web/retained Ravelry metadata, multiple project documents, private import/share/photo-to-PDF, layered markup/chart tools/export, guides/calibration/bookmarks, progress/yarn photos, yarn inventory/project notes/usage, calculators/references, Insights and completion events, Glance widgets, preferences, trial/one-time Pro and manual replacement backup.

Release Crashlytics and consented release PostHog are configured integrations. Debug Sentry, sample data and isolated test control are variant-specific. Ravelry currently means its website in WebView and selected website PDFs entering Save Pattern.

“Implemented” here means present source and reachable code, subject to validation/entitlement/state rules. It does not imply fresh runtime, Play approval, live vendor delivery or a deployed version distributed to users.

### Retained, partial, historical, and planned

| Classification | Concrete current boundary |
| --- | --- |
| Retained backend | Firebase Ravelry OAuth/search/import code, rules/index/deployment configuration and tests; no Android consumer |
| Removed Android integration | Native Ravelry API/OAuth/search/detail/import/client, Auth/Functions dependencies and completion manifest callback |
| Compatibility data | Legacy project PDF/reader columns, old file/URI resolution and saved metadata accessors; current authority is documented above |
| Shaping scope | Setup fields and cadence counter/display are implemented; no current automatic stitch-total forecast/next-shaping-row calculator |
| Shared reference limitation | Knitting/crochet abbreviation context currently returns the same dataset |
| Tool link limitation | Yarn Estimator's Choose from My Yarn opens My Yarn; no automatic return/prefill of a selected card is established by the current route callback |
| Disabled tool | Dependency Analysis plugin remains catalogued but commented out for the recorded AGP compatibility boundary |
| Profile versus smoke scope | Baseline Profile generator collects startup; a separate external driver has seven smoke methods, of which CI selects two. Neither source declaration establishes measured performance |
| Recorded future work | Continuous cloud sync specification; legacy backend global-limit compatibility cleanup only after deployed writer-drain verification |
| Intentionally absent | Voice/microphone/TTS, model-backed parser/cloud notes/content processing, release Sentry/replay, Firebase AI/ML Kit/Gemini, native storage-provider OAuth and automatic content sync |

The last two categories are not permission to implement them. No new roadmap, migration or unrequested production fix is introduced by this document.

### Supported limits and unresolved verification

| Area | Established fact / remaining verification |
| --- | --- |
| Current-tree execution | Only source/document consistency and deterministic pre-read secrets checks ran; no fresh app build/test/security suite, and historical receipts do not certify this dirty working tree |
| Task execution constraints | The remaining-work handoff records no local emulator use because of RAM limits and defers Play work. Its physical-phone history is dated, not proof of a connected device or authorization for a documentation task |
| UI/accessibility | Measured adaptive layouts, semantics and focused tests exist; none rerun here. Prior 320 dp, 200%, combined and TalkBack configurations require an explicit request before repetition |
| Source shaping description | Stored fields and actual cycle display replace the obsolete forecast claim; product intent for further shaping behavior is not inferred |
| Large session history | 100,000-session writer/archive admission and bounded queries; maximum device latency/memory and writer blocking not measured here |
| File/provider resilience | Bounded copy/ownership/journals exist; hostile provider cancellation can retain admission, partial exported output can remain |
| Backup | Format/data 1, schemas 25–28 accepted; no merge, encryption or authenticity; other budgets can reject an otherwise permitted native history |
| Cleanup | SQL commits before best-effort file deletion; orphans possible, with directory-specific pruning rather than a universal sweep |
| Browser | Arbitrary HTTPS redirect/subframe/download destinations can pass; retained website session, no native Disconnect/DNS firewall |
| Ravelry permission | Recorded requirement to obtain written site permission before Play release; acquisition not established |
| Billing/trial | Local timing/Play state handling implemented; live SKU/price/pending/ack/refund/revocation and store setup not verified |
| Telemetry | Release/debug/consent code inspected; vendor payloads, delivery, retention and symbolication not verified live |
| Backend | Deployment, secrets, Firestore indexes/rules/TTL and public service exposure unknown; source presence is not deployment proof |
| Release | Signing/config/admission paths exist; no fresh signed artifact, release merged-manifest proof, mapping upload, Play review or publication |
| Privacy/contact | Technical privacy draft reflects current architecture; responsible publisher contact remains missing |
| Localization | 1,631 translatable names per locale across 12 locale options, including 325 onboarding resources; source inventory/contract checks are distinct from native-speaker and rendered-device coverage |
| CI/CodeQL/Sonar | Build/analysis, API 36 optimized smoke and Functions jobs configured; hosted Sonar quality gate enabled in YAML; actual current outcomes and branch protection unqueried; no CodeQL workflow |
| Security checks | 611 registered exceptions with the expiry breakdown above, 12 OWASP XML suppression nodes and 22 Detekt baseline IDs are inventories; only deterministic pre-read secrets checks ran in this task |

An unexecuted check is uncertainty, not automatically an implementation defect. The document records actual limitations and source discrepancies without treating every stronger possible design as required work.

### Related documents and authority

| Source | Use and caveat |
| --- | --- |
| [AGENTS.md](AGENTS.md), [CODEX.md](CODEX.md) | Aligned working rules, ownership/security/UI conventions; unchanged here |
| [CLAUDE.md](CLAUDE.md) | Product wording and visual direction; current implementation still decides exact behavior |
| [README.md](README.md) | Short current overview; its Gradle 9.8.1 / schema 28 snapshot agrees with this working tree |
| [ONLINE_OFFLINE.md](ONLINE_OFFLINE.md) | Current network/offline distinctions, not the old native Firebase flow |
| [PRIVACY-POLICY.md](PRIVACY-POLICY.md) | Updated technical draft with current browser/telemetry/backup model; publisher contact still required |
| [config/security-decisions.md](config/security-decisions.md) | Recorded current browser, Crashlytics, analytics and removed-credential decisions |
| [config/posthog.md](config/posthog.md) | Current consent/event policy and PostHog Android 3.75.0 configuration; its dated real-SDK receipts remain evidence for their own source/transport scope |
| [LANGUAGE_AUDIT.md](LANGUAGE_AUDIT.md), [shared corrections](config/language-audit/shared-fixes.md), locale reports under [config/language-audit](config/language-audit) | Dated content acceptances/technical receipts; earlier key counts and hashes do not establish current resource coverage |
| [config/local-backup.md](config/local-backup.md) | Detailed format/recovery design plus explicitly dated emulator/SAF evidence |
| [config/insights-session-budget.md](config/insights-session-budget.md), [config/annotation-page-limits.md](config/annotation-page-limits.md) | Resource-budget rationale and implementation limits |
| [config/future-sync-spec.md](config/future-sync-spec.md) | Future decision/specification boundary, not a shipped synchronization service |
| [config/ravelry-backend-progress.md](config/ravelry-backend-progress.md), [backend plan](<Ravelry Firebase Backend And Saved Patterns Plan.md>) | Retained backend history; not Android reachability or cloud-state verification |
| [KNITTOOLS_REMAINING_WORK_HANDOFF.md](KNITTOOLS_REMAINING_WORK_HANDOFF.md) | Sequential task coordination, dated evidence and current operational restrictions; not a blanket authorization or whole-release PASS |
| Root product/UI plans and tracked `docs/superpowers` material | Historical intent; previous typography/layout/voice proposals can be superseded or deliberately absent |
| [.deepsec/README.md](.deepsec/README.md) | DeepSec usage/history; its old Phase-9 Auth/Functions allowance wording is superseded by current release-surface code |
| Ignored `reports/` artifacts | Read as dated evidence tied to command/source fingerprints; availability in another checkout is not guaranteed |

### Reference maintenance and verification boundary

The November 10 refresh preserves the existing product-first reference and full schema dictionary, adds task-oriented reading paths and test-design guidance, and reconciles the dirty working-tree snapshot, incoming-PDF restoration/stale-action handling, retained-backend refresh failure races, CI concurrency, local test helpers and related-document status. The 19-table/214-field dictionary, 12-locale resource inventory, declared dependency values, source links and named test sources were checked for consistency. Source HEAD alone does not identify the uncommitted implementation described here; later edits require rechecking the affected claims.

When maintaining this file, update the affected behavioral contract, its source/test pointers and any changed inventories together. Rebuild contents from headings. Keep current facts separate from dated execution evidence and planned features. A source assertion, old hash, test declaration or deployment plan must not become a claim that a new checkout or live service was tested.

The current database schema is **28**, and canonical readable project PDF state belongs to **project_documents**. Android Ravelry uses the website/download path; the retained API backend is not called. Crashlytics is release crash reporting, PostHog is opt-in release usage analytics with a delivery/queue consent boundary, and Sentry is debug-only. Manual backup is a local-data archive/replacement through SAF. Cloud sync, voice and model-backed content processing remain outside the implemented product.
