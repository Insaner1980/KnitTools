# KnitTools Deepsec

This is a separate Deepsec workspace for the KnitTools Android app.

Deepsec is not an SCA/CVE replacement. Keep OWASP Dependency-Check for dependency vulnerabilities, and use Deepsec for agent-assisted review of app-specific flows that generic static rules do not understand well.

## Run

From `C:\Dev\KnitTools\.deepsec`:

```powershell
pnpm install
pnpm deepsec:report
```

`pnpm deepsec:report` runs all Deepsec built-in matchers plus KnitTools custom matchers, processes the candidates, and exports Markdown findings.

For only KnitTools custom matchers:

```powershell
pnpm deepsec:report:custom
```

For scan-only checks:

```powershell
pnpm deepsec:scan
pnpm deepsec:scan:custom
```

Generated runtime data such as `.deepsec/data/knittools/project.json`, `.deepsec/data/knittools/tech.json`, `.deepsec/data/knittools/files`, `.deepsec/data/knittools/runs`, `.deepsec/data/knittools/reports`, and `.deepsec/findings` is ignored.
