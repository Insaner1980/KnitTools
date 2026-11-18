# Portuguese Language Audit (pt)

## Scope and snapshot

- Decision: **AUDIT_PASS** under revised LANGUAGE_AUDIT.md section 16. B01–B23 are REVIEW_COMPLETE / PASS; native-speaker review **NOT_REQUESTED**.
- Boundary: the entire current Portuguese content set and relevant shared/generated meanings. Stop after Portuguese. No campaign-wide acceptance, rendering campaign, commit, push, pull request or messaging.
- Date: 2026-10-19. Branch: `codex/audit-korjaukset-20261018`; HEAD: `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`. Working-file hashes, not HEAD alone, identify reviewed content.
- Existing work: shared code, tests, instructions, multiple locales and earlier audit records were already dirty. `entry-status.txt`, `entry-portuguese.diff` and `entry-worktree-hashes.json` preserve the entry state. The current monthly quantity-resource migration was already present and was preserved.
- Edited production files: only `values-pt/strings.xml`, `values-pt/reference_strings.xml`, `values-pt/pattern_annotations.xml`. `pattern_reading_assistance.xml` was read and is unchanged from entry.
- Current inventory: **1,349 keys = 1,317 strings + 32 plurals; 1,413 resource variants** (96 quantity variants). No arrays. One string alias resolves locally. All four XML files were read in full.
- Additional inventory: **623 rows**: 218 source/composition cases (182 formatting-contract groups + 36 material contexts), 86 code-text records (77 abbreviation triples, 8 thickness identifiers, 1 debug-fixture disposition), 275 numeric cells (152 needle cells + 123 body measurements), and 44 symbol/label pairs (24 chart + 20 care). **2,036 current coverage rows total.**
- Scope exclusions: arbitrary user-authored/imported names, notes, web pages, designer metadata and system/provider dialogs are data or external copy. Their insertion into app text was reviewed. Debug seed sample names/notes are classified separately; the release seeder is a no-op. No real purchase, destructive restore, or live Ravelry account action was needed.
- `input-hashes.json` freezes resources, source and validation inputs. The broad manifest is an identity/preservation record, not a claim that every app source line was reviewed. `coverage.tsv`, `caller-inventory.json`, `formatted-callers.json` and the batch notes identify the reviewed scope. No frozen input drift occurred during the checks.
- The prior Spanish inventory was used only as a key-to-batch discovery map. Portuguese resources, technique descriptions, grammar and context decisions were read independently; no Spanish or English acceptance verdict was copied.

### Current Portuguese resource identity

| File | SHA-256 |
| --- | --- |
| `app/src/main/res/values-pt/pattern_annotations.xml` | `9c83aefcedb9ba2895b9150affb8573276293337b56d2c97d6b0fd20fa7a969e` |
| `app/src/main/res/values-pt/pattern_reading_assistance.xml` | `2bd860c9737af6be27adc989fa477e5c0ea6dfcfc2eaf572e1efca4b5cf07865` |
| `app/src/main/res/values-pt/reference_strings.xml` | `cd0fd5e0400b483257846431d4d11a3228053087c86c4d8c3fb44e549a34ea56` |
| `app/src/main/res/values-pt/strings.xml` | `1ee1cf611d552b4dfdc02121146e82b692e45e4c18d8078fd31f281a07078946` |

## Language decisions and glossary

The existing interface has a clear European Portuguese base: Guardar, Definições, ecrã, ficheiro, câmara, registar, croché, liga, Introduza and as suas. Preserve that base and its polite instruction voice. Existing understandable alternatives such as bebê, quadris, alvejante, você, pontos/malhas, novelos/meadas and receita/modelo/padrão do not by themselves create a material audience conflict. They were not mechanically converted. No pt-BR or pt-PT resource directory was added.

The generic pt resource qualifier does not force European numeric grammar. The configured Android locale selects pt/pt-BR versus pt-PT quantities. This review preserves those real regional rules and the already correct plural resources rather than using a hand-written English singular rule. All declared one/many/other forms were read. Short unit symbols remain invariant; numeric noun labels retain their current plural forms at large digit counts.

| Concept | Approved forms | Context / exclusions | Evidence | Status |
| --- | --- | --- | --- | --- |
| Knit / purl | meia / liga; tricotar for the general activity | A knit-specific instruction now says em meia. Do not replace every generic tricotar. | S-R4;S-DROPS;S-DMC | PASS |
| Stitch / stitch pattern | malha, ponto / ponto, motivo | Live stitches and repeated fabric motif are disambiguated by operation/context. | S-R4;S-DROPS | PASS |
| Row / round | carreira / volta | Selected counter units and gauge axes remain separate. | S-R4;SOURCE | PASS |
| Repeat | repetição; motivo | Repetition occurrence is a noun; repetir remains an action. | S-DROPS;SOURCE | PASS |
| Gauge / swatch | tensão / amostra | Density versus measured sample; row and stitch axes are independent. | S-R4;S-DROPS | PASS |
| Increase / decrease | aumento / diminuição | Knit decreases state meia; M1 uses the bar; KFB uses both loops of the same stitch. | S-R4;S-CYC-ABBR | PASS |
| Slip / yarn position | deslizar, passar, escorregar; fio à frente/atrás | Slip orientation and working-yarn location are independent of right/wrong fabric side. | S-R4;S-CYC-CHART | PASS |
| Short rows / turning | carreiras encurtadas; virar | Wrap-and-turn wraps yarn around the next stitch; not short drawn lines. | S-DROPS;S-R4 | PASS |
| Cables / hold direction | torcido; frente/atrás | Four/six-stitch cables and direction matched actual chart glyph convention. | S-R4;S-CYC-CHART | PASS |
| Yarn / package / thickness | fio; novelo, meada; espessura | Package count, mass, length and category are different. No universal ply equivalence. | S-R4-YARN;SHARED | PASS |
| Colour / dye lot | cor; lote de tingimento | Variation supports considering extra yarn, not an absolute one-extra-package rule. | S-DYE | PASS |
| Care | lavar, secar, passar a ferro, limpeza a seco | Read the actual process glyph; P/F are not chemical product names. | S-GINETEX;SHARED | PASS |
| Pattern / document | receita, modelo, padrão; PDF | Keep valid pattern words; distinguish web metadata from a readable local PDF. | SOURCE;S-DROPS | PASS |
| Guide / bookmark / annotation | guia; marcador; anotação | Context and explicit actions distinguish reference position from editable markup. | SOURCE | PASS |
| Custom names / gender | opaque user-defined names | No capitalization rewrite or forced agreement with arbitrary names. Selection uses a noun heading. | SOURCE | PASS |
| Numbers / units | locale decimal comma; cm, mm, m, yd, g, h, min | Canonical calculation precision stays independent of presentation. Regional plural selection follows Android. | S-ANDROID;S-CLDR;ANDROID-REUSE | PASS |

## Sources

All external sources below were opened/read on 2026-10-19. Specific native craft sources establish Portuguese terms; shared numeric/glyph verification is reused only for hash-matched implementation. Source consultation is not native-speaker review. Full passage/limitation register: [sources.tsv](sources.tsv).

| ID | Source | Use |
| --- | --- | --- |
| S-DROPS | [DROPS Design Portuguese knitting glossary](https://www.garnstudio.com/glossary.php?lang=pt&langf=pt&langt=en) (Portuguese; European editorial vocabulary) | Craft concepts: malha/ponto, meia/ liga, carreira/volta, tension/swatch, next/alternate, wraps and short rows. Valid alternatives were preserved. Not used as authority for every numerical conversion. |
| S-R4 | [Rosários 4 / LimaKnits, Lividus XS tutorial, p.4 abbreviations](https://rosarios4.com/wp-content/uploads/2022/01/Tutorial_Rosarios4_324_Lividus_XS_LimaKnist_PT.pdf) (Portuguese / Portugal) | Independent native craft pattern: meia/ liga, carreiras/voltas, knitwise slipping, PPM, A1E/A1D and lifted increases. These establish operation distinctions, not a mandatory global house vocabulary. |
| S-DMC | [DMC, Como ler diagramas de tricô](https://www.dmc.com/DK/pt/sbs-knitting-stitch-diagrams) (Portuguese) | Working-yarn front/back, knit versus purl, twisted stitches and chart technique vocabulary. Only relevant technique passages used; no wholesale adoption of the site wording. |
| S-R4-YARN | [Rosários 4, Marialva product specification](https://rosarios4.com/montra/marialva/) (Portuguese / Portugal) | Package mass/length, needle size and thickness category are distinct quantities. Product-specific estimates are not generalized to other yarns. |
| S-CYC-ABBR | [Craft Yarn Council, Knitting Abbreviations](https://www.craftyarncouncil.com/standards/knitting-abbreviations) (English / US notation) | Token identity and operation direction: English tokens remain in the reference and are disclosed as such. Portuguese wording checked separately with native craft sources. |
| S-CYC-CHART | [Craft Yarn Council, Knit Chart Symbols](https://www.craftyarncouncil.com/standards/knit-chart-symbols) (English / US) | Right-side symbol convention, slipped stitch yarn position and cable directions; matched current shared data/rendering evidence. Not a claim of universal chart notation. |
| S-CYC-WPI | [Craft Yarn Council, How to Measure Wraps Per Inch](https://www.craftyarncouncil.com/standards/how-measure-wraps-inch-wpi) (English) | Count adjacent, nonoverlapping wraps over one inch without excessive tension. WPI is an estimate, not a package mass or exact universal yarn category. |
| S-GINETEX | [GINETEX, Care symbols](https://www.ginetex.net/GB/labelling/care-symbols.asp) (English / international) | Professional dry cleaning P permits perchloroethylene/hydrocarbons; F hydrocarbon solvents. Legacy A is reconciled with shared care-source evidence, not claimed as a new universal current symbol. |
| S-DYE | [DROPS Design, Fiesta yarn information and FAQ](https://www.garnstudio.com/yarn.php?cid=28&show=drops-fiesta&sort=product) (Portuguese) | Different dye lots can differ in colour; even a printed yarn lot may vary. Does not justify always buying exactly one additional package. |
| S-ANDROID | [Android Developers, String resources / Quantity strings](https://developer.android.com/guide/topics/resources/string-resource#Plurals) (English / Android) | Quantity selection is locale-sensitive and formatting arguments are separate. other is required; categories are grammatical, not a simple quantity=1 branch. |
| S-AOSP | [AOSP ResourcesImpl, getQuantityText](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/content/res/ResourcesImpl.java) (English / Android source) | Uses ICU plural rules for the configured locale and falls back to other. Source semantics are corroborated by existing actual Android tests; this moving branch is not the historical test APK identity. |
| S-CLDR | [Unicode CLDR 48, Language Plural Rules](https://www.unicode.org/cldr/charts/48/supplemental/language_plural_rules.html) (Portuguese and European Portuguese rules) | pt one: integer part0..1; pt_PT one: integer1 with no fractional digits. Modern rules include many for exact nonzero million multiples. Category rules do not mandate spelling digits as words or changing the app locale. |
| S-ZERO | [Ciberdúvidas, Zero, com ou sem plural?](https://ciberduvidas.iscte-iul.pt/artigos/rubricas/idioma/zero-com-ou-sem-plural/2837) (Portuguese; regional usage) | Explains the regional zero agreement difference; supports preserving actual configured Portuguese-locale selection rather than applying Spanish/English assumptions. |
| S-NUMBERS | [Ciberdúvidas, A escrita de numerais](https://ciberduvidas.iscte-iul.pt/consultorio/perguntas/a-escrita-de-numerais/32694) (Portuguese) | Digits are normal in measurements, tables and statistics. Compact numeric UI labels remain as written; no automatic insertion of de in every many form. This is not a claim about spelling out milhão. |

Local evidence IDs:

- **SOURCE**: current callers named in coverage and `formatted-callers.json`; relevant repository, presentation and domain implementations inspected for actual semantics.
- **READ-B01…READ-B23**: independent Portuguese reading notes below and in `batch-notes.json`. Each coverage row gives its exact current text/variant or code datum, caller references and input hash.
- **RESOURCES**: fresh resource processing, three JVM tests, placeholder/type/alias/quantity reconciliation and final diff checks documented in `validation.json` and `structural-validation.json`.
- **SHARED**: `../shared-fixes.md` and the exact matching manifests listed in `reused-evidence.json`. Ten relevant calculator/table/category/glyph/widget inputs match the shared 20261018 / 20261018-followup manifests. Shared facts are reused; their Portuguese expression is newly reviewed. Earlier CYC body-size/needle tables and Clover numbered diameters remain the applicable factual references. No new shared-code edit or re-run of those calculator/glyph tests is claimed.
- **ANDROID-REUSE**: historical `reports/language-audit/en/20261018/instrumentation.log`, lines 454–482, contains two named `YarnUsageLocalizationTest` starts and successful status-code-0 completions: `fractionalSkeinCountUsesRegionalPluralRules` and `roundedSkeinCountMatchesDisplayedQuantity` (run 2026-10-18 21:09–21:15 +03:00). Current production presentation/number formatter/calculator/SkeinCountText hashes match that run’s input manifest. Current Portuguese skein forms match both entry and HEAD, and the test source matches HEAD. The historical manifest did not pin the test source itself (`NOT_IN_RECORD` is retained); this is prior runtime evidence corroborated by current source, not a newly executed exact-APK assertion. No other outcome from that larger historical run is claimed here.

## Batch tracker

| Batch | Owned rows | Reviewed rows | Progress | Verdict | Open findings | Evidence |
| --- | ---: | ---: | --- | --- | --- | --- |
| B01 | 29 | 29 | REVIEW_COMPLETE | PASS | None | READ-B01; coverage.tsv batch=B01 |
| B02 | 136 | 136 | REVIEW_COMPLETE | PASS | None | READ-B02; coverage.tsv batch=B02 |
| B03 | 115 | 115 | REVIEW_COMPLETE | PASS | None | READ-B03; coverage.tsv batch=B03 |
| B04 | 48 | 48 | REVIEW_COMPLETE | PASS | None | READ-B04; coverage.tsv batch=B04 |
| B05 | 172 | 172 | REVIEW_COMPLETE | PASS | None | READ-B05; coverage.tsv batch=B05 |
| B06 | 97 | 97 | REVIEW_COMPLETE | PASS | None | READ-B06; coverage.tsv batch=B06 |
| B07 | 83 | 83 | REVIEW_COMPLETE | PASS | None | READ-B07; coverage.tsv batch=B07 |
| B08 | 85 | 85 | REVIEW_COMPLETE | PASS | None | READ-B08; coverage.tsv batch=B08 |
| B09 | 92 | 92 | REVIEW_COMPLETE | PASS | None | READ-B09; coverage.tsv batch=B09 |
| B10 | 82 | 82 | REVIEW_COMPLETE | PASS | None | READ-B10; coverage.tsv batch=B10 |
| B11 | 83 | 83 | REVIEW_COMPLETE | PASS | None | READ-B11; coverage.tsv batch=B11 |
| B12 | 35 | 35 | REVIEW_COMPLETE | PASS | None | READ-B12; coverage.tsv batch=B12 |
| B13 | 88 | 88 | REVIEW_COMPLETE | PASS | None | READ-B13; coverage.tsv batch=B13 |
| B14 | 36 | 36 | REVIEW_COMPLETE | PASS | None | READ-B14; coverage.tsv batch=B14 |
| B15 | 27 | 27 | REVIEW_COMPLETE | PASS | None | READ-B15; coverage.tsv batch=B15 |
| B16 | 15 | 15 | REVIEW_COMPLETE | PASS | None | READ-B16; coverage.tsv batch=B16 |
| B17 | 348 | 348 | REVIEW_COMPLETE | PASS | None | READ-B17; coverage.tsv batch=B17 |
| B18 | 234 | 234 | REVIEW_COMPLETE | PASS | None | READ-B18; coverage.tsv batch=B18 |
| B19 | 82 | 82 | REVIEW_COMPLETE | PASS | None | READ-B19; coverage.tsv batch=B19 |
| B20 | 37 | 37 | REVIEW_COMPLETE | PASS | None | READ-B20; coverage.tsv batch=B20 |
| B21 | 35 | 35 | REVIEW_COMPLETE | PASS | None | READ-B21; coverage.tsv batch=B21 |
| B22 | 74 | 74 | REVIEW_COMPLETE | PASS | None | READ-B22; coverage.tsv batch=B22 |
| B23 | 3 | 3 | REVIEW_COMPLETE | PASS | None | READ-B23; coverage.tsv batch=B23 |

### READ-B01: Common controls and shared labels

Action infinitives, cancellation, retry, headings and units read in Portuguese. Selection is used for projects, photos, patterns and yarn cards: the fixed masculine plural was replaced by the invariant heading Seleção. Names are inserted unchanged; cm, mm, g, m and PDF are symbols, not inflected nouns.

Evidence: S-DROPS;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B02: Projects and folders

Projeto and pasta denote different objects. All Projects and Unfiled are filters; deleting a folder preserves its projects. Move counts describe selected projects, not folders. Complete/reopen acts on project state; deletion confirms permanent removal. Knitting/crochet and user-defined names remain distinct. FolderRepository deletion/move and CounterRepository deletion were inspected.

Evidence: SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B03: Counter and widget

Carreira, volta and repetição are separately selected by MainCounterLabelType. CUSTOM inserts the user label without assuming grammatical gender. Targets and remaining/past-target compositions use CounterValueFormatter/MainCounterDisplayText. Add/decrease names and steps describe actual deltas; widget uses the shared neutral action contract. Compact secondary counter and stitch position are separate.

Evidence: S-R4;SOURCE;SHARED;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B04: Extra counters and reminders

Named/linked counters, shaping, repeats and sections were read with their forms and MultiCounterComponents. A reminder occurrence is an indexed repetition, not an imperative: Repetição replaces Repetir. The interval chooses the plural; occurrence is argument 1, interval argument 2. Start/current/end values and linked updates are not interchangeable.

Evidence: SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B05: Sessions and Insights

Start/stop, recovery Add/Discard/Edit and date/range names were read against their actual state and argument use. DurationText substitutes its own localized number runs after quantity selection; h/min are invariant. Days-meta selects on range days, not active days. The bounded shared date correction prevents an active-day numerator exceeding the selected range. Completion counts are events, including repeat completions. Relative months use floor(days/30) and the current quantity resource in both last-worked contexts; há 1 m/há 2 m is intentionally compact. Historical row labels are not inferred from editable project units.

Evidence: SOURCE;SHARED;S-ANDROID;S-CLDR;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B06: My Yarn and care

Thickness is not package mass or ply construction. The eight retained English weight labels are industry/category identities, not untranslated sentences. Novelo and meada remain valid package words in their existing contexts. Labels were paired with all 20 care glyphs: wash temperatures/hand/cross, bleach triangle, tumble dots/cross, flat line, iron dots/cross and dry-clean circles. P and F are process classes, not solvent names; their descriptions now identify permitted solvent families. A is a legacy usual-solvents label, not a recommendation for every chemical.

Evidence: S-DROPS;S-R4-YARN;S-GINETEX;SHARED;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B07: Project yarn and usage

Planeado, reservado and utilizado are independent nullable inputs; the scope sentence wrongly excluded planned/used and was corrected. Remaining is allocated minus used; negative results are labelled overage and display absolute magnitude. Unknown is not zero. Gram/skein conversion requires a confirmed positive length/mass basis. Save to My Yarn preserves the project note and does not invent usage or reduce global stash. ICU selection follows the displayed decimal number; existing pt-BR/pt-PT Android evidence was reconciled.

Evidence: S-R4-YARN;SOURCE;ANDROID-REUSE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B08: Saved/web/Ravelry patterns

Receita, modelo and padrão are retained where they clearly mean a knitting pattern. Metadata links, local PDFs and free/paid/unknown availability remain distinct. Ravelry chrome opens its own web sign-in and PDF download, not the removed API/OAuth flow. Unlinking web metadata clears only its link. Library deletion removes its master annotation layer while attached project PDFs and project overlays remain. Duplicate and unavailable messages match the saved-pattern state.

Evidence: SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B09: PDF and image import

Photo/PDF wording does not promise OCR or cloud sync. Incoming PDF is copied before the Save pattern destination choice. Project-document name, order and primary selection describe document relations. Image order and 1-based page positions, duplicate skips, cancellation and copy/conversion failures were read. The source limits are 20 pages, 25 MiB/image, 200 MiB/session and a 100 MiB web PDF cap; resource messages that receive a limit use that supplied argument.

Evidence: SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B10: Reading assistance

Page numbers are 1-based PDF pages, not counter values. Bookmark, guide and row marker meanings stay separate. Manual guide/page movement pauses following; calibration stores first/last values. Guardar primeira/última disagreed with Primeiro/Último valor and is corrected. Follow announcements insert selected counter-label text and page numbers; percent positions use an escaped percent sign.

Evidence: SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B11: Markup and export

Read all 75 annotation resources, including tools, layers, selection, undo/redo, clear-page and export failures. Shared master notes are read-only in project readers; project notes are editable overlays. Clearing a page means its annotations, not deleting the PDF page. Export page/total and filename arguments are correctly ordered; the source document is preserved. The isolated tu imperatives in write failure now match Feche/tente and the established instruction voice.

Evidence: SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B12: Notes and photos

Local note save/autosave and failed-save messages were checked against NotesEditorViewModel: debounced saves, explicit save and final persistence retain failure state rather than claim success. Photo captions, dates, import/camera actions, selected-photo deletion and share text were read. User names and note/photo contents themselves are data, not translation inputs.

Evidence: SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B13: Measurements and gauge

Tensão denotes stitch/row density and amostra the measured swatch. Each axis retains independent manual/swatch provenance. MeasurementCalculator canonical mm and counts/mm are separate from display rounding; unit changes preserve values. Result and Copy text use localized numbers and units with explicit original/target/actual labels and estimate warnings. The tool does not promise automatic persistence.

Evidence: S-DROPS;S-R4;SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B14: Increase/decrease

K/M1/K2tog are English knit-operation tokens. Their Portuguese legend was added to the existing visible flat/circular explanation. M1 adds between stitches; flat work has N-1 internal gaps and circular work N. A K2tog consumes two source stitches, reducing by one; invalid reductions retain their source-derived error counts. The paste hints explicitly require English: Increase 12 stitches evenly across 96 stitches, 22 stitches and 30 rows per 10cm, and width 30 cm, 22 stitches match the actual regex branches. No cloud parsing is claimed.

Evidence: S-CYC-ABBR;S-R4;SOURCE;SHARED;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B15: Cast On

Body width and total edge stitches were checked against actual arithmetic: repeats round the unrounded body count, ties choose the lower usable repeat, then edges are added. Actual width includes edges; alternative totals and units remain explicit. Stitches-result uses quantity selection. Metric gauge is per 10 cm; imperial is per 4 inches, not an exact relabelling of 10 cm.

Evidence: S-DROPS;SOURCE;SHARED;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B16: Yarn estimator

Required/per-package length yields exact decimal skeins; purchase count rounds upward. Reported total mass is whole purchased packages times package mass, so the weight label now states that scope. The decimal estimate label no longer forces a plural after a value that can be 1.00. Dye-lot variation is real, but always buying exactly one extra package was unsupported; the advice now says to consider extra yarn.

Evidence: S-R4-YARN;S-DYE;SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B17: Needles and body-size tables

All 38 needle rows/four columns and 123 measurement cells across six categories were read. Exact Clover Japanese diameters are not nearest US equivalents; dashes mean no supplied mapping. US 000/00 made the old 0-50 range false. Body dimensions and range endpoints are retained; displayed inches derive from cm/2.54. Criança pequena distinguishes the toddler head range from Criança. Size-system identifiers and valid vocabulary such as quadris remain unchanged.

Evidence: SHARED;SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B18: Abbreviations

Every token/meaning/description triple was read, including knit/purl, slip orientation, lean, yarn location, cable hold, fabrics, short rows and gauge. English tokens are now disclosed in the entry description. Meia makes knit-specific operations explicit without replacing generic tricotar everywhere. SSK slips separately knitwise; S2KP slips the pair together; M1 bar orientation is specified. WYIF/WYIB are working-yarn positions, not fixed fabric sides. WPI counts adjacent wraps, and short rows/gauge use carreiras.

Evidence: S-DROPS;S-R4;S-DMC;S-CYC-ABBR;S-CYC-WPI;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B19: Chart symbols

All 24 live glyph/name/description triples were compared with the current data/drawing mappings and CYC conventions. The legend now specifies the right-side view. Knit/purl back-loop distinctions, net decreases, lifted increases and front/back cable holds were reviewed. The slipped-purlwise glyph requires yarn behind on the right side. Repeat boundaries are square brackets. Unsupported knitwise-slip labels remain unused but were still read; no data/glyph edit was made.

Evidence: S-CYC-CHART;S-R4;SHARED;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B20: Settings and privacy

European-base controls Guardar/Definições/ecrã/ficheiro/câmara and polite instructions remain. Native language names and system-default choice are identities, not Portuguese translations. Analytics is optional/release-only and uses the allowlisted PostHog boundary with EU endpoint; capture/profile/replay defaults are disabled. Imperial settings initialize defaults; Measurements and Gauge can change units, so the old all-calculators absolute was corrected. Update/check/restart actions match their states.

Evidence: SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B21: Backup and restore

Backup is a user-exported archive, not synchronization. Preview labels name project/pattern/yarn/file counts without singular agreement dependence. Restore explicitly replaces current content; archive validation and replacement precede success. Local files and database tables are included, but preferences, trial/purchases, account sessions and widgets are not transferred. Active work requires recovery after restore. Encryption is not promised. No user-data restore was performed.

Evidence: SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B22: Pro and billing

Fourteen-day trial, explicit start, remaining-day quantity, single active free project and continued access to existing content were read against TrialManager/ProState and callers. Prices come from billing formattedPrice, not a hardcoded currency. Restore purchases is distinct from backup restore or cancelling a dialog. Debug feature override does not assert a real purchase. No payment or live purchase was performed.

Evidence: SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

### READ-B23: Reconciliation

All four Portuguese files and every declared quantity are accounted for. One alias resolves to the shared 120-character error. Manifest/widget XML identities are included; unused resources are retained and marked. Non-resource English abbreviation/category identities and debug sample data are explicitly classified. No unassigned Portuguese item or specific remaining comprehension question was found. Existing regional alternatives and short numeric labels are retained; no global normalization or new locale folder.

Evidence: SOURCE;RESOURCES. Exact owned items are the corresponding `batch` rows in `coverage.tsv`.

## Findings

**54 corrected text variants across 52 keys**, each checked against its final wording. Categories: CONTEXT 7, FACT 26, LANGUAGE 9, TERM 12. Severities: S2 42, S3 12. There is no target correction count. The three occurrence-plural variants are separate records of the same correction.

The original text here is the task-entry text, including pre-existing user changes, not necessarily HEAD. `corrections.json` contains the exact machine-readable original/replacement pairs. All source/format checks below refer to the frozen hashes above; no unrun rendering or native review is implied.

### LA-pt-B01-01: n_selected (default)

- Classification: CONFIRMED_DEFECT. Category: CONTEXT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `n_selected` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: %d selecionados
- Final: Seleção: %d
- Problem, visible consequence and minimum-correction rationale: A fixed masculine plural after the number produces 1 selecionados and mismatches feminine photos/recipes/cards. A heading before the count names the selected set without inflecting the inserted count.
- Evidence: LibraryTopBar; ProjectListScreen. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B10-02: pattern_calibration_save_first (default)

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `pattern_calibration_save_first` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Guardar primeira
- Final: Guardar primeiro valor
- Problem, visible consequence and minimum-correction rationale: The adjacent label is Primeiro valor (masculine); the feminine primeira has lost its referent after the shared counter-neutral correction.
- Evidence: PatternViewerScreen.RowCalibrationState rowLabelRes/saveButtonLabelRes. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B10-03: pattern_calibration_save_last (default)

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `pattern_calibration_save_last` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Guardar última
- Final: Guardar último valor
- Problem, visible consequence and minimum-correction rationale: The adjacent label is Último valor (masculine); última no longer agrees with that label.
- Evidence: PatternViewerScreen.RowCalibrationState rowLabelRes/saveButtonLabelRes. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B04-04: reminder_repeat_occurrence_format (one)

- Classification: CONFIRMED_DEFECT. Category: CONTEXT. Severity: S3. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `reminder_repeat_occurrence_format` / `one`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Repetir #%1$d, intervalo %2$d
- Final: Repetição #%1$d, intervalo %2$d
- Problem, visible consequence and minimum-correction rationale: The displayed first number is an occurrence index, not an instruction to repeat. Replace the action infinitive with the occurrence noun; preserve interval as selection quantity.
- Evidence: MultiCounterComponents.formatCounterValueDisplay: intervalRows selects; occurrence then intervalRows format. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B04-05: reminder_repeat_occurrence_format (many)

- Classification: CONFIRMED_DEFECT. Category: CONTEXT. Severity: S3. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `reminder_repeat_occurrence_format` / `many`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Repetir #%1$d, intervalo %2$d
- Final: Repetição #%1$d, intervalo %2$d
- Problem, visible consequence and minimum-correction rationale: The displayed first number is an occurrence index, not an instruction to repeat. Replace the action infinitive with the occurrence noun; preserve interval as selection quantity.
- Evidence: MultiCounterComponents.formatCounterValueDisplay: intervalRows selects; occurrence then intervalRows format. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B04-06: reminder_repeat_occurrence_format (other)

- Classification: CONFIRMED_DEFECT. Category: CONTEXT. Severity: S3. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `reminder_repeat_occurrence_format` / `other`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Repetir #%1$d, intervalo %2$d
- Final: Repetição #%1$d, intervalo %2$d
- Problem, visible consequence and minimum-correction rationale: The displayed first number is an occurrence index, not an instruction to repeat. Replace the action infinitive with the occurrence noun; preserve interval as selection quantity.
- Evidence: MultiCounterComponents.formatCounterValueDisplay: intervalRows selects; occurrence then intervalRows format. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B16-07: total_weight (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `total_weight` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Peso total: %s g
- Final: Peso dos novelos inteiros: %s g
- Problem, visible consequence and minimum-correction rationale: The value is ceil(required length / package length) times package mass, not the mass of yarn consumed by the project.
- Evidence: YarnEstimator.estimate; YarnEstimatorScreen.YarnResultCard. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B16-08: estimated_skeins (default)

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `estimated_skeins` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Estimativa: cerca de %s novelos
- Final: Quantidade estimada de novelos: %s
- Problem, visible consequence and minimum-correction rationale: The decimal estimate can be 1,00; an unconditional post-number novelos gives incorrect agreement. The unit heading accepts every displayed decimal.
- Evidence: YarnEstimatorScreen.formatSkeinsEstimateForDisplay. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B16-09: extra_skein_note (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `extra_skein_note` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Compre sempre um novelo extra — os lotes de tingimento podem variar
- Final: Considere fio extra — os lotes de tingimento podem variar
- Problem, visible consequence and minimum-correction rationale: Always buy exactly one extra ball is an unsupported universal purchase rule; needs depend on the project and the calculator only rounds to whole packages.
- Evidence: YarnEstimator.estimate; yarn package and dye-lot context. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B17-10: tip_needle_sizes_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `tip_needle_sizes_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: O sistema métrico (mm) é o padrão universal. Os tamanhos US usam um sistema numerado (0–50). UK/Canadá usam outro sistema numerado. Os tamanhos japoneses têm a sua própria numeração. Verifique sempre o tamanho em mm para ter a certeza.
- Final: O sistema métrico (mm) é o padrão universal. Os tamanhos US usam um sistema numerado. UK/Canadá usam outro sistema numerado. Os tamanhos japoneses têm a sua própria numeração. Verifique sempre o tamanho em mm para ter a certeza.
- Problem, visible consequence and minimum-correction rationale: The stated US range 0–50 excludes 00 and 000 that this table itself shows. Remove only the false range.
- Evidence: NeedleSizeData.sizes; shared B17 evidence. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B20-11: tip_imperial_units_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `tip_imperial_units_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Quando ativado, todas as calculadoras usam polegadas em vez de centímetros e jardas em vez de metros. A tensão passa a ser medida por 4 polegadas em vez de por 10 cm.
- Final: Usa polegadas em vez de centímetros e jardas em vez de metros como unidades predefinidas. A tensão é indicada por 4 polegadas em vez de por 10 cm. Em Medidas e tensão, pode escolher outras unidades.
- Problem, visible consequence and minimum-correction rationale: Not every calculator is forced to imperial units: GaugeViewModel initializes defaults, then allows independent unit/basis selection.
- Evidence: GaugeViewModel initialization, switchBasis and switchFromUnit; CastOnScreen; YarnEstimatorScreen. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B07-12: yarn_usage_scope (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `yarn_usage_scope` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Regista apenas o fio reservado para este projeto. As quantidades em O meu fio não mudam.
- Final: Regista o fio planeado, reservado e utilizado neste projeto. As quantidades em O meu fio não mudam.
- Problem, visible consequence and minimum-correction rationale: Usage supports three independent amounts, including planned/used without an allocation; apenas o fio reservado falsely narrows the feature.
- Evidence: YarnUsageAmounts; ProjectYarnUsageRepository; YarnUsagePresentation. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B17-13: size_row_toddler (default)

- Classification: CONFIRMED_DEFECT. Category: CONTEXT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `size_row_toddler` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Criança
- Final: Criança pequena
- Problem, visible consequence and minimum-correction rationale: Toddler and child both show Criança beside different head ranges, hiding the source category distinction.
- Evidence: SizeChartData.headEntries; shared CYC head table. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-14: desc_abbreviations (default)

- Classification: CONFIRMED_DEFECT. Category: CONTEXT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `desc_abbreviations` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Referência de abreviaturas comuns de tricô
- Final: Referência de abreviaturas de tricô em inglês
- Problem, visible consequence and minimum-correction rationale: The tokens are English, not Portuguese local abbreviations. Name their language while keeping all notation identifiers unchanged.
- Evidence: AbbreviationData.abbreviations; S-CYC-ABBR. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B06-15: care_dryclean_any (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `care_dryclean_any` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Qualquer solvente
- Final: Limpeza a seco com solventes usuais
- Problem, visible consequence and minimum-correction rationale: Qualquer solvente omits the professional dry-cleaning context and suggests unrestricted chemicals.
- Evidence: CareSymbol.DRYCLEAN_ANY; S-GINETEX; shared CYC care evidence. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B06-16: care_dryclean_p (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `care_dryclean_p` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Solvente P
- Final: Limpeza a seco com percloroetileno e hidrocarbonetos (P)
- Problem, visible consequence and minimum-correction rationale: P identifies a permitted dry-cleaning process, not a chemical named solvent P.
- Evidence: CareSymbol.DRYCLEAN_P; S-GINETEX. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B06-17: care_dryclean_f (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `care_dryclean_f` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Solvente F
- Final: Limpeza a seco com hidrocarbonetos (F)
- Problem, visible consequence and minimum-correction rationale: F identifies a dry-cleaning solvent class, not a chemical named solvent F.
- Evidence: CareSymbol.DRYCLEAN_F; S-GINETEX. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-18: abbr_ssk_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_ssk_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Diminuição inclinada para a esquerda: deslizar 2 pontos em tricô, tricotá-los juntos nas alças traseiras
- Final: Diminuição inclinada para a esquerda: deslize 2 pontos, um de cada vez, como para meia, e tricote-os juntos em meia pelas alças de trás
- Problem, visible consequence and minimum-correction rationale: Em tricô can mean purl in Portuguese craft usage, whereas SSK requires knitwise slips; individual slips are also required.
- Evidence: S-R4; S-CYC-ABBR. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-19: abbr_wyif_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_wyif_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Segure o fio na frente (lado errado) do trabalho
- Final: Segure o fio à frente do trabalho
- Problem, visible consequence and minimum-correction rationale: Front is relative to the knitter, not necessarily the wrong side of the fabric. The original parenthesis falsely equates them.
- Evidence: S-R4; S-CYC-ABBR; S-DMC. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-20: abbr_wyib_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_wyib_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Segure o fio na parte de trás (lado tricotado) do trabalho
- Final: Segure o fio atrás do trabalho
- Problem, visible consequence and minimum-correction rationale: Behind the working needles is not a fixed knit/right fabric side; remove the false side equivalence.
- Evidence: S-R4; S-CYC-ABBR; S-DMC. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-21: abbr_m1_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_m1_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Pegue a barra entre os pontos e tricote na alça de trás para aumentar
- Final: Levante a barra entre os pontos da frente para trás e tricote-a em meia pela alça de trás para aumentar
- Problem, visible consequence and minimum-correction rationale: The bar must be mounted in the specified direction and worked in knit; otherwise working its back loop need not close the hole.
- Evidence: S-R4; S-DMC. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-22: abbr_s2kp_meaning (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_s2kp_meaning` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Deslize 2, tricote 1, passe os pontos deslizados por cima
- Final: Deslize 2 juntas como para meia, tricote 1 em meia e passe as malhas deslizadas por cima
- Problem, visible consequence and minimum-correction rationale: Slipping two separately gives a different result; the centered decrease requires taking the pair together.
- Evidence: S-CYC-ABBR; S-DMC. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-23: abbr_k2tog_meaning (default)

- Classification: CONFIRMED_DEFECT. Category: TERM. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_k2tog_meaning` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Tricotar 2 juntos
- Final: Tricotar 2 juntas em meia
- Problem, visible consequence and minimum-correction rationale: Tricotar alone names the craft, not the knit operation contrasted with P2tog.
- Evidence: S-R4; S-DMC. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-24: abbr_k2tog_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_k2tog_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Diminuição inclinada para a direita: tricotar dois pontos como um
- Final: Diminuição inclinada para a direita: tricotar dois pontos juntos em meia
- Problem, visible consequence and minimum-correction rationale: The procedure must specify knit rather than an unspecified knitting operation.
- Evidence: S-CYC-ABBR; S-DMC. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-25: abbr_k3tog_meaning (default)

- Classification: CONFIRMED_DEFECT. Category: TERM. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_k3tog_meaning` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Tricotar 3 juntos
- Final: Tricotar 3 juntas em meia
- Problem, visible consequence and minimum-correction rationale: K3tog requires knit, whereas the old name could also describe P3tog.
- Evidence: S-CYC-ABBR; S-DROPS. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-26: abbr_ssk_meaning (default)

- Classification: CONFIRMED_DEFECT. Category: TERM. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_ssk_meaning` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Deslizar, deslizar, tricotar
- Final: Deslizar, deslizar, tricotar em meia
- Problem, visible consequence and minimum-correction rationale: The final K operation needs meia in the Portuguese expansion.
- Evidence: S-R4; S-CYC-ABBR. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-27: abbr_skp_meaning (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_skp_meaning` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Deslizar, tricotar, passar o ponto deslizado por cima
- Final: Deslizar 1 como para meia, tricotar 1 em meia e passar o ponto deslizado por cima
- Problem, visible consequence and minimum-correction rationale: The expansion omits knitwise insertion and the knit stitch required for this specific decrease.
- Evidence: S-R4; S-CYC-ABBR. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-28: abbr_sk2p_meaning (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_sk2p_meaning` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Deslizar 1, tricotar 2 malhas juntas, passar o ponto deslizado por cima
- Final: Deslizar 1 como para meia, tricotar 2 malhas juntas em meia e passar o ponto deslizado por cima
- Problem, visible consequence and minimum-correction rationale: Slip orientation and knit operation distinguish this decrease from other slip/purl combinations.
- Evidence: S-CYC-ABBR; S-DROPS. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-29: abbr_kfb_meaning (default)

- Classification: CONFIRMED_DEFECT. Category: TERM. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_kfb_meaning` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Tricotar frente e costas
- Final: Tricotar em meia pela frente e por trás da malha
- Problem, visible consequence and minimum-correction rationale: Frente e costas can name garment panels and omits the specific knit operation; the operation works both loops of one stitch.
- Evidence: S-CYC-ABBR; S-DROPS. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-30: abbr_kfb_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_kfb_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Tricotar na frente e atrás do mesmo ponto para aumentar
- Final: Tricotar em meia pela frente e por trás do mesmo ponto para aumentar
- Problem, visible consequence and minimum-correction rationale: The description must preserve K (knit), contrasted with PFB.
- Evidence: S-CYC-ABBR; S-DROPS. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-31: abbr_ktbl_meaning (default)

- Classification: CONFIRMED_DEFECT. Category: TERM. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_ktbl_meaning` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Tricotar no laço traseiro
- Final: Tricotar em meia pela alça de trás
- Problem, visible consequence and minimum-correction rationale: Tricotar alone does not distinguish Ktbl from Ptbl.
- Evidence: S-DMC; S-DROPS. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-32: abbr_ktbl_desc (default)

- Classification: CONFIRMED_DEFECT. Category: TERM. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_ktbl_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Ponto tricotado torcido
- Final: Ponto meia torcido
- Problem, visible consequence and minimum-correction rationale: Tricotado torcido does not identify the knit stitch, unlike the paired purl entry.
- Evidence: S-DMC; S-DROPS. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-33: abbr_alt_meaning (default)

- Classification: CONFIRMED_DEFECT. Category: TERM. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_alt_meaning` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Alternativo
- Final: Alternado
- Problem, visible consequence and minimum-correction rationale: Alternate in an instruction means every other, not an alternative option.
- Evidence: S-CYC-ABBR; S-DROPS. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-34: abbr_beg_meaning (default)

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_beg_meaning` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Começando
- Final: Início
- Problem, visible consequence and minimum-correction rationale: Beg names the beginning of a row; Começando is a gerund, not the referenced position.
- Evidence: AbbreviationData; S-CYC-ABBR. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-35: abbr_foll_meaning (default)

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_foll_meaning` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Seguindo
- Final: Seguinte
- Problem, visible consequence and minimum-correction rationale: The paired description refers to the next row/instruction; Seguindo is an action gerund.
- Evidence: AbbreviationData; S-DROPS. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-36: abbr_selvage_desc (default)

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_selvage_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: O ponto de borda funcionou de forma diferente para uma borda perfeita
- Final: Ponto de borda trabalhado de forma diferente para dar acabamento à borda
- Problem, visible consequence and minimum-correction rationale: Funcionou is an erroneous translation of worked; an edge stitch is worked, not a border that functioned. Remove the unsupported promise of a perfect edge.
- Evidence: S-DROPS; S-DMC. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-37: abbr_wpi_meaning (default)

- Classification: CONFIRMED_DEFECT. Category: TERM. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_wpi_meaning` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Envoltórios por polegada
- Final: Voltas de fio por polegada
- Problem, visible consequence and minimum-correction rationale: Envoltórios means coverings/wrappers, not the number of yarn wraps used in this measurement.
- Evidence: S-CYC-WPI. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-38: abbr_wpi_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_wpi_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Medição da espessura do fio: envolve uma régua por polegada
- Final: Medição da espessura do fio: conte as voltas lado a lado, sem apertar nem sobrepor, ao longo de uma polegada de uma régua
- Problem, visible consequence and minimum-correction rationale: Envolve uma régua por polegada fails to specify what is counted and can imply wrapping the ruler itself. Explain the one-inch span.
- Evidence: S-CYC-WPI. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B19-39: sym_knit_tbl_name (default)

- Classification: CONFIRMED_DEFECT. Category: TERM. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `sym_knit_tbl_name` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Malha TBL
- Final: Meia pela alça de trás
- Problem, visible consequence and minimum-correction rationale: Malha TBL fails to distinguish the knit operation from the paired purl operation.
- Evidence: S-CYC-CHART; S-DROPS. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B19-40: sym_knit_tbl_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `sym_knit_tbl_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Tricotar na alça traseira (torcido)
- Final: Tricotar em meia pela alça de trás (torcido)
- Problem, visible consequence and minimum-correction rationale: The shared glyph is knit-through-back-loop on the stated right side, not any twisted stitch.
- Evidence: S-CYC-CHART; S-DMC. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B19-41: sym_k2tog_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `sym_k2tog_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Tricotar 2 pontos juntos – diminuição inclinada para a direita
- Final: Tricotar 2 pontos juntos em meia – diminuição inclinada para a direita
- Problem, visible consequence and minimum-correction rationale: The knit action must be explicit alongside the distinct purl glyph.
- Evidence: S-CYC-CHART; S-DMC. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B19-42: sym_ssk_desc (default)

- Classification: CONFIRMED_DEFECT. Category: TERM. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `sym_ssk_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Escorregar, deslizar, tricotar – diminuição inclinada para a esquerda
- Final: Escorregar, deslizar, tricotar em meia – diminuição inclinada para a esquerda
- Problem, visible consequence and minimum-correction rationale: The K operation must be explicit as meia. The two existing synonymous slip verbs remain unchanged; stylistic normalization is unnecessary.
- Evidence: S-CYC-CHART; S-R4. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B19-43: sym_k3tog_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `sym_k3tog_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Tricotar 3 pontos juntos – diminuição dupla
- Final: Tricotar 3 pontos juntos em meia – diminuição dupla
- Problem, visible consequence and minimum-correction rationale: K3tog requires knit, not a generic knitting operation.
- Evidence: S-CYC-CHART; S-DROPS. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B19-44: sym_sk2p_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `sym_sk2p_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Deslizar 1, tricotar 2 pontos juntos, passar o ponto deslizado por cima
- Final: Deslizar 1 como para meia, tricotar 2 pontos juntos em meia e passar o ponto deslizado por cima
- Problem, visible consequence and minimum-correction rationale: Slip orientation and knit action are required to identify the operation drawn.
- Evidence: S-CYC-CHART; S-CYC-ABBR. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B19-45: sym_kfb_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `sym_kfb_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Tricotar frente e costas – aumentar
- Final: Tricotar em meia pela frente e por trás do mesmo ponto – aumento
- Problem, visible consequence and minimum-correction rationale: Frente e costas alone can indicate garment panels; the glyph means working both loops of the same stitch in knit.
- Evidence: S-CYC-CHART; S-CYC-ABBR. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B19-46: sym_lifted_inc_left_name (default)

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `sym_lifted_inc_left_name` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Levantado Inc Esquerda
- Final: Aumento levantado à esquerda
- Problem, visible consequence and minimum-correction rationale: Levantado Inc Esquerda is a broken English/Portuguese fragment rather than an operation name.
- Evidence: S-R4. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B19-47: sym_lifted_inc_right_name (default)

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `sym_lifted_inc_right_name` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Levantado Inc à Direita
- Final: Aumento levantado à direita
- Problem, visible consequence and minimum-correction rationale: Levantado Inc à Direita is a broken English/Portuguese fragment rather than an operation name.
- Evidence: S-R4. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B19-48: sym_repeat_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `sym_repeat_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Parêntese que delimita a repetição do motivo
- Final: Parênteses retos que delimitam a repetição do motivo
- Problem, visible consequence and minimum-correction rationale: The actual renderer draws square brackets, not a round parenthesis.
- Evidence: ChartSymbolScreen.drawChartSymbol repeat branch. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B19-49: sym_slip_purlwise_desc (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `sym_slip_purlwise_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Deslize a malha como para tricotar em liga, sem a tricotar
- Final: Deslize a malha como para tricotar em liga, sem a tricotar e com o fio atrás
- Problem, visible consequence and minimum-correction rationale: The adopted right-side CYC glyph includes yarn-behind; omitting yarn position loses an essential part of the glyph meaning.
- Evidence: S-CYC-CHART; S-R4. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B19-50: chart_symbols_disclaimer (default)

- Classification: CONFIRMED_DEFECT. Category: FACT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `chart_symbols_disclaimer` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Os símbolos seguem convenções padrão. Verifique sempre a legenda de símbolos da sua receita.
- Final: Os símbolos representam os pontos vistos pelo direito do trabalho. Verifique sempre a legenda de símbolos da sua receita.
- Problem, visible consequence and minimum-correction rationale: Directional and operation glyphs use the right-side CYC convention; generic standard conventions does not communicate the side needed to interpret them.
- Evidence: ChartSymbolData; S-CYC-CHART. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B11-51: pattern_annotation_write_failed (default)

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/pattern_annotations.xml` / `pattern_annotation_write_failed` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Não foi possível guardar o texto. A anotação pode ter sido alterada. Fecha o editor e tenta novamente.
- Final: Não foi possível guardar o texto. A anotação pode ter sido alterada. Feche o editor e tente novamente.
- Problem, visible consequence and minimum-correction rationale: This error alone switches to tu imperatives Fecha/tenta within the established third-person instruction style.
- Evidence: Portuguese error-copy context throughout the same feature. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B14-52: tip_flat_vs_circular_desc (default)

- Classification: CONFIRMED_DEFECT. Category: CONTEXT. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/strings.xml` / `tip_flat_vs_circular_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Tricô plano: trabalha de ida e volta em agulhas direitas ou circulares, virando no fim de cada carreira.\n\nTricô circular: trabalha numa volta contínua. Os aumentos/diminuições distribuem-se uniformemente ao longo da volta.
- Final: Tricô plano: trabalha de ida e volta em agulhas direitas ou circulares, virando no fim de cada carreira.\n\nTricô circular: trabalha numa volta contínua. Os aumentos/diminuições distribuem-se uniformemente ao longo da volta.\n\nNos resultados, K = tricotar em meia, M1 = aumentar uma malha entre pontos e K2tog = tricotar duas malhas juntas em meia. × indica o número de repetições.
- Problem, visible consequence and minimum-correction rationale: Generated K/M1/K2tog and multiplication notation are displayed unchanged in this Portuguese calculator; the existing in-screen tip did not explain their action meanings.
- Evidence: IncreaseDecreaseCalculator; IncreaseDecreaseScreen PatternResultCard and InfoTipText. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-53: abbr_wt_desc (default)

- Classification: CONFIRMED_DEFECT. Category: TERM. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_wt_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Enrole o próximo ponto e vire o trabalho para modelar linhas curtas
- Final: Enrole o fio à volta do próximo ponto e vire o trabalho para formar carreiras encurtadas
- Problem, visible consequence and minimum-correction rationale: The technique is short-row shaping, not modelling short lines; identify the established technical term and the yarn that wraps the stitch.
- Evidence: S-DROPS; S-R4; GaugePresentation. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### LA-pt-B18-54: abbr_gauge_desc (default)

- Classification: CONFIRMED_DEFECT. Category: TERM. Severity: S2. State: VERIFIED.
- Resource: `app/src/main/res/values-pt/reference_strings.xml` / `abbr_gauge_desc` / `default`. Affected uses: all current caller references for this key in coverage.tsv.
- Original: Número de pontos e linhas por unidade de medida
- Final: Número de pontos e carreiras por unidade de medida
- Problem, visible consequence and minimum-correction rationale: Gauge measures stitch and worked-row densities; linhas can mean strands or drawn lines, while the calculator explicitly measures carreiras.
- Evidence: S-DROPS; S-R4; GaugePresentation. See source register and the owning READ-B note.
- Verification: final Portuguese wording re-read; exact entry/replacement diff, unchanged resource contract/placeholders, successful Android resource processing and affected resource JVM checks. No new runtime claim.
- Shared/other-language impact: none; Portuguese resources only. Remaining concrete uncertainty/dependency: none.

### Preserved wording and non-defects

- Regional vocabulary/spelling and legitimate craft alternatives were retained. Native sources sometimes choose different terms; difference alone is not a defect.
- Generic English abbreviation tokens and yarn-category identifiers remain because they identify notation/category, with Portuguese explanation. Names and imported values are not translated.
- `relative_time_months_ago` remains plural one/many/other, all `há %1$d m`. The compact unit is invariant and the enclosing last-worked context disambiguates it. Both production uses were checked.
- Unused legacy resources were read and retained rather than deleted. Their lack of a live caller is recorded; no rendering is claimed.
- Valid compact labels, numeric headings, and `%s` names do not receive invented agreement changes. There is no blanket singular/plural rewrite.

## Shared handoffs and other-language impact

No newly confirmed shared defect or unresolved Portuguese question requires a handoff. The previously recorded shared date-boundary and month-quantity fixes are current inputs. Their affected Portuguese sentences and callers were checked independently. No shared implementation, shared tests, other locale, root audit plan, AGENTS.md or CODEX.md was edited by this task.

## Validation log

| Date | Check | Scope | Actual result / limitation |
| --- | --- | --- | --- |
| 2026-10-19 | `:app:processDebugResources :app:testDebugUnitTest --rerun` with the two named resource suites, `--console=plain --no-daemon` | Portuguese XML plus required shared resource contracts | BUILD SUCCESSFUL in 1m37s; 3 tests, 0 failed/skipped. 38 tasks: 11 executed, 9 cached, 18 up-to-date. Tests executed; not every build task reran. |
| 2026-10-19 | XML inventory, default-key/type/placeholder parity, aliases, duplicate/Unicode checks | All 1,349 keys / 1,413 variants | PASS. No missing/extra keys, type changes, placeholder mismatch or unresolved alias. All 32 plurals retain one/many/other. |
| 2026-10-19 | Entry-to-final correction reconciliation | 54 changed variants / 52 keys | Exact match to corrections.json; no unrecorded text edit. |
| 2026-10-19 | Source hashes and earlier evidence matching | Shared facts plus regional skein presentation | Matched paths and explicit NOT_IN_RECORD limitation in reused-evidence.json. Historical Android evidence is not fresh device execution. |
| 2026-10-19 | `git diff --check -- app/src/main/res/values-pt config/language-audit/pt` | Authorized paths | PASS. Git may announce existing CRLF-to-LF normalization; no whitespace error. |
| 2026-10-19 | Entry SHA-256 preservation check | 1226 tracked/nonignored entry files | Only the three named Portuguese resource files changed; no missing entry file or outside-scope change. Frozen validation input drift: zero. |
| 2026-10-19 | Temporary process check | Gradle single-use daemon PID 41848 / exec session 11038 | Command exited 0; daemon process no longer exists. No task-started server/emulator/watcher remains. |

Exact command, suite names/test cases/timestamps and preservation summary: [validation.json](validation.json). Compact Gradle output: [resource-tests.log](resource-tests.log).

No new Android runtime, screenshot, 320 dp, 200% font-scale, combined-width/font, TalkBack, purchase or destructive restore check was performed. No concrete language question remained that required such execution. All V cells are blank; this is not a rendering pass and creates no rendering backlog.

## Native-speaker review

- Status: **NOT_REQUESTED**. No external human reviewer was contacted. AI review plus Portuguese craft-source comparison is not native craft-expert sign-off.
- No specific unresolved expression has been deferred to an invented reviewer.

## Closeout

- Coverage: 1,349/1,349 keys, 1,413/1,413 resource variants; 623 additional context/code/data/symbol records. All B01–B23 are complete. Every current row has T/F/L/C/R complete; the single debug-fixture disposition has explicit nonapplicable content dimensions and C PASS. V remains blank.
- Findings: all 54 correction records VERIFIED; zero confirmed remaining S1–S3 defects, zero specific unresolved language questions, zero new shared handoffs. Non-defect alternatives remain unchanged.
- Outcome: **Portuguese AUDIT_PASS; native-speaker review NOT_REQUESTED**. This is content acceptance under revised section 16, not exhaustive UI certification or a guarantee of no undiscovered issue.
- Existing unrelated work preserved. No commit, push, PR or messages. Stop after Portuguese.

## Resume point

- Last fully reviewed scope: B23 reconciliation of current Portuguese resources, material contexts and final evidence.
- Unfinished authorized work: none. No follow-up rendering or other-language campaign is opened.
- Later source/resource changes invalidate only affected rows and hashes; reassess them if a new task is assigned.

TSV convention: UTF-8, header, tabs as separators; embedded tabs/newlines are escaped as `\t` / `\n`. Resource Android escapes are preserved as source text. Coverage statuses are review judgments supported by the referenced text, source and checks; the inventory script itself did not perform linguistic review.
