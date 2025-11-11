# 🔍 **Check If Autofill is Working - Live Logs**

## 📱 **How to See What's Happening**

If autofill isn't showing, let's see WHY by checking the logs!

---

## ✅ **METHOD 1: Check Via ADB (Computer Connected)**

### **Step 1: Connect Phone to Computer**

- Connect phone via USB
- Enable USB Debugging on phone

### **Step 2: Open PowerShell**

Navigate to your project:

```powershell
cd D:/Hackathons/SafeSphere/Hackss-main/Hackss-main/Hackss-main
```

### **Step 3: Start Log Monitoring**

```powershell
adb logcat | Select-String "SafeSphere"
```

### **Step 4: Test Autofill**

1. Keep PowerShell window open
2. On phone: Open Chrome → geeksforgeeks.org
3. Tap on "Username or Email" field
4. Watch the logs!

---

## 🎯 **WHAT YOU SHOULD SEE (Success)**

When you tap the login field, logs should show:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📝 FILL REQUEST RECEIVED
📱 App: Chrome
📦 Package: com.android.chrome
✅ Login fields detected:
   👤 Username field: username
   🔑 Password field: password
🌐 Browser detected - URL: geeksforgeeks.org
🔍 Searching by domain: geeksforgeeks.org
📦 Total credentials in vault: 1
📋 Credentials in vault:
   [0] Service: 'geeksforgeeks.org', URL: 'geeksforgeeks.org', Username: 'your-username'
💾 Found 1 saved credentials
✅ Using 1 matched credentials
✅ Fill response sent successfully
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**This means:** ✅ Autofill is working! Check your keyboard for suggestions.

---

## 🔴 **PROBLEM SCENARIOS**

### **Scenario 1: No logs at all**

```
(nothing appears)
```

**This means:** ❌ SafeSphere Autofill is **NOT ENABLED** in system settings!

**Fix:**

```
Settings → Autofill service → SafeSphere Autofill ✓
```

---

### **Scenario 2: "DISCONNECTED" message**

```
❌ SafeSphere Autofill Service DISCONNECTED
```

**This means:** ❌ SafeSphere was previously enabled but got disabled

**Fix:**

1. Settings → Autofill service → Select **None**
2. Wait 5 seconds
3. Settings → Autofill service → Select **SafeSphere Autofill**
4. Restart Chrome

---

### **Scenario 3: "No login fields detected"**

```
📝 FILL REQUEST RECEIVED
📱 App: Chrome
❌ No login fields detected
```

**This means:** ❌ SafeSphere couldn't find username/password fields

**Possible reasons:**

1. Page hasn't loaded yet - wait and try again
2. Field has non-standard attributes
3. WebView doesn't expose field info

**Fix:** Try tapping both username AND password fields

---

### **Scenario 4: "Found 0 saved credentials"**

```
📝 FILL REQUEST RECEIVED
📱 App: Chrome
🌐 Browser detected - URL: geeksforgeeks.org
🔍 Searching by domain: geeksforgeeks.org
📦 Total credentials in vault: 0
💾 Found 0 saved credentials
```

**This means:** ❌ No passwords saved in SafeSphere vault

**Fix:**

1. Open SafeSphere app
2. Manually add a password:
    - Service: `geeksforgeeks.org`
    - Username: `your-username`
    - Password: `your-password`
    - URL: `geeksforgeeks.org`
3. Save and try autofill again

---

### **Scenario 5: "Found X saved credentials" but no match**

```
📦 Total credentials in vault: 3
📋 Credentials in vault:
   [0] Service: 'Twitter', URL: 'twitter.com', Username: 'user1'
   [1] Service: 'Gmail', URL: 'gmail.com', Username: 'user2'
   [2] Service: 'GeeksForGeeks', URL: 'Chrome', Username: 'user3'
💾 Found 0 saved credentials
```

**This means:** ❌ URL doesn't match (saved as "Chrome" instead of "geeksforgeeks.org")

**Fix:**

1. Open SafeSphere app
2. Find the GeeksForGeeks password entry
3. Edit it:
    - Change URL from `Chrome` to `geeksforgeeks.org`
4. Save
5. Try autofill again

---

## ✅ **METHOD 2: Check Without Computer**

### **In SafeSphere App - Verify Password is Saved**

1. Open **SafeSphere** app
2. Tap **Passwords** tab (bottom navigation)
3. Look for your GeeksForGeeks entry

**It should show:**

```
🌐 Service: geeksforgeeks.org
👤 Username: your-username
🔗 URL: geeksforgeeks.org
📅 Last modified: Today
```

**NOT:**

```
🌐 Service: Chrome
🔗 URL: (empty)
```

---

## 🧪 **LIVE TEST SEQUENCE**

### **Test 1: Service is Running**

**Command:**

```powershell
adb shell dumpsys autofill | Select-String "SafeSphere"
```

**Expected:**

```
AutofillService: com.runanywhere.startup_hackathon20/com.runanywhere.startup_hackathon20.autofill.SafeSphereAutofillService
```

If you see this → ✅ Service is enabled

---

### **Test 2: Check Saved Passwords Count**

**Command:**

```powershell
adb logcat -c
adb logcat | Select-String "Total credentials in vault"
```

Then tap login field on phone.

**Expected:**

```
📦 Total credentials in vault: 1
```

If shows 0 → ❌ No passwords saved

---

### **Test 3: Full Debug Output**

**Command:**

```powershell
adb logcat -c
adb logcat *:S SafeSphereAutofill:V
```

Then tap login field.

**Expected:** Full detailed logs of autofill process

---

## 📊 **CHECKLIST FROM LOGS**

Use logs to verify each step:

- [ ] ✅ Service connected: `CONNECTED - Ready to autofill!`
- [ ] ✅ Fill request received: `FILL REQUEST RECEIVED`
- [ ] ✅ Login fields found: `Login fields detected`
- [ ] ✅ Domain extracted: `Browser detected - URL: geeksforgeeks.org`
- [ ] ✅ Credentials in vault: `Total credentials in vault: X` (X > 0)
- [ ] ✅ Match found: `Found X saved credentials` (X > 0)
- [ ] ✅ Response sent: `Fill response sent successfully`

**All checked?** → Autofill SHOULD appear on screen!

---

## 🎯 **QUICK DEBUG SCRIPT**

Save this as `check-autofill.ps1`:

```powershell
# Clear previous logs
adb logcat -c

Write-Host "📱 Monitoring SafeSphere Autofill..." -ForegroundColor Green
Write-Host "Now tap on a login field in Chrome on your phone!" -ForegroundColor Yellow
Write-Host ""

# Monitor logs
adb logcat | Select-String "SafeSphere" | ForEach-Object {
    $line = $_.Line
    
    if ($line -match "CONNECTED") {
        Write-Host "✅ SERVICE CONNECTED" -ForegroundColor Green
    } elseif ($line -match "FILL REQUEST") {
        Write-Host "📝 FILL REQUEST DETECTED" -ForegroundColor Cyan
    } elseif ($line -match "Found (\d+) saved") {
        Write-Host "💾 $($matches[1]) CREDENTIALS MATCHED" -ForegroundColor Magenta
    } elseif ($line -match "ERROR|Failed") {
        Write-Host "❌ ERROR: $line" -ForegroundColor Red
    } else {
        Write-Host $line -ForegroundColor White
    }
}
```

Run:

```powershell
.\check-autofill.ps1
```

---

## 🔧 **FINAL VERIFICATION**

### **Everything Enabled?**

Run this command:

```powershell
adb shell settings get secure autofill_service
```

**Expected:**

```
com.runanywhere.startup_hackathon20/com.runanywhere.startup_hackathon20.autofill.SafeSphereAutofillService
```

**If shows:**

- `null` → ❌ No autofill service enabled
- `com.google...` → ❌ Google autofill is active, not SafeSphere

**Fix:** Go to Settings → Autofill service → Select SafeSphere

---

## 🎉 **SUCCESS INDICATORS**

When everything is working, you'll see:

### **In Logs:**

```
✅ SafeSphere Autofill Service CONNECTED
📝 FILL REQUEST RECEIVED  
💾 Found 1 saved credentials
✅ Fill response sent successfully
```

### **On Phone Screen:**

```
[Above keyboard]
┌─────────────────────────┐
│ 🔐 SafeSphere (1 saved) │
│  geeksforgeeks.org      │
│  your-username          │
└─────────────────────────┘
```

---

**Use these logs to debug and find exactly what's blocking autofill! 🚀**
