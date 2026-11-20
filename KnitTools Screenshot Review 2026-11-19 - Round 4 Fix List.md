# KnitTools Screenshot Review 2026-11-19: Round 4 Fix List

Follow-up to `KnitTools Screenshot Review 2026-11-19 - Round 3 Fix Plan.md`. Round 3 is implemented and verified from `reports/knittools-all-screens-en-2026-11-19-round3/`. Only the three items below remain. Do not change anything else.

Implementer: Codex. Owner and reviewer: Emma.

Ground rules are the same as rounds 1 to 3:
- `CLAUDE.md`, `AGENTS.md`, `CODEX.md` and `.claude/rules/ui.md` are binding.
- Use shared components and theme tokens.
- No `·`, `•` or ` | ` separators.
- All 12 locales for any string change.
- Finnish comments and commit messages.
- `UiConsistencySourceTest` exception lists must not grow.
- No commit, push or PR unless Emma asks.

---

## 1. The stitch tracker is half visible under the bottom navigation

Screens: [EN 04-active-work-session](reports/knittools-all-screens-en-2026-11-19-round3/en/04-active-work-session.png), [FI 04-active-work-session](reports/knittools-all-screens-en-2026-11-19-round3/fi/04-active-work-session.png). The top edge of the Stitches pill and the tops of its − and + buttons show just above the navigation bar.

Cause: `ui/screens/counter/CounterHeroLayout.kt:146`. When space is short, the compact candidate excludes the stitch tracker (`includeStitchTracker = false`). The comment there says "the tracker and bottom spacing can scroll below the repeat, target and primary actions". The tracker then starts wherever the hero content ends, which on this phone is a few dp above the fold.

Letting the tracker scroll below the fold is acceptable. Showing a sliver of it is not: it looks broken and invites taps on the edges of the buttons.

Change: when the chosen layout leaves the tracker out of the measured hero, the tracker must start **at or below the fold**. Do this by keeping the hero body's `minimumBodyHeight` equal to the available body height in that branch, so the tracker item begins exactly at the fold and is revealed by scrolling. Do not add a hardcoded spacer; use the existing measured heights.

Test (`AdaptiveCounterTest`): use the same device size and state as the round-3 R1 test (session, REPEAT, target, stitch tracking). Assert that the stitch tracker is either fully inside the viewport or entirely below it, never intersecting the bottom edge. Add a second case at a height where the tracker fits, and assert that it is fully visible.

## 2. Brand is shown twice in the yarn name

Screens: [EN 02-overview-active-session](reports/knittools-all-screens-en-2026-11-19-round3/en/02-overview-active-session.png) and [FI 02-overview-active-session](reports/knittools-all-screens-en-2026-11-19-round3/fi/02-overview-active-session.png) show "Alpine Alpine Merino DK". The yarn card itself ([EN 08-07-yarn-actions](reports/knittools-all-screens-en-2026-11-19-round3/en/08-07-yarn-actions.png)) has the brand "Alpine" and the name "Alpine Merino DK".

Cause: `domain/model/YarnCard.kt:24`, where `YarnCard.displayName` always joins brand and name.

Change: if the trimmed name already starts with the trimmed brand (case-insensitive, followed by a space or the end of the name), return the name alone. Otherwise keep "brand name". Do not change stored data.

Test: a unit test for `displayName` with these cases:
- brand "Alpine", name "Alpine Merino DK" → "Alpine Merino DK"
- brand "Alpine", name "Merino DK" → "Alpine Merino DK"
- brand "Alp", name "Alpine Merino" → "Alp Alpine Merino" (not a word boundary)
- brand blank → name
- name blank → brand
- both blank → fallback.

## 3. Re-take two stale Finnish screenshots

[FI 04-stitches-per-row](reports/knittools-all-screens-en-2026-11-19-round3/fi/04-stitches-per-row.png) and [FI 04-target-rows](reports/knittools-all-screens-en-2026-11-19-round3/fi/04-target-rows.png) were captured at 12.14–12.15, before the round-3 R1 counter fix was screenshotted at 12.39–12.40. They still show REPEAT ("TOISTO") below Undo during an active session.

Re-take both in EN and FI with an active session and stitch tracking enabled, the dialog open, and animations finished. Confirm that REPEAT stays above the row label. If it does not, the state "session + stitch tracking" still flips the layout: fix that under the round-3 R1 rule, and extend the R1 test to that state.

---

## Checklist

- [ ] Items 1 and 2 done with their tests; unit tests, lint and `AdaptiveCounterTest` green.
- [ ] Screenshots re-taken into `reports/knittools-all-screens-en-2026-11-19-round4/`: the counter with an active session and stitch tracking, normal and with each dialog open (EN, FI); the project overview yarn section (EN, FI).
- [ ] No other changes.
- [ ] No commit, push or PR unless Emma asked.
