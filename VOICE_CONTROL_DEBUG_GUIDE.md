# 🎤 Voice Control Debugging Guide

## ✅ FIXED: Voice Control Now Auto-Enabled!

Voice control is now **automatically enabled** when you log in to SafeSphere. No manual setup
needed!

---

## 📱 How to Check If It's Working

### **Step 1: Check Logcat**

```bash
# Filter for voice-related logs
adb logcat | grep -E "SafeSphere|AdvancedVoice|VoiceCommand"
```

### **Step 2: Look for These Log Messages**

#### **When App Opens:**

```
SafeSphere: App opened - currentUser: true, biometric: false, wasInBackground: false
SafeSphere: TTS initialized successfully
AdvancedVoice: Started listening...
AdvancedVoice: onReadyForSpeech - MICROPHONE IS ACTIVE
```

#### **When You Speak:**

```
AdvancedVoice: onRmsChanged: 15.2 dB - AUDIO DETECTED
AdvancedVoice: onEndOfSpeech - USER STOPPED SPEAKING, processing...
AdvancedVoice: Recognized: go to passwords
AdvancedVoice: === COMMAND PROCESSING ===
AdvancedVoice: Input: go to passwords
AdvancedVoice: Intent: NAVIGATE_PASSWORDS
AdvancedVoice: Action: NAVIGATE_PASSWORDS
AdvancedVoice: Confidence: 0.95
AdvancedVoice: Command emitted to flow with timestamp: 1234567890
SafeSphere: Voice command received (1234567890): NAVIGATE_PASSWORDS, parameter: null
SafeSphere: Navigating to passwords
```

---

## 🔧 Troubleshooting

### **Problem 1: No "Listening..." indicator**

**Cause**: Voice control not enabled or microphone permission denied

**Solution**:

```kotlin
// Check in logcat:
// AdvancedVoice: TTS initialized successfully  ← Should see this
// AdvancedVoice: Started listening...          ← Should see this

// If missing, check Settings > Voice Control toggle
```

### **Problem 2: Listening but not recognizing speech**

**Cause**: Speech recognizer not picking up audio

**Check**:

```
1. Media volume turned up?
2. Speaking clearly and loudly?
3. Microphone working? (test with voice recorder app)
4. Check logs for: "onRmsChanged: X dB - AUDIO DETECTED"
   - If no AUDIO DETECTED, microphone issue
```

### **Problem 3: Recognizes but doesn't navigate**

**Cause**: Navigation command not being handled

**Check Logs**:

```
AdvancedVoice: Action: NAVIGATE_PASSWORDS    ← Command recognized
SafeSphere: Voice command received...        ← Should see this
SafeSphere: Navigating to passwords          ← Should see this

If missing "Voice command received", the wrapper isn't working
```

### **Problem 4: "ERROR 13: Permission issue"**

**Cause**: Microphone permission revoked

**Solution**:

```bash
# Grant permission manually
adb shell pm grant com.runanywhere.startup_hackathon20 android.permission.RECORD_AUDIO

# Or in app: Settings > Apps > SafeSphere > Permissions > Microphone > Allow
```

---

## 🎯 Commands to Test

### **Navigation Commands:**

```
✅ "go home"
✅ "open passwords"
✅ "show vault"
✅ "go to settings"
✅ "AI chat"
✅ "check security"
✅ "AI predictor"
```

### **Natural Variations:**

```
✅ "take me home"
✅ "I want to see passwords"
✅ "show me the vault"
✅ "open settings"
✅ "check my security"
```

---

## 📊 Expected Behavior

### **1. App Opens (After Login)**

- ✅ Welcome message speaks: "Welcome to SafeSphere, your safety, your control"
- ✅ Voice control auto-enabled (no manual toggle needed)
- ✅ Listening starts 3 seconds after welcome
- ✅ Bottom indicator shows: 🎤 "Listening..."

### **2. You Speak a Command**

- ✅ Indicator changes to: ⚙️ "Processing..."
- ✅ NLP analysis displays with confidence score
- ✅ App navigates to the requested screen
- ✅ Voice speaks: "Opening passwords" (or similar)
- ✅ Listening resumes automatically

### **3. Continuous Operation**

- ✅ Always listening (no button press needed)
- ✅ Recognizes natural language
- ✅ Extracts entities (app names, etc.)
- ✅ Auto-recovers from errors
- ✅ Visual feedback at bottom of screen

---

## 🐛 Common Issues & Fixes

### **Issue: "No wake word detected, ignoring"**

**Cause**: Wake word mode enabled (shouldn't be)

**Fix**: Already fixed in code - `voiceSystem.enable(wakeWordMode = false)`

### **Issue: "ERROR_NO_MATCH: No speech matched"**

**Cause**: Speech recognizer didn't understand

**Fix**: Speak clearly, check these patterns:

```
✅ Short, clear commands
✅ Pause before/after speaking
✅ Speak at normal volume
❌ Too fast or too quiet
❌ Background noise
```

### **Issue: Command recognized but wrong screen**

**Cause**: NLP mapping issue

**Check**: Look at NLP analysis display - does intent match what you said?

```
Input: "go to passwords"
Intent: Navigate passwords  ← Should match
Confidence: 95%             ← Should be high
```

### **Issue: Welcome message doesn't play**

**Cause**: TTS not ready or volume muted

**Check**:

```
1. Media volume > 0?
2. TTS initialized? (check logs)
3. Device has Google TTS installed?
```

---

## 🔍 Full Debug Command

Run this to see EVERYTHING:

```bash
adb logcat -v time *:S SafeSphere:D AdvancedVoice:D VoiceCommand:D AndroidRuntime:E
```

**What to look for:**

1. ✅ "TTS initialized successfully"
2. ✅ "Started listening..."
3. ✅ "onReadyForSpeech"
4. ✅ "onRmsChanged" (when speaking)
5. ✅ "Recognized: [your command]"
6. ✅ "Command emitted to flow"
7. ✅ "Voice command received"
8. ✅ "Navigating to [screen]"

---

## 🎬 Test Scenario

**Run this test to verify everything works:**

1. **Build and install:**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

2. **Open app and login**

3. **Watch logcat:**

```bash
adb logcat | grep -E "SafeSphere|AdvancedVoice"
```

4. **You should see:**

```
AdvancedVoice: TTS initialized successfully
AdvancedVoice: Speaking: "Welcome to SafeSphere, your safety, your control"
AdvancedVoice: TTS onStart: welcome
AdvancedVoice: Started listening...
AdvancedVoice: onReadyForSpeech - MICROPHONE IS ACTIVE
```

5. **Speak: "go to passwords"**

6. **You should see:**

```
AdvancedVoice: onRmsChanged: 12.5 dB - AUDIO DETECTED
AdvancedVoice: onEndOfSpeech - USER STOPPED SPEAKING
AdvancedVoice: Recognized: go to passwords
AdvancedVoice: === COMMAND PROCESSING ===
AdvancedVoice: Input: go to passwords
AdvancedVoice: Intent: NAVIGATE_PASSWORDS
AdvancedVoice: Action: NAVIGATE_PASSWORDS
AdvancedVoice: Command emitted to flow with timestamp: 1234567890
SafeSphere: Voice command received (1234567890): NAVIGATE_PASSWORDS
SafeSphere: Navigating to passwords
```

7. **Screen should change to passwords**

---

## ✅ Success Checklist

- [ ] App opens without errors
- [ ] Welcome message plays
- [ ] Listening indicator appears
- [ ] Microphone icon pulses
- [ ] Speaking triggers "Processing..."
- [ ] Command displays with confidence score
- [ ] Navigation happens
- [ ] Voice speaks confirmation
- [ ] Listening resumes automatically

---

## 🆘 Still Not Working?

**Share these logs:**

```bash
# Capture 30 seconds of logs while testing
adb logcat -v time > voice_debug.txt &
# (speak command)
# (wait 5 seconds)
killall adb

# Share voice_debug.txt
```

**Include**:

1. Device model
2. Android version
3. Command you spoke
4. What happened vs expected
5. Full logs from above

---

## 🎤 Voice Control is READY!

After this fix, voice control should work automatically:

- ✅ Auto-enabled on login
- ✅ No manual setup
- ✅ Continuous listening
- ✅ Natural language commands
- ✅ Visual & audio feedback

**Just speak and it works!** 🚀