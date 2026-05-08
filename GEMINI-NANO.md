# KnitTools - Gemini Nano Features

This document covers the on-device Gemini Nano surface that still exists in the codebase. Cloud AI features are documented in `PROJECT.md` and `ONLINE_OFFLINE.md`.

**Platform:** On-device Gemini Nano via ML Kit GenAI / AICore  
**Visibility:** Hidden when `NanoAvailability.isUsable()` returns false  
**Gate:** Pro-only through the caller's Pro feature checks

## Implemented

### 1. Instruction Parser - Increase/Decrease

**File:** `app/src/main/java/com/finnvek/knittools/ai/nano/InstructionParser.kt`  
**UI:** Increase/Decrease calculator through `PasteInstructionButton`  
**Status:** Implemented

Parses increase/decrease instructions and fills calculator inputs. Examples include:

- "Increase 12 stitches evenly across 96 stitches"
- "Dec 8 sts over 120 sts"
- "Increase to 108 from 96"
- "K2tog every 12th stitch (96 sts)"

### 2. Instruction Parser - Gauge

**File:** `app/src/main/java/com/finnvek/knittools/ai/nano/InstructionParser.kt`  
**UI:** Gauge calculator through `PasteInstructionButton`  
**Status:** Implemented

Parses gauge/tension instructions such as:

- "22 sts and 30 rows = 10cm"
- "Tension: 28 sts x 36 rows to 10cm on 4mm needles"
- "5.5 sts per inch, 7 rows per inch"

### 3. Instruction Parser - Swatch Measurement

**File:** `app/src/main/java/com/finnvek/knittools/ai/nano/InstructionParser.kt`  
**UI:** Gauge calculator through `PasteInstructionButton`  
**Status:** Implemented

Parses swatch measurement text such as:

- "Measured width is 30 cm"
- "Width 30, 22 stitches"
- "My swatch is 12cm wide with 26 stitches"

## Removed Or Moved

### Yarn label parsing

The previous `ai/nano/YarnLabelNanoParser.kt` and OCR/regex yarn-label path is no longer present in production code. Current yarn label scanning uses `app/src/main/java/com/finnvek/knittools/ai/YarnLabelGeminiScanner.kt` through Firebase AI / Gemini multimodal image input and requires network.

### Project summary

Project summary is no longer a Nano implementation under `ai/nano`. Current code uses `app/src/main/java/com/finnvek/knittools/ai/ProjectSummarizer.kt`, with cloud Gemini through `GeminiAiService` plus a local `simpleSummary(...)` fallback.

## Principles

- Nano features should hide their UI when Nano is unavailable.
- Nano should not invent project facts; facts must come from user input or local project data.
- Any high-risk calculation should remain deterministic in Kotlin, with AI used only for parsing or wording when appropriate.
