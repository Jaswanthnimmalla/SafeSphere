@echo off
echo ========================================
echo SafeSphere Autofill Complete Fix
echo ========================================
echo.

echo Step 1: Checking if ADB is available...
where adb >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: ADB not found in PATH!
    echo Please install Android SDK Platform Tools first.
    echo Download from: https://developer.android.com/studio/releases/platform-tools
    pause
    exit /b 1
)
echo ADB found! ✓
echo.

echo Step 2: Checking if device is connected...
adb devices | findstr "device$" >nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: No device connected!
    echo Please connect your Android device via USB.
    echo Enable USB Debugging in Developer Options.
    pause
    exit /b 1
)
echo Device connected! ✓
echo.

echo Step 3: Checking Android version...
for /f "tokens=*" %%a in ('adb shell getprop ro.build.version.sdk') do set API_LEVEL=%%a
echo Android API Level: %API_LEVEL%
if %API_LEVEL% LSS 26 (
    echo ERROR: Android version too old! Requires Android 8.0+ (API 26+)
    pause
    exit /b 1
)
echo Android version OK! ✓
echo.

echo Step 4: Uninstalling old version...
adb uninstall com.runanywhere.startup_hackathon20 >nul 2>&1
echo Old version removed! ✓
echo.

echo Step 5: Clearing autofill settings...
adb shell settings put secure autofill_service null
echo Settings cleared! ✓
echo.

echo Step 6: Building new APK with all fixes...
echo This may take 2-3 minutes...
call gradlew.bat clean assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)
echo Build successful! ✓
echo.

echo Step 7: Installing new version...
adb install -r app\build\outputs\apk\debug\*.apk
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Installation failed!
    pause
    exit /b 1
)
echo Installation successful! ✓
echo.

echo Step 8: Enabling autofill service via ADB...
adb shell settings put secure autofill_service com.runanywhere.startup_hackathon20/.autofill.SafeSphereAutofillService
echo.

echo Step 9: Verifying service is enabled...
for /f "tokens=*" %%a in ('adb shell settings get secure autofill_service') do set AUTOFILL_SERVICE=%%a
echo Current autofill service: %AUTOFILL_SERVICE%
echo.

echo Step 10: Rebooting device...
echo Please wait for device to reboot...
adb reboot
echo Waiting for device to come back online...
timeout /t 5 /nobreak >nul
adb wait-for-device
echo Device online! ✓
echo.

echo Step 11: Starting log monitor...
echo.
echo ========================================
echo Monitoring SafeSphere Autofill Service
echo ========================================
echo.
echo IMPORTANT: Watch for this message:
echo   "✅ SafeSphere Autofill Service CONNECTED - Ready to autofill!"
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

adb logcat -c
adb logcat -s SafeSphereAutofill:* -v time
