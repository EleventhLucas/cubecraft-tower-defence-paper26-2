#!/usr/bin/env bash
set -euo pipefail
source "$(dirname -- "$0")/common.sh"
ctd_java25
ctd_lock
if [[ ! -f "$CTD_SERVER_DIR/paper.jar" || ! -f "$CTD_SERVER_DIR/plugins/CubeCraftTowerDefence.jar" ]]; then
  echo 'Run bash dev-server/prepare.sh first, or use the VS Code launch configuration.' >&2
  exit 1
fi
if ! grep -Eq '^eula=true\s*$' "$CTD_SERVER_DIR/eula.txt" 2>/dev/null; then
  echo 'Minecraft EULA: https://www.minecraft.net/en-us/eula'
  read -r -p 'Accept the Minecraft EULA for this local CubeCraft test server? Type yes: ' ctd_accept
  [[ "$ctd_accept" == "yes" ]] || { echo 'Server not started.'; exit 1; }
  printf 'eula=true\n' > "$CTD_SERVER_DIR/eula.txt"
fi
python3 "$CTD_ROOT/dev-server/initialize.py"
echo 'Connect with Minecraft Java 26.2 at 127.0.0.1:25566 (unless changed in server/server.properties).'
echo 'Use /ctdfreeplay for solo testing, /ctdinstall visit to preview, or /ctdjoin for 1v1. Enter stop here to shut down.'
cd "$CTD_SERVER_DIR"
exec "$JAVA_HOME/bin/java" -Xms1G -Xmx4G -jar paper.jar --nogui
