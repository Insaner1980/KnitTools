# AI Features

This document describes the current AI feature surface. `PROJECT.md` remains the broader source of truth.

## 4.1 Gemini Nano - Instruction Parser (Pro)

**Purpose:** Parse natural-language knitting instructions into calculator inputs.

**How it works:**

- Implemented in `app/src/main/java/com/finnvek/knittools/ai/nano/InstructionParser.kt`
- Used by the shared `PasteInstructionButton`
- Available only when Pro access is active and `NanoAvailability.isUsable()` returns true
- Uses ML Kit GenAI Prompt API / AICore on-device
- Does not use the network
- Hidden when Nano is unavailable

Supported current surfaces include Increase/Decrease and Gauge parsing.

## 4.2 Yarn Label Scanner (Pro)

**Purpose:** Extract yarn information from a photographed label.

**Current implementation:**

- Implemented in `app/src/main/java/com/finnvek/knittools/ai/YarnLabelGeminiScanner.kt`
- Called through `YarnCardViewModel.scanWithGemini(...)` via `YarnLabelScanRepository`
- Camera file creation lives in `app/src/main/java/com/finnvek/knittools/data/storage/YarnLabelPhotoStorage.kt`
- Parsed output uses the shared `app/src/main/java/com/finnvek/knittools/ai/ParsedYarnLabel.kt` model
- Uses Firebase AI / Gemini multimodal image input through `GeminiAiService.generateFromImage(...)`
- Requires network
- The old ML Kit OCR -> Gemini Nano -> regex yarn-label pipeline is no longer in production code

Manual yarn card creation remains offline.
