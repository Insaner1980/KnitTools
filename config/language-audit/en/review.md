# English Language Audit (en)

## Current acceptance decision — 2026-10-19 LA-en-B05-01 repair

**AUDIT_PASS; native-speaker review NOT_REQUESTED. LA-en-B05-01 VERIFIED.** The separately authorized repair resolves the only concrete blocker in the preceding acceptance review. This is English content acceptance under revised section 16, not exhaustive UI certification. All **231 supplementary rendering cases remain unverified**, with both registries and every V verdict unchanged. Stop after this defect; no other language, audit or rendering continuation is authorized.

### Reproduction, correction and affected English results

The three exact synthetic examples were executed through **actual Kotlin `InsightsViewModel.uiState`, `InsightsSessionAccumulator`, `SessionMetrics` and chart-axis logic before editing production code**. Only the repository flow, feature gates and calendar clock are substituted by the existing JVM/MockK infrastructure; this does not claim Room or Android UI execution. Every example asserts its completed-session end is before its explicit synthetic current instant. `reproduction.log` and retained XML show **5 methods: 4 FAIL / 1 PASS**: the three reported failures and the same defect in fixed ranges fail; the same-zone/project-filter control passes. Failures retain actual active counts 2 versus expected 1, 3 versus expected 2, and first date November 1 versus expected October 31. They are preserved, not replaced by final passing results.

Cause: `activeDates` had a lower bound but no device-calendar upper bound, while `daysInRange`, rangeEnd and the chart axis end at today. The All Time range also used an unbounded first stored-zone date, although the existing chart already bounds its first day to today. Valid past instants can have later saved-zone calendar dates after a zone change.

Correction: the accumulator now admits only activity dates within the displayed calendar interval through today. The ViewModel bounds the presentation's first-session date to today before producing the axis, denominator and range. The actual date set is filtered before counting; the displayed numerator is not clamped. Raw session dates/zones, completed records, exact duration accumulation, query/project filters and ordinary range semantics are preserved. Total time continues to include completed-session durations under the existing instant-based rules, even when their stored-zone date lies outside the current calendar window. No resource, translation, plural selector or historical row/pace gate changes.

| Exact reported case | Verified final calendar bounds and counts | English composition | Preserved total |
| --- | --- | --- | --- |
| Now Oct 1 02:00 -10:00; Oct 1 -10:00 and Oct 2 +14:00 sessions | Oct 1–Oct 1; active 1 / days 1 | **Active 1 of 1 day**; **Oct 2026** | 60 minutes |
| Now Oct 2 02:00 -10:00; Oct 1/2 -10:00 and Oct 3 +14:00 sessions | Oct 1–Oct 2; active 2 / days 2 | **Active 2 of 2 days**; **Oct 2026** | 90 minutes |
| Now Oct 31 02:00 -10:00; Nov 1 +14:00 session | Oct 31–Oct 31; active 0 / days 1; stored first date still Nov 1 | Visible All Time range **Oct 2026**, never Nov→Oct. Shared metadata is **Active 0 of 1 day**; the one-session chart gate remains unchanged. | 30 minutes |

Re-read both `insights_days_meta_format` variants, the three `insights_chart_a11y_daily/weekly/monthly` templates and the All Time `insights_range_format` caller (`#site2`). Their wording is accurate with the corrected arguments. The regression composes the unchanged English XML with the resulting production state, checking the shared active-day metadata embedded in all three chart descriptions. Source assertions verify both the untouched chart header and accessibility description consume `daysMetaText` with the denominator as plural quantity. English month labels use an explicit JVM `MMM yyyy` pattern matching the inspected English month/year composition; Android ICU pattern selection, Compose rendering and spoken output were **not** rerun. Unchanged presentation source plus the producer/resource evidence resolves this concrete factual/context question; no device check is needed to determine the corrected dates or counts.

The same-zone control retains Sep 30–Oct 2, **Active 3 of 3 days**, **Sep 2026 – Oct 2026**, 90 minutes. Month/week and selected-project controls preserve their established starts, counts and durations. Both fixed ranges exclude the later saved-zone active date while retaining 60 minutes. Existing exact-second, stored-zone, chart, filter and historical-unit suppression regressions also pass.

### New executions and acceptance reconciliation

- New final execution: **107/107 JVM tests PASS in 11 suites**, zero failures/errors/skips. `InsightsSessionZoneRangeTest` contributes five methods; the other ten existing suites are enumerated in `test-arguments.txt` and retained XML. Direct Gradle `:app:testDebugUnitTest`; production and JVM test compilation completed. Scoped ktlint format/check of the two implementation files and the new regression file PASS. No lint/device/build-release certification is claimed.
- Evidence root: `reports/language-audit/shared/20261019-b05/`: entry file snapshots and fingerprints, `reproduction.log`, `reproduction-results/`, `fixed-tests.log`, `fixed-results/`, `test-summary.json`, `ktlint.log`, inventory and final preservation/hash checks. Prior failures from earlier tasks remain in their original records. Older acceptance/shared/device results below are **reused evidence**, not newly executed checks.
- Entry reconciliation: all **448/448** previous manifest hashes matched. Fresh XML inventory remains **1,351 keys / 1,349 translatable keys / 1,382 resource variants / 2,315 unique context/code rows**, with no missing or extra covered variant. Resource bytes, aliases, other caller bodies and factual/terminology content are unchanged. The prior complete B01–B23 acceptance review supplies unchanged content judgments; only the six named F/C failures are newly corrected and re-read. The one existing `InsightsHistoricalUnit` code-backed row receives the new ViewModel hash and a focused regression recheck; its no-historical-unit policy remains intact.
- Final manifest: **449 inputs** (the new focused test added; the two changed implementation hashes and the changed `LANGUAGE_AUDIT.md` / `shared-fixes.md` record hashes refreshed). No unrelated evidence hash is renewed. Current counts: **T/L/C 2,315 PASS; F 2,304 PASS / 11 N/A; R 1,879 PASS / 436 N/A**. B05 and the other content batches are REVIEW_COMPLETE / PASS; no concrete content blocker or specific unresolved language question remains.
- Supplementary V remains **1,858 PASS / 231 BLOCKED / 226 N/A**; registry **1,177 IDs, 231 PENDING**, including all 15 frozen Insights cases. Both registry files remain byte-identical. No V row is relabelled from JVM/source evidence.
- Existing feature branch and unrelated work preserved; no commit, push or PR. Only this repair, focused tests and necessary audit records changed. Gradle used `--no-daemon`; task-owned launcher/daemon completion was verified at closeout. No user records, emulator, 320 dp, 200% font scale, combined case, TalkBack or screenshot campaign.

---

## Historical acceptance decision — 2026-10-19 before B05-01 repair

**REVIEW_COMPLETE / OPEN; not AUDIT_PASS. Native-speaker review: NOT_REQUESTED.** Current B01–B23 content coverage is complete, but the stopped Insights questions establish one shared factual/context defect, **LA-en-B05-01**, below. The 231 unverified rendering cases are supplementary coverage and are **not** the reason for this decision. Language acceptance is not exhaustive UI certification.

This bounded acceptance review supersedes the outcome, not the evidence, of the historical sections below. It does not resume rendering, authorize a repair, or reopen unchanged verified findings.

### Current inventory and input reconciliation

- Branch `codex/audit-korjaukset-20261018`, HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`; staged diff empty. Existing tracked/untracked work was fingerprinted before editing and preserved.
- Compared all **448** manifest entries: **447 matched**; only `LANGUAGE_AUDIT.md` differed. Its recorded SHA-256 was `c27ce85693d54fa4f6a26ff0b4dab1fb3a92cd30e74d9b87c357d302415b9d64`; entry SHA-256 was `204db739fea5952d76b94c035a0a1ad6f5b0ba1ea41884609a8b62991d7c88ff`. The current policy explicitly separates language acceptance from exhaustive rendering in sections 11 and 16. No resource, formatter, factual-data, production-meaning or test input changed since the stopped inspection. This is newly resolved evidence about unchanged code, not input drift.
- Enumerated current `main`, `debug`, `debugShared`, and `release` Kotlin/XML sources excluding other locales: **403 files**, all in the matching manifest; no added unmanifested or removed baseline source file. Existing caller/context review therefore remains applicable. No unchanged batch was reread as a new audit.
- Parsed all default text-bearing XML and compared every key/variant, text and resource path with coverage: **1,351 keys, including two nontranslatable numeric examples; 1,349 translatable keys; 1,382 resource variants; 1,879 resource-context rows plus 436 code-backed rows = 2,315 unique rows across 23 batches**. No inventory difference; every row's source hash matches. The one alias (`web_pattern_error_designer_too_long`) and its reviewed target/contexts are unchanged. There are no string arrays.
- Only the checked `LANGUAGE_AUDIT.md` manifest entry is refreshed after this decision's summary edits. All production/evidence hashes retain their prior values; this does not renew approval of unreviewed content.

### Existing findings and verification evidence

All ten original shared findings remain **VERIFIED**, as do B03-04, B19-02 and B03-05. Their independent English rechecks above the historical initial audit remain controlling. Read the shared correction/follow-up records and retained receipts: the original shared XML reports 278/278 passing tests in 35 suites; follow-up XML reports 188/188 in 25 suites; the final historical-unit Room method receipt is 1/1 PASS. These are **reused historical executions**, not tests run in this task. All 12 final follow-up input hashes still match.

The eight differences from the older 65-input shared manifest are the already reconciled historical-unit/symbol follow-up and subsequent English wording corrections, not new drift. B08-01 remains **VERIFIED**: retained `20261019-b08/fixed-instrumentation.log` records `savedPdfStates` passing, and `control-instrumentation.log` records the corrected negative control passing. The first fixed run's one failed control remains historical failure evidence. Three of its four final input hashes still match; the later English `tool_result_placeholder` change explains the string-file difference and was independently verified by the rendering continuation. Neither the PDF explanation nor its caller changed. B13-01/B14-03 and the earlier English corrections retain their final-version review. No verified issue is reopened.

### LA-en-B05-01 — session-zone dates contradict the displayed range

**CONFIRMED_DEFECT; FACT/CONTEXT; S2; OPEN.** Owner: a separately authorized shared Insights repair. Affected English items: both variants of `insights_days_meta_format`, its embedded use in `insights_chart_a11y_daily/weekly/monthly`, and `insights_range_format` at `InsightsScreen.insightsRangeLabel`'s All Time caller (`#site2`). The literals and plural choice are not themselves mistranslations; their production arguments make false statements.

Production chain inspected:

- `CounterRepository.observeSessionsForInsights` / `firstInsightDate` (1622–1703) read completed sessions and derive the first date in each stored session zone; the batch queries in `SessionDao` impose no current-date upper bound.
- `SessionMetrics.dailySecondContributions` (158–208) retains the session's zone. `InsightsSessionAccumulator.add` (64–81) adds those dates to `activeDates` with only a lower date bound. `SessionPaceAccumulator` (135–153) separately counts future measured buckets.
- `InsightsViewModel.buildUiState` (303–344) uses the full active-date count, but `daysInRange` (481–490) and `rangeEnd` end at the device's current date. One positive current bucket plus one future bucket meets the two-bucket chart gate.
- `InsightsScreen.chartSection` (404–448) consequently reaches `chartHeaderMeta` / `daysMetaText` (459, 563–569) before any user selection. The same metadata is inserted into chart semantics (537–560; `InsightsChart` 77–83). The plural quantity is correctly the denominator, `daysInRange`; changing the plural selector would not fix the false fraction.

Source-traced counterexamples, independently checked with a one-shot Python `datetime` calculation using the explicit -10:00/+14:00 offsets already represented by the existing Honolulu/Kiritimati regression source:

| Case | Completed sessions and current time | Derived production result |
| --- | --- | --- |
| Singular denominator | Device now `2026-10-01T02:00-10:00` (12:00Z). Session A: Oct 1 00:00–00:30 at -10:00; B: Oct 2 00:30–01:00 at +14:00. Both finish before now (10:30Z and 11:00Z). | All Time first date Oct 1; active dates Oct 1 and 2; denominator 1; 60 minutes; one retained and one future positive daily bucket. Header: **Active 2 of 1 day**. Pro chart semantics embeds the same false fraction. |
| Plural denominator | Device now Oct 2 02:00 at -10:00. Thirty-minute sessions on Oct 1 and Oct 2 at 00:00 -10:00, plus Oct 3 00:30–01:00 +14:00. All end before now. | All Time first date Oct 1; three active dates over two device-calendar days: **Active 3 of 2 days**. Chart gate remains satisfied. |
| All Time range caller | Device now `2026-10-31T02:00-10:00`. Only session: `2026-11-01T00:30+14:00` to 01:00, ending at Oct 31 11:00Z, before now. | `rangeStart=2026-11-01`, `rangeEnd=2026-10-31`; month labels are passed in reverse order (November 2026 → October 2026). The visible All Time kicker is outside the chart gate, so a single session suffices. |

These are valid past instants with saved session zones (for example after device-zone changes), not fabricated future timestamps or restored historical row/pace metrics. The existing `SessionMetricsTest` source explicitly preserves Kiritimati dates after a switch to Honolulu; `InsightsSessionAccumulatorTest` source also confirms that future buckets are intentionally counted for the comparison flag. Those sources were inspected, not rerun. The Python check validates the dates/count arithmetic; it is **not execution of Kotlin, Room, Android formatting, pixels or spoken output**. The data-flow and unconditional resource composition establish the defect without a new rendering run. Weekly/monthly descriptions inherit the same unchecked metadata producer; only the daily composition is instantiated in these examples.

**Smallest next action:** authorize one shared Insights correction making the active-day numerator, denominator and displayed range describe the same date set while preserving stored session zones and completed-session data. Add a focused producer/composition regression for the three examples above, then recheck only the affected English output. Require a truthful active-days fraction and a non-reversed range; do not hide the problem by merely changing singular/plural wording or clamping the displayed numerator. No full rendering backlog or general date/analytics audit is needed. No repair was made here.

### Disposition of the stopped date-range question

| Recorded caller | Actual context and acceptance disposition |
| --- | --- |
| `projectFabricRangeLabel`, `insights_range_format` primary / line 351 | Describes the fabric model's displayed window in its visible header and `insights_project_fabric_a11y_summary`. Same-year start is short, end carries the year; cross-year endpoints both carry years. The model's generated days end at today and it returns null without visible activity. **Meaning resolved; no defect established in this caller.** |
| `insightsRangeLabel` All Time / line 598, `#site2` | Month/year range is visible in `InsightsContextRow`'s kicker and reused in chart semantics; same-month history uses one month/year label. **Open only for the concrete reversed range in B05-01**, not for missing rendering. |
| `insightsRangeLabel` week/month / line 614, `#site3` | The fixed date range feeds the chart content description, even though the visible kicker is All Time only. Its start is the selected week/month boundary and end is today; English month-first patterns keep both month names, and day-first patterns compact only within the same month/year. The surrounding “Daily/Weekly/Monthly … time” describes bucket granularity. **Range wording/context resolved; no independent range-format defect.** The embedded active-day metadata remains covered by B05-01. No TalkBack or rendering verdict is inferred. |

The range-menu labels belong to `insightsRangeOptions` / `InsightsRangeTitle`, not the project-filter labels; the earlier registry scope imprecision is not a language defect. All 15 frozen cases remain unverified, including their existing owner labels. The other frozen fallback/headings/detail/menu entries contain no separately recorded language question requiring a new task here.

### Acceptance record and stopping point

- **B05: REVIEW_COMPLETE / OPEN (B05-01).** Other batches retain complete content review and no open content defect under the revised gate. Six existing composition rows now have F/C FAIL, with prior decisions retained in their notes: T/L **2,315 PASS**; F **2,298 PASS / 6 FAIL / 11 N/A**; C **2,309 PASS / 6 FAIL**; R **1,879 PASS / 436 N/A**.
- Rendering is unchanged: **1,858 PASS / 231 BLOCKED / 226 N/A**; registry **1,177 IDs**, including **231 PENDING**. Both registries remain byte-identical. No pending case becomes PASS or N/A, and no historical rendering pass is broadened.
- Performed: branch/staged/workspace checks; SHA-256 reconciliation; current XML/coverage/source-inventory comparison; shared receipt/XML inspection; the named production-path review; independent date/count arithmetic; final record/diff consistency and preservation checks. No build, test suite, fixture, instrumentation, screenshot campaign, 320 dp, 200% font scale, combined case or TalkBack run. No temporary background process was started.
- **Stop after this decision.** No commit, push, PR, repair, other language or rendering continuation. Native-speaker review remains **NOT_REQUESTED**; exhaustive UI certification remains incomplete and separate from language acceptance.

---

## Historical outcome — 2026-10-19 InsightsScreen inspection stopped at user request

**STOPPED_AFTER_INSPECTION. English remains CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING; not AUDIT_PASS.** The user explicitly stopped further exhaustive rendering verification before any fixture, build, instrumentation test or screenshot batch was started. This handoff supersedes earlier resume instructions; do not automatically resume another owner or language.

- Frozen selection: **15 cases — EN-V-0119, EN-V-0120, EN-V-0481, EN-V-0482, EN-V-0483, EN-V-0500, EN-V-0501, EN-V-0502, EN-V-0505, EN-V-0509, EN-V-0510, EN-V-0517, EN-V-0527, EN-V-0528, EN-V-0529**. They exactly match the current unresolved `InsightsScreen.*` registry rows. Actual production references were inspected in `InsightsScreen.kt`; the registry's scope label for 0527–0529 is imprecise: these are `insightsRangeOptions` entries rendered by `InsightsRangeTitle`, not visible labels inside `InsightsProjectFilter`.
- Completed: inspected project instructions, current audit outcome/records, relevant production callers and ViewModel/accumulator/chart/fabric paths; verified **all 448 recorded input hashes match**; fingerprinted the existing dirty workspace on `codex/audit-korjaukset-20261018`; saved original records and the frozen selection under `reports/language-audit/en/20261019-insights/`.
- New rendering verdicts: **0 PASS, 0 N/A, 15 still unverified/PENDING**. No new rendered image or text-geometry evidence exists from this task. Coverage and both case registries remain unchanged; overall pending English count remains **231** (V: 1,858 PASS / 231 BLOCKED / 226 N/A across 2,315 rows). Earlier completed owners and evidence remain intact. BLOCKED here continues to mean outstanding evidence, not a newly established external blocker.
- Source observations only: `daysMetaText` selects the plural using **daysInRange**, not activeDays. `chartSection` requires positive minutes and meaningful chart data; the ViewModel counts positive measured buckets plus future measured buckets. This raises a reachability/count-consistency question when stored session zones produce dates after the device's current date. It was **not reproduced or confirmed as a defect**. Do not classify the singular branch N/A without resolving that path, and do not manufacture a state by restoring historical row/pace metrics.
- The fixed week/month range branch in `insightsRangeLabel` appears to feed the chart content description while the visible kicker is restricted to All Time. This remains a **source observation, not an N/A disposition**; no selected case was approved or excluded when stopping.
- Remaining verification, only if separately requested: both fallback-name callers, each date-range caller's visual/reachability classification, daily/monthly and project-breakdown headings, empty day details, singular/plural active-day metadata with production-valid quantities, combined chart metadata, and all three range-menu options. Use the existing isolated host and production formatting. No unfinished fixture exists to resume.
- **No confirmed terminology, factual, grammar, comprehension or shared implementation defect was established in this bounded inspection. No wording suggestion or production edit was made.** No earlier accepted evidence was invalidated by an input change. The potential issues above are investigation notes, not findings or repair authorization.
- Cleanup: only the task-started `KnitToolsLanguageEn20261018` emulator (`emulator-5580`, PID 34224), its QEMU child and verified console child were stopped. `process-cleanup.json` records identity and completion. No app installation, data seeding or user-record access occurred. No 320 dp, 200% font scale, combined or TalkBack checks ran. No commit, push or pull request.

Evidence: `selected-cases.tsv`, `changed-inputs.json` (empty), `start.json`, `git-status-before.txt`, `before/`, `process-cleanup.json`, and `final-verification.json` in the task evidence directory. No acceptance criteria were changed. **Stop here; the user's latest instruction cancels further rendering work in this task.**

---

## Historical PatternPickerSheet outcome — counts retained; resume superseded above

**PATTERNPICKERSHEET_COMPLETE. English remains CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING; not AUDIT_PASS.** All **15 selected cases PASS**, with **0 NOT_APPLICABLE and 0 unresolved**. English now has **231 pending rendering cases overall** (previously 246). No English wording or shared implementation defect was confirmed, and no production code/resource or shared-test file was changed. ProjectListScreen, CounterScreen, PatternViewerScreen and all unrelated results remain unchanged.

This section supersedes the historical counts and resume instructions below. T/F/L/C/R decisions and earlier evidence are preserved. Native-speaker review remains NOT_REQUESTED. The coordinator-owned root campaign summary is unchanged; the English coverage and current rendering registries hold this bounded result.

### Frozen selection, source verification and input identity

The current PENDING registry rows owned by `PatternPickerSheet.*` were compared with every actual production resource reference and frozen before runtime execution:

**EN-V-0188, EN-V-0212, EN-V-0213, EN-V-0214, EN-V-0215, EN-V-0216, EN-V-0217, EN-V-0233, EN-V-0234, EN-V-0235, EN-V-0248, EN-V-0322, EN-V-0443, EN-V-0445, EN-V-0995.**

Branch `codex/audit-korjaukset-20261018`; HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. All **446 prior input hashes matched at entry and reconciliation**. The manifest additionally pins the new ignored fixture and its init script, for **448 inputs**. The existing dirty/untracked workspace was fingerprinted and preserved. No earlier accepted evidence was invalidated by a production-input change.

Evidence root: `reports/language-audit/en/20261019-patternpicker/`. `selected-cases.tsv` retains the frozen entry rows; `actual-callers.json` pins source locations/expressions. `source-decisions.md` traces production conditions and actual CounterScreen/PatternViewerScreen entry points. `observations.json` maps all 15 cases to their actual caller, variants, inspected PNG/hash, exact Compose geometry or native pixel measurements, and generating method/run receipt. There are **19 distinct accepted images**. Cumulative adb pulls are attributed to their original generating runs, not the pull time. `before/` preserves the prior records.

### Production conditions and inspected results

The fixture renders the **actual public PatternPickerSheet**, its production private content, image-import surface, ConfirmationDialog, Hilt-owned PatternImageImportViewModel and KnitToolsTheme. It reuses the existing CounterScreen audit Activity/manifest and prior text-geometry helper; no duplicate layout or resource gallery was introduced. Outer navigation is omitted, so this is owner-component verification, not navigation E2E.

| IDs (EN-V prefix) | Production condition, context and result |
| --- | --- |
| 0188 / 0322 | INITIAL_PROJECT_PATTERN displays **Attach pattern**; ADD_READABLE_PROJECT_DOCUMENT displays **Add Pattern**. Both titles independently inspected in empty/populated and partial/expanded states. Complete single-line text fits. CounterScreen chooses the mode by entry/document state; PatternViewerScreen Documents Add uses ADD_READABLE. |
| 0212 | The nonempty effective saved list displays **SAVED PATTERNS**, the intentional SectionLabel uppercase transformation. INITIAL includes PDF/web/Ravelry metadata. ADD_READABLE filters to local PDFs and excludes already attached IDs. Both modes' bounded inner lists were scrolled, and empty effective lists correctly omit the heading. |
| 0213 / 0214 | **Import PDF** and **Import from Drive or Dropbox** fit completely in both modes. Both actual callbacks launch the same OpenDocument contract; its base MIME type is `*/*` with `application/pdf` in EXTRA_MIME_TYPES. Controlled cancellation returns to the sheet. No provider/account or synchronization claim is made. |
| 0215 / 0995 | **Import from Ravelry** and **Add Web Pattern** fit completely in INITIAL, are absent in ADD_READABLE, and their actual callbacks dismiss before invoking the requested navigation callback. No website, account or download was used. |
| 0216 / 0217 | **Take pattern photo** and **Choose images** fit in both modes with purchased (no badge), trial/free (PRO badge), and disabled null-project presentation. Production source confirms the nonnull-project enabled condition and entitlement/Pro-prompt branch. The gallery callback staged two synthetic images; the camera action exercised real Android permission denials. No real purchase, camera capture or external photo selection is claimed. |
| 0248 | **Continue without pattern** appears only in INITIAL. It fits after scrolling the long populated sheet and directly in expanded empty state; clicking dismisses. Partially clipped edge captures are retained but not accepted as proof of this label. |
| 0233 / 0234 / 0235 | Dismissing two nonbusy selected synthetic images opens the actual confirmation. **Discard selected images?**, **The selected pattern pages will be removed.**, **Cancel**, and **Discard images** are fully visible together. Cancel retains both staged pages and re-shows the sheet. A second dismissal and Discard remove both staged copies, retain the source images, and close the sheet. No PDF/project attachment was created. |
| 0443 | Actual first Android camera denial leaves `shouldShowRequestPermissionRationale=true`; production helper displays **Camera permission is needed to take photos**. Complete two-line native Toast inspected. |
| 0445 | Actual second Android camera denial leaves rationale=false; production helper displays **Camera permission denied — enable it in Settings**. Complete two-line native Toast, including em dash, inspected. |

The selected-image surface replaces ordinary picker content when there are selected pages, busy work or an error. Dismissing a busy import cancels it; dismissing an idle nonempty selection requests confirmation. A null project disables camera/gallery rows and makes the PDF-launch closure a no-op. Restoring-project state postpones pending document/camera result handling; stale project ownership is checked before acceptance. These conditions were traced in current production code; no approval is inferred from another owner's identical strings.

PDF/image **result injection is controlled**, through ActivityResultRegistry, while the unmodified production callbacks and real staging/cancellation code run. Camera tests use the **Activity's real registry and Android's actual permission dialog**, click Don't allow twice, record denied permission and true/false rationale, and capture the resulting native Toasts. Thus those two cases include actual permission-workflow execution on this device, beyond controlled presentation. There was no permission grant or camera launch.

### Geometry and validation actually executed

Device: task-started `KnitToolsLanguageEn20261018`, `emulator-5580`, Android 16/API 36, 1080×2400px, 420dpi, approximately 411dp width, font scale 1.0, `[en,en_US]`, light production theme. Package `com.finnvek.knittools.activepolicytest` is isolated and has no INTERNET/BILLING permissions. The existing physical device was not used. **320dp, 200% font scale, their combination and TalkBack were not run.**

Accepted Compose targets have empty geometry issue lists, with actual bounds/size, constraints, paragraph width, per-line extents/end, max-line/ellipsis checks and parent clipping retained. The prior center-coordinate correction is unchanged. Offscreen rows, normal scroll-edge clipping and hidden background-sheet nodes remain flagged in raw captures and receive no approval. Long-sheet top/bottom views and independent saved-list scrolls were inspected. All selected Compose labels/sentences fit on one line in their accepted viewport.

The complete discard dialog has measured text nodes of **670×76px** (title), **634×44px** (message), **99×44px** (Cancel) and **222×44px** (Discard images). Native Toasts are measured separately from Compose: both surfaces occupy `[120,2053,960,2211]`; ordinary-message glyph bounds are `[254,2098,859,2175]`, permanent-message bounds `[253,2098,913,2175]`, each with two complete lines and margins. These are pixel measurements, not fabricated native text-layout results.

| Executed check | Actual result and retained failures |
| --- | --- |
| Four direct builds: `:app:assembleDebug :app:assembleDebugAndroidTest -Pknittools.isolatedTest=activepolicy -I reports/language-audit/en/20261019-patternpicker/fixture.init.gradle --no-daemon --console=plain` | **All four succeeded**, using the existing Temurin JDK17 toolchain. Each reports 6 executed / 78 up-to-date tasks; these are not four fresh recompilations of every production source. Fixture override-parameter-name warning and initial Gradle deprecation notice remain in raw logs. |
| run1, 03:51:49–03:52:56 +03:00 | Four methods: modesAndScrolling and actualCameraPermissionDenials passed. selectedImageDiscard failed because the first Back only collapsed the sheet; actionContexts failed on a fixture assumption that PDF MIME was the base intent type. First Toast image caught the entrance animation and is rejected despite the passing test. |
| run2, 03:54:17–03:54:54 +03:00 | Camera workflow and corrected PDF/navigation callbacks **PASS**. Stable images show both full Toasts. Discard reached the complete dialog but failed because Cancel matched both the dialog and hidden sheet. |
| run3, 03:56:34–03:56:46 +03:00 | Discard retry still failed: restricting Cancel to a dialog ancestor also matched the sheet's dialog host. Preserved as a failed fixture-selection attempt. |
| run4, 03:58:06–03:58:21 +03:00 | selectedImageDiscard **PASS**, selecting and asserting the visible confirmation Cancel. Both confirmation captures and retained-page state inspected; real staging deletion verified. |
| Evidence/registry reconciliation | **15 PASS / 0 N/A / 0 unresolved**; all 15 actual caller/PNG/geometry/run mappings checked. Current registry totals agree. All prior inputs and unrelated rows/workspace changes preserved. |

There are **four distinct finally passing test methods**. Mixed/failed earlier runs are not recast as successful. Receipts pin exact instrumentation commands, APK/test/fixture/host hashes and run times. Resource/unit suites were not rerun because production code/resources did not change. No shared implementation handoff was necessary; the retained failures were fixture navigation/selection/contract expectations and capture timing.

### Current counts, blockers and resume boundary

Inventory remains **2,315 coverage rows and 1,177 stable rendering IDs**. Dimensions: **T 2,315 PASS; F 2,304 PASS / 11 N/A; L 2,315 PASS; C 2,315 PASS; R 1,879 PASS / 436 N/A; V 1,858 PASS / 231 BLOCKED / 226 N/A**. B08 V is 103 PASS / 15 BLOCKED / 5 N/A; B09 V is 71 PASS / 8 BLOCKED / 10 N/A. BLOCKED remains the coverage value for outstanding evidence, not a claim of an external blocker for every other case.

**Exact blockers for this selected scope: none. Confirmed English/shared defects: none.** No selected case was marked N/A. The current remaining registry contains **231 rows and no PatternPickerSheet, PatternViewerScreen, CounterScreen or ProjectListScreen cases**. Earlier accepted contexts were not broadened to other owners. Stop after PatternPickerSheet; do not begin another owner or language and do not declare the entire English audit passed.

Task-created staging remnants from failed runs were removed from their verified isolated paths. Cleanup initially stopped safely on an incorrect .png extension guard (production stages .img), then once while the cleanup-only emulator restart was still booting; the final retry succeeded after boot/unlock. Both failed attempts and their resolutions are retained in cleanup-retained-failures.json. No audit test was rerun for cleanup. The task-started emulator and identified children were stopped; direct Gradle and instrumentation commands exited. Cleanup identity/results are retained in `process-cleanup.json` and `staging-cleanup.json`. No temporary process is retained. No commit, push or pull request was made.

---

## Historical PatternViewerScreen outcome — counts and resume superseded above

**PATTERNVIEWERSCREEN_COMPLETE. English remains CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING; not AUDIT_PASS.** Every currently unresolved PatternViewerScreen-owned case was processed: **15 selected, 14 PASS, 1 justified NOT_APPLICABLE, 0 unresolved**. English has **246 pending rendering cases overall**. No English wording or shared implementation defect was confirmed; no production resource/code or shared-test change was made. ProjectListScreen, CounterScreen and previously verified shared findings remain closed and unchanged.

This section supersedes historical counts and resume instructions below. All other owners' rows and earlier evidence, including T/F/L/C/R decisions, are preserved. Native-speaker review remains NOT_REQUESTED. The coordinator-owned root campaign summary is unchanged; these English records and the current remaining registry carry this outcome.

### Frozen selection and input identity

The current registry was filtered for PENDING `PatternViewerScreen.*` rows and every actual production resource reference was checked before execution. The resulting frozen selection matches the requested IDs:

**EN-V-0037, EN-V-0049, EN-V-0192, EN-V-0196, EN-V-0197, EN-V-0206, EN-V-0207, EN-V-0429, EN-V-0702, EN-V-0703, EN-V-0704, EN-V-0705, EN-V-1056, EN-V-1061, EN-V-1065.**

Branch `codex/audit-korjaukset-20261018`; HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. Existing dirty/untracked work was fingerprinted before edits and preserved. **All 443 previously recorded hashes matched at entry and exit.** The manifest additionally pins the new ignored fixture/init script and the reused Room fixture source, giving **446 inputs**. No earlier accepted evidence was invalidated by a production input change.

Evidence root: `reports/language-audit/en/20261019-patternviewer/`. `selected-cases.tsv` freezes entry rows; `actual-callers.json` records the exact source references and real functions. `observations.json` maps every selected ID to its disposition, caller, required variants, inspected PNG/hash, exact node or native measurement, and generating method/run receipt. It cites **39 distinct accepted images**. `reviewed-images.json`, `native-measurements.json`, `source-decisions.md` and `reconciliation.json` retain the inspection and scope details. Cumulative capture directories are attributed to the run that generated each image, not the time of a later adb pull.

### Actual production rendering and results

The isolated fixture renders production `PatternViewerScreen` and `LibraryPatternViewerScreen`, their private content/bottom bars/calibration panel, real ViewModels and `KnitToolsTheme`. It reuses `RoomCounterTestFixture`, the completed CounterScreen Hilt Activity/manifest and its text-geometry helper. No duplicate layout or resource gallery was used. Synthetic PDFs contain 1, 2 or 42 small pages. The host omits outer navigation, so this proves the selected owner rendering and native destination, not navigation E2E or release-service behavior.

Device: task-owned `KnitToolsLanguageEn20261018` / `emulator-5580`, Android 16/API 36, 1080×2400px, 420dpi, approximately 411dp, font scale 1.0, app configuration `[en,en_US]`, light production theme, isolated package `com.finnvek.knittools.activepolicytest`. No real documents/accounts or physical device were used. **320dp, 200% font scale, their combination and TalkBack were not run.**

| IDs (EN-V prefix) | Actual caller/state and inspected result |
| --- | --- |
| 0037 | `PatternViewerContent`, controlled PAGE_LIMIT in markup mode: complete 109-character warning, two intact lines in a 1016×100px text node; preserved-annotations sentence and toolbar context visible. |
| 0049 | `RowCalibrationPanel`: Cancel fits beside Save first and Save last in both stages; actual Cancel removes each panel. |
| 0192 | `PatternViewerContent` through the actual project route: a selected document with unavailable PDF displays the complete centered No pattern attached message. |
| 0196 | `PatternViewerContent`: holding the existing renderer IO boundary exposes Loading pattern…; the literal ellipsis is intact. Releasing IO allows the real synthetic PDF to render. |
| 0197 | **N/A** for the exact `rendererError.ifBlank` resource expression; production-path proof below. A real corrupt synthetic PDF separately displays the active nonblank error message. |
| 0206 | `LibraryPatternViewerBottomBar`, labelLarge/semibold: zero loaded pages → Page 1 of 1; actual 1-, 2- and 42-page documents, first/second/last positions. Complete text and arrow context fit. |
| 0207 | `BottomBarNavigationRow`, bodyMedium beside counter/bookmark controls: same caller-valid variants independently rendered. Final stable captures show matching PDF page content and indicator. |
| 0429 | Actual projectContentErrors → native Toast: full Something went wrong — try again, including em dash, within measured toast bounds. Controlled event. |
| 0702 | `PatternViewerContent`: Exporting 0/0, 1/1, 1/2, 2/2, 1/42, 2/42 and 42/42 all fit. Counts are completed pages, not one-based positions; 0/0 is the real startup/preflight state. |
| 0703 | `PatternViewerContent`: full PDF export failed. The original PDF was not changed. fits in one 916×50px node in reading and markup modes. Controlled failure state. |
| 0704,0705 | Actual production preflight and CreateDocument launcher open Android's save UI with pattern-annotated.pdf, Test-annotated.pdf and Élodie’s long cable cardigan-annotated.pdf. Exact native values match. Short names fit; the long field scrolls horizontally, with overlapping start/end images exposing the entire value and suffix. All pickers were cancelled. |
| 1056 | Actual Documents unlink callback receives repository StaleAction from the synthetic stored link ID10 versus controlled stale UI ID900; Could not save web pattern is fully visible in a native Toast. |
| 1061,1065 | Actual Documents Open website callback maps controlled SecurityException / ActivityNotFoundException to Could not open website / No browser available. Complete native Toasts visible after the sheet hides. |

Page values are Ints: the caller adds 1 to zero-based positions and clamps total pages to at least 1. Visible page0 and decimal1.5 are impossible variants, not missing tests. Export callbacks report 1..total after the initial0/0; 0/42 is not emitted by this implementation. No artificial 0/42 or fractional output is accepted.

Controlled state proves error/progress **presentation**, not execution of a real export, storage or browser failure. The actual exporter preflight and system save launcher ran, but **no PDF export write was performed**. The corrupt synthetic PDF did exercise a real renderer-open failure. Related components and ViewModels were used only to reach selected callers; their separately owned pending cases are not accepted.

### EN-V-0197 production-path N/A proof

The selected occurrence is `PatternViewerScreen.kt:1975`, inside `state.rendererError.ifBlank { stringResource(R.string.pattern_open_failed) }`; its branch requires a nonnull error. The private content-state type has only two production construction sites (:469 and :1341). Both pass `rememberPatternRenderState().rendererError` unchanged. That function returns null, the nonblank `patternOpenFailed` resource on open failure (:1424), or the same nonblank resource on page-render failure (:1471). The current English resource is `Pattern could not be opened.` (`values/strings.xml:147`). No production path supplies a blank nonnull error, so this specific fallback expression cannot execute. A fabricated blank state would not prove reachability.

The real corrupt-PDF image `captures-run1/failed-real-pdf.png` shows the active nonblank branch, not the selected fallback. No missing fixture or unfinished work was labelled N/A; no cleanup of unreachable code was performed.

### Geometry, inspected pixels and actual validation

Every accepted Compose target has an empty measurement issue list and was paired with inspected pixels. The reused helper retains actual bounds/size, constraints, paragraph width, per-line extents/end, the existing center-coordinate correction, parent clipping, horizontal/vertical overflow, max-lines truncation, ellipsis and incomplete-text checks. No detector was removed or weakened. Nonvisible horizontally scrollable toolbar items and the hidden Documents sheet remain flagged in raw captures; those nodes receive no approval here.

Native Toast text/surface pixel bounds and Android save-field text/bounds are recorded separately in `native-measurements.json`; they are not presented as Compose geometry. The long native filename retains its full value and uses normal editable-field scrolling. Both ends were inspected at full resolution. No English clipping/truncation defect was confirmed.

Two run1 project page images were captured while the asynchronous bitmap still showed the previous page despite the updated indicator. They were rejected as stable-page proof even though the test passed. The fixture now waits after changing document state; run4 images show matching page1/page2/page42 content and indicators. This resolved a capture-timing gap, not a confirmed shared implementation defect.

| Executed check | Actual result and retained failures |
| --- | --- |
| `:app:assembleDebug :app:assembleDebugAndroidTest -Pknittools.isolatedTest=activepolicy -I reports/language-audit/en/20261019-patternviewer/fixture.init.gradle --no-daemon` | Builds2–5 succeeded. Build1 failed on fixture-only unavailable UiAutomator imports, PdfDocument lifetime syntax and missing model arguments; corrected using existing Android APIs and models, without adding dependencies. Raw logs and the initial fixture remain. |
| run1, 2026-10-19 03:22:56–03:24:03 +03:00 | Five methods: four passed; projectErrorFeedback failed on duplicate Documents text selection. Initial page-transition captures independently rejected. |
| run2, 03:25:10–03:25:43 +03:00 | projectMissingDocument passed; projectErrorFeedback reached the browser messages but failed on the driver's shortened Unlink pattern selector. |
| run3, 03:26:35–03:27:03 +03:00 | projectErrorFeedback **PASS**, using the actual Unlink pattern information label and clickable-node selection; all four native error messages inspected. |
| run4, 03:29:59–03:30:25 +03:00 | projectIndicatorsAndCalibration **PASS**; stable page/indicator images and both Cancel contexts inspected. |
| Evidence/registry/workspace reconciliation | **14 PASS / 1 N/A / 0 unresolved**. Case images, geometry/native measurements, source snapshots and passing generating methods verified. All prior443 input hashes and unrelated workspace fingerprints preserved. |

There are **six distinct finally passing methods** across the retained runs; earlier mixed runs are not recast as passed. Receipts contain exact instrumentation commands, APK/test/fixture hashes and start/end timestamps; `build-log-times.json` records build-log creation/final-write times. The initial native-metadata reconciliation comparison failed because Python used the host default text encoding; explicit UTF-8 fixed the reader, with no device-data change. No resource/unit suite rerun was necessary because production resources/code did not change.

### Current counts, blockers and resume boundary

Inventory remains **2,315 context/code rows and 1,177 stable rendering IDs**. Dimensions: **T 2,315 PASS; F 2,304 PASS / 11 N/A; L 2,315 PASS; C 2,315 PASS; R 1,879 PASS / 436 N/A; V 1,843 PASS / 246 BLOCKED / 226 N/A.** BLOCKED remains the coverage value for missing evidence, not a claim that every remaining case has an external technical blocker. The remaining registry has **246 cases and no PatternViewerScreen-, CounterScreen- or ProjectListScreen-owned row**.

**Exact blockers for this selected scope: none. Confirmed wording/shared defects: none.** No earlier approval was extended to an unverified context. A future separately authorized task may select another owner after comparing current inputs; do not resume from superseded historical counts or declare the full English audit passed. Stop after PatternViewerScreen.

Both task-started emulator runs and their identified child processes were stopped; no temporary process remains (`process-cleanup.json`, `process2-cleanup.json`). All Gradle commands used a single-use daemon and exited. The existing physical device was not used. No commit, push or pull request was made.

---

## Historical CounterScreen outcome — counts and resume superseded above

**COUNTERSCREEN_COMPLETE. English remains CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING; not AUDIT_PASS.** Every currently unresolved CounterScreen-owned case was processed: **26 selected, 26 PASS, 0 NOT_APPLICABLE, 0 unresolved**. English has **261 pending rendering cases overall**. No English wording or shared implementation defect was confirmed; no production resource/code change was needed. ProjectListScreen remains complete (38 PASS, 4 justified N/A). Original shared findings and LA-en-B08-01 remain verified and unchanged.

This section supersedes historical counts and resume instructions below. All other owners' results, earlier evidence and T/F/L/C/R decisions are preserved. Native-speaker review remains NOT_REQUESTED. The coordinator-owned root campaign table is unchanged; the current English records and remaining registry carry this outcome.

### Frozen selection and current input identity

The current remaining registry was filtered for unresolved `CounterScreen.*` cases before execution, rather than forcing a historical count. The frozen set is:

EN-V-0057, EN-V-0071, EN-V-0073, EN-V-0125, EN-V-0148, EN-V-0156, EN-V-0158, EN-V-0163, EN-V-0184, EN-V-0251, EN-V-0285, EN-V-0287, EN-V-0290, EN-V-0296, EN-V-0412, EN-V-0421, EN-V-0450, EN-V-0590, EN-V-0645, EN-V-0760, EN-V-0870, EN-V-1026, EN-V-1029, EN-V-1053, EN-V-1059, EN-V-1063.

Branch `codex/audit-korjaukset-20261018`, entry HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. Existing dirty/untracked work was fingerprinted and preserved. **All 439 previously recorded input hashes matched at entry and exit.** The manifest additionally pins four final isolated-fixture inputs (test, init script, minimal Hilt host and manifest), giving **443 entries**. Hashes identify inputs; approval is granted only by the case-specific evidence below. No earlier approval became stale through this task.

Evidence root: `reports/language-audit/en/20261019-counter/`. `selected-cases.tsv` freezes the exact entry rows. `actual-callers.json` verifies each current production resource reference. Historical scope names containing `clearPendingWebPatternReplacement` are extraction artifacts: their actual resource lines belong to `ProjectContentHost`; the acceptance map records this without renumbering cases. `observations.json` links every accepted ID to its real caller, required variant, exact visible text, PNG hash, measured node/geometry (or native Toast pixels), generating test method, run receipt and instrumentation log. It cites **32 distinct accepted proof images**, with four additional action/state images in `reviewed-images.json`. Cumulative capture directories are attributed to their actual generating run, not a later pull.

### Production rendering and exact contexts

The fixture renders production `ProjectContentHost`, its actual `CounterViewModel`, dialogs/sheets and `KnitToolsTheme`. It reuses existing `RoomCounterTestFixture`, isolated-package build support and the completed ProjectListScreen measurement solution. An ignored, minimal `@AndroidEntryPoint` Activity supplies the Hilt entry point required by nested production ViewModels; fixture ViewModels use synthetic in-memory Room. No duplicate UI or string gallery is used. The host omits outer app navigation, so this is owner rendering evidence, not navigation E2E or a release-service claim.

Device: task-owned `KnitToolsLanguageEn20261018` / emulator-5580, Android 16/API 36, 1080×2400, 420 dpi, approximately 411 dp, font scale 1.0, `[en,en_US]`, light production theme, isolated package `com.finnvek.knittools.activepolicytest`. No real user projects or accounts were operated on. **320 dp, 200% font scale, their combination, and TalkBack were not run.**

| IDs (EN-V prefix) | Production state and required variants | Inspected result |
| --- | --- | --- |
| 0071,0287,0290 | CounterScreenDialogs Reset title, body and confirmation | Full text, fixed target 0, Cancel/Reset context; actual reset changes isolated count 12 to 0. Final capture has no preceding sheet. |
| 0148,0156,0158,0163 | Delete/Complete confirmation buttons and bodies, Test / Élodie’s long cable cardigan | Exact caller arguments, punctuation, complete wrapping and distinct neutral/destructive actions. Buttons are measured separately from identically worded titles. |
| 0125 | ProjectContentHost → ProjectDetailsSheet title, both names, keyboard closed/open | Full title remains visible above form and actions with the IME present. |
| 0057,0073,0184 | StitchCountDialog Delete, Save and title; empty, 0, 1, 2, 42, existing value, numeric IME | Empty/zero Save disabled; positive values enabled. Save 42 and Delete update real isolated state. All selected text fits. |
| 0590,0645 | Actual CountersListSheet empty state and title | Complete title and empty explanation in their own sheet. |
| 0296 | NotesSheet Add note… placeholder; empty, focused, populated and cleared | Exact literal ellipsis glyph, complete placeholder with IME; populated field correctly hides it. |
| 0251,0412,0450 | YarnPickerSheet title/empty explanation and YarnPickerItem fallbacks 1, 2, 42 | Complete labels and number pairing. Persisted Long IDs cannot be fractional; zero is Room's auto-generated insertion sentinel. Actual Yarn #2 selection links the synthetic card. |
| 0285 | CounterTopBarTitle initial empty-name fallback | My Project is fully visible from the actual owner path. |
| 0421 | ProjectContentHost generic SnackbarHost and native Toast consumers | Exact full sentence/em dash in both surfaces, including separate native Toast pixel measurements. |
| 0760 | ProjectContentHost missing-name fallback inside SessionStartConflictDialog | Full sentence containing another project and Return/Save/Discard/Cancel context; two intact body lines. |
| 0870 | Real folder move event → ProjectContentHost SnackbarHost | Complete Project moved. feedback after closing the move sheet. |
| 1026,1029 | Actual web-pattern replacement dialog; selected pattern names Test / Élodie’s long cable cardigan | Two complete title/body lines, exact name argument, Cancel/Attach to project context. |
| 1053 | Pending replacement pattern deleted before confirmation → real repository PatternMissing | Complete Could not save web pattern snackbar; original attachment remains. |
| 1059,1063 | Actual external-open result mapping for controlled SecurityException / ActivityNotFoundException | Complete Could not open website / No browser available. Production Documents sheet hides before its callback, so feedback is visible on the overview. |

Generic error channels and the session conflict use explicit transient ViewModel inputs; web opener errors use a controlled Context.startActivity exception. These prove production presentation/mapping, not an actual filesystem/database/browser failure. Folder movement, stitch persistence, reset and the missing-pattern replacement use real isolated repository/state operations. Related components were exercised only to reach these selected callers; their separately owned pending cases were not accepted. `source-decisions.md` records the source paths, variant limits and reproduction details. **No N/A disposition or blocker is needed.**

### Geometry, pixel inspection and executed checks

All accepted exact-node measurements have empty issue lists and were paired with inspected pixels. The reused helper retains actual node size/bounds, layout constraints, paragraph width/alignment, raw line extents/end and the existing center-coordinate correction. Parent clipping, horizontal/vertical overflow, ellipsis, max-lines and incomplete text checks remain enabled. The native Toast has separate measured text/background bounds in `toast-pixel-measurement.json`; its system window is not misrepresented as a Compose node. No new measurement discrepancy or application clipping defect was confirmed.

Test success alone did not grant approval: run3's reset image still showed the preceding details sheet and was rejected, then recaptured in run5 after explicit dismissal. `web-open-failed-modal` still contained the previous snackbar, so the failed-open literal is accepted only from `web-open-failed-dismissed`. A filename ending in keyboard is not IME proof: the initial empty-stitch screenshot preceded keyboard appearance; actual IME proof uses the positive-value and existing-value captures. Raw unsuccessful captures remain retained.

| Executed command/run | Actual result and retained failures |
| --- | --- |
| `:app:assembleDebug :app:assembleDebugAndroidTest -Pknittools.isolatedTest=activepolicy -I reports/language-audit/en/20261019-counter/fixture.init.gradle --no-daemon` | Builds 2 and 4–9 succeeded; final build 9 succeeded. Builds 1 and 3 failed on fixture-only DAO-name/syntax errors, corrected without production edits. |
| run1: eight methods | Two passed: stitchEditingAndCounters and sessionAndGenericFeedback. Six host/lazy-list driver failures retained; accepted images only from the two successful methods. |
| run2: MainActivity host experiment | Startup stalled; isolated package deliberately force-stopped. Process crashed log retained; no acceptance. |
| run3: six methods | Five passed: notesAndTitleFallback, yarnSelection, folderMoveFeedback, webOpeningFeedback, projectConfirmationsAndDetails. Web replacement failed on the wrong section selector. Reset image independently rejected despite method success. |
| run4: webReplacementAndSaveFailure | Failed because Test matched background and picker; driver restricted to the clickable row. |
| run5: projectConfirmationsAndDetails, webReplacementAndSaveFailure | **2 PASS**, with inspected final reset, name variants, replacement confirmation and failure feedback. |
| Case/input/workspace reconciliation | **26 PASS / 0 N/A / 0 unresolved**; all original 439 inputs match; nonselected records and unrelated work preserved. |

There are **eight distinct finally passing test methods across run1/run3/run5**, not a claim that the earlier mixed runs passed. APK/test/source identities and snapshots are pinned in each receipt. No production test was weakened. The final source also adjusts timing between web-opener messages; accepted opener proof remains attributed to the passing run3 snapshot. This scope did not need a new broad resource/unit test run because no production resource/code changed.

### Current counts and resume boundary

Inventory remains **2,315 context/code rows and 1,177 original stable rendering IDs**. Dimensions: **T 2,315 PASS; F 2,304 PASS / 11 N/A; L 2,315 PASS; C 2,315 PASS; R 1,879 PASS / 436 N/A; V 1,829 PASS / 261 BLOCKED / 225 N/A.** BLOCKED is the existing coverage value for missing evidence, not proof of a technical blocker in every remaining case. Updated affected batches are pinned in `reconciliation.json`. The remaining registry contains **261 cases, no CounterScreen-owned row and no ProjectListScreen-owned row**.

**Exact blockers for the selected scope: none. Confirmed wording/shared defects: none.** Stop after CounterScreen. A future separately authorized task may select another owner from the current 261 cases after checking its inputs. Do not restart completed audits, reopen unchanged shared findings or declare the entire English audit passed. Task-owned process cleanup is recorded in `process-cleanup.json`; no temporary process is retained. No commit, push or pull request was made.

---

## Historical ProjectListScreen outcome — counts and resume superseded above

**PROJECTLISTSCREEN_COMPLETE. English remains CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING; not AUDIT_PASS.** Processed every current unresolved case owned by ProjectListScreen: **42 selected, 38 PASS, 4 justified NOT_APPLICABLE, 0 unresolved**. English now has **287 pending rendering cases overall**. No new English wording or shared implementation defect was confirmed; no production resource/code change was needed. Original shared findings and LA-en-B08-01 remain verified, unchanged and outside this task.

This current section supersedes the older counts and resume text below. Other owners' results, all earlier evidence and T/F/L/C/R decisions are preserved. Native-speaker review remains NOT_REQUESTED.

### Frozen selection and input identity

The current remaining registry selected these exact IDs before fixtures were created (the count happened to remain 42):

EN-V-0112, EN-V-0115, EN-V-0136, EN-V-0146, EN-V-0147, EN-V-0149, EN-V-0150, EN-V-0151, EN-V-0152, EN-V-0153, EN-V-0157, EN-V-0159, EN-V-0160, EN-V-0168, EN-V-0169, EN-V-0170, EN-V-0171, EN-V-0172, EN-V-0174, EN-V-0176, EN-V-0177, EN-V-0179, EN-V-0180, EN-V-0181, EN-V-0182, EN-V-0845, EN-V-0848, EN-V-0863, EN-V-0869, EN-V-0872, EN-V-0874, EN-V-0876, EN-V-0877, EN-V-0878, EN-V-0880, EN-V-0881, EN-V-0882, EN-V-0884, EN-V-0907, EN-V-0908, EN-V-0911, EN-V-0912

Branch `codex/audit-korjaukset-20261018`, HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. Existing dirty/untracked work was fingerprinted first and preserved. **All 437 previously recorded input hashes matched at entry and exit.** No stale approval was renewed. The input manifest now additionally pins this run's isolated fixture and init script (439 entries); those hashes identify reproducibility inputs, not blanket approval of unrelated UI.

Evidence root: `reports/language-audit/en/20261019-projectlist/`. `selected-cases.tsv` freezes the input set. `observations.json` maps each accepted case to exact caller, variant, literal, inspected PNG/hash, actual text node/geometry and generating run receipt. It cites **43 distinct inspected images**. Cumulative pull directories are not new executions: folder-loading/mutation-error captures are attributed to run1, ordinary/creation/move/count captures to run2, and the final centered-action measurement to run4. `before/` preserves entry reports and registries; `start.json` preserves the existing workspace identity.

### Actual states and rendering observations

Production `ProjectListScreen`, its real `ProjectListViewModel`, in-memory Room and `KnitToolsTheme` were rendered through the existing ComponentActivity/viewModelProvider test seam. No duplicated screen layout or resource-only gallery was used. The host renders the actual owner screen without the outer app navigation bar; this is screen/dialog rendering evidence, not navigation E2E or a release-service claim. Device: task-owned `KnitToolsLanguageEn20261018` / emulator-5580, Android16/API36,1080×2400,420dpi,411dp,font1.0, app configuration `[en,en_US]`, light theme, isolated `com.finnvek.knittools.activepolicytest`. No real user database/account was touched.

| IDs (EN-V prefix) | Exact contexts checked | Observation |
| --- | --- | --- |
| 0112,0115,0136,0146,0147 | Active/Completed sections and empty explanations; counts0/1/2/42 | Both uppercase section-label callers and the shared formatted label were inspected with actual counts. The42-item Completed section was scrolled into view. Empty explanations are fully visible under their own section. |
| 0168–0172,0174 | Overflow selection/sort/show-completed; all three sort selections; checked/unchecked completed toggle | Complete labels and check-mark association, no wrapping/clipping defect. |
| 0151,0176,0177,0863 | Selection title0/1/2/42, Select All, bottom Move/Delete | Values and actions remain distinct; full words visible. Bottom-bar geometry diagnosis is documented below. Fractional selected counts have no caller path. |
| 0149,0150,0159,0160,0179–0182 | Actual bulk-complete/delete headings, confirmation buttons and singular/plural bodies0/1/2/42 | Every title/button occurrence mapped separately. Complete body wraps to two intact lines; delete warning is complete. Count0 is reached by deleting the selected synthetic Room row while the modal is already open, not by inventing an enabled empty-selection action. |
| 0845 | Filtered ProjectListContent loading item | `Loading folders` in the actual Unfiled screen; not borrowed from the separately owned sheet. |
| 0848,0872,0874 | New-project destination Unfiled, Test, Élodie’s long cable cardigan; destination deleted before Create project | Full destination text and accents retained. Actual repository FolderMissing result renders the complete error above the field with keyboard visible. Background selector ellipsis is intentional user-name display and is not the destination-text case. |
| 0876,0878,0880–0882 | Folders-sheet load/reorder error; create/rename/delete folder error | Correct production route and dialog for each literal. Full error text and retry/form/cancel/destructive context inspected. |
| 0877,0911,0912 | Move-sheet load failure; persistence error1 and0/2/42 | Separate caller from folder management. Full error, selected count and Retry context inspected. |
| 0869,0907,0908 | Actual folder-deletion and move success events1/2/42 through SnackbarHost | Full singular/plural messages and both deletion sentences inspected. Empty-set move returns StaleAction, so a successful zero-project snackbar cannot be emitted. |

Transient read/persistence errors were supplied only to the ViewModel's existing `_folderState` through fixture reflection; production code resolved every resource and rendered every surface. This proves the presentation and caller mapping, **not** an injected filesystem/SQLite failure. Ordinary folder moves/deletion and destination-missing creation used real isolated repository operations. Separately owned component/ViewModel pending cases were not approved.

### Four justified N/A cases

- **EN-V-0152, EN-V-0153, EN-V-0157:** the private single-project `DeleteProjectDialog` title, confirmation and message are unreachable. `showDeleteDialog` starts false; its only true assignment is the `onDeleteSwipe` lambda at ProjectListScreen.kt:536–540. The action is declared at:918 but never invoked or forwarded by ProjectListContent/ProjectListItem/ProjectCard (:1074–1106). The private dialog has no other caller. Reachable bulk delete, including quantity1, is separately rendered. No dead-code cleanup was performed.
- **EN-V-0884:** the move-sheet `folder_move_error` fallback at ProjectListScreen.kt:423 cannot be the displayed literal. Read failure has its own branch; PersistenceFailure uses the plural error at:412–416. `ProjectFoldersState.errorResource` maps every other non-success sealed result to a specific resource. `ProjectListViewModel.mutateFolder` sends success only to folderEvents, never mutationError. Thus no production result reaches this fallback. Another owner's separate move-sheet caller remains unchanged.

`source-decisions.md` records the proof and source paths. No missing fixture or unfinished case was labelled N/A.

### Measurement diagnosis and executed checks

No confirmed application clipping defect. The reused semantics helper initially flagged centered bottom actions because GetTextLayoutResult's reconstructed paragraph width was282px while actual Text nodes were76px (Move),139px (Complete),93px (Delete). The raw centered line extents were103–179,72–210,95–187px. Normalizing only TextAlign.Center by `(paragraphWidth - actualNodeWidth)/2` gives0–76,0.5–138.5,0.5–92.5px; all line ends and44px heights fit. Original full-resolution PNGs visibly contain every glyph. Run4 records paragraph width, actual node bounds, alignment, original line extents and normalized result. Other clipping, line-end, ellipsis, maximum-line and parent-bounds checks remain intact. This evidence-helper correction is confined to the ignored fixture; no shared helper or production source was edited.

| Executed command/run | Actual result |
| --- | --- |
| `:app:assembleDebug :app:assembleDebugAndroidTest -Pknittools.isolatedTest=activepolicy -I reports/language-audit/en/20261019-projectlist/fixture.init.gradle --no-daemon` | All four fixture builds succeeded. App APK identity stays the same across the runs; each receipt pins app/test APK plus fixture snapshot. |
| run1: four methods | Folder loading/mutation errors PASS. Three driver failures retained: New Project text finder ignored its content-description semantics; snackbar checked before appearance; activity back dispatcher closed the host instead of dismissing the popup. |
| run2: listMenusAndSelection, creationDestinationsAndMissingFolder, moveErrorsAndSuccessMessages, selectionDisappearsWhileModalIsOpen | **4 PASS**, corrected finders/await/native popup Back and lazy-list scrolling; completed every method. |
| run3 and run4: centeredSelectionActions | Both method executions PASS; run3 retained the misleading horizontal diagnostic, run4 records the corrected coordinate comparison and inspected final pixels. |
| Case/registry/hash/workspace reconciliation | **42 dispositions; 38 PASS / 4 N/A / 0 unresolved.** Exact nonselected records unchanged; all original437 source hashes unchanged. |

There are **six distinct finally passing fixture methods** across the runs; the initial failed runs are not relabelled as passes. No JVM/resource suite was rerun because no resource or production implementation changed; successful Android assembly covers current resource processing. Excluded320dp,200%font scale,their combination and TalkBack were not run. No restricted wrapper, other owner/language work, commit, push or PR.

### Current totals and resume point

Inventory unchanged:2,315 coverage items. **V:1,803 PASS /287 BLOCKED /225 NOT_APPLICABLE.** T2,315 PASS; F2,304 PASS/11 N/A; L2,315 PASS; C2,315 PASS; R1,879 PASS/436 N/A. BLOCKED remains the existing TSV representation of pending rendering, not a claim of an external technical blocker.

Changed batches only: B01 V61 PASS/32 pending/17 N/A (110 rows); B02 V107 PASS/34 pending/32 N/A (173 rows). All other batches keep their previous counts. The remaining registry has287 rows and **no ProjectListScreen-owned row**; all1,177 original stable render IDs remain in render-cases.tsv. **Exact blockers in this task: none.**

Stop here: ProjectListScreen is finished. Resume a future separately authorized task from the287 remaining owner-specific cases, checking their inputs first. Do not restart completed content batches or reopen unchanged verified shared findings. This bounded task does not pass the entire English audit. Task-owned emulator/process cleanup is recorded in process-cleanup.json; no temporary process is retained.

---

## Historical rendering continuation — superseded by ProjectListScreen outcome above

**CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING. Not AUDIT_PASS. Native-speaker review: NOT_REQUESTED.** LA-en-B08-01 is independently **VERIFIED**. Its measurement uncertainty is resolved; the complete no-PDF paragraph and attached-PDF state have evidence. All ten original shared findings and LA-en-B03-04/B19-02/B03-05 retain their independently verified, unchanged decisions. One further confirmed English context error, LA-en-B13-01, was corrected and verified. **No confirmed English text/fact/context defect or material diagnostic uncertainty remains open. Required rendering is still incomplete: 329 cases remain.** This is a continuation result, not a declaration that every executable case has been finished.

This section and the current coverage/remaining-case TSVs supersede earlier status/counts below. Historical failures and observations remain intact. No previous unmodified content batch was restarted; this run inspected the three changed inputs and the outstanding rendering contexts, plus the one newly corrected English value. The coordinator-owned root campaign table is intentionally unchanged and therefore retains the previous snapshot until coordination reconciles this report.

- Scope: English B01–B23 only. Branch `codex/audit-korjaukset-20261018`, entry HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. No commit, push, PR, other-locale edit, shared-code/test edit or user-data operation.
- Entry comparison: all 437 recorded inputs were fingerprinted. Exactly three had changed beforehand: LANGUAGE_AUDIT.md, shared-fixes.md, MentalModelClarityRuntimeTest.kt. `changed-inputs.json` records old/current identities. No resource key, variant or production caller was added or removed. The only production change made here is `tool_result_placeholder` in default English strings.xml. Its final-version rows and all resource-file fingerprints were updated.
- Inventory retained: **1,351 keys; two nontranslatable; 1,349 translatable keys; 1,382 resource variants; 2,315 unique context/code rows** (1,879 resource occurrences, 436 code-backed items). New fixture states are evidence for existing contexts, not duplicate inventory entries.
- Final dimensions: **T 2,315 PASS; F 2,304 PASS / 11 N/A; L 2,315 PASS; C 2,315 PASS; R 1,879 PASS / 436 N/A; V 1765 PASS / 329 BLOCKED / 221 N/A.** BLOCKED in the TSV is the existing dimension value for missing required evidence; the outcome is RUNTIME_PENDING, not an assertion that every remaining case has a technical blocker.
- Of this run's 890 entry cases: **559 now have accepted rendering evidence; 2 are source-proven unreachable; 329 remain pending.** The earlier 1,206 V passes and 219 N/A decisions remain. The prior placeholder V pass was rechecked after its wording changed.
- Project exclusions retained: **320 dp, 200% font scale, their combination, and TalkBack were not run.** Ordinary proof uses API36,1080×2400,420dpi (about411dp),font1.0,en-US,light production theme in isolated package `com.finnvek.knittools.activepolicytest` on emulator-5580. This does not add a dark-theme, accessibility-service or release-service claim.

## LA-en-B08-01 — independent resolution

**VERIFIED, NEEDS_EVIDENCE resolved.** Read the current helper and original/fixed/control receipts, not just the shared summary. All four files in shared `20261019-b08/final-input-hashes.tsv` matched before this run's unrelated placeholder change. SavedPatternDetailScreen and OverviewEmptyText remain unchanged. The no-PDF paragraph is complete: actual node width984, reconstructed semantics paragraph width996, actual line right edge983,70 characters including the final period,44px height. Comparing the reconstructed width against the node caused the old false overflow result.

The corrected helper checks actual node/layout minimum dimensions, line extents/end, ellipsis, maximum lines and parent clipping. Its negative horizontal, vertical and ellipsis controls were reviewed. The first fixed six-method run had five passes and one negative-control test defect; the focused corrected control rerun passes. The savedPdfStates method passes in the fixed run and verifies both states, including the formerly unreached attached-PDF state. These are six distinct finally passing methods, not a claim that the initially failing run passed.

Independently inspected shared `fixed-images/api-36-ravelry-no-pdf.png` and attached-PDF image. **EN-V-0621,0617,0622** now pass with pinned shared evidence in observations.json. The latter two also appear in this run's supplied Ravelry/offline detail state. These captures prove the labels and paragraph, not successful network download or reading a real PDF from the synthetic URI. The historical BLOCKED diagnosis below is superseded only for these stated cases.

## LA-en-B13-01 — misleading guidance for filled invalid gauge input

**CONFIRMED_DEFECT; LANGUAGE/CONTEXT; S3; VERIFIED.** `GaugeComponents.GaugeResults:143` uses `tool_result_placeholder` whenever result sections are empty. The actual parser rejects filled negative/nonnumeric input too, so the former **“Fill in the fields to see the result”** was inaccurate in those states. Changed only that English value to **“Enter valid values to see the result”**. No new resource, formatting argument, parser, calculation or shared behavior changed.

The initial gauge fixture exposed the mismatch. The final `gaugePlaceholderStates` uses the real MeasurementNumberParser and production GaugeContent for empty input, `-1`, and `abc`. All three final states were captured and read; the field error and corrected guidance fit together. Final proof is in `captures-group5/gauge-fixed-*.png/.json`, group5 receipt and `review-panels/gauge-fixed-01.png`. The fixture supplies parsed state; it does not claim to have driven every ViewModel input event. Final assembly passes; ten targeted resource tests executed afresh and pass. Existing B14-03 corrections remain intact. Other-language owners may independently examine their equivalent guidance; no English shared-code handoff is required.

## Exact rendering evidence and reuse boundaries

Evidence root: `reports/language-audit/en/20261019-render-continuation/`. **observations.json is the authoritative per-case acceptance map**: stable EN-V ID, original key/variant/caller/requirement, exact visible text, raw PNG SHA-256, node ID, clean actual geometry, generating run receipt and scope notes. It cites 242 distinct PNG files for 559 newly accepted cases. `reviewed-images.json` and contact-panel indexes record inspected images; image review alone does not approve every background node. `candidate-map.json` is only a search aid and is superseded by observations.json. No bulk approval based on matching words or test exit status was used.

Production composables and KnitToolsTheme rendered controlled states. One checked state covers multiple rows only when their actual owned text is present. Examples of deliberately separate contexts: document heading vs information button; unlink link vs dialog title vs confirmation button; annotation Edit text action vs edit-dialog title; folder count1 title vs count2 plural; Web Pattern attach action vs replacement confirmation; actual singular/plural duration output. Obscured modal background, off-screen/clipped nodes, unmatched caller occurrences, group1 unhosted image-import layout and uninspected captures do not receive approval.

The shared session error evidence is narrowly justified: CounterViewModel's recorded producers call showWorkSessionError, which assigns workSessionErrorRes and retry presence; CounterScreen:982–988 resolves these through WorkSessionErrorDialog. Each message and corresponding Retry/OK layout is inspected separately. Rendering reuse does **not** claim that storage failure, history limits, recovery, purchase, restore or network behavior was executed. Chart-direction/tracking/corner helper labels are mapped to their actual chart-dialog radio choices, including the distinct C2C corner state.

The fixtures cover bookmark states/errors; backup phases/preview/errors/destructive confirmation; hosted image-import phases/errors; session conflicts/completion/deletion/recovery/errors/durations; Web Pattern forms/errors/incoming/replacement; Pro prompt source/trial variants; annotation tools/layers/text/callout/chart forms; yarn usage units/errors/source states; counter/reminder forms/menus/confirmations; measurement/gauge result modes and units; folders/delete counts/move/empty states; incoming PDF choices; step-size labels/errors; reader menus/page jump/documents; saved-pattern details/actions/errors; My Yarn and yarn details; project overview sections; Insights trends/durations/relative dates/completion history; Library and counter history. The fixture snapshots specify each actual value, and the observations map is the exact accepted subset. This list does not approve untouched route states with similar names.

## Newly justified N/A and outstanding cases

Only two new N/A decisions were made; both are source-proven English plural branches, with T/F/L/C/R retained:

- **EN-V-0496**: Insights best-streak trend requires currentStreak>0 and bestStreak>1; English quantity1 cannot reach the plural. Reachable quantity12 has separate rendering.
- **EN-V-0909**: MoveToFolderSheet handles projectCount1 with singular title resources before its plural else branch; `folder_move_selected_projects_count[one]` cannot be selected. Count1 alternatives and count2 plural remain separately evidenced.

`new-na.json` has the exact functions/guards. Missing evidence did not become N/A. The existing nonvisual/semantics-only decisions remain unchanged; none is a TalkBack pass.

**The current `reports/language-audit/en/20261019-recheck/remaining-render-cases.tsv` contains exactly 329 unresolved cases.** Every row retains its literal/variant, current caller and precise required state, plus a current pending reason. `render-cases.tsv` preserves all1,177 original stable IDs and accepted/N/A history. Before-copies prevent the new result from replacing prior work unnoticed.

There is no demonstrated general device/build blocker: the isolated emulator and fixture builds worked. Much of the remaining work is **not yet executed** (route/ViewModel-host dialogs, other form variants, clipboard output and generated cases). It must not be described as impossible or passed. The ComponentActivity fixture does not instantiate the complete Hilt/navigation route host; those exact host states need a suitable fixture. Empty Web Pattern required-field attempts left Save disabled and did not show the requested error messages; those cases remain open pending a reachable-state trace, not N/A. Actual Play billing and authenticated Ravelry operations were unavailable in this no-network isolated package; controlled text rendering is a separate obligation and remains pending where not evidenced. No purchase, login, network or actual backup replacement was attempted.

Remaining contexts by source owner (exact IDs/requirements are in the TSV):

| Owner | Pending rows |
| --- | ---: |
| ProjectListScreen | 42 |
| CounterScreen | 26 |
| InsightsScreen | 15 |
| PatternPickerSheet | 15 |
| PatternViewerScreen | 15 |
| ProUpgradeViewModel | 10 |
| AllPhotosScreen | 9 |
| NotesEditorScreen | 9 |
| ProjectListViewModel | 9 |
| CastOnScreen | 8 |
| CounterWorkspaceSections | 8 |
| MainCounterDisplayText | 8 |
| MainActivity | 7 |
| PhotoGalleryScreen | 7 |
| ProUpgradeScreen | 7 |
| ProjectOverviewScreen | 7 |
| SettingsScreen | 7 |
| PasteInstructionButton | 6 |
| ProjectCardText | 5 |
| YarnCardDetailScreen | 5 |
| YarnManagementSheet | 5 |
| IncomingPdfHost | 4 |
| IncreaseDecreaseScreen | 4 |
| PhotoComponents | 4 |
| RavelryBrowserScreen | 4 |
| SessionHistoryScreen | 4 |
| SettingsViewModel | 4 |
| CounterViewModel | 3 |
| CounterWidget | 3 |
| GaugeViewModel | 3 |
| MyYarnScreen | 3 |
| NumberInputField | 3 |
| PatternCard | 3 |
| ProjectDetailsSheet | 3 |
| ProjectOverviewSections | 3 |
| SizeChartScreen | 3 |
| WebPatternEditorScreen | 3 |
| app/src/main/java/com/finnvek/knittools/domain/calculator/CastOnCalculator | 3 |
| ContinueProjectCard | 2 |
| CounterWidgetState | 2 |
| GaugePresentation | 2 |
| MoveProjectToFolderSheet | 2 |
| ProjectFolderComponents | 2 |
| ProjectPickerSheet | 2 |
| SavedPatternsScreen | 2 |
| SkeinCountText | 2 |
| TargetRowsDialog | 2 |
| YarnWeightField | 2 |
| counter_widget_info | 2 |
| AbbreviationsScreen | 1 |
| AndroidManifest | 1 |
| BackupScreen | 1 |
| CounterProjectContentCards | 1 |
| GaugeScreen | 1 |
| IncomingPdfViewModel | 1 |
| InfoTip | 1 |
| InsightsChart | 1 |
| LibraryTopBar | 1 |
| MultiCounterComponents | 1 |
| NeedleSizeScreen | 1 |
| PatternAnnotationModeUi | 1 |
| SavedPatternRepository | 1 |
| StitchCounter | 1 |
| YarnUsagePresentation | 1 |
| YarnUsageRow | 1 |
| app/src/main/java/com/finnvek/knittools/domain/calculator/LocaleNumberFormatter | 1 |
| app/src/main/java/com/finnvek/knittools/domain/calculator/YarnEstimator | 1 |
| app/src/main/java/com/finnvek/knittools/domain/model/PatternDisplayNames | 1 |
| widget_counter_preview | 1 |

## Executed validation and retained failures

Temporary evidence fixture only: `src/EnglishRenderContinuationTest.kt`, compiled through `fixture.init.gradle`; no production or shared test source-set change. Each group receipt pins app/test APK and a saved fixture snapshot. Pull directories are cumulative; observations attribute identical files to the earliest generating receipt, rather than pretending later runs re-executed earlier methods.

| Run | Selected methods | Actual result |
| --- | --- | --- |
| group1 | backupStates, imageImportStates, bookmarkStates | PASS 3 |
| group2 | imageImportStates, sessionStates, webEditorStates, annotationStates, proPromptStates | PASS 5 |
| group3 | yarnUsageStates, counterReminderStates, gaugeStates | PASS 3 |
| group4 | folderAndIncomingStates, mainCounterStepStates, documentAndActionStates, viewerMenuStates | FAIL 1/4 |
| group5 | documentAndActionStates, gaugePlaceholderStates | PASS 2 |
| group6 | supplementalDialogStates, savedPatternDetailStates | PASS 2 |
| group7 | myYarnStates, overviewSectionStates, insightsSectionStates | PASS 3 |
| group8 | libraryAndHistoryStates, yarnManagementStates, yarnDetailStates | PASS 3 |
| group9 | finalStatusStates, libraryAndHistoryStates | PASS 2 |
| group10 | finalVariantStates | PASS 1 |

There are 25 distinct selected fixture methods. Group4's document driver called performScrollTo on a visible control outside a scroll container and failed; group5 corrected only the driver and reran the complete method successfully. Initial fixture source wiring/compilation errors and group1's unsuitable image host remain in logs; no failed build/run is accepted as final validation. The history fixture's first supplied decrease sample had inconsistent synthetic values; group9 supplies12→11 and is the accepted history image. Synthetic display-state inputs are not production data or calculator-source defects.

Fresh focused command: `:app:testDebugUnitTest --rerun --tests '*ReferenceResourceLocalizationTest' --tests '*CountPluralResourcesSourceTest' --tests '*CounterStringResourcesSourceTest' -Pknittools.isolatedTest=activepolicy --no-daemon`: **10 tests,0 failures,0 errors,0 skipped**, three XML suites copied to unit-results/. `resource-tests.log` records BUILD SUCCESSFUL and the test task executed. This existing resource suite reads parity across locales but does not audit or modify other languages. Earlier UP-TO-DATE output was not substituted for this execution. Final app/test assembly also passed; current receipt pins app SHA-256 `5990941054285bca0c86ff46919d3d8e1c717095f0ade2e1d78327869d586d47`.

## Historical batch tracker — before ProjectListScreen

All applicable T/F/L/C/R decisions remain complete, with the scoped final English correction above. A batch with pending V is not a full pass.

| Batch | Rows | V PASS | V pending | V N/A | Outcome |
| --- | ---: | ---: | ---: | ---: | --- |
| B01 | 110 | 59 | 34 | 17 | RUNTIME_PENDING |
| B02 | 173 | 71 | 74 | 28 | RUNTIME_PENDING |
| B03 | 115 | 56 | 31 | 28 | RUNTIME_PENDING |
| B04 | 64 | 59 | 5 | 0 | RUNTIME_PENDING |
| B05 | 160 | 118 | 21 | 21 | RUNTIME_PENDING |
| B06 | 142 | 85 | 15 | 42 | RUNTIME_PENDING |
| B07 | 78 | 67 | 6 | 5 | RUNTIME_PENDING |
| B08 | 123 | 89 | 29 | 5 | RUNTIME_PENDING |
| B09 | 89 | 62 | 17 | 10 | RUNTIME_PENDING |
| B10 | 85 | 50 | 5 | 30 | RUNTIME_PENDING |
| B11 | 80 | 68 | 6 | 6 | RUNTIME_PENDING |
| B12 | 52 | 23 | 23 | 6 | RUNTIME_PENDING |
| B13 | 107 | 94 | 10 | 3 | RUNTIME_PENDING |
| B14 | 39 | 25 | 11 | 3 | RUNTIME_PENDING |
| B15 | 25 | 15 | 10 | 0 | RUNTIME_PENDING |
| B16 | 19 | 18 | 1 | 0 | RUNTIME_PENDING |
| B17 | 377 | 370 | 0 | 7 | Applicable checks complete |
| B18 | 235 | 235 | 0 | 0 | Applicable checks complete |
| B19 | 83 | 81 | 0 | 2 | Applicable checks complete |
| B20 | 39 | 36 | 2 | 1 | RUNTIME_PENDING |
| B21 | 30 | 30 | 0 | 0 | Applicable checks complete |
| B22 | 82 | 50 | 26 | 6 | RUNTIME_PENDING |
| B23 | 8 | 4 | 3 | 1 | RUNTIME_PENDING |

## Historical closeout and resume point — before ProjectListScreen

Final input hashes pin437 files; hashes identify content, not approval. The preserved entry manifest, scoped diff check, exact reverse-replacement check, resource/coverage reconciliation and process cleanup are recorded in `final-validation.json`. Only the English value and English audit records change outside ignored raw evidence. No commit, push or PR. The task-owned emulator is stopped after evidence collection; unrelated processes are untouched.

Resume from the current remaining-render-cases.tsv, state by state. Reuse observations.json and prior still-valid evidence; do not restart completed B01–B23 content review. Remaining required rendering prevents AUDIT_PASS under LANGUAGE_AUDIT.md§16. Native-speaker review remains **NOT_REQUESTED**, not completed or implied.

---

## Historical follow-up snapshot — superseded by the rendering continuation above

**REVIEW_COMPLETE / OPEN. Not AUDIT_PASS. Native-speaker review: NOT_REQUESTED.** All ten original shared findings and the three follow-up findings LA-en-B03-04, LA-en-B19-02 and LA-en-B03-05 are independently VERIFIED for English. Two newly confirmed English placeholder errors are corrected and verified. No confirmed English text/fact/context defect remains open. There is one material rendering-validation uncertainty, LA-en-B08-01, and required ordinary rendering evidence is still incomplete.

The current figures and decisions in this section supersede all earlier snapshots below. Prior evidence, corrections and unsuccessful runs remain retained as history; they are not silently turned into passes.

- Scope: English B01–B23 only, default `values` resources and their current consumers. No translation approval, other-language edit, commit, push or PR. Branch `codex/audit-korjaukset-20261018`; HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`.
- Read LANGUAGE_AUDIT.md, shared-fixes.md, English review/coverage/input hashes, AGENTS.md, CODEX.md, CLAUDE.md and the affected implementation/tests. Compared all 428 prior inputs: 11 had changed before this follow-up. The complete drift list and before-copies are in `reports/language-audit/en/20261019-recheck/`. All 12 final shared-fix input fingerprints matched the actual sources; the five relevant Android method receipts and 25 JVM XML files were read independently.
- At entry, **zero resource variants had changed, been added or removed** since the preceding English review. The shared changes affected the Insights producer, two chart drawing branches and tests. Existing contexts cover those changes; the historical-unit generated case is updated to the current four-label/two-filter regression. This chat subsequently changes exactly `increase_result_placeholder` and `decrease_result_placeholder`. No new resources or production callers were introduced. Final hashes pin **437 input files**; hashes identify versions, not approval.
- Inventory remains **1,351 keys, two explicitly nontranslatable; 1,349 translatable keys; 1,382 inventoried resource variants**. Coverage remains **2,315 distinct rows**: 1,879 resource occurrences and 436 code-backed items. The resource inventory equals coverage, including plural branches and materially different callers.
- Final dimensions: **T 2,315 PASS; F 2,304 PASS / 11 N/A; L 2,315 PASS; C 2,315 PASS; R 1,879 PASS / 436 N/A; V 1,206 PASS / 890 BLOCKED / 219 N/A**. No current FAIL is concealed: the prior five factual failures and nine context failures were resolved by the specifically verified fixes. The new PDF discrepancy is NEEDS_EVIDENCE and remains BLOCKED, not a confirmed clipping defect.
- Of the **1,177 entry V=BLOCKED rows**, 145 are proven nonvisual, six are proven unreachable in the current production flow, 136 now have exact contextual rendering evidence, and 890 remain visible cases without adequate acceptance evidence. Of those 890, **889 are pending evidence, one has conflicting evidence**. These are row/context counts, not 890 necessarily unique screens.
- Only the two English values and the three English audit records were edited. Reversing these two value replacements in memory reproduces the entry strings.xml SHA-256 exactly, including line endings. Every other fingerprinted file is byte-preserved. One tracked non-ASCII plan path omitted by the entry snapshot's Git quoting remains HEAD-clean with its pre-task modification time; it was not edited. No shared Kotlin, tests, data, project instructions or coordinator-owned report changed.
- `EXCLUDED_BY_PROJECT_RULE`: 320 dp, 200% font scale, their combination and TalkBack. None run. Accessibility text retains language, meaning and caller review; V=N/A is not a spoken-output or accessibility-service pass.

## Independent verification of the three latest shared fixes

### LA-en-B03-04 — historical units: VERIFIED

Current `InsightsViewModel` no longer derives historical unit validity from today's mutable project label. It unconditionally supplies `rowMetricsAvailable=false` and `MinutesPerRowDisplay.Unavailable`. `InsightsStatsRow` requires the row flag before showing either a row total or pace. Time, session records and current counter progress remain available. This is a truthful suppression of an unproven unit; it does not infer that past counts were rows.

The actual `InsightsCounterUnitsRuntimeTest` uses Room, the production repository and ViewModel, and a controlled session clock. It records a ROUNDS session from10 to20 over61 seconds, then changes the current label through ROUNDS, ROWS, REPEATS and CUSTOM. Both all-project and selected-project filters assert absent historical rows/pace, two display minutes, and unchanged saved duration/count data. The session-history range remains neutral, `Counter: 10 → 20`; the current counter can legitimately say Row20. The independently viewed `shared-historical-units-false.png` agrees.

The first shared run failed because its expected displayed minute count was one; actual ceil(61/60)=2 is correct. The corrected isolated method passes in `instrumentation-2.log` (3.375 seconds); the first failure is retained. Current input hashes exactly match that run's final manifest. This correction closes the remaining scope of original LA-en-B03-02. Unreachable row/pace labels receive N/A with the production guard as evidence; the actual transition case receives PASS from the real regression and inspected rendering.

### LA-en-B19-02 — KFB and P2tog: VERIFIED

Current `ChartSymbolScreen` draws KFB with an upright stem at x=.55 from y=.25 to.75 and an upper-left branch from(.3,.3) to(.55,.5). P2tog's dot is centered at(.35,.35), radius.06, separated above-left from the slash. I compared these against the actual illustrated alternatives on [CYC Knitting Chart Symbols](https://www.craftyarncouncil.com/standards/knit-chart-symbols), reopened on2026-10-19, and the retained source-image extracts `cyc-kfb.png` and `cyc-p2tog.png`.

The first CYC KFB alternative and second P2tog alternative support the chosen shapes. The complete production screens `shared-corrected-KFB.png` and `shared-corrected-P2tog.png` show the ordinary glyph alongside its independently read English name/operation and right-side convention. The current32dp light/dark matrices `shared-charts-false.png` and `shared-charts-true.png` also show distinct dot/stroke and branch/stem. An image-difference assertion was not used as a technique or terminology authority. This closes the remaining scope of LA-en-B19-01; the unchanged other22 symbol entries retain their individual earlier source review.

### LA-en-B03-05 — stale stop-summary assertion: VERIFIED

The actual ordinary method `WorkSessionComposeTest.stopSummaryRequiresExplicitSaveDiscardOrCancel` now asserts `Counter progress: 12` for3661 seconds/12 counter increments. Title, `1:01:01`, Save, Discard, Cancel and their callbacks remain checked. Its passing receipt is in shared `instrumentation-1.log`. This repairs the test's obsolete row assumption; it is not a new app wording change. No whole-class execution or excluded large-font method was used.

### All ten original shared findings

| Original finding | Current English decision and evidence boundary |
| --- | --- |
| LA-en-B14-01 | VERIFIED. Existing independent M1 expansion of4,998 sequences/2,499 valid inputs and151 rejections retained; renderer/algorithm unchanged. Distinct flat decrease and circular increase/decrease96±12 cases newly inspected. |
| LA-en-B15-02 | VERIFIED. Existing unrounded-nearest-repeat arithmetic and12.4cm→16st rendering retained; no repetition of the accepted midpoint probe. |
| LA-en-B17-02 | VERIFIED. All38 current needle rows/152 cells and all16 JP mappings retain independent Clover evidence; current title/search/disclaimer/header consumers now explicitly mapped to the existing screenshot. |
| LA-en-B17-03 | VERIFIED. Existing46 size rows/123 metric measures/58 ranges and conversion evidence retained; corrected arm endpoint and data unchanged. |
| LA-en-B19-01 | VERIFIED, completed by B19-02 above; all24 current operation/glyph pairs have individual source review. |
| LA-en-B03-02 | VERIFIED, completed by B03-04 above; neutral history does not claim a unit and historical row/pace claims are suppressed. |
| LA-en-B03-03 | VERIFIED. Existing four widget dimensions and receiver-write result retained; widget renderer unchanged. |
| LA-en-B06-03 | VERIFIED by removal. Eight canonical thickness labels retained, unsupported regional ply hints still absent. Removal is not factual endorsement of those hints. |
| LA-en-B06-04 | VERIFIED. Existing20 independently checked care glyphs and visible-label/shared-layout evidence retained; no new TalkBack claim. |
| LA-en-B16-03 | VERIFIED. Existing450m/(200m per skein)=2.25; purchase3×50g=150g calculation and visible whole-skein wording retained. |

## New English correction and shared uncertainty

### LA-en-B14-03 — filled invalid inputs were described as empty: VERIFIED

- Classification CONFIRMED_DEFECT; category LANGUAGE/CONTEXT; severity S3; state VERIFIED.
- Keys: `increase_result_placeholder`, `decrease_result_placeholder`, `app/src/main/res/values/strings.xml`. Consumer: `IncreaseDecreaseScreen.resultPlaceholderRes`, both incomplete and rejected-input branches.
- Reproduction: actual current0/change12, both fields visibly filled. `MeasurementNumberParser` rejects zero and returns no value; the screen shows its null-result placeholder. The old “Fill in both fields to see where to increase/decrease” misdescribed this state.
- Final: “Enter valid stitch counts to see where to increase” and “…decrease”. This is the smallest English-only correction covering both empty and invalid input without inventing a computation result or changing the parser/algorithm. No placeholders or formatting arguments added.
- Final app and test APK assembly PASS; ten targeted resource tests PASS. Final installed app: empty increase state, actual0/12 increase state, and actual0/12 decrease state separately captured/read; all final text fits. `final-increase-empty.png`, `final-increase-invalid.png`, `final-decrease-invalid.png`.
- Other-language owners should examine their equivalent null-result guidance independently. No shared behavior change is required for the English wording correction. The domain nonpositive-error strings are currently unreachable from this screen; that is documented separately as V=N/A, not a display claim.

### LA-en-B08-01 — no-PDF explanation has conflicting rendering evidence: BLOCKED

- Classification **NEEDS_EVIDENCE**; category RENDERING; severity S3 if clipping is established; state BLOCKED. This is a precise shared diagnostic handoff, not a confirmed English wording error.
- Resource/context: `saved_pattern_detail_no_pdf_explanation`, `SavedPatternDetailScreen.RavelrySourceSection` → `OverviewEmptyText`; case **EN-V-0621**. English meaning remains correct: “The pattern details are saved. No PDF is attached for offline reading.”
- Ordinary API36,411dp,font1.0,en/light; synthetic Ravelry metadata with a long title and no local PDF. `MentalModelClarityRuntimeTest.savedPdfStates` scrolls to the paragraph, captures it and fails `assertReadable` at line246 because a returned TextLayoutResult reports `hasVisualOverflow`. The same method fails again on an isolated rerun. Raw logs: `clarity-instrumentation.log`, `pdf-retry-instrumentation.log`.
- Independently viewed `clarity/api-36-ravelry-no-pdf.png` appears to contain the entire sentence, including the final period. Neither the image nor the failing generic assertion alone resolves the contradiction. `OverviewEmptyText` has no maxLines cap. Existing `GaugeScreenTest.assertTextDoesNotClipHorizontally` documents a Compose semantics-measurement discrepancy and checks actual node/line geometry, but that is a diagnostic lead, not proof that this failure is false.
- Shared owner action: capture this node's actual size/constraints, each line's left/right/end/ellipsized state and paragraph bounds on the ordinary configuration; compare them to the production screenshot. Fix actual clipping if present, or correct the unsupported assertion if measurement evidence establishes a test defect. Keep the full English paragraph and strict readability checks. Rerun this exact method and reach its attached-PDF second state, which both failed runs never reached. Do not resolve it by removing the assertion or marking the screenshot PASS.
- No shared test/renderer edited in this English-owned chat. All other locales share the component, but no other-language failure is inferred. This material uncertainty prevents AUDIT_PASS.

## V classification, exact case list and evidence reuse

All1,177 formerly blocked rows were first assigned stable **EN-V-0001…EN-V-1177** IDs and traced to their current consumers before new rendering runs (`classification-complete.json`). The first classification was145 nonvisual, three unreachable,1,029 visible. Exercising the zero-input state and reading its complete producer path subsequently proved three more unreachable cases; final classification is145 nonvisual/six unreachable/1,026 visible.

**Durable exact case registry:** [coverage.tsv](coverage.tsv). Every formerly blocked row now includes its EN-V ID, key/variant, exact caller, visible or nonvisual rationale and its evidence or still-required state in `result_note`. The detailed execution register is `reports/language-audit/en/20261019-recheck/render-cases.tsv`; its **remaining-render-cases.tsv contains exactly890 unresolved visible cases** with owning function, source trigger, text, proposed inputs and result. Proposed input values are requirements only, not claims of execution. The exhaustive membership is the TSV, not the illustrative groups below.

N/A does not discard language review. The151 newly N/A formerly blocked rows comprise:

| Current reason | Rows | Verification |
| --- | ---: | --- |
| Accessibility/semantics-only consumers |134| Actual Icon contentDescription, stateDescription, semantic click actions, spoken value, custom-action label or equivalent sink checked. Visible uses of the same resource remain separate. |
| Resource-ID comparisons |9| Equality/when cases select tint, status class or a different semantic resource; these occurrences do not resolve/render text. Actual text-emitting uses remain in coverage. |
| Framework resource false match |1| EN-V-0056 uses `android.R.string.ok`, not the app-owned key; actual app OK cases remain separate. |
| Clipboard metadata |1| EN-V-0913 is the first ClipData label argument; copied result content retains separate required review. |
| Historical pace unreachable |3| EN-V-0498/0499/1175: production flag and unavailable pace guard; actual Room regression verifies absence. |
| Calculator invalid-message fallback unreachable |1| EN-V-0333: the private result component only consumes calculate(); every invalid result is constructed with a non-null message, so fallback `invalid_input` is not reached. |
| Calculator nonpositive domain messages unreachable |2| EN-V-0337/0338: parser rejects nonpositive input before calculate(); actual0/12 produces the separately reviewed placeholder. |

The final219 V=N/A rows additionally include64 prior justified N/A rows and four newly unreachable historical Rows/Min per row occurrences (two primary, two transition contexts). These four follow the changed production guard; two were previously visual PASS and two were FAIL. No unreachable message is claimed to have rendered. `web_pattern_unlink_content_description` at its **visible confirmation body** remains a required visual case (EN-V-1039); its name is not grounds for N/A.

The136 newly supported cases use only inspected exact screenshots and traceable current callers. Repeated drawing/layout evidence covers exactly the named callers in the evidence map below. It does not approve other warnings, destructive confirmations, plural branches or different input-generated sentences. For example, two-pattern deletion covers `other=2`, not singular; the named web-pattern deletion and generic saved-pattern removal each have their own inspected image; folder deletion count2 does not prove the singular preservation message. Old unchanged screenshot evidence was reused without rerunning accepted factual audits.

### Remaining visible cases and actual blockers

- **889 RUNTIME_PENDING cases:** no adequate exact-case rendering evidence yet. They remain V=BLOCKED under the schema. This is missing work/evidence, not an assertion that the emulator or all cases are technically blocked. They include remaining local save/conflict/errors, project and folder variants, distinct incoming PDF/image/document/annotation/export states, session recovery/range/time forms, calculator helper/error/result variants and backup phases. Exact keys, inputs and callers are in the registry.
- **One material evidence conflict:** EN-V-0621 / LA-en-B08-01 above, requiring shared renderer/test diagnostics. The failure also prevented the second attached-PDF state in that test from supplying evidence; those separate rows remain pending.
- Actual external-service state boundaries remain: no real Google Play price/purchase/restore/update delivery or authenticated Ravelry download was driven. The isolated sideload is not a Play delivery fixture; an ordinary visible loading state is not evidence of any later billing result. No real purchase is required by LANGUAGE_AUDIT.md. Missing controlled state fixtures or external outcomes must not be replaced by a blanket pass.
- The new WebPatternEditor, session and backup component tests prove only their explicitly asserted states/actions. Tests that merely assert display presence, or use plain MaterialTheme instead of production KnitToolsTheme, do not by themselves close the distinct full-paragraph/font/wrapping cases. They are recorded as validation, without promoting unrelated rows.

## Current validation log

Raw root **E19**: `reports/language-audit/en/20261019-recheck/`. Shared root **SH19**: `reports/language-audit/shared/20261018-followup/`. These ignored directories are local evidence, not new tracked infrastructure.

- Ordinary owned AVD `KnitToolsLanguageEn20261018`, serial`emulator-5580`, Pixel6/Android16/API36,1080×2400px,420dpi,411dp width,font1.0, light theme unless named shared light/dark evidence. `am get-config` confirms system en-US; Android per-app locale API confirms `[en]`. Synthetic isolated package `com.finnvek.knittools.activepolicytest`, version1.0.0; physical user device untouched.
- Entry APK SHA-256 `d1fcf03a191b3130050ee878a1053bfda94745b965b1c29f9822ae2494f5c14e`; test APK `011d39fc5acab9078fbb13bc3e61f44466ab89f801d048a02afc58d7b28f857e`. New component runs and pre-copy-edit manual images used these exact shared binaries. The two English placeholder values are the only later production difference.
- Final APK SHA-256 **`7b2325b50b717389974c51da513cc4a7d8f36a9d77c6db1eee17669679cb4e07`**; test APK unchanged. Both installed successfully. `final-*`, `library-launchers.png`, `abbreviations-search-title.png` use the final app; earlier accepted imagery is retained only for unchanged renderers/text. No new binary hash is falsely assigned to an earlier image.
- Shared final validation independently inspected, **not re-executed merely for this audit**:188 JVM tests/25 XML suites,0 failures/errors/skips; selected ordinary Android five distinct methods all have passing receipts (four in the first run, corrected Room method in the second). Scoped ktlint and assembly logs read. Their unchanged source fingerprints match.
- New ordinary Android selection: **24 distinct methods:23 PASS,1 FAIL**; the failing PDF method was rerun once and failed again. Total25 invocations, not25 distinct methods. The four session methods already had earlier passing receipts; their repeat results add no new English rendering coverage. No full class containing excluded tests selected.

| E19 log / class | Exact selected scope | Actual result |
| --- | --- | --- |
| clarity-instrumentation.log / MentalModelClarityRuntimeTest | singleDeletion; webDeletion; bulkDeletion; savedPdfStates; projectYarnEntries |4 PASS; savedPdfStates FAIL,18.640s total. Four successful methods have independently viewed production-theme images. |
| pdf-retry-instrumentation.log | savedPdfStates only |FAIL again,4.734s; no attached-PDF second state reached. |
| folders-instrumentation.log / ProjectFolderComponentsTest | folderSheetExposesVirtualViewsAndBoundaryActions; nameDialogShowsValidationAndOnlySubmitsValidCurrentText; deleteDialogUsesAffectedProjectPluralAndPreservationCopy; moveSheetAnnouncesCurrentDestinationAndMovesSingleOrBulkSelection; createDialogShowsRepositoryDuplicateErrorWithoutSubmitting; emptyFoldersAndUnfiledStateRemainDistinct; unfiledEmptyStateUsesItsOwnLocalizedExplanation |7 PASS,18.874s. Seven distinct images independently viewed. The rename image is the final valid Gifts state, not its earlier51-character validation state. |
| web-instrumentation.log / WebPatternEditorScreenTest | unsupportedUrlShowsValidationAndDoesNotSave; validHttpDraftShowsSourceAndSavesOnlyAfterConfirmation; editEditorShowsExistingDraftAndDuplicateFeedback; incomingDraftRequiresExplicitChoice; incomingDraftCanKeepTheCurrentDraft; projectReplacementRequiresExplicitConfirmation |6 PASS,13.391s; actual synthetic editor states and callback assertions, no blanket visual acceptance. |
| sessions-instrumentation.log / WorkSessionComposeTest | persistenceErrorOffersRetryWithoutClaimingSuccess; anotherProjectStartRequiresExplicitSessionResolution; projectCompletionRequiresExplicitSessionResolution; projectDeletionRequiresExplicitSessionResolution |4 PASS,6.608s; ordinary methods only, plain MaterialTheme fixture, no new production-font paragraph acceptance. |
| backup-instrumentation.log / BackupContentTest | exportSelectionProgressAndSuccessExposeCorrectControls; validatedPreviewRequiresAnExplicitDestructiveConfirmation |2 PASS,4.489s; supplied preview counts2/3/4/5 and explicit confirm callback. No real backup replacement or untested error-state claim. |

After the two English edits, ran `.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --tests '*ReferenceResourceLocalizationTest' --tests '*CountPluralResourcesSourceTest' --tests '*CounterStringResourcesSourceTest' '-Pknittools.isolatedTest=activepolicy' --no-daemon`: **BUILD SUCCESSFUL,1m48s;10 tests/three XML suites,0 failures/errors/skips**. Actual Gradle daemon criteria select Adoptium17. `final-build-tests.log`, `final-unit-results/`, `final-build-identity.json`. No new Kotlin test, dependency or source infrastructure was needed for the two literal changes.

Final verification checks current XML inventory, unique coverage IDs, all recorded hashes, source callers, allowed dimension values, exact unresolved-case membership, finding resolution and two-value inverse fingerprint. `git diff --check` passes; existing CRLF-normalization notices are not errors. All other existing files remain preserved.

Owned emulator PID25392 and QEMU child22244 were stopped with `adb -s emulator-5580 emu kill`; its remaining conhost32684 was stopped only after PID/name/creation-time ownership checks. `process-cleanup.json` records zero remaining owned processes. No persistent server/watcher or test Gradle daemon was left running.

## Current batch tracker

| Batch | Rows | V PASS | V BLOCKED | V N/A | Current outcome |
| --- | ---: | ---: | ---: | ---: | --- |
| B01 | 110 | 27 | 66 | 17 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B02 | 173 | 39 | 107 | 27 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B03 | 115 | 37 | 50 | 28 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B04 | 64 | 26 | 38 | 0 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B05 | 160 | 33 | 107 | 20 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B06 | 142 | 67 | 33 | 42 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B07 | 78 | 35 | 38 | 5 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B08 | 123 | 29 | 89 | 5 | REVIEW_COMPLETE / OPEN |
| B09 | 89 | 2 | 77 | 10 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B10 | 85 | 28 | 27 | 30 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B11 | 80 | 16 | 58 | 6 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B12 | 52 | 19 | 27 | 6 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B13 | 107 | 41 | 63 | 3 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B14 | 39 | 25 | 11 | 3 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B15 | 25 | 15 | 10 | 0 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B16 | 19 | 16 | 3 | 0 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B17 | 377 | 369 | 1 | 7 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B18 | 235 | 235 | 0 | 0 | PASS |
| B19 | 83 | 81 | 0 | 2 | PASS |
| B20 | 39 | 36 | 2 | 1 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B21 | 30 | 7 | 23 | 0 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B22 | 82 | 21 | 55 | 6 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |
| B23 | 8 | 2 | 5 | 1 | CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING |

All applicable T/F/L/C/R reviews are complete. B18 and B19 have complete current visual coverage; every other batch still contains the exact pending rows shown above. A batch's textual review completion is not a visual pass.

## Exact new rendering evidence map

| Exact EN-V cases | Inspected image(s) | Scope and limits |
| --- | --- | --- |
| 0545, 0546, 0547 | `reports/language-audit/shared/20261018-followup/shared-corrected-KFB.png` | Exact current ChartSymbolScreen category headings Increases, Cables and Other are visible in the independently inspected shared screenshot. No library-launcher coverage inferred. |
| 0189, 0190, 0624 | `clarity/api-36-delete-Ravelry.png` | Current SavedPatternDetailScreen Remove pattern title, full Library/project-annotation preservation warning and confirmation button inspected separately with a long synthetic title behind the dialog; singleDeletion passed. |
| 1005, 1007, 1008 | `clarity/api-36-delete-WebLink.png` | Web-pattern-specific deletion title, named warning and confirm action fit. Long Cable cardigan title inserts into three lines. Full distinct preservation wording read; webDeletion passed. |
| 0600, 0601 | `clarity/api-36-delete-bulk.png` | Two selected patterns; complete plural preservation confirmation and title rendered. This covers only other=2, not the singular variant. bulkDeletion passed and its TextLayoutResult had no visual overflow. |
| 0610, 0611, 0614, 0619, 0620, 0623, 0627 | `clarity/api-36-ravelry-no-pdf.png`; `clarity/api-36-delete-bulk.png` | Exact Ravelry source badge/section, Open in Ravelry primary action, Open on Ravelry, Requires Ravelry, availability-unknown badge and Attach to Project text inspected. The separate no-PDF explanatory paragraph is NOT accepted: its TextLayoutResult assertion failed. |
| 0255, 0256, 0258, 0265, 0266, 0267, 0268, 0269, 0986 | `clarity/api-36-yarn-unsaved.png`; `clarity/api-36-yarn-saved.png` | YarnManagementSheet: three synthetic project notes, quantity1, unsaved and savedYarnCardId=20 states. Both distinct copy/preservation explanations read and rendered in full; My Yarn actions and Track usage fit. No form/editor or failure variant inferred. projectYarnEntries passed. |
| 0842, 0852, 0855, 0857, 0859 | `project-folder-component-screenshots/folders-sheet-light.png` | Folders sheet with Personal/Gifts and first-folder action menu: Rename/Delete, disabled Move earlier and enabled Move later all fully visible. ID-comparison-only occurrences remain N/A. |
| 0086, 0851 | `project-folder-component-screenshots/rename-dialog-validation-light.png` | Rename folder dialog, Gifts, Save. The 51-character validation was asserted before editing to Gifts; the screenshot covers only the final valid state. |
| 0854, 0892, 0893, 0906 | `project-folder-component-screenshots/delete-folder-two-projects-light.png` | Personal, assignedProjectCount=2: distinct title, named question, plural preservation body and delete button fully visible. No singular-body coverage inferred. |
| 0846, 0901 | `project-folder-component-screenshots/move-to-folder-light.png` | Single Cardigan project move sheet with current Personal destination and alternative Unfiled/Gifts. Only visible destination label and exact one-project heading accepted. |
| 0849, 0850, 0889 | `project-folder-component-screenshots/create-folder-duplicate-light.png` | Create folder dialog with duplicate Personal; disabled create button and complete duplicate validation shown. Production FolderNameDialog renders supplied repository-error state; this is not an actual repository failure claim. |
| 0864 | `project-folder-component-screenshots/folders-sheet-empty-light.png` | Empty folder sheet shows No folders, separate All Projects/Unfiled choices and Create folder. Other empty destination-sheet occurrence stays pending. |
| 0867 | `project-folder-component-screenshots/unfiled-empty-light.png` | Actual ProjectFolderEmptyState component with Unfiled and no hidden completed projects. Full No Unfiled projects text rendered; no folder-name or hidden-completed variants inferred. |
| 0094, 0095, 0096, 0099, 0328, 0329, 0334, 0335, 0336 | `reports/language-audit/en/20261018-followup/m1-flat96.png` | Previously captured unchanged IncreaseDecreaseScreen, flat 96+12. Full current/change labels and st suffixes, total108 badge, Easy to remember and Balanced captions and both full sequences visually read. Reuses the existing identical source renderer; no new run of the accepted M1 calculation. Partly scrolled Flat/Circular labels are not accepted from this image. |
| 0093, 0349, 0350, 0351, 0352 | `reports/language-audit/en/20261018-followup/caston12-4.png` | Existing unchanged CastOnScreen:12.4cm at10st/10cm,repeat8,edges0. Result16 stitches, actual16.0cm, adjusted-down8st(8.0cm), adjusted-up16st(16.0cm) all fully visible. Covers plural other=16 and these specific metric sentence forms; singular, imperial and separate generated-input cases are not inferred. |
| 0103, 0355, 0356, 0357, 0358, 0359, 0360 | `reports/language-audit/en/20261018-followup/needles-top.png` | Existing unchanged NeedleSizeScreen: title, search placeholder, complete two-sentence approximation/JP-scale disclaimer and mm/US/UK/JP headers read in full. No Library launcher or hidden tooltip inferred. |
| 0072, 0101, 0254 | `reports/language-audit/en/20261018-followup/yarn150g.png` | Existing unchanged YarnEstimatorScreen with450m,200m/50g: title, Choose from My Yarn action and Result caption fully visible. Different My Yarn editor consumers remain separate. |
| 0098, 0100, 0104, 0461, 0462, 0463, 0609, 0613 | `tools.png` | Actual English Tools landing page. Every exact launcher and full description read separately in its own card. No content on the destination or remote Ravelry website inferred. |
| 0324, 0325, 0326, 0327 | `increase-empty.png` | Ordinary screen with empty fields (96/12 are placeholders), both mode and knitting-style selector labels fully visible. This pre-copy-edit image is only evidence for unchanged selector labels. |
| 0634, 0635, 0636 | `flat-circular-help.png` | Distinct Flat vs. circular field caption and opened InfoTip title/body inspected; both paragraphs complete with explicit row-turn versus continuous-round meaning. Not grouped with other tooltips. |
| 0330, 1168 | `flat-decrease-96-12.png` | Actual flat decrease96-12=84. Easy(K6,K2tog)x12 consumes96 and outputs84; balanced sixK6 groups,sixK5 groups,12decreases,finalK6 also consumes96/outputs84. Full multiline instructions and Decrease by visible. |
| 1170 | `circular-decrease-96-12.png` | Actual circular decrease96-12=84; both generated alternatives(K6,K2tog)x12 fully visible with Circular selected. This circular case is separately inspected, not inferred from flat. |
| 1169 | `circular-increase-96-12.png` | Actual circular increase96+12=108; both alternatives(K8,M1)x12 fully visible with Circular selected. Original stitches96,12distinct bars; final M1 uses the across-round-boundary bar. No standalone flat end increase inferred. |
| 0088, 0361, 0362, 0363, 0364, 0365, 0366, 0368, 0372, 0384, 0739, 1084 | `settings-top.png` | Actual Settings screen in English, light theme, app language English. Title, General, Theme and its three choices, language title/current choice/helper, Restore purchases, KnitTools Pro and Backup & Restore fully visible. Loading purchase indicator is not evidence for any billing/trial result. |
| 0367, 0369, 0370, 0371, 0373, 0374, 0375, 0376, 0377, 0378, 0379, 0380, 0381 | `language-sheet.png` | Opened English App language sheet with English selected. Every endonym and Follow system visible in full, including accents. This is review of the English selector only, not a review or selection of another app locale. Shared labelResId English occurrence is also shown here. |
| 0393 | `settings-bottom.png` | Settings footer displays KnitTools 1.0.0 in full, using actual BuildConfig version argument. No store update prompt inferred. |
| 0331 | `final-increase-empty.png`; `final-increase-invalid.png` | Final installed APK after English correction: separately inspected empty fields and actual0/12 inputs. Enter valid stitch counts to see where to increase fits in full and truthfully covers both incomplete and parser-rejected values. No result or zero-validity claim. |
| 0332 | `final-decrease-invalid.png` | Final installed APK, Decrease+Flat with actual0/12 inputs. Enter valid stitch counts to see where to decrease fully visible; source uses exactly the same null-result placeholder for empty and invalid fields. Empty decrease state was separately observed before the wording change; final copy is inspected here. |
| 0102, 0105, 0107, 0109, 0464, 0465, 0466, 0467, 0468, 0469, 0470, 0471, 0472, 0540, 0544 | `library-launchers.png` | Actual English Library landing page on final APK. Three collection cards and all four reference cards, each title and distinct full description, separately read. Counts1pattern,1yarn,2photos; no hidden destination states inferred. |
| 0539, 0541 | `abbreviations-search-title.png` | Actual Abbreviations screen on final APK, empty query. Full screen title and Search abbreviations placeholder read; previously accepted individual abbreviation definitions are retained rather than re-audited. |
| 0273, 1121, 1122 | `reports/language-audit/en/20261018/runtime-final/p1198_yarnDetailQuantityStatusAndProject/yarn-details.png` | Existing unchanged YarnCardDetailScreen screenshot: Quantity with8skeins, Linked project Edit and Details Edit actions, each fully visible. Only these exact remaining callers gain evidence; optional absent needle/length/color values do not. |

## Native review and closeout

Native-speaker validation **NOT_REQUESTED**. No native reviewer, independent human knitting expert or regional panel signed off this work. US knitting terminology and region-specific reference evidence retain their explicit glossary/source boundaries in the historical record; the three latest shared fixes were independently checked as documented above.

LANGUAGE_AUDIT.md section16 prevents AUDIT_PASS while required rendering or material uncertainty is outstanding. **Current final status: REVIEW_COMPLETE / OPEN;890 required visible cases remain unresolved, including one evidence conflict.** The narrower textual review is complete, but the entire English audit is not accepted. Coordinator-owned LANGUAGE_AUDIT.md/shared-fixes.md were read but not rewritten; their pre-recheck English counts should be reconciled by their owner using this current report.

Resume only from the current hashes and the remaining-render-cases/coverage rows; retain accepted unchanged evidence, resolved ten findings, native status, exclusions and the new PDF handoff. Do not restart B01–B23 or repeat excluded device checks.

---

## Historical English snapshots — superseded by the current section above

Everything below is the preserved2026-10-18 report, including its then-current OPEN findings, counts, old build identities and initial-audit history. These are historical states only. The2026-10-19 decisions above and current coverage.tsv control acceptance.



### Current scope and outcome — final post-fix review

**English B01–B23: REVIEW_COMPLETE / OPEN. Not AUDIT_PASS. Native-speaker review: NOT_REQUESTED.** Eight of the ten shared findings are resolved from the English perspective. B03-02 and B19-01 remain partially resolved: historical round counts can become “ROWS” after changing the project label, and two chart glyphs still contradict the adopted legend. One existing session test also retains a stale row-specific expectation. Required unobserved rendering cases remain explicitly pending.

- Follow-up date: 2026-10-18, Europe/Helsinki. Branch `codex/audit-korjaukset-20261018`, HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`.
- Read LANGUAGE_AUDIT.md, shared-fixes.md, the original English review/coverage/hashes, AGENTS.md, CODEX.md, CLAUDE.md and current implementation. The plan and shared report are coordination-owned; their historical campaign statuses are not silently rewritten here.
- Starting worktree already contained the original English work and shared corrections, including other-language resources. Fingerprinted all nonignored existing files before editing. This follow-up changes only three English values in `app/src/main/res/values/strings.xml` and the three English evidence files. No shared Kotlin/data/tests or other-language files were changed. The original 19 English corrections remain intact.
- Compared all 403 prior input hashes: 23 implementation/resource inputs changed. Independently inspected their changed content and affected consumers. All 65 shared final-manifest inputs were compared: 64 remain byte-identical; only the three authorized English values differ in strings.xml. Final `input-hashes.json` pins 428 current inputs, including executed test sources and project/audit instructions.
- Current default resource inventory: 1,351 keys, including two explicitly nontranslatable keys; **1,349 translatable keys**, 1,382 variants. No arrays or English qualifier overrides. Compared with the original English snapshot, 58 variants are new/changed and one obsolete M1-warning resource was removed. Resource text, placeholders, plural branches and all affected caller distributions were reconciled; 28 caller distributions changed.
- Coverage: 2,315 exact resource/variant/context or code-backed rows. There are 1,879 resource occurrences (1,382 variants plus 497 extra contexts) and 436 code-backed items (275 reference cells, 85 code text, 44 symbols, 32 generated cases). Added indirect care-label/semantics uses and the historical-unit transition. Replaced old table cells rather than counting obsolete values as current. `retired-items.json` records 272 replaced/retired historical rows; this is not 272 removed resources.
- New raw evidence: `reports/language-audit/en/20261018-followup/` (ignored local files). Original evidence: `reports/language-audit/en/20261018/`. Copies of the three original English records are retained in the follow-up `before/` directory. Hashes identify versions; they do not assert acceptance.
- Unchanged accepted items retain their earlier evidence. Only changed inputs, affected contexts and selected outstanding ordinary states were reopened. Scope remains English/US terminology; no en-US resource set, translation approval, live account, purchase or production data.
- `EXCLUDED_BY_PROJECT_RULE`: 320 dp, 200% font scale, their combination and TalkBack. None rerun. Accessibility wording still received source/context review; nonvisual semantics is not a spoken-output pass.

### Language decisions and glossary

| Concept | Approved expression/forms | Context | Alternatives and exclusions | Evidence | Status |
| --- | --- | --- | --- | --- | --- |
| Craft | knitting, crochet; project/work session for both | Mixed-craft app | Do not assume knitting from the existence of a row counter | CraftType; READ-B02/B05 | PASS |
| Row/round/repeat | row for flat work, round for circular work, repeat for repeated section | Selected main label takes precedence over craft default | Position is not a quantity; custom labels are user text | CounterValueFormatter; S01 | PASS; historical Insights unit defect remains (B03-04) |
| Stitch | stitch, stitch count, stitch pattern | Loop, operation, resulting count are distinct | Do not call every counter tick a stitch | S01; calculator models | PASS |
| Pattern | pattern, chart, pattern repeat, saved pattern, PDF/document, web pattern | Instructions versus repeat versus stored metadata | A metadata link does not promise an offline PDF | S01; saved-pattern/document repositories | PASS |
| Gauge | gauge, stitch gauge, row gauge; per 10 centimeters / per 4 inches | US English; independent stitch/row density | Tension is a valid regional alternative, not needed here; 4in is 10.16cm | MeasurementCalculator; S03 | PASS |
| Swatch | gauge swatch, measured width/height | Actual measured sample and provenance | Do not treat placeholder numbers or a yarn label as measured gauge | GaugePresentation/ViewModel; S14 | PASS |
| Increase/decrease | increase/decrease; make one; knit two together | Operation, consumed stitches, net change and lean separately | M1 is not interchangeable with KFB/YO | S01,S05,S06 | PASS; generated M1 independently rechecked |
| Slip and yarn position | knitwise/purlwise; yarn in front/back of the work | Insertion orientation and yarn position independently | Front is the side facing the knitter, not always the purl side | S01,S05 | PASS after correction |
| Sides/loops | right/wrong side; front/back loop; left/right leaning | Fabric side, loop orientation and direction are separate axes | Conventional Western stitch mount is assumed by generic twisted-loop entries | S01,S02 | PASS |
| Cast on/bind off | cast on, bind off | Starting/finishing knitting | Cast off is a regional alternative; app save/delete are unrelated | S01 | PASS |
| Repeats/shaping | repeating counter, repeat section, shaping, occurrence | Count-up/wrapping/derived section and interval reminder | Repeat number differs from row within repeat | ProjectCounterLogic; S01 | PASS; neutral count wording rechecked |
| Yarn weight | Lace, Fingering, Sport, DK, Worsted, Aran, Bulky, Super Bulky | Canonical thickness values already persisted | Physical mass is weight in g; a regional ply category is not strand construction | S09,S13 | PASS; unsupported hints removed, not factually endorsed |
| Yarn units | meters/yards, grams, skeins; whole skeins | Length, mass, package count, fractional project usage | Whole-package purchase mass differs from consumed mass | YarnEstimator/YarnUsageCalculator; S13 | PASS after correction |
| Yarn ownership | planned, allocated, used, remaining, over by; unknown | Independent project values; remaining = allocated − used | Zero is known; no automatic deduction from My Yarn | ProjectYarnUsageRepository | PASS |
| Color | color name/number, dye lot | Manufacturer metadata | Different dye lots can vary; no universal extra-skein quantity | S14; yarn form | PASS |
| Needles | needle diameter in mm; US, UK/Canadian, JP | System and manufacturer matter | Do not state a universal 0–50 range or exact regional equivalence | S03,S10,S11,S15 | PASS; every JP cell rechecked against Clover |
| Size/fit | body measurement, circumference, width, length | Body reference, not finished garment size/ease | Numerical dress sizes and age bands need their own mapping | S04,S16–S21 | PASS; every current metric endpoint rechecked against its CYC table |
| Care | wash max °C, hand wash, do not wash/bleach/tumble dry/iron/dry clean; dry clean (P/F) | Symbol permission/prohibition | Product label governs. P/F specify professional process classes, not single solvents | S07,S08 | Labels PASS after correction |
| App actions | save, attach/unlink, add/remove, delete, import/export, restore/replace | Ownership and actual consequence | Restore replaces local project data; export is not sync | Repositories and UI callers | PASS |
| Time/insights | work session, elapsed time, session history, minutes per row, completion event | Start/Stop is explicit; completion cycles may repeat | A completion event is not a distinct project; mixed counter units need shared policy | Session repositories/Insights state | PASS language; historical unit dependency B03-04 open |

### Sources

The initial review accessed the sources on 2026-10-18. This follow-up freshly reopened S02, S05, S08, S10 and S16–S21 on the same date; unchanged-source findings retain their original evidence. Evidence below paraphrases the specific inspected passage/diagram. Sources sharing CYC provenance are not independent corroboration. Source IDs support concepts; each corresponding English entry was read separately.

| ID | Title and publisher | Exact URL | Language/region | Inspected passage, supported claim and limitations |
| --- | --- | --- | --- | --- |
| S01 | Knitting Abbreviations Master List, Craft Yarn Council | https://www.craftyarncouncil.com/standards/knitting-abbreviations | English/US | Complete abbreviation list; slip orientation, stitch operations, front/back yarn, SSK and centered double decrease. Pattern definitions may differ. |
| S02 | Knitting Chart Symbols, CYC | https://www.craftyarncouncil.com/standards/knit-chart-symbols | English/US | Text and actual diagrams including the cable section inspected in browser. Empty square/dot/slashes, twisted loops, slipped stitches, lifted increases and cable widths. The old standalone cable-symbols URL was unavailable; the accessible cable section is the evidence. Alternative conventions need an identified legend. |
| S03 | Hooks & Needles, CYC | https://www.craftyarncouncil.com/standards/hooks-and-needles | English/US | Entire metric/US table and manufacturer-variation note; includes US 17 at 12.5/12.75mm and US 70 at 35mm. Does not verify UK or JP. |
| S04 | Body Sizing, CYC | https://www.craftyarncouncil.com/standards/body-sizing | English/US | Body measurement definitions and ease distinction. Six actual linked tables separately inspected below. |
| S05 | Make One Right / Make One Left, Purl Soho | https://www.purlsoho.com/create/make-one-right-m1r-make-one-left-m1l/ | English/US | M1L lifts front-to-back and knits back loop; M1R lifts back-to-front and knits front loop. Both require the strand between stitches. Staff explanation on adjacent increases supports separating them with a worked stitch. Independent of CYC. |
| S06 | Slip Slip Knit Pass (S2KP), Purl Soho | https://www.purlsoho.com/create/slip-slip-knit-pass-s2kp/ | English/US | Slip two together knitwise, knit one, pass both over. Independent check of the technique-critical correction. |
| S07 | Table of Care Symbols, GINETEX | https://www.ginetex.es/wp-content/uploads/2017/07/GINETEX-Table-of-care-symbols-white-Web.pdf | English/international | PDF text: washing maxima in Celsius, drying/ironing/bleaching/professional cleaning processes; P and F are process classes. Symbols also visually checked against S08. Older published table; no new numerical ironing-temperature advice inferred. |
| S08 | Care Symbols, CYC | https://www.craftyarncouncil.com/standards/care-symbols | English/US and international labels | Text and diagrams inspected: wash tub/numerals/hand, triangle/cross, drying square/circle/dots, iron/dots, cleaning circles A/P/F. A is legacy any-solvent notation. Regional tumble-dry dot wording differs; app does not add a conflicting numerical temperature. |
| S09 | Standard Yarn Weight System, CYC | https://www.craftyarncouncil.com/standards/yarn-weight-system | English/US | Thickness categories and gauge/needle guidance. Does not establish the app's AU/UK ply mappings. |
| S10 | Takumi needle specification catalog, Clover | https://clover.co.jp/recipe/takumikikakus.pdf | Japanese/JP numeric system | Manufacturer table, page 1: JP 0=2.1mm through 15=6.6mm in 0.3mm steps. Used only for numeric JP equivalence, not English language approval. It is the actual target system's primary specification. |
| S11 | Knitting Needle Sizes, KnitPro | https://www.knitpro.eu/en/blog/knitting-needle-sizes | English/manufacturer | Manufacturer comparison supports fractional US labels and regional variation. Its 9/10mm UK table has an inconsistency with its narrative, so that disputed area was checked independently with S15 rather than adopted. |
| S13 | Brooklyn Tweed Yarns, manufacturer | https://brooklyntweed.com/pages/yarn | English/US | Shelter is worsted and 2-ply; Loft is fingering and 2-ply. Confirms thickness and construction are different. Does not verify an AU/UK category mapping. |
| S14 | Weidlinger, Nadya Stallings / Brooklyn Tweed | https://brooklyntweed.com/products/weidlinger | English/US | Materials note conditionally suggests an extra skein when specified counts approach exact yardage. Gauge is after blocking; finished dimensions and ease separately specified. Supports qualified advice, not a universal extra-skein rule. |
| S15 | UK & US Needle & Hook Sizes, Wool Zone | https://www.woolzone.co.uk/wp-content/uploads/2021/06/Needle-Conversion-Chart.pdf | English/UK | Publisher's entire comparison table, page 1; UK 9mm=00 and 10mm=000. Used for UK cells only; not substituted for CYC's US 17 or Clover JP values. |
| S16 | Baby Size Chart, CYC | https://www.craftyarncouncil.com/standards/baby-size-chart | English/US | Entire table, fixed ages 3/6/12/18/24 months; chest/waist/hips and back-waist-length definitions (not generic body length). No preemie or app age-band rule. |
| S17 | Child/Youth Size Charts, CYC | https://www.craftyarncouncil.com/standards/child-youth-sizes | English/US | Sizes 2–16; each corresponding chest/waist/hip/arm cell. No app height series established. |
| S18 | Woman Size Charts, CYC | https://www.craftyarncouncil.com/standards/woman-size | English/US | XS–5X body ranges; no documented mapping to the app's numeric dress-size labels or single representative values. |
| S19 | Man Size Charts, CYC | https://www.craftyarncouncil.com/standards/man-size | English/US | S–5X body ranges. No app XS row; compare every scalar against its corresponding range. |
| S20 | Head Circumference Chart, CYC | https://www.craftyarncouncil.com/standards/head-circumference-chart | English/US | Preemie/baby/toddler/child/tween/woman/man ranges; not a sourced adult S/M/L table. |
| S21 | Hand Size Charts, CYC | https://www.craftyarncouncil.com/standards/hand-size-charts | English/US | Child age ranges and separate women/men sizes; does not establish the app's generic adult S/M/L/XL mapping. |
| S22 | NIST Guide to the SI, Chapter 6 | https://www.nist.gov/pml/special-publication-811/nist-guide-si-chapter-6-rules-and-style-conventions-printing-and-using | English/US | Section 6.1.2: ordinary unit symbols retain lowercase regardless of surrounding capitalized text; supports g, not G, for mass. |


Follow-up source interpretation: S02 now supports a corrected right-side convention disclaimer, but does not support the current KFB chevron or overlapping P2tog dot (B19-02). S10 page 1 was fetched again and its actual table image inspected. Every current metric endpoint in S16–S21 was read; the app intentionally derives inches from cm/2.54. The child/youth arm definition is length to the underarm, now explicit in English. No test or coordinator verdict substitutes for these comparisons.

### Current batch tracker

Every row is exact membership in coverage.tsv by batch. Source inspection is complete; acceptance remains OPEN wherever required V evidence or a finding is unresolved.

| Batch | Current items | V PASS | V FAIL | V pending | V N/A | Work / acceptance | Current dependency |
| --- | ---: | ---: | ---: | ---: | ---: | --- | --- |
| B01 | 110 | 19 | 0 | 91 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B02 | 173 | 20 | 0 | 150 | 3 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B03 | 115 | 37 | 0 | 78 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage; B03-05 stale test expectation |
| B04 | 64 | 26 | 0 | 38 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B05 | 160 | 34 | 3 | 121 | 2 | REVIEW_COMPLETE / OPEN | LA-en-B03-02, LA-en-B03-04 |
| B06 | 142 | 64 | 0 | 37 | 41 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B07 | 78 | 26 | 0 | 52 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B08 | 123 | 10 | 0 | 113 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B09 | 89 | 2 | 0 | 87 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B10 | 85 | 28 | 0 | 49 | 8 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B11 | 80 | 16 | 0 | 64 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B12 | 52 | 17 | 0 | 34 | 1 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B13 | 107 | 40 | 0 | 67 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B14 | 39 | 4 | 0 | 35 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B15 | 25 | 9 | 0 | 16 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B16 | 19 | 13 | 0 | 6 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B17 | 377 | 359 | 0 | 11 | 7 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B18 | 235 | 231 | 0 | 4 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B19 | 83 | 74 | 2 | 5 | 2 | REVIEW_COMPLETE / OPEN | LA-en-B19-01, LA-en-B19-02 |
| B20 | 39 | 13 | 0 | 26 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B21 | 30 | 6 | 0 | 24 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B22 | 82 | 19 | 0 | 63 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |
| B23 | 8 | 2 | 0 | 6 | 0 | REVIEW_COMPLETE / OPEN | Required unobserved states remain in coverage |

### Independent resolution of all ten shared findings

`VERIFIED` below applies to the named defect, not to every unrelated pending state in its batch. Shared technical test success alone was not used to approve facts, terminology, naturalness or drawings.

| Original finding | English recheck and actual evidence | Current state |
| --- | --- | --- |
| LA-en-B14-01 | Read flat/circular output and new rejection language. S05 M1 needs a distinct original between-stitch bar. Independent expansion of 4,998 actual generated sequences: 2,499 valid inputs consume N original stitches and use k distinct available bars; 151 invalid inputs rejected. Flat maximum N−1, circular N, N≥2. Visible 96+12 retains final stitch; 1+3 gives corrected actionable error. | VERIFIED; language follow-up B14-02 also verified |
| LA-en-B15-02 | Compared neighboring usable repeats to the exact unrounded body count. 11.9→8, 12 tie→8, 12.4/12.5/12.6→16; edges2 added afterward→18. Imperial case 4.96in at10/4in→16, actual6.4in. Actual 12.4cm screen shows16st/16cm. | VERIFIED |
| LA-en-B17-02 | Every one of 16 JP0–15 mappings compared with Clover page1: 2.1–6.6mm, step0.3mm. Current 38 rows/152 cells reconciled, all rows viewed through overlapping scroll positions. Unchanged US/UK source evidence retained. Missing equivalents remain dashes, not fabricated sizes. | VERIFIED |
| LA-en-B17-03 | Every current measurement/end point checked against its corresponding CYC table: 46 rows, 123 metric measurements, including58 ranges. Unsupported categories removed; group/age/size labels read separately. All metric cells rendered and inspected; every inch conversion checked as cm/2.54, with women's/hand repeated-cell layouts inspected in imperial. Arm heading corrected to its actual measure. | VERIFIED; B17-04 also verified |
| LA-en-B19-01 | All24 remaining triples re-read; actual CYC diagrams and current ordinary-size glyphs compared. Twisted stitches, directional M1/lifted increases, double decreases and four/six-cell cables now agree. Unsupported knitwise-slip entry is absent. KFB and P2tog still disagree; disclaimer cannot repair them. | OPEN, partially resolved; precise remainder B19-02 |
| LA-en-B03-02 | Neutral session, photo, reminder, shaping/repeat, reader and per-project progress text independently read and rendered. Same layout can serve all selected units because these callers no longer select a row noun. Current non-ROWS exclusion in Insights works, but changing a project to ROWS retroactively labels actual recorded rounds as rows. | OPEN, partially resolved; precise remainder B03-04 |
| LA-en-B03-03 | Fresh120×48,120×80,160×160,300×160 widget PNGs inspected; complete0 glyph visible. Existing host bounds and receiver-write test passes unchanged. These widget dimensions are not the excluded320dp application test. | VERIFIED |
| LA-en-B06-03 | All eight canonical thickness labels read, stored values unchanged, every unsupported regional ply hint removed. Actual selector shows eight labels plus Other. No claim that the removed mappings were factually verified. | VERIFIED by removal of unsupported presentation |
| LA-en-B06-04 | All20 care drawing branches/source diagrams checked, current gallery inspected. Hand is recognizable and drying dots separate. Existing labels now reach visible detail and semantics; actual hand/bleach/one- and two-dot detail labels inspected, identical label layout reused for remaining individually read labels. | VERIFIED |
| LA-en-B16-03 | Actual result450m/(200m per skein)=2.25, purchase3 skeins,3×50g=150g. The visible whole-skein badge preserves lowercase g; arithmetic and English label match. | VERIFIED |

### New English corrections

#### LA-en-B14-02: M1 rejection advice cannot resolve a one-stitch input

- Classification CONFIRMED_DEFECT; category LANGUAGE/CONTEXT; severity S2; state VERIFIED.
- Key/file: `increase_decrease_error_not_enough_bars`, default strings.xml; IncreaseDecreaseScreen validation message, both flat/circular modes.
- Before: “Not enough spaces between stitches for these M1 increases. Reduce the number of increases.” With one starting stitch, reducing a positive increase count cannot satisfy the N≥2 precondition.
- Final: “Not enough spaces between stitches for these M1 increases. Use at least two starting stitches and reduce the number of increases if needed.” This changes only the actionable English advice, preserving the shared algorithm and placeholders (none).
- Evidence: actual preconditions, S05, independent probe, initial invalid1+3 screen and final `m1-invalid-corrected.png`. Final resource tests and app build pass. No remaining English uncertainty; other locale owners must independently inspect their equivalent advice.

#### LA-en-B17-04: Arm heading omits the endpoint of the measurement

- Classification CONFIRMED_DEFECT; category TERM/CONTEXT; severity S2; state VERIFIED.
- Key/file: `size_col_arm`, default strings.xml; child/youth body chart column.
- Before “Arm”; final “Arm length to underarm”. S17 defines this specific length, not arm circumference or total shoulder-to-wrist length. The exact table values were separately verified.
- Smallest correction: one English header, no data or layout change. Actual two-line header fits the current table (`sizes-child-right.png`); compilation and resource checks pass. Other languages are not approved by this wording.

#### LA-en-B19-03: Symbol disclaimer omits the adopted right-side convention

- Classification CONFIRMED_DEFECT; category CONTEXT; severity S2; state VERIFIED for this text only.
- Key/file: `chart_symbols_disclaimer`, default strings.xml; Chart Symbols footer paired with all current entries.
- Before “Symbols follow standard conventions. Always check your pattern's symbol key.” Final “Symbols describe stitches as seen from the right side of the work. Always check your pattern's symbol key.” This identifies the actual CYC viewing convention and avoids implying a universal legend.
- Evidence: actual S02 diagrams and shared renderer's adopted convention, all24 English names/descriptions, visible footer in `symbols-cables.png`, resource/build checks. KFB/P2tog remain separately OPEN; this disclaimer is not their remedy.

### Precise shared handoffs

#### LA-en-B03-04: Editing a project label retroactively turns historical rounds into rows

- Classification CONFIRMED_DEFECT; category FACT/CONTEXT; severity S2; state OPEN. Remaining scope of B03-02; shared implementation owner. English visible resources: `insights_stat_rows`, `insights_stat_min_per_row` and generated row/pace values, B05.
- Files: `ui/screens/insights/InsightsViewModel.kt` (`canShowRowMetrics` and aggregation), `ui/screens/insights/InsightsSections.kt`; provenance boundary in `domain/model/KnitSession.kt` and label editing in `repository/CounterRepository.kt` (`updateProjectDetails`).
- Reproduction on the current isolated app: project uses ROUNDS; start an actual session at10, add10 rounds, stop/save18 seconds; filter Insights to it. Time displays without row totals. Edit the same project label to ROWS through the UI. The same historical session now produces **10 ROWS** and **<1 MIN PER ROW**. History correctly stays “Counter:10→20”. No elapsed-time anchors were fabricated.
- Evidence: `session-round-progress.png`, `session-round-stop.png`, `history-rounds-neutral.png`, `insights-rounds.png`, `insights-historical-rounds-shown-as-rows.png`; current-unit predicate and absence of a per-session unit snapshot. The passing new unit/runtime tests exercise current labels, not historical label provenance.
- Required correction: suppress or neutrally label historical quantity/pace unless its actual unit is known. The current project label is insufficient evidence. Any future unit snapshot must leave old unknown units unknown; do not infer them from today's label. Minimum regression: record a ROUNDS session, change current label to ROWS, check filtered and all-project metrics while preserving time/session history.
- All locales share this semantic error. English-only string changes cannot repair the numeric claim. Do not mark B03-02 resolved until the historical transition is corrected and retested.

#### LA-en-B19-02: KFB and P2tog drawings still contradict the adopted CYC legend

- Classification CONFIRMED_DEFECT; category FACT/CONTEXT/RENDERING; severity S1; state OPEN. Remaining scope of B19-01; shared drawing owner.
- Files/members: `ui/screens/chartsymbols/ChartSymbolScreen.kt`, `ChartSymbolDrawing` p2tog/kfb branches and `drawVShape`; `domain/calculator/ChartSymbolData.kt`; paired `sym_p2tog_name/desc` and `sym_kfb_name/desc`.
- KFB currently uses a plain upward chevron. Actual S02 k1fb alternatives are a branched upright stem or the illustrated numbered downward-chevron alternative; neither is the current plain upward chevron. The local comment calling it a CYC chevron is not evidence.
- P2tog's dot center lies on its slash (normalized .35,.65 on the .25,.75→.75,.25 stroke). The actual CYC diagram separates the dot above/left, keeping purl identity legible. Current small glyph obscures that distinction.
- Evidence: actual source images opened in the browser and `symbols-basic.png`, `symbols-increases.png`, `symbols-cables.png`; every other current symbol/label pair rechecked separately. This finding corrects the initial audit's overbroad acceptance of these two glyphs; the earlier assertion is superseded.
- Required minimal correction: implement one actual documented KFB alternative and separate the P2tog dot from the slash. Recheck both actual small previews with their English operation text and the declared right-side convention. A generic diagram-difference test cannot establish technique correctness. All locales share the renderer; no shared drawing changed in this chat.

#### LA-en-B03-05: Stop-summary test still expects the retired row wording

- Classification CONFIRMED_DEFECT; category RESOURCE/validation; severity S3; state OPEN. Shared test owner; this is an outdated test expectation, not proof of a current UI wording failure.
- File/member: `app/src/androidTest/java/com/finnvek/knittools/ui/screens/counter/WorkSessionComposeTest.kt`, `stopSummaryRequiresExplicitSaveDiscardOrCancel`, line50. Fixture3661 seconds/12 progress; assertion expects “12 rows”, while the current resource/caller emits “Counter progress:12”.
- Actual selected run fails there after the title/duration checks. Five other selected ordinary session tests pass. `session-instrumentation.log` retains the complete failure; manual zero/positive Stop and Save flows separately succeeded and do not rewrite this test as passing.
- Required minimum correction: assert the intended neutral counter-progress text/resource, preserving the remaining Save/Discard/Cancel behavior checks; rerun this ordinary method. Do not run the excluded200%/narrow-width methods in the class. No locale translation or app behavior change is required to fix the test.

### Follow-up validation and evidence

Ordinary device: owned AVD `KnitToolsLanguageEn20261018`, serial `emulator-5580`, Pixel6/API36 Android16,1080×2400px/420dpi (about411dp),font1.0, English app/system configuration `en,en-US`. Isolated package `com.finnvek.knittools.activepolicytest`, version1.0.0. Synthetic data only; local debug entitlement fixture is not a real purchase. Final APK SHA-256: `4e3a25744de1d4d0cb1ccf1de29084324d23f9c2e0e8b0bd3db6f11e0ed37472`. Final test APK SHA-256: `c3942b5c77d937b6b8fb2f8050474a9a4814bcfcf3f1e1ed7adc5148a419f703`.

The first follow-up build contained the arm/disclaimer fixes; the final build adds only the M1 error advice. Earlier renderer evidence is retained only for unchanged rendering/wording. The first APK's binary checksum was not retained separately; its source differences and build/test logs are retained. Final APK hashes above are not falsely assigned to earlier captures. Final M1 wording was observed after final installation.

#### Commands and actual results

- `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`, with the ten suite filters below, `-Pknittools.isolatedTest=activepolicy --no-daemon`: **84 tests/10 suites PASS**, zero failures/errors/skips; `build-tests.log`, `unit-results/`, `unit-summary.json`.
- After the final M1 wording edit: rebuild both APKs and repeat `ReferenceResourceLocalizationTest`, `CountPluralResourcesSourceTest`, `CounterStringResourcesSourceTest`: **10 tests/3 suites PASS**, zero failures/errors/skips; `final-build-tests.log`, `final-unit-results/`. This is a repeated subset, not94 distinct tests. Both final APKs installed successfully.
- Direct compiled-JVM calculation probe:2,650 M1 input/mode cases,2,499 valid/151 rejected,4,998 generated easy/balanced sequences independently expanded and checked; exact cast-on midpoint/edge/imperial cases and yarn package arithmetic. `FollowupProbe.java`, `calculator-probe.tsv`, `check_probe.py`, `probe-summary.json`. Initial shell quoting of a JVM property failed before probe execution and was corrected; only the completed probe is evidence.
- First ordinary Android selection:14 methods,13 pass/1 failure (`TOTAL:20 ST` not displayed in `increaseAndDecreaseModesReplaceTheVisibleTotal`, line86). Exact isolated rerun **PASS** (`instrumentation-increase-retry.log`,6.339s). The original failure remains recorded; its timing cause is not proven. All14 distinct methods have a passing result.
- Final ordinary WorkSession selection:6 methods,5 pass/1 stale expectation failure B03-05. Combined20 distinct selected Android methods:19 have passing results; one remains failed. No full-class invocation of WorkSessionComposeTest, no excluded layout tests.
- The initial session runner name was invalid; startup ran zero tests (`session-instrumentation-runner-error.log`). Verified installed runner `androidx.test.runner.AndroidJUnitRunner`, then ran the six methods above. A startup failure is not a pass or app defect.
- Final consistency: XML parses, resource inventory/placeholder/type checks and coverage/input hashes reconcile. In-memory reversal of exactly the three English value edits reproduces the starting strings.xml SHA-256. All other pre-existing nonignored files are preserved except the three explicitly updated English evidence files. `git diff --check` is recorded separately in final verification.

| Suite | Tests | Result |
| --- | ---: | --- |
| com.finnvek.knittools.domain.calculator.CastOnCalculatorTest | 18 | PASS |
| com.finnvek.knittools.domain.calculator.ChartSymbolDataTest | 6 | PASS |
| com.finnvek.knittools.domain.calculator.IncreaseDecreaseCalculatorTest | 22 | PASS |
| com.finnvek.knittools.domain.calculator.NeedleSizeDataTest | 11 | PASS |
| com.finnvek.knittools.domain.calculator.SizeChartDataTest | 13 | PASS |
| com.finnvek.knittools.domain.model.StandardYarnWeightTest | 2 | PASS |
| com.finnvek.knittools.ReferenceResourceLocalizationTest | 2 | PASS |
| com.finnvek.knittools.ui.CountPluralResourcesSourceTest | 1 | PASS |
| com.finnvek.knittools.ui.screens.counter.CounterStringResourcesSourceTest | 7 | PASS |
| com.finnvek.knittools.ui.screens.insights.InsightsCounterUnitsTest | 2 | PASS |

#### Exact ordinary Android inventory

| Class | Methods actually selected | Result |
| --- | --- | --- |
| WidgetHostRuntimeTest | responsiveSizesRenderAndButtonWritesThroughReceiver | PASS |
| SharedAuditSymbolsRuntimeTest | careSymbolsLight, careSymbolsDark, chartSymbolsLight, chartSymbolsDark | 4 PASS; image-difference assertions do not prove factual meaning |
| AuditPart14ToolsRuntimeTest | castOnRendersSnappedCountAndDropsInvalidResult; increaseAndDecreaseModesReplaceTheVisibleTotal; yarnPurchaseWeightAndChooseActionKeepCurrentInputs; everySizeCategoryDisplaysItsOwnHeaderAndFirstRow; abbreviationQueryAndExpansionSurviveCraftContextChange | 5 have PASS; increase method initially failed, isolated retry passed |
| InsightsCounterUnitsRuntimeTest | selectedUnitsKeepMainLabelAndNeutralHistoryWhileOnlyRowsShowRowMetrics | PASS; historical edit case not covered |
| AuditPart08RuntimeMatrixTest | p1150_counterControlsWriteOnlyIntendedValues; p1158_projectPdfReadingMarkupLayersAndExport; p1190_libraryPdfReadingMarkupLayersAndExport | 3 PASS |
| WorkSessionComposeTest | stopSummaryRequiresExplicitSaveDiscardOrCancel | FAIL, B03-05 |
| WorkSessionComposeTest | recoveryDialogExposesReasonAddDiscardEditAndValidatedEditor; persistenceErrorOffersRetryWithoutClaimingSuccess; anotherProjectStartRequiresExplicitSessionResolution; projectCompletionRequiresExplicitSessionResolution; projectDeletionRequiresExplicitSessionResolution | 5 PASS |

Correction to the initial p1150 diagnosis: the retained original failure was “Could not scroll to Project actions” at line424 while the draft-step sheet was open, not a later count assertion. The shared Cancel/Save repair was inspected and the fresh p1150 passes. Fresh p1158 also passes; the original SAF cancellation failure remains historical evidence, not erased.

#### Inspected rendering and remaining boundaries

| Evidence group | Exact new inspection | Boundary |
| --- | --- | --- |
| FOLLOWUP-PROBE / M1 and cast-on | `m1-flat96.png`, invalid input before/after (`m1-invalid-corrected.png` final), `caston12-4.png`; full generated instruction, labels and error read | Probe is arithmetic/technique evidence; not2,650 separately rendered inputs |
| FOLLOWUP-SIZES | `sizes-baby-left/right`, `sizes-child-left/right`, `sizes-women-left/right`, `sizes-men-left/right`, `sizes-head`, `sizes-hand`; imperial women left/right and hand | All metric endpoints checked, all metric cells viewed; repeated cell layout reused for imperial, every conversion checked independently |
| FOLLOWUP-NEEDLES | `needles-top`, `needles-middle1`, `needles-middle2`, `needles-bottom-settled` | All38 rows/152 cells; a transient scroll capture was followed by settled inspection, no persistent compression defect confirmed |
| FOLLOWUP-SYMBOLS | `symbols-basic`, `symbols-increases`, `symbols-cables`; all24 names/descriptions and glyphs, right-side disclaimer | Two glyph failures stay FAIL regardless of passing image tests |
| FOLLOWUP-CARE | All20 gallery glyphs; `care-labels-top/middle/bottom`, `care-hand`, `care-drying`, `care-two-dots`; actual paired labels and all20 source meanings read | Repeated fixed-width label layout reused; not every possible20-label yarn-card combination, no TalkBack |
| Yarn | `yarn-weight-options.png` contains eight canonical labels plus Other; `yarn150g.png` actual3skeins/2.25/150g | Unrelated stash/editor errors retain their own original/pending status |
| FOLLOWUP-PDF | `pdf-counter-menu`, `pdf-marker-menu`, `pdf-calibrate`, `pdf-calibrate-last`, `pdf-calibrate-invalid`, fresh p1158 `reading-guides.png` | Count42, follow/return/marker/clear/calibration wording inspected; accessibility-only follow announcements are source-reviewed N/A for visual display, not spoken PASS |
| FOLLOWUP-BOOKMARKS | `bookmark-add-dialog`, `bookmark-populated`, `bookmark-rename`, `bookmark-rename-dialog`, `bookmark-delete-dialog` | Required-name state, page2/3 Selected, maximum50 characters, rename/delete consequences; delete was cancelled, no claim of forced failure branches |
| Counter/repeats | `counter-shaping-counts`, `counter-repeat-counts`, `counter-repeat-progress` | Inclusive40..44 gives Count3/5 at42; Repeat1/3; neutral labels inspected. Same unit-neutral layout reused across selected label types |
| Reminders/photos | `reminder-neutral-form`, `reminder-repeat-one`, `reminder-repeat-two-confirmed`; `photo-viewer-count`, `photo-grid-count`, `library-photo-count` | Count42; Repeat#1 interval1/2; photo positions3/42. Synthetic photo notes set null to exercise fallback; this is presentation evidence, not a rename-operation test. Overview thumbnail Count42 is semantics only |
| Sessions/Insights | `session-round-progress`, `session-round-stop`, `history-rounds-neutral`, `insights-project-progress`, `history-zero-and-positive-progress` | Actual18s/10rounds and8s/zero progress; neutral range/single forms. Per-project progress41/10, relative times. Historical unit transition separately FAIL |
| Actual reboot recovery | `session-reboot-recovery`, `session-recovery-pending-progress` | Real owned-emulator boot count3→4 while session active. Review restart reason, confirmed0:00:00, time away0:00:48; cancel, add two rounds, reopen shows Pending counter progress:2. Continuation and later Stop/Save worked. No clock/elapsed anchors fabricated. Editor validation proof comes from the passing selected test, not this manual flow |
| FOLLOWUP-RUNTIME / widget | four `widget-*.png` images | All four ordinary widget sizes and receiver writes pass |

All58 changed/new English resource variants now have actual rendered or explicit identical-layout evidence where visual, or a concrete nonvisual/unused N/A reason. Correct language does not approve paired erroneous glyphs or historical units. Changed code/data items have their separate facts/symbol/generated-output rows. Common unchanged entries are not automatically upgraded merely because they appeared near a new screenshot.

**Remaining required evidence is real and still pending.** The current TSV has 1177 V=BLOCKED rows. This means *not observed*, not that the emulator was unavailable or that every row requires a unique screenshot. Required distinct warnings, destructive confirmations and dynamic forms cannot be accepted merely from source. Pending groups include remaining project/folder/bulk conflict/error paths; incoming PDF and camera/gallery/provider failures; document/order/availability states; annotation/export error/progress/cancellation variants; remaining local save/retry conflicts; midnight/DST/long-gap/clock-unavailable session/range states; and backup corrupt/space/write phases. Live Ravelry login/download and real Play/account price/purchase/restore states were not driven; no real purchase is required by the plan. They are not marked passed. Exact membership, caller and evidence are in `coverage.tsv`, filter V=BLOCKED. No new infrastructure, real account or destructive user-data operation was introduced to manufacture a PASS.

#### Evidence-reference mapping

`FOLLOWUP-DELTA`/`CALLERS`: `resource-delta.json`, `caller-delta.json`, `callers.json`, current source and this report; the original delta predates the three final wording edits, which are explicitly listed above. `FOLLOWUP-SHARED`: the independent ten-finding table, not the shared correction report alone. `FOLLOWUP-INSIGHTS`: B03-04 and its two before/after Insights screens. `FOLLOWUP-NEUTRAL-LAYOUT`: actual reminder/photo/shaping/session forms plus the unit-independent caller data flow; explicitly reused layout, not separate claims for every unit permutation. `SCREEN:*`: file under the follow-up raw root. `SESSION-TEST:*`: the named selected method and `session-instrumentation.log`. Unchanged `READ-Bxx`, `RES`, original screenshot/source IDs retain the initial-audit meaning below. Missing visual evidence remains BLOCKED.

### Historical initial audit evidence — superseded where identified above

The following retained record describes the initial English audit, before shared corrections. Its OPEN/BLOCKED states, old table/glyph counts, old input identities and first-run failures are **historical**, not the current verdict. The current independent resolution table and precise follow-up findings control. The p1150 failure diagnosis and initial KFB/P2tog acceptance are explicitly corrected above. The original nineteen English value fixes remain unchanged. This history preserves why those changes and handoffs were made; it does not restart or reapprove unchanged items.

#### Initial finding details (historical states)

##### LA-en-B03-01: Widget wording assumes rows

- Classification: CONFIRMED_DEFECT. Category: CONTEXT. Severity: S2. State: VERIFIED (resource correction verified; shared context/rendering dependency remains).
- File: `app/src/main/res/values/strings.xml`; variant `value`. Every affected caller is listed for each key in coverage.
- Original → final:
  - `widget_name`: “Row Counter” → “Project Counter”
  - `widget_description`: “Row counter widget for active project” → “Counter widget for the active project”
  - `row_label_with_target`: “Row %1$d / %2$d” → “%1$d / %2$d”
- User-visible consequence and evidence: Widget has no selected-label argument; numeric count/target remains exact for all crafts.
- Smallest sufficient correction: Use a project counter name/description and neutral count/target.
- Shared/other-language impact: no shared implementation changed; other locales must review equivalent concepts independently.
- Checks actually performed: CounterWidget source, preview layout, formatter call and RES; ordinary host run failed on the independent compact-glyph finding LA-en-B03-03.
- Remaining dependency: runtime acceptance is limited to observed cases in the validation matrix; unrelated shared findings stay open.

##### LA-en-B05-01: Insights empty state excludes crochet

- Classification: CONFIRMED_DEFECT. Category: CONTEXT. Severity: S2. State: VERIFIED (source/resource and observed ordinary layout verified).
- File: `app/src/main/res/values/strings.xml`; variant `value`. Every affected caller is listed for each key in coverage.
- Original → final:
  - `insights_empty_title`: “No time on the needles yet.” → “No work sessions yet.”
- User-visible consequence and evidence: The screen aggregates sessions from knitting and crochet, including crochet-only accounts. Needles are not universal.
- Smallest sufficient correction: Name the absent work sessions directly.
- Shared/other-language impact: no shared implementation changed; other locales must review equivalent concepts independently.
- Checks actually performed: Insights empty-state caller and RES; empty UI layout validation.
- Remaining dependency: runtime acceptance is limited to observed cases in the validation matrix; unrelated shared findings stay open.

##### LA-en-B06-01: Care temperatures omit the Celsius unit

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED (resource correction verified; shared context/rendering dependency remains).
- File: `app/src/main/res/values/strings.xml`; variant `value`. Every affected caller is listed for each key in coverage.
- Original → final:
  - `care_wash_30`: “30°” → “30°C”
  - `care_wash_40`: “40°” → “40°C”
  - `care_wash_60`: “60°” → “60°C”
- User-visible consequence and evidence: English users may interpret a bare degree sign as Fahrenheit. S07/S08 specify Celsius maxima for these wash symbols.
- Smallest sufficient correction: Add C to the three existing labels, without changing temperature values.
- Shared/other-language impact: no shared implementation changed; other locales must review equivalent concepts independently.
- Checks actually performed: S07/S08, care label/icon mapping, RES.
- Remaining dependency: runtime acceptance is limited to observed cases in the validation matrix; unrelated shared findings stay open.

##### LA-en-B06-02: Professional cleaning labels call P and F individual solvents

- Classification: CONFIRMED_DEFECT. Category: TERM. Severity: S2. State: VERIFIED (resource correction verified; shared context/rendering dependency remains).
- File: `app/src/main/res/values/strings.xml`; variant `value`. Every affected caller is listed for each key in coverage.
- Original → final:
  - `care_dryclean_p`: “P solvent” → “Dry clean (P)”
  - `care_dryclean_f`: “F solvent” → “Dry clean (F)”
- User-visible consequence and evidence: P/F denote professional dry-cleaning process classes; the original wording implies a solvent named P or F.
- Smallest sufficient correction: Use the existing dry-clean action plus the exact symbol letter.
- Shared/other-language impact: no shared implementation changed; other locales must review equivalent concepts independently.
- Checks actually performed: S07/S08, care mapping, RES.
- Remaining dependency: runtime acceptance is limited to observed cases in the validation matrix; unrelated shared findings stay open.

##### LA-en-B15-01: Desired width does not identify the body before edge stitches

- Classification: CONFIRMED_DEFECT. Category: CONTEXT. Severity: S2. State: VERIFIED (source/resource and observed ordinary layout verified).
- File: `app/src/main/res/values/strings.xml`; variant `value`. Every affected caller is listed for each key in coverage.
- Original → final:
  - `desired_width`: “Desired width” → “Desired body width”
- User-visible consequence and evidence: The calculator snaps the body to a repeat and then adds total edge stitches. Requested full width would imply a different result.
- Smallest sufficient correction: Add body to this existing input label.
- Shared/other-language impact: no shared implementation changed; other locales must review equivalent concepts independently.
- Checks actually performed: CALC baseline44 and ordinary Cast On screenshot; RES.
- Remaining dependency: runtime acceptance is limited to observed cases in the validation matrix; unrelated shared findings stay open.

##### LA-en-B16-01: Weight result is whole-package purchase mass

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED (resource correction verified; shared context/rendering dependency remains).
- File: `app/src/main/res/values/strings.xml`; variant `value`. Every affected caller is listed for each key in coverage.
- Original → final:
  - `total_weight`: “Total weight: %s g” → “Weight of whole skeins: %s g”
- User-visible consequence and evidence: 450m at200m/50g requires2.25 packages but buys3 and displays150g. Consumption mass would be112.5g.
- Smallest sufficient correction: Identify whole skeins in the result label, preserving %s and g.
- Shared/other-language impact: no shared implementation changed; other locales must review equivalent concepts independently.
- Checks actually performed: YarnEstimator source, CALC purchase case, existing runtime estimator assertion, RES.
- Remaining dependency: runtime acceptance is limited to observed cases in the validation matrix; unrelated shared findings stay open.

##### LA-en-B16-02: Unconditional extra-skein instruction is unsupported

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED (source/resource and observed ordinary layout verified).
- File: `app/src/main/res/values/strings.xml`; variant `value`. Every affected caller is listed for each key in coverage.
- Original → final:
  - `extra_skein_note`: “Always buy one extra skein — dye lots may vary” → “Allow for extra yarn if needed, and check that the dye lots match.”
- User-visible consequence and evidence: The app already rounds up packages. A fixed one-extra rule does not follow from every project or dye-lot situation; S14 uses conditional advice.
- Smallest sufficient correction: Qualify extra yarn need and retain matching-dye-lot guidance.
- Shared/other-language impact: no shared implementation changed; other locales must review equivalent concepts independently.
- Checks actually performed: S14 materials passage, calculator output, RES.
- Remaining dependency: runtime acceptance is limited to observed cases in the validation matrix; unrelated shared findings stay open.

##### LA-en-B17-01: Needle-system help falsely bounds US sizes and universalizes systems

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED (source/resource and observed ordinary layout verified).
- File: `app/src/main/res/values/strings.xml`; variant `value`. Every affected caller is listed for each key in coverage.
- Original → final:
  - `tip_needle_sizes_desc`: “Metric (mm) is the universal standard. US sizes use a numbered system (0–50). UK/Canadian sizes use a different numbered system. Japanese sizes have their own numbering. Always check the mm size to be sure.” → “Needle numbering varies by system and manufacturer. Compare the diameter in millimeters (mm), and check your gauge.”
- User-visible consequence and evidence: CYC includes US70 and manufacturer variations. A universal0–50 range does not describe the actual system.
- Smallest sufficient correction: Explain system/manufacturer variation and use mm/gauge as the comparison.
- Shared/other-language impact: no shared implementation changed; other locales must review equivalent concepts independently.
- Checks actually performed: S03/S11, full needle table review, RES.
- Remaining dependency: runtime acceptance is limited to observed cases in the validation matrix; unrelated shared findings stay open.

##### LA-en-B18-01: SSK description omits separately slipping knitwise and working back loops

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S1. State: VERIFIED (source/resource and observed ordinary layout verified).
- File: `app/src/main/res/values/strings.xml`; variant `value`. Every affected caller is listed for each key in coverage.
- Original → final:
  - `abbr_ssk_desc`: “Left-leaning decrease: slip 2 stitches knitwise, knit them together through back loops” → “Left-leaning decrease: slip 2 stitches knitwise one at a time, then knit them together through their back loops”
- User-visible consequence and evidence: The shortened sequence permits a different stitch orientation/operation from the intended left-leaning decrease.
- Smallest sufficient correction: State the two slips one at a time knitwise and knit through their back loops.
- Shared/other-language impact: no shared implementation changed; other locales must review equivalent concepts independently.
- Checks actually performed: S01/S05 technique comparison; abbreviation caller, RES.
- Remaining dependency: runtime acceptance is limited to observed cases in the validation matrix; unrelated shared findings stay open.

##### LA-en-B18-02: S2KP omits together-knitwise slip orientation

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S1. State: VERIFIED (source/resource and observed ordinary layout verified).
- File: `app/src/main/res/values/strings.xml`; variant `value`. Every affected caller is listed for each key in coverage.
- Original → final:
  - `abbr_s2kp_desc`: “Centered double decrease” → “Centered double decrease: slip 2 together knitwise, knit 1, then pass both slipped stitches over the knitted stitch”
- User-visible consequence and evidence: Two individually slipped stitches do not establish the intended centered double decrease.
- Smallest sufficient correction: Specify slipping two together knitwise, knitting one, passing both over.
- Shared/other-language impact: no shared implementation changed; other locales must review equivalent concepts independently.
- Checks actually performed: S01 and independent S06; abbreviation caller, RES.
- Remaining dependency: runtime acceptance is limited to observed cases in the validation matrix; unrelated shared findings stay open.

##### LA-en-B18-03: Generic M1 describes a direction without identifying it

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED (source/resource and observed ordinary layout verified).
- File: `app/src/main/res/values/strings.xml`; variant `value`. Every affected caller is listed for each key in coverage.
- Original → final:
  - `abbr_m1_desc`: “Pick up bar between stitches and knit through back loop to increase” → “For a left-leaning M1, lift the bar between stitches from front to back and knit through its back loop. Follow the pattern if it specifies a different method.”
- User-visible consequence and evidence: The specified lift/front-to-back and knit-back-loop method is M1L; patterns may specify a different M1.
- Smallest sufficient correction: Name the left-leaning method and preserve the pattern-defined alternative.
- Shared/other-language impact: no shared implementation changed; other locales must review equivalent concepts independently.
- Checks actually performed: S01/S05; separate generated-instruction dependency B14-01, RES.
- Remaining dependency: runtime acceptance is limited to observed cases in the validation matrix; unrelated shared findings stay open.

##### LA-en-B18-04: Front/back yarn position is incorrectly equated with fabric sides

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED (source/resource and observed ordinary layout verified).
- File: `app/src/main/res/values/strings.xml`; variant `value`. Every affected caller is listed for each key in coverage.
- Original → final:
  - `abbr_wyif_desc`: “Hold yarn to the front (purl side) of work” → “Hold yarn in front of the work, on the side facing you”
  - `abbr_wyib_desc`: “Hold yarn to the back (knit side) of work” → “Hold yarn behind the work, on the side facing away from you”
- User-visible consequence and evidence: The purl side is not always facing the knitter. Yarn position follows the current work orientation.
- Smallest sufficient correction: Describe front as facing the knitter and back as away.
- Shared/other-language impact: no shared implementation changed; other locales must review equivalent concepts independently.
- Checks actually performed: S01 yarn-position terms and caller context; RES.
- Remaining dependency: runtime acceptance is limited to observed cases in the validation matrix; unrelated shared findings stay open.

##### LA-en-B20-01: Imperial setting help overstates forced calculator units

- Classification: CONFIRMED_DEFECT. Category: CONTEXT. Severity: S2. State: VERIFIED (source/resource and observed ordinary layout verified).
- File: `app/src/main/res/values/strings.xml`; variant `value`. Every affected caller is listed for each key in coverage.
- Original → final:
  - `tip_imperial_units_desc`: “When enabled, all calculators use inches instead of centimeters and yards instead of meters. Gauge is measured per 4 inches instead of per 10 cm.” → “Sets inches and yards as the default calculator units. Gauge defaults to measurements per 4 inches instead of per 10 cm. You can change units in Measurements and Gauge.”
- User-visible consequence and evidence: GaugeViewModel initializes from the preference only before saved state is ready, and lets users select separate from/to units and gauge basis.
- Smallest sufficient correction: State defaults and the Measurements and Gauge override.
- Shared/other-language impact: no shared implementation changed; other locales must review equivalent concepts independently.
- Checks actually performed: GaugeViewModel/GaugeScreen source and final resource recheck TEST2.
- Remaining dependency: runtime acceptance is limited to observed cases in the validation matrix; unrelated shared findings stay open.


##### LA-en-B03-02: Row-specific consumers ignore the selected main-counter unit

- Classification: CONFIRMED_DEFECT. Category: CONTEXT/FACT. Severity: S2. State: OPEN (shared dependency).
- Keys/contexts: `rows_format` in CounterScreen end-session summary and CounterWorkspace active session; `session_row_range` in session rows; row/pace totals in InsightsSections/InsightsViewModel; `row_label_format` in project/all-photo/gallery views; `row_number`, `repeat_every_rows`, `shape_every_n` and repeat-section row fields; `current_row_short` and row-marker/guide copy in PatternViewerScreen. Exact resource call locations and separate ROUNDS/REPEATS/CUSTOM rows are in coverage.
- Original behavior: these consumers insert the stored main count into row-specific English although a project can count rounds, repeats or a custom unit. The main counter's own `MainCounterDisplayText` correctly maps selected labels; a resource-wide replacement would damage that correct ROWS use.
- Consequence: a crochet round or custom repeat count becomes a row in history, photo position, reader assistance or work totals; mixed-project pace is labeled minutes per row without a unit policy.
- Evidence: actual callers, CounterValueFormatter and data flow through session/Insights models. The shared underlying fields retain historical `rows` names; field names do not authorize user-visible unit changes.
- Proposed correction: coordinator assigns shared UI/domain work to carry selected units through these consumers and defines mixed-unit aggregation. Retain storage compatibility; do not rewrite all occurrences to rounds. Add focused selected-label tests at the affected boundaries.
- Minimality/impact: English-only edits cannot select among four units. All locales use these shared consumers. No shared code was changed here.
- Checks: source/data-flow inspection, main counter formatter/target tests; ordinary main-counter ROWS/ROUNDS/REPEATS/CUSTOM runtime reviewed. Row-specific non-main consumers remain acceptance-blocking until a shared implementation result is available.

##### LA-en-B06-03: Bare ply hints lack a stated regional convention

- Classification: NEEDS_EVIDENCE. Category: FACT/CONTEXT. Severity: S2. State: BLOCKED.
- Member: `domain/model/StandardYarnWeight.kt`, all eight `ply` fields and their YarnWeightSelector display. Values: Lace 1–3; Fingering 4; Sport 5; DK 8; Worsted 10; Aran 10; Bulky 12; Super Bulky 14.
- Consequence: the bare “ply” suffix can be read as actual strand construction, while the code comment intends an AU/UK thickness hint. S13 shows different thicknesses with the same two-ply construction.
- Evidence: all eight values/callers read; S09 thickness categories and S13 actual manufacturer construction. No exact primary regional table establishing this complete mapping was available in the inspected material.
- Proposed correction: shared owner identifies and names the intended convention and separates its hint from construction, or removes unsupported hints. Keep canonical persisted labels unchanged. Do not invent a replacement number for Super Bulky.
- Checks/limits: StandardYarnWeightTest passes its existing contract; it does not establish regional factual provenance. All locales share these values. A new display resource or model change belongs to coordination.

##### LA-en-B14-01: Generated M1 instructions can require nonexistent bars

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S1. State: OPEN (shared implementation).
- Member: `domain/calculator/IncreaseDecreaseCalculator.kt`, flat easy sequence and increased-count boundary; instruction presentation in IncreaseDecreaseScreen.
- Reproduction using current compiled classes: `calculate(96,12,INCREASE,FLAT)` returns valid, 108 stitches, easy `(K8, M1) × 12`. The last M1 comes after the last original stitch at the flat edge. `calculate(1,3,INCREASE,FLAT)` returns valid with a warning and `M1 × 3, K1`; there are no three separate between-stitch bars before that first stitch.
- Consequence: stitch arithmetic is correct, but the stated operation cannot be followed as the app's M1 definition. Circular 96+12 has a next stitch across the round boundary and is not the same defect.
- Evidence: direct CalculatorProbe execution, S01 and independent S05. Existing tests verify count/sequence contracts, not physical availability of M1 bars.
- Proposed correction: shared owner distributes between-stitch M1 only where a bar exists and rejects/chooses a separately specified technique for impossible density/edge cases. Add concrete regressions for both reproduced inputs. Do not relabel M1 as KFB: that changes consumed-stitch semantics.
- Minimality/impact: cannot be repaired with an English sentence. All languages consume the same generated sequence. No shared algorithm/test change in this chat.

##### LA-en-B15-02: Double rounding can choose the less accurate cast-on repeat

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: OPEN (shared implementation).
- Member: `domain/calculator/CastOnCalculator.kt`, `calculate` → `calculateWithRepeat`.
- Reproduction: width 12.4cm, gauge 10 stitches/10cm, repeat 8, edges 0 yields exact 12.4, then rounded 12; the tie selects 8 stitches / 8cm. Sixteen stitches / 16cm is closer to the requested body width (3.6cm away versus 4.4cm).
- Evidence: source and direct execution of current compiled class. Baseline 20cm,20/10cm,repeat6,edges2 correctly gives 44 stitches / 22cm and remains separate evidence.
- Proposed correction: shared owner selects the nearest repeat using the unrounded exact body count, then adds total edge stitches, with a focused boundary regression and an explicit tie rule.
- Minimality/impact: English label now accurately says body width, but cannot repair numerical selection. All locales share the calculator. No calculation semantics changed here.

##### LA-en-B17-02: Japanese needle equivalents are materially shifted

- Classification: CONFIRMED_DEFECT for material mismatches; NEEDS_EVIDENCE for close approximate pairs. Category: FACT. Severity: S1. State: OPEN.
- Member: `domain/calculator/NeedleSizeData.kt`, Japanese column. Every cell is recorded separately in coverage.
- Reproduction: app pairs 4.5mm with JP7, 5mm with JP8, 5.5mm with JP9 and 6mm with JP10. Clover's actual values are respectively 4.2,4.5,4.8,5.1mm (S10). JP10 at 6mm is a 0.9mm difference. Earlier small deviations are not called exact errors without an approved approximation rule; they remain BLOCKED.
- Consequence: choosing a needle by the displayed number can change gauge materially. A general “approximate” caveat does not establish an arbitrary shifted sequence.
- Proposed correction: shared data owner specifies exact Japanese sizes versus nearest equivalent rows and updates the table/schema presentation accordingly; preserve valid manufacturer US differences. Add exact-source fixtures, not positivity/sortedness alone.
- Impact/checks: all locales share the cells. Existing NeedleSizeDataTest and English runtime table checks pass, but do not prove this column correct. No shared data edited.

##### LA-en-B17-03: Body-size cells and category mappings do not match their cited source

- Classification: CONFIRMED_DEFECT for mismatched cells; NEEDS_EVIDENCE for unsupported mappings/scalars. Category: FACT. Severity: S1. State: OPEN.
- Member: `domain/calculator/SizeChartData.kt`, all six tables; exact 145-cell comparisons and judgments in coverage.
- Examples: child size 2 chest is 53.5cm versus CYC 53; waist 51 versus 53.5; arm 24 versus 21.5. Women's XS bust 78.5 is outside CYC 71–76. Men's XL waist 101.5 is outside CYC 106.5–112. Baby age intervals are mapped to values without an explained relationship to CYC fixed-month columns. Head/hand adult S/M/L(/XL), preemie body cells, height series and numeric dress sizes need actual sources or explicit transformation rules.
- Consequence: a general “reference” caveat cannot make an incorrect or unsourced source attribution accurate. Some exact child cells match and are individually PASS; in-range adult scalars remain BLOCKED because CYC gives ranges, not that chosen single value.
- Evidence: S04 and full tables S16–S21. Each stored cm value was read; inch conversion is consistently cm/2.54 and does not cure the source mismatch.
- Proposed correction: shared owner chooses and documents the intended tables, category labels and range/scalar model; replace only verified values, add source-cell tests, and coordinate label changes across languages. Do not invent another universal size chart.
- Checks/impact: SizeChartDataTest/ReferenceDataSourceTest and rendered six-category tests verify current structure, not factual truth. Shared all-language data; unchanged here.

##### LA-en-B19-01: Some drawings do not support the stated chart-symbol meanings

- Classification: CONFIRMED_DEFECT for wrong/indistinguishable symbols; NEEDS_EVIDENCE for other variants. Category: FACT/CONTEXT. Severity: S1. State: OPEN.
- Members: `ChartSymbolData.kt` and drawing branches in `ui/screens/chartsymbols/ChartSymbolScreen.kt`; exact symbol IDs and English keys in coverage.
- Confirmed: `slip_knitwise` is drawn as a horizontal bar, a CYC purl convention, not evidence for slipping knitwise. `knit_tbl` uses the same simple chevron as KFB and lacks the CYC twisted-loop crossing; `purl_tbl` is a dot and short stroke rather than the twisted-loop symbol. Lifted-increase left/right drawings lean opposite their names. The 2/2 and 3/3 cable branches use identical two-line glyphs without distinct stitch width/count.
- Additional uncertainty: the exact M1L/M1R and triple-decrease graphical variants need a named legend/source; difference from one convention alone does not prove every alternate symbol wrong.
- Consequence: readers can perform the wrong stitch or infer the wrong cable width. Descriptions remain separately linguistically reviewed; “check your pattern's key” does not validate the app's own incorrect legend.
- Evidence: actual drawing code, ordinary English screenshot, S02 diagrams visually inspected. All 25 symbols were inspected, including correct empty-square knit, dot purl, YO circle, k2tog/SSK slashes, centered decrease, no-stitch square and self-explanatory marker/repeat signs.
- Proposed correction: shared drawing/data owner adopts a documented coherent legend, corrects only confirmed mismatches and makes cable counts visually distinguishable. Add focused drawable/legend checks and coordinate any new resources. No SVG/image or shared UI change here.

##### LA-en-B03-03: Compact widget clips the bottom of the count

- Classification: CONFIRMED_DEFECT. Category: RENDERING. Severity: S2. State: OPEN (shared layout).
- Members: `widget/CounterWidget.kt`, `SmallWidgetCount` (22sp, one line, around lines263–273) and compact120×48 layout. Existing `WidgetHostRuntimeTest.responsiveSizesRenderAndButtonWritesThroughReceiver` fails at line106: “Counter glyph bottom is visible”.
- Reproduction: owned English API36 emulator, font1.0, host120×48dp, synthetic long name and count0. `widget-120x48.png` independently inspected: the lower arc of zero is cut off. Name ellipsis is intentional and not this finding.
- Proposed correction: shared owner adjusts compact count/frame measurement so a full glyph fits, then reruns the existing host test including120×80,160×160,300×160 and receiver writes. Those later cases were not reached in this failed run. This is not the excluded320dp application check.
- Minimality/impact: neutral English widget metadata was corrected separately. A string change cannot repair glyph bounds; all locales share the layout. No widget code or test was edited.

##### LA-en-B06-04: Care glyphs lose meaning and labels are disconnected

- Classification: CONFIRMED_DEFECT. Category: FACT/CONTEXT/RENDERING. Severity: S2. State: OPEN (shared drawing/UI).
- Members: `ui/components/care/CareSymbolIcon.kt` lines72–84 and141–158; `CareSymbol.kt` all20 entries; `ui/screens/yarncard/YarnCardDetailScreen.kt` lines466–489.
- Reproduction: all20 selected symbols on a synthetic yarn card; `manual-care.png`, `manual-care-drying.png`, `manual-care-ironing.png` inspected. WASH_HAND is a two-line caret above a tub, without a recognizable hand. DRY_TUMBLE_NORMAL has overlapping dots: at28dp the centers are3.36dp apart, less than their combined4dp radii. The result resembles a joined figure8 instead of two separated dots.
- Source comparison: S07/S08 actual hand and two-dot drying conventions. Other18 drawing branches were individually inspected and retain their process meaning. This does not approve every regional use of legacy A.
- Context gap: the detail row renders only icons. The enum's `labelRes` values are not used by the row or its semantics. Corrected Celsius/P/F labels therefore do not reach this UI. This is source/ordinary visual evidence, not a TalkBack run.
- Proposed correction: shared owner draws a recognizable hand, separates the dots and connects the existing labels to meaningful visible/semantic context. Recheck the20 symbols at the existing ordinary size. No new English resources or care-system redesign needed.
- Impact: all locales share the drawings and label connection; unchanged here. Resource V is NOT_APPLICABLE for the unconsumed labels, with C FAIL carrying the missing connection.

##### LA-en-B16-03: Whole-skein mass is displayed with an uppercase G

- Classification: CONFIRMED_DEFECT. Category: RENDERING/CONTEXT. Severity: S3. State: OPEN (shared presentation).
- Members: `ui/screens/yarn/YarnEstimatorScreen.kt` around198 passes `total_weight` to `ui/components/BadgePill.kt:35`, which applies `localizedUppercase()` to the complete text.
- Reproduction:450m required,200m/50g skein. `manual-yarn-result.png` shows “WEIGHT OF WHOLE SKEINS: 150 G”. The correct resource contains lowercase g; numerical purchase mass150g is correct. S22 specifies unit-symbol case.
- Proposed correction: preserve the unit's case at this caller/presentation boundary while keeping the existing styling policy for ordinary labels. Do not work around it with a new translation or change quantity arithmetic. Adjust a focused shared assertion so it does not merely accept the fully uppercased resource.
- Impact: all languages using this badge share the transformation; unchanged here. The resource wording correction is complete, but this rendered case remains FAIL.


#### Initial validation log (historical only)

All checks below ran on2026-10-18. Raw evidence root: `reports/language-audit/en/20261018/` (ignored). Conclusions, inputs and limitations are retained here and in coverage; raw filenames are supplementary evidence.

| Check | Actual result | Boundary/evidence |
| --- | --- | --- |
| Initial Gradle command | Failed before tests because `--tests` followed assembleDebug | `gradle.log`; corrected invocation below, no test result inferred |
| TEST1: processDebugResources, assembleDebug, testDebugUnitTest, isolatedTest=notes,37 explicit class filters | BUILD SUCCESSFUL;245 tests,37 classes,0 failures/errors/skips;4m10s | `gradle-args.json`, `gradle-corrected.log`, copied XML `unit-results-first/`. Before only the final imperial-help edit. Includes calculation, numeric presentation, counter unit, Pro, web/Ravelry, resource/plural, abbreviation/needle/size/chart contracts. These tests do not establish external craft facts. |
| TEST2: assembleDebug, assembleDebugAndroidTest, testDebugUnitTest, isolatedTest=activepolicy | BUILD SUCCESSFUL;3 tests in2 classes,0 failures/errors/skips;3m57s | `gradle-device-args.json`, `gradle-device.log`; final19 edits compiled. ReferenceResourceLocalizationTest2, CountPluralResourcesSourceTest1. Repeated resource checks, not3 additional unique tests over TEST1. |
| CALC: current compiled app calculators through temporary Java probe, Temurin17 | Baseline metric55, imperial44, repeat44/22cm, purchase3/150g verified; M1 and double-rounding defects reproduced | `CalculatorProbe.java`, `calculator-probe.log`;11 cases in coverage. An initial JAVA_HOME-based launch failed without execution; explicit JDK path succeeded. Console multiplication-sign encoding artifact is not an app defect. |
| Initial selected Android instrumentation |33 reported results:30 passed,2 UI methods failed,1 class initialization error;359.522s | `instrumentation.log`, method inventory below. The wrong package for ProjectYarnUsageRuntimeTest was an invocation mistake, not an application failure. |
| Existing ProjectYarnUsageRuntimeTest in correct package |2 passed;18.709s | `instrumentation-yarn-retry.log`. First retry was blocked by the CLI's UiAutomation registration; stopped the owned CLI instrumentation and reran successfully. Real isolated Room, UI edit/recreation, completion/reopen, source deletion, zero and unchanged stash/file sentinels. No mock database. |
| Existing p1146 fixture recreation |1 passed;18.024s (repeat of an already passed method) | `instrumentation-fixture-recreate.log`. An earlier temporary manual fixture transfer incorrectly piped binary DB bytes through adb shell, corrupting only the disposable app's synthetic database; a reset attempt failed. Recreated only that owned fixture and replaced transfer with adb push/run-as copy plus integrity_check. This was an operator error, not an app regression. |
| Existing WidgetHostRuntimeTest |1 failed: Counter glyph bottom is visible | `instrumentation-widget.log`, `widget-120x48.png`; independently confirmed LA-en-B03-03. Remaining sizes and receiver writes not reached. |
| Final structural checks | PASS: unchanged keys/types/plural variants/placeholder identities;19 values changed;well-formed XML;git diff --check | Final checks also compare original shared-input hashes and Git locale paths. No shared source/test or other locale edits. |

Android total is32 distinct passing methods after the corrected yarn run, including resource-format assertions as well as actual UI/Room tests. It is not an all-green suite: the two initial UI methods remain failed in their recorded run, and the widget failure is open. The repeated p1146 pass is not counted as another unique test.

Final APK SHA-256:

- App: `180b9939a6638e6f66b63b29d477107b675f26629193de0fb4c331b09f885af5`.
- Test: `5c1cf61f0d26575de7f51c0d9df36507e488d52a617bf859bb390a25d62147fc`.

##### Exact executed test inventory

| Unit-test class | TEST1 count | TEST2 count |
| --- | ---: | ---: |
| com.finnvek.knittools.data.datastore.AppLanguageMigrationSourceTest | 2 | — |
| com.finnvek.knittools.data.datastore.AppLanguageTest | 4 | — |
| com.finnvek.knittools.domain.calculator.AbbreviationDataTest | 6 | — |
| com.finnvek.knittools.domain.calculator.CastOnCalculatorTest | 17 | — |
| com.finnvek.knittools.domain.calculator.ChartSymbolDataTest | 6 | — |
| com.finnvek.knittools.domain.calculator.CounterValueFormatterTest | 16 | — |
| com.finnvek.knittools.domain.calculator.DurationDisplayFormatterTest | 6 | — |
| com.finnvek.knittools.domain.calculator.IncreaseDecreaseCalculatorTest | 21 | — |
| com.finnvek.knittools.domain.calculator.InstructionParserTest | 39 | — |
| com.finnvek.knittools.domain.calculator.LocaleNumberFormatterTest | 2 | — |
| com.finnvek.knittools.domain.calculator.MeasurementCalculatorTest | 20 | — |
| com.finnvek.knittools.domain.calculator.MeasurementNumberFormatterTest | 6 | — |
| com.finnvek.knittools.domain.calculator.MeasurementNumberParserTest | 12 | — |
| com.finnvek.knittools.domain.calculator.MinutesPerRowFormatterTest | 5 | — |
| com.finnvek.knittools.domain.calculator.NeedleSizeDataTest | 10 | — |
| com.finnvek.knittools.domain.calculator.ReferenceDataSourceTest | 1 | — |
| com.finnvek.knittools.domain.calculator.SizeChartDataTest | 11 | — |
| com.finnvek.knittools.domain.calculator.YarnEstimatorTest | 8 | — |
| com.finnvek.knittools.domain.calculator.YarnUsageCalculatorTest | 5 | — |
| com.finnvek.knittools.domain.model.StandardYarnWeightTest | 2 | — |
| com.finnvek.knittools.InstructionHintLocalizationTest | 2 | — |
| com.finnvek.knittools.MeasurementsLocalizationTest | 2 | — |
| com.finnvek.knittools.PatternReadingAssistanceLocalizationTest | 1 | — |
| com.finnvek.knittools.ProjectDocumentLocalizationTest | 1 | — |
| com.finnvek.knittools.ProLocalizationSourceTest | 3 | — |
| com.finnvek.knittools.RavelryLocalizationSourceTest | 2 | — |
| com.finnvek.knittools.ReferenceResourceLocalizationTest | 2 | 2 |
| com.finnvek.knittools.ui.components.LocaleFormattingTest | 4 | — |
| com.finnvek.knittools.ui.components.MainCounterTargetStatusTest | 7 | — |
| com.finnvek.knittools.ui.CountPluralResourcesSourceTest | 1 | 1 |
| com.finnvek.knittools.ui.LocaleSensitiveFormattingSourceTest | 4 | — |
| com.finnvek.knittools.ui.ProjectLocalizationSourceTest | 3 | — |
| com.finnvek.knittools.ui.screens.counter.CounterStringResourcesSourceTest | 7 | — |
| com.finnvek.knittools.ui.screens.insights.InsightsRowFormattingTest | 1 | — |
| com.finnvek.knittools.ui.screens.needles.NeedleSizeLabelTest | 2 | — |
| com.finnvek.knittools.ui.screens.sizecharts.SizeChartScreenSourceTest | 1 | — |
| com.finnvek.knittools.WebPatternLocalizationArchitectureContractTest | 3 | — |

| Android method | Recorded result |
| --- | --- |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1162_sessionDeleteFailureKeepsRowAndAllowsRetry | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1150_counterControlsWriteOnlyIntendedValues | FAIL |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1178_libraryAllDestinationsReachable | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1317_backupPrivacyAndSettingsLanguageSheetAreReachable | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1174_projectGaugeCopiesResultWithoutChangingProject | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1206_toolsAllFiveLaunchers | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1154_galleryViewerAndEmptyState | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1142_projectListFoldersCreationAndEmptyState | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1186_savedPatternDetailActions | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1190_libraryPdfReadingMarkupLayersAndExport | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1316_proWordingRemainsReachableWithoutStartingAPurchase | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1162_sessionHistoryDeleteConfirmation | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1146_overviewSectionsAndActionSheet | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1158_corruptProjectPdfShowsErrorAndKeepsNavigation | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1182_savedPatternsSelectionAndEmptyState | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1700_backupRestoreThroughDocumentPickerThenNewProjectAndSession | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1158_projectPdfReadingMarkupLayersAndExport | FAIL |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1194_myYarnListCreationSelection | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1166_counterHistoryRowsAndLiveRemoval | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1170_notesImeSaveAndRecreation | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1198_yarnDetailQuantityStatusAndProject | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1315_weeklyChartDescriptionAndActionsUseTheSelectedLanguage | PASS |
| com.finnvek.knittools.ui.AuditPart08RuntimeMatrixTest.p1202_allPhotosFilterViewerRemoval | PASS |
| com.finnvek.knittools.ui.AuditPart14ToolsRuntimeTest.increaseAndDecreaseModesReplaceTheVisibleTotal | PASS |
| com.finnvek.knittools.ui.AuditPart14ToolsRuntimeTest.castOnRendersSnappedCountAndDropsInvalidResult | PASS |
| com.finnvek.knittools.ui.AuditPart14ToolsRuntimeTest.everySizeCategoryDisplaysItsOwnHeaderAndFirstRow | PASS |
| com.finnvek.knittools.ui.AuditPart14ToolsRuntimeTest.abbreviationQueryAndExpansionSurviveCraftContextChange | PASS |
| com.finnvek.knittools.ui.AuditPart14ToolsRuntimeTest.yarnPurchaseWeightAndChooseActionKeepCurrentInputs | PASS |
| com.finnvek.knittools.ui.screens.needles.NeedleSizeRuntimeTest.correctedReferenceIsVisibleInDarkTheme | PASS |
| com.finnvek.knittools.ui.screens.needles.NeedleSizeRuntimeTest.correctedReferenceIsVisibleInLightTheme | PASS |
| com.finnvek.knittools.ui.screens.counter.YarnUsageLocalizationTest.fractionalSkeinCountUsesRegionalPluralRules | PASS |
| com.finnvek.knittools.ui.screens.counter.YarnUsageLocalizationTest.roundedSkeinCountMatchesDisplayedQuantity | PASS |
| com.finnvek.knittools.ui.ProjectYarnUsageRuntimeTest.initializationError | FAIL |
| com.finnvek.knittools.ui.screens.counter.ProjectYarnUsageRuntimeTest.linkedCardEntryUsesTheSamePersistenceAndKeepsGlobalQuantity | PASS |
| com.finnvek.knittools.ui.screens.counter.ProjectYarnUsageRuntimeTest.realCounterYarnFlowPreservesPairAcrossRecreationCompletionAndSourceDeletion | PASS |
| WidgetHostRuntimeTest.responsiveSizesRenderAndButtonWritesThroughReceiver | FAIL; confirmed compact glyph clipping |
| AuditPart08RuntimeMatrixTest.p1146_overviewSectionsAndActionSheet (fixture recreation repeat) | PASS; already included in distinct count |

##### Failed-method investigation

- `p1150_counterControlsWriteOnlyIntendedValues`:42→43→42 and undo43 assertions completed; the custom-step flow remained in its sheet and the later count assertion failed. Independent manual UI saved step5 and changed3→8 correctly (`manual-round8-step5.png`). This verifies that interaction, not every unexecuted assertion in the failed method.
- `p1158_projectPdfReadingMarkupLayersAndExport`: after opening SAF in a nested folder, one Back moved to the parent rather than closing the picker. Manual cancellation with a second Back returned to the project PDF and Next page reached2/3 (`manual-project-pdf-cancel.png`). A separate actual library export saved a53,514-byte,3-page annotated PDF under the owned emulator Download/KnitToolsAudit folder and returned to the reader (`manual-pdf-after-export.png`). Neither manual result rewrites the test log as passed.
- Markup toolbar candidate rejected: the lower actions are inside the intended scrollable toolbar (`PatternAnnotationToolbar.kt`, heightIn plus verticalScroll). `manual-markup-scrolled.png` shows Undo/Redo/Clear page fully after scrolling; the initial partial viewport is not recorded as a clipping defect.

##### Required ordinary context matrix

All screenshot paths below are relative to the raw root. Exact individually inspected image paths are retained in `reviewed-screens.json`; uninspected captures do not earn a PASS. Standard configuration411dp/font1.0, English app/system configuration `en,en-US`. Screenshot metadata/hierarchy was checked together with pixels where available.

| Dimension / batches | Inputs and actual observation | Remaining boundary |
| --- | --- | --- |
| Craft and selected main unit B02/B03 | Knitting ROWS in existing tests; crochet ROUNDS0/1/2/3; explicit crochet ROWS1; knitting REPEATS2; CUSTOM Motifs1234. Screens `manual-round0/1/2/3`, `manual-crochet-rows`, `manual-repeats`, `manual-custom`. | Non-main row-specific consumers have LA-en-B03-02; main-slot success does not approve them. |
| Counts, targets, steps B03/B04 | Target2:2 rounds left,1 round left,Target reached,1 round past target; step5 saved in UI,3→8 gives6 rounds past. Large custom hero1,234, name retained. Repeating, shaping and repeat-section forms inspected in `manual-repeating`, `manual-shaping`, `manual-repeat-section`; reminder form in `manual-reminder`. | Selected-unit values were seeded into the owned synthetic DB for presentation, except actual creation/default and step interaction. No claim that all counter-edit save branches ran. |
| Names B02/B03/B08/B12 | Short Cable scarf; long `Zoé – Autumn blanket: cable border & cuffs`; actual custom label Motifs. Full names in overview/filter, intentional one-line title ellipsis; accents/punctuation retained. Notes and photo-rename IME states inspected. | Arbitrary user content is not translated; display truncation in the configured title is not a forced text mutation. |
| Quantities B07 | `manual-usage-unknown`:blank values,Remaining unknown,disabled Save. Entered400m allocated/450m used:Over by50m; yards54.68yd. Without basis, Grams shows positive-length/weight requirement. Entered200m/50g basis:allocated100g,used112.5g,over12.5g/50m/0.25skeins. Skein mode2/2.25. Zero used gives2skeins/400m/100g remaining. Saved and reopened:Used0m,Remaining400m. | `manual-usage-*`8 images plus delete dialog. Real stash preservation separately asserted by existing Room/UI tests. No user stash used. |
| Numeric input B07/B13–B16 | Incomplete `.` shows Enter a valid amount and Correct the amount fields before changing units (`manual-usage-invalid`). Exact long yard values remain in editor; rounded result54.68yd is separate. Gauge calculate/convert/measure/copy layouts inspected; test metric/imperial conversions and numeric boundary cases ran. | Literal composed examples in coverage are not screenshot evidence. Locale-specific tests may contain other locale fixtures, but those outputs receive no language approval here. |
| Calculation results B14–B16 | Android test17+3 displays20st, circular mode retains20, decrease displays14;invalid17 removes output. Cast-on test10cm,23/10cm,repeat4,edges2 displays26st;blank gauge removes result. Yarn test501m,200m/50g displays150G and0length removes output; manual450 gives3skeins/exact2.25/150G with qualified advice. | Shared M1/double-rounding issues remain. Unit-case G is an observed shared failure even though the existing test accepts uppercase. JVM55/44/repeat44 cases are calculation proof only. |
| Empty/populated/error/retry B01/B02/B05/B06/B12 | Existing folder/create/list, overview/actions, notes, photo, saved-pattern and yarn states inspected. Global Insights empty correction in `manual-insights-no-sessions`; filtered empty in `manual-insights-empty`. Session delete failure/retry and yarn status-write failure shown and tests passed. | Other exact error/conflict/recovery/provider states are V BLOCKED individually; source/caller review does not simulate them. |
| Web/PDF B08–B11 | Manual web form correctly saved Cable scarf/example.com local metadata (`manual-web-saved`). Existing saved-pattern detail/attach/selection, project/library PDF, corrupt PDF error, empty bookmarks, guide/layer/markup actions inspected. Reader page1→2 of3, export/cancel recovery described above. | No live Ravelry login/download/network errors, camera/photo-provider permission flows or every bookmark/destructive confirmation state. These specific keys remain V BLOCKED. External website text excluded. |
| Reference content B06/B17–B19 | All20 care icons inspected across3screens. Needle ordinary light screenshot and two existing light/dark tests; size-category test verifies6 headers/first rows, women's table screenshot inspected; chart diagram screenshot inspected. SSK/S2KP/M1/WYIF correction detail screenshots, WYIB actual hierarchy; all77 texts independently read, longest M1 wraps in shared AbbreviationRow. | Every numeric/reference item was fact-checked independently; exact unobserved cells/scroll positions retain V BLOCKED. Wrong/uncertain data or glyphs remain F/C FAIL/BLOCKED regardless of layout. |
| Time B03/B05/B23 | Counter history Oct18,2026 and9:14:37PM,42→43; Insights2h3min/82rows and1min/row; actual session Stop dialog0:00:20,Save/Discard/Cancel (`manual-session-stop`). Unit tests cover0/1/59/60/61 and multi-hour decomposition; range formatting source traced. | Runtime midnight/DST/reboot recovery/date-boundary states not exercised; no invented result. Existing session anchors were not manipulated to fake elapsed time. |
| Pro/trial B22 | Existing local policy fixture:free/trial14days,active,failed,ended,preservation wording; screenshots inspected. | No live Play price/purchase/restore-account transaction. Debug test policy is not actual purchase state. |
| Backup B21 | Description/privacy/exclusions and actions visibly inspected; existing p1700 performs local SAF restore, creates a project and saves session successfully. | Not every corrupt/space/write/failure phase was forced; exact preview/progress states without inspected pixels remain V BLOCKED. |
| Widget B03 | Existing host120×48 capture visibly clips count0;test fails. | LA-en-B03-03 open;larger sizes and receiver writes not reached. |

All20 care glyphs, all25 chart drawings, all100 needle cells, all145 size cells and all77 abbreviation tokens were individually inspected in source/factual work. Only repeated layout structure was reused for rendering; no table/technique/fact sampling was used.

##### Process and scope cleanup

The owned emulator was started as PID40720 with child25636 for AVD `KnitToolsLanguageEn20261018` / serial `emulator-5580`. The owned Android CLI instrumentation was stopped and `adb -s emulator-5580 emu kill` was issued after the final capture. Final verification checks both PIDs and device absence. No unrelated device/process was terminated. Gradle/test commands had already exited; no watcher/server remains. Raw evidence and the disposable AVD files are retained, with no running emulator.

`EXCLUDED_BY_PROJECT_RULE`:320dp,200%font scale,their combination,TalkBack. No restricted quality wrapper, dependency update, real account/purchase, Numeris read, or production-data change. Native review remains NOT_REQUESTED.



##### Ordinary runtime interpretation

Owned disposable AVD `KnitToolsLanguageEn20261018`, serial `emulator-5580`, Android 16/API 36, Pixel 6 profile, 1080×2400px, width 411dp, font scale 1.0. App locale set through PreferencesManager/AppCompat to English; captured configuration `en,en-US`. Light theme is primary; existing needle test additionally checks dark theme. Isolated package `com.finnvek.knittools.activepolicytest`, version 1.0.0; no production app data. The ProPolicy test fixture simulates entitlement/trial state locally and disables real billing; it is not proof of a real purchase. Tests may recreate their own synthetic database.

The run uses existing AuditPart08RuntimeMatrixTest methods, AuditPart14ToolsRuntimeTest, NeedleSizeRuntimeTest, YarnUsageLocalizationTest and ProjectYarnUsageRuntimeTest. Method names and actual results are recorded, not inferred from a Gradle build. Screenshots were inspected directly; a method's successful assertion is not automatically a screenshot pass. The wrong-field URL entry in the existing web-editor fixture proves only the form. A separate manual Save of Cable scarf with https://example.com/pattern succeeded and its metadata detail was inspected (manual-web-saved.png); no website was opened.

`EXCLUDED_BY_PROJECT_RULE`: 320dp, 200% font scale, their combination, TalkBack. None rerun. Accessibility copy and Compose semantic descriptions were still read. Restricted quality wrappers were not invoked. No new dependencies or permanent test infrastructure.

### Native-speaker review

- Status: **NOT_REQUESTED**. No human/native reviewer was contacted or invented.
- This is a source-based English language/factual/context review with the recorded technical and device evidence. No claim of native craft-expert sign-off. Absence of a native review alone is not the reason for OPEN; the concrete shared defects, failed test and missing required evidence are.

### Current closeout

**REVIEW_COMPLETE / OPEN for English B01–B23; not AUDIT_PASS.** Current inventory2,315 items,1,349 translatable keys,1,382 resource variants. Three new English values corrected and checked; original19 corrections preserved. Eight of ten shared findings verified; two partially resolved with exact remaining handoffs B03-04 and B19-02. B03-05 records the stale test expectation separately. All required applicable text/fact/context/resource work is recorded; known factual/context failures and 1177 unobserved visual contexts remain visible.

| Dimension | PASS | FAIL | BLOCKED | NOT_APPLICABLE |
| --- | ---: | ---: | ---: | ---: |
| T | 2315 | 0 | 0 | 0 |
| F | 2299 | 5 | 0 | 11 |
| L | 2315 | 0 | 0 | 0 |
| C | 2306 | 9 | 0 | 0 |
| R | 1879 | 0 | 0 | 436 |
| V | 1069 | 5 | 1177 | 64 |

Counts describe evidence dimensions, not percentages of language quality. V PASS includes the explicitly described identical-layout reuse; all reference cells and technique meanings were individually checked. No factual sampling or test-only terminology approval. Current hashes are in `input-hashes.json`; coverage is the exact item list for every batch. This report is authoritative for the English follow-up; coordination-owned LANGUAGE_AUDIT.md/shared-fixes.md still describe their earlier snapshot and need the owner to incorporate these new handoffs.

Cleanup: owned emulator PID34752 and its qemu child40692 stopped with `adb -s emulator-5580 emu kill`; its remaining console child38928 was confirmed to have parent34752 and stopped. Final identity check confirms those owned processes and emulator-5580 are absent. PID40692 was subsequently reused by an unrelated svchost process; it was not touched. A separate physical device appeared in adb during closeout and was not used. Android CLI instrumentation stopped and the owned browser tab closed. Gradle commands used --no-daemon and completed. No temporary watcher/server retained; raw evidence and disposable AVD files remain. No commit, push or PR.

### Resume point

English-only post-fix review recorded through B23. Coordinate the two remaining shared semantic/drawing corrections and the stale ordinary test, then recheck only their changed English consumers. Required pending V contexts are listed per caller in coverage; do not silently carry them to PASS or restart already accepted unchanged content. Native review remains NOT_REQUESTED. All other languages remain outside this chat's approval.
