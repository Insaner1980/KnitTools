# KnitTools Online / Offline Capabilities

**Last checked against source:** 2026-10-11

This document distinguishes local app work, website/service requests, and Android document-provider operations. It describes implementation, not a newly executed offline/device campaign. [PROJECT.md](PROJECT.md) contains the detailed ownership and evidence boundaries.

## Quick Reference

| Feature | Local/offline behavior | Network boundary |
| --- | --- | --- |
| Projects and folders | Create/manage, sort/filter, complete/reopen, and delete local Room data | No content service; active-project admission depends on resolved entitlement |
| Counters | Main/secondary/extra/stitch counters, targets, history, Undo, linked/repeating/shaping/repeat-section rules | No network mutation; Pro rules apply to gated creation/use |
| Notes and reminders | Local notes/base-merge saving and in-app row reminders | No cloud processing or scheduled push service |
| Work sessions | Start/Stop, persistent timing anchors, recovery review, and completed history | No background server timer; device clock/boot anchors determine recovery |
| Insights | Local session/project/completion aggregation and activity fabric | Charts/streak access depends on Pro state, not remote calculation |
| Calculators and references | Measurements/gauge, cast-on, increase/decrease, yarn estimates, regex paste and static references | No model/API call; regex paste remains entitlement-gated |
| Yarn and usage | Local card edits/links/photos, project notes and nullable meter amounts | New Library cards require Pro; no stash cloud service |
| Progress photos | Local capture, storage, gallery and All Photos | A selected external/cloud image source may need its provider's network |
| Saved Patterns | Existing source metadata and private PDFs remain local | Remote HTTPS thumbnails and website opening need network |
| PDF reading/markup | Private PDFs, per-document reader state, guides, mappings, bookmarks, layers/chart tools and rendering/export generation | External source/destination document providers may need network |
| Ravelry | Previously saved metadata and downloaded private PDFs remain usable | WebView pages/sign-in and selected HTTPS PDF downloads use the website and hosted services |
| Backup/restore | Logical ZIP creation/validation and confirmed local replacement | Selected SAF provider can be local or cloud-backed; no automatic sync |
| Widget | App-owned state and repository counter updates | Initial trial/Play entitlement must resolve; no separate widget data server |
| Settings/language/theme | Local preferences and Android per-app locales | External Help and Guide opens a website |
| Trial/Play features | Trial timing is local and user-started | Purchase/restore, review and updates use Google Play; offline Play availability is not guaranteed |
| Diagnostics/analytics | Local product work does not require sending usage content | Release Crashlytics; optional release PostHog; configured debug Sentry |

“Local” assumes required private files exist and the requested operation passes its entitlement/data checks. It does not promise every Pro operation works before Play state loads. The app waits for initial Pro/Billing readiness for widget gates; this is not an independent permanent offline purchase cache guarantee.

## Local Product Work

Room, DataStore, app-owned files and pure domain logic supply:

- Project overview/list, folders, SQL sorting, filtered selection, names/craft/labels, targets, lifecycle and completion events.
- Main counter/history/Undo, legacy secondary count, stitches, additional counters, sections and in-app row reminders.
- Explicit sessions that survive backgrounding/process recreation through persistent anchors; recovery is reviewed when time/boot identity is untrusted.
- Notes, yarn metadata/links/quantities, project yarn notes, and independent planned/allocated/used meters.
- Private PDFs and project document management, reading guides/calibration/bookmarks, bounded annotation tools and rasterized marked-PDF creation.
- Progress photos, camera/gallery photo-to-PDF conversion, Library metadata and locally copied documents/images.
- Locale-aware Insights, local calculators and reference tables, widget state and preferences.

Calculator paste uses `domain/calculator/InstructionParser`, a bounded regex parser for selected English forms. There is no generative model, language-service request, voice recognizer, or cloud note rewrite. Reference translations do not imply arbitrary-language instruction parsing.

## Ravelry And Pattern Websites

### Current Ravelry Browser

Tools → Ravelry, Import from Ravelry, shared Ravelry links, web-editor Ravelry links and Saved Pattern → Open in Ravelry use `RavelryBrowserScreen`.

Users sign in on Ravelry's own website in WebView. Website cookies/DOM storage retain the session. Start URLs accept HTTPS `ravelry.com`/`www.ravelry.com`; otherwise the browser opens its default search page. Checkout/payment links and ordinary non-Ravelry main-frame links open externally. HTTPS redirects/subframes can remain in-app, so this is not a universal Ravelry-only network-host allowlist.

Selected PDF downloads use `WebPdfDownloader`: HTTPS on every hop, per-URL website cookies, up to five redirects and a 100 MiB byte ceiling. A temporary download is validated/copied into private storage and passed to Save Pattern. Once saved privately, reading it is local; an unfinished download or website page is not promised offline access.

Android no longer calls Firebase Auth/Functions for Ravelry or handles native OAuth completion callbacks. API search/import/auth UI was removed. The retained `functions/` backend is separate code with no current Android consumer; its presence is not a user-facing network requirement.

### Other Web Patterns And Thumbnails

User-added web patterns are free local title/designer/URL metadata. Opening them revalidates the URL and launches an external browsable activity. HTTP links can be stored with a warning; this does not enable cleartext Ravelry downloads. No general website download/offline-page capture is implemented.

`RemotePatternImage` loads valid HTTPS thumbnail URLs through Coil. Invalid/missing/failed images are omitted. A thumbnail is not a durable project-owned image or a guaranteed offline asset; existing metadata/PDF work does not depend on its successful download.

## Android File Providers And Backup

PDF import uses Storage Access Framework `OpenDocument`; gallery uses Photo Picker with a specific narrow/large-font SAF fallback. Incoming PDFs and accepted images are copied privately because a temporary URI grant is not permanent file ownership. Opening an arbitrary external URI can require the selected provider to fetch content first.

Marked-PDF export and `.knittools-backup` export select a destination through SAF. A local provider can work offline; Drive/Dropbox/other installed providers may require their own connection, authentication or synchronization. A successful local archive is distinct from provider-side durability, and failed writes may leave partial destination bytes.

Manual backup contains logical records and referenced durable files; confirmation replaces current app data rather than merging it. It excludes preferences, trial/Play state, website cookies and widget configurations. It is unencrypted. Automatic Android backup/device transfer is disabled separately.

There is no Drive/Dropbox SDK, storage-provider OAuth, continuous background synchronization, cross-device live counting or conflict/merge service. [config/future-sync-spec.md](config/future-sync-spec.md) describes future specification work, not shipped synchronization.

## Play, Telemetry And Startup

Google Play manages `knittools_pro` purchase/restore, review and flexible updates. The trial is evaluated locally from wall/elapsed/boot anchors. Reaching a local review threshold does not guarantee Play displays review UI; live service/product availability was not checked here.

Release Crashlytics collection is configured independently of the usage-analytics setting. PostHog is release-only and off by default; after consent it sends predefined usage/lifecycle events and normalized screen names to the configured EU endpoint. No project content/IDs, URLs, notes, search text or billing state are supplied by that app event interface. Disabling it stops future app capture, not deletion of previously sent records.

Debug Sentry is initialized only with a configured DSN; release has a no-op implementation and no Sentry dependency. Network failures and telemetry capture are not a remote authority for project/session persistence. Startup applies local language asynchronously, initializes trial/Billing state and recovers/prunes owned files; it does not initialize Firebase anonymous Ravelry authentication, App Check or an AI service.

## Intentionally Absent Surface

Voice/microphone, SpeechRecognizer/TextToSpeech, model-backed instruction parsing, journal processing, yarn-label interpretation, cloud project-content processing, release Sentry and session replay are absent. Old specifications do not turn those features into active implementations.

See [PRIVACY-POLICY.md](PRIVACY-POLICY.md) for the current technical privacy draft and [PROJECT.md](PROJECT.md#testing-and-evidence) for source-bound historical evidence. No new device/offline, provider, Ravelry, Play or telemetry run was performed for this document update.
