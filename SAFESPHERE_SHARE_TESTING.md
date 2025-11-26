# SafeSphere Share - Quick Testing Guide

## Setup (Do This Once)

1. **Install SafeSphere on TWO Android devices**
    - Minimum: Android 4.0+
    - Recommended: Android 10+

2. **Enable WiFi on both devices**
    - Settings → WiFi → Turn ON
    - You don't need to connect to a network, just enable WiFi

3. **Grant permissions on both devices**
    - Open SafeSphere → Menu → SafeSphere Share
    - When prompted, grant Location and Storage permissions
    - If permission dialog doesn't appear, tap "Grant Permissions" button

## How to Test File Sharing

### Device A (Sender)

1. Open SafeSphere
2. Tap Menu (☰) → **SafeSphere Share**
3. Stay on **"Send" tab**
4. Tap **"Select Files to Share"**
5. Choose one or more files (photos, videos, documents)
6. Tap **"Find Nearby Devices"**
7. Wait 10-30 seconds for Device B to appear
8. Tap **"Send"** next to Device B's name
9. Watch the progress bar fill up!

### Device B (Receiver)

1. Open SafeSphere
2. Tap Menu (☰) → **SafeSphere Share**
3. Switch to **"Receive" tab**
4. Keep this screen open
5. Wait for Device A to discover and send files
6. Files will appear in: **Device Storage → SafeSphereShare/**

## Expected Results

### ✅ What Should Happen

1. **Permission Request**
    - Clean permission dialog (not settings popup)
    - Request for Location and Storage access
    - Clear explanation of why permissions are needed

2. **Device Discovery**
    - Searching animation with progress indicator
    - Device appears within 10-30 seconds
    - Shows device name and signal strength
    - Can see device distance indicator

3. **File Transfer**
    - Real-time progress bar (0-100%)
    - Transfer speed display (KB/s or MB/s)
    - Estimated time remaining
    - Success message when complete

4. **Error Handling**
    - If WiFi is off: "WiFi Direct is disabled"
    - If no devices found: Helpful troubleshooting tips
    - Clear error messages in red banner

### ❌ What Should NOT Happen

1. ~~App opening Android settings automatically~~
2. ~~Permission popup redirecting to settings~~
3. ~~No feedback during device search~~
4. ~~Demo/fake devices appearing in list~~
5. ~~App crashing when permissions denied~~

## Common Issues & Solutions

### "No devices found"

**Check:**

- [ ] WiFi is enabled on BOTH devices
- [ ] Both devices are within 10 meters
- [ ] Device B is on the "Receive" tab
- [ ] No obstacles blocking WiFi signal

**Fix:**

1. Turn WiFi off and on again on both devices
2. Wait 10 seconds
3. Tap "Search Again" on Device A

### "Permission Denied"

**Fix:**

1. Tap "Grant Permissions" button
2. Or: Settings → Apps → SafeSphere → Permissions
3. Enable Location and Storage
4. Return to SafeSphere

### "Connection Failed"

**Fix:**

1. Restart the discovery on Device A
2. Ensure Device B is still on Receive tab
3. Try swapping sender/receiver roles
4. Restart both devices if issue persists

### "Transfer is slow"

**Check:**

- [ ] Devices are close together (< 5 meters)
- [ ] No walls between devices
- [ ] No other apps using WiFi heavily
- [ ] Device WiFi hardware capabilities

## Performance Benchmarks

### Expected Transfer Speeds

| File Size | Expected Time | Speed |
|-----------|---------------|-------|
| 10 MB     | 5-10 seconds  | ~10 Mbps |
| 100 MB    | 30-60 seconds | ~15-25 Mbps |
| 1 GB      | 5-10 minutes  | ~20-50 Mbps |

**Note:** Actual speeds depend on:

- Device WiFi hardware (older devices are slower)
- Distance between devices
- Environmental interference
- File system performance

## Real vs Demo Devices

### ✅ REAL Devices (Current Implementation)

- Uses actual WiFi Direct (Wi-Fi P2P)
- Discovers physical Android devices nearby
- Real peer-to-peer file transfer
- No internet required
- Speeds: 10-250 Mbps

### ❌ Demo Devices (Removed)

- Fake devices with hardcoded names
- No actual connection
- Simulated transfers
- For UI testing only

## Troubleshooting Checklist

Before reporting an issue, verify:

- [ ] WiFi is ON (not connected, just enabled)
- [ ] Location permission granted
- [ ] Storage permission granted
- [ ] Both devices on SafeSphere Share screen
- [ ] Devices within 10 meters
- [ ] No other WiFi Direct connections active
- [ ] Android version is 4.0 or higher

## Testing Different Scenarios

### Scenario 1: Fresh Install

1. Install SafeSphere for first time
2. Open SafeSphere Share
3. Should request permissions cleanly
4. Grant permissions
5. Should immediately start working

### Scenario 2: Permission Denial

1. Open SafeSphere Share
2. Deny permissions when asked
3. Should show permission rationale card
4. Tap "Grant Permissions" again
5. Allow permissions
6. Should work immediately

### Scenario 3: WiFi Disabled

1. Turn off WiFi
2. Try to discover devices
3. Should show "WiFi Direct is disabled" error
4. Turn on WiFi
5. Try again - should work

### Scenario 4: No Nearby Devices

1. Ensure no other devices nearby have SafeSphere
2. Start discovery
3. Wait 30 seconds
4. Should show "No devices found" with tips
5. Tap "Search Again"

### Scenario 5: Large File Transfer

1. Select a large video file (> 100 MB)
2. Send to nearby device
3. Should show:
    - Accurate progress percentage
    - Real-time speed (MB/s)
    - Time remaining estimate
    - Ability to cancel

## Logs for Debugging

If something doesn't work, check Android logcat:

```bash
# Filter for WiFi Direct logs
adb logcat | grep "WiFiDirectService"

# Filter for repository logs
adb logcat | grep "ShareRepository"

# Filter for SafeSphere logs
adb logcat | grep "SafeSphere"
```

Look for these log messages:

- ✅ "WiFi Direct initialized successfully"
- ✅ "Discovery started successfully"
- ✅ "Found X device(s)"
- ✅ "Connection established"
- ✅ "Transfer complete"

## Success Criteria

The feature is working correctly if:

1. ✅ Permissions requested properly (no settings popup)
2. ✅ Real devices discovered (not demo devices)
3. ✅ Clear real-time feedback during discovery
4. ✅ Error messages displayed when issues occur
5. ✅ Files transfer successfully between devices
6. ✅ Progress bar updates accurately
7. ✅ Transfer speed and ETA displayed
8. ✅ Helpful tips when no devices found

## Quick Reference Commands

```bash
# Check WiFi Direct support
adb shell getprop | grep wifi

# Check permissions
adb shell dumpsys package com.runanywhere.startup_hackathon20 | grep permission

# Clear app data (reset permissions)
adb shell pm clear com.runanywhere.startup_hackathon20

# Install APK on two devices
adb -s <device1_serial> install SafeSphere.apk
adb -s <device2_serial> install SafeSphere.apk
```

## Contact & Support

If you encounter issues not covered in this guide:

1. Check `SAFESPHERE_SHARE_FIXES.md` for detailed implementation
2. Review logcat output for specific errors
3. Ensure both devices meet minimum requirements
4. Try with different device combinations

---

**Last Updated:** January 2025
**Version:** 1.0 (Real Device Support)
**Status:** ✅ Production Ready
