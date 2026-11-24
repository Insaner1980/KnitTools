# KnitTools Screenshot Review 2026-11-23: Round 2 Fix List

Follow-up to `KnitTools Screenshot Review 2026-11-23 - Tips Removal and Consistency Plan.md`. That plan is implemented and verified from `reports/knittools-samsung-a13-kuvagalleria-2026-11-23/`. Only the items below remain. Do not change anything else.

Implementer: Codex. Owner and reviewer: Emma.

Ground rules are the same as round 1:
- `CLAUDE.md`, `AGENTS.md`, `CODEX.md` and `.claude/rules/ui.md` are binding.
- Use shared components and theme tokens.
- No `·`, `•` or ` | ` separators.
- All 12 locales for any string change. Preserve placeholders.
- Finnish comments and commit messages.
- `UiConsistencySourceTest` exception lists must not grow.
- No commit, push or PR unless Emma asks.

---

## 1. The stop-session dialog title breaks in the middle of a word

Screen: `fi/200/06-stop-session.png`. At 200 % font scale the title shows "Lopetetaanko työskentelyistunt" on one line and "o?" on the next.

Cause: `work_session_stop_summary_title` (used at `ui/screens/counter/CounterScreen.kt:1608`) contains the long Finnish compound "työskentelyistunto". The dialog is too narrow for it at 200 %.

Change:
- fi: "Lopetetaanko istunto?". This matches `work_session_active_short` ("Istunto"), which the counter's session line already uses at large font scales.
- Check the other 11 locales at 200 % for the same dialog. Shorten only those whose title breaks inside a word, using the locale's existing short session word. Leave the rest as they are.
- Then check every other dialog and sheet title at 200 % in fi, de, nl, sv and nb (the locales with long compounds) for mid-word breaks. Report each one you find. Fix it only by shortening the copy, never by reducing the text style.

Test: a Compose test that renders the stop-session dialog at font scale 2.0 in fi and asserts that the title's text layout has no line that ends inside a word. Compare each line end against a word boundary. If a reusable helper for this check fits, put it in the test utilities so later dialogs can use it.

## 2. The needle size note repeats itself

Screens: `fi/100/02-needles-note.png`, `fi/200/02-needles-note.png`, `sv/100/02-needles-note.png`. Merging the old info dialog into the note made the text mention Japanese sizes twice in fi and sv. At 200 % the note fills almost the whole first view, which pushes the table out of sight.

Cause: `needle_size_disclaimer` now holds both the old visible note and the old dialog text.

Change: one short paragraph, no `\n\n`, in every locale. Use the English text as the model: it is already close to the target.
- en: "Sizes are approximate equivalents. Numbering varies by system and manufacturer, so compare the diameter in millimeters and check your gauge."
- fi: "Koot ovat likimääräisiä vastaavuuksia. Numerointi vaihtelee järjestelmän ja valmistajan mukaan, joten vertaa halkaisijaa millimetreinä ja tarkista neuletiheys."
- sv and the other locales: translate the English sentence. Use the same term for "gauge" that each locale already uses in the gauge tool.

Keep the note in the shared `InfoNote`. Do not reintroduce the dialog.

## 3. "Ohita" does not line up with the notice text

Screens: `fi/100/01-backup-notice.png` and `fi/100/07-reading-line-notice.png`, and the same rows in en and sv. The message starts at the content edge, but "Ohita" ("Skip", "Hoppa över") starts about 14 px further in.

Cause: `ui/components/ContextualNotice.kt`. The skip action is a `TextButton` with `contentPadding = PaddingValues()`. `TextButton` still enforces `ButtonDefaults.MinWidth`, and a short label is centered inside that minimum width.

Change: the skip label's text must start exactly at the message's start edge, and the action label's text must end exactly at the message's end edge, in every locale and at both font scales. Keep a touch target of at least 48 dp for both actions, keep the button role and keep the current colors. For example, use a clickable text with `minimumInteractiveComponentSize()` and start or end content alignment. Do not use a negative offset.

Test: a Compose test for `ContextualNotice` in fi and en, at font scales 1.0 and 2.0. It asserts that the skip text's left bound equals the message text's left bound, and that the action text's right bound equals the message's right bound. Allow at most 1 px of difference.

## 4. Detekt in `PatternViewerScreen.kt`

`detekt` reports `PatternViewerScreen.kt` with 31 functions, above the limit of 30. The reading-line notice was added to this file in round 1, so this finding cannot be treated as unrelated.

Change: move the reading-line notice wiring (and any helper that belongs with it) out of `PatternViewerScreen.kt` into an existing pattern-reader file, or into a new small file next to it. Do not raise the Detekt limit, and do not add a suppression. Do not touch the other six Detekt findings in this round. They were already in files with changes made before round 1.

## Not a change: the create button over the backup notice

At 200 % (`fi/200/01-backup-notice.png`) the Uusi projekti button covers the notice's bottom divider when the screen first opens. The list already reserves `ProjectListDimens.ListBottomPaddingWithCreateButton`, so the notice can scroll clear of the button. Leave it as it is.

---

## Verification

- `./gradlew :app:testDebugUnitTest` and `lint-check`. Detekt must have no finding in `PatternViewerScreen.kt` or in any file touched in this round.
- Regenerate these screens in fi, en and sv at 100 % and 200 %: stop-session dialog, needle sizes note, backup notice, reading-line notice. Add any dialog that item 1's sweep changed.
- Hand-off lists each item as done or changed, with file references, plus the list of dialog titles checked in item 1.
