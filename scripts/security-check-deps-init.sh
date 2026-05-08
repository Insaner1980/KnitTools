#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec env \
  DEPENDENCY_CHECK_AUTO_UPDATE=true \
  DEPENDENCY_CHECK_TIMEOUT_SECONDS=0 \
  DEPENDENCY_CHECK_TASK=:app:dependencyCheckUpdate \
  "$ROOT_DIR/scripts/security-check.sh" --with-deps "$@"
