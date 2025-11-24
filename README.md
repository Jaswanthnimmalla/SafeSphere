# 🔐 SafeSphere - Privacy-First Security Platform

**Your Data. Your Device. Your Privacy.**

SafeSphere is a cutting-edge mobile application that demonstrates the power of **local-first
computing**, **military-grade encryption**, and **offline AI** to protect user privacy and prevent
cyber-attacks.

---

## 🌟 Overview

SafeSphere is a comprehensive privacy protection platform that proves data doesn't need to leave
your device to be useful. It combines:

- **🔒 AES-256-GCM Encryption** - Military-grade data protection
- **🔑 Password Manager** - Secure vault with autofill support
- **🤖 Offline AI** - Privacy advisor that runs entirely on-device
- **🛡️ Hardware-Backed Security** - Keys stored in Android KeyStore
- **📴 Offline-First** - No internet required for core functionality
- **✍️ Digital Signatures** - RSA-2048 for data integrity
- **📊 Visual Data Mapping** - Transparent storage visualization

---

## 🎯 Key Features

### 1. **Password Vault** 🔐

- Store passwords with AES-256-GCM encryption
- Organized by categories (Email, Social, Banking, etc.)
- Password health analyzer with breach detection
- Autofill support for apps and browsers
- Each entry individually encrypted and digitally signed

### 2. **Autofill Service** 📝

- Auto-fill passwords in any app or browser
- Auto-save new credentials
- Smart credential detection
- Works offline

### 3. **Privacy Vault** 🗄️

- Store sensitive documents and data
- Multiple categories (Personal, Financial, Medical, etc.)
- Hardware-backed encryption

### 4. **Offline AI Chat** 💬
- Privacy advisor running 100% on your device
- No data sent to cloud servers
- Real-time streaming responses

### 5. **Data Map** 📊
- Visual representation of your encrypted storage
- Security score based on encryption coverage
- Category breakdown and storage tracking

### 6. **Threat Monitoring** ⚡

- Real-time security alerts
- Data breach detection
- Security score tracking

---

## 🚀 Quick Start

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 26 (Android 8.0) or higher
- Physical Android device recommended

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Jaswanthnimmalla/SafeSphere.git
   cd SafeSphere
   ```

2. **Open in Android Studio**
    - File → Open → Select project directory
    - Wait for Gradle sync to complete

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or click Run in Android Studio

### Setup Autofill (Optional)

1. Open your device **Settings**
2. Search for "Autofill service" or navigate to:
    - **Settings → System → Languages & input → Advanced → Autofill service**
3. Select **SafeSphere**
4. Grant required permissions

---

## 🏗️ Architecture

### Technology Stack

- **Jetpack Compose** - Modern declarative UI
- **Android KeyStore** - Hardware-backed security
- **Kotlin Coroutines** - Async operations
- **Room Database** - Local encrypted storage
- **Material Design 3** - Modern UI components

### Project Structure
```
app/src/main/java/com/runanywhere/startup_hackathon20/
├── SafeSphereApplication.kt          # App initialization
├── SafeSphereMainActivity.kt         # Main UI entry
├── data/                             # Data models & repositories
├── security/                         # Encryption & key management
├── viewmodels/                       # State management
├── autofill/                         # Autofill service
└── ui/                               # UI screens & components
```

---

## 🔒 Security Features

### Encryption

- **Algorithm**: AES-256-GCM (Galois/Counter Mode)
- **Key Size**: 256 bits
- **Provider**: Android KeyStore (hardware-backed)

### Key Management

- Keys generated in Android KeyStore
- Never extractable from hardware
- Unique per device
- RSA-2048 for digital signatures

---

## 🤝 Contributing

Contributions are welcome! Areas for improvement:

- Additional password categories
- Enhanced biometric authentication
- Backup/restore functionality
- Tablet UI optimization

---

## 📄 License

This project is licensed under the MIT License.

---

**Built with ❤️ and 🔐 for privacy advocates.**

*"The best security is the kind users don't have to think about."*
