# KnitTools Visual Redesign — "70s Craft Revival"

**Type:** Delta spec — visual theme only  
**Scope:** Colors, typography, component styling  
**Does NOT change:** Navigation, routes, Room DB, ViewModels, business logic, screen structure, feature set

---

## 1. Overview

Replace the current light warm beige/gold/dusty rose theme with a single unified dark "70s Craft Revival" theme.

**Current state:** The app has a single light theme (warm beige background, white cards, gold primary, dusty rose secondary). There is no dark theme.

**New state:** Replace the light theme entirely with a dark olive palette. Still a single theme — just dark instead of light.

**Design direction:** Dark olive background, warm earthy accents (burnt orange, avocado green, mustard gold), modern geometric fonts. Premium craft aesthetic — vintage warmth with contemporary typography.

---

## 2. Fonts

### Remove
- Manrope (all weights) — remove font files and all references

### Add
Two new font families, bundled as static font files in `res/font/`:

**Clash Display** (display/headline font)  
- Source: https://www.fontshare.com/fonts/clash-display  
- License: Fontshare (free for commercial use)  
- Weights needed: Medium (500), SemiBold (600), Bold (700)  
- Usage: Screen titles, project names, counter number, tool/reference card titles, nav labels

**General Sans** (body font)  
- Source: https://www.fontshare.com/fonts/general-sans  
- License: Fontshare (free for commercial use)  
- Weights needed: Regular (400), Medium (500), SemiBold (600)  
- Usage: Descriptions, labels, input fields, body text, buttons

### Font files to add to `res/font/`
```
clash_display_medium.ttf
clash_display_semibold.ttf
clash_display_bold.ttf
general_sans_regular.ttf
general_sans_medium.ttf
general_sans_semibold.ttf
```

### FontFamily definitions in Type.kt
```kotlin
val ClashDisplay = FontFamily(
    Font(R.font.clash_display_medium, FontWeight.Medium),
    Font(R.font.clash_display_semibold, FontWeight.SemiBold),
    Font(R.font.clash_display_bold, FontWeight.Bold),
)

val GeneralSans = FontFamily(
    Font(R.font.general_sans_regular, FontWeight.Normal),
    Font(R.font.general_sans_medium, FontWeight.Medium),
    Font(R.font.general_sans_semibold, FontWeight.SemiBold),
)
```

---

## 3. Type Scale

Update all 12 M3 typography roles in `Type.kt`:

| Role | Font | Size | Weight | Letter Spacing | Notes |
|------|------|------|--------|----------------|-------|
| displayLarge | ClashDisplay | 57sp | Bold | -0.25sp | |
| displayMedium | ClashDisplay | 45sp | Bold | 0sp | |
| displaySmall | ClashDisplay | 36sp | SemiBold | 0sp | |
| headlineLarge | ClashDisplay | 32sp | Bold | 0sp | |
| headlineMedium | ClashDisplay | 28sp | SemiBold | 0sp | |
| headlineSmall | ClashDisplay | 24sp | SemiBold | 0sp | |
| titleLarge | ClashDisplay | 22sp | SemiBold | 0sp | |
| titleMedium | GeneralSans | 16sp | SemiBold | 0.15sp | |
| titleSmall | GeneralSans | 14sp | Medium | 0.1sp | |
| bodyLarge | GeneralSans | 16sp | Normal | 0.5sp | |
| bodyMedium | GeneralSans | 14sp | Normal | 0.25sp | |
| bodySmall | GeneralSans | 12sp | Normal | 0.4sp | |
| labelLarge | GeneralSans | 14sp | SemiBold | 0.1sp | |
| labelMedium | GeneralSans | 12sp | SemiBold | 0.5sp | |
| labelSmall | GeneralSans | 11sp | SemiBold | 1.5sp | All-caps labels, same as before |

**Row Counter number (inline override):** `ClashDisplay, 115sp, Bold, fontFeatureSettings = "tnum"`  
**Project name (MERINO BEANIE):** `headlineMedium` with `textTransform = uppercase`, `letterSpacing = 2sp`  
**Pattern Repeat pill:** `labelSmall` (all-caps)  
**Bottom nav labels:** `labelSmall` (all-caps)

---

## 4. Color Palette

### Remove
- All current light theme colors
- All current dark theme colors
- The `KnitToolsExtendedColors` values will need new hex values

### Single unified palette in `Color.kt`

```kotlin
// === Backgrounds ===
val Background = Color(0xFF1E1E12)        // Main app background — dark olive
val BackgroundAlt = Color(0xFF252518)      // Slightly lighter, for contrast areas

// === Surfaces (dark khaki/olive cards — NOT white) ===
val Surface = Color(0xFF2E2E20)            // Base card surface
val SurfaceHigh = Color(0xFF3A3A2A)        // Elevated cards, tool/reference items
val SurfaceHighest = Color(0xFF454535)     // Highest elevation, input fields

// === Primary — Burnt Orange ===
val Primary = Color(0xFFC45100)            // Main action color, + button, CTAs
val PrimaryContainer = Color(0xFFD4722A)   // Lighter orange, gradients
val OnPrimary = Color(0xFFFFFFFF)          // Text/icons on primary

// === Secondary — Avocado Green ===
val Secondary = Color(0xFF8BA44A)          // Labels like "CURRENT ROW", section headers
val SecondaryMuted = Color(0xFF6B8A35)     // Dimmer green for less prominent labels
val SecondaryContainer = Color(0xFF3A4020) // Green-tinted container backgrounds

// === Tertiary — Mustard Gold ===
val Tertiary = Color(0xFFC9A435)           // Knitting tips, accent highlights
val TertiaryContainer = Color(0xFF3A3520)  // Quick tip card background

// === Text ===
val TextPrimary = Color(0xFFE8E4D0)        // Main text — warm cream
val TextSecondary = Color(0xFFB8B4A0)      // Descriptions, secondary info
val TextMuted = Color(0xFF8A866E)          // Muted text, chevrons, timestamps
val TextDisabled = Color(0xFF5A5840)       // Disabled state

// === Accent (carried from current design) ===
val DustyRose = Color(0xFFB8908F)          // Pro trial text, AI summary link, yarn card accent

// === Status ===
val Error = Color(0xFFC44D4D)
val ErrorContainer = Color(0xFF3A2020)
val Success = Color(0xFF8BA44A)            // Same as Secondary
val SuccessContainer = Color(0xFF3A4020)

// === Navigation ===
val NavBackground = Color(0xFF161610)      // Bottom nav background — very dark
val NavText = Color(0xFF6A664E)            // Inactive nav items
val NavActive = Color(0xFFC45100)          // Active nav item — burnt orange
val NavActiveBg = Color(0xFF3A2010)        // Active tab indicator background

// === Divider ===
val Divider = Color(0xFF3A3A2A)

// === Tool/Reference Card Accent Colors ===
// Each tool and reference item has a unique accent color for its TITLE text
val AccentGauge = Color(0xFFC45100)        // Burnt orange (= Primary)
val AccentIncDec = Color(0xFF8BA44A)       // Avocado (= Secondary)
val AccentCastOn = Color(0xFFC9A435)       // Mustard (= Tertiary)
val AccentYarnEst = Color(0xFFB8908F)      // Dusty rose
val AccentNeedleSizes = Color(0xFFC45100)  // Burnt orange
val AccentSizeCharts = Color(0xFF8BA44A)   // Avocado
val AccentAbbreviations = Color(0xFFC9A435)// Mustard
val AccentChartSymbols = Color(0xFFB8908F) // Dusty rose
```

### M3 Color Scheme mapping in `Theme.kt`

Map the above colors to M3 roles:

| M3 Role | Value |
|---------|-------|
| background | Background |
| surface | Surface |
| surfaceVariant | SurfaceHigh |
| surfaceContainerLow | Surface |
| surfaceContainer | SurfaceHigh |
| surfaceContainerHigh | SurfaceHighest |
| primary | Primary |
| primaryContainer | PrimaryContainer |
| onPrimary | OnPrimary |
| secondary | Secondary |
| secondaryContainer | SecondaryContainer |
| tertiary | Tertiary |
| tertiaryContainer | TertiaryContainer |
| onBackground | TextPrimary |
| onSurface | TextPrimary |
| onSurfaceVariant | TextSecondary |
| outline | TextMuted |
| outlineVariant | Divider |
| error | Error |
| errorContainer | ErrorContainer |

### Extended Colors update
Update `KnitToolsExtendedColors` values:
- `surfaceTint` → `SurfaceHighest`
- `secondaryOutline` → `Divider`
- `onSurfaceMuted` → `TextMuted`
- `brandWine` → `DustyRose`
- `inactiveContent` → `TextDisabled`

---

## 5. Theme Mode

### Replace light theme with dark
- The app currently has a single light theme. Replace it with a single dark theme using the palette above.
- Change the color scheme from `lightColorScheme(...)` to `darkColorScheme(...)` with the new values.
- `ThemeMode` enum in DataStore can remain for future use but currently the app always applies this one palette.

### Noise texture
- If there is a noise texture overlay in `MainActivity.kt` or theme setup, remove it — the new palette creates enough depth through surface layering.

---

## 6. Component-Specific Changes

### Bottom Navigation Bar (`KnitToolsBottomBar`)
- Background: `NavBackground` (#161610)
- Inactive icons/text: `NavText` (#6A664E)
- Active icon/text: `NavActive` (#C45100)
- Active indicator background: `NavActiveBg` (#3A2010)
- Labels: `labelSmall`, all-caps

### Row Counter Screen
- Background: `Background` (no Scaffold override needed, same as app background)
- Project name: `headlineMedium`, all-caps, `letterSpacing = 2.sp`, color `TextPrimary`
- Counter number: `ClashDisplay`, `115.sp`, `Bold`, color `TextPrimary`, `fontFeatureSettings = "tnum"`
- "CURRENT ROW" label: `labelSmall`, all-caps, color `Secondary`
- Plus button: gradient background `Primary` → `PrimaryContainer` (135deg), white "+" icon, `boxShadow` equivalent via `Modifier.shadow()`
- Minus / Undo buttons: `SurfaceHigh` background, `TextSecondary` icon color
- Pattern repeat pill: `SurfaceHigh` background, `TextMuted` text
- Stats row (stitches / time): `SurfaceHigh` background cards, `TextPrimary` values
- "Reset Counter": `TextMuted`
- "View AI summary": `DustyRose` color, italic
- Linked yarn chip: small `DustyRose` circle + `TextSecondary` name

### Project List Screen
- Cards: `SurfaceHigh` background
- Project name: `ClashDisplay`, color `TextPrimary`
- Row count: color `Primary`
- Section labels (ACTIVE / COMPLETED): `labelSmall`, color `Secondary`
- FAB: `Primary` background

### Tools List Screen
- "KnitTools" title: `ClashDisplay headlineMedium`, color `TextPrimary`
- "Pro trial" text: `DustyRose`, italic
- **Tool card titles: each card has a unique accent color (see Section 4)**:
  - Gauge Calculator → `AccentGauge` (burnt orange)
  - Increase / Decrease → `AccentIncDec` (avocado)
  - Cast On Calculator → `AccentCastOn` (mustard)
  - Yarn Estimator → `AccentYarnEst` (dusty rose)
- Tool card descriptions: `TextMuted`
- Tool card background: `SurfaceHigh`
- Tool card chevron: `TextMuted`
- Quick Tip card: `TertiaryContainer` background, `Tertiary` label, `TextSecondary` body
- Divider above Quick Tip: `Divider` color

### Calculator Screens (Gauge, Inc/Dec, Cast On, Yarn Estimator)
- Back arrow + title: `TextMuted` arrow, `ClashDisplay titleLarge` title
- "Paste instruction" link: `Tertiary` color
- Unit toggle (cm/inches): active segment gradient `Primary` → `PrimaryContainer`, inactive `SurfaceHigh`
- Section labels ("My Gauge", "STITCHES PER 10CM"): `labelSmall`, color `Secondary`
- Input fields: `SurfaceHigh` background, `TextMuted` unit suffix
- Calculate button: gradient `Primary` → `PrimaryContainer`, white text, `ClashDisplay`
- Result cards: `SurfaceHigh` background, result numbers `TextPrimary` in `ClashDisplay`

### Reference Hub Screen
- Title + description: same pattern as Tools List
- **Reference card titles: each card has a unique accent color**:
  - Needle Sizes → `AccentNeedleSizes` (burnt orange)
  - Size Charts → `AccentSizeCharts` (avocado)
  - Abbreviations → `AccentAbbreviations` (mustard)
  - Chart Symbols → `AccentChartSymbols` (dusty rose)
- Card descriptions: `TextMuted`
- Card background: `SurfaceHigh`

### Reference Detail Screens (Needle Sizes, Size Charts, Abbreviations, Chart Symbols)
- Table/list backgrounds: alternate between `Surface` and `SurfaceHigh`
- Search field: `SurfaceHighest` background
- Header text: `TextPrimary`
- Data text: `TextSecondary`

### Yarn Card Review Screen (OCR scan result)
- Background: `Background`
- Scanned field labels: `labelSmall`, color `Secondary`
- Scanned field values: `TextPrimary`, `ClashDisplay titleMedium`
- Edit/confirm buttons: `Primary` accent
- Card preview: `SurfaceHigh` background

### Yarn Card List Screen (saved yarn cards)
- Cards: `SurfaceHigh` background
- Yarn name: `ClashDisplay`, color `TextPrimary`
- Brand/details: `TextSecondary`
- Color swatch circles: preserve existing yarn color display

### Yarn Card Detail Screen
- Background: `Background`
- Section labels: `labelSmall`, color `Secondary`
- Values: `TextPrimary`
- Care symbols: preserve existing rendering, update background if needed to `SurfaceHigh`
- Photo: preserve existing photo display

### Settings Screen
- Follow same surface hierarchy: `Background` base, `SurfaceHigh` for setting items
- Toggle/switch: `Primary` when on

### Pro Upgrade Screen
- CTA button: gradient `Primary` → `PrimaryContainer`
- Feature list: `TextSecondary`
- Price: `ClashDisplay`, `Primary` color
- **Preserve the existing knitting image — do not remove it**

### Glance Widget
- Update widget colors to match new palette (dark background, cream text, orange accent)

---

## 7. Files to Modify

| File | Changes |
|------|---------|
| `ui/theme/Color.kt` | Replace all color definitions with new palette |
| `ui/theme/Theme.kt` | Single color scheme (no light/dark split), update extended colors |
| `ui/theme/Type.kt` | Replace Manrope with ClashDisplay + GeneralSans, new type scale |
| `ui/theme/Shapes.kt` | No changes expected (verify roundedness is sufficient) |
| `res/font/` | Remove Manrope files, add Clash Display + General Sans files |
| `ui/navigation/KnitToolsBottomBar.kt` | Dark nav colors |
| `ui/screens/CounterScreen.kt` | Counter styling updates |
| `ui/screens/ToolsListScreen.kt` | Accent-colored tool titles |
| `ui/screens/ReferenceHubScreen.kt` | Accent-colored reference titles |
| `ui/screens/GaugeConverterScreen.kt` | Input/result styling |
| `ui/screens/IncDecScreen.kt` | Input/result styling |
| `ui/screens/CastOnScreen.kt` | Input/result styling |
| `ui/screens/YarnEstimatorScreen.kt` | Input/result styling |
| `ui/screens/YarnCardReviewScreen.kt` | Surface colors, label colors |
| `ui/screens/YarnCardListScreen.kt` | Card styling |
| `ui/screens/YarnCardDetailScreen.kt` | Surface colors, label colors |
| `ui/screens/ProjectListScreen.kt` | Card styling |
| `ui/screens/SessionHistoryScreen.kt` | Card styling |
| `ui/screens/SettingsScreen.kt` | Surface colors |
| `ui/screens/ProUpgradeScreen.kt` | CTA styling |
| `ui/components/*` | Update any hardcoded colors (there should be none, but verify) |
| `widget/` | Update Glance widget colors |
| `MainActivity.kt` | Remove noise texture setup if present, switch to dark color scheme |
| `data/datastore/AppPreferences.kt` | ThemeMode enum can stay but is unused for color switching |

---

## 8. What NOT to Change

- Navigation structure (3 tabs, all routes)
- Screen layouts and component hierarchy
- Room database, entities, DAOs, migrations
- ViewModels and their scoping
- Business logic (calculators, parsers, billing)
- AI features (OCR, Nano)
- String resources (except if any reference theme mode names)
- Test files (unit tests and instrumented tests)
- Package structure

---

## 9. Implementation Order (Suggested)

1. Add new font files to `res/font/`
2. Update `Type.kt` with new FontFamily definitions and type scale
3. Update `Color.kt` with complete new palette
4. Update `Theme.kt` — single dark scheme, extended colors
5. Update `MainActivity.kt` — remove noise texture, simplify theme application
6. Update `KnitToolsBottomBar.kt` — dark nav styling
7. Update screen files one by one (Counter → Tools → Reference → Calculators → Others)
8. Update Glance widget
9. Verify no hardcoded colors remain: `grep -rn "#F8F4F0\|#FFFFFF\|#C9A96E\|#B8908F\|#2A1E17" --include="*.kt"`
10. Run app, visually verify each screen
