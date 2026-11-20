# KnitTools Screenshot Review 2026-11-19: Round 3 Fix Plan

Follow-up to `KnitTools Screenshot Review 2026-11-19 - Round 2 Fix Plan.md`. Round 2 is implemented and mostly correct. This round fixes:
- one layout regression (R1)
- five bugs (R2 to R6)
- three visual overshoots (V1 to V3)
- four copy and terminology items (T1 to T4).

Nothing else should change. Do not touch screens or rules that are not listed here.

Source screenshots: `reports/knittools-all-screens-en-2026-11-19-round2/` (`en/`, `fi/`, `de/`, flat file names).
Each reference is a link to the image.

Implementer: Codex. Owner and reviewer: Emma.

---

## 0. Ground rules

Same as rounds 1 and 2:

- `CLAUDE.md`, `AGENTS.md`, `CODEX.md` and `.claude/rules/ui.md` are binding.
- Shared components only; theme tokens only; Type.kt roles only.
- No `·`, `•` or ` | ` separators; no colored left-edge bars.
- Every changed string goes into all 12 value folders; delete unused keys in every locale. Finnish uses "ohje", never "kaava".
- Comments and commit messages in Finnish; delete dead code in the same change.
- `UiConsistencySourceTest` exception lists must not grow.
- No commit, push or PR unless Emma asks.

Verification after the whole round:

- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:lintDebug`
- `AdaptiveCounterTest` and the other instrumented tests named below
- re-take every screenshot listed in this plan in EN and FI (DE where noted), after animations finish, into `reports/knittools-all-screens-en-2026-11-19-round3/`.

---

## R. Regression and bugs

### R1. Counter layout changes when a work session starts

Screens: [EN 04-active-work-session](reports/knittools-all-screens-en-2026-11-19-round2/en/04-active-work-session.png), [FI 04-active-work-session](reports/knittools-all-screens-en-2026-11-19-round2/fi/04-active-work-session.png). Compare with the same project without a session: [EN 04-extra-counter-actions](reports/knittools-all-screens-en-2026-11-19-round2/en/04-extra-counter-actions.png).

At the default font scale, with a session active:
- The REPEAT row jumps from above "Rows" to below Undo.
- The target line ("of 96" / "54 rows left") moves below the hero. Only half of its progress bar shows above the bottom navigation.

The knitter's controls move just because a timer started, and the target is hidden. That breaks muscle memory and the `ui.md` rule that the target is shown on the "of 72 / 38 rows left" line.

Cause:
- `CounterHeroLayout.kt` (`rememberCounterHeroLayout`, the branch commented "Move supporting controls below the main number, Stop and Undo") returns `numberMaximumHeight = primaryNumberSpace`.
- `CounterWorkspaceSections.kt:512` then derives `showSupportingControls = !numberMaximumHeight.isSpecified`.
- So capping the number also moves REPEAT, the target line and the stitch tracker into the separate `CounterSupportingControls` list item (`CounterWorkspaceSections.kt:227`).
- This branch is reached on a normal phone at font scale 1.0 as soon as the session strip is added.

Change:
1. Decouple the two decisions. Add an explicit `supportingControlsPlacement` enum (values `InHero` and `BelowHero`) to `CounterHeroLayout`, and stop inferring it from `numberMaximumHeight` in `CounterHero`.
2. Insert a new candidate **before** the "move below" branch: `StackedCompact` with the session strip inline, supporting controls kept **in the hero** in their normal positions, and the main number capped to the remaining height. The cap must not go below `counterMainNumberMinHeight()`.
   - Compute the height of this candidate the same way as `stackedCounterHeroHeight(compactSpacing = true)`, but with the number's natural height replaced by the minimum.
   - If it fits, use it.
3. Keep the "below hero" placement only as the last resort before side-by-side, for example at font scale 2.0 on a small phone. When it is used, the order below the hero must stay REPEAT, then the target line, then stitches, the same order as in the hero.
4. The position of REPEAT and of the target line must be identical with and without an active session whenever both layouts are `Stacked` or `StackedCompact` with controls in the hero.

Tests (`AdaptiveCounterTest`):
- At 411 × 780 dp, font scale 1.0, a project with REPEAT, a target and stitch tracking: with and without a session, assert that REPEAT is above the row label and that "of N" and "rows left" are displayed without scrolling. Also assert that REPEAT's top coordinate does not change when a session is added.
- Keep the round-2 font-200 test green.

### R2. Highlighter direction picker is clipped under the PDF

Screens: [EN 03-23-markup-extra-tools](reports/knittools-all-screens-en-2026-11-19-round2/en/03-23-markup-extra-tools.png), [EN 03-23-markup-extra-tools-dark](reports/knittools-all-screens-en-2026-11-19-round2/en/03-23-markup-extra-tools-dark.png), [FI 03-23-markup-extra-tools](reports/knittools-all-screens-en-2026-11-19-round2/fi/03-23-markup-extra-tools.png), [DE 03-23-markup-extra-tools](reports/knittools-all-screens-en-2026-11-19-round2/de/03-23-markup-extra-tools.png).

`PatternHighlighterAxisLock` (`ui/screens/pattern/PatternAnnotationInteraction.kt:21`) has four values: FREE, HORIZONTAL, VERTICAL and DOMINANT_AXIS. `SegmentedToggle` therefore renders a 2 × 2 grid. The palette's `heightIn(max = PatternAnnotationTokens.TOOLBAR_MAX_HEIGHT)` cuts off the second row (Vertical, Dominant axis), and its bottom corners. Two options are unreachable unless the user discovers that the palette scrolls.

Change:
1. The palette must never clip a control. Measure the palette content. If it is taller than `TOOLBAR_MAX_HEIGHT`, either raise the cap for that tool (at most 40 % of the window height) or move the least-used settings into a compact row. Do not rely on hidden inner scrolling.
2. For the highlighter, show the direction as a single-row `SegmentedToggle` with short labels when they fit ("Free", "Horizontal", "Vertical", "Auto"). Rename `DOMINANT_AXIS`'s label to "Auto" (FI "Automaattinen", DE "Automatisch") in all locales. Check that four options fit at 360 dp width in EN, FI and DE. If they do not, use the 2 × 2 grid and raise the palette height so the whole grid is visible.
3. Add a Compose test: with the highlighter tool selected, all four direction options are displayed and fully inside the palette bounds, at font scale 1.0 and 1.3.

### R3. Imperial prefill shows an unrounded length

Screens: [EN E2-estimator-prefilled-imperial](reports/knittools-all-screens-en-2026-11-19-round2/en/E2-estimator-prefilled-imperial.png) ("218.72265966754156 yards"), [FI E2-estimator-prefilled-imperial](reports/knittools-all-screens-en-2026-11-19-round2/fi/E2-estimator-prefilled-imperial.png).

Cause: `ui/screens/yarn/YarnEstimatorScreen.kt:288-296`, `savedYarnSkeinFields` divides meters by `METERS_PER_YARD` and prints the full `BigDecimal`.

Change:
- Round converted values to whole units (yards and meters), half-up. Grams stay as entered.
- Format with the user's locale through the same number formatter the estimator uses for display. No hardcoded `.` or `,`.
- Add a unit test: 200 m → "219" yd, 220 m → "241" yd, 100 g → "100".

### R4. Insights says "Nothing yet this month." under visible completions

Screens: [EN E1-project-completions-one](reports/knittools-all-screens-en-2026-11-19-round2/en/E1-project-completions-one.png), [EN E1-project-completions-three](reports/knittools-all-screens-en-2026-11-19-round2/en/E1-project-completions-three.png), [FI E1-project-completions-one](reports/knittools-all-screens-en-2026-11-19-round2/fi/E1-project-completions-one.png).

Cause: `InsightsScreen.kt:305` adds `completionSection` and then `:337` adds `InsightsRangeEmptyNote` whenever the range has no session minutes, even though the range has completions.

Change: when the range has completions but no session minutes, the note talks only about time:
- Title `insights_no_sessions_week` / `_month` / `_all_time`: "No work sessions this week." / "No work sessions this month." / "No work sessions yet." (FI "Ei työskentelyistuntoja tällä viikolla." / "…tässä kuussa." / "Ei vielä työskentelyistuntoja.").
- Keep the existing body text.
- Place the note **above** the completion section, so the order reads: time first (empty), then completions.
- When there are neither minutes nor completions, keep the current "Nothing yet…" note unchanged.

### R5. Completion chart labels are clipped at the right edge in Finnish

Screen: [FI E1-project-completions-three](reports/knittools-all-screens-en-2026-11-19-round2/fi/E1-project-completions-three.png) ("1 valmistumine", "3. marrask. 20" cut off).

Cause: `InsightsCompletionSection.kt` `CompletionTimeline` uses a fixed `InsightsDimens.CompletionBucketWidth` (112 dp) inside a `LazyRow` with no end padding, and the labels are single line with the full date including the year.

Change:
1. When all buckets fit in the available width, lay them out in a `Row` with equal weights instead of the `LazyRow`. Use the `LazyRow` only when they do not fit, and then add `contentPadding(end = 16.dp)` via an `InsightsDimens` token.
2. Bucket labels:
   - Count: show the number only (`labelMedium`), because the section title already says "N completions".
   - Date: drop the year (localized short month-day format through the existing locale date helpers).
   - Both lines `maxLines = 1` with ellipsis as a last resort.
   - The year already appears in the list below.

Accept: in FI and DE, three buckets on a 360 dp phone show every label in full.

### R6. Set target buttons wrap in Finnish

Screen: [FI 04-target-rows](reports/knittools-all-screens-en-2026-11-19-round2/fi/04-target-rows.png). "Poista tavoite" and "Peruuta" sit on the first line and "Tallenna" alone on the second.

Change: move the destructive secondary action out of the button row, in both dialogs that have one:
- Set target: "Remove target"
- Stitches per row: "Remove" ([EN 04-stitches-per-row](reports/knittools-all-screens-en-2026-11-19-round2/en/04-stitches-per-row.png))

Render it as a `TextButton` in `colorScheme.error`, start-aligned, directly under the field inside the dialog content, and only when a value exists. The button row then contains only Cancel and Save, as in every other dialog. Apply the same rule anywhere else a dialog has three actions.

---

## V. Visual overshoots from round 2

### V1. Control track color is far too dark (light theme) and inverted (dark theme)

Screens: [EN 02-add-reminder](reports/knittools-all-screens-en-2026-11-19-round2/en/02-add-reminder.png), [EN 02-repeating-reminder](reports/knittools-all-screens-en-2026-11-19-round2/en/02-repeating-reminder.png), [EN 02-add-reminder-dark](reports/knittools-all-screens-en-2026-11-19-round2/en/02-add-reminder-dark.png), [EN 03-22-markup-toolbar](reports/knittools-all-screens-en-2026-11-19-round2/en/03-22-markup-toolbar.png), [EN 03-22-markup-toolbar-dark](reports/knittools-all-screens-en-2026-11-19-round2/en/03-22-markup-toolbar-dark.png).

Cause: `segmentedTrackOnDialog` and `sliderInactiveTrack` both map to one color:
- light theme: `LightControlTrack = 0xFF695A4C` (`theme/Color.kt:87`), a chocolate brown block in a khaki dialog
- dark theme: `DarkControlTrack = 0xFFA69A85` (`:32`), a light block in a dark dialog.

On the slider, the inactive track looks heavier than the active orange part.

Change:
1. **Dialog segmented track:** a track one step away from the dialog surface, in the same direction as the sheet toggles.
   - Light: the dialog surface is `LightSurfaceHigh` (`0xFFBBB59A`), so use `LightSurfaceHighest` (`0xFFA49D80`).
   - Dark: one step lighter than the dark dialog surface (`SurfaceHighest` `0xFF454535`). Add `DarkSurfaceDialogTrack`, around `0xFF55553F`; tune it until the track edge is visible.
   - Unselected labels must keep at least 4.5:1 against the track in both themes. Write a unit test like `YarnPaletteContrastTest`.
2. **Slider inactive track:** a separate, quieter token.
   - Light: a warm taupe clearly lighter than the active primary and darker than the toolbar background (`0xFFE9E4D0`), for example `0xFFC9C2A8`.
   - Dark: around `0xFF4E4E3C` against the dark toolbar.
   - Requirement: at least 1.8:1 against the toolbar background, so the track is visible, and visually lighter than the active track.
3. Delete `LightControlTrack` / `DarkControlTrack` if they become unused.

### V2. PDF reader bottom bar is always two rows on phones

Screens: [EN 03-10-project-pdf-reader](reports/knittools-all-screens-en-2026-11-19-round2/en/03-10-project-pdf-reader.png), [EN 03-22-markup-toolbar](reports/knittools-all-screens-en-2026-11-19-round2/en/03-22-markup-toolbar.png).

Cause: `PatternViewerDimens.BottomBarSingleRowMinWidth = 416.dp` (`theme/PatternViewerDimens.kt:11`) is wider than a typical phone's bar (about 380 dp after padding). The bar therefore always uses two rows and takes reading space, even though the content fit on one row before.

Change: decide one or two rows from the **measured** width of the content: count label, −, +, ←, page indicator, →, bookmark, plus spacing. Use `SubcomposeLayout` or measure both groups' intrinsic widths. Keep the rule "never decide from the current text alone" by measuring with the widest expected strings: the counter label at 99 999 and the page indicator at "999 / 999" in the current locale.

Accept:
- one row on a 411 dp phone at font scale 1.0 in EN, FI and DE
- two rows at font scale 1.3 in DE ([DE 03-22-markup-toolbar-font-130](reports/knittools-all-screens-en-2026-11-19-round2/de/03-22-markup-toolbar-font-130.png))
- the bar never switches layout while paging.

### V3. Document row starts with a lowercase "page 1"

Screens: [EN 03-33-multiple-documents](reports/knittools-all-screens-en-2026-11-19-round2/en/03-33-multiple-documents.png) ("Appendix / page 1"), [FI 03-33-multiple-documents](reports/knittools-all-screens-en-2026-11-19-round2/fi/03-33-multiple-documents.png) ("sivu 1").

Cause: `project_documents_page` "page %1$d" (FI "sivu %1$d") is designed to follow "Main pattern, ", but `PatternViewerScreen.kt:1117-1122` also uses it as the first part.

Change: add `project_documents_page_standalone` "Page %1$d" (FI "Sivu %1$d"; all locales) and use it when there is no "Main pattern" part. The content description keeps using the lowercase form after the label.

---

## T. Copy and terminology

### T1. Finnish: "työrupeama" and "työskentelyistunto" are mixed

Screens: [FI 01-projects-active-session](reports/knittools-all-screens-en-2026-11-19-round2/fi/01-projects-active-session.png) and [FI 02-overview-active-session](reports/knittools-all-screens-en-2026-11-19-round2/fi/02-overview-active-session.png) ("Työrupeama alle 1 min"), against [FI 04-active-work-session](reports/knittools-all-screens-en-2026-11-19-round2/fi/04-active-work-session.png) ("Työskentelyistunto käynnissä").

`values-fi/strings.xml` uses "istunto" 26 times and "rupeama" 4 times. Use **"työskentelyistunto"** in full contexts and **"istunto"** where space is tight. Change these four keys:
- `work_session_hero_duration_format` "Työrupeama %1$s" becomes "Istunto %1$s"
- `work_session_hero_under_minute` "Työrupeama alle 1 min" becomes "Istunto alle 1 min"
- `delete_project_message`: "työrupeamien historia" becomes "työskentelyistuntojen historia"
- `delete_session_message`: "Tämä työrupeama poistetaan historiasta." becomes "Tämä työskentelyistunto poistetaan historiasta."

Then `git grep -n -i "rupeam" app/src/main/res/values-fi` must return nothing.

Note: `CLAUDE.md` currently says "työrupeaman kesto" and "työrupeaman rivin" in Finnish prose. Update those sentences to "työskentelyistunnon" so the docs match the UI term.

### T2. Finnish: "kerä" and "vyyhti" are mixed for skein

Screens: [FI 08-track-yarn-usage](reports/knittools-all-screens-en-2026-11-19-round2/fi/08-track-yarn-usage.png) and [FI E4-yarn-usage-pending-grams](reports/knittools-all-screens-en-2026-11-19-round2/fi/E4-yarn-usage-pending-grams.png) ("Vyyhdit", "Vyyhdin pituus", "vyyhtimuunnos"), against [FI 08-07-yarn-actions](reports/knittools-all-screens-en-2026-11-19-round2/fi/08-07-yarn-actions.png) ("8 kerää") and [FI 06-estimator-tools-tab](reports/knittools-all-screens-en-2026-11-19-round2/fi/06-estimator-tools-tab.png) ("Pituus / kerä").

Use **"kerä"** everywhere; it is the majority form and appears on yarn cards and in the estimator. Update every `yarn_usage_*` key and the skein plural in `values-fi`:
- "Vyyhdit" becomes "Kerät"
- "vyyhtiä" becomes "kerää"
- "Vyyhdin pituus" becomes "Kerän pituus"
- "Vyyhdin paino" becomes "Kerän paino"
- "Lisää vyyhtimuunnos" becomes "Lisää kerämuunnos"
- "Poista vyyhtimuunnos" becomes "Poista kerämuunnos"
- the conversion-required text: "Syötä kerän pituus ja paino positiivisina lukuina, jotta voit käyttää grammoja tai keriä."
- the plural "%1$s vyyhti" becomes "%1$s kerä" / "%1$s kerää".

Then `git grep -n -i "vyyh" app/src/main/res/values-fi` must return nothing.

### T3. English: "under a minute" is written two ways

Screens: [EN 01-projects-active-session](reports/knittools-all-screens-en-2026-11-19-round2/en/01-projects-active-session.png) ("Session <1min"), against [EN 04-active-work-session](reports/knittools-all-screens-en-2026-11-19-round2/en/04-active-work-session.png) ("Under 1min").

Use one form, "Under 1min": `work_session_hero_under_minute` becomes "Session under 1min". The strip keeps "Under 1min". Check the other 10 locales for the same mismatch between the two keys, and align them the same way.

### T4. Session strip label at large font

Screen: [EN 04-active-work-session-font-200](reports/knittools-all-screens-en-2026-11-19-round2/en/04-active-work-session-font-200.png) ("Work se…").

Change: when the strip is forced inline and the full label `work_session_active` does not fit on one line, use a new short label `work_session_active_short` "Session" (FI "Istunto", other locales likewise) instead of truncating. Measure with `rememberTextMeasurer`. Never show an ellipsized label in the strip.

---

## Intentionally unchanged

- The flat purple project image in the round-2 shots is a solid-color test photo.
- German "Markier…" truncating only at font scale 1.3 in markup mode is acceptable.
- The three equal completion bars are allowed by `MINIMUM_CHART_BARS`; only their labels change (R5).

## Final checklist

- [ ] R1 to R6, V1 to V3, T1 to T4 done.
- [ ] New and changed strings exist in all 12 value folders; unused keys deleted; `git grep` checks from T1 and T2 return nothing.
- [ ] Contrast tests for V1 added and green.
- [ ] `AdaptiveCounterTest` (R1), palette test (R2), estimator unit test (R3) green; full unit tests and lint green.
- [ ] Screenshots re-taken into `reports/knittools-all-screens-en-2026-11-19-round3/`, covering every link in this plan in EN and FI, plus DE for R2, R5 and V2.
- [ ] `CLAUDE.md` / `ui.md` / `AGENTS.md` / `CODEX.md` updated for T1 wording, and for R1 if the documented counter layout rule changes.
- [ ] No commit, push or PR unless Emma asked.
