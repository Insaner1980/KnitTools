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
- release-artifaktit estetään ilman Ravelry-credentialeja
- release-artifaktit estetään myös ilman eksplisiittistä opt-in-lippua `KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS=true`, koska release upottaa Ravelry-arvot `BuildConfig`iin
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

Counterin projektivalinta:

- route `counter` ei kanna `projectId`:tä navigaatioargumenttina
- aktiivinen projekti valitaan ja säilytetään jaetussa `CounterViewModel`:ssä
- widget-launch, Ravelryn "Start Project" ja project list -navigaatio käyttävät samaa valintamallia
- `CounterLaunchRequest` on runtime-entry point intentti- ja cross-flow-launchille, ei pysyvä route-contract

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

Koodissa vahvistuvat nykyfaktat:

- 14 päivän ilmainen kokeilu
- yksi kertamaksullinen Pro-tuote (ei tilausmalli)

Huomio source of truthista:

- product ID + trial-pituus: koodi (`BillingManager`, `TrialManager`)
- saatavuus, hinnat ja store-listaukset eivät ole tämän tiedoston tekninen source of truth, koska ne eivät vahvistu toteutuksesta

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

## Teema, värit ja typografia

Source of truth:

- `app/src/main/java/com/finnvek/knittools/ui/theme/Color.kt`
- `app/src/main/java/com/finnvek/knittools/ui/theme/Theme.kt`
- `app/src/main/java/com/finnvek/knittools/ui/theme/Type.kt`
- `app/src/main/java/com/finnvek/knittools/ui/theme/Shapes.kt`
- `app/src/main/res/font/outfit.ttf`

### Teema-arkkitehtuuri

- Material 3 + Compose, oma `KnitToolsTheme`-wrapper
- light + dark color scheme, valinta `AppPreferences.themeMode`-asetuksen kautta
- `isSystemInDarkTheme()` vaikuttaa vain silloin kun käyttäjä on valinnut teemaksi `SYSTEM`
- visuaalinen suunta: "70s Craft Revival" — lämpimät oliivit, poltettu oranssi, avokado, sinappi
- ei dynaamisia (Material You) värejä, paletti on lukittu
- standardin `MaterialTheme.colorScheme` lisäksi erillinen `KnitToolsExtendedColors` (`MaterialTheme.knitToolsColors`)
- extended-tokenit: `surfaceTint`, `secondaryOutline`, `onSurfaceMuted`, `brandWine`, `inactiveContent`, `navBarContainer`, `navBarIndicator`

### Värit — dark

Taustat:

- `Background` `#1E1E12` — tumma oliivi, päätausta
- `BackgroundAlt` `#252518` — kontrastialue
- `Surface` `#2E2E20` — korttien peruspinta
- `SurfaceHigh` `#3A3A2A` — korotetut kortit
- `SurfaceHighest` `#454535` — syötekentät

Brand:

- `Primary` `#C45100` — poltettu oranssi (CTA, + nappi)
- `PrimaryContainer` `#D4722A` — vaaleampi oranssi, gradientit
- `Secondary` `#8BA44A` — avokado (labelit, osio-otsikot, "CURRENT ROW")
- `SecondaryMuted` `#6B8A35`, `SecondaryContainer` `#3A4020`
- `Tertiary` `#C9A435` — sinappi (vinkit, aksentit)
- `TertiaryContainer` `#3A3520` — quick tip -kortin tausta

Teksti:

- `TextPrimary` `#E8E4D0` — lämmin kerma
- `TextSecondary` `#B8B4A0`
- `TextMuted` `#8A866E`
- `TextDisabled` `#5A5840`

Aksentti:

- `DustyRose` `#B8908F` — Pro trial -teksti, AI summary, yarn card

Status:

- `Error` `#C44D4D`, `ErrorContainer` `#3A2020`
- `Success` `#8BA44A` (= Secondary), `SuccessContainer` `#3A4020`

Navigaatio (alanavi):

- `NavBackground` `#161610` — erittäin tumma
- `NavText` `#B0AC92` — inaktiiviset
- `NavActive` `#C45100`
- `NavActiveBg` `#3A2010` — aktiivisen tabin indikaattori

Ravelry-erikoistapaukset: `RavelryTeal` `#5F8A8B`, `LightRavelryTeal` `#4A7172`.

### Värit — light

Taustat (light):

- `LightBackground` `#E8E4D0` — lämmin kerma, sama sävy kuin dark-teeman pääteksti ja app-ikonin tausta
- `LightBackgroundAlt` `#DDD8C3`
- `LightSurface` `#D2CDB5` — korttien peruspinta
- `LightSurfaceHigh` `#BBB59A` — korotetut kortit (huom. tummempi = korkeampi korotus)
- `LightSurfaceMediumHigh` `#C8C3A8` — dialogit, popupit
- `LightSurfaceHighest` `#A49D80` — syötekentät

Brand (primary jaettu darkin kanssa):

- `LightSecondary` `#6B8A2E` — tummempi avokado
- `LightSecondaryMuted` `#5A7525`, `LightSecondaryContainer` `#D0DDB5`
- `LightTertiary` `#9A7B18` — tumma kulta/sinappi
- `LightTertiaryContainer` `#E8DFB5`

Teksti (lämmin ruskea, ei mustaa):

- `LightTextPrimary` `#2E2A1E`
- `LightTextSecondary` `#5C5643`
- `LightTextMuted` `#8A8370`
- `LightTextDisabled` `#C0BAA5`

Aksentti (light): `LightDustyRose` `#9E706E`.

Status (light): `LightErrorContainer` `#EAD0D0`, `LightSuccessContainer` `#D0DDB5`.

Navigaatio (light):

- `LightNavBackground` `#DDD8C3`
- `LightNavText` `#5A5440`
- `LightNavActiveBg` `#EAD0B5`

Erotin: `LightDivider` `#C5C0A8`.

### Lankaikonipaletti

Deterministinen ID-pohjainen valinta listasta `YarnColors` (`Color.kt`):

- `#C45100` poltettu oranssi
- `#8BA44A` avokado
- `#C9A435` sinappi
- `#B8908F` dusty rose
- `#9A6B4A` terrakotta
- `#5A8A7A` teal
- `#9A82AA` laventeli
- `#A85A3A` ruosteenpunainen

### Typografia

Fontti:

- yksi family: **Outfit**, variable font (`res/font/outfit.ttf`)
- weightit: `Normal`, `Medium`, `SemiBold`, `Bold`, `ExtraBold`
- ladataan `FontVariation.Settings(FontVariation.weight(...))`-kautta

Material 3 -roolit `AppTypography`:ssa (size sp, letter spacing sp):

| Rooli | Weight | Size | Letter spacing |
|-|-|-|-|
| `displayLarge` | Bold | 57 | -0.25 |
| `displayMedium` | Bold | 45 | 0 |
| `displaySmall` | SemiBold | 36 | 0 |
| `headlineLarge` | Bold | 32 | 0 |
| `headlineMedium` | SemiBold | 28 | 0 |
| `headlineSmall` | SemiBold | 24 | 0 |
| `titleLarge` | SemiBold | 22 | 0 |
| `titleMedium` | SemiBold | 16 | 0.15 |
| `titleSmall` | Medium | 14 | 0.1 |
| `bodyLarge` | Normal | 16 | 0.5 |
| `bodyMedium` | Normal | 14 | 0.25 |
| `bodySmall` | Normal | 12 | 0.4 |
| `labelLarge` | SemiBold | 14 | 0.1 |
| `labelMedium` | SemiBold | 12 | 0.5 |
| `labelSmall` | SemiBold | 11 | 1.5 (all-caps: "CURRENT ROW", "QUICK TIP", nav-labelit) |

Säännöt:

- ei inline-overrideja `letterSpacing` / `fontSize` / `fontWeight` Type.kt:n ulkopuolella
- ainoa hyväksytty poikkeus: CounterScreenin pääluku **115sp Bold**

### Muodot (`AppShapes`)

- `small` — `RoundedCornerShape(8.dp)`
- `medium` — `RoundedCornerShape(12.dp)` (kortit)
- `large` — `RoundedCornerShape(16.dp)` (modaalit, isot pinnat)

### Pinta- ja scaffold-säännöt

- Scaffold-taustaväri kaikissa näytöissä: `MaterialTheme.colorScheme.background` (ei `surface`)
- `ToolScreenScaffold`: puhdas teemapinta, ei ambient-kuvia, transparent TopAppBar, max content width `600dp`
- Tools/Library-listoissa: ei ikoneita korteissa, otsikkoteksti aksenttivärillä per kohde
- Window insets: NavHost käsittelee `consumeWindowInsets(scaffoldPadding)`, sisemmät Scaffoldit eivät lisää tuplainsetejä

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

Julkaisuvalmiuden muistilista:

- pidä dependency-check kehitysvaiheessa manuaalisena, mutta dokumentoi ennen julkaisua puhtaan koneen komento ja tarvittavat `DEPENDENCY_CHECK_AUTO_UPDATE` / `NVD_API_KEY` -odotukset
- päätä ennen julkaisua, jääkö Baseline Profile manuaaliseksi vai lisätäänkö sille emulaattori-/managed-device-polku CI:hin
- lisää `ktlintCheck` pakolliseksi vasta, kun nykyinen koodi on siivottu ktlint-puhtaaksi eikä se hidasta normaalia ominaisuuskehitystä

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
- reference-näkymät: needles, size charts, abbreviations, chart symbols

### Tools

- gauge
- increase/decrease
- cast on
- yarn estimator
- Ravelry search/detail

### Insights

- ajankäyttö
- tahtimittarit
- charts/heatmap/streak-tyyppiset näkymät
- debug-build voi näyttää placeholder-projektidataa charttiin, jos oikeaa sessiodataa ei vielä ole

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
