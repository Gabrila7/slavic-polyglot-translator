@echo off
chcp 65001 > nul
echo =================================================================
echo    SlavicPolyglot - Tradutor EN/ES/RU & Teclado Russo (Build)
echo =================================================================
echo.

if not exist "bin" mkdir "bin"
if not exist "dist" mkdir "dist"

echo [1/3] Compilando codigo-fonte Java (UTF-8)...
javac -encoding UTF-8 -d bin src\com\gabrila7\slavicpolyglot\model\*.java src\com\gabrila7\slavicpolyglot\service\*.java src\com\gabrila7\slavicpolyglot\ui\*.java src\com\gabrila7\slavicpolyglot\*.java

if %errorlevel% neq 0 (
    echo.
    echo [ERRO] Falha durante a compilacao. Verifique se o JDK esta instalado.
    pause
    exit /b 1
)

echo [2/3] Empacotando executavel multiplataforma (dist\SlavicPolyglot.jar)...
jar cfe dist\SlavicPolyglot.jar com.gabrila7.slavicpolyglot.Main -C bin .

echo [3/3] Iniciando SlavicPolyglot Desktop...
echo.
java -Dfile.encoding=UTF-8 -jar dist\SlavicPolyglot.jar
