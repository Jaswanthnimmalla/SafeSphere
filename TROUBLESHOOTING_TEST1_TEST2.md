# 🔧 Troubleshooting: TEST 1 & TEST 2 Not Working

## ❌ **Problem: TEST 1 and TEST 2 Both Not Working**

---

## 🧪 **TEST 1 Issue: Password Manager Card Not Working**

### **Common Problems:**

#### **Problem 1: Card Not Visible**

**Symptoms:**

- Don't see "🗝️ Password Manager" card on Dashboard
- Dashboard loads but card is missing

**Solutions:**

1. **Scroll Down:**
   ```
   The card is positioned AFTER "Security Score" card
   Try scrolling down on the Dashboard
   ```

2. **Check if you're on Dashboard:**
   ```
   - Look at top bar - should say "SafeSphere"
   - Should see "Security Score" card at top
   - Password Manager card is below it
   ```

3. **Rebuild the app:**
   ```bash
   .\gradlew.bat clean
   .\gradlew.bat assembleDebug
   ```

4. **Force close and reopen:**
    - Settings → Apps → SafeSphere → Force Stop
    - Open app again

#### **Problem 2: Buttons Don't Work**

**Symptoms:**

- Card visible but clicking buttons does nothing
- "View All" or "Add Password" not responding

**Solutions:**

1. **Check logcat for errors:**
   ```bash
   # If you have Android Studio open:
   # View → Tool Windows → Logcat
   
   # Look for errors when clicking buttons
   ```

2. **Verify navigation is set up:**
    - Make sure you logged in successfully
    - Dashboard should be the main screen after login

3. **Try the navigation drawer instead:**
    - Tap hamburger menu (☰) at top left
    - Tap "Passwords" from the drawer
    - This is an alternative way to access Passwords screen

#### **Problem 3: Count Shows "0" Even After Adding Passwords**

**Symptoms:**

- Added passwords but card still shows "0 Saved Passwords"

**Solutions:**

1. **Restart the app:**
    - Force close completely
    - Reopen
    - Count should refresh

2. **Check if passwords are actually saved:**
    - Open navigation drawer (☰)
    - Tap "Passwords"
    - Do you see any passwords in the list?
    - If yes: Just a display issue, restart app
    - If no: Passwords didn't save (see TEST 2 issues below)

---

## 🧪 **TEST 2 Issue: Add Password Dialog Not Working**

### **Common Problems:**

#### **Problem 1: + Button (FAB) Not Visible**

**Symptoms:**

- On Passwords screen but no + button

**Solutions:**

1. **Check if you're on the right screen:**
   ```
   Top should say "Passwords" (not "SafeSphere")
   Should see search bar
   Should see category filters
   + button is at bottom-right corner
   ```

2. **Scroll down:**
    - The + button is floating at bottom-right
    - Make sure you're not in a dialog already

#### **Problem 2: Dialog Doesn't Open**

**Symptoms:**

- Tap + button but nothing happens

**Solutions:**

1. **Check for existing dialogs:**
    - Press back button to close any open dialogs
    - Try + button again

2. **Check memory:**
    - Device may be low on memory
    - Close other apps
    - Try again

3. **Restart app:**
    - Force close SafeSphere
    - Reopen and try again

#### **Problem 3: Can't Type in Fields**

**Symptoms:**

- Dialog opens but keyboard doesn't show
- Can't enter text in fields

**Solutions:**

1. **Tap the field directly:**
    - Make sure you tap inside the text field
    - Not on the label

2. **Check keyboard:**
    - Settings → System → Languages & input → On-screen keyboard
    - Make sure a keyboard is enabled

3. **Try rotating device:**
    - Sometimes triggers keyboard to show

#### **Problem 4: Save Button Doesn't Work**

**Symptoms:**

- Filled all fields but "Save" button is grayed out
- OR button is blue but nothing happens when tapped

**Solutions:**

1. **Fill REQUIRED fields:**
   ```
   REQUIRED (must fill):
   - Service Name
   - Username
   - Password
   
   OPTIONAL (can skip):
   - URL
   - Notes
   ```

2. **Check minimum length:**
    - Make sure fields are not empty
    - Type at least one character in each required field

3. **Look for error messages:**
    - Red text below fields
    - Toast messages at bottom

#### **Problem 5: Password Saved But Not Visible**

**Symptoms:**

- Tap "Save", see "Password saved!" toast
- But password doesn't appear in list

**Solutions:**

1. **Scroll down in password list:**
    - New password added at end
    - Scroll to bottom

2. **Clear any search query:**
    - Look at search bar at top
    - If there's text, tap X to clear
    - Password should appear

3. **Clear category filter:**
    - If a category is selected (highlighted)
    - Tap it again to clear filter
    - All passwords should show

4. **Check storage:**
   ```bash
   # In Android Studio Terminal or Command Prompt:
   
   # Check if file was created:
   adb shell run-as com.runanywhere.startup_hackathon20 ls -la files/
   
   # Should see: password_vault.enc
   ```

---

## 🔍 **Step-by-Step Debugging**

### **For TEST 1 (Password Manager Card):**

1. **Launch app**
2. **Login/Register if needed**
3. **Check you're on Dashboard:**
    - Top bar says "SafeSphere"
    - See Security Score card
4. **Scroll down slowly**
5. **Look for card with:**
    - 🗝️ emoji
    - "Password Manager" text
    - Orange or green badge
6. **If still not visible:**
    - Open navigation drawer (☰)
    - Tap "Passwords" instead

### **For TEST 2 (Add Password):**

1. **Get to Passwords screen** (via TEST 1 or drawer)
2. **Verify you're there:**
    - Top says "Passwords"
    - See search bar
    - See categories below
3. **Look bottom-right for + button**
    - It's a circular floating button
    - Blue/purple color
    - Has + icon
4. **Tap it**
5. **Dialog should slide up from bottom**
6. **Fill form:**
   ```
   Service Name: TestService
   Username: test@test.com
   Password: Test123
   ```
7. **Tap "Save"**
8. **Look for toast: "Password saved!"**
9. **Check list - new entry should appear**

---

## 🛠️ **Emergency Fixes**

### **If Nothing Works:**

1. **Uninstall and Reinstall:**
   ```bash
   # Uninstall
   adb uninstall com.runanywhere.startup_hackathon20
   
   # Rebuild
   .\gradlew.bat clean
   .\gradlew.bat assembleDebug
   
   # Reinstall
   adb install app\build\outputs\apk\debug\app-debug.apk
   ```

2. **Clear App Data:**
    - Settings → Apps → SafeSphere
    - Storage → Clear Storage
    - Reopen app

3. **Test on Different Device:**
    - Try an emulator instead of physical device
    - Or vice versa

4. **Check Android Version:**
    - Settings → About Phone → Android Version
    - Need Android 8.0+ for full features
    - Some features work on Android 7.0+

---

## 📸 **What You Should See**

### **TEST 1 - Password Manager Card:**

```
┌─────────────────────────────────────┐
│ Security Score                      │
│           85                         │
│  ●●●●●●●●●●●●●●                     │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ 🗝️ Password Manager    [View All]  │
│                                     │
│ 0 Saved Passwords  [Autofill: OFF] │
│                                     │
│ [Add Password]                      │
└─────────────────────────────────────┘  ← This card!
```

### **TEST 2 - Add Password Dialog:**

```
When you tap +, you should see:

┌─────────────────────────────────────┐
│ Add Password                        │
│                                     │
│ Service Name                        │
│ [Twitter            ]               │
│                                     │
│ Username/Email                      │
│ [test@email.com     ]               │
│                                     │
│ Password                        👁  │
│ [•••••••••          ]               │
│                                     │
│ URL (Optional)                      │
│ [https://twitter.com]               │
│                                     │
│ Category                            │
│ [📱 Social          ▼]              │
│                                     │
│ [Cancel]  [Save]                    │
└─────────────────────────────────────┘
```

---

## ✅ **Quick Verification**

Run this checklist:

- [ ] App installs without errors
- [ ] Can login/register
- [ ] See Dashboard screen
- [ ] See "Security Score" card
- [ ] **Scroll down** - see "Password Manager" card
- [ ] Tap "View All" - goes to Passwords screen
- [ ] See + button at bottom-right
- [ ] Tap + - dialog opens
- [ ] Can type in "Service Name" field
- [ ] Can type in "Username" field
- [ ] Can type in "Password" field
- [ ] "Save" button becomes enabled
- [ ] Tap "Save" - see toast message
- [ ] Password appears in list

**If all ✅ - both tests working!**

---

## 🆘 **Still Not Working?**

### **Share This Information:**

1. **Android Version:**
    - Settings → About Phone → Android Version
    - Example: "Android 12"

2. **Device Model:**
    - Settings → About Phone → Model
    - Example: "Pixel 6" or "Emulator API 30"

3. **What You See:**
    - Take screenshot of Dashboard
    - Take screenshot of Passwords screen
    - Describe what happens when you tap buttons

4. **Error Messages:**
    - Any red text
    - Any toast messages
    - Any crash dialogs

5. **Logcat Output:**
   ```bash
   # In Android Studio:
   # View → Tool Windows → Logcat
   # Filter by "SafeSphere"
   # Copy any errors
   ```

---

## 💡 **Alternative Testing Method**

If TEST 1 and TEST 2 don't work, try **TEST 6 (Auto-Save)** instead:

1. **Enable Autofill:**
    - Settings → System → Languages & input → Autofill service
    - Select "SafeSphere"

2. **Open Chrome**

3. **Go to: `https://github.com/login`**

4. **Enter any credentials:**
    - Username: testuser
    - Password: testpass123

5. **Tap "Sign in"**

6. **Look for Android system prompt: "Save to SafeSphere?"**

7. **Tap "Save"**

8. **Open SafeSphere → Passwords**

9. **Should see "GitHub" password in list!**

This tests if the core password storage works, even if the UI has issues.

---

**Good luck! 🍀**
