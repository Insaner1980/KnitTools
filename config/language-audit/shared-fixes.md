# Shared language-audit corrections

## LA-es-B05-58 — relative month quantity selection (2026-10-19)

**SHARED_FIX_VERIFIED / SPANISH_VERIFIED. Spanish AUDIT_PASS; native review NOT_REQUESTED.** This scope fixes the sole Spanish content blocker. The preceding Spanish OPEN result remains in `es/review.md` as history.

Both `InsightsProjectRow` and `InsightsLastWorkedNote` use `InsightsSections.kt::relativeDayText`. Its old month `stringResource` formatted `hace 2 mes` for 60 days. Only that branch now uses `months = days / 30` and `pluralStringResource(R.plurals.relative_time_months_ago, months, months)`. Date thresholds, timezone/date conversion and day/week arithmetic are unchanged.

Exact resource scope: `relative_time_months_ago` changes from **string to plurals** in the eleven `app/src/main/res/values{,-fi,-sv,-nb,-da,-de,-nl,-fr,-it,-es,-pt}/strings.xml` files. Seven locales declare one/other; es/fr/it/pt declare one/many/other, matching their existing declared forms. All other resource bytes are unchanged. Each form retains `%1$d`; only Spanish plural wording changes. No abbreviations are expanded.

| Locale | one | many | other |
| --- | --- | --- | --- |
| en (default) | `%1$dmo ago` | not declared | `%1$dmo ago` |
| da | `%1$dmdr siden` | not declared | `%1$dmdr siden` |
| de | `vor %1$d Mon` | not declared | `vor %1$d Mon` |
| es | `hace %1$d mes` | `hace %1$d meses` | `hace %1$d meses` |
| fi | `%1$d kk sitten` | not declared | `%1$d kk sitten` |
| fr | `il y a %1$d mois` | `il y a %1$d mois` | `il y a %1$d mois` |
| it | `%1$d m fa` | `%1$d m fa` | `%1$d m fa` |
| nb | `for %1$dmnd siden` | not declared | `for %1$dmnd siden` |
| nl | `%1$dmnd geleden` | not declared | `%1$dmnd geleden` |
| pt | `há %1$d m` | `há %1$d m` | `há %1$d m` |
| sv | `%1$d mån sedan` | not declared | `%1$d mån sedan` |

Inventory delta per locale: one fewer string and one more plural key; key totals stay unchanged. Seven two-form locales gain one resource variant each; four three-form locales gain two each (**15 net variants, 26 new plural items replacing 11 strings**). Spanish now has 1,349 keys, 1,413 resource variants and 2,110 current coverage rows. Two failed rows are retained as retired history. Other-locale inventory/coverage/hash snapshots are left intact as historical evidence; this explicit mapping accounts for the migrated key without silently renewing other-language reviews. No non-Spanish wording changes or new content defect is asserted.

Validation actually executed: Android `processDebugResources`; 6/6 fresh JVM tests in ReferenceResourceLocalizationTest, CountPluralResourcesSourceTest and InsightsCopySourceTest; isolated debug and Android-test APK builds; scoped ktlint over the four changed Kotlin files; 1/1 targeted Android Compose test on Pixel 9 / API 37. In both real last-worked components, 30/59/60/90 days give `hace 1 mes` / `hace 1 mes` / `hace 2 meses` / `hace 3 meses`. Day/week controls at 0/1/6/7/29 days and the declared Spanish plural resource at quantity 1000000 pass. Byte comparison confirms only the monthly caller branch and single resource element per locale changed. See `es/month-fix-verification.json` for exact forms, hashes, checks and historical evidence; raw logs/XML/APK hashes are in `reports/language-audit/es/20261019-month-plurals/`.

Spanish independently reread the three forms and both full sentences. All 234 prior input hashes matched at entry; only this defect's two coverage failures remained. Updated section 16 reconciliation grants AUDIT_PASS with no remaining content blocker. Native review remains NOT_REQUESTED. All V statuses remain unchanged. All English audit records, including the 231 supplementary rendering cases and their registries, remain byte-identical. No screenshots, excluded UI checks, other language audit, commit, push or PR. No remaining follow-up for this defect.

---


## LA-en-B05-01 — bounded session-zone activity dates (2026-10-19)

**SHARED_FIX_VERIFIED / ENGLISH_VERIFIED.** English now meets revised section 16: **AUDIT_PASS; native review NOT_REQUESTED**. This new decision supersedes the older acceptance wording below, not its historical evidence. Other locales retain their existing audit status; they must independently read the affected compositions in their assigned audit. No translation is changed or approved here.

Production reproduction uses the report's three explicit past-instant examples through the real Kotlin ViewModel/accumulator/SessionMetrics pipeline. Before the correction, `InsightsSessionZoneRangeTest` reports 4 failures (the three reported cases plus the same issue in fixed ranges) and a passing ordinary/project-filter control. Retained log/XML: `reports/language-audit/shared/20261019-b05/reproduction.log` and `reproduction-results/`.

The accumulator admitted future **calendar dates in stored session zones**, while the denominator and display end use device today. The All Time first date was also passed through after the display end, contrary to the chart's existing bounded start. The fix applies the displayed upper bound when accumulating `activeDates`, and bounds the ViewModel's presentation first date to today. It changes the underlying date set, not a formatted count. The chart's today-ended range, project queries/filters, completed records, stored zones, exact seconds and total duration rules remain intact. Historical row/pace output stays suppressed.

Verified examples: Oct 1–Oct 1 gives **Active 1 of 1 day**, 60 minutes; Oct 1–Oct 2 gives **Active 2 of 2 days**, 90 minutes; the lone stored Nov 1 session at device Oct 31 retains its original date and 30 minutes but produces Oct 31–Oct 31, active 0 / days 1 and **Oct 2026**, never a reversed Nov→Oct range. Same-zone Sep 30–Oct 2 remains **Active 3 of 3 days**, **Sep 2026 – Oct 2026**, 90 minutes. Week/month and selected-project controls are preserved.

Fresh final validation: **107/107 JVM tests in 11 suites PASS**, including the five focused regressions and relevant existing exact-second/stored-zone, ViewModel, accumulator, chart, date-pattern and historical-unit tests. Scoped ktlint PASS. The test composes unchanged English plural/range/chart-description resources using production state and source-checks their actual Compose wiring. This is JVM producer/resource composition plus source verification, not Android ICU, rendered pixels or spoken-output testing. No concrete remaining device-only question exists for this defect. See `fixed-tests.log`, `fixed-results/`, `test-arguments.txt`, `test-summary.json` and `ktlint.log` in the evidence directory.

English independently re-read only the affected six factual/context rows: both `insights_days_meta_format` variants, all three chart accessibility templates and All Time `insights_range_format#site2`. They now have F/C PASS, retaining their previous failure notes and unchanged V statuses. The current complete acceptance review is reused for unchanged content; tests alone do not grant the verdict. Updated report/coverage/input hashes and the root English summary record this decision. **All 231 supplementary rendering cases and both registries remain unchanged.** No real data, locale wording, dependencies, additional feature, other audit/language, commit, push or PR. Task-owned Gradle processes exit; no emulator or excluded UI check is started.

---

Work dates: 2026-10-18–19 (Europe/Helsinki). Scope: the ten original shared English handoffs, the two failed AuditPart08 methods, and follow-up findings LA-en-B03-04, LA-en-B19-02, LA-en-B03-05 and LA-en-B08-01. This is a shared correction report, not English language acceptance.

**Historical shared status before B05-01 repair (2026-10-19):** English independently VERIFIED all ten original shared findings and B03-04/B19-02/B03-05. The latest B08-01 measurement conflict is a confirmed test defect, corrected and verified below; all six methods using or testing the readability helper have passing results. English remains REVIEW_COMPLETE / OPEN because its ordinary rendering evidence is incomplete. The dated results below retain their original scope and are not retroactively renamed as current acceptance.

Initial branch `codex/audit-korjaukset-20261018`, HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. Existing work: 19 English string corrections, untracked LANGUAGE_AUDIT.md and config/language-audit/en. Their original files/diff/hashes were retained under `reports/language-audit/shared/20261018/`. Original audit evidence remains under `reports/language-audit/en/20261018/`.

## Evidence and decisions

All ten original findings and the relevant coverage rows were read. Claims were checked against current callers/data/drawing branches. The original calculator probe log, widget failure stack and bitmap, care screenshots, and both UI failure logs/screens were inspected separately from new validation.

Primary sources checked in this task:

- [Purl Soho M1 tutorial](https://www.purlsoho.com/create/make-one-right-m1r-make-one-left-m1l/): M1 uses the strand between stitches; adjacent increases need distinct bars. Circular joining differs from a flat edge.
- [Clover 2026 specification, page 1](https://clover.co.jp/recipe/takumikikakus.pdf): numbered JP 0–15 = 2.1–6.6 mm in 0.3 mm steps. Downloaded PDF and rendered page retained in shared raw evidence. Web PDF rendering failed; the original downloaded PDF was then inspected locally.
- CYC [baby](https://www.craftyarncouncil.com/standards/baby-size-chart), [child/youth](https://www.craftyarncouncil.com/standards/child-youth-sizes), [women](https://www.craftyarncouncil.com/standards/woman-size), [men](https://www.craftyarncouncil.com/standards/man-size), [head](https://www.craftyarncouncil.com/standards/head-circumference-chart), [hand](https://www.craftyarncouncil.com/standards/hand-size-charts): retain the published body categories and cm cells/ranges; do not invent scalar midpoints, dress sizes, height series, or preemie body measurements. Inches remain derived from cm/2.54, not a transcription of CYC's independently rounded inch cells.
- [CYC chart legend](https://www.craftyarncouncil.com/standards/knit-chart-symbols): original twisted-loop, directional M1/lifted increase, double decrease and version-1 cable images individually inspected and retained. Differences from this convention alone do not establish that every alternative is wrong; corrections explicitly adopt this convention.
- [CYC care symbols](https://www.craftyarncouncil.com/standards/care-symbols): hand in a tub and separate drying dots.
- [Brooklyn Tweed yarn specifications](https://brooklyntweed.com/pages/yarn): Shelter (worsted) and Loft (fingering) are both 2-ply construction. This does not establish or refute every AU/UK thickness mapping. Unsupported bare hints are removed rather than replaced with invented numbers.
- [NIST unit-symbol rules, 6.1.2](https://www.nist.gov/pml/special-publication-811/nist-guide-si-chapter-6-rules-and-style-conventions-printing-and-using): preserve unit-symbol case.

## Finding tracker

| Finding | Verified cause and scoped correction | Status |
| --- | --- | --- |
| LA-en-B14-01 | Flat easy M1 could land after the last stitch; dense inputs could reuse nonexistent bars. Reserve a final stitch and reject unavailable-bar requests. Decrease semantics unchanged. | SHARED_FIX_VERIFIED; locale recheck required |
| LA-en-B15-02 | Nearest repeat used an already rounded count. Compare exact body count; ties retain the lower usable repeat; add edges afterward. | SHARED_FIX_VERIFIED; locale recheck required |
| LA-en-B17-02 | JP column mixed shifted and unexplained approximate equivalences. Put all 16 exact Clover sizes on their own metric rows; keep US/UK values. | SHARED_FIX_VERIFIED; locale recheck required |
| LA-en-B17-03 | Wrong cells and unsupported categories. Restore source ranges/categories; omit unsupported baby head/length, child height, preemie body and men's XS. | SHARED_FIX_VERIFIED; locale recheck required |
| LA-en-B19-01 | Adopt CYC right-side glyphs. Remove unsupported knitwise-slip entry, distinguish twisted loops, correct direction/branch geometry, draw four/six-cell cable crossings. | SHARED_FIX_VERIFIED; locale recheck required |
| LA-en-B03-02 | Historical rows fields store main-counter values without a unit snapshot. Neutral counter wording avoids relabeling history. Follow-up B03-04 suppresses historical row/pace metrics regardless of the editable current label. | SHARED_FIX_VERIFIED; locale recheck required |
| LA-en-B03-03 | Original 120×48 bitmap and text bounds show a clipped zero. Remove unnecessary small-widget vertical padding. Do not weaken host bounds assertions. | SHARED_FIX_VERIFIED; locale recheck required |
| LA-en-B06-03 | Regional thickness provenance remains unverified. Remove all eight ply hints; canonical saved labels unchanged. | SHARED_FIX_VERIFIED; locale recheck required |
| LA-en-B06-04 | Hand was a caret; two drying dots overlapped; labels unused in detail. Draw hand/separate dots and expose existing labels visibly and in semantics. | SHARED_FIX_VERIFIED; locale recheck required |
| LA-en-B16-03 | BadgePill uppercased g. Add an opt-out used by whole-skein mass; other badges retain existing styling. | SHARED_FIX_VERIFIED; locale recheck required |

## UI-test investigation

- `p1150_counterControlsWriteOnlyIntendedValues`: the actual retained failure is `Could not scroll to Project actions` at line 424, not a failed later count assertion. Increment/decrement/undo and unchanged step=1 checks had passed. The retained `missing-Project_actions.png` visibly shows the step editor still open with draft 3. The test's keyboard/back assumption failed to dismiss the sheet. Explicit Cancel plus asserting dismissal preserves the cancellation check. Add a real Save path and verify step/count persistence separately.
- `p1158_projectPdfReadingMarkupLayersAndExport`: timeout at `exerciseReader` line 537, still in the external document picker. One Back navigated its folder hierarchy. Cancellation now waits for the external DocumentsUI window, backs out only while that package owns the active window, then asserts reader return and page 2/3. The first fresh run also exposed a separate readiness race at the first Next page action: controls were visible while totalPages was still loading. The production button is correctly disabled until currentPage < totalPages - 1. The test now awaits its known three-page fixture, checks 1/3 → 2/3 → 1/3, and retains the enabled-button assertion. This adds readiness and page-result checks rather than weakening them. Original manual cancellation/export evidence is not an automated pass.
- The wrongly qualified `ProjectYarnUsageRuntimeTest` initialization failure was a command-selection error already corrected in the original audit. It is not one of the two requested failed methods or an app defect.

## Validation

Final targeted unit run: **278/278 passed**, 35 suites, no failures/errors/skips (`build-unit-3.log`, `unit-final-summary.json`, retained XML in `unit-final/`). Changed Kotlin files passed scoped ktlint. All 16 distinct selected Android methods have passing results as detailed below. Final APK packaging check is recorded in Closeout. No 320 dp, 200% font scale, combined case or TalkBack check was run. No migrations, dependency changes, commits, pushes or PRs.

## Required locale rechecks

All eleven locales must recheck affected B03/B04/B05/B06/B10/B12/B14/B15/B16/B17/B19 contexts against the final files. Only resources required by shared behavior/data changes are edited here. Existing English evidence remains a historical baseline, not an approval of modified inputs. English must complete its own selective post-fix review and outstanding runtime coverage before acceptance; the other ten languages remain NOT_STARTED.

## Exact correction and recheck scope

- **B03/B04/B05/B12:** `CounterScreen` and `CounterWorkspaceSections` use new `counter_progress_format`; `SessionItem`, photo/gallery/overview callers and reminder editors use the existing neutralized `session_row_range`, `session_row_single`, `row_label_format`, `row_number`, `repeat_every_rows`. Shaping/section fields use `shape_every_n`, `repeat_section_start_row`, `repeat_section_end_row`, `repeat_section_progress_format`; the reminder occurrence plural keeps both argument identities and neutralizes the interval unit. `work_session_pending_rows_format` is also neutral. No stored counters, sessions, photo positions, repeat arithmetic or database fields change. The four selected main-counter labels and their targets remain selected-unit-specific.
- **B10 reader:** only the two main-counter-value labels in `PatternViewerScreen` switch from `current_row_short` to `row_label_format`. The eleven follow/marker state resources in `pattern_reading_assistance.xml`, plus `pattern_clear_row_mark`, `pattern_calibrate_rows`, `pattern_calibration_first_row`, `pattern_calibration_last_row`, `pattern_calibration_invalid_row`, now describe counter positions. Actual PDF chart/grid rows and physical row-gauge terminology remain unchanged.
- **Insights policy (superseded by B03-04 below):** the initial current-project-label predicate was insufficient and has been removed. Every stored session has an unknown historical unit, including sessions of projects currently labeled ROWS. Historical row totals and min/row stay unavailable for all project filters. Time, neutral session/project counter progress, charts, completions and stored values remain unchanged. No historical unit reconstruction or new unit snapshot is introduced.
- **B14/B15:** new `increase_decrease_error_not_enough_bars` replaces the permissive warning in every locale. Flat increases permit at most N−1 distinct bars, circular increases at most N, with at least two starting stitches; both instructions consume exactly N original stitches. Tests expand both patterns for 2,499 valid input/style cases (4,998 sequences), plus rejected densities, exact reproductions and existing decrease/overflow cases. Cast-on repeat selection compares the unrounded body count before adding edges; tests cover below/at/above the midpoint, both unit modes and edge stitches.
- **B17:** JP 0–15 are on exact Clover metric rows; US/UK entries are retained. The six CYC size categories now contain 5/8/9/8/7/9 rows. Numeric ranges keep both endpoints; inches are derived from the published centimeter cells. Baby labels use source ages 3/6/12/18/24 months; unsupported numerical dress-size suffixes disappear from the displayed adult rows. Hand rows distinguish child age bands and women/men S/M/L; the head table includes new `size_row_tween`. The unused men's XS resource is retained unchanged, while its unsupported table row is removed. Existing unused resources are not cleaned up.
- **B06:** all eight bare ply hints are removed from `StandardYarnWeight` and `YarnWeightField`; persisted labels are identical. This resolves unsupported presentation without claiming the original regional mapping was proven false. All twenty existing care descriptions are attached to glyph semantics, and selected care labels are visible in yarn detail. The hand and separate drying dots use the existing Canvas implementation; no image assets/dependencies are introduced.
- **B16:** `BadgePill` retains its existing uppercase default; only the whole-skein weight result opts out. The existing English correction “Weight of whole skeins” is preserved, including lowercase `g`.
- **B19:** 24 retained symbols use the existing Canvas. Twisted knit/purl loops are distinct; M1/lifted and double-decrease variants explicitly follow CYC; the unsupported knitwise-slip entry is omitted. Version-1 cable bands distinguish direction and 4/6 stitch subdivisions (64/96 dp reference icons), rather than the old identical two-line drawings. Existing correct symbols and descriptions stay intact.

Locale owners must re-read only the changed resources and affected rendered contexts above, in their own language. Recheck singular/plural and positional formatting, neutral counter wording, selected ROWS/ROUNDS/REPEATS/CUSTOM boundaries, translated error messages, age/category labels, range wrapping and decimal separators, care labels, lowercase mass units and the modified chart/needle tables. These shared technical checks do not grant linguistic approval to fi/sv/nb/da/de/nl/fr/it/es/pt.

## Validation interpretation and retained failures

Raw evidence root: `reports/language-audit/shared/20261018/` (ignored local evidence). Test commands use `-Pknittools.isolatedTest=activepolicy`, `--no-daemon`, and direct scoped Gradle tasks, never restricted wrappers. Runtime uses the retained disposable API36 AVD, serial emulator-5580, isolated package `com.finnvek.knittools.activepolicytest`, 1080×2400 px / 420 dpi (about 411 dp), font scale 1.0. Synthetic data only.

- `build-unit-1.log`: both APKs compiled; 84 unit tests, 83 passed and one new range-format expectation failed (`12` versus the established one-decimal `12.0`). The expectation was corrected; both range endpoints remain asserted.
- `build-unit-2.log`: both APKs compiled; 277 unit tests, 274 passed. One source-copy expectation still required the old row word, and two pace fixtures supplied sessions with no project rows. Update the exact copy expectation to the authorized neutral count text; supply an explicit ROWS project in those two pace tests. Their exact pace assertions remain unchanged. Separate tests require unknown/mixed units to suppress row metrics and verify filtering without changing time/count values.
- `instrumentation-1.log`: 14 methods, 13 passed. The widget, four symbol/theme cases, counter Cancel/Save case, five Tools cases and both needle themes passed. The project PDF method failed on the initial disabled Next page before PDF loading completed; the retained log identifies that exact line. It did not reach the later picker checks in this attempt.
- `instrumentation-2.log`: 3/3 passed, 69.642 s: project PDF, library PDF, and ROWS/ROUNDS/REPEATS/CUSTOM rendered labels/history/row-metric policy. Across both runs all 16 distinct selected methods have a passing result; the earlier failure remains recorded.
- Automated widget assertions check full text/glyph bounds at 120×48, 120×80, 160×160, 300×160 and actual isolated Room writes through the widget receiver. The original assertions were not modified. No 320 dp app-width test was run.
- Separate visual inspection: all four widget PNGs; `shared-care-{false,true}.png` (20 glyphs in both themes); `shared-charts-{false,true}.png` (24 glyphs in both themes); `runtime-counter/counter-controls.png` (count46 and step3); project PDF `runtime-final/.../reading-guides.png` (neutral Count42). Gallery screenshots exercise the production glyph renderer at ordinary icon size; they are not a claim that every full screen/state or locale was visually reviewed.
- The original manual PDF cancellation and successful file export remain original manual evidence, not new automated export-output validation. The new automated PDF cases verify navigation, markup/layers and cancellation/return, not exported file contents.
- Resource parity and placeholders were independently checked in `resource-verification.log`; all 19 prior English corrections are byte-equivalent at XML value level. Original English review, coverage and input-hash files retain their initial SHA-256 identities. Native review remains NOT_REQUESTED.

## Final input identity and campaign reconciliation

- Original English review/coverage/input-hash files are unchanged (checked against `en-evidence-before.json`); the 19 pre-existing English corrections all remain unchanged. The original coverage is historical and must not be silently counted as fresh PASS for modified inputs.
- Original English inventory: 1,349 keys includes two non-translatable keys (`widget_preview_primary_count`, `counter_step_custom_placeholder`), hence 1,347 translatable keys. Current resources have **1,349 translatable keys per locale**, after adding `counter_progress_format` and `size_row_tween`; the M1 error replaces a prior key. All eleven locales have matching keys/types and string placeholders. The original report's 2,246 coverage rows are not a current denominator after these changes.
- Rebuild B17/B19 code-backed coverage: 38 needle rows (including all 16 numbered JP sizes), 46 body-size rows containing 123 measurements (58 of them ranges), 24 chart glyphs and zero ply hints. Other categories were not re-audited here.
- Final application/test input manifest: `reports/language-audit/shared/20261018/final-input-hashes.tsv`, 65 changed/new inputs, SHA-256 `0619c07cda8c8e973915f177073dd0cfd8bf94863a25707ff546738abe1cd32b`. It includes the pre-existing English string file as part of final application identity, without attributing its earlier 19 changes to this task.
- `LANGUAGE_AUDIT.md`: English B01–B23 = REVIEW_COMPLETE / OPEN, post-fix recheck required; the ten shared entries are SHARED_FIX_VERIFIED / LOCALE_RECHECK_REQUIRED. Other languages remain NOT_STARTED. English's outstanding ordinary runtime gaps remain those in its own coverage; this shared run does not close unrelated V=BLOCKED rows.
- Runtime used the second compiled APKs. Subsequent application-resource change only restored the unused men's XS label to its original value to avoid unnecessary translation edits; final resource/build checks cover that cleanup. Later Kotlin differences are test-fixture corrections/formatting, not changes to tested application behavior.

## Closeout

Final packaging: `build-final.log`, **BUILD SUCCESSFUL** (39 s), `:app:assembleDebug` and `:app:assembleDebugAndroidTest`. This includes the retained original unused XS resources. `git diff --check` passes. Final targeted units: 278 passed / 0 failed / 0 skipped; scoped Kotlin formatting/check passes. The earlier failed runs remain in the evidence list above.

Initial-run conclusion, subsequently reopened by the English review: no unresolved shared implementation blocker was identified for these ten findings at that point. B03-04 and B19-02 below correct the remaining defects discovered by independent review. The former ply mapping and omitted knitwise-slip convention are not newly declared factually false; their unsupported presentation was removed. Required next work is selective locale revalidation, including English's own post-fix review, not another implementation migration.

Temporary owned emulator PID14208 / child40812 was stopped with `adb -s emulator-5580 emu kill`; both PIDs and the device are absent. Gradle invocations use single-use daemons and exit naturally. No commit, push or PR was created; the branch remains `codex/audit-korjaukset-20261018` with no staged changes.


## Follow-up corrections from the English review

These are implementation fixes for the exact “Precise shared handoffs” in `en/review.md`. They do not rewrite the independent English findings or their V results. Starting comparison found zero drift against all 428 English input hashes; the three claimed follow-up defects were still present. Raw follow-up evidence: `reports/language-audit/shared/20261018-followup/`.

| Finding | Verified cause and correction | Current state |
| --- | --- | --- |
| LA-en-B03-04 (remaining B03-02) | `KnitSession` has no unit snapshot. `updateProjectDetails` can change the current label without altering session history. Remove `canShowRowMetrics`, which incorrectly treated the current ROWS label as historical evidence. `InsightsViewModel` now leaves row metrics unavailable for every existing session and filter. | SHARED_FIX_VERIFIED / LOCALE_RECHECK_REQUIRED |
| LA-en-B19-02 (remaining B19-01) | CYC K1fb source shows an upright stem with an upper-left branch as one documented alternative. Replace the plain upward chevron with that alternative. Move the P2tog dot above/left of its diagonal so the purl marker remains separate. | SHARED_FIX_VERIFIED / LOCALE_RECHECK_REQUIRED |
| LA-en-B03-05 | Current `SessionStopSummaryDialog` uses `counter_progress_format`, but `WorkSessionComposeTest` still expected “12 rows”. Assert “Counter progress: 12”, keep the exact duration and Save assertions, and also exercise the existing Discard/Cancel callbacks. | SHARED_FIX_VERIFIED / ENGLISH_RECHECK_REQUIRED |

### Claim verification and limits

- **Historical units:** inspected the original `insights-historical-rounds-shown-as-rows.png` and before/after narrative under `reports/language-audit/en/20261018-followup/`, current model, editable repository writer, aggregation predicate and UI gates. The screenshot displays 10 ROWS / <1 MIN PER ROW for the recorded round session. Current labels cannot recover absent provenance. Suppression is the smallest sufficient correction without a schema migration or new translations. This also suppresses row/pace metrics for projects that currently count rows; such history is equally unproven. Main-counter selected labels remain available and unchanged. Stored `rowsWorked`/totals retain their existing internal names and values; they are not newly asserted to represent rows.
- **Symbols:** freshly read the [CYC chart legend](https://www.craftyarncouncil.com/standards/knit-chart-symbols) and inspected the actual [K1fb image](https://media.craftyarncouncil.com/sites/default/files/images/standards/knit-symbols/knit-symbol-K1fb-on-RS.png) and [P2tog image](https://media.craftyarncouncil.com/sites/default/files/images/standards/knit-symbols/knit-symbol-P2tog-on-RS.png), retained as `cyc-kfb.png` / `cyc-p2tog.png`. P2tog's second documented alternative has a detached purl dot. Compared the original English `symbols-basic.png` / `symbols-increases.png` with the two exact drawing branches. The source explicitly permits pattern-key variations; the correction follows the app's adopted CYC right-side convention rather than declaring other conventions universally wrong. No symbol names/descriptions, inventory or other glyphs changed.
- **Stale assertion:** inspected the original `session-instrumentation.log` failure at WorkSessionComposeTest line50, final resource and actual dialog consumer. The failure is in the expected text. No app behavior or check was weakened; the test now checks all three explicit callbacks.

### Focused validation plan and results

Final Gradle invocation uses `-Pknittools.isolatedTest=activepolicy`, `--no-daemon`, the retained `ktlint.init.gradle` and exact nine-file Kotlin list. Tasks: `:app:runKtlintFormatOverMainSourceSet :app:runKtlintCheckOverMainSourceSet :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`, with filters `*Insights*Test`, `*ChartSymbolDataTest`, `*MinutesPerRowFormatterTest`. `build-tests-2.log`: BUILD SUCCESSFUL, scoped ktlint passes, **188/188 JVM tests in 25 suites pass**, zero failures/errors/skips; retained XML in `unit-final/` and counts in `unit-final-summary.json`. No 320 dp, 200% font scale, combined case or TalkBack checks.

Earlier failures remain evidence: `build-tests-1.log` had 186/188 passes. One source assertion still required `MinutesPerRowFormatter.fromSeconds`; it now verifies the explicit unknown-unit policy and retained UI guard. The new 61-second test wrongly expected 1 display minute; the unchanged `secondsToDisplayMinutes` rounds up to 2. The exact expectation was corrected to 2, retaining elapsed-time/count assertions. Neither failure justified an application-time change. Original XML is retained in `unit-first/`.

Android commands select individual methods using `adb -s emulator-5580 shell am instrument -w -r -e class <methods> com.finnvek.knittools.activepolicytest.test/androidx.test.runner.AndroidJUnitRunner`. `instrumentation-1.log`: 5 methods, 4 pass; the Room test initially failed on that same incorrect 1-minute expectation. After correcting only test expectations and rebuilding/installing the test APK, `instrumentation-2.log` reruns the Room method alone: **1/1 PASS, 3.375 s**. Across the two runs, **all five distinct selected methods have passing results**. This is not a claim that the first run passed or that six distinct tests ran.

| Ordinary Android method | Final evidence |
| --- | --- |
| `WorkSessionComposeTest#stopSummaryRequiresExplicitSaveDiscardOrCancel` | PASS, first run; exact title, 1:01:01 duration, neutral progress and all three callbacks |
| `InsightsCounterUnitsRuntimeTest#historicalSessionKeepsUnknownUnitAfterProjectLabelEdits` | PASS, second run; real Room/repository/production ViewModel and UI components |
| `SharedAuditSymbolsRuntimeTest#chartSymbolsLight` | PASS, first run; ordinary 32 dp renderer, detached dot/stem checks |
| `SharedAuditSymbolsRuntimeTest#chartSymbolsDark` | PASS, first run; same checks in dark theme |
| `SharedAuditSymbolsRuntimeTest#correctedChartSymbolsWithEnglishDescriptions` | PASS, first run; production Chart Symbols screen, both exact label/description pairs |

The Room test records a session through the real repository with a controlled clock, applies the main counter 10→20, stops/saves 61 seconds, then edits the project through ROUNDS→ROWS→REPEATS→CUSTOM. It checks filtered and all-project states, unchanged persisted session fields, retained time/progress and absence of ROWS/min-per-row labels. This is real Room/repository/UI evidence with a test clock, not a claim about newly measured wall-clock time. The JVM transition test uses repository-flow fixtures and is reported separately.

Separate manual image inspection: `shared-corrected-KFB.png` shows the branched upright stem beside “Knit front and back – increase”; `shared-corrected-P2tog.png` shows a detached upper-left dot beside “Purl 2 together” and the right-side disclaimer. `shared-charts-{false,true}.png` confirms their ordinary-size rendering in both themes. These images and the actual CYC originals establish the selected convention; pixel assertions alone do not establish knitting correctness. `shared-historical-units-{false,true}.png` shows the component fixture after selecting ROWS, with “Row 20” for the current counter and “Counter: 10 → 20” / “2min” for history, without historical row/pace metrics. These are component captures from the actual Room-backed state, not new full-navigation screenshots. They do not close unrelated English V gaps.

Follow-up input identity: `final-input-hashes.tsv` contains 12 inputs (nine Kotlin files and the three project instruction files), SHA-256 `c55dda8fe7deb95557d1e6a202812ff3b58635d5e936c8043f2fb7c1a44c5d0b`. AGENTS/CODEX remain byte-identical; their unit-policy sentence and CLAUDE's pace description now match the implementation. `final-apk-hashes.tsv` pins app APK `d1fcf03a191b3130050ee878a1053bfda94745b965b1c29f9822ae2494f5c14e` and final test APK `011d39fc5acab9078fbb13bc3e61f44466ab89f801d048a02afc58d7b28f857e`. First-run symbol/Stop evidence uses the same application implementation and unchanged test methods; the final test source differs only in the Room expectation. No application change followed that first device run.

### English and other-locale handoff

English's current report remains REVIEW_COMPLETE / OPEN with 2,315 rows and V=1069 PASS / 5 FAIL / 1177 BLOCKED / 64 N/A. Those are its pre-correction results, not a fresh verdict for these files. Recheck only B03-04's historical-unit transition and affected B05 contexts, KFB/P2tog with their paired operation text/right-side disclaimer, and B03-05's final test receipt; retain unchanged accepted work. Eight original shared findings were independently verified by English; the two remaining findings are addressed here but require that independent recheck.

All eleven locales share the unit policy and renderer. No resource text was edited in this follow-up. Their locale owners must account for the now-absent row/pace metrics and inspect the two corrected glyphs with their own paired wording. This does not start another language audit.

The interrupted English V classification is preserved in `reports/language-audit/en/20261018-recheck/`: `before/`, all 1,177 caller records and batch excerpts, `classification-draft.tsv` (134 source-traced nonvisual proposals) and `HANDOFF.md`. No proposal was promoted to PASS or N/A in English coverage. Finish classification, exact case inputs and missing rendering evidence in the English chat as explicitly directed by the user.

### Follow-up closeout

All three requested follow-up defects are fixed and their targeted validation is complete; no shared implementation blocker remains for this scope. This does not resolve missing English rendering evidence or independently accept its five pre-correction V failures. English remains REVIEW_COMPLETE / OPEN until its own recheck. The 134 nonvisual proposals remain a draft, not new N/A verdicts. No migrations, dependencies, resource strings or other-locale audit files changed in this follow-up.

Compared with the starting worktree snapshot, only the nine scoped Kotlin files, AGENTS/CODEX/CLAUDE and the two requested shared tracking documents changed. All English report/coverage/input-hash files and all locale resources retain their starting SHA-256 identities; all other pre-existing work is preserved. `git diff --check` passes (existing line-ending warnings are recorded separately). The installed isolated application APK was pulled and its hash matches the final app APK above.

The owned follow-up emulator PID18692 / QEMU child36648 / console child10336 was stopped using the scoped emulator command; these processes and emulator-5580 are absent. The retained disposable AVD is not deleted. Single-use Gradle invocations finished. No commit, push, PR or staged change was made. Final preservation/status and evidence hashes are retained in `final-verification.json`, `git-status-final.txt` and `evidence-hashes.tsv` in the follow-up evidence directory.

## LA-en-B08-01 / EN-V-0621 — PDF explanation measurement

Shared diagnostic follow-up, 2026-10-19. Raw evidence: `reports/language-audit/shared/20261019-b08/`. The current English report has independently verified all ten original shared findings and the three previous follow-ups. Its current totals supersede the earlier snapshots above: V=1,206 PASS / 890 BLOCKED / 219 N/A; 889 pending evidence and one measurement conflict. This section resolves that conflict without rewriting English-owned acceptance records.

### Reproduction and measured cause

Read the current report, exact coverage rows, production `SavedPatternDetailScreen.RavelrySourceSection` / `OverviewEmptyText`, and the complete `MentalModelClarityRuntimeTest.savedPdfStates` path. Both original failing logs and the original production-theme PNG were inspected. The paragraph has neither a maxLines cap nor a fixed height. The existing Gauge test supplied a diagnostic lead only; its conclusion was not assumed to apply.

Reproduced on the retained disposable API36 AVD `KnitToolsLanguageEn20261018`, serial `emulator-5580`, 1080×2400 px / 420 dpi, ordinary 411 dp width, font scale 1.0, en-US, light theme. Isolated package `com.finnvek.knittools.activepolicytest`, synthetic metadata only. Added measurement diagnostics while keeping the original failing assertion unchanged. `diagnostic-instrumentation.log`: **1 test FAIL**, 5.293 s, at the original overflow assertion. `diagnostic-layout.txt` records:

| Measurement | Actual result |
| --- | --- |
| Exact paragraph | The pattern details are saved. No PDF is attached for offline reading. |
| Text node and returned layout size | 984 × 44 px |
| Visible node bounds in root/window | left42, top1097, right1026, bottom1141 px; fully visible |
| Input constraints | width0…996 px; height0…unbounded |
| Reconstructed semantics paragraph | 996 × 44 px |
| Line0 geometry | left0, right983, top0, bottom44 px |
| Character coverage | start0, end70, visibleEnd70; all70 characters, no ellipsis |
| Flags | didOverflowWidth=true; didOverflowHeight=false |

The fresh `diagnostic-no-pdf.png` was separately viewed: the entire single-line paragraph, including its final period, fits inside the screen and measured node. The 12 px discrepancy is empty paragraph-box width, not 12 px of clipped text.

Verified the exact project dependency's official source artifacts, [foundation-android 1.12.1 sources](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-android/1.12.1/foundation-android-1.12.1-sources.jar) and [ui-text-android 1.12.1 sources](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-text-android/1.12.1/ui-text-android-1.12.1-sources.jar), retained with extracts and hashes. `TextStringSimpleNode.applySemantics` calls `ParagraphLayoutCache.slowCreateTextLayoutResultOrNull`: it reconstructs a MultiParagraph using the prior maximum constraints but returns the original measured `layoutSize`. `TextLayoutResult.didOverflowWidth` compares those two widths. Here 984 < 996 sets the flag although the actual line ends at983. The [TextLayoutResult API](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/TextLayoutResult) defines layout-overflow flags; they are not an independent screenshot verdict.

**Classification: confirmed test measurement defect; no application clipping established in this case.** No production code, typography, spacing, dependency or resource text change is needed. The complete English paragraph is preserved byte-for-byte.

### Minimal correction and validation

Only `MentalModelClarityRuntimeTest.kt` changes. `assertReadable` keeps display presence and requires a returned layout, checks every line against the smaller actual node/layout dimensions, checks vertical bounds, rejects ellipsis and maxLines truncation, requires the final character offset to equal the full text length, and checks parent-clipped bounds. The 1 px tolerance covers integer rounding, matching the established geometry approach; it is not a tolerance for missing characters. Diagnostics retain constraints and all line bounds. The faulty aggregate paragraph-width assertion is replaced, not silently dropped.

The new negative-control method uses the same complete paragraph and production theme with deliberately constrained text nodes: horizontal clipping, vertical clipping and ellipsis. Each must throw the specific corresponding readability assertion, so a missing node or unrelated failure cannot make the control pass. The device configuration remains ordinary; this is not a rerun of an excluded screen-width/font-scale audit.

`savedPdfStates` retains the no-PDF paragraph and enabled Ravelry-action checks, then must reach the attached-PDF state, prove the explanation disappears, show the PDF attached chip and enabled Open Pattern action, and invoke its callback. The synthetic URI is intentionally unreadable: this proves detail-screen state/callback behavior, not actual PDF parsing or external Ravelry delivery.

Validation and retained failures:

- `build-diagnostic.log`: both isolated APKs assembled successfully, 1m52s. The unchanged overflow assertion reproduced the reported failure with the added measurements; `diagnostic-instrumentation.log` remains FAIL.
- `build-fixed.log`: both APKs and one-file scoped ktlint format/check PASS, 2m. Direct Gradle tasks with `-Pknittools.isolatedTest=activepolicy --no-daemon`; no restricted wrappers or broad UI matrix.
- `fixed-instrumentation.log`: **6 methods, 5 PASS / 1 FAIL**, 20.335s. `savedPdfStates` passes completely, as do `singleDeletion`, `webDeletion`, `bulkDeletion` and `projectYarnEntries`. The latter four were rerun only because they call the changed measurement helper; this is not new language-audit acceptance for their contexts.
- The negative control initially rejected the horizontal clipping, but with a vertical-bounds diagnostic: the same simple-text semantics path rewrapped its no-wrap text into four lines. The control's stricter expected-axis check therefore failed. Retained metrics in `fixed-layout.txt` prove this is a control-fixture issue, not a production fix failure. `BasicText` source selects the measured-layout path when `onTextLayout` is provided; the horizontal control now uses that path so it exercises the horizontal assertion directly. No readability assertion or threshold was relaxed.
- `build-controls.log`: final test APK rebuild and scoped ktlint PASS, 1m11s. `control-instrumentation.log`: negative-control method **PASS**, 2.234s. It separately requires the expected assertion for all three defects. `control-layout.txt` proves horizontal line983 > node315 px; vertical line44 > node11 px; and ellipsis with visibleEnd20 < full end70. Across the retained runs **all six distinct methods have passing results**; the eight total invocations include the two failures described above.
- Separate manual inspection of `fixed-images/api-36-ravelry-no-pdf.png` and `api-36-ravelry-attached-pdf.png`: the complete no-PDF paragraph is visible; after state change it is absent and PDF attached / Open Pattern are visible. The enabled Open Pattern callback is additionally asserted automatically. Manual viewing is not substituted for the recorded instrumentation results. These are production screen/theme component captures with synthetic state, not full app navigation or file-reading evidence.

Final application APK SHA-256 remains `7b2325b50b717389974c51da513cc4a7d8f36a9d77c6db1eee17669679cb4e07`, identical to the latest English audit. The five passing production-state methods used test APK `7cfb2df8b7cc5547e3f594f77a1e7eccdaf11cf516c050fb687c193b1fb2452a`; only the negative-control callback/comment changed afterward. Final test APK is `dcb2b8a1d572f389e361ac0fd06ed2cdf6f18d15517b6d839534329021794f80`. `final-input-hashes.tsv` pins the test and three unchanged production inputs (manifest SHA-256 `1d6383fe3e9413625b46d1e0397dd7b3bd17dabf817fa447579a93d819ce4de8`). No new JVM suite is necessary for this instrumentation-only correction.

### Exact English handoff

EN-V-0621 receives measured production-component evidence for the no-PDF explanation; EN-V-0617 receives the attached-PDF chip state, and EN-V-0622 receives the Open Pattern state/action. Final images and test receipts must be reconciled by the English owner; they do not approve other warnings, offline/download outcomes, languages, or the remaining 889 unrelated evidence cases. English review/coverage/input hashes remain unchanged in this shared correction chat. English remains REVIEW_COMPLETE / OPEN, not AUDIT_PASS.

**Shared outcome: TEST_FIX_VERIFIED; no remaining implementation/measurement blocker for LA-en-B08-01.** The paragraph does not need a wording or production layout correction. English can reconcile these three exact contexts against the new evidence; other languages are not audited or assigned a failure. The shared helper change creates no translation recheck requirement.

Preservation comparison allows exactly `MentalModelClarityRuntimeTest.kt`, this report and `LANGUAGE_AUDIT.md`; all other starting files, including locale resources and English audit records, remain byte-identical. `git diff --check` passes. Evidence/source hashes and final preservation checks are retained locally. The owned emulator PID39312 / QEMU39272 / console42420 was stopped and is absent; disposable AVD files remain. No 320 dp, 200% font scale, combination or TalkBack checks; no dependencies, migrations, commit, push, PR or staged changes.
