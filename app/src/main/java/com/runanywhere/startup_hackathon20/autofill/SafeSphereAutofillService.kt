package com.runanywhere.startup_hackathon20.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.content.IntentSender
import android.graphics.BlendMode
import android.graphics.drawable.Icon
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.*
import android.util.Log
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.runanywhere.startup_hackathon20.R
import com.runanywhere.startup_hackathon20.data.PasswordCategory
import com.runanywhere.startup_hackathon20.data.PasswordVaultEntry
import com.runanywhere.startup_hackathon20.data.PasswordVaultRepository
import com.runanywhere.startup_hackathon20.security.SecurityManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * SafeSphere Autofill Service - Complete Password Manager Replacement
 * 
 * Features:
 * - 🔍 Smart detection of login forms in ANY app or website
 * - 💾 Auto-save credentials with "Save to SafeSphere?" prompt
 * - 🔐 Auto-fill passwords from SafeSphere vault
 * - 🌐 Works in browsers (Chrome, Firefox, etc.) and native apps
 * - 📱 Beautiful UI matching SafeSphere design
 * - 🛡️ 100% local - no data sent to cloud
 * 
 * Requires Android 8.0+ (API 26+)
 */
@RequiresApi(Build.VERSION_CODES.O)
class SafeSphereAutofillService : AutofillService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repository: PasswordVaultRepository

    companion object {
        private const val TAG = "SafeSphereAutofill"
        
        // Intent extras for authentication
        const val EXTRA_USERNAME = "username"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SERVICE = "service"
        const val EXTRA_URL = "url"
        const val EXTRA_PACKAGE = "package"
    }

    override fun onCreate() {
        super.onCreate()
        repository = PasswordVaultRepository.getInstance(applicationContext)
        Log.d(TAG, "🔐 SafeSphere Autofill Service initialized")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    /**
     * Called when user focuses on a login form
     * We provide autofill suggestions here
     */
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        // EMERGENCY CRASH PREVENTION - Wrap EVERYTHING in try-catch
        try {
            Log.d(TAG, "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📝 FILL REQUEST RECEIVED")

            // Use IO dispatcher for stability and wrap everything in try-catch
            scope.launch(Dispatchers.IO) {
                try {
                    val context = request.fillContexts?.lastOrNull()
                    if (context == null) {
                        Log.d(TAG, "❌ No context available")
                        withContext(Dispatchers.Main) {
                            try {
                                callback.onSuccess(null)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error calling callback.onSuccess(null)", e)
                            }
                        }
                        return@launch
                    }

                    val structure = context.structure
                    val packageName = structure?.activityComponent?.packageName
                    if (packageName == null) {
                        Log.d(TAG, "❌ No package name available")
                        withContext(Dispatchers.Main) {
                            try {
                                callback.onSuccess(null)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error calling callback.onSuccess(null)", e)
                            }
                        }
                        return@launch
                    }

                    val appName = try {
                        getAppName(packageName)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting app name", e)
                        packageName
                    }

                    Log.d(TAG, "📱 App: $appName")
                    Log.d(TAG, "📦 Package: $packageName")

                    // Parse the view structure to find login fields
                    val parser = AssistStructureParser(structure)
                    val loginFields = try {
                        parser.findLoginFields()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing login fields", e)
                        null
                    }

                    if (loginFields == null) {
                        Log.d(TAG, "❌ No login fields detected")
                        withContext(Dispatchers.Main) {
                            try {
                                callback.onSuccess(null)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error calling callback.onSuccess(null)", e)
                            }
                        }
                        return@launch
                    }

                    Log.d(TAG, "✅ Login fields detected:")
                    Log.d(TAG, "   👤 Username field: ${loginFields.usernameHint}")
                    Log.d(TAG, "   🔑 Password field: ${loginFields.passwordHint}")

                    // Determine search query
                    val url = try {
                        parser.extractUrl()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error extracting URL", e)
                        null
                    }

                    val searchQuery = when {
                        url != null && isBrowserApp(packageName) -> {
                            extractDomain(url).also {
                                Log.d(TAG, "🌐 Browser detected - URL: $url")
                                Log.d(TAG, "🔍 Searching by domain: $it")
                            }
                        }
                        else -> {
                            appName.also {
                                Log.d(TAG, "📱 Native app - searching by: $it")
                            }
                        }
                    }

                    // Search for saved credentials with error handling
                    val savedPasswords = try {
                        val allPasswords = repository.passwords.first()
                        allPasswords.filter { password ->
                            try {
                                matchesCredential(password, searchQuery, url, packageName)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error matching credential", e)
                                false
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching saved passwords", e)
                        emptyList()
                    }

                    Log.d(TAG, "💾 Found ${savedPasswords.size} saved credentials")

                    // Build autofill response with error handling
                    val response = try {
                        buildFillResponse(
                            loginFields = loginFields,
                            savedPasswords = savedPasswords,
                            appName = appName,
                            url = url ?: packageName,
                            packageName = packageName
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error building fill response", e)
                        null
                    }

                    withContext(Dispatchers.Main) {
                        try {
                            callback.onSuccess(response)
                            Log.d(TAG, "✅ Fill response sent successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error calling callback.onSuccess", e)
                        }
                    }
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Fill request failed with exception", e)
                    withContext(Dispatchers.Main) {
                        try {
                            callback.onFailure(e.message ?: "Unknown error")
                        } catch (callbackError: Exception) {
                            Log.e(TAG, "Error calling callback.onFailure", callbackError)
                        }
                    }
                }
            }
        } catch (topLevelError: Exception) {
            // ABSOLUTE LAST RESORT - prevent complete crash
            Log.e(TAG, "❌❌❌ CRITICAL: Top-level exception in onFillRequest", topLevelError)
            try {
                callback.onSuccess(null)
            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ CRITICAL: Cannot even call callback", e)
            }
        }
    }

    /**
     * Called when user submits credentials (after login)
     * We save the credentials here
     */
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        Log.d(TAG, "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "💾 SAVE REQUEST RECEIVED")

        scope.launch(Dispatchers.IO) {
            try {
                val context = request.fillContexts.lastOrNull()
                if (context == null) {
                    Log.d(TAG, "❌ No context available")
                    withContext(Dispatchers.Main) {
                        callback.onSuccess()
                    }
                    return@launch
                }

                val structure = context.structure
                val packageName = structure.activityComponent.packageName
                val appName = getAppName(packageName)
                
                Log.d(TAG, "📱 App: $appName")
                Log.d(TAG, "📦 Package: $packageName")

                // Parse structure to find login fields
                val parser = AssistStructureParser(structure)
                val loginFields = parser.findLoginFields()

                if (loginFields == null) {
                    Log.d(TAG, "❌ No login fields found")
                    withContext(Dispatchers.Main) {
                        callback.onSuccess()
                    }
                    return@launch
                }

                Log.d(TAG, "✅ Login fields identified:")
                Log.d(TAG, "   👤 Username field: ${loginFields.usernameHint}")
                Log.d(TAG, "   🔑 Password field: ${loginFields.passwordHint}")

                // Extract credentials from the submitted form
                val credentials = extractCredentialsFromDatasets(request, loginFields)
                
                if (credentials == null) {
                    Log.d(TAG, "❌ Could not extract credentials - skipping save")
                    withContext(Dispatchers.Main) {
                        callback.onSuccess()
                    }
                    return@launch
                }

                Log.d(TAG, "✅ Credentials extracted successfully:")
                Log.d(TAG, "   👤 Username: ${credentials.username}")
                Log.d(TAG, "   🔑 Password: ${credentials.password.take(3)}${"*".repeat(credentials.password.length - 3)}")

                val url = parser.extractUrl()
                Log.d(TAG, "🌐 Extracted URL: ${url ?: "none"}")

                val category = try {
                    detectCategory(packageName)
                } catch (e: Exception) {
                    Log.e(TAG, "Error detecting category, using default", e)
                    PasswordCategory.OTHER
                }
                Log.d(TAG, "📁 Category: $category")

                // Check if already saved
                val existingPasswords = try {
                    repository.passwords.first()
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching existing passwords", e)
                    emptyList()
                }
                
                val alreadyExists = existingPasswords.any {
                    it.service.equals(appName, ignoreCase = true) && 
                    it.username.equals(credentials.username, ignoreCase = true)
                }

                if (alreadyExists) {
                    Log.d(TAG, "ℹ️ Password already exists - updating existing entry")
                    // Find and update existing
                    val existing = existingPasswords.find {
                        it.service.equals(appName, ignoreCase = true) && 
                        it.username.equals(credentials.username, ignoreCase = true)
                    }
                    
                    if (existing != null) {
                        try {
                            repository.updatePassword(
                                id = existing.id,
                                service = appName,
                                username = credentials.username,
                                password = credentials.password,
                                url = url ?: packageName,
                                notes = "Auto-saved by SafeSphere",
                                category = category
                            )
                            Log.d(TAG, "✅ Password UPDATED successfully in SafeSphere vault!")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed to update password", e)
                        }
                    }
                } else {
                    // Save new password
                    Log.d(TAG, "💾 Saving NEW password to SafeSphere vault...")
                    try {
                        repository.savePassword(
                            service = appName,
                            username = credentials.username,
                            password = credentials.password,
                            url = url ?: packageName,
                            category = category,
                            notes = "Auto-saved by SafeSphere"
                        )
                        Log.d(TAG, "✅ Password SAVED successfully to SafeSphere vault!")
                        Log.d(TAG, "   📝 Service: $appName")
                        Log.d(TAG, "   👤 Username: ${credentials.username}")
                        Log.d(TAG, "   🌐 URL: ${url ?: packageName}")
                        Log.d(TAG, "   📁 Category: $category")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to save password", e)
                        Log.e(TAG, "   Error details: ${e.message}")
                        e.printStackTrace()
                    }
                }

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

                withContext(Dispatchers.Main) {
                    callback.onSuccess()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Save request failed with exception", e)
                Log.e(TAG, "   Exception type: ${e.javaClass.simpleName}")
                Log.e(TAG, "   Message: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback.onSuccess() // Don't fail the save
                }
            }
        }
    }

    /**
     * Build the autofill response with datasets and save info
     */
    private fun buildFillResponse(
        loginFields: LoginFields,
        savedPasswords: List<PasswordVaultEntry>,
        appName: String,
        url: String,
        packageName: String
    ): FillResponse {
        val responseBuilder = FillResponse.Builder()

        // Add datasets for each saved password (autofill suggestions)
        savedPasswords.forEach { password ->
            val dataset = createDataset(loginFields, password)
            responseBuilder.addDataset(dataset)
        }

        // ALWAYS add SaveInfo so credentials can be saved
        val saveInfoBuilder = SaveInfo.Builder(
            SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
            arrayOf(loginFields.usernameId, loginFields.passwordId)
        )

        // Add optional fields if present
        loginFields.emailId?.let {
            saveInfoBuilder.setOptionalIds(arrayOf(it))
        }

        // Custom save prompt description
        if (savedPasswords.isEmpty()) {
            saveInfoBuilder.setDescription("Save to SafeSphere?")
        } else {
            saveInfoBuilder.setDescription("Update in SafeSphere?")
        }

        responseBuilder.setSaveInfo(saveInfoBuilder.build())

        // Add header (shown at top of autofill dropdown) - API 28+
        if (savedPasswords.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val headerPresentation = createHeaderPresentation(savedPasswords.size)
            if (headerPresentation != null) {
                responseBuilder.setHeader(headerPresentation)
            }
        }

        return responseBuilder.build()
    }

    /**
     * Create a dataset (autofill suggestion) for one password
     */
    private fun createDataset(
        loginFields: LoginFields,
        password: PasswordVaultEntry
    ): Dataset {
        val datasetBuilder = Dataset.Builder()

        // Create beautiful presentation view
        val presentation = createDatasetPresentation(password)

        // Decrypt password
        val decryptedPassword = try {
            SecurityManager.decrypt(password.encryptedPassword)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt password", e)
            ""
        }

        // Set values for username field
        datasetBuilder.setValue(
            loginFields.usernameId,
            AutofillValue.forText(password.username),
            presentation
        )

        // Set values for password field
        datasetBuilder.setValue(
            loginFields.passwordId,
            AutofillValue.forText(decryptedPassword),
            presentation
        )

        // If there's an email field, fill it too
        loginFields.emailId?.let { emailId ->
            if (password.username.contains("@")) {
                datasetBuilder.setValue(
                    emailId,
                    AutofillValue.forText(password.username),
                    presentation
                )
            }
        }

        return datasetBuilder.build()
    }

    /**
     * Create beautiful presentation for autofill suggestion
     */
    private fun createDatasetPresentation(password: PasswordVaultEntry): RemoteViews {
        return try {
            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_2)

            try {
                // Main text: service name (simple text only)
                val serviceName = password.service?.takeIf { it.isNotBlank() } ?: "Password"
                presentation.setTextViewText(android.R.id.text1, serviceName)

                // Sub text: username (simple text only)
                val username = password.username?.takeIf { it.isNotBlank() } ?: "Saved credential"
                presentation.setTextViewText(android.R.id.text2, username)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting text in presentation", e)
                // Ultra-safe fallback
                presentation.setTextViewText(android.R.id.text1, "SafeSphere")
                presentation.setTextViewText(android.R.id.text2, "Tap to fill")
            }

            presentation
        } catch (e: Exception) {
            Log.e(TAG, "Error creating RemoteViews presentation", e)
            // Absolute fallback - create most basic presentation possible
            try {
                val fallback = RemoteViews(packageName, android.R.layout.simple_list_item_1)
                fallback.setTextViewText(android.R.id.text1, "SafeSphere - Tap to fill")
                fallback
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Even fallback presentation failed!", fallbackError)
                // Last resort - but this should never happen
                RemoteViews(packageName, android.R.layout.simple_list_item_1)
            }
        }
    }

    /**
     * Create header for autofill dropdown
     */
    private fun createHeaderPresentation(count: Int): RemoteViews? {
        return try {
            val header = RemoteViews(packageName, android.R.layout.simple_list_item_1)
            try {
                header.setTextViewText(
                    android.R.id.text1,
                    "SafeSphere ($count saved)"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error setting header text", e)
                header.setTextViewText(android.R.id.text1, "SafeSphere")
            }
            header
        } catch (e: Exception) {
            Log.e(TAG, "Error creating header presentation", e)
            // Return null instead of crashing - header is optional
            null
        }
    }

    /**
     * Extract credentials from the submitted form
     */
    private fun extractCredentialsFromDatasets(
        request: SaveRequest,
        loginFields: LoginFields
    ): Credentials? {
        Log.d(TAG, "🔍 Extracting credentials from save request...")

        var username: String? = null
        var password: String? = null

        // Try to get values from the structure
        for (context in request.fillContexts) {
            val structure = context.structure

            Log.d(TAG, "   📋 Checking context with ${structure.windowNodeCount} windows")

            for (i in 0 until structure.windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                val credentials = extractFromNode(windowNode.rootViewNode, loginFields)
                if (credentials != null) {
                    Log.d(TAG, "   ✅ Found credentials from node!")
                    return credentials
                }
            }
        }

        // If we didn't find credentials from the structure, 
        // they might be in the client state or we need to search harder
        Log.d(TAG, "   ⚠️ Could not extract credentials from structure directly")
        Log.d(TAG, "   🔄 Trying alternative extraction methods...")

        // Try one more time with a more aggressive search
        for (context in request.fillContexts) {
            val structure = context.structure

            for (i in 0 until structure.windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                val result = searchAllNodesForCredentials(windowNode.rootViewNode, loginFields)
                if (result.first != null && result.second != null) {
                    Log.d(TAG, "   ✅ Found credentials from aggressive search!")
                    return Credentials(result.first!!, result.second!!)
                }
            }
        }

        Log.d(TAG, "   ❌ Failed to extract credentials from all methods")
        return null
    }

    /**
     * Search all nodes recursively for any text values
     */
    private fun searchAllNodesForCredentials(
        node: AssistStructure.ViewNode,
        loginFields: LoginFields
    ): Pair<String?, String?> {
        var username: String? = null
        var password: String? = null

        try {
            val autofillId = node.autofillId
            val autofillValue = node.autofillValue

            // Try autofill value first
            if (autofillValue != null && autofillValue.isText) {
                val text = autofillValue.textValue.toString()

                if (text.isNotBlank()) {
                    when (autofillId) {
                        loginFields.usernameId -> {
                            username = text
                            Log.d(
                                TAG,
                                "      📧 Found username via autofillValue: ${text.take(3)}..."
                            )
                        }

                        loginFields.passwordId -> {
                            password = text
                            Log.d(
                                TAG,
                                "      🔑 Found password via autofillValue: ${text.take(2)}..."
                            )
                        }

                        loginFields.emailId -> {
                            if (username == null) {
                                username = text
                                Log.d(
                                    TAG,
                                    "      📧 Found email via autofillValue: ${text.take(3)}..."
                                )
                            }
                        }
                    }
                }
            }

            // Also try text property as fallback
            val textValue = node.text?.toString()
            if (textValue != null && textValue.isNotBlank()) {
                when (autofillId) {
                    loginFields.usernameId -> {
                        if (username == null) {
                            username = textValue
                            Log.d(TAG, "      📧 Found username via text: ${textValue.take(3)}...")
                        }
                    }

                    loginFields.passwordId -> {
                        if (password == null) {
                            password = textValue
                            Log.d(TAG, "      🔑 Found password via text: ${textValue.take(2)}...")
                        }
                    }

                    loginFields.emailId -> {
                        if (username == null) {
                            username = textValue
                            Log.d(TAG, "      📧 Found email via text: ${textValue.take(3)}...")
                        }
                    }
                }
            }

            // Check children recursively
            for (i in 0 until node.childCount) {
                val childResult = searchAllNodesForCredentials(node.getChildAt(i), loginFields)
                if (username == null) username = childResult.first
                if (password == null) password = childResult.second

                // Early exit if we found both
                if (username != null && password != null) {
                    return Pair(username, password)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in searchAllNodesForCredentials", e)
        }

        return Pair(username, password)
    }

    /**
     * Recursively extract credentials from view nodes
     */
    private fun extractFromNode(
        node: AssistStructure.ViewNode,
        loginFields: LoginFields
    ): Credentials? {
        var username: String? = null
        var password: String? = null

        // Check current node
        val autofillId = node.autofillId
        val autofillValue = node.autofillValue

        if (autofillValue != null && autofillValue.isText) {
            val text = autofillValue.textValue.toString()
            
            when (autofillId) {
                loginFields.usernameId -> username = text
                loginFields.passwordId -> password = text
                loginFields.emailId -> if (username == null) username = text
            }
        }

        // Check children recursively
        for (i in 0 until node.childCount) {
            val childResult = extractFromNode(node.getChildAt(i), loginFields)
            if (childResult != null) {
                if (username == null) username = childResult.username
                if (password == null) password = childResult.password
            }
        }

        return if (username != null && password != null) {
            Credentials(username, password)
        } else {
            null
        }
    }

    /**
     * Check if credential matches search criteria
     */
    private fun matchesCredential(
        password: PasswordVaultEntry,
        searchQuery: String,
        url: String?,
        packageName: String
    ): Boolean {
        val query = searchQuery.lowercase()

        // Match by service name (exact or partial)
        if (password.service.lowercase().contains(query) || 
            query.contains(password.service.lowercase())) {
            Log.d(TAG, "   ✅ Matched by service name: ${password.service}")
            return true
        }

        // Match by URL domain (ignore paths, subdomains, protocols)
        if (url != null && password.url.isNotBlank()) {
            val currentDomain = extractDomain(url)
            val savedDomain = extractDomain(password.url)

            Log.d(TAG, "   🔍 Comparing domains: '$currentDomain' vs '$savedDomain'")

            if (currentDomain.isNotBlank() && savedDomain.isNotBlank()) {
                // Direct exact match
                if (currentDomain.equals(savedDomain, ignoreCase = true)) {
                    Log.d(TAG, "   ✅ Matched by exact domain: $currentDomain")
                    return true
                }

                // One contains the other (e.g., twitter.com and x.com are both Twitter)
                if (currentDomain.contains(savedDomain, ignoreCase = true) ||
                    savedDomain.contains(currentDomain, ignoreCase = true)
                ) {
                    Log.d(TAG, "   ✅ Matched by domain contains")
                    return true
                }

                // Special case: twitter.com and x.com are the same
                if ((currentDomain == "twitter.com" && savedDomain == "x.com") ||
                    (currentDomain == "x.com" && savedDomain == "twitter.com")
                ) {
                    Log.d(TAG, "   ✅ Matched by Twitter/X alias")
                    return true
                }

                // Match by just the site name part (before TLD)
                val currentName = currentDomain.substringBeforeLast(".")
                val savedName = savedDomain.substringBeforeLast(".")
                if (currentName.equals(savedName, ignoreCase = true) &&
                    currentName.isNotBlank()
                ) {
                    Log.d(TAG, "   ✅ Matched by domain name: $currentName")
                    return true
                }
            }
        }
        
        // Match by package name keywords
        val packageParts = packageName.split(".")
        val matched = packageParts.any { part ->
            if (part.length > 3) { // Ignore short parts like "com", "org", "app"
                password.service.lowercase().contains(part.lowercase()) ||
                        password.url.lowercase().contains(part.lowercase())
            } else {
                false
            }
        }

        if (matched) {
            Log.d(TAG, "   ✅ Matched by package name part")
        } else {
            Log.d(
                TAG,
                "   ❌ No match - Service: '${password.service}', URL: '${password.url}', Query: '$query', CurrentURL: '$url'"
            )
        }

        return matched
    }

    /**
     * Get app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            // Fallback: capitalize last part of package name
            packageName.split(".").lastOrNull()
                ?.replaceFirstChar { it.uppercase() }
                ?: packageName
        }
    }

    /**
     * Detect category from package name
     */
    private fun detectCategory(packageName: String): PasswordCategory {
        val pkg = packageName.lowercase()
        
        return when {
            // Email
            pkg.contains("gmail") || pkg.contains("mail") || 
            pkg.contains("outlook") || pkg.contains("yahoo") -> 
                PasswordCategory.EMAIL
            
            // Social Media
            pkg.contains("facebook") || pkg.contains("instagram") ||
            pkg.contains("twitter") || pkg.contains("linkedin") ||
            pkg.contains("whatsapp") || pkg.contains("telegram") ||
            pkg.contains("snapchat") || pkg.contains("tiktok") -> 
                PasswordCategory.SOCIAL
            
            // Banking & Finance
            pkg.contains("bank") || pkg.contains("paypal") ||
            pkg.contains("venmo") || pkg.contains("gpay") ||
            pkg.contains("paytm") || pkg.contains("phonepe") -> 
                PasswordCategory.BANKING
            
            // Shopping
            pkg.contains("amazon") || pkg.contains("ebay") ||
            pkg.contains("flipkart") || pkg.contains("shopping") -> 
                PasswordCategory.SHOPPING
            
            // Entertainment
            pkg.contains("netflix") || pkg.contains("spotify") ||
            pkg.contains("youtube") || pkg.contains("prime") ||
            pkg.contains("hulu") || pkg.contains("disney") -> 
                PasswordCategory.ENTERTAINMENT
            
            // Browsers
            pkg.contains("chrome") || pkg.contains("browser") ||
            pkg.contains("firefox") || pkg.contains("edge") ||
            pkg.contains("opera") || pkg.contains("brave") -> 
                PasswordCategory.WEB
            
            // Default to APP
            else -> PasswordCategory.APP
        }
    }

    /**
     * Check if package is a browser
     */
    private fun isBrowserApp(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg.contains("chrome") ||
                pkg.contains("browser") ||
                pkg.contains("firefox") ||
                pkg.contains("edge") ||
                pkg.contains("opera") ||
                pkg.contains("brave") ||
                pkg.contains("samsung.internet") ||
                pkg.contains("ucbrowser") ||
                pkg.contains("duckduckgo")
    }

    /**
     * Extract domain from URL
     */
    private fun extractDomain(url: String): String {
        return try {
            var domain = url
                .replace("https://", "")
                .replace("http://", "")
                .replace("www.", "") // Remove www at any position
                .substringBefore("/") // Remove path
                .substringBefore(":") // Remove port
                .substringBefore("?") // Remove query params
                .trim()

            // Remove www from start if present
            if (domain.startsWith("www.")) {
                domain = domain.substring(4)
            }

            // Get just the main domain (e.g., google.com from accounts.google.com)
            // For most cases, keep the full subdomain but also extract base
            val parts = domain.split(".")

            // Return the base domain (last 2 parts) for better matching
            // e.g., x.com from mobile.x.com or twitter.com from mobile.twitter.com
            if (parts.size > 2) {
                // Keep the last 2 parts (domain + TLD)
                domain = parts.takeLast(2).joinToString(".")
            }

            domain.lowercase()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting domain from: $url", e)
            url.lowercase()
        }
    }

    /**
     * Data classes
     */
    private data class LoginFields(
        val usernameId: AutofillId,
        val passwordId: AutofillId,
        val emailId: AutofillId? = null,
        val usernameHint: String = "",
        val passwordHint: String = ""
    )

    private data class Credentials(
        val username: String,
        val password: String
    )

    /**
     * AssistStructure Parser - Intelligently finds login fields
     */
    private class AssistStructureParser(private val structure: AssistStructure) {
        
        private var usernameField: AutofillId? = null
        private var passwordField: AutofillId? = null
        private var emailField: AutofillId? = null
        private var usernameHint: String = ""
        private var passwordHint: String = ""
        
        fun findLoginFields(): LoginFields? {
            // Parse all windows
            for (i in 0 until structure.windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                parseNode(windowNode.rootViewNode)
            }
            
            return if (usernameField != null && passwordField != null) {
                LoginFields(
                    usernameId = usernameField!!,
                    passwordId = passwordField!!,
                    emailId = emailField,
                    usernameHint = usernameHint,
                    passwordHint = passwordHint
                )
            } else {
                null
            }
        }
        
        fun extractUrl(): String? {
            for (i in 0 until structure.windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                val url = findUrlInNode(windowNode.rootViewNode)
                if (url != null) return url
            }
            return null
        }
        
        private fun parseNode(node: AssistStructure.ViewNode?) {
            if (node == null) return
            
            try {
                val autofillId = node.autofillId ?: return
                val hint = node.hint?.lowercase() ?: ""
                val idEntry = node.idEntry?.lowercase() ?: ""
                val text = node.text?.toString()?.lowercase() ?: ""
                val autofillHints = node.autofillHints ?: emptyArray()
                val inputType = node.inputType
                val className = node.className ?: ""
                
                // Detect password field
                val isPasswordField = 
                    autofillHints.any { it.contains("password") } ||
                    (inputType and 0x80) != 0 || // TYPE_TEXT_VARIATION_PASSWORD
                    (inputType and 0x90) != 0 || // TYPE_TEXT_VARIATION_WEB_PASSWORD
                    (inputType and 0xe0) != 0 || // TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    hint.contains("password") || hint.contains("pwd") ||
                    idEntry.contains("password") || idEntry.contains("pwd") ||
                    text.contains("password")
                
                if (isPasswordField && passwordField == null) {
                    passwordField = autofillId
                    passwordHint = hint.ifEmpty { idEntry.ifEmpty { "password" } }
                    Log.d(TAG, "   🔑 Password field found: $passwordHint")
                }
                
                // Detect email field
                val isEmailField =
                    autofillHints.any { it.contains("email") } ||
                    (inputType and 0x20) != 0 || // TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    hint.contains("email") || hint.contains("e-mail") ||
                    idEntry.contains("email") || idEntry.contains("e_mail") ||
                    text.contains("email")
                
                if (isEmailField && emailField == null && !isPasswordField) {
                    emailField = autofillId
                    Log.d(TAG, "   📧 Email field found: $hint")
                }
                
                // Detect username field
                val isUsernameField =
                    autofillHints.any { it.contains("username") || it.contains("user") } ||
                    hint.contains("username") || hint.contains("user") ||
                    hint.contains("login") || hint.contains("account") ||
                    idEntry.contains("username") || idEntry.contains("user") ||
                    idEntry.contains("login") || idEntry.contains("account") ||
                    text.contains("username") || text.contains("user")
                
                if ((isUsernameField || isEmailField) && usernameField == null && !isPasswordField) {
                    usernameField = autofillId
                    usernameHint = hint.ifEmpty { idEntry.ifEmpty { "username" } }
                    Log.d(TAG, "   👤 Username field found: $usernameHint")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing node", e)
            }
            
            // Recursively check children with error handling
            try {
                for (i in 0 until node.childCount) {
                    try {
                        parseNode(node.getChildAt(i))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing child node $i", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error iterating child nodes", e)
            }
        }
        
        private fun findUrlInNode(node: AssistStructure.ViewNode): String? {
            // Check webDomain (for WebView)
            node.webDomain?.let { return it }
            
            // Check text content for URLs
            val text = node.text?.toString() ?: ""
            if (text.startsWith("http://") || text.startsWith("https://")) {
                return text
            }
            
            // Check children
            for (i in 0 until node.childCount) {
                val childUrl = findUrlInNode(node.getChildAt(i))
                if (childUrl != null) return childUrl
            }
            
            return null
        }
    }
}
