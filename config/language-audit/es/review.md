# Spanish Language Audit (es)

**Current decision (2026-10-19): AUDIT_PASS; LA-es-B05-58 VERIFIED; native review NOT_REQUESTED.** See the verified closeout appended below. The original audit and OPEN decision that follow are retained as a historical record, superseded only for the month defect and current acceptance.

## Historical Spanish audit before the authorized month repair


**Decision: REVIEW_COMPLETE / OPEN.** All assigned B01–B23 content was reviewed. One confirmed shared plural-selection defect prevents AUDIT_PASS. Native-speaker review: **NOT_REQUESTED**.

## Scope and snapshot
- Date: 2026-10-19 (Europe/Helsinki). Workspace: `C:/Dev/KnitTools`.
- Boundary: Spanish resources and Spanish audit evidence only; stop after Spanish. No shared implementation/test/other-locale edits, commit, push, PR or cross-chat messages.
- Branch: `codex/audit-korjaukset-20261018`; HEAD: `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`.
- Entry state: pre-existing shared and locale changes preserved, including already-neutralized Spanish counter/guide copy and table labels. `starting-state.json` records the complete starting status and file hashes; `entry-texts.json` preserves every Spanish original. No earlier Spanish audit existed.
- Current inventory: **1,349 keys = 1,318 strings + 31 plurals; 1,411 variants = 1,318 + 93; no arrays** across four Spanish XML files. Current inventory was derived from Spanish files, not another locale.
- Coverage: **2,101 rows** = 1,411 resource variants + 17 distinct additional contexts + 275 numeric cells + 85 invariant notation/category records + 44 symbols + 1 debug fixture corpus + 268 generated/composed cases. All were content-reviewed; V is blank throughout.
- `coverage.tsv` is UTF-8 TSV. Literal newlines/tabs in text are represented by `\n`/`\t`; existing Android escapes remain visible. Stable item IDs identify variant and materially distinct context. Repeated identical callers share rows; `caller-inventory.json` stores their source snippets.
- `input-hashes.json` records 234 current inputs, including Spanish/default resources, relevant callers/producers and resource-test inputs. `shared-evidence-reconciliation.json` records19/19 matching selected shared-input hashes. Hashing another locale as a parity-test input is not a content audit of that locale.
- **57 keys / 59 resource texts changed** in `strings.xml`, `reference_strings.xml` and `pattern_annotations.xml`; `pattern_reading_assistance.xml` was read and left unchanged from task entry.
- Scope exclusions: user/imported names, PDFs, URLs, third-party Ravelry pages and Play-formatted prices are not app-owned translations. Debug fixture text is recorded separately as synthetic stored data, not release interface copy. Retained unused resources were read without deleting them.

## Language decisions and glossary
Generic `es` alone establishes no regional mandate. Existing second-person singular wording (`tus`, `puedes`, `comprueba`) and frequent `ganchillo`, `ovillo`, `patrón`, `punto bobo`, `muestra` form a broadly understandable set with vocabulary common in Spain. Existing valid `ronda`, `puntada`, `hilera/fila`, `madeja` and infinitive/formal technical instructions were not mechanically normalized. This is preservation of current usage, not a new Spain-only policy.

| Concept | Retained/corrected expressions | Context and boundaries | Evidence | Status |
| --- | --- | --- | --- | --- |
| Knit / purl | derecho / revés | Bare tejer must not obscure K versus P in technique instructions. | ES-GLOSS; CYC-ABBR | PASS |
| Row / round | vuelta; ronda | Current selected counter labels preserved; fila/hilera valid in flat technique descriptions. Historical arbitrary counter values stay neutral. | MainCounterDisplayText; READ-B03 | PASS |
| Repeats | repetición; repetir | Noun for occurrence, verb for instruction. | CounterValueFormatter | PASS |
| Gauge / swatch | muestra; tensión/calibre in glossary | Density and physical swatch distinguished by fields and explanation; stitch and row axes independent. | ES-GAUGE; GaugePresentation | PASS |
| Yarn / package | hilo, ovillo, madeja | Existing package terms retained; project usage is not global stash. | READ-B06/B07/B16 | PASS |
| Pattern | patrón; patrón web | Garment instructions/metadata versus stitch-pattern context; names are user data. | READ-B08/B09/B18 | PASS |
| Fabric side / yarn position | LD/LR; delante/detrás de la labor | Position relative to work is not equivalent to right/wrong fabric side. | ES-GLOSS; CYC-ABBR | PASS |
| Technique notation | K/P/M1/K2tog/SSK etc. | Invariant English knitting notation, Spanish explanation; pattern-specific definitions take precedence. | ES-GLOSS; CYC-ABBR | PASS |
| Thickness categories | Lace, Fingering, Sport, DK, Worsted, Aran, Bulky, Super Bulky | Persisted/imported standard category identifiers; no unsupported ply mapping. | StandardYarnWeight | PASS |
| Reference systems | mm / US / UK / JP | Numeric/country identifiers stay invariant; equivalences approximate across systems. | SHARED-NEEDLES-* | PASS |

## Sources
Fresh source reads below were performed on 2026-10-19. Entries prefixed SHARED are explicitly reused 2026-10-18–19 evidence in `../en/review.md` and `../shared-fixes.md`, with current code/data hashes checked. They were not all fetched or rendered again. Spanish text was independently read even where numerical/glyph facts were reused.

| ID | Title / publisher | URL | Language / region | Passage and limits |
| --- | --- | --- | --- | --- |
| ES-GLOSS | DROPS multilingual knitting glossary | https://www.garnstudio.com/glossary.php?lang=es&langf=es&langt=en | Spanish/international publisher | Derecho/revés, LD/LR, gauge, yarn, wrap and loop terminology. Usage evidence, not authority for every numerical entry; unrelated pound-conversion entry not adopted. |
| ES-KATIA-INCREASE | Katia baby romper technique instructions | https://www.katia.com/blog/es/pelele-de-bebe-a-dos-agujas-en-una-pieza-patron-gratis/ | Spanish/Spain publisher | Increase subsection supports front-to-back/back-loop method and opposite method. Decrease heading/direction inconsistency was not adopted; decreases checked with DROPS/CYC. |
| ES-CDD | DROPS centered double decrease, video1763 written explanation | https://www.garnstudio.com/video.php?id=1763&lang=es | Spanish | Slip2 together knitwise, knit1 and pass slipped stitches; net decrease2. Written steps read; no claim of video playback. |
| ES-SKP | DROPS slip/knit/pass decrease, video208 explanation | https://www.garnstudio.com/video.php?id=208&lang=es | Spanish | Left-leaning SKP operation; same net result is not a claim that SSK has identical steps. |
| ES-STOCKINETTE | DROPS stockinette, video45 explanation | https://www.garnstudio.com/video.php?id=45&lang=es | Spanish | Flat knit/purl alternation and fabric-side distinction; cross-checked against the glossary and Katia instructions. |
| ES-GAUGE | Katia advice for reading patterns, section4 | https://www.katia.com/blog/es/consejos-para-seguir-patrones-de-revistas-katia-tejer-sin-dudas/ | Spanish/Spain publisher | Muestra del punto and independent stitch/row counts over10cm; calculator arithmetic checked independently. |
| ES-CARE | GINETEX Spain care descriptions | https://www.ginetex.es/etiquetado_productos_textiles/simbolos_de_cuidado/?symbol-name=professional-care | Spanish | Washing, drying, ironing and professional-cleaning terminology. Initial text read; later page reopen failed. Shared authoritative care PDF below supplies the unchanged symbol/maximum/process facts. |
| CYC-ABBR | Craft Yarn Council knitting abbreviations | https://www.craftyarncouncil.com/standards/knitting-abbreviations | English/US notation | Meaning of invariant English tokens; not evidence that English wording itself is acceptable Spanish. |
| CYC-SYMBOLS | Craft Yarn Council knitting chart symbols | https://www.craftyarncouncil.com/standards/knit-chart-symbols | English/US convention | Right-side appearance, purl/knit, slip yarn position, decrease and cable identity. Convention is not universal; pattern legend takes priority. |
| CYC-WPI | CYC: How to Measure Wraps Per Inch | https://www.craftyarncouncil.com/standards/how-measure-wraps-inch-wpi | English/US | Count adjacent wraps across one inch, without stretching. Only definition used, not numerical category boundaries. |
| ES-WPI | Las Varetas Crochet: Sweater Primera Cita, p3 | https://lasvaretascrochet.com/wp-content/uploads/2021/12/Sweater-Primera-Cita-Por-LasVaretasCrochet-1.pdf | Spanish/Argentina | Designer explains WPI as vueltas por pulgada. Terminology evidence only; pattern instructions and size tables not adopted. |
| ANDROID-PLURALS | Android string resource documentation | https://developer.android.com/guide/topics/resources/string-resource | English/Android | Quantity selection and formatting are separate; quantity must be passed again as formatting argument. |
| AOSP | ResourcesImpl.getQuantityText | https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/content/res/ResourcesImpl.java | Java/Android source | PluralRules selects the requested quantity; resource lookup falls back to other. This is source verification, not this devices runtime execution. |
| CLDR | Unicode CLDR48 cardinal rules | https://www.unicode.org/cldr/charts/48/supplemental/language_plural_rules.html | Locale data/es | one for n=1, other for ordinary non-one values, many for round millions on implementations using these rules. All31 current resources explicitly declare one/many/other. |
| ES-NUMBERS | RAE Diccionario panhispánico: cardinales | https://www.rae.es/dpd/cardinales | Spanish/pan-Hispanic | Cardinal usage; millón as written noun takes de. Not treated as automatic proof that every full-digit UI value needs an inserted de. |
| ES-DIGITS | FundéuRAE: miles y millones | https://www.fundeu.es/recomendacion/miles-y-millones-claves-de-escritura/ | Spanish | Full-digit and word representations differ. No unrelated global numeric-formatting change made. |
| ES-MILLIONS | GNOME Spanish translation coordinator discussion | https://discourse.gnome.org/t/millions-plural-form-in-french-spanish-portuguese-italian/12867 | English discussion by Spanish translation maintainer | Contextual corroboration for existing full-digit noun plural practice, not normative grammar or Android API authority. Existing numeric many wording retained; no speculative de insertion. |
| SHARED-CARE | GINETEX care symbol table, reused English S07 | https://www.ginetex.es/wp-content/uploads/2017/07/GINETEX-Table-of-care-symbols-white-Web.pdf | English/international | Previously verified care glyphs/maximum Celsius/process classes. Legacy A retained; no new ironing-temperature numerical advice inferred. |
| SHARED-NEEDLES-US | CYC hooks and needles, reused English S03 | https://www.craftyarncouncil.com/standards/hooks-and-needles | English/US | All current US values; manufacturer variation. Does not establish UK/JP equivalence. |
| SHARED-NEEDLES-JP | Clover Takumi specification, reused English S10 | https://clover.co.jp/recipe/takumikikakus.pdf | Japanese/JP | Page1 JP0–15 exact2.1–6.6mm in0.3mm steps; numeric fact, not Spanish language evidence. |
| SHARED-NEEDLES-UK | Wool Zone conversion chart, reused English S15 | https://www.woolzone.co.uk/wp-content/uploads/2021/06/Needle-Conversion-Chart.pdf | English/UK | UK cells including9mm=00 and10mm=000. Whole152-cell current table read; original comparisons reused only after hash match. |
| SHARED-SIZES | CYC child/youth sizes and five sibling tables, reused English S16–S21 | https://www.craftyarncouncil.com/standards/child-youth-sizes | English/US | 123 current measure cells in46 rows, including58 ranges. Complete six source URLs retained in en/review.md Sources. Spanish headings independently read; child arm length ends at underarm. |

### ANDROID-PLURALS and composition
All31 plural resources have one/many/other. Source selection was traced through `pluralStringResource` or `getQuantityString` to AOSP/ICU; no French or English singular assumptions were imported. Representative cardinal values are0→other,1→one,2→other; modern Spanish CLDR also has many for exact million multiples. Older Android ICU may use other for those values, which remains plural here. `YarnUsagePresentation.skeinPluralQuantity` parses the **formatted** locale number and maps ICU categories to representative integer quantities; 0,5/1,5 use plural,1 uses singular. Units h/min/m/g/yd are symbols, not inflected nouns.
The many variants were all read, including business-impossible trial or image counts. The app formats full digits, not the word millón. Existing plural noun forms were retained instead of inserting de everywhere from a CLDR minimal-pair example; RAE noun guidance and the Spanish translation-maintainer discussion were considered separately from the Android category algorithm. This is not a claim that Android versions have identical ICU data.
All244 formatted variants have controlled source-composition rows with explicit arguments. These are text substitutions reviewed against argument roles, **not Android runtime observations**; dates/prices in examples are illustrative strings.24 additional cases cover calculator arithmetic, decimal input, units, cyclic counters and bounded Insights dates. Existing escaped quotes/newlines, percent escapes and all placeholder signatures were preserved.

### SHARED-FACTS reuse
The38 needle rows (152 cells),46 sizing rows (123 measures,58 ranges),24 chart symbols and20 care glyphs use the unchanged data/drawing inputs recorded in `shared-evidence-reconciliation.json`. Their existing individual source comparisons and glyph assessments were reused. All current data and Spanish headings/descriptions were read in this task; no English wording or PASS automatically approved Spanish.
Current CastOn/IncreaseDecrease/YarnEstimator sources match the shared corrected versions. Independent arithmetic cases are recorded in coverage. The latest B05 date fix also matches: `InsightsSessionAccumulator.add` excludes dates after display today and `InsightsViewModel` bounds the first displayed session date. The shared107-test result is historical reused producer evidence, not a test run here. Spanish active1/1,2/2 and0/1 compositions and knit/crochet/mixed chart phrases were separately reviewed. Previous English instrumentation failures or unperformed Spanish screenshots are not relabeled as passed.

## Batch tracker
| Batch | Resource variants | Total rows reviewed | Progress | Verdict | Evidence / open finding |
| --- | ---: | ---: | --- | --- | --- |
| B01 | 27 | 30 | COMPLETE | PASS | READ-B01; coverage.tsv |
| B02 | 110 | 149 | COMPLETE | PASS | READ-B02; coverage.tsv |
| B03 | 81 | 128 | COMPLETE | PASS | READ-B03; coverage.tsv |
| B04 | 42 | 49 | COMPLETE | PASS | READ-B04; coverage.tsv |
| B05 | 131 | 181 | COMPLETE | OPEN | READ-B05; coverage.tsv; LA-es-B05-58 |
| B06 | 62 | 101 | COMPLETE | PASS | READ-B06; coverage.tsv |
| B07 | 68 | 86 | COMPLETE | PASS | READ-B07; coverage.tsv |
| B08 | 76 | 86 | COMPLETE | PASS | READ-B08; coverage.tsv |
| B09 | 78 | 95 | COMPLETE | PASS | READ-B09; coverage.tsv |
| B10 | 68 | 80 | COMPLETE | PASS | READ-B10; coverage.tsv |
| B11 | 77 | 82 | COMPLETE | PASS | READ-B11; coverage.tsv |
| B12 | 32 | 39 | COMPLETE | PASS | READ-B12; coverage.tsv |
| B13 | 78 | 96 | COMPLETE | PASS | READ-B13; coverage.tsv |
| B14 | 30 | 39 | COMPLETE | PASS | READ-B14; coverage.tsv |
| B15 | 20 | 29 | COMPLETE | PASS | READ-B15; coverage.tsv |
| B16 | 12 | 16 | COMPLETE | PASS | READ-B16; coverage.tsv |
| B17 | 72 | 349 | COMPLETE | PASS | READ-B17; coverage.tsv |
| B18 | 157 | 234 | COMPLETE | PASS | READ-B18; coverage.tsv |
| B19 | 58 | 82 | COMPLETE | PASS | READ-B19; coverage.tsv |
| B20 | 35 | 36 | COMPLETE | PASS | READ-B20; coverage.tsv |
| B21 | 28 | 34 | COMPLETE | PASS | READ-B21; coverage.tsv |
| B22 | 69 | 79 | COMPLETE | PASS | READ-B22; coverage.tsv |
| B23 | 0 | 1 | COMPLETE | OPEN | READ-B23; coverage.tsv; LA-es-B05-58 |

### Contextual reading notes

**READ-B01:** Navigation/action labels read in their tab, sheet and dialog contexts; brand and personal names are invariant. No noun-agreement dependency introduced.

**READ-B02:** Project creation/completion/deletion, virtual folders and selection read with callers. Names are opaque user data; folder deletion preserves projects and selection counts use quantity forms.

**READ-B03:** Selected ROWS/ROUNDS/REPEATS/CUSTOM labels and steps reviewed separately. Spanish vuelta for rows and ronda for circular rounds retained; history and arbitrary counter changes are neutral.

**READ-B04:** Explicit session Start/Stop, recovery add/discard/edit and stored duration/progress wording read against active-session state. No implicit tracking or historical row-unit claim.

**READ-B05:** Insights filtered range, durations, completions and stored-zone dates reviewed. Active-day plural selects denominator; historical counter progress is neutral. Monthly relative-time defect remains in its own row.

**READ-B06:** Yarn metadata, stash package counts, thickness and all 20 care labels reviewed. Canonical thickness names retained; removed ply hints not resurrected. Care uses actual icon/process meaning.

**READ-B07:** Project yarn note and planned/allocated/used amounts reviewed separately; unknown is not zero; overage is absolute allocated-minus-used deficit and does not change stash.

**READ-B08:** Saved metadata, web links, library deletion/attachment and Ravelry browser copy reviewed with actual actions. No removed API/OAuth flow promised. Deleting master annotations does not delete project PDF/overlay.

**READ-B09:** PDF document relations/import/image ordering and limitations read with importer/picker callers. Photo-to-PDF and multiple project documents distinguished from metadata.

**READ-B10:** PDF page controls, counter-position calibration/follow, bookmarks and reading guides reviewed. PDF page numbers differ from counter values; jumping pauses follow; bookmark/guide is not exported markup.

**READ-B11:** Markup tools/layers, current-page clear, export and error copy reviewed. Master is read-only in project; editable overlay is project-owned; clear removes annotations rather than a PDF page.

**READ-B12:** Progress photo/delete/share, session history and note editor text reviewed. Counter location is neutral, and user note/photo metadata remains user content.

**READ-B13:** Width/height, stitch/row axes, canonical measurements, selectable units, exact/rounded counts and copy output reviewed against GaugePresentation and arithmetic. Decimal comma preserved.

**READ-B14:** K/M1/K2tog notation is English knitting convention; net stitch change and consumed stitches reviewed. Flat/circular bars and the regex-supported English paste examples remain distinct.

**READ-B15:** Body width before repeat rounding and total edge stitches reviewed. Result includes edges; actual width can differ from requested body width; 10 cm and 4 in bases are not identical.

**READ-B16:** Required length, estimated fractional packages, whole-package purchase weight and conditional spare package advice reviewed against YarnEstimator.

**READ-B17:** Every current needle/size-table cell read with headings; hash-matched shared source comparison reused. Metric diameter, approximate cross-system equivalents, body measurements and underarm endpoint distinguished.

**READ-B18:** Every token/meaning/description read together as English knitting notation with Spanish explanation. Derecho/revés, slip orientation, yarn position, increases, decreases, cables and fabrics checked separately.

**READ-B19:** Every symbol/name/description read against the right-side CYC convention. Shared unchanged drawing evidence reused for glyph identity only; Spanish label meaning judged independently.

**READ-B20:** Language names, theme, immediate locale application, optional analytics and units read against settings/preferences/analytics sources. No regional migration or new privacy promise.

**READ-B21:** Manual ZIP export/preview/replace and typed failure copy read against BackupRepository. Restore replaces local content and is not continuous sync; no destructive restore executed.

**READ-B22:** One-time INAPP purchase, explicit 14-day trial, free active-project limit and retained existing content checked against Pro/Trial/callers. Debug override is not a purchased entitlement.

**READ-B23:** Residual invariant/debug text and full key/variant reconciliation; one explicit shared handoff; no supplementary rendering campaign.

## Findings
All57 applied corrections are enumerated below. The machine-readable `corrections.json` retains the same exact originals, replacements, category, severity and rationale; no stylistic mass rewrite was applied. Unless explicitly stated otherwise, variant is default and all grouped callers in coverage are affected. VERIFIED means final Spanish reread, source/claim check, placeholder verification, Android resource processing and fresh targeted resource tests; it does not mean rendered-device or native-speaker verification. No shared-code or other-locale change was made for these corrections.

### LA-es-B01-01: n_selected
- Classification: CONFIRMED_DEFECT; category: LANGUAGE; severity: S3; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#n_selected`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: %d seleccionados
- Specific problem: Fixed masculine plural also used for one item and feminine photos/cards.
- User-visible consequence: Selection count must agree for all selected item types.
- Final minimal correction: Selección: %d
- Evidence and necessity: caller-inventory.json#n_selected. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B04-02: reminder_repeat_occurrence_format
- Classification: CONFIRMED_DEFECT; category: LANGUAGE; severity: S3; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#reminder_repeat_occurrence_format`; variants: one, many, other. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Repetir #%1$d, intervalo %2$d
- Specific problem: Repetir is an infinitive where this numbered occurrence needs a noun.
- User-visible consequence: Distinguishes occurrence from an action.
- Final minimal correction: Repetición #%1$d, intervalo %2$d
- Evidence and necessity: CounterValueFormatter; MultiCounterComponents. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B06-03: care_wash_30
- Classification: CONFIRMED_DEFECT; category: CONTEXT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#care_wash_30`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: 30°
- Specific problem: Bare degree label omits the care action, temperature scale and maximum meaning.
- User-visible consequence: The same text describes the care icon for accessibility.
- Final minimal correction: Lavar a un máximo de 30 °C
- Evidence and necessity: ES-CARE; CareSymbol. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B06-04: care_wash_40
- Classification: CONFIRMED_DEFECT; category: CONTEXT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#care_wash_40`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: 40°
- Specific problem: Bare degree label omits the care action, temperature scale and maximum meaning.
- User-visible consequence: The same text describes the care icon for accessibility.
- Final minimal correction: Lavar a un máximo de 40 °C
- Evidence and necessity: ES-CARE; CareSymbol. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B06-05: care_wash_60
- Classification: CONFIRMED_DEFECT; category: CONTEXT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#care_wash_60`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: 60°
- Specific problem: Bare degree label omits the care action, temperature scale and maximum meaning.
- User-visible consequence: The same text describes the care icon for accessibility.
- Final minimal correction: Lavar a un máximo de 60 °C
- Evidence and necessity: ES-CARE; CareSymbol. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B06-06: care_dryclean_p
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#care_dryclean_p`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Disolvente P
- Specific problem: P/F identify professional cleaning processes, not solvents named P/F.
- User-visible consequence: Avoids identifying a nonexistent solvent.
- Final minimal correction: Limpieza en seco (P)
- Evidence and necessity: ES-CARE; CareSymbol. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B06-07: care_dryclean_f
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#care_dryclean_f`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Disolvente F
- Specific problem: P/F identify professional cleaning processes, not solvents named P/F.
- User-visible consequence: Avoids identifying a nonexistent solvent.
- Final minimal correction: Limpieza en seco (F)
- Evidence and necessity: ES-CARE; CareSymbol. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B07-08: yarn_usage_scope
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#yarn_usage_scope`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Registra solo el hilo reservado para este proyecto. Las cantidades en Mi hilo no cambian.
- Specific problem: Solo el hilo reservado wrongly limits the editor to allocated yarn.
- User-visible consequence: Planned, allocated and used amounts are independently editable.
- Final minimal correction: Registra el uso de hilo de este proyecto. Las cantidades en Mi hilo no cambian.
- Evidence and necessity: ProjectYarnUsageRepository; YarnUsagePresentation. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B10-09: pattern_calibration_save_first
- Classification: CONFIRMED_DEFECT; category: LANGUAGE; severity: S3; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#pattern_calibration_save_first`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Guardar primera
- Specific problem: Primera disagrees with the field Primer valor.
- User-visible consequence: Keeps the save action tied to the counter value.
- Final minimal correction: Guardar primer valor
- Evidence and necessity: PatternViewerScreen. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B10-10: pattern_calibration_save_last
- Classification: CONFIRMED_DEFECT; category: LANGUAGE; severity: S3; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#pattern_calibration_save_last`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Guardar última
- Specific problem: Última disagrees with the field Último valor.
- User-visible consequence: Keeps the save action tied to the counter value.
- Final minimal correction: Guardar último valor
- Evidence and necessity: PatternViewerScreen. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B11-11: pattern_annotation_clear_page
- Classification: CONFIRMED_DEFECT; category: CONTEXT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/pattern_annotations.xml#pattern_annotation_clear_page`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Borrar página
- Specific problem: Borrar página suggests deleting the PDF page.
- User-visible consequence: The action clears the editable annotation layer on the page and preserves the PDF.
- Final minimal correction: Borrar anotaciones de la página
- Evidence and necessity: PatternAnnotationToolbar; PatternViewerScreen; PatternAnnotationCommand. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B16-12: total_weight
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#total_weight`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Peso total: %s g
- Specific problem: Total weight is the mass of purchased whole packages, not exact required yarn mass.
- User-visible consequence: 450 m at 200 m/50 g gives 3 whole packages and 150 g, not 112.5 g.
- Final minimal correction: Peso de los ovillos enteros: %s g
- Evidence and necessity: YarnEstimator; shared B16. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B16-13: estimated_skeins
- Classification: CONFIRMED_DEFECT; category: LANGUAGE; severity: S3; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#estimated_skeins`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Estimado: aprox. %s ovillos
- Specific problem: Fixed post-number plural also receives 1,00.
- User-visible consequence: Label-before-value works for whole and fractional amounts without incorrect agreement.
- Final minimal correction: Ovillos estimados: aprox. %s
- Evidence and necessity: YarnScreen.formatSkeinsEstimateForDisplay. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B16-14: extra_skein_note
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#extra_skein_note`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Compra siempre un ovillo extra; los lotes de tinte pueden variar
- Specific problem: Siempre makes an unsupported universal purchase recommendation.
- User-visible consequence: Additional yarn is conditional rather than mandatory for every project.
- Final minimal correction: Considera comprar un ovillo extra; los lotes de tinte pueden variar
- Evidence and necessity: YarnEstimator; shared B16. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B17-15: tip_needle_sizes_desc
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#tip_needle_sizes_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: El sistema métrico (mm) es el estándar universal. Las tallas US usan un sistema numerado (0–50). Las tallas UK/Canadá usan un sistema numerado diferente. Las tallas japonesas tienen su propia numeración. Comprueba siempre la medida en mm para asegurarte.
- Specific problem: US range 0–50 excludes displayed 000 and 70 sizes.
- User-visible consequence: The guide must not contradict its own conversion table.
- Final minimal correction: El sistema métrico (mm) es el estándar universal. Las tallas US usan un sistema numerado. Las tallas UK/Canadá usan un sistema numerado diferente. Las tallas japonesas tienen su propia numeración. Comprueba siempre la medida en mm para asegurarte.
- Evidence and necessity: NeedleSizeData; shared B17. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B17-16: size_col_arm
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#size_col_arm`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: brazo
- Specific problem: Brazo suggests full arm length but the child table measures underarm length.
- User-visible consequence: Avoids using the wrong endpoints for fit.
- Final minimal correction: Largo hasta la sisa
- Evidence and necessity: SizeChartData; shared CYC children sizing. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B20-17: tip_imperial_units_desc
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#tip_imperial_units_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Cuando está activado, todas las calculadoras usan pulgadas en lugar de centímetros y yardas en lugar de metros. La muestra se mide por 4 pulgadas en lugar de por 10 cm.
- Specific problem: Todas ... usan implies forced units; Measurements and Gauge allows independent units and gauge basis.
- User-visible consequence: Explains the preference as initial values while preserving in-screen choices.
- Final minimal correction: Cuando está activado, las calculadoras parten de pulgadas en lugar de centímetros y yardas en lugar de metros, con la muestra por 4 pulgadas en lugar de por 10 cm. En Medidas y muestra puedes elegir otras unidades.
- Evidence and necessity: GaugeViewModel initialization and GaugeAction.Basis/FromUnit/ToUnit. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B22-18: pro_prompt_reminders_title
- Classification: CONFIRMED_DEFECT; category: CONTEXT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#pro_prompt_reminders_title`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Añade un recordatorio de fila con Pro
- Specific problem: Row-only reminder wording contradicts reminders driven by any selected counter unit.
- User-visible consequence: Covers rounds, repeats and custom counters.
- Final minimal correction: Añade un recordatorio del contador con Pro
- Evidence and necessity: MainCounterDisplayText; RowReminder; shared B03. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B22-19: pro_prompt_reminders_body
- Classification: CONFIRMED_DEFECT; category: CONTEXT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#pro_prompt_reminders_body`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Crear un recordatorio de fila nuevo requiere Pro. Tus recordatorios actuales seguirán funcionando y se podrán editar.
- Specific problem: Row-only reminder wording contradicts reminders driven by any selected counter unit.
- User-visible consequence: Covers rounds, repeats and custom counters.
- Final minimal correction: Crear un recordatorio del contador nuevo requiere Pro. Tus recordatorios actuales seguirán funcionando y se podrán editar.
- Evidence and necessity: MainCounterDisplayText; RowReminder; shared B03. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B22-20: pro_group_workflow_body
- Classification: CONFIRMED_DEFECT; category: CONTEXT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#pro_group_workflow_body`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Usa el contador compacto de repeticiones, contadores adicionales, de repetición, de forma y de sección, recordatorios de fila, fotos del patrón como PDF y el widget de la pantalla de inicio.
- Specific problem: Row-only reminder wording contradicts reminders driven by any selected counter unit.
- User-visible consequence: Covers rounds, repeats and custom counters.
- Final minimal correction: Usa el contador compacto de repeticiones, contadores adicionales, de repetición, de forma y de sección, recordatorios del contador, fotos del patrón como PDF y el widget de la pantalla de inicio.
- Evidence and necessity: MainCounterDisplayText; RowReminder; shared B03. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B03-21: widget_name
- Classification: CONFIRMED_DEFECT; category: CONTEXT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#widget_name`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Contador de vueltas
- Specific problem: Widget supports rounds, repeats and custom labels as well as rows.
- User-visible consequence: Widget name should describe its supported counter.
- Final minimal correction: Contador de proyectos
- Evidence and necessity: widget KnitCounterWidget; MainCounterDisplayText. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B03-22: widget_description
- Classification: CONFIRMED_DEFECT; category: CONTEXT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#widget_description`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Widget de contador de vueltas para el proyecto activo
- Specific problem: Row-only widget description omits other supported counter units.
- User-visible consequence: Matches the selected project counter.
- Final minimal correction: Widget de contador para el proyecto activo
- Evidence and necessity: widget KnitCounterWidget. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-23: desc_abbreviations
- Classification: CONFIRMED_DEFECT; category: CONTEXT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#desc_abbreviations`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Referencia de abreviaturas comunes de tejido
- Specific problem: Tokens are English notation although the current title does not identify their language.
- User-visible consequence: Users should not interpret K/P/CO as Spanish abbreviations.
- Final minimal correction: Referencia de abreviaturas inglesas de tejido
- Evidence and necessity: AbbreviationData; ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-24: abbr_k_meaning
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_k_meaning`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: tejer
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: derecho
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-25: abbr_k2tog_meaning
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_k2tog_meaning`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Tejer 2 juntos
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Tejer 2 juntos del derecho
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-26: abbr_k2tog_desc
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_k2tog_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Disminución inclinada hacia la derecha: tejer dos puntos como uno solo
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Disminución inclinada hacia la derecha: tejer dos puntos juntos del derecho como uno solo
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-27: abbr_ssk_meaning
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_ssk_meaning`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Deslizar, deslizar, tejer
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Deslizar, deslizar, tejer del derecho
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-28: abbr_ssk_desc
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S1; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_ssk_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Disminución inclinada hacia la izquierda: deslizar 2 puntos como de derecho, tejerlos juntos a través de los bucles posteriores
- Specific problem: The two slips must be individual, and the final operation is knit.
- User-visible consequence: Slipping both together changes stitch orientation and result.
- Final minimal correction: Disminución inclinada hacia la izquierda: deslizar 2 puntos uno a uno como de derecho y tejerlos juntos del derecho a través de los bucles posteriores
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-29: abbr_skp_meaning
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_skp_meaning`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Deslizar, tejer, pasar el punto deslizado
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Deslizar, tejer del derecho, pasar el punto deslizado
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-30: abbr_k3tog_meaning
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_k3tog_meaning`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Tejer 3 juntos
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Tejer 3 juntos del derecho
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-31: abbr_s2kp_meaning
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S1; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_s2kp_meaning`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Deslizar 2, tejer 1 derecho, pasar los puntos deslizados por encima
- Specific problem: Missing together/knitwise slip condition for the centered decrease.
- User-visible consequence: Individual slips can produce a different decrease.
- Final minimal correction: Deslizar 2 juntos como de derecho, tejer 1 derecho, pasar los puntos deslizados por encima
- Evidence and necessity: ES-CDD; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-32: abbr_m1_desc
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S1; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_m1_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Levantar la barra entre los puntos y tejer a través del bucle posterior para aumentar.
- Specific problem: The stated back-loop method requires a particular direction when picking up the strand.
- User-visible consequence: A different pickup direction can leave an unintended eyelet.
- Final minimal correction: Levantar la barra entre los puntos de delante hacia atrás con la aguja izquierda y tejerla del derecho por el bucle posterior para aumentar.
- Evidence and necessity: ES-KATIA-INCREASE; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-33: abbr_kfb_meaning
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_kfb_meaning`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Tejer por delante y por detrás
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Tejer del derecho por delante y por detrás
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-34: abbr_kfb_desc
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_kfb_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Tejer por delante y por detrás del mismo punto para aumentar.
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Tejer del derecho por delante y por detrás del mismo punto para aumentar.
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-35: abbr_sl1k_desc
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_sl1k_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Deslizar un punto como si fuera a tejer
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Deslizar un punto como si fuera a tejer del derecho
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-36: abbr_ktbl_meaning
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_ktbl_meaning`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Tejer a través del bucle trasero
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Tejer del derecho a través del bucle trasero
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-37: abbr_bo_desc
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_bo_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Puntadas seguras para evitar que se deshagan.
- Specific problem: Puntadas seguras is a literal mistranslation of the closing action.
- User-visible consequence: Explains binding off rather than describing safe stitches.
- Final minimal correction: Cerrar los puntos para evitar que se deshagan.
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-38: abbr_wyif_desc
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S1; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_wyif_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Sostenga el hilo hacia el frente (lado revés) del trabajo.
- Specific problem: Front yarn position is incorrectly equated with wrong side of fabric.
- User-visible consequence: The yarn stays on the working front irrespective of fabric side.
- Final minimal correction: Mantener el hilo delante de la labor.
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-39: abbr_wyib_meaning
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_wyib_meaning`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Con hilo en la espalda
- Specific problem: Espalda describes an anatomical back rather than yarn position.
- User-visible consequence: Identifies the yarn behind the work.
- Final minimal correction: Con hilo detrás
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-40: abbr_wyib_desc
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S1; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_wyib_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Sostenga el hilo en la parte posterior (lado tejido) del trabajo.
- Specific problem: Back yarn position is incorrectly equated with a knitted fabric side.
- User-visible consequence: The yarn stays behind the work irrespective of fabric side.
- Final minimal correction: Mantener el hilo detrás de la labor.
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-41: abbr_st_st_desc
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_st_st_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Tejer por el LD, tejer de revés por el WS (o tejer cada vuelta en forma circular)
- Specific problem: Generic knit and mixed LD/WS notation obscure operations and sides.
- User-visible consequence: Flat stockinette alternates knit/purl; circular stockinette is knit throughout.
- Final minimal correction: Tejer del derecho por el LD y del revés por el LR (o del derecho en cada vuelta circular)
- Evidence and necessity: ES-STOCKINETTE; ES-GLOSS. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-42: abbr_rev_st_st_desc
- Classification: CONFIRMED_DEFECT; category: LANGUAGE; severity: S3; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_rev_st_st_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: De revés por el LD, de derecho por el WS
- Specific problem: WS is mixed with Spanish LD/LR abbreviations.
- User-visible consequence: Makes the fabric sides consistent with Spanish definitions.
- Final minimal correction: De revés por el LD, de derecho por el LR
- Evidence and necessity: ES-GLOSS. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-43: abbr_g_st_desc
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_g_st_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Tejer cada fila (o tejer en vueltas alternas de derecho/revés en forma circular)
- Specific problem: Generic tejer fails to specify knit on every flat row.
- User-visible consequence: Distinguishes garter stitch from other fabrics.
- Final minimal correction: Tejer del derecho cada fila (o alternar vueltas del derecho y del revés en circular)
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-44: abbr_foll_meaning
- Classification: CONFIRMED_DEFECT; category: LANGUAGE; severity: S3; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_foll_meaning`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Siguiendo
- Specific problem: Siguiendo is a gerund, not the following row/instruction adjective.
- User-visible consequence: Matches the meaning documented in the accompanying description.
- Final minimal correction: Siguiente
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B18-45: abbr_wpi_desc
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#abbr_wpi_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Medición del espesor del hilo: se envuelve alrededor de una regla por pulgada
- Specific problem: Original says wrap around a ruler per inch but omits what is counted.
- User-visible consequence: WPI counts wraps over one inch, not length of yarn wound.
- Final minimal correction: Medición del espesor del hilo: número de vueltas de hilo que caben en una pulgada de una regla.
- Evidence and necessity: CYC-WPI; ES-WPI. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B19-46: sym_knit_name
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#sym_knit_name`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: tejer
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: derecho
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B19-47: sym_knit_desc
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#sym_knit_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Tejer por el LD, revés por el LR
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Del derecho por el LD, del revés por el LR
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B19-48: sym_purl_desc
- Classification: CONFIRMED_DEFECT; category: LANGUAGE; severity: S3; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#sym_purl_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: De revés por el LD, de derecho por el WS
- Specific problem: WS is mixed with Spanish LD/LR abbreviations.
- User-visible consequence: Makes the fabric sides consistent with Spanish definitions.
- Final minimal correction: De revés por el LD, de derecho por el LR
- Evidence and necessity: ES-GLOSS; CYC-SYMBOLS. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B19-49: sym_knit_tbl_name
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#sym_knit_tbl_name`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Tejer TBL
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Derecho TBL
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B19-50: sym_knit_tbl_desc
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#sym_knit_tbl_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Tejer a través del bucle trasero (retorcido)
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Tejer del derecho a través del bucle trasero (retorcido)
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B19-51: sym_slip_purlwise_desc
- Classification: CONFIRMED_DEFECT; category: FACT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#sym_slip_purlwise_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Punto deslizado como del revés
- Specific problem: The selected CYC right-side glyph specifies yarn behind; current text omits it.
- User-visible consequence: Yarn position changes the visible float.
- Final minimal correction: Deslizar el punto como de revés, con el hilo detrás
- Evidence and necessity: CYC-SYMBOLS; ChartSymbolData. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B19-52: sym_k2tog_desc
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#sym_k2tog_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Tejer 2 juntos - disminución inclinada hacia la derecha
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Tejer 2 juntos del derecho - disminución inclinada hacia la derecha
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B19-53: sym_ssk_desc
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#sym_ssk_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Deslizar, deslizar, tejer: disminución inclinada hacia la izquierda
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Deslizar, deslizar, tejer del derecho: disminución inclinada hacia la izquierda
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B19-54: sym_k3tog_desc
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#sym_k3tog_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Tejer 3 puntos juntos - doble disminución
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Tejer 3 puntos juntos del derecho - doble disminución
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B19-55: sym_sk2p_desc
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#sym_sk2p_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Deslizar 1, tejer 2 puntos juntos, pasar el punto deslizado por encima
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Deslizar 1, tejer 2 puntos juntos del derecho, pasar el punto deslizado por encima
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B19-56: sym_kfb_desc
- Classification: CONFIRMED_DEFECT; category: TERM; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/reference_strings.xml#sym_kfb_desc`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Tejer por delante y por detrás – aumentar
- Specific problem: Generic tejer does not distinguish the knit operation from purl.
- User-visible consequence: The English K operation must explicitly mean derecho. 
- Final minimal correction: Tejer del derecho por delante y por detrás – aumentar
- Evidence and necessity: ES-GLOSS; CYC-ABBR. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B19-57: chart_symbols_disclaimer
- Classification: CONFIRMED_DEFECT; category: CONTEXT; severity: S2; state: VERIFIED.
- Resource: `app/src/main/res/values-es/strings.xml#chart_symbols_disclaimer`; variants: default. Affected contexts: coverage rows and caller-inventory entry for this key.
- Original: Los símbolos siguen convenciones estándar. Comprueba siempre la leyenda de símbolos de tu patrón.
- Specific problem: The chart convention is right-side appearance, not the same operation on both sides.
- User-visible consequence: Prevents applying right-side instructions unchanged on a wrong-side row.
- Final minimal correction: Los símbolos muestran los puntos vistos por el lado derecho de la labor. Comprueba siempre la leyenda de símbolos de tu patrón.
- Evidence and necessity: CYC-SYMBOLS; ChartSymbolData. The stated problem changes meaning, correctness or agreement; the remaining wording was preserved.
- Checks: final-text reread; unchanged argument signature; RESOURCES PASS. Shared-code/other-language edits: none. Remaining dependency: none for this correction.

### LA-es-B05-58: singular month string receives plural counts
- Classification: CONFIRMED_DEFECT; category: LANGUAGE/CONTEXT; severity: S3; state: OPEN.
- Resource: `app/src/main/res/values-es/strings.xml#relative_time_months_ago`, default string. Original and current: `hace %1$d mes`.
- Caller: `app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsSections.kt:537`, `relativeDayText`, monthly branch at line551. Used by last-worked project text at lines417/453.
- Concrete reproduction by source composition: `today - sessionDate = 60 days`; integer division `days / 30 = 2`; `stringResource` gives **hace 2 mes**.30days gives correct **hace 1 mes**.90days gives incorrect **hace 3 mes**. This does not depend on rendering.
- Impact: ordinary project inactivity produces incorrect Spanish number agreement. A fixed mes/meses substitution cannot correctly express both values. A new awkward parenthetical spelling or ambiguous abbreviation is not used to conceal the shared caller issue.
- Precise proposed correction: shared owner should replace the month string contract with a quantity resource/call, using `pluralStringResource(R.plurals.relative_time_months_ago, months, months)` (or an equivalent narrowly scoped localized relative-date producer), Spanish one=`hace %1$d mes`, other/many=`hace %1$d meses`. Coordinate any resource-type parity changes across supported locales. Preserve current30-day arithmetic unless separately authorized.
- Verification needed for the shared fix: focused30/59/60/90-day composition checks, correct quantity argument, resource parity/compilation; independently reread affected Spanish last-worked text. No screenshots are required to establish this grammar issue.
- Scope boundary: changing shared Kotlin, shared tests or other locales is expressly excluded from this task. No shared edit performed. Spanish acceptance remains blocked until this defect is corrected and the affected rows rechecked.

## Shared handoffs and other-language impact
Only **LA-es-B05-58** is an open handoff to the shared implementation/coordination owner. No messages were sent and shared tracking documents were not edited. Existing shared corrections were reconciled against current hashes and their Spanish meaning was independently checked. Other locales may use invariant abbreviations or have different grammar; no unreviewed other-language defect is asserted.

## Validation log
| Date | Check | Actual result | Boundary |
| --- | --- | --- | --- |
| 2026-10-19 | `:app:processDebugResources :app:testDebugUnitTest` with two selected test classes, `--console=plain --no-daemon` | Exit0, BUILD SUCCESSFUL38s; resources executed; test task UP-TO-DATE | Resource processing fresh; initial test status reused, not called fresh |
| 2026-10-19 | `:app:testDebugUnitTest --rerun --tests com.finnvek.knittools.ReferenceResourceLocalizationTest --tests com.finnvek.knittools.ui.CountPluralResourcesSourceTest --console=plain --no-daemon` | Exit0, BUILD SUCCESSFUL29s; fresh3/3 tests across2 suites;0 failures/errors/skips | Exact raw XML/logs retained under reports/language-audit/es/20261019; no shared test changes |
| 2026-10-19 | All Spanish XML variants, resource identity and format-signature comparison | 1,411/1,411;0 signature errors;1,349-key parity | Static parse/type/position verification, not linguistic proof by itself |
| 2026-10-19 | `git diff --check -- app/src/main/res/values-es` and entry-hash comparison | PASS; only the three intended Spanish XML files changed among pre-existing inputs | Existing CRLF normalization warnings are not whitespace errors; unrelated work preserved |
| 2026-10-19 | Shared factual/producer hash comparison |19/19 MATCH | Explicit reuse, no new shared behavior test claim |
| 2026-10-19 | Toolchain | Gradle9.8.1; actual daemon Java home Eclipse Adoptium17; launcher Oracle21.0.12 | Existing checked-in Java17 daemon selection retained; no toolchain change |
| 2026-10-19 | Runtime/native review | NOT_RUN / NOT_REQUESTED | No concrete remaining device-only language question; no emulator, screenshots,320dp,200% or TalkBack run |

`validation.json` records exact build/test timestamps, exit codes and suite counts. `input-hashes.json` binds the final reviewed resources/callers. Raw output is ignored evidence, not the sole explanation of any conclusion. Source inspection and independent arithmetic are distinguished from the three tests actually executed.

## Native-speaker review
- Status: **NOT_REQUESTED**. No native reviewer or craft-expert sign-off is claimed.

## Closeout
- All B01–B23 reviewed:1,349 keys,1,411 variants,2,101 total coverage rows. Every source variant has exactly one primary record;17 additional contexts are deliberate, not duplicate-key inflation.
- Applied57 minimal key corrections affecting59 resource texts; all verified as above. One confirmed S3 issue remains open:LA-es-B05-58. No content row remains unread.
- Final decision: **REVIEW_COMPLETE / OPEN**, not AUDIT_PASS, because the monthly singular/plural defect remains. Missing screenshots do not cause this decision; all V values remain blank.
- No unrelated input changed, no user data was manipulated, no commit/push/PR, and no other language audit began. Temporary Gradle calls used `--no-daemon` and completed; exact daemon PIDs2400/30132 were confirmed stopped, with process closeout recorded in validation.json.

## Resume point
- Last complete batch: B23 reconciliation, after all Spanish content and corrections.
- Current unfinished language dependency: LA-es-B05-58 only. No further Spanish rewriting is proposed.
- A later authorized shared repair should recheck only the month quantity contract and its affected Spanish compositions/inputs, then reassess section16. This report does not authorize that repair or a rendering backlog.

## LA-es-B05-58 verified closeout (2026-10-19)

**Current decision: AUDIT_PASS; native-speaker review NOT_REQUESTED.** This authorized shared repair resolves the sole blocker from the historical Spanish audit below. Acceptance follows revised LANGUAGE_AUDIT.md section 16 and is content acceptance, not exhaustive UI certification.

### Confirmed path and smallest correction

`InsightsProjectRow` and `InsightsLastWorkedNote` both call `relativeDayText`. Previously its `days >= 30` branch formatted a string without quantity selection: 60 / 30 = 2 produced `hace 2 mes`. The branch now calculates the same `months = days / 30` and calls `pluralStringResource(R.plurals.relative_time_months_ago, months, months)`. Both the selection quantity and the formatted integer are the month count. The date conversion, all thresholds, and the day/week branches are byte-for-byte unchanged.

Spanish `one` is `hace %1$d mes`; `many` and `other` are `hace %1$d meses`. The same key changes from string to plurals in all 11 supported `strings.xml` files. No other resource text changed in this repair. Exact forms and input hashes for every locale are recorded in `month-fix-verification.json` and `../shared-fixes.md`. Existing wording in en/fi/sv/nb/da/de/nl/fr/it/pt is copied unchanged into each applicable form, including abbreviations. This migration is not a new language audit or a new acceptance decision for those locales.

### ES-MONTH-FIX: executed checks and Spanish reread

- `:app:processDebugResources :app:testDebugUnitTest --rerun --tests com.finnvek.knittools.ReferenceResourceLocalizationTest --tests com.finnvek.knittools.ui.CountPluralResourcesSourceTest --tests com.finnvek.knittools.ui.screens.insights.InsightsCopySourceTest --console=plain --no-daemon`: BUILD SUCCESSFUL; **6 tests / 3 suites, zero failures/errors/skips**, resource processing executed.
- `:app:assembleDebug :app:assembleDebugAndroidTest :app:runKtlintCheckOverMainSourceSet -Pknittools.isolatedTest=activepolicy -I reports/language-audit/es/20261019-month-plurals/ktlint.init.gradle --console=plain --no-daemon`: BUILD SUCCESSFUL. Ktlint is restricted to the four Kotlin files changed by this repair. No broad lint/security wrappers.
- `InsightsRelativeMonthTextTest.spanishMonthsUseQuantityInBothLastWorkedContexts`: **1 Android test PASS**, Pixel 9 / API 37, isolated `com.finnvek.knittools.activepolicytest` package. Spanish configuration context is local to the fixture; no device/app language setting changed. Real `InsightsProjectRow` and `InsightsLastWorkedNote` compositions use compiled Android resources and synthetic project data without database writes.
- Observed in both contexts: **30 → hace 1 mes; 59 → hace 1 mes; 60 → hace 2 meses; 90 → hace 3 meses**. The exact full strings are `Avance del contador: 12, <relative>` and `Última vez <relative>`. Each reads naturally and preserves its original meaning. `one` retains singular `mes`; both declared plural forms use `meses`. The resource-only quantity 1000000 also returns `hace 1000000 meses` on this Android version; no month-arithmetic redesign follows from this category check.
- Neighbor controls **0 → hoy; 1 → ayer; 6 → hace 6 d; 7 → hace 1 sem; 29 → hace 4 sem** pass in both production contexts. The source comparison confirms their implementation and resource bytes are unchanged.
- Byte comparison: only the one named resource element changes in each of the 11 XML files; every other resource byte is unchanged. Each new form has exactly one positional `%1$d`. All-file resource parity and plural `other`/duplicate checks pass. No screenshot, layout, 320 dp, 200% font scale, combination or TalkBack check was performed.

Raw logs, copied JUnit XML, APK hashes, the scoped source/byte verification and task-entry hashes are retained in ignored `reports/language-audit/es/20261019-month-plurals/`. Machine-readable final results are in `month-fix-verification.json` and the appended `month_fix` validation entry. Historical test results below remain historical.

### Section 16 reconciliation and stopping point

All **234/234** recorded Spanish input hashes matched at repair entry, so the prior B01–B23 content review is reused for unchanged content. Coverage contained exactly two failing L/C rows, both LA-es-B05-58, and no other concrete content issue or unanswered content question. Only the affected month resource, its two consumers and new output cases were re-read. The unchanged wrapping templates retain their original evidence plus this explicit month-context check.

Current inventory, parsed from all four Spanish XML files: **1,349 keys = 1,317 strings + 32 plurals; 1,413 resource variants = 1,317 strings + 96 plural items; no arrays**. All variants have exactly one current primary coverage record. Current coverage is **2,110 rows**; two retired failure rows remain as history, for **2,112 stored rows**. B05 now has **133 current resource variants / 190 current rows, PASS**; B23 reconciliation is **PASS**. The old string and its failed source-composition row are explicitly retired, never silently changed to passing historical evidence. All V cells remain blank. All unchanged coverage-row hashes retain their earlier evidence identity; changed file hashes are reconciled separately with exact before/after values, not treated as a rerun of unrelated content.

LA-es-B05-58 is **VERIFIED**, all 58 Spanish findings are closed, and no concrete content blocker remains. The 57 earlier key corrections and their 59 resource texts remain unchanged; this repair adds one key contract correction. Spanish therefore receives **AUDIT_PASS** under revised section 16. Native review remains **NOT_REQUESTED**. English's 231 supplementary rendering cases and all English audit files/registries remain byte-identical; other locales' historical audit evidence is not renewed. No Portuguese or other audit starts. No remaining follow-up for this defect, commit, push or PR.

Process closeout: task-owned Gradle PIDs 8604 and 36212 exited naturally; Kotlin compilation was in-process. The instrumentation target process also exited. No emulator was started and no task-owned background process remains. Final preservation check: 1,201 unrelated starting files unchanged, including all three English audit files; both ignored English rendering registries also match their previously recorded hashes and retain exactly 231 pending cases. `git diff --check` passes; staging remains empty.
