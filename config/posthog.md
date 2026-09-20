# PostHog usage analytics

## Configuration

- SDK: `com.posthog:posthog-android:3.64.1`, pinned in the version catalog.
- Ingestion: `https://eu.i.posthog.com`; project 274433.
- Local config: ignored root `posthog.properties`, containing `projectToken=<project token>`.
- CI alternative: `KNITTOOLS_POSTHOG_PROJECT_TOKEN`. The environment value takes precedence.
- Release artifact tasks require a configured project token. Personal API keys are not accepted.
- Debug and benchmark variants disable collection even when the local preference is enabled.
- The end user enables **Settings → Share usage statistics**. It defaults to off; the SDK is not initialized until consent is loaded and enabled. DataStore remains the consent authority.

## Event contract

| Events | Meaning |
| --- | --- |
| `app opened`, `app backgrounded` | Foreground use after consent; background event includes `duration_seconds` |
| `$screen`, `screen exited` | Allowlisted navigation screen name; exit includes foreground duration |
| `project creation started/submitted/cancelled`, `project created`, `project creation failed` | Project list creation flow; cancellation is dialog dismissal, failure is an invalid project or missing folder result |
| `pdf import started/succeeded/failed` | Project PDF import after a source is selected; success follows the repository result |
| `counter incremented`, `counter decremented` | Main counter button actions; these describe requested actions, not persistence success |
| `work session start requested`, `work session saved` | Start action and confirmed save of a stopped work session |

No project IDs, names, counter values, text, URLs, photos, documents, billing state, or account identifiers are passed. Screen arguments are discarded and unknown screen names are dropped. All events disable GeoIP enrichment. SDK-generated pseudonymous IDs support returning-user and session analysis; they are not linked to Firebase or Ravelry identities.

Screen duration measures foreground time on a destination, including idle time. Process termination may omit exit/background events. Missing funnel completion does not prove a user cancellation. Modals are not separate navigation screens, and widget changes are not included in this first event set.

## Verification

Focused JVM tests cover consent defaults, no client initialization before consent, revocation, route sanitization, lifecycle deduplication, SDK failure isolation, SDK configuration, and preference-write failure. A synthetic ingestion request checks only endpoint acceptance; real-device delivery and dashboards must be verified separately with a consented release build. Native session replay, surveys, experiments, and an additional crash reporter are not enabled.

References: [Android SDK](https://posthog.com/docs/libraries/android), [API keys and EU ingestion](https://posthog.com/docs/api).
