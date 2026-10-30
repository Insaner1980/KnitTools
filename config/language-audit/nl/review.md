# Dutch content audit — B01–B23

Date: 2026-10-19. Locale: `nl` (Dutch; existing je/jouw address retained).

**Decision: AUDIT_PASS under revised LANGUAGE_AUDIT.md section 16. Native-speaker review: NOT_REQUESTED.** All current Dutch content is reviewed, authorized corrections verified, and no confirmed language defect or specific unresolved language question remains. This is content acceptance, not exhaustive UI certification.

## Scope and input identity

- Workspace `C:/Dev/KnitTools`; branch `codex/audit-korjaukset-20261018`; entry HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`.
- Instructions read: AGENTS.md, CODEX.md, relevant CLAUDE.md wording, revised LANGUAGE_AUDIT.md, shared-fixes.md. AGENTS/CODEX match byte-for-byte. No existing nl audit directory was present at entry.
- The working tree already contained substantial shared implementation, test, instruction and locale changes. `entry-status.txt`, `entry-dutch.diff`, `entry-texts.json` and `entry-input-hashes.json` preserve that boundary. All pre-existing non-nl inputs compared unchanged after this work; see `validation.json`.
- Only `values-nl/strings.xml`, `values-nl/reference_strings.xml` and this nl evidence directory were edited. The two other Dutch resource files were reviewed but retain entry bytes. No dependency, shared source, shared test, other locale, index, commit, push, PR or chat message changed.
- `input-hashes.json` pins the final resource/caller/producer/instruction/evidence inputs. `shared-evidence-reconciliation.tsv` records 18/18 current shared source hashes matching the factual evidence baseline. Another language's acceptance was never used to approve Dutch prose.

## Current inventory and coverage

Fresh enumeration of the four `values-nl/*.xml` files gives **1,349 keys: 1,318 strings and 31 plurals / 62 declared plural forms, totaling 1,380 variants; no arrays**. Every variant was read. Default-only `widget_preview_primary_count` and `counter_step_custom_placeholder` are explicitly nontranslatable numeric samples, not missing translations. The one resource alias (`web_pattern_error_designer_too_long`) resolves to the existing 120-character limit text and is covered.

| File | Strings | Plural keys | Declared variants |
| --- | ---: | ---: | ---: |
| pattern_annotations.xml | 75 | 0 | 75 |
| pattern_reading_assistance.xml | 49 | 0 | 49 |
| reference_strings.xml | 257 | 0 | 257 |
| strings.xml | 937 | 31 | 999 |
| Total | 1,318 | 31 | 1,380 |

`coverage.tsv` has **1,816 rows**: 1,380 primary resource variants, 87 code-text entries (77 abbreviation tokens, eight canonical yarn categories, two nontranslatable samples), 275 reference-data cells, 44 symbol pairs and 30 generated/context compositions. The data inventory was extracted from current Kotlin: 38 needle rows / 152 cells; 46 body/head/hand rows / 123 measurements, including 58 ranges; 24 chart symbols; 20 care symbols. No earlier locale's counts or statuses were copied as Dutch proof.

Every row carries the current resource/source hash, caller references where applicable, an explicit reading/composition note and evidence IDs. Identical repeated uses share references. Material differences have separate composition rows: generic counter actions, bare versus unit-bearing targets, four selectable counter units and arbitrary names, occurrence versus interval, nested Insights dates/durations, null/zero/overage and fractional skeins, Gauge copy/units, calculator notation, hand-size prefixes, filenames versus user names, and date/clock generation. `caller-inventory.json` retains the inspected reference snippets; it is evidence, not a rendering backlog.

Eleven retained resources have no current production caller: the old Insights row-count quantity, old size columns/adult/men-XS labels, unsupported knitwise-slip symbol pair, and old yarn ply format. They were still read and are explicitly marked RETAINED_UNUSED. App/widget names have manifest callers. No unused resource was deleted. Existing calls to hidden historical row/pace labels were traced to the current suppression policy rather than treated as active unit claims.

Coverage dimensions: T/L/C PASS on all 1,816 rows; F PASS on 1,814, NOT_APPLICABLE only on two numeric samples; R PASS on 1,382 XML-backed entries and NOT_APPLICABLE on 434 code/data/composition rows, with reasons. **All V cells remain blank**: no new Dutch runtime/rendering or TalkBack check was performed. Missing screenshots do not block this content decision.

## Review method and evidence IDs

- `READ-Bxx`: independent reading of the exact current Dutch items owned by that batch, with the contextual decisions below. Ordinary Dutch grammar/naturalness is reviewer analysis; a source URL does not substitute for it.
- `CALLERS`: current Kotlin/XML/manifest references and relevant producers, captured in caller-inventory.json, coverage.tsv and input-hashes.json. Formatted arguments, quantities and consequences were traced. All formatted Dutch resource callers were inspected.
- `SOURCE`: source/composition reasoning, not executed Android output. Compact arithmetic cases in coverage are inputs/analysis, not literal screenshots.
- `RES`: final fresh XML/key/type/alias/placeholder comparison, resource compilation and the three targeted test cases described below.
- `SHARED`: current-hash reuse of shared factual calculations/table and glyph evidence documented in shared-fixes.md and the factual sections of en/review.md. The 18-file reconciliation is explicit; Dutch labels and meanings were read separately. No English visual coverage status was promoted into Dutch V.
- `SHARED-B05`: latest future-session-zone active-day/range repair in the English report and the matching current Insights producers. The earlier open snapshot in shared-fixes.md is superseded by that repair. The Dutch one/other forms and all three chart descriptions still agree with denominator-based plural selection.

## Concept and voice register

| Concept | Accepted current Dutch and contexts | Misleading alternative / decision | Evidence |
| --- | --- | --- | --- |
| Address | je/jouw; direct imperative controls | Three accidental u descriptions corrected. Unit u for hours is retained. | READ-B18; CALLERS |
| Craft | breien, haken; handwerken for mixed activity | Needle-only empty session state does not cover crochet. | CALLERS |
| Row / round | toer, rij, naald where used as a worked course; ronde for selected circular count | No mechanical replacement of every rij/toer/naald. Selected counter label is independent of craft. | S-KATIA; S-DROPS-GARTER; CALLERS |
| Repeat | herhaling; pattern section versus occurrence/interval | Herhaal remains valid command; numbered occurrence needs Herhaling. | S-CYC-ABBR; CALLERS |
| Stitch / stitch pattern | steek/steken; tricotsteek, ribbelsteek, gerstekorrelsteek | K must identify recht, not generic brei; seed placement is not unexplained offset. | S-KATIA; S-HANDWERKLES; S-DROPS-GARTER |
| Pattern | patroon for instructions, stitch pattern or PDF; web metadata separately described | No imported Finnish-only terminology rule. Metadata does not imply an offline PDF. | CALLERS |
| Gauge / swatch | stekenverhouding, stekenproef, proeflap as context permits | Density versus sample distinguished; not wholesale synonym normalization. | S-KATIA; CALLERS |
| Increase / decrease | meerderen/minderen; afname remains valid description | balk/staaf is not the between-stitch strand; generic value adjustment uses verlagen/verhogen. | S-BREICLUB-M1; S-KATIA-M1; S-CYC-ABBR |
| Direction / net effect | left/right lean, one versus double decrease | SSK slips separately; centered S2KP slips the pair together knitwise. | S-CYC-ABBR; S-BREICLUB-SSK; SHARED S06 |
| Slip / yarn position | recht/averecht afhalen; garen vóór/achter het werk | Front/back of current work is not a permanent right/wrong fabric side. | S-CYC-ABBR; S-CYC-SYMBOL |
| Loops / sides | voorste/achterste lus; goede/verkeerde kant | KFB/PFB use both legs of the same stitch. Generic surface wording was insufficient. | S-CYC-ABBR; S-CYC-SYMBOL |
| Cables | hold needle before/behind work; 2/2 and 3/3 crossing | achterover is not behind the work; retained CN/K tokens are defined notation. | S-CYC-SYMBOL; CALLERS |
| Cast on / finish | opzetten, afkanten | Distinct from app save/delete. Total edge stitches added after repeat rounding. | S-KATIA; SHARED; CALLERS |
| Yarn thickness / mass | eight canonical category names; g for mass | No invented universal ply mapping or uppercase G. | SHARED; CALLERS |
| Yarn packages | streng/strengen, bol where context fits | No blanket package-term rewrite. Decimal estimate and whole-package purchase mass differ. | CALLERS; SHARED S14 |
| Project yarn | gepland, gereserveerd, gebruikt, resterend, overschreden | Unknown is not zero; allocation is not mandatory; stash unaffected. | CALLERS |
| Needles | naald; mm diameter; regional US/UK/JP numbers | Remove false fixed US0–50 range; decimal system labels localize, leading zeros remain. | SHARED S03/S10/S15; CALLERS |
| Size / fit | body dimensions; borst/buste/taille/heupen; arm to underarm | Hand Lengte contextualized by Hand; no finished garment/ease claim. Tiener retained as broad youth category without invented numeric ages. | S-CYC-BODY; CALLERS |
| Care | wassen °C; bleaching/drying/ironing; chemisch reinigen(P/F) | P/F are professional process categories, not solvents named P/F. Product label governs. | S-CARE; SHARED S07/S08 |
| App actions | opslaan, koppelen, loskoppelen, verwijderen, herstellen | Delete/replacement scope and file ownership checked; backup is not sync. | CALLERS |
| Sessions / Insights | werksessie; tijd; neutral tellervoortgang; voltooiingen | Completion events can repeat; current labels cannot establish historical rows/pace. | CALLERS; SHARED-B05 |

Valid alternatives and preferences were deliberately retained: rij/toer/naald in their contexts; afname alongside mindering; ordinary accessible button brevity; the broad Tiener category; recognizable English standard yarn/abbreviation tokens; existing casing; decimal `1,00 strengen`; user-chosen custom labels. These are not a target for stylistic standardization.

## Source register

Fresh web access below was on **2026-10-19**, unless explicitly labeled reused. Relevant passages were read; search snippets were not treated as proof. Two pages of the same publisher are not independent corroboration. CYC provides the notation convention, not approval of Dutch phrasing.

| ID | Publisher / title / exact URL | Language and inspected claim | Limits / independence |
| --- | --- | --- | --- |
| S-KATIA | Katia, [Breipatronen lezen en begrijpen](https://www.katia.com/blog/nl/katia-breipatronen-lezen-en-begrijpen/) | Dutch; charts, density/swatch, stitch operations; sections 3–5. | Contextual terminology, not a mandated one-word translation. |
| S-KATIA-M1 | Katia / Helena’s Handarbeit, [Stripesweater](https://www.katia.com/blog/nl/stripesweater-helenas-handarbeit/) | Dutch original pattern instructions; M1R lifts between-stitch yarn back-to-front. | Same Katia publisher as previous source; independent of Breiclub. |
| S-BREICLUB-M1 | Susanne / Breiclub, [Meerderen met M1L en M1R](https://breiclub.nl/over-breien/meerderen-breiwerk-met-m1l-en-m1r/) | Dutch original tutorial; M1L front-to-back/twisted, M1R back-to-front. | Confirms technique and strand vocabulary; no automatic app algorithm proof. |
| S-BREICLUB-SSK | Babette Nijhof / Breiclub, [I-cord breien](https://breiclub.nl/over-breien/breien-voor-dummies-de-i-cord-breien/) | Dutch worked leaf example: separately slip, then knit together. | Corroborates SSK; knitwise orientation supported by CYC. |
| S-HANDWERKLES | Handwerkles, [Gerstekorrel](https://www.handwerkles.nl/index.php/19-breisteken/122-gerstekorrel) | Dutch original instruction; alternating knit/purl placement, odd-count example. | Independent of Katia/DROPS; app text states general placement rather than copying odd-count setup. |
| S-DROPS-GARTER | DROPS Design, [Ribbelsteek in de rondte](https://www.garnstudio.com/video.php?id=46&lang=nl) | Dutch written video instructions: all knit round, all purl round, repeat. | Same manufacturer’s other lesson lifts a strand with a hole; that is a different operation, not evidence for no-hole M1. |
| S-CYC-ABBR | Craft Yarn Council, [Knitting abbreviations](https://www.craftyarncouncil.com/standards/knitting-abbreviations) | English/US; K/P, KFB/PFB, SK2P, SSK, S2KP2, yarn position and notation definitions. | Pattern-specific definitions take priority. Technique-critical S2KP also corroborated by unchanged SHARED S06 (Purl Soho), not another CYC translation. |
| S-CYC-SYMBOL | CYC, [Knit chart symbols](https://www.craftyarncouncil.com/standards/knit-chart-symbols) | English/US; right-side convention, knit/purl inverse, unbarred V yarn behind, lifted/M1 and cable directions. | Glyph appearance uses pinned prior comparison plus current draw branches; no fresh Dutch screenshot claim. Same provenance as CYC abbreviations. |
| S-HARDYBARN | Hardybarn Designs, [Lifted increases](https://www.hardybarn.co.uk/liftedincreases/) | English original designer tutorial; lifted increases use a stitch below, paired left/right variants. | Independent of CYC; supports descriptive Dutch rather than inventing an allegedly universal term. |
| S-CARE | GINETEX clevercare, [Onderhoudssymbolen](https://clevercare.info/nl/care-symbols-index/) | Dutch; washing temperatures, bleaching/drying/ironing and professional P/F categories. | Product label governs; legacy A corroborated by reused CYC care table. No new numeric ironing limits inferred. |
| S-TAAL | Onze Taal, [Samenstelling](https://onzetaal.nl/taalloket/samenstelling) and [Tussen-n](https://onzetaal.nl/taalloket/tussen-n) | Dutch language reference; compound/spelling framework. | Reviewer checks actual spelling/grammar; language source does not verify craft facts. |
| S-CYC-BODY | CYC [baby](https://www.craftyarncouncil.com/standards/baby-size-chart), [child/youth](https://www.craftyarncouncil.com/standards/child-youth-sizes), [women](https://www.craftyarncouncil.com/standards/woman-size), [men](https://www.craftyarncouncil.com/standards/man-size), [head](https://www.craftyarncouncil.com/standards/head-circumference-chart), [hand](https://www.craftyarncouncil.com/standards/hand-size-charts) | English/US; exact table evidence reused from 2026-10-18; head page also reopened 2026-10-19. All current cell membership, Dutch categories and formatted values independently inspected. | Shared numeric evidence only; no inherited Dutch approval. CYC sources are one provenance. |
| SHARED-NEEDLES | CYC [Hooks/needles](https://www.craftyarncouncil.com/standards/hooks-and-needles); Clover [Takumi catalog](https://clover.co.jp/recipe/takumikikakus.pdf); Wool Zone [UK/US table](https://www.woolzone.co.uk/wp-content/uploads/2021/06/Needle-Conversion-Chart.pdf) | Reused 2026-10-18 evidence: respectively US, JP0–15 exact diameters, UK00/000. Source/code hash reconciliation recorded. | Different systems need their own sources. Known KnitPro9/10mm discrepancy was not propagated. |
| SHARED-OTHER | See en/review.md source register S06/S07/S08/S09/S13/S14/S22 and shared-fixes.md | Reused independent S2KP technique, care symbols, thickness versus ply, conditional spare-skein advice, lowercase units and current shared calculations. | Historical validation remains historical. Prose and source identity checked for Dutch applicability. |

Unavailable fetches (Purl Soho live lifted-increase page, ANW lookup, oversized/unavailable Etitex PDFs) were not used as read evidence. Available original Hardybarn, GINETEX and existing pinned specialist evidence resolved the relevant questions. No open dependency on those failed fetches remains.

## Batch completion

Exact ownership is `coverage.tsv` filtered by batch; every current primary variant appears once. Added code/context rows are counted separately. Findings are fully listed with original text, problem/consequence, minimum replacement and affected callers in [corrections.md](corrections.md), with machine-readable [corrections.json](corrections.json). Every applied finding is VERIFIED. Batch notes below record the independent reading rather than a blanket parity-test conclusion.

| Batch | Primary variants reviewed / owned | Additional rows | Corrected variants | Content / unresolved |
| --- | ---: | ---: | ---: | --- |
| B01 | 27/27 | 0 | 0 | COMPLETE; PASS / none |
| B02 | 104/104 | 0 | 0 | COMPLETE; PASS / none |
| B03 | 78/78 | 5 | 2 | COMPLETE; PASS / none |
| B04 | 41/41 | 1 | 2 | COMPLETE; PASS / none |
| B05 | 123/123 | 6 | 1 | COMPLETE; PASS / none |
| B06 | 60/60 | 28 | 6 | COMPLETE; PASS / none |
| B07 | 67/67 | 2 | 1 | COMPLETE; PASS / none |
| B08 | 75/75 | 0 | 0 | COMPLETE; PASS / none |
| B09 | 76/76 | 0 | 0 | COMPLETE; PASS / none |
| B10 | 68/68 | 0 | 0 | COMPLETE; PASS / none |
| B11 | 77/77 | 0 | 0 | COMPLETE; PASS / none |
| B12 | 31/31 | 0 | 0 | COMPLETE; PASS / none |
| B13 | 76/76 | 2 | 0 | COMPLETE; PASS / none |
| B14 | 30/30 | 2 | 1 | COMPLETE; PASS / none |
| B15 | 19/19 | 2 | 1 | COMPLETE; PASS / none |
| B16 | 12/12 | 2 | 2 | COMPLETE; PASS / none |
| B17 | 72/72 | 277 | 2 | COMPLETE; PASS / none |
| B18 | 157/157 | 77 | 31 | COMPLETE; PASS / none |
| B19 | 58/58 | 24 | 17 | COMPLETE; PASS / none |
| B20 | 35/35 | 0 | 1 | COMPLETE; PASS / none |
| B21 | 28/28 | 0 | 0 | COMPLETE; PASS / none |
| B22 | 66/66 | 0 | 3 | COMPLETE; PASS / none |
| B23 | 0/0 | 8 | 0 | COMPLETE; PASS / none |

### B01

Read shared controls, destinations and units as used in navigation/forms/dialogs. Save/Cancel/Delete/Open are distinct; shared measurement format preserves value before unit. No English title-case normalization. Inserted names remain user text.

### B02

Read creation, edit, folder filters, bulk completion/reopen/delete and their singular/plural warnings. Folder deletion preserves projects; project deletion removes owned content. Virtual filters are views, not real folders. Plural selection follows selected project count; names are not translated or declined.

### B03

Read ROWS/ROUNDS/REPEATS/CUSTOM separately from craft type, target positions versus amounts, steps, reset/undo, secondary and widget controls. Generic plus/minus must not name stitch shaping. Historical progress stays neutral after current label changes. See generated compositions for remaining/past-target and custom-name arguments.

### B04

Read initial/current/repeat positions, linked state, shaping and repeat-section behavior, reminder targets and occurrence labels. A numbered occurrence is a herhaling; interval is separate from occurrence number. Instructions preserve actual counter action versus a knitting operation.

### B05

Read Start/Stop/conflict/recovery/history and all Insights messages. Duration differs from counter progress; completed events can repeat for one project. Empty state covers both crafts. Active-days plural uses denominator daysInRange; latest shared future-date clamp is reconciled. Chart descriptions compose range, craft verb, days and duration. No historical row or pace claim inferred from editable labels.

### B06

Read stash status/quantity, color versus dye lot, fibre/thickness/package mass, card source/link and all 20 care labels paired with current glyph definitions. Eight canonical thickness categories are intentional standard names, not translated persisted IDs or ply counts. Added missing washing/Celsius and professional-cleaning context to standalone labels.

### B07

Read project note/save-to-stash and usage states including null versus zero, independent planned/allocated/used, negative remainder and conversion prerequisites. Units are based on confirmed positive metre/gram relationship; fractional skein grammar follows rounded displayed quantity. Usage does not deduct stash. Removed false reservation-only restriction.

### B08

Read local metadata, availability, web link versus readable PDF, browser sign-in/download/payment and duplicates/delete/unlink/attach. Ravelry is a website browser, not restored API/OAuth. External website content is excluded. Designer-length alias resolves to the existing 120-character limit text; errors describe current validation.

### B09

Read incoming PDF destination, document ownership/primary/order, camera/gallery photo-to-PDF, limits, permissions, cancellation and failures. App-owned copies and metadata are distinct; no OCR/sync claim. Counts refer to selected images/documents. Destructive replacement warnings preserve the destination and scope.

### B10

Read page and project-row distinctions, guides/bookmarks/markers, calibration, follow/pause and return-to-row actions. Bookmark is not annotation; manual movement pauses follow, explicit markers set mapping. Per-document ownership and active-document checks agree with wording.

### B11

Read every markup tool, selection, undo/redo, layer scope, export/progress/cancel/error and bound warning. Saved master is read-only in project viewer; project overlay editable. Marked PDF export preserves original, excludes guides/bookmarks, and does not silently write a new source document.

### B12

Read notes autosave/status, capture/import/gallery, sharing, photo-delete warnings and inserted project names. Local notes remain local; share exports selected content. Existing natural alternatives retained. User note/photo text is not app translation.

### B13

Read every measurement/gauge field, task, swatch example, validation and copy-output label. Length versus count, stitch versus row density and 10cm versus4in basis remain distinct. Canonical values are not display rounding. Dutch comma and dot input paths, task-specific unit overrides and result composition inspected.

### B14

Read increase/decrease mode/distribution, parser hints/examples and all rejection advice. M1 adds a stitch without consuming one; flat N-1 versus circular N bars and minimum2 respected. K/M1/K2tog notation is intentionally defined notation. Corrected only compound spelling; shared arithmetic evidence reused after hash match.

### B15

Read width/gauge/repeat/total-edge-stitches/results/alternatives. Exact count is rounded to closest repeat before total edges, tie lower. Density field now names stekenverhouding instead of malformed noun phrase. Metric10cm and imperial4in basis independently traced.

### B16

Read required length, skein length/mass, decimal estimate and whole-package output. 450m at200m/50g gives2.25 packages, purchase3 and150g; Dutch display uses2,25. Weight label must identify whole skeins. Spare skein recommendation is conditional, not universally mandatory.

### B17

Read every Dutch column/category/age/size label, all38 needle rows and46 body/hand/head rows. Reconciled152 needle cells and123 measurements (58 ranges) with shared factual evidence and current source hashes. Unit labels and decimal comma composition checked. Arm is underarm length; hand length is unambiguous in Hand category. Tiener is retained as broad youth category, not an invented fixed age interval. Unused old labels remain accounted for.

### B18

Read all77 token/meaning/description triples, including each separate plural/spelling/operation distinction. Je/jouw is the current address convention; three u intrusions corrected. English pattern tokens remain intentionally English. No claim of a separate crochet abbreviation dictionary. See concept register and per-text findings for independently checked technique corrections.

### B19

Read24 current glyph/name/description triples plus retained unused knitwise-slip resources. Compared current drawing branches with pinned shared visual/factual evidence and CYC convention. Dutch explanations reviewed independently; clarified knit operations, lifted increases, unbarred-V yarn position and right-side convention. Cable widths/directions and KFB/P2tog corrections already in shared source retained.

### B20

Read every setting, native language label, system default, theme, units, privacy/consent and app info message. Release analytics opt-in and allowlisted data agree with implementation; no cloud-sync claim. Unit preference is a default with Gauge overrides, now stated explicitly. Build/version values are generated, not prose.

### B21

Read backup/preview/export/restore and errors against selected archive validation and transaction code. Replacement is clearly warned; archive not encrypted; restore is not continuous sync. Counts/metadata dates are separate from filename date. No user backup or destructive restore executed.

### B22

Read trial start/expiry, billing states/restore, price formatting, free active-project rule and each gated action. Trial14days, one-time INAPP product and existing-content policy agree with current source; debug unlock is not purchase. Reminder wording made unit-neutral because labels can be rounds/repeats/custom.

### B23

Reconciled resource inventory, aliases, residual literals, formatted compositions, unused entries, shared input hashes and final diff. Every resource/variant has its own primary row; repeated identical uses share caller references. Runtime not required for a named unresolved language question. No native-speaker review or exhaustive rendering claim.

## Validation log and limits

- Final XML comparison: all 1,349 translatable keys present, compatible resource kinds and resolved aliases, all 1,380 nl variants accounted for, formatting argument signatures compatible with defaults. Exactly 70 variants / 69 keys differ from the entry Dutch snapshot; each difference is a recorded correction.
- Fresh Gradle run completed successfully in 4m 6s; processDebugResources executed. The selected test task was explicitly rerun, not reported from an UP-TO-DATE result. ReferenceResourceLocalizationTest: 2/2; CountPluralResourcesSourceTest: 1/1; zero failures/errors/skips. Raw XML receipts and validation-gradle.log retained here.
- Command: `./gradlew.bat :app:processDebugResources :app:testDebugUnitTest --rerun --tests '*ReferenceResourceLocalizationTest' --tests '*CountPluralResourcesSourceTest' '-Pknittools.isolatedTest=activepolicy' --console=plain --no-daemon`.
- Gradle wrapper 9.8.1; daemon toolchain configured to Adoptium17 by gradle/gradle-daemon-jvm.properties. The shell java executable reported Oracle21; it was not substituted for the configured daemon toolchain. No toolchain/dependency changed.
- `git diff --check -- app/src/main/res/values-nl` passed. Git emitted existing CRLF-to-LF warnings; no whitespace error or wholesale newline rewrite.
- `validation.json` records the comparison of every pre-existing non-nl input: no changed/deleted input outside scope. Existing staged and unstaged work was preserved.
- Shared reuse is source/factual evidence only: calculators, 152 needle cells, 123 size measurements, 24 chart symbols, 20 care symbols, neutral historical units and the latest active-day date clamp. The old shared/English runtime receipts were not rerun or called Dutch UI validation.
- No Android runtime, screenshot, native-speaker, 320dp, 200% font, combination or TalkBack checks ran. No concrete unresolved meaning/comprehension question required a targeted device check. No rendering backlog was created.
- No emulator/server/watcher was started. The one-shot Gradle invocation used --no-daemon and finished; temporary Python commands completed. Process closeout is recorded in validation.json.

## Findings, shared handoff and acceptance

**70 corrected text variants in 69 keys.** Reasons: incorrect/incomplete technique or chart descriptions; three address inconsistencies; misspelling/grammar and untranslated calques; care-process labels; scope/argument mismatches in counts, yarn usage, whole-skein weight, unit preference and mixed-craft/Pro wording. There was no target change count. Existing correct alternatives and all unrelated changes were retained.

Shared repair handoff: **none**. Previously repaired shared facts were reconciled against current inputs; no new shared defect or stale dependent content remains. The Dutch report does not alter another language’s acceptance.

Section16 reconciliation complete: current inventory/variants/alias; code-backed tables/tokens/symbols; material arguments and generated text; all B01–B23; confirmed findings; current shared changes; actual affected checks; final diff and preserved work; temporary-process closeout. All corrected Dutch text was re-read after application. No confirmed S1–S3 defect or specific unresolved language question remains.

**AUDIT_PASS; native review NOT_REQUESTED.** This is not a native craft-expert sign-off, exhaustive UI certification, or a guarantee against undiscovered errors. Missing screenshots alone are not a blocker under the revised policy. No work remains within this Dutch content task; stop after nl.
