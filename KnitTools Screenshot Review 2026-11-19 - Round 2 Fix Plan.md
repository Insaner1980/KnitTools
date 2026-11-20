# KnitTools Screenshot Review 2026-11-19: Round 2 Fix Plan

Follow-up to `KnitTools Screenshot Review 2026-11-19 - Implementation Plan.md`. That plan has been implemented (uncommitted working tree on branch `codex/paikalliset-muutokset-20261119`). This round fixes what the new screenshots show.

Source screenshots: `reports/knittools-all-screens-en-2026-11-19-after/` (`en/`, `fi/`, `de/`), 198 images.
References use **LANG NN/MM** = language, folder number, file number. Each is a link.

Implementer: Codex. Owner and reviewer: Emma.

---

## 0. Ground rules

Everything in section 0 of the first plan still applies. In short:

- `CLAUDE.md`, `AGENTS.md`, `CODEX.md` and `.claude/rules/ui.md` are binding.
- Use shared components; no local copies; no hardcoded colors or dimensions in `ui/screens`; use Type.kt roles only.
- Never use `·`, `•` or ` | ` as separators. Never add a colored left-edge bar.
- Every new or changed string goes into `res/values` and all 11 locales. Delete unused strings in every locale. Finnish calls a knitting pattern "ohje", never "kaava".
- Write code comments and commit messages in Finnish.
- Delete dead code in the same change.
- `UiConsistencySourceTest` exception lists must not grow.
- No commit, push or PR unless Emma asks.

Verification after each package:

- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:lintDebug`
- the instrumented tests named in the item
- re-take the listed screenshots in EN and FI, plus DE where width matters

**Screenshot capture hygiene.** Three images in the last set were not usable as evidence. Re-take them, and wait for enter animations to finish before capturing:

- EN 03/08: rendered without `KnitToolsTheme`; this looks like a test-harness capture.
- EN 03/25: captured mid-fade.
- EN 06/18: no bottom-nav tab highlighted.

---

## Work package order

1. **F**: resource organization (do first, everything else adds strings).
2. **A**: bugs (A1 to A6).
3. **B**: copy and logic (B1 to B7).
4. **C**: Finnish terminology (C1 to C3).
5. **D**: typography, spacing and color (D1 to D9).
6. **E**: items the screenshots could not verify (E1 to E4).

---

## F. Resource organization

### F1. Move the `strings_screenshot_*.xml` keys into real resource files

Today new keys live in `res/values*/strings_screenshot_forms.xml`, `strings_screenshot_pattern.xml`, `strings_screenshot_review.xml` and `strings_screenshot_session.xml`. Those names describe the review that produced the keys, not the feature they belong to. Some existing keys were moved there too (for example `help_and_guide` and `estimated_skeins`), so a maintainer searching by feature will not find them.

Change:
1. Move every key back into `strings.xml` next to related keys. Pattern-reader keys may go into the existing `pattern_reading_assistance.xml`. Do this for `values` and all 11 locales.
2. Delete the four `strings_screenshot_*.xml` files in every locale.
3. Keep key names unchanged unless an item below renames them.
4. Also verify `values-fi/pattern_annotations.xml`: it has no `values/pattern_annotations.xml` counterpart (the default strings live in `values/strings.xml`). Make the file layout consistent across locales, so every locale uses the same file for the same keys. `ProjectLocalizationSourceTest` and `OnboardingResourcesSourceTest` must pass.

Accept: no file name under `res/values*` contains "screenshot"; `git grep -n "strings_screenshot"` returns nothing.

---

## A. Bugs

### A1. Child size chart: the fifth column is off-screen and its header wraps

Screens: [EN 07/07](reports/knittools-all-screens-en-2026-11-19-after/en/07-library/07-size-chart-child.png), [FI 07/07](reports/knittools-all-screens-en-2026-11-19-after/fi/07-library/07-size-chart-child.png). The header "Arm unde…" / "Ra kaina…" is cut at the right edge and wraps to two lines, which makes the header row taller. None of the column's values (21.5 to 42 cm) are visible. All other categories fit, including German ([DE 07/05](reports/knittools-all-screens-en-2026-11-19-after/de/07-library/05-size-charts.png)).

Cause: `SizeChartData.headers(CHILD)` has 5 columns (`size_col_arm` = "Arm length to underarm" / "Ranteesta kainaloon"). `SizeChartScreen` uses a fixed `ValueColumnMinWidth` per value column, so 5 columns exceed a 328 dp viewport.

Change:
1. Add a short header string, `size_col_sleeve` "Sleeve" (FI "Hiha"), and use it as the visible header for that column. Keep the long text "Arm length to underarm" as the header's `contentDescription` and as a one-line note under the table, `size_chart_sleeve_note` "Sleeve: wrist to underarm." (FI "Hiha: ranteesta kainaloon."). The note is shown only for the Child category.
2. Size the columns from content instead of a fixed minimum. Measure the widest header and the widest cell text per column with `rememberTextMeasurer()` at the current font scale, add the horizontal cell padding, and use those widths. If the total fits the viewport, distribute the extra space with weights. If it does not fit, keep the pinned Size column and horizontal scroll from round 1.
3. Header text: `maxLines = 1`. Header row height must be identical in every category.

Accept: at font scale 1.0 on a 360 dp wide phone, all 5 child columns and their values are visible in EN, FI and DE; header rows are one line.

### A2. At 200 % font the main counter number shrinks to label size

Screen: [EN 04/16-font-200](reports/knittools-all-screens-en-2026-11-19-after/en/04-counter/16-active-work-session-font-200.png). The main number "20" is about as tall as the "Rows" label above it. The session strip (two lines at this scale) takes the height that the number used to have.

Change (`CounterHeroLayout.kt` / `rememberCounterHeroLayout`, `CounterWorkspaceSections.kt` `WorkSessionStrip`):
1. Define a floor for the main number: `CounterDimens.MainNumberMinHeight` (it must be at least about 2× the `Rows` label height at the same font scale). The fitted font size may not go below it.
2. When the stacked layout cannot keep that floor with the strip present, apply these fallbacks in order:
   - (a) render the strip on **one line** (label, duration and Stop in a single `Row`, label with ellipsis)
   - (b) reduce the hero's top weight spacer to 0
   - (c) switch to the side-by-side layout if the width allows (the existing 552 dp rule)
   - (d) only then let the content scroll, with Undo and Stop still on the first screen.
3. Add an `AdaptiveCounterTest` case: font scale 2.0, a 360 × 640 dp phone and an active session. Assert that the main number's rendered height is at least `MainNumberMinHeight` and that Stop and Undo are displayed.

### A3. The Rename counter dialog is almost full screen width

Screens: [EN 04/08](reports/knittools-all-screens-en-2026-11-19-after/en/04-counter/08-rename-extra-counter.png), [FI 04/08](reports/knittools-all-screens-en-2026-11-19-after/fi/04-counter/08-rename-extra-counter.png). Other dialogs use about 83 % of the width ([EN 04/13](reports/knittools-all-screens-en-2026-11-19-after/en/04-counter/13-stitches-per-row.png) uses the same `ScrollableFormDialog`).

Cause to verify: `RenameCounterDialog` (`MultiCounterComponents.kt:331`) is composed from inside the Counters `ModalBottomSheet` content (`MultiCounterComponents.kt:194`). The dialog window then inherits the sheet window's width handling.

Change: hoist the rename dialog state out of the sheet content, so the dialog is composed at the same level as the other counter dialogs (in `CounterScreen`), or make `ScrollableFormDialog` set an explicit width policy that does not depend on the host window. Check every other dialog composed inside a sheet, for example the Reset and Delete counter confirmations, and the bookmark and document dialogs.

Accept: every dialog in the app has the same width on the same device. Add a Compose test that asserts the rename dialog width equals the stitches-per-row dialog width.

### A4. "Imported PDF" is stored in the database and stays in English

Screens: [FI 07/17](reports/knittools-all-screens-en-2026-11-19-after/fi/07-library/17-add-web-pattern.png) (Finnish UI shows "Imported PDF"), [EN 07/23](reports/knittools-all-screens-en-2026-11-19-after/en/07-library/23-pdf-pattern-detail.png).

Cause: `repository/SavedPatternRepository.kt:203`, `designerName = context.getString(R.string.imported_pattern_designer)`. A localized UI label is persisted as user data.

Change:
1. Stop writing the label: save `designerName = null` for imported PDFs. The saved pattern's source (PDF) is already a separate field.
2. Display: wherever a saved pattern shows its designer line (Saved Patterns list card, pattern detail, Add Pattern list, Save pattern sheet), show nothing when `designerName` is null. The PDF `BadgePill` already says the type, so "Imported PDF" plus a "PDF" pill was redundant.
3. Migration: add a Room migration (next schema version) that sets `designerName = NULL` where it equals any locale's value of `imported_pattern_designer` ("Imported PDF", "Tuotu PDF" and the other 10 translations; list them literally in the migration with a Finnish comment explaining why). Update `KnitToolsDatabaseMigrationSourceTest` and the schema JSON. `CLAUDE.md` says "Room v29": update the number.
4. Delete `imported_pattern_designer` from all locales if it becomes unused.

Accept: switching the app language never leaves an English "Imported PDF" in the UI, and existing rows are cleaned.

### A5. "Remind at [88] rows" describes a position as a count

Screens: [EN 02/10](reports/knittools-all-screens-en-2026-11-19-after/en/02-project-overview/10-add-reminder.png), [EN 02/11](reports/knittools-all-screens-en-2026-11-19-after/en/02-project-overview/11-repeating-reminder.png), [FI 02/10](reports/knittools-all-screens-en-2026-11-19-after/fi/02-project-overview/10-add-reminder.png) ("Muistuta kohdassa … kerrosta" is ungrammatical). The round-1 plan specified this suffix; that was a mistake.

Change (`ReminderComponents.kt`, `ReminderDialogFields`):
1. The first field is a position. Label it with the live counter label and give it **no suffix**:
   - Rows: new `reminder_at_row` "Remind at row" (FI "Muistuta kerroksella")
   - Rounds: `reminder_at_round` "Remind at round" (FI "Muistuta kierroksella")
   - Repeats: `reminder_at_repeat` "Remind at repeat" (FI "Muistuta toistolla")
   - Custom: `reminder_at_custom` "Remind at %1$s" (FI "Muistuta kohdassa %1$s", where %1$s is the custom label).
2. "Repeat every" keeps the plural unit suffix ("8 rows" / "8 kerrosta"); this one is correct as it is.
3. Delete `reminder_at_label` if unused.

### A6. Save pattern sheet still uses the old in-field label

Screens: [EN 03/37](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/37-save-shared-pdf-options.png), [FI 03/37](reports/knittools-all-screens-en-2026-11-19-after/fi/03-pattern-and-documents/37-save-shared-pdf-options.png).

Change: in `ui/screens/library/IncomingPdfSheet.kt`, replace the pattern-name `TextField` with `LabeledTextField` (label above), like every other form. Then grep `ui/screens` for remaining `TextField(` with a `label = {` lambda and convert any other leftovers. Extend the `UiConsistencySourceTest` rule from round 1 so it also covers this case.

---

## B. Copy and logic

### B1. Dialog bodies repeat the title's question

Screens:
- [EN 02/05](reports/knittools-all-screens-en-2026-11-19-after/en/02-project-overview/05-complete-project.png): "Complete project?" / "Complete Forest Cardigan? The project will be…"
- [EN 02/06](reports/knittools-all-screens-en-2026-11-19-after/en/02-project-overview/06-delete-project-confirmation.png)
- [EN 01/07](reports/knittools-all-screens-en-2026-11-19-after/en/01-projects/07-delete-folder-confirmation.png)
- [EN 08/08](reports/knittools-all-screens-en-2026-11-19-after/en/08-yarn-and-photos/08-delete-yarn-confirmation.png)
- [EN 08/17](reports/knittools-all-screens-en-2026-11-19-after/en/08-yarn-and-photos/17-delete-progress-photo.png)
- [FI 03/25](reports/knittools-all-screens-en-2026-11-19-after/fi/03-pattern-and-documents/25-clear-annotations.png)
- and the same pattern in Finnish.

Rule: the title asks; the body names the object (when the title does not) and states the consequence. The body never asks again. Exact English copy (translate faithfully into all locales):

- `complete_project_message`: "%s moves to your completed projects."
- `delete_project_message`: "%s and its counters, notes and photos will be deleted. This cannot be undone." Verify the list against what `CounterRepository` actually deletes and adjust the list if it differs.
- `delete_projects_message` (the plural at `strings.xml:128`): one "%d project will be deleted. This cannot be undone.", other "%d projects will be deleted. This cannot be undone."
- `folder_delete_message`: "%1$s will be deleted." The existing second line ("This folder is empty." / "N projects will be kept and become Unfiled.") stays.
- `delete_yarn_card_message`: "This cannot be undone. Projects that use this yarn keep their own yarn notes." Verify the second sentence against the repository behavior; drop it if it is not true.
- `delete_photo_confirm`: "This cannot be undone."
- `pattern_annotation_clear_page_body`: "All your marks on this page will be removed. You can undo this."
- `project_documents_remove_last_message` and `project_documents_remove_primary_message`: rewrite in the same style, without a question.
- Unlink pattern: already in this style; keep it.

Grep all `*_message` and `*_body` strings used by `ConfirmationDialog` for any other leading question.

### B2. The "Rows already done" field shows "e.g. 0"

Screens: [EN 01/17](reports/knittools-all-screens-en-2026-11-19-after/en/01-projects/17-new-project.png), [EN 01/19](reports/knittools-all-screens-en-2026-11-19-after/en/01-projects/19-new-project-more-options.png), [FI 01/19](reports/knittools-all-screens-en-2026-11-19-after/fi/01-projects/19-new-project-more-options.png).

Cause: `ProjectDetailsSheet.kt:269` sets `NumberInputOptions(placeholder = "0")`, and the round-1 change prefixes every numeric placeholder with `field_example_format`.

Change: add `NumberInputOptions.placeholderIsExample: Boolean = true`. The "e.g." prefix is applied only when it is true. `ProjectDetailsSheet` passes `false` for the start count (it shows a plain "0", meaning "the default is 0"). Calculators keep the prefix.

### B3. Stitches per row: title and field label are identical

Screens: [EN 04/13](reports/knittools-all-screens-en-2026-11-19-after/en/04-counter/13-stitches-per-row.png), [FI 04/13](reports/knittools-all-screens-en-2026-11-19-after/fi/04-counter/13-stitches-per-row.png).

Change (`CounterScreen.kt:1870`): field label `stitches_per_row_label` becomes "Stitches in one row" (FI "Silmukoita yhdessä kerroksessa"). For Rounds projects, use "Stitches in one round" (FI "Silmukoita yhdessä kierroksessa"); add both keys. The title stays "Stitches per row".

### B4. Only one orange action per dialog or panel

Screens: [EN 04/03](reports/knittools-all-screens-en-2026-11-19-after/en/04-counter/03-target-rows.png) ("Remove target" and "Save" are both orange), [EN 03/26](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/26-calibrate-counter-position.png) and [EN 03/27](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/27-calibrate-second-position.png) (calibration "Cancel" is orange).

Change:
1. `TargetRowsDialog.kt:65`: "Remove target" uses `colorScheme.error`, the same as "Remove" in the stitches-per-row dialog, and sits on the left side next to Cancel (same layout as `CounterScreen.kt:1885`).
2. Calibration panel (`PatternViewerScreen.kt` around line 1320): Cancel becomes the shared `CancelButton`, and Save stays the only orange action. Put the two buttons at the end of the row (Cancel, then Save), like dialogs, instead of splitting the row in two halves.
3. Calibration field labels: `pattern_calibration_first_row` "First count" becomes "Row number now" and `pattern_calibration_last_row` "Last count" becomes "Row number here". Use the live counter label via the same mechanism as A5 (rows / rounds / repeats / custom).

### B5. Destructive items in overflow menus are not consistently red

Screens: red in [EN 02/03](reports/knittools-all-screens-en-2026-11-19-after/en/02-project-overview/03-project-actions.png) and [EN 04/07](reports/knittools-all-screens-en-2026-11-19-after/en/04-counter/07-extra-counter-actions.png); black in [EN 02/12](reports/knittools-all-screens-en-2026-11-19-after/en/02-project-overview/12-reminder-actions.png), [EN 03/20](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/20-bookmark-actions.png), [EN 07/20](reports/knittools-all-screens-en-2026-11-19-after/en/07-library/20-web-pattern-actions.png), [EN 08/07](reports/knittools-all-screens-en-2026-11-19-after/en/08-yarn-and-photos/07-yarn-actions.png), [EN 08/25](reports/knittools-all-screens-en-2026-11-19-after/en/08-yarn-and-photos/25-project-yarn-note-options.png), [EN 03/06](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/06-document-actions.png).

Change:
1. Add `isDestructive: Boolean = false` to `RowMenuAction` (`ui/components/RowOverflowMenu.kt:18`). Destructive items render with `colorScheme.error`.
2. Do the same for every other overflow menu that has Delete or Remove: bookmark row, document row, web-pattern detail, yarn-card detail, folder row, session row.
3. Add a `UiConsistencySourceTest` rule: a `DropdownMenuItem` or `RowMenuAction` whose label resource is `delete*`, `*_delete`, `*_remove` or `remove*` must be marked destructive.

### B6. English duration format is inconsistent

Screens: [EN 04/16](reports/knittools-all-screens-en-2026-11-19-after/en/04-counter/16-active-work-session.png) "Under 1 min" next to [EN 04/16-font-200](reports/knittools-all-screens-en-2026-11-19-after/en/04-counter/16-active-work-session-font-200.png) "4min" and Insights "3h 10min".

The established English format is `%dmin` / `%1$dh %2$dmin` (`insights_duration_*`). Change `work_session_under_minute` to "Under 1min" in English. Each other locale keeps its own minute spacing, consistent with its `insights_duration_minutes` (Finnish "Alle 1 min" is already correct).

### B7. Yarn Estimator picker has no way out when there are no yarn cards

Screen: [EN 06/13](reports/knittools-all-screens-en-2026-11-19-after/en/06-tools/13-estimator-yarn-picker.png).

Change: when `YarnCardPickerSheet` has no cards, show the text plus a text action "Add yarn in My Yarn" (new `yarn_picker_empty_action`, FI "Lisää lanka Omiin lankoihin"). The action navigates to My Yarn with the add form open. That navigation must keep the Tools tab selected: register the My Yarn route in the Tools graph, the same way `session_history` is registered in Insights. If the add form cannot be opened directly, open My Yarn and say so in the final report.

---

## C. Finnish terminology

### C1. "Asiakirja" and "dokumentti" are mixed for the same thing

Screens: [FI 03/05](reports/knittools-all-screens-en-2026-11-19-after/fi/03-pattern-and-documents/05-project-documents.png) "Asiakirjat", [FI 03/07](reports/knittools-all-screens-en-2026-11-19-after/fi/03-pattern-and-documents/07-rename-document.png) "Nimeä asiakirja uudelleen", [FI 03/08](reports/knittools-all-screens-en-2026-11-19-after/fi/03-pattern-and-documents/08-remove-document-confirmation.png) "Poistetaanko asiakirja?", against [FI 03/04](reports/knittools-all-screens-en-2026-11-19-after/fi/03-pattern-and-documents/04-overview-with-pdf.png) "Ohje ja dokumentit", "Hallitse dokumentteja", [FI 03/30](reports/knittools-all-screens-en-2026-11-19-after/fi/03-pattern-and-documents/30-add-another-pattern.png) "Lisää dokumentti", "PDF-dokumentit".

Change: use **"dokumentti"** everywhere in Finnish. It is already in the section title "Ohje ja dokumentit" and is the more common word for a PDF file. Update in `values-fi`:
- `project_documents_title` "Asiakirjat" becomes "Dokumentit"
- `project_documents_rename_title` becomes "Nimeä dokumentti uudelleen"
- `project_documents_remove_title` becomes "Poistetaanko dokumentti?"
- `project_documents_open_action` becomes "Avaa dokumentti"
- `project_documents_empty` becomes "Ei dokumentteja"
- `project_documents_remove_primary_message` (rewrite per B1)
- any document-name field label.

Then `git grep -n -i "asiakirj" app/src/main/res/values-fi` must return nothing. Check the other Nordic locales (sv, nb, da) for the same split and pick one word per locale.

### C2. "Verkko-ohjeet" in Settings collides with the web-pattern term

Screen: [FI 09/04](reports/knittools-all-screens-en-2026-11-19-after/fi/09-settings-and-pro/04-settings-scrolled.png). In this app "verkko-ohje" means a pattern saved as a web link (`CLAUDE.md`). The Settings link to online help must not reuse it.

Change: FI `help_and_guide` "Verkko-ohjeet" becomes "Tuki verkossa". Check `de`, `sv`, `nb`, `da`, `nl` for the same collision with their web-pattern term, and rename where needed.

### C3. Smaller Finnish copy fixes

- **Undo wording.** "Tätä ei voi perua" ([FI 08/17](reports/knittools-all-screens-en-2026-11-19-after/fi/08-yarn-and-photos/17-delete-progress-photo.png)) and "Tätä ei voi kumota" ([FI 02/06](reports/knittools-all-screens-en-2026-11-19-after/fi/02-project-overview/06-delete-project-confirmation.png)) are mixed. Use "Tätä ei voi kumota." everywhere; the app's Undo button is "Kumoa".
- **Units in labels.** Yarn usage labels "(Metrit)" ([FI 08/21](reports/knittools-all-screens-en-2026-11-19-after/fi/08-yarn-and-photos/21-track-yarn-usage.png)) are unnatural. Show the unit as a field suffix ("m", "yd", "g", "vyyhtiä") instead of in the label, in every locale and in English too ("Planned (Meters)"). This is the same suffix pattern Cast On and Yarn Estimator use. Labels become "Suunniteltu", "Varattu tähän projektiin", "Käytetty", "Vyyhdin pituus", "Vyyhdin paino". Apply this in `ProjectYarnUsageSheet` for all fields.
- **Yarn usage scope text.** "Omat langat -määrät eivät muutu." is awkward. Rewrite as "Seuranta koskee vain tätä projektia. Omien lankojen määrät eivät muutu."
- **Page counter format.** "Sivu 1/1" ([FI 03/32](reports/knittools-all-screens-en-2026-11-19-after/fi/03-pattern-and-documents/32-photo-to-pdf.png)) and "Sivu 1 / 1" (reader, [FI 03/10](reports/knittools-all-screens-en-2026-11-19-after/fi/03-pattern-and-documents/10-project-pdf-reader.png)) are mixed. Use one shared page-indicator string, "Sivu %1$d / %2$d", for both. In English the reader says "Page 1 of 1"; make the review screen use the same key.

---

## D. Typography, spacing and color

### D1. Helper text alignment under fields

Screens: [EN 01/03](reports/knittools-all-screens-en-2026-11-19-after/en/01-projects/03-create-folder.png) and [EN 03/13](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/13-page-jump.png): the helper is indented 16 dp from the field edge. In [EN 01/19](reports/knittools-all-screens-en-2026-11-19-after/en/01-projects/19-new-project-more-options.png) the helper is flush with the field edge. The label above is flush in both.

Change: `LabeledTextField` currently passes the helper or error through Material's `supportingText` slot, which adds 16 dp start padding. Render it instead as a separate `Text` under the field, with the same style and top spacing as the `NumberInputField` helper (flush, `bodySmall`, `onSurfaceVariant`; `colorScheme.error` for errors). Keep the `semantics { error(...) }`. Result: label, field edge and helper share one left edge everywhere.

### D2. Segmented toggle inside dialogs looks like a text field

Screens: [EN 02/10](reports/knittools-all-screens-en-2026-11-19-after/en/02-project-overview/10-add-reminder.png), [EN 02/11](reports/knittools-all-screens-en-2026-11-19-after/en/02-project-overview/11-repeating-reminder.png), [EN 02/13](reports/knittools-all-screens-en-2026-11-19-after/en/02-project-overview/13-edit-reminder.png). The track is the cream input-field color with a dark 1 dp outline, heavier than the fields' faint border. Sheets ([EN 04/09](reports/knittools-all-screens-en-2026-11-19-after/en/04-counter/09-add-counter.png)) use a taupe track with no outline.

Change (`SegmentedToggle.kt:66`): for `SegmentedToggleSurface.Dialog`, use a **filled track without an outline**, with a new token `knitToolsColors.segmentedTrackOnDialog`. In the light theme it should be a taupe one step darker than the dialog surface (similar to the sheet track against the sheet). Check both themes: the track edge must keep at least 3:1 contrast against the dialog surface, and unselected labels at least 4.5:1 against the track. Remove the `.border(...)`.

### D3. Documents sheet: pattern information block

Screens: [EN 03/33](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/33-multiple-documents.png), [FI 03/33](reports/knittools-all-screens-en-2026-11-19-after/fi/03-pattern-and-documents/33-multiple-documents.png).

Problems:
- The gap from the name to the designer is about twice the other line gaps.
- The pattern name is regular weight, while the PDF names below are bold.
- A bold "Web pattern" line sits between two muted lines.

Change (Documents sheet in `PatternViewerScreen.kt`):
- Pattern name: `titleSmall`, same weight as the PDF row headlines; ⋮ vertically centered on the name line.
- Designer: `bodySmall`, `onSurfaceVariant`.
- Type: replace the bold "Web pattern" text with the shared `BadgePill` ("Web pattern"), as the Saved Patterns list does.
- Site: `bodySmall`, `onSurfaceVariant`.
- Use one vertical spacing token between all lines of the block (`OverviewDimens` or a new `DocumentsSheetDimens.InfoLineSpacing`, 2 dp), and a larger one (12 dp) before "Open website".

### D4. Bookmark rows are inset relative to the sheet title

Screens: [EN 03/19](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/19-bookmarks.png), [EN 03/20](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/20-bookmark-actions.png). The bookmark name starts about 8 dp to the right of "Bookmarks", because of the TextButton's inner padding.

Change: replace the `TextButton` in `PatternBookmarkRow` with a clickable `Row` (`Modifier.clickable` with the same ripple and 48 dp min height) without extra start padding. Text then aligns with the sheet title and with other sheet rows.

### D5. Calibration panel spacing

Screens: [EN 03/26](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/26-calibrate-counter-position.png), [FI 03/26](reports/knittools-all-screens-en-2026-11-19-after/fi/03-pattern-and-documents/26-calibrate-counter-position.png). The title, instruction and field label are stacked with no spacing; the field label touches the instruction.

Change:
- Title: `titleSmall`.
- 4 dp, then the instruction in `bodyMedium`, `onSurfaceVariant`.
- 12 dp, then the `NumberInputField` with its own label.
- 8 dp, then the button row from B4.

Use tokens, not literals.

### D6. Overview hero has four stacked text levels during a session

Screens: [EN 08/29](reports/knittools-all-screens-en-2026-11-19-after/en/08-yarn-and-photos/29-overview-yarn-amounts.png) (kicker, "Work session active", "Right sleeve", then the row count), compared with the project-list hero [EN 01/03](reports/knittools-all-screens-en-2026-11-19-after/en/01-projects/03-create-folder.png), where the session line takes the section's place.

Change (`ContinueProjectCard`): when a session is active, show the session state as the **trailing part of the kicker line**: "CONTINUE KNITTING" on the left, and "Session 12min" right-aligned on the same line in `labelSmall` and `onSurfaceVariant`. Do not add a separator character; position separates them. Remove the separate "Work session active" line. The section line ("Right sleeve") stays where it is. Use the same rule on the project list hero and the overview hero, so both look identical. Keep the "compact text, no background, no border" rule.

### D7. Markup mode top bar leaves the title about 9 characters

Screens: [EN 03/22](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/22-markup-toolbar.png), [EN 07/27](reports/knittools-all-screens-en-2026-11-19-after/en/07-library/27-library-markup.png), [EN 03/22-font-130](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/22-markup-toolbar-font-130.png) ("Knittools-…").

Change: in annotation mode, replace the document title with the mode name `pattern_mark_up` ("Mark up" / FI "Merkitse") in the same title style. The user knows which document they are in, and the mode name explains why the bar changed. Restore the document title when leaving markup.

### D8. PDF reader bottom bar layout jumps between one and two rows

Screens: [FI 03/22](reports/knittools-all-screens-en-2026-11-19-after/fi/03-pattern-and-documents/22-markup-toolbar.png) (one row) and [FI 03/23](reports/knittools-all-screens-en-2026-11-19-after/fi/03-pattern-and-documents/23-markup-extra-tools.png) (two rows) at the same font scale; [EN 03/22-font-130](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/22-markup-toolbar-font-130.png) puts the bookmark icon alone at the far right of row one.

Change: decide one row versus two from the font scale and window width only (`FontScaleThresholds.COMPACT` and available width), never from the current text. In the two-row layout:
- row one: counter label, −, +
- row two: ←, page indicator, →, bookmark, with the bookmark next to the page controls.

Both rows share the same start edge.

### D9. Smaller visual fixes

- **Total weight pill is invisible.** Yarn Estimator "Weight of whole skeins: 600 g" ([EN 06/14](reports/knittools-all-screens-en-2026-11-19-after/en/06-tools/14-yarn-estimator-result.png)) is a `BadgePill` with the same color as the result card, so only orange letter-spaced text shows. Render it as a plain `bodyMedium` line in `onSurfaceVariant`, like Cast On's "Actual width" line (`YarnEstimatorScreen.kt:237`).
- **Slider track color.** The markup width slider's inactive track is pale green, outside the warm palette ([EN 03/22](reports/knittools-all-screens-en-2026-11-19-after/en/03-pattern-and-documents/22-markup-toolbar.png)). In `PatternAnnotationToolbar.kt:590`, set `SliderDefaults.colors(...)` from tokens: active track `colorScheme.primary`, inactive track a new token `knitToolsColors.sliderInactiveTrack` (warm taupe, at least 3:1 against the palette surface), thumb `colorScheme.primary`. Check the dark theme as well.
- **Button casing.** `saved_pattern_detail_open_pattern` "Open Pattern" becomes "Open pattern", matching "Open website".
- **Cast On placeholder wrap.** The Cast On result placeholder wraps with a lone "on" on line two ([EN 06/08](reports/knittools-all-screens-en-2026-11-19-after/en/06-tools/08-cast-on.png)). Shorten it to "Fill in width and gauge to see your cast-on" and check FI and DE for the same wrap.

---

## E. Not verifiable from the screenshots: verify and capture

For each item, check the behavior on a device, fix it if it is wrong, and add the listed screenshot to the next set.

- **E1. Project completions chart (round 1, D7).** Capture Insights "This Month" scrolled to Project completions, with one completion and with three on different days. Expect no "0 completions" labels and no timeline under `MINIMUM_CHART_BARS`.
- **E2. Yarn Estimator selection (round 1, A10).** With at least two yarn cards, capture the picker and the prefilled fields after selection, in metric and imperial.
- **E3. "Saved in" line (round 1, B5).** Capture the New Project sheet opened from a folder view.
- **E4. Pending unit highlight (round 1, C5).** Capture Yarn usage right after tapping Grams without a conversion, with the toggle visible and the conversion fields focused.

---

## Final checklist

- [ ] F, A to D done; E verified and captured.
- [ ] No `strings_screenshot_*.xml` files remain; all keys exist in every locale; unused keys deleted.
- [ ] Room migration for A4 added and tested; `CLAUDE.md` schema version updated.
- [ ] No `·`, `•` or ` | ` separators added.
- [ ] `UiConsistencySourceTest` exception lists did not grow; new rules from A6 and B5 added.
- [ ] Unit tests, lint and the named instrumented tests green.
- [ ] Screenshots re-taken in EN and FI (DE for A1, B1, C3, D8), with finished animations and the app theme, into `reports/knittools-all-screens-en-<date>-round2/`.
- [ ] `CLAUDE.md` / `ui.md` / `AGENTS.md` / `CODEX.md` updated where a rule changed (A4 schema, B5 destructive menu rule, D1 helper alignment, D6 hero session line, D7 markup title).
- [ ] No commit, push or PR unless Emma asked.
