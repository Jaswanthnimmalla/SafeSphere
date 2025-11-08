# 🔧 Manual Autofill Debugging Guide - Step by Step

## ⚠️ **PROBLEM: Autofill Credentials Not Displaying**

Follow these steps **IN ORDER** to diagnose and fix the issue.

---

## 📋 **Step 1: Verify SafeSphere Autofill Service is Enabled**

### **A. Check in Device Settings**

1. Open **Settings** on your Android device
2. Search for **"Autofill"** or **"Autofill service"**
3. Look for one of these paths:
    - `Settings → System → Languages & input → Advanced → Autofill service`
    - `Settings → Passwords & accounts → Autofill service`
    - `Settings → System → Languages & input → Autofill service`

4. ✅ **MUST SHOW:** "SafeSphere" or "SafeSphere Autofill"
5. ❌ **If you see:** "None" or "Google" → **TAP IT** and select "SafeSphere"

### **B. Enable SafeSphere Autofill**

If SafeSphere is not in the list:

1. **Uninstall the app completely**
2. **Reinstall** from `app/build/outputs/apk/debug/app-debug.apk`
3. **Restart your device** (important!)
4. Check Settings → Autofill service again

---

## 📋 **Step 2: Verify Credentials Are Actually Saved**

### **A. Open SafeSphere App**

1. Open SafeSphere
2. Tap **"Passwords"** tab (bottom navigation)
3. ✅ **MUST SEE:** List of saved passwords
4. ❌ **If empty:** No credentials saved → Go to Step 3

### **B. Check Saved Credential Details**

For each saved password, verify:

1. **Service Name** is set (e.g., "Reddit", "Twitter", "GitHub")
2. **Username** is set (e.g., "testuser123")
3. **URL** is set (e.g., "reddit.com" - just domain, no https://)

**IMPORTANT:** If URL has full path like `https://reddit.com/login?next=/`:

- Edit the password
- Change URL to just: `reddit.com`
- Save

---

## 📋 **Step 3: Save a Test Credential Manually**

Let's create a known-good test credential:

1. Open SafeSphere → Passwords tab
2. Tap **"+"** (Add Password button)
3. Fill EXACTLY as shown:
   ```
   Service Name: Reddit
   Username: testuser123
   Password: TestPass123!
   URL: reddit.com
   Category: Social Media
   ```
4. Tap **"Save"**
5. ✅ **VERIFY:** "Reddit" appears in password list

---

## 📋 **Step 4: Test Autofill on Chrome Browser**

### **A. Clear Browser Data (Important!)**

1. Open **Chrome** browser
2. Tap ⋮ (three dots) → Settings
3. Privacy and security → Clear browsing data
4. Select:
    - Browsing history
    - Cookies and site data
    - Cached images and files
5. Clear data

### **B. Visit Test Website**

1. In Chrome, go to: `reddit.com`
2. Tap **"Log In"** or **"Sign In"**
3. Wait for page to fully load

### **C. Test Autofill**

1. **Tap on the USERNAME field** (not password field first)
2. Wait 2-3 seconds
3. ✅ **SHOULD SEE:** Autofill dropdown with "Reddit - testuser123"
4. ❌ **If no dropdown:** Continue to next steps

### **D. Alternative: Try Password Field**

1. **Tap on the PASSWORD field** instead
2. Wait 2-3 seconds
3. Sometimes autofill triggers on password field

---

## 📋 **Step 5: Check Android Version Compatibility**

### **Minimum Requirements:**

SafeSphere Autofill requires:

- ✅ **Android 8.0 (API 26) or higher**
- ❌ **Android 7.1 or lower** → Autofill NOT supported

### **Check Your Android Version:**

1. Settings → About phone → Android version
2. Must be **8.0** or higher

---

## 📋 **Step 6: Check if Other Autofill Services Are Interfering**

### **A. Disable Google Autofill**

1. Settings → Google → Autofill
2. If "Autofill with Google" is ON → **Turn it OFF**

### **B. Check for Conflicting Apps**

Uninstall/disable these if installed:

- LastPass
- 1Password
- Dashlane
- Keeper
- Bitwarden
- Any other password manager

---

## 📋 **Step 7: Check App Permissions**

1. Settings → Apps → SafeSphere
2. Tap **"Permissions"**
3. ✅ **MUST HAVE:**
    - Storage (Allow)
    - Biometric (if testing biometric features)

---

## 📋 **Step 8: Enable Developer Logging (Advanced)**

If you have ADB installed, run this to see detailed logs:

```powershell
# Find ADB (usually in Android SDK platform-tools)
# Common locations:
# C:\Users\[YourName]\AppData\Local\Android\Sdk\platform-tools\adb.exe
# Or use full path

# Clear logs
[path-to-adb]\adb.exe logcat -c

# Monitor autofill logs
[path-to-adb]\adb.exe logcat | Select-String "SafeSphereAutofill"
```

Then test autofill and watch for logs.

### **Expected Log Output (Success):**

```
📝 FILL REQUEST RECEIVED
📱 App: Chrome
✅ Login fields detected
💾 Found 1 saved credentials
✅ Matched by exact domain: reddit.com
✅ Fill response sent successfully
```

### **Common Error Logs:**

**"❌ No login fields detected"**

- Website doesn't have proper login form
- Try a different website

**"💾 Found 0 saved credentials"**

- No credentials match current page
- Check service name and URL

**"❌ No match found"**

- Saved URL doesn't match current page
- Edit credential and fix URL

---

## 📋 **Step 9: Test Different Scenarios**

### **Scenario A: Reddit Website**

1. Save credential: Service="Reddit", URL="reddit.com"
2. Visit: `reddit.com/login`
3. Tap username field
4. Should show autofill

### **Scenario B: Twitter/X Website**

1. Save credential: Service="Twitter", URL="x.com"
2. Visit: `x.com` or `twitter.com`
3. Tap username field
4. Should show autofill

### **Scenario C: GitHub**

1. Save credential: Service="GitHub", URL="github.com"
2. Visit: `github.com/login`
3. Tap username field
4. Should show autofill

**If NONE of these work** → Continue to Step 10

---

## 📋 **Step 10: Reinstall from Scratch**

This is the **nuclear option** but often fixes mysterious issues:

1. **Backup your data:**
    - Open SafeSphere
    - Take screenshots of all saved passwords
    - Or manually note them down

2. **Complete uninstall:**
   ```
   Settings → Apps → SafeSphere → Uninstall
   ```

3. **Clear cached data:**
    - Restart device
    - This clears any lingering system cache

4. **Reinstall:**
    - Install `app-debug.apk` again
    - Fresh start

5. **Set up again:**
    - Enable autofill service
    - Add one test credential
    - Test immediately

---

## 📋 **Step 11: Common Issues & Solutions**

### **Issue 1: Dropdown Appears But Says "No Credentials"**

**Solution:**

- Service name or URL doesn't match
- Edit credential in SafeSphere
- Make sure URL is just domain (e.g., `reddit.com`, not full URL)

### **Issue 2: Dropdown Doesn't Appear At All**

**Solution:**

- Autofill service not enabled → Check Step 1
- Android version too old → Check Step 5
- Permissions missing → Check Step 7

### **Issue 3: Dropdown Appears But Doesn't Fill**

**Solution:**

- Decryption issue
- Try deleting and re-saving the credential
- Make sure you set a master password/PIN when first opening app

### **Issue 4: Works on Some Websites But Not Others**

**Solution:**

- Some websites block autofill for security
- Examples: banking apps, some payment forms
- This is normal - not all websites support autofill

### **Issue 5: Works in Browser But Not Native Apps**

**Solution:**

- Native apps need different matching
- When saving from native app, URL will be package name (e.g., `com.reddit.android`)
- Make sure service name matches app name (e.g., "Reddit")

---

## 📋 **Step 12: Report Back with Details**

If autofill still doesn't work after all these steps, provide these details:

1. **Android version:** (e.g., Android 13)
2. **Device model:** (e.g., Samsung Galaxy S22)
3. **Autofill service enabled:** Yes/No (from Settings)
4. **Test website:** (e.g., reddit.com/login)
5. **Saved credential details:**
    - Service: ?
    - Username: ?
    - URL: ?
6. **What happens when you tap login field:** (Nothing? Different dropdown? Error?)
7. **If you have ADB, paste log output**

---

## ✅ **Quick Checklist (Before Asking for Help)**

- [ ] Android 8.0 or higher?
- [ ] SafeSphere autofill service is ENABLED in Settings?
- [ ] At least one password is SAVED in SafeSphere?
- [ ] Saved password has URL set to just domain (e.g., `reddit.com`)?
- [ ] Tested on Chrome browser with `reddit.com`?
- [ ] Tapped on USERNAME field (not password field first)?
- [ ] Waited 2-3 seconds after tapping?
- [ ] No other password managers installed?
- [ ] Google Autofill is DISABLED?
- [ ] App has Storage permission?
- [ ] Device has been restarted after installation?

**If ALL boxes are checked and it still doesn't work, then we have a bug that needs investigating.**

---

## 🎯 **Most Common Cause: Autofill Service Not Enabled**

**95% of the time, the issue is:**

```
Settings → Autofill service → Shows "Google" or "None"
```

**Fix:**

```
Settings → Autofill service → Select "SafeSphere"
```

**Then restart device** (important for system to register the change!)

---

## 🚀 **Expected Behavior (When Working):**

1. Open Chrome → Go to `reddit.com/login`
2. Tap username field
3. **1-2 seconds later:** Dropdown appears with:
   ```
   SafeSphere (1 saved)
   ┌─────────────────────────┐
   │ Reddit                  │
   │ testuser123             │
   └─────────────────────────┘
   ```
4. Tap the dropdown item
5. **Username and password auto-filled!** ✅

If this is what you see → **Autofill is working perfectly!** 🎉

If you don't see this → Follow the steps above to diagnose the issue.

---

## 📞 **Still Need Help?**

Provide:

1. Screenshot of Settings → Autofill service page
2. Screenshot of SafeSphere → Passwords tab (with saved credentials visible)
3. Video of you testing autofill (tap username field, show what happens)
4. Android version and device model
5. If possible, ADB logs while testing

**With these details, I can pinpoint the exact issue!** 🔍