# KnitTools Screenshot Review 2026-11-19: Implementation Plan

Source: full English-UI screenshot pass, 222 images in `reports/knittools-all-screens-en-2026-11-19/`.
Screenshot references below use the form **NN/MM** = folder number / file number, and each one is a link to the image.
Every finding was checked against the code on branch `codex/paikalliset-muutokset-20261119` (commit `f81a9ffe`). Line numbers are from that commit; re-locate with grep if they have moved.

Implementer: Codex. Reviewer and owner: Emma.

---

## 0. Ground rules for this work

Read these before touching code. They override anything in this plan that seems to conflict.

1. `CLAUDE.md`, `AGENTS.md`, `CODEX.md` and `.claude/rules/ui.md` are binding. In particular:
   - Shared components only (`FormSheet`, `SheetTitle`, `SegmentedToggle`, `LabeledTextField`, `NumberInputField`, `ConfirmationDialog`, `CancelButton`, `OverviewSection*`, `RowOverflowMenu`, `ResultCard`, `CrossStitchEmptyState`). Do not create local copies. If a shared component lacks a parameter, add the parameter to the shared component.
   - No hardcoded colors or dimensions in `ui/screens`. New values go into theme tokens (`knitToolsColors`, `*Dimens`).
   - No inline `fontSize`, `fontWeight`, `letterSpacing`. Use `Type.kt` roles.
   - `UiConsistencySourceTest` must keep passing **without** growing its exception lists.
2. **Never** use `·`, `•` or ` | ` as separators in any UI text, string resource or layout. Separate with layout (own line, opposite ends of a row, weight/color), or with a comma or parentheses in running text.
3. **Never** add a colored vertical bar on the left edge of a card, row or panel.
4. Every new or changed user-visible string goes into `res/values` **and** all 11 locales: `da, de, es, fi, fr, it, nb, nl, pt, pt-rBR, sv`. Preserve placeholders and plural quantities. No `MissingTranslation` suppression. Finnish UI calls a knitting pattern "ohje", never "kaava". Delete strings (in all locales) that become unused.
5. Code comments in Finnish (project convention). Commit messages in Finnish.
6. Delete dead code in the same change (functions, tokens, strings, imports that become unused).
7. Do **not** commit, push or open a PR unless Emma explicitly asks. If asked to push, push to a feature branch, never `main`.
8. When a rule documented in `CLAUDE.md` / `ui.md` / `AGENTS.md` / `CODEX.md` changes because of this work, update the text in every one of those files where it appears.

### Verification to run at the end of each work package

- `./gradlew :app:testDebugUnitTest` (includes `UiConsistencySourceTest`, `CountPluralResourcesSourceTest`, `ProjectLocalizationSourceTest`, `LocaleSensitiveFormattingSourceTest`, `LayoutDetailsSourceTest`, `InsightsRedesignSourceTest`).
- `./gradlew :app:lintDebug`
- Instrumented tests named in each package, on an emulator.
- Re-take the affected screenshots in English **and** Finnish (the long-string worst case among our locales is usually German; spot-check `de` for anything that changes width).

---

## Work package order

1. **S**: Active work session status (decided, see section S).
2. **A**: Clear bugs (A1 to A10).
3. **B**: Consistent text fields and dialogs (B1 to B5).
4. **C**: Pattern documents and yarn (C1 to C6).
5. **D**: Live counter label, calculators, Insights (D1 to D8).
6. **E**: Small polish (E1 to E4).

Each package should leave the app building and all tests green.

---

## S. Active work session status: move it into the counter hero

### Problem

[04/16](reports/knittools-all-screens-en-2026-11-19/04-counter/16-active-work-session.png): while a work session is running, the status row is not visible. It is rendered as its own `LazyColumn` item **after** the counter hero (`ui/screens/counter/CounterWorkspaceSections.kt:212`, `item(key = "active-work-session")`). The hero is sized to fill the first viewport (`heroLayout.minimumHeight`), so the row lands under the bottom navigation bar. Only a sliver of its rounded background shows above the nav bar.

`CLAUDE.md` requires the active state to be visible "near the counter hero as a compact text row without border, gradient, glow or color stripe". Today it is near the hero in code, but invisible on screen, and it also has a filled `surfaceContainer` background pill, which reads as a card.

### Decision (made for the user's sake)

The session status becomes a **one-line strip at the very top of the counter hero**, above the repeat row. Reasons:

- The knitter looks at the counter while working, so the timer has to sit where their eyes already are, without scrolling.
- Top placement does not push the minus, plus, step and Undo controls down. The hero's measured layout absorbs the 48 dp, so Undo stays above the fold (`ui.md`: "Undo must not fall below the fold when the main buttons are visible").
- A text-only strip, with no background, keeps the hero calm and matches the documented rule.
- The Stop action keeps its own 48 dp touch target in the same line, so stopping does not require opening the project actions sheet.

### Exact behavior

1. **Remove** `item(key = "active-work-session")` from the `LazyColumn` in `CounterWorkspaceSections.kt` (around line 212).
2. **Add** `WorkSessionStrip` (internal composable, same file or a new `CounterWorkSessionStrip.kt` in `ui/screens/counter`).
   - Input: `label: String`, `durationText: String`, `actionLabel: String`, `onAction: () -> Unit`, `contentDescription: String`.
   - Layout: `Row(fillMaxWidth, heightIn(min = CounterDimens.WorkSessionStripMinHeight /* 48.dp */), verticalAlignment = CenterVertically)`.
     - Left, `Modifier.weight(1f)`: label in `labelLarge`, `colorScheme.onSurface`, then the duration in `bodyMedium`, `colorScheme.onSurfaceVariant`, separated by a fixed spacer (`CounterDimens.WorkSessionStripInlineSpacing`, 8 dp). Both are single line with ellipsis. **No separator character.**
     - Right: `TextButton(onClick = onAction)` with `actionLabel`, min height 48 dp.
   - **No** `background`, `clip`, border, shadow, gradient or colored stripe.
   - With `fontScale >= FontScaleThresholds.LARGE` or a width under `CounterDimens.WorkSessionStackBreakpoint`, stack: the label and duration in a column on the left, the button on the right, vertically centered. Do not move the button to its own line.
   - Semantics: `Modifier.semantics(mergeDescendants = false) { contentDescription = <work_session_active_description> }` on the text part. The button stays separately focusable.
3. **Content**:
   - Normal state: label `work_session_active` ("Work session active"); duration from the shared formatter (see point 5); action `work_session_stop_short` (new: "Stop") which calls `actions.onStopSession`.
   - Recovery state (`needsRecoveryReview`): label `work_session_recovery_needed`, duration from the shared formatter; action `work_session_resolve`, which calls `actions.onResolveSessionRecovery`. The label uses `colorScheme.error`. Do not add a background.
   - Drop the "Counter progress: N" text from the strip. It stays in the Stop dialog.
4. **Placement**:
   - Stacked hero (`CounterHero`, `CounterWorkspaceSections.kt:505`): render the strip as the **first child** of the hero `Column`, before `Spacer(Modifier.weight(CounterDimens.CounterHeroTopWeight))`.
   - Side-by-side hero (`ShortCounterHero` + `ShortCounterSupportingControls`): render the strip as the first child of `ShortCounterSupportingControls`, so it sits above Undo and the step ring and stays visible without scrolling.
   - Pass the session data through `CounterHeroState` (add `activeSession: CounterHeroSessionState?` holding `durationSeconds`, `needsRecoveryReview`, `projectName`), filled only when `state.activeSession?.projectId == state.projectId`. This is the same condition used today.
5. **Duration format**: replace the `H:MM:SS` formatter everywhere a session duration is *displayed*.
   - Use `durationText(DurationDisplayFormatter.fromMinutes((seconds / 60).toInt()))` (shared, `ui/components/DurationText.kt`).
   - Under 60 seconds, show the new `work_session_under_minute` ("Under 1 min").
   - Add a small helper `workSessionDurationText(seconds: Long): String` in `ui/components/DurationText.kt`, so the strip, the Stop dialog and the recovery dialog share one path.
   - Delete `formatWorkSessionDuration` from `CounterWorkspaceSections.kt:393` once it is unused. If the recovery "edit total time" input needs seconds, keep a private helper for that input only.
6. **Hero measurement**: `rememberCounterHeroLayout` (`CounterHeroLayout.kt`) must add `CounterDimens.WorkSessionStripMinHeight` to the required height when a session is active, before it chooses roomy, compact or side-by-side. Undo must remain visible at the smallest supported phone height with a session active.
7. **Delete** `ActiveWorkSessionRow`, `WorkSessionStatusText` and the now-unused `CounterDimens.WorkSession*` tokens (`WorkSessionStartPadding`, `WorkSessionEndPadding`, `WorkSessionVerticalPadding`, `WorkSessionStackedSpacing`, `WorkSessionInlineSpacing` if unused). Keep `WorkSessionActionMinHeight` only if reused.
8. **Stop summary dialog** (`CounterScreen.kt:1596`, `SessionStopSummaryDialog`, screenshot [04/18](reports/knittools-all-screens-en-2026-11-19/04-counter/18-stop-work-session.png)):
   - Duration line: `work_session_duration_format` with `workSessionDurationText(...)`. Today it shows "Duration: 0:00:09".
   - "Discard session" button: `ButtonDefaults.textButtonColors(contentColor = colorScheme.error)`. This matches the destructive styling used by the "discard and delete" dialog a few lines above.
   - Replace the neutral `CancelButton` default text with `CancelButton(text = stringResource(R.string.work_session_keep_running))` (new: "Keep running"), because cancelling here keeps the session running.
9. **Recovery dialog** (`CounterScreen.kt:1405-1430`): use `workSessionDurationText` for the confirmed time and the time-while-away lines.

### New strings (all 12 locales)

- `work_session_stop_short`: "Stop"
- `work_session_under_minute`: "Under 1 min"
- `work_session_keep_running`: "Keep running"

### Tests

- Update `WorkSessionComposeTest` and `AuditPart11ComponentsTest`, which import `ActiveWorkSessionRow`, to target `WorkSessionStrip`.
- `AdaptiveCounterTest`: add cases with an active session for (a) a compact phone, 360 x 640 dp, (b) a large font, `fontScale` 2.0, and (c) landscape side-by-side. Each case asserts that the Stop button **and** Undo are displayed without scrolling.
- A unit test for `workSessionDurationText`: 0 s, 59 s, 60 s, 3 599 s, 3 600 s and 3 660 s.

### Docs

- `CLAUDE.md` (and the same rule in `AGENTS.md` and `CODEX.md` if present): change "Aktiivinen tila näkyy projektirivillä ja counter-heron lähellä kompaktina tekstirivinä…" to say that the active state is shown on the project row and as a one-line text strip **at the top of the counter hero**, with no background, border, gradient, glow or color stripe, and that it is included in the hero height measurement.

---

## A. Clear bugs

### A1. Bookmark row overflow button wraps under the text

Screens: [03/19](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/19-bookmarks.png), [03/20](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/20-bookmark-actions.png), [03/21](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/21-rename-bookmark.png).

Cause: `ui/screens/pattern/PatternBookmarkSheet.kt:250`, `PatternBookmarkRow` is a `Column` containing a full-width `TextButton` **and then** the `Box { IconButton(⋮) }`, so ⋮ lands on its own line below the text.

Change:
1. Make the row a `Row(verticalAlignment = CenterVertically)`. Give the jump target `Modifier.weight(1f)` and put the ⋮ `Box` at the end.
2. The text colors are currently all orange, because they inherit the `TextButton` content color. Use:
   - bookmark name: `titleSmall`, `colorScheme.onSurface`
   - page line: `bodySmall`, `colorScheme.onSurfaceVariant`.
3. Remove the separate "Selected bookmark" text line (`pattern_bookmark_selected`). Show selection instead with a trailing `Icons.Default.Check`, tinted `colorScheme.primary`, placed before ⋮ and only when selected. Keep the existing `semantics { selected = … }`. Delete the string `pattern_bookmark_selected` from all locales if it becomes unused (check accessibility usages first).
4. Hide the "Previous bookmark / Next bookmark" row when the list has fewer than 2 bookmarks ([03/17](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/17-bookmarks-empty.png) shows both disabled with no bookmarks).

Accept: ⋮ is on the same line as the name, right aligned; the name is readable dark text; with 0 or 1 bookmarks there is no disabled navigation row.

### A2. Markup palette clips Undo, Redo and Clear page under the PDF

Screens: [03/22](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/22-markup-toolbar.png), [03/24](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/24-markup-drawn.png), [03/25](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/25-clear-annotations.png), [07/27](reports/knittools-all-screens-en-2026-11-19/07-library/27-library-markup.png). The labels are visibly cut in half at the palette's bottom edge.

Cause: `ui/screens/pattern/PatternAnnotationToolbar.kt:135`. The palette `Column` has `heightIn(max = PatternAnnotationTokens.TOOLBAR_MAX_HEIGHT /* 280.dp */)` + `verticalScroll`, and `AnnotationHistoryControls` (line ~276) is the last child, so it falls past 280 dp and is half visible, with no scroll hint.

Change:
1. Move Undo and Redo to the **top app bar** while annotation mode is active, as `IconButton`s with `Icons.AutoMirrored.Filled.Undo` and `Icons.AutoMirrored.Filled.Redo`, placed to the left of "Done". `contentDescription` = the existing `pattern_annotation_undo` / `pattern_annotation_redo`. Enabled states come from `state.canUndo` / `state.canRedo`. This applies to both the project reader and the Library reader, which share the toolbar.
2. Move "Clear page" (`pattern_annotation_clear_page`) into the reader ⋮ menu, visible only in annotation mode and enabled with `state.editableLayerVisible && !state.isSaving`. If clearing the page has no confirmation today, route it through `ConfirmationDialog(isDestructive = true)` with title "Clear this page?" (new `pattern_annotation_clear_page_title`) and body "Remove all your marks on this page? You can undo this." (new `pattern_annotation_clear_page_body`). Adjust the body text if undo does not cover clear-page.
3. Delete `AnnotationHistoryControls` and its call site.
4. Re-check that `TOOLBAR_MAX_HEIGHT` still fits the remaining palette content (tools, colors, width slider, pressure toggle, selection controls). Reduce it if there is now spare space. The palette must not cover more of the page than necessary.

Accept: no clipped text anywhere in the palette at font scales 1.0 and 1.3; Undo and Redo are always visible during markup.

### A3. Size chart tables cut off the fourth column

Screens: [07/05](reports/knittools-all-screens-en-2026-11-19/07-library/05-size-charts.png), [07/06](reports/knittools-all-screens-en-2026-11-19/07-library/06-size-chart-baby.png), [07/07](reports/knittools-all-screens-en-2026-11-19/07-library/07-size-chart-child.png), [07/08](reports/knittools-all-screens-en-2026-11-19/07-library/08-size-chart-men.png).

Cause: `ui/screens/sizecharts/SizeChartScreen.kt:70`: `tableWidth = max(maxWidth, 112.dp * fontScale * headers.size)`, with equal `weight(1f)` columns. With 4 columns that is 448 dp on a 328 dp content area, so it scrolls horizontally with nothing to indicate it. Every cell also repeats " cm", which wastes width.

Change:
1. Move the unit into the header: "Bust (cm)", "Chest (in)" and so on. Add a format string `size_chart_header_with_unit` = "%1$s (%2$s)" and use the existing cm/in unit strings. Cells show the bare number or range ("71–76").
2. Column widths: the Size column is fixed at `SizeChartDimens.SizeColumnWidth` (56 dp × fontScale). Value columns take `weight(1f)` with a minimum of `SizeChartDimens.ValueColumnMinWidth` (88 dp × fontScale). `tableWidth = max(maxWidth, SizeColumnWidth + ValueColumnMinWidth × (columns − 1))`. Put the new tokens in a `SizeChartDimens` object in `ui/theme`.
3. If the table is still wider than the viewport (large font scales), keep the horizontal scroll but pin the Size column. Render it outside the scrolling area, so the row label never scrolls away.
4. Check every category, including Head and Hand, in both metric and imperial.

Accept: at font scale 1.0 on a 360 dp wide phone, all columns of every category are fully visible without scrolling; at 1.5 the Size column stays pinned.

### A4. Reminder sheet shows "Repeat #0, interval 8"

Screen: [02/09](reports/knittools-all-screens-en-2026-11-19/02-project-overview/09-reminders.png), [02/12](reports/knittools-all-screens-en-2026-11-19/02-project-overview/12-reminder-actions.png).

Cause: `ReminderComponents.kt:224-230` joins `row_label_format` ("Count %d") with `CounterValueFormatter.forReminderRepeat(...)`. That resolves to the plural `reminder_repeat_occurrence_format` (`strings.xml:279`), which prints an internal occurrence index ("#0"). Its `one` and `other` forms are also identical.

Change (together with D1, which provides the label helpers):
1. Reminder detail line = `<count text>` + optional `, <interval text>` + optional `, <next text>`:
   - Count text: `mainCounterCountText(MainCounterCountSlot(reminder.targetRow, project.mainCounterLabelType, project.customLabel))` gives "Row 88" / "Round 88" / "Repeat 88" / "<Custom> 88".
   - Interval text (only for repeating reminders), new plurals:
     - `reminder_interval_rows`: one "then every row", other "then every %d rows"
     - `reminder_interval_rounds`: one "then every round", other "then every %d rounds"
     - `reminder_interval_repeats`: one "then every repeat", other "then every %d repeats"
     - `reminder_interval_custom` (string): "then every %1$d (%2$s)"
   - Next text (only for repeating reminders when `currentRow >= targetRow`): `reminder_next_format` = "next: %1$s", where `%1$s` is the count text for the next occurrence. Compute the next occurrence in `ReminderLogic` (add a pure function plus a unit test).
2. Remove `CounterValueDisplay.ReminderRepeat` and the `reminder_repeat_occurrence_format` plural if nothing else uses them (`MultiCounterComponents.kt:271` does today; switch it to the same new helper or delete that branch).
3. The project overview reminder row ([02/02](reports/knittools-all-screens-en-2026-11-19/02-project-overview/02-overview-scrolled.png) shows "Row 88") must use the same helper, so both places read identically.

Accept: "Work cable chart" reads "Row 88, then every 8 rows" in the sheet and in the overview. No "#" and no "Count" anywhere for reminders.

### A5. Reading-line chip covers the text it marks

Screens: [03/14](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/14-reader-line-visible.png), [03/15](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/15-vertical-guide.png), [03/28](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/28-reader-page-two.png). The "Count 86" chip sits on the line at the left margin and hides "Row 2:".

Cause: `PatternViewerScreen.kt:2655`, `ReadingLineRowLabel`. The label is vertically centered on the line (`containerHeight * yFraction - labelHeight / 2`) at `padding(start = 8.dp)`.

Change:
1. Anchor the chip **above** the reading band, right-aligned and ending just left of the drag-handle dot. Offset it up by `labelHeight + PatternViewerDimens.ReadingLineLabelGap` (new token, 4 dp) from the band's top edge.
2. If there is not enough room above (the line is near the top), place the chip **below** the band instead, keeping the same right alignment.
3. Chip text: see D1 (live counter label: "Row 86").
4. Keep the existing accessibility and drag behavior unchanged.

Accept: at any line position the chip does not overlap the band, so the marked line of text is fully readable.

### A6. Photo-to-PDF documents get a timestamp name

Screens: [03/33](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/33-multiple-documents.png) ("pattern-images-1795083818707.pdf"), [03/35](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/35-photo-derived-pdf-reader.png) (title "Pattern-images-179…").

Cause: `ui/screens/pattern/PatternImageImportViewModel.kt:431`, `fileName = "pattern-images-${System.currentTimeMillis()}.pdf"`. The label shown to the user is derived from that file name, and the Documents sheet prints `document.label` raw (`PatternViewerScreen.kt:1141`, `Text(document.label)`), without `PatternDisplayNames.forDisplay`.

Change:
1. Keep a unique technical file name, but set the **document label** to `pattern_images_document_label` (new: "%1$s pattern photos", where %1$s is the project name), passed through `PatternDisplayNames.documentLabel(...)` for the length limit. If a document with the same label already exists in the project, append " 2", " 3" and so on.
   - Finnish: "%1$s, ohjeen kuvat" (or another natural phrasing; never "kaava").
   - Verify where the label is derived. If the label is built from the file name, pass the label separately to the repository.
2. Documents sheet headline: `PatternDisplayNames.forDisplay(document.label)`.
3. Existing rows that already carry a `pattern-images-<digits>.pdf` label: `PatternDisplayNames.forDisplay` should map that pattern to the localized fallback "Pattern photos" (new `pattern_images_document_fallback`). Do not migrate the database.

Accept: a new photo PDF in "Forest Cardigan" is titled "Forest Cardigan pattern photos" in the overview, the Documents sheet and the reader title.

### A7. Empty Saved Patterns promises an action that does not exist

Screen: [11/15](reports/knittools-all-screens-en-2026-11-19/11-onboarding/15-empty-saved-patterns.png). The text says "Add a web pattern or import a PDF", but the only button is "Add Web Pattern". The Library has no PDF import of its own. PDFs arrive by attaching them to a project or by opening a PDF with KnitTools.

Change:
1. `empty_saved_patterns`: split into title `empty_saved_patterns_title` "No saved patterns yet" and body `empty_saved_patterns_body` "Save a link to a pattern website here. PDFs you attach to projects or open with KnitTools appear here too." Delete the old key.
2. Render with `CrossStitchEmptyState(glyph = CrossStitchGlyph.PATTERN, title, message, action = { LabeledCounterImageButton(... web_pattern_add ...) })`. To make that possible, add an optional `action: (@Composable () -> Unit)? = null` slot to `CrossStitchEmptyState` (`ui/components/CrossStitchIcon.kt:217`), rendered under the message with `CrossStitchDimens.EmptyStateActionGap` (new token, 16 dp).

### A8. Text fields show an error immediately and turn dark

Screens: [01/03](reports/knittools-all-screens-en-2026-11-19/01-projects/03-create-folder.png) (Create folder), [01/20](reports/knittools-all-screens-en-2026-11-19/01-projects/20-new-project-custom-counter.png) (Custom label), [03/18](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/18-add-bookmark.png) (Add bookmark).

Two causes:
- Validation is shown before the user has done anything:
  - `ProjectFolderComponents.kt:240` shows `validationMessage` for an empty initial name.
  - `ProjectDetailsSheet.kt:447` shows `main_counter_custom_label_error` as soon as Custom is selected.
  - `PatternBookmarkSheet.kt:340` sets `isError = !valid`, which includes empty.
- `ui/components/TextFieldColors.kt`, `fieldColors()` does not set `errorContainerColor` or `errorIndicatorColor`, so Material falls back to the dark `errorContainer` fill and a red underline. `ui.md` notes that a darker field "read as disabled".

Change:
1. Add a "touched" rule for all three, and for any other form found with the same pattern: show the validation message only after (a) the user typed and the value is now invalid, or (b) the field lost focus after having had focus, or (c) a confirm or collapse attempt.
   - Keep the confirm button disabled while the value is invalid.
   - For `ProjectDetailsSheet`, the existing rule "a blocking error cannot be hidden by collapsing More options" stays. If the user collapses with an invalid custom label, mark the field touched, show the message, and keep the section open.
   - Store `touched` in `rememberSaveable`.
2. In `fieldColors()` set `errorContainerColor = container`, `errorIndicatorColor = transparentIndicator`, `errorCursorColor = colorScheme.error`, `errorLabelColor = colorScheme.error`, `errorSupportingTextColor = colorScheme.error`. In `LabeledTextField` / `NumberInputField`, when `isError`, draw the border with `colorScheme.error` (1 dp, or 2 dp while focused) instead of `primaryContainer`. The field must never change its fill color because of an error.

Accept: opening each of the three dialogs shows a neutral empty field and a disabled confirm; the field fill is identical in normal, focused and error states; the error shows a red border and red supporting text.

### A9. Delete-folder dialog says "0 projects will be kept"

Screen: [01/07](reports/knittools-all-screens-en-2026-11-19/01-projects/07-delete-folder-confirmation.png). Plural at `strings.xml:1165`.

Change: when the folder has 0 projects, use the new `folder_delete_empty_message` "This folder is empty." instead of the plural line. Keep the plural for 1 or more.

### A10. Yarn Estimator "Choose from My Yarn" does not choose anything

Screens: [06/12](reports/knittools-all-screens-en-2026-11-19/06-tools/12-yarn-estimator.png), [06/13](reports/knittools-all-screens-en-2026-11-19/06-tools/13-estimator-yarn-picker.png).

Cause: `ui/navigation/NavGraph.kt:745`, `onSavedYarns = { navController.navigateSingleTopTo(Screen.MyYarn.route) }`. This opens the full Library My Yarn screen: the bottom-nav highlight jumps from Tools to Library, tapping a yarn opens its detail page, and nothing ever returns to the estimator. The label "Choose from My Yarn" promises a selection.

Change:
1. Extract the private `YarnPickerSheet` from `CounterScreen.kt` ("Select yarn card", around line 2140) into a shared `ui/components/YarnCardPickerSheet.kt` (`SheetTitle`, `SheetOptionRow`, `colorScheme.surface`). Use it from `CounterScreen` and from the estimator.
2. `YarnEstimatorScreen` opens that sheet in place (no navigation). Its ViewModel exposes the yarn cards from `YarnCardRepository`. On selection, prefill "Length per skein" and "Weight per skein" from the card, converting to the estimator's current unit system. Leave "Total yarn needed" untouched.
3. If the selected card has neither length nor weight per skein, keep the fields unchanged and show a snackbar: "This yarn has no length or weight per skein yet." (new `yarn_estimator_card_missing_values`).
4. Remove the `onSavedYarns` parameter and the `Screen.MyYarn` navigation from the estimator route.

Accept: Tools stays highlighted; choosing "North Mill Willow DK" fills 220 m and 100 g; the user stays on the estimator.

---

## B. Consistent text fields and dialogs

### B1. One text-field style in all dialogs

Screens:
- [01/03](reports/knittools-all-screens-en-2026-11-19/01-projects/03-create-folder.png) and [01/06](reports/knittools-all-screens-en-2026-11-19/01-projects/06-rename-folder.png): same dialog, different look depending on whether there is text.
- [03/07](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/07-rename-document.png): rename document.
- [03/13](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/13-page-jump.png): page jump.
- [03/18](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/18-add-bookmark.png) and [03/21](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/21-rename-bookmark.png): bookmarks.
- [04/03](reports/knittools-all-screens-en-2026-11-19/04-counter/03-target-rows.png): OutlinedTextField with a notched label.
- [04/08](reports/knittools-all-screens-en-2026-11-19/04-counter/08-rename-extra-counter.png): rename counter.
- [04/13](reports/knittools-all-screens-en-2026-11-19/04-counter/13-stitches-per-row.png): stitches per row.

Today there are four visual variants: an inside label, a filled field with an underline, a square field and an outlined field.

Change: in every single-field or form **dialog**, use `LabeledTextField` for text and `NumberInputField` for numbers (label above, 14 dp shape, the `inputFieldContainer` fill, which per `ui.md` is lighter than the dialog surface).
- To support the existing helper lines, add `supportingText: String? = null` to `LabeledTextField`, and to `NumberInputField` if missing. The helper lines are "Maximum 50 characters" and "Page 1 of 2". The error message replaces the supporting text.
- Add `enabled: Boolean = true` where a caller needs it (bookmark saving).
- Call sites to convert (grep `TextField(` / `OutlinedTextField(` in dialogs):
  - `ProjectFolderComponents.kt:256` (create and rename folder)
  - `PatternBookmarkSheet.kt:336`
  - `PatternViewerScreen.kt:1020` (rename document)
  - `PatternViewerScreen.kt:1994` (page jump, use `NumberInputField`)
  - `MultiCounterComponents.kt:352` (rename counter)
  - `TargetRowsDialog.kt:46` (`NumberInputField`)
  - the stitches-per-row dialog (`NumberInputField`)
  - `PatternAnnotationToolbar.kt:352/391/397/450/459/499` (text, callout and chart tracker dialogs).
- The calibration field `PatternViewerScreen.kt:1358` is inline, not in a dialog. Convert it to `NumberInputField` too.
- After conversion, delete `dialogTextFieldColors()` if it has no remaining users (11 usages today).
- Add a `UiConsistencySourceTest` rule: no raw `TextField(` or `OutlinedTextField(` in `ui/screens` outside `SearchTextField` and the notes editor. Use the existing exception mechanism only for those two.

### B2. Segmented toggle track is invisible inside dialogs

Screens: [02/10](reports/knittools-all-screens-en-2026-11-19/02-project-overview/10-add-reminder.png), [02/11](reports/knittools-all-screens-en-2026-11-19/02-project-overview/11-repeating-reminder.png), [02/13](reports/knittools-all-screens-en-2026-11-19/02-project-overview/13-edit-reminder.png). One-time/Repeating looks like a lone pill with floating text.

Cause: `ui/components/SegmentedToggle.kt:58` paints the track with `colorScheme.surfaceVariant`, which equals the dialog surface in the light theme.

Change: add a token `knitToolsColors.segmentedTrackOnDialog` (light and dark values with at least 3:1 contrast to the dialog surface for the track edge, similar to the sheet look in [04/09](reports/knittools-all-screens-en-2026-11-19/04-counter/09-add-counter.png)), and a parameter `SegmentedToggle(surface: SegmentedToggleSurface = Default)` with `Default` and `Dialog` options. Pass `Dialog` from every `SegmentedToggle` inside an `AlertDialog` or `ScrollableFormDialog` (grep; the reminder dialog is one of them). Do not change sheets.

### B3. Dialog titles, confirm verbs and destructive wording

Make these exact changes (new keys where a label is shared with menus):

- Complete project dialog ([02/05](reports/knittools-all-screens-en-2026-11-19/02-project-overview/05-complete-project.png)): title `complete_project_title` "Complete project?" (menu item and button keep "Complete").
- Delete project dialog ([02/06](reports/knittools-all-screens-en-2026-11-19/02-project-overview/06-delete-project-confirmation.png)): title `delete_project_title` "Delete project?".
- Delete yarn card ([08/08](reports/knittools-all-screens-en-2026-11-19/08-yarn-and-photos/08-delete-yarn-confirmation.png)): title `delete_yarn_card_title` "Delete yarn card?".
- Delete photo ([08/17](reports/knittools-all-screens-en-2026-11-19/08-yarn-and-photos/17-delete-progress-photo.png)): title `delete_photo_title` "Delete photo?", body "Delete this photo? This cannot be undone." (reuse the existing body key if it already says this).
- Rename extra counter ([04/08](reports/knittools-all-screens-en-2026-11-19/04-counter/08-rename-extra-counter.png)): title `rename_counter_title` "Rename counter", confirm `save` (not `confirm`).
- Set target ([04/03](reports/knittools-all-screens-en-2026-11-19/04-counter/03-target-rows.png)): confirm `save` (not `ok`). Keep "Remove target".
- Stitches per row ([04/13](reports/knittools-all-screens-en-2026-11-19/04-counter/13-stitches-per-row.png)): "Delete" becomes `stitches_per_row_remove` "Remove". Field label `stitches_per_row_label` "Stitches per row".
- Page jump ([03/13](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/13-page-jump.png)): confirm `pattern_page_jump_go` "Go".
- Remove last document ([03/08](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/08-remove-document-confirmation.png)): change `project_documents_remove_last_message` to "Remove %1$s? This project will no longer have a pattern PDF to read."
- Unlink pattern ([03/09](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/09-unlink-pattern-confirmation.png)): title `web_pattern_unlink_title` "Unlink pattern?", body `web_pattern_unlink_body` "Remove the pattern details for %1$s from this project? The saved pattern and this project's PDFs are kept.", confirm `web_pattern_unlink_confirm` "Unlink". Verify against `CLAUDE.md` that unlink touches only pattern information and never PDFs (it does). Use `ConfirmationDialog` (not destructive red: nothing is deleted).
- All plain confirmations must go through `ConfirmationDialog` (`ui.md`). Grep for remaining custom `AlertDialog`s that only confirm, and convert them.

Rule to apply everywhere (add it to `ui.md` as well): a dialog title for a destructive or state-changing confirmation is a question ending in "?"; the confirm button repeats the verb ("Delete", "Remove", "Unlink", "Complete"); forms confirm with "Save".

### B4. Same sheet, two names, and "Attach to project" casing

Screens: [03/01](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/01-attach-pattern.png) "Attach pattern", [03/30](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/30-add-another-pattern.png) "Add Pattern"; [07/18](reports/knittools-all-screens-en-2026-11-19/07-library/18-web-pattern-detail.png) "Attach to project" and [07/23](reports/knittools-all-screens-en-2026-11-19/07-library/23-pdf-pattern-detail.png) "Attach to Project".

Change (`PatternPickerSheet.kt:546`):
- `INITIAL_PROJECT_PATTERN` uses the title `attach_pattern`, changed to "Add Pattern".
- `ADD_READABLE_PROJECT_DOCUMENT` uses the new `project_content_add_document` "Add Document". The user already has a pattern, so this sheet adds another PDF document. Check whether `project_content_add_pattern` is still used elsewhere (`CounterProjectContentCards.kt:81`) before renaming or deleting it.
- Detail rows: `saved_pattern_detail_attach_to_project` and `web_pattern_attach` both become "Attach to project" (sentence case, one key; delete the duplicate). The `ProjectPickerSheet` title stays "Attach to Project" (sheet title).

### B5. New project sheet destination line

Screen: [01/17](reports/knittools-all-screens-en-2026-11-19/01-projects/17-new-project.png) "New project destination: Unfiled".

Change `folder_project_creation_destination` to "Saved in %1$s". Same placement and style.

---

## C. Pattern documents and yarn

### C1. Pattern and documents section in the project overview

Screens: [03/04](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/04-overview-with-pdf.png), [03/38](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/38-new-project-from-pdf.png). The rows read "Woodland Cardigan / PDF pattern, page 1", "Documents" (no subtitle), "Woodland Cardigan / Web pattern". The same name appears twice and "Documents" is unexplained.

Change (`ProjectOverviewSections.kt:198`):
- The "Documents" row becomes `project_documents_manage` "Manage documents" with the plural subtitle `project_documents_count` (one "%d PDF", other "%d PDFs").
- Keep the primary PDF row (title + "PDF, page N") and the web pattern row (title + "Web pattern").
- Order: web pattern information row first (if any), then the primary PDF row, then "Manage documents".

### C2. Documents sheet content

Screens: [03/05](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/05-project-documents.png), [03/06](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/06-document-actions.png), [03/33](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/33-multiple-documents.png), [03/34](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/34-secondary-document-actions.png).

Today each PDF row lists "Primary pattern / Position 1 of 1 / Current document / PDF available" (`PatternViewerScreen.kt:1143-1160`). That is four lines of internal state. The pattern information block shows "Open website", "Edit web pattern" and "Unlink pattern information" as equal chevron rows, so the destructive-ish Unlink looks like navigation.

Change:
1. **Pattern information block**:
   - Keep the `SectionLabel` header ("Pattern information") and the name, designer, type and site lines.
   - "Open website" stays the single navigation row, with a trailing `Icons.AutoMirrored.Filled.OpenInNew` instead of a chevron, because it leaves the app.
   - Move "Edit web pattern" and "Unlink" into a `RowOverflowMenu` (⋮) at the right of the pattern name line. Unlink opens the B3 dialog.
2. **PDF documents block**:
   - Add `SectionLabel("PDF documents")` (new `project_documents_pdf_section`), aligned to the same 16 dp start as the information block. Today the PDF rows are indented to 32 dp.
   - Row headline: `PatternDisplayNames.forDisplay(label)`.
   - Supporting content is at most one line, built from these parts:
     - "Main pattern" (rename the string `project_documents_primary` to this text; primary only)
     - "page %d" (last read page, if known)
     - an "Open now" badge (`BadgePill`, new `project_documents_open_now`), only on the document currently open in the reader.
   - When the document file is unavailable, show `project_documents_unavailable` in `colorScheme.error` on its own line.
   - Join the text parts with ", ". The badge is laid out after the text.
   - Delete `project_documents_secondary`, `project_documents_position`, `project_documents_available` and `project_documents_open` if unused, together with their translations. **Keep** the custom accessibility actions and their labels (move earlier, move later, set primary, remove). Position information can live in the merged content description: "Woodland Cardigan, main pattern, 1 of 2".
   - In the per-document ⋮ menu, hide "Move earlier" and "Move later" when there is only one document, instead of showing them disabled ([03/06](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/06-document-actions.png)).

### C3. Reader top bar and menus

Screens: [03/10](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/10-project-pdf-reader.png), [03/12](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/12-reader-options.png), [03/26](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/26-calibrate-counter-position.png), [03/27](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/27-calibrate-second-position.png).

Today the reading-line button (`PatternViewerScreen.kt:1717`, `Icons.Default.HorizontalRule`) is a plain dash that reads as "minimize", and it opens the same 11-item menu as ⋮.

Change (respect `ui.md`: the visible reading-line button anchors the `READING_LINE` tip and opens a menu that contains the current settings, without changing the line values):
1. New vector drawable `ic_reading_line.xml`: a horizontal line with a small filled circle at its right end, matching the on-page overlay. Size 24 dp, tinted `onSurfaceVariant`. Do not use a generic Material icon here.
2. Split the menus:
   - **Reading-line button menu**: Show/Hide reading line; Follow counter / Stop following; Set marker here; Clear row mark; Clear page marks; Show/Hide vertical guide; Center vertical guide; Calibrate counter positions.
   - **⋮ menu**: Page jump; Documents; Open project notes; Annotation layers; Export annotated PDF (plus Clear page in annotation mode, see A2).
   - Hide items that are disabled because they do not apply (for example "Center vertical guide" while the guide is hidden) instead of showing them disabled.
3. Copy (live label, see D1):
   - `pattern_set_row_marker_here` becomes "Mark %1$s here" with `%1$s` = "row 86" (lower-case count text from a new helper `mainCounterCountTextInline`, or the new strings `current_row_inline` "row %d", `current_round_inline` "round %d", `current_repeat_inline` "repeat %d").
   - `pattern_follow_current_row_off` becomes "Stop following counter".
4. **Calibration** ([03/26](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/26-calibrate-counter-position.png) and [03/27](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/27-calibrate-second-position.png)):
   - Read the calibration logic in the ViewModel first. Then add one instruction line above the field for each step, and a step indicator, describing what the user must physically do (move the line onto a row, enter that row's number).
   - Proposed copy, to adjust to match the real logic: step 1 `pattern_calibration_step_first` "Step 1 of 2: move the line onto the row you're on, check the number, then save." and step 2 `pattern_calibration_step_last` "Step 2 of 2: move the line onto a later row, enter its number, then save."
   - Buttons: "Save first" / "Save last" become `save`, and the step indicator carries the order. Delete `pattern_calibration_save_first` / `_last` if unused.

### C4. Project yarn sheet

Screens: [08/20](reports/knittools-all-screens-en-2026-11-19/08-yarn-and-photos/20-project-yarn.png), [08/24](reports/knittools-all-screens-en-2026-11-19/08-yarn-and-photos/24-project-yarn-with-usage.png), [08/29](reports/knittools-all-screens-en-2026-11-19/08-yarn-and-photos/29-overview-yarn-amounts.png).

Problems:
- Two linked yarns render as identical "North Mill Willow DK" rows: the label lacks the colorway. `CounterViewModel.kt:719` builds `id to card.displayName(...)` only.
- The project-yarn card glues fields into one comma string (`YarnManagementSheet.kt:392`, `joinToString(", ")`).
- A permanent explanation line, "This project entry stays separate. Its notes were not copied to the My Yarn card.", sits under a saved entry (`YarnManagementSheet.kt:367`).

Change:
1. Replace `linkedYarns: List<Pair<Long, String>>` with `List<LinkedYarnSummary>` (`id`, `name`, `colorLine: String?`, where `colorLine` = color name and/or color number using the existing short formats `yarn_color_number_short`), and update all callers (`CounterScreen.kt:710`, `:1886`, `:1940`, the reducers).
   - `LinkedYarnRow` shows the name, then `colorLine` on a second line in `bodySmall`/`onSurfaceVariant`, aligned with the name (past the color dot).
2. Overview yarn section: verify that **every** linked yarn is listed. In [08/29](reports/knittools-all-screens-en-2026-11-19/08-yarn-and-photos/29-overview-yarn-amounts.png) only one of the two linked Willow DK cards appears next to "Contrast yarn" while the sheet lists both. If the overview limits the count on purpose, add the same "See all" header action that Reminders uses; otherwise fix the missing row.
3. Project-yarn card body: each present field on its own line, without "Label:" glue for free text:
   - "Quantity: 2" (keep the label: it is a number)
   - the description line
   - notes (max 2 lines, ellipsis).
4. Remove the permanent `saved_to_my_yarn_explanation` text. Show it once as a snackbar right after "Save to My Yarn" succeeds. Keep the `BadgePill` "Saved to My Yarn". Keep `save_to_my_yarn_explanation` under the button before saving, because it explains the action.

### C5. Yarn usage unit choice when grams or skeins need a conversion

Screens: [08/21](reports/knittools-all-screens-en-2026-11-19/08-yarn-and-photos/21-track-yarn-usage.png), [08/22](reports/knittools-all-screens-en-2026-11-19/08-yarn-and-photos/22-yarn-usage-grams.png).

After the user taps Grams without a skein conversion, Meters stays selected and Grams looks "pressed grey". The required-conversion message appears twice (`ProjectYarnUsageSheet.kt:123` and `:150`).

Change:
1. Add `pendingIndex: Int? = null` to `SegmentedToggle`. The pending option is drawn with a 1.5 dp `colorScheme.primary` outline and normal text, which is distinct from both selected and unselected. Add `semantics { stateDescription = "Needs skein details" }` (new `segmented_option_pending_state`).
2. When `draft.pendingUnit != null`: open the conversion fields (already happens), move focus to "Length per skein", and bring it into view. Show `yarn_usage_conversion_required` **once**, directly above the conversion fields. Remove the other occurrence.

### C6. Delete photo dialog and photo captions

Covered by B3 (title and body). Photo captions keep the neutral "Count N", because photos store a historical number; see "Out of scope".

---

## D. Live counter label, calculators, Insights

### D1. Live counter values use the project's own label

Today the same live value is called "Row 88" (overview), "Count 88" (reminder sheet), "Counter value" (reminder dialog), "counts" (repeat suffix) and "Count 86" (reader bottom bar and line chip):
[02/02](reports/knittools-all-screens-en-2026-11-19/02-project-overview/02-overview-scrolled.png), [02/09](reports/knittools-all-screens-en-2026-11-19/02-project-overview/09-reminders.png), [02/10](reports/knittools-all-screens-en-2026-11-19/02-project-overview/10-add-reminder.png), [02/11](reports/knittools-all-screens-en-2026-11-19/02-project-overview/11-repeating-reminder.png), [03/10](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/10-project-pdf-reader.png), [03/14](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/14-reader-line-visible.png).

Rule: a **live** value of the main counter (current or target number in the current project) uses `mainCounterCountText(MainCounterCountSlot(count, labelType, customLabel))` from `ui/components/MainCounterDisplayText.kt`. **Historical** values (sessions, photos) keep the neutral wording (`ui.md`: units are not stored historically).

Change:
1. `PatternViewerScreen.kt:2682` (line chip) and `:2745` (bottom bar): use `mainCounterCountText`. Plumb `mainCounterLabelType` and `customLabel` of the open project into the viewer UI state. The Library reader has no counter and is unaffected.
2. Reminder dialog (`ReminderComponents.kt:368-388`):
   - `row_number` "Counter value" becomes the new `reminder_at_label` "Remind at".
   - The field gets the unit suffix from the new strings `counter_unit_rows` "rows", `counter_unit_rounds` "rounds", `counter_unit_repeats` "repeats", or the custom label as-is.
   - "Repeat every" uses the same suffix in place of `repeat_every_rows` "counts".
   - Delete `row_number` and `repeat_every_rows` if unused.
3. Reminder list and overview: see A4.
4. `row_label_format` ("Count %d") then remains only for photos (`PhotoComponents.kt:148`, `PhotoGalleryScreen.kt:409`, `AllPhotosScreen.kt:349`, `ProjectOverviewSections.kt:282`). Rename it to `photo_count_label_format` so nobody reuses it for live values. Keep the English text.

### D2. Calculator results repeat themselves

- Cast On ([06/09](reports/knittools-all-screens-en-2026-11-19/06-tools/09-cast-on-result.png)): "110 stitches", then "Adjusted down: 110 st (50.0 cm)", then an info note "Edge stitches total: 2".
  - In `CastOnScreen.kt:219-231`, show "Adjusted down" / "Adjusted up" only when that stitch count differs from `result.stitches`.
  - Remove the `edge_stitches_total_format` info note: it repeats an input the user just typed. Delete the string if unused.
- Yarn Estimator ([06/14](reports/knittools-all-screens-en-2026-11-19/06-tools/14-yarn-estimator-result.png)): "6 skeins" with "Estimated: about 6.00 skeins" below.
  - In `YarnEstimatorScreen.kt:210`, show the exact line only when `exactSkeins` is not a whole number. Use at most 1 decimal and the new copy `estimated_skeins` "Exact need: %s skeins" (e.g. "Exact need: 5.4 skeins").

### D3. Convert measurements result uses the shared result card

Screen: [06/18](reports/knittools-all-screens-en-2026-11-19/06-tools/18-convert-measurements-result.png). The result is plain text with a full-width filled "Copy result" button; Cast On and Yarn Estimator use `ResultCard`.

Change (`ui/screens/gauge/GaugeComponents.kt:136`, `GaugeResults`):
- Wrap the results in `ResultCard`.
- For sections with a single result line (conversion, and any other single-value result), render the value in the same style as Cast On's main number (`displayMedium`, `colorScheme.primary`), with the input echo ("42 cm") as a `bodyMedium` line under it.
- Multi-line sections keep their line list inside the card.
- "Copy result" becomes a `TextButton` at the card's bottom end, which keeps the `measurement_copy` test tag.

### D4. Calculator example values look like real input

Screens: [06/02](reports/knittools-all-screens-en-2026-11-19/06-tools/02-increase-decrease.png), [06/12](reports/knittools-all-screens-en-2026-11-19/06-tools/12-yarn-estimator.png). The example values 96 / 12 / 1200 differ from real values only by a shade, while the placeholder below says "Enter valid stitch counts".

Keep the example values (decided in `ui.md`), but:
1. Render numeric placeholders as `field_example_format` "e.g. %1$s" (new) inside `NumberInputField` when `NumberInputOptions.placeholder` is set.
2. Change the result placeholder copy for Increase/Decrease to "Enter your stitch counts to see where to increase" / "…decrease". Check the existing keys; "valid" implies the user did something wrong.

### D5. Counter History rows

Screen: [04/05](reports/knittools-all-screens-en-2026-11-19/04-counter/05-counter-history.png). Every row says "Counter changed" and shows the time with seconds ("11:33:56 AM").

Change (`ui/screens/counterhistory/CounterHistoryScreen.kt:123`):
- Use the locale's short time format, without seconds (find where `row.time` is built and use `DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)` through the existing locale-aware date helpers; `LocaleSensitiveFormattingSourceTest` must pass).
- When the action is `CHANGED`, hide the action line: the "85 → 86" transition already says it.
- Keep the merged content description.

### D6. Insights project rows glue two facts

Screens: [05/03](reports/knittools-all-screens-en-2026-11-19/05-insights-and-sessions/03-this-week.png), [05/12](reports/knittools-all-screens-en-2026-11-19/05-insights-and-sessions/12-insights-time-by-project.png). "Counter progress: 28, today".

Keep the neutral "Counter progress" wording (decided). In `InsightsSections.kt:430`, render two lines:
- `counter_progress_format`
- `insights_last_worked_format` ("Last worked today"), both `insightsProjectSub` style.

Delete `insights_project_sub_format` if unused. Check that row height tokens in `InsightsDimens` still give correct spacing.

### D7. Project completions chart shows empty days

Screen: [05/10](reports/knittools-all-screens-en-2026-11-19/05-insights-and-sessions/10-this-month.png). "0 completions" labels for Nov 1, 2, 3, while the only real bar (Nov 19) is scrolled out of view.

Change (`InsightsCompletionSection.kt:114`): build the timeline from buckets with `count > 0` only. If fewer than `MINIMUM_CHART_BARS` non-empty buckets remain (reuse the existing constant), omit the timeline entirely and keep only the completion list. This matches the rule "a single bar is always full height and says nothing". Update `InsightsRedesignSourceTest` if it asserts the timeline structure.

### D8. Stop dialog and durations

Covered in S (points 8 and 9).

---

## E. Small polish

### E1. Settings help entries

Screen: [09/04](reports/knittools-all-screens-en-2026-11-19/09-settings-and-pro/04-settings-scrolled.png). "How to Use KnitTools" and "Help & Guide" sit next to each other; the second opens an external web page (`SettingsScreen.kt:198`) but looks like an in-app page.

Change:
- `help_and_guide` becomes "Online help".
- Add `trailingIcon` support to `SettingsActionRow` (`SettingsScreen.kt:282`) and use `Icons.AutoMirrored.Filled.OpenInNew` instead of the chevron for this row.
- Append `external_link_description` ("opens in browser", new) to its content description.

### E2. Unified empty states for Library lists

Screens: [11/15](reports/knittools-all-screens-en-2026-11-19/11-onboarding/15-empty-saved-patterns.png), [11/16](reports/knittools-all-screens-en-2026-11-19/11-onboarding/16-empty-my-yarn.png), compared with [08/11](reports/knittools-all-screens-en-2026-11-19/08-yarn-and-photos/11-all-photos-empty.png).

Change:
- Saved Patterns: see A7.
- My Yarn (`MyYarnScreen.kt:245`): `CrossStitchEmptyState(glyph = YARN, title = "No yarn saved yet" (new `empty_my_yarn_title`), message = "Save yarn once with its color and dye lot, then link it to any project." (new `empty_my_yarn_body`), action = Add Yarn pill)`.
- Do **not** change the Insights empty state: it is editorial by design (`ui.md`).

### E3. Web pattern disclaimers repeated three times

Screens: [07/17](reports/knittools-all-screens-en-2026-11-19/07-library/17-add-web-pattern.png), [07/18](reports/knittools-all-screens-en-2026-11-19/07-library/18-web-pattern-detail.png), [07/21](reports/knittools-all-screens-en-2026-11-19/07-library/21-edit-web-pattern.png). The detail page shows three separate lines: "This link opens the original website", "The page is not saved for offline use", "Website availability is controlled by the source".

Change: on the detail page, show one line, `web_pattern_offline_note` "Opens on the original website. Not saved for offline use." (new). In the add and edit sheets, keep only the same single line. Delete the three old keys if unused. `CLAUDE.md` terminology for web patterns (verkko-ohje, never PDF or offline) still applies to the Finnish copy.

### E4. Pro page purchase-status error has no retry

Screen: [09/08](reports/knittools-all-screens-en-2026-11-19/09-settings-and-pro/08-knittools-pro.png). "Something went wrong — try again" with nothing to tap (`ProUpgradeScreen.kt:220`, `purchaseQueryFailed`).

Change: under the message, add `TextButton(R.string.retry)`, which re-runs the purchase-state query (add `retryPurchaseState()` to `ProUpgradeViewModel`, delegating to the existing `ProManager` refresh path; verify the method name). Keep "Restore purchases" below it. Do not change the price-tag or offer logic.

---

## Verified and intentionally not changed

These came up in the screenshot pass but are documented decisions or capture artifacts. Do not change them:

- English screen titles in Title Case (`ui.md`).
- Neutral "Counter progress" wording in sessions and Insights, and "Count N" on photos (historical units are not stored).
- Insights order (the trend line sits under the stats) and the "Every day" section label.
- Calculator example values (only their presentation changes, see D4).
- Clay-style 3D illustrations in onboarding and on the Pro page next to the cross-stitch icons in-app (documented in `ui.md`).
- "Continue knitting" on a new project at row 0 ([03/38](reports/knittools-all-screens-en-2026-11-19/03-pattern-and-documents/38-new-project-from-pdf.png)): `ContinueProjectCard` wording is documented.
- "COMPLETED 0 / No completed projects yet" in a folder ([01/24](reports/knittools-all-screens-en-2026-11-19/01-projects/24-folder-projects.png)): shown only because "Show Completed" is on.
- Blank Ravelry browser ([06/23](reports/knittools-all-screens-en-2026-11-19/06-tools/23-ravelry-browser.png)) and the empty toast pill ([10/08](reports/knittools-all-screens-en-2026-11-19/10-guide/08-show-tips-again.png)): emulator or capture artifacts. Re-check on a networked device; if Ravelry stays blank with network, file a separate issue.
- "Test preview" text on the onboarding price step ([11/11](reports/knittools-all-screens-en-2026-11-19/11-onboarding/11-pro-offer.png)): it lives only in `src/debug/res`.
- Launch-price end time shown with minute precision and time zone (`ui.md`).

---

## Final checklist for the implementer

- [ ] All packages S and A to E done, or explicitly reported as skipped with a reason.
- [ ] Every new or changed string exists in all 12 value folders; deleted keys are gone from all of them.
- [ ] No `·`, `•` or ` | ` separators introduced anywhere.
- [ ] `UiConsistencySourceTest` exception lists did not grow.
- [ ] Unit tests, lint and the named instrumented tests are green.
- [ ] Affected screenshots re-taken in English and Finnish, saved to `reports/knittools-all-screens-en-<date>/` with the same numbering, and a short before/after list in the final report.
- [ ] `CLAUDE.md` / `ui.md` / `AGENTS.md` / `CODEX.md` updated where a documented rule changed (S, B1, B3, C3, D1).
- [ ] No commit, push or PR unless Emma asked for it.
