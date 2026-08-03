#!/usr/bin/env bash
set -euo pipefail

PROJECT_ARG="${1:-.}"
if (($# > 0)); then
  shift
fi
PROJECT_DIR="$(cd "$PROJECT_ARG" && pwd)"
REPO_LOCAL_SCRIPT="$PROJECT_DIR/scripts/security-check-full.sh"

if [[ ! -f "$REPO_LOCAL_SCRIPT" ]]; then
  printf 'SECURITY_CHECK_ENTRYPOINT_MISSING: %s\n' "$REPO_LOCAL_SCRIPT" >&2
  exit 2
fi

exec "$REPO_LOCAL_SCRIPT" "$@"
