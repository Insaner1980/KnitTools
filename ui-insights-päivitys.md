# Insights UI Update — Detailed Plan (A: Premium Polish + B: Stitch Fabric)

Status: **draft for review** — no code changes yet.
Scope: `app/src/main/java/com/finnvek/knittools/ui/screens/insights/`, `ui/theme/InsightsDimens.kt`, `res/values*/strings.xml`.
Goal: make the Insights tab clear, premium and memorable ("wow") while keeping the 70s Craft Revival identity: editorial typography, hairlines instead of cards, theme tokens only, light + dark.

---

## 0. Current state (baseline)

Section order today: filters → range kicker → hero → strong rule → stats row → trend line → "Every day" (bar chart) → "Where the time went" (project list) → footer note.

Known problems, confirmed from screenshots (FI/EN, light/dark):

1. **Selection band dominates the chart.** `ChartPlot` draws a full-height `selectionBandColor` rect behind the selected bucket (`InsightsChart.kt`, `drawRect(selectionBandColor …)`). With sparse All Time data (2 month buckets) the band is a huge empty rectangle that reads as a rendering glitch.
2. **All Time with few months is 90 % empty space** — two skinny bars pinned to the far edges of the plot.
3. **"0 DAY STREAK" is the first stat many users read.** Negative framing; the best streak is relegated to a muted trend line.
4. **Filter chips look like stock Material**, take two rows of prime space, and have no relationship to the app's identity.
5. **Project share is a bare percentage** — no glanceable visual of proportion.
6. **Nothing moves.** No entrance or transition animation anywhere; premium feel needs restrained motion.

Non-goals (explicitly out of scope):

- No recap/"Year in Yarn" share feature (direction C — later).
- No changes to Pro gating logic, ViewModel data loading, session metrics math, or Room.
- No changes to `Type.kt` roles or global theme tokens; everything new lives in `InsightsDimens` and strings.
- No new dependencies.

---

## A. Premium polish

### A1. Chart selection: highlight, don't paint a band

**Decision (approved in discussion): remove the selection band entirely; selection is expressed by dimming unselected bars.** No dots, no decorative separators anywhere.

Changes in `InsightsChart.kt` / `ChartPlot`:

- Delete the `selectionBandColor` rect draw and the `ChartSelectionBandAlpha` token.
- When `selectedIndex != null`, draw unselected bars with their stack colors multiplied by a new alpha token; the selected bar keeps full color. When `selectedIndex == null` (only possible with an empty chart), all bars are full.
- **Keep the existing 3 dp baseline marker** (`ChartSelectionMarkerHeight`). It is load-bearing: the selected bucket can have 0 minutes (no bar to dim/highlight), and on month view bars are 10 dp wide — the marker is the only unambiguous anchor. It is a marker under the axis, not a decoration.
- Animate the dim transition with **one** `Animatable<Float>` progress restarted on selection change (per-bar `animateFloatAsState` for 31 buckets is wasteful); inside the Canvas pass, lerp each unselected bar's alpha from full toward `ChartUnselectedBarAlpha` by that progress. Duration ~180 ms, no overshoot.

New tokens in `InsightsDimens`:

```kotlin
@Suppress("MayBeConstant") val ChartUnselectedBarAlpha = 0.45f   // tune visually 0.4–0.6
```

Removed tokens: `ChartSelectionBandAlpha`.

Edge cases:

- Selected bucket with `totalMinutes == 0`: no bar; baseline marker alone shows selection (unchanged behavior).
- Screen-reader path (`CustomAccessibilityAction` next/previous) goes through the same `selectBucket`, so dimming follows automatically.

### A2. Sparse All Time chart → superseded by B

The month-bucket bar chart for All Time is replaced by the stitch fabric (see B). Week and month ranges keep the current day-bar chart, where density is never a problem (7–31 buckets). **Therefore no "center sparse bars" work is done** — YAGNI.

### A3. Filters: one segmented control, 70s style

Replace the three separate range chips with a single **full-width segmented control**: one pill-shaped track (1 dp `outlineVariant` border, `FilterChipShape` = 50 % rounding), three equal-width segments, and a **sliding `primary`-colored pill indicator** behind the selected label.

- Selected label: `onPrimary`; unselected: `onSurfaceVariant`; typography unchanged (`labelLarge`).
- Indicator slides with `animateDpAsState`/`animateFloatAsState` on the segment's offset fraction, ~220 ms, standard easing. This one moving element is the "premium tell" of the whole filter area.
- Accessibility identical to today: container `selectableGroup()`, each segment `selectable(selected, Role.Tab)`. Min touch target 48 dp stays (`FilterChipMinTouchTarget`).
- Long labels (localization): each segment `maxLines = 1` + ellipsis; the track is full width so segments get ⅓ each — verify FI ("Tämä kuukausi") at fontScale 2.0; if it ellipsizes, allow the row to fall back to the current chip layout above a `fontScale` threshold (same pattern as `ChartReadoutStackFontScale`).
- The project filter keeps its current single-chip + dropdown design (it already carries the yarn-color dot identity), but its top margin tightens so the whole filter block is visually one unit.

Implementation: new private composable `InsightsRangeSelector` in `InsightsScreen.kt` replacing `InsightsRangeChip` (delete the old composable — no dead code). Track/indicator measured with `onSizeChanged` or `BoxWithConstraints`; no SubcomposeLayout needed for 3 fixed segments.

New tokens: `RangeSelectorHeight = 44.dp`, `RangeSelectorIndicatorPadding = 3.dp`, `RangeSelectorFallbackFontScale = 1.3f` (reuse value of `ChartReadoutStackFontScale` if identical — one source of truth: extract a shared `FilterStackFontScale` if both end up equal).

### A4. Streak: positive framing

`InsightsStatsRow` third stat (only when `canUseStreak`):

| Condition | Value shown | Label |
|---|---|---|
| `currentStreak > 0` | `currentStreak` | "day streak" (existing `insights_stat_day_streak`) |
| `currentStreak == 0 && bestStreak > 0` | `bestStreak` | **new** `insights_stat_best_streak` ("best streak" / "paras putki") |
| both 0 | `0` | "day streak" (new user; nothing to celebrate yet) |

Trend line (`trendLineText`, All Time branch) currently prints "Best streak N days". To avoid duplication: **show the best-streak trend line only when the stat column is showing the current streak** (`currentStreak > 0`). When the stat already shows best streak, the All Time trend line returns null.

No math changes; `bestStreak`/`currentStreak` already exist in `InsightsUiState`.

Strings: add `insights_stat_best_streak` to `values/strings.xml` (EN) + `values-fi` (+ other locales per normal localization pass; missing locales fall back to EN).

### A5. Project rows: share bar

In `InsightsProjectRow`, under the name + sub-line column (the `weight(1f)` column), add a thin horizontal **share bar**: width = `project.totalMinutes / rangeTotalMinutes` fraction of the column width, height 3 dp, fully rounded, color `yarnColorForId(projectId)` (same source as the dot and chart — no new color logic). The percentage text on the right stays; the bar makes it glanceable, the number makes it precise.

- Only drawn when `rangeTotalMinutes > 0` (same guard as the percent text).
- Minimum visible width 3 dp when fraction > 0 so a 1 % project doesn't vanish.
- Entrance: bar grows from 0 to its fraction on first composition of the row, ~350 ms, `FastOutSlowIn`; animate with `animateFloatAsState` keyed on the fraction so range/filter changes retarget smoothly.
- Implementation: a `Box` with `fillMaxWidth(fraction)` inside a fixed-height container — no Canvas needed.

New tokens: `ProjectShareBarHeight = 3.dp`, `ProjectShareBarTopMargin = 8.dp`, `ProjectShareBarCorner = 1.5.dp`.

### A6. Motion: hero count-up + bar growth

Restrained, fast, standard easing — motion says "crafted", never "playful dashboard".

1. **Hero count-up.** `DurationHero` is a shared component — do **not** modify it. In `InsightsHero`, animate the minutes value at the call site: `animateIntAsState(targetValue = state.totalMinutes, tween(500, easing = FastOutSlowIn))` and feed `DurationDisplayFormatter.fromMinutes(animatedMinutes)` into `DurationHero`. Runs on first data load and on range/filter change (retarget from current shown value — feels continuous, not a reset).
2. **Chart bar growth.** In `ChartPlot`, one `Animatable<Float>` progress 0→1, restarted when `buckets` identity changes (range/filter change). Bar height = `progress * computedHeight`. Duration ~400 ms. The stacked segments scale with the bar automatically since they're drawn inside the clipped silhouette.
3. **Selection dim crossfade** — covered in A1.
4. **Segmented-control slide** — covered in A3.

No entrance animation for list rows beyond the share bar (LazyColumn item animations would fight scroll performance for little gain).

Accessibility/motion sensitivity: all animations are decorative and short (<500 ms); values land exactly on final state. No parallax, no looping motion.

### A7. Micro-copy (optional, low priority)

`insights_chart_max_format` FI "enint. %s" is bureaucratic next to the otherwise warm copy. Candidate: "huippu %s" (EN "peak %s"). Decide during implementation with the translator hat on; not blocking.

---

## B. Stitch fabric — the signature All Time visual

### B1. Concept

For **All Time**, the month-bucket bar chart (the weakest current visual) is replaced by a **knitted fabric calendar**: a grid where **each day is one stitch** — a small V-shaped stockinette glyph — colored by the day's dominant project yarn color, with ink intensity encoding how much time was worked. Weeks are columns (most recent on the right), weekdays are rows (locale-aware first day of week). Empty days are faint un-knit bumps at hairline alpha, so the fabric shows its "cast-on" texture rather than holes.

The metaphor is exact: *your time knits a fabric.* It reuses the existing color language (`yarnColorForId`), needs zero new theme colors, and no competitor screen looks like it.

Week and month ranges keep the existing day-bar chart — at 7–31 buckets it is the better quantitative display, and the fabric needs multiple weeks to look like fabric.

### B2. Placement and gating

- Lives exactly where the chart lives today: under the "Every day" section header, only when `timeRange == ALL_TIME`.
- Same Pro gate as the chart (`isPro` / `ProFeature.INSIGHTS_CHARTS`): non-Pro users see the existing `InsightsProChartCard`, unchanged, exactly once. No second upsell surface.
- The `ChartReadout` row above the visual is reused as-is (day label + duration + rows, live region) — the fabric is a different plot under the same readout contract.

### B3. Data model (pure, unit-testable)

New file `InsightsFabricModel.kt` (same package, alongside `InsightsChartModel.kt`):

```kotlin
/** One day of the fabric: dominant project decides the yarn color, level the ink. */
data class InsightsFabricDay(
    val date: LocalDate,
    val totalMinutes: Int,
    val dominantProjectId: Long?,   // null when totalMinutes == 0
    val level: Int,                 // 0 = empty, 1..3 = intensity
)

data class InsightsFabricModel(
    val days: List<InsightsFabricDay>, // continuous, oldest first, ends today
    val firstDayOfWeek: DayOfWeek,
    val weekCount: Int,
)
```

Builder `buildInsightsFabric(sessions, projectFilter…, today, zoneRules, firstDayOfWeek, maxWeeks)`:

- Daily aggregation reuses the same per-day bucketing rules as the chart: `SessionMetrics.paceBuckets(…, DAY, …)` per project (respecting `analyticsZoneOr`, cross-midnight splitting), then per day: dominant project = max seconds (tie → smaller projectId, deterministic), total minutes via the existing display-minute rules.
- Window: from `max(firstSessionDate-week, today - maxWeeks)` aligned to the locale week start, through today. `maxWeeks = 26` (half a year) — enough to be impressive, bounded for perf and layout. Constant `FABRIC_MAX_WEEKS = 26` next to `ALL_TIME_MONTH_BUCKET_LIMIT`.
- Intensity levels relative to the window's max daily minutes: `0` empty; `1` ≤ ⅓ of max; `2` ≤ ⅔; `3` above. (Relative, not absolute — a 20 min/day knitter gets a full-ink fabric too.)
- Project filter applies exactly like everywhere else (sessions are already filtered upstream by `selectedProjectId`).

ViewModel: `InsightsUiState` gets `val fabric: InsightsFabricModel? = null`, built in `buildUiState` only when `timeRange == ALL_TIME && featureGates.canUseCharts`. Everything else untouched.

### B4. Rendering (`InsightsFabric.kt`, Canvas)

- **Stitch glyph:** two short diagonal strokes with round caps forming a V (stockinette knit stitch). Stroke width ≈ cell/4.5. Level 1/2/3 → color alpha 0.45 / 0.7 / 1.0 of `yarnColorForId(dominantProjectId)`. Level 0 → the same V at `RuleHairlineAlpha` in `onSurface` (the un-knit texture).
- **Grid:** 7 rows (weekday, top = locale first day of week), N columns (weeks, chronological, newest right). Cell size = available width / weekCount, clamped to `FabricMinCellSize = 11.dp`; if the clamp is hit, drop the oldest weeks to fit (the model's `maxWeeks` is the upper bound, width is the lower). Fabric height = 7 × cell + labels — roughly the current `ChartPlotHeight` at typical widths, so the screen rhythm holds.
- **Month labels:** under the grid, at each column that contains the 1st of a month, `MMM` in the existing axis-label style (`ChartAxisLabelFontSize`, `onSurfaceMuted`). Reuses `axisLabel`-style formatting; max ~6 labels (`MONTH_AXIS_MAX_LABELS` logic).
- **Selection:** tap or horizontal drag selects a day (haptic on change, same as chart). Selected cell gets a 1.5 dp `onSurface` rounded-rect outline; the readout above updates. Selection state lives in the same `selectedBucketIndex` slot as the chart (index into `fabric.days`), reset on range/filter change (already handled by the existing `LaunchedEffect`).
- **A11y:** same contract as the chart — `clearAndSetSemantics` with one summary description (new string, e.g. "Fabric of your last %1$d weeks: %2$s active days, longest day %3$s") + `CustomAccessibilityAction` next/previous day driving the shared `moveChartSelection`-equivalent (extract or generalize `moveChartSelection` to work over any bucket count — it already only needs size + index; **reuse it, don't duplicate**).
- **Entrance animation — the wow moment:** on first composition per range entry, stitches appear in a left→right sweep (column-major order, oldest week first — a calendar reads chronologically even though real fabric grows bottom-up). One `Animatable` progress 0→1 over ~600 ms; a stitch is drawn when `index/total <= progress`. One animatable, one Canvas — cheap. Skip when the fabric has < 14 days (looks like a stutter, not a reveal).

New tokens: `FabricMinCellSize = 11.dp`, `FabricStitchStrokeRatio` (const), `FabricSelectedOutlineWidth = 1.5.dp`, `FabricLabelBandHeight = 22.dp` (match `ChartAxisBandHeight`), alpha levels `FabricInkLevel1/2/3 = 0.45f / 0.7f / 1f`.

New strings (EN + FI minimum): fabric a11y summary (monthly-equivalent of `insights_chart_a11y_monthly`), fabric interaction hint (reuse existing `insights_chart_hint_day` if the wording fits both, else `insights_fabric_hint`).

### B5. What happens to existing chart code

- `InsightsChart` keeps serving WEEK/MONTH. The `PaceGroupingInterval.MONTH` branch of `InsightsChart`/axis code **stays** (it's still used by `insightsChartAxis` until All Time is switched over, and axis month labels are reused by the fabric) — but `allTimeAxis`/`ALL_TIME_MONTH_BUCKET_LIMIT` and the `ChartAllTimeBarWidth` token become dead once the fabric lands: **delete them in the same change** and simplify `insightsChartAxis` to WEEK/MONTH ranges only, with `buildChartBuckets` skipped for ALL_TIME. Check all callers/tests referencing month-bucket All Time behavior and update them.
- `chartContentDescription`'s monthly branch moves to the fabric's summary string or is deleted if unused.

---

## Section order after the change (unchanged except the visual swap)

filters (segmented control + project chip) → kicker → hero (count-up) → strong rule → stats (positive streak) → trend line → "Every day" [WEEK/MONTH: bar chart with dim-selection · ALL_TIME: stitch fabric] → "Where the time went" (rows + share bars) → footer note.

---

## Testing & verification

Unit tests (JVM, `app/src/test/...`):

1. `InsightsFabricModelTest` — window alignment to locale week start (Mon vs Sun locales); dominant-project tie-break; level thresholds incl. all-days-equal and single-day windows; maxWeeks clamp; empty sessions → null/empty model; project filter passthrough; zone handling reuses `SessionMetrics` (cross-midnight day split — one regression test).
2. `InsightsChartModelTest` updates — remove/replace All Time month-axis cases; keep day-axis cases.
3. Streak framing — pure decision function extracted (e.g. `streakStatDisplay(current, best)`) and tested: 3 branches + trend-line suppression rule.
4. `axisLabelIndices` untouched (already tested).

Manual QA matrix (before claiming done):

- Light + dark × FI + EN × fontScale 1.0 / 1.3 / 2.0.
- TalkBack: filter group announces "selected, tab, 1 of 3"; chart & fabric: single summary + working next/previous actions; readout live region announces on action.
- Pro OFF: chart card shown once, no fabric, no streak column — layout intact.
- Data shapes: 0 sessions (empty state), 1 session today, sparse All Time (the screenshot case: 2 months), dense month (31 buckets), single-project filter.
- `./gradlew test`, `:app:detekt`, ktlint via user's `lc`.

Docs: update the Insights bullet in `CLAUDE.md` (UI Rules) and `PROJECT.md` if section structure/ownership changes — required by the architectural-change rule.

---

## Implementation order (each step ships green)

1. **A1** selection dim (delete band token) — smallest, most visible fix.
2. **A4** streak framing + new string.
3. **A5** project share bars.
4. **A3** segmented control (delete `InsightsRangeChip`).
5. **A6** hero count-up + bar growth.
6. **B** fabric: model + tests → renderer (static) → selection + a11y → entrance animation → swap All Time over and delete dead month-bucket All Time code.
7. Docs + QA matrix pass.

## Acceptance criteria

- No selection band anywhere; selection readable in both themes via dim + baseline marker.
- Streak stat never leads with a demotivating 0 when a best streak exists.
- Range switching animates (indicator slide, hero retarget, bars regrow) with no jank at fontScale 2.0.
- All Time shows the stitch fabric for Pro users; each day's color matches the project list dot for the same project.
- All new colors come from `MaterialTheme`/`knitToolsColors`/`yarnColorForId`; all new dimensions/text sizes live in `InsightsDimens`; all user-visible text in `strings.xml` (EN) with FI translation.
- No dead code left: `ChartSelectionBandAlpha`, `InsightsRangeChip`, `ChartAllTimeBarWidth`, `allTimeAxis` month path removed with their references.
- `test` + `detekt` + ktlint clean; TalkBack paths verified by hand.
