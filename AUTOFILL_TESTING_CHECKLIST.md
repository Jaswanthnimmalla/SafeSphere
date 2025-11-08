# 🧪 SafeSphere Autofill Testing Checklist

## Pre-Testing Setup

### 1. Enable Autofill Service

- [ ] Open SafeSphere app
- [ ] Go to Passwords tab
- [ ] Tap "Enable Autofill" banner
- [ ] Tap "Open Settings"
- [ ] Select "SafeSphere" from list
- [ ] Tap "OK" to confirm
- [ ] Verify "✅ Autofill ON" shows in app

### 2. Enable ADB Logging (Optional - for debugging)

```bash
adb logcat | grep SafeSphereAutofill
```

---

## Test Suite 1: Browser Autofill

### Test 1.1: Save Password in Chrome

**Steps:**

1. Open Chrome browser
2. Navigate to: `https://twitter.com/login`
3. Enter test credentials:
    - Username: `testuser@email.com`
    - Password: `TestPass123!`
4. Tap "Log in" button

**Expected Result:**

- ✅ "Save to SafeSphere?" prompt appears
- ✅ Can tap "Save" button
- ✅ Password appears in SafeSphere Passwords tab

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

### Test 1.2: Autofill in Chrome

**Steps:**

1. Clear Twitter login form (or log out)
2. Navigate back to `https://twitter.com/login`
3. Tap on username/email field

**Expected Result:**

- ✅ Dropdown appears: "🔐 SafeSphere (1 saved)"
- ✅ Shows: "🌐 Twitter - testuser@email.com"
- ✅ Tapping fills both username and password
- ✅ Can log in successfully

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

### Test 1.3: Multiple Saved Credentials

**Steps:**

1. Save 2-3 different accounts for Twitter
2. Open login page
3. Tap username field

**Expected Result:**

- ✅ Dropdown shows all saved accounts
- ✅ Each shows correct username
- ✅ Tapping correct account fills properly

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

## Test Suite 2: Native App Autofill

### Test 2.1: Save in Instagram App

**Steps:**

1. Install Instagram (if not installed)
2. Open Instagram
3. Tap "Log in"
4. Enter credentials
5. Tap "Log in" button

**Expected Result:**

- ✅ "Save to SafeSphere?" prompt appears
- ✅ Password saves successfully
- ✅ Appears in Passwords tab as "📱 Instagram"

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

### Test 2.2: Autofill in Instagram

**Steps:**

1. Log out of Instagram
2. Open Instagram app
3. Tap on username field

**Expected Result:**

- ✅ Autofill dropdown appears
- ✅ Shows saved Instagram credentials
- ✅ Fills both fields correctly

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

### Test 2.3: Banking App (if available)

**Steps:**

1. Open any banking app
2. Enter credentials and login
3. Check for save prompt

**Expected Result:**

- ✅ Save prompt appears (or already saved)
- ✅ Categorized as "🏦 Banking"

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

## Test Suite 3: Edge Cases

### Test 3.1: Update Existing Password

**Steps:**

1. Use app/site where password is saved
2. Change password to new value
3. Login with new password

**Expected Result:**

- ✅ "Update in SafeSphere?" prompt appears
- ✅ Tapping "Save" updates the password
- ✅ Next autofill uses new password

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

### Test 3.2: Multiple Fields Form

**Steps:**

1. Find login form with extra fields (e.g., domain, email + username)
2. Fill and submit

**Expected Result:**

- ✅ Correctly identifies username and password fields
- ✅ Ignores extra fields

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

### Test 3.3: URL Domain Matching

**Steps:**

1. Save password for `accounts.google.com`
2. Open `mail.google.com`
3. Tap login field

**Expected Result:**

- ✅ Suggests saved Google account
- ✅ Domain matching works (google.com)

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

## Test Suite 4: UI/UX

### Test 4.1: Autofill Dropdown Appearance

**Expected:**

- ✅ Clear, readable text
- ✅ Proper icons (🔐, 📱, 🌐)
- ✅ Username visible
- ✅ Service name visible
- ✅ Dropdown has header on Android 9+

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

### Test 4.2: Save Prompt Appearance

**Expected:**

- ✅ Clear "Save to SafeSphere?" text
- ✅ Shows app/service name
- ✅ Has "Save" and "Never" options
- ✅ Prompt dismisses properly

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

## Test Suite 5: Security

### Test 5.1: Password Encryption

**Steps:**

1. Save a password
2. Check encrypted file with ADB:

```bash
adb shell cat /data/data/com.runanywhere.startup_hackathon20/files/password_vault.enc
```

**Expected Result:**

- ✅ File content is encrypted (unreadable)
- ✅ No plain text passwords visible

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

### Test 5.2: Decryption on Autofill

**Expected:**

- ✅ Password decrypts correctly during autofill
- ✅ Login works with filled password
- ✅ No errors in logcat

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

## Test Suite 6: Cross-App Scenarios

### Test 6.1: Different Browser

**Steps:**

1. Save password in Chrome
2. Open same site in Firefox/Edge
3. Check autofill

**Expected Result:**

- ✅ Same password suggested
- ✅ URL matching works across browsers

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

### Test 6.2: App vs Browser for Same Service

**Steps:**

1. Save password for Instagram in browser
2. Try autofill in Instagram app

**Expected Result:**

- ✅ Suggests same credentials
- ✅ Works in both contexts

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

## Test Suite 7: Popular Apps/Sites

Test with these common services:

### Social Media

- [ ] Facebook (app)
- [ ] Instagram (app)
- [ ] Twitter/X (browser)
- [ ] LinkedIn (browser)

### Email

- [ ] Gmail (browser)
- [ ] Outlook (browser)
- [ ] Yahoo Mail (app)

### Shopping

- [ ] Amazon (browser)
- [ ] eBay (browser)

### Entertainment

- [ ] Netflix (browser)
- [ ] Spotify (app)

---

## Performance Testing

### Test P1: Response Time

**Expected:**

- ✅ Autofill dropdown appears within 1-2 seconds
- ✅ Save prompt appears immediately after login
- ✅ No lag or freezing

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

### Test P2: Memory Usage

**Steps:**

1. Open SafeSphere
2. Check memory in Android Settings
3. Use autofill 10+ times
4. Check memory again

**Expected Result:**

- ✅ No significant memory leak
- ✅ App remains responsive

**Status:** [ ] PASS / [ ] FAIL  
**Notes:** _______________

---

## Compatibility Testing

### Android Versions

- [ ] Android 8.0 (API 26)
- [ ] Android 9.0 (API 28)
- [ ] Android 10 (API 29)
- [ ] Android 11 (API 30)
- [ ] Android 12 (API 31)
- [ ] Android 13 (API 33)
- [ ] Android 14 (API 34)

### Devices

- [ ] Samsung device
- [ ] Google Pixel
- [ ] OnePlus
- [ ] Xiaomi/Redmi
- [ ] Other: ___________

---

## Logging & Debugging

### Check These Log Messages

**On Fill Request:**

```
📝 FILL REQUEST RECEIVED
📱 App: [App Name]
✅ Login fields detected:
   👤 Username field: [hint]
   🔑 Password field: [hint]
💾 Found X saved credentials
✅ Fill response sent successfully
```

**On Save Request:**

```
💾 SAVE REQUEST RECEIVED
✅ Credentials extracted:
   👤 Username: [username]
   🔑 Password: ***
💾 Saving new password to SafeSphere vault
✅ Password saved successfully to SafeSphere!
```

---

## Known Issues / Limitations

### Won't Work:

- ❌ Custom keyboard apps may interfere
- ❌ Some apps with non-standard login forms
- ❌ Apps that prevent autofill explicitly
- ❌ Android versions below 8.0

### Partial Support:

- ⚠️ Two-factor authentication (saves only password)
- ⚠️ Multi-step logins (may need manual intervention)
- ⚠️ Invisible password fields

---

## Test Results Summary

**Total Tests:** _____ / _____  
**Passed:** _____  
**Failed:** _____  
**Blocked:** _____

**Critical Issues Found:**

1. _______________
2. _______________
3. _______________

**Minor Issues Found:**

1. _______________
2. _______________
3. _______________

**Overall Status:** [ ] READY FOR RELEASE / [ ] NEEDS FIXES

---

## Sign-Off

**Tester Name:** _______________  
**Date:** _______________  
**Device:** _______________  
**Android Version:** _______________  
**App Version:** _______________

**Notes:**
_______________
_______________
_______________

---

## Quick Debug Commands

### View Autofill Service Status

```bash
adb shell settings get secure autofill_service
```

### View Real-Time Logs

```bash
adb logcat -s SafeSphereAutofill:D
```

### Check Encrypted Password File

```bash
adb shell ls -l /data/data/com.runanywhere.startup_hackathon20/files/
```

### Force Stop and Restart Service

```bash
adb shell am force-stop com.runanywhere.startup_hackathon20
adb shell am start -n com.runanywhere.startup_hackathon20/.SafeSphereMainActivity
```

---

**Happy Testing!** 🧪🔐
