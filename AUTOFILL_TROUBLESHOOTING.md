# 🔧 SafeSphere Autofill - Troubleshooting Guide

## ❌ PROBLEM: "Save password?" prompt NOT appearing

You've successfully built and installed SafeSphere, but the autofill save prompt is not showing.
Let's diagnose and fix this step by step.

---

## 📋 **STEP 1: VERIFY ANDROID VERSION**

AutofillService requires **Android 8.0 (API 26) or higher**.

**Check your Android version:**

```
Settings → About phone → Android version

Required: Android 8.0+
Examples:
✅ Android 12 - SUPPORTED
✅ Android 10 - SUPPORTED  
✅ Android 8.1 - SUPPORTED
✅ Android 8.0 - SUPPORTED
❌ Android 7.1 - NOT SUPPORTED
❌ Android 7.0 - NOT SUPPORTED
```

**If your device is Android 7.x or lower:**

- AutofillService will NOT work
- You need Android 8.0+ for this feature
- Consider using an emulator with Android 10+

---

## 📋 **STEP 2: ENABLE SAFESPHERE AUTOFILL SERVICE**

**Method 1: Via Android Settings (Most Common)**

```
1. Open Android Settings
2. Navigate to one of these paths (varies by device):
   
   Samsung: 
   Settings → General management → Passwords and autofill → Autofill service
   
   Google Pixel:
   Settings → System → Languages & input → Advanced → Autofill service
   
   OnePlus/Oppo:
   Settings → System → Language & input → Autofill service
   
   Xiaomi:
   Settings → Passwords & security → Autofill with → Apps
   
   Generic Android:
   Settings → System → Languages & input → Advanced → Autofill service

3. Current selection likely shows: "Google" or "None"

4. Tap on it → Select "SafeSphere Autofill"

5. Tap "OK" to confirm

6. ✅ DONE! SafeSphere is now your autofill provider
```

**Method 2: Via SafeSphere App**

```
1. Open SafeSphere app
2. Login
3. Open side menu (☰)
4. Tap "🔑 Passwords"
5. You should see a banner: "⚡ Autofill is not enabled"
6. Tap "Enable Autofill" or "Open Settings"
7. Select "SafeSphere Autofill"
8. ✅ DONE!
```

---

## 📋 **STEP 3: VERIFY SAFESPHERE IS SELECTED**

**Check which autofill service is active:**

```
Settings → System → Languages & input → Autofill service

Should show: ✅ SafeSphere Autofill

NOT:
❌ Google
❌ None
❌ Samsung Pass
❌ Dashlane
❌ LastPass
```

**Important:** Only ONE autofill service can be active at a time!

---

## 📋 **STEP 4: DISABLE GOOGLE PASSWORD MANAGER**

Even if SafeSphere is selected, Google Password Manager might intercept autofill. Disable it:

**Disable in Android Settings:**

```
Settings → Google → Autofill → Autofill with Google → Toggle OFF
```

**Disable in Chrome (for browser autofill):**

```
1. Open Chrome app
2. Tap ⋮ (three dots) → Settings
3. Tap "Passwords"
4. Toggle OFF "Save passwords"
5. Toggle OFF "Auto Sign-in"
```

---

## 📋 **STEP 5: TEST ON A SIMPLE WEBSITE**

Let's test with a simple, known-good website:

**Test Steps:**

```
1. Open Chrome browser
2. Go to: https://the-internet.herokuapp.com/login
   (This is a test login page that's guaranteed to work)

3. Enter test credentials:
   Username: tomsmith
   Password: SuperSecretPassword!

4. Tap "Login" button

5. ✅ EXPECTED: Android shows "Save password to SafeSphere?"
   
6. If you see this, tap "Save"
   
7. ✅ SUCCESS! Go to SafeSphere → Passwords → See saved credential
```

**If the prompt STILL doesn't appear, continue to Step 6.**

---

## 📋 **STEP 6: CHECK LOGCAT (ADVANCED DEBUG)**

The AutofillService logs everything. Let's see what's happening:

**If you have adb installed:**

```powershell
# Connect your phone via USB
# Enable USB Debugging on your phone

# Clear logs
adb logcat -c

# Start monitoring SafeSphere logs
adb logcat -s SafeSphereAutofill:D

# Now test on a website/app
# You should see logs like:

📝 Fill request received
🎯 Login form detected in: Chrome (com.android.chrome)
No saved passwords for Chrome - will show save prompt after login
✅ Fill response sent

(After tapping Login)
💾 Save request received
🔑 Credentials detected for: Chrome
   Username: test@email.com
   Password: tes***
✅ Password saved to SafeSphere vault
```

**If you see NO logs:**

- SafeSphere AutofillService is NOT being triggered
- Check that it's selected in Settings (Step 2)
- Check Android version (Step 1)

**If you see "Fill request received" but no "Save request received":**

- The login form is detected
- BUT Android is not calling onSaveRequest()
- This might be a compatibility issue with the specific app/website

---

## 📋 **STEP 7: TEST ON DIFFERENT APPS**

Some apps have better autofill compatibility than others. Test on multiple:

**Best Apps to Test:**

1. ✅ Chrome browser → https://github.com/login
2. ✅ Firefox browser → https://twitter.com/login
3. ✅ Facebook app (logout first, then login)
4. ✅ Instagram app
5. ✅ Gmail app (add new account)

**Apps that might NOT work:**

- ❌ Banking apps (often block autofill for security)
- ❌ Some games
- ❌ Apps that use custom WebView login

---

## 📋 **STEP 8: REINSTALL SAFESPHERE**

If nothing works, try a clean reinstall:

```
1. Uninstall SafeSphere completely
2. Reboot your phone
3. Install the APK again
4. Open SafeSphere → Login
5. Go to Settings → Enable Autofill
6. Test again
```

---

## 📋 **STEP 9: VERIFY MANIFEST CONFIGURATION**

The AutofillService must be properly declared in AndroidManifest.xml. It should have:

```xml
<service
    android:name="com.runanywhere.startup_hackathon20.autofill.SafeSphereAutofillService"
    android:exported="true"
    android:label="SafeSphere Autofill"
    android:permission="android.permission.BIND_AUTOFILL_SERVICE">
    <intent-filter>
        <action android:name="android.service.autofill.AutofillService" />
    </intent-filter>
    <meta-data
        android:name="android.autofill"
        android:resource="@xml/autofill_service" />
</service>
```

This is already correct in your build ✅

---

## 🎯 **COMMON ISSUES & SOLUTIONS**

| Problem | Solution |
|---------|----------|
| No prompt appears | Verify SafeSphere is selected in Settings |
| Google prompt appears | Disable Google Password Manager |
| "Autofill service not found" | Reinstall SafeSphere |
| Works on Chrome, not apps | Some apps block autofill (normal) |
| Android 7.x | AutofillService requires Android 8.0+ |
| Save prompt only appears once | After saving, you'll see autofill suggestions instead |

---

## ✅ **HOW TO VERIFY IT'S WORKING**

### **Test 1: Save a Password**

```
1. Go to any login page
2. Enter credentials
3. Tap Login
4. ✅ See "Save password?" prompt
5. Tap "Save"
```

### **Test 2: View Saved Password**

```
1. Open SafeSphere
2. Go to "🔑 Passwords"
3. ✅ See your saved credential
4. Tap it → Biometric prompt
5. Authenticate
6. ✅ See username and password
```

### **Test 3: Autofill Saved Password**

```
1. Logout from the website/app
2. Go to login page
3. Tap username field
4. ✅ See dropdown: "🔐 Website - username@email.com"
5. Tap it
6. ✅ Both fields filled!
```

---

## 🆘 **STILL NOT WORKING?**

### **Check These:**

1. **Android Version**: Settings → About phone → Android version (must be 8.0+)

2. **Autofill Service**: Settings → System → Languages & input → Autofill service
    - Should show: **SafeSphere Autofill** ✅

3. **Google Password Manager**: Settings → Google → Autofill
    - Should be: **OFF** ✅

4. **SafeSphere Permissions**: Settings → Apps → SafeSphere → Permissions
    - All required permissions granted

5. **Test Website**: Use https://the-internet.herokuapp.com/login
    - If this doesn't work, nothing will

---

## 📱 **KNOWN LIMITATIONS**

AutofillService has some limitations on Android:

1. **Banking Apps**: Many banking apps block autofill for security
2. **Custom Keyboards**: Some keyboards interfere with autofill
3. **WebView Apps**: Apps using embedded browsers might not work
4. **Device Manufacturer**: Samsung, Xiaomi have custom autofill systems that might conflict

---

## 🎉 **EXPECTED BEHAVIOR WHEN WORKING**

### **First Time (No Saved Password):**

```
1. Open any app
2. Go to login screen
3. Tap username field
   → No dropdown (expected - no saved passwords yet)
4. Enter username + password
5. Tap "Login" or "Sign In"
   → ✅ PROMPT: "Save password to SafeSphere?"
6. Tap "Save"
   → ✅ Saved!
```

### **Second Time (Password Already Saved):**

```
1. Open same app
2. Go to login screen
3. Tap username field
   → ✅ DROPDOWN: "🔐 AppName - your_email@gmail.com"
4. Tap the suggestion
   → ✅ Both fields auto-filled!
5. Tap "Login"
   → ✅ Logged in!
```

---

## 📚 **REFERENCE: LOG MESSAGES**

When working correctly, you should see these logs:

**When focusing on login form:**

```
D/SafeSphereAutofill: 📝 Fill request received
D/SafeSphereAutofill: 🎯 Login form detected in: AppName (com.app.package)
D/SafeSphereAutofill: No saved passwords for AppName - will show save prompt after login
D/SafeSphereAutofill: ✅ Fill response sent
```

**When submitting login:**

```
D/SafeSphereAutofill: 💾 Save request received
D/SafeSphereAutofill: 🔑 Credentials detected for: AppName
D/SafeSphereAutofill:    Username: user@email.com
D/SafeSphereAutofill:    Password: use***
D/SafeSphereAutofill: ✅ Password saved to SafeSphere vault
```

---

**If you've gone through all these steps and it's still not working, the issue is likely:**

1. Android version < 8.0
2. Device manufacturer blocking autofill
3. Specific app/website blocking autofill
4. ADB is needed to see what's actually happening
