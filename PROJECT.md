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
- Integraatiot: Ravelry OAuth2/API, Google Play Billing, In-App Review, In-App Update
- Paikalliset lisäominaisuudet: regex-pohjainen laskuriohjeparseri `domain/calculator/InstructionParser.kt`
- Lokalisaatio: `localeConfig` + useat `values-*`-hakemistot
- Room schema version: `12`
- `compileSdk` / `targetSdk` / `minSdk`: `36 / 36 / 29`
- `baselineprofile`-moduulin `minSdk`: `29`
- Java target: `17`
- Gradle wrapper: `9.4.1`
- AGP: `9.1.0`
- Kotlin Compose plugin: `2.3.10`
- KSP: `2.3.6`
- Compose BOM: `2026.05.01`
- Room: `2.8.4`
- Glance: `1.1.1`
- Ktor: `3.5.0`
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
  - `app/src/main/java/com/finnvek/knittools/MainActivityTheme.kt`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/res/values/themes.xml`
- navigaatio:
  - `app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/navigation/KnitToolsBottomBar.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/navigation/CounterLaunchRequest.kt`
- data:
  - `app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt`
  - `app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt`
  - `app/src/main/java/com/finnvek/knittools/repository/`
- Pro / billing / trial:
  - `app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt`
  - `app/src/main/java/com/finnvek/knittools/pro/`
- paikallinen laskuriohjeparseri:
  - `app/src/main/java/com/finnvek/knittools/domain/calculator/InstructionParser.kt`
- widgetit:
  - `app/src/main/java/com/finnvek/knittools/widget/`

## Moduulit ja build

### `:app`

Päätuote. Sisältää käytännössä kaiken tuotantologiikan:

- Compose-screenit ja navigaatio
- Room- ja DataStore-kerrokset
- Ravelry-integraation
- paikallinen laskuriohjeparseri
- Play Billing / Pro / trial
- Glance-widgetin

Pluginit `app/build.gradle.kts`:ssä:

- `com.android.application`
- `org.jetbrains.kotlin.plugin.compose`
- `com.google.devtools.ksp`
- `androidx.room`
- `com.google.dagger.hilt.android`
- `org.owasp.dependencycheck`
- `androidx.baselineprofile`
- `org.jlleitschuh.gradle.ktlint`
- `dev.detekt`
- `org.jetbrains.kotlin.plugin.serialization`
- `com.github.skydoves.compose.stability.analyzer`
- `jacoco`

Build-huomiot:

- `org.jetbrains.kotlin.android`-pluginia ei käytetä
- root-buildissä `dependency-analysis` on tarkoituksella kommentoitu pois AGP 9.x -yhteensopivuuden takia
- Sonar-konfiguraatio delegoi Gradlen hallitsemat source/binary-polut Gradle-pluginille ja ajaa `:app:jacocoDebugUnitTestReport` ennen `sonar`-taskia
- release signing on ympäristömuuttujapohjainen
- release-artifaktit estetään ilman signing-muuttujia
- release-artifaktit estetään ilman Ravelry-credentialeja
- release-artifaktit estetään ilman eksplisiittistä opt-in-lippua `KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS=true`, koska release upottaa Ravelry-arvot `BuildConfig`iin tietoisena no-backend-riskinä
- debug lukee Ravelry-avaimet ignored `debug.credentials.properties` -tiedostosta

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
   - käynnistää In-App Update -tarkistuksen
   - lukee teeman suoraan `PreferencesManager.preferences`-flow'sta
   - pitää splashin näkyvissä kunnes startup-teema on ratkaistu
   - ottaa `enableEdgeToEdge()`-tilan käyttöön vasta kun light/dark-teema tiedetään
   - renderöi `KnitToolsNavHost`in
3. `MainActivity.onResume()`
   - kutsuu `inAppUpdateManager.checkDownloadedOnResume()`
   - synkronoi Android 13+ per-app-locale-tilan takaisin DataStore-peiliin
4. `MainActivity.onNewIntent()`
   - päivittää OAuth- ja widget-launch-intentit

Lisähuomiot:

- appi on manifestissa lukittu portrait-orientaatioon
- widget-counter-launch vaatii `CounterLaunchTokenStore`n antaman tunnetun launch-id:n; OAuth callback ei saa avata counteria
- kulutettu counter-launch-id säilytetään recreationin yli, ja kulutetut intent-extrat poistetaan
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
- `pattern_viewer/{projectId}`
- `library_pattern_viewer/{savedPatternId}`
- `notes_editor/{projectId}`

ViewModel-scope:

- `CounterViewModel` on scoped `Projects`-graafin tasolle
- `LibraryViewModel` on scoped `Library`-graafin tasolle
- `YarnCardViewModel` on scoped `Library`-graafin parent-entryn tasolle `yarn_card_detail/{cardId}`-reitillä

Counterin projektivalinta:

- route `counter` ei kanna `projectId`:tä navigaatioargumenttina
- aktiivinen projekti valitaan ja säilytetään jaetussa `CounterViewModel`:ssä
- widget-launch, Ravelryn "Start Project", yarn-card detailin linked-project-avaus ja project list -navigaatio käyttävät samaa valintamallia
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
- `yarncard/YarnCardDetailScreen.kt`

Huomio:

- `LibraryPatternViewerScreen` elää samassa tiedostossa kuin `PatternViewerScreen`
- `HomeScreen` on käytännössä Tools-tabin aloitusnäkymä

## Pakettikartta

### Sovelluslogiikan pääpaketit

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
  - `AppFileStorage.kt`
  - `CounterLaunchTokenStore.kt`
  - `PatternDocumentStorage.kt`
  - `PdfPageRenderer.kt`
  - `ProgressPhotoStorage.kt`
  - `StorageFileNames.kt`
- `di/`
  - `DatabaseModule.kt`
  - `DispatchersModule.kt`
  - `NetworkModule.kt`
- `domain/calculator/`
  - laskenta- ja paikalliset parserilogiikat, mukaan lukien regex-pohjainen `InstructionParser`
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
  - `ProjectNameRules.kt`
  - `ProjectCounterRepository.kt`
  - `RavelryRepository.kt`
  - `ReminderRepository.kt`
  - `SavedPatternRepository.kt`
  - `YarnCardRepository.kt`
- `widget/`
  - `CounterWidget.kt`
  - `CounterWidgetActions.kt`
  - `CounterWidgetDataResolver.kt`
  - `CounterWidgetReceiver.kt`
  - `CounterWidgetState.kt`
  - `WidgetCounterAction.kt`
  - `WidgetEntryPoint.kt`
- `util/`
  - `extensions/UnitConversion.kt`

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
- `ProjectYarnNoteEntity`
- `SavedPatternEntity`
- `PatternAnnotationEntity`

Migraatiotilanne:

- automaattiset migraatiot: `1 -> 2`, `2 -> 3`
- käsinkirjoitetut migraatiot: `3 -> 4`, `4 -> 5`, `5 -> 6`, `6 -> 7`, `7 -> 8`, `8 -> 9`, `9 -> 10`, `10 -> 11`, `11 -> 12`
- schema exportataan hakemistoon `app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/`, jossa uusin export on `12.json`

Näkyvä uusin lisäys:

- `sessions.durationSeconds` ja `sessions.rowsWorked` lisättiin migraatiossa `9 -> 10`; vanhat rivit backfillataan `durationMinutes * 60` ja positiivisella `endRow - startRow` -arvolla
- `sessions(endedAt, startedAt)` ja `sessions(projectId, endedAt, startedAt)` -indeksit lisättiin migraatiossa `10 -> 11`
- `project_yarn_notes` lisättiin migraatiossa `11 -> 12`
- `sessions.startedAt`-indeksi on migraatiossa `8 -> 9`
- `counter_projects.targetRows` on migraatiossa `7 -> 8`

Session-laskennan nykyrajat:

- `KnitSession` ja `SessionEntity` kantavat sekä display-minuutit että tarkat `durationSeconds` / `rowsWorked` -kentät
- `SessionDao.getTotalMinutes(...)` summaa `durationSeconds`-kentän ja pyöristää ylöspäin minuutteihin
- Insights käyttää `SessionMetrics`-apuria, joka jakaa cross-midnight-sessiot laitteen paikallisiin päiviin ja laskee pace-arvot sekunneista ja tehdyistä riveistä

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
- dismissed tooltipit

Lisäksi käytössä on erillisiä DataStoreja:

- `trial_state`
- `review_state`
- `counter_widget`

### Paikallinen tiedostodata

Entry pointit:

- `AppFileStorage`
- `PatternDocumentStorage`
- `PdfPageRenderer`
- `ProgressPhotoStorage`
- `FileProvider` + `res/xml/file_paths.xml`

Tallennuspolut nykykoodissa:

- pattern PDF:t tallennetaan appin sisäiseen `pattern_pdfs/<projectId>`-hakemistoon `file://`-URIlla
- pattern camera capture -kuvat luodaan `pattern_captures/<projectId>`-hakemistoon ja ne ovat FileProviderin kautta ulos annettava väliaikainen pattern-kuvapolku
- progress-kuvat tallennetaan `progress_photos/<projectId>`-hakemistoon
- `file_paths.xml` exposeeraa vain `yarn_photos`, `progress_photos` ja `pattern_captures`; app-owned `pattern_pdfs` avataan sisäisen resolverin kautta, ei FileProvider-rootina
- `AppFileStorage` tunnistaa edelleen legacy `patterns/...` -FileProvider-URI:t sisäistä lukua/siivousta varten

## Kielet ja lokalisaatio

Manifest käyttää `android:localeConfig="@xml/locales_config"`.

`App` kutsuu käynnistyksessä `PreferencesManager.applyStoredAppLanguage()`.
`MainActivity.onResume()` kutsuu Android 13+ -laitteilla `syncAppLanguageFromSystem()`, joten DataStore `app_language` on nykyään per-app-locale-tilan peili eikä ainoa runtime-kielen omistaja.

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

- OAuth2 Authorization Code -flow Chrome Custom Tabilla; PKCE `S256` -parametrit lähetetään authorization requestissa
- Chrome Custom Tabs autentikointiin
- access/refresh-tokenit sekä pending `state`/`code_verifier` tallennetaan `EncryptedSharedPreferences`iin `MasterKey`-avaimella
- Ktor + OkHttp -pohjainen HTTP-client, jossa `connectTimeout=15s`, `callTimeout=45s`, `read/writeTimeout=30s`
- callback URI: `com.finnvek.knittools://oauth/callback`
- API-kutsu käyttää ensin Bearer-tokenia, refreshaa 401/403-vastauksen jälkeen, signouttaa refresh-epäonnistumisen jälkeen ja putoaa Basic Auth -polkuun
- transientit 5xx-vastaukset yritetään uudelleen rajatusti; muut ei-2xx-vastaukset nostavat `RavelryHttpException`in

BuildConfig-kentät:

- `RAVELRY_BASIC_AUTH_USER`
- `RAVELRY_BASIC_AUTH_PASSWORD`
- `RAVELRY_OAUTH2_CLIENT_ID`
- `RAVELRY_OAUTH2_CLIENT_SECRET`

Debug lukee nämä `debug.credentials.properties`:sta.
Release lukee ne ympäristömuuttujista ja vaatii accepted-risk opt-in -lipun.

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

Huomio nykytilasta:

- `ProState.hasFeature(feature)` palauttaa yhä käytännössä saman kuin `isPro`
- per-feature-gating on UI- ja käyttölogiikassa nimetty, mutta ei vielä eriytetty ostotasojen mukaan
- trialin pituus on `14` päivää

### Paikallinen parseri

Sovelluksessa ei ole enää mallipohjaista tulkintakerrosta eikä voice-command-flowta. Jäljellä oleva laskuriohjeiden tulkinta on paikallinen:

- `domain/calculator/InstructionParser.kt`
  - paste-to-parse käyttää regex-pohjaista paikallista parseria
  - parseri ei tee verkko- tai SDK-kutsuja

Älä päättele vanhoista dokumenteista, että counterissa olisi puhekomentoja: tuotantokoodissa ei ole voice-handleria, TTS-vastaajaa eikä mikrofonilupaa.

### Notes

Muistiinpanoissa on tavallinen debounced editori `NotesEditorViewModel`.

Nykyinen toteutus:

- `NotesEditorViewModel.onNotesChanged()` autosave 1000 ms debounce
- `CounterRepository.saveProjectNotes()` yhdistää muokkaukset editorin pohjatekstiin, jotta rinnakkaiset tallennukset eivät ylikirjoitu

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
  - ensin instanssin oma Glance-state
  - sitten shared widget-store
  - fallbackina `CounterRepository.getLatestActiveProject()`
  - viimeisenä `CounterWidgetState.defaultData(...)`
- widget-toiminnot kulkevat `CounterRepository.applyWidgetCountChange(...)` -metodin kautta, joka tekee count/history/current-stitch-reset -päivityksen transaktiona

UI-tila juuri nyt:

- sisäinen korttirakenne
- selkeämpi lämmin reunus
- paksumpi progress bar
- target rows, section ja stitch tracking näkyvät widget-datassa, jos aktiivinen projekti käyttää niitä
- visuaalisesti eri koot käyttävät samaa komponenttiperhettä, small hieman tiiviimpänä

Source of truth:

- `app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt`
- `app/src/main/java/com/finnvek/knittools/widget/CounterWidgetDataResolver.kt`
- `app/src/main/java/com/finnvek/knittools/widget/CounterWidgetState.kt`
- `app/src/main/java/com/finnvek/knittools/widget/CounterWidgetActions.kt`
- `app/src/main/java/com/finnvek/knittools/widget/CounterWidgetReceiver.kt`
- `app/src/main/java/com/finnvek/knittools/widget/WidgetCounterAction.kt`
- `app/src/main/java/com/finnvek/knittools/widget/WidgetEntryPoint.kt`
- `app/src/main/java/com/finnvek/knittools/MainActivity.kt`
- `app/src/main/AndroidManifest.xml`
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

- `DustyRose` `#B8908F` — Pro trial -teksti, yarn card

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
- muistiinpanoissa on full-screen editori
- widgetit on viilattu korttimaisemmiksi, mutta niiden ulkoreuna on silti launcher-maskauksen armoilla

## Manifesti ja platform surface

Manifestin nykyinen pinta:

- permissions:
  - `INTERNET`
  - `VIBRATE`
  - `CAMERA`
- `usesCleartextTraffic="false"`
- `android:allowBackup="false"`
- `android:dataExtractionRules="@xml/data_extraction_rules"`
- `android:fullBackupContent="@xml/backup_rules"`
- kamera-feature on `required="false"`
- `MainActivity` on `exported=true`
- `CounterWidgetReceiver` on `exported=true` + `BIND_APPWIDGET`
- `CounterWidgetActions` on `exported=false`
- `FileProvider` on `exported=false`

Huomio:

- appissa ei ole `google-services`-pluginia
- `app/google-services.json` ei kuulu nykyiseen buildiin eikä sitä pidä commitoida

## Testit ja verifiointi

Nykyiset testit painottuvat ainakin näihin:

- domain calculators
- paikalliset parserit
- repository-logiikka
- data storage- ja Room source/migration -rajat
- Pro/trial-logiikka
- billing restore / already-owned -polut
- useat ViewModel- ja UI-tasoiset testit
- Android migration testit
- navigation argument safety ja counter launch -tokenointi
- widget data resolver ja action flow

Pienimmät hyödylliset tarkistuskomennot:

- `./gradlew assembleDebug`
- `./gradlew test`
- `./gradlew :app:detekt`
- `./gradlew lint`
- `./gradlew :app:jacocoDebugUnitTestReport`
- `./gradlew :app:generateBaselineProfile`

Julkaisuvalmiuden muistilista:

- pidä dependency-check kehitysvaiheessa manuaalisena, mutta dokumentoi ennen julkaisua puhtaan koneen komento ja tarvittavat `DEPENDENCY_CHECK_AUTO_UPDATE` / `NVD_API_KEY` -odotukset
- päätä ennen julkaisua, jääkö Baseline Profile manuaaliseksi vai lisätäänkö sille emulaattori-/managed-device-polku CI:hin
- pidä `ktlintCheck`, detekt ja Android lint pakollisina CI:ssä; nykyinen build-workflow ajaa `assembleDebug`, `test`, `:app:ktlintCheck`, `:app:detekt` ja `lint`
- CodeQL-workflow on manuaalibuildinen Java/Kotlin-analyysi ja rakentaa `assembleDebug --no-daemon`

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
- saved pattern avaa `library_pattern_viewer/{savedPatternId}`-reitin vain paikalliselle/importoidulle pattern-URI:lle; Ravelry-linkit avaavat `library_ravelry_detail/{patternId}`-reitin
- `My Yarn` listaa olemassa olevat yarn cardit ja avaa `yarn_card_detail/{cardId}`-näkymän
- yarn card detailissä voi muuttaa statusta, määrää ja projektia, avata linkitetyn projektin counteriin sekä poistaa kortin
- tämän checkoutin tuotantokoodissa `YarnCardRepository.saveCard(...)`-metodille ei ole UI-kutsuja; älä oleta erillistä manuaalista yarn card -luontilomaketta ilman uutta kooditarkistusta
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
- Pro-gating tyhjentää chart-listat non-Pro-tilassa, mutta perusmittarit lasketaan edelleen `InsightsUiState`en
- debug-build näyttää footer-tekstin myös ilman sessiodataa; chartit eivät rakenna keksittyä placeholder-dataa

### Parseri

- regex-pohjainen pasted instruction -parseri laskureiden avuksi

### Monetisaatio

- 14 päivän trial
- yksi kertamaksullinen Pro-tuote
- feature-nimet on mallinnettu `ProFeature`-enumilla

## Asiat jotka vanhenevat helposti

Näihin kannattaa suhtautua epäluuloisesti vanhoissa dokumenteissa:

- build-versiot muuttuvat usein `gradle/libs.versions.toml`-tiedostossa; älä kopioi niitä muistista
- `allowBackup`: nykyinen on `false`, ei `true`
- Room schema version: nykyinen on `12`; tarkista aina `KnitToolsDatabase.version` ja `app/schemas/...`
- voice-command-flow on poistettu; älä palauta sitä ilman uutta product/security-päätöstä
- widgetit eivät ole enää pelkkä basic counter-preview vaan niissä on oma state-sync ja viimeistelty kortti-UI
- vanhat `yarn_card_review` / `library_yarn_card_review` -reitit eivät ole nykyisessä `Screen.kt` / `NavGraph.kt` -pinnassa; käytössä on `yarn_card_detail/{cardId}`
- `AppLanguage.promptLanguageName()` on ilman tuotantokutsuja oleva legacy-helper; sen nimi tai kommenttisanasto ei yksin todista mallipohjaisen parserin tai pilvi-AI:n olemassaoloa
- `README.md` ei ole nykytilan source of truth

## Suhde muihin dokumentteihin

- `AGENTS.md`
  - työskentely- ja turvallisuussäännöt
- `CODEX.md`
  - sama ydinlinja Codex-käyttöön
- `CLAUDE.md`
  - hyödyllinen erityisesti product wording-, UX- ja visuaalisissa kysymyksissä

Kun tarvitset lopullisen teknisen totuuden, palaa aina koodiin.
