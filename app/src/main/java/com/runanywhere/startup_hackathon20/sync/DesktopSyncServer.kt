package com.runanywhere.startup_hackathon20.sync

import android.content.Context
import android.text.Html.escapeHtml
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom

/**
 * Complete Desktop Sync Server - Full SafeSphere Web App
 *
 * Features:
 * - Built-in HTTP server (no external dependencies!)
 * - Serves complete web version of SafeSphere
 * - All features accessible via browser
 * - 100% offline functionality
 * - Persistent until manually stopped
 * - Works on any device with a browser
 */
class DesktopSyncServer(
    private val context: Context,
    private val port: Int = 8888
) {

    companion object {
        private const val TAG = "DesktopSync"
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _connectedClients = MutableStateFlow(0)
    val connectedClients: StateFlow<Int> = _connectedClients

    private var sessionKey: String? = null
    private var totalRequests = 0


    /**
     * Start the HTTP server
     */
    fun startServer(): Boolean {
        return try {
            if (_isRunning.value) {
                Log.d(TAG, "Server already running")
                return true
            }

            Log.d(TAG, "Starting Desktop Sync server...")

            // Generate session key
            sessionKey = generateSessionKey()
            Log.d(TAG, "Generated session key: $sessionKey")

            // Get IP address first to verify network connectivity
            val ipAddress = getLocalIpAddress()
            Log.d(TAG, "Device IP address: $ipAddress")

            // Start server socket - bind to all interfaces (0.0.0.0)
            try {
                serverSocket = ServerSocket(port, 50, null) // Bind to all interfaces
                Log.d(TAG, "ServerSocket created successfully on port $port")
            } catch (e: java.net.BindException) {
                Log.e(TAG, "Port $port is already in use", e)
                return false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create ServerSocket", e)
                return false
            }

            _isRunning.value = true

            Log.d(TAG, "✅ Desktop sync server started on port $port")
            Log.d(TAG, "📡 Server URL: ${getConnectionUrl()}")
            Log.d(TAG, "🔑 Session key: $sessionKey")

            // Start accepting connections
            serverJob = scope.launch {
                acceptConnections()
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start server", e)
            _isRunning.value = false
            false
        }
    }

    /**
     * Stop the HTTP server
     */
    fun stopServer() {
        try {
            Log.d(TAG, "Stopping desktop sync server...")

            _isRunning.value = false
            serverJob?.cancel()
            serverSocket?.close()

            serverSocket = null
            serverJob = null
            sessionKey = null
            totalRequests = 0
            _connectedClients.value = 0

            Log.d(TAG, "✅ Desktop sync server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    /**
     * Get connection URL with session key
     */
    fun getConnectionUrl(): String {
        val ip = getLocalIpAddress()
        return "http://$ip:$port/?key=$sessionKey"
    }

    /**
     * Accept incoming HTTP connections
     */
    private suspend fun acceptConnections() {
        try {
            while (_isRunning.value && serverSocket != null) {
                try {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null) {
                        scope.launch {
                            handleClient(clientSocket)
                        }
                    }
                } catch (e: Exception) {
                    if (_isRunning.value) {
                        Log.e(TAG, "Error accepting connection", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Server accept loop error", e)
        }
    }

    private val passwordRepository by lazy {
        com.runanywhere.startup_hackathon20.data.PasswordVaultRepository.getInstance(context)
    }

    private var lastDataHash = 0

    /**
     * Handle individual HTTP client request
     */
    private fun handleClient(socket: Socket) {
        try {
            totalRequests++
            _connectedClients.value = totalRequests

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            // Read HTTP request
            val requestLine = reader.readLine() ?: return
            Log.d(TAG, "📥 Request: $requestLine")

            // Parse request
            val parts = requestLine.split(" ")
            if (parts.size >= 2) {
                val method = parts[0]
                val path = parts[1]

                // Read headers
                val headers = mutableMapOf<String, String>()
                var line = reader.readLine()
                var contentLength = 0
                while (line != null && line.isNotEmpty()) {
                    val headerParts = line.split(": ", limit = 2)
                    if (headerParts.size == 2) {
                        headers[headerParts[0].lowercase()] = headerParts[1]
                        if (headerParts[0].lowercase() == "content-length") {
                            contentLength = headerParts[1].toIntOrNull() ?: 0
                        }
                    }
                    line = reader.readLine()
                }

                // Read body if present
                val body = if (contentLength > 0) {
                    val buffer = CharArray(contentLength)
                    reader.read(buffer, 0, contentLength)
                    String(buffer)
                } else {
                    ""
                }

                // Route request
                when {
                    path == "/" || path.startsWith("/?") -> {
                        serveMainApp(writer)
                    }

                    // API Endpoints
                    path == "/api/ping" -> servePing(writer)
                    path == "/api/status" -> serveStatus(writer)
                    path == "/api/sync" -> serveSync(writer)
                    path == "/api/passwords" && method == "GET" -> serveGetPasswords(writer)
                    path == "/api/passwords" && method == "POST" -> serveAddPassword(writer, body)
                    path.startsWith("/api/passwords/") && method == "DELETE" -> {
                        val id = path.substringAfterLast("/")
                        serveDeletePassword(writer, id)
                    }
                    path.startsWith("/api/passwords/") && path.endsWith("/decrypt") && method == "GET" -> {
                        val id = path.substringAfter("/api/passwords/").substringBefore("/decrypt")
                        serveDecryptPassword(writer, id)
                    }
                    path.startsWith("/api/passwords/") && method == "PUT" -> {
                        val id = path.substringAfter("/api/passwords/")
                        serveUpdatePassword(writer, id, body)
                    }

                    path == "/api/generate" && method == "POST" -> serveGeneratePassword(
                        writer,
                        body
                    )

                    // Autofill & Auto-save endpoints
                    path.startsWith("/api/autofill") && method == "GET" -> serveAutofill(
                        writer,
                        parts.getOrNull(1)
                    )

                    path == "/api/save-prompt" && method == "GET" -> serveSavePrompt(writer)
                    path == "/api/save-prompt/accept" && method == "POST" -> serveAcceptPrompt(
                        writer,
                        body
                    )

                    path == "/api/save-prompt" && method == "DELETE" -> serveDismissPrompt(writer)

                    else -> {
                        serveNotFound(writer)
                    }
                }
            }

            writer.flush()
            socket.close()

        } catch (e: Exception) {
            Log.e(TAG, "Error handling client", e)
        }
    }

    // ====== AUTOFILL & AUTO-SAVE ENDPOINTS ======

    // GET /api/autofill?url=example.com
    private fun serveAutofill(writer: PrintWriter, fullPath: String?) {
        try {
            // Extract query parameter from path
            val urlParam = fullPath?.substringAfter("url=", "")?.substringBefore("&")

            if (urlParam.isNullOrBlank()) {
                writeErrorResponse(writer, "Missing url parameter")
                return
            }

            // Search for matching passwords
            val allPasswords = passwordRepository.passwords.value
            val matches = allPasswords.filter { pwd ->
                val domain = extractDomain(urlParam)
                val savedDomain = extractDomain(pwd.url)

                // Match by domain or service name
                domain.isNotBlank() && savedDomain.isNotBlank() &&
                        (domain.equals(savedDomain, ignoreCase = true) ||
                                pwd.service.contains(domain, ignoreCase = true) ||
                                domain.contains(pwd.service, ignoreCase = true))
            }

            // Build JSON array of matches
            val json = matches.joinToString(
                prefix = "[", postfix = "]", separator = ","
            ) { pw ->
                """
                {
                    "id": "${pw.id}",
                    "service": ${escapeJson(pw.service)},
                    "username": ${escapeJson(pw.username)},
                    "strengthScore": ${pw.strengthScore}
                }
                """.trimIndent()
            }

            writeJsonResponse(writer, json)
            Log.d(TAG, "✅ Autofill: Found ${matches.size} matches for $urlParam")

        } catch (e: Exception) {
            Log.e(TAG, "Error in autofill", e)
            writeErrorResponse(writer, "Autofill failed: ${e.message}")
        }
    }

    // GET /api/save-prompt - Check if there's a pending save prompt
    private fun serveSavePrompt(writer: PrintWriter) {
        try {
            val prompt = passwordRepository.pendingSavePrompt.value

            val json = if (prompt != null) {
                """
                {
                    "pending": true,
                    "service": ${escapeJson(prompt.service)},
                    "username": ${escapeJson(prompt.username)},
                    "detectedFrom": ${escapeJson(prompt.detectedFrom)}
                }
                """.trimIndent()
            } else {
                """{"pending": false}"""
            }

            writeJsonResponse(writer, json)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting save prompt", e)
            writeErrorResponse(writer, "Failed to get prompt: ${e.message}")
        }
    }

    // POST /api/save-prompt/accept
    private fun serveAcceptPrompt(writer: PrintWriter, body: String) {
        try {
            val obj = org.json.JSONObject(body)
            val category = try {
                com.runanywhere.startup_hackathon20.data.PasswordCategory.valueOf(
                    obj.optString("category", "WEB")
                )
            } catch (e: Exception) {
                com.runanywhere.startup_hackathon20.data.PasswordCategory.WEB
            }

            val result = kotlinx.coroutines.runBlocking {
                passwordRepository.acceptSavePrompt(category)
            }

            if (result.isSuccess) {
                writeJsonResponse(writer, """{"accepted": true}""")
                Log.d(TAG, "✅ Save prompt accepted via web")
            } else {
                writeErrorResponse(writer, "Failed to accept prompt")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting prompt", e)
            writeErrorResponse(writer, "Failed to accept: ${e.message}")
        }
    }

    // DELETE /api/save-prompt - Dismiss the prompt
    private fun serveDismissPrompt(writer: PrintWriter) {
        try {
            passwordRepository.dismissSavePrompt()
            writeJsonResponse(writer, """{"dismissed": true}""")
            Log.d(TAG, "✅ Save prompt dismissed via web")
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing prompt", e)
            writeErrorResponse(writer, "Failed to dismiss: ${e.message}")
        }
    }

    // Helper: Extract domain from URL (same logic as autofill service)
    private fun extractDomain(url: String): String {
        return try {
            url.trim()
                .replace("https://", "")
                .replace("http://", "")
                .replace("www.", "")
                .lowercase()
                .substringBefore("/")
                .substringBefore(":")
                .trim()
        } catch (e: Exception) {
            url
        }
    }

    // ====== API ENDPOINTS ======

    // GET /api/passwords
    private fun serveGetPasswords(writer: PrintWriter) {
        try {
            val passwordList = passwordRepository.passwords.value
            // Build JSON array
            val json = passwordList.joinToString(
                prefix = "[", postfix = "]", separator = ","
            ) { pw ->
                """
                {
                    "id": "${pw.id}",
                    "service": ${escapeJson(pw.service)},
                    "username": ${escapeJson(pw.username)},
                    "url": ${escapeJson(pw.url)},
                    "category": "${pw.category.name}",
                    "strengthScore": ${pw.strengthScore},
                    "isFavorite": ${pw.isFavorite},
                    "createdAt": ${pw.createdAt},
                    "modifiedAt": ${pw.modifiedAt}
                }
                """.trimIndent()
            }
            writeJsonResponse(writer, json)
            lastDataHash = passwordList.hashCode()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting passwords", e)
            writeErrorResponse(writer, "Failed to get passwords: ${e.message}")
        }
    }

    // POST /api/passwords
    private fun serveAddPassword(writer: PrintWriter, body: String) {
        try {
            val obj = org.json.JSONObject(body)
            val service = obj.optString("service", "")
            val username = obj.optString("username", "")
            val password = obj.optString("password", "")

            if (service.isBlank() || username.isBlank() || password.isBlank()) {
                writeErrorResponse(writer, "Missing required fields")
                return
            }

            // Call suspend function using runBlocking
            val result = kotlinx.coroutines.runBlocking {
                passwordRepository.savePassword(
                    service = service,
                    username = username,
                    password = password,
                    url = obj.optString("url", ""),
                    notes = obj.optString("notes", ""),
                    category = try {
                        com.runanywhere.startup_hackathon20.data.PasswordCategory.valueOf(
                            obj.optString("category", "WEB")
                        )
                    } catch (e: Exception) {
                        com.runanywhere.startup_hackathon20.data.PasswordCategory.WEB
                    }
                )
            }

            if (result.isSuccess) {
                val pw = result.getOrNull()!!
                val json = """
                {
                    "id": "${pw.id}",
                    "service": ${escapeJson(pw.service)},
                    "username": ${escapeJson(pw.username)},
                    "url": ${escapeJson(pw.url)},
                    "strengthScore": ${pw.strengthScore},
                    "createdAt": ${pw.createdAt}
                }
                """.trimIndent()
                writeJsonResponse(writer, json)
                lastDataHash = passwordRepository.passwords.value.hashCode()
            } else {
                writeErrorResponse(writer, "Failed to save password")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding password", e)
            writeErrorResponse(writer, "Failed to add password: ${e.message}")
        }
    }

    // PUT /api/passwords/:id
    private fun serveUpdatePassword(writer: PrintWriter, id: String, body: String) {
        try {
            val existing = passwordRepository.passwords.value.find { it.id == id }
            if (existing == null) {
                writeErrorResponse(writer, "Password not found")
                return
            }

            val obj = org.json.JSONObject(body)
            val service = obj.optString("service", existing.service)
            val username = obj.optString("username", existing.username)

            // Get the password - need to decrypt existing one if no new password provided
            val password = if (obj.has("password") && obj.optString("password").isNotBlank()) {
                obj.optString("password")
            } else {
                // Use existing (decrypt it)
                val decryptResult = kotlinx.coroutines.runBlocking {
                    passwordRepository.getDecryptedPassword(id)
                }
                decryptResult.getOrNull()?.password ?: ""
            }

            if (password.isBlank()) {
                writeErrorResponse(writer, "Invalid password")
                return
            }

            // Call suspend function
            val result = kotlinx.coroutines.runBlocking {
                passwordRepository.updatePassword(
                    id = id,
                    service = service,
                    username = username,
                    password = password,
                    url = obj.optString("url", existing.url),
                    notes = obj.optString("notes", ""),
                    category = try {
                        com.runanywhere.startup_hackathon20.data.PasswordCategory.valueOf(
                            obj.optString("category", existing.category.name)
                        )
                    } catch (e: Exception) {
                        existing.category
                    }
                )
            }

            if (result.isSuccess) {
                // Get updated entry
                val updated = passwordRepository.passwords.value.find { it.id == id }!!
                val json = """
                {
                    "id": "${updated.id}",
                    "service": ${escapeJson(updated.service)},
                    "username": ${escapeJson(updated.username)},
                    "strengthScore": ${updated.strengthScore},
                    "modifiedAt": ${updated.modifiedAt}
                }
                """.trimIndent()
                writeJsonResponse(writer, json)
                lastDataHash = passwordRepository.passwords.value.hashCode()
            } else {
                writeErrorResponse(writer, "Failed to update password")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating password", e)
            writeErrorResponse(writer, "Failed to update password: ${e.message}")
        }
    }

    // DELETE /api/passwords/:id
    private fun serveDeletePassword(writer: PrintWriter, id: String) {
        try {
            val result = kotlinx.coroutines.runBlocking {
                passwordRepository.deletePassword(id)
            }

            if (result.isSuccess) {
                val json = """{"deleted":true}"""
                writeJsonResponse(writer, json)
                lastDataHash = passwordRepository.passwords.value.hashCode()
            } else {
                writeErrorResponse(writer, "Failed to delete password")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting password", e)
            writeErrorResponse(writer, "Failed to delete password: ${e.message}")
        }
    }

    // GET /api/passwords/:id/decrypt
    private fun serveDecryptPassword(writer: PrintWriter, id: String) {
        try {
            val result = kotlinx.coroutines.runBlocking {
                passwordRepository.getDecryptedPassword(id)
            }

            if (result.isSuccess) {
                val decrypted = result.getOrNull()!!
                val json = """{"password":${escapeJson(decrypted.password)}}"""
                writeJsonResponse(writer, json)
            } else {
                writeErrorResponse(writer, "Failed to decrypt password")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting password", e)
            writeErrorResponse(writer, "Failed to decrypt: ${e.message}")
        }
    }

    // POST /api/generate
    private fun serveGeneratePassword(writer: PrintWriter, body: String) {
        try {
            val obj = org.json.JSONObject(body)
            val length = obj.optInt("length", 16)
            val includeNumbers = obj.optBoolean("includeNumbers", true)
            val includeSymbols = obj.optBoolean("includeSymbols", true)

            val password = generateRandomPassword(length, includeNumbers, includeSymbols)
            val json = """{"password":${escapeJson(password)}}"""
            writeJsonResponse(writer, json)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating password", e)
            writeErrorResponse(writer, "Failed to generate password: ${e.message}")
        }
    }

    // GET /api/sync - Check if data changed
    private fun serveSync(writer: PrintWriter) {
        try {
            val currentHash = passwordRepository.passwords.value.hashCode()
            val changed = (currentHash != lastDataHash)
            val count = passwordRepository.passwords.value.size
            val json = """{"changed":$changed,"hash":"$currentHash","count":$count}"""
            writeJsonResponse(writer, json)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing", e)
            writeErrorResponse(writer, "Failed to sync: ${e.message}")
        }
    }

    // ====== UTILS ======

    private fun writeJsonResponse(writer: PrintWriter, json: String) {
        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: application/json")
        writer.println("Content-Length: ${json.toByteArray().size}")
        writer.println("Connection: close")
        writer.println()
        writer.println(json)
    }

    private fun writeErrorResponse(writer: PrintWriter, msg: String) {
        val json = """{"error":${escapeJson(msg)}}"""
        writer.println("HTTP/1.1 400 Bad Request")
        writer.println("Content-Type: application/json")
        writer.println("Content-Length: ${json.toByteArray().size}")
        writer.println("Connection: close")
        writer.println()
        writer.println(json)
    }

    private fun escapeJson(value: String?): String {
        if (value == null) return "\"\""
        return "\"" + value.replace("\"", "\\\"").replace("\n", "\\n") + "\""
    }

    // Allows matching the web app's JS strength function server-side
    private fun calculatePasswordStrength(password: String): Int {
        var strength = 0
        if (password.length >= 8) strength += 20
        if (password.length >= 12) strength += 20
        if (password.length >= 16) strength += 10
        if (password.any { it.isLowerCase() }) strength += 10
        if (password.any { it.isUpperCase() }) strength += 15
        if (password.any { it.isDigit() }) strength += 15
        if (password.any { !it.isLetterOrDigit() }) strength += 10
        return minOf(100, strength)
    }

    private fun generateRandomPassword(
        length: Int,
        includeNumbers: Boolean,
        includeSymbols: Boolean
    ): String {
        var chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        if (includeNumbers) chars += "0123456789"
        if (includeSymbols) chars += "!@#\$%^&*()_+-=[]{}|;:,.<>?"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Serve main SafeSphere web application
     */
    private fun serveMainApp(writer: PrintWriter) {
        val html = getWebAppHTML(sessionKey ?: "UNKNOWN")

        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: text/html; charset=UTF-8")
        writer.println("Content-Length: ${html.toByteArray().size}")
        writer.println("Connection: close")
        writer.println()
        writer.println(html)
    }

    /**
     * Serve ping endpoint
     */
    private fun servePing(writer: PrintWriter) {
        val json = """{"status":"ok","timestamp":${System.currentTimeMillis()}}"""

        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: application/json")
        writer.println("Content-Length: ${json.toByteArray().size}")
        writer.println("Connection: close")
        writer.println()
        writer.println(json)
    }

    /**
     * Serve status endpoint
     */
    private fun serveStatus(writer: PrintWriter) {
        val json =
            """{"running":${_isRunning.value},"requests":$totalRequests,"key":"$sessionKey"}"""

        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: application/json")
        writer.println("Content-Length: ${json.toByteArray().size}")
        writer.println("Connection: close")
        writer.println()
        writer.println(json)
    }

    /**
     * Serve 404 not found
     */
    private fun serveNotFound(writer: PrintWriter) {
        val html = "<html><body><h1>404 Not Found</h1></body></html>"

        writer.println("HTTP/1.1 404 Not Found")
        writer.println("Content-Type: text/html")
        writer.println("Content-Length: ${html.toByteArray().size}")
        writer.println("Connection: close")
        writer.println()
        writer.println(html)
    }

    /**
     * Get local IP address
     */
    private fun getLocalIpAddress(): String {
        return try {
            // Try WiFi Manager first
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE)
                    as? android.net.wifi.WifiManager

            if (wifiManager != null) {
                val wifiInfo = wifiManager.connectionInfo
                if (wifiInfo != null) {
                    val ipInt = wifiInfo.ipAddress

                    if (ipInt != 0) {
                        val ip = String.format(
                            java.util.Locale.US,
                            "%d.%d.%d.%d",
                            ipInt and 0xff,
                            ipInt shr 8 and 0xff,
                            ipInt shr 16 and 0xff,
                            ipInt shr 24 and 0xff
                        )
                        Log.d(TAG, "WiFi IP detected: $ip")
                        return ip
                    }
                }
            }

            // Fallback: Try to get IP from NetworkInterface
            Log.d(TAG, "WiFi IP not available, trying NetworkInterface...")
            val networkInterfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val addresses = networkInterface.inetAddresses

                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()

                    // Skip loopback and IPv6
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        val ip = address.hostAddress ?: continue
                        // Prefer 192.168.x.x addresses
                        if (ip.startsWith("192.168.")) {
                            Log.d(TAG, "NetworkInterface IP detected: $ip")
                            return ip
                        }
                    }
                }
            }

            Log.w(TAG, "No valid IP address found, using default")
            "192.168.1.100" // Default fallback

        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP address", e)
            "192.168.1.100" // Default fallback
        }
    }

    /**
     * Generate random session key
     */
    private fun generateSessionKey(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = SecureRandom()
        return (1..8)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Get complete web application HTML with EXACT Android UI styling
     */
    private fun getWebAppHTML(sessionKey: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SafeSphere Desktop</title>
    <style>
        /* =========================
           ANDROID APP COLOR SCHEME
           ========================= */
        :root {
            /* Primary Colors - EXACT from Android */
            --deep-navy: #050C1B;
            --dark-background: #0A1628;
            --card-background: #0F1A2E;
            --surface-variant: #1A2942;
            
            /* Accent Colors */
            --cyan-glow: #00FFFF;
            --cyan-light: #29ABE2;
            --purple: #8888FF;
            --amber-stressed: #FFAA00;
            
            /* Text Colors */
            --text-primary: #FFFFFF;
            --text-secondary: #B0BEC5;
            --text-tertiary: #78909C;
            
            /* Emotion/Status Colors */
            --success: #4CAF50;
            --warning: #FFA726;
            --error: #EF5350;
            --info: #42A5F5;
        }
        
        /* =========================
           RESET & BASE STYLES
           ========================= */
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: linear-gradient(180deg, var(--deep-navy) 0%, var(--dark-background) 100%);
            color: var(--text-primary);
            min-height: 100vh;
            overflow-x: hidden;
        }
        
        /* =========================
           GLASSMORPHISM EFFECTS
           ========================= */
        .glass-card {
            background: rgba(15, 26, 46, 0.7);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border: 1px solid rgba(41, 171, 226, 0.2);
            border-radius: 18px;
            padding: 20px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
            transition: all 0.3s ease;
        }
        
        .glass-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 12px 48px rgba(0, 255, 255, 0.2);
            border-color: rgba(41, 171, 226, 0.4);
        }
        
        /* =========================
           TOP BAR - EXACT ANDROID STYLE
           ========================= */
        .top-bar {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            height: 80px;
            background: rgba(15, 26, 46, 0.95);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border-bottom: 1px solid rgba(41, 171, 226, 0.2);
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 20px 16px 0 16px;
            z-index: 1000;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
        }
        
        .top-bar-gradient-bg {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: linear-gradient(90deg, 
                rgba(0, 255, 255, 0.1) 0%, 
                rgba(136, 136, 255, 0.1) 50%, 
                rgba(255, 170, 0, 0.1) 100%);
            pointer-events: none;
        }
        
        .menu-btn, .notif-btn {
            width: 48px;
            height: 48px;
            border-radius: 50%;
            background: rgba(0, 255, 255, 0.1);
            border: none;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            font-size: 24px;
            color: var(--cyan-glow);
            transition: all 0.2s ease;
            z-index: 2;
        }
        
        .menu-btn:hover, .notif-btn:hover {
            background: rgba(0, 255, 255, 0.2);
            transform: scale(1.05);
        }
        
        .top-bar-title {
            font-size: 26px;
            font-weight: bold;
            text-align: center;
            flex: 1;
            z-index: 2;
            background: linear-gradient(90deg, var(--cyan-glow), var(--purple), var(--amber-stressed));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }
        
        /* =========================
           NAVIGATION DRAWER - ANDROID STYLE
           ========================= */
        .nav-drawer {
            position: fixed;
            top: 0;
            left: -320px;
            width: 320px;
            height: 100vh;
            background: var(--card-background);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border-right: 1px solid rgba(41, 171, 226, 0.3);
            z-index: 2000;
            transition: left 0.3s ease;
            overflow-y: auto;
            box-shadow: 4px 0 32px rgba(0, 0, 0, 0.5);
        }
        
        .nav-drawer.open {
            left: 0;
        }
        
        .drawer-overlay {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.7);
            z-index: 1999;
            opacity: 0;
            pointer-events: none;
            transition: opacity 0.3s ease;
        }
        
        .drawer-overlay.show {
            opacity: 1;
            pointer-events: all;
        }
        
        .drawer-header {
            padding: 32px 24px;
            background: linear-gradient(135deg, rgba(0, 255, 255, 0.1), rgba(136, 136, 255, 0.1));
            border-bottom: 1px solid rgba(41, 171, 226, 0.2);
        }
        
        .drawer-user-info {
            display: flex;
            align-items: center;
            gap: 16px;
        }
        
        .drawer-user-avatar {
            width: 64px;
            height: 64px;
            border-radius: 50%;
            background: linear-gradient(135deg, var(--cyan-light), var(--purple));
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 32px;
        }
        
        .drawer-user-name {
            font-size: 18px;
            font-weight: bold;
            color: var(--text-primary);
        }
        
        .drawer-user-email {
            font-size: 14px;
            color: var(--text-secondary);
        }
        
        .drawer-nav {
            padding: 16px 0;
        }
        
        .drawer-nav-item {
            display: flex;
            align-items: center;
            gap: 16px;
            padding: 16px 24px;
            cursor: pointer;
            transition: all 0.2s ease;
            border-left: 3px solid transparent;
        }
        
        .drawer-nav-item:hover {
            background: rgba(0, 255, 255, 0.1);
            border-left-color: var(--cyan-glow);
        }
        
        .drawer-nav-item.active {
            background: rgba(0, 255, 255, 0.15);
            border-left-color: var(--cyan-glow);
        }
        
        .drawer-nav-item-icon {
            font-size: 24px;
            width: 32px;
            text-align: center;
        }
        
        .drawer-nav-item-text {
            font-size: 16px;
            font-weight: 500;
            color: var(--text-primary);
        }
        
        .drawer-divider {
            height: 1px;
            background: rgba(41, 171, 226, 0.2);
            margin: 16px 24px;
        }
        
        /* =========================
           MAIN CONTENT AREA
           ========================= */
        .main-content {
            margin-top: 80px;
            padding: 24px 16px 24px 16px;
            max-width: 1200px;
            margin-left: auto;
            margin-right: auto;
        }
        
        /* =========================
           DASHBOARD CARDS
           ========================= */
        .security-score-card {
            text-align: center;
            padding: 32px 20px;
            margin-bottom: 24px;
        }
        
        .circular-progress {
            position: relative;
            width: 120px;
            height: 120px;
            margin: 16px auto;
        }
        
        .circular-progress svg {
            transform: rotate(-90deg);
        }
        
        .progress-ring-bg {
            fill: none;
            stroke: rgba(176, 190, 197, 0.1);
            stroke-width: 12;
        }
        
        .progress-ring {
            fill: none;
            stroke: var(--cyan-glow);
            stroke-width: 12;
            stroke-linecap: round;
            transition: stroke-dashoffset 0.5s ease;
        }
        
        .progress-text {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            font-size: 36px;
            font-weight: bold;
            color: var(--text-primary);
        }
        
        .dashboard-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 16px;
            margin-top: 24px;
        }
        
        .dashboard-card {
            cursor: pointer;
            height: 140px;
            position: relative;
            overflow: hidden;
        }
        
        .dashboard-card-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 16px;
        }
        
        .dashboard-card-icon {
            font-size: 32px;
        }
        
        .dashboard-card-indicator {
            width: 8px;
            height: 8px;
            border-radius: 50%;
        }
        
        .dashboard-card-footer {
            position: absolute;
            bottom: 20px;
            left: 20px;
            right: 20px;
        }
        
        .dashboard-card-title {
            font-size: 16px;
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: 4px;
        }
        
        .dashboard-card-desc {
            font-size: 12px;
            color: var(--text-secondary);
        }
        
        /* =========================
           PASSWORD MANAGER SCREEN
           ========================= */
        .password-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 24px;
        }
        
        .password-count {
            font-size: 18px;
            font-weight: bold;
            color: var(--text-primary);
        }
        
        .add-btn, .generate-btn, .primary-btn {
            background: linear-gradient(135deg, var(--cyan-light), var(--purple));
            border: none;
            border-radius: 12px;
            padding: 12px 24px;
            font-size: 16px;
            font-weight: 600;
            color: white;
            cursor: pointer;
            transition: all 0.2s ease;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        
        .add-btn:hover, .generate-btn:hover, .primary-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 24px rgba(0, 255, 255, 0.3);
        }
        
        .password-item {
            padding: 20px;
            margin-bottom: 12px;
            cursor: pointer;
        }
        
        .password-item-header {
            display: flex;
            align-items: center;
            gap: 16px;
        }
        
        .password-item-icon {
            width: 48px;
            height: 48px;
            border-radius: 12px;
            background: rgba(0, 255, 255, 0.2);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
        }
        
        .password-item-info {
            flex: 1;
        }
        
        .password-item-title {
            font-size: 16px;
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: 4px;
        }
        
        .password-item-username {
            font-size: 14px;
            color: var(--text-secondary);
        }
        
        .password-strength-badge {
            padding: 4px 12px;
            border-radius: 8px;
            font-size: 12px;
            font-weight: 600;
        }
        
        .strength-strong {
            background: rgba(76, 175, 80, 0.2);
            color: var(--success);
            border: 1px solid var(--success);
        }
        
        .strength-medium {
            background: rgba(255, 167, 38, 0.2);
            color: var(--warning);
            border: 1px solid var(--warning);
        }
        
        .strength-weak {
            background: rgba(239, 83, 80, 0.2);
            color: var(--error);
            border: 1px solid var(--error);
        }
        
        /* =========================
           FORMS & INPUTS
           ========================= */
        .input-group {
            margin-bottom: 20px;
        }
        
        .input-label {
            display: block;
            font-size: 14px;
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: 8px;
        }
        
        .input-field {
            width: 100%;
            padding: 14px 16px;
            border-radius: 12px;
            border: 1px solid rgba(41, 171, 226, 0.3);
            background: rgba(15, 26, 46, 0.5);
            color: var(--text-primary);
            font-size: 16px;
            transition: all 0.2s ease;
        }
        
        .input-field:focus {
            outline: none;
            border-color: var(--cyan-glow);
            box-shadow: 0 0 0 3px rgba(0, 255, 255, 0.1);
        }
        
        .input-field::placeholder {
            color: var(--text-tertiary);
        }
        
        /* =========================
           MODALS
           ========================= */
        .modal-overlay {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.8);
            display: none;
            align-items: center;
            justify-content: center;
            z-index: 3000;
            backdrop-filter: blur(8px);
        }
        
        .modal-overlay.show {
            display: flex;
        }
        
        .modal {
            background: var(--card-background);
            border: 1px solid rgba(41, 171, 226, 0.3);
            border-radius: 24px;
            padding: 32px;
            max-width: 500px;
            width: 90%;
            max-height: 90vh;
            overflow-y: auto;
            box-shadow: 0 24px 64px rgba(0, 0, 0, 0.5);
        }
        
        .modal-header {
            font-size: 24px;
            font-weight: bold;
            margin-bottom: 24px;
            color: var(--text-primary);
        }
        
        .modal-footer {
            display: flex;
            gap: 12px;
            margin-top: 24px;
        }
        
        .btn-secondary {
            flex: 1;
            padding: 14px 24px;
            border-radius: 12px;
            border: 1px solid rgba(41, 171, 226, 0.3);
            background: transparent;
            color: var(--text-primary);
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s ease;
        }
        
        .btn-secondary:hover {
            background: rgba(41, 171, 226, 0.1);
            border-color: var(--cyan-light);
        }
        
        /* =========================
           EMPTY STATES
           ========================= */
        .empty-state {
            text-align: center;
            padding: 64px 32px;
        }
        
        .empty-state-icon {
            font-size: 64px;
            margin-bottom: 24px;
        }
        
        .empty-state-title {
            font-size: 20px;
            font-weight: bold;
            color: var(--text-primary);
            margin-bottom: 12px;
        }
        
        .empty-state-desc {
            font-size: 14px;
            color: var(--text-secondary);
            margin-bottom: 32px;
        }
        
        /* =========================
           UTILITY CLASSES
           ========================= */
        .hidden {
            display: none !important;
        }
        
        .text-center {
            text-align: center;
        }
        
        .mb-16 {
            margin-bottom: 16px;
        }
        
        .mb-24 {
            margin-bottom: 24px;
        }
        
        .mt-24 {
            margin-top: 24px;
        }
        
        /* =========================
           RESPONSIVE DESIGN
           ========================= */
        @media (max-width: 768px) {
            .dashboard-grid {
                grid-template-columns: 1fr;
            }
            
            .top-bar-title {
                font-size: 20px;
            }
            
            .modal {
                padding: 24px;
            }
            
            .nav-drawer {
                width: 280px;
                left: -280px;
            }
        }
        
        /* =========================
           SCROLLBAR STYLING
           ========================= */
        ::-webkit-scrollbar {
            width: 10px;
        }
        
        ::-webkit-scrollbar-track {
            background: var(--dark-background);
        }
        
        ::-webkit-scrollbar-thumb {
            background: rgba(41, 171, 226, 0.3);
            border-radius: 5px;
        }
        
        ::-webkit-scrollbar-thumb:hover {
            background: rgba(41, 171, 226, 0.5);
        }

        /* =========================
           AUTOFILL & AUTO-SAVE FEATURES
           ========================= */
        .autofill-dropdown {
            position: absolute;
            top: 100%;
            left: 0;
            right: 0;
            margin-top: 8px;
            background: var(--card-background);
            border: 1px solid rgba(41, 171, 226, 0.3);
            border-radius: 12px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
            max-height: 300px;
            overflow-y: auto;
            z-index: 100;
            display: none;
        }
        
        .autofill-dropdown.show {
            display: block;
        }
        
        .autofill-item {
            padding: 12px 16px;
            cursor: pointer;
            transition: all 0.2s ease;
            border-bottom: 1px solid rgba(41, 171, 226, 0.1);
        }
        
        .autofill-item:last-child {
            border-bottom: none;
        }
        
        .autofill-item:hover {
            background: rgba(0, 255, 255, 0.1);
        }
        
        .autofill-item-title {
            font-size: 14px;
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: 4px;
        }
        
        .autofill-item-username {
            font-size: 12px;
            color: var(--text-secondary);
        }
        
        .autofill-indicator {
            position: absolute;
            right: 12px;
            top: 50%;
            transform: translateY(-50%);
            font-size: 18px;
            color: var(--cyan-glow);
            animation: pulse 2s infinite;
        }
        
        @keyframes pulse {
            0%, 100% { opacity: 0.5; }
            50% { opacity: 1; }
        }
        
        .auto-save-banner {
            position: fixed;
            top: 100px;
            left: 50%;
            transform: translateX(-50%);
            background: rgba(15, 26, 46, 0.98);
            backdrop-filter: blur(20px);
            border: 1px solid rgba(0, 255, 255, 0.5);
            border-radius: 16px;
            padding: 20px 24px;
            box-shadow: 0 12px 48px rgba(0, 255, 255, 0.3);
            z-index: 5000;
            display: none;
            min-width: 400px;
            max-width: 90%;
            animation: slideDown 0.3s ease;
        }
        
        @keyframes slideDown {
            from {
                opacity: 0;
                transform: translate(-50%, -20px);
            }
            to {
                opacity: 1;
                transform: translate(-50%, 0);
            }
        }
        
        .auto-save-banner.show {
            display: block;
        }
        
        .auto-save-header {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 16px;
        }
        
        .auto-save-icon {
            font-size: 32px;
        }
        
        .auto-save-title {
            font-size: 16px;
            font-weight: bold;
            color: var(--text-primary);
        }
        
        .auto-save-message {
            font-size: 14px;
            color: var(--text-secondary);
            margin-bottom: 16px;
        }
        
        .auto-save-actions {
            display: flex;
            gap: 12px;
        }
        
        .auto-save-btn {
            flex: 1;
            padding: 10px 20px;
            border-radius: 10px;
            border: none;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s ease;
        }
        
        .auto-save-btn-primary {
            background: linear-gradient(135deg, var(--cyan-light), var(--purple));
            color: white;
        }
        
        .auto-save-btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(0, 255, 255, 0.3);
        }
        
        .auto-save-btn-secondary {
            background: transparent;
            border: 1px solid rgba(41, 171, 226, 0.3);
            color: var(--text-primary);
        }
        
        .auto-save-btn-secondary:hover {
            background: rgba(41, 171, 226, 0.1);
        }
        
        .input-with-autofill {
            position: relative;
        }
    </style>
</head>
<body>
    <!-- Auto-Save Banner -->
    <div class="auto-save-banner" id="autoSaveBanner">
        <div class="auto-save-header">
            <div class="auto-save-icon">🔐</div>
            <div>
                <div class="auto-save-title">Save Password to SafeSphere?</div>
            </div>
        </div>
        <div class="auto-save-message" id="autoSaveMessage">
            Do you want to save this password for <strong id="autoSaveService">example.com</strong>?
        </div>
        <div class="auto-save-actions">
            <button class="auto-save-btn auto-save-btn-secondary" onclick="dismissAutoSave()">Not Now</button>
            <button class="auto-save-btn auto-save-btn-primary" onclick="acceptAutoSave()">💾 Save Password</button>
        </div>
    </div>
    
    <!-- Navigation Drawer Overlay -->
    <div class="drawer-overlay" id="drawerOverlay"></div>
    
    <!-- Navigation Drawer -->
    <div class="nav-drawer" id="navDrawer">
        <div class="drawer-header">
            <div class="drawer-user-info">
                <div class="drawer-user-avatar">👤</div>
                <div>
                    <div class="drawer-user-name">SafeSphere User</div>
                    <div class="drawer-user-email">user@safesphere.local</div>
                </div>
            </div>
        </div>
        
        <div class="drawer-nav">
            <div class="drawer-nav-item active" data-screen="dashboard">
                <div class="drawer-nav-item-icon">🏠</div>
                <div class="drawer-nav-item-text">Dashboard</div>
            </div>
            <div class="drawer-nav-item" data-screen="passwords">
                <div class="drawer-nav-item-icon">🗝️</div>
                <div class="drawer-nav-item-text">Password Manager</div>
            </div>
            <div class="drawer-nav-item" data-screen="health">
                <div class="drawer-nav-item-icon">🏥</div>
                <div class="drawer-nav-item-text">Password Health</div>
            </div>
            <div class="drawer-nav-item" data-screen="generator">
                <div class="drawer-nav-item-icon">🎲</div>
                <div class="drawer-nav-item-text">Password Generator</div>
            </div>
            
            <div class="drawer-divider"></div>
            
            <div class="drawer-nav-item" data-screen="ai-predictor">
                <div class="drawer-nav-item-icon">🤖</div>
                <div class="drawer-nav-item-text">AI Security Predictor</div>
            </div>
            <div class="drawer-nav-item" data-screen="vault">
                <div class="drawer-nav-item-icon">🔐</div>
                <div class="drawer-nav-item-text">Privacy Vault</div>
            </div>
            <div class="drawer-nav-item" data-screen="ai-chat">
                <div class="drawer-nav-item-icon">💬</div>
                <div class="drawer-nav-item-text">AI Chat</div>
            </div>
            
            <div class="drawer-divider"></div>
            
            <div class="drawer-nav-item" data-screen="settings">
                <div class="drawer-nav-item-icon">⚙️</div>
                <div class="drawer-nav-item-text">Settings</div>
            </div>
        </div>
    </div>
    
    <!-- Top Bar -->
    <div class="top-bar">
        <div class="top-bar-gradient-bg"></div>
        <button class="menu-btn" id="menuBtn">☰</button>
        <div class="top-bar-title" id="topBarTitle">SafeSphere</div>
        <button class="notif-btn" id="notifBtn">🔔</button>
    </div>
    
    <!-- Main Content -->
    <div class="main-content" id="mainContent">
        <!-- Dashboard Screen (default) -->
        <div id="dashboardScreen">
            <!-- Security Score Card -->
            <div class="glass-card security-score-card">
                <div style="font-size: 16px; color: var(--text-secondary); margin-bottom: 12px;">Security Score</div>
                <div class="circular-progress">
                    <svg width="120" height="120">
                        <circle class="progress-ring-bg" cx="60" cy="60" r="52"></circle>
                        <circle class="progress-ring" cx="60" cy="60" r="52" 
                                stroke-dasharray="326.73" stroke-dashoffset="65.35"></circle>
                    </svg>
                    <div class="progress-text">80</div>
                </div>
                <div style="font-size: 14px; color: var(--text-secondary);">
                    <span id="encryptedCount">0</span> items encrypted
                </div>
            </div>
            
            <!-- Quick Access Section -->
            <h2 style="font-size: 20px; font-weight: bold; margin-bottom: 16px;">Quick Access</h2>
            
            <div class="dashboard-grid">
                <div class="glass-card dashboard-card" onclick="navigateTo('passwords')">
                    <div class="dashboard-card-header">
                        <div class="dashboard-card-icon">🗝️</div>
                        <div class="dashboard-card-indicator" style="background: var(--cyan-glow);"></div>
                    </div>
                    <div class="dashboard-card-footer">
                        <div class="dashboard-card-title">Password Manager</div>
                        <div class="dashboard-card-desc"><span id="passwordCount">0</span> passwords</div>
                    </div>
                </div>
                
                <div class="glass-card dashboard-card" onclick="navigateTo('ai-predictor')">
                    <div class="dashboard-card-header">
                        <div class="dashboard-card-icon">🤖</div>
                        <div class="dashboard-card-indicator" style="background: var(--purple);"></div>
                    </div>
                    <div class="dashboard-card-footer">
                        <div class="dashboard-card-title">AI Security Predictor</div>
                        <div class="dashboard-card-desc">Predict future risks</div>
                    </div>
                </div>
                
                <div class="glass-card dashboard-card" onclick="navigateTo('vault')">
                    <div class="dashboard-card-header">
                        <div class="dashboard-card-icon">🔐</div>
                        <div class="dashboard-card-indicator" style="background: var(--cyan-light);"></div>
                    </div>
                    <div class="dashboard-card-footer">
                        <div class="dashboard-card-title">Privacy Vault</div>
                        <div class="dashboard-card-desc">Encrypted storage</div>
                    </div>
                </div>
                
                <div class="glass-card dashboard-card" onclick="navigateTo('ai-chat')">
                    <div class="dashboard-card-header">
                        <div class="dashboard-card-icon">💬</div>
                        <div class="dashboard-card-indicator" style="background: var(--purple);"></div>
                    </div>
                    <div class="dashboard-card-footer">
                        <div class="dashboard-card-title">AI Chat</div>
                        <div class="dashboard-card-desc">Offline advisor</div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Password Manager Screen -->
        <div id="passwordsScreen" class="hidden">
            <div class="password-header">
                <div class="password-count"><span id="passwordCount2">0</span> Passwords</div>
                <button class="add-btn" onclick="showAddPasswordModal()">
                    <span>+</span> Add Password
                </button>
            </div>
            
            <div id="passwordList">
                <!-- Will be populated by JavaScript -->
            </div>
            
            <div id="emptyPasswordState" class="empty-state hidden">
                <div class="empty-state-icon">🔐</div>
                <div class="empty-state-title">No Passwords Yet</div>
                <div class="empty-state-desc">Start securing your accounts by adding your first password</div>
                <button class="primary-btn" onclick="showAddPasswordModal()">Add First Password</button>
            </div>
        </div>
        
        <!-- Password Generator Screen -->
        <div id="generatorScreen" class="hidden">
            <div class="glass-card">
                <h2 style="font-size: 22px; font-weight: bold; margin-bottom: 24px;">Password Generator</h2>
                
                <div class="input-group">
                    <label class="input-label">Generated Password</label>
                    <div style="display: flex; gap: 12px;">
                        <input type="text" id="generatedPassword" class="input-field" readonly 
                               style="font-family: monospace; font-size: 18px;">
                        <button class="primary-btn" onclick="copyPassword()">📋</button>
                    </div>
                </div>
                
                <div class="input-group">
                    <label class="input-label">Length: <span id="lengthValue">16</span></label>
                    <input type="range" id="lengthSlider" min="8" max="32" value="16" 
                           style="width: 100%; accent-color: var(--cyan-glow);"
                           oninput="document.getElementById('lengthValue').textContent = this.value; generatePassword()">
                </div>
                
                <div class="input-group">
                    <label style="display: flex; align-items: center; gap: 12px; cursor: pointer;">
                        <input type="checkbox" id="includeNumbers" checked onchange="generatePassword()"
                               style="width: 20px; height: 20px; accent-color: var(--cyan-glow);">
                        <span class="input-label" style="margin: 0;">Include Numbers</span>
                    </label>
                </div>
                
                <div class="input-group">
                    <label style="display: flex; align-items: center; gap: 12px; cursor: pointer;">
                        <input type="checkbox" id="includeSymbols" checked onchange="generatePassword()"
                               style="width: 20px; height: 20px; accent-color: var(--cyan-glow);">
                        <span class="input-label" style="margin: 0;">Include Symbols</span>
                    </label>
                </div>
                
                <button class="primary-btn" onclick="generatePassword()" style="width: 100%; justify-content: center;">
                    🔄 Generate New
                </button>
            </div>
        </div>
        
        <!-- Password Health Screen -->
        <div id="healthScreen" class="hidden">
            <div class="glass-card">
                <h2 style="font-size: 22px; font-weight: bold; margin-bottom: 24px;">Password Health</h2>
                
                <div class="security-score-card" style="padding: 20px;">
                    <div class="circular-progress" style="margin: 0 auto 16px;">
                        <svg width="120" height="120">
                            <circle class="progress-ring-bg" cx="60" cy="60" r="52"></circle>
                            <circle id="healthProgressRing" class="progress-ring" cx="60" cy="60" r="52" 
                                    stroke-dasharray="326.73" stroke-dashoffset="0"></circle>
                        </svg>
                        <div class="progress-text" id="healthScore">100</div>
                    </div>
                    <div style="font-size: 16px; font-weight: 600; color: var(--text-primary);" id="healthStatus">
                        All passwords are secure!
                    </div>
                </div>
                
                <div class="mt-24">
                    <h3 style="font-size: 18px; font-weight: bold; margin-bottom: 16px;">Statistics</h3>
                    <div style="display: grid; gap: 12px;">
                        <div class="glass-card" style="padding: 16px;">
                            <div style="display: flex; justify-content: space-between;">
                                <span style="color: var(--text-secondary);">Strong Passwords</span>
                                <span style="font-weight: bold;" id="strongCount">0</span>
                            </div>
                        </div>
                        <div class="glass-card" style="padding: 16px;">
                            <div style="display: flex; justify-content: space-between;">
                                <span style="color: var(--text-secondary);">Weak Passwords</span>
                                <span style="font-weight: bold; color: var(--error);" id="weakCount">0</span>
                            </div>
                        </div>
                        <div class="glass-card" style="padding: 16px;">
                            <div style="display: flex; justify-content: space-between;">
                                <span style="color: var(--text-secondary);">Duplicates</span>
                                <span style="font-weight: bold; color: var(--warning);" id="duplicateCount">0</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Settings Screen -->
        <div id="settingsScreen" class="hidden">
            <div class="glass-card">
                <h2 style="font-size: 22px; font-weight: bold; margin-bottom: 24px;">Settings</h2>
                
                <div style="display: flex; flex-direction: column; gap: 16px;">
                    <div class="glass-card" style="padding: 16px; display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <div style="font-weight: 600;">Dark Mode</div>
                            <div style="font-size: 14px; color: var(--text-secondary);">Always enabled</div>
                        </div>
                        <div style="font-size: 24px;">🌙</div>
                    </div>
                    
                    <div class="glass-card" style="padding: 16px; display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <div style="font-weight: 600;">Encryption</div>
                            <div style="font-size: 14px; color: var(--text-secondary);">AES-256 Enabled</div>
                        </div>
                        <div style="font-size: 24px;">🔒</div>
                    </div>
                    
                    <div class="glass-card" style="padding: 16px; display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <div style="font-weight: 600;">Offline Mode</div>
                            <div style="font-size: 14px; color: var(--text-secondary);">100% Local</div>
                        </div>
                        <div style="font-size: 24px;">📡</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <!-- Add Password Modal -->
    <div class="modal-overlay" id="addPasswordModal">
        <div class="modal">
            <h2 class="modal-header">Add Password</h2>
            
            <div class="input-group input-with-autofill" style="position:relative;">
                <label class="input-label">Website/App</label>
                <input type="text" id="passwordSite" class="input-field" placeholder="example.com" autocomplete="off"
                       oninput="showAutofillDropdown(this.value)">
                <!-- Autofill Dropdown -->
                <div class="autofill-dropdown" id="autofillDropdown">
                    <!-- Entries populated via JavaScript -->
                </div>
            </div>
            
            <div class="input-group">
                <label class="input-label">Username/Email</label>
                <input type="text" id="passwordUsername" class="input-field" placeholder="user@example.com">
            </div>
            
            <div class="input-group">
                <label class="input-label">Password</label>
                <input type="password" id="passwordValue" class="input-field" placeholder="Enter password">
            </div>
            
            <div class="modal-footer">
                <button class="btn-secondary" onclick="closeAddPasswordModal()">Cancel</button>
                <button class="primary-btn" onclick="savePassword()" style="flex: 1; justify-content: center;">Save</button>
            </div>
        </div>
    </div>
    
    <script>
        // =========================
        // STATE MANAGEMENT (API-BASED)
        // =========================
        let passwords = [];
        let currentScreen = 'dashboard';
        let lastSyncHash = '';
        let syncInterval = null;
        
        // =========================
        // API CALLS
        // =========================
        async function fetchPasswords() {
            try {
                const response = await fetch('/api/passwords');
                if (response.ok) {
                    const data = await response.json();
                    passwords = data.map(p => ({
                        id: p.id,
                        site: p.service,
                        username: p.username,
                        password: '••••••••', // encrypted
                        encryptedPassword: p.encryptedPassword || '',
                        url: p.url || '',
                        category: p.category || 'WEB',
                        strengthScore: p.strengthScore || 0,
                        isFavorite: p.isFavorite || false,
                        createdAt: p.createdAt || Date.now(),
                        modifiedAt: p.modifiedAt || Date.now()
                    }));
                    updateUI();
                    return true;
                }
                console.error('Failed to fetch passwords:', response.status);
                return false;
            } catch (e) {
                console.error('Error fetching passwords:', e);
                return false;
            }
        }
        
        async function addPasswordToAndroid(site, username, password, url = '', notes = '', category = 'WEB') {
            try {
                const response = await fetch('/api/passwords', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({
                        service: site,
                        username: username,
                        password: password,
                        url: url,
                        notes: notes,
                        category: category
                    })
                });
                
                if (response.ok) {
                    await fetchPasswords(); // Refresh list
                    return true;
                }
                console.error('Failed to add password:', response.status);
                return false;
            } catch (e) {
                console.error('Error adding password:', e);
                return false;
            }
        }
        
        async function deletePasswordFromAndroid(id) {
            try {
                const response = await fetch('/api/passwords/' + id, {
                    method: 'DELETE'
                });
                
                if (response.ok) {
                    await fetchPasswords(); // Refresh list
                    return true;
                }
                console.error('Failed to delete password:', response.status);
                return false;
            } catch (e) {
                console.error('Error deleting password:', e);
                return false;
            }
        }
        
        async function decryptPassword(id) {
            try {
                const response = await fetch('/api/passwords/' + id + '/decrypt');
                if (response.ok) {
                    const data = await response.json();
                    return data.password;
                }
                console.error('Failed to decrypt password:', response.status);
                return null;
            } catch (e) {
                console.error('Error decrypting password:', e);
                return null;
            }
        }
        
        async function generatePasswordFromAPI(length = 16, includeNumbers = true, includeSymbols = true) {
            try {
                const response = await fetch('/api/generate', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({
                        length: length,
                        includeNumbers: includeNumbers,
                        includeSymbols: includeSymbols
                    })
                });
                
                if (response.ok) {
                    const data = await response.json();
                    return data.password;
                }
                console.error('Failed to generate password:', response.status);
                return null;
            } catch (e) {
                console.error('Error generating password:', e);
                return null;
            }
        }
        
        async function checkForSync() {
            try {
                const response = await fetch('/api/sync');
                if (response.ok) {
                    const data = await response.json();
                    if (data.changed && data.hash !== lastSyncHash) {
                        console.log('📥 Data changed on Android, syncing...');
                        lastSyncHash = data.hash;
                        await fetchPasswords();
                    }
                    return true;
                }
                return false;
            } catch (e) {
                console.error('Sync check failed:', e);
                return false;
            }
        }
        
        // =========================
        // AUTOFILL FUNCTIONS
        // =========================
        let autofillSuggestions = [];
        let selectedAutofillIndex = -1;
        
        async function showAutofillDropdown(query) {
            const dropdown = document.getElementById('autofillDropdown');
            
            if (!query || query.length < 2) {
                dropdown.classList.remove('show');
                return;
            }
            
            // Search for matching passwords via API
            try {
                const response = await fetch('/api/autofill?url=' + encodeURIComponent(query));
                if (response.ok) {
                    autofillSuggestions = await response.json();
                    
                    if (autofillSuggestions.length > 0) {
                        renderAutofillDropdown();
                        dropdown.classList.add('show');
                    } else {
                        dropdown.classList.remove('show');
                    }
                } else {
                    dropdown.classList.remove('show');
                }
            } catch (e) {
                console.error('Autofill error:', e);
                dropdown.classList.remove('show');
            }
        }
        
        function renderAutofillDropdown() {
            const dropdown = document.getElementById('autofillDropdown');
            dropdown.innerHTML = autofillSuggestions.map((item, index) => 
                '<div class="autofill-item" onclick="selectAutofillItem(' + index + ')">' +
                '<div class="autofill-item-title">🔐 ' + escapeHtml(item.service) + '</div>' +
                '<div class="autofill-item-username">' + escapeHtml(item.username) + '</div>' +
                '</div>'
            ).join('');
        }
        
        async function selectAutofillItem(index) {
            const item = autofillSuggestions[index];
            if (!item) return;
            
            // Fill in the form
            document.getElementById('passwordSite').value = item.service;
            document.getElementById('passwordUsername').value = item.username;
            
            // Decrypt and fill password
            const decrypted = await decryptPassword(item.id);
            if (decrypted) {
                document.getElementById('passwordValue').value = decrypted;
            }
            
            // Hide dropdown
            document.getElementById('autofillDropdown').classList.remove('show');
            
            // Show visual feedback
            alert('✅ Autofilled from SafeSphere!');
        }
        
        // Close autofill dropdown when clicking outside
        document.addEventListener('click', function(e) {
            const dropdown = document.getElementById('autofillDropdown');
            const siteInput = document.getElementById('passwordSite');
            
            if (dropdown && siteInput && !dropdown.contains(e.target) && e.target !== siteInput) {
                dropdown.classList.remove('show');
            }
        });
        
        // =========================
        // AUTO-SAVE FUNCTIONS
        // =========================
        let pendingAutoSave = null;
        
        async function checkAutoSavePrompt() {
            try {
                const response = await fetch('/api/save-prompt');
                if (response.ok) {
                    const data = await response.json();
                    if (data.pending) {
                        showAutoSaveBanner(data.service, data.username);
                    }
                }
            } catch (e) {
                console.error('Auto-save check error:', e);
            }
        }
        
        function showAutoSaveBanner(service, username) {
            const banner = document.getElementById('autoSaveBanner');
            const serviceEl = document.getElementById('autoSaveService');
            const messageEl = document.getElementById('autoSaveMessage');
            
            pendingAutoSave = { service, username };
            
            serviceEl.textContent = service;
            messageEl.innerHTML = 'Save password for <strong>' + escapeHtml(username) + '</strong> on <strong>' + escapeHtml(service) + '</strong>?';
            banner.classList.add('show');
            
            // Auto-hide after 10 seconds
            setTimeout(() => {
                if (banner.classList.contains('show')) {
                    dismissAutoSave();
                }
            }, 10000);
        }
        
        async function acceptAutoSave() {
            const banner = document.getElementById('autoSaveBanner');
            
            if (!pendingAutoSave) {
                banner.classList.remove('show');
                return;
            }
            
            try {
                const response = await fetch('/api/save-prompt/accept', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({ category: 'WEB' })
                });
                
                if (response.ok) {
                    banner.classList.remove('show');
                    pendingAutoSave = null;
                    
                    // Refresh password list
                    await fetchPasswords();
                    
                    // Show success notification
                    showNotification('✅ Password saved to SafeSphere!', 'success');
                } else {
                    showNotification('❌ Failed to save password', 'error');
                }
            } catch (e) {
                console.error('Accept auto-save error:', e);
                showNotification('❌ Error saving password', 'error');
            }
        }
        
        async function dismissAutoSave() {
            const banner = document.getElementById('autoSaveBanner');
            banner.classList.remove('show');
            pendingAutoSave = null;
            
            try {
                await fetch('/api/save-prompt', { method: 'DELETE' });
            } catch (e) {
                console.error('Dismiss auto-save error:', e);
            }
        }
        
        function showNotification(message, type = 'info') {
            // Simple alert for now - could be enhanced with toast notifications
            alert(message);
        }
        
        // Start real-time sync polling (every 3 seconds)
        function startSync() {
            if (syncInterval) return;
            syncInterval = setInterval(() => {
                checkForSync();
                checkAutoSavePrompt(); // Also check for auto-save prompts
            }, 3000);
            console.log('🔄 Real-time sync started');
        }
        
        function stopSync() {
            if (syncInterval) {
                clearInterval(syncInterval);
                syncInterval = null;
                console.log('⏸️ Real-time sync stopped');
            }
        }
        
        // Load data from Android
        async function loadData() {
            console.log('📡 Loading data from Android...');
            const success = await fetchPasswords();
            if (success) {
                console.log('✅ Loaded ' + passwords.length + ' passwords from Android');
                startSync(); // Start real-time polling
                checkAutoSavePrompt(); // Check for pending auto-save prompts
            } else {
                console.error('❌ Failed to load data from Android');
            }
        }
        
        // =========================
        // NAVIGATION
        // =========================
        function navigateTo(screen) {
            currentScreen = screen;
            
            // Hide all screens
            document.querySelectorAll('[id$="Screen"]').forEach(el => el.classList.add('hidden'));
            
            // Show current screen
            const screenElement = document.getElementById(screen + 'Screen');
            if (screenElement) {
                screenElement.classList.remove('hidden');
            }
            
            // Update drawer active state
            document.querySelectorAll('.drawer-nav-item').forEach(item => {
                item.classList.remove('active');
                if (item.dataset.screen === screen) {
                    item.classList.add('active');
                }
            });
            
            // Update top bar title
            const titles = {
                'dashboard': 'SafeSphere',
                'passwords': 'Password Manager',
                'generator': 'Password Generator',
                'health': 'Password Health',
                'settings': 'Settings',
                'ai-predictor': 'AI Security Predictor',
                'vault': 'Privacy Vault',
                'ai-chat': 'AI Chat'
            };
            document.getElementById('topBarTitle').textContent = titles[screen] || 'SafeSphere';
            
            // Close drawer
            closeDrawer();
            
            // Update screen-specific content
            if (screen === 'passwords') {
                renderPasswordList();
            } else if (screen === 'health') {
                updateHealthScreen();
            } else if (screen === 'generator') {
                generatePassword();
            }
        }
        
        // =========================
        // DRAWER MANAGEMENT
        // =========================
        function toggleDrawer() {
            const drawer = document.getElementById('navDrawer');
            const overlay = document.getElementById('drawerOverlay');
            drawer.classList.toggle('open');
            overlay.classList.toggle('show');
        }
        
        function closeDrawer() {
            const drawer = document.getElementById('navDrawer');
            const overlay = document.getElementById('drawerOverlay');
            drawer.classList.remove('open');
            overlay.classList.remove('show');
        }
        
        // =========================
        // PASSWORD MANAGER
        // =========================
        function renderPasswordList() {
            const listEl = document.getElementById('passwordList');
            const emptyState = document.getElementById('emptyPasswordState');
            
            if (passwords.length === 0) {
                listEl.innerHTML = '';
                emptyState.classList.remove('hidden');
                return;
            }
            
            emptyState.classList.add('hidden');
            
            listEl.innerHTML = passwords.map((p, index) => {
                const strength = p.strengthScore || calculatePasswordStrength('••••••••');
                const strengthClass = strength >= 80 ? 'strength-strong' : 
                                    strength >= 50 ? 'strength-medium' : 'strength-weak';
                const strengthText = strength >= 80 ? 'Strong' : 
                                   strength >= 50 ? 'Medium' : 'Weak';
                
                return '<div class="glass-card password-item" onclick="viewPasswordDetails(\'' + p.id + '\')">' +
                    '<div class="password-item-header">' +
                    '<div class="password-item-icon">🔐</div>' +
                    '<div class="password-item-info">' +
                    '<div class="password-item-title">' + escapeHtml(p.site) + '</div>' +
                    '<div class="password-item-username">' + escapeHtml(p.username) + '</div>' +
                    '</div>' +
                    '<div class="password-strength-badge ' + strengthClass + '">' + strengthText + '</div>' +
                    '</div>' +
                    '</div>';
            }).join('');
        }
        
        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }
        
        async function viewPasswordDetails(id) {
            const pwd = passwords.find(p => p.id === id);
            if (!pwd) return;
            
            const decrypted = await decryptPassword(id);
            if (decrypted) {
                alert('Site: ' + pwd.site + '\nUsername: ' + pwd.username + '\nPassword: ' + decrypted);
            } else {
                alert('Failed to decrypt password');
            }
        }
        
        function showAddPasswordModal() {
            document.getElementById('addPasswordModal').classList.add('show');
        }
        
        function closeAddPasswordModal() {
            document.getElementById('addPasswordModal').classList.remove('show');
            document.getElementById('passwordSite').value = '';
            document.getElementById('passwordUsername').value = '';
            document.getElementById('passwordValue').value = '';
        }
        
        async function savePassword() {
            const site = document.getElementById('passwordSite').value;
            const username = document.getElementById('passwordUsername').value;
            const password = document.getElementById('passwordValue').value;
            
            if (!site || !username || !password) {
                alert('Please fill all fields');
                return;
            }
            
            // Show loading indicator
            const btn = event.target;
            const originalText = btn.textContent;
            btn.textContent = '💾 Saving...';
            btn.disabled = true;
            
            const success = await addPasswordToAndroid(site, username, password);
            
            btn.textContent = originalText;
            btn.disabled = false;
            
            if (success) {
                closeAddPasswordModal();
                
                if (currentScreen === 'passwords') {
                    renderPasswordList();
                }
                
                // Show success message
                alert('✅ Password saved to Android!');
            } else {
                alert('❌ Failed to save password');
            }
        }
        
        function calculatePasswordStrength(password) {
            let strength = 0;
            if (password.length >= 8) strength += 20;
            if (password.length >= 12) strength += 20;
            if (password.length >= 16) strength += 10;
            if (/[a-z]/.test(password)) strength += 10;
            if (/[A-Z]/.test(password)) strength += 15;
            if (/[0-9]/.test(password)) strength += 15;
            if (/[^a-zA-Z0-9]/.test(password)) strength += 10;
            return Math.min(100, strength);
        }
        
        // =========================
        // PASSWORD GENERATOR
        // =========================
        async function generatePassword() {
            const length = parseInt(document.getElementById('lengthSlider').value);
            const includeNumbers = document.getElementById('includeNumbers').checked;
            const includeSymbols = document.getElementById('includeSymbols').checked;
            
            // Try API first
            const password = await generatePasswordFromAPI(length, includeNumbers, includeSymbols);
            
            if (password) {
                document.getElementById('generatedPassword').value = password;
            } else {
                // Fallback to client-side generation
                let chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
                if (includeNumbers) chars += '0123456789';
                if (includeSymbols) chars += '!@#\$%^&*()_+-=[]{}|;:,.<>?';
                
                let pwd = '';
                for (let i = 0; i < length; i++) {
                    pwd += chars.charAt(Math.floor(Math.random() * chars.length));
                }
                
                document.getElementById('generatedPassword').value = pwd;
            }
        }
        
        function copyPassword() {
            const passwordField = document.getElementById('generatedPassword');
            passwordField.select();
            document.execCommand('copy');
            
            const btn = event.target;
            const originalText = btn.textContent;
            btn.textContent = '✓ Copied!';
            setTimeout(() => {
                btn.textContent = originalText;
            }, 2000);
        }
        
        // =========================
        // PASSWORD HEALTH
        // =========================
        function updateHealthScreen() {
            if (passwords.length === 0) {
                document.getElementById('healthScore').textContent = '100';
                document.getElementById('healthStatus').textContent = 'No passwords to analyze';
                document.getElementById('strongCount').textContent = '0';
                document.getElementById('weakCount').textContent = '0';
                document.getElementById('duplicateCount').textContent = '0';
                return;
            }
            
            let strongCount = 0;
            let weakCount = 0;
            
            passwords.forEach(pwd => {
                const strength = pwd.strengthScore || 50;
                if (strength >= 80) strongCount++;
                else if (strength < 50) weakCount++;
            });
            
            // Note: duplicate detection requires decrypted passwords
            // For now, just show 0
            const duplicateCount = 0;
            
            // Calculate overall health score
            const healthScore = Math.round(
                (strongCount / passwords.length * 60) +
                ((passwords.length - weakCount) / passwords.length * 30) +
                ((passwords.length - duplicateCount) / passwords.length * 10)
            );
            
            document.getElementById('healthScore').textContent = healthScore;
            document.getElementById('strongCount').textContent = strongCount;
            document.getElementById('weakCount').textContent = weakCount;
            document.getElementById('duplicateCount').textContent = duplicateCount;
            
            // Update status
            let status = 'All passwords are secure!';
            if (healthScore < 60) status = 'Critical issues found!';
            else if (healthScore < 80) status = 'Some issues need attention';
            document.getElementById('healthStatus').textContent = status;
            
            // Update progress ring
            const circumference = 2 * Math.PI * 52;
            const offset = circumference - (healthScore / 100 * circumference);
            const ring = document.getElementById('healthProgressRing');
            ring.style.strokeDashoffset = offset;
            ring.style.stroke = healthScore >= 80 ? 'var(--success)' : 
                              healthScore >= 60 ? 'var(--warning)' : 'var(--error)';
        }
        
        // =========================
        // UI UPDATES
        // =========================
        function updateUI() {
            // Update password counts
            document.getElementById('passwordCount').textContent = passwords.length;
            const count2 = document.getElementById('passwordCount2');
            if (count2) count2.textContent = passwords.length;
            
            // Update encrypted count (all passwords are encrypted)
            document.getElementById('encryptedCount').textContent = passwords.length;
            
            // Update security score based on real passwords
            let totalStrength = 0;
            passwords.forEach(p => {
                totalStrength += (p.strengthScore || 50);
            });
            const avgStrength = passwords.length > 0 ? Math.round(totalStrength / passwords.length) : 80;
            
            const circumference = 2 * Math.PI * 52;
            const offset = circumference - (avgStrength / 100 * circumference);
            document.querySelector('.progress-ring').style.strokeDashoffset = offset;
            document.querySelector('.progress-text').textContent = avgStrength;
            
            // Update current screen
            if (currentScreen === 'passwords') {
                renderPasswordList();
            } else if (currentScreen === 'health') {
                updateHealthScreen();
            }
        }
        
        // =========================
        // EVENT LISTENERS
        // =========================
        document.getElementById('menuBtn').addEventListener('click', toggleDrawer);
        document.getElementById('drawerOverlay').addEventListener('click', closeDrawer);
        document.getElementById('notifBtn').addEventListener('click', () => {
            alert('No new notifications');
        });
        
        // Navigation items
        document.querySelectorAll('.drawer-nav-item').forEach(item => {
            item.addEventListener('click', () => {
                const screen = item.dataset.screen;
                if (screen) navigateTo(screen);
            });
        });
        
        // Cleanup on page unload
        window.addEventListener('beforeunload', () => {
            stopSync();
        });
        
        // =========================
        // INITIALIZATION
        // =========================
        loadData();
        
        console.log('✅ SafeSphere Desktop Web App Loaded!');
        console.log('🔐 Syncing with Android app in real-time!');
        console.log('🎨 UI matches Android app perfectly!');
        console.log('📡 Polling for changes every 3 seconds...');
    </script>
</body>
</html>
        """.trimIndent()
    }
}
