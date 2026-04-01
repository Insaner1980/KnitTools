# KnitTools — Expanded Yarn Label OCR (Spec Addition)

> **Context:** This describes additions to the existing Yarn Estimator OCR (section 3.5 / 4.2 in the main spec). Implement AFTER the basic OCR (weight, meterage, needle size) is working and verified. This document does not replace the main spec — it extends the OCR feature.

---

## 1. What Changes

The current OCR scans a yarn label and extracts three values into the Yarn Estimator calculator fields. The expansion adds a **Yarn Card** — a saved summary of everything on the label.

**Basic OCR (already in spec):** Scan → extract weight/meterage/needle size → fill calculator fields.

**Expanded OCR (this addition):** Scan → extract ALL label data → show Yarn Card for review → save card → calculator fields also auto-fill as before.

The Yarn Card is NOT a stash manager or inventory system. It's a quick-reference card for the yarn you're currently working with. Think of it as a digital version of keeping the yarn label in your project bag.

---

## 2. Data Extracted from Label

OCR attempts to extract these fields. All are optional — the card saves whatever was found.

| Field | Example | Detection method |
|-------|---------|-----------------|
| Brand | "Drops" | Text block near top of label |
| Yarn name | "Baby Merino" | Text block, typically largest text |
| Fiber content | "100% Merino Wool" | Regex: percentages + fiber keywords |
| Weight (grams) | "50g" | Regex: number + g/gr/grams |
| Length | "175m" | Regex: number + m/meters/yds/yards |
| Suggested needle size | "3.5mm" | Regex: number + mm, or "US 4" pattern |
| Suggested gauge | "24 sts × 32 rows = 10cm" | Regex: number + sts/stitches pattern |
| Color name | "Old Pink" | Text near color number |
| Color number | "46" | Regex: short number near "col" or "color" |
| Dye lot | "23891" | Regex: number near "lot" or "dye lot" |
| Yarn weight category | "DK" | Keyword match: Lace/Fingering/Sport/DK/Worsted/Aran/Bulky/Super Bulky |

**Care symbols** are NOT extracted via OCR (they're tiny icons, unreliable). Instead, the Yarn Card has a manual care symbol picker — a row of standard textile care icons the user taps to toggle on/off.

---

## 3. Navigation & Flow

### Entry point

Same as current spec: camera button on the Yarn Estimator screen. No new navigation elements on the home screen.

### Flow

```
Yarn Estimator screen
  └─ tap camera icon (Pro)
      └─ Camera viewfinder (existing)
          └─ tap capture
              └─ NEW: Yarn Card review screen (instead of just a confirmation dialog)
                  ├─ Save card → returns to Yarn Estimator with fields filled
                  └─ Discard → returns to Yarn Estimator, nothing saved
```

### Accessing saved cards

A small secondary button on the Yarn Estimator screen: "Saved yarns" (or a small yarn ball icon). Opens a simple list of saved Yarn Cards. Tapping a card:
- Shows the full card details
- "Use in calculator" button fills the Yarn Estimator fields from this card

**Free:** Can scan and view the card during the session, but cannot save cards.
**Pro:** Save unlimited Yarn Cards. Access saved cards anytime.

---

## 4. Yarn Card UI

The card follows KnitTools visual style: sage green accents, warm craft aesthetic, Material Design 3 surfaces.

### Review screen (after scan)

```
┌──────────────────────────────────┐
│ ← Scanned Yarn                  │
│                                  │
│  ┌────────────────────────────┐  │
│  │                            │  │
│  │   [Scanned label photo]    │  │
│  │   (small, tappable to      │  │
│  │    view full size)         │  │
│  │                            │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌────────────────────────────┐  │
│  │ Brand         [Drops     ] │  │
│  │ Yarn          [Baby Merin] │  │
│  │ Fiber         [100% Merin] │  │
│  │ Color         [Old Pink  ] │  │
│  │ Color #       [46        ] │  │
│  │ Dye lot       [23891     ] │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌────────────────────────────┐  │
│  │ Weight    [50   ] g        │  │
│  │ Length    [175  ] m ↔ yds  │  │
│  │ Needle    [3.5  ] mm       │  │
│  │ Gauge     [24 sts / 32 r ] │  │
│  │ Category  [DK         ▼ ] │  │
│  └────────────────────────────┘  │
│                                  │
│  Care:  🧺  🔲  🔲  🔲  🔲  🔲  │
│  (tappable care symbol row)      │
│                                  │
│  ┌──────────┐  ┌──────────────┐  │
│  │ Discard  │  │ Save & Use ◉ │  │
│  └──────────┘  └──────────────┘  │
└──────────────────────────────────┘
```

**Key behaviors:**
- Every field is editable — OCR pre-fills, user corrects
- Fields OCR couldn't detect are left empty (not hidden)
- The scanned photo is saved with the card as a thumbnail reference
- "Save & Use" saves the card AND fills the Yarn Estimator calculator
- "Discard" returns to Yarn Estimator without saving, but still fills the three calculator fields (weight, length, needle) if detected — same as current spec behavior
- Unit toggle (m ↔ yds) converts the length value inline

### Saved card (in list view)

Compact card showing: brand + yarn name, color swatch dot (user can set color), weight category badge, and fiber content as subtitle. One line per card.

### Saved card (detail view)

Same layout as review screen but with "Use in calculator" and "Delete" buttons instead of Save/Discard. "Edit" icon in top bar to modify any field.

---

## 5. Care Symbol Picker

A horizontal scrollable row of standard ISO 3758 textile care symbols. Each is a small icon button that toggles on/off.

Five categories, each with 3-5 variants:

| Category | Variants |
|----------|----------|
| Washing | Machine 30° / 40° / 60° / Hand wash / Do not wash |
| Bleaching | Any bleach / Non-chlorine / Do not bleach |
| Drying | Tumble low / Tumble normal / Flat dry / Do not tumble |
| Ironing | Low / Medium / High / Do not iron |
| Dry cleaning | Any solvent / P solvent / F solvent / Do not dry clean |

Implementation: static drawable icons bundled with the app. No OCR involved — purely manual selection. Each category shows as a collapsible chip group or a small bottom sheet when tapped.

---

## 6. Image Suggestions for Nano Banana

Two potential images for this feature area:

### Yarn Estimator card (home screen — already exists in spec)
Current spec: "kitchen scale with yarn ball and measuring tape"
This can stay as-is. The OCR is accessed from within this tool, not from the home screen.

### Yarn Card empty state illustration
When the user opens "Saved yarns" and has no cards yet, show a small illustration instead of blank space:
- **Prompt idea:** "A single yarn label lying on a sage green knitted fabric background, soft natural lighting, shallow depth of field, overhead view, craft photography style, warm tones"
- This reinforces what the feature does without being a UI screenshot

### Camera viewfinder frame
The scan screen could have a decorative border/frame around the camera viewfinder:
- **Prompt idea:** "Decorative rectangular frame made of delicate yarn/thread in sage green and cream colors, on transparent background, minimal, elegant"
- This would be an overlay PNG on the camera preview — purely decorative, helps the user know where to position the label
- Note: this needs to be a PNG with transparency, verify Nano Banana can produce this cleanly. If not, Claude Code can create a simple SVG frame instead.

---

## 7. Technical Notes for Claude Code

1. The Yarn Card uses the same Room database. New entity: `YarnCard` with all fields from section 2, plus `photoUri` (internal app storage path) and `createdAt` timestamp.
2. Photo is saved to app-internal storage (not gallery), same privacy approach as main spec.
3. OCR extraction logic: run ML Kit Text Recognition, get full text, then apply regex patterns sequentially to identify each field. Start with high-confidence patterns (grams, meters) and work toward lower-confidence ones (brand, yarn name).
4. The care symbol picker state is stored as a simple integer bitmask or comma-separated string in Room — not a complex data structure.
5. The Yarn Card list should support swipe-to-delete with undo snackbar.
6. This feature adds no new permissions beyond what the main spec already requires (CAMERA).
7. The "Saved yarns" list is a simple LazyColumn — no need for paging or complex list management. A knitter might save 20-50 cards over a year at most.
