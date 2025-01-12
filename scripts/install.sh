#!/usr/bin/env sh

# Filename: install.sh
# Created on: November  9, 2024
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

print_success() {
    echo -e "\033[0;32m$1\033[0m"
}

print_error() {
    echo -e "\033[0;31m$1\033[0m"
}

MOINEX_DIR="$HOME/.moinex"
DOT_LOCAL_DIR="$HOME/.local"

# Instala dependências pip
if grep -qi arch /etc/os-release; then
    if pip install --break-system-packages -r requirements.txt; then
        print_success ">> Dependências Python instaladas"
    else
        print_error "Erro ao instalar as dependências Python"
        exit 1
    fi
else
    if pip install -r requirements.txt; then
        print_success ">> Dependências Python instaladas"
    else
        print_error "Erro ao instalar as dependências Python"
        exit 1
    fi
fi

# Criar diretórios
if mkdir -p "$MOINEX_DIR/bin"; then
    print_success ">> Diretório $MOINEX_DIR/bin criado"
else
    print_error "Erro ao criar o diretório $MOINEX_DIR/bin"
    exit 1
fi

if mkdir -p "$MOINEX_DIR/data"; then
    print_success ">> Diretório $MOINEX_DIR/data criado"
else
    print_error "Erro ao criar o diretório $MOINEX_DIR/data"
    exit 1
fi

if mkdir -p "$DOT_LOCAL_DIR/state/moinex"; then
    print_success ">> Diretório $DOT_LOCAL_DIR/state/moinex criado"
else
    print_error "Erro ao criar o diretório $DOT_LOCAL_DIR/state/moinex"
    exit 1
fi

if mkdir -p "$DOT_LOCAL_DIR/share/icons"; then
    print_success ">> Diretório $DOT_LOCAL_DIR/share/icons criado"
else
    print_error "Erro ao criar o diretório $DOT_LOCAL_DIR/share/icons"
    exit 1
fi

# Copiar ícones
if cp img/icons/moinex-icon-256.png "$DOT_LOCAL_DIR/share/icons/moinex-icon.png"; then
    print_success ">> Ícone 256x256 copiado"
else
    print_error "Erro ao copiar o ícone 256x256"
    exit 1
fi

if cp img/icons/moinex-icon-128.png "$DOT_LOCAL_DIR/share/icons/moinex-icon-128.png"; then
    print_success ">> Ícone 128x128 copiado"
else
    print_error "Erro ao copiar o ícone 128x128"
    exit 1
fi

if cp img/icons/moinex-icon-64.png "$DOT_LOCAL_DIR/share/icons/moinex-icon-64.png"; then
    print_success ">> Ícone 64x64 copiado"
else
    print_error "Erro ao copiar o ícone 64x64"
    exit 1
fi

if cp img/icons/moinex-icon-32.png "$DOT_LOCAL_DIR/share/icons/moinex-icon-32.png"; then
    print_success ">> Ícone 32x32 copiado"
else
    print_error "Erro ao copiar o ícone 32x32"
    exit 1
fi

# Copiar arquivo .desktop
if cp docs/moinex.desktop "$DOT_LOCAL_DIR/share/applications/moinex.desktop"; then
    print_success ">> Arquivo .desktop copiado"
else
    print_error "Erro ao copiar o arquivo .desktop"
    exit 1
fi

chmod +x "$DOT_LOCAL_DIR/share/applications/moinex.desktop"

print_success ">> Permissões de execução concedidas ao arquivo .desktop"

# Construir o JAR
if mvn clean package; then
    print_success ">> JAR criado com sucesso"
else
    print_error "Erro ao criar o JAR"
    exit 1
fi

# Copiar JAR e script de execução
if cp target/Moinex-1.0-SNAPSHOT.jar "$MOINEX_DIR/bin/moinex.jar"; then
    print_success ">> JAR copiado para $MOINEX_DIR/bin"
else
    print_error "Erro ao copiar o JAR para $MOINEX_DIR/bin"
    exit 1
fi

if cp scripts/run.sh "$MOINEX_DIR/"; then
    print_success ">> Script de execução copiado para $MOINEX_DIR/"
else
    print_error "Erro ao copiar o script de execução"
    exit 1
fi

print_success ">> Instalação concluída com sucesso. Aproveite o Moinex!"
