# Insights Streak Activity Grid — Fix Delta

**Scope:** Fix layout, sizing, and locale-awareness of the KNITTING ACTIVITY grid  
**Replaces:** The grid layout and label rules from the original streak grid spec  
**No new features** — this is a visual/layout correction only

---

## Problem summary

The current implementation renders the grid as a small cluster in one corner of the card. Day labels wrap to two lines ("Mo n", "We d"). Week columns are not distributed across the full width. The grid does not respect locale-based first day of week.

---

## 1. Full-width dynamic grid layout

The grid must fill the entire card width. Do not use fixed 12.dp cell sizes.

Layout calculation:

```
dayLabelWidth = 24.dp  (fixed, holds single-character labels)
availableWidth = cardInnerWidth - dayLabelWidth
cellSize = (availableWidth - (7 * cellGap)) / 8
cellGap = 3.dp
```

Use `Modifier.fillMaxWidth()` on the grid container. Calculate `cellSize` dynamically using `BoxWithConstraints` or `onSizeChanged` — the cells should be as large as the available space allows while maintaining square aspect ratio and fitting exactly 8 columns.

Cell shape: square with `RoundedCornerShape(3.dp)`, same as original spec.

---

## 2. Single-character day labels

Replace multi-character day abbreviations with single characters to prevent wrapping.

Use the first character of `DayOfWeek.getDisplayName(TextStyle.NARROW, locale)`. This gives locale-appropriate single characters automatically (M, T, W... in English; m, t, o... in Finnish; M, D, M... in German).

Show labels for rows 0, 2, 4, 6 only (every other row). Rows 1, 3, 5 get no label — the space is left blank but the row still renders.

Label style: `labelSmall`, color `onSurfaceMuted`, no letter spacing overrides.

---

## 3. Remove week labels

Remove the bottom row of week labels ("8w", "6w", "4w", "2w", "now") entirely. They add clutter and the grid is self-explanatory — rightmost column is the current week. This also simplifies the layout.

---

## 4. Locale-aware first day of week

The grid row order must respect the device locale:

```kotlin
val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
```

Build the 7-row sequence starting from `firstDayOfWeek`:

```kotlin
val dayOrder = (0 until 7).map { firstDayOfWeek.plus(it.toLong()) }
// Finland/Europe: [MONDAY, TUESDAY, ..., SUNDAY]
// USA: [SUNDAY, MONDAY, ..., SATURDAY]
```

Use `dayOrder` for:
- Row rendering order (row 0 = first day of locale week)
- Mapping session dates to the correct grid position
- Day label display

Column mapping: column 0 = 8 weeks ago, column 7 = current week. Within each column, determine the date for each row cell:

```kotlin
// Find the start of the current locale-week containing today
val today = LocalDate.now()
val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
val gridStartDate = currentWeekStart.minusWeeks(7) // 8 weeks total

// Cell date for column c, row r:
val cellDate = gridStartDate.plusWeeks(c.toLong()).plusDays(r.toLong())
```

Future dates (cellDate > today): render with `onSurfaceMuted` at 30% alpha as originally specified.

---

## 5. Card container

The grid card should use the same card style as TIME PER PROJECT:
- `MaterialTheme.colorScheme.surfaceVariant` background (or whatever the TIME PER PROJECT card uses)
- Same corner radius and padding
- Internal padding: `16.dp` horizontal, `12.dp` vertical
- The grid itself has no extra internal padding beyond the cell gaps

---

## 6. Unchanged from original spec

Everything not mentioned above stays as originally specified:
- Color intensity levels (4 levels based on minutes, using tertiary/mustard)
- Entry animation (diagonal stagger)
- Long-press tooltip per cell
- Pro gating behavior
- Data source (aggregate sessions by date)
- Section header style and placement
- Localized section header string

---

## Verification checklist

After implementation, verify:
- [ ] Grid fills the full card width on different screen sizes
- [ ] All 8 columns x 7 rows are visible
- [ ] Day labels are single characters, no wrapping
- [ ] No week labels below the grid
- [ ] On a Finnish locale device, week starts Monday
- [ ] On a US locale device, week starts Sunday
- [ ] Future days in current week are visually distinct (muted)
- [ ] Today's cell is in the correct position (rightmost column, correct row)
