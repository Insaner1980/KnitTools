# Project Instructions

## Build & Test

- `./gradlew assembleDebug` — debug build
- `./gradlew test` — unit tests
- `./gradlew :app:detekt` — static analysis
- `./gradlew lint` — Android lint
- `./gradlew :app:generateBaselineProfile` — generoi Baseline Profile yhdistetyllä laitteella

## Quality Tools (global, in ~/bin/)

- `lint-check` (alias `lc`) — runs ktlint + detekt + Android lint, results in `reports/`
- `security-check` (alias `sc`) — runs semgrep + OWASP dependency-check, results in `reports/`
- Don't run these scripts yourself — user runs them via `! lc` / `! sc`
- `reports/` is gitignored, never commit it

## Conventions

- Hilt for DI, Room for local DB, DataStore for preferences
- ViewModels expose StateFlow, screens collect via collectAsStateWithLifecycle()
- All strings in `res/values/strings.xml` for localization
- No hardcoded colors/dimensions — use theme tokens (`MaterialTheme.colorScheme.*` and `MaterialTheme.knitToolsColors.*`)
- No inline `letterSpacing`, `fontSize`, or `fontWeight` overrides — use Type.kt roles. Exception: CounterScreen's main number (115sp Bold)
- Finnish in commit messages and comments

## UI Rules

- Scaffold-taustaväri: `MaterialTheme.colorScheme.background` kaikissa näytöissä, ei `surface`
- ToolScreenScaffold: puhdas teemapinta, ei ambient-kuvia
- Light+dark teema ("70s Craft Revival"), light oletuksena. `MaterialTheme.knitToolsColors` extended-tokeneille.
- Tools/Library-listat: ei ikoneita korteissa, aksenttivärinen otsikkoteksti per kohde
- Window insets: `consumeWindowInsets(scaffoldPadding)` NavHostissa — sisemmät Scaffoldit eivät lisää tuplainsetejä
- CounterViewModel scopattu `TopLevelDestination.Projects.route`-tasolle (jaettu Counter + ProjectList)
- LibraryViewModel scopattu `TopLevelDestination.Library.route`-tasolle (jaettu Library + alanäytöt)
- Navigaatio: 5 tabia (Projects, Library, Tools, Insights, Settings). Sovellus käynnistyy Projects-tabista. Room v7.
- Voice commands v2: `VoiceCommandHandler` (continuous + one-shot, sealed class `VoiceCommand` with count), `VoiceCommandInterpreter` (Gemini AI fallback), `VoiceResponseManager` (TTS), `AiQuotaManager.hasVoiceQuota()` (50/pv). Tunnistus: exact keyword → counted command (paikallinen EN+FI lukuparseri 1-20) → first-word fallback → Gemini
- Multi-select UI: `SelectionIndicator` ja `SelectModeDeleteBar` jaetut internal composablet `SavedPatternsScreen.kt`:ssä
- Notes: bottom sheet + full-screen editor (`notes_editor/{projectId}`), `NotesEditorViewModel` (debounced auto-save). Full-screen editorin TopAppBarissa `+ AI` -nappi avaa `JournalEntryBottomSheet` (Speak/Type) → `JournalEntryProcessor` (Gemini 2.5 Flash Lite siivoaa välimerkit) → `NotesEditorViewModel.appendJournalEntry()` lisää päivättyyn + rivi-headeriin. Fallback (offline/quota/ei-Pro) säilyttää raakatekstin. Pro-gate: `ProFeature.AI_FEATURES`.
- Simple speech-to-text: `ai/speech/SimpleSpeechRecognizer` — kevyt raakatranskriptio-wrapper (erillään `VoiceCommandHandler`ista joka on kytketty keyword-parsingiin). Journal käyttää tätä.
- Ravelry API: debug lukee avaimet `local.properties`:sta (`ravelry.basicAuthUser`, `ravelry.basicAuthPassword`, `ravelry.oauth2ClientId`, `ravelry.oauth2ClientSecret`) → BuildConfig; release lukee `KNITTOOLS_RAVELRY_*`-ympäristömuuttujista ja vaatii `KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS=true`

## Google Play

- SplashScreen: `installSplashScreen()` ennen `super.onCreate()`, teema `values/themes.xml` (light/dark)
- In-App Review: triggerit Pro-osto tai 20+ laskuritoimintoa, pyydetään kerran (DataStore)
- In-App Updates: flexible mode, checkForUpdate (onCreate), checkDownloadedOnResume (onResume)
- Baseline Profiles: `:baselineprofile`-moduuli, generoi `./gradlew :app:generateBaselineProfile`
