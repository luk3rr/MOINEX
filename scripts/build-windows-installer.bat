@echo off
setlocal enabledelayedexpansion
REM Filename: build-windows-installer.bat
REM Created on: January 4, 2026
REM Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
REM
REM Script to build Windows installer with embedded JRE using jpackage
REM This creates a self-contained executable that users can install without needing Java

setlocal enabledelayedexpansion

echo [INFO] Verificando requisitos...

REM Pin binary paths to JAVA_HOME so the CI-configured JDK is used, not whatever java is on PATH
if defined JAVA_HOME (
    set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
    set "JPACKAGE_BIN=%JAVA_HOME%\bin\jpackage.exe"
    echo [INFO] JAVA_HOME=%JAVA_HOME%
) else (
    echo [WARN] JAVA_HOME nao definido. Usando binarios do PATH - versao incorreta pode ser selecionada.
    set "JAVA_BIN=java"
    set "JPACKAGE_BIN=jpackage"
)

REM Check Java version via temp file to avoid double-quote-inside-single-quote parsing issues in for /f
"!JAVA_BIN!" -version > "%TEMP%\moinex_java_ver.txt" 2>&1
for /f "usebackq tokens=1-3" %%a in ("%TEMP%\moinex_java_ver.txt") do (
    if "%%b"=="version" if not defined JAVA_VERSION_STRING set "JAVA_VERSION_STRING=%%c"
)
del "%TEMP%\moinex_java_ver.txt" 2>nul
set JAVA_VERSION_STRING=!JAVA_VERSION_STRING:"=!

for /f "tokens=1 delims=." %%a in ("!JAVA_VERSION_STRING!") do set JAVA_MAJOR=%%a
if "!JAVA_MAJOR!"=="1" (
    for /f "tokens=2 delims=." %%a in ("!JAVA_VERSION_STRING!") do set JAVA_MAJOR=%%a
)

if !JAVA_MAJOR! LSS 21 (
    echo [ERROR] Java 21 ou superior e necessario. Versao atual: !JAVA_MAJOR!
    exit /b 1
)

echo [SUCCESS] Java !JAVA_MAJOR! detectado

REM Check if jpackage exists
if defined JAVA_HOME (
    if not exist "!JPACKAGE_BIN!" (
        echo [ERROR] jpackage nao encontrado em !JPACKAGE_BIN!. Verifique JAVA_HOME.
        exit /b 1
    )
) else (
    where jpackage >nul 2>&1
    if !ERRORLEVEL! NEQ 0 (
        echo [ERROR] jpackage nao encontrado. Certifique-se de que esta usando Java 21+.
        exit /b 1
    )
)

echo [SUCCESS] jpackage encontrado: !JPACKAGE_BIN!

REM Setup embedded Python
echo [INFO] Configurando Python embarcado...
if not exist "python-embedded\python.exe" (
    call scripts\setup-python-embedded.bat
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Falha ao configurar Python embarcado
        exit /b 1
    )
) else (
    echo [INFO] Python embarcado ja configurado
)

REM Build the JAR
echo [INFO] Compilando o projeto com Gradle...
call gradlew.bat clean build -x test

if not exist "build\libs\moinex.jar" (
    echo [ERROR] JAR nao foi gerado. Verifique a compilacao Gradle.
    exit /b 1
)

echo [SUCCESS] JAR compilado com sucesso

REM Stage only moinex.jar so jpackage --input sees a single JAR and produces one app.classpath entry
echo [INFO] Preparando diretorio de staging para jpackage...
if exist "build\jpackage-input" rmdir /S /Q "build\jpackage-input"
mkdir "build\jpackage-input"
copy "build\libs\moinex.jar" "build\jpackage-input\moinex.jar"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Falha ao copiar moinex.jar para diretorio de staging.
    exit /b 1
)
echo [SUCCESS] JAR copiado para diretorio de staging

REM Create output directory
set OUTPUT_DIR=installer-output
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM Extract version from git tag first, fallback to build.gradle.kts
for /f "tokens=*" %%i in ('git describe --tags --abbrev^=0 2^>nul') do set GIT_TAG=%%i

if not "!GIT_TAG!"=="" (
    REM Remove 'v' prefix if present
    set VERSION=!GIT_TAG:v=!
    echo [INFO] Versao obtida da tag Git: !VERSION!
) else (
    REM Fallback to build.gradle.kts version
    for /f "tokens=*" %%i in ('powershell -Command "(Get-Content build.gradle.kts | Select-String 'version = ').ToString() -replace 'version = |\"\"', ''"') do set VERSION=%%i
    
    REM If version is empty or SNAPSHOT, use default
    if "!VERSION!"=="" set VERSION=1.0.0
    if "!VERSION!"=="1.0-SNAPSHOT" set VERSION=1.0.0
    
    echo [INFO] Versao obtida do build.gradle.kts: !VERSION!
)

REM Copy Python embedded to build directory so jpackage can include it
echo [INFO] Copiando Python embarcado para o diretorio build...
if not exist "build\python-embedded" mkdir "build\python-embedded"
xcopy /E /I /Y python-embedded\* build\python-embedded\

echo [INFO] Gerando instalador Windows com jpackage...
echo [INFO]   Versao: %VERSION%
echo [INFO]   Isso pode levar alguns minutos...

REM Run jpackage to create Windows installer
"!JPACKAGE_BIN!" ^
    --input build\jpackage-input ^
    --name Moinex ^
    --main-jar moinex.jar ^
    --main-class org.springframework.boot.loader.launch.JarLauncher ^
    --type exe ^
    --dest "%OUTPUT_DIR%" ^
    --app-version "%VERSION%" ^
    --vendor "Lucas Araujo" ^
    --description "Aplicacao de gestao financeira pessoal" ^
    --copyright "Copyright (c) 2024-2026 Lucas Araujo. Licensed under GPL-3.0" ^
    --icon docs\img\icons\moinex-icon.ico ^
    --app-content build\python-embedded ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut ^
    --win-shortcut-prompt ^
    --win-menu-group "Moinex" ^
    --win-update-url "https://github.com/luk3rr/MOINEX/releases" ^
    --java-options "-Xmx1g" ^
    --verbose

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Instalador gerado com sucesso!
    echo [SUCCESS] Localizacao: %OUTPUT_DIR%\
) else (
    echo [ERROR] Erro ao gerar o instalador.
    exit /b 1
)
