# KnitTools Online / Offline Capabilities

**Last verified against codebase:** 2026-05-22

This document maps user-facing features to their real network requirement, based on the current code.

## Quick Reference

| Category | Works fully offline | Requires network | Notes |
|---|---|---|---|
| Counters & projects | Yes | No | Room, DataStore, local files |
| Calculators | Yes | No | Paste-to-parse uses a local regex parser |
| Reference data | Yes | No | Hardcoded data sources |
| Yarn cards | Yes | No | Manual yarn card creation only |
| Pattern viewer | Yes | No | Saved PDF rendering and annotations are local |
| Saved patterns | Yes | No | Imported/downloaded files remain local |
| Ravelry | No | Yes | Search, detail fetch, OAuth, and PDF download |
| Voice commands | Mostly | No app call | Android speech recognition may depend on device language packs |
| Notes | Yes | No | Typed editor and autosave are local |
| Photos, sessions, insights, widget | Yes | No | Local storage and Room-derived state |
| Google Play platform features | No | Yes | Purchases, restore, review, updates |

## Fully Offline Features

These features operate on Room, DataStore, app-owned files, hardcoded reference data, or pure calculations:

- Row counter, undo, reset, secondary stitch counter, repeat sections, mappings, and row reminders.
- Project list, create, rename, delete, duplicate, reorder, linked yarns, and project notes.
- Session tracking, history, insights, activity grid, and widget state.
- Cast on, gauge, swatch, increase/decrease, and yarn estimator calculators.
- Paste-to-parse in calculator screens through `domain/calculator/InstructionParser`.
- Reference screens: needle sizes, abbreviations, chart symbols, and size charts.
- Yarn cards created and edited manually.
- Saved pattern library, local PDF rendering, annotations, page navigation, zoom, and current-row marking.
- Progress photos and All Photos.
- Settings screen state, except platform billing/update/review actions.

## Network Features

These features require internet because they call external services:

- Ravelry pattern search through `RavelryApiService.searchPatterns`.
- Ravelry pattern detail through `RavelryApiService.getPatternDetail`.
- Ravelry OAuth login and token refresh.
- Importing a pattern PDF from Ravelry or another remote URL. Once saved, the file is available offline.
- Google Play Billing purchase and restore flows.
- Google Play in-app review and in-app update flows.

## Voice Commands

Counter voice commands are local keyword commands under `ui/screens/counter`:

- `VoiceCommandHandler` wraps Android `SpeechRecognizer`.
- `VoiceCommandParser` maps recognized phrases to counter actions.
- `VoiceResponseManager` speaks local TextToSpeech confirmations.

KnitTools does not send recognized phrases to an app-owned network service. The Android speech recognizer itself can still require network on devices without offline recognition support; this is platform behavior outside KnitTools' direct control.

## Removed Product Surface

The current app surface does not include model-backed summary, journal cleanup, yarn label image interpretation, pattern instruction interpretation, conversational voice, or on-device generative parsing. Do not describe those as active features in product copy, help text, support material, or tests unless a new implementation is added.

## Implementation Notes

- App startup initializes language preferences, billing, and Pro state. It no longer initializes a model service or App Check provider.
- Calculator paste parsing is local and deterministic. Keep it in `domain/calculator/InstructionParser`.
- Saved PDFs are app-owned files under the pattern document storage flow.
- Pattern camera capture temp images live under `pattern_captures/<projectId>` and are exposed only through FileProvider.
- Ravelry remains the only app-owned remote API integration.

## What Changed Recently

- **2026-05-22** - Removed the model-backed feature surface and its external model dependencies. Local voice commands and local regex paste parsing remain.
- **2026-04-18** - Voice commands were made offline-friendly for basic counter actions through the local parser and Android TextToSpeech.
