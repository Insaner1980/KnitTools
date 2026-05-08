# KnitTools — Online / Offline Capabilities

**Last verified against codebase:** 2026-05-08

This document maps every user-facing feature to its real network requirement, based on the actual code (not the marketing copy). The goal is to give writers, designers, and support a single source of truth when wording empty states, error toasts, and feature descriptions.

---

## Quick reference

| Category | Works fully offline | Requires network | Hybrid |
|---|---|---|---|
| Counters & projects | ✅ | — | — |
| Calculators | ✅ | — | Paste-to-parse uses on-device Gemini Nano |
| Reference data | ✅ | — | — |
| Yarn cards (manual) | ✅ | — | — |
| Yarn label AI scan | — | ✅ | — |
| Pattern viewer (PDF) | ✅ | — | "Jump to row X" uses cloud AI |
| Saved patterns | ✅ | — | — |
| Ravelry | — | ✅ | — |
| Voice commands | ✅ basic | — | Live API + AI interpretation are online |
| Notes (typing) | ✅ | — | — |
| Voice journal / AI cleanup | — | ✅ | Raw text is kept if offline |
| AI project summary | — | ✅ | — |
| Photos, sessions, insights, widget | ✅ | — | — |

---

## Works fully offline

These features do not touch the network at all. They operate on Room (local DB), DataStore (preferences), files on disk, hardcoded reference data, or pure calculations.

### Project & counter workflow
- **Row counter** (main count, undo, reset, secondary stitch counter, repeat sections, mappings)
- **Multiple project counters** (Pro) — additional row, stitch, and shaping counters per project
- **Shaping counter logic** — increase/decrease shaping with an "every N rows" cadence
- **Row reminders** — local notifications scheduled by `ReminderLogic`
- **Project list, create / rename / delete / duplicate / reorder**
- **Session tracking** — start/stop knitting sessions, total time, sessions per project
- **Linked yarns** between yarn cards and projects
- **Notes (typed)** — bottom sheet and full-screen editor with auto-save

### Calculators
- **Cast On Calculator** — uses `SizeChartData` (hardcoded sizing tables)
- **Gauge Converter / Swatch Calculator** — pure math
- **Increase / Decrease Calculator** — pure math
- **Yarn Estimator** — uses hardcoded weight/yardage tables (`YarnEstimator.kt`)

> **Paste-to-parse** (calculator screens, Pro): the "paste an instruction and we extract the numbers" button uses **Gemini Nano on-device** (`InstructionParser` → `com.google.mlkit.genai.prompt.Generation`). This is offline once the Nano model has been downloaded by the system. If Nano is unavailable, the button is hidden — no cloud fallback.

### Reference data (the Reference screens)
All four reference screens read from hardcoded Kotlin data sources. No network, no DB call beyond local search.
- **Needle Sizes** (`NeedleSizeData`)
- **Knitting Abbreviations** (`AbbreviationData`)
- **Chart Symbols** (`ChartSymbolData`)
- **Size Charts** (`SizeChartData`)

### Yarn cards
- **Browse, search, edit, delete saved yarn cards** — Room only
- **Manually create a yarn card** — fill the form by hand, no AI

### Pattern viewer
- **Open a saved PDF pattern** — pages render with `PdfRenderer`, files are stored under `filesDir/patterns/`
- **Annotations** — local Room DB
- **Navigate pages, zoom, mark current row** — purely local

### Saved patterns library
- **List, search, filter, delete saved patterns** — Room only
- Once a pattern is saved (PDF imported or downloaded earlier), the viewer never needs to go back to the network

### Photos, insights, widget
- **Progress photos** — captured with the camera, stored in `filesDir`, displayed in the project gallery and All Photos screen
- **Insights tab** — graphs and stats are computed from local sessions/counter history
- **Counter widget** — reads project state from Room via `CounterWidgetState`
- **Activity grid** — local session aggregation

### Settings, billing, Pro state
- The settings screen itself is offline. Note that **Google Play Billing** flows (purchasing Pro, restoring purchases, in-app review prompt, in-app updates) require network — but these are platform interactions, not app features per se.

---

## Requires network

These features will not work without a validated internet connection. `CounterViewModel.isOnline()` checks `NET_CAPABILITY_INTERNET` **and** `NET_CAPABILITY_VALIDATED`, so captive portals and "Wi-Fi without data" are treated as offline.

### Cloud AI features (all use Firebase AI Logic → Gemini)

| Feature | Implementation | Model |
|---|---|---|
| **Yarn label AI scan** (Yarn Estimator / Yarn Card) | `YarnLabelGeminiScanner` via `GeminiAiService.generateFromImage` | Gemini 2.5 Flash Lite (multimodal) |
| **Pattern instruction "find row N"** | `PatternInstructionGemini` (in pattern viewer) | Gemini multimodal — sends a rendered page image |
| **Pattern instruction explainer** | `GeminiAiService.explainInstruction` | Gemini Flash |
| **AI project summary** (Counter screen "Summary" card, Pro) | `ProjectSummarizer.summarize` | Gemini Flash |
| **AI voice command interpreter** (fallback when local parser misses) | `VoiceCommandInterpreter` | Gemini Flash |
| **Voice journal cleanup** (full-screen Notes editor → AI button → Speak/Type) | `JournalEntryProcessor` | Gemini 2.5 Flash Lite |
| **Voice Live API conversation** (Pro, opt-in) | `VoiceLiveSession` | Gemini Live API (audio in/out) |

All of the above gracefully degrade: if the call fails or quota (`AiQuotaManager` / `VoiceLiveQuotaManager`) is exhausted, the app shows a localized message instead of crashing. Voice journal explicitly preserves the raw transcript when AI cleanup fails — the user's words are never lost.

### Ravelry integration
- **Pattern search** — `RavelryApiService.searchPatterns` (HTTPS to api.ravelry.com)
- **Pattern detail** — `RavelryApiService.getPatternDetail`
- **OAuth login flow** — redirect-based, requires the browser
- **Importing a pattern PDF** — fetches the PDF over HTTP, then stores it locally; afterwards the pattern viewer is fully offline

> Saved patterns themselves stay accessible offline. Only the Ravelry-side actions (search, fetch new patterns, OAuth) need a connection.

### Google Play platform features
- In-App Purchases (Pro upgrade, restore)
- In-App Review prompt
- In-App Updates

---

## Hybrid features (this is where wording matters)

These behave differently depending on connectivity. They are the most likely places for user confusion, so error and empty-state copy needs to be specific.

### Voice commands (Counter screen) — offline-friendly since commit `a91cf3c`

There are three voice paths, falling back automatically:

1. **Local recognition + local parser** — `VoiceCommandHandler` uses Android's `SpeechRecognizer`. On modern Android with the Google app's on-device language pack downloaded, recognition runs fully on-device. The parser (`VoiceCommandParser`) understands a curated keyword set in EN+FI, including counted forms ("add three", "back five", "next row", "undo", "reset", "stop listening", "help"). TTS confirmation comes from Android's local engine.
   - **Result:** the basic voice commands listed above work without internet.
   - **Caveat:** if the device has no offline speech recognition pack, Android's `SpeechRecognizer` may itself require network to transcribe. This is a device/OS condition KnitTools cannot detect ahead of time. Network errors from `SpeechRecognizer` are handled silently in continuous mode; KnitTools just keeps trying.

2. **AI interpretation** — for ambiguous or non-keyword phrases, `VoiceCommandInterpreter` sends the recognized text to Gemini Flash to decide the action. **Requires network.** If offline, the unrecognized phrase is just ignored.

3. **Voice Live API** (Pro, opt-in toggle) — full duplex audio conversation via `VoiceLiveSession`. **Requires network.** When the user starts Live mode but is offline, KnitTools falls back to v2 (mode #1 + #2) and shows the friendly `voice_offline_mode` hint once per session — no scary error dialog.

> **Bottom line for copy:** "Voice commands work offline for basic actions like next row, undo, and reset. Conversational mode (Live) needs internet."

### Yarn label scanning — one online scanner

| Entry point | Scanner | Network |
|---|---|---|
| **Yarn Estimator screen** ("scan yarn label" → estimate skeins) | `YarnLabelGeminiScanner` (cloud Gemini Flash Lite, multimodal) | **Online** |
| **Yarn Card screen** ("create yarn card from label photo", Pro) | `YarnLabelGeminiScanner` (cloud Gemini Flash Lite, multimodal) | **Online** |

The previous ML Kit OCR → Gemini Nano → regex yarn-label pipeline has been removed from the production surface. The camera file creation lives in `data/storage/YarnLabelPhotoStorage`; parsing lives in `ai/YarnLabelGeminiScanner`. Manual yarn card creation is always offline.

### Pattern viewer — PDF is offline, "smart row" is online

- Reading a saved PDF, swiping pages, marking the current row: **offline**
- The "go to row N" / "where am I in the pattern" feature that finds an instruction by row number: **online** (`PatternInstructionGemini` sends the page image to Gemini)
- Importing a new pattern from Ravelry or via URL: **online** (one-time fetch, then offline forever)

### Notes editor — text is offline, AI journal entry is online

- Typing notes, viewing notes, auto-saving: **offline**
- The "+ AI" button in the full-screen editor opens a Speak/Type bottom sheet:
  - Speak path: `SimpleSpeechRecognizer` for transcript (same caveat as voice commands above) → `JournalEntryProcessor` (Gemini, **online**) cleans punctuation → appended with date/row header
  - Type path: typed text → same `JournalEntryProcessor` cloud cleanup
  - **Offline / quota / no-Pro fallback:** the raw text is appended unchanged. The user's words are preserved either way.

---

## Implementation notes for future contributors

- **Network check** lives in `CounterViewModel.isOnline()`. Use the same shape (validated capability) for any new online-gated feature.
- **AI quotas** are tracked separately for general AI calls (`AiQuotaManager`, 500/month shared by general AI and voice fallback) and Live API time (`VoiceLiveQuotaManager`).
- **All Gemini cloud calls** funnel through `GeminiAiService` — there are no scattered Firebase calls elsewhere in the codebase.
- **On-device AI** (Gemini Nano) goes through `com.google.mlkit.genai.prompt.Generation`. Availability is checked via `NanoAvailability.check()`. Nano features hide their UI when Nano is unavailable rather than falling back to cloud.
- When adding a new AI feature, decide explicitly which lane it belongs in: cloud-only, on-device only, or hybrid with a non-AI fallback. The codebase currently has examples of all three.

---

## What changed recently

- **2026-05-08** — Yarn label scanning was consolidated onto the Firebase AI / Gemini multimodal scanner. The old offline ML Kit OCR + Nano + regex scanner was removed from the production surface.
- **2026-04-18 (`a91cf3c`)** — Voice commands made offline-friendly. Local parser + Android TTS now confirms commands without touching the cloud. Live API errors no longer surface raw error text — users see the positive `voice_offline_mode` hint once per session.
- **2026-04-16** — AI Journal entry added (full-screen Notes editor → AI button). Online-only AI cleanup with raw-text fallback.
- **Voice Live API** — wired into the Counter UI through `CounterScreen`, `CounterViewModel`, and `VoiceLiveSession`.
