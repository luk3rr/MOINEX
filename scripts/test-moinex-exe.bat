@echo off
REM Test Moinex.exe with error capture

echo Testing Moinex.exe...
echo.

set "INSTALL_DIR=C:\Program Files\Moinex"
if not exist "%INSTALL_DIR%" (
    set "INSTALL_DIR=%LOCALAPPDATA%\Moinex"
)

echo Installation directory: %INSTALL_DIR%
echo.

cd /d "%INSTALL_DIR%"

echo [INFO] Running Moinex.exe with error redirection...
echo [INFO] Log will be saved to: %TEMP%\moinex-exe-test.log
echo.

Moinex.exe > "%TEMP%\moinex-exe-test.log" 2>&1

echo.
echo [INFO] Exit code: %ERRORLEVEL%
echo.
echo [INFO] Log content:
type "%TEMP%\moinex-exe-test.log"
echo.

pause
