#!/usr/bin/env bash

# Filename: validate-linux-package.sh
# Created on: November 2, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
#
# Validates the packages in installer-output/ before they are published:
# embedded Python imports, .deb contents and a 30 s launch under Xvfb with a temporary user.home.
# Usage: scripts/validate-linux-package.sh [--no-launch]

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${ROOT_DIR}/installer-output"
WORK_DIR="$(mktemp -d -t moinex-validate-XXXXXX)"
LAUNCH=1
IMPORT_CHECK="import requests, yfinance, pandas, numpy"

[ "${1:-}" = "--no-launch" ] && LAUNCH=0

cleanup() {
    rm -rf "${WORK_DIR}"
}
trap cleanup EXIT

log() {
    printf '[validate-linux-package] %s\n' "$1"
}

fail() {
    printf '[validate-linux-package] ERRO: %s\n' "$1" >&2
    exit 1
}

shopt -s nullglob
DEBS=("${OUTPUT_DIR}"/moinex_*_amd64.deb)
TARS=("${OUTPUT_DIR}"/moinex-*-linux-x64.tar.gz)
[ "${#DEBS[@]}" -eq 1 ] || fail "esperava exatamente um .deb em ${OUTPUT_DIR}"
[ "${#TARS[@]}" -eq 1 ] || fail "esperava exatamente um tar.gz em ${OUTPUT_DIR}"
DEB="${DEBS[0]}"
TAR="${TARS[0]}"

log "Conferindo o conteúdo do .deb"
INFO="$(dpkg-deb -I "${DEB}")"
grep -q "Package: moinex" <<< "${INFO}" || fail ".deb sem Package: moinex"
CONTENTS="$(dpkg-deb -c "${DEB}")"
for expected in "./opt/moinex/bin/Moinex" "./opt/moinex/lib/python/bin/python3" "./opt/moinex/lib/moinex-Moinex.desktop" "./opt/moinex/lib/app/moinex.jar"; do
    grep -Eq " ${expected}( -> .*)?\$" <<< "${CONTENTS}" || fail ".deb sem ${expected}"
done
OUTSIDE="$(awk '{print $6}' <<< "${CONTENTS}" | grep -v '^\./opt' | grep -v '^\./$' || true)"
[ -z "${OUTSIDE}" ] || fail ".deb contém arquivos fora de /opt: ${OUTSIDE}"

log "Conferindo o tar.gz"
mkdir -p "${WORK_DIR}/tar"
tar -xzf "${TAR}" -C "${WORK_DIR}/tar"
[ -x "${WORK_DIR}/tar/Moinex/bin/Moinex" ] || fail "tar.gz sem o launcher executável"
"${WORK_DIR}/tar/Moinex/lib/python/bin/python3" -I -c "${IMPORT_CHECK}" || fail "imports do Python do tar.gz falharam"

log "Conferindo o Python do .deb"
mkdir -p "${WORK_DIR}/deb"
dpkg-deb -x "${DEB}" "${WORK_DIR}/deb"
"${WORK_DIR}/deb/opt/moinex/lib/python/bin/python3" -I -c "${IMPORT_CHECK}" || fail "imports do Python do .deb falharam"

if [ "${LAUNCH}" -eq 1 ]; then
    command -v xvfb-run >/dev/null 2>&1 || fail "xvfb-run ausente (use --no-launch para pular)"
    log "Subindo a app por 30 s sob Xvfb com user.home temporário"
    mkdir -p "${WORK_DIR}/home"
    set +e
    JAVA_TOOL_OPTIONS="-Duser.home=${WORK_DIR}/home" \
        timeout 30 xvfb-run -a "${WORK_DIR}/deb/opt/moinex/bin/Moinex" > "${WORK_DIR}/launch.log" 2>&1
    STATUS=$?
    set -e
    [ "${STATUS}" -eq 124 ] || { tail -30 "${WORK_DIR}/launch.log" >&2; fail "a app encerrou antes de 30 s (status ${STATUS})"; }
    grep -q "Started application" "${WORK_DIR}/launch.log" || { tail -30 "${WORK_DIR}/launch.log" >&2; fail "a app não chegou a iniciar o Spring"; }
fi

log "Pacotes válidos: $(basename "${DEB}") e $(basename "${TAR}")"
