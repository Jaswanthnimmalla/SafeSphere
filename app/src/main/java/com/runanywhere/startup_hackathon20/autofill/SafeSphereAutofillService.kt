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
     * Called when the service is connected - CRITICAL for autofill to work!
     */
    override fun onConnected() {
        super.onConnected()
        Log.d(TAG, "✅ SafeSphere Autofill Service CONNECTED - Ready to autofill!")
        Log.d(TAG, "   Service is now active and listening for fill requests")
    }

    /**
     * Called when the service is disconnected
     */
    override fun onDisconnected() {
        super.onDisconnected()
        Log.d(TAG, "❌ SafeSphere Autofill Service DISCONNECTED")
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
                        Log.d(TAG, "📦 Total credentials in vault: ${allPasswords.size}")

                        // Log all credentials for debugging
                        if (allPasswords.isEmpty()) {
                            Log.d(TAG, "⚠️ WARNING: No credentials found in vault!")
                        } else {
                            Log.d(TAG, "📋 Credentials in vault:")
                            allPasswords.forEachIndexed { index, pass ->
                                Log.d(
                                    TAG,
                                    "   [$index] Service: '${pass.service}', URL: '${pass.url}', Username: '${pass.username}'"
                                )
                            }
                        }

                        val filtered = allPasswords.filter { password ->
                            try {
                                matchesCredential(password, searchQuery, url, packageName)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error matching credential", e)
                                false
                            }
                        }

                        filtered
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching saved passwords", e)
                        emptyList()
                    }

                    Log.d(TAG, "💾 Found ${savedPasswords.size} saved credentials")

                    // CRITICAL FIX: Even if no matches, we should show autofill to allow saving
                    // If no perfect matches found, show ALL credentials as fallback
                    val credentialsToShow = if (savedPasswords.isNotEmpty()) {
                        Log.d(TAG, "✅ Using ${savedPasswords.size} matched credentials")
                        savedPasswords
                    } else {
                        // Fallback: Get ALL credentials from vault
                        val allPasswords = try {
                            repository.passwords.first()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching all passwords", e)
                            emptyList()
                        }
                        
                        if (allPasswords.isNotEmpty()) {
                            Log.d(TAG, "⚠️ No perfect matches - showing ALL ${allPasswords.size} credentials as fallback")
                        } else {
                            Log.d(TAG, "ℹ️ No credentials in vault yet")
                        }
                        
                        allPasswords
                    }

                    // Build autofill response with error handling
                    val response = try {
                        buildFillResponse(
                            loginFields = loginFields,
                            savedPasswords = credentialsToShow,
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
        // Create beautiful presentation view
        val presentation = createDatasetPresentation(password)

        // For Android 11+ (API 30+), also create inline presentation
        val datasetBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use the newer constructor with inline presentation
            try {
                val inlinePresentation = createInlinePresentation(password)
                Dataset.Builder(presentation)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating inline presentation, falling back to basic dataset", e)
                Dataset.Builder(presentation)
            }
        } else {
            Dataset.Builder(presentation)
        }

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
     * Create inline presentation for Android 11+ (shown in keyboard autofill UI)
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun createInlinePresentation(password: PasswordVaultEntry): android.service.autofill.InlinePresentation? {
        return try {
            // For Android 11+, we can create inline presentations
            // This is shown directly in the keyboard
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // InlinePresentation requires InlineSuggestionUi which is complex
                // For now, returning null to use the basic presentation
                // This can be enhanced later with proper inline UI
                null
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating inline presentation", e)
            null
        }
    }

    /**
     * Create beautiful presentation for autofill suggestion
     */
    private fun createDatasetPresentation(password: PasswordVaultEntry): RemoteViews {
        return try {
            val presentation =
                RemoteViews(applicationContext.packageName, android.R.layout.simple_list_item_2)

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
                val fallback =
                    RemoteViews(applicationContext.packageName, android.R.layout.simple_list_item_1)
                fallback.setTextViewText(android.R.id.text1, "SafeSphere - Tap to fill")
                fallback
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Even fallback presentation failed!", fallbackError)
                // Last resort - but this should never happen
                RemoteViews(applicationContext.packageName, android.R.layout.simple_list_item_1)
            }
        }
    }

    /**
     * Create header for autofill dropdown
     */
    private fun createHeaderPresentation(count: Int): RemoteViews? {
        return try {
            val header =
                RemoteViews(applicationContext.packageName, android.R.layout.simple_list_item_1)
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

        Log.d(TAG, "   🔍 Checking password: ${password.service}")
        Log.d(TAG, "      Saved URL: '${password.url}'")
        Log.d(TAG, "      Search Query: '$query'")
        Log.d(TAG, "      Current URL: '$url'")
        Log.d(TAG, "      Package: '$packageName'")

        // ==================== STRATEGY 1: EXACT DOMAIN MATCH ====================
        // This is the most reliable for browser-based credentials
        if (url != null && password.url.isNotBlank()) {
            val currentDomain = extractDomain(url)
            val savedDomain = extractDomain(password.url)

            Log.d(TAG, "      [Domain Match] Comparing: '$currentDomain' vs '$savedDomain'")

            if (currentDomain.isNotBlank() && savedDomain.isNotBlank()) {
                // Exact match
                if (currentDomain.equals(savedDomain, ignoreCase = true)) {
                    Log.d(TAG, "   ✅ MATCHED by exact domain: $currentDomain")
                    return true
                }

                // Subdomain match (e.g., mobile.twitter.com matches twitter.com)
                if (currentDomain.endsWith(".$savedDomain") || savedDomain.endsWith(".$currentDomain")) {
                    Log.d(TAG, "   ✅ MATCHED by subdomain")
                    return true
                }

                // Remove TLD and compare base names
                val currentBase = currentDomain.substringBeforeLast(".")
                val savedBase = savedDomain.substringBeforeLast(".")

                if (currentBase.isNotBlank() && savedBase.isNotBlank() &&
                    currentBase.equals(savedBase, ignoreCase = true)
                ) {
                    Log.d(TAG, "   ✅ MATCHED by base domain name: $currentBase")
                    return true
                }

                // Special case: twitter.com ↔ x.com
                if ((currentDomain.contains("twitter") && savedDomain.contains("x.com")) ||
                    (currentDomain.contains("x.com") && savedDomain.contains("twitter"))
                ) {
                    Log.d(TAG, "   ✅ MATCHED by Twitter/X alias")
                    return true
                }
            }
        }

        // ==================== STRATEGY 2: SERVICE NAME MATCH ====================
        // Match by service name (exact or partial)
        if (password.service.lowercase().contains(query)) {
            Log.d(TAG, "   ✅ MATCHED by service contains query: ${password.service}")
            return true
        }

        if (query.contains(password.service.lowercase())) {
            Log.d(TAG, "   ✅ MATCHED by query contains service: ${password.service}")
            return true
        }

        // Word-by-word matching
        val serviceWords = password.service.lowercase().split(" ", "-", "_", ".")
        val queryWords = query.split(" ", "-", "_", ".")

        for (serviceWord in serviceWords) {
            for (queryWord in queryWords) {
                if (serviceWord.length > 2 && queryWord.length > 2) {
                    if (serviceWord.contains(queryWord) || queryWord.contains(serviceWord)) {
                        Log.d(
                            TAG,
                            "   ✅ MATCHED by service/query word: '$serviceWord' ~ '$queryWord'"
                        )
                        return true
                    }
                }
            }
        }

        // ==================== STRATEGY 3: URL FUZZY MATCH ====================
        // More aggressive URL matching for cases where domain extraction fails
        if (url != null && password.url.isNotBlank()) {
            val normalizedCurrentUrl = normalizeUrl(url)
            val normalizedSavedUrl = normalizeUrl(password.url)

            Log.d(
                TAG,
                "      [URL Fuzzy] Comparing: '$normalizedCurrentUrl' vs '$normalizedSavedUrl'"
            )

            // Direct substring match
            if (normalizedCurrentUrl.contains(normalizedSavedUrl) ||
                normalizedSavedUrl.contains(normalizedCurrentUrl)
            ) {
                Log.d(TAG, "   ✅ MATCHED by URL substring")
                return true
            }

            // Extract all meaningful words from URLs
            val currentUrlWords = extractMeaningfulWords(normalizedCurrentUrl)
            val savedUrlWords = extractMeaningfulWords(normalizedSavedUrl)

            // Check if any significant word matches
            for (currentWord in currentUrlWords) {
                for (savedWord in savedUrlWords) {
                    if (currentWord.length > 3 && savedWord.length > 3) {
                        if (currentWord == savedWord ||
                            currentWord.contains(savedWord) ||
                            savedWord.contains(currentWord)
                        ) {
                            Log.d(TAG, "   ✅ MATCHED by URL word: '$currentWord' ~ '$savedWord'")
                            return true
                        }
                    }
                }
            }
        }

        // ==================== STRATEGY 4: PACKAGE NAME MATCH ====================
        // Match by package name - useful for native apps
        val packageParts = packageName.lowercase().split(".")

        // Check if saved URL or service contains any significant package part
        for (part in packageParts) {
            if (part.length > 3 && part !in listOf(
                    "com", "org", "app", "net", "www", "mobile", "android", "io", "co"
                )
            ) {

                // Check in service name
                if (password.service.lowercase().contains(part)) {
                    Log.d(TAG, "   ✅ MATCHED by package part in service: '$part'")
                    return true
                }

                // Check in URL
                if (password.url.lowercase().contains(part)) {
                    Log.d(TAG, "   ✅ MATCHED by package part in URL: '$part'")
                    return true
                }

                // Check if package part is similar to service words
                for (serviceWord in serviceWords) {
                    if (serviceWord.length > 3) {
                        // Calculate similarity
                        if (calculateSimilarity(part, serviceWord) > 0.7) {
                            Log.d(
                                TAG,
                                "   ✅ MATCHED by package/service similarity: '$part' ~ '$serviceWord'"
                            )
                            return true
                        }
                    }
                }
            }
        }

        // ==================== STRATEGY 5: PACKAGE IN URL ====================
        // Check if package name is stored in URL field (common for native app saves)
        if (password.url.contains(packageName, ignoreCase = true)) {
            Log.d(TAG, "   ✅ MATCHED by full package in URL")
            return true
        }

        // Reverse: check if saved URL is the package
        if (packageName.contains(password.url, ignoreCase = true) && password.url.length > 5) {
            Log.d(TAG, "   ✅ MATCHED by URL is package substring")
            return true
        }

        // ==================== STRATEGY 6: SERVICE NAME IN URL/PACKAGE ====================
        // Check if service name appears in current URL or package
        if (password.service.length > 3) {
            val serviceClean = password.service.lowercase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")

            val urlClean = (url ?: "").lowercase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace(".", "")

            val packageClean = packageName.lowercase()
                .replace(".", "")

            if (serviceClean.length > 3) {
                if (urlClean.contains(serviceClean) || serviceClean.contains(urlClean.takeIf { it.length > 3 }
                        ?: "")) {
                    Log.d(TAG, "   ✅ MATCHED by service in URL (cleaned)")
                    return true
                }

                if (packageClean.contains(serviceClean) || serviceClean.contains(packageClean.takeIf { it.length > 3 }
                        ?: "")) {
                    Log.d(TAG, "   ✅ MATCHED by service in package (cleaned)")
                    return true
                }
            }
        }

        Log.d(TAG, "   ❌ No match found")
        return false
    }

    /**
     * Normalize URL for comparison - remove protocol, www, trailing slashes, etc.
     */
    private fun normalizeUrl(url: String): String {
        return url.lowercase()
            .trim()
            .replace("https://", "")
            .replace("http://", "")
            .replace("www.", "")
            .replace("www", "") // Remove www even without dot
            .replace(Regex("/.*$"), "") // Remove everything after first slash
            .replace(Regex(":\\d+"), "") // Remove port
            .replace(Regex("[?#].*$"), "") // Remove query and fragment
            .trim()
    }

    /**
     * Extract meaningful words from URL or package name
     */
    private fun extractMeaningfulWords(text: String): List<String> {
        val stopWords = setOf(
            "com",
            "org",
            "www",
            "app",
            "net",
            "co",
            "io",
            "mobile",
            "android",
            "https",
            "http"
        )

        return text.lowercase()
            .split(".", "/", "-", "_", ":")
            .filter { it.length > 2 && it !in stopWords }
            .distinct()
    }

    /**
     * Calculate string similarity (Levenshtein-based)
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1

        // Simple substring check
        if (longer.contains(shorter)) {
            return shorter.length.toDouble() / longer.length.toDouble()
        }

        // Count matching characters
        var matches = 0
        for (c in shorter) {
            if (longer.contains(c)) matches++
        }

        return matches.toDouble() / longer.length.toDouble()
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
                .trim()
                .replace("https://", "")
                .replace("http://", "")
                .lowercase()

            // Remove www at any position early - multiple variations
            domain = domain
                .replace("www.", "")
                .replace("www", "")
                .trim()
            
            // Remove path, query, fragment, port
            domain = domain
                .substringBefore("/")
                .substringBefore(":")
                .substringBefore("?")
                .substringBefore("#")
                .trim()

            // Handle package names (e.g., com.twitter.android)
            if (domain.contains(".") && !domain.contains(" ")) {
                val parts = domain.split(".")
                
                // If it looks like a package name (e.g., com.twitter.android)
                // Return the base domain instead
                if (parts.size >= 3 && parts[0] in listOf("com", "org", "net", "app", "io", "co")) {
                    // For reverse domain notation (package names), extract meaningful part
                    // com.twitter.android -> twitter
                    val meaningfulPart = parts[1]
                    if (meaningfulPart.length > 2 && meaningfulPart !in listOf(
                            "app",
                            "mobile",
                            "www"
                        )
                    ) {
                        Log.d(
                            TAG,
                            "      [extractDomain] Package format detected: '$url' -> '$meaningfulPart'"
                        )
                        return meaningfulPart
                    }
                }

                // For normal domains (e.g., mobile.twitter.com)
                // Return base domain (last 2 parts)
                if (parts.size >= 2) {
                    // Handle cases like co.uk, com.au, etc.
                    val lastPart = parts.last()
                    val secondLastPart = parts[parts.size - 2]

                    // Check if it's a two-part TLD (co.uk, com.au, etc.)
                    if (lastPart.length == 2 && secondLastPart in listOf(
                            "co",
                            "com",
                            "gov",
                            "org",
                            "net",
                            "ac",
                            "edu"
                        )
                    ) {
                        // Take last 3 parts (e.g., bbc.co.uk)
                        if (parts.size >= 3) {
                            val baseDomain = parts.takeLast(3).joinToString(".")
                            Log.d(
                                TAG,
                                "      [extractDomain] Two-part TLD detected: '$url' -> '$baseDomain'"
                            )
                            return baseDomain
                        }
                    }

                    // Normal case: take last 2 parts (domain + TLD)
                    val baseDomain = parts.takeLast(2).joinToString(".")
                    Log.d(TAG, "      [extractDomain] Normal domain: '$url' -> '$baseDomain'")
                    return baseDomain
                }
            }

            Log.d(TAG, "      [extractDomain] Using as-is: '$url' -> '$domain'")
            domain
        } catch (e: Exception) {
            Log.e(TAG, "      [extractDomain] Error extracting domain from: $url", e)
            // Even in error, try to clean it up
            url.lowercase()
                .trim()
                .replace("https://", "")
                .replace("http://", "")
                .replace("www.", "")
                .replace("www", "")
                .substringBefore("/")
                .substringBefore(":")
                .trim()
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
