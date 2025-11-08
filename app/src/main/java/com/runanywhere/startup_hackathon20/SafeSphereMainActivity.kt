package com.runanywhere.startup_hackathon20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.runanywhere.startup_hackathon20.data.*
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen
import com.runanywhere.startup_hackathon20.ui.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * SafeSphere - Privacy-First Mobile Application
 *
 * Main Activity containing all UI screens and navigation
 *
 * Features:
 * - Onboarding & Awareness
 * - Dashboard with quick access
 * - Privacy Vault (encrypted storage)
 * - Offline AI Chat
 * - Data Map (visualization)
 * - Threat Simulation (educational)
 * - Settings & Consent
 * - Model Management
 */
class SafeSphereMainActivity : FragmentActivity() {

    private val viewModel: SafeSphereViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge for gesture navigation
        enableEdgeToEdge()

        setContent {
            SafeSphereTheme {
                SafeSphereApp(viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Mark app as stopped (going to background)
        val prefs = getSharedPreferences("safesphere_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("app_in_background", true).apply()
    }

    override fun onStart() {
        super.onStart()
        // App is coming to foreground
        // The lock state will be checked in SafeSphereApp composable
    }
}

/**
 * Main app container with navigation
 */
@Composable
fun SafeSphereApp(viewModel: SafeSphereViewModel) {
    // Splash screen state
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onSplashComplete = { showSplash = false })
    } else {
        MainAppContent(viewModel)
    }
}

/**
 * Main app content (after splash screen)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: SafeSphereViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? FragmentActivity

    // Biometric lock state
    var isAppLocked by remember { mutableStateOf(true) }
    var biometricAttempts by remember { mutableStateOf(0) }
    val maxBiometricAttempts = 5
    var showPasswordFallback by remember { mutableStateOf(false) }

    // Make biometric settings reactive by creating state
    val prefs =
        context.getSharedPreferences("safesphere_prefs", android.content.Context.MODE_PRIVATE)

    var isBiometricEnabled by remember {
        mutableStateOf(prefs.getBoolean("biometric_enabled", false))
    }

    var wasInBackground by remember {
        mutableStateOf(prefs.getBoolean("app_in_background", false))
    }

    // Re-read values on every composition to stay fresh
    LaunchedEffect(Unit) {
        isBiometricEnabled = prefs.getBoolean("biometric_enabled", false)
        wasInBackground = prefs.getBoolean("app_in_background", false)

        android.util.Log.d(
            "SafeSphere",
            "App opened - currentUser: ${currentUser != null}, biometric: $isBiometricEnabled, wasInBackground: $wasInBackground"
        )
    }

    // Show biometric prompt automatically when app opens (if enabled and user logged in)
    LaunchedEffect(currentUser, isBiometricEnabled, wasInBackground) {
        android.util.Log.d(
            "SafeSphere",
            "LaunchedEffect triggered - currentUser: ${currentUser != null}, biometric: $isBiometricEnabled, wasInBackground: $wasInBackground"
        )

        if (currentUser != null && isBiometricEnabled && wasInBackground && activity != null) {
            android.util.Log.d("SafeSphere", "Showing biometric prompt")
            // App needs to be unlocked with biometric
            com.runanywhere.startup_hackathon20.security.BiometricAuthManager.authenticate(
                activity = activity,
                title = "Unlock SafeSphere",
                subtitle = "Use biometric to unlock",
                onSuccess = {
                    android.util.Log.d("SafeSphere", "Biometric success")
                    isAppLocked = false
                    biometricAttempts = 0
                    wasInBackground = false
                    // Clear the background flag
                    prefs.edit().putBoolean("app_in_background", false).apply()
                },
                onError = { errorCode, errorMessage ->
                    android.util.Log.e("SafeSphere", "Biometric error: $errorCode - $errorMessage")
                },
                onFailed = {
                    android.util.Log.d("SafeSphere", "Biometric failed")
                    biometricAttempts++
                    if (biometricAttempts >= maxBiometricAttempts) {
                        showPasswordFallback = true
                    }
                }
            )
        } else {
            android.util.Log.d("SafeSphere", "No biometric required, unlocking")
            // No biometric required, unlock immediately
            isAppLocked = false
        }
    }

    // Handle back button/gesture navigation
    BackHandler(enabled = true) {
        scope.launch {
            // Close drawer if open
            if (drawerState.isOpen) {
                drawerState.close()
            } else {
                // Navigate back based on current screen
                val canGoBack = viewModel.navigateBack()
                if (!canGoBack) {
                    // On dashboard or login, exit app (handled by system)
                }
            }
        }
    }

    // Screens that don't need navigation drawer
    val authScreens = listOf(
        SafeSphereScreen.LOGIN,
        SafeSphereScreen.REGISTER,
        SafeSphereScreen.ONBOARDING
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SafeSphereColors.Background
    ) {
        // Background gradient effect
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
            // Show biometric lock screen if app is locked
            if (isAppLocked && currentUser != null && isBiometricEnabled) {
                BiometricLockScreen(
                    attempts = biometricAttempts,
                    maxAttempts = maxBiometricAttempts,
                    showPasswordFallback = showPasswordFallback,
                    onRetryBiometric = {
                        if (activity != null && biometricAttempts < maxBiometricAttempts) {
                            com.runanywhere.startup_hackathon20.security.BiometricAuthManager.authenticate(
                                activity = activity,
                                title = "Unlock SafeSphere",
                                subtitle = "Attempt ${biometricAttempts + 1} of $maxBiometricAttempts",
                                onSuccess = {
                                    isAppLocked = false
                                    biometricAttempts = 0
                                    wasInBackground = false
                                    prefs.edit().putBoolean("app_in_background", false).apply()
                                },
                                onError = { errorCode, errorMessage ->
                                    // Error occurred
                                },
                                onFailed = {
                                    biometricAttempts++
                                    if (biometricAttempts >= maxBiometricAttempts) {
                                        showPasswordFallback = true
                                    }
                                }
                            )
                        }
                    },
                    onPasswordLogin = { email, password ->
                        // Verify password
                        scope.launch {
                            val credentials = LoginCredentials(email, password)
                            val result = viewModel.login(credentials)
                            // If login successful, unlock app
                            if (result is AuthResult.Success) {
                                isAppLocked = false
                                biometricAttempts = 0
                                showPasswordFallback = false
                                wasInBackground = false
                                // Clear the background flag
                                prefs.edit().putBoolean("app_in_background", false).apply()
                            }
                        }
                    }
                )
            } else {
                // Show normal app content
                // Check if current screen needs drawer
                if (currentScreen in authScreens) {
                    // Show screens without drawer (Login, Register, Onboarding)
                    when (currentScreen) {
                        SafeSphereScreen.LOGIN -> LoginScreen(
                            onLoginSuccess = { user ->
                                // User logged in successfully
                            },
                            onNavigateToRegister = {
                                viewModel.navigateToScreen(SafeSphereScreen.REGISTER)
                            },
                            onLogin = { credentials ->
                                viewModel.login(credentials)
                            },
                            onNavigateToDashboard = {
                                viewModel.navigateToScreen(SafeSphereScreen.DASHBOARD)
                            }
                        )

                        SafeSphereScreen.REGISTER -> RegisterScreen(
                            onRegisterSuccess = { user ->
                                // User registered successfully
                            },
                            onNavigateToLogin = {
                                viewModel.navigateToScreen(SafeSphereScreen.LOGIN)
                            },
                            onRegister = { data ->
                                viewModel.register(data)
                            },
                            onNavigateToOnboarding = {
                                viewModel.navigateToScreen(SafeSphereScreen.ONBOARDING)
                            }
                        )

                        SafeSphereScreen.ONBOARDING -> OnboardingScreen(viewModel)
                        else -> {}
                    }
                } else {
                    // Show screens WITH navigation drawer and bottom nav
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = true, // Enable swipe gestures
                        drawerContent = {
                            SafeSphereDrawerContent(
                                currentUser = currentUser,
                                currentScreen = currentScreen,
                                onNavigate = { screen ->
                                    viewModel.navigateToScreen(screen)
                                    scope.launch { drawerState.close() }
                                },
                                onLogout = {
                                    viewModel.logout()
                                }
                            )
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                BeautifulTopBar(
                                    title = getScreenTitle(currentScreen),
                                    onMenuClick = {
                                        scope.launch {
                                            if (drawerState.isClosed) drawerState.open()
                                            else drawerState.close()
                                        }
                                    },
                                    onNotificationClick = {
                                        viewModel.navigateToScreen(SafeSphereScreen.NOTIFICATIONS)
                                    }
                                )
                            },
                            bottomBar = {
                                // Removed bottom bar
                            },
                            containerColor = Color.Transparent
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                when (currentScreen) {
                                    SafeSphereScreen.DASHBOARD -> DashboardScreen(viewModel)
                                    SafeSphereScreen.PRIVACY_VAULT -> PrivacyVaultScreen(viewModel)
                                    SafeSphereScreen.PASSWORDS -> PasswordsScreen(viewModel)
                                    SafeSphereScreen.AI_CHAT -> AIChatScreen(viewModel)
                                    SafeSphereScreen.DATA_MAP -> DataMapScreen(viewModel)
                                    SafeSphereScreen.THREAT_SIMULATION -> ThreatSimulationScreen(
                                        viewModel
                                    )

                                    SafeSphereScreen.SETTINGS -> SettingsScreen(viewModel)
                                    SafeSphereScreen.MODELS -> ModelsScreen(viewModel)
                                    SafeSphereScreen.NOTIFICATIONS -> NotificationsScreen(viewModel)
                                    SafeSphereScreen.PASSWORD_HEALTH -> PasswordHealthScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { viewModel.navigateBack() }
                                    )

                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }

            // Show UI messages as snackbar
            uiMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .zIndex(10f),
                    containerColor = SafeSphereColors.Primary,
                    contentColor = Color.White
                ) {
                    Text(message)
                }
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearMessage()
                }
            }
        }
    }
}

/**
 * Biometric Lock Screen - Shown when app is locked
 */
@Composable
fun BiometricLockScreen(
    attempts: Int,
    maxAttempts: Int,
    showPasswordFallback: Boolean,
    onRetryBiometric: () -> Unit,
    onPasswordLogin: (email: String, password: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Text(
                text = "🔐",
                fontSize = 80.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SafeSphere Locked",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (showPasswordFallback) {
                // Show password login form
                Text(
                    text = "Maximum biometric attempts reached.\nPlease enter your credentials.",
                    fontSize = 14.sp,
                    color = SafeSphereColors.Error,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onPasswordLogin(email, password) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafeSphereColors.Primary
                    )
                ) {
                    Text("Unlock")
                }
            } else {
                // Show biometric prompt status
                Text(
                    text = "Use fingerprint or face to unlock",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (attempts > 0) {
                    Text(
                        text = "Attempt $attempts of $maxAttempts failed",
                        fontSize = 12.sp,
                        color = SafeSphereColors.Error
                    )

                    Text(
                        text = "${maxAttempts - attempts} attempts remaining",
                        fontSize = 12.sp,
                        color = SafeSphereColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Fingerprint icon
                Text(
                    text = "👆",
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRetryBiometric,
                    enabled = attempts < maxAttempts,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafeSphereColors.Primary
                    )
                ) {
                    Text("Try Again")
                }
            }
        }
    }
}

/**
 * Beautiful Top Bar with Gradient Title
 */
@Composable
fun BeautifulTopBar(
    title: String,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = SafeSphereColors.Surface.copy(alpha = 0.95f),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            SafeSphereColors.Primary.copy(alpha = 0.1f),
                            SafeSphereColors.Secondary.copy(alpha = 0.1f),
                            SafeSphereColors.Accent.copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, top = 20.dp, end = 8.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Menu button (hamburger icon)
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SafeSphereColors.Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu",
                            tint = SafeSphereColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Center: Beautiful Gradient Title
                Text(
                    text = title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                SafeSphereColors.Primary,
                                SafeSphereColors.Secondary,
                                SafeSphereColors.Accent
                            )
                        )
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                // Right: Notification button
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SafeSphereColors.Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = SafeSphereColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        // Notification badge
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp)
                                .clip(CircleShape)
                                .background(SafeSphereColors.Error)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Beautiful Bottom Navigation Bar with 3 Buttons
 */
@Composable
fun BeautifulBottomBar(
    currentScreen: SafeSphereScreen,
    onMenuClick: () -> Unit,
    onHomeClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        color = SafeSphereColors.Surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            SafeSphereColors.Primary.copy(alpha = 0.05f),
                            SafeSphereColors.Secondary.copy(alpha = 0.05f),
                            SafeSphereColors.Accent.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Unique Menu Button (Hexagon style)
                BottomNavButton(
                    icon = Icons.Filled.Menu,
                    label = "Menu",
                    isSelected = false,
                    onClick = onMenuClick,
                    isUnique = true
                )

                // Center: Home Button (Large)
                BottomNavButton(
                    icon = Icons.Filled.Home,
                    label = "Home",
                    isSelected = currentScreen == SafeSphereScreen.DASHBOARD,
                    onClick = onHomeClick,
                    isCenter = true
                )

                // Right: Notification Button
                BottomNavButton(
                    icon = Icons.Filled.Notifications,
                    label = "Alerts",
                    isSelected = currentScreen == SafeSphereScreen.NOTIFICATIONS,
                    onClick = onNotificationClick,
                    showBadge = true
                )
            }
        }
    }
}

/**
 * Bottom Navigation Button
 */
@Composable
fun BottomNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isUnique: Boolean = false,
    isCenter: Boolean = false,
    showBadge: Boolean = false
) {
    val buttonSize = if (isCenter) 64.dp else 56.dp
    val iconSize = if (isCenter) 28.dp else 24.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Background
            if (isUnique) {
                // Hexagon-like unique shape for menu
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(RoundedCornerShape(12.dp))
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
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                }
            } else if (isCenter) {
                // Large center button with elevation
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .offset(y = (-12).dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        SafeSphereColors.Primary,
                                        SafeSphereColors.Secondary
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        SafeSphereColors.Surface,
                                        SafeSphereColors.Surface
                                    )
                                )
                            }
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    SafeSphereColors.Primary.copy(alpha = 0.3f),
                                    SafeSphereColors.Secondary.copy(alpha = 0.3f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else SafeSphereColors.Primary,
                        modifier = Modifier.size(iconSize)
                    )
                }
            } else {
                // Regular button
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) SafeSphereColors.Primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) SafeSphereColors.Primary else SafeSphereColors.TextSecondary,
                        modifier = Modifier.size(iconSize)
                    )
                    
                    // Badge for notifications
                    if (showBadge && !isSelected) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-8).dp, y = 8.dp)
                                .clip(CircleShape)
                                .background(SafeSphereColors.Error)
                        )
                    }
                }
            }
        }

        // Label
        if (!isCenter) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isUnique) SafeSphereColors.Primary
                       else if (isSelected) SafeSphereColors.Primary
                       else SafeSphereColors.TextSecondary
            )
        }
    }
}

/**
 * Get screen title for top bar
 */
fun getScreenTitle(screen: SafeSphereScreen): String {
    return when (screen) {
        SafeSphereScreen.DASHBOARD -> "SafeSphere"
        SafeSphereScreen.PRIVACY_VAULT -> "Privacy Vault"
        SafeSphereScreen.AI_CHAT -> "AI Assistant"
        SafeSphereScreen.DATA_MAP -> "Data Insights"
        SafeSphereScreen.THREAT_SIMULATION -> "Security Training"
        SafeSphereScreen.SETTINGS -> "Settings"
        SafeSphereScreen.MODELS -> "AI Models"
        SafeSphereScreen.NOTIFICATIONS -> "Notifications"
        SafeSphereScreen.PASSWORD_HEALTH -> "Password Health"
        else -> "SafeSphere"
    }
}

/**
 * Onboarding Screen - Explains privacy concepts
 */
@Composable
fun OnboardingScreen(viewModel: SafeSphereViewModel) {
    var currentPage by remember { mutableStateOf(0) }
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to SafeSphere",
            description = "Your privacy fortress. All data stays on your device, encrypted with military-grade AES-256.",
            icon = "🔐",
            color = SafeSphereColors.Primary
        ),
        OnboardingPage(
            title = "Offline AI Power",
            description = "AI runs entirely on your device. No cloud, no tracking, no data leaks. Complete privacy.",
            icon = "🤖",
            color = SafeSphereColors.Secondary
        ),
        OnboardingPage(
            title = "Hardware Encryption",
            description = "Your encryption keys are stored in secure hardware. Even if someone steals your phone, they can't access your data.",
            icon = "🛡️",
            color = SafeSphereColors.Accent
        ),
        OnboardingPage(
            title = "You're in Control",
            description = "No backdoors. No cloud sync. No tracking. Your data belongs to you, and only you.",
            icon = "✨",
            color = SafeSphereColors.Success
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Page indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentPage) 32.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentPage) SafeSphereColors.Primary
                            else SafeSphereColors.TextSecondary.copy(alpha = 0.3f)
                        )
                        .animateContentSize()
                )
            }
        }

        // Content
        val page = pages[currentPage]
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = page.icon,
                fontSize = 80.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = page.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = page.description,
                fontSize = 16.sp,
                color = SafeSphereColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentPage > 0) {
                GlassButton(
                    text = "Back",
                    onClick = { currentPage-- },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.width(16.dp))

            GlassButton(
                text = if (currentPage == pages.size - 1) "Get Started" else "Next",
                onClick = {
                    if (currentPage == pages.size - 1) {
                        viewModel.initializeDemoData()
                        viewModel.navigateToScreen(SafeSphereScreen.DASHBOARD)
                    } else {
                        currentPage++
                    }
                },
                primary = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String,
    val color: Color
)

/**
 * Quick Action Button Component
 */
@Composable
fun QuickActionButton(
    icon: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = SafeSphereColors.TextPrimary
            )
        }
    }
}

/**
 * Dashboard Screen - Central hub
 */
@Composable
fun DashboardScreen(viewModel: SafeSphereViewModel) {
    val isOffline by viewModel.isOfflineMode.collectAsState()
    val stats by viewModel.storageStats.collectAsState()
    val vaultItems by viewModel.vaultItems.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Get password repository and count
    val passwordRepository = remember { com.runanywhere.startup_hackathon20.data.PasswordVaultRepository.getInstance(context) }
    val savedPasswords by passwordRepository.passwords.collectAsState()
    val passwordCount = savedPasswords.size
    
    // Check autofill status
    val isAutofillEnabled = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val autofillManager = context.getSystemService(android.view.autofill.AutofillManager::class.java)
            autofillManager?.hasEnabledAutofillServices() == true
        } else {
            false
        }
    }

    // Calculate password health in real-time
    val passwordHealth = remember(vaultItems) {
        if (vaultItems.isEmpty()) {
            null
        } else {
            com.runanywhere.startup_hackathon20.utils.PasswordAnalyzer.analyzePasswords(vaultItems) { encryptedContent ->
                com.runanywhere.startup_hackathon20.security.SecurityManager.decrypt(
                    encryptedContent
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Security Score Card
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Security Score",
                    fontSize = 16.sp,
                    color = SafeSphereColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = stats.securityScore / 100f,
                        modifier = Modifier.size(120.dp),
                        strokeWidth = 12.dp,
                        color = when {
                            stats.securityScore >= 90 -> SafeSphereColors.Success
                            stats.securityScore >= 70 -> SafeSphereColors.Warning
                            else -> SafeSphereColors.Error
                        },
                        trackColor = SafeSphereColors.TextSecondary.copy(alpha = 0.1f)
                    )

                    Text(
                        text = "${stats.securityScore}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${stats.encryptedItems} of ${stats.totalItems} items encrypted",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // PASSWORD MANAGER QUICK ACCESS CARD - NEW PROMINENT SECTION
        PasswordManagerQuickAccessCard(
            passwordCount = passwordCount,
            isAutofillEnabled = isAutofillEnabled,
            onViewAllClick = { viewModel.navigateToScreen(SafeSphereScreen.PASSWORDS) },
            onEnableAutofillClick = {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
                    context.startActivity(intent)
                }
            },
            onAddPasswordClick = { viewModel.navigateToScreen(SafeSphereScreen.PASSWORDS) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Password Health Card - Show if passwords exist
        passwordHealth?.let { health ->
            val breachedCount = health.passwordDetails.count { it.breachResult?.isBreached == true }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateToScreen(SafeSphereScreen.PASSWORD_HEALTH) }
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔐 Password Health",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.TextPrimary
                        )

                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = "View Details",
                            tint = SafeSphereColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Breach Alert Banner (if any breached passwords)
                    if (breachedCount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD32F2F).copy(alpha = 0.15f))
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFD32F2F).copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🚨",
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = "$breachedCount LEAKED!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Score and Issues
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT: Circular Score Indicator (like Security Score)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(100.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = health.overallScore / 100f,
                                modifier = Modifier.size(100.dp),
                                strokeWidth = 10.dp,
                                color = when {
                                    health.overallScore >= 80 -> Color(0xFF388E3C)
                                    health.overallScore >= 60 -> Color(0xFFFBC02D)
                                    else -> Color(0xFFD32F2F)
                                },
                                trackColor = SafeSphereColors.TextSecondary.copy(alpha = 0.1f)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${health.overallScore}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        health.overallScore >= 80 -> Color(0xFF388E3C)
                                        health.overallScore >= 60 -> Color(0xFFFBC02D)
                                        else -> Color(0xFFD32F2F)
                                    }
                                )
                                Text(
                                    text = "/ 100",
                                    fontSize = 12.sp,
                                    color = SafeSphereColors.TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // RIGHT: Issues Summary
                        Column(modifier = Modifier.weight(1f)) {
                            // Issues list
                            if (health.weakPasswords > 0 || health.duplicatePasswords > 0 || breachedCount > 0) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (breachedCount > 0) {
                                        Text(
                                            text = "🚨 $breachedCount breached",
                                            fontSize = 13.sp,
                                            color = Color(0xFFD32F2F),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (health.weakPasswords > 0) {
                                        Text(
                                            text = "⚠️ ${health.weakPasswords} weak",
                                            fontSize = 13.sp,
                                            color = SafeSphereColors.TextSecondary
                                        )
                                    }
                                    if (health.duplicatePasswords > 0) {
                                        Text(
                                            text = "❌ ${health.duplicatePasswords} duplicates",
                                            fontSize = 13.sp,
                                            color = SafeSphereColors.TextSecondary
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "✅ All passwords secure!",
                                    fontSize = 14.sp,
                                    color = Color(0xFF388E3C),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Quick Access Grid
        Text(
            text = "Quick Access",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SafeSphereColors.TextPrimary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Password Manager Quick Access Card is positioned above the grid below

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DashboardCard(
                    title = "Privacy Vault",
                    icon = "🔐",
                    description = "${stats.totalItems} items",
                    color = SafeSphereColors.Primary,
                    onClick = { viewModel.navigateToScreen(SafeSphereScreen.PRIVACY_VAULT) },
                    modifier = Modifier.weight(1f)
                )

                DashboardCard(
                    title = "AI Chat",
                    icon = "💬",
                    description = "Offline advisor",
                    color = SafeSphereColors.Secondary,
                    onClick = { viewModel.navigateToScreen(SafeSphereScreen.AI_CHAT) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DashboardCard(
                    title = "Data Map",
                    icon = "📊",
                    description = "Visualize storage",
                    color = SafeSphereColors.Accent,
                    onClick = { viewModel.navigateToScreen(SafeSphereScreen.DATA_MAP) },
                    modifier = Modifier.weight(1f)
                )

                DashboardCard(
                    title = "Threats",
                    icon = "🛡️",
                    description = "Simulation zone",
                    color = SafeSphereColors.Warning,
                    onClick = { viewModel.navigateToScreen(SafeSphereScreen.THREAT_SIMULATION) },
                    modifier = Modifier.weight(1f)
                )
            }

            DashboardCard(
                title = "Manage AI Models",
                icon = "🤖",
                description = "Download offline AI",
                color = SafeSphereColors.Info,
                onClick = { viewModel.navigateToScreen(SafeSphereScreen.MODELS) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Password Manager Quick Access Card
 */
@Composable
fun PasswordManagerQuickAccessCard(
    passwordCount: Int,
    isAutofillEnabled: Boolean,
    onViewAllClick: () -> Unit,
    onEnableAutofillClick: () -> Unit,
    onAddPasswordClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🗝️",
                            fontSize = 32.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Password Manager",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = SafeSphereColors.TextPrimary
                        )
                    }
                }
                // View All button
                GlassButton(
                    text = "View All",
                    onClick = onViewAllClick,
                    modifier = Modifier
                        .height(34.dp)
                        .align(Alignment.CenterVertically),
                    primary = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$passwordCount Saved Password${if (passwordCount == 1) "" else "s"}",
                    fontSize = 15.sp,
                    color = SafeSphereColors.TextSecondary
                )

                Spacer(Modifier.width(22.dp))

                // Autofill Status
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isAutofillEnabled) SafeSphereColors.Success.copy(alpha = 0.18f)
                            else SafeSphereColors.Warning.copy(alpha = 0.18f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isAutofillEnabled) "Autofill: ON" else "Autofill: OFF",
                            fontWeight = FontWeight.Medium,
                            color = if (isAutofillEnabled) SafeSphereColors.Success else SafeSphereColors.Warning,
                            fontSize = 13.sp,
                        )
                        if (!isAutofillEnabled) {
                            // Add enable autofill quick action
                            TextButton(
                                onClick = onEnableAutofillClick,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Enable",
                                    color = SafeSphereColors.Primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassButton(
                    text = "Add Password",
                    onClick = onAddPasswordClick,
                    modifier = Modifier.height(34.dp),
                    primary = false
                )
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = icon,
                    fontSize = 32.sp
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.TextPrimary
                )

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Privacy Vault Screen - Encrypted storage management
 */
@Composable
fun PrivacyVaultScreen(viewModel: SafeSphereViewModel) {
    val vaultItems by viewModel.vaultItems.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<PrivacyVaultItem?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            if (vaultItems.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🔐",
                        fontSize = 64.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Your vault is empty",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Add sensitive data to store it encrypted",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    GlassButton(
                        text = "Add First Item",
                        onClick = { showAddDialog = true },
                        primary = true
                    )
                }
            } else {
                // Vault items list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 88.dp // Extra padding for FAB
                    )
                ) {
                    items(vaultItems) { item ->
                        VaultItemCard(
                            item = item,
                            onClick = { selectedItem = item }
                        )
                    }
                }
            }
        }

        // Floating Action Button - Always visible when items exist
        if (vaultItems.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = SafeSphereColors.Primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Item",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Add/Edit Dialog
        if (showAddDialog) {
            AddVaultItemDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, content, category ->
                    viewModel.addVaultItem(title, content, category)
                    showAddDialog = false
                }
            )
        }

        // View Item Dialog - This has biometric authentication
        selectedItem?.let { item ->
            ViewVaultItemDialog(
                item = item,
                viewModel = viewModel,
                onDismiss = { selectedItem = null }
            )
        }
    }
}

@Composable
fun VaultItemCard(
    item: PrivacyVaultItem,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SafeSphereColors.Primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.category.icon,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.TextPrimary
                )

                Text(
                    text = item.category.displayName,
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }

            if (item.isEncrypted) {
                Text(
                    text = "🔒",
                    fontSize = 20.sp
                )
            }
        }
    }
}

// ... Continue in next part ...
