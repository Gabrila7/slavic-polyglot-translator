#!/usr/bin/env bash
# Gera um binario executavel unico para Linux (SlavicPolyglot-linux.run)
# embutindo o stub shell + o pacote JAR na mesma imagem executavel.
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

mkdir -p bin dist
javac -encoding UTF-8 -d bin \
    src/com/gabrila7/slavicpolyglot/model/*.java \
    src/com/gabrila7/slavicpolyglot/service/*.java \
    src/com/gabrila7/slavicpolyglot/ui/*.java \
    src/com/gabrila7/slavicpolyglot/*.java

jar cfe dist/SlavicPolyglot.jar com.gabrila7.slavicpolyglot.Main -C bin .

cat << 'EOF' > dist/SlavicPolyglot-linux.run
#!/usr/bin/env bash
exec java -Dfile.encoding=UTF-8 -jar "$0" "$@"
EOF

cat dist/SlavicPolyglot.jar >> dist/SlavicPolyglot-linux.run
chmod +x dist/SlavicPolyglot-linux.run
echo "[OK] Executavel Linux gerado em: dist/SlavicPolyglot-linux.run"
