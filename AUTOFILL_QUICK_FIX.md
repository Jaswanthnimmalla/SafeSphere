# ⚡ SafeSphere Autofill - Quick Fix

## 🔥 **ISSUE: Save prompt not appearing**

### **INSTANT FIX (90% of cases):**

**1. Enable SafeSphere as Autofill Service:**

```
Settings → System → Languages & input → Autofill service 
→ Select "SafeSphere Autofill"
```

**2. Disable Google Password Manager:**

```
Settings → Google → Autofill → Autofill with Google → Toggle OFF
```

**3. Disable Chrome Password Manager:**

```
Chrome → Settings → Passwords
→ Toggle OFF "Save passwords"
→ Toggle OFF "Auto Sign-in"
```

**4. Test on a known-good website:**

```
Open Chrome → Go to: https://the-internet.herokuapp.com/login
Username: tomsmith
Password: SuperSecretPassword!
Tap "Login"
✅ Should see: "Save password to SafeSphere?"
```

---

## ✅ **3-SECOND CHECKLIST**

- [ ] Android 8.0+ (Settings → About phone)
- [ ] SafeSphere selected (Settings → System → Languages & input → Autofill)
- [ ] Google autofill OFF (Settings → Google → Autofill → OFF)
- [ ] Test website works (the-internet.herokuapp.com/login)

---

## 🎯 **PATH TO AUTOFILL SETTINGS (BY DEVICE)**

**Samsung:**

```
Settings → General management → Passwords and autofill → Autofill service
```

**Google Pixel:**

```
Settings → System → Languages & input → Advanced → Autofill service
```

**OnePlus/Oppo:**

```
Settings → System → Language & input → Autofill service
```

**Xiaomi:**

```
Settings → Passwords & security → Autofill with → Apps
```

**Generic Android:**

```
Settings → System → Languages & input → Advanced → Autofill service
```

---

## 📱 **VERIFY IT'S WORKING:**

### **Expected Flow:**

**New Password:**

```
1. Enter credentials on any app/website
2. Tap "Login"
3. ✅ See: "Save password to SafeSphere?"
4. Tap "Save"
5. ✅ Saved!
```

**Existing Password:**

```
1. Tap username field
2. ✅ See: "🔐 AppName - email@example.com"
3. Tap it
4. ✅ Fields auto-filled!
```

---

## ⚠️ **REQUIREMENTS**

| Requirement | Status | How to Check |
|-------------|--------|--------------|
| Android 8.0+ | ✅ | Settings → About phone → Android version |
| SafeSphere Installed | ✅ | Open SafeSphere app |
| Autofill Service Enabled | ? | Settings → System → Languages & input → Autofill service |
| Google Disabled | ? | Settings → Google → Autofill → OFF |

---

## 🆘 **STILL NOT WORKING?**

Read the full troubleshooting guide: `AUTOFILL_TROUBLESHOOTING.md`

Or install the new APK and test:

```powershell
# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Enable Autofill
Settings → System → Languages & input → Autofill service → SafeSphere

# Test
Chrome → https://the-internet.herokuapp.com/login
Username: tomsmith
Password: SuperSecretPassword!
Tap Login
```

---

**99% of autofill issues are fixed by enabling SafeSphere in Settings and disabling Google autofill!
**
