package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.data.*
import com.runanywhere.startup_hackathon20.security.BiometricAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Login Screen - Beautiful Material Design 3 with Auto-fill support
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (User) -> Unit,
    onNavigateToRegister: () -> Unit,
    onLogin: suspend (LoginCredentials) -> AuthResult,
    onNavigateToDashboard: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var savedCredentials by remember { mutableStateOf<List<PasswordVaultEntry>>(emptyList()) }
    var showSaveCredentialsDialog by remember { mutableStateOf(false) }
    var loginCredentials by remember { mutableStateOf<Pair<String, String>?>(null) }
    var loggedInUser by remember { mutableStateOf<User?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Biometric state for auto-prompt
    var biometricAttempts by remember { mutableStateOf(0) }
    var showBiometricPrompt by remember { mutableStateOf(false) }
    var biometricExhausted by remember { mutableStateOf(false) }
    val maxBiometricAttempts = 5

    // Check if biometric should auto-trigger on app open
    val isBiometricEnabled = remember {
        BiometricAuthManager.isBiometricLoginEnabled(context)
    }

    // Auto-trigger biometric on first load if enabled and credentials exist
    LaunchedEffect(Unit) {
        if (isBiometricEnabled && activity != null && biometricAttempts == 0) {
            // Check if saved credentials exist
            val credentials = BiometricAuthManager.getSavedCredentials(context)
            if (credentials != null) {
                showBiometricPrompt = true
            }
        }
    }

    // Show biometric prompt
    LaunchedEffect(showBiometricPrompt, biometricAttempts) {
        if (showBiometricPrompt && activity != null && biometricAttempts < maxBiometricAttempts && !biometricExhausted) {
            showBiometricPrompt = false
            biometricAttempts++

            BiometricAuthManager.authenticate(
                activity = activity,
                title = "Unlock SafeSphere",
                subtitle = "Attempt $biometricAttempts of $maxBiometricAttempts",
                negativeButtonText = "Use Password",
                onSuccess = {
                    // Biometric success - login with saved credentials
                    coroutineScope.launch {
                        val credentials = BiometricAuthManager.getSavedCredentials(context)
                        if (credentials != null) {
                            val (savedEmail, savedPassword) = credentials
                            isLoading = true
                            val result = onLogin(LoginCredentials(savedEmail, savedPassword))
                            isLoading = false

                            when (result) {
                                is AuthResult.Success -> {
                                    onNavigateToDashboard()
                                }
                                is AuthResult.Error -> {
                                    errorMessage = result.message
                                    biometricExhausted = true
                                }
                            }
                        }
                    }
                },
                onError = { errorCode, errorString ->
                    if (errorCode == 10 || errorCode == 13) {
                        // User cancelled - allow manual login
                        biometricExhausted = true
                        errorMessage = "Biometric cancelled. Please use password."
                    } else if (biometricAttempts < maxBiometricAttempts) {
                        // Failed attempt - allow retry
                        errorMessage =
                            "Attempt $biometricAttempts/$maxBiometricAttempts failed. ${maxBiometricAttempts - biometricAttempts} attempts remaining."
                        // Auto-trigger next attempt after a brief delay
                        coroutineScope.launch {
                            delay(1500)
                            if (biometricAttempts < maxBiometricAttempts && !biometricExhausted) {
                                showBiometricPrompt = true
                            }
                        }
                    } else {
                        // Max attempts reached
                        errorMessage =
                            "Maximum biometric attempts reached. Please use password to login."
                        biometricExhausted = true
                    }
                },
                onFailed = {
                    if (biometricAttempts < maxBiometricAttempts) {
                        errorMessage =
                            "Attempt $biometricAttempts/$maxBiometricAttempts failed. ${maxBiometricAttempts - biometricAttempts} attempts remaining."
                        // Auto-trigger next attempt after a brief delay
                        coroutineScope.launch {
                            delay(1500)
                            if (biometricAttempts < maxBiometricAttempts && !biometricExhausted) {
                                showBiometricPrompt = true
                            }
                        }
                    } else {
                        errorMessage =
                            "Maximum biometric attempts reached. Please use password to login."
                        biometricExhausted = true
                    }
                }
            )
        }
    }

    // Load saved credentials on mount
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val passwordRepo = PasswordVaultRepository.getInstance(context)

                // Get current passwords (no collection, just a single read)
                val allPasswords = passwordRepo.passwords.value
                val filtered = allPasswords.filter {
                    it.service.contains("SafeSphere", ignoreCase = true) ||
                            it.url.contains("startup_hackathon20", ignoreCase = true)
                }

                withContext(Dispatchers.Main) {
                    savedCredentials = filtered
                }
            } catch (e: Exception) {
                // No saved credentials
            }
        }
    }

    // Save Credentials Dialog (after successful login)
    if (showSaveCredentialsDialog && loggedInUser != null && loginCredentials != null) {
        val savedEmail = loginCredentials!!.first
        val savedPassword = loginCredentials!!.second
        val user = loggedInUser!!

        SaveCredentialsDialog(
            email = savedEmail,
            password = savedPassword,
            onSave = {
                // Save credentials to vault
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val passwordRepo = PasswordVaultRepository.getInstance(context)

                        passwordRepo.savePassword(
                            service = "SafeSphere App",
                            username = savedEmail,
                            password = savedPassword,
                            url = "com.runanywhere.startup_hackathon20",
                            category = PasswordCategory.OTHER,
                            notes = "SafeSphere login credentials"
                        )

                        withContext(Dispatchers.Main) {
                            showSaveCredentialsDialog = false
                            onNavigateToDashboard()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showSaveCredentialsDialog = false
                            onNavigateToDashboard()
                        }
                    }
                }
            },
            onDismiss = {
                showSaveCredentialsDialog = false
                onNavigateToDashboard()
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SafeSphereColors.Background,
                        SafeSphereColors.BackgroundDark
                    )
                )
            )
    ) {
        // Animated background circles
        AnimatedBackgroundCircles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo and Title
            SafeSphereLogo()

            Spacer(modifier = Modifier.height(48.dp))

            // Login Card
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Welcome Back",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    Text(
                        text = "Sign in to continue to your secure vault",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Show saved credentials dropdown if available
                    if (savedCredentials.isNotEmpty()) {
                        SavedCredentialsDropdown(
                            savedCredentials = savedCredentials,
                            onCredentialSelected = { savedEntry ->
                                email = savedEntry.username
                                // Decrypt and fill password
                                coroutineScope.launch {
                                    try {
                                        val passwordRepo =
                                            PasswordVaultRepository.getInstance(context)
                                        val decrypted =
                                            passwordRepo.getDecryptedPassword(savedEntry.id)
                                                .getOrNull()
                                        decrypted?.let {
                                            password = it.password
                                            passwordVisible = true
                                        }
                                    } catch (e: Exception) {
                                        // Handle error
                                    }
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Email Field
                    AuthTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = "Email",
                        placeholder = "Enter your email",
                        leadingIcon = Icons.Filled.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    AuthTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = "Password",
                        placeholder = "Enter your password",
                        leadingIcon = Icons.Filled.Lock,
                        trailingIcon = if (passwordVisible) "👀" else "🔒",
                        onTrailingIconClick = { passwordVisible = !passwordVisible },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (email.isNotBlank() && password.isNotBlank()) {
                                    // Trigger login
                                }
                            }
                        )
                    )

                    // Error Message
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        errorMessage?.let { error ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SafeSphereColors.Error.copy(alpha = 0.1f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = SafeSphereColors.Error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    fontSize = 14.sp,
                                    color = SafeSphereColors.Error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Login Button
                    GlassButton(
                        text = if (isLoading) "Signing In..." else "Sign In",
                        onClick = {
                            if (!isLoading && email.isNotBlank() && password.isNotBlank()) {
                                isLoading = true
                                errorMessage = null

                                // Perform login
                                coroutineScope.launch {
                                    val result = onLogin(LoginCredentials(email, password))
                                    isLoading = false

                                    when (result) {
                                        is AuthResult.Success -> {
                                            // Check if biometric is available and not yet enabled
                                            val biometricAvailable =
                                                BiometricAuthManager.isBiometricAvailable(context)
                                            val biometricEnabled =
                                                BiometricAuthManager.isBiometricLoginEnabled(context)

                                            if (biometricAvailable is com.runanywhere.startup_hackathon20.security.BiometricAvailability.Available && !biometricEnabled) {
                                                // Save credentials for biometric login
                                                BiometricAuthManager.saveCredentialsForBiometric(
                                                    context,
                                                    email,
                                                    password
                                                )
                                            }

                                            // Check if credentials are already saved
                                            val alreadySaved = savedCredentials.any {
                                                it.username.equals(email, ignoreCase = true)
                                            }

                                            if (!alreadySaved) {
                                                // Show save credentials dialog
                                                loggedInUser = result.user
                                                loginCredentials = Pair(email, password)
                                                showSaveCredentialsDialog = true
                                            } else {
                                                // Credentials already saved, navigate to dashboard
                                                onNavigateToDashboard()
                                            }
                                        }

                                        is AuthResult.Error -> {
                                            errorMessage = result.message
                                        }
                                    }
                                }
                            } else {
                                errorMessage = "Please enter email and password"
                            }
                        },
                        primary = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Biometric Login Button
                    if (activity != null) {
                        OutlinedButton(
                            onClick = {
                                BiometricAuthManager.authenticate(
                                    activity = activity,
                                    title = "Login to SafeSphere",
                                    subtitle = "Use fingerprint or face to unlock",
                                    negativeButtonText = "Cancel",
                                    onSuccess = {
                                        // Biometric success - try to login with saved credentials
                                        coroutineScope.launch {
                                            val credentials =
                                                BiometricAuthManager.getSavedCredentials(context)
                                            if (credentials != null) {
                                                val (savedEmail, savedPassword) = credentials
                                                isLoading = true
                                                val result = onLogin(
                                                    LoginCredentials(
                                                        savedEmail,
                                                        savedPassword
                                                    )
                                                )
                                                isLoading = false

                                                when (result) {
                                                    is AuthResult.Success -> onNavigateToDashboard()
                                                    is AuthResult.Error -> errorMessage =
                                                        result.message
                                                }
                                            } else {
                                                errorMessage =
                                                    "Please login with password first to enable biometric"
                                            }
                                        }
                                    },
                                    onError = { errorCode: Int, errString: String ->
                                        errorMessage = "Biometric error: $errString"
                                    },
                                    onFailed = {
                                        errorMessage = "Biometric authentication failed"
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SafeSphereColors.Primary
                            ),
                            border = BorderStroke(1.dp, SafeSphereColors.Primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "👍",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Login with Fingerprint")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Register Link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account?",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sign Up",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.Primary,
                    modifier = Modifier.clickable(onClick = onNavigateToRegister)
                )
            }
        }
    }
}

/**
 * Saved Credentials Dropdown
 */
@Composable
fun SavedCredentialsDropdown(
    savedCredentials: List<PasswordVaultEntry>,
    onCredentialSelected: (PasswordVaultEntry) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = "Saved Credentials",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = SafeSphereColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SafeSphereColors.Primary
                ),
                border = BorderStroke(1.dp, SafeSphereColors.Primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔐", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tap to auto-fill saved credentials")
                    }
                    Text(text = "▼", fontSize = 12.sp)
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(SafeSphereColors.Surface)
            ) {
                savedCredentials.forEach { credential ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = credential.service,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SafeSphereColors.TextPrimary
                                    )
                                    Text(
                                        text = credential.username,
                                        fontSize = 14.sp,
                                        color = SafeSphereColors.TextSecondary
                                    )
                                }
                                Text(
                                    text = "🔐",
                                    fontSize = 20.sp
                                )
                            }
                        },
                        onClick = {
                            onCredentialSelected(credential)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Register Screen - Beautiful Material Design 3
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: (User) -> Unit,
    onNavigateToLogin: () -> Unit,
    onRegister: suspend (RegistrationData) -> AuthResult,
    onNavigateToOnboarding: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSaveCredentialsDialog by remember { mutableStateOf(false) }
    var registeredUser by remember { mutableStateOf<User?>(null) }
    var registrationCredentials by remember { mutableStateOf<Pair<String, String>?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Save Credentials Dialog
    if (showSaveCredentialsDialog && registeredUser != null && registrationCredentials != null) {
        val savedEmail = registrationCredentials!!.first
        val savedPassword = registrationCredentials!!.second
        val user = registeredUser!!

        SaveCredentialsDialog(
            email = savedEmail,
            password = savedPassword,
            onSave = {
                // Save credentials to vault
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val passwordRepo = PasswordVaultRepository.getInstance(context)
                        
                        passwordRepo.savePassword(
                            service = "SafeSphere App",
                            username = savedEmail,
                            password = savedPassword,
                            url = "com.runanywhere.startup_hackathon20",
                            category = PasswordCategory.OTHER,
                            notes = "SafeSphere login credentials"
                        )
                        
                        withContext(Dispatchers.Main) {
                            showSaveCredentialsDialog = false
                            // Navigate to onboarding after saving
                            onNavigateToOnboarding()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showSaveCredentialsDialog = false
                            // Still navigate even if save failed
                            onNavigateToOnboarding()
                        }
                    }
                }
            },
            onDismiss = {
                showSaveCredentialsDialog = false
                // Navigate to onboarding even if user skipped saving
                onNavigateToOnboarding()
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SafeSphereColors.Background,
                        SafeSphereColors.BackgroundDark
                    )
                )
            )
    ) {
        // Animated background circles
        AnimatedBackgroundCircles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo
            SafeSphereLogo(small = true)

            Spacer(modifier = Modifier.height(32.dp))

            // Register Card
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Create Account",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    Text(
                        text = "Join SafeSphere for complete privacy",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Name Field
                    AuthTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        label = "Full Name",
                        placeholder = "Enter your name",
                        leadingIcon = Icons.Filled.Person,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Field
                    AuthTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = "Email",
                        placeholder = "Enter your email",
                        leadingIcon = Icons.Filled.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    AuthTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = "Password",
                        placeholder = "Create a strong password",
                        leadingIcon = Icons.Filled.Lock,
                        trailingIcon = if (passwordVisible) "👀" else "🔒",
                        onTrailingIconClick = { passwordVisible = !passwordVisible },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    // Password Strength Indicator
                    if (password.isNotEmpty()) {
                        PasswordStrengthIndicator(password)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password Field
                    AuthTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = "Confirm Password",
                        placeholder = "Re-enter your password",
                        leadingIcon = Icons.Filled.Lock,
                        trailingIcon = if (confirmPasswordVisible) "👀" else "🔒",
                        onTrailingIconClick = { confirmPasswordVisible = !confirmPasswordVisible },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )

                    // Error Message
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        errorMessage?.let { error ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SafeSphereColors.Error.copy(alpha = 0.1f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = SafeSphereColors.Error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    fontSize = 14.sp,
                                    color = SafeSphereColors.Error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Register Button
                    GlassButton(
                        text = if (isLoading) "Creating Account..." else "Create Account",
                        onClick = {
                            if (!isLoading && name.isNotBlank() && email.isNotBlank() &&
                                password.isNotBlank() && confirmPassword.isNotBlank()
                            ) {

                                if (password != confirmPassword) {
                                    errorMessage = "Passwords do not match"
                                    return@GlassButton
                                }

                                isLoading = true
                                errorMessage = null

                                // Perform registration
                                coroutineScope.launch {
                                    val result = onRegister(
                                        RegistrationData(
                                            name = name,
                                            email = email,
                                            password = password,
                                            confirmPassword = confirmPassword
                                        )
                                    )
                                    isLoading = false

                                    when (result) {
                                        is AuthResult.Success -> {
                                            // Show save credentials dialog
                                            registeredUser = result.user
                                            registrationCredentials = Pair(email, password)
                                            showSaveCredentialsDialog = true
                                        }

                                        is AuthResult.Error -> {
                                            errorMessage = result.message
                                        }
                                    }
                                }
                            } else {
                                errorMessage = "Please fill all fields"
                            }
                        },
                        primary = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sign In",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.Primary,
                    modifier = Modifier.clickable(onClick = onNavigateToLogin)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * Save Credentials Dialog - Shown after registration
 */
@Composable
fun SaveCredentialsDialog(
    email: String,
    password: String,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SafeSphereColors.Surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔐",
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = "Save to SafeSphere?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Would you like to save your login credentials in SafeSphere Vault?",
                    fontSize = 16.sp,
                    color = SafeSphereColors.TextPrimary,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Show credentials (masked)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = SafeSphereColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = email,
                                fontSize = 14.sp,
                                color = SafeSphereColors.TextPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = SafeSphereColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "••••••••",
                                fontSize = 14.sp,
                                color = SafeSphereColors.TextPrimary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "✅ Encrypted with AES-256\n✅ Stored locally on device\n✅ Auto-fill on next login",
                    fontSize = 13.sp,
                    color = SafeSphereColors.Success,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            GlassButton(
                text = "💾 Save",
                onClick = onSave,
                primary = true
            )
        },
        dismissButton = {
            GlassButton(
                text = "Not Now",
                onClick = onDismiss,
                primary = false
            )
        }
    )
}

/**
 * SafeSphere Logo Component
 */
@Composable
fun SafeSphereLogo(small: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Shield Icon
        Box(
            modifier = Modifier
                .size(if (small) 60.dp else 80.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            SafeSphereColors.Primary,
                            SafeSphereColors.Secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🛡️",
                fontSize = if (small) 32.sp else 40.sp
            )
        }

        Spacer(modifier = Modifier.height(if (small) 8.dp else 16.dp))

        Text(
            text = "SafeSphere",
            fontSize = if (small) 24.sp else 32.sp,
            fontWeight = FontWeight.Bold,
            color = SafeSphereColors.TextPrimary
        )

        if (!small) {
            Text(
                text = "Your Privacy. Your Control.",
                fontSize = 14.sp,
                color = SafeSphereColors.TextSecondary
            )
        }
    }
}

/**
 * Animated Background Circles
 */
@Composable
fun AnimatedBackgroundCircles() {
    var animate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animate = true
        while (true) {
            delay(3000)
            animate = !animate
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Circle 1
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = 100.dp)
                .size(300.dp)
                .blur(100.dp)
                .background(
                    SafeSphereColors.Primary.copy(alpha = 0.1f),
                    CircleShape
                )
        )

        // Circle 2
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-50).dp)
                .size(250.dp)
                .blur(80.dp)
                .background(
                    SafeSphereColors.Secondary.copy(alpha = 0.1f),
                    CircleShape
                )
        )

        // Circle 3
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 50.dp, y = 150.dp)
                .size(200.dp)
                .blur(70.dp)
                .background(
                    SafeSphereColors.Accent.copy(alpha = 0.08f),
                    CircleShape
                )
        )
    }
}

/**
 * Custom Auth Text Field
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: String? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = SafeSphereColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = SafeSphereColors.TextSecondary.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = SafeSphereColors.Primary
                )
            },
            trailingIcon = trailingIcon?.let {
                {
                    IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                        Text(text = it, fontSize = 20.sp)
                    }
                }
            },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SafeSphereColors.Primary,
                unfocusedBorderColor = SafeSphereColors.TextSecondary.copy(alpha = 0.3f),
                focusedTextColor = SafeSphereColors.TextPrimary,
                unfocusedTextColor = SafeSphereColors.TextPrimary,
                cursorColor = SafeSphereColors.Primary
            )
        )
    }
}

/**
 * Password Strength Indicator
 */
@Composable
fun PasswordStrengthIndicator(password: String) {
    val strength = calculatePasswordStrength(password)

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index < strength.level) strength.color
                            else SafeSphereColors.TextSecondary.copy(alpha = 0.2f)
                        )
                )
            }
        }

        Text(
            text = strength.text,
            fontSize = 12.sp,
            color = strength.color,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

data class PasswordStrengthResult(
    val level: Int,
    val text: String,
    val color: Color
)

fun calculatePasswordStrength(password: String): PasswordStrengthResult {
    var level = 0
    val length = password.length

    if (length >= 8) level++
    if (password.any { it.isUpperCase() }) level++
    if (password.any { it.isDigit() }) level++
    if (password.any { !it.isLetterOrDigit() }) level++

    return when (level) {
        0, 1 -> PasswordStrengthResult(1, "Weak password", SafeSphereColors.Error)
        2 -> PasswordStrengthResult(2, "Fair password", SafeSphereColors.Warning)
        3 -> PasswordStrengthResult(3, "Good password", SafeSphereColors.Info)
        4 -> PasswordStrengthResult(4, "Strong password", SafeSphereColors.Success)
        else -> PasswordStrengthResult(1, "Weak password", SafeSphereColors.Error)
    }
}
