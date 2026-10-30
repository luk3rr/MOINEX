#!/usr/bin/env bash
# Generates one PNG per screen and theme with the development screenshot runner.
# Usage: scripts/capture-screens.sh <output-dir>   (CAPTURE_ONLY=home,wallets limits the screens)
# The app runs with a throwaway user.home and a dummy database, never with the real data.
set -euo pipefail

if [ $# -ne 1 ]; then
    echo "Usage: scripts/capture-screens.sh <output-dir>" >&2
    exit 1
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$1"
OUT_DIR="$(cd "$1" && pwd)"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
JAVA_BIN="$JAVA_HOME/bin/java"
JAR="$ROOT/build/libs/moinex.jar"
FAKE_HOME="$(mktemp -d -t moinex-capture-XXXXXX)"
DB_PATH="$FAKE_HOME/.moinex/data/moinex.db"
BOOTSTRAP_LOG="$FAKE_HOME/bootstrap.log"
JAVA_OPTS=(--enable-native-access=ALL-UNNAMED "-Duser.home=$FAKE_HOME")

cleanup() {
    if [ -n "${BOOTSTRAP_PID:-}" ] && kill -0 "$BOOTSTRAP_PID" 2>/dev/null; then
        kill "$BOOTSTRAP_PID" 2>/dev/null || true
    fi
    rm -rf "$FAKE_HOME"
}
trap cleanup EXIT

echo "[1/4] Building the jar"
(cd "$ROOT" && JAVA_HOME="$JAVA_HOME" ./gradlew bootJar --console=plain -q)

echo "[2/4] Creating the migrated database in $FAKE_HOME"
"$JAVA_BIN" "${JAVA_OPTS[@]}" -jar "$JAR" >"$BOOTSTRAP_LOG" 2>&1 &
BOOTSTRAP_PID=$!
for _ in $(seq 1 90); do
    if grep -q "Successfully applied" "$BOOTSTRAP_LOG" 2>/dev/null; then
        break
    fi
    sleep 1
done
if ! grep -q "Successfully applied" "$BOOTSTRAP_LOG"; then
    echo "The database was not migrated; see $BOOTSTRAP_LOG" >&2
    exit 1
fi
sleep 3
kill "$BOOTSTRAP_PID" 2>/dev/null || true
wait "$BOOTSTRAP_PID" 2>/dev/null || true
BOOTSTRAP_PID=""

echo "[3/4] Loading the dummy data"
python3 "$ROOT/scripts/generate_dummy_data.py" --db_path "$DB_PATH"

echo "[4/4] Capturing the screens into $OUT_DIR"
"$JAVA_BIN" "${JAVA_OPTS[@]}" "-Dmoinex.capture.dir=$OUT_DIR" "-Dmoinex.capture.only=${CAPTURE_ONLY:-}" -jar "$JAR"

echo "Done: $(find "$OUT_DIR" -name '*.png' | wc -l) images in $OUT_DIR"
