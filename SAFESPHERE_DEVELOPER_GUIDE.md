# 🛠️ SafeSphere Developer Guide

Quick reference for developers working with SafeSphere.

---

## 📦 Module Overview

### Core Modules

| Module | Purpose | Key Classes |
|--------|---------|-------------|
| `security/` | Encryption & key management | `SecurityManager` |
| `data/` | Data models & repository | `PrivacyVaultRepository`, `PrivacyVaultItem` |
| `viewmodels/` | State & business logic | `SafeSphereViewModel` |
| `ui/` | UI components & screens | `SafeSphereTheme`, Components, Screens |

---

## 🔐 Security Implementation

### 1. Initialize Security

```kotlin
// In Application.onCreate()
SecurityManager.initialize(this)
```

### 2. Encrypt/Decrypt Data

```kotlin
// Encrypt
val plaintext = "Sensitive data"
val encrypted = SecurityManager.encrypt(plaintext)
// Returns: Base64 string with IV + ciphertext + auth tag

// Decrypt
val decrypted = SecurityManager.decrypt(encrypted)
// Returns: Original plaintext

// Error handling
try {
    val data = SecurityManager.decrypt(encryptedData)
} catch (e: SecurityException) {
    // Handle decryption failure
}
```

### 3. Sign & Verify

```kotlin
// Sign data
val signature = SecurityManager.sign(data)

// Verify signature
val isValid = SecurityManager.verify(data, signature)
if (isValid) {
    // Data is authentic
} else {
    // Data has been tampered with
}
```

### 4. Check Security Status

```kotlin
val status = SecurityManager.getSecurityStatus()
println("Initialized: ${status.initialized}")
println("AES Key: ${status.aesKeyPresent}")
println("RSA Key: ${status.rsaKeyPresent}")
println("Hardware: ${status.hardwareBacked}")
```

---

## 💾 Data Management

### 1. Get Repository Instance

```kotlin
val repository = PrivacyVaultRepository.getInstance(context)
```

### 2. Add Vault Item

```kotlin
viewModelScope.launch {
    val result = repository.addItem(
        title = "Credit Card",
        content = "4532-1234-5678-9010\nCVV: 123",
        category = VaultCategory.FINANCIAL
    )
    
    result.onSuccess { item ->
        println("Saved: ${item.id}")
    }.onFailure { error ->
        println("Error: ${error.message}")
    }
}
```

### 3. Retrieve & Decrypt Item

```kotlin
viewModelScope.launch {
    val result = repository.getDecryptedItem(itemId)
    
    result.onSuccess { decryptedItem ->
        // Use decrypted content
        println(decryptedItem.content)
    }.onFailure { error ->
        // Handle error
    }
}
```

### 4. Update Item

```kotlin
viewModelScope.launch {
    repository.updateItem(
        id = itemId,
        title = "Updated Title",
        content = "New content",
        category = VaultCategory.NOTES
    )
}
```

### 5. Delete Item

```kotlin
viewModelScope.launch {
    repository.deleteItem(itemId)
}
```

### 6. Get Statistics

```kotlin
val stats = repository.getStorageStats()
println("Total: ${stats.totalItems}")
println("Encrypted: ${stats.encryptedItems}")
println("Size: ${stats.totalSize} bytes")
println("Score: ${stats.securityScore}/100")
```

### 7. Observe Vault Changes

```kotlin
// In Composable or ViewModel
val vaultItems by repository.vaultItems.collectAsState()

// React to changes
LaunchedEffect(vaultItems) {
    println("Vault updated: ${vaultItems.size} items")
}
```

---

## 🤖 AI Integration

### 1. Register Models

```kotlin
// In Application class
suspend fun registerModels() {
    addModelFromURL(
        url = "https://huggingface.co/model.gguf",
        name = "My Privacy Model",
        type = "LLM"
    )
}
```

### 2. Download Model

```kotlin
viewModelScope.launch {
    RunAnywhere.downloadModel(modelId).collect { progress ->
        println("Progress: ${(progress * 100).toInt()}%")
    }
}
```

### 3. Load Model

```kotlin
viewModelScope.launch {
    val success = RunAnywhere.loadModel(modelId)
    if (success) {
        println("Model loaded")
    }
}
```

### 4. Generate Text

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    val prompt = "Explain encryption"
    
    RunAnywhere.generateStream(prompt).collect { token ->
        // Append token to response
        response += token
    }
}
```

### 5. List Available Models

```kotlin
val models = listAvailableModels()
models.forEach { model ->
    println("${model.name}: Downloaded=${model.isDownloaded}")
}
```

---

## 🎨 UI Components

### 1. Use Glass Card

```kotlin
@Composable
fun MyScreen() {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Content here")
        }
    }
}
```

### 2. Use Glass Button

```kotlin
@Composable
fun MyButtons() {
    // Primary button
    GlassButton(
        text = "Save",
        onClick = { /* action */ },
        primary = true
    )
    
    // Secondary button
    GlassButton(
        text = "Cancel",
        onClick = { /* action */ },
        primary = false
    )
    
    // Disabled button
    GlassButton(
        text = "Processing",
        onClick = {},
        enabled = false
    )
}
```

### 3. Use Header

```kotlin
@Composable
fun MyScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        SafeSphereHeader(
            title = "Screen Title",
            subtitle = "Subtitle text",
            onBackClick = { /* navigate back */ },
            onSettingsClick = { /* open settings */ },
            onActionClick = { /* action */ },
            actionIcon = "+"
        )
        
        // Screen content
    }
}
```

### 4. Show Dialog

```kotlin
@Composable
fun MyScreen() {
    var showDialog by remember { mutableStateOf(false) }
    
    if (showDialog) {
        AddVaultItemDialog(
            onDismiss = { showDialog = false },
            onSave = { title, content, category ->
                // Save item
                showDialog = false
            }
        )
    }
}
```

---

## 🔄 State Management

### 1. Create ViewModel

```kotlin
class MyViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun doAction() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            // Do work
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }
}

data class UiState(
    val loading: Boolean = false,
    val data: List<String> = emptyList()
)
```

### 2. Use ViewModel in Composable

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    if (uiState.loading) {
        CircularProgressIndicator()
    } else {
        LazyColumn {
            items(uiState.data) { item ->
                Text(item)
            }
        }
    }
}
```

### 3. Handle Side Effects

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val message by viewModel.uiMessage.collectAsState()
    
    message?.let { msg ->
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(msg)
        }
        
        LaunchedEffect(msg) {
            delay(3000)
            viewModel.clearMessage()
        }
    }
}
```

---

## 🎯 Navigation

### 1. Define Screens

```kotlin
enum class Screen {
    ONBOARDING,
    DASHBOARD,
    PRIVACY_VAULT,
    AI_CHAT,
    DATA_MAP,
    THREAT_SIMULATION,
    SETTINGS,
    MODELS
}
```

### 2. Navigate

```kotlin
// In ViewModel
fun navigateToScreen(screen: Screen) {
    _currentScreen.value = screen
}

// In Composable
viewModel.navigateToScreen(Screen.DASHBOARD)
```

### 3. Router

```kotlin
@Composable
fun SafeSphereApp(viewModel: SafeSphereViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    
    when (currentScreen) {
        Screen.DASHBOARD -> DashboardScreen(viewModel)
        Screen.PRIVACY_VAULT -> PrivacyVaultScreen(viewModel)
        Screen.AI_CHAT -> AIChatScreen(viewModel)
        // ... other screens
    }
}
```

---

## 📊 Data Models

### 1. Vault Item

```kotlin
data class PrivacyVaultItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val encryptedContent: String,
    val category: VaultCategory,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val signature: String = "",
    val isEncrypted: Boolean = true,
    val size: Int = 0
)
```

### 2. Decrypted Item

```kotlin
data class DecryptedVaultItem(
    val id: String,
    val title: String,
    val content: String,  // Plaintext
    val category: VaultCategory,
    val createdAt: Long,
    val modifiedAt: Long
)
```

### 3. Storage Statistics

```kotlin
data class StorageStats(
    val totalItems: Int,
    val encryptedItems: Int,
    val totalSize: Long,
    val categoryBreakdown: Map<VaultCategory, Int>,
    val securityScore: Int  // 0-100
)
```

### 4. Threat Event

```kotlin
data class ThreatEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: ThreatType,
    val severity: ThreatSeverity,
    val description: String,
    val mitigated: Boolean = false
)
```

---

## 🧪 Testing Utilities

### 1. Test Encryption

```kotlin
@Test
fun testEncryption() {
    val plaintext = "test data"
    val encrypted = SecurityManager.encrypt(plaintext)
    val decrypted = SecurityManager.decrypt(encrypted)
    
    assertEquals(plaintext, decrypted)
    assertNotEquals(plaintext, encrypted)
}
```

### 2. Test Signing

```kotlin
@Test
fun testSigning() {
    val data = "test data"
    val signature = SecurityManager.sign(data)
    val isValid = SecurityManager.verify(data, signature)
    
    assertTrue(isValid)
    
    val tamperedData = "modified data"
    val stillValid = SecurityManager.verify(tamperedData, signature)
    
    assertFalse(stillValid)
}
```

### 3. Test Repository

```kotlin
@Test
fun testVault() = runBlocking {
    val repo = PrivacyVaultRepository.getInstance(context)
    
    val result = repo.addItem(
        title = "Test",
        content = "Content",
        category = VaultCategory.NOTES
    )
    
    assertTrue(result.isSuccess)
    
    val item = result.getOrNull()!!
    assertEquals("Test", item.title)
    assertTrue(item.isEncrypted)
}
```

---

## 🎨 Theme Customization

### 1. Colors

```kotlin
object SafeSphereColors {
    val Primary = Color(0xFF2196F3)
    val Secondary = Color(0xFF00BCD4)
    val Accent = Color(0xFF9C27B0)
    
    val Background = Color(0xFF0A0E1A)
    val Surface = Color(0xFF1A1F2E)
    
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB0B3C1)
    
    val Success = Color(0xFF4CAF50)
    val Error = Color(0xFFF44336)
    val Warning = Color(0xFFFF9800)
}
```

### 2. Apply Theme

```kotlin
@Composable
fun MyApp() {
    SafeSphereTheme {
        // Your app content
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SafeSphereColors.Background
        ) {
            MainScreen()
        }
    }
}
```

---

## 🔍 Debugging

### 1. Enable Logging

```kotlin
companion object {
    private const val TAG = "SafeSphere"
}

Log.d(TAG, "Debug message")
Log.i(TAG, "Info message")
Log.w(TAG, "Warning message")
Log.e(TAG, "Error message", exception)
```

### 2. Check Security Status

```kotlin
fun debugSecurity() {
    val status = SecurityManager.getSecurityStatus()
    Log.d(TAG, """
        Security Status:
        - Initialized: ${status.initialized}
        - AES Key: ${status.aesKeyPresent}
        - RSA Key: ${status.rsaKeyPresent}
        - Hardware: ${status.hardwareBacked}
    """.trimIndent())
}
```

### 3. Inspect Vault

```kotlin
fun debugVault() {
    val stats = repository.getStorageStats()
    Log.d(TAG, """
        Vault Stats:
        - Total: ${stats.totalItems}
        - Encrypted: ${stats.encryptedItems}
        - Size: ${stats.totalSize} bytes
        - Score: ${stats.securityScore}/100
    """.trimIndent())
}
```

---

## 🚀 Performance Tips

### 1. Use Lazy Loading

```kotlin
// Load items on demand
LazyColumn {
    items(vaultItems) { item ->
        VaultItemCard(item)
    }
}
```

### 2. Cache Decrypted Data

```kotlin
// Cache in ViewModel
private val decryptedCache = mutableMapOf<String, String>()

fun getDecrypted(id: String): String {
    return decryptedCache.getOrPut(id) {
        SecurityManager.decrypt(encryptedData)
    }
}
```

### 3. Background Processing

```kotlin
// Heavy operations on IO dispatcher
viewModelScope.launch(Dispatchers.IO) {
    val encrypted = SecurityManager.encrypt(largeData)
    
    withContext(Dispatchers.Main) {
        // Update UI
    }
}
```

### 4. Batch Operations

```kotlin
// Encrypt multiple items efficiently
suspend fun encryptBatch(items: List<String>): List<String> {
    return withContext(Dispatchers.IO) {
        items.map { SecurityManager.encrypt(it) }
    }
}
```

---

## 🔧 Common Patterns

### 1. Result Handling

```kotlin
suspend fun saveItem(data: String): Result<Unit> {
    return try {
        val encrypted = SecurityManager.encrypt(data)
        repository.addItem("Title", encrypted, VaultCategory.NOTES)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Usage
saveItem(data).onSuccess {
    showMessage("Saved")
}.onFailure { error ->
    showError(error.message)
}
```

### 2. Flow Collection

```kotlin
// Collect in composable
val items by repository.vaultItems.collectAsState()

// Collect in ViewModel
init {
    viewModelScope.launch {
        repository.vaultItems.collect { items ->
            updateStats(items)
        }
    }
}
```

### 3. Error Boundaries

```kotlin
@Composable
fun SafeContent(content: @Composable () -> Unit) {
    var error by remember { mutableStateOf<Throwable?>(null) }
    
    if (error != null) {
        ErrorScreen(error!!) {
            error = null
        }
    } else {
        try {
            content()
        } catch (e: Exception) {
            error = e
        }
    }
}
```

---

## 📝 Code Style

### 1. Naming Conventions

```kotlin
// Classes: PascalCase
class SecurityManager

// Functions: camelCase
fun encryptData()

// Constants: UPPER_SNAKE_CASE
const val MAX_ITEMS = 1000

// Private: prefix with underscore
private val _items = MutableStateFlow<List<Item>>(emptyList())
```

### 2. Documentation

```kotlin
/**
 * Encrypts data using AES-256-GCM
 * 
 * @param plaintext The data to encrypt
 * @return Base64-encoded ciphertext with IV
 * @throws SecurityException if encryption fails
 */
fun encrypt(plaintext: String): String
```

### 3. Null Safety

```kotlin
// Use safe calls
val length = item?.content?.length

// Use elvis operator
val content = item?.content ?: "default"

// Use let for null checks
item?.let { encryptItem(it) }
```

---

## 🎓 Best Practices

1. **Always encrypt sensitive data** before storing
2. **Verify signatures** before decrypting
3. **Use hardware-backed keys** when available
4. **Handle errors gracefully** with proper user feedback
5. **Test encryption/decryption** thoroughly
6. **Keep keys in KeyStore** never in SharedPreferences
7. **Use coroutines** for async operations
8. **Dispose resources** properly (close streams, cancel jobs)
9. **Validate user input** before encryption
10. **Log carefully** never log sensitive data

---

## 🔗 Quick Links

- [SecurityManager.kt](app/src/main/java/com/runanywhere/startup_hackathon20/security/SecurityManager.kt)
- [PrivacyVaultRepository.kt](app/src/main/java/com/runanywhere/startup_hackathon20/data/PrivacyVaultRepository.kt)
- [SafeSphereViewModel.kt](app/src/main/java/com/runanywhere/startup_hackathon20/viewmodels/SafeSphereViewModel.kt)
- [SafeSphereTheme.kt](app/src/main/java/com/runanywhere/startup_hackathon20/ui/SafeSphereTheme.kt)

---

**Happy coding! 🔐**
