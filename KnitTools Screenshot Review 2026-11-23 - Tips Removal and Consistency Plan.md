# KnitTools Screenshot Review 2026-11-23: Tips Removal and Consistency Plan

Source: the Pixel 9 gallery in `reports/knittools-pixel9-kuvagalleria-2026-11-23/` (fi, en, sv at 100 % and 200 % font scale). Emma has approved every item below. Visual choices come from the mockups in the Claude session of 2026-11-23; each choice is marked **Choice**. Emma delegated these choices, and they are final:
- 1A and 3A: contextual notices use the same cardless hairline row as the Insights Pro prompt, so the app has one notice style. The icon in 1B added a third decorative element next to the cross-stitch glyphs and fabric swatches without adding meaning.
- 2A: the action sits where the remaining count was, so the counter hero keeps its height and Lopeta and Kumoa still fit in the first view. A pill (2B) would add a row and compete with the 3D buttons.
- 3A over 3B: follow-main-counter mode is the main benefit of the reading line, and the toolbar button alone does not reveal it. One dismissible row teaches it once without a floating overlay.
- 4A: the box style (4B) stacks a rounded container on a beige page that already has cards; the cardless note matches the guide and Insights editorial style.
- 5A: keeps the color identity of each tool and fixes readability. A single dark title color would lose the identity.

Implementer: Codex. Owner and reviewer: Emma.

Ground rules (same as the 2026-11-19 rounds):
- `CLAUDE.md`, `AGENTS.md`, `CODEX.md` and `.claude/rules/ui.md` are binding.
- Use shared components and theme tokens. No hardcoded colors, dimensions, `fontSize`, `fontWeight` or `letterSpacing`.
- No `·`, `•` or ` | ` separators. No colored vertical bar on the left edge of a card, row or panel.
- All 12 locales (default en, da, de, es, fi, fr, it, nb, nl, pt, pt-rBR, sv) for every string change. Preserve placeholders and plurals. Delete unused keys in every locale.
- Finnish comments and commit messages.
- `UiConsistencySourceTest` exception lists must not grow.
- Delete dead code, tests and resources in the same change. Grep all callers on rename or removal.
- No commit, push or PR unless Emma asks.

**Out of scope, do not touch:** the 3D illustrations (Pro page yarn crate, onboarding illustrations). They are deliberate and stay as they are.

Work in four packages, in order: A, B, C, D. Each package must build and pass `./gradlew :app:testDebugUnitTest` before the next one starts.

---

## A. Remove the floating tip system

### Why

The tip bubble is the only floating overlay in an app that otherwise uses sheets, dialogs and hairline editorial content. It uses the same `cardContainer` color as project cards and has no elevation, so it reads as a misplaced card. Of the 14 tips, most repeat onboarding (FIRST_ROW, UNDO), the guide (OVERVIEW_INTRO, FOLDERS, TOOLS_INTRO) or visible UI (PATTERN_TILE, WORK_SESSION, WIDGET). Three tips point at the same ⋮ menu. Wording also drifts: the tip action says "Pidä näyttö päällä" while the setting says "Pidä näyttö hereillä".

Learning lives in two places after this change: onboarding and the bundled guide (with its existing Kokeile actions). Three useful tips become ordinary UI states (A2–A4) and one optional one-time row stays (A5).

### A1. Delete the system

Delete completely:
- `ui/tips/` (`TipHost`, `TipSession`, `TipAnchor`, `TipEnvironment`, `TipText`, `StitchMarkerTip`, `InlineTipCard`, `LocalTipBlocked`, `LocalTipRouteKey`, `LocalTipSession`)
- `domain/tips/` (`TipId`, `TipRules`, `TipContext`, `TipSignal`, `BASIC_TIPS`)
- All `Modifier.tipAnchor`, `tipProtected` and `tipBlockWhenFocused` call sites: `CounterScreen.kt`, `CounterWorkspaceSections.kt`, `CounterProjectContentCards.kt`, `HomeScreen.kt`, `PatternViewerScreen.kt`, `ProjectListScreen.kt`, `ProjectOverviewScreen.kt`, `ProjectOverviewSections.kt`.
- Tip wiring in `MainActivity.kt` (`tipSession`, `tipsBlocked`, providers) and `NavGraph.kt` (`TipEnvironment`, `onRouteChanged`, `LocalTipBlocked`).
- `CounterViewModel` tip signals (`TipSignal.Increment`, `TargetReached`, `SessionSaved`) and the `TipSession` dependency.
- "Näytä vihjeet uudelleen" in both Settings (`SettingsViewModel.showTipsAgain`) and the guide root (`GuideScreen` `onResetTips`, `GuideViewModel.showTipsAgain`, `GuideNavigation`). Strings `settings_show_tips_again`, `settings_tips_reset_done`.
- Strings: every `tip_*` key used only by the tip system (`tip_got_it`, `tip_first_row_*`, `tip_undo`, `tip_step_size`, `tip_work_session`, `tip_overview_intro`, `tip_pattern_tile`, `tip_reading_line`, `tip_keep_screen_on`, `tip_keep_screen_on_action`, `tip_target_reached`, `tip_first_session_saved`, `tip_backup`, `tip_backup_action`, `tip_folders`, `tip_widget`, `tip_widget_action`, `tip_tools_intro`) and `journey_open_pattern_tip`. **Keep** the `tip_*_title` / `tip_*_desc` keys used by `InfoTip` field help (pattern repeat, edge stitches, flat vs circular, needle sizes, imperial units); they are not part of this system. Reuse the backup text in A2 by moving it to a new key, not by keeping a `tip_` name.
- Dimensions in `OnboardingDimens` used only by tips (`TipMaxWidth`, `TipPadding`, `TipNotchSize`, `TipMarkerSize`, `TipMarkerStroke`, `TipSlideDistance`, `TipScreenInset`, `TIP_*` constants).
- Analytics: `UsageEvent.TIP_SHOWN`, `TIP_ACTED`, `TIP_DISMISSED` and the `tip` feature entries in `UsageAnalytics.kt`.
- Tests: `TipRulesTest`, `TipSessionTest`, `TipHostTest`, `CounterTipSignalsTest`. Update `CounterViewModelFixture`, `GuideViewModelTest`, `SettingsViewModelTest`, `CounterCelebrationSignalsTest`, `LibraryLaunchSurfaceSourceTest`, `TrialNoticeRestorationTest`, `CounterHistoryTestRepository` and `ProjectSetupNavigationTest` so they no longer reference tips.

`OnboardingStateRepository` (`onboarding_state` DataStore):
- Remove the tip-only fields and keys: `tipsSeen` (`tips_seen`), `counterOpenCount`, `counterOpenDays`, `counterOpenLastDay`, `firstSessionTipPending`, `targetCelebratedProjects` (+ order key), `projectIncrementCounts`, `firstSaveObserved`, and `guidedPatternProjectId` if nothing else reads it after `journey_open_pattern_tip` is gone. Verify each with a grep before removing.
- **Keep** `firstUseEpochDay` and `backupCreated`; A2 needs them.
- Remove `markBasicTipsSeen` and `resetTips`, and the call in `BackupViewModel` (restore). Keep the backup-export call that sets `backupCreated`.
- Add a one-time cleanup that removes the deleted keys from existing installs on first read (`editPreferencesSafely`), so no orphaned data stays on disk. Cover it in `OnboardingStateRepositoryTest`.

Project setup (`ProjectSetupList.kt`) currently depends on `LocalTipSession` and `LocalTipRouteKey` for route-entry generation:
- Replace that with a route-entry generation owned outside the tip system (for example in `CounterViewModel` or the NavGraph entry), preserving the documented behavior: "route-entry generation distinguishes a return to a reused navigation entry from recreation" and the hide-on-later-visit and celebration rules. `ProjectSetupNavigationTest` must pass unchanged in intent.
- The WORK_TIME setup row currently calls `openCounterWithWorkSessionTip`. Replace it with: open Counter through the existing navigation and open `ProjectActionsBottomSheet` with the Työskentelyistunto section visible. Do **not** start a session (CLAUDE.md: a session starts only from Start). Delete `openCounterWithWorkSessionTip` and `onOpenCounterWithWorkSessionTip`.

PDF reader: the reading-line toolbar button stays. It was documented as the READING_LINE tip anchor; it is now just the reading-line settings button.

### A2. Backup notice in Projects

Replaces the BACKUP tip. Same eligibility as today: at least one project in any folder, no backup created, first use at least 7 days ago. Shown below the active list where `InlineTipCard` was.

**Choice 1A:** a cardless row between hairlines, no icon:
- Body: "Projektisi ovat vain tässä puhelimessa." (move the existing wording to a new key such as `projects_backup_notice`; shorten the other locales the same way).
- One action row: "Ohita" at the start in muted text, "Tee varmuuskopio" at the end in `primaryReadable`. Use `Row` with `SpaceBetween`, not a separator character.
- Ohita stores a new persistent `backupNoticeDismissed` flag in `OnboardingStateRepository`. Tee varmuuskopio opens Backup. A successful export sets `backupCreated` as today, which hides the row.
- Use existing hairline and section tokens (same pattern as the Insights Pro prompt). No card, no elevation.

Test: Compose test for visibility rules (fewer than 7 days, backup created, dismissed, no projects), and that Ohita persists across recreation.

### A3. Target reached in Counter

Replaces the TARGET_REACHED tip. `counter_target_reached` ("Tavoite saavutettu") already exists in the progress line.

**Choice 2A:** when `count >= target`, the trailing end of the progress line under the bar shows the text action "Merkitse valmiiksi" (`primaryReadable`) instead of the remaining count. The leading end shows "Tavoite saavutettu" as today. Over-target (`counter_target_past_format`) keeps the same action.
- The action calls the existing completion flow (same confirmation dialog and `ProjectCompletedSheet` as the overview menu "Merkitse valmistuneeksi"). Reuse it, do not add a second path.
- Completed projects never show it.
- The hero height measurement must include the line so Lopeta and Kumoa still fit in the first view (`AdaptiveCounterTest` at 100 % and 200 % font scale).

### A4. Keep screen awake in the Counter actions sheet

Replaces the KEEP_SCREEN_ON tip. Add a "Pidä näyttö hereillä" switch row to `ProjectActionsBottomSheet` under LASKURITYÖKALUT, directly after "Seuraa silmukoita", bound to the same `PreferencesManager.keepScreenAwake` as Settings (one source of truth, same `keep_screen_awake` string). Reuse the existing switch row component used by "Seuraa silmukoita".

### A5. Reading-line notice in the PDF reader

**Choice 3A:** a one-time row directly above the reader's bottom bar, only when the project has a main counter, follow-main-counter mode is off and the notice has not been dismissed:
- Body: "Lukulinja voi siirtyä laskurin mukana." (new key).
- Action row: "Ohita" at the start, "Ota käyttöön" at the end. Ota käyttöön turns on follow-main-counter mode through the existing setting; both actions set a new persistent `readingLineNoticeDismissed` flag.
- Same hairline row style as A2. Hidden in markup mode and while any reader menu or sheet is open. It must not cover PDF content: the page area shrinks while the row is shown.

### A6. Documentation

- `AGENTS.md` and `CODEX.md`: delete the "Guidance uses shared `TipHost`…" paragraph. Rewrite the onboarding, celebration, PDF reading-mode, guide and project-setup paragraphs so they no longer mention tips, tip anchors, tip reset or `TipSession`. Add one short paragraph that states the new rule: no floating tips or coach marks. Teaching belongs to onboarding and the guide. Contextual notices are cardless hairline rows (A2, A5) with a persistent dismissal.
- Mention A2–A5 where the owning screen is described.

---

## B. Terms and copy

### B1. "Liitä ohje" means two things

Projects and the guide use "Liitä ohje" for attaching a pattern to a project. The yellow paste button in Lisäykset / kavennukset and Mitat ja tiheys (`PasteInstructionButton`, `paste_instruction`) also says "Liitä ohje", but it pastes instruction text from the clipboard.
- fi: `paste_instruction` → "Liitä ohjeteksti" (the Pro prompt already says "Liitä ohjeteksti").
- fi: `attach_pattern` and `project_content_add_pattern` say "Lisää ohje". CLAUDE.md fixes the term as "Liitä ohje": change both to "Liitä ohje", and the attach sheet title to match.
- Check every other locale for the same collision between the paste button and the attach-pattern label; fix only where they collide.

### B2. One way to show counter progress

Today: "Laskurin etenemä: 28" (`counter_progress_format`, Insights and the Stop session dialog) and "Laskuri: 79 → 86" (`session_row_range`, work history).
- Delta: "Laskuri +%1$d". Range: "Laskuri %1$d → %2$d". No colon in either. Apply the same pattern in all locales (en "Counter +28", "Counter 79 → 86").

### B3. Settings section names

"TIEDOT" and "TIETOA" sit next to each other and mean almost the same.
- `settings_section_data` → fi "Varmuuskopiot", en "Backup".
- `settings_section_info` → fi "Ohje ja tuki", en "Help and support".
- Store all section labels in normal case and let the section label style handle capitals (today `PRO`, `TIEDOT`, `TIETOA` are stored in capitals but `Yleiset` is not). Keep `PRO` as a brand.

### B4. Move-to-folder sheet title

"Siirrä projekti Forest Cardigan kansioon" reads awkwardly. Title becomes "Siirrä kansioon" (all locales, new key) and the project name goes on its own line below the title in secondary text. Delete `folder_move_project_to`.

### B5. Guide: folders item

The guide's Kansiot item ends with the free-quota rule ("Yksi työn alla oleva projekti kerrallaan on maksuton…"). Remove that sentence in all locales. Pro rules belong to the Pro page.

---

## C. Component consistency

### C1. One page-level info note

There are three styles today: a filled box with the icon on the left (Kaaviomerkit, Lankalaskuri, `InfoNote`), and plain text with a tappable (i) on the right (Puikkokoot, Mitat ja tiheys, `InfoTip` rows).

**Choice 4A:** cardless. Info icon at the start in `primaryReadable`, `bodySmall` text in `onSurfaceVariant`, a hairline below. No background, no rounded box.
- Restyle `InfoNote` to this and move its hardcoded `20.dp`, `18.dp`, `12.dp` and `alpha = 0.6f` into theme tokens.
- Use `InfoNote` for every page-level note: Kaaviomerkit, Lankalaskuri, Puikkokoot (`NeedleSizeScreen.kt:78`) and Mitat ja tiheys (`GaugeScreen.kt:219`). If an `InfoTip` dialog only repeats the visible text, drop the dialog. If it holds more text, show the full text in the note.
- Field-level help stays as it is (`LabelWithInfo`, `NumberInputField(info …)`).
- Add a source test that page-level notes use `InfoNote`.

### C2. Multi-field forms are sheets

"Lisää muistutus" is a dialog with four inputs and a segmented control, and its Tallenna is a text button. "Lisää laskuri" of the same size is a `FormSheet` with a filled Tallenna.
- Rule: one input uses a dialog, more than one uses `FormSheet`.
- Move add/edit reminder (`ReminderComponents.kt`) to `FormSheet` with `FormSheetActions`. Keep validation and the repeating fields exactly as they are.
- Single-input dialogs stay (Luo kansio, Lisää kirjanmerkki, Siirry sivulle).
- Add the rule to `.claude/rules/ui.md`.

### C3. One filled primary action per Pro page

On the Pro page, "Aloita 14 päivän ilmainen kokeilu" is an outlined button even when it is the only action, while other pages use a filled main action (Vie varmuuskopio, Avaa verkkosivu).
- When the purchase price is unavailable or the trial is the only available action, the trial button is filled.
- When the purchase is available, the purchase stays filled and the trial stays outlined.
- Same rule in the Settings Pro row and the onboarding trial step, where the same component is used. Do not touch the illustration.

### C4. Folder sheet empty state

In the folder picker, "Ei kansioita" uses a larger text style with a hairline and looks like a selectable row.
- Show it as muted `bodyMedium` text "Et ole vielä luonut kansioita." (new key) without a divider or touch target, between Kansiottomat and Luo kansio. Same in the move-to-folder sheet.

### C5. Table headers stay visible

The Puikkokoot table header (mm, US, UK, JP) scrolls away, which leaves unlabeled numbers. Use `stickyHeader` for the table header row. Do the same for Kokotaulukot if its table header scrolls with the rows. The search field and note scroll normally.

### C6. Library first section label

In `02-library/01-library.png` the first section label (OMA KOKOELMA) is half under the title. Check on a device whether the list opens scrolled or the content padding is wrong. Fix only if it reproduces, and note the result in the hand-off.

---

## D. Title color contrast

In Työkalut and Kirjasto, each item title uses an accent color. On the light card (`LightCardContainer` `#D6D0B8`), contrast is: primary `#C45100` 3.0:1, tertiary `#9A7B18` 2.6:1, dusty rose `#9E706E` 2.7:1. The mustard and rose titles fail even the 3:1 large-text minimum.

**Choice 5A:** keep the hues, darken them for titles in the light theme.
- One function in the theme (for example `MaterialTheme.knitToolsColors.titleAccent(role)`) maps each accent to a readable title color. Use existing tokens where they fit: primary → `LightPrimaryReadable` `#9A3F00` (4.4:1), rose → `LightSectionLabel` `#7F5452` (4.1:1). Add one new token for mustard, `#6E5810` (4.4:1). Secondary and teal already pass.
- Dark theme: verify the same accents against the dark card colors and add mappings only where they fail.
- Use the function in `HomeScreen.kt` and `LibraryScreen.kt`. No other screen changes.
- Add a contrast unit test (pattern of `ProjectListContrastTest`) that every title accent reaches 4.5:1 on its card in both themes, or at least 3:1 if the title style qualifies as large text. Document which threshold you used.

---

## Verification

- `./gradlew :app:testDebugUnitTest`, `lint-check` clean for touched files, and the affected instrumented tests (`AdaptiveCounterTest`, `ProjectSetupNavigationTest`, the new A2/A5 tests).
- `grep -rn "Tip\(Host\|Session\|Id\|Rules\|Context\|Signal\)\|tipAnchor\|StitchMarkerTip\|InlineTipCard" app/src` returns nothing.
- Regenerate the gallery into `reports/knittools-pixel9-kuvagalleria-<date>/` (fi, en, sv, 100 % and 200 %) and include in the hand-off: the A2 and A5 rows, the A3 target state, the actions sheet (A4), the B1–B4 screens, C1 on all four tool pages, the reminder sheet, the Pro page without a price, the folder sheet with no folders, the scrolled needle table, and Työkalut and Kirjasto (D).
- Hand-off lists each item as done, changed or not reproduced, with file references.
