# KnitTools Project Reference

## Scope and verified source snapshot

This document describes the implementation inspected on **2026-10-11**, not a release certification. Current source and configuration take precedence over development plans, earlier reports, and older documentation.

| Item | Inspected value |
| --- | --- |
| Repository | KnitTools |
| Branch | `fix/vanhentuneet-laitetestit` |
| HEAD | `770f019f21001087eb5bc57a028b674c44f149de` |
| Initial working tree | Clean; no staged changes or relevant untracked files |
| Android application ID / namespace | `com.finnvek.knittools` |
| Android modules | `:app`, `:baselineprofile` |
| Retained backend | `functions/`, Firebase Functions v2 TypeScript |
| Room schema | 25 |
| Current Room tables | 18 |
| App version | `versionCode = 1`, `versionName = "1.0.0"` |
| Supported app languages | 11, plus the System language selection |
| Platform | Android API 29–37; compile SDK 37, target SDK 37 |
| Documentation verification | Source/configuration/report inspection and document checks; no new build, JVM, emulator, device, billing, network, scanner, or deployment execution |

The source inventory contains 386 main Kotlin files, 304 JVM-test Kotlin files, 71 Android-test Kotlin files, and 33 backend TypeScript files. Counts describe the inspected tree, not executed tests. Historical receipts and their limits are grouped in [Testing and evidence](#testing-and-evidence).

Important source roots are [app/src/main/java/com/finnvek/knittools](app/src/main/java/com/finnvek/knittools), [app/src/main/res](app/src/main/res), [app/src/test](app/src/test), [app/src/androidTest](app/src/androidTest), [app/schemas](app/schemas), and [functions/src](functions/src). In prose, package-relative paths such as `repository/CounterRepository.kt` are relative to that main Kotlin root. Markdown source links use repository-relative paths.

## Contents

- [Product and feature behavior](#product-and-feature-behavior)
  - [Projects and organization](#projects-and-organization)
  - [Counters, stitches, reminders, and notes](#counters-stitches-reminders-and-notes)
  - [Work sessions and history](#work-sessions-and-history)
  - [Library, patterns, and project documents](#library-patterns-and-project-documents)
  - [PDF reading, annotations, and creation](#pdf-reading-annotations-and-creation)
  - [Photos and yarn](#photos-and-yarn)
  - [Calculators and reference material](#calculators-and-reference-material)
  - [Insights](#insights)
  - [Widgets and settings](#widgets-and-settings)
- [Navigation and external entry points](#navigation-and-external-entry-points)
- [Visual system and accessibility](#visual-system-and-accessibility)
- [Architecture and lifetime](#architecture-and-lifetime)
- [Persistence reference](#persistence-reference)
  - [Room ownership and constraints](#room-ownership-and-constraints)
  - [Complete table dictionary](#complete-table-dictionary)
  - [Migration chain](#migration-chain)
  - [Preferences and other persisted state](#preferences-and-other-persisted-state)
- [Files, PDF processing, and cleanup](#files-pdf-processing-and-cleanup)
- [Current Ravelry integration](#current-ravelry-integration)
- [Pro, trial, and billing](#pro-trial-and-billing)
- [Manual backup and replacement restore](#manual-backup-and-replacement-restore)
- [Security, privacy, and telemetry](#security-privacy-and-telemetry)
- [Localization](#localization)
- [Toolchain, dependencies, and build variants](#toolchain-dependencies-and-build-variants)
- [CI, analyzers, and release configuration](#ci-analyzers-and-release-configuration)
- [Testing and evidence](#testing-and-evidence)
- [Development and review invariants](#development-and-review-invariants)
- [Current status, limitations, and document authority](#current-status-limitations-and-document-authority)

## Product and feature behavior

KnitTools is a local Android workspace for knitting and crochet. It connects project organization, row/round counting, patterns, yarn information, work sessions, and measurements without requiring an app account for local work. Projects are the central ownership boundary: a project can have counters, notes, sessions, documents, annotations, bookmarks, reminders, yarn notes/usage, and progress photos. Library entries can be reused across projects.

The five primary destinations are **Projects, Library, Tools, Insights, and Settings**. The app distinguishes a project's overview from its working counter. Most project management lives in the overview; the counter prioritizes the number, immediate controls, and work-session actions. The app implements local pattern PDF import, project attachment, reading-line calibration, layered annotations, and annotated export. Ravelry is currently a website browser and PDF-download entry point, not an Android API/OAuth integration.

Local counters, sessions, calculations, saved metadata, and files do not require Ravelry. Internet is needed for Ravelry pages/downloads, remote thumbnails, Play operations, release crash reporting, and enabled usage analytics. Selecting a cloud-backed Android document provider for import or backup is still a user-selected file operation, not background synchronization.

### Projects and organization

**Entry and workflow.** Projects opens a sorted project list with a Continue hero when the current filter contains a suitable active project with progress. A project card opens `project_overview/{projectId}`. The hero's Continue action selects the project and opens Counter. New-project completion and approved external counter launches also open Counter. The overview shares counter state and provides the project name, craft, progress, documents/pattern, yarn, notes, reminders, photos, and management actions.

**Creation and naming.** Creation supports knitting/crochet and main-counter label configuration. Knitting defaults to rows; crochet defaults to rounds. Custom labels are trimmed and constrained by the domain model. Project names are trimmed and must be nonblank; `ProjectNameRules` does not impose a verified maximum length. Creation makes a unique display name using case-insensitive `Locale.ROOT` keys and suffixes such as ` (2)`. Do not apply the additional-counter or folder name limit to project names.

**Free active-project policy.** Without the unlimited-project entitlement, creation/reactivation allows one active project. Completed projects do not consume that quota. Existing projects are not deleted when entitlement ends; existing multiple active projects remain usable, while new active-project admission is restricted. The repository enforces the policy within the same transaction as creation/reactivation. An optional target folder is validated and assigned in that creation transaction.

**Organization.** All Projects and Unfiled are typed virtual filters, not database rows or sentinel folder IDs. A project has at most one folder assignment; no assignment means Unfiled. Folder create/rename trims names, rejects controls and line separators, normalizes uniqueness with NFC plus `Locale.ROOT` lowercase, and limits the display name to 50 Kotlin string units. Folder deletion moves its projects to Unfiled without changing project content. Bulk moves validate every distinct project and destination before writing.

**List state.** Sort modes are Updated, Name, and Created (`updated`, `name`, `created`); Updated is the default. SQL supplies the sorted active/completed flows, and the ViewModel filters them by folder afterward. The current folder filter lives in `SavedStateHandle`, not DataStore. Fresh tasks start in All Projects; restored folder IDs are validated only after Room supplies a real snapshot. Show Completed is a global preference. Folder filtering affects the Projects list and Continue hero, not Library, Insights, widgets, project-ID routes, or history.

**Selection and terminal actions.** Selection is limited to visible items. Bulk complete/delete processes project IDs sequentially and stops on the first failure/unavailable result; earlier successful changes are not rolled back as one batch. A selected active-session project requires an explicit session Save/Discard decision before completion, or discard authorization before deletion. Rename, craft/label/target changes, folder moves, completion, reopening, and deletion are managed through the overview/list actions rather than the counter's tool sheet.

**Completion.** Each active-to-completed transition records a `project_completions` event atomically. Reopening retains those events and checks the active-project quota using the observed completion generation. Repeated completion cycles count separately in Insights. Deletion cascades project-owned rows, removes reciprocal yarn links, and performs physical cleanup only after the database mutation commits.

Sources: [ProjectListViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListViewModel.kt), [CounterRepository.kt](app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt), [ProjectFolderRepository.kt](app/src/main/java/com/finnvek/knittools/repository/ProjectFolderRepository.kt), [ProjectNameRules.kt](app/src/main/java/com/finnvek/knittools/repository/ProjectNameRules.kt), [ProjectSortOrder.kt](app/src/main/java/com/finnvek/knittools/domain/model/ProjectSortOrder.kt).

### Counters, stitches, reminders, and notes

**Main counter.** The hero displays the selected row/round/custom unit above the large number. Increment/decrement use the configured positive step, floor at zero, and saturate at `Int.MAX_VALUE` using wider arithmetic. A target adds the shared progress bar and localized target/remaining text. The target is a presentation goal, not an upper bound on the count. Completed projects reject working-counter changes.

`CounterRepository.applyMainCounterChange` is the canonical writer. It rereads the current project and updates the main count, history, current-stitch reset, linked extra counters, active-session checkpoint, and followed reading-line position in one transaction. Widgets delegate to those semantics. UI state cannot safely replace the repository's current-row read.

**Undo and history.** Main changes create history rows with action, previous/new values, and timestamp. Undo considers the latest eligible history row and checks it against the actual current count/action before reversing it and deleting that row. It is not arbitrary time travel or an in-memory-only button. Counter History groups persisted records by local date and uses device time-format preferences; missing projects leave the route. The 100,000-row completed-session ceiling is not a counter-history limit.

**Legacy secondary counter.** The independent `counter_projects.secondaryCount` remains the compact secondary counter. It is not duplicated into `project_counters`. `secondaryCounterUsed` is a monotonic repository-backed flag: content already used remains accessible after Pro ends. New secondary-counter use is gated separately from ordinary main counting.

**Extra counters.** Add/Edit Counter persists the chosen type, step, order, and linked-to-main option. Names are trimmed and limited to 50 Kotlin string units without splitting a surrogate pair. Domain validation is centralized in `ProjectCounterType` and `ProjectCounterLogic`.

| Type | Behavior and required values |
| --- | --- |
| `COUNT_UP` | Independent count/step; optionally receives actual main-counter deltas |
| `REPEATING` | Positive repeat threshold; wraps `(count + step) % repeatAt` after reaching the threshold |
| `SHAPING` | Nonnegative starting stitches, a stitch change, and positive row cadence; counter itself increments normally |
| `REPEAT_SECTION` | Positive start row, inclusive end at/after start, positive repeat count; derives position from main rows and cannot be marked linked |

Shaping display computes `max(0, startingStitches + floor(currentRow / shapeEveryN) * stitchChange)`, bounded to `Int.MAX_VALUE`. Shaping occurs at positive exact multiples of cadence; next-shaping row is the next such multiple. A repeat section uses `end - start + 1` rows per repeat, derives repeat/row from the main row, reports zero before its start, and clamps to the final tracked repeat afterward. Existing counters can be operated/edited after entitlement ends; creating gated types requires their corresponding features. Old generated `Pattern repeat` backfill copies are excluded at the UI boundary.

**Stitches.** The Track stitches switch enables tracking. If no positive total is configured, enabling opens the total dialog; saving a valid total stores it and enables tracking in one transaction, while cancelling leaves tracking disabled. Editing the total separately preserves the enabled/disabled choice. Disabling tracking preserves the total and resets the current stitch to zero; clearing the total also disables tracking. Current stitch stays in `0..stitchCount`; lowering the total clamps it, and a successful main-row change resets current stitch to zero when tracking is enabled. Stitch tracking and secondary counting are distinct state. Completed projects cannot change working stitch state.

**Reminders.** Row reminders are in-app messages for an exact row or positive repeat-interval multiples. Messages are trimmed, nonblank, and limited to 200 characters. They are not Android scheduled notifications and do not promise an alert for every intermediate row skipped by a multi-row increment. Creation is Pro-gated; stored reminders remain project-owned content.

**Notes.** The Notes editor is local and project-owned. The top bar says Notes; a separate `ProjectNameLabel` identifies context without uppercasing the name. `saveProjectNotes` merges an editor result against its base notes so concurrent editor flows are not silently overwritten. `notesCreated` preserves access to existing content after entitlement ends. There is no remote cleanup, journal processing, or model-backed rewrite pipeline.

Sources: [CounterViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt), [ProjectCounterRepository.kt](app/src/main/java/com/finnvek/knittools/repository/ProjectCounterRepository.kt), [ProjectCounterLogic.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/ProjectCounterLogic.kt), [ShapingCounterLogic.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/ShapingCounterLogic.kt), [RepeatSectionLogic.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/RepeatSectionLogic.kt), [NotesEditorScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/notes/NotesEditorScreen.kt).

### Work sessions and history

**Explicit work lifecycle.** Start/Stop actions control a single Room-owned active session across the application. Navigation, backgrounding, opening history, and ViewModel clearing do not start or finalize it. A UI ticker updates visible timing; durable wall-clock/elapsed-realtime/boot/zone anchors establish correctness while the app is not ticking.

Starting distinguishes Started, Already Active, Project Conflict, Missing Project, Completed Project, History Limit Reached, and Persistence Failure. Only one active project session exists. Main app/widget counter changes checkpoint trusted duration and net rows in the same transaction as counting. Completed session rows store start/end row, display minutes, exact seconds, net rows worked, and a zone captured at session start. The completed-session retention ceiling is 100,000 rows.

**Timing and recovery.** On the same trusted boot, elapsed time is checkpointed seconds plus the nonnegative elapsed-realtime difference, with saturating addition. Recovery review is required for invalid anchors, unavailable boot identity, changed boot count, regressing elapsed realtime, or an unreviewed interval of at least 86,400 seconds. Reasons are `REBOOTED`, `BOOT_IDENTITY_UNAVAILABLE`, `INVALID_ANCHORS`, and `LONG_RUNNING`. A credible wall-clock duration after reboot is a suggestion, not silently trusted time.

**Review actions.** Active records carry a stable `sessionToken` and a `recoveryIntervalToken`. Stop/discard/prompt/recovery operations reject stale context. Stop refuses automatic finalization while review is needed. Recovery Add continues from new trusted anchors; Discard and Edit finalize through idempotent token-guarded repository transactions. Invalid duration, stale action, history limit, and persistence failures remain explicit outcomes. Completion/deletion UI decisions do not replace repository safeguards.

**History and metrics.** Session History supports project context and a delete menu. Completed history rows have no edit action; duration editing belongs to active-session recovery. Display minutes remain compatible with old rows, but calculations use exact `durationSeconds` and `rowsWorked`. New zone IDs preserve the original local day when a session is later viewed elsewhere; legacy null zones fall back to the current device zone. Cross-midnight splitting belongs to the metrics/Insights calculation, not a naive UTC-date grouping.

Sources: [ActiveSessionTiming.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/ActiveSessionTiming.kt), [ActiveSessionResults.kt](app/src/main/java/com/finnvek/knittools/repository/ActiveSessionResults.kt), [SessionTimeSource.kt](app/src/main/java/com/finnvek/knittools/data/time/SessionTimeSource.kt), [SessionHistoryViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/session/SessionHistoryViewModel.kt), [SessionMetrics.kt](app/src/main/java/com/finnvek/knittools/ui/screens/insights/SessionMetrics.kt).

### Library, patterns, and project documents

**Library.** The hub leads to Saved Patterns, My Yarn, and All Photos. Library state is independent of the Projects folder filter. Saved Patterns includes local PDFs, retained Ravelry metadata, and user-added website links. A saved item can be attached through a project picker that includes active and completed projects.

**Saved Pattern identity.** The existing `saved_patterns` row uses `SavedPatternSource` (`Ravelry`, `LocalFile`, `WebLink`, `Other`, `Unknown`) and nullable source fields. Source enum definitions live in `domain/model/SavedPattern.kt`. Persisted fields are `ravelryPatternId`, original/canonical URL, local PDF URI, offline availability, update/sync timestamps, and `PatternAvailability` (`free`, `paid`, `unknown`). Compatibility getters such as `ravelryId` do not justify restoring integer-zero or old URL sentinels in storage.

Duplicate matching checks Ravelry ID, canonical URL, normalized original URL, then title/designer only when explicitly requested. Details own available actions according to actual source/file availability. Missing observed items leave their detail route. Remote thumbnails use the shared HTTPS-only component and disappear on invalid/missing/failed URLs rather than inserting a placeholder or project-owned duplicate image.

**Website links.** Add/Edit Web Pattern saves a local title/designer/URL. Valid HTTP links are permitted with an insecure-link warning; browsing revalidates before an external browsable `ACTION_VIEW`. Ravelry pattern links instead enter the in-app Ravelry browser. Compatible old `Other` rows count as web patterns only without a local PDF and with a valid non-Ravelry web URL.

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

**Bookmarks.** Project bookmarks have a trimmed nonempty name of at most 50 string units, validated page/Y, and the active project's document key. Names may repeat. Order is page, Y, creation, ID. Detach preserves bookmarks, reattachment of the same document restores them, replacement hides the old document's bookmarks, and project deletion cascades. A jump pauses follow and issues a one-shot viewport focus request after page rendering. Bookmarks are free and excluded from annotation export.

**Layers and tools.** Library owns an editable saved-pattern master layer. A project viewer renders that reusable master read-only plus its own editable overlay for the same document key. Detach deactivates project ownership without deleting edits; same-document reattach restores it. Tools include pen, highlighter, eraser, select, line, arrow, rectangle, ellipse, text, callout, and chart. Text/callout open an editor; chart tracking resolves domain drafts/counter identities rather than creating a separate opaque counter system.

Coordinates are normalized page coordinates transformed once by `PatternPageCoordinateTransform`. Pointer movement remains in ViewModel memory; persistence occurs at gesture/command boundaries. A 50-command in-memory undo/redo stack is document/layer-context guarded. `PatternAnnotationCanvasRenderer` renders viewer overlays and rasterized export consistently. Budget/codec checks reject unsafe overlarge legacy pages as a whole while retaining rows; they do not silently display a truncated prefix.

**Export.** Marked PDF export uses SAF `CreateDocument(application/pdf)`, renders one bounded page bitmap at a time into a cache temporary PDF, reports progress, then copies to the selected destination. It preserves the original PDF and cleans the temporary file on success/failure/cancellation. The output is rasterized: original searchable text/vector/link structure is not preserved. Reading guides/bookmarks are excluded. The chosen provider may retain a partial destination on failure. Exact budgets are in [Files, PDF processing, and cleanup](#files-pdf-processing-and-cleanup).

**Camera/gallery.** Photo-to-PDF creation uses photo/PDF wording, not a document scanner claim. Camera captures and Photo Picker images are immediately copied into a private session, ordered in a restorable ViewModel draft, bounded/decoded, and converted page by page. Limits: 20 pages, 25 MiB/image, 200 MiB/session, 1,800-pixel long edge, 32 MiB reserve. Animated images are rejected; transparency is flattened onto white. Camera-created patterns are gated by `PATTERN_CAMERA_SCAN`; document import/reading is separate. Gallery's narrow/large-font provider fallback is described under storage/accessibility.

Sources: [PatternViewerScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt), [PatternAnnotationViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationViewModel.kt), [PatternAnnotationRepository.kt](app/src/main/java/com/finnvek/knittools/repository/PatternAnnotationRepository.kt), [PatternDocumentStorage.kt](app/src/main/java/com/finnvek/knittools/data/storage/PatternDocumentStorage.kt), [PdfPageRenderer.kt](app/src/main/java/com/finnvek/knittools/data/storage/PdfPageRenderer.kt).

### Photos and yarn

**Progress photos.** Project Photos captures/images are stored privately with row number, note, and creation time. Project Gallery and Library All Photos offer viewing and row menus; All Photos uses the shared project filter. The photo viewer is fully opaque. Capture-target creation and abandoned-target deletion belong to `ProgressPhotoRepository` / `CounterViewModel` on injected IO, not composables. New capture is gated; existing photos remain data. Notes are capped at 100 characters; image processing is bounded to a 1,920-pixel edge and JPEG quality 80.

**My Yarn.** A yarn card stores brand/name/fiber, grams/length per skein, needle/gauge information, color/dye lot, weight category/care bitset, optional private photo, integer stash quantity, status, and optional project link. Status is In Stash, In Use, or Finished; unsupported stored values normalize to In Stash. Quantity defaults to one stash unit and is a nonnegative integer, not a meter/gram balance.

New yarn-card creation requires `UNLIMITED_YARN`; there is no implemented numeric free-card allowance. Edits/existing cards remain supported after entitlement ends. Do not invent a free limit of several cards. Status uses `SegmentedToggle` below the name and a shared trailing status pill on list cards. Linked cards show the localized project line; redundant In Use decoration is hidden where that line already communicates it. Yarn weight uses standardized Ravelry names plus editable imported/Other text.

Detail routes observe the actual card. Missing rows exit; edits use repository results and observation instead of optimistic local-only state. `YarnCardRepository` owns reciprocal `linkedProjectId` and project yarn-ID CSV writes; `YarnCardLinks` parses/formats that CSV, capped at 1,000 IDs per project. Photo replacement copies first, persists the new URI, then deletes the old private photo. Startup pruning removes unreferenced yarn-photo files.

**Project yarn notes.** A lightweight project-owned yarn description can exist without a Library card. Save to My Yarn creates/reuses a linked card and preserves the note through `savedYarnCardId` in one transaction. If usage already exists, it binds that record rather than creating duplicate usage; saving a note does not create usage automatically.

**Project usage.** A yarn card and the project note referring to it resolve to one logical `project_yarn_usage` record. Planned, allocated, and used amounts are independent nullable canonical `Double` meters: null means unknown, zero means known zero. Remaining is allocated minus used; negative values display overage. Converting grams or fractional skeins requires a user-confirmed positive meter/gram relationship. Usage has no Pro gate and does not deduct global stash quantity, update project timestamps/counters, touch sessions, or change files/documents.

`ProjectYarnUsageRepository` owns coherent observation, source identity, validation, revision checks, and transactional create/update/delete. Source deletion preserves editable usage through nullable source FKs and a creation-time name snapshot. Live source names take priority when available. Unlinking, completion, and reopening retain usage.

Sources: [YarnCardRepository.kt](app/src/main/java/com/finnvek/knittools/repository/YarnCardRepository.kt), [ProjectYarnNoteRepository.kt](app/src/main/java/com/finnvek/knittools/repository/ProjectYarnNoteRepository.kt), [ProjectYarnUsageRepository.kt](app/src/main/java/com/finnvek/knittools/repository/ProjectYarnUsageRepository.kt), [YarnCardDetailScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/yarncard/YarnCardDetailScreen.kt), [ProgressPhotoRepository.kt](app/src/main/java/com/finnvek/knittools/repository/ProgressPhotoRepository.kt).

### Calculators and reference material

Tools presents local calculators/reference pages plus Ravelry. Calculator actions do not write Room, preferences, project notes, or network services. Copy writes plain text to the clipboard. Optional project context for Gauge is navigation context, not automatic project mutation.

| Tool | Implemented calculation / behavior | Authority |
| --- | --- | --- |
| Increase/Decrease | Valid positive current/change counts; decrease requires fewer than current and enough stitches for paired decreases. Computes easy/balanced flat/circular instructions using `M1`, `K2tog`, and distributed gaps; uses wider arithmetic to reject overflow | `IncreaseDecreaseCalculator` |
| Measurements and Gauge | Unit conversion, swatch/manual stitch and row density, count for desired size, size from count, and adjustment from pattern gauge to actual gauge | `MeasurementCalculator`, `GaugeViewModel`, `GaugePresentation` |
| Cast On | Canonical density from per-10-cm/per-4-inch gauge; rounds body stitch count, optionally snaps to nearest repeat, then adds edge stitches; reports down/up alternatives and actual widths | `CastOnCalculator` |
| Yarn Estimator | Positive finite total length, length/skein, grams/skein; exact skeins rounded up to two decimals, purchase count `ceil(total/lengthPerSkein)`, total grams from whole purchase count | `YarnEstimator` |
| Needle Sizes | 25 static metric sizes from 1.5–25 mm, available US/UK-Canadian/Japanese equivalents; metric-prefix or normalized exact regional search | `NeedleSizeData` |
| Size Charts | Static Craft Yarn Council measurements in cm, derived inches; Baby, Child/Youth, Women, Men, Head, Hand categories | `SizeChartData` |
| Abbreviations | Resource-backed localized meanings/descriptions and case-insensitive search; current knitting/crochet selector returns the same dataset | Abbreviation data/screen |
| Chart Symbols | Static resource-backed symbols/reference meanings | Chart-symbol data/screen |

Measurements keep raw edit text and canonical `Double` millimeters/counts-per-millimeter in `GaugeViewModel` and `SavedStateHandle`. Unit switches preserve canonical values; stitch and row density keep separate manual/swatch provenance. Density is count/length, size is count/density, and adjusted count is actual density times original pattern size. Display rounding never feeds arithmetic. Whole-count conversion rejects nonfinite/negative/overflow values; some results legitimately round to zero.

Regex paste is entitlement-gated using current `isPro`, separate from the feature enum. `InstructionParser` accepts at most 10,000 characters, supports selected English increase/decrease/gauge/swatch phrases, and returns `input_too_long` beyond its limit. Per-inch gauge is converted to the per-four-inch representation. Localized output does not establish arbitrary-language parsing support. There is no model/AI parser.

Sources: [MeasurementCalculator.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/MeasurementCalculator.kt), [GaugeViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/gauge/GaugeViewModel.kt), [IncreaseDecreaseCalculator.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/IncreaseDecreaseCalculator.kt), [CastOnCalculator.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/CastOnCalculator.kt), [YarnEstimator.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/YarnEstimator.kt), [InstructionParser.kt](app/src/main/java/com/finnvek/knittools/domain/calculator/InstructionParser.kt).

### Insights

Insights combines completed sessions, projects, and completion events into a single `InsightsUiState`. Repository flows expose loading explicitly; seeded empty lists cannot prematurely replace the skeleton. Heavy aggregation runs upstream on injected IO. Basic totals, exact pace, completion counts/history, and the project activity fabric are data views; trend/charts and streak have specific gates rather than hiding all Insights.

The range selector offers All Time (default), This Week, and This Month; the project pill filters independently of Projects folders. Session metrics use exact seconds/net rows and each session's zone, including cross-midnight date/pace splits. Row totals use wider arithmetic. Empty ranges omit hero/stats/trend; absent completions omit that section.

| Range | Chart buckets |
| --- | --- |
| This Week | Locale-first-weekday through today, daily |
| This Month | First day of current month through today, daily |
| All Time, no first session | Current-month daily axis |
| All Time, under two calendar months | Daily from first session through today |
| All Time, two through six calendar months | Locale-aligned weekly buckets, maximum 26 |
| All Time, beyond six months | Monthly buckets, maximum 12 |

Zero intervals remain in the axis; sparse work is not compressed into adjacent active-only columns. A meaningful chart needs positive total minutes and more than a single active bucket; `INSIGHTS_CHARTS` determines chart versus Pro prompt. Week/Month trend compares the matching previous period only if the baseline reaches 30 minutes, with integer-rounded percent change. Streak is independent: consecutive local active dates ending today or yesterday, plus best historical streak, gated by `STREAK`.

The chart supports selection, tap/horizontal drag, previous/next accessibility actions, and a selected baseline marker. Labels are sparse (day maximum 5, week 4, month 6); today clearance applies beyond seven buckets. The 26-week project fabric uses locale week starts, code-drawn texture, and multiple project strips within an active date rather than one collapsed project color. Completion events include repeated complete/reopen cycles and use the same range/project filters; their timeline follows `INSIGHTS_CHARTS`.

Sources: [InsightsViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsViewModel.kt), [InsightsSessionAccumulator.kt](app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsSessionAccumulator.kt), [InsightsChartModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsChartModel.kt), [InsightsProjectFabricModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsProjectFabricModel.kt).

### Widgets and settings

**Widget.** The Glance home-screen widget shares application-owned project state and canonical repository counter semantics. Registered responsive sizes are 120×48, 120×80, 160×160, and 300×160 dp. Small is horizontal unless height is at least 80 dp, then stacked; medium/large show project header, optional target, count, clamped progress, and 48 dp +/- controls. Large requires both 300×160 dimensions; medium requires both 160×160. Progress changes to tertiary at target reached.

Actions load state, wait for initial trial/billing readiness before the `WIDGET` gate, apply the repository mutation, reread the project, and synchronize all instances. Missing/completed projects yield empty/fallback state. Receiver asynchronous completion is guarded; refresh failure is best effort, not rollback of a committed counter change. Launch uses a one-time app-local counter route token, not authorization from a raw project-ID extra.

**Settings.** General contains Language, Theme, Haptic Feedback, Keep Screen Awake, and Imperial Units. Pro contains current status/upgrade and Restore Purchases when not already purchased. Data contains Backup. Info contains Help and Guide, privacy summary, optional usage analytics, and app version. The default theme is Light, language System, haptics enabled, keep-awake disabled, imperial disabled, completed hidden, analytics disabled. System theme is a selectable mode; no dynamic-color theme is implemented.

**Play review/update.** [InAppReviewManager.kt](app/src/main/java/com/finnvek/knittools/pro/InAppReviewManager.kt) counts selected successful counter/work actions locally and becomes eligible after 20 actions if no review was requested. Main Activity observes eligibility, persists the request flag before requesting Play review, and resets it if requesting review info fails; cancellation propagates. Play decides whether its review UI actually appears. [InAppUpdateManager.kt](app/src/main/java/com/finnvek/knittools/pro/InAppUpdateManager.kt) checks flexible updates at activity creation and downloaded updates on resume, guards duplicate flows/listener notifications, and presents an indefinite Restart snackbar after download. Restart completes the update; failure can notify again. Activity destruction unregisters the listener. Neither integration is a verified store interaction in this audit.

Sources: [CounterWidget.kt](app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt), [CounterWidgetActions.kt](app/src/main/java/com/finnvek/knittools/widget/CounterWidgetActions.kt), [SettingsScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/settings/SettingsScreen.kt), [PreferencesManager.kt](app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt).

## Navigation and external entry points

### Graphs and route inventory

[Screen.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt) defines routes and `TopLevelDestination`; [NavGraph.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt) registers destinations and arguments. `TopLevelNavigation` centralizes tab switching and restore reset. Required positive entity IDs are checked before resolving content; unavailable/deleted entities pop or return to the owning tab instead of rendering stale data.

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
| `tools` | Tools | Calculator/reference launcher |
| `increase_decrease` | Tools | Increase/decrease tool |
| `cast_on` | Tools | Cast-on tool |
| `yarn` | Tools | Yarn Estimator |
| `needles` | Tools | Needle conversion/reference |
| `size_charts` | Tools | Measurement categories |
| `abbreviations?craftType={craftType}` | Tools; default `KNITTING`, domain fallback | Craft-context abbreviation reference |
| `chart_symbols` | Tools | Chart-symbol reference |
| `insights` | Insights | Aggregated metrics/filter/chart state |
| `settings` | Settings | Preferences, Pro, data, information |
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
- Incoming-PDF and Library attachment success open the overview. The web editor's Open Project completion currently uses the internal counter-launch path. Do not assume every attachment-related action has the same destination.

### Activity intents and widget launch

`MainActivity` is an exported single-top activity with launcher, PDF Open/Share, and `ACTION_SEND text/plain` filters. `onNewIntent` routes through the same incoming handlers; the consumed PDF intent is reset to MAIN and stream/type/data cleared to prevent replay. There is no registered native Ravelry OAuth completion URI.

PDF acceptance checks explicit/resolved MIME for VIEW, or PDF SEND with an actual stream. Text sharing is bounded and parsed before a browser/editor decision. App-local counter launches use `CounterLaunchRequest` / `CounterLaunchTokenStore`: a one-time stored token admits an app-created launch request, and the requested project must resolve through application state. The token store does not encode a project-ID binding. Raw externally supplied project-ID/widget extras are not treated as sufficient authorization. Tokens are navigation aids, not public API credentials.

Sources: [MainActivity.kt](app/src/main/java/com/finnvek/knittools/MainActivity.kt), [TopLevelNavigation.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/TopLevelNavigation.kt), [CounterLaunchRequest.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/CounterLaunchRequest.kt), [CounterLaunchTokenStore.kt](app/src/main/java/com/finnvek/knittools/data/storage/CounterLaunchTokenStore.kt), [AndroidManifest.xml](app/src/main/AndroidManifest.xml).

## Visual system and accessibility

### Identity and theme ownership

The implemented visual identity uses warm parchment/light olive surfaces or dark olive surfaces, an orange main action, condensed readable type, stitch graphics, and fabric textures. Current layouts take precedence over the older Fable/Projects design plans. [CLAUDE.md](CLAUDE.md) supplies product/visual guidance; code owns actual rendering.

[KnitToolsTheme in Theme.kt](app/src/main/java/com/finnvek/knittools/ui/theme/Theme.kt) provides Material 3 plus project-specific extended colors. Light is the preference default; Light, Dark, and System are selectable. Colors are fixed project palettes, not wallpaper-derived dynamic color. Use semantic tokens instead of scattering raw colors.

### Palette and semantic colors

[Color.kt](app/src/main/java/com/finnvek/knittools/ui/theme/Color.kt) and `Theme.kt` are the palette/mapping authorities. The following values are hexadecimal sRGB, not contrast certification.

| Token / role | Dark | Light |
| --- | --- | --- |
| Background | `#1E1E12` | `#E8E4D0` |
| Background alternate | `#252518` | `#DDD8C3` |
| Surface | `#2E2E20` | `#D2CDB5` |
| Surface high | `#3A3A2A` | `#BBB59A` |
| Surface medium-high | — | `#C8C3A8` |
| Surface highest | `#454535` | `#A49D80` |
| Primary action | `#C45100` | `#C45100` |
| Primary container | `#D4722A` | Same primary-container token |
| On primary | `#FFFFFF` | `#FFFFFF` |
| On accent | `#161610` | `#161610` |
| Secondary | `#93AE4F` | `#394B18` |
| Secondary muted | `#6B8A35` | `#5A7525` |
| Secondary container | `#3A4020` | `#D0DDB5` |
| Tertiary | `#C9A435` | `#9A7B18` |
| Tertiary container | `#3A3520` | `#E8DFB5` |
| Primary text | `#E8E4D0` | `#2E2A1E` |
| Secondary text | `#B8B4A0` | `#4C4634` |
| Muted text | `#A8A491` | `#4A473C` |
| Disabled text | `#5A5840` | `#C0BAA5` |
| Dusty rose | `#B8908F` | `#9E706E` |
| Section label | Dusty rose | `#7F5452` |
| Error | `#C44D4D` | `#C44D4D` |
| Error container | `#3A2020` | `#EAD0D0` |
| Success | `#8BA44A` | `#8BA44A` |
| Success container | `#3A4020` | `#D0DDB5` |
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
| Counter minus icon | Theme-mapped icon | `#211E16` |

`selectedCardContainer` uses primary at 7% alpha. Table header uses dark surface-highest/light card container. Activity intensities proceed through low, secondary, tertiary, primary-container colors. Material foreground/background mappings and extended tokens are separate; e.g. the light secondary is a deliberately dark foreground.

The deterministic project/yarn palette in `YarnColorPalette` has eight slots. Dark: `#C45100`, `#8BA44A`, `#C9A435`, `#B8908F`, `#9A6B4A`, `#5A8A7A`, `#9A82AA`, `#A85A3A`. Light: `#C45100`, `#70843C`, `#957927`, `#A3706E`, `#9A6B4A`, `#578576`, `#8E73A0`, `#A85A3A`. Entity-derived stable selection makes color consistent across project/yarn/fabric views. Static color tests do not prove a complete rendered screen passes contrast requirements.

### Typography, shapes, dimensions, and assets

[Type.kt](app/src/main/java/com/finnvek/knittools/ui/theme/Type.kt) uses **Barlow Semi Condensed**, not Outfit. Five bundled static TTF weights are Regular 400, Medium 500, SemiBold 600, Bold 700, ExtraBold 800, paired with system sans fallback. The SIL OFL license is [barlow_semi_condensed_ofl.txt](app/src/main/assets/licenses/barlow_semi_condensed_ofl.txt).

| Material typography | Size / weight | Letter spacing where explicit |
| --- | --- | --- |
| Display large / medium / small | 57 Bold / 45 Bold / 36 SemiBold sp | Large `-0.25 sp` |
| Headline large / medium / small | 32 Bold / 28 SemiBold / 24 SemiBold sp | Default |
| Title large / medium / small | 22 SemiBold / 16 SemiBold / 14 Medium sp | Medium `0.15`, small `0.1 sp` |
| Body large / medium / small | 16 Regular / 14 Regular / 12 Regular sp | `0.5` / `0.25` / `0.4 sp` |
| Label large / medium / small | 14 SemiBold / 12 SemiBold / 11 SemiBold sp | `0.1` / `0.5` / `1.5 sp` |

Project-card name is 18 sp; hero name 22 sp Bold; extra-counter name 17 sp and value headline-medium Bold. Project names are never uppercased. `SectionLabel` uppercases ordinary section labels; `ProjectNameLabel` preserves user content. Main counter fits from 115 sp down to 48 sp with bounded fitting iterations. Insights hero is 76 sp ExtraBold (minimum 52), secondary 46 Bold, units 26/20; stat values 20 Bold (minimum 14), axis 13. Width fitting is implemented behavior, not proof that all locale/font combinations remain comfortably readable.

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
| Extra-counter card | Minimum 88 dp, radius 16, padding 22×18, value column 74 |
| Extra +/- and overflow | 56 dp touch / 42 visual; overflow 48 |
| Overview | Sides 16, photo hero 210 / no-photo 96, rows 56, radius 24 |
| Overview yarn/steppers | Swatch 48, stepper visual 44 / touch 48 |
| Content glyph / empty state | Row glyph 28, empty graphic 96, padding 32, gap 24 |

Counter images (`counter_plus.webp`, `counter_minus.webp`, `counter_undo.webp`, `continue_button.webp`) and splash artwork are density-independent raster assets. Launcher assets have density variants. `CrossStitchIcon`, `CrossStitchGlyph`, `CrossStitchDimens`, and accent mapping share project-content glyphs; `FabricSwatch` draws knitting/crochet/yarn texture in code. These graphics are not a separate remote asset service.

### Shared component contracts and screen hierarchy

- Every standard sheet uses the theme surface and `SheetTitle`. Project actions is the explicit exception whose title is a project card. Choices use `SheetOptionRow`; form actions use `FormSheetActions` and a filled confirmation button.
- `LabeledTextField` and `NumberInputField` use labels above the same frame. `ProjectYarnForm` is a shared `FormSheet`; usage uses `SheetTitle`, segmented units, and shared form actions. Unit text is not duplicated in labels and values.
- Plain confirmations use `ConfirmationDialog`; destructive delete/replace selects destructive styling. Custom AlertDialog remains for forms/multiple choices. `CancelButton` is always neutral; Material-dialog confirm actions are text buttons.
- `RowOverflowMenu` holds applicable row actions; completed session rows offer Delete only. `SelectionActionBar` is one low row of text actions with red delete. `ProjectFilterPill` / `FilterMenuItem` are shared by Insights and Library Photos. FilterChip is restricted to the PDF annotation palette.
- Project cards use `ProjectCard` in a shaped Surface, with thumbnail, two-line name, optional section/pattern line, progress/completion/session state, and selection. They open overview and contain no counter button. `ContinueProjectCard` is the shared working-entry card. The old cardless `ProjectListItem` description is obsolete.
- Counter gives the unit/large number/target and image-backed +/-/Undo first, with secondary/stitches/session and content tools below. Overview mode uses `ProjectOverviewScreen`/sections for management and the shared Continue progress card. Its small-tile grid reduces columns below 340 dp; workspace session actions stack below 360 dp.
- Library is a destination hub with focused lists/details, not a Projects-folder view. My Yarn places status under the name and usage actions inside the card. Tools uses calculator input/result components and text actions such as Choose from My Yarn. Gauge choices and Size Chart categories use `SegmentedToggle`.
- Insights uses range/project filters, hero, compact stats/trend, chart, completion data, and activity fabric according to available data. Settings is divided into General, Pro, Data, and Info; backup belongs to Data. Notes has only the Notes top-bar title. PDF labels are cleaned by `PatternDisplayNames`.

`UiConsistencySourceTest` enforces exact component exception lists. Source-contract enforcement prevents certain structural regressions; it does not verify screenshot geometry or interaction on a device.

### Responsive layout, semantics, focus, and feedback

Project-card compact mode applies at width ≤320 dp or font scale >1.3. `SegmentedToggle` uses radio-button roles/selectable grouping and minimum 48 dp targets; more than three options becomes a two-column grid. At font scale ≥1.5, its shape/expanded multiline labels adapt, but a two/three-option selector does not become a grid just because the font is larger. Gallery image import uses a SAF fallback when width <360 dp **and** font scale ≥1.5.

Bottom navigation has five icon/uppercase-localized labels, theme container, primary selected color, and inactive semantic colors. Its Material indicator is the container color, despite an active-background token existing. Label measurement considers translated labels and reduces size in 0.5 sp steps down to its configured minimum, then uses one-line ellipsis. This is a source mechanism, not a large-font legibility guarantee.

Image-backed controls expose Button roles and localized descriptions; decorative image/glyph descriptions are null. `LabeledCounterImageButton` keeps the clickable action before `clearAndSetSemantics` so the combined label/Pro badge does not remove activation. Form fields use keyboard/IME and focus handling through their shared components; route changes, sheet closure, and picker outcomes are state-owned. Custom chart selection exposes next/previous actions. Gesture-based PDF editing, viewport focus after bookmark jumps, and system Back require runtime testing beyond semantics inspection.

Counter image press feedback animates in 90 ms with scale/translation (up to 1.8% shrink and 1.5 dp shift), and disabled alpha 0.62. Shared result transitions use 6 dp displacement and 200/150 ms enter/exit constants. Startup considers system animation availability. A universal custom reduced-motion/focus-ring implementation has not been established by this inspection.

Root `PreferenceAwareHapticFeedback` gates Compose/default long-press haptics using the DataStore preference; explicit counter vibration is also preference-gated. Keep-screen-awake is opt-in. The activity currently requests portrait orientation. Splash styling is fixed by Android theme resources while app preference/theme initialization resolves; it is not evidence of an independently themed night splash.

Required UI validation for future changes must explicitly cover **320 dp**, **200% font scale**, **their combination**, both themes, long translations, TalkBack/focus traversal, keyboard/IME, scrolling and touch targets. None of those runtime checks was executed during this documentation task. Tests/configuration are catalogued below without converting existence into proof.

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

### Coroutines, Flow, state restoration, and concurrency

[DispatchersModule.kt](app/src/main/java/com/finnvek/knittools/di/DispatchersModule.kt) provides `@IoDispatcher` for boundary-crossing blocking work and `@ApplicationScope` with `SupervisorJob` and Main dispatcher. Billing/Pro own their process-lifetime state scopes. ViewModels use `viewModelScope` for screen work; application scope is reserved for final persistence/cleanup that must outlive the ViewModel, with injected IO still selected for blocking operations.

UI-facing Room flows apply `retryOnRepositoryReadFailure` **after mapping**. The retry boundary preserves cancellation and caps backoff; a read or mapper failure cannot permanently terminate state sharing. Loading is represented explicitly where empty defaults would be misleading. Typed operation results distinguish validation/stale/missing/entitlement/storage failures; expected rejection is not an invented success.

`SavedStateHandle` owns transient/restorable selection/drafts such as folder filters, Gauge numeric canonical state, pending imports and share requests. `rememberSaveable` handles appropriate presentation state, such as Library guides. Room owns project/session/document truth. Persistable provider permission is not permanent app-owned storage: transient selected images/PDFs must be copied where required.

Selected project and document changes use context/generation guards so delayed work cannot write the new selection using old draft data. Session actions use tokens; metadata attachment uses expected IDs; usage updates use revision checks. Mutexes coordinate pending PDF import, file-reference/restore operations and yarn-photo changes. These locks serve different boundaries; they are not a substitute for Room transactions.

[DatabaseTransactionRunner.kt](app/src/main/java/com/finnvek/knittools/data/local/DatabaseTransactionRunner.kt) coordinates multi-DAO repository mutations. Main counter/history/link/session/reading writes, bidirectional yarn links, document ownership, project admission, completion events, and replacement restore each keep their stated atomic boundary. File operations cannot join a SQLite transaction: cleanup inputs are captured first, database mutation commits first, then unused files are deleted best effort.

**Database failure or pre-commit cancellation leaves files intact**. A post-commit cleanup failure may leave an orphan but must not present the committed deletion as rolled back. `SavedPatternRepository.deleteLocalPatternFileIfUnused` is the shared-PDF deletion gate, coordinated with file-reference operations. Backup provider worker ownership and cancellation have additional limits described in the backup section.

## Persistence reference

### Room ownership and constraints

[KnitToolsDatabase.kt](app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt) declares schema 25 and 18 entities. [DatabaseModule.kt](app/src/main/java/com/finnvek/knittools/di/DatabaseModule.kt) opens `knittools.db`, registers every manual migration plus Room auto-migrations, and installs constraints. Destructive fallback is not configured. Exported JSON schemas 1–25 are tracked in [app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase](app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase). Version 25's identity hash is `9e9baa96c63eedd96a597d2ce371a893`.

| Table | Entity / DAO | Canonical writer / meaning |
| --- | --- | --- |
| `counter_projects` | `CounterProjectEntity` / `CounterProjectDao` | `CounterRepository`; project lifecycle, main/secondary/stitch counts, notes, compatibility reader fields |
| `counter_history` | `CounterHistoryEntity` / `CounterProjectDao` | Main-counter transaction and validated Undo |
| `yarn_cards` | `YarnCardEntity` / `YarnCardDao` | `YarnCardRepository`; stash metadata/quantity, photo and reciprocal links |
| `sessions` | `SessionEntity` / `SessionDao` | `CounterRepository`; completed exact session records |
| `active_sessions` | `ActiveSessionEntity` / `SessionDao` | `CounterRepository`; one active session, timing/recovery/token anchors |
| `row_reminders` | `RowReminderEntity` / `RowReminderDao` | Counter repository flow; project in-app reminders |
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

The table dictionary below reproduces column affinity, nullability, default, key, index, and FK metadata from the current exported schema. Kotlin booleans/enums/timestamps can use INTEGER/TEXT storage; affinity alone does not replace domain validation. Auto-generated primary IDs are distinct from the active-session singleton or project-keyed folder assignment.

**Additional installed constraints not fully expressed by the schema JSON:**

- `ActiveSessionSchemaConstraints` insert/update triggers require singleton ID 1; project deletion cascades its active row.
- Annotation layers require exactly one owner (project XOR saved pattern); owner+document-key uniqueness and triggers restrict a project to at most one active layer. Layer deletion cascades annotations.
- `ProjectDocumentSchemaConstraints` prevents multiple primary documents. Repositories maintain the stronger invariant: exactly one primary if the project document set is nonempty.
- Folder assignments are keyed by project ID; folder deletion cascades assignments, leaving projects unfiled. Name uniqueness is stored in `normalizedName` and enforced by a unique index.
- Usage source indexes are unique per project/card and project/note; both nullable source FKs use `SET NULL`, so deleting source metadata preserves usage. Repository identity additionally coalesces a note and its saved card.

Project `linkedPatternId`, yarn `linkedProjectId`, project yarn-ID CSV, and note `savedYarnCardId` include soft relationships that must be maintained by repository transactions. Do not assume every non-null ID has an FK. Saved-pattern indexes for Ravelry/URLs/PDF are lookup indexes, not a comprehensive SQL uniqueness policy.

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

### Migration chain

Room exports every version 1–25. Versions 1→2 and 2→3 use declared auto-migrations; subsequent migrations are explicitly registered. Schema snapshots, migration implementation, constraint callbacks, and migration tests must change together for new persistent state.

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

Migration 24→25 requires a credible completion timestamp at/after creation; it does not invent all historical completion cycles. Session null zones and completion null zones keep the established current-device-zone fallback. Migration 18→19 must preserve tables that reference the rebuilt saved-pattern table; a successful table rebuild without those rows is not a correct migration.

Useful deterministic ordering includes document `sortOrder,id`, extra-counter `sortOrder,id`, bookmark `pageIndex,yFraction,createdAt,id`, descending session `startedAt,id`, and descending history timestamp/ID. Do not silently substitute composition order or mutable names for persistent tie-breakers.

### Preferences and other persisted state

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
| Dismissed tooltips | Empty string set; local UI assistance state |
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

### Image creation and photo replacement

`PatternImageImportViewModel` owns ordered/restorable pending image state; storage deduplicates selected source URIs and immediately copies accepted images. Default is Android Photo Picker; when width <360 dp and font scale ≥1.5, `OpenMultipleDocuments` supplies the explicit accessibility fallback. URI permissions are not saved as durable project image references.

Pattern-image limits are 20 pages, 25 MiB per image, 200 MiB per session, 1,800-pixel decoded long edge, and 32 MiB free-space reserve. Decode is sampled/bounded and pages are rendered sequentially; animated content is rejected and alpha becomes white. Generated PDFs attach only through `CounterRepository`. Stale capture pruning considers only credible positive/nonfuture timestamps older than seven days.

`ProgressPhotoStorage` samples bounded images to a 1,920-pixel edge/JPEG 80. `YarnPhotoStorage` copies raw source bytes into a private `.jpg` path, bounded to 25 MiB and reserve 32 MiB; the filename is not a guarantee of transcoding. Yarn replacement deletes a failed new copy, commits the new URI before old deletion, and serializes swaps/pruning/backup coordination. `App.onCreate` schedules unreferenced yarn-photo pruning for interrupted swaps.

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

### Annotated export budgets and failure behavior

The exporter uses a source-side ceiling of 14,400 pixels, maximum 100 pages, rendered bitmap side 1,800, at most 3.24 million pixels/page and 48.6 million total. Annotation export caps include 2,000 annotations, 16 MiB payload, 250,000 work units, and 20,000 tracker work units. The temporary PDF is capped at 200 MiB, with 32 MiB reserve (232 MiB combined admission requirement).

Rendering one bitmap at a time bounds bitmap lifetime; Android `PdfDocument` still retains its page representation until writing completes. It is not a fully streaming PDF rewrite. Export uses SAF CreateDocument, a complete cache temp, then provider copy. Temp cleanup occurs on success/failure/cancellation; provider-side partial output can remain. Source PDF, read-only master ownership, guides/bookmarks, and original text/vector fidelity have the distinct behaviors described above.

### Deletion and reference safety

Project deletion, document removal, saved-pattern deletion, and pattern detach collect app-owned cleanup references before the authoritative database transaction. A shared PDF is deleted only when Library, project documents, and still-relevant legacy project URIs no longer reference it. Deleting an annotation layer is a database ownership action, not permission to remove a shared source file.

Physical cleanup is best effort after commit; failures leave orphans for the owning cleanup/recovery path rather than misreport rollback. Failed attachment/import cleans the unattached copy when possible. Progress-photo pruning, yarn-photo pruning, capture-session pruning, shared-PDF deletion, and backup-journal recovery have separate rules; there is no universal filesystem sweep that is safe for every directory.

## Current Ravelry integration

### Reachable Android behavior

The current Android integration is [RavelryBrowserScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryBrowserScreen.kt), [RavelryLinks.kt](app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryLinks.kt), and [WebPdfDownloader.kt](app/src/main/java/com/finnvek/knittools/data/remote/WebPdfDownloader.kt). Entry points include Tools → Ravelry, Import from Ravelry, shared Ravelry links, the web-pattern editor's Ravelry link, and Saved Pattern → Open in Ravelry.

A start page must pass `ravelryPageUrlOrNull`: trimmed HTTPS with host exactly `ravelry.com` or `www.ravelry.com`; otherwise the default is `https://www.ravelry.com/patterns/search`. This helper is not the stronger general `WebPatternUrl` validator and does not independently enforce all credential/port restrictions. Shared pattern classification is stricter (HTTPS/default port and `/patterns/library/<slug>`).

Users sign in on Ravelry's own website inside WebView. Website cookies/DOM storage retain the session. There is no app-owned Ravelry account, Android Firebase anonymous-auth step, OAuth token exchange, credential extraction/JavaScript bridge, or backend pattern-search/import UI. Previously saved Ravelry metadata and availability remain readable. Current Android Firebase usage is Crashlytics only.

### Navigation, settings, and accepted boundary

WebView enables JavaScript and DOM storage, accepts website cookies, disables file/content access, and enables WebView debugging only in debug builds. It flushes cookies/destroys its view on release. HTTPS is mandatory for in-browser navigation/downloads; cleartext is disabled at manifest level.

Navigation blocks non-HTTPS. Checkout paths/subdomains and PayPal, Stripe, Braintree Gateway/API hosts go to an external browser, including purchase start URLs. A normal nonredirect main-frame link to a non-Ravelry host also opens externally. **HTTPS server redirects and subframes can remain in the WebView**, including hosted document paths. Do not describe this as a universal Ravelry-only host allowlist or a comprehensive private-network/redirect firewall. Navigation matching accepts Ravelry subdomains while initial-page validation accepts only the two exact hosts above.

The browser does not implement a native Clear Ravelry Account/Disconnect flow; website session handling is distinct from removed backend disconnect. Cookies are not included in manual backups. Page content and third-party website behavior remain external. [config/security-decisions.md](config/security-decisions.md) records the in-app browser/download decision and the requirement to obtain written Ravelry permission before Play release; this inspection does not establish that permission has been obtained.

### PDF downloads into Save Pattern

WebView's DownloadListener accepts a PDF MIME or `.pdf` filename guess and starts a website download with the user agent, content disposition, and cookies selected for each exact URL. The app then verifies the copied PDF instead of trusting the extension alone. Downloads go straight into the Save Pattern flow, avoiding the phone Downloads-folder behavior that motivated replacing Custom Tabs.

`WebPdfDownloader` uses injected IO and `HttpURLConnection`, HTTPS on every hop, up to five redirects, per-hop connect/read timeout 30 seconds, 64 KiB chunks, and **100 MiB** maximum actual bytes. Relative redirects resolve against the current URL. Arbitrary HTTPS download hosts can pass; there is no Ravelry-host restriction on every redirect. Cookies are fetched per URL rather than forwarding one header to all hosts. Per-hop timeouts are not a 30-second whole-operation deadline.

Temporary `cache/web_pdf_downloads/pattern-*.pdf` files are removed after copy or failure/cancellation; connection cleanup and chunk-level cancellation are explicit. The validated private copy follows Incoming PDF save/attach/library logic. No backend API PDF download, scraping bypass, or automatic entitlement to a paid pattern is implemented.

### Retained backend, disconnected Android integration, and future status

The repository still contains a substantial Firebase Functions v2 Ravelry backend. It is retained code, not a currently reachable Android feature. Since the 2026-10-09 change, Android no longer has `RavelryAuthManager`, `RavelryBackendClient`, `RavelryRepository`, old search/detail/import screens, Firebase Auth/Functions dependencies, or `knittools://ravelry-auth-complete` manifest handling. Retained backend constants/protocols mentioning that URI do not register an Android route.

[functions/src/index.ts](functions/src/index.ts) exports eight callables plus one HTTP callback: `ravelryStartAuth`, `ravelryCallback`, `ravelryCompleteAuth`, `ravelryAuthStatus`, `ravelryDisconnect`, `ravelryCurrentUser`, `ravelrySearchPatterns`, `ravelryImportPatternById`, and `ravelryImportPatternByUrl`. Functions target `europe-west1`/Node 22. Firebase deployment/rules/secrets/TTL readiness is not verified by their presence.

Retained implementation details relevant if maintaining the backend:

- PKCE OAuth state and hashed browser-start ticket, HttpOnly/Secure/SameSite=Lax browser cookie, single-use state, generation-bound token writes, and bounded HMAC completion proof. State/completion lifetime is ten minutes. Start creates state; callback alone does not activate a token without authenticated completion.
- Firestore `ravelryOAuthStates/{state}` and `ravelryTokens/{uid}`; `connectionGeneration` guards callback/completion/refresh against disconnect tombstones. `tokenAccess.ts` handles server-side expiry/refresh and rotated refresh tokens after owning limiter admission.
- Per-UID/minute limits: auth/callback/disconnect 10, search 30, import 20. Backend-global fixed windows: corresponding auth/callback/disconnect 60, search 120, import 80, using ten shards. Active legacy `<bucket>_global` windows are still checked for rollout compatibility; cleanup requires confirming all legacy writers are drained. Warm-process saturated-window caching avoids rescanning confirmed saturation.
- Callback global admission precedes state lookup; known-owner UID admission follows. There is no IP-keyed quota. One actor may still consume global quota/backend work; source limits are not proof of production cost behavior.
- Search accepts bounded query/filter/difficulty/page/page size; import validates positive safe Ravelry IDs or canonical exact pattern URLs. API calls refuse redirects, time out at ten seconds, and bound token/API response bodies (64 KiB/1 MiB). Sanitized metadata has bounded title/designer/user/permalink/HTTPS thumbnail fields; no PDF is downloaded.
- [firestore.rules](firestore.rules) denies client access to backend-owned collections. [firestore.indexes.json](firestore.indexes.json) defines OAuth-state queries. [firebase.json](firebase.json) describes build/deployment; configuration is not deployment proof.

OAuth client ID/secret belong in backend Secret Manager, never Android or this document. Reintroducing Android API search/auth or continuous sync requires a new explicit product/security decision. Existing backend plans describe historical implementation work; they are not automatic authorization to reconnect it.

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
| `PATTERN_CAMERA_SCAN` | Photo-to-PDF camera flow; name is an internal enum, user copy says photo/PDF |
| `INSIGHTS_CHARTS` | Meaningful activity charts and completion timeline |
| `STREAK` | Current/best streak access |
| `UNLIMITED_YARN` | New Library yarn-card creation; no numeric free-card allowance |

Ordinary main counting, basic session/completion data, folders, web-pattern metadata, project usage, PDF import/reading, bookmarks, and local backup are not globally hidden behind Pro. Annotation code inspected here has no separate annotation Pro gate. Regex paste checks `isPro` directly; a debug feature override does not automatically turn that property into purchased/trial state.

Existing data is retained when trial/purchase access ends. Notes/secondary access specifically uses persisted monotonic flags; existing counters, yarn cards and stored content remain distinct from creating new gated content. Reactivation/new-project limits are still repository-enforced. A UI Pro badge or an already-open sheet is not enforcement.

[ProManager.kt](app/src/main/java/com/finnvek/knittools/pro/ProManager.kt) combines trial/billing readiness and state. `hasFeatureAfterInitialLoad` waits for Pro and Billing initial readiness (each bounded at two seconds) before widget decisions, avoiding failure from the default pre-load state. A timeout remains a real uncertainty/failure path, not live Play proof.

### Trial persistence and clock handling

[TrialManager.kt](app/src/main/java/com/finnvek/knittools/pro/TrialManager.kt) implements a **14-day, user-started** local trial. It is not automatically activated by installation or backed by a server account. Preferences DataStore `trial_state` stores:

`trial_start_timestamp`, `last_known_timestamp`, `clock_tampered`, `trial_end_notice_shown`, `trial_elapsed_duration_millis`, `trial_anchor_wall_clock_millis`, `trial_anchor_elapsed_realtime_millis`, and `trial_anchor_boot_count`.

A valid time snapshot has positive wall time, nonnegative elapsed realtime, and nonnegative available boot count. Clean unstarted legacy state can start only from a valid snapshot. Corruption writes tampered=true; legacy active timing or malformed partial anchors fails closed rather than trusting a reset trial.

Same-boot timing advances with nonnegative elapsed-realtime delta. A later boot uses nonnegative wall delta plus one hour per boot-count increment, accepted only when consistent with elapsed realtime. Decreasing/missing boot identity, reversed/inconsistent anchors, overflow, and wall rollback greater than one hour against last-known/start tamper-lock entitlement. `daysRemaining` uses ceiling days; active requires remaining duration and no tamper flag. Refresh delay is the smaller of 15 minutes and next elapsed-day boundary, then 15 minutes after expiry.

There is no general trial-key cleanup routine; evaluations overwrite persisted state, with absent boot anchor removed when appropriate. Manual backup excludes these keys. App reset/reinstallation/Android storage policy and live Play ownership are separate from this local implementation; do not claim server-enforced perpetual trial identity.

### Google Play purchase and restore

[BillingManager.kt](app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt) queries the **one-time INAPP product `knittools_pro`**. There is no subscription tier or hard-coded price. UI uses Play `formattedPrice`; offer selection excludes rental/preorder offers, prefers current valid non-limited offers with deterministic ID/price selection. Pending one-time purchases are enabled.

The Billing client connects asynchronously, enables automatic reconnection, and caps explicit connection attempts at three with two-second spacing. Initial purchase readiness has a two-second wait boundary. Restore queries owned INAPP purchases and returns Restored, Not Found, or Failed. Matching PURCHASED records unlock local entitlement immediately, before acknowledgement; PENDING emits pending state without unlocking.

Acknowledgement requires a matching purchased, unacknowledged record with a nonblank token. Retryable network/disconnected/unavailable/error/item-not-owned responses requery purchases with up to three retries five seconds apart; nonretryable failures stop. Ack failure does not immediately remove in-memory entitlement. ITEM_ALREADY_OWNED triggers requery; empty/failing restore emits an explicit already-owned restore failure.

A later successful owned-purchase query recomputes entitlement from current PURCHASED records, so absence can remove purchased access. There is no backend purchase-validation/refund-notification service. Product availability, price, Play Console configuration, purchase/acknowledgement, pending completion and revocation behavior were not tested live during this task. Play Review/Update integrations are separate from Pro purchase enforcement.

Main Activity waits for initial launch/Pro readiness before claiming a one-time passive trial-ended notice. Explicit Pro/widget/share/counter launches suppress that passive notice so it does not compete with the requested flow. Continue Free dismisses it; See Pro opens the upgrade flow. Notice claiming is persisted through `TrialManager`, not inferred from every recomposition.

## Manual backup and replacement restore

### Workflow and archive contract

[BackupScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/backup/BackupScreen.kt) and [BackupViewModel.kt](app/src/main/java/com/finnvek/knittools/ui/screens/backup/BackupViewModel.kt) expose user-selected SAF export and import preview. Confirmed restore **replaces all current project data**; it is not a merge. Preview/confirmation state is separate from a live mutation.

[BackupRepository.kt](app/src/main/java/com/finnvek/knittools/repository/BackupRepository.kt) coordinates [data/backup](app/src/main/java/com/finnvek/knittools/data/backup). `.knittools-backup` is an `application/octet-stream` ZIP with format version 1, data version 1, current schema 25, `manifest.json`, 18 `tables/<table>.jsonl` streams, and content-addressed `files/<sha256>.bin` entries. Table streams use ordered column headers and primitive row arrays. It contains logical records, not a copied SQLite database/WAL.

The manifest identifies KnitTools format, creation time, app version, schema, and bounded file paths/sizes/hashes. Structural allowlists, duplicate/path checks, ZIP/ZIP64 local/central inventory agreement, trailing-data/decompression/depth checks, declared budgets, hashes, row validation, and a shadow Room database reject invalid archives before confirmation/replacement. App version is informational; other format/data/schema versions need an adapter, currently absent. SHA-256 detects inconsistent bytes but supplies no cryptographic authenticity. The archive is **unencrypted and unauthenticated**; project notes/photos/PDFs are readable by anyone who can access the file.

Included: all 18 tables, durable referenced PDFs, progress photos and yarn photos, relationships, annotation payloads, mappings/bookmarks, folders/usage/completions, and active-session recovery state. Excluded: preferences/language/theme/consent/tooltips, trial/Play state, credentials/WebView cookies, widget configurations/launch tokens, caches/pending capture sessions, diagnostics, and raw SQLite files. Destination settings remain destination-owned.

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

Limits are admission/streaming checks, not promises that every device can complete a maximum-sized backup. Native session writer and backup reader share the session ceiling. Validation includes foreign/soft links, enums, payload/geometry/mapping budgets, file references, and Room constraints; filenames alone do not establish a valid archive.

### Export snapshot and provider IO

Export freezes/stages external content before the locked database/private-file snapshot. This is **not one simultaneous snapshot across arbitrary external providers and Room**. A newly referenced external file not prestaged causes failure rather than being silently omitted. Database transaction, `PatternFileReferenceCoordinator`, yarn-photo coordination, and the backup mutex preserve the app-owned boundary.

A fully validated private archive precedes destination copy. The SAF provider can fail/retain a partial output. `BackupProviderIo` reserves a singleton atomic operation across reads/writes, rejects concurrent work before opening a descriptor, and owns its worker plus abort jobs. Daemon provider workers/closers and a watchdog are bounded by operation ownership rather than a broad application thread kill.

The no-progress timeout is **30 seconds per blocking open/read/write/flush/close**. Positive-byte progress resets it; zero-byte reads do not. Stream chunks are 64 KiB. Cancellation signals the provider and closes the descriptor; NonCancellable cleanup waits for owned worker completion before rethrowing. A provider that ignores cancellation/close can retain the reservation until process restart. This is not a guaranteed whole-operation deadline or guaranteed termination of hostile provider code.

### Identity rebasing and replacement transaction

Preview has a fresh selection UUID and stale-action guards. Its approval state is ViewModel memory, not a durable process-restored authorization. Export/validation can be cancelled; replacement does not allow cancellation/Back to abandon a live transaction. Source archive freezes and validates in private staging/shadow Room before confirmation, and must remain valid for the selected operation. Restoration rebases identities above live existing IDs/SQLite sequences instead of reusing old IDs into existing back stacks. It rewrites FKs, soft IDs, yarn CSV, stable document keys, annotation chart-tracker counter IDs and dependent relations coherently.

Restored files receive fresh private paths, including `pattern_pdfs/0/restore-<UUID>-<sha256>.pdf`, rather than overwriting existing files in place. Active sessions receive fresh session/recovery tokens and require review; foreign device elapsed/boot anchors are not accepted as trusted running time. Successful restore clears stale navigation state and refreshes widgets best effort.

Replacement stages new files, writes/fsyncs an atomic `restore-files.json` journal before publication, publishes new files, then clears/imports all live tables and reseeds identity sequences in **one Room transaction**. All required restored app-owned files must exist before committing. Validation/publication/transaction failure before commit preserves old database and files.

After commit, old reference-free files are deleted best effort. Cleanup/journal failure does not roll back committed data; a retained journal is recovered at startup against authoritative live references. Journal recovery must run before independent cleanup can remove files needed to resolve an interrupted restore. UI success and widget refresh are not proof of a remote provider's durability.

### Drive, Dropbox, and future synchronization

Any installed SAF provider may let users manually select a local/cloud file for PDF import or backup. There is no Drive/Dropbox SDK, OAuth, automatic synchronization, conflict resolution service, or cross-device live counter/session sync. Pattern PDF v1 import remains `OpenDocument(application/pdf)` plus grants/private copy.

[config/future-sync-spec.md](config/future-sync-spec.md) records future specification work: manual export/import or backup/restore precedes continuous sync. Background/offline/conflict handling, credentials, provider lifecycle and Pro policy need an explicit new spec. Do not market cross-device sync from a functioning file picker.

## Security, privacy, and telemetry

### Android manifest and external surface

[AndroidManifest.xml](app/src/main/AndroidManifest.xml) requests INTERNET, VIBRATE and CAMERA; camera hardware is optional. Runtime camera permission belongs to photo capture. No broad storage permission is required for SAF/Photo Picker. There is no `RECORD_AUDIO` permission, `SpeechRecognizer`, `TextToSpeech`, conversational voice command, or production microphone flow.

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

[config/posthog.md](config/posthog.md) describes the consent boundary; implementation/catalog take precedence over its older SDK-version wording. `PostHogAnalytics` initializes lazily in release when `usageAnalyticsEnabled` is true (default false), uses the EU endpoint, and handles lifecycle/navigation. `UsageAnalytics` accepts predefined event/allowlisted screen names only, without project content/IDs, URLs, notes, counts/search text or billing state.

The 12 domain events are: `project creation started`, `project creation submitted`, `project creation cancelled`, `project created`, `project creation failed`, `pdf import started`, `pdf import succeeded`, `pdf import failed`, `counter incremented`, `counter decremented`, `work session start requested`, and `work session saved`. Lifecycle/navigation also uses `app opened`, `app backgrounded`, `$screen`, and `screen exited`.

Screen normalization strips query and slash suffix, then allowlists 29 names: tools, counter, increase_decrease, gauge, cast_on, yarn, needles, size_charts, abbreviations, chart_symbols, settings, backup, pro_upgrade, yarn_card_detail, ravelry_browser, project_list, photo_gallery, pattern_viewer, notes_editor, session_history, counter_history, library, saved_patterns, saved_pattern_detail, web_pattern_editor, library_pattern_viewer, my_yarn, all_photos, insights. Unknown routes are dropped; the current `project_overview` route is not in that list.

Automatic capture, user profiles, replay, feature flags, push capture, tracing/logging/native crash capture, and GeoIP enrichment are disabled. Pseudonymous installation/session/device context still belongs to the SDK boundary. Disabling consent stops future app capture; it is not remote deletion of already-sent records. Payload behavior was inspected, not tested against a live provider here.

### Debug-only Sentry

Sentry Android core is `debugImplementation` only with initialization under [app/src/debug](app/src/debug); release source is no-op. DSN comes from ignored debug configuration/environment; no DSN means no initialization. Automatic startup/provider collection is disabled; traces/profiles, activity/frame collection, NDK, broad breadcrumbs, screenshots/view hierarchy and related automatic features are disabled. There is no Sentry Gradle upload plugin. Debug diagnostics are not a release analytics opt-in substitute.

### Accepted limits and security evidence

Browser redirect/subframe and cookie rules, local trial timing, client-side Play entitlement, provider cancellation limits, post-commit orphans, rasterized export and unencrypted archives are documented implementation boundaries. They are not proven vulnerabilities merely because stronger alternatives exist. The written Ravelry permission requirement is unresolved in the recorded decision. Release manifests/artifacts, live purchases, provider semantics, cloud deployments and vendor payloads remain unverified by this documentation inspection.

Scanner exceptions are controlled by [config/check-exceptions.json](config/check-exceptions.json), not blanket rule hiding. MobSF requires a single rule and exact `findingPath`; `.mobsf` must not globally suppress a registered finding rule. Current inventories and historical scan receipts appear in the QA sections.

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
| Swedish | `sv`, `values-sv` |

System is a selection mode, not a twelfth translation. There are no separately configured Brazilian Portuguese/Latin-American Spanish variants in this inventory. Android fallback handles unsupported device languages. Runtime language uses per-app platform/AppCompat APIs; DataStore is only the mirror/migration state. `supportsRtl` does not mean an Arabic/Hebrew translation or RTL device proof exists.

Static inspection found **1,346 translatable string/plural/string-array resource names in every supported locale**, with no missing/extra names. `translatable=false` preview/constants are excluded. Name parity does not prove semantic translation quality, format-placeholder correctness, plural behavior or fit on screen; dedicated tests cover parts of those separately.

### Formatting, units, and terminology

UI strings/plurals/content descriptions use Android resources; numbers/dates use locale-sensitive helpers. Session/history display respects local dates and 12/24-hour preferences; Insights uses stored session zones and locale first weekday. Null legacy zones fall back to the device. Folder uniqueness uses Unicode NFC/Root lowercase, not language-sensitive display sorting. Project names preserve original case.

Canonical calculation data uses mm, density/counts-per-mm and meters; user display can use metric/imperial units. Inches derive from canonical measurements; Yarn Estimator length can switch meters/yards while weight remains grams. Gauge raw input is preserved independently from formatted output. Size Charts use localized numeric formatting (integral zero decimals, otherwise one).

Finnish product terminology calls a knitting pattern an “ohje” and a web pattern a “verkko-ohje”; localized My Yarn uses `my_yarn_title`. Filename-like imported labels are cleaned without inventing designer metadata. English knitting abbreviations and the regex instruction grammar remain deliberately limited; translations do not imply parser support for all 11 languages or distinct crochet abbreviation data.

Accessibility/source tests and historical instrumented locale evidence are listed below. No current 320 dp/200%/TalkBack/RTL or full multilingual visual run occurred in this documentation task.

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
| `.github` | CI, CodeQL, Dependabot |
| `docs`, root plans | Product/design/implementation history; must be checked against current source |
| `reports`, build directories | Mostly ignored local evidence/generated outputs, not current production source |

[settings.gradle.kts](settings.gradle.kts) includes only `:app` and `:baselineprofile`; Foojay resolver convention is 1.0.0. Repositories are Google, Maven Central and Gradle plugin portal as applicable, with project repository additions forbidden. AGP built-in Kotlin is used; do not restore a separate `org.jetbrains.kotlin.android` plugin by habit.

[gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties) pins **Gradle 9.8.0** and its distribution SHA-256. [gradle-daemon-jvm.properties](gradle/gradle-daemon-jvm.properties) selects ADOPTIUM/Eclipse Temurin **JDK 17**; Java source/target and CI are 17. [gradle.properties](gradle.properties) enables build/configuration cache, disables parallel project execution, limits workers to two, gives Gradle 2 GiB heap, and uses in-process Kotlin compilation. AndroidX, nontransitive R and R8 full mode are configured.

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
| WorkManager | 2.12.0 | Declared runtime dependency; does not imply continuous sync |
| Firebase BOM | 34.19.0 | Current Android Crashlytics dependency |
| Google Services / Crashlytics plugin | 4.5.0 / 3.0.8 | Variant configuration/mapping integration |
| PostHog | 3.72.1 | Release opt-in usage analytics |
| Sentry Android core | 8.60.0 | Debug-only diagnostics |
| Baseline Profile / Benchmark | 1.5.0 | Stable producer/benchmark tooling |
| Profile Installer | 1.4.1 | Profile installation support |
| Wire runtime | 7.1.0 | Explicit security-compatible resolution override |
| ktlint Gradle | 14.2.0 | Format/lint plugin |
| Detekt | 2.0.0-alpha.5 | Temporary AGP 9 compatibility exception |
| Compose rules | 0.6.3 | Detekt/ktlint Compose checks; newest release built against Detekt 2.0.0-alpha.5 |
| Android security lint | 1.0.4 | Additional lint checks |
| Compose stability analyzer | 0.12.0 | Debug stability baseline/report |
| OWASP Dependency-Check | 13.0.0 | Dependency advisory scan |
| Sonar Gradle plugin | 7.5.0.8588 | Analysis/coverage integration |
| Dependency Analysis | 3.19.2 | Catalogued but root plugin disabled pending AGP 9 compatibility |
| JUnit / MockK / Turbine | 4.13.2 / 1.14.11 / 1.2.1 | JVM tests |
| org.json | 20260814 | JVM Android-JSON-compatible tests |
| AndroidX Test JUnit / Runner / Espresso | 1.3.0 / 1.7.0 / 3.7.0 | Device/Compose test support |

Root buildscript forces Jackson core/databind **2.22.3** and jsoup **1.23.2** for tooling. Subprojects force Wire 7.1.0 because negative-length decoding was corrected from 6.3.0. These are distinct from unrelated runtime upgrades. Detekt alpha is an explicit temporary exception; retain it until a stable AGP-compatible release is verified. Baseline tooling already uses stable 1.5.0. The inactive dependency-analysis catalog entry is not a configured passing check.

[gradle/verification-metadata.xml](gradle/verification-metadata.xml) records artifact verification checksums. Update it only with verified resolved dependencies/artifacts; XML presence is not proof of a newly resolved graph. Backend uses `functions/package-lock.json`; DeepSec has its own pnpm lock/workspace plus root forwarding scripts/lock. No dependency was changed or resolved during this documentation task.

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

[functions/package.json](functions/package.json) is private, targets Node 22 and `lib/index.js`, builds with TypeScript 7.0.2, and tests by build then `node --test "lib/**/*.test.js"`. It declares firebase-admin 14.5.0, firebase-functions 7.4.0, @types/node 26.6.4 and pinned transitive overrides for brace-expansion/form-data/js-yaml/protobufjs/rimraf/uuid. Declared type-package version is separate from the Node runtime. Deployment uses root Firebase config with predeploy compilation; no deployment, secrets/rules change or live function call occurred here.

## CI, analyzers, and release configuration

### GitHub configuration

[build.yml](.github/workflows/build.yml) runs on main pushes/PRs with read-only contents permission, pinned Checkout v7.0.1, Setup Java v6.0.1 (Temurin 17/signature verification), and Gradle Setup v6.4.0. It assembles debug, runs Gradle tests, `:app:ktlintCheck`, `:app:detekt`, and lint. A separate Node 22 job uses Setup Node v7.1.0, `npm --prefix functions ci --ignore-scripts`, then backend tests. It is not an emulator/device or Play purchase job.

[codeql.yml](.github/workflows/codeql.yml) uses pinned CodeQL action v4.38.2 for Java/Kotlin manual-build analysis (`assembleDebug --no-daemon`), main push/PR and weekly Monday 06:00 UTC schedule, 360-minute job timeout, and security-events write permission. Workflow existence is configured execution, not evidence of the latest remote run.

[dependabot.yml](.github/dependabot.yml) schedules weekly Gradle, GitHub Actions, `.deepsec` npm, and `functions` npm updates. No CI deployment or automatic app publication workflow is established by these files.

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
- **Detekt:** [config/detekt/detekt.yml](config/detekt/detekt.yml), parallel analysis as configured, and [app/detekt-baseline.xml](app/detekt-baseline.xml). Current static baseline: zero manually suppressed, 23 current issue IDs; this is not a fresh scan verdict.
- **OWASP:** plugin 13.0.0, debug/release runtime classpaths, HTML/JSON, default fail CVSS ≥7, local suppression XML, configurable data/update/NVD key/delay/retry. Direct task data-directory handling accounts for the plugin compatibility workaround. Hosted suppressions, KEV, OSS Index and RetireJS are disabled; node audit can use configured pnpm. Advisory coverage is limited to enabled analyzers/data.
- **OSV:** wrapper/checker dependency scanning with exception ownership and bounded operations. A no-findings receipt is different from zero dependencies or an unexecuted query.
- **Semgrep / MobSF / secret scanners:** [config/semgrep/knittools-security.yml](config/semgrep/knittools-security.yml), project custom rules and checker delegates; gitleaks/TruffleHog results and exact exceptions must be interpreted from raw receipts, not summary adjectives.
- **Sonar / JaCoCo:** root analysis depends on `:app:jacocoDebugUnitTestReport`; [sonar-project.properties](sonar-project.properties) supplies project configuration. JaCoCo reads Kotlin/Javac debug classes, rejects empty class inputs, and excludes generated code plus UI/widget/billing/pro/framework/storage/local/remote areas. Its percentage describes included classes, not whole-app runtime coverage. Sonar execution uploads data and needs separate authorization.

At the inspected snapshot, [config/check-exceptions.json](config/check-exceptions.json) has **44 entries**, all expiring **2026-10-31**: 37 MobSF, 2 gitleaks, 2 OSV, 1 Detekt, 1 OWASP and 1 TruffleHog. [dependency-check-suppressions.xml](config/dependency-check-suppressions.xml) has **12 suppression nodes**. A prior receipt's 16 applied suppressions is a historical run count, not the current XML-node count. Keep rationale/path/expiry and scanner scope distinct.

### DeepSec and release-surface checks

[.deepsec/package.json](.deepsec/package.json) pins DeepSec 2.3.9; [.deepsec/deepsec.config.ts](.deepsec/deepsec.config.ts) registers Android/Kotlin entry point, exported component, intent input, FileProvider/path, URI grant, widget mutation, sensitive logging and historical Ravelry credential/callable matchers. Root npm scripts forward into that workspace. A source scan selects candidates; process/revalidate uses an external model; export emits stored finding state. Reusing prior analyses or finding zero new files is not fresh semantic analysis of every source file.

`.deepsec/scripts/mark-accepted-risks.mjs` synchronizes narrowly identified historical credential findings with `config/security-decisions.md` and refuses reintroduced removed-risk surfaces. Old auth priority paths/prompt wording can remain historical configuration; they do not restore Android auth. Built-in TS/JS/Python matcher coverage alone is insufficient for the Kotlin application. Agent-assisted review is not a substitute for dependency SCA or device testing.

[tools/release-surface.ps1](tools/release-surface.ps1) checks release dependency/manifest/exported/credential/schema boundaries; companion release-surface, exported-surface and archive tests validate its own behavior. It allows Firebase BOM/Crashlytics and Google Services for Crashlytics while prohibiting AI/ML Kit/Gemini/voice and tracked private Google configuration, and scans locally/env-known Ravelry secret values without printing them. Firebase Auth and Functions are not approved Android dependencies; the forbidden-dependency checks enforce their absence.

Static release-surface success cannot establish a signed artifact, actual R8 behavior, provider/runtime grants, live telemetry, Play compliance, website permission or deployed Functions. Release readiness additionally needs valid signing/config, privacy/contact/release disclosures, supported platform tests and applicable Play/site decisions. Nothing was published or uploaded in this task.

## Testing and evidence

### Current test architecture

The inspected source has 304 JVM-test Kotlin files (294 named `*Test.kt`) and 71 Android-test Kotlin files (69 named `*Test.kt`), plus fixtures. Functions has 14 `.test.ts` files among 33 TypeScript files, including emulator-labeled harnesses. This is a source inventory, not a count of executed test methods, registered suites or successful checks.

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
| Baseline Profile | [baselineprofile/src](baselineprofile/src) | Configured profile producer; measurement needs an actual authorized device/emulator run |

Migration-specific instrumentation covers layered annotations (17), availability rebuild (19), bookmarks/guides (20), active sessions (21), project documents (22), folders (23), and broader migration paths. Schema-24 usage/schema-25 completion source/export/backup tests add their own boundaries; do not label every schema change as freshly instrumented from file existence.

Counter/session tests cover actual main-value rereads, concurrent writes, Undo eligibility, linked deltas, active checkpoint and recovery token idempotence. Real Room snapshot tests exercise cancellation releasing writers, project deletion/history replacement between batches and keyset filtering. Backup tests include 10,000/10,001/100,000 accepted sessions, 100,001 rejection, identity/annotation/file failures, and provider ownership. Native history is retained; the admission ceiling does not truncate older oversize histories.

### Insights resource and consistency evidence

Current `SessionDao` reads at most **256 rows per keyset query**. `CounterRepository` holds one Room transaction across session existence/project-activity facts, earliest local date and all history batches, folds/release batches, and publishes only a completed snapshot. Cancellation is checked per batch/session. Invalidation triggers replacement calculation; it does not create the consistent snapshot itself.

Week/month SQL restricts overlapping rows to the preceding comparison period or later and applies project filtering; the lower bound uses UTC+18 midnight to preserve accepted stored-zone dates. Exact zone-aware splitting stays in Kotlin. First All-Time date examines bounded candidates within 36 hours of the earliest UTC start. `InsightsActivityDates` uses sparse 4,096-day bit blocks; visible chart/fabric windows retain bounded facts, while historical totals/streaks still include all sessions. Future session data is not silently discarded.

[config/insights-session-budget.md](config/insights-session-budget.md) explains ceilings and performance limits. Analysis may split up to 366 day segments/session; full 100,000-row history can require substantial aggregate work/storage. The session transaction can delay writers. Independently observed projects/completion events/gates do not make the entire combined UI one database-wide snapshot. Neither batching nor pure tests establish maximum device latency/memory.

### Historical execution receipts

These reports already existed and were read during the audit. **None is a fresh execution against HEAD `770f019f21001087eb5bc57a028b674c44f149de`.** Local ignored reports may be unavailable in a fresh clone.

| Existing evidence | Source binding and command/result | Practical limit |
| --- | --- | --- |
| [2026-10-11 build receipt](reports/runs/20261011T023916836-2b9a64bb/manifest.json) | 02:39–02:40 EEST; Gradle 9.8.0, `:app:assembleDebug :baselineprofile:assemble`, CLEAN/exit 0; dirty branch `fix/vanhentuneet-laitetestit`, HEAD `fb1cfb348f65b80355874b4f79a313bdb6d6857b` | Earlier working-tree input, not today's clean HEAD; build is not device/layout/release evidence |
| [2026-10-11 JVM receipt](reports/runs/20261011T024206355-b9fb2836/manifest.json) | 02:42:06–02:42:49 EEST; `:app:testDebugUnitTest`, CLEAN/exit 0; same earlier source fingerprint; **all 38 tasks UP-TO-DATE** | No newly executed case/suite count; receipt explicitly says instrumented NOT_RUN |
| [2026-10-11 security receipt](reports/runs/20261011T020448023-16038dab/manifest.json) | CLEAN aggregate; `sourceState` null; OSV/OWASP report no findings, with advisories/suppressions and separate secret/MobSF results | Cannot bind to current HEAD; aggregate CLEAN does not mean every advisory absent or every finding unsuppressed |
| [2026-10-11 DeepSec receipt](reports/runs/20261011T022705024-0423c4e1/manifest.json) | 67 candidates, reused stored analyses, zero newly processed files, zero active exported findings | Historical stored-state refresh, not 67 fresh semantic reviews or complete Kotlin assurance |
| [2026-10-01 targeted Android results](reports/targeted-android-20261001/results.json) and [runner output](reports/targeted-android-20261001/instrumentation.txt) | API 36 emulator campaign: `OK (12 tests)`, 4 session/Room, 6 backup, 2 localization; recorded source hashes in [source-tested.json](reports/targeted-android-20261001/source-tested.json) | Historical correction candidate, not current HEAD; local-file/Room/ICU tests, not SAF provider, live Ravelry/Play or all layouts |
| [2026-10-01 provider-race follow-up](reports/pr176-check-20261001/README.md) | Historical focused JVM regression investigation/verification and preservation evidence | Earlier passing suite did not rule out a provider timing race; read red/fix artifacts and exact candidate before reuse |

The two Oct-11 build/JVM receipts have stable before/after source state: 890 input files, input SHA-256 `384d962e58831d4924f8a313d028f543379efccaa36654a5de5c5d8b00579ec0`, Git-status SHA-256 `d528e236b2d9328ca20c945cd5b5114c7cb7932409deb8ad178f559164bda609`. They establish which earlier input was checked; they do not certify later commits.

Historical Oct-1 documentation at `85536415eb1f2c617c8e96db477b954e3cff21da` described a dirty correction candidate (31 modified/four untracked initially) and 2,059 JVM tests/299 suites after corrections. The provider follow-up README records reading those prior result artifacts. Preserve that as dated context only; later API/UI/build changes and current test inventory differ. The twelve targeted Android cases include synthetic 100,000-session local-file round-trip, not measured full-history performance or a production SAF cloud-provider campaign.

Current on-disk JUnit XML files may reflect cached/older runs (e.g. an aggregate 2,008 tests/297 XML suites). Without a receipt/source/time link, they are not assigned to a current successful execution. ADB exit 0, `OK (0 tests)`, screenshots, an UP-TO-DATE task, and a successful APK build are each insufficient substitutes for an actual named runtime/test outcome.

### Documentation-specific validation boundary

This rewrite is checked with `git diff --check`, complete diff review, source-path/heading/TOC/table/fence checks, exact route/schema/locales/catalog comparisons, literal documentation assertions from `ArchitectureSingleSourceSourceTest`, stale-claim searches, and preservation fingerprints. Replaying those literal assertions read-only is a document check, not running JUnit or the Android test suite. No aggregate wrappers, expensive scanners, external uploads, physical-device operations or new emulator execution are part of this task.

## Development and review invariants

### Workflow and source authority

[AGENTS.md](AGENTS.md) and [CODEX.md](CODEX.md) are aligned working instructions and must change together if either is edited. [CLAUDE.md](CLAUDE.md) owns product wording/design guidance. Inspect actual branch, staged/dirty work and owning implementation before a change. Source/config/schema/test implementation outrank a plan or AI finding; verify reported defects before editing.

Apply the minimal-sufficient-work deletion rule after correctness, explicit requirements and required checks. Preserve unrelated user changes. Reviews/plans do not authorize edits. Implementation requests require actual completion; Android skill-driven source/configuration changes have the project-specific proposed-scope approval boundary. UI plans explicitly include 320 dp, 200% font, and their combination. Report static, local, device, provider, service and release evidence separately.

Create a task feature branch when the checkout is a protected integration branch, use `codex/` for new branches, and commit/push only on explicit request. Never create/open/merge a PR: the user does that manually in GitHub. Inspect staged content and complete push commit/destination scope; no direct protected-branch updates. Commit messages are concise Finnish; source identifiers/comments stay English. Stop only task-owned temporary processes, never broad executable-name kills.

### Ownership checklist for code changes

| Change area | Read first / invariant to preserve |
| --- | --- |
| Main count/Undo/widget | `CounterRepository.applyMainCounterChange`, `applyWidgetCountChange`, history/domain logic; current-row reread and all dependent mutations atomic |
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

Use existing focused tests for realistic changed behavior/failure paths; do not mirror implementation or add a new harness when an existing owning check suffices. Real Room invariants need Android database coverage when appropriate; source tests alone cannot prove constraints. Provider fakes cover ownership but not every OEM/document service. UI changes need actual narrow/large-font interaction evidence, not only semantics/source matches.

Preserve cancellation at coroutine boundaries; ordinary exception recovery must not swallow it. Application-lifetime persistence must still select injected IO. Flow retry comes after domain mapping. Multi-DAO writes use `DatabaseTransactionRunner`; do not add a ViewModel sequence that can leave half a relationship. File cleanup failure after commit is not a rolled-back delete.

When reporting checks, record command, revision/dirty fingerprint, time, executed case count versus cached tasks, exact scope and blocks. Raw reports precede fixes; exception/baseline changes require evidence and rationale. A reviewer should be able to follow UI→ViewModel→domain/repository→DAO/storage→test without relying on this document alone.

## Current status, limitations, and document authority

### Implemented and reachable

Current Android routes expose project overview/counter/folders/lifecycle, local counts and history, extra/secondary/stitch/reminder/note state, explicit sessions/recovery, Library saved metadata and web links, multiple project PDF documents, import/share/photo-to-PDF, guides/calibration/bookmarks, layered annotation/chart tools/export, project/progress/yarn photos, yarn stash/notes/usage, calculators/references, Insights, widgets, preferences, user-started trial, one-time Pro and manual full-content replacement backup. Release Crashlytics and optional consented PostHog are configured. Ravelry opens its website in-app and imports selected website PDFs through Save Pattern.

### Retained, historical, and planned boundaries

- The Firebase Ravelry backend is retained but disconnected from current Android. Native API search/import/auth routes and callbacks were removed. Earlier backend plans and deployed-state assumptions must not be presented as current app functionality.
- Project legacy reader/PDF columns and old URI compatibility exist for migration/read/cleanup. `project_documents` is current authority; compatible saved metadata does not revive old persistence sentinels.
- Dependency Analysis is catalogued but disabled for toolchain compatibility. Debug diagnostics/demo/test state is intentionally separate from release behavior. Baseline tooling is configured without a new measurement here.
- Future continuous Drive/Dropbox/cross-device synchronization remains specification work; manual SAF import/backup is implemented. Merge restore/conflict resolution/storage-provider OAuth is absent.
- Voice/microphone, text-to-speech, model-backed parser/cloud notes, release Sentry/replay/tracing, Firebase AI/ML Kit/Gemini and App Check are not implemented. Historic design/spec files for these do not authorize restoration.

### Supported limitations and outstanding verification

| Item | Established boundary / remaining uncertainty |
| --- | --- |
| Runtime proof | Current HEAD was inspected, not newly built/tested. Historical receipts are older/dirty candidates; current remote CI is unknown |
| Layout/accessibility | Adaptive source/semantics and tests exist; 320 dp, 200%, combined, TalkBack/IME/full-language/device matrix was not run |
| Large histories | 100,000 sessions supported by writer/archive admission and historical Room round-trip; maximum resource use, Insights writer delay and real-device responsiveness unmeasured |
| Provider behavior | Fake tests and historical local-file restore do not prove production SAF provider cancellation/durability/retry; blocked workers and partial outputs remain possible |
| Backup compatibility | Current version/schema validation only; no general cross-schema adapter/merge/encryption/authenticity. Other budgets can reject an otherwise valid native history |
| File cleanup | Post-commit best effort can leave orphans; startup owners cover specific directories/journals, not a universal PDF sweep |
| Browser | Arbitrary HTTPS redirect/subframe/download destinations can pass; website session retained; no universal host/DNS firewall or app Disconnect |
| Ravelry permission | Security decision still requires written site permission before Play release; acquisition not established |
| Billing/trial | Source handles local timing/Play states; actual product/price/ack/refund/revocation and store configuration not verified live |
| Cloud/release | Retained Functions deployment/indexes/rules/secrets/TTL, signed release artifact, mapping upload, vendor telemetry and Play release compliance unverified |
| Localization | 11-locale name parity; editorial meaning/grammar/full render fit and broader ICU/platform behavior not certified |
| Scanner scope | 44 expiring exceptions, current baseline/suppression inventories and old receipt results are not a fresh vulnerability verdict |

No speculative implementation defect is implied by an unexecuted check. These boundaries tell a future developer where evidence is needed before making a stronger claim.

### Related documents and resolved stale assumptions

| Document / source | Authority and current caveat |
| --- | --- |
| [AGENTS.md](AGENTS.md), [CODEX.md](CODEX.md) | Aligned working/ownership rules; no edits made in this task |
| [CLAUDE.md](CLAUDE.md) | Product wording/current visual guidance; verify implementation against code |
| [README.md](README.md) | Short overview; old auth/package descriptions can lag removed Android Ravelry code |
| [ONLINE_OFFLINE.md](ONLINE_OFFLINE.md) | Older network inventory; Firebase/OAuth/API and no-PDF-download wording is superseded by current browser code |
| [PRIVACY-POLICY.md](PRIVACY-POLICY.md) | Draft with missing contact, dated 2026-09-03; says no analytics/release crash reporting and old Firebase Ravelry/no-download flow, contradicting current source. Needs a separate authorized update before publication |
| [config/security-decisions.md](config/security-decisions.md) | Recorded security decisions, including current browser permission requirement |
| [config/ravelry-backend-progress.md](config/ravelry-backend-progress.md) | Backend history/rollout context; not Android reachability or live deployment evidence |
| [Ravelry Firebase Backend And Saved Patterns Plan.md](<Ravelry Firebase Backend And Saved Patterns Plan.md>) | Historical API/backend plan, not the current browser product |
| [config/local-backup.md](config/local-backup.md), [config/insights-session-budget.md](config/insights-session-budget.md), [config/annotation-page-limits.md](config/annotation-page-limits.md) | Detailed budgets/rationale; current implementation and dated runtime evidence still govern |
| [config/future-sync-spec.md](config/future-sync-spec.md) | Future sync specification boundary, not shipped service |
| [config/posthog.md](config/posthog.md) | Consent/data policy; older SDK 3.64.1 mention is superseded by catalog 3.72.1 |
| Root Fable/Projects/Insights plans and `docs` | Design/implementation history; older Outfit, cardless rows or selectors are not current visual contracts |
| `reports` | Dated, often ignored evidence; pair outcomes with input identity and command rather than importing old pass claims |

This reconstruction removes contradictory claims of active Android Firebase Ravelry OAuth/search/import; corrects the former no-Ravelry-PDF-download list; restores the actual `ProjectCard`/Continue/overview hierarchy; documents schema 25 and all 18 tables; updates Gradle/dependency/SDK facts; replaces outdated free-tier assumptions with repository gates; and separates current backup/telemetry behavior from historical reports. Missing/deleted source names are not used as active code references.

Future questions should start from current source and configuration, then tests and raw runtime receipts, then decision documents. Update this reference when those sources change; do not resolve drift by appending a newer statement below an older contradictory claim.
