# Quick Test Guide - Credential Detection

## ⚡ Quick Test (5 minutes)

### Prerequisites

- SafeSphere app installed
- Autofill service enabled (Settings → System → Languages & Input → Autofill → SafeSphere)
- Android 8.0+ device

### Test Scenario 1: Twitter/X

**Step 1: Save credential manually**

1. Open SafeSphere app
2. Go to Passwords tab → "+" button
3. Fill in:
    - Service: `Twitter`
    - Username: `testuser@email.com`
    - Password: `TestPass123!`
    - URL: `twitter.com`
    - Category: Social Media
4. Save

**Step 2: Test autofill**

1. Open Chrome browser
2. Go to `https://twitter.com/login` (or `https://x.com/login`)
3. Tap on the username field
4. **Expected:** You should see "Twitter" in autofill dropdown
5. **Expected:** Selecting it fills both username and password

**Variations to test:**

- `https://twitter.com`
- `https://www.twitter.com`
- `https://mobile.twitter.com/login`
- `https://x.com` (should also work due to alias handling)

### Test Scenario 2: GitHub

**Step 1: Save credential manually**

1. Open SafeSphere app
2. Add new password:
    - Service: `GitHub`
    - Username: `developer123`
    - Password: `SecureGitHub456!`
    - URL: `github.com`
    - Category: Work
3. Save

**Step 2: Test autofill**

1. Open Chrome
2. Go to `https://github.com/login`
3. Tap on username field
4. **Expected:** See "GitHub" suggestion
5. **Expected:** Credentials fill correctly

**Variations to test:**

- `https://github.com`
- `https://www.github.com/login`
- `https://github.com/session/new`

### Test Scenario 3: Native App (Instagram)

**Step 1: Install Instagram app** (if not already installed)

**Step 2: Save credential manually in SafeSphere**

1. Service: `Instagram`
2. Username: `testaccount`
3. Password: `TestInsta789!`
4. URL: `com.instagram.android` (or just `instagram`)
5. Category: Social Media

**Step 3: Test autofill in app**

1. Open Instagram app
2. If logged in, logout
3. Go to login screen
4. Tap on username field
5. **Expected:** See autofill suggestion from SafeSphere
6. **Expected:** Select and credentials fill

## 🔍 Monitoring Logs

**Open terminal and run:**

```bash
adb logcat -s SafeSphereAutofill:D -v time
```

**What to look for:**

### ✅ SUCCESS - You should see:

```
📝 FILL REQUEST RECEIVED
📱 App: Chrome
🌐 Browser detected - URL: https://twitter.com/login
🔍 Searching by domain: twitter.com
💾 Found 1 saved credentials
   🔍 Checking password: Twitter
      [Domain Match] Comparing: 'twitter.com' vs 'twitter.com'
   ✅ MATCHED by exact domain: twitter.com
✅ Fill response sent successfully
```

### ❌ FAILURE - You would see:

```
📝 FILL REQUEST RECEIVED
📱 App: Chrome
🌐 Browser detected - URL: https://twitter.com/login
🔍 Searching by domain: twitter.com
💾 Found 1 saved credentials
   🔍 Checking password: Twitter
      [Domain Match] Comparing: 'twitter.com' vs 'facebook.com'
   ❌ No match found
❌ No login fields detected
```

## 🎯 What Should Happen

### ✅ Credentials ARE detected when:

- Exact domain match (`twitter.com` saved, visiting `twitter.com`)
- Subdomain variations (`twitter.com` saved, visiting `mobile.twitter.com`)
- With/without www (`twitter.com` saved, visiting `www.twitter.com`)
- Different paths (`twitter.com` saved, visiting `twitter.com/login`)
- Service name matches (`Twitter` saved, any twitter URL)

### ❌ Credentials are NOT detected when:

- Completely different domain (`twitter.com` saved, visiting `facebook.com`)
- Service name is completely different AND domain doesn't match
- URL field was left empty when saving

## 🔧 If Autofill Doesn't Show

### Check 1: Is autofill field detected?

In logs, look for:

```
✅ Login fields detected:
   👤 Username field: username
   🔑 Password field: password
```

**If you see "❌ No login fields detected":**

- The website/app may have custom input fields
- Autofill API may not recognize them
- This is a browser/app limitation, not SafeSphere issue

### Check 2: Is credential found?

In logs, look for:

```
💾 Found 1 saved credentials
```

**If it says "Found 0 saved credentials":**

- Credential isn't saved yet
- Check SafeSphere app → Passwords tab

### Check 3: Is matching working?

In logs, look for:

```
   🔍 Checking password: Twitter
      [Domain Match] Comparing: 'twitter.com' vs 'twitter.com'
   ✅ MATCHED by exact domain
```

**If you see "❌ No match found":**

- The matching logic didn't find a match
- Check what the "Saved URL" vs "Current URL" are
- They should be similar enough to match

## 📱 Common Test Sites

| Site | URL to Visit | URL to Save |
|------|--------------|-------------|
| Twitter | `https://twitter.com/login` | `twitter.com` |
| Facebook | `https://facebook.com/login` | `facebook.com` |
| GitHub | `https://github.com/login` | `github.com` |
| Gmail | `https://accounts.google.com` | `google.com` or `gmail.com` |
| LinkedIn | `https://www.linkedin.com/login` | `linkedin.com` |
| Reddit | `https://www.reddit.com/login` | `reddit.com` |

## 🎓 Pro Tips

1. **Always save just the domain** in the URL field:
    - ✅ Good: `twitter.com`
    - ❌ Bad: `https://twitter.com/i/flow/login?redirect_after_login=%2F`

2. **Service name matters**:
    - Use recognizable names: "Twitter", "GitHub", "Facebook"
    - Avoid vague names: "Website 1", "Login"

3. **Check logs first**:
    - Before reporting issues, check `adb logcat`
    - Logs show exactly what's being compared

4. **Test in Chrome first**:
    - Chrome has the best autofill support
    - Some browsers (Firefox, Brave) may have different behavior

5. **Native apps are tricky**:
    - Some apps don't support autofill
    - Try tapping on the input field multiple times
    - Some apps require you to tap a "password" icon first

## 🐛 Known Limitations

1. **Custom input fields**: Some websites use non-standard inputs that Android's Autofill API doesn'
   t recognize
2. **WebView apps**: Apps using embedded browsers may not support autofill
3. **Two-factor authentication**: Only username/password are filled, not 2FA codes
4. **Dynamic forms**: Forms that load via JavaScript after page load may not be detected

---

**Need help?** Check the full guide: `CREDENTIAL_DETECTION_FIX.md`
