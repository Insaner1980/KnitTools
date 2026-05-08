# KnitTools - Firebase AI Logic Setup Guide

**Type:** Historical setup guide and current implementation reference  
**Status:** Setup completed in the current codebase  
**Current source of truth:** `PROJECT.md`, `ONLINE_OFFLINE.md`, and the files listed below

## Current Implementation

Firebase AI Logic is already wired into the app.

Implemented files:

| File | Purpose |
|------|---------|
| `app/google-services.json` | Firebase project config |
| `app/src/main/java/com/finnvek/knittools/ai/GeminiAiService.kt` | Shared Firebase AI / Gemini wrapper |
| `app/src/main/java/com/finnvek/knittools/ai/AiQuotaManager.kt` | Monthly AI credit tracking |
| `app/src/main/java/com/finnvek/knittools/di/NetworkModule.kt` | Network-related Hilt dependencies |
| `app/src/main/java/com/finnvek/knittools/pro/ProState.kt` | `ProFeature.AI_FEATURES`, `VOICE_COMMANDS`, `VOICE_LIVE`, `OCR`, and related gates |

Gradle wiring:

- Top-level `build.gradle.kts` declares the Google Services plugin
- `app/build.gradle.kts` applies Google Services
- `app/build.gradle.kts` includes Firebase BoM and `firebase-ai`
- `gradle/libs.versions.toml` owns Firebase plugin/dependency versions

## Active AI Surface

Cloud AI calls go through `GeminiAiService`:

- `generateText(...)` uses `gemini-2.5-flash-lite`
- `generateTextForVoice(...)` uses `gemini-2.5-flash`
- `generateFromImage(...)` sends bitmap + prompt multimodal input

Current cloud AI consumers include:

- Pattern instruction extraction and combining
- Project summary generation
- Yarn label scanning through `YarnLabelGeminiScanner`
- Voice command interpretation fallback
- AI journal cleanup
- Pattern explanation

AI quota is tracked by `AiQuotaManager` with `MONTHLY_ALLOWANCE = 500`. Voice AI fallback shares this same monthly quota through `hasVoiceQuota()`.

## Historical Setup Steps

The original manual Firebase setup was:

1. Run `./gradlew signingReport` and copy the debug SHA-1.
2. Create or open the Firebase project.
3. Register Android package `com.finnvek.knittools`.
4. Download `google-services.json` into `app/`.
5. Enable Firebase AI Logic with Gemini Developer API.
6. Verify an app AI feature can make a successful request.

## Notes About Stale Older Plans

Older plan text may mention:

- `ProFeature.NANO_FEATURES`
- `di/AiModule.kt`
- `data/DataStoreKeys.kt`
- `YarnLabelNanoParser`
- Nano-based `ProjectSummarizer`

Those are not current implementation details. The current code uses the files and feature flags listed above.

## Security Notes

- The Gemini API key is not hardcoded in app source.
- Firebase project configuration lives in `app/google-services.json`.
- No Analytics or Crashlytics dependency is intentionally part of this setup.
- App Check with Play Integrity is still the relevant production hardening step before broad release.
