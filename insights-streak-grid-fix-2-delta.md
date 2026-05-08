# Insights Streak Grid — Sizing & Visual Fix Delta

**Scope:** Fix oversized cells, improve empty-state appearance, add today indicator  
**No new features or data changes**

---

## 1. Constrain cell size

Cells are currently too large because they divide the full card width equally. Fix:

```kotlin
val maxCellSize = 28.dp
val cellGap = 3.dp
val dayLabelWidth = 20.dp

// Calculate available width inside card padding
val naturalCellSize = (availableWidth - dayLabelWidth - (7 * cellGap)) / 8

// Use the smaller of natural and max
val cellSize = minOf(naturalCellSize, maxCellSize)
```

Center the entire grid horizontally within the card if the constrained grid is narrower than the card. Use `Arrangement.Center` or a centered `Row`/`Box`.

This makes the grid compact and proportional regardless of screen width.

---

## 2. Empty cells: outline instead of solid fill

Currently empty cells (level 0, no activity) use a solid `surfaceVariant` fill. This makes the grid look like a flat blob.

Change level 0 cells to:
- **Background:** `Color.Transparent`
- **Border:** `1.dp` border using `MaterialTheme.knitToolsColors.onSurfaceMuted` at **20% alpha**
- Same `RoundedCornerShape(3.dp)`

This creates a visible grid structure even when empty while making active days pop visually against the hollow cells.

Future-day cells (dates after today in current week) keep the same treatment as empty cells — transparent with the faint outline border. No need to distinguish them separately since the today indicator (below) makes the boundary clear.

---

## 3. Today indicator

The cell for today's date gets an additional accent border to anchor the user's eye:

- **Border:** `2.dp` border using `MaterialTheme.colorScheme.primary` (burnt orange)
- This is applied **on top of** whatever fill the cell has (could be empty outline, could be an active-day color fill)
- If today has activity, the cell shows its normal intensity fill + the orange border
- If today has no activity, the cell shows transparent background + the orange border (no additional outline)

This is the only cell with this treatment. Do not add indicators for yesterday, streak start, etc.

---

## 4. Card padding adjustment

Reduce the card's internal vertical padding to make it feel tighter:

- Horizontal padding: `16.dp` (unchanged)
- Vertical padding: `12.dp` top, `12.dp` bottom (reduce if currently larger)

The grid should feel like a compact badge, not a dominant section.

---

## Summary of cell rendering rules

| State | Background | Border |
|-------|-----------|--------|
| No activity (level 0) | Transparent | 1dp `onSurfaceMuted` 20% alpha |
| Light activity (1-15 min) | `tertiary` 40% alpha | None |
| Medium activity (16-45 min) | `tertiary` 70% alpha | None |
| Active (46+ min) | `tertiary` 100% | None |
| Future day | Transparent | 1dp `onSurfaceMuted` 20% alpha |
| Today (any level) | Normal fill per level | 2dp `primary` (burnt orange) |

---

## Unchanged

- Grid dimensions: 8 columns x 7 rows
- Locale-aware first day of week
- Single-character day labels on every other row
- No week labels
- Entry animation (diagonal stagger)
- Long-press tooltip
- Pro gating
- Color intensity thresholds
- Section header and placement
