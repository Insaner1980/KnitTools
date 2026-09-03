# Privacy Policy

This technical privacy summary describes the current KnitTools implementation.

## Data Collection

KnitTools has no analytics, tracking, advertising, or release crash-reporting integration.

## Data Storage

Projects, counters, notes, imported pattern PDFs, annotations, yarn data, sessions, and photos are stored locally on the device. Android backup and device transfer are disabled for app data.

Optional network features use only the data needed for their operation:

- Ravelry connection, search, and metadata import use Firebase anonymous authentication and callable Firebase Functions. The backend stores the Ravelry OAuth state and tokens associated with the anonymous Firebase user and sends requested operations to Ravelry. KnitTools does not upload or download Ravelry pattern PDFs.
- Ravelry thumbnail images may be loaded from HTTPS URLs returned in pattern metadata.
- Purchases, restore, in-app review, and app updates use Google Play services.
- User-added pattern websites open in an external browser; KnitTools does not fetch, cache, or inspect those pages.

Debug builds can send diagnostics to Sentry only when a developer has configured a debug DSN. Release builds contain no Sentry dependency.

## Permissions

The Android manifest requests Internet access for the optional network features, vibration for feedback, and camera access for user-started pattern and progress-photo capture. Camera hardware is optional. The app does not request microphone permission.

## Contact

Contact information must be supplied before this policy is published.

*Last updated: 2026-09-03*
