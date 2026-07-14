# KnitTools Deepsec

This is a separate Deepsec workspace for the KnitTools Android app.

Deepsec is not an SCA/CVE replacement. Keep OWASP Dependency-Check for dependency vulnerabilities, and use Deepsec for agent-assisted review of app-specific flows that generic static rules do not understand well.

## Run

From `C:\Dev\KnitTools\.deepsec`:

```powershell
pnpm install
pnpm deepsec:report
```

`pnpm deepsec:report` runs all Deepsec built-in matchers plus KnitTools custom matchers, processes the candidates, revalidates stored findings against the current code, synchronizes the historical Ravelry credential finding state with `config/security-decisions.md`, and exports Markdown findings.

For only KnitTools custom matchers:

```powershell
pnpm deepsec:report:custom
```

For scan-only checks:

```powershell
pnpm deepsec:scan
pnpm deepsec:scan:custom
```

`deepsec export` writes the stored project finding state, not only the candidates from the immediately preceding scan. Keep `deepsec:revalidate` and `scripts/mark-accepted-risks.mjs` in the report scripts so fixed, false-positive, removed-risk, or accepted-risk findings are refreshed before export.

`scripts/mark-accepted-risks.mjs` is intentionally narrow: when `config/security-decisions.md` says the Ravelry embedded credential risk is still accepted, it only marks Ravelry `secrets-exposure` findings in `app/build.gradle.kts`, `RavelryAuthManager.kt`, and `RavelryApiService.kt` as accepted-risk. When the same decision is marked removed from Android, the script first verifies that Android source no longer contains the old Ravelry credential surfaces, then marks the historical findings fixed. If those credential surfaces return while the decision remains removed, the report fails. Phase 9 additionally keeps `tools/release-surface.ps1` aligned with that removed-risk state by allowing only Firebase Auth/Functions/Google Services for the Ravelry backend and scanning locally known Ravelry secret values without printing them.

Generated runtime data such as `.deepsec/data/knittools/project.json`, `.deepsec/data/knittools/tech.json`, `.deepsec/data/knittools/files`, `.deepsec/data/knittools/runs`, `.deepsec/data/knittools/reports`, and `.deepsec/findings` is ignored.
