#!/usr/bin/env sh

# Filename: uninstall.sh
# Created on: December 8, 2024
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

print_success() {
    echo "\033[0;32m$1\033[0m"
}

print_error() {
    echo "\033[0;31m$1\033[0m"
}

MOINEX_DIR="$HOME/.moinex"
DOT_LOCAL_DIR="$HOME/.local"

# Remover JAR e diretórios relacionados
if rm -rf "$MOINEX_DIR"; then
    print_success ">> Diretório $MOINEX_DIR removido com sucesso"
else
    print_error "Erro ao remover o diretório $MOINEX_DIR"
fi

# Remover ícones
if rm -f "$DOT_LOCAL_DIR/share/icons/moinex-icon.png" \
          "$DOT_LOCAL_DIR/share/icons/moinex-icon-128.png" \
          "$DOT_LOCAL_DIR/share/icons/moinex-icon-64.png" \
          "$DOT_LOCAL_DIR/share/icons/moinex-icon-32.png"; then
    print_success ">> Ícones removidos com sucesso"
else
    print_error "Erro ao remover os ícones"
fi

# Remover o arquivo .desktop
if rm -f "$DOT_LOCAL_DIR/share/applications/moinex.desktop"; then
    print_success ">> Arquivo .desktop removido com sucesso"
else
    print_error "Erro ao remover o arquivo .desktop"
fi

# Remover o estado do aplicativo
if rm -rf "$DOT_LOCAL_DIR/state/moinex"; then
    print_success ">> Diretório de estado removido com sucesso"
else
    print_error "Erro ao remover o diretório de estado"
fi

print_success ">> Desinstalação concluída com sucesso"
