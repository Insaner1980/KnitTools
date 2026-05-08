# Row Counter Screen — Corrections

Fix these issues in `CounterScreen.kt`. Do NOT add, invent, or restructure anything beyond what is listed here.

## Remove

1. **Back arrow (←)** — Remove completely. Navigation to Project List is via the chevron (>) next to the project name. The back arrow creates a loop between Row Counter and Project List.
2. **"COUNTERS (1) + Add" section** — This was not requested. Remove it entirely. Pattern repeat should not be wrapped in a "Counters" section.
3. **"REMINDERS (0) + Add" section** — This feature does not exist in the app. Remove it entirely.
4. **Camera icon** next to the AI badge — Remove. Only the "AI" text badge and chevron (>) should be on that line.

## Fix

5. **Project name must be all-caps** — Apply `text.uppercase()` or equivalent to the project name. It should display as "MERINO BEANIE", not "Merino Beanie".
6. **Pattern repeat** — Restore to its original compact pill/chip form (the design that existed before this redesign). It should sit directly below the counter number, centered, with −/+ buttons inside the pill.
7. **Counter vertical centering** — The counter number (CURRENT ROW label + number + pattern repeat pill) should be vertically centered in the available space between the project identity section and the main buttons. Use `Modifier.weight(1f)` spacers above and below the counter group.

## Do NOT

- Add any new features, sections, or UI elements
- Change button sizes, colors, or layout (−/+/undo)
- Change the stats row or "Reset Counter"
- Change any ViewModel logic or data flow
