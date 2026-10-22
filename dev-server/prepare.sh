#!/usr/bin/env bash
set -euo pipefail
source "$(dirname -- "$0")/common.sh"
ctd_lock
ctd_java25
cd "$CTD_ROOT"
bash ./gradlew stageLocalPlugin --console=plain
python3 "$CTD_ROOT/dev-server/prepare.py"
