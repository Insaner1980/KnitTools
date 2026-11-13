# Brazilian Portuguese implementation and content review (pt-BR)

Evidence limitation recorded 2026-11-13: some labels in the historical `validation.json` contain mojibake. The referenced original captures under `reports/ptbr-work` are unavailable locally, so their rendered text and the origin of the encoding corruption cannot be reverified. Original records are preserved; their `VERIFIED` labels must not be read as fresh visual verification of those strings or the current app.

## Scope and snapshot

- Date: 2026-10-19, Europe/Helsinki. Branch: `codex/audit-korjaukset-20261018`.
- Explicit bounded authorization: introduce Brazilian Portuguese alongside generic `pt`, review the complete Brazilian content, integrate existing language selection, validate, and stop. No commit, push, PR or additional language campaign.
- Content decision: **AUDIT_PASS — B01–B23 language content complete**. Native-speaker review: **NOT_REQUESTED**. This review is AI-assisted source/context review, not human native-speaker sign-off.
- Four complete new files: `values-pt-rBR/strings.xml`, `reference_strings.xml`, `pattern_annotations.xml`, `pattern_reading_assistance.xml`.
- **1,350 keys: 1,318 strings and 32 plurals; 1,414 variants**, including all 96 one/many/other items. No arrays; one local alias. File variant counts: strings 1,033; reference 257; annotations 75; reading assistance 49. The two default-only nontranslatable numeric examples remain inherited unchanged.
- **401 variants across 391 keys adapted from the current audited pt starting point**. The remaining 1,013 variants were read independently and retained. The count is a result, not a rewrite target. Exact original/final entries are in [adaptations.tsv](adaptations.tsv).
- [coverage.tsv](coverage.tsv) records every final variant plus 20 grouped code/generated-content contexts (1434 total rows), its batch, actual resource callers, current file hash and content judgments. Literal tabs/newlines inside cells are escaped as `\t`/`\n`; `finding_ids` is blank because regional adaptation decisions are grouped in this report and linked to the exact diff ledger. The prior pt ledger supplied key-to-batch discovery only; its PASS statuses were not used to decide Brazilian wording. The new native language-option label belongs to B20. B23 reconciles the whole ledger and has no exclusive resource keys.
- All four files were read in full, including unused legacy strings. Formatted-call discovery from pt was checked against current source snippets (182 key groups), with deeper reading of calculators, unit presenters, locale APIs, reference tables and composed Insights text. Caller references in the new ledger were regenerated from current source. No exhaustive rendering claim is attached to these source checks: V remains blank.
- [input-hashes.json](input-hashes.json) identifies the resource and caller inputs. [structural-validation.json](structural-validation.json) records XML, key/type, placeholder, alias and quantity checks. The inventory tool records review judgments; it does not itself perform linguistic review.
- Existing dirty source, resource, test, instruction and audit work was preserved. Other languages receive only the native option label `Português (Brasil)`. Old pt values and stored `pt` identifiers remain unchanged. Existing audit ledgers and the 231 historical English rendering cases are untouched.

## Language integration

`AppLanguage.BRAZILIAN_PORTUGUESE` stores and applies `pt-BR`. Exact matching is case-insensitive before existing base-language fallback. Thus `pt-BR` and `PT-br` stay Brazilian, `pt` stays Portuguese, and `pt-PT`/`pt-AO`, `nb-NO`, `en-US` and `fr-CA` retain their base fallback. The first comma-separated tag still controls selection; empty/unknown tags retain System default behavior.

Settings lists the enum entries and maps the new entry to the native label. Android's `locales_config.xml` declares `pt-BR`. Source tests now map that tag to Android's `values-pt-rBR` qualifier and include it in supported-locale and many-category lists. Explicit 11-locale assertions became 12-locale assertions. No packaging locale restriction was present. PreferencesManager still owns the existing DataStore mirror, AppCompat pre-33 path, Android LocaleManager path, startup migration and system synchronization. No extra manager, preference migration or dependency was introduced.

## Decisions and sources

| Concept | Brazilian decision and context | Evidence |
| --- | --- | --- |
| Knit / purl | `meia` / `tricô`; the activity is also `tricô`, while instructions explicitly say `em meia` or `em tricô`. `liga` is replaced only in the new Brazilian set. | BR-CIRCULO, BR-PINGOUIN, CYC-ABBR |
| Stitch / row / round | `ponto`, `carreira`, `volta`. A geometric chart row remains `linha`; neutral counter progress remains `valor`/`contador`, including crochet and custom labels. | BR-CIRCULO; CounterValueFormatter, MainCounterDisplayText |
| Slip and decreases | `passar sem fazer` / `deslizar`; preserve knitwise/purlwise orientation, yarn position, left/right inclination, and central double decrease. English abbreviation tokens remain searchable. | BR-CIRCULO; CYC-ABBR; CYC-CHART |
| Gauge / sample | `amostra` in the common task names and pattern instructions; `densidade` where comparing numerical gauge differences. Physical swatch dimensions and stitch/row density stay distinct in labels. | BR-PINGOUIN; BR-AMOSTRA; GaugePresentation |
| Cable / garter / seed | `trança`, `cordões de tricô`, `ponto arroz`. Expanded cable descriptions explicitly move 2/3 stitches to an auxiliary needle and preserve front/back hold and knit operation. | BR-CABLE; BR-CIRCULO; CYC-ABBR |
| Cast-on / edge | `montar pontos`, `pontos de borda`; edge input is the total across both edges, outside the repeat. | BR-CIRCULO; CastOnCalculator |
| Yarn / package | `fio`, `novelo`, `lote de tingimento`. Usage records count packages using user-confirmed length/mass, so the new Brazilian usage labels use `novelo` consistently with the estimator and stash. | BR-CIRCULO; BR-PINGOUIN; YarnUsagePresentation |
| Care | Keep `alvejante`, `lavar à mão`, `secar na horizontal`, `passar a ferro`, and process-specific `limpeza a seco`. P/F solvent restrictions and prohibitions are unchanged; no universal laundering advice is added. | BR-CARE; GINETEX; CareSymbol |
| Pattern / motif | Saved/readable craft instructions use `receita`; `padrão` remains valid for an established stitch pattern and `motivo` for repeats. A `modelo online` becomes `receita online`; it still saves metadata, not the page/PDF. | BR-CIRCULO; SavedPatternDetailScreen; WebPatternEditorScreen |
| Body-size identifiers | CYC XS/S/M/L/XL/2XL–5XL stay source identifiers, not asserted Brazilian clothing-size equivalents. Mixed GG/XL labels are aligned to those actual source categories in this new set only. | CYC-SIZE; SizeChartData; SizeChartScreen |
| UI and recovery | `Salvar`, `Excluir`, `Configurações`, `arquivo`, `tela`, `câmera`, `registrar`, `compartilhar`, `backup/restauração`; imperative third-person wording remains consistent. Save failure, cancellation, deletion and replacement consequences are preserved. | Current production callers and independent Brazilian reading |
| Plurals / numbers | Brazilian ICU selects one at 0 and 1, other at 2, many for exact nonzero millions on the tested ICU. Fractional package forms follow the displayed rounded number. Decimal comma and Brazilian grouping/date patterns come from the selected locale. | CLDR; ANDROID; runtime checks |

Source links, accessed 2026-10-19:

- **BR-CIRCULO:** [Círculo, Quimono Salmão](https://www.circulo.com.br/receitas/quimono-salmao): Brazilian abbreviations, purl/knit, central decrease, points/rows, gauge and edge points. Recipe-specific counts are not generalized.
- **BR-PINGOUIN:** [Pingouin, Cachecol Érika](https://blog.pingouin.com.br/cachecol-erika-feito-em-trico-com-o-fio-elis/): independent Brazilian meia/tricô, amostra, carreira and package vocabulary.
- **BR-CABLE:** [Círculo, Cardigã com trança tesouro Fofura, diagram legend](https://www.circulo.com.br/storage/receitas/62ffc4cd2217765115962ffc4cd2217c.pdf): front/back auxiliary-needle position for left/right cables, and cordões de tricô.
- **BR-AMOSTRA:** [Blog by Day, Aula de Tricô nº 05: Amostra](https://blogbyday.euamotrico.com.br/2017/04/aula-de-trico-n-05-amostra.html): search-index passage explicitly discusses sample measurement and blocagem; full-page fetch returned HTTP 429. Used only for the named Brazilian terms, with gauge meaning independently supported by the two accessible manufacturer sources and current calculator source.
- **BR-CARE:** [Círculo, Dicas para cuidar e conservar tecidos](https://www.circulo.com.br/post/dicas-para-cuidar-e-conservar-tecidos-circulo-guia-completo): Brazilian care vocabulary. Product-specific advice is not treated as the definition of every app symbol.
- **CYC-ABBR:** [Craft Yarn Council, knitting abbreviations](https://www.craftyarncouncil.com/standards/knitting-abbreviations): English token/operation identity, independently paired with Brazilian explanations.
- **CYC-CHART:** [Craft Yarn Council, knit chart symbols](https://www.craftyarncouncil.com/standards/knit-chart-symbols): right-side convention and operation distinctions; current ChartSymbolData retains this convention.
- **CYC-SIZE:** [Craft Yarn Council, body sizing](https://www.craftyarncouncil.com/standards/body-sizing): body measurements and source categories; no new recalibration of the already audited shared numeric tables.
- **GINETEX:** [Care symbols](https://www.ginetex.net/GB/labelling/care-symbols.asp): care-process and solvent meanings.
- **CLDR:** [Unicode CLDR 48 plural rules](https://www.unicode.org/cldr/charts/48/supplemental/language_plural_rules.html): Portuguese and European Portuguese are distinct; pt-BR uses the Portuguese cardinal rule.
- **ANDROID:** [Android string resources](https://developer.android.com/guide/topics/resources/string-resource#Plurals): locale-driven quantity selection, separate formatting argument, required other fallback and escaping.

## Batch reading and meaningful contexts

### B01
Navigation and common actions: read all 27 variants. `Zerar` refers to resetting the counter to zero; deletion uses `Excluir`, while unlink/removal stays distinct. Selection heading `Seleção: %d` remains neutral for projects, photos, recipes and yarn cards. Arbitrary names are inserted unchanged, without imposed gender or capitalization. Native language names remain native.

### B02
Projects and folders: 110 variants. Project completion moves the project to completed; reopening does not erase completion history. Folder deletion preserves projects, and no-folder is an organizational view. Bulk quantities retain all three forms and select by affected project count. `Nome` replaces the main counter's `Etiqueta`; the associated custom option is masculine `Personalizado`. No product behavior changes.

### B03
Main counter: 81 variants. Re-read ROWS/ROUNDS/REPEATS/CUSTOM slots in MainCounterDisplayText: `1 carreira`, `2 carreiras`; `1 volta`, `2 voltas`; `1 repetição`, `2 repetições`. A supplied custom label is opaque. Target summaries insert either a localized count phrase or a bare formatted number depending on the project-card context; both fit the retained neutral captions. Step actions insert the step and selected label, not an assumed knitting unit. Project names stay unchanged.

### B04
Extra counters and reminders: 42 variants. `Modelagem`, `Seção de repetição`, linked-main-counter wording and reset/deletion actions match ProjectCounterLogic. `Repetição #2, intervalo 1` selects by the interval argument and does not confuse it with the occurrence. Section progress keeps repeat/total-repeat then position/length placeholders in that order. Chart geometry remains rows/columns rather than blindly converting every line into a worked carreira.

### B05
Sessions, history and Insights: 133 variants. Recovery offers saving, discarding or editing; editing ends the session, and navigation does not start/finish it. Saved sessions and neutral net counter progress are not mislabeled as worked rows. Completed-project totals count completion events, including repeated completion cycles. Duration strings remain h/min; the duration presenter supplies positional numbers after quantity selection. Denominator selects `Ativo 1 de 2 dias`, while active-day accessibility selects the second formatting slot. Chart phrases compose `Tempo diário dedicado a fazer crochê`/`tricotar`/`criar` with period, active-days and duration. Numeric summaries and custom project names do not require gender rewrites.

`relative_time_months_ago` retains all three exact `há %1$d m` variants. Both InsightsProjectRow and InsightsLastWorkedNote call the shared relativeDayText: days 30/59 select and format months 1, 60 selects/formats 2, 90 selects/formats 3. The last-worked context distinguishes month distance from duration. The separate selection and display arguments remain identical; no string-to-plural regression or English fallback was introduced.

### B06
Yarn metadata: 62 variants. `Meus fios` is used consistently in headings, linking and save actions. The project-only note remains distinct from a stash card; saving a copy does not copy private notes or remove the project entry. `Em estoque`, `Em uso`, `Esgotado` describe yarn availability, not project completion. The source-linked card/name semantics were checked in YarnManagementSheet, MyYarnScreen and YarnUsageRow.

### B07
Yarn usage: 68 variants. Planned, allocated and used quantities remain independent. Allocated 100 m / used 125 m gives `Excesso: 25 m`; 100/75 gives `Restante: 25 m`; missing values remain unknown, while zero remains numeric. Deleting usage preserves the yarn card, project note and other data. User-confirmed positive length/weight is required for grams/packages. `Novelo` replaces the European starting set's meada for this package-count UI. Fractional selection uses skeinPluralQuantity after MeasurementNumberFormatter: 0,5 and 1,99 select one in pt-BR; rounded 2 selects other. Existing pt-PT expectations remain meada/meadas and are not rewritten.

### B08
Saved recipes and web metadata: 76 variants. `Receita` consistently describes craft instructions. Read versus offline PDF, availability and original-site actions remain distinct. Library deletion removes its master annotation layer while project PDFs/project overlays are preserved; the long confirmations still state both effects. Unlinking web metadata does not delete project documents. HTTP caution still refers to the connection, not yarn/project linking.

### B09
PDF/image import: 78 variants. `Baixando`, `arquivo`, `câmera`, `Salvar receita` and conversion gerunds are Brazilian. Image indices are displayed as index+1; progress uses completed/current pages and total in the existing order. The 20-page, 25 MiB/image and 200 MiB/session limits, replacement timing and failure preservation remain unchanged. Camera wording remains photo-to-PDF without promising OCR or cloud sync. Ravelry's literal Download PDF matches the external page action.

### B10
Reading assistance and bookmarks: 68 variants. `Acompanhamento`, `Centralizar`, `Salvar primeiro/último valor` follow the actual counter-value calibration. Bookmark, reading guide and row marker remain different concepts. Counter value and PDF page are distinct arguments; pages are displayed 1-based. `%1$d%%` still produces a single percent sign for guide position. Manual navigation pauses follow; annotations are not implied by bookmarks.

### B11
Annotations: 77 variants. Read all 75 annotation-file strings plus their cross-file actions. Library/master notes remain read-only in a project; project notes are the editable overlay. `Marca-texto` names the highlighter rather than a bookmark. The export filename's `-anotado.pdf` refers to the PDF; progress remains completed/total. Failed export preserves the original, and failed text save keeps its retry instruction. Geometric chart rows/columns and axes retain their spatial meaning.

### B12
Photos: 32 variants. `Compartilhar`, `Excluir`, and rename wording match the actual actions. Counter captions remain `Valor`, since photos may belong to crochet/custom-unit projects. Date formatting uses rememberLocaleDateFormat with the configuration locale. Delete quantities and named photo fallbacks were read separately.

### B13
Measurements and gauge: 78 variants. `Amostra` is the Brazilian task/pattern term, `densidade` clarifies a numerical comparison. Stitch-width and row-height axes stay independent. Manual check: 22 stitches/100 mm and 30 rows/100 mm give 66 stitches and 90 rows at 300 mm. GaugePresentation composes localized headings, values, units and warnings for display/copy. Copy does not save notes or project settings. Washed/blocada advice concerns a measured sample, not permission to apply heat to every yarn. Conversions remain exact internally; display rounding does not feed calculation.

### B14
Increase/decrease: 30 variants. Generated K/M1/K2tog tokens are preserved and explained in Brazilian Portuguese. 10 existing points + 3 M1 gives 13; flat work has 9 interior bars, circular work has 10. Three K2tog consume six original points plus four plain points, leaving seven. The displayed × is repetition count. The tip distinguishes flat back-and-forth rows from continuous rounds. English-only regex paste examples stay explicitly English-only.

### B15
Cast-on: 20 variants. `Pontos de borda` is total edges, outside the repeat. Width 20 cm at 22/10 cm gives 44 body points; repeat 6 rounds body to 42, then two total edges give 44. Alternate 48+2 gives 50 and a wider actual result. Imperial 8 inches at 22/4 inches gives 44 body points; +2 edges gives 46. Four inches are 101.6 mm, not 100 mm. Labels preserve actual versus desired width and adjusted-up/down meanings.

### B16
Yarn estimator: 12 variants. 250 m / 100 m per 50 g package gives 2.5 exact packages, three whole packages and 150 g purchased weight. `Peso dos novelos inteiros` remains distinct from the decimal estimate. Dye-lot wording says to consider extra yarn, not always buy one extra package. Formatters receive rememberCurrentLocale and display decimal comma.

### B17
Needles and body sizes: 72 variants. Read current NeedleSizeData and SizeChartData. The existing 152 needle cells and 123 body-measurement cells remain shared source data, not newly translated numbers. This review checks their Brazilian headings, units, category identities and presentation; it does not reopen their accepted numerical audit. Japanese diameters are not invented nearest equivalents. Metric cm body values convert to inches by division by 2.54; 40.5 cm displays as 15,9 inches. Ranges format both ends with the locale. CYC letter identifiers are preserved consistently; they do not claim local Brazilian sizing equivalence. Hand category/size composition remains readable.

### B18
Abbreviations: 157 variants. Independently read every meaning/description against the 77 English token triples in AbbreviationData. Tokens remain English because this screen explicitly teaches English notation. Corrected Brazilian purl, edge, garter, seed, slipped-stitch, cable and row/round expressions preserve each operation. SSK/SKP remain distinct techniques with matching leftward decrease intent; S2KP/CDD centralization is retained. WPI is adjacent wraps per inch without overlap, not ply construction. The eight StandardYarnWeight values remain Ravelry category identifiers, with no invented universal strand-count equivalence. User/imported yarn names and debug sample names are data, not a new translation source.

### B19
Symbols and care: 58 variants. Paired the 24 current ChartSymbolData entries and 20 CareSymbol values with the Brazilian labels. Right-side convention remains explicit; no universal-symbol claim is added. Knit/purl chart semantics reverse on the wrong side. The purlwise slipped-stitch glyph retains yarn behind; the unsupported knitwise-slip resource is retained but not claimed as a displayed glyph. Cables cross 2/2 or 3/3 in the specified direction, not twisted single stitches. Existing care wording was already natural Brazilian Portuguese and was retained, including P/F solvent distinctions. No new glyph rendering campaign was run.

### B20
Settings: 36 variants. Native label `Português (Brasil)` and BCP 47 `pt-BR` remain distinct from `Português`/`pt`. Existing preferences and system-default semantics are preserved. `Vibração ao tocar`, `tela`, `Configurações` and analytics copy use Brazilian vocabulary without changing consent or service claims. Imperial preference describes defaults rather than forcing all calculator units. Analytics excludes project content and remains opt-in.

### B21
Backup: 28 variants. `Backup`, `restauração`, `arquivo` and `criptografado` express the existing local export/replacement workflow. Privacy warning still says unencrypted; restore replaces local content and requires confirmation. Settings/device language, purchases and account sessions do not migrate; active work needs review. Preview counts use invariant labels, date/version slots keep their types. No destructive restore was executed for localization.

### B22
Pro/billing: 69 variants. Trial remains 14 days; one-time purchase has no subscription. Existing content remains usable according to the current Pro model; new-item and advanced-view gates are not described as data deletion. `Meus fios`, `receita`, `tela inicial` and `recurso` match the new Brazilian terms. Price is supplied by billing rather than hardcoded currency. Failed, pending, cancelled and restored purchases remain distinct. No purchase was performed and debug feature availability is not described as paid entitlement.

### B23
Reconciliation: all 1,414 variant rows and 1,350 keys covered. Every one/many/other variant was read; no quantity was dropped to satisfy a test. Aliases, markup, escapes and formatting slots were compared to base resources. `web_pattern_error_designer_too_long` still resolves locally to the 120-character error. No missing key, placeholder mismatch, unexplained alias or malformed XML remains. Independent final rereading covers both adapted and retained text; the diff ledger records only actual differences from pt.

Locale-sensitive production paths were checked: rememberCurrentLocale reads LocalConfiguration, date helpers receive that locale, GaugePresentation and YarnUsagePresentation receive it explicitly, and Insights uses AppCompat/application locales with its existing system-default fallback. Locale.US in gauge parser bridging is for machine-formatted canonical input, not Brazilian display; Locale.ROOT in numeric timer notation and identifiers is deliberate. No new accidental language/number/date fallback was found.

## Validation and completion

See [validation.json](validation.json) for exact commands/results and runtime boundaries. Initial JVM validation found one remaining 11-file assertion; it was updated to 12 without removing its placeholder check. Only the final rerun represents the frozen result.

Runtime integration and content acceptance are recorded separately. No 320 dp, 200% font-scale, combined-width/font, TalkBack or exhaustive screen campaign is included. API levels not actually exercised remain unverified.

Unresolved content issues: none identified after the full review. Content acceptance is **AUDIT_PASS**; the focused integration result is **PASS**.

- Debug resources, app APK and instrumentation APK compiled successfully. Final JVM selection: **84 tests, 0 failures/errors/skips**. Scoped ktlint over all changed/new Kotlin files: PASS.
- API 36 Android emulator: **5 instrumentation tests passed**, covering real preferences observation, selection/System reset, external LocaleManager synchronization, fresh-manager startup, Brazilian resources, dates/numbers, zero/one/multiple/million and fractional package plurals, and unchanged generic pt wording.
- Real UI interaction: selected Português (Brasil), observed Configurações and Brazilian labels, force-stopped and relaunched, verified pt-BR and the checked option; selected Português and observed Definições with exact pt; selected System and observed English with empty application locales. Android Settings → App language → Português → Brasil then produced pt-BR and the checked Brazilian option in KnitTools. Settings application changes apply asynchronously; the confirming read was taken after returning to the app.
- API <33/AppCompat runtime behavior was not exercised; the shared tag logic and architecture were inspected and tested on the JVM. No release artifact, device matrix, purchase, backup restore or exhaustive rendering is claimed. Material-provided accessibility labels are dependency text outside the app-owned content ledger.
- Original eleven locale files reconstruct their entry hashes after removing only the added option line (and normalizing line endings). Original audit evidence, instruction files and historical rendering registries remain unchanged. The temporary read-only emulator was stopped and no task-owned background process remains.

No unfinished requested work remains. Stop here; no commit, push, PR or follow-up audit. Native-speaker review remains **NOT_REQUESTED**; no external human reviewer was contacted. No other audit is reopened.
