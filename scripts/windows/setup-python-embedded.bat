@echo off
REM Filename: setup-python-embedded.bat
REM Created on: January 4, 2026
REM Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
REM
REM Script to download and setup Python embeddable for Windows installer

setlocal enabledelayedexpansion

set PYTHON_VERSION=3.12.8
set PYTHON_EMBED_URL=https://www.python.org/ftp/python/%PYTHON_VERSION%/python-%PYTHON_VERSION%-embed-amd64.zip
set PYTHON_DIR=python-embedded
set PIP_VERSION=25.3
set PIP_WHEEL_URL=https://files.pythonhosted.org/packages/py3/p/pip/pip-%PIP_VERSION%-py3-none-any.whl

echo [INFO] Configurando Python embarcado para o instalador...

REM Create directory for embedded Python
if not exist "%PYTHON_DIR%" mkdir "%PYTHON_DIR%"

REM Download Python embeddable
echo [INFO] Baixando Python %PYTHON_VERSION% embarcado...
powershell -Command "Invoke-WebRequest -Uri '%PYTHON_EMBED_URL%' -OutFile 'python-embed.zip'"

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Falha ao baixar Python embarcado
    exit /b 1
)

REM Extract Python
echo [INFO] Extraindo Python...
powershell -Command "Expand-Archive -Path 'python-embed.zip' -DestinationPath '%PYTHON_DIR%' -Force"

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Falha ao extrair Python
    exit /b 1
)

del python-embed.zip

REM Enable site-packages by uncommenting import site in python312._pth
echo [INFO] Habilitando site-packages...
cd "%PYTHON_DIR%"
for %%f in (python*._pth) do (
    powershell -Command "(Get-Content '%%f') -replace '#import site', 'import site' | Set-Content '%%f'"
)
cd ..

REM Install pip by unpacking its wheel into site-packages (get-pip.py is refused by pip's Windows self-modification guard)
echo [INFO] Baixando pip %PIP_VERSION%...
powershell -Command "Invoke-WebRequest -Uri '%PIP_WHEEL_URL%' -OutFile 'pip.whl'"

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Falha ao baixar pip
    exit /b 1
)

echo [INFO] Instalando pip...
powershell -Command "Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::ExtractToDirectory('pip.whl', '%PYTHON_DIR%\Lib\site-packages')"

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Falha ao instalar pip
    exit /b 1
)

del pip.whl

"%PYTHON_DIR%\python.exe" -m pip --version

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] pip nao funciona apos a instalacao
    exit /b 1
)

REM Install setuptools first (required for building some packages)
echo [INFO] Instalando setuptools e wheel...
"%PYTHON_DIR%\python.exe" -m pip install setuptools wheel

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Falha ao instalar setuptools
    exit /b 1
)

REM Install required packages
echo [INFO] Instalando dependencias Python de requirements.txt...
"%PYTHON_DIR%\python.exe" -m pip install -r requirements.txt

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Falha ao instalar dependencias Python
    exit /b 1
)

echo [SUCCESS] Python embarcado configurado com sucesso em %PYTHON_DIR%\
