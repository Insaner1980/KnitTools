# Danish Language Audit (da)

## Scope and snapshot

- **Decision: AUDIT_PASS**, under current `LANGUAGE_AUDIT.md` section 16. Native-speaker review: **NOT_REQUESTED**.
- Variety: contemporary written Danish for Denmark, retaining understandable established craft synonyms. This is an independent Danish content review, not native craft-expert sign-off.
- Assigned task: all Danish content in B01–B23, minimal Danish-only corrections, then stop. Completed 2026-10-19.
- Branch: `codex/audit-korjaukset-20261018`; HEAD: `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. The audited working tree includes pre-existing uncommitted shared repairs; HEAD alone is not its identity.
- Read AGENTS.md, CODEX.md, relevant CLAUDE.md wording/UX instructions, LANGUAGE_AUDIT.md and shared-fixes.md. There were no previous Danish audit records. No other language's acceptance was adopted.
- Existing work was captured in `starting-status.txt`, `starting-input-hashes.json`, `starting-danish-diff.patch` and `starting-texts.json`. Three Danish files already had changes. This task changed only `values-da/strings.xml`, `values-da/reference_strings.xml` and the Danish audit directory. Existing reading-assistance changes were preserved byte for byte.
- Final source/resource identity: `input-hashes.tsv`. This includes dependencies and reference lookup inputs; a hash entry is not a claim that every line of that file was reviewed. Actual caller references and reading decisions are in `coverage.tsv`.
- All four Danish XML files were read completely, including every plural variant and unused retained text. Counts were obtained from current XML, not copied from an earlier language.

| Resource file | Strings | Plural keys | Text variants |
| --- | ---: | ---: | ---: |
| pattern_annotations.xml | 75 | 0 | 75 |
| pattern_reading_assistance.xml | 49 | 0 | 49 |
| reference_strings.xml | 257 | 0 | 257 |
| strings.xml | 937 | 31 | 999 |
| **Total** | **1,318** | **31** | **1,380** |

There are **1,349 translatable keys**, no arrays, and one string alias. The two default-only `translatable=false` numeric examples are accounted for separately. They do not increase the Danish translatable denominator to 1,382.

The ledger has **1,649 rows**: 1,380 primary resource variants, 22 additional materially different resource contexts, and 247 code/data/generated-output records. Repeated identical uses share caller references. Code-backed coverage includes all 38 needle rows (four cells each), 46 size rows (123 measurement cells, including 58 ranges), 77 abbreviation triples, 24 chart-symbol triples, 20 care-symbol mappings and eight yarn-category literals. Calculators, durations, dates, counter units, aliases, numeric controls and filename presentation have additional records.

TSV is UTF-8; embedded newlines/tabs are represented as literal `\n`/`\t`. Resource text containing Android `\n` escapes is retained literally. Row hashes identify the complete source file. T/F/L/C/R reflect content, meaning and resource review; **V is blank throughout because no new rendering check was performed**. C=N/A is used only for retained resources with no direct production caller, with that reason recorded. R=N/A on code/data means that XML validation does not apply. L=N/A on needle identifiers/numeric examples means there is no Danish inflection to review.

The English ledger was used only to discover existing batch ownership and non-resource surfaces. All Danish text, current counts, contextual decisions and corrections were independently reviewed. The current production source, rather than key spelling alone, determines interpretation.

## Language decisions and glossary

| Concept | Accepted Danish | Context and decision | Evidence |
| --- | --- | --- | --- |
| Needle / worked row / circular round | pind; række/pind; omgang/runde | Keep pind for physical knitting needles and explicitly knitting instructions. Shared ROWS counters also support crochet and now use række/rækker. Preserve valid omgang/runde variants; no blanket replacement. | S1, S2, S12; MainCounterLabelType and MainCounterDisplayText |
| Knit / purl | ret / vrang | Generic strik does not specify a knit stitch. Forming a stitch and inserting knitwise/purlwise are distinguished. | S1, S2, S4 |
| Stitch / pattern / repeat | maske; mønster/opskrift; gentagelse | Maske is one stitch; a stitch pattern is not a single stitch. Mønster and opskrift remain where understandable; no Norwegian/Swedish-driven standardization. Counter repetition and position within a repetition remain different. | S1; CounterValueFormatter |
| Gauge / swatch | strikkefasthed / strikkeprøve | Preserve knitting-tool terminology and separate stitch density from row density. Width/height, manual/swatch values, exact/rounded results and 10 cm/4 in bases are independently checked. | S2, S8; GaugePresentation |
| Increase / decrease | udtagning / indtagning | These describe fabric operations; generic numeric controls use Øg/Mindsk. Lifted-strand and front/back-leg increases are not interchangeable. | S1, S2, S4; production control callers |
| Slip / drop | tag løst af / dropping a stitch is a different action | Slip retains the stitch. Required knitwise/purlwise orientation and simultaneous versus separate transfer are explicit where they determine the operation. | S1, S2, S4, S9 |
| Stitch legs / yarn position | forreste/bagerste maskeled; foran/bag arbejdet | Yarn in front/behind is relative to work position, not permanently the fabric's wrong/right side. | S1, S2 |
| Cables / short rows | snoning; venderækker, fold om og vend | Cable holds are in front of or behind the work; four/six total stitches and two/three held stitches remain distinct. | S1, S3, S10 |
| Yarn package / weight | nøgle/nøgler; fed; g | Keep package terms in their existing contexts; fed has the same singular/plural form. Thickness category is not package mass or strand count. Lower-case g is preserved. | S6, S8; YarnEstimator/YarnUsageCalculator |
| International reference identifiers | K/P/M1/K2tog, US/UK/JP, Lace…Super Bulky | Intentionally retain source identifiers. The abbreviation tool now identifies English notation; calculator help explains the generated tokens. Number-system identifiers such as 000 are not decimal diameters. | S4; SHARED; current data files |
| Body measurements / ages | kropsmål; mdr.; præteenager | Tables describe bodies, not finished garments. Month labels cannot be confused with meters; preteen is not teenage. | SHARED; SizeChartData and Danish headings |
| Care | professionel rensning (P/F) | The letters identify professional cleaning processes/groups, not substances named P/F. Existing temperature, prohibition and dot-count distinctions are preserved. | S11; SHARED |

Correct, understandable existing wording was kept even where an alternative is possible: række/pind in knitting, runde/omgang, løkke where it actually means a loop, mønster/opskrift, and package vocabulary were not globally normalized. English reference identifiers and ordinary loanwords were not classified as accidental Swedish/Norwegian. EOR is explicitly defined by this app as every other row; references also use EOR for end of row, so this entry is not presented as a universal abbreviation standard.

## Sources

All external sources below were accessed in this task on **2026-10-19**. Technical meanings were cross-checked rather than treating a translated dictionary or another locale as sole authority. Source statements do not establish naturalness of unrelated UI text; L decisions also include direct Danish proofreading.

| ID | Publisher and source | Language/region | Claim used and limitation |
| --- | --- | --- | --- |
| S1 | [PaaPinden, Irene Birk: knitting dictionary](https://www.paapinden.dk/en/strikkeordbog?order=field_en_abbr&sort=asc) | Danish/English | Danish craft terms and paired operation meanings. Convention-dependent tokens are not universal; independent technique sources also checked. |
| S2 | [Filcolana: Evelina, Danish original pattern](https://en.filcolana.dk/amfile/file/download/file/1839/product/1078/) | Danish | Technique/abbreviation passage: gauge, worked rows, right/left decreases and lifted strand. Product pattern, not a universal gauge recommendation. |
| S3 | [DROPS Design: rib cable tutorial](https://www.garnstudio.com/video.php?id=1209&lang=dk) | Danish | Written instructions distinguish held stitches, front position and subsequent knit/purl work. Written content read; video playback not claimed. |
| S4 | [Craft Yarn Council: knitting abbreviations](https://www.craftyarncouncil.com/standards/knitting-abbreviations) | US English | Source-token meanings and operations, particularly SSP, SSK, K/P and decrease combinations. Not evidence of Danish naturalness. |
| S5 | [Dansk Sprognævn / sproget.dk: one or several words](https://sproget.dk/typiske-problemer/et-eller-flere-ord/) | Danish | Compound-word rule used for the two wrongly separated compounds. Not permission for general stylistic rewriting. |
| S6 | [Den Danske Ordbog: nøgle](https://ordnet.dk/ddo/ordbog?def_id=21057484&entry_id=11036681&query=%3Fi) | Danish | Yarn-ball sense and neuter/plural inflection. Does not require replacing valid fed throughout the app. |
| S8 | [DROPS Design: Belle yarn](https://www.garnstudio.com/yarn.php?cid=16&page=2&show=drops-belle) | Danish | Actual package length/mass, gauge and yarn-label usage. Product care/composition is not generalized to all yarns. |
| S9 | [PaaPinden: centered double decrease](https://www.paapinden.dk/strik/centreret-dobbeltindtagning) | Danish | Paired knitwise transfer, knit one and pass-over sequence. |
| S10 | [PaaPinden: wrap-and-turn short rows](https://www.paapinden.dk/strik/venderaekker-med-fold-om-og-vend-metoden) | Danish | Fold om/venderække terminology and wrap-around-stitch meaning. |
| S11 | [GINETEX newsletter 23, p. 5](https://www.ginetex.net/userfiles/files/gin_int_newsletter_23_rz_01.pdf) and [GINETEX/COFREET care guide](https://www.ginetex.net/userfiles/files/COFREET_plaquette_240x170_04_17_UK_web.pdf) | International, English | Professional cleaning P/F process distinction; follow the textile care label. No current legal advice or home solvent procedure is inferred from the historical newsletter. |
| S12 | [Hobbii / Elisa's Crochet: Space Shuttle](https://hobbii.dk/collections/free-patterns/products/hp-1008006-space-shuttle-stacking-toy) | Danish | Explicit crochet hook, back-and-forth rows and circular rounds distinction. Supports fixing shared crochet/knitting count labels, not replacing knitting-only pind. |
| SHARED | [shared-fixes.md](../shared-fixes.md) and [English B05-01 repair receipt](../en/review.md) | Shared code/facts | Existing calculator, Clover needle, CYC body-table, chart/care-geometry, neutral historical units, lower-case mass and date-range repair evidence. These receipts are reused only for unchanged implementation/factual inputs; they do not approve Danish translations. |

The attempted SUS care PDF was unavailable and is **not** used as evidence. A GINETEX navigation page that did not expose the relevant symbol definitions was likewise not treated as proof; the actual publisher PDFs above resolved the question.

## Batch tracker

| Batch | Owned/reviewed ledger rows | Progress | Verdict | Open findings | Evidence |
| --- | ---: | --- | --- | --- | --- |
| B01 | 27/27 | REVIEW_COMPLETE | PASS | None | READ-B01; coverage.tsv |
| B02 | 104/104 | REVIEW_COMPLETE | PASS | None | READ-B02; coverage.tsv |
| B03 | 95/95 | REVIEW_COMPLETE | PASS | None | READ-B03; coverage.tsv |
| B04 | 42/42 | REVIEW_COMPLETE | PASS | None | READ-B04; coverage.tsv |
| B05 | 128/128 | REVIEW_COMPLETE | PASS | None | READ-B05; coverage.tsv |
| B06 | 88/88 | REVIEW_COMPLETE | PASS | None | READ-B06; coverage.tsv |
| B07 | 70/70 | REVIEW_COMPLETE | PASS | None | READ-B07; coverage.tsv |
| B08 | 77/77 | REVIEW_COMPLETE | PASS | None | READ-B08; coverage.tsv |
| B09 | 77/77 | REVIEW_COMPLETE | PASS | None | READ-B09; coverage.tsv |
| B10 | 68/68 | REVIEW_COMPLETE | PASS | None | READ-B10; coverage.tsv |
| B11 | 77/77 | REVIEW_COMPLETE | PASS | None | READ-B11; coverage.tsv |
| B12 | 34/34 | REVIEW_COMPLETE | PASS | None | READ-B12; coverage.tsv |
| B13 | 78/78 | REVIEW_COMPLETE | PASS | None | READ-B13; coverage.tsv |
| B14 | 35/35 | REVIEW_COMPLETE | PASS | None | READ-B14; coverage.tsv |
| B15 | 23/23 | REVIEW_COMPLETE | PASS | None | READ-B15; coverage.tsv |
| B16 | 14/14 | REVIEW_COMPLETE | PASS | None | READ-B16; coverage.tsv |
| B17 | 156/156 | REVIEW_COMPLETE | PASS | None | READ-B17; coverage.tsv |
| B18 | 234/234 | REVIEW_COMPLETE | PASS | None | READ-B18; coverage.tsv |
| B19 | 82/82 | REVIEW_COMPLETE | PASS | None | READ-B19; coverage.tsv |
| B20 | 36/36 | REVIEW_COMPLETE | PASS | None | READ-B20; coverage.tsv |
| B21 | 28/28 | REVIEW_COMPLETE | PASS | None | READ-B21; coverage.tsv |
| B22 | 67/67 | REVIEW_COMPLETE | PASS | None | READ-B22; coverage.tsv |
| B23 | 9/9 | REVIEW_COMPLETE | PASS | None | READ-B23; coverage.tsv |

Exact owned items, variants, repeated caller references and additional meanings are the corresponding `batch` rows in `coverage.tsv`. Each row records the current text/source hash, content decision, evidence and linked finding where applicable. The reading notes below define the `READ-Bxx` references.

**READ-B01.** Read Danish controls, navigation and units in their actual caller roles; imperative actions, noun headings and unchanged cm/mm/g/percent notation remain distinct.

**READ-B02.** Read project/folder create, edit, selection, completion, reopen and deletion copy; folder deletion preserves projects; virtual filters are not project states; inserted user names remain opaque.

**READ-B03.** Read ROWS/ROUNDS/REPEATS/CUSTOM, target before/at/after, ordinal position versus quantity, step one/many and widget wording against MainCounterDisplayText/CounterValueFormatter. Generic adjustment actions are not stitch shaping.

**READ-B04.** Read linked and independent extras, repeating/shaping/section counters and reminder start/interval/occurrence wording against ProjectCounterLogic/CounterValueFormatter; a repeat index is not a row within a repeat.

**READ-B05.** Read sessions, recovery, history, completion events and Insights. Checked active-date denominator, session-zone split, display duration and neutral historical counter wording; see SHARED and context cases. No fresh runtime claim.

**READ-B06.** Read yarn metadata, stash/package quantity, thickness and care labels; paired all care IDs with drawing dispatch; retained international category names, removed-ply shared repair applies. S1/S2/S3/S6/S8/S11 and SHARED.

**READ-B07.** Read planned/allocated/used, null versus zero, remainder/overage and positive conversion basis against YarnUsageCalculator/Presentation; no stash deduction; saving project yarn metadata preserves project note.

**READ-B08.** Read PDF versus web metadata, free/paid/unknown, Ravelry browser and saved-pattern actions; delete/unlink wording matches reference-aware repository behavior; designer length alias resolves to neutral limit text.

**READ-B09.** Read import/camera/gallery/document names and limits; copied local PDF, page order/primary relation, 20 pages/25 MiB image/200 MiB selection, cancellation and failure semantics; no OCR/sync promise.

**READ-B10.** Read all bookmark/reading-guide/marker/calibration/follow copy; page and counter position remain distinct, manual movement pauses follow, bookmark and guides are excluded from annotated export.

**READ-B11.** Read all annotation tools/layers/selection/history/export copy; project overlay is editable, shared master is read-only, export includes selected layers and preserves source; progress and error actions remain distinct.

**READ-B12.** Read local notes/save state and progress/photo library actions; inserted project names and dates remain data; photo counter snapshots use neutral label, delete affects selected photo scope.

**READ-B13.** Read independent stitch/row gauge axes, 10 cm versus 4 in basis, swatch/canonical values, exact/rounded outputs, warnings and Copy payload against GaugePresentation/MeasurementCalculator; localized parsing is separate from display.

**READ-B14.** Read increase/decrease error arguments, flat/circular distribution and English generated K/M1/K2tog notation; Danish help now defines it. Source arithmetic checks consume original stitches; M1 distinct bars and minimum two preserved.

**READ-B15.** Read desired/actual width, total edge stitches, gauge basis and nearest repeat; source calculations retain exact count before repeat rounding and add edges after repeat; alternatives are adjacent valid repeat totals.

**READ-B16.** Read required/package length, fractional versus whole purchased packages and mass; 450/200 gives 2.25, purchase 3, at 50 g gives 150 g; label now states whole packages and avoids universal extra-ball instruction.

**READ-B17.** Read every Danish heading/age/unit and all source table cells; body measurements not finished sizes, metric source/ranges preserved and inches derived /2.54; regional needle IDs not decimal measurements. SHARED supplies factual table receipts.

**READ-B18.** Read every English token with Danish meaning and expanded description; ret/vrang, slip orientation, stitch legs, yarn side, short rows, fabrics and cable holds checked independently using S1/S2/S3/S4/S9/S10.

**READ-B19.** Read every symbol/name/description and drawing ID; right-side convention, decrease lean, CDD and four/six-cell cables checked against source dispatch and SHARED factual geometry evidence. No fresh rendered-icon inspection.

**READ-B20.** Read settings, native language names, system default, theme, consent/privacy and app update text against PreferencesManager/PostHogAnalytics/config; opt-in and allowlisted data promises checked in source.

**READ-B21.** Read backup/export/preview/replace warnings and errors against BackupRepository/Tables; restore replaces after validation, not sync; app-owned included data versus settings/billing exclusions retained. No restore executed.

**READ-B22.** Read trial/Pro/free-limit/purchase/restore wording against ProState/ProManager/TrialManager and callers; existing-content access and debug-only override distinguished; price is supplied by billing, no invented price or purchase test.

**READ-B23.** Reconciled residual numeric/date/filename text, all current resource variants and app-owned source text; third-party pages, user documents/content, diagnostic logs and developer tooling are not Danish app copy.

## Findings and minimal corrections

**96 text variants in 95 keys corrected: 94 strings and both variants of one plural.** No keys, quantities, placeholders, array structures, implementation or shared tests were added or removed.

`corrections.tsv` is the full finding register using the plan's finding fields: unique LA-da-Bxx IDs, category/severity, classification, exact file/key/variant, original text, smallest justified replacement, specific problem/user consequence, context evidence, final verification, shared impact and remaining uncertainty. Individual unrelated errors have separate IDs. The cause groups below are navigation aids, not a substitution for the per-key record. All findings are **CONFIRMED_DEFECT / VERIFIED**; none remains open.

| Cause group | Changed variants | Necessary correction |
| --- | ---: | --- |
| DA-01 | 14 | Specify ret/vrang operation and insertion rather than generic knitting or ambiguous side words. |
| DA-02 | 6 | Clarify slipped-stitch transfer, retention and required orientation/paired transfer. |
| DA-03 | 16 | Replace incorrect physical-cable terminology and describe cable hold positions/stitch counts. |
| DA-04 | 4 | Remove false front/back versus right/wrong-side equivalences and literal public/private fabric-side wording. |
| DA-05 | 17 | Correct strand/stitch-leg/wrap/short-row technical calques. |
| DA-06 | 9 | Correct confirmed grammar, compounds, typo, abbreviation punctuation and false-friend range/streak wording. |
| DA-07 | 4 | Generic counter and yarn-quantity actions must describe numeric adjustment, not stitch shaping. |
| DA-08 | 2 | Identify computed whole-package mass; remove unsupported always-buy-an-extra instruction. |
| DA-09 | 1 | Project usage covers independent planned/allocated/used fields, not only allocated yarn. |
| DA-10 | 1 | Paid availability identifies a paid pattern, not a completed purchase. |
| DA-11 | 7 | Correct premature/preteen/month age labels without altering table values. |
| DA-12 | 3 | State actual needle-system limitation, right-side chart convention and English abbreviation language. |
| DA-13 | 2 | Name professional cleaning processes while retaining P/F distinction. |
| DA-14 | 1 | Explain existing generated English calculator notation in its existing Danish help. |
| DA-15 | 9 | Shared row controls and Insights also serve crochet; use række and neutral recorded-time wording there. |

Changes were selected for a specific error or comprehension consequence, not to reach a count. The final pass found DA-15 by tracing explicit CROCHET+ROWS through `resolvedMainCounterLabelType` and `MainCounterDisplayText`; this was not a mechanical pind replacement. Physical needles, knitting reference instructions, flat-knitting help and knitting-gauge terminology retain their correct wording.

## Shared handoffs and other-language impact

**No new shared repair handoff or unresolved shared dependency.** Existing shared changes were independently reconciled with Danish:

- B03/B05/B10/B12: historical counts use neutral value/progress wording because sessions/photos do not preserve historical units. Current selected units still use proper quantities. Existing suppression of historical row/pace metrics is retained.
- B05-01: inspected current range bounds and stored-zone handling. Reused the latest English repair's **107/107 JVM tests in 11 suites**, explicitly as prior shared evidence, not tests run here. Danish compositions were re-read for active 1/1, 2/2, 0/1 and 3/3 days, retained durations, today-ended ranges and same-/cross-month labels. The old OPEN wording in historical summaries is not the current repair outcome.
- B06/B16: no unsupported ply hints; actual care drawing dispatch and selected label meaning checked; g remains lower-case and mass refers to whole packages.
- B14/B15: M1 uses available distinct bars; no final flat-edge M1; nearest-repeat selection uses exact count. Danish help/error/result meaning was checked against these corrected paths and manual arithmetic.
- B17: all 38 needle rows and all 123 body-measurement cells were checked against current mappings and the shared Clover/CYC factual receipts; source age categories, ranges and derived inches remain intact.
- B19: all 24 Danish triples were paired with the actual renderer branches and adopted right-side convention. Existing shared image/geometry receipts support unchanged drawings; no new visual inspection is claimed.

The B08 explanation-measurement follow-up was a shared test-helper repair, not evidence of a Danish text defect. No other locale's resource or acceptance record was edited. A global campaign table remains outside this task's edit boundary; this directory contains the Danish decision.

## Validation log

| Date | Check | Actual result and limitation |
| --- | --- | --- |
| 2026-10-19 | Full Danish XML reading, caller review, terminology sources and code/data reconciliation | All 1,380 current variants plus 22 material resource contexts and 247 code/data records reviewed. Naturalness is reviewer judgment, not established by a unit test. |
| 2026-10-19 | XML parse; default/target key/type/quantity and formatting-token comparison; alias resolution | PASS. Exact 1,349-key parity; 31 one/other plurals; no arrays; one valid alias; two intentionally nontranslatable numeric defaults separately accounted for. |
| 2026-10-19 | `:app:processDebugResources` plus three targeted test classes | Initial command PASS in 46 s. After final DA-15 edits, resource build PASS in 16 s; its test task was UP-TO-DATE, so that was not counted as fresh final test execution. |
| 2026-10-19 | `:app:testDebugUnitTest --rerun` with the three selected classes | PASS in 23 s; **5/5 tests**, zero failure/error/skip. After the last two wording reductions, the combined resource build and explicit test rerun also PASS in 25 s (8 tasks executed). Its final XML timestamps and hashes are recorded in validation.json. |
| 2026-10-19 | Final coverage set, current row/file hashes, original/final correction reconciliation | PASS: every current variant exactly once in the primary denominator; all 96 changes have exact initial/final text and verification. |
| 2026-10-19 | Starting-file SHA-256 preservation and scoped `git diff --check` | PASS. Only the two authorized Danish resource files differ from the starting pre-existing file snapshot. No whitespace errors; Git reports existing CRLF normalization warnings. |
| 2026-10-19 | Temporary process closeout | One-shot Python/PowerShell commands and the single-use Gradle invocations ended. No emulator, server, watcher or browser automation was started. No unrelated process was stopped. |

Selected tests: `ReferenceResourceLocalizationTest` (2), `CountPluralResourcesSourceTest` (1), `InstructionHintLocalizationTest` (2). Final command:

```powershell
.\gradlew.bat :app:testDebugUnitTest --rerun `
  --tests 'com.finnvek.knittools.ReferenceResourceLocalizationTest' `
  --tests 'com.finnvek.knittools.ui.CountPluralResourcesSourceTest' `
  --tests 'com.finnvek.knittools.InstructionHintLocalizationTest' `
  --console=plain --no-daemon
```

Resource build used `:app:processDebugResources` with the same test filters. Logs: `validation-gradle.log`, `validation-gradle-final.log`, `validation-tests-final.log`, `validation-final.log`; durable results and receipt hashes: `validation.json`. Gradle emitted its existing future Gradle-10 deprecation notice; no check was weakened or disabled.

## Native-speaker review

- **NOT_REQUESTED**. No human native-speaker or professional Danish knitting/crochet reviewer participated.
- No reviewer identity, device execution, screenshot inspection or spoken-output result is implied.

## Closeout

**B01–B23 REVIEW_COMPLETE / PASS; Danish AUDIT_PASS.** Complete current content coverage, independent terminology/naturalness/context review, corrected and verified findings, current input hashes and passing relevant resource checks satisfy revised section 16. No confirmed defect or specific unresolved language/comprehension question remains.

No runtime check was necessary to resolve the named content questions: callers, actual data/calculations and reliable craft sources resolved them. Missing screenshots alone do not block acceptance. This is not exhaustive UI/rendering certification. No 320 dp, 200% font scale, combination or TalkBack check was repeated; no per-screen rendering backlog was created.

## Resume point

- Last completed scope: B23, reconciliation of all Danish B01–B23 content against final inputs.
- Unfinished assigned work: none. Stop after Danish.
- No next language, runtime campaign, commit, push, PR or message to another chat is authorized or performed.
- Future changes to a recorded input require only the affected Danish content/context to be reconsidered; this decision does not approve future text.
