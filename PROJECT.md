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
- Integraatiot: Ravelry Firebase backend, Google Play Billing, In-App Review, In-App Update
- Debug-diagnostiikka: Sentry Android Core vain debug-luokkapolussa
- Paikalliset lisäominaisuudet: regex-pohjainen laskuriohjeparseri `domain/calculator/InstructionParser.kt`
- Lokalisaatio: `localeConfig` + useat `values-*`-hakemistot
- Room schema version: `17`
- `compileSdk` / `targetSdk` / `minSdk`: `36 / 36 / 29`, kaikki `gradle/libs.versions.toml`-avaimista `androidCompileSdk`, `androidTargetSdk` ja `androidMinSdk`
- `baselineprofile`-moduulin `compileSdk` / `targetSdk` / `minSdk`: samat version catalog -arvot `36 / 36 / 29`
- Java target: `17`
- Gradle wrapper: `9.4.1`
- AGP: `9.1.1`
- Kotlin Compose plugin: `2.3.21`
- KSP: `2.3.9`
- Compose BOM: `2026.05.01`
- Room: `2.8.4`
- Glance: `1.1.1`
- Ktor: `3.5.0`
- Billing: `8.3.0`
- Sentry Android Core: `8.43.1` debug-only
- AndroidX Browser: `1.10.0`
- Firebase BOM: `34.14.0`
- Google Services Gradle plugin: `4.4.4`
- Functions runtime: `nodejs22`
- `firebase-functions`: `7.2.5`
- `firebase-admin`: `13.10.0`
- TypeScript: `7.0.2`
- DeepSec: `2.1.2`
- versionCode / versionName: `1 / 1.0.0`

Source of truth:

- `app/build.gradle.kts`
- `baselineprofile/build.gradle.kts`
- `gradle/libs.versions.toml`
- `settings.gradle.kts`
- `functions/package.json`
- `.deepsec/package.json`

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
- päälaskurin domain ja näyttötekstit:
  - `app/src/main/java/com/finnvek/knittools/domain/model/CraftType.kt`
  - `app/src/main/java/com/finnvek/knittools/domain/model/MainCounterLabelType.kt`
  - `app/src/main/java/com/finnvek/knittools/domain/model/MainCounterChange.kt`
  - `app/src/main/java/com/finnvek/knittools/domain/model/ReadingLine.kt`
  - `app/src/main/java/com/finnvek/knittools/domain/calculator/CounterValueFormatter.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/components/MainCounterDisplayText.kt`
- projektityötila ja projektikortit:
  - `app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/components/CounterImageButton.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/components/CounterStepperButton.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterProjectContentCards.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreenDecisions.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterUiStateReducers.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/counter/YarnManagementSheet.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/counter/PatternAttachmentUriResolver.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternPickerSheet.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/components/ProjectCard.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/components/ProjectDetailsDialog.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/components/ProjectYarnTextField.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt`
- Library ja lankakortit:
  - `app/src/main/java/com/finnvek/knittools/ui/screens/library/MyYarnScreen.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/library/LibraryViewModel.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternDetailScreen.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/yarncard/YarnCardDetailScreen.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/yarncard/YarnCardViewModel.kt`
  - `app/src/main/java/com/finnvek/knittools/data/storage/YarnPhotoStorage.kt`
  - `app/src/main/java/com/finnvek/knittools/repository/ProjectYarnNoteRepository.kt`
- Ravelry ja saved-pattern metadata:
  - `app/src/main/java/com/finnvek/knittools/MainActivity.kt`
  - `app/src/main/java/com/finnvek/knittools/auth/RavelryAuthManager.kt`
  - `app/src/main/java/com/finnvek/knittools/auth/FirebaseAnonymousAuthGateway.kt`
  - `app/src/main/java/com/finnvek/knittools/data/remote/RavelryBackendClient.kt`
  - `app/src/main/java/com/finnvek/knittools/data/remote/RavelryBackendMappers.kt`
  - `app/src/main/java/com/finnvek/knittools/data/remote/RavelryApiService.kt`
  - `app/src/main/java/com/finnvek/knittools/ravelry/RavelryShareImportUrls.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelrySearchScreen.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryDetailScreen.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryAccountHeader.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryImportConfirmationSheet.kt`
  - `app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryExternalLinks.kt`
  - `app/src/main/java/com/finnvek/knittools/repository/RavelryRepository.kt`
  - `app/src/main/java/com/finnvek/knittools/repository/SavedPatternRepository.kt`
  - `functions/src/ravelry/`
- Pro / billing / trial:
  - `app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt`
  - `app/src/main/java/com/finnvek/knittools/pro/`
- release- ja security-surface -sopimustarkistukset:
  - `tools/release-surface.ps1`
  - `tools/release-surface-test.ps1`
  - `tools/rs.ps1`
  - `tools/rst.ps1`
- CI- ja scanner-konfiguraatio:
  - `.github/workflows/build.yml`
  - `.github/workflows/codeql.yml`
  - `.github/dependabot.yml`
  - `gradle/osv-scanner.toml`
  - `.deepsec/deepsec.config.ts`
  - `.deepsec/matchers/`
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
- root `sonar`-task lukee `sonar-project.properties`-tiedoston, mutta ei syötä Gradlen itse hallitsemia source/binary/test/library-propertyjä Sonar-pluginille; app-moduulille asetetaan erikseen `sonar.coverage.jacoco.xmlReportPaths`
- `sonar-project.properties` rajaa coverage-portin domain-, repository- ja parserilogiikkaan; coverage-exclusion-listalla ovat muun muassa `App.kt`, `MainActivity.kt`, debug/release-source-setien `SentryInit.kt`, `di`, `ui`, `widget`, `auth`, `billing`, `pro` ja `data/*`
- `tools/sonar.ps1` ajaa nykyisin vain `.\gradlew.bat sonar --console=plain`, ei `assembleDebug sonar`; siksi Sonar-skannaus ei ole sidottu Firebase artifact -buildin `app/google-services.json`-porttiin, vaikka Gradlen `sonar`-task ajaa JaCoCo-raportin ennen analyysiä
- `compileSdk`, `minSdk` ja `targetSdk` tulevat version catalogista myös `:app`- ja `:baselineprofile`-build-skripteissä; älä etsi näille kovakoodattuja numeroita moduulien `android {}` -lohkoista
- release signing on ympäristömuuttujapohjainen ja käyttää neljää `KNITTOOLS_*`-muuttujaa: `KNITTOOLS_KEYSTORE_PATH`, `KNITTOOLS_KEYSTORE_PASSWORD`, `KNITTOOLS_KEY_ALIAS` ja `KNITTOOLS_KEY_PASSWORD`
- `gradle.taskGraph.whenReady` pysäyttää release signing -portilla nämä app-release-artifactit, jos signing-muuttujia puuttuu: `:app:assembleRelease`, `:app:bundleRelease`, `:app:packageRelease`, `:app:packageReleaseBundle`, `:app:packageReleaseUniversalApk`, `:app:signReleaseBundle` ja `:app:publishRelease`
- Ravelryn vanha backenditön release-polku on superseded; Android ei enää määritä Ravelry credential `BuildConfig` -kenttiä, release opt-in -gatea, Basic Auth fallbackia eikä Ravelry token-storea
- Android Firebase release artifact -build vaatii ignored `app/google-services.json` -tiedoston paikallisesti tai CI:ssä `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64` -salaisuudesta generoidun tiedoston; `verifyGoogleServicesJson` käyttää `inputs.files(targetFile)`-inputia ja `GoogleServicesJsonTaskActions.verify(...)`-tarkistusta, joka pysäyttää `assembleRelease`- ja `bundleRelease`-taskit vasta taskin suorituksessa, jos tiedosto puuttuu. Debug artifact -build voi luoda ignored `app/src/debug/google-services.json` -placeholderin paikallista buildattavuutta varten, jos oikeaa configia ei ole.
- debug lukee Sentry DSN:n `KNITTOOLS_SENTRY_DSN`- tai `SENTRY_DSN`-ympäristömuuttujasta tai ignored `debug.credentials.properties` -tiedostosta avaimella `sentry.dsn`; Sentry on vain debug-luokkapolussa, automaattinen session/tracing/breadcrumb/screenshot/view-hierarchy/NDK-keruu on pois päältä, ja release-luokkapolun puhtaus tarkistetaan `tools\sentry.ps1`- sekä `tools\rs.ps1`-komennoilla

### `:baselineprofile`

Erillinen Android Test -moduuli:

- namespace: `com.finnvek.knittools.baselineprofile`
- target project: `:app`
- käyttää `androidx.benchmark.macro.junit4` + `uiautomator`
- `baselineProfile { useConnectedDevices = true }`

### `functions`

Ravelry Firebase -backend:

- Firebase Functions v2 TypeScript `functions/src`
- root `firebase.json` määrittää `functions`-sourcen ja `nodejs22` runtimen
- root `firestore.rules` kieltää client read/write -pääsyn `ravelryOAuthStates/{state}`- ja `ravelryTokens/{uid}`-kokoelmiin
- `functions/package.json` omistaa backendin versiontotuuden: `node` engine `22`, `firebase-functions 7.2.5`, `firebase-admin 13.10.0`, `typescript 7.0.2` ja `@types/node 26.1.1`; `firebase-functions-test` ei kuulu nykyiseen dependency-surfaceen
- `functions/src/config.ts` omistaa alueen `europe-west1`, kokoelmanimet ja Secret Manager -sidokset `RAVELRY_CLIENT_ID` / `RAVELRY_CLIENT_SECRET`
- `functions/src/ravelry/auth.ts` julkaisee `ravelryStartAuth`, `ravelryCallback`, `ravelryAuthStatus`, `ravelryDisconnect` ja `ravelryCurrentUser`
- `functions/src/ravelry/authCore.ts`, `oauthStateStore.ts`, `oauth2.ts`, `tokenStore.ts` ja `client.ts` omistavat backend-OAuth2-flow'n, PKCE state -tallennuksen, server-side token exchangen, token tallennuksen ja current-user API-kutsun
- `functions/src/ravelry/patternImport.ts`, `urlParsing.ts`, `client.ts` ja `sanitizedTypes.ts` omistavat backend-haun ja metadata-only importin: `ravelrySearchPatterns`, `ravelryImportPatternById` ja `ravelryImportPatternByUrl` palauttavat vain sallitut Ravelry ID/title/designer/thumbnail/canonical/original URL/availability/pagination -kentät eivätkä lataa pattern-PDF:iä
- `functions/package.json` sisältää npm `overrides` -lukitukset `form-data 4.0.6`, `js-yaml 5.2.0` ja `uuid 11.1.1`; nämä ovat osa nykyistä dependency-surfacea, eivät Android-riippuvuuksia
- Androidissa on Firebase Auth/Functions -riippuvuudet sekä `RavelryBackendClient` callable-rajalle; `RavelryAuthManager` omistaa backend-auth-statuksen, start/disconnect-kutsut ja token-free `knittools://ravelry-auth-complete` callbackin; auth avataan Auth Tabilla ja Custom Tabs jää fallbackiksi
- Saved patterns siirrettiin Room schema 14:ssa lähdemetadataksi: `source`, nullable `ravelryPatternId`, `originalUrl`, `canonicalUrl`, nullable `localPdfUri`, `isAvailableOffline`, `updatedAt` ja nullable `lastSyncedAt`; tietokannan nykyversio on 17
- Phase 8 UI-polku on valmis: `RavelryImportConfirmationSheet` hoitaa hakutulos- ja jaetun URL:n import-vahvistuksen, `SavedPatternDetailScreen` hoitaa metadata-availabilityn ja toiminnot, PatternPickerSheet listaa kaikki saved patternit, ja projektin pattern-kortti avaa metadata-only linkit detailiin ilman PDF-vieweriä
- Phase 9 release-surface -vahti sallii vain Firebase Auth/Functions/Google Services -pinnan tälle backendille, kieltää Firebase AI/ML Kit/Gemini/voice-riippuvuudet, estää trackatut `app/google-services.json`- ja `app/src/debug/google-services.json` -tiedostot ja skannaa tunnetut paikalliset/env Ravelry-secret-arvot tulostamatta niitä
- `firebase-admin` on lukittu uusimpaan `firebase-functions@7.2.5`:n peer dependencyyn sopivaan 13.x-versioon, ei suunnitelman yhteensopimattomaan 14.x-versioon
- Release artifact -build ei ole konfiguroitavissa tyhjästä checkoutista ilman oikeaa Firebase-konfigia: `:app:assembleRelease` ja `:app:bundleRelease` riippuvat `verifyGoogleServicesJson`-tehtävästä, joka vaatii ignored `app/google-services.json` -tiedoston. Lisäksi release signing -portti kattaa alemman tason release package/sign/publish -taskit, vaikka Firebase-configin taskiriippuvuus on nykykoodissa sidottu vain `assembleRelease`- ja `bundleRelease`-nimiin. Debug artifact -build voi käyttää ignored placeholderia paikalliseen käännökseen.

## Käynnistys ja runtime

Nykyinen käynnistyslogiikka:

1. `App.onCreate()`
   - alustaa source-set-kohtaisen `SentryInit`-polun; debug voi käyttää Sentryä, release on no-op
   - käynnistää `PreferencesManager.applyStoredAppLanguage()`-kutsun `@ApplicationScope`ssa ilman `runBlocking`ia
   - käynnistää `YarnCardRepository.pruneUnreferencedPhotoFiles()`-siivouksen, joka vertaa `yarn_photos`-tiedostoja Roomin lankakorttien `photoUri`-viitteisiin
   - käynnistää `PatternDocumentStorage.pruneStaleCaptureImages(...)`-siivouksen injektoidulla `@IoDispatcher`illa
   - `BillingManager.initialize()`
   - `ProManager.initialize()`
   - odottaa `ProManager.initialStateReady`-tilaa ja päivittää kaikki widgetit aina, kun `ProFeature.WIDGET`-käyttöoikeus muuttuu
2. `MainActivity.onCreate()`
   - `installSplashScreen()` ennen `super.onCreate()`
   - lukee mahdollisen `CounterLaunchRequest`in intentistä
   - käsittelee mahdollisen Ravelry OAuth callbackin
   - käynnistää In-App Update -tarkistuksen
   - lukee teeman suoraan `PreferencesManager.preferences`-flow'sta
   - pitää splashin näkyvissä kunnes startup-teema on ratkaistu
   - ottaa `enableEdgeToEdge()`-tilan käyttöön vasta kun light/dark-teema tiedetään
   - käärii Composen `ProvidePreferenceAwareHapticFeedback(...)`-provideriin, jotta DataStoren `hapticFeedback`-asetus koskee myös komponenttien oletushaptiikkaa kuten `combinedClickable`-pitkäpainalluksia
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

Navigation callback -rajat:

- `KnitToolsNavHost` ottaa ulkoiset sivuvaikutuscallbackit yhtenä `KnitToolsNavActions`-mallina; `MainActivity` syöttää sen kautta Pro-oston, Ravelry-authin, Browse Ravelry -avaamisen, counter-launchin kuittauksen ja share-importin kuittauksen
- Projects-graafin counter-reitti välittää `CounterScreen`ille `CounterScreenActions`-mallin, jotta counter-screenin route-toiminnot pysyvät ryhmiteltyinä eikä composable-parametrilista kasva erillisillä callbackeilla
- Ravelry-reitit välittävät `RavelrySearchScreen`ille `RavelrySearchActions`-mallin; siihen kuuluvat pattern detail -avaus, back, auth launch, Browse Ravelry ja saved-pattern detail -avaus
- `Screen.RavelryImport(url)` enkoodaa URL:n `Uri.encode(...)`-kutsulla ja `Screen.RavelryImport.importUrl(...)` dekoodaa route-argumentin; raakaa Ravelry URL:ää ei kuljeteta route-segmentissä

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
- `ravelry_import/{importUrl}`
- `ravelry_detail/{patternId}`

### Library-graafi

- `library`
- `saved_patterns`
- `saved_pattern_detail/{savedPatternId}`
- `library_pattern_viewer/{savedPatternId}`
- `library_ravelry_detail/{patternId}`
- `my_yarn`
- `yarn_card_detail/{cardId}`
- `all_photos`
- referenssireitit:
  - `needles`
  - `size_charts`
  - `abbreviations?craftType={craftType}`
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
- `library/SavedPatternDetailScreen.kt`
- `library/SavedPatternsScreen.kt`
- `needles/NeedleSizeScreen.kt`
- `notes/NotesEditorScreen.kt`
- `pattern/PatternViewerScreen.kt`
- `pro/ProUpgradeScreen.kt`
- `project/ProjectListScreen.kt`
- `ravelry/RavelryDetailScreen.kt`
- `ravelry/RavelryImportConfirmationSheet.kt`
- `ravelry/RavelrySearchScreen.kt`
- `session/SessionHistoryScreen.kt`
- `settings/SettingsScreen.kt`
- `sizecharts/SizeChartScreen.kt`
- `yarn/YarnEstimatorScreen.kt`
- `yarncard/YarnCardDetailScreen.kt`

Huomio:

- `LibraryPatternViewerScreen` elää samassa tiedostossa kuin `PatternViewerScreen`
- `HomeScreen` on käytännössä Tools-tabin aloitusnäkymä
- `RavelryAccountHeader`, `PatternCard` ja `RavelryExternalLinks` eivät ole itsenäisiä screen-reittejä, mutta ne ovat nykyisen Ravelry UI -pinnan keskeisiä komponentteja

## Pakettikartta

### Sovelluslogiikan pääpaketit

- `auth/`
  - `RavelryAuthManager.kt`
  - `FirebaseAnonymousAuthGateway.kt`
  - `FirebaseTaskAwait.kt`
- `billing/`
  - `BillingManager.kt`
- `data/datastore/`
  - `AppLanguage.kt`
  - `PreferencesManager.kt`
- `data/local/`
  - Room entityt, DAO:t ja `KnitToolsDatabase`
- `data/remote/`
  - `RavelryApiService.kt`
  - `RavelryBackendClient.kt` sisältää sekä `RavelryBackendClient`-rajapinnan että Firebase Functions -toteutuksen `FirebaseRavelryBackendClient`
  - `RavelryBackendMappers.kt`
  - `RavelryModels.kt`
- `data/storage/`
  - `AppFileStorage.kt`
  - `CounterLaunchTokenStore.kt`
  - `PatternDocumentStorage.kt`
  - `PdfPageRenderer.kt`
  - `ProgressPhotoStorage.kt`
  - `StorageFileNames.kt`
  - `YarnPhotoStorage.kt`
- `di/`
  - `DatabaseModule.kt`
  - `DispatchersModule.kt`
  - `FirebaseModule.kt`
  - `NetworkModule.kt`
- `domain/calculator/`
  - laskenta- ja paikalliset parserilogiikat, mukaan lukien regex-pohjainen `InstructionParser`, `CounterValueFormatter` ja projektikohtaisen lisälaskurin domain-logiikka
- `domain/model/`
  - domain-mallit, mukaan lukien `CraftType`, `MainCounterLabelType`, `MainCounterChange`, `ReadingLine`, `ProjectCounterType`, `ProjectSortOrder` ja `YarnCardLinks`
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
  - `ProjectYarnNoteRepository.kt`
  - `RavelryRepository.kt`
  - `ReminderRepository.kt`
  - `SavedPatternRepository.kt`
  - `YarnCardRepository.kt`
- `ravelry/`
  - `RavelryShareImportUrls.kt`
- `ui/`
  - `navigation/` omistaa route-mallit, top-level-tabien source of truthin ja route-argumenttien fallbackit
  - `screens/counter/` omistaa counterin työtilan, content cardit, yarn management -sheetin, pattern picker -entryn, projektitoiminnot, laskuriosiot, pienet counter-päätöshelperit ja `CounterUiState`-reducerit
  - `screens/library/` omistaa Library-hubin, My Yarn -listan, saved patterns -listan, saved pattern detailin ja all photos -listan
  - `screens/ravelry/` omistaa Ravelry search/detail -näkymät, account headerin, import-vahvistuksen, pattern-kortit ja ulkoisen Ravelry-linkin avaushelperin
  - `screens/yarncard/` omistaa lankakortin detailin, manuaalisen input-mallin ja detail-editoinnin
  - `components/` sisältää jaettuja UI-rakennuspalikoita, kuten `ProjectCard`, `HubListItem`, dialogit, inputit ja tooltipit
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
- käsinkirjoitetut migraatiot: `3 -> 4`, `4 -> 5`, `5 -> 6`, `6 -> 7`, `7 -> 8`, `8 -> 9`, `9 -> 10`, `10 -> 11`, `11 -> 12`, `12 -> 13`, `13 -> 14`, `14 -> 15`, `15 -> 16`
- schema exportataan hakemistoon `app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/`, jossa uusin export on `16.json`

Näkyvä uusin lisäys:

- `sessions.durationSeconds` ja `sessions.rowsWorked` lisättiin migraatiossa `9 -> 10`; vanhat rivit backfillataan `durationMinutes * 60` ja positiivisella `endRow - startRow` -arvolla
- `sessions(endedAt, startedAt)` ja `sessions(projectId, endedAt, startedAt)` -indeksit lisättiin migraatiossa `10 -> 11`
- `project_yarn_notes` lisättiin migraatiossa `11 -> 12`
- `counter_projects.craftType`, `counter_projects.mainCounterLabelType`, `counter_projects.mainCounterCustomLabel`, `counter_projects.readingLineEnabled`, `counter_projects.readingLineYFraction` ja `project_counters.linkedToMainCounter` lisättiin migraatiossa `12 -> 13`
- `saved_patterns` uudelleenmuotoiltiin migraatiossa `13 -> 14`: vanhat `ravelryId` / `patternUrl` -sentinelit muunnetaan lähdemetadataksi `source`, `ravelryPatternId`, `originalUrl`, `canonicalUrl`, `localPdfUri`, `isAvailableOffline`, `updatedAt` ja `lastSyncedAt`
- schema 14 lisää saved-pattern-hakuihin indeksit `ravelryPatternId`, `canonicalUrl`, `originalUrl` ja `localPdfUri`
- migraatio `14 -> 15` lisää indeksit `counter_projects.linkedPatternId`- ja `yarn_cards.linkedProjectId`-kentille; se ei muuta domain-mallia
- migraatio `15 -> 16` lisää nullable `sessions.zoneId` -kentän; vanhat sessiot jäävät tarkoituksella ilman aikavyöhykettä
- `sessions.startedAt`-indeksi on migraatiossa `8 -> 9`
- `counter_projects.targetRows` on migraatiossa `7 -> 8`

Session-laskennan nykyrajat:

- `KnitSession` ja `SessionEntity` kantavat display-minuutit, tarkat `durationSeconds` / `rowsWorked` -kentät ja nullable `zoneId`-arvon
- `SessionDao.getTotalMinutes(...)` summaa `durationSeconds`-kentän ja pyöristää ylöspäin minuutteihin
- uusi sessio tallentaa session alussa `ZoneId.systemDefault().id`-arvon `CounterViewModel`in `sessionZoneId`-tilaan; sama arvo säilyy `SavedStateHandle`ssa recreationin yli
- Insights käyttää `SessionMetrics`-apuria, joka jakaa cross-midnight-sessiot session omassa aikavyöhykkeessä päivä- ja pace-bucketeihin; legacy-null tai virheellinen `zoneId` putoaa nykyiseen laitteen aikavyöhykkeeseen
- range-rajaus käyttää pyydetyn analyysivyöhykkeen päivänalkua, mutta session sisäinen päivä-/bucket-jako käyttää `KnitSession.analyticsZoneOr(...)`-ratkaisua

### Repository-, transaktio- ja coroutine-rajat

Source of truth:

- `data/local/DatabaseTransactionRunner.kt`
- `di/DispatchersModule.kt`
- `repository/RepositoryFlowResilience.kt`
- repository-luokat `repository/`-hakemistossa

Nykyiset invariantit:

- usean DAO:n kirjoitukset kulkevat `DatabaseTransactionRunner.run { ... }` -rajasta, jonka tuotantototeutus käyttää `RoomDatabase.withTransaction`
- UI ei saa pilkkoa pattern attach/detach-, yarn/project-linkki- tai päälaskurin count/history/linked-counter -kirjoituksia erillisiksi Room-operaatioiksi
- UI:lle nousevat Room-havaintovirrat käyttävät DAO->domain-mäppäyksen jälkeen `retryOnRepositoryReadFailure()`-rajaa `CounterRepository`ssa, `ProjectCounterRepository`ssa, `ProjectYarnNoteRepository`ssa, `ReminderRepository`ssa, `ProgressPhotoRepository`ssa, `PatternAnnotationRepository`ssa, `SavedPatternRepository`ssa ja `YarnCardRepository`ssa
- retry ei niele `CancellationException`ia eikä ei-`Exception`-tyyppisiä vakavia virheitä; tavallinen luku- tai mapping-poikkeus käynnistää virran uudelleen viiveillä `250`, `500`, `1000`, `2000`, `4000`, jonka jälkeen viive on rajattu `5000` millisekuntiin
- IO-dispatcher tulee Hiltin `@IoDispatcher`-qualifierilla; repositoryjen ja ViewModelien ei pidä kovakoodata `Dispatchers.IO`:ta
- prosessin eliniän työ käyttää Hiltin `@ApplicationScope`-scopea (`SupervisorJob + Dispatchers.Main.immediate`); blokkaava työ valitsee silti erikseen injektoidun IO-dispatcherin
- `CounterViewModel.onCleared()` käyttää application-scopea vain viimeiseen sessiosnapshotin tallennukseen, ja `NotesEditorViewModel.onCleared()` vain viimeiseen debounce-editorin poistumistallennukseen; normaalit ruututyöt pysyvät `viewModelScope`ssa

### Päälaskurin ja lisälaskureiden domain

Nykyinen päälaskuri ei ole enää pelkkä rivilaskuri.

Source of truth:

- `domain/model/CraftType.kt`
- `domain/model/MainCounterLabelType.kt`
- `domain/model/MainCounterChange.kt`
- `domain/model/ProjectCounterType.kt`
- `domain/calculator/CounterValueFormatter.kt`
- `repository/CounterRepository.kt`
- `ui/components/MainCounterDisplayText.kt`

Nykyiset rajat:

- projektin craft type on `KNITTING` tai `CROCHET`; vanhat ja tuntemattomat persisted-arvot putoavat `KNITTING`-tilaan
- oletuspäälaskurin nimi tulee craft typestä: neulonta käyttää `ROWS`, virkkaus käyttää `ROUNDS`
- päälaskurin label voi olla `ROWS`, `ROUNDS`, `REPEATS` tai `CUSTOM`
- custom-label trimmataan, tyhjä arvo hylätään ja maksimipituus on `32` merkkiä `MainCounterLabelType.kt`:ssa
- `CounterValueFormatter` muodostaa päälaskurin hero-labelin, target-tekstin, plus/miinus-content descriptionit, project card -count-tekstin sekä lisälaskureiden repeat/shaping/reminder-displayn
- kaikki päälaskurimuutokset kulkevat `CounterRepository.applyMainCounterChange(...)` -metodin kautta, myös widgetin `applyWidgetCountChange(...)`
- repository tekee countin, historyn, current-stitch-resetin ja linkitettyjen lisälaskureiden delta-päivitykset samassa `DatabaseTransactionRunner`-transaktiossa
- `ProjectCounter.linkedToMainCounter` saa lisälaskurin seuraamaan päälaskurin todellista muutosta; decrement ja reset käyttävät toteutunutta deltaa eivätkä voi pudottaa laskuria alle nollan
- repeat-section counterit eivät saa olla linked-to-main, koska niiden eteneminen johdetaan jo päälaskurin riveistä

### DataStore

`PreferencesManager` on source of truth ainakin näille:

- teema (`ThemeMode`)
- appin kieli (`AppLanguage`)
- haptic feedback
- keep screen awake
- metriikka/imperial
- completed projects -näyttö
- project sort order
- dismissed one-shot tooltipit

Lisäksi käytössä on erillisiä DataStoreja:

- `trial_state`
- `review_state`
- `counter_widget`

DataStore- ja token-rajat:

- `PreferencesDataStoreSafety.kt` käsittelee vain `IOException`-virheet fallbackina; muut poikkeukset etenevät kutsujalle eikä ohjelmointivirheitä muuteta hiljaisiksi oletusarvoiksi
- `trial_state` sisältää trialin aloituksen, monotonisesti kasvavan last-known-timestampin ja pysyvän clock-tampered-lipun; yli tunnin kellon taaksepäin siirto lukitsee trialin, ja refresh rajataan normaalisti enintään 15 minuutin välein sekä päivänvaihtoon
- `review_state` säilyttää action countin ja sen, onko review jo pyydetty; eligibility-raja on 20 toimintoa ja requested merkitään ennen Play review flow'n käynnistämistä
- `counter_widget` on kaikkien Glance-instanssien jaettu aktiivisen projektin tila; widget ei sido eri projektia jokaiseen instanssiin
- widget-counter-launch-tokenit eivät ole DataStoressa vaan synkronoidussa SharedPreferences-storessa `counter_launch_tokens`; `CounterLaunchTokenStore` pitää enintään 100 tokenia, hyväksyy vain 24 tuntia voimassa olevan appin itse antaman tokenin ja käyttää synkronista `commit()`-kirjoitusta kertakäyttökulutuksen onnistumisen varmistamiseen

### Paikallinen tiedostodata

Entry pointit:

- `AppFileStorage`
- `PatternDocumentStorage`
- `PdfPageRenderer`
- `ProgressPhotoStorage`
- `YarnPhotoStorage`
- `FileProvider` + `res/xml/file_paths.xml`

Tallennuspolut nykykoodissa:

- pattern PDF:t tallennetaan appin sisäiseen `pattern_pdfs/<projectId>`-hakemistoon `file://`-URIlla
- ulkoiset pattern-PDF:t kopioidaan `PatternDocumentStorage.copyPdfToInternal(...)`-polulla appin sisäiseen `pattern_pdfs/<projectId>`-hakemistoon; jo app-owned pattern-URI säilytetään ilman uutta kopiota `resolvePatternAttachmentUri(...)`-päätöksellä
- jos ulkoisen pattern-PDF:n kopiointi epäonnistuu, `resolvePatternAttachmentUri(...)` palauttaa `null` eikä patternia liitetä projektiin
- pattern camera capture -kuvat luodaan `pattern_captures/<projectId>`-hakemistoon ja ne ovat FileProviderin kautta ulos annettava väliaikainen pattern-kuvapolku
- progress-kuvat tallennetaan `progress_photos/<projectId>`-hakemistoon
- yarn card -kuvat kopioidaan `YarnPhotoStorage.copyPhoto(...)`-metodilla appin sisäiseen `yarn_photos/<cardId>`-hakemistoon ja tallennetaan lankakortille app-owned `file://`-URIksi
- `file_paths.xml` exposeeraa vain `progress_photos` ja `pattern_captures`; `yarn_photos` ja `pattern_pdfs` eivät ole nykyisiä FileProvider-share-rootteja
- `AppFileStorage` tunnistaa silti sisäistä lukua/siivousta varten app-owned `file://`-URI:t sekä legacy FileProvider-rootit `yarn_photos`, `progress_photos`, `pattern_captures`, `pattern_pdfs` ja `patterns`
- lankakortin kuvan vaihto poistaa vanhan app-owned kuvan vain jos uuden kuvan URI tallentui onnistuneesti; epäonnistunut tallennus siivoaa vasta kopioidun uuden kuvan
- `App.onCreate()` käynnistää lankakuvien orphan-siivoamisen: vain Roomissa viitatut, ei-tyhjät `photoUri`-arvot säilytetään; keskeytyneen kuvanvaihdon jättämä tiedosto voidaan siis siivota seuraavassa prosessikäynnistyksessä
- `PdfPageRenderer` sulkee konstruktorissa avatun `ParcelFileDescriptor`in myös silloin, kun `PdfRenderer`-konstruktio epäonnistuu; `renderPage()` ja `close()` ovat synkronoituja, ja `close()` sulkee descriptorin `finally`-haarassa
- PDF-bitmapin kumpikin dimensio rajataan enintään `4096` pikseliin `calculatePdfRenderBitmapSize(...)`-apurissa; sivun aspect ratio säilytetään ja epäkelvot nolla-/negatiiviset mitat normalisoidaan vähintään yhteen pikseliin

### Pattern PDF, import ja reading line

Pattern viewer -tila jakautuu projektin pysyvään attached-PDF-polkuun ja libraryn session sisäiseen katselutilaan.

Source of truth:

- `ui/screens/pattern/PatternPickerSheet.kt`
- `ui/screens/pattern/PatternViewerScreen.kt`
- `ui/screens/counter/PatternAttachmentUriResolver.kt`
- `data/storage/PatternDocumentStorage.kt`
- `data/storage/PdfPageRenderer.kt`
- `domain/model/ReadingLine.kt`
- `domain/calculator/RowMappingParser.kt`
- `repository/CounterRepository.kt`
- `config/future-sync-spec.md`

Nykyiset rajat:

- pattern import käyttää Android Storage Access Frameworkia `OpenDocument(application/pdf)` -sopimuksella ja persistable read grantilla
- `Open device files` ja Drive/Dropbox-copy käyttävät nykykoodissa samaa SAF PDF -pickeriä; sovelluksessa ei ole Drive/Dropbox SDK:ta, OAuthia, provider-kohtaista token storagea eikä taustasynkkaa
- app-owned pattern PDF säilyy `pattern_pdfs/<projectId>`-polussa; ulkoinen PDF kopioidaan sisäiseen tallennukseen ennen attachia
- projektin attached-PDF:n reading line tallennetaan kenttiin `counter_projects.readingLineEnabled` ja `counter_projects.readingLineYFraction`
- attached-PDF:n riviankkurit tallennetaan `counter_projects.patternRowMapping`-kenttään serialisoituina `RowMarker(row,page,yPosition)` -arvoina
- projektin pattern viewerin overflow-toiminnot voivat tallentaa nykyisen reading line -kohdan nykyiseksi riviksi, poistaa nykyisen rivimerkin, poistaa sivun rivimerkit tai käynnistää kaksipistekalibroinnin; library viewer ei näytä näitä projektirivin persistointitoimintoja
- drag commit luo tai päivittää nykyisen row/page-ankkurin `CounterViewModel.upsertPatternRowMarker(...)` -polulla; live drag käyttää projektin viewerissä transienttia preview-tilaa ennen commitia
- reading line -overlay näyttää projektin viewerissä nykyisen rivin pienenä labelina viivan vieressä; library-only viewer näyttää vain paikallisen viivan ilman rivimerkintöjen persistointia
- kahden pisteen kalibrointi yhdistää ensimmäisen ja viimeisen ankkurin `mergePatternRowMarkers(...)` -polulla ilman skeemamuutosta
- riviliikkeen ratkaisu on `domain/calculator/resolveReadingLineYFraction(...)`-apurin vastuulla: täsmäankkuri voittaa, kaksi saman sivun ankkuria interpoloi, yksipuolinen ankkuri ei lukitse viivaa ja toisen sivun ankkurit eivät liikuta nykyistä sivua
- `sanitizeReadingLineYFraction(...)` rajoittaa äärellisen arvon välille `0.05f..0.95f`, ja oletusarvo on `0.5f`; funktio käyttää suoraan `Float.coerceIn(...)`-kutsua eikä tee erillistä `NaN`-normalisointia
- library-only pattern viewer käyttää `rememberSaveable(patternUri)` -tilaa sivulle ja reading linelle; se ei luo saved-pattern-skeemapolkua pelkän katselun takia
- jatkuva Drive/Dropbox-sync on tulevaa speksiä `config/future-sync-spec.md`:ssä, ei nykyominaisuus; ennen sitä pitää määritellä Pro-gate, konfliktit, offline-käytös, OAuth/token storage ja background sync

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

Nykyiset kieli- ja `values`-resurssihakemistot:

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

Nykyinen toteutus käyttää Firebase-backend-rajaa Ravelry API -pintaan. Tavoitearkkitehtuuri on superseded-päätöksen jälkeen `Ravelry Firebase Backend And Saved Patterns Plan.md` + `config/ravelry-backend-progress.md`: Firebase Auth anonymous UID ja Cloud Functions v2 omistavat Ravelry-secretit, token exchangen, API-kutsut, auth-statuksen, disconnectin, haun ja URL-importin.

Backendin OAuth2 start/callback/status/disconnect/current-user sekä metadata-only search/import -flow ovat olemassa `functions/`-hakemistossa. Androidissa on Firebase Auth/Functions -riippuvuudet, anonymous sign-in -gateway ja `RavelryBackendClient`, joka kutsuu backendin callableja. Tools > Ravelry auth-tila ja OAuth callback -flow ovat Phase 6:n jälkeen backendin omistamia: Android avaa vain backendin palauttaman authorize URL:n ja käsittelee token-free callbackin. Saved patterns ovat Phase 7:n jälkeen schema 14 -lähdemetadatassa, eivät persisted `ravelryId = 0` / `patternUrl` -sentineleissä. Phase 8:n jälkeen Ravelry UI ja saved-pattern attachment -polut käyttävät samaa metadataa ilman source-kategoriaotsikoita. Phase 9:n jälkeen release-surface-sopimus sallii vain Firebase Auth/Functions/Google Services -pinnan tälle backendille ja pitää vanhat Android Ravelry-secret-pinnat removed-risk-regressiona.

Nykyinen Android-koodi Phase 8:n jälkeen:

- `FirebaseAnonymousAuthGateway` varmistaa anonymous Firebase Auth -käyttäjän ennen callable-kutsuja
- `FirebaseRavelryBackendClient` kutsuu callableja `ravelryStartAuth`, `ravelryAuthStatus`, `ravelryDisconnect`, `ravelryCurrentUser`, `ravelrySearchPatterns`, `ravelryImportPatternById` ja `ravelryImportPatternByUrl`
- `RavelryApiService` on yhteensopivuusraja vanhalle repository/UI-kutsupinnalle ja delegoi search/detail-haut backend-clientille
- `RavelryBackendMappers` muuntaa backendin sanitisoidut pattern-metadata-vastaukset nykyisiin `PatternSearchResponse`- ja `PatternDetail`-malleihin
- `RavelryAuthManager` omistaa `RavelryAuthState`-tilan, backend `startAuth` / `authStatus` / `disconnect` -kutsut, pending state -tarkistuksen sekä cancelled/expired/backend-unavailable -tilat
- `MainActivity.launchRavelryAuth` avaa authin AndroidX Browser Auth Tabilla ja käyttää Custom Tabsia fallbackina; `MainActivity` käsittelee vain `knittools://ravelry-auth-complete?state=...` callbackin eikä token-arvoja kulje deep linkissä
- `MainActivity` vastaanottaa `ACTION_SEND text/plain` share-intentit, käyttää `RavelryShareImportUrls.extractPatternUrl(...)` -validointia ja reitittää hyväksytyt Ravelry pattern URL:t `RavelryShareImportRequest`in kautta `ravelry_import/{importUrl}`-reitille ilman counter-launch-sivuvaikutusta
- `RavelrySearchScreen` ja `RavelryDetailScreen` näyttävät state-aware `RavelryAccountHeader`-näkymän not-connected, connected-as-username, cancelled, expired, backend-unavailable ja disconnect-tiloille; connected-tilan Browse Ravelry avaa `https://www.ravelry.com/patterns/search` Custom Tabsilla ja share päällä
- `RavelrySearchScreen` pitää Search-välilehden näkyvissä myös kirjautumattomana, mutta hakukenttä, IME-search, retry ja pagination ovat käytössä vain, kun `authState is RavelryAuthState.Connected`; Saved Patterns -välilehti pysyy käytettävissä ilman connected-hakutilaa
- `RavelrySearchScreen` on jaettu ylläpidettävyyden takia apureihin `RavelrySearchField`, `ravelrySearchResults`, `ravelrySearchLoadingItem`, `ravelrySearchErrorItem` ja `ravelrySearchEmptyStateItem`; `shouldRequestRavelryLoadMore(...)` estää load-more-kutsut, jos haku ei ole connected, tuloksia ei ole, lataus on kesken, virhe on päällä tai näytössä ei ole nykyinen submitted search
- `PatternCard` ottaa nykyisin yhden `PatternCardState`-mallin (`name`, `designerName`, `thumbnailUrl`, `difficulty`, `isFree`) ja valinnaisen action-slotin; sama kortti palvelee Ravelry-hakutuloksia, import-vahvistusta, Ravelry Saved -välilehteä ja Libraryn Saved Patterns -listaa
- `RavelryImportConfirmationSheet` käsittelee hakutulos- ja share-URL-importin tilat loading/ready/already-saved/needs-sign-in/could-not-import/backend-unavailable; duplicate-polku avaa nykyisen `SavedPatternDetail`-rivin
- `SavedPatternDetailScreen` näyttää title/designer/thumbnail/availability-tiedot ja hoitaa Open Pattern / Open on Ravelry / Attach to Project / Remove -toiminnot
- `PatternPickerSheet` listaa kaikki saved patternit, kutsuu `CounterViewModel.attachSavedPattern`-polkua valinnassa ja tarjoaa erillisen Import from Ravelry -toiminnon; SAF PDF -liite säilyy `Attach PDF from device` -polkuna
- `SavedPatternSource` erottaa `RAVELRY`, `LOCAL_FILE` ja `OTHER` -lähteet; repositoryn duplikaattihaku tarkistaa Ravelry ID:n, canonical URL:n, normalisoidun original URL:n ja title+designer-vastaavuuden vain erikseen pyydettynä
- `CounterRepository.attachSavedPattern` kirjoittaa `linkedPatternId`in sekä mahdollisen `localPdfUri`n yhteen pattern-attachment-transaktioon; project pattern cards avaavat PDF-viewerin vain, kun `patternUri` on olemassa, ja metadata-only linkit avaavat `SavedPatternDetail`-reitin
- `RavelryExternalLinks.openRavelryUrl(...)` keskittää ulkoisen Ravelry-linkin avaamisen ja käsittelee `ActivityNotFoundException`-tapauksen Toast-fallbackilla
- `NetworkModule` tarjoaa yhä Ktor + OkHttp -pohjaisen HTTP-clientin, jossa `connectTimeout=15s`, `callTimeout=45s`, `read/writeTimeout=30s`; nykyinen Ravelry-polku käyttää kuitenkin Firebase callable -clientiä eikä tee suoria Ravelry HTTP -kutsuja Androidista
- callable-virheet mapataan nykyiseen Ravelry HTTP -poikkeusmalliin: 400, 401, 404, 412, 429, 503 tai 500
- Ravelry-hakupyynnön optional-parametrit rakennetaan `PatternSearchParams.toBackendData()`-apurilla: `query`, `page` ja `pageSize` lähetetään aina, mutta `craft`, `availability`, `pc`, `weight`, `difficultyFrom` ja `difficultyTo` lisätään vain, kun arvo on olemassa; nykyinen koodi ei käytä nullable-arvojen yleistä `.filterValues { it != null }` -siivousta
- `FirebaseModule` sitoo `FirebaseRavelryBackendClient`in `RavelryBackendClient`-rajapintaan `fun interface FirebaseBindingsModule` -moduulilla, ja `FirebaseFunctions` instansioidaan alueelle `europe-west1`
- backend julkaisee seitsemän callable-funktiota (`ravelryStartAuth`, `ravelryAuthStatus`, `ravelryDisconnect`, `ravelryCurrentUser`, `ravelrySearchPatterns`, `ravelryImportPatternById`, `ravelryImportPatternByUrl`) sekä yhden HTTP callbackin (`ravelryCallback`); exportit omistaa `functions/src/index.ts`
- `functions/src/config.ts` omistaa alueen `europe-west1`, Firestore-kokoelmat `ravelryOAuthStates`, `ravelryTokens` ja `ravelryRateLimits`, Secret Manager -sidokset `RAVELRY_CLIENT_ID` / `RAVELRY_CLIENT_SECRET`, parametrin `RAVELRY_CALLBACK_URL`, 10 minuutin OAuth state TTL:n ja app-redirectin `knittools://ravelry-auth-complete`
- callable-rajat vaativat Firebase Auth UID:n; `functions/src/ravelry/callable.ts` muuntaa backendin 400/401/404/429/5xx-virheet Firebase Functions -virheiksi ja käyttää muissa sopimusvirheissä `failed-precondition`/`internal`-koodeja
- OAuth käyttää PKCE S256 -haastetta, state kulutetaan transaktionaalisesti vain kerran, code exchange ja token storage pysyvät serverillä, ja vanhentunut access token päivitetään `tokenAccess.ts`-rajassa; rotated refresh token korvaa vanhan tallennetun arvon
- Firestore-rate-limitit ovat UID-kohtaisia ja transaktionaalisia: auth `10/min`, search `30/min`, import `20/min`
- Ravelry API -kutsut käyttävät Bearer-tokenia vain `api.ravelry.com`-hostiin; URL-import hyväksyy vain HTTP(S)-URL:n muodossa `ravelry.com/patterns/library/{slug}` ja normalisoi canonical URL:n
- `firestore.rules` kieltää client read/write -pääsyn sekä Ravelry-kokoelmiin että oletuksena kaikkiin muihin dokumentteihin; Admin SDK:ta käyttävät Functions-funktiot omistavat tallennuksen

BuildConfig-kentät:

- debug-only `SENTRY_DSN`

Android ei enää määritä Ravelry credential `BuildConfig` -kenttiä, eikä `debug.credentials.properties`, `local.properties`, resurssit tai source saa sisältää Ravelry-secretejä. `app/google-services.json` pidetään ignored-polussa paikallisesti tai luodaan CI:ssä `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64` -salaisuudesta. Sentryn release-polku on no-op eikä release-luokkapolussa saa olla `io.sentry`-riippuvuuksia.

### Pro / Billing / Trial

Koodissa vahvistuvat nykyfaktat:

- 14 päivän ilmainen kokeilu
- yksi kertamaksullinen Pro-tuote (ei tilausmalli)

Huomio source of truthista:

- product ID + trial-pituus: koodi (`BillingManager`, `TrialManager`)
- saatavuus, hinnat ja store-listaukset eivät ole tämän tiedoston tekninen source of truth, koska ne eivät vahvistu toteutuksesta

Billing-tuote:

- `BillingManager.PRODUCT_ID = "knittools_pro"`
- tuotetyyppi on `BillingClient.ProductType.INAPP`, ei subscription
- Billing 8:n automaattisen reconnectin lisäksi ensimmäisen non-OK setupin ympärillä on kolme yritystä kahden sekunnin välein; yritysten loputtua purchase state merkitään valmiiksi mutta unavailable-tilaan
- startup-query hyväksyy Pro-ostoksi vain `PURCHASED`-tilan ja acknowledgeaa tarvittaessa; acknowledge-uusintaviive on viisi sekuntia
- restore-polun tulosmalli erottaa `RESTORED`, `NOT_FOUND` ja `FAILED`

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

- release- ja non-debug-käytössä `ProState.hasFeature(feature, debugUnlockAllFeatures = false)` vastaa yhtä Pro-tasoa: trial active tai purchase avaa ominaisuudet
- debug-buildissä `ProState.hasFeature(...)` avaa feature-gatet `BuildConfig.DEBUG`-oletuksella muuttamatta `isPro`-arvoa, billing-ostotilaa, trial-tilaa, `purchaseTimestamp`ia tai upgrade UI:n ostoväitteitä
- per-feature-gating on UI- ja käyttölogiikassa nimetty, mutta ostotasoja on edelleen vain yksi
- `ProManager` yhdistää trial- ja ostotilan prioriteetilla purchased > active trial > expired; `initialStateReady` seuraa billing-readinessiä
- `hasFeatureAfterInitialLoad(...)` odottaa Pro- ja purchase-tilan alustusta enintään kaksi sekuntia ja tarkistaa vielä billing-flagin, jotta maksettu käyttäjä ei fail-closea cold startissa
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

## Projektityötila ja korttipinnat

### Counter workspace

Counterin varsinainen työtila on nykyään yksi `LazyColumn` tiedostossa `CounterWorkspaceSections.kt`.

Keskeiset rajat:

- `CounterScreen.kt` omistaa sheet/dialog-statea, `CounterScreenActions`-mallilla ryhmitellyt route callbackit, feature-gate-päätökset ja ViewModel-kutsujen johdotuksen
- `CounterScreen.kt` omistaa myös counter-reitin top barin: back-nuoli, iso uppercase-projektinimi ja overflow ovat samassa headerissa
- `CounterScreenDecisions.kt` omistaa pienet feature-portitus- ja stitch tracking -päätökset (`requestCounterFeature`, `handleStitchTrackingToggle`)
- `CounterUiStateReducers.kt` omistaa projektin havainnoinnin, counter-muutosten, aktiivisen reminderin ja dismissed reminder -tilan yhdistämisen `CounterUiState`en
- `CounterWorkspaceSections.kt` omistaa työtilan järjestyksen, päälaskuriheron, target-helperin, content-card-slotin, lisälaskurit, stitch trackingin ja reminder-alertin sijoittelun
- `CounterImageButton.kt` omistaa päälaskuriheron ensisijaiset plus/miinus/undo-napit; visualit tulevat `drawable-nodpi/counter_plus_button.webp`, `counter_minus_button.webp` ja `counter_undo_button.webp` -asseteista, mutta painallus, semantics ja content descriptionit pysyvät Compose-komponentissa
- `CounterImageButton`-funktion pakolliset parametrit ovat `imageRes`, `contentDescription`, `visualSize` ja `onClick`; ensimmäinen valinnainen parametri on `modifier`, ennen `visualOffsetY`- ja `enabled`-parametreja. Tätä järjestystä suojaa `CounterImageButtonSourceTest`, jotta komponentti pysyy Compose-kutsupintojen odotuksen mukaisena.
- `CounterStepperButton.kt` omistaa pienemmät repeat-, stitch- ja extra-counter-stepperit; `CounterStepButtonFaceAppearance` ryhmittelee stepper-face-mitat ja värit, ja `CounterStepSymbolIcon` piirtää plus/miinus-symbolin `Canvas`illa pyöreän borderin sisälle ilman Material-ikoneita tai hero-button-bitmappeja
- `CounterProjectContentCards.kt` omistaa projektin viiden neliökortin "content cards" -mallin, jossa Reminders-kortti keskitetään omalle rivilleen
- `YarnManagementSheet.kt` omistaa counterista avattavan yarn management -sheetin ja projektikohtaisen yarn note -lomakkeen
- `ProjectDetailsDialog.kt` on jaettu projektin luonnin ja projektin tietojen muokkauksen pinta; se omistaa nimen, craft typen, päälaskurin labelin ja custom-labelin UI-valinnat
- `MainCounterDisplayText.kt` muuntaa `CounterValueFormatter`in slotit lokalisoiduiksi Compose-teksteiksi päälaskurin herolle, targetille, content descriptioneille ja project cardeille
- `ui/theme/CounterDimens.kt` omistaa counterin hero-, progress-, repeat-pill-, grid-, extra-counter-, spacing-, icon- ja touch-target-mitoitustokenit
- `CounterQuickActions.kt` ja `CounterProjectInfo.kt` on poistettu nykyisestä pinnasta; älä palauta niiden mallia dokumentin perusteella

Ensimmäisen viewportin järjestys on tarkoituksella rauhallinen laskurityökalu:

- top bar näyttää vain back-nuolen, projektinimen ja overflow-menun
- ensimmäinen `LazyColumn`-item on `counter-hero`, jonka sisällä ovat mahdollinen repeat/section-rivi, row label, iso rivinumero, target progress/helper, `CounterImageButton`-päälaskurikontrollit ja aktiivinen stitch tracker silloin kun silmukkaseuranta on käytössä
- aktiivinen reminder-alert, `ProjectContentCards` ja extra counters alkavat vasta hero-scrollin jälkeen
- `Screen.Counter.route` ei kuulu `HIDE_BOTTOM_BAR_ROUTES`-joukkoon, joten alanavigaatio pysyy näkyvissä counterissa

Päälaskurin nappimalli:

- hero-päälaskurin minus käyttää `R.drawable.counter_minus_button` -assettia, plus käyttää `R.drawable.counter_plus_button` -assettia ja undo käyttää `R.drawable.counter_undo_button` -assettia
- hero-nappien touch- ja visual-mitat ovat `CounterDimens.CounterMinusTouchSize`, `CounterMinusVisualSize`, `CounterPrimaryTouchSize`, `CounterPrimaryVisualSize`, `CounterUndoTouchSize` ja `CounterUndoVisualSize`
- hero-napit eivät käytä enää vanhaa `CounterCraftButton`-mallia eivätkä geneerisiä `plus_button` / `minus_button` -drawableja
- repeat-pill, stitch tracker ja extra-counterit käyttävät edelleen jaettua `CounterStepButtonFace` / `CounterStepSymbolIcon` -pintaa

`ProjectContentCards` näyttää aina nämä viisi neliökorttia ilman preview-tekstejä, tiedostonimiä, kuvamääriä, chevroneita tai reminder-viestejä. Ensimmäiset neljä korttia ovat kahden sarakkeen neliögridissä ja Reminders-kortti on samankokoisena keskitetty omalle rivilleen:

- pattern:
  - title on `Open Pattern`, jos `patternUri` tai `linkedPattern` on olemassa
  - title on `Add Pattern`, jos patternia ei ole
  - jos `patternUri` on olemassa, kortti avaa PDF-katselun
  - jos vain `linkedPattern` on olemassa, kortti avaa pattern-info-polun
  - jos patternia ei ole, kortti avaa pattern picker -flow'n
- yarn:
  - title on aina `Yarn`
  - kortti avaa yarn management -sheetin sekä tyhjänä että olemassa olevalla yarn-datalla
- notes:
  - title on aina `Notes`
  - kortti noudattaa nykyistä Pro-gateä ja notes-flow'ta
- photos:
  - title on aina `Photos`
  - kortti noudattaa progress photo Pro-gateä ja photo gallery -flow'ta
- reminders:
  - title on aina `Reminders`
  - kortti avaa nykyisen reminder management -flow'n

Counter-headerin nykyinen UX-raja:

- headerissa ei näytetä pattern-subtitlea, PDF-nimeä, Ravelry-nimeä eikä `Pattern attached` -tekstiä
- tarkka pattern-nimi tai tiedostonimi ei näy counterin projektikortissa
- `ProjectCard` piilottaa raakamuotoisen `.pdf`-nimen secondary-linelta, jos se olisi muuten ainoa pattern-nimi

Yarn management -sheetin nykyinen malli:

- `YarnManagementSheet` näyttää sekä linkitetyt My Yarn -kortit että projektikohtaiset yarn notes samassa sheetissä
- kaksi päävalintaa ovat `Choose from My Yarn` ja `Add yarn to project`; projektikohtainen yarn note ei luo automaattisesti lankakorttia
- `CounterViewModel.saveProjectYarnNote(...)` tallentaa `project_yarn_notes`-rivin `ProjectYarnNoteRepository.save(...)`-polun kautta
- `CounterViewModel.saveProjectYarnNoteToMyYarn(noteId)` luo linkitetyn `YarnCard`in statuksella `YarnCardStatus.IN_USE` ja tallentaa `savedYarnCardId`-viitteen samaan repository-transaktioon
- projektikohtaiset yarn notes pysyvät management sheetissä; niitä ei tuoda takaisin `ProjectContentCards`-previewksi

### Project list

`ProjectListScreen` ja `ProjectListViewModel` kokoavat Projects-tabin listapinnan.

Nykyinen listakäyttäytyminen:

- aktiiviset ja valmistuneet projektit haetaan `CounterRepository`n sort-order-aware flow'ista
- sort order tulee `ProjectSortOrder`-enumista ja DataStore tallentaa `persistedValue`-arvon
- free-käyttäjälle uuden aktiivisen projektin luonti pysäytetään, jos aktiivisia projekteja on jo vähintään yksi
- projektin luonti käyttää `ProjectDetailsDialog`ia, oletuksena `CraftType.KNITTING` ja craft typen mukainen päälaskurin label
- completed-projektien näkyvyys tulee `PreferencesManager.showCompletedProjects`-asetuksesta
- `ContinueKnittingProject` valitaan ensimmäisestä aktiivisesta projektista, jonka `count > 0`
- project card näyttää päälaskurin craft/label-sääntöjen mukaisen count-tekstin, viimeksi päivitetyn päivän, ensimmäisen linkitetyn langan nimen, kuvamäärän, pattern-tilan ja note-indikaattorin
- project cardin pattern-, photo-, note- ja yarn-pinnat ovat klikkialueita, eivät pelkkiä koristeita
- yarn-korttiin navigointi käyttää ensimmäistä `parseYarnCardIds(project.yarnCardIds)`-tulosta ja vie Library-tabin `yarn_card_detail/{cardId}`-reitille

### Project actions

`ProjectActionsBottomSheet` sisältää projektin hallintatoiminnot.

Nykyinen jako:

- projektisisältö: notes, photos, pattern, yarn ja reminders ovat ensisijaisesti content-cardien kautta; aktiivinen reminder voi näkyä vasta hero-scrollin jälkeisessä sisällössä, ei herossa eikä preview-korttina
- action sheetin `This project` -osio sisältää reminders-listan ja counters-listan
- action sheetin `Counter tools` -osio sisältää add counter -polun, stitches-per-row-asetuksen ja track stitches -kytkimen
- action sheetin `Project actions` -osio sisältää session historyn, rename-, reset-, complete/archive- ja delete-polut
- stitch trackingin kytkentä pyytää ensin stitch countin, jos seuranta yritetään ottaa käyttöön ilman positiivista stitch countia

## Library ja lankakortit

### Library hub

`LibraryScreen` on laskurista erillinen kokoelmanäkymä, ei geneerinen dashboard.

Nykyiset hub-rivit:

- `Saved Patterns`
- `My Yarn`
- `All Photos`
- referenssit: needles, size charts, abbreviations, chart symbols
- abbreviations-reitillä on optional craft type -argumentti `abbreviations?craftType={craftType}`; puuttuva tai tuntematon arvo putoaa `CraftType.KNITTING`-tilaan

Hub näyttää laskurit saved pattern-, yarn card- ja photo-määrille `LibraryViewModel`n flow'ista.

Reference-huomio:

- `AbbreviationData.search(...)` ottaa craft typen vastaan, mutta nykyisessä datassa `KNITTING` ja `CROCHET` palauttavat saman abbreviation-listan

### My Yarn

`MyYarnScreen` on nykyisessä checkoutissa sekä lista että manuaalisen lankakortin luontipinta.

Nykyiset faktat:

- tyhjässä tilassa on eksplisiittinen `Add Yarn` -painike
- ei-tyhjässä listassa on `FloatingActionButton`, joka avaa `ManualYarnCardSheet`in
- `ManualYarnCardSheet` käyttää `ManualYarnCardInput`-mallia
- manuaalisen kortin kentät ovat `yarnName`, `brand`, `quantity`, `weightCategory`, `colorName`, `colorNumber` ja `dyeLot`
- `LibraryViewModel.createManualYarnCard(...)` trimmaa tekstikentät, vaatii ei-tyhjän nimen, pakottaa määrän vähintään arvoon `1` ja tallentaa statuksella `YarnCardStatus.IN_STASH`
- manuaalinen flow ei ole skanneri eikä AI-parseri; sen kopio ja testit on kirjoitettu ilman yarn label scan -kieltä
- listakortin summary käyttää tekstiä ja yhtä väripistettä, ei metadata pill -komponentteja
- long press käynnistää multi-select-tilan; valitut kortit poistetaan `YarnCardRepository.deleteCards(...)`-metodilla

### Yarn card detail

`YarnCardDetailScreen` käyttää `YarnCardViewModel`ia Library-graafin parent scopessa.

Nykyiset detail-toiminnot:

- reitti tarkistaa `cardId`-argumentin ja poistuu Libraryyn, jos id puuttuu tai `YarnCardRepository.observeCard(id)` palauttaa kadonneen rivin
- status, määrä ja linkitetty projekti ovat muokattavissa
- linkitetyn projektin counteriin voi avata detailistä `CounterViewModel.selectProjectByIdForLaunch(...)`-polun kautta
- "Edit details" avaa saman `ManualYarnCardSheet`-komponentin esitäytettynä nykyisestä kortista
- detail-edit tallentaa `YarnCardRepository.saveCard(...)`-metodilla saman id:n päälle ja säilyttää olemassa olevat lisäkentät, kuten `fiberContent`, `weightGrams`, `lengthMeters`, `needleSize`, `gaugeInfo`, `careSymbols`, `photoUri`, status ja linkitetty projekti
- tyhjät optional detailit eivät piilota osiota kokonaan, vaan näyttävät intentional partial-data -empty staten
- yarn photo -toiminto käyttää Android photo picker -sopimusta `ActivityResultContracts.PickVisualMedia.ImageOnly`
- `YarnCardViewModel.updatePhotoUri(...)` välittää valitun URI:n repositorylle; storage-kopiointi ja vanhan kuvan siivous eivät tapahdu composablessa

### Yarn link invariants

Lankakorttien ja projektien välinen linkitys ei ole enää yksisuuntainen UI-apuri.

Source of truth:

- kortin suora linkki: `yarn_cards.linkedProjectId`
- projektin käänteinen lista: `counter_projects.yarnCardIds`
- CSV-parsaus ja formatointi: `domain/model/YarnCardLinks.kt`
- kirjoitusrajapinta: `YarnCardRepository.saveCard(...)`, `updateLinkedProjectId(...)`, `clearLinkedProject(...)` ja `deleteCards(...)`

Repository-säännöt:

- `saveCard(...)` normalisoi olemassa olevan `linkedProjectId`-arvon vain olemassa olevaan projektiin
- `updateLinkedProjectId(...)` päivittää kortin ja kaikkien projektien CSV-linkit saman `DatabaseTransactionRunner`-transaktion sisällä
- `deleteCards(...)` poistaa kortti-id:t projektien CSV-listoista ennen korttirivien poistoa ja siivoaa app-owned kuvat IO-dispatcherilla
- projektin poisto kutsuu `YarnCardRepository.clearLinkedProject(projectId)` transaktion sisällä

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
- widget-toiminnot kulkevat `CounterRepository.applyWidgetCountChange(...)` -metodin kautta, joka käyttää samaa `applyMainCounterChange(...)`-semantiikkaa kuin appin päälaskuri ja päivittää count/history/current-stitch-resetin sekä linked-to-main-lisälaskurit transaktiona
- cold start -Pro-portti käyttää `ProManager.hasFeatureAfterInitialLoad(...)`-polkua eikä tulkitse oletus-`ProState`a lopulliseksi ennen billing/trial-readinessiä
- action receiver on `exported=false`, käyttää `goAsync()`-työtä ja Hilt `WidgetEntryPoint` -rajaa; widget-mutaatio ei kulje exported `MainActivity`-extraan luottamalla

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
- `Secondary` `#93AE4F` — avokado (labelit, osio-otsikot, "CURRENT ROW")
- `SecondaryMuted` `#6B8A35`, `SecondaryContainer` `#3A4020`
- `Tertiary` `#C9A435` — sinappi (vinkit, aksentit)
- `TertiaryContainer` `#3A3520`

Teksti:

- `TextPrimary` `#E8E4D0` — lämmin kerma
- `TextSecondary` `#B8B4A0`
- `TextMuted` `#A8A491`
- `TextDisabled` `#5A5840`

Aksentti:

- `DustyRose` `#B8908F` — Pro trial -teksti, yarn card

Status:

- `Error` `#C44D4D`, `ErrorContainer` `#3A2020`
- `Success` `#8BA44A`, `SuccessContainer` `#3A4020`

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

- `LightSecondary` `#394B18` — tummempi avokado
- `LightSecondaryMuted` `#5A7525`, `LightSecondaryContainer` `#D0DDB5`
- `LightTertiary` `#9A7B18` — tumma kulta/sinappi
- `LightTertiaryContainer` `#E8DFB5`

Teksti (lämmin ruskea, ei mustaa):

- `LightTextPrimary` `#2E2A1E`
- `LightTextSecondary` `#4C4634`
- `LightTextMuted` `#4A473C`
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
| `labelSmall` | SemiBold | 11 | 1.5 (all-caps: "CURRENT ROW", nav-labelit) |

Nykyiset poikkeukset:

- `Type.kt` on perusroolien source of truth, mutta tuotantokoodissa on joitain paikallisia `copy(...)`-poikkeuksia painon, koon ja merkkivälin säätöön
- iso counter-numero käyttää fontScale-kompensoitua noin **115sp Bold** -tyyliä `CounterWorkspaceSections.kt`:ssa
- bottom navigation pienentää ja jakaa label-fonttikoon runtime-mittauksen perusteella, jotta pisimmät lokalisoidut tabitekstit mahtuvat viidelle tabille
- counterin content-cardit, action sheet -otsikot, stitch tracking -badge ja jotkin chart-/label-pinnat käyttävät paikallisia typografian paino- tai label-säätöjä
- uutta UI:ta tehdessä ensisijainen sääntö on silti käyttää `AppTypography`-rooleja ja lisätä uusi poikkeus vain, jos nykyinen komponenttipinta tai responsiivinen teksti sitä vaatii

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
- counterin ensimmäinen viewport on top bar + iso row-counter-hero + alanavigaatio; reminder-alertit ja projektisisällöt ovat scrollin alla olevia sisältöjä, ja `ProjectContentCards` käyttää neljän kortin gridia plus keskitettyä Reminders-korttia vanhojen quick action / project info -rivien sijaan
- päälaskurin suuret plus/miinus/undo-napit ovat image-backed WebP-napit, mutta pienemmät repeat/stitch/extra-counter-stepperit ovat Compose Canvasilla piirrettyjä pyöreitä symboleita
- project list -kortit toimivat nyt myös syvälinkkeinä patterniin, kuviin, muistiinpanoihin ja ensimmäiseen linkitettyyn lankakorttiin
- `My Yarn` tukee manuaalista lankakortin luontia; se ei ole skanneri- tai AI-parseripinta
- yarn card detailissä voi muokata manuaalisia perustietoja ja vaihtaa kuvan Android photo pickerillä
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
  - launcher intent
  - OAuth callback intent: `knittools://ravelry-auth-complete`
  - Ravelry share import: `ACTION_SEND` + `text/plain`
- `CounterWidgetReceiver` on `exported=true` + `BIND_APPWIDGET`
- `CounterWidgetActions` on `exported=false`
- `FileProvider` on `exported=false`
- FileProvider-roots: `progress_photos/` ja `pattern_captures/`
- runtime-kielivalinta käyttää manifestin `android:localeConfig="@xml/locales_config"` -asetusta

Huomio:

- `google-services`-plugin on sallittu vain Ravelry Firebase -integraatiolle ja applikoidaan, kun ignored `app/google-services.json` on paikallaan
- `app/google-services.json` kuuluu nykyiseen Firebase-buildiin, mutta sitä ei pidä commitoida; CI luo sen `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64` -salaisuudesta, ja staattinen Android lint saa toimia ilman paikallista tiedostoa

## CI, scannerit ja paikalliset wrapperit

GitHub Actions:

- `.github/workflows/build.yml` ajaa push- ja pull request -tapahtumissa `assembleDebug`, `test`, `:app:ktlintCheck`, `:app:detekt` ja `lint`
- build-workflow käyttää pinnejä `actions/checkout` v6.0.3, `actions/setup-java` v5.5.0 ja `gradle/actions/setup-gradle` v6.2.0; versio näkyy kommentissa, mutta hash on varsinainen lukitus
- `.github/workflows/codeql.yml` ajaa push-, pull request- ja maanantaiaikataululla Java/Kotlin CodeQL -analyysin manuaalibuildilla; se käyttää `assembleDebug --no-daemon`, `android-actions/setup-android` v4.0.1 ja `github/codeql-action` v4.36.2 pin-hasheilla
- `.github/dependabot.yml` seuraa Gradle-riippuvuuksia rootissa, GitHub Actions -riippuvuuksia rootissa, npm-riippuvuuksia `.deepsec`-hakemistossa sekä npm-riippuvuuksia `functions`-hakemistossa; CodeQL action -päivitykset ryhmitellään `codeql-action`-ryhmään

Paikalliset PowerShell-wrapperit:

- `tools/ac.ps1`, `cr.ps1`, `cs.ps1`, `db.ps1`, `dc.ps1`, `ds.ps1`, `ga.ps1`, `lc.ps1`, `ms.ps1`, `os.ps1`, `pc.ps1`, `ql.ps1`, `sc.ps1`, `sentry.ps1` ja `ss.ps1` ovat ohuet delegaatit `C:\Dev\Android-check\tools\InvokeProjectCheck.ps1`-skriptiin
- wrapperit asettavat `-ProjectCheckCommand`-arvon ja palauttavat nyt kutsutun tarkistuksen exit-koodin `exit $LASTEXITCODE` -rivillä; `pc.ps1` lisäksi asettaa oletuksena `PMD_CPD_MINIMUM_TOKENS=100`, jos ympäristömuuttuja puuttuu
- `tools/rs.ps1` ja `tools/rst.ps1` pysyvät repo-lokaaleina release-surface -reitteinä; `tools/ad.ps1` on erillinen debug APK:n build/install -wrapper

OSV ja dependency-surface:

- `gradle/osv-scanner.toml` ei tee projektinlaajuista package-ignorea; OSV lukee tuettuja lock/verification-tiedostoja, joten build-tool-only-false positivet rajataan pakettikohtaisilla `[[PackageOverrides]]`-entryillä
- nykyiset OSV-poikkeukset koskevat Gradle verification metadataan päätyviä build-tool-riippuvuuksia, kuten Logback, Jackson, Guava `31.0.1-jre`, Okio `2.10.0`, Wire Runtime, Netty 4.1.x, Commons Lang, Apache HttpClient, jose4j, Bouncy Castle ja JDOM2; appin runtime-CVE-polku tarkistetaan erikseen OWASP dependency-checkillä

DeepSec:

- `.deepsec/package.json` käyttää `deepsec 2.1.2`:ta ja sisältää `deepsec:scan`, `deepsec:scan:custom`, `deepsec:process`, `deepsec:revalidate`, `deepsec:export`, `deepsec:report`, `deepsec:report:custom` ja `test:matchers` -skriptit
- custom scan ajaa matcherit `android-exported-component`, `android-kotlin-entrypoint-surface`, `android-intent-input-surface`, `fileprovider-broad-path`, `knittools-file-uri-surface`, `android-uri-share-without-clipdata`, `ravelry-firebase-callable-surface`, `ravelry-credential-surface`, `sensitive-android-log` ja `widget-mutation-surface`
- `.deepsec/deepsec.config.ts` rajaa KnitTools-projektin rootiksi `..`, lisää prioriteettipoluiksi Android-manifestin, `auth/`, `widget/` ja `data/storage/` -pinnat ja ohjaa tarkastusta exported component-, OAuth callback-, FileProvider URI grant-, widget mutation-, Ravelry credential- ja sensitive logging -riskeihin
- uudet Kotlin-matcherit nostavat tarkastuspintaan Android activity/receiver/service/worker-entrypointit, intent data/extras -luvut, FileProvider/SAF/contentResolver/file-copy/file-delete -rajat, Firebase Auth/Functions callable -rajat, Ravelry callable -nimet ja widgetin counter-mutaatiot
- `test:matchers` kääntää matcherit TypeScriptillä `.tmp/test-build`-hakemistoon ja ajaa Node testit `knittools-kotlin-security-surface.test.js`-tiedostolle
- DeepSec accepted-risk -merkintä on rajattu dokumentoituihin historiallisiin Ravelry credential -findingeihin `config/security-decisions.md`-päätöksen perusteella; se ei saa laajeta kalliisiin API abuse-, prompt injection- tai muihin finding-tyyppeihin ilman uutta päätöstä

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
- project workspace -source-testit, jotka varmistavat `ProjectContentCards`-rakenteen, puhtaan ensimmäisen viewportin, counter-copyrajojen ja poistettujen quick-action/project-info-komponenttien puuttumisen
- counter button -source-testit, jotka varmistavat päälaskurin `CounterImageButton`-assetit, undo-buttonin WebP-polun, `modifier`-parametrin sijainnin ensimmäisenä valinnaisena parametrina, pienempien stepperien Canvas-piirretyt symbolit ja `CounterStepButtonFaceAppearance`-rajapinnan
- My Yarn / Yarn Card detail -source-testit, jotka varmistavat manuaalisen lankakortin, photo picker -toiminnon ja skannerikielen puuttumisen
- repository transaction boundary -testit, jotka varmistavat pattern-, yarn- ja project-linkkien sekä tiedostosiivouksen transaktiorajat
- Ravelry/Firebase source- ja unit-testit, jotka varmistavat backend callable -rajat, Auth Tab / Custom Tabs -authin, Browse Ravelry -Custom Tabs -avaamisen share päällä, share-importin, import confirmation -tilat, search-tabin connected-gatet, saved pattern detailin, lokalisaatiot, URL-avausfallbackin ja release-surface -sopimuksen
- DeepSec source- ja matcher-testit, jotka varmistavat custom report -putken revalidation-vaiheen, accepted-risk-rajauksen sekä Kotlin entrypoint-, intent input-, FileProvider/SAF-, Firebase callable- ja widget mutation -matcherit
- CI/source-testit varmistavat myös CodeQL/build-workflowjen ja security-tooling-konfigien odotettuja ankkureita, joten workflow- tai scanner-versiomuutos voi rikkoa testin vaikka app-koodi ei muuttuisi
- `SonarMaintainabilitySourceTest` varmistaa tällä hetkellä konkreettisia ylläpidettävyysrajoja: `MainActivity`n tiivis OAuth/share-import-haara, `toUri()`-käyttö, Ravelry backend -pyynnön optional-parametriapu, `FirebaseBindingsModule`-fun-interface, action/state-parametrien ryhmittely, Ravelry search -renderöinnin apurijako ja `SentryInit.kt` coverage-exclusion
- `DomainModelCoverageTest` kattaa domain-mallien helposti unohtuvia fallbackeja: yarn card display name, `SavedPatternSource.fromPersistedValue`, saved patternin legacy-yhteensopivuuspropertyt `ravelryId`/`patternUrl`, persisted enum fallbackit, custom main counter label -sanitoinnin ja unsupported yarn status -normalisoinnin

Pienimmät hyödylliset tarkistuskomennot:

- `.\gradlew.bat --no-configuration-cache :app:testDebugUnitTest --rerun-tasks`
- `.\gradlew.bat --no-configuration-cache :app:lintDebug :app:ktlintCheck :app:detekt`
- `.\gradlew.bat --no-configuration-cache :app:kspDebugKotlin`
- `.\gradlew.bat sonar --console=plain` silloin kun `SONAR_TOKEN` on asetettu; wrapper `tools\sonar.ps1` kirjoittaa `reports\sonar.txt` ja voi hakea avoimet issuet `sonar.exe list issues` -komennolla, jos CLI löytyy
- `npm --prefix functions test`
- `npm --prefix functions run build`
- `pnpm --dir .deepsec test:matchers`
- `pnpm --dir .deepsec run deepsec:scan:custom`
- `git diff --check`
- `tools\rs.ps1` KnitTools-kohtaiseen release-/security-surface -sopimustarkistukseen
- `tools\rst.ps1` release-surface -skriptin self-testiin

Artifact-buildien nykyinen paikallisraja:

- `.\gradlew.bat --no-configuration-cache :app:assembleDebug` saa kääntyä ilman oikeaa Firebase-projektia debug-only placeholderilla; `.\gradlew.bat --no-configuration-cache :app:assembleRelease` ja `.\gradlew.bat --no-configuration-cache :app:bundleRelease` pysähtyvät tarkoituksella `:app:verifyGoogleServicesJson`-tehtävään, jos ignored `app/google-services.json` puuttuu; signing-muuttujien puute pysäyttää lisäksi `appReleaseArtifactTasks`-joukkoon kuuluvat package/sign/publish-release-taskit
- tämä checkout on viimeksi tarkistettu niin, että `app/google-services.json`, `KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64` ja `GOOGLE_SERVICES_JSON_BASE64` puuttuivat; debug build, staattinen Android lint / ktlint / detekt ja unit-testit ovat silti ajettavissa ilman paikallista Firebase JSONia

Julkaisuvalmiuden muistilista:

- pidä dependency-check kehitysvaiheessa manuaalisena, mutta dokumentoi ennen julkaisua puhtaan koneen komento ja tarvittavat `DEPENDENCY_CHECK_AUTO_UPDATE` / `NVD_API_KEY` -odotukset
- päätä ennen julkaisua, jääkö Baseline Profile manuaaliseksi vai lisätäänkö sille emulaattori-/managed-device-polku CI:hin
- pidä `ktlintCheck`, detekt ja Android lint pakollisina CI:ssä; nykyinen build-workflow ajaa `assembleDebug`, `test`, `:app:ktlintCheck`, `:app:detekt` ja `lint`
- CodeQL-workflow on manuaalibuildinen Java/Kotlin-analyysi ja rakentaa `assembleDebug --no-daemon`
- `release-surface.ps1` ei korvaa linttiä, dependency-checkiä tai Semgrepiä; se tarkistaa vain tässä repossa sovitut manifest-, FileProvider-, credential-, Sentry-, Firebase/AI/voice-, Room-, widget-launch- ja lokalisaatiorajat. Ravelry Firebase -migraation aikana sallittu Firebase-pinta on rajattu Firebase Auth/Functions/Google Services -polkuun. Skripti hylkää trackatun `app/google-services.json`in ja etsii paikallisesti/envistä tunnettuja Ravelry-secret-arvoja lähteistä, resursseista, generated BuildConfig -vakioista, Gradle-tiedostoista, manifesteista, testeistä, APK:sta ja AAB:sta tulostamatta arvoja.

Älä käytä agenttityössä käyttäjän wrapper-skriptejä `lint-check` tai `security-check`.

## Ominaisuudet nykykoodin perusteella

### Projektit ja laskuri

- useita projekteja
- päälaskuri, jonka craft type on neulonta tai virkkaus
- päälaskurin label voi olla rows, rounds, repeats tai custom
- päälaskurin hero-plus/miinus/undo käyttää `CounterImageButton`-komponenttia ja kolmea appin omaa WebP-button-assettia
- stitch tracking
- useita projektikohtaisia laskureita
- lisälaskurit voivat seurata päälaskurin toteutunutta deltaa `linkedToMainCounter`-kentällä, paitsi repeat-section-laskurit
- shaping/repeating-counter-polut
- row reminders
- progress photos
- projektimuistiinpanot
- project content cards patternille, langalle, muistiinpanoille, kuville ja seuraavalle muistutukselle
- session history
- pattern-PDF:n liittäminen projektiin
- saved patternin metadata-only liittäminen projektiin `linkedPatternId`-polulla; jos saved patternilla on `localPdfUri`, sama attachment voi avata PDF-viewerin
- pattern viewer + pysyvä reading line / row-marker -overlay sekä Free-tason PDF-annotointi: kynä, korostus, muodot, tekstimuistiinpanot, huomautukset, undo/redo, master/project-tasot, laskuriin yhdistetty kaavion seuranta ja SAF-vienti
- projektin attached-PDF:n reading line tallentuu projektiriville, ja rivikartta tallentuu `patternRowMapping`-kenttään `RowMarker(row,page,yPosition)` -ankkureina; library-only viewerin reading line on vain katselusession tila
- projektin pattern viewer tukee reading line -rivin tallennusta, rivimerkkien poistoa ja kahden pisteen rivikalibrointia; library-only viewer ei tallenna näitä Roomiin
- Drive/Dropbox-copy on nykykoodissa SAF PDF -pickerin käyttäjätekstiä, ei jatkuvaa pilvisynkkaa
- target rows
- project list -korttien deep linkit pattern viewer-, photo gallery-, notes editor- ja yarn card detail -pintoihin

### Library

- saved patterns
- my yarn / yarn cards
- saved pattern avaa `saved_pattern_detail/{savedPatternId}`-reitin metadata- ja toimintopinnaksi; detailin `Open Pattern` avaa `library_pattern_viewer/{savedPatternId}`-reitin vain paikalliselle `localPdfUri`-PDF:lle ja Ravelry-linkit avataan ulkoisesti Ravelryssä
- `My Yarn` listaa olemassa olevat yarn cardit, tukee multi-select-poistoa ja avaa `yarn_card_detail/{cardId}`-näkymän
- `My Yarn` tukee manuaalista yarn card -luontia `ManualYarnCardSheet`in kautta
- yarn card detailissä voi muuttaa statusta, määrää, projektia, manuaalisia perustietoja ja kuvaa, avata linkitetyn projektin counteriin sekä poistaa kortin
- yarn card -kuvat tallennetaan app-owned `yarn_photos/<cardId>` -polkuun `YarnPhotoStorage`n kautta eikä FileProvider-share-polkuina
- all photos
- multi-select batch-poistot
- reference-näkymät: needles, size charts, abbreviations, chart symbols
- abbreviations-reitti hyväksyy craft type -argumentin, mutta nykyinen abbreviation-data on sama neulonnalle ja virkkaukselle

### Tools

- gauge
- increase/decrease
- cast on
- yarn estimator
- Ravelry search/detail/import, mukaan lukien share-intentistä tuleva Ravelry pattern URL

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
- debug-build avaa feature-gatet `ProState.hasFeature(...)`-polussa muuttamatta ostotilaa tai trial-tilaa

## Koodintarkistuskysymysten tarkastuspintakartta

Tämä osio on tarkoitettu tarkkojen review-kysymysten muodostamiseen. Hyvä kysymys nimeää tutkittavan entry pointin, invariantin, hyväksytyn toteutusrajan ja vaaditun todisteen. Pelkkä luokan tai ominaisuuden nimi ei riitä, koska useat KnitToolsin riskit syntyvät kahden kerroksen välissä.

### 1. Room, relaatiot ja migraatiot

Tarkista yhdessä vähintään:

- `KnitToolsDatabase.kt`, `DatabaseModule.kt`, uusin `16.json`, relevantit `*Entity.kt`-tiedostot ja DAO:t
- domain-mallit sekä `EntityMappers.kt`; pelkkä schema ei todista persisted enumien fallbackia tai sanitointia
- kaikki migraatiopolut vanhimmasta tuetusta versiosta 16:een, ei vain viimeinen `15 -> 16`

Kriittiset faktat:

- oikea `ON DELETE CASCADE` koskee projektin lapsitauluja `counter_history`, `sessions`, `row_reminders`, `progress_photos`, `project_counters`, `project_yarn_notes` ja `pattern_annotations`
- `yarn_cards.linkedProjectId`, `counter_projects.linkedPatternId`, `counter_projects.yarnCardIds` ja `project_yarn_notes.savedYarnCardId` eivät ole tietokantatason foreign key -suhteita; niiden eheys riippuu repository-koodista
- legacy `counter_projects.secondaryCount` on eri konsepti kuin nimetyt `project_counters`-rivit; migraatio tai UI-boundary ei saa luoda siitä automaattista `Pattern repeat` -kopiota
- schema 13:n craft/label/reading-line/link-to-main -kentät, schema 14:n saved-pattern-lähdemetadata, schema 15:n indeksit ja schema 16:n session zone pitää tarkistaa erillisinä muutoksina
- `saved_patterns.ravelryPatternId` on nullable ja vain positiivinen ID on Ravelry-identiteetti; domainin `ravelryId`/`patternUrl` ovat compatibility-propertyjä, eivät nykyisiä persisted sentinelejä

Sopivia review-kysymysrajoja ovat esimerkiksi migraation idempotenssi, backfillin tietohävikki, nullable/default-muutos, indeksin vastaavuus todelliseen kyselyyn, enum-fallback ja repositoryllä ylläpidetyn soft-relaation rikkoutuminen.

### 2. Päälaskuri, history ja linkitetyt laskurit

Yhteinen todistusketju:

- UI-event: `CounterScreen.kt` / `CounterWorkspaceSections.kt`
- state orchestration: `CounterViewModel.kt`
- kirjoitusraja: `CounterRepository.applyMainCounterChange(...)`
- domain-sääntö: `MainCounterChange`, `ProjectCounterLogic`, `RepeatSectionLogic`, `ShapingCounterLogic`
- Room-kirjoitukset: `CounterProjectDao`, `CounterHistoryDao`, `ProjectCounterDao`
- widgetin rinnakkaispolku: `CounterWidgetActions` -> `CounterRepository.applyWidgetCountChange(...)`

Invarianssit:

- valmistunutta projektia ei muuteta
- uusi count ei mene negatiiviseksi, ja linked-counter saa toteutuneen deltan eikä pyydettyä deltaa
- count, history, current-stitch reset ja sallitut linked-counter-deltat ovat yksi Room-transaktio
- undo palauttaa viimeisen history-rivin `previousValue`-arvon, poistaa käytetyn history-rivin ja kääntää linked-counter-deltan
- repeat-section ei saa olla `linkedToMainCounter`, koska sen tila johdetaan päälaskurin riveistä
- widgetin plus/miinus ei saa toteuttaa suppeampaa semantiikkaa kuin appin vastaava toiminto

### 3. Sessio, lifecycle ja Insights

Tarkista `CounterViewModel`in session-start/stop/snapshot-polut, `SessionEntity`, mapperit, `SessionDao`, `SessionMetrics` ja `InsightsViewModel` yhtenä kokonaisuutena.

Invarianssit:

- session aloitushetken zone tallentuu ja säilyy recreationin yli
- `durationSeconds` ja `rowsWorked` ovat laskennan tarkat lähteet; `durationMinutes` on näyttö-/legacy-kenttä
- cross-midnight-segmentointi, heatmap-päivä ja pace bucket käyttävät session zonea; vain legacy-null tai virheellinen zone käyttää nykyistä laitezonea
- DST-siirtymä, aikavyöhykkeen vaihto session jälkeen, nolla-/negatiivinen kesto, range-boundary ja rivien suhteellinen allokointi ovat olennaisia reunatapauksia
- raskas historiadata lasketaan upstreamissa injektoidulla IO-dispatcherilla ennen yhden `InsightsUiState`-tilan keräämistä Composessa
- `CounterViewModel.onCleared()`-snapshot ja `NotesEditorViewModel.onCleared()`-flush ovat tarkoitukselliset application-lifetime-poikkeukset; muu ViewModel-työ ei saa vuotaa `@ApplicationScope`en

### 4. Pattern PDF, reading line ja tiedosto-omistajuus

Tarkista vähintään `PatternPickerSheet`, `PatternAttachmentUriResolver`, `CounterViewModel`, `CounterRepository`, `SavedPatternRepository`, `PatternDocumentStorage`, `AppFileStorage`, `PdfPageRenderer`, `RowMappingParser` ja `ReadingLine`.

Invarianssit:

- ulkoinen PDF tulee SAF `OpenDocument(application/pdf)` -polusta ja kopioidaan app-owned `pattern_pdfs/<projectId>`-hakemistoon ennen attachia
- DB-linkitys ja tiedoston lifecycle eivät ole sama transaktio: vanha tiedosto poistetaan vasta onnistuneen kirjoituksen jälkeen ja vain, jos yksikään projekti tai saved pattern ei enää viittaa siihen
- FileProvider ei exposeeraa `pattern_pdfs`- eikä `yarn_photos`-juurta
- projektin reading line / row markerit ovat Room-tilaa; library viewerin sivu ja viiva ovat vain saveable UI-tilaa
- saman sivun kaksi ankkuria voi interpoloida; toisen sivun ankkuri ei saa liikuttaa nykyistä sivua
- `PdfPageRenderer.renderPage()` ja `close()` ovat serialisoituja, descriptor suljetaan myös konstruktorivirheessä ja bitmap rajataan 4096 pikseliin kummassakin dimensiossa
- näkyvä annotointivirta käyttää `PatternAnnotationViewModel`ia, normalisoituja sivukoordinaatteja ja yhteistä `PatternAnnotationCanvasRenderer`-rendereriä; projektissa master-taso on vain luettava ja projektitaso muokattava

### 5. Kuvat ja file-before/DB-before-delete -järjestys

Progress photo-, yarn photo- ja pattern capture -virtoja ei pidä arvioida yhtenä geneerisenä file helper -polkuna:

- progress capture target ja abandoned capture -siivous kuuluvat `ProgressPhotoRepository`lle ja `ProgressPhotoStorage`lle
- yarn photo replacement kopioi uuden, persistoituu, poistaa vanhan vasta onnistumisen jälkeen ja siivoaa epäonnistuneen uuden kopion
- projektin poisto siivoaa progress- ja capture-tiedostot IO-dispatcherilla, nollaa soft-linkit transaktiossa ja tarkistaa pattern-PDF:n viitteet lopuksi
- yarn-card batch delete irrottaa projektien CSV-linkit ja DB-rivit transaktiossa sekä poistaa tiedostot `NonCancellable + @IoDispatcher` -rajassa
- startup orphan-prune on recovery-polku, ei korvike normaalin replacement/delete-polun rollbackille
- app-owned URI -tarkistus on kanonisoitava `AppFileStorage`ssa ennen poistoa; ulkoista `content://`-mediaa ei saa poistaa app-owned-tiedostona

### 6. Yarn-, pattern- ja project-soft-linkit

Reviewn pitää kattaa linkin molemmat suunnat:

- yarn: `YarnCard.linkedProjectId` + `CounterProject.yarnCardIds`
- pattern: `CounterProject.linkedPatternId` + optional `patternUri` + `SavedPattern.localPdfUri`
- project yarn note: note-rivi + optional `savedYarnCardId`

Invarianssit:

- `YarnCardRepository.updateLinkedProjectId(...)` on eksplisiittisen relinkin kanoninen writer
- `saveCard(...)` normalisoi puuttuvaan projektiin osoittavan linkin pois ja pitää kaikkien projektien CSV-listat synkassa
- `ProjectYarnNoteRepository.saveToMyYarn(...)` luo lankakortin ja kirjoittaa `savedYarnCardId`-viitteen samassa transaktiossa mutta ei poista projektin note-riviä
- saved-pattern-duplikaattijärjestys on positive Ravelry ID -> canonical URL -> exact/normalized original URL -> title+designer vain eksplisiittisesti pyydettäessä
- pattern detach/delete ei saa poistaa paikallista PDF:ää, jos toinen projekti tai saved pattern viittaa samaan URIin

### 7. Exported Android -pinta, intentit ja widget-launch

Tarkista manifesti yhdessä varsinaisen intentin kuluttajan kanssa:

- `MainActivity` on exported launcher, OAuth callback- ja `ACTION_SEND text/plain` -entry point
- `CounterWidgetReceiver` on exported vain `BIND_APPWIDGET`-suojauksella; varsinainen action receiver `CounterWidgetActions` on non-exported
- OAuth callback ei saa kuluttaa counter-tokenia eikä counter-extra saa laukaista OAuth/share-polun sivuvaikutusta
- widget-counteriin siirtyminen hyväksyy vain `CounterLaunchTokenStore`n appin itse antaman, kuluttamattoman, alle 24 tuntia vanhan request ID:n
- tokenin kulutus on atominen, consumed ID säilyy recreationin yli ja intent-extrat tyhjennetään kulutuksen jälkeen
- share-import hyväksyy vain validoidun Ravelry pattern URL:n ja URL route-enkoodataan ennen navigointia
- FileProvider on non-exported, grant URI permissions on käytössä ja root-lista pysyy kapeana

### 8. Pro, trial, billing ja cold start

Tarkista sekä näkyvä UI-gate että toiminnon route/ViewModel/repository-entry. Passiivinen disabled UI ei yksin suojaa suoraa navigointia tai widget-actionia.

Invarianssit:

- yksi INAPP-tuote `knittools_pro`; trial ja purchase avaavat saman yhden Pro-tason
- purchase state -oletus ei ole vahvistettu free-tila ennen `purchaseStateReady`/`initialStateReady`-signaalia
- trialin kellomanipulaation lippu on pysyvä ja last-known-aika monotoninen
- debug unlock vaikuttaa `hasFeature`-päätökseen, ei `isPro`-, trial-, purchase- tai upgrade-copy-tilaan
- widget käyttää cold-start-safe `hasFeatureAfterInitialLoad(...)`-porttia
- historia-, muistiinpano-, kamera-, insights-, reminder-, photo-, extra-counter- ja yarn-rajoja pitää tarkistaa varsinaisesta mutaatiopinnasta, ei vain composablen näkyvyydestä

### 9. Ravelry Android <-> Functions -sopimus

Review-kysymyksessä kannattaa nimetä sekä Android callable että backend export:

- Android: `FirebaseAnonymousAuthGateway`, `RavelryBackendClient`, `RavelryBackendMappers`, `RavelryAuthManager`, `RavelryApiService`
- backend: `functions/src/index.ts`, `config.ts`, `callable.ts`, `authCore.ts`, `oauthStateStore.ts`, `oauth2.ts`, `tokenAccess.ts`, `rateLimit.ts`, `patternImport.ts`, `urlParsing.ts`, `sanitizedTypes.ts`

Invarianssit:

- callable-nimi, payload-key, nullable/required-kenttä ja response mapper täsmäävät molemmilla puolilla
- Android ei vastaanota client secretiä, access tokenia, refresh tokenia eikä authorization codea app callbackissa
- state on kertakäyttöinen, vanhenee 10 minuutissa ja PKCE verifier sidotaan server-side stateen
- token refresh tapahtuu ennen current-user/search/import-kutsua ja rotated refresh token tallennetaan
- search/import on UID-rate-limitattu ja palauttaa vain sanitisoitua metadataa; backend ei lataa pattern-PDF:ää
- URL-import ei saa hyväksyä mielivaltaista hostia tai Ravelryn muuta URL-pintaa
- availability, canonical URL ja original URL säilyvät mapperissa; puuttuva/ei-positiivinen Ravelry ID ei saa muuttua persisted ID 0:ksi
- Firestore-client ei saa lukea tai kirjoittaa OAuth state-, token- tai rate-limit-dokumentteja

### 10. Navigaatio, shared state ja UI-sopimukset

Tarkista `Screen.kt`, `NavGraph.kt`, `TopLevelNavigation.kt` ja relevantti ViewModel yhdessä.

Invarianssit:

- `TopLevelDestination` omistaa viisi tabia ja niiden start-route-arvot
- `CounterViewModel` on Projects-graafin ja `LibraryViewModel` Library-graafin shared state; väärä back-stack-entry voi luoda rinnakkaisen tilan tai kadottaa valinnan
- `counter`-route ei kanna project ID:tä; project selection tapahtuu shared `CounterViewModel`in kautta myös widget-, Ravelry-, project list- ja yarn detail -entryissä
- route-argumentti validoidaan ja enkoodataan; erityisesti `ravelry_import/{importUrl}` ei saa sisältää raakaa URL-segmenttiä
- yarn detail poistuu, kun observe-flow kertoo rivin kadonneen; optimistic local-only edit ei saa pitää poistettua korttia näkyvissä
- counterin ensimmäinen viewport-, top bar-, fixed five-card grid- ja theme-token-sopimukset ovat nykyisiä tuotepäätöksiä, eivät vain visuaalisia mieltymyksiä
- haptic-preferenssi vaikuttaa sekä eksplisiittisiin counter-värinöihin että `LocalHapticFeedback`in kautta komponenttien oletushaptiikkaan

### 11. Testituloksen tulkinta

Testikerrokset todistavat eri asioita:

- `app/src/test`: JVM-unit-testit, ViewModel/repository-testit ja suuri joukko source-contract-testejä
- `app/src/androidTest`: oikeat Room migration/transaction- ja Compose semantics -instrumentaatiotestit
- `functions/src/**/*.test.ts`: Node-testit OAuth-, state-, rate-limit- ja pattern-import-logiikalle
- `baselineprofile`: käynnistyksen macrobenchmark/baseline profile -generaattori

Reviewssa pitää erottaa:

- behavioral-testi, joka suorittaa logiikan
- fake/DAO-testitupla, joka todistaa repository-sekvenssin mutta ei Roomin oikeaa rollbackia
- source-testi, joka etsii toteutuksesta merkkijonoja tai rakennetta
- instrumentation-testi, joka käyttää oikeaa Android/Room/Compose-runtimea

Source-testin vihreä tulos ei todista, että etsitty ankkuri löytyi, ellei testi ensin varmista ankkurin olemassaoloa. `indexOf`, `substringAfter`, `substringBefore` ja niiden yhdistelmät ovat erityinen false-positive-pinta. Toisaalta source-testin rikkoutuminen voi olla tarkoituksellinen arkkitehtuurisopimuksen hälytys, vaikka runtime-koodi muuten kääntyisi.

### 12. Toteuttamattomat asiat, joita review-kysymys ei saa olettaa

- ei jatkuvaa Drive/Dropbox-syncia, provider SDK:ta, provider OAuthia, background cloud syncia tai multi-device-konfliktimallia
- ei voice/microphone/SpeechRecognizer/TextToSpeech/conversational AI -ominaisuutta
- ei model-backed instruction parseria; `InstructionParser` on regex-only
- ei WorkManager Worker -arkkitehtuuria appin tuotantokoodissa; WorkManager-version kohdistus on Glancen transitiivista build-surfacea
- ei release-Sentryä, analyticsia, tracingia, replayta tai source-context uploadia
- ei Androidissa Ravelry Basic Authia, client secretiä, token storagea tai suoraa Ravelry API -kutsua
- ei erillisiä Pro-tuotetasoja feature-enumin perusteella; `ProFeature` nimeää gateja yhden Pro-tason sisällä

## Asiat jotka vanhenevat helposti

Näihin kannattaa suhtautua epäluuloisesti vanhoissa dokumenteissa:

- build-versiot muuttuvat usein `gradle/libs.versions.toml`-tiedostossa; älä kopioi niitä muistista
- Android SDK -arvojen source of truth on version catalog: `androidCompileSdk`, `androidTargetSdk` ja `androidMinSdk`; `app/build.gradle.kts` ja `baselineprofile/build.gradle.kts` lukevat nämä `libs.versions.*.get().toInt()` -polulla
- `allowBackup`: nykyinen on `false`, ei `true`
- Room schema version: nykyinen on `17`; tarkista aina `KnitToolsDatabase.version`, koko rekisteröity migraatioketju `DatabaseModule.kt`:ssa ja `app/schemas/.../17.json`
- schema 14:n helposti unohtuvat saved-pattern-kentät ovat `source`, `ravelryPatternId`, `originalUrl`, `canonicalUrl`, `localPdfUri`, `isAvailableOffline`, `updatedAt` ja `lastSyncedAt`; schema 15 lisää foreign-key-hakujen indeksit, schema 16 nullable `sessions.zoneId` -kentän ja schema 17 annotaatiotasot sekä versioidut payloadit
- session päivä- ja pace-jako ei saa käyttää aina nykyistä laitteen zonea: uusi sessio tallentaa aloitusvyöhykkeen ja vain legacy-null/virheellinen `zoneId` käyttää nykyistä laitevyöhykettä fallbackina
- `ravelry_import/{importUrl}` on URL-enkoodattu route; älä käytä raakaa URL:ää route-segmenttinä
- `KnitToolsNavHost`, `CounterScreen` ja `RavelrySearchScreen` eivät enää ota kaikkia reittitoimintoja irrallisina callback-parametreina; nykyiset action-mallit ovat `KnitToolsNavActions`, `CounterScreenActions` ja `RavelrySearchActions`
- `PatternCard` ei enää ota erillisiä `name` / `designerName` / `thumbnailUrl` / `difficulty` / `isFree` -parametreja, vaan `PatternCardState`-mallin
- `saved_pattern_detail/{savedPatternId}` on nykyinen saved-pattern metadata/detail -pinta; vanha oletus suoraan PDF-vieweriin avaamisesta pätee vain, kun `localPdfUri` on olemassa
- `app/google-services.json` on tarkoituksella ignored ja release artifact -buildin edellytys; debug artifact voi käyttää ignored `app/src/debug/google-services.json` -placeholderia, eikä root-configin puute tarkoita, että lint/unit-testit olisivat rikki
- vanha `VerifyGoogleServicesJsonTask`-niminen oletus on poistunut build-scriptistä; nykyinen portti on `verifyGoogleServicesJson`-task + `GoogleServicesJsonTaskActions.verify(...)`
- älä sekoita release signing -porttia ja Firebase-config-porttia: signing gate tarkistaa `appReleaseArtifactTasks`-joukon seitsemän `:app:*Release*`-artifact-taskia, kun taas `verifyGoogleServicesJson` on nykykoodissa `firebaseConfiguredArtifactTaskNames = setOf("assembleRelease", "bundleRelease")`
- Sonar-wrapper ei nykyisin aja `assembleDebug`-taskia; jos Sonar-skannaus pysähtyy Firebase JSON -porttiin, tarkista ensin ettei wrapperia tai Gradle-taskigraafia ole palautettu vanhaan `assembleDebug sonar` -malliin
- Android-check-wrapperien onnistuminen/epäonnistuminen tulee nyt niiden delegoiman prosessin exit-koodista; jos wrapper näyttää hiljaisesti onnistuvan, tarkista että tiedostossa on edelleen `exit $LASTEXITCODE`
- `gradle/osv-scanner.toml` ei saa palata yleiseen `ignore = true` -malliin koko verification metadata -pinnalle; nykyinen malli on pakettikohtaiset false-positive-poikkeukset
- DeepSec custom scan ei ole enää vain manifest/FileProvider/Ravelry credential/logging -pinta; nykyinen custom matcher -lista sisältää myös Kotlin entrypointit, intent-inputit, KnitTools file/URI -rajat, Ravelry Firebase callable -rajat ja widget-mutaatiot
- `.deepsec/data/knittools/INFO.md` on scannerin kontekstiteksti eikä toteutuksen source of truth; jos se on ristiriidassa nykyisen koodin, `config/security-decisions.md`-päätöksen tai tämän tiedoston kanssa, tarkista koodi ennen findingin hyväksymistä
- `ProState.hasFeature(...)` ei ole pelkkä `isPro` debug-buildissä; debug avaa feature-gatet erillisenä kehittäjäpolkuna
- Sentry on debug-only diagnostiikkaa; älä lisää Sentry Gradle -pluginia, replayta, tracingia, logcat breadcrumbseja tai release-riippuvuutta ilman uutta product/security-päätöstä
- jos manifest-, FileProvider-, release-credential-, Sentry-, Firebase/AI/voice-, Room-, widget-launch- tai locale-raja näyttää epävarmalta, tarkista nykyinen sopimus myös `tools\release-surface.ps1`:stä ja Ravelry-backendin osalta `config/ravelry-backend-progress.md`:stä
- voice-command-flow on poistettu; älä palauta sitä ilman uutta product/security-päätöstä
- widgetit eivät ole enää pelkkä basic counter-preview vaan niissä on oma state-sync ja viimeistelty kortti-UI
- widgetin plus/miinus käyttää samaa `CounterRepository.applyMainCounterChange(...)`-semantiikkaa kuin appin päälaskuri, joten linked-to-main-lisälaskurit muuttuvat myös widgetistä
- vanhat `yarn_card_review` / `library_yarn_card_review` -reitit eivät ole nykyisessä `Screen.kt` / `NavGraph.kt` -pinnassa; käytössä on `yarn_card_detail/{cardId}`
- `CounterQuickActions` ja `ProjectInfoSection` eivät ole nykyinen counter workspace -malli; käytössä on `CounterProjectContentCards.kt`
- `CounterCraftButton`, `CounterHeroActionButton`, `plus_button` ja `minus_button` eivät ole nykyinen päälaskurin nappimalli; käytössä on `CounterImageButton` sekä `counter_plus_button.webp` / `counter_minus_button.webp` / `counter_undo_button.webp`
- `abbreviations`-route ei ole enää pelkkä staattinen route-string, vaan `abbreviations?craftType={craftType}`; data on silti tällä hetkellä sama neulonnalle ja virkkaukselle
- Drive/Dropbox on nykykoodissa vain SAF-pohjainen pattern-PDF:n valinta-/kopiointipolku; jatkuva sync on tulevaa speksiä `config/future-sync-spec.md`:ssä
- project attached-PDF:n reading line ja `patternRowMapping`-riviankkurit ovat pysyvää projektitilaa, mutta library-only viewerin reading line ei ole Room-skeemassa
- `QuickTipCard.kt` on poistettu; jos näet Quick Tip -tekstiä vanhoissa spekseissä, tarkista nykyinen `ui/components` ja `strings.xml`
- `file_paths.xml` ei exposeeraa `yarn_photos`-rootia nykykoodissa, vaikka `AppFileStorage` osaa edelleen ratkaista legacy `yarn_photos`-URI:t sisäistä siivousta varten
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
