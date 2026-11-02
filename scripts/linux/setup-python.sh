#!/usr/bin/env bash

# Filename: setup-python-linux.sh
# Created on: November 2, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
#
# Downloads a relocatable CPython (python-build-standalone), verifies its SHA256
# against the value pinned below, installs requirements.txt into it and prunes it.
# The result in build/python-linux is embedded in the Linux packages.

set -euo pipefail

PYTHON_RELEASE_TAG="20261026"
PYTHON_ARCHIVE="cpython-3.12.16+${PYTHON_RELEASE_TAG}-x86_64-unknown-linux-gnu-install_only_stripped.tar.gz"
PYTHON_SHA256="ce4f6d0a6cfdb3bed6e44c2f9affd85943c76a42e25ad3a259002f31d6fb8a20"
PYTHON_URL="https://github.com/astral-sh/python-build-standalone/releases/download/${PYTHON_RELEASE_TAG}/cpython-3.12.16%2B${PYTHON_RELEASE_TAG}-x86_64-unknown-linux-gnu-install_only_stripped.tar.gz"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DOWNLOAD_DIR="${ROOT_DIR}/build/python-linux-download"
OUTPUT_DIR="${ROOT_DIR}/build/python-linux"
IMPORT_CHECK="import requests, yfinance, pandas, numpy"

log() {
    printf '[setup-python-linux] %s\n' "$1"
}

fail() {
    printf '[setup-python-linux] ERRO: %s\n' "$1" >&2
    exit 1
}

verify_imports() {
    "${OUTPUT_DIR}/bin/python3" -I -c "${IMPORT_CHECK}" >/dev/null 2>&1
}

if [ -x "${OUTPUT_DIR}/bin/python3" ] && verify_imports; then
    log "Python embutido já preparado em ${OUTPUT_DIR}"
    exit 0
fi

mkdir -p "${DOWNLOAD_DIR}"
ARCHIVE_PATH="${DOWNLOAD_DIR}/${PYTHON_ARCHIVE}"

if [ ! -f "${ARCHIVE_PATH}" ]; then
    log "Baixando ${PYTHON_ARCHIVE}"
    curl --fail --silent --show-error --location --output "${ARCHIVE_PATH}" "${PYTHON_URL}" \
        || fail "falha ao baixar o Python"
fi

ACTUAL_SHA256="$(sha256sum "${ARCHIVE_PATH}" | awk '{print $1}')"
if [ "${ACTUAL_SHA256}" != "${PYTHON_SHA256}" ]; then
    rm -f "${ARCHIVE_PATH}"
    fail "SHA256 do Python não confere (esperado ${PYTHON_SHA256}, obtido ${ACTUAL_SHA256})"
fi
log "SHA256 conferido"

rm -rf "${OUTPUT_DIR}" "${DOWNLOAD_DIR}/extract"
mkdir -p "${DOWNLOAD_DIR}/extract"
tar -xzf "${ARCHIVE_PATH}" -C "${DOWNLOAD_DIR}/extract"
mv "${DOWNLOAD_DIR}/extract/python" "${OUTPUT_DIR}"
rm -rf "${DOWNLOAD_DIR}/extract"

log "Instalando requirements.txt"
"${OUTPUT_DIR}/bin/python3" -I -m pip install --no-cache-dir --disable-pip-version-check \
    -r "${ROOT_DIR}/requirements.txt"

log "Podando arquivos desnecessários"
find "${OUTPUT_DIR}" -type d -name "__pycache__" -prune -exec rm -rf {} +
STDLIB_DIR="$(echo "${OUTPUT_DIR}"/lib/python3.*)"
rm -rf "${STDLIB_DIR}/test" "${STDLIB_DIR}/idlelib" "${STDLIB_DIR}/tkinter" "${STDLIB_DIR}/turtledemo"
rm -rf "${STDLIB_DIR}/site-packages/pandas/tests"
find "${STDLIB_DIR}/site-packages/numpy" -type d -name "tests" -prune -exec rm -rf {} +

verify_imports || fail "os imports (${IMPORT_CHECK}) falharam depois da poda"

log "Python embutido pronto: $(du -sh "${OUTPUT_DIR}" | awk '{print $1}')"
