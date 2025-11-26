# 🚀 Quick Install - SafeSphere

## ⚡ Fastest Way to Install (3 Steps)

### Step 1: Setup ADB (One-Time Setup)

**Don't have ADB?** Download here: https://developer.android.com/studio/releases/platform-tools

**Quick check if you have ADB:**

```powershell
adb version
```

### Step 2: Enable USB Debugging on Phone

1. Settings → About Phone → Tap "Build Number" 7 times
2. Settings → Developer Options → Enable "USB Debugging"
3. Connect phone via USB cable
4. Accept "Allow USB debugging?" prompt on phone

### Step 3: Build & Install

**Option A: Use the batch script (Easiest)**

```powershell
cd "SafeSphere-main"
.\install.bat
```

Just double-click `install.bat` and it does everything!

**Option B: Manual commands**

```powershell
cd "SafeSphere-main"
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## ✅ That's It!

SafeSphere is now installed on your device! 🎉

Look for the **SafeSphere** icon in your app drawer.

## 📱 First Time Setup

1. Open SafeSphere
2. Grant permissions (Location, Storage, Camera, etc.)
3. Create account or login
4. Start using!

## 🔥 Testing SafeSphere Share Feature

Need to test file sharing? Install on **TWO devices**:

```powershell
# Build once
.\gradlew assembleDebug

# Install on Device 1
adb -s <device1_serial> install -r app\build\outputs\apk\debug\app-debug.apk

# Install on Device 2  
adb -s <device2_serial> install -r app\build\outputs\apk\debug\app-debug.apk
```

Get device serials with: `adb devices`

Then follow: `SAFESPHERE_SHARE_TESTING.md`

## 🐛 Common Issues

| Issue | Solution |
|-------|----------|
| "adb not recognized" | Install Platform Tools and add to PATH |
| "device unauthorized" | Check phone for USB debugging prompt |
| "device offline" | Unplug and replug USB cable |
| Build fails | Make sure you have JDK 17 installed |
| Install fails | Run: `adb uninstall com.runanywhere.startup_hackathon20` |

## 📚 Detailed Guides

- **Full Installation Guide:** See `INSTALL_ON_DEVICE.md`
- **SafeSphere Share Testing:** See `SAFESPHERE_SHARE_TESTING.md`
- **Share Feature Fixes:** See `SAFESPHERE_SHARE_FIXES.md`

## 🛠️ Useful Commands

```powershell
# Check connected devices
adb devices

# View app logs
adb logcat | Select-String "SafeSphere"

# Uninstall app
adb uninstall com.runanywhere.startup_hackathon20

# Clear app data (reset)
adb shell pm clear com.runanywhere.startup_hackathon20

# Reinstall after changes
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## ⏱️ Time Estimates

- **First build:** 3-5 minutes (downloads dependencies)
- **Installation:** 10-30 seconds
- **Total:** ~5 minutes

Subsequent builds take only 30 seconds - 2 minutes!

## 🎯 Quick Test After Install

1. Open SafeSphere ✅
2. Create account ✅
3. Add a password to Password Manager ✅
4. Go to SafeSphere Share ✅
5. Grant permissions (no popup!) ✅
6. Try finding nearby devices ✅

All working? **Success!** 🎉

---

**Need help?** Check the detailed guides or review logcat output.
