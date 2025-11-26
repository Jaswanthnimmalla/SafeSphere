@echo off
echo ========================================
echo SafeSphere - Quick Update for Realme
echo ========================================
echo.

echo Checking for Realme device...
adb devices
echo.

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: ADB not found!
    echo Please install ADB Platform Tools first.
    pause
    exit /b 1
)

echo Building latest version...
call gradlew assembleDebug
echo.

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo Installing update to Realme device...
echo (Your data will be preserved)
adb install -r app\build\outputs\apk\debug\app-debug.apk
echo.

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Installation failed. Trying clean install...
    echo Uninstalling old version...
    adb uninstall com.runanywhere.startup_hackathon20
    echo.
    echo Installing fresh...
    adb install app\build\outputs\apk\debug\app-debug.apk
    echo.
)

echo ========================================
echo Update Complete!
echo ========================================
echo.
echo SafeSphere has been updated on your Realme device!
echo.
echo Important Realme/ColorOS Settings:
echo 1. Disable battery optimization for SafeSphere
echo 2. Lock app in recent apps (tap lock icon)
echo 3. For file sharing: Disable WiFi+ in WiFi settings
echo.
echo New features in this update:
echo - Fixed permission handling (no more settings popup!)
echo - Real WiFi Direct device discovery
echo - Better error messages
echo - Improved user guidance
echo.
pause
