@echo off
setlocal EnableDelayedExpansion

echo ========================================
echo CRITICAL BUG FIX - RemoteViews Package
echo ========================================
echo.
echo BUG FOUND: RemoteViews was using wrong package name!
echo.
echo OLD CODE (BROKEN):
echo   RemoteViews(packageName, android.R.layout.simple_list_item_2)
echo   ^- packageName = "com.android.chrome" (target app)
echo.
echo NEW CODE (FIXED):
echo   RemoteViews(applicationContext.packageName, android.R.layout.simple_list_item_2)
echo   ^- applicationContext.packageName = "com.runanywhere.startup_hackathon20"
echo.
echo This was causing Android to look for the layout in Chrome's package
echo instead of SafeSphere's package, so the autofill UI couldn't be created!
echo.
echo ========================================
echo.

REM Find ADB
set "ADB_PATH="

if exist "C:\Users\JASWANTH\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
    set "ADB_PATH=C:\Users\JASWANTH\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    echo [OK] ADB found
) else (
    echo [ERROR] ADB not found! Please connect USB and try again.
    pause
    exit /b 1
)

echo.
echo [1/5] Checking device connection...
"!ADB_PATH!" devices | findstr "device$" > nul
if errorlevel 1 (
    echo [ERROR] No device connected!
    echo.
    echo Please:
    echo 1. Connect phone via USB
    echo 2. Enable USB Debugging
    echo 3. Authorize this computer on phone
    echo 4. Run this script again
    pause
    exit /b 1
)
echo [OK] Device connected

echo.
echo [2/5] Uninstalling old version...
"!ADB_PATH!" uninstall com.runanywhere.startup_hackathon20 > nul 2>&1
echo [OK] Done

echo.
echo [3/5] Installing NEW version with fix...
"!ADB_PATH!" install -r "D:\Hackathons\SafeSphere\Hackss-main\Hackss-main\Hackss-main\app\build\outputs\apk\debug\SafeSphere-v1.0.0-debug.apk"
if errorlevel 1 (
    echo [ERROR] Installation failed!
    pause
    exit /b 1
)
echo [OK] Installed successfully

echo.
echo [4/5] Rebooting device (REQUIRED for autofill to work)...
"!ADB_PATH!" reboot
echo [OK] Rebooting... (wait 30 seconds)
timeout /t 30 /nobreak > nul

echo.
echo [5/5] Waiting for device to come back online...
:wait_loop
"!ADB_PATH!" devices | findstr "device$" > nul
if errorlevel 1 (
    timeout /t 2 /nobreak > nul
    goto wait_loop
)
echo [OK] Device is back online!

echo.
echo ========================================
echo INSTALLATION COMPLETE!
echo ========================================
echo.
echo NOW TEST:
echo 1. Settings -^> System -^> Languages ^& input -^> Autofill service
echo 2. Select "SafeSphere Autofill"
echo 3. Open Chrome
echo 4. Go to github.com/login
echo 5. Tap on username field
echo 6. AUTOFILL SHOULD NOW APPEAR! ✓
echo.
echo ========================================
pause
