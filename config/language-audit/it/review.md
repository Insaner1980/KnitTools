# Italian Language Audit (it)

## Scope and snapshot

- Assigned boundary: all Italian content, B01–B23, necessary Italian-only corrections and acceptance under revised LANGUAGE_AUDIT.md section 16. Stop after Italian.
- Variety: general Italian used by Italian knitting/crochet readers. Native-speaker review: **NOT_REQUESTED**.
- Date: 2026-10-19. Branch: `codex/audit-korjaukset-20261018`; HEAD: `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`.
- The workspace already contained extensive shared, English and other-locale work, including **55 Italian keys already changed relative to HEAD** (11 reader keys, 26 reference keys, 18 main-resource keys). `preexisting-italian-changes.json` lists them; these are not attributed to this audit's change count. `starting-state.json` pins the starting file bytes and preserves the original four Italian XML files. No existing Italian audit record was present.
- Current inventory: **1,349 keys = 1,318 strings + 31 plurals**. Every plural declares `one`, `many`, `other`: **1,411 resource variants**, no arrays. There is one resolved string alias. Counts come from current XML, not another language's denominator.
- `coverage.tsv`: **1,883 rows**: 1,411 resource variants, 275 reference cells, 88 code-text items, 44 symbol pairs, and 65 composition/quantity cases. Repeated identical callers share a resource row; materially different argument meanings have additional composition rows.
- Reference inventory: 38 needle rows × four cells = 152; 46 body-size rows containing 123 measurement cells; 77 abbreviation tokens; eight yarn categories; two intentionally nontranslatable numeric samples and one grouped arithmetic-symbol record; 24 chart symbols and 20 care symbols.
- **101 text variants changed across 93 keys in three XML files**. `pattern_annotations.xml` was read and remains unchanged. No shared implementation, tests, other language, root tracker, build configuration or dependencies were edited.
- Final inputs are pinned in `input-hashes.json` (145 files). `inventory-summary.json` and `build-records.py` reproduce the current inventory, exact-diff reconciliation, format checks and shared receipt comparison. They do not automate linguistic approval.
- The English inventory supplied only a batch-membership discovery map. All Italian strings were independently read; non-resource entries were freshly extracted from current Kotlin data. No English/French acceptance or rendering status was copied.
- Exclusions: user-entered project/note/designer text, external Ravelry web content, Play-provided localized prices, OS document/photo-picker content, internal logs/exceptions and third-party notices are not app-owned Italian translations. Insertions and action meaning around them were reviewed. Retained resources without a current direct caller are explicitly marked in coverage rather than silently omitted.

## Language decisions and glossary

| Concept | Expression/context | Alternatives and limits | Evidence/status |
| --- | --- | --- | --- |
| User address | Informal singular `tu`; short action imperatives (`Scegli`, `Salva`, `Aggiungi`); infinitive questions in confirmations | Reference definitions can use instructional infinitives without changing the app's address convention. No mass voice rewrite. | Independent full Italian reading; PASS |
| Needle / row / round | `ferro` is a knitting tool or knitting row; generic selectable ROWS and gauge axis use `riga/righe`; ROUNDS uses `giro/giri` | Crochet can explicitly choose ROWS, so those shared labels cannot require needles. Knitting-only glossary instructions retain `ferro`. Unused historical Insights row resources retain their knitting wording; current UI hides historical unit-dependent metrics. | MainCounterDisplayText, CounterValueFormatter, MainCounterLabelType, GaugePresentation; IT1/IT2; PASS |
| Stitch / stitch pattern | `maglia` for an individual loop/operation; `punto` is also valid for a stitch or fabric pattern | Do not globally replace `punto`. `Punto seme`, `maglia rasata`, `legaccio`, `coste` remain; seed-stitch instructions now explain alternating the visible knit/purl stitches. | IT1–IT4; PASS |
| Slip and decreases | `passare una maglia`; SSK slips individually knitwise then knits together through back loops; S2KP slips both together | `far scivolare` in a complete explanation is acceptable and retained. Bare literal `Scivolare`/`lavorare a maglia` did not define the specific operation. SSP now states the action instead of an ambiguous side-dependent lean. | IT1, IT3, IT5; PASS |
| Increases / yarn position | `aumentare una maglia`; M1 uses the intervening strand; front/back means relative to the working side | Front is not necessarily the fabric's wrong side; back is not necessarily its right side. Lifted increases and M1 are not conflated. | IT1, IT3, IT6; PASS |
| Gauge | `campione`, `tensione`; individual stitch and row axes | `calibro` as a standalone knitting-gauge translation was misleading. The adjustment warning correctly excludes garment construction changes. | IT1, IT7; calculator inspection; PASS |
| Patterns | `modello`, `schema`, `motivo`, contextual PDF/website wording | These valid Italian alternatives are not systematically standardized. A webpage entry is not an offline PDF. | SavedPatternRepository, project-document/web-pattern callers; PASS |
| Yarn packages | `gomitolo/gomitoli` in stash/estimator; `matassa/matasse` in usage conversion | Both are established package terms. They share a user-confirmed package length; neither implies an automatic physical package conversion or stash deduction. No preference-only terminology rewrite. | IT1/IT5; SkeinCountText, YarnUsagePresentation; PASS |
| Care | Wash labels express maximum °C; professional dry cleaning identifies A/P/F treatments | `Solvente P/F` incorrectly sounded like named solvents; labels now identify dry cleaning. Labels are also icon content descriptions, so bare `30°` lacked the operation and maximum meaning. | CareSymbolIcon, CARE; PASS |
| Numbers and quantities | Italian decimal comma; invariant symbols `cm`, `m`, `g`, `in`, `yd`, `h`, `min`; one singular, other/many plural | US needle identifiers such as `1.5` are regional codes, not localized decimal measurements. Numeric headings (`Progetti: 1`) need no singular label. | Number-format callers; ANDROID/CLDR; PASS |

## Sources

Access date for external sources below: **2026-10-19**. Sources substantiate specific technical concepts, not blanket approval of Italian naturalness. The final wording was independently reread in its UI/reference context.

| ID | Publisher/title and URL | Passage used and limits |
| --- | --- | --- |
| IT1 | [DROPS Design Italian–English glossary](https://www.garnstudio.com/glossary.php?lang=it&langf=it&langt=en) | Italian terms for needles, stitches, gauge, slipping, back loops, packages and alternating operations. Multiple valid synonyms are retained; glossary entries alone do not establish a whole procedure. |
| IT2 | [DROPS Italian pattern guidance](https://www.garnstudio.com/pattern.php?cid=4&id=2026) | Italian flat/round knitting/crochet wording; cross-checked against the actual craft-independent counter selection. Does not require changing knitting-only glossary rows. |
| IT3 | [We Are Knitters: Come leggere uno schema a ferri](https://blog.weareknitters.it/trucchi-per-knitter/come-leggere-uno-schema-a-ferri/) | Make-one strand, knit/purl chart interpretation and accavallata simple/double descriptions. This publisher's chart-reading convention is not imposed as universal. |
| IT4 | [DROPS: Maglia a grana di riso](https://www.garnstudio.com/video.php?id=20&lang=it) | Written instructions alternate knit/purl and work the opposite of the stitch as it presents on subsequent rows. Supports corrected procedure, not mandatory renaming of `Punto seme`. |
| IT5 | [Sarah Wilson: Crosswaves, Italian translation](https://www.filiandcolori.com/download/Crosswaves.pdf) | Page 2 legend describes SSK/SSP and KFB, distinguishing individual slips and twisted purl decrease. Read the actual PDF text. Unrelated pattern values/typographical errors are not treated as authoritative. |
| IT6 | [DROPS: Aumentare usando il cappio sotto](https://www.garnstudio.com/video.php?id=11&lang=it) and [accavallata semplice](https://www.garnstudio.com/video.php?id=13&lang=it) | Lifted increase versus slip-knit-pass decrease context. These are distinct from an interstitial M1. |
| IT7 | [Katia: Consigli per seguire gli schemi](https://www.katia.com/blog/it/consigli-per-seguire-facilmente-gli-schemi-della-rivista-katia/) | Italian gauge/campione and needle terminology; swatch measured with intended needles. Arithmetic and app behavior were checked separately. |
| CARE | [GINETEX care symbols](https://www.ginetex.net/gb/labelling/care-symbols.asp) | Maximum washing temperature; professional-care circle and A/P/F categories. The shared evidence supplies the unchanged glyph geometry; no new laundry-standard certification is claimed. |
| ANDROID | [Android string resources: plurals](https://developer.android.com/guide/topics/resources/string-resource#Plurals); [AOSP ResourcesImpl](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/master/core/java/android/content/res/ResourcesImpl.java) | Actual `getQuantityText` calls ICU `select(quantity)`, chooses category and falls back to `other`; formatting arguments are separate from selection. This is framework source review, not a device execution. |
| CLDR | [CLDR 48 cardinal rules, Italian](https://unicode.org/cldr/charts/48/supplemental/language_plural_rules.html#it) | Integer 1 singular; 0, 2, 21 and ordinary fractions plural; modern rules additionally classify exact nonzero millions as many. Device ICU versions may differ; all current Italian many/other forms are identical, so this difference does not change their wording. |
| SHARED | [Shared correction evidence](../shared-fixes.md), raw 20261018, 20261018-followup, 20261019-b08 and 20261019-b05 input receipts | CYC/Clover reference values, care/chart geometry, calculator corrections, neutral historical counter units and bounded activity dates. See the per-file reconciliation; historical tests are reused, not claimed as fresh executions. |

The attempted large chamber-of-commerce textile PDF and unavailable tutorial PDFs were not used as proof. Italian craft terms were checked through readable sources above. A historical memory pointer helped locate SkeinCountText; current source, not the old French plural advice, governs this audit.

## Batch tracker

Exact keys, variants, caller locations and text are the corresponding `batch` rows in `coverage.tsv`. `READ-Bnn` means the independent Italian reading and the concrete notes below; `CALLERS` points to `caller-inventory.json`; `STRUCTURE` to the mechanical validation; `PLURALS` to the Android/ICU reconciliation below. V is blank throughout: no Italian rendered/spoken execution is claimed.

| Batch | Owned/reviewed rows | Progress / verdict | Open findings | Evidence and context |
| --- | ---: | --- | --- | --- |
| B01 | 27/27 | Complete / PASS | None | Common actions/navigation/units; selection grammar corrected for one/many and feminine entities. |
| B02 | 116/116 | Complete / PASS | None | Project/folder creation, completion/reopening, bulk moves and deletion; missing assignment is Unfiled, folder deletion keeps projects. Names are opaque arguments. |
| B03 | 88/88 | Complete / PASS | None | MainCounterDisplayText/CounterValueFormatter; each selected unit, target/remaining/overrun, step and widget. Generic rows changed only where crochet is reachable. |
| B04 | 45/45 | Complete / PASS | None | Extra/repeating/linked/shaping/section counters and reminders. Count label `Ripeti` corrected to a noun; interval argument is distinct from occurrence. |
| B05 | 144/144 | Complete / PASS | None | Start/Stop/recovery, clock versus duration units, completion events, histories, range filters and chart descriptions. B05-01 shared date repair rechecked; neutral count policy retained. |
| B06 | 92/92 | Complete / PASS | None | Yarn metadata, status, package quantity, eight canonical thickness names; all 20 care labels paired with unchanged glyphs. |
| B07 | 71/71 | Complete / PASS | None | Independent usage amounts, unknown/zero/overage, fractional package conversion and source unlink. Scoped to project usage without changing stash quantity. |
| B08 | 78/78 | Complete / PASS | None | Library/web/Ravelry metadata and browser actions, availability and duplicates. Deleting library annotation master differs from preserving project PDF/overlay. |
| B09 | 80/80 | Complete / PASS | None | Incoming PDF, primary/multiple documents and photo-to-PDF flow; limits and cancellation language match actual storage/import rules. |
| B10 | 68/68 | Complete / PASS | None | Bookmarks, reading lines, follow/pause and calibration. `Interrompi il seguito` corrected to the concrete counter-follow action. |
| B11 | 78/78 | Complete / PASS | None | Every annotation resource; tools/layers/selection/export, original-PDF preservation and localized filename suffix. No edits needed. |
| B12 | 33/33 | Complete / PASS | None | Notes, local photo capture/import, gallery, share/delete and save feedback. No false cloud or synchronization promise. |
| B13 | 84/84 | Complete / PASS | None | Density and measure axes, 10cm/4in, unit switches, actual/pattern gauge, exact/rounded quantities and copy output; generic row terminology corrected. |
| B14 | 35/35 | Complete / PASS | None | Flat/circular increase/decrease, distinct M1 bars, generated K/M1/K2tog consumption and invalid cases; existing English parser examples retained as supported syntax. |
| B15 | 23/23 | Complete / PASS | None | Cast-on body width, total edges, repeat rounding, displayed alternatives and metric/imperial arithmetic. |
| B16 | 13/13 | Complete / PASS | None | Fractional estimated packages versus rounded purchase mass; singular-safe heading, lowercase g and contingent extra-yarn advice. |
| B17 | 349/349 | Complete / PASS | None | Every 152 needle/123 size-table cell, headings/categories, ranges and cm-derived inches. Baby/child distinction and L/XL label defects corrected. |
| B18 | 234/234 | Complete / PASS | None | All 77 token/meaning/description triples; tools, stitch operation, decreases, orientation, positions, fabrics and informal abbreviations. |
| B19 | 82/82 | Complete / PASS | None | 24 glyph pairs plus retained non-displayed resource definitions; RS convention, increases/decreases, cable span and directions checked. |
| B20 | 35/35 | Complete / PASS | None | Settings/native language names, privacy/analytics opt-in, app updates and default units. Measurement unit control exception stated accurately. |
| B21 | 30/30 | Complete / PASS | None | Backup export/preview/replace, unencrypted privacy warning, exclusions and error preservation promises. Repository source inspected; no real restore. |
| B22 | 72/72 | Complete / PASS | None | Fourteen-day trial, free active-project limit, existing-content access, paid advanced views, purchase restoration and unit-neutral reminders. No purchase performed. |
| B23 | 6/6 | Complete / PASS | None | Remaining generated durations/numbers/clocks/date/name cleanup; full resource/default parity, residual searches and all shared acknowledgements closed. |

## Findings and minimal corrections

`corrections.tsv` is the detailed finding register: one row per changed variant, grouped by stable `LA-it-Bnn-nn` key ID. It records classification, category, severity, original text, specific problem/user consequence, final text, minimal-change reason and VERIFIED status. `corrections.json` retains the original edit record. Final XML differences are mechanically required to match this register exactly; no undocumented text change is accepted.

The 93 findings cover these concrete problems, not a preferred universal wording style:

- Incorrect needles/technique vocabulary (`ago` in knitting-tool instructions; literal slip/make-one/pick-up/marker terms); missing procedure details for SSK/S2KP and incorrect front/back fabric-side equations; unclear gauge, alternating-row, WPI, short-row and seed-stitch instructions.
- Knitting-only row wording in craft-independent counter/measurement contexts; row-specific widget, reminder and empty-state claims where other units/crochet are possible.
- Selection and count grammar; a repeat count rendered as an imperative; active-day numerator/denominator wording; estimated-package wording that could produce `1 gomitoli`.
- Care labels without their operation/maximum or treating P/F as solvent names; yarn usage described as only reserved yarn; overage not distinguished from surplus; whole purchased-package mass confused with consumed yarn; an unconditional extra-package recommendation.
- An unintelligible stop-follow label, false all-calculators unit promise, incomplete/universal needle-numbering guidance, Baby/Child collision and malformed L/XL size labels.

Four technique findings (individual SSK slips, simultaneous S2KP slips and both viewer-relative yarn positions) are S1 because an incorrect interpretation can alter the worked fabric. There are 83 S2 semantic/technical/context findings and six S3 grammar/typography findings. No S1–S3 remains open.

### Retained wording / rejected preferences

- `Punto seme` was retained as an understandable attested Italian alternative; only its mixed-language/ambiguous procedure changed. No mandatory rename to grana di riso.
- `modello/schema/motivo`, `gomitolo/matassa`, `far scivolare`, `filo dietro/asola`, legitimate infinitive technique definitions and informal app imperatives remain where they convey the right concept.
- `work_session_active_description` was initially suspected of disagreeing with singular duration units. The real caller always formats **h:mm:ss**, so the singular/mixed-unit examples do not reach it. The proposed edit was withdrawn and original clock shorthand retained. **NOT_A_DEFECT / NOT_APPLIED** for that hypothesis.
- No changes to unused historical Insights row strings were justified by the suppressed metrics; their resource content was still read. No automatic French zero/singular rule was applied to Italian.
- `web_pattern_error_designer_too_long` legitimately aliases the title-length message because both limits are 120; alias target and current validation were checked.

## Quantity and composition reconciliation

The 31 quantity resources each have three individually read coverage rows and a separate quantity-selection case. AOSP uses the integer **selection quantity**, not the first formatting argument. Italian zero/21 take plural; one takes singular. Exact millions may select many on current ICU or other on older versions; all declared many/other texts agree. Numeric count output is not written-out `un milione` prose.

Special callers were explicitly traced: reminder quantity is the interval while its first argument is occurrence; Insights day metadata selects by days in range while inserting active days first; project-fabric summary selects/inserts active days separately from its range-label argument. Fractional yarn usage formats the amount, parses that visible value with the current locale and maps ICU one/many/other to Android integer selectors. Thus `0,5 matasse`, `1 matassa`, `1,5 matasse` are correct; an amount displayed after rounding as `1` selects singular. This conclusion uses actual production/framework source and CLDR rules, not an Italian emulator test.

Generated cases also account for custom labels and percent signs inside user names, counter targets/steps, clock and duration boundaries, active days, completion events, usage remainder/overage, measurement/copy output, calculator sequences, reference conversions, file names and dates. No textual placeholder was added, removed, renumbered or changed in type.

## Shared handoffs and other-language impact

**No new shared repair handoff is required.** `shared-evidence-reconciliation.tsv` compares the latest applicable raw receipts against current source: **24/24 production files MATCH**. It is not an approval import from another locale. Italian conveys each applicable repaired meaning as follows:

| Shared finding | Italian acknowledgement |
| --- | --- |
| B03-02 / B03-04 | Current session/photo/reminder/reader wording is unit-neutral; historical row/pace metrics remain suppressed. Explicit ROWS selection was independently corrected to support crochet. |
| B03-03 | Unchanged widget layout/producer evidence reused only for its shared numeric-count behavior; Italian widget title is generic. No new Italian clipping/screenshot claim. |
| B06-03 | Eight canonical yarn-thickness names have no unsupported ply labels. |
| B06-04 | Same 20 corrected care glyphs; Italian labels independently checked against operation and icon semantics. |
| B14-01 | Current M1 distribution reserves flat final stitches and rejects insufficient bars; Italian error and notation convey this. |
| B15-02 | Current repeat rounding precedes display rounding; Italian result/alternative labels match verified arithmetic. |
| B16-03 | Whole-package mass retains lowercase g because the badge disables uppercase; Italian label now names the actual mass. |
| B17-02 / B17-03 | JP values are exact Clover diameters; regional numbering caveat retained; every size value/category/range re-read in its Italian heading context. |
| B19-01 / B19-02 | Same corrected CYC-based glyph mapping/geometry; all Italian pairings re-read, including RS convention and KFB/P2tog distinctions. |
| B03-05 | Historical English test assertion correction has no Italian translation impact. |
| B08-01 | Shared measurement-helper repair and unchanged production layout acknowledged; neither an Italian rendering pass nor a remaining language defect. |
| B05-01 | Current accumulator/view model match the repaired raw receipt; bounded active dates and nonreversed All Time range support Italian numerator/denominator/date compositions. |

The existing English supplementary rendering campaign, including its 231 unverified cases, is unchanged. No other locale is approved by this work and the root campaign tracker remains coordination-owned.

## Validation log

All runs use the repository wrapper (Gradle 9.8.1) and its Temurin JDK 17 daemon criteria. The shell's separate default Java 21 is not the configured build daemon. No toolchain change was made. Commands use `--no-daemon` so task-owned single-use daemons stop after the build.

- `python config/language-audit/it/build-records.py`: current XML/type/key/variant/alias checks; every format signature compared with default; duplicate/missing variants rejected; exact original/final diff equals correction register; all reference entries extracted; shared hashes reconciled; unrelated starting files compared. **PASS**. Linguistic approval remains the human-readable review, not this script.
- Initial `gradlew.bat --no-daemon :app:processDebugResources :app:testDebugUnitTest --tests com.finnvek.knittools.ReferenceResourceLocalizationTest --tests com.finnvek.knittools.ui.CountPluralResourcesSourceTest`: **BUILD SUCCESSFUL**, resource processing executed, tests **UP-TO-DATE**. Not counted as fresh test execution.
- Targeted `:app:testDebugUnitTest --rerun` then executed both selected suites: **3 tests, 0 failures/errors**. A subsequent combined resource/test run also passed. These intermediate runs preceded the last minimality correction and are retained as history.
- Final command: `gradlew.bat --no-daemon :app:processDebugResources :app:testDebugUnitTest --rerun --tests com.finnvek.knittools.ReferenceResourceLocalizationTest --tests com.finnvek.knittools.ui.CountPluralResourcesSourceTest`. **BUILD SUCCESSFUL, exit 0; resource processing and test task both executed; 3/3 tests passed, no failures/errors.** Log interval: 11:07:28–11:08:12 UTC. `validation-results.json` and `validation-closeout.log` retain the precise timestamps/task statuses. All 145 final input hashes were rechecked afterward.
- No Android runtime, screenshot campaign, accessibility rerun, physical-device check, destructive restore, real purchase or native-speaker review. No specific remaining language/comprehension question required device execution after caller inspection. Care captions can wrap without a max-line cutoff in their current source; this is source evidence, not a rendered-layout certification.
- Historical shared arithmetic/glyph/date tests are reused only for unchanged producers, with explicit raw hash receipts. They were not rerun here.
- Scoped `git diff --check` passed. Starting-state comparison found **zero unrelated file changes**. Final Gradle log identifies Temurin Java 17 and a stopped single-use daemon (PID 8356, no longer running); all launched command sessions exited. No task-owned temporary process remains.

`validation-results.json` records exact UTC start/end, exit codes, final suite totals, task outcomes and input identity. TSV cells escape tabs/newlines as `\t`/`\n`; literal Android escapes remain visible. Resource file hashes pin their exact XML text.

## Native-speaker review

- Status: **NOT_REQUESTED**.
- No native speaker or craft expert is represented as having signed off.
- Independent source-assisted language/technical review is the evidence used here.

## Closeout and resume point

**AUDIT_PASS — native review NOT_REQUESTED.** All B01–B23 content is covered against current inputs; final changes have explicit reasons and no confirmed defect or specific unresolved language question remains. This is Italian language acceptance under revised section 16, not exhaustive rendered/spoken UI certification.

No shared handoff, unreviewed Italian batch or rendering backlog is created. Existing unrelated work is preserved. The task stops after Italian, with no commit, push, pull request, new chat or cross-chat message. Reopen only an explicitly assigned future task, and invalidate only evidence whose pinned inputs have actually changed.
