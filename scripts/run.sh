#!/usr/bin/env sh

# Filename: run.sh
# Created on: November  9, 2024
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

MOINEX_JAVA_HOME=""
DEFAULT_JAVA_21_PATH="/usr/lib/jvm/java-21-openjdk"

JAR_PATH="$HOME/.moinex/bin/moinex.jar"

print_error() {
    echo -e "\033[0;31m$1\033[0m"
}

# Check if JAVA_HOME is set; otherwise, try to set it with the system's default Java
if [ -z "$JAVA_HOME" ]; then
    JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
fi

JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)

# Check if the Java version is compatible with Moinex, which requires Java 21
# If the version is compatible, set MOINEX_JAVA_HOME to JAVA_HOME
# Otherwise, try to find Java 21 in the default path
if [ "$JAVA_VERSION" -ge 21 ]; then
    MOINEX_JAVA_HOME="$JAVA_HOME"
else
    # Se JAVA_HOME não é compatível, tenta encontrar o Java 21 no caminho padrão
    if [ -d "$DEFAULT_JAVA_21_PATH" ]; then
        MOINEX_JAVA_HOME="$DEFAULT_JAVA_21_PATH"
    else
        # Exibe erro se nenhuma instalação compatível foi encontrada
        print_error "Erro: Não foi encontrada uma versão compatível com Java 21. Verifique se o Java 21 está instalado."
        exit 1
    fi
fi

if [ ! -d "$MOINEX_JAVA_HOME" ]; then
    print_error "Erro: O diretório $MOINEX_JAVA_HOME não foi encontrado. Verifique se o Java 21 está instalado."
  exit 1
fi

if [ ! -f "$JAR_PATH" ]; then
    print_error "Erro: O arquivo $JAR_PATH não foi encontrado. Verifique se o Moinex foi instalado corretamente."
  exit 1
fi

"$MOINEX_JAVA_HOME/bin/java" -jar "$JAR_PATH" "$@"