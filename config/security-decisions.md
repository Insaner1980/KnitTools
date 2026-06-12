# Security Decisions

## Ravelry embedded credentials

Status: Removed from Android; superseded historical accepted risk

Decision date: 2026-05-15
Superseded date: 2026-06-11
Android removal date: 2026-06-11
Release-surface hardening date: 2026-06-12

KnitTools previously had no backend service for Ravelry access. That decision is superseded by `Ravelry Firebase Backend And Saved Patterns Plan.md`: the target architecture moves Ravelry secrets, token exchange, API calls, auth status, disconnect, search, and import to Firebase Auth plus Cloud Functions v2.

The old Android client-only secret path has now been removed from Android. Android no longer defines Ravelry credential `BuildConfig` fields, no longer stores Ravelry access/refresh tokens in `EncryptedSharedPreferences`, and no longer uses the Basic Auth fallback for Ravelry API calls. Ravelry secrets and token exchange belong to Cloud Functions only.

Mitigations:

- Android Firebase integration uses anonymous Firebase Auth plus callable Cloud Functions. `app/google-services.json` stays ignored and must be provided locally or generated in CI from `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64`.
- New Ravelry work must not add fresh Android secret surfaces; secrets and token exchange belong server-side.
- Phase 3 backend code owns OAuth2 start/callback/status/disconnect/current-user flow server-side. It stores PKCE state in `ravelryOAuthStates/{state}` and tokens in `ravelryTokens/{uid}`.
- Phase 4 backend search/import is metadata-only. `ravelrySearchPatterns`, `ravelryImportPatternById`, and `ravelryImportPatternByUrl` sanitize API responses to Ravelry ID, title, designer, thumbnail, canonical URL, original URL, availability, and pagination; they do not download paid/private PDFs or return raw Ravelry response bodies.
- Phase 5 Android code owns only Firebase client dependencies, anonymous auth, and callable client mapping. Phase 6 Android code owns backend auth UI state, Auth Tab launch with Custom Tabs fallback, backend status/start/disconnect delegation, and token-free `knittools://ravelry-auth-complete` callback handling; Android still does not receive Ravelry access or refresh tokens.
- Phase 9 release-surface hardening allows only Firebase Auth, Firebase Functions, and the Google Services plugin/config path for this backend. `tools/release-surface.ps1` still forbids Firebase AI, ML Kit, Gemini/Google Generative AI, voice/speech dependencies, release-path Sentry, and broad FileProvider roots.
- `app/google-services.json` may exist only as an ignored local or CI-generated file; tracking it in git fails the release-surface check.
- `tools/release-surface.ps1` scans source, resources, generated constants, Gradle files, manifests, tests, APKs, and AABs for locally or environment-provided known Ravelry secret values. The scan reports only file locations and never prints the secret value.
- Semgrep and DeepSec now treat the old Android Ravelry BuildConfig/Gradle secret surfaces as removed-risk regressions, not accepted risk.
