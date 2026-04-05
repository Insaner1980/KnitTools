# Project List — Management Features

**Scope:** Add project management actions to Project List screen: archive, delete, rename, reactivate. Two interaction methods: long-press context menu and swipe gestures.

---

## Project List Layout

The Project List screen (`project_list` route) shows two sections:

### ACTIVE section
- Header: "ACTIVE" labelSmall, all-caps, dusty rose color
- Cards for each active project showing:
  - Project name (titleMedium)
  - Current row count in gold + total session time (bodyMedium, secondary)
  - Linked yarn name if any (bodyMedium, muted)
- Tap → switches to this project and returns to Row Counter

### COMPLETED section
- Header: "COMPLETED" labelSmall, all-caps, dusty rose color
- Cards for each completed project showing:
  - Project name (titleMedium)
  - Total rows + completion date (bodyMedium, secondary)
- Tap (Pro) → opens read-only project view: all data visible (rows, history, linked yarn, notes) but counter buttons (+/−/undo) are disabled/hidden
- Tap (Free) → soft Pro prompt: "Unlock Pro to view completed projects" with gold CTA button. Do not block — show the prompt as a dialog or banner, not a full replacement screen.

---

## Long-press Context Menu

Long-press on any project card shows a dropdown or popup menu (M3 DropdownMenu):

### On active project:
- **Rename** — opens inline edit or small dialog with text field, pre-filled with current name
- **Archive** — moves project to Completed section. Sets `isCompleted = true`, records `completedAt` timestamp, stores `totalRows` from current count
- **Delete** — shows ConfirmationDialog: "Delete [project name]? This will permanently remove the project and all its history." Confirm button in error color (#D4725C). Deletes project, all sessions (CASCADE), and removes yarn card links.

### On completed project:
- **Rename** — same as above
- **Reactivate** — moves project back to Active section. Sets `isCompleted = false`, clears `completedAt` and `totalRows`. Project resumes from where it was.
- **Delete** — same confirmation dialog as above

---

## Swipe Gestures

### Swipe left on active project:
- Reveals archive action with gold/muted background and archive icon
- Swipe completes → archives immediately (same logic as long-press Archive)
- Short haptic feedback on action trigger

### Swipe left on completed project:
- Reveals delete action with error color (#D4725C) background and trash icon
- Swipe completes → shows ConfirmationDialog (same as long-press Delete)
- Does NOT delete without confirmation

### Implementation:
Use `SwipeToDismissBox` (M3) or equivalent. Single-direction swipe (start-to-end = left-to-right reveal). Background color and icon indicate the action. Animate card back into place if user doesn't complete the swipe.

---

## Data changes

### CounterProjectEntity updates needed:
- Ensure `isCompleted: Boolean` field exists (default false)
- Ensure `completedAt: Long?` field exists (nullable)
- Ensure `totalRows: Int?` field exists (nullable)
- On archive: set `isCompleted = true`, `completedAt = System.currentTimeMillis()`, `totalRows = currentRow`
- On reactivate: set `isCompleted = false`, `completedAt = null`, `totalRows = null`
- On delete: delete project entity. Sessions should CASCADE delete via foreign key.

### DAO queries needed:
- `getActiveProjects(): Flow<List<CounterProjectEntity>>` — where isCompleted = false, ordered by updatedAt desc
- `getCompletedProjects(): Flow<List<CounterProjectEntity>>` — where isCompleted = true, ordered by completedAt desc

---

## FAB

"+ New Project" button at the bottom of the screen or as a FloatingActionButton. Gold filled, cream text/icon. Creates project immediately with auto-generated name ("Project N" where N is next available number), navigates back to Row Counter with new project active.
