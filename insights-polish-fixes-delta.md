# Insights Polish Fixes — Delta Spec

**Scope:** Small visual fixes across InsightsScreen  
**Risk level:** Low — text formatting, color, and separator changes only  
**No layout restructure, no new data, no schema changes**

---

## 1. AVG PACE unit: shorten to "r/hr"

The three-column stat card layout truncates "rows/hr" to "58 ro...".

Fix: change the unit suffix from `rows/hr` to `r/hr` everywhere it appears in Insights.

The stat card should display: `58 r/hr`

Do not change the label text "AVG PACE" — only the value's unit suffix.

---

## 2. Remove dot/bullet separators from dates

The date labels in Time Per Project currently show a leading dot: `· today`, `· yesterday`, `· 5d ago`.

Remove the `· ` prefix entirely. Display just: `today`, `yesterday`, `5d ago`, `1w ago`.

The date text style and color stay unchanged — only the dot character and its trailing space are removed.

---

## 3. Stats row separator: spacer + color instead of middle dot

The stats text below each Time Per Project bar currently uses a middle dot separator: `0.9h · 13 rows`.

Replace with:

- Remove the ` · ` middle dot
- Split into two separate `Text` composables inside a `Row`
- First text (time): `0.9h` in `onSurface` color, current text style
- `Spacer(Modifier.width(12.dp))` between them
- Second text (rows): `13 rows` in `onSurfaceVariant` color, same text style

This makes time the primary value and rows the secondary, without any punctuation.

When either value is missing or zero:
- Time is `—` and rows exist: show `—` then spacer then `6 rows`
- Time exists and rows is 0: show `0.9h` only, no spacer, no rows text
- Both zero: show `—` only

---

## 4. Footer: italic + rotating messages + divider

### 4a. Divider above footer

Add a subtle horizontal divider above the footer text:

- `HorizontalDivider` or `Divider`
- Color: `MaterialTheme.knitToolsColors.onSurfaceMuted` at **15% alpha**
- Thickness: `1.dp`
- Horizontal padding: `48.dp` on each side (so it doesn't span the full width — a short centered line)
- Bottom padding from divider to text: `12.dp`

### 4b. Italic text style

Change the footer text to italic:

```kotlin
style = MaterialTheme.typography.bodySmall.copy(
    fontStyle = FontStyle.Italic
)
```

Color stays `onSurfaceVariant` as currently implemented.

### 4c. Rotating messages

Replace the single hardcoded message with a pool of messages. Select one per session (not per recomposition).

Implementation:

```kotlin
val footerMessages = listOf(
    R.string.insights_footer_1,
    R.string.insights_footer_2,
    R.string.insights_footer_3,
    R.string.insights_footer_4,
    R.string.insights_footer_5
)

// Stable per session — pick based on today's date so it changes daily but not on scroll
val messageIndex = remember {
    LocalDate.now().toEpochDay().mod(footerMessages.size).toInt()
}
```

String resources:

```xml
<!-- EN -->
<string name="insights_footer_1">Every row counts — keep knitting!</string>
<string name="insights_footer_2">Your needles are waiting.</string>
<string name="insights_footer_3">One more row?</string>
<string name="insights_footer_4">Stitch by stitch, row by row.</string>
<string name="insights_footer_5">Great things start with a single cast on.</string>

<!-- FI -->
<string name="insights_footer_1">Jokainen kerros lasketaan — jatka neulomista!</string>
<string name="insights_footer_2">Puikot odottavat.</string>
<string name="insights_footer_3">Vielä yksi kerros?</string>
<string name="insights_footer_4">Silmukka silmukalta, kerros kerrokselta.</string>
<string name="insights_footer_5">Suuret asiat alkavat yhdestä luomiskerroksesta.</string>
```

The message changes once per day. It does not animate or transition — just shows a different message each day.

---

## What NOT to change

- Do not change stat card layout (3 columns stays)
- Do not change streak grid visuals
- Do not change bar colors or bar track
- Do not change the project filter or time range filter
- Do not change card shapes or backgrounds
- Do not add any new UI elements beyond what is described here
