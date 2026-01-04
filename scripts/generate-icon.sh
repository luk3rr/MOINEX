#!/usr/bin/env bash
# Filename: generate-icon.sh
# Created on: January 4, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
# Script to generate Windows .ico file from PNG

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ICON_DIR="$PROJECT_ROOT/docs/img/icons"
PNG_FILE="$ICON_DIR/moinex-icon-256.png"
ICO_FILE="$ICON_DIR/moinex-icon.ico"

echo "[INFO] Gerando arquivo .ico para Windows..."

# Check if ImageMagick is installed
if ! command -v convert &> /dev/null; then
    echo "[ERROR] ImageMagick não está instalado."
    echo "[INFO] Instale com: sudo apt-get install imagemagick (Ubuntu/Debian)"
    echo "[INFO] Ou use uma ferramenta online: https://convertio.co/png-ico/"
    exit 1
fi

# Check if PNG exists
if [ ! -f "$PNG_FILE" ]; then
    echo "[ERROR] Arquivo PNG não encontrado: $PNG_FILE"
    exit 1
fi

# Generate .ico with multiple resolutions
convert "$PNG_FILE" -define icon:auto-resize=256,128,64,48,32,16 "$ICO_FILE"

if [ $? -eq 0 ]; then
    echo "[SUCCESS] Ícone .ico gerado com sucesso!"
    echo "[SUCCESS] Localização: $ICO_FILE"
else
    echo "[ERROR] Erro ao gerar ícone .ico"
    exit 1
fi
