# French Language Audit (fr)

## Scope and snapshot

- Language: neutral French, with French craft vocabulary understandable in France and other French-speaking regions. No new regional voice was imposed.
- Assigned boundary: all B01–B23 French content, necessary French-only corrections, and revised section 16 acceptance. Stop after French.
- Start / final update: 2026-10-19.
- Branch: `codex/audit-korjaukset-20261018`; HEAD: `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`.
- The working tree already contained extensive shared implementation, test and multilingual resource changes, including three French XML files. `entry-snapshot.json` preserves the starting status, 1,190 file hashes and the original four French files. Only the French resources and this locale's audit records were changed in this task.
- Final identity: `input-hashes.json` contains 137 current input hashes. HEAD alone does not identify the audited working tree. The per-row resource/source hashes are in `coverage.tsv`.
- Current resources: **1,349 translatable keys = 1,318 strings + 31 plurals**. French declares **one, many, other** for each plural: 93 plural variants, hence **1,411 resource variants**. No string arrays.
- Coverage: **1,854 rows**, comprising 1,318 strings, 93 plural forms, 87 code-text items, 275 reference-data items, 44 symbol pairs and 37 materially different generated/context compositions. The 87 code texts comprise 77 English notation tokens, eight yarn-category names and two nontranslatable numeric examples.
- Four French XML files were read. Three changed; `pattern_annotations.xml` did not. Corrections are **94 variants in 92 keys**, relative to entry, not to HEAD.
- Earlier language records supplied shared factual evidence and a batch-ownership navigation aid only. French text and context decisions were made independently. No earlier language's pass was treated as French approval.
- Exclusions: other locales, shared implementation/tests, external Ravelry web content, user-created names/content and full UI certification. No new rendering campaign, 320 dp, 200% font scale, combined case or TalkBack test.

## Language decisions and glossary

The established voice is retained: infinitive action labels and vous/imperative instructions. Article omission in short labels is intentional where natural. Curly apostrophes and valid accents remain; Android escapes and formatting tokens must survive. Correct alternatives are not normalized merely for stylistic consistency.

| Concept | Approved expression/forms | Context | Alternatives and exclusions | Evidence | Status |
| --- | --- | --- | --- | --- | --- |
| Individual stitch / stitch pattern | maille / point, motif | Loops versus fabric pattern | Point is valid for point de riz, not an individual loop instruction | S-DROPS; S-CYC-ABBR | PASS |
| Row / round | rang / tour | Flat / circular work and craft-aware counters | Generic Compteur when the unit is not fixed; never infer old session units from an editable label | SOURCE; READ-B03/B05 | PASS |
| Gauge / swatch | échantillon; number of mailles/rangs per measurement | Separate density values and sample instructions | Do not substitute a rounded display value into arithmetic | S-DROPS; GaugePresentation | PASS |
| Twisted stitch / cable | maille torse / torsade | Loop orientation versus crossed groups | Retain understandable boucle arrière and English TBL explanations | S-DROPS; S-CYC-ABBR | PASS |
| Increase / decrease | augmentation / diminution | M1, K2tog, SSK, SSP, S2KP and lifted increases | Operations must preserve orientation and whether stitches slip separately/together | S-PHILDAR; S-BISCOTTE; S-CYC-ABBR | PASS |
| Repeat | répétition; Répétition n° | Repeated pattern or occurrence | Répéter is an action, not the occurrence noun | READ-B04 | PASS |
| Yarn amounts | planned, allocated and used French labels remain distinct | Unknown, zero, remaining and overage | No automatic global stash mutation; estimates and physical stock counts differ | YarnUsagePresentation; READ-B07/B16 | PASS |
| English notation | K, M1, K2tog and reference abbreviation tokens | Imported/generated knitting notation | Explain notation in French; do not translate fixed code tokens | READ-B14/B18 | PASS |
| Care | temperature maximum; solvent groups P/F | Label paired with symbol and explanation | P/F are not generic ordinary/gentle variants | S-CARE; S-GINETEX | PASS |
| Percentage and numeric count | nonbreaking space before %; digits + counted noun | Android escaped %% and plurals | Keep one/many/other; written full digits do not gain de | S-SPACING; S-OQLF; S-ANDROID | PASS |

## Sources

| ID | Title and publisher | URL | Language/region | Access date | Claim and limitation |
| --- | --- | --- | --- | --- | --- |
| S-DROPS | DROPS Design bilingual knitting lexicon | https://www.garnstudio.com/glossary.php?langf=en&langt=fr&lang=fr | French / English | 2026-10-19 | Maille, rang, échantillon and technique vocabulary; regional alternatives are not automatically defects. |
| S-PHILDAR | Phildar / Woolschool increases and knitting basics | https://woolschool.phildar.com/tricot-augmentations-intercalaires | French | 2026-10-19 | Intercalary increases and loop orientation; checked together with actual operation, not names alone. |
| S-BISCOTTE | Les Laines Biscotte intercalary increases | https://leslainesbiscotte.com/fr/blogs/tricot-101/left-simple-inset-increase-and-right-simple-inset-increase | French / Canada | 2026-10-19 | French operation wording and directional increase distinction. |
| S-CYC-ABBR | Craft Yarn Council knitting abbreviations | https://www.craftyarncouncil.com/standards/knitting-abbreviations | English / US | 2026-10-19 | Meaning of the English abbreviation tokens; French naturalness independently reviewed. |
| S-CYC-CHART | Craft Yarn Council knit chart symbols | https://www.craftyarncouncil.com/standards/knit-chart-symbols | English / US | 2026-10-19 | Chart symbol conventions; pattern-specific legends take precedence. |
| S-CARE | COFREET care guide | https://www.lavermonlinge.com/userfiles/files/COFREET%20GUIDE%20VF.pdf | French | 2026-10-19 | Maximum wash temperatures and P/F solvent groups, not ordinary/gentle cleaning strength. |
| S-GINETEX | GINETEX competence center | https://www.ginetex.net/thematique/FR/labelling/ginetex-competence-center | French | 2026-10-19 | Authoritative care-symbol framework; shared glyph checks remain historical evidence. |
| S-OQLF | OQLF number reading | https://vitrinelinguistique.oqlf.gouv.qc.ca/25100/la-prononciation/prononciation-des-nombres/regles-generales-de-lecture-des-nombres | French / Canada | 2026-10-19 | Distinguish spoken million de from a noun directly following a full written numeric count. |
| S-SPACING | Government of Canada cardinal numbers | https://nos-langues.canada.ca/fr/cles-de-la-redaction/nombres-cardinaux | French / Canada | 2026-10-19 | French number/unit and percent spacing; no wholesale typography rewrite. |
| S-ANDROID | Android string resources and plurals | https://developer.android.com/guide/topics/resources/string-resource#Plurals | English | 2026-10-19 | Quantity categories are language-specific; formatting count and selecting category are separate. |
| S-CLDR | Unicode CLDR French numbers | https://unicode.org/cldr/charts/40/verify/numbers/fr.html | French examples | 2026-10-19 | French one/many/other and decimal behavior; actual Android evidence is separately identified. |

Production code and current resource text are the authority for product behavior. French craft references establish terminology and technique meaning; English CYC documents establish notation semantics. They do not establish French naturalness by themselves.

## Batch tracker

Counts include primary variants and explicitly separate code/data/context items. Repeated identical callers are grouped; they are not counted as fresh language variants.

| Batch | Owned items | Reviewed items | Progress | Verdict | Open finding IDs | Evidence |
| --- | ---: | ---: | --- | --- | --- | --- |
| B01 | 28 | 28 | COMPLETE | PASS | None | READ-B01; coverage.tsv |
| B02 | 110 | 110 | COMPLETE | PASS | None | READ-B02; coverage.tsv |
| B03 | 85 | 85 | COMPLETE | PASS | None | READ-B03; coverage.tsv |
| B04 | 44 | 44 | COMPLETE | PASS | None | READ-B04; coverage.tsv |
| B05 | 138 | 138 | COMPLETE | PASS | None | READ-B05; coverage.tsv |
| B06 | 91 | 91 | COMPLETE | PASS | None | READ-B06; coverage.tsv |
| B07 | 70 | 70 | COMPLETE | PASS | None | READ-B07; coverage.tsv |
| B08 | 77 | 77 | COMPLETE | PASS | None | READ-B08; coverage.tsv |
| B09 | 79 | 79 | COMPLETE | PASS | None | READ-B09; coverage.tsv |
| B10 | 70 | 70 | COMPLETE | PASS | None | READ-B10; coverage.tsv |
| B11 | 78 | 78 | COMPLETE | PASS | None | READ-B11; coverage.tsv |
| B12 | 33 | 33 | COMPLETE | PASS | None | READ-B12; coverage.tsv |
| B13 | 80 | 80 | COMPLETE | PASS | None | READ-B13; coverage.tsv |
| B14 | 33 | 33 | COMPLETE | PASS | None | READ-B14; coverage.tsv |
| B15 | 21 | 21 | COMPLETE | PASS | None | READ-B15; coverage.tsv |
| B16 | 13 | 13 | COMPLETE | PASS | None | READ-B16; coverage.tsv |
| B17 | 349 | 349 | COMPLETE | PASS | None | READ-B17; coverage.tsv |
| B18 | 234 | 234 | COMPLETE | PASS | None | READ-B18; coverage.tsv |
| B19 | 82 | 82 | COMPLETE | PASS | None | READ-B19; coverage.tsv |
| B20 | 36 | 36 | COMPLETE | PASS | None | READ-B20; coverage.tsv |
| B21 | 29 | 29 | COMPLETE | PASS | None | READ-B21; coverage.tsv |
| B22 | 70 | 70 | COMPLETE | PASS | None | READ-B22; coverage.tsv |
| B23 | 4 | 4 | COMPLETE | PASS | None | READ-B23; coverage.tsv |

## Content evidence and context decisions

### READ-B01

Read shared actions, navigation, nouns, errors and units with their callers. Actions use French infinitives; instructional prose uses vous/imperatives. These are compatible, not inconsistent address. The shared selection count now avoids gender and singular/plural mismatch across projects, patterns, yarn and photos.

### READ-B02

Read project creation, naming, craft type, custom counter labels, completion/reopening and folders. Names are user data, not untranslated app copy. Active/completed states, folder deletion versus project deletion and bulk moves have distinct consequences. No change needed.

### READ-B03

Read main, secondary, linked and repeating counter labels and widget compositions. CounterValueFormatter/MainCounterDisplayText distinguish rows, rounds, stitches, repeats and custom names; three generic labels were incorrectly row-specific. Every one/many/other variant was read independently; numeric many counts must not acquire a written de.

### READ-B04

Read reminder trigger intervals, repeats, occurrence numbers, shaping actions and session recovery. Repetition occurrence is a noun followed by its ordinal number, not an instruction to repeat. Interval agreement follows incréments. Start/stop/review/discard retain distinct meanings; the existing active clock description is understandable.

### READ-B05

Read loading, empty, range/project filters, time totals, completions, streaks, charts and descriptions. Current historical progress is deliberately neutral because sessions have no historical unit snapshot. Empty copy now covers crochet and non-row projects. Reconciled the repaired today-ended active-date set and All Time range from shared evidence; no remaining French date-range question.

### READ-B06

Read yarn metadata, stash state, composition, weight categories and all 20 care symbol/name/description pairs. Temperature labels express a maximum and P/F identify solvent groups. Care diagrams use existing shared validation; French instructions were independently checked against COFREET/GINETEX. Metadata amounts are not automatically project usage.

### READ-B07

Read project-only yarn notes and usage forms, units, validation, source snapshots, save-to-stash and null/zero/overage. Planned, allocated and used are independent values, so the scope copy no longer makes allocation mandatory. YarnUsagePresentation confirms French decimal skein rules; saving notes does not automatically create usage.

### READ-B08

Read saved patterns, web links, Ravelry browser, duplicates, errors, paid/free/unknown availability and linking. Metadata-only web patterns do not promise an offline PDF. URL validation and Ravelry routing agree with French actions. The designer-length error alias intentionally reuses the same 120-character title limit text.

### READ-B09

Read incoming PDF, camera/gallery import, document selection, ordering, rename, primary selection, removals and export. Photo/PDF wording matches the implemented flow. Checked the 20-page/image/session limits and file attachment consequences against production sources; no French correction needed.

### READ-B10

Read reader guides, following/manual state, calibration, page and bookmark actions. Four formatted percentages needed a French nonbreaking space before the percent sign; Android %% escaping is preserved. Calibration repère labels were retained as valid. Guides and bookmarks do not claim to be annotations.

### READ-B11

Read all annotation resources, layers, undo/redo, colors/tools, errors and marked-PDF export. The saved master is read-only in projects; project overlays are editable. Export creates a separate PDF and preserves the source. No resource in pattern_annotations.xml required a change.

### READ-B12

Read notes, formatting, editing/save/discard and attachment-related context. Notes remain local, and project names are inserted as user-provided labels. No new cloud processing or automatic cleanup promise; no correction needed.

### READ-B13

Read progress photo acquisition, empty/gallery/viewer states and removal/share consequences. Photo strings refer to project photos rather than pattern scans. Delete and cancelled capture behavior match source; no correction needed.

### READ-B14

Read increase/decrease and paste-parser wording against calculation and generated English notation. The tool applies to a row or round. Added the local explanation of K/M1/K2tog and the total repetition count because the generated notation itself is intentionally English. Calculator arithmetic and limits reuse unchanged shared factual evidence.

### READ-B15

Read measurements/gauge labels, provenance, units, validation, outputs and copy composition against GaugePresentation/GaugeViewModel. Independent stitch/row density, canonical precision and displayed rounding are preserved. Swatch is échantillon; maille and rang are counted independently. No rewrite needed.

### READ-B16

Read yarn estimator inputs, estimates, skein conversion and allowance. Total weight belongs to the full ball/skein, not the project or a sample. A decimal estimate cannot share an unconditional plural label, so the invariant estimate wording is used. The existing allowance does not always mean one extra skein.

### READ-B17

Read every heading and all 275 numeric reference fields: 38 needle rows × four cells and 123 populated size measurements (including 58 range records). Reviewed all six size groups, with 5/8/9/8/7/9 rows. Baby sizes identify ages 3/6/12/18 months, not intervals. Circumferences and underarm length are explicit. Metric needle size is precise while US/UK designations vary. Numeric data and applicable source checks are unchanged.

### READ-B18

Read all 77 English abbreviation tokens and all French meanings/descriptions. Title identifies the English notation; tokens themselves are intentionally not translated. Individual loops use maille; motif/point remains valid for stitch patterns. Distinguished torse from torsade, knitwise slipping from knitting, SSK from simultaneous S2KP slipping, SSP reorientation, M1 pickup, PFB loop legs, cable hold direction, yarn relative to the knitter, wrap-and-turn, flat stockinette and seed stitch. Valid regional/technical alternatives were retained.

### READ-B19

Read all 24 chart symbol/name/description pairs and drawing branches, including twisted stitches, slip, decreases, lifted/bar increases, cables, no-stitch and marker. State chart/right-side conventions and prioritize the pattern legend. A twisted stitch is not a cable; a blank no-stitch square is not a worked stitch. Existing shared glyph evidence is reused only for unchanged drawing inputs.

### READ-B20

Read language/units, appearance, haptics, analytics, settings descriptions and their actual preference consumers. General imperial units and Gauge-specific choices are distinct: the latter can override the default. Opt-in usage analytics text has no content-upload claim.

### READ-B21

Read backup/export/import, replacement warnings, errors and file choices against BackupRepository. Import replaces the authoritative data transactionally rather than harmlessly adding a few projects. Cancellation and file availability wording remain accurate. No correction needed.

### READ-B22

Read Pro feature groups, trials/purchases, restoration, limits and existing-content access against ProState. Reminder gates concern counter progress, not exclusively rows, so three prompts were corrected. Debug feature access is not a purchase; no product claim was inferred from the debug override.

### READ-B23

Read remaining credits/license/support copy and reconcile every included resource, code-backed label, alias and orphan. No remaining missing translation, open content question or shared handoff was found.

### Composition and resource reconciliation

`coverage.tsv` uses the LANGUAGE_AUDIT.md schema. Text cells preserve TSV escaping for literal tabs/newlines. `CALLERS` refers to `caller-inventory.json` (1,777 references, with source snippets); `SOURCE` refers to the row's source and caller paths. `READ-Bnn` refers to the explicit independent reading decision above. `RES` refers to final resource reconciliation and Gradle validation below. `SHARED` refers to `shared-evidence-reconciliation.tsv` and `../shared-fixes.md`; `ANDROID-REUSED` refers to the historical evidence described below.

All 1,411 primary resource variants have exact key/type/quantity ownership and final input hashes. All placeholders and the one resource alias match the default contract. `web_pattern_error_designer_too_long` intentionally aliases the title-length error because both limits are 120. Two default-only nontranslatable examples (`widget_preview_primary_count` = 42, `counter_step_custom_placeholder` = 10) are accounted for as numeric code-text rows, not missing French translations.

Eleven retained unused keys were still read: `size_col_head`, `size_col_height`, `size_row_adult_s`, `size_row_adult_m`, `size_row_adult_l`, `size_row_adult_xl`, `size_row_men_xs_30_32`, `sym_slip_knitwise_name`, `sym_slip_knitwise_desc`, `yarn_weight_ply_format`, `insights_rows_count`. Their lack of a current production caller does not justify inventing runtime evidence or deleting content in this task.

The 37 separate composition records cover mixed-gender selection, each main-counter unit and target/custom/widget contexts, repeat/shaping occurrences, Insights date/range/history-unit contexts, durations, completed-project counts, integer and fractional skeins, null/zero/overage, web metadata, PDF import/reader/annotation actions, notes/photos, gauge copying and calculation, generated knitting notation, cast-on and yarn estimates, tables, units, backup, Pro, filenames and date formatting. Illustrative combinations are source-derived examples, not newly executed Android fixtures. All 182 formatted-resource caller argument types were reviewed, with detailed inspection where inserted values change meaning.

### French quantity selection and evidence reuse

Android quantity selection is not English count == 1. French integer 0 and 1 select one; many can apply to exact million counts with newer ICU data, while older implementations can select other. Both forms must therefore be independently correct. `rounds_format` and `repeats_format` now use the full numeric count followed directly by the noun, including many. Integer skein counts and decimal usage are separate presentation paths.

The successful 2026-10-18 `YarnUsageLocalizationTest` evidence is reused from `reports/language-audit/en/20261018/instrumentation.log`, lines 454–482, for `fractionalSkeinCountUsesRegionalPluralRules` and `roundedSkeinCountMatchesDisplayedQuantity`. Current test assertions include fr-FR 0.5 → “0,5 écheveau”, 1.994 → “1,99 écheveau”, and 1.996 → “2 écheveaux”. The three relevant production hashes still match the historical start-inputs record; relevant French plural texts were not changed. `reused-android-evidence.json` records the reconciliation. This is historical Android execution plus current source reconciliation, **not a new device test**. `SkeinCountTextTest` was read for integer cases but is not claimed as newly executed here.

All 18 shared factual input hashes in `shared-evidence-reconciliation.tsv` match the reused evidence. The independently checked French meanings remain compatible with current calculation/table/glyph sources and the recorded Insights range repair. Shared evidence does not replace the French language reading.

## Findings

See [corrections.md](corrections.md) for original text, final text, exact defect, consequence, affected contexts, classification, severity and verification boundary for all **94 corrected variants / 92 keys**. Machine-readable exact changes are in `corrections.json`.

- Category totals: TERM 36, CONTEXT 30, FACT 19, LANGUAGE 9.
- Severity totals: S1 11, S2 74, S3 9; all VERIFIED, no OPEN findings.
- Seven candidate rewrites were withdrawn after the second reading because the existing expression was valid. `retained-alternatives.json` records these; they are not counted as changes.
- Changes target actual instructions, wrong unit/context claims, missing technical distinctions and grammatical/formatting errors. No target change count or global stylistic rewrite was used.

## Shared handoffs and other-language impact

No newly confirmed shared defect or handoff remains. Other locales, shared implementation and shared tests were preserved. Existing shared fixes are described only to explain applicable evidence. The entry-hash comparison found no changed starting file outside the French scope.

## Validation log

| Date | Input/build identity | Check | Scope | Actual result | Evidence/limitation |
| --- | --- | --- | --- | --- | --- |
| 2026-10-19 | Final working tree; input-hashes.json | XML/key/type/quantity/placeholder/alias reconciliation | All four French XML files, default contracts | PASS: 1,349 keys / 1,411 variants | validation.json; no UI claim |
| 2026-10-19 | Final working tree | Coverage identity, variant ownership and SHA-256 reconciliation | All 1,854 rows | PASS; unique IDs and complete variants | coverage.tsv; validation.json |
| 2026-10-19 | Earlier intermediate state | Initial resource build and two resource test classes | Resources; tests restored FROM-CACHE | Successful, superseded by final run | validation-gradle.log; not relied on as final execution |
| 2026-10-19 | Final corrected resources; Gradle 9.8.1, configured JDK 17 | `:app:processDebugResources :app:testDebugUnitTest --rerun` with the two class filters below | Android resource processing and three targeted resource tests | BUILD SUCCESSFUL in 2m43s; test task executed; 3 tests, 0 failures/errors/skips | validation-final-gradle.log and copied TEST XML receipts |
| 2026-10-19 | Entry versus final working tree | Starting-file hashes and branch comparison | Preservation boundary | No outside-scope starting file changed; branch unchanged | entry-snapshot.json; validation.json |
| 2026-10-19 | Final scoped diff | `git diff --check -- app/src/main/res/values-fr config/language-audit/fr` | Changed French files | PASS | Only CRLF-to-LF informational notices |
| 2026-10-19 | 18 current shared inputs | Compare factual evidence source hashes and re-read French meaning | Tables, calculators, glyph/context sources | 18/18 MATCH | shared-evidence-reconciliation.tsv; historical tests not rerun |
| 2026-10-19 | Three unchanged production inputs and relevant French plurals | Reconcile 2026-10-18 Android fractional skein results | French quantity/rounding behavior | Applicable historical evidence | reused-android-evidence.json; no fresh Android execution |

Final command:

```powershell
.\gradlew.bat :app:processDebugResources :app:testDebugUnitTest --rerun --tests 'com.finnvek.knittools.ReferenceResourceLocalizationTest' --tests 'com.finnvek.knittools.ui.CountPluralResourcesSourceTest' --console=plain --no-daemon
```

Test receipts: `ReferenceResourceLocalizationTest` has two tests; `CountPluralResourcesSourceTest` has one. All passed. The build emitted the existing Gradle 10 deprecation notice; it did not fail. No new tests, dependency versions or shared source changes were needed. No emulator, preview server or watcher was started. The single-use Gradle invocation completed.

## Native-speaker review

- Status: **NOT_REQUESTED**.
- No native-speaker or native craft-expert review is claimed.
- No specific unresolved content issue was left awaiting such a review.

## Closeout

- Coverage: **B01–B23 complete; 1,349 keys / 1,411 resource variants / 1,854 total records**.
- Dimensions: T/L/C PASS 1,854 each; F PASS 1,852 and NOT_APPLICABLE two numeric examples; R PASS 1,413 and NOT_APPLICABLE 441 non-resource items. N/A does not hide a product claim.
- All V cells are blank: this task makes no fresh rendering claim. No existing French pending rendering registry was cleared and no rendering backlog was created.
- All 94 corrected variants were re-read in final form and validated; no confirmed defect or specific unresolved language/comprehension question remains.
- **French verdict: AUDIT_PASS under revised section 16; native-speaker review NOT_REQUESTED.** The basis is complete current content coverage, resolved findings, applicable current resource checks and explicitly reconciled shared/Android evidence. This is not exhaustive UI certification.
- No commit, push, pull request or message to another chat. No subsequent language task started.

## Resume point

Last completed batch: B23 and final all-batch reconciliation. No unfinished assigned work. Future source/resource changes invalidate the corresponding input hashes and affected meanings; no additional task or runtime campaign is proposed here.
