# KnitTools

## Mikä tämä tiedosto on

`PROJECT.md` on tämän repositorion käytännöllinen nykytilakuva.

Tavoite ei ole kuvata toiveita tai vanhoja speksejä, vaan vastata näihin:

- mitä sovelluksessa on juuri nyt
- mistä tiedostoista kyseisen asian source of truth löytyy
- missä kohdissa vanha oletus menee helposti pieleen

Jos `PROJECT.md` ja toteutus ovat ristiriidassa, koodi voittaa.

## Luottamusjärjestys

Nykytilan kannalta hyödyllinen järjestys:

1. lähdekoodi `app/src/main/java/...`
2. build- ja manifest-tiedostot
3. tämä tiedosto
4. `CODEX.md` / `AGENTS.md` työnkulkuohjeina
5. `README.md` viimeisenä, koska se ei ole projektin paras nykytilan lähde

## Snapshot

- Android-appi paketissa `com.finnvek.knittools`
- Moduulit: `:app`, `:baselineprofile`
- UI: Jetpack Compose + Material 3
- DI: Hilt
- Data: Room + DataStore + sisäinen tiedostotallennus
- Widgetit: Glance App Widget
- Verkko: Ktor + OkHttp
- Integraatiot: Ravelry OAuth2/API, Firebase AI, Google Play Billing, In-App Review, In-App Update
- On-device-ominaisuudet: ML Kit Text Recognition, ML Kit GenAI Prompt API, omat parserit `ai/nano/`
- Lokalisaatio: `localeConfig` + useat `values-*`-hakemistot
- Room schema version: `8`
- `compileSdk` / `targetSdk` / `minSdk`: `36 / 36 / 29`
- `baselineprofile`-moduulin `minSdk`: `28`
- Java target: `17`
- AGP: `9.1.0`
- Kotlin Compose plugin: `2.3.10`
- Room: `2.8.4`
- Glance: `1.1.1`
- Firebase BoM: `34.12.0`
- Billing: `8.3.0`
- versionCode / versionName: `1 / 1.0.0`

Source of truth:

- `app/build.gradle.kts`
- `baselineprofile/build.gradle.kts`
- `gradle/libs.versions.toml`
- `settings.gradle.kts`

## Nopea orientoituminen

Jos avaat vain muutaman tiedoston, avaa nämä:

- käynnistys:
  - `app/src/main/java/com/finnvek/knittools/App.kt`
  - `app/src/main/java/com/finnvek/knittools/MainActivity.kt`
- navigaatio:
  - `app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/navigation/KnitToolsBottomBar.kt`
- data:
  - `app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt`
  - `app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt`
  - `app/src/main/java/com/finnvek/knittools/repository/`
- Pro / billing / trial:
  - `app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt`
  - `app/src/main/java/com/finnvek/knittools/pro/`
- AI / voice:
  - `app/src/main/java/com/finnvek/knittools/ai/`
- widgetit:
  - `app/src/main/java/com/finnvek/knittools/widget/`

## Moduulit ja build

### `:app`

Päätuote. Sisältää käytännössä kaiken tuotantologiikan:

- Compose-screenit ja navigaatio
- Room- ja DataStore-kerrokset
- Ravelry-integraation
- AI- ja voice-flowt
- Play Billing / Pro / trial
- Glance-widgetin

Pluginit `app/build.gradle.kts`:ssä:

- `com.android.application`
- `org.jetbrains.kotlin.plugin.compose`
- `com.google.devtools.ksp`
- `com.google.dagger.hilt.android`
- `org.jlleitschuh.gradle.ktlint`
- `dev.detekt`
- `org.jetbrains.kotlin.plugin.serialization`
- `com.google.gms.google-services`
- `org.owasp.dependencycheck`
- `androidx.baselineprofile`

Build-huomiot:

- `org.jetbrains.kotlin.android`-pluginia ei käytetä
- release signing on ympäristömuuttujapohjainen
- release-artifaktit estetään ilman signing-muuttujia
- release-artifaktit estetään myös ilman Ravelry-credentialeja, ellei eksplisiittistä opt-in-lippua ole annettu
- debug lukee Ravelry-avaimet `local.properties`:sta

### `:baselineprofile`

Erillinen Android Test -moduuli:

- namespace: `com.finnvek.knittools.baselineprofile`
- target project: `:app`
- käyttää `androidx.benchmark.macro.junit4` + `uiautomator`
- `baselineProfile { useConnectedDevices = true }`

## Käynnistys ja runtime

Nykyinen käynnistyslogiikka:

1. `App.onCreate()`
   - `PreferencesManager.applyStoredAppLanguage()`
   - `BillingManager.initialize()`
   - `ProManager.initialize()`
2. `MainActivity.onCreate()`
   - `installSplashScreen()` ennen `super.onCreate()`
   - lukee mahdollisen `CounterLaunchRequest`in intentistä
   - käsittelee mahdollisen Ravelry OAuth callbackin
   - ottaa `enableEdgeToEdge()`-tilan käyttöön
   - käynnistää In-App Update -tarkistuksen
   - hakee asetukset `SettingsViewModel`in kautta
   - renderöi `KnitToolsNavHost`in
3. `MainActivity.onResume()`
   - kutsuu `inAppUpdateManager.checkDownloadedOnResume()`
4. `MainActivity.onNewIntent()`
   - päivittää OAuth- ja widget-launch-intentit

Lisähuomiot:

- appi on manifestissa lukittu portrait-orientaatioon
- snackbar näyttää flexible update -asennuskehotteen
- review-pyyntö kytketään runtime-tilaan, ei pelkkään staattiseen näkymään

## Navigaatio

`TopLevelDestination` tiedostossa `Screen.kt` on top-level-tasojen source of truth.

Nykyiset top-level-tabit:

1. `Projects`
2. `Library`
3. `Tools`
4. `Insights`
5. `Settings`

Top-level route-arvot:

- `projects_tab`
- `library_tab`
- `tools_tab`
- `insights_tab`
- `settings_tab`

Start-route per top-level:

- `Projects` -> `project_list`
- `Library` -> `library`
- `Tools` -> `tools`
- `Insights` -> `insights`
- `Settings` -> `settings`

Sovellus käynnistyy `Projects`-tabiin.

Bottom bar piilotetaan nykyään näillä routeilla:

- `pro_upgrade`
- `yarn_card_review`
- `yarn_card_detail/{cardId}`
- `pattern_viewer/{projectId}`
- `library_pattern_viewer/{savedPatternId}`
- `notes_editor/{projectId}`

ViewModel-scope:

- `CounterViewModel` on scoped `Projects`-graafin tasolle
- `LibraryViewModel` on scoped `Library`-graafin tasolle
- `YarnCardViewModel` luodaan nav host -tasolla ja jaetaan Tools/Library-flow’hin

## Reittikartta

### Projects-graafi

- `project_list`
- `counter`
- `photo_gallery`
- `pattern_viewer/{projectId}`
- `session_history/{projectId}`
- `notes_editor/{projectId}`

### Tools-graafi

- `tools`
- `gauge`
- `increase_decrease`
- `cast_on`
- `yarn`
- `yarn_card_review`
- `ravelry`
- `ravelry_detail/{patternId}`

### Library-graafi

- `library`
- `saved_patterns`
- `library_pattern_viewer/{savedPatternId}`
- `library_ravelry_detail/{patternId}`
- `my_yarn`
- `yarn_card_detail/{cardId}`
- `all_photos`
- referenssireitit:
  - `needles`
  - `size_charts`
  - `abbreviations`
  - `chart_symbols`

### Muut

- `insights`
- `settings`
- `pro_upgrade`

## Nykyinen screen-inventaario

Tämä lista kuvaa toteutuksessa olevat screen-tiedostot, ei suunnitelmia:

- `abbreviations/AbbreviationsScreen.kt`
- `caston/CastOnScreen.kt`
- `chartsymbols/ChartSymbolScreen.kt`
- `counter/CounterScreen.kt`
- `counter/PhotoGalleryScreen.kt`
- `gauge/GaugeScreen.kt`
- `home/HomeScreen.kt`
- `increase/IncreaseDecreaseScreen.kt`
- `insights/InsightsScreen.kt`
- `library/AllPhotosScreen.kt`
- `library/LibraryScreen.kt`
- `library/MyYarnScreen.kt`
- `library/SavedPatternsScreen.kt`
- `needles/NeedleSizeScreen.kt`
- `notes/NotesEditorScreen.kt`
- `pattern/PatternViewerScreen.kt`
- `pro/ProUpgradeScreen.kt`
- `project/ProjectListScreen.kt`
- `ravelry/RavelryDetailScreen.kt`
- `ravelry/RavelrySearchScreen.kt`
- `session/SessionHistoryScreen.kt`
- `settings/SettingsScreen.kt`
- `sizecharts/SizeChartScreen.kt`
- `yarn/YarnEstimatorScreen.kt`
- `yarncard/YarnCardReviewScreen.kt`

Huomio:

- `LibraryPatternViewerScreen` elää samassa tiedostossa kuin `PatternViewerScreen`
- `HomeScreen` on käytännössä Tools-tabin aloitusnäkymä

## Pakettikartta

### Sovelluslogiikan pääpaketit

- `ai/`
  - cloud: `GeminiAiService`, `PatternInstructionGemini`, `PatternInstructionCombinerGemini`, `PatternTextExtractor`, `ProjectSummarizer`, `YarnLabelGeminiScanner`, `VoiceCommandInterpreter`
  - quota: `AiQuotaManager`
  - journal: `ai/journal/`
  - live-voice: `ai/live/`
  - on-device parserit: `ai/nano/`
  - OCR: `ai/ocr/`
  - puhewrapper: `ai/speech/`
- `auth/`
  - `RavelryAuthManager.kt`
- `billing/`
  - `BillingManager.kt`
- `data/datastore/`
  - `AppLanguage.kt`
  - `PreferencesManager.kt`
- `data/local/`
  - Room entityt, DAO:t ja `KnitToolsDatabase`
- `data/remote/`
  - `RavelryApiService.kt`
  - `RavelryModels.kt`
- `data/storage/`
  - `PatternDocumentStorage.kt`
  - `ProgressPhotoStorage.kt`
- `di/`
  - `DatabaseModule.kt`
  - `NetworkModule.kt`
- `domain/calculator/`
  - laskenta- ja parserilogiikat
- `domain/model/`
  - domain-mallit
- `pro/`
  - `InAppReviewManager.kt`
  - `InAppUpdateManager.kt`
  - `ProManager.kt`
  - `ProState.kt`
  - `TrialManager.kt`
- `repository/`
  - `CounterRepository.kt`
  - `PatternAnnotationRepository.kt`
  - `ProgressPhotoRepository.kt`
  - `ProjectCounterRepository.kt`
  - `RavelryRepository.kt`
  - `ReminderRepository.kt`
  - `SavedPatternRepository.kt`
  - `YarnCardRepository.kt`
- `widget/`
  - `CounterWidget.kt`
  - `CounterWidgetActions.kt`
  - `CounterWidgetReceiver.kt`
  - `CounterWidgetState.kt`
  - `WidgetEntryPoint.kt`

## Data ja pysyvä tila

### Room

`KnitToolsDatabase` sisältää nämä entityt:

- `CounterProjectEntity`
- `CounterHistoryEntity`
- `YarnCardEntity`
- `SessionEntity`
- `RowReminderEntity`
- `ProgressPhotoEntity`
- `ProjectCounterEntity`
- `SavedPatternEntity`
- `PatternAnnotationEntity`

Migraatiotilanne:

- automaattiset migraatiot: `1 -> 2`, `2 -> 3`
- käsinkirjoitetut migraatiot: `3 -> 4`, `4 -> 5`, `5 -> 6`, `6 -> 7`, `7 -> 8`
- schema exportataan hakemistoon `app/schemas/.../8.json`

Näkyvä uusin lisäys:

- `counter_projects.targetRows` lisättiin migraatiossa `7 -> 8`

### DataStore

`PreferencesManager` on source of truth ainakin näille:

- teema (`ThemeMode`)
- appin kieli (`AppLanguage`)
- haptic feedback
- keep screen awake
- metriikka/imperial
- knitting tips -näyttö
- completed projects -näyttö
- project sort order
- voice live -kytkin
- dismissed tooltipit

Lisäksi käytössä on erillisiä DataStoreja:

- `ai_quota`
- `voice_live_quota`
- `trial_state`
- `counter_widget`

### Paikallinen tiedostodata

Entry pointit:

- `PatternDocumentStorage`
- `ProgressPhotoStorage`
- `FileProvider` + `res/xml/file_paths.xml`

## Kielet ja lokalisaatio

Manifest käyttää `android:localeConfig="@xml/locales_config"`.

`App` kutsuu käynnistyksessä `PreferencesManager.applyStoredAppLanguage()`.

Tuetut kielet `locales_config.xml`:n mukaan:

- `en`
- `fi`
- `sv`
- `de`
- `fr`
- `es`
- `pt`
- `it`
- `nb`
- `da`
- `nl`

Nykyiset locale-resurssihakemistot:

- `values`
- `values-da`
- `values-de`
- `values-es`
- `values-fi`
- `values-fr`
- `values-it`
- `values-nb`
- `values-nl`
- `values-pt`
- `values-sv`
- `values-night`

## Integraatiot

### Ravelry

Nykyinen toteutus:

- OAuth2 Authorization Code -flow
- Chrome Custom Tabs autentikointiin
- tokenit `EncryptedSharedPreferences`iin
- Ktor-pohjainen HTTP-client
- callback URI: `com.finnvek.knittools://oauth/callback`

BuildConfig-kentät:

- `RAVELRY_BASIC_AUTH_USER`
- `RAVELRY_BASIC_AUTH_PASSWORD`
- `RAVELRY_OAUTH2_CLIENT_ID`
- `RAVELRY_OAUTH2_CLIENT_SECRET`

Debug lukee nämä `local.properties`:sta.
Release lukee ne ympäristömuuttujista.

### Pro / Billing / Trial

Billing-tuote:

- `BillingManager.PRODUCT_ID = "knittools_pro"`

`ProStatus`:

- `TRIAL_ACTIVE`
- `TRIAL_EXPIRED`
- `PRO_PURCHASED`

`ProFeature`-enum sisältää tällä hetkellä:

- `UNLIMITED_PROJECTS`
- `FULL_HISTORY`
- `NOTES`
- `SECONDARY_COUNTER`
- `OCR`
- `GEMINI_NANO`
- `WIDGET`
- `ROW_REMINDERS`
- `PROGRESS_PHOTOS`
- `MULTIPLE_COUNTERS`
- `SHAPING_COUNTER`
- `REPEAT_SECTION`
- `PATTERN_CAMERA_SCAN`
- `INSIGHTS_CHARTS`
- `STREAK`
- `UNLIMITED_YARN`
- `VOICE_COMMANDS`
- `VOICE_LIVE`
- `AI_FEATURES`

Huomio nykytilasta:

- `ProState.hasFeature(feature)` palauttaa yhä käytännössä saman kuin `isPro`
- per-feature-gating on UI- ja käyttölogiikassa nimetty, mutta ei vielä eriytetty ostotasojen mukaan
- trialin pituus on `14` päivää

### AI

AI ei ole yksi ominaisuus vaan useita polkuja:

- cloud-Gemini: `ai/`
- notes/journal-flow: `ai/journal/`
- live-voice: `ai/live/`
- on-device parserit: `ai/nano/`
- OCR: `ai/ocr/`
- kevyt puheentunnistuswrapper: `ai/speech/SimpleSpeechRecognizer.kt`

`AiQuotaManager`:

- kuukausikiintiö `500`
- sama quota käytössä tekstipohjaisille AI-kutsuille
- klassinen voice-fallback käyttää samaa quota-järjestelmää

### Voice: kaksi erillistä putkea

1. Klassinen keyword-flow (`VOICE_COMMANDS`)
   - `ui/screens/counter/VoiceCommandHandler.kt`
   - `ai/VoiceCommandInterpreter.kt`
   - `ui/screens/counter/VoiceResponseManager.kt`
   - tunnistusjärjestys: exact keyword -> counted command -> first-word fallback -> Gemini
   - paikallinen EN+FI count-parseri kattaa käytännössä luvut 1–20
   - Gemini-fallback kuluttaa `AiQuotaManager`in kiintiötä

2. Gemini Live API -keskustelu (`VOICE_LIVE`)
   - `ai/live/VoiceLiveSession.kt`
   - `ai/live/ProjectVoiceContext.kt`
   - `ai/live/VoiceFunctionDeclarations.kt`
   - oma quota `VoiceLiveQuotaManager`
   - kuukausikiintiö `30` minuuttia

Wiring:

- `CounterViewModel` ja `CounterScreen` käyttävät kumpaakin putkea
- `PreferencesManager.voiceLiveEnabled` toimii erillisenä käyttöasetuksena Live-puolelle

### Notes / journal / AI append

Muistiinpanoissa on kaksi kerrosta:

- tavallinen debounced editori `NotesEditorViewModel`
- AI-journal append -flow `ai/journal/`

Nykyinen toteutus:

- `NotesEditorViewModel.onNotesChanged()` autosave 1000 ms debounce
- `appendJournalEntry()` lisää päivämääräotsikon ja tarvittaessa `Row {currentRow}`
- journal-flow voi syöttää siivotun tekstin notes-editoriin

## Widgetit

Nykyinen Glance-widget:

- on tuotantokoodissa
- on Pro-gatettu
- avaa sovelluksen suoraan counteriin
- käyttää kolmea responsive-kokoa:
  - small `120dp x 48dp`
  - medium `160dp x 160dp`
  - large `300dp x 160dp`
- small näyttää projektin nimen + laskurin
- medium ja large näyttävät lisäksi `+` / `-` -toiminnot
- actionit toteutetaan broadcast-receiverin kautta
- widget seuraa shared widget-statea ja peilaa sen myös instanssikohtaiseen Glance-stateen
- uuden widget-instanssin bootstrap:
  - ensin shared widget-store
  - fallbackina `CounterRepository.getFirstProject()`

UI-tila juuri nyt:

- sisäinen korttirakenne
- selkeämpi lämmin reunus
- paksumpi progress bar
- visuaalisesti eri koot käyttävät samaa komponenttiperhettä, small hieman tiiviimpänä

Source of truth:

- `app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt`
- `app/src/main/java/com/finnvek/knittools/widget/CounterWidgetState.kt`
- `app/src/main/java/com/finnvek/knittools/widget/CounterWidgetActions.kt`
- `app/src/main/res/xml/counter_widget_info.xml`
- `app/src/main/res/layout/widget_counter_preview.xml`

## Näkyvät UI-huomiot

Toteutuksessa näkyviä asioita, joita ei kannata päätellä vanhoista mockeista:

- bottom navigation on viritetty viidelle lokalisoidulle tabille
- `Tools` ei ole geneerinen dashboard-gridi vaan oma Home/Tool-entry-näkymä
- `Library` sisältää sekä sisällöt että reference-näkymät
- muistiinpanoissa on sekä full-screen editori että AI-journal-polku
- widgetit on viilattu korttimaisemmiksi, mutta niiden ulkoreuna on silti launcher-maskauksen armoilla

## Manifesti ja platform surface

Manifestin nykyinen pinta:

- permissions:
  - `INTERNET`
  - `VIBRATE`
  - `CAMERA`
  - `RECORD_AUDIO`
- `usesCleartextTraffic="false"`
- `android:allowBackup="false"`
- kamera-feature on `required="false"`
- `MainActivity` on `exported=true`
- `CounterWidgetReceiver` on `exported=true` + `BIND_APPWIDGET`
- `CounterWidgetActions` on `exported=false`
- `FileProvider` on `exported=false`

Huomio:

- appissa on `google-services`-plugin käytössä
- repo sisältää Firebase-kytkennän vaatiman `app/google-services.json`-tiedoston

## Testit ja verifiointi

Nykyiset testit painottuvat ainakin näihin:

- domain calculators
- AI-parserit ja wrapperit
- repository-logiikka
- Pro/trial-logiikka
- useat ViewModel- ja UI-tasoiset testit
- Android migration testit

Pienimmät hyödylliset tarkistuskomennot:

- `./gradlew assembleDebug`
- `./gradlew test`
- `./gradlew :app:detekt`
- `./gradlew lint`
- `./gradlew :app:generateBaselineProfile`

Älä käytä agenttityössä käyttäjän wrapper-skriptejä `lint-check` tai `security-check`.

## Ominaisuudet nykykoodin perusteella

### Projektit ja laskuri

- useita projekteja
- rivilaskuri
- stitch tracking
- useita projektikohtaisia laskureita
- shaping/repeating-counter-polut
- row reminders
- progress photos
- projektimuistiinpanot
- session history
- pattern-PDF:n liittäminen projektiin
- pattern viewer + annotations
- target rows

### Library

- saved patterns
- my yarn / yarn cards
- OCR + Gemini/Nano-pohjainen yarn label -skannaus
- all photos
- multi-select batch-poistot

### Tools

- gauge
- increase/decrease
- cast on
- yarn estimator
- Ravelry search/detail
- reference-näkymät: needles, size charts, abbreviations, chart symbols

### Insights

- ajankäyttö
- tahtimittarit
- charts/heatmap/streak-tyyppiset näkymät

### AI ja ääni

- klassinen voice command -flow
- Live API -voice flow
- AI-päiväkirjamerkinnät muistiinpanoihin
- Gemini/Nano-pohjaiset parserit

### Monetisaatio

- 14 päivän trial
- yksi kertamaksullinen Pro-tuote
- feature-nimet on mallinnettu `ProFeature`-enumilla

## Asiat jotka vanhenevat helposti

Näihin kannattaa suhtautua epäluuloisesti vanhoissa dokumenteissa:

- Room schema version: nykyinen on `8`, ei `7`
- `allowBackup`: nykyinen on `false`, ei `true`
- voice-parserin local count-range: käytännössä `1–20`, ei `1–100`
- widgetit eivät ole enää pelkkä basic counter-preview vaan niissä on oma state-sync ja viimeistelty kortti-UI
- `README.md` ei ole nykytilan source of truth

## Suhde muihin dokumentteihin

- `AGENTS.md`
  - työskentely- ja turvallisuussäännöt
- `CODEX.md`
  - sama ydinlinja Codex-käyttöön
- `CLAUDE.md`
  - hyödyllinen erityisesti product wording-, UX- ja visuaalisissa kysymyksissä

Kun tarvitset lopullisen teknisen totuuden, palaa aina koodiin.
