@echo off
REM Filename: build-windows-installer.bat
REM Created on: January 4, 2026
REM Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
REM
REM Script to build Windows installer with embedded JRE using jpackage
REM This creates a self-contained executable that users can install without needing Java

setlocal enabledelayedexpansion

echo [INFO] Verificando requisitos...

REM Check Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION_STRING=%%g
)
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
where jpackage >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] jpackage nao encontrado. Certifique-se de que esta usando Java 21+.
    exit /b 1
)

echo [SUCCESS] jpackage encontrado

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
echo [INFO] Compilando o projeto com Maven...
call mvn clean package -DskipTests

if not exist "target\moinex.jar" (
    echo [ERROR] JAR nao foi gerado. Verifique a compilacao Maven.
    exit /b 1
)

echo [SUCCESS] JAR compilado com sucesso

REM Create output directory
set OUTPUT_DIR=installer-output
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM Set version (default if not found)
set VERSION=1.0.0

echo [INFO] Gerando instalador Windows com jpackage...
echo [INFO]   Versao: %VERSION%
echo [INFO]   Isso pode levar alguns minutos...

REM Run jpackage to create Windows installer
jpackage ^
    --input target ^
    --name Moinex ^
    --main-jar moinex.jar ^
    --main-class org.springframework.boot.loader.launch.JarLauncher ^
    --type exe ^
    --dest "%OUTPUT_DIR%" ^
    --app-version "%VERSION%" ^
    --vendor "Lucas Araujo" ^
    --description "Aplicacao de gestao financeira pessoal" ^
    --icon docs\img\icons\moinex-icon.ico ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut ^
    --win-menu-group "Moinex" ^
    --java-options "-Xmx512m" ^
    --verbose

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Erro ao gerar o instalador.
    exit /b 1
)

REM Copy Python embedded to the installation directory structure
echo [INFO] Copiando Python embarcado para o diretorio de instalacao...
xcopy /E /I /Y python-embedded "%OUTPUT_DIR%\Moinex\python-embedded"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Instalador gerado com sucesso!
    echo [SUCCESS] Localizacao: %OUTPUT_DIR%\
) else (
    echo [ERROR] Erro ao gerar o instalador.
    exit /b 1
)
