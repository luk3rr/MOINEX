#!/usr/bin/env bash

# Filename: build-linux-package.sh
# Created on: November 2, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
#
# Builds the Linux packages of MOINEX with an embedded JRE (jpackage) and an embedded Python.
# Usage: scripts/linux/build-package.sh [app-image|tar|deb|all] [--version X.Y.Z]
# Output: installer-output/moinex-<version>-linux-x64.tar.gz and installer-output/moinex_<version>-1_amd64.deb

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_DIR="${ROOT_DIR}/installer-output"
STAGING_DIR="${ROOT_DIR}/build/jpackage-input"
APP_IMAGE_DIR="${ROOT_DIR}/build/linux-app-image"
PYTHON_DIR="${ROOT_DIR}/build/python-linux"
APP_CONTENT_DIR="${ROOT_DIR}/build/linux-app-content"
RESOURCE_DIR="${ROOT_DIR}/build/linux-resources"
APP_NAME="Moinex"
PACKAGE_NAME="moinex"

TARGET="all"
VERSION=""

log() {
    printf '[build-linux-package] %s\n' "$1"
}

fail() {
    printf '[build-linux-package] ERRO: %s\n' "$1" >&2
    exit 1
}

while [ $# -gt 0 ]; do
    case "$1" in
        app-image|tar|deb|all) TARGET="$1" ;;
        --version)
            shift
            [ $# -gt 0 ] || fail "--version exige um valor"
            VERSION="$1"
            ;;
        *) fail "argumento desconhecido: $1" ;;
    esac
    shift
done

resolve_version() {
    if [ -n "${VERSION}" ]; then
        return
    fi
    local tag
    tag="$(git -C "${ROOT_DIR}" describe --tags --abbrev=0 2>/dev/null || true)"
    if [ -n "${tag}" ]; then
        VERSION="${tag#v}"
        return
    fi
    VERSION="$(sed -n 's/^version = "\(.*\)"/\1/p' "${ROOT_DIR}/build.gradle.kts" | head -1)"
    [ -n "${VERSION}" ] || fail "não foi possível determinar a versão"
}

require_tools() {
    local tool
    for tool in jpackage tar; do
        command -v "${tool}" >/dev/null 2>&1 || fail "ferramenta ausente: ${tool}"
    done
    if [ "${TARGET}" = "deb" ] || [ "${TARGET}" = "all" ]; then
        for tool in fakeroot dpkg-deb; do
            command -v "${tool}" >/dev/null 2>&1 || fail "ferramenta ausente para o .deb: ${tool}"
        done
    fi
}

build_jar() {
    log "Compilando moinex.jar"
    (cd "${ROOT_DIR}" && ./gradlew bootJar -x test)
    [ -f "${ROOT_DIR}/build/libs/moinex.jar" ] || fail "build/libs/moinex.jar não foi gerado"
    rm -rf "${STAGING_DIR}"
    mkdir -p "${STAGING_DIR}"
    cp "${ROOT_DIR}/build/libs/moinex.jar" "${STAGING_DIR}/moinex.jar"
}

build_app_image() {
    "${ROOT_DIR}/scripts/linux/setup-python.sh"
    build_jar
    log "Preparando o conteúdo extra (lib/python)"
    rm -rf "${APP_CONTENT_DIR}"
    mkdir -p "${APP_CONTENT_DIR}"
    cp -al "${PYTHON_DIR}" "${APP_CONTENT_DIR}/python" 2>/dev/null || cp -a "${PYTHON_DIR}" "${APP_CONTENT_DIR}/python"
    log "Gerando o app-image (versão ${VERSION})"
    rm -rf "${APP_IMAGE_DIR}"
    mkdir -p "${APP_IMAGE_DIR}"
    jpackage \
        --type app-image \
        --input "${STAGING_DIR}" \
        --dest "${APP_IMAGE_DIR}" \
        --name "${APP_NAME}" \
        --main-jar moinex.jar \
        --main-class org.springframework.boot.loader.launch.JarLauncher \
        --app-version "${VERSION}" \
        --vendor "Lucas Araujo" \
        --description "Aplicação de gestão financeira pessoal" \
        --copyright "Copyright (c) 2024-2026 Lucas Araujo. Licensed under AGPL-3.0" \
        --icon "${ROOT_DIR}/docs/img/icons/moinex-icon-256.png" \
        --app-content "${APP_CONTENT_DIR}/python" \
        --java-options "-Xmx1g"
    [ -x "${APP_IMAGE_DIR}/${APP_NAME}/bin/${APP_NAME}" ] || fail "launcher não encontrado no app-image"
    [ -x "${APP_IMAGE_DIR}/${APP_NAME}/lib/python/bin/python3" ] || fail "Python embutido não encontrado no app-image"
}

build_tar() {
    local archive="${OUTPUT_DIR}/${PACKAGE_NAME}-${VERSION}-linux-x64.tar.gz"
    log "Gerando ${archive##*/}"
    mkdir -p "${OUTPUT_DIR}"
    tar -C "${APP_IMAGE_DIR}" -czf "${archive}" "${APP_NAME}"
}

build_deb() {
    log "Gerando o .deb"
    mkdir -p "${OUTPUT_DIR}"
    rm -rf "${RESOURCE_DIR}"
    mkdir -p "${RESOURCE_DIR}"
    cp "${ROOT_DIR}/packaging/linux/Moinex.desktop" "${RESOURCE_DIR}/Moinex.desktop"
    cp "${ROOT_DIR}/docs/img/icons/moinex-icon-256.png" "${RESOURCE_DIR}/Moinex.png"
    jpackage \
        --type deb \
        --app-image "${APP_IMAGE_DIR}/${APP_NAME}" \
        --dest "${OUTPUT_DIR}" \
        --name "${PACKAGE_NAME}" \
        --app-version "${VERSION}" \
        --vendor "Lucas Araujo" \
        --description "Aplicação de gestão financeira pessoal" \
        --copyright "Copyright (c) 2024-2026 Lucas Araujo. Licensed under AGPL-3.0" \
        --license-file "${ROOT_DIR}/LICENSE" \
        --install-dir /opt \
        --linux-package-name "${PACKAGE_NAME}" \
        --linux-deb-maintainer "araujolucas@dcc.ufmg.br" \
        --linux-menu-group "Office;Finance;" \
        --linux-app-category utils \
        --linux-package-deps "libgtk-3-0 | libgtk-3-0t64, libxtst6, libxxf86vm1" \
        --resource-dir "${RESOURCE_DIR}" \
        --linux-shortcut
}

resolve_version
require_tools
mkdir -p "${OUTPUT_DIR}"
build_app_image

case "${TARGET}" in
    tar) build_tar ;;
    deb) build_deb ;;
    all)
        build_tar
        build_deb
        ;;
esac

log "Concluído. Arquivos em ${OUTPUT_DIR}:"
ls -lh "${OUTPUT_DIR}"
