# KnitTools Language Audit

## 1. Purpose and current status

This document records the completed Android language-content audit campaign and retains its execution policy: knitting terminology, factual accuracy, natural language, and correct meaning in context. The campaign closeout below reconciles existing independent locale decisions and the verified narrow month-resource migration; it is not a new full audit. The retained procedures and start prompt apply only to separately assigned future work, not to an outstanding campaign task.

The audit must establish that app-owned text:

- Uses established knitting and, wherever applicable, crochet terminology.
- Describes techniques, measurements, calculations, and app behavior correctly.
- Reads naturally in the target language and selected language variety.
- Remains correct in every actual calling context, including formatted and quantity-dependent text.
- Preserves resource/format correctness after authorized corrections, with targeted runtime inspection only when needed to resolve a concrete language concern.

**Minimal correction is the default.** Keep existing wording when it is correct, natural, and understandable. Alternative phrasing or reviewer preference is not a defect. Every proposed correction must explain the specific problem, its effect on meaning or comprehension, and why the change is necessary. Do not normalize all wording to impose a new style.

**Plan prepared:** 2026-10-18, Europe/Helsinki.

**Historical preparation snapshot:** `e4416c7c`, branch `codex/audit-korjaukset-20261018`; the working tree was clean before this document was created.

**Policy revised:** 2026-10-19, Europe/Helsinki. Language-content acceptance is separate from supplementary rendering coverage.

**Campaign closeout:** 2026-10-19, Europe/Helsinki. **Campaign status: COMPLETE — language content.** All eleven locale reports record **AUDIT_PASS** for B01-B23: en, fi, sv, nb, da, de, nl, fr, it, es and pt. Their latest decisions and closeouts report no confirmed remaining content defect, specific unresolved language question or blocking shared dependency. Native-speaker review remains **NOT_REQUESTED** for all eleven.

The historical English and Spanish `REVIEW_COMPLETE / OPEN` decisions are superseded by their explicit verified closeouts: **LA-en-B05-01 VERIFIED** and **LA-es-B05-58 VERIFIED**. Older shared `LOCALE_RECHECK_REQUIRED` labels describe the handoff at that time; later independent locale reviews reconciled the affected meanings. Section 17 records the current decisions without rewriting those historical reports.

At the original eleven-language closeout, resources contained **1,349 translatable keys per locale**. The verified `relative_time_months_ago` string-to-plurals migration preserves key totals and adds **15 variants** across the campaign: **1,381 translatable variants** in each of en/fi/sv/nb/da/de/nl and **1,413** in each of fr/it/es/pt. The two default-only nontranslatable numeric examples are separate. Sections 4.4 and 17 distinguish current inventory from original audit counts and preserve the earlier evidence identity.

**Separately authorized pt-BR addition (2026-10-19): COMPLETE.** Brazilian Portuguese is now the twelfth selectable language, alongside unchanged generic `pt`. Its independent [content review](config/language-audit/pt-BR/review.md) records **AUDIT_PASS**, with native review **NOT_REQUESTED**. Current inventory is **1,350 translatable keys per locale**, **1,382 variants** per two-form locale and **1,414** per three-form locale; see section 4.5. Focused integration checks are separate from language acceptance.

English supplementary rendering remains **1,858 PASS / 231 BLOCKED (unverified) / 226 NOT_APPLICABLE**. All 15 frozen Insights cases and both rendering registries remain unchanged. The 231 cases are supplementary coverage, not unfinished language-audit obligations. Completion concerns language content, **not exhaustive UI certification or native craft-expert sign-off**. No further campaign action is required; do not resume rendering or start another audit automatically.

## 2. Authorization and scope boundaries

The earlier campaign-closeout documentation task authorized editing only LANGUAGE_AUDIT.md and excluded app/resource/test edits, evidence changes, emulator/build/runtime execution and further audits. The subsequent explicit pt-BR implementation request supersedes those earlier task limits only for the new locale, its existing selection architecture, focused tests, native option labels and this summary. It does not authorize reopening other audits. The reusable prompt in section 18 applies only when the user assigns another bounded audit task. A review-only request remains read-only.

| Area | Rule |
| --- | --- |
| Task ownership | One explicitly bounded task per chat. Own only its assigned items in one language and their evidence; later tasks may continue the same language. No automatic task per screen. |
| Shared implementation | Inspect shared code to establish meaning. Record necessary shared code/data/test changes as explicit handoffs; do not silently expand a language edit into an architectural or cross-language change. |
| Existing work | Inspect branch, status, and relevant diffs before editing. Preserve all existing changes. Never reset, clean, stash, or overwrite another chat's work. |
| Branches | Follow `AGENTS.md`: changes belong on a feature branch. Do not switch branches underneath another active chat using the same checkout. A separate chat is not a separate filesystem. |
| Publication | Do not commit or push without the user's explicit request. Never create, open, submit, or merge a PR. |
| Product behavior | Preserve behavior and UI structure unless a separately authorized fix requires a change. Do not make a misleading claim true by adding a new feature. |
| Dependencies | No dependency upgrades, schema migrations, navigation changes, or new localization framework. The separately requested pt-BR addition is authorized; other regional additions are not. |
| Existing UI checks | Do not rerun 320 dp width, 200% font scale, their combination, or TalkBack unless the user explicitly requests those specific checks. Inspect accessibility wording as text without claiming a new TalkBack run. |
| Quality wrappers | Do not run the user's `lc`, `sc`, or other restricted wrappers. Use relevant direct Gradle tasks where permitted. |
| User data | Use synthetic examples. Do not change real projects, notes, PDFs, photos, purchases, or settings merely to obtain audit evidence. |
| External services | No Numeris access, Ravelry account actions, purchases, external uploads, or messages to other people are needed for this audit. |
| Temporary processes | Track and stop only temporary processes started for this work. Prefer commands that exit naturally. |

Read current `AGENTS.md`, `CODEX.md`, and the wording rules in `CLAUDE.md` at execution time. Current user instructions take precedence. If an instruction contradicts implementation, record the contradiction and determine which text/behavior is intended; do not treat either an old document or the current implementation as proof of a knitting fact.

## 3. One bounded task per project chat

### 3.1 Execution order and ownership

Use the following default order. English goes first to identify shared factual and product-meaning problems early. Finnish follows as the first localized application of the process. Each later language still requires independent evidence; an approved English sentence is not proof that its translation is correct.

| Order | Language scope across tasks | Locale | Owned resource directory | Language-specific focus | Recorded progress |
| --- | --- | --- | --- | --- | --- |
| 1 | English language audit | `en` | `app/src/main/res/values/` | Declare the English convention used by instructional text; distinguish US/UK terminology where relevant. Default resources also affect fallback. | [AUDIT_PASS](config/language-audit/en/review.md); B01-B23 complete; native review NOT_REQUESTED |
| 2 | Finnish language audit | `fi` | `app/src/main/res/values-fi/` | Established Finnish craft terminology, natural inflection, and the required `ohje` / `verkko-ohje` product wording. | [AUDIT_PASS](config/language-audit/fi/review.md); B01-B23 complete; native review NOT_REQUESTED |
| 3 | Swedish language audit | `sv` | `app/src/main/res/values-sv/` | Contextual row/round wording, stitch versus stitch-pattern meanings, and natural compounds. | [AUDIT_PASS](config/language-audit/sv/review.md); B01-B23 complete; native review NOT_REQUESTED |
| 4 | Norwegian Bokmal language audit | `nb` | `app/src/main/res/values-nb/` | Bokmal throughout; check for unintended Danish, Swedish, or Nynorsk forms. | [AUDIT_PASS](config/language-audit/nb/review.md); B01-B23 complete; native review NOT_REQUESTED |
| 5 | Danish language audit | `da` | `app/src/main/res/values-da/` | Danish craft usage and grammar; distinguish needle, worked row, and circular round by context. | [AUDIT_PASS](config/language-audit/da/review.md); B01-B23 complete; native review NOT_REQUESTED |
| 6 | German language audit | `de` | `app/src/main/res/values-de/` | Craft compounds, noun capitalization, case, and consistent user address. | [AUDIT_PASS](config/language-audit/de/review.md); B01-B23 complete; native review NOT_REQUESTED |
| 7 | Dutch language audit | `nl` | `app/src/main/res/values-nl/` | Natural craft vocabulary for rows, rounds, needles, and gauge in their actual contexts. | [AUDIT_PASS](config/language-audit/nl/review.md); B01-B23 complete; native review NOT_REQUESTED |
| 8 | French language audit | `fr` | `app/src/main/res/values-fr/` | Craft vocabulary, gender/number agreement, elision, and punctuation spacing. | [AUDIT_PASS](config/language-audit/fr/review.md); B01-B23 complete; native review NOT_REQUESTED |
| 9 | Italian language audit | `it` | `app/src/main/res/values-it/` | Distinguish knitting and crochet uses of rows/rounds; verify action wording and agreement. | [AUDIT_PASS](config/language-audit/it/review.md); B01-B23 complete; native review NOT_REQUESTED |
| 10 | Spanish language audit | `es` | `app/src/main/res/values-es/` | Existing Spanish variety preserved; no regional normalization. | [AUDIT_PASS](config/language-audit/es/review.md); B01-B23 complete; native review NOT_REQUESTED |
| 11 | Portuguese language audit | `pt` | `app/src/main/res/values-pt/` | Establish regional usage; do not mix European and Brazilian conventions without a documented reason. | [AUDIT_PASS](config/language-audit/pt/review.md); B01-B23 complete; native review NOT_REQUESTED |
| 12 | Separately requested Brazilian Portuguese implementation and audit | `pt-BR` | `app/src/main/res/values-pt-rBR/` | Independent Brazilian craft terminology, full content and regional locale integration. | [AUDIT_PASS](config/language-audit/pt-BR/review.md); B01-B23 complete; native review NOT_REQUESTED |

At the original campaign baseline, the repository declared generic `en`, `es`, and `pt` tags without separate regional resource sets. This is a verified configuration fact, not an approved editorial decision about regional vocabulary. Inspect existing usage first. Preserve a coherent existing variety when evidence supports it; ask only if conflicting choices would materially change the language's intended audience. That earlier audit did not authorize `pt-BR`, `pt-PT`, `en-GB`, or other regional additions. The later explicit request authorizes `pt-BR`; `pt` remains unchanged and no other regional locale is added.

The directory boundary is an ownership boundary, not permission to edit every file there. For example, the English chat must not change colors or themes in `values/`.

### 3.2 How the chats share progress

1. The project-level coordination chat maintains this plan, the language summary, and shared handoffs.
2. Each language chat reads this plan, the current shared handoffs, and its existing language evidence before starting or resuming.
3. Each task covers only its assigned keys, batches, questions, or acceptance reconciliation. Complete B01-B23 cumulatively across explicitly assigned tasks; do not automatically create tasks per screen or batch.
4. Each language chat records its work in the files specified in section 12. Conversation messages alone are not the audit record.
5. At its assigned boundary, the task stops and records exact completed coverage, findings, checks actually run, unresolved items, and a resume point. A suggested next task is not authorization to begin it.
6. The coordination chat reconciles those files and updates only the appropriate summary rows. Do not infer completion from a chat title or a final statement without its evidence.

**Default to sequential execution of user-assigned tasks.** The language order above is a recommendation, not an instruction to continue automatically. A new task for the same language reuses its saved records. Later languages independently review their content; they do not repeat the English rendering campaign. If the user chooses concurrent language chats, their locale files must remain disjoint, shared files have one owner, and a check is invalid if its inputs change during execution. Do not launch parallel Gradle checks against the same outputs.

Separate chats do not authorize creating chats automatically, sending messages to them, or spawning agents. Follow the user's actual coordination instructions.

### 3.3 Shared findings and invalidation

Use a shared handoff when the problem concerns a calculator, a reference-data value, a symbol drawing, a shared formatter, a default-language meaning, or a resource needed by multiple languages.

- Record the exact file/member, evidence, expected meaning, affected resource keys, affected languages, proposed smallest fix, and required validation.
- Keep the affected content item `PENDING` or `FAIL` while a specific unresolved language concern or confirmed defect prevents acceptance. Continue independent work only within the assigned task boundary.
- Another language chat must not make a competing fix to the shared file.
- After the owning chat fixes a shared issue, each affected language rechecks the relevant keys and contexts. Do not rerun unrelated completed batches.
- A later discovery can invalidate an earlier language's affected items. Mark their current review `PENDING` with the changed input and reason; retain the previous result as history.
- Shared factual evidence can be referenced by several languages. Each language must still prove that its wording expresses the verified fact accurately.

## 4. Historical inventory baseline and current reconciliation

### 4.1 Locale and resource authorities

Sections 4.1-4.3 preserve the 2026-10-18 preparation snapshot; their counts and discovery instructions are historical, not remaining campaign obligations. Current counts are in section 4.5; section 4.4 retains the intervening eleven-language closeout.

The following files were inspected during preparation:

- `app/src/main/res/xml/locales_config.xml`: eleven declared languages.
- `app/src/main/java/com/finnvek/knittools/data/datastore/AppLanguage.kt`: language selection, including `SYSTEM` and the same eleven language tags.
- All text-bearing XML files in the eleven relevant `values` directories.
- `app/src/test/java/com/finnvek/knittools/ProjectSourceFiles.kt`: configured-directory discovery and the narrower helper that discovers only files named `strings.xml`.
- `app/src/test/java/com/finnvek/knittools/ReferenceResourceLocalizationTest.kt`: all-file key/type parity and alias/plural structural checks.

Do not inventory only `strings.xml`. Do not infer a missing translation from a missing same-named file: the English, Finnish, and other language directories distribute the same keys differently.

### 4.2 Text-bearing files at the inspected revision

All paths in this table are relative to `app/src/main/res/`.

| Language group | File | String keys | Plural keys | Notes |
| --- | --- | ---: | ---: | --- |
| English | `values/strings.xml` | 1,269 | 31 | Includes two non-translatable numeric strings and the default annotation/reference text. |
| English | `values/pattern_reading_assistance.xml` | 49 | 0 | Reading assistance and bookmark text. |
| Finnish | `values-fi/strings.xml` | 1,192 | 31 | Includes reference text. |
| Finnish | `values-fi/pattern_annotations.xml` | 75 | 0 | Annotation text. |
| Finnish | `values-fi/pattern_reading_assistance.xml` | 49 | 0 | Reading assistance and bookmark text. |
| Each of `sv`, `nb`, `da`, `de`, `nl`, `fr`, `it`, `es`, `pt` | `values-<locale>/strings.xml` | 936 | 31 | General application text. |
| Each of those nine locales | `values-<locale>/reference_strings.xml` | 256 | 0 | Reference text. |
| Each of those nine locales | `values-<locale>/pattern_annotations.xml` | 75 | 0 | Annotation text. |
| Each of those nine locales | `values-<locale>/pattern_reading_assistance.xml` | 49 | 0 | Reading assistance and bookmark text. |

Baseline totals:

- **41 text-bearing XML files.**
- **1,347 translatable resource keys per language:** 1,316 strings and 31 plurals.
- **14,817 language/key pairs** before expanding plural variants and calling contexts.
- Each language has one string alias: `web_pattern_error_designer_too_long` references `web_pattern_error_title_too_long`. Review the resolved wording in both contexts.
- `en`, `fi`, `sv`, `nb`, `da`, `de`, and `nl` contain 62 plural items each; `fr`, `it`, `es`, and `pt` contain 93 each.
- This gives **1,378 string/plural-item entries** in each of the first seven languages and **1,409** in each of the other four: **15,282 entries** across the eleven languages. Alias entries are included; additional contexts and non-resource content are not.
- No `string-array` resources were found in these files at this revision. Discover and include any added later.
- The two default-only `translatable="false"` keys are `widget_preview_primary_count` and `counter_step_custom_placeholder`. They are numeric examples, not missing translations. Check their role where displayed; do not translate them blindly.

A preparation-time XML check found no duplicate translatable keys and no missing, extra, or type-mismatched keys against the default set in these eleven directories. This establishes structural parity only. It does not establish correct translation, correct plural selection, correct placeholder meaning, or complete coverage of user-visible code literals.

### 4.3 Content outside the XML count

Inspect these implementation areas as part of the owning batches. Paths below are relative to `app/src/main/java/com/finnvek/knittools/`.

| Content | Starting points | Required audit consequence |
| --- | --- | --- |
| Main counter meaning | `domain/model/CraftType.kt`, `domain/model/MainCounterLabelType.kt`, `domain/calculator/CounterValueFormatter.kt`, `ui/components/MainCounterDisplayText.kt` | Trace selected units, custom labels, counts, targets, and action descriptions. A craft default does not override the user's explicit unit. |
| Generated instructions | `domain/calculator/IncreaseDecreaseCalculator.kt`, `domain/calculator/InstructionParser.kt`, `ui/screens/increase/IncreaseDecreaseScreen.kt` | Include generated `K`, `M1`, and decrease notation and the explanations needed to understand it in each language. |
| Gauge and measurements | `domain/calculator/MeasurementCalculator.kt`, `MeasurementNumberParser.kt`, `MeasurementNumberFormatter.kt`, `ui/screens/gauge/GaugePresentation.kt` | Verify numerical meaning, dimensions, input syntax, rounded versus exact results, and composed output. |
| Cast-on and yarn calculations | `domain/calculator/CastOnCalculator.kt`, `YarnEstimator.kt`, `YarnUsageCalculator.kt` | Review arithmetic and the text describing the result, not only field labels. |
| Abbreviations | `domain/calculator/AbbreviationData.kt` | Review abbreviation token, meaning, description, and search behavior together. |
| Chart symbols | `domain/calculator/ChartSymbolData.kt`, `ui/screens/chartsymbols/ChartSymbolScreen.kt` and the drawing implementation reached from it | Match each drawn symbol to its meaning and stated convention. |
| Needle/size tables | `domain/calculator/NeedleSizeData.kt`, `SizeChartData.kt` | Verify every displayed data cell, label, unit, source applicability, and approximation statement. |
| Yarn categories | `domain/model/StandardYarnWeight.kt`, `ui/screens/library/YarnWeightField.kt` | Labels and ply hints exist in code and are displayed. Separate canonical saved values from localized presentation. |
| Care symbols | `ui/components/care/CareSymbol.kt`, `CareSymbolIcon.kt` | Match labels to rendered icons and source-supported care meanings. |
| Time and statistics | `domain/calculator/DurationDisplayFormatter.kt`, `MinutesPerRowFormatter.kt`, `ui/components/LocaleDateFormat.kt`, `ui/screens/insights/` | Check units, numbers, dates, ranges, and the claims made by metric labels. |
| Widgets | `widget/`, `app/src/main/res/layout/widget_counter_preview.xml` | Include rendered and preview text, actions, dynamic numbers, and any intentionally unit-neutral wording. |

This is a starting map, not a complete list of callers. Search all relevant source sets and resources for additional app-owned visible text, content descriptions, toast/snackbar text, dialog messages, formatted exports, and resource references. Classify debug-only text separately. Exclude logs, test fixtures, licenses, and internal identifiers unless they are actually presented as application copy.

### 4.4 Historical eleven-language closeout inventory and reconciliation — 2026-10-19

At that closeout, a small read-only XML enumeration of all eleven resource directories confirmed **41 text-bearing XML files**, **1,349 translatable keys per locale = 1,317 strings + 32 plural keys**, no arrays, and **14,839 language/key pairs**. The current plural items and translatable variant totals are:

| Locales | Strings per locale | Plural keys per locale | Plural items per locale | Translatable variants per locale | Group total |
| --- | ---: | ---: | ---: | ---: | ---: |
| en, fi, sv, nb, da, de, nl (7) | 1,317 | 32 | 64 (one/other) | 1,381 | 9,667 |
| fr, it, es, pt (4) | 1,317 | 32 | 96 (one/many/other) | 1,413 | 5,652 |
| **Campaign** | | | | | **15,319** |

The default-only `translatable="false"` strings remain `widget_preview_primary_count=42` and `counter_step_custom_placeholder=10`. They are intentional numeric examples, not missing translations or additional translatable variants. Default XML therefore has **1,351 total keys / 1,383 total variants**, including those two examples. Some locale coverage ledgers include their use as separate records; do not add them again to the translatable denominator.

Historical counts remain valid for their recorded stages:

- Preparation: **1,347 translatable keys**, **1,378 / 1,409 variants** per two-/three-form locale, **15,282 variants** total (section 4.2).
- Original shared fixes added `counter_progress_format` and `size_row_tween`; the M1 error replaced an existing key. Before the month migration: **1,349 keys = 1,318 strings + 31 plurals**, **1,380 / 1,411 translatable variants**, **15,304 variants** total. The English report's historical **1,382 resource variants** includes the two numeric examples: **1,380 translatable + 2 nontranslatable**. Finnish explicitly records the same distinction.
- LA-es-B05-58 replaced one string per locale with two or three plural items: **11 strings replaced by 26 items, net +15 variants**, no key-count increase. Spanish's verified closeout already records 1,413 variants; Portuguese's later complete audit already includes that contract. Do not apply their migration delta a second time.

**Evidence bridge, not a renewed full audit:** the saved en/fi/sv/nb/da/de/nl/fr/it coverage still records the earlier month string. Its accepted wording is copied unchanged into every current form. This closeout compared all eleven current month definitions with the exact forms in [shared-fixes.md](config/language-audit/shared-fixes.md) and [month-fix-verification.json](config/language-audit/es/month-fix-verification.json); all match, with the same positional `%1$d`. Ten current `strings.xml` file hashes also match that repair receipt. Portuguese's later wording corrections explain its different whole-file hash; its current file matches the identity in [pt/review.md](config/language-audit/pt/review.md), and its month forms remain identical to the repair.

The prior independent acceptances, the saved narrow caller/resource validation, Spanish's affected-form/context reread and Portuguese's post-migration review support the reconciled current record. Unchanged wording was not reviewed again, old row counts were not inflated, and unrelated evidence was not renewed. Locale reports, coverage, corrections, historical hashes and test receipts retain their original bytes and scope. No unexpected material discrepancy or concrete content blocker was found.

### 4.5 Current inventory after the authorized pt-BR addition — 2026-10-19

There are now **12 declared/selectable locales**, **45 text-bearing XML files**, and **1,350 translatable keys per locale = 1,318 strings + 32 plurals**: **16,200 language/key pairs**. The existing eleven sets gain only the native `settings_language_brazilian_portuguese` label, `Português (Brasil)`. Their prior text is unchanged. The new `values-pt-rBR` set contains all four applicable files with no missing, extra or type-mismatched keys.

| Locales | Strings per locale | Plural keys | Plural items | Translatable variants per locale | Group total |
| --- | ---: | ---: | ---: | ---: | ---: |
| en, fi, sv, nb, da, de, nl (7) | 1,318 | 32 | 64 | 1,382 | 9,674 |
| fr, it, es, pt, pt-BR (5) | 1,318 | 32 | 96 | 1,414 | 7,070 |
| **Current total** | | | | | **16,744** |

The default-only numeric examples remain unchanged and excluded: default XML has **1,352 total keys / 1,384 total variants** including them. Historical counts and whole-file hashes in sections 4.1–4.4 and the original locale evidence remain historical. No old coverage ledger is rewritten to pretend that the added native option label was present earlier. The label was checked in every current locale; removing its line reconstructs each original resource file hash (accounting for existing line endings).

Brazilian coverage records all **1,414 variants**, independently read in their production contexts, plus grouped code/generated-content decisions. **401 variants / 391 keys** required regional adaptation; **1,013 variants** were retained after review. See [pt-BR review](config/language-audit/pt-BR/review.md), [coverage](config/language-audit/pt-BR/coverage.tsv), [adaptations](config/language-audit/pt-BR/adaptations.tsv) and [validation](config/language-audit/pt-BR/validation.json). The new review does not reuse pt acceptance as proof of Brazilian naturalness.

## 5. Scope and coverage rules

### 5.1 Included

- All app-owned visible text in the assigned language, including empty, loading, disabled, success, and error states.
- All help text, descriptions, tips, examples, calculations, reference tables, abbreviation entries, and symbol legends.
- Knitting and crochet contexts that already exist in the app. Do not add new instructional content merely to make the audit broader.
- Accessibility labels and action descriptions as language content.
- Dynamically composed sentences, names inserted into messages, plurals, ordinal-like positions, units, dates, durations, and number formatting.
- App-owned clipboard and export wording where it exists.
- Shared data or drawings whose correctness is necessary to judge the language.

### 5.2 Excluded

- User-entered project names, notes, yarn metadata, custom labels, and their spelling; review the application's surrounding grammar instead.
- Contents of user PDFs, photos, imported third-party descriptions, and Ravelry's web pages; review KnitTools' controls and claims around them.
- Website/store-listing translations and historical backend text that the Android app does not use.
- A general security, architecture, performance, purchase, sync, or accessibility audit.
- Exhaustive UI-state/rendering verification. It requires its own scope; language acceptance does not require screenshots, fixtures, or runtime execution for every occurrence, caller, screen, error path, or transient message.
- New translations, features or regional variants outside the explicitly assigned scope, and stylistic rewrites of already acceptable wording. The pt-BR addition has separate explicit authorization.

### 5.3 Review unit and denominator

The minimum language-review unit is:

`locale + resource type + resource key + variant + materially distinct context`

Use `value` for a string, the actual quantity name for a plural item, and the item index for a string array. Distinct contexts include, for example, a standalone title, a noun inserted after a number, and the same noun inserted into a button's action description.

For code-backed content use:

`locale + source file/member + data row or output case + context`

Examples include a needle-table cell, a chart symbol, a generated decrease sequence, or a yarn-category hint. These are additional audit items, not invisible exceptions to the XML denominator.

Every translatable key must have an owner batch. Every variant must be read. Every materially different caller must be accounted for. An unchanged text still needs a recorded review result. Repeated identical uses may share one coverage row and caller references unless their meaning or argument semantics materially differ. Inspect the actual differing callers. Every resource and variant still receives content review; do not sample linguistic reading, factual claims, table cells, or terminology. Rendering coverage is separate and concern-driven (section 11.2).

### 5.4 Inventory procedure before the first batch

1. Record `git branch --show-current`, `git rev-parse HEAD`, `git status --short`, and existing diffs for files the chat may edit. Read the current instructions.
2. Read `locales_config.xml` and `AppLanguage.kt`. Compare declarations with actual resource directories; do not silently skip a new locale or qualifier.
3. Parse every text-bearing XML file in the default and target directories. Include `string`, `plurals`, and `string-array` nodes and their attributes.
4. Record resource name, type, file, variant, exact text, alias target, format placeholders, and `translatable` status. Resolve aliases without losing their separate calling contexts.
5. Compare keys and types against the current default resources. Treat missing keys, extra keys, duplicate definitions, invalid aliases, and fallback as separate findings to investigate.
6. Search callers and add code-backed data, composed messages, and display literals. A search hit is a candidate; confirm whether it reaches the UI.
7. Assign each item to exactly one primary batch from section 8. Add references to secondary contexts instead of double-counting ownership.
8. Review unassigned items individually. B23 must finish with zero unexplained/unassigned items; it must not become an unreviewed catch-all.
9. Record SHA-256 hashes of the audited input files, including relevant shared files. A hash records which version was read; it does not prove review.
10. Save the inventory and coverage record before marking any batch complete.

Do not delete unused-looking text as an audit shortcut. Record the suspected lack of callers, inspect source/build configuration as needed, and document the disposition. Existing in-scope text still receives language review unless the user explicitly approves its exclusion or removal.

## 6. Quality dimensions and statuses

Record content dimensions separately; one passing dimension cannot substitute for another. Rendering evidence is supplementary, not a universal sixth acceptance gate.

| Code | Dimension | Required evidence |
| --- | --- | --- |
| `T` | Terminology | Target-language craft usage, glossary concept, correct craft and operation. |
| `F` | Factual correctness | Technique/source verification, independently checked arithmetic, or actual implementation behavior, as appropriate. |
| `L` | Linguistic quality | Reading of the target text for grammar, naturalness, register, and clarity; acceptable wording stays unchanged. |
| `C` | Context correctness | Actual callers where meaning or arguments differ, state, selected counter unit, and complete composed messages. |
| `R` | Resource/format correctness | Placeholders, aliases, quantities, escaping, locale formatting, and relevant resource checks after corrections. |
| `V` | Supplementary rendering evidence | Record what was actually inspected and its limits. Targeted inspection is required only for a concrete language concern unresolved by source/context review (section 11.2). |

For new item/dimension decisions use `PENDING` (not yet reviewed or unresolved, with reason), `PASS` (reviewed with evidence), `FAIL` (confirmed defect), or `NOT_APPLICABLE` (with a concrete reason). Keep existing records in their original schema and statuses: historical `NOT_STARTED`, `IN_REVIEW`, `BLOCKED`, and `STALE` are not retroactively converted. In particular, the English V `BLOCKED` rows record unverified rendering, not necessarily language defects.

For new records, V may be left blank when no rendering check was performed; that is not a rendering pass or N/A. Record a concrete unresolved runtime question as a finding linked to the affected content dimension; missing screenshots alone are not such a question. Never turn an unperformed check into `PASS` or `NOT_APPLICABLE` to satisfy a gate.

Batch progress is `NOT_STARTED`, `IN_PROGRESS`, or `REVIEW_COMPLETE`; its content verdict is `PASS` or `OPEN`. Use the language outcomes in section 16. A task can finish its assigned scope while the language remains incomplete.

Native-speaker validation remains separate: `NOT_REQUESTED`, `REQUESTED`, `REVIEWED_WITH_ISSUES`, or `REVIEWED_PASS`, with actual qualification and reviewed scope. Never claim it for an AI review, automated test, or source comparison. No reviewer must automatically be hired or contacted. A specific unresolved expression needing that expertise remains `PENDING` and blocks acceptance; absence of native review alone does not.

## 7. Terminology and source policy

### 7.1 Build a concept-based glossary

Maintain a concept-based glossary for the active language to resolve terminology and preserve acceptable alternatives, not to normalize wording. Expand it when new concepts are encountered. It must contain the concept, approved term, applicable context, acceptable alternatives, misleading alternatives, evidence, and status.

| Concept family | Distinctions that must be resolved |
| --- | --- |
| Craft | Knitting versus crochet; generic wording when the feature serves both. |
| Row / round / repeat | Flat-work row, circular round, repeat count, row position, and total worked quantity. |
| Stitch | A stitch on the needle, a stitch operation, stitch count, and a stitch pattern. |
| Pattern | Written instructions, stitch pattern, chart, repeat, saved metadata, and PDF document. |
| Gauge / tension | Stitch density versus row density; the stated measurement basis; terminology of the chosen variety. |
| Swatch | Measured sample, its dimensions, and whether care/blocking conditions affect the stated comparison. |
| Increase / decrease | Operation, lean/direction, number of stitches consumed, and net count change. |
| Slip | Knitwise/purlwise insertion, yarn in front/back, and whether a stitch is worked or moved. |
| Sides / loops | Right/wrong side of fabric versus right/left direction; front/back loop versus yarn position. |
| Cast on / bind off | Starting and finishing operations, not mounting/saving/deleting in the app. |
| Repeat / shaping | Pattern repeat, repeat section, linked counter, and scheduled shaping action. |
| Yarn weight | Thickness category versus physical mass; ply category/hint versus actual strand construction. |
| Yarn units | Length, mass, skein/ball/hank, package quantity, and fractional quantities. |
| Yarn ownership | Global stash, project allocation, planned use, actual use, remaining amount, and overage. |
| Colors | Color name, manufacturer color number, and dye lot. |
| Needles / hooks | Tool type, metric diameter, and manufacturer/region numbering convention. |
| Size / fit | Body measurement, finished garment measurement, ease, circumference, width, and length. |
| Care | Washing, bleaching, drying, ironing, and professional cleaning; permission versus prohibition. |
| App actions | Save, attach, unlink, remove, delete, import, export, restore, replace, and open. |
| Sessions / insights | Elapsed time, completed session, active session, pace, completion event, and project count. |

Do not enforce a one-to-one dictionary replacement where the language uses one term in several contexts or requires different grammatical forms. Search every occurrence, then inspect callers before changing it. For Finnish, the project explicitly requires `ohje` for a knitting pattern and `verkko-ohje` for web-pattern metadata; this does not authorize replacing unrelated geometric uses of a word.

Preserve app-owned canonical identifiers and persisted values. For example, a proposal to localize `StandardYarnWeight.label` must distinguish the display label from the stored Ravelry-style category value and be handed off if it needs shared implementation work.

### 7.2 Evidence hierarchy

1. For app behavior, inspect current callers, models, repository behavior, and relevant tests. Run a targeted check when source inspection cannot establish the user-visible result.
2. For a knitting/crochet technique or term, use specialist primary material in the target language: an established yarn manufacturer, original designer/publisher, professional organization, or original technical reference.
3. For a table or conversion, use the source for that actual table/system. A page about US sizes does not verify a Japanese or UK column.
4. Use authoritative language references for grammar and spelling. They do not establish knitting meaning by themselves.
5. Use an independent second specialist source for ambiguous, conflicting, regional, or technique-critical claims. Two translations of the same publisher's page are not independent evidence.

Search snippets, machine translation, back-translation, an AI answer, previous approval, and another language's text are leads or aids only. Read the relevant source passage or diagram. A URL in a source-code comment proves that a source was cited, not that the app matches it.

Ordinary UI wording does not need a web citation for every word. Every technical assertion and disputed term needs a traceable basis. Record one concept-level source once and reference it from all applicable items, while still reading each target-language item.

### 7.3 Initial source register

These are retained preparation starting points, not the final campaign source record. Access status is from 2026-10-18; the latest locale reports contain the completed content decisions and their actual sources. Historical access failures below do not create new closeout obligations.

| Source | Intended use | Preparation status and limitation |
| --- | --- | --- |
| [Craft Yarn Council knitting abbreviations](https://www.craftyarncouncil.com/standards/knitting-abbreviations) | US knitting abbreviation meanings and operation descriptions. | Read during planning discussion; explicitly US terminology. Does not approve translations. |
| [Craft Yarn Council crochet abbreviations](https://www.craftyarncouncil.com/standards/crochet-abbreviations) | Crochet notation and US/UK differences where relevant. | Accessible; target-language equivalence remains to be established. |
| [Craft Yarn Council hooks and needles](https://www.craftyarncouncil.com/standards/hooks-and-needles) | Metric and US reference values, with manufacturer variation. | Accessible; does not independently verify all UK/Japanese values in the app. |
| [Craft Yarn Council body sizing](https://www.craftyarncouncil.com/standards/body-sizing) | Body measurements, measurement methods, and fit/ease distinctions. | Accessible; individual table cells still need their exact underlying references. |
| [Craft Yarn Council knit chart symbols](https://www.craftyarncouncil.com/standards/knit-chart-symbols) | Chart-symbol convention cited by `ChartSymbolData.kt`. | Fetch timed out during preparation. Unverified; retry or obtain another authoritative source when auditing B19. |
| [Craft Yarn Council cable symbols](https://www.craftyarncouncil.com/standards/cable-symbols) | Cable-symbol convention cited by `ChartSymbolData.kt`. | Not accessible through the research tool during preparation. Unverified. |
| [Android localization](https://developer.android.com/guide/topics/resources/localization) | Resource discovery, defaults, and fallback. | Accessible. Platform documentation, not linguistic evidence. |
| [Android string resources](https://developer.android.com/guide/topics/resources/string-resource) | Resource syntax, formatting, escaping, and plurals. | Accessible. Verify the relevant section for the actual issue. |
| [Unicode CLDR plural rules](https://cldr.unicode.org/index/cldr-spec/plural-rules) | Quantity-category reasoning and number-dependent forms. | Accessible during preparation. Use a targeted Android check if source/context review leaves a concrete quantity-selection concern unresolved. |

Discover and record exact target-language specialist pages during each language audit. A multilingual glossary can help find candidates, but inspect original contextual usage and identify shared publisher provenance. Do not mark every language sourced merely because a provider offers a language selector.

For each used source record: source ID, title, publisher/author, exact URL, language/region, access date, relevant section, supported claim, limitations, and whether another cited source depends on the same underlying material. Paraphrase evidence; do not copy entire third-party articles into reports.

When sources conflict, identify whether they describe different techniques, regions, notation systems, or product-specific instructions. Do not choose by majority vote or smooth over the difference. If the conflict cannot be resolved, leave the item `PENDING` with the narrow missing decision/evidence.

## 8. Batches for every language

Each language must eventually cover B01-B23 through its assigned tasks; no task continues beyond its boundary. The key names below are discovery anchors, not exhaustive selectors. The inventory assigns the complete current key set to these batches before approval is possible. Related prefixes sometimes belong to different features; assign by actual caller, not by spelling alone.

Work in these batches rather than loading all language files into one undifferentiated review. Choose manageable reading groups within the assigned boundary; do not turn group size into a new task or reporting requirement. Keep a technique's name and explanation together. Record exact membership, not just line ranges, which can move after edits.

| Batch | Scope and discovery anchors | Mandatory review |
| --- | --- | --- |
| B01 | Common controls, navigation, shared units, headings, empty/error language; `tab_*`, `unit_*`, shared actions and labels; navigation and shared components. | Consistent action verbs and terminology, complete sentences, appropriate brevity, unit meanings, and context-dependent uses of shared labels. Do not apply English title casing to another language automatically. |
| B02 | Projects, folders, filters, bulk selection, create/edit/complete/reopen/delete; `project_*`, `folder_*`, selection and completion plurals; project screens. | Project versus folder, active versus completed, virtual All Projects/Unfiled views, scope of bulk actions, deletion consequences, craft choice, and preservation of user names. |
| B03 | Main counter, selected label/unit, targets, remaining/over-target text, step size, undo/reset, widget; `main_counter_*`, `counter_*`, `current_*`, `rows_format`, `rounds_format`, `repeats_format`. | Trace `MainCounterLabelType` at every caller. Check ROWS, ROUNDS, REPEATS, CUSTOM; steps 1 and greater than 1; positions versus quantities; unit-neutral widget wording where necessary. |
| B04 | Extra, linked, repeating, shaping, and repeat-section counters; row reminders; relevant `repeat_*`, `linked_*`, `reminder_*`, `row_*` and counter form copy. | Explain initial/current values, repeat boundaries, link behavior, occurrence counts, timing, and what each action changes. Do not confuse a repeat number with a row within a repeat. |
| B05 | Work sessions, session/counter history, recovery, durations, Insights, completion events; `work_*`, `session_*`, `insights_*`, history labels. | Start/stop versus background state, recovery choices, elapsed time, pace direction, range/project filters, completed events versus distinct projects, and all number/date/duration grammar. |
| B06 | My Yarn, yarn cards, stash status, quantity, thickness, color/dye lot, care symbols; yarn metadata keys, `care_*`, `StandardYarnWeight`. | Mass versus thickness, skein/package meaning, canonical category labels, ply hints, source/linked-project labels, and every care label against its actual drawn icon. |
| B07 | Project yarn notes and project yarn usage; `yarn_usage_*`, project yarn forms, `YarnUsagePresentation.kt`. | Planned/allocated/used are independent; remaining means allocated minus used; overage is explicit; unknown is not zero; conversion basis and fractional quantities are accurately described. No implied automatic stash deduction. |
| B08 | Saved patterns, web-pattern metadata, Ravelry entry points and app browser chrome; `saved_pattern*`, `web_pattern_*`, `ravelry_*`, pattern availability/actions. | Distinguish metadata from a readable document and external webpage from local PDF. Verify free/paid/unknown wording, duplicate messages, link confirmation, and save/attach/unlink/delete consequences. |
| B09 | Incoming PDFs, project documents, camera/gallery import and permissions; `incoming_pdf_*`, `project_document*`, `pattern_image*`, import and capture copy. | What is copied and saved, destination, ordering/primary document, limits, cancellation, replacement, and failure wording. Camera copy uses photo/PDF terminology. Do not promise OCR or cloud sync. |
| B10 | PDF reading mode, bookmarks, guides, row markers, calibration, follow/pause; all keys in `pattern_reading_assistance.xml` plus related viewer copy. | Document page versus project counter position; bookmark versus annotation; marker versus reading guide; what follows automatically, what pauses, and what calibration means. |
| B11 | PDF markup, tools, annotation layers, undo/redo, selection, marked-PDF export; annotation keys, `pattern_annotations.xml` where present. | Editable project layer versus read-only shared layer, tool actions, export scope, source preservation, progress, and cancellation/error messages. Follow default keys even where English stores them in `strings.xml`. |
| B12 | Notes, progress photos, photo library/viewer, photo deletion, sharing; note/photo/gallery copy and corresponding callers. | Local editing/autosave meaning, save state, captured versus imported images, destructive action scope, and natural text around inserted names. User content itself is excluded. |
| B13 | Measurements and Gauge; `measurement_*`, gauge labels, swatch examples, `GaugePresentation.kt`. | Stitch/row axes, density basis, length conversions, original/target/actual values, exact versus rounded output, estimation warnings, localized parsing, Copy output, and simultaneous presence of related labels. |
| B14 | Increase/Decrease and its pasted examples; `increase_*`, `decrease_*`, `mode_*`, `style_*`, relevant `instruction_*` and tips. | Knit operation and net result, flat/circular behavior, generated notation, consumed stitches, distribution of operations, errors, and examples that the real regex parser accepts. |
| B15 | Cast On; `cast_on*`, `desired_width`, `stitch_gauge`, `pattern_repeat_optional`, `edge_stitches*`, `stitches_result`, `actual_width`, `adjusted_*`. | Desired body width, total edge stitches, repeat rounding, adjusted alternatives, actual resulting width, and metric/imperial gauge basis. |
| B16 | Yarn Estimator; `total_yarn_needed`, `length_per_skein`, `weight_per_skein`, `estimated_skeins`, `total_weight`, `extra_skein_note`, relevant yarn tool copy. | Required length versus package length, exact versus purchased skeins, rounding upward, meaning of total weight, uncertainty, dye-lot advice, and unsupported absolute recommendations. |
| B17 | Needle Sizes and Size Charts; `needle_*`, `header_mm/us/uk/jp`, `size_*`, `NeedleSizeData.kt`, `SizeChartData.kt`. | Every table cell, regional size system, approximation, unit, age/category heading, body versus garment dimension, circumference versus flat width, and missing-value representation. |
| B18 | Abbreviations; `abbr_*`, abbreviation screen controls, `AbbreviationData.kt`. | Every token/meaning/description triple; knit/purl, increases/decreases, lean, slip orientation, yarn position, cable directions, short rows, fabrics, and informal terms. Also verify the language in which notation tokens are defined. |
| B19 | Chart Symbols; `sym_*`, symbol categories, chart-symbol screen/data/drawings. | Every symbol/name/description triple, right/wrong-side convention, stitch count, direction, cable hold position, and distinction between an illustrative convention and a universal standard. |
| B20 | Settings, language selection, theme/display controls, privacy/analytics, app information and update prompts; relevant `settings_*`, language labels, preferences. | Native language names, system-default meaning, consistent terminology, true opt-in/status descriptions, and no claims about data handling unsupported by the actual app. |
| B21 | Backup and restore; `backup_*`, `restore_*`, backup screen and preview. | Backup versus sync, export versus restore, replace consequences, version/error language, included/excluded data, and preview counts. Do not perform a destructive restore on user data to check copy. |
| B22 | Pro, trial, billing and gated-action prompts; `pro_*`, `billing_*`, upgrade/status copy. | Current entitlement behavior, trial start/expiry, price text and locale, free project limit, existing-content access, cancellation/restore-purchase meanings, and debug override versus real entitlement. No real purchase is required. |
| B23 | Remaining app-owned text, terminology consistency, coverage reconciliation, targeted-question closeout, and report. | Read every residual item, reconcile all keys/variants/contexts, search conflicting terminology, account for shared handoffs, resolve specific language concerns, run only necessary affected checks, and apply section 16. |

Each batch record must include the exact item list or a reference to its rows in `coverage.tsv`, the relevant source/code references, findings, corrections, checks, and remaining uncertainties. A sentence such as “reviewed the counter strings” is not sufficient evidence of coverage.

## 9. Per-batch review procedure

### 9.1 Establish the actual meaning

1. Read the target-language text before relying on the English wording. Write down the meaning a user would reasonably take from it.
2. Locate relevant callers and inspect those whose meaning or arguments differ. Record enough screen/action/state, craft/unit, and argument context to establish meaning; group identical uses without duplicate rows.
3. Compare with the intended operation and current implementation. English is a comparison aid, not unquestioned authority.
4. For instructional or reference content, identify the exact claim and source requirements before deciding whether it is correct.
5. Classify text that is intentionally invariant: brand names, notation tokens, standard category codes, user data, or numeric examples. Explain each exemption from translation; English-looking text is not automatically a defect or automatically acceptable.

### 9.2 Review terminology and facts

1. Check the glossary and same-language specialist usage in context.
2. Verify instructions step by step. Count stitches consumed, produced, and remaining; establish orientation and stated conditions.
3. Check numbers and units independently of the app output and the existing test expectation.
4. For product copy, verify what the action actually changes and preserves. Do not infer behavior from the button label.
5. Record ambiguity rather than silently making a broad replacement. When a source fact is wrong, record the affected other languages as shared dependencies.

### 9.3 Read for naturalness

Read the text as a user would encounter it, including the surrounding heading, field, button, and populated message. Check:

- Natural word order, grammatical case, gender, number, agreement, and idiomatic verbs.
- Consistent user address, voice, register, and degree of formality.
- Short labels that remain understandable without relying on English syntax.
- Clear actor and object: what will be saved, deleted, replaced, linked, or restored.
- Appropriate capitalization, punctuation, quotation marks, spacing, accents, and compounds.
- Avoidance of literal calques, accidental mixed languages, developer terminology, and unsupported promises.
- Preserved meaning when shortening a label; a compact wrong term is not a successful fit correction.

Distinguish a demonstrable problem from a preference. Keep a correct and natural existing expression when an alternative is merely different. Do not invent a defect to justify rewriting every string.

### 9.4 Verify composed text and corrections

1. Substitute realistic values into every formatted construction. Review the final sentence, not just `%1$s` in isolation.
2. Check all materially different contexts. A noun appropriate as a title may need another form inside an instruction.
3. Before each proposed correction, explain the specific defect, evidence, and effect on meaning or comprehension. If no defect is established, keep the wording. If corrections are authorized, change the smallest necessary target-language text. Preserve resource names, argument semantics, file layout, and unrelated wording.
4. If correct wording is impossible with the current shared resource/caller, record a concrete shared fix instead of forcing misleading grammar into the existing template.
5. Read the changed passage again without looking at the old version. Recheck its fact and all affected callers.
6. Run the relevant technical checks at a sensible batch or language boundary. Record failures honestly and fix regressions caused by the authorized change.
7. Update item statuses and evidence only for the versions actually reviewed.

## 10. High-risk factual checks

### 10.1 Stitch techniques and notation

- Distinguish the number of worked operations from the resulting net stitch change.
- Verify knit versus purl, front/back loop, yarn in front/back, slip orientation, fabric side, and left/right lean separately.
- Do not treat different left-leaning or centered decreases as identical merely because their net change matches.
- Check that a cable's number refers to the intended total crossed stitches and that the holding position matches the drawing and description.
- Establish whether chart instructions describe the visible result from the right side or the operation on the current side. Consult the applicable legend.
- Do not replace a pattern's notation convention with another region's convention without making the meaning explicit.
- Check instructions for missing steps or conditions that would cause a reader to produce a different result.
- In mixed-craft screens, do not instruct crocheters to manipulate knitting needles, or present a knitting-only technique as craft-neutral.

### 10.2 Calculators and unit semantics

Use independent calculations as audit cases. The following are concrete starting cases, not tests already run:

| Case | Expected mathematical interpretation | What the copy must distinguish |
| --- | --- | --- |
| 22 stitches per 10 cm, desired width 25 cm | `22 / 10 * 25 = 55` stitches before any separate repeat/edge adjustment. | Width versus height and stitch density versus row density. |
| 22 stitches per 4 inches, desired width 8 inches | `22 / 4 * 8 = 44` stitches. | A 4-inch gauge basis is not exactly 10 cm; 4 inches is 10.16 cm. |
| 1 inch converted to centimeters | 2.54 cm. | Exact conversion versus the rounded number shown to the user. |
| 96 existing stitches, 12 increases | 108 resulting stitches; verify the generated sequence consumes the correct original stitches. | Increase count versus final count; an M1-style operation is not interchangeable with every other increase. |
| 96 existing stitches, 12 two-together decreases | 84 resulting stitches; the 12 decreases consume 24 existing stitches. | Decrease count versus stitches consumed and stitches remaining. |
| Cast-on body width 20 cm, gauge 20 stitches/10 cm, repeat 6, total edge stitches 2 | Raw body count 40; the inspected calculator chooses body count 42, then adds 2, giving 44 and a resulting width of 22 cm. | Desired body width, total edge stitches, repeat rounding, and actual full width. Recheck this expectation against any subsequent implementation changes. |
| Required yarn 450 m, 200 m per 50 g package | Exact quantity 2.25 packages; whole-package purchase quantity 3; full purchased-package mass 150 g. | Exact consumption mass is a different quantity from purchased-package mass. |
| Project allocated yarn 400 m, used yarn 450 m | Remaining is -50 m, presented as 50 m over allocation. | Overage is not a negative stash balance; an unknown allocation is not zero. |

Also cover: zero versus missing input; incomplete numeric input; decimal comma/point behavior in the supported locale; fractional quantities where allowed; boundary values; invalid combinations; and meaningful error messages. Use the actual operation's supported domain, rather than forcing every test value into every field.

For each explanation of a calculation, verify both the formula and its assumptions. Display rounding must not be described as exact, and the language audit must not silently alter calculation semantics. If the algorithm is wrong, create a shared implementation handoff with a reproducible failing example.

### 10.3 Tables, yarn, and care

- Verify every reference-table cell and its exact source/system. Row-count, positivity, and sortedness tests do not prove the data correct.
- Preserve valid variation between manufacturers and systems; do not turn approximate needle equivalents into exact universal claims. Metric diameter is the reliable comparison in the cited CYC guidance.
- Distinguish actual body dimensions from finished garment dimensions and ease, as the cited CYC sizing guidance does. Do not infer an individual's fit from a generic age/size label.
- Distinguish yarn thickness categories from grams and from actual ply construction. Review the `StandardYarnWeight` hints as factual content, not merely translation tokens.
- Separate skein/ball/package naming from the quantity that the app stores and calculates.
- Match care text to the actual care icon. Verify temperatures, dots, lines, prohibitions, and professional-cleaning letters where displayed. Obtain an authoritative care-symbol source before approving those claims.
- Product-specific yarn care instructions govern care recommendations. Do not turn a broad fiber tendency into an unconditional instruction for every yarn.
- Review absolute language such as “always,” “never,” and “exact” especially carefully. It needs evidence and the right conditions, not merely a fluent translation.

### 10.4 Product facts and consequences

Check the current implementation before approving statements about:

- Project completion, reopening, completion-event counts, folder deletion, and bulk actions.
- Active versus completed sessions, recovery, durations, and pace units.
- Notes/autosave and local persistence.
- Project yarn usage versus the global stash.
- Metadata links, local PDFs, multiple project documents, saved patterns, and shared-file preservation.
- Bookmark/guide behavior versus annotation/export behavior.
- Manual backup/restore versus continuous sync.
- Ravelry's in-app browser and app-owned PDF import, without resurrecting removed OAuth/API functionality in copy.
- Pro/trial availability and preservation of existing content after access ends.
- Analytics consent and any data-handling statement present in Settings.

These are text-accuracy checks. They do not authorize backend work, purchases, destructive data operations, or a separate privacy/security campaign.

## 11. Android resources and runtime validation

### 11.1 Resource checks

These checks support content correctness. Run the smallest relevant checks after resource changes; unchanged valid evidence can be reused. A documentation-only task needs no build.

- XML remains well-formed; after resource corrections, Android resource processing succeeds.
- Resource key names and types match the actual supported-language contract.
- Positional argument identity and type remain compatible with callers. Word order can change; the meaning of `%1$s` or `%2$d` cannot silently change with it.
- Reordered placeholders are valid when the language requires them. Do not reject a correct translation merely because token order differs from English.
- Verify literal percent signs, apostrophes, quotes, escapes, newlines, markup, and any styled or annotated spans actually present.
- Resolve aliases in the target locale and inspect each alias's context.
- Review every plural item and the actual quantity passed by the caller. The number selecting the form and the number displayed must express the intended concept.
- Do not confuse a position such as page 1 or row 1 with a count of one page or one row.
- Preserve required quantity categories according to current Android/CLDR behavior and the repository's contracts. The observed two-/three-item pattern is an inventory fact, not proof that every branch is linguistically correct.
- Check for accidental fallback and mixed-language output. A fallback that prevents a crash is not a completed translation.
- Preserve the project's prohibition on decorative U+00B7 separators; do not globally normalize legitimate language punctuation.
- Do not remove or weaken a test just because it catches a proposed wording change. Determine whether the contract or the change is correct.

### 11.2 Targeted runtime inspection

Start with the resource text, surrounding copy, actual callers and argument values, implementation, factual sources, and valid existing evidence. Runtime inspection is necessary when that review cannot resolve a **specific concern affecting terminology, facts, meaning, or comprehension**, for example:

- An action's visible context leaves it unclear what will be deleted, replaced, or saved.
- Dynamic number/unit composition or plural selection may produce a wrong or misleading sentence.
- Important instructions may be truncated, detached from their value, or obscured.
- A drawn craft/care symbol's meaning cannot be established from source and existing evidence.

State the precise question first, then inspect only the production context and meaningful variants needed to answer it. Reuse valid evidence for unchanged shared layouts, including existing English evidence when it establishes the same layout fact. That reuse never substitutes for reading another language or checking its materially different composition/length when relevant. Use a fixture, screenshot, or device run only if it supplies the missing evidence; do not manufacture a full state matrix.

For an executed check, record the question, target language, relevant build/input identity, device/configuration, production context, inputs, observed result, and evidence location. Distinguish source inspection, controlled presentation, reused evidence, and actual runtime behavior. Inspect actual pixels or measurements when the question concerns truncation; a string assertion alone cannot settle it.

No screenshot, fixture, or runtime run is required for every resource occurrence, caller, screen, error path, transient message, instruction, or plural variant. All of their text and materially different meanings remain in content scope. Exhaustive UI verification is separate work with its own explicit scope; do not reproduce the English campaign for other languages.

A confirmed defect or a specific unresolved question affecting language comprehension remains blocking, including when the necessary targeted check cannot safely run. Record the exact question and evidence needed. A generic absence of screenshots, a device, or exhaustive rendering coverage is not a language defect and does not block acceptance. Unverified rendering stays unverified, even when it is outside the language acceptance gate.

Do not repeat 320 dp, 200% font scale, their combination, or TalkBack without the user's explicit request. Use synthetic content and preserve user data for any authorized targeted check.

## 12. Evidence files and templates

### 12.1 File ownership

Create evidence files when the relevant language audit actually begins, not as empty placeholders during planning.

| File | Owner | Contents |
| --- | --- | --- |
| `LANGUAGE_AUDIT.md` | Coordination chat | Plan, language summary, and shared handoff summary. Language chats read it; they report proposed summary updates in their own report. |
| `config/language-audit/<locale>/review.md` | Current assigned task for that locale | Snapshot, variety decision, glossary, sources, batch status, findings, shared handoffs, validation log, closeout, and resume point. |
| `config/language-audit/<locale>/coverage.tsv` | Current assigned task for that locale | Complete item inventory and per-dimension evidence/status. |
| `reports/language-audit/<locale>/<run-id>/` | Chat running that validation | Raw command output, screenshots, temporary rendered examples, and detailed machine output when useful. This directory is ignored and must not be committed. |

Keep the durable conclusions and enough supporting detail in the language's `review.md` and `coverage.tsv`. A link to an ignored log by itself is not a durable explanation of a finding. Do not copy large raw logs into versioned documents.

Sequential tasks for the same language update its existing records without overwriting earlier evidence. Only one task owns a file at a time. Separate languages never append to the same coverage file. The report and coverage file are sufficient; hashes may be recorded there or in an existing manifest. No per-screen registry, screenshot manifest, duplicate status tracker, database, dashboard, new package, or permanent automation is required. Preserve existing registries and manifests as evidence; do not rebuild or relabel them to fit this policy. Reuse existing tools and tests. Creation of these records does not authorize committing them.

### 12.2 Language report template

```markdown
# <Language> Language Audit (<locale>)

## Scope and snapshot
- Language and selected variety:
- Assigned task boundary (exact batches/items/questions) and stopping point:
- Prior records reused / work completed in this task:
- Start date / last update:
- Branch / HEAD:
- Existing relevant changes at start:
- Audited resource/shared-input paths and SHA-256 hashes:
- Current source version after corrections:
- Resource key / variant / additional-context counts:
- Non-resource item count and ownership:
- Scope exclusions with reasons:

## Language decisions and glossary
| Concept | Approved expression/forms | Context | Alternatives and exclusions | Evidence | Status |
| --- | --- | --- | --- | --- | --- |

## Sources
| ID | Title and publisher | URL | Language/region | Access date | Passage/claim and limitations |
| --- | --- | --- | --- | --- | --- |

## Batch tracker
| Batch | Owned items | Reviewed items | Progress | Verdict | Open finding IDs | Evidence |
| --- | ---: | ---: | --- | --- | --- | --- |

## Findings
Use the finding template in LANGUAGE_AUDIT.md.

## Shared handoffs and other-language impact
List exact shared files/keys/concepts, owner, evidence, and blocking consequences.

## Validation log
| Date | Input/build identity | Check/command | Scope | Actual result | Evidence/limitation |
| --- | --- | --- | --- | --- | --- |

## Native-speaker review
- Status:
- Reviewer qualification and reviewed scope, if applicable:
- Remaining issues:

## Closeout
- Coverage totals and reconciliation:
- Confirmed findings by category/severity and resolution:
- Specific unresolved language questions / shared dependencies:
- Supplementary rendering evidence and unverified cases (separate from language blockers):
- Technical checks actually executed:
- Task result and language verdict, without implying whole-language acceptance from a partial task:

## Resume point
- Last fully reviewed item/sub-batch:
- Current unfinished work:
- Suggested next exact items, only if assigned in a later task:
- Inputs that changed and require rechecking:
```

Do not populate the native-speaker field with an invented identity or the batch tracker with estimated completion. If the chat is interrupted, update the resume point before stopping when possible.

### 12.3 Coverage file schema

Use UTF-8 TSV with a header. Escape literal tabs and newlines in text cells consistently, for example as `\t` and `\n`; document the convention. Preserve original and final wording in findings where changed. Capture the audited text or its unambiguous version reference in the coverage record.

Content coverage columns (retain the existing V column for supplementary rendering evidence; it is not a mandatory check per row):

```text
item_id	locale	batch	kind	key_or_member	variant	resource_path	context_id	caller_refs	text_or_case	input_hash	T	F	L	C	R	V	evidence_refs	finding_ids	result_note
```

Rules:

- `item_id` is unique and stable within the language; it is not a line number.
- `kind` distinguishes `string`, `plural`, `array`, `code_text`, `generated_output`, `reference_data`, and `symbol`.
- For a `plural`, record separate rows for every declared quantity. Add rows only where a caller materially changes the meaning or argument semantics; group repeated identical uses with caller references. Do not remove existing occurrence rows or historical evidence merely to simplify tracking.
- A technical result applying to many items can use one evidence reference. Do not duplicate its raw output in every row.
- A `PASS` needs identifiable evidence: a source/concept decision, explicit contextual reading note, calculation, or executed check, appropriate to the dimension. A reviewer statement can substantiate `L`; a web URL alone cannot establish naturalness of every sentence.
- `F=NOT_APPLICABLE` must not hide a factual promise or action consequence. Explain the reason in `result_note`.
- Retain records for changed/removed items with their disposition when needed to explain previous coverage. Do not count retired records in the current denominator.
- Every default/target mismatch and every app-owned non-resource item needs a recorded disposition.
- V evidence and content acceptance are separate. Existing pending V rows stay unchanged unless an authorized check establishes a new result. No automatic PASS or N/A conversion is permitted; no rendering registry needs to be cleared for a language pass.
- A concrete unresolved runtime concern must name the affected content item, question, and acceptance impact. A missing screenshot without such a concern is only an evidence limitation.

### 12.4 Finding template

```markdown
### LA-<locale>-B<nn>-<sequence>: <specific problem>

- Classification: CONFIRMED_DEFECT / NEEDS_EVIDENCE / STYLE_PREFERENCE / NOT_A_DEFECT
- Category: TERM / FACT / LANGUAGE / CONTEXT / RESOURCE / RENDERING
- Severity: S1 / S2 / S3 / S4
- State: OPEN / VERIFIED / NOT_APPLIED
- Resource key/member and file:
- Variant and every affected context:
- Original text or data:
- User-visible consequence:
- Evidence and reasoning: the specific defect or unresolved question; preference alone is not a defect.
- Proposed/final correction:
- Why existing wording is not acceptable and this correction is necessary:
- Shared-code or other-language impact:
- Checks actually performed, with version and result:
- Remaining uncertainty or dependency:
```

A single finding can cover several occurrences of the same proven issue, but list all affected keys and contexts. Keep unrelated problems separate. Leave fixes OPEN until checked; explain pending evidence, shared dependencies, or replacement findings in the notes. Preserve historical state labels and links rather than migrating old records.

## 13. Classification and severity

| Severity | Meaning | Examples | Acceptance consequence |
| --- | --- | --- | --- |
| S1 | Serious incorrect instruction, calculation meaning, or destructive consequence. | Wrong stitch operation/direction; materially wrong measurement; replacement presented as harmless addition. | Blocks acceptance until corrected and verified. |
| S2 | Clear terminology, factual, contextual, or language error that misleads or obstructs use. | Row/round meaning changed; gauge basis unclear/wrong; action label describes a different operation; missing translation in a reachable state. | Blocks acceptance until corrected and verified. |
| S3 | Confirmed smaller language or formatting defect affecting correctness or comprehension. | Grammatical error, accidental mixed language, clear typo, truncation of necessary wording. | Must be resolved for strict language acceptance; do not dismiss it merely because the app remains usable. |
| S4 | Optional preference when existing wording is correct, natural, and clear. | Two equally acceptable formulations. | Does not block acceptance; keep the existing wording. |

Severity and certainty are separate. A potentially serious but unverified claim is `NEEDS_EVIDENCE`, not a confirmed S1 defect. Record its possible impact while resolving the evidence. Unresolved material uncertainty prevents a strict pass even when no confirmed defect has yet been established.

Correcting a confirmed defect requires verification of the final wording. Keep it OPEN until the relevant check supports VERIFIED. A style preference is NOT_APPLIED and does not justify an edit.

## 14. Existing checks and how to use them

### 14.1 Baseline resource checks

The following existing tests were found during preparation. None was executed as part of writing this plan.

| Existing test | Relevant coverage | Limitation |
| --- | --- | --- |
| `com.finnvek.knittools.ReferenceResourceLocalizationTest` | Translatable key/type parity across configured directories, duplicate keys, alias resolution, plural `other` presence. | Does not read naturalness or establish technique facts. |
| `com.finnvek.knittools.ui.CountPluralResourcesSourceTest` | Selected count resources and expected plural-resource usage in source. | Selected keys/callers only; does not validate all quantity wording or runtime grammar. |
| `com.finnvek.knittools.InstructionHintLocalizationTest` | Advertised localized parser examples and relevant quote escaping. | Tests those examples, not arbitrary language input or every technical claim. |
| `com.finnvek.knittools.MeasurementsLocalizationTest` | Measurement resource parity, placeholders, quantities, and selected presentation contracts. | Resource checks do not replace calculator/factual review. |
| `com.finnvek.knittools.PatternReadingAssistanceLocalizationTest` | Reading-assistance key coverage. | Does not prove wording correctly describes a rendered action. |
| `com.finnvek.knittools.ProjectDocumentLocalizationTest` | Project-document resource coverage. | Does not establish document-operation consequences. |
| `com.finnvek.knittools.WebPatternLocalizationArchitectureContractTest` | Web-pattern resources, placeholders, and selected implementation contracts. | Does not prove target-language clarity or all runtime behavior. |
| `com.finnvek.knittools.RavelryLocalizationSourceTest` | Selected PDF/Ravelry/saved-pattern copy contracts. | Inspect its current assertions; historical naming is not proof the removed API flow exists. |
| `com.finnvek.knittools.ProLocalizationSourceTest` | Selected Pro resource, quantity, placeholder, and separator contracts. | Does not verify real purchases or linguistic quality. |

After authorized resource corrections, a small common validation command is:

```powershell
.\gradlew.bat :app:processDebugResources :app:testDebugUnitTest `
  --tests 'com.finnvek.knittools.ReferenceResourceLocalizationTest' `
  --tests 'com.finnvek.knittools.ui.CountPluralResourcesSourceTest' `
  --console=plain --no-daemon
```

Add affected-area tests from the tables, not every test in the repository after every sentence edit. The source-only preparation check is not a substitute for Android resource compilation after changing text.

### 14.2 Behavior and presentation checks by area

These are existing classes to select when the changed scope warrants them. Except where a full name is shown, resolve the class under `app/src/test/java/com/finnvek/knittools/` and use its actual package in `--tests`.

| Area | Existing checks to inspect/select |
| --- | --- |
| Projects and main counters | `ui.ProjectLocalizationSourceTest`, `ui.screens.counter.CounterStringResourcesSourceTest`, `domain.calculator.CounterValueFormatterTest`, `ui.components.MainCounterTargetStatusTest` |
| Locale-sensitive output | `ui.LocaleSensitiveFormattingSourceTest`, `ui.components.LocaleFormattingTest`, `domain.calculator.LocaleNumberFormatterTest`, `ui.screens.insights.InsightsRowFormattingTest` |
| Durations and pace | `domain.calculator.DurationDisplayFormatterTest`, `domain.calculator.MinutesPerRowFormatterTest` |
| Measurement calculations | `domain.calculator.MeasurementCalculatorTest`, `domain.calculator.MeasurementNumberParserTest`, `domain.calculator.MeasurementNumberFormatterTest` |
| Increase/decrease and examples | `domain.calculator.IncreaseDecreaseCalculatorTest`, `domain.calculator.InstructionParserTest`, `InstructionHintLocalizationTest` |
| Cast-on and yarn | `domain.calculator.CastOnCalculatorTest`, `domain.calculator.YarnEstimatorTest`, `domain.calculator.YarnUsageCalculatorTest`, `domain.model.StandardYarnWeightTest` |
| Reference data | `domain.calculator.AbbreviationDataTest`, `ChartSymbolDataTest`, `NeedleSizeDataTest`, `SizeChartDataTest`, `ReferenceDataSourceTest` in the same calculator package |
| Reference screen contracts | `ui.screens.needles.NeedleSizeLabelTest`, `ui.screens.sizecharts.SizeChartScreenSourceTest` |
| Language mapping | `data.datastore.AppLanguageTest`, `data.datastore.AppLanguageMigrationSourceTest` |

`ReferenceDataSourceTest` checks for source URLs in code; it is not independent validation of each fact. Some resource tests inspect only `strings.xml`, while others combine all XML files. Read the owning test before claiming coverage of a moved or separately stored resource.

Device-side tests also exist, including `ui.screens.counter.YarnUsageLocalizationTest` and `ui.screens.needles.NeedleSizeRuntimeTest` under `app/src/androidTest/`. Inspect their setup, data effects, assumptions, and supported variants before selecting them. Their existence does not authorize a broad instrumentation run or imply that every language is covered.

### 14.3 Validation discipline

1. Use the existing JDK 17/Gradle toolchain. Do not upgrade tools to facilitate this audit.
2. Record exact command, start/end, exit code, relevant test totals, input identity, and actual task outcomes.
3. Distinguish test execution from `UP-TO-DATE`/`FROM-CACHE`. Do not label reused results as a new execution. Force a targeted rerun only when fresh execution is necessary and otherwise unavailable.
4. A failed command, unavailable report, or test startup failure is not a pass. Diagnose the owning scope and rerun when possible.
5. If another chat changes an input during a check, discard the affected result and rerun after the inputs are stable.
6. Run debug assembly only when needed for the planned rendered check or a relevant build concern. A doc-only change does not require an APK build.
7. Add/update a focused test only for a real uncovered regression mechanism, such as argument reversal or a changed parser example. Do not add tests whose only assertion is that every edited sentence equals its new spelling.
8. Fix regressions introduced by the authorized work. Record unrelated pre-existing failures separately; do not weaken checks to get green output.
9. Do not use mocks as proof of actual database commit/rollback or device rendering. Such behavior is outside a normal text edit; escalate its scope if relevant.
10. Stop validation once the changed behavior and required language checks are adequately proven. No unrelated scans, dependency checks, benchmarks, or broad cleanup.

## 15. Change and resume protocol

### 15.1 Before resuming a language

- Read its `review.md`, `coverage.tsv`, and the current shared handoffs.
- Compare current input hashes/diffs to the recorded snapshot.
- Preserve valid completed work. Do not start the entire language again just because the conversation is new.
- Mark only affected current content reviews `PENDING` with the stale-input reason, including changed contexts in an otherwise unchanged translation. Preserve prior results as history.
- If resources were added, add inventory rows and batch ownership. If removed or moved, reconcile their disposition without losing findings.
- Continue from saved records only within the new task's explicit boundary. A historical resume point never overrides a stop instruction or authorizes another screen/task. Preserve the stopped English rendering handoff unless the user separately requests that work.

### 15.2 When a shared meaning changes

Record a semantic change notice containing:

```text
Shared change ID:
Owning chat/scope:
File/member and changed resource keys:
Previous meaning:
Corrected meaning and evidence:
Affected languages/batches:
Required rechecks:
Shared validation result:
Per-language acknowledgement/status:
```

A default-English correction can require a translation recheck even if no placeholder or key changed. Conversely, an English spelling correction with no semantic effect does not automatically invalidate every language. Explain the affected scope.

The coordination chat updates shared summaries after reading the language reports and current files. Do not overwrite another chat's summary with an older copy of this document. User-owned chat creation and cross-chat messaging remain subject to the user's explicit instructions.

## 16. Acceptance gates and stopping conditions

### 16.1 Batch acceptance

A batch is accepted only when:

- Its exact owned keys, every variant, code-backed items, factual claims, terms, and materially distinct contexts are recorded and reviewed.
- Applicable `T`, `F`, `L`, `C`, and `R` checks are complete against current inputs.
- No confirmed S1-S3 language defect or specific unresolved question affecting terminology, facts, meaning, or comprehension remains, including shared dependencies or stale content evidence.
- Any necessary targeted runtime check has resolved its named language concern; section 11.2 is the only language-driven runtime requirement.
- Authorized fixes have a specific justification and have been re-read and verified against their final version.
- Exclusions and `NOT_APPLICABLE` content decisions have concrete reasons.

A missing screenshot or incomplete exhaustive rendering matrix is not an acceptance blocker. Do not require all V rows to be PASS/N/A. This does not clear confirmed truncation or any other concrete comprehension issue, and does not claim unverified rendering was tested.

### 16.2 Language acceptance

Use these outcomes for new language decisions:

| Outcome | Meaning |
| --- | --- |
| `NOT_STARTED` | No content review has been recorded. |
| `IN_PROGRESS` | Some content remains unreviewed; a bounded task may already be complete. |
| `REVIEW_COMPLETE / OPEN` | Content has been reviewed, but specific defects or unresolved language questions remain. |
| `AUDIT_PASS` | Full current content coverage, applicable checks complete, and no confirmed defect or specific unresolved language question remains. This is language acceptance, not exhaustive UI certification. |

Keep older outcomes such as English `CONTENT_REVIEW_COMPLETE / RUNTIME_PENDING` as historical recorded decisions under the prior policy. Do not migrate them automatically to a new verdict. A separately assigned acceptance review must reconcile current content evidence and specific unresolved questions before granting a language pass. This closeout records the existing independent `AUDIT_PASS` decisions and the narrow migration reconciliation in section 4.4; it does not conduct a new full language audit or replace historical evidence.

Report native-speaker validation separately for every outcome. `AUDIT_PASS; native review NOT_REQUESTED` is not native craft-expert sign-off. Neither is a guarantee that no undiscovered error exists.

Before `AUDIT_PASS`, reconcile:

1. Current translatable keys against the inventory, not a remembered baseline count.
2. Every plural/array variant, alias, materially different caller/argument context, code-backed text, factual claim, and knitting/crochet term.
3. Cumulative B01-B23 content coverage across saved tasks and its final-version evidence.
4. Confirmed findings and specific unresolved language questions, including those requiring targeted runtime inspection.
5. Shared semantic changes since review and only their affected language items.
6. Relevant technical checks after authorized corrections. Preserve supplementary rendering results and limitations separately.
7. The final diff and existing work, ensuring only authorized changes.
8. Temporary processes started for the task, stopping those no longer needed.

### 16.3 Campaign acceptance and task boundaries

The original campaign is complete when all eleven original languages independently meet section 16.2 and affected shared semantic changes have been reconciled. A global key-parity test, English approval, or another language's native review cannot approve a translation. Each language needs its own terminology, factual-expression, naturalness, and context review; it does not need to repeat English's rendering campaign.

Every chat has one explicitly bounded task. Stop after completing and reporting that scope, even if more of the language or campaign remains. If blocked, complete only independent work inside the assigned boundary, record the specific blocker and resume point, and stop. Do not automatically start another screen, batch, language, chat, or exhaustive rendering campaign. Never turn an unresolved item into PASS merely to finish.

## 17. Campaign tracker

**Original campaign decision: COMPLETE — language content (2026-10-19).** The latest acceptance and closeout sections in all eleven linked reports are controlling. Retained historical OPEN/RUNTIME_PENDING results, older preparation counts and old shared handoff labels are not current blockers. Each language's independent review covers B01-B23, applicable T/F/L/C/R, its code-backed content and materially distinct meanings; no native review is implied.

The separately authorized twelfth locale, pt-BR, is also complete and accepted. Original locale decisions are retained; the current denominator includes their one added native option label.

### Locale decisions and saved scope

| Locale / decision source | Completed scope and decision | Current translatable keys / variants | Saved audit variants and coverage rows (not rewritten) | Native review | Open content blocker |
| --- | --- | ---: | --- | --- | --- |
| [en](config/language-audit/en/review.md) | B01-B23 complete; AUDIT_PASS | 1,350 / 1,382 | 1,380 translatable + 2 numeric = 1,382; 2,315 rows | NOT_REQUESTED | None |
| [fi](config/language-audit/fi/review.md) | B01-B23 complete; AUDIT_PASS | 1,350 / 1,382 | 1,380 translatable (+2 numeric in coverage); 2,315 rows | NOT_REQUESTED | None |
| [sv](config/language-audit/sv/review.md) | B01-B23 complete; AUDIT_PASS | 1,350 / 1,382 | 1,380 translatable; 1,826 rows | NOT_REQUESTED | None |
| [nb](config/language-audit/nb/review.md) | B01-B23 complete; AUDIT_PASS | 1,350 / 1,382 | 1,380 translatable; 1,626 rows | NOT_REQUESTED | None |
| [da](config/language-audit/da/review.md) | B01-B23 complete; AUDIT_PASS | 1,350 / 1,382 | 1,380 translatable; 1,649 rows | NOT_REQUESTED | None |
| [de](config/language-audit/de/review.md) | B01-B23 complete; AUDIT_PASS | 1,350 / 1,382 | 1,380 translatable; 1,619 rows | NOT_REQUESTED | None |
| [nl](config/language-audit/nl/review.md) | B01-B23 complete; AUDIT_PASS | 1,350 / 1,382 | 1,380 translatable; 1,816 rows | NOT_REQUESTED | None |
| [fr](config/language-audit/fr/review.md) | B01-B23 complete; AUDIT_PASS | 1,350 / 1,414 | 1,411 translatable; 1,854 rows | NOT_REQUESTED | None |
| [it](config/language-audit/it/review.md) | B01-B23 complete; AUDIT_PASS | 1,350 / 1,414 | 1,411 translatable; 1,883 rows | NOT_REQUESTED | None |
| [es](config/language-audit/es/review.md) | B01-B23 complete; AUDIT_PASS | 1,350 / 1,414 | 1,413 after repair; 2,110 current rows + 2 retired failures | NOT_REQUESTED | None |
| [pt](config/language-audit/pt/review.md) | B01-B23 complete; AUDIT_PASS | 1,350 / 1,414 | 1,413 after migration; 2,036 rows | NOT_REQUESTED | None |
| [pt-BR](config/language-audit/pt-BR/review.md) | B01-B23 complete; AUDIT_PASS | 1,350 / 1,414 | 1,414 resource variants + 20 grouped contexts; 1434 rows | NOT_REQUESTED | None |

The pre-migration variant counts in nine saved audits are reconciled by section 4.4, not rewritten as fresh reviews. Spanish's original audit had 1,411 variants / 2,101 rows; its verified month closeout supplies the current figures above and retains two failed historical rows. Coverage-row totals include differing context/code groupings and are not comparable measures of review quality. They are not recalculated by adding the XML variant delta to old context rows.

**Supplementary rendering:** English retains **1,858 PASS / 231 BLOCKED / 226 N/A** across its 2,315 saved rows, with **1,177 original registry IDs / 231 pending cases**, including all 15 frozen Insights cases. Its saved content dimensions remain T/L/C 2,315 PASS; F 2,304 PASS / 11 N/A; R 1,879 PASS / 436 N/A. German retains 44 V PASS rows for reused shared glyph evidence and 1,575 blank V cells; all V cells in the other nine locale ledgers remain blank. None of those blanks is a rendering pass. Spanish's narrow month Compose test remains scoped production-context evidence in its closeout, without relabelling its blank V cells. No registry, screenshot, rendering result or native-review status changed here.

### Shared handoff reconciliation

The ten original shared findings, B03-04/B19-02 follow-ups, B03-05 stop-summary assertion and B08-01 measurement-helper repair are verified in the saved English and shared reports. The later locale reports independently reconcile their affected wording and meanings, with shared technical evidence reused within its limits. The older `LOCALE_RECHECK_REQUIRED` and `ENGLISH_RECHECK_REQUIRED` labels in retained shared history describe work subsequently completed; they are not new instructions. The source reports and their earlier failures remain intact.

**LA-en-B05-01:** the saved correction bounds the active-date set and All Time presentation start to the existing today-ended calendar range, preserving completed-session durations/zones and project filters. The report records **107/107 JVM tests in 11 suites and scoped ktlint PASS**, plus the six affected English composition rereads. Later locale reviews reconcile the corrected date/count meaning. No remaining content blocker or new Android rendering/spoken-output claim follows from this closeout.

**LA-es-B05-58:** `relative_time_months_ago` became a plural in all eleven original locales; the shared monthly caller selects and formats the same `months = days / 30` count. Spanish one is `hace %1$d mes`, many/other are `hace %1$d meses`; all other locale forms retain their previously accepted wording. The saved repair records **6/6 JVM tests, Android resource processing, scoped ktlint and 1/1 production-context Android Compose test PASS**. Both last-worked contexts cover 30/59/60/90 days, with day/week controls unchanged. These are **existing executions, not checks rerun here**. The exact +15-variant mapping and current definition/hash comparison are in section 4.4. Spanish independently accepted the changed forms; Portuguese reviewed the migrated resource in its later audit; the remaining nine acceptances are carried forward only through this documented narrow mapping.

| ID | Resolved issue | Current campaign disposition | Remaining action |
| --- | --- | --- | --- |
| LA-en-B03-02 | Neutral counter units in sessions, photos, reminders, reader and Insights | VERIFIED; affected locale content reconciled | None |
| LA-en-B03-03 | Compact widget clipped count | VERIFIED; shared layout evidence retained, locale meaning reconciled | None |
| LA-en-B06-03 | Unsupported bare ply hints removed | VERIFIED; removed presentation reconciled, original mapping not newly proven | None |
| LA-en-B06-04 | Care glyph geometry and labels | VERIFIED; affected locale pairings reconciled | None |
| LA-en-B14-01 | M1 bars at edges/dense input | VERIFIED; affected locale instructions/errors reconciled | None |
| LA-en-B15-02 | Nearest-repeat rounding | VERIFIED; affected locale calculation meaning reconciled | None |
| LA-en-B16-03 | Uppercased gram symbol | VERIFIED; lowercase mass unit and whole-package meaning reconciled | None |
| LA-en-B17-02 | JP diameter associations | VERIFIED; current table and locale labels reconciled | None |
| LA-en-B17-03 | Sizing values, categories and ranges | VERIFIED; current data and locale meanings reconciled | None |
| LA-en-B19-01 | Chart glyph meanings and cable spans | VERIFIED; adopted convention and locale pairings reconciled | None |
| LA-en-B03-04 | Historical rounds relabelled as rows after current-label edit | VERIFIED; historical row/pace suppression reconciled | None |
| LA-en-B19-02 | KFB chevron and overlapping P2tog dot | VERIFIED; corrected glyphs and paired wording reconciled | None |
| LA-en-B03-05 | Stop-summary assertion expected retired row wording | ENGLISH_VERIFIED; test-only correction | None |
| LA-en-B08-01 / EN-V-0621 | Reconstructed paragraph width caused a false clipping assertion | TEST_FIX_VERIFIED / ENGLISH_VERIFIED; later English receipt reconciliation complete | None |
| LA-en-B05-01 | Stored-zone dates contradicted active-day counts and All Time range | VERIFIED; English repair closeout and later locale compositions reconciled | None |
| LA-es-B05-58 | Month string received plural counts | VERIFIED; Spanish closeout and all eleven resource definitions reconciled | None |

Evidence: [shared corrections and retained history](config/language-audit/shared-fixes.md), the eleven locale reports linked above, and [exact month migration receipt](config/language-audit/es/month-fix-verification.json). This campaign summary does not extend old checks to untested UI states or renew unrelated evidence.

### Historical campaign-closeout validation and stopping point

The earlier documentation-only closeout read the then-current instructions, all locale decisions/closeouts and shared-fix records; enumerated current resource counts; compared the eleven migrated definitions with their saved receipt; checked saved coverage counts, batch membership and V statuses; and checked this document for contradictory status/next-action text. These are inventory/document comparisons, not a repeated language review, build, test suite, runtime run or exhaustive source audit. Historical input manifests remain unchanged even where this coordinator-owned summary now differs.

**Unresolved content blockers: none. Next campaign action: none.** All eleven acceptances and relevant shared changes reconcile. Native-speaker review remains **NOT_REQUESTED** everywhere. Exhaustive UI certification is outside this completion; English's 231 unverified supplementary cases remain stopped and do not constitute unfinished language-audit obligations. Stop after this closeout. No additional task, audit, rendering continuation, commit, push or pull request is authorized.

### pt-BR implementation closeout

The new locale passed XML/format/plural checks, debug APK and instrumentation APK assembly, **84 JVM tests** and **5 Android instrumentation tests** on API 36. Direct UI interaction verified distinct native options, pt-BR selection and Brazilian copy, force-stop/relaunch persistence, pt and System-default switching, and selection of Brasil in Android Settings followed by the matching checked KnitTools option. Android resource tests additionally cover zero/one/multiple/million quantities, fractional packages, dates, decimal/grouping separators and distinct pt wording. Earlier Android versions were not exercised. No exhaustive rendering, 320 dp, 200% font or TalkBack run was performed. Native review remains NOT_REQUESTED.

The language acceptance, exact technical scope and limitations are recorded separately in [pt-BR/review.md](config/language-audit/pt-BR/review.md) and [validation.json](config/language-audit/pt-BR/validation.json). No concrete unresolved content or integration defect remains. The temporary read-only emulator was stopped. No commit, push, PR, or further campaign work was performed.

## 18. Reusable English start prompt for one bounded language task

Use this English-language prompt only when the user assigns a task. Replace `<language>`, `<locale>`, and `<explicit boundary>` before execution. The boundary may name batches, keys, specific questions, or acceptance reconciliation; it is not automatically a whole language or one screen. If no material boundary can be determined from the request, clarify it before audit execution. Sequential tasks for the same language use the same saved records.

This prompt authorizes scoped corrections when used as written. For review only, replace that authorization with an explicit no-edit instruction. It grants no action during this documentation task. For English continuation, preserve the stopped InsightsScreen handoff; do not resume the rendering campaign.

```text
Audit and correct KnitTools in <language> (<locale>) only, following
LANGUAGE_AUDIT.md. This chat has one task: <explicit boundary>.
Complete that assigned scope and stop. Do not automatically continue to another
screen, batch, language, or task.

Read AGENTS.md, CODEX.md, relevant wording rules in CLAUDE.md, the audit plan,
shared handoffs, and this locale's saved review, coverage, and input records.
Inspect the branch, working tree, and relevant implementation before editing.
Preserve existing work and completed evidence. Resume only the assigned items;
do not restart completed content review or override a saved user stop.

Strictly review every translatable resource and plural/array variant within
the boundary, relevant code-generated text, factual claim, and knitting/crochet
term. Inspect actual callers when meaning or argument semantics differ.
Read the target language independently for facts, naturalness, and contextual
meaning; English approval never approves another language. Use traceable
specialist sources for technical or disputed claims. Keep cumulative coverage
complete across tasks, with shared rows for repeated identical uses.

Keep wording that is correct, natural, and understandable. Alternative phrasing
or reviewer preference is not a defect. Explain the specific problem and why
each proposed correction is necessary; make only minimal authorized corrections.
Do not normalize all wording to a new style or edit other locales. Record shared
code/data/test needs as concrete handoffs without expanding this task's scope.

Use source/context review and valid existing evidence first. Perform targeted
runtime inspection only to resolve a concrete language concern that those
cannot settle, such as ambiguous action meaning, dynamic number/unit composition,
or suspected truncation of important instructions. Reuse valid evidence for
unchanged shared layouts. Do not require screenshots, fixtures, runtime execution,
or separate rows for every occurrence, caller, screen, error or transient state.
Do not repeat the English rendering campaign for another language.

Confirmed defects and specific unresolved questions affecting terminology, facts,
meaning, or comprehension block acceptance. Missing screenshots alone do not.
Keep unverified rendering cases unverified, including the 231 retained English
cases; never relabel them PASS or N/A merely because the policy changed. Preserve
historical results and input records. A bounded task does not automatically grant
whole-language AUDIT_PASS; apply section 16 only when acceptance is assigned.

Update only the assigned locale items and their existing audit records under
config/language-audit/<locale>/. Use reports/language-audit/<locale>/<run-id>/
only for raw evidence when needed. The coordination task owns the root plan.
Record exact completed items, necessary checks actually run, specific unresolved
questions, unchanged evidence reused, and the stopping/resume point.
No per-screen registry or screenshot campaign is required.

Run the smallest relevant checks after corrections. Do not repeat 320 dp,
200% font scale, their combination, or TalkBack. Do not run restricted wrappers,
upgrade dependencies, redesign UI, or change real user data. No commit, push,
PR, new chats, agents, or messages to other chats. Stop task-owned temporary
processes when finished.

Report concisely in Finnish: coverage, changes and reasons, actual checks,
language blockers separately from unverified rendering, native-review status,
and evidence paths. Do not claim native-speaker review without actual evidence.
Keep durable audit documents in English and quoted app text in its language.
Stop at the assigned boundary.
```

The language report must be self-contained enough for the coordination chat to assess it from files. A completion message such as “all translations fixed” without coverage and validation evidence is not an accepted deliverable.
