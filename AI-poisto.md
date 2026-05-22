# AI-poisto

Tämä dokumentti kuvaa, mitä KnitToolsista pitää poistaa, jos tavoitteena on poistaa sovelluksesta kaikki AI-/Gemini-/GenAI-pohjainen toiminnallisuus. Kartoitus on tehty nykyisestä checkoutista 2026-05-22.

Nykyinen työpuu oli jo likainen ennen tämän dokumentin lisäämistä: `AGENTS.md`, `PROJECT.md` ja useat AI-lähdetiedostot olivat muokattuina. Tämä dokumentti ei palauta eikä muuta niitä.

## Rajaus

Poistettavaksi kuuluu kaikki sovelluksen oma mallipohjainen AI:

- Firebase AI Logic / Gemini -pilvikutsut.
- Gemini Live API -äänikeskustelu.
- ML Kit GenAI Prompt API / Gemini Nano -parseri.
- AI-quota- ja Live-quota-DataStoret.
- AI:hin sidotut Pro-gatet, UI:t, testit, dokumentit, CI/Firebase-konfiguraatio ja dependency verification -metadata.

Kaikki "voice" ei ole automaattisesti AI:tä. `VoiceCommandHandler`, `VoiceCommandParser` ja `VoiceResponseManager` ovat paikallinen SpeechRecognizer/TTS/keyword-putki. Ne voi pitää, jos halutaan säilyttää perusäänikomennot ilman Geminiä. Poistettavaa AI:tä ovat klassisen voice-flow'n Gemini-fallback ja koko Live API -flow.

Kaikki "camera scan" ei ole automaattisesti AI:tä. Progress photos ja pattern camera scan ovat kameran/tiedostonhallinnan ominaisuuksia. Yarn label scan ja pattern instruction -tulkinta ovat nykykoodissa AI:tä, koska ne lähettävät kuvan Geminille.

## Ulkoinen tarkistus

Tarkistin nykyiset viralliset rajapintadokumentit ennen poistosuunnitelmaa:

- Firebase AI Logicin Android-ohjeistus kuvaa Firebase AI SDK:n, Firebase BoM:n ja Firebase App Check -kytkennän. Tämä tukee havaintoa, että `firebase-ai`, `firebase-appcheck-playintegrity`, `google-services` ja `google-services.json` kuuluvat nykyiseen AI-pintaan. Lähde: https://firebase.google.com/docs/ai-logic
- ML Kit GenAI Prompt API käyttää `com.google.mlkit:genai-prompt`-riippuvuutta ja `Generation.getClient()` / `GenerativeModel` -pintaa. Tämä tukee havaintoa, että `ai/nano` on poistettava, jos Gemini Nano poistetaan. Lähde: https://developers.google.com/ml-kit/genai

## Poistettavat AI-lähdetiedostot

Poista koko `app/src/main/java/com/finnvek/knittools/ai/` sen jälkeen, kun kutsujat on irrotettu:

- `AiJsonExtractor.kt`
- `AiQuotaManager.kt`
- `AiVoiceAction.kt`
- `GeminiAiService.kt`
- `ParsedYarnLabel.kt`
- `PatternInstructionCombinerGemini.kt`
- `PatternInstructionGemini.kt`
- `ProjectSummarizer.kt`
- `VoiceCommandInterpreter.kt`
- `YarnLabelGeminiScanner.kt`
- `journal/JournalEntryProcessor.kt`
- `live/FirebaseVoiceLiveConnector.kt`
- `live/LiveVoiceFunctionCallMapper.kt`
- `live/ProjectVoiceContext.kt`
- `live/VoiceFunctionDeclarations.kt`
- `live/VoiceLiveQuotaManager.kt`
- `live/VoiceLiveSession.kt`
- `nano/InstructionParser.kt`
- `nano/NanoAvailability.kt`
- `speech/SimpleSpeechRecognizer.kt`, jos AI-journalin Speak/Type-bottom sheet poistetaan kokonaan. Jos halutaan jättää ei-AI-raakapuhemerkintä, tämä pitää siirtää pois `ai/`-paketista ja nimetä neutraalisti.

`InstructionParser.kt` sisältää myös regex-fallbackin. Jos "paste instruction" halutaan säilyttää ilman AI:tä, irrota regex-parseri uuteen ei-AI-paikkaan, esimerkiksi `domain/calculator/InstructionTextParser.kt`, ja poista vain ML Kit GenAI -osa. Jos ominaisuus poistetaan kokonaan, poista myös `PasteInstructionButton`.

## Repository- ja utility-kerros

Poista tai muokkaa nämä AI-riippuvuudet:

- `app/src/main/java/com/finnvek/knittools/repository/PatternInstructionRepository.kt`: poista kokonaan, jos pattern viewerista poistetaan riviohjeen haku, instruction explainer ja combine instructions.
- `app/src/main/java/com/finnvek/knittools/repository/YarnLabelScanRepository.kt`: poista kokonaan, jos yarn label -kuvaskannaus poistetaan. Se on pelkkä Gemini-skannauksen orkestroija.
- `app/src/main/java/com/finnvek/knittools/util/NetworkStatusProvider.kt`: nykyhaun perusteella käytössä vain AI-poluille. Poista, jos mikään ei käytä sitä AI-poiston jälkeen.
- `app/src/main/java/com/finnvek/knittools/data/storage/YarnLabelPhotoStorage.kt`: poista vain, jos poistetaan myös yarn label -kamerakuvaus eikä tallennettuja yarn label -kuvia haluta enää tukea. `YarnCard.photoUri` ei ole itsessään AI, joten Room-kenttää ei kannata poistaa ilman erillistä migraatiota.

## App, Firebase ja App Check

Nykyisessä koodissa Firebase näkyy vain AI/App Check -kytkennässä.

Poista `app/src/main/java/com/finnvek/knittools/App.kt`:stä:

- `FirebaseApp.initializeApp(this)`
- `FirebaseAppCheck.getInstance().installAppCheckProviderFactory(...)`
- importit `com.google.firebase.FirebaseApp`, `FirebaseAppCheck` ja `PlayIntegrityAppCheckProviderFactory`
- koko `initializeFirebaseAppCheck()`-metodi

Jos AI-poiston jälkeen mikään muu Firebase-tuote ei jää käyttöön, poista myös:

- `app/build.gradle.kts`: `alias(libs.plugins.google.services)`
- root `build.gradle.kts`: `alias(libs.plugins.google.services) apply false`
- `gradle/libs.versions.toml`: `googleServices`, `firebaseBom`, `firebase-bom`, `firebase-ai`, `firebase-appcheck-playintegrity`, `google-services`
- `app/build.gradle.kts`: `implementation(platform(libs.firebase.bom))`, `implementation(libs.firebase.ai)`, `implementation(libs.firebase.appcheck.playintegrity)`
- `.github/scripts/create-ci-google-services-json.sh`
- `.github/workflows/build.yml` ja `.github/workflows/codeql.yml`: "Create CI Firebase config" -vaiheet
- ignored paikallinen `app/google-services.json`
- `.gitignore`-rivi `app/google-services.json`, jos Firebase-konfigia ei enää tarvita lainkaan

Älä commitoi olemassa olevaa `app/google-services.json`-tiedostoa. Se on tällä hetkellä `.gitignore`ssa.

## Gradle ja dependency verification

Poista AI-riippuvuudet:

- `app/build.gradle.kts`: `implementation(libs.mlkit.genai.prompt)`
- `gradle/libs.versions.toml`: `mlkitGenaiPrompt` ja `mlkit-genai-prompt`
- `app/build.gradle.kts`: `implementation(libs.mlkit.text.recognition)`, jos poistetaan kaikki ML/OCR/AI eikä uutta ei-AI OCR-korvaajaa rakenneta. Nykyisestä tuotantokoodista ei löytynyt `TextRecognition`-käyttöä.
- `gradle/libs.versions.toml`: `mlkitTextRecognition` ja `mlkit-text-recognition`, jos yllä oleva poistetaan.

Poista myös Firebase AI:n takia lisätyt Ktor-constraintit:

- `app/build.gradle.kts`: constraints riveillä, joiden `because(...)` sanoo "Firebase AI tuo Ktor 3.0.x -transitiiveja".
- `gradle/libs.versions.toml`: `ktor-client-logging` ja `ktor-client-websockets`, jos ne jäävät käyttämättömiksi.

Päivitä dependency verification:

- Poista tai regeneroi `gradle/verification-metadata.xml`:n AI-komponentit, kuten `com.google.firebase:firebase-ai`, `firebase-ai-ondevice-interop`, `firebase-appcheck*`, `com.google.mlkit:genai-*` ja käyttämättömät ML Kit OCR -komponentit.
- Turvallisin tapa on tehdä lähdekoodipoiston jälkeen:
  `./gradlew.bat --write-verification-metadata sha256 help --console=plain`

Päivitä analyysi- ja coverage-konfiguraatiot:

- `app/detekt-baseline.xml`: poista AI-tiedostoihin viittaavat baseline-ID:t.
- `sonar-project.properties`: poista coverage exclusionit AI-tiedostoille.
- `app/build.gradle.kts`: poista `jacocoCoverageExclusionPatterns`-listasta AI-tiedostot, joita ei enää ole.

## Pro-gatet ja tuotelistaus

Poista `app/src/main/java/com/finnvek/knittools/pro/ProState.kt`:stä AI-only featuret:

- `OCR`, jos yarn label -AI-skannaus poistuu eikä OCR-korvaajaa rakenneta.
- `GEMINI_NANO`
- `VOICE_LIVE`
- `AI_FEATURES`

`VOICE_COMMANDS` kannattaa pitää vain, jos paikallinen keyword-voice jää tuotteeseen. Jos kaikki voice poistetaan, poista sekin.

Päivitä Pro UI:

- `app/src/main/java/com/finnvek/knittools/ui/screens/pro/ProUpgradeScreen.kt`: poista listasta `pro_feature_ocr`, `pro_feature_ai_features`, `pro_feature_voice_live` ja tarvittaessa `pro_feature_voice_commands`.
- Kaikki `res/values*/strings.xml`: poista vastaavat `pro_feature_*`-käännökset.

## Counter ja project summary

Poista AI summary:

- `app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterSummaryGenerator.kt`
- `CounterUiState`: `isAiAvailable`, `projectSummary`, `summaryError`, `isSummaryLoading`
- `CounterViewModel`: `geminiAiService`, `aiQuotaManager`, `counterSummaryGenerator`, `refreshAiAvailability()`, `generateSummary()`, `clearSummary()`, `summaryJob`
- `ProjectActionsBottomSheet.kt`: `isAiAvailable`, `onOpenSummary`, "AI summary" -rivi
- `CounterScreen.kt`: summary sheet -tila ja `onOpenSummary`-kytkentä

Jos halutaan säilyttää täysin paikallinen projektikooste, siirrä `ProjectSummarizer.simpleSummary(...)` ennen AI-paketin poistoa uuteen ei-AI helperiin. Muuten poista koko summary-pinta.

## Äänikomennot

Poista Gemini-fallback:

- `CounterViewModel`: poista `VoiceCommandInterpreter`, `AiVoiceAction`, `voiceCommandCache`, `interpretVoiceCommand(...)`, `executeVoiceAction(...)` ja kaikki helperit, joita käytetään vain AI-tulkinnan tai Live API:n kautta.
- `CounterScreen.kt`: poista `voiceCommandHandler.unrecognizedText.collect { viewModel.interpretVoiceCommand(text) }`. Tämän jälkeen tunnistamattomat fraasit jäävät huomiotta.
- `strings.xml`: poista tai muuta `voice_quota_monthly_exhausted`; sitä tarvitaan vain AI-fallbackin quota-virheeseen.

Poista Live API:

- `CounterUiState`: `canUseVoiceLive`, `isLiveSessionActive`, `voiceLiveEnabled`
- `CounterViewModel`: `voiceLiveSession`, `voiceLiveQuotaManager`, `_fallbackToV2`, `fallbackToV2`, `startLiveVoice()`, `stopLiveVoice()`, `canStartLiveVoice()`, `observeLiveVoiceState()`, `handleLiveVoiceState()`, `buildLiveVoiceErrorMessage()`, `buildProjectVoiceContext()`, `buildFunctionCallHandler()`, `isOnline()`, sekä `onCleared()`-kohdan `voiceLiveSession.stop()`
- `CounterScreen.kt`: Live API -togglelogiikka, fallback snackbar, Live-session disposable cleanup ja `state.voiceLiveEnabled`-haarat
- `SettingsViewModel.kt`: `VoiceLiveQuotaManager`, `VoiceLiveUsage`, `voiceLiveUsage`
- `SettingsScreen.kt`: "Natural voice" -switch ja live quota -teksti
- `PreferencesManager.kt`: `voiceLiveEnabled`, `setVoiceLiveEnabled(...)`, `KEY_VOICE_LIVE`
- `AndroidManifest.xml`: `RECORD_AUDIO`-permission vain jos kaikki voice/journal speech poistetaan. Jos paikalliset voice-komennot jäävät, permission jää.

Pidettävä, jos paikallinen voice jää:

- `ui/screens/counter/VoiceCommandHandler.kt`
- `ui/screens/counter/VoiceCommandParser.kt`
- `ui/screens/counter/VoiceResponseManager.kt`
- voice-tekstiresurssit, jotka liittyvät paikalliseen palautteeseen ja TTS-vastauksiin
- `TooltipManager.VOICE_COMMANDS`, jos käytössä paikallisen voice-ominaisuuden tooltipinä

## Notes / journal

Poista AI-journal:

- `app/src/main/java/com/finnvek/knittools/ai/journal/JournalEntryProcessor.kt`
- `app/src/main/java/com/finnvek/knittools/ui/screens/notes/JournalEntryViewModel.kt`
- `app/src/main/java/com/finnvek/knittools/ui/screens/notes/JournalEntryBottomSheet.kt`
- `NotesEditorScreen.kt`: `JournalEntryViewModel`, `showJournalSheet`, `JournalEntryBottomSheet`, `journal_ai_badge`-nappi ja offline snackbar
- `NotesEditorViewModel.kt`: `AiQuotaManager`, `isAiAvailable`, AI-quota-tarkistus ja AI-käytettävyyden päivitys `appendJournalEntry(...)`-metodissa

`appendJournalEntry(...)` on käyttökelpoinen vain, jos halutaan jättää ei-AI "lisää päivätty merkintä" -toiminto. Jos bottom sheet poistuu kokonaan, metodi ja siihen liittyvät testit voi poistaa.

## Pattern viewer

Poista pattern AI -ominaisuudet:

- `PatternInstructionRepository.kt`
- `PatternViewerViewModel.kt`: `InstructionDisplayState`, `ExplanationState`, `CombineState`, instruction/explanation/combine cachet ja kaikki `instructionRepository`-kutsut
- `PatternViewerScreen.kt`: `CombinedInstructionResult`-importti, instruction overlay, explain-on-tap, combine instructions -toiminto, bottom sheet ja clipboard helperit
- `res/values*/strings.xml`: pattern combine / AI instruction -tekstit, jos niitä ei enää käytetä

Älä poista PDF-renderöintiä, annotations-ominaisuutta, page navigationia tai pattern camera scan -kuvasta PDF:ksi -polkua, ellei erikseen päätetä poistaa camera scan -ominaisuuksia.

## Yarn label scan

Poista yarn label AI -skannaus:

- `YarnLabelGeminiScanner.kt`
- `ParsedYarnLabel.kt`, jos mikään ei käytä skannattua mallia poiston jälkeen
- `YarnLabelScanRepository.kt`
- `YarnCardViewModel.kt`: `scanRepository`, `aiQuotaManager`, `canScanYarnLabel`, `loadFromScan(...)`, `scanWithGemini(...)`, scan request id -logiikka, `createScanPhotoUri()`, `deletePhotoFile(...)` jos vain skannaus käyttää sitä
- `MyYarnScreen.kt`: scan FAB, camera permission flow ja `onScanLabel` / `onCreateScanPhotoUri` / `onScanPhoto` -parametrit
- `YarnEstimatorScreen.kt`: camera scan -nappi ja scan-to-review -flow
- `NavGraph.kt`: `YarnCardReviewScreen`-reitit, jotka ovat vain scan-reviewta varten
- `YarnLabelCaptureResultHandler.kt`, jos muuta kamerakuvaus-flow'ta ei jää käyttämään sitä

Säästä tai harkitse erikseen:

- `YarnCard.photoUri` / Room entity -kenttä. Poisto vaatisi Room-migraation, ja kenttä voi edelleen olla hyödyllinen vanhojen skannattujen korttien kuvan näyttämiseen.
- `YarnLabelPhotoStorage.kt` ja `file_paths.xml` `yarn_photos`, jos vanhoja yarn card -kuvia halutaan näyttää/siivota.

## Paste instruction / Gemini Nano

Poista Gemini Nano:

- `ai/nano/InstructionParser.kt`
- `ai/nano/NanoAvailability.kt`
- `PasteInstructionButton.kt`, jos koko paste-to-parse poistetaan
- `CounterViewModel.refreshNanoAvailability()`, `isNanoAvailable`, `ProFeature.GEMINI_NANO`
- `GaugeScreen.kt` ja `IncreaseDecreaseScreen.kt`: `PasteInstructionButton`-käytöt
- `res/values*/strings.xml`: `paste_instruction`, `instruction_hint*`, `parsing_instruction`, `instruction_parsed`, `instruction_parse_failed`, `ai_error_busy`, `ai_error_quota`, `ai_error_unavailable`, jos mikään muu ei käytä niitä

Jos regex-parseri säilytetään ilman AI:tä, tee se ennen AI-paketin poistoa:

- Siirrä `ParsedInstruction` ja `parseWithRegex(...)` ei-AI-pakettiin.
- Poista `Generation.getClient()`, `GenerativeModel`, `FeatureStatus`, `GenAiException`, promptit, retryt ja mallin lataus.
- Nimeä UI uudelleen niin, ettei siinä luvata AI:tä tai Nanoa.

## Resurssit ja käännökset

Päivitä kaikki `app/src/main/res/values*/strings.xml`-tiedostot, ei vain `values/strings.xml`.

Poistettavia tai uudelleennimettäviä ryhmiä:

- AI summary: `view_ai_summary`, `ai_quota_exhausted`, `ai_summary_fallback`, `project_actions_ai_summary`
- Journal AI: kaikki `journal_*`, jos AI-journal bottom sheet poistuu kokonaan
- Pro AI: `pro_feature_ocr`, `pro_feature_ai_features`, `pro_feature_voice_live`, tarvittaessa `pro_feature_voice_commands`
- Yarn scan: `scan_yarn_label`, `yarn_scan_failed`, `scanned_yarn`, `scanning`, jos skannaus poistuu eikä muu käyttö tarvitse niitä
- Pattern AI: `pattern_combine_*`, `pattern_instruction_copied`, jos combine/instruction overlay poistuu
- Nano/parser: `paste_instruction`, `instruction_hint*`, `parsing_instruction`, `instruction_parsed`, `instruction_parse_failed`, `ai_error_busy`, `ai_error_quota`, `ai_error_unavailable`
- Live voice: `voice_live_error`, `voice_live_quota_exhausted`, `voice_natural_response`, `voice_live_quota_remaining`
- Voice AI fallback: `voice_quota_monthly_exhausted`, `voice_summary_unavailable`, `voice_summary_generating`, `voice_summary_none`, jos AI summary poistetaan eikä paikallinen summary jää

Varo `ai_error_unknown`-merkkijonoa: sitä käytetään myös ei-AI-polkujen geneerisenä virheenä, kuten delete-failure-, billing-, Ravelry-save- ja restore-purchase-polut. Sitä ei kannata vain poistaa. Parempi on nimetä se uudelleen esimerkiksi `generic_error_unknown` ja päivittää kutsujat.

`privacy_summary` sanoo jo "No analytics, no cloud sync, no accounts." AI-poiston jälkeen tämä on lähempänä totuutta, mutta Ravelry ja Play Billing ovat edelleen verkkointegraatioita. Päivitä privacy/online-offline-teksti täsmällisesti.

## Testit

Poista AI-only testit:

- `app/src/androidTest/java/com/finnvek/knittools/ai/AiQuotaManagerTest.kt`
- `app/src/test/java/com/finnvek/knittools/ai/AiQuotaSourceTest.kt`
- `app/src/test/java/com/finnvek/knittools/ai/GeminiStructuredOutputSourceTest.kt`
- `app/src/test/java/com/finnvek/knittools/ai/PatternInstructionCombinerGeminiTest.kt`
- `app/src/test/java/com/finnvek/knittools/ai/PatternInstructionGeminiTest.kt`
- `app/src/test/java/com/finnvek/knittools/ai/ProjectSummarizerTest.kt`
- `app/src/test/java/com/finnvek/knittools/ai/VoiceCommandInterpreterTest.kt`
- `app/src/test/java/com/finnvek/knittools/ai/YarnLabelGeminiScannerTest.kt`
- `app/src/test/java/com/finnvek/knittools/ai/journal/JournalEntryProcessorTest.kt`
- `app/src/test/java/com/finnvek/knittools/ai/live/LiveVoiceFunctionCallMapperTest.kt`
- `app/src/test/java/com/finnvek/knittools/ai/live/ProjectVoiceContextTest.kt`
- `app/src/test/java/com/finnvek/knittools/ai/live/VoiceFunctionDeclarationsSourceTest.kt`
- `app/src/test/java/com/finnvek/knittools/ai/live/VoiceLiveSessionTest.kt`
- `app/src/test/java/com/finnvek/knittools/ai/nano/InstructionParserTest.kt`, jos regex-parseria ei säilytetä
- `app/src/test/java/com/finnvek/knittools/repository/PatternInstructionRepositoryTest.kt`
- `app/src/test/java/com/finnvek/knittools/ui/screens/counter/CounterSummaryGeneratorTest.kt`

Muokkaa tai poista osittain AI:hin sidotut testit:

- `YarnCardViewModelTest.kt`: poista scan/Gemini/quota-testit tai korvaa ei-AI-kuvatoiminnon testeillä.
- `PatternViewerViewModelTest.kt`: poista instruction/combine/explain-testit.
- `NotesEditorViewModelTest.kt`: poista `isAiAvailable` ja `appendJournalEntry`-testit, jos journal poistuu.
- `SettingsViewModelTest.kt`: poista `VoiceLiveQuotaManager` ja usage-testi.
- `DataStoreErrorHandlingSourceTest.kt`: poista `AiQuotaManager` ja `VoiceLiveQuotaManager` -source-polut.
- `FeatureGateRaceSourceTest.kt`: poista voice-AI ja summary-AI -source-oletukset.
- `ProFeatureGateSourceTest.kt`: poista OCR/VoiceLive/AI gate -source-oletukset.
- `CounterStitchTrackingSourceTest.kt`: jos `AiVoiceAction.Reset` poistuu, päivitä reset-regressio paikalliseen `VoiceCommand.Reset`-polkuun tai poista testi.

## Dokumentaatio ja muistipaikat

Päivitä AI-poiston toteutuksen yhteydessä:

- `PROJECT.md`: poista AI-arkkitehtuuri, Firebase AI, quota, Gemini Nano, Live voice, AI-journal, yarn label Gemini, pattern instruction Gemini ja AI-testiosiot.
- `AGENTS.md` ja `CODEX.md`: pidä ne linjassa. Poista AI-arkkitehtuurisäännöt ja Firebase App Check -ohjeet, jos niitä ei enää ole.
- `memory/MEMORY.md`: lisää arkkitehtuurimuutos, koska vastuut ja data-flow muuttuvat.
- `README.md`: poista Firebase AI / `ai`-paketin kuvaus.
- `AI-ominaisuudet.md`: poista tiedosto tai korvaa "poistettu" -historialla.
- `GEMINI-NANO.md`: poista tiedosto tai merkitse vanhentuneeksi, jos Nano poistetaan.
- `ONLINE_OFFLINE.md`: kirjoita uusiksi niin, ettei siinä luvata AI- tai hybridipolkuja.
- `PRIVACY-POLICY.md`: varmista, ettei siinä ole ristiriitaa jäljelle jäävien Ravelry/Play Billing -verkkopolkujen kanssa.
- `sonar-project.properties`: poista AI coverage exclusionit.
- `config/semgrep/knittools-security.yml`: arvioi, tarvitseeko `prompt|voice|ai`-sanat edelleen pitää logisääntöjen regexissä. `voice` voi jäädä relevantiksi, jos paikallinen voice jää.

## Data ja migraatiot

Room-migraatiota ei näytä tarvittavan pelkkään AI-poistoon, jos et poista pysyviä kenttiä kuten `yarn_cards.photoUri`.

DataStoret:

- `ai_quota` ja `voice_live_quota` jäävät käyttäjän laitteelle hylätyiksi tiedostoiksi, jos managerit poistetaan. Tämä on yleensä hyväksyttävää.
- `voice_live_enabled` jää pääpreferencesiin hylätyksi avaimena, jos sitä ei enää lueta. Sitä ei tarvitse migroida pois, ellei haluta aktiivista cleanupia.

Jos halutaan siivota käyttäjien laitteelta vanhat AI-DataStoret, tee se erillisenä pieninä muutoksina ja testaa, ettei app startup hidastu tai kaadu korruptoituneisiin storeihin.

## Ehdotettu toteutusjärjestys

1. Päätä scope: pidetäänkö paikalliset voice-komennot ja säilytetäänkö regex-only paste-to-parse.
2. Poista Firebase/Gemini-pilvipalvelut ja niiden kutsujat: summary, yarn scan, pattern instruction, journal processor, voice Gemini fallback.
3. Poista Gemini Live API ja sen settings/preference/quota-polut.
4. Poista Gemini Nano tai irrota regex-parseri ei-AI-paikkaan.
5. Siivoa Pro-gatet, resurssit ja UI-navigaatio.
6. Siivoa Gradle, Firebase, google-services, dependency verification, detekt/sonar/jacoco.
7. Poista tai päivitä testit.
8. Päivitä `PROJECT.md`, `AGENTS.md`, `CODEX.md`, `memory/MEMORY.md`, README ja muut root-dokumentit.

## Tarkistuskomennot poiston jälkeen

Älä aja projektin wrapper-skriptejä `lc` tai `sc`; AGENTS.md sanoo, että käyttäjä ajaa ne itse.

Käytä ensin nopeita hakuja:

```powershell
rg -n -i "firebase\.ai|com\.google\.firebase\.ai|FirebaseAppCheck|Gemini|GenAI|AICore|Nano|AiQuota|VoiceLive|VoiceCommandInterpreter|JournalEntryProcessor|YarnLabelGemini|PatternInstructionGemini|ProjectSummarizer" app/src gradle build.gradle.kts settings.gradle.kts README.md PROJECT.md AGENTS.md CODEX.md
rg -n "ProFeature\.(OCR|GEMINI_NANO|AI_FEATURES|VOICE_LIVE)" app/src/main app/src/test
rg -n "ai_quota|voice_live_quota|voice_live_enabled" app/src/main app/src/test
rg -n "google-services|firebase-bom|firebase-ai|appcheck|mlkit-genai|genai-prompt" build.gradle.kts app/build.gradle.kts gradle/libs.versions.toml .github
```

Sitten käytä pienintä hyödyllistä build-varmistusta:

```powershell
./gradlew.bat --write-verification-metadata sha256 help --console=plain
./gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --console=plain
./gradlew.bat lint --console=plain
```

Jos pidät paikallisen voice-flow'n, lisää tai säilytä testit, jotka todistavat `VoiceCommandParser`in ja `VoiceCommandHandler`in peruskomennot ilman Gemini-importteja.

## Suurin riski

Suurin tekninen riski ei ole yksittäinen tiedosto vaan Hilt-graafin rikkoutuminen: `GeminiAiService`, `AiQuotaManager`, `VoiceLiveSession`, `VoiceLiveQuotaManager`, `JournalEntryProcessor`, `PatternInstructionRepository`, `YarnLabelScanRepository` ja `CounterSummaryGenerator` ovat injektoituja useaan ViewModeliin. Poisto kannattaa tehdä kutsujakerros kerrallaan ja kääntää `:app:compileDebugKotlin` usein.

Toinen riski on se, että `ai_error_unknown` on nimeltään AI mutta käytössä geneerisenä virheenä ei-AI-poluille. Se pitää nimetä hallitusti, ei poistaa sokkona.
