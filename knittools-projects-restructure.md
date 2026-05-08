# Projects Tab Restructure: List as Root Screen

## What to change

The Projects tab currently opens the Row Counter (CounterScreen) as its root. Change it so the **Project List (ProjectListScreen) is the root** of the Projects tab. The Row Counter becomes a detail screen you navigate to from the list.

## New navigation flow

```
Projects tab root → ProjectListScreen (list of all projects)
                       → tap project card → CounterScreen (row counter for that project)
                       → back arrow → ProjectListScreen
                       → tap "Continue Knitting" card → CounterScreen (last active project)
                       → FAB "+" → create new project → CounterScreen
```

## Changes to ProjectListScreen

### Add "Continue Knitting" card at the top
- Shown only when there is at least one project with rows > 0
- Shows the most recently updated active project
- Contains: project name (large, bold), current row count, total time
- Right side: a circular play/arrow button with Primary gradient background
- Tapping it navigates to CounterScreen with that project loaded
- Background: subtle Primary color tint (Primary at ~10% opacity with 1px Primary border at ~25% opacity)
- Label above project name: "CONTINUE KNITTING" in labelSmall, Primary color, uppercase

### Enrich project cards with more info
Each project card in the ACTIVE section should show:
- Project name (bold)
- Row count (Primary color, bold) + last active time (TextMuted)
- If yarn is linked: small color circle + yarn name
- If progress photos exist: camera icon + photo count
- Chevron on the right

### Remove the back arrow
ProjectListScreen is now a root screen, not a detail screen. Remove the back arrow from the top bar. Show "Projects" as the screen title instead.

## Changes to CounterScreen

### Add back arrow
CounterScreen is now a detail screen. Add a back arrow at the top left that navigates back to ProjectListScreen.

### Remove project navigation chevron
The current chevron (>) next to the project name that navigates to ProjectListScreen is no longer needed since users come FROM the list. Remove it.

### Keep everything else
The counter, buttons, stats, pattern repeat, yarn link, notes, photos, AI summary — all stay exactly as they are.

## Changes to navigation (Screen.kt / KnitToolsNavHost)

- Change `TopLevelDestination.Projects` startDestination from `Screen.Counter.route` to `Screen.ProjectList.route`
- CounterScreen should receive the project ID as a navigation argument
- Ensure CounterViewModel is still scoped to the Projects nav graph so state is shared correctly
- The "Continue Knitting" card and project card taps should both navigate to `Screen.Counter.route` with the project ID

## Changes to Session History
- Session History should be accessible from CounterScreen (tap on time stat), same as now
- Back from Session History returns to CounterScreen, not ProjectListScreen

## What NOT to change
- Tools tab and Reference tab — no changes
- Room database, entities, DAOs
- Counter logic, session tracking, billing
- Any screen other than ProjectListScreen and CounterScreen
- The actual Row Counter UI (buttons, number, stats) stays identical
