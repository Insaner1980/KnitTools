# German Language Audit (de)

## Scope and snapshot

- Language: standard written German, existing **du** address, non-regional `de` resources. No new brand voice or systematic stylistic normalization.
- Assigned task: complete German B01–B23 content review and necessary German-only corrections. Stop after German; no other locale, shared-code repair, rendering campaign, commit, push, PR or cross-chat message.
- Start/last update: 2026-10-19. Branch `codex/audit-korjaukset-20261018`; HEAD `e4416c7cf0a7a9ea57006a970307ee2f3d1d228f`.
- Read current AGENTS.md/CODEX.md (identical), wording directions in CLAUDE.md, revised LANGUAGE_AUDIT.md, shared-fixes.md. There was no existing German audit directory. Other languages' acceptance was not used as evidence of German correctness.
- Entry worktree was substantially dirty. `entry-status.txt`, `entry-german.diff`, `entry-texts.json` and `entry-worktree-hashes.json` preserve the starting state. German `strings.xml`, `reference_strings.xml` and `pattern_reading_assistance.xml` already contained earlier work; those entry changes were preserved. This task changes only 73 strings in the first two files and creates records here.
- Current independently enumerated coverage: **1,349 keys = 1,318 strings + 31 plural resources; 1,380 variants = 1,318 string values + 62 plural items; zero arrays** across four German XML files. Includes one alias. Default resource scan also finds two nontranslatable numerical constants (`widget_preview_primary_count=42`, `counter_step_custom_placeholder=10`); these explain the raw 1,382 default-item count. They are not missing German translations. All 31 plural resources have the same one/other categories as default.
- Additional coverage: **239 rows** = 77 abbreviation token/meaning/description triples, 38 needle rows, 46 body-size rows (123 cells, 58 ranges), 24 chart-symbol pairs, 20 care-symbol pairs, 8 canonical yarn categories and 26 generated/material-context groups. Total **1,619 coverage rows**. Identical repeated callers share records; every resource variant retains its own row.
- `inventory.json` was extracted from current German XML and current production references. Existing English batch membership was used only as a discovery index, then checked against German meanings/callers; no English text verdict or denominator was copied. `coverage.tsv` contains final German text and source/data cases, with explicit caller references and hashes.
- `final-input-hashes.json` identifies final resources, current callers, relevant shared producers/instructions/tests and durable evidence. Hash-matched historical shared evidence is listed separately in `reused-evidence.json`; a mismatching old chart-screen receipt is superseded by the matching followup, not silently accepted.
- Scope exclusions: user-entered project/folder/custom-counter/yarn names, notes and imported document content; Ravelry webpage language; Play product price/currency; Android-owned picker/permission text; developer logs, test tags, internal exceptions and debug fixture prose. App-owned labels, failure messages and fallbacks surrounding them remain in scope. Dormant German resources were read and retained, not dropped from the denominator.

## Language decisions and glossary

| Concept | Approved forms | Context / alternatives and exclusions | Evidence | Status |
| --- | --- | --- | --- | --- |
| Address | du, dein; infinitive action labels | Existing app help establishes du. Formal Sie commands inside the app reference conflicted; third-person sie/Sie referring to projects is correct and retained. | Independent full German reading | PASS |
| Knitting / crochet | Stricken / Häkeln | Craft-aware continuation and project type; knitting reference remains identified as knitting, not crochet stitch notation. | B02/B03/B18 callers; S-DE-MF | PASS |
| Row / round | Reihe / Runde | Distinct counter labels; Zählerstand/Position/Zählerfortschritt where arbitrary units are possible. | MainCounterDisplayText, widget, session callers | PASS |
| Stitch / fabric pattern | Masche / Muster, Strickmuster | Never interpret English stitch as a sewing stitch here. Anleitung/Muster can both describe a knitting pattern where context is clear. | S-DE-MF, S-DE-KL | PASS |
| Web pattern | Webanleitung | Website metadata, not a weaving design (Webmuster). Existing German Webanleitung retained as the term. | S-LANG-WEB; metadata callers | PASS |
| Repeat | Rapport, Wiederholung | Pattern multiple versus repeat count/section progress; no blanket replacement between these different meanings. | Calculator and counter callers | PASS |
| Gauge / swatch | Maschenprobe; Maschen/Reihen pro basis | German uses Maschenprobe for both sample and stated density; field/context supplies distinction. | S-DE-MF, S-DE-SWATCH; GaugePresentation | PASS |
| Knit / purl | rechts / links stricken | English K/P tokens remain explicitly English. | S-DE-MF; S-CYC-ABBR | PASS |
| Increase / decrease | Zunahme / Abnahme; zunehmen / abnehmen | Right/left lean, one/two-stitch change and consumed stitches checked independently. | S-DE-MF, S-DE-M1, S-DE-SSP, S-CYC-ABBR | PASS |
| Inter-stitch strand / loop | Querfaden / Maschenglied | Leiste and hinteres Bein were erroneous calques. Fabric side and stitch-loop orientation are different. | S-DE-MF, S-DE-M1, S-DE-KFB | PASS |
| Slip / yarn location | wie zum Rechts-/Linksstricken abheben; Faden vor/hinter der Arbeit | Relative to current working position, not automatically right/wrong fabric face. | S-DE-MF, S-DE-KL | PASS |
| Garter / stockinette | Krausstich (retained), glatt rechts | Kraus rechts is also valid; Lopi uses Krausstich, so stylistic preference did not justify changing it. Circular stockinette requires all knit stitches. | S-DE-LOPI; S-DE-MF | PASS |
| Yarn mass / thickness | Gewicht / Garnstärke | g is mass; international category labels do not determine ply count. | StandardYarnWeight; shared yarn evidence | PASS |
| Packages / dye lot | Knäuel, Strang / Farbpartie | Existing appropriate package forms retained; fractional package quantity is not a count of strands in yarn construction. | S-DE-KL; YarnUsagePresentation | PASS |
| Body sizing | Körpermaße, Brust/Büste, Taille, Hüften, Armlänge ab Achsel | Table values are body, not garment measures. Büste is an attested anatomical sense; Preteen retains the source age category without invented boundaries. | S-LANG-BUST; S-CYC-SIZE | PASS |
| Care | Waschen, Bleichen, Trocknen, Bügeln, chemische Reinigung | Read label together with glyph; professional P/F classes do not instruct home solvent treatment. | S-DE-CARE; shared glyph pixels | PASS |
| Numbers / units | decimal comma, m/cm/mm/g, min/h; one/other | Arbitrary names are not declined. Locale number/date producers and all placeholder types checked. | Production formatting code; validation.json | PASS |

## Sources

External sources below were accessed on **2026-10-19**, unless explicitly labelled retained shared evidence. Statements were evaluated in context; a glossary alone was not treated as authority for every technique step.

| ID | Title and publisher | URL | Language/region | Passage/claim and limitations |
| --- | --- | --- | --- | --- |
| S-DE-MF | Englische Strickbegriffe, Maschenfein | https://www.maschenfein.de/wp-content/uploads/2024/03/Englische-Strickbegriffe-Maschenfein.pdf | German/English | Three-page craft glossary: knit/purl, slip orientation, KFB/tbl, yarn position, fabrics and gauge. Cross-checked critical operational steps; not permission to rewrite synonyms. |
| S-DE-KL | Deutsch–Englisch Strickglossar, Knitlingo | https://www.knitlingo.com/de/strickglossar/deutsch-englisch | German/English | Craft translation equivalents and English versus German notation. |
| S-DE-M1 | Ivory Cable Sweater, DROPS Design | https://www.garnstudio.com/pattern.php?cid=10&id=12450 | German | Zunahmetipp distinguishes lifting the Querfaden from front/back and knitting the proper leg for left/right M1. |
| S-DE-SSP | Linksgeneigte Abnahmen, Knitdesign | https://www.knitdesign.de/linksgeneigte-abnahmen | German | SSP: knitwise slip, return, purl through back loops; distinguishes purl-side execution from ordinary P2tog. |
| S-DE-KFB | Zunahmen per KFB, KBF und KFBF, Ursula strickt | https://www.ursulastrickt.de/blog/375-zunahmen-per-kfb-kbf-und-kfbf/ | German | Same retained stitch knitted front/back; one extra stitch. |
| S-DE-WPI | Garnkunde, NettisNadelkunst | https://www.nettisnadelkunst-shop.de/garnkunde | German | Adjacent wraps over 2.54cm counted to estimate yarn thickness; not an exact universal weight-class mapping. |
| S-DE-SWATCH | Maschenprobe waschen, Stricken ohne Naht | https://www.strickenohnenaht.com/post/maschenprobe | German | Washing changes measured gauge; needle recommendation is a starting point. Source's material-specific care opinions were not generalized. |
| S-DE-CARE | Pflegesymbole, GINETEX Germany | https://ginetex.de/pflegekennzeichnung/pflegesymbole/ | German | Official care-symbol meanings, dryer/iron dots, flat drying, professional cleaning classes. App does not assert iron temperatures. |
| S-DE-LOPI | Nott og Nift, Lopi Design | https://www.lopidesign.is/de/products/nott-og-nift | German | Manufacturer uses Krausstich for the back-and-forth knitted shawl; supports retaining an understandable alternative. |
| S-LANG-WEB | Webmuster, Langenscheidt | https://en.langenscheidt.com/german-english/webmuster | German/English | Means woven design; inappropriate name for saved website instructions. |
| S-LANG-BUST | Büste, Duden | https://www.duden.de/rechtschreibung/Bueste | German | Anatomical female-chest sense supports not treating the existing table heading as a false translation. |
| S-CYC-ABBR | Knitting Abbreviations, Craft Yarn Council | https://www.craftyarncouncil.com/standards/knitting-abbreviations | US English | English reference tokens; use German sources for German explanation and operational clarity. |
| S-CYC-CHART | Knit Chart Symbols, Craft Yarn Council | https://www.craftyarncouncil.com/standards/knit-chart-symbols | US English | Right-side convention, stitch/lean changes, slip yarn position and cable v1. Actual existing corrected glyph pixels also inspected. |
| S-CYC-NEEDLE | Hooks & Needles, Craft Yarn Council | https://www.craftyarncouncil.com/standards/hooks-and-needles | US English | 000/00 and US/mm mappings; manufacturer variations, mm priority. |
| S-KNITPRO | Knitting Needle Sizes, KnitPro | https://www.knitpro.eu/en/blog/knitting-needle-sizes | English | US1.5/2.5 and conventional UK numbers. Web table loses zeros at UK9/10mm; cross-checked against source prose and S-HENS, not copied blindly. |
| S-HENS | Umrechnungstabelle für Stricknadeln, Laughing Hens | https://www.laughinghens.de/umrechnungstabelle-fur-nadeln | German | UK14…000 and7.5mm/UK1; upper US variations not treated as universal replacements for CYC. |
| S-CYC-SIZE | Child/Youth and head circumference, Craft Yarn Council | https://www.craftyarncouncil.com/standards/child-youth-sizes ; https://www.craftyarncouncil.com/standards/head-circumference-chart | US English | Current eight underarm-to-wrist rows and age/head categories independently read. Other current size cells use hash-matched shared CYC evidence, separately reviewed for German labels/composition. |
| SHARED-SIZE / SHARED-JP | Verified CYC size/Clover JP corrections | ../shared-fixes.md | Shared factual evidence | Current SizeChartData/NeedleSizeData match retained final hashes. Six CYC body-chart source links and Clover https://clover.co.jp/recipe/takumikikakus.pdf are documented there. Numeric evidence reused; German semantics independently reviewed. |
| SHARED-YARN / SHARED-CARE / SHARED-CHART | Shared corrected categories and glyphs | ../shared-fixes.md ; reused-evidence.json | Shared source/pixel evidence | Current hashes checked; actual care and chart images inspected. Does not establish German layout or native language approval. |

The DROPS bilingual glossary was consulted as background, but inconsistent literal equivalents there were not used to overrule the precise sources above. An attempted additional WPI PDF fetch failed; no claim relies on it.

## Batch tracker

| Batch | Owned items | Reviewed items | Progress | Verdict | Open finding IDs | Evidence |
| --- | ---: | ---: | --- | --- | --- | --- |
| B01 | 27 | 27 | COMPLETE | PASS | None | [Reading note](#reading-b01), coverage.tsv |
| B02 | 104 | 104 | COMPLETE | PASS | None | [Reading note](#reading-b02), coverage.tsv |
| B03 | 80 | 80 | COMPLETE | PASS | None | [Reading note](#reading-b03), coverage.tsv |
| B04 | 42 | 42 | COMPLETE | PASS | None | [Reading note](#reading-b04), coverage.tsv |
| B05 | 127 | 127 | COMPLETE | PASS | None | [Reading note](#reading-b05), coverage.tsv |
| B06 | 88 | 88 | COMPLETE | PASS | None | [Reading note](#reading-b06), coverage.tsv |
| B07 | 68 | 68 | COMPLETE | PASS | None | [Reading note](#reading-b07), coverage.tsv |
| B08 | 76 | 76 | COMPLETE | PASS | None | [Reading note](#reading-b08), coverage.tsv |
| B09 | 77 | 77 | COMPLETE | PASS | None | [Reading note](#reading-b09), coverage.tsv |
| B10 | 69 | 69 | COMPLETE | PASS | None | [Reading note](#reading-b10), coverage.tsv |
| B11 | 78 | 78 | COMPLETE | PASS | None | [Reading note](#reading-b11), coverage.tsv |
| B12 | 31 | 31 | COMPLETE | PASS | None | [Reading note](#reading-b12), coverage.tsv |
| B13 | 78 | 78 | COMPLETE | PASS | None | [Reading note](#reading-b13), coverage.tsv |
| B14 | 32 | 32 | COMPLETE | PASS | None | [Reading note](#reading-b14), coverage.tsv |
| B15 | 20 | 20 | COMPLETE | PASS | None | [Reading note](#reading-b15), coverage.tsv |
| B16 | 13 | 13 | COMPLETE | PASS | None | [Reading note](#reading-b16), coverage.tsv |
| B17 | 157 | 157 | COMPLETE | PASS | None | [Reading note](#reading-b17), coverage.tsv |
| B18 | 234 | 234 | COMPLETE | PASS | None | [Reading note](#reading-b18), coverage.tsv |
| B19 | 82 | 82 | COMPLETE | PASS | None | [Reading note](#reading-b19), coverage.tsv |
| B20 | 36 | 36 | COMPLETE | PASS | None | [Reading note](#reading-b20), coverage.tsv |
| B21 | 28 | 28 | COMPLETE | PASS | None | [Reading note](#reading-b21), coverage.tsv |
| B22 | 66 | 66 | COMPLETE | PASS | None | [Reading note](#reading-b22), coverage.tsv |
| B23 | 6 | 6 | COMPLETE | PASS | None | [Reading note](#reading-b23), coverage.tsv |

Counts include both XML variants and the explicitly listed additional contexts/data; they do not count every repeated call as a new meaning. T/F/L/C/R are PASS for all 1,619 reviewed records. V is blank for 1,575 rows and PASS for 44 shared symbol-pixel rows only. A blank V means unperformed German rendering, not failure and not an implied pass.

## Contextual reading evidence

<a id="reading-b01"></a>

### B01: Common controls and navigation

Read all shared actions, headings, units and error fragments in German. Infinitive action labels and du help are compatible. German noun capitalization is retained, not English title case. cm/mm/m/g and h/min denote units, never yarn thickness. Names and URLs are interpolated as opaque values; colon/separate-sentence constructions avoid assumed gender or declension.

<a id="reading-b02"></a>

### B02: Projects and folders

Read active/completed, bulk-count, create, reactivate and delete copy. ProjectFolderComponents confirms All Projects/Unfiled are virtual filters, folder deletion keeps projects, and reorder actions move vertically. Inspected selectedFilterDescription: every inserted base already ends with a period, so folder_selected_description is correct unchanged. Folder names are NFC-normalized and limited to 50 UTF-16 units; German says maximum length without claiming grapheme count. Completion is reversible; project deletion is destructive.

<a id="reading-b03"></a>

### B03: Main counter and widget

Reviewed ROWS/Reihe, ROUNDS/Runde, REPEATS/Wiederholung and custom labels through MainCounterDisplayText/CounterValueFormatter and CounterWidget. Custom nouns are not forcibly inflected. Target, remaining and over-target counts preserve argument order; step 1 and larger steps have separate descriptions. The widget target caller has no row-type guard: neutral Zählerstand corrects its false Reihe. Counter/session/photo/reminder position labels remain neutral where the selected unit is unknown.

<a id="reading-b04"></a>

### B04: Extra counters and reminders

Read linked versus independent counters, repeating/shaping/section descriptions, start/current/target fields and reset/delete dialogs. ProjectCounterLogic owns progression; repeat number is distinct from position within its section. Reminder positions refer to main-counter values, so neutral counter wording is required. Existing reminder title/action and quoted user message have different roles; the message is not a knitting instruction. No automatic row-only interpretation of crochet rounds.

<a id="reading-b05"></a>

### B05: Sessions, history and Insights

Read all recovery Add/Discard/Edit, start/stop, session edits, ranges, trends, streaks and completion-event text. Counter progress is neutral because sessions lack historical unit snapshots; current ROWS selection does not reinterpret old counts. Inspected InsightsScreen arguments: days meta quantity is daysInRange, numerator activeDays; German dative Tag/Tagen follows the denominator. 0 of 1, 1 of 1, 2 of 2 are coherent. Reused current B05 date-set repair after checking hashes: bounded active dates and today-ended All Time. Completion labels count events, not distinct projects. Durations split integer hours/minutes; locale dates use skeletons. Historical row/pace copy retained as dormant resources, not asserted as currently displayed.

<a id="reading-b06"></a>

### B06: My Yarn and care

Read yarn mass, length, package quantity, dye lot, fiber, status and care labels. Thickness is Garnstärke; g is mass. Canonical Lace/Fingering/Sport/DK/Worsted/Aran/Bulky/Super Bulky are searchable industry identifiers, not untranslated prose. The removed bare-ply mapping is not resurrected; yarn_weight_ply_format has no current caller. Independently compared all 20 German care labels to CareSymbol mappings and retained shared pixels. GINETEX confirms the temperature wash digits, hand wash, bleach prohibitions, dryer dots, flat drying, iron dots and P/F professional cleaning classes. Schonend/Normal trocknen are intelligible with the dryer icon. No unsupported material-specific washing recommendation added.

<a id="reading-b07"></a>

### B07: Project yarn notes and usage

Reviewed source cards versus project-only notes and saving to My Yarn. Read YarnUsagePresentation and YarnUsageCalculator: planned/allocated/used are independent nullable quantities, remaining=allocated-used, negative remainder uses absolute overage, unknown is not zero. Meters/yards/grams/fractional packages use a confirmed positive length/mass ratio; no automatic stash deduction is promised. Usage deletion names only the usage record and explicitly preserves source metadata. 1 Strang and 1,5 Stränge use the displayed decimal value for plural selection.

<a id="reading-b08"></a>

### B08: Saved patterns, web links and Ravelry

Reviewed metadata availability (free/paid/unknown), duplicate actions, external website versus readable PDF, link/replace/unlink/delete messages. Inspected SavedPatternRepository deletion/reference checks and CounterRepository metadata attach/unlink: linked PDFs survive Library deletion; master annotation layers cascade while project layers remain project-owned; metadata replacement does not remove documents. Ravelry opens the WebView and its own sign-in; no API/OAuth/sync promise. Webmuster is the German weaving word, so replace only this false friend with the existing Webanleitung. Keep valid Muster/Anleitung alternatives elsewhere. Title/designer alias has the same 120-unit validation limit in both contexts.

<a id="reading-b09"></a>

### B09: PDF intake and project documents

Reviewed incoming Save pattern choices, primary-document/order/remove states, camera permission and gallery conversion messages. ProjectDocumentRepository removes only the relation and cleans files only when unused; removing primary promotes another and removing last leaves no readable document. PatternImageImportLimits confirms 20 images, 25 MiB each and 200 MiB total; copy makes image-to-PDF purpose clear, no OCR. WebPdfDownloader has HTTPS/cookie handling and a 100 MiB cap. Cancellation/error wording does not promise a completed save. Imported labels are user content cleaned by PatternDisplayNames.

<a id="reading-b10"></a>

### B10: Reading assistance and bookmarks

Read every variant in pattern_reading_assistance.xml and related viewer controls. Page indices are shown plus one. Position refers to selected project counter, not an invariant row unit. Marker recording, two-point calibration, guide visibility and follow/pause are separate actions. PatternBookmarkRepository jump updates page/Y with follow false; bookmarks are document-key scoped. Library guides are local viewer state, not project annotations. No promise that guides/bookmarks are part of marked-PDF export.

<a id="reading-b11"></a>

### B11: PDF annotations and export

Read all 75 annotation-resource strings, actions, states, shape names, limits and progress. Project editable overlay versus read-only Library layer is explicit. PatternPdfExporter checks a distinct destination, renders to a temporary PDF, then copies and cleans it; German copy preserves original-document distinction. Completed/total pages in progress are arguments in that order. Undo/redo, selection, color, width and erase descriptions are intelligible and not mistaken for bookmark/reading-line actions.

<a id="reading-b12"></a>

### B12: Notes and progress photos

Read local notes save states, photo naming/import/capture/share/delete and project/global photo contexts. User note/photo content is not translated. Counter positions accompanying photos are neutral; project names remain opaque labels. Bulk delete counts have singular/plural variants; share invokes external destination, not an app cloud promise. Existing note merge/persistence ownership remains unchanged; no actual persistence/rollback test claimed.

<a id="reading-b13"></a>

### B13: Measurements and Gauge

Read every field, explanation, warning, error, plural and clipboard section; inspected all GaugePresentation task branches and MeasurementCalculator. Width uses stitch density, height uses row density; independent provenance is stated. Exact canonical values survive unit changes; nearest integer output and approximation warnings are separate. 10 cm and 4 in are different bases (4 in=101.6 mm), not interchangeable equal lengths. Decimal comma input and localized output fit German. Task copy labels original/target/actual and does not imply saving to projects. Swatch washing/measurement advice agrees with German craft source; instructions cannot override yarn care.

<a id="reading-b14"></a>

### B14: Increase/decrease

Read modes/styles, all validation messages, examples and generated notation caller. M1 consumes no old stitch; flat inputs have N-1 internal bars and circular N. K2tog consumes two and reduces one. 96+12 gives108; 96-12 gives84. English regex examples remain explicitly identified as English and accepted by InstructionParser; no claim of free German sentence parsing. Added definitions to existing calculator help for K/M1/K2tog, numeric K counts and repetition multiplication so generated output can be understood in German. No algorithm edit.

<a id="reading-b15"></a>

### B15: Cast On

Read desired width, stitch gauge, repeat, total edge stitches and actual width. Inspected CastOnCalculator with verified shared nearest-repeat repair: use exact count before selecting multiple, ties choose lower, then add total edges. Example 20cm at20/10cm with repeat6+2 edges gives42+2=44 stitches and22cm; imperial22/4in across8in gives44. German edges wording describes a total (2 means one each side), not per-side duplication. Alternatives describe width resulting from the full count.

<a id="reading-b16"></a>

### B16: Yarn estimator

Read all inputs/results and inspected YarnEstimator/YarnEstimatorScreen. Required length divided by package length yields exact packages; purchase count rounds upward; totalWeight is whole purchased packages times package mass. Corrected label to Gewicht ganzer Knäuel and conditional reserve advice. Example550m/100m=5.5 packages, buy6; at50g total300g, not275g consumed. BadgePill uppercase=false preserves g. Knäuel and Strang are valid package terms in their actual contexts; no blanket renaming.

<a id="reading-b17"></a>

### B17: Needles and size tables

Reviewed all German categories/headers plus every current table row:38 needle rows,46 sizing rows,123 measurement cells including 58 ranges. Reused hash-matched CYC/Clover shared factual repair; independently checked regional conventions and German interpretation. UK values are conventional labels, not SI calculations; use mm if manufacturers differ. JP0–15 map exactly2.1–6.6mm. All sizing measurements are body measures; inches are cm/2.54 rounded for display. Women/men categories have their own row prefix in hand chart. Arm column is underarm-to-wrist; baby entries identify months. Preteen and Büste were retained as intelligible category/anatomical wording; no styling-only rename.

<a id="reading-b18"></a>

### B18: English abbreviation reference

Read every token/meaning/description triple and all categories/search UI. AbbreviationData is a knitting reference even for crochet users; German entry/title now identify English knitting notation. Independently verified knit/purl, slip orientation, yarn position relative to work, stitch loops versus fabric sides, increases/decreases, cable holds, short rows, fabrics and WPI using German craft references. Corrected actual calques and conflicting formal Sie commands; preserved impersonal infinitive descriptions and valid loans. Krausstich is used by Lopi; preference for kraus rechts alone is insufficient to change it. Informal project acronyms remain disclosed English tokens with German definitions.

<a id="reading-b19"></a>

### B19: Chart-symbol reference

Read each of 24 displayed symbol/name/description triples, category/disclaimer/search copy and two dormant knitwise-slip resources. Reused actual followup glyph pixels for corrected P2tog/KFB plus current source hashes; checked all German labels against CYC right-side convention and German operation sources. Explicitly disclosed right-side view; corrected KFB/Slip/SK2P precision and invalid K/P+zus hybrid tokens. Four/six-stitch cables and held-front/back directions agree with v1 drawings. No universal-symbol claim; instruction-specific legend takes precedence.

<a id="reading-b20"></a>

### B20: Settings and preferences

Read settings, theme/system/language/haptic/unit choices, data/backup link and help. Existing du address preserved. AppLanguage maps de and de-* to German; language display names are autonyms. GaugeViewModel uses imperial preference only for initialization and later allows independent unit changes, so corrected the absolute all-calculators claim. No new preference behavior; rendering and locale switching not executed.

<a id="reading-b21"></a>

### B21: Backup and privacy

Read backup export/preview/restore/cancel/failures, destructive replacement and privacy text. BackupRepository prepares/validates an archive then replaces live tables/files inside the existing transaction path. Copy correctly says replace, not merge; unencrypted archive warning and user-selected storage are explicit. Settings/purchase state are not promised in the data archive. Inspected PostHogAnalytics opt-in gate, allowlisted route names and duration/event fields; copy does not claim project content collection. No export/import or network analytics operation executed.

<a id="reading-b22"></a>

### B22: Pro and onboarding

Read all Pro states,14-day trial, purchase/restore actions, basic versus existing-content access and prompt plural variants. ProState/TrialManager and repository gates confirm status distinction, free active-project limit and preserved existing content. Billing price text comes from Play, not a German hardcoded price; no real billing run claimed. Reminder gates apply to all main-counter units: changed three row-only labels to counter wording. Existing prompt wording about notes/photos/counters retained; no Pro policy change.

<a id="reading-b23"></a>

### B23: Reconciliation and non-resource output

Reconciled current XML keys/types/variants, aliases, callers and source-owned output. No target key is missing or extra; default-only plural categories are explicitly reconciled. Grouped repeated identical callers but recorded materially different argument/meaning contexts. Included English notation tokens, all reference rows/cells, canonical yarn categories, care/chart glyphs, localized number/date/duration output, filename-derived labels and external/system/user text boundaries. Debug demo names/test tags/logs/internal exception messages are not production translation copy. No unassigned German text or open concrete language question remains.

## Findings

**29 confirmed finding groups; 73 German string values corrected. All VERIFIED: 7 S1, 17 S2 and 5 S3.** Original/final pairs below are the entry working-tree text, not HEAD text. `corrections.json` also retains the exact path, key, variant, production caller references, problem and verification for each change. All variants here are strings/default; no plural wording was changed. Every listed correction was reread in the final German context. The common resource verification is recorded in validation.json; source reasoning establishes language/technique correctness, not device execution.

### LA-de-B02-01

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: ProjectFolderComponents; existing German du convention.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `folder_name_required` | Geben Sie einen Ordnernamen ein. | Gib einen Ordnernamen ein. | Formal Sie command conflicts with the established du address. |

### LA-de-B02-02

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: ProjectFolderComponents; existing German du convention.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `folder_move_earlier` | Früher verschieben | Nach oben verschieben | Earlier/later describes time, but these actions reorder a vertical folder list immediately. |
| `folder_move_later` | Später verschieben | Nach unten verschieben | Earlier/later describes time, but these actions reorder a vertical folder list immediately. |
| `folder_move_earlier_description` | Ordner %1$s früher verschieben | Ordner %1$s nach oben verschieben | Earlier/later describes time, but these actions reorder a vertical folder list immediately. |
| `folder_move_later_description` | Ordner %1$s später verschieben | Ordner %1$s nach unten verschieben | Earlier/later describes time, but these actions reorder a vertical folder list immediately. |

### LA-de-B03-01

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: CounterWidget.WidgetTargetLabel; MainCounterDisplayText.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `row_label_with_target` | Reihe %1$d / %2$d | Zählerstand %1$d / %2$d | WidgetTargetLabel receives all main-counter types without a ROWS guard; Reihe falsely labels rounds, repeats and custom counts. |

### LA-de-B08-01

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-LANG-WEB; SavedPatternRepository and CounterRepository metadata methods.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `web_pattern_add` | Webmuster hinzufügen | Webanleitung hinzufügen | Webmuster means a woven pattern, whereas this feature saves a knitting/crochet instruction website link; use the existing German Webanleitung term. |
| `web_pattern_label` | Webmuster | Webanleitung | Webmuster means a woven pattern, whereas this feature saves a knitting/crochet instruction website link; use the existing German Webanleitung term. |
| `web_pattern_edit` | Webmuster bearbeiten | Webanleitung bearbeiten | Webmuster means a woven pattern, whereas this feature saves a knitting/crochet instruction website link; use the existing German Webanleitung term. |
| `web_pattern_edit_title` | Webmuster bearbeiten | Webanleitung bearbeiten | Webmuster means a woven pattern, whereas this feature saves a knitting/crochet instruction website link; use the existing German Webanleitung term. |
| `web_pattern_delete` | Webmuster löschen | Webanleitung löschen | Webmuster means a woven pattern, whereas this feature saves a knitting/crochet instruction website link; use the existing German Webanleitung term. |
| `web_pattern_delete_confirm_title` | Webmuster löschen? | Webanleitung löschen? | Webmuster means a woven pattern, whereas this feature saves a knitting/crochet instruction website link; use the existing German Webanleitung term. |
| `web_pattern_edit_description` | Webmuster %1$s bearbeiten | Webanleitung %1$s bearbeiten | Webmuster means a woven pattern, whereas this feature saves a knitting/crochet instruction website link; use the existing German Webanleitung term. |
| `web_pattern_attach_description` | Webmuster %1$s verknüpfen | Webanleitung %1$s verknüpfen | Webmuster means a woven pattern, whereas this feature saves a knitting/crochet instruction website link; use the existing German Webanleitung term. |
| `web_pattern_save_failed` | Webmuster konnte nicht gespeichert werden | Webanleitung konnte nicht gespeichert werden | Webmuster means a woven pattern, whereas this feature saves a knitting/crochet instruction website link; use the existing German Webanleitung term. |
| `web_pattern_update_failed` | Webmuster konnte nicht aktualisiert werden | Webanleitung konnte nicht aktualisiert werden | Webmuster means a woven pattern, whereas this feature saves a knitting/crochet instruction website link; use the existing German Webanleitung term. |
| `web_pattern_delete_failed` | Webmuster konnte nicht gelöscht werden | Webanleitung konnte nicht gelöscht werden | Webmuster means a woven pattern, whereas this feature saves a knitting/crochet instruction website link; use the existing German Webanleitung term. |

### LA-de-B14-01

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: IncreaseDecreaseCalculator and IncreaseDecreaseScreen; S-DE-MF.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `tip_flat_vs_circular_desc` | Flach gestrickt: Du arbeitest hin und her auf geraden oder Rundnadeln und wendest am Ende jeder Reihe.\n\nRund gestrickt: Du arbeitest in einer fortlaufenden Runde. Zu- und Abnahmen werden gleichmäßig über die Runde verteilt. | Flach gestrickt: Du arbeitest hin und her auf geraden oder Rundnadeln und wendest am Ende jeder Reihe.\n\nRund gestrickt: Du arbeitest in einer fortlaufenden Runde. Zu- und Abnahmen werden gleichmäßig über die Runde verteilt.\n\nDie Ergebnisse verwenden englische Abkürzungen: K = rechts stricken, M1 = eine Masche aus dem Querfaden verschränkt zunehmen, K2tog = zwei Maschen rechts zusammenstricken. Die Zahl nach K gibt die Maschenzahl an, × die Anzahl der Wiederholungen. | Generated K/M1/K2tog sequences are untranslated notation with no explanation in the calculator; existing contextual help can explain them without shared code. |

### LA-de-B16-01

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: YarnEstimator and YarnEstimatorScreen; whole-package calculation in B16 note.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `total_weight` | Gesamtgewicht: %s g | Gewicht ganzer Knäuel: %s g | The displayed mass is ceil(required length/package length)*package mass, not consumed yarn mass. |

### LA-de-B16-02

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: YarnEstimator and YarnEstimatorScreen; whole-package calculation in B16 note.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `extra_skein_note` | Kaufe immer ein zusätzliches Knäuel — Farbpartien können variieren | Kaufe bei Bedarf ein zusätzliches Knäuel — Farbpartien können variieren | Always buying one extra package is unsupported regardless of project size or an existing reserve; retain dye-lot advice as conditional. |

### LA-de-B17-01

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-CYC-NEEDLE, S-CYC-SIZE; current NeedleSizeData/SizeChartData.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `tip_needle_sizes_desc` | Metrisch (mm) ist der universelle Standard. US-Größen verwenden ein nummeriertes System (0–50). UK/Kanada nutzen ein anderes Nummernsystem. Japanische Größen haben eine eigene Nummerierung. Prüfe sicherheitshalber immer die mm-Angabe. | Metrisch (mm) ist der universelle Standard. US-Größen verwenden ein nummeriertes System. UK/Kanada nutzen ein anderes Nummernsystem. Japanische Größen haben eine eigene Nummerierung. Prüfe sicherheitshalber immer die mm-Angabe. | Claimed US range 0–50 omits 00/000 already shown in the actual table; remove only the false range. |

### LA-de-B17-02

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-CYC-NEEDLE, S-CYC-SIZE; current NeedleSizeData/SizeChartData.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `size_col_arm` | Arm | Armlänge ab Achsel | The eight values are arm length from underarm to wrist, not whole arm length or circumference. |

### LA-de-B17-03

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-CYC-NEEDLE, S-CYC-SIZE; current NeedleSizeData/SizeChartData.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `size_row_age_0_3m` | 3 m | 3 Monate | Lowercase m is the meter symbol in a measurement table; the row identifies an age in months. |
| `size_row_age_3_6m` | 6m | 6 Monate | Lowercase m is the meter symbol in a measurement table; the row identifies an age in months. |
| `size_row_age_6_12m` | 12 m | 12 Monate | Lowercase m is the meter symbol in a measurement table; the row identifies an age in months. |
| `size_row_age_12_18m` | 18 m | 18 Monate | Lowercase m is the meter symbol in a measurement table; the row identifies an age in months. |
| `size_row_age_18_24m` | 24 m | 24 Monate | Lowercase m is the meter symbol in a measurement table; the row identifies an age in months. |

### LA-de-B18-01

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-DE-MF, S-DE-KL, S-CYC-ABBR; token/meaning/description triples.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `abbreviations_title` | Abkürzungen | Englische Abkürzungen | The table tokens are English, not German abbreviations; disclose the notation language. |
| `desc_abbreviations` | Nachschlagewerk für gängige Strickabkürzungen | Nachschlagewerk für gängige englische Strickabkürzungen | Library entry must identify the English notation convention it opens. |

### LA-de-B18-02

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-DE-MF, S-DE-KL, S-CYC-ABBR; token/meaning/description triples.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `abbr_yo_desc` | Wickeln Sie den Faden um die Nadel, um eine neue Masche und ein dekoratives Loch zu erzeugen | Wickle den Faden um die Nadel, um eine neue Masche und ein dekoratives Loch zu erzeugen | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |
| `abbr_kwise_desc` | Führen Sie die Nadel ein, als ob Sie rechts stricken würden | Führe die Nadel ein, als ob du rechts stricken würdest | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |
| `abbr_pwise_desc` | Führen Sie die Nadel wie zum Linksstricken ein | Führe die Nadel wie zum Linksstricken ein | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |
| `abbr_psso_desc` | Heben Sie die abgehobene Masche über die zuletzt gearbeitete Masche | Hebe die abgehobene Masche über die zuletzt gearbeitete Masche | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |
| `abbr_dec_desc` | Reduzieren Sie die Anzahl der Maschen | Reduziere die Anzahl der Maschen | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |
| `abbr_inc_desc` | Fügen Sie der Arbeit Maschen hinzu | Füge der Arbeit Maschen hinzu | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |
| `abbr_sl1k_desc` | Heben Sie eine Masche ab, als ob Sie rechts stricken würden | Hebe eine Masche ab, als ob du rechts stricken würdest | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |
| `abbr_sl1p_desc` | Heben Sie eine Masche ab, als ob Sie links stricken würden | Hebe eine Masche ab, als ob du links stricken würdest | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |
| `abbr_pu_desc` | Nehmen Sie Maschen entlang einer Kante auf | Nimm Maschen entlang einer Kante auf | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |
| `abbr_pm_desc` | Platzieren Sie einen Maschenmarkierer auf der Nadel | Platziere einen Maschenmarkierer auf der Nadel | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |
| `abbr_sm_desc` | Bewegen Sie den Markierer von der linken zur rechten Nadel | Bewege den Markierer von der linken zur rechten Nadel | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |
| `abbr_wt_desc` | Wickeln Sie die nächste Masche ein und wenden Sie die Arbeit, um die Reihen zu verkürzen | Wickle die nächste Masche ein und wende die Arbeit, um die Reihen zu verkürzen | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |
| `abbr_cont_desc` | Arbeiten Sie wie gewohnt weiter | Arbeite wie gewohnt weiter | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |
| `abbr_patt_desc` | Fahren Sie im gewohnten Muster fort | Fahre im gewohnten Muster fort | Formal Sie address is inconsistent with German app help using du; change only address and necessary verb inflection. |

### LA-de-B18-03

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-DE-MF, S-DE-KL, S-CYC-ABBR; token/meaning/description triples.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `abbr_ws_desc` | Die private/Rückseite der Arbeit | Die Rückseite der Arbeit | Private is a mistranslation of the wrong/inner fabric side and has no craft meaning here. |

### LA-de-B18-04

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-DE-MF, S-DE-KL, S-CYC-ABBR; token/meaning/description triples; S-DE-M1.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `abbr_m1l_desc` | Nehmen Sie nach links geneigte Zunahmen mit der Leiste zwischen den Maschen vor | Nimm eine nach links geneigte Masche aus dem Querfaden zwischen den Maschen zu. | Leiste means strip/slat, not the strand used for M1; also correct formal address. |
| `abbr_m1r_desc` | Rechtsgerichtete Zunahmen mit Hilfe der Leiste zwischen den Maschen | Nach rechts geneigte Zunahme aus dem Querfaden zwischen den Maschen | Leiste is not the inter-stitch strand; preserve the named right lean. |

### LA-de-B18-05

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S1. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-DE-MF, S-DE-KL, S-CYC-ABBR; token/meaning/description triples; S-DE-KFB.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `abbr_kfb_meaning` | Vorder- und Rückseite stricken | Eine Masche rechts und rechts verschränkt stricken | Front/back refers to loops of one stitch, not the fabric sides. |
| `abbr_kfb_desc` | Zum Zunehmen vorne und hinten in die gleiche Masche stricken | Dieselbe Masche zuerst rechts und dann rechts verschränkt abstricken, erst danach von der Nadel gleiten lassen | Specify both knit operations on the same retained stitch; yields two stitches from one. |
| `abbr_pfb_desc` | Zum Zunehmen links in die Vorder- und Rückseite derselben Masche stricken | Dieselbe Masche zuerst links und dann links verschränkt abstricken, erst danach von der Nadel gleiten lassen | Front/back refers to loops of the same stitch; avoid fabric-side interpretation. |

### LA-de-B18-06

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-DE-MF, S-DE-KL, S-CYC-ABBR; token/meaning/description triples.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `abbr_tbl_desc` | Arbeiten Sie in das hintere Bein der Masche ein | Arbeite durch das hintere Maschenglied | Hinteres Bein/einarbeiten is a calque that fails to identify the needle insertion point; use the craft term and du. |

### LA-de-B18-07

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S1. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-DE-MF, S-DE-KL, S-CYC-ABBR; token/meaning/description triples.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `abbr_wyif_desc` | Halten Sie den Faden an der Vorderseite (der linken Seite) der Arbeit | Halte den Faden vor der Arbeit, unabhängig davon, welche Seite zu dir zeigt | Front yarn position is not necessarily the wrong fabric side; remove the false equivalence. |
| `abbr_wyib_meaning` | Mit Garn auf der Rückseite | Mit Garn hinter der Arbeit | Rückseite can mean the fixed wrong fabric side; WYIB is relative yarn position. |
| `abbr_wyib_desc` | Halten Sie den Faden auf der Rückseite (Strickseite) der Arbeit | Halte den Faden hinter der Arbeit, unabhängig davon, welche Seite zu dir zeigt | Back yarn position is not necessarily the knit/right fabric side. |

### LA-de-B18-08

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-DE-MF, S-DE-KL, S-CYC-ABBR; token/meaning/description triples.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `abbr_eor_desc` | Arbeiten Sie an abwechselnden Reihen | Arbeite in jeder zweiten Reihe | An abwechselnden Reihen is an unclear calque and formal address; explain alternate rows directly. |

### LA-de-B18-09

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S1. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-DE-MF, S-DE-KL, S-CYC-ABBR; token/meaning/description triples; S-DE-SSP.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `abbr_s2kp_meaning` | 2 Maschen abheben, 1 rechts stricken, die abgehobenen Maschen überziehen | 2 Maschen zusammen wie zum Rechtsstricken abheben, 1 rechts stricken, die abgehobenen Maschen überziehen | Simultaneous knitwise slip is essential to the stated centered double decrease; sequential/purlwise slipping differs. |
| `abbr_ssp_desc` | Nach links geneigte linke Abnahme | 2 Maschen einzeln wie zum Rechtsstricken abheben, zurück auf die linke Nadel setzen und links verschränkt zusammenstricken | Slip orientation and twisted purl-together step are missing from the name/description pair; plain p2tog is a different decrease. |
| `abbr_skp_meaning` | Abheben, rechts stricken, die abgehobene Masche überziehen | Wie zum Rechtsstricken abheben, rechts stricken, die abgehobene Masche überziehen | Knitwise slipping is required for this left-leaning decrease; unspecified slipping is otherwise commonly purlwise. |
| `abbr_sk2p_meaning` | 1 Masche abheben, 2 Maschen rechts zusammenstricken, die abgehobene Masche überziehen | 1 Masche wie zum Rechtsstricken abheben, 2 Maschen rechts zusammenstricken, die abgehobene Masche überziehen | Identify the knitwise slip required for SK2P. |

### LA-de-B18-10

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S1. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-DE-MF, S-DE-KL, S-CYC-ABBR; token/meaning/description triples.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `abbr_st_st_desc` | Von der Vorderseite rechts stricken, von der Rückseite links stricken (oder jede Runde im Kreis stricken) | Von der Vorderseite rechts stricken, von der Rückseite links stricken (oder in Runden alle Maschen rechts stricken) | The circular alternative omits the required knit operation; just stricken does not define stockinette. |

### LA-de-B18-11

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-DE-MF, S-DE-KL, S-CYC-ABBR; token/meaning/description triples; S-DE-WPI.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `abbr_wpi_meaning` | Wraps pro Zoll | Wicklungen pro Zoll | Translate the ordinary English word Wraps; WPI token remains unchanged. |
| `abbr_wpi_desc` | Messung der Garnstärke: Umwickelt ein Lineal pro Zoll | Zur Bestimmung der Garnstärke die nebeneinanderliegenden Wicklungen auf 2,54 cm eines Lineals zählen | Original neither supplies a grammatical instruction nor states what to count; WPI counts adjacent wraps in one inch. |

### LA-de-B19-01

- Classification: CONFIRMED_DEFECT. Category: LANGUAGE. Severity: S3. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-CYC-CHART, S-DE-MF; current ChartSymbolData and inspected shared glyphs.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `sym_yarn_over_desc` | Wickeln Sie den Faden um die Nadel, sodass ein Loch entsteht | Wickle den Faden um die Nadel, sodass ein Loch entsteht | Same formal/informal address inconsistency in the symbol reference. |

### LA-de-B19-02

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S1. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-CYC-CHART, S-DE-MF; current ChartSymbolData and inspected shared glyphs.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `sym_kfb_desc` | Vorder- und Rückseite stricken – Zunahmen | Dieselbe Masche rechts und rechts verschränkt stricken – eine Zunahme | Original says knit fabric front/back and plural increases; symbol is one KFB increase. |

### LA-de-B19-03

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S1. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-CYC-CHART, S-DE-MF; current ChartSymbolData and inspected shared glyphs.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `sym_sk2p_desc` | 1 Masche abheben, 2 re zusammenstricken, die abgehobene Masche überziehen | 1 Masche wie zum Rechtsstricken abheben, 2 re zusammenstricken, die abgehobene Masche überziehen | Same SK2P slip-orientation omission in the symbol explanation. |

### LA-de-B19-04

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-CYC-CHART, S-DE-MF; current ChartSymbolData and inspected shared glyphs.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `chart_symbols_disclaimer` | Die Symbole folgen gängigen Konventionen. Prüfe immer die Zeichenerklärung deiner Anleitung. | Die Symbole zeigen die Ansicht von der rechten Seite der Arbeit. Die Beschreibungen gelten für diese Seite, sofern nichts anderes angegeben ist. Prüfe immer die Zeichenerklärung deiner Anleitung. | Current CYC-based symbols use a right-side convention; generic common conventions does not settle how to work reverse-side rows. |

### LA-de-B19-05

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-CYC-CHART, S-DE-MF; current ChartSymbolData and inspected shared glyphs.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `sym_k2tog_name` | K2zus | K2tog | K/P plus German zus forms an invented hybrid token, inconsistent with the actual English abbreviation table and generated notation. |
| `sym_p2tog_name` | P2zus | P2tog | K/P plus German zus forms an invented hybrid token, inconsistent with the actual English abbreviation table and generated notation. |
| `sym_k3tog_name` | K3zus | K3tog | K/P plus German zus forms an invented hybrid token, inconsistent with the actual English abbreviation table and generated notation. |

### LA-de-B19-06

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S1. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/reference_strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: S-CYC-CHART, S-DE-MF; current ChartSymbolData and inspected shared glyphs.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `sym_slip_purlwise_desc` | Die Masche wie zum Linksstricken abheben, ohne sie zu stricken | Die Masche wie zum Linksstricken abheben, ohne sie zu stricken; der Faden liegt hinter der Arbeit | The adopted CYC V symbol specifies yarn in back on the right side; omission permits a different visible float. |

### LA-de-B20-01

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: GaugeViewModel initialization versus restored/manually selected units.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `tip_imperial_units_desc` | Wenn aktiviert, verwenden alle Rechner Zoll statt Zentimeter und Yard statt Meter. Die Maschenprobe wird dann pro 4 Zoll statt pro 10 cm gemessen. | Wenn aktiviert, werden Zoll statt Zentimeter und Yard statt Meter voreingestellt. Die Maschenprobe wird dann pro 4 Zoll statt pro 10 cm gemessen. Unter Maße und Maschenprobe kannst du die Einheiten separat wählen. | GaugeViewModel initializes from the preference but retains independently selectable/restored units; all calculators use these units is too absolute. |

### LA-de-B22-01

- Classification: CONFIRMED_DEFECT. Category: TERM / FACT / CONTEXT. Severity: S2. State: VERIFIED.
- Resource files: `app/src/main/res/values-de/strings.xml`. Variant: default. All affected production callers are listed per key in corrections.json and coverage.tsv.
- Evidence: ProPromptSheet; ReminderComponents; selected main-counter unit.
- Shared-code/other-language impact: none changed. The issue is resolved locally in German; no shared repair dependency.
- Verification: final-context reread; placeholder/key/type parity; Android resource compilation and six targeted resource tests PASS. No runtime or native-review claim. Remaining uncertainty: none specific.

| Key | Original | Final | Specific problem, consequence and minimal-change rationale |
| --- | --- | --- | --- |
| `pro_prompt_reminders_title` | Reihenerinnerung mit Pro hinzufügen | Zählererinnerung mit Pro hinzufügen | Reminders follow any selected main-counter unit, not only rows. |
| `pro_prompt_reminders_body` | Eine neue Reihenerinnerung erfordert Pro. Deine bestehenden Erinnerungen funktionieren weiter und bleiben bearbeitbar. | Eine neue Zählererinnerung erfordert Pro. Deine bestehenden Erinnerungen funktionieren weiter und bleiben bearbeitbar. | Reminders follow any selected main-counter unit, not only rows. |
| `pro_group_workflow_body` | Nutze den kompakten Wiederholungszähler, zusätzliche, Wiederholungs-, Formungs- und Abschnittszähler, Reihenerinnerungen, Musterfotos als PDF und das Startbildschirm-Widget. | Nutze den kompakten Wiederholungszähler, zusätzliche, Wiederholungs-, Formungs- und Abschnittszähler, Zählererinnerungen, Musterfotos als PDF und das Startbildschirm-Widget. | Reminder unit scope also applies in the Pro feature summary. |

### Considered alternatives not applied

- `folder_selected_description`: NOT_A_DEFECT / NOT_APPLIED. `%1$s` is already a complete, period-terminated description from all three branches of selectedFilterDescription. Adding another period would introduce an error.
- `abbr_g_st_meaning`: NOT_A_DEFECT / NOT_APPLIED. Krausstich is attested in German Lopi knitting instructions. Replacing it with kraus rechts would be stylistic normalization.
- `size_row_tween`: NOT_A_DEFECT / NOT_APPLIED. Preteen is an understandable loan matching the source category; a long paraphrase or invented age range is unnecessary.
- `size_col_bust`: NOT_A_DEFECT / NOT_APPLIED. Büste has the anatomical sense confirmed by Duden. Brust would be an alternative, not a proven correction.
- Knäuel/Strang, Garn/Wolle, Anleitung/Muster, infinitive descriptions versus du imperatives: kept where context is correct. No mass terminology/voice rewrite.
- Dryer Schonend/Normal and professional P/F labels: understood together with their explicit care glyphs; no additional prose required.

## Shared handoffs and other-language impact

**No new confirmed shared defect or shared repair handoff.** The existing shared changes were checked for their actual effect on German:

| Shared matter | German disposition |
| --- | --- |
| Unit-neutral positions and historical sessions | Neutral German source/counter/session/reader strings reviewed; widget target and three Pro reminder texts needed local fixes. No historical rows inferred from current editable unit. |
| Widget compact layout | Existing layout evidence retained; no German compound-specific comprehension question requiring another campaign. No German pixel claim. |
| Yarn ply hints and care glyphs | Current canonical categories have no bare ply inference; all 20 German labels paired with corrected glyphs. |
| M1 bars and Cast On nearest repeat | Current algorithms match retained tested hashes; German explanations and independent arithmetic checked. |
| Gram badge and JP/body tables | Current fixes present; g remains lowercase,38 needle and46 size rows fully included. |
| Chart symbols, KFB/P2tog followup | Old initial ChartSymbolScreen hash does not match; followup hash does. Followup actual pixels inspected, all 24 German meanings read. |
| B05 calendar/date-set repair | Current accumulator/ViewModel/test hashes match20261019 receipt; German days, ranges and chart sentences rechecked with current argument semantics. |
| Old English stop-summary expectation / PDF measurement-helper repair | Historical test-only fixes, not German text or production meaning changes. No English rendering backlog resumed. |

The coordinator can update its own German summary to the closeout below; this task did not edit LANGUAGE_AUDIT.md or shared-fixes.md and did not message another chat.

## Validation log

| Date | Input identity | Check | Actual result | Evidence / limitation |
| --- | --- | --- | --- | --- |
| 2026-10-19 | Entry hashes/status and current branch | Dirty-state preservation | Starting unrelated work preserved | 1,161 entry-hashed files; only two authorized German files differ. One Unicode-named tracked document omitted by initial quoted-path enumeration was independently verified equal to HEAD and absent from entry dirty status; details in validation.json. |
| 2026-10-19 | Final four German XMLs | XML enumeration, key/type and normalized positional placeholder signatures, aliases, quantity categories | PASS: 1,349 keys/1,380 variants; no missing/extra keys; one alias resolves;31 one/other plurals | Fresh source validation, not linguistic approval of other locales. |
| 2026-10-19 | Final resources; Eclipse Adoptium 17; Gradle 9.8.1 | `:app:processDebugResources` | Executed PASS | AAPT/Android resource processing; no APK/device run. |
| 2026-10-19 | Same Gradle command | ReferenceResourceLocalizationTest (2), CountPluralResourcesSourceTest (1), ProLocalizationSourceTest (3) | **6 executed, 6 passed, 0 failed/errors/skipped** | Source contracts inspect configured locales; only German was edited. Does not prove other-language quality. |
| 2026-10-19 | hash-matched shared inputs | Historical shared factual/algorithm evidence and actual care/chart pixels | Reused within the documented scope | Shared20261018 and followup, plus20261019-b05 receipts; not rerun. Existing images `reports/language-audit/shared/20261018/shared-care-false.png` and `reports/language-audit/shared/20261018-followup/shared-charts-false.png` were visually inspected. |
| 2026-10-19 | Final correction pairs and source callers | Final German reread, per-batch factual/context review and arithmetic | PASS | Reviewer reading, German sources and implementation; no native-speaker claim. |

Exact command, test report hashes and timestamps are in validation.json. Gradle reported BUILD SUCCESSFUL in 1m 29s,8 tasks executed and 30 up-to-date. Kotlin/source compilation dependencies reused existing up-to-date outputs; resource processing and selected tests actually executed. The daemon used the managed Eclipse Adoptium 17 installation. Its build began 12:18:26.075+03:00; log last write 12:19:35+03:00. Single-use PID 26080 exited and was verified absent. Generic Gradle 10 deprecation notice does not fail these checks; no toolchain changes were made.

## Native-speaker review

- Status: **NOT_REQUESTED**.
- No human native speaker or native craft expert participated. Source-backed assistant review is reported separately and is not native sign-off.

## Closeout

- **B01–B23 COMPLETE; German AUDIT_PASS under revised section 16.** Full current content coverage: 1,349/1,349 keys,1,380/1,380 variants and 239 additional source/context/data rows. No unexplained/unassigned items.
- **73 strings changed, 29 confirmed finding groups VERIFIED.** Actual reasons: technique errors/omissions, false craft terms, misleading counter/unit/calculation meaning, incorrect table age/arm labels and US range, missing English-notation explanation, and inconsistent Sie commands. No target edit count and no systematic rewrite.
- Specific unresolved language question, confirmed remaining S1–S3 defect or shared dependency: **none**.
- Supplementary rendering: no new German runtime checks, screenshots, font-scale/width/TalkBack checks or native review. Only 44 existing shared glyph-pixel facts reused. All other V remains blank/unverified; no exhaustive UI certification or rendering backlog is created.
- Required resource compilation and focused checks pass. Final `git diff --check` passes (only existing Git CRLF-normalization notices); byte hashes confirm this task did not alter pattern_reading_assistance.xml or pattern_annotations.xml. Exact originals, reasons and replacements remain in this report and corrections.json; preservation checks are in validation.json.
- No commit/push/PR or messages to another chat. No temporary process remains.

## Resume point

- Last completed: B23 current coverage/input reconciliation and acceptance.
- Unfinished assigned work: none. Stop after German.
- Reopen only affected rows if their recorded input hashes or meanings change, or if a new concrete defect is supplied; no automatically scheduled further audit/rendering task.
