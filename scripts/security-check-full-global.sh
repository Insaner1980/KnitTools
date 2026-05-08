#!/usr/bin/env bash
# security-check-full — globaali wrapper täydelle security-ajolle.
# Käyttö: security-check-full [polku]

set -uo pipefail

PROJECT_ARG="${1:-.}"
PROJECT_DIR="$(cd "$PROJECT_ARG" && pwd)"
REPO_LOCAL_SCRIPT="$PROJECT_DIR/scripts/security-check-full.sh"

if [[ -x "$REPO_LOCAL_SCRIPT" ]]; then
    exec "$REPO_LOCAL_SCRIPT"
fi

DEPENDENCY_CHECK_ENABLED=true exec /home/emma/bin/security-check "$PROJECT_DIR"
