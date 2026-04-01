4. AI Features

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
