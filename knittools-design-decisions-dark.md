# KnitTools — Design Decisions (Dark Theme)

> **Purpose:** Authoritative visual design guide for Claude Code implementation. Use alongside the main spec (`knitting-toolkit-spec.md`). Where this document contradicts the main spec on visual matters, this document wins.
>
> **Scope:** Dark theme only. Light theme will be specified separately later.
>
> **Style direction:** Material Design 3 (Android) with editorial craft elevation — warm, tactile, premium. The app should feel like the same world as the home screen photography, not like a generic calculator.

---

## 1. Color System — Warm Neutral Shift

The initial prototypes use pure neutral grays. Shift all surface tokens toward a barely perceptible warm undertone. The goal is "lived-in warmth" — if you can consciously identify the warmth, it's too much.

### Dark Theme Surface Tokens

| Role | Token name | Value | Usage |
|------|-----------|-------|-------|
| Base background | `surface` | `#0f0e0d` | Screen background, root |
| Lowest container | `surface-container-lowest` | `#000000` | Inset areas behind notation text |
| Low container | `surface-container-low` | `#141312` | Toggle pill backgrounds, note cards |
| Container | `surface-container` | `#1a1918` | General container |
| High container | `surface-container-high` | `#211f1d` | Input field backgrounds |
| Highest container | `surface-container-highest` | `#282623` | Result cards, list items |

### Accent & Text Tokens

| Role | Token name | Value |
|------|-----------|-------|
| Primary accent | `primary` | `#b0cfad` |
| Primary container | `primary-container` | `#3f593e` |
| On-primary | `on-primary` | `#2d462d` |
| On-surface (primary text) | `on-surface` | `#e7e5e4` |
| On-surface-variant (secondary text) | `on-surface-variant` | `#acabaa` |
| Error / warning | `error` | `#f97758` |
| Outline variant | `outline-variant` | `#484848` |

### Color Usage Rules

- Primary sage green is used sparingly: selected toggle state, result numbers, active states. If more than ~15% of visible pixels are sage green, there's too much.
- No pure white (`#FFFFFF`) for text. `#e7e5e4` is the brightest text color.
- No visible green tint on backgrounds or cards. The warmth comes from amber/brown, not green.

---

## 2. Surface Texture

Apply a very fine monochrome noise texture to the root background surface only.

### Specification

- **Asset:** 256×256px tileable grayscale noise PNG
- **Opacity:** 2–3% (if you can see it at arm's length, reduce it)
- **Blend mode:** Overlay or soft-light
- **Applied to:** Root background only (the `surface` layer behind everything)
- **NOT applied to:** Cards, input fields, app bar, or any elevated surfaces

### What This Achieves

Prevents the "dead digital black" flat screen look. Creates a subtle analog quality that bridges between the photographic home screen cards and the functional UI.

### Implementation Note (Jetpack Compose)

Draw the noise as a background modifier on the root surface composable using a tiled `ImageBitmap`. Alternatively, generate the noise procedurally in a Canvas with a fixed seed for consistency.

---

## 3. Tool Screen Ambient Image (Blurred Header Glow)

Each tool screen has a unique photograph that creates a soft, warm color presence behind the top app bar area. This connects each tool screen back to the craft photography world of the home screen.

### Layout Structure (top of each tool screen)

```
┌──────────────────────────────────────┐
│  [Blurred image, positioned here]    │  ← absolute positioned, extends
│  ┌─ Top App Bar ─────────────────┐   │     from top of screen down
│  │ ← Title                    ⚙  │   │     ~160dp. Transparent app bar
│  └───────────────────────────────┘   │     floats on top of it.
│                                      │
│  ~~~ gradient fade to surface ~~~    │  ← Linear gradient from
│                                      │     transparent to surface color
│  [Normal screen content starts]      │
└──────────────────────────────────────┘
```

### Specification

- **Image:** Unique per tool (see section 3.1 for image descriptions)
- **Position:** Fixed at top of screen, behind app bar, width = screen width
- **Height:** Image source should be taller than needed; only top ~160dp visible before fade
- **Blur:** Gaussian blur, radius 40–50px (heavy — no recognizable shapes, just color and warmth)
- **Opacity:** 15–20% (should feel like ambient lighting, not a visible image)
- **Fade:** Linear gradient overlay from transparent at top to `surface` color at bottom of the image area
- **Interaction:** None — purely decorative, does not scroll, does not respond to touch
- **App bar:** Transparent with backdrop blur. The blurred image provides the subtle color behind it.

### 3.1 Tool Screen Ambient Images

These are DIFFERENT from the home screen card images. They should be closer, more intimate, more abstract — extreme close-ups of textures and materials. When blurred heavily, they become warm color fields rather than recognizable objects.

| Tool | Ambient image description |
|------|--------------------------|
| **Row Counter** | Extreme macro close-up of sage green knit stitches in stockinette, filling the entire frame. Warm side-lighting creating highlight and shadow in the stitch texture. |
| **Increase / Decrease** | Close-up of bamboo knitting needles crossing, with cream yarn wrapped around them. Warm wood tones and soft fiber texture. |
| **Gauge Converter** | Overhead close-up of a brass ruler edge lying on sage green knitted fabric. Warm metallic tones meeting soft fiber. |
| **Cast On Calculator** | Close-up of a single knitting needle with fresh cast-on loops in cream yarn, shallow depth of field. Warm, soft, beginning-of-project feeling. |
| **Yarn Estimator** | Close-up of several yarn balls in different muted colors (sage, dusty rose, cream) nestled together. Soft, rounded shapes and fiber texture. |
| **Needle Sizes** | Close-up of assorted knitting needle tips — bamboo, metal, wood — varying sizes fanning out. Mix of warm materials. |

### 3.2 Image Generation Notes

These images will be generated with Nano Banana (or similar). Because they are heavily blurred in use, exact detail quality matters less than color palette and overall warmth. Prompts should emphasize:
- Warm, natural lighting
- The specific color palette (sage green, cream, warm wood, brass)
- Macro/close-up framing
- No text, no logos, no watermarks
- Square 1:1 aspect ratio (will be cropped/scaled in use)

---

## 4. Animations

All animations are subtle and functional. The goal is "polished and responsive," never "showy."

### 4.1 Global Rules

- **Maximum duration:** 300ms for any single animation. If the user has to wait for it, it's too long.
- **Easing:** Use `FastOutSlowIn` (Material standard) for most transitions. Never linear, never bouncy.
- **Respect system settings:** If the device has "reduce motion" / animator duration scale = 0, skip all animations.
- **No animation on:** input field focus changes, label movements, scroll, keyboard appearance, toggle state changes (instant snap is fine for toggles).

### 4.2 Result Number Updates

When calculation results change (user modifies an input field):

- **Style:** Crossfade with a subtle vertical slide
- **Outgoing number:** Fades out (150ms) while sliding down 6dp
- **Incoming number:** Fades in (200ms) while sliding up from 6dp below final position
- **Applies to:** All result numbers (stitch counts, row counts, skein counts, notation text)
- **Timing:** Starts immediately when input changes (no debounce delay on the animation itself — calculation can have a small debounce but animation begins on the new value)

### 4.3 Row Counter Increment

When the user taps + or − on the Row Counter:

- **Style:** Vertical digit roll (like a mechanical counter)
- **Direction:** + rolls upward (old number exits top, new enters from bottom), − rolls downward (opposite)
- **Duration:** 200ms
- **Only the changing digits roll** — if count goes from 147 to 148, only the "7" rolls to "8". The "14" stays still. This is the key detail that makes it feel mechanical and delightful.
- **Applies to:** Main counter number only. Secondary counter can use the simpler crossfade.

### 4.4 Screen Transitions (Home → Tool)

When navigating from a home screen card to a tool screen:

- **Style:** Standard Material 3 fade-through transition
- **Duration:** 250ms total (150ms fade out, 100ms fade in)
- **No shared element transitions** — they are complex to implement and the visual payoff is small in this app since the tool screens look very different from the cards.
- **Back navigation:** Same fade-through in reverse, or use the system predictive back gesture animation.

### 4.5 Nothing Else

No animation on: cards appearing on home screen, input fields gaining focus, toggles switching, scrolling, result cards initially appearing when screen loads. Static and immediate is fine for these.

---

## 5. Typography

### Font

Use **Manrope** as the primary font for the entire app. It has a geometric, modern quality that fits the "premium craft tool" aesthetic while remaining highly readable.

Fallback: system default (Roboto on most Android devices).

### Scale Application

| Element | Size | Weight | Color | Tracking |
|---------|------|--------|-------|----------|
| App bar title | 24sp | Semibold (600) | `on-surface` | Tight (-0.02em) |
| Section header ("Calculation Results") | 20sp | Semibold (600) | `on-surface` | Normal |
| Input field value | 20–24sp | Bold (700) | `on-surface` | Normal |
| Result primary number ("116 stitches") | 48–56sp | Extrabold (800) | `primary` | Tight (-0.03em) |
| Result secondary number ("114 stitches") | 20sp | Bold (700) | `on-surface` | Normal |
| Knitting notation ("K14, M1...") | 22sp | Bold (700) | `primary` | Slightly wide (0.03em) |
| Label above input | 13sp | Medium (500) | `on-surface-variant` | Normal |
| Badge text ("TOTAL: 128 STS") | 11sp | Bold (700) | `primary` | Wide (0.1em) |
| Subtitle / category label ("EASY PATTERN") | 12sp | Bold (700) | `on-surface-variant` | Wide (0.08em), uppercase |
| Explanatory text | 14sp | Regular (400) | `on-surface-variant` | Normal |
| Info/note text (italic) | 14sp | Regular (400) italic | `on-surface-variant` | Normal |

---

## 6. Component Specifications

### 6.1 Top App Bar

- **Background:** Transparent with backdrop blur (20px) over the ambient image area
- **Left:** Sage green back arrow icon → navigates to home
- **Center-left:** Screen title in semibold
- **Right:** Settings gear icon in `on-surface-variant` gray. No other icons (no camera icon on Yarn Estimator — the OCR scan button goes in the screen body, not the app bar)
- **Height:** Standard Material 3 top app bar (64dp)
- **Behavior:** Fixed, does not scroll with content

### 6.2 Input Fields

- **Style:** Filled, no border, no outline, no underline
- **Background:** `surface-container-high` (`#211f1d`)
- **Corner radius:** 12dp
- **Padding:** 16dp horizontal, 16dp vertical
- **Value text:** 20–24sp bold, `on-surface`
- **Unit label:** Right-aligned inside field, `on-surface-variant`, regular weight
- **Label above field:** 13sp medium, `on-surface-variant`, 8dp below label to field top
- **Optional indicator:** Small "optional" text right-aligned on the same line as the label, 12sp, 60% opacity
- **No icons inside input fields**
- **Focus state:** 2px ring in `primary-container` color, no other visual change

### 6.3 Segmented Toggle (Increase/Decrease, Metric/Imperial, cm/in)

Consistent style across ALL tool screens:

- **Container:** Pill shape (full radius), background `surface-container-low`, padding 6dp
- **Selected option:** Pill shape inside container, background `primary` (`#b0cfad`), text `on-primary` (`#2d462d`), bold weight
- **Unselected option:** No background, text `on-surface-variant`, medium weight
- **Width:** ~70% of screen width, horizontally centered
- **Height:** ~44dp total (container), ~36dp (pills inside)
- **Animation on toggle:** None (instant switch)

### 6.4 Result Cards

- **Background:** `surface-container-highest` (`#282623`)
- **Corner radius:** 16dp for outer card, 12dp for inner inset areas
- **NO borders** — no left border, no outline, no stroke of any kind
- **Padding:** 24dp
- **Elevation:** None (no drop shadows). Cards are distinguished from background purely by tonal shift.
- **Gradient (optional, per-card):** Very subtle radial gradient centered behind the main result number. From `primary-container` at 8–10% opacity in the center, fading to transparent. This gives the result area a barely-there warm glow without being a visible "glow effect."

### 6.5 Result Number Inset (for knitting notation)

Used inside Increase/Decrease result cards to highlight the notation:

- **Background:** `surface-container-lowest` (black) at 20% opacity
- **Corner radius:** 12dp
- **Padding:** 20dp
- **Text:** Knitting notation in `primary` color, bold, slightly wide letter spacing
- **Purpose:** Creates a recessed "display panel" feel for the notation

### 6.6 Badge / Pill Labels

Used for "TOTAL: 128 STS" and similar:

- **Background:** `primary` at 10–15% opacity
- **Corner radius:** Full (pill)
- **Padding:** 8dp horizontal, 4dp vertical
- **Text:** `primary` color, 11sp, bold, uppercase, wide tracking
- **No border**

### 6.7 Info Notes

Used for "Always buy one extra skein — dye lots may vary" and similar:

- **Background:** `surface-container-low`
- **Corner radius:** 16dp
- **Padding:** 20dp
- **Left element:** Small info icon in `primary` color (dimmed)
- **Text:** Italic, 14sp, `on-surface-variant`

### 6.8 Section Headers with Icons

Used in Gauge Converter for "Pattern gauge" and "Your gauge" sections:

- **Layout:** Icon + text on same line
- **Icon:** Material icon in `primary` color, 20dp
- **Text:** 20sp bold, `on-surface`
- **Spacing:** 24dp below header before first input of that section

### 6.9 Comparison Rows (Cast On "nearest with pattern repeat")

- **Background:** `surface-container-highest` at 40% opacity
- **Corner radius:** 16dp
- **Padding:** 20dp
- **Layout:** Left side = stitch count (bold, large) + width in cm (small, gray). Right side = circular icon container with up/down arrow
- **Up arrow container:** `primary` at 10% opacity background, `primary` icon
- **Down arrow container:** `error` at 10% opacity background, `error` icon
- **NO colored borders** on left or any side

---

## 7. Screen-Specific Notes

### 7.1 Home Screen

No changes from current implementation. Uses the 2×3 photo card grid with gradient overlays, sage green icon badges, and white text. The home screen is already the quality benchmark — the tool screens need to rise to its level.

### 7.2 Row Counter


- Counter number: Extra large (72sp+), centered, `on-surface` color
- The + button: Large circular button, sage green gradient background (`primary` to `primary-container` at 135°), centered below the count
- The − button: Smaller, outlined style, next to +
- Secondary counter: Smaller section below, same interaction pattern but with smaller text and buttons
- Session timer: Small, bottom of screen, `on-surface-variant`
- Keep screen awake indicator: Subtle icon or text when active

### 7.3 Increase / Decrease Calculator

Layout decisions:
- Remove the decorative image at the bottom
- Remove colored left borders on result cards
- Add "Flat / Circular" toggle somewhere (a small secondary toggle below the main Increase/Decrease toggle, or as a chip/selector near the inputs)
- Keep: "EASY PATTERN" and "BALANCED PATTERN" card structure with notation insets, explanatory text, icon + uppercase labels, "TOTAL: 128 STS" badge

### 7.4 Gauge Converter

Layout decisions:
- Remove the decorative "Atelier Precision" section at the bottom
- Make cm/in toggle consistent with other screen toggles (solid `primary` background for selected, not translucent green)
- Unify input field sizing — all six inputs should be the same visual size/weight
- Keep: Section grouping with icons ("Pattern gauge" / "Your gauge"), conversion results card with stitches and rows side by side, percentage badges

### 7.5 Cast On Calculator

Layout decisions:
- Remove the decorative image and "Ready to start your next masterpiece?" text at the bottom
- Remove icons inside input fields (straighten, grid_on) — use unit text only, consistent with other screens
- Remove colored left borders on comparison rows
- Keep: Large centered "116 stitches" result, "Nearest with pattern repeat" comparison section with up/down arrows, "Includes 2 edge stitches" note

### 7.6 Yarn Estimator

Layout decisions:
- Remove the hero image + "PLANNING MODE" badge at the top
- Remove the camera icon from the app bar — OCR scan is a Pro feature accessed from a button within the screen body
- Move the Metric/Imperial toggle to a standard position consistent with other screens
- Unify label style with other screens (check uppercase vs sentence case)
- Keep: "8 skeins" large result with bento-style card, "400g" total weight card, italic info note with icon, "Saved yarns (3)" pill button

### 7.7 Needle Sizes

Keep to the spec: searchable/scrollable table with four columns (Metric, US, UK, Japanese). Use the same surface/card styling. Tappable rows highlight in `primary` at low opacity.

---

## 8. Elements Explicitly NOT Used

Elements explicitly excluded from all screens:

- **Decorative photography sections** at the bottom of tool screens
- **Colored left/right borders** on any cards
- **Floating Action Buttons** (FAB)
- **Navigation drawers** (side menus) — the app uses hub-and-spoke, home screen only
- **"Atelier Menu"** or any drawer-style navigation — not in the spec
- **Hero images** at the top of tool screens (replaced by the subtle ambient blur)
- **Motivational text** ("Ready to start your next masterpiece?", "Planning Mode")
- **Large decorative icons** behind result numbers (the ghost inventory_2 icon in Yarn Estimator)
- **Icons inside input fields** (ruler, grid) — use text unit labels only

---

## 9. What Still Needs to Be Done

Before Claude Code implements:

1. **Generate ambient images** for each tool screen (6 images) using Nano Banana with prompts based on section 3.1
2. **Generate the noise texture** PNG asset (256×256 tileable grayscale noise)
3. **Design the light theme** color system and ambient image behavior (separate document)
4. **Decide on Manrope licensing** — verify it's available for bundling in an Android app (it's on Google Fonts, so should be fine)
5. **OCR scan entry point** for Yarn Estimator — where exactly does the scan button go in the screen body? Needs a decision.
