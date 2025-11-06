# 🔐 SafeSphere - Privacy-First Security Platform

**Your Data. Your Device. Your Privacy.**

SafeSphere is a cutting-edge mobile application that demonstrates the power of **local-first
computing**, **military-grade encryption**, and **offline AI** to protect user privacy and prevent
cyber-attacks.

---

## 🌟 Overview

SafeSphere is a comprehensive privacy protection platform built with the **RunAnywhere SDK** that
proves data doesn't need to leave your device to be useful. It combines:

- **🔒 AES-256-GCM Encryption** - Military-grade data protection
- **🤖 Offline AI** - Privacy advisor that runs entirely on-device
- **🛡️ Hardware-Backed Security** - Keys stored in Android KeyStore
- **📴 Offline-First** - No internet required for core functionality
- **✍️ Digital Signatures** - RSA-2048 for data integrity
- **📊 Visual Data Mapping** - Transparent storage visualization

---

## 🎯 Key Features

### 1. **Privacy Vault** 🔐

- Store sensitive data with AES-256-GCM encryption
- Organized by categories (Personal, Financial, Medical, etc.)
- Each item individually encrypted and digitally signed
- Decrypt on-demand with hardware-backed keys

### 2. **Offline AI Chat** 💬

- Privacy advisor running 100% on your device
- No data sent to cloud servers
- Real-time streaming responses
- Powered by RunAnywhere SDK with local LLM inference

### 3. **Data Map** 📊

- Visual representation of your encrypted storage
- Security score (0-100) based on encryption coverage
- Category breakdown of stored items
- Storage usage tracking

### 4. **Threat Simulation** ⚡

- Educational security scenarios
- Shows how SafeSphere mitigates real-world threats
- Demonstrates benefits of offline-first architecture

---

## 🚀 Quick Start

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 24 (Android 7.0) or higher
- ~500 MB free storage (for AI models)
- Physical Android device recommended

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Hackss-main
   ```

2. **Open in Android Studio**
    - File → Open → Select project directory
    - Wait for Gradle sync to complete

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   # Or click Run in Android Studio
   ```

### First Launch

1. **Onboarding** - Learn about SafeSphere's privacy principles
2. **Initialize Demo Data** - Tap "Get Started" to load sample encrypted items
3. **Download AI Model** - Navigate to "Models" and download a privacy advisor
4. **Load Model** - Tap "Load Model" to enable AI chat
5. **Explore Features** - Try the vault, chat, data map, and threat simulation

---

## 🏗️ Architecture

### Technology Stack
```
SafeSphere Application
├── RunAnywhere SDK (Core & LLM Module)
├── Android KeyStore (Hardware Security)
├── Jetpack Compose (Modern UI)
├── Kotlin Coroutines (Async Operations)
└── Material Design 3 (Glass-morphism Theme)
```

### Project Structure

```
app/src/main/java/com/runanywhere/startup_hackathon20/
├── SafeSphereApplication.kt       # App initialization & SDK setup
├── SafeSphereMainActivity.kt      # Main UI entry point
├── data/
│   ├── PrivacyVaultItem.kt        # Data models
│   └── PrivacyVaultRepository.kt  # Encrypted storage management
├── security/
│   └── SecurityManager.kt         # Encryption & key management
├── viewmodels/
│   └── SafeSphereViewModel.kt     # State management & business logic
└── ui/
    ├── SafeSphereTheme.kt         # Dark glass-morphism theme
    ├── SafeSphereComponents.kt    # Reusable UI components
    └── SafeSphereScreens.kt       # All app screens
```

---

## 🔒 Security Features

### Encryption Details

**Algorithm**: AES-256-GCM (Galois/Counter Mode)

- **Key Size**: 256 bits
- **IV Size**: 12 bytes (96 bits)
- **Tag Size**: 128 bits
- **Provider**: Android KeyStore (hardware-backed when available)

### Key Management

**AES Key**

- Generated in Android KeyStore
- Never extractable from hardware
- Unique per device

**RSA Key Pair**

- 2048-bit key size
- SHA-256 digest algorithm
- Used for signing/verification

---

## 📚 Resources

### Documentation

- [SafeSphere Complete Guide](SAFESPHERE_README.md)
- [Developer Guide](SAFESPHERE_DEVELOPER_GUIDE.md)
- [Advanced Features](SAFESPHERE_ADVANCED_FEATURES.md)
- [Enterprise Implementation](SAFESPHERE_ENTERPRISE_IMPLEMENTATION.md)

### RunAnywhere SDK

- [GitHub Repository](https://github.com/RunanywhereAI/runanywhere-sdks)
- [Complete Documentation](RUNANYWHERE_SDK_COMPLETE_GUIDE.md)
- [Quick Start Guide](app/src/main/java/com/runanywhere/startup_hackathon20/QUICK_START_ANDROID.md)

---

## 🤝 Contributing

We welcome contributions! Areas for improvement:

- **Additional Vault Categories** - More data types
- **Biometric Authentication** - Fingerprint/face unlock
- **Backup/Restore** - Encrypted export functionality
- **More AI Models** - Additional privacy-focused models
- **Tablet UI** - Optimized layout for larger screens

---

## 📄 License

This project follows the license of the RunAnywhere SDK.

---

## 🙏 Acknowledgments

- **RunAnywhere Team** - For the amazing offline AI SDK
- **llama.cpp** - Efficient local inference engine
- **Android KeyStore** - Hardware-backed security
- **Material Design** - Beautiful UI components

---

**Built with ❤️ and 🔐 by privacy advocates, for privacy advocates.**

*"The best security is the kind users don't have to think about."*
