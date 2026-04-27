# Counter Screen Redesign — Minimal Layout + Bottom Sheet Menu

## Goal

Strip the counter screen down to only what is essential while knitting (counter, primary buttons, optional pattern repeat row). Move all secondary actions and project metadata into a bottom sheet menu opened from the existing three-dot icon in the top bar.

The counter number ("Row 99") must stay anchored — same size and roughly the same vertical position regardless of which optional rows are present (pattern repeat, etc.). This stability is the core of the redesign; the screen should feel calm rather than rearranging itself when state changes.

## Scope

In scope:
- Modify `ui/screens/counter/CounterScreen.kt`
- Add a new `ProjectActionsBottomSheet` composable (new file in `ui/screens/counter/` or a sibling location consistent with how other bottom sheets are organized in the project — check existing patterns first)
- Add a contextual "Show pattern" icon to the counter screen top bar
- Wire the three-dot icon to open the new bottom sheet instead of its current behavior

Out of scope (do not modify):
- Project list screen, library, tools, settings, or any other top-level screen
- Counter logic, repository code, ViewModel state shape, or any database/Room code
- Visual design tokens or theme colors (use existing tokens — do not introduce new ones)
- Glance widget
- Any AI / Nano / OCR functionality
- Bottom navigation bar

## What is removed from the main counter screen

Remove these from the visible main layout (they move to the bottom sheet, except where noted):

1. **Project info card** at the top showing yarn name, stitch count, elapsed time, "View AI summary" link, notes icon, and "Track stitches" toggle. Remove the entire card.
2. **"View AI summary" link** — moves to bottom sheet.
3. **Notes icon** in the project info card — moves to bottom sheet.
4. **"Track stitches" toggle** — moves to bottom sheet as a toggle row.
5. **"Counters" link** at the bottom of the screen — moves to bottom sheet.
6. **"Add counter" link** at the bottom of the screen — moves to bottom sheet.
7. **"Show pattern" link** (visible when project has a pattern attached, e.g. PDF) — moves to top bar as an icon (see below), not bottom sheet.

Stitch count and elapsed time are no longer shown on the main counter screen at all. Time tracking continues to run in the background; the value is visible elsewhere (Insights, session history). Stitch count likewise stays accessible via the "Yarn" or "Counters" sheet entries — confirm where it currently surfaces and keep it reachable, but do not add it back to the main screen.

## What stays on the main counter screen

In this exact vertical order:

1. Status bar (system)
2. Top bar: back arrow · `[Show pattern icon — only if project has pattern]` · camera · mic · three-dot menu
3. Project title (e.g. "CLASSIC RIBBED HAT") — keep current size and weight
4. Pattern source line (e.g. "Classic Ribbed Hat · Ravelry") in primary orange — keep
5. **(Optional, only if a Repeating-type counter is active for this project)** Compact pattern repeat row — see specs below
6. Counter block: "Row N / Total" label, big counter number, thin progress bar — see specs below
7. Button row: minus, big plus (wood button), undo
8. Bottom navigation

No "Counters" / "Add counter" links between the button row and the bottom nav. That space is intentionally empty.

## Anchoring requirement (important)

The counter number and button row must occupy the same vertical position whether or not the pattern repeat row is present. Do this by giving the counter+buttons a flex/weight container that centers vertically in the available space, and letting the optional pattern repeat row push down from the top instead of pushing the counter down.

Concretely: the counter number should not visibly jump or resize when a Repeating counter is added or removed. Test by toggling Repeating on/off and confirming the "99" stays in roughly the same screen position at the same size.

## Compact pattern repeat row spec

This replaces the current larger pattern repeat UI on the counter screen. It is rendered only when the project has a Repeating-type counter active.

- Container: rounded rectangle, surface tone matching existing `surface1` (the lighter khaki used for cards in the current theme). Corner radius 12dp. Horizontal margin matches other content (likely 20dp).
- Height: ~44dp (vs. the current pattern repeat block which is significantly taller).
- Layout: single horizontal row, space-between.
  - Left: label "REPEAT" in small caps style — 12sp, weight 700, secondary text color, letter-spacing slightly increased.
  - Right: minus button (28dp circle, surface2 background) · current value (e.g. "4/68", 14sp weight 700) · plus button (28dp circle, surface2 background). Use the existing icon set (Material).
- The value format ("4/68" vs other shapes) should mirror what the current Repeating counter UI displays. Do not change the underlying counter logic or value shape — only the presentation.

If the project has multiple Repeating counters or both a Repeating and a Repeat-section counter, follow whatever the current behavior shows for "primary" counter selection on this screen. If no clear precedent exists in the codebase, surface only the most recently active one and stop. Do not invent a new selection rule.

## Top bar — Show pattern icon

Add a Material `Description` / `PictureAsPdf` / equivalent icon (pick the closest match to what the codebase uses for pattern documents — check `PatternViewerScreen` or related code first) to the top bar between the back arrow and the camera icon.

Visibility rule: shown only if the current project has a pattern attached (PDF or saved Ravelry pattern). Hidden otherwise. Use the same data the existing "Show pattern" link uses to determine visibility.

Tap action: same as the current "Show pattern" link — opens the pattern viewer for this project.

Icon color: same primary orange as camera and mic.

If the top bar gets too crowded on smaller screens (5 icons + back arrow), the pattern icon takes priority over camera/mic. But verify visually before changing anything; on most phones 5 icons + back fit fine.

## Bottom sheet — `ProjectActionsBottomSheet`

Triggered by tapping the three-dot icon in the top bar. Replace whatever the three-dot currently opens with this sheet.

### Structure

Material 3 `ModalBottomSheet`. Drag handle at top (use the standard Material drag handle — do not custom-roll). Three sections separated by thin dividers (1dp, surface2 color, 60% opacity, with horizontal inset matching content).

### Section 1: "THIS PROJECT"

Section header: 11sp, weight 700, tertiary text color, letter-spacing 0.8px, padded `14dp top, 8dp bottom, 22dp horizontal`.

Items (each 48dp tap target minimum, 12dp vertical / 22dp horizontal padding, icon left + label, optional badge or chevron right):

1. **Yarn** — icon: Material `Inventory2` or closest match. Trailing: count of yarn cards attached if any (number, tertiary text, no badge styling), then chevron-right. Tapping navigates to the existing yarn entry/edit flow. Label is always "Yarn" regardless of whether yarn is attached; the destination handles empty state.
2. **Notes** — icon: `EditNote` or `Description`. Trailing: chevron-right. Navigates to existing `NotesEditorScreen`.
3. **AI summary** — icon: `Article` or similar neutral document icon. Do **not** use a sparkle/wand icon. Trailing: chevron-right. Triggers the existing AI summary action that the "View AI summary" link currently triggers.
4. **Photos** — icon: `Image` or `PhotoLibrary`. Trailing: count of progress photos for this project, then chevron-right. Navigates to the existing project photos screen.

### Section 2: "COUNTERS"

1. **Counters** — icon: `FormatListNumbered` or similar. Trailing: total count of counters on this project (incl. main counter, repeating, etc.), then chevron-right. Navigates to wherever the existing "Counters" link navigates today.
2. **Add counter** — icon: `AddCircleOutline`. No trailing element. Triggers the same add-counter dialog the existing "Add counter" link triggers (the one shown in the current Add Counter dialog with Count up / Repeating / Shaping / Repeat section options).
3. **Track stitches** — icon: `Tag` or `Numbers`. Trailing: a Material `Switch` reflecting the same state the current Track stitches toggle reflects. Tapping anywhere on the row should toggle (consistent Material behavior).

### Section 3: "PROJECT ACTIONS"

1. **Session history** — icon: `History`. Trailing: chevron-right. Navigates to existing `SessionHistoryScreen`.
2. **Rename** — icon: `Edit`. No trailing element. Opens existing rename flow.
3. **Reset counter** — icon: `Restore` or `Refresh`. No trailing element. Opens existing reset confirmation dialog.
4. **Complete project** — icon: `CheckCircleOutline`. No trailing element. Opens existing complete confirmation flow.

After "Complete project", insert ~8dp of vertical space (no divider) before the final destructive action:

5. **Delete project** — icon: `DeleteOutline`, both icon and label colored using the existing error/danger color from the theme (do not introduce a new color — find what's used elsewhere for delete actions in the project, e.g. `Trash` references). No trailing element. Opens existing delete confirmation flow.

### Bottom sheet visual notes

- Background: same `bg` color as the rest of the screen (the warm off-white `#E8E4D0` in light theme; use the equivalent dark theme token in dark mode). Do not introduce a new surface color for the sheet.
- Top corner radius 28dp.
- Content padding bottom: 18dp (plus system insets).
- Scrolls if content exceeds height — verify on shorter screens.

## Text color and contrast

Goal: maximize legibility while knitting (often in mixed lighting). The screen should read as **near-black text + selective primary orange for actions/links** in light theme, and the equivalent high-contrast inversion in dark theme.

### Light theme

All text on the counter screen and inside the bottom sheet should use a near-black ink color. If the project already has a token like `onBackground` or a dedicated "ink" / high-contrast text color, use that. If the closest existing token is a soft brown/dark khaki (e.g. `#1E1E12` or similar), check whether a stronger near-black token exists and prefer it. **Do not invent a new color token** — if no suitable near-black exists, stop and report the current text color tokens so I can decide whether to add one.

Affected text elements (light theme, near-black):
- Project title ("CLASSIC RIBBED HAT")
- Big counter number ("99")
- "REPEAT" label in the compact pattern repeat row
- Pattern repeat numeric value (e.g. "4/68")
- Bottom sheet section item labels (Yarn, Notes, AI summary, Photos, Counters, Add counter, Track stitches, Session history, Rename, Reset counter, Complete project)
- Bottom navigation icon and label colors for inactive tabs (verify current value — only darken if not already near-black)
- Back arrow and three-dot icons in the top bar

### "Row N / Total" label change

The "Row 99 / 357" label currently uses the secondary (green) color. Change it to a muted text color — secondary or tertiary text token, whichever produces a clear visual hierarchy where the big counter number is the primary focus and the "Row N / Total" label is supportive but not green. **Remove the green entirely from this element.** It should read as part of the "ink-and-orange" palette of the screen, not as a separate accent.

### What stays orange (do not change)

These elements keep the existing primary orange (`#C45100` or whatever the current primary token is):
- Pattern source line under the title (e.g. "Classic Ribbed Hat · Ravelry")
- Top bar action icons: camera, mic, Show pattern (when visible)
- Big "+" wood button (visual treatment unchanged)
- Active tab indicator and label in the bottom navigation
- Track stitches toggle when in the "on" state (Material Switch with primary color)

### What stays in its existing color (do not change)

- Delete project row in the bottom sheet — keeps the existing danger/error color
- Wood button graphic itself — visual treatment unchanged
- Section header labels in the bottom sheet ("THIS PROJECT", "COUNTERS", "PROJECT ACTIONS") — keep the tertiary text color so they remain visually subordinate to the item rows. The hierarchy should be: section header (small, muted) → item label (near-black, prominent).

### Dark theme

Mirror the same intent in dark theme: text becomes a high-contrast off-white (the equivalent token to whatever ink color is used in light theme). Orange accent elements stay orange — primary orange already works on dark backgrounds. Verify the "Row N / Total" muted text token has sufficient contrast in dark theme; if not, surface this rather than guessing.

### Out of scope for this color change

This text-color change applies **only to the counter screen and the new bottom sheet**. Do not touch text colors on:
- Project list, library, tools, insights, settings, or any reference screens
- Yarn card detail, pattern viewer, notes editor, session history
- Any dialog or other bottom sheet that already exists

If a shared composable is reused between the counter screen and other screens (e.g. a shared text style), prefer applying the new color locally on the counter screen / bottom sheet rather than modifying the shared style.

## Edge cases

- **No pattern attached**: top bar Show pattern icon is hidden. Pattern source line under the title still shows whatever the existing logic shows ("Classic Ribbed Hat · Ravelry") — no change there.
- **No yarn attached**: "Yarn" row in bottom sheet still shows, no count badge. Tapping opens add-yarn flow.
- **No photos**: "Photos" row shows count "0" or hides the count — match whatever convention the rest of the app uses for zero-count entries. Do not invent a new pattern.
- **No Repeating counter active**: compact pattern repeat row is not rendered. Counter and buttons sit in the same vertical position.
- **Multiple counters of various types**: counter screen shows only the main counter (large 99) and the optional compact Repeating row. All other counters are managed via the "Counters" entry in the bottom sheet, same as today.
- **Dark theme**: all changes must work in both light and dark themes. Use existing theme tokens — do not hardcode colors.

## Implementation guidance

- Read the current `CounterScreen.kt`, `SessionHistoryScreen.kt`, and any existing `ModalBottomSheet` usage in the codebase before writing code. Match existing patterns for bottom sheets, icon sizing, padding, and divider treatment.
- Do not introduce a new ViewModel. The bottom sheet items map to existing actions and existing destinations. If a wiring point doesn't already exist for one specific action, surface the gap rather than inventing a new state path.
- If during implementation you find that an item I described maps to multiple existing flows or no flow at all, **stop and report what you found** rather than picking one arbitrarily. Examples:
  - The "AI summary" entry — confirm it triggers the same code path as the current "View AI summary" link, not a different summary endpoint.
  - The "Yarn" entry — confirm where the current yarn card view/edit flow lives.
- Do not modify code outside the files listed in Scope unless required to wire up the new sheet. If a change is needed elsewhere (e.g. adding a callback parameter), explain why before doing it.
- Strings: add new strings to all locale files that currently exist (`values/`, `values-fi/`, `values-de/`, etc.). Provide English values; mark Finnish as `TODO_FINNISH_REVIEW` so I can verify Finnish terminology myself before merge.

## Confidence and reporting expectations

When implementing:
- Cite file path and approximate line for any existing code you rely on.
- If a UI behavior I described doesn't match current code (e.g. "Track stitches" toggle currently lives elsewhere or works differently than described), report that and ask before reconciling.
- It is acceptable and preferred to come back with "I couldn't find an existing pattern for X — here are the options I see" rather than guessing.

## Out of scope reminder

Do not change anything related to: ProjectListScreen, the bottom navigation, the wood-button counter graphic, the counter +1 / -1 / undo logic, voice commands, Glance widget, AI quotas, Pro gating, or any database migrations. The counter screen layout and the new bottom sheet are the only surfaces being changed.
