#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORTS_DIR="$ROOT_DIR/reports"
SEM_GREP_TEXT_REPORT="$REPORTS_DIR/security-code.txt"
SEM_GREP_JSON_REPORT="$REPORTS_DIR/security-code.json"
DEP_TEXT_REPORT="$REPORTS_DIR/security-deps.txt"
DEP_RAW_REPORT="$REPORTS_DIR/security-deps-raw.txt"
SEM_GREP_REMOTE_TIMEOUT_SECONDS="${SEM_GREP_REMOTE_TIMEOUT_SECONDS:-60}"
SEM_GREP_USE_REMOTE_CONFIGS="${SEM_GREP_USE_REMOTE_CONFIGS:-false}"
GRADLE_USER_HOME_DIR="$REPORTS_DIR/.gradle-home"
DEPENDENCY_CHECK_DATA_DIR="$GRADLE_USER_HOME_DIR/dependency-check-data"
DEPENDENCY_CHECK_DB_FILE="$DEPENDENCY_CHECK_DATA_DIR/11.0/odc.mv.db"
DEPENDENCY_CHECK_ENABLED="${DEPENDENCY_CHECK_ENABLED:-false}"
DEPENDENCY_CHECK_REQUIRE_NVD_API_KEY="${DEPENDENCY_CHECK_REQUIRE_NVD_API_KEY:-true}"
DEPENDENCY_CHECK_TIMEOUT_SECONDS="${DEPENDENCY_CHECK_TIMEOUT_SECONDS:-900}"
DEPENDENCY_CHECK_AUTO_UPDATE="${DEPENDENCY_CHECK_AUTO_UPDATE:-false}"
DEPENDENCY_CHECK_TASK="${DEPENDENCY_CHECK_TASK:-:app:dependencyCheckAnalyze}"

mkdir -p "$REPORTS_DIR"

summarize_dependency_check_report() {
  python3 - <<'PY' "$ROOT_DIR/reports/dependency-check-report.json" "$DEP_TEXT_REPORT" "$DEP_RAW_REPORT"
import json
import sys
from collections import defaultdict
from pathlib import Path

json_path = Path(sys.argv[1])
text_path = Path(sys.argv[2])
raw_path = Path(sys.argv[3])

existing_lines = text_path.read_text().splitlines() if text_path.exists() else []

if not json_path.exists():
    summary_lines = [
        "Yhteenvetoa ei voitu muodostaa: dependency-check-report.json puuttuu.",
        f"Raakaloki: {raw_path}",
    ]
    text_path.write_text("\n".join(existing_lines + summary_lines) + "\n")
    print("\n".join(summary_lines))
    raise SystemExit(0)

data = json.loads(json_path.read_text())
package_vulns = defaultdict(set)

for dependency in data.get("dependencies", []):
    vulns = dependency.get("vulnerabilities") or []
    if not vulns:
        continue
    vuln_ids = {item.get("name", "?") for item in vulns}
    packages = [pkg.get("id") for pkg in dependency.get("packages", []) if pkg.get("id")]
    if not packages:
        packages = [dependency.get("fileName", "?")]
    for package_id in packages:
        package_vulns[package_id].update(vuln_ids)

summary_lines = []
if not package_vulns:
    summary_lines.append("Ei löydöksiä.")
else:
    total_packages = len(package_vulns)
    total_vulns = sum(len(vulns) for vulns in package_vulns.values())
    summary_lines.append(f"Löydöksiä: {total_packages} pakettia, {total_vulns} yksilöllistä CVE-viittausta")
    for package_id in sorted(package_vulns):
        cves = ", ".join(sorted(package_vulns[package_id]))
        summary_lines.append(f"- {package_id}: {cves}")

summary_lines.append(f"Raakaloki: {raw_path}")
text_path.write_text("\n".join(existing_lines + summary_lines) + "\n")
print("\n".join(summary_lines))
PY
}

run_semgrep() {
  if ! command -v semgrep >/dev/null 2>&1; then
    printf 'semgrep ei ole asennettu. Ohitetaan koodiskannaus.\n' | tee "$SEM_GREP_TEXT_REPORT"
    return 0
  fi

  local config_file="$ROOT_DIR/config/semgrep/knittools-security.yml"
  local semgrep_home="$REPORTS_DIR/.semgrep-home"
  local common_args=(
    scan
    --metrics=off
    --quiet
    --json
    --config "$config_file"
    --exclude app/build
    --exclude baselineprofile/build
    --exclude build
    --exclude reports
  )
  local -a remote_configs=()
  local -a active_args=("${common_args[@]}")
  local status=0
  local remote_status=0

  mkdir -p "$semgrep_home"
  mkdir -p "$GRADLE_USER_HOME_DIR"

  printf '== Semgrep security scan ==\n' | tee "$SEM_GREP_TEXT_REPORT"
  if [[ "$SEM_GREP_USE_REMOTE_CONFIGS" == "true" ]]; then
    remote_configs=(
      --config p/secrets
    )
    active_args=("${common_args[@]}" "${remote_configs[@]}")
    (
      cd "$ROOT_DIR"
      HOME="$semgrep_home" timeout "${SEM_GREP_REMOTE_TIMEOUT_SECONDS}s" semgrep "${active_args[@]}" --disable-version-check --output "$SEM_GREP_JSON_REPORT"
    )
    remote_status=$?
    if [[ $remote_status -eq 1 ]]; then
      remote_status=0
    fi
    if [[ $remote_status -ne 0 ]]; then
      printf 'Semgrep remote-configit eivät olleet käytettävissä. Ajetaan fallback vain paikallisilla säännöillä.\n' | tee -a "$SEM_GREP_TEXT_REPORT"
      active_args=("${common_args[@]}")
    else
      status=0
    fi
  fi

  if [[ $remote_status -ne 0 || "$SEM_GREP_USE_REMOTE_CONFIGS" != "true" ]]; then
    (
      cd "$ROOT_DIR"
      HOME="$semgrep_home" semgrep "${active_args[@]}" --disable-version-check --output "$SEM_GREP_JSON_REPORT"
    )
    status=$?
    if [[ $status -eq 1 ]]; then
      status=0
    fi
  fi

  python3 - <<'PY' "$SEM_GREP_JSON_REPORT" "$SEM_GREP_TEXT_REPORT"
import json
import sys
from pathlib import Path

json_path = Path(sys.argv[1])
text_path = Path(sys.argv[2])

data = json.loads(json_path.read_text())
results = data.get("results", [])

lines = ["== Semgrep security scan =="]
if not results:
    lines.append("Ei löydöksiä.")
else:
    lines.append(f"Löydöksiä: {len(results)}")
    for item in results:
        path = item.get("path", "?")
        start = (((item.get("start") or {}).get("line")) or "?")
        check_id = item.get("check_id", "?")
        message = item.get("extra", {}).get("message", "").strip()
        lines.append(f"- {path}:{start} [{check_id}] {message}")

text_path.write_text("\n".join(lines) + "\n")
PY

  cat "$SEM_GREP_TEXT_REPORT"

  return "$status"
}

run_dependency_check() {
  printf '== Gradle dependency-check ==\n' | tee "$DEP_TEXT_REPORT"
  : > "$DEP_RAW_REPORT"

  if [[ "$DEPENDENCY_CHECK_ENABLED" != "true" ]]; then
    printf 'Ohitetaan dependency-check oletuksena, jotta `sc` pysyy nopeana.\n' | tee -a "$DEP_TEXT_REPORT"
    printf 'Aja tarvittaessa täysi riippuvuusskannaus: DEPENDENCY_CHECK_ENABLED=true sc\n' | tee -a "$DEP_TEXT_REPORT"
    return 0
  fi

  if [[ "$DEPENDENCY_CHECK_REQUIRE_NVD_API_KEY" == "true" && -z "${NVD_API_KEY:-}" ]]; then
    printf 'Ohitetaan dependency-check: NVD_API_KEY puuttuu ja hidas täyspäivitys on estetty oletuksena.\n' | tee -a "$DEP_TEXT_REPORT"
    printf 'Aja tarvittaessa: NVD_API_KEY=... sc\n' | tee -a "$DEP_TEXT_REPORT"
    return 0
  fi

  if [[ "$DEPENDENCY_CHECK_AUTO_UPDATE" != "true" && ! -f "$DEPENDENCY_CHECK_DB_FILE" ]]; then
    printf 'Dependency-checkin paikallinen CVE-tietokanta puuttuu.\n' | tee -a "$DEP_TEXT_REPORT"
    printf 'Alusta se kerran erikseen komennolla: ./scripts/security-check-deps-init.sh\n' | tee -a "$DEP_TEXT_REPORT"
    printf 'Sen jälkeen `sc-full` käyttää paikallista cachea eikä jää pitkäksi aikaa lataamaan NVD-dataa.\n' | tee -a "$DEP_TEXT_REPORT"
    return 0
  fi

  if [[ "$DEPENDENCY_CHECK_AUTO_UPDATE" == "true" ]]; then
    printf 'Ensimmäinen NVD-päivitys voi kestää pitkään. Tämä komento alustaa paikallisen CVE-tietokannan.\n' | tee -a "$DEP_TEXT_REPORT"
  else
    printf 'Käytetään paikallista dependency-check-cachea ilman verkkopäivitystä.\n' | tee -a "$DEP_TEXT_REPORT"
  fi

  local status=0
  set +e
  if [[ "$DEPENDENCY_CHECK_TIMEOUT_SECONDS" == "0" ]]; then
    (
      cd "$ROOT_DIR"
      GRADLE_USER_HOME="$GRADLE_USER_HOME_DIR" \
        DEPENDENCY_CHECK_AUTO_UPDATE="$DEPENDENCY_CHECK_AUTO_UPDATE" \
        ./gradlew --no-daemon "$DEPENDENCY_CHECK_TASK" --no-configuration-cache --console=plain
    ) >"$DEP_RAW_REPORT" 2>&1
  else
    (
      cd "$ROOT_DIR"
      GRADLE_USER_HOME="$GRADLE_USER_HOME_DIR" \
        DEPENDENCY_CHECK_AUTO_UPDATE="$DEPENDENCY_CHECK_AUTO_UPDATE" \
        timeout "${DEPENDENCY_CHECK_TIMEOUT_SECONDS}s" ./gradlew --no-daemon "$DEPENDENCY_CHECK_TASK" --no-configuration-cache --console=plain
    ) >"$DEP_RAW_REPORT" 2>&1
  fi
  status=$?
  set -e

  if [[ $status -eq 0 ]]; then
    summarize_dependency_check_report
    return 0
  fi

  if rg -q "Unable to obtain the update lock|old transaction with the same id|MVStoreException|odc\\.mv\\.db" \
    "$DEP_TEXT_REPORT" \
    "$DEP_RAW_REPORT" \
    "$DEPENDENCY_CHECK_DATA_DIR" 2>/dev/null; then
    printf 'Dependency-checkin paikallinen data oli lukossa tai vioittunut. Tyhjennetään cache ja yritetään kerran uudelleen.\n' | tee -a "$DEP_TEXT_REPORT"
    rm -rf "$DEPENDENCY_CHECK_DATA_DIR"
    mkdir -p "$DEPENDENCY_CHECK_DATA_DIR"
    if [[ "$DEPENDENCY_CHECK_AUTO_UPDATE" != "true" ]]; then
      printf 'Täysi verkkopäivitys on pois päältä, joten uusi cache pitää alustaa erikseen komennolla: ./scripts/security-check-deps-init.sh\n' | tee -a "$DEP_TEXT_REPORT"
      return 0
    fi
    set +e
    if [[ "$DEPENDENCY_CHECK_TIMEOUT_SECONDS" == "0" ]]; then
      (
        cd "$ROOT_DIR"
        GRADLE_USER_HOME="$GRADLE_USER_HOME_DIR" \
          DEPENDENCY_CHECK_AUTO_UPDATE="$DEPENDENCY_CHECK_AUTO_UPDATE" \
          ./gradlew --no-daemon "$DEPENDENCY_CHECK_TASK" --no-configuration-cache --console=plain
      ) >>"$DEP_RAW_REPORT" 2>&1
    else
      (
        cd "$ROOT_DIR"
        GRADLE_USER_HOME="$GRADLE_USER_HOME_DIR" \
          DEPENDENCY_CHECK_AUTO_UPDATE="$DEPENDENCY_CHECK_AUTO_UPDATE" \
          timeout "${DEPENDENCY_CHECK_TIMEOUT_SECONDS}s" ./gradlew --no-daemon "$DEPENDENCY_CHECK_TASK" --no-configuration-cache --console=plain
      ) >>"$DEP_RAW_REPORT" 2>&1
    fi
    status=$?
    set -e
  fi

  if [[ $status -eq 0 ]]; then
    summarize_dependency_check_report
  else
    printf 'Dependency-check epäonnistui. Raakaloki: %s\n' "$DEP_RAW_REPORT" | tee -a "$DEP_TEXT_REPORT"
  fi

  return "$status"
}

main() {
  local exit_code=0

  if [[ "${1:-}" == "--with-deps" ]]; then
    DEPENDENCY_CHECK_ENABLED=true
    shift
  fi

  printf 'Raportit kirjoitetaan hakemistoon %s\n' "$REPORTS_DIR"
  if ! run_semgrep; then
    printf 'Semgrep-skannaus epäonnistui. Tarkista %s.\n' "$SEM_GREP_TEXT_REPORT" | tee -a "$SEM_GREP_TEXT_REPORT"
    exit_code=1
  fi

  if ! run_dependency_check; then
    printf 'Dependency-check epäonnistui. Tarkista %s.\n' "$DEP_TEXT_REPORT" | tee -a "$DEP_TEXT_REPORT"
    exit_code=1
  fi

  printf 'Valmis. Katso %s, %s ja dependency-checkin JSON/HTML-raportit hakemistosta %s.\n' \
    "$SEM_GREP_TEXT_REPORT" \
    "$DEP_TEXT_REPORT" \
    "$REPORTS_DIR"

  exit "$exit_code"
}

main "$@"
