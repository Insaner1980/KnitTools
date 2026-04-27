# Row Counter Notes: Current Implementation Notes

This document records the current notes and journal-entry implementation before UX changes.

## Current UI Structure

### Row counter screen

The row counter screen has a notes icon in the compact project info area.

- File: `app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt`
- The state flag is `showNotesSheet`.
- The notes icon is only shown for Pro users.
- If the project already has notes, the icon tint uses the primary color; otherwise it uses `onSurfaceVariant`.

Relevant current flow:

```text
Row counter screen
-> notes icon
-> Notes bottom sheet
-> expand icon
-> full NotesEditorScreen
-> +AI
-> JournalEntryBottomSheet
-> Speak / Type
-> Add entry
```

### Notes bottom sheet in row counter

The row counter notes sheet is implemented directly in `CounterScreen.kt` as `NotesSheet`.

Current UI:

- `ModalBottomSheet`
- small uppercase `NOTES` label
- expand icon button in the top right
- one `TextField` showing/editing the full project notes
- placeholder string is `add_note`
- minimum text field height is controlled with `minLines = 6`

Important code references:

- `CounterScreen.kt:189`: `var showNotesSheet by rememberSaveable { mutableStateOf(false) }`
- `CounterScreen.kt:239`: `onExpandNotes = { state.projectId?.let(onNotesEditor) }`
- `CounterScreen.kt:290`: `onShowNotes = { showNotesSheet = true }`
- `CounterScreen.kt:375-376`: the sheet receives `showNotesSheet` and `state.notes`
- `CounterScreen.kt:1542-1548`: notes icon button and tint
- `CounterScreen.kt:1965-2007`: `NotesSheet` bottom sheet layout and text field

### Full notes editor screen

The full editor is a separate navigation destination:

- Route: `notes_editor/{projectId}`
- Screen: `NotesEditorScreen`
- ViewModel: `NotesEditorViewModel`

Navigation references:

- `Screen.kt:93-98`: `Screen.NotesEditor`
- `NavGraph.kt:245-251`: `NotesEditorScreen` destination
- `NavGraph.kt:192-193`: row counter can navigate to the editor

Current editor UI:

- top app bar with back button
- title uses the project name
- top-right text button with `+AI`
- full-screen `TextField` for project notes
- if `+AI` is pressed by a Pro user, `JournalEntryBottomSheet` opens

Important code references:

- `NotesEditorScreen.kt:88-104`: top-right `+AI` action
- `NotesEditorScreen.kt:115-141`: full-screen notes `TextField`
- `NotesEditorScreen.kt:145-160`: `JournalEntryBottomSheet` wiring

## Current Notes Persistence

### Row counter notes sheet

The row counter uses `CounterViewModel.setNotes`.

Current behavior:

- refuses to update notes if the user does not have `ProFeature.NOTES`
- updates UI state immediately
- launches a coroutine to persist the new text through `repository.updateProjectNotes`
- no debounce in this path

References:

- `CounterViewModel.kt:841-847`

```kotlin
fun setNotes(notes: String) {
    if (!proManager.hasFeature(ProFeature.NOTES)) return
    _uiState.update { it.copy(notes = notes) }
    viewModelScope.launch {
        val id = _uiState.value.projectId ?: return@launch
        repository.updateProjectNotes(id, notes)
    }
}
```

### Full notes editor

The full notes editor uses debounced autosave.

Current behavior:

- typing updates `NotesEditorViewModel` UI state immediately
- the database save is delayed by `DEBOUNCE_MS`
- `DEBOUNCE_MS` is currently `1000L`
- back navigation calls `saveImmediately`

References:

- `NotesEditorViewModel.kt:65-72`: debounced save
- `NotesEditorViewModel.kt:75-79`: immediate save
- `NotesEditorViewModel.kt:108`: `DEBOUNCE_MS = 1000L`
- `NotesEditorScreen.kt:60-63`: back handler saves immediately
- `NotesEditorScreen.kt:77-80`: top app bar back button saves immediately

Autosave race check result:

- The journal append uses the latest in-memory `state.notes`, not only the last persisted database value.
- `onNotesChanged` updates `_uiState` before the delayed save.
- `appendJournalEntry` reads `_uiState.value`.

Relevant lines:

- `NotesEditorViewModel.kt:66`: `_uiState.update { it.copy(notes = text) }`
- `NotesEditorViewModel.kt:70`: `delay(DEBOUNCE_MS)`
- `NotesEditorViewModel.kt:88`: `val state = _uiState.value`
- `NotesEditorViewModel.kt:97`: appends to `state.notes`

## Current Journal Entry / AI Flow

The current `+AI` button in the full notes editor opens `JournalEntryBottomSheet`.

Files:

- `app/src/main/java/com/finnvek/knittools/ai/journal/JournalEntryBottomSheet.kt`
- `app/src/main/java/com/finnvek/knittools/ai/journal/JournalEntryViewModel.kt`
- `app/src/main/java/com/finnvek/knittools/ai/journal/JournalEntryProcessor.kt`

### Bottom sheet UI

Current first screen:

- title: `journal_add_entry_title`
- button: `journal_mode_speak`
- button: `journal_mode_type`
- optional speech unavailable message

Speak mode:

- back arrow
- title: `journal_speak_title`
- large microphone pulse control
- listening/tap-to-speak helper text
- transcription preview box
- error text if present
- `Add entry` button

Type mode:

- back arrow
- title: `journal_type_title`
- `OutlinedTextField`
- error text if present
- `Add entry` button

Processing mode:

- centered progress indicator
- `journal_processing` text

Relevant references:

- `JournalEntryBottomSheet.kt:151-186`: mode switch
- `JournalEntryBottomSheet.kt:189-229`: mode selection UI
- `JournalEntryBottomSheet.kt:231-299`: speak UI
- `JournalEntryBottomSheet.kt:301-334`: type UI
- `JournalEntryBottomSheet.kt:336-351`: processing UI

### Journal entry processing

`JournalEntryViewModel.submit` chooses raw input based on current mode:

- `Speak` uses `partialSpeech.trim()`
- `Type` uses `typedText.trim()`
- blank input sets `journal_empty_error`
- non-blank input switches to `Processing`
- then it calls `processor.process(raw)`

References:

- `JournalEntryViewModel.kt:104-130`

The processor currently:

- trims the raw text
- checks Pro AI feature access
- checks quota
- builds a prompt
- calls `GeminiAiService.generateText(prompt)`
- post-processes the model response by trimming and removing surrounding quotes
- returns success only when cleaned output is non-blank
- otherwise returns fallback with the raw trimmed text

References:

- `JournalEntryProcessor.kt:37-63`
- `JournalEntryProcessor.kt:65-80`: prompt
- `JournalEntryProcessor.kt:82-87`: post-process

The prompt tells the model to format the text as a journal entry without changing meaning or rewording content. It specifically says to preserve the user's wording and only fix obvious punctuation/capitalization issues.

### Offline / API failure behavior

`GeminiAiService.generateText` returns `null` if the call fails, including no network.

References:

- `GeminiAiService.kt:31-35`
- `GeminiAiService.kt:95-110`

When the processor receives a blank/null AI result, it returns fallback:

- `JournalProcessResult.Fallback(trimmed, JournalProcessResult.Fallback.Reason.ApiError)`

Reference:

- `JournalEntryProcessor.kt:57-58`

The full notes editor still appends the text. If AI was not used, it shows a snackbar:

- quota exhausted -> `ai_quota_exhausted`
- otherwise -> `journal_offline_notice`

References:

- `NotesEditorScreen.kt:148-158`
- `strings.xml:196`: `AI formatting unavailable, entry saved as typed.`

## Current Journal Append Format

`NotesEditorViewModel.appendJournalEntry` appends journal entries to the bottom of the existing notes.

Current format:

```text
<localized short date> · Row <currentRow>

<cleanedText>
```

If notes already exist, the new block is appended after:

```text

---

```

If the current row is `0`, the row part is omitted.

References:

- `NotesEditorViewModel.kt:87-99`

Current implementation:

```kotlin
val state = _uiState.value
val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(LocalDate.now())
val rowPart = if (state.currentRow > 0) " · Row ${state.currentRow}" else ""
val header = "$date$rowPart"
val block = "$header\n\n$cleanedText"
val newNotes =
    if (state.notes.isBlank()) {
        block
    } else {
        "${state.notes.trimEnd()}\n\n---\n\n$block"
    }
onNotesChanged(newNotes)
```

The current row comes from the project count:

- `NotesEditorViewModel.kt:58`: `currentRow = project.count`

## Current Voice Command Add Note Path

This is separate from the `JournalEntryBottomSheet` flow.

The row counter voice command system can produce `AiVoiceAction.AddNote`.

When executed, it appends the note text to `state.notes` with only a newline separator. It does not add a date, row number, or `---` separator.

References:

- `CounterViewModel.kt:1540-1541`: parses `add_note`
- `CounterViewModel.kt:1700-1702`: executes `AddNote`
- `CounterViewModel.kt:1895-1899`: appends note text

Current implementation:

```kotlin
private fun handleAddNote(
    state: CounterUiState,
    action: AiVoiceAction.AddNote,
): String {
    val separator = if (state.notes.isNotBlank()) "\n" else ""
    setNotes(state.notes + separator + action.text)
    return context.getString(R.string.voice_note_saved)
}
```

## Product/UX Direction Discussed

The preferred direction discussed for the upcoming changes:

- Treat the feature as notes, not AI-generated content.
- Do not label entries as `AI-formatted`, `AI-generated`, `Dictated`, or `Written`.
- The user already knows whether they typed or dictated the note.
- Keep the notes content clean and readable.
- Use only a date header and the note text for inserted note blocks.
- Do not include row number by default.
- The row counter already has its own current row state, and that state is persisted with the project.

Preferred inserted note format:

```text
27.4.2026

Today I finished the sleeve and marked the decreases here.
```

With multiple entries:

```text
27.4.2026

Today I finished the sleeve and marked the decreases here.

---

26.4.2026

Remember to check the shaping before the next repeat.
```

Suggested UX simplification:

```text
Row counter
-> notes icon / Add note
-> one notes bottom sheet
-> user writes directly in the same notes field
-> microphone can add dictated text into the same notes field
```

Avoid this nested flow:

```text
Row counter
-> notes bottom sheet
-> expand to full editor
-> +AI
-> Add journal entry
-> Speak / Type
-> Add entry
```

## Main Open Design Questions

1. Should the row counter notes bottom sheet become the primary note editor?
2. Should the full-screen notes editor still exist, or only be an expanded version of the same editing experience?
3. Should dictated note insertion happen directly inside the notes sheet instead of through `JournalEntryBottomSheet`?
4. Should `JournalEntryProcessor` continue calling Gemini for punctuation/capitalization, or should speech-to-text output be inserted exactly as recognized?
5. Should all programmatic note appends use the same date-plus-note format?

