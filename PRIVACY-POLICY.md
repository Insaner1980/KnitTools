# Privacy Policy

> **Draft:** Publisher contact information is still required before this policy is published. This document describes the current source implementation; it is not evidence of a deployed release or verified live provider payloads.

*Last checked against implementation: 2026-10-11*

## Local Project Data

KnitTools stores projects, folders, counters/history, notes, work sessions/recovery state, completion events, saved-pattern metadata, PDF documents/annotations/bookmarks, yarn cards/notes/usage and progress photos in private app storage. Preferences, language selection, local trial timing and widget state are also stored on the device.

Local project work does not require a KnitTools account. There is no project-content cloud synchronization or app feature that uploads notes, PDFs or photos for AI analysis. Calculators and instruction paste parsing run locally; the parser uses selected regex rules rather than a model service.

Automatic Android app backup and device transfer are disabled. Manual backup is a separate user-selected operation described below.

## Ravelry And External Websites

Ravelry opens in an in-app WebView. Sign-in takes place on Ravelry's own website; WebView cookies and website storage retain that website session. KnitTools does not implement its own Ravelry password handler, OAuth token exchange, Firebase anonymous authentication or callable Ravelry API requests in Android.

The browser sends normal website requests, including applicable cookies and a user agent. Checkout/payment links open in an external browser. HTTPS redirects and embedded pages can involve other hosts; browsing is not restricted to one server. Website operators handle their own account, page and payment data.

When you select a PDF download, the app requests it over HTTPS with cookies appropriate to each requested URL, validates it, and copies it into private storage for Save Pattern. Downloads can follow HTTPS redirects to hosted document services. A saved private PDF can be read locally afterward. This does not upload the PDF to a KnitTools backend.

User-added web patterns store local metadata. Opening their website launches an external browser; KnitTools does not implement general offline website capture. Remote pattern thumbnails may be loaded from their HTTPS image hosts. Those hosts receive the image requests; thumbnail loading is distinct from storing your project photos.

The repository retains historical Firebase Ravelry backend code, but the current Android app does not call it. It is not an active app account/token-storage service for this implementation.

## Release Crash Reporting

Release builds enable automatic crash and application-not-responding (ANR) reporting through Firebase Crashlytics. The usage analytics setting controls PostHog only.

Crash reports can include technical information such as stack traces and app/device diagnostics handled by the SDK. The app's restricted usage-event interface does not govern Crashlytics reports. R8 mapping upload is configured for release builds to support readable crash traces. Debug/profile-producer variants disable Crashlytics collection/upload.

## Optional Usage Analytics

Usage analytics is **off by default**. You can enable or disable it in Settings. It operates in release builds through PostHog with the configured EU ingestion endpoint.

The app supplies predefined events for project creation, PDF import success/failure, counter increment/decrement and work-session actions. It also records app foreground/background and allowlisted screen visits/durations. Route parameters are removed before screen names are captured.

The app does not supply project names/content/IDs, pattern URLs, notes, PDFs, photos, yarn content, numeric counter values, search text, billing state or user profiles to this usage-event interface. SDK-managed installation/session identifiers and app/device context may accompany events. GeoIP enrichment is disabled; using an EU endpoint is not a statement that every provider processing/retention detail has been verified.

Automatic screen/lifecycle/deep-link capture, push capture, session replay, surveys, feature-flag loading and automatic error/native-crash capture are disabled in the PostHog configuration. Screen and lifecycle events are sent through the app's explicit limited interface.

Disabling usage analytics stops future app capture. It does not request deletion of records already sent to the provider. This document does not invent provider retention periods or claim an in-app remote-data deletion facility.

## Google Play

Google Play handles the one-time `knittools_pro` purchase, purchase restoration, review requests and app updates. Billing exchanges product/purchase information with Play to determine entitlement and acknowledge purchases. The current app has no separate backend purchase-validation service.

The user-started 14-day trial is evaluated from locally stored timing/boot anchors. Trial and Play state are excluded from manual project backups. No payment-card form is implemented by KnitTools; Ravelry website checkout uses the external browser flow described above.

## Imports, Exports And Manual Backup

You select source/destination documents with Android's Storage Access Framework or images with Photo Picker/the supported document-picker fallback. Providers can be local or cloud-backed and handle their own authentication/network behavior. Incoming PDFs and selected images are copied into app-owned storage for the app's workflows.

Manual `.knittools-backup` files include project records and referenced PDFs/progress/yarn photos. They are **unencrypted and unauthenticated** ZIP archives: anyone with access to an exported file can read its content. Backup excludes settings/analytics consent, trial/purchases, WebView cookies, credentials and widget configurations. Restore replaces current project content; it does not synchronize or merge devices.

Marked-PDF export writes a user-selected copy; the original stays private unless separately exported. FileProvider grants expose only the app's pattern-capture and progress-photo paths for their intended operations. Calculator Copy places the selected result text in the system clipboard.

## Debug Diagnostics

Debug builds can send diagnostics to Sentry only when a developer supplies a DSN. No DSN means no initialization. Automatic tracing/profiling, screenshots/view hierarchy and broad automatic collection are disabled. Release builds contain no Sentry dependency and use a no-op initialization path.

## Permissions

- **Internet:** website browsing/downloads, remote images, Play operations and configured telemetry.
- **Vibration:** preference-controlled haptic feedback.

KnitTools opens the device's camera app for user-started pattern and progress photo capture. Camera hardware is optional, and KnitTools does not request camera permission. The app does not request microphone permission or broad storage access. Document/image access uses the Android pickers and grants. There is no speech recognition, text-to-speech or conversational voice flow.

## Retention And Deletion

Local records remain until their applicable delete/replace action or app-data removal. Project deletion cascades project-owned records; shared PDFs remain while another Library/project reference uses them. Physical cleanup happens after the database mutation and is best effort, so an interrupted/failed cleanup can leave an orphan for its recovery/pruning path.

Exported files and copies held by selected external providers are outside the app's private storage; deleting a local project does not delete those exports. Clearing/uninstalling app data does not revoke a Play purchase or delete data already held by website/telemetry operators. No universal remote-data deletion action is implemented in KnitTools.

## Contact

Publisher contact information has not been supplied in this repository. Supply the actual responsible publisher/contact channel before publishing this policy; no email address or retention/legal commitment is inferred here.

For the engineering reference and verification limits, see [PROJECT.md](PROJECT.md#security-privacy-and-telemetry) and [ONLINE_OFFLINE.md](ONLINE_OFFLINE.md).
