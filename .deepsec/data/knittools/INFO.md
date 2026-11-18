# KnitTools security context

KnitTools is an Android/Kotlin app for knitting projects, counters, pattern documents, photos, Ravelry integration, billing, and home-screen widgets.

High-value user data:
- Project names, row/stitch counters, progress notes, pattern instructions, and imported pattern documents.
- Backend-only Ravelry OAuth tokens and Secret Manager client credentials. Android must not receive or persist them.
- Local files exposed through the app FileProvider.
- Billing/Pro entitlement state and widget actions that can mutate counters.

Important trust boundaries:
- `MainActivity` is exported for launcher, `knittools://ravelry-auth-complete` callbacks, and shared plain text. Treat all intent data, shared text, and callback parameters as untrusted.
- `CounterWidgetReceiver` is exported for AppWidget updates with `android.permission.BIND_APPWIDGET`; `CounterWidgetActions` should remain non-exported.
- File sharing must use narrow FileProvider paths, temporary URI grants, and ClipData when sending content URIs to other apps.
- Model-backed parsing, AI help, OCR, voice, and microphone commands are intentionally absent; do not reintroduce them without a new product/security decision.
- Ravelry token exchange and storage are owned by Firebase Functions and Firestore. Secret Manager owns the client credentials, Firestore rules deny client token access, and Android must remain free of Ravelry secrets and tokens.

Security expectations:
- `android:allowBackup` and `android:usesCleartextTraffic` should stay false unless there is a documented, narrow exception.
- Exported components need explicit caller/input validation and should not perform privileged mutations from untrusted intents.
- Logs must not include user project data, counters, pattern text, billing details, Ravelry tokens, OAuth codes, or credentials.
- FileProvider XML must not expose broad roots such as `root-path`, generic external storage, or `path="."`.
- Dependency CVE findings are handled by OWASP Dependency-Check, not Deepsec.
