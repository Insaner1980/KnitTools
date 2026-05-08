# Calculator Screens — Premium Visual Polish

Applies to all four calculator screens: Gauge Calculator, Increase/Decrease, Cast On Calculator, Yarn Estimator.

## 1. Group fields inside section cards

Wrap each logical section's input fields inside a SurfaceHigh (#3A3A2A) rounded card (18dp corner radius, 16dp padding). This creates visual grouping instead of fields floating loose on the dark background.

Example for Gauge Calculator:
- Card 1: "Your Gauge" header + mode toggle + all swatch/gauge input fields
- Card 2: "Pattern Gauge" header + its input fields  
- Card 3: "Pattern Instructions" header + its input fields

Example for Increase/Decrease:
- Card 1: Increase/Decrease toggle + Flat/Circular toggle + Current stitches + Increase/Decrease by fields

Example for Cast On Calculator:
- Card 1: cm/inches toggle + all input fields (desired width, stitch gauge, pattern repeat, edge stitches)

Example for Yarn Estimator:
- Card 1: all input fields grouped

## 2. Input field styling

Change input fields from current SurfaceHigh to **SurfaceHighest (#454535)** background so they are clearly distinguishable from the card they sit inside. Add a subtle 1dp outline using the Divider color (#3A3A2A) with 50% opacity. Corner radius 14dp.

## 3. Section header accent colors

Give each section header a unique accent color, same approach as Tools/Reference cards:

**Gauge Calculator:**
- "Your Gauge" → Primary (#C45100) burnt orange
- "Pattern Gauge" → Secondary (#8BA44A) avocado
- "Pattern Instructions" → Tertiary (#C9A435) mustard

**Increase/Decrease:**
- Keep single section, header in Primary (#C45100)

**Cast On Calculator:**
- Keep single section, header in Primary (#C45100)

**Yarn Estimator:**
- Keep single section, header in Primary (#C45100)

The section header icon should also use the same accent color as the text.

## 4. Result card styling

When results appear, display them in a card with:
- Background: Primary (#C45100) at 10% opacity
- Border: 1.5dp solid Primary (#C45100) at 30% opacity
- Corner radius: 18dp
- Result numbers: large, bold (Outfit ExtraBold), Primary color
- Result labels: TextMuted, labelSmall uppercase

## 5. Paste Instruction button

Style it more prominently:
- Background: Tertiary (#C9A435) at 10% opacity
- Text and sparkle icons: Tertiary color
- Corner radius: 14dp
- Full width
- Padding: 14dp vertical

## 6. Toggle buttons (cm/inches, Increase/Decrease, Flat/Circular, Measure swatch/Enter directly)

Keep current styling — active segment with Primary gradient, inactive with SurfaceHigh. No changes needed.

## What NOT to change
- Calculation logic
- Field labels and placeholder text
- Navigation
- Toggle behavior
- Paste instruction functionality
- Screen titles and back arrows
