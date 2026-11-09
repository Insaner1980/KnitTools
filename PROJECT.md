# KnitTools Product and Engineering Reference

## Scope and verified source snapshot

This is the product and technical reference for the **inspected working tree on October 20, 2026 (Europe/Helsinki)**, including its uncommitted implementation. It is intended for a developer or coding agent with no prior chat history. Current source and configuration are authoritative; plans, previous documentation and historical test receipts are secondary evidence.

This update retains the October 18 reference and the pre-existing October 19 Brazilian Portuguese additions, reconciles their Git/source history, and refreshes the affected sections and inventories. It is a documentation/source verification, not a new build or runtime test run.

Schema and backup-table facts were reconciled with schema 28 on November 9, 2026. Dated validation records below remain historical evidence.

All repository links are relative to the root. Package shorthand such as `repository/CounterRepository.kt` means a path below `app/src/main/java/com/finnvek/knittools`. “Implemented” means present and reachable subject to state, validation and entitlement; it does not mean released, live-service verified or freshly runtime-tested.

| Snapshot item | Inspected value |
| --- | --- |
| Repository | `C:\Dev\KnitTools` |
| Branch | `codex/audit-korjaukset-20261018` |
| HEAD | `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f` |
| Verification date | October 20, 2026, Europe/Helsinki |
| Starting working tree | 99 modified tracked paths and 132 untracked files; 231 entries with `git status --porcelain=v1 -uall` |
| Starting index | No staged changes |
| Documentation output | Root `PROJECT.md` only; its pre-existing Brazilian Portuguese updates were retained and reconciled |
| Application ID / namespace | `com.finnvek.knittools` |
| App version | `1.0.0`, version code `1` |
| Android SDK | Minimum 29; target and compile 37 |
| Room schema | 28 |
| Current database shape | 19 entities/tables, 214 columns; 28 exported schema versions |
| Supported app languages | 12 locale options; System is a selection mode |
| Resource inventory | 1,308 translatable strings and 31 plurals per locale; 1,339 names per locale |
| Modules | `:app` and `:baselineprofile` |
| Production Android inventory | 380 Kotlin files under app/src/main |
| Test source inventory | 324 JVM Kotlin files; 141 Android-test Kotlin files, plus Java test helpers |
| Functions inventory | 33 TypeScript files including 14 test files; retained backend with no Android consumer |

Source file counts include helpers and fixtures and must not be read as executed test counts. A SHA-256 preservation baseline was captured for all 1,252 tracked and nonignored untracked files before editing, including `PROJECT.md`; the other 1,251 files are the preservation comparison set. Ignored build/report artifacts are dated evidence, not authoritative production sources. Secret-bearing local configuration was not copied into this reference.

### Uncommitted implementation covered by this reference

The checkout is not a clean copy of HEAD. `git log -- PROJECT.md` identifies `e4416c7c` (October 18) as the latest committed documentation update, and there are no later commits at the inspected HEAD. Relative to the old document's `605a802f` baseline, that one commit includes the main-step editor, text-annotation and project-yarn-note editing, mutation/file/backup/trial/billing fixes, expanded tests and Sonar test-binary wiring. Those are now committed implementation, not pending working-tree additions. The pre-existing documentation-only diff adds the October 19 Brazilian Portuguese description.

The remaining uncommitted source-visible differences from HEAD are:

| Working-tree area | Current behavior included |
| --- | --- |
| Language integration and content | Regional `pt-BR` selection/resources alongside `pt`; independent locale corrections, shared neutral counter wording, month plurals, and removal of unused reference/resource entries |
| Sessions and Insights | Neutral counter-progress display; historical row/pace metrics unavailable because sessions have no unit snapshot; active dates and presentation start bounded by device today |
| Calculators | M1 bar-capacity validation and flat-edge handling; cast-on repeat selection from the exact body count; whole-skein mass keeps lowercase `g` |
| Reference material and yarn | Exact numbered Japanese diameters, revised body-size values/ranges/categories, 24 chart glyphs with wider cables, corrected care glyphs/text, removed ply hints |
| Widget | Small widget's vertical padding is zero, including the nonstacked normal-font layout |
| Tests and audit records | New locale/unit/date/symbol tests; resource/wording and runtime-fixture updates; untracked `LANGUAGE_AUDIT.md` and `config/language-audit/` evidence |
| Validation configuration | `BadgePill.uppercase` added to the stability baseline; 55 scoped language-audit secret-scanner exceptions added |
| Instructions | AGENTS/CODEX/CLAUDE explain why editable current labels cannot establish historical session row units |

These are inspected implementation facts, not changes made by this documentation task. The source locations and boundary conditions appear in the owning chapters. The current changes add no Room migration, dependency upgrade, Android module, navigation destination, backend call or new Pro gate.

### Evidence and investigation boundaries

The original reconstruction followed UI actions through their owning layers. The October 20 update read the complete reference and repository instructions, inspected the commit since its recorded baseline and all changed-file areas, and traced the current affected calculators, reference data/renderers, session/Insights presentation, locale resources, tests and configuration. Unchanged architecture and detailed schema descriptions are retained; current inventories, links and document contracts are checked separately from historical executions.

No application source, tests, dependencies, manifests, assets, backend configuration or other documentation were changed. No build, scanner suite, device/emulator operation, external upload, commit, push, pull request, deployment or release was performed. Documentation validation is described under [Testing and evidence](#testing-and-evidence). Historical execution evidence has its own provenance and limitations.

The user-facing feature chapters come first. Navigation and design explain how those workflows are presented; architecture and the persistence dictionary explain ownership; security, tooling, test evidence and development rules explain how to change the project safely.

## Contents

- [Scope and verified source snapshot](#scope-and-verified-source-snapshot)
  - [Uncommitted implementation covered by this reference](#uncommitted-implementation-covered-by-this-reference)
  - [Evidence and investigation boundaries](#evidence-and-investigation-boundaries)
- [Product and feature behavior](#product-and-feature-behavior)
  - [Product model and everyday workflows](#product-model-and-everyday-workflows)
  - [Feature availability at a glance](#feature-availability-at-a-glance)
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
  - [Reconstruction audit and maintenance boundary](#reconstruction-audit-and-maintenance-boundary)

## Product and feature behavior

### Product model and everyday workflows

KnitTools is an Android companion for people making knitting and crochet projects. Its core job is to keep the current place in a piece of work: the row or round, smaller repeat counts, the relevant instruction page, materials and notes, and the time spent making it. It combines a project workspace with a reusable pattern and yarn library and offline calculation/reference tools.

Local work requires no KnitTools account. A project has an explicit craft type, KNITTING or CROCHET. Knitting defaults to Rows and crochet to Rounds; users can select Repeats or a custom main-counter label. Changing the wording does not change the stored count or turn the counter into a different calculation engine.

The application is organized around five persistent tabs:

| Tab | User purpose | Main workflow |
| --- | --- | --- |
| Projects | Organize and continue work | Create a project, choose craft/label/folder, open its overview, continue into the counter, and complete or reopen it |
| Library | Reuse materials and instructions | Browse saved patterns, My Yarn and all progress photos; consult needle, size, abbreviation and chart-symbol references |
| Tools | Calculate and obtain instructions | Calculate measurements/gauge, increases/decreases, cast-on counts or skein requirements; open Ravelry's website |
| Insights | Understand recorded activity | Select a time range/project; inspect time, neutral net counter progress, active days, completion events and available charts |
| Settings | Control the app and protect local work | Choose language/theme/preferences, manage trial/purchase access and create or restore manual backups |

A complete making workflow starts with a project overview. The overview groups progress, instructions, materials, notes, photos and reminders. The Continue action opens the count-focused workspace. A user can explicitly start a work session, count while reading an attached PDF, mark the current instruction row, track a repeated section, and later stop and save the session. Moving to another screen or backgrounding the app is not a session Stop action.

Before any project exists, Projects shows a nonblocking start state inside its normal list, with bottom navigation available. `WelcomeExampleCard` lets the user try counting from 23, Undo, growing fabric and a moving example reading line without creating or changing a project. Count and taps survive recreation through local saveable state. The inline New Project action replaces the floating pill only in this full introduction. Dismissal or the Tools-only action persists `welcomeDismissed`; the compact state uses the last-created craft and the normal floating pill. An empty folder retains its existing empty state whenever any active or completed project exists elsewhere. Real project and onboarding state must load before choosing this view.

The bundled How to Use KnitTools guide has five topics and 26 local items, with exact English copy in strings_onboarding.xml. GuideContent owns topic keys, resource references, optional feature metadata and nine existing-screen actions. GuideScreen reuses ToolScreenScaffold and flat overview rows/hairline sections. Settings Info and the project footer open the same guide under their own graph, preserving bottom navigation. Project-dependent actions resolve current repository data and fall back to Projects when absent; navigation ignores results from departed entries. Show tips again reuses TipSession reset and its success/error handling. Guide content is available immediately; Pro markers wait for initialStateReady.

The active starter project's overview includes a flat Project setup section directly below Continue. Its four free steps use existing facts: a positive count or target, a project document or linked web pattern, project yarn notes or linked yarn cards, and a completed work session. Unfinished steps precede completed, noninteractive rows. The section reuses overview rows/headers. Hide and completion persist through OnboardingStateRepository; CounterViewModel retains only a current-visit celebration marker so All set stays visible during that visit, survives recreation, and disappears on the next entry even when navigation reuses an earlier back stack entry. Work time navigates to the existing Counter and requests immediate manual WORK_SESSION guidance; the request waits for a loaded, resumed, unblocked matching project and valid placement, consumes no automatic-tip budget, and is cancelled on leaving. Counter and overview tip hosts use their own navigation entry IDs so outgoing content cannot dismiss the destination's guidance. No Room table or entitlement changes are involved.

Patterns are reusable Library metadata and optional private PDFs; a project's document relation independently owns its reader state. Yarn cards are reusable stash metadata; project yarn notes and usage are project-owned. Completion retains the project's data and adds an event; reopening does not erase earlier completions. These distinctions prevent shared Library content from silently becoming project-specific or stock quantities from being silently consumed by estimates.

Onboarding Phase 8 celebrates a new single-project completion with `ProjectCompletedSheet`. Its immutable event snapshot contains the completed project and saved session minutes; ViewModels retain it before clearing Counter selection and add the newest loaded photo. One Projects-list host displays the photo or a 160 dp craft-aware fabric tile, craft-specific title, localized duration/count and start/finish dates. Close and Start Your Next Project consume the exact event ID; Next invokes ordinary project creation with the current folder and repository quota. The original single/bulk intent survives session confirmation, so a shrinking bulk queue cannot produce a single-project celebration. Idempotent completion does not show a sheet; reopening and completing creates a fresh event.

Implemented pattern capabilities include **local pattern PDF import, project attachment, reading-line calibration, layered annotations, and annotated export**. Reading, editing and export are bounded local operations. A website link alone does not create an offline PDF. Ravelry provides a website session and user-selected PDF downloads, not a native API search/account feature.

The visual identity is a warm craft workspace: cream and olive surfaces, burnt-orange actions, fabric swatches, cross-stitch glyphs and Barlow Semi Condensed typography. Large counter controls and a clear first viewport prioritize the repeated action of counting. The app does not implement cloud synchronization, a social project feed, AI interpretation of patterns, OCR, or a voice assistant. Future specifications are separated from the reachable product below.

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

## Projects and organization

### Projects, folders, sorting, and selection

Onboarding Phase 2 extends ordinary project creation with an optional starting count (empty means 0; valid range 0–99,999) and target (empty means no target; otherwise 1–99,999). `ProjectStartValues` is the shared parser, including the existing target dialog. `CounterRepository.createProject` keeps quota admission first in the transaction and inserts these as initial project values, with no history row, session checkpoint, linked-counter delta, or reading-line movement. Undo immediately after creation does nothing. Imported-PDF creation keeps its existing behavior.

The Create variant of `ProjectDetailsSheet` uses shared labeled fields, a craft-aware next-count helper, and a noninteractive `ContinueProjectCard` preview only before the first project. Edit mode keeps the existing name/craft/label scope. Unfiltered `observeHasAnyProject()` drives this distinction even when completed projects or other folders are hidden. Successful ordinary creation remembers the craft and marks the starter project only when the database was empty before insertion. Pending creation preserves count/target through quota retries; Java serial UID remains 1 while payloadVersion 2 distinguishes new drafts, so legacy payloads can be dropped safely before resubmission. This compatibility correction was approved because Lifecycle 2.11.0 restores serialized Bundle values before constructing SavedStateHandle.

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

History rows store the action, before/after values and timestamp. Undo loads the latest history row and requires the matching project, nonnegative unequal values, a current count equal to the recorded new value, and a supported reversible action with consistent direction. It writes the previous value and deletes that history row atomically with dependent counter/reader/session work. Repeated Undo can walk earlier valid rows; it is not a transient UI-only snapshot or arbitrary historical restore.

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

The stored main-counter step is exposed through presets and custom input, committed in `e4416c7c`. The repository's setter changes only the selected project's step and update timestamp; it is not a count mutation and therefore creates no count-history row, session row delta or reminder hit. A subsequent increment/decrement uses the persisted step through the canonical counter change.

The ViewModel serializes saves with a mutex and owns the operation lifetime. The sheet can close without transferring persistence ownership to a disappearing composition. Project identity is rechecked before applying presentation state; late responses must not change another selected project's step. This admission/control boundary is separate from ordinary extra-counter step fields and does not add a Pro feature.

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

**Linked counters.** The add draft can mark eligible extra counters linked to main. The canonical main transaction adds the actual signed main-count delta to their stored count, clamped to `0..Int.MAX_VALUE`. This path does not call the direct repeating increment/modulo routine; linked and manually incremented repeating counters must not be documented as identical storage transitions. Undo applies the reverse main delta. Repeat-section counters are excluded because they already derive position.

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

The WebView enables JavaScript and DOM storage because the website requires them. File and content access are disabled. Cookie acceptance is enabled; debugging follows `BuildConfig.DEBUG`. The inspected implementation adds no JavaScript-to-native bridge or certificate-error bypass. Disposal flushes cookies and destroys the WebView; it does not clear the website account session. Browser page/history state is not persisted with WebView save/restore APIs.

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

[config/security-decisions.md](config/security-decisions.md) records the accepted browser approach and the requirement to obtain written Ravelry permission before Google Play release. This reconstruction did not establish that permission has been obtained. It also did not verify live website login, payment behavior, cookies, CAPTCHA or downloadable access. Site behavior and provider state are external to the checked-in source.

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

Phase 9 keeps the Pro page editorial, with Free Forever and Pro Adds sections, a personal line only when real facts are available, and the existing Play price, one-time purchase and restore controls. Contextual prompts lead with the feature benefit and explain that the user-started 14-day trial requires no payment method and never becomes a charge. These presentation changes do not rebalance feature entitlements.

`TrialJourneyStateRepository.state` is nullable until all four Room observations, onboarding state and `ProManager.initialStateReady` have supplied their real values. It supplies the unfiltered active count and latest active project, plus projects, named counters, yarn cards and progress photos whose `createdAt` belongs to `[trialStartTimestamp, trialStartTimestamp + 14 days)`. The end is exclusive, so later free additions do not appear as trial activity after expiry. The calculation uses a nonnegative timestamp difference to avoid overflow. `ProjectCounterRepository.getAllCounters()` maps the new all-project DAO observation and uses the existing retry boundary. Summary wording uses localized plurals and the platform locale-aware list formatter; unknown or empty summaries remain absent.

`TrialJourneyContent` is shared by the Pro page and `TrialJourneySheet`. Its `OverviewLinkRow` actions and the text progress line (`trial_journey_progress`) use actual completed facts; the sheet replaces the section label with `trial_journey_intro`. The five actions are widget pinning, adding a `REPEATING` counter, adding a project, opening this week's Insights chart, and camera-to-PDF. Project-dependent rows are omitted without a latest active project, leaving project creation and the chart. `TrialJourneyNavigation` routes directly into these existing actions; project creation retains its normal admission/draft flow, and Counter actions wait for the intended selected project and their existing readiness/blocking gates. Opening a destination is not completion evidence.

`CounterDestination` owns selection of the requested project before rendering the shared Counter content or handing off a journey action. Its request identity and current-entry guards prevent a stale load from consuming a newer request. Journey requests remain pending until the matching action is handled; their saved-state payload contains only primitive fields. Missing project targets return through the existing Projects fallback.

`CounterWidgetPinHost.hasInstalledWidgets` queries the launcher's actual widget IDs and is refreshed when the UI resumes. A successful pin request is not proof of installation. Widgets have no creation timestamp, so they can complete a checklist row but never enter the timestamped trial summary. `trial_chart_seen_for_start` and `trial_photo_pdf_created_for_start` in the existing onboarding store bind the other two observations to the trial start. The chart marker requires the actual visible weekly chart and resumed lifecycle; photo-PDF completion requires a successful camera/gallery PDF attachment. Ordinary PDF import cannot satisfy it, and best-effort guidance persistence cannot change an already successful import result.

Both Pro entry ViewModels request the narrow singleton `TrialJourneyCoordinator` only for `TrialStartResult.Started`, before returning the contextual result/resuming the pending original action. Already-active or failed starts do not queue another introduction. MainActivity saves/restores the coordinator's pending Boolean in its existing instance-state bundle; no extra DataStore or third journey marker is created. Presentation waits for real facts, a resumed route, and clear modal surfaces.

`TrialManager.claimTrialThreeDayNotice()` evaluates current persisted timing and claims `trial_three_day_notice_shown` in one serialized edit, only for an active untampered trial with exactly three days remaining. It returns true only after the write succeeds. The host waits for initial facts and clear startup/modal gates before claiming, and saves a pending notice across recreation. The trial-ended notice uses the same personal summary and gives Continue Free and See Pro equal visual weight. Existing explicit-launch suppression and one-time end-notice ownership remain in place.

### Trial persistence and clock handling

[TrialManager.kt](app/src/main/java/com/finnvek/knittools/pro/TrialManager.kt) implements a **14-day, user-started** local trial. It is not automatically activated by installation or backed by a server account. Preferences DataStore `trial_state` stores:

`trial_start_timestamp`, `last_known_timestamp`, `clock_tampered`, `trial_end_notice_shown`, `trial_three_day_notice_shown`, `trial_elapsed_duration_millis`, `trial_anchor_wall_clock_millis`, `trial_anchor_elapsed_realtime_millis`, and `trial_anchor_boot_count`.

A valid time snapshot has positive wall time, nonnegative elapsed realtime, and nonnegative available boot count. Clean unstarted legacy state can start only from a valid snapshot. Corruption writes tampered=true; legacy active timing or malformed partial anchors fails closed rather than trusting a reset trial.

Same-boot timing advances with nonnegative elapsed-realtime delta. A later boot uses nonnegative wall delta plus one hour per boot-count increment, accepted only when consistent with elapsed realtime. Decreasing/missing boot identity, reversed/inconsistent anchors, overflow, and wall rollback greater than one hour against last-known/start tamper-lock entitlement. `daysRemaining` uses ceiling days; active requires remaining duration and no tamper flag. Refresh delay is the smaller of 15 minutes and next elapsed-day boundary, then 15 minutes after expiry.

There is no general trial-key cleanup routine; evaluations overwrite persisted state, with absent boot anchor removed when appropriate. Manual backup excludes these keys. App reset/reinstallation/Android storage policy and live Play ownership are separate from this local implementation; do not claim server-enforced perpetual trial identity.

Clock snapshots are taken inside the serialized DataStore edit used for start/refresh. This ordering prevents overlapping operations from persisting an older snapshot after a newer one and incorrectly marking an ordinary concurrent request as clock tampering. The local trial still is not a server-backed identity or a cryptographic entitlement.

### Google Play purchase and restore

[BillingManager.kt](app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt) queries the **one-time INAPP product `knittools_pro`**. There is no subscription tier or hard-coded price. UI uses Play `formattedPrice`; offer selection excludes rental/preorder offers, prefers current valid non-limited offers with deterministic ID/price selection. Pending one-time purchases are enabled.

The Billing client connects asynchronously, enables automatic reconnection, and caps explicit connection attempts at three with two-second spacing. Initial purchase readiness has a two-second wait boundary. Restore queries owned INAPP purchases and returns Restored, Not Found, or Failed. Matching PURCHASED records unlock local entitlement immediately, before acknowledgement; PENDING emits pending state without unlocking.

Acknowledgement requires a matching purchased, unacknowledged record with a nonblank token. Retryable network/disconnected/unavailable/error/item-not-owned responses requery purchases with up to three retries five seconds apart; nonretryable failures stop. Ack failure does not immediately remove in-memory entitlement. ITEM_ALREADY_OWNED triggers requery; empty/failing restore emits an explicit already-owned restore failure.

A later successful owned-purchase query recomputes entitlement from current PURCHASED records, so absence can remove purchased access. There is no backend purchase-validation/refund-notification service. Product availability, price, Play Console configuration, purchase/acknowledgement, pending completion and revocation behavior were not tested live during this task. Play Review/Update integrations are separate from Pro purchase enforcement.

Main Activity waits for initial launch/Pro readiness before claiming a one-time passive trial-ended notice. Explicit Pro/widget/share/counter launches suppress that passive notice so it does not compete with the requested flow. Continue Free dismisses it; See Pro opens the upgrade flow. Notice claiming is persisted through `TrialManager`, not inferred from every recomposition.

`BillingManager` handles `ITEM_ALREADY_OWNED` from both the purchase-update callback and the immediate `launchBillingFlow` result by querying owned purchases. It does not grant Pro from that response code alone. A successful query containing no qualifying purchased item, or a failed query, produces the already-owned restoration failure message. Pending items do not unlock access.

Purchase queries carry a generation and client identity so an older response cannot replace a newer purchase notification. A failed query preserves previously established purchased state; a successful empty query can remove it. Acknowledgement has its own in-flight/retry tracking. These are local source guarantees; actual Play SKU availability, price, account restoration, pending payment, refunds and revocation were not exercised in this reconstruction.

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

The manifest identifies KnitTools format, creation time, app version, schema, and bounded file paths/sizes/hashes. Structural allowlists, duplicate/path checks, ZIP/ZIP64 local/central inventory agreement, trailing-data/decompression/depth checks, declared budgets, hashes, row validation, and a shadow Room database reject invalid archives before confirmation/replacement. App version is informational; other format/data/schema versions need an adapter, currently absent. SHA-256 detects inconsistent bytes but supplies no cryptographic authenticity. The archive is **unencrypted and unauthenticated**; project notes/photos/PDFs are readable by anyone who can access the file.

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
| `ravelry_browser?url={url}` | Global; optional encoded URL | Validated start page or default Ravelry search page |
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

`MainActivity` is an exported single-top activity with launcher, PDF Open/Share, and `ACTION_SEND text/plain` filters. `onNewIntent` routes through the same incoming handlers; the consumed PDF intent is reset to MAIN and stream/type/data cleared to prevent replay. There is no registered native Ravelry OAuth completion URI.

PDF acceptance checks explicit/resolved MIME for VIEW, or PDF SEND with an actual stream. Text sharing is bounded and parsed before a browser/editor decision. App-local counter launches use `CounterLaunchRequest` / `CounterLaunchTokenStore`: a one-time stored token admits an app-created launch request, and the requested project must resolve through application state. The token store does not encode a project-ID binding. Raw externally supplied project-ID/widget extras are not treated as sufficient authorization. Tokens are navigation aids, not public API credentials.

Sources: [MainActivity.kt](app/src/main/java/com/finnvek/knittools/MainActivity.kt), [TopLevelNavigation.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/TopLevelNavigation.kt), [CounterLaunchRequest.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/CounterLaunchRequest.kt), [CounterLaunchTokenStore.kt](app/src/main/java/com/finnvek/knittools/data/storage/CounterLaunchTokenStore.kt), [AndroidManifest.xml](app/src/main/AndroidManifest.xml).

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

The deterministic project/yarn palette in `YarnColorPalette` has eight slots. Dark: `#C45100`, `#8BA44A`, `#C9A435`, `#B8908F`, `#9A6B4A`, `#5A8A7A`, `#9A82AA`, `#A85A3A`. Light: `#C45100`, `#70843C`, `#957927`, `#A3706E`, `#9A6B4A`, `#578576`, `#8E73A0`, `#A85A3A`. Entity-derived stable selection makes color consistent across project/yarn/fabric views. Static color tests do not prove a complete rendered screen passes contrast requirements.

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
| Counter | 24 dp horizontal, hero minimum 360 dp, control width maximum 360 |
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
- Counter's viewport-sized hero groups the count/target, image-backed controls, secondary/stitch state and its own spacing. Active-session status and the project-content area follow the hero in the LazyColumn; a 48 dp reveal gap separates content. `ProjectContentHost(overview = true)` uses `ProjectOverviewScreen`/sections for management and the shared Continue progress card. Its yarn grid reduces columns below 340 dp; workspace session actions stack below 360 dp.
- Library is a destination hub with focused lists/details, not a Projects-folder view. My Yarn places status under the name and usage actions inside the card. Tools uses calculator input/result components and text actions such as Choose from My Yarn. Gauge choices and Size Chart categories use `SegmentedToggle`.
- Insights uses range/project filters, hero, compact stats/trend, chart, completion data, and activity fabric according to available data. Settings is divided into General, Pro, Data, and Info; backup belongs to Data. Notes has only the Notes top-bar title. PDF labels are cleaned by `PatternDisplayNames`.

`UiConsistencySourceTest` enforces exact component exception lists. Source-contract enforcement prevents certain structural regressions; it does not verify screenshot geometry or interaction on a device.

### Responsive layout, semantics, focus, and feedback

Project-card compact mode applies at width <320 dp or font scale >1.3. `SegmentedToggle` uses radio-button roles/selectable grouping and minimum 48 dp targets; more than three options becomes a two-column grid. At font scale ≥1.5, its shape/expanded multiline labels adapt, but a two/three-option selector does not become a grid just because the font is larger. Gallery image import uses a SAF fallback when width <360 dp **and** font scale ≥1.5.

Bottom navigation has five icon/uppercase-localized labels, theme container, primary selected color, and inactive semantic colors. Its Material indicator is the container color, despite an active-background token existing. Label measurement considers translated labels and reduces size in 0.5 sp steps down to its configured minimum, then uses one-line ellipsis. This is a source mechanism, not a large-font legibility guarantee.

Image-backed controls expose Button roles and localized descriptions; decorative image/glyph descriptions are null. `LabeledCounterImageButton` keeps the clickable action before `clearAndSetSemantics` so the combined label/Pro badge does not remove activation. Form fields use keyboard/IME and focus handling through their shared components; route changes, sheet closure, and picker outcomes are state-owned. Custom chart selection exposes next/previous actions. Gesture-based PDF editing, viewport focus after bookmark jumps, and system Back require runtime testing beyond semantics inspection.

Counter image press feedback animates in 90 ms with scale/translation (up to 1.8% shrink and 1.5 dp shift), and disabled alpha 0.62. Shared result transitions use 6 dp displacement and 200/150 ms enter/exit constants. Startup considers system animation availability. A universal custom reduced-motion/focus-ring implementation has not been established by this inspection.

Root `PreferenceAwareHapticFeedback` gates Compose/default long-press haptics using the DataStore preference; explicit counter vibration is also preference-gated. Keep-screen-awake is opt-in. The activity currently requests portrait orientation. Splash styling is fixed by Android theme resources while app preference/theme initialization resolves; it is not evidence of an independently themed night splash.

Source-level adaptation and test availability are recorded here without treating them as fresh visual proof. Existing runtime evidence is consolidated under Testing and evidence.

Current layouts use a wrapping `FlowRow` for Continue-card count/target content, a weighted label group that keeps the Settings unit switch visible, and measured chart-axis labels that omit colliding ticks while reserving space for Today. Weekly chart semantics now have their own weekly resource rather than falling through to the monthly description. These are source-observed adaptations; no fresh rendering claim is made.

Shared `FormSheet` uses a current dismissal flag in `confirmValueChange` and blocks Back while cancellation is disabled. A pending save can therefore retain its editor and draft across attempted hide gestures. PDF page bitmap state is keyed by renderer and page, so a page transition does not publish the preceding page's retained bitmap as the new page. Bookmark viewport focus is consumed only when its target page is rendered.

**Project-specific validation policy:** the previously completed 320 dp, 200% font, combined and TalkBack checks must not be repeated without the user's explicit request for those checks. This documentation pass inspected source and existing evidence only.

### Screen composition and interaction reference

| Screen family | Implemented hierarchy and controls | State / adaptation details |
| --- | --- | --- |
| Projects | Folder title selector, Continue hero, active/completed sections, tonal `ProjectCard` rows, bottom New Project image pill | SQL order retained; compact cards below 320 dp or above 1.3 font scale; selection uses the same cards |
| Project overview | Transparent top bar, latest photo or 96 dp fabric placeholder, name/status, Continue card, hairline sections | Photo hero 210 dp; project name is not uppercased; menu owns lifecycle/manage actions |
| Counter | Unit and fitted number, target/progress, image +/-/Undo, central ×N step editor and transient signed delta, secondary/stitches, followed by session/content/extra-counter/reminder areas | Project-specific steps 1–99; hero minimum is max(available viewport minus 24 dp, 360 dp); scroll remains possible when content does not fit |
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

Keyboard behavior is primarily Compose field/IME and focus behavior. There is no documented desktop-style command palette or global hardware-key shortcut map. The manifest is portrait-oriented; centered/max-width layouts are not evidence of a complete landscape/tablet redesign.

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
| Library reading guides | `rememberSaveable(patternUri)` | Presentation restoration; no project/document schema row created |
| Play product/entitlement | Billing/Pro process state and Play query | Readiness is explicit; not copied in a backup |
| Website login | WebView cookies/storage | Website session retained locally; not native OAuth or app identity |
| Restore preview approval | Selection-owned VM memory | Process recreation does not recreate approval to replace content |

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

Useful deterministic ordering includes document `sortOrder,id`, extra-counter `sortOrder,id`, bookmark `pageIndex,yFraction,createdAt,id`, descending session `startedAt,id`, and descending history timestamp/ID. Do not silently substitute composition order or mutable names for persistent tie-breakers.

### Preferences and other persisted state

Onboarding Phase 1 adds `last_project_craft_type` (default KNITTING, unknown values fall back to KNITTING) to ordinary preferences. The separate `OnboardingStateRepository` owns `onboarding_state`: initialization version, welcome dismissal, starter project, setup visibility/celebration, seen tip IDs, first-use day, counter entry/day counts, first-session pending state, a bounded chronological set of 200 celebrated project IDs, and successful-backup status. It uses safe read defaults and corruption replacement and is excluded from backup. Application-scope IO initialization checks unfiltered active-or-completed project existence and seeds basic tips for existing users. Successful replacement restore seeds basic tips; export success records backup creation. These guidance writes do not change the result of an already successful backup operation.

Phase 4 adds process-owned `TipSession` and pure `TipRules`, with `TipHost` rendering a shared `StitchMarkerTip` against registered bounds. The host waits for a resumed, loaded route and two seconds, respects modal/input/import/recovery/error blockers, and protects counter controls from overlap. Outside taps also reach the underlying control. One tip can be shown at a time; two automatic tips may be shown per process, plus chained UNDO. Seen state persists when a shown tip ends. `projectIncrementCounts` stores actual committed increments up to 30 per project, independent of the starting count and undoable history. Settings resets both persistent tip state and the process budget only on successful persistence. The backup suggestion is an inline Projects list card and uses the unfiltered project membership snapshot.

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

`allowBackup=false`, cloud/device-transfer rules exclude the root, `usesCleartextTraffic=false`, RTL support and locale config are declared. The exported Main Activity is singleTop/portrait/adjustResize. The widget receiver is exported with `BIND_APPWIDGET`; counter action receivers are nonexported. FileProvider is nonexported with temporary URI grants and only the two narrow image roots. Manifest declarations/configuration do not establish a tested release manifest or device/provider behavior.

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

Automatic capture, user profiles, replay, feature flags, push capture, tracing/logging/native crash capture, and GeoIP enrichment are disabled. Pseudonymous installation/session/device context still belongs to the SDK boundary. Disabling consent stops future app capture; it is not remote deletion of already-sent records. Payload behavior was inspected, not tested against a live provider here.

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

The October 20 source inventory contains **1,339 translatable resource names per locale: 1,308 strings and 31 plurals**, with matching names/types across all 12 locale sets. The base resources additionally have two nontranslatable strings. The seven two-form locales (en/fi/sv/nb/da/de/nl) each contain 1,370 text variants; the five three-form locales (fr/it/es/pt/pt-BR) each contain 1,401, for 16,595 translatable variants and 16,068 language/key pairs. Brazilian Portuguese covers all four files: `strings.xml`, `reference_strings.xml`, `pattern_annotations.xml` and `pattern_reading_assistance.xml`. Comparing strings.xml alone gives a misleading translation count.

The October 19 audit inventory of 1,350 keys / 1,318 strings / 32 plurals is historical. Current source removes ten strings (`yarn_weight_ply_format`, `size_col_head`, `size_col_height`, the four `size_row_adult_*` labels, `size_row_men_xs_30_32`, and the two `sym_slip_knitwise_*` entries) plus the unused `insights_rows_count` plural from every locale. This accounts for the 11-key difference without treating the older audit hashes/counts as current. Name/type parity does not prove grammar, placeholders, plural behavior or render fit; the saved acceptance and execution records retain their own input scope.

[LANGUAGE_AUDIT.md](LANGUAGE_AUDIT.md) records the October 19 closeout: all eleven original languages completed B01–B23 with `AUDIT_PASS`, followed by independently reviewed Brazilian Portuguese with the same result. Native-speaker review is `NOT_REQUESTED` throughout. These are recorded content decisions, not a new editorial review or proof that their old resource inventories still match the current tree. The [shared corrections](config/language-audit/shared-fixes.md) retain earlier failures and superseded recheck statuses; the campaign closeout reconciles those statuses and records no remaining content blocker at that time.

The [Brazilian Portuguese review](config/language-audit/pt-BR/review.md) and [validation receipt](config/language-audit/pt-BR/validation.json) record 84 passing JVM tests, five passing Android tests on API 36, debug builds and scoped ktlint. The Android checks covered language selection, persistence/startup, system synchronization, Brazilian resources and switching back to `pt`/System. Older Android/AppCompat runtime behavior was not exercised. These are prior task results, not tests rerun for this documentation update or certification of the subsequent resource removals. The 231 historical unverified English rendering cases remain supplementary evidence gaps and do not establish exhaustive UI certification.

### Formatting, units, and terminology

UI strings/plurals/content descriptions use Android resources; numbers/dates use locale-sensitive helpers. Session/history display respects local dates and 12/24-hour preferences; Insights uses stored session zones and locale first weekday. Null legacy zones fall back to the device. Folder uniqueness uses Unicode NFC/Root lowercase, not language-sensitive display sorting. Project names preserve original case.

Canonical calculation data uses mm, density/counts-per-mm and meters; user display can use metric/imperial units. Inches derive from canonical measurements; Yarn Estimator length can switch meters/yards while weight remains grams and its result preserves lowercase `g`. Gauge raw input is preserved independently from formatted output. Size Charts use localized numeric formatting (integral zero decimals, otherwise one), including both endpoints of ranges.

Finnish product terminology calls a knitting pattern an “ohje” and a web pattern a “verkko-ohje”; localized My Yarn uses `my_yarn_title`. Filename-like imported labels are cleaned without inventing designer metadata. English knitting abbreviations and the regex instruction grammar remain deliberately limited; translations do not imply parser support for all 12 locale options or distinct crochet abbreviation data.

Accessibility/source tests and historical instrumented locale evidence are listed below. No current 320 dp/200%/TalkBack/RTL or full multilingual visual run occurred in this documentation task.

### Resource and input conventions

Onboarding uses `values/strings_onboarding.xml` and matching da, de, es, fi, fr, it, nb, nl, pt, pt-rBR and sv files. Phase 10 supplies all 212 resources per locale and removes the temporary MissingTranslation suppression. Each key has a Finnish translator-context comment. English screen titles retain Title Case; other languages use natural local casing and craft idioms. Finnish uses ohje and verkko-ohje, and My Yarn uses each language's existing product name. `OnboardingResourcesSourceTest` checks every configured locale, exact key sets, types, placeholders, plural quantities, translator comments, live main-source consumers, prohibited separators, English title casing and Finnish terminology. `ProLocalizationSourceTest` preserves Pro/Billing localization coverage and the current active-project policy copy, and checks every `strings*.xml` and `pattern_reading_assistance.xml` for middle dots. The translation gate is tracked in `ANDROID_RELEASE_READINESS_PLAN.md`; run evidence is in `reports/onboarding-implementation/PROGRESS.md`.

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
| Android Gradle Plugin | 9.4.1 | App/test plugin, built-in Kotlin toolchain |
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
| Kotlinx serialization | 1.11.0 | Domain payload/archive JSON and Room support |
| Guava | 33.7.2-android | Required compatibility dependency |
| Splashscreen | 1.2.0 | Android launch surface |
| Play Review / Update | 2.0.2 / 2.1.0 | Play integrations |
| Billing | 9.1.0 | One-time Pro |
| Glance | 1.1.1 | Widget rendering |
| Coil | 3.6.3 | Compose image loading; explicit `coil-network-ktor3` for remote thumbnails |
| Ktor | 3.6.0 | Core/OkHttp image-network support; mock client in tests |
| WorkManager | 2.12.0 | Constrained Glance transitive runtime; does not implement continuous sync |
| Firebase BOM | 34.19.0 | Current Android Crashlytics dependency |
| Google Services / Crashlytics plugin | 4.5.0 / 3.0.8 | Variant configuration/mapping integration |
| PostHog | 3.72.1 | Release opt-in usage analytics |
| Sentry Android core | 8.60.0 | Debug-only diagnostics |
| Baseline Profile / Benchmark | 1.5.0 | Stable producer/benchmark tooling |
| Profile Installer | 1.4.1 | Profile installation support |
| Wire runtime | 7.1.0 | Explicit security-compatible resolution override |
| ktlint Gradle | 14.2.0 | Format/lint plugin |
| Detekt | 2.0.0-alpha.5 | Temporary AGP 9 compatibility exception |
| Compose rules | 0.6.3 | Detekt/ktlint Compose checks; pinned compatible rules build for Detekt 2.0.0-alpha.5 |
| Android security lint | 1.0.4 | Additional lint checks |
| Compose stability analyzer | 0.12.0 | Debug stability baseline/report |
| OWASP Dependency-Check | 13.0.0 | Dependency advisory scan |
| Sonar Gradle plugin | 7.5.0.8588 | Analysis/coverage integration |
| Dependency Analysis | 3.19.2 | Catalogued but root plugin disabled pending AGP 9 compatibility |
| JUnit / MockK / Turbine | 4.13.2 / 1.14.11 / 1.2.1 | JVM tests |
| org.json | 20260814 | JVM Android-JSON-compatible tests |
| AndroidX Test JUnit / Runner / Espresso | 1.3.0 / 1.7.0 / 3.7.0 | Device/Compose test support |

Root buildscript forces Jackson core/databind **2.22.3** and jsoup **1.23.2** for tooling. Subprojects force Wire 7.1.0 because negative-length decoding was corrected from 6.3.0. These are distinct from unrelated runtime upgrades. Detekt alpha is an explicit temporary exception; retain it until a stable AGP-compatible release is verified. Baseline tooling already uses stable 1.5.0. The inactive dependency-analysis catalog entry is not a configured passing check.

[gradle/verification-metadata.xml](gradle/verification-metadata.xml) records artifact verification checksums with metadata verification enabled and signature verification disabled. Update it only with verified resolved dependencies/artifacts; XML presence is not proof of a newly resolved graph. Backend uses `functions/package-lock.json`; DeepSec has its own pnpm lock/workspace plus root forwarding scripts/lock. No dependency was changed or resolved during this documentation task.

### Variants, isolated tests, and release admission

Normal debug includes tooling, Sentry and the debug demo facade/shared test support. Release source implements no-op debug diagnostics/demo behavior, is nondebuggable, minifies and shrinks resources with [proguard-rules.pro](app/proguard-rules.pro). Baseline plugin supplies `benchmarkRelease` and `nonMinifiedRelease` producer support; neither placeholder configuration is a release certificate.

`-Pknittools.isolatedTest=activepolicy` or `notes` configures only debug, with application suffix `.activepolicytest` / `.notestest`. [app/src/isolatedTest/AndroidManifest.xml](app/src/isolatedTest/AndroidManifest.xml) removes INTERNET/BILLING. The active-policy variant disables normal debug entitlement unlock; isolated profiles avoid destructive test work against the normal app package and control debug sample/test state. These properties must match the actual APK/test package for instrumentation; they are not runtime production options.

Release artifact tasks require:

1. `KNITTOOLS_KEYSTORE_PATH`, `KNITTOOLS_KEYSTORE_PASSWORD`, `KNITTOOLS_KEY_ALIAS`, `KNITTOOLS_KEY_PASSWORD`, and an existing keystore.
2. Real ignored `app/google-services.json` or `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64`, validated for the application package.
3. A syntactically valid PostHog project token from `KNITTOOLS_POSTHOG_PROJECT_TOKEN` or ignored `posthog.properties` (`projectToken`). SDK consent remains off by default even when the build requires ingestion configuration.

Debug/benchmark/nonminified producer variants can use generated ignored placeholders if no real Firebase configuration exists. Release artifacts cannot. `lintRelease` without artifact tasks may skip Google Services generation when real configuration is absent; passing lint in that path is not proof of a signed release artifact. Debug DSN uses `KNITTOOLS_SENTRY_DSN`/`SENTRY_DSN` or ignored local debug configuration.

The build disables generated Hilt unit-test Java compile tasks through a targeted workaround and reads AGP built-in Kotlin output directories for JaCoCo. Do not assume older AGP class paths or automatically remove these compatibility details without checking the owning configuration/tests.

### Baseline Profile and backend build

[baselineprofile/build.gradle.kts](baselineprofile/build.gradle.kts) uses the Android test plugin, namespace `com.finnvek.knittools.baselineprofile`, target `:app`, and connected-device producer configuration. A generator/profile installer is configured; no new profile generation or measured startup/render performance was executed here.

[functions/package.json](functions/package.json) is private, targets Node 22 and `lib/index.js`, builds with TypeScript 7.0.2, and tests by build then `node --test "lib/**/*.test.js"`. It declares firebase-admin 14.5.0, firebase-functions 7.4.0, @types/node 26.6.5 and pinned transitive overrides for brace-expansion/form-data/js-yaml/protobufjs/rimraf/uuid. Declared type-package version is separate from the Node runtime. Deployment uses root Firebase config with predeploy compilation; no deployment, secrets/rules change or live function call occurred here.

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

Backend runtime/tooling versions are declared separately: firebase-admin 14.5.0, firebase-functions 7.4.0, TypeScript 7.0.2, Node types 26.6.5, Node runtime 22. Type declarations do not raise the deployed runtime. Package overrides are brace-expansion 5.0.12, form-data 4.0.6, js-yaml 5.2.0, protobufjs 7.6.5, rimraf 6.1.3 and uuid 11.1.1. The independent npm lockfile governs installation.

## CI, analyzers, and release configuration

### GitHub configuration

[build.yml](.github/workflows/build.yml) runs on main pushes/PRs with read-only contents permission, pinned Checkout v7.0.1, Setup Java v6.0.1 (Temurin 17/signature verification), and Gradle Setup v6.4.0. It assembles debug, runs Gradle tests, `:app:ktlintCheck`, `:app:detekt`, and lint. A separate Node 22 job uses Setup Node v7.1.0, `npm --prefix functions ci --ignore-scripts`, then backend tests. It is not an emulator/device or Play purchase job.

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

At the inspected snapshot, [config/check-exceptions.json](config/check-exceptions.json) has **109 entries**: 46 MobSF, 41 gitleaks, 17 TruffleHog, 2 OSV, 1 Semgrep, 1 Detekt and 1 OWASP. The original 54 expire **2026-10-31**; 55 added language-audit non-secret exceptions (39 gitleaks, 16 TruffleHog) expire **2027-01-18**. The additions record scoped scanner matches in audit/reference material with rationale and selectors; they are not an authorization to suppress a scanner rule globally or evidence of a fresh scan.

[dependency-check-suppressions.xml](config/dependency-check-suppressions.xml) still has **12 suppression nodes**. A receipt's applied-suppression count (for example 14 in the Oct-16 security receipt) is a run count, not the current XML-node count. Keep rationale/path/expiry, registered entries, applied findings and scanner scope distinct. The stability baseline additionally records `BadgePill`'s new stable Boolean `uppercase` parameter; this inventory update is not a new stability run.

### DeepSec and release-surface checks

[.deepsec/package.json](.deepsec/package.json) pins DeepSec 2.3.9; [.deepsec/deepsec.config.ts](.deepsec/deepsec.config.ts) registers Android/Kotlin entry point, exported component, intent input, FileProvider/path, URI grant, widget mutation, sensitive logging and historical Ravelry credential/callable matchers. Root npm scripts forward into that workspace. A source scan selects candidates; process/revalidate uses an external model; export emits stored finding state. Reusing prior analyses or finding zero new files is not fresh semantic analysis of every source file.

`.deepsec/scripts/mark-accepted-risks.mjs` synchronizes narrowly identified historical credential findings with `config/security-decisions.md` and refuses reintroduced removed-risk surfaces. Old auth priority paths/prompt wording can remain historical configuration; they do not restore Android auth. Built-in TS/JS/Python matcher coverage alone is insufficient for the Kotlin application. Agent-assisted review is not a substitute for dependency SCA or device testing.

[tools/release-surface.ps1](tools/release-surface.ps1) checks release dependency/manifest/exported/credential/schema boundaries; companion release-surface, exported-surface and archive tests validate its own behavior. It allows Firebase BOM/Crashlytics and Google Services for Crashlytics while prohibiting AI/ML Kit/Gemini/voice and tracked private Google configuration, and scans locally/env-known Ravelry secret values without printing them. Firebase Auth and Functions are not approved Android dependencies; the forbidden-dependency checks enforce their absence.

Static release-surface success cannot establish a signed artifact, actual R8 behavior, provider/runtime grants, live telemetry, Play compliance, website permission or deployed Functions. Release readiness additionally needs valid signing/config, privacy/contact/release disclosures, supported platform tests and applicable Play/site decisions. Nothing was published or uploaded in this task.

### Configured execution versus current readiness

The single checked-in workflow file is `build.yml`, with independent Android and Functions jobs. It does not run an emulator, benchmark, screenshot/accessibility matrix, signed release, live Billing purchase, Firebase deployment, or automatic Play publishing. Repository code does not establish GitHub branch protection, enabled hosted analysis, current remote CI status or Play Console setup; those external states were not queried.

`tools/ad.ps1` is the install delegate and is intentionally separate from inspection. Other script tests cover wrapper portability, security delegation, pnpm dependency-check configuration, Sonar planning/timeouts, and release surface/archives. Their existence verifies neither current outcomes nor authorization to invoke scanner/upload/device operations during a documentation task.

The Sonar helper permits a read-only `-PlanOnly`; an actual upload requires `-AllowExternalUpload` and `SONAR_TOKEN`, and managed Gradle execution has a timeout. CLI authentication for issue reading is a separate mechanism. A successful upload task or an empty local issue response is not the authoritative hosted coverage/quality-gate result for a later revision.

Configured suppression inventories are not a scanner pass. Parse/tool errors, invalid/missing fresh reports and excluded source scope must remain visible. `REPORT_INVALID` or `TOOL_ERROR` with zero parsed findings is not a clean result. Detekt baseline count, registered exceptions, XML suppression nodes, applied findings, advisories and unsuppressed findings measure different things.

## Testing and evidence

### Current test architecture

The inspected source has 324 JVM-source Kotlin files and 141 Android-test Kotlin files, including fixtures/helpers. Functions has 14 `.test.ts` files among 33 TypeScript files, including harnesses. This is a source inventory, not a count of executed test methods, registered suites or successful checks.

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

Migration-specific instrumentation covers layered annotations (17), availability rebuild (19), bookmarks/guides (20), active sessions (21), project documents (22), folders (23), and broader migration paths. Schema-24 usage/schema-25 completion source/export/backup tests add their own boundaries; do not label every schema change as freshly instrumented from file existence.

Counter/session tests cover actual main-value rereads, concurrent writes, Undo eligibility, linked deltas, active checkpoint and recovery token idempotence. Real Room snapshot tests exercise cancellation releasing writers, project deletion/history replacement between batches and keyset filtering. Backup tests include 10,000/10,001/100,000 accepted sessions, 100,001 rejection, identity/annotation/file failures, and provider ownership. Native history is retained; the admission ceiling does not truncate older oversize histories.

### Feature-to-test ownership map

The following classes show where to select a focused check. They are present source, **not a claim of a fresh passing run**.

| Behavior / realistic failure path | Current owning tests |
| --- | --- |
| Active-project quota and reopening races | `ActiveProjectPolicyTest`, `ActiveProjectPolicyRoomTest`, isolated `ActiveProjectPolicyRuntimeTest`, `CounterReactivationPolicyTest` |
| Onboarding persistence, initialization and backup markers | `OnboardingStateRepositoryTest`, `LastProjectCraftPreferenceTest`, backup ViewModel tests; onboarding state is separate from backup contents |
| Starting count, target, pending draft compatibility and creation form | `ProjectStartValuesTest`, `CounterRepositoryCreateProjectTest`, `ProjectCreationActionsTest`, `ProjectCreationStateTest`, `ProjectDetailsSheetTest`; real Room and isolated policy cases in `ActiveProjectPolicyRoomTest` / `ActiveProjectPolicyRuntimeTest` |
| Projects start-state eligibility, dismissal, Tools and local demo | `ProjectsStartStateViewModelTest`, `ProjectFoldersScreenTest`, `WelcomeExampleCardTest`; no repository writes from the example and once-per-process demo analytics |
| Tip triggers, process budget, placement, input and reset | `TipRulesTest`, `TipSessionTest`, `TipHostTest`, `SettingsViewModelTest`, committed-counter and import feedback tests; no quota consumed before visible placement |
| Project setup, visit-scoped completion and explicit work-session guidance | `ProjectSetupProgressTest`, `CounterProjectSetupTest`, `ProjectSetupListTest`, `ProjectSetupNavigationTest`; TipSession/TipHost tests cover manual requests, unchanged automatic budgets, route ownership and delayed placement |
| Bundled guide content, actions and graph ownership | `GuideContentTest`, `GuideResourcesSourceTest`, `GuideViewModelTest`, `GuideScreenTest`, `GuideNavigationTest`; topic round trips, unknown-topic fallback, all actions, loaded project selection, stale-entry rejection, Pro readiness and reset feedback |
| Contextual Pro marker states, contrast and semantics | `ProActionAccessTest`, `ProMarkerContrastTest`, `ProMarkerTest`, `UiConsistencySourceTest`; context/status/readiness/flag matrix, shared resolver calls and unchanged exception lists |
| Pro consumer readiness, discovery and widget pinning | `ProPresentationConsumersTest`, `ProjectFoldersScreenTest`, `CounterProActionsTest`, `CounterProReadinessTest`, `CounterWidgetPinHostTest`; unfiltered project quota, independent marker/control readiness, merged action semantics, visible gated actions and supported/unsupported/declined pin requests |
| Committed target, first-session and completion celebrations | `CounterRepositoryCelebrationTest`, `CounterCelebrationSignalsTest`, `CounterCompletionCelebrationTest`, `ProjectListCompletionCelebrationTest`, `OnboardingStateRepositoryTest`, `TipSessionTest`, `TipHostTest`, `ProjectCompletedSheetTest`, `ProjectFoldersScreenTest`; transactional facts/rollback, visible per-project target memory, durable first-save identity, craft-aware facts, exact event consumption and Next with folder/quota preservation |
| Trial journey facts, progress, start queue and three-day notice | `TrialJourneyStateRepositoryTest`, `TrialThreeDayNoticeTest`, `OnboardingStateRepositoryTest`, `ProjectCounterRepositoryTest`, `TrialJourneyStartViewModelTest`, `InsightsTrialJourneyTest`, `PatternImageImportViewModelTest`, `TrialJourneyUiTest`, `TrialJourneyPresentationTest`, `TrialJourneyNavigationTest`, `TrialNoticeRestorationTest`; real loading, half-open trial window, active-project targeting, markers, successful-start queueing, actual visible/import completion, modal settling/focus, restored direct actions, atomic claims, persistence failure and notice recreation without repeat claims. Execution receipts are tracked separately in `reports/onboarding-implementation/PROGRESS.md` |
| Onboarding resource lifecycle | `OnboardingResourcesSourceTest`, `BrazilianPortugueseLocaleContractTest`, `ReferenceResourceLocalizationTest`; only the explicitly suppressed onboarding file may temporarily lack translations, and removing its suppression restores full completeness checks |
| Current-row main writes, Undo and widget semantics | `CounterRepositoryMainCounterChangeTest`, `CounterHistoryRoomTest`, `CounterRepositoryWidgetActionTest`, `CounterMainWriteResultTest` |
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
| Shared text/PDF intent queue and process restoration | `PatternShareCoordinatorViewModelTest`, `PatternShareIntentRestoreTest`, `IncomingPdfViewModelTest`, `ExternalActivityResultOwnershipTest` |
| Ravelry browser navigation / downloads | `RavelryLinksTest`, `RavelryBrowserNavigationTest`, `RavelryBrowserSourceTest`, `WebPdfDownloaderTest` |
| General website validation/external intents | `WebPatternUrlTest`, `WebPatternRepositoryRoomTest`, `ExternalWebLinkIntentTest`, editor route/screen tests |
| Play/trial/readiness failure paths | `BillingManagerTest`, `BillingRecoveryTest`, `TrialManagerTest`, `ProManagerTest`, `WidgetReadinessRuntimeTest` |
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
| Consent and data minimization | `PostHogAnalyticsTest`, `AutomaticBackupRulesTest`, release/security/source tests |

Locale source tests now discover configured tags through `ProjectSourceFiles.configuredResourceDirectories()`, translating regional `pt-BR` into Android's `values-pt-rBR` qualifier, and parse all applicable text-resource files. Existing source/UI tests were aligned to neutral counter text, hidden historical pace, and current resource placement. Runtime fixtures also wait for relevant PDF readiness, scope duplicate selectors, and measure actual rendered text lines; such test repairs do not establish new product behavior.

The Android suite includes destructive/process-style scenarios and opt-in fixture controls. Use the isolated application profiles and explicit runner arguments where required; ordinary connected instrumentation must not be assumed safe against a user's normal app data. The existence of a process probe does not prove that every kill point or all OEM process policies were exercised.

### Text annotation editing validation

The owning instrumentation uses [AnnotationTextTestFixture.kt](app/src/androidTest/java/com/finnvek/knittools/ui/screens/pattern/AnnotationTextTestFixture.kt): a uniquely named, file-backed Room database with synthetic project/document/master/overlay data and an assertion that the target package is `com.finnvek.knittools.activepolicytest`. Closing and reopening that database checks persisted state, not just ViewModel memory.

- [PatternAnnotationTextRoomTest.kt](app/src/androidTest/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationTextRoomTest.kt) has 16 cases covering edits and unchanged metadata/unknown JSON fields, reopen persistence, Cancel and late callbacks, repeated Undo/Redo and delete/restore history, stale Undo, invalid text, project read-only versus Library edit permission, page/document changes, deletion, selection changes and competing text/style/geometry writes. The failure test throws after the update inside an outer real Room transaction, before commit. Separate cancellation tests suspend before commit or after the outer transaction has returned, cancel the operation, and assert both the cancellation result and database contents; the post-commit case also reopens the database. These controlled boundaries do not simulate every process-kill or provider failure.
- [PatternAnnotationTextUiTest.kt](app/src/androidTest/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationTextUiTest.kt) has two scenarios for each of en/fi/de/fr × light/dark, giving 16 cases per display configuration. It exercises the production toolbar action wiring with the real ViewModel/repository, long multiline Unicode text, IME visibility, scrolling, text/input and native accessibility focus, minimum 48 dp touch bounds, label fit, read-only behavior and conflict feedback. Instrumentation arguments `textWidth` and `textFontScale` verify the requested device configuration. This is focused editor coverage, not a full PDF-reader journey or spoken TalkBack audit.
- [PatternAnnotationPayloadCodecTest.kt](app/src/test/java/com/finnvek/knittools/domain/model/PatternAnnotationPayloadCodecTest.kt) covers text replacement, unknown-field preservation, escaped Unicode and invalid/oversized/unsupported-version input. Existing `PatternAnnotationPageBudgetTest` and `PatternAnnotationEditorContextTest` remain companion regressions for bounded writes and editor context behavior.


Execution evidence is consolidated in [Existing execution receipts and their source limits](#existing-execution-receipts-and-their-source-limits). The fixture/test descriptions above establish intended assertions and implementation wiring, not a fresh execution in this task. The project forbids repeating its previously completed 320 dp / 200% / TalkBack checks without an explicit request for those checks.

### Existing execution receipts and their source limits

This table describes **pre-existing evidence read on disk**, not commands executed for this documentation update. The October 18 reconstruction recorded the older receipts; the October 20 refresh also inspected the later language-audit summaries. Reports under `reports/` are ignored/local artifacts and may be absent from a fresh clone. HEAD equality is insufficient when the source tree is dirty: compare the recorded input scope, hashes, time and actual executed tests.

| Evidence and date | Recorded source, command and outcome | Interpretation |
| --- | --- | --- |
| [Oct 19 shared reference summary](reports/language-audit/shared/20261019-b05/test-summary.json) | 107 JVM tests in 11 suites; zero failures, errors or skips | Targeted historical reference/calculator coverage, not a fresh run of the current checkout |
| [Oct 18 shared follow-up summary](reports/language-audit/shared/20261018-followup/unit-final-summary.json) and [shared correction history](config/language-audit/shared-fixes.md) | 188 JVM tests in 25 suites; zero failures/errors/skips; the report separately records five distinct Android methods passing after a focused rerun | Historical unit-policy/symbol follow-up; initial Android failures and their reruns remain in the record |
| [Oct 19 Brazilian Portuguese validation](config/language-audit/pt-BR/validation.json) | 84 JVM cases, five Android cases on API 36, build and scoped formatting checks recorded as passing | Locale integration evidence for those recorded inputs; no native-speaker review and no fresh execution here |
| [Oct 19 month-plural verification](config/language-audit/es/month-fix-verification.json) | Six JVM cases and one Android case on API 37, with resource/build and scoped formatting checks | Recorded plural/context fix; later reference-resource removals are not covered merely because this receipt passed |
| [Oct 19 PDF explanation measurement follow-up](config/language-audit/shared-fixes.md#la-en-b08-01--en-v-0621--pdf-explanation-measurement) | Six distinct Android methods eventually passed across retained runs, including three deliberately clipped/ellipsized negative controls | Corrected a test measurement defect without changing production typography or copy; synthetic detail-state/callback evidence does not prove PDF parsing or external delivery |
| [Oct 18 build manifest](reports/runs/20261018T181013661-bbbba5f4/manifest.json) and [raw build log](reports/runs/20261018T181013661-bbbba5f4/build.txt) | Dirty HEAD `605a802f`; `:app:assembleDebug :baselineprofile:assemble`; exit 0; all 113 actionable tasks up-to-date | Successful existing build task graph, not a fresh compilation or generated Baseline Profile |
| [Oct 18 JVM manifest](reports/runs/20261018T182204097-a1444e4d/manifest.json) and [raw test log](reports/runs/20261018T182204097-a1444e4d/test.txt) | Same dirty HEAD; `:app:testDebugUnitTest`; CLEAN, 1 executed / 37 up-to-date tasks; instrumentation explicitly NOT_RUN | Evidence for that recorded candidate, not all current Android runtime behavior |
| [Oct 18 lint manifest](reports/runs/20261018T185957776-74a1e7e8/manifest.json) | `:app:ktlintCheck :app:detekt` and `:app:lintDebug`; all CLEAN; Detekt 22 baseline entries; parsed lint findings 0 | Configured debug scope, not a new security or release verdict |
| [Oct 18 security manifest](reports/runs/20261018T174149500-d89fd1ea/manifest.json) | CLEAN summary; sourceState null; OSV 118 advisories / 0 retained findings; OWASP 14 applied suppressions | Unbound source snapshot; summary does not mean zero raw advisories or all external vulnerability data is current |
| [Oct 18 part-17 audit report](reports/audit-part17-20261018-01a14ea6/report.md), [run commands](reports/audit-part17-20261018-01a14ea6/validations.json), [device summary](reports/audit-part17-20261018-01a14ea6/device-summary.json) | Report records 2,147 JVM cases and 502 distinct final successful Android scenarios (545 successful executions including reruns), plus isolated process/Room/file/widget/editor exercises | Counts belong to that campaign. Initial failed/skipped/zero-discovery attempts remain distinguished. The report explicitly excludes live Play, Ravelry, cloud/telemetry and broad OEM/API coverage |
| [Oct 18 focused JVM summary](reports/audit-part17-20261018-01a14ea6/final-focused-jvm-summary.json) | 14 cases across three changed source/domain test classes; zero failures/errors/skips; timestamps retained | A focused rerun, not 14 additional unique cases on top of the broad suite |
| [Oct 18 backend log](reports/audit-part17-20261018-01a14ea6/backend-node22-test.log) | Reported Node 22.23.3; `npm --prefix functions test`; raw TAP totals 111 passed, 14 suites, zero failed/skipped | Local backend tests, not deployed OAuth/Firestore/Ravelry integration |
| [Oct 18 provider/render follow-up](reports/audit-part16-followup-20261018/FOLLOWUP.md) and [validation receipt](reports/audit-part16-followup-20261018/validation.json) | 21 device cases and 35 JVM cases recorded; PDF source-alias rejection, render-page isolation, image-state process restoration and sharing with another UID | Controlled synthetic fixtures. Provider aliases are known local aliases; camera hardware and arbitrary cloud provider internals were not verified |
| [Oct 16 JVM receipt](reports/runs/20261016T014050586-e2b0def5/manifest.json) | Successful unit-test command against dirty `8bb045dd`; instrumentation NOT_RUN | Older candidate predating current working-tree additions |
| [Oct 1 targeted Android receipt](reports/targeted-android-20261001/results.json) and [source binding](reports/targeted-android-20261001/source-tested.json) | 12 named cases in an older local emulator/Room/file/locale campaign | Useful historical regression evidence, not current-tree certification |

The Oct-18 build and JVM wrapper receipts contain 970 inputs with SHA-256 `ada769f69fb45cbff5485f8ecbffbed7917ccdfa8c1d8340f97fc578e19bdfb5`; the later lint receipt contains 970 inputs with `23589199d45fa714f697f72ec400e239ae1607cf75d3bb8e573a9995be99b828`. Both report stable inputs within their own run. Their differing fingerprints are preserved rather than collapsed into one “everything passed” status.

The part-17 report records final source fingerprint `4562441a2d886cf4a7e222fe9e9d21299fedf40cebfe3d1c32b83ef37d0ef58a`. It explains that one test wording edit completed during the broad run before compilation/test execution, making whole-run source stability false; a later focused rerun was stable. Its final Android correction changed a test APK while the production APK hash remained unchanged. Read its per-run evidence before attributing coverage to a later tree.

That campaign's ordinary editor configuration was 411 dp / font scale 1.15, with light/dark and selected en/fi/de/fr cases. It deliberately did not repeat 320 dp, 200% font, their combination or TalkBack. The earlier part-16 follow-up records its own explicitly requested narrow/large-font matrix. Neither campaign establishes the visual correctness of every screen, translation or device.

[config/local-backup.md](config/local-backup.md) retains an older API-36 emulator/DocumentsUI campaign and named unverified cloud-provider/resource boundaries. Earlier PROJECT.md annotation-editing notes reported an October 17 campaign whose temporary logs were no longer available; that narrative is not promoted to independently verified current execution evidence here. Current annotation tests and the later available campaign records are the useful reproducible references.

No fresh hosted Sonar gate, CI run, scanner service result, Play purchase, Crashlytics delivery, PostHog payload, Firebase deployment or Ravelry login was queried. Cached/up-to-date tasks, skipped tests, source-contract tests and genuine runtime cases must remain separate evidence classes.

### Documentation-specific verification

This update uses documentation-specific checks: compare the captured branch/HEAD/index and non-document file hashes; review the complete PROJECT.md diff; run `git diff --check -- PROJECT.md`; validate heading anchors, table of contents, Markdown tables and code fences; resolve local links; compare routes, schema fields, resource locales and dependency versions with their authoritative files; and replay the PROJECT-related literal assertions in `ArchitectureSingleSourceSourceTest`.

The October 18 documentation checks passed for all 30 leaf route templates and five graph routes, all 13 ProFeature entries, the 18-table/210-column dictionary, locale resource-name parity, and every declared version-catalog value. The generated contents matched 147 section entries; 213 local Markdown file/directory links resolved, and internal links, table column counts and code fences were consistent. The PROJECT-specific source-contract assertions and whitespace check passed. A before/after comparison of 1,120 tracked and nonignored untracked files found only PROJECT.md changed; branch, HEAD, index and the starting dirty-path inventory were preserved. These checks establish documentation consistency, not runtime correctness.

The **October 20 documentation checks** resolved all 226 local file/directory links and 152 internal links, including the referenced Markdown section anchor; confirmed the unchanged 147-entry contents against 149 headings, 66 consistently shaped tables and balanced code fences; checked schema 25's 18 tables/210 fields, all 50 version-catalog values, and resource name/type parity across 12 locales; and replayed the PROJECT-specific source assertions. The fresh resource count is 1,339 names per locale and 16,595 text variants overall. Source counts also confirm 46 size-chart rows with 123 measurements, including 58 ranges. `git diff --check -- PROJECT.md` passed. SHA-256 comparison of the other 1,251 tracked/nonignored files found no changes; branch, HEAD, unchanged index (no staged changes) and the original dirty-path inventory were preserved. External URLs were not fetched, and external standards, hosted-service state and the older audit's content acceptance were not revalidated.

Literal assertion replay is not a JUnit run. XML/schema/source inspection is not an APK build or a visual test. No Gradle build, instrumented/JVM/backend suite, emulator, physical device, scanner, deployment or external upload was performed for the documentation change. Existing test code and reports are identified as such; missing runtime evidence is explicitly retained as uncertainty.

The documentation-only boundary is root PROJECT.md. Existing modified and untracked implementation/test/configuration files are part of the described source snapshot and are preserved. No test or source is altered to satisfy a documentation assertion.

## Development and review invariants

### Workflow and source authority

[AGENTS.md](AGENTS.md) and [CODEX.md](CODEX.md) are aligned working instructions and must change together if either is edited. [CLAUDE.md](CLAUDE.md) owns product wording/design guidance. Inspect actual branch, staged/dirty work and owning implementation before a change. Source/config/schema/test implementation outrank a plan or AI finding; verify reported defects before editing.

Apply the minimal-sufficient-work deletion rule after correctness, explicit requirements and required checks. Preserve unrelated user changes. Reviews/plans do not authorize edits. Implementation requests require actual completion; For Android skill-driven work, inspect read-only and explain the evidence/approach before scoped edits; an explicit fix/implementation request authorizes those edits without a second approval of the same work. The specific repository prohibition on repeating previously completed 320 dp, 200% font, combined and TalkBack checks takes precedence over generic UI checklists. Report static, local, device, provider, service and release evidence separately.

Create a task feature branch when the checkout is a protected integration branch, use `codex/` for new branches, and commit/push only on explicit request. Never create/open/merge a PR: the user does that manually in GitHub. Inspect staged content and complete push commit/destination scope; no direct protected-branch updates. Commit messages are concise Finnish; source identifiers/comments stay English. Stop only task-owned temporary processes, never broad executable-name kills.

For this reconstruction, only PROJECT.md is an authorized output. In ordinary project work, commit/push require an explicit request; never commit/push protected integration branches or create/merge PRs. User-created PRs are the repository workflow. Use the current AGENTS.md/CODEX.md instructions, including their specific prohibition on repeating previously completed accessibility configurations, rather than treating an older checklist as authorization.

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

Current Android routes expose projects/folders/lifecycle, overview and main counter/history, secondary/stitch/extra/repeat/cadence counters, notes/reminders, explicit sessions and reviewed recovery, Library PDF/web/retained Ravelry metadata, multiple project documents, private import/share/photo-to-PDF, layered markup/chart tools/export, guides/calibration/bookmarks, progress/yarn photos, yarn inventory/project notes/usage, calculators/references, Insights and completion events, Glance widgets, preferences, trial/one-time Pro and manual replacement backup.

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
| Limited profile scenario | Baseline Profile generator currently collects startup; broad user journeys/performance claims need additional scenarios/measurements |
| Recorded future work | Continuous cloud sync specification; legacy backend global-limit compatibility cleanup only after deployed writer-drain verification |
| Intentionally absent | Voice/microphone/TTS, model-backed parser/cloud notes/content processing, release Sentry/replay, Firebase AI/ML Kit/Gemini, native storage-provider OAuth and automatic content sync |

The last two categories are not permission to implement them. No new roadmap, migration or unrequested production fix is introduced by this document.

### Supported limits and unresolved verification

| Area | Established fact / remaining verification |
| --- | --- |
| Current-tree execution | No fresh Android build/test/scanner in this documentation task; existing receipts bind their own dirty source snapshots, not this final document |
| UI/accessibility | Adaptive source, semantics and tests exist; no fresh 320 dp, 200%, combined, TalkBack/keyboard/IME, full locale or device matrix |
| Source shaping description | Stored fields and actual cycle display replace the obsolete forecast claim; product intent for further shaping behavior is not inferred |
| Large session history | 100,000-session writer/archive admission and bounded queries; maximum device latency/memory and writer blocking not measured here |
| File/provider resilience | Bounded copy/ownership/journals exist; hostile provider cancellation can retain admission, partial exported output can remain |
| Backup | Current schema/format acceptance only; no merge, encryption or authenticity; other budgets can reject an otherwise permitted native history |
| Cleanup | SQL commits before best-effort file deletion; orphans possible, with directory-specific pruning rather than a universal sweep |
| Browser | Arbitrary HTTPS redirect/subframe/download destinations can pass; retained website session, no native Disconnect/DNS firewall |
| Ravelry permission | Recorded requirement to obtain written site permission before Play release; acquisition not established |
| Billing/trial | Local timing/Play state handling implemented; live SKU/price/pending/ack/refund/revocation and store setup not verified |
| Telemetry | Release/debug/consent code inspected; vendor payloads, delivery, retention and symbolication not verified live |
| Backend | Deployment, secrets, Firestore indexes/rules/TTL and public service exposure unknown; source presence is not deployment proof |
| Release | Signing/config/admission paths exist; no fresh signed artifact, release merged-manifest proof, mapping upload, Play review or publication |
| Privacy/contact | Technical privacy draft reflects current architecture; responsible publisher contact remains missing |
| Localization | 1,339 translatable resource names per locale across 12 locale options (October 20); October 19 audit acceptances/counts precede 11 removed keys and are historical; no new language or exhaustive rendering certification |
| CI/CodeQL/Sonar | Build workflow configured, local CodeQL wrapper present, hosted state unqueried; old CodeQL workflow claim removed |
| Security checks | 109 dated exceptions (54 expire October 31, 2026; 55 January 18, 2027), 12 XML suppression nodes and 22 Detekt baseline IDs are inventories, not a new vulnerability verdict |

An unexecuted check is uncertainty, not automatically an implementation defect. The document records actual limitations and source discrepancies without treating every stronger possible design as required work.

### Related documents and authority

| Source | Use and caveat |
| --- | --- |
| [AGENTS.md](AGENTS.md), [CODEX.md](CODEX.md) | Aligned working rules, ownership/security/UI conventions; unchanged here |
| [CLAUDE.md](CLAUDE.md) | Product wording and visual direction; current implementation still decides exact behavior |
| [README.md](README.md) | Short current overview; its Gradle 9.8.0 sentence is behind wrapper 9.8.1 |
| [ONLINE_OFFLINE.md](ONLINE_OFFLINE.md) | Current network/offline distinctions, not the old native Firebase flow |
| [PRIVACY-POLICY.md](PRIVACY-POLICY.md) | Updated technical draft with current browser/telemetry/backup model; publisher contact still required |
| [config/security-decisions.md](config/security-decisions.md) | Recorded current browser, Crashlytics, analytics and removed-credential decisions |
| [config/posthog.md](config/posthog.md) | Current event/configuration policy, SDK 3.72.1; live vendor operation remains separate |
| [LANGUAGE_AUDIT.md](LANGUAGE_AUDIT.md), [shared corrections](config/language-audit/shared-fixes.md), locale reports under [config/language-audit](config/language-audit) | Recorded content acceptances, technical handoffs and historical scoped checks; their 1,350-key snapshot predates the current 1,339-key inventory |
| [config/local-backup.md](config/local-backup.md) | Detailed format/recovery design plus explicitly dated emulator/SAF evidence |
| [config/insights-session-budget.md](config/insights-session-budget.md), [config/annotation-page-limits.md](config/annotation-page-limits.md) | Resource-budget rationale and implementation limits |
| [config/future-sync-spec.md](config/future-sync-spec.md) | Future decision/specification boundary, not a shipped synchronization service |
| [config/ravelry-backend-progress.md](config/ravelry-backend-progress.md), [backend plan](<Ravelry Firebase Backend And Saved Patterns Plan.md>) | Retained backend history; not Android reachability or cloud-state verification |
| Root product/UI plans and tracked `docs/superpowers` material | Historical intent; previous typography/layout/voice proposals can be superseded or deliberately absent |
| [.deepsec/README.md](.deepsec/README.md) | DeepSec usage/history; its old Phase-9 Auth/Functions allowance wording is superseded by current release-surface code |
| Ignored `reports/` artifacts | Read as dated evidence tied to command/source fingerprints; availability in another checkout is not guaranteed |

### Reconstruction audit and maintenance boundary

The October 18 reconstruction established this product-first reference. The October 20 update retains its structure and technical detail, updating affected workflows, inventories, configuration and evidence in place. Architecture, the full schema dictionary, file rules and toolchain remain together; historical verification is consolidated under Testing and evidence.

The current refresh corrects branch/HEAD and distinguishes changes already committed in `e4416c7c` from the remaining working tree. It reconciles historical-unit suppression, session-zone bounds, neutral counter copy, M1/cast-on behavior, reference tables/symbols, care/weight/widget presentation, Brazilian Portuguese and month plurals, expanded test ownership, and scanner/stability inventories. Older resource audit counts and execution receipts are explicitly historical rather than extended to later inputs.

All Android Ravelry descriptions use the WebView/site-session/download implementation. Backend OAuth/search/import is confined to the retained-backend explanation. Crashlytics is release crash reporting; PostHog is separate release-only optional usage analytics; debug Sentry is variant-specific. Manual backup remains free local-data export/replacement through SAF, not cloud synchronization. Current schema is 27 and current reader authority is project_documents.

Preserve the distinction between code, configuration, source assertions, historical run receipts and unverified external behavior when maintaining this reference. A newly added test, a green summary without positive test execution, a stable old hash or a deployment plan must not become a claim that a new working tree or live service was verified. Missing proof is not itself a confirmed production defect.
