# PostHog usage analytics

## Configuration

- SDK: `com.posthog:posthog-android`, pinned in the version catalog.
- Ingestion: `https://eu.i.posthog.com`; project 274433.
- Local config: ignored root `posthog.properties`, containing `projectToken=<project token>`.
- CI alternative: `KNITTOOLS_POSTHOG_PROJECT_TOKEN`. The environment value takes precedence.
- Release artifact tasks require a configured project token. Personal API keys are not accepted.
- Debug and benchmark variants disable collection even when the local preference is enabled.
- The end user enables **Share usage statistics** on the welcome step or in Settings. It defaults to off, never blocks continuing, and can be disabled again. Both controls write the same DataStore preference. The SDK is not initialized until persisted consent is loaded and enabled.

## Revocation policy

The user approved stricter revocation semantics in RW-07B on 2026-10-28: once revocation takes effect, block new captures and the start of any new usage-event delivery, discard pending events from the previous consent period, and prevent their replay through flush, retry, re-opt-in or SDK recreation using the same storage. Re-opt-in permits only newly consented events. Requests already in flight may complete; completed delivery and server-held data cannot be undone by this setting.

RW-07B implements this contract through `PostHogConsentClient`. Each SDK instance has an irreversible HTTP delivery gate. Revocation blocks capture and retires that gate **before** `optOut()` or `close()`; SDK flush, backoff retries, close and even reuse of the retired configuration cannot reopen it. Denied requests receive a local discard response without entering the HTTP transport; this is not evidence of server delivery. OkHttp connection-failure retries and redirects are disabled. Response-driven follow-ups within an already admitted call share that call's best-effort cancellation. Data already sent may have reached the server.

Each initialization uses a new, never-reused `cacheDir/knittools-usage-analytics/consent-<random UUID>` directory for event, replay, log and legacy queue prefixes. Revocation attempts to delete only that owned directory, and later initialization retries cleanup of recognized retired directories. A failed/interrupted deletion or a late asynchronous writer can leave bytes behind, but those paths never regain delivery eligibility. The former default SDK queue is not imported or broadly deleted. This deliberately also drops pending events across process/SDK recreation while consent remains enabled; analytics is best-effort, without durable cross-process delivery.

DataStore remains authoritative. Turning off atomically rotates a local consent-generation marker so a collector that receives only the final state of a rapid off/on transition still retires the old client. The marker is never sent to PostHog. Consent takes effect at the controller's application of the persisted snapshot; preference persistence and observation are asynchronous. SDK consent persistence is disabled, and SDK pseudonymous identity preferences are retained. Re-opt-in creates a fresh client and permits only events captured under that new consent.

The recorded acceptance checkpoint used publicly callable `PostHogInternal` HTTP-client and storage-prefix configuration in Android **3.72.1 / core 6.47.1**, under the user's scoped authorization. Rerun the real-SDK acceptance tests when changing either SDK version. The implementation retires its delivery gate and deletes its owned queue directory directly.

RW-07A remains historical evidence of the previous capture-only contract. The initial [blocked investigation](../reports/rw-07b-20261028/REPORT.md) is preserved; the [dated continuation](../reports/rw-07b-20261028/CONTINUATION-20261028.md) records implementation, acceptance and limits. **The production correction is absent from the retained RW-11D/E candidate**, which was not rebuilt or overwritten.

## Event contract

After consent, the Android SDK includes the app's display language as `$app_language` when it can resolve it (for example, `fi`). PostHog Android 4.0.3 can read it on Android 13 and later. The field may be absent while the SDK first reads the language.

| Events | Meaning |
| --- | --- |
| `app opened`, `app backgrounded` | Foreground use after consent; background event includes `duration_seconds` |
| `$screen`, `screen exited` | Allowlisted navigation screen name; exit includes foreground duration |
| `project creation started/submitted/cancelled`, `project created`, `project creation failed` | Existing project-list creation funnel, plus successful onboarding and shared-PDF project creation |
| `onboarding continued/back/skipped/completed` | Persisted onboarding navigation; fixed `step` names. Separate allowlisted onboarding screens measure the currently visible step after consent, never earlier steps retroactively |
| `calculator result shown/copied` | First valid result per tool visit/ViewModel task and confirmed clipboard copy in Measurements. Fixed `tool`; no inputs, outputs or pasted instructions |
| `pdf import started/succeeded/failed/cancelled` | File, onboarding, camera, gallery, Ravelry-browser and shared-document flows. Fixed `source`, with bounded `reason` where available. Success follows storage/attachment; existing-document duplicates count as successful opening. Onboarding success means the draft PDF was staged, before project creation |
| `counter incremented`, `counter decremented` | Main counter button actions; these describe requested actions, not persistence success |
| `work session start requested`, `work session saved` | Start action and confirmed save of a stopped work session |
| `extra counter created/used`, `counter undo requested`, `counter step changed`, `counter target changed`, `stitch tracking changed` | Counter features; fixed `counter_type` and enabled/disabled `setting`, never values or targets. Undo is a request |
| `reading aid changed`, `bookmark created/opened` | Horizontal/vertical guides, following the current row, and bookmark use; fixed `aid` and `setting`, no page, position, title or document key |
| `annotation created`, `annotation tool selected`, `annotated pdf export started/succeeded/failed/cancelled` | Committed annotation insertion, fixed `annotation_tool`, and PDF export outcome; no text, paths or geometry |
| `web pattern saved` | Saved/updated local web-pattern metadata; fixed web source, never the URL |
| `project completed/reopened`, `folder created`, `projects moved` | Confirmed project and organization operations; no names, IDs or selected-project counts |
| `yarn card created`, `yarn linked`, `yarn usage saved`, `notes saved`, `progress photo added` | Successful library, linking, yarn-usage, note and photo operations. Notes report at most once per editor instance, not per keystroke; no material names, quantities, text or images |
| `backup export/restore started/succeeded/failed/cancelled` | Manual backup/restore outcomes; fixed error categories only. Starts follow file selection; cancellations may therefore have no start event |
| `widget pin requested`, `widget installed`, `widget counter changed` | Pin request, first actual widget installation (`onEnabled`), and committed widget counter changes. Additional widgets do not generate another installed event while one exists |
| `insights range changed`, `insights chart viewed` | Fixed `range`; chart viewing once per range per ViewModel, when real data is visible. No project filters or statistics values |
| `welcome demo used`, `welcome tools chosen`, `welcome dismissed`, `guide opened`, `guide topic opened` | Welcome actions and fixed guide `topic` |
| `tip shown/acted/dismissed` | Visible tips and their actions, with a fixed `tip` name |
| `pro prompt shown/dismissed`, `pro details requested` | Contextual prompt visibility and actions with fixed `feature`; no billing, trial, ownership or price information |

`UsageAnalytics` accepts only `UsageEvent` and `UsageDetail` enum values, not arbitrary strings or maps. No project IDs, names, counter values, text, URLs, photos, documents, billing state, account identifiers, raw errors or logs are passed. Screen arguments are discarded and unknown screen names are dropped. All events disable GeoIP enrichment. SDK-generated pseudonymous IDs and SDK device/app metadata remain; identities are not linked to Firebase or Ravelry accounts. This is pseudonymous usage analytics, not a promise of zero device metadata.

Screen duration measures foreground time on a destination, including idle time. Process termination may omit exit/background events. Missing funnel completion does not prove a user cancellation. Modals are not separate navigation screens; contextual Pro prompts have explicit events. Widget actions wait up to one second for the initial persisted consent snapshot and otherwise drop the event. All delivery remains best-effort; a short widget-only process may end before a batch is sent. Counts describe consenting users only and must not be treated as all-user totals.

## Verification

Focused JVM tests cover consent defaults, no client initialization before consent, revocation, route sanitization, lifecycle deduplication, SDK failure isolation, SDK configuration, and preference-write failure. Real-SDK acceptance tests use actual disk queues, synthetic events/token and a terminal local recording transport installed before initialization for every URL; no request reaches a live service. Executor barriers, latches and a controlled SDK clock cover queued events, disabled capture/flush/retry/close, re-opt-in, prior-storage recreation, delayed writers, failed cleanup and the dispatch boundary. An actual file-backed DataStore test covers conflated consent changes and marker persistence. These are local JVM results, not Android/device, live-service/dashboard or signed-candidate acceptance. Native session replay, surveys, experiments, and an additional crash reporter are not enabled.

The opt-in `PostHogLiveDeliveryTest` additionally exercises the production controller/factory with the real Android SDK and EU HTTP ingestion. It runs only with instrumentation argument `posthogLive=true`, uses isolated preferences/cache and three synthetic events carrying `traffic=verification`, and tears down its SDK workers and storage. Filter `traffic != verification` (including events without that property) in product reports. A successful HTTP acknowledgement is checked separately against the server event query; neither proves delivery from a published signed release. See [current verification](../reports/posthog-verification/VERIFICATION.md).

The connected Gradle tasks use a network-free profile. For an authorized live check, use a disposable emulator without a personal KnitTools installation. Configure the project token, assemble the APKs without an isolation profile, and run only the live test. Replace `SERIAL` with that emulator's serial from `adb devices`.

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
adb -s SERIAL install app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL install app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s SERIAL shell am instrument -w -e class com.finnvek.knittools.analytics.PostHogLiveDeliveryTest -e posthogLive true com.finnvek.knittools.test/androidx.test.runner.AndroidJUnitRunner
```

References: [Android SDK](https://posthog.com/docs/libraries/android), [API keys and EU ingestion](https://posthog.com/docs/api).
