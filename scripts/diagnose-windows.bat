@echo off
REM Filename: diagnose-windows.bat
REM Script to diagnose Moinex installation issues on Windows

echo ========================================
echo Moinex Windows Diagnostic Tool
echo ========================================
echo.

REM Check installation directory
set "INSTALL_DIR=C:\Program Files\Moinex"
if not exist "%INSTALL_DIR%" (
    set "INSTALL_DIR=%LOCALAPPDATA%\Moinex"
)

echo [INFO] Installation directory: %INSTALL_DIR%
echo.

REM Check directory structure
echo [INFO] Checking directory structure...
if exist "%INSTALL_DIR%\app" (
    echo [OK] app\ directory exists
) else (
    echo [ERROR] app\ directory NOT found
)

if exist "%INSTALL_DIR%\runtime" (
    echo [OK] runtime\ directory exists
) else (
    echo [ERROR] runtime\ directory NOT found
)

if exist "%INSTALL_DIR%\python-embedded" (
    echo [OK] python-embedded\ directory exists
) else (
    echo [ERROR] python-embedded\ directory NOT found
)
echo.

REM Check JAR file
echo [INFO] Checking JAR file...
if exist "%INSTALL_DIR%\app\moinex.jar" (
    echo [OK] moinex.jar found
) else (
    echo [ERROR] moinex.jar NOT found
)
echo.

REM Check Python
echo [INFO] Checking Python...
if exist "%INSTALL_DIR%\python-embedded\python.exe" (
    echo [OK] python.exe found
    "%INSTALL_DIR%\python-embedded\python.exe" --version
) else (
    echo [ERROR] python.exe NOT found
)
echo.

REM Check Java
echo [INFO] Checking Java runtime...
if exist "%INSTALL_DIR%\runtime\bin\java.exe" (
    echo [OK] java.exe found
    "%INSTALL_DIR%\runtime\bin\java.exe" -version
) else (
    echo [ERROR] java.exe NOT found
)
echo.

REM Try to run with verbose output
echo [INFO] Attempting to run Moinex with verbose logging...
echo [INFO] Output will be saved to: %TEMP%\moinex-debug.log
echo.

cd /d "%INSTALL_DIR%"

if exist "Moinex.exe" (
    echo [INFO] Running Moinex.exe...
    Moinex.exe > "%TEMP%\moinex-debug.log" 2>&1
    
    echo.
    echo [INFO] Log file created at: %TEMP%\moinex-debug.log
    echo [INFO] Content:
    type "%TEMP%\moinex-debug.log"
) else (
    echo [ERROR] Moinex.exe not found in %INSTALL_DIR%
)

echo.
echo ========================================
echo Diagnostic complete
echo ========================================
pause
