# KnitTools v2.0 Delta Spec — Navigation Restructure & New Features

**Scope:** Navigation restructure (4→5 tabs, Reference replaced by Library), Library tab, Insights tab, Yarn Stash expansion, Photos aggregation, Row Repeat with Shaping, Voice Commands, Contextual Onboarding, Help & Guide link.

**Base:** Current v1.0.0 codebase + existing specs.

---

## 1. Navigation Changes

### Bottom Navigation Bar: 5 tabs

| Tab | Label | Icon | Route (root) | Notes |
|-----|-------|------|---------------|-------|
| Projects | PROJECTS | FolderOpen | `project_list` | Unchanged |
| Library | LIBRARY | AutoStories | `library` | **NEW** — replaces Reference |
| Tools | TOOLS | Build | `tools` | Default start tab, unchanged |
| Insights | INSIGHTS | BarChart | `insights` | **NEW** |
| Settings | SETTINGS | Settings | `settings` | Unchanged — remains in nav bar |

`TopLevelDestination` enum updated to 5 entries: Projects, Library, Tools, Insights, Settings.

Settings stays as its own tab. No relocation needed.

### New routes (5)

| Screen | Route | Tab | Notes |
|--------|-------|-----|-------|
| Library | `library` | Library (root) | Replaces `reference` as root |
| Saved Patterns | `saved_patterns` | Library | NEW |
| My Yarn | `my_yarn` | Library | NEW — replaces `yarn_card_list` entry point |
| All Photos | `all_photos` | Library | NEW |
| Insights | `insights` | Insights (root) | NEW |

### Removed routes (3)

| Route | Reason |
|-------|--------|
| `reference` (as root) | Absorbed into Library |
| `yarn_card_list` | Replaced by `my_yarn` in Library |
| `ravelry_saved` | Replaced by `saved_patterns` in Library |

### Unchanged routes

All existing Reference detail routes (`needles`, `size_charts`, `abbreviations`, `chart_symbols`) remain — they navigate from Library instead of Reference Hub. All Tools routes unchanged. All Projects detail routes unchanged. Yarn Card detail route (`yarn_card_detail/{cardId}`) unchanged, accessed from My Yarn. Ravelry Search (`ravelry`) and Ravelry Detail (`ravelry_detail/{patternId}`) stay in Tools. Settings route (`settings`) unchanged.

### Updated total route count: ~25

---

## 2. Library Tab (NEW)

### Library Screen (root: `library`)

Layout: Scrollable list with two sections, same visual style as current Tools List and Reference Hub.

**MY COLLECTION section** (labelSmall dusty rose header)

| Item | Route | Accent Color | Right badge |
|------|-------|-------------|-------------|
| Saved Patterns | `saved_patterns` | Burnt orange (#C45100) | Item count |
| My Yarn | `my_yarn` | Avocado (#8BA44A) | Item count |
| Photos | `all_photos` | Mustard (#C9A435) | Item count |

- HubListItem components, no icons, accent-colored title text — identical pattern to Tools List and current Reference Hub
- Item count displayed as muted text on the right side of each HubListItem

**REFERENCE section** (labelSmall dusty rose header)

| Item | Route | Accent Color |
|------|-------|-------------|
| Needle Sizes | `needles` | Burnt orange (#C45100) |
| Size Charts | `size_charts` | Avocado (#8BA44A) |
| Abbreviations | `abbreviations` | Mustard (#C9A435) |
| Chart Symbols | `chart_symbols` | Dusty rose (#B8908F) |

- Identical to current Reference Hub items
- QuickTipCard remains on this screen

### Saved Patterns Screen (`saved_patterns`)

- Replaces the "Saved Patterns" tab inside Ravelry screen
- Shows all SavedPatternEntity items
- Same card design as current Ravelry Saved Patterns (name, difficulty, Free/Paid badge)
- TopAppBar: back arrow + "Saved Patterns" title
- Tap → navigates to `ravelry_detail/{patternId}`
- Empty state with appropriate illustration

### My Yarn Screen (`my_yarn`)

- Replaces `yarn_card_list` as entry point for saved yarn cards
- Shows all YarnCardEntity items
- Card design: brand + yarn name, fiber content, weight category badge (colored), quantity badge (e.g. "3 skeins"), status indicator (In Stash / In Use / Finished)
- TopAppBar: back arrow + "My Yarn" title
- FAB: add yarn manually (same flow as current yarn card creation)
- Tap → navigates to existing `yarn_card_detail/{cardId}`
- Empty state with yarn_card_empty image

### All Photos Screen (`all_photos`)

- Aggregates all ProgressPhotoEntity across all projects
- Layout: 2-column grid with SurfaceHigh card frames (identical card style to existing Photo Gallery)
- Each card shows: photo, **project name** (in addition to row number + date)
- Top: horizontal scrollable filter chips — "All" (default) + one chip per project that has photos
- Tap photo → PhotoViewer (same component as existing)
- TopAppBar: back arrow + "Photos" title
- Empty state with yarn_card_empty image
- **Pro only** (same restriction as existing Photo Gallery)

---

## 3. Insights Tab (NEW)

### Insights Screen (root: `insights`)

TopAppBar: "Insights" headlineMedium title, no back arrow (root screen).

**Project filter** — Top of screen. Dropdown or tappable chip: "All Projects" (default). Tap opens project selector (list of all projects). When a project is selected, all metrics below filter to that project.

**Metrics grid** — 2×2 grid of surfaceHigh cards (18dp corners, 16dp padding):

| Metric | Label | Format | Free/Pro |
|--------|-------|--------|----------|
| Total Time | TOTAL TIME | "X.Xh" or "Xh Xm" | Free |
| Avg Pace | AVG PACE | "X rows/hr" | Free |
| Projects Completed | COMPLETED | count | Free |
| Top Streak | TOP STREAK | "X days" | Pro |

Card design: labelSmall uppercase label (Secondary/avocado color) at top, large display number below (headlineMedium or larger), muted description text.

**Pace over time chart** — Below metrics grid. Line chart showing rows/hr over time. Time range toggle: Week / Month / Year (SegmentedToggle). surfaceHigh card background. Primary color line. Pro only.

**Time spent knitting chart** — Horizontal bar chart, one bar per project. surfaceHigh card background. Bars use deterministic color from YarnColors palette (same logic as project cards). Pro only.

**Pro hint banner** — Shown to free users below the two free metrics. "Unlock charts, streaks, and more with Pro" style, same as existing Pro hint banners.

### Insights Summary Card (Project List)

- Positioned below Continue Knitting hero card, above ACTIVE section
- surfaceHigh background, rounded corners (18dp)
- Single row: "4.2h this week · 18 rows/hr" (bodyMedium)
- Small chart icon on the left (Insights tab icon, muted color)
- Entire card tappable → navigates to Insights tab
- Hidden when no session data exists
- Free (uses the two free metrics)

### InsightsViewModel

- Scoped to Insights tab nav graph
- Sources: SessionEntity (aggregated queries), CounterProjectEntity
- Exposes: totalTime, avgPace, completedCount, topStreak, paceOverTime (list of data points), timePerProject
- Project filter: selectedProjectId StateFlow (null = all)

### Data source

All metrics derived from existing SessionEntity data. No new database tables needed. New DAO queries:

- `getTotalDurationMinutes(projectId: Long?)` — sum of durationMinutes
- `getSessionsInRange(start: Long, end: Long, projectId: Long?)` — for chart data
- `getStreakDays(projectId: Long?)` — count consecutive days with sessions
- `getCompletedProjectCount()` — count where isCompleted = true

---

## 4. Yarn Stash Expansion

### YarnCardEntity changes (Migration 6)

New fields:

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `quantityInStash` | Int | 1 | Number of skeins in stash |
| `status` | String | "IN_STASH" | Enum values: IN_STASH, IN_USE, FINISHED |
| `linkedProjectId` | Long? | null | FK to counter_projects, nullable, no cascade |

Migration 5→6: `ALTER TABLE yarn_cards ADD COLUMN quantityInStash INTEGER NOT NULL DEFAULT 1; ALTER TABLE yarn_cards ADD COLUMN status TEXT NOT NULL DEFAULT 'IN_STASH'; ALTER TABLE yarn_cards ADD COLUMN linkedProjectId INTEGER;`

### Yarn Card Detail screen changes

- Display quantity ("3 skeins in stash") — editable with +/- buttons
- Display status badge — tappable to cycle through IN_STASH → IN_USE → FINISHED
- Display linked project name if linkedProjectId set — tappable to navigate to counter
- Status colors: IN_STASH = avocado, IN_USE = burnt orange, FINISHED = textMuted

### Free/Pro for My Yarn

- Free: save up to 3 yarn cards
- Pro: unlimited
- Pro hint shown when free user has 3 cards and taps FAB

### Data flow: Yarn Estimator → Library

When user scans yarn label via OCR in Yarn Estimator and saves:
1. YarnCardEntity created (existing flow)
2. After save, dialog: "Link to [current project]?" (existing flow)
3. Card now appears in Library → My Yarn (new location)
4. **Toast/snackbar: "Saved to My Yarn in Library"** — guides user to the new location

Yarn Estimator screen: camera icon and saved yarns icon in TopAppBar remain, but saved yarns icon now navigates to `my_yarn` route in Library tab instead of `yarn_card_list` in Tools.

### Data flow: Ravelry → Library

When user saves a pattern from Ravelry Search:
1. SavedPatternEntity created (existing flow)
2. **Toast/snackbar: "Saved to Library"** — guides user to Library tab

---

## 5. Row Repeat with Shaping (Pro)

### AddCounterDialog changes

New counter type added to existing types:

| Type | Label | Description | Existing/New |
|------|-------|-------------|-------------|
| Count up | Simple counter | Count each row | Existing |
| Repeating | Pattern repeat | Resets at N | Existing |
| **Shaping** | **Shaping counter** | **Track increases/decreases** | **NEW** |

When "Shaping" is selected, additional fields appear:

| Field | Label | Type | Example |
|-------|-------|------|---------|
| Starting stitches | Starting stitch count | Number | 80 |
| Change per shaping row | Stitches to add/remove | Number (signed) | -2 |
| Every N rows | Shape every N rows | Number | 4 |

### ProjectCounterEntity changes (Migration 6)

New fields:

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `counterType` | String | "COUNT_UP" | COUNT_UP, REPEATING, SHAPING |
| `startingStitches` | Int? | null | For SHAPING type |
| `stitchChange` | Int? | null | For SHAPING type (negative = decrease) |
| `shapeEveryN` | Int? | null | For SHAPING type |

Migration includes these in the 5→6 migration.

### Shaping Counter display (CounterListItem)

- Shows counter name + current count (same as other types)
- Additional line below: "Next shaping: Row X → Y sts"
- When current row hits a shaping row, highlight text in Primary color
- Uses ProjectCounterLogic for calculations (new `calculateCurrentStitches()` and `nextShapingRow()` methods)

---

## 6. Voice Commands (Pro)

### Activation

- **Long-press on + button** activates voice listening mode
- No additional UI elements in TopAppBar — keeps Counter screen clean
- First activation: tooltip "Hold + to use voice commands"

### Listening state

- Pulsating microphone indicator appears inline, between counter number and buttons
- Subtle animation (opacity pulse or expanding ring), consistent with app's understated design philosophy
- Indicator disappears when listening stops (on command recognition or timeout)

### Supported commands

| Command (EN) | Action | Notes |
|-------------|--------|-------|
| "plus" / "next" / "add" | +1 row | Same as tap + |
| "minus" / "back" / "undo" | -1 row | Same as tap − |
| "undo" | Undo last action | Same as undo button |
| "reset" | Reset counter | Shows confirmation dialog |

Localization: Finnish equivalents ("plus", "seuraava", "lisää", "miinus", "takaisin", "kumoa", "nollaa"). Language follows device locale.

### Implementation

- Android SpeechRecognizer (on-device, no network required when available)
- Continuous listening while button held, or toggle mode (press to start, press to stop)
- Haptic feedback on recognized command (if haptic setting enabled)
- Permission: `RECORD_AUDIO` — requested on first long-press, with rationale dialog

### Pro gating

- Voice commands are a Pro feature
- Free users who long-press: show Pro upgrade prompt instead of activating voice

---

## 7. Contextual Onboarding & Tooltips

### Tooltip system

Contextual tooltips shown once per feature, on first encounter. Tracked in DataStore as a set of dismissed tooltip IDs.

| Tooltip ID | Trigger | Text | Location |
|-----------|---------|------|----------|
| `voice_commands` | First time opening Counter screen (Pro) | "Hold + to use voice commands" | Near + button |
| `library_tab` | First time Library tab appears | "Your saved patterns, yarn, and photos live here" | Library tab area |
| `insights_tab` | First time Insights tab appears | "Track your knitting stats and progress" | Insights tab area |
| `shaping_counter` | First time opening AddCounterDialog (Pro) | "New: Shaping counters track your increases and decreases" | Counter type selector |
| `swipe_project` | First time viewing Project List with items | "Swipe left to archive or delete" | First project card |
| `long_press_project` | First time viewing Project List with items (delayed, after swipe tooltip dismissed) | "Long-press for more options" | First project card |
| `yarn_stash` | First time opening My Yarn | "Track your yarn stash — quantity, status, and linked projects" | My Yarn screen |
| `photo_counter` | First time opening Counter with camera icon | "Tap camera to take progress photos" | Camera icon |

### Implementation

- M3 `RichTooltip` or `PlainTooltip` component
- DataStore key: `dismissed_tooltips` (Set<String>)
- Each tooltip shown max once, dismisses on tap or after 5 seconds
- Tooltips do not stack — max one visible at a time

### Help & Guide link

- **Settings screen:** New item "Help & Guide" at the bottom of the settings list
- Tap → opens KnitTools website guide page via Custom Chrome Tab
- Same pattern as existing web links in the app
- No in-app help section — website handles all documentation

---

## 8. Cross-Tab Navigation Feedback

When content is saved in one tab and lives in another, toast/snackbar messages guide the user:

| Action | Message | Tab where content lives |
|--------|---------|------------------------|
| Save pattern from Ravelry | "Saved to Library" | Library → Saved Patterns |
| Save yarn card from OCR | "Saved to My Yarn in Library" | Library → My Yarn |
| Take progress photo | (no toast — stays in current tab context) | Library → Photos (aggregated) |
| Session auto-saved | (no toast — background operation) | Insights |

---

## 9. Data Flow Summary

| Action | Source (tab) | Destination (tab) |
|--------|-------------|-------------------|
| Save pattern from Ravelry | Tools → Ravelry Search | Library → Saved Patterns |
| Start project from Ravelry | Tools → Ravelry Search | Projects → Row Counter |
| Save yarn card from OCR | Tools → Yarn Estimator | Library → My Yarn |
| Link yarn to project | Projects → Counter | Library → My Yarn (updates linkedProjectId) |
| Take progress photo | Projects → Counter | Library → Photos (aggregated) |
| Session recorded | Projects → Counter | Insights (aggregated) |

---

## 10. Migration Plan

**Room Migration 5→6** (manual migration):

1. `ALTER TABLE yarn_cards ADD COLUMN quantityInStash INTEGER NOT NULL DEFAULT 1`
2. `ALTER TABLE yarn_cards ADD COLUMN status TEXT NOT NULL DEFAULT 'IN_STASH'`
3. `ALTER TABLE yarn_cards ADD COLUMN linkedProjectId INTEGER`
4. `ALTER TABLE project_counters ADD COLUMN counterType TEXT NOT NULL DEFAULT 'COUNT_UP'`
5. `ALTER TABLE project_counters ADD COLUMN startingStitches INTEGER`
6. `ALTER TABLE project_counters ADD COLUMN stitchChange INTEGER`
7. `ALTER TABLE project_counters ADD COLUMN shapeEveryN INTEGER`
8. Backfill: `UPDATE project_counters SET counterType = 'REPEATING' WHERE repeatAt IS NOT NULL`

Instrumented migration test: `migrate5to6` + `migrate1to6` (full chain).

---

## 11. Files to Create/Modify

### New files
- `ui/screens/library/LibraryScreen.kt` + `LibraryViewModel.kt`
- `ui/screens/library/SavedPatternsScreen.kt`
- `ui/screens/library/MyYarnScreen.kt`
- `ui/screens/library/AllPhotosScreen.kt`
- `ui/screens/insights/InsightsScreen.kt` + `InsightsViewModel.kt`
- `ui/screens/insights/InsightsSummaryCard.kt` (component for Project List)
- `domain/calculator/ShapingCounterLogic.kt`
- `ui/screens/counter/VoiceCommandHandler.kt` (voice recognition logic)
- `ui/components/TooltipManager.kt` (contextual tooltip tracking + display)

### Modified files
- `ui/navigation/Screen.kt` — new routes, remove old routes
- `ui/navigation/TopLevelDestination.kt` — 5 entries (Projects, Library, Tools, Insights, Settings)
- `ui/navigation/KnitToolsNavHost.kt` — new nav graphs for Library and Insights
- `ui/navigation/KnitToolsBottomBar.kt` — updated tabs (5)
- `ui/screens/projects/ProjectListScreen.kt` — Insights summary card
- `ui/screens/counter/CounterScreen.kt` — long-press voice activation on + button, voice indicator
- `ui/screens/counter/MultiCounterComponents.kt` — AddCounterDialog shaping type
- `ui/screens/settings/SettingsScreen.kt` — "Help & Guide" link
- `domain/calculator/ProjectCounterLogic.kt` — shaping calculations
- `data/local/ProjectCounterEntity.kt` — new fields
- `data/local/YarnCardEntity.kt` — new fields
- `data/local/KnitToolsDatabase.kt` — migration 5→6, version bump
- `data/local/SessionDao.kt` — new aggregate queries
- `data/local/YarnCardDao.kt` — queries for status/stash
- `data/local/ProgressPhotoDao.kt` — cross-project query
- `data/datastore/PreferencesManager.kt` — dismissed_tooltips set
- `repository/YarnCardRepository.kt` — stash methods
- `pro/ProFeature.kt` — new features: INSIGHTS_CHARTS, STREAK, SHAPING_COUNTER, UNLIMITED_YARN, VOICE_COMMANDS
- `AndroidManifest.xml` — RECORD_AUDIO permission

### Removed/deprecated
- Reference Hub screen (content moved to Library)
- `yarn_card_list` route (replaced by `my_yarn`)
- `ravelry_saved` route (replaced by `saved_patterns`)

---

## 12. Future Scope (v3)

Features discussed but intentionally deferred:

- **PDF Pattern Viewer** — Open PDF patterns inside the app with row tracking/highlight. Accessed from project card ("Attach pattern" action). Linked to Counter for synchronized row tracking. Includes basic annotation/scribble. This is a large feature requiring its own delta spec.
- **Stitch-level tracking** — Count stitches within a row (complement to row counter). Natural extension of Multiple Counters.
- **Explicit Repeat Tracking** — "Repeat rows 3–8, 5 times" with visual progress. Pattern-integrated repeat sections beyond the current Repeating counter type.
