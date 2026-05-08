# KnitTools — "At the Same Time" Instruction Combiner

**Type:** Delta spec — new feature in Pattern Viewer  
**Status:** Spec ready for implementation  
**Depends on:** Pattern Viewer instruction display with Gemini (already implemented)  
**Pro only:** Yes  
**Key value:** Solves one of knitting's most frustrating problems. No competitor offers this.

---

## 1. What This Feature Does

The user is in Pattern Viewer looking at a page that contains overlapping instruction sections (typically signaled by "AT THE SAME TIME", "while continuing to", "and at the same time", or similar phrasing). They tap a menu option and AI reads the page, identifies the overlapping sections, and generates a single combined row-by-row list.

The result is a clear, sequential list the knitter can follow row by row without having to mentally merge two separate instruction blocks.

---

## 2. User Flow

1. User is in Pattern Viewer, sees a complex section with "AT THE SAME TIME"
2. User opens overflow menu (⋮ in top bar)
3. Taps **"Combine instructions"**
4. Bottom sheet opens with loading shimmer
5. Gemini receives the current page as an image and generates the combined list
6. Combined row-by-row list appears in the bottom sheet
7. User can scroll through the list while knitting
8. User dismisses the bottom sheet when done

### Overflow menu after this change:
```
┌─────────────────────┐
│ Combine instructions │
│ Jump to page...      │
│ Detach pattern       │
└─────────────────────┘
```

---

## 3. Prompt Design

```
You are a knitting pattern expert. Look at this pattern page and find any sections where multiple instructions happen simultaneously (indicated by phrases like "AT THE SAME TIME", "while continuing to", "meanwhile", or similar).

If overlapping instructions are found, combine them into a single row-by-row list. Each row should state everything that happens on that row.

Return a JSON object:
{
  "found": true/false,
  "title": "short description of what is being combined, e.g. 'Armhole + Neck Shaping'",
  "startRow": number (first row of the combined section),
  "rows": [
    {"row": 1, "side": "RS" or "WS" or null, "instruction": "combined instruction for this row"},
    {"row": 2, "side": "RS" or "WS" or null, "instruction": "..."},
    ...
  ]
}

If no overlapping instructions are found on this page, return:
{"found": false}

Rules:
- Number rows sequentially starting from the beginning of the combined section
- Mark each row as RS (right side) or WS (wrong side) if the pattern specifies
- When two things happen on the same row, join them clearly: "K2tog at beg (armhole dec) AND SSK at end (neck dec)"
- Preserve all stitch abbreviations exactly as written in the pattern
- Include stitch counts in parentheses where the pattern provides them
- If the pattern says "repeat row X", write out what that row actually is
- Do not add rows beyond what the pattern specifies
```

### Example response

```json
{
  "found": true,
  "title": "Armhole + Neck Shaping",
  "startRow": 1,
  "rows": [
    {"row": 1, "side": "RS", "instruction": "K2tog, K to last 2 sts, SSK (armhole dec both ends)"},
    {"row": 2, "side": "WS", "instruction": "Purl across"},
    {"row": 3, "side": "RS", "instruction": "K2tog, K to last 2 sts, SSK (armhole dec)"},
    {"row": 4, "side": "WS", "instruction": "Purl across"},
    {"row": 5, "side": "RS", "instruction": "K2tog, K12, BO 10, K12, SSK (armhole dec + neck BO)"},
    {"row": 6, "side": "WS", "instruction": "Purl to neck edge (work each side separately from here)"},
    {"row": 7, "side": "RS", "instruction": "K2tog at beg (armhole), K to last 2 before neck, SSK (neck dec)"},
    {"row": 8, "side": "WS", "instruction": "Purl across"},
    {"row": 9, "side": "RS", "instruction": "K2tog at beg (armhole), K to end (no neck dec this row)"},
    {"row": 10, "side": "WS", "instruction": "Purl across"},
    {"row": 11, "side": "RS", "instruction": "K to last 2 before neck, SSK (neck dec only, armhole decs complete)"},
    {"row": 12, "side": "WS", "instruction": "Purl across"}
  ]
}
```

---

## 4. Bottom Sheet UI

### Loading state:
```
┌──────────────────────────────────────┐
│  ── Combine Instructions             │
│                                      │
│  ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄  (shimmer)       │
│  ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄                  │
│  ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄                  │
└──────────────────────────────────────┘
```

### Result state:
```
┌──────────────────────────────────────┐
│  ── Armhole + Neck Shaping           │
│                                      │
│  Row 1 (RS)                          │
│  K2tog, K to last 2 sts, SSK        │
│  (armhole dec both ends)             │
│                                      │
│  Row 2 (WS)                          │
│  Purl across                         │
│                                      │
│  Row 3 (RS)                          │
│  K2tog, K to last 2 sts, SSK        │
│  (armhole dec)                       │
│                                      │
│  ...scrollable...                    │
└──────────────────────────────────────┘
```

### No overlapping instructions found:
```
┌──────────────────────────────────────┐
│  ── Combine Instructions             │
│                                      │
│  No overlapping instructions found   │
│  on this page.                       │
└──────────────────────────────────────┘
```

---

## 5. Styling

### Bottom sheet
- Modal bottom sheet (same pattern as Project Summary)
- Drag handle at top
- Scrollable content for long row lists

### Title
- The `title` from Gemini response (e.g. "Armhole + Neck Shaping")
- Font: `titleMedium`
- Color: `MaterialTheme.colorScheme.primary`

### Row entries
- Row number + side indicator as header
- Font: `labelSmall`, all caps, `MaterialTheme.colorScheme.onSurface` at 60% alpha
- Example: `ROW 5 (RS)`

### Instruction text
- Font: `bodyMedium`
- Color: `MaterialTheme.colorScheme.onSurface`
- Full text, no truncation (user needs to see everything)

### Row separator
- 8dp vertical spacing between rows
- Optional subtle divider line between row entries

### Current row highlight
- If the user's current counter row falls within the combined range, highlight that row entry with a subtle primary-tinted background
- This helps the knitter find where they are in the list

---

## 6. Long-Press to Copy

Long-press anywhere in the bottom sheet copies the entire combined list as plain text to clipboard. Format:

```
Armhole + Neck Shaping

Row 1 (RS): K2tog, K to last 2 sts, SSK (armhole dec both ends)
Row 2 (WS): Purl across
Row 3 (RS): K2tog, K to last 2 sts, SSK (armhole dec)
...
```

Snackbar: "Combined instructions copied"

This is valuable because the knitter might want to paste it into notes or print it.

---

## 7. Implementation

### 7.1 New method in GeminiAiService

```kotlin
data class CombinedRow(
    val row: Int,
    val side: String?,
    val instruction: String
)

data class CombinedInstructionResult(
    val found: Boolean,
    val title: String?,
    val startRow: Int?,
    val rows: List<CombinedRow>
)

suspend fun combineInstructions(pageBitmap: Bitmap): CombinedInstructionResult?
```

### 7.2 ViewModel additions

```kotlin
data class CombineState(
    val result: CombinedInstructionResult? = null,
    val isLoading: Boolean = false,
    val isVisible: Boolean = false
)

private val _combineState = MutableStateFlow(CombineState())
val combineState = _combineState.asStateFlow()

fun onCombineInstructionsTapped(pageBitmap: Bitmap) {
    viewModelScope.launch {
        _combineState.value = CombineState(isLoading = true, isVisible = true)
        val result = geminiAiService.combineInstructions(pageBitmap)
        _combineState.value = CombineState(
            result = result,
            isVisible = true
        )
    }
}

fun onCombineSheetDismissed() {
    _combineState.value = CombineState()
}
```

### 7.3 Caching

Cache the result per page number. If the user opens "Combine instructions" on the same page twice, the second time is instant.

```kotlin
private val combineCache = mutableMapOf<Int, CombinedInstructionResult>()
```

Cache cleared when pattern is detached or changed.

### 7.4 Quota

One AI call per unique page. Cached results don't count. This is an occasional action, not per-row, so quota impact is minimal.

---

## 8. Files to Modify

| File | Change |
|------|--------|
| GeminiAiService.kt | Add combineInstructions() multimodal method |
| PatternViewerScreen.kt | Add "Combine instructions" to overflow menu, add bottom sheet for result |
| PatternViewerViewModel.kt (or CounterViewModel) | Add combineState, onCombineInstructionsTapped(), cache |

## 9. Files NOT Modified

| File | Reason |
|------|--------|
| Room entities / migrations | No persistence — result is in-memory and bottom sheet |
| Navigation | No new routes — bottom sheet is inline |
| Other screens | Pattern Viewer only |
| Instruction display / explainer | Independent features, no interaction |

---

## 10. Edge Cases

| Scenario | Behavior |
|----------|----------|
| No "at the same time" on page | Bottom sheet shows "No overlapping instructions found on this page." |
| Multiple overlapping sections on one page | Gemini combines all of them into one list. Title reflects the most prominent section. |
| Instructions span across pages | v1 reads only the current page. If instructions continue on next page, the list ends where the page ends. Acceptable limitation. |
| Very long combined list (30+ rows) | Scrollable bottom sheet handles it. No truncation. |
| Pattern uses non-English "at the same time" phrasing | Gemini understands pattern context in multiple languages. "Gleichzeitig", "en même temps", "samalla" etc. |
| Pattern uses "while continuing to" instead of "at the same time" | Same concept — Gemini handles alternative phrasing. |
| User's current row is in the combined range | That row entry gets subtle highlight background |
| User's current row is outside the combined range | No highlight, full list still shown |
| Offline | Menu item still visible, tapping shows "Requires internet connection" in bottom sheet |
| Quota exhausted | "Monthly AI limit reached" in bottom sheet |

---

## 11. Token Usage

This is a heavier call because it sends a page image and expects a longer response:
- Input: ~1000 tokens (image) + ~200 tokens (prompt) = ~1200 tokens
- Output: ~300-800 tokens (depends on number of combined rows)
- Cost per call: still well under 1 cent

Used rarely — maybe once or twice per pattern, only when the knitter encounters an overlapping section. Negligible quota impact.

---

## 12. Testing

### Core functionality
- [ ] Page with "AT THE SAME TIME" → bottom sheet shows combined row list
- [ ] Page without overlapping instructions → "No overlapping instructions found"
- [ ] Combined list is logical and rows are numbered sequentially
- [ ] RS/WS markings are correct where pattern specifies

### UI
- [ ] Bottom sheet is scrollable for long lists
- [ ] Current row is highlighted if within range
- [ ] Long-press copies entire list to clipboard
- [ ] Dismiss bottom sheet clears state
- [ ] Second open on same page → instant (cache)

### Edge cases
- [ ] Pattern in non-English language → still detects and combines
- [ ] "While continuing to" phrasing → detected and combined
- [ ] Offline → error message in bottom sheet

---

## 13. Success Criteria

1. Correctly combines overlapping instructions into a row-by-row list
2. No competitor offers this — genuine differentiator for experienced knitters
3. Result is clear enough that a knitter can follow it without referring back to the original text
4. Accessible via one menu tap — does not clutter the normal Pattern Viewer experience
5. Copy to clipboard makes the result portable (paste into notes, print, etc.)
