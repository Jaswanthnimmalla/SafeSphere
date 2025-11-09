@echo off
setlocal EnableDelayedExpansion

echo ========================================
echo SafeSphere Autofill - Simple Installer
echo ========================================
echo.

REM Find ADB
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist "!ADB!" (
    set "ADB=%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe"
)
if not exist "!ADB!" (
    echo ERROR: Cannot find ADB!
    pause
    exit /b 1
)

echo [1/10] ADB found: !ADB!
echo.

echo [2/10] Uninstalling old version...
"!ADB!" uninstall com.runanywhere.startup_hackathon20 >nul 2>&1
echo Done!
echo.

echo [3/10] Clearing autofill settings...
"!ADB!" shell settings put secure autofill_service null
echo Done!
echo.

echo [4/10] Building new APK (this takes 2-3 minutes)...
call gradlew.bat clean assembleDebug
if errorlevel 1 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)
echo Done!
echo.

echo [5/10] Installing new version...
"!ADB!" install -r app\build\outputs\apk\debug\SafeSphere-v1.0.0-debug.apk
if errorlevel 1 (
    echo ERROR: Installation failed!
    pause
    exit /b 1
)
echo Done!
echo.

echo [6/10] Enabling autofill service...
"!ADB!" shell settings put secure autofill_service com.runanywhere.startup_hackathon20/.autofill.SafeSphereAutofillService
echo Done!
echo.

echo [7/10] Verifying autofill is enabled...
"!ADB!" shell settings get secure autofill_service
echo.

echo [8/10] Rebooting device...
"!ADB!" reboot
echo Waiting 15 seconds for reboot...
timeout /t 15 /nobreak >nul
"!ADB!" wait-for-device
echo Done!
echo.

echo [9/10] Checking service status...
timeout /t 5 /nobreak >nul
echo.

echo [10/10] Starting log monitor...
echo.
echo ========================================
echo WATCH FOR THIS MESSAGE:
echo "SafeSphere Autofill Service CONNECTED"
echo ========================================
echo.
echo If you see "CONNECTED", autofill is working!
echo.
echo Now test:
echo 1. Open Chrome on phone
echo 2. Go to github.com/login
echo 3. Tap username field
echo 4. Autofill should appear!
echo.
echo Press Ctrl+C to stop logs
echo ========================================
echo.

"!ADB!" logcat -c
"!ADB!" logcat -s SafeSphereAutofill:* -v time
