# KnitTools Yarn UX Delta Spec

**Scope:** Changes to already-implemented My Yarn list cards, Yarn Card Detail screen layout, and new Counter screen yarn summary.

**Base:** Current codebase with v2-delta-spec already implemented (Library tab, My Yarn screen, YarnCardEntity with quantityInStash/status/linkedProjectId fields).

**Principle:** List = scannable summary. Detail = management. Counter = compact reference.

---

## 1. My Yarn List Card — Simplify

### Current state (from v2 implementation)
Card shows: brand + yarn name, fiber content, weight category badge, quantity badge, status indicator.

### Change: reduce card content
Remove fiber content from the card. Keep only:

| Row | Content | Style |
|-----|---------|-------|
| 1 | Brand + yarn name | titleSmall (brand muted, name default) |
| 2 | Weight category badge | BadgePill, colored by weight category |
| 3 | Status pill + quantity pill side by side | Status pill left, quantity right |

Example:
```
Sandnes Garn
Sunday                        [DK]
[In Use]                  3 skeins
```

### Conditional: linked project

- Show linked project name **only when status is IN_USE**
- Display as muted bodySmall text below the pills: `→ Classic Ribbed Hat`
- Hidden for IN_STASH and FINISHED — not relevant in those states

### What is NOT on the list card

- Fiber content (detail only)
- Needle size, gauge, color info (detail only)
- Inline +/− quantity controls (detail only)
- Status change controls (detail only)
- Care symbols (detail only)

### Tap action

Tap anywhere on card → navigate to `yarn_card_detail/{cardId}`

---

## 2. Yarn Card Detail Screen — Reorder Layout

### Current state (from v2 implementation)
Detail screen shows quantity (editable +/−), status badge (tappable cycle), and linked project name. Layout order not strictly defined — fields displayed without clear hierarchy.

### Change: reorder into mutable-first, reference-second hierarchy

**Section A — Identity (top)**

- Photo thumbnail (compact, left-aligned or top-aligned — not a full-bleed hero image)
- Brand (bodyMedium, onSurfaceMuted)
- Yarn name (headlineSmall)

**Section B — Actions (immediately below identity)**

These are the things the user changes often. Grouped together in a surfaceHigh card.

| Item | Display | Interaction |
|------|---------|-------------|
| Status | Pill showing current status (colored: IN_STASH = avocado, IN_USE = burnt orange, FINISHED = onSurfaceMuted) | Tap → ModalBottomSheet with three options. No cycle-tap. |
| Quantity | `− 3 skeins +` inline control | Tap −/+ to adjust. Minimum 0. |
| Linked project | `→ Classic Ribbed Hat` or `Link to project` if none | Tap → project picker sheet (if no link) or navigate to project (if linked). Long-press or secondary action to change/remove link. |

**Section C — Yarn details (middle)**

Static reference information. surfaceHigh card, two-column grid or labeled rows.

| Field | Notes |
|-------|-------|
| Fiber content | e.g. "100% Merino Wool" |
| Weight category | e.g. "DK" |
| Weight | e.g. "50g" |
| Length | e.g. "160m" |
| Needle size | e.g. "4.0mm" |
| Gauge info | e.g. "22 sts × 30 rows / 10cm" |
| Color name | e.g. "Dusty Pink" |
| Color number | e.g. "4523" |
| Dye lot | e.g. "2847" |

Only show fields that have data. Don't render empty rows.

**Section D — Care symbols (below details)**

- Compact horizontal row of CareSymbolIcon components
- Own labeled section: "Care" (labelSmall, onSurfaceMuted)
- Only visible if care symbols exist on the card

**Section E — Destructive actions (bottom or overflow)**

- Edit and Delete in TopAppBar overflow menu
- Or: single "Delete" as a muted text button at the very bottom
- Not scattered through the content

### TopAppBar

- Back arrow + "Yarn name" (truncated if long)
- Overflow menu: Edit, Delete

---

## 3. Counter Screen — Yarn Summary (NEW)

### Current state
Counter screen project card shows individual linked yarn names with × unlink buttons and a separate "+ Add yarn" link. This gets crowded with multiple yarns.

### Change: replace with compact summary + BottomSheet

### Display rules

| State | Text | Style |
|-------|------|-------|
| No yarn linked | `Add yarn` | Tertiary color |
| 1 yarn linked | Yarn name (e.g. `Aara Love`) | bodyMedium, default text color |
| 2+ yarns linked | `X yarns` (e.g. `2 yarns`) | bodyMedium, default text color |

### Interaction

Tap → **ModalBottomSheet** (YarnManagementSheet):

- List of linked yarns, each with:
  - Yarn name + brand (bodyMedium)
  - Unlink × action (trailing icon)
- "+ Add yarn" action at bottom (opens existing yarn picker)

### What to remove from project card

- Individual yarn name rows with × unlink buttons
- Separate "+ Add yarn" text link
- Any inline yarn editing

---

## 4. Status Change — Replace Cycle-Tap with Selection Sheet

### Current state (from v2 implementation)
Status badge is tappable and cycles through IN_STASH → IN_USE → FINISHED directly. This is too hidden and easy to trigger accidentally.

### Change
Status change **always** opens a selection sheet/menu with three explicit options:
  - In Stash
  - In Use
  - Finished
- Applies to: My Yarn list (if status pill is tappable), Yarn Card Detail, and any future surface showing status

---

## 5. Counter Screen — Pattern Row Fix

### Current state
When a pattern is attached, the Counter screen shows both the pattern name (e.g. "Classic Ribbed Hat · Ravelry") AND "Attach pattern" side by side. This implies multiple patterns can be attached, which is not the intended behavior.

### Change

| State | Display | Behavior |
|-------|---------|----------|
| No pattern attached | `Attach pattern` (Tertiary color) | Opens pattern picker |
| Pattern attached | Pattern name + source only (e.g. `Classic Ribbed Hat · Ravelry`) | Tappable — opens pattern viewer |

### Rules
- Never show both pattern name AND "Attach pattern" at the same time
- The tappable pattern name is the only element when a pattern exists
- Change/remove pattern actions live in Pattern Viewer overflow menu (see knittools-v3-pattern-viewer-note.md)
- Keep the pattern row outside the project card, in its current position under the project title

### File
- `ui/screens/counter/CounterScreen.kt` — conditional rendering of pattern row

---

## 6. Data Model

No database changes. All fields (quantityInStash, status, linkedProjectId) are already in place from the v2 migration. This spec only changes UI layout and interaction patterns.

---

## 7. Files Affected

### New
- `ui/screens/counter/YarnManagementSheet.kt` — ModalBottomSheet for Counter screen yarn management
- `ui/screens/library/YarnStatusSheet.kt` — Shared ModalBottomSheet for status selection (reusable between My Yarn and Detail)

### Modified
- `ui/screens/library/MyYarnScreen.kt` — Card layout per this spec
- `ui/screens/tools/YarnCardDetailScreen.kt` — Section hierarchy per this spec
- `ui/screens/counter/CounterScreen.kt` — Compact yarn summary + YarnManagementSheet trigger

### Unchanged
- YarnCardEntity, YarnCardDao, YarnCardRepository — no data model changes beyond v2-delta-spec
- YarnCardViewModel — may need minor updates for status sheet state, but no architectural changes
