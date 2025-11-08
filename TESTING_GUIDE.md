# 🧪 SafeSphere Testing Guide

## 📱 **How to Test SafeSphere Password Manager**

Complete step-by-step testing instructions for all features.

---

## ⚙️ **Pre-Testing Setup**

### **Step 1: Build and Install the App**

```bash
# Build the APK
.\gradlew.bat assembleDebug

# Install on device/emulator
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Or use Android Studio:

- Click **Run** (▶) button
- Select your device/emulator
- Wait for installation

---

## 🧪 **TEST 1: Password Manager Quick Access Card**

### **What to Test:**

The new Password Manager card in the Dashboard

### **Steps:**

1. **Launch SafeSphere**
    - Open the app
    - Login/Register if needed
    - Navigate to Dashboard (Home screen)

2. **Locate the Card**
    - Look for "🗝️ Password Manager" card
    - Should be positioned after "Security Score"
    - Should be before "Password Health" card

3. **Check Display:**
    - **Password Count**: Should show "0 Saved Passwords" initially
    - **Autofill Status**: Shows "Autofill: OFF" with orange badge
    - **Buttons**: "View All" (top right), "Add Password" (bottom)

4. **Test "Enable Autofill":**
    - Click "Enable" link in the orange badge
    - Android Settings should open
    - If nothing happens: Check Android version (needs Android 8.0+)

5. **Test "View All":**
    - Click "View All" button
    - Should navigate to Passwords screen
    - Should show search bar, categories, empty state

6. **Test "Add Password":**
    - Click "Add Password" button
    - Should navigate to Passwords screen
    - Can then click + FAB to add password

### **Expected Results:**

- ✅ Card visible and styled correctly
- ✅ All text displays properly
- ✅ Buttons are clickable
- ✅ Navigation works
- ✅ Autofill status accurate

### **Common Issues & Fixes:**

**Issue:** Card not visible

- **Fix:** Scroll down on Dashboard - it's below Security Score

**Issue:** "Enable" does nothing

- **Fix:** Android 8.0+ required for autofill

**Issue:** Count shows wrong number

- **Fix:** Restart app to refresh count

---

## 🧪 **TEST 2: Add Password Dialog**

### **What to Test:**

The Add Password functionality

### **Steps:**

1. **Navigate to Passwords Screen:**
    - From Dashboard → Click "Password Manager" card → "View All"
    - OR tap "Passwords" in navigation drawer

2. **Open Add Dialog:**
    - Tap the **+ (Plus)** Floating Action Button (bottom right)
    - Dialog should slide up

3. **Fill Form:**
   ```
   Service Name: Twitter
   Username: test@email.com
   Password: Test123!@#
   URL (optional): https://twitter.com
   Category: Social (tap to change)
   Notes (optional): My test account
   ```

4. **Test Show/Hide Password:**
    - Tap the eye icon (👁/🔒) next to password field
    - Password should toggle between visible and hidden

5. **Test Category Selector:**
    - Tap the category dropdown
    - Should show: Social, Banking, Email, Shopping, etc.
    - Select "Social"

6. **Save Password:**
    - Tap "Save" button
    - Should show toast: "Password saved!"
    - Dialog should close
    - New password should appear in list

### **Expected Results:**

- ✅ Dialog opens smoothly
- ✅ All fields editable
- ✅ Password show/hide works
- ✅ Category selector works
- ✅ Save button enabled only when required fields filled
- ✅ Password appears in list after save
- ✅ Count updates in Dashboard card

### **Common Issues & Fixes:**

**Issue:** Dialog doesn't open

- **Fix:** Make sure you're tapping the + FAB, not other buttons
- **Fix:** Check if you're on Passwords screen

**Issue:** Save button disabled

- **Fix:** Fill required fields: Service Name, Username, Password

**Issue:** Password not saved

- **Fix:** Check logcat: `adb logcat | grep SafeSphere`
- **Fix:** Check storage permissions

**Issue:** Can't see new password

- **Fix:** Scroll down in password list
- **Fix:** Clear search query if any

---

## 🧪 **TEST 3: View Password Dialog**

### **Steps:**

1. **Add a password** (use TEST 2 above)

2. **Open View Dialog:**
    - Tap any password card in the list
    - Dialog should open

3. **View Details:**
    - Service name displayed at top
    - Category shown with icon
    - Username visible (not hidden)

4. **Reveal Password:**
    - Tap "🔓 Reveal Password" button
    - Wait for decryption (spinner shows)
    - Password appears as dots initially

5. **Show Password:**
    - Tap eye icon (👁)
    - Password becomes visible

6. **Copy Functions:**
    - Tap copy icon (📋) next to username
    - Toast: "Username copied"
    - Tap copy icon (📋) next to password
    - Toast: "Copied to clipboard"

7. **Delete Password:**
    - Tap "Delete" button
    - Password deleted
    - Toast: "Password deleted"
    - Dialog closes

### **Expected Results:**

- ✅ Dialog opens on tap
- ✅ All details displayed correctly
- ✅ Reveal password works
- ✅ Copy functions work
- ✅ Delete works
- ✅ Count updates after delete

---

## 🧪 **TEST 4: Autofill Setup**

### **Steps:**

1. **From Passwords Screen:**
    - See orange banner: "⚡ Enable Autofill"
    - Tap the banner

2. **In Setup Dialog:**
    - Read instructions
    - Tap "Open Settings" button

3. **In Android Settings:**
    - Look for "SafeSphere" in list
    - Tap it
    - Tap "OK" to confirm

4. **Return to App:**
    - Press back to return to SafeSphere
    - Banner should disappear
    - Status should show "✅ Autofill ON" (green)

### **Expected Results:**

- ✅ Banner visible when autofill disabled
- ✅ Settings opens correctly
- ✅ SafeSphere appears in list
- ✅ Status updates after enabling

---

## 🧪 **TEST 5: Real Autofill (System-Wide)**

### **Prerequisites:**

- Autofill enabled (TEST 4)
- At least 1 password saved (TEST 2)

### **Test with Twitter:**

1. **Open Chrome browser**

2. **Go to:** `https://twitter.com/login`

3. **Tap username field**

4. **Expected:**
    - Dropdown appears
    - Shows: "🔐 SafeSphere (1 saved)"
    - Shows: "Twitter - test@email.com"

5. **Tap the credential**

6. **Result:**
    - ✅ Username filled
    - ✅ Password filled
    - ✅ Can now login!

### **Test with Any App:**

1. **Open Instagram app**

2. **Go to login screen**

3. **Tap email field**

4. **If you have Instagram password saved:**
    - Dropdown appears with credential
    - Tap to auto-fill

### **Expected Results:**

- ✅ Dropdown appears in all apps
- ✅ Saved credentials shown
- ✅ Both fields filled on tap
- ✅ Works in Chrome, Firefox, etc.
- ✅ Works in native apps (Instagram, Facebook, etc.)

---

## 🧪 **TEST 6: Auto-Save (System-Wide)**

### **Steps:**

1. **Open Chrome**

2. **Go to:** `https://github.com/login`

3. **Enter NEW credentials:**
    - Username: your_username
    - Password: your_password

4. **Tap "Sign in"**

5. **Expected (after login):**
    - Android shows: "Save to SafeSphere?"
    - Shows service name, username, masked password

6. **Tap "Save"**

7. **Verify:**
    - Open SafeSphere app
    - Go to Passwords screen
    - New "GitHub" password should be in list

### **Expected Results:**

- ✅ Save prompt appears after login
- ✅ Credential details correct
- ✅ Saved to app after tapping "Save"
- ✅ Available for autofill next time

---

## 🧪 **TEST 7: Search & Filter**

### **Steps:**

1. **Add multiple passwords** (Twitter, Gmail, Instagram)

2. **Test Search:**
    - Type "twit" in search bar
    - Only Twitter should show
    - Clear search → all show again

3. **Test Category Filter:**
    - Tap "Social" category
    - Only social passwords show (Twitter, Instagram)
    - Tap "Social" again to clear filter

### **Expected Results:**

- ✅ Search filters instantly
- ✅ Category filter works
- ✅ Can clear filters

---

## 🧪 **TEST 8: Password Strength Indicator**

### **Steps:**

1. **Add password with weak password:** `123456`
    - Strength bar: Red
    - Label: "Weak"

2. **Add password with strong password:** `MySecureP@ss123!`
    - Strength bar: Green
    - Label: "Strong"

### **Expected Results:**

- ✅ Weak = Red bar
- ✅ Medium = Orange bar
- ✅ Strong = Green bar

---

## 📊 **Testing Checklist**

### **Dashboard Features:**

- [ ] Password Manager card visible
- [ ] Password count accurate
- [ ] Autofill status correct
- [ ] "View All" button works
- [ ] "Enable" link works
- [ ] "Add Password" button works

### **Password Management:**

- [ ] Can add password
- [ ] Can view password
- [ ] Can reveal password
- [ ] Can copy username
- [ ] Can copy password
- [ ] Can delete password
- [ ] Search works
- [ ] Category filter works

### **Autofill:**

- [ ] Can enable autofill
- [ ] Status updates correctly
- [ ] Works in Chrome
- [ ] Works in Firefox
- [ ] Works in native apps
- [ ] Auto-save works
- [ ] Auto-fill works

---

## 🐛 **Troubleshooting**

### **Problem: Autofill not working**

**Check:**

```bash
# Verify autofill service is enabled
adb shell settings get secure autofill_service
```

**Should show:** `com.runanywhere.startup_hackathon20/.autofill.SafeSphereAutofillService`

**Fix:**

```bash
# Enable manually via ADB
adb shell settings put secure autofill_service com.runanywhere.startup_hackathon20/.autofill.SafeSphereAutofillService
```

### **Problem: Passwords not saving**

**Check logs:**

```bash
adb logcat | grep -i "password\|autofill\|safesphere"
```

**Look for:**

- "Password saved successfully"
- "Encryption successful"
- Any error messages

### **Problem: Can't see saved passwords**

**Check file:**

```bash
adb shell run-as com.runanywhere.startup_hackathon20 ls files/
```

**Should see:** `password_vault.enc`

### **Problem: App crashes**

**Get crash logs:**

```bash
adb logcat | grep -i "androidruntime\|fatal\|exception"
```

---

## 🎯 **Quick Test (5 Minutes)**

**Fastest way to verify everything works:**

1. **Launch app** → Dashboard visible ✓
2. **See Password Manager card** → "0 Saved Passwords" ✓
3. **Tap "Add Password"** → Dialog opens ✓
4. **Fill form** → Save ✓
5. **See password in list** → "1 Saved Password" ✓
6. **Tap password** → View dialog opens ✓
7. **Tap "Reveal Password"** → Password shown ✓
8. **Tap copy** → "Copied to clipboard" ✓
9. **Check Dashboard** → Count shows "1" ✓

**All ✓? Everything works!** 🎉

---

## 📱 **Device Requirements**

- **Android Version:** 8.0 (API 26) or higher
- **Storage:** 50 MB free space
- **Permissions:** None required initially

---

## 🎉 **Success Criteria**

Your app is working correctly if:

- ✅ Can add passwords manually
- ✅ Can view and reveal passwords
- ✅ Can copy credentials
- ✅ Can delete passwords
- ✅ Autofill can be enabled
- ✅ Passwords auto-fill in Chrome
- ✅ Passwords auto-save when logging in
- ✅ Dashboard card shows accurate count
- ✅ All UI elements render correctly
- ✅ No crashes or errors

**READY TO DEMO!** 🚀
