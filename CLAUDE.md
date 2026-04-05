# Project Instructions

## Build & Test

- `./gradlew assembleDebug` — debug build
- `./gradlew test` — unit tests
- `./gradlew :app:detekt` — static analysis
- `./gradlew lint` — Android lint

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
- No inline `letterSpacing`, `fontSize`, or `fontWeight` overrides — use Type.kt roles. Exception: CounterScreen's main number (96sp ExtraBold)
- Finnish in commit messages and comments

## UI Architecture

- **Navigaatio:** M3 NavigationBar 3 välilehdellä (Projects, Tools, Reference). TopLevelDestination-enum Screen.kt:ssä. Nested NavGraphs per tab.
- **Projects-tab:** CounterScreen (root), ProjectListScreen, SessionHistoryScreen. CounterViewModel scopattu `TopLevelDestination.Projects.route`-tasolle — jaettu kaikkien Projects-tabin näyttöjen kesken.
- **Tools-tab:** Suora laskimilista HubListItem-komponenteilla (Gauge, Increase/Decrease, Cast On, Yarn Estimator) + QuickTipCard. Yarn Card -näytöt Tools-tabin alla. Ei 2×2 gridiä, ei CalculatorsHubia.
- **ToolScreenScaffold:** Puhdas teemapinta (`background`-väri), ei ambient-kuvia. Valinnainen onSettings-callback.
- **Scaffold-taustaväri:** `background` (#F8F4F0 light / #1A1410 dark) kaikissa näytöissä. Ei `surface`-väriä Scaffold-taustana.
- **Värijärjestelmä:** Gold (#C9A96E) primary molemmissa teemoissa, dusty rose secondary. Extended colors KnitToolsExtendedColors-luokassa (surfaceTint, secondaryOutline, onSurfaceMuted, brandWine, inactiveContent). Käytä `MaterialTheme.knitToolsColors` extended-tokeneille.
- **Data:** Room v3 — CounterProjectEntity (sectionName, stitchCount, isCompleted, totalRows, completedAt, yarnCardIds), SessionEntity, CounterHistoryEntity, YarnCardEntity. AutoMigration 1→2→3.
- **Window insets:** `consumeWindowInsets(scaffoldPadding)` NavHostin modifierissa — sisemmät Scaffoldit eivät lisää tuplainsetejä.
- **Drawable-resurssit:** Kaikki kuvat `drawable-nodpi/`-kansiossa (.webp ja .png)
