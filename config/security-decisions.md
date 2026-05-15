# Security Decisions

## Ravelry embedded credentials

Status: Accepted risk

Decision date: 2026-05-15

KnitTools has no backend service and will not add one for Ravelry access. The Android app therefore keeps the existing client-only integration: release builds may embed Ravelry Basic Auth credentials and an OAuth client secret in `BuildConfig` after an explicit `KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS=true` opt-in.

Mitigations:

- OAuth authorization requests include PKCE (`S256`) when Ravelry accepts it.
- Release builds fail unless all Ravelry values are present and the explicit embedded-secret opt-in is set.
- Credentials must come from environment variables or ignored `debug.credentials.properties`; they must not be committed or logged.
- Ravelry credential extraction from the APK remains possible and is accepted as the tradeoff for a no-backend architecture.
