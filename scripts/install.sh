#!/usr/bin/env sh

# Filename: install.sh
# Created on: November 9, 2024
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

print_success() {
    echo "\033[0;32m$1\033[0m"
}

print_error() {
    echo "\033[0;31m$1\033[0m"
}

print_info() {
    echo "\033[0;34m$1\033[0m"
}

print_info "A obter a lista de versões disponíveis..."

TAG_LIST=$(git tag -l --sort=-creatordate --format='%(refname:short) -- %(subject)')

if [ -z "$TAG_LIST" ]; then
    print_error "Nenhuma versão (tag) encontrada neste repositório."
    exit 1
fi

print_info "Por favor, selecione a versão que deseja instalar:"

COUNT=2
EXIT_OPTION=0
MAIN_BRANCH_OPTION=1
echo "  $EXIT_OPTION) Sair"
echo "  $MAIN_BRANCH_OPTION) Instalar a versão de desenvolvimento (branch main, pode ser instável)"
echo "$TAG_LIST" | while read -r TAG; do
    echo "  $COUNT) $TAG"
    COUNT=$((COUNT + 1))
done

while true; do
    printf "Digite o número da sua escolha (%d-%d): " "$EXIT_OPTION" "$COUNT"
    read -r CHOICE

    case $CHOICE in
        ''|*[!0-9]*)
            print_error "Seleção inválida. Por favor, digite um número."
            continue
            ;;
    esac

    if [ "$CHOICE" -ge $EXIT_OPTION ] && [ "$CHOICE" -le "$COUNT" ]; then
        if [ "$CHOICE" -eq "$EXIT_OPTION" ]; then
            print_error "Instalação cancelada."
            exit 0
        elif [ "$CHOICE" -eq "$MAIN_BRANCH_OPTION" ]; then
            VERSION="main"
            break
        else
            TAG_CHOICE_INDEX=$((CHOICE - 1))
            CHOSEN_LINE=$(echo "$TAG_LIST" | sed -n "${TAG_CHOICE_INDEX}p")
            VERSION=$(echo "$CHOSEN_LINE" | cut -d ' ' -f 1)
            break
        fi
    else
        print_error "Seleção inválida. Por favor, digite um número entre $EXIT_OPTION e $COUNT."
    fi
done


if [ "$VERSION" = "main" ]; then
    print_success ">> A fazer checkout para a branch 'main' e a obter as últimas alterações..."
    if git checkout main && git pull; then
        print_success ">> Checkout para 'main' realizado com sucesso."
    else
        print_error "Erro: Não foi possível fazer o checkout da branch 'main'."
        exit 1
    fi
else
    print_success ">> A fazer checkout para a versão $VERSION..."
    if git checkout "tags/$VERSION" 2>/dev/null; then
        print_success ">> Checkout para a versão $VERSION realizado com sucesso."
    else
        print_error "Erro: Não foi possível fazer o checkout da tag '$VERSION'."
        exit 1
    fi
fi

MOINEX_DIR="$HOME/.moinex"
DOT_LOCAL_DIR="$HOME/.local"

if pip install --break-system-packages -r requirements.txt; then
    print_success ">> Dependências Python instaladas"
else
    print_error "Erro ao instalar as dependências Python"
    exit 1
fi

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

cp img/icons/moinex-icon-256.png "$DOT_LOCAL_DIR/share/icons/moinex-icon.png"
cp img/icons/moinex-icon-128.png "$DOT_LOCAL_DIR/share/icons/moinex-icon-128.png"
cp img/icons/moinex-icon-64.png "$DOT_LOCAL_DIR/share/icons/moinex-icon-64.png"
cp img/icons/moinex-icon-32.png "$DOT_LOCAL_DIR/share/icons/moinex-icon-32.png"
print_success ">> Ícones copiados"

if cp docs/moinex.desktop "$DOT_LOCAL_DIR/share/applications/moinex.desktop"; then
    print_success ">> Arquivo .desktop copiado"
else
    print_error "Erro ao copiar o arquivo .desktop"
    exit 1
fi

chmod +x "$DOT_LOCAL_DIR/share/applications/moinex.desktop"
print_success ">> Permissões de execução concedidas ao arquivo .desktop"

if mvn clean package; then
    print_success ">> JAR criado com sucesso"
else
    print_error "Erro ao criar o JAR. Verifique se o maven e o java 21 estão instalados"
    exit 1
fi

if cp target/moinex.jar "$MOINEX_DIR/bin/moinex.jar"; then
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

print_success ">> Instalação da versão $VERSION concluída com sucesso. Aproveite o Moinex!"