#!/usr/bin/env bash

# Filename: test-linux-package-containers.sh
# Created on: November 2, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
#
# Tests the Linux packages in clean Ubuntu containers, as a user would: installs the .deb with apt,
# starts the app under Xvfb, reinstalls over it (upgrade), removes it, and checks that the user data
# survives. Also runs the portable tar.gz. Meant for the packages built by the CI (download the
# workflow artifact) or by scripts/linux/build-package.sh.
#
# Usage: scripts/linux/test-package-containers.sh [PACKAGE_DIR]
#   PACKAGE_DIR  directory with moinex_*_amd64.deb and moinex-*-linux-x64.tar.gz (default: installer-output)
#   IMAGES       space-separated images to test (default: "ubuntu:22.04 ubuntu:24.04")
# Needs docker; run it with sudo (or as a user in the docker group).
# A line starting with FAIL means a problem; the script exits 1 if any check failed.

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PACKAGE_DIR_ARG="${1:-${ROOT_DIR}/installer-output}"
[ -d "${PACKAGE_DIR_ARG}" ] || { echo "diretório não encontrado: ${PACKAGE_DIR_ARG}" >&2; exit 1; }
PACKAGE_DIR="$(cd "${PACKAGE_DIR_ARG}" && pwd)"
IMAGES="${IMAGES:-ubuntu:22.04 ubuntu:24.04}"

command -v docker >/dev/null 2>&1 || { echo "docker não encontrado" >&2; exit 1; }
ls "${PACKAGE_DIR}"/moinex_*_amd64.deb "${PACKAGE_DIR}"/moinex-*-linux-x64.tar.gz >/dev/null 2>&1 \
    || { echo "esperava o .deb e o tar.gz em ${PACKAGE_DIR}" >&2; exit 1; }

read -r -d '' INNER <<'INNER_SCRIPT'
set -u
export DEBIAN_FRONTEND=noninteractive
export PYTHONDONTWRITEBYTECODE=1
ok() { echo "PASS: $1"; }
ko() { echo "FAIL: $1"; }

apt-get update -qq >/dev/null 2>&1
mkdir -p /etc/xdg/menus/applications-merged /usr/share/desktop-directories /usr/share/applications

echo "== .deb: instalar"
if apt-get install -y -qq /pkg/moinex_*_amd64.deb >/tmp/install.log 2>&1; then ok "apt install do .deb"; else ko "apt install do .deb"; tail -15 /tmp/install.log; fi
[ -x /opt/moinex/bin/Moinex ] && ok "launcher em /opt/moinex/bin/Moinex" || ko "launcher ausente"
/opt/moinex/lib/python/bin/python3 -B -I -c "import requests, yfinance, pandas, numpy" && ok "imports do Python embutido" || ko "imports do Python embutido"

mkdir -p /root/.moinex/data && echo dados-de-teste > /root/.moinex/data/marcador.txt
apt-get install -y -qq xvfb >/dev/null 2>&1
timeout 40 xvfb-run -a /opt/moinex/bin/Moinex >/tmp/run.log 2>&1
code=$?
if [ "$code" -eq 124 ] && grep -q "Started application" /tmp/run.log; then ok ".deb: app sobe e fica rodando"; else ko ".deb: app (saida=$code)"; tail -25 /tmp/run.log; fi
[ -f /root/.moinex/data/moinex.db ] && ok "app criou o banco em ~/.moinex/data" || ko "app não criou o banco"

echo "== .deb: reinstalar por cima (atualização)"
apt-get install -y -qq --reinstall /pkg/moinex_*_amd64.deb >/tmp/reinstall.log 2>&1 && ok "reinstalação" || { ko "reinstalação"; tail -10 /tmp/reinstall.log; }
[ "$(cat /root/.moinex/data/marcador.txt)" = "dados-de-teste" ] && ok "dados intactos após atualizar" || ko "dados alterados após atualizar"

echo "== .deb: remover"
apt-get remove -y -qq moinex >/tmp/remove.log 2>&1 && ok "apt remove" || { ko "apt remove"; tail -10 /tmp/remove.log; }
if [ ! -e /opt/moinex ]; then ok "/opt/moinex removido"; else ko "/opt/moinex ainda existe"; echo "sobrou:"; find /opt/moinex | head -15; fi
[ "$(cat /root/.moinex/data/marcador.txt)" = "dados-de-teste" ] && ok "dados preservados após remover" || ko "dados apagados ou alterados"

echo "== .tar.gz (sem instalar o .deb)"
rm -rf /root/.moinex
mkdir -p /tmp/tar && tar -xzf /pkg/moinex-*-linux-x64.tar.gz -C /tmp/tar
/tmp/tar/Moinex/lib/python/bin/python3 -B -I -c "import requests, yfinance, pandas, numpy" && ok "imports do Python do tar.gz" || ko "imports do Python do tar.gz"
timeout 40 xvfb-run -a /tmp/tar/Moinex/bin/Moinex >/tmp/run2.log 2>&1
code=$?
if [ "$code" -eq 124 ] && grep -q "Started application" /tmp/run2.log; then ok ".tar.gz: app sobe e fica rodando"; else ko ".tar.gz: app (saida=$code)"; tail -25 /tmp/run2.log; fi
INNER_SCRIPT

FAILED=0
for IMG in ${IMAGES}; do
    echo
    echo "################ ${IMG} ################"
    OUT="$(docker run --rm -v "${PACKAGE_DIR}":/pkg:ro "${IMG}" bash -c "${INNER}" 2>&1)"
    printf '%s\n' "${OUT}"
    if grep -q '^FAIL' <<< "${OUT}"; then FAILED=1; fi
done

echo
if [ "${FAILED}" -eq 0 ]; then echo "Todos os testes passaram."; else echo "Há testes com FAIL."; exit 1; fi
