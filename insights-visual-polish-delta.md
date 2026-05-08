# Insights Visual Polish — Delta Spec

**Style direction:** Material Design 3, KnitTools "70s Craft Revival" theme  
**Scope:** Visual-only changes to InsightsScreen. No new data, no new queries, no schema changes.  
**Risk level:** Low — cosmetic animations and color accents only

---

## 1. Accent color per stat card label

Currently all four stat cards (Total Time, Avg Pace, Completed, Top Streak) use the same muted label color. Add a distinct accent color to each card's **label text only** — the value text and card background stay unchanged.

Color assignments using existing theme tokens:

| Card | Label color token |
|------|------------------|
| TOTAL TIME | `MaterialTheme.colorScheme.primary` (burnt orange) |
| AVG PACE | `MaterialTheme.colorScheme.secondary` (avocado) |
| COMPLETED | `MaterialTheme.colorScheme.tertiary` (mustard/gold) |
| TOP STREAK | `MaterialTheme.knitToolsColors.brandWine` (dusty rose) |

These work in both light and dark theme because the tokens already adapt.

Do **not** change the card background, card shape, or value text color. Only the small uppercase label text gets the accent.

---

## 2. Animated number count-up on stat cards

When the Insights screen opens or the project filter changes, stat card values should animate from 0 to their target value.

Implementation:

- Use `Animatable<Float>` or `animateFloatAsState` with `tween(durationMillis = 600, easing = FastOutSlowInEasing)`
- For time values like "1.8h": animate the float from 0f to 1.8f, format with one decimal during animation
- For integer values like "2" or "9 days": animate as Int using `animateIntAsState`, same duration
- For pace like "59 rows/hr": animate as Int from 0 to 59
- Stagger the four cards slightly: 0ms, 80ms, 160ms, 240ms delay per card (use `animationSpec = tween(durationMillis = 600, delayMillis = N)`)
- Trigger animation on first composition AND when the filter chip selection changes (key the animation on the filter value)

Important: do not animate on every recomposition. Use `LaunchedEffect(filterKey)` or `remember(filterKey)` to restart only when filter changes.

---

## 3. Time Per Project bar entry animation

The horizontal bars in the TIME PER PROJECT chart should animate their width from 0 to final width when appearing.

Implementation:

- Each bar animates its width fraction from 0f to its target fraction
- Use `animateFloatAsState` with `tween(durationMillis = 500, easing = FastOutSlowInEasing)`
- Stagger bars: each bar starts 60ms after the previous one
- The stagger starts after the stat card animations finish (add ~400ms base delay so it feels sequential: cards animate in, then bars sweep in)
- Trigger on same filter key as stat cards

Bar visual improvement:
- Add `RoundedCornerShape(4.dp)` to bar ends (clip shape) if not already present
- This is a small touch that fits the craft aesthetic

---

## 4. Section header style for TIME PER PROJECT

The "TIME PER PROJECT" section header should use the same accent color treatment as the stat card labels — specifically `MaterialTheme.colorScheme.secondary` (avocado green) since it already appears green in the screenshots. If it already uses this color, leave it unchanged.

---

## What NOT to change

- Do not change stat card background colors or shapes
- Do not change the overall screen layout or spacing
- Do not add new UI elements, icons, or decorative graphics
- Do not change the project filter chip design
- Do not add a streak/activity grid (deferred to a separate spec)
- Do not change the bottom navigation bar
- Do not add any new dependencies — use only Compose animation APIs already available
