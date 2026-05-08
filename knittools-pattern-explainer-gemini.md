# KnitTools — Pattern Explainer

**Type:** Delta spec — new feature addition to Pattern Viewer  
**Status:** Spec ready for implementation  
**Depends on:** Pattern Viewer instruction display (must be working first)  
**Pro only:** Yes  
**Interaction:** Tap instruction text → explanation slides in below

---

## 1. What This Feature Does

When the instruction text is visible in the Pattern Viewer bottom bar, the user can tap it to get a plain-language explanation of what the abbreviations and techniques mean.

This is useful for knitters who encounter unfamiliar abbreviations, complex stitch combinations, or techniques they haven't used before.

---

## 2. User Flow

1. Bottom bar shows instruction: `K2tog, YO, SSK, K to end`
2. User taps the instruction text
3. Loading shimmer appears below the instruction
4. Explanation slides in: `"Knit 2 together (right-leaning decrease), yarn over (creates a hole), slip slip knit (left-leaning decrease), then knit to the end of the row."`
5. User taps instruction text again → explanation hides
6. User taps +/− to change row → explanation hides automatically, new instruction appears

### What does NOT happen
- No new buttons, icons, or menu items
- No separate screen or dialog
- No popup or tooltip

---

## 3. Prompt Design

```
You are a knitting instructor explaining a pattern instruction to someone who knows basic knitting but may not recognize all abbreviations.

Explain this knitting instruction in plain language. Go through each part in order. For each abbreviation, state what it means and briefly what the knitter does physically. Keep it concise — one sentence per abbreviation or short group. Do not repeat the original instruction at the start.

Respond in English.

Instruction: {instructionText}
```

### Example inputs and outputs

**Input:** `K2tog, YO, SSK, K to end`
**Output:** `Knit 2 together (right-leaning decrease), yarn over (creates a hole), slip slip knit (left-leaning decrease), then knit to the end of the row.`

**Input:** `*K3, P2; rep from * to last 3 sts, K3`
**Output:** `Repeat this sequence across the row: knit 3, purl 2. Keep repeating until 3 stitches remain, then knit those 3.`

**Input:** `Sl1 wyif, K to end`
**Output:** `Slip 1 stitch purlwise with yarn in front (creates a neat selvedge edge), then knit to the end of the row.`

**Input:** `Row 15: Knit across`
**Output:** `Knit every stitch across the entire row.`

---

## 4. Bottom Bar Layout

### Instruction visible, explanation closed (default):
```
┌──────────────────────────────────────┐
│  Row 7   —   +     ← Page 2 of 3 →  │
│  K2tog, YO, SSK, K to end     ← tappable
└──────────────────────────────────────┘
```

### Instruction visible, explanation loading:
```
┌──────────────────────────────────────┐
│  Row 7   —   +     ← Page 2 of 3 →  │
│  K2tog, YO, SSK, K to end           │
│  ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄  (shimmer)      │
└──────────────────────────────────────┘
```

### Instruction visible, explanation open:
```
┌──────────────────────────────────────┐
│  Row 7   —   +     ← Page 2 of 3 →  │
│  K2tog, YO, SSK, K to end           │
│  Knit 2 together (right-leaning     │
│  decrease), yarn over (creates a     │
│  hole), slip slip knit (left-leaning │
│  decrease), then knit to the end of  │
│  the row.                            │
└──────────────────────────────────────┘
```

### No instruction (row not found, free user, offline):
```
┌──────────────────────────────────────┐
│  Row 7   —   +     ← Page 2 of 3 →  │
└──────────────────────────────────────┘
```
(Nothing to tap — feature is invisible)

---

## 5. Styling

### Instruction text (existing, add tap indication)
- Same styling as instruction display spec
- Add `clickable` modifier
- No visual change to indicate tappability — the text itself is the affordance
- When explanation is open, instruction text gets slightly brighter or bolder to show it's "active" (optional, visual-first decision)

### Explanation text
- Font: Outfit
- Size: `bodySmall` (same as instruction)
- Color: `MaterialTheme.colorScheme.onSurface` at 60% alpha (more muted than instruction)
- Max lines: 6 (explanation can be longer than instruction)
- Overflow: ellipsis (very complex instructions may truncate — acceptable)
- Top padding from instruction: 4dp
- AnimatedVisibility with fade + expand vertically
- Background: subtle surface tint behind explanation to visually separate it from instruction

### Shimmer
- Same style as instruction loading shimmer
- 2 lines instead of 1 (explanation is longer)

---

## 6. Caching

Explanation is cached per instruction text (not per row number, because different rows can have the same instruction like "Knit across"):

```kotlin
private val explanationCache = mutableMapOf<String, String>()
```

If the user taps the same instruction again after closing, the cached explanation appears instantly without a Gemini call.

Cache is cleared when pattern is detached or changed (same lifecycle as instruction cache).

---

## 7. Long-Press Copy Migration

Currently long-press on instruction text copies to clipboard. Since tap is now used for explanation:

- **Tap** = toggle explanation
- **Long-press** = copy instruction to clipboard (keep existing behavior unchanged)

Both gestures coexist on the same text. This is standard Android behavior (tap for primary action, long-press for secondary).

If explanation is open, long-press still copies the original instruction text, not the explanation.

To copy the explanation text: user can long-press the explanation text itself. Same snackbar "Copied to clipboard".

---

## 8. Implementation

### 8.1 New method in GeminiAiService

```kotlin
suspend fun explainInstruction(instruction: String): String?
```

Simple text-in, text-out call. No image needed — the instruction text is already extracted.

### 8.2 ViewModel additions

```kotlin
data class ExplanationState(
    val explanation: String? = null,
    val isLoading: Boolean = false,
    val isVisible: Boolean = false,
    val forInstruction: String = ""
)

private val _explanationState = MutableStateFlow(ExplanationState())
val explanationState = _explanationState.asStateFlow()

fun onInstructionTapped(instructionText: String) {
    if (_explanationState.value.isVisible 
        && _explanationState.value.forInstruction == instructionText) {
        // Toggle off
        _explanationState.value = ExplanationState()
        return
    }
    
    // Check cache
    val cached = explanationCache[instructionText]
    if (cached != null) {
        _explanationState.value = ExplanationState(
            explanation = cached,
            isVisible = true,
            forInstruction = instructionText
        )
        return
    }
    
    // Fetch
    viewModelScope.launch {
        _explanationState.value = ExplanationState(
            isLoading = true,
            isVisible = true,
            forInstruction = instructionText
        )
        val result = geminiAiService.explainInstruction(instructionText)
        if (result != null) {
            explanationCache[instructionText] = result
            _explanationState.value = ExplanationState(
                explanation = result,
                isVisible = true,
                forInstruction = instructionText
            )
        } else {
            _explanationState.value = ExplanationState()
        }
    }
}

fun onRowChanged() {
    // Hide explanation when row changes
    _explanationState.value = ExplanationState()
}
```

### 8.3 Quota

Each explanation counts as 1 AI call. Cached explanations do not count. Identical instructions across different rows share the same cache entry.

---

## 9. Files to Modify

| File | Change |
|------|--------|
| GeminiAiService.kt | Add explainInstruction() method |
| PatternViewerScreen.kt bottom bar | Add tap handler on instruction text, add explanation row with AnimatedVisibility |
| PatternViewerViewModel.kt (or CounterViewModel) | Add explanationState, onInstructionTapped(), cache |

## 10. Files NOT Modified

| File | Reason |
|------|--------|
| Room entities / migrations | No persistence needed |
| Navigation | No new routes |
| Other screens | Pattern Viewer only |

---

## 11. Edge Cases

| Scenario | Behavior |
|----------|----------|
| Instruction is "Knit across" (very simple) | Explanation is brief: "Knit every stitch across the entire row." Still useful for beginners. |
| Instruction is very long (2 lines truncated) | Explanation explains the full instruction. Gemini receives the complete text even if UI truncates. |
| Instruction is null / not shown | Nothing to tap — feature is invisible |
| Explanation is loading, user taps again | Cancel and hide |
| Explanation is loading, user changes row | Cancel, hide, new instruction appears |
| Explanation is open, user changes row | Hide explanation, show new instruction |
| Explanation is open, user taps instruction | Hide explanation |
| No internet | Explanation doesn't appear, instruction tap does nothing visible |
| Quota exhausted | Same as no internet |
| Same instruction on multiple rows | Cache hit — explanation appears instantly |

---

## 12. Token Usage

Very lightweight:
- Input: ~100 tokens (prompt + instruction text)
- Output: ~50-100 tokens (explanation)
- Cost per explanation: fraction of a cent

Users won't explain every row — this is an occasional "what does this mean?" action. Estimated 5-10 times per session at most.

---

## 13. Testing

- [ ] Tap instruction text → shimmer → explanation appears below
- [ ] Tap instruction again → explanation hides
- [ ] Change row → explanation hides, new instruction appears
- [ ] Tap same instruction on different row → instant (cache hit)
- [ ] Long-press instruction → copies to clipboard (not affected by tap feature)
- [ ] Long-press explanation → copies explanation to clipboard
- [ ] Very simple instruction ("Knit across") → still produces useful explanation
- [ ] Complex instruction with repeats → explanation walks through each part
- [ ] Offline → tap does nothing visible
- [ ] Free user → no instruction shown, nothing to tap

---

## 14. Success Criteria

1. Tapping instruction is intuitive — no UI clutter, no new buttons
2. Explanations are accurate and concise
3. Abbreviations are explained with physical action (what the knitter does)
4. Cache makes repeated taps instant
5. Feature is invisible when not needed (no instruction = nothing to tap)
