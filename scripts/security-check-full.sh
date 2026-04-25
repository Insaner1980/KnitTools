#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec env \
  DEPENDENCY_CHECK_AUTO_UPDATE="${DEPENDENCY_CHECK_AUTO_UPDATE:-false}" \
  DEPENDENCY_CHECK_TIMEOUT_SECONDS="${DEPENDENCY_CHECK_TIMEOUT_SECONDS:-0}" \
  "$ROOT_DIR/scripts/security-check.sh" --with-deps "$@"
