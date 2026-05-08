# KnitTools v3.0 Delta Spec — Pattern Viewer & Advanced Tracking

**Scope:** PDF Pattern Viewer with row tracking, stitch-level tracking, explicit repeat tracking.

**Base:** v2.0.0 codebase (Library tab, Insights, Shaping counters, Voice Commands).

---

## 1. PDF Pattern Viewer

### Concept

Open PDF knitting patterns directly inside the app with synchronized row tracking. The viewer highlights the active row and lets the user annotate the pattern. Linked to the Counter so advancing the row counter also advances the pattern highlight.

### Entry point

**Project card in Counter screen.** New tappable action in the compact project card:

- If no pattern attached: "Attach pattern" text link (Tertiary color) → opens pattern picker
- If pattern attached: pattern name displayed (tappable → opens viewer), with × to detach

No new icons in TopAppBar. The project card already holds yarn links and notes — pattern fits the same pattern.

### Pattern picker

ModalBottomSheet with three sources:

| Source | Description | Notes |
|--------|-------------|-------|
| Saved Patterns | From Library → Saved Patterns (Ravelry) | Uses existing SavedPatternEntity.patternUrl |
| Device files | Android file picker (ACTION_OPEN_DOCUMENT) | PDF only, persistable URI permission |
| Camera scan | Photograph a printed pattern → PDF conversion | Uses CameraX + ML Kit (existing deps) |

Selected pattern is stored as a URI reference on the project entity.

### CounterProjectEntity changes (Migration 7)

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `patternUri` | String? | null | Content URI or file path to PDF |
| `patternName` | String? | null | Display name |
| `currentPatternPage` | Int | 0 | Last viewed page |
| `patternRowMapping` | String? | null | JSON: row-to-page/position mapping |

Note: `linkedPatternId` (existing, FK to saved_patterns) is kept for Ravelry patterns. `patternUri` is the actual file reference for the viewer.

### Pattern Viewer screen

New route: `pattern_viewer/{projectId}` in Projects tab.

**Layout:**

- **TopAppBar:** Back arrow + pattern name (truncated) + tools overflow menu (annotation mode, page jump, detach pattern)
- **PDF render area:** Full-width, vertically scrollable. Rendered using `android.graphics.pdf.PdfRenderer` (API 21+, no external dependency).
- **Row highlight overlay:** Semi-transparent Primary-colored band across the active row region. Position determined by row mapping (manual or auto).
- **Bottom bar:** Compact floating bar at bottom:
  - Current row number (synced with Counter)
  - −/+ row buttons (mirrors Counter, updates both)
  - Page indicator ("Page 3 of 12")

### Row-to-pattern mapping

Two modes:

**Manual mapping (free + Pro):**
- User long-presses on the PDF to place a "row marker" at a vertical position
- Each marker corresponds to a row number (auto-incremented or manually set)
- Markers stored in `patternRowMapping` as JSON array: `[{"row": 1, "page": 0, "yPosition": 0.15}, ...]`
- Between markers, the highlight interpolates linearly

**Auto mapping (Pro, Gemini Nano):**
- On pattern attach, Nano analyzes visible text to detect row numbers, chart rows, or instruction structure
- Suggests row markers automatically — user confirms or adjusts
- Falls back to manual if Nano unavailable or detection confidence low
- Uses existing `InstructionParser` patterns extended for row detection

### Annotation / Scribble (Pro)

- Toggle annotation mode from TopAppBar overflow menu
- Drawing overlay on top of PDF render
- Tools: pen (Primary color), highlighter (Secondary, 40% opacity), eraser
- Stroke data stored per page as serialized path data in Room (new entity)
- Annotations persist across sessions

### PatternAnnotationEntity (Migration 7)

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK auto-generate |
| `projectId` | Long | FK to counter_projects, CASCADE |
| `page` | Int | PDF page number |
| `pathData` | String | Serialized drawing path data (JSON) |
| `color` | String | Hex color |
| `strokeWidth` | Float | dp |
| `createdAt` | Long | Timestamp |

### Synchronization with Counter

- Pattern Viewer and Counter share the same `CounterViewModel` (already scoped to Projects nav graph)
- Advancing counter in either screen updates both
- When pattern viewer is open, a "Show pattern" chip appears in Counter screen below the project card (tappable → navigates to viewer)
- When counter is active, pattern viewer bottom bar shows live row count

### Free/Pro

| Feature | Free | Pro |
|---------|------|-----|
| Attach PDF from device | Yes | Yes |
| Attach from Saved Patterns | Yes | Yes |
| Manual row mapping | Yes | Yes |
| Row highlight sync | Yes | Yes |
| Auto row mapping (Nano) | — | Yes |
| Annotation/scribble | — | Yes |
| Camera scan to PDF | — | Yes |

---

## 2. Stitch-Level Tracking

### Concept

Optional per-row stitch counter that tracks position within a row. Complements the main row counter — row counter tracks vertical progress, stitch counter tracks horizontal progress.

### UI in Counter screen

When enabled for a project, a new compact bar appears between the main counter number and the +/−/undo buttons:

```
         Row 47
  ──────────────────
  Stitch: ◄ 23/120 ►
  ──────────────────
      −    [+]   undo
```

- `◄` and `►` buttons decrement/increment stitch count
- Display: "23/120" (current / total stitches in row)
- Total stitches per row comes from project's `stitchCount` field (existing) or per-row override
- When stitch count reaches total: subtle highlight, optional haptic
- Advancing to next row resets stitch counter to 0
- Stitch counter is hidden when not enabled (no visual noise for users who don't need it)

### Enabling stitch tracking

- Toggle in project card: "Track stitches" switch
- Or: if `stitchCount` is set on the project, offer to enable stitch tracking via tooltip on first Counter open

### CounterProjectEntity changes (Migration 7)

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `stitchTrackingEnabled` | Boolean | false | Show stitch counter |
| `currentStitch` | Int | 0 | Current stitch position in row |

### Voice command extension

New voice commands when stitch tracking is enabled:

| Command (EN) | Action |
|-------------|--------|
| "stitch" / "next stitch" | +1 stitch |
| "back stitch" | −1 stitch |

Finnish: "silmukka", "seuraava silmukka", "edellinen silmukka"

### Interaction with Pattern Viewer

When both stitch tracking and pattern viewer are active, the row highlight in the viewer can optionally show a vertical cursor at the approximate stitch position (if row width is mapped). This is a stretch goal — not required for initial implementation.

### Free/Pro

- Stitch tracking: **Free** (it's a basic counting feature)
- Voice commands for stitches: **Pro** (part of voice commands Pro feature)

---

## 3. Explicit Repeat Tracking

### Concept

Track multi-row repeat sections: "Repeat rows 3–8, 5 times." The app knows which repeat you're on, which row within the repeat, and shows progress. Goes beyond the existing Repeating counter type (which just resets at N) by being aware of the row range and total repeat count.

### New counter type in AddCounterDialog

| Type | Label | Description | Existing/New |
|------|-------|-------------|-------------|
| Count up | Simple counter | Count each row | Existing |
| Repeating | Pattern repeat | Resets at N | Existing |
| Shaping | Shaping counter | Track increases/decreases | Existing (v2) |
| **Repeat section** | **Row repeat** | **Track multi-row repeats** | **NEW** |

When "Repeat section" is selected:

| Field | Label | Type | Example |
|-------|-------|------|---------|
| Start row | Repeat starts at row | Number | 3 |
| End row | Repeat ends at row | Number | 8 |
| Total repeats | Number of repeats | Number | 5 |

### ProjectCounterEntity changes (Migration 7)

New fields for REPEAT_SECTION type:

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `repeatStartRow` | Int? | null | First row of repeat section |
| `repeatEndRow` | Int? | null | Last row of repeat section |
| `totalRepeats` | Int? | null | How many times to repeat |
| `currentRepeat` | Int? | null | Which repeat we're on (1-indexed) |

`counterType` gains new value: `REPEAT_SECTION`

### Repeat Section display (CounterListItem)

Expanded display compared to other counter types:

```
Sleeve increases
  Repeat 3 of 5 · Row 4 of 6
  ████████░░░░░░  (progress bar)
  Next: Row 5 — k2tog, knit to end
```

- Counter name (top)
- "Repeat X of Y · Row Z of N" (Secondary/avocado color)
- Compact progress bar (Primary fill, SurfaceHighest track)
- Optional: next row instruction (if pattern viewer is linked and row mapping exists)

### Auto-advance logic

- Main row counter advances → repeat section counter checks if current row is within its range
- If yes: updates internal row-within-repeat position
- When end row reached: increments repeat count, resets row position to start
- When all repeats completed: counter shows "Complete" state with checkmark
- **Does not auto-advance main counter** — it observes the main counter, doesn't control it

### Interaction with Shaping Counter

Repeat sections and shaping counters can coexist on the same project. Common real-world case: "Repeat rows 3–8 five times, decreasing 2 stitches every 4th row." Both counters observe the main row counter independently.

### Free/Pro

- Repeat section tracking: **Pro** (advanced tracking feature)

---

## 4. Migration Plan

**Room Migration 6→7** (manual migration):

1. `ALTER TABLE counter_projects ADD COLUMN patternUri TEXT`
2. `ALTER TABLE counter_projects ADD COLUMN patternName TEXT`
3. `ALTER TABLE counter_projects ADD COLUMN currentPatternPage INTEGER NOT NULL DEFAULT 0`
4. `ALTER TABLE counter_projects ADD COLUMN patternRowMapping TEXT`
5. `ALTER TABLE counter_projects ADD COLUMN stitchTrackingEnabled INTEGER NOT NULL DEFAULT 0`
6. `ALTER TABLE counter_projects ADD COLUMN currentStitch INTEGER NOT NULL DEFAULT 0`
7. `ALTER TABLE project_counters ADD COLUMN repeatStartRow INTEGER`
8. `ALTER TABLE project_counters ADD COLUMN repeatEndRow INTEGER`
9. `ALTER TABLE project_counters ADD COLUMN totalRepeats INTEGER`
10. `ALTER TABLE project_counters ADD COLUMN currentRepeat INTEGER`
11. `CREATE TABLE pattern_annotations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, page INTEGER NOT NULL, pathData TEXT NOT NULL, color TEXT NOT NULL, strokeWidth REAL NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY (projectId) REFERENCES counter_projects(id) ON DELETE CASCADE)`
12. `CREATE INDEX index_pattern_annotations_projectId ON pattern_annotations(projectId)`

Instrumented migration tests: `migrate6to7` + `migrate1to7` (full chain).

---

## 5. Files to Create/Modify

### New files
- `ui/screens/pattern/PatternViewerScreen.kt` + `PatternViewerViewModel.kt`
- `ui/screens/pattern/PatternPickerSheet.kt` (ModalBottomSheet for source selection)
- `ui/screens/pattern/AnnotationOverlay.kt` (drawing canvas)
- `ui/screens/pattern/RowHighlightOverlay.kt` (row tracking visualization)
- `ui/screens/pattern/PdfPageRenderer.kt` (PdfRenderer wrapper)
- `ui/components/StitchCounter.kt` (compact inline stitch tracking bar)
- `domain/calculator/RepeatSectionLogic.kt`
- `domain/calculator/RowMappingParser.kt` (manual + Nano row detection)
- `data/local/PatternAnnotationEntity.kt` + `PatternAnnotationDao.kt`

### Modified files
- `ui/navigation/Screen.kt` — `pattern_viewer/{projectId}` route
- `ui/navigation/KnitToolsNavHost.kt` — pattern viewer in Projects nav graph
- `ui/screens/counter/CounterScreen.kt` — pattern link in project card, stitch counter bar, "Show pattern" chip
- `ui/screens/counter/MultiCounterComponents.kt` — AddCounterDialog repeat section type, RepeatSectionItem display
- `domain/calculator/ProjectCounterLogic.kt` — repeat section calculations
- `data/local/CounterProjectEntity.kt` — new fields (patternUri, patternName, currentPatternPage, patternRowMapping, stitchTrackingEnabled, currentStitch)
- `data/local/ProjectCounterEntity.kt` — new fields (repeatStartRow, repeatEndRow, totalRepeats, currentRepeat)
- `data/local/KnitToolsDatabase.kt` — migration 6→7, version bump, PatternAnnotationEntity added
- `ai/nano/InstructionParser.kt` — extended for row detection in patterns
- `pro/ProFeature.kt` — new features: PATTERN_ANNOTATION, PATTERN_AUTO_MAPPING, PATTERN_CAMERA_SCAN, REPEAT_SECTION

### New permissions
- None (CAMERA already declared for OCR + Progress Photos; file picker uses SAF, no permission needed)

---

## 6. Gemini Nano Extensions

### New Nano feature: Pattern Row Detector

| Feature | File | Description |
|---------|------|-------------|
| Pattern Row Detector | `ai/nano/PatternRowDetector.kt` | Analyzes PDF text content to detect row boundaries, chart structures, and instruction patterns. Outputs suggested row markers with confidence scores. |

- Input: extracted text from PDF page (via PdfRenderer + text extraction)
- Output: list of `RowMarkerSuggestion(row: Int, page: Int, yPosition: Float, confidence: Float)`
- Confidence threshold: 0.7 — below this, marker is shown as "suggested" (dashed line) for user confirmation
- Regex fallback for common patterns: "Row 1:", "Rnd 1:", numbered chart rows
- Added to existing Nano feature table in GEMINI-NANO.md

---

## 7. Free/Pro Summary

| Feature | Free | Pro |
|---------|------|-----|
| Attach PDF pattern | Yes | Yes |
| Manual row mapping | Yes | Yes |
| Row highlight sync | Yes | Yes |
| Stitch-level tracking | Yes | Yes |
| Auto row mapping (Nano) | — | Yes |
| Pattern annotation/scribble | — | Yes |
| Camera scan to PDF | — | Yes |
| Repeat section counter | — | Yes |
| Voice commands for stitches | — | Yes |
