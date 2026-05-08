# KnitTools Light Theme & Vertical Stripes

Delta spec for adding a light theme alongside the existing dark theme, plus decorative vertical stripes on all screens.

---

## Light Theme

### Overview
Add a light theme as the **default** theme. The existing dark theme remains unchanged. Theme follows the same 70s Craft Revival aesthetic with warm, cream-based colors. No pure white anywhere.

### Color Palette (Light)

| Token | Hex | Description |
|-------|-----|-------------|
| **Background** | `#E8E4D0` | Warm cream (matches app icon background) |
| **Surface** | `#D2CDB5` | Cards, base surfaces |
| **SurfaceHigh** | `#BBB59A` | Elevated cards, tool/reference items |
| **SurfaceHighest** | `#A49D80` | Highest elevation, input fields |
| **Primary** | `#C45100` | Burnt orange — same as dark theme |
| **Secondary** | `#6B8A2E` | Darker avocado (adjusted for light bg contrast) |
| **Tertiary** | `#9A7B18` | Dark gold/mustard (adjusted — `#C9A435` too faint on light bg) |
| **DustyRose** | `#9E706E` | Deeper dusty rose (adjusted for light bg) |
| **Error** | `#C44D4D` | Same as dark theme |
| **TextPrimary** | `#2E2A1E` | Dark warm brown (not black) |
| **TextSecondary** | `#5C5643` | Medium brown |
| **TextMuted** | `#8A8370` | Muted brown |
| **NavBg** | `#DDD8C3` | Navigation bar background |
| **NavInactive** | `#A09880` | Inactive nav items |
| **NavActive** | `#C45100` | Active nav — same primary |

### Surface Hierarchy
Light theme inverts the dark theme logic — darker = more elevated:
- Background (lightest) → Surface → SurfaceHigh → SurfaceHighest (darkest)

### Text Colors
- No black text anywhere. TextPrimary is warm dark brown `#2E2A1E`.
- All text tokens shift to warm brown family.

### Navigation Bar
- Light/warm nav bar (`#DDD8C3`), not dark. Consistent with overall light aesthetic.

### Tool & Reference Accent Colors (Light)
Same mapping as dark theme but using adjusted values:

| Item | Color |
|------|-------|
| Gauge Converter / Needle Sizes | `#C45100` (Primary) |
| Increase-Decrease / Size Charts | `#6B8A2E` (Secondary) |
| Cast On / Abbreviations | `#9A7B18` (Tertiary) |
| Yarn Estimator / Chart Symbols | `#9E706E` (DustyRose) |
| Ravelry | TBD — `#5F8A8B` (muted teal) or `#8B4A6B` (plum) |

### Theme Selection
- DataStore `themeMode` already supports SYSTEM/LIGHT/DARK.
- **Default: LIGHT** (change from current DARK default).
- System mode follows Android system setting.

### Implementation Notes
- Add light color scheme to `Theme.kt` alongside existing `darkColorScheme`.
- Extended colors (`KnitToolsExtendedColors`) need light variants.
- No hardcoded colors in screen code — everything through theme tokens (already enforced).
- Wooden plus button image (`plus_button.webp`) works on both themes.

---

## Vertical Stripes

### Overview
Two decorative vertical stripes on the left edge of every screen background. Purely decorative, no interaction.

### Specification

| Property | Value |
|----------|-------|
| **Position** | Left edge, flush against screen edge (left: 0) |
| **Stripe 1 (outer)** | Width: 4dp, Color: Primary (`#C45100` light / `#C45100` dark) |
| **Stripe 2 (inner)** | Width: 4dp, Color: Tertiary (`#9A7B18` light / `#C9A435` dark) |
| **Gap between stripes** | 3dp (stripe 1: 0–4dp, stripe 2: 7–11dp) |
| **Vertical extent** | 20% from top, 20% from bottom (i.e. height = 60%, centered vertically) |
| **Z-order** | Behind all content (z-index 0) |
| **Scope** | All screens — stops at nav bar top edge, does not extend into nav bar |

### Implementation Notes
- Implement as a shared composable (e.g. `BackgroundStripes`) placed inside every `Scaffold` or `ToolScreenScaffold`.
- Stripes use theme colors so they automatically adapt to light/dark theme.
- Content padding (20dp) ensures stripes never overlap with any UI elements.
- Stripes are purely cosmetic `Box` elements with `Modifier.fillMaxHeight(0.6f)` and appropriate offset.
