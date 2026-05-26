Task: Redesign only the KnitTools row counter screen.

Scope:
Only modify the row counter screen and its directly used row-counter workspace components.
Do not redesign Pattern Viewer, Library, Tools, Insights, Settings, widgets, project list, yarn detail, notes editor, or global navigation behavior except where the counter screen needs to keep the existing bottom navigation visible.

Relevant files to inspect first:
- app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt
- app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterProjectContentCards.kt
- app/src/main/java/com/finnvek/knittools/ui/components/ProjectCard.kt
- app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt
- app/src/main/java/com/finnvek/knittools/ui/navigation/KnitToolsBottomBar.kt

Product goal:
The first visible screen of the row counter must be a calm, large, easy-to-use counter.
The user should be able to open the project and immediately count rows without seeing or accidentally tapping project cards.
Project cards must start only after the user scrolls down.

Keep:
- Existing KnitTools dark/light & warm visual identity.
- Existing big uppercase project header style.
- Existing bottom navigation visible on the counter screen.
- Existing tabs: Projects, Library, Tools, Insights, Settings.
- Existing row counter logic, project selection logic, reminders logic, yarn logic, notes logic, photos logic, and extra counter logic.
- Existing Pro gating behavior.

Do not:
- Do not hide the bottom navigation on the counter route.
- Do not add a sticky mini row counter to other tabs.
- Do not add a floating row counter at the bottom of Library, Tools, Insights, or Settings.
- Do not show project cards in the first viewport of the counter screen.
- Do not show “Pattern attached” under the project title.
- Do not show the attached PDF filename under the project title.
- Do not show a Next Reminder card directly under the main counter controls.
- Do not show notes preview text in row counter project cards.
- Do not show yarn names in row counter project cards.
- Do not show photo counts in row counter project cards.
- Do not show reminder row text in row counter project cards.
- Do not use middle-dot separators in UI copy, for example do not use “Row 74 · Try on before cuff”.
- Do not reintroduce old CounterQuickActions or CounterProjectInfo models if they have been removed.

First viewport layout:
The initial visible area of the counter screen should contain only:

1. Header
2. Main counter hero
3. Bottom navigation

No project cards should be visible before scrolling.

Header:
Keep the current style:
- Back arrow
- Large uppercase project name
- Overflow menu

Do not add a subtitle under the project name.
Remove/hide the current “Pattern attached” line from this screen header.
Pattern state should be represented later in the scrolled project card grid, not in the header.

Main counter hero:
The row number and the large plus button must be visually centered in the available space between the header and bottom navigation.

The hero should include:
- Optional compact repeat/section control only if active
- Row label, for example “Row 67 / 88”
- Very large current row number
- Progress bar
- Main control row:
  - minus button
  - large wooden plus button
  - undo button

The large wooden plus button must be the dominant control.
Make its visual size and touch target larger than the minus and undo buttons.
The minus and undo buttons should still have comfortable touch targets.

The counter should not feel squeezed against project content.
Use vertical spacing so the number and plus button feel central and intentional.

Repeat / section controls:
If repeat or section controls are active, keep them compact.
They must not push the main row number and plus button away from the center.
If needed, make the repeat control smaller or move secondary counter details below the first viewport.

Bottom navigation:
Keep the bottom navigation visible on the counter screen.
The user must be able to quickly switch to Library, Tools, Insights, and Settings.
Do not add counter-specific floating controls above the bottom navigation.

Scrollable content after first viewport:
After the user scrolls down, show the project content section.

Use this order:

Project
- square card grid

Extra Counters
- only if extra counters exist

Other existing lower sections/actions can remain lower down or in the overflow menu, but they must not appear in the first viewport.

Project card grid:
Replace the current wide information-style project cards on the counter screen with same-sized square action cards.

Cards:
- Open Pattern or Add Pattern
- Yarn
- Notes
- Photos
- Reminders

Important:
Each card must contain only:
- icon
- title

No subtitles.
No secondary text.
No previews.
No raw PDF filename.
No yarn name.
No notes excerpt.
No photo count.
No reminder row.
No chevron unless the existing design absolutely requires it; prefer no chevron for these square action cards.

Examples of correct card text:
- Open Pattern
- Add Pattern
- Yarn
- Notes
- Photos
- Reminders

Examples of text that must not appear inside these cards:
- two_sleeves_one_promise.pdf
- Isager Highland Wool
- First sleeve is moving again...
- 1 photo
- Row 74
- Try on before cuff
- Pattern attached

Pattern card behavior:
If the project has an attached pattern PDF or linked pattern, the card title should be “Open Pattern”.
If the project has no pattern, the card title should be “Add Pattern”.
Do not show “Pattern attached” anywhere in the first viewport.

Yarn card behavior:
The card title should always be “Yarn”.
Do not show linked yarn names or project yarn note names inside the card.
Tapping the card should keep the current yarn management behavior.

Notes card behavior:
The card title should always be “Notes”.
Do not show the first note line inside the card.
Tapping the card should keep the current notes behavior.

Photos card behavior:
The card title should always be “Photos”.
Do not show photo count inside the card.
Tapping the card should keep the current photo behavior.

Reminders card behavior:
The card title should always be “Reminders”.
Do not show the nearest reminder row or reminder text inside the card.
Tapping the card should open the reminder management/list behavior currently available from the project actions or content card flow.
Do not show a next reminder card near the plus button.

Extra counters:
Extra counters can appear below the Project card grid.
They must not be visible in the first viewport unless there is genuinely enough space after the full main counter hero and bottom navigation, which is unlikely.
Do not place extra counter controls near the main plus button in a way that creates accidental taps.

Spacing:
Prioritize large touch targets and breathing room over showing more content.
The first viewport should feel intentionally sparse.
The counter is the main tool; project cards are secondary navigation.

Typography:
Use existing KnitTools typography tokens where possible.
Keep the large uppercase project title style.
Keep the main row number large.
Do not reduce text sizes to fit more content into the first viewport.
If content does not fit, it should move below the fold.

Copy rules:
Use plain labels.
Avoid decorative separators.
Do not use middle dots between phrases.
Prefer separate lines or simple labels.

Testing / verification:
After implementing, run at least:
- ./gradlew assembleDebug
- ./gradlew test
- ./gradlew :app:detekt
- ./gradlew lint

Manual UI checks:
1. Open an active project counter.
2. Confirm the first viewport shows the uppercase header, main row counter, large plus button, and bottom navigation.
3. Confirm no project cards are visible in the first viewport.
4. Confirm there is no “Pattern attached” text under the header.
5. Confirm bottom navigation remains visible on the counter screen.
6. Scroll down and confirm the Project section appears as square same-sized cards.
7. Confirm cards contain only icon + title.
8. Confirm Open Pattern / Add Pattern, Yarn, Notes, Photos, and Reminders all still open the correct flows.
9. Confirm no sticky or floating row counter appears in other tabs.
10. Confirm no UI copy uses middle-dot separators like “Row 74 · Try on before cuff”.