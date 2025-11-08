# 🔐 SafeSphere - Keystore Setup Guide

## ✅ **SET YOUR OWN PASSWORDS - SIMPLE METHOD**

Follow these 3 easy steps:

---

## 📝 **STEP 1: GENERATE KEYSTORE**

Open PowerShell and run:

```powershell
cd "D:/Hackathons/SafeSphere/Hackss-main/Hackss-main/Hackss-main"

keytool -genkey -v -keystore safesphere-release.keystore -alias safesphere -keyalg RSA -keysize 2048 -validity 10000
```

### **You'll be asked:**

```
Enter keystore password: [Type your password - it won't show on screen]
Re-enter new password: [Type same password again]

Enter key password for <safesphere>
    (RETURN if same as keystore password): [Press ENTER to use same password]

What is your first and last name?
  [Unknown]: Your Name or SafeSphere

What is the name of your organizational unit?
  [Unknown]: SafeSphere

What is the name of your organization?
  [Unknown]: SafeSphere

What is the name of your City or Locality?
  [Unknown]: Your City

What is the name of your State or Province?
  [Unknown]: Your State

What is the two-letter country code for this unit?
  [Unknown]: IN (for India) or your country

Is CN=..., OU=..., O=..., L=..., ST=..., C=... correct?
  [no]: yes
```

### **Result:**

✅ File created: `safesphere-release.keystore`

⚠️ **IMPORTANT:**

- Remember your password! Write it down safely.
- You'll need it for ALL future app updates
- If you lose it, you can't update the app on Play Store!

---

## 📝 **STEP 2: ADD YOUR PASSWORD TO GRADLE.PROPERTIES**

1. **Open:** `gradle.properties` in your project root
2. **Find these lines:**
   ```properties
   KEYSTORE_PASSWORD=YOUR_PASSWORD_HERE
   KEY_PASSWORD=YOUR_PASSWORD_HERE
   ```
3. **Replace with your actual password:**
   ```properties
   KEYSTORE_PASSWORD=MySecure123Password!
   KEY_PASSWORD=MySecure123Password!
   ```

### **Example:**

**Before:**

```properties
KEYSTORE_PASSWORD=YOUR_PASSWORD_HERE
KEY_PASSWORD=YOUR_PASSWORD_HERE
```

**After (with your actual password):**

```properties
KEYSTORE_PASSWORD=SafeSphere2025!
KEY_PASSWORD=SafeSphere2025!
```

**Note:** Both passwords are usually the same (you pressed ENTER in Step 1)

⚠️ **SECURITY:**

- ✅ `gradle.properties` is already in `.gitignore`
- ✅ Your passwords won't be committed to Git
- ✅ Keep this file private!

---

## 📦 **STEP 3: BUILD RELEASE AAB**

Now you can build the release version:

```powershell
cd "D:/Hackathons/SafeSphere/Hackss-main/Hackss-main/Hackss-main"

# Clean previous builds
./gradlew clean

# Build release AAB
./gradlew bundleRelease
```

### **Output:**

✅ `app/build/outputs/bundle/release/app-release.aab`

This AAB file is ready to upload to Play Store!

---

## 🔍 **VERIFY IT WORKED**

After building, you should see:

```
BUILD SUCCESSFUL in Xs

> Task :app:bundleRelease
Built the following apks:
app-release.aab
```

**No password errors** = Success! ✅

---

## 🎯 **QUICK REFERENCE**

### **Your Setup:**

- **Keystore file:** `safesphere-release.keystore` (in project root)
- **Keystore password:** (what you entered in Step 1)
- **Key alias:** `safesphere`
- **Key password:** (same as keystore password)
- **Validity:** 10,000 days (~27 years)

### **Passwords stored in:**

- `gradle.properties` → `KEYSTORE_PASSWORD`
- `gradle.properties` → `KEY_PASSWORD`

### **Files to NEVER commit to Git:**

- ❌ `safesphere-release.keystore`
- ❌ `gradle.properties` (with real passwords)
- ❌ `*.aab` files
- ✅ Already protected by `.gitignore`

---

## 🔄 **FOR FUTURE UPDATES**

When you need to publish updates:

1. Update version in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 2  // Increment
   versionName = "1.1.0"
   ```

2. Build AAB (same command):
   ```powershell
   ./gradlew clean bundleRelease
   ```

3. Upload to Play Console

**Same keystore, same passwords - Forever!**

---

## ⚠️ **IMPORTANT WARNINGS**

### **DO:**

- ✅ Save your keystore password in a password manager
- ✅ Backup `safesphere-release.keystore` file (multiple locations)
- ✅ Keep passwords private
- ✅ Use the same keystore for ALL future updates

### **DON'T:**

- ❌ Lose your keystore file
- ❌ Forget your password
- ❌ Commit keystore to Git
- ❌ Share your keystore publicly
- ❌ Create a new keystore later (can't update app!)

---

## 🆘 **TROUBLESHOOTING**

### **Error: "keystore password was incorrect"**

**Solution:**

1. Check password in `gradle.properties`
2. Make sure it matches what you entered in Step 1
3. No extra spaces or quotes needed

### **Error: "Keystore file ... not found"**

**Solution:**

1. Check keystore exists: `dir safesphere-release.keystore`
2. Should be in project root (same folder as `gradlew`)
3. Re-generate if missing (Step 1)

### **Error: "Key with alias 'safesphere' not found"**

**Solution:**

- Keystore file is wrong or corrupted
- Re-generate keystore (Step 1)

---

## ✅ **DONE!**

You've successfully:

- ✅ Created your release keystore
- ✅ Set your own passwords
- ✅ Configured Gradle to use them
- ✅ Built release AAB

**Next step:** Upload AAB to Play Store!

See: `PLAY_STORE_RELEASE_GUIDE.md` for upload instructions.

---

## 📞 **NEED HELP?**

If you get errors:

1. Check password is correct in `gradle.properties`
2. Verify keystore file exists
3. Try building: `./gradlew bundleRelease`
4. Check build output for specific errors

**Your passwords are safe and easy to manage in `gradle.properties`!** 🔐✨
