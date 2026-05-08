# Insights Streak Activity Grid — Delta Spec

**Style direction:** Material Design 3, KnitTools "70s Craft Revival" theme  
**Scope:** New composable added to InsightsScreen below the stat cards, above TIME PER PROJECT  
**Depends on:** Existing SessionEntity data (no new entities, no schema changes)  
**Pro gating:** Yes — free users see the grid placeholder with Pro prompt

---

## Overview

A compact activity grid showing the last 8 weeks (56 days) of knitting activity. Each day is a small square. The color intensity reflects how much the user knitted that day. This replaces the flat "TOP STREAK" number with something visual and motivating.

The TOP STREAK stat card remains as-is — the grid is an **additional** element, not a replacement.

---

## Layout

```
KNITTING ACTIVITY                    ← section header, same style as TIME PER PROJECT
Mon  ▪ ▪ ▪ ▪ ▪ ▪ ▪ ▪
Wed  ▪ ▪ ▪ ▪ ▪ ▪ ▪ ▪
Fri  ▪ ▪ ▪ ▪ ▪ ▪ ▪ ▪
Sun  ▪ ▪ ▪ ▪ ▪ ▪ ▪ ▪               ← 4 labeled rows, 7 hidden
     8w  7w  6w  5w  4w  3w  2w  now ← subtle week labels below
```

Specifics:

- Grid is 8 columns (weeks) x 7 rows (Mon–Sun, top to bottom)
- Only show day labels for Mon, Wed, Fri, Sun on the left — skip Tue, Thu, Sat labels (rows still exist, just unlabeled) to keep it compact
- Week labels below: show "8w" through "now" (or localized equivalent) for every other column to avoid clutter
- Each cell is a rounded square: `12.dp` size, `2.dp` gap, `RoundedCornerShape(3.dp)`
- The grid lives inside a Card with the same surface treatment as the TIME PER PROJECT card
- Rightmost column is the current week (today's day highlighted or current)
- Future days in the current week should be `outline` color (faint dotted/muted) to distinguish from "no activity"

---

## Color intensity levels

Use 4 intensity levels based on total knitting minutes that day, mapped to the app's accent palette:

| Level | Condition | Dark theme color | Light theme color |
|-------|-----------|-----------------|-------------------|
| 0 — none | 0 minutes | `surfaceVariant` (#3A3A2A) | `surfaceVariant` (#BBB59A) |
| 1 — light | 1–15 min | `tertiary` at 40% alpha | `tertiary` at 40% alpha |
| 2 — medium | 16–45 min | `tertiary` at 70% alpha | `tertiary` at 70% alpha |
| 3 — active | 46+ min | `tertiary` full (#C9A435 / #9A7B18) | `tertiary` full |

Using `tertiary` (mustard/gold) as the heat color because:
- `primary` (orange) is already the dominant app accent
- `secondary` (avocado) is used for section headers
- Mustard/gold gives a warm "glow" that feels like achievement without clashing

Future days (days after today in current week): use `MaterialTheme.knitToolsColors.onSurfaceMuted` at 30% alpha.

---

## Data source

Query session data grouped by date for the last 56 days:

- Source: `SessionDao` — aggregate `durationMinutes` per calendar date
- Use `LocalDate` for grouping (respect device timezone)
- The ViewModel should expose a `Map<LocalDate, Int>` (date → total minutes) or a pre-bucketed list
- If the project filter is active, only include sessions for the filtered project
- If no sessions exist at all, show the grid with all cells at level 0 (still looks like a grid, just empty)

Add to `InsightsViewModel`:
- A new `StateFlow<Map<LocalDate, Int>>` for daily activity
- Computed from the same session query filtered by the current project selection
- No new DAO method needed if sessions already have `startedAt` timestamp — just aggregate in the ViewModel or repository

---

## Entry animation

- Cells fade in with a diagonal stagger: top-left first, bottom-right last
- Use `animateFloatAsState` for alpha, from 0f to 1f
- Each cell delay = `(columnIndex + rowIndex) * 20ms`
- Total animation duration ~500ms for the full grid
- Base delay: start after stat card animations (~700ms from screen entry)
- Trigger on same filter key as other Insights animations

---

## Interaction

- No tap action on individual cells (keep it simple for v1)
- Long-press on any cell shows a tooltip: "Mon 7 Apr — 32 min" (date + minutes that day). Use the existing tooltip/popup pattern from the app.
- If the day has 0 minutes, tooltip says "Mon 7 Apr — No knitting"

---

## Pro gating

- Free users: show the section header "KNITTING ACTIVITY" and a blurred/placeholder version of the grid (all cells at level 0 with a centered overlay text "Upgrade to Pro" or a lock icon + "Pro" badge consistent with other Pro prompts in the app)
- Tapping the placeholder opens the Pro upgrade flow
- Pro users: full interactive grid

---

## Placement in InsightsScreen

Current order:
1. Project filter chip
2. Stat cards (2x2 grid)
3. TIME PER PROJECT section

New order:
1. Project filter chip
2. Stat cards (2x2 grid)
3. **KNITTING ACTIVITY section (new)**
4. TIME PER PROJECT section

---

## Localization

- Section header: add `insights_knitting_activity` to strings.xml (EN: "KNITTING ACTIVITY", FI: "NEULONTA-AKTIIVISUUS")
- Day labels: use `DayOfWeek.getDisplayName(TextStyle.SHORT, locale)` — automatically localizes to Mon/Ma/Mo etc.
- Week labels: "now" → localize ("nyt" in Finnish). Older weeks use "2w", "3w" etc. which are compact enough to be universal, but add to strings if needed.
- Tooltip date format: use `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)` for locale-aware dates

---

## What NOT to change

- Do not modify the TOP STREAK stat card — it stays as-is
- Do not add tap-to-navigate on cells (no drill-down for v1)
- Do not store activity grid data in Room — it's derived from existing sessions
- Do not change existing session tracking logic
- Do not add any new dependencies
