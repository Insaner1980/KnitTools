# KnitTools — Live Instruction Display in Pattern Viewer

**Type:** Delta spec — new feature addition to existing Pattern Viewer  
**Depends on:** v3-delta-spec (Pattern Viewer, CounterViewModel sync, Nano integration)  
**Pro only:** Yes (requires Gemini Nano + Pro)  
**Scope:** Pattern Viewer screen only — no changes to Counter screen

---

## 1. What This Feature Does

When the user advances rows in Pattern Viewer (via the bottom bar +/− controls), the app extracts the current row's instruction from the PDF text and displays it as a readable summary directly in the bottom bar. The user sees what to do on the current row without scanning the PDF themselves.

**User flow:**
1. User has a PDF pattern attached to a project
2. User opens Pattern Viewer
3. Bottom bar shows current row number + page indicator (existing behavior)
4. **New:** Below the row number, a compact instruction summary appears: e.g. `"K14, M1, (K15, M1) ×7"` or `"Knit across"` or `"Row 47: SSK, knit to last 3 sts, K2tog, K1"`
5. User taps +, row advances, instruction updates to the next row
6. If Nano cannot extract an instruction for this row, the instruction area is simply hidden — no error, no placeholder

**What this is NOT:**
- This is NOT a full pattern parser/compiler
- This does NOT create structured Room entities for pattern sections
- This does NOT change the Counter screen in any way
- This does NOT require the user to do row mapping first (though mapping improves highlight accuracy)

---

## 2. Architecture Overview

```
PDF page (PdfRenderer)
    ↓
Text extraction (PdfRenderer page → Bitmap → ML Kit Text Recognition)
    ↓
Page text cache (in-memory, per page, stored in ViewModel)
    ↓
Nano instruction extraction (E4B, prompted with page text + current row number)
    ↓
Instruction cache (in-memory Map<Int, String>, row number → instruction text)
    ↓
Bottom bar UI (instruction text displayed below row controls)
```

### Key architectural decisions

**Text extraction uses ML Kit, not PdfRenderer text APIs.** PdfRenderer does not expose text content — it renders pages as Bitmap. ML Kit Text Recognition (already a dependency for yarn label OCR) extracts text from the rendered bitmap. This is done once per page and cached.

**Nano does the instruction extraction, not regex.** Knitting patterns are too varied in format, language, and structure for reliable regex extraction. Nano E4B understands context: it knows what "Row 47" means even when the pattern says "Rnd 47:", "R47", "47.", or uses a completely different format. This is the core value proposition.

**No regex fallback for this feature.** Unlike InstructionParser (which has regex fallback for calculator field filling), this feature is Nano-only. If Nano is unavailable, the instruction display simply does not appear. The Pattern Viewer remains fully functional without it — it just doesn't show the instruction summary. This keeps the feature clean and avoids false confidence from bad regex matches on arbitrary pattern text.

**Caching is aggressive.** Text extraction is cached per page. Instruction extraction is cached per row number (within the current pattern). When the user taps +, the cache is checked first. Nano is only called for uncached rows.

---

## 3. Implementation Details

### 3.1 Text Extraction

**Where:** New utility class `PatternTextExtractor.kt` in `ai/` package.

**Input:** `PdfRenderer.Page` (already available in Pattern Viewer)

**Process:**
1. Render page to Bitmap at a resolution sufficient for OCR (300 DPI equivalent — the page is already rendered for display, but OCR may need a higher-res render)
2. Pass Bitmap to ML Kit `TextRecognizer` (same instance pattern as `YarnLabelScanner`)
3. Return extracted text as a single String, preserving line breaks
4. Cache result in a `MutableMap<Int, String>` keyed by page number (0-indexed)

**Important constraints:**
- Run on `Dispatchers.Default`, not Main
- Release Bitmap after OCR completes
- If OCR returns empty text, cache empty string (don't retry on every row change)
- Text extraction should happen when a page is first viewed, not on pattern attach (lazy)
- Maximum one concurrent text extraction — cancel previous if user rapidly flips pages

```kotlin
// Pseudocode for the cache contract
class PatternTextExtractor @Inject constructor(
    private val textRecognizer: TextRecognizer
) {
    private val pageTextCache = mutableMapOf<Int, String>()

    suspend fun getPageText(page: PdfRenderer.Page): String {
        pageTextCache[page.index]?.let { return it }
        val bitmap = renderPageForOcr(page)
        val text = recognizeText(bitmap)
        bitmap.recycle()
        pageTextCache[page.index] = text
        return text
    }

    fun clearCache() { pageTextCache.clear() }
}
```

### 3.2 Instruction Extraction via Nano

**Where:** New class `PatternInstructionExtractor.kt` in `ai/nano/` package.

**Input:** Page text (String) + current row number (Int)

**Output:** Instruction text (String?) — null if extraction fails or confidence is low

**Nano prompt design:**

```
You are a knitting pattern reader. Given the text content of a pattern page and a row number, extract the exact instruction for that specific row.

Rules:
- Return ONLY the instruction text for the requested row, nothing else
- If the row uses a reference like "repeat Row 2", resolve it if the referenced row is visible on this page
- Preserve knitting abbreviations exactly as written (K, P, SSK, K2tog, YO, etc.)
- If the row number does not appear on this page, return exactly: NOT_FOUND
- If the instruction spans multiple lines, join them with a single space
- Do not add explanations, commentary, or formatting
- Do not translate or modify abbreviations

Page text:
{pageText}

Row number: {rowNumber}
```

**Response handling:**
- If response is "NOT_FOUND" → return null
- If response is empty or garbage → return null
- If response is valid → cache it and return
- Trim whitespace, remove any leading "Row X:" prefix if Nano includes it (the UI already shows the row number)

**Caching:**
```kotlin
class PatternInstructionExtractor @Inject constructor(
    private val nanoAvailability: NanoAvailability
) {
    private val instructionCache = mutableMapOf<Int, String?>()

    suspend fun getInstruction(
        pageText: String,
        rowNumber: Int
    ): String? {
        if (!nanoAvailability.isAvailable()) return null
        instructionCache[rowNumber]?.let { return it }

        val instruction = queryNano(pageText, rowNumber)
        instructionCache[rowNumber] = instruction
        return instruction
    }

    fun clearCache() { instructionCache.clear() }
}
```

**Performance considerations:**
- Nano inference takes ~200-500ms on E4B — acceptable for a row change action
- Pre-fetch: when user advances to row N, also pre-fetch row N+1 in the background (the most likely next action)
- If Nano returns BUSY, do not retry immediately — show no instruction for this row change, try again on next

### 3.3 ViewModel Integration

**Where:** Existing `CounterViewModel` (already shared between Counter and Pattern Viewer)

**New state:**
```kotlin
// In CounterViewModel or a dedicated PatternViewerState
data class InstructionDisplayState(
    val instruction: String? = null,
    val isLoading: Boolean = false,
    val rowNumber: Int = 0
)

private val _instructionState = MutableStateFlow(InstructionDisplayState())
val instructionState: StateFlow<InstructionDisplayState> = _instructionState
```

**Trigger:** When `currentRow` changes AND Pattern Viewer is the active screen AND a pattern is attached:
1. Set `isLoading = true`
2. Get page text for current page (cached or extract)
3. Get instruction for current row (cached or Nano query)
4. Update state with result
5. Pre-fetch next row instruction in background

**Important:** Only trigger instruction extraction when the Pattern Viewer composable is active (composed). Do NOT trigger when the user is in Counter screen — waste of resources and Nano quota.

```kotlin
// Triggered by row change in Pattern Viewer context only
fun onRowChangedInViewer(newRow: Int, currentPage: Int) {
    viewModelScope.launch {
        _instructionState.value = InstructionDisplayState(
            isLoading = true,
            rowNumber = newRow
        )

        val pageText = patternTextExtractor.getPageText(currentPage)
        val instruction = patternInstructionExtractor.getInstruction(pageText, newRow)

        _instructionState.value = InstructionDisplayState(
            instruction = instruction,
            isLoading = false,
            rowNumber = newRow
        )

        // Pre-fetch next row
        launch {
            patternInstructionExtractor.getInstruction(pageText, newRow + 1)
        }
    }
}
```

**Cache lifecycle:**
- Clear both caches when pattern is detached or changed
- Clear instruction cache (but not text cache) when a different pattern page is navigated to via page jump (row numbers may not match across pages)
- Do NOT clear caches on screen rotation or tab switch (ViewModel survives these)

### 3.4 UI Changes — Bottom Bar

**Where:** Pattern Viewer bottom bar composable (existing)

**Current bottom bar layout:**
```
┌──────────────────────────────────────┐
│  [−]    Row 47    [+]    Page 3/12   │
└──────────────────────────────────────┘
```

**New bottom bar layout when instruction is available:**
```
┌──────────────────────────────────────┐
│  [−]    Row 47    [+]    Page 3/12   │
│  K14, M1, (K15, M1) ×7              │
└──────────────────────────────────────┘
```

**New bottom bar layout when instruction is loading:**
```
┌──────────────────────────────────────┐
│  [−]    Row 47    [+]    Page 3/12   │
│  ┄┄┄┄┄┄┄┄┄┄┄┄┄┄  (shimmer)         │
└──────────────────────────────────────┘
```

**New bottom bar layout when no instruction (Nano unavailable, free user, or NOT_FOUND):**
```
┌──────────────────────────────────────┐
│  [−]    Row 47    [+]    Page 3/12   │
└──────────────────────────────────────┘
```
(Identical to current — instruction row simply does not render)

**Instruction text styling:**
- Font: Outfit (same as app)
- Size: `bodySmall` (MaterialTheme.typography.bodySmall)
- Color: `MaterialTheme.colorScheme.onSurface` at 80% alpha (secondary emphasis)
- Max lines: 2 (with ellipsis — very long instructions are truncated)
- Horizontal padding: same as the row controls above
- Top padding from row controls: 4dp
- Text alignment: start (left-aligned)
- The instruction row uses `AnimatedVisibility` with a fade + slide-up entrance when instruction becomes available, and fade-out when it becomes null

**Loading shimmer:**
- Single line shimmer placeholder, 60% width of bottom bar
- Same height as bodySmall text
- Uses the app's existing shimmer/placeholder pattern if one exists, otherwise a simple animated alpha pulse (0.3 → 0.6 → 0.3) on a surface-colored rectangle
- Duration: subtle, ~1.5s cycle

**Long-press to copy:**
- Long-pressing the instruction text copies it to clipboard
- Show a brief snackbar: "Instruction copied"
- This is useful when the user wants to paste the instruction elsewhere

### 3.5 Interaction with Existing Features

**Row mapping:** The instruction display works independently of row mapping. Row mapping determines WHERE the highlight appears on the PDF. Instruction display determines WHAT instruction text to show. They are complementary but not dependent on each other.

**Row highlight:** No changes. The semi-transparent highlight band continues to work based on row mapping. The instruction display is purely additive in the bottom bar.

**Annotation mode:** Instruction display remains visible in annotation mode. No conflict.

**Counter screen sync:** When the user advances a row in Counter screen (not in viewer), the instruction is NOT pre-fetched. It will be fetched when the user returns to Pattern Viewer and the new row number triggers the extraction pipeline.

**Voice commands:** No changes. Voice commands in Counter screen advance the row. If the user then opens Pattern Viewer, the instruction for the new row will be shown.

**Camera scan patterns:** Works the same. Camera-scanned single-page PDFs will have their text extracted via OCR just like any other PDF page.

---

## 4. Pro/Free Gating

| Aspect | Free | Pro |
|--------|------|-----|
| Pattern Viewer | Yes | Yes |
| Bottom bar row controls | Yes | Yes |
| Row highlight (with mapping) | Yes | Yes |
| **Live instruction display** | **No** | **Yes** |

The instruction display requires both Pro AND Nano availability. The gating check is:

```kotlin
val showInstruction = proState.hasFeature(ProFeature.NANO_FEATURES) 
    && nanoAvailability.isAvailable()
```

When either condition is false, the instruction row simply does not render. No badge, no banner, no "upgrade to see instructions" prompt in the viewer itself. The viewer remains clean.

The Pro upgrade prompt for Nano features is already handled at a higher level (the existing Nano feature gating pattern).

---

## 5. Edge Cases

| Scenario | Behavior |
|----------|----------|
| Pattern has no text (image-only pages) | OCR extracts nothing → instruction hidden |
| Pattern uses charts instead of text | Nano returns NOT_FOUND → instruction hidden |
| Row number doesn't exist on current page | Nano returns NOT_FOUND → instruction hidden. Consider: could search adjacent pages, but v1 should NOT do this — keep it simple |
| Pattern is in a non-English language | Nano (Gemma 4, 140+ languages) handles this natively. The instruction is shown as-is in the original language |
| Very long instruction (multi-line) | Truncated to 2 lines with ellipsis. Long-press to copy full text |
| User rapidly taps + many times | Each tap cancels the previous Nano query (if still in flight) and starts a new one. Cache hits are instant. Only the latest row's instruction is shown |
| Nano quota exhausted (BUSY) | Instruction hidden for this row. No error message. Try again on next row change |
| Nano battery quota exceeded | Same as BUSY — instruction hidden, no error |
| Process death while viewer is open | On restore, current row and page are restored from ViewModel saved state. Instruction cache is lost — instruction will be re-fetched on next row change |
| Pattern detached while viewing | Clear all caches. Instruction disappears immediately |
| Pattern changed to a different PDF | Clear all caches. New text extraction begins for new pattern |

---

## 6. Files to Create

| File | Package | Purpose |
|------|---------|---------|
| `PatternTextExtractor.kt` | `ai/` | PDF page → OCR text extraction + cache |
| `PatternInstructionExtractor.kt` | `ai/nano/` | Page text + row → instruction via Nano + cache |

## 7. Files to Modify

| File | Change |
|------|--------|
| `CounterViewModel.kt` | Add `instructionState`, `onRowChangedInViewer()`, pre-fetch logic, cache lifecycle |
| Pattern Viewer bottom bar composable | Add instruction text row with AnimatedVisibility, shimmer loading state, long-press to copy |
| `di/` (Hilt module) | Provide `PatternTextExtractor` and `PatternInstructionExtractor` |

## 8. Files NOT Modified

| File | Reason |
|------|--------|
| Counter screen | This feature lives entirely in Pattern Viewer |
| Room entities | No new database tables — all state is in-memory cache |
| Room migrations | No schema changes |
| Navigation | No new routes |
| Settings | No new user preferences |
| Any calculator screen | Unrelated |

---

## 9. Testing

### Unit tests for PatternInstructionExtractor

| Test | Input | Expected |
|------|-------|----------|
| Simple row match | Page text with "Row 3: K2, P2 across", row=3 | "K2, P2 across" |
| Row not found | Page text with rows 1-10, row=25 | null |
| Empty page text | "", row=1 | null |
| Cache hit | Same row requested twice | Second call returns cached value, no Nano call |
| Cache cleared on pattern change | Change pattern, request same row | Nano called again |
| Nano unavailable | nanoAvailability returns false | null (no Nano call made) |
| Nano returns garbage | Random unrelated text | null |
| Pre-fetch | Advance to row 5 | Row 6 is also cached after a delay |

### Unit tests for PatternTextExtractor

| Test | Input | Expected |
|------|-------|----------|
| Valid page | Rendered PDF page bitmap | Non-empty text string |
| Cache hit | Same page requested twice | Second call returns cached, no OCR |
| Empty page | Blank PDF page | Empty string (cached, not retried) |
| Cache clear | clearCache() called | Next request triggers fresh OCR |

### Manual testing checklist

- [ ] Attach PDF pattern, open viewer, verify instruction appears for row 1
- [ ] Tap + several times, verify instruction updates each time
- [ ] Tap + rapidly 10 times, verify no crash and final row's instruction appears
- [ ] Navigate to a page with charts only, verify instruction gracefully hidden
- [ ] Test with pattern in Finnish/German/Japanese, verify instruction appears in original language
- [ ] Detach pattern, verify no stale instruction visible
- [ ] Kill app, reopen, verify instruction re-fetches correctly
- [ ] Test as free user, verify instruction row never appears
- [ ] Test on device without Nano support, verify instruction row never appears
- [ ] Long-press instruction text, verify copy to clipboard works
- [ ] Verify bottom bar does not grow too tall with 2-line instruction

---

## 10. What This Enables Later (Not in Scope Now)

These are explicitly NOT part of this spec but become possible once this foundation exists:

- **Voice read-aloud of current instruction** — TTS reads the instruction text when user says "read"
- **Instruction history** — show previous row's instruction in a collapsed state
- **Cross-page row lookup** — if row is not on current page, search adjacent pages
- **Structured pattern parsing** — full pattern compilation into Room entities (the ChatGPT "pattern engine" idea)
- **Auto-advance page** — when instructions run out on current page, auto-flip to next

None of these should be built now. This spec delivers the core value: see your current row's instruction without scanning the PDF.
