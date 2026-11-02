#!/usr/bin/env bash

# Filename: log.sh
# Created on: November 2, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
#
# Shows the MOINEX application log. The log file is the same no matter how the app was installed
# (.deb, tar.gz, install.sh, bootRun): ${user.home}/.local/state/moinex/moinex.log (see logback.xml).
# Usage: scripts/dev/log.sh [-n LINES] [-s] [-e] [-g PATTERN] [-a]
#   -n LINES    number of lines to show first (default 200)
#   -s          snapshot: print and exit instead of following the log live (default is live, like tail -f)
#   -e          only WARN and ERROR lines
#   -g PATTERN  only lines matching the extended regex PATTERN (case-insensitive)
#   -a          include the rotated files (moinex.1.log ... moinex.3.log), oldest first
# MOINEX_LOG_DIR overrides the directory.

set -euo pipefail

LOG_DIR="${MOINEX_LOG_DIR:-${HOME}/.local/state/moinex}"
LINES=200
FOLLOW=1
ERRORS_ONLY=0
PATTERN=""
ALL=0

usage() {
    sed -n '7,16p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

while getopts "n:seg:ah" opt; do
    case "${opt}" in
        n) LINES="${OPTARG}" ;;
        s) FOLLOW=0 ;;
        e) ERRORS_ONLY=1 ;;
        g) PATTERN="${OPTARG}" ;;
        a) ALL=1 ;;
        h) usage; exit 0 ;;
        *) usage >&2; exit 2 ;;
    esac
done

LOG_FILE="${LOG_DIR}/moinex.log"
if [ ! -f "${LOG_FILE}" ]; then
    printf 'Log não encontrado em %s\n' "${LOG_FILE}" >&2
    printf 'A app grava o log em ${user.home}/.local/state/moinex/ em qualquer forma de instalação.\n' >&2
    printf 'Abra a app ao menos uma vez ou aponte o diretório com MOINEX_LOG_DIR=...\n' >&2
    exit 1
fi

filter() {
    if [ "${ERRORS_ONLY}" -eq 1 ]; then
        grep --line-buffered -E ' (WARN|ERROR) |^\s+at |^Caused by' || true
    else
        cat
    fi | if [ -n "${PATTERN}" ]; then grep --line-buffered -iE "${PATTERN}" || true; else cat; fi
}

if [ "${ALL}" -eq 1 ]; then
    FILES=()
    for index in 3 2 1; do
        [ -f "${LOG_DIR}/moinex.${index}.log" ] && FILES+=("${LOG_DIR}/moinex.${index}.log")
    done
    FILES+=("${LOG_FILE}")
else
    FILES=("${LOG_FILE}")
fi

printf '[log] %s\n' "${FILES[*]}" >&2

cat "${FILES[@]}" | filter | tail -n "${LINES}"

if [ "${FOLLOW}" -eq 1 ]; then
    tail -n 0 -F "${LOG_FILE}" | filter
fi
