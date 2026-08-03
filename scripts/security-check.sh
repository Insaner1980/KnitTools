#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CANONICAL_ENTRYPOINT="$ROOT_DIR/tools/sc.ps1"
POWERSHELL_ARGS=()

while (($# > 0)); do
  case "$1" in
    --without-deps) POWERSHELL_ARGS+=("-WithoutDeps") ;;
    --with-deps) ;;
    --full) POWERSHELL_ARGS+=("-Full") ;;
    --history) POWERSHELL_ARGS+=("-History") ;;
    --plan-only) POWERSHELL_ARGS+=("-PlanOnly") ;;
    --resolve-only) POWERSHELL_ARGS+=("-ResolveOnly") ;;
    --init-verification) POWERSHELL_ARGS+=("-InitVerification") ;;
    *)
      printf 'SECURITY_CHECK_ARGUMENT_UNSUPPORTED: %s\n' "$1" >&2
      exit 2
      ;;
  esac
  shift
done

if [[ ! -f "$CANONICAL_ENTRYPOINT" ]]; then
  printf 'SECURITY_CHECK_ENTRYPOINT_MISSING: %s\n' "$CANONICAL_ENTRYPOINT" >&2
  exit 2
fi

if command -v pwsh >/dev/null 2>&1; then
  exec pwsh -NoProfile -File "$CANONICAL_ENTRYPOINT" "${POWERSHELL_ARGS[@]}"
fi

if command -v powershell.exe >/dev/null 2>&1; then
  if command -v cygpath >/dev/null 2>&1; then
    CANONICAL_ENTRYPOINT="$(cygpath -w "$CANONICAL_ENTRYPOINT")"
  fi
  exec powershell.exe -NoProfile -File "$CANONICAL_ENTRYPOINT" "${POWERSHELL_ARGS[@]}"
fi

if command -v powershell >/dev/null 2>&1; then
  exec powershell -NoProfile -File "$CANONICAL_ENTRYPOINT" "${POWERSHELL_ARGS[@]}"
fi

printf 'SECURITY_CHECK_POWERSHELL_MISSING: pwsh tai powershell ei ole PATHissa.\n' >&2
exit 2
