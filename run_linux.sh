#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "================================================================="
echo "   SlavicPolyglot - Tradutor EN/ES/RU & Teclado Russo (Linux)"
echo "================================================================="

if ! command -v java >/dev/null 2>&1; then
    echo "[ERRO] Java (JRE/JDK) nao encontrado. Instale com:"
    echo "  Ubuntu/Debian: sudo apt install default-jre"
    echo "  Fedora:        sudo dnf install java-latest-openjdk"
    echo "  Arch Linux:    sudo pacman -S jre-openjdk"
    exit 1
fi

if [ ! -f "dist/SlavicPolyglot.jar" ]; then
    echo "[1/2] Compilando fontes Java..."
    mkdir -p bin dist
    javac -encoding UTF-8 -d bin \
        src/com/gabrila7/slavicpolyglot/model/*.java \
        src/com/gabrila7/slavicpolyglot/service/*.java \
        src/com/gabrila7/slavicpolyglot/ui/*.java \
        src/com/gabrila7/slavicpolyglot/*.java
    jar cfe dist/SlavicPolyglot.jar com.gabrila7.slavicpolyglot.Main -C bin .
fi

echo "Iniciando aplicacao..."
exec java -Dfile.encoding=UTF-8 -jar dist/SlavicPolyglot.jar "$@"
