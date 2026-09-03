# KnitTools Online / Offline Capabilities

**Last verified against codebase:** 2026-09-03

This document maps user-facing features to their real network requirement, based on the current code.

## Quick Reference

| Category | Works fully offline | Requires network | Notes |
|---|---|---|---|
| Counters & projects | Yes | No | Room, DataStore, local files |
| Calculators | Yes | No | Paste-to-parse uses a local regex parser |
| Reference data | Yes | No | Hardcoded data sources |
| Yarn cards | Yes | No | Manual yarn card creation only |
| Pattern viewer | Yes | No | Saved PDF rendering and annotations are local |
| Saved patterns | Local metadata and PDFs | External websites and remote thumbnails | Imported PDFs remain local; website links open outside the app |
| Ravelry | Saved metadata only | Yes | Authentication, search, and metadata import use Firebase; there is no Ravelry PDF download |
| Voice commands | Not implemented | No | Voice and microphone commands are intentionally absent |
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
- Saved pattern library metadata, imported local PDFs, PDF rendering, annotations, page navigation, zoom, and current-row marking.
- Progress photos and All Photos.
- Settings screen state, except platform billing/update/review actions.

## Network Features

These features require internet because they call external services:

- Firebase anonymous authentication and callable Functions for Ravelry connection state, search, and metadata import.
- Server-side Ravelry OAuth and token refresh. Android does not receive or store Ravelry access or refresh tokens.
- HTTPS Ravelry thumbnails shown from metadata. The app does not copy them into project or pattern storage.
- Opening a user-added web pattern in an external browser. The app does not download or cache the page.
- Google Play Billing purchase and restore flows.
- Google Play in-app review and in-app update flows.

## Voice Commands

Voice and microphone commands are intentionally absent. The app does not use `SpeechRecognizer`, `TextToSpeech`, or a conversational voice service.

## Removed Product Surface

The current app surface does not include model-backed summary, journal cleanup, yarn label image interpretation, pattern instruction interpretation, conversational voice, or on-device generative parsing. Do not describe those as active features in product copy, help text, support material, or tests unless a new implementation is added.

## Implementation Notes

- App startup initializes language preferences, billing, and Pro state. It no longer initializes a model service or App Check provider.
- Calculator paste parsing is local and deterministic. Keep it in `domain/calculator/InstructionParser`.
- Saved PDFs are app-owned files under the pattern document storage flow.
- Pattern camera capture temp images live under `pattern_captures/<projectId>` and are exposed only through FileProvider.
- Ravelry metadata access is the only app-owned remote integration and goes through Firebase Auth and callable Functions. Ravelry PDFs are not downloaded.

## What Changed Recently

- **2026-09-03** - Reverified the Firebase-backed, metadata-only Ravelry path and the intentionally absent voice/microphone surface.
- **2026-05-22** - Removed the model-backed feature surface and its external model dependencies. Local regex paste parsing remains.
