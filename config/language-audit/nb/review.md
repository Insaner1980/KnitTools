# Norwegian Bokmål Language Audit (nb)

**AUDIT_PASS — B01–B23 REVIEW_COMPLETE / PASS. Native-speaker review: NOT_REQUESTED.**

## Scope and snapshot

- Assigned boundary: complete Norwegian Bokmål content audit and minimal nb-only corrections, B01–B23, with revised section 16 acceptance. Stop after nb.
- Date: 2026-10-19 (Europe/Helsinki). Branch: codex/audit-korjaukset-20261018. HEAD before/after: e4416c7cf0a7a9ea57006a970307ee2f3d1d228f. No commit, push, PR, other-chat message or delegated agent.
- Read AGENTS.md, CODEX.md, relevant CLAUDE.md wording, revised LANGUAGE_AUDIT.md and shared-fixes.md. No prior nb audit directory existed at entry. Existing shared, English, Finnish and Swedish work was preserved.
- Entry state: [entry-status.txt](entry-status.txt) and SHA-256 of 1,142 tracked/nonignored files in [entry-worktree-hashes.json](entry-worktree-hashes.json). Only the two authorized nb resource files changed among these. Inverting the exact corrections reproduces their entry bytes, including existing nb changes and line endings.
- Final audited identity: [input-hashes.json](input-hashes.json), 455 SHA-256 entries. All 449 inputs in the existing English/shared evidence manifest still match. This supports reuse of unchanged producer evidence, not Norwegian translation approval.
- Independent XML inventory: **1,349 translatable keys / 1,380 variants**: 1,318 strings and 31 plurals with one/other, no arrays. One alias. All nb resource elements have only the name attribute, so translatable/formatting defaults apply. No missing, extra, duplicate, type-mismatched or fallback keys.
- Coverage: **1,626 rows** = 1,380 resource variants + **246 additional records**: 77 abbreviation triples, 8 yarn categories, 38 needle rows, 46 size rows with 123 measurement cells (including range endpoints), 24 active chart symbols, 20 care symbols, 31 composed/context cases, 2 intentionally nontranslatable numeric examples.
- Every Norwegian text and plural variant was read independently. Production callers were inspected when operation, argument meaning or composition mattered. Repeated identical uses share caller references; materially different meanings are separately recorded. Existing inventory batch anchors were discovery aids, reconciled against section 8 and actual nb content.
- [coverage.tsv](coverage.tsv) follows the plan schema. Text cells escape literal tabs/newlines/carriage returns as backslash-t/backslash-n/backslash-r. Primary text is exact current XML; generated records explicitly distinguish source-derived cases from execution. Per-row hashes identify the input version.
- Exclusions: user-authored content; third-party Ravelry pages; internal identifiers/diagnostics/logs; test/demo-only prose. The two numeric UI examples are explicitly included as B23 code_text; T/F are N/A because they contain no lexical term or factual assertion.
- No new rendering, spoken-output, native-speaker review, destructive restore or purchase. Source/context evidence resolved the specific language questions. All V cells are blank, not PASS/N/A. No rendering backlog; 320 dp, 200% font scale and TalkBack were not repeated.

### Resource files

| File | Strings | Plural keys | Variants |
| --- | ---: | ---: | ---: |
| pattern_annotations.xml | 75 | 0 | 75 |
| pattern_reading_assistance.xml | 49 | 0 | 49 |
| reference_strings.xml | 257 | 0 | 257 |
| strings.xml | 937 | 31 | 999 |

The alias web_pattern_error_designer_too_long resolves to web_pattern_error_title_too_long, “Maksimalt 120 tegn”; both callers enforce the same limit. The two shared nontranslatable numeric examples are outside the translation denominator.

Eleven retained keys (12 variants) have no direct main-source caller: size_col_head, size_col_height, size_row_adult_s/m/l/xl, size_row_men_xs_30_32, sym_slip_knitwise_name/desc, yarn_weight_ply_format, insights_rows_count (one/other). All were still read and included; none was deleted or silently excluded.

## Language decisions and glossary

| Concept | Accepted forms and context | Boundary / evidence | Status |
| --- | --- | --- | --- |
| Row / round | pinne in knitting instructions; rad for generic/cross-craft rows; omgang/runde in circular contexts | Valid contextual alternatives retained. Shared row step title also serves crochet. MainCounterDisplayText, MainCounterStepSheet; NO-GLOSSARY/NO-TECH | PASS |
| Stitch / stitch pattern / instructions | maske; mønster; oppskrift | Do not call live knitting loops sting. Existing contextual mønster is not globally rewritten. NO-LANG/NO-GLOSSARY | PASS |
| Knit / purl | rett / vrang; strikk rett / strikk vrang | Strikk alone does not distinguish K from P in paired instructions. NO-TECH/NO-SOLBERG | PASS |
| Repeat value / action | Repetisjon for count/ordinal; Gjenta for command | Counter heading and reminder occurrence are values, not commands. CounterWorkspaceSections/MultiCounterComponents | PASS |
| Gauge / swatch | strikkefasthet, fasthet in context; prøvelapp | Stitch/row density, per10cm/per4in, exact/rounded count distinguished. NO-TECH/NO-SOLBERG; GaugePresentation | PASS |
| Increase / decrease | økning / felling; existing understandable reduksjon retained | Numeric Reduser differs from knitting Fell. M1 uses tråden mellom maskene, not stangen. NO-TECH/NO-SOLBERG | PASS |
| Slip / double decrease | rett/vrang løs av; two together knitwise for S2KP | SSK slips separately; S2KP together. NO-SOLBERG/NO-CDD | PASS |
| Loops / yarn position | fremre/bakre maskebue; foran/bak arbeidet relative to knitter | WYIF/WYIB do not mean fabric wrong/right side. KFB/PFB work the same stitch twice. NO-TECH/NO-CDD | PASS |
| Every other / short rows | annenhver; forkortede pinner | alternative is a false friend; wrapping needs yarn as object. NO-LANG/NO-TECH | PASS |
| Yarn / package | garn, nøste; existing hespe unit | Fractions use plural; thickness category does not specify ply. Whole-package mass is not consumed mass. YarnUsagePresentation/YarnEstimator; SHARED-YARN | PASS |
| Knitting needle | pinne / strikkepinne | Corrections concern knitting-tool definitions; sewing/crochet contexts not mechanically changed. NO-LANG/NO-GLOSSARY | PASS |
| Numbers / units | decimal comma; cm/mm/m/g, t/min; source sizing tokens | Preserve lowercase g, leading 000/00, missing dash; cm/in uses2.54. Source review, not Android-ICU execution | PASS |
| Bokmål alternatives | Ingen abonnement, existing natural inflections and compounds | Both abonnement and abonnementer are valid plurals. No unnecessary Swedish/Danish/Nynorsk replacement or style standardization. NO-LANG | PASS |

## Sources

New Norwegian sources accessed/read 2026-10-19. Shared sources are reused 2026-10-18–19 evidence with matching current input identities; they are not claimed as new web or device runs.

| ID | Publisher / source | Claim and limitations |
| --- | --- | --- |
| NO-GLOSSARY | [DROPS/Garnstudio English–Norwegian glossary](https://www.garnstudio.com/glossary.php?lang=en&langf=en&langt=no&sort=2) | Craft terminology and English-token mapping. Source typos are not copied as grammar authority. |
| NO-TECH | [Yndestrikk, Strikkehjelp](https://www.yndestrikk.no/pages/strikkehjelp) | Norwegian terms, knit/purl, twisted stitches, directional M1 and yarn side. Norwegian section used, not Danish text elsewhere. |
| NO-SOLBERG | [Solberg Spinderi, Forkortelser](https://solbergspinderi.no/wp-content/uploads/2022/06/Forkortelser.pdf) | Separate SSK slips, M1 directions, flat/circular garter, gauge context; pattern-specific conventions can differ. |
| NO-CDD | [Filcolana, Lisa, Norwegian translation by Merete Norheim](https://filcolana.dk/amfile/file/download/file/3318/product/975/) | 2023 edition, centered double decrease slips2 together knitwise; yarn position relative to knitter. |
| NO-LANG | UiB/Språkrådet Bokmålsordboka: [annenhver](https://ordbokene.no/bm/annenhver), [strikkepinne](https://ordbokene.no/bm/strikkepinne), [maske](https://ordbokene.no/bm/maske), [abonnement](https://ordbokene.no/nob/bm/45) | Meanings, compounds and accepted plural alternatives; not native craft-expert sign-off. |
| SHARED-NEEDLES | [CYC needles](https://www.craftyarncouncil.com/standards/hooks-and-needles), [Clover specification](https://clover.co.jp/recipe/takumikikakus.pdf) | Reused system/manufacturer caveat and exact JP0–15 series. All38rows independently read in Norwegian context. |
| SHARED-SIZES | CYC [baby](https://www.craftyarncouncil.com/standards/baby-size-chart), [child/youth](https://www.craftyarncouncil.com/standards/child-youth-sizes), [women](https://www.craftyarncouncil.com/standards/woman-size), [men](https://www.craftyarncouncil.com/standards/man-size), [head](https://www.craftyarncouncil.com/standards/head-circumference-chart), [hand](https://www.craftyarncouncil.com/standards/hand-size-charts) | Reused46rows/123measurement facts; nb headings, arm endpoint, ages, prefixes and unit composition independently reviewed. |
| SHARED-SYMBOLS / SHARED-CARE | CYC [chart symbols](https://www.craftyarncouncil.com/standards/knit-chart-symbols), [care symbols](https://www.craftyarncouncil.com/standards/care-symbols) | Reused drawing evidence for24chart+20care mappings; Norwegian names/descriptions independently read. No new visual inspection claimed. |
| SHARED-YARN | [Brooklyn Tweed specifications](https://brooklyntweed.com/pages/yarn), [NIST unit symbols](https://www.nist.gov/pml/special-publication-811/nist-guide-si-chapter-6-rules-and-style-conventions-printing-and-using) | Reused thickness/ply distinction and case-sensitive g. |
| SHARED-FACTS | [shared-fixes.md](../shared-fixes.md), [English evidence report](../en/review.md) | Unchanged algorithms, ownership/state consequences, glyphs and test receipts. English language acceptance is not Norwegian evidence. |

## Batch tracker

Exact membership, texts, source/caller references, dimension decisions and findings are in coverage.tsv. Complete contextual reading notes are also saved in [batch-reading-notes.json](batch-reading-notes.json).

| Batch | Owned / reviewed rows | Progress | Verdict | Open findings | Contextual reading |
| --- | ---: | --- | --- | --- | --- |

| B01 | 27 / 27 | REVIEW_COMPLETE | PASS | None | Read shared actions and navigation as Bokmaal UI. Preserve KnitTools, short units and native language names; each caller retains the action object. No stylistic casing standardization. |
| B02 | 105 / 105 | REVIEW_COMPLETE | PASS | None | Project/folder create, edit, selection, completion, reactivation and deletion reviewed. User names remain intact. Folder deletion removes membership, not projects; selected project counts and virtual All Projects/Unfiled distinguished. |
| B03 | 71 / 71 | REVIEW_COMPLETE | PASS | None | Selected ROWS/ROUNDS/REPEATS/CUSTOM control count, target, remaining and step labels. Checked values 0/1/2/3 with target2, custom Motiver and steps1/5. Widget target stays unit-neutral. Decrement is numeric Reduser, not a fabric decrease. |
| B04 | 44 / 44 | REVIEW_COMPLETE | PASS | None | Extra-counter cycle position differs from repeat number. Reminder occurrence is argument1, interval argument2 and plural selector; both interval forms read as Repetisjon #2, intervall 1/3. Linked changes follow main counter; section start/end are counter positions. |
| B05 | 138 / 138 | REVIEW_COMPLETE | PASS | None | Reviewed sessions, recovery, time, histories, completion events and Insights. 0/1/59/60/61 minutes compose 0 min/1 min/59 min/1 t/1 t 1 min. Active X of Y uses Y for the plural. Range/verb/day/longest-duration sentence checked for knitting, crochet and mixed activity. Stored historical count units stay neutral; rows/pace suppressed. |
| B06 | 87 / 87 | REVIEW_COMPLETE | PASS | None | Read yarn metadata, physical package quantity, thickness, color, dye lot and 20 care labels. Canonical thickness names stay untranslated; removed ply hints stay absent. Retain Norwegian 30/40/60 degree labels in wash context; P/F are cleaning classes, not solvent names. |
| B07 | 68 / 68 | REVIEW_COMPLETE | PASS | None | Planned, allocated and used remain independent. 400m allocated minus450m used shows Overskredet med: 50 m, unknown allocation stays Ukjent restmengde. Confirmed positive length/weight conversion; 1 hespe/1,5 hesper. Project usage never implies stash subtraction. Saving metadata leaves project notes separate. |
| B08 | 77 / 77 | REVIEW_COMPLETE | PASS | None | Read metadata-only, local PDF and website/Ravelry states separately. Open website is external, Ravelry opens app browser; web metadata is not offline PDF. Reviewed single/bulk/web deletion warnings preserving attached project PDFs/overlays while deleting Library master annotations. Alias resolves to Maksimalt 120 tegn for title and designer. |
| B09 | 77 / 77 | REVIEW_COMPLETE | PASS | None | Read Open/Share incoming PDF, camera/gallery and project-document actions. Photo-to-PDF wording does not promise OCR. 20 images,25MiB each,200MiB total; ordering, primary replacement, final-document removal, cancellation and copy failures preserve their stated objects. |
| B10 | 69 / 69 | REVIEW_COMPLETE | PASS | None | All49 reading-assistance resources plus viewer copy read. Page and counter value distinguished; guides/bookmarks are not annotation marks. Manual movement pauses follow; explicit marker/calibration actions persist anchors. %d%% remains a percent, not extra formatting argument. |
| B11 | 78 / 78 | REVIEW_COMPLETE | PASS | None | All75 annotation-file entries plus markup-mode actions read. Tool names, axes, layer ownership/read-only state, position, export progress/filename and source-preserving failure text checked. Chart grid rows differ from project counter values; C2C is a technique token. |
| B12 | 32 / 32 | REVIEW_COMPLETE | PASS | None | Notes autosave/failure/discard read with editor state. Photo selection, rename, sharing and deletion do not imply deleting project. Recorded photo positions are Verdi rather than assumed rows. Inserted names treated as user content. |
| B13 | 79 / 79 | REVIEW_COMPLETE | PASS | None | Gauge axes and measurement/adjustment fields reviewed separately. 22/10cm x25cm=55,22/4in x8in=44,1in=2.54cm. Output labels distinguish exact count, rounded count and resulting length. Decimal comma/dot accepted, incomplete input retained; copied text uses localized labels and warnings. Shape/ease warnings remain conditional. |
| B14 | 32 / 32 | REVIEW_COMPLETE | PASS | None | Generated K/M1/K2tog notation is deliberate English knitting notation, explained by Norwegian abbreviations; parser examples explicitly English and match regex.96+12=108;96-12=84; M1 consumes no original stitch and K2tog consumes2. Flat reserves an end stitch; circular uses a joining bar. Invalid positive/overflow/dense inputs read against actual branches. |
| B15 | 20 / 20 | REVIEW_COMPLETE | PASS | None | Cast-on uses desired width before total edges.20cm at20/10cm,repeat6,edges2 gives44 stitches and22cm total;12.4 at10/10cm,repeat8 gives16,not8. Both adjusted alternatives include edges. Singular/plural result and metric/4inch basis reviewed. |
| B16 | 14 / 14 | REVIEW_COMPLETE | PASS | None | 450m at200m per50g gives2.25 packages, purchase3,whole-package150g. Labels distinguish these quantities and retain lowercase g. Extra yarn advice is conditional; yarn quantity is not strand thickness. |
| B17 | 157 / 157 | REVIEW_COMPLETE | PASS | None | All38 needle rows and46 size rows/123 measurements reviewed with localized headings. Reused hash-matched factual CYC/Clover validation. Decimal comma, missing-value dash, both range endpoints and cm/2.54 inch conversion checked. Body measurements are not garment dimensions. Child arm endpoint and preteen head category corrected. |
| B18 | 234 / 234 | REVIEW_COMPLETE | PASS | None | All77 notation/meaning/description triples reviewed independently in Norwegian, including expanded search descriptions. Knit/purl, separate SSK slips vs together S2KP slips, cast on/off, yarn side vs fabric side, M1 strand, front/back loops, four/six-stitch cables, short rows and stitch patterns checked against NO sources. Tokens are deliberately English; descriptions are Bokmaal. |
| B19 | 82 / 82 | REVIEW_COMPLETE | PASS | None | All24 active chart name/description/glyph triples reviewed using current CYC right-side mapping and preserved shared glyph validation. Four/six-stitch cables and left/right crossings distinguished. Retained omitted knitwise-slip resource also read, without inventing an active symbol. No new rendering claim. |
| B20 | 36 / 36 | REVIEW_COMPLETE | PASS | None | Settings and native language selection read. nb declared in app language/config. Imperial preference sets defaults with gauge overrides. Consent copy matches release-only predefined PostHog capture, separate Crashlytics, no project contents or cloud sync. Theme/haptics/update actions preserve their objects. |
| B21 | 29 / 29 | REVIEW_COMPLETE | PASS | None | Backup preview and restore copy read against local export/replace behavior. Clear unencrypted warning; settings, purchases/trial,login/widget exclusions and active-session review preserved. Failure copy distinguished from successful replacement. No destructive restore executed. |
| B22 | 67 / 67 | REVIEW_COMPLETE | PASS | None | Pro/trial/purchase/gated-action copy read against current features.14days start explicitly, existing content retained, one free active project, completed projects not counted. New object vs continued editing distinguished. Reminders apply to all counter units. Ingen abonnement retained as a valid zero-ending plural. |
| B23 | 3 / 3 | REVIEW_COMPLETE | PASS | None | Reconciled fresh inventory, all variants, alias, code literals and distinct compositions. Main/release user-facing search excludes logs, internal IDs, test/preview/demo fixtures and third-party page contents. Date/time formatting and numeric/brand/notation exceptions recorded explicitly. No rendering campaign or native-review claim. |

## Findings and minimal corrections

**72 variants across 71 keys**, only in values-nb/strings.xml and values-nb/reference_strings.xml. The other two nb XML files, other locales, shared implementation and tests are unchanged.

Every row below is CONFIRMED_DEFECT / VERIFIED. [corrections.json](corrections.json) contains the full finding template: classification, category/severity/state, file/key/variant/callers, original/replacement, problem/consequence, evidence, necessity/minimality, shared impact, performed checks and remaining uncertainty. IDs are stable within this nb audit.

| ID / batch / variant | Key | Original | Final | Specific defect and necessity |
| --- | --- | --- | --- | --- |
| NB-001 / B03 / value | counter_decrease | Fell | Reduser | Fell commands a knitting decrease, but these buttons decrement numeric counters and yarn quantity. |
| NB-002 / B03 / value | counter_decrease_named | Fell %1$s | Reduser %1$s | A named counter/stitch tracker is decremented; the action does not work a decrease in the fabric. |
| NB-003 / B04 / value | counter_repeat_label | Gjenta | Repetisjon | Gjenta is an imperative, but this is the heading over a stored repeat count, not a repeat action. |
| NB-004 / B04 / one | reminder_repeat_occurrence_format | Gjenta #%1$d, intervall %2$d | Repetisjon #%1$d, intervall %2$d | Gjenta is a command; the first argument is the ordinal occurrence, while the second is the interval. |
| NB-005 / B04 / other | reminder_repeat_occurrence_format | Gjenta #%1$d, intervall %2$d | Repetisjon #%1$d, intervall %2$d | Gjenta is a command; the first argument is the ordinal occurrence, while the second is the interval. |
| NB-006 / B03 / value | widget_name | Radteller | Prosjektteller | Widget also counts rounds, repeats and custom units; its name must not assert that the value is rows. |
| NB-007 / B03 / value | widget_description | Radteller-widget for aktivt prosjekt | Teller-widget for aktivt prosjekt | The widget follows the selected project counter regardless of craft/unit. |
| NB-008 / B03 / value | row_label_with_target | Rad %1$d / %2$d | %1$d / %2$d | Widget supplies count and target but no row-unit guarantee; Rad mislabels rounds/custom values. |
| NB-009 / B05 / value | insights_empty_title | Ingen tid på pinnene ennå. | Ingen arbeidsøkter ennå. | The empty state also serves crochet; needles incorrectly exclude crochet-only use. |
| NB-010 / B06 / value | care_dryclean_p | P-løsemiddel | Rens (P) | P identifies a professional cleaning process category, not an individual solvent named P. |
| NB-011 / B06 / value | care_dryclean_f | F-løsemiddel | Rens (F) | F identifies a professional cleaning process category, not an individual solvent named F. |
| NB-012 / B14 / value | increase_decrease_error_not_enough_bars | Det er ikke nok mellomrom mellom maskene til disse M1-økningene. Reduser antall økninger. | Det er ikke nok mellomrom mellom maskene til disse M1-økningene. Bruk minst to startmasker og reduser antall økninger om nødvendig. | For one starting stitch no positive increase count passes; merely reducing increases cannot resolve the error. |
| NB-013 / B15 / value | desired_width | Ønsket bredde | Ønsket bredde uten kantmasker | Calculator adds edge stitches after calculating the desired width; the input is not full width including edges. |
| NB-014 / B15 / value | tip_pattern_repeat_desc | Hvis mønsteret ditt gjentas hver X masker (f.eks. en flette hver 8. maske), skriver du inn tallet her. Kalkulatoren avrunder maskene i selve mønsteret, før kantmasker, til nærmeste multiplum slik at mønsteret passer. | Hvis mønsteret ditt gjentas hver X. maske (f.eks. en flette hver 8. maske), skriver du inn tallet her. Kalkulatoren avrunder maskene i selve mønsteret, før kantmasker, til nærmeste multiplum slik at mønsteret passer. | Hver X masker is an ungrammatical ordinal interval. The existing example already uses the correct singular ordinal construction. |
| NB-015 / B16 / value | total_weight | Total vekt: %s g | Vekt av hele nøster: %s g | The value is whole-package mass (3 x 50 g = 150 g), not the consumption mass (2.25 x 50 g = 112.5 g). |
| NB-016 / B16 / value | extra_skein_note | Kjøp alltid ett ekstra nøste — innfargingspartier kan variere | Kjøp ekstra garn ved behov — sjekk at innfargingspartiene er like | An unconditional extra package is not warranted by every project; calculator already rounds packages up. Retain relevant dye-lot advice. |
| NB-017 / B17 / value | tip_needle_sizes_desc | Metrisk (mm) er den universelle standarden. US-størrelser bruker et nummersystem (0–50). UK/Canada bruker et annet nummersystem. Japanske størrelser har sin egen nummerering. Sjekk alltid mm-størrelsen for å være sikker. | Nummereringen varierer mellom størrelsessystemer og produsenter. Sammenlign diameteren i millimeter (mm), og sjekk strikkefastheten. | US numbering is not bounded to 0–50; manufacturer/system variation prevents a universal equivalence claim. |
| NB-018 / B19 / value | chart_symbols_disclaimer | Symbolene følger standardkonvensjoner. Sjekk alltid symbolforklaringen i oppskriften. | Symbolene viser maskene sett fra rettsiden av arbeidet. Sjekk alltid symbolforklaringen i oppskriften. | The renderer adopts the CYC right-side convention; the previous universal-standard claim omitted that necessary viewing convention. |
| NB-019 / B20 / value | tip_imperial_units_desc | Når dette er aktivert, bruker alle kalkulatorer tommer i stedet for centimeter og yard i stedet for meter. Fastheten måles da per 4 tommer i stedet for per 10 cm. | Angir tommer og yard som standardenheter i kalkulatorene, med strikkefasthet per 4 tommer i stedet for per 10 cm. Du kan endre enhetene i Mål og strikkefasthet. | The preference supplies defaults; Measurements and Gauge permits independent unit/basis changes. The old all-calculators claim is false. |
| NB-021 / B17 / value | size_col_arm | Arm | Armlengde til armhulen | The child/youth values measure wrist-to-underarm length, not unspecified arm size/circumference. |
| NB-022 / B17 / value | size_row_tween | Yngre tenåring | Barn før tenårene | Tween is the preteen category, whereas yngre tenåring assigns the head range to teenagers. |
| NB-023 / B18 / value | abbr_rs_desc | Den offentlig vendte siden av arbeidet | Utsiden av arbeidet | Offentlig vendte is a literal public-facing calque, obscuring the intended outside/right side of fabric. |
| NB-024 / B18 / value | abbr_ws_desc | Den private/baksiden av arbeidet | Baksiden av arbeidet | Private is not a fabric-side designation; remove the misleading alternative. |
| NB-025 / B18 / value | abbr_kwise_desc | Sett inn nålen som om den skulle strikkes | Stikk pinnen inn som om du skal strikke rett | The old sentence says the needle would be knitted and omits the knitwise-versus-purlwise distinction. |
| NB-026 / B18 / value | abbr_pwise_desc | Stikk inn nålen som om den skulle strikkes vrang | Stikk pinnen inn som om du skal strikke vrang | The pronoun in the old sentence refers to knitting the needle instead of inserting it purlwise. |
| NB-027 / B18 / value | abbr_skp_desc | Venstrestilt nedgang, alternativ til SSK | Venstrestilt felling, alternativ til SSK | Nedgang is a decline, not the established knitting operation felling. |
| NB-028 / B18 / value | abbr_k3tog_desc | Høyrestilt dobbel nedgang | Høyrestilt dobbel felling | Nedgang fails to name the knitting operation in this technique definition. |
| NB-029 / B18 / value | abbr_sk2p_desc | Venstrestilt dobbel nedgang | Venstrestilt dobbel felling | Nedgang fails to name the knitting operation in this technique definition. |
| NB-030 / B18 / value | abbr_s2kp_desc | Sentrert dobbel reduksjon | Sentrert dobbel reduksjon: ta to masker rett løs av sammen, strikk én rett og trekk de to løse maskene over | The meaning line leaves slip orientation/togetherness unspecified; separate slips can produce a different decrease. |
| NB-031 / B18 / value | abbr_sk2p_meaning | Ta 1 løs, strikk 2 rett sammen, stikk den løse masken over | Ta 1 løs, strikk 2 rett sammen, trekk den løse masken over | Stikk tells the reader to insert/stab; the intended step is passing the slipped stitch over. |
| NB-032 / B18 / value | abbr_m1_desc | Plukk opp stangen mellom maskene og strikk gjennom den bakre løkken for å øke | For en venstrelent M1: plukk opp tråden mellom maskene forfra og strikk den vridd rett. Følg oppskriften hvis den angir en annen metode. | Stangen mistranslates the yarn strand; back-loop alone omits lift orientation and presents M1L as every possible M1. |
| NB-033 / B18 / value | abbr_m1l_desc | Økning til venstre ved å bruke stangen mellom maskene | Økning til venstre ved å bruke tråden mellom maskene | Stangen describes a rigid bar, not the yarn strand used for M1L. |
| NB-034 / B18 / value | abbr_m1r_desc | Høyrelent øk med stangen mellom maskene | Høyrelent økning med tråden mellom maskene | Øk is an imperative where a noun is required, and stangen mistranslates the strand. |
| NB-035 / B18 / value | abbr_kfb_desc | Strikk foran og bak i samme maske for å øke | Strikk rett i fremre og bakre maskebue i samme maske for å øke | The instruction must specify the two loops of the same stitch and knit operation; front/back alone can mean fabric faces. |
| NB-036 / B18 / value | abbr_pfb_desc | Stikk vrang foran og bak i samme maske for å øke | Strikk vrang i fremre og bakre maskebue i samme maske for å øke | Stikk is not strikk; identify the same-stitch loops for the purl increase. |
| NB-037 / B18 / value | abbr_sl1k_desc | Ta en maske som om du skulle strikke | Ta én maske rett løs av | Som om du skulle strikke does not distinguish knitwise from purlwise; rett is essential. |
| NB-038 / B18 / value | abbr_sl1p_desc | Ta en maske som om den skulle strikkes vrang | Ta én maske vrang løs av | Ta en maske alone omits that it is slipped without working; name the actual action. |
| NB-039 / B18 / value | abbr_eor_desc | Arbeid på alternative rader | Arbeid på annenhver rad | Alternative means alternative choices, not every other row. |
| NB-040 / B18 / value | abbr_wyif_desc | Hold garnet foran (vrangsiden) av arbeidet | Hold garnet foran arbeidet, på siden som vender mot deg | Yarn front is relative to the knitter, not necessarily the wrong/purl side. |
| NB-041 / B18 / value | abbr_wyib_desc | Hold garnet til baksiden (strikkesiden) av arbeidet | Hold garnet bak arbeidet, på siden som vender fra deg | Yarn back is relative to the knitter, not necessarily the knit/right side. |
| NB-042 / B18 / value | abbr_wt_desc | Pakk neste maske og snu arbeidet for å forme korte rader | Legg garnet rundt neste maske og snu arbeidet for å forme forkortede pinner | Pakk neste maske omits what is wrapped around the stitch; korte rader does not clearly name partial-row shaping. |
| NB-043 / B18 / value | abbr_st_st_desc | Strikk på retten, vrang på vrangen (eller strikk hver runde rundt) | Strikk rett på retten, vrang på vrangen (eller strikk rett hver runde rundt) | Strikk alone means knitting generically; it does not tell a beginner to work knit stitches as opposed to purl. |
| NB-044 / B18 / value | abbr_rev_st_st_desc | Vrang på retten, strikk på vrangen | Vrang på retten, rett på vrangen | Strikk is generic; reverse stockinette requires knit stitches on the wrong side. |
| NB-045 / B18 / value | abbr_g_st_desc | Strikk hver rad (eller strikk/vrang vekslende runder) | Strikk rett hver rad (eller rett/vrang vekslende runder) | The flat and circular garter instructions lack the essential knit operation. |
| NB-046 / B18 / value | abbr_rem_desc | Sting igjen på pinnen | Masker igjen på pinnen | Sting refers to sewing stitches here; the live knitting loops are masker. |
| NB-047 / B18 / value | abbr_foll_meaning | Følger | Følgende | Følger is a finite verb; the token identifies the following/next instruction. |
| NB-048 / B19 / value | sym_knit_name | Strikk | Rett | Strikk is the craft/action generically; this symbol denotes a knit stitch. |
| NB-049 / B19 / value | sym_knit_desc | Strikk på retten, vrang på vrangen | Rett på retten, vrang på vrangen | Missing rett obscures the knit/purl contrast defining the two sides. |
| NB-050 / B19 / value | sym_purl_desc | Vrang på retten, strikk på vrangen | Vrang på retten, rett på vrangen | The wrong-side operation is knit, not unspecified strikking. |
| NB-051 / B19 / value | sym_knit_tbl_desc | Strikk gjennom den bakre løkken (vridd) | Strikk rett gjennom den bakre løkken (vridd) | The knit-tbl operation requires rett; generic strikk does not distinguish purl-tbl. |
| NB-052 / B19 / value | sym_slip_knitwise_desc | Skyv masken som om den skulle strikkes | Ta masken rett løs av | The retained resource omits the knitwise distinction; it remains reviewed even though the symbol entry is currently omitted. |
| NB-053 / B19 / value | sym_sk2p_desc | Ta 1 løs, 2 r sammen, stikk den løse masken over | Ta 1 løs, 2 r sammen, trekk den løse masken over | Stikk is the wrong action for passing the slipped stitch over. |
| NB-054 / B19 / value | sym_m1l_desc | Gjør 1 venstre - venstrevendt økning | Øk én maske mot venstre - venstrevendt økning | Gjør 1 venstre is a broken literal translation and does not name the increase action. |
| NB-055 / B19 / value | sym_lifted_inc_left_name | Lifted Inc. Venstre | Løftet økning mot venstre | Lifted Inc. is untranslated English prose, not a canonical symbol token like M1L. |
| NB-056 / B19 / value | sym_lifted_inc_right_name | Løftet Inc. Høyre | Løftet økning mot høyre | Inc. is untranslated English prose, not a canonical symbol token. |
| NB-057 / B19 / value | sym_lifted_inc_right_desc | Høyre-lent løftet økning | Høyrelent løftet økning | Høyre-lent incorrectly splits this compound adjective. |
| NB-058 / B18 / value | abbr_sts_desc | En eller flere løkker på nålen | En eller flere løkker på pinnen | This knitting definition refers to a knitting needle, not a sewing needle or crochet hook; use the established tool term pinne. |
| NB-059 / B18 / value | abbr_co_desc | Lag innledende masker på nålen | Lag innledende masker på pinnen | This knitting definition refers to a knitting needle, not a sewing needle or crochet hook; use the established tool term pinne. |
| NB-060 / B18 / value | abbr_pm_desc | Plasser en maskemarkør på nålen | Plasser en maskemarkør på pinnen | This knitting definition refers to a knitting needle, not a sewing needle or crochet hook; use the established tool term pinne. |
| NB-061 / B18 / value | abbr_sm_desc | Flytt markøren fra venstre til høyre nål | Flytt markøren fra venstre til høyre pinne | This knitting definition refers to a knitting needle, not a sewing needle or crochet hook; use the established tool term pinne. |
| NB-062 / B18 / value | abbr_circ_desc | To nålespisser koblet sammen med en kabel | To pinnespisser koblet sammen med en kabel | This knitting definition refers to a knitting needle, not a sewing needle or crochet hook; use the established tool term pinne. |
| NB-063 / B18 / value | abbr_lh_desc | Venstre nål eller venstre side | Venstre pinne eller venstre side | This knitting definition refers to a knitting needle, not a sewing needle or crochet hook; use the established tool term pinne. |
| NB-064 / B18 / value | abbr_rh_desc | Høyre nål eller høyre side | Høyre pinne eller høyre side | This knitting definition refers to a knitting needle, not a sewing needle or crochet hook; use the established tool term pinne. |
| NB-065 / B19 / value | sym_yarn_over_desc | Vikle garnet rundt nålen for å lage et hull | Vikle garnet rundt pinnen for å lage et hull | This knitting definition refers to a knitting needle, not a sewing needle or crochet hook; use the established tool term pinne. |
| NB-066 / B03 / value | counter_step_rows_title | Pinner per trykk | Rader per trykk | The ROWS choice also serves crochet, where pinner names a knitting needle/worked needle. Rader preserves the selected unit for both crafts. |
| NB-067 / B18 / value | abbr_ktbl_meaning | Strikk gjennom bakre løkke | Strikk rett gjennom bakre løkke | Ktbl specifies knit through the back loop, not an unspecified knitted operation. |
| NB-068 / B18 / value | abbr_ktbl_desc | vridd strikket maske | Vridd rettmaske | The definition must distinguish the knit stitch from the adjacent twisted purl entry. |
| NB-069 / B19 / value | sym_knit_tbl_name | Strikk TBL | Rett TBL | The chart name denotes the knit stitch, not knitting in general, opposite Vrang TBL. |
| NB-070 / B19 / value | sym_kfb_desc | Strikk foran og bak - øk | Strikk rett i fremre og bakre maskebue i samme maske - øk | The two loops belong to the same stitch; front/back without this can describe sides of the fabric. |
| NB-071 / B22 / value | pro_prompt_reminders_title | Legg til en radpåminnelse med Pro | Legg til en tellerpåminnelse med Pro | The gate is shown for any main-counter unit, including rounds/custom values, not only rows. |
| NB-072 / B22 / value | pro_prompt_reminders_body | En ny radpåminnelse krever Pro. De eksisterende påminnelsene fortsetter å virke og kan redigeres. | En ny tellerpåminnelse krever Pro. De eksisterende påminnelsene fortsetter å virke og kan redigeres. | The gate is shown for reminders on all selected main-counter units, not only rows. |
| NB-073 / B22 / value | pro_group_workflow_body | Bruk den kompakte repetisjonstelleren, ekstra-, repetisjons-, formings- og seksjonstellere, radpåminnelser, mønsterbilder som PDF og startskjermwidgeten. | Bruk den kompakte repetisjonstelleren, ekstra-, repetisjons-, formings- og seksjonstellere, tellerpåminnelser, mønsterbilder som PDF og startskjermwidgeten. | The marketed reminder function follows the main counter regardless of unit; replace only radpaaminnelser. |

Primary categories: CONTEXT 19, FACT 5, LANGUAGE 5, TERM 43. Severity: S1 1, S2 68, S3 3. All72 VERIFIED; none OPEN. These count changed variants, not unique concepts.

### Retained wording and rejected preference

- NB-020 — NOT_A_DEFECT / NOT_APPLIED, S4: one_time_purchase remains “Engangskjøp. Ingen abonnement.” Bokmålsordboka permits both plural forms; the proposed plural-only replacement was discarded and entry bytes restored. The ID gap is deliberate and does not count as a correction.
- Existing rad/pinne/runde/omgang alternatives stay when correct in context. No global terminology or title-case rewrite.
- Washing labels with30/40/60° remain understandable in washing context; English C-label edits were not mechanically copied. P/F solvent-name errors required separate nb fixes.
- Existing natural package words, intentional English notation, canonical yarn labels and user-authored names remain.

## Shared handoffs and other-language impact

**No new shared defect or unresolved handoff.** Independently reconciled against Norwegian expression:

- Selected counter/custom unit and neutral widget/history behavior; numeric decrement vs fabric decrease; historical rows/pace remain hidden when units cannot be known.
- LA-en-B05-01 bounded session-zone dates: both insights_days_meta_format variants, three chart descriptions and range composition. Source-derived Norwegian cases: “Aktiv 1 av 1 dag”, “Aktiv 2 av 2 dager”, future-zone case “Aktiv 0 av 1 dag” with non-reversed Oct2026 range. Shared Kotlin regression evidence supports the producer; this is not new nb device execution.
- M1 available-bar and minimum-start constraints, exact repeat rounding before edges, whole-package weight and lowercase g, needle values, all size ranges and categories.
-24chart/20care mappings; PDF source/deletion/export consequences; folder/project/yarn ownership; consent and Pro gating.
- Earlier English supplementary rendering gaps or repaired test-harness problems are not copied as Norwegian language blockers. No specific unresolved Norwegian comprehension question remains.

## Validation log

| Date / input | Check | Actual result | Evidence and limitation |
| --- | --- | --- | --- |
|2026-10-19, intermediate corrections | ./gradlew.bat :app:processDebugResources --no-daemon --console=plain | BUILD SUCCESSFUL,56s | [First log](resource-processing.log); Android resources, not JVM/UI tests |
|2026-10-19, final resources | Same command after final8variant changes | BUILD SUCCESSFUL,14s;19tasks:7executed/12up-to-date | [Final log](resource-processing-final.log) |
|2026-10-19, final inputs | Python XML/coverage/hash assertions | PASS | FourXML parsed; key/type/variant/duplicate parity;31one/otherpairs; resolved alias;1380placeholder signatures;72fix arguments;1626unique coverage IDs; every variant accounted for. [validation.json](validation.json) |
|2026-10-19, final inputs | SHA-256 preservation reconciliation | PASS |1142entryfiles: only2authorizednbXMLchanged; inverse fixes recover entry bytes.455finalinputs and449/449reusedinputs match |
|2026-10-19 | git diff --check, scoped to nb resources/records | PASS | Existing CRLF-to-LF-on-future-Git-write notices only; no whitespace errors; original bytes reconstruct |
| Reused2026-10-18–19 | Existing shared test receipts | Reused, NOT rerun | Shared report records278JVM checks and16distinct instrumentation methods, plus later107JVM tests in11suites for B05-01/related logic. Producer/drawing evidence, not Norwegian linguistic/rendering approval. Historical failures remain in original reports |
|2026-10-19 | New runtime / rendering | NOT_RUN | No named device-only language question; no screenshot matrix, spoken-output or excluded layout checks |

Technical checks establish valid resources and preserved work. Independent Norwegian reading, sources and caller/composition inspection establish content acceptance. Coverage-generation assertions corrected record/path/batch mistakes before final evidence was saved; they are not application runtime tests.

## Native-speaker review

- **NOT_REQUESTED**. No reviewer identity or craft-expert sign-off claimed.
- No specific unresolved question identified; this is not a guarantee against undiscovered issues.

## Closeout

- Current coverage:1349/1349translatablekeys,1380/1380variants,246/246additionalrecords; all B01–B23complete. Unassigned/unexplained:0.
- T/F:1624PASS and2NOT_APPLICABLE for numeric examples. L/C/R:1626PASS. V:1626blank, no new rendering claim.
-72variant corrections/71keys VERIFIED; no confirmed remaining defect, specific unresolved language question or shared handoff.
- **AUDIT_PASS under revised section16**, native review NOT_REQUESTED. Language content acceptance, not exhaustive UI certification.
- Both task-owned single-use Gradle runs exited. No emulator/server/watcher or temporary background process remains from this task.
- Unrelated work preserved; no commit/push/PR, no other language or rendering task started.

## Resume point

- Last completed: B23 coverage/context/format/preservation and section16 acceptance reconciliation.
- Unfinished assigned work: none.
- No next task is scheduled. Reopen only a specific reported issue or content affected by an input change after these hashes.
