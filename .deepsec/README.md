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

`scripts/mark-accepted-risks.mjs` applies the decision in `config/security-decisions.md` to the three original Ravelry `secrets-exposure` findings. It matches each record by file path, finding ID, and originating run. Under the current decision, it marks those findings fixed after checking the Android source for the old credential surfaces. If one of those surfaces returns, the report fails before the script updates the stored findings. `tools/release-surface.ps1` also checks for reintroduced Ravelry credentials and rejects Android dependencies on Firebase Auth and Functions.

Generated runtime data such as `.deepsec/data/knittools/project.json`, `.deepsec/data/knittools/tech.json`, `.deepsec/data/knittools/files`, `.deepsec/data/knittools/runs`, `.deepsec/data/knittools/reports`, and `.deepsec/findings` is ignored.
