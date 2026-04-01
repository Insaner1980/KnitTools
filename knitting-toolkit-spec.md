# Knitting Toolkit App — Full Specification

**Developer:** Finnvek (solo indie developer)
**Platform:** Android (Kotlin, Jetpack Compose, Material Design 3)
**Target SDK:** Android 14+ (API 34), Min SDK: Android 10 (API 29)
**Architecture:** MVVM with Room for local storage, DataStore for preferences
**App name:** KnitTools
**Version:** 1.0.0

---

## 1. App Concept

A focused knitting toolkit app that combines six essential calculation tools into one clean, beautiful package. Not a project manager, not a pattern reader, not a social platform — just the practical math tools every knitter needs at hand.

**Core philosophy:**
- Do six things well instead of everything poorly
- Beautiful, calm UI that feels like opening a craft box
- No ads in free version, no subscription ever
- Privacy-first: zero analytics, zero data collection, zero network calls (except Google Play Billing and Gemini Nano where supported)
- Offline-first: everything works without internet

**Target audience:** Knitters and crocheters who need quick calculations while crafting. Both beginners who don't want to do math and experienced crafters who want speed and convenience.

---

## 2. Navigation & Screen Structure

No bottom navigation bar. The app uses a simple hub-and-spoke model:

**Home screen** → tap a card → **Tool screen** → back arrow → **Home screen**

Settings is accessed via a gear icon in the top app bar on the home screen.

### 2.1 Home Screen

A 2×3 grid of cards. Each card contains:
- A realistic photograph as the card background image (AI-generated, bundled with the app)
- A semi-transparent overlay for text readability
- Tool name in white text (both themes — text is always on the photo)
- A small sage green icon badge in one corner

The six cards (in order):
1. **Row Counter** — image: mechanical row counter on sage green knitted fabric
2. **Increase / Decrease** — image: knitted swatch with stitch markers
3. **Gauge Converter** — image: gauge swatch pinned flat with brass ruler
4. **Cast On Calculator** — image: two bamboo needles crossed beside fresh yarn ball
5. **Yarn Estimator** — image: kitchen scale with yarn ball and measuring tape
6. **Needle Sizes** — image: collection of various needles (DPN, straight, circular) scattered with needle gauge tool

App name is displayed at the top of the home screen. The grid is scrollable on smaller screens.

---

## 3. Tool Specifications

### 3.1 Row Counter

**Purpose:** Count rows and stitches while knitting.

**UI elements:**
- Project name field at top (editable, tappable)
- Large number display in center (current count)
- Large circular "+" button in sage green (primary action — most of the screen tap area)
- Smaller "−" button beside it
- Reset button (with confirmation dialog)
- Secondary counter below main counter (for pattern repeats, color changes)
- Timer showing session duration

**Behavior:**
- Tapping "+" increments by 1 (default), configurable step size
- Optional haptic feedback on tap (configurable in settings)
- Screen stays awake while counter is active (configurable)
- Counter state persists when app is closed or backgrounded
- Undo last action (single undo, not full history)

**Free:** One active project at a time. Counter history limited to last 24 hours.
**Pro:** Unlimited simultaneous projects. Full counter history. Notes per project. Secondary counters.

### 3.2 Increase / Decrease Calculator

**Purpose:** Calculate how to evenly space increases or decreases across a row.

**Input fields:**
- Current number of stitches (e.g., 120)
- Number of stitches to increase or decrease (e.g., 8)
- Toggle: Increase or Decrease mode

**Output:**
- Written stitch-by-stitch instruction in standard knitting notation
- Two methods displayed: "Easy to remember" (consistent repeat) and "Balanced" (mathematically optimal spacing)
- Example output: "K14, M1, (K15, M1) × 7 — total: 128 stitches"

**Behavior:**
- Results update in real-time as user types
- Validation: warn if decrease would result in zero or negative stitches
- Supports both flat (row) and circular (round) knitting

### 3.3 Gauge Converter

**Purpose:** Adjust stitch/row counts when your gauge doesn't match the pattern.

**Input fields:**
- Pattern gauge: stitches per 10 cm / 4 inches
- Pattern gauge: rows per 10 cm / 4 inches
- Your gauge: stitches per 10 cm / 4 inches
- Your gauge: rows per 10 cm / 4 inches
- Number of stitches in pattern instruction
- Number of rows in pattern instruction
- Unit toggle: cm / inches

**Output:**
- Adjusted stitch count for your gauge
- Adjusted row count for your gauge
- Percentage difference shown (e.g., "Your gauge is 5% tighter")

**Behavior:**
- Results update in real-time
- Rounding to nearest whole number with option to see decimal

### 3.4 Cast On Calculator

**Purpose:** Calculate how many stitches to cast on for a desired width.

**Input fields:**
- Desired width (in cm or inches)
- Your stitch gauge (stitches per 10 cm / 4 inches)
- Pattern repeat (optional — ensures cast-on is a multiple of pattern repeat + edge stitches)
- Edge stitches (optional — extra stitches for selvedge)

**Output:**
- Number of stitches to cast on
- If pattern repeat is set: nearest valid cast-on number (adjusted up and down)
- Resulting actual width

### 3.5 Yarn Estimator

**Purpose:** Calculate how many skeins/balls of yarn are needed for a project.

**Input fields:**
- Total yarn needed for project (meters or yards)
- Meters/yards per skein (from yarn label)
- Weight per skein in grams (from yarn label)
- Unit toggle: metric / imperial

**Output:**
- Number of skeins needed (rounded up to whole skeins)
- Total weight of yarn needed
- A small note: "Always buy one extra skein — dye lots may vary"

**Pro feature — Yarn Label OCR:**
- Camera button that opens ML Kit text recognition
- User photographs the yarn label
- App extracts: weight (grams), meterage/yardage, and suggested needle size
- Extracted values auto-fill the input fields
- Manual correction always available if OCR is inaccurate
- Graceful fallback: if camera permission denied or OCR fails, user enters manually

### 3.6 Needle Size Converter

**Purpose:** Convert knitting needle sizes between US, UK, metric, and Japanese systems.

**UI:**
- A searchable/scrollable reference table with four columns: Metric (mm), US, UK/Canadian, Japanese
- Tap any row to highlight it
- Search/filter: type a size in any system and the matching row highlights
- Covers sizes from 2.0mm to 25mm

**Data is static** — hardcoded in the app, no API needed.

---

## 4. AI Features

### 4.1 Gemini Nano — Instruction Parser (Pro)

**Purpose:** Parse natural language knitting instructions into calculator inputs.

**How it works:**
- Available on supported devices (Pixel 9+, recent Samsung flagships, devices with AICore)
- A text input field (or paste) where the user enters a knitting instruction
- Example input: "Increase 12 stitches evenly across the row of 96 stitches"
- Gemini Nano extracts: current stitches = 96, increase = 12
- Values auto-fill the Increase/Decrease calculator
- Also works for gauge instructions: "Gauge: 22 sts and 30 rows = 4 inches in stockinette"

**Integration:**
- Uses ML Kit GenAI APIs (Prompt API) via AICore
- On-device only, no network calls
- Check device capability at runtime: if AICore/Gemini Nano not available, hide the feature entirely (no error, no placeholder, just not shown)
- The feature is presented as a small "paste instruction" button on relevant tool screens, not as a separate screen

**Limitations to handle:**
- AICore inference quota per app — implement exponential backoff on BUSY errors
- Battery quota — respect PER_APP_BATTERY_USE_QUOTA_EXCEEDED
- Foreground only — no background inference

### 4.2 ML Kit OCR — Yarn Label Scanner (Pro)

**Purpose:** Extract yarn information from a photographed label.

**Integration:**
- ML Kit Text Recognition v2 (on-device, bundled model)
- Camera opens in a simple viewfinder, user taps to capture
- Post-processing: regex patterns to identify common label formats (e.g., "100g", "200m", "needle: 4mm")
- Results shown in a confirmation dialog before auto-filling

**Size impact:** ML Kit Text Recognition adds approximately 3-5 MB to APK.

---

## 5. Visual Design

### 5.1 Theme System

Three modes: Dark, Light, System (follows device setting). Default: System.

**Dark theme:**
- Background: near-black (#0D0D0D)
- Card/surface: dark gray (#1A1A1A) with subtle elevation shadow
- Primary text: white (#FFFFFF)
- Secondary text: light gray (#B0B0B0)
- Accent: sage green (#8BA888)
- Accent variant (pressed/active states): lighter sage (#A3BFA0)

**Light theme:**
- Background: warm off-white (#F5F4F0)
- Card/surface: pure white (#FFFFFF) with soft drop shadow
- Primary text: near-black (#1A1A1A)
- Secondary text: dark gray (#5A5A5A)
- Accent: sage green (#6B8F6B) — slightly deeper than dark theme for contrast
- Accent variant: darker sage (#557455)

**Shared:**
- Error/warning: warm coral (#D4725C)
- Success: sage green (same as accent)
- Font: System default (Roboto on most Android devices)

### 5.2 Home Screen Cards

Each card uses its AI-generated photograph as background. Implementation:
- Image fills the card with rounded corners (12dp radius)
- A gradient overlay from bottom (60% black) to top (transparent) ensures text readability
- Tool name in white, semi-bold, positioned at bottom-left of the card
- This design is identical in both dark and light themes since text sits on the photograph

### 5.3 Tool Screens

Clean, functional layouts. No background images — just the theme colors.
- Top app bar with back arrow and tool name
- Input fields use Material Design 3 outlined text fields
- Sage green for primary buttons and active states
- Results area visually separated (card or divider)
- Generous spacing and padding for touch targets (minimum 48dp)

### 5.4 App Icon

TBD — should incorporate sage green and a knitting motif (crossed needles, yarn ball, or similar). Designed to stand out on both light and dark launcher backgrounds.

---

## 6. Monetization

### 6.1 Free Tier

- All six tools fully functional
- No ads anywhere, ever
- One active counter project at a time
- Counter history: last 24 hours only
- No OCR, no Gemini Nano, no widget, no notes

### 6.2 Pro Tier — One-Time IAP

**Price:** €1.99 at launch (single IAP: `knittools_pro`)
**Pricing roadmap:**
1. **€1.99** — launch, first 3 weeks
2. **€2.99** — after initial reviews accumulate
3. **€3.49–€3.99** — A/B test via Play Console price experiments when 100+ reviews

**Pro features:**
- Unlimited simultaneous counter projects
- Full counter history (no 24h limit)
- Notes attached to each counter project
- Secondary counters per project
- Yarn label OCR scanning (ML Kit)
- Gemini Nano instruction parsing (on supported devices)
- Home screen widget for active counter
- Future pro features included at no extra cost

### 6.3 Reverse Trial

- 7-day silent trial of Pro features starting from first app launch
- No onboarding overlay or popup — user simply has access
- A subtle indicator on the home screen: "Pro trial — 5 days left"
- After trial expires, Pro features lock with a gentle prompt: "Unlock all tools for €1.99"
- Trial timestamp stored in SharedPreferences (first launch, not install date)

### 6.4 Implementation

- Google Play Billing Library (latest version)
- Never hardcode prices — always fetch from BillingClient
- All purchase state stored locally via DataStore
- Restore purchases button in Settings

---

## 7. Widget

**Pro feature.** A home screen widget for the active row counter.

**Widget sizes:**
- Small (2×1): Shows project name and current count. Tap anywhere opens the app to that counter.
- Medium (3×2): Shows project name, current count, and +/− buttons that work directly from widget.

**Implementation:** Glance (Jetpack Compose-based widget framework).

---

## 8. Data & Privacy

### 8.1 Data Storage

- All data stored locally on device using Room database
- User preferences in DataStore
- No cloud sync, no accounts, no sign-in
- No Firebase, no analytics SDKs, no crash reporting in release builds

### 8.2 Play Store Data Safety

- Data collected: None
- Data shared: None
- Data encrypted: N/A (no data transmitted)
- Users can request deletion: N/A (all data is local, user can clear app data or uninstall)

### 8.3 Permissions

- `CAMERA` — only when user initiates yarn label OCR scan (runtime permission request)
- `BILLING` — Google Play Billing for IAP
- No internet permission required for core functionality
- No storage permission (uses app-internal storage only)

---

## 9. Technical Architecture

### 9.1 Project Structure

```
com.finnvek.knittools/
├── ui/
│   ├── home/              # Home screen with card grid
│   ├── counter/           # Row Counter screen
│   ├── increase/          # Increase/Decrease calculator
│   ├── gauge/             # Gauge Converter
│   ├── caston/            # Cast On Calculator
│   ├── yarn/              # Yarn Estimator + OCR
│   ├── needles/           # Needle Size Converter
│   ├── settings/          # Settings screen
│   ├── theme/             # Theme configuration, colors, typography
│   └── components/        # Shared UI components
├── data/
│   ├── db/                # Room database, DAOs, entities
│   ├── preferences/       # DataStore preferences
│   └── repository/        # Repository pattern
├── domain/
│   ├── calculator/        # Pure calculation logic (all six tools)
│   └── model/             # Domain models
├── ai/
│   ├── ocr/               # ML Kit text recognition wrapper
│   └── nano/              # Gemini Nano instruction parser
├── billing/               # Google Play Billing wrapper
└── widget/                # Glance widget
```

### 9.2 Key Dependencies

```kotlin
// Jetpack Compose + Material 3
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose")

// Room for local database
implementation("androidx.room:room-runtime")
implementation("androidx.room:room-ktx")
ksp("androidx.room:room-compiler")

// DataStore for preferences
implementation("androidx.datastore:datastore-preferences")

// ML Kit Text Recognition (on-device)
implementation("com.google.mlkit:text-recognition:16.x.x")

// ML Kit GenAI APIs (Gemini Nano)
implementation("com.google.android.gms:play-services-mlkit-genai:1.x.x")

// Google Play Billing
implementation("com.android.billingclient:billing-ktx:7.x.x")

// Glance for widgets
implementation("androidx.glance:glance-appwidget:1.x.x")

// CameraX for OCR viewfinder
implementation("androidx.camera:camera-camera2")
implementation("androidx.camera:camera-lifecycle")
implementation("androidx.camera:camera-view")
```

**Note:** Verify all version numbers at implementation time. Versions listed here are approximate.

### 9.3 Calculation Logic

All calculation functions are pure Kotlin functions with no side effects. They live in `domain/calculator/` and are unit-testable independently of Android framework.

Example signature:
```kotlin
fun calculateEvenIncrease(
    currentStitches: Int,
    increaseBy: Int
): IncreaseResult

data class IncreaseResult(
    val totalStitches: Int,
    val easyPattern: String,      // "K14, M1, (K15, M1) × 7"
    val balancedPattern: String,  // "K15, M1, (K14, M1) × 4, (K15, M1) × 3"
    val isValid: Boolean,
    val errorMessage: String?
)
```

---

## 10. Quality & Code Standards

Follow the same standards as runcheck:
- **detekt** for static analysis
- **Android Lint** enabled, no warnings in production code
- **Unit tests** for all calculator functions (pure math — easy to test)
- **UI tests** for critical flows (counter increment, IAP flow)
- **No warnings** policy in production builds
- **ProGuard/R8** minification enabled for release
- **Edge-to-edge** display with proper system bar insets
- **All strings** in resources file for future localization (English only at launch)

---

## 11. Play Store Listing (Draft)

**Category:** Tools (or Lifestyle — test both)
**Content rating:** Everyone

**Short description (80 chars max):**
TBD — based on ASO keyword research

**Full description:**
TBD — to be written after app name is decided

**Screenshots:**
- Home screen (dark theme)
- Home screen (light theme)
- Row Counter in use
- Increase/Decrease calculator with result
- Gauge Converter
- Yarn Estimator with OCR scan

**Feature graphic:**
TBD — sage green color scheme with knitting imagery

---

## 12. Development Phases

### Phase 1: Core (MVP)
- Home screen with 6 cards and images
- All 6 calculator tools with full free-tier functionality
- Theme system (dark/light/system)
- Settings screen
- Room database for counter persistence
- DataStore for preferences

### Phase 2: Pro Features
- Google Play Billing integration
- IAP: `knittools_pro`
- Reverse trial (7 days)
- Unlimited projects, full history, notes, secondary counters
- ML Kit OCR yarn label scanning

### Phase 3: AI & Widget
- Gemini Nano instruction parser (capability check + graceful degradation)
- Home screen widget (Glance)

### Phase 4: Polish & Launch
- Play Store listing optimization (ASO research)
- Screenshots and feature graphic
- Final testing on Pixel 9
- Data Safety form
- Launch

---

## 13. Notes for Claude Code

1. All calculator logic should be pure functions in `domain/calculator/` — no Android dependencies, fully unit-testable.
2. Gemini Nano availability must be checked at runtime. If not available, hide the feature entirely. Never show an error or grayed-out button.
3. The OCR camera screen should be simple — viewfinder with a capture button, not a full camera app.
4. The reverse trial timer starts from first app launch, not installation. Store timestamp in DataStore.
5. Home screen card images are bundled as drawable resources (not downloaded). They should be compressed appropriately to keep APK size reasonable.
6. The app must support both dark and light themes. Test both thoroughly. Card images look the same in both themes (text overlay is on the photo).
7. Keep screen awake option for Row Counter should use FLAG_KEEP_SCREEN_ON on the counter screen only, not globally.
8. Every "destructive" action (reset counter, delete project) requires a confirmation dialog.
9. The app name is **KnitTools** — package name is `com.finnvek.knittools`.
10. Follow the same code quality standards as runcheck: detekt, lint, no warnings.
