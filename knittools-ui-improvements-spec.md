# KnitTools UI Improvements — Counter Screen, Overflow Menus, Multi-Select

Delta spec for Counter screen redesign, overflow menus on Counter and Project List screens, multi-select project management, and Completed project visibility toggle.

---

## 1. Counter Screen Redesign

### Project Card — Compact Two-Row Layout
Consolidate all project metadata into a single compact card:

**Row 1:** Yarn info + actions
- Linked yarn(s) with color dot, yarn name, × remove button
- "+ Add yarn" link (Primary color)
- Notes icon (right-aligned)

**Row 2:** Stats + AI summary
- Stitches count (e.g. "57 st") — left side, TextMuted color
- Session time (e.g. "00:52") — left side, TextMuted color, tappable → Session History
- "View AI summary" link — right side, DustyRose color

Card uses `Surface` background, 14dp corner radius, 10dp vertical / 14dp horizontal padding.

### Counter Number
- Unchanged: large centered number (120sp Bold), "CURRENT ROW" label above in Secondary color.
- **More breathing room** — remove all elements from between counter and buttons that aren't essential.

### Pattern Repeat / Multiple Counters
- Positioned between counter number and main buttons.
- Compact inline layout: small −/+ circles (24dp, SurfaceHigh) flanking label text.
- Text style: 11sp, fontWeight 600, letterSpacing 1, TextMuted color.
- Example: `[−]  PATTERN REPEAT: 9  [+]`

### Main Buttons
- Unchanged: minus (48dp) / plus (72dp wooden button) / undo (48dp).
- **Nothing below the buttons.** Stats and Reset Counter removed from this area.

### Stats Relocation
- Stitches count and session time moved into the project card (row 2).
- Session time remains tappable → navigates to Session History.

### Reset Counter Relocation
- Removed from main screen surface.
- Moved to overflow menu (see section 2).

---

## 2. Counter Screen Overflow Menu

### Three-dot icon in TopAppBar
Add a vertical three-dot icon (⋮) to the right side of the Counter screen TopAppBar, next to the existing camera icon.

**Icon placement:** Camera icon first, then three-dot icon, both right-aligned.

### Menu Items (in order)

| Item | Action | Style |
|------|--------|-------|
| Session History | Navigate to Session History screen | Normal |
| Rename Project | Open Rename dialog (AlertDialog + TextField) | Normal |
| Complete Project | Mark project as completed, navigate back to Project List | Normal, confirmation dialog |
| Reset Counter | Reset row count to 0 | Normal, confirmation dialog |
| Delete Project | Delete project permanently | Error color text, confirmation dialog |

### Confirmation Dialogs
- **Complete Project:** "Complete [project name]? The project will be moved to your completed projects."
  - Confirm button: Primary color, text "Complete"
  - Cancel button: text-only
- **Reset Counter:** "Reset counter to 0? This cannot be undone."
  - Confirm button: Error color, text "Reset"
  - Cancel button: text-only
- **Delete Project:** "Delete [project name]? This cannot be undone."
  - Confirm button: Error color, text "Delete"
  - Cancel button: text-only

### Implementation
- Use M3 `DropdownMenu` anchored to the three-dot icon.
- Delete item uses `MaterialTheme.colorScheme.error` for text color.

---

## 3. Project List Overflow Menu

### Three-dot icon in Project List TopAppBar
Add a vertical three-dot icon to the right side of the Project List screen header area.

### Menu Items

| Item | Action |
|------|--------|
| Select Projects | Activate multi-select mode |
| Sort by | Submenu: Name / Last Updated / Created Date |
| Show Completed | Toggle visibility of Completed section |

### Show Completed Toggle
- **Default: off** — only Active projects visible.
- When toggled on, Completed section appears below Active section with "COMPLETED" section label.
- Preference persisted in DataStore (`showCompletedProjects: Boolean`, default `false`).
- Menu item shows checkmark when enabled.

### Sort Order
- Options: Name (A–Z), Last Updated (newest first, default), Created Date (newest first).
- Persisted in DataStore (`projectSortOrder: String`, default `"updated"`).
- All three options affect both Active and Completed sections.

---

## 4. Multi-Select Mode

### Activation
Two entry points:
1. **Overflow menu → "Select Projects"**
2. **Long-press any project card** — activates multi-select and selects that project.

Note: Long-press currently shows context menu (Rename/Archive/Delete). When multi-select is implemented, long-press activates multi-select instead. Single-project actions move to a context menu accessible via the Counter screen overflow or a tap-and-hold in multi-select mode.

### Multi-Select UI

**Project cards in multi-select mode:**
- Checkbox appears on the left side of each project card.
- Tapping a card toggles its checkbox (does not navigate to Counter).
- Selected cards get a subtle highlight (Primary color at 5-10% opacity overlay).

**Top bar changes:**
- Title changes to "[N] selected" (e.g. "3 selected").
- Left: X (close) button to exit multi-select mode.
- Right: "Select All" text button.

**Bottom action bar:**
- Slides up from bottom when multi-select is active.
- Background: SurfaceHigh.
- Buttons: "Complete" (Primary color) and "Delete" (Error color).
- Both require confirmation dialog.

### Confirmation Dialogs (Multi-Select)

- **Complete N projects:** "Complete [N] projects? They will be moved to your completed projects."
  - Confirm: Primary color, "Complete"
- **Delete N projects:** "Delete [N] projects? This cannot be undone."
  - Confirm: Error color, "Delete"

### Exit Multi-Select
- X button in top bar.
- Back gesture/button.
- After completing an action (complete/delete) — automatically exits.

---

## 5. Summary of Removed/Changed Behaviors

| Before | After |
|--------|-------|
| Stats row (stitches + time) below buttons in Counter | Moved to project card row 2 |
| Reset Counter text below stats in Counter | Moved to Counter overflow menu |
| Long-press project card → context menu (Rename/Archive/Delete) | Long-press → activates multi-select |
| "Archive" terminology | Changed to "Complete" |
| Completed projects always visible in Project List | Hidden by default, toggle in overflow menu |
| No way to complete/delete/rename project from Counter screen | Available via Counter overflow menu |
| No multi-select in Project List | Multi-select via overflow or long-press |

---

## 6. Implementation Notes

- Counter screen overflow: add `DropdownMenu` state to `CounterScreen` or `CounterViewModel`.
- Project List overflow: add to `ProjectListScreen`.
- Multi-select state: manage in `CounterViewModel` (which is scoped to the Projects nav graph and shared between ProjectList and Counter).
- DataStore additions: `showCompletedProjects` (Boolean), `projectSortOrder` (String).
- Room: no schema changes needed — `isCompleted` field already exists on `CounterProjectEntity`.
- Rename dialog: reuse existing `ConfirmationDialog` or `AlertDialog` pattern.
