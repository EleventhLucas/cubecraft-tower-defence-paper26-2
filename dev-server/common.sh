#!/usr/bin/env bash
# Sourced by the local build/server commands.
CTD_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
CTD_SERVER_DIR="$CTD_ROOT/server"

ctd_java25() {
  local candidate version
  local candidates=()
  [[ -z "${JAVA25_HOME:-}" ]] || candidates+=("$JAVA25_HOME/bin/java")
  [[ -z "${JAVA_HOME:-}" ]] || candidates+=("$JAVA_HOME/bin/java")
  candidate="$(command -v java || true)"
  [[ -z "$candidate" ]] || candidates+=("$candidate")
  candidates+=("$HOME"/.gradle/jdks/*/bin/java /usr/lib/jvm/*/bin/java)
  for candidate in "${candidates[@]}"; do
    [[ -x "$candidate" && -x "$(dirname "$candidate")/javac" ]] || continue
    version="$("$candidate" -version 2>&1)" || continue
    if [[ "$version" =~ version\ \"25[.\"] ]]; then
      candidate="$(readlink -f "$candidate")"
      export JAVA_HOME="$(dirname "$(dirname "$candidate")")"
      export PATH="$JAVA_HOME/bin:$PATH"
      echo "Using Java 25: $JAVA_HOME"
      return
    fi
  done
  echo 'A Java 25 JDK is required. Set JAVA25_HOME to its installation directory.' >&2
  return 1
}

ctd_lock() {
  mkdir -p "$CTD_SERVER_DIR"
  exec 9>"$CTD_SERVER_DIR/.launch.lock"
  flock -n 9 || {
    echo 'CubeCraft TD is already running or being prepared. Enter stop in its terminal first.' >&2
    return 1
  }
}
