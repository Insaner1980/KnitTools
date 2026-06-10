# Codex plan: Ravelry data, auth, backend, and import changes

Current date: 2026-06-10

This file is for Codex. It describes the product and engineering direction, but Codex should inspect the existing code and make implementation decisions that fit the current architecture.

## Product decision

Ravelry must ship in the first public release.

Ravelry must remain visible under the Tools tab. Do not bury Ravelry under Settings or Library as the main entry point.

The user-facing saved pattern list should remain simple. Do not create visible source-based libraries such as "Saved from Ravelry" or "Saved in KnitTools". If a pattern is saved or imported into the app, it belongs in the normal Saved Patterns library.

The pattern origin is useful internally, but it should not become the main user-facing category.

## Main goals

1. Remove all Ravelry secrets from the Android app.
2. Move Ravelry OAuth token exchange and secret signing to Firebase Cloud Functions.
3. Preserve the current user-facing Ravelry feature as much as possible.
4. Preserve saved patterns and project attachment behavior.
5. Keep Tools > Ravelry as the main place for Ravelry search, browsing, sign-in, and saved patterns.
6. Allow users to browse real Ravelry pages when useful.
7. Allow users to save or import patterns into the app's normal Saved Patterns library.
8. Keep the app honest about whether a pattern is available offline or only opens on Ravelry.

## Non-negotiable security requirements

- Android must not contain the Ravelry consumer secret, client secret, token secret, access token secret, or private API secret.
- Android must not perform OAuth token exchange that requires a confidential secret.
- Android must not sign Ravelry OAuth requests with a confidential Ravelry secret.
- Do not put Ravelry secrets in `BuildConfig`, resources, manifests, Gradle files, local properties copied into the app, generated constants, tests, or sample data.
- Do not log tokens, token secrets, verifiers, authorization codes, client secrets, consumer secrets, Firebase ID tokens, or raw authorization headers.
- Do not send OAuth secrets to Crashlytics logs or exception messages.
- Ravelry sign-in must use Custom Tabs or Auth Tab, not `android.webkit.WebView`.
- Firestore token documents must not be directly readable or writable by Android clients.
- The backend must validate state and expiry for OAuth callback handling.
- The backend must support disconnect and delete the user's stored Ravelry token data.

## First task: audit only

Before changing behavior, audit the current app.

Find every occurrence of:

- `ravelry`
- `api.ravelry.com`
- `ravelry.com/oauth`
- `oauth/request_token`
- `oauth/authorize`
- `oauth/access_token`
- `oauth2/auth`
- `oauth2/token`
- `consumerKey`
- `consumerSecret`
- `client_id`
- `client_secret`
- `access key`
- `secret key`
- token exchange code
- OAuth signing code
- current saved pattern models
- current Ravelry search code
- current Ravelry saved pattern code
- current project pattern attachment code

Report:

1. Whether the current Ravelry auth appears to use OAuth 1.0a, OAuth2, basic auth, or something else.
2. Which files contain secrets or secret-like values.
3. Which files perform token exchange or request signing.
4. Which files save Ravelry pattern data locally.
5. Which files attach patterns to projects.
6. Whether the current sign-in screen uses Custom Tabs, Auth Tab, external browser, or Android WebView.
7. Which existing behavior must be preserved.

Do not remove functionality during the audit step.

## Preferred backend architecture

Use Firebase Cloud Functions v2 with TypeScript.

Use Firebase Anonymous Auth on Android so callable functions receive a stable Firebase `uid` without requiring a visible KnitTools account.

Use Firebase Secret Manager for Ravelry app credentials.

Use Firestore for:

- temporary OAuth state records
- server-side Ravelry user token records
- optional import job/status records

Use a single shared region constant.

Suggested backend structure:

```text
functions/
  package.json
  tsconfig.json
  src/
    index.ts
    config.ts
    ravelry/
      auth.ts
      oauth1.ts
      oauth2.ts
      ravelryClient.ts
      tokenStore.ts
      patternImport.ts
      urlParsing.ts
      types.ts
```

Use only the OAuth helper that matches the audited Ravelry implementation.

## Firestore server-side schema

Suggested collections:

```text
ravelryOAuthStates/{state}
  uid: string
  requestToken: string | null
  requestTokenSecret: string | null
  codeVerifierHash: string | null
  redirectAfterAuth: string | null
  createdAt: timestamp
  expiresAt: timestamp
  usedAt: timestamp | null
  authType: "oauth1" | "oauth2"

ravelryTokens/{uid}
  authType: "oauth1" | "oauth2"
  accessToken: string
  accessTokenSecret: string | null
  refreshToken: string | null
  expiresAt: timestamp | null
  ravelryUserId: string | null
  ravelryUsername: string | null
  createdAt: timestamp
  updatedAt: timestamp
  lastVerifiedAt: timestamp | null
```

Client Firestore rules should deny direct client access to these collections. Cloud Functions should use the Admin SDK.

Consider encrypting stored token values before writing them to Firestore if the project already has a clean encryption pattern. Do not delay launch only to build a complicated encryption subsystem, but keep the token storage isolated and server-only.

## Functions to implement

### `ravelryStartAuth`

Type: callable function.

Requires Firebase Auth context.

Responsibilities:

- Create a cryptographically random `state`.
- Store `uid`, state metadata, expiry, and any temporary token secret needed for the OAuth flow.
- Start the Ravelry OAuth flow server-side.
- Return only non-secret data to Android.

Return example:

```json
{
  "authorizeUrl": "https://www.ravelry.com/...",
  "state": "opaque-state-value",
  "expiresAt": 1234567890
}
```

Do not return request token secrets, client secrets, consumer secrets, access tokens, or refresh tokens.

### `ravelryCallback`

Type: HTTPS request function.

Responsibilities:

- Receive the callback from Ravelry.
- Validate required callback parameters.
- Validate `state` exists, belongs to a known `uid`, is unused, and has not expired.
- Complete the token exchange server-side.
- Fetch current Ravelry user identity if the API supports it.
- Store the user's Ravelry tokens under `ravelryTokens/{uid}`.
- Mark the state as used.
- Redirect back to the app deep link.

Preferred app deep link:

```text
knittools://ravelry-auth-complete?state=STATE
```

Do not include access tokens or token secrets in the deep link.

### `ravelryAuthStatus`

Type: callable function.

Requires Firebase Auth context.

Return only non-sensitive state:

```json
{
  "connected": true,
  "username": "example",
  "lastVerifiedAt": 1234567890
}
```

If not connected:

```json
{
  "connected": false
}
```

### `ravelryDisconnect`

Type: callable function.

Requires Firebase Auth context.

Responsibilities:

- Delete `ravelryTokens/{uid}`.
- Delete or expire unused OAuth states for the uid.
- Return `{ "disconnected": true }`.

If Ravelry offers a token revocation endpoint for the active auth type, use it. If not, delete the server-side stored token and report the limitation in code comments.

### `ravelryCurrentUser`

Type: callable function.

Purpose: minimal test proxy.

Responsibilities:

- Require Firebase Auth.
- Read the stored token server-side.
- Call the Ravelry current user endpoint if available.
- Return sanitized JSON.

This function is for testing the connection and should never expose tokens.

### `ravelrySearchPatterns`

Type: callable function.

Implement if the app keeps native Ravelry search.

Responsibilities:

- Require Firebase Auth if Ravelry search requires a connected account.
- Proxy the search through the backend.
- Return a small sanitized result list.
- Avoid returning unnecessary fields.
- Add pagination if the current UI already supports it.

Suggested result fields:

```json
{
  "items": [
    {
      "ravelryPatternId": 123,
      "title": "Pattern title",
      "designerName": "Designer",
      "thumbnailUrl": "https://...",
      "patternUrl": "https://www.ravelry.com/patterns/library/...",
      "availability": "free | paid | unknown"
    }
  ],
  "nextPage": 2
}
```

### `ravelryImportPatternByUrl`

Type: callable function.

Responsibilities:

- Require Firebase Auth.
- Accept a Ravelry pattern URL.
- Canonicalize and validate the URL.
- Parse a pattern ID or slug if possible.
- Fetch pattern metadata from Ravelry through the backend.
- Return sanitized metadata to Android.

Do not download paid PDFs or private files unless the API clearly supports it for the authenticated user and the current app has a compliant storage model.

### `ravelryImportPatternById`

Type: callable function.

Responsibilities:

- Require Firebase Auth.
- Fetch pattern metadata by Ravelry pattern ID.
- Return sanitized metadata to Android.

### `ravelryImportLibrary`

Implement only if the current Ravelry API access supports reading the user's library or saved patterns reliably.

Do not use the word "sync" in the UI unless this function supports repeated imports, duplicate handling, update detection, and user control over changes.

For launch, manual import is acceptable.

## OAuth branch decisions

### If the current app uses OAuth 1.0a

Use a server-side OAuth 1.0a flow:

1. `ravelryStartAuth` gets request token server-side.
2. Store request token secret in Firestore state.
3. Android opens authorize URL.
4. `ravelryCallback` receives `oauth_token`, `oauth_verifier`, and `state`.
5. Backend exchanges request token and verifier for access token and access token secret.
6. Backend stores user token server-side.

### If the current app uses OAuth2

Prefer authorization code flow with server-side token exchange.

Use `state`. Use PKCE if Ravelry supports it for this app type. If a client secret is required for token exchange, keep it only in Secret Manager and only use it from Cloud Functions.

If the only available OAuth2 flow is implicit flow with `response_type=token`, do not silently implement a weaker Android-token design. Report the limitation and propose the safest available alternative.

## Android data model changes

Do not split the visible saved pattern library by source.

Internally, add source metadata so the app can avoid duplicates and open the original source when useful.

Suggested local model fields:

```text
id: local app id
source: RAVELRY | LOCAL_FILE | MANUAL | OTHER
ravelryPatternId: nullable long/string
originalUrl: nullable string
canonicalUrl: nullable string
title: string
designerName: nullable string
thumbnailUrl: nullable string
localPdfUri: nullable string
hasLocalUsableFile: boolean
isAvailableOffline: boolean
notes: nullable string
savedAt: timestamp
updatedAt: timestamp
lastSyncedAt: nullable timestamp
linkedProjectIds: if existing app model supports this
```

User-facing library name stays:

```text
Saved Patterns
```

Do not show `source` as the primary list category.

Show practical availability when useful:

- `Available offline`
- `PDF attached`
- `Open on Ravelry`
- `Requires Ravelry`

Use these only when they help the user. Do not clutter every card.

## Duplicate detection

When importing a Ravelry pattern, prevent duplicates.

Use this order:

1. Match by `ravelryPatternId` if available.
2. Match by canonical Ravelry URL.
3. Match by normalized original URL.
4. As a weak fallback, match by title and designer only if the user confirms.

If a duplicate exists, open the existing saved pattern instead of creating a new one.

## Android auth integration

Add or update dependencies according to the current Gradle setup:

- Firebase Authentication
- Firebase Functions
- AndroidX Browser
- App Check later if enabled

Use Firebase BoM if the project already uses Firebase BoM.

Flow:

1. Ensure Firebase anonymous auth.
2. Call `ravelryStartAuth`.
3. Open returned authorization URL with Auth Tab or Custom Tabs.
4. Handle `knittools://ravelry-auth-complete` deep link.
5. Call `ravelryAuthStatus`.
6. Update Ravelry screen state.

Do not parse or store Ravelry access tokens on Android.

## Ravelry web browsing and URL import

Ravelry can be used as the better browsing and discovery experience.

Add a visible `Browse Ravelry` action in Tools > Ravelry if it fits the existing screen.

Open real Ravelry pages in Custom Tabs.

If practical, add a Custom Tab menu item or action labeled:

```text
Save to KnitTools
```

This label is acceptable inside the Ravelry browsing context because it clarifies the destination. After import, the pattern appears under the normal Saved Patterns list.

Also add an Android share target for `text/plain` URLs so users can share a Ravelry pattern URL to KnitTools from any browser. The receiving screen should confirm before saving.

## Import confirmation behavior

When the app receives a Ravelry URL or search result import:

1. Show a confirmation screen or bottom sheet.
2. Display title, designer, thumbnail if available, and source URL.
3. Make the primary action `Save Pattern`.
4. If the pattern already exists, show `Already saved` and open the existing item.
5. Do not automatically save every visited Ravelry page.

## Existing data migration

Do not break users who already have saved patterns in the app.

If the Room database or other local storage schema changes:

- Add a proper migration.
- Preserve existing saved pattern IDs where possible.
- Backfill `source` as `RAVELRY` only when the existing item is clearly from Ravelry.
- Backfill `source` as `LOCAL_FILE` if it is a local PDF or local document.
- Backfill as `OTHER` or `UNKNOWN` if origin cannot be determined.

Do not expose uncertain origin in the UI.

## Release safety scan

Add a release safety script that scans source and build artifacts for known secret values.

The script should check at least:

- Android source files
- resources
- generated BuildConfig or generated constants
- Gradle files
- manifests
- tests
- release APK contents
- release AAB contents if available

Inputs should be local environment variables or a local ignored file containing the known secret values. Do not commit the secret values to the repo.

The build should fail if a known Ravelry secret appears in a release artifact.

## Testing requirements

Add tests or manual test scripts for:

- Ravelry URL parsing
- canonical URL creation
- duplicate detection
- import by URL
- import by ID if implemented
- auth status when not connected
- disconnect behavior
- expired OAuth state
- invalid OAuth state
- callback without required parameters
- backend unavailable error in Android UI
- cancelled sign-in
- saved pattern migration

Manual QA checklist:

1. New install, not connected.
2. Sign in with Ravelry.
3. Connection persists after app restart.
4. Disconnect works.
5. Search still works if it existed before.
6. Saved Patterns still opens.
7. Import by Ravelry URL works.
8. Sharing a Ravelry URL to KnitTools works.
9. Duplicate import opens existing pattern.
10. Pattern can be attached to a project.
11. Row counter can open the attached pattern or pattern detail.
12. Offline/local PDF state is accurate.
13. No tokens appear in logs or Crashlytics.
14. Release APK/AAB does not contain known Ravelry secrets.

## Implementation phases

Use small phases rather than one huge edit.

### Phase 1: audit

Report current Ravelry auth type, secrets, files, and behavior. Do not change functionality.

### Phase 2: backend skeleton

Create Firebase Functions v2 TypeScript structure, config, secret bindings, Firestore token store, and placeholder functions.

### Phase 3: auth migration

Move Ravelry login to backend functions. Android calls the backend and handles only status, not tokens.

### Phase 4: saved pattern preservation

Ensure existing Saved Patterns still work. Add internal source metadata without changing the visible library name.

### Phase 5: import by URL

Implement `ravelryImportPatternByUrl`, share target, and import confirmation.

### Phase 6: search or browse decision

Keep native search if it is already useful and can be safely proxied through the backend. Otherwise keep the search UI but use Ravelry web browsing as the main discovery path.

### Phase 7: cleanup and release hardening

Remove all Android secrets, remove unused OAuth code, add scan script, add tests, and document setup.

## Acceptance criteria

The work is done when:

- Tools > Ravelry remains visible.
- The app can connect to Ravelry through Firebase backend.
- Android contains no Ravelry secret values.
- OAuth token exchange happens server-side.
- Ravelry login uses Custom Tabs or Auth Tab, not Android WebView.
- Saved Patterns remains the user-facing saved pattern list.
- Existing saved patterns still work.
- Ravelry imported patterns appear in Saved Patterns.
- Pattern source is stored internally but not used as the main visible category.
- Imported patterns can be attached to projects.
- Disconnect deletes server-side Ravelry tokens.
- Release artifact scanning passes.
- The app has clear error states for cancelled login, invalid callback, expired state, backend failure, and Ravelry failure.

## References

- Firebase Cloud Functions overview: https://firebase.google.com/docs/functions
- Firebase callable functions: https://firebase.google.com/docs/functions/callable
- Firebase Functions configuration and Secret Manager: https://firebase.google.com/docs/functions/config-env
- Firebase anonymous authentication on Android: https://firebase.google.com/docs/auth/android/anonymous-auth
- Firebase App Check for Cloud Functions: https://firebase.google.com/docs/app-check/cloud-functions
- Android Auth Tab: https://developer.chrome.com/docs/android/custom-tabs/guide-auth-tab
- Android Custom Tabs interactivity: https://developer.chrome.com/docs/android/custom-tabs/guide-interactivity
- Android receiving shared data: https://developer.android.com/training/sharing/receive
- OAuth 2.0 for Native Apps, RFC 8252: https://datatracker.ietf.org/doc/html/rfc8252
- Ravelry Goodies page: https://www.ravelry.com/about/goodies
