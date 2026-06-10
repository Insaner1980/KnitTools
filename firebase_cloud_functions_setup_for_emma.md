# Firebase Cloud Functions setup for the Ravelry integration

Current date: 2026-06-10

This file is for the human setup work. Codex should not need your Ravelry secrets in the repository or in prompts. Codex can write the app and backend code, but you should create the Firebase project settings, enable the required Firebase products, set the secrets, and copy exact callback URLs into Ravelry.

## Goal

KnitTools will keep Ravelry visible inside Tools, but Ravelry secrets and OAuth token exchange must move out of the Android app. Firebase Cloud Functions will become the small backend layer between the Android app and Ravelry.

The Android app should only know how to call your Firebase backend. It should not contain the Ravelry consumer secret, client secret, token secret, access token secret, or any Ravelry token exchange logic.

## The target architecture

The intended flow is:

1. The user opens Tools > Ravelry in KnitTools.
2. KnitTools signs the user into Firebase anonymously if needed.
3. KnitTools calls a Firebase Cloud Function named `ravelryStartAuth`.
4. The Cloud Function uses Ravelry secrets stored in Secret Manager and returns only a Ravelry authorization URL.
5. KnitTools opens the Ravelry authorization page using Android Custom Tabs or Auth Tab.
6. Ravelry redirects back to an HTTPS Cloud Function callback.
7. The callback function completes the token exchange server-side, stores the user's Ravelry token server-side, and redirects back to the app.
8. The app asks `ravelryAuthStatus` whether the user is connected.
9. Pattern search and import calls go through Firebase Cloud Functions, not directly through Android secrets.

## What you do yourself

### 1. Use the existing Firebase project if possible

If you already created a Firebase project for Crashlytics, use that same project for Cloud Functions, Firebase Auth, Firestore, and later App Check. This keeps the Android package, SHA fingerprints, Crashlytics, auth, functions, and data safety documentation in one place.

Do not create a second Firebase project unless you intentionally want separate dev and production projects.

### 2. Confirm the Android app registration

In Firebase Console:

1. Open your Firebase project.
2. Go to Project settings.
3. Confirm that the Android app is registered with the real package name used by the Play Store build.
4. Add SHA-1 and SHA-256 fingerprints for the debug and release signing keys.
5. Download the current `google-services.json`.
6. Put it in the correct Android app module, usually `app/google-services.json`.

Codex can verify that the Gradle setup reads this file correctly, but you should download the file yourself from Firebase Console.

### 3. Upgrade billing only if Cloud Functions deployment requires it

Cloud Functions for Firebase runs backend code in Google's managed environment. In practice, production Cloud Functions deployments usually require the Firebase Blaze plan. If the Firebase CLI or console asks you to upgrade, use the Blaze pay-as-you-go plan and set a budget alert in Google Cloud Billing.

Recommended safety step:

1. Open Google Cloud Console > Billing > Budgets and alerts.
2. Create a low monthly budget alert for this project.
3. Add alert thresholds, for example 50 percent, 90 percent, and 100 percent.

Cloud Functions for this app should be cheap at low traffic, but still set alerts before release.

### 4. Enable Firebase Authentication with anonymous sign-in

In Firebase Console:

1. Go to Build > Authentication.
2. Open the Sign-in method tab.
3. Enable Anonymous.
4. Save.

This gives the backend a Firebase `uid` for each app install or user instance without forcing the user to create a visible KnitTools account.

Do not enable automatic cleanup for anonymous accounts until Codex confirms that deleting old anonymous accounts will not break Ravelry connection state for existing users.

### 5. Create Cloud Firestore

In Firebase Console:

1. Go to Build > Firestore Database.
2. Create the database.
3. Select production mode.
4. Choose a region and keep it consistent with the region used for functions when possible.

Firestore is needed for:

- temporary OAuth state records
- server-side Ravelry token records
- optional import status records

Client apps should not read the Ravelry token documents. Codex should write Firestore security rules that deny direct client access to token storage.

A strict starting point for token collections is:

```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /ravelryOAuthStates/{stateId} {
      allow read, write: if false;
    }

    match /ravelryTokens/{uid} {
      allow read, write: if false;
    }
  }
}
```

Firebase Admin SDK code inside Cloud Functions can still access these documents.

### 6. Install Firebase CLI locally

On your development computer:

```bash
npm install -g firebase-tools
firebase login
firebase projects:list
firebase use YOUR_FIREBASE_PROJECT_ID
```

If the repository does not already contain Firebase config files, Codex can initialize them, but the usual command is:

```bash
firebase init functions firestore
```

When asked, choose TypeScript for Functions unless Codex has a strong reason to use another setup.

### 7. Decide function region

Pick one Cloud Functions region and keep it consistent. For a Nordic/EU user base, an EU region is sensible. Do not hardcode a region in multiple places manually. Let Codex create one shared constant, for example:

```ts
const REGION = "europe-west1";
```

The exact region choice is less important than consistency and avoiding accidental duplicate deployments in multiple regions.

### 8. Get the Ravelry developer credentials

In Ravelry Pro or Ravelry developer settings, create or open the KnitTools application.

You need to know which auth type the current app uses. Ask Codex to audit this first:

- OAuth 1.0a: usually uses request token, authorize, access token, consumer key, and consumer secret.
- OAuth2: usually uses client ID, client secret, authorization endpoint, token endpoint, and redirect URI.

Do not paste the secret values into a Codex prompt. Codex only needs the names of the secrets, not the values.

### 9. Set Ravelry secrets in Firebase Secret Manager

After Codex confirms the auth type, set the matching secrets.

For OAuth 1.0a, use:

```bash
firebase functions:secrets:set RAVELRY_CONSUMER_KEY
firebase functions:secrets:set RAVELRY_CONSUMER_SECRET
```

For OAuth2, use:

```bash
firebase functions:secrets:set RAVELRY_CLIENT_ID
firebase functions:secrets:set RAVELRY_CLIENT_SECRET
```

You will be prompted to paste the secret values in the terminal. These values should not be committed to Git.

For local emulator testing, Codex may ask you to create `functions/.secret.local`. If you do that, make sure it is in `.gitignore` and never committed.

### 10. Deploy once to get the real callback URL

When Codex has created the first backend functions, deploy them:

```bash
firebase deploy --only functions
```

After deployment, copy the actual HTTPS URL for the callback function. It will look roughly like this, but use the exact deployed URL from your project:

```text
https://REGION-PROJECT_ID.cloudfunctions.net/ravelryCallback
```

Put that exact URL into the Ravelry app's redirect or callback URL settings.

If Ravelry requires a custom scheme callback instead of HTTPS, stop and have Codex report the limitation. The preferred design is Ravelry to Cloud Function callback, then Cloud Function to app deep link.

### 11. Give Codex the non-secret setup facts

After the Firebase setup is done, give Codex only these facts:

```text
Firebase project ID: YOUR_PROJECT_ID
Functions region: YOUR_REGION
Android package name: YOUR_PACKAGE_NAME
Ravelry callback URL registered: EXACT_CALLBACK_URL
Ravelry auth type found by audit: OAuth 1.0a or OAuth2
Secret names set in Firebase: RAVELRY_CONSUMER_KEY and RAVELRY_CONSUMER_SECRET, or RAVELRY_CLIENT_ID and RAVELRY_CLIENT_SECRET
```

Do not give Codex the secret values.

### 12. Test the deployed auth flow

Use a debug build first.

Test this exact sequence:

1. Open Tools > Ravelry.
2. Tap Sign in with Ravelry.
3. Confirm that Ravelry opens in Custom Tabs or Auth Tab, not inside a normal Android WebView.
4. Sign in on Ravelry.
5. Approve the app.
6. Confirm that the browser returns to KnitTools.
7. Confirm that Tools > Ravelry now says connected.
8. Close the app and reopen it.
9. Confirm that Ravelry is still connected.
10. Use Disconnect.
11. Confirm that Ravelry is no longer connected and server-side token data is deleted.

### 13. Enable App Check later, not before the flow works

App Check is a good protection layer for callable functions because it helps ensure backend calls come from your real app. Add it after the basic flow works.

Recommended order:

1. Codex adds App Check dependencies and optional debug setup.
2. You enable App Check in Firebase Console.
3. For Android, use the Play Integrity provider for production.
4. Test debug builds with the debug provider or debug token.
5. Only then enable enforcement for callable functions.

Do not turn on strict enforcement before Codex has implemented and tested App Check handling, or your app may suddenly fail to call its backend.

### 14. Update privacy and Play Store data safety text

Before release, update the privacy policy and Play Console Data safety form.

At minimum, account for:

- Firebase Crashlytics crash diagnostics
- Firebase anonymous authentication identifier
- Cloud Functions requests
- Ravelry account connection status
- Ravelry pattern metadata imported into KnitTools
- any local pattern files or PDFs stored on the user's device

Do not say that KnitTools syncs the Ravelry library unless Codex implements real repeated sync with duplicate handling.

Use "import" for launch unless true sync exists.

### 15. Rotate secrets if they were ever exposed

If the Ravelry secret has already been committed to Git or included in a build, treat it as exposed.

Do this:

1. Generate new Ravelry credentials if Ravelry allows it.
2. Update Firebase Secret Manager with the new values.
3. Redeploy functions.
4. Remove the old secret from Android code and build configs.
5. If the old secret was committed, do not rely only on deleting it from the latest commit. Consider the Git history exposed.

## What Codex should not need from you

Codex should not need:

- the actual Ravelry consumer secret or client secret
- your Firebase service account private key
- direct access to your Firebase Console
- direct access to your Ravelry developer account
- your Google Cloud billing details

Codex can work with secret names, function names, paths, and non-secret project IDs.

## Human checklist before release

Use this as your final manual checklist:

- Firebase project is the real production project.
- Android package name matches the Play Store package.
- Release SHA-1 and SHA-256 fingerprints are added to Firebase.
- Anonymous sign-in is enabled.
- Firestore exists.
- Firestore token collections are not readable by the client.
- Ravelry callback URL is registered exactly.
- Ravelry secrets are in Firebase Secret Manager.
- No Ravelry secret is in Android source code, resources, BuildConfig, Gradle files, manifest, tests, or sample data.
- A release APK or AAB was scanned for the secret values.
- Ravelry login uses Custom Tabs or Auth Tab, not Android WebView.
- Disconnect deletes server-side Ravelry tokens.
- Crashlytics logs do not contain tokens, secrets, verifiers, or authorization codes.
- Privacy policy and Play Data safety are updated.

## References

- Firebase Cloud Functions overview: https://firebase.google.com/docs/functions
- Firebase Functions configuration and Secret Manager: https://firebase.google.com/docs/functions/config-env
- Firebase callable functions: https://firebase.google.com/docs/functions/callable
- Firebase anonymous authentication on Android: https://firebase.google.com/docs/auth/android/anonymous-auth
- Firebase App Check overview: https://firebase.google.com/docs/app-check
- Firebase App Check for Cloud Functions: https://firebase.google.com/docs/app-check/cloud-functions
- Firebase App Check with Play Integrity on Android: https://firebase.google.com/docs/app-check/android/play-integrity-provider
- Android Custom Tabs overview: https://developer.chrome.com/docs/android/custom-tabs
- Android Auth Tab: https://developer.chrome.com/docs/android/custom-tabs/guide-auth-tab
- OAuth 2.0 for Native Apps, RFC 8252: https://datatracker.ietf.org/doc/html/rfc8252
