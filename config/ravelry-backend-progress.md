# Ravelry Firebase backend progress

## 2026-06-11

Branch: `codex/project-workspace-cards`

Started phase: Phase 1 - Audit and Progress Baseline
Completed phase: Phase 1 - Audit and Progress Baseline

## Scope

- Read `Ravelry Firebase Backend And Saved Patterns Plan.md`, `codex_ravelry_data_backend_plan.md`, and `codex_ravelry_ui_plan.md`.
- Checked current official docs for Firebase Android setup, Firebase Functions TypeScript, callable functions, Secret Manager parameters, AndroidX Browser/Auth Tab, Android share receive, and Ravelry's public API entry before changing project files.
- Created this progress file as the restart point for the phase-by-phase backend migration.
- Marked the old no-backend Ravelry decision as superseded in project docs, while preserving all current Android behavior.

## Current audit table

| Required area | Current source of truth | Phase 1 finding | Behavior to preserve |
| --- | --- | --- | --- |
| Auth type | `app/src/main/java/com/finnvek/knittools/auth/RavelryAuthManager.kt`; `app/src/main/java/com/finnvek/knittools/data/remote/RavelryApiService.kt` | Current auth is OAuth2 Authorization Code with PKCE in Android, plus Basic Auth fallback for API calls. No OAuth 1.0a implementation was found in main source. | Ravelry sign-in remains available from Tools, search/detail continue to work until backend phases replace the owner. |
| Secret surfaces | `app/build.gradle.kts`; ignored `debug.credentials.properties`; release env names `KNITTOOLS_RAVELRY_*`; `BuildConfig.RAVELRY_*`; `config/security-decisions.md`; `tools/release-surface.ps1` | Android still exposes Ravelry Basic Auth credentials and OAuth2 client secret through BuildConfig when configured. This is now a legacy temporary state, not the target architecture. | Do not add new Android Ravelry secret surfaces. Existing release gates stay until the backend migration removes them in a later phase. |
| Token exchange files | `RavelryAuthManager.createOAuthUri`, `handleCallback`, `exchangeCodeForTokens`, `requestTokens`, `refreshAccessToken` | Android performs code exchange against `https://www.ravelry.com/oauth2/token`, builds a Basic header from `BuildConfig.RAVELRY_OAUTH2_CLIENT_ID` and `BuildConfig.RAVELRY_OAUTH2_CLIENT_SECRET`, and stores access/refresh tokens in `EncryptedSharedPreferences`. | Preserve login/logout user flow while moving exchange and token ownership server-side in later phases. |
| Saved pattern writers | `RavelryRepository.savePattern`; `SavedPatternRepository.saveRavelryPatternIfMissing`; `SavedPatternRepository.saveImportedPatternIfMissing`; `SavedPatternDao.insert`; `SavedPatternEntity`; `EntityMappers.kt` | Saved patterns are currently Ravelry-centric with `ravelryId`, title/designer metadata, optional thumbnail and `patternUrl`; local imported PDFs use `ravelryId = 0` and `content://` or `file://` URLs. | Existing saved pattern IDs, local PDF imports, and visible `Saved Patterns` library must keep working. |
| Project attachment writers | `CounterRepository.attachPattern` / `detachPattern`; `CounterProjectDao.updatePattern`; `RavelryRepository.createProjectFromPattern`; `CounterViewModel.attachPattern` | Local PDF attachment writes saved-pattern row, annotations, `patternUri`, and project pattern state through repository transactions. Ravelry "Start Project" creates a saved pattern and sets `linkedPatternId`. | Project attachments must remain atomic; local PDF viewer still requires `patternUri`, while metadata-only Ravelry links stay metadata until schema/UX phases change them. |
| Browser / WebView status | `RavelryAuthManager.startOAuthFlow`; `RavelrySearchScreen.kt`; `RavelryDetailScreen.kt`; `app/src/main/AndroidManifest.xml` | Sign-in and Ravelry page opening use `CustomTabsIntent`. No `android.webkit.WebView` usage was found in app main source. Current callback is `com.finnvek.knittools://oauth/callback`. | Keep browser-based auth and page opening. Later backend callback should become `knittools://ravelry-auth-complete?state=STATE` without putting tokens in the deep link. |
| Behavior to preserve | `TopLevelDestination`, `Screen.Ravelry`, `RavelryViewModel`, `RavelrySearchScreen`, `SavedPatternsScreen`, `SavedPatternRouteTarget` | Tools > Ravelry is visible; Search and Saved Patterns remain the user-facing split; Ravelry links open detail routes while local/imported patterns open the local viewer. | Do not split visible saved libraries by source. Imported Ravelry patterns should appear in normal Saved Patterns. |

## Superseded docs

- `AGENTS.md` and `CODEX.md` no longer treat backendless Ravelry as the target architecture; they now point to the Firebase backend plan and this progress file.
- `CLAUDE.md`, `README.md`, `PROJECT.md`, `memory/MEMORY.md`, and `config/security-decisions.md` now identify the client-secret/Basic-Auth Android path as a temporary legacy state until the backend migration removes it.
- No production code, Gradle dependencies, Room schema, Firebase files, or generated artifacts were changed in this phase.

## Next phase

Phase 2 is not started.

Expected next step after the user says `jatka`: create the Firebase backend skeleton (`firebase.json`, `firestore.rules`, and the `functions/` TypeScript project) without touching Android behavior beyond what Phase 2 explicitly requires.

## Verification

- `rg` confirmed current Ravelry auth, token exchange, saved pattern, project attachment, Custom Tabs, callback, BuildConfig, and Firebase/no-backend references.
- `git status --porcelain=v1` was empty before Phase 1 edits.
- No Gradle or npm tests were run because this phase changed documentation only.

## 2026-06-11

Branch: `codex/project-workspace-cards`

Started phase: Phase 2 - Firebase Backend Skeleton
Completed phase: Phase 2 - Firebase Backend Skeleton

## Changes

- Added root `firebase.json` for Firebase Functions v2 deployment from `functions/` with runtime `nodejs22`.
- Added root `firestore.rules`.
- Added `functions/` TypeScript project with:
  - `functions/package.json`
  - `functions/package-lock.json`
  - `functions/tsconfig.json`
  - `functions/src/config.ts`
  - `functions/src/index.ts`
  - `functions/src/ravelry/auth.ts`
  - `functions/src/ravelry/tokenStore.ts`
  - `functions/src/ravelry/urlParsing.ts`
  - `functions/src/ravelry/client.ts`
  - `functions/src/ravelry/patternImport.ts`
  - `functions/src/ravelry/sanitizedTypes.ts`
  - `functions/src/ravelry/skeleton.test.ts`
- `config.ts` owns `europe-west1`, `ravelryOAuthStates`, `ravelryTokens`, and Secret Manager bindings for `RAVELRY_CLIENT_ID` / `RAVELRY_CLIENT_SECRET`.
- `auth.ts` exports placeholder Functions v2 endpoints for `ravelryStartAuth`, `ravelryCallback`, `ravelryAuthStatus`, `ravelryDisconnect`, and `ravelryCurrentUser`.
- Firestore rules deny all client reads/writes to `ravelryOAuthStates/{state}` and `ravelryTokens/{uid}`.
- `.gitignore` now ignores generated Functions output, local secrets, debug logs, and `functions/node_modules`.
- Updated `AGENTS.md`, `CODEX.md`, `PROJECT.md`, and `memory/MEMORY.md` for the new backend skeleton.

## Version notes

- Verified npm latest versions before implementation: `firebase-functions@7.2.5`, `firebase-admin@14.0.0`, `typescript@6.0.3`, `firebase-functions-test@3.5.0`, and `@types/node@25.9.3`.
- `firebase-functions@7.2.5` declares `firebase-admin` peer dependency `^11.10.0 || ^12.0.0 || ^13.0.0`; npm refused the planned `firebase-admin@14.0.0` pairing.
- To keep the dependency tree valid without `--legacy-peer-deps`, Phase 2 uses `firebase-admin@13.10.0`, the latest 13.x compatible version found from npm.
- Firebase's current Functions runtime docs list Node.js 22, 20, and deprecated 18. Phase 2 uses `nodejs22` even though the local shell currently runs Node 24.

## TDD and verification

- RED: `npm --prefix functions test` first failed on missing Phase 2 modules after the Node test types were enabled.
- GREEN: `npm --prefix functions test` completed with 1 passing Node test after adding the skeleton modules.
- `npm --prefix functions run build` completed successfully after documentation updates.
- `node -e "const fs=require('fs'); for (const f of ['firebase.json','functions/package.json','functions/tsconfig.json']) JSON.parse(fs.readFileSync(f,'utf8')); console.log('json ok')"` confirmed the JSON files parse.
- `npm --prefix functions audit --audit-level=high` returned exit 0; the remaining audit output is the 9 moderate findings noted above.
- `git diff --check` returned exit 0 with only CRLF/LF normalization warnings on existing text files.
- `npm --prefix functions install` succeeded with the compatible dependency set and generated `functions/package-lock.json`.
- npm reported 9 moderate audit findings in the dependency tree. No `npm audit fix` was run because this phase pins the Firebase skeleton dependency set and audit remediation belongs to a later security hardening pass.

## Not done yet

- Phase 3 was not started.
- No real OAuth state generation, callback validation, code exchange, token storage, Ravelry API call, search, import, Android Firebase dependency, or Android auth/UI behavior was implemented.

## 2026-06-11

Branch: `codex/project-workspace-cards`

Started phase: Phase 3 - Backend OAuth2 Flow
Completed phase: Phase 3 - Backend OAuth2 Flow

## Changes

- Implemented backend-owned OAuth2 auth flow in `functions/src/ravelry/`:
  - `ravelryStartAuth`
  - `ravelryCallback`
  - `ravelryAuthStatus`
  - `ravelryDisconnect`
  - `ravelryCurrentUser`
- Added `authCore.ts` for testable auth flow behavior.
- Added `oauthStateStore.ts` for Firestore-backed `ravelryOAuthStates/{state}` PKCE state storage.
- Added `oauth2.ts` for server-side authorization-code token exchange against Ravelry.
- Replaced the placeholder token store with Firestore-backed `ravelryTokens/{uid}` storage.
- Added current-user Ravelry API support in `client.ts`.
- Kept Android unchanged; it does not call the backend until the later auth integration phase.

## Security and behavior notes

- `RAVELRY_CLIENT_ID` and `RAVELRY_CLIENT_SECRET` remain bound through `defineSecret`.
- The OAuth client secret, access token, refresh token, and PKCE code verifier are not returned by callable responses or app redirects.
- `ravelryStartAuth` returns an `authorizeUrl` for the later Android auth phase. Per OAuth2 requirements this URL includes a `client_id` query parameter; the actual client secret and verifier stay server-side.
- Callback handling validates missing, invalid, expired, and already-used state before exchange.
- Successful callbacks store tokens under `ravelryTokens/{uid}` and redirect only to `knittools://ravelry-auth-complete?state=STATE`.
- State consumption is checked before token exchange; the Firestore state store marks `usedAtMillis` inside a transaction so replay-race losers do not exchange a second token.
- Firestore token writes prune undefined optional fields before `set()` so minimal token responses do not fail at runtime.

## TDD and verification

- RED: `npm --prefix functions test` first failed on missing Phase 3 modules after adding `authCore.test.ts`.
- GREEN: `npm --prefix functions test` passed after adding auth core, stores, token exchange, current-user client, and Firebase function wrappers.
- Regression RED: `npm --prefix functions test` failed when optional token fields were persisted as `undefined`.
- Regression GREEN: `npm --prefix functions test` passed after pruning undefined token fields in core storage and the Firestore token store.
- Replay RED: `npm --prefix functions test` failed after the state-store contract was changed to report failed state consumption.
- Replay GREEN: `npm --prefix functions test` passed after `oauthStateStore.ts` marked state usage transactionally and `authCore.ts` rejected failed consumption before token exchange.

## Not done yet

- Phase 4 was not started.
- No backend search/import implementation was added.
- No Android Firebase dependency, anonymous auth gateway, backend client, legacy secret removal, UI state replacement, Room migration, or saved-pattern schema change was implemented.

## 2026-06-11

Branch: `codex/project-workspace-cards`

Started phase: Phase 4 - Backend Search And Import
Completed phase: Phase 4 - Backend Search And Import

## Changes

- Implemented backend metadata search/import in `functions/src/ravelry/`:
  - `ravelrySearchPatterns`
  - `ravelryImportPatternById`
  - `ravelryImportPatternByUrl`
- Added `callable.ts` as the shared callable auth/error adapter for Ravelry backend functions.
- Implemented `client.ts` Ravelry calls for:
  - `GET https://api.ravelry.com/patterns/search.json`
  - `GET https://api.ravelry.com/patterns/{id}.json`
- Implemented `urlParsing.ts` for `https://www.ravelry.com/patterns/library/{slug}` URL normalization and non-Ravelry rejection.
- Expanded `sanitizedTypes.ts` with sanitized search response and pagination types.
- Kept Android unchanged; it does not call the backend until the later auth integration phase.

## Security and behavior notes

- Search/import require a Firebase UID plus an existing backend `ravelryTokens/{uid}` access token.
- Returned pattern data is limited to Ravelry ID, title, designer, thumbnail, canonical URL, original URL, availability, and pagination.
- Raw Ravelry responses are not returned to Android.
- Search/import do not download or return paid/private PDF URLs. Ravelry remains metadata/link import in this phase.
- URL import resolves direct numeric pattern URLs by ID; slug URLs search by slug, match canonical URLs where possible, then fetch detail by ID for sanitized metadata.
- Ravelry non-2xx responses keep `ravelry_http_{status}` error codes and HTTP status values for callable error mapping.

## TDD and verification

- RED: `npm --prefix functions test` failed after adding `patternImport.test.ts` because `searchPatternsForUser`, the new client signature, and option-object import functions did not exist yet.
- GREEN: `npm --prefix functions test` passed after implementing URL parsing, sanitized Ravelry client search/detail calls, metadata import by ID/URL, and callable exports.
- Error-mapping RED: `npm --prefix functions test` failed when Ravelry HTTP 429 surfaced only as a generic error.
- Error-mapping GREEN: `npm --prefix functions test` passed after adding `RavelryClientHttpError` with `ravelry_http_{status}` code and `httpStatus`.

## Not done yet

- Phase 5 was not started.
- No Android Firebase dependency, anonymous auth gateway, backend client, legacy secret removal, UI state replacement, Room migration, saved-pattern schema change, share route, or saved-pattern UX change was implemented.

## 2026-06-11

Branch: `codex/project-workspace-cards`

Started phase: Phase 5 - Android Firebase Integration
Completed phase: Phase 5 - Android Firebase Integration

## Changes

- Added Android Firebase dependencies through the version catalog:
  - Firebase BoM `34.14.0`
  - `firebase-auth`
  - `firebase-functions`
  - Google Services Gradle plugin `4.5.0`
- Added conditional Google Services plugin application in `app/build.gradle.kts` so unit tests and static Android lint can still resolve without a local Firebase JSON, while release build artifact tasks require `app/google-services.json` and debug artifact tasks may generate an ignored placeholder.
- Added `verifyGoogleServicesJson`, which fails release artifact tasks unless the ignored local `app/google-services.json` exists.
- Added CI setup in `.github/workflows/build.yml` and `.github/workflows/codeql.yml` to generate `app/google-services.json` from `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64`.
- Added `FirebaseAnonymousAuthGateway` and a small Firebase `Task.await()` coroutine bridge.
- Added `RavelryBackendClient`, `FirebaseRavelryBackendClient`, and `RavelryBackendMappers`.
- Added `FirebaseModule` to provide `FirebaseAuth`, region-pinned `FirebaseFunctions.getInstance("europe-west1")`, and the backend client binding.
- Changed `RavelryApiService` into a repository-compatible backend delegator for search/detail.
- Removed Android-owned Ravelry credential `BuildConfig` fields, release Ravelry env gates, embedded-secret opt-in handling, Basic Auth fallback, and `security-crypto`.
- Replaced `RavelryAuthManager` with a temporary no-secret/no-token compatibility shell until Phase 6 owns UI auth state and callback flow.
- Updated project docs and security decision docs to mark Android Ravelry secrets and token storage as removed.

## Security and behavior notes

- Android no longer stores Ravelry access/refresh tokens.
- Android no longer embeds Ravelry OAuth client secret or Basic Auth credentials.
- Backend callable search/import still returns only sanitized metadata and no pattern PDF URLs.
- `RavelryAuthManager` intentionally remains a temporary compatibility surface for existing UI/MainActivity callers; it does not own tokens or secrets.
- Android Firebase config remains uncommitted: release builds use ignored `app/google-services.json`, CI uses `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64`, and local debug builds may use ignored `app/src/debug/google-services.json` placeholder config.

## Version notes

- Official Firebase Android release notes list BoM `34.14.0` as the current BoM update for this plan step.
- Official Google Services Gradle Plugin docs show `com.google.gms:google-services:4.5.0`.
- Firebase Android setup docs now direct Kotlin apps to depend on the main Firebase modules instead of the removed KTX modules in BoM 34.x, so Phase 5 uses `firebase-auth` and `firebase-functions`, not KTX artifacts.

## TDD and verification

- RED: Added Android source/unit tests for Firebase dependency declarations, Google Services setup gates, CI JSON generation, backend client seams, Ravelry secret removal, backend response mapping, and `RavelryApiService` delegation.
- First targeted Gradle test run was blocked before the new assertions by an unrelated dirty-worktree asset deletion: `ProUpgradeScreen.kt` references missing `R.drawable.pro_upgrade` after `app/src/main/res/drawable-nodpi/pro_upgrade.webp` was deleted outside this phase.
- Dependency verification was updated with:
  - `.\gradlew.bat --write-verification-metadata sha256 --no-configuration-cache :app:tasks --all`
  - `.\gradlew.bat --write-verification-metadata sha256 --no-configuration-cache :app:dependencies --configuration debugRuntimeClasspath`
- GREEN: `npm --prefix functions test` passed with 12 backend tests after Phase 5 Android/doc updates.
- GREEN: `.\gradlew.bat --no-configuration-cache :app:tasks --all` resolved the Android Gradle task graph and showed `verifyGoogleServicesJson`.
- GREEN: `.\gradlew.bat --no-configuration-cache :app:ktlintMainSourceSetCheck :app:ktlintTestSourceSetCheck` passed after formatting the new Android backend client and tests.
- GREEN: `git diff --check` returned exit 0; Git only printed existing CRLF/LF normalization warnings.
- BLOCKED: the targeted Android unit test command still stops at `:app:compileDebugKotlin` because `ProUpgradeScreen.kt:138` references missing `R.drawable.pro_upgrade`; the corresponding `app/src/main/res/drawable-nodpi/pro_upgrade.webp` deletion is unrelated to this Ravelry phase and was left untouched.
- `rg` confirmed no Android main-source or app-build references remain for `RAVELRY_BASIC_AUTH`, `RAVELRY_OAUTH2`, `KNITTOOLS_ALLOW_EMBEDDED_RAVELRY`, `KNITTOOLS_RAVELRY`, `EncryptedSharedPreferences`, `MasterKey`, `KEY_ACCESS_TOKEN`, `KEY_REFRESH_TOKEN`, `security.crypto`, `releaseEnvOrEmpty`, `quotedBuildConfigValue`, `embeddedRavelry`, or `releaseRavelry`.

## Not done yet

- At the end of Phase 5, Phase 6 had not been started yet.
- At the end of Phase 5, Tools > Ravelry UI auth state, backend-owned OAuth start/status/disconnect UX, new callback route, Room schema 14, saved-pattern schema migration, URL/share import route, and release-surface hardening were still later phases.

## 2026-06-11

Branch: `codex/project-workspace-cards`

Started phase: Phase 6 - Android Auth And UI State
Completed phase: Phase 6 - Android Auth And UI State

## Changes

- Replaced the temporary Android `RavelryAuthManager` compatibility shell with backend-owned session state:
  - `RavelryAuthState.NotConnected`
  - `RavelryAuthState.Starting`
  - `RavelryAuthState.AwaitingBrowser`
  - `RavelryAuthState.Connected(username)`
  - `RavelryAuthState.Cancelled`
  - `RavelryAuthState.Expired`
  - `RavelryAuthState.BackendUnavailable`
  - `RavelryAuthState.Disconnecting`
- `RavelryAuthManager` now delegates to `RavelryBackendClient.startAuth`, `authStatus`, and `disconnect`; Android no longer creates an OAuth URI or owns token refresh/sign-out state.
- Updated the Android callback route to `knittools://ravelry-auth-complete`; callback handling refreshes backend auth status and never receives tokens.
- Added browser cancellation handling so Auth Tab cancellation leaves the UI in an explicit cancelled state instead of a pending state.
- Updated `MainActivity` to:
  - handle only backend-owned Ravelry callback intents before counter launch routing
  - clear counter extras for OAuth callback intents
  - open Ravelry auth with AndroidX Browser Auth Tab
  - fall back to Custom Tabs if the Auth Tab activity cannot be started
- Updated AndroidX Browser from `1.8.0` to `1.9.0`, the first stable release with `AuthTabIntent`, and refreshed Gradle dependency verification metadata for the new artifact.
- Updated `KnitToolsNavHost` to pass a single Ravelry auth launcher into Tools and Library Ravelry routes.
- Updated Tools > Ravelry search/detail UI to collect `authState`, launch backend sign-in requests, refresh backend auth status, disconnect through the backend manager, and keep the Ravelry surface visible.
- Added state-aware Ravelry sign-in UI strings for not connected, connected, connected-as-username, cancelled, expired, backend unavailable, disconnect, connecting, pending, and disconnecting.
- Added/updated Android source and unit tests for backend auth state, callback route, Auth Tab launch, backend delegation, cancellation, sign-in launch events, and disconnect delegation.
- Updated `AGENTS.md`, `CODEX.md`, `CLAUDE.md`, `PROJECT.md`, `config/security-decisions.md`, and `memory/MEMORY.md` with the new Android auth ownership.

## Security and behavior notes

- Android still does not store Ravelry access or refresh tokens.
- The Android callback route is token-free and limited to `knittools://ravelry-auth-complete?state=...`.
- Ravelry search/detail remain visible even when the backend auth state is not connected.
- Auth Tab is used for auth launch on AndroidX Browser `1.9.0`; Custom Tabs remains the fallback path.
- Library-only and project PDF behavior were not changed in this phase.

## Version notes

- Official AndroidX Browser release notes list `androidx.browser:browser:1.9.0` as released on 2025-07-30.
- AndroidX `AuthTabIntent` API reference marks the class and custom-scheme launch API as added in `1.9.0`.
- Chrome's Auth Tab guidance recommends Auth Tab for authentication and keeping Custom Tabs as fallback for unsupported devices.

## TDD and verification

- RED: Added Android source/unit tests for the backend-owned callback route, UI auth state replacement, backend auth manager delegation, sign-in launch event, and disconnect flow. The first targeted Gradle run was still blocked by the unrelated missing `R.drawable.pro_upgrade` asset before tests could execute.
- GREEN: `.\gradlew.bat --write-verification-metadata sha256 --no-configuration-cache :app:dependencies --configuration debugRuntimeClasspath` resolved the upgraded `androidx.browser:browser:1.9.0` artifact and updated dependency verification metadata.
- GREEN: `.\gradlew.bat --no-configuration-cache :app:ktlintMainSourceSetCheck :app:ktlintTestSourceSetCheck` passed after formatting the new auth/navigation/test code.
- GREEN: `npm --prefix functions test` passed with 12 backend tests.
- GREEN: `git diff --check` returned exit 0; Git only printed existing CRLF/LF normalization warnings.
- `rg` confirmed active Android main source no longer uses the legacy Ravelry OAuth URI creation/authenticated boolean/token callback signatures; remaining matches are negative source-test assertions.
- BLOCKED: the targeted Android unit test command still stops at `:app:compileDebugKotlin` because `ProUpgradeScreen.kt:138` references missing `R.drawable.pro_upgrade`; the corresponding `app/src/main/res/drawable-nodpi/pro_upgrade.webp` deletion is unrelated to this Ravelry phase and was left untouched.

## Not done yet

- At the end of Phase 6, Phase 7 had not been started.
- Room schema 14, saved-pattern schema migration, URL/share import route, saved-pattern UX changes, and release-surface hardening remain later phases.

## 2026-06-11

Branch: `codex/project-workspace-cards`

Started phase: Phase 7 - Saved Pattern Schema Migration
Completed phase: Phase 7 - Saved Pattern Schema Migration

## Changes

- Moved Room from schema 13 to schema 14 and exported `app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/14.json`.
- Added `KnitToolsDatabase.MIGRATION_13_14` and registered it in `DatabaseModule`.
- Rebuilt `saved_patterns` during migration with source metadata:
  - `source`
  - nullable `ravelryPatternId`
  - `originalUrl`
  - `canonicalUrl`
  - nullable `localPdfUri`
  - `isAvailableOffline`
  - `updatedAt`
  - nullable `lastSyncedAt`
- Preserved existing saved-pattern `id` values during the table rebuild.
- Backfilled legacy rows:
  - `ravelryId > 0` -> `RAVELRY`
  - `content://` or `file://` legacy `patternUrl` -> `LOCAL_FILE`
  - other legacy rows -> `OTHER`
- Added `SavedPatternSource` to the domain model and updated entity/domain mappers.
- Updated Ravelry saved-pattern writes to persist Ravelry metadata through `source = RAVELRY`, `ravelryPatternId`, `originalUrl`, and `canonicalUrl`.
- Updated imported local PDF saved-pattern writes to persist `source = LOCAL_FILE`, `localPdfUri`, and `isAvailableOffline = true`.
- Updated saved-pattern routing and project pattern attachment to use `localPdfUri` for local PDF flows and Ravelry source metadata for Ravelry detail routes.
- Added `SavedPatternRepository.findDuplicateCandidate` with duplicate detection order: Ravelry ID, canonical URL, normalized original URL, then title+designer only when explicitly requested.
- Updated Android unit/source tests, migration tests, `AGENTS.md`, `CODEX.md`, `CLAUDE.md`, `PROJECT.md`, and `memory/MEMORY.md` for schema 14.

## Behavior and compatibility notes

- Existing compatibility getters `SavedPattern.ravelryId` and `SavedPattern.patternUrl` remain computed from the new metadata while UI callers are migrated to source-specific fields.
- Local/imported PDFs remain attachable through the existing project PDF flow; Ravelry patterns remain metadata links until a local PDF is attached.
- No Pro upgrade image asset was restored. The missing `R.drawable.pro_upgrade` compile failure is still a separate pending asset issue.

## TDD and verification

- RED: Added `SavedPatternSchema14SourceTest`; the initial targeted unit test command was blocked before assertions by the known missing `R.drawable.pro_upgrade` asset.
- GREEN: `.\gradlew.bat --no-configuration-cache :app:kspDebugKotlin` passed and exported Room schema 14.
- GREEN: `.\gradlew.bat --no-configuration-cache :app:ktlintMainSourceSetCheck :app:ktlintTestSourceSetCheck` passed after formatting the new schema/repository/test code.
- BLOCKED: `.\gradlew.bat --no-configuration-cache :app:testDebugUnitTest --tests "com.finnvek.knittools.SavedPatternSchema14SourceTest"` still stops at `:app:compileDebugKotlin` because `ProUpgradeScreen.kt:138` references missing `R.drawable.pro_upgrade`; the corresponding `app/src/main/res/drawable-nodpi/pro_upgrade.webp` deletion was intentionally left untouched.
- `rg` confirmed active source no longer uses the old persisted saved-pattern constructor fields or DAO methods (`ravelryId =`, `patternUrl =`, `getByRavelryId`, `getByPatternUrl`).
- `git diff --check` returned no whitespace errors; Git only printed existing CRLF/LF normalization warnings.
- `rg` confirmed schema 14 export contains the new `saved_patterns` metadata fields.

## Not done yet

- At the end of Phase 7, URL/share import route, saved-pattern UX changes, and release-surface hardening were still later phases.

## 2026-06-12

Branch: `codex/project-workspace-cards`

Started phase: Phase 8 - Ravelry UI And Saved Patterns UX
Completed phase: Phase 8 - Ravelry UI And Saved Patterns UX

## Changes

- Phase 8 completed the Android Ravelry UI and saved-pattern UX path.
- Added one `RavelryImportConfirmationSheet` flow for search-result and shared URL imports with `Loading`, `Ready`, `AlreadySaved`, `NeedsSignIn`, `CouldNotImport`, and `BackendUnavailable` states.
- Added Android `ACTION_SEND text/plain` share import handling for validated Ravelry pattern URLs; consumed share intents do not launch counter navigation.
- Added connected-state Browse Ravelry through Custom Tabs with `SHARE_STATE_ON`; no WebView or custom toolbar save button was added.
- Added `SavedPatternDetailScreen` for title/designer/thumbnail, PDF/offline/Ravelry availability, Open Pattern / Open on Ravelry, Attach to Project, and Remove actions.
- Changed `PatternPickerSheet` to list all saved patterns and attach them through `CounterViewModel.attachSavedPattern`; Import from Ravelry navigates to Ravelry import/search, and SAF PDF attach stays separate.
- Changed project pattern-card routing so attached PDFs still open the PDF viewer, while metadata-only `linkedPatternId` links open `SavedPatternDetail`.
- Localized new Ravelry/Saved Patterns strings in all current `values-*` files without source-category headings such as "Saved from Ravelry".

## Behavior and compatibility notes

- Ravelry remains metadata-only until a local PDF is attached; backend and Android still do not download pattern PDFs.
- Existing `linkedPatternId` remains the metadata link between projects and saved patterns.
- `CounterRepository.attachSavedPattern` is the atomic writer for saved-pattern attachment; UI must not split local PDF state and linked saved-pattern state into separate writes.

## TDD and verification

- RED: `.\gradlew.bat :app:testDebugUnitTest --tests "com.finnvek.knittools.RavelryPhase8DocumentationSourceTest"` failed on missing Phase 8 documentation before this progress entry.
- GREEN: `.\gradlew.bat :app:testDebugUnitTest --tests "com.finnvek.knittools.RavelryPhase8DocumentationSourceTest" --tests "com.finnvek.knittools.RavelryLocalizationSourceTest"` passed after Phase 8 docs and progress handoff were completed.

## Not done yet

- At the end of Phase 8, release-surface hardening remained later work.

## 2026-06-12

Branch: `codex/project-workspace-cards`

Started phase: Phase 9 - Security And Documentation Hardening
Completed phase: Phase 9 - Security And Documentation Hardening

## Changes

- Updated `tools/release-surface.ps1` for the post-Firebase Ravelry backend contract:
  - Firebase Auth, Firebase Functions, and Google Services are allowed only for this backend.
  - Firebase AI, ML Kit, Gemini/Google Generative AI, and voice/speech dependencies remain forbidden.
  - `app/google-services.json` may exist locally only as an ignored/untracked file; a tracked file fails `firebase-boundary`.
  - Release signing and Firebase config gates are checked without reintroducing Android Ravelry credential gates.
- Added `known-ravelry-secrets` scanning for locally or environment-provided Ravelry secret values across source, resources, generated constants, Gradle files, manifests, tests, APKs, and AABs; findings redact the secret value.
- Updated `tools/release-surface-test.ps1` to cover the new baseline plus tracked google-services, ML Kit, and known-secret leak mutations.
- Updated `config/security-decisions.md`, Semgrep rules, DeepSec accepted-risk marker docs/reasoning, `AGENTS.md`, `CODEX.md`, `PROJECT.md`, and `memory/MEMORY.md` for the Phase 9 removed-risk contract.

## TDD and verification

- RED: `.\tools\release-surface-test.ps1` failed because the old release-surface still required removed Android Ravelry credential gates and rejected all Firebase/Google Services dependencies.
- GREEN: `.\tools\release-surface-test.ps1` passed with 11 selftests after the Firebase whitelist, tracked Google Services check, forbidden dependency rules, and known-secret scan were added.
- GREEN: `.\tools\release-surface.ps1` passed on the current workspace with 13 checks, 0 warnings, and 0 failures.

## Not done yet

- Superseded by the Final Verification Sweep below.

## 2026-06-12

Branch: `codex/project-workspace-cards`

Started phase: Final Verification Sweep
Completed phase: Final Verification Sweep

## Changes

- Ran the original backend/Ravelry verification sweep as far as the current local checkout allows.
- Updated stale source tests to match the schema 14 and Phase 8/9 architecture:
  - `ArchitectureSingleSourceSourceTest` now checks Room schema 14 and the current Ravelry detail label resources.
  - `RavelryDetailFlowSourceTest` now checks URL failure handling in the shared `RavelryExternalLinks` helper.
  - `RavelryPhase8DocumentationSourceTest` now matches the historical Phase 8 handoff text after Phase 9 completion.
  - `DaoQuerySourceTest` now checks the deterministic saved-pattern lookup SQL without relying on raw Kotlin string concatenation shape.
  - `RavelryAuthManagerTest` mocks Android `Uri` in JVM unit tests instead of calling unmocked Android framework parsing.
- Fixed final verification blockers found by static checks:
  - removed unused `CounterScreen.kt` imports,
  - formatted long `CounterWorkspaceSourceTest` assertions,
  - moved pure `RavelryViewModel` error/search helper functions out of the ViewModel class so detekt's `TooManyFunctions` rule passes.

## Documentation checked

- Firebase Android setup docs for `google-services.json`/Android Firebase configuration.
- Firebase callable Functions docs for Android callable client expectations.
- Firebase Functions environment configuration docs for backend config boundaries.
- Android lint docs for Gradle lint execution.
- Google Services Gradle plugin docs for config-file processing.

## Verification

- GREEN: `npm --prefix functions test` passed with 3 suites, 12 tests, 12 passed, 0 failed.
- GREEN: `npm --prefix functions run build` passed.
- RED/GREEN: targeted failing Android source/unit tests first failed on stale source assertions and Android `Uri` JVM parsing; after the narrow test fixes, the targeted command passed:
  `.\gradlew.bat --no-configuration-cache :app:testDebugUnitTest --tests "com.finnvek.knittools.ArchitectureSingleSourceSourceTest" --tests "com.finnvek.knittools.auth.RavelryAuthManagerTest" --tests "com.finnvek.knittools.data.local.DaoQuerySourceTest" --tests "com.finnvek.knittools.RavelryDetailFlowSourceTest" --tests "com.finnvek.knittools.RavelryPhase8DocumentationSourceTest"`.
- GREEN: final fresh `.\gradlew.bat --no-configuration-cache :app:testDebugUnitTest --rerun-tasks` passed after all verification-sweep edits.
- BLOCKED: `.\gradlew.bat --no-configuration-cache :app:assembleDebug :app:lintDebug :app:ktlintCheck :app:detekt` stopped at `:app:verifyGoogleServicesJson` because `app/google-services.json` does not exist locally.
- GREEN: `.\gradlew.bat --no-configuration-cache :app:lintDebug :app:ktlintCheck :app:detekt` passed when run separately from the artifact task blocked by the missing local Firebase config.
- GREEN: `.\tools\release-surface-test.ps1` passed with 11 selftests, 0 failed.
- GREEN: `.\tools\release-surface.ps1` passed with 13 checks, 0 warnings, 0 failures.
- GREEN: `git diff --check` on the verification-sweep touched files returned exit 0; Git printed only CRLF/LF normalization warnings.
- CHECKED: `Test-Path app\google-services.json` returned `False`.
- CHECKED: `rg "R\.drawable\.pro_upgrade|pro_upgrade" app/src/main/java app/src/main/res` found only the `Screen.ProUpgrade` route, so the old deleted `pro_upgrade` image is not a current compile blocker.

## Not done yet

- APK/AAB artifact verification remains blocked until the ignored local `app/google-services.json` is provided or generated by the local environment.

## 2026-06-12

Branch: `codex/project-workspace-cards`

Started phase: Artifact Verification Retry
Stopped phase: Artifact Verification Retry

## Documentation checked

- Firebase Android setup docs for the required Android Firebase app configuration file.
- Google Services Gradle Plugin docs for `google-services.json` processing.
- Android command-line build docs for Gradle artifact build execution.

## Verification

- CHECKED: `Test-Path app\google-services.json` returned `False`.
- CHECKED: `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64` and `GOOGLE_SERVICES_JSON_BASE64` are absent from the local environment; no secret value was printed.
- CHECKED: `git check-ignore -v app/google-services.json` confirmed `.gitignore` covers the local Firebase config path.
- BLOCKED: `.\gradlew.bat --no-configuration-cache :app:assembleDebug` stopped at `:app:verifyGoogleServicesJson` because `C:\Dev\KnitTools\app\google-services.json` does not exist.

## Not done yet

- APK/AAB artifact verification is still blocked until a real ignored local `app/google-services.json` is added, or the local environment provides `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64` so the file can be generated without committing secrets.
