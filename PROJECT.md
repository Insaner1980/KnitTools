# KnitTools

## Mikä tämä tiedosto on

`PROJECT.md` on tämän repositorion koodilähtöinen nykytilakuva.

Tämä dokumentti yrittää vastata kolmeen kysymykseen:

- mitä sovelluksessa on juuri nyt
- mistä tiedostoista kyseisen asian source of truth löytyy
- mitä osa-alueita ei kannata päätellä vanhoista spekseistä tai template-dokumenteista

Jos tämä tiedosto ja toteutus ovat ristiriidassa, koodi voittaa.

Huomio: repojuuren `README.md` on edelleen pitkälti starter-template eikä projektin paras nykytilan lähde.

## Snapshot

- Android-sovellus paketissa `com.finnvek.knittools`
- Moduulit: `:app`, `:baselineprofile`
- Build-järjestelmä: Gradle Kotlin DSL + version catalog
- UI: Jetpack Compose + Material 3
- DI: Hilt
- Persistenssi: Room + DataStore + sisäinen tiedostotallennus
- Widgetit: Glance App Widget
- Verkko: Ktor + OkHttp
- Etäintegraatiot: Ravelry OAuth2/API, Firebase AI, Google Play Billing, In-App Review, In-App Update
- On-device AI / parsing: ML Kit Text Recognition, ML Kit GenAI Prompt API, omat parserit `ai/nano/` ja `ai/ocr/`
- Lokalisaatio: `localeConfig` käytössä + erilliset `values-*`-resurssihakemistot useille kielille
- Room schema version: `7`
- `compileSdk` / `targetSdk` / `minSdk`: `36 / 36 / 29`
- `baselineprofile`-moduulin `minSdk`: `28`
- Java target: `17`
- AGP: `9.1.0`
- Kotlin Compose -plugin: `2.3.10`

Build- ja version source of truth:

- `settings.gradle.kts`
- `app/build.gradle.kts`
- `baselineprofile/build.gradle.kts`
- `gradle/libs.versions.toml`

## Source Of Truth

Nopein tapa orientoitua nykyiseen koodiin:

- Käynnistys ja applikaatiotason init:
  - `app/src/main/java/com/finnvek/knittools/App.kt`
  - `app/src/main/java/com/finnvek/knittools/MainActivity.kt`
- Navigaatio:
  - `app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/navigation/KnitToolsBottomBar.kt`
- Tietokanta ja migraatiot:
  - `app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt`
  - `app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/`
- Repository-kerros:
  - `app/src/main/java/com/finnvek/knittools/repository/`
- Asetukset ja pysyvä sovellusstate:
  - `app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/settings/SettingsViewModel.kt`
- AI:
  - `app/src/main/java/com/finnvek/knittools/ai/`
- Ravelry-auth ja API:
  - `app/src/main/java/com/finnvek/knittools/auth/RavelryAuthManager.kt`
  - `app/src/main/java/com/finnvek/knittools/repository/RavelryRepository.kt`
  - `app/src/main/java/com/finnvek/knittools/di/NetworkModule.kt`
- Pro / billing / trial:
  - `app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt`
  - `app/src/main/java/com/finnvek/knittools/pro/`
- Widgetit:
  - `app/src/main/java/com/finnvek/knittools/widget/`

## Modulit Ja Build

### `:app`

Päätuote. Sisältää:

- kaikki Compose-screenit ja navigaation
- Room- ja DataStore-kerrokset
- Ravelry-integraation
- AI- ja voice-flowt
- Play Billing / Pro / Trial -logiikan
- Glance-widgetin

`app/build.gradle.kts` käyttää näitä plugineja:

- `com.android.application`
- `org.jetbrains.kotlin.plugin.compose`
- `com.google.devtools.ksp`
- `com.google.dagger.hilt.android`
- `androidx.baselineprofile`
- `org.jlleitschuh.gradle.ktlint`
- `dev.detekt`
- `org.jetbrains.kotlin.plugin.serialization`
- `com.google.gms.google-services`

Huomio buildistä:

- `org.jetbrains.kotlin.android`-pluginia ei käytetä
- release signing on täysin ympäristömuuttujapohjainen
- release build estetään, jos signing-muuttujat puuttuvat
- release build estetään myös ilman Ravelry-credentialeja tai erillistä opt-in-lippua `KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS=true`

### `:baselineprofile`

Erillinen Android Test -moduuli, joka kohdistuu `:app`-moduuliin.

- namespace: `com.finnvek.knittools.baselineprofile`
- käyttää `androidx.benchmark.macro.junit4` + `uiautomator`
- `baselineProfile { useConnectedDevices = true }`

## Käynnistys Ja Runtime

Nykyinen käynnistyslogiikka:

1. `App.onCreate()`
   - kutsuu `PreferencesManager.applyStoredAppLanguage()`
   - initialisoi `BillingManager`in
   - initialisoi `ProManager`in
2. `MainActivity.onCreate()`
   - kutsuu `installSplashScreen()` ennen `super.onCreate()`
   - lukee mahdollisen widget-/intent-pohjaisen `CounterLaunchRequest`in
   - käsittelee mahdollisen Ravelry OAuth callbackin
   - ottaa `edgeToEdge`-tilan käyttöön
   - käynnistää In-App Update -tarkistuksen
   - lukee teema-asetukset `SettingsViewModel`in kautta
   - renderöi `KnitToolsNavHost`in
3. `MainActivity` lisäksi:
   - näyttää snackbarin, kun joustava sovelluspäivitys on ladattu
   - pyytää In-App Review'n ehdollisesti
   - tarkistaa `onResume()`-vaiheessa, onko päivitys valmis asennettavaksi

## Navigaatio

`TopLevelDestination` `Screen.kt`:ssä on source of truth top-level-tabeille.

Nykyiset top-level-tabit:

1. `Projects`
2. `Library`
3. `Tools`
4. `Insights`
5. `Settings`

Niiden top-level route-arvot:

- `projects_tab`
- `library_tab`
- `tools_tab`
- `insights_tab`
- `settings_tab`

Top-level start routet:

- `Projects` -> `project_list`
- `Library` -> `library`
- `Tools` -> `tools`
- `Insights` -> `insights`
- `Settings` -> `settings`

Sovellus käynnistyy `Projects`-tabiin.

Bottom bar piilotetaan tällä hetkellä näillä routeilla:

- `pro_upgrade`
- `yarn_card_review`
- `yarn_card_detail/{cardId}`
- `pattern_viewer/{projectId}`
- `library_pattern_viewer/{savedPatternId}`
- `notes_editor/{projectId}`

ViewModel-scope nykyisessä nav hostissa:

- `CounterViewModel` scoped `Projects`-graafin tasolle
- `LibraryViewModel` scoped `Library`-graafin tasolle
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

## Nykyinen Screen-Inventaario

Tämä lista kuvaa toteutuksessa olevat screenitiedostot, ei suunnitelmia:

- `abbreviations/AbbreviationsScreen.kt`
- `caston/CastOnScreen.kt`
- `chartsymbols/ChartSymbolScreen.kt`
- `counter/CounterScreen.kt`
- `counter/PhotoGalleryScreen.kt`
- `gauge/GaugeScreen.kt`
- `home/HomeScreen.kt` (renderöidään `tools`-reitillä Tools-tabin aloitusnäkymänä)
- `increase/IncreaseDecreaseScreen.kt`
- `insights/InsightsScreen.kt`
- `library/LibraryScreen.kt`
- `library/SavedPatternsScreen.kt`
- `library/MyYarnScreen.kt`
- `library/AllPhotosScreen.kt`
- `needles/NeedleSizeScreen.kt`
- `notes/NotesEditorScreen.kt`
- `pattern/PatternViewerScreen.kt`
- `pro/ProUpgradeScreen.kt`
- `project/ProjectListScreen.kt`
- `ravelry/RavelrySearchScreen.kt`
- `ravelry/RavelryDetailScreen.kt`
- `session/SessionHistoryScreen.kt`
- `settings/SettingsScreen.kt`
- `sizecharts/SizeChartScreen.kt`
- `yarn/YarnEstimatorScreen.kt`
- `yarncard/YarnCardReviewScreen.kt`

Lisäksi nykyinen pattern-/notes-/insights-/counter-flow sisältää erillisiä apukomponentteja ja viewmodel-tiedostoja samoissa alikansioissa.

## Pakettikartta

### Sovelluslogiikan pääpaketit

- `ai/`
  - cloud-puoli: `GeminiAiService`, `PatternInstructionGemini`, `PatternInstructionCombinerGemini`, `PatternTextExtractor`, `ProjectSummarizer`, `YarnLabelGeminiScanner`, `VoiceCommandInterpreter`
  - kiintiöt: `AiQuotaManager`
  - journal-flow: `ai/journal/`
  - live-voice: `ai/live/`
  - on-device parserit: `ai/nano/`
  - OCR: `ai/ocr/`
  - kevyt puheentunnistuswrapper: `ai/speech/`
- `auth/`
  - `RavelryAuthManager.kt`
- `billing/`
  - `BillingManager.kt`
- `data/datastore/`
  - `AppLanguage.kt`
  - `PreferencesManager.kt`
- `data/local/`
  - kaikki Room entityt, DAO:t ja `KnitToolsDatabase`
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
  - laskentalogiikat kuten gauge, cast-on, shaping, counter, reminder
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

## Data Ja Pysyvä Tila

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
- käsinkirjoitetut migraatiot: `3 -> 4`, `4 -> 5`, `5 -> 6`, `6 -> 7`
- skeema exportataan hakemistoon `app/schemas/.../7.json`

Schema-tason näkyvät laajennukset versiosta `7` eteenpäin:

- `counter_projects` sisältää nyt pattern-PDF:ään liittyviä kenttiä
- `counter_projects` sisältää stitch tracking -kenttiä
- `project_counters` sisältää repeating/shaping-kenttiä
- `pattern_annotations` on oma taulunsa

### DataStore

`PreferencesManager` on nykyinen source of truth sovelluksen preferensseille, kuten:

- teema
- kieli
- listaus- ja UI-preferenssit
- voice / tip / review -tyyppiset asetukset

### Paikallinen tiedostodata

Tiedostotallennuksen näkyvät entrypointit:

- `PatternDocumentStorage`
- `ProgressPhotoStorage`
- `FileProvider` manifestissa + polut `res/xml/file_paths.xml`

## Kielet Ja Lokalisaatio

Manifest käyttää `android:localeConfig="@xml/locales_config"`-asetusta, ja käynnistyksessä `App` kutsuu `PreferencesManager.applyStoredAppLanguage()`.

Nykyinen tuettu kielilista `locales_config.xml`:n mukaan:

- englanti `en`
- suomi `fi`
- ruotsi `sv`
- saksa `de`
- ranska `fr`
- espanja `es`
- portugali `pt`
- italia `it`
- norja bokmål `nb`
- tanska `da`
- hollanti `nl`

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

Kielivalinta näkyy myös Settings-flow’ssa `AppLanguage`-enumina ja `SettingsScreen`in kielivalitsimena.

## Integraatiot

### Ravelry

Nykyinen toteutus käyttää:

- OAuth2 Authorization Code -flow'ta
- Chrome Custom Tabia autentikointiin
- `EncryptedSharedPreferences`-tallennusta access- ja refresh-tokenille
- Ktor-pohjaista HTTP-clientiä

Ravelry BuildConfig -kentät:

- `RAVELRY_BASIC_AUTH_USER`
- `RAVELRY_BASIC_AUTH_PASSWORD`
- `RAVELRY_OAUTH2_CLIENT_ID`
- `RAVELRY_OAUTH2_CLIENT_SECRET`

Debug build lukee ne `local.properties`:sta.
Release build lukee ne ympäristömuuttujista.

### Pro / Billing / Trial

Nykyinen Play Billing -tuote:

- `BillingManager.PRODUCT_ID = "knittools_pro"`

`ProState`:

- statusvaihtoehdot: `TRIAL_ACTIVE`, `TRIAL_EXPIRED`, `PRO_PURCHASED`
- `isPro` on tosi, jos tila on ostettu tai trial aktiivinen
- `hasFeature(feature)` palauttaa tällä hetkellä käytännössä saman kuin `isPro`

`ProFeature`-enumissa on jo featurekohtaiset nimet, mm.:

- `WIDGET`
- `VOICE_COMMANDS`
- `VOICE_LIVE`
- `AI_FEATURES`
- `PATTERN_CAMERA_SCAN`
- `INSIGHTS_CHARTS`

### AI

AI-pinta ei ole yksi ominaisuus vaan useita erillisiä polkuja:

- cloud-pohjaiset Gemini-kutsut `ai/`-paketissa
- journal- ja note-append-flow `ai/journal/`
- live-voice quota/session -toteutus `ai/live/`
- on-device parsing ja saatavuustarkistukset `ai/nano/`
- OCR ja skannaus `ai/ocr/`
- kevyt raakapuheentunnistus `ai/speech/SimpleSpeechRecognizer.kt`

### Voice: kaksi erillistä polkua

KnitTools käyttää kahta toisistaan riippumatonta ääniputkea. Ne elävät rinnakkain, gate eri `ProFeature`-lipulla, eivätkä jaa audio-tilaa:

1. Klassinen keyword-flow (Pro-gate: `VOICE_COMMANDS`)
   - `ui/screens/counter/VoiceCommandHandler.kt` — `SpeechRecognizer`-pohjainen, continuous + one-shot, sealed `VoiceCommand` (mukaan lukien count-variantit)
   - `ai/VoiceCommandInterpreter.kt` — Gemini-fallback kun paikallinen parseri ei löydä osumaa
   - `ui/screens/counter/VoiceResponseManager.kt` — Android `TextToSpeech`-wrapper (`UtteranceProgressListener`, locale-fallback englantiin), koordinoi `VoiceCommandHandler`in kanssa estääkseen mic-palautesilmukan (TTS puhuu → SR paussilla)
   - tunnistusjärjestys: exact keyword → counted command (paikallinen EN+FI lukuparseri 1–20) → first-word fallback → Gemini
   - kiintiö: `AiQuotaManager.hasVoiceQuota()` 50/pv

2. Gemini Live API -keskustelu (Pro-gate: `VOICE_LIVE`)
   - `ai/live/VoiceLiveSession.kt` — Firebase AI `LiveSession`, `@OptIn(PublicPreviewAPI::class)`, `@Singleton`
   - `LiveGenerationConfig` käyttää `ResponseModality.AUDIO`:a ja `SpeechConfig(voice = Voice(...))`:a
   - `session.startAudioConversation(handler)` hoitaa mic-kaappauksen ja äänentoiston itse → Android TTS:ää **ei** tässä putkessa käytetä
   - `LiveVoiceState`: `IDLE`, `CONNECTING`, `ACTIVE`, `ERROR`; timeout-job sulkee session hiljaisuuden jälkeen
   - `ai/live/VoiceFunctionDeclarations.kt` määrittelee function-call-työkalut, `ai/live/ProjectVoiceContext.kt` rakentaa projektikohtaisen system-kontekstin
   - kiintiö: `ai/live/VoiceLiveQuotaManager.kt` (erillinen laskuri klassisesta)

Wiring: `CounterViewModel` ja `CounterScreen` kutsuvat kumpaakin putkea; molemmat reagoivat `ProState.hasFeature(...)`-gateen ja kumpikaan ei ole käytettävissä trial-tilan ulkopuolella ilman Pro-statusta.

## Widgetit

Nykyinen Glance-widget:

- on edelleen mukana tuotantokoodissa
- on Pro-gatettu (`CounterWidget` tarkistaa `proManager().isPro()`)
- osaa avata sovelluksen suoraan counteriin
- medium-koossa näyttää `+` / `-` -toiminnot broadcast-actioneina
- käyttää `CounterWidgetState`-tilaa ensimmäisen renderöinnin bootstrapissa

Manifestin näkyvät komponentit:

- `MainActivity` on exported
- `CounterWidgetReceiver` on exported + `BIND_APPWIDGET`
- `CounterWidgetActions` on non-exported
- `FileProvider` on non-exported

## Security Ja Platform Surface

Manifestin nykyinen pinta:

- permissions:
  - `INTERNET`
  - `VIBRATE`
  - `CAMERA`
  - `RECORD_AUDIO`
- `usesCleartextTraffic="false"`
- `android:allowBackup="true"`
- kamera-feature on merkitty `required="false"`
- OAuth callback tulee custom-schemen kautta:
  - `com.finnvek.knittools://oauth/callback`

Release- ja credential-muistiinpanot:

- release signing on ympäristömuuttujapohjainen
- Ravelry-secrets voidaan upottaa release BuildConfigiin vain eksplisiittisellä opt-in-lipulla
- repo sisältää `google-services`-pluginin ja `app/google-services.json`-tiedoston

## Testit Ja Verifiointi

Nykyiset testit painottuvat näihin alueisiin:

- domain calculators
- AI-parserit ja Gemini-wrapperit
- `ProjectCounterRepository`
- `ProState` ja `TrialManager`
- useat screen/viewmodel-testit `ui/`-puolella
- Android migration test `app/src/androidTest/java/com/finnvek/knittools/data/local/MigrationTest.kt`

Pienimmät hyödylliset tarkistuskomennot:

- `./gradlew assembleDebug`
- `./gradlew test`
- `./gradlew :app:detekt`
- `./gradlew lint`
- `./gradlew :app:generateBaselineProfile`

Älä käytä repo-ohjeiden mukaan käyttäjän wrapper-skriptejä `lint-check` tai `security-check`.

## Ominaisuudet (landing page -lähde)

Kokonaiskuva käyttäjälle näkyvistä ominaisuuksista. `ProFeature`-merkintä tarkoittaa gatettua Pro-ominaisuutta.

### Projektit ja laskuri

- Useampia neulontaprojekteja (rajaton, `UNLIMITED_PROJECTS`)
- Rivilaskuri suurella 110sp-näytöllä
- Silmukkalaskuri rinnalla (stitch tracking)
- Useita laskureita per projekti (`MULTIPLE_COUNTERS`): silmukkalaskurit, muotoilu- ja toistojaksot
- Muotoilulaskuri (`SHAPING_COUNTER`): automaattiset lisäykset/vähennykset säännöllisin välein
- Toistosektiot (`REPEAT_SECTION`): säkenaikainen sijainti kuviojaksossa
- Sekundaarilaskuri (`SECONDARY_COUNTER`): kaksi samanaikaista laskuria
- Rivi-muistutukset (`ROW_REMINDERS`): merkitse muistutettava rivi ja viesti
- Edistymiskuvat projektia kohti (`PROGRESS_PHOTOS`)
- Projektikohtaiset muistiinpanot (`NOTES`) + bottom sheet + full-screen editori
- Istuntohistoria ja täysi historia (`FULL_HISTORY`)
- Ravelry-kuvion liittäminen projektiin, PDF-kuviokatselija ja riviannotaatiot

### Kirjasto (Library)

- Tallennetut kuviot (Ravelryltä tai ladatut PDF-kuviot)
- Oma lankavarasto (`UNLIMITED_YARN`) — lankakortit (merkki, väri, koostumus, mittasuhteet)
- Lankalipun skannaus OCR:llä (`OCR`) — ML Kit Text Recognition + `YarnLabelParser`
- Kaikki valokuvat -galleria projektien yli (`AllPhotosScreen`)
- Multi-select + batch-poisto (kuviot, langat, kuvat)

### Työkalut (Tools)

- Tiheyslaskuri (Gauge)
- Kavennus- ja lisäyslaskuri (Increase/Decrease) — tasainen ja pyöreä
- Loissilmukkalaskuri (Cast-on)
- Langanmenekkiarvio (Yarn Estimator)
- Ravelry-haku ja kuvion tarkastelu
- Kuvion kamera-skannaus PDF:ksi (`PATTERN_CAMERA_SCAN`)
- Viiteopas: neulakoot (`NeedleSizeScreen`), kokotaulukot (`SizeChartScreen`), neulontalyhenteet (`AbbreviationsScreen`), kuvionmerkit (`ChartSymbolScreen`)

### Insights

- Neulonta-aika yhteensä ja projekteittain
- Rivejä per tunti (pace)
- Viikonpäivä-heatmap (`INSIGHTS_CHARTS`)
- Putkimittari/streak (`STREAK`)
- Pace ajan kuluessa -kaavio (`INSIGHTS_CHARTS`)

### AI ja ääni

- Ääniohjaus laskurille (`VOICE_COMMANDS`): klassinen SpeechRecognizer + Gemini-fallback, EN+FI lukuparseri 1–100, jakaa AI-kiintiön 500/kk (`AiQuotaManager.MONTHLY_ALLOWANCE`)
- Live API -äänikeskustelu (`VOICE_LIVE`): Firebase AI LiveSession, function-calling projektikontekstilla, erillinen kiintiölaskuri (`VoiceLiveQuotaManager`)
- AI-päiväkirjamerkinnät muistiinpanoihin (`AI_FEATURES`): puhu tai kirjoita, Gemini siivoaa → aikaleimatty merkintä
- Gemini Nano -kuvio-ohjeiden jäsennys (`GEMINI_NANO`, on-device)
- TTS-vastaukset (`VoiceResponseManager`) — locale-fallback englantiin
- Offline-ystävälliset ääniflowt ja vaimennettu Live API -virheiden näyttö

### Ravelry

- OAuth2-autentikointi Chrome Custom Tabilla
- Kuvion haku ja selaus
- Kuvion tallennus Kirjastoon
- PDF- tai verkkoselainavaus suoraan sovelluksesta

### Widget

- Glance-pohjainen kotinäyttö-widget (`WIDGET`): + / − broadcastit, avaa sovelluksen aktiiviseen laskuriin

### Henkilökohtaistaminen ja käyttömukavuus

- Vaalea + tumma teema ("70s Craft Revival"), järjestelmä-seuraus
- 11 kieltä: `en`, `fi`, `sv`, `de`, `fr`, `es`, `pt`, `it`, `nb`, `da`, `nl`
- Splash-näyttö, In-App Review, In-App Update (flexible)
- Baseline Profile -optimoitu käynnistys
- Tietosuoja: kaikki data paikallisesti (Room + DataStore + FileProvider), ei pilvisynkronointia; vain AI-kutsut lähtevät laitteesta

### Monetisaatio

- Ilmainen trial-aika (`TrialManager`, `TRIAL_ACTIVE` → `TRIAL_EXPIRED`)
- Yksi kertaosto: Play Billing tuote `knittools_pro` (`BillingManager`)
- Trial- ja Pro-ominaisuuksien gating kaikille yllä mainituille `ProFeature`-merkityille kohdille

## Suhde Muihin Ohjedokumentteihin

- `CLAUDE.md`
  - käytä, kun tarvitaan product wordingia, UX-rakennetta tai visuaalista suuntaa
- `AGENTS.md`
  - tiiviit työskentely- ja turvallisuussäännöt agenteille
- `CODEX.md`
  - sama käytännössä Codexille, pidetään linjassa `AGENTS.md`:n kanssa

Kun tarvitset lopullisen teknisen totuuden, palaa aina suoraan koodiin.
