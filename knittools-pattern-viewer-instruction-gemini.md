# KnitTools — Pattern Viewer Instruction Display with Gemini

**Type:** Delta spec — add instruction display to simplified Pattern Viewer  
**Status:** Spec ready for implementation  
**Depends on:** Firebase AI Logic setup, Pattern Viewer simplification (already done)  
**Pro only:** Yes  
**Key feature:** First multimodal image feature in Pattern Viewer. Replaces broken OCR→Nano pipeline entirely.

---

## 1. What This Feature Does

When the user advances rows in Pattern Viewer (via bottom bar +/− controls), the app sends the current PDF page as an image to Gemini Flash Lite and asks: "What is the instruction for row X on this page?" The instruction appears in the bottom bar.

This replaces the old pipeline (PdfRenderer → bitmap → ML Kit OCR → text → Nano → instruction) with a single step (PdfRenderer → bitmap → Gemini → instruction). The OCR intermediate step is eliminated entirely.

**Why this is better:**
- OCR struggled with pattern formatting, columns, and fonts — Gemini sees the page as a human does
- Charts and visual patterns are now readable — OCR could not handle these at all
- Multi-language patterns work natively — no locale-specific regex needed
- One API call instead of two processing steps

---

## 2. Current State of Pattern Viewer

After simplification, the viewer has:
- Clean top bar: back arrow + pattern name + overflow menu (detach, jump to page)
- PDF rendering: full width, pinch-to-zoom with `transformable`, double-tap reset
- Bottom bar: Row number with +/− controls, page navigation with arrows
- No instruction display (removed during simplification, waiting for this spec)
- No highlight bar (removed during simplification)
- No annotation tools, no row mapping, no toolbar icons

This spec adds two things to the bottom bar: instruction text and an automatic highlight bar.

---

## 3. Prompt Design

Single prompt that returns both instruction and approximate position:

```
You are a knitting pattern reader. Look at this pattern page image and find the instruction for the requested row.

Return ONLY a JSON object:
{
  "instruction": "the exact instruction text for the row, or null if not found",
  "positionPercent": "estimated vertical position of the row on the page as a number 0-100, or null if not determinable"
}

Rules:
- Return the instruction exactly as written in the pattern, preserving abbreviations (K, P, SSK, K2tog, YO, etc.)
- If the row uses a reference like "repeat Row 2", resolve it if the referenced row is visible on this page
- If the instruction spans multiple lines, join them with a single space
- If the row number does not appear on this page, return {"instruction": null, "positionPercent": null}
- positionPercent 0 = top of page, 100 = bottom of page
- Do not add explanations or commentary
- Do not translate or modify abbreviations

Row number: {rowNumber}
```

The page image is sent alongside this prompt as a multimodal input.

### Example responses

**Row found with text instruction:**
```json
{
  "instruction": "K2, K2tog, YO, K2tog, K to end.",
  "positionPercent": 62
}
```

**Row found in chart section:**
```json
{
  "instruction": "K3, YO, SSK, K4, K2tog, YO, K3 (from chart)",
  "positionPercent": 45
}
```

**Row not on this page:**
```json
{
  "instruction": null,
  "positionPercent": null
}
```

---

## 4. Architecture

```
PDF page (PdfRenderer)
    ↓
Render page to Bitmap (existing — already done for display)
    ↓
Send image + row number to Gemini via GeminiAiService
    ↓
Parse JSON response → instruction text + position
    ↓
Cache result (in-memory, keyed by page + row)
    ↓
Display in bottom bar + position highlight bar on PDF
```

### 4.1 Bitmap for Gemini

The page is already rendered as a Bitmap for display. For Gemini, render at a resolution that is readable but not excessively large. A width of 1024-1536px is sufficient — large enough for Gemini to read text clearly, small enough to keep token usage reasonable.

If the display bitmap is already in this range, reuse it. If it's much larger (high-DPI display), render a separate lower-res bitmap for Gemini.

### 4.2 Caching

Two-level cache in ViewModel:

```kotlin
// Page image cache — avoid re-rendering bitmap for Gemini
private val pageBitmapCache = mutableMapOf<Int, Bitmap>()

// Instruction cache — avoid re-querying same row on same page
private val instructionCache = mutableMapOf<String, InstructionResult>()

// Cache key = "page:row" e.g. "2:7"
fun cacheKey(page: Int, row: Int) = "$page:$row"
```

Cache is cleared when:
- Pattern is detached
- Pattern is changed to a different PDF
- App process dies (in-memory only, no Room persistence)

### 4.3 Pre-fetching

When user advances to row N, pre-fetch row N+1 in the background (most likely next action). This makes the next tap feel instant if cache hits.

```kotlin
// On row change to N:
// 1. Show instruction for row N (from cache or fetch)
// 2. Background: also fetch row N+1 on same page
```

Cancel pre-fetch if user changes row again before it completes.

---

## 5. Bottom Bar Changes

### Current bottom bar (after simplification):
```
┌──────────────────────────────────────┐
│  Row 7   —   +     ← Page 2 of 3 →  │
└──────────────────────────────────────┘
```

### New bottom bar with instruction:
```
┌──────────────────────────────────────┐
│  Row 7   —   +     ← Page 2 of 3 →  │
│  K2tog, K2tog, K2tog, K1            │
└──────────────────────────────────────┘
```

### Loading state:
```
┌──────────────────────────────────────┐
│  Row 7   —   +     ← Page 2 of 3 →  │
│  ┄┄┄┄┄┄┄┄┄┄┄┄┄┄  (shimmer)         │
└──────────────────────────────────────┘
```

### No instruction found (or row 0, or free user, or offline):
```
┌──────────────────────────────────────┐
│  Row 7   —   +     ← Page 2 of 3 →  │
└──────────────────────────────────────┘
```
(Instruction row simply doesn't render — no error message in bottom bar)

### Instruction text styling
- Font: Outfit (same as app)
- Size: `bodySmall` (MaterialTheme.typography.bodySmall)
- Color: `MaterialTheme.colorScheme.onSurface` at 80% alpha
- Max lines: 2 (ellipsis on overflow)
- Horizontal padding: same as row controls above
- Top padding from row controls: 4dp
- Text alignment: start
- AnimatedVisibility with fade + slide-up when instruction appears, fade-out when it disappears
- Long-press to copy to clipboard, brief snackbar "Instruction copied"

### Shimmer loading
- Single line, 60% width of bottom bar
- Same height as bodySmall text
- Subtle alpha pulse (0.3 → 0.6 → 0.3), ~1.5s cycle

---

## 6. Highlight Bar

The highlight bar is a semi-transparent horizontal band drawn on top of the PDF at the estimated position of the current row.

### Behavior
- Position comes from Gemini response `positionPercent`
- If positionPercent is null, highlight bar is hidden
- The bar moves automatically when row changes (new Gemini response provides new position)
- No manual interaction — purely visual
- No gesture handling on the bar

### Visual
- Full width of the PDF image
- Height: ~20dp (thin band)
- Color: `MaterialTheme.colorScheme.primary` at 20% alpha (subtle burnt orange tint)
- Rendered as an overlay inside the PDF image Box, positioned at yPosition = positionPercent% of image height

### Implementation
```kotlin
// Inside the PDF image Box
if (positionPercent != null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .offset(y = (imageHeight * positionPercent / 100f).dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    )
}
```

Note: position calculation must account for current zoom level and pan offset so the bar stays aligned with the PDF content when zoomed.

---

## 7. Gating

```kotlin
val showInstruction = proState.hasFeature(ProFeature.AI_FEATURES)
    && aiQuotaManager.hasQuota()
```

When either condition is false:
- Instruction row in bottom bar does not render
- Highlight bar does not render
- No Gemini calls are made
- Viewer remains fully functional (PDF + row counter + page navigation)

No "upgrade" prompts inside the viewer. The viewer stays clean.

---

## 8. Implementation

### 8.1 New method in GeminiAiService

```kotlin
data class PatternInstructionResult(
    val instruction: String?,
    val positionPercent: Int?
)

suspend fun getPatternInstruction(
    pageBitmap: Bitmap,
    rowNumber: Int
): PatternInstructionResult?
```

### 8.2 ViewModel additions

Add to CounterViewModel (or PatternViewerViewModel if it exists separately):

```kotlin
data class InstructionDisplayState(
    val instruction: String? = null,
    val positionPercent: Int? = null,
    val isLoading: Boolean = false,
    val rowNumber: Int = 0
)

private val _instructionState = MutableStateFlow(InstructionDisplayState())
val instructionState = _instructionState.asStateFlow()
```

### 8.3 Trigger flow

Instruction fetch is triggered when:
1. Row changes (user taps +/−)
2. Page changes (user navigates to different page)
3. Pattern is first opened (if row > 0)

Not triggered when:
- Row is 0
- User is free / no quota
- Pattern is not attached

### 8.4 Rapid tap handling

When user taps + rapidly:
- Cancel any in-flight Gemini request
- Only the latest row's request completes
- Cache hits are instant (no cancellation needed)

```kotlin
private var fetchJob: Job? = null

fun onRowChanged(row: Int, page: Int) {
    fetchJob?.cancel()
    fetchJob = viewModelScope.launch {
        // Check cache first
        val cached = instructionCache[cacheKey(page, row)]
        if (cached != null) {
            _instructionState.value = InstructionDisplayState(
                instruction = cached.instruction,
                positionPercent = cached.positionPercent,
                rowNumber = row
            )
            return@launch
        }
        
        // Show loading
        _instructionState.value = InstructionDisplayState(isLoading = true, rowNumber = row)
        
        // Fetch from Gemini
        val result = geminiAiService.getPatternInstruction(pageBitmap, row)
        
        // Cache and display
        if (result != null) {
            instructionCache[cacheKey(page, row)] = result
            _instructionState.value = InstructionDisplayState(
                instruction = result.instruction,
                positionPercent = result.positionPercent,
                rowNumber = row
            )
        } else {
            _instructionState.value = InstructionDisplayState(rowNumber = row)
        }
        
        // Pre-fetch next row
        prefetchRow(row + 1, page)
    }
}
```

### 8.5 Quota counting

One AI call per unique page+row combination that is not cached. Pre-fetches also count. Counted on successful response only.

---

## 9. Files to Modify

| File | Change |
|------|--------|
| GeminiAiService.kt | Add getPatternInstruction() multimodal method |
| PatternViewerScreen.kt | Add instruction text row to bottom bar, add highlight bar overlay |
| PatternViewerViewModel.kt (or CounterViewModel) | Add instructionState, fetch/cache/prefetch logic |
| Bottom bar composable | Add instruction text with AnimatedVisibility, shimmer, long-press copy |

## 10. Files NOT Modified

| File | Reason |
|------|--------|
| PatternTextExtractor.kt | No longer needed for this flow — Gemini reads the image directly |
| PatternInstructionExtractor.kt | Replaced by GeminiAiService.getPatternInstruction() |
| Room entities / migrations | No schema changes — instruction state is in-memory only |
| Navigation | No new routes |
| Counter screen | Instruction display is Pattern Viewer only |

## 11. Files That Can Be Removed Later

| File | Reason |
|------|--------|
| PatternTextExtractor.kt | Was OCR step for Nano pipeline. Check if used elsewhere before deleting. |
| PatternInstructionExtractor.kt | Was Nano instruction extraction. Replaced by Gemini. |

---

## 12. Edge Cases

| Scenario | Behavior |
|----------|----------|
| Row 0 (no row set) | No Gemini call, no instruction, no highlight bar |
| Row not on current page | Gemini returns null → instruction hidden, highlight hidden |
| Page has only charts, no text | Gemini reads chart symbols and returns instruction from chart |
| Page has both text and chart | Gemini reads whichever contains the requested row |
| Pattern is image-only (photo/scan) | Gemini reads whatever is visible in the image |
| Very long instruction (multi-line) | Truncated to 2 lines with ellipsis. Long-press copies full text. |
| User rapidly taps + many times | Previous requests cancelled, only latest row shown |
| No internet | Instruction row hidden, no error in bottom bar. Viewer works normally. |
| Quota exhausted | Same as no internet — instruction row hidden silently |
| API error | Instruction row hidden for that row. Try again on next row change. |
| Pattern detached while viewing | Clear all caches. Return to Counter screen. |
| Pattern changed to different PDF | Clear all caches. Fresh fetch on next row change. |
| Process death | Caches lost. Row and page restored from DB. Instruction re-fetched. |
| Zoomed in on PDF | Highlight bar stays aligned with PDF content at correct position |
| Free user | No instruction row, no highlight bar, no Gemini calls |

---

## 13. Token Usage Estimate

Per call:
- Input: ~1000 tokens (image) + ~100 tokens (prompt text) = ~1100 tokens
- Output: ~50 tokens (JSON response)

With pre-fetching, an active session with 50 row changes across 3 page changes:
- Unique calls: ~55 (50 rows + 5 pre-fetches that were used)
- Total input: ~60K tokens
- Total output: ~3K tokens
- Cost on Gemini 2.5 Flash-Lite: ~$0.006 + ~$0.001 = less than 1 cent per session

---

## 14. Testing

### Core functionality
- [ ] Open Pattern Viewer with row > 0 → instruction appears in bottom bar
- [ ] Tap + → instruction updates to next row
- [ ] Tap − → instruction updates to previous row
- [ ] Instruction matches what is visible on the PDF page
- [ ] Row not on page → instruction hidden, no error
- [ ] Chart-only page → Gemini reads chart and returns instruction

### Highlight bar
- [ ] Highlight bar appears at approximately correct position on page
- [ ] Highlight bar moves when row changes
- [ ] Highlight bar stays aligned when zoomed in
- [ ] Highlight bar hidden when position unknown

### Performance
- [ ] Tap + rapidly 10 times → no crash, final row instruction shown
- [ ] Second visit to same row → instant (cache hit)
- [ ] Pre-fetch makes next row feel instant after first row loads

### Gating and errors
- [ ] Free user sees no instruction row and no highlight bar
- [ ] Offline → instruction row hidden, viewer works normally
- [ ] Firebase AI monitoring shows requests

### Interactions
- [ ] Long-press instruction → copied to clipboard
- [ ] Detach pattern → caches cleared, return to Counter
- [ ] Kill app → instruction re-fetches after reopen

---

## 15. Success Criteria

1. Instruction display works reliably — significantly better than old OCR→Nano pipeline
2. Chart-based patterns are readable (impossible with old pipeline)
3. Highlight bar provides useful visual feedback on the PDF
4. Feature feels fast — cache hits are instant, pre-fetch hides latency
5. Multimodal image pipeline through Firebase AI Logic is validated
