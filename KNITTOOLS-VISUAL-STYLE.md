# KnitTools visual style reference for RowTools

Source snapshot: KnitTools working tree, 2026-11-23. This is the Android app's “70s Craft Revival” style in its Compose UI, packaged artwork, Android launch surfaces and home-screen widget. Values below come from the current source and resources. Images retain their original pixels, and Android and Material defaults apply where the app leaves a property unset.

## Contents

- [Brand and visual hierarchy](#1-brand-and-visual-hierarchy)
- [Color system](#2-color-system)
- [Typography, shapes and spacing](#3-typography-shapes-and-spacing)
- [Icons and original artwork](#4-icons-and-original-artwork)
- [Shared component recipes](#5-shared-component-recipes)
- [Screen and surface references](#6-screen-and-surface-references)
- [Home-screen widget](#7-home-screen-widget)
- [Exact dimension and motion token catalog](#8-exact-dimension-and-motion-token-catalog)
- [Exact cross-stitch grids](#9-exact-cross-stitch-grids)
- [Material vector usage by source file](#10-material-vector-usage-by-source-file)

## 1. Brand and visual hierarchy

- Use a warm cream and khaki palette in Light and deep olive with warm cream lettering in Dark. Light is the app's default. Settings offers Light, Dark and System. Use the app's two fixed schemes. System mode selects between the same schemes.
- Burnt orange is the action color. Use `primary` for solid actions, graphical accents and the selected navigation icon. Use `primaryReadable` for small orange text on theme surfaces. A separate orange title accent is available for hub rows. Avocado, mustard, dusty rose and muted teal identify secondary content and craft details.
- Give the page `background`, repeated items `cardContainer` and the principal action area `actionContainer`. The last surface is pale or deep petrol and makes the orange controls stand out. Use tonal separation and hairline dividers. Reserve elevation for surfaces that already call for it, such as a dropdown or the labeled image-button pill.
- Let Barlow Semi Condensed and one dominant number or action establish the hierarchy. Uppercase localized section and navigation labels. Preserve the original casing of project names, including in the counter app bar. Use the shared text sizes below.
- The three-dimensional counter buttons, stitch-drawn content glyphs, fabric swatches and clay-style artwork are the distinctive graphics. Use each at the corresponding surface. Functional navigation, back, overflow, search and similar actions use the exact Material icons listed below. Tool and Library hub cards use accented title text and supporting copy.
- Carry over a KnitTools component when RowTools needs the corresponding control. The style also works with fewer tabs and flatter screen structure. Match both themes, icon variants, typographic roles and control states for the features RowTools actually exposes.

Sources: [UI rules](.claude/rules/ui.md), [CLAUDE.md](CLAUDE.md), [theme selection](app/src/main/java/com/finnvek/knittools/MainActivityTheme.kt), [preference defaults](app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt).

## 2. Color system

All opaque values use sRGB `#RRGGBB`. An opacity is a compositing instruction. For example, selected cards use the original orange at 7%, so the displayed pixel depends on the surface underneath. Use `MaterialTheme.colorScheme` for Material roles and `MaterialTheme.knitToolsColors` for extended roles. `KnitToolsTheme` installs both sets, `AppTypography` and `AppShapes`.

### Material 3 color scheme

| Role | Light value and source token | Dark value and source token |
| --- | --- | --- |
| `primary` | `#C45100` (`Primary`) | `#C45100` (`Primary`) |
| `onPrimary` | `#FFFFFF` (`OnPrimary`) | `#FFFFFF` (`OnPrimary`) |
| `primaryContainer` | `#D4722A` (`PrimaryContainer`) | `#D4722A` (`PrimaryContainer`) |
| `onPrimaryContainer` | `#161610` (`OnAccent`) | `#161610` (`OnAccent`) |
| `secondary` | `#394B18` (`LightSecondary`) | `#93AE4F` (`Secondary`) |
| `onSecondary` | `#FFFFFF` (`OnPrimary`) | `#161610` (`OnAccent`) |
| `secondaryContainer` | `#D0DDB5` (`LightSecondaryContainer`) | `#3A4020` (`SecondaryContainer`) |
| `onSecondaryContainer` | `#2E2A1E` (`LightTextPrimary`) | `#E8E4D0` (`TextPrimary`) |
| `tertiary` | `#9A7B18` (`LightTertiary`) | `#C9A435` (`Tertiary`) |
| `onTertiary` | `#161610` (`OnAccent`) | `#161610` (`OnAccent`) |
| `tertiaryContainer` | `#E8DFB5` (`LightTertiaryContainer`) | `#3A3520` (`TertiaryContainer`) |
| `onTertiaryContainer` | `#2E2A1E` (`LightTextPrimary`) | `#E8E4D0` (`TextPrimary`) |
| `surface` | `#D2CDB5` (`LightSurface`) | `#2E2E20` (`Surface`) |
| `surfaceVariant` | `#BBB59A` (`LightSurfaceHigh`) | `#3A3A2A` (`SurfaceHigh`) |
| `surfaceContainerLowest` | `#E8E4D0` (`LightBackground`) | `#1E1E12` (`Background`) |
| `surfaceContainerLow` | `#D2CDB5` (`LightSurface`) | `#2E2E20` (`Surface`) |
| `surfaceContainer` | `#C8C3A8` (`LightSurfaceMediumHigh`) | `#3A3A2A` (`SurfaceHigh`) |
| `surfaceContainerHigh` | `#BBB59A` (`LightSurfaceHigh`) | `#454535` (`SurfaceHighest`) |
| `surfaceContainerHighest` | `#A49D80` (`LightSurfaceHighest`) | `#454535` (`SurfaceHighest`) |
| `onSurface` | `#2E2A1E` (`LightTextPrimary`) | `#E8E4D0` (`TextPrimary`) |
| `onSurfaceVariant` | `#4C4634` (`LightTextSecondary`) | `#B8B4A0` (`TextSecondary`) |
| `background` | `#E8E4D0` (`LightBackground`) | `#1E1E12` (`Background`) |
| `onBackground` | `#2E2A1E` (`LightTextPrimary`) | `#E8E4D0` (`TextPrimary`) |
| `error` | `#C44D4D` (`Error`) | `#C44D4D` (`Error`) |
| `onError` | `#FFFFFF` (`OnPrimary`) | `#FFFFFF` (`OnPrimary`) |
| `errorContainer` | `#EAD0D0` (`LightErrorContainer`) | `#3A2020` (`ErrorContainer`) |
| `onErrorContainer` | `#2E2A1E` (`LightTextPrimary`) | `#E8E4D0` (`TextPrimary`) |
| `outline` | `#4A473C` (`LightTextMuted`) | `#A8A491` (`TextMuted`) |
| `outlineVariant` | `#C5C0A8` (`LightDivider`) | `#3A3A2A` (`Divider`) |

These are the explicit arguments passed to `lightColorScheme` and `darkColorScheme`. Other role values, including inverse roles, `scrim` and Material's own `surfaceTint`, come from the bundled Material implementation. The extended `surfaceTint` below is an independent KnitTools role.

### Extended KnitTools colors

| Role | Light value and source token | Dark value and source token |
| --- | --- | --- |
| `onPhotoScrim` | `#E8E4D0` (`TextPrimary`) | `#E8E4D0` (`TextPrimary`) |
| `surfaceTint` | `#A49D80` (`LightSurfaceHighest`) | `#454535` (`SurfaceHighest`) |
| `secondaryOutline` | `#C5C0A8` (`LightDivider`) | `#3A3A2A` (`Divider`) |
| `onSurfaceMuted` | `#4A473C` (`LightTextMuted`) | `#A8A491` (`TextMuted`) |
| `brandWine` | `#9E706E` (`LightDustyRose`) | `#B8908F` (`DustyRose`) |
| `tealAccent` | `#4A7172` (`LightRavelryTeal`) | `#5F8A8B` (`RavelryTeal`) |
| `inactiveContent` | `#5A5440` (`LightNavText`) | `#B0AC92` (`NavText`) |
| `navBarContainer` | `#DDD8C3` (`LightNavBackground`) | `#161610` (`NavBackground`) |
| `navBarIndicator` | `#EAD0B5` (`LightNavActiveBg`) | `#3A2010` (`NavActiveBg`) |
| `cardContainer` | `#D6D0B8` (`LightCardContainer`) | `#3A3A2A` (`SurfaceHigh`) |
| `selectedCardContainer` | `#C45100` at 7% | `#C45100` at 7% |
| `actionContainer` | `#C4D9D0` (`LightActionContainer`) | `#1A2E2B` (`ActionContainer`) |
| `sectionLabel` | `#7F5452` (`LightSectionLabel`) | `#B8908F` (`DustyRose`) |
| `tableHeaderContainer` | `#D6D0B8` (`LightCardContainer`) | `#454535` (`SurfaceHighest`) |
| `inputFieldContainer` | `#F3F0E3` (`LightInputField`) | `#454535` (`SurfaceHighest`) |
| `segmentedTrackOnDialog` | `#A49D80` (`LightSurfaceHighest`) | `#55553F` (`DarkSurfaceDialogTrack`) |
| `onSegmentedTrackOnDialog` | `#2E2A1E` (`LightTextPrimary`) | `#E8E4D0` (`TextPrimary`) |
| `sliderInactiveTrack` | `#B0A78A` (`LightSliderInactiveTrack`) | `#4E4E3C` (`DarkSliderInactiveTrack`) |
| `screenFieldContainer` | `#D6D0B8` (`LightCardContainer`) | `#454535` (`SurfaceHighest`) |
| `emptyStateText` | `#6A6350` (`LightEmptyStateText`) | `#A8A491` (`TextMuted`) |
| `fieldPlaceholderText` | `#6A6350` (`LightEmptyStateText`) | `#B8B4A0` (`TextSecondary`) |
| `paperSurface` | `#F7F4E8` (`LightPaperSurface`) | `#ECE8D8` (`PaperSurface`) |
| `paperInk` | `#B9B29A` (`PaperInk`) | `#B9B29A` (`PaperInk`) |
| `paperText` | `#4C4634` (`PaperText`) | `#4C4634` (`PaperText`) |
| `primaryReadable` | `#9A3F00` (`LightPrimaryReadable`) | `#E8823A` (`PrimaryReadable`) |
| `proMarkerContainer` | `#E8DFB5` (`LightProMarkerContainer`) | `#3A3520` (`ProMarkerContainer`) |
| `proMarkerContent` | `#6B5410` (`LightProMarkerContent`) | `#E3C766` (`ProMarkerContent`) |
| `proMarkerStitch` | `#9A7B18` (`LightProMarkerStitch`) | `#C9A435` (`ProMarkerStitch`) |
| `yarnSwatchNeutral` | `#BDB69A` (`LightYarnSwatchNeutral`) | `#4A4A38` (`YarnSwatchNeutral`) |
| `activityCellEmpty` | `#AFA98C` (`LightActivityCellEmpty`) | `#464633` (`ActivityCellEmpty`) |
| `transparentIndicator` | `#00000000` (transparent) | `#00000000` (transparent) |
| `activityRamp` | `#B8C47A`, `#93AE4F`, `#C9A435`, `#C45100` | `#6B8A35`, `#93AE4F`, `#C9A435`, `#D4722A` |
| `yarnPalette` | `LightYarnColors` below | `YarnColors` below |

Use `cardContainer` and `actionContainer` for their named purposes. The extended `navBarIndicator` is declared in the palette. Current navigation intentionally paints the indicator in `navBarContainer`.

### Hub title accents

| Role | Light value and source token | Dark value and source token |
| --- | --- | --- |
| `primary` | `#963C00` (`LightPrimaryTitle`) | `#F18C43` (`DarkPrimaryTitle`) |
| `secondary` | `#394B18` (`LightSecondary`) | `#93AE4F` (`Secondary`) |
| `tertiary` | `#6A5510` (`LightTertiaryTitle`) | `#C9A435` (`Tertiary`) |
| `rose` | `#754C4A` (`LightRoseTitle`) | `#C49B9A` (`DarkRoseTitle`) |
| `teal` | `#385D5E` (`LightTealTitle`) | `#80AAAB` (`DarkTealTitle`) |

### Indexed yarn colors

| Index | Hue | Light | Dark |
| --- | --- | --- | --- |
| 0 | Burnt orange | `#C45100` | `#CC6821` |
| 1 | Avocado | `#687A38` | `#8BA44A` |
| 2 | Mustard | `#8A7024` | `#C9A435` |
| 3 | Dusty rose | `#966765` | `#B8908F` |
| 4 | Terracotta | `#976949` | `#A57B5D` |
| 5 | Teal | `#517B6D` | `#5E8D7D` |
| 6 | Lavender | `#836A93` | `#9A82AA` |
| 7 | Rust | `#A85A3A` | `#B57357` |

Project thumbnails, chart segments and widget fabrics pick this palette by ID modulo eight. Preserve index ordering. Neutral yarn placeholders use `yarnSwatchNeutral`. The activity-cell ramp is ordered from lowest nonempty intensity to highest, with `activityCellEmpty` for inactive cells.

### Additional declared base tokens

| Token | Value | Meaning |
| --- | --- | --- |
| `SecondaryMuted` | `#6B8A35` | Dark activity ramp entry 1. |
| `Success` | `#8BA44A` | Declared status swatch. The shared Success message uses primary. |
| `LightActivityLow` | `#B8C47A` | Light activity ramp entry 1. |

Sources: [Color.kt](app/src/main/java/com/finnvek/knittools/ui/theme/Color.kt), [Theme.kt](app/src/main/java/com/finnvek/knittools/ui/theme/Theme.kt), [YarnColorPalette.kt](app/src/main/java/com/finnvek/knittools/ui/theme/YarnColorPalette.kt).

## 3. Typography, shapes and spacing

### Font files and text roles

Bundle Barlow Semi Condensed. The font family registers each bundled face as `OptionalLocal`, followed by the Android device `sans-serif` at the same weight. The bundled weights are Regular 400, Medium 500, SemiBold 600, Bold 700 and ExtraBold 800. Use the original TTF files and retain their SIL Open Font License. [Font directory](app/src/main/res/font), [bundled OFL](app/src/main/assets/licenses/barlow_semi_condensed_ofl.txt).

| Material role | Size (sp) | Weight | Tracking (sp) |
| --- | --- | --- | --- |
| `displayLarge` | 57 | 700 Bold | -0.25 |
| `displayMedium` | 45 | 700 Bold | — |
| `displaySmall` | 36 | 600 SemiBold | — |
| `headlineLarge` | 32 | 700 Bold | — |
| `headlineMedium` | 28 | 600 SemiBold | — |
| `headlineSmall` | 24 | 600 SemiBold | — |
| `titleLarge` | 22 | 600 SemiBold | — |
| `titleMedium` | 16 | 600 SemiBold | 0.15 |
| `titleSmall` | 14 | 500 Medium | 0.1 |
| `bodyLarge` | 16 | 400 Regular | 0.5 |
| `bodyMedium` | 14 | 400 Regular | 0.25 |
| `bodySmall` | 12 | 400 Regular | 0.4 |
| `labelLarge` | 14 | 600 SemiBold | 0.1 |
| `labelMedium` | 12 | 600 SemiBold | 0.5 |
| `labelSmall` | 11 | 600 SemiBold | 1.5 |

### App-specific role overrides

Each named role copies the indicated base. Only the listed properties change.

| App role | Base role | Size override | Weight override | Tracking override |
| --- | --- | --- | --- | --- |
| `counterExtraName` | `titleMedium` | `17.sp` | Inherited | Inherited |
| `counterExtraValue` | `headlineMedium` | Inherited | 700 Bold | Inherited |
| `insightsKicker` | `labelMedium` | `13.sp` | 500 Medium | Inherited |
| `insightsHeroLead` | `titleMedium` | `15.sp` | 500 Medium | Inherited |
| `insightsTrend` | `bodySmall` | `13.sp` | 500 Medium | Inherited |
| `insightsStatValue` | `titleLarge` | `20.sp` | 700 Bold | `(-0.03f).em` |
| `insightsProjectName` | `titleMedium` | `19.sp` | Inherited | Inherited |
| `insightsProjectSub` | `bodySmall` | `13.sp` | 500 Medium | Inherited |
| `insightsProjectDuration` | `titleMedium` | `19.sp` | 700 Bold | Inherited |
| `insightsEmptyTitle` | `headlineMedium` | `32.sp` | Inherited | Inherited |
| `insightsEmptyBody` | `bodyLarge` | `16.sp` | Inherited | Inherited |
| `insightsEmptyRangeTitle` | `headlineSmall` | `22.sp` | Inherited | Inherited |
| `insightsProCardTitle` | `titleMedium` | `17.sp` | Inherited | Inherited |
| `insightsProCardBody` | `bodyMedium` | `15.sp` | Inherited | Inherited |
| `insightsChartAxis` | `labelMedium` | `13.sp` | Inherited | Inherited |
| `insightsHeroPrimaryNumber` | `displayLarge` | `76.sp` | 800 ExtraBold | `(-0.055f).em` |
| `insightsHeroSecondaryNumber` | `displayMedium` | `46.sp` | 700 Bold | `(-0.055f).em` |
| `insightsHeroPrimaryUnit` | `headlineMedium` | `26.sp` | Inherited | Inherited |
| `insightsHeroSecondaryUnit` | `titleLarge` | `20.sp` | Inherited | Inherited |
| `projectCardName` | `titleLarge` | `18.sp` | Inherited | Inherited |
| `projectHeroCount` | `titleLarge` | Inherited | 700 Bold | Inherited |

Source: [Type.kt](app/src/main/java/com/finnvek/knittools/ui/theme/Type.kt).

The typography declarations leave line heights unset. A tracking dash in the table means the source leaves the property unset. Keep sizes in `sp` and respect system font scaling. For ordinary single-line app-bar titles, `ToolScreenScaffold` can fit `titleLarge` down to 16 sp. Navigation measures the longest localized label and lowers the shared size in 0.5 sp steps. Plain content section labels use `labelSmall` and `sectionLabel`. The localized uppercase conversion is the same for navigation and sections.

### Shape and spacing defaults

| Element | KnitTools treatment |
| --- | --- |
| Material small, medium, large | 8, 12 and 16 dp corner radius respectively. Material supplies shape defaults for other slots. |
| Main content cards | `shapes.large`, typically flat. |
| Tiles, list and option rows | `shapes.medium` where framed. Overview link rows sit directly on the page. |
| Labeled text and number inputs | 14 dp radius, 48 dp minimum height. |
| Search field | `shapes.medium`, with the screen field surface. |
| Text actions and compact controls | 48 dp minimum target where the shared components specify it. Icon artwork can be smaller within the target. |
| Pills and title selector | Fully rounded caps. The title selector uses 8 dp horizontal and 6 dp vertical padding and a 28 dp dropdown indicator. |
| Shared spacing | 4 dp compact, 8 dp standard, 12 dp content gap, 16 dp normal padding, 24 dp large padding. |
| Shared icon sizes | 20 dp standard icon, 18 dp information icon, 28 dp stitched glyph in overview rows. |
| Project thumbnail and hero photo | 14 dp thumbnail radius and 24 dp hero-photo radius. |
| Hairlines | Material `HorizontalDivider` with `outlineVariant` for editorial sections, guides and notices. |
| Overlays | The fold-aware dropdown surface has 3 dp shadow elevation. The labeled image pill has 3 dp shadow. Dialogs, sheets and other Material controls retain the defaults indicated by their component source. |

### Responsive shell

- Measure the space available to the content. Compact width is below 600 dp, Medium is 600 to below 840 dp and Expanded starts at 840 dp. Compact height is below 480 dp. Use the navigation rail at 600 dp width and 480 dp height or above. Use the 64 dp short bottom bar with the icon beside the label in a wide, short window. Use the normal bottom bar in compact width. The rail is designed to stay 80 dp wide, with a 64 dp label budget.
- Center the app bar and content within the same width policy. `Form` caps at 600 dp, `ListDetail` at 840 dp and `Chart` at 1200 dp. `Unrestricted` has no cap. `dialogWidth()` combines the Form cap with 83% width. The primary shell fits within safe drawing insets and can choose an unobstructed pane around a fold. Consume scaffold insets before passing content to nested scaffolds.
- `ToolScreenScaffold` uses a clean background and a transparent app bar in both scroll states. It supports a title that appears after the in-content title leaves view and permits multiline titles when requested.
- `COMPACT = 1.3` and `LARGE = 1.5` are the shared font-scale thresholds. Preserve adaptive wrapping and the visual and touch-target sizes independently.

Sources: [Shapes.kt](app/src/main/java/com/finnvek/knittools/ui/theme/Shapes.kt), [ComponentDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/ComponentDimens.kt), [AdaptiveLayout.kt](app/src/main/java/com/finnvek/knittools/ui/layout/AdaptiveLayout.kt), [AdaptiveContent.kt](app/src/main/java/com/finnvek/knittools/ui/layout/AdaptiveContent.kt), [ToolScreenScaffold.kt](app/src/main/java/com/finnvek/knittools/ui/components/ToolScreenScaffold.kt).

## 4. Icons and original artwork

### Material controls and navigation

The source uses legacy Compose Material `Icons` vectors through Material 3 `Icon`, with `material-icons-extended` and Compose BOM `2026.09.00` declared in the working tree. The cached BOM maps Material 3 to `1.4.0` and Material Icons Extended to `1.7.8`. Preserve family, exact glyph and auto-mirroring. `Icons.Default` is the source spelling for some filled icons.

| Destination order | Exact icon |
| --- | --- |
| Projects | `Icons.Outlined.FolderOpen` |
| Library | `Icons.Filled.AutoStories` |
| Tools | `Icons.Outlined.Build` |
| Insights | `Icons.Outlined.BarChart` |
| Settings | `Icons.Outlined.Settings` |

The bottom bar and rail use `navBarContainer`, `primary` on the selected icon and `primaryReadable` on its label. Inactive icon and label use `inactiveContent`. The indicator is painted in `navBarContainer`, so its fill blends into the bar. Bottom bar has 6 dp horizontal padding, single-line uppercase labels, `labelSmall` with 0 sp tracking and SemiBold selected or Medium inactive. The rail uses `labelSmall` with that role's tracking. The Kotlin files provide the exact width budgets and ellipsis behavior.

Shared Back uses `Icons.AutoMirrored.Filled.ArrowBack` tinted `outline`. Shared More options uses `Icons.Default.MoreVert` with inherited tint. The dropdown indicator is `Icons.Filled.ArrowDropDown` in `onSurfaceVariant`. The separate PDF reading-line icon is a 24 × 24 dp, 24 × 24 viewport vector with black source fill and path `M3,11h14v2H3zM21,12a3,3 0,1 1,-6 0a3,3 0,1 1,6 0`. Its display tint is supplied by the caller. See the complete source-use index in section 10.

Sources: [navigation destinations](app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt), [bottom bar](app/src/main/java/com/finnvek/knittools/ui/navigation/KnitToolsBottomBar.kt), [rail](app/src/main/java/com/finnvek/knittools/ui/navigation/KnitToolsNavigationRail.kt), [top-bar buttons](app/src/main/java/com/finnvek/knittools/ui/components/TopBarIconButtons.kt), [reading-line vector](app/src/main/res/drawable/ic_reading_line.xml), [version catalog](gradle/libs.versions.toml).

### Cross-stitch glyphs

Use these for meaningful craft content and related empty states. They are decorative when a visible label supplies the accessible name. Each nonempty cell consists of two crossing strokes with rounded caps. For a canvas of width `w` and height `h`, let `columns` be the longest row and `rows` the row count. The cell size is `min(w,h) / max(9.5, max(columns,rows) + 0.5)`. Center the entire cell grid. Begin each cross 0.18 cell in from the left and top edges of its cell. Span 0.64 cell in each direction. Use stroke width `max(0.30 × cell, 1.2 dp)`. `X` uses the given tint, `o` multiplies tint alpha by 0.55 and `.` leaves the cell empty.

| Glyph | Default accent | Common uses |
| --- | --- | --- |
| `PATTERN` | `primary` | Folded pattern page in counter tile, overview, Library empty state and onboarding |
| `YARN` | `secondary` | Yarn ball in counter tile, overview, yarn empty state and onboarding |
| `NOTES` | `brandWine` | Ruled notes page in counter tile, overview and onboarding |
| `PHOTOS` | `tertiary` | Photo outline in counter tile, overview, photo empty state and onboarding |
| `REMINDER` | `tealAccent` | Hollow bell in counter tile, overview and onboarding |
| `COUNTER` | `primary` | Round counter in onboarding, the Guide and the project-setup target row |
| `CLOCK` | `tealAccent` | Time-related rows in onboarding, the Guide and project setup |
| `CALCULATOR` | `secondary` | Calculator in onboarding toolbox |
| `CHART` | `tertiary` | Bar chart in onboarding toolbox |

Standard counter tile glyph is 56 dp, compact tile glyph 32 dp, overview row glyph 28 dp and full-screen empty-state glyph 96 dp. A cross-stitch empty state centers its content, with 32 dp outer padding, a 24 dp gap after the glyph, an optional `titleMedium` title, 8 dp from title to copy and 16 dp from copy to action. The exact nine grids are in section 9. [CrossStitchIcon.kt](app/src/main/java/com/finnvek/knittools/ui/components/CrossStitchIcon.kt), [CrossStitchDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/CrossStitchDimens.kt).

### Raster and Android resources

Reuse the original alpha-bearing images for pixel and shading fidelity. Their baked lighting, recessed symbols and colored rim belong to the asset. The circular stamp includes the KnitTools wordmark and KT monogram. If RowTools needs its own lockup, use the stamp as the style reference and match its composition with RowTools lettering.

| Asset | Pixels | Visible artwork and use |
| --- | --- | --- |
| [counter_plus_button.webp](app/src/main/res/drawable-nodpi/counter_plus_button.webp) | 500 × 500 | Burnt-orange round button with recessed plus. Main increment, additions and New Project pill. |
| [counter_minus_button.webp](app/src/main/res/drawable-nodpi/counter_minus_button.webp) | 500 × 500 | Avocado-green round button with recessed minus. Main decrement. |
| [counter_undo_button.webp](app/src/main/res/drawable-nodpi/counter_undo_button.webp) | 500 × 500 | Mustard round button with a recessed curved arrow. Undo. |
| [counter_continue_button.webp](app/src/main/res/drawable-nodpi/counter_continue_button.webp) | 500 × 500 | Burnt-orange round button with recessed play triangle. Continue. |
| [splash_logo.webp](app/src/main/res/drawable-nodpi/splash_logo.webp) | 500 × 500 | Orange concentric KnitTools stamp on transparency. App launch and Welcome. |
| [onboarding_welcome_hero.webp](app/src/main/res/drawable-nodpi/onboarding_welcome_hero.webp) | 1200 × 800 | Orange knitting on needles with orange and green yarn balls. |
| [onboarding_toolbox_hero.webp](app/src/main/res/drawable-nodpi/onboarding_toolbox_hero.webp) | 1200 × 800 | Open warm-toned toolbox of yarn, needles and tools. |
| [onboarding_pro_hero.webp](app/src/main/res/drawable-nodpi/onboarding_pro_hero.webp) | 1200 × 800 | Wooden crate of colored yarn and a knitted swatch. |
| [onboarding_project_ready.webp](app/src/main/res/drawable-nodpi/onboarding_project_ready.webp) | 1200 × 800 | Woven project basket with orange knitting and colored stitch markers. |
| [onboarding_craft_knit.webp](app/src/main/res/drawable-nodpi/onboarding_craft_knit.webp) | 480 × 480 | Orange swatch, needles and yarn. |
| [onboarding_craft_crochet.webp](app/src/main/res/drawable-nodpi/onboarding_craft_crochet.webp) | 480 × 480 | Green swatch, hook and yarn. |
| [onboarding_craft_both.webp](app/src/main/res/drawable-nodpi/onboarding_craft_both.webp) | 480 × 480 | Intersecting tools and a multicolored yarn ball. |
| [widget_knit_stitch_tile.png](app/src/main/res/drawable-xxxhdpi/widget_knit_stitch_tile.png) | 60 × 44 | Faint white stockinette stitch, repeated and tinted by the widget. |
| Launcher foreground PNGs | 108, 162, 216, 324 and 432 px square | `ic_launcher_foreground.png` in `mipmap-mdpi`, `-hdpi`, `-xhdpi`, `-xxhdpi` and `-xxxhdpi` respectively. The adaptive icon uses the same foreground as its monochrome layer. |
| [ic_launcher-playstore.png](app/src/main/ic_launcher-playstore.png) | 512 × 512 | Existing store icon at app-module level. |

The launcher background is `#E8E1C5`. The Compose Light page is `#E8E4D0`. The fixed Android splash is `#1E1E12` in every app theme, with a 10% inset around `splash_logo` and light system-bar icons during the splash. The loaded app follows the selected theme and uses edge-to-edge with transparent app-chosen system-bar scrims and theme-appropriate system icons. [Launcher definition](app/src/main/res/mipmap-anydpi/ic_launcher.xml), [splash drawable](app/src/main/res/drawable/icon_splash.xml), [platform theme XML](app/src/main/res/values/themes.xml), [Android colors](app/src/main/res/values/colors.xml), [MainActivity.kt](app/src/main/java/com/finnvek/knittools/MainActivity.kt).

### Fabric and other drawn artwork

- `FabricSwatch` draws soft, dimensional strands with per-strand shading and a radial black vignette. Knitting has rows of V-shaped stockinette. Crochet has staggered pairs of slanted paths. Yarn swatches use twisted diagonal plies at −35°. Reference units are based on a 72-unit project swatch and a 48-unit yarn swatch. Section 8 records all fabric dimensions. `shade(a)` moves each RGB channel towards white by `a` for a nonnegative value and multiplies by `1 + a` for a negative value. Alpha is preserved. Exact Bézier geometry, gradient stops and draw order are in the sources. [FabricSwatch.kt](app/src/main/java/com/finnvek/knittools/ui/components/FabricSwatch.kt), [FabricStrokes.kt](app/src/main/java/com/finnvek/knittools/ui/components/FabricStrokes.kt).
- `ProjectThumbnail` clips the craft-specific fabric under an optional center-cropped photo. Project colors use `palette[projectId.mod(palette.size)]`. A yarn thumbnail with no image uses the neutral yarn token and yarn-ply artwork. Project and yarn overview placeholders put a matching swatch behind the add-photo pill at 55% and 30% opacity respectively. [ProjectThumbnail.kt](app/src/main/java/com/finnvek/knittools/ui/components/ProjectThumbnail.kt), [OverviewSection.kt](app/src/main/java/com/finnvek/knittools/ui/components/OverviewSection.kt), [YarnColorPalette.kt](app/src/main/java/com/finnvek/knittools/ui/theme/YarnColorPalette.kt).
- Reference symbols have their own meaning-driven artwork. `CareSymbolIcon` draws laundry-care marks with Canvas. `ChartSymbolScreen` draws knitting chart cells in a Canvas with the selected symbol's strokes. `PatternPageSketch` draws the on-paper instructional sample from the paper and ink tokens. `InsightsChart`, `InsightsProjectFabric` and `InsightsSections` draw cloth-textured bars or cells using the yarn palette and secondary chart tokens. [Care symbols](app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbolIcon.kt), [chart symbols](app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt), [paper sketch](app/src/main/java/com/finnvek/knittools/ui/components/PatternPageSketch.kt), [Insights chart](app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsChart.kt), [daily fabric](app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsProjectFabric.kt).

Fabric shading amounts use `FabricStrokes.shade` with the base yarn color. The V stitch uses left leg +0.16 and right leg −0.02, with an additional +0.12 highlight and −0.10 shadow on those legs respectively. The knit edge is −0.28. Crochet shades its long strand +0.14 and −0.08, and the short strand +0.30 and +0.04. The crochet edge is −0.32. The yarn strand starts at +0.10, with an additional +0.16 highlight, a −0.06 dark edge color calculation on the base, and a −0.34 outline. The black vignette reaches 28% opacity at the rim. Preserve the original stroke paths when reusing these patterns. [FabricSwatch.kt](app/src/main/java/com/finnvek/knittools/ui/components/FabricSwatch.kt).

## 5. Shared component recipes

| Pattern | Exact appearance to carry over |
| --- | --- |
| Main action | Use a filled Material action in `primary` with `onPrimary`. The prominent image-control family uses the four packaged WebPs, `ContentScale.Fit` and its original press response. `LabeledCounterImageButton` uses a fully rounded petrol `actionContainer` pill, 3 dp shadow, `labelLarge` in `primaryReadable`, 20 dp start and 6 dp end text inset. |
| Secondary and destructive actions | Use quiet text or outlined Material actions where the reference calls for them. `CancelButton` is in `onSurfaceVariant`. `ConfirmationDialog` uses a question, neutral Cancel and a confirm action colored `primary` or `error` for a destructive action. See the components for the final layout and default button styles. |
| Hub or setting entry | `HubListItem` uses a 16 dp radius, 16 dp padding and zero elevation on `cardContainer`. Its `titleLarge` uses the assigned title accent or `onSurface`. Descriptions use `bodyMedium` and `onSurfaceVariant`. The 24 dp trailing chevron uses `onSurfaceMuted`. An optional leading icon uses 20 dp art on a 40 dp `primaryContainer` circle. Settings groups use `SectionLabel`. Copy the assigned title accent by role. The Tools and Library hubs offer text-led entries. |
| Editorial section | Use `OverviewSectionHeader` with `outlineVariant` hairline, section label and right-aligned `OverviewTextAction` in `labelLarge` and `primaryReadable`. `OverviewLinkRow` uses `titleMedium` and optional `bodyMedium` with a trailing affordance for actionable rows. `OverviewEmptyText` uses `bodyMedium` and `emptyStateText`. |
| Contextual notice | Flat text between two hairlines. Use `bodyMedium` in `onSurfaceVariant`, a neutral dismissal and a right-aligned `primaryReadable` action. Both actions use `labelLarge` and a minimum 48 dp target. |
| List card selected state | `cardContainerColor(selected)` returns `cardContainer` for ordinary items and 7% `primary` for a selected item. Preserve the shared check affordance. The multi-select action bar is a single low row of text actions on `cardContainer`, with destructive text in `error`. |
| Text and number fields | `LabeledTextField` and `NumberInputField` put the label and optional Info icon above the field. Use `inputFieldContainer` in card, dialog and sheet, and `screenFieldContainer` for search on the page. Keep the same field fill on error. Shared Material indicator colors are transparent. The 14 dp custom outline is 1 dp idle with `outlineVariant` at 50%, or 2 dp focused in `primaryContainer`. Error uses `error` for the border and supporting text. A disabled fill is 40% of the normal fill. The supporting line starts at the field edge with 4 dp top gap. |
| Segmented selector | Full width. Use a flat 6 dp padded track in `surfaceVariant` on the page or `segmentedTrackOnDialog` inside a dialog. Selected segment is solid `primary` with `onPrimary` and `labelMedium`. Inactive type is `bodySmall`, using `onSurfaceVariant` in the normal track or `onSegmentedTrackOnDialog` in a dialog. Pending status uses a 1.5 dp orange outline. Use pill shapes in a regular row, or 16 dp outer and 12 dp inner corners in a grid or at large font scale. Targets are at least 48 dp high. |
| Form and sheet | Single-field edit uses a dialog. Multi-field forms use `FormSheet` on `surface`, `headlineSmall` title, 20 dp horizontal inset, 32 dp bottom padding and 12 dp field spacing. Option rows use medium corners, 16 dp horizontal and 12 dp vertical padding and 56 dp minimum height. Pair neutral Cancel with the primary confirm at the trailing side. |
| Calculator input and result | Group inputs in `ToolInputCard` with `SectionLabel`. `ResultCard` uses `actionContainer`, a 1.5 dp `primary` outline at 30% and 16 dp corners. `ResultNumberInset` uses `surfaceContainerLowest` at 20% and 20 dp padding. The waiting state is a centered `bodyMedium` line in `emptyStateText` with a 1.5 dp dashed outline, 6 dp dash and gap, and 16 dp corner radius. |
| Progress | `ProjectProgressBar` is 4 dp high with fully rounded ends, `onSurface` at 10% for the track and `primaryReadable` for the fill. Projects list, Continue and the counter target use this component. The widget uses a separate day/night track at 15% and its own fill. |
| Status and markers | `StatusMessage` uses `surfaceContainerLow`, 16 dp corners and padding, 20 dp icon and 12 dp spacing. Info and Success use `primary` and their own outline icons, Error uses `error`. Default `BadgePill` uses `actionContainer`, `primaryReadable`, `labelSmall` and localized uppercase. It is fully rounded with 8 dp horizontal and 4 dp vertical padding. Apply Pro's stitch motif only where a `ProMarker` or `LaunchPriceTag` is called for. |
| Pro and trial stitch tags | `ProMarker` is 22 dp high, 8 dp padded horizontally, with 4 dp corners and a 1 dp dashed inner stroke inset 2 dp. Dash and gap are 3 and 2 dp. PRO has a fill, TRIAL is transparent. Both use the same theme-specific content and stitch tokens and `labelSmall`. The price tag has 5 dp outer inset, 14 dp padding and a 1.5 dp outline with 6 dp dash, 4 dp gap and 12 dp stitch corner. |

Component sources: [shared components](app/src/main/java/com/finnvek/knittools/ui/components), [FormSheet.kt](app/src/main/java/com/finnvek/knittools/ui/components/FormSheet.kt), [SegmentedToggle.kt](app/src/main/java/com/finnvek/knittools/ui/components/SegmentedToggle.kt), [TextFieldColors.kt](app/src/main/java/com/finnvek/knittools/ui/components/TextFieldColors.kt), [NumberInputField.kt](app/src/main/java/com/finnvek/knittools/ui/components/NumberInputField.kt), [OverviewSection.kt](app/src/main/java/com/finnvek/knittools/ui/components/OverviewSection.kt).

## 6. Screen and surface references

### Counter and project workspace

- On the main counter, let the localized number dominate. Use `displayMedium` with Bold, tabular figures (`tnum`) and a 115 sp regular target, fitted towards 48 sp when constrained. The rolling digits use `onSurface`. Give the hero 24 dp side padding. Keep the row or round label and relevant target or repeat information close to the number. Keep the active timing strip within the hero when present.
- The regular stack pairs the green minus on the left and orange plus on the right, with a clear mustard Undo beneath the controls. Plus uses 125 dp artwork, minus 123 dp shifted down 1 dp, and both have nominal 144 dp touch slots. Undo is normally 92 dp. The layout measures available height and text, choosing spacious stacked, compact stacked or side-by-side once both stacks cannot fit and at least 552 dp is available. In side-by-side the number and controls share a row and Undo fits between the buttons under the step ring. At 600 × 840 dp or larger use the large metric set at 1.4× where the metrics specify scaling. The stacked hero can expand at most 64 dp beyond its measured minimum.
- Secondary steppers use flat circular faces and a drawn plus or minus. The line spans 76% of the symbol box and the rounded stroke is 14% of its size. In the extra-counter variant use `surfaceContainerHighest` in Light and `surfaceVariant` in Dark for the circle. Increment uses `primary` and decrement `onSurfaceVariant`. Both have a matching ring with the same width as the symbol stroke. The disabled symbol and outline use 38% alpha, subdued 64%. Minor step controls retain their own sizes from `CounterDimens` and `PatternViewerDimens`.
- Keep extra counter cards around 88 dp minimum height with 16 dp corners, 22 dp horizontal and 18 dp vertical padding, 56 dp button targets, 42 dp button faces and 20 dp symbols. The project-content grid uses five stitched tiles in 2 + 2 + 1 up to a 360 dp grid width, or 3 + 2 using full width from 480 dp. Tile padding and gap are 12 dp. Set explicit minimum areas for the main controls before arranging supplemental content.
- Projects uses flat tonal list cards, a selected folder title selector and a petrol Continue hero. Keep the orange count, matching track and play asset in the hero. The action art there is 64 dp in a 72 dp target. Regular cards use 72 dp thumbnails, 56 dp in the compact case, a 14 dp thumbnail-to-text gap and 10 dp card spacing. The grid gains a second column at 760 dp of list width. New Project uses the orange plus together with the text pill, and can move into the bar in a short window.
- Project Overview and yarn detail use a cropped hero photo or a low 96 dp fabric placeholder, followed by clear unboxed sections. The regular photo is 210 dp high. The overview has 16 dp side padding, 20 dp between sections and 56 dp minimum link rows. The project overview's Continue hero follows the same treatment as the Projects hero. Use `primaryReadable` on small trailing text actions. Yarn quantity reuses the plus and minus image buttons with 44 dp art and 48 dp targets.

Sources: [Counter hero layout](app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterHeroLayout.kt), [Counter workspace](app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt), [project content tiles](app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterProjectContentCards.kt), [Continue card](app/src/main/java/com/finnvek/knittools/ui/components/ContinueProjectCard.kt), [Projects screen](app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt), [project overview](app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectOverviewScreen.kt).

### Library, Tools, Settings, Guide and Insights

| Screen | Visual reference |
| --- | --- |
| Library | Wide, text-led tonal navigation rows. Its first three titles use Primary, Secondary and Tertiary accents. Reference entries use the scheme-specific accent set. Saved patterns and yarn use their own card, thumbnail, selection and status components. Photos use a grid and existing filter pill. |
| Tools | Grouped tonal calculator rows using Primary, Secondary, Tertiary, Rose and Teal title accents. Individual tools share screen scaffolds, labeled input groups and the petrol result pattern. |
| Settings | Warm page, shared uppercase section headings and quiet option rows. Theme and preference controls use the app's own surfaces and accents. |
| Guide | Flat, hairline-separated topics inside `ToolScreenScaffold`. Root uses `OverviewLinkRow`. Detail uses `titleMedium` and `bodyMedium`. |
| Insights | Flat editorial layout on `background`, 16 dp side padding and 32 dp bottom padding. The app bar shows the range, with the project filter as a tonal pill at the right. Use the distinct hero typography and knitted chart bars, tight left-aligned stats, hairlines, one mix bar per view and project colors by stable ID. Bars round to 3 dp. Knit overlay clips inside the bar silhouette, uses 14% scrim shading and is skipped below 14 dp height. Read `InsightsDimens` for chart, timeline, grid and loading skeleton dimensions. |

Sources: [Library](app/src/main/java/com/finnvek/knittools/ui/screens/library/LibraryScreen.kt), [Tools](app/src/main/java/com/finnvek/knittools/ui/screens/home/HomeScreen.kt), [Settings](app/src/main/java/com/finnvek/knittools/ui/screens/settings/SettingsScreen.kt), [Guide](app/src/main/java/com/finnvek/knittools/ui/screens/guide/GuideScreen.kt), [Insights](app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsScreen.kt).

### PDF, photos and notes

- Render the PDF page in its original colors onto a white bitmap (`#FFFFFF`), including in Dark. The project reader puts the reading-line control in the toolbar and keeps page movement, counter and bookmark affordances by the document in their existing bottom-bar positions. Reading mode shows the document title. Markup brings in the annotation tools. Use 48 dp controls, a 48 dp compact toolbar and the bottom-bar insets listed in section 8. The project reading bottom bar uses 3 dp tonal elevation.
- The horizontal reading band is `primary` at 14% opacity and 4.5% of overlay height. The vertical band is `tertiary` at 10% opacity and 3.5% of overlay width. Each band has a 24 dp minimum thickness. The controls use 48 dp handles with 6 dp corner radius. The sample page sketch has its own 16% horizontal band on the paper tokens.
- PDF annotation colors are document-tool colors. In Android ARGB, default pen is `#FF1F1F1F`, highlighter `#60FFD54F`, text background `#FFFFFFFF` at 85% and callout `#FF6750A4` with 16% background. The user palette is black `#1F1F1F`, red `#D32F2F`, blue `#1976D2`, green `#388E3C` and amber `#FFC107`. Chart highlighting uses 35% alpha and the grid uses `outline`. Annotation geometry is calibrated against a 1000-unit reference page. Section 8 records the widths and toolbar dimensions.
- Photos use real image crops, theme fabric placeholders as appropriate, and warm light `onPhotoScrim` for legibility over photography. The notes editor uses a transparent app bar, a Form-width page, normal-cased `ProjectNameLabel`, and a full-size transparent multiline field with 16 dp horizontal and top padding. Text uses `bodyLarge` in `onBackground`. Placeholder uses `onSurfaceVariant`. Focused and unfocused indicators are transparent. Apply IME padding inside the scaffold.

Sources: [PDF reader](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt), [PDF bottom bar](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerBottomBar.kt), [markup tokens](app/src/main/java/com/finnvek/knittools/ui/theme/PatternAnnotationTokens.kt), [PDF renderer](app/src/main/java/com/finnvek/knittools/data/storage/PdfPageRenderer.kt), [photo surfaces](app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoComponents.kt), [notes editor](app/src/main/java/com/finnvek/knittools/ui/screens/notes/NotesEditorScreen.kt).

### Onboarding, Pro and motion

The welcome, toolbox, ready and Pro graphics are baked transparent clay-style illustrations with warm craft colors. Use `CraftIllustration` and the original assets. The shared illustration heights include 140 dp Welcome, 150 dp Pro and 220 dp for the general height. Below 480 dp viewport height the short variant caps at 84 dp. Omit it below 360 dp or at a short height with font scale above 1.3. Craft-choice art is 88 dp. Use the mustard stitched price tag and flat editorial sections when matching the Pro or trial surfaces. Keep celebration or progress artwork tied to its own event.

| Motion | KnitTools source treatment |
| --- | --- |
| Main image-button press | 90 ms tween to a 1.8% scale reduction and a 1.5 dp downward translation. Disabled artwork stays at 62% opacity. |
| Rolling digits | Per-digit directional slide as the value changes, 200 ms `FastOutSlowInEasing`. |
| Calculator result | 6 dp offset with fade, 200 ms enter and 150 ms exit using `FastOutSlowInEasing`. |
| Counter step feedback | 900 ms display window with the fixed space in `CounterDimens`. |
| Illustration and choices | One-off illustration entrance with 16 dp rise and 450 ms entrance token. Group entrance uses 10 dp rise, 320 ms duration, 120 ms base delay and 55 ms per indexed item. Selection token is 180 ms. |
| Onboarding navigation | 320 ms horizontal transition across 15% of the container, plus fade. Outgoing fade uses half the duration. Demo count and reading line use 220 ms and 360 ms respectively. |
| Cold splash exit | 760 ms in total, with a 640 ms drawing reveal and 120 ms fade. Outer sweep, inner wording and center monogram are layered by `SplashLogoDrawable`. |

Sources: [CraftIllustration.kt](app/src/main/java/com/finnvek/knittools/ui/components/CraftIllustration.kt), [OnboardingMotion.kt](app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingMotion.kt), [ProUpgradeScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/pro/ProUpgradeScreen.kt), [SplashLogoDrawable.kt](app/src/main/java/com/finnvek/knittools/ui/SplashLogoDrawable.kt), [RollingCounter.kt](app/src/main/java/com/finnvek/knittools/ui/components/RollingCounter.kt).

## 7. Home-screen widget

The Glance widget has its own day/night mapping. Use the launcher’s widget corner radius where the API supports it. The base fill is `#D2CDB5` in Light and `#2E2E20` in Dark. Primary text is `#2E2A1E` or `#E8E4D0`, secondary text `#4C4634` or `#B8B4A0`, and the widget's `surfaceVariant` is `#4A473C` or `#A8A491`. Action colors are `#C45100` and white in both modes. The widget's tertiary role is `#D4722A`. The live stitch tile repeats across the whole card and is tinted with the day/night project yarn palette. The static launcher preview samples avocado at `#687A38` / `#8BA44A`.

Responsive modes are 120 × 48, 120 × 80, 160 × 160 and 300 × 160 dp. The two small variants offer tight and stacked readouts. The medium and large variants arrange the project label, count and shared 3D plus/minus image controls within the size limits. Use the widget's Glance text styles and dimensions rather than the Compose counter's 115 sp type. The provider caps resizing at 360 × 200 dp on API 31 and above and supplies the wide XML preview there. All widget dimensions and type sizes are in section 8.

Sources: [CounterWidget.kt](app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt), [widget preview](app/src/main/res/layout/widget_counter_preview.xml), [light preview colors](app/src/main/res/values/widget_preview_colors.xml), [dark preview colors](app/src/main/res/values-night/widget_preview_colors.xml), [repeating stitch drawable](app/src/main/res/drawable/widget_knit_texture.xml), [API 31 widget provider](app/src/main/res/xml-v31/counter_widget_info.xml).

## 8. Exact dimension and motion token catalog

The following tables give the current declared Kotlin values by source file. A number with no unit is a fraction, scalar, millisecond duration or count as indicated by the token name. A token may be a declared option, geometry input or legacy value. The actual component path above shows which values apply at a given call site. App and widget sizing are independent. Screen-local dimensions and conditional layout can also be found in the linked component sources.

### ComponentDimens

Source: [ComponentDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/ComponentDimens.kt).

| Token | Source value |
| --- | --- |
| `CompactSpacing` | `4.dp` |
| `StandardSpacing` | `8.dp` |
| `ContentSpacing` | `12.dp` |
| `ContentPadding` | `16.dp` |
| `LargeContentPadding` | `24.dp` |
| `StandardIconSize` | `20.dp` |
| `AnimatedResultOffset` | `6.dp` |
| `AnimatedResultEnterDurationMillis` | `200` |
| `AnimatedResultExitDurationMillis` | `150` |
| `SegmentedContainerPadding` | `6.dp` |
| `SegmentedItemTextInset` | `6.dp` |
| `SegmentedItemMinHeight` | `48.dp` |
| `FlushTextButtonPadding` | `PaddingValues(vertical = ButtonDefaults.TextButtonContentPadding.calculateTopPadding())` |
| `SelectionBarActionMinHeight` | `48.dp` |
| `ResultInsetPadding` | `20.dp` |
| `ResultCardBorderWidth` | `1.5.dp` |
| `FlatElevation` | `0.dp` |
| `TitleSelectorHorizontalPadding` | `8.dp` |
| `TitleSelectorVerticalPadding` | `6.dp` |
| `TitleSelectorIndicatorSize` | `28.dp` |
| `TitleSelectorShape` | `RoundedCornerShape(percent = 50)` |
| `FormSheetHorizontalPadding` | `20.dp` |
| `FormSheetBottomPadding` | `32.dp` |
| `FormSheetItemSpacing` | `12.dp` |
| `FormSheetActionMinHeight` | `48.dp` |
| `SheetOptionHorizontalPadding` | `16.dp` |
| `SheetOptionVerticalPadding` | `12.dp` |
| `SheetOptionMinHeight` | `56.dp` |
| `InfoIconSize` | `18.dp` |
| `ResultPlaceholderStrokeWidth` | `1.5.dp` |
| `ResultPlaceholderDash` | `6.dp` |
| `ResultPlaceholderCornerRadius` | `16.dp` |

### CounterDimens

Source: [CounterDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/CounterDimens.kt).

| Token | Source value |
| --- | --- |
| `StepRingMinimumSize` | `48.dp` |
| `StepRingMaximumSize` | `96.dp` |
| `StepRingTextPadding` | `8.dp` |
| `StepRingButtonGap` | `8.dp` |
| `StepFeedbackWidth` | `72.dp` |
| `STEP_FEEDBACK_DURATION_MILLIS` | `900L` |
| `LabeledImageButtonElevation` | `3.dp` |
| `LabeledImageButtonTextStart` | `20.dp` |
| `LabeledImageButtonTextEnd` | `6.dp` |
| `ScreenHorizontalPadding` | `24.dp` |
| `ContentBottomPadding` | `24.dp` |
| `WorkspaceSectionSpacing` | `20.dp` |
| `WorkspaceGroupSpacing` | `16.dp` |
| `HeroBottomBreathingSpace` | `24.dp` |
| `CounterProjectRevealGap` | `48.dp` |
| `CounterHeroTopWeight` | `0.32f` |
| `CounterHeroBottomWeight` | `0.68f` |
| `HeroButtonSpacing` | `64.dp` |
| `HeroButtonCompactSpacing` | `8.dp` |
| `HeroMaxExtraHeight` | `64.dp` |
| `ShortHeroMinWidth` | `552.dp` |
| `ShortHeroSpacing` | `16.dp` |
| `ShortHeroUndoSize` | `80.dp` |
| `ShortHeroUndoGap` | `8.dp` |
| `LargeHeroMinWidth` | `600.dp` |
| `LargeHeroMinHeight` | `840.dp` |
| `LargeHeroScale` | `1.4f` |
| `CounterMainNumberFontSize` | `115.sp` |
| `CounterMainNumberMinimumFontSize` | `48.sp` |
| `MainNumberMinHeight` | `48.dp` |
| `CounterMainNumberFitIterations` | `8` |
| `CounterTargetHelperSpacing` | `8.dp` |
| `CounterTargetHelperFontSize` | `19.sp` |
| `CounterRepeatMinWidth` | `232.dp` |
| `CounterRepeatMaxWidth` | `256.dp` |
| `CounterRepeatHeight` | `48.dp` |
| `CounterRepeatCornerRadius` | `24.dp` |
| `CounterRepeatHorizontalPadding` | `16.dp` |
| `CounterRepeatLabelFontSize` | `15.sp` |
| `CounterRepeatValueFontSize` | `21.sp` |
| `CounterRepeatButtonTouchSize` | `48.dp` |
| `CounterRepeatButtonVisualSize` | `32.dp` |
| `CounterRepeatIconSize` | `16.dp` |
| `CounterRepeatToRowSpacing` | `32.dp` |
| `CounterPrimaryTouchSize` | `144.dp` |
| `CounterPrimaryVisualSize` | `125.dp` |
| `CounterMinusTouchSize` | `144.dp` |
| `CounterMinusVisualSize` | `123.dp` |
| `CounterMinusOpticalOffsetY` | `1.dp` |
| `CounterUndoVisualSize` | `92.dp` |
| `CounterUndoTouchSize` | `CounterUndoVisualSize` |
| `CounterUndoVerticalSpacing` | `16.dp` |
| `CounterControlsMaxWidth` | `360.dp` |
| `CounterControlsToStitchTrackerSpacing` | `72.dp` |
| `CounterControlsToStitchTrackerCompactSpacing` | `8.dp` |
| `ProjectCardGridSpacing` | `12.dp` |
| `ProjectCardGridMaxWidth` | `360.dp` |
| `ProjectCardThreeColumnMinWidth` | `480.dp` |
| `ProjectCardPadding` | `12.dp` |
| `ProjectCardIconSize` | `56.dp` |
| `ProjectCardCompactIconSize` | `32.dp` |
| `ProjectCardIconTitleSpacing` | `12.dp` |
| `ExtraCounterCardMinHeight` | `88.dp` |
| `ExtraCounterCardCornerRadius` | `16.dp` |
| `ExtraCounterCardHorizontalPadding` | `22.dp` |
| `ExtraCounterCardVerticalPadding` | `18.dp` |
| `ExtraCounterContentSpacing` | `8.dp` |
| `ExtraCounterStepperTouchSize` | `56.dp` |
| `ExtraCounterStepperVisualSize` | `42.dp` |
| `ExtraCounterStepperIconSize` | `20.dp` |
| `ExtraCounterOverflowTouchSize` | `48.dp` |
| `ExtraCounterOverflowIconSize` | `22.dp` |
| `RepeatSectionProgressHeight` | `8.dp` |
| `WorkspaceSectionActionIconSize` | `18.dp` |
| `WorkspaceSectionActionIconSpacing` | `4.dp` |
| `StitchTrackerContainerAlpha` | `0.42f` |
| `StitchTrackerMinHeight` | `68.dp` |
| `StitchTrackerCornerRadius` | `16.dp` |
| `StitchTrackerHorizontalPadding` | `16.dp` |
| `StitchTrackerVerticalPadding` | `6.dp` |
| `StitchTrackerContentSpacing` | `12.dp` |
| `CompactTopBarHeight` | `48.dp` |
| `TopBarTitleEndPadding` | `8.dp` |
| `ProjectActionsHorizontalPadding` | `22.dp` |
| `ProjectActionsRowVerticalPadding` | `12.dp` |
| `ProjectActionsSectionTopPadding` | `14.dp` |
| `ProjectActionsSectionBottomPadding` | `8.dp` |
| `ProjectActionsDividerVerticalPadding` | `4.dp` |
| `WorkSessionStackBreakpoint` | `360.dp` |
| `WorkSessionStripMinHeight` | `48.dp` |
| `WorkSessionStripInlineSpacing` | `8.dp` |

### ProjectListDimens

Source: [ProjectListDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/ProjectListDimens.kt).

| Token | Source value |
| --- | --- |
| `TwoColumnMinWidth` | `760.dp` |
| `CardSpacing` | `10.dp` |
| `CardPaddingStart` | `10.dp` |
| `CardPaddingVertical` | `10.dp` |
| `CardPaddingEnd` | `14.dp` |
| `ThumbnailSize` | `72.dp` |
| `ThumbnailSizeCompact` | `56.dp` |
| `ThumbnailCornerRadius` | `14.dp` |
| `ThumbnailTextGap` | `14.dp` |
| `CompactMaxWidthDp` | `320f` |
| `ScreenHorizontalPadding` | `16.dp` |
| `ListTopPadding` | `8.dp` |
| `ListBottomPadding` | `16.dp` |
| `ListBottomPaddingWithCreateButton` | `112.dp` |
| `HeroPadding` | `20.dp` |
| `HeroContentGap` | `12.dp` |
| `HeroActionTouchSize` | `72.dp` |
| `HeroActionVisualSize` | `64.dp` |
| `ItemLineGap` | `4.dp` |
| `ProgressGroupTopGap` | `8.dp` |
| `ProgressTrackHeight` | `4.dp` |
| `ProgressTrackAlpha` | `0.15f` |
| `FooterActionTouchSize` | `48.dp` |
| `SectionTopSpacing` | `20.dp` |
| `SectionBottomSpacing` | `8.dp` |
| `CreateButtonVisualSize` | `64.dp` |
| `CreateButtonMargin` | `16.dp` |
| `CreateButtonTopBarVisualSize` | `40.dp` |
| `CreateButtonTopBarEndPadding` | `4.dp` |

### ProjectOverviewDimens

Source: [ProjectOverviewDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/ProjectOverviewDimens.kt).

| Token | Source value |
| --- | --- |
| `ScreenHorizontalPadding` | `16.dp` |
| `HeroPhotoHeight` | `210.dp` |
| `NoPhotoHeight` | `96.dp` |
| `PLACEHOLDER_FABRIC_ALPHA` | `0.55f` |
| `PLACEHOLDER_YARN_FABRIC_ALPHA` | `0.3f` |
| `StepperVisualSize` | `44.dp` |
| `StepperTouchSize` | `48.dp` |
| `TopContentGap` | `8.dp` |
| `HeroPhotoCornerRadius` | `24.dp` |
| `HeaderTopGap` | `18.dp` |
| `PillHorizontalPadding` | `12.dp` |
| `SectionTopGap` | `20.dp` |
| `RowMinHeight` | `56.dp` |
| `ActionTouchSize` | `48.dp` |
| `YarnSwatchSize` | `48.dp` |
| `YarnGridMinWidthForFourColumns` | `340.dp` |
| `ContentGap` | `8.dp` |
| `DetailRowGap` | `12.dp` |
| `DetailLabelGap` | `16.dp` |
| `ContentBottomPadding` | `32.dp` |
| `SheetCornerRadius` | `18.dp` |

### CrossStitchDimens

Source: [CrossStitchDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/CrossStitchDimens.kt).

| Token | Source value |
| --- | --- |
| `GridCells` | `9.5f` |
| `GridMargin` | `0.5f` |
| `StitchInset` | `0.18f` |
| `StitchSpan` | `0.64f` |
| `StitchStrokeRatio` | `0.3f` |
| `SoftStitchAlpha` | `0.55f` |
| `MinStitchStroke` | `1.2.dp` |
| `RowGlyphSize` | `28.dp` |
| `EmptyStateGlyphSize` | `96.dp` |
| `EmptyStatePadding` | `32.dp` |
| `EmptyStateGlyphGap` | `24.dp` |
| `EmptyStateTextGap` | `8.dp` |
| `EmptyStateActionGap` | `16.dp` |

### FabricSwatchDimens

Source: [FabricSwatchDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/FabricSwatchDimens.kt).

| Token | Source value |
| --- | --- |
| `ProjectReferenceSize` | `72f` |
| `YarnReferenceSize` | `48f` |
| `VignetteCenterX` | `0.42f` |
| `VignetteCenterY` | `0.36f` |
| `VignetteRadius` | `0.78f` |
| `VignetteClearStop` | `0.62f` |
| `VignetteAlpha` | `0.28f` |
| `KnitStitchWidth` | `17f` |
| `KnitRowRatio` | `0.74f` |
| `KnitLegTopInset` | `0.44f` |
| `KnitLegBottomInset` | `0.05f` |
| `KnitLegTopLift` | `0.02f` |
| `KnitLegBottomDrop` | `1.1f` |
| `KnitLegExtraLength` | `0.04f` |
| `KnitLegHalfThickness` | `0.25f` |
| `KnitEdgeWidth` | `0.035f` |
| `CrochetStitchWidth` | `11f` |
| `CrochetRowRatio` | `0.72f` |
| `CrochetStrandThickness` | `0.4f` |
| `CrochetShortStrandRatio` | `0.86f` |
| `CrochetEdgeWidth` | `0.03f` |
| `CrochetStartX` | `0.1f` |
| `CrochetStartDrop` | `1.05f` |
| `CrochetLongReach` | `0.7f` |
| `CrochetLongRise` | `1.15f` |
| `CrochetShortStartX` | `0.42f` |
| `CrochetShortStartRise` | `0.45f` |
| `CrochetShortReach` | `1f` |
| `CrochetShortRise` | `1.05f` |
| `YarnPly` | `9f` |
| `YarnRowStep` | `0.92f` |
| `YarnColumnStep` | `1.9f` |
| `YarnStrandHalfLength` | `1.05f` |
| `YarnStrandHalfThickness` | `0.42f` |
| `YarnStrandAngle` | `-35f` |
| `YarnEdgeWidth` | `0.06f` |

### FontScaleThresholds

Source: [FontScaleThresholds.kt](app/src/main/java/com/finnvek/knittools/ui/theme/FontScaleThresholds.kt).

| Token | Source value |
| --- | --- |
| `COMPACT` | `1.3f` |
| `LARGE` | `1.5f` |

### IllustrationDimens

Source: [IllustrationDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/IllustrationDimens.kt).

| Token | Source value |
| --- | --- |
| `Height` | `220.dp` |
| `WelcomeHeight` | `140.dp` |
| `CompactHeight` | `170.dp` |
| `ProPageHeight` | `150.dp` |
| `ShortHeight` | `84.dp` |
| `ShortViewportHeight` | `480.dp` |
| `HiddenBelowViewportHeight` | `360.dp` |
| `LogoSize` | `128.dp` |
| `EmptyStateHeight` | `140.dp` |
| `Rise` | `16.dp` |
| `EntranceRise` | `10.dp` |
| `ENTER_MILLIS` | `450` |
| `BOUNCE_SCALE_FROM` | `0.88f` |
| `BOUNCE_DAMPING` | `0.55f` |
| `ENTRANCE_MILLIS` | `320` |
| `ENTRANCE_STAGGER_MILLIS` | `55` |
| `ENTRANCE_BASE_DELAY_MILLIS` | `120` |
| `SELECTION_MILLIS` | `180` |

### OnboardingDimens

Source: [OnboardingDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/OnboardingDimens.kt).

| Token | Source value |
| --- | --- |
| `JourneyProgressHeight` | `6.dp` |
| `JourneyProgressGap` | `5.dp` |
| `JourneyToolbarHeight` | `48.dp` |
| `JourneyDemoUndoSize` | `48.dp` |
| `JourneyDemoUndoTouchSize` | `56.dp` |
| `DemoPatternRowGap` | `20.dp` |
| `JourneyDemoCounterSize` | `80.dp` |
| `JourneySectionGap` | `24.dp` |
| `ShortContentHeight` | `480.dp` |
| `ShortSectionGap` | `12.dp` |
| `SideBySideActionsScreenHeight` | `480.dp` |
| `JourneyDemoTouchSize` | `88.dp` |
| `JourneyDemoButtonSize` | `72.dp` |
| `JourneyDemoNumberMinWidth` | `96.dp` |
| `CraftTileImageSize` | `88.dp` |
| `CraftTileMinHeight` | `104.dp` |
| `SelectionCheckSize` | `24.dp` |
| `SelectionBorderWidth` | `1.5.dp` |
| `STEP_TRANSITION_MILLIS` | `320` |
| `STEP_SLIDE_FRACTION` | `0.15f` |
| `CRAFT_ADVANCE_DELAY_MILLIS` | `250L` |
| `DEMO_NUMBER_MILLIS` | `220` |
| `DEMO_LINE_MILLIS` | `360` |
| `ToolboxGlyphPadSize` | `52.dp` |
| `ToolboxGlyphSize` | `36.dp` |
| `ToolboxLineStroke` | `2.5.dp` |
| `ToolboxMoreOutline` | `DashedOutlineStyle(width = 1.dp, dash = 5.dp, gap = 4.dp, corner = 12.dp)` |
| `TOOLBOX_COLUMNS` | `2` |
| `TOOLBOX_COMPACT_COLUMNS` | `1` |
| `TOOLBOX_DETAIL_MILLIS` | `900` |
| `TOOLBOX_PRESS_DEPTH` | `0.14f` |
| `TOOLBOX_PRESS_COUNT` | `2` |
| `TOOLBOX_LINE_FROM` | `0.3f` |
| `TOOLBOX_LINE_TO` | `0.72f` |
| `TOOLBOX_CHART_BASE` | `0.15f` |
| `JourneyStackedBreakpoint` | `320.dp` |
| `ExampleCardPadding` | `16.dp` |
| `SketchWidth` | `132.dp` |
| `SketchHeight` | `176.dp` |
| `SwatchWidth` | `150.dp` |
| `FixedStitchScale` | `1.dp` |
| `SwatchRowHeight` | `FixedStitchScale * FabricSwatchDimens.KnitStitchWidth * FabricSwatchDimens.KnitRowRatio` |
| `PlusVisualSize` | `96.dp` |
| `PlusTouchSize` | `104.dp` |
| `CaptionMinHeight` | `48.dp` |
| `CompletedProjectImageSize` | `160.dp` |
| `ReadingLineWidth` | `2.dp` |
| `PaperInset` | `10.dp` |
| `TextBarHeight` | `4.dp` |
| `TextRowHeight` | `16.dp` |
| `ReadingBandHeight` | `14.dp` |
| `ChartCellWidth` | `13.dp` |
| `ChartCellHeight` | `10.dp` |
| `ChartLineWidth` | `1.dp` |
| `START_COUNT` | `23` |
| `START_ROWS` | `4` |
| `MAX_ROWS` | `7` |
| `GROWTH_DURATION_MILLIS` | `250` |
| `READING_BAND_ALPHA` | `0.16f` |
| `TEXT_ROWS` | `8` |
| `CHART_COLUMNS` | `8` |
| `CHART_ROWS` | `2` |
| `CHART_INK_SHADE` | `0.15f` |
| `TextBarFractions` | `listOf(0.8f, 0.95f, 0.65f, 1f, 0.6f, 0.88f, 0.97f, 0.72f)` |

### LaunchPriceTagDimens

Source: [LaunchPriceTagDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/LaunchPriceTagDimens.kt).

| Token | Source value |
| --- | --- |
| `OuterInset` | `5.dp` |
| `InnerPadding` | `14.dp` |
| `StitchWidth` | `1.5.dp` |
| `Stitch` | `DashedOutlineStyle(width = StitchWidth, dash = 6.dp, gap = 4.dp, corner = 12.dp)` |

### ProMarkerDimens

Source: [ProMarkerDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/ProMarkerDimens.kt).

| Token | Source value |
| --- | --- |
| `Height` | `22.dp` |
| `HorizontalPadding` | `8.dp` |
| `CornerRadius` | `4.dp` |
| `StitchWidth` | `1.dp` |
| `StitchInset` | `2.dp` |
| `StitchDash` | `3.dp` |
| `StitchGap` | `2.dp` |

### InsightsDimens

Source: [InsightsDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/InsightsDimens.kt).

| Token | Source value |
| --- | --- |
| `CompletionPlotHeight` | `48.dp` |
| `CompletionBucketWidth` | `96.dp` |
| `CompletionTimelineEndPadding` | `16.dp` |
| `CompletionBarWidth` | `20.dp` |
| `ScreenHorizontalPadding` | `16.dp` |
| `ContentBottomPadding` | `32.dp` |
| `FiltersTopPadding` | `4.dp` |
| `ContextRowGap` | `12.dp` |
| `FilterChipShape` | `RoundedCornerShape(percent = 50)` |
| `FilterChipHorizontalPadding` | `12.dp` |
| `FilterChipVerticalPadding` | `7.dp` |
| `FilterChipSpacing` | `8.dp` |
| `FilterChipIndicatorSpacing` | `4.dp` |
| `FilterChipIndicatorSize` | `18.dp` |
| `FilterPillHeight` | `36.dp` |
| `FilterChipDotSize` | `10.dp` |
| `MenuNeutralDotSize` | `6.dp` |
| `FilterChipDotSpacing` | `8.dp` |
| `KickerFontSize` | `13.sp` |
| `HeroTopPadding` | `6.dp` |
| `HeroBottomPadding` | `6.dp` |
| `HeroLeadFontSize` | `15.sp` |
| `HeroPrimaryNumberFontSize` | `76.sp` |
| `HeroPrimaryNumberMinFontSize` | `52.sp` |
| `HeroPrimaryUnitFontSize` | `26.sp` |
| `HeroSecondaryNumberFontSize` | `46.sp` |
| `HeroSecondaryUnitFontSize` | `20.sp` |
| `HeroUnitToMinutesSpacing` | `11.dp` |
| `HeroNumberLetterSpacing` | `(-0.055f).em` |
| `StatsRowTopPadding` | `16.dp` |
| `StatsRowBottomPadding` | `10.dp` |
| `StatValueFontSize` | `20.sp` |
| `StatValueMinFontSize` | `14.sp` |
| `StatValueLetterSpacing` | `(-0.03f).em` |
| `StatLabelTopMargin` | `2.dp` |
| `StatColumnGap` | `28.dp` |
| `TrendFontSize` | `13.sp` |
| `TrendBottomPadding` | `4.dp` |
| `ChartGridlineAlpha` | `0.55f` |
| `SectionTopPadding` | `22.dp` |
| `SectionHeaderBottomPadding` | `12.dp` |
| `ChartPlotHeight` | `168.dp` |
| `ChartPlotHeightSparse` | `108.dp` |
| `ChartPlotHeightMedium` | `140.dp` |
| `ChartAxisBandHeight` | `22.dp` |
| `ChartGridlineStroke` | `1.dp` |
| `ChartMinBarHeight` | `9.dp` |
| `ChartBarMinGap` | `2.dp` |
| `ChartBarCorner` | `3.dp` |
| `ChartWeekBarWidth` | `34.dp` |
| `ChartMonthBarWidth` | `10.dp` |
| `ChartAllTimeBarWidth` | `22.dp` |
| `ChartStitchTargetWidth` | `6.dp` |
| `ChartStitchAspect` | `0.78f` |
| `ChartStitchStrokeRatio` | `0.16f` |
| `ChartStitchShadowAlpha` | `0.14f` |
| `ChartStitchMinBarHeight` | `14.dp` |
| `ChartSelectionMarkerBand` | `12.dp` |
| `ChartSelectionMarkerHeight` | `3.dp` |
| `ProjectFabricCellGap` | `1.dp` |
| `ProjectFabricSelectionStroke` | `1.dp` |
| `ProjectFabricMaxCellSize` | `32.dp` |
| `ProjectFabricEmptyCellAlpha` | `0.6f` |
| `ProjectFabricDetailTopPadding` | `8.dp` |
| `ChartAxisLabelFontSize` | `13.sp` |
| `ProjectRowMinHeight` | `48.dp` |
| `ProjectRowVerticalPadding` | `15.dp` |
| `ProjectRowGap` | `14.dp` |
| `ProjectDotSize` | `14.dp` |
| `ProjectNameFontSize` | `19.sp` |
| `ProjectSubFontSize` | `13.sp` |
| `ProjectSubTopMargin` | `2.dp` |
| `ProjectDurationFontSize` | `19.sp` |
| `MixBarHeight` | `22.dp` |
| `MixBarCorner` | `4.dp` |
| `MixBarTopPadding` | `4.dp` |
| `MixBarBottomPadding` | `16.dp` |
| `EmptyTitleFontSize` | `32.sp` |
| `EmptyBodyFontSize` | `16.sp` |
| `EmptyBodyTopMargin` | `12.dp` |
| `EmptyRangeNoteVerticalPadding` | `24.dp` |
| `EmptyRangeTitleFontSize` | `22.sp` |
| `ProCardPadding` | `24.dp` |
| `ProCardTitleFontSize` | `17.sp` |
| `ProCardBodyFontSize` | `15.sp` |
| `ProCardBodyTopMargin` | `8.dp` |
| `SkeletonCornerRadius` | `12.dp` |
| `SkeletonHeroHeight` | `152.dp` |
| `SkeletonStatsHeight` | `64.dp` |
| `SkeletonSpacing` | `16.dp` |
| `SkeletonAlpha` | `0.12f` |

### PatternViewerDimens

Source: [PatternViewerDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/PatternViewerDimens.kt).

| Token | Source value |
| --- | --- |
| `HIGHLIGHTER_PALETTE_WINDOW_FRACTION` | `0.4f` |
| `CompactTopBarHeight` | `48.dp` |
| `BottomBarHorizontalPadding` | `16.dp` |
| `BottomBarVerticalPadding` | `10.dp` |
| `BottomBarButtonSize` | `48.dp` |
| `BottomBarRowSpacing` | `4.dp` |
| `BottomBarCountSpacing` | `8.dp` |
| `LibraryBottomBarHorizontalPadding` | `8.dp` |
| `LibraryBottomBarVerticalPadding` | `6.dp` |
| `ReadingLineLabelGap` | `4.dp` |
| `ReadingLineHandleSize` | `48.dp` |
| `ReadingLineHandleRadius` | `6.dp` |
| `ReadingLineMinimumBandHeight` | `24.dp` |
| `DocumentStatusGap` | `8.dp` |
| `DocumentInfoLineSpacing` | `2.dp` |
| `DocumentWebsiteGap` | `12.dp` |
| `CalibrationPanelElevation` | `2.dp` |
| `CalibrationInstructionGap` | `4.dp` |
| `CalibrationFieldGap` | `12.dp` |
| `CalibrationActionGap` | `8.dp` |

### PatternAnnotationTokens

Source: [PatternAnnotationTokens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/PatternAnnotationTokens.kt).

| Token | Source value |
| --- | --- |
| `PEN_DEFAULT_ARGB` | `0xFF1F1F1F.toInt()` |
| `HIGHLIGHTER_DEFAULT_ARGB` | `0x60FFD54F` |
| `PEN_DEFAULT_WIDTH` | `4f` |
| `HIGHLIGHTER_DEFAULT_WIDTH` | `20f` |
| `ERASER_HIT_TOLERANCE` | `0.02f` |
| `SELECTION_HIT_TOLERANCE` | `0.025f` |
| `SELECTION_OUTLINE_WIDTH` | `3f` |
| `TEXT_DEFAULT_SIZE` | `18f` |
| `TEXT_BACKGROUND_ARGB` | `0xFFFFFFFF.toInt()` |
| `TEXT_BACKGROUND_ALPHA` | `0.85f` |
| `CALLOUT_DEFAULT_ARGB` | `0xFF6750A4.toInt()` |
| `CHART_HIGHLIGHT_ALPHA` | `0.35f` |
| `COLOR_PALETTE_ARGB` | `listOf(0xFF1F1F1F.toInt(), 0xFFD32F2F.toInt(), 0xFF1976D2.toInt(), 0xFF388E3C.toInt(), 0xFFFFC107.toInt())` |
| `REFERENCE_PAGE_SIZE` | `1_000f` |
| `HIGHLIGHTER_DEFAULT_ALPHA` | `96` |
| `CALLOUT_BACKGROUND_ALPHA` | `0.16f` |
| `CALLOUT_TEXT_SIZE` | `24f` |
| `CALLOUT_CORNER_RADIUS` | `12f` |
| `CHART_GRID_WIDTH` | `2f` |
| `ARROW_HEAD_MULTIPLIER` | `4f` |
| `ARROW_HEAD_ANGLE` | `0.55f` |
| `MINIMUM_PRESSURE_SCALE` | `0.15f` |
| `TOOL_TOUCH_TARGET` | `48.dp` |
| `TOOLBAR_MAX_HEIGHT` | `280.dp` |
| `TOOLBAR_HORIZONTAL_PADDING` | `12.dp` |
| `TOOLBAR_VERTICAL_PADDING` | `8.dp` |
| `TOOLBAR_ITEM_SPACING` | `8.dp` |
| `HIGHLIGHTER_TOOLBAR_SPACING` | `4.dp` |

### CounterWidgetDimens

Source: [CounterWidgetDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/CounterWidgetDimens.kt).

| Token | Source value |
| --- | --- |
| `ButtonSize` | `48.dp` |
| `LargeButtonSize` | `56.dp` |
| `SmallButtonTouchSize` | `48.dp` |
| `SmallButtonInset` | `3.dp` |
| `HorizontalPadding` | `14.dp` |
| `VerticalPadding` | `8.dp` |
| `MediumVerticalPadding` | `4.dp` |
| `SmallHorizontalPadding` | `10.dp` |
| `LargeFontVerticalPadding` | `2.dp` |
| `ProgressHeight` | `4.dp` |
| `ProgressTopSpacing` | `4.dp` |
| `SectionSpacing` | `4.dp` |
| `NameSize` | `13.sp` |
| `SmallNameSize` | `11.sp` |
| `CaptionSize` | `10.sp` |
| `SmallCountSize` | `22.sp` |
| `StackedCountSize` | `28.sp` |
| `CountSize` | `30.sp` |
| `LargeCountSize` | `48.sp` |
| `ProPromptSize` | `12.sp` |

### SizeChartDimens

Source: [SizeChartDimens.kt](app/src/main/java/com/finnvek/knittools/ui/theme/SizeChartDimens.kt).

| Token | Source value |
| --- | --- |
| `ScreenPadding` | `16.dp` |
| `RowSpacing` | `8.dp` |
| `CellPadding` | `4.dp` |

## 9. Exact cross-stitch grids

Rows are top to bottom. Keep each grid's own row and column count. Use the drawing formula and tint assignments in section 4.

### PATTERN

```
XXXXXX...
X....XX..
X....X.X.
X....XXXX
X...o...X
X..ooo..X
X...o...X
X.......X
XXXXXXXXX
```

### YARN

```
....XXX......
..XXXXXXX....
.X..XXXX.X...
.XXX..XXXX...
X.XXX..XXXX..
XX..XXX.XXX..
XXX..XX..XX..
.XXX..XX.X...
.XXXX..XXX...
..XXXX.XX....
....XXX.X..X.
.........XX.X
```

### NOTES

```
XXXXXXX
X.....X
X.ooo.X
X.....X
X.ooo.X
X.....X
X.oo..X
XXXXXXX
```

### PHOTOS

```
XXXXXXXXX
X.......X
X.....o.X
X.......X
X..o....X
X.ooo.o.X
XoooooooX
XXXXXXXXX
```

### REMINDER

```
....X....
..XXXXX..
.X.....X.
.X.....X.
.X.....X.
X.......X
XXXXXXXXX
.........
...XXX...
```

### COUNTER

```
..XXXXX..
.X.....X.
X...o...X
X...o...X
X.ooooo.X
X...o...X
X...o...X
.X.....X.
..XXXXX..
```

### CLOCK

```
..XXXXX..
.X.....X.
X...o...X
X...o...X
X...ooo.X
X.......X
X.......X
.X.....X.
..XXXXX..
```

### CALCULATOR

```
XXXXXXXXX
X.......X
X.ooooo.X
X.......X
X.X.X.X.X
X.......X
X.X.X.X.X
X.......X
XXXXXXXXX
```

### CHART

```
.......X.
.......X.
....X..X.
....X..X.
.X..X..X.
.X..X..X.
.X..X..X.
.........
XXXXXXXXX
```

## 10. Material vector usage by source file

Use this index to look up the icon namespace and variant used for individual surfaces and states. The vectors here are the ones referenced in the current app UI sources. Section 4 covers custom graphics and packaged resources.

| App surface source | Material icon references |
| --- | --- |
| [HubListItem.kt](app/src/main/java/com/finnvek/knittools/ui/components/HubListItem.kt) | `Icons.AutoMirrored.Filled.KeyboardArrowRight` |
| [InfoNote.kt](app/src/main/java/com/finnvek/knittools/ui/components/InfoNote.kt) | `Icons.Outlined.Info` |
| [InfoTip.kt](app/src/main/java/com/finnvek/knittools/ui/components/InfoTip.kt) | `Icons.Outlined.Info` |
| [OverviewSection.kt](app/src/main/java/com/finnvek/knittools/ui/components/OverviewSection.kt) | `Icons.AutoMirrored.Filled.KeyboardArrowRight`, `Icons.Default.Check` |
| [PasteInstructionButton.kt](app/src/main/java/com/finnvek/knittools/ui/components/PasteInstructionButton.kt) | `Icons.Outlined.FilterVintage` |
| [ProjectFilterPill.kt](app/src/main/java/com/finnvek/knittools/ui/components/ProjectFilterPill.kt) | `Icons.Filled.Check` |
| [ScreenTitleSelector.kt](app/src/main/java/com/finnvek/knittools/ui/components/ScreenTitleSelector.kt) | `Icons.Filled.ArrowDropDown` |
| [StatusMessage.kt](app/src/main/java/com/finnvek/knittools/ui/components/StatusMessage.kt) | `Icons.Outlined.CheckCircle`, `Icons.Outlined.ErrorOutline`, `Icons.Outlined.Info` |
| [ToolScreenScaffold.kt](app/src/main/java/com/finnvek/knittools/ui/components/ToolScreenScaffold.kt) | `Icons.AutoMirrored.Filled.ArrowBack`, `Icons.Default.Settings` |
| [TopBarIconButtons.kt](app/src/main/java/com/finnvek/knittools/ui/components/TopBarIconButtons.kt) | `Icons.AutoMirrored.Filled.ArrowBack`, `Icons.Default.MoreVert` |
| [Screen.kt](app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt) | `Icons.Filled.AutoStories`, `Icons.Outlined.BarChart`, `Icons.Outlined.Build`, `Icons.Outlined.FolderOpen`, `Icons.Outlined.Settings` |
| [CounterScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt) | `Icons.AutoMirrored.Filled.ArrowBack`, `Icons.Default.MoreVert`, `Icons.Outlined.OpenInFull` |
| [CounterWorkspaceSections.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt) | `Icons.Filled.Add` |
| [MultiCounterComponents.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt) | `Icons.Filled.CheckCircle`, `Icons.Filled.MoreVert` |
| [PhotoComponents.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoComponents.kt) | `Icons.Filled.Close`, `Icons.Filled.Delete`, `Icons.Filled.Share` |
| [PhotoGalleryScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoGalleryScreen.kt) | `Icons.AutoMirrored.Filled.ArrowBack` |
| [ProjectActionsBottomSheet.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/ProjectActionsBottomSheet.kt) | `Icons.AutoMirrored.Filled.KeyboardArrowRight` |
| [ReminderComponents.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/ReminderComponents.kt) | `Icons.Filled.Notifications` |
| [YarnManagementSheet.kt](app/src/main/java/com/finnvek/knittools/ui/screens/counter/YarnManagementSheet.kt) | `Icons.Filled.Close` |
| [LibraryTopBar.kt](app/src/main/java/com/finnvek/knittools/ui/screens/library/LibraryTopBar.kt) | `Icons.AutoMirrored.Filled.ArrowBack`, `Icons.Filled.Close` |
| [MyYarnScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/library/MyYarnScreen.kt) | `Icons.AutoMirrored.Filled.KeyboardArrowRight` |
| [SavedPatternsScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternsScreen.kt) | `Icons.Filled.CheckCircle`, `Icons.Outlined.Circle` |
| [NotesEditorScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/notes/NotesEditorScreen.kt) | `Icons.AutoMirrored.Filled.ArrowBack` |
| [OnboardingScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingScreen.kt) | `Icons.AutoMirrored.Filled.ArrowBack` |
| [OnboardingSteps.kt](app/src/main/java/com/finnvek/knittools/ui/screens/onboarding/OnboardingSteps.kt) | `Icons.Default.Check` |
| [PatternBookmarkSheet.kt](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternBookmarkSheet.kt) | `Icons.Default.Check`, `Icons.Default.MoreVert` |
| [PatternImageImportSurface.kt](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternImageImportSurface.kt) | `Icons.AutoMirrored.Filled.ArrowBack`, `Icons.AutoMirrored.Filled.ArrowForward`, `Icons.Default.Delete` |
| [PatternViewerBottomBar.kt](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerBottomBar.kt) | `Icons.AutoMirrored.Filled.ArrowBack`, `Icons.AutoMirrored.Filled.ArrowForward`, `Icons.Default.Add`, `Icons.Default.Remove`, `Icons.Outlined.BookmarkBorder` |
| [PatternViewerScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt) | `Icons.AutoMirrored.Filled.ArrowBack`, `Icons.AutoMirrored.Filled.ArrowForward`, `Icons.AutoMirrored.Filled.OpenInNew`, `Icons.AutoMirrored.Filled.Redo`, `Icons.AutoMirrored.Filled.Undo`, `Icons.Default.MoreVert` |
| [ProjectFolderComponents.kt](app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectFolderComponents.kt) | `Icons.Filled.Check`, `Icons.Filled.MoreVert` |
| [ProjectListScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt) | `Icons.Filled.Check`, `Icons.Filled.Close`, `Icons.Filled.MoreVert` |
| [ProjectOverviewScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectOverviewScreen.kt) | `Icons.AutoMirrored.Filled.ArrowBack`, `Icons.Default.MoreVert` |
| [ProjectOverviewSections.kt](app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectOverviewSections.kt) | `Icons.AutoMirrored.Filled.KeyboardArrowRight` |
| [ProjectsStartState.kt](app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectsStartState.kt) | `Icons.Default.Close` |
| [SettingsScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/settings/SettingsScreen.kt) | `Icons.AutoMirrored.Filled.KeyboardArrowRight`, `Icons.AutoMirrored.Filled.OpenInNew`, `Icons.Filled.Check` |
| [YarnEstimatorScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/yarn/YarnEstimatorScreen.kt) | `Icons.AutoMirrored.Filled.KeyboardArrowRight` |
| [YarnCardDetailScreen.kt](app/src/main/java/com/finnvek/knittools/ui/screens/yarncard/YarnCardDetailScreen.kt) | `Icons.Filled.Check` |
