# Row Counter Screen Redesign

## Goal

Redesign the Row Counter screen (`CounterScreen.kt`) to feel like you are **inside** the project — not looking at a dashboard. Remove the utilitarian "Row Counter" title and the boxed project card. Make the project name the dominant visual element at the top.

## Reference

See the attached Stitch mockup image for the target feel. Key qualities to match:
- Project name is the screen's headline — large, bold, dominant
- Yarn and metadata sit directly beneath the name, compact and understated
- Generous breathing room around the counter number
- No card/surface wrapping the project info — it sits directly on the background
- Clean, minimal, premium feel

## Current State (what to change)

The screen currently has:
1. `"Row Counter"` title top-left (TopAppBar or equivalent)
2. Settings gear icon top-right
3. A `Surface` card containing: "CURRENT PROJECT" label, project name, sparkle icon, chevron, linked yarn with × button, "Add yarn" link, notes icon
4. "CURRENT ROW" label + large RollingCounter (96sp)
5. Pattern repeat pill with −/+ inside
6. Main buttons: − (48dp outlined) / + (72dp gold filled) / undo (48dp outlined)
7. Stats row: stitches + time on surfaceVariant background
8. "Reset Counter" text below stats

## New Layout (top to bottom)

### 1. Top bar area

```
[←]                              [⚙]
```

- **Remove** the "Row Counter" title entirely — no TopAppBar title text
- Back arrow (←) on the left → navigates to Project List
- Settings gear (⚙) on the right → navigates to Settings
- Use a transparent/background-colored top bar, not an elevated surface

### 2. Project identity (NO card wrapper)

```
MERINO BEANIE                  [AI] [>]
Aara Love · Add yarn +     [notes icon]
```

- **Project name**: `headlineLarge` weight `ExtraBold`, color `onSurface`. This is the biggest text on screen after the counter number. Tap → rename dialog (existing behavior).
- **Right of name**: small "AI" text badge (Pro, triggers AI summary) and chevron (>) to Project List — same row, aligned right. The "AI" badge is a compact pill/badge using `labelSmall`, `onSurfaceVariant` text with a subtle `surfaceVariant` or tonal background and rounded corners. Not an icon — literal text "AI". Chevron uses `onSurfaceVariant` color, muted.
- **Second line**: Linked yarn name(s) with dusty rose yarn ball icon, interpunct separator (·), then "Add yarn +" in `secondary` color. If no yarn linked, just show "Add yarn +".
- **Notes icon**: Small icon at the end of the second line (or right-aligned). Opens the existing ModalBottomSheet.
- **No Surface card, no background, no border** — these elements sit directly on the Scaffold background color.
- Horizontal padding: 24dp (matching general screen padding)
- Top padding: 8dp below the top bar icons

### 3. Counter area (vertically centered in remaining space)

```


         CURRENT ROW
            21

        Pattern repeat: 3
```

- "CURRENT ROW" label: `labelSmall`, all-caps, letter-spacing 1.5sp, `onSurfaceVariant` color — **keep as-is**
- Counter number: RollingCounter 96sp ExtraBold — **keep as-is**
- Pattern repeat pill: existing compact pill with −/+ — **keep as-is**
- **Key change**: Add more vertical breathing room above and below the counter. The counter area should feel centered in the space between the project identity and the buttons. Use `Spacer(modifier = Modifier.weight(1f))` above and below the counter group to push it toward vertical center.

### 4. Buttons

```
       (−)    ( + )    (↩)
```

- Keep existing layout: − (48dp outlined) / + (72dp gold filled) / undo (48dp outlined)
- No changes to button styling or behavior

### 5. Stats row + Reset

```
    ┌─────────────┬──────────────┐
    │   0 st      │   ⏱ 39:03   │
    └─────────────┴──────────────┘
            Reset Counter
```

- Keep existing stats row (surfaceVariant background, stitches + time)
- Keep "Reset Counter" muted text below
- No changes

### 6. Bottom navigation bar

- Keep existing 3-tab NavigationBar (Projects / Tools / Reference)

## Summary of visual changes

| Element | Before | After |
|---------|--------|-------|
| Screen title | "Row Counter" top-left | **Removed entirely** |
| Project info | Inside a Surface card with "CURRENT PROJECT" label | **Directly on background, no card, no label** |
| Project name | `titleLarge` inside card | **`headlineLarge` ExtraBold, dominant** |
| Yarn info | Multi-line inside card with × buttons | **Single compact line: "Aara Love · Add yarn +"** |
| Yarn remove (×) | Visible × icon next to each yarn | **Move to long-press or edit mode** (keep × for now if simpler, but make it smaller/subtler) |
| Counter vertical position | Fixed position | **Vertically centered** in available space using weight spacers |
| Breathing room | Tight | **Generous** — more space above/below counter |

## What NOT to change

- RollingCounter animation and sizing (96sp ExtraBold)
- Pattern repeat pill design and behavior
- Button sizes, colors, and layout (−/+/undo)
- Stats row design
- "Reset Counter" text
- Bottom NavigationBar
- All tap/click behaviors and navigation targets
- ViewModel logic, state management, data flow
- Session tracking

## Implementation notes

- The project identity section replaces both the TopAppBar title and the project card composable
- Use `Scaffold` with `topBar` that only contains back arrow and settings icon (no title)
- The project name and yarn line go into the Scaffold `content`, not the top bar
- Use `Modifier.weight(1f)` spacers in a Column to vertically center the counter group
- Test with long project names — truncate with ellipsis if needed (`maxLines = 1, overflow = TextOverflow.Ellipsis`)
- Test with multiple linked yarns — if more than 2, consider showing count ("2 yarns") instead of listing all names

## Confidence note

If any existing behavior or component referenced here does not exist in the current code, or if the current implementation differs from what's described in "Current State", report the discrepancy — do not guess or invent code.
