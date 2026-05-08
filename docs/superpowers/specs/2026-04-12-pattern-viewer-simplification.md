# Pattern Viewer Simplification

**Type:** Redesign — strip complexity, keep core value  
**Status:** Spec ready for implementation  
**Pro only:** Nano instruction display yes, PDF viewer no

---

## 1. Problem

Current Pattern Viewer is overcomplicated:
- 7+ mystery icons in toolbar that users don't understand
- Manual row mapping via long-press is unintuitive and inaccurate
- Annotation tools (draw, highlight, undo) add clutter without clear value
- PDF doesn't fill screen width properly
- Gesture conflicts between zoom, scroll, and long-press
- Feature feels like a tech demo, not a tool for knitters

Industry standard (Knit Companion, My Row Counter, Yarnly) is simple: PDF + highlight bar + linked counter. KnitTools tries too much and delivers too little.

## 2. Design: Strip to Core Value

### What stays
- PDF rendering (full screen width, pinch-to-zoom)
- Bottom bar: Row number + / controls, Page navigation
- Nano instruction display below bottom bar (KnitTools's differentiator)
- Position memory (row + page saved to DB, survives app restart)

### What gets removed
- **AnnotationOverlay** — all drawing/highlighting tools
- **Manual row mapping** — long-press to place highlight bar
- **Auto-map** (FilterVintage icon) — Nano marker detection
- **Row mapping toolbar** — all annotation-related icons
- **PatternAnnotationRepository** usage in viewer (keep repository for future, remove from UI)
- **RowHighlightOverlay long-press** — no manual positioning
- **"Long-press on the page to map the current row position"** hint text
- **Detector message** text

### What changes
- **Top bar:** Back + pattern name + overflow menu (detach pattern only)
- **Highlight bar:** Purely decorative/informational — fixed position calculated from row number vs total rows on page, moves automatically when counter changes. No manual interaction. Hidden if position can't be estimated.
- **PDF display:** Full width, proper pinch-to-zoom + pan, vertical scroll for tall pages

## 3. Architecture

### 3.1 Simplified Top Bar

```
┌──────────────────────────────────────┐
│  ←   Pattern Name            ⋮      │
└──────────────────────────────────────┘
```

- Back arrow (existing style: outline tint, transparent background)
- Pattern name (titleLarge, truncated with ellipsis)
- Overflow menu: "Detach pattern" only

Remove: annotation mode toggle, auto-map, undo, brush, highlighter, eye, ".." icons.

### 3.2 PDF Display

Full-width rendering without card wrapper or horizontal padding:

```kotlin
BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxWidth / aspectRatio)
            .graphicsLayer(scaleX = scale, scaleY = scale, ...)
    ) {
        Image(bitmap, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxSize())
        // Highlight bar (automatic, no interaction)
    }
}
```

**Pinch-to-zoom implementation:**
- Use `transformable` state from `foundation.gestures` (not custom `detectTransformGestures`)
- `rememberTransformableState` handles zoom + pan natively without blocking other gestures
- Scale range: 1f to 5f
- Pan clamped to prevent scrolling out of bounds
- Double-tap to reset zoom to 1f

This approach avoids the gesture conflicts that `detectTransformGestures` caused, because `transformable` is designed to coexist with other gesture handlers.

### 3.3 Automatic Highlight Bar

Instead of manual long-press mapping, estimate row position automatically:

**Simple heuristic (v1):**
```
yPosition = currentRow / estimatedTotalRowsOnPage
```

Where `estimatedTotalRowsOnPage` comes from:
1. If Nano has extracted instructions for rows on this page, use the range (e.g., rows 1-10 found → 10 rows)
2. Fallback: hide the highlight bar entirely

The bar is purely visual — no interaction, no gesture handling. Just a semi-transparent band drawn at the estimated position.

**Key difference from current:** No `RowHighlightOverlay` gesture handling. The bar moves automatically when Row +/- is pressed. If position can't be estimated, it simply doesn't show. No error, no hint text.

### 3.4 Bottom Bar (unchanged structure)

```
┌──────────────────────────────────────┐
│  Row 7   —   +     ← Page 2 of 3 →  │
│  K2tog, K2tog, K2tog, K1            │ ← Nano instruction (Pro only)
└──────────────────────────────────────┘
```

- Row counter with +/- buttons
- Page navigation with arrows
- Nano instruction text (existing implementation — keep as-is)
- Shimmer loading state (existing — keep)
- Long-press to copy instruction (existing — keep)

### 3.5 Overflow Menu

Accessed via ⋮ in top bar:

```
┌─────────────────┐
│ Detach pattern   │
│ Jump to page...  │
└─────────────────┘
```

Keep existing "Jump to page" dialog (AlertDialog with page number input). Remove all annotation-related menu items.

## 4. Files to Modify

| File | Change |
|------|--------|
| `PatternViewerScreen.kt` | Remove annotation toolbar, simplify top bar, remove AnnotationOverlay, remove long-press hint text, remove detector message, implement proper zoom, simplify RowHighlightOverlay to auto-position |
| `RowHighlightOverlay.kt` | Remove `enableMapping` and `onLongPress` parameters. Keep only `yPosition` display (pure visual). Remove `pointerInput`/`detectTapGestures`. |
| `PatternViewerViewModel.kt` | Remove annotation state, remove row mapping state, add auto-position estimation |

## 5. Files to Leave Alone

| File | Reason |
|------|--------|
| `PatternTextExtractor.kt` | Still needed for Nano instruction pipeline |
| `PatternInstructionExtractor.kt` | Core value — keep as-is |
| `PatternAnnotationRepository.kt` | Don't delete — may be useful later. Just stop using it from viewer. |
| `AnnotationOverlay.kt` | Can delete or keep — not referenced after removal from viewer |
| `PdfPageRenderer.kt` | Keep as-is — rendering logic is fine |
| Bottom bar composables | Keep instruction display, shimmer, copy-on-long-press |

## 6. Files to Potentially Delete

| File | Reason |
|------|--------|
| `AnnotationOverlay.kt` | No longer used after viewer simplification |

## 7. Pinch-to-Zoom: Technical Approach

Use `rememberTransformableState` + `Modifier.transformable()` instead of raw `detectTransformGestures`:

```kotlin
var scale by remember { mutableFloatStateOf(1f) }
var offset by remember { mutableStateOf(Offset.Zero) }

val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
    scale = (scale * zoomChange).coerceIn(1f, 5f)
    if (scale > 1f) {
        offset += panChange
    } else {
        offset = Offset.Zero
    }
}

Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(imageHeight)
        .transformable(state = transformableState)
        .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = offset.x,
            translationY = offset.y,
        )
) {
    Image(...)
    HighlightBar(...)  // No gesture handling
}
```

`transformable` is designed for multi-touch gestures and does NOT consume single-touch events, so it coexists with scroll and other gestures naturally.

**Double-tap to reset:**
```kotlin
.pointerInput(Unit) {
    detectTapGestures(
        onDoubleTap = {
            scale = 1f
            offset = Offset.Zero
        }
    )
}
```

## 8. Edge Cases

| Scenario | Behavior |
|----------|----------|
| Row 0 (no row set) | Highlight bar hidden, no Nano query |
| Nano unavailable or free user | No instruction text, bottom bar shows only row + page |
| Nano returns NOT_FOUND for row | Instruction area hidden for that row |
| Highlight position can't be estimated | Bar hidden, PDF still shows normally |
| Pattern detached | Return to Counter screen |
| Very long PDF page (portrait) | Vertical scroll works, zoom available |
| Wide PDF page (landscape) | Fills width, height proportional, can zoom in |
| App restart | Row, page, and pattern URI restored from DB |

## 9. Success Criteria

1. User opens Pattern Viewer and immediately sees the PDF filling the screen
2. No mystery icons — only back, title, and menu
3. Pressing +/- updates row, instruction appears at bottom
4. Pinch-to-zoom works without breaking any other gesture
5. No setup required — viewer is useful from the first second
6. Annotation mode and row mapping no longer exist in the UI

## 10. What This Does NOT Include

- Voice commands (separate feature, Counter-side)
- Cross-page row lookup (Nano searches only current page)
- Structured pattern parsing
- PDF editing/cropping
- Multiple patterns per project
