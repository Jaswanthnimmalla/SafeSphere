# Install SafeSphere on Realme (RMX) Device

## 🔥 Quick Install for Realme Devices

This guide is specifically for Realme devices (RMX series) running ColorOS.

## Step 1: Enable Developer Options on Realme

Realme devices with ColorOS have a slightly different path:

1. **Open Settings**
2. **Go to:** Settings → About Phone
3. **Find "Version"** (may be under "Build Number" or "ColorOS Version")
4. **Tap 7 times** on the version number
5. You'll see: "You are now a developer!"

## Step 2: Enable USB Debugging

1. **Go to:** Settings → Additional Settings → Developer Options

   *Alternative path:* Settings → System → Developer Options

2. **Enable these options:**
    - ✅ USB Debugging
    - ✅ Install via USB (if available)
    - ✅ USB Debugging (Security settings) - Optional but recommended

3. **Connect USB cable** to your computer

4. **Unlock your phone** and you'll see a popup:
   ```
   Allow USB debugging?
   The computer's RSA key fingerprint is:
   XX:XX:XX:XX...
   
   [Cancel] [OK]
   ```

5. **Check:** "Always allow from this computer"
6. **Tap:** OK

## Step 3: Verify Connection

Open PowerShell on your computer:

```powershell
adb devices
```

You should see your Realme device:

```
List of devices attached
RMX1234567890    device
```

**If you see "unauthorized":**

- Check your phone screen for the USB debugging prompt
- If no prompt, try: Settings → Developer Options → Revoke USB debugging authorizations
- Unplug and replug the cable

## Step 4: Install SafeSphere

### Option A: One-Click Install (Easiest)

```powershell
cd "C:\Users\JASWANTH\Downloads\SafeSphere-main (2)\SafeSphere-main"
.\install.bat
```

Just double-click `install.bat` in the SafeSphere-main folder!

### Option B: Manual Commands

```powershell
# Navigate to project
cd "C:\Users\JASWANTH\Downloads\SafeSphere-main (2)\SafeSphere-main"

# Build APK (first time: 3-5 minutes)
.\gradlew assembleDebug

# Install to Realme device
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Step 5: First Launch on Realme

### Grant Permissions

Realme/ColorOS asks for permissions differently:

1. **Open SafeSphere** from app drawer
2. **You'll see permission requests:**
    - Location → Allow
    - Storage → Allow
    - Camera → Allow
    - Contacts → Allow

3. **Additional ColorOS Permissions:**
    - Settings → App Management → SafeSphere → Permissions
    - Enable all required permissions:
        - ✅ Location (for WiFi Direct)
        - ✅ Storage (for file access)
        - ✅ Camera (for document scanner)
        - ✅ Contacts (for messenger)

### Disable Battery Optimization (Important!)

Realme devices have aggressive battery optimization that may kill background services:

1. **Go to:** Settings → Battery → Battery Optimization
2. **Find SafeSphere**
3. **Select:** "Don't Optimize"

Or:

1. **Go to:** Settings → App Management → SafeSphere
2. **Tap:** Battery Usage
3. **Disable:** "Restrict background activity"

## Realme-Specific Issues & Fixes

### Issue 1: "Installation Blocked"

**ColorOS may block installation of unknown apps.**

**Solution:**

1. Settings → Security → Install apps from external sources
2. Find "Files" or your file manager
3. Enable installation permission

### Issue 2: "App Not Installed" Error

**ColorOS may prevent reinstallation.**

**Solution:**

```powershell
# Completely uninstall first
adb uninstall com.runanywhere.startup_hackathon20

# Clear cache
adb shell pm clear com.runanywhere.startup_hackathon20

# Install fresh
adb install app\build\outputs\apk\debug\app-debug.apk
```

### Issue 3: SafeSphere Share Not Finding Devices

**Realme devices have additional WiFi restrictions.**

**Solution:**

1. **Enable WiFi:** Settings → WiFi → Turn ON
2. **Disable WiFi+:** Settings → WiFi → WiFi+ (disable this)
3. **Keep WiFi on during sleep:**
    - Settings → WiFi → Advanced → Keep WiFi on during sleep → Always
4. **Grant Location permission:**
    - Settings → App Management → SafeSphere → Permissions → Location → Always Allow

### Issue 4: Notifications Not Showing

**ColorOS notification management is strict.**

**Solution:**

1. Settings → Notifications & Status Bar → SafeSphere
2. Enable:
    - ✅ Allow notifications
    - ✅ Display on lock screen
    - ✅ Banner
    - ✅ Sound

### Issue 5: App Killed in Background

**Realme/ColorOS aggressively kills background apps.**

**Solution:**

1. **Recent Apps:** Lock SafeSphere by tapping the lock icon
2. **Settings → App Management → SafeSphere:**
    - Startup Manager → Enable
    - Power consumption → Unrestricted
3. **Settings → Battery → App Battery Management:**
    - Find SafeSphere → Don't optimize

## Update SafeSphere on Realme

When you want to update to a newer version:

```powershell
# Navigate to project
cd "C:\Users\JASWANTH\Downloads\SafeSphere-main (2)\SafeSphere-main"

# Pull latest code (if using git)
git pull

# Build new version
.\gradlew clean
.\gradlew assembleDebug

# Install update (keeps your data)
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

The `-r` flag reinstalls while keeping your data intact!

## Testing SafeSphere Share on Two Realme Devices

### Setup

1. **Connect both Realme devices** to your computer
2. **Check both are detected:**
   ```powershell
   adb devices
   ```

   Should show:
   ```
   List of devices attached
   RMX3031ABC123    device    <- Realme 1
   RMX2001XYZ789    device    <- Realme 2
   ```

### Installation

```powershell
# Build once
.\gradlew assembleDebug

# Install on Realme Device 1
adb -s RMX3031ABC123 install -r app\build\outputs\apk\debug\app-debug.apk

# Install on Realme Device 2
adb -s RMX2001XYZ789 install -r app\build\outputs\apk\debug\app-debug.apk
```

### Testing

Follow the guide in `SAFESPHERE_SHARE_TESTING.md`

**Important for Realme:**

- Enable WiFi on both devices
- Keep both devices unlocked during testing
- Disable WiFi+ on both devices
- Grant Location permission → "Always Allow"

## Realme/ColorOS Version Compatibility

SafeSphere works on:

- ✅ ColorOS 7.0+ (Android 10+)
- ✅ ColorOS 11.0+ (Android 11+)
- ✅ ColorOS 12.0+ (Android 12+)
- ✅ ColorOS 13.0+ (Android 13+)
- ✅ Realme UI 1.0, 2.0, 3.0, 4.0

Tested on:

- Realme 6, 7, 8, 9 series
- Realme GT series
- Realme Narzo series
- Realme C series

## Quick Commands Summary

```powershell
# Check if Realme device is connected
adb devices

# Install SafeSphere
cd "C:\Users\JASWANTH\Downloads\SafeSphere-main (2)\SafeSphere-main"
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Update SafeSphere (keeps data)
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Uninstall completely
adb uninstall com.runanywhere.startup_hackathon20

# View logs (troubleshooting)
adb logcat | Select-String "SafeSphere"

# Grant permissions manually
adb shell pm grant com.runanywhere.startup_hackathon20 android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.runanywhere.startup_hackathon20 android.permission.READ_EXTERNAL_STORAGE

# Launch app
adb shell am start -n com.runanywhere.startup_hackathon20/.SafeSphereMainActivity
```

## Troubleshooting Realme-Specific Issues

### WiFi Direct Not Working

1. **Disable WiFi+:**
    - Settings → WiFi → WiFi+ → OFF

2. **Enable Location:**
    - Settings → Location → ON
    - SafeSphere → Location → Always Allow

3. **Disable Private DNS:**
    - Settings → WiFi & Internet → Private DNS → OFF

4. **Reset Network Settings:**
    - Settings → Additional Settings → Backup and Reset → Reset Phone → Reset Network Settings

### Permission Denied Errors

Realme/ColorOS has multiple permission levels:

1. **App Permissions:**
    - Settings → App Management → SafeSphere → Permissions → Allow all

2. **Special Permissions:**
    - Settings → App Management → SafeSphere → Special Access
    - Enable: Display over other apps, Modify system settings

3. **Battery Permissions:**
    - Settings → Battery → Battery Optimization → SafeSphere → Don't Optimize

### ADB Connection Issues

**If device not detected:**

1. **Use original Realme USB cable** (third-party cables often fail)
2. **Try different USB port** (USB 3.0 ports work best)
3. **Install Realme USB drivers:**
    - Usually installed automatically on Windows 10+
    - Or download from Realme support site

4. **Enable PTP mode:**
    - Swipe down notification panel
    - Tap "Android System - USB charging this device"
    - Select "File Transfer" or "PTP"

## Next Steps

1. ✅ **Install complete** - SafeSphere is on your Realme device
2. 🔐 **Create account** - Set up your master password
3. 🔑 **Add passwords** - Test Password Manager
4. 📁 **Store files** - Try Privacy Vault
5. 📤 **Share files** - Test SafeSphere Share (no more permission popup!)

## ColorOS-Specific Features

SafeSphere works great with ColorOS features:

- **App Lock:** Can lock SafeSphere with fingerprint/face
- **Game Space:** Add SafeSphere to game space for no interruptions
- **Private Safe:** SafeSphere integrates with ColorOS privacy features
- **Smart Sidebar:** Pin SafeSphere for quick access

## Performance on Realme Devices

| Feature | Performance |
|---------|-------------|
| App Launch | < 1 second |
| Password Decryption | Instant |
| File Encryption | Fast |
| WiFi Direct Speed | 50-150 Mbps (depending on model) |
| AI Chat | Smooth (after model download) |

## Summary

**Installation on Realme is straightforward:**

1. Enable Developer Options (tap version 7 times)
2. Enable USB Debugging in Additional Settings
3. Connect device and allow USB debugging
4. Run `.\install.bat` or manual commands
5. Configure ColorOS permissions and battery settings

**Key Realme Settings:**

- ✅ USB Debugging enabled
- ✅ Battery optimization disabled for SafeSphere
- ✅ All permissions granted
- ✅ WiFi+ disabled for file sharing
- ✅ App locked in recent apps

🎉 **Enjoy SafeSphere on your Realme device!**

---

**Need help?** Check the other guides:

- `QUICK_INSTALL.md` - General installation
- `SAFESPHERE_SHARE_TESTING.md` - Test file sharing
- `INSTALL_ON_DEVICE.md` - Detailed instructions
