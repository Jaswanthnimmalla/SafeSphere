@echo off
setlocal EnableDelayedExpansion
echo ========================================
echo SafeSphere Autofill Complete Fix
echo (Searching for ADB automatically)
echo ========================================
echo.

echo Step 1: Locating ADB...

set "ADB_PATH="

REM Check common Android Studio locations
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set "ADB_PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    echo Found ADB in: %LOCALAPPDATA%\Android\Sdk\platform-tools\
    goto :adb_found
)

if exist "%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
    set "ADB_PATH=%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    echo Found ADB in: %USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\
    goto :adb_found
)

if exist "C:\Users\%USERNAME%\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
    set "ADB_PATH=C:\Users\%USERNAME%\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    echo Found ADB in: C:\Users\%USERNAME%\AppData\Local\Android\Sdk\platform-tools\
    goto :adb_found
)

if exist "C:\Android\Sdk\platform-tools\adb.exe" (
    set "ADB_PATH=C:\Android\Sdk\platform-tools\adb.exe"
    echo Found ADB in: C:\Android\Sdk\platform-tools\
    goto :adb_found
)

if exist "C:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe" (
    set "ADB_PATH=C:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe"
    echo Found ADB in: C:\Program Files (x86)\Android\android-sdk\platform-tools\
    goto :adb_found
)

:adb_found
if not defined ADB_PATH (
    echo ERROR: Could not find ADB automatically!
    echo.
    echo Please manually provide the ADB path.
    echo Common locations:
    echo   - %LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
    echo   - C:\Users\YourUsername\AppData\Local\Android\Sdk\platform-tools\adb.exe
    echo.
    set /p ADB_PATH="Enter full path to adb.exe (or press Ctrl+C to exit): "
    if not exist "!ADB_PATH!" (
        echo ERROR: File not found at: !ADB_PATH!
        pause
        exit /b 1
    )
)

echo ADB found! [OK]
echo.

echo Step 2: Checking if device is connected...
"!ADB_PATH!" devices > temp_devices.txt
findstr "device$" temp_devices.txt > nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: No device connected!
    echo.
    echo Please:
    echo 1. Connect your Android device via USB
    echo 2. Enable USB Debugging: Settings → About Phone → Tap "Build Number" 7 times
    echo 3. Then: Settings → Developer Options → Enable "USB Debugging"
    echo 4. Authorize this computer on your device when prompted
    del temp_devices.txt
    pause
    exit /b 1
)
del temp_devices.txt
echo Device connected! [OK]
echo.

echo Step 3: Checking Android version...
"!ADB_PATH!" shell getprop ro.build.version.sdk > temp_api.txt
set /p API_LEVEL=<temp_api.txt
del temp_api.txt
set API_LEVEL=%API_LEVEL: =%
echo Android API Level: %API_LEVEL%
if %API_LEVEL% LSS 26 (
    echo ERROR: Android version too old! Requires Android 8.0+ (API 26+)
    exit /b 1
)
echo Android version OK! [OK]
echo.

echo Step 4: Uninstalling old version...
"!ADB_PATH!" uninstall com.runanywhere.startup_hackathon20 >nul 2>&1
echo Old version removed! [OK]
echo.

echo Step 5: Clearing autofill settings...
"!ADB_PATH!" shell settings put secure autofill_service null
echo Settings cleared! [OK]
echo.

echo Step 6: Building new APK with all fixes...
echo This may take 2-3 minutes...
call gradlew.bat clean assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)
echo Build successful! [OK]
echo.

echo Step 7: Installing new version...
"!ADB_PATH!" install -r app\build\outputs\apk\debug\SafeSphere-v1.0.0-debug.apk
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Installation failed!
    pause
    exit /b 1
)
echo Installation successful! [OK]
echo.

echo Step 8: Enabling autofill service via ADB...
"!ADB_PATH!" shell settings put secure autofill_service com.runanywhere.startup_hackathon20/.autofill.SafeSphereAutofillService
echo Service enabled! [OK]
echo.

echo Step 9: Verifying service is enabled...
"!ADB_PATH!" shell settings get secure autofill_service > temp_service.txt
set /p AUTOFILL_SERVICE=<temp_service.txt
del temp_service.txt
echo Current autofill service: !AUTOFILL_SERVICE!
echo.

echo Step 10: Rebooting device...
echo Please wait for device to reboot...
"!ADB_PATH!" reboot
echo Waiting for device to come back online...
timeout /t 5 /nobreak >nul
"!ADB_PATH!" wait-for-device
timeout /t 10 /nobreak >nul
echo Device online! [OK]
echo.

echo Step 11: Starting log monitor...
echo.
echo ========================================
echo Monitoring SafeSphere Autofill Service
echo ========================================
echo.
echo IMPORTANT: Watch for this message:
echo   " SafeSphere Autofill Service CONNECTED - Ready to autofill!"
echo.
echo If you see it, the service is working!
echo.
echo Now:
echo 1. Open Chrome on your phone
echo 2. Go to: https://github.com/login
echo 3. Tap on the username field
echo 4. Watch this console for autofill logs
echo.
echo Press Ctrl+C to stop monitoring
echo ========================================
echo.

"!ADB_PATH!" logcat -c
"!ADB_PATH!" logcat -s SafeSphereAutofill:* -v time
