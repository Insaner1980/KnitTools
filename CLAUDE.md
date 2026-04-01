# Project Instructions

## Build & Test

- `./gradlew assembleDebug` — debug build
- `./gradlew test` — unit tests
- `./gradlew detektMain` — static analysis
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
- No hardcoded colors/dimensions — use theme tokens
- Finnish in commit messages and comments
