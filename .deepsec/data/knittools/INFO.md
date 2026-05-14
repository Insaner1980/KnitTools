# KnitTools security context

KnitTools is an Android/Kotlin app for knitting projects, counters, pattern documents, photos, OCR, AI help, Ravelry integration, billing, and home-screen widgets.

High-value user data:
- Project names, row/stitch counters, progress notes, pattern instructions, OCR text, generated AI explanations, voice input, and imported pattern documents.
- Ravelry OAuth access tokens and client credentials used for API calls.
- Local files exposed through the app FileProvider.
- Billing/Pro entitlement state and widget actions that can mutate counters.

Important trust boundaries:
- `MainActivity` is exported for launcher and `com.finnvek.knittools://oauth/callback` OAuth deep links. Treat all intent data and OAuth callback parameters as untrusted.
- `CounterWidgetReceiver` is exported for AppWidget updates with `android.permission.BIND_APPWIDGET`; `CounterWidgetActions` should remain non-exported.
- File sharing must use narrow FileProvider paths, temporary URI grants, and ClipData when sending content URIs to other apps.
- Firebase AI usage should use App Check limited-use tokens. AI prompts, OCR text, pattern instructions, and voice data should not be logged.
- Ravelry tokens are expected to be stored in encrypted storage. Release builds currently guard embedded Ravelry secrets via build-time checks; review this area carefully.

Security expectations:
- `android:allowBackup` and `android:usesCleartextTraffic` should stay false unless there is a documented, narrow exception.
- Exported components need explicit caller/input validation and should not perform privileged mutations from untrusted intents.
- Logs must not include user project data, counters, pattern text, OCR/AI prompt content, billing details, Ravelry tokens, OAuth codes, or credentials.
- FileProvider XML must not expose broad roots such as `root-path`, generic external storage, or `path="."`.
- Dependency CVE findings are handled by OWASP Dependency-Check, not Deepsec.
