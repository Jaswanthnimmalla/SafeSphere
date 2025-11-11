# SafeSphere Autofill - Enable Guide 🔐

## How to Enable Autofill (Like Chrome)

SafeSphere has a built-in **Autofill Service** that works just like Chrome/Google Password Manager!
Here's how to enable it:

---

## Step 1: Enable SafeSphere Autofill Service

### On Your Android Device:

1. **Open Device Settings**
    - Pull down notification shade
    - Tap the ⚙️ Settings icon

2. **Go to Autofill Settings**
    - Navigate to: **Settings → System → Languages & Input**
    - Or search: "Autofill" in Settings search bar
    - Tap on **"Autofill service"** or **"Advanced → Autofill service"**

3. **Select SafeSphere**
    - You'll see a list of autofill services
    - Tap on **"SafeSphere"**
    - Confirm the selection

4. **Grant Permissions** (if prompted)
    - Tap **"OK"** or **"Allow"**
    - This lets SafeSphere fill passwords in apps and browsers

---

## Step 2: Save Some Passwords

### Method 1: Manual Save (In SafeSphere App)

1. Open **SafeSphere** app
2. Tap **Menu (☰)** → **Passwords**
3. Tap **"+ Add Password"** button
4. Fill in:
    - **Service Name:** "GitHub" (or any website/app name)
    - **Username:** "GANI-7177" (your username)
    - **Password:** Your password
    - **URL:** "github.com" (website domain)
    - **Category:** Choose appropriate category
5. Tap **"Save"**

### Method 2: Auto-Save (After Logging In)

1. **Open any app or browser** (Chrome, GitHub app, etc.)
2. **Go to login page**
3. **Enter your credentials**
4. **Tap "Sign In" or "Login"**
5. **SafeSphere will show a prompt:** "Save to SafeSphere?"
6. **Tap "Save"**
7. Credentials are now saved!

---

## Step 3: Test Autofill

### Testing in Browser (Chrome, Firefox, etc.):

1. **Open browser** (Chrome, Firefox, Edge, etc.)
2. **Go to GitHub.com** (or any login page)
3. **Tap the username field**
4. **You should see:**
    - A dropdown appearing with **"SafeSphere (1 saved)"** header
    - **"GANI-7177"** with masked password (••••••••)
    - **"Manage passwords..."** option

5. **Tap your username**
6. **Credentials fill automatically!**
7. **Tap "Sign In"**

### Testing in Native Apps (Twitter, Instagram, etc.):

1. **Open any app** with login
2. **Tap login button**
3. **Tap username/email field**
4. **SafeSphere dropdown appears**
5. **Tap your saved credential**
6. **Auto-fills both username AND password!**

---

## How It Looks (Screenshot Reference)

```
┌─────────────────────────────────────┐
│  Sign in to GitHub                  │
│                                     │
│  Username or email address          │
│  ┌───────────────────────────────┐ │
│  │ GANI-7177▼                    │ │  ← Click here
│  └───────────────────────────────┘ │
│                                     │
│  ┌─────────────────────────────────┐
│  │ SafeSphere (2 saved)            │ ← Dropdown appears
│  ├─────────────────────────────────┤
│  │ 👤 GANI-7177                    │ ← Tap to fill
│  │    ••••••••                     │
│  ├─────────────────────────────────┤
│  │ 👤 NANDHU-7177                  │ ← Another account
│  │    ••••••••                     │
│  ├─────────────────────────────────┤
│  │ 🔑 Manage passwords... 🌈        │ ← Opens SafeSphere
│  └─────────────────────────────────┘
│                                     │
│  Password                           │
│  ┌───────────────────────────────┐ │
│  │ •••••••••••                   │ │ ← Auto-filled!
│  └───────────────────────────────┘ │
│                                     │
│  [    Sign in    ]                  │
└─────────────────────────────────────┘
```

---

## Settings You May Need

### If Autofill Doesn't Appear:

#### 1. Check Autofill Service is Enabled

```
Settings → System → Languages & Input → Autofill service → SafeSphere ✅
```

#### 2. Check App Permissions

```
Settings → Apps → SafeSphere → Permissions
✅ All required permissions granted
```

#### 3. Restart SafeSphere App

- Force stop SafeSphere
- Open SafeSphere again
- Try autofill again

#### 4. Check Android Version

- **Minimum:** Android 8.0 (Oreo) or higher
- **Recommended:** Android 10+
- **Best:** Android 11+ (supports inline autofill in keyboard)

---

## Troubleshooting

### Issue: "Autofill service not found in Settings"

**Solution:**

1. Ensure SafeSphere is installed correctly
2. Go to: Settings → Apps → SafeSphere
3. Check "App info" shows version 1.0.0 or higher
4. If not visible, reinstall app
5. Try searching "Autofill" in Settings search

### Issue: "Dropdown doesn't appear"

**Solution:**

1. Verify autofill service is **SafeSphere** (not Chrome/Google)
2. Add a test password in SafeSphere first
3. Try in Chrome browser first (most reliable)
4. Clear browser cache
5. Restart device

### Issue: "No credentials shown in dropdown"

**Solution:**

1. Check SafeSphere → Passwords → Verify passwords are saved
2. Check service name matches (e.g., "GitHub" for github.com)
3. Try saving with exact URL: "github.com" (no https://)
4. Check password is not filtered/hidden

### Issue: "Autofill shows wrong password"

**Solution:**

1. SafeSphere matches by **domain name** or **app name**
2. If multiple accounts exist, all will be shown
3. Select the correct one from dropdown
4. Delete old/wrong passwords from SafeSphere

### Issue: "Auto-save doesn't work"

**Solution:**

1. Ensure you tap "Sign In" button (triggers save)
2. Don't use browser's "Remember me"
3. SafeSphere prompts appear **after** successful login
4. If no prompt, manually save in SafeSphere app

---

## Advanced Settings

### Per-App Autofill Control

**Enable/Disable for specific apps:**

1. Go to: Settings → Apps → [App Name] → Autofill
2. Toggle autofill on/off for that app

### Fallback Autofill (All Passwords)

SafeSphere has a **smart fallback**:

- If no exact match found → Shows **all** saved passwords
- This helps when domain/app detection isn't perfect
- You can still select the correct credential manually

### Browser Compatibility

**Fully Tested:**

- ✅ Chrome
- ✅ Firefox
- ✅ Edge
- ✅ Samsung Internet
- ✅ Brave
- ✅ Opera

**Partially Tested:**

- ⚠️ DuckDuckGo
- ⚠️ UC Browser
- ⚠️ Other browsers

---

## Security Notes

### How SafeSphere Autofill is Secure:

1. **100% Local** - No data sent to cloud or internet
2. **AES-256 Encryption** - All passwords encrypted
3. **Hardware-backed** - Uses Android Keystore
4. **Biometric Lock** - Optional face/fingerprint protection
5. **No Analytics** - Zero tracking or telemetry
6. **Open Source** - Code available for audit

### Privacy Guarantees:

- ✅ Passwords never leave your device
- ✅ No network access for autofill
- ✅ No logging of autofill events
- ✅ No sharing with third parties
- ✅ No ads or tracking

---

## Comparison with Chrome Autofill

| Feature | SafeSphere | Chrome/Google |
|---------|-----------|---------------|
| **Offline** | ✅ 100% local | ❌ Requires Google account |
| **Privacy** | ✅ No cloud sync | ⚠️ Syncs to Google servers |
| **Encryption** | ✅ AES-256 | ✅ Google encryption |
| **Open Source** | ✅ Auditable | ❌ Closed source |
| **Device-only** | ✅ Never leaves phone | ❌ Syncs across devices |
| **Biometric** | ✅ Optional | ✅ Optional |
| **Auto-save** | ✅ Yes | ✅ Yes |
| **Auto-fill** | ✅ Yes | ✅ Yes |
| **Categories** | ✅ Organized | ⚠️ Basic |
| **Breach Detection** | ✅ Built-in | ✅ Via Google |
| **Cost** | ✅ Free | ✅ Free |

---

## Quick Reference

### Enable Autofill (One-Time Setup)

```
Settings → System → Languages & Input → Autofill service → SafeSphere
```

### Add Password Manually

```
SafeSphere → Menu → Passwords → + Add Password
```

### View Saved Passwords

```
SafeSphere → Menu → Passwords → [Tap any password]
```

### Disable Autofill (If Needed)

```
Settings → System → Languages & Input → Autofill service → None
```

---

## Summary

✅ **SafeSphere autofill is already built-in!**
✅ **Works exactly like Chrome autofill**
✅ **Just enable it in Settings → Autofill service**
✅ **Save passwords manually or auto-save on login**
✅ **Tap username field to see autofill dropdown**
✅ **100% private and secure - no cloud sync**

---

## Need Help?

1. **In-App Help:**
    - Open SafeSphere
    - Menu → About Us → Contact Support

2. **Check Logs:**
    - Open SafeSphere
    - If autofill doesn't work, check logcat:
   ```
   adb logcat | grep SafeSphereAutofill
   ```

3. **Report Issues:**
    - Screenshots help!
    - Mention: App name, Android version, what happened

---

**Last Updated:** January 11, 2025
**Version:** 1.0.0
**Status:** ✅ WORKING & TESTED
