# KnitTools — Future Considerations: Counter Customization

**Status:** Deferred. Not for v1.0. Revisit after user feedback post-launch.

**Source:** ChatGPT review of current UI, April 2026.

---

## Concept: Per-Project Counter Layout Customization

### Problem
Different knitting projects need different Counter screen layouts:
- A sock project benefits from a visible pattern repeat section
- A sweater project needs shaping counters prominently
- A simple scarf project only needs the big row counter

Currently all projects share the same fixed layout.

### Proposed Model: Two-Level Customization

**Level 1 — Settings (app-wide defaults)**

New settings section: "Counter layout defaults"

Toggles controlling what is shown by default in all projects:
- Show project tools card
- Show session time
- Show extra counters section
- Show pattern repeat when active
- Expand counters by default

**Level 2 — Per-project overrides (Row Counter overflow menu)**

New overflow menu item: "Customize Counter"

Opens a bottom sheet or dialog where each section has three states:
- **Use app default** (follows Settings)
- **Show** (always visible for this project)
- **Hide** (always hidden for this project)

### What should always remain visible (not customizable)
- Project name
- Current row number
- Minus / plus / undo controls

### What can be customizable
- Project tools card
- Session time
- Pattern repeat section
- Extra counters section
- Stats row
- AI summary shortcut
- Stitch tracker (v3)
- Pattern shortcut (v3)

### Why deferred

1. **No user data yet.** We don't know which sections people actually want to hide. Building a full override system before launch risks overengineering.
2. **Complexity cost.** Three-state per-section per-project is a lot of preference state to manage (DataStore or Room field per project).
3. **Simpler alternatives first.** A collapsible Counters section and possibly a single "compact mode" toggle would cover 80% of the need with 20% of the effort.

### Recommended path
1. **v1.0:** Ship as-is. Observe feedback.
2. **v1.x:** Add collapsible/expandable Counters section if users request it.
3. **v2.0:** If demand exists, implement the full two-level customization system.

---

## Additional UI Observations (Low Priority)

- Project card hierarchy in Counter screen could be tightened — the card is visually nice but information density is slightly unbalanced
- `Pattern repeat: 0` is fairly prominent when inactive — consider muting or hiding when value is 0 (similar to the 0-rows treatment in Project List)
- Counters section is the strongest candidate for a collapsible/expandable pattern if any single section gets that treatment first
