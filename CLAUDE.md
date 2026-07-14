# Project Instructions

## Build & Test

- `./gradlew assembleDebug` — debug build
- `./gradlew test` — unit tests
- `./gradlew :app:detekt` — static analysis
- `./gradlew lint` — Android lint
- `./gradlew :app:generateBaselineProfile` — generoi Baseline Profile yhdistetyllä laitteella

## Quality Tools (global, in ~/bin/)

- `lint-check` (alias `lc`) — runs ktlint + detekt + Android lint, results in `reports/`
- `security-check` (alias `sc`) — global wrapper; repo-local authoritative implementation is `scripts/security-check.sh`
- `security-check-full` (alias `sc-full`) — compatibility alias täydelle security-ajolle
- Don't run these scripts yourself — user runs them via `! lc` / `! sc`
- `reports/` is gitignored, never commit it
- Security risk decisions and temporary exceptions are documented in `config/security-decisions.md`
- `sc` käyttää tässä repossa paikallisia Semgrep-sääntöjä ja ajaa myös OWASP dependency-checkin.
- Dependency-checkin voi ohittaa vain erikseen: `DEPENDENCY_CHECK_ENABLED=false sc`
- Ensimmäinen OWASP dependency-check -ajo voi olla hidas, koska se alustaa CVE-tietokannan automaattisesti. `NVD_API_KEY` nopeuttaa NVD-päivitystä, jos sellainen on käytössä.
- `sonar` ajaa projektin SonarCloud-skannauksen Gradlen `sonar`-taskilla, joka ajaa ensin `:app:jacocoDebugUnitTestReport`-tehtävän, ja kirjoittaa lokin `reports/sonar.txt`; `sonar auth login/status/...` ohjautuu edelleen SonarQube CLI:lle.
- SonarCloud-skannaus tarvitsee `SONAR_TOKEN`-ympäristömuuttujan. SonarQube CLI:n keychain-kirjautumista käytetään issueiden lukemiseen `reports/sonar-issues.json`-raporttiin.

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
- Navigaatio: 5 tabia (Projects, Library, Tools, Insights, Settings). Sovellus käynnistyy Projects-tabista. Room v14.
- Voice commands: `VoiceCommandHandler`, `VoiceCommandParser` ja `VoiceResponseManager` ovat paikallinen SpeechRecognizer/TTS/keyword-putki. Ei Gemini-fallbackia, AI-kiintiötä tai keskustelevaa voice-flow'ta.
- Multi-select UI: `SelectionIndicator` ja `SelectModeDeleteBar` jaetut internal composablet `SavedPatternsScreen.kt`:ssä
- Notes: bottom sheet + full-screen editor (`notes_editor/{projectId}`), `NotesEditorViewModel` ja paikallinen auto-save. Project note replacement kulkee `CounterRepository.saveProjectNotes`-polun kautta, jotta rinnakkaiset editorivirrat eivät ylikirjoita toisiaan.
- Notes editing is local-only; älä palauta cloud-journalia, AI-siistintää tai voice-transcript-journalointia ilman uutta nimenomaista päätöstä.
- Ravelry API: vanha backenditön päätös on superseded. Ravelry-secretit, token exchange ja tokenit eivät elä Androidissa; Android käyttää Firebase Auth + Cloud Functions -backend-rajaa `RavelryBackendClient`in kautta. `RavelryAuthManager` omistaa backend-auth-tilan, start/status/disconnect-kutsut ja token-free `knittools://ravelry-auth-complete` callbackin; auth avataan Auth Tabilla ja Custom Tabs jää fallbackiksi. Saved patterns ovat Room schema 14 -lähdemetadatassa (`SavedPatternSource`, `ravelryPatternId`, canonical/original URL ja paikallinen PDF-URI). Seuraava vaihe on URL/share-import ja saved-pattern UX.

## Google Play

- SplashScreen: `installSplashScreen()` ennen `super.onCreate()`, teema `values/themes.xml` (light/dark)
- In-App Review: triggerit Pro-osto tai 20+ laskuritoimintoa, pyydetään kerran (DataStore)
- In-App Updates: flexible mode, checkForUpdate (onCreate), checkDownloadedOnResume (onResume)
- Baseline Profiles: `:baselineprofile`-moduuli, generoi `./gradlew :app:generateBaselineProfile`
