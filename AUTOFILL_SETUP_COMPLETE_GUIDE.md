# 🔐 **SafeSphere Autofill - Complete Setup Guide**

## ✅ **CRITICAL: Phone Settings You MUST Enable**

### **Step 1: Enable SafeSphere as Autofill Service**

1. Open your phone **Settings** app
2. Search for **"Autofill"** in settings
3. Go to one of these (depends on your Android version):
    - **Passwords & accounts** → **Autofill service**
    - **System** → **Languages & input** → **Autofill service**
    - **Google** → **Autofill** → **Autofill service**
    - **Passwords, passkeys & autofill** → **Autofill service**

4. **SELECT: SafeSphere Autofill** ✅

5. You should see:
   ```
   ✓ SafeSphere Autofill
     (Currently selected)
   ```

---

### **Step 2: Disable Competing Autofill Services**

⚠️ **IMPORTANT**: Only ONE autofill service can be active at a time!

Make sure these are **DISABLED**:

- ❌ Google Autofill
- ❌ Google Password Manager
- ❌ Chrome Password Manager
- ❌ Any other password managers

**How to disable Google Autofill:**

1. Settings → **Google** → **Autofill**
2. Turn OFF **"Autofill with Google"**
3. Or just select SafeSphere instead

---

### **Step 3: Chrome Browser Settings**

Open **Chrome** → **⋮ (three dots)** → **Settings**:

#### **A. Enable Autofill for Chrome**

1. Go to **Passwords**
2. Make sure these are **ON**:
    - ✅ **"Offer to save passwords"**
    - ✅ **"Auto Sign-in"**

#### **B. Clear Chrome's Own Password Manager**

1. In Chrome Settings → **Passwords**
2. If you see saved passwords there, **don't worry** - Chrome will now use SafeSphere
3. Chrome will show SafeSphere suggestions instead

#### **C. Important Chrome Flags (Optional but Helpful)**

Type in Chrome address bar: `chrome://flags`

Search and **ENABLE** these:

- ✅ **"Autofill"** → Enabled
- ✅ **"Touch To Fill"** → Enabled

Then **Restart Chrome**

---

## 🧪 **Testing Your Setup**

### **Test 1: Verify SafeSphere is Active**

1. Open any website login (e.g., twitter.com, facebook.com)
2. Tap on the **Username/Email** field
3. You should see a **keyboard bar above keyboard** saying:
   ```
   SafeSphere (X saved)
   ```

### **Test 2: Save a New Password**

1. Go to **geeksforgeeks.org** (or any site)
2. Enter username and password
3. Click **Sign In**
4. After login, you should see:
   ```
   Save to SafeSphere? ✓
   ```
5. Tap **Save**
6. ✅ Password is now saved!

### **Test 3: Auto-Fill the Password**

1. **Log out** from GeeksForGeeks
2. Go back to login page
3. Tap on **Username or Email** field
4. You should see above keyboard:
   ```
   📱 SafeSphere (1 saved)
   
   geeksforgeeks.org
   your-username
   ```
5. **Tap on the suggestion**
6. ✨ Both username and password should auto-fill!

---

## 🐛 **Troubleshooting: Autofill Not Showing?**

### **Problem 1: No Autofill Suggestions Appear**

**Solution A: Check Autofill Service**

```
Settings → Autofill service → Make sure "SafeSphere Autofill" is selected
```

**Solution B: Restart Chrome**

1. Close Chrome completely (swipe away from Recent Apps)
2. Open Chrome again
3. Try the login page again

**Solution C: Clear Chrome Cache**

1. Chrome → Settings → Privacy → Clear browsing data
2. Select: **Cached images and files**
3. Click **Clear data**

### **Problem 2: "Save to SafeSphere?" Doesn't Appear**

**Solution: Check Save Info is Enabled**

1. This should be automatic
2. Make sure you're actually submitting the login form (clicking Sign In button)
3. The save prompt appears AFTER successful login

### **Problem 3: Wrong Credentials Shown**

**Solution: Domain Matching**

- SafeSphere matches by domain name
- If you saved "geeksforgeeks.org", it will show on "www.geeksforgeeks.org"
- If it doesn't match, the URL might be different

---

## 🔍 **How SafeSphere Matches Credentials**

SafeSphere uses **smart matching**:

1. **Exact Domain Match** (Best)
    - Saved: `geeksforgeeks.org`
    - Shows on: `geeksforgeeks.org`, `www.geeksforgeeks.org`, `m.geeksforgeeks.org`

2. **Subdomain Match**
    - Saved: `github.com`
    - Shows on: `github.com`, `gist.github.com`, `mobile.github.com`

3. **App Name Match**
    - Saved: "Twitter" (from com.twitter.android app)
    - Shows on: twitter.com in browser

4. **Fallback: Show All Passwords**
    - If no perfect match, SafeSphere shows all saved passwords
    - You can manually select the right one

---

## 📱 **Current Saved Password Structure**

When you saved GeeksForGeeks login, SafeSphere stored:

```kotlin
Service: "Chrome" or "geeksforgeeks.org"
Username: "your-username"
Password: "••••••••" (encrypted)
URL: "geeksforgeeks.org"
Category: WEB
```

---

## ✅ **FINAL CHECKLIST**

Before testing, verify:

- [ ] SafeSphere app is **installed**
- [ ] Settings → **Autofill service** = **SafeSphere Autofill** ✓
- [ ] Google Autofill is **disabled** or SafeSphere is **selected**
- [ ] Chrome → Settings → **"Offer to save passwords"** = ON
- [ ] You have **saved at least one password** in SafeSphere
- [ ] Chrome has been **restarted** after enabling SafeSphere

---

## 🎯 **Expected Behavior**

### **When Saving:**

1. Enter credentials → Click Sign In
2. See: **"Save to SafeSphere?"** (small system dialog)
3. Tap **Save**
4. ✅ Saved to SafeSphere vault

### **When Filling:**

1. Tap username/email field
2. See keyboard bar: **"SafeSphere (X saved)"**
3. Tap your credential
4. ✨ Username + Password filled automatically!

---

## 🚨 **If Still Not Working**

### **Check Autofill Permissions:**

1. Settings → Apps → SafeSphere
2. Check permissions:
    - ✅ Special app access → Display over other apps → **Allowed**
    - ✅ Autofill service → **Allowed**

### **Re-enable SafeSphere Autofill:**

1. Settings → Autofill service → Select **None**
2. Wait 5 seconds
3. Settings → Autofill service → Select **SafeSphere Autofill**
4. Restart Chrome
5. Try again

### **Check Logs (Developer Mode):**

Connect phone to computer:

```bash
adb logcat | grep "SafeSphere"
```

You should see when you tap login fields:

```
📝 FILL REQUEST RECEIVED
📱 App: Chrome
🌐 Browser detected - URL: geeksforgeeks.org
🔍 Searching by domain: geeksforgeeks.org
💾 Found 1 saved credentials
✅ Fill response sent successfully
```

---

## 🎉 **Success Indicators**

You'll know it's working when:

1. ✅ Keyboard shows "SafeSphere (X saved)" bar
2. ✅ Tapping shows your saved credentials
3. ✅ Tapping credential fills both username and password
4. ✅ After login, "Save to SafeSphere?" appears

---

## 📞 **Quick Settings Path Reference**

### **Android 13/14:**

```
Settings → Passwords, passkeys & autofill → 
Autofill service → SafeSphere Autofill ✓
```

### **Android 12:**

```
Settings → System → Languages & input → 
Autofill service → SafeSphere Autofill ✓
```

### **Android 11:**

```
Settings → System → Languages & input → Advanced →
Autofill service → SafeSphere Autofill ✓
```

### **Android 10:**

```
Settings → System → Languages & input →
Autofill service → SafeSphere Autofill ✓
```

---

## 🔐 **Your GeeksForGeeks Password**

Since you already saved it, check in SafeSphere app:

1. Open SafeSphere
2. Go to **Passwords** tab
3. You should see:
   ```
   🌐 geeksforgeeks.org
   👤 your-username
   ```

Now when you go to geeksforgeeks.org in Chrome and tap login fields, SafeSphere will show this
credential!

---

**That's it! Your autofill should now work perfectly! 🚀**
