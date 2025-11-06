package com.runanywhere.startup_hackathon20.autofill

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.*
import android.util.Log
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.runanywhere.startup_hackathon20.R
import com.runanywhere.startup_hackathon20.data.PasswordCategory
import com.runanywhere.startup_hackathon20.data.PasswordVaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SafeSphere Autofill Service
 * 
 * Replaces Google Password Manager with local-only autofill
 * 
 * Features:
 * - Detects login forms in all apps and browsers
 * - Shows "Save to SafeSphere?" prompt after login
 * - Auto-fills credentials when user taps input fields
 * - 100% offline - no data sent to cloud
 */
class SafeSphereAutofillService : AutofillService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: PasswordVaultRepository

    companion object {
        private const val TAG = "SafeSphereAutofill"
    }

    override fun onCreate() {
        super.onCreate()
        repository = PasswordVaultRepository.getInstance(applicationContext)
        Log.d(TAG, "🔐 SafeSphere Autofill Service started")
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
        Log.d(TAG, "📝 Fill request received")

        scope.launch {
            try {
                // Parse the screen structure
                val structure = request.fillContexts.lastOrNull()?.structure
                if (structure == null) {
                    callback.onSuccess(null)
                    return@launch
                }

                // Find login fields (username/email and password)
                val loginFields = findLoginFields(structure)
                if (loginFields == null) {
                    Log.d(TAG, "No login fields found")
                    callback.onSuccess(null)
                    return@launch
                }

                // Get app/website info
                val packageName = structure.activityComponent.packageName
                val appName = getAppName(packageName)

                Log.d(TAG, "🎯 Login form detected in: $appName ($packageName)")

                // Search for saved credentials for this app/website
                val savedPasswords = repository.searchPasswords(appName)

                if (savedPasswords.isEmpty()) {
                    Log.d(TAG, "No saved passwords for $appName")
                    callback.onSuccess(null)
                    return@launch
                }

                // Build autofill response
                val response = createFillResponse(loginFields, savedPasswords, appName)
                callback.onSuccess(response)

                Log.d(TAG, "✅ Provided ${savedPasswords.size} autofill suggestions")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Fill request failed: ${e.message}", e)
                callback.onSuccess(null)
            }
        }
    }

    /**
     * Called when user submits a form with credentials
     * We save the credentials here
     */
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        Log.d(TAG, "💾 Save request received")

        scope.launch {
            try {
                // Parse the submitted data
                val structure = request.fillContexts.lastOrNull()?.structure
                if (structure == null) {
                    callback.onSuccess()
                    return@launch
                }

                // Find login fields
                val loginFields = findLoginFields(structure)
                if (loginFields == null) {
                    callback.onSuccess()
                    return@launch
                }

                // Extract credentials from the form
                val credentials = extractCredentials(request, loginFields)
                if (credentials == null) {
                    Log.d(TAG, "Could not extract credentials")
                    callback.onSuccess()
                    return@launch
                }

                // Get app/website info
                val packageName = structure.activityComponent.packageName
                val appName = getAppName(packageName)
                val url = extractUrl(structure) ?: packageName

                Log.d(TAG, "🔑 Credentials detected for: $appName")
                Log.d(TAG, "   Username: ${credentials.username}")
                Log.d(TAG, "   Password: ${credentials.password.take(3)}***")

                // Save to password vault
                val category = detectCategory(packageName)
                
                val result = repository.savePassword(
                    service = appName,
                    username = credentials.username,
                    password = credentials.password,
                    url = url,
                    category = category
                )

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Password saved to SafeSphere vault")
                    callback.onSuccess()
                } else {
                    Log.e(TAG, "❌ Failed to save password: ${result.exceptionOrNull()?.message}")
                    callback.onSuccess()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Save request failed: ${e.message}", e)
                callback.onSuccess()
            }
        }
    }

    /**
     * Find username and password fields in the screen structure
     */
    private fun findLoginFields(structure: AssistStructure): LoginFields? {
        var usernameField: AutofillId? = null
        var passwordField: AutofillId? = null

        // Traverse the view hierarchy
        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            val viewNode = windowNode.rootViewNode
            
            findLoginFieldsRecursive(viewNode) { username, password ->
                usernameField = username
                passwordField = password
            }
        }

        return if (usernameField != null && passwordField != null) {
            LoginFields(usernameField!!, passwordField!!)
        } else {
            null
        }
    }

    /**
     * Recursively search for login fields
     */
    private fun findLoginFieldsRecursive(
        node: AssistStructure.ViewNode,
        callback: (username: AutofillId?, password: AutofillId?) -> Unit
    ) {
        val autofillId = node.autofillId
        val autofillHints = node.autofillHints
        val inputType = node.inputType
        val hint = node.hint?.lowercase() ?: ""
        val idEntry = node.idEntry?.lowercase() ?: ""

        // Check if this is a username/email field
        val isUsernameField = autofillHints?.any { 
            it == "username" || it == "email" || it == "emailAddress"
        } == true || hint.contains("email") || hint.contains("username") ||
          idEntry.contains("email") || idEntry.contains("username") ||
          idEntry.contains("user")

        // Check if this is a password field
        val isPasswordField = autofillHints?.any { 
            it == "password"
        } == true || (inputType and 0x00000080) != 0 || // PASSWORD variation
          hint.contains("password") || idEntry.contains("password") ||
          idEntry.contains("pwd")

        if (isUsernameField && autofillId != null) {
            callback(autofillId, null)
        }

        if (isPasswordField && autofillId != null) {
            callback(null, autofillId)
        }

        // Recursively check children
        for (i in 0 until node.childCount) {
            findLoginFieldsRecursive(node.getChildAt(i), callback)
        }
    }

    /**
     * Create fill response with autofill suggestions
     */
    private fun createFillResponse(
        loginFields: LoginFields,
        savedPasswords: List<com.runanywhere.startup_hackathon20.data.PasswordVaultEntry>,
        appName: String
    ): FillResponse {
        val responseBuilder = FillResponse.Builder()

        // Add a dataset for each saved password
        savedPasswords.forEach { password ->
            val dataset = createDataset(loginFields, password)
            responseBuilder.addDataset(dataset)
        }

        // Add save info so we can save new credentials
        val saveInfo = SaveInfo.Builder(
            SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
            arrayOf(loginFields.usernameId, loginFields.passwordId)
        ).build()

        responseBuilder.setSaveInfo(saveInfo)

        return responseBuilder.build()
    }

    /**
     * Create a dataset (autofill suggestion) for one password entry
     */
    private fun createDataset(
        loginFields: LoginFields,
        password: com.runanywhere.startup_hackathon20.data.PasswordVaultEntry
    ): Dataset {
        val datasetBuilder = Dataset.Builder()

        // Create presentation view (what user sees in the dropdown)
        val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(
                android.R.id.text1,
                "🔐 ${password.service} - ${password.username}"
            )
        }

        // We can't decrypt password here (requires biometric)
        // So we just show the suggestion, and decrypt on-demand
        // For now, we'll use placeholder - in full implementation,
        // we'd show a biometric prompt before autofilling

        datasetBuilder.setValue(
            loginFields.usernameId,
            AutofillValue.forText(password.username),
            presentation
        )

        // Note: Password decryption would happen after biometric auth
        // For demo purposes, we show masked password
        val maskedPassword = "••••••••"
        datasetBuilder.setValue(
            loginFields.passwordId,
            AutofillValue.forText(maskedPassword),
            presentation
        )

        return datasetBuilder.build()
    }

    /**
     * Extract credentials from submitted form
     */
    private fun extractCredentials(
        request: SaveRequest,
        loginFields: LoginFields
    ): Credentials? {
        val clientState = request.clientState ?: return null
        
        var username: String? = null
        var password: String? = null

        // Get values from the filled form
        for (context in request.fillContexts) {
            val structure = context.structure

            for (i in 0 until structure.windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                extractCredentialsRecursive(
                    windowNode.rootViewNode,
                    loginFields
                ) { u, p ->
                    username = u
                    password = p
                }
            }
        }

        return if (username != null && password != null) {
            Credentials(username!!, password!!)
        } else {
            null
        }
    }

    /**
     * Recursively extract credentials from view nodes
     */
    private fun extractCredentialsRecursive(
        node: AssistStructure.ViewNode,
        loginFields: LoginFields,
        callback: (username: String?, password: String?) -> Unit
    ) {
        val autofillId = node.autofillId
        val autofillValue = node.autofillValue

        if (autofillValue != null && autofillValue.isText) {
            val text = autofillValue.textValue.toString()

            when (autofillId) {
                loginFields.usernameId -> callback(text, null)
                loginFields.passwordId -> callback(null, text)
            }
        }

        // Recursively check children
        for (i in 0 until node.childCount) {
            extractCredentialsRecursive(node.getChildAt(i), loginFields, callback)
        }
    }

    /**
     * Get human-readable app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            // Fallback to package name
            packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() }
                ?: packageName
        }
    }

    /**
     * Extract URL from browser view structure
     */
    private fun extractUrl(structure: AssistStructure): String? {
        // Try to find URL in browser address bar
        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            val url = findUrlRecursive(windowNode.rootViewNode)
            if (url != null) return url
        }
        return null
    }

    /**
     * Recursively search for URL in view hierarchy
     */
    private fun findUrlRecursive(node: AssistStructure.ViewNode): String? {
        // Check if this node contains a URL
        val webDomain = node.webDomain
        if (webDomain != null) return webDomain

        val text = node.text?.toString()
        if (text != null && (text.startsWith("http://") || text.startsWith("https://"))) {
            return text
        }

        // Check children
        for (i in 0 until node.childCount) {
            val url = findUrlRecursive(node.getChildAt(i))
            if (url != null) return url
        }

        return null
    }

    /**
     * Detect password category from package name
     */
    private fun detectCategory(packageName: String): PasswordCategory {
        return when {
            packageName.contains("gmail") || packageName.contains("mail") || 
            packageName.contains("outlook") -> PasswordCategory.EMAIL
            
            packageName.contains("facebook") || packageName.contains("instagram") ||
            packageName.contains("twitter") || packageName.contains("linkedin") ||
            packageName.contains("whatsapp") -> PasswordCategory.SOCIAL
            
            packageName.contains("bank") || packageName.contains("paypal") ||
            packageName.contains("venmo") || packageName.contains("gpay") -> PasswordCategory.BANKING
            
            packageName.contains("amazon") || packageName.contains("ebay") ||
            packageName.contains("shopping") -> PasswordCategory.SHOPPING
            
            packageName.contains("netflix") || packageName.contains("spotify") ||
            packageName.contains("youtube") || packageName.contains("game") -> PasswordCategory.ENTERTAINMENT
            
            packageName.contains("chrome") || packageName.contains("browser") ||
            packageName.contains("firefox") -> PasswordCategory.WEB
            
            else -> PasswordCategory.APP
        }
    }

    /**
     * Data class to hold login field IDs
     */
    private data class LoginFields(
        val usernameId: AutofillId,
        val passwordId: AutofillId
    )

    /**
     * Data class to hold extracted credentials
     */
    private data class Credentials(
        val username: String,
        val password: String
    )
}
