@echo off
echo ========================================
echo SafeSphere - Quick Install Script
echo ========================================
echo.

echo Step 1: Checking connected devices...
adb devices
echo.

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: ADB not found!
    echo.
    echo Please install ADB Platform Tools:
    echo https://developer.android.com/studio/releases/platform-tools
    echo.
    echo Or install Android Studio:
    echo https://developer.android.com/studio
    echo.
    pause
    exit /b 1
)

echo Step 2: Building APK (this may take a few minutes on first build)...
call gradlew assembleDebug
echo.

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed!
    echo.
    echo Try these solutions:
    echo 1. Make sure you have JDK 17 installed
    echo 2. Check your internet connection (downloads dependencies)
    echo 3. Run: gradlew clean
    echo 4. Try again
    echo.
    pause
    exit /b 1
)

echo Step 3: Installing to device...
adb install -r app\build\outputs\apk\debug\app-debug.apk
echo.

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Installation failed!
    echo.
    echo Common solutions:
    echo 1. Check if device is connected: adb devices
    echo 2. Enable USB debugging on your phone
    echo 3. Accept USB debugging prompt on your phone
    echo 4. If "INSTALL_FAILED_UPDATE_INCOMPATIBLE", uninstall first:
    echo    adb uninstall com.runanywhere.startup_hackathon20
    echo.
    pause
    exit /b 1
)

echo ========================================
echo Installation Complete!
echo ========================================
echo.
echo SafeSphere has been installed on your device!
echo Look for the SafeSphere icon in your app drawer.
echo.
echo Next steps:
echo 1. Open SafeSphere on your device
echo 2. Grant permissions when prompted
echo 3. Create an account or login
echo 4. Start using SafeSphere!
echo.
echo For SafeSphere Share testing:
echo - Install on a second device
echo - See SAFESPHERE_SHARE_TESTING.md for guide
echo.
pause
