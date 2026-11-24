# KnitTools Screenshot Review 2026-11-23: Round 3 Fix List

Follow-up to `KnitTools Screenshot Review 2026-11-23 - Round 2 Fix List.md`. Round 2 is implemented and verified from `reports/knittools-pixel9-korjauskierros-2-2026-11-23/`. Only the items below remain. Do not change anything else.

Implementer: Codex. Owner and reviewer: Emma.

Ground rules are the same as rounds 1 and 2:
- `CLAUDE.md`, `AGENTS.md`, `CODEX.md` and `.claude/rules/ui.md` are binding.
- Use shared components and theme tokens.
- No `·`, `•` or ` | ` separators.
- Preserve placeholders. Change only the locales named below unless an item says to check all of them.
- Finnish comments and commit messages.
- `UiConsistencySourceTest` exception lists must not grow.
- No commit, push or PR unless Emma asks.

---

## 1. A dialog title uses a different word for "pattern" than its body

Screens: `dialogit/de-web_pattern_replace_confirm_title.png` and `dialogit/nb-web_pattern_replace_confirm_title.png`.

Round 2 shortened `web_pattern_replace_confirm_title`, and the new titles use a word the rest of the app does not use for a pattern:
- de: the title says "Musterdaten ersetzen?", but the body (`web_pattern_replace_confirm_message`) says "Anleitungsdaten", and the app uses "Anleitung" everywhere (`attach_pattern` = "Anleitung hinzufügen").
- nb: the title says "Erstatte mønsterdata?", but the body says "oppskriftsopplysningene", and the app uses "oppskrift" (`attach_pattern` = "Legg til oppskrift").

Change:
- de: "Anleitungsdaten ersetzen?"
- nb: "Erstatte oppskriftsdata?"
- Measure both with the round 2 title check (200 %, 411 dp and 360 dp). If either one breaks inside a word, use "Daten ersetzen?" or "Erstatte dataene?" instead. The body already names the pattern.
- Check `web_pattern_replace_confirm_title` against `attach_pattern` and `web_pattern_replace_confirm_message` in all 12 locales. Every locale must use one word for a pattern across the title, the body and the attach action. Report any other mismatch and fix it the same way.

## 2. The delete button uses a longer noun than its dialog title

Screens: `dialogit/de-yarn_usage_delete_title.png` and `dialogit/nl-yarn_usage_delete_title.png`. The title was shortened in round 2, but the destructive button was not. The button now uses a different and longer noun, wraps to two lines, and pushes Cancel out of line.
- de: title "Verbrauch löschen?", button `yarn_usage_delete` "Verbrauchserfassung löschen"
- nl: title "Verbruik verwijderen?", button "Verbruiksregistratie verwijderen"

Change `yarn_usage_delete` so it matches its title, the way English does ("Delete usage?" with "Delete usage"):
- de: "Verbrauch löschen"
- nl: "Verbruik verwijderen"

The other locales already match. Do not change them. `yarn_usage_delete_named` (the content description with the yarn name) keeps its current wording.

Test: extend the round 2 title check, or add a resource test, so that in every locale `yarn_usage_delete_title` without its question mark equals `yarn_usage_delete`. Allow only the locale's own question-mark spacing (for example the space before "?" in French).

## 3. English "Under 1min" is missing a space

Screen: `en/200/06-stop-session.png` ("Session Under 1min", "Duration: Under 1min").

Change: `values/strings.xml` `work_session_under_minute` → "Under 1 min". Every other locale already has the space.

## 4. The price loading indicator looks like a stray comma

Screen: `dialogit/nb-trial_ended_screen_title.png`. Above "Laster pris…" there is a small orange arc on its own line. At that size it reads as a stray comma, not as a loading indicator.

Cause: `ui/components/ProPriceDetails.kt:52–55`. In the `BillingProductStatus.Loading` branch, `CircularProgressIndicator` and the `loading_price` text are separate children of the surrounding column, so they stack.

Change: put the indicator and the text in one `Row`, vertically centered, with the indicator first and the existing standard spacing between them. Keep `ComponentDimens.StandardIconSize`. All three users of `ProPriceDetails` (the Pro page, the contextual prompt and the trial-ended notice) get the fix automatically. Do not add a separate layout for any of them.

---

## Verification

- `./gradlew :app:testDebugUnitTest` and `lint-check`. Detekt must have no finding in any file touched in this round.
- Regenerate at 200 % in dark and light: the de and nb replace-pattern dialogs, the de and nl delete-usage dialogs, the en stop-session dialog, and the nb trial-ended dialog while the price is loading.
- Hand-off lists each item as done or changed, with file references, plus any extra locale mismatches found in item 1.
