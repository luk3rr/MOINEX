#!/usr/bin/env bash
# Measures how long the main window takes to appear, over several runs, with a throwaway user.home.
# Usage: scripts/measure-startup.sh [runs]
# Needs xwininfo and a graphical session. Prints one line per run and the average, in seconds.
set -euo pipefail

RUNS="${1:-5}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
JAR="$ROOT/build/libs/moinex.jar"
TIMEOUT_SECONDS=90
total=0

if [ ! -f "$JAR" ]; then
    (cd "$ROOT" && JAVA_HOME="$JAVA_HOME" ./gradlew bootJar --console=plain -q)
fi

for run in $(seq 1 "$RUNS"); do
    fake_home="$(mktemp -d -t moinex-startup-XXXXXX)"
    "$JAVA_HOME/bin/java" --enable-native-access=ALL-UNNAMED "-Duser.home=$fake_home" -jar "$JAR" >/dev/null 2>&1 &
    pid=$!
    start=$(date +%s.%N)
    elapsed=""
    while kill -0 "$pid" 2>/dev/null; do
        if xwininfo -root -tree 2>/dev/null | grep -q '"Moinex"'; then
            elapsed=$(echo "$(date +%s.%N) - $start" | bc -l)
            break
        fi
        now=$(echo "$(date +%s.%N) - $start" | bc -l)
        if [ "$(echo "$now > $TIMEOUT_SECONDS" | bc -l)" -eq 1 ]; then
            break
        fi
        sleep 0.1
    done
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
    rm -rf "$fake_home"
    if [ -z "$elapsed" ]; then
        echo "run $run: window did not appear" >&2
        exit 1
    fi
    printf "run %d: %.2f s\n" "$run" "$elapsed"
    total=$(echo "$total + $elapsed" | bc -l)
    sleep 2
done

printf "average over %d runs: %.2f s\n" "$RUNS" "$(echo "$total / $RUNS" | bc -l)"
