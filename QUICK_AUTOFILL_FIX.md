# 🚨 **QUICK FIX: GeeksForGeeks Autofill Not Showing**

## ⚡ **3 SETTINGS YOU MUST CHECK RIGHT NOW**

---

## ✅ **1. ENABLE SAFESPHERE AUTOFILL (MOST IMPORTANT!)**

### **On Your Phone:**

**Step 1:** Open **Settings** app

**Step 2:** Search for **"Autofill"** or go to:

- **System** → **Languages & input** → **Autofill service**

OR

- **Google** → **Autofill service**

OR

- **Passwords & accounts** → **Autofill service**

**Step 3:** **TAP ON:**

```
SafeSphere Autofill ✓
```

**NOT:**

- ❌ Google
- ❌ None

**Step 4:** You should see:

```
✓ SafeSphere Autofill
  (selected)
```

---

## ✅ **2. CHROME SETTINGS**

### **In Chrome Browser:**

**Step 1:** Open Chrome → **⋮** (three dots) → **Settings**

**Step 2:** Go to **Passwords**

**Step 3:** Make sure these are **ON**:

- ✅ **Offer to save passwords**
- ✅ **Auto Sign-in**

---

## ✅ **3. RESTART CHROME COMPLETELY**

**IMPORTANT:** Chrome caches autofill settings!

**Step 1:** Open **Recent Apps** (square button)

**Step 2:** Find Chrome, **swipe it away**

**Step 3:** Wait 3 seconds

**Step 4:** Open Chrome again

---

## 🧪 **TEST NOW:**

1. Open Chrome
2. Go to **geeksforgeeks.org**
3. Tap **Log in**
4. **Tap on "Username or Email" field**
5. Look **ABOVE YOUR KEYBOARD**

You should see:

```
┌─────────────────────────┐
│ 📱 SafeSphere (1 saved) │
│                         │
│  geeksforgeeks.org      │
│  your-username          │
└─────────────────────────┘
```

**Tap it** → ✨ **Auto-fills!**

---

## 🐛 **STILL NOT WORKING?**

### **Problem: No dropdown appears**

**Try this:**

1. Settings → Autofill service → Select **"None"**
2. Wait 5 seconds
3. Settings → Autofill service → Select **"SafeSphere Autofill"**
4. **Restart phone**
5. Try again

---

### **Problem: "SafeSphere Autofill" not in list**

**Solution: Reinstall the app**

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then go to Settings → Autofill service → Select SafeSphere

---

### **Problem: Autofill shows but wrong credentials**

**Solution: Check what was saved**

1. Open SafeSphere app
2. Go to **Passwords** tab
3. Find your GeeksForGeeks entry
4. Check the **URL** field

It should say:

- `geeksforgeeks.org` ✅
- `www.geeksforgeeks.org` ✅

NOT:

- `Chrome` ❌
- Empty ❌

If wrong, **edit it** in SafeSphere:

1. Tap the password entry
2. Edit URL to: `geeksforgeeks.org`
3. Save
4. Try autofill again

---

## 🎯 **EXPECTED BEHAVIOR**

### **When you tap login field:**

```
┌──────────────────────────────────────┐
│  Username or Email                   │  ← You tap here
│  [________________________]          │
│                                      │
│  ┌────────────────────────────────┐ │
│  │ 🔐 SafeSphere (1 saved)       │ │  ← This appears
│  │                                │ │     above keyboard
│  │ • geeksforgeeks.org           │ │
│  │   your-username               │ │  ← Tap this
│  └────────────────────────────────┘ │
│                                      │
│  Password                            │
│  [________________________]          │
│                                      │
│  [ Sign In ]                         │
└──────────────────────────────────────┘
```

---

## ✅ **FINAL CHECKLIST**

Before testing, make sure:

- [ ] ✅ Settings → **Autofill service** = **SafeSphere Autofill**
- [ ] ✅ Chrome → Settings → Passwords → **"Offer to save passwords"** = ON
- [ ] ✅ Chrome **completely closed and reopened**
- [ ] ✅ SafeSphere app has your GeeksForGeeks password saved
- [ ] ✅ You're testing on **geeksforgeeks.org** (same domain you saved)

---

## 📱 **VERIFY YOUR SAVED PASSWORD**

Open SafeSphere app right now:

1. Tap **Passwords** tab (at bottom)
2. Look for your GeeksForGeeks entry
3. It should show:
   ```
   🌐 geeksforgeeks.org
   👤 your-username
   📅 Saved today
   ```

If you see it → **autofill WILL work** after you enable the settings above!

---

## 🎉 **SUCCESS = SEE THIS:**

When autofill works, you'll see:

1. **Above keyboard**: "SafeSphere (X saved)" bar
2. **Tap it**: Shows your credentials
3. **Tap credential**: Both username AND password fill automatically
4. **No typing needed**: Just tap Sign In!

---

## 🔧 **APK LOCATION**

If you need to reinstall:

```
D:/Hackathons/SafeSphere/Hackss-main/Hackss-main/Hackss-main/app/build/outputs/apk/debug/app-debug.apk
```

Install:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

**Do these 3 steps and autofill will work! 🚀**

1. ✅ Settings → Autofill service → **SafeSphere Autofill**
2. ✅ Chrome → Settings → Passwords → **Enable "Offer to save passwords"**
3. ✅ **Restart Chrome completely**

**Then test on geeksforgeeks.org login page!**
