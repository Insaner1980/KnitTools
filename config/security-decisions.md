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

- Android Firebase integration uses anonymous Firebase Auth plus callable Cloud Functions. Release-capable `app/google-services.json` stays ignored and must be provided locally or generated in CI from `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64`; local debug builds may generate ignored `app/src/debug/google-services.json` placeholder config when no real Firebase project config is available.
- New Ravelry work must not add fresh Android secret surfaces; secrets and token exchange belong server-side.
- Phase 3 backend code owns OAuth2 start/callback/status/disconnect/current-user flow server-side. It stores PKCE state in `ravelryOAuthStates/{state}` and tokens in `ravelryTokens/{uid}`.
- Phase 4 backend search/import is metadata-only. `ravelrySearchPatterns`, `ravelryImportPatternById`, and `ravelryImportPatternByUrl` sanitize API responses to Ravelry ID, title, designer, thumbnail, canonical URL, original URL, availability, and pagination; they do not download paid/private PDFs or return raw Ravelry response bodies.
- Phase 5 Android code owns only Firebase client dependencies, anonymous auth, and callable client mapping. Phase 6 Android code owns backend auth UI state, Auth Tab launch with Custom Tabs fallback, backend status/start/disconnect delegation, and token-free `knittools://ravelry-auth-complete` callback handling; Android still does not receive Ravelry access or refresh tokens.
- Phase 9 release-surface hardening allows only Firebase Auth, Firebase Functions, and the Google Services plugin/config path for this backend. `tools/release-surface.ps1` still forbids Firebase AI, ML Kit, Gemini/Google Generative AI, voice/speech dependencies, release-path Sentry, and broad FileProvider roots.
- `app/google-services.json` and generated `app/src/debug/google-services.json` may exist only as ignored local or CI-generated files; tracking either one in git fails the release-surface check.
- The Firebase Android `current_key` value is a public client identifier, not an authorization secret. The project key is restricted to Firebase APIs and does not allow Generative Language API; this restriction was reconfirmed on 2026-09-15. Exact scanner exceptions may cover only the `current_key` field in ignored `app/google-services.json`, and a verified Gemini finding must remain blocking.
- `tools/release-surface.ps1` scans source, resources, generated constants, Gradle files, manifests, tests, APKs, and AABs for locally or environment-provided known Ravelry secret values. The scan reports only file locations and never prints the secret value.
- Semgrep and DeepSec now treat the old Android Ravelry BuildConfig/Gradle secret surfaces as removed-risk regressions, not accepted risk.

## Firebase Crashlytics

Decision date: 2026-09-14
Status: Approved by the user's explicit installation request

Automatic Firebase Crashlytics crash and ANR reporting is enabled for the release variant. The Crashlytics Gradle plugin uploads release R8 mapping files for readable stack traces. Debug and baseline-profile variants disable collection and mapping uploads; debug Sentry remains unchanged. Google Analytics, breadcrumbs, custom logs, user identifiers, and project metadata are not added.

The SDK uses the existing Firebase BoM and ignored `app/google-services.json` configuration. This is a narrow exception to the earlier Firebase and release crash-reporting restrictions. The in-app privacy summary discloses Crashlytics reporting.

Setup follows https://firebase.google.com/docs/crashlytics/android/get-started and https://firebase.google.com/docs/crashlytics/android/get-deobfuscated-reports. Live verification requires the real Firebase config, a release build on a test device, a controlled test crash, app restart, and confirmation of the report in Firebase Console. Do not ship a test-crash trigger. Store privacy disclosures must account for the enabled SDK before publication.

## PostHog usage analytics

Decision date: 2026-09-14
Status: Approved by the user's request for broad usage information and supplied project token

Release builds may send predefined usage events to PostHog EU after the user enables Share usage statistics in Settings. The preference defaults to false. The SDK is lazy and is not initialized before consent; debug and benchmark variants never enable the controller. Disabling the setting stops new capture and opts the SDK out. Previously received reports are not deleted by the switch.

Events contain no project IDs, names, notes, pattern URLs, PDFs, photos, search text, counter values, billing state, or account identity. Screen names come from an explicit allowlist. The SDK supplies pseudonymous installation/session identifiers and app/device context; events set `$geoip_disable` to prevent IP geolocation enrichment. Session replay, automatic screen/deep-link/lifecycle capture, push capture, error capture, person profiles, and feature flags are disabled. The SDK may fetch its remote configuration after consent; optional capture features remain disabled locally. No PostHog logger is called.

The public project token is stored in ignored local configuration or an environment variable and is embedded in the app as required by the SDK. Personal and project-secret API keys must never be embedded. This is a narrow exception to the previous analytics prohibition. See `config/posthog.md` for the event contract and verification limits.
