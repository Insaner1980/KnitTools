# Insights Overhaul — Delta Spec

**Style direction:** Material Design 3, KnitTools "70s Craft Revival" theme  
**Scope:** Visual and structural improvements to InsightsScreen  
**Schema changes:** None  
**New data queries:** One new filtered aggregation in ViewModel  
**Risk level:** Medium — layout restructure + new filter logic, but no schema or navigation changes

---

## 1. Stat cards: remove TOP STREAK, switch to 3-column single row

### Why

TOP STREAK becomes redundant once streak info moves into the activity grid card (section 2). Three cards in a single row is tighter and leaves more room for actual content.

### Changes

Remove the TOP STREAK stat card entirely.

Remaining cards: **TOTAL TIME**, **AVG PACE**, **COMPLETED**

Layout: single row, 3 equal-width cards with `8.dp` horizontal gap between them. Each card uses `Modifier.weight(1f)` inside a `Row`.

Accent label colors stay as previously specified:
- TOTAL TIME → `primary` (burnt orange)
- AVG PACE → `secondary` (avocado)
- COMPLETED → `tertiary` (mustard)

Value text sizing: if the current size is too large for three columns (text overflow on smaller screens), reduce to `headlineSmall` or `titleLarge`. Test on a 360dp-wide screen to verify nothing clips.

Number count-up animations remain unchanged, just remove the fourth card's animation and its stagger slot.

---

## 2. Streak grid card: add streak stats inside

### Why

The grid currently sits as a visual element without any textual context. Adding streak numbers makes it an actual insight, not decoration.

### Changes

Add a row below the grid cells, inside the same card, with two streak stats:

```
[grid cells]

Current streak: 3 days        Best: 9 days
```

Layout:
- `Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween)`
- Left: "Current streak: X days" (or "Current streak: X d" if space is tight)
- Right: "Best: X days"
- Top padding from grid: `8.dp`
- Text style: `labelSmall`
- Label part ("Current streak:", "Best:") in `onSurfaceVariant` color
- Value part ("3 days", "9 days") in `onSurface` color, `fontWeight = Medium`

Data source:
- Current streak: count consecutive days backward from today (or yesterday if today has no sessions yet) where the user has at least one session. Already calculable from the daily activity map that powers the grid.
- Best streak: the longest consecutive run in all session history. This is already calculated for the TOP STREAK card — reuse that logic.

Both values respect the active project filter and time range filter.

Localization:
- Add strings: `insights_current_streak` (EN: "Current streak:", FI: "Nykyinen putki:")
- Add strings: `insights_best_streak` (EN: "Best:", FI: "Paras:")
- Day label: reuse existing pluralized day string if available, otherwise add `insights_streak_days` (EN: "%d days", FI: "%d päivää") with singular form for 1

---

## 3. Time range filter

### Why

All stats currently show all-time data. A time range filter makes Insights immediately more useful — "how was my week" vs "how was my month" are different questions.

### Changes

Add a second row of filter chips below the project filter chip:

```
[All Projects ▼]
[All Time] [This Week] [This Month]
```

Implementation:
- Three `FilterChip` components in a `Row` with `8.dp` gap
- Use the same chip style as the project filter chip (outlined, selected = filled)
- Default selection: `All Time`
- State: add `timeRange: TimeRange` enum to ViewModel (`ALL_TIME`, `THIS_WEEK`, `THIS_MONTH`)

Filtering logic:
- `THIS_WEEK`: sessions where `startedAt >= start of current locale week` (use `WeekFields.of(locale).firstDayOfWeek` for consistency with the grid)
- `THIS_MONTH`: sessions where `startedAt >= first day of current month`
- `ALL_TIME`: no date filter

What the filter affects:
- All three stat cards (total time, avg pace, completed count)
- Streak grid highlights (only show activity within the time range)
- Streak stats below grid (current streak and best streak within range)
- Time Per Project list (only show projects with activity in range, ordered by time descending)

What it does NOT affect:
- The project filter chip — both filters work together (project AND time range)
- Navigation or any other screen

The streak grid always renders all 8 weeks of cells for visual context, but only colors cells that fall within the selected time range. Cells outside the range render as empty (outline style). This means "This Week" still shows the full 8-week grid structure but only the rightmost column has colored cells.

ViewModel changes:
- Add `timeRange` StateFlow
- Existing session aggregation queries get an additional date filter parameter
- Keep the aggregation in the ViewModel/repository layer, not in the DAO (avoid adding new DAO methods if possible — filter in Kotlin from the existing flows)

Localization:
- `insights_all_time` (EN: "All Time", FI: "Kaikki")
- `insights_this_week` (EN: "This Week", FI: "Tämä viikko")
- `insights_this_month` (EN: "This Month", FI: "Tämä kuukausi")

Animations: when the time range changes, trigger the same count-up and bar entry animations as on initial load (rekey on `timeRange` + `projectFilter`).

---

## 4. Time Per Project visual fixes

### 4a. Bar background track

Add a full-width background track behind each progress bar:

- Track color: `MaterialTheme.colorScheme.surfaceVariant` (or `onSurface` at 8% alpha — whichever provides better contrast against the card background)
- Track height: same as the progress bar
- Track corner shape: same `RoundedCornerShape(4.dp)` as the bar
- The colored bar draws on top of the track

This gives every bar a visual baseline and makes proportions immediately readable.

### 4b. Minimum bar width

Bars representing very small values currently render as tiny dots. Set a minimum visible width:

```kotlin
val barFraction = maxOf(data.fraction, 0.03f) // at least 3% of track width
```

This ensures even the smallest value shows a recognizable bar segment.

### 4c. Stats text: neutral color

The hours and rows text below each bar (e.g., "0.8h · 13 rows") currently uses the project's accent color. Change to a neutral color:

- Use `MaterialTheme.colorScheme.onSurfaceVariant`
- Same text style as current, just different color
- The project's color identity is carried by the bar itself — the metadata doesn't need to repeat it

### 4d. Date placement

Move the date label from the right edge of the project name row to inline with the project name:

Current:
```
Project 4                              today
[bar]
0.8h · 13 rows
```

New:
```
Project 4 · today
[bar]
0.8h · 13 rows
```

Implementation:
- Project name and date in the same `Row` or as a single `Text` with `buildAnnotatedString`
- Date part uses `onSurfaceVariant` color and normal weight
- Separator: ` · ` (middle dot with spaces)
- Project name keeps current style (`onSurface`, medium weight)

If the combined text would overflow (long project name), truncate the project name with ellipsis, keeping the date always visible. Use `Modifier.weight(1f)` on the name and fixed width for ` · today`.

### 4e. Zero-time formatting

Replace "0.0h" with a more meaningful display:

```kotlin
when {
    totalMinutes == 0 -> "—"           // em dash, no time recorded at all
    totalMinutes < 6  -> "<0.1h"       // less than 6 minutes rounds to 0.0
    else              -> formatHours() // normal formatting
}
```

This avoids the "data error" appearance of "0.0h".

### 4f. Row count display consistency

If a project has rows but shows `—` for time, still display rows: `— · 6 rows`

If a project has 0 rows and 0 time: show only `—` (no "0 rows").

---

## 5. Empty state / motivational footer

### Why

When there's little data or the user scrolls past all content, the screen ends abruptly. A small footer message makes it feel intentional.

### Changes

Below the Time Per Project card (or below the streak grid if no projects have time), add a centered text:

- Text: `insights_footer_message`
  - EN: "Every row counts — keep knitting!"
  - FI: "Jokainen kerros lasketaan — jatka neulomista!"
- Style: `bodySmall`, `onSurfaceVariant` color, centered
- Top padding: `24.dp`
- Bottom padding: `16.dp`

Show this only when there is at least some session data. If Insights is completely empty (no sessions at all), the existing empty state handling takes precedence — do not show this footer alongside a full empty state.

---

## Implementation order

1. **Stat cards → 3 columns** (simple layout change, low risk)
2. **Time Per Project visual fixes** (4a–4f, isolated to one composable)
3. **Streak stats inside grid card** (moves existing logic, removes TOP STREAK card)
4. **Time range filter** (new filter state + refiltering, highest complexity)
5. **Footer message** (trivial, do last)

---

## What NOT to change

- Do not change the overall screen background color or card surface colors (these are app-wide theme decisions)
- Do not change project color assignment logic
- Do not change corner radius of cards
- Do not add drill-down navigation from Time Per Project rows
- Do not change the streak grid cell colors, sizing, or animation (already specced and implemented)
- Do not add a "less → more" legend to the streak grid
- Do not change any Room entities or create new database tables
- Do not change the project filter chip design
