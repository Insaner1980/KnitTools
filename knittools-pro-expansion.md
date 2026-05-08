# KnitTools — Pro Feature Expansion

**Purpose:** Strengthen the Pro value proposition for users whose devices don't support Gemini Nano. These three features work on all devices, integrate into existing screens, and create daily-use habits that make the reverse trial conversion compelling.

**Context:** Currently, non-Nano Pro features are: unlimited projects, full session history, notes, pattern repeat counter, yarn label OCR + saved cards, and home screen widget. These are valuable but don't create strong enough "loss aversion" after the 7-day trial. The features below address this gap.

**Style direction:** Material Design 3, KnitTools warm craft aesthetic (see existing design spec).

---

## Feature 1: Row Reminders

**Priority:** Highest — this is the single most impactful addition.
**Pro gate:** Yes. Free users see reminders during trial but cannot create new ones after trial expires. Existing reminders remain visible (read-only) as a teaser.
**Location:** Counter Screen (Projects tab)

### What it does

Users attach reminders to specific row numbers. When the counter reaches that row, a prominent inline alert appears on the Counter Screen. Examples:
- "Row 12: Start armhole shaping"
- "Row 47: Switch to color B"
- "Every 8 rows: Check gauge"

### Reminder types

1. **One-time reminder** — triggers at a specific row number, then marks itself as done
2. **Repeating reminder** — triggers every N rows (e.g., every 8th row), shows which repeat you're on

### Data model

New Room entity — requires migration v3 → v4:

```
RowReminderEntity
├── id: Long (PK, auto-generate)
├── projectId: Long (FK → counter_projects, CASCADE)
├── targetRow: Int
├── repeatInterval: Int? (null = one-time, value = every N rows)
├── message: String (max 200 chars)
├── isCompleted: Boolean (default false, only for one-time)
├── createdAt: Long
```

New DAO: `RowReminderDao`
- `getRemindersForProject(projectId): Flow<List<RowReminderEntity>>`
- `getActiveReminderAtRow(projectId, currentRow): Flow<List<RowReminderEntity>>`
- `insert(reminder)`
- `update(reminder)`
- `delete(reminder)`
- `deleteAllForProject(projectId)`

### UI placement on Counter Screen

```
┌─────────────────────────────────────┐
│ CURRENT PROJECT          ✨  >     │  ← existing project card
│ My Socks                           │
│ 🧶 Novita 7v  ×    + Add yarn  📝 │
├─────────────────────────────────────┤
│                                     │
│           TOTAL ROWS                │
│             047                     │  ← existing rolling counter
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🔔 Row 48: Switch to heel flap ││  ← NEW: reminder alert (gold bg)
│  └─────────────────────────────────┘│
│                                     │
│     [ − ]      [ + ]      [ ↺ ]    │  ← existing buttons
│                                     │
│  ┌──pattern repeat──┐               │
│  │  − Section 4/8 + │               │  ← existing pattern repeat
│  └──────────────────┘               │
│                                     │
│  REMINDERS (2)            + Add  🔒 │  ← NEW: reminder list header
│  ┌─────────────────────────────────┐│     (🔒 = Pro badge if not Pro)
│  │ Row 48  Switch to heel flap  ● ││  ← upcoming (gold dot)
│  │ Row 72  Bind off              ○ ││  ← further away (muted dot)
│  │ Every 8 rows  Check gauge     ↻ ││  ← repeating (↻ icon)
│  └─────────────────────────────────┘│
│                                     │
│    STITCHES 64    TIME 1h 23m       │  ← existing stats row
│           Reset Counter             │
└─────────────────────────────────────┘
```

### Reminder alert behavior

- When `currentRow == targetRow` (or `currentRow % repeatInterval == 0`): show a prominent alert card between the counter and the +/- buttons
- Alert card: `surfaceVariant` background with gold left border, bell icon, reminder message, "Dismiss" text button
- Haptic feedback (if enabled) when alert triggers
- For repeating reminders: show "Repeat 3/8" counter in the alert
- Alert auto-dismisses when user taps + (advances past the row) but stays if they tap −

### Add Reminder dialog

Triggered by "+ Add" button in the reminders section header.

```
┌──────────────────────────────────┐
│         ADD REMINDER             │
│                                  │
│  Row number     [___48___]       │  ← NumberInputField (existing)
│                                  │
│  ○ One-time   ● Repeating        │  ← SegmentedToggle (existing)
│                                  │
│  Repeat every  [____8____] rows  │  ← only if Repeating selected
│                                  │
│  Message                         │
│  ┌──────────────────────────┐    │
│  │ Switch to heel flap      │    │
│  └──────────────────────────┘    │
│                                  │
│        [Cancel]    [Save]        │
│                                  │
└──────────────────────────────────┘
```

- Uses existing `NumberInputField` and `SegmentedToggle` components
- Standard `AlertDialog` pattern (same as rename dialog)
- Message field: single-line `TextField`, max 200 chars

### Swipe-to-delete

Reminders in the list can be swiped to delete (same pattern as project swipe-to-dismiss). No confirmation needed for reminder deletion.

### ViewModel changes

`CounterViewModel` already manages counter state. Add:
- `reminders: StateFlow<List<RowReminder>>` — all reminders for current project
- `activeAlert: StateFlow<RowReminder?>` — the reminder to show as alert (if any)
- `addReminder(targetRow, repeatInterval, message)`
- `dismissReminder(reminderId)`
- `deleteReminder(reminderId)`

Active alert computation: when `count` changes, check if any reminder matches current row.

### Tests

- Unit tests for reminder trigger logic (one-time, repeating, edge cases like row 0, negative)
- Unit tests for repeating reminder repeat count calculation
- Migration test v3 → v4

---

## Feature 2: Progress Photos

**Priority:** Second — creates emotional attachment to the app.
**Pro gate:** Yes. Free users can view photos taken during trial but cannot add new ones after trial expires.
**Location:** Counter Screen (inline) + Session History (gallery view)

### What it does

Users take a photo of their work-in-progress directly from the Counter Screen. Photos are timestamped and tagged with the current row number, creating a visual timeline of the project.

### Data model

New Room entity — add in the same v3 → v4 migration:

```
ProgressPhotoEntity
├── id: Long (PK, auto-generate)
├── projectId: Long (FK → counter_projects, CASCADE)
├── photoUri: String (local file URI)
├── rowNumber: Int (counter value when photo was taken)
├── note: String? (optional short caption, max 100 chars)
├── createdAt: Long
```

New DAO: `ProgressPhotoDao`
- `getPhotosForProject(projectId): Flow<List<ProgressPhotoEntity>>`
- `getLatestPhoto(projectId): Flow<ProgressPhotoEntity?>`
- `getPhotoCount(projectId): Flow<Int>`
- `insert(photo)`
- `delete(photo)`

### Photo storage

- Save to app-internal storage: `context.filesDir/progress_photos/{projectId}/{timestamp}.jpg`
- Compress to JPEG quality 80, max dimension 1920px (keeps file size ~200-400KB)
- When a project is deleted (CASCADE), also delete the photo files from disk via a `ProjectCleanupWorker` or repository-level cleanup
- No cloud sync — consistent with privacy-first philosophy

### UI placement on Counter Screen

Add a camera icon button to the project card header row:

```
┌─────────────────────────────────────┐
│ CURRENT PROJECT      📷  ✨  >     │  ← NEW: camera icon added
│ My Socks                           │
│ 🧶 Novita 7v  ×    + Add yarn  📝 │
│                                     │
│ ┌─ latest photo ──────────────────┐│  ← NEW: thumbnail strip (if photos exist)
│ │ 🖼 Row 12  🖼 Row 28  🖼 Row 41 ││     horizontally scrollable
│ └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

- Camera icon: 24dp, `onSurfaceVariant` color, same visual weight as sparkle and note icons
- Photo thumbnail strip: horizontal `LazyRow`, 48dp height, rounded 8dp corners, shows last 5 photos
- Tap a thumbnail → full-screen photo viewer (standard M3 dialog or full-screen composable) with row number and date overlay
- Tap camera icon → system camera intent (already have CAMERA permission for OCR), auto-save on return

### Session History integration

On the Session History screen, show photos taken during each session:

```
┌─────────────────────────────────────┐
│ April 3, 2026                       │
│ 45 min · Rows 28 → 41              │
│ ┌──────┐ ┌──────┐                  │  ← photos from this session
│ │ 🖼   │ │ 🖼   │                  │
│ │Row 34│ │Row 41│                  │
│ └──────┘ └──────┘                  │
├─────────────────────────────────────┤
│ April 1, 2026                       │
│ 1h 12min · Rows 12 → 28            │
│ No photos                          │
└─────────────────────────────────────┘
```

Photos are associated with sessions by matching `createdAt` timestamp to session `startedAt`/`endedAt` range.

### Share functionality

Long-press on a photo in the viewer → standard Android share sheet. This naturally drives social sharing (Instagram, Ravelry) without building custom integrations.

### ViewModel changes

`CounterViewModel` — add:
- `photos: StateFlow<List<ProgressPhoto>>` — all photos for current project
- `latestPhotos: StateFlow<List<ProgressPhoto>>` — last 5 for thumbnail strip
- `takePhoto()` — launches camera intent, saves result
- `deletePhoto(photoId)` — with confirmation dialog

### Tests

- Unit tests for photo-to-session matching logic
- Unit tests for file path generation
- Migration test (combined with Feature 1 in v3 → v4)

---

## Feature 3: Multiple Counters

**Priority:** Third — extends existing functionality naturally.
**Pro gate:** Yes. Free users get the existing single secondary counter (pattern repeat). Pro unlocks multiple named counters.
**Location:** Counter Screen (Projects tab)

### What it does

Instead of a single pattern repeat counter, Pro users can add multiple named counters that track independently. Each counter has its own name, value, and optional repeat interval. Examples:
- "Sleeve increases" — count up
- "Cable repeat" — repeating every 8 rows
- "Buttonholes" — count specific occurrences

### Data model

New Room entity — add in the same v3 → v4 migration:

```
ProjectCounterEntity
├── id: Long (PK, auto-generate)
├── projectId: Long (FK → counter_projects, CASCADE)
├── name: String (max 50 chars)
├── count: Int (default 0)
├── stepSize: Int (default 1)
├── repeatAt: Int? (null = count up indefinitely, value = reset after N)
├── sortOrder: Int (for manual ordering)
├── createdAt: Long
```

New DAO: `ProjectCounterDao`
- `getCountersForProject(projectId): Flow<List<ProjectCounterEntity>>`
- `insert(counter)`
- `update(counter)`
- `delete(counter)`
- `reorder(projectId, counterIds: List<Long>)`

### Migration from existing secondaryCount

The existing `secondaryCount` and `stepSize` fields on `CounterProjectEntity` represent the current single pattern repeat counter. Migration strategy:

- Keep the existing fields for backward compatibility
- If a project has `secondaryCount > 0`, auto-create a `ProjectCounterEntity` named "Pattern repeat" with the existing values
- New UI reads from `ProjectCounterEntity` table instead
- Old fields become unused but remain in schema (no breaking change)

### UI placement on Counter Screen

Replace the single pattern repeat pill with a counter list:

```
┌─────────────────────────────────────┐
│           TOTAL ROWS                │
│             047                     │
│                                     │
│  🔔 Row 48: Switch to heel flap    │  ← Feature 1 reminder
│                                     │
│     [ − ]      [ + ]      [ ↺ ]    │
│                                     │
│  COUNTERS (3)             + Add  🔒 │  ← NEW: replaces single pattern repeat
│  ┌─────────────────────────────────┐│
│  │ Pattern repeat    − 4/8 +      ││  ← existing style, now in a list
│  │ Sleeve increases  −  6  +      ││
│  │ Buttonholes       −  2  +      ││
│  └─────────────────────────────────┘│
│                                     │
│    STITCHES 64    TIME 1h 23m       │
│           Reset Counter             │
└─────────────────────────────────────┘
```

- Each counter is a compact row: name (left), −/count/+ controls (right)
- Repeating counters show "4/8" format (current/repeatAt)
- Non-repeating counters show just the number
- "+ Add" opens an Add Counter dialog (similar to Add Reminder dialog)
- Long-press on a counter → context menu: Rename / Reset / Delete
- Free users: see a single "Pattern repeat" counter (existing behavior), "+ Add" shows Pro gate

### Add Counter dialog

```
┌──────────────────────────────────┐
│        ADD COUNTER               │
│                                  │
│  Name          [_Sleeve inc__]   │
│                                  │
│  ○ Count up   ● Repeating        │
│                                  │
│  Repeat every  [____8____]       │  ← only if Repeating
│                                  │
│  Step size     [____1____]       │
│                                  │
│        [Cancel]    [Save]        │
│                                  │
└──────────────────────────────────┘
```

### Free vs. Pro behavior

- **Free:** One counter ("Pattern repeat") — same as current behavior. This counter is auto-created when the user first taps "+ Add" in free mode (with Pro gate message for additional counters).
- **Pro:** Unlimited named counters with full controls.
- **Trial → expired:** Existing counters remain visible and functional (no data loss), but user cannot add new ones.

### ViewModel changes

`CounterViewModel` — add:
- `projectCounters: StateFlow<List<ProjectCounter>>` — all counters for current project
- `addCounter(name, repeatAt, stepSize)`
- `incrementCounter(counterId)`
- `decrementCounter(counterId)`
- `resetCounter(counterId)`
- `deleteCounter(counterId)`
- `renameCounter(counterId, newName)`

### Tests

- Unit tests for counter increment/decrement, repeat reset logic
- Unit tests for migration of existing secondaryCount data
- Unit tests for sort order management

---

## Database Migration Plan (v3 → v4)

All three features share a single Room migration. Use `AutoMigration` if possible (additive schema only — new tables, no column removals).

### New tables
1. `row_reminders` — RowReminderEntity
2. `progress_photos` — ProgressPhotoEntity
3. `project_counters` — ProjectCounterEntity

### Migration steps
1. Create three new tables
2. For each existing project with `secondaryCount > 0`: insert a `ProjectCounterEntity` with name="Pattern repeat", count=secondaryCount, stepSize=stepSize, repeatAt=stepSize

Step 2 requires data transformation, so this may need a manual `Migration(3, 4)` instead of `AutoMigration`. The new tables themselves can be auto-migrated, but the data backfill from `secondaryCount` needs code.

### Instrumented test
- `migrate3to4`: Verify new tables exist, verify secondaryCount data is correctly migrated to project_counters
- `migrate1to4`: Full chain migration test

---

## Pro Feature Summary (Updated)

After this expansion, the full Pro feature set — **all working on every device** — is:

| Feature | Trial hook strength | Used every session? |
|---------|-------------------|-------------------|
| Row Reminders | 🔥 Very high | Yes |
| Multiple Counters | 🔥 High | Yes (for complex projects) |
| Progress Photos | 🔥 High (emotional) | Frequently |
| Unlimited Projects | Medium | Only when starting 2nd project |
| Full Session History | Medium | Occasionally |
| Notes | Medium | Occasionally |
| Yarn Label OCR + Cards | Medium | Per new yarn |
| Home Screen Widget | Low-Medium | Passive |

**Nano-only features (bonus on supported devices):**

| Feature | Description |
|---------|-------------|
| Instruction Parser ×3 | Paste & parse knitting instructions |
| Yarn Label AI Parser | Enhanced OCR parsing |
| Project Summary | AI-generated project overview |

The top three daily-use features are now all device-independent. This should significantly improve conversion for the majority of users who don't have Nano-capable devices.

---

## Implementation Order

Recommended sequence for Claude Code:

1. **Database migration v3 → v4** — all three new entities + data migration for secondaryCount
2. **Row Reminders** — highest impact, most standalone
3. **Multiple Counters** — depends on migration being done, replaces existing pattern repeat
4. **Progress Photos** — can be done last as it involves file I/O and camera integration

Each feature can be implemented and tested independently after the migration is in place.

---

## Spec Document Updates Required

After implementation, update these existing spec documents:

| Document | Changes needed |
|----------|---------------|
| `knitting-toolkit-spec.md` | Add three new features to Counter Screen section, update Pro feature list, update Room schema to v4, add new DAOs |
| `knittools-design-spec-v4.md` | Add visual specs for reminder alert, photo strip, counter list |
| `PROJECT.md` | Update entity table, test counts, component list, monetization section |
| `GEMINI-NANO.md` | No changes needed (features are Nano-independent) |
