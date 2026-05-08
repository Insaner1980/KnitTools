# Row Counter — Dynamic Stitch Count

## Change

Make the stitch count in the stats row dynamic instead of a static stored value.

## How it works

1. The user taps the stitch count field in the stats row
2. A dialog appears where they can enter stitches per row (e.g. 120)
3. This value is saved to the project entity (`stitchesPerRow` field, or repurpose existing `stitchCount` field)
4. The stats row displays: stitches per row × current row count = total stitches (e.g. "2,520 st" when stitchesPerRow=120 and row=21)
5. The total updates automatically whenever the row counter changes

## Display states

- **No stitches set**: Show "Set stitches" as placeholder text (tappable, muted/secondary color)
- **Stitches set**: Show calculated total, e.g. "2,520 st" (formatted with thousands separator)

## Dialog

Simple AlertDialog with a single NumberInputField:
- Title: "Stitches per row"
- One numeric input field
- Cancel / Save buttons
- If a value was previously set, pre-fill it

## Do NOT

- Change the time field or its behavior
- Change the stats row layout or styling
- Add any other new fields or features
