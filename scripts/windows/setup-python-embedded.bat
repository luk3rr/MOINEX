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
set GET_PIP_URL=https://bootstrap.pypa.io/get-pip.py

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

REM Download get-pip.py
echo [INFO] Baixando get-pip.py...
powershell -Command "Invoke-WebRequest -Uri '%GET_PIP_URL%' -OutFile '%PYTHON_DIR%\get-pip.py'"

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Falha ao baixar get-pip.py
    exit /b 1
)

REM Install pip
echo [INFO] Instalando pip...
"%PYTHON_DIR%\python.exe" "%PYTHON_DIR%\get-pip.py"

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Falha ao instalar pip
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
echo [INFO] Instalando dependencias Python (requests, yfinance, pandas)...
"%PYTHON_DIR%\python.exe" -m pip install requests==2.32.3 yfinance==0.2.54 pandas==2.2.3

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Falha ao instalar dependencias Python
    exit /b 1
)

REM Clean up get-pip.py
del "%PYTHON_DIR%\get-pip.py"

echo [SUCCESS] Python embarcado configurado com sucesso em %PYTHON_DIR%\
