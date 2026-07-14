# Pattern reading line progress

## 2026-06-08 11:32 +03:00

Branch: `codex/project-workspace-cards`

Started phase: Vaihe 0 - Baseline ja etenemismuistio
Completed phase: Vaihe 0 - Baseline ja etenemismuistio

## Baseline

- Read `KnitTools PDF Reading Line - toteutussuunnitelma.md`, `AGENTS.md`, and `CODEX.md`.
- Checked the latest official Android docs for Compose pointer input / drag gesture handling and `PdfRenderer` rendering before implementation work.
- `git status --short` showed an already dirty worktree before this progress file was created. Existing modified files include `AGENTS.md`, `CODEX.md`, `PROJECT.md`, Gradle files, reading-line-related app sources, and related source tests. Existing untracked files include the PDF reading-line plan, debug/release source-set folders, `PdfPageRendererSourceTest.kt`, `CounterUndoSourceTest.kt`, and `tools/sentry.ps1`.
- `config/pattern-reading-line-progress.md` did not exist before this phase.

## Current reading-line facts checked from source

- `app/src/main/java/com/finnvek/knittools/data/storage/PdfPageRenderer.kt` still calls `bitmap.eraseColor(Color.WHITE)` before `page.render(...)`.
- `app/src/main/java/com/finnvek/knittools/domain/model/ReadingLine.kt` still defines shared clamp constants and `READING_LINE_ROW_STEP_FRACTION`.
- `app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt` still contains `TrackReadingLineForCurrentRow`.
- `CounterViewModel` still exposes `upsertPatternRowMarker` and `mergePatternRowMarkers`.

## Verification

- Ran source checks with `rg` for `eraseColor`, `Color.WHITE`, `ReadingLine`, `sanitizeReadingLineYFraction`, `READING_LINE_ROW_STEP_FRACTION`, `TrackReadingLineForCurrentRow`, `patternRowMapping`, `RowMarker`, `upsertPatternRowMarker`, and `mergePatternRowMarkers`.
- No Gradle tests were run in Vaihe 0 because no production or test code was changed.

## Not done yet

- Vaihe 1 was not started.
- No drag-commit API, row-anchor commit flow, interpolation helper, viewer controls, UI label, calibration flow, persistence polish, architecture docs, or memory update note was added in this phase.
- Per the plan, continue only after the user says `jatka`.

## 2026-06-08 11:52 +03:00

Branch: `codex/project-workspace-cards`

Started phase: Vaihe 1 - Drag-commit-rajapinta lukulinjalle
Completed phase: Vaihe 1 - Drag-commit-rajapinta lukulinjalle

## Changes

- Added a failing-first `PatternViewerSourceTest` assertion for separate live update and drag-end commit callbacks.
- Added `onReadingLineYFractionCommit` to `PatternViewerContentActions`.
- Passed the commit callback from `PatternViewerDocument` to `ReadingLineOverlay`.
- Updated `ReadingLineOverlay` to use named `detectVerticalDragGestures` callbacks:
  - `onVerticalDrag` updates the live y fraction.
  - `onDragEnd` commits the last calculated y fraction.
  - `onDragCancel` does not commit a new anchor.
- Preserved the existing zoom correction with `dragAmount / scale.coerceAtLeast(1f)`.
- Kept the library viewer session-local by writing commit updates only to the local `rememberSaveable` state.

## Verification

- RED: `.\gradlew :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest` failed as expected in `reading line drag has separate live update and drag end commit callbacks` at `PatternViewerSourceTest.kt:54` before production code was changed.
- GREEN caveat: the exact planned command later reached `:app:testDebugUnitTest`, but returned exit 1 because the existing Gradle configuration cache could not be stored.
- GREEN proof: `.\gradlew --no-configuration-cache :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest --rerun-tasks` finished with `BUILD SUCCESSFUL` and `33 actionable tasks: 33 executed`.
- Source check: `rg` confirmed `onReadingLineYFractionCommit`, `onYFractionCommit`, `onDragEnd`, `onDragCancel`, `onVerticalDrag`, and `dragAmount / scale.coerceAtLeast(1f)` are present in the pattern viewer path.

## Not done yet

- Vaihe 2 was not started.
- Drag release does not yet create or update `RowMarker(row,page,yPosition)` anchors.
- No interpolation helper, viewer controls, row label, calibration flow, persistence preview-state, architecture docs, or memory update note was added in this phase.
- Per the plan, continue only after the user says `jatka`.

## 2026-06-08 12:02 +03:00

Branch: `codex/project-workspace-cards`

Started phase: Vaihe 2 - Manuaalinen riviankkuri dragin lopussa
Completed phase: Vaihe 2 - Manuaalinen riviankkuri dragin lopussa

## Changes

- Added a failing-first `PatternViewerSourceTest` assertion for project PDF drag commit behavior.
- Project pattern viewer commit now:
  - clamps the committed y fraction with `sanitizeReadingLineYFraction(yFraction)`
  - calls `counterViewModel.updateReadingLineYFraction(sanitizedYFraction)`
  - calls `counterViewModel.upsertPatternRowMarker(row = counterState.counter.count, page = currentPage, yPosition = sanitizedYFraction)`
- Library pattern viewer remains session-local and does not call `upsertPatternRowMarker`.
- Added a source assertion that `CounterViewModel.upsertPatternRowMarker` replaces an existing same row/page marker instead of appending a duplicate.
- No schema changes were made; the existing `patternRowMapping` JSON path is still used.

## Verification

- RED: `.\gradlew --no-configuration-cache :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest --tests com.finnvek.knittools.domain.calculator.RowMappingParserTest --rerun-tasks` failed as expected in `project reading line drag commit stores current row page anchor` at `PatternViewerSourceTest.kt:77` before production code was changed.
- GREEN: `.\gradlew --no-configuration-cache :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest --tests com.finnvek.knittools.domain.calculator.RowMappingParserTest --rerun-tasks` finished with `BUILD SUCCESSFUL` and `33 actionable tasks: 33 executed`.
- Source check: `rg` confirmed the project viewer commit path includes `sanitizeReadingLineYFraction(yFraction)`, `upsertPatternRowMarker`, `row = counterState.counter.count`, `page = currentPage`, and `yPosition = sanitizedYFraction`.
- Checked the latest official Android Compose state-hoisting docs before implementation; the commit remains a ViewModel event for project business state, while library state stays local.

## Not done yet

- Vaihe 3 was not started.
- One-anchor/fallback row movement semantics are still the current pre-phase-3 behavior.
- No interpolation helper changes, viewer controls, row label, calibration flow, persistence preview-state, architecture docs, or memory update note was added in this phase.
- Per the plan, continue only after the user says `jatka`.

## 2026-06-08 12:15 +03:00

Branch: `codex/project-workspace-cards`

Started phase: Vaihe 3 - Korjaa riviliikkeen päätöksenteko ankkureilla
Completed phase: Vaihe 3 - Korjaa riviliikkeen päätöksenteko ankkureilla

## Changes

- Added `domain/calculator/resolveReadingLineYFraction`.
- Updated row-position resolution rules:
  - exact marker on the current page returns its clamped y fraction
  - previous and next markers on the current page interpolate
  - one-sided anchors no longer lock the reading line to the anchor y value
  - missing mapping plus non-zero row delta uses row-step fallback from the current y fraction
  - missing mapping plus zero row delta returns no change
  - markers from other pages do not influence the current page
- Updated `TrackReadingLineForCurrentRow` to delegate row movement decisions to `resolveReadingLineYFraction`.
- Updated `interpolateYPosition` so one-sided marker lookup returns `null` instead of extrapolating to the marker y value.
- Updated `AGENTS.md` and `CODEX.md` with the domain helper ownership because row-movement responsibility moved out of the pattern viewer UI.
- No schema changes were made.

## Verification

- RED: `.\gradlew --no-configuration-cache :app:testDebugUnitTest --tests com.finnvek.knittools.domain.model.ReadingLineTest --tests com.finnvek.knittools.domain.calculator.RowMappingParserTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest --rerun-tasks` failed before production code with unresolved `resolveReadingLineYFraction` references in `RowMappingParserTest.kt`.
- GREEN: `.\gradlew --no-configuration-cache :app:testDebugUnitTest --tests com.finnvek.knittools.domain.model.ReadingLineTest --tests com.finnvek.knittools.domain.calculator.RowMappingParserTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest --rerun-tasks` finished with `BUILD SUCCESSFUL` and `33 actionable tasks: 33 executed`.
- Source check: `rg` confirmed `resolveReadingLineYFraction` is used by the pattern viewer and that the old one-sided `previous?.yPosition ?: next?.yPosition` fallback is gone from `RowMappingParser.kt`.
- Documentation check: `AGENTS.md` and `CODEX.md` both mention `domain/calculator/resolveReadingLineYFraction`.
- Checked the latest official Kotlin collection docs before implementation for the safe `firstOrNull` / `lastOrNull` marker lookup style.

## Not done yet

- Vaihe 4 was not started.
- No user-visible row-marker controls, row label, calibration flow, persistence preview-state, final architecture pass, or memory update note was added in this phase.
- Memory update note was not created because this chat did not explicitly request or allow updating Codex memory.
- Per the plan, continue only after the user says `jatka`.

## 2026-06-08 12:33 +03:00

Branch: `codex/project-workspace-cards`

Started phase: Vaihe 4 - Käyttäjän näkyvät kontrollit mallille 1
Completed phase: Vaihe 4 - Käyttäjän näkyvät kontrollit mallille 1

## Changes

- Added project-only pattern viewer overflow actions:
  - `Save line as row X`
  - `Clear row mark`
  - `Clear page marks`
- Project viewer wires those actions to the current row/current page:
  - save calls `upsertPatternRowMarker(row = counterState.counter.count, page = currentPage, yPosition = counterState.readingLineYFraction)`
  - clear row calls `removePatternRowMarker(row = counterState.counter.count, page = currentPage)`
  - clear page calls `removePatternRowMarkersForPage(currentPage)`
- Library pattern viewer keeps no-op callbacks and does not show project row-marker menu items.
- Added `CounterViewModel.removePatternRowMarker(row, page)` and `removePatternRowMarkersForPage(page)` using `parseMapping`, `filterNot`, `serializeMapping`, and `updatePatternRowMapping`.
- Added `pattern_save_line_as_row`, `pattern_clear_row_mark`, and `pattern_clear_page_marks` to all 11 localized `strings.xml` files.
- No schema changes were made.

## Verification

- RED: `.\gradlew --no-configuration-cache :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest --tests com.finnvek.knittools.domain.calculator.RowMappingParserTest --rerun-tasks` failed before production code with 4 expected `PatternViewerSourceTest` failures for missing project controls, topbar menu items, ViewModel removal methods, and localized strings.
- GREEN: `.\gradlew --no-configuration-cache :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest --tests com.finnvek.knittools.domain.calculator.RowMappingParserTest --rerun-tasks` finished with `BUILD SUCCESSFUL` and `33 actionable tasks: 33 executed`.
- Source check: `rg` confirmed the project-only callbacks and ViewModel remove methods are present in the source.
- Resource check: PowerShell `Select-String` confirmed all three new row-marker string keys exist in every localized `strings.xml`.
- Checked the latest official Android Compose menu and resource docs before implementation.

## Not done yet

- Vaihe 5 was not started.
- No row label, lock mode, calibration flow, persistence preview-state, final architecture pass, or memory update note was added in this phase.
- Memory update note was not created because this chat did not explicitly request or allow updating Codex memory.
- Per the plan, continue only after the user says `jatka`.

## 2026-06-08 12:47 +03:00

Branch: `codex/project-workspace-cards`

Started phase: Vaihe 5 - Visuaalinen laatu lukulinjalle
Completed phase: Vaihe 5 - Visuaalinen laatu lukulinjalle (row-label only)

## Changes

- Passed the project `currentRow` into `ReadingLineOverlay`.
- Kept the reading-line drag pointer input on the full overlay surface, while the visual line/band still renders in the transformed PDF layer.
- Added `ReadingLineRowLabel` as a small left-edge pill aligned to the reading-line y fraction.
- The label uses the existing localized `current_row_short` string resource instead of hardcoded visible text.
- The label uses Material theme colors (`primaryContainer` / `onPrimaryContainer`) and `MaterialTheme.shapes.small`; no new inline color tokens or string resources were added.
- The label is clamped vertically inside the PDF area so it does not drift outside the rendered page bounds.

## Verification

- RED: `.\gradlew --no-configuration-cache :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest --rerun-tasks` failed before production code with the expected new `PatternViewerSourceTest` failures for missing row-label wiring and missing localized label text.
- GREEN: `.\gradlew --no-configuration-cache :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest --rerun-tasks` finished with `BUILD SUCCESSFUL` and `33 actionable tasks: 33 executed`.
- Build proof: `.\gradlew --no-configuration-cache :app:assembleDebug` finished with `BUILD SUCCESSFUL` and `42 actionable tasks: 19 executed, 23 up-to-date`.
- Source check: `rg` confirmed `currentRow = state.currentRow`, `ReadingLineRowLabel`, `current_row_short`, `primaryContainer`, and `onPrimaryContainer` are present in the pattern viewer path.
- Checked the latest official Android Compose layout docs before implementation for Box-based overlay composition and child positioning.

## Not done yet

- Optional lock mode was not started because the phase was kept to the row-label slice.
- No calibration flow, persistence preview-state, final architecture pass, or memory update note was added in this phase.
- Manual Android visual inspection on a device/emulator was not run in this phase; verification is source-level plus debug APK build.
- Memory update note was not created because this chat did not explicitly request or allow updating Codex memory.
- Per the plan, continue only after the user says `jatka`.

## 2026-06-08 13:21 +03:00

Branch: `codex/project-workspace-cards`

Started phase: Vaihe 6 - Kahden pisteen kalibrointi
Completed phase: Vaihe 6 - Kahden pisteen kalibrointi

## Changes

- Added `createCalibrationRowMarkers` in `domain/calculator/RowMappingParser.kt`.
- The calibration helper:
  - returns exactly two `RowMarker` values for first and last calibrated rows
  - clamps y positions through the shared reading-line clamp
  - rejects matching first/last row values by returning `null`
  - keeps sorting compatible with existing row-mapping serialization/interpolation behavior
- Added a project-only calibration mode in `PatternViewerScreen`:
  - starts from the pattern viewer overflow menu
  - enables the reading line when calibration starts
  - Step 1 saves the current line/page as the first marker with the entered row number
  - Step 2 saves the current line/page as the last marker with the entered row number
  - accepted calibration calls `counterViewModel.mergePatternRowMarkers(markers)`
  - cancel clears only the transient calibration state and does not call the mapping writer
  - invalid or matching row values stay local and show a localized error
- Library pattern viewer still has no calibration panel and no `mergePatternRowMarkers` path.
- Added calibration strings to all 11 localized `strings.xml` files:
  - `pattern_calibrate_rows`
  - `pattern_calibration_first_row`
  - `pattern_calibration_last_row`
  - `pattern_calibration_save_first`
  - `pattern_calibration_save_last`
  - `pattern_calibration_invalid_row`
- No schema changes were made.

## Verification

- RED: `.\gradlew --no-configuration-cache :app:testDebugUnitTest --tests com.finnvek.knittools.domain.calculator.RowMappingParserTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest --rerun-tasks` failed before production code with unresolved `createCalibrationRowMarkers`.
- GREEN: the same command finished with `BUILD SUCCESSFUL` and `33 actionable tasks: 33 executed`.
- Re-ran the same command after a small calibration-panel layout cleanup; it again finished with `BUILD SUCCESSFUL` and `33 actionable tasks: 33 executed`.
- `git diff --check` passed for the Phase 6 Kotlin, test, resource, and progress files; only existing CRLF normalization warnings were printed.
- `rg -n ".{121,}"` found no over-120-character lines in the changed Kotlin/test files.
- PowerShell XML parsing confirmed every new calibration string key exists in every localized `strings.xml`.
- Checked the latest official Android Compose state-hoisting and Dialog/component docs before implementation. The implemented calibration state stays local to the project viewer until the accepted marker merge event reaches the ViewModel.

## Not done yet

- Vaihe 7 was not started.
- No transient drag preview-state, DB-write reduction, final architecture pass, or memory update note was added in this phase.
- Manual Android visual inspection on a device/emulator was not run in this phase; verification is source-level plus targeted Gradle unit/source tests.
- Memory update note was not created because this chat did not explicitly request or allow updating Codex memory.
- Per the plan, continue only after the user says `jatka`.

## 2026-06-08 13:47 +03:00

Branch: `codex/project-workspace-cards`

Started phase: Vaihe 7 - Persistointipolish ja suorituskyky
Completed phase: Vaihe 7 - Persistointipolish ja suorituskyky

## Changes

- Added project viewer `readingLinePreviewYFraction` transient UI state for reading-line drag.
- Added `isReadingLineDragging` so external committed state syncs back into preview only when the user is not dragging.
- Live drag now updates only the local preview state:
  - `onReadingLineYFractionChange` sanitizes the y fraction into `readingLinePreviewYFraction`
  - the project content action no longer wires live drag directly to `counterViewModel.updateReadingLineYFraction`
- Drag commit still writes the committed y fraction and current row/page anchor:
  - `counterViewModel.updateReadingLineYFraction(sanitizedYFraction)`
  - `counterViewModel.upsertPatternRowMarker(row = counterState.counter.count, page = currentPage, yPosition = sanitizedYFraction)`
- Drag cancel now clears the dragging flag and restores preview from the committed `counterState.readingLineYFraction`.
- Added drag start/cancel callbacks through `PatternViewerContentActions` and `ReadingLineOverlay`.
- Library viewer remains session-local; its drag start/cancel callbacks are no-ops and it still writes no project row anchors.
- No ViewModel, repository, DAO, schema, or resource changes were made in this phase.

## Verification

- RED: `.\gradlew --no-configuration-cache :app:testDebugUnitTest --tests com.finnvek.knittools.ui.screens.pattern.PatternViewerSourceTest --rerun-tasks` failed before production code with the expected three new `PatternViewerSourceTest` failures for missing preview state and drag start/cancel callbacks.
- GREEN: the same command finished with `BUILD SUCCESSFUL` and `33 actionable tasks: 33 executed`.
- Build proof: `.\gradlew --no-configuration-cache :app:assembleDebug` finished with `BUILD SUCCESSFUL` and `42 actionable tasks: 5 executed, 37 up-to-date`.
- Re-ran `PatternViewerSourceTest` after test formatting cleanup; it finished with `BUILD SUCCESSFUL` and `33 actionable tasks: 33 executed`.
- `git diff --check` passed for the Phase 7 files; only existing CRLF normalization warnings were printed.
- `rg -n ".{121,}"` found no over-120-character lines in the changed Kotlin/test files.
- Checked the latest official Android Compose state-hoisting and performance best-practice docs before implementation. The preview is local UI state and committed persistence remains a ViewModel event.

## Not done yet

- Vaihe 8 was not started.
- No documentation/architecture-memory pass, `PROJECT.md` review, final acceptance sweep, or memory update note was added in this phase.
- Manual Android jank inspection on a device/emulator was not run in this phase; verification is source-level plus debug APK build.
- Memory update note was not created because this chat did not explicitly request or allow updating Codex memory.
- Per the plan, continue only after the user says `jatka`.

## 2026-06-08 13:53 +03:00

Branch: `codex/project-workspace-cards`

Started phase: Vaihe 8 - Dokumentointi ja arkkitehtuurimuisti
Completed phase: Vaihe 8 - Dokumentointi ja arkkitehtuurimuisti

## Changes

- Updated `AGENTS.md` and `CODEX.md` with the same reading-line architecture text:
  - attached project PDF reading-line state persists on `counter_projects.readingLineEnabled` and `readingLineYFraction`
  - row anchors live in `counter_projects.patternRowMapping` as serialized `RowMarker(row,page,yPosition)` values owned by `domain/calculator/RowMappingParser`
  - drag commit creates or updates the current row/page anchor through `CounterViewModel.upsertPatternRowMarker`
  - calibration merges two anchors through `mergePatternRowMarkers`
  - live drag uses project-viewer preview state before commit
  - library-only pattern viewer state remains session/rotation-saveable and does not create a saved-pattern schema path in v1
- Updated `PROJECT.md` because its current-state Pattern PDF / reading-line section already documented persistence, but did not yet describe `patternRowMapping`, `RowMarker(row,page,yPosition)`, drag commit anchors, calibration merge, or interpolation ownership.
- Did not create an ad-hoc memory update note because this chat did not explicitly allow writing Codex memory.
- No production code, tests, schemas, or resources were changed in this phase.

## Verification

- Code-backed source checks before editing confirmed:
  - `CounterViewModel.updateReadingLineYFraction` writes committed reading-line state
  - `CounterViewModel.upsertPatternRowMarker`, `removePatternRowMarker`, `removePatternRowMarkersForPage`, and `mergePatternRowMarkers` own row-marker writes
  - `PatternViewerScreen` uses `readingLinePreviewYFraction` for live drag preview and commits through the ViewModel event path
  - `LibraryPatternViewerScreen` keeps page and reading-line state under `rememberSaveable(patternUri)`
  - `domain/calculator/RowMappingParser.kt` owns `RowMarker`, parsing, serialization, calibration marker creation, and row-position resolution
- Planned verification command run:
  - `rg -n "patternRowMapping|readingLineEnabled|readingLineYFraction|RowMarker" AGENTS.md CODEX.md PROJECT.md config`
- The verification output contains the expected reading-line persistence and row-anchor terms in `AGENTS.md`, `CODEX.md`, `PROJECT.md`, and `config/pattern-reading-line-progress.md`.
- Checked the latest official Android Compose state-hoisting docs before the documentation update. The documented split matches the code: UI preview state stays local and committed persistence remains a ViewModel/repository event.

## Not done yet

- No final full acceptance sweep beyond the Phase 8 documentation verification was run in this phase.
- Manual Android device/emulator testing was not run in this phase.
- Memory update note was not created because this chat did not explicitly request or allow updating Codex memory.
- Per the plan, continue only after the user says `jatka`.
