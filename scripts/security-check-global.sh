#!/usr/bin/env bash
# security-check — globaali wrapper joka suosii repo-local toteutusta.
# Käyttö: security-check [polku]

set -uo pipefail

PROJECT_ARG="${1:-.}"
PROJECT_DIR="$(cd "$PROJECT_ARG" && pwd)"
REPORTS_DIR="$PROJECT_DIR/reports"
REPO_LOCAL_SCRIPT="$PROJECT_DIR/scripts/security-check.sh"

if [[ -x "$REPO_LOCAL_SCRIPT" ]]; then
    exec "$REPO_LOCAL_SCRIPT"
fi

mkdir -p "$REPORTS_DIR"

echo "=== security-check: $PROJECT_DIR ==="
echo ""

SEMGREP_REPORT="$REPORTS_DIR/security-code.txt"

if command -v semgrep &>/dev/null; then
    echo "[semgrep] Ajetaan tietoturva-analyysi..."

    semgrep scan "$PROJECT_DIR" \
        --config auto \
        --exclude "build" \
        --exclude ".gradle" \
        --exclude "reports" \
        --text \
        --quiet \
        >"$SEMGREP_REPORT" 2>&1 || true

    if [[ -s "$SEMGREP_REPORT" ]] && grep -qE "findings|vulnerability|issue" "$SEMGREP_REPORT" 2>/dev/null; then
        SEMGREP_COUNT=$(grep -cE "^.*:.*:" "$SEMGREP_REPORT" 2>/dev/null || echo "?")
        echo "[semgrep] $SEMGREP_COUNT löydöstä → $SEMGREP_REPORT"
    else
        echo "[semgrep] Ei löydöksiä"
        echo "Ei löydöksiä." > "$SEMGREP_REPORT"
    fi
else
    echo "[semgrep] Ohitetaan — semgrep puuttuu"
fi

echo ""

DEPS_REPORT="$REPORTS_DIR/security-deps.txt"

if command -v dependency-check &>/dev/null; then
    echo "[dep-check] Tarkistetaan riippuvuuksien haavoittuvuudet (CVE)..."
    echo "            (ensimmäinen ajo lataa NVD-tietokannan, voi kestää muutaman minuutin)"

    DC_ARGS=(--scan "$PROJECT_DIR" --exclude "**/build/**" --exclude "**/.gradle/**"
             --format JSON --out "$REPORTS_DIR" --project "$(basename "$PROJECT_DIR")"
             --disableAssembly --log /tmp/dep-check.log)

    if [[ -n "${NVD_API_KEY:-}" ]]; then
        DC_ARGS+=(--nvdApiKey "$NVD_API_KEY")
    fi

    dependency-check "${DC_ARGS[@]}" 2>/dev/null || true

    DC_JSON="$REPORTS_DIR/dependency-check-report.json"
    if [[ -f "$DC_JSON" ]]; then
        python3 -c "
import json
with open('$DC_JSON') as f:
    data = json.load(f)
deps = data.get('dependencies', [])
vulns = [d for d in deps if d.get('vulnerabilities')]
print(f'Skannattu: {len(deps)} riippuvuutta')
print(f'Haavoittuvia: {len(vulns)}')
for d in vulns:
    print(f\"\\n--- {d['fileName']} ---\")
    for v in d['vulnerabilities']:
        sev = v.get('severity', '?')
        name = v.get('name', '?')
        print(f'  {sev}: {name}')
if not vulns:
    print('Ei tunnettuja haavoittuvuuksia.')
" > "$DEPS_REPORT" 2>/dev/null || echo "Raportin parsinta epäonnistui." > "$DEPS_REPORT"

        VULN_COUNT=$(grep -c "^---" "$DEPS_REPORT" 2>/dev/null || true)
        VULN_COUNT="${VULN_COUNT:-0}"
        if [[ "$VULN_COUNT" -gt 0 ]]; then
            echo "[dep-check] $VULN_COUNT haavoittuvaa riippuvuutta → $DEPS_REPORT"
        else
            echo "[dep-check] Ei tunnettuja haavoittuvuuksia"
        fi
    else
        echo "[dep-check] Raporttia ei luotu (tarkista /tmp/dep-check.log)"
        echo "Raporttia ei luotu." > "$DEPS_REPORT"
    fi
else
    echo "[dep-check] Ohitetaan — dependency-check puuttuu"
fi

echo ""
echo "=== Valmis. Tulokset: $REPORTS_DIR/ ==="
