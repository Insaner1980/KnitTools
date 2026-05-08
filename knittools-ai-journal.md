# KnitTools — AI Knitting Journal

**Type:** Delta spec — new AI-powered journal entry system  
**Status:** Spec ready for implementation  
**Depends on:** Notes Redesign (full-screen notes editor), Gemini 2.5 Flash Lite via Firebase AI Logic (already implemented)  
**Pro only:** Yes  

---

## 1. What This Feature Is

A low-friction way for knitters to capture session notes. The user either speaks or types a quick note after a knitting session, and the AI lightly formats it into a dated journal entry appended to the project's notes. The user's original wording is preserved — AI only adds structure (timestamp, light formatting), not interpretation.

### Example

**User speaks or types:**
> "Tänään vaihdoin pienempiin puikkoihin 3.5mm:stä 3.25mm:iin koska gauge oli liian löysä, ja aloitin hihan kaventelun rivillä 47"

**AI adds to notes:**
```
14.4.2026 · Row 47

Tänään vaihdoin pienempiin puikkoihin 3.5mm:stä 3.25mm:iin koska gauge oli liian löysä, ja aloitin hihan kaventelun rivillä 47.
```

The original text is preserved verbatim. AI adds:
- Date header in user's locale format
- Current row number (from project state)
- Minor punctuation/capitalization cleanup if needed (especially for speech input where transcription may lack punctuation)
- A blank line separator between entries

---

## 2. Entry Points

### 2.1 Journal button in full-screen notes editor

In the full-screen notes editor (from Notes Redesign spec), add a new action button in the top bar.

```
┌──────────────────────────────────────┐
│  ←  Notes — Project 4       [+ AI]  │
├──────────────────────────────────────┤
│                                      │
│  (existing notes content)            │
│                                      │
```

- Icon + label: "+ AI" or similar (small text button with AI badge style)
- Tapping opens the journal entry dialog

### 2.2 Optional: Quick entry from Counter bottom sheet

In the notes bottom sheet, add a small AI button next to the existing expand icon. Same behavior as from the full editor — opens the journal entry dialog.

This is optional based on design preference — main entry point should be the full editor.

---

## 3. Journal Entry Dialog

A bottom sheet (or dialog) with two input modes:

```
┌──────────────────────────────────────┐
│                                      │
│  Add journal entry                   │
│                                      │
│  ┌────────────────────────────────┐  │
│  │ 🎤  Speak                      │  │
│  └────────────────────────────────┘  │
│  ┌────────────────────────────────┐  │
│  │ ⌨  Type                        │  │
│  └────────────────────────────────┘  │
│                                      │
│  Cancel                              │
└──────────────────────────────────────┘
```

Two buttons. Tapping one reveals the appropriate input UI in the same sheet.

### 3.1 Speak mode

```
┌──────────────────────────────────────┐
│                                      │
│  Speak your entry                    │
│                                      │
│  ┌────────────────────────────────┐  │
│  │                                │  │
│  │           🎤                   │  │
│  │                                │  │
│  │     Listening...               │  │
│  │                                │  │
│  └────────────────────────────────┘  │
│                                      │
│  [Transcribed text appears here      │
│   as you speak]                      │
│                                      │
│  Cancel              Add entry       │
└──────────────────────────────────────┘
```

- Uses Android SpeechRecognizer (same as Voice Commands v2)
- Shows partial transcription in real-time
- User taps stop (mic icon) or auto-stops after silence
- Transcribed text is shown for review before committing
- User can cancel or tap "Add entry" to commit

### 3.2 Type mode

```
┌──────────────────────────────────────┐
│                                      │
│  Type your entry                     │
│                                      │
│  ┌────────────────────────────────┐  │
│  │ What happened this session?    │  │
│  │                                │  │
│  │                                │  │
│  │                                │  │
│  └────────────────────────────────┘  │
│                                      │
│  Cancel              Add entry       │
└──────────────────────────────────────┘
```

- Standard text field, autofocus
- Placeholder: "What happened this session?" (localize)
- User types, taps "Add entry" to commit

---

## 4. AI Processing

When user commits (either mode), send the raw text to Gemini 2.5 Flash Lite with a specific prompt:

### System prompt

```
You receive a short note from a knitter about their current knitting session. Your job is to format it as a journal entry WITHOUT changing the meaning or rewording the content.

Rules:
1. PRESERVE the user's original wording. Do not paraphrase, summarize, or rewrite.
2. Fix obvious punctuation and capitalization issues (especially from speech-to-text).
3. Do NOT add new information the user did not provide.
4. Do NOT interpret emotions, guess intent, or add context.
5. If the text is in Finnish, keep it in Finnish. If English, keep it in English. Match the user's language.

Respond with ONLY the cleaned text. No preamble, no quotation marks.
```

### User prompt

```
{raw user text}
```

### Example transformations

| Input (speech) | Output |
|----------------|--------|
| "vaihdoin puikkoja pienemmäksi gauge oli liian löysä" | "Vaihdoin puikkoja pienemmäksi, gauge oli liian löysä." |
| "started decreasing at row 47 going well" | "Started decreasing at row 47. Going well." |
| "forgot to count stitches might need to redo row" | "Forgot to count stitches. Might need to redo row." |

The AI touches punctuation and capitalization only. No content changes.

---

## 5. Appending to Notes

Once AI returns the cleaned text, format it with a header and append to the project's existing notes:

### Entry format

```
{existing notes}

---

{day}.{month}.{year} · Row {currentRow}

{AI-cleaned text}
```

- Horizontal separator between entries
- Date format matches device locale (Finnish: 14.4.2026, US: 4/14/2026)
- "Row X" only included if the project has a counter with a current row (most projects do)
- If project has no current row (e.g., brand new, row=0), omit "· Row X"

### Entry ordering

Entries are appended chronologically — newest at the bottom. Rationale:
- When notes are short, user sees everything at once
- When notes are long, newest is easiest to find by scrolling to bottom
- Feels like a diary

Alternative (newest first) can be considered but the default is bottom-append.

---

## 6. Offline and Error Handling

| Scenario | Behavior |
|----------|----------|
| User offline | AI processing fails. Offer to save raw text as-is without formatting (just add date header, no AI cleanup). |
| Gemini API error | Same as offline — save raw text with date header. |
| AI quota exhausted | Same — save raw text with date header. Show brief message: "AI formatting unavailable, entry saved as typed." |
| User empty text | Show validation: "Entry cannot be empty." Don't call AI. |
| Speech recognizer fails | Show error, let user retry or switch to Type mode. |

The feature should NEVER lose user input. Even if AI fails, the raw text gets saved with a timestamp.

---

## 7. Quota Counting

Journal entries use the existing AI quota system (same as project summaries, yarn label parsing, etc.). No new quota dimension needed. Each entry = 1 AI call.

Counting:
- Tier: same as other text-based AI features
- Typical entry: ~100-300 tokens input, ~100-300 tokens output
- Cost per entry: fraction of a cent

---

## 8. UI Polish

### AI badge

The "+ AI" button in the notes editor top bar uses the plain "AI" text badge style (per established KnitTools convention — no sparkle/star icons).

### Loading state

While AI is processing (typically 1-2 seconds), show a subtle loading indicator in the dialog. Don't dismiss the sheet until AI returns.

### Success feedback

After entry is added:
- Close the journal entry dialog
- Notes editor scrolls to bottom to show the new entry
- Brief haptic feedback (if haptics enabled)

---

## 9. Files to Create

| File | Purpose |
|------|---------|
| `JournalEntryBottomSheet.kt` | Bottom sheet with Speak/Type mode selection and input UIs |
| `JournalEntryViewModel.kt` | Handles speech recognition, AI call, and notes append |
| `JournalEntryProcessor.kt` | AI prompt building and response handling |

## 10. Files to Modify

| File | Change |
|------|--------|
| `NotesEditorScreen.kt` | Add "+ AI" button in top bar that opens JournalEntryBottomSheet |
| Notes bottom sheet composable | Optionally add journal entry button next to expand icon |
| `CounterRepository.kt` or `NotesRepository.kt` | Add `appendJournalEntry(projectId, formattedEntry)` method |
| `GeminiAiService.kt` | Add journal entry prompt method (or reuse existing text-completion method) |
| Pro gating | Journal entry is Pro-only. Free users see upgrade prompt on tapping "+ AI". |

## 11. Files to Leave Alone

| File | Reason |
|------|--------|
| Room entities | Uses existing `notes` String field on CounterProjectEntity |
| Voice Commands v3 / Live API | Journal entry uses simpler SpeechRecognizer — no need for Flash Live complexity here |
| Other AI features | Independent feature |

---

## 12. Edge Cases

| Scenario | Behavior |
|----------|----------|
| User adds entry with no current row | Header shows only date, no "Row X" |
| User adds entry to project with no existing notes | Entry becomes the first content in notes. No leading separator. |
| User adds multiple entries same day | Each entry gets its own date header. Chronological ordering. |
| User speaks in Finnish | AI detects language, cleans Finnish text without translating. |
| User mixes languages | AI keeps mixed text as-is, only fixes punctuation. |
| User adds a very short note ("done") | Still valid. Gets date header + "done." |
| User adds a very long note (500+ words) | Still processed normally. No truncation. |
| User edits notes manually after AI entry | Manual edits are preserved. AI entries are just text after creation. |
| User deletes an AI entry by editing notes | No AI tracking — it's just text, fully user-editable. |

---

## 13. Testing

### Core flow
- [ ] "+ AI" button visible in notes editor top bar (Pro users)
- [ ] Speak mode: transcription works, AI formats, entry appended
- [ ] Type mode: text accepted, AI formats, entry appended
- [ ] Date header uses device locale format
- [ ] Row number included when project has current row
- [ ] Row number omitted when row = 0

### AI behavior
- [ ] Finnish speech input → Finnish output, no translation
- [ ] English speech input → English output
- [ ] Speech with missing punctuation → AI adds punctuation
- [ ] Typed text with correct punctuation → AI leaves mostly unchanged
- [ ] AI does NOT add interpretation or emotional language
- [ ] AI does NOT change the meaning of the user's words

### Error handling
- [ ] Offline → entry saved with date header, no AI cleanup
- [ ] Quota exhausted → entry saved with date header, message shown
- [ ] Empty input → validation error, no AI call
- [ ] SpeechRecognizer error → user can retry or switch to Type mode

### Integration
- [ ] New entry appears at bottom of notes
- [ ] Previous entries preserved, not overwritten
- [ ] Notes editor scrolls to new entry after add
- [ ] Multiple entries same day → separate date headers
- [ ] Entry appears in Counter bottom sheet notes preview too

### Pro gating
- [ ] Free user sees "+ AI" button → tap shows upgrade prompt
- [ ] Pro user has full access

---

## 14. Success Criteria

1. Adding a journal entry is faster than typing into the notes editor directly
2. User's words are preserved — they recognize their own language in the entry
3. AI adds structure (date, row, punctuation) without changing meaning
4. Offline fallback never loses user input
5. Feature integrates with existing notes — not a separate system
6. Pro users get value; free users see what they're missing

---

## 15. Future Considerations

Deferred for future work, NOT part of this spec:
- Auto-prompt after session ends ("You knit 15 rows in 25 minutes. Add a note?")
- Entry search/filter within a project
- Entry tags (milestone, problem, inspiration)
- Cross-project journal view (all entries across all projects chronologically)
- Export journal as PDF or markdown

These can be added later if the basic journal proves valuable.
