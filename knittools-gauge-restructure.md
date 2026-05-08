# Gauge Calculator — Restructure for Clarity

## Current problem

The Gauge Calculator has confusing sections: "My Gauge" (swatch measurement), "Pattern gauge", "Your gauge", and "Pattern instructions". "My Gauge" and "Your gauge" mean the same thing with different names. Users don't understand the flow.

## New structure

Reorganize into a clear top-to-bottom flow with three distinct sections. Remove "My Gauge" and "Your gauge" labels. Replace with this:

### Section 1: "Your Gauge"
How the user determines their personal gauge. Two input modes with a toggle:

**Mode A: "Measure swatch" (default)**
- Measured width (cm/inches)
- Stitches in swatch
- Measured height (cm/inches)
- Rows in swatch
- Below the fields, show calculated result: "= X sts / Y rows per 10 cm" in Secondary color

**Mode B: "Enter gauge directly"**
- Stitches per 10 cm
- Rows per 10 cm

Toggle between modes using a small text button or segmented control below the "Your Gauge" section header: "Measure swatch" | "Enter directly"

### Section 2: "Pattern Gauge"
What the knitting pattern/instructions say the gauge should be:
- Stitches per 10 cm
- Rows per 10 cm

### Section 3: "Pattern Instructions"  
The original pattern measurements to convert:
- Stitches in pattern
- Rows in pattern

### Result
Show converted values:
- Adjusted stitches
- Adjusted rows
- Multiplier/ratio

### Other elements (keep as-is, just reorder)
- "Paste instruction" stays at the top (Pro feature)
- cm/inches toggle stays at the top
- The result card styling stays the same

## Section headers
Use the existing section header style with icon + bold text:
- 🧶 Your Gauge  (with mode toggle below)
- 📋 Pattern Gauge
- 📄 Pattern Instructions

## What NOT to change
- The actual calculation logic — keep all existing formulas
- Paste instruction feature
- cm/inches toggle
- Result card design
- Navigation (back arrow, screen title)
