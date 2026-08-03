# Ravelry Firebase Backend And Saved Patterns Plan

## Summary
- Scope: implement the backend/data/auth plan, with UI integration points aligned to the local `codex_ravelry_ui_plan.md`.
- Current audited state: KnitTools uses Ravelry OAuth2 Authorization Code + PKCE, but Android exchanges tokens itself, stores tokens in `EncryptedSharedPreferences`, and embeds Ravelry OAuth2/Basic Auth credentials through `BuildConfig`.
- Target state: Android contains no Ravelry secrets or Ravelry tokens. Firebase Auth anonymous UID + Cloud Functions v2 own auth, token exchange, Ravelry API calls, status, disconnect, search, and URL import.
- Defaults: OAuth2 branch, Firebase Functions v2 TypeScript, region `europe-west1`, Firebase BoM `34.14.0`, Google Services plugin `4.5.0`, AndroidX Browser `1.10.0`.

## Implementation Plan
1. **Audit and Progress Baseline**
   - Create `config/ravelry-backend-progress.md` with the required audit table: auth type, secret surfaces, token exchange files, saved pattern writers, project attachment writers, browser/WebView status, behavior to preserve.
   - Mark old no-backend docs as superseded by this new Firebase backend decision, but do not remove behavior in this phase.
   - Preserve dirty worktree changes and avoid touching unrelated counter/reading-line files.

2. **Firebase Backend Skeleton**
   - Add `firebase.json`, `firestore.rules`, and `functions/` TypeScript project.
   - Use `firebase-functions@7.2.5`, `firebase-admin@14.0.0`, `typescript@6.0.3`, `firebase-functions-test@3.5.0`.
   - Create `functions/src/config.ts`, `index.ts`, and `ravelry/` modules for auth, token storage, URL parsing, client calls, pattern import, and sanitized types.
   - Firestore rules deny all client reads/writes to `ravelryOAuthStates/{state}` and `ravelryTokens/{uid}`.

3. **Backend OAuth2 Flow**
   - Implement `ravelryStartAuth`, `ravelryCallback`, `ravelryAuthStatus`, `ravelryDisconnect`, and `ravelryCurrentUser`.
   - Bind `RAVELRY_CLIENT_ID` and `RAVELRY_CLIENT_SECRET` with `defineSecret`; do not expose them through callable responses, logs, redirects, tests, or fixtures.
   - Store state with `uid`, expiry, `usedAt`, `authType="oauth2"`, redirect metadata, and PKCE/server exchange metadata if required.
   - Callback validates `state`, expiry, unused state, and required params, exchanges the code server-side, stores tokens under `ravelryTokens/{uid}`, then redirects to `knittools://ravelry-auth-complete?state=STATE`.

4. **Backend Search And Import**
   - Implement `ravelrySearchPatterns`, `ravelryImportPatternByUrl`, and `ravelryImportPatternById`.
   - Return only sanitized pattern fields: Ravelry ID, title, designer, thumbnail, canonical URL, original URL, free/paid/unknown availability, and pagination.
   - Do not download paid/private PDFs in v1. Treat Ravelry as metadata/link import unless the API explicitly proves a safe user-owned PDF path.

5. **Android Firebase Integration**
   - Add Firebase BoM, `firebase-auth`, `firebase-functions`, and Google Services plugin in Gradle; keep `app/google-services.json` ignored.
   - Add CI/local setup gate: Android Firebase phase requires a real local `app/google-services.json` or a CI-generated one from a GitHub secret.
   - Add `RavelryBackendClient` and `FirebaseAnonymousAuthGateway` under existing repository/data boundaries.
   - Remove Ravelry credential `BuildConfig` fields, release env gates, and Ravelry token storage from Android.

6. **Android Auth And UI State**
   - Replace `RavelryAuthManager` token ownership with backend auth/session status ownership.
   - Sign in anonymously before callable calls; open returned `authorizeUrl` with Auth Tab when available, Custom Tabs fallback otherwise.
   - Update `MainActivity` to handle only `knittools://ravelry-auth-complete?state=...`, clear OAuth/counter extras, then refresh backend auth status.
   - UI keeps Tools > Ravelry visible, with clear not-connected, connected-as-username, cancelled, expired, backend-unavailable, and disconnect states.

7. **Saved Pattern Schema Migration**
   - Move Room to schema `14`.
   - Replace Ravelry-centric saved pattern shape with internal source metadata: `source`, nullable `ravelryPatternId`, `originalUrl`, `canonicalUrl`, nullable `localPdfUri`, `isAvailableOffline`, `updatedAt`, nullable `lastSyncedAt`; preserve existing `id` values.
   - Backfill: `ravelryId > 0` -> `RAVELRY`; `content://` or `file://` pattern URL -> `LOCAL_FILE`; unknown rows -> `OTHER`.
   - Update repository duplicate detection in order: Ravelry ID, canonical URL, normalized original URL, then title+designer only with confirmation.

8. **Import And Saved Patterns UX Boundaries**
   - Imported Ravelry patterns save into the normal Saved Patterns list; do not create visible source-based libraries.
   - Add URL/share import route that validates `text/plain` Ravelry URLs, calls backend import, shows confirmation, and never auto-saves visited pages.
   - Saved pattern opening rules: local PDF opens viewer, metadata-only Ravelry pattern opens detail or Ravelry page, project attachment uses existing `CounterRepository.attachPattern` for local PDFs and `linkedPatternId` for metadata.

9. **Security And Documentation Hardening**
   - Update `AGENTS.md`, `CODEX.md`, `PROJECT.md`, `memory/MEMORY.md`, `config/security-decisions.md`, Semgrep rules, DeepSec accepted-risk marker, and `tools/release-surface.ps1`.
   - `release-surface` should allow only Firebase Auth/Functions/Google Services for this backend, while still forbidding Firebase AI, ML Kit, Gemini, voice/speech, release Sentry, and broad FileProvider paths.
   - Add a secret-surface scan that checks source, resources, BuildConfig/generated constants, Gradle files, manifests, tests, APK, and AAB against local/env-provided known Ravelry secret values without printing them.

## Test Plan
- Backend: `npm --prefix functions test`, `npm --prefix functions run build`; cover expired state, invalid state, used state, missing callback params, disconnect, token-store isolation, URL parsing, import-by-ID/URL, sanitized search response.
- Android unit/source tests: auth client mapping, ViewModel status/error states, duplicate detection, Room migration 13->14, saved pattern route targets, share URL validation, no WebView, no `BuildConfig.RAVELRY_*`.
- Verification commands: `.\gradlew.bat --no-configuration-cache :app:testDebugUnitTest --rerun-tasks`, `.\gradlew.bat --no-configuration-cache :app:assembleDebug :app:lintDebug :app:ktlintCheck :app:detekt`, `.\tools\release-surface-test.ps1`.
- Manual QA: new install, sign in, restart persistence, disconnect, search, save, URL share import, duplicate import opens existing item, attach to project, open local PDF vs Ravelry link, offline availability copy, no token/secret logs, release artifact secret scan.

## Assumptions And Sources
- Human setup still supplies Firebase project ID, real `google-services.json`, Ravelry callback URL registration, and Secret Manager values; Codex never needs secret values.
- Ravelry public docs are limited, so endpoint/auth details must be confirmed against the current working Android implementation and the developer console during audit.
- Official references used: [Firebase Android setup](https://firebase.google.com/docs/android/setup), [Firebase callable functions](https://firebase.google.com/docs/functions/callable), [Firebase secret parameters](https://firebase.google.com/docs/functions/config-env), [Firebase anonymous auth](https://firebase.google.com/docs/auth/android/anonymous-auth), [AndroidX Browser releases](https://developer.android.com/jetpack/androidx/releases/browser), [Chrome Auth Tab](https://developer.chrome.com/docs/android/custom-tabs/guide-auth-tab), [Custom Tabs interactivity](https://developer.chrome.com/docs/android/custom-tabs/guide-interactivity), [Android share receive](https://developer.android.com/training/sharing/receive), [Ravelry Goodies/API entry](https://www.ravelry.com/about/goodies).
