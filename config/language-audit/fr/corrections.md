# French correction register

Date: 2026-10-19. Changes are relative to the preserved entry snapshot, not HEAD. Each entry is a final-version content finding. Stable IDs are retained after seven unnecessary candidate rewrites were withdrawn; gaps are intentional. No native-speaker or new device validation is implied.

## LA-fr-B01-01: n_selected (default)

- Classification: CONFIRMED_DEFECT
- Category: LANGUAGE
- Severity: S3
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `n_selected` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/library/LibraryTopBar.kt:34; app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt:693
- Original text: %d sélectionnés
- User-visible consequence, evidence and necessity: One shared string labels projects, patterns, yarn cards and feminine photos at counts including 1. Fixed masculine plural is not valid in all these contexts; invariant noun avoids changing callers.
- Final correction: Sélection : %d
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B03-01: rounds_format (many)

- Classification: CONFIRMED_DEFECT
- Category: LANGUAGE
- Severity: S3
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `rounds_format` / `many`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/components/MainCounterDisplayText.kt:101
- Original text: %1$d de tours
- User-visible consequence, evidence and necessity: The argument is a full decimal integer, not the noun million. Written digits do not take de before the counted noun; many remains a separate Android category.
- Final correction: %1$d tours
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B03-02: repeats_format (many)

- Classification: CONFIRMED_DEFECT
- Category: LANGUAGE
- Severity: S3
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `repeats_format` / `many`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/components/MainCounterDisplayText.kt:102
- Original text: %1$d de répétitions
- User-visible consequence, evidence and necessity: The argument is a full decimal integer, not the noun million. Written digits do not take de before the counted noun; many remains a separate Android category.
- Final correction: %1$d répétitions
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B03-03: widget_name (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `widget_name` / `default`
- Affected context(s): app/src/main/AndroidManifest.xml:57
- Original text: Compteur de rangs
- User-visible consequence, evidence and necessity: Widget follows the main counter for ROWS/ROUNDS/REPEATS/CUSTOM; row-only naming misidentifies non-row counters.
- Final correction: Compteur de projet
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B03-04: widget_description (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `widget_description` / `default`
- Affected context(s): app/src/main/res/xml-v31/counter_widget_info.xml:14; app/src/main/res/xml/counter_widget_info.xml:10
- Original text: Widget de compteur de rangs pour le projet actif
- User-visible consequence, evidence and necessity: The widget also handles rounds, repeats and custom units.
- Final correction: Widget du compteur du projet actif
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B03-05: row_label_with_target (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `row_label_with_target` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt:478
- Original text: Rang %1$d / %2$d
- User-visible consequence, evidence and necessity: WidgetData has count/target, not a selected-unit label. A universal Rang mislabels crochet rounds and custom counts.
- Final correction: Compteur %1$d / %2$d
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B04-01: reminder_repeat_occurrence_format (one)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `reminder_repeat_occurrence_format` / `one`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt:248
- Original text: Répéter #%1$d, intervalle %2$d
- User-visible consequence, evidence and necessity: This is a numbered occurrence, not the command to repeat; quantity selection uses the interval argument, not the occurrence.
- Final correction: Répétition n° %1$d, intervalle %2$d
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B04-02: reminder_repeat_occurrence_format (many)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `reminder_repeat_occurrence_format` / `many`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt:248
- Original text: Répéter #%1$d, intervalle %2$d
- User-visible consequence, evidence and necessity: This is a numbered occurrence, not the command to repeat; quantity selection uses the interval argument, not the occurrence.
- Final correction: Répétition n° %1$d, intervalle %2$d
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B04-03: reminder_repeat_occurrence_format (other)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `reminder_repeat_occurrence_format` / `other`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt:248
- Original text: Répéter #%1$d, intervalle %2$d
- User-visible consequence, evidence and necessity: This is a numbered occurrence, not the command to repeat; quantity selection uses the interval argument, not the occurrence.
- Final correction: Répétition n° %1$d, intervalle %2$d
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B04-04: repeat_every (default)

- Classification: CONFIRMED_DEFECT
- Category: LANGUAGE
- Severity: S3
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `repeat_every` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt:625; app/src/main/java/com/finnvek/knittools/ui/screens/counter/ReminderComponents.kt:372
- Original text: Répéter toutes les
- User-visible consequence, evidence and necessity: The reminder field combines this prefix with a number and masculine incréments. Toutes has the wrong gender.
- Final correction: Répéter tous les
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B05-01: insights_empty_title (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `insights_empty_title` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsSections.kt:565
- Original text: Pas encore de temps sur les aiguilles.
- User-visible consequence, evidence and necessity: Insights includes crochet and mixed projects; needles-only wording excludes those crafts.
- Final correction: Pas encore de temps enregistré.
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B06-01: care_wash_30 (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `care_wash_30` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt; app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt:11; app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbolIcon.kt
- Original text: 30°
- User-visible consequence, evidence and necessity: The label is used alone in care selections and semantics; bare degrees omit both the operation and Celsius/maximum meaning.
- Final correction: Lavage à 30 °C maximum
- Evidence: S-CARE. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B06-02: care_wash_40 (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `care_wash_40` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt; app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt:12; app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbolIcon.kt
- Original text: 40°
- User-visible consequence, evidence and necessity: The label is used alone in care selections and semantics; bare degrees omit both the operation and Celsius/maximum meaning.
- Final correction: Lavage à 40 °C maximum
- Evidence: S-CARE. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B06-03: care_wash_60 (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `care_wash_60` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt; app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt:13; app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbolIcon.kt
- Original text: 60°
- User-visible consequence, evidence and necessity: The label is used alone in care selections and semantics; bare degrees omit both the operation and Celsius/maximum meaning.
- Final correction: Lavage à 60 °C maximum
- Evidence: S-CARE. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B06-04: care_dryclean_p (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `care_dryclean_p` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt; app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt:36; app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbolIcon.kt
- Original text: Solvant P
- User-visible consequence, evidence and necessity: P identifies an allowed cleaning process/solvent group, not a substance named Solvant P.
- Final correction: Nettoyage à sec au perchloroéthylène ou aux hydrocarbures (P)
- Evidence: S-CARE. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B06-05: care_dryclean_f (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `care_dryclean_f` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt; app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbol.kt:37; app/src/main/java/com/finnvek/knittools/ui/components/care/CareSymbolIcon.kt
- Original text: Solvant F
- User-visible consequence, evidence and necessity: F identifies hydrocarbon cleaning, not a substance named Solvant F.
- Final correction: Nettoyage à sec aux hydrocarbures (F)
- Evidence: S-CARE. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B07-01: yarn_usage_scope (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `yarn_usage_scope` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/counter/ProjectYarnUsageSheet.kt:102
- Original text: Suit uniquement le fil réservé à ce projet. Les quantités dans Mon fil restent inchangées.
- User-visible consequence, evidence and necessity: Planned, allocated and used are independent nullable quantities; usage is not limited to yarn with an allocated amount.
- Final correction: Suit uniquement l’utilisation du fil pour ce projet. Les quantités dans Mon fil restent inchangées.
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B10-03: pattern_reading_line_position_state (default)

- Classification: CONFIRMED_DEFECT
- Category: LANGUAGE
- Severity: S3
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/pattern_reading_assistance.xml` / `pattern_reading_line_position_state` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt:2176
- Original text: %1$d%% depuis le haut
- User-visible consequence, evidence and necessity: French percentage notation requires spacing before %. Keep %% escaped for Android formatting; use NBSP to retain spacing.
- Final correction: %1$d %% depuis le haut
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B10-04: pattern_vertical_guide_position_state (default)

- Classification: CONFIRMED_DEFECT
- Category: LANGUAGE
- Severity: S3
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/pattern_reading_assistance.xml` / `pattern_vertical_guide_position_state` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt:2291
- Original text: %1$d%% depuis la gauche
- User-visible consequence, evidence and necessity: French percentage notation requires spacing before %. Keep %% escaped for Android formatting; use NBSP to retain spacing.
- Final correction: %1$d %% depuis la gauche
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B10-05: pattern_reading_line_following_state (default)

- Classification: CONFIRMED_DEFECT
- Category: LANGUAGE
- Severity: S3
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/pattern_reading_assistance.xml` / `pattern_reading_line_following_state` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt:2174
- Original text: %1$d%% depuis le haut. Suivi du compteur principal.
- User-visible consequence, evidence and necessity: French percentage notation requires spacing before %. Keep %% escaped for Android formatting; use NBSP to retain spacing.
- Final correction: %1$d %% depuis le haut. Suivi du compteur principal.
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B10-06: pattern_reading_line_manual_state (default)

- Classification: CONFIRMED_DEFECT
- Category: LANGUAGE
- Severity: S3
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/pattern_reading_assistance.xml` / `pattern_reading_line_manual_state` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt:2175
- Original text: %1$d%% depuis le haut. Suivi du compteur en pause.
- User-visible consequence, evidence and necessity: French percentage notation requires spacing before %. Keep %% escaped for Android formatting; use NBSP to retain spacing.
- Final correction: %1$d %% depuis le haut. Suivi du compteur en pause.
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B14-01: desc_increase_decrease (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `desc_increase_decrease` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/home/HomeScreen.kt:71
- Original text: Répartissez les augmentations ou diminutions uniformément sur un rang
- User-visible consequence, evidence and necessity: The tool offers flat rows and circular rounds; its description must cover both.
- Final correction: Répartissez les augmentations ou diminutions uniformément sur un rang ou un tour
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B14-02: tip_flat_vs_circular_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `tip_flat_vs_circular_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/increase/IncreaseDecreaseScreen.kt:117
- Original text: Tricot à plat : vous travaillez en aller-retour sur des aiguilles droites ou circulaires en tournant à la fin de chaque rang.\n\nTricot en rond : vous travaillez en continu. Les augmentations/diminutions sont réparties uniformément tout autour du rang.
- User-visible consequence, evidence and necessity: Circular mode works a tour, not a flat rang. The production result is English K/M1/K2tog notation without another on-screen key; explain its meaning and total repeat multiplier.
- Final correction: Tricot à plat : vous travaillez en aller-retour sur des aiguilles droites ou circulaires en tournant à la fin de chaque rang.\n\nTricot en rond : vous travaillez en continu. Les augmentations/diminutions sont réparties uniformément tout autour du tour.\n\nNotation anglaise des résultats : K = tricoter à l’endroit, M1 = augmentation intercalaire, K2tog = tricoter 2 mailles ensemble à l’endroit ; × indique le nombre total de répétitions.
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B16-01: total_weight (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `total_weight` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/yarn/YarnEstimatorScreen.kt:201
- Original text: Poids total : %s g
- User-visible consequence, evidence and necessity: YarnEstimator computes ceil(required/perSkein) times package weight, not the exact used-yarn mass.
- Final correction: Poids des pelotes entières : %s g
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B16-02: estimated_skeins (default)

- Classification: CONFIRMED_DEFECT
- Category: LANGUAGE
- Severity: S3
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `estimated_skeins` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/yarn/YarnEstimatorScreen.kt:209
- Original text: Estimation : environ %s pelotes
- User-visible consequence, evidence and necessity: The formatted decimal can be 0,50 or 1,00, which take singular French; an invariant label preserves arbitrary decimals without shared-code changes.
- Final correction: Estimation en pelotes : environ %s
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B16-03: extra_skein_note (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `extra_skein_note` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/yarn/YarnEstimatorScreen.kt:217
- Original text: Achetez toujours une pelote supplémentaire ; les bains de teinture peuvent varier
- User-visible consequence, evidence and necessity: Always buying exactly one extra package is an unsupported universal recommendation; necessary margin depends on project and package.
- Final correction: Prévoyez une marge selon votre projet ; les bains de teinture peuvent varier.
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B17-01: tip_needle_sizes_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `tip_needle_sizes_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/needles/NeedleSizeScreen.kt:78
- Original text: Le système métrique (mm) est la norme universelle. Les tailles US utilisent un système numéroté (0–50). Les tailles UK/Canada utilisent un autre système numéroté. Les tailles japonaises ont leur propre numérotation. Vérifiez toujours la taille en mm pour être sûr.
- User-visible consequence, evidence and necessity: Metric diameter is the comparison reference, not a universal sizing system; current US entries include 000 and 00 below 0.
- Final correction: Le diamètre en millimètres est la référence la plus précise. Les tailles US utilisent un système numéroté (de 000 à 50 dans ce tableau). Les tailles UK/Canada utilisent un autre système numéroté. Les tailles japonaises ont leur propre numérotation. Vérifiez toujours la taille en mm pour être sûr.
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B17-02: size_row_age_0_3m (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `size_row_age_0_3m` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:122
- Original text: 0 à 3 m
- User-visible consequence, evidence and necessity: The current CYC-derived row is for this stated age, not a range of younger ages; m also risks confusion with meters.
- Final correction: 3 mois
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B17-03: size_row_age_3_6m (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `size_row_age_3_6m` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:123
- Original text: 3 à 6 m
- User-visible consequence, evidence and necessity: The current CYC-derived row is for this stated age, not a range of younger ages; m also risks confusion with meters.
- Final correction: 6 mois
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B17-04: size_row_age_6_12m (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `size_row_age_6_12m` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:124
- Original text: 6 à 12 m
- User-visible consequence, evidence and necessity: The current CYC-derived row is for this stated age, not a range of younger ages; m also risks confusion with meters.
- Final correction: 12 mois
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B17-05: size_row_age_12_18m (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `size_row_age_12_18m` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:125
- Original text: 12 à 18 mois
- User-visible consequence, evidence and necessity: The current CYC-derived row is for this stated age, not a range of younger ages; m also risks confusion with meters.
- Final correction: 18 mois
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B17-06: size_col_chest (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `size_col_chest` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:38; app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:47; app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:66
- Original text: Poitrine
- User-visible consequence, evidence and necessity: Current body table measures circumference for chest/waist/hips, and underarm-to-wrist length for arm. Taille otherwise duplicates the size header; precise measurement type prevents using flat width or full arm length.
- Final correction: Tour de poitrine
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B17-07: size_col_waist (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `size_col_waist` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:39; app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:48; app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:58; app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:67
- Original text: Taille
- User-visible consequence, evidence and necessity: Current body table measures circumference for chest/waist/hips, and underarm-to-wrist length for arm. Taille otherwise duplicates the size header; precise measurement type prevents using flat width or full arm length.
- Final correction: Tour de taille
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B17-08: size_col_hips (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `size_col_hips` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:40; app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:49; app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:59; app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:68
- Original text: Hanches
- User-visible consequence, evidence and necessity: Current body table measures circumference for chest/waist/hips, and underarm-to-wrist length for arm. Taille otherwise duplicates the size header; precise measurement type prevents using flat width or full arm length.
- Final correction: Tour de hanches
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B17-09: size_col_bust (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `size_col_bust` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:57
- Original text: Buste
- User-visible consequence, evidence and necessity: Current body table measures circumference for chest/waist/hips, and underarm-to-wrist length for arm. Taille otherwise duplicates the size header; precise measurement type prevents using flat width or full arm length.
- Final correction: Tour de poitrine
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B17-10: size_col_arm (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `size_col_arm` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/SizeChartData.kt:50
- Original text: Bras
- User-visible consequence, evidence and necessity: Current body table measures circumference for chest/waist/hips, and underarm-to-wrist length for arm. Taille otherwise duplicates the size header; precise measurement type prevents using flat width or full arm length.
- Final correction: Longueur sous le bras
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-01: abbreviations_title (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `abbreviations_title` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/abbreviations/AbbreviationsScreen.kt:43; app/src/main/java/com/finnvek/knittools/ui/screens/library/LibraryScreen.kt:126
- Original text: Abréviations
- User-visible consequence, evidence and necessity: All 77 displayed tokens are English CYC notation, not French abbreviations. Make token language explicit without changing the tokens.
- Final correction: Abréviations anglaises
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-02: abbr_p_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_p_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:17
- Original text: Insérer l\'aiguille à l\'envers, enrouler le fil, passer à travers
- User-visible consequence, evidence and necessity: À l’envers can describe reversing the needle rather than purlwise insertion. Comme pour tricoter specifies the stitch orientation.
- Final correction: Insérer l’aiguille comme pour tricoter à l’envers, enrouler le fil, passer à travers
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-03: abbr_yo_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_yo_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:21
- Original text: Enroulez le fil autour de l\'aiguille pour créer un nouveau point et un trou décoratif.
- User-visible consequence, evidence and necessity: The described object is an individual maille (or its brin), not a stitch pattern/point, anatomical jambe, or a generic twisted/cabled fabric; retain the operation and correct its technical French.
- Final correction: Enroulez le fil autour de l’aiguille pour créer une nouvelle maille et un trou décoratif.
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-04: abbr_dec_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_dec_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:36
- Original text: Réduire le nombre de points
- User-visible consequence, evidence and necessity: The described object is an individual maille (or its brin), not a stitch pattern/point, anatomical jambe, or a generic twisted/cabled fabric; retain the operation and correct its technical French.
- Final correction: Réduire le nombre de mailles
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-05: abbr_inc_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_inc_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:43
- Original text: Ajouter des points au travail
- User-visible consequence, evidence and necessity: The described object is an individual maille (or its brin), not a stitch pattern/point, anatomical jambe, or a generic twisted/cabled fabric; retain the operation and correct its technical French.
- Final correction: Ajouter des mailles à l’ouvrage
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-06: abbr_sl_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_sl_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:45
- Original text: Déplacer le point de l\'aiguille de gauche à droite sans le tricoter
- User-visible consequence, evidence and necessity: The described object is an individual maille (or its brin), not a stitch pattern/point, anatomical jambe, or a generic twisted/cabled fabric; retain the operation and correct its technical French.
- Final correction: Déplacer la maille de l’aiguille de gauche à droite sans la tricoter
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-07: abbr_co_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_co_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:49
- Original text: Créer les premiers points sur l\'aiguille
- User-visible consequence, evidence and necessity: The described object is an individual maille (or its brin), not a stitch pattern/point, anatomical jambe, or a generic twisted/cabled fabric; retain the operation and correct its technical French.
- Final correction: Créer les premières mailles sur l’aiguille
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-08: abbr_pm_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_pm_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:53
- Original text: Placer un marqueur de point sur l\'aiguille
- User-visible consequence, evidence and necessity: The described object is an individual maille (or its brin), not a stitch pattern/point, anatomical jambe, or a generic twisted/cabled fabric; retain the operation and correct its technical French.
- Final correction: Placer un marqueur de mailles sur l’aiguille
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-10: abbr_tbl_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_tbl_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:63
- Original text: Travaillez dans la jambe arrière du point
- User-visible consequence, evidence and necessity: Jambe arrière du point is an anatomical English calque; the operation works the brin arrière of a maille.
- Final correction: Travailler dans le brin arrière de la maille
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-11: abbr_ktbl_meaning (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_ktbl_meaning` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:64
- Original text: Tricoter dans la boucle arrière
- User-visible consequence, evidence and necessity: Ktbl specifically means knit through back loop; generic tricoter omits the knit/purl distinction.
- Final correction: Tricoter à l’endroit dans le brin arrière
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-12: abbr_ktbl_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_ktbl_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:64
- Original text: Point tricoté torsadé
- User-visible consequence, evidence and necessity: Point tricoté torsadé confuses an individual twisted knit stitch with a cabled stitch pattern. Maille endroit torse names the operation.
- Final correction: Maille endroit torse
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-14: abbr_ptbl_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_ptbl_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:65
- Original text: Point envers torsadé
- User-visible consequence, evidence and necessity: Point envers torsadé confuses an individual twisted purl stitch with a cable. Maille envers torse is the technical term.
- Final correction: Maille envers torse
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-15: abbr_rem_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_rem_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:83
- Original text: Points laissés sur l\'aiguille
- User-visible consequence, evidence and necessity: The described object is an individual maille (or its brin), not a stitch pattern/point, anatomical jambe, or a generic twisted/cabled fabric; retain the operation and correct its technical French.
- Final correction: Mailles restantes sur l’aiguille
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-16: abbr_alt_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_alt_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:85
- Original text: Un rang ou un point sur deux
- User-visible consequence, evidence and necessity: The described object is an individual maille (or its brin), not a stitch pattern/point, anatomical jambe, or a generic twisted/cabled fabric; retain the operation and correct its technical French.
- Final correction: Un rang ou une maille sur deux
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-17: abbr_sk_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_sk_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:91
- Original text: Manque le point suivant
- User-visible consequence, evidence and necessity: Manque le point suivant is a statement of absence, not the instruction to skip the next stitch.
- Final correction: Sauter la maille suivante
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-18: abbr_selvage_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_selvage_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:92
- Original text: Point de bordure travaillé différemment pour une bordure soignée
- User-visible consequence, evidence and necessity: The described object is an individual maille (or its brin), not a stitch pattern/point, anatomical jambe, or a generic twisted/cabled fabric; retain the operation and correct its technical French.
- Final correction: Maille lisière travaillée différemment pour une bordure soignée
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-19: abbr_gauge_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_gauge_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:100
- Original text: Nombre de points et de rangs par unité de mesure
- User-visible consequence, evidence and necessity: The described object is an individual maille (or its brin), not a stitch pattern/point, anatomical jambe, or a generic twisted/cabled fabric; retain the operation and correct its technical French.
- Final correction: Nombre de mailles et de rangs par unité de mesure
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-20: abbr_k2tog_meaning (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_k2tog_meaning` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:25
- Original text: Tricoter 2 ensemble
- User-visible consequence, evidence and necessity: K is specifically the knit operation; generic tricoter does not distinguish it from P2tog.
- Final correction: Tricoter 2 mailles ensemble à l’endroit
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-21: abbr_k2tog_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_k2tog_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:25
- Original text: Diminution vers la droite : tricoter deux mailles en une seule
- User-visible consequence, evidence and necessity: Specify the knit operation required by K2tog.
- Final correction: Diminution vers la droite : tricoter deux mailles ensemble à l’endroit
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-22: abbr_k3tog_meaning (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_k3tog_meaning` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:30
- Original text: Tricoter 3 ensemble
- User-visible consequence, evidence and necessity: Specify the knit operation required by K3tog, distinct from P3tog.
- Final correction: Tricoter 3 mailles ensemble à l’endroit
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-23: abbr_ssk_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S1
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_ssk_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:27
- Original text: Diminution vers la gauche : glisser 2 mailles à l\'endroit, les tricoter ensemble dans le brin arrière
- User-visible consequence, evidence and necessity: Slipping two together produces a different stitch orientation; SSK slips them individually.
- Final correction: Diminution vers la gauche : glisser 2 mailles une à une à l’endroit, les tricoter ensemble à l’endroit dans le brin arrière
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-24: abbr_ssp_meaning (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_ssp_meaning` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:28
- Original text: Glisser, glisser, envers
- User-visible consequence, evidence and necessity: Envers alone omits the final verb in the three-operation sequence.
- Final correction: Glisser, glisser, tricoter à l’envers
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-25: abbr_ssp_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S1
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_ssp_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:28
- Original text: Diminution des mailles envers vers la gauche
- User-visible consequence, evidence and necessity: SSP requires individually reoriented stitches and purling through back loops; viewing side determines the stated lean.
- Final correction: Glisser 2 mailles une à une à l’endroit, les remettre sur l’aiguille gauche, puis les tricoter ensemble à l’envers par les brins arrière ; diminution inclinée à gauche vue sur l’endroit
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-26: abbr_s2kp_meaning (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S1
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_s2kp_meaning` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:32
- Original text: Glisser 2, tricoter 1 maille endroit, passer les mailles glissées par-dessus
- User-visible consequence, evidence and necessity: Centered S2KP requires slipping the two together knitwise; the original omitted both together and orientation.
- Final correction: Glisser 2 mailles ensemble à l’endroit, tricoter 1 maille endroit, passer les mailles glissées par-dessus
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-27: abbr_m1_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S1
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_m1_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:38
- Original text: Relever la barre entre les mailles et tricoter dans la boucle arrière pour augmenter
- User-visible consequence, evidence and necessity: The stated back-loop method closes the hole only with the matching front-to-back pickup.
- Final correction: Relever le fil entre les mailles de l’avant vers l’arrière, puis le tricoter à l’endroit par le brin arrière pour augmenter
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-28: abbr_pfb_meaning (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_pfb_meaning` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:42
- Original text: Tricoter à l\'envers devant et derrière
- User-visible consequence, evidence and necessity: PFB works two legs of one stitch, not front and back surfaces of the fabric.
- Final correction: Tricoter à l’envers dans le brin avant puis le brin arrière
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-29: abbr_pfb_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_pfb_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:42
- Original text: Tricoter à l\'envers sur le devant et l\'arrière du même point pour augmenter
- User-visible consequence, evidence and necessity: Correct point and clarify two legs of the same stitch, preserving the purl operation.
- Final correction: Tricoter la même maille à l’envers dans le brin avant puis le brin arrière pour augmenter
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-30: abbr_sl1k_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_sl1k_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:46
- Original text: Glisser une maille comme pour tricoter
- User-visible consequence, evidence and necessity: Knitwise slip is orientation-specific; generic tricoter could include purlwise.
- Final correction: Glisser une maille comme pour la tricoter à l’endroit
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-31: abbr_c4f_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_c4f_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:67
- Original text: Glisser 2 vers CN et tenir devant, K2, K2 depuis CN
- User-visible consequence, evidence and necessity: Original mixed Glisser N vers CN / retenir en arrière is unclear instruction French; explicitly identify stitches, holder, hold side and knit order.
- Final correction: Glisser 2 mailles sur l’aiguille à torsades et les maintenir devant, tricoter 2 mailles à l’endroit, puis les 2 mailles de l’aiguille à torsades à l’endroit
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-32: abbr_c4b_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_c4b_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:68
- Original text: Glissez 2 vers CN et retenez en arrière, K2, K2 depuis CN
- User-visible consequence, evidence and necessity: Original mixed Glisser N vers CN / retenir en arrière is unclear instruction French; explicitly identify stitches, holder, hold side and knit order.
- Final correction: Glisser 2 mailles sur l’aiguille à torsades et les maintenir derrière, tricoter 2 mailles à l’endroit, puis les 2 mailles de l’aiguille à torsades à l’endroit
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-33: abbr_c6f_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_c6f_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:69
- Original text: Glisser 3 vers CN et tenir devant, K3, K3 depuis CN
- User-visible consequence, evidence and necessity: Original mixed Glisser N vers CN / retenir en arrière is unclear instruction French; explicitly identify stitches, holder, hold side and knit order.
- Final correction: Glisser 3 mailles sur l’aiguille à torsades et les maintenir devant, tricoter 3 mailles à l’endroit, puis les 3 mailles de l’aiguille à torsades à l’endroit
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-34: abbr_c6b_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_c6b_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:70
- Original text: Glissez 3 vers CN et retenez en arrière, K3, K3 depuis CN
- User-visible consequence, evidence and necessity: Original mixed Glisser N vers CN / retenir en arrière is unclear instruction French; explicitly identify stitches, holder, hold side and knit order.
- Final correction: Glisser 3 mailles sur l’aiguille à torsades et les maintenir derrière, tricoter 3 mailles à l’endroit, puis les 3 mailles de l’aiguille à torsades à l’endroit
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-35: abbr_wyif_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S1
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_wyif_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:72
- Original text: Tenir le fil vers l\'avant (côté envers) de l\'ouvrage
- User-visible consequence, evidence and necessity: Front means relative to the knitter; it is not necessarily the wrong side.
- Final correction: Tenir le fil devant l’ouvrage, du côté qui vous fait face
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-36: abbr_wyib_meaning (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_wyib_meaning` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:73
- Original text: Avec du fil dans le dos
- User-visible consequence, evidence and necessity: Dans le dos is an anatomical calque and can suggest putting yarn behind the knitter.
- Final correction: Avec le fil derrière
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-37: abbr_wyib_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S1
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_wyib_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:73
- Original text: Tenir le fil vers l\'arrière (côté tricot) de l\'ouvrage
- User-visible consequence, evidence and necessity: Back means away from the knitter; it is not necessarily the right/knit side.
- Final correction: Tenir le fil derrière l’ouvrage, du côté opposé à vous
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-38: abbr_wt_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S1
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_wt_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:75
- Original text: Enroulez le point suivant et tournez l\'ouvrage pour former des rangs raccourcis.
- User-visible consequence, evidence and necessity: The yarn wraps the stitch; Enroulez le point does not specify the performed operation.
- Final correction: Enroulez le fil autour de la maille suivante et tournez l’ouvrage pour former des rangs raccourcis.
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-39: abbr_st_st_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S1
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_st_st_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:77
- Original text: Tricoter sur l\'endroit, tricoter à l\'envers sur l\'envers (ou tricoter tous les tours en circulaire)
- User-visible consequence, evidence and necessity: Stockinette needs knit operations on RS/all rounds, not the generic tricoter.
- Final correction: Tricoter à l’endroit sur l’endroit, à l’envers sur l’envers (ou à l’endroit à tous les tours en circulaire)
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-40: abbr_seed_st_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S1
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_seed_st_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:80
- Original text: Alternance K1, P1 avec décalage à chaque rangée
- User-visible consequence, evidence and necessity: Unspecified offset each row is not reliable for odd versus even stitch counts; establish opposing knit/purl placement.
- Final correction: Alterner 1 maille endroit, 1 maille envers ; aux rangs suivants, tricoter les mailles comme elles ne se présentent pas
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B18-41: abbr_wpi_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `abbr_wpi_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/AbbreviationData.kt:99
- Original text: Mesure de l\'épaisseur du fil : s\'enroule autour d\'une règle par pouce
- User-visible consequence, evidence and necessity: The original says s’enroule, not what is counted; WPI is number of adjacent wraps per inch.
- Final correction: Mesure de l’épaisseur du fil : nombre de tours de fil côte à côte sur un pouce de règle
- Evidence: S-CYC-ABBR and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-01: chart_symbols_disclaimer (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `chart_symbols_disclaimer` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt:64
- Original text: Les symboles suivent les conventions standard. Vérifiez toujours la légende de votre modèle.
- User-visible consequence, evidence and necessity: Current drawings explicitly use the CYC right-side convention; generic standard claim omits viewing side needed for decrease direction.
- Final correction: Les symboles suivent les conventions du Craft Yarn Council, vus sur l’endroit de l’ouvrage. Vérifiez toujours la légende de votre modèle.
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-02: sym_knit_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_knit_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:17; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Tricoter sur l\'endroit, tricoter à l\'envers sur l\'envers
- User-visible consequence, evidence and necessity: Distinguish knit stitch from generic knitting on the right side.
- Final correction: Tricoter à l’endroit sur l’endroit, à l’envers sur l’envers
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-04: sym_knit_tbl_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_knit_tbl_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:28; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Tricoter dans la boucle arrière (torsadée)
- User-visible consequence, evidence and necessity: Generic tricoter and torsadée can obscure knit versus purl and twisted stitch versus cable.
- Final correction: Tricoter à l’endroit dans le brin arrière
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-06: sym_purl_tbl_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_purl_tbl_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:34; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Tricoter à l\'envers dans la boucle arrière (torsadée)
- User-visible consequence, evidence and necessity: Use brin for the leg worked and avoid confusing a twisted stitch with a cable.
- Final correction: Tricoter à l’envers dans le brin arrière
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-07: sym_slip_purlwise_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S1
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_slip_purlwise_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:40; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Glisser une maille comme pour la tricoter à l’envers
- User-visible consequence, evidence and necessity: The current unbarred V is the CYC purlwise slip with yarn at back. Yarn placement matters.
- Final correction: Glisser une maille comme pour la tricoter à l’envers, avec le fil derrière l’ouvrage
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-08: sym_k2tog_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_k2tog_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:44; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Tricoter 2 ensemble – diminution vers la droite
- User-visible consequence, evidence and necessity: Explicitly identify knit operation for the right-leaning decrease.
- Final correction: Tricoter 2 mailles ensemble à l’endroit – diminution vers la droite
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-09: sym_ssk_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: FACT
- Severity: S1
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_ssk_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:45; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Glisser, glisser, tricoter – diminution vers la gauche
- User-visible consequence, evidence and necessity: Slip orientation and individual slipping are required by SSK.
- Final correction: Glisser 2 mailles une à une à l’endroit, les tricoter ensemble à l’endroit par les brins arrière – diminution vers la gauche
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-10: sym_k3tog_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_k3tog_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:48; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Tricoter 3 mailles ensemble – double diminution
- User-visible consequence, evidence and necessity: Explicitly identify knit operation for K3tog.
- Final correction: Tricoter 3 mailles ensemble à l’endroit – double diminution
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-11: sym_m1l_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_m1l_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:51; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Faites 1 à gauche – augmentation vers la gauche
- User-visible consequence, evidence and necessity: Faire 1 is a literal English calque, not the French name of the increase.
- Final correction: Augmentation intercalaire inclinée à gauche
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-12: sym_lifted_inc_left_name (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_lifted_inc_left_name` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:56; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Levé Inc à gauche
- User-visible consequence, evidence and necessity: Levé Inc is broken agreement and untranslated Inc; state the established French technique name.
- Final correction: Augmentation levée à gauche
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-13: sym_m1r_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_m1r_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:52; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Faire 1 à droite – augmentation vers la droite
- User-visible consequence, evidence and necessity: Faire 1 is a literal English calque, not the French name of the increase.
- Final correction: Augmentation intercalaire inclinée à droite
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-14: sym_lifted_inc_right_name (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_lifted_inc_right_name` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:62; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Levé Inc à droite
- User-visible consequence, evidence and necessity: Levé Inc is broken agreement and untranslated Inc; state the established French technique name.
- Final correction: Augmentation levée à droite
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-15: sym_no_stitch_name (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_no_stitch_name` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:94; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Pas de point
- User-visible consequence, evidence and necessity: An absent grid stitch is a maille, not a stitch pattern/point.
- Final correction: Pas de maille
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-16: sym_no_stitch_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_no_stitch_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:95; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Boîte grise — espace réservé pour la mise en forme
- User-visible consequence, evidence and necessity: The gray cell is a no-stitch placeholder; it does not instruct a shaping operation.
- Final correction: Case grise sans maille à travailler
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B19-17: sym_marker_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: TERM
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/reference_strings.xml` / `sym_marker_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt; app/src/main/java/com/finnvek/knittools/domain/calculator/ChartSymbolData.kt:98; app/src/main/java/com/finnvek/knittools/ui/screens/chartsymbols/ChartSymbolScreen.kt
- Original text: Position du marqueur de point
- User-visible consequence, evidence and necessity: Stitch marker marks mailles, not a point pattern.
- Final correction: Position du marqueur de mailles
- Evidence: S-CYC-CHART and S-DROPS. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B20-01: tip_imperial_units_desc (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `tip_imperial_units_desc` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/settings/SettingsScreen.kt:147
- Original text: Lorsque cette option est activée, tous les calculateurs utilisent les pouces au lieu des centimètres et les yards au lieu des mètres. L’échantillon est alors mesuré sur 4 pouces au lieu de 10 cm.
- User-visible consequence, evidence and necessity: Gauge initializes from the preference but exposes independent unit and gauge-basis controls; all always using imperial is false.
- Final correction: Lorsque cette option est activée, les calculateurs utilisent par défaut les pouces au lieu des centimètres et les yards au lieu des mètres. L’échantillon est alors indiqué sur 4 pouces au lieu de 10 cm. Dans Mesures et échantillon, vous pouvez changer les unités et la base de l’échantillon.
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B22-01: pro_prompt_reminders_title (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `pro_prompt_reminders_title` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/components/ProPromptSheet.kt:170
- Original text: Ajoutez un rappel de rang avec Pro
- User-visible consequence, evidence and necessity: Reminders apply to the selectable main count, including rounds and custom labels, not only rows.
- Final correction: Ajoutez un rappel de compteur avec Pro
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B22-02: pro_prompt_reminders_body (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `pro_prompt_reminders_body` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/components/ProPromptSheet.kt:219
- Original text: Créer un nouveau rappel de rang nécessite Pro. Vos rappels existants continuent de fonctionner et restent modifiables.
- User-visible consequence, evidence and necessity: Neutral counter reminders match the production model for every unit.
- Final correction: Créer un nouveau rappel de compteur nécessite Pro. Vos rappels existants continuent de fonctionner et restent modifiables.
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## LA-fr-B22-03: pro_group_workflow_body (default)

- Classification: CONFIRMED_DEFECT
- Category: CONTEXT
- Severity: S2
- State: VERIFIED
- Resource: `app/src/main/res/values-fr/strings.xml` / `pro_group_workflow_body` / `default`
- Affected context(s): app/src/main/java/com/finnvek/knittools/ui/screens/pro/ProUpgradeScreen.kt:125
- Original text: Utilisez le compteur de répétitions compact, les compteurs supplémentaires, de répétition, de mise en forme et de section, les rappels de rang, les photos de modèle en PDF et le widget d’écran d’accueil.
- User-visible consequence, evidence and necessity: Pro description must also include reminders for rounds, repeats and custom count labels.
- Final correction: Utilisez le compteur de répétitions compact, les compteurs supplémentaires, de répétition, de mise en forme et de section, les rappels de compteur, les photos de modèle en PDF et le widget d’écran d’accueil.
- Evidence: SOURCE; SHARED; the batch decision in review.md. The final text was re-read against the referenced production meaning; retained wording outside the defect was not standardized.
- Shared-code or other-language impact: none; only this French resource variant changes.
- Checks actually performed: final source/context re-read; XML, alias and placeholder reconciliation; final `:app:processDebugResources`; the two resource test classes (three tests) listed in review.md. Build identity: input-hashes.json. These checks validate resources and source meaning, not a new rendered/device case for this individual string.
- Remaining uncertainty or dependency: no specific unresolved content question; supplementary rendering and native-speaker review were not performed.

## Retained alternatives

The seven candidates listed in retained-alternatives.json were restored to their entry wording: valid calibration action wording, understandable back-loop terminology and TBL notation, and the contextual active-session time description. They are NOT_A_DEFECT / NOT_APPLIED, not remaining findings. Other valid French alternatives and infinitive button labels were preserved.
