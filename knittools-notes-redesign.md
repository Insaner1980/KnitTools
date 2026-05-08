# KnitTools — Notes Redesign

**Type:** Delta spec — UI improvement, no AI  
**Status:** Spec ready for implementation  
**Depends on:** Nothing — standalone UI work  
**Pro only:** No — notes are available to all users

---

## 1. Problem

Current notes are a small text field in a bottom sheet. Writing more than a few words is uncomfortable, and reading longer notes requires squinting at a tiny area. Notes are also only accessible from the Counter screen — there's no way to see or access them from the Project List.

## 2. What Changes

Three improvements:

1. **Bottom sheet stays as quick entry** — same as now, but with expand option
2. **Full-screen notes editor** — opens from bottom sheet, proper writing experience
3. **Notes indicator in Project List** — small icon showing which projects have notes

---

## 3. Bottom Sheet Improvements

### Current:
```
┌──────────────────────────────────────┐
│  ── NOTES                            │
│  ┌────────────────────────────────┐  │
│  │ Add note...                    │  │
│  │                                │  │
│  │                                │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
```

### New:
```
┌──────────────────────────────────────┐
│  ── NOTES                       ⛶   │
│  ┌────────────────────────────────┐  │
│  │ Add note...                    │  │
│  │                                │  │
│  │                                │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
```

Changes:
- Add expand icon (⛶) in top-right corner of bottom sheet, next to "NOTES" header
- Tapping expand icon navigates to full-screen notes editor
- Bottom sheet text field remains functional for quick edits
- Text syncs — whatever is written in bottom sheet appears in full editor and vice versa
- Bottom sheet auto-saves on dismiss (existing behavior, keep as-is)

### Expand icon styling
- Icon: `Icons.Outlined.OpenInFull` or similar expand/fullscreen icon
- Size: 20dp
- Color: `MaterialTheme.colorScheme.primary`
- Tappable area: 48dp minimum touch target

---

## 4. Full-Screen Notes Editor

A new screen that provides a comfortable writing and reading experience.

### Layout:
```
┌──────────────────────────────────────┐
│  ←  Notes — Project 4                │
├──────────────────────────────────────┤
│                                      │
│  Switched to smaller needles         │
│  (3.25mm instead of 3.5mm) because   │
│  the gauge was too loose.            │
│                                      │
│  Started the decrease section on     │
│  April 10. Using K2tog instead of    │
│  SSK for the right-leaning           │
│  decreases because I prefer the      │
│  look.                               │
│                                      │
│  Need to buy more yarn — almost out  │
│  of the main color. Check if the     │
│  dye lot is still available.         │
│                                      │
│  ▊                                   │
│                                      │
│                                      │
│                                      │
│                                      │
│                                      │
│                                      │
└──────────────────────────────────────┘
```

### Behavior
- Full-screen route within the Projects navigation graph
- Top bar: back arrow + "Notes — {project name}" (truncated with ellipsis if long)
- Single large TextField that fills the screen
- Scrollable vertically for long notes
- Keyboard appears when screen opens (auto-focus the text field)
- Auto-save: saves to Room on every pause in typing (debounced, ~1 second after last keystroke) and on back navigation
- No explicit save button needed — it just saves
- Bottom bar hidden (same as Pattern Viewer)

### Styling
- Text field has no visible border — just text on background
- Font: `bodyLarge` (bigger than bottom sheet for readability)
- Color: `MaterialTheme.colorScheme.onBackground`
- Background: `MaterialTheme.colorScheme.background` (full scaffold)
- Horizontal padding: 16dp
- Top padding: 16dp
- Placeholder text when empty: "Write your notes here..." in muted color

---

## 5. Notes Indicator in Project List

### On project cards that have notes:
A small notes icon appears on the project card, indicating that the project has notes content.

### Placement
- Right side of the project card, aligned with other metadata
- Same row as existing metadata (time, row count, etc.) or below project name
- Only visible when `notes` field is not null and not blank

### Icon
- Icon: `Icons.Outlined.StickyNote2` or `Icons.Outlined.Description` (small document icon)
- Size: 16dp
- Color: `MaterialTheme.colorScheme.onSurface` at 50% alpha (subtle, not distracting)

### Tap behavior
- Tapping the notes icon on a project card opens the full-screen notes editor directly for that project
- This is a shortcut — user doesn't have to open Counter first, then open the bottom sheet, then expand

### No notes
- Icon is not shown. No empty state indicator. Clean card.

---

## 6. Navigation

### New route
- `notes_editor/{projectId}` — full-screen notes editor

### Entry points
1. **Counter bottom sheet** → expand icon → navigates to `notes_editor/{projectId}`
2. **Project List** → notes icon on project card → navigates to `notes_editor/{projectId}`

### Back navigation
- From full-screen editor → back to wherever the user came from (Counter or Project List)
- Notes are auto-saved before navigation

---

## 7. Data

No Room changes needed. The `notes` field already exists on `CounterProjectEntity` as a `String?`. The full-screen editor reads and writes the same field.

### Auto-save implementation
```kotlin
// In NotesEditorViewModel or CounterViewModel
private var saveJob: Job? = null

fun onNotesChanged(text: String) {
    _notesText.value = text
    saveJob?.cancel()
    saveJob = viewModelScope.launch {
        delay(1000) // Debounce 1 second
        counterRepository.updateNotes(projectId, text)
    }
}

fun onBackPressed() {
    saveJob?.cancel()
    // Save immediately
    viewModelScope.launch {
        counterRepository.updateNotes(projectId, _notesText.value)
    }
}
```

---

## 8. Files to Create

| File | Purpose |
|------|---------|
| `NotesEditorScreen.kt` | Full-screen notes editor composable |

## 9. Files to Modify

| File | Change |
|------|--------|
| `Screen.kt` | Add `notes_editor/{projectId}` route |
| `NavGraph.kt` | Register new route, pass projectId |
| Notes bottom sheet composable | Add expand icon in header |
| Project List item composable | Add notes indicator icon with tap handler |
| `CounterViewModel` or new `NotesEditorViewModel` | Auto-save logic for full editor |

## 10. Files NOT Modified

| File | Reason |
|------|--------|
| Room entities / migrations | notes field already exists |
| Room DAOs | updateNotes method likely already exists |
| Settings | No new preferences |
| Other screens | Only Project List and Counter affected |

---

## 11. Edge Cases

| Scenario | Behavior |
|----------|----------|
| Empty notes, user opens full editor | Placeholder text shown, cursor active |
| User writes in bottom sheet, opens full editor | Same text appears — single source of truth |
| User writes in full editor, goes back to Counter | Bottom sheet shows updated text |
| User opens full editor from Project List | Editor loads notes for that project, back returns to Project List |
| User opens full editor from Counter | Back returns to Counter |
| Very long notes (thousands of characters) | Scrollable text field, no truncation |
| Process death while editing | Last auto-saved version is preserved (up to 1 second of loss) |
| User navigates away without explicit save | Auto-save fires on back press |
| Multiple projects have notes | Each project card shows its own notes icon |
| Project has notes that are only whitespace | Treated as empty — no icon shown, trimmed before check |

---

## 12. Testing

### Bottom sheet
- [ ] Expand icon visible in notes bottom sheet header
- [ ] Tapping expand opens full-screen editor with same text
- [ ] Writing in bottom sheet and then expanding shows updated text

### Full-screen editor
- [ ] Opens with existing notes pre-filled
- [ ] Keyboard appears automatically
- [ ] Text is readable and comfortable to write
- [ ] Scrolls for long notes
- [ ] Auto-saves after 1 second pause
- [ ] Back navigation saves and returns correctly
- [ ] Top bar shows project name

### Project List
- [ ] Notes icon visible on projects that have notes
- [ ] Notes icon NOT visible on projects without notes
- [ ] Tapping notes icon opens full-screen editor for that project
- [ ] Back from editor returns to Project List

### Data integrity
- [ ] Writing in full editor → close → open Counter bottom sheet → same text
- [ ] Writing in bottom sheet → open full editor → same text
- [ ] Kill app while editing → reopen → last auto-saved text preserved

---

## 13. Success Criteria

1. Long notes are comfortable to read and write in the full-screen editor
2. Quick notes in bottom sheet still work as before — no regression
3. Notes are accessible from Project List without opening Counter first
4. Auto-save means the user never has to think about saving
5. No Room migration needed — uses existing data field
