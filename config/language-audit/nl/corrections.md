# Dutch corrections — 2026-10-19

69 resource keys / 70 declared text variants. Baseline is the entry working tree, not HEAD. Existing Dutch edits are preserved in entry-dutch.diff and entry-texts.json. Each replacement below was re-read and checked after application. No change is justified solely by consistency with another locale.

All findings: CONFIRMED_DEFECT; final state VERIFIED. Only the named Dutch text is changed. No shared implementation/other-language impact or remaining dependency. Verification: final XML/placeholder/alias comparison, processDebugResources, ReferenceResourceLocalizationTest (2 tests), CountPluralResourcesSourceTest (1 test), final scope/hash comparison; see validation.json and validation-gradle.log. Caller snapshots in caller-inventory.json and final input hashes establish the version. Runtime and native review were not performed.

Each replacement is limited to the defective term, clause or missing distinction. Where an instruction needed a longer replacement, the added words identify the stitch, orientation or operation omitted/misstated by the original.

## LA-nl-B03-001: `counter_decrease` (default)

- Classification: CONFIRMED_DEFECT / CONTEXT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Minderen
- Problem, consequence and necessity: Generic minus control changes yarn quantity, secondary count, widget count and PDF value; stitch-specific wording misidentifies the operation.
- Smallest justified replacement: Verlagen
- Evidence: CALLERS; READ-B03; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt:526; app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt:811; app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt:2486; app/src/main/java/com/finnvek/knittools/ui/screens/yarncard/YarnCardDetailScreen.kt:344

## LA-nl-B03-002: `counter_increase` (default)

- Classification: CONFIRMED_DEFECT / CONTEXT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Meerderen
- Problem, consequence and necessity: Generic plus control changes yarn quantity, secondary count, widget count and PDF value; stitch-specific wording misidentifies the operation.
- Smallest justified replacement: Verhogen
- Evidence: CALLERS; READ-B03; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt:527; app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt:826; app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt:2492; app/src/main/java/com/finnvek/knittools/ui/screens/yarncard/YarnCardDetailScreen.kt:352

## LA-nl-B04-003: `reminder_repeat_occurrence_format` (one)

- Classification: CONFIRMED_DEFECT / LANGUAGE / S3; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Herhaal #%1$d, interval %2$d
- Problem, consequence and necessity: This labels a numbered occurrence, not an imperative to repeat. Both declared quantities need the noun.
- Smallest justified replacement: Herhaling #%1$d, interval %2$d
- Evidence: CALLERS; READ-B04; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt:248

## LA-nl-B04-004: `reminder_repeat_occurrence_format` (other)

- Classification: CONFIRMED_DEFECT / LANGUAGE / S3; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Herhaal #%1$d, interval %2$d
- Problem, consequence and necessity: This labels a numbered occurrence, not an imperative to repeat. Both declared quantities need the noun.
- Smallest justified replacement: Herhaling #%1$d, interval %2$d
- Evidence: CALLERS; READ-B04; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt:248

## LA-nl-B05-005: `insights_empty_title` (default)

- Classification: CONFIRMED_DEFECT / CONTEXT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Nog geen tijd op de naalden.
- Problem, consequence and necessity: The empty Insights range also covers crochet; the needle-only description excludes that supported craft.
- Smallest justified replacement: Nog geen werksessies.
- Evidence: CALLERS; READ-B05; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsSections.kt:565

## LA-nl-B06-006: `care_wash_30` (default)

- Classification: CONFIRMED_DEFECT / CONTEXT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: 30°
- Problem, consequence and necessity: Standalone accessible care label omits both washing and Celsius; a bare degree value does not identify the operation.
- Smallest justified replacement: Wassen op 30 °C
- Evidence: S-CARE;CALLERS; READ-B06; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt:11

## LA-nl-B06-007: `care_wash_40` (default)

- Classification: CONFIRMED_DEFECT / CONTEXT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: 40°
- Problem, consequence and necessity: Standalone accessible care label omits both washing and Celsius; a bare degree value does not identify the operation.
- Smallest justified replacement: Wassen op 40 °C
- Evidence: S-CARE;CALLERS; READ-B06; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt:12

## LA-nl-B06-008: `care_wash_60` (default)

- Classification: CONFIRMED_DEFECT / CONTEXT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: 60°
- Problem, consequence and necessity: Standalone accessible care label omits both washing and Celsius; a bare degree value does not identify the operation.
- Smallest justified replacement: Wassen op 60 °C
- Evidence: S-CARE;CALLERS; READ-B06; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt:13

## LA-nl-B06-009: `care_dryclean_any` (default)

- Classification: CONFIRMED_DEFECT / CONTEXT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Alle oplosmiddelen
- Problem, consequence and necessity: Standalone label names substances but not the professional dry-cleaning process represented by A.
- Smallest justified replacement: Chemisch reinigen (alle oplosmiddelen)
- Evidence: S-CARE;SHARED; READ-B06; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt:35

## LA-nl-B06-010: `care_dryclean_p` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: P-oplosmiddel
- Problem, consequence and necessity: P/F mark professional cleaning categories, not individual solvents named P or F.
- Smallest justified replacement: Chemisch reinigen (P)
- Evidence: S-CARE;CALLERS; READ-B06; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt:36

## LA-nl-B06-011: `care_dryclean_f` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: F-oplosmiddel
- Problem, consequence and necessity: P/F mark professional cleaning categories, not individual solvents named P or F.
- Smallest justified replacement: Chemisch reinigen (F)
- Evidence: S-CARE;CALLERS; READ-B06; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt:37

## LA-nl-B07-012: `yarn_usage_scope` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Houdt alleen het garen bij dat voor dit project is gereserveerd. De hoeveelheden in Mijn garen blijven ongewijzigd.
- Problem, consequence and necessity: Planned, allocated and used quantities are independent; allocation is not required. Existing wording wrongly restricts tracking to reserved yarn.
- Smallest justified replacement: Houdt alleen het garenverbruik voor dit project bij. De hoeveelheden in Mijn garen blijven ongewijzigd.
- Evidence: CALLERS; READ-B07; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/screens/counter/ProjectYarnUsageSheet.kt:102

## LA-nl-B14-013: `increase_decrease_error_total_out_of_range` (default)

- Classification: CONFIRMED_DEFECT / LANGUAGE / S3; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Het resultaat is te groot. Gebruik kleinere steekenaantallen.
- Problem, consequence and necessity: Misspells steken as steeken inside the compound.
- Smallest justified replacement: Het resultaat is te groot. Gebruik kleinere stekenaantallen.
- Evidence: S-TAAL; READ-B14; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/screens/increase/IncreaseDecreaseScreen.kt:242

## LA-nl-B15-014: `stitch_gauge` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Stekenproef steken
- Problem, consequence and necessity: Stekenproef steken does not name the numerical density requested by the field; its adjacent unit supplies the measurement basis.
- Smallest justified replacement: Stekenverhouding
- Evidence: S-KATIA;CALLERS; READ-B15; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/screens/caston/CastOnScreen.kt:124

## LA-nl-B16-015: `total_weight` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Totaal gewicht: %s g
- Problem, consequence and necessity: Calculator multiplies rounded-up whole skeins by package weight. Totaal gewicht does not distinguish purchased yarn from estimated consumption.
- Smallest justified replacement: Gewicht van hele strengen: %s g
- Evidence: CALLERS; READ-B16; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/screens/yarn/YarnEstimatorScreen.kt:201

## LA-nl-B16-016: `extra_skein_note` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Koop altijd één extra streng — verfbaden kunnen verschillen
- Problem, consequence and necessity: Different dye lots justify considering spare yarn, not an unconditional instruction to buy exactly one extra skein for every project.
- Smallest justified replacement: Overweeg een extra streng — verfbaden kunnen verschillen
- Evidence: CALLERS; READ-B16; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/screens/yarn/YarnEstimatorScreen.kt:217

## LA-nl-B17-017: `size_col_arm` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Arm
- Problem, consequence and necessity: Source measurement runs from underarm to wrist, not the entire arm.
- Smallest justified replacement: Armlengte tot oksel
- Evidence: SHARED;S-CYC-BODY; READ-B17; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:50

## LA-nl-B17-018: `tip_needle_sizes_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Metrisch (mm) is de universele standaard. US-maten gebruiken een nummersysteem (0–50). UK/Canadese maten gebruiken een ander nummersysteem. Japanse maten hebben hun eigen nummering. Controleer altijd de maat in mm om zeker te zijn.
- Problem, consequence and necessity: Universal-standard claim is too strong and US range 0–50 excludes current 000/00 entries. Retain conversion advice without inventing a fixed system range.
- Smallest justified replacement: Metrische maten geven de diameter in mm aan. US-maten gebruiken een nummersysteem. UK/Canadese maten gebruiken een ander nummersysteem. Japanse maten hebben hun eigen nummering. Controleer altijd de maat in mm om zeker te zijn.
- Evidence: SHARED;CALLERS; READ-B17; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/screens/needles/NeedleSizeScreen.kt:78

## LA-nl-B18-019: `abbr_k_meaning` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei
- Problem, consequence and necessity: K means knit stitch, whereas Brei means knitting generally and fails to distinguish K from P.
- Smallest justified replacement: Recht
- Evidence: S-CYC-ABBR;S-KATIA; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:16

## LA-nl-B18-020: `abbr_sts_meaning` (default)

- Classification: CONFIRMED_DEFECT / LANGUAGE / S3; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Steek(en)
- Problem, consequence and necessity: Steek(en) expands to the misspelling steeken.
- Smallest justified replacement: Steek/steken
- Evidence: S-TAAL; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:18

## LA-nl-B18-021: `abbr_pwise_desc` (default)

- Classification: CONFIRMED_DEFECT / LANGUAGE / S3; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Steek de naald in alsof u hem averecht wilt maken
- Problem, consequence and necessity: Uses u instead of established je and an unclear hem; match the adjacent kwise instruction.
- Smallest justified replacement: Steek de naald in alsof je averecht gaat breien
- Evidence: S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:23

## LA-nl-B18-022: `abbr_k2tog_meaning` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei er 2 samen
- Problem, consequence and necessity: Omits knitwise operation; breien alone does not distinguish it from p2tog.
- Smallest justified replacement: Brei 2 steken recht samen
- Evidence: S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:25

## LA-nl-B18-023: `abbr_k2tog_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Naar rechts leunende mindering: brei twee steken als één
- Problem, consequence and necessity: Omits knitwise operation in an instruction for K2tog.
- Smallest justified replacement: Naar rechts leunende mindering: brei twee steken recht als één
- Evidence: S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:25

## LA-nl-B18-024: `abbr_ssk_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S1; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Naar links leunende mindering: haal 2 steken recht af, brei ze samen door de achterste lussen
- Problem, consequence and necessity: Must distinguish separately slipped stitches from jointly slipped CDD stitches; retain direction and loop information.
- Smallest justified replacement: Naar links leunende mindering: haal 2 steken één voor één recht af, brei ze recht samen door de achterste lussen
- Evidence: S-CYC-ABBR;S-BREICLUB-SSK; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:27

## LA-nl-B18-025: `abbr_skp_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Naar links neigende daling, alternatief voor SSK
- Problem, consequence and necessity: Daling names a fall rather than the knitting decrease technique.
- Smallest justified replacement: Naar links neigende mindering, alternatief voor SSK
- Evidence: S-CYC-ABBR;S-KATIA; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:29

## LA-nl-B18-026: `abbr_k3tog_meaning` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei er 3 samen
- Problem, consequence and necessity: Omits knitwise operation; must distinguish p3tog.
- Smallest justified replacement: Brei 3 steken recht samen
- Evidence: S-CYC-SYMBOL; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:30

## LA-nl-B18-027: `abbr_s2kp_meaning` (default)

- Classification: CONFIRMED_DEFECT / FACT / S1; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: 2 afhalen, 1 recht, haal de afgehaalde steken over
- Problem, consequence and necessity: Centered decrease requires slipping the pair together knitwise; unspecified slipping can give a different decrease.
- Smallest justified replacement: 2 steken samen recht afhalen, 1 recht, haal de afgehaalde steken over
- Evidence: S-CYC-ABBR;CALLERS; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:32

## LA-nl-B18-028: `abbr_sk2p_meaning` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: 1 afhalen, 2 recht samen breien, de afgehaalde steek eroverheen halen
- Problem, consequence and necessity: SK2P requires knitwise slipping; unspecified orientation does not completely identify the operation.
- Smallest justified replacement: 1 steek recht afhalen, 2 recht samen breien, de afgehaalde steek eroverheen halen
- Evidence: S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:33

## LA-nl-B18-029: `abbr_dec_meaning` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Verlagen
- Problem, consequence and necessity: This glossary entry names a knitting decrease, not generic value lowering.
- Smallest justified replacement: Minderen
- Evidence: S-CYC-ABBR;S-KATIA; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:36

## LA-nl-B18-030: `abbr_inc_meaning` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Verhogen
- Problem, consequence and necessity: This glossary entry names a knitting increase, not generic value raising.
- Smallest justified replacement: Meerderen
- Evidence: S-CYC-ABBR;S-KATIA; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:43

## LA-nl-B18-031: `abbr_m1_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S1; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Neem de staaf tussen de steken op en brei door de achterste lus om te meerderen
- Problem, consequence and necessity: Staaf is not the yarn strand. Rear-loop instruction without pickup direction can produce a hole; generic M1 must allow the specified left/right variant.
- Smallest justified replacement: Neem de draad tussen de steken op en brei deze gedraaid recht; volg de richting die het patroon aangeeft
- Evidence: S-BREICLUB-M1;S-KATIA-M1;S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:38

## LA-nl-B18-032: `abbr_m1l_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Naar links leunende meerdering met behulp van de balk tussen de steken
- Problem, consequence and necessity: Balk means a beam, not the strand between stitches.
- Smallest justified replacement: Naar links leunende meerdering met behulp van de draad tussen de steken
- Evidence: S-BREICLUB-M1;S-KATIA-M1; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:39

## LA-nl-B18-033: `abbr_m1r_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Meerdering naar rechts met behulp van de balk tussen de steken
- Problem, consequence and necessity: Balk means a beam, not the strand between stitches.
- Smallest justified replacement: Meerdering naar rechts met behulp van de draad tussen de steken
- Evidence: S-BREICLUB-M1;S-KATIA-M1; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:40

## LA-nl-B18-034: `abbr_kfb_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei aan de voor- en achterkant van dezelfde steek om te meerderen
- Problem, consequence and necessity: Must knit into both legs of the same stitch; vague front/back surfaces and omitted recht do not specify that.
- Smallest justified replacement: Brei recht in de voorste en achterste lus van dezelfde steek om te meerderen
- Evidence: S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:41

## LA-nl-B18-035: `abbr_pfb_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei averecht naar de voor- en achterkant van dezelfde steek om te meerderen
- Problem, consequence and necessity: Naar de voor- en achterkant does not specify working into both legs of the same stitch.
- Smallest justified replacement: Brei averecht in de voorste en achterste lus van dezelfde steek om te meerderen
- Evidence: S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:42

## LA-nl-B18-036: `abbr_sl1k_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Haal één steek af alsof u wilt breien
- Problem, consequence and necessity: Uses u instead of je and omits knitwise orientation.
- Smallest justified replacement: Haal één steek af alsof je recht gaat breien
- Evidence: S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:46

## LA-nl-B18-037: `abbr_sl1p_desc` (default)

- Classification: CONFIRMED_DEFECT / LANGUAGE / S3; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Haal één steek af alsof u hem averecht wilt maken
- Problem, consequence and necessity: Uses u instead of je and unclear hem; preserve purlwise orientation.
- Smallest justified replacement: Haal één steek af alsof je averecht gaat breien
- Evidence: S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:47

## LA-nl-B18-038: `abbr_rnd_meaning` (default)

- Classification: CONFIRMED_DEFECT / LANGUAGE / S3; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Rond
- Problem, consequence and necessity: Rond is not the noun for one completed round.
- Smallest justified replacement: Ronde
- Evidence: S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:56

## LA-nl-B18-039: `abbr_ktbl_meaning` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei door de achterste lus
- Problem, consequence and necessity: Omits knitwise operation, necessary to distinguish PTBL.
- Smallest justified replacement: Brei recht door de achterste lus
- Evidence: S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:64

## LA-nl-B18-040: `abbr_ktbl_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Gedraaide gebreide steek
- Problem, consequence and necessity: Gebreide also includes purl stitches and does not identify the knit stitch.
- Smallest justified replacement: Gedraaide rechte steek
- Evidence: S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:64

## LA-nl-B18-041: `abbr_c4b_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Schuif 2 naar CN en houd achterover, K2, K2 vanuit CN
- Problem, consequence and necessity: Achterover means leaning backwards, not holding the cable needle behind the fabric.
- Smallest justified replacement: Schuif 2 steken naar CN en houd deze achter het werk, K2, K2 vanuit CN
- Evidence: S-CYC-SYMBOL; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:68

## LA-nl-B18-042: `abbr_wyif_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S1; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Houd het garen aan de voorkant (averechte kant) van het werk
- Problem, consequence and necessity: Yarn position in front of current work is not necessarily the purl/wrong side of the fabric.
- Smallest justified replacement: Houd het garen aan de voorkant van het werk
- Evidence: S-CYC-ABBR;S-CYC-SYMBOL; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:72

## LA-nl-B18-043: `abbr_wyib_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S1; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Houd het garen tegen de achterkant (gebreide kant) van het werk
- Problem, consequence and necessity: Yarn position behind current work is not necessarily the knit/right side of the fabric.
- Smallest justified replacement: Houd het garen achter het werk
- Evidence: S-CYC-ABBR;S-CYC-SYMBOL; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:73

## LA-nl-B18-044: `abbr_st_st_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei recht aan de goede kant, averecht aan de verkeerde kant (of brei elke naald in de rondte)
- Problem, consequence and necessity: In-the-round branch omits recht, so does not distinguish stockinette from reverse stockinette.
- Smallest justified replacement: Brei recht aan de goede kant, averecht aan de verkeerde kant (of brei elke naald in de rondte recht)
- Evidence: S-KATIA;S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:77

## LA-nl-B18-045: `abbr_g_st_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei elke rij recht (of brei / averecht afwisselende ronden in cirkelvormige)
- Problem, consequence and necessity: Incomplete grammatical fragment obscures the alternating knit/purl rounds needed for circular garter stitch.
- Smallest justified replacement: Brei elke rij recht (of brei in het rond afwisselend één ronde recht en één ronde averecht)
- Evidence: S-DROPS-GARTER;S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:79

## LA-nl-B18-046: `abbr_seed_st_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Afwisselend K1, P1 met offset elke rij
- Problem, consequence and necessity: Untranslated offset does not explain seed stitch placement and can be mistaken for continuing ribbing.
- Smallest justified replacement: Afwisselend 1 recht, 1 averecht; brei in elke volgende rij recht boven averecht en averecht boven recht
- Evidence: S-HANDWERKLES;S-KATIA; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:80

## LA-nl-B18-047: `abbr_sk_desc` (default)

- Classification: CONFIRMED_DEFECT / LANGUAGE / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Mis de volgende steek
- Problem, consequence and necessity: Mis de volgende steek describes failing to hit a stitch instead of the intentional skip command.
- Smallest justified replacement: Sla de volgende steek over
- Evidence: S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:91

## LA-nl-B18-048: `abbr_selvage_desc` (default)

- Classification: CONFIRMED_DEFECT / LANGUAGE / S3; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Randsteek werkte anders voor een nette rand
- Problem, consequence and necessity: Worked was rendered as the finite past-tense werkte, breaking the descriptive sentence.
- Smallest justified replacement: Randsteek die anders wordt gebreid voor een nette rand
- Evidence: CALLERS; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:92

## LA-nl-B18-049: `abbr_wpi_desc` (default)

- Classification: CONFIRMED_DEFECT / LANGUAGE / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Garendiktemeting: wikkelt zich rond een liniaal per inch
- Problem, consequence and necessity: Existing sentence says the measurement wraps itself; it fails to name the count being measured.
- Smallest justified replacement: Garendiktemeting: aantal wikkelingen om een liniaal per inch
- Evidence: S-CYC-ABBR; READ-B18; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:99

## LA-nl-B19-050: `sym_knit_name` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei
- Problem, consequence and necessity: Brei names knitting generally rather than the knit stitch contrasted with Averecht.
- Smallest justified replacement: Recht
- Evidence: S-CYC-SYMBOL;S-KATIA; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:17

## LA-nl-B19-051: `sym_knit_tbl_name` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei TBL
- Problem, consequence and necessity: Brei omits knitwise meaning and does not distinguish purl TBL.
- Smallest justified replacement: Recht TBL
- Evidence: S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:27

## LA-nl-B19-052: `sym_knit_tbl_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei door de achterste lus (gedraaid)
- Problem, consequence and necessity: Instruction omits knitwise operation.
- Smallest justified replacement: Brei recht door de achterste lus (gedraaid)
- Evidence: S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:28

## LA-nl-B19-053: `sym_slip_purlwise_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Haal een steek af alsof je averecht gaat breien
- Problem, consequence and necessity: The unbarred V glyph indicates yarn behind on the right-side view, distinct from yarn-in-front slip.
- Smallest justified replacement: Haal een steek af alsof je averecht gaat breien, met het garen achter het werk
- Evidence: S-CYC-SYMBOL;CALLERS; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:40

## LA-nl-B19-054: `sym_k2tog_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei er 2 samen – naar rechts leunend minderen
- Problem, consequence and necessity: Instruction omits knitwise operation, distinct from P2tog.
- Smallest justified replacement: Brei 2 steken recht samen – naar rechts leunend minderen
- Evidence: S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:44

## LA-nl-B19-055: `sym_k3tog_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei er 3 samen – dubbele mindering
- Problem, consequence and necessity: Instruction omits knitwise operation, distinct from P3tog.
- Smallest justified replacement: Brei 3 steken recht samen – dubbele mindering
- Evidence: S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:48

## LA-nl-B19-056: `sym_sk2p_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: 1 slip, 2 recht samen, haal de afgehaalde steek over
- Problem, consequence and necessity: Slip is untranslated and lacks knitwise orientation for SK2P.
- Smallest justified replacement: 1 steek recht afhalen, 2 recht samen, haal de afgehaalde steek over
- Evidence: S-CYC-ABBR;S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:49

## LA-nl-B19-057: `sym_m1l_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Maak 1 links – naar links leunende verhoging
- Problem, consequence and necessity: Verhoging means raising; the described operation is a stitch increase.
- Smallest justified replacement: Meerder 1 links – naar links leunende meerdering
- Evidence: S-BREICLUB-M1;S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:51

## LA-nl-B19-058: `sym_m1r_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Maak 1 rechts – naar rechts leunende verhoging
- Problem, consequence and necessity: Verhoging means raising; the described operation is a stitch increase.
- Smallest justified replacement: Meerder 1 rechts – naar rechts leunende meerdering
- Evidence: S-BREICLUB-M1;S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:52

## LA-nl-B19-059: `sym_kfb_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Brei de voor- en achterkant – meerder
- Problem, consequence and necessity: Front/back wording omits the same-stitch legs and knitwise operation.
- Smallest justified replacement: Brei recht in de voorste en achterste lus van dezelfde steek – meerder
- Evidence: S-CYC-ABBR;S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:53

## LA-nl-B19-060: `sym_lifted_inc_left_name` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Opgeheven Inc Links
- Problem, consequence and necessity: Opgeheven Inc is a literal mistranslation with an unexplained English abbreviation; identify the source stitch and direction.
- Smallest justified replacement: Meerdering uit onderliggende steek, links
- Evidence: S-HARDYBARN;S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:56

## LA-nl-B19-061: `sym_lifted_inc_left_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Links leunende opgeheven verhoging
- Problem, consequence and necessity: Opgeheven verhoging does not identify a knitting technique; distinguish lifted increase from between-stitch M1.
- Smallest justified replacement: Naar links leunende meerdering uit een onderliggende steek
- Evidence: S-HARDYBARN;S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:57

## LA-nl-B19-062: `sym_lifted_inc_right_name` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Opgeheven Inc Rechts
- Problem, consequence and necessity: Opgeheven Inc is a literal mistranslation with an unexplained English abbreviation; identify the source stitch and direction.
- Smallest justified replacement: Meerdering uit onderliggende steek, rechts
- Evidence: S-HARDYBARN;S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:62

## LA-nl-B19-063: `sym_lifted_inc_right_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Naar rechts leunende opgeheven verhoging
- Problem, consequence and necessity: Opgeheven verhoging does not identify a knitting technique; distinguish lifted increase from between-stitch M1.
- Smallest justified replacement: Naar rechts leunende meerdering uit een onderliggende steek
- Evidence: S-HARDYBARN;S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:63

## LA-nl-B19-064: `sym_no_stitch_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Grijze doos - tijdelijke aanduiding voor vormgeving
- Problem, consequence and necessity: Doos is a box; temporary placeholder implies a stitch will later be added. This chart cell contains no stitch.
- Smallest justified replacement: Grijs vakje – hier is geen steek
- Evidence: S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:95

## LA-nl-B19-065: `sym_repeat_desc` (default)

- Classification: CONFIRMED_DEFECT / TERM / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/reference_strings.xml`.
- Original: Patroonherhalingsbeugel
- Problem, consequence and necessity: Patroonherhalingsbeugel incorrectly describes a physical bracket rather than the chart repeat boundary.
- Smallest justified replacement: Markering van de patroonherhaling
- Evidence: CALLERS; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:99

## LA-nl-B19-066: `chart_symbols_disclaimer` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Symbolen volgen standaardconventies. Controleer altijd de legenda van je patroon.
- Problem, consequence and necessity: Directional/knit/purl instructions require the right-side viewing convention to interpret wrong-side rows correctly.
- Smallest justified replacement: Symbolen volgen standaardconventies en tonen de steken vanaf de goede kant van het werk. Controleer altijd de legenda van je patroon.
- Evidence: S-CYC-SYMBOL; READ-B19; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt:64

## LA-nl-B20-067: `tip_imperial_units_desc` (default)

- Classification: CONFIRMED_DEFECT / FACT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Als dit is ingeschakeld, gebruiken alle rekenhulpen inch in plaats van centimeters en yard in plaats van meters. De stekenproef wordt dan gemeten per 4 inch in plaats van per 10 cm.
- Problem, consequence and necessity: Global setting initializes defaults; the gauge tool supports explicit independent unit and gauge-basis choices. Existing all-calculators claim hides that override.
- Smallest justified replacement: Als dit is ingeschakeld, gebruiken de rekenhulpen standaard inch in plaats van centimeters en yard in plaats van meters. De stekenproef wordt dan gemeten per 4 inch in plaats van per 10 cm. Bij Maten en stekenproef kun je de eenheden zelf kiezen.
- Evidence: CALLERS; READ-B20; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/screens/settings/SettingsScreen.kt:147

## LA-nl-B22-068: `pro_prompt_reminders_title` (default)

- Classification: CONFIRMED_DEFECT / CONTEXT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Een rijherinnering toevoegen met Pro
- Problem, consequence and necessity: Reminder targets follow the chosen counter unit, including rounds and custom labels; Pro text must not promise a row-only feature.
- Smallest justified replacement: Een herinnering toevoegen met Pro
- Evidence: CALLERS; READ-B22; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/components/ProPromptSheet.kt:170

## LA-nl-B22-069: `pro_prompt_reminders_body` (default)

- Classification: CONFIRMED_DEFECT / CONTEXT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Een nieuwe rijherinnering vereist Pro. Je bestaande herinneringen blijven werken en bewerkbaar.
- Problem, consequence and necessity: Reminder targets follow the chosen counter unit, including rounds and custom labels; Pro text must not promise a row-only feature.
- Smallest justified replacement: Een nieuwe herinnering vereist Pro. Je bestaande herinneringen blijven werken en bewerkbaar.
- Evidence: CALLERS; READ-B22; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/components/ProPromptSheet.kt:219

## LA-nl-B22-070: `pro_group_workflow_body` (default)

- Classification: CONFIRMED_DEFECT / CONTEXT / S2; state VERIFIED.
- File: `app/src/main/res/values-nl/strings.xml`.
- Original: Gebruik de compacte herhalingsteller, extra, herhalings-, vorm- en sectietellers, rijherinneringen, patroonfoto\'s als PDF en de startschermwidget.
- Problem, consequence and necessity: Reminder targets follow the chosen counter unit, including rounds and custom labels; Pro text must not promise a row-only feature.
- Smallest justified replacement: Gebruik de compacte herhalingsteller, extra, herhalings-, vorm- en sectietellers, herinneringen, patroonfoto\'s als PDF en de startschermwidget.
- Evidence: CALLERS; READ-B22; final technical checks above.
- Contexts: app/src/main/java/com/finnvek/knittools/ui/screens/pro/ProUpgradeScreen.kt:125
