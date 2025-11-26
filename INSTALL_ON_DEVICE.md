# Install SafeSphere on Android Device

## Prerequisites

### 1. Install Android Debug Bridge (ADB)

ADB is required to install apps from your computer to your Android device.

#### Option A: Install Android Studio (Recommended)

1. Download Android Studio from: https://developer.android.com/studio
2. Install Android Studio
3. ADB will be located at: `C:\Users\<YourUsername>\AppData\Local\Android\Sdk\platform-tools\`

#### Option B: Install Platform Tools Only (Lightweight)

1. Download Platform Tools from: https://developer.android.com/studio/releases/platform-tools
2. Extract the ZIP file to a folder (e.g., `C:\platform-tools\`)
3. Add the folder to your system PATH:
    - Right-click "This PC" → Properties → Advanced System Settings
    - Click "Environment Variables"
    - Under "System variables", find "Path" and click "Edit"
    - Click "New" and add: `C:\platform-tools\`
    - Click OK on all windows
4. Restart PowerShell/Command Prompt

### 2. Enable USB Debugging on Your Android Device

1. **Enable Developer Options:**
    - Go to Settings → About Phone
    - Tap "Build Number" 7 times
    - You'll see "You are now a developer!"

2. **Enable USB Debugging:**
    - Go to Settings → System → Developer Options
    - Turn on "USB Debugging"
    - (Optional) Turn on "Install via USB" for easier installation

3. **Connect Your Device:**
    - Connect your Android device to your computer via USB cable
    - On your phone, you'll see a prompt "Allow USB debugging?"
    - Check "Always allow from this computer"
    - Tap "Allow" or "OK"

### 3. Verify ADB Connection

Open PowerShell or Command Prompt and run:

```powershell
adb devices
```

You should see something like:

```
List of devices attached
1234567890ABCDEF    device
```

If you see "unauthorized", check your phone for the USB debugging prompt.

## Installation Methods

### Method 1: Build and Install (Recommended - Latest Code)

This method builds the APK from source code with all the latest fixes.

#### Step 1: Navigate to Project Directory

```powershell
cd "C:\Users\JASWANTH\Downloads\SafeSphere-main (2)\SafeSphere-main"
```

#### Step 2: Clean Previous Builds (Optional)

```powershell
.\gradlew clean
```

#### Step 3: Build Debug APK

```powershell
.\gradlew assembleDebug
```

This will take 2-5 minutes on first build (downloads dependencies).

The APK will be created at:

```
app\build\outputs\apk\debug\app-debug.apk
```

#### Step 4: Install to Connected Device

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

The `-r` flag reinstalls the app if it's already installed.

### Method 2: Direct Install (If APK Already Exists)

If you already have the APK file:

```powershell
adb install -r path\to\app-debug.apk
```

### Method 3: Install on Multiple Devices

If you have multiple devices connected:

```powershell
# List all devices
adb devices

# Install on specific device
adb -s <device_serial> install -r app\build\outputs\apk\debug\app-debug.apk
```

Example:

```powershell
adb -s 1234567890ABCDEF install -r app\build\outputs\apk\debug\app-debug.apk
```

## Quick Install Script

Save this as `install.bat` in the SafeSphere-main folder:

```batch
@echo off
echo ========================================
echo SafeSphere - Quick Install Script
echo ========================================
echo.

echo Step 1: Checking connected devices...
adb devices
echo.

echo Step 2: Building APK (this may take a few minutes)...
call gradlew assembleDebug
echo.

echo Step 3: Installing to device...
adb install -r app\build\outputs\apk\debug\app-debug.apk
echo.

echo ========================================
echo Installation Complete!
echo ========================================
echo.
echo SafeSphere should now be installed on your device.
echo Look for the SafeSphere icon in your app drawer.
echo.
pause
```

Then just double-click `install.bat` to build and install!

## Installing on TWO Devices (For Testing SafeSphere Share)

To test the file sharing feature, you need SafeSphere on two devices:

### Step 1: Connect Both Devices

Connect both devices via USB and verify:

```powershell
adb devices
```

Should show:

```
List of devices attached
DEVICE1_SERIAL    device
DEVICE2_SERIAL    device
```

### Step 2: Build Once

```powershell
.\gradlew assembleDebug
```

### Step 3: Install on Device 1

```powershell
adb -s DEVICE1_SERIAL install -r app\build\outputs\apk\debug\app-debug.apk
```

### Step 4: Install on Device 2

```powershell
adb -s DEVICE2_SERIAL install -r app\build\outputs\apk\debug\app-debug.apk
```

### Step 5: Test File Sharing

Follow the guide in `SAFESPHERE_SHARE_TESTING.md`

## Troubleshooting

### "adb is not recognized"

**Solution:**

- ADB is not installed or not in PATH
- Install Platform Tools (see Prerequisites)
- Add to PATH and restart PowerShell
- Or use full path: `C:\platform-tools\adb.exe devices`

### "error: device unauthorized"

**Solution:**

- Check your phone for USB debugging authorization prompt
- If no prompt appears:
    - Revoke USB debugging authorizations: Settings → Developer Options → Revoke USB debugging
      authorizations
    - Unplug and replug the USB cable
    - Accept the new authorization prompt

### "error: device offline"

**Solution:**

- Unplug and replug USB cable
- Restart ADB: `adb kill-server` then `adb start-server`
- Restart your phone
- Try a different USB cable or port

### "INSTALL_FAILED_UPDATE_INCOMPATIBLE"

**Solution:**

- Uninstall the existing app first:
  ```powershell
  adb uninstall com.runanywhere.startup_hackathon20
  ```
- Then install again:
  ```powershell
  adb install app\build\outputs\apk\debug\app-debug.apk
  ```

### "Gradle build failed"

**Solution:**

- Make sure you have JDK 17 installed
- Check `local.properties` has correct SDK path
- Run: `.\gradlew --version` to verify Gradle setup
- Try: `.\gradlew clean` then `.\gradlew assembleDebug`

### Build takes too long

**First build:** 2-5 minutes (downloads dependencies) - Normal
**Subsequent builds:** 30 seconds - 2 minutes - Normal

If it takes longer:

- Check your internet connection (downloads dependencies)
- Disable antivirus temporarily
- Close other heavy applications

### "No connected devices"

**Solution:**

- Make sure USB debugging is enabled
- Try a different USB cable (some cables are charge-only)
- Try a different USB port on your computer
- Install phone's USB drivers (usually automatic on Windows 10+)

## Uninstalling SafeSphere

### From Device:

- Long press SafeSphere icon → App Info → Uninstall

### Via ADB:

```powershell
adb uninstall com.runanywhere.startup_hackathon20
```

## Updating SafeSphere

When you make code changes:

```powershell
# Build new APK
.\gradlew assembleDebug

# Install (overwrites existing app, keeps data)
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Useful ADB Commands

```powershell
# Check connected devices
adb devices

# Install app
adb install -r app-debug.apk

# Uninstall app
adb uninstall com.runanywhere.startup_hackathon20

# View live logs
adb logcat | Select-String "SafeSphere"

# Take screenshot
adb shell screencap /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Clear app data (reset app)
adb shell pm clear com.runanywhere.startup_hackathon20

# Grant permission manually
adb shell pm grant com.runanywhere.startup_hackathon20 android.permission.ACCESS_FINE_LOCATION

# Launch app
adb shell am start -n com.runanywhere.startup_hackathon20/.SafeSphereMainActivity

# Restart ADB server
adb kill-server
adb start-server
```

## Release Build (For Production)

For a production-ready APK with optimizations:

```powershell
.\gradlew assembleRelease
```

APK will be at: `app\build\outputs\apk\release\app-release-unsigned.apk`

Note: Release APKs need to be signed before installation.

## Next Steps After Installation

1. **Open SafeSphere** on your device
2. **Grant permissions** when prompted (Biometric, Storage, Location, etc.)
3. **Create account** or login
4. **Test features:**
    - Privacy Vault - Store encrypted files
    - Password Manager - Save passwords securely
    - SafeSphere Share - Share files with nearby devices
    - Offline Messenger - Chat without internet
    - AI Chat - Talk to offline AI

5. **For SafeSphere Share:**
    - Install on second device
    - See `SAFESPHERE_SHARE_TESTING.md` for testing guide

## Support

If you encounter issues:

- Check `SAFESPHERE_SHARE_FIXES.md` for known issues
- Review logcat: `adb logcat | Select-String "SafeSphere"`
- Ensure Android version is 7.0+ (API 24+)
- Check device has WiFi capability for file sharing

---

**Quick Start Command (Copy & Paste):**

```powershell
cd "C:\Users\JASWANTH\Downloads\SafeSphere-main (2)\SafeSphere-main"
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

**Estimated Time:**

- First build: 3-5 minutes
- Installation: 10-30 seconds
- Total: ~5 minutes

🎉 **Your SafeSphere app will be ready to use!**
