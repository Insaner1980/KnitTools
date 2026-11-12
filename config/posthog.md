# PostHog usage analytics

## Configuration

- SDK: `com.posthog:posthog-android:3.75.0`, pinned in the version catalog.
- Ingestion: `https://eu.i.posthog.com`; project 274433.
- Local config: ignored root `posthog.properties`, containing `projectToken=<project token>`.
- CI alternative: `KNITTOOLS_POSTHOG_PROJECT_TOKEN`. The environment value takes precedence.
- Release artifact tasks require a configured project token. Personal API keys are not accepted.
- Debug and benchmark variants disable collection even when the local preference is enabled.
- The end user enables **Settings → Share usage statistics**. It defaults to off; the SDK is not initialized until consent is loaded and enabled. DataStore remains the consent authority.

## Revocation policy

The user approved stricter revocation semantics in RW-07B on 2026-10-28: once revocation takes effect, block new captures and the start of any new usage-event delivery, discard pending events from the previous consent period, and prevent their replay through flush, retry, re-opt-in or SDK recreation using the same storage. Re-opt-in permits only newly consented events. Requests already in flight may complete; completed delivery and server-held data cannot be undone by this setting.

RW-07B implements this contract through `PostHogConsentClient`. Each SDK instance has an irreversible HTTP delivery gate. Revocation blocks capture and retires that gate **before** `optOut()` or `close()`; SDK flush, backoff retries, close and even reuse of the retired configuration cannot reopen it. Denied requests receive a local discard response without entering the HTTP transport; this is not evidence of server delivery. OkHttp connection-failure retries and redirects are disabled. Response-driven follow-ups within an already admitted call share that call's best-effort cancellation. Data already sent may have reached the server.

Each initialization uses a new, never-reused `cacheDir/knittools-usage-analytics/consent-<random UUID>` directory for event, replay, log and legacy queue prefixes. Revocation attempts to delete only that owned directory, and later initialization retries cleanup of recognized retired directories. A failed/interrupted deletion or a late asynchronous writer can leave bytes behind, but those paths never regain delivery eligibility. The former default SDK queue is not imported or broadly deleted. This deliberately also drops pending events across process/SDK recreation while consent remains enabled; analytics is best-effort, without durable cross-process delivery.

DataStore remains authoritative. Turning off atomically rotates a local consent-generation marker so a collector that receives only the final state of a rapid off/on transition still retires the old client. The marker is never sent to PostHog. Consent takes effect at the controller's application of the persisted snapshot; preference persistence and observation are asynchronous. SDK consent persistence is disabled, and SDK pseudonymous identity preferences are retained. Re-opt-in creates a fresh client and permits only events captured under that new consent.

The recorded acceptance checkpoint used publicly callable `PostHogInternal` HTTP-client and storage-prefix configuration in Android **3.72.1 / core 6.47.1**, under the user's scoped authorization. This is historical, version-specific evidence and does not by itself validate the currently pinned 3.75.0 SDK. Rerun the real-SDK acceptance tests when changing either SDK version. The implementation does not rely on `optOut()`, `reset()` or `close()` purging a queue, and uses no production reflection.

RW-07A remains historical evidence of the previous capture-only contract. The initial [blocked investigation](../reports/rw-07b-20261028/REPORT.md) is preserved; the [dated continuation](../reports/rw-07b-20261028/CONTINUATION-20261028.md) records implementation, acceptance and limits. **The production correction is absent from the retained RW-11D/E candidate**, which was not rebuilt or overwritten.

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

Focused JVM tests cover consent defaults, no client initialization before consent, revocation, route sanitization, lifecycle deduplication, SDK failure isolation, SDK configuration, and preference-write failure. Real-SDK acceptance tests use actual disk queues, synthetic events/token and a terminal local recording transport installed before initialization for every URL; no request reaches a live service. Executor barriers, latches and a controlled SDK clock cover queued events, disabled capture/flush/retry/close, re-opt-in, prior-storage recreation, delayed writers, failed cleanup and the dispatch boundary. An actual file-backed DataStore test covers conflated consent changes and marker persistence. These are local JVM results, not Android/device, live-service/dashboard or signed-candidate acceptance. Native session replay, surveys, experiments, and an additional crash reporter are not enabled.

References: [Android SDK](https://posthog.com/docs/libraries/android), [API keys and EU ingestion](https://posthog.com/docs/api).
