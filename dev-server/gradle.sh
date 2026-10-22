#!/usr/bin/env bash
set -euo pipefail
source "$(dirname -- "$0")/common.sh"
ctd_java25
cd "$CTD_ROOT"
exec bash ./gradlew "$@"
