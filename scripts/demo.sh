#!/usr/bin/env bash
# Opens MOINEX on a throwaway database filled with mock data, for screenshots and demos.
# Usage: scripts/demo.sh   (normally run through `make demo`)
# The app runs with a temporary user.home, so the real ~/.moinex database is never read or changed.
# The temporary directory is removed when the window is closed.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
JAVA_BIN="$JAVA_HOME/bin/java"
JAR="$ROOT/build/libs/moinex.jar"
DEMO_HOME="$(mktemp -d -t moinex-demo-XXXXXX)"
DB_PATH="$DEMO_HOME/.moinex/data/moinex.db"
BOOTSTRAP_LOG="$DEMO_HOME/bootstrap.log"
APP_LOG="$DEMO_HOME/.local/state/moinex/moinex.log"
JAVA_OPTS=(--enable-native-access=ALL-UNNAMED "-Duser.home=$DEMO_HOME")

migrated() {
    grep -qs "Successfully applied" "$BOOTSTRAP_LOG" "$APP_LOG"
}

cleanup() {
    if [ -n "${BOOTSTRAP_PID:-}" ] && kill -0 "$BOOTSTRAP_PID" 2>/dev/null; then
        kill "$BOOTSTRAP_PID" 2>/dev/null || true
    fi
    rm -rf "$DEMO_HOME"
}
trap cleanup EXIT

if [ ! -f "$JAR" ]; then
    echo "The jar is missing: $JAR (run make jar first)" >&2
    exit 1
fi

echo "[1/3] Creating the migrated temporary database in $DEMO_HOME"
"$JAVA_BIN" "${JAVA_OPTS[@]}" -jar "$JAR" >"$BOOTSTRAP_LOG" 2>&1 &
BOOTSTRAP_PID=$!
for _ in $(seq 1 90); do
    if migrated; then
        break
    fi
    sleep 1
done
if ! migrated; then
    echo "The database was not migrated; see $BOOTSTRAP_LOG and $APP_LOG" >&2
    exit 1
fi
sleep 3
kill "$BOOTSTRAP_PID" 2>/dev/null || true
wait "$BOOTSTRAP_PID" 2>/dev/null || true
BOOTSTRAP_PID=""

echo "[2/3] Loading the mock data"
python3 "$ROOT/scripts/generate_dummy_data.py" --db_path "$DB_PATH"

echo "[3/3] Opening MOINEX (close the window to discard the demo data)"
"$JAVA_BIN" "${JAVA_OPTS[@]}" -jar "$JAR"
