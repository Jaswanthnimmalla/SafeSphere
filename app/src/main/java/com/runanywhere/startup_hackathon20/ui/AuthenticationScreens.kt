package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.delay

/**
 * Login Screen - Beautiful Material Design 3
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (User) -> Unit,
    onNavigateToRegister: () -> Unit,
    onLogin: suspend (LoginCredentials) -> AuthResult
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

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

                    // Email Field
                    AuthTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = "Email",
                        placeholder = "Enter your email",
                        leadingIcon = Icons.Default.Email,
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
                        leadingIcon = Icons.Default.Lock,
                        trailingIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
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
                                    imageVector = Icons.Default.Warning,
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
                                kotlinx.coroutines.GlobalScope.launch {
                                    val result = onLogin(LoginCredentials(email, password))
                                    isLoading = false

                                    when (result) {
                                        is AuthResult.Success -> {
                                            onLoginSuccess(result.user)
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

                    // Biometric Login Button (Future feature)
                    OutlinedButton(
                        onClick = { /* TODO: Biometric */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SafeSphereColors.Primary
                        ),
                        border = BorderStroke(1.dp, SafeSphereColors.Primary.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Login with Fingerprint")
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
 * Register Screen - Beautiful Material Design 3
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: (User) -> Unit,
    onNavigateToLogin: () -> Unit,
    onRegister: suspend (RegistrationData) -> AuthResult
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

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
                        leadingIcon = Icons.Default.Person,
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
                        leadingIcon = Icons.Default.Email,
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
                        leadingIcon = Icons.Default.Lock,
                        trailingIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
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
                        leadingIcon = Icons.Default.Lock,
                        trailingIcon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
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
                                    imageVector = Icons.Default.Warning,
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
                                kotlinx.coroutines.GlobalScope.launch {
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
                                            onRegisterSuccess(result.user)
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
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
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
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = SafeSphereColors.TextSecondary
                        )
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
